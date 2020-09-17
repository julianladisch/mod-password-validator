package org.folio.services.validator.engine;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.folio.HttpStatus;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.services.validator.registry.ValidatorRegistryService;
import org.folio.services.validator.util.ValidatorHelper;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.folio.rest.RestVerticle.MODULE_SPECIFIC_ARGS;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

/**
 * Implementation of the ValidationEngineService;
 * calls ValidationRegistry service to obtain rules,
 * runs rules to validate password,
 * pushes validation result in result handler to return.
 *
 * @see ValidationEngineService
 * @see ValidatorRegistryService
 */
public class ValidationEngineServiceImpl implements ValidationEngineService {

  private static final String OKAPI_URL_HEADER = "x-okapi-url";
  private static final String REGEXP_USER_NAME_PLACEHOLDER = "<USER_NAME>";

  // Logger
  private final Logger logger = LoggerFactory
    .getLogger(ValidationEngineServiceImpl.class);
  // Timeout to wait for response
  private int lookupTimeout = Integer
    .parseInt(MODULE_SPECIFIC_ARGS.getOrDefault("lookup.timeout", "1000"));
  // Repository component to validation obtain rules
  private ValidatorRegistryService validatorRegistryProxy;
  // Http client to call programmatic rules as internal OKAPI endpoints
  private WebClient webClient;

  public ValidationEngineServiceImpl() {
  }

  public ValidationEngineServiceImpl(final Vertx vertx) {
    this.validatorRegistryProxy = ValidatorRegistryService
      .createProxy(vertx, ValidatorHelper.REGISTRY_SERVICE_ADDRESS);
    initWebClient(vertx);
  }

  private void initWebClient(final Vertx vertx) {
    this.webClient = WebClient.create(vertx,
      new WebClientOptions().setConnectTimeout(lookupTimeout).setIdleTimeout(lookupTimeout));
  }

  /**
   * Validates received password.
   * Calls ValidationRegistry service to obtain rules specific to tenant,
   * runs rules to validate password in one loop,
   * pushes validation result into result handler to return.
   *
   * @param password       received password for validation
   * @param requestHeaders request headers needed for access backend FOLIO services to perform programmatic rules validation
   * @param resultHandler  handler with validation results in format <Status, Message>
   */

  @Override
  public void validatePassword(final String userId,
                               final String password,
                               final Map<String, String> requestHeaders,
                               final Handler<AsyncResult<JsonObject>> resultHandler) {
    MultiMap caseInsensitiveHeaders = MultiMap.caseInsensitiveMultiMap().addAll(requestHeaders);
    String tenantId = caseInsensitiveHeaders.get(OKAPI_HEADER_TENANT);
    validatorRegistryProxy.getAllTenantRules(tenantId, 500, 0, "query=state=Enabled", rulesResponse -> {
      if (rulesResponse.failed()) {
        resultHandler.handle(Future.failedFuture(rulesResponse.cause().getMessage()));
        return;
      }
      lookupUser(userId, caseInsensitiveHeaders).onComplete(lookupUserHandler -> {
        if (lookupUserHandler.failed()) {
          resultHandler.handle(Future.failedFuture(lookupUserHandler.cause().getMessage()));
          return;
        }
        List<Rule> rules = rulesResponse.result().mapTo(RuleCollection.class).getRules();
        prepareRulesBeforeValidation(rules, lookupUserHandler);
        Future<List<String>> errorMessagesFuture = validatePasswordByRules(rules, userId, password, caseInsensitiveHeaders);
        errorMessagesFuture.onComplete(asyncResult -> {
          if (asyncResult.failed()) {
            resultHandler.handle(Future.failedFuture(asyncResult.cause()));
            return;
          }
          prepareResponse(asyncResult.result(), resultHandler);
        });
      });
    });
  }

  private List<Rule> prepareRulesBeforeValidation(List<Rule> rules, AsyncResult<JsonObject> lookupUserHandler) {
    JsonObject user = lookupUserHandler.result();
    String userName = user.getString("username");
    for (Rule rule : rules) {
      if (Rule.Type.REG_EXP.equals(rule.getType())) {
        rule.setExpression(rule.getExpression().replace(REGEXP_USER_NAME_PLACEHOLDER, userName));
      }
    }
    return rules.stream().sorted(Comparator.comparing(Rule::getOrderNo)).collect(Collectors.toList());
  }

  private Future<List<String>> validatePasswordByRules(final List<Rule> rules,
                                                       final String userId,
                                                       final String password,
                                                       final MultiMap headers) {
    List<String> errorMessages = new ArrayList<>(rules.size());
    rules.sort(Comparator.comparing(Rule::getOrderNo));

    Promise<List<String>> promise = Promise.promise();
    List<Future> programmaticRulesFutures = new ArrayList<>();
    for (Rule rule : rules) {
      if (Rule.Type.REG_EXP.equals(rule.getType())) {
        validatePasswordByRexExpRule(password, rule, errorMessages);
      } else if (Rule.Type.PROGRAMMATIC.equals(rule.getType())) {
        programmaticRulesFutures
          .add(getValidatePasswordByProgrammaticRuleFuture(userId, password, rule, errorMessages, headers));
      }
    }
    // Notify external method future handler when all programmatic rule futures complete
    CompositeFuture.all(programmaticRulesFutures).onComplete(compositeFutureAsyncResult -> {
      if (compositeFutureAsyncResult.succeeded()) {
        promise.complete(errorMessages);
      } else {
        promise.fail(compositeFutureAsyncResult.cause().getMessage());
      }
    });
    return promise.future();
  }

  private void validatePasswordByRexExpRule(final String password,
                                            final Rule rule,
                                            final List<String> errorMessages) {
    String expression = rule.getExpression();
    if (!Pattern.compile(expression).matcher(password).matches()) {
      errorMessages.add(rule.getErrMessageId());
    }
  }

  private Future<JsonObject> lookupUser(String userId, MultiMap headers) {
    Promise<JsonObject> promise = Promise.promise();
    String okapiUrl = headers.get(OKAPI_URL_HEADER);
    String userNameRequestUrl = String.format("%s/users?query=id==%s", okapiUrl, userId);
    HttpRequest<Buffer> request = webClient.getAbs(userNameRequestUrl);
    request
      .putHeader(OKAPI_HEADER_TOKEN, headers.get(OKAPI_HEADER_TOKEN))
      .putHeader(OKAPI_HEADER_TENANT, headers.get(OKAPI_HEADER_TENANT))
      .putHeader(HttpHeaders.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON)
      .putHeader(HttpHeaders.ACCEPT.toString(), MediaType.APPLICATION_JSON)
      .send(ar -> {
        if (ar.failed()) {
          promise.fail(ar.cause().getMessage());
          return;
        }
        HttpResponse<Buffer> response = ar.result();
        if (response.statusCode() != HttpStatus.HTTP_OK.toInt()) {
          promise.fail("Error getting user by user id : " + userId);
          return;
        }
        JsonObject resultObject = response.bodyAsJsonObject();
        if (!resultObject.containsKey("totalRecords") || !resultObject.containsKey("users")) {
          promise.fail("Error, missing field(s) 'totalRecords' and/or 'users' in user response object");
        } else {
          int recordCount = resultObject.getInteger("totalRecords");
          if (recordCount > 1) {
            String errorMessage = "Bad results from username";
            logger.error(errorMessage);
            promise.fail(errorMessage);
          } else if (recordCount == 0) {
            String errorMessage = "No user found by user id : " + userId;
            logger.error(errorMessage);
            promise.fail(errorMessage);
          } else {
            JsonObject resultUser = resultObject.getJsonArray("users").getJsonObject(0);
            promise.complete(resultUser);
          }
        }
      });
    return promise.future();
  }

  private Future<String> getValidatePasswordByProgrammaticRuleFuture(final String userId,
                                                                     final String password,
                                                                     final Rule rule,
                                                                     final List<String> errorMessages,
                                                                     final MultiMap headers) {
    String okapiURL = headers.get(OKAPI_URL_HEADER);
    String remoteModuleUrl = okapiURL + rule.getImplementationReference();

    Promise<String> promise = Promise.promise();
    HttpRequest<Buffer> passwordValidationRequest = webClient.postAbs(remoteModuleUrl);
    passwordValidationRequest
      .putHeader(OKAPI_HEADER_TOKEN, headers.get(OKAPI_HEADER_TOKEN))
      .putHeader(OKAPI_HEADER_TENANT, headers.get(OKAPI_HEADER_TENANT))
      .putHeader(HttpHeaders.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON)
      .putHeader(HttpHeaders.ACCEPT.toString(), MediaType.APPLICATION_JSON)
      .sendJsonObject(buildResetPasswordAction(userId, password), ar -> {
        if (ar.failed()) {
          promise.fail(ar.cause().getMessage());
          return;
        }
        HttpResponse<Buffer> validationResponse = ar.result();
        if (validationResponse.statusCode() == HttpStatus.HTTP_OK.toInt()) {
          String validationResult = validationResponse.bodyAsJsonObject().getString(ValidatorHelper.RESPONSE_VALIDATION_RESULT_KEY);
          if (ValidatorHelper.VALIDATION_INVALID_RESULT.equals(validationResult)) {
            errorMessages.add(rule.getErrMessageId());
          }
          promise.complete();
        } else {
          // TODO Inform administrator that remote module is down
          logger.error("FOLIO module by the address " + remoteModuleUrl + " is not available.");
          String errorMessage;
          switch (rule.getValidationType()) {
            case STRONG:
              errorMessage = "Programmatic rule " +
                rule.getName() +
                " returns status code " +
                validationResponse.statusCode();
              logger.error(errorMessage);
              promise.fail(errorMessage);
              break;
            case SOFT:
              promise.complete();
              break;
            default:
              errorMessage = "Please add an action for the new added " +
                "rule type when internal FOLIO module is not available";
              logger.error(errorMessage);
              promise.fail(errorMessage);
          }
        }
      });
    return promise.future();
  }

  private JsonObject buildResetPasswordAction(final String userId, final String password) {
    return new JsonObject()
      .put(ValidatorHelper.REQUEST_PARAM_KEY, password)
      .put(ValidatorHelper.REQUEST_USER_ID_KEY, userId);
  }

  private void prepareResponse(final List<String> errorMessages,
                               final Handler<AsyncResult<JsonObject>> resultHandler) {
    JsonObject validationResult = new JsonObject();
    if (errorMessages.isEmpty()) {
      validationResult.put(ValidatorHelper.RESPONSE_VALIDATION_RESULT_KEY, ValidatorHelper.VALIDATION_VALID_RESULT);
    } else {
      validationResult.put(ValidatorHelper.RESPONSE_VALIDATION_RESULT_KEY, ValidatorHelper.VALIDATION_INVALID_RESULT);
    }
    validationResult.put(ValidatorHelper.RESPONSE_ERROR_MESSAGES_KEY, errorMessages);
    resultHandler.handle(Future.succeededFuture(validationResult));
  }
}

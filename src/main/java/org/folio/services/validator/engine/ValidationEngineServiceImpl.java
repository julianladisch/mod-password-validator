package org.folio.services.validator.engine;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import javafx.util.Pair;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.services.validator.registry.ValidatorRegistryService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.folio.rest.RestVerticle.MODULE_SPECIFIC_ARGS;

/**
 * Implementation of validation engine;
 * calls ValidationRegistry service to obtain rules,
 * runs rules to validate password,
 * pushes validation result in result handler to return.
 *
 * @author Igor Gorchakov
 */
public class ValidationEngineServiceImpl implements ValidationEngineService {

  // OKAPI url header with OKAPI getaway address
  private static String OKAPI_URL_HEADER = "X-Okapi-URL";
  // OKAPI tenant header with tenant id
  private static String OKAPI_TENANT_HEADER = "X-Okapi-Tenant";
  // OKAPI token header contains token to call OKAPI internal modules
  private static String OKAPI_TOKEN_HEADER = "X-Okapi-Token";
  // Logger
  private final Logger logger = LoggerFactory
    .getLogger(ValidationEngineServiceImpl.class);
  // Timeout to wait for response
  private int lookupTimeout = Integer
    .parseInt(MODULE_SPECIFIC_ARGS.getOrDefault("lookup.timeout", "1000"));
  // Repository component to validation obtain rules
  private ValidatorRegistryService validatorRegistryProxy;
  // Http client to call programmatic rules as internal OKAPI endpoints
  private HttpClient httpClient;

  public ValidationEngineServiceImpl(final Vertx vertx) {
    this.validatorRegistryProxy = ValidatorRegistryService
      .createProxy(vertx, ValidatorRegistryService.ADDRESS);
    initHttpClient(vertx);
  }

  private void initHttpClient(final Vertx vertx) {
    HttpClientOptions options = new HttpClientOptions();
    options.setConnectTimeout(lookupTimeout);
    options.setIdleTimeout(lookupTimeout);
    this.httpClient = vertx.createHttpClient(options);
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
  public void validatePassword(String password,
                               Map<String, String> requestHeaders,
                               Handler<AsyncResult<JsonObject>> resultHandler) {
    MultiMap caseInsensitiveHeaders = new CaseInsensitiveHeaders().addAll(requestHeaders);
    String tenantId = caseInsensitiveHeaders.get(OKAPI_TENANT_HEADER);

    validatorRegistryProxy.getAllTenantRules(tenantId, response -> {
      if (response.succeeded()) {

        List<Rule> rules = response.result().mapTo(RuleCollection.class).getRules();

        Future<List<String>> errorMessagesFuture = validatePasswordByRules(rules, password, caseInsensitiveHeaders);
        errorMessagesFuture.setHandler(asyncResult -> {
          if (asyncResult.succeeded()) {
            prepareResponse(asyncResult.result(), resultHandler);
          } else {
            resultHandler.handle(Future.failedFuture(asyncResult.cause().getMessage()));
          }
        });
      }
    });
  }

  private Future<List<String>> validatePasswordByRules(final List<Rule> rules,
                                                       final String password,
                                                       final MultiMap headers) {
    List<String> errorMessages = new ArrayList(rules.size());
    rules.sort(Comparator.comparing(Rule::getOrderNo));

    Future<List<String>> future = Future.future();
    List<Future> programmaticRulesFutures = new ArrayList<>();

    for (Rule rule : rules) {
      if (Rule.Type.REG_EXP.equals(rule.getType())) {
        validatePasswordByRexExpRule(password, rule, errorMessages);
      } else if (Rule.Type.PROGRAMMATIC.equals(rule.getType())) {
        programmaticRulesFutures.add(getValidatePasswordByProgrammaticRuleFuture(password, rule, errorMessages, headers));
      }
    }

    // Notify external method future handler when all programmatic rule futures complete
    CompositeFuture.all(programmaticRulesFutures).setHandler(compositeFutureAsyncResult -> {
      if (compositeFutureAsyncResult.succeeded()) {
        future.complete(errorMessages);
      } else {
        future.fail(compositeFutureAsyncResult.cause().getMessage());
      }
    });
    return future;
  }

  private void validatePasswordByRexExpRule(final String password,
                                            final Rule rule,
                                            final List<String> errorMessages) {
    String expression = rule.getExpression();
    if (!Pattern.compile(expression).matcher(password).matches()) {
      errorMessages.add(rule.getErrMessageId());
    }
  }

  private Future<Pair<Rule, String>> getValidatePasswordByProgrammaticRuleFuture(final String password,
                                                                                 final Rule rule,
                                                                                 final List<String> errorMessages,
                                                                                 final MultiMap headers) {
    String okapiURL = headers.get(OKAPI_URL_HEADER);
    String remoteModuleUrl = okapiURL + rule.getImplementationReference();

    Future<Pair<Rule, String>> future = Future.future();
    HttpClientRequest passwordValidationRequest = httpClient.post(remoteModuleUrl, validationResponse -> {
      HttpResponseStatus responseStatus = HttpResponseStatus.valueOf(validationResponse.statusCode());
      if (responseStatus.equals(responseStatus.OK)) {
        validationResponse.bodyHandler(body -> {
          String validationResult = new JsonObject(body.toString()).getString("Result");
          if ("Invalid".equals(validationResult)) {
            errorMessages.add(rule.getErrMessageId());
          }
          future.complete();
        });
      } else {
        // TODO Inform administrator that remote module is down
        logger.error("Error: FOLIO module by the address " + remoteModuleUrl + " is not available.");
        switch (rule.getValidationType()) {
          case STRONG: {
            String errorMessage = new StringBuilder()
              .append("Programmatic rule ")
              .append(rule.getName())
              .append(" returns status code ")
              .append(validationResponse.statusCode())
              .toString();
            logger.error(errorMessage);
            future.fail(errorMessage);
            break;
          }
          case SOFT: {
            future.complete();
            break;
          }
          default: {
            String errorMessage = "Please add an action for the new added " +
              "rule type when internal FOLIO module is not available";
            logger.error(errorMessage);
            future.fail(errorMessage);
          }
        }
      }
    });
    passwordValidationRequest
      .putHeader(OKAPI_TOKEN_HEADER, headers.get(OKAPI_TOKEN_HEADER))
      .putHeader(OKAPI_TENANT_HEADER, headers.get(OKAPI_TENANT_HEADER))
      .putHeader("Content-Type", "application/json")
      .putHeader("Accept", "application/json")
      .write(new JsonObject().put("password", password).toString())
      .end();
    return future;
  }

  private void prepareResponse(final List<String> errorMessages,
                               final Handler<AsyncResult<JsonObject>> resultHandler) {
    JsonObject validationResult = new JsonObject();
    if (errorMessages.isEmpty()) {
      validationResult.put("result", "Valid");
    } else {
      validationResult.put("result", "Invalid");
      validationResult.put("messages", errorMessages);
    }
    resultHandler.handle(Future.succeededFuture(validationResult));
  }
}

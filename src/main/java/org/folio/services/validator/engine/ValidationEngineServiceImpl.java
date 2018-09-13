package org.folio.services.validator.engine;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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

  private static String OKAPI_URL_HEADER = "X-Okapi-URL";
  private static String OKAPI_TENANT_HEADER = "X-Okapi-Tenant";
  private static String OKAPI_TOKEN_HEADER = "X-Okapi-Token";
  private final Logger logger = LoggerFactory.getLogger(ValidationEngineServiceImpl.class);
  private int lookupTimeout = Integer.parseInt(MODULE_SPECIFIC_ARGS.getOrDefault("lookup.timeout", "1000"));
  private ValidatorRegistryService validatorRegistryProxy;
  private HttpClient httpClient;

  public ValidationEngineServiceImpl(Vertx vertx) {
    this.validatorRegistryProxy = ValidatorRegistryService.createProxy(vertx, ValidatorRegistryService.ADDRESS);
    initHttpClient(vertx);
  }

  private void initHttpClient(Vertx vertx) {
    HttpClientOptions options = new HttpClientOptions();
    options.setConnectTimeout(lookupTimeout);
    options.setIdleTimeout(lookupTimeout);
    this.httpClient = vertx.createHttpClient(options);
  }

  /**
   * Validates received password.
   * Calls ValidationRegistry service to obtain rules specific to tenant,
   * runs rules to validate password,
   * pushes validation result into result handler to return.
   *
   * @param password      received password for validation
   * @param headers       request headers needed for access backend FOLIO services to perform programmatic rules validation
   * @param resultHandler handler with validation results in format <Status, Message>
   */

  @Override
  public void validatePassword(String password, Map<String, String> headers, Handler<AsyncResult<JsonObject>> resultHandler) {
    String tenantId = headers.get(OKAPI_TENANT_HEADER);

    validatorRegistryProxy.getAllTenantRules(tenantId, response -> {
      if (response.succeeded()) {

        List<Rule> rules = response.result().mapTo(RuleCollection.class).getRules();

        List<String> errorMessages = validatePasswordByRules(rules, password, headers);

        formResponse(errorMessages, resultHandler);
      }
    });
  }

  private List<String> validatePasswordByRules(List<Rule> rules, String password, Map<String, String> headers) {
    List<String> errorMessages = new ArrayList(rules);
    rules.sort(Comparator.comparing(Rule::getOrderNo));

    for (Rule rule : rules) {
      if (Rule.Type.REG_EXP.equals(rule.getType())) {
        validatePasswordByRexExpRule(password, rule, errorMessages);
      } else if (Rule.Type.PROGRAMMATIC.equals(rule.getType())) {
        validatePasswordByProgrammaticRule(password, rule, errorMessages, headers);
      }
    }

    return errorMessages;
  }

  private void validatePasswordByRexExpRule(String password, Rule rule, List<String> errorMessages) {
    String expression = rule.getExpression();
    if (!Pattern.compile(expression).matcher(password).matches()) {
      errorMessages.add(rule.getErrMessageId());
    }
  }

  private void validatePasswordByProgrammaticRule(String password, Rule rule, List<String> errorMessages, Map<String, String> headers) {
    String okapiURL = headers.get(OKAPI_URL_HEADER);
    String remoteModuleUrl = okapiURL + rule.getImplementationReference();

    HttpClientRequest passwordValidationRequest = httpClient.post(remoteModuleUrl, validationResponse -> {
      if (validationResponse.statusCode() == 200) {
        validationResponse.bodyHandler(body -> {
          String validationResult = new JsonObject(body.toString()).getString("Result");
          if ("Invalid".equals(validationResult)) {
            errorMessages.add(rule.getErrMessageId());
          }
        });
      } else {
        String errorMessage = new StringBuilder()
          .append("Programmatic rule ")
          .append(rule.getName())
          .append(" returns status code ")
          .append(validationResponse.statusCode())
          .toString();
        logger.error(errorMessage);
      }
    });
    passwordValidationRequest
      .putHeader(OKAPI_TOKEN_HEADER, headers.get(OKAPI_TOKEN_HEADER))
      .putHeader(OKAPI_TENANT_HEADER, headers.get(OKAPI_TENANT_HEADER))
      .putHeader("Content-Type", "application/json")
      .putHeader("Accept", "application/json")
      .write(new JsonObject().put("password", password).toString())
      .end();
  }

  private void formResponse(List<String> errorMessages, Handler<AsyncResult<JsonObject>> resultHandler) {
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

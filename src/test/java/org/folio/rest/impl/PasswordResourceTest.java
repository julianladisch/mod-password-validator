package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.folio.services.validator.engine.ValidationEngineService.PASSWORD_VALIDATION_INVALID_RESULT;
import static org.folio.services.validator.engine.ValidationEngineService.PASSWORD_VALIDATION_VALID_RESULT;
import static org.folio.services.validator.engine.ValidationEngineService.REQUEST_PASSWORD_PARAM_KEY;
import static org.folio.services.validator.engine.ValidationEngineService.RESPONSE_ERROR_MESSAGES_KEY;
import static org.folio.services.validator.engine.ValidationEngineService.RESPONSE_VALIDATION_RESULT_KEY;

@RunWith(VertxUnitRunner.class)
public class PasswordResourceTest {

  public static final String ERR_MESSAGE_ID = "errMessageId";

  private static final JsonObject REGEXP_RULE_ONE_LETTER_ONE_NUMBER = new JsonObject()
    .put("name", "Regexp rule")
    .put("type", "RegExp")
    .put("validationType", "Strong")
    .put("moduleName", "mod-password-validator")
    .put("expression", "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$")
    .put("description", "At least one letter and one number")
    .put(ERR_MESSAGE_ID, "password.validation.error.one-letter-one-number");

  private static final JsonObject REGEXP_RULE_MIN_LENGTH_8 = new JsonObject()
    .put("name", "Regexp rule")
    .put("type", "RegExp")
    .put("validationType", "Strong")
    .put("moduleName", "mod-password-validator")
    .put("expression", "^.{8,}$")
    .put("description", "Minimum eight characters")
    .put(ERR_MESSAGE_ID, "password.validation.error.min-8");

  private static final JsonObject REGEXP_RULE_ENABLED = new JsonObject()
    .put("ruleId", "7ab1c2cd-37ad-4b8f-a514-e78424798e27")
    .put("name", "regexp rule")
    .put("type", "RegExp")
    .put("validationType", "Strong")
    .put("state", "Enabled")
    .put("moduleName", "mod-password-validator")
    .put("expression", "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$")
    .put("description", "At least one letter and one number")
    .put("orderNo", 1)
    .put("errMessageId", "");

  private static final JsonObject REGEXP_RULE_DISABLED = new JsonObject()
    .put("ruleId", "00d2c43c-06fe-450c-af3f-8700cdc28fc5")
    .put("name", "regexp rule")
    .put("type", "RegExp")
    .put("validationType", "Strong")
    .put("state", "Disabled")
    .put("moduleName", "mod-password-validator")
    .put("expression", "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$")
    .put("description", "At least one letter and one number")
    .put("orderNo", 1)
    .put("errMessageId", "");

  private static final JsonObject PROGRAMMATIC_RULE = new JsonObject()
    .put("ruleId", "1ed60281-bfb0-44c4-adb3-3d4e04fba550")
    .put("name", "programmatic rule")
    .put("type", "Programmatic")
    .put("validationType", "Soft")
    .put("state", "Enabled")
    .put("moduleName", "mod-password-validator")
    .put("expression", "")
    .put("implementationReference", "Some implementation")
    .put("description", "Programmatic rule")
    .put("orderNo", 1)
    .put("errMessageId", "");


  private static final String HOST = "http://localhost:";
  private static final String HTTP_PORT = "http.port";
  private static final String ORDER_NO = "orderNo";
  private static final String STATE = "state";
  private static final String VALIDATION_PATH_QUERYABLE = "/password/validators?type=";
  private static final String VALIDATOR_RULE_TYPE_REGEXP = "RegExp";

  private static final String TENANT = "diku";
  private static final String VALIDATION_RULES_TABLE_NAME = "validation_rules";
  private static final String RULE_ID = "ruleId";

  private static Vertx vertx;
  private static int port;


  @Rule
  public Timeout rule = Timeout.seconds(180);  // 3 minutes for loading embedded postgres

  @BeforeClass
  public static void setUpClass(final TestContext context) throws IOException {
    Async async = context.async();
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();

    PostgresClient.setIsEmbedded(true);
    PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    TenantClient tenantClient = new TenantClient("localhost", port, TENANT, TENANT);

    final DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put(HTTP_PORT, port));
    vertx.deployVerticle(RestVerticle.class.getName(), options, res -> {
      try {
        tenantClient.post(null, res2 -> {
          async.complete();
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
  }

  @After
  public void tearDown(TestContext context) throws Exception {
    PostgresClient.getInstance(vertx, TENANT).delete(VALIDATION_RULES_TABLE_NAME, new Criterion(), event -> {
      if (event.failed()) {
        context.fail(event.cause());
      }
    });
  }

  @Test
  public void shouldReturnBadRequestStatusWhenPasswordIsAbsentInBody(TestContext context) {
    Async async = context.async();
    JsonObject emptyBody = new JsonObject();
    validatePassword(emptyBody, 422, (result -> async.complete()))
      .setHandler(chainedRes -> {
        if (chainedRes.failed()) {
          context.fail(chainedRes.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnSuccessfulValidationWhenNoActiveRulesForTargetTenantExists(final TestContext context) {
    Async async = context.async();
    JsonObject passwordBody = new JsonObject().put(REQUEST_PASSWORD_PARAM_KEY, "test");
    JsonObject expectedResponse = new JsonObject().put(RESPONSE_VALIDATION_RESULT_KEY, PASSWORD_VALIDATION_VALID_RESULT)
      .put(RESPONSE_ERROR_MESSAGES_KEY, new JsonArray());

    postRule(REGEXP_RULE_ONE_LETTER_ONE_NUMBER.put(ORDER_NO, 0).put(STATE, "Disabled"), TestUtil.NO_ASSERTS)
      .compose(r -> validatePassword(passwordBody, 200, result -> {
        context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
        context.assertEquals(new JsonObject(result.result().getBody()), expectedResponse);
        async.complete();
      })).setHandler(chainedRes -> {
      if (chainedRes.failed()) {
        context.fail(chainedRes.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void shouldReturnSuccessfulValidationWhenPasswordPassesAllRules(final TestContext context) {
    Async async = context.async();
    JsonObject passwordBody = new JsonObject().put(REQUEST_PASSWORD_PARAM_KEY, "password123");
    JsonObject expectedResponse = new JsonObject().put(RESPONSE_VALIDATION_RESULT_KEY, PASSWORD_VALIDATION_VALID_RESULT)
      .put(RESPONSE_ERROR_MESSAGES_KEY, new JsonArray());

    postRule(REGEXP_RULE_ONE_LETTER_ONE_NUMBER.put(ORDER_NO, 0).put(STATE, "Enabled"), TestUtil.NO_ASSERTS)
      .compose(r -> postRule(REGEXP_RULE_MIN_LENGTH_8.put(ORDER_NO, 1).put(STATE, "Enabled"), TestUtil.NO_ASSERTS))
      .compose(r -> validatePassword(passwordBody, 200, result -> {
        context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
        context.assertEquals(new JsonObject(result.result().getBody()), expectedResponse);
        async.complete();
      })).setHandler(chainedRes -> {
      if (chainedRes.failed()) {
        context.fail(chainedRes.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void shouldReturnFailedValidationResultWithMessageWhenPasswordDidNotPassRule(final TestContext context) {
    Async async = context.async();
    JsonObject passwordBody = new JsonObject().put(REQUEST_PASSWORD_PARAM_KEY, "badPassword");
    String expectedErrorMessageId = REGEXP_RULE_ONE_LETTER_ONE_NUMBER.getString(ERR_MESSAGE_ID);
    JsonObject expectedResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, PASSWORD_VALIDATION_INVALID_RESULT)
      .put(RESPONSE_ERROR_MESSAGES_KEY,
        new JsonArray()
          .add(expectedErrorMessageId)
      );

    postRule(REGEXP_RULE_ONE_LETTER_ONE_NUMBER.put(ORDER_NO, 0).put(STATE, "Enabled"), TestUtil.NO_ASSERTS)
      .compose(r -> postRule(REGEXP_RULE_MIN_LENGTH_8.put(ORDER_NO, 1).put(STATE, "Enabled"), TestUtil.NO_ASSERTS))
      .compose(r -> validatePassword(passwordBody, 200, result -> {
        context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
        context.assertEquals(new JsonObject(result.result().getBody()), expectedResponse);
        async.complete();
      })).setHandler(chainedRes -> {
      if (chainedRes.failed()) {
        context.fail(chainedRes.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void shouldNotReturnProgrammaticOnGetActiveRegExpValidators(final TestContext context) {
    final Async async = context.async();
    postRule(PROGRAMMATIC_RULE, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED))
      .compose(r -> getTenantRulesByType(VALIDATOR_RULE_TYPE_REGEXP, result -> {
        context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
        List<org.folio.rest.jaxrs.model.Rule> rules = new JsonObject(result.result().getBody()).mapTo(RuleCollection.class).getRules();
        context.assertTrue(rules.stream().noneMatch(rule1 -> rule1.getType() == org.folio.rest.jaxrs.model.Rule.Type.PROGRAMMATIC));
      }))
      .setHandler(chainedRes -> {
        if (chainedRes.failed()) {
          context.fail(chainedRes.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldNotReturnDisabledOnGetActiveRegExpValidators(final TestContext context) {
    final Async async = context.async();
    postRule(REGEXP_RULE_DISABLED, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED))
      .compose(r -> getTenantRulesByType(VALIDATOR_RULE_TYPE_REGEXP, result -> {
        context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
        List<org.folio.rest.jaxrs.model.Rule> rules = new JsonObject(result.result().getBody()).mapTo(RuleCollection.class).getRules();
        context.assertTrue(rules.stream().noneMatch(rule1 -> rule1.getState() == org.folio.rest.jaxrs.model.Rule.State.DISABLED));
      }))
      .setHandler(chainedRes -> {
        if (chainedRes.failed()) {
          context.fail(chainedRes.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnRulesOnGetActiveRegExpValidators(final TestContext context) {
    final Async async = context.async();
    postRule(REGEXP_RULE_ENABLED, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED))
      .compose(r -> getTenantRulesByType(VALIDATOR_RULE_TYPE_REGEXP, result -> {
        context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
        List<org.folio.rest.jaxrs.model.Rule> rules = new JsonObject(result.result().getBody()).mapTo(RuleCollection.class).getRules();
        context.assertTrue(rules.stream().allMatch(rule1 -> rule1.getState() == org.folio.rest.jaxrs.model.Rule.State.ENABLED
          && rule1.getType() == org.folio.rest.jaxrs.model.Rule.Type.REG_EXP));
      }))
      .setHandler(chainedRes -> {
        if (chainedRes.failed()) {
          context.fail(chainedRes.cause());
        } else {
          async.complete();
        }
      });
  }

  @Ignore
  @Test
  public void shouldReturnAllActiveTenantRulesWhenTypeNotSpecified(final TestContext context) {
    final Async async = context.async();
    postRule(PROGRAMMATIC_RULE, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED))
      .compose(r -> postRule(REGEXP_RULE_DISABLED, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED)))
      .compose(r -> postRule(REGEXP_RULE_ENABLED, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED)))
      .compose(r -> getTenantRulesByType("", result -> {
        context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
        RuleCollection collection = new JsonObject(result.result().getBody()).mapTo(RuleCollection.class);
        context.assertTrue(collection.getTotalRecords() == 2);
        List<org.folio.rest.jaxrs.model.Rule> rules = collection.getRules();
        context.assertTrue(rules.size() == 2);
        context.assertTrue(rules.stream().filter(rule1 ->
          rule1.getName().equals(PROGRAMMATIC_RULE.getString("name"))).collect(Collectors.toList()).size() == 1);
        context.assertTrue(rules.stream().filter(rule1 ->
          rule1.getName().equals(REGEXP_RULE_ENABLED.getString("name"))).collect(Collectors.toList()).size() == 1);
      }))
      .setHandler(chainedRes -> {
        if (chainedRes.failed()) {
          context.fail(chainedRes.cause());
        } else {
          async.complete();
        }
      });
  }

  private Future<TestUtil.WrappedResponse> postRule(JsonObject rule,
                                                    Handler<AsyncResult<TestUtil.WrappedResponse>> handler) {
    return TestUtil.doRequest(vertx, HOST + port + "/tenant/rules", HttpMethod.POST, null, rule.toString(),
      201, "Adding new rule", handler);
  }

  private Future<TestUtil.WrappedResponse> validatePassword(JsonObject password, int expectedCode,
                                                            Handler<AsyncResult<TestUtil.WrappedResponse>> handler) {
    return TestUtil.doRequest(vertx, HOST + port + "/password/validate", HttpMethod.POST, null,
      password.toString(), expectedCode, "Validating password", handler);
  }

  private Future<TestUtil.WrappedResponse> getTenantRulesByType(String type, Handler<AsyncResult<TestUtil.WrappedResponse>> handler) {
    return TestUtil.doRequest(vertx, HOST + port + VALIDATION_PATH_QUERYABLE + type, HttpMethod.GET, null, null,
      HttpStatus.SC_OK, "Getting enabled rules by type", handler);
  }
}

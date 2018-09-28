package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.folio.services.validator.util.ValidatorHelper.REQUEST_PARAM_KEY;
import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_ERROR_MESSAGES_KEY;
import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_VALIDATION_RESULT_KEY;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_INVALID_RESULT;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_VALID_RESULT;

@RunWith(VertxUnitRunner.class)
public class PasswordResourceTest {

  public static final String ERR_MESSAGE_ID = "errMessageId";

  private static final JsonObject REGEXP_RULE_ONE_LETTER_ONE_NUMBER = new JsonObject()
    .put("name", "Regexp rule")
    .put("type", "RegExp")
    .put("validationType", "Strong")
    .put("moduleName", "mod-password-validator")
    .put("expression", "^(?=.*[A-Za-z])(?=.*\\d).+$")
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


  private static final String HOST = "http://localhost:";
  private static final String HTTP_PORT = "http.port";
  private static final String ORDER_NO = "orderNo";
  private static final String STATE = "state";

  private static final String OKAPI_URL_HEADER = "x-okapi-url";
  private static final String TENANT = "diku";
  private static final String VALIDATION_RULES_TABLE_NAME = "validation_rules";

  private static Vertx vertx;
  private static int port;
  private static int userMockPort;
  private static String useExternalDatabase;

  @Rule
  public Timeout rule = Timeout.seconds(180);  // 3 minutes for loading embedded postgres

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
    Async async = context.async();
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();
    userMockPort = NetworkUtils.nextFreePort();

    useExternalDatabase = System.getProperty(
      "org.folio.password.validator.test.database",
      "embedded");

    switch(useExternalDatabase) {
      case "environment":
        System.out.println("Using environment settings");
        break;
      case "external":
        String postgresConfigPath = System.getProperty(
          "org.folio.password.validator.test.config",
          "/postgres-conf-local.json");
        PostgresClient.setConfigFilePath(postgresConfigPath);
        break;
      case "embedded":
        PostgresClient.setIsEmbedded(true);
        PostgresClient.getInstance(vertx).startEmbeddedPostgres();
        break;
      default:
        String message = "No understood database choice made." +
          "Please set org.folio.password.validator.test.database" +
          "to 'external', 'environment' or 'embedded'";
        throw new Exception(message);
    }

    TenantClient tenantClient = new TenantClient("localhost", port, TENANT, TENANT);

    final DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put(HTTP_PORT, port));
    DeploymentOptions userMockOptions = new DeploymentOptions()
      .setConfig(new JsonObject().put("port", userMockPort)).setWorker(true);
    vertx.deployVerticle(UserMock.class.getName(), userMockOptions, mockRes -> {
      if (mockRes.failed()) {
        mockRes.cause().printStackTrace();
        context.fail(mockRes.cause());
      } else {
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
    });

  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      if(useExternalDatabase.equals("embedded")) {
        PostgresClient.stopEmbeddedPostgres();
      }
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
  public void shouldReturnSuccessfulValidationWhenPasswordPassesAllRules(final TestContext context) {
    Async async = context.async();
    JsonObject passwordBody = new JsonObject().put(REQUEST_PARAM_KEY, "P@sword12");
    JsonObject expectedResponse = new JsonObject().put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT)
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
    JsonObject passwordBody = new JsonObject().put(REQUEST_PARAM_KEY, "badPassword");
    String expectedErrorMessageId = REGEXP_RULE_ONE_LETTER_ONE_NUMBER.getString(ERR_MESSAGE_ID);
    JsonObject expectedResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_INVALID_RESULT)
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

  private Future<TestUtil.WrappedResponse> postRule(JsonObject rule,
                                                    Handler<AsyncResult<TestUtil.WrappedResponse>> handler) {
    return TestUtil.doRequest(vertx, HOST + port + "/tenant/rules", HttpMethod.POST, null, rule.toString(),
      201, "Adding new rule", handler);
  }

  private Future<TestUtil.WrappedResponse> validatePassword(JsonObject password, int expectedCode,
                                                            Handler<AsyncResult<TestUtil.WrappedResponse>> handler) {
    CaseInsensitiveHeaders headers = new CaseInsensitiveHeaders();
    headers.add(OKAPI_USERID_HEADER, UserMock.ADMIN_ID);
    headers.add(OKAPI_URL_HEADER, HOST + userMockPort);
    headers.add(OKAPI_HEADER_TOKEN, "token");
    return TestUtil.doRequest(vertx, HOST + port + "/validate", HttpMethod.POST, headers,
      password.toString(), expectedCode, "Validating password", handler);
  }

}

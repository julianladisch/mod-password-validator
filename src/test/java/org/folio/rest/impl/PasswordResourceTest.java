package org.folio.rest.impl;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
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
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.IOException;

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


  private static final String HOST = "http://localhost:";
  private static final String HTTP_PORT = "http.port";
  private static final String ORDER_NO = "orderNo";
  private static final String STATE = "state";
  private static final String RESULT = "result";
  private static final String VALID = "Valid";
  private static final String MESSAGES = "messages";
  private static final String INVALID = "Invalid";


  private static Vertx vertx;
  private static int port;


  @Rule
  public Timeout rule = Timeout.seconds(180);  // 3 minutes for loading embedded postgres

  @BeforeClass
  public static void setUp(final TestContext context) throws IOException {
    Async async = context.async();
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();

    PostgresClient.setIsEmbedded(true);
    PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    TenantClient tenantClient = new TenantClient("localhost", port, "diku", "diku");

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
  public static void tearDown(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
  }

  @Ignore
  @Test
  public void shouldReturnBadRequestStatusWhenPasswordIsAbsentInBody(TestContext context) {
    Async async = context.async();
    JsonObject emptyBody = new JsonObject();
    validatePassword(emptyBody, 422)
      .setHandler(response -> {
        if (response.failed()) {
          context.fail(response.cause());
        } else {
          async.complete();
        }
      });
  }

  @Ignore
  @Test
  public void shouldReturnSuccessfulValidationWhenNoActiveRulesForTargetTenantExists(final TestContext context) {
    Async async = context.async();
    JsonObject passwordBody = new JsonObject().put("password", "test");
    JsonObject expectedResponse = new JsonObject().put(RESULT, VALID).put(MESSAGES, new JsonArray());

    postRule(REGEXP_RULE_ONE_LETTER_ONE_NUMBER.put(ORDER_NO, 0).put(STATE, "Disabled"))
      .compose(w -> validatePassword(passwordBody, 200)
        .setHandler(result -> {
          context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
          context.assertEquals(new JsonObject(result.result().getBody()), expectedResponse);
        })
      )
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
  public void shouldReturnSuccessfulValidationWhenPasswordPassesAllRules(final TestContext context) {
    Async async = context.async();
    JsonObject passwordBody = new JsonObject().put("password", "password123");
    JsonObject expectedResponse = new JsonObject().put(RESULT, VALID).put(MESSAGES, new JsonArray());

    postRule(REGEXP_RULE_ONE_LETTER_ONE_NUMBER.put(ORDER_NO, 0).put(STATE, "Enabled"))
      .compose(w -> postRule(REGEXP_RULE_MIN_LENGTH_8.put(ORDER_NO, 1).put(STATE, "Enabled")))
      .compose(w -> validatePassword(passwordBody, 200)
        .setHandler(result -> {
          context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
          context.assertEquals(new JsonObject(result.result().getBody()), expectedResponse);
        })
      )
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
  public void shouldReturnFailedValidationResultWithMessageWhenPasswordDidNotPassRule(final TestContext context) {
    Async async = context.async();
    JsonObject passwordBody = new JsonObject().put("password", "badPassword");
    String expectedErrorMessageId = REGEXP_RULE_ONE_LETTER_ONE_NUMBER.getString(ERR_MESSAGE_ID);
    JsonObject expectedResponse = new JsonObject()
      .put(RESULT, INVALID)
      .put(MESSAGES,
        new JsonArray()
          .add(expectedErrorMessageId)
      );

    postRule(REGEXP_RULE_ONE_LETTER_ONE_NUMBER.put(ORDER_NO, 0).put(STATE, "Enabled"))
      .compose(w -> postRule(REGEXP_RULE_MIN_LENGTH_8.put(ORDER_NO, 1).put(STATE, "Enabled")))
      .compose(w -> validatePassword(passwordBody, 200)
        .setHandler(result -> {
          context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
          context.assertEquals(new JsonObject(result.result().getBody()), expectedResponse);
        })
      )
      .setHandler(chainedRes -> {
        if (chainedRes.failed()) {
          context.fail(chainedRes.cause());
        } else {
          async.complete();
        }
      });
  }

  private Future<TestUtil.WrappedResponse> postRule(JsonObject rule) {
    return TestUtil.doRequest(vertx, HOST + port + "/tenant/rules", HttpMethod.POST, null, rule.toString(),
      201, "Adding new rule");
  }

  private Future<TestUtil.WrappedResponse> validatePassword(JsonObject password, int expectedCode) {
    return TestUtil.doRequest(vertx, HOST + port + "/password/validate", HttpMethod.POST, null,
      password.toString(), expectedCode, "Validating password");
  }
}

package org.folio.rest.impl;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
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

  private static final JsonObject REGEXP_RULE_ONE_LETTER_ONE_NUMBER = new JsonObject()
    .put("name", "Regexp rule")
    .put("type", "RegExp")
    .put("validationType", "Strong")
    .put("moduleName", "mod-password-validator")
    .put("expression", "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$")
    .put("description", "At least one letter and one number")
    .put("errMessageId", "not_valid_letter_and_number");

  private static final JsonObject REGEXP_RULE_MIN_LENGTH_8 = new JsonObject()
    .put("name", "Regexp rule")
    .put("type", "RegExp")
    .put("validationType", "Strong")
    .put("moduleName", "mod-password-validator")
    .put("expression", "^.{8,}$")
    .put("description", "Minimum eight characters")
    .put("errMessageId", "not_valid_regexp");

  private static final String VALIDATION_PATH_QUERYABLE = "/password/validators?type=";
  private static final String VALIDATION_PATH = "/password/validate";
  private static final String HEADER_X_OKAPI_TENANT = "x-okapi-tenant";
  private static final String HEADER_CONTENT_TYPE = "content-type";
  private static final String HEADER_ACCEPT = "Accept";
  private static final String ACCEPT_VALUES = "application/json, text/plain";
  private static final String APPLICATION_JSON = "application/json";
  private static final String HOST = "http://localhost:";
  private static final String HTTP_PORT = "http.port";
  private static final String TENANT = "diku";
  private static final String VALIDATOR_RULE_TYPE_STRONG = "strong";

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

  @Test
  public void shouldReturnBadRequestStatusWhenPasswordIsAbsentInBody(TestContext context) {
    Async async = context.async();
    String url = HOST + port;
    String validatorsUrl = url + VALIDATION_PATH;

    Handler<HttpClientResponse> checkingHandler = response -> {
      context.assertEquals(response.statusCode(), HttpStatus.SC_BAD_REQUEST);
      async.complete();
    };
    JsonObject body = new JsonObject();
    sendRequest(validatorsUrl, HttpMethod.GET, checkingHandler, body.toString());
  }

  @Ignore
  @Test
  public void shouldReturnSuccessfulValidationWhenNoActiveRulesForTargetTenant(final TestContext context) {
    Async async = context.async();
    JsonObject passwordBody = new JsonObject().put("password", "test");
    JsonObject expectedResponse = new JsonObject().put("result", "Valid").put("messages", new JsonArray());

    postRule(REGEXP_RULE_ONE_LETTER_ONE_NUMBER.put("orderNo", 0).put("state", "Disabled"))
      .compose(w -> validatePassword(passwordBody)
        .setHandler(result -> {
          context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
          context.assertEquals(new JsonObject(result.result().getBody()), expectedResponse);
        })
      )
    .setHandler(chainedRes -> {
      if(chainedRes.failed()) {
        context.fail(chainedRes.cause());
      } else {
        async.complete();
      }
    });
  }


  @Test
  public void shouldReturnValidatorsWithStrongRuleType(final TestContext context) {
    //TODO Replace testing stub
    final Async async = context.async();
    final String url = HOST + port;
    final String validatorsUrl = url + VALIDATION_PATH_QUERYABLE + VALIDATOR_RULE_TYPE_STRONG;

    final Handler<HttpClientResponse> handler = response -> {
      context.assertEquals(response.statusCode(), HttpStatus.SC_OK);
      context.assertEquals(response.headers().get(HEADER_CONTENT_TYPE), APPLICATION_JSON);
      response.bodyHandler(body -> {
        context.assertTrue(body.toJsonObject().equals(getRuleCollectionStub()));
        async.complete();
      });
    };
    sendRequest(validatorsUrl, HttpMethod.GET, handler);
  }

  private void sendRequest(final String url, final HttpMethod method, final Handler<HttpClientResponse> handler) {
    sendRequest(url, method, handler, "");
  }

  private void sendRequest(final String url, final HttpMethod method, final Handler<HttpClientResponse> handler, final String content) {
    final Buffer buffer = Buffer.buffer(content);
    vertx.createHttpClient()
      .requestAbs(method, url, handler)
      .putHeader(HEADER_X_OKAPI_TENANT, TENANT)
      .putHeader(HEADER_ACCEPT, ACCEPT_VALUES)
      .putHeader(HEADER_CONTENT_TYPE, APPLICATION_JSON)
      .end(buffer);
  }

  private JsonObject getRuleCollectionStub() {
    return new JsonObject()
      .put("rules", new JsonArray())
      .put("totalRecords", 0);
  }

  private Future<TestUtil.WrappedResponse> postRule(JsonObject rule) {
    return TestUtil.doRequest(vertx, HOST + port + "/tenant/rules", HttpMethod.POST, null, rule.toString(),
      201, "Adding new rule");
  }

  private Future<TestUtil.WrappedResponse> validatePassword(JsonObject password) {
    return TestUtil.doRequest(vertx, HOST + port + "/password/validate", HttpMethod.POST, null,
      password.toString(), 200, "Validating password");
  }
}

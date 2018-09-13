package org.folio.rest.impl;

import io.vertx.core.DeploymentOptions;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(VertxUnitRunner.class)
public class PasswordResourceTest {
  private static final String POST_VALIDATION_CREDENTIALS = "{\"password\":\"12345\"};";
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
  private Vertx vertx;
  private int port;

  @Rule
  public Timeout rule = Timeout.seconds(180);  // 3 minutes for loading embedded postgres

  @Before
  public void setUp(final TestContext context) throws IOException {
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

  @After
  public void tearDown(final TestContext context) {
    vertx.close(context.asyncAssertSuccess());
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
        System.out.println(body.toJsonObject());
        System.out.println(getRuleCollectionStub());
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

  private JsonObject getPasswordValidationStub() {
    return new JsonObject().put("result", "Valid").put("messages", new JsonArray());
  }
}

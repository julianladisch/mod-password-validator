package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Map;

public class TestUtil {

  private static final Logger logger = LoggerFactory.getLogger(TestUtil.class);

  public static final Handler<AsyncResult<WrappedResponse>> NO_ASSERTS = x -> {
  };

  static class WrappedResponse {
    private String explanation;
    private int code;
    private String body;
    private JsonObject json;
    private HttpClientResponse response;

    public WrappedResponse(String explanation, int code, String body,
                           HttpClientResponse response) {
      this.explanation = explanation;
      this.code = code;
      this.body = body;
      this.response = response;
      try {
        json = new JsonObject(body);
      } catch (Exception e) {
        json = null;
      }
    }

    public String getExplanation() {
      return explanation;
    }

    public int getCode() {
      return code;
    }

    public String getBody() {
      return body;
    }

    public HttpClientResponse getResponse() {
      return response;
    }

    public JsonObject getJson() {
      return json;
    }
  }

  public static Future<WrappedResponse> doRequest(Vertx vertx, String url,
                                                  HttpMethod method, CaseInsensitiveHeaders headers, String payload,
                                                  Integer expectedCode, String explanation) {
    return doRequest(vertx, url, method, headers, payload, expectedCode, explanation, NO_ASSERTS);
  }

  public static Future<WrappedResponse> doRequest(Vertx vertx, String url,
                                                  HttpMethod method, CaseInsensitiveHeaders headers, String payload,
                                                  Integer expectedCode, String explanation,
                                                  Handler<AsyncResult<WrappedResponse>> handler) {
    Future<WrappedResponse> future = Future.future();
    HttpClient client = vertx.createHttpClient();
    HttpClientRequest request = client.requestAbs(method, url);
    //Add standard headers
    request.putHeader("X-Okapi-Tenant", "diku")
      .putHeader("content-type", "application/json")
      .putHeader("accept", "application/json");
    if (headers != null) {
      for (Map.Entry entry : headers.entries()) {
        request.putHeader((String) entry.getKey(), (String) entry.getValue());
        logger.debug("Adding header {} with value {}", entry.getKey(), entry.getValue());
      }
    }
    //standard exception handler
    request.exceptionHandler(future::fail);
    request.handler(req -> {
      req.bodyHandler(buf -> {
        String explainString = "(no explanation)";
        if (explanation != null) {
          explainString = explanation;
        }
        if (expectedCode != null && expectedCode != req.statusCode()) {
          future.fail(method.toString() + " to " + url + " failed. Expected status code "
            + expectedCode + ", got status code " + req.statusCode() + ": "
            + buf.toString() + " | " + explainString);
        } else {
          logger.debug("Got status code {} with payload of: {} | {}", req.statusCode(), buf.toString(), explainString);
          WrappedResponse wr = new WrappedResponse(explanation, req.statusCode(), buf.toString(), req);
          handler.handle(Future.succeededFuture(wr));
          future.complete(wr);
        }
      });
    });
    logger.debug("Sending {} request to url '{} with payload: {} '\n", method.toString(), url, payload);
    if (method == HttpMethod.PUT || method == HttpMethod.POST) {
      request.end(payload);
    } else {
      request.end();
    }
    return future;
  }
}

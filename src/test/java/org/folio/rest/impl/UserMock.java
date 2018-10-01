package org.folio.rest.impl;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;


public class UserMock extends AbstractVerticle {

  public static final String ADMIN_ID = "8bd684c1-bbc3-4cf1-bcf4-8013d02a94ce";

  private JsonObject admin = new JsonObject()
    .put("username", "admin")
    .put("id", ADMIN_ID)
    .put("active", true);
  private JsonObject responseAdmin = new JsonObject()
    .put("users", new JsonArray()
      .add(admin))
    .put("totalRecords", 1);

  public void start(Future<Void> future) {
    final int port = context.config().getInteger("port");

    Router router = Router.router(vertx);
    HttpServer server = vertx.createHttpServer();

    router.route("/users").handler(this::handleUsers);
    System.out.println("Running UserMock on port " + port);
    server.requestHandler(router::accept).listen(port, result -> {
      if (result.failed()) {
        future.fail(result.cause());
      } else {
        future.complete();
      }
    });
  }

  private void handleUsers(RoutingContext context) {
    try {
      String query = context.request().getParam("query");
      if (query.equals("username==admin")) {
        context.response()
          .setStatusCode(200)
          .end(responseAdmin.encode());
      } else if (query.equals("id==" + ADMIN_ID)) {
        context.response()
          .setStatusCode(200)
          .end(responseAdmin.encode());
      } else {
        context.response()
          .setStatusCode(404)
          .end("Not found");
      }
    } catch (Exception e) {
      context.response()
        .setStatusCode(500)
        .end("Error");
    }
  }
}



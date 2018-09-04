package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.folio.rest.jaxrs.resource.PasswordResource;
import javax.ws.rs.core.Response;
import java.util.Map;

public class PasswordResourceImpl implements PasswordResource {

  private static final String VALIDATION_STUB_PATH = "ramls/examples/password_output.sample";
  private static final String RULES_STUB_PATH = "ramls/examples/ruleCollection.sample";
  private static final String HEADER_CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_JSON = "application/json";

  @Override
  public void postPasswordValidate(final Map<String, String> okapiHeaders,
                                   final Handler<AsyncResult<Response>> asyncResultHandler,
                                   final Context vertxContext) {
    //TODO replace stub response
    vertxContext.owner().fileSystem().readFile(VALIDATION_STUB_PATH, event -> {
      if (event.succeeded()) {
        asyncResultHandler.handle(
          Future.succeededFuture(
            Response.ok(event.result().toString())
              .header(HEADER_CONTENT_TYPE, APPLICATION_JSON).build()
          ));
      } else {
        asyncResultHandler.handle(Future.succeededFuture(PostPasswordValidateResponse
          .withPlainBadRequest("The password is invalid")));
      }
    });
  }

  @Override
  public void getPasswordValidators(final String type, final Map<String, String> okapiHeaders,
                                    final Handler<AsyncResult<Response>> asyncResultHandler,
                                    final Context vertxContext) {
    //TODO replace stub response
    vertxContext.owner().fileSystem().readFile(RULES_STUB_PATH, event -> {
      if (event.succeeded()) {
        asyncResultHandler.handle(
          Future.succeededFuture(
            Response.ok(event.result().toString())
              .header(HEADER_CONTENT_TYPE, APPLICATION_JSON)
              .build()
          ));
      } else {
        asyncResultHandler.handle(Future.succeededFuture(GetPasswordValidatorsResponse
          .withPlainNotFound("Validators not found")));
      }
    });
  }
}

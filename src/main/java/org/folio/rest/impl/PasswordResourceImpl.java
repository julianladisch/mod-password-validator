package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.PasswordJson;
import org.folio.rest.jaxrs.model.ValidationTemplateJson;
import org.folio.rest.jaxrs.resource.PasswordResource;
import org.folio.services.validator.engine.ValidationEngineService;

import javax.ws.rs.core.Response;
import java.util.Map;

public class PasswordResourceImpl implements PasswordResource {

  private static final String RULES_STUB_PATH = "ramls/examples/ruleCollection.sample";
  private static final String HEADER_CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_JSON = "application/json";

  private final Logger logger = LoggerFactory.getLogger(PasswordResourceImpl.class);

  private ValidationEngineService validationEngineProxy;

  public PasswordResourceImpl() {
  }

  public PasswordResourceImpl(Vertx vertx, String tenantId) {
    validationEngineProxy =
      ValidationEngineService.createProxy(vertx, ValidationEngineService.ADDRESS);
  }

  @Override
  public void postPasswordValidate(final PasswordJson entity,
                                   final Map<String, String> okapiHeaders,
                                   final Handler<AsyncResult<Response>> asyncResultHandler,
                                   final Context vertxContext) {
    validationEngineProxy.validatePassword(entity.getPassword(), okapiHeaders, result -> {
      Response response;
      if (result.succeeded()) {
        response = PostPasswordValidateResponse.withJsonOK(result.result().mapTo(ValidationTemplateJson.class));
      } else {
        String errorMessage = "Failed to validate password: " + result.cause().getLocalizedMessage();
        logger.error(errorMessage, result.cause());
        response = PostPasswordValidateResponse.withPlainInternalServerError(result.cause().getLocalizedMessage());
      }
      asyncResultHandler.handle(Future.succeededFuture(response));
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
          .withPlainInternalServerError("Validators not found")));
      }
    });
  }
}

package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.PasswordJson;
import org.folio.rest.jaxrs.model.ValidationTemplateJson;
import org.folio.rest.jaxrs.resource.PasswordResource;
import org.folio.services.validator.engine.ValidationEngineService;
import org.folio.services.validator.util.ValidatorHelper;

import javax.ws.rs.core.Response;
import java.util.Map;

public class ValidateResourceImpl implements PasswordResource {

  private final Logger logger = LoggerFactory.getLogger(ValidateResourceImpl.class);

  @Override
  public void postPasswordValidate(PasswordJson entity,
                           Map<String, String> okapiHeaders,
                           Handler<AsyncResult<Response>> asyncResultHandler,
                           Context vertxContext) throws Exception {
    try {
      ValidationEngineService validationEngineProxy =
        ValidationEngineService.createProxy(vertxContext.owner(), ValidatorHelper.VALIDATOR_ENGINE_ADDRESS);
      validationEngineProxy.validatePassword(entity.getPassword(), okapiHeaders, result -> {
        Response response;
        if (result.succeeded()) {
          response = PostPasswordValidateResponse.withJsonOK(result.result().mapTo(ValidationTemplateJson.class));
        } else {
          String errorMessage = "Failed to validate password: " + result.cause().getLocalizedMessage();
          logger.error(errorMessage, result.cause());
          response = PostPasswordValidateResponse.withPlainInternalServerError(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
        }
        asyncResultHandler.handle(Future.succeededFuture(response));
      });
    } catch (Exception e) {
      logger.error("Failed to validate password: " + e.getLocalizedMessage(), e);
      asyncResultHandler.handle(Future.succeededFuture(
        PostPasswordValidateResponse.withPlainInternalServerError(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }

  //Workaround for raml module builder bug RMB-1 (fixed in raml-module-builder version 20.0.0)
  @Override
  public void getPasswordValidate(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
    asyncResultHandler.handle(Future.succeededFuture(GetPasswordValidateResponse.withPlainOK("stub")));
  }
}

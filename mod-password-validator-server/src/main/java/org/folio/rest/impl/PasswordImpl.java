package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.ValidationTemplate;
import org.folio.rest.jaxrs.resource.Password;
import org.folio.services.validator.engine.ValidationEngineService;
import org.folio.services.validator.util.ValidatorHelper;

import javax.ws.rs.core.Response;
import java.util.Map;

public class PasswordImpl implements Password {

  private final Logger logger = LoggerFactory.getLogger(PasswordImpl.class);

  @Override
  public void postPasswordValidate(org.folio.rest.jaxrs.model.Password entity,
                                   Map<String, String> okapiHeaders,
                                   Handler<AsyncResult<Response>> asyncResultHandler,
                                   Context vertxContext) {
    try {
      ValidationEngineService validationEngineProxy =
        ValidationEngineService.createProxy(vertxContext.owner(), ValidatorHelper.VALIDATOR_ENGINE_ADDRESS);
      validationEngineProxy.validatePassword(entity.getUserId(), entity.getPassword(), okapiHeaders, result -> {
        Response response;
        if (result.succeeded()) {
          response = PostPasswordValidateResponse.respond200WithApplicationJson(result.result().mapTo(ValidationTemplate.class));
        } else {
          String errorMessage = "Failed to validate password: " + result.cause().getLocalizedMessage();
          logger.error(errorMessage, result.cause());
          response = PostPasswordValidateResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
        }
        asyncResultHandler.handle(Future.succeededFuture(response));
      });
    } catch (Exception e) {
      logger.error("Failed to validate password: " + e.getLocalizedMessage(), e);
      asyncResultHandler.handle(Future.succeededFuture(
        PostPasswordValidateResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }
}

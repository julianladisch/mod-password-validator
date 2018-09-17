package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.PasswordJson;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.rest.jaxrs.model.ValidationTemplateJson;
import org.folio.rest.jaxrs.resource.PasswordResource;
import org.folio.services.validator.engine.ValidationEngineService;
import org.folio.services.validator.registry.ValidatorRegistryService;
import org.folio.services.validator.util.ValidatorHelper;

import javax.ws.rs.core.Response;
import java.util.Map;

public class PasswordResourceImpl implements PasswordResource {

  private final Logger logger = LoggerFactory.getLogger(PasswordResourceImpl.class);

  private ValidatorRegistryService validatorRegistryService;
  private ValidationEngineService validationEngineProxy;
  private String tenantId;

  public PasswordResourceImpl() {
  }

  public PasswordResourceImpl(Vertx vertx, String tenantId) {
    this.tenantId = tenantId;
    validationEngineProxy =
      ValidationEngineService.createProxy(vertx, ValidatorHelper.VALIDATOR_ENGINE_ADDRESS);
    validatorRegistryService = ValidatorRegistryService.createProxy(vertx, ValidatorHelper.REGISTRY_SERVICE_ADDRESS);
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
  public void getPasswordValidators(final String type,
                                    final Map<String, String> okapiHeaders,
                                    final Handler<AsyncResult<Response>> asyncResultHandler,
                                    final Context vertxContext) throws Exception {
    try {
      vertxContext.runOnContext(v -> validatorRegistryService.getActiveRulesByType(tenantId, type, reply -> {
        if (reply.succeeded()) {
          RuleCollection rules = reply.result().mapTo(RuleCollection.class);
          asyncResultHandler.handle(
            Future.succeededFuture(GetPasswordValidatorsResponse.withJsonOK(rules)));
        } else {
          logger.error("Failed to get password validators", reply.cause());
          asyncResultHandler.handle(
            Future.succeededFuture(GetPasswordValidatorsResponse.withPlainInternalServerError("Failed to get password validators")));
        }
      }));
    } catch (Exception e) {
      logger.error("Error running on verticle for getPasswordValidators: " + e.getMessage(), e.getCause());
      asyncResultHandler.handle(Future.succeededFuture(
        GetPasswordValidatorsResponse.withPlainInternalServerError(
          Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }
}

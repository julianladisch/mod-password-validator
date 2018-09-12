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

import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.validator.registry.ValidatorRegistryService;

import javax.ws.rs.core.Response;
import java.util.Map;

public class PasswordResourceImpl implements PasswordResource {

  private final Logger logger = LoggerFactory.getLogger(PasswordResourceImpl.class);

  private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
  private static final String INTERNAL_ERROR = "Internal Server error";

  private ValidatorRegistryService validatorRegistryService;
  private ValidationEngineService validationEngineProxy;

  public PasswordResourceImpl() {
  }

  public PasswordResourceImpl(Vertx vertx, String tenantId) {
    validationEngineProxy =
      ValidationEngineService.createProxy(vertx, ValidationEngineService.ADDRESS);
    validatorRegistryService = ValidatorRegistryService.createProxy(vertx, ValidatorRegistryService.ADDRESS);
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
      vertxContext.runOnContext(v -> {

        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        validatorRegistryService.getActiveRulesByType(tenantId, type, reply -> {
          if(reply.succeeded()){
            RuleCollection rules = reply.result().mapTo(RuleCollection.class);
            asyncResultHandler.handle(
              Future.succeededFuture(GetPasswordValidatorsResponse.withJsonOK(rules)));
          } else {
            asyncResultHandler.handle(
              Future.succeededFuture(GetPasswordValidatorsResponse.withPlainInternalServerError(reply.cause().getMessage())));
          }
        });
      });
    } catch (Exception e) {
      logger.debug("Error running on verticle for getTenantRulesByRuleId: " + e.getMessage());
      asyncResultHandler.handle(Future.succeededFuture(GetPasswordValidatorsResponse.withPlainInternalServerError(INTERNAL_ERROR)));
    }
  }
}

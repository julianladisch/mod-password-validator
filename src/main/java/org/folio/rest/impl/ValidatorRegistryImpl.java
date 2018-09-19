package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.rest.jaxrs.resource.TenantRulesResource;
import org.folio.services.validator.registry.ValidatorRegistryService;
import org.folio.services.validator.util.ValidatorHelper;

import javax.ws.rs.core.Response;
import java.util.Map;

public class ValidatorRegistryImpl implements TenantRulesResource {

  private final Logger logger = LoggerFactory.getLogger(ValidatorRegistryImpl.class);

  private static final String ORDER_NUMBER_ERROR = "Order number cannot be negative";
  private static final String VALIDATION_TYPE_ERROR = "In case of RegExp rule Validation Type can only be Strong";
  private static final String IMPLEMENTATION_REFERENCE_REQUIRED_ERROR = "In case of Programmatic rule Implementation reference should be provided";

  private final ValidatorRegistryService validatorRegistryService;
  private String tenantId;

  public ValidatorRegistryImpl(Vertx vertx, String tenantId) {
    this.tenantId = tenantId;
    this.validatorRegistryService = ValidatorRegistryService.createProxy(vertx, ValidatorHelper.REGISTRY_SERVICE_ADDRESS);
  }

  @Override
  public void getTenantRules(final Map<String, String> okapiHeaders,
                             final Handler<AsyncResult<Response>> asyncResultHandler,
                             final Context vertxContext) throws Exception {
    try {
      vertxContext.runOnContext(v -> validatorRegistryService.getAllTenantRules(tenantId, reply -> {
        if (reply.succeeded()) {
          RuleCollection rules = reply.result().mapTo(RuleCollection.class);
          asyncResultHandler.handle(
            Future.succeededFuture(GetTenantRulesResponse.withJsonOK(rules)));
        } else {
          logger.error("Failed to get all tenant rules", reply.cause());
          asyncResultHandler.handle(
            Future.succeededFuture(GetTenantRulesResponse.withPlainInternalServerError("Failed to get all tenant rules")));
        }
      }));
    } catch (Exception e) {
      logger.error("Error running on verticle for getTenantRules: " + e.getMessage(), e);
      asyncResultHandler.handle(Future.succeededFuture(
        GetTenantRulesResponse.withPlainInternalServerError(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }

  @Override
  public void postTenantRules(final Rule entity,
                              final Map<String, String> okapiHeaders,
                              final Handler<AsyncResult<Response>> asyncResultHandler,
                              final Context vertxContext) throws Exception {
    try {
      vertxContext.runOnContext(v -> {
        String errorMessage = validateRule(entity);
        if (errorMessage != null) {
          asyncResultHandler.handle(
            Future.succeededFuture(PostTenantRulesResponse.withPlainBadRequest(errorMessage)));
        } else {
          validatorRegistryService.createTenantRule(tenantId, JsonObject.mapFrom(entity), reply -> {
            if (reply.succeeded()) {
              asyncResultHandler.handle(
                Future.succeededFuture(PostTenantRulesResponse.withJsonCreated(reply.result().mapTo(Rule.class))));
            } else {
              logger.error("Failed to create new rule", reply.cause());
              asyncResultHandler.handle(
                Future.succeededFuture(PostTenantRulesResponse.withPlainInternalServerError("Failed to create new rule")));
            }
          });
        }
      });
    } catch (Exception e) {
      logger.error("Error running on verticle for postTenantRules: " + e.getMessage(), e);
      asyncResultHandler.handle(Future.succeededFuture(
        PostTenantRulesResponse.withPlainInternalServerError(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }

  @Override
  public void putTenantRules(final Rule entity,
                             final Map<String, String> okapiHeaders,
                             final Handler<AsyncResult<Response>> asyncResultHandler,
                             final Context vertxContext) throws Exception {
    try {
      vertxContext.runOnContext(v -> {
        String errorMessage = validateRule(entity);
        if (errorMessage != null) {
          asyncResultHandler.handle(
            Future.succeededFuture(PutTenantRulesResponse.withPlainBadRequest(errorMessage)));
        } else {
          validatorRegistryService.updateTenantRule(tenantId, JsonObject.mapFrom(entity), reply -> {
            if (reply.succeeded()) {
              JsonObject result = reply.result();
              if (result == null) {
                String message = "Rule " + entity.getRuleId() + " does not exist";
                logger.debug(message);
                asyncResultHandler.handle(
                  Future.succeededFuture(PutTenantRulesResponse.withPlainNotFound(message)));
              } else {
                asyncResultHandler.handle(
                  Future.succeededFuture(PutTenantRulesResponse.withJsonOK(result.mapTo(Rule.class))));
              }
            } else {
              logger.error("Failed to update rule", reply.cause());
              asyncResultHandler.handle(
                Future.succeededFuture(PutTenantRulesResponse.withPlainInternalServerError("Failed to update rule")));
            }
          });
        }
      });
    } catch (Exception e) {
      logger.error("Error running on verticle for putTenantRules: " + e.getMessage(), e);
      asyncResultHandler.handle(Future.succeededFuture(
        PutTenantRulesResponse.withPlainInternalServerError(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }

  }

  @Override
  public void getTenantRulesByRuleId(final String ruleId,
                                     final Map<String, String> okapiHeaders,
                                     final Handler<AsyncResult<Response>> asyncResultHandler,
                                     final Context vertxContext) throws Exception {
    try {
      vertxContext.runOnContext(v -> validatorRegistryService.getTenantRuleByRuleId(tenantId, ruleId, reply -> {
        if (reply.succeeded()) {
          JsonObject result = reply.result();
          if (result == null) {
            String message = "Rule " + ruleId + " does not exist";
            logger.debug(message);
            asyncResultHandler.handle(
              Future.succeededFuture(GetTenantRulesByRuleIdResponse.withPlainNotFound(message)));
          } else {
            asyncResultHandler.handle(
              Future.succeededFuture(GetTenantRulesByRuleIdResponse.withJsonOK(result.mapTo(Rule.class))));
          }
        } else {
          logger.error("Failed to get rule by id " + ruleId, reply.cause());
          asyncResultHandler.handle(
            Future.succeededFuture(GetTenantRulesByRuleIdResponse.withPlainInternalServerError("Failed to get rule by id " + ruleId)));
        }
      }));
    } catch (Exception e) {
      logger.error("Error running on verticle for getTenantRulesByRuleId: " + e.getMessage(), e);
      asyncResultHandler.handle(Future.succeededFuture(
        GetTenantRulesByRuleIdResponse.withPlainInternalServerError(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }

  private String validateRule(Rule entity) {
    String errorMessage = null;
    if (entity.getOrderNo() < 0) {
      logger.debug("Invalid orderNo parameter");
      errorMessage = ORDER_NUMBER_ERROR;
    } else if (Rule.Type.REG_EXP.equals(entity.getType()) && !Rule.ValidationType.STRONG.equals(entity.getValidationType())) {
      logger.debug("Invalid validationType parameter");
      errorMessage = VALIDATION_TYPE_ERROR;
    } else if (Rule.Type.PROGRAMMATIC.equals(entity.getType())
      && (entity.getImplementationReference() == null || entity.getImplementationReference().isEmpty())) {
      logger.debug("Implementation reference is not specified for type Programmatic");
      errorMessage = IMPLEMENTATION_REFERENCE_REQUIRED_ERROR;
    }
    return errorMessage;
  }

}

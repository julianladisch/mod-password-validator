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
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.validator.registry.ValidatorRegistryService;

import javax.ws.rs.core.Response;
import java.util.Map;

public class ValidatorRegistryImpl implements TenantRulesResource {

  private final Logger logger = LoggerFactory.getLogger(ValidatorRegistryImpl.class);

  private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
  private static final String INTERNAL_ERROR = "Internal Server error";
  private static final String ORDER_NUMBER_ERROR = "Order number cannot be negative";
  private static final String VALIDATION_TYPE_ERROR = "In case of RegExp rule Validation Type can only be Strong";

  private final ValidatorRegistryService validatorRegistryService;

  public ValidatorRegistryImpl(Vertx vertx, String tenantId) {
    this.validatorRegistryService = ValidatorRegistryService.createProxy(vertx, ValidatorRegistryService.ADDRESS);
  }

  @Override
  public void getTenantRules(final Map<String, String> okapiHeaders,
                             final Handler<AsyncResult<Response>> asyncResultHandler,
                             final Context vertxContext) throws Exception {
    try {
      vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
        validatorRegistryService.getAllTenantRules(tenantId, reply -> {
          if(reply.succeeded()) {
            RuleCollection rules = reply.result().mapTo(RuleCollection.class);
            asyncResultHandler.handle(
              Future.succeededFuture(GetTenantRulesResponse.withJsonOK(rules)));
          } else {
            logger.error(reply.cause().getMessage(), reply.cause());
            asyncResultHandler.handle(
              Future.succeededFuture(GetTenantRulesResponse.withPlainInternalServerError(reply.cause().getMessage())));
          }
        });
      });
    } catch (Exception e) {
      logger.error("Error running on verticle for getTenantRules: " + e.getMessage(), e.getCause());
      asyncResultHandler.handle(Future.succeededFuture(GetTenantRulesResponse.withPlainInternalServerError(INTERNAL_ERROR)));
    }
  }

  @Override
  public void postTenantRules(final Rule entity,
                              final Map<String, String> okapiHeaders,
                              final Handler<AsyncResult<Response>> asyncResultHandler,
                              final Context vertxContext) throws Exception {
    try {
      vertxContext.runOnContext(v -> {
        if(entity.getOrderNo() < 0) {
          logger.debug("Invalid orderNo parameter");
          asyncResultHandler.handle(
            Future.succeededFuture(PostTenantRulesResponse.withPlainBadRequest(ORDER_NUMBER_ERROR)));
        } else if(Rule.Type.REG_EXP.equals(entity.getType()) && !Rule.ValidationType.STRONG.equals(entity.getValidationType())) {
          logger.debug("Invalid validationType parameter");
          asyncResultHandler.handle(
            Future.succeededFuture(PostTenantRulesResponse.withPlainBadRequest(VALIDATION_TYPE_ERROR)));
        } else {
          String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
          validatorRegistryService.createTenantRule(tenantId, JsonObject.mapFrom(entity), reply -> {
            if(reply.succeeded()) {
              asyncResultHandler.handle(
                Future.succeededFuture(PostTenantRulesResponse.withJsonCreated(reply.result().mapTo(Rule.class))));
            } else {
              logger.error(reply.cause().getMessage(), reply.cause());
              asyncResultHandler.handle(
                Future.succeededFuture(PostTenantRulesResponse.withPlainInternalServerError(reply.cause().getMessage())));
            }
          });
        }
      });
    } catch (Exception e) {
      logger.error("Error running on verticle for postTenantRules: " + e.getMessage(), e.getCause());
      asyncResultHandler.handle(Future.succeededFuture(PostTenantRulesResponse.withPlainInternalServerError(INTERNAL_ERROR)));
    }
  }

  @Override
  public void putTenantRules(final Rule entity,
                             final Map<String, String> okapiHeaders,
                             final Handler<AsyncResult<Response>> asyncResultHandler,
                             final Context vertxContext) throws Exception {
    try {
      vertxContext.runOnContext(v -> {
        if(entity.getOrderNo() < 0) {
          logger.debug("Invalid orderNo parameter");
          asyncResultHandler.handle(
            Future.succeededFuture(PutTenantRulesResponse.withPlainBadRequest(ORDER_NUMBER_ERROR)));
        } else if(Rule.Type.REG_EXP.equals(entity.getType()) && !Rule.ValidationType.STRONG.equals(entity.getValidationType())) {
          logger.debug("Invalid validationType parameter");
          asyncResultHandler.handle(
            Future.succeededFuture(PutTenantRulesResponse.withPlainBadRequest(VALIDATION_TYPE_ERROR)));
        } else {
          String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
          validatorRegistryService.updateTenantRule(tenantId, JsonObject.mapFrom(entity), reply -> {
            if(reply.succeeded()) {
              JsonObject result = reply.result();
              if(result == null) {
                String message = "Rule " + entity.getRuleId() + " does not exist";
                logger.debug(message);
                asyncResultHandler.handle(
                  Future.succeededFuture(PutTenantRulesResponse.withPlainNotFound(message)));
              } else {
                asyncResultHandler.handle(
                  Future.succeededFuture(PutTenantRulesResponse.withJsonOK(result.mapTo(Rule.class))));
              }
            } else {
              logger.error(reply.cause().getMessage(), reply.cause());
              asyncResultHandler.handle(
                Future.succeededFuture(PutTenantRulesResponse.withPlainInternalServerError(reply.cause().getMessage())));
            }
          });
        }
      });
    } catch (Exception e) {
      logger.error("Error running on verticle for putTenantRules: " + e.getMessage());
      asyncResultHandler.handle(Future.succeededFuture(PutTenantRulesResponse.withPlainInternalServerError(INTERNAL_ERROR)));
    }

  }

  @Override
  public void getTenantRulesByRuleId(final String ruleId,
                                     final Map<String, String> okapiHeaders,
                                     final Handler<AsyncResult<Response>> asyncResultHandler,
                                     final Context vertxContext) throws Exception {
    try {
      vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
        validatorRegistryService.getTenantRuleByRuleId(tenantId, ruleId, reply -> {
          if(reply.succeeded()) {
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
            logger.error(reply.cause().getMessage(), reply.cause());
            asyncResultHandler.handle(
              Future.succeededFuture(GetTenantRulesByRuleIdResponse.withPlainInternalServerError(reply.cause().getMessage())));
          }
        });
      });
    } catch (Exception e) {
      logger.error("Error running on verticle for getTenantRulesByRuleId: " + e.getMessage());
      asyncResultHandler.handle(Future.succeededFuture(GetTenantRulesByRuleIdResponse.withPlainInternalServerError(INTERNAL_ERROR)));
    }
  }

}

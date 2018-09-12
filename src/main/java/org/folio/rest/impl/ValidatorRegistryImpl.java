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
          if(reply.succeeded()){
            RuleCollection rules = reply.result().mapTo(RuleCollection.class);
            asyncResultHandler.handle(
              Future.succeededFuture(GetTenantRulesResponse.withJsonOK(rules)));
          } else {
            asyncResultHandler.handle(
              Future.succeededFuture(GetTenantRulesResponse.withPlainInternalServerError(reply.result().toString())));
          }
        });
      });
    } catch (Exception e) {
      logger.debug("Error running on verticle for getTenantRules: " + e.getMessage());
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

        if(entity.getOrderNo() < 0){
          asyncResultHandler.handle(
            Future.succeededFuture(PostTenantRulesResponse.withPlainBadRequest("Order number cannot be negative")));
        }
        if(entity.getType() == Rule.Type.REG_EXP && entity.getValidationType() != Rule.ValidationType.STRONG) {
          asyncResultHandler.handle(
            Future.succeededFuture(PostTenantRulesResponse.withPlainBadRequest("In case of RegExp rule Validation Type can only be Strong")));
        }

        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        validatorRegistryService.createTenantRule(tenantId, JsonObject.mapFrom(entity), reply -> {
          if(reply.succeeded()){
            asyncResultHandler.handle(
              Future.succeededFuture(PostTenantRulesResponse.withJsonCreated(reply.result().mapTo(Rule.class))));
          } else {
            asyncResultHandler.handle(
              Future.succeededFuture(PostTenantRulesResponse.withPlainInternalServerError(reply.result().toString())));
          }
        });
      });
    } catch (Exception e) {
      logger.debug("Error running on verticle for postTenantRules: " + e.getMessage());
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

        if(entity.getOrderNo() < 0){
          asyncResultHandler.handle(
            Future.succeededFuture(PostTenantRulesResponse.withPlainBadRequest("Order number cannot be negative")));
        }
        if(entity.getType() == Rule.Type.REG_EXP && entity.getValidationType() != Rule.ValidationType.STRONG) {
          asyncResultHandler.handle(
            Future.succeededFuture(PostTenantRulesResponse.withPlainBadRequest("In case of RegExp rule Validation Type can only be Strong")));
        }

        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        validatorRegistryService.updateTenantRule(tenantId, JsonObject.mapFrom(entity), reply -> {
          if(reply.succeeded()){
            JsonObject result = reply.result();
            if(result == null) {
              asyncResultHandler.handle(
                Future.succeededFuture(PutTenantRulesResponse.withPlainNotFound("Rule " + entity.getRuleId() + " does not exist")));
            } else {
              Rule rule = result.mapTo(Rule.class);
              asyncResultHandler.handle(
                Future.succeededFuture(PutTenantRulesResponse.withJsonOK(rule)));
            }
          } else {
            asyncResultHandler.handle(
              Future.succeededFuture(PutTenantRulesResponse.withPlainInternalServerError(reply.result().toString())));
          }
        });
      });
    } catch (Exception e) {
      logger.debug("Error running on verticle for putTenantRules: " + e.getMessage());
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
          if(reply.succeeded()){
            JsonObject result = reply.result();
            if (result == null) {
              asyncResultHandler.handle(
                Future.succeededFuture(GetTenantRulesByRuleIdResponse.withPlainNotFound("Rule " + ruleId + " does not exist")));
            } else {
              asyncResultHandler.handle(
                Future.succeededFuture(GetTenantRulesByRuleIdResponse.withJsonOK(result.mapTo(Rule.class))));
            }
          } else {
            asyncResultHandler.handle(
              Future.succeededFuture(GetTenantRulesByRuleIdResponse.withPlainInternalServerError(reply.result().toString())));
          }
        });
      });
    } catch (Exception e) {
      logger.debug("Error running on verticle for getTenantRulesByRuleId: " + e.getMessage());
      asyncResultHandler.handle(Future.succeededFuture(GetTenantRulesByRuleIdResponse.withPlainInternalServerError(INTERNAL_ERROR)));
    }
  }

}

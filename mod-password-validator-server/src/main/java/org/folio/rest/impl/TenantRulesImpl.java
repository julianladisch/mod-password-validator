package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.rest.jaxrs.resource.TenantRules;
import org.folio.services.validator.registry.ValidatorRegistryService;
import org.folio.services.validator.util.ValidatorHelper;

import javax.ws.rs.core.Response;
import java.util.Map;

public class TenantRulesImpl implements TenantRules {

  private final Logger logger = LoggerFactory.getLogger(TenantRulesImpl.class);

  private static final String ORDER_NUMBER_ERROR = "Order number cannot be negative";
  private static final String RULE_ID_EMPTY_ERROR = "Entity id and ruleId cannot be null or empty";
  private static final String RULE_ID_MATCH_ERROR = "Entity id and ruleId cannot be different";
  private static final String VALIDATION_TYPE_ERROR = "In case of RegExp rule Validation Type can only be Strong";
  private static final String IMPLEMENTATION_REFERENCE_REQUIRED_ERROR = "In case of Programmatic rule Implementation reference should be provided";

  private final ValidatorRegistryService validatorRegistryService;
  private String tenantId;

  public TenantRulesImpl(Vertx vertx, String tenantId) {
    this.tenantId = tenantId;
    this.validatorRegistryService = ValidatorRegistryService.createProxy(vertx, ValidatorHelper.REGISTRY_SERVICE_ADDRESS);
  }

  @Override
  public void getTenantRules(int offset, int limit, String query, Map<String, String> okapiHeaders,
                             Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      validatorRegistryService.getAllTenantRules(tenantId, limit, offset, query, reply -> {
        if (reply.succeeded()) {
          RuleCollection rules = reply.result().mapTo(RuleCollection.class);
          asyncResultHandler.handle(
            Future.succeededFuture(GetTenantRulesResponse.respond200WithApplicationJson(rules)));
        } else {
          String message = "Failed to get all tenant rules";
          logger.error(message, reply.cause());
          asyncResultHandler.handle(
            Future.succeededFuture(GetTenantRulesResponse.respond500WithTextPlain(message)));
        }
      });
    } catch (Exception e) {
      logger.error("Error running on verticle for getTenantRules: " + e.getMessage(), e);
      asyncResultHandler.handle(Future.succeededFuture(
        GetTenantRulesResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }

  @Override
  public void postTenantRules(final Rule entity,
                              final Map<String, String> okapiHeaders,
                              final Handler<AsyncResult<Response>> asyncResultHandler,
                              final Context vertxContext) {
    try {
      String errorMessage = validateRule(entity);
      if (errorMessage != null) {
        asyncResultHandler.handle(
          Future.succeededFuture(PostTenantRulesResponse.respond400WithTextPlain(errorMessage)));
      } else {
        validatorRegistryService.createTenantRule(tenantId, JsonObject.mapFrom(entity), reply -> {
          if (reply.succeeded()) {
            asyncResultHandler.handle(
              Future.succeededFuture(PostTenantRulesResponse.respond201WithApplicationJson(reply.result().mapTo(Rule.class))));
          } else {
            String message = "Failed to create new rule";
            logger.error(message, reply.cause());
            asyncResultHandler.handle(
              Future.succeededFuture(PostTenantRulesResponse.respond500WithTextPlain(message)));
          }
        });
      }
    } catch (Exception e) {
      logger.error("Error running on verticle for postTenantRules: " + e.getMessage(), e);
      asyncResultHandler.handle(Future.succeededFuture(
        PostTenantRulesResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }

  @Override
  public void putTenantRules(final Rule entity,
                             final Map<String, String> okapiHeaders,
                             final Handler<AsyncResult<Response>> asyncResultHandler,
                             final Context vertxContext) {
    try {
      String errorMessage = validateRuleOnPut(entity);
      if (errorMessage != null) {
        asyncResultHandler.handle(
          Future.succeededFuture(PutTenantRulesResponse.respond400WithTextPlain(errorMessage)));
      } else {
        validatorRegistryService.updateTenantRule(tenantId, JsonObject.mapFrom(entity), reply -> {
          if (reply.succeeded()) {
            JsonObject result = reply.result();
            if (result == null) {
              String message = "Rule " + entity.getRuleId() + " does not exist";
              logger.debug(message);
              asyncResultHandler.handle(
                Future.succeededFuture(PutTenantRulesResponse.respond404WithTextPlain(message)));
            } else {
              asyncResultHandler.handle(
                Future.succeededFuture(PutTenantRulesResponse.respond200WithApplicationJson(result.mapTo(Rule.class))));
            }
          } else {
            String message = "Failed to update rule";
            logger.error(message, reply.cause());
            asyncResultHandler.handle(
              Future.succeededFuture(PutTenantRulesResponse.respond500WithTextPlain(message)));
          }
        });
      }
    } catch (Exception e) {
      logger.error("Error running on verticle for putTenantRules: " + e.getMessage(), e);
      asyncResultHandler.handle(Future.succeededFuture(
        PutTenantRulesResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }

  @Override
  public void getTenantRulesByRuleId(final String ruleId,
                                     final Map<String, String> okapiHeaders,
                                     final Handler<AsyncResult<Response>> asyncResultHandler,
                                     final Context vertxContext) {
    try {
      validatorRegistryService.getTenantRuleByRuleId(tenantId, ruleId, reply -> {
        if (reply.succeeded()) {
          JsonObject result = reply.result();
          if (result == null) {
            String message = "Rule " + ruleId + " does not exist";
            logger.debug(message);
            asyncResultHandler.handle(
              Future.succeededFuture(GetTenantRulesByRuleIdResponse.respond404WithTextPlain(message)));
          } else {
            asyncResultHandler.handle(
              Future.succeededFuture(GetTenantRulesByRuleIdResponse.respond200WithApplicationJson(result.mapTo(Rule.class))));
          }
        } else {
          String message = "Failed to get rule by id " + ruleId;
          logger.error(message, reply.cause());
          asyncResultHandler.handle(
            Future.succeededFuture(GetTenantRulesByRuleIdResponse.respond500WithTextPlain(message)));
        }
      });
    } catch (Exception e) {
      logger.error("Error running on verticle for getTenantRulesByRuleId: " + e.getMessage(), e);
      asyncResultHandler.handle(Future.succeededFuture(
        GetTenantRulesByRuleIdResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }

  /**
   * Validates Rule entity
   *
   * @param entity - Rule entity to validate
   * @return error message
   */
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

  private String validateRuleOnPut(Rule entity) {
    if (StringUtils.isEmpty(entity.getId()) && StringUtils.isEmpty(entity.getRuleId())) {
      logger.debug("Rule id cannot be null or empty");
      return RULE_ID_EMPTY_ERROR;
    } else if (StringUtils.isEmpty(entity.getId()) && !StringUtils.isEmpty(entity.getRuleId())) {
      entity.setId(entity.getRuleId());
    } else if (!StringUtils.isEmpty(entity.getId()) && StringUtils.isEmpty(entity.getRuleId())) {
      entity.setRuleId(entity.getId());
    } else if (!entity.getId().equals(entity.getRuleId())) {
      logger.debug("Rule id and ruleId are different");
      return RULE_ID_MATCH_ERROR;
    }
    return validateRule(entity);
  }

}

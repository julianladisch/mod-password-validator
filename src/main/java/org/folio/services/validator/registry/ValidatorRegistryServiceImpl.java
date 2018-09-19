package org.folio.services.validator.registry;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;

import java.util.List;
import java.util.UUID;

public class ValidatorRegistryServiceImpl implements ValidatorRegistryService {

  private final Logger logger = LoggerFactory.getLogger(ValidatorRegistryServiceImpl.class);

  private static final String VALIDATION_RULES_TABLE_NAME = "validation_rules";
  private static final String RULE_ID_FIELD = "ruleId";
  private static final String RULE_ID_JSONB_FIELD = "'ruleId'";
  private static final String STATE_JSONB_FIELD = "'state'";
  private static final String TYPE_JSONB_FIELD = "'type'";

  private final Vertx vertx;

  public ValidatorRegistryServiceImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Returns all rules for tenant
   *
   * @param tenantId           tenant id
   * @param asyncResultHandler result handler
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  public ValidatorRegistryService getAllTenantRules(String tenantId, Handler<AsyncResult<JsonObject>> asyncResultHandler) {
    try {
      CQL2PgJSON cql2pgJson = new CQL2PgJSON(VALIDATION_RULES_TABLE_NAME + ".jsonb");
      CQLWrapper cql = new CQLWrapper();
      cql.setField(cql2pgJson);
      String[] fieldList = {"*"};
      PostgresClient.getInstance(vertx, tenantId).get(VALIDATION_RULES_TABLE_NAME, Rule.class, fieldList, cql, true, false, getReply -> {
        if (getReply.failed()) {
          logger.error("Error while querying the db to get all tenant rules", getReply.cause());
          asyncResultHandler.handle(Future.failedFuture(getReply.cause()));
        } else {
          RuleCollection rules = new RuleCollection();
          List<Rule> ruleList = (List<Rule>) getReply.result().getResults();
          rules.setRules(ruleList);
          rules.setTotalRecords(ruleList.size());
          asyncResultHandler.handle(Future.succeededFuture(JsonObject.mapFrom(rules)));
        }
      });
    } catch (Exception e) {
      logger.error("Error while getting all tenant rules", e);
      asyncResultHandler.handle(Future.failedFuture(e));
    }
    return this;
  }

  /**
   * Creates rule for tenant with specified id
   *
   * @param tenantId           tenant id
   * @param validationRule     rule to save
   * @param asyncResultHandler result handler
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  public ValidatorRegistryService createTenantRule(String tenantId, JsonObject validationRule, Handler<AsyncResult<JsonObject>> asyncResultHandler) {
    try {
      String id = UUID.randomUUID().toString();
      validationRule.put(RULE_ID_FIELD, id);
      PostgresClient.getInstance(vertx, tenantId).save(VALIDATION_RULES_TABLE_NAME, id, validationRule.mapTo(Rule.class), postReply -> {
        if (postReply.failed()) {
          logger.error("Error while saving the rule to the db", postReply.cause());
          asyncResultHandler.handle(Future.failedFuture(postReply.cause()));
        } else {
          asyncResultHandler.handle(Future.succeededFuture(validationRule));
        }
      });
    } catch (Exception e) {
      logger.error("Error while creating new tenant rule", e);
      asyncResultHandler.handle(Future.failedFuture(e));
    }
    return this;
  }

  /**
   * Updates rule for tenant with specified id by identifier from given <code>validationRule</code>
   *
   * @param tenantId           tenant id
   * @param validationRule     rule to update
   * @param asyncResultHandler result handler
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  public ValidatorRegistryService updateTenantRule(String tenantId, JsonObject validationRule, Handler<AsyncResult<JsonObject>> asyncResultHandler) {
    try {
      String id = validationRule.getString(RULE_ID_FIELD);
      Criteria idCrit = constructCriteria(RULE_ID_JSONB_FIELD, id);
      PostgresClient.getInstance(vertx, tenantId).get(VALIDATION_RULES_TABLE_NAME, Rule.class, new Criterion(idCrit), true, false, getReply -> {
        if (getReply.failed()) {
          logger.error("Error while querying the db to get the rule by id", getReply.cause());
          asyncResultHandler.handle(Future.failedFuture(getReply.cause()));
        } else if (getReply.result().getResults().isEmpty()) {
          logger.debug("Rule " + id + " was not found in the db");
          asyncResultHandler.handle(Future.succeededFuture(null));
        } else {
          PostgresClient.getInstance(vertx, tenantId).update(VALIDATION_RULES_TABLE_NAME, validationRule.mapTo(Rule.class), new Criterion(idCrit), true, putReply -> {
            if (putReply.failed()) {
              logger.error("Error while updating the rule " + id + " in the db", putReply.cause());
              asyncResultHandler.handle(Future.failedFuture(putReply.cause()));
            } else {
              asyncResultHandler.handle(Future.succeededFuture(validationRule));
            }
          });
        }
      });
    } catch (Exception e) {
      logger.error("Error while updating the rule in the db", e);
      asyncResultHandler.handle(Future.failedFuture(e));
    }
    return this;
  }

  /**
   * Searches for validation rule by given <code>tenantId</code> and <code>ruleId</code>
   *
   * @param tenantId           tenant id
   * @param ruleId             rule id
   * @param asyncResultHandler result handler
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  public ValidatorRegistryService getTenantRuleByRuleId(String tenantId, String ruleId, Handler<AsyncResult<JsonObject>> asyncResultHandler) {
    try {
      Criteria idCrit = constructCriteria(RULE_ID_JSONB_FIELD, ruleId);
      PostgresClient.getInstance(vertx, tenantId).get(VALIDATION_RULES_TABLE_NAME, Rule.class, new Criterion(idCrit), true, false, getReply -> {
        if (getReply.failed()) {
          logger.error("Error while querying the db to get the rule by id", getReply.cause());
          asyncResultHandler.handle(Future.failedFuture(getReply.cause()));
        } else {
          List<Rule> ruleList = (List<Rule>) getReply.result().getResults();
          if (ruleList.isEmpty()) {
            logger.debug("Rule " + ruleId + "was not found in the db");
            asyncResultHandler.handle(Future.succeededFuture(null));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(JsonObject.mapFrom(ruleList.get(0))));
          }
        }
      });
    } catch (Exception e) {
      logger.error("Error while getting rule by id", e);
      asyncResultHandler.handle(Future.failedFuture(e));
    }
    return this;
  }

  /**
   * Searches for enabled rules with specified tenant id and rule type
   *
   * @param tenantId           tenant id
   * @param type               rule type
   * @param asyncResultHandler result handler
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  public ValidatorRegistryService getEnabledRulesByType(String tenantId, String type, Handler<AsyncResult<JsonObject>> asyncResultHandler) {
    try {
      Criterion criterion;
      Criteria stateCrit = constructCriteria(STATE_JSONB_FIELD, Rule.State.ENABLED.toString());
      if (type != null && !type.isEmpty()) {
        Criteria typeCrit = constructCriteria(TYPE_JSONB_FIELD, type);
        criterion = new Criterion();
        criterion.addCriterion(stateCrit, "AND", typeCrit);
      } else {
        criterion = new Criterion(stateCrit);
      }
      PostgresClient.getInstance(vertx, tenantId).get(VALIDATION_RULES_TABLE_NAME, Rule.class, criterion, true, false, getReply -> {
        if (getReply.failed()) {
          logger.error("Error while querying the db to get all enabled tenant rules by type", getReply.cause());
          asyncResultHandler.handle(Future.failedFuture(getReply.cause()));
        } else {
          RuleCollection rules = new RuleCollection();
          List<Rule> ruleList = (List<Rule>) getReply.result().getResults();
          rules.setRules(ruleList);
          rules.setTotalRecords(ruleList.size());
          asyncResultHandler.handle(Future.succeededFuture(JsonObject.mapFrom(rules)));
        }
      });
    } catch (Exception e) {
      logger.error("Error while getting all enabled rules by type", e);
      asyncResultHandler.handle(Future.failedFuture(e));
    }
    return this;
  }

  private Criteria constructCriteria(String jsonbField, String value) {
    Criteria criteria = new Criteria();
    criteria.addField(jsonbField);
    criteria.setOperation("=");
    criteria.setValue(value);
    return criteria;
  }

}

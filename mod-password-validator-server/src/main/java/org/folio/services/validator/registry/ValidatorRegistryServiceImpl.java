package org.folio.services.validator.registry;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of Validator Registry Service,
 * calls PostgresClient to perform CRUD operations on Rule entity
 */
public class ValidatorRegistryServiceImpl implements ValidatorRegistryService {

  private final Logger logger = LoggerFactory.getLogger(ValidatorRegistryServiceImpl.class);

  private static final String VALIDATION_RULES_TABLE_NAME = "validation_rules";
  private static final String RULE_ID_FIELD = "ruleId";

  private final Vertx vertx;

  public ValidatorRegistryServiceImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Returns all rules for tenant
   *
   * @param tenantId           tenant id
   * @param limit              maximum number of results to return
   * @param offset             starting index in a list of results
   * @param query              query string to filter rules based on matching criteria in fields
   * @param asyncResultHandler result handler
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  public ValidatorRegistryService getAllTenantRules(String tenantId, int limit, int offset, String query, Handler<AsyncResult<JsonObject>> asyncResultHandler) {
    try {
      CQLWrapper cql = getCQL(query, limit, offset);
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
      PostgresClient.getInstance(vertx, tenantId).update(VALIDATION_RULES_TABLE_NAME, validationRule.mapTo(Rule.class), id, putReply -> {
        if (putReply.failed()) {
          logger.error("Error while updating the rule " + id + " in the db", putReply.cause());
          asyncResultHandler.handle(Future.failedFuture(putReply.cause()));
        } else if (putReply.result().rowCount() == 0) {
          logger.debug("Rule " + id + " was not found in the db");
          asyncResultHandler.handle(Future.succeededFuture(null));
        } else {
          asyncResultHandler.handle(Future.succeededFuture(validationRule));
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
    Promise<Rule> promise = Promise.promise();
    PostgresClient.getInstance(vertx, tenantId).getById(VALIDATION_RULES_TABLE_NAME, ruleId, Rule.class, promise);

    try {
      PostgresClient.getInstance(vertx, tenantId).getById(VALIDATION_RULES_TABLE_NAME, ruleId, Rule.class, getReply -> {
        if (getReply.failed()) {
          logger.error("Error while querying the db to get the rule by id", getReply.cause());
          asyncResultHandler.handle(Future.failedFuture(getReply.cause()));
        } else {
          Rule rule = getReply.result();
          if (rule == null) {
            logger.debug("Rule " + ruleId + "was not found in the db");
            asyncResultHandler.handle(Future.succeededFuture(null));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(JsonObject.mapFrom(rule)));
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
   * Build CQL from request URL query
   *
   * @param query - query from URL
   * @param limit - limit of records for pagination
   * @return - CQL wrapper for building postgres request to database
   * @throws org.folio.cql2pgjson.exception.FieldException
   */
  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2pgJson = new CQL2PgJSON(VALIDATION_RULES_TABLE_NAME + ".jsonb");
    return new CQLWrapper(cql2pgJson, query)
      .setLimit(new Limit(limit))
      .setOffset(new Offset(offset));
  }

}

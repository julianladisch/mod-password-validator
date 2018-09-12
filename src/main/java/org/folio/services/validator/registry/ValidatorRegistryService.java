package org.folio.services.validator.registry;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface ValidatorRegistryService {

  static final String ADDRESS = "validator-registry.queue";

  static ValidatorRegistryService create(Vertx vertx) {
    return new ValidatorRegistryServiceImpl(vertx);
  }

  static ValidatorRegistryService createProxy(Vertx vertx, String address) {
    return new ValidatorRegistryServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  ValidatorRegistryService getAllTenantRules(String tenantId, Handler<AsyncResult<JsonObject>> asyncResultHandler);

  @Fluent
  ValidatorRegistryService createTenantRule(String tenantId, JsonObject validationRule, Handler<AsyncResult<JsonObject>> asyncResultHandler);

  @Fluent
  ValidatorRegistryService updateTenantRule(String tenantId, JsonObject validationRule, Handler<AsyncResult<JsonObject>> asyncResultHandler);

  @Fluent
  ValidatorRegistryService getTenantRuleByRuleId(String tenantId, String ruleId, Handler<AsyncResult<JsonObject>> asyncResultHandler);

  @Fluent
  ValidatorRegistryService getActiveRulesByType(String tenantId, String type, Handler<AsyncResult<JsonObject>> asyncResultHandler);

}

package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ServiceBinder;
import org.folio.rest.resource.interfaces.InitAPI;
import org.folio.services.validator.engine.ValidationEngineService;
import org.folio.services.validator.registry.ValidatorRegistryService;
import org.folio.services.validator.util.ValidatorHelper;

/**
 * Performs preprocessing operations before the verticle is deployed,
 * e.g. components registration, initializing, binding.
 *
 * @author Igor Gorchakov
 */
public class InitAPIs implements InitAPI {
  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    new ServiceBinder(vertx)
      .setAddress(ValidatorHelper.REGISTRY_SERVICE_ADDRESS)
      .register(ValidatorRegistryService.class, ValidatorRegistryService.create(vertx));
    new ServiceBinder(vertx)
      .setAddress(ValidatorHelper.VALIDATOR_ENGINE_ADDRESS)
      .register(ValidationEngineService.class, ValidationEngineService.create(vertx));

    handler.handle(Future.succeededFuture(true));
  }
}

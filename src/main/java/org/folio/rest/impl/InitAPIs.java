package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.resource.interfaces.InitAPI;

/**
 * Perform preprocessing operation before the verticle is deployed,
 * e.g. components registration, initializing, binding.
 *
 * @author Igor-Gorchakov
 */
public class InitAPIs implements InitAPI {
  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    // TODO Bind Vertx with new added component here
    handler.handle(Future.succeededFuture());
  }
}

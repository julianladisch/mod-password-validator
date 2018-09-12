package org.folio.services.validator.engine;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Map;


/**
 * The root interface for validation engine implementations.
 * The main concept is to validate incoming password.
 *
 * @author Igor Gorchakov
 */
@ProxyGen
public interface ValidationEngineService {

  static final String ADDRESS = "validation-engine.queue";

  static ValidationEngineService create(Vertx vertx) {
    //TODO add implementation
    return null;
  }

  /**
   * Creates proxy instance that helps to push message into the message queue
   *
   * @param vertx   vertx instance
   * @param address host address
   * @return ValidationEngineService instance
   */
  static ValidationEngineService createProxy(Vertx vertx, String address) {
    return new ValidationEngineServiceVertxEBProxy(vertx, address);
  }

  /**
   * Performs received password validation
   *
   * @param password      password
   * @param headers       headers
   * @param resultHandler handler with validation results in format <Status, Message>
   */
  void validatePassword(String password, Map<String, String> headers, Handler<AsyncResult<JsonObject>> resultHandler);

}

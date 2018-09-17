package org.folio.services.validator.util;

public final class ValidatorHelper {

  public static final String VALIDATOR_ENGINE_ADDRESS = "validation-engine.queue";
  public static final String REGISTRY_SERVICE_ADDRESS = "validator-registry.queue";

  public static final String RESPONSE_VALIDATION_RESULT_KEY = "result";
  public static final String RESPONSE_ERROR_MESSAGES_KEY = "messages";
  public static final String REQUEST_PARAM_KEY = "password";
  public static final String VALIDATION_VALID_RESULT = "valid";
  public static final String VALIDATION_INVALID_RESULT = "invalid";

  private ValidatorHelper() {
  }

}

package org.folio.spring.client;

/**
 * Exception indicating that okapi module responded in unexpected way
 */
public class OkapiModuleClientException extends RuntimeException {

  private int status;

  public OkapiModuleClientException() {
    super();
  }

  public OkapiModuleClientException(String message) {
    super(message);
  }

  public OkapiModuleClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public OkapiModuleClientException(Throwable cause) {
    super(cause);
  }

  public OkapiModuleClientException(int status, String message) {
    super(message);
    this.status = status;

  }
}

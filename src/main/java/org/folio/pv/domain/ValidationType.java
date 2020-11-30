package org.folio.pv.domain;

public enum ValidationType {
  SOFT("Soft"),
  STRONG("Strong");

  private String value;

  ValidationType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static ValidationType fromValue(String value) {
    for (ValidationType vt : ValidationType.values()) {
      if (vt.value.equals(value)) {
        return vt;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

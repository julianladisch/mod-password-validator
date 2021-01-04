package org.folio.pv.domain;

public enum RuleType {
  REGEXP("RegExp"),
  PROGRAMMATIC("Programmatic"),
  PWNEDPASSWORD("PwnedPassword");

  private final String value;

  RuleType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static RuleType fromValue(String value) {
    for (RuleType rt : RuleType.values()) {
      if (rt.value.equals(value)) {
        return rt;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

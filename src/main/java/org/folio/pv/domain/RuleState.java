package org.folio.pv.domain;

public enum RuleState {

  ENABLED("Enabled"),
  DISABLED("Disabled");

  private final String value;

  RuleState(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static RuleState fromValue(String value) {
    for (RuleState rt : RuleState.values()) {
      if (rt.value.equals(value)) {
        return rt;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

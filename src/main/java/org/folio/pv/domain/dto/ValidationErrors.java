package org.folio.pv.domain.dto;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class ValidationErrors {

  private static final ValidationErrors NONE = new ValidationErrors();

  private final List<String> errorMessages;


  public static ValidationErrors none() {
    return NONE;
  }

  public static ValidationErrors of(String... messages) {
    return new ValidationErrors(Arrays.asList(messages));
  }

  public static ValidationErrors of(List<String> messages) {
    return new ValidationErrors(messages);
  }

  private ValidationErrors() {
    this(emptyList());
  }

  private ValidationErrors(List<String> errorMessages) {
    this.errorMessages = errorMessages != null ? new ArrayList<>(errorMessages) : emptyList();
  }

  public boolean hasErrors() {
    return !errorMessages.isEmpty();
  }

  public List<String> getErrorMessages() {
    return unmodifiableList(errorMessages);
  }
}
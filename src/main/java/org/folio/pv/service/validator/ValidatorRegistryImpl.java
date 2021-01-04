package org.folio.pv.service.validator;

import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.pv.client.PwnedClient;
import org.folio.pv.domain.RuleType;
import org.folio.pv.domain.entity.PasswordValidationRule;
import org.folio.spring.FolioExecutionContext;

@Component
@RequiredArgsConstructor
@Log4j2
class ValidatorRegistryImpl implements ValidatorRegistry {

  private final FolioExecutionContext folioExecutionContext;
  private final ObjectMapper jacksonObjectMapper;
  private final PwnedClient pwnedClient;


  @Override
  public Validator validatorByRule(@NonNull PasswordValidationRule rule) {
    Objects.requireNonNull(rule, "Validation rule is null");

    Validator validator;

    var ruleType = RuleType.fromValue(rule.getRuleType());

    if (ruleType == RuleType.REGEXP) {
      validator = new RegExpValidator(rule);
    } else if (ruleType == RuleType.PROGRAMMATIC) {
      validator = new ProgrammaticValidator(rule, folioExecutionContext, jacksonObjectMapper);
    } else if (ruleType == RuleType.PWNEDPASSWORD) {
      validator = new PwnedPasswordValidator(rule, pwnedClient);
    } else {
      throw new IllegalStateException("Validator is not registered for rule type: " + ruleType);
    }
    
    return validator;
  }

}
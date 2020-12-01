package org.folio.pv.service;

import org.folio.pv.domain.dto.Password;
import org.folio.pv.domain.dto.ValidationResult;
import org.folio.pv.domain.dto.ValidationRule;
import org.folio.pv.domain.dto.ValidationRuleCollection;

public interface ValidationRuleService {

  ValidationRule getValidationRuleById(String ruleId);

  ValidationRuleCollection getValidationRules(Integer offset, Integer limit, String orderBy);

  ValidationRule createOrUpdateValidationRule(ValidationRule validationRule);

  ValidationRule storeValidationRule(ValidationRule validationRule);

  ValidationResult validatePasswordByRules(Password passwordContainer);
}

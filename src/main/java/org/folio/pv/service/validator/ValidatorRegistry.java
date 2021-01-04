package org.folio.pv.service.validator;

import org.springframework.lang.NonNull;

import org.folio.pv.domain.entity.PasswordValidationRule;

public interface ValidatorRegistry {

  Validator validatorByRule(@NonNull PasswordValidationRule rule);

}

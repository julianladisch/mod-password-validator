package org.folio.pv.controller;

import javax.validation.Valid;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.pv.domain.dto.Password;
import org.folio.pv.domain.dto.ValidationResult;
import org.folio.pv.rest.resource.PasswordApi;
import org.folio.pv.service.ValidationRuleService;


@Log4j2
@RestController
@RequestMapping(value = "/")
public class PasswordValidatorController implements PasswordApi {
  private final ValidationRuleService validationRuleService;

  @Autowired
  public PasswordValidatorController(ValidationRuleService validationRuleService) {
    this.validationRuleService = validationRuleService;
  }

  @Override
  public ResponseEntity<ValidationResult> validatePassword(@Valid Password passwordContainer) {
    ValidationResult validationResult = validationRuleService.validatePasswordByRules(passwordContainer);
    return new ResponseEntity<>(validationResult, HttpStatus.OK);
  }
}

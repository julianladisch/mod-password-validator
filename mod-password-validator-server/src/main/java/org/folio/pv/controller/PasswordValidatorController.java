package org.folio.pv.controller;

import lombok.extern.slf4j.Slf4j;
import org.folio.pv.domain.dto.Password;
import org.folio.pv.domain.dto.ValidationResult;
import org.folio.pv.rest.resources.PasswordApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping(value = "/")
public class PasswordValidatorController implements PasswordApi {
  @Override
  public ResponseEntity<ValidationResult> validatePassword(@Valid Password password) {
    return null;
  }
}

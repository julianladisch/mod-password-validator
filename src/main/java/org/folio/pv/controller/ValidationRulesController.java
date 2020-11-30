package org.folio.pv.controller;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.extern.log4j.Log4j2;
import org.folio.pv.domain.dto.ValidationRule;
import org.folio.pv.domain.dto.ValidationRuleCollection;
import org.folio.pv.rest.resource.RulesApi;
import org.folio.pv.service.ValidationRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping(value = "/tenant/")
public class ValidationRulesController implements RulesApi {

  private final ValidationRuleService validationRuleService;

  @Autowired
  public ValidationRulesController(ValidationRuleService validationRuleService) {
    this.validationRuleService = validationRuleService;
  }

  @Override
  public ResponseEntity<ValidationRule> getTenantRuleById(String ruleId) {
    var rule = validationRuleService.getValidationRuleById(ruleId);
    return rule == null ? ResponseEntity.notFound().build() : new ResponseEntity<>(rule, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ValidationRuleCollection> getTenantRules(@Min(0) @Max(2147483647) @Valid Integer offset, @Min(0) @Max(2147483647) @Valid Integer limit, @Valid String query) {
    var validationRules = validationRuleService.getValidationRules(offset, limit, "");
    return new ResponseEntity<>(validationRules, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ValidationRule> postTenantRules(@Valid ValidationRule validationRule) {
    var rule = validationRuleService.createOrUpdateValidationRule(validationRule);
    return new ResponseEntity<>(rule, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ValidationRule> putTenantRule(@Valid ValidationRule validationRule) {
    var rule = validationRuleService.storeValidationRule(validationRule);
    return new ResponseEntity<>(rule, HttpStatus.OK);
  }
}

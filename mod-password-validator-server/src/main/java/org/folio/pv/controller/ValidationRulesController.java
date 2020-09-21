package org.folio.pv.controller;

import lombok.extern.slf4j.Slf4j;
import org.folio.pv.rest.dto.ValidationRule;
import org.folio.pv.rest.dto.ValidationRuleCollection;
import org.folio.pv.rest.resources.RulesApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Slf4j
@RestController
@RequestMapping(value = "/tenant/")
public class ValidationRulesController implements RulesApi {

  @Override
  public ResponseEntity<ValidationRule> getTenantRuleById(String ruleId) {
    return null;
  }

  @Override
  public ResponseEntity<ValidationRuleCollection> getTenantRules(@Min(0) @Max(2147483647) @Valid Integer offset, @Min(0) @Max(2147483647) @Valid Integer limit, @Valid String query) {
    return null;
  }

  @Override
  public ResponseEntity<ValidationRule> postTenantRules(@Valid ValidationRule validationRule) {
    return null;
  }

  @Override
  public ResponseEntity<ValidationRule> putTenantRule(@Valid ValidationRule validationRule) {
    return null;
  }
}

package org.folio.pv.rest.resource;

import org.apache.commons.lang3.StringUtils;
import org.folio.pv.domain.dto.ValidationRule;
import org.folio.pv.service.ValidationRuleCRUDService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Path("/tenant/rules")
public class RulesApiResource implements RulesApi {

  private ValidationRuleCRUDService validationRuleCRUDService;

  @Inject
  public void init(ValidationRuleCRUDService validationRuleCRUDService) {
    this.validationRuleCRUDService = validationRuleCRUDService;
  }

  @Override
  public Response getTenantRuleById(String ruleId) {
    return Response.ok(validationRuleCRUDService.getTenantRuleById(ruleId)).build();
  }

  @Override
  public Response getTenantRules(@Min(0) @Max(2147483647) Integer offset, @Min(0) @Max(2147483647) Integer limit, String query) {
    return Response.ok(validationRuleCRUDService.getValidationRules(offset, limit, "")).build();
  }

  @Override
  public Response postTenantRules(@Valid ValidationRule validationRule) {
    return Response.ok(validationRuleCRUDService.createOrUpdateValidationRule(validationRule)).build();
  }

  @Override
  public Response putTenantRule(@Valid ValidationRule validationRule) {
    return StringUtils.isBlank(validationRule.getId()) ?
      Response.status(BAD_REQUEST).entity("Id can not be null or empty").build() :
      Response.ok(validationRuleCRUDService.createOrUpdateValidationRule(validationRule)).build();
  }
}

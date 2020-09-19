package org.folio.spring.controller;

import liquibase.exception.LiquibaseException;
import lombok.extern.slf4j.Slf4j;
import org.folio.rest.dto.TenantAttributes;
import org.folio.rest.resources.TenantApi;
import org.folio.spring.FolioExecutionContextService;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.Objects;

@Slf4j
@RequestMapping(value = "/_/")
@ResponseBody
public class TenantController implements TenantApi {

  private final FolioSpringLiquibase folioSpringLiquibase;

  private final FolioExecutionContextService contextService;

  public TenantController(FolioSpringLiquibase folioSpringLiquibase,
                          FolioExecutionContextService contextService) {
    this.folioSpringLiquibase = folioSpringLiquibase;
    this.contextService = contextService;
  }

  @Override
  public ResponseEntity<String> postTenant(@Valid TenantAttributes tenantAttributes) {
    if (Objects.nonNull(folioSpringLiquibase)) {
      var folioExecutionContext = contextService.getFolioExecutionContext();
      var tenantId = folioExecutionContext.getTenantId();

      var schemaName = folioExecutionContext.getFolioModuleMetadata().getDBSchemaName(tenantId);

      folioSpringLiquibase.setDefaultSchema(schemaName);
      try {
        folioSpringLiquibase.performLiquibaseUpdate();
      } catch (LiquibaseException e) {
        e.printStackTrace();
        log.error("Liquibase error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Liquibase error: " + e.getMessage());
      }
    }
    return ResponseEntity.ok().body("true");
  }
}

package org.folio.pv.rest.resource;

import org.folio.pv.domain.dto.TenantAttributes;
import org.folio.pv.service.MigrationService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/_/tenant")
public class TenantApiResource implements TenantApi {

  private MigrationService migrationService;

  @Inject
  public void init(MigrationService migrationService) {
    this.migrationService = migrationService;
  }

  @Override
  public Response deleteTenant() {
    return null;
  }

  @Override
  public Response getTenant() {
    return null;
  }

  @Override
  public Response postTenant(@Valid TenantAttributes tenantAttributes) {
    migrationService.performMigration();
    return Response.ok(Boolean.TRUE).build();
  }
}

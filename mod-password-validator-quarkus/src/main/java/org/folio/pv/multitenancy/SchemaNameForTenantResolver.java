package org.folio.pv.multitenancy;

import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.vertx.ext.web.RoutingContext;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class SchemaNameForTenantResolver implements TenantResolver {

  private RoutingContext context;

  @Inject
  public void init(RoutingContext context) {
    this.context = context;
  }

  @Override
  public String getDefaultTenantId() {
    return "public";
  }

  @Override
  public String resolveTenantId() {
    var header = context.request().getHeader("x-okapi-tenant");
    return header + "_mod_password_validator";
  }
}

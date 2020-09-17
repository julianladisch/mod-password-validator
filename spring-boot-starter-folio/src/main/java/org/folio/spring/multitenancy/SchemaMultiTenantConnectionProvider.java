package org.folio.spring.multitenancy;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class SchemaMultiTenantConnectionProvider
  extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl implements
  ServiceRegistryAwareService {

  public DataSource dataSource;

  @Override
  protected DataSource selectAnyDataSource() {
    return selectDataSource(null);
  }

  @Override
  protected DataSource selectDataSource(String tenantIdentifier) {
    return dataSource;
  }

  @Override
  public void injectServices(ServiceRegistryImplementor serviceRegistry) {
    dataSource = (DataSource) serviceRegistry.getService(ConfigurationService.class)
      .getSettings()
      .get(AvailableSettings.DATASOURCE);
  }

  @Override
  public Connection getConnection(String tenantIdentifier)
    throws SQLException {

    Connection connection = super.getConnection(tenantIdentifier);
    if (!TenantIdentifierResolver.DEFAULT_TENANT.equals(tenantIdentifier)) {
      connection.createStatement()
        .execute(String.format("SET search_path = %s;", tenantIdentifier));
    }
    return connection;
  }

}

package org.folio.spring.config;

import org.apache.logging.log4j.util.Strings;
import org.folio.spring.FolioExecutionContextService;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public class DataSourceFolioWrapper extends DelegatingDataSource {
  private final FolioExecutionContextService folioExecutionContextService;

  public DataSourceFolioWrapper(DataSource targetDataSource, FolioExecutionContextService folioExecutionContextService) {
    super(targetDataSource);
    this.folioExecutionContextService = folioExecutionContextService;
  }

  private Connection prepareConnectionSafe(Connection connection) throws SQLException {
    if (Objects.nonNull(connection)) {
      var folioExecutionContext = folioExecutionContextService.getFolioExecutionContext();

      var tenantId = folioExecutionContext.getTenantId();
      connection.prepareStatement(
        String.format(
          "SET search_path = %s;",
          Strings.isBlank(tenantId) ? "public" : folioExecutionContext.getFolioModuleMetadata().getDBSchemaName(tenantId) + ", public")
      ).execute();

      return connection;
    }
    return null;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return prepareConnectionSafe(obtainTargetDataSource().getConnection());
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return prepareConnectionSafe(obtainTargetDataSource().getConnection(username, password));
  }
}

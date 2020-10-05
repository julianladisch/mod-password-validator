package org.folio.pv.service;

import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.quarkus.liquibase.LiquibaseFactory;
import liquibase.Liquibase;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.SQLException;

@Slf4j
@ApplicationScoped
public class MigrationService {

  private TenantResolver tenantResolver;
  private LiquibaseFactory liquibaseFactory;
  private DataSource dataSource;

  @Inject
  public void init(TenantResolver tenantResolver, LiquibaseFactory liquibaseFactory, DataSource dataSource) {
    this.tenantResolver = tenantResolver;
    this.liquibaseFactory = liquibaseFactory;
    this.dataSource = dataSource;
  }

  public void performMigration() {
    var schemaName = tenantResolver.resolveTenantId();

    try (var connection = dataSource.getConnection()) {
      try (var statement = connection.createStatement()) {
        statement.execute("create schema if not exists " + schemaName + ";");
      }
    } catch (SQLException e) {
      e.printStackTrace();
      log.error("Default schema " + schemaName + " has not been created.", e);
    }

    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
      liquibase.getDatabase().setDefaultSchemaName(schemaName);
      liquibase.update(liquibaseFactory.createContexts());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}

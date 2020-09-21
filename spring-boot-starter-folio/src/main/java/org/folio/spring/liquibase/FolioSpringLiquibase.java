package org.folio.spring.liquibase;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

import java.sql.SQLException;

@Slf4j
public class FolioSpringLiquibase extends SpringLiquibase {

  @Override
  public void afterPropertiesSet() {
    //just suppress liquibase auto-execution
  }

  public void performLiquibaseUpdate() throws LiquibaseException {
    var defaultSchema = getDefaultSchema();
    if (Strings.isNotBlank(defaultSchema)) {
      try (var c = getDataSource().getConnection()) {
        c.createStatement().execute("create schema if not exists " + defaultSchema + ";");
      } catch (SQLException e) {
        e.printStackTrace();
        log.error("Default schema " + defaultSchema + " has not been created.", e);
      }
    }

    super.afterPropertiesSet();
  }

}

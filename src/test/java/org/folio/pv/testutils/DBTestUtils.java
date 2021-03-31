package org.folio.pv.testutils;


import static org.folio.pv.testutils.APITestUtils.TENANT_ID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import lombok.experimental.UtilityClass;
import org.springframework.jdbc.core.JdbcTemplate;

import org.folio.pv.domain.entity.PasswordValidationRule;
import org.folio.spring.FolioModuleMetadata;

@UtilityClass
public class DBTestUtils {

  public static final String VALIDATION_RULES_TABLE_NAME = "validationrules";

  public static PasswordValidationRule getValidationRuleById(UUID id, FolioModuleMetadata metadata,
                                                             JdbcTemplate jdbcTemplate) {
    var sql = "SELECT * FROM " + validationRulesTable(TENANT_ID, metadata) + " WHERE id = ?";
    return jdbcTemplate.query(sql, new Object[] {id}, rs -> {
      rs.next();
      var rule = new PasswordValidationRule();
      rule.setId(getUuid("id", rs));
      rule.setRuleState(rs.getString("rule_state"));
      rule.setRuleType(rs.getString("rule_type"));
      rule.setValidationType(rs.getString("validation_type"));
      rule.setName(rs.getString("name"));
      rule.setErrMessageId(rs.getString("err_message_id"));
      rule.setRuleExpression(rs.getString("rule_expression"));
      rule.setCreatedDate(rs.getTimestamp("created_date"));
      rule.setUpdatedDate(rs.getTimestamp("updated_date"));
      rule.setOrderNo(rs.getInt("order_no"));
      return rule;
    });
  }

  public static String validationRulesTable(String tenantId, FolioModuleMetadata metadata) {
    return getTableName(VALIDATION_RULES_TABLE_NAME, tenantId, metadata);
  }

  public static String getTableName(String tableName, String tenantId, FolioModuleMetadata metadata) {
    return metadata.getDBSchemaName(tenantId) + "." + tableName;
  }

  private static UUID getUuid(String columnLabel, ResultSet rs) throws SQLException {
    var string = rs.getString(columnLabel);
    return string == null ? null : UUID.fromString(string);
  }
}

package org.folio.pv.testutils;

import java.nio.charset.StandardCharsets;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.text.StringRandomizer;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import org.folio.pv.domain.RuleType;
import org.folio.pv.domain.entity.PasswordValidationRule;
import org.folio.spring.FolioModuleMetadata;

public class RandomTestData {

  private static final EnhancedRandom ruleRandomizer;
  private static final StringRandomizer moduleNameRandomizer;

  static {
    ruleRandomizer = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .objectPoolSize(100)
        .charset(StandardCharsets.UTF_8)
        .stringLengthRange(5, 50)
        .overrideDefaultInitialization(false)
        .build();

    moduleNameRandomizer = StringRandomizer.aNewStringRandomizer(10);
  }

  private RandomTestData() {
  }

  public static PasswordValidationRule nextRandomRuleOfType(RuleType type) {
    PasswordValidationRule result = ruleRandomizer.nextObject(PasswordValidationRule.class);
    result.setRuleType(type.getValue());

    return result;
  }
  
  public static FolioModuleMetadata nextRandomModuleMetadata() {
    return new FolioModuleMetadataImpl(moduleNameRandomizer.getRandomValue());
  }
  
  @Value
  private static class FolioModuleMetadataImpl implements FolioModuleMetadata {

    String moduleName;
    
    @Override
    public String getDBSchemaName(String tenantId) {
      if (StringUtils.isBlank(tenantId)) {
        throw new IllegalArgumentException("tenantId can't be null or empty");
      }

      return tenantId + (StringUtils.isNotBlank(moduleName) ? "_" + moduleName.toLowerCase().replace('-', '_') : "");
    }
  }
}
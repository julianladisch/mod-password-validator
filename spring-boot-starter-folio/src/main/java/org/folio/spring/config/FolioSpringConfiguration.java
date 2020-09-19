package org.folio.spring.config;

import org.apache.logging.log4j.util.Strings;
import org.folio.rest.resources.TenantApi;
import org.folio.spring.FolioExecutionContextService;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.controller.TenantController;
import org.folio.spring.foliocontext.DefaultFolioExecutionContextService;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.folio.spring.controller")
public class FolioSpringConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public FolioModuleMetadata folioModuleMetadata(@Value("${spring.application.name}") String applicationName) {
    var schemaSuffix = Strings.isNotBlank(applicationName) ? "_" + applicationName.replaceAll("-", "_") : "";
    return new FolioModuleMetadata() {
      @Override
      public String getModuleName() {
        return applicationName;
      }

      @Override
      public String getDBSchemaName(String tenantId) {
        if (Strings.isBlank(tenantId)) {
          throw new IllegalArgumentException("tenantId can't be null or empty");
        }
        return tenantId + schemaSuffix;
      }
    };
  }

  @Bean
  @ConditionalOnMissingBean
  public FolioExecutionContextService folioExecutionContextService(@Autowired FolioModuleMetadata folioModuleMetadata) {
    return new DefaultFolioExecutionContextService(folioModuleMetadata);
  }

  @Bean
  @Qualifier("dataSourceSchemaAdvisorBeanPostProcessor")
  public BeanPostProcessor dataSourceSchemaAdvisorBeanPostProcessor(@Autowired FolioExecutionContextService folioExecutionContextService) {
    return new DataSourceSchemaAdvisorBeanPostProcessor(folioExecutionContextService);
  }


  @Bean
  @ConditionalOnMissingBean
  public TenantApi defaultTenantController(@Autowired(required = false) FolioSpringLiquibase folioSpringLiquibase,
                                           @Autowired FolioExecutionContextService contextService) {
    return new TenantController(folioSpringLiquibase, contextService);
  }


}

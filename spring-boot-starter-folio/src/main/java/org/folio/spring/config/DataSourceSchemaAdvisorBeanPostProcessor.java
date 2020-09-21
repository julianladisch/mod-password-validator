package org.folio.spring.config;

import org.folio.spring.FolioExecutionContextService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;

public class DataSourceSchemaAdvisorBeanPostProcessor implements BeanPostProcessor {
  private final FolioExecutionContextService folioExecutionContextService;

  public DataSourceSchemaAdvisorBeanPostProcessor(FolioExecutionContextService folioExecutionContextService) {
    this.folioExecutionContextService = folioExecutionContextService;
  }

  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if ("dataSource".equals(beanName)) {
      return new DataSourceFolioWrapper((DataSource) bean, folioExecutionContextService);
    } else {
      return bean;
    }
  }
}

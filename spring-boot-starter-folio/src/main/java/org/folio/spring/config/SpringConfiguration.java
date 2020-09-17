package org.folio.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.spring.async.AsyncService;
import org.folio.spring.client.EnrichHeadersClient;
import org.folio.spring.integration.OkapiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public AsyncService async() {
    return new AsyncService();
  }

  @Bean
  @ConditionalOnMissingBean
  public OkapiService okapiService(@Autowired ObjectMapper objectMapper,
                                   @Autowired AsyncService async) {
    return new OkapiService(objectMapper, async);
  }

  @Bean
  @ConditionalOnMissingBean
  public EnrichHeadersClient feignClient(@Autowired OkapiService okapiService) {
    return new EnrichHeadersClient(okapiService);
  }

}

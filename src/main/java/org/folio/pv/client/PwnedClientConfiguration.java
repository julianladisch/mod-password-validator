package org.folio.pv.client;

import feign.Client;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.okhttp.OkHttpClient;
import java.util.ArrayList;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.clientconfig.OkHttpFeignConfiguration;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;

@Import(OkHttpFeignConfiguration.class)
public class PwnedClientConfiguration {

  @Autowired
  private ObjectFactory<HttpMessageConverters> messageConverters;

  @Bean
  public Client feignClient(okhttp3.OkHttpClient okHttpClient) {
    return new OkHttpClient(okHttpClient);
  }

  @Bean
  public Decoder feignDecoder () {
    var springConverters = messageConverters.getObject().getConverters();
    var decoderConverters = new ArrayList<HttpMessageConverter<?>>(springConverters.size() + 1);

    decoderConverters.addAll(springConverters);
    decoderConverters.add(new HashedPasswordUsageCollectionConverter<>());

    var httpMessageConverters = new HttpMessageConverters(decoderConverters);

    return new SpringDecoder(() -> httpMessageConverters);
  }

  @Bean
  public RequestInterceptor addPaddingInterceptor(
      @Value("${pwned-passwords.padding.enabled}") boolean paddingEnabled) {
    return new PwnedClientAddPaddingInterceptor(paddingEnabled);
  }

}
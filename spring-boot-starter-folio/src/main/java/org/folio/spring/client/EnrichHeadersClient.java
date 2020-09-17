package org.folio.spring.client;

import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.folio.spring.integration.OkapiService;

import java.util.Collection;
import java.util.Map;

public class EnrichHeadersClient extends Client.Default {

  private final OkapiService okapiService;

  public EnrichHeadersClient(OkapiService okapiService) {
    super(null, null);
    this.okapiService = okapiService;
  }

  @Override
  @SneakyThrows
  public Response execute(Request request, Options options) {
    Map<String, Collection<String>> headers = okapiService.getOkapiHeaders();
    headers.putAll(request.headers());
    FieldUtils.writeDeclaredField(request, "headers", headers, true);
    FieldUtils.writeDeclaredField(request, "url",
      request.url().replace("http://", okapiService.getOkapiGatewayUrl()), true);
    return super.execute(request, options);
  }

}

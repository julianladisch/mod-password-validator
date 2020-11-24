package org.folio.pv.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.pv.domain.dto.HashedPasswordUsage;

@FeignClient(name = "pwned-passwords",
    url = "${pwned-passwords.client.url}",
    configuration = PwnedClientConfiguration.class)
public interface PwnedClient {

  @GetMapping(path = "/range/{hashPrefix}", produces = MediaType.TEXT_PLAIN_VALUE)
  List<HashedPasswordUsage> getPwdRange(@PathVariable String hashPrefix);

}

package org.folio.pv.service;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.folio.pv.client.PwnedClient;
import org.folio.pv.domain.dto.Password;

@Service
@Log4j2
public class PwnedPasswordsService {

  private final PwnedClient pwnedClient;

  public PwnedPasswordsService(@Autowired PwnedClient pwnedClient) {
    this.pwnedClient = pwnedClient;
  }

  public boolean validate(Password passwordContainer) {
    var hash = DigestUtils.sha1Hex(passwordContainer.getPassword());
    var suffix = StringUtils.left(hash, 5);

    var response = pwnedClient.getPwdRange(suffix);

    return !response.isEmpty();
  }
}
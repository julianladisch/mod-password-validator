package org.folio.pv.service;

import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.pv.client.PwnedClient;
import org.folio.pv.domain.dto.HashedPasswordUsage;
import org.folio.pv.domain.dto.Password;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class PwnedPasswordsService {

  private final PwnedClient pwnedClient;


  public PwnedPasswordsService(@Autowired PwnedClient pwnedClient) {
    this.pwnedClient = pwnedClient;
  }

  public boolean validate(Password passwordContainer) {
    String hash = DigestUtils.sha1Hex(passwordContainer.getPassword()).toUpperCase();
    String hashPrefix = StringUtils.left(hash, 5);
    String hashSuffix = hash.substring(5);

    log.debug("Checking password with prefix: {}", hashPrefix);

    List<HashedPasswordUsage> usages = pwnedClient.getPwdRange(hashPrefix);

    Optional<HashedPasswordUsage> knownHash = usages.stream()
        .filter(usage -> usage.getSuffix().equals(hashSuffix) && usage.getUsageCount() > 0)
        .findFirst();

    log.info("Pwned Passwords validation: usageCount = {}",
        knownHash.map(HashedPasswordUsage::getUsageCount).orElse(0));

    return knownHash.isEmpty();
  }
}
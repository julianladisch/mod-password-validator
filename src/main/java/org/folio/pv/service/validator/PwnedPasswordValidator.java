package org.folio.pv.service.validator;

import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import org.folio.pv.client.PwnedClient;
import org.folio.pv.domain.dto.HashedPasswordUsage;
import org.folio.pv.domain.entity.PasswordValidationRule;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Log4j2
class PwnedPasswordValidator implements Validator {

  private final PasswordValidationRule rule;
  private final PwnedClient pwnedClient;


  @Override
  public ValidationErrors validate(String password, UserData user) {
    if (StringUtils.isBlank(password)) {
      return ValidationErrors.none();
    }
    
    var hash = DigestUtils.sha1Hex(password).toUpperCase();
    var hashPrefix = StringUtils.left(hash, 5);
    var hashSuffix = hash.substring(5);

    log.debug("Checking password with prefix: {}", hashPrefix);

    List<HashedPasswordUsage> usages = pwnedClient.getPwdRange(hashPrefix);

    Optional<HashedPasswordUsage> knownHash = usages.stream()
        .filter(usage -> usage.getSuffix().equals(hashSuffix) && usage.getUsageCount() > 0)
        .findFirst();

    log.info("Pwned Passwords validation: usageCount = {}",
        knownHash.map(HashedPasswordUsage::getUsageCount).orElse(0));

    return knownHash.isEmpty() ? ValidationErrors.none() : ValidationErrors.of(rule.getErrMessageId());
  }

}
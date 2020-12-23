package org.folio.pv.service.validator;

import lombok.Value;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

@Value
class PasswordHash {

  String hash;
  String prefix;
  String suffix;

  PasswordHash(String password) {
    hash = DigestUtils.sha1Hex(password).toUpperCase();
    prefix = StringUtils.left(hash, 5);
    suffix = hash.substring(5);
  }

}
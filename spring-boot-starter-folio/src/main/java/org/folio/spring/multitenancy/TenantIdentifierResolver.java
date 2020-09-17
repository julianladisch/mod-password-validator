package org.folio.spring.multitenancy;

import org.folio.spring.integration.XOkapiHeaders;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {

  public static final String DEFAULT_TENANT = "default";
  private static final String MOD_USERS_NAME = "_mod_users_poc";

  @Override
  public String resolveCurrentTenantIdentifier() {
    RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
    if (!(attribs instanceof ServletRequestAttributes)) {
      return DEFAULT_TENANT + MOD_USERS_NAME;
    }
    return ((ServletRequestAttributes) attribs).getRequest().getHeader(XOkapiHeaders.TENANT)
      + MOD_USERS_NAME;
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return false;
  }
}

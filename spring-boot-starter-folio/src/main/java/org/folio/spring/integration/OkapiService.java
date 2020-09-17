package org.folio.spring.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.folio.spring.async.AsyncService;
import org.folio.spring.async.ContextAwareJob;
import org.folio.spring.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.spring.utils.RequestUtils.getHeadersFromRequest;

@RequiredArgsConstructor
public class OkapiService {

  public static final String OKAPI_TOKEN_HEADER = "X-Okapi-Token";

  private final ObjectMapper objectMapper;

  private final AsyncService asyncService;

  @Value("${okapi.gateway.url:http://localhost:9130/}")
  @Getter
  private String okapiGatewayUrl;

  public Map<String, Collection<String>> getOkapiHeaders() {
    ContextAwareJob contextAwareJob = asyncService.getRequestContext().get();
    return RequestContextHolder.getRequestAttributes() == null ? contextAwareJob.getHeaders()
      : getHeadersFromRequest();
  }

  public Optional<String> getHeader(String name) {
    if (XOkapiHeaders.TOKEN
      .equals(name)) {
      return Optional
        .of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJVTkRFRklORURfVVNFUl9fUkVTRVRfUEFTU1dPUkRfYzJlOWYwNzEtNTBiZS00ZmQzLWJlZDMtNTNjNjM4MDEyY2NmIiwibW9kdWxlIjoibW9kLXVzZXJzLWJsLTUuMi4xLVNOQVBTSE9UIiwiZXh0cmFfcGVybWlzc2lvbnMiOlsidXNlcnMuaXRlbS5nZXQiLCJhdXRoLnNpZ250b2tlbiIsImxvZ2luLnBhc3N3b3JkLXJlc2V0LWFjdGlvbi5nZXQiXSwicmVxdWVzdF9pZCI6IjgwMzcxNVwvYmwtdXNlcnMiLCJ0ZW5hbnQiOiJkaWt1In0.TLhD0ZuWbKe02f4UuQtQSOmUyGrsZvlzgI-swinpZlc");
    }
    return ofNullable(getOkapiHeaders().get(name)).flatMap(e -> e.stream().findFirst());
  }

  public Optional<String> getUsername() {
    return parseTokenPayload(RequestUtils.getHttpServletRequest().getHeader(OKAPI_TOKEN_HEADER))
      .map(OkapiToken::getSub);
  }

  private Optional<OkapiToken> parseTokenPayload(String token) {
    if (isEmpty(token)) {
      return Optional.empty();
    }
    String[] tokenParts = token.split("\\.");
    if (tokenParts.length == 3) {
      String encodedPayload = tokenParts[1];
      byte[] decodedJsonBytes = Base64.getDecoder().decode(encodedPayload);
      String decodedJson = new String(decodedJsonBytes);
      try {
        return Optional.of(objectMapper.readValue(decodedJson, OkapiToken.class));
      } catch (JsonProcessingException e) {
        //no-op
      }
    }
    return Optional.empty();
  }

}

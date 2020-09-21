package org.folio.spring.utils;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RequestUtils {

  private RequestUtils() {
  }

  public static HttpServletRequest getHttpServletRequest() {
    var requestAttributes = RequestContextHolder.getRequestAttributes();
    return (requestAttributes instanceof ServletRequestAttributes) ? ((ServletRequestAttributes) requestAttributes).getRequest() : null;
  }

  public static Map<String, Collection<String>> getHttpHeadersFromRequest() {
    return getHttpHeadersFromRequest(getHttpServletRequest());
  }

  public static Map<String, Collection<String>> getHttpHeadersFromRequest(HttpServletRequest request) {
    return (Objects.nonNull(request)) ?
      Collections
        .list(request.getHeaderNames())
        .stream()
        .collect(Collectors.toMap(
          String::toLowerCase,
          h -> Collections.list(request.getHeaders(h)),
          (a, b) -> {
            a.addAll(b);
            return a;
          }
        )) : null;
  }

}

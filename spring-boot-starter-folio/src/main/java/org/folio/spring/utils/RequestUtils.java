package org.folio.spring.utils;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RequestUtils {

  public static final String OKAPI_HEADERS_PREFIX = "x-okapi";

  private RequestUtils() {
  }

  public static Map<String, Collection<String>> getHeadersFromRequest() {
    return getHeadersFromRequest(getHttpServletRequest());
  }

  public static HttpServletRequest getHttpServletRequest() {
    return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
      .map(e -> ((ServletRequestAttributes) e).getRequest())
      .orElseThrow(() -> new IllegalStateException(
        "getHeadersFromRequest is called in thread, that isn't aware about request."));
  }

  public static Map<String, Collection<String>> getHeadersFromRequest(HttpServletRequest request) {
    return Collections
      .list(request.getHeaderNames())
      .stream()
      .filter(h -> h.startsWith(OKAPI_HEADERS_PREFIX))
      .collect(Collectors.toMap(
        Function.identity(),
        h -> Collections.list(request.getHeaders(h))
      ));
  }

}

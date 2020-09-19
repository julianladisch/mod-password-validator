package org.folio.spring.foliocontext;

import lombok.extern.slf4j.Slf4j;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioExecutionContextService;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.utils.RequestUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.folio.spring.integration.XOkapiHeaders.OKAPI_HEADERS_PREFIX;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;

@Slf4j
public class DefaultFolioExecutionContextService implements FolioExecutionContextService {
  private final ThreadLocal<FolioExecutionContext> folioExecutionContextThreadLocal = new ThreadLocal<>();

  private final FolioModuleMetadata folioModuleMetadata;

  private final FolioExecutionContext emptyFolioExecutionContext = new EmptyFolioExecutionContext();

  public DefaultFolioExecutionContextService(FolioModuleMetadata folioModuleMetadata) {
    this.folioModuleMetadata = folioModuleMetadata;
  }

  @Override
  public FolioExecutionContext getFolioExecutionContext() {
    return getOrCreateFolioExecutionContext();
  }

  @Override
  public void contextBegin(FolioExecutionContext context) {
    folioExecutionContextThreadLocal.set(context);
  }

  @Override
  public FolioExecutionContext contextEnd() {
    var context = folioExecutionContextThreadLocal.get();
    folioExecutionContextThreadLocal.remove();
    return context;
  }

  private FolioExecutionContext getOrCreateFolioExecutionContext() {
    var context = folioExecutionContextThreadLocal.get();
    return (Objects.nonNull(context)) ? context : createFolioExecutionContextFromRequest();
  }

  private FolioExecutionContext createFolioExecutionContextFromRequest() {
    var httpHeadersFromRequest = RequestUtils.getHttpHeadersFromRequest();
    return Objects.nonNull(httpHeadersFromRequest) ?
      new DefaultFolioExecutionContext(httpHeadersFromRequest) : emptyFolioExecutionContext;
  }

  private class DefaultFolioExecutionContext implements FolioExecutionContext {

    private final Map<String, Collection<String>> allHeaders;
    private final Map<String, Collection<String>> okapiHeaders;

    private final String tenantId;
    private final String okapiUrl;
    private final String token;
    private final String userName;

    private DefaultFolioExecutionContext(Map<String, Collection<String>> allHeaders) {
      this.allHeaders = allHeaders;
      this.okapiHeaders = new HashMap<>(allHeaders);
      this.okapiHeaders.entrySet().removeIf(e -> !e.getKey().toLowerCase().startsWith(OKAPI_HEADERS_PREFIX));

      this.tenantId = retrieveFirstSafe(okapiHeaders.get(TENANT));
      this.okapiUrl = retrieveFirstSafe(okapiHeaders.get(URL));
      this.token = retrieveFirstSafe(okapiHeaders.get(TOKEN));
      this.userName = null; //TODO: retrieve user name
    }

    private String retrieveFirstSafe(Collection<String> strings) {
      return (Objects.nonNull(strings) && !strings.isEmpty()) ? strings.iterator().next() : "";
//      return ofNullable(strings).flatMap(s -> s.stream().findFirst()).orElse("");
    }

    @Override
    public String getTenantId() {
      return tenantId;
    }

    @Override
    public String getOkapiUrl() {
      return okapiUrl;
    }

    @Override
    public String getToken() {
      return token;
    }

    @Override
    public String getUserName() {
      return userName;
    }

    @Override
    public Map<String, Collection<String>> getAllHeaders() {
      return allHeaders;
    }

    @Override
    public Map<String, Collection<String>> getOkapiHeaders() {
      return okapiHeaders;
    }

    @Override
    public FolioModuleMetadata getFolioModuleMetadata() {
      return folioModuleMetadata;
    }
  }

  private class EmptyFolioExecutionContext implements FolioExecutionContext {

    @Override
    public String getTenantId() {
      return null;
    }

    @Override
    public String getOkapiUrl() {
      return null;
    }

    @Override
    public String getToken() {
      return null;
    }

    @Override
    public String getUserName() {
      return null;
    }

    @Override
    public Map<String, Collection<String>> getAllHeaders() {
      return null;
    }

    @Override
    public Map<String, Collection<String>> getOkapiHeaders() {
      return null;
    }

    @Override
    public FolioModuleMetadata getFolioModuleMetadata() {
      return folioModuleMetadata;
    }
  }

}

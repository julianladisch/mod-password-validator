package org.folio.spring.scope.filter;

import lombok.extern.slf4j.Slf4j;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.folio.spring.utils.RequestUtils.getHttpHeadersFromRequest;

@Slf4j
public class FolioExecutionScopeFilter extends GenericFilterBean {
  private final FolioModuleMetadata folioModuleMetadata;

  public FolioExecutionScopeFilter(FolioModuleMetadata folioModuleMetadata) {
    this.folioModuleMetadata = folioModuleMetadata;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (request instanceof HttpServletRequest) {
      var httpHeaders = getHttpHeadersFromRequest((HttpServletRequest) request);
      var defaultFolioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, httpHeaders);
      FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(defaultFolioExecutionContext);
      try {
        chain.doFilter(request, response);
      } finally {
        FolioExecutionScopeExecutionContextManager.endFolioExecutionContext();
      }
    }
  }


}

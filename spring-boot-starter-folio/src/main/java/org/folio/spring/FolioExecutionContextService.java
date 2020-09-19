package org.folio.spring;

public interface FolioExecutionContextService {
  FolioExecutionContext getFolioExecutionContext();

  void contextBegin(FolioExecutionContext context);

  FolioExecutionContext contextEnd();
}

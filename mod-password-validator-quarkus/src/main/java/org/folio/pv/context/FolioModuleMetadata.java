package org.folio.pv.context;

public interface FolioModuleMetadata {
  String getModuleName();

  String getDBSchemaName(String tenantId);
}

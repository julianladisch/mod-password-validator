package org.folio.pv.domain.dto;

import lombok.Value;

@Value
public class HashedPasswordUsage {

  String suffix;
  int usageCount;
}
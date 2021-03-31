package org.folio.pv.domain.dto;

import java.util.Comparator;

import lombok.Value;

@Value
public class HashedPasswordUsage implements Comparable<HashedPasswordUsage> {

  String suffix;
  int usageCount;

  @Override
  public int compareTo(HashedPasswordUsage o) {
    return Comparator.comparingInt(HashedPasswordUsage::getUsageCount)
      .thenComparing(HashedPasswordUsage::getSuffix)
      .compare(o, this);
  }
}
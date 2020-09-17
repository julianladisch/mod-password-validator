package org.folio.spring.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * A set of errors
 */
@Data
public class ErrorsDto {

  private List<ErrorDto> errors = null;

  @JsonProperty("total_records")
  private Integer totalRecords = null;
}


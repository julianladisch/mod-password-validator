package org.folio.spring.integration;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ErrorDto {

  private String message;

  private String type;

  private String code;

  private List<Map<String, Object>> parameters;

}


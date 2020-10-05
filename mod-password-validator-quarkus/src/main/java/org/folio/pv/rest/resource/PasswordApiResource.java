package org.folio.pv.rest.resource;

import org.folio.pv.domain.dto.Password;

import javax.validation.Valid;
import javax.ws.rs.core.Response;

public class PasswordApiResource implements PasswordApi {
  @Override
  public Response validatePassword(@Valid Password password) {
    return Response.ok("public Response validatePassword(@Valid Password password)").build();
  }
}

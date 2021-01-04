package org.folio.pv.service.validator;

public interface Validator {

  ValidationErrors validate(String password, UserData user);
}

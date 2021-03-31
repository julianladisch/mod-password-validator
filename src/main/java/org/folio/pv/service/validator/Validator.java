package org.folio.pv.service.validator;

import org.folio.pv.domain.dto.UserData;
import org.folio.pv.domain.dto.ValidationErrors;

public interface Validator {

  ValidationErrors validate(String password, UserData user);
}

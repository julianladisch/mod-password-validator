package org.folio.pv.service.validator;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.folio.pv.domain.entity.PasswordValidationRule;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Log4j2
public class RegExpValidator implements Validator {

  private static final String REGEXP_USER_NAME_PLACEHOLDER = "<USER_NAME>";

  private final PasswordValidationRule rule;


  @Override
  public ValidationErrors validate(String password, UserData user) {
    var expression = rule.getRuleExpression();

    var failed = false;
    if (isNotBlank(expression)) {
      var exprWithUser = expression.replace(REGEXP_USER_NAME_PLACEHOLDER, user.getName());
      log.info("Validating password against regexp: {}", exprWithUser);

      var pattern = Pattern.compile(exprWithUser);

      failed = pattern.matcher(password).matches();
      log.info("Password matching result: {}", failed);
    }

    return failed ? ValidationErrors.of(rule.getErrMessageId()) : ValidationErrors.none();
  }

}
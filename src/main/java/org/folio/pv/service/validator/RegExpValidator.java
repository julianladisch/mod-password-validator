package org.folio.pv.service.validator;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.regex.Pattern;

import org.folio.pv.domain.entity.PasswordValidationRule;

public class RegExpValidator implements Validator {

  private static final String REGEXP_USER_NAME_PLACEHOLDER = "<USER_NAME>";

  private final PasswordValidationRule rule;


  public RegExpValidator(PasswordValidationRule rule) {
    this.rule = rule;
  }

  @Override
  public ValidationErrors validate(String password, UserData user) {
    var expression = rule.getRuleExpression();

    var failed = false;
    if (isNotBlank(expression)) {
      var pattern = Pattern.compile(expression.replace(REGEXP_USER_NAME_PLACEHOLDER, user.getName()));

      failed = pattern.matcher(password).matches();
    }

    return failed ? ValidationErrors.of(rule.getErrMessageId()) : ValidationErrors.none();
  }

}
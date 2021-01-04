package org.folio.pv.service.validator;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import static org.folio.pv.testutils.RandomTestData.nextRandomRuleOfType;

import io.github.glytching.junit.extension.random.Random;
import io.github.glytching.junit.extension.random.RandomBeansExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.pv.client.PwnedClient;
import org.folio.pv.domain.RuleType;
import org.folio.pv.domain.dto.HashedPasswordUsage;
import org.folio.pv.domain.entity.PasswordValidationRule;

@ExtendWith({
    MockitoExtension.class,
    RandomBeansExtension.class
})
class PwnedPasswordValidatorTest {

  @Random
  private UserData userData;

  private PasswordValidationRule rule;
  @Mock
  private PwnedClient pwnedClient;
  private PwnedPasswordValidator validator;


  @BeforeEach
  void setUp() {
    rule = nextRandomRuleOfType(RuleType.PWNEDPASSWORD);

    validator = new PwnedPasswordValidator(rule, pwnedClient);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldIgnoreEmptyPassword(String password) {
    ValidationErrors errors = validator.validate(password, userData);

    assertFalse(errors.hasErrors());
  }

  @Test
  void shouldPassIfPasswordUsageIsZero(@Random String password) {
    PasswordHash hash = new PasswordHash(password);

    when(pwnedClient.getPwdRange(hash.getPrefix()))
        .thenReturn(singletonList(new HashedPasswordUsage(hash.getSuffix(), 0)));

    ValidationErrors errors = validator.validate(password, userData);

    assertFalse(errors.hasErrors());
  }

  @Test
  void shouldPassIfNoPasswordUsageFound(@Random String password) {
    PasswordHash hash = new PasswordHash(password);

    when(pwnedClient.getPwdRange(hash.getPrefix())).thenReturn(emptyList());

    ValidationErrors errors = validator.validate(password, userData);

    assertFalse(errors.hasErrors());
  }

  @Test
  void shouldReturnErrorWithMessageIdIfPasswordUsageFound(@Random String password) {
    PasswordHash hash = new PasswordHash(password);

    when(pwnedClient.getPwdRange(hash.getPrefix()))
        .thenReturn(singletonList(new HashedPasswordUsage(hash.getSuffix(), 1)));

    ValidationErrors errors = validator.validate(password, userData);

    Assertions.assertAll(
        () -> assertTrue(errors.hasErrors()),
        () -> assertThat(errors.getErrorMessages()).containsExactly(rule.getErrMessageId())
    );
  }
  
}
package org.folio.pv.service.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.glytching.junit.extension.exception.ExpectedException;
import io.github.glytching.junit.extension.random.Random;
import io.github.glytching.junit.extension.random.RandomBeansExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.pv.client.PwnedClient;
import org.folio.pv.domain.RuleType;
import org.folio.pv.domain.entity.PasswordValidationRule;
import org.folio.spring.FolioExecutionContext;

@ExtendWith({
    MockitoExtension.class,
    RandomBeansExtension.class
})
class ValidatorRegistryImplTest {

  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ObjectMapper jacksonObjectMapper;
  @Mock
  private PwnedClient pwnedClient; 

  @InjectMocks
  private ValidatorRegistryImpl registry;


  @Test
  @ExpectedException(type = NullPointerException.class, messageIs = "Validation rule is null")
  void shouldFailWithSpecificNPEIfRuleIsNull() {
    registry.validatorByRule(null);
  }

  @Test
  @ExpectedException(type = IllegalArgumentException.class, messageContains = "Unexpected value")
  void shouldFailIfRuleTypeIsInvalid(@Random String ruleType) {
    PasswordValidationRule rule = mockedRuleWithType(ruleType);

    registry.validatorByRule(rule);
  }

  @ParameterizedTest
  @MethodSource("validatorPerRuleProvider")
  void shouldReturnTheCorrectValidatorPerRuleType(PasswordValidationRule rule,
      Class<Validator> expectedValidatorClass) {
    Validator validator = registry.validatorByRule(rule);

    assertThat(validator).isInstanceOf(expectedValidatorClass);
  }

  private static Stream<Arguments> validatorPerRuleProvider() {
    return Stream.of(
        arguments(mockedRuleWithType(RuleType.REGEXP.getValue()), RegExpValidator.class),
        arguments(mockedRuleWithType(RuleType.PROGRAMMATIC.getValue()), ProgrammaticValidator.class),
        arguments(mockedRuleWithType(RuleType.PWNEDPASSWORD.getValue()), PwnedPasswordValidator.class)
    );
  }

  private static PasswordValidationRule mockedRuleWithType(String ruleType) {
    PasswordValidationRule rule = new PasswordValidationRule();
    rule.setRuleType(ruleType);

    return rule;
  }
}
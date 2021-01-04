package org.folio.pv.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.glytching.junit.extension.random.Random;
import io.github.glytching.junit.extension.random.RandomBeansExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.folio.pv.client.UserClient;
import org.folio.pv.domain.RuleState;
import org.folio.pv.domain.ValidationType;
import org.folio.pv.domain.dto.Password;
import org.folio.pv.domain.dto.ValidationResult;
import org.folio.pv.domain.dto.ValidationRule;
import org.folio.pv.domain.dto.ValidationRuleCollection;
import org.folio.pv.domain.entity.PasswordValidationRule;
import org.folio.pv.mapper.ValidationRuleMapper;
import org.folio.pv.repository.ValidationRuleRepository;
import org.folio.pv.service.validator.UserData;
import org.folio.pv.service.validator.ValidationErrors;
import org.folio.pv.service.validator.Validator;
import org.folio.pv.service.validator.ValidatorRegistry;
import org.folio.spring.data.OffsetRequest;

@ExtendWith({
    RandomBeansExtension.class
})
@ExtendWith(SpringExtension.class)
class ValidationRuleServiceImplTest {

  @MockBean
  private ValidationRuleMapper mapper;
  @MockBean
  private ValidationRuleRepository repository;
  @MockBean
  private UserClient userClient;
  @MockBean
  private ValidatorRegistry validationRegistry;

  @Autowired
  private ValidationRuleService service;

  @TestConfiguration
  static class Config {

    @Bean
    public ValidationRuleService employeeService(ValidationRuleMapper mapper, ValidationRuleRepository repository,
        UserClient userClient, ValidatorRegistry validationRegistry) {
      return new ValidationRuleServiceImpl(mapper, repository, userClient, validationRegistry);
    }
  }

  @Test
  void shouldReturnValidationRuleById(@Random UUID ruleId, @Random PasswordValidationRule rule,
      @Random ValidationRule ruleDto) {
    
    when(repository.findById(ruleId)).thenReturn(Optional.of(rule));
    when(mapper.mapEntityToDto(rule)).thenReturn(ruleDto);

    ValidationRule result = service.getValidationRuleById(ruleId.toString());

    assertSame(ruleDto, result);
  }

  @Test
  void shouldReturnNullIfValidationRuleNotFoundById(@Random UUID ruleId) {
    when(repository.findById(ruleId)).thenReturn(Optional.empty());

    ValidationRule result = service.getValidationRuleById(ruleId.toString());

    assertNull(result);
  }

  @Test
  void shouldReturnValidationRules(@Random Integer offset, @Random Integer limit, @Random String orderBy,
      @Random List<PasswordValidationRule> rules, @Random ValidationRuleCollection ruleCollection) {

    OffsetRequest offsetReq = new OffsetRequest(offset, limit);
    Page<PasswordValidationRule> rulePage = new PageImpl<>(rules);

    when(repository.findAll(eq(offsetReq))).thenReturn(rulePage);
    when(mapper.mapEntitiesToValidationRuleCollection(rulePage)).thenReturn(ruleCollection);

    ValidationRuleCollection result = service.getValidationRules(offset, limit, orderBy);

    assertSame(ruleCollection, result);
  }

  @Test
  void shouldStoreValidationRule(@Random ValidationRule ruleDto, @Random PasswordValidationRule rule) {
    when(mapper.mapDtoToEntity(ruleDto)).thenReturn(rule);

    PasswordValidationRule storedRule = new PasswordValidationRule().copyForUpdate(rule);
    when(repository.save(rule)).thenReturn(storedRule);

    ValidationRule expected = new ValidationRule();
    when(mapper.mapEntityToDto(storedRule)).thenReturn(expected);

    ValidationRule result = service.storeValidationRule(ruleDto);

    assertSame(expected, result);
  }

  @Test
  void shouldCreateValidationRule(@Random ValidationRule ruleDto, @Random PasswordValidationRule rule) {
    rule.setId(null);
    rule.setCreatedDate(null);
    
    when(mapper.mapDtoToEntity(ruleDto)).thenReturn(rule);
    when(repository.save(rule)).thenReturn(rule);
    when(mapper.mapEntityToDto(rule)).thenReturn(ruleDto);

    ValidationRule result = service.createOrUpdateValidationRule(ruleDto);

    assertSame(ruleDto, result);
  }

  @Test
  void shouldUpdateValidationRule(@Random ValidationRule ruleDto, @Random PasswordValidationRule rule) {
    when(mapper.mapDtoToEntity(ruleDto)).thenReturn(rule);
    when(repository.getOne(rule.getId())).thenReturn(rule);
    when(repository.save(rule)).thenReturn(rule);
    when(mapper.mapEntityToDto(rule)).thenReturn(ruleDto);

    ValidationRule result = service.createOrUpdateValidationRule(ruleDto);

    assertSame(ruleDto, result);
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class ValidatePassword {

    private static final String INVALID_PASSWORD = "password.invalid";

    private final ValidationResult VALID = new ValidationResult()
        .result(ValidationRuleServiceImpl.VALIDATION_VALID_RESULT)
        .messages(emptyList());
    private final ValidationResult INVALID = new ValidationResult()
        .result(ValidationRuleServiceImpl.VALIDATION_INVALID_RESULT)
        .messages(singletonList(INVALID_PASSWORD));

    @Mock
    private Validator validator;


    @Test
    void shouldFailIfUserNotFoundById(@Random Password password) {
      String userId = password.getUserId();
      when(userClient.getUserByQuery(contains(userId))).thenReturn("{\"totalRecords\": 0}");

      Exception exc = Assertions.assertThrows(RuntimeException.class,
          () -> service.validatePasswordByRules(password));

      assertAll(
          () -> assertThat(exc.getMessage()).containsIgnoringCase("not found"),
          () -> assertThat(exc.getMessage()).contains(userId));
    }

    @Test
    void shouldSucceed(@Random Password password, @Random String userName, @Random PasswordValidationRule enabledRule) {
      String userId = password.getUserId();
      mockFindUserById(userId, userName);

      mockValidatorByRule(enabledRule);

      ValidationErrors errors = ValidationErrors.none();
      mockValidator(password, userName, errors);

      ValidationResult result = service.validatePasswordByRules(password);

      assertEquals(VALID, result);
    }

    @Test
    void shouldFailWithValidatorMsg(@Random Password password, @Random String userName,
        @Random PasswordValidationRule enabledRule) {
      String userId = password.getUserId();
      mockFindUserById(userId, userName);

      enabledRule.setValidationType(ValidationType.STRONG.getValue());
      mockValidatorByRule(enabledRule);

      ValidationErrors errors = ValidationErrors.of(INVALID_PASSWORD);
      mockValidator(password, userName, errors);

      ValidationResult result = service.validatePasswordByRules(password);

      assertEquals(INVALID, result);
    }

    private void mockFindUserById(String userId, String userName) {
      when(userClient.getUserByQuery(contains(userId))).thenReturn("{\n" +
          "  \"users\": [\n" +
          "    {\n" +
          "      \"username\": \"" + userName + "\"\n" +
          "    }\n" +
          "  ],\n" +
          "  \"totalRecords\": 1\n" +
          "}");
    }

    private void mockValidator(Password password, String userName, ValidationErrors errors) {
      when(validator.validate(eq(password.getPassword()), eq(new UserData(password.getUserId(), userName))))
          .thenReturn(errors);
    }

    private void mockValidatorByRule(PasswordValidationRule enabledRule) {
      when(repository.findByRuleState(RuleState.ENABLED.getValue())).thenReturn(singletonList(enabledRule));
      when(validationRegistry.validatorByRule(enabledRule)).thenReturn(validator);
    }
  }
}
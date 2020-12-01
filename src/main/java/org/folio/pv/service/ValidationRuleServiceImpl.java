package org.folio.pv.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

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
import org.folio.pv.service.validator.ValidatorRegistry;
import org.folio.spring.data.OffsetRequest;

@Service
@RequiredArgsConstructor
@Log4j2
public class ValidationRuleServiceImpl implements ValidationRuleService {

  public static final String VALIDATION_VALID_RESULT = "valid";
  public static final String VALIDATION_INVALID_RESULT = "invalid";

  private final ValidationRuleMapper validationRuleMapper;
  private final ValidationRuleRepository validationRuleRepository;
  private final UserClient userClient;
  private final ValidatorRegistry validationRegistry;


  @Override
  public ValidationRule getValidationRuleById(String ruleId) {
    var id = UUID.fromString(ruleId);

    return validationRuleRepository.findById(id).map(validationRuleMapper::mapEntityToDto).orElse(null);
  }

  @Override
  public ValidationRuleCollection getValidationRules(Integer offset, Integer limit, String orderBy) {
    var validationRuleList = validationRuleRepository.findAll(new OffsetRequest(offset, limit));
    return validationRuleMapper.mapEntitiesToValidationRuleCollection(validationRuleList);
  }

  @Override
  public ValidationRule createOrUpdateValidationRule(ValidationRule validationRule) {
    var rule = validationRuleMapper.mapDtoToEntity(validationRule);
    if (rule.getId() == null) {
      if (rule.getCreatedDate() == null) {
        rule.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
      }
    } else {
      rule = validationRuleRepository.getOne(rule.getId()).copyForUpdate(rule);
    }
    return validationRuleMapper.mapEntityToDto(validationRuleRepository.save(rule));
  }

  @Override
  public ValidationRule storeValidationRule(ValidationRule validationRule) {
    var rule = validationRuleMapper.mapDtoToEntity(validationRule);
    return validationRuleMapper.mapEntityToDto(validationRuleRepository.save(rule));
  }

  @Override
  public ValidationResult validatePasswordByRules(final Password passwordContainer) {
    var userName = getUserNameByUserId(passwordContainer.getUserId());
    var userData = new UserData(passwordContainer.getUserId(), userName);

    var password = passwordContainer.getPassword();

    var enabledRules = validationRuleRepository.findByRuleState(RuleState.ENABLED.getValue());
    enabledRules.sort(Comparator.comparing(PasswordValidationRule::getOrderNo));

    List<String> validationMessages = new ArrayList<>();

    for (PasswordValidationRule rule : enabledRules) {
      var validator = validationRegistry.validatorByRule(rule);

      log.info("Validating password with rule: {}", ruleBriefDescription(rule));

      var errors = validator.validate(password, userData);

      log.info("Validation errors: {}", errors.hasErrors() ? "'None'" : errors.getErrorMessages());

      validationMessages.addAll(errors.getErrorMessages());

      if (errors.hasErrors() && ValidationType.STRONG == ValidationType.fromValue(rule.getValidationType())) {
        break;
      }
    }

    var validationResult = new ValidationResult();
    validationResult.setMessages(validationMessages);
    validationResult.setResult(validationMessages.isEmpty() ? VALIDATION_VALID_RESULT : VALIDATION_INVALID_RESULT);
    log.info("Validation result: {}", validationResult);

    return validationResult;
  }

  private String ruleBriefDescription(PasswordValidationRule rule) {
    return new ToStringBuilder(rule)
        .append("id", rule.getId())
        .append("name", rule.getName())
        .append("type", rule.getRuleType())
        .append("validationType", rule.getValidationType())
        .build();
  }

  private String getUserNameByUserId(String userId) {
    var userContainerStr = userClient.getUserByQuery("id==" + userId);
    var userContainer = new JSONObject(userContainerStr);

    var totalRecords = userContainer.getInt("totalRecords");
    if (totalRecords == 0) {
      throw new RuntimeException("User is not found: id = " + userId);
    }
    JSONObject user = (JSONObject) userContainer.getJSONArray("users").get(0);
    return user.getString("username");
  }

}

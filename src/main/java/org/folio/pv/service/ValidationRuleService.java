package org.folio.pv.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import org.folio.pv.client.UserClient;
import org.folio.pv.domain.RuleType;
import org.folio.pv.domain.ValidationType;
import org.folio.pv.domain.dto.Password;
import org.folio.pv.domain.dto.ValidationResult;
import org.folio.pv.domain.dto.ValidationRule;
import org.folio.pv.domain.dto.ValidationRuleCollection;
import org.folio.pv.domain.entity.PasswordValidationRule;
import org.folio.pv.mapper.ValidationRuleMapper;
import org.folio.pv.repository.ValidationRuleRepository;
import org.folio.pv.service.validator.ProgrammaticValidator;
import org.folio.pv.service.validator.RegExpValidator;
import org.folio.pv.service.validator.UserData;
import org.folio.pv.service.validator.Validator;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;

@Service
@RequiredArgsConstructor
@Log4j2
public class ValidationRuleService {

  public static final String VALIDATION_VALID_RESULT = "valid";
  public static final String VALIDATION_INVALID_RESULT = "invalid";

  private final FolioExecutionContext folioExecutionContext;
  private final ValidationRuleMapper validationRuleMapper;
  private final ValidationRuleRepository validationRuleRepository;
  private final UserClient userClient;
  private final ObjectMapper jacksonObjectMapper;


  public ValidationRule getValidationRuleById(String ruleId) {
    var id = UUID.fromString(ruleId);

    return validationRuleRepository.findById(id).map(validationRuleMapper::mapEntityToDto).orElse(null);
  }

  public ValidationRuleCollection getValidationRules(Integer offset, Integer limit, String orderBy) {
    var validationRuleList = validationRuleRepository.findAll(new OffsetRequest(offset, limit));
    return validationRuleMapper.mapEntitiesToValidationRuleCollection(validationRuleList);
  }

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

  public ValidationRule storeValidationRule(ValidationRule validationRule) {
    var rule = validationRuleMapper.mapDtoToEntity(validationRule);
    return validationRuleMapper.mapEntityToDto(validationRuleRepository.save(rule));
  }

  public ValidationResult validatePasswordByRules(final Password passwordContainer) {
    var enabledRules = validationRuleRepository.findByRuleState("Enabled");
    var userName = getUserNameByUserId(passwordContainer.getUserId());
    var userData = new UserData(passwordContainer.getUserId(), userName);

    var password = passwordContainer.getPassword();

    enabledRules.sort(Comparator.comparing(PasswordValidationRule::getOrderNo));

    List<String> validationMessages = new ArrayList<>();

    for (PasswordValidationRule rule : enabledRules) {
      Validator validator;
      if (RuleType.REGEXP.getValue().equals(rule.getRuleType())) {
        validator = new RegExpValidator(rule);
      } else {
        validator = new ProgrammaticValidator(rule, folioExecutionContext, jacksonObjectMapper);
      }

      var errors = validator.validate(password, userData);

      validationMessages.addAll(errors.getErrorMessages());

      if (errors.hasErrors() && ValidationType.STRONG.getValue().equals(rule.getValidationType())) {
        break;
      }
    }

    var validationResult = new ValidationResult();
    validationResult.setMessages(validationMessages);
    validationResult.setResult(validationMessages.isEmpty() ? VALIDATION_VALID_RESULT : VALIDATION_INVALID_RESULT);

    return validationResult;
  }

  private String getUserNameByUserId(String userId) {
    var userContainerStr = userClient.getUserByQuery("id==" + userId);
    var userContainer = new JSONObject(userContainerStr);

    var totalRecords = userContainer.getInt("totalRecords");
    if (totalRecords == 0) {
      throw new RuntimeException("User is not found. Id: " + userId);
    }
    JSONObject user = (JSONObject) userContainer.getJSONArray("users").get(0);
    return user.getString("username");
  }

}

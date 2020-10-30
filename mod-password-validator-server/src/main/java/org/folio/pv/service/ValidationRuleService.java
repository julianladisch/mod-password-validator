package org.folio.pv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Log4j2
public class ValidationRuleService {
  private static final String REGEXP_USER_NAME_PLACEHOLDER = "<USER_NAME>";
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

  public ValidationResult validatePasswordByRules(final Password passwordContainer) throws IOException {
    var enabledRules = validationRuleRepository.findByRuleState("Enabled");
    var userName = getUserNameByUserId(passwordContainer.getUserId());

    var password = passwordContainer.getPassword();

    enabledRules.sort(Comparator.comparing(PasswordValidationRule::getOrderNo));

    List<String> validationMessages = new ArrayList<>();

    for (PasswordValidationRule rule : enabledRules) {
      boolean validationFailed;
      if (RuleType.REGEXP.getValue().equals(rule.getRuleType())) {
        var ruleExpression = rule.getRuleExpression();
        validationFailed = StringUtils.isNotBlank(ruleExpression) &&
          !Pattern.compile(ruleExpression.replaceAll(REGEXP_USER_NAME_PLACEHOLDER, userName)).matcher(password).matches();
        if (validationFailed) {
          validationMessages.add(rule.getErrMessageId());
        }
      } else {
        var messages = checkProgrammaticRule(rule, passwordContainer).getMessages();
        validationFailed = messages != null && messages.size() > 0;
        if (validationFailed) {
          validationMessages.addAll(messages);
        }
      }

      if (validationFailed && ValidationType.STRONG.getValue().equals(rule.getValidationType())) {
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

  private ValidationResult checkProgrammaticRule(PasswordValidationRule rule, Password passwordContainer) throws IOException {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      var httpPost = new HttpPost(folioExecutionContext.getOkapiUrl() + rule.getImplementationReference());

      folioExecutionContext.getOkapiHeaders().forEach((key, values) -> values.forEach(value -> httpPost.addHeader(key, value)));
      // Some servers choke on the default accept string.
      httpPost.addHeader("accept", "*/*");
      httpPost.addHeader("content-type", "application/json");
      httpPost.setEntity(new StringEntity(jacksonObjectMapper.writeValueAsString(passwordContainer)));

      CloseableHttpResponse response = client.execute(httpPost);

      var buffer = new ByteArrayOutputStream();
      response.getEntity().writeTo(buffer);

      String body = new String(buffer.toByteArray());

      var statusCode = response.getStatusLine().getStatusCode();
      if (statusCode < 200 || statusCode > 202) {
        if (ValidationType.STRONG.getValue().equals(rule.getValidationType())) {
          throw new RuntimeException(body);
        } else {
          return new ValidationResult();
        }
      }

      return jacksonObjectMapper.readValue(body, ValidationResult.class);
    }
  }

  public void doUserTestCall() {
    var userNameByUserId = getUserNameByUserId("963d44ec-f89f-4608-a998-ab53ec31c688");
    System.out.println(userNameByUserId);
  }
}

package org.folio.services.validator.engine;


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.rest.impl.AsyncResultAnswer;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.services.validator.registry.ValidatorRegistryService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ValidationEngineUnitTest {

  private static RuleCollection regExpRuleCollection;

  private final String RESPONSE_VALIDATION_RESULT_KEY = ValidationEngineService.RESPONSE_VALIDATION_RESULT_KEY;
  private final String RESPONSE_ERROR_MESSAGES_KEY = ValidationEngineService.RESPONSE_ERROR_MESSAGES_KEY;

  @Mock
  private ValidatorRegistryService validatorRegistryService;

  @InjectMocks
  private ValidationEngineService validationEngineService = new ValidationEngineServiceImpl();
  private Map<String, String> requestHeaders = new HashMap<>();


  @BeforeClass
  public static void setUp() {
    initRegExpRules();
  }

  private static void initRegExpRules() {
    regExpRuleCollection = new RuleCollection();

    List<Rule> rulesList = new ArrayList<>();
    Rule regExpLimitedLength_Rule = new JsonObject()
      .put("ruleId", "cckf8809-009o-8fhx-aldz-dhfnzb8e0fk1")
      .put("name", "length_between")
      .put("type", "RegExp")
      .put("validationType", "Strong")
      .put("state", "Enabled")
      .put("moduleName", "mod-password-validator")
      .put("expression", "^.{6,12}$")
      .put("description", "Password must be between 6 and 12 digits")
      .put("orderNo", 0)
      .put("errMessageId", "password.length.invalid")
      .mapTo(Rule.class);

    Rule regExpOnlyAlphabetical_Rule = new JsonObject()
      .put("ruleId", "dkv54p0d-aldc-zz09-bvcz-gjfnd81l0sdz")
      .put("name", "alphabetical_only")
      .put("type", "RegExp")
      .put("validationType", "Strong")
      .put("state", "Enabled")
      .put("moduleName", "mod-password-validator")
      .put("expression", "^[A-Za-z]+$")
      .put("description", "Password must contain upper and lower alphabetical characters only")
      .put("orderNo", 1)
      .put("errMessageId", "password.alphabetical.invalid")
      .mapTo(Rule.class);

    rulesList.add(regExpLimitedLength_Rule);
    rulesList.add(regExpOnlyAlphabetical_Rule);
    regExpRuleCollection.setRules(rulesList);
  }

  @Test
  public void shouldReturnValidResultCheckedByRegExpRules() {
    // given
    String password = "Password";

    Mockito.doAnswer(new AsyncResultAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 2))
      .when(validatorRegistryService)
      .getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      Assert.assertEquals(validationResult, ValidationEngineService.PASSWORD_VALIDATON_VALID_RESULT);
      Assert.assertNull(response.getValue(RESPONSE_ERROR_MESSAGES_KEY));
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldReturnInvalidLengthBetweenResultCheckedByRegExpRules() {
    // given
    String password = "passw";

    Mockito.doAnswer(new AsyncResultAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 2))
      .when(validatorRegistryService)
      .getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(validationResult, ValidationEngineService.PASSWORD_VALIDATON_INVALID_RESULT);
      Assert.assertEquals(errorMessages.getList().size(), 1);
      Assert.assertEquals(errorMessages.getList().get(0), regExpRuleCollection.getRules().get(0).getErrMessageId());
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldReturnInvalidAlphabeticalResultCheckedByRegExpRules() {
    // given
    String password = "9password";

    Mockito.doAnswer(new AsyncResultAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 2))
      .when(validatorRegistryService)
      .getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(validationResult, ValidationEngineService.PASSWORD_VALIDATON_INVALID_RESULT);
      Assert.assertEquals(errorMessages.getList().size(), 1);
      Assert.assertEquals(errorMessages.getList().get(0), regExpRuleCollection.getRules().get(1).getErrMessageId());
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldReturnInvalidResultForEachRuleCheckedByRegExpRules() {
    // given
    String password = "9pass";
    Mockito.doAnswer(new AsyncResultAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 2))
      .when(validatorRegistryService)
      .getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(validationResult, ValidationEngineService.PASSWORD_VALIDATON_INVALID_RESULT);
      Assert.assertEquals(errorMessages.getList().size(), regExpRuleCollection.getRules().size());
      for (Rule rule : regExpRuleCollection.getRules()) {
        Assert.assertTrue(errorMessages.getList().contains(rule.getErrMessageId()));
      }
    };

    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }
}

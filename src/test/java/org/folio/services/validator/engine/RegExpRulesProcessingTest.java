package org.folio.services.validator.engine;


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.rest.impl.GenericHandlerAnswer;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_ERROR_MESSAGES_KEY;
import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_VALIDATION_RESULT_KEY;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_INVALID_RESULT;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_VALID_RESULT;

@RunWith(MockitoJUnitRunner.class)
public class RegExpRulesProcessingTest {

  private static final String OKAPI_HEADER_TENANT_VALUE = "tenant";
  private static final String OKAPI_HEADER_TOKEN_VALUE = "token";

  private static final Rule PROGRAMMATIC_IS_IN_BAD_PASSWORD_LIST_RULE = new JsonObject()
    .put("ruleId", "0199fjcmld-009o-8fhx-v9dr-zzxdol43")
    .put("name", "is_in_bad_password_list")
    .put("type", "Programmatic")
    .put("validationType", "Strong")
    .put("state", "Enabled")
    .put("moduleName", "mod-login")
    .put("implementationReference", "/auth/credentials/isInBadPasswordList")
    .put("description", "Password must not be in bad password list")
    .put("orderNo", 0)
    .put("errMessageId", "password.in.bad.password.list")
    .mapTo(Rule.class);


  private static final Rule SOFT_PROGRAMMATIC_RULE = new JsonObject()
    .put("ruleId", "0199fjcmld-009o-8fhx-v9dr-zzxdol43")
    .put("name", "is_in_bad_password_list")
    .put("type", "Programmatic")
    .put("validationType", "Soft")
    .put("state", "Enabled")
    .put("moduleName", "mod-login")
    .put("implementationReference", "/auth/credentials/isInBadPasswordList")
    .put("description", "Password must not be in bad password list")
    .put("orderNo", 0)
    .put("errMessageId", "password.in.bad.password.list")
    .mapTo(Rule.class);

  private static RuleCollection regExpRuleCollection;
  private static RuleCollection programmaticRuleCollection;
  private static Map<String, String> requestHeaders;

  @InjectMocks
  private ValidationEngineService validationEngineService = new ValidationEngineServiceImpl();
  @Mock
  private ValidatorRegistryService validatorRegistryService;
  @Mock
  private HttpClient httpClient;
  @Mock
  private HttpClientRequest httpClientRequest;
  @Mock
  private HttpClientResponse httpClientResponse;

  @BeforeClass
  public static void setUp() {
    initRequestHeaders();
    initRegExpRules();
    initProgrammaticRules();
  }

  private static void initRequestHeaders() {
    requestHeaders = new HashMap<>();
    requestHeaders.put(org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT, OKAPI_HEADER_TENANT_VALUE);
    requestHeaders.put(org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN, OKAPI_HEADER_TOKEN_VALUE);
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

  private static void initProgrammaticRules() {
    programmaticRuleCollection = new RuleCollection();
    programmaticRuleCollection.setRules(Collections.singletonList(PROGRAMMATIC_IS_IN_BAD_PASSWORD_LIST_RULE));
  }

  @Test
  public void shouldReturnValidResultCheckedByRegExpRules() {
    // given
    String password = "Password";

    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 2))
      .when(validatorRegistryService)
      .getEnabledRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      Assert.assertEquals(VALIDATION_VALID_RESULT, validationResult);
      Assert.assertNull(response.getValue(RESPONSE_ERROR_MESSAGES_KEY));
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getEnabledRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldReturnInvalidLengthBetweenResultCheckedByRegExpRules() {
    // given
    String password = "passw";

    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 2))
      .when(validatorRegistryService)
      .getEnabledRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(VALIDATION_INVALID_RESULT, validationResult);
      Assert.assertEquals(1, errorMessages.getList().size());
      Assert.assertEquals(errorMessages.getList().get(0), regExpRuleCollection.getRules().get(0).getErrMessageId());
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getEnabledRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldReturnInvalidAlphabeticalResultCheckedByRegExpRules() {
    // given
    String password = "9password";

    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 2))
      .when(validatorRegistryService)
      .getEnabledRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(VALIDATION_INVALID_RESULT, validationResult);
      Assert.assertEquals(1, errorMessages.getList().size());
      Assert.assertEquals(errorMessages.getList().get(0), regExpRuleCollection.getRules().get(1).getErrMessageId());
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getEnabledRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldReturnInvalidResultForEachRuleCheckedByRegExpRules() {
    // given
    String password = "9pass";
    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 2))
      .when(validatorRegistryService)
      .getEnabledRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(VALIDATION_INVALID_RESULT, validationResult);
      Assert.assertEquals(errorMessages.getList().size(), regExpRuleCollection.getRules().size());
      for (Rule rule : regExpRuleCollection.getRules()) {
        Assert.assertTrue(errorMessages.getList().contains(rule.getErrMessageId()));
      }
    };

    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getEnabledRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }
}

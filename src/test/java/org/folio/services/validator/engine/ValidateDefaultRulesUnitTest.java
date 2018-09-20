package org.folio.services.validator.engine;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_VALIDATION_RESULT_KEY;
import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_ERROR_MESSAGES_KEY;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_INVALID_RESULT;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_VALID_RESULT;

@RunWith(MockitoJUnitRunner.class)
public class ValidateDefaultRulesUnitTest {

  private static RuleCollection regExpRuleCollection;

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
    Rule regMinimumLength_Rule = new JsonObject()
      .put("ruleId", "5105b55a-b9a3-4f76-9402-a5243ea63c95")
      .put("name", "password_length")
      .put("type", "RegExp")
      .put("validationType", "Strong")
      .put("state", "Enabled")
      .put("moduleName", "mod-password-validator")
      .put("expression", "^.{8,}$")
      .put("description", "The password length must be minimum 8 digits")
      .put("orderNo", 0)
      .put("errMessageId", "password.length.invalid")
      .mapTo(Rule.class);

    Rule regAlphabeticalLetters_Rule = new JsonObject()
      .put("ruleId", "dc653de8-f0df-48ab-9630-13aacfe8e8f4")
      .put("name", "alphabetical_letters")
      .put("type", "RegExp")
      .put("validationType", "Strong")
      .put("state", "Enabled")
      .put("moduleName", "mod-password-validator")
      .put("expression", "(?=.*[a-z])(?=.*[A-Z]).+")
      .put("description", "The password must contain both upper and lower case letters")
      .put("orderNo", 1)
      .put("errMessageId", "password.alphabetical.invalid")
      .mapTo(Rule.class);

    Rule regNumericSymbol_Rule = new JsonObject()
      .put("ruleId", "3e3c53ae-73c2-4eba-9f09-f2c9a892c7a2")
      .put("name", "numeric_symbol")
      .put("type", "RegExp")
      .put("validationType", "Strong")
      .put("state", "Enabled")
      .put("moduleName", "mod-password-validator")
      .put("expression", "(?=.*\\d).+")
      .put("description", "The password must contain at least one numeric character")
      .put("orderNo", 2)
      .put("errMessageId", "password.number.invalid")
      .mapTo(Rule.class);

    Rule regSpecialCharacter_Rule = new JsonObject()
      .put("ruleId", "2e82f890-49e8-46fc-923d-644f33dc5c3f")
      .put("name", "special_character")
      .put("type", "RegExp")
      .put("validationType", "Strong")
      .put("state", "Enabled")
      .put("moduleName", "mod-password-validator")
      .put("expression", "(?=.*[!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]).+")
      .put("description", "The password must contain at least one special character")
      .put("orderNo", 3)
      .put("errMessageId", "password.specialCharacter.invalid")
      .mapTo(Rule.class);

    Rule regUserName_Rule = new JsonObject()
      .put("ruleId", "2f390fa6-a2f8-4027-abaf-ee61952668bc")
      .put("name", "no_user_name")
      .put("type", "RegExp")
      .put("validationType", "Strong")
      .put("state", "Enabled")
      .put("moduleName", "mod-password-validator")
      .put("expression", "^(?:(?!<USER_NAME>).)+$")
      .put("description", "The password must not contain your username")
      .put("orderNo", 4)
      .put("errMessageId", "password.usernameDuplicate.invalid")
      .mapTo(Rule.class);

    Rule regSequence_Rule = new JsonObject()
      .put("ruleId", "8d4a2124-8a54-4c49-84c8-36a8f7fc01a8")
      .put("name", "keyboard_sequence")
      .put("type", "RegExp")
      .put("validationType", "Strong")
      .put("state", "Enabled")
      .put("moduleName", "mod-password-validator")
      .put("expression", "^(?:(?!qwe)(?!asd)(?!zxc)(?!qaz)(?!zaq)(?!xsw)(?!wsx)(?!edc)(?!cde)(?!rfv)(?!vfr)(?!tgb)(?!bgt)(?!yhn)(?!nhy)(?!ujm)(?!mju)(?!ik,)(?!,ki)(?!ol.)(?!.lo)(?!p;/)(?!/;p)(?!123).)+$")
      .put("description", "The password must contain at least one special character")
      .put("orderNo", 5)
      .put("errMessageId", "password.keyboardSequence.invalid")
      .mapTo(Rule.class);

    Rule regRepeatingSymbols_Rule = new JsonObject()
      .put("ruleId", "98b961b4-16b8-4e62-a359-abf3805e16b0")
      .put("name", "repeating_characters")
      .put("type", "RegExp")
      .put("validationType", "Strong")
      .put("state", "Enabled")
      .put("moduleName", "mod-password-validator")
      .put("expression", "^(?:(.)(?!\\1))*$")
      .put("description", "The password must not contain repeating symbols")
      .put("orderNo", 6)
      .put("errMessageId", "password.repeatingSymbols.invalid")
      .mapTo(Rule.class);

    Rule regWhiteSpace_Rule = new JsonObject()
      .put("ruleId", "51e201ba-95d3-44e5-b4ec-f0059f11afcb")
      .put("name", "no_white_space_character")
      .put("type", "RegExp")
      .put("validationType", "Strong")
      .put("state", "Enabled")
      .put("moduleName", "mod-password-validator")
      .put("expression", "^[^\\s]+$")
      .put("description", "The password must not contain a white space")
      .put("orderNo", 7)
      .put("errMessageId", "password.whiteSpace.invalid")
      .mapTo(Rule.class);

    rulesList.add(regMinimumLength_Rule);
    rulesList.add(regAlphabeticalLetters_Rule);
    rulesList.add(regNumericSymbol_Rule);
    rulesList.add(regSpecialCharacter_Rule);
    rulesList.add(regUserName_Rule);
    rulesList.add(regSequence_Rule);
    rulesList.add(regRepeatingSymbols_Rule);
    rulesList.add(regWhiteSpace_Rule);

    regExpRuleCollection.setRules(rulesList);
  }

  @Test
  public void shouldReturnOkWhenPasswordMatchesRules() {
    // given
    String password = "P@sw0rd1";

    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 4))
      .when(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      Assert.assertEquals(VALIDATION_VALID_RESULT, validationResult);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertTrue(errorMessages.isEmpty());
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldFailWhenDoNotMatchPasswordLength() {
    // given
    String password = "P@sw0rd";

    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 4))
      .when(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      Assert.assertEquals(VALIDATION_INVALID_RESULT, validationResult);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(1, errorMessages.getList().size());
      Assert.assertEquals(regExpRuleCollection.getRules().get(0).getErrMessageId(), errorMessages.getList().get(0));
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldFailWhenDoNotHaveUpperCaseLetter() {
    // given
    String password = "p@sw0rds";

    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 4))
      .when(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      Assert.assertEquals(VALIDATION_INVALID_RESULT, validationResult);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(1, errorMessages.getList().size());
      Assert.assertEquals(regExpRuleCollection.getRules().get(1).getErrMessageId(), errorMessages.getList().get(0));
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldFailWhenDoNotHaveLowerCaseLetter() {
    // given
    String password = "P@SW0RDS";

    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 4))
      .when(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      Assert.assertEquals(VALIDATION_INVALID_RESULT, validationResult);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(1, errorMessages.getList().size());
      Assert.assertEquals(regExpRuleCollection.getRules().get(1).getErrMessageId(), errorMessages.getList().get(0));
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldFailWhenNoNumberCharacter() {
    // given
    String password = "p@sWords";

    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 4))
      .when(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      Assert.assertEquals(VALIDATION_INVALID_RESULT, validationResult);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(1, errorMessages.getList().size());
      Assert.assertEquals(regExpRuleCollection.getRules().get(2).getErrMessageId(), errorMessages.getList().get(0));
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldFailWhenNoSpecialCharacter() {
    // given
    String password = "pasW0rds";

    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 4))
      .when(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      Assert.assertEquals(VALIDATION_INVALID_RESULT, validationResult);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(1, errorMessages.getList().size());
      Assert.assertEquals(regExpRuleCollection.getRules().get(3).getErrMessageId(), errorMessages.getList().get(0));
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  //TODO : add the test for userName when the code is ready
  @Test
  public void shouldFailWhenPasswordContainsUserName() {

  }

  @Test
  public void shouldFailWhenPasswordContainsSequence() {
    // given
    String password = "p@sw0qwertyrD";

    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 4))
      .when(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      Assert.assertEquals(VALIDATION_INVALID_RESULT, validationResult);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(1, errorMessages.getList().size());
      Assert.assertEquals(regExpRuleCollection.getRules().get(5).getErrMessageId(), errorMessages.getList().get(0));
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldFailWhenHaveRepeatingSymbols() {
    // given
    String password = "p@ssw0rD";

    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 4))
      .when(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      Assert.assertEquals(VALIDATION_INVALID_RESULT, validationResult);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(1, errorMessages.getList().size());
      Assert.assertEquals(regExpRuleCollection.getRules().get(6).getErrMessageId(), errorMessages.getList().get(0));
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldFailWhenHaveWhiteSpace() {
    // given
    String password = "P@s w0rd1";

    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(regExpRuleCollection)), 4))
      .when(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());

    // when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      Assert.assertEquals(VALIDATION_INVALID_RESULT, validationResult);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      Assert.assertEquals(1, errorMessages.getList().size());
      Assert.assertEquals(regExpRuleCollection.getRules().get(7).getErrMessageId(), errorMessages.getList().get(0));
    };
    validationEngineService.validatePassword(password, requestHeaders, checkingHandler);

    // then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }
}

package org.folio.services.validator.engine;

import static org.hamcrest.MatcherAssert.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticle;
import org.folio.rest.impl.GenericHandlerAnswer;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.services.validator.registry.ValidatorRegistryService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_ERROR_MESSAGES_KEY;
import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_VALIDATION_RESULT_KEY;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_INVALID_RESULT;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_VALID_RESULT;

/**
 * Test for Validation Engine component. Testing password processing by RegExp rules.
 */
@RunWith(VertxUnitRunner.class)
public class RegExpRulesProcessingTest {

  private static final JsonObject USER_SERVICE_MOCK_RESPONSE = new JsonObject()
    .put("users", new JsonArray()
      .add(new JsonObject()
        .put("username", "admin")
        .put("id", "9d990cae-2685-4868-9fca-d0ad013c0640")
        .put("active", true)))
    .put("totalRecords", 1);

  private static final Rule REGEXP_LIMITED_LENGTH_RULE = new Rule()
    .withRuleId("cckf8809-009o-8fhx-aldz-dhfnzb8e0fk1")
    .withName("length_between")
    .withType(Rule.Type.REG_EXP)
    .withValidationType(Rule.ValidationType.STRONG)
    .withState(Rule.State.ENABLED)
    .withModuleName("mod-password-validator")
    .withExpression("^.{6,12}$")
    .withDescription("Password must be between 6 and 12 digits")
    .withOrderNo(0)
    .withErrMessageId("password.length.invalid");

  private static final Rule REGEXP_ONLY_ALPHABETICAL_RULE = new Rule()
    .withRuleId("dkv54p0d-aldc-zz09-bvcz-gjfnd81l0sdz")
    .withName("alphabetical_only")
    .withType(Rule.Type.REG_EXP)
    .withValidationType(Rule.ValidationType.STRONG)
    .withState(Rule.State.ENABLED)
    .withModuleName("mod-password-validator")
    .withExpression("^[A-Za-z]+$")
    .withDescription("Password must contain upper and lower alphabetical characters only")
    .withOrderNo(1)
    .withErrMessageId("password.alphabetical.invalid");

  private static final String OKAPI_HEADER_TENANT_VALUE = "tenant";
  private static final String OKAPI_HEADER_TOKEN_VALUE = "token";
  private static final String USER_ID_VALUE = "db6ffb67-3160-43bf-8e2f-ecf9a420288b";

  private static final String OKAPI_URL_HEADER = "x-okapi-url";

  private static RuleCollection regExpRuleCollection;


  @InjectMocks
  private ValidationEngineService validationEngineService = new ValidationEngineServiceImpl();
  @Mock
  private ValidatorRegistryService validatorRegistryService;
  @Spy
  private WebClient webClient = WebClient.create(Vertx.vertx());

  private Map<String, String> requestHeaders;

  @org.junit.Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));

  @BeforeClass
  public static void setUpBeforeClass() {
    initRegExpRules();
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
    requestHeaders = new HashMap<>();
    requestHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, OKAPI_HEADER_TENANT_VALUE);
    requestHeaders.put(RestVerticle.OKAPI_HEADER_TOKEN, OKAPI_HEADER_TOKEN_VALUE);
    requestHeaders.put(RestVerticle.OKAPI_USERID_HEADER, USER_ID_VALUE);
    requestHeaders.put(OKAPI_URL_HEADER, "http://localhost:" + userMockServer.port());
    mockUserModule(HttpStatus.SC_OK, USER_SERVICE_MOCK_RESPONSE);
  }

  /**
   * Testing the case when received password satisfies every RegExp rule.
   * Expected result is to receive the response contains valid validation result
   * and empty error message list:
   * {
   * "result" : "valid"
   * "messages" : []
   * }
   */
  @Test
  public void shouldReturnValidResultCheckedByRegExpRules(TestContext testContext) {
    //given
    String password = "Password";
    mockRegistryServiceResponse(JsonObject.mapFrom(regExpRuleCollection));

    //expect
    Handler<AsyncResult<JsonObject>> checkingHandler = testContext.asyncAssertSuccess(response -> {
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      assertThat(validationResult, Matchers.is(VALIDATION_VALID_RESULT));
      assertThat(errorMessages, Matchers.emptyIterable());
      Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
    });

    //when
    validationEngineService.validatePassword(USER_ID_VALUE, password, requestHeaders, checkingHandler);
  }

  /**
   * Testing the case when received password doesn't satisfy "length_between" RegExp rule.
   * Expected result is to receive the response contains invalid validation result
   * and 1 error message code belongs to "length_between" rule:
   * {
   * "result" : "invalid"
   * "messages": ["password.length.invalid"]
   * }
   */
  @Test
  public void shouldReturnInvalidLengthBetweenResultCheckedByRegExpRules(TestContext testContext) {
    //given
    String password = "passw";
    mockRegistryServiceResponse(JsonObject.mapFrom(regExpRuleCollection));

    //expect
    Handler<AsyncResult<JsonObject>> checkingHandler = testContext.asyncAssertSuccess(response -> {
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      assertThat(validationResult, Matchers.is(VALIDATION_INVALID_RESULT));
      assertThat(errorMessages, Matchers.contains(REGEXP_LIMITED_LENGTH_RULE.getErrMessageId()));
      Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
    });

    //when
    validationEngineService.validatePassword(USER_ID_VALUE, password, requestHeaders, checkingHandler);
  }

  /**
   * Testing the case when received password doesn't satisfy "alphabetical_only" RegExp rule.
   * Expected result is to receive the response contains invalid validation result
   * and 1 error message code belongs to "alphabetical_only" rule:
   * {
   * "result" : "invalid"
   * "messages": ["password.alphabetical.invalid"]
   * }
   */
  @Test
  public void shouldReturnInvalidAlphabeticalOnlyResultCheckedByRegExpRules(TestContext testContext) {
    //given
    String password = "9password";
    mockRegistryServiceResponse(JsonObject.mapFrom(regExpRuleCollection));

    //expect
    Handler<AsyncResult<JsonObject>> checkingHandler = testContext.asyncAssertSuccess(response -> {
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      assertThat(validationResult, Matchers.is(VALIDATION_INVALID_RESULT));
      assertThat(errorMessages, Matchers.contains(REGEXP_ONLY_ALPHABETICAL_RULE.getErrMessageId()));
      Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
    });

    //when
    validationEngineService.validatePassword(USER_ID_VALUE, password, requestHeaders, checkingHandler);
  }

  /**
   * Testing the case when received password doesn't satisfy
   * both "length_between" and "alphabetical_only" RegExp rules.
   * Expected result is to receive the response contains invalid validation result
   * and 2 error message codes rule:
   * {
   * "result" : "invalid"
   * "messages": ["password.length.invalid", "password.alphabetical.invalid"]
   * }
   */
  @Test
  public void shouldReturnInvalidResultForEachRegExpRule(TestContext testContext) {
    //given
    String password = "9pass";
    mockRegistryServiceResponse(JsonObject.mapFrom(regExpRuleCollection));

    //expect
    Handler<AsyncResult<JsonObject>> checkingHandler = testContext.asyncAssertSuccess(response -> {
      String validationResult = response.getString(RESPONSE_VALIDATION_RESULT_KEY);
      JsonArray errorMessages = (JsonArray) response.getValue(RESPONSE_ERROR_MESSAGES_KEY);
      assertThat(validationResult, Matchers.is(VALIDATION_INVALID_RESULT));
      assertThat(errorMessages, Matchers.containsInAnyOrder(
        regExpRuleCollection.getRules().stream().map(Rule::getErrMessageId).toArray()));
      Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
    });

    //when
    validationEngineService.validatePassword(USER_ID_VALUE, password, requestHeaders, checkingHandler);
  }

  private static void initRegExpRules() {
    regExpRuleCollection = new RuleCollection()
      .withRules(Arrays.asList(REGEXP_LIMITED_LENGTH_RULE, REGEXP_ONLY_ALPHABETICAL_RULE));
  }

  private void mockUserModule(int status, JsonObject response) {
    WireMock.stubFor(WireMock.get("/users?query=id==" + USER_ID_VALUE)
      .willReturn(WireMock.okJson(response.toString())
      ));
  }

  private void mockRegistryServiceResponse(JsonObject jsonObject) {
    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(jsonObject), 4))
      .when(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }
}

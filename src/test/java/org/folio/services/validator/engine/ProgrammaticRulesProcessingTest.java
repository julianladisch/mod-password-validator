package org.folio.services.validator.engine;


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.http.HttpStatus;
import org.folio.rest.impl.GenericHandlerAnswer;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.services.validator.registry.ValidatorRegistryService;
import org.folio.services.validator.util.ValidatorHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_ERROR_MESSAGES_KEY;
import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_VALIDATION_RESULT_KEY;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_INVALID_RESULT;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_VALID_RESULT;


/**
 * Test for Validation Engine component. Testing password processing by Programmatic rules.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProgrammaticRulesProcessingTest {

  private static final String OKAPI_HEADER_TENANT_VALUE = "tenant";
  private static final String OKAPI_HEADER_TOKEN_VALUE = "token";

  private static final JsonObject USER_SERVICE_MOCK_RESPONSE = new JsonObject()
    .put("users", new JsonArray()
      .add(new JsonObject()
        .put("username", "admin")
        .put("id", "9d990cae-2685-4868-9fca-d0ad013c0640")
        .put("active", true)))
    .put("totalRecords", 1);

  private static final Rule STRONG_PROGRAMMATIC_RULE = new Rule()
    .withRuleId("739c63d4-bb53-11e8-a355-529269fb1459")
    .withName("is_in_bad_password_list")
    .withType(Rule.Type.PROGRAMMATIC)
    .withValidationType(Rule.ValidationType.STRONG)
    .withState(Rule.State.ENABLED)
    .withModuleName("mod-login")
    .withImplementationReference("/auth/credentials/isInBadPasswordList")
    .withDescription("Password must not be in bad password list")
    .withOrderNo(0)
    .withErrMessageId("password.in.bad.password.list");

  private static final Rule SOFT_PROGRAMMATIC_RULE = new Rule()
    .withRuleId("739c66f4-bb53-11e8-a355-529269fb1459")
    .withName("soft-programmatic-role")
    .withType(Rule.Type.PROGRAMMATIC)
    .withValidationType(Rule.ValidationType.SOFT)
    .withState(Rule.State.ENABLED)
    .withModuleName("mod-login")
    .withImplementationReference("/auth/credentials/isInBadPasswordList")
    .withDescription("Password must not be in bad password list")
    .withOrderNo(0)
    .withErrMessageId("password.in.bad.password.list");

  @Mock
  private ValidatorRegistryService validatorRegistryService;
  @Mock
  private HttpClient httpClient;
  @InjectMocks
  private ValidationEngineService validationEngineService = new ValidationEngineServiceImpl();

  private Map<String, String> requestHeaders;

  @Before
  public void setUp() throws Exception {
    requestHeaders = new HashMap<>();
    requestHeaders.put(OKAPI_HEADER_TENANT, OKAPI_HEADER_TENANT_VALUE);
    requestHeaders.put(OKAPI_HEADER_TOKEN, OKAPI_HEADER_TOKEN_VALUE);
    mockUserModule(HttpStatus.SC_OK, USER_SERVICE_MOCK_RESPONSE);
  }

  /**
   * Testing the case when received password satisfies Strong Programmatic rule.
   * Expected result is to receive the response contains valid validation result
   * and empty error message list:
   * {
   * "result" : "valid"
   * "messages" : []
   * }
   */
  @Test
  public void shouldReturnValidResultWhenProgrammaticRuleReturnsValid() {
    //given
    mockRegistryService(Collections.singletonList(STRONG_PROGRAMMATIC_RULE));

    JsonObject httpClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT);
    mockProgrammaticRuleClient(HttpStatus.SC_OK, httpClientMockResponse);

    //when
    JsonObject expectedResult = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT)
      .put(RESPONSE_ERROR_MESSAGES_KEY, new JsonArray());
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      Assert.assertEquals(expectedResult, response);
    };
    String givenPassword = "password";
    validationEngineService.validatePassword(givenPassword, requestHeaders, checkingHandler);

    //then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  /**
   * Testing the case when received password doesn't satisfy Strong Programmatic rule.
   * Expected result is to receive the response contains invalid validation result
   * and error message code belongs to the rule:
   * {
   * "result" : "invalid",
   * "messages" : "[password.in.bad.password.list]"
   * }
   */
  @Test
  public void shouldReturnInvalidResultWithMessagesWhenProgrammaticRulesReturnsInvalid() {
    //given
    mockRegistryService(Collections.singletonList(STRONG_PROGRAMMATIC_RULE));

    JsonObject httpClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_INVALID_RESULT);
    mockProgrammaticRuleClient(HttpStatus.SC_OK, httpClientMockResponse);

    //when
    JsonObject expectedResult = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_INVALID_RESULT)
      .put(ValidatorHelper.RESPONSE_ERROR_MESSAGES_KEY, new JsonArray().add(STRONG_PROGRAMMATIC_RULE.getErrMessageId()));
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      Assert.assertEquals(expectedResult, response);
    };
    String givenPassword = "password";
    validationEngineService.validatePassword(givenPassword, requestHeaders, checkingHandler);

    //then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  /**
   * Testing the case when received password satisfies Soft Programmatic rule.
   * Expected result is to receive the response contains valid validation result:
   * {
   * "result" : "valid",
   * "messages" : "[]"
   * }
   */
  @Test
  public void shouldReturnValidResultWhenWhenSoftProgrammaticRuleReturnErrorStatus() {
    //given
    mockRegistryService(Collections.singletonList(SOFT_PROGRAMMATIC_RULE));

    JsonObject httpClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT);
    mockProgrammaticRuleClient(HttpStatus.SC_INTERNAL_SERVER_ERROR, httpClientMockResponse);

    //when
    JsonObject expectedResult = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT)
      .put(RESPONSE_ERROR_MESSAGES_KEY, new JsonArray());
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      Assert.assertEquals(expectedResult, response);
    };
    String givenPassword = "password";
    validationEngineService.validatePassword(givenPassword, requestHeaders, checkingHandler);

    //then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  /**
   * Testing the case when external FOLIO module returns internal server error.
   * Expected result is to receive failed async result.
   */
  @Test
  public void shouldFailWhenStrongProgrammaticRuleReturnErrorStatus() {
    //given
    mockRegistryService(Collections.singletonList(STRONG_PROGRAMMATIC_RULE));

    JsonObject httpClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT);
    mockProgrammaticRuleClient(HttpStatus.SC_INTERNAL_SERVER_ERROR, httpClientMockResponse);

    //when
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      Assert.assertTrue(result.failed());
    };
    String givenPassword = "password";
    validationEngineService.validatePassword(givenPassword, requestHeaders, checkingHandler);

    //then
    Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
      ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }


  private void mockRegistryService(List<Rule> rules) {
    JsonObject registryResponse = JsonObject.mapFrom(new RuleCollection().withRules(rules));
    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(registryResponse)), 4))
      .when(validatorRegistryService)
      .getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  private void mockUserModule(int status, JsonObject response) {
    HttpClientResponse userModuleResponse = Mockito.mock(HttpClientResponse.class);
    HttpClientRequest userModuleRequest = Mockito.mock(HttpClientRequest.class);

    Mockito.doReturn(userModuleRequest)
      .when(httpClient)
      .getAbs(ArgumentMatchers.anyString());

    Mockito.doAnswer(new GenericHandlerAnswer<>(userModuleResponse, 0))
      .when(userModuleRequest)
      .handler(ArgumentMatchers.any(Handler.class));

    Mockito.doReturn(status)
      .when(userModuleResponse)
      .statusCode();

    Buffer userBodyMock = Mockito.mock(Buffer.class);
    Mockito.doReturn(response)
      .when(userBodyMock)
      .toJsonObject();

    Mockito.doAnswer(new GenericHandlerAnswer<>(userBodyMock, 0, userModuleResponse))
      .when(userModuleResponse)
      .bodyHandler(ArgumentMatchers.any(Handler.class));

    Mockito.doAnswer(InvocationOnMock::getMock)
      .when(userModuleRequest)
      .putHeader(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
  }

  private void mockProgrammaticRuleClient(int status, JsonObject response) {
    HttpClientResponse programmaticRuleResponse = Mockito.mock(HttpClientResponse.class);
    HttpClientRequest programmaticRuleRequest = Mockito.mock(HttpClientRequest.class);

    Mockito.doAnswer(new GenericHandlerAnswer<>(programmaticRuleResponse, 1, programmaticRuleRequest))
      .when(httpClient)
      .post(ArgumentMatchers.anyString(), ArgumentMatchers.any(Handler.class));

    Mockito.doReturn(status)
      .when(programmaticRuleResponse)
      .statusCode();

    Buffer ruleResponseBodyMock = Mockito.mock(Buffer.class);
    Mockito.doReturn(response)
      .when(ruleResponseBodyMock)
      .toJsonObject();

    Mockito.doAnswer(new GenericHandlerAnswer<>(ruleResponseBodyMock, 0, programmaticRuleResponse))
      .when(programmaticRuleResponse)
      .bodyHandler(ArgumentMatchers.any(Handler.class));

    Mockito.doAnswer(InvocationOnMock::getMock)
      .when(programmaticRuleRequest)
      .putHeader(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
    Mockito.doAnswer(InvocationOnMock::getMock)
      .when(programmaticRuleRequest)
      .write(ArgumentMatchers.anyString());
  }
}

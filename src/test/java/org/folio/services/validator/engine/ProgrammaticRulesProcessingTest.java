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
import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_VALIDATION_RESULT_KEY;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_INVALID_RESULT;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_VALID_RESULT;
import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_ERROR_MESSAGES_KEY;


/**
 * Test for Validation Engine component. Testing password processing by Programmatic rules.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProgrammaticRulesProcessingTest {

  private static final String OKAPI_HEADER_TENANT_VALUE = "tenant";
  private static final String OKAPI_HEADER_TOKEN_VALUE = "token";

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
  @Mock
  private HttpClientRequest httpClientRequest;
  @Mock
  private HttpClientResponse httpClientResponse;
  @InjectMocks
  private ValidationEngineService validationEngineService = new ValidationEngineServiceImpl();

  private Map<String, String> requestHeaders;

  @Before
  public void setUp() throws Exception {
    requestHeaders = new HashMap<>();
    requestHeaders.put(OKAPI_HEADER_TENANT, OKAPI_HEADER_TENANT_VALUE);
    requestHeaders.put(OKAPI_HEADER_TOKEN, OKAPI_HEADER_TOKEN_VALUE);
  }

  /**
   * Testing the case when received password satisfies Strong Programmatic rule.
   * Expected result is to receive the response contains valid validation result
   * and empty error message list:
   * {
   *    "result" : "valid"
   *    "messages" : []
   * }
   */
  @Test
  public void shouldReturnValidResultWhenProgrammaticRuleReturnsValid() {
    //given
    mockRegistryService(Collections.singletonList(STRONG_PROGRAMMATIC_RULE));

    JsonObject httpClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT);
    mockHttpClient(HttpStatus.SC_OK, httpClientMockResponse);

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
   *    "result" : "invalid",
   *    "messages" : "[password.in.bad.password.list]"
   * }
   */
  @Test
  public void shouldReturnInvalidResultWithMessagesWhenProgrammaticRulesReturnsInvalid() {
    //given
    mockRegistryService(Collections.singletonList(STRONG_PROGRAMMATIC_RULE));

    JsonObject httpClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_INVALID_RESULT);
    mockHttpClient(HttpStatus.SC_OK, httpClientMockResponse);

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
   *    "result" : "valid",
   *    "messages" : "[]"
   * }
   */
  @Test
  public void shouldReturnValidResultWhenWhenSoftProgrammaticRuleReturnErrorStatus() {
    //given
    mockRegistryService(Collections.singletonList(SOFT_PROGRAMMATIC_RULE));

    JsonObject httpClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT);
    mockHttpClient(HttpStatus.SC_INTERNAL_SERVER_ERROR, httpClientMockResponse);

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
    mockHttpClient(HttpStatus.SC_INTERNAL_SERVER_ERROR, httpClientMockResponse);

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

  private void mockHttpClient(int status, JsonObject response) {
    Mockito.doAnswer(new GenericHandlerAnswer<>(httpClientResponse, 1, httpClientRequest))
      .when(httpClient)
      .post(ArgumentMatchers.anyString(), ArgumentMatchers.any(Handler.class));

    Mockito.doReturn(status)
      .when(httpClientResponse)
      .statusCode();

    Buffer ruleResponseBodyMock = Mockito.mock(Buffer.class);
    Mockito.doReturn(response)
      .when(ruleResponseBodyMock)
      .toJsonObject();

    Mockito.doAnswer(new GenericHandlerAnswer<>(ruleResponseBodyMock, 0, httpClientResponse))
      .when(httpClientResponse)
      .bodyHandler(ArgumentMatchers.any(Handler.class));

    Mockito.doAnswer(InvocationOnMock::getMock)
      .when(httpClientRequest)
      .putHeader(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
    Mockito.doAnswer(InvocationOnMock::getMock)
      .when(httpClientRequest)
      .write(ArgumentMatchers.anyString());
  }

}

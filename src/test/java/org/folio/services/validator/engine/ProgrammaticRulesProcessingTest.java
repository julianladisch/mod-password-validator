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

@RunWith(MockitoJUnitRunner.class)
public class ProgrammaticRulesProcessingTest {

  private static final String OKAPI_HEADER_TENANT_VALUE = "tenant";
  private static final String OKAPI_HEADER_TOKEN_VALUE = "token";

  private static final Rule STRONG_PROGRAMMATIC_RULE = new JsonObject()
    .put("ruleId", "739c63d4-bb53-11e8-a355-529269fb1459")
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
    .put("ruleId", "739c66f4-bb53-11e8-a355-529269fb1459")
    .put("name", "soft-programmatic-role")
    .put("type", "Programmatic")
    .put("validationType", "Soft")
    .put("state", "Enabled")
    .put("moduleName", "mod-login")
    .put("implementationReference", "/auth/credentials/isInBadPasswordList")
    .put("description", "Password must not be in bad password list")
    .put("orderNo", 0)
    .put("errMessageId", "password.in.bad.password.list")
    .mapTo(Rule.class);

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

  private void mockRegistryService(List<Rule> rules) {
    JsonObject registryResponse = JsonObject.mapFrom(new RuleCollection().withRules(rules));
    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(registryResponse)), 2))
      .when(validatorRegistryService)
      .getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  private void mockHttpClient(int status) {
    mockHttpClient(status, null);
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

  @Test
  public void shouldReturnValidResultWhenProgrammaticRuleReturnsValid() {
    //given
    mockRegistryService(Collections.singletonList(STRONG_PROGRAMMATIC_RULE));

    JsonObject httpClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT);
    mockHttpClient(HttpStatus.SC_OK, httpClientMockResponse);

    //when
    JsonObject expectedResult = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT);
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      Assert.assertEquals(expectedResult, response);
    };
    String givenPassword = "password";
    validationEngineService.validatePassword(givenPassword, requestHeaders, checkingHandler);

    //then
    Mockito.verify(validatorRegistryService).getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

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
    Mockito.verify(validatorRegistryService).getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void shouldReturnValidResultWhenWhenSoftProgrammaticRuleReturnErrorStatus() {
    //given
    mockRegistryService(Collections.singletonList(SOFT_PROGRAMMATIC_RULE));

    JsonObject httpClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT);
    mockHttpClient(HttpStatus.SC_INTERNAL_SERVER_ERROR, httpClientMockResponse);

    //when
    JsonObject expectedResult = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT);
    Handler<AsyncResult<JsonObject>> checkingHandler = result -> {
      JsonObject response = result.result();
      Assert.assertEquals(expectedResult, response);
    };
    String givenPassword = "password";
    validationEngineService.validatePassword(givenPassword, requestHeaders, checkingHandler);

    //then
    Mockito.verify(validatorRegistryService).getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

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
    Mockito.verify(validatorRegistryService).getActiveRulesByType(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

}

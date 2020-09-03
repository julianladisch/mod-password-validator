package org.folio.services.validator.engine;


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
import org.folio.rest.impl.GenericHandlerAnswer;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.services.validator.registry.ValidatorRegistryService;
import org.folio.services.validator.util.ValidatorHelper;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_ERROR_MESSAGES_KEY;
import static org.folio.services.validator.util.ValidatorHelper.RESPONSE_VALIDATION_RESULT_KEY;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_INVALID_RESULT;
import static org.folio.services.validator.util.ValidatorHelper.VALIDATION_VALID_RESULT;


/**
 * Test for Validation Engine component. Testing password processing by Programmatic rules.
 */
@RunWith(VertxUnitRunner.class)
public class ProgrammaticRulesProcessingTest {

  private static final String OKAPI_HEADER_TENANT_VALUE = "tenant";
  private static final String OKAPI_HEADER_TOKEN_VALUE = "token";
  private static final String USER_ID_VALUE = "db6ffb67-3160-43bf-8e2f-ecf9a420288b";

  private static final String OKAPI_URL_HEADER = "x-okapi-url";

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
  @Spy
  private WebClient webClient = WebClient.create(Vertx.vertx());
  @InjectMocks
  private ValidationEngineService validationEngineService = new ValidationEngineServiceImpl();

  @org.junit.Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));

  private Map<String, String> requestHeaders;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
    requestHeaders = new HashMap<>();
    requestHeaders.put(OKAPI_HEADER_TENANT, OKAPI_HEADER_TENANT_VALUE);
    requestHeaders.put(OKAPI_HEADER_TOKEN, OKAPI_HEADER_TOKEN_VALUE);
    requestHeaders.put(OKAPI_URL_HEADER, "http://localhost:" + userMockServer.port());
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
  public void shouldReturnValidResultWhenProgrammaticRuleReturnsValid(TestContext testContext) {
    //given
    mockRegistryService(Collections.singletonList(STRONG_PROGRAMMATIC_RULE));

    JsonObject webClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT);
    mockProgrammaticRuleClient(STRONG_PROGRAMMATIC_RULE.getImplementationReference(),
      HttpStatus.SC_OK, webClientMockResponse);

    //expect
    JsonObject expectedResult = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT)
      .put(RESPONSE_ERROR_MESSAGES_KEY, new JsonArray());
    Handler<AsyncResult<JsonObject>> checkingHandler = testContext.asyncAssertSuccess(response -> {
      assertThat(response, Matchers.is(expectedResult));
      Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
    });

    //when
    String givenPassword = "password";
    validationEngineService.validatePassword(USER_ID_VALUE, givenPassword, requestHeaders, checkingHandler);
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
  public void shouldReturnInvalidResultWithMessagesWhenProgrammaticRulesReturnsInvalid(TestContext testContext) {
    //given
    mockRegistryService(Collections.singletonList(STRONG_PROGRAMMATIC_RULE));

    JsonObject webClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_INVALID_RESULT);
    mockProgrammaticRuleClient(STRONG_PROGRAMMATIC_RULE.getImplementationReference(),
      HttpStatus.SC_OK, webClientMockResponse);

    //expect
    JsonObject expectedResult = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_INVALID_RESULT)
      .put(ValidatorHelper.RESPONSE_ERROR_MESSAGES_KEY, new JsonArray().add(STRONG_PROGRAMMATIC_RULE.getErrMessageId()));
    Handler<AsyncResult<JsonObject>> checkingHandler = testContext.asyncAssertSuccess(response -> {
      assertThat(response, Matchers.is(expectedResult));
      Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
    });

    //when
    String givenPassword = "password";
    validationEngineService.validatePassword(USER_ID_VALUE, givenPassword, requestHeaders, checkingHandler);
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
  public void shouldReturnValidResultWhenWhenSoftProgrammaticRuleReturnErrorStatus(TestContext testContext) {
    //given
    mockRegistryService(Collections.singletonList(SOFT_PROGRAMMATIC_RULE));

    JsonObject webClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT);
    mockProgrammaticRuleClient(SOFT_PROGRAMMATIC_RULE.getImplementationReference(),
      HttpStatus.SC_INTERNAL_SERVER_ERROR, webClientMockResponse);

    //expect
    JsonObject expectedResult = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT)
      .put(RESPONSE_ERROR_MESSAGES_KEY, new JsonArray());
    Handler<AsyncResult<JsonObject>> checkingHandler = testContext.asyncAssertSuccess(response -> {
      assertThat(response, Matchers.is(expectedResult));
      Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
    });

    //when
    String givenPassword = "password";
    validationEngineService.validatePassword(USER_ID_VALUE, givenPassword, requestHeaders, checkingHandler);
  }

  /**
   * Testing the case when external FOLIO module returns internal server error.
   * Expected result is to receive failed async result.
   */
  @Test
  public void shouldFailWhenStrongProgrammaticRuleReturnErrorStatus(TestContext testContext) {
    //given
    mockRegistryService(Collections.singletonList(STRONG_PROGRAMMATIC_RULE));

    JsonObject webClientMockResponse = new JsonObject()
      .put(RESPONSE_VALIDATION_RESULT_KEY, VALIDATION_VALID_RESULT);
    mockProgrammaticRuleClient(STRONG_PROGRAMMATIC_RULE.getImplementationReference(),
      HttpStatus.SC_INTERNAL_SERVER_ERROR, webClientMockResponse);

    //expect
    Handler<AsyncResult<JsonObject>> checkingHandler = testContext.asyncAssertFailure(exception -> {
      Mockito.verify(validatorRegistryService).getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any());
    });

    //when
    String givenPassword = "password";
    validationEngineService.validatePassword(USER_ID_VALUE, givenPassword, requestHeaders, checkingHandler);
  }


  private void mockRegistryService(List<Rule> rules) {
    JsonObject registryResponse = JsonObject.mapFrom(new RuleCollection().withRules(rules));
    Mockito.doAnswer(new GenericHandlerAnswer<>(Future.succeededFuture(JsonObject.mapFrom(registryResponse)), 4))
      .when(validatorRegistryService)
      .getAllTenantRules(ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  private void mockUserModule(int status, JsonObject response) {
    WireMock.stubFor(WireMock.get("/users?query=id==" + USER_ID_VALUE)
      .willReturn(WireMock.okJson(response.toString())
      ));
  }

  private void mockProgrammaticRuleClient(String urlPath, int status, JsonObject response) {
    stubFor(post(urlEqualTo(urlPath))
      .willReturn(aResponse()
        .withStatus(status)
        .withBody(response.toString())
      ));
  }
}

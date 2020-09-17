package org.folio.rest.impl;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Password;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.services.validator.util.ValidatorHelper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MediaType;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;


@RunWith(VertxUnitRunner.class)
public class PasswordResourceTest {
  private static final Logger logger = LoggerFactory.getLogger(PasswordResourceTest.class);

  private static final String HOST = "http://localhost:";
  private static final String HTTP_PORT = "http.port";

  private static final String OKAPI_URL_HEADER = "x-okapi-url";
  private static final String TENANT = "diku";
  private static final String ADMIN_ID = "3b47c4ad-a588-461f-a8e3-c0bdae547f77";

  private static final Header TENANT_HEADER = new Header(RestVerticle.OKAPI_HEADER_TENANT, TENANT);
  private static final Header TOKEN_HEADER = new Header(OKAPI_HEADER_TOKEN, "token");

  private static final String VALIDATE_PATH = "/password/validate";
  private static final String TENANT_RULES_PATH = "/tenant/rules";

  private static final String PASSWORD_VALIDATION_RESULT_JSON_PATH = "result";
  private static final String PASSWORD_VALIDATION_MESSAGES_JSON_PATH = "messages";

  private static final String VALIDATION_RULES_TABLE_NAME = "validation_rules";
  private static final String USERS_KEY = "users";
  private static final String TOTAL_RECORDS_KEY = "totalRecords";

  private static Vertx vertx;
  private static int port;


  @org.junit.Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));
  @org.junit.Rule
  public Timeout rule = Timeout.seconds(180);  // 3 minutes for loading embedded postgres

  private Header userMockUrlHeader;

  @BeforeClass
  public static void setUpClass(final TestContext context) {
    Async async = context.async();
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();

    TenantClient tenantClient = new TenantClient(HOST + port, TENANT, TENANT);
    DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put(HTTP_PORT, port));
    vertx.deployVerticle(RestVerticle.class.getName(), options, res -> {
      try {
        TenantAttributes t = new TenantAttributes()
          .withModuleTo(String.format("mod-password-validator-%s", PomReader.INSTANCE.getVersion()));
        tenantClient.postTenant(t, res2 -> async.complete());
      } catch (Exception e) {
        logger.error(e.getMessage());
      }
    });
  }

  @Before
  public void setUp(TestContext context) {
    userMockUrlHeader = new Header(OKAPI_URL_HEADER, HOST + userMockServer.port());
    clearRulesTable(context);
  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
  }

  @Test
  public void shouldReturnBadRequestStatusWhenPasswordIsAbsentInBody(TestContext context) {
    JsonObject emptyJsonObject = new JsonObject();
    requestSpecification()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(userMockUrlHeader)
      .body(emptyJsonObject)
      .when()
      .post(VALIDATE_PATH)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void shouldReturnSuccessfulValidationWhenPasswordPassesAllRules(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(buildRegexpRuleOneLetterOneNumber().withOrderNo(0).withState(Rule.State.ENABLED))
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    requestSpecification()
      .header(TENANT_HEADER)
      .body(buildRegexpRuleMinLength8().withOrderNo(1).withState(Rule.State.ENABLED))
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    mockUserService();
    Password passwordToValidate = new Password()
      .withPassword("P@sword12")
      .withUserId(ADMIN_ID);

    requestSpecification()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(userMockUrlHeader)
      .body(passwordToValidate)
      .when()
      .post(VALIDATE_PATH)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body(PASSWORD_VALIDATION_RESULT_JSON_PATH, is(ValidatorHelper.VALIDATION_VALID_RESULT));
  }

  @Test
  public void shouldReturnFailedValidationResultWhenUserNotFound() {
    ResponseDefinitionBuilder mockDefinition = WireMock.okJson(new JsonObject()
      .put(USERS_KEY, new JsonArray())
      .put(TOTAL_RECORDS_KEY, 0).toString());
    initMockUserService(mockDefinition);

    Password passwordToValidate = new Password()
      .withPassword("P@sword12")
      .withUserId(ADMIN_ID);

    requestSpecification()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(userMockUrlHeader)
      .body(passwordToValidate)
      .when()
      .post(VALIDATE_PATH)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void shouldReturnFailedValidationResultWhenUserApiReturnBadRequest() {
    ResponseDefinitionBuilder mockDefinition = WireMock.badRequest();
    initMockUserService(mockDefinition);

    Password passwordToValidate = new Password()
      .withPassword("P@sword12")
      .withUserId(ADMIN_ID);

    requestSpecification()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(userMockUrlHeader)
      .body(passwordToValidate)
      .when()
      .post(VALIDATE_PATH)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void shouldReturnFailedValidationResultWhenUserReturnIncorrectRequest() {
    ResponseDefinitionBuilder mockDefinition = WireMock.okJson(new JsonObject()
      .put(TOTAL_RECORDS_KEY, 0).toString());
    initMockUserService(mockDefinition);

    Password passwordToValidate = new Password()
      .withPassword("P@sword12")
      .withUserId(ADMIN_ID);

    requestSpecification()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(userMockUrlHeader)
      .body(passwordToValidate)
      .when()
      .post(VALIDATE_PATH)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void shouldReturnFailedValidationResultWhenUserReturnIncorrectTotalRecords() {
    ResponseDefinitionBuilder mockDefinition = WireMock.okJson(new JsonObject()
      .put(USERS_KEY, new JsonArray())
      .put(TOTAL_RECORDS_KEY, 2).toString());
    initMockUserService(mockDefinition);

    Password passwordToValidate = new Password()
      .withPassword("P@sword12")
      .withUserId(ADMIN_ID);

    requestSpecification()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(userMockUrlHeader)
      .body(passwordToValidate)
      .when()
      .post(VALIDATE_PATH)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void shouldReturnFailedValidationResultWithMessageWhenPasswordDidNotPassRule(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(buildRegexpRuleOneLetterOneNumber().withOrderNo(0).withState(Rule.State.ENABLED))
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    requestSpecification()
      .header(TENANT_HEADER)
      .body(buildRegexpRuleMinLength8().withOrderNo(1).withState(Rule.State.ENABLED))
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    mockUserService();
    Password passwordToValidate = new Password()
      .withPassword("badPassword")
      .withUserId(ADMIN_ID);

    requestSpecification()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(userMockUrlHeader)
      .body(passwordToValidate)
      .when()
      .post(VALIDATE_PATH)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body(PASSWORD_VALIDATION_RESULT_JSON_PATH, is(ValidatorHelper.VALIDATION_INVALID_RESULT))
      .body(PASSWORD_VALIDATION_MESSAGES_JSON_PATH, contains(buildRegexpRuleOneLetterOneNumber().getErrMessageId()));
  }

  @Test
  public void shouldNotValidatePasswordAgainstDisabledRules(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(buildRegexpRuleOneLetterOneNumber().withOrderNo(0).withState(Rule.State.ENABLED))
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    requestSpecification()
      .header(TENANT_HEADER)
      .body(buildRegexpRuleMinLength8().withOrderNo(1).withState(Rule.State.DISABLED))
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    mockUserService();
    Password passwordToValidate = new Password()
      .withPassword("1a")
      .withUserId(ADMIN_ID);

    requestSpecification()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(userMockUrlHeader)
      .body(passwordToValidate)
      .when()
      .post(VALIDATE_PATH)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body(PASSWORD_VALIDATION_RESULT_JSON_PATH, is(ValidatorHelper.VALIDATION_VALID_RESULT));
  }

  private RequestSpecification requestSpecification() {
    return RestAssured.given()
      .port(port)
      .contentType(MediaType.APPLICATION_JSON);
  }

  private Rule buildRegexpRuleOneLetterOneNumber() {
    return new Rule()
      .withName("Regexp rule")
      .withType(Rule.Type.REG_EXP)
      .withValidationType(Rule.ValidationType.STRONG)
      .withModuleName("mod-password-validator")
      .withExpression("^(?=.*[A-Za-z])(?=.*\\d).+$")
      .withDescription("At least one letter and one number")
      .withErrMessageId("password.validation.error.one-letter-one-number");
  }

  private Rule buildRegexpRuleMinLength8() {
    return new Rule()
      .withName("Regexp rule")
      .withType(Rule.Type.REG_EXP)
      .withValidationType(Rule.ValidationType.STRONG)
      .withModuleName("mod-password-validator")
      .withExpression("^.{8,}$")
      .withDescription("Minimum eight characters")
      .withErrMessageId("password.validation.error.min-8");
  }

  private void mockUserService() {
    ResponseDefinitionBuilder mockDefinition = WireMock.okJson(buildUserMockResponse().toString());
    initMockUserService(mockDefinition);
  }

  private void initMockUserService(ResponseDefinitionBuilder mockDefinition) {
    WireMock.stubFor(WireMock.get("/users?query=id==" + ADMIN_ID)
      .willReturn(mockDefinition));
  }

  private JsonObject buildUserMockResponse() {
    JsonObject admin = new JsonObject()
      .put("username", "admin")
      .put("id", ADMIN_ID)
      .put("active", true);

    return new JsonObject()
      .put(USERS_KEY, new JsonArray()
        .add(admin))
      .put(TOTAL_RECORDS_KEY, 1);
  }

  private void clearRulesTable(TestContext context) {
    PostgresClient.getInstance(vertx, TENANT).delete(VALIDATION_RULES_TABLE_NAME, new Criterion(), event -> {
      if (event.failed()) {
        context.fail(event.cause());
      }
    });
  }
}

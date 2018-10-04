package org.folio.rest.impl;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.PasswordJson;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
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
import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;


@RunWith(VertxUnitRunner.class)
public class PasswordResourceTest {

  private static final String HOST = "http://localhost:";
  private static final String HTTP_PORT = "http.port";

  private static final String OKAPI_URL_HEADER = "x-okapi-url";
  private static final String TENANT = "diku";
  public static final String ADMIN_ID = "3b47c4ad-a588-461f-a8e3-c0bdae547f77";

  private static final Header TENANT_HEADER = new Header(RestVerticle.OKAPI_HEADER_TENANT, TENANT);
  private static final Header TOKEN_HEADER = new Header(OKAPI_HEADER_TOKEN, "token");
  private static final Header USER_ID_HEADER = new Header(OKAPI_USERID_HEADER, ADMIN_ID);

  private static final String VALIDATE_PATH = "/password/validate";
  private static final String TENANT_RULES_PATH = "/tenant/rules";

  private static final String PASSWORD_VALIDATION_RESULT_JSON_PATH = "result";
  private static final String PASSWORD_VALIDATION_MESSAGES_JSON_PATH = "messages";

  private static final String VALIDATION_RULES_TABLE_NAME = "validation_rules";

  private static Vertx vertx;
  private static int port;
  private static String useExternalDatabase;


  @org.junit.Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));
  @org.junit.Rule
  public Timeout rule = Timeout.seconds(180);  // 3 minutes for loading embedded postgres

  private Header userMockUrlHeader;

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
    Async async = context.async();
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();

    useExternalDatabase = System.getProperty(
      "org.folio.password.validator.test.database",
      "embedded");

    switch (useExternalDatabase) {
      case "environment":
        System.out.println("Using environment settings");
        break;
      case "external":
        String postgresConfigPath = System.getProperty(
          "org.folio.password.validator.test.config",
          "/postgres-conf-local.json");
        PostgresClient.setConfigFilePath(postgresConfigPath);
        break;
      case "embedded":
        PostgresClient.setIsEmbedded(true);
        PostgresClient.getInstance(vertx).startEmbeddedPostgres();
        break;
      default:
        String message = "No understood database choice made." +
          "Please set org.folio.password.validator.test.database" +
          "to 'external', 'environment' or 'embedded'";
        throw new Exception(message);
    }

    TenantClient tenantClient = new TenantClient("localhost", port, TENANT, TENANT);
    DeploymentOptions restVerticleDeploymentOptions = new DeploymentOptions().setConfig(new JsonObject().put(HTTP_PORT, port));
    vertx.deployVerticle(RestVerticle.class.getName(), restVerticleDeploymentOptions, res -> {
      try {
        tenantClient.post(null, res2 -> {
          async.complete();
        });
      } catch (Exception e) {
        e.printStackTrace();
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
      if (useExternalDatabase.equals("embedded")) {
        PostgresClient.stopEmbeddedPostgres();
      }
      async.complete();
    }));
  }

  @Test
  public void shouldReturnBadRequestStatusWhenPasswordIsAbsentInBody(TestContext context) {
    JsonObject emptyJsonObject = new JsonObject();
    RestAssured.given()
      .port(port)
      .contentType(MediaType.APPLICATION_JSON)
      .header(TENANT_HEADER)
      .header(USER_ID_HEADER)
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
    RestAssured.given()
      .port(port)
      .contentType(MediaType.APPLICATION_JSON)
      .header(TENANT_HEADER)
      .body(buildRegexpRuleOneLetterOneNumber().withOrderNo(0).withState(Rule.State.ENABLED))
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    RestAssured.given()
      .port(port)
      .contentType(MediaType.APPLICATION_JSON)
      .header(TENANT_HEADER)
      .body(buildRegexpRuleMinLength8().withOrderNo(1).withState(Rule.State.ENABLED))
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    mockUserService();
    PasswordJson passwordToValidate = new PasswordJson().withPassword("P@sword12");
    RestAssured.given()
      .port(port)
      .contentType(MediaType.APPLICATION_JSON)
      .header(TENANT_HEADER)
      .header(USER_ID_HEADER)
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
  public void shouldReturnFailedValidationResultWithMessageWhenPasswordDidNotPassRule(final TestContext context) {
    RestAssured.given()
      .port(port)
      .contentType(MediaType.APPLICATION_JSON)
      .header(TENANT_HEADER)
      .body(buildRegexpRuleOneLetterOneNumber().withOrderNo(0).withState(Rule.State.ENABLED))
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    RestAssured.given()
      .port(port)
      .contentType(MediaType.APPLICATION_JSON)
      .header(TENANT_HEADER)
      .body(buildRegexpRuleMinLength8().withOrderNo(1).withState(Rule.State.ENABLED))
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    mockUserService();
    PasswordJson passwordToValidate = new PasswordJson().withPassword("badPassword");
    RestAssured.given()
      .port(port)
      .contentType(MediaType.APPLICATION_JSON)
      .header(TENANT_HEADER)
      .header(USER_ID_HEADER)
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
    WireMock.stubFor(WireMock.get("/users?query=id==" + ADMIN_ID)
      .willReturn(WireMock.okJson(buildUserMockResponse().toString())
      ));
  }

  private JsonObject buildUserMockResponse() {
    JsonObject admin = new JsonObject()
      .put("username", "admin")
      .put("id", ADMIN_ID)
      .put("active", true);

    return new JsonObject()
      .put("users", new JsonArray()
        .add(admin))
      .put("totalRecords", 1);
  }

  private void clearRulesTable(TestContext context) {
    PostgresClient.getInstance(vertx, TENANT).delete(VALIDATION_RULES_TABLE_NAME, new Criterion(), event -> {
      if (event.failed()) {
        context.fail(event.cause());
      }
    });
  }
}

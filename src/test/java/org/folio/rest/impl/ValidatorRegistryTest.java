package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Rule;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(VertxUnitRunner.class)
public class ValidatorRegistryTest {

  private static final Logger logger = LoggerFactory.getLogger(ValidatorRegistryTest.class);

  private static final JsonObject PROGRAMMATIC_RULE_DISABLED = new JsonObject()
    .put("name", "programmatic rule disabled")
    .put("type", "Programmatic")
    .put("validationType", "Soft")
    .put("state", "Disabled")
    .put("moduleName", "mod-password-validator")
    .put("expression", "")
    .put("implementationReference", "Some implementation")
    .put("description", "Programmatic rule")
    .put("orderNo", 0)
    .put("errMessageId", "");

  private static final JsonObject PROGRAMMATIC_RULE_ENABLED = new JsonObject()
    .put("name", "programmatic rule enabled")
    .put("type", "Programmatic")
    .put("validationType", "Soft")
    .put("state", "Disabled")
    .put("moduleName", "mod-password-validator")
    .put("expression", "")
    .put("implementationReference", "Some implementation")
    .put("description", "Programmatic rule")
    .put("orderNo", 1)
    .put("errMessageId", "");

  private static final JsonObject REGEXP_RULE_ENABLED = new JsonObject()
    .put("name", "regexp rule enabled")
    .put("type", "RegExp")
    .put("validationType", "Strong")
    .put("state", "Enabled")
    .put("moduleName", "mod-password-validator")
    .put("expression", "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$")
    .put("description", "At least one letter and one number")
    .put("orderNo", 1)
    .put("errMessageId", "");

  private static final JsonObject REGEXP_RULE_DISABLED = new JsonObject()
    .put("name", "regexp rule disabled")
    .put("type", "RegExp")
    .put("validationType", "Strong")
    .put("state", "Disabled")
    .put("moduleName", "mod-password-validator")
    .put("expression", "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$")
    .put("description", "At least one letter and one number")
    .put("orderNo", 1)
    .put("errMessageId", "");

  private static final JsonObject INVALID_RULE_NEGATIVE_ORDER_NUMBER = new JsonObject()
    .put("name", "invalid rule with negative order number")
    .put("type", "Programmatic")
    .put("validationType", "Soft")
    .put("state", "Disabled")
    .put("moduleName", "mod-password-validator")
    .put("expression", "")
    .put("implementationReference", "Some implementation")
    .put("description", "Programmatic rule")
    .put("orderNo", -1)
    .put("errMessageId", "");

  private static final JsonObject INVALID_REGEXP_RULE_SOFT = new JsonObject()
    .put("name", "invalid regexp rule with soft validation type")
    .put("type", "RegExp")
    .put("validationType", "Soft")
    .put("state", "Enabled")
    .put("moduleName", "mod-password-validator")
    .put("expression", "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$")
    .put("description", "At least one letter and one number")
    .put("orderNo", 1)
    .put("errMessageId", "");

  private static final JsonObject VALID_RULE = new JsonObject()
    .put("name", "valid regexp rule")
    .put("type", "RegExp")
    .put("validationType", "Strong")
    .put("state", "Enabled")
    .put("moduleName", "mod-password-validator")
    .put("expression", "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$")
    .put("description", "At least one letter and one number")
    .put("orderNo", 1);

  private static final String TENANT_RULES_PATH = "/tenant/rules";
  private static final String HOST = "http://localhost:";
  private static final String HTTP_PORT = "http.port";
  private static final String TENANT = "diku";
  private static final String INCORRECT_TENANT = "test";
  private static final String RULE_ID = "ruleId";
  private static final String ID = "id";
  private static final String VALIDATION_RULES_TABLE_NAME = "validation_rules";

  private static final Header TENANT_HEADER = new Header(RestVerticle.OKAPI_HEADER_TENANT, TENANT);

  private static Vertx vertx;
  private static int port;

  @org.junit.Rule
  public Timeout timeout = Timeout.seconds(180);

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
    Async async = context.async();
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();

    PostgresClient.getInstance(vertx).startEmbeddedPostgres();

    TenantClient tenantClient = new TenantClient(HOST + port, "diku", null);

    final DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put(HTTP_PORT, port));
    vertx.deployVerticle(RestVerticle.class.getName(), options, res -> {
      try {
        TenantAttributes t = new TenantAttributes().withModuleTo("mod-password-validator-1.0.0");
        tenantClient.postTenant(t, res2 -> {
          async.complete();
        });
      } catch (Exception e) {
        logger.error(e.getMessage());
      }
    });
  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
  }

  @Before
  public void setUp(TestContext context) {
    clearRulesTable(context);
  }

  @Test
  public void shouldReturnEmptyListIfNoRulesExist(final TestContext context) {
    RestAssured.given()
      .port(port)
      .header(TENANT_HEADER)
      .when()
      .get(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("totalRecords", is(0))
      .body("rules", empty());
  }

  @Test
  public void shouldReturnAllTenantRulesWhenNoQueryIsSpecified(final TestContext context) {
    List<JsonObject> rulesToPost = Arrays.asList(PROGRAMMATIC_RULE_DISABLED, REGEXP_RULE_DISABLED,
      REGEXP_RULE_ENABLED, PROGRAMMATIC_RULE_ENABLED);
    for (JsonObject rule : rulesToPost) {
      RestAssured.given()
        .port(port)
        .contentType(MediaType.APPLICATION_JSON)
        .header(TENANT_HEADER)
        .body(rule.toString())
        .when()
        .post(TENANT_RULES_PATH)
        .then()
        .statusCode(HttpStatus.SC_CREATED);
    }

    Object[] ruleNames = rulesToPost.stream().map(r -> r.getString("name")).toArray();
    RestAssured.given()
      .port(port)
      .header(TENANT_HEADER)
      .when()
      .get(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("totalRecords", is(rulesToPost.size()))
      .body("rules*.name", contains(ruleNames));
  }

  @Test
  public void shouldNotReturnProgrammaticOnGetTenantRulesByTypeRegExp(final TestContext context) {
    List<JsonObject> rulesToPost = Arrays.asList(REGEXP_RULE_ENABLED, PROGRAMMATIC_RULE_ENABLED);
    for (JsonObject rule : rulesToPost) {
      RestAssured.given()
        .port(port)
        .contentType(MediaType.APPLICATION_JSON)
        .header(TENANT_HEADER)
        .body(rule.toString())
        .when()
        .post(TENANT_RULES_PATH)
        .then()
        .statusCode(HttpStatus.SC_CREATED);
    }


    RestAssured.given()
      .port(port)
      .header(TENANT_HEADER)
      .param("query", "type=RegExp")
      .when()
      .get(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("rules*.type", everyItem(not(Rule.Type.PROGRAMMATIC.toString())));

  }

  @Test
  public void testFailedGetTenantRules() {
    RestAssured.given()
      .port(port)
      .header(new Header(RestVerticle.OKAPI_HEADER_TENANT, INCORRECT_TENANT))
      .param("query", "type=RegExp")
      .when()
      .get(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void testFailedGetTenantRulesByRuleId() {
    requestSpecification()
      .header(new Header(RestVerticle.OKAPI_HEADER_TENANT, INCORRECT_TENANT))
      .pathParam("ruleId", UUID.randomUUID())
      .when()
      .get(TENANT_RULES_PATH + "/{ruleId}")
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void testFailedPostTenantRules() {
    List<JsonObject> rules = Arrays.asList(REGEXP_RULE_ENABLED, PROGRAMMATIC_RULE_ENABLED);
    requestSpecification()
      .header(new Header(RestVerticle.OKAPI_HEADER_TENANT, INCORRECT_TENANT))
      .body(rules.get(0).toString())
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void testFailedPutTenantRules() {
    List<JsonObject> rules = Arrays.asList(REGEXP_RULE_ENABLED, PROGRAMMATIC_RULE_ENABLED);
    requestSpecification()
      .header(new Header(RestVerticle.OKAPI_HEADER_TENANT, INCORRECT_TENANT))
      .body(rules.get(0).toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldNotReturnDisabledOnGetTenantRulesByStateEnabled(final TestContext context) {
    List<JsonObject> rulesToPost = Arrays.asList(PROGRAMMATIC_RULE_DISABLED, REGEXP_RULE_DISABLED,
      REGEXP_RULE_ENABLED, PROGRAMMATIC_RULE_ENABLED);
    for (JsonObject rule : rulesToPost) {
      RestAssured.given()
        .port(port)
        .contentType(MediaType.APPLICATION_JSON)
        .header(TENANT_HEADER)
        .body(rule.toString())
        .when()
        .post(TENANT_RULES_PATH)
        .then()
        .statusCode(HttpStatus.SC_CREATED);
    }

    RestAssured.given()
      .port(port)
      .header(TENANT_HEADER)
      .param("query", "state=Enabled")
      .when()
      .get(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("rules*.state", everyItem(not(Rule.State.DISABLED.toString())));
  }

  @Test
  public void shouldReturnBadRequestOnPostWhenNoRulePassedInBody(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(new JsonObject().toString())
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void shouldReturnBadRequestOnPostWhenNegativeOrderNumber(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(INVALID_RULE_NEGATIVE_ORDER_NUMBER.toString())
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturnBadRequestOnPostWhenSoftValidationTypeForRegexpType(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(INVALID_REGEXP_RULE_SOFT.toString())
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturnBadRequestOnPostWhenNoImplementationReferenceSpecifiedForProgrammaticType(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(buildProgrammaticRuleEnabled().put("implementationReference", "").toString())
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);

    requestSpecification()
      .header(TENANT_HEADER)
      .body(buildProgrammaticRuleEnabled().put("implementationReference", (String) null).toString())
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldCreateValidRule(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(VALID_RULE.toString())
      .when()
      .post(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .body("name", is(VALID_RULE.getString("name")))
      .body("type", is(VALID_RULE.getString("type")))
      .body("validationType", is(VALID_RULE.getString("validationType")))
      .body("orderNo", is(VALID_RULE.getInteger("orderNo")))
      .body("state", is(VALID_RULE.getString("state")))
      .body("moduleName", is(VALID_RULE.getString("moduleName")))
      .body("expression", is(VALID_RULE.getString("expression")))
      .body("description", is(VALID_RULE.getString("description")));
  }

  @Test
  public void shouldReturnBadRequestOnPutWhenNoRulePassedInBody(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(new JsonObject().toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void shouldReturnBadRequestOnPutWhenNegativeOrderNumber(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(INVALID_RULE_NEGATIVE_ORDER_NUMBER.toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturnBadRequestOnPutWhenSoftValidationTypeForRegexpType(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(INVALID_REGEXP_RULE_SOFT.toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturnBadRequestOnPutWhenNoImplementationReferenceSpecifiedForProgrammaticType(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(buildProgrammaticRuleEnabled().put("implementationReference", "").toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);

    requestSpecification()
      .header(TENANT_HEADER)
      .body(buildProgrammaticRuleEnabled().put("implementationReference", (String) null).toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturnNotFoundWhenRuleDoesNotExist(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .body(REGEXP_RULE_ENABLED.put(ID, UUID.randomUUID().toString()).toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  public void testUpdateExistingRuleCases(final TestContext context) {
    Response response = requestSpecification()
      .header(TENANT_HEADER)
      .body(PROGRAMMATIC_RULE_DISABLED.toString())
      .when()
      .post(TENANT_RULES_PATH);

    Assert.assertThat(response.statusCode(), is(HttpStatus.SC_CREATED));
    Rule createdRule = response.body().as(Rule.class);

    /* no id and no ruleId → 400 */
    requestSpecification()
      .header(TENANT_HEADER)
      .body(createdRule.toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);

    /* no id only → 200 */
    JsonObject ruleToUpdate = buildProgrammaticRuleDisabled()
      .put(RULE_ID, createdRule.getRuleId())
      .put("state", Rule.State.ENABLED.toString());

    requestSpecification()
      .header(TENANT_HEADER)
      .body(ruleToUpdate.toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("state", is(Rule.State.ENABLED.toString()));

    /* no ruleId only → 200 */
    ruleToUpdate = buildProgrammaticRuleDisabled()
      .put(ID, createdRule.getRuleId())
      .put("state", Rule.State.DISABLED.toString());

    requestSpecification()
      .header(TENANT_HEADER)
      .body(ruleToUpdate.toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("state", is(Rule.State.DISABLED.toString()));

    /* different id and ruleId → 400 */
    ruleToUpdate = buildProgrammaticRuleDisabled()
      .put(ID, createdRule.getRuleId())
      .put(RULE_ID, UUID.randomUUID().toString());

    requestSpecification()
      .header(TENANT_HEADER)
      .body(ruleToUpdate.toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);

    /* id and ruleId do not exist → 404 */
    String randomId = UUID.randomUUID().toString();
    ruleToUpdate = buildProgrammaticRuleDisabled()
      .put(ID, randomId)
      .put(RULE_ID, randomId)
      .put("state", Rule.State.ENABLED.toString());

    requestSpecification()
      .header(TENANT_HEADER)
      .body(ruleToUpdate.toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND);

    /* should be updated */
    ruleToUpdate = buildProgrammaticRuleDisabled()
      .put(RULE_ID, createdRule.getRuleId())
      .put(ID, createdRule.getRuleId())
      .put("state", Rule.State.ENABLED.toString());

    requestSpecification()
      .header(TENANT_HEADER)
      .body(ruleToUpdate.toString())
      .when()
      .put(TENANT_RULES_PATH)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("state", is(Rule.State.ENABLED.toString()));
  }

  @Test
  public void shouldReturnNotFoundOnGetRuleByIdWhenRuleDoesNotExist(final TestContext context) {
    requestSpecification()
      .header(TENANT_HEADER)
      .pathParam("ruleId", UUID.randomUUID().toString())
      .when()
      .get(TENANT_RULES_PATH + "/{ruleId}")
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  public void shouldReturnRuleById(final TestContext context) {
    Response response = requestSpecification()
      .header(TENANT_HEADER)
      .body(PROGRAMMATIC_RULE_DISABLED.toString())
      .when()
      .post(TENANT_RULES_PATH);
    Assert.assertThat(response.statusCode(), is(HttpStatus.SC_CREATED));
    Rule createdRule = response.body().as(Rule.class);

    requestSpecification()
      .header(TENANT_HEADER)
      .pathParam("ruleId", createdRule.getRuleId())
      .when()
      .get(TENANT_RULES_PATH + "/{ruleId}")
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("ruleId", is(createdRule.getRuleId()));
  }

  private RequestSpecification requestSpecification() {
    return RestAssured.given()
      .port(port)
      .contentType(MediaType.APPLICATION_JSON);
  }

  private void clearRulesTable(TestContext context) {
    PostgresClient.getInstance(vertx, TENANT).delete(VALIDATION_RULES_TABLE_NAME, new Criterion(), event -> {
      if (event.failed()) {
        context.fail(event.cause());
      }
    });
  }

  private JsonObject buildProgrammaticRuleDisabled() {
    return new JsonObject()
      .put("name", "programmatic rule disabled")
      .put("type", "Programmatic")
      .put("validationType", "Soft")
      .put("state", "Disabled")
      .put("moduleName", "mod-password-validator")
      .put("expression", "")
      .put("implementationReference", "Some implementation")
      .put("description", "Programmatic rule")
      .put("orderNo", 0)
      .put("errMessageId", "");
  }

  private JsonObject buildProgrammaticRuleEnabled() {
    return new JsonObject()
      .put("name", "programmatic rule enabled")
      .put("type", "Programmatic")
      .put("validationType", "Soft")
      .put("state", "Disabled")
      .put("moduleName", "mod-password-validator")
      .put("expression", "")
      .put("implementationReference", "Some implementation")
      .put("description", "Programmatic rule")
      .put("orderNo", 1)
      .put("errMessageId", "");
  }
}

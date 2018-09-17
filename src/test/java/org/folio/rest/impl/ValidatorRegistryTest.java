package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.RuleCollection;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(VertxUnitRunner.class)
public class ValidatorRegistryTest {

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
  private static final String RULE_ID = "ruleId";
  private static final String VALIDATION_RULES_TABLE_NAME = "validation_rules";

  private static Vertx vertx;
  private static int port;

  @Rule
  public Timeout rule = Timeout.seconds(180);

  @BeforeClass
  public static void setUpClass(final TestContext context) throws IOException {
    Async async = context.async();
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();

    PostgresClient.setIsEmbedded(true);
    PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    TenantClient tenantClient = new TenantClient("localhost", port, TENANT, "diku");

    final DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put(HTTP_PORT, port));
    vertx.deployVerticle(RestVerticle.class.getName(), options, res -> {
      try {
        tenantClient.post(null, res2 -> {
          async.complete();
        });
      } catch (Exception e) {
        e.printStackTrace();
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

  @After
  public void tearDown(TestContext context) throws Exception {
    PostgresClient.getInstance(vertx, TENANT).delete(VALIDATION_RULES_TABLE_NAME, new Criterion(), event -> {
      if (event.failed()) {
        context.fail(event.cause());
      }
    });
  }

  @Test
  public void shouldReturnEmptyListIfNoRulesExist(final TestContext context) {
    final Async async = context.async();
    getTenantRules(result -> {
      context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
      RuleCollection collection = new JsonObject(result.result().getBody()).mapTo(RuleCollection.class);
      context.assertTrue(collection.getTotalRecords() == 0);
      List<org.folio.rest.jaxrs.model.Rule> rules = collection.getRules();
      context.assertTrue(rules.size() == 0);
      })
      .setHandler(result -> {
        if(result.failed()) {
          context.fail(result.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnAllTenantRules(final TestContext context) {
    final Async async = context.async();
    postRule(PROGRAMMATIC_RULE_DISABLED, HttpStatus.SC_CREATED, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED))
      .compose(r -> postRule(REGEXP_RULE_DISABLED, HttpStatus.SC_CREATED, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED)))
      .compose(r -> postRule(REGEXP_RULE_ENABLED, HttpStatus.SC_CREATED, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED)))
      .compose(r -> postRule(PROGRAMMATIC_RULE_ENABLED, HttpStatus.SC_CREATED, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED)))
      .compose(r -> getTenantRules(result -> {
        context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
        RuleCollection collection = new JsonObject(result.result().getBody()).mapTo(RuleCollection.class);
        context.assertTrue(collection.getTotalRecords() == 4);
        List<org.folio.rest.jaxrs.model.Rule> rules = collection.getRules();
        context.assertTrue(rules.size() == 4);
        context.assertTrue(rules.stream().filter(rule1 ->
          rule1.getName().equals(PROGRAMMATIC_RULE_DISABLED.getString("name"))).collect(Collectors.toList()).size() == 1);
        context.assertTrue(rules.stream().filter(rule1 ->
          rule1.getName().equals(REGEXP_RULE_DISABLED.getString("name"))).collect(Collectors.toList()).size() == 1);
        context.assertTrue(rules.stream().filter(rule1 ->
          rule1.getName().equals(REGEXP_RULE_ENABLED.getString("name"))).collect(Collectors.toList()).size() == 1);
        context.assertTrue(rules.stream().filter(rule1 ->
          rule1.getName().equals(PROGRAMMATIC_RULE_ENABLED.getString("name"))).collect(Collectors.toList()).size() == 1);
      }))
      .setHandler(chainedRes -> {
        if(chainedRes.failed()) {
          context.fail(chainedRes.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnBadRequestOnPostWhenNoRulePassedInBody(final TestContext context) {
    final Async async = context.async();
    postRule(new JsonObject(), HttpStatus.SC_UNPROCESSABLE_ENTITY, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_UNPROCESSABLE_ENTITY))
      .setHandler(result -> {
        if(result.failed()) {
          context.fail(result.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnBadRequestOnPostWhenNegativeOrderNumber(final TestContext context) {
    final Async async = context.async();
    postRule(INVALID_RULE_NEGATIVE_ORDER_NUMBER, HttpStatus.SC_BAD_REQUEST, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_BAD_REQUEST))
      .setHandler(result -> {
        if(result.failed()) {
          context.fail(result.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnBadRequestOnPostWhenSoftValidationTypeForRegexpType(final TestContext context) {
    final Async async = context.async();
    postRule(INVALID_REGEXP_RULE_SOFT, HttpStatus.SC_BAD_REQUEST, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_BAD_REQUEST))
      .setHandler(result -> {
        if(result.failed()) {
          context.fail(result.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnBadRequestOnPostWhenNoImplementationReferenceSpecifiedForProgrammaticType(final TestContext context) {
    final Async async = context.async();
    String ref = null;
    postRule(PROGRAMMATIC_RULE_ENABLED.put("implementationReference", ""), HttpStatus.SC_BAD_REQUEST, result ->
      context.assertEquals(result.result().getCode(), HttpStatus.SC_BAD_REQUEST))
      .compose(r -> postRule(PROGRAMMATIC_RULE_ENABLED.put("implementationReference", ref), HttpStatus.SC_BAD_REQUEST,
        result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_BAD_REQUEST)))
      .setHandler(result -> {
        if(result.failed()) {
          context.fail(result.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldCreateValidRule(final TestContext context) {
    final Async async = context.async();
    postRule(VALID_RULE, HttpStatus.SC_CREATED, result -> {
        context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED);
        org.folio.rest.jaxrs.model.Rule rule = new JsonObject(result.result().getBody()).mapTo(org.folio.rest.jaxrs.model.Rule.class);
        context.assertEquals(rule.getName(), VALID_RULE.getString("name"));
        context.assertEquals(rule.getType().toString(), VALID_RULE.getString("type"));
        context.assertEquals(rule.getValidationType().toString(), VALID_RULE.getString("validationType"));
        context.assertEquals(rule.getOrderNo(), VALID_RULE.getInteger("orderNo"));
        context.assertEquals(rule.getState().toString(), VALID_RULE.getString("state"));
        context.assertEquals(rule.getModuleName(), VALID_RULE.getString("moduleName"));
        context.assertEquals(rule.getExpression(), VALID_RULE.getString("expression"));
        context.assertEquals(rule.getDescription(), VALID_RULE.getString("description"));
    })
      .setHandler(chainedRes -> {
        if(chainedRes.failed()) {
          context.fail(chainedRes.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnBadRequestOnPutWhenNoRulePassedInBody(final TestContext context) {
    final Async async = context.async();
    updateRule(new JsonObject(), HttpStatus.SC_UNPROCESSABLE_ENTITY, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_UNPROCESSABLE_ENTITY))
      .setHandler(result -> {
        if(result.failed()) {
          context.fail(result.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnBadRequestOnPutWhenNegativeOrderNumber(final TestContext context) {
    final Async async = context.async();
    updateRule(INVALID_RULE_NEGATIVE_ORDER_NUMBER, HttpStatus.SC_BAD_REQUEST, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_BAD_REQUEST))
      .setHandler(result -> {
        if(result.failed()) {
          context.fail(result.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnBadRequestOnPutWhenSoftValidationTypeForRegexpType(final TestContext context) {
    final Async async = context.async();
    updateRule(INVALID_REGEXP_RULE_SOFT, HttpStatus.SC_BAD_REQUEST, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_BAD_REQUEST))
      .setHandler(result -> {
        if(result.failed()) {
          context.fail(result.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnBadRequestOnPutWhenNoImplementationReferenceSpecifiedForProgrammaticType(final TestContext context) {
    final Async async = context.async();
    String ref = null;
    updateRule(PROGRAMMATIC_RULE_ENABLED.put("implementationReference", ""), HttpStatus.SC_BAD_REQUEST, result ->
      context.assertEquals(result.result().getCode(), HttpStatus.SC_BAD_REQUEST))
      .compose(r -> updateRule(PROGRAMMATIC_RULE_ENABLED.put("implementationReference", ref), HttpStatus.SC_BAD_REQUEST,
        result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_BAD_REQUEST)))
      .setHandler(result -> {
        if(result.failed()) {
          context.fail(result.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnNotFoundWhenRuleDoesNotExist(final TestContext context) {
    final Async async = context.async();
    updateRule(REGEXP_RULE_ENABLED, HttpStatus.SC_NOT_FOUND, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_NOT_FOUND))
      .setHandler(result -> {
        if(result.failed()) {
          context.fail(result.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldUpdateExistingRule(final TestContext context) {
    final Async async = context.async();
    postRule(PROGRAMMATIC_RULE_DISABLED, HttpStatus.SC_CREATED, result -> {
      context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED);
      org.folio.rest.jaxrs.model.Rule rule = new JsonObject(result.result().getBody()).mapTo(org.folio.rest.jaxrs.model.Rule.class);
      PROGRAMMATIC_RULE_DISABLED.put(RULE_ID, rule.getRuleId());
    })
      .compose(r -> updateRule(PROGRAMMATIC_RULE_DISABLED.put("state", org.folio.rest.jaxrs.model.Rule.State.ENABLED.toString()), HttpStatus.SC_OK, result -> {
        context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
        org.folio.rest.jaxrs.model.Rule rule = new JsonObject(result.result().getBody()).mapTo(org.folio.rest.jaxrs.model.Rule.class);
        context.assertEquals(rule.getState(), org.folio.rest.jaxrs.model.Rule.State.ENABLED);
        }))
      .setHandler(chainedRes -> {
        if(chainedRes.failed()) {
          context.fail(chainedRes.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnNotFoundOnGetRuleByIdWhenRuleDoesNotExist(final TestContext context) {
    final Async async = context.async();
    getRuleById("nonexistent_rule_id", HttpStatus.SC_NOT_FOUND, result -> context.assertEquals(result.result().getCode(), HttpStatus.SC_NOT_FOUND))
      .setHandler(result -> {
        if(result.failed()) {
          context.fail(result.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void shouldReturnRuleById(final TestContext context) {
    final Async async = context.async();
    postRule(PROGRAMMATIC_RULE_DISABLED, HttpStatus.SC_CREATED, result -> {
      context.assertEquals(result.result().getCode(), HttpStatus.SC_CREATED);
      org.folio.rest.jaxrs.model.Rule rule = new JsonObject(result.result().getBody()).mapTo(org.folio.rest.jaxrs.model.Rule.class);
      PROGRAMMATIC_RULE_DISABLED.put(RULE_ID, rule.getRuleId());
    })
      .compose(r -> getRuleById(PROGRAMMATIC_RULE_DISABLED.getString(RULE_ID), HttpStatus.SC_OK, result -> {
        context.assertEquals(result.result().getCode(), HttpStatus.SC_OK);
        org.folio.rest.jaxrs.model.Rule rule = new JsonObject(result.result().getBody()).mapTo(org.folio.rest.jaxrs.model.Rule.class);
        context.assertEquals(rule.getRuleId(), PROGRAMMATIC_RULE_DISABLED.getString(RULE_ID));
      }))
      .setHandler(chainedRes -> {
        if(chainedRes.failed()) {
          context.fail(chainedRes.cause());
        } else {
          async.complete();
        }
      });
  }

  private Future<TestUtil.WrappedResponse> postRule(JsonObject rule, int expectedStatus, Handler<AsyncResult<TestUtil.WrappedResponse>> handler) {
    return TestUtil.doRequest(vertx, HOST + port + TENANT_RULES_PATH, HttpMethod.POST, null, rule.toString(),
      expectedStatus, "Adding new rule", handler);
  }

  private Future<TestUtil.WrappedResponse> getTenantRules(Handler<AsyncResult<TestUtil.WrappedResponse>> handler) {
    return TestUtil.doRequest(vertx, HOST + port + TENANT_RULES_PATH, HttpMethod.GET, null, null,
      HttpStatus.SC_OK, "Getting all tenant rules", handler);
  }

  private Future<TestUtil.WrappedResponse> updateRule(JsonObject rule, int expectedStatus, Handler<AsyncResult<TestUtil.WrappedResponse>> handler) {
    return TestUtil.doRequest(vertx, HOST + port + TENANT_RULES_PATH, HttpMethod.PUT, null, rule.toString(),
      expectedStatus, "Updating a rule", handler);
  }

  private Future<TestUtil.WrappedResponse> getRuleById(String ruleId, int expectedStatus, Handler<AsyncResult<TestUtil.WrappedResponse>> handler) {
    return TestUtil.doRequest(vertx, HOST + port + TENANT_RULES_PATH + "/" + ruleId, HttpMethod.GET, null, null,
      expectedStatus, "Getting a rule by id", handler);
  }

}

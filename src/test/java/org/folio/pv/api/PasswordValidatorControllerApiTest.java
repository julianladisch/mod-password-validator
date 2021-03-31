package org.folio.pv.api;

import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import static org.folio.pv.testutils.APITestUtils.PASSWORD_VALIDATE_PATH;
import static org.folio.pv.testutils.APITestUtils.mockGet;
import static org.folio.pv.testutils.APITestUtils.mockPost;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.folio.pv.domain.dto.Password;
import org.folio.pv.domain.dto.ValidationResult;

class PasswordValidatorControllerApiTest extends BaseApiTest {

  @Test
  void validateInvalidPassword() {
    mockGet("/users.*", "{\"users\":[{\"username\":\"cedrick\"}],\"totalRecords\":1}", SC_OK,
      APPLICATION_JSON_VALUE, wireMockServer
    );
    var password = new Password().password("test-password").userId(UUID.randomUUID().toString());

    var validationResult = verifyPost(PASSWORD_VALIDATE_PATH, password, SC_OK).as(ValidationResult.class);

    assertThat(validationResult)
      .hasFieldOrPropertyWithValue("result", "invalid");
  }

  @Test
  void validateValidPassword() {
    mockGet("/users.*", "{\"users\":[{\"username\":\"cedrick\"}],\"totalRecords\":1}", SC_OK,
      APPLICATION_JSON_VALUE, wireMockServer
    );
    mockPost("/authn/password/repeatable", "{\"result\":\"valid\"}", SC_OK, wireMockServer);
    mockGet("/range/.*", "0018A45C4D1DEF81644B54AB7F969B88D65:0", SC_OK, TEXT_PLAIN_VALUE, wireMockServer);
    var password = new Password().password("7Xu^&t[:J3Hha(<B").userId(UUID.randomUUID().toString());

    var validationResult = verifyPost(PASSWORD_VALIDATE_PATH, password, SC_OK).as(ValidationResult.class);

    assertThat(validationResult)
      .hasFieldOrPropertyWithValue("result", "valid");
  }
}
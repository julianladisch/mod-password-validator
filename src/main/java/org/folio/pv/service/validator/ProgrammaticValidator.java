package org.folio.pv.service.validator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.folio.pv.domain.ValidationType;
import org.folio.pv.domain.dto.Password;
import org.folio.pv.domain.dto.ValidationResult;
import org.folio.pv.domain.entity.PasswordValidationRule;
import org.folio.spring.FolioExecutionContext;

public class ProgrammaticValidator implements Validator {

  private final PasswordValidationRule rule;
  private final FolioExecutionContext folioExecutionContext;
  private final ObjectMapper jacksonObjectMapper;


  public ProgrammaticValidator(PasswordValidationRule rule, FolioExecutionContext folioExecutionContext,
      ObjectMapper jacksonObjectMapper) {
    this.rule = rule;
    this.folioExecutionContext = folioExecutionContext;
    this.jacksonObjectMapper = jacksonObjectMapper;
  }

  @Override
  public ValidationErrors validate(String password, UserData user) {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      var httpPost = new HttpPost(folioExecutionContext.getOkapiUrl() + rule.getImplementationReference());

      addHeaders(httpPost);
      addBody(httpPost, password, user);

      var response = client.execute(httpPost);

      var buffer = new ByteArrayOutputStream();
      response.getEntity().writeTo(buffer);

      String body = new String(buffer.toByteArray());

      var statusCode = response.getStatusLine().getStatusCode();
      if (statusCode < 200 || statusCode > 202) {
        if (ValidationType.STRONG.getValue().equals(rule.getValidationType())) {
          throw new RuntimeException(body);
        } else {
          return ValidationErrors.none();
        }
      }

      ValidationResult validationResult = jacksonObjectMapper.readValue(body, ValidationResult.class);
      return ValidationErrors.of(validationResult.getMessages());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void addBody(HttpPost httpPost, String password, UserData user) throws UnsupportedEncodingException,
      JsonProcessingException {
    httpPost.setEntity(new StringEntity(jacksonObjectMapper.writeValueAsString(
        new Password().password(password).userId(user.getId())))
    );
  }

  private void addHeaders(HttpPost httpPost) {
    folioExecutionContext.getOkapiHeaders()
        .forEach((key, values) -> values.forEach(value -> httpPost.addHeader(key, value)));
    // Some servers choke on the default accept string.
    httpPost.addHeader("accept", "*/*");
    httpPost.addHeader("content-type", "application/json");
  }

}
package org.folio.spring.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.async.AsyncService;
import org.folio.spring.integration.ErrorsDto;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

@Slf4j
@Component("errorDecoder")
@RequiredArgsConstructor
public class ClientErrorDecoder implements ErrorDecoder {

  private static final List<Integer> allowedCodes = asList(404, 400, 422, 403);
  private final ObjectMapper objectMapper;
  private final AsyncService async;

  @Override
  @SneakyThrows
  public Exception decode(String methodKey, Response response) {
    if (isNull(response)) {
      //response is null, meaning the previous call failed.
      //set previousFailure flag to true so that we don't send another error response to the client
      return new OkapiModuleClientException(500,
        "response is null from one of the services requested");
    }

    int statusCode = response.status();

    ErrorsDto errors = null;
    String message = IOUtils.toString(response.body().asInputStream());

    if (StringUtils.isNotEmpty(message)) {
      try {
        errors = objectMapper.readValue(message, ErrorsDto.class);
      } catch (IOException e) {
        // no-op
      }
    }
    log.warn(message);
    if (!allowedCodes.contains(statusCode) || (statusCode == 422 && errors == null)) {
      statusCode = 500;
    }
    async.getContext()
      .addError(String
        .format("%s method failed with status code %d and message: %s", methodKey, statusCode,
          message));

    return new OkapiModuleClientException(statusCode, message);
  }
}

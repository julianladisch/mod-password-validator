package org.folio.pv.testutils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.experimental.UtilityClass;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@UtilityClass
public class APITestUtils {

  public static final String TENANT_ID = "test";
  public static final String RULES_PATH = "/tenant/rules";
  public static final String PASSWORD_VALIDATE_PATH = "/password/validate";

  public static final String LIMIT_PARAM = "limit";

  public static String rulesPath(String... params) {
    if (params.length % 2 != 0) {
      throw new IllegalArgumentException("Params should be pairs of key and value");
    }
    var uriBuilder = new URIBuilder().setPath(RULES_PATH);
    for (int i = 0; i < params.length; i += 2) {
      String param = params[i];
      String value = params[i + 1];
      uriBuilder.addParameter(param, value);
    }
    try {
      return uriBuilder.build().toString();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  public static String rulePath(String id) {
    return RULES_PATH + "/" + id;
  }


  public static void mockGet(String url, String body, int status, String contentType, WireMockServer mockServer) {
    mockServer.stubFor(get(urlMatching(url))
      .willReturn(aResponse().withBody(body)
        .withHeader(HttpHeaders.CONTENT_TYPE, contentType)
        .withStatus(status)));
  }

  public static void mockPost(String url, String body, int status, WireMockServer mockServer) {
    mockServer.stubFor(post(urlMatching(url))
      .willReturn(aResponse().withBody(body)
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(status)));
  }
}

package org.folio.pv.client;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.folio.pv.domain.dto.HashedPasswordUsage;

@ExtendWith(MockitoExtension.class)
class HashedPasswordUsageCollectionConverterTest {

  private static final String VALID_INPUT = "0018A45C4D1DEF81644B54AB7F969B88D65:0\n"
    + "00D4F6E8FA6EECAD2A3AA415EEC418D38EC:2\n"
    + "011053FD0102E94D6AE2F8B83D76FAF94F6:1\n"
    + "012A7CA357541F0AC487871FEEC1891C49C:3\n"
    + "0136E006E24E7D152139815FB0FC6A50B15:2";

  @Test
  void testReadToList(@Mock HttpInputMessage inputMessage) throws IOException {
    when(inputMessage.getBody()).thenReturn(toInputStream(VALID_INPUT));
    when(inputMessage.getHeaders()).thenReturn(getHeaders());

    var converter = new HashedPasswordUsageCollectionConverter<List<HashedPasswordUsage>>();
    var actual = converter.read(getParameterizedListType(), null, inputMessage);

    assertThat(actual)
      .hasSize(5)
      .startsWith(new HashedPasswordUsage("0018A45C4D1DEF81644B54AB7F969B88D65", 0))
      .endsWith(new HashedPasswordUsage("0136E006E24E7D152139815FB0FC6A50B15", 2));
  }

  @Test
  void testReadToSortedSet(@Mock HttpInputMessage inputMessage) throws IOException {
    when(inputMessage.getBody()).thenReturn(toInputStream(VALID_INPUT));
    when(inputMessage.getHeaders()).thenReturn(getHeaders());

    var converter = new HashedPasswordUsageCollectionConverter<SortedSet<HashedPasswordUsage>>();
    var actual = converter.read(getParameterizedSortedSetType(), null, inputMessage);

    assertThat(actual)
      .hasSize(5)
      .startsWith(new HashedPasswordUsage("012A7CA357541F0AC487871FEEC1891C49C", 3))
      .endsWith(new HashedPasswordUsage("0018A45C4D1DEF81644B54AB7F969B88D65", 0));
  }

  @Test
  void testReadToSet(@Mock HttpInputMessage inputMessage) throws IOException {
    when(inputMessage.getBody()).thenReturn(toInputStream(VALID_INPUT));
    when(inputMessage.getHeaders()).thenReturn(getHeaders());

    var converter = new HashedPasswordUsageCollectionConverter<LinkedHashSet<HashedPasswordUsage>>();
    var actual = converter.read(getParameterizedSetType(), null, inputMessage);

    assertThat(actual)
      .hasSize(5)
      .startsWith(new HashedPasswordUsage("0018A45C4D1DEF81644B54AB7F969B88D65", 0))
      .endsWith(new HashedPasswordUsage("0136E006E24E7D152139815FB0FC6A50B15", 2));
  }

  @Test
  void testReadInvalidInputMessage(@Mock HttpInputMessage inputMessage) throws IOException {
    when(inputMessage.getBody()).thenReturn(toInputStream("0018A45C4D1DEF81644B54AB7F969B88D65:p"));
    when(inputMessage.getHeaders()).thenReturn(getHeaders());

    var converter = new HashedPasswordUsageCollectionConverter<LinkedHashSet<HashedPasswordUsage>>();

    var parameterizedSetType = getParameterizedSetType();
    var exception = Assertions.assertThrows(HttpMessageNotReadableException.class,
      () -> converter.read(parameterizedSetType, null, inputMessage));
    assertThat(exception).hasMessageContaining("Invalid format of the line");
  }

  @Test
  void testCanRead() {
    var converter = new HashedPasswordUsageCollectionConverter<LinkedHashSet<HashedPasswordUsage>>();
    var actual = converter.canRead(getParameterizedSetType(), null, MediaType.TEXT_PLAIN);

    Assertions.assertTrue(actual);
  }

  private HttpHeaders getHeaders() {
    var headers = new HttpHeaders();
    headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
    return headers;
  }

  @SuppressWarnings("UnstableApiUsage")
  private Type getParameterizedListType() {
    return new TypeToken<List<HashedPasswordUsage>>() {

    }.getType();
  }

  @SuppressWarnings("UnstableApiUsage")
  private Type getParameterizedSortedSetType() {
    return new TypeToken<SortedSet<HashedPasswordUsage>>() {

    }.getType();
  }

  @SuppressWarnings("UnstableApiUsage")
  private Type getParameterizedSetType() {
    return new TypeToken<Set<HashedPasswordUsage>>() {

    }.getType();
  }
}
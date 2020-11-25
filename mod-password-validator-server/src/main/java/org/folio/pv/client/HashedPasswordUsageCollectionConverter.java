package org.folio.pv.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import org.folio.pv.domain.dto.HashedPasswordUsage;

public class HashedPasswordUsageCollectionConverter<T extends Collection<HashedPasswordUsage>>
    extends AbstractGenericHttpMessageConverter<T> {

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private final Pattern usagePattern = Pattern.compile("\\s*([0-9a-fA-F]+)\\s*:\\s*(\\d+)\\s*");


  public HashedPasswordUsageCollectionConverter() {
    super(MediaType.TEXT_PLAIN);
  }

  @Override
  public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
    return false;
  }

  @Override
  public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
    if (!(type instanceof ParameterizedType)) {
      return false;
    }
    
    ParameterizedType parameterizedType = (ParameterizedType) type;
    if (!(parameterizedType.getRawType() instanceof Class)) {
      return false;
    }
    
    Class<?> rawType = (Class<?>) parameterizedType.getRawType();
    if (!(Collection.class.isAssignableFrom(rawType))) {
      return false;
    }
    
    if (parameterizedType.getActualTypeArguments().length != 1) {
      return false;
    }
    
    Type typeArgument = parameterizedType.getActualTypeArguments()[0];
    if (!(typeArgument instanceof Class)) {
      return false;
    }
    
    Class<?> typeArgumentClass = (Class<?>) typeArgument;
    return HashedPasswordUsage.class.isAssignableFrom(typeArgumentClass) && canRead(mediaType);
  }

  @Override
  public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
    return false;
  }

  @Override
  public boolean canWrite(@Nullable Type type, @Nullable Class<?> clazz, @Nullable MediaType mediaType) {
    return false;
  }

  @Override
  public T read(Type type, Class<?> contextClass,
      HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
    
    ParameterizedType parameterizedType = (ParameterizedType) type;
    T result = createCollection((Class<?>) parameterizedType.getRawType());

    try (BufferedReader reader = getReader(inputMessage)) {
      String line;
      int ln = 0;

      while ((line = reader.readLine()) != null) {
        result.add(parseUsage(line, ln, inputMessage));
        ln++;
      }
    }

    logger.debug("Total number of hashed password usages extracted from the input: " + result.size());

    return result;
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return false;
  }

  @Override
  protected void writeInternal(T hashedPasswordUsages, Type type,
      HttpOutputMessage outputMessage) throws HttpMessageNotWritableException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected T readInternal(Class<? extends T> clazz,
      HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
    throw new UnsupportedOperationException();
  }

  private HashedPasswordUsage parseUsage(String line, int lineNumber, HttpInputMessage inputMessage) {
    Matcher m = usagePattern.matcher(line);

    if (m.matches()) {
      String suffix = m.group(1);

      int usageCount;
      try {
        usageCount = Integer.parseInt(m.group(2));
      } catch (NumberFormatException e) {
        throw new HttpMessageNotReadableException(
            "[line:" + lineNumber + "] Invalid usage number value: " + m.group(2),
            e, inputMessage);
      }

      return new HashedPasswordUsage(suffix, usageCount);
    } else {
      throw new HttpMessageNotReadableException(
          "[line:" + lineNumber + "] Invalid format of the line: '" + line + "'",
          inputMessage);
    }
  }

  @SuppressWarnings("unchecked")
  private T createCollection(Class<?> collectionClass) {
    if (!collectionClass.isInterface()) {
      try {
        return (T) ReflectionUtils.accessibleConstructor(collectionClass).newInstance();
      } catch (Throwable ex) {
        throw new IllegalArgumentException("Could not instantiate collection class: " + collectionClass.getName(), ex);
      }
    } else if (List.class == collectionClass) {
      return (T) new ArrayList();
    } else if (SortedSet.class == collectionClass) {
      return (T) new TreeSet();
    } else {
      return (T) new LinkedHashSet();
    }
  }
  
  private static BufferedReader getReader(HttpInputMessage inputMessage) throws IOException {
    return new BufferedReader(new InputStreamReader(inputMessage.getBody(), getCharset(inputMessage.getHeaders())));
  }

  private static Charset getCharset(HttpHeaders headers) {
    Charset charset = headers.getContentType() != null ? headers.getContentType().getCharset() : null;
    return (charset != null ? charset : DEFAULT_CHARSET);
  }
}
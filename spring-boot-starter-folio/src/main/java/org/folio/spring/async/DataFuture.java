package org.folio.spring.async;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "of")
public class DataFuture<E> {

  private final E data;
  @Getter
  private final CompletableFuture completableFuture;

  public E retrieveData() {
    completableFuture.join();
    return data;
  }
}

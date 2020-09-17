package org.folio.spring.async;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor(staticName = "of")
public class ContextAwareJob implements Runnable {

  private final Runnable task;

  @Getter
  private final Map<String, Collection<String>> headers;

  @Getter
  private List<String> errors = new ArrayList<>();


  public synchronized void addError(String error) {
    errors.add(error);
  }

  @Override
  public void run() {
    task.run();
  }
}

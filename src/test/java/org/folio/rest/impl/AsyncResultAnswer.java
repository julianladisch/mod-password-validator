package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Answer for mocking async results
 */
public class AsyncResultAnswer<T> implements Answer<Void> {

  private AsyncResult<T> result;

  private int argumentIndex;

  /**
   * Constructor
   * @param result result to pass to handler
   * @param handlerArgumentIndex index of handler in mocked method
   */
  public AsyncResultAnswer(AsyncResult<T> result, int handlerArgumentIndex) {
    this.result = result;
    this.argumentIndex = handlerArgumentIndex;
  }

  @Override
  public Void answer(InvocationOnMock invocation) throws Throwable {
    Handler<AsyncResult<T>> handler = invocation.getArgument(argumentIndex);
    handler.handle(result);
    return null;
  }
}

package org.folio.rest.impl;

import io.vertx.core.Handler;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Answer for mocking handler results
 */
public class GenericHandlerAnswer<A, R> implements Answer<R> {

  private A handlerResult;
  private int argumentIndex;
  private R returnResult;

  /**
   * Constructor
   *
   * @param handlerResult        result to pass to handler
   * @param handlerArgumentIndex index of handler in mocked method
   */
  public GenericHandlerAnswer(A handlerResult, int handlerArgumentIndex) {
    this.handlerResult = handlerResult;
    this.argumentIndex = handlerArgumentIndex;
  }

  /**
   * Constructor
   *
   * @param handlerResult        result to pass to handler
   * @param handlerArgumentIndex index of handler in mocked method
   * @param returnResult         result to return
   */
  public GenericHandlerAnswer(A handlerResult, int handlerArgumentIndex, R returnResult) {
    this(handlerResult, handlerArgumentIndex);
    this.returnResult = returnResult;
  }


  @Override
  public R answer(InvocationOnMock invocation) {
    Handler<A> handler = invocation.getArgument(argumentIndex);
    handler.handle(handlerResult);
    return returnResult;
  }
}

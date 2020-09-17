package org.folio.spring.async;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.runAsync;
import static org.folio.spring.utils.RequestUtils.getHeadersFromRequest;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

@Slf4j
public class AsyncService {

  private ExecutorService executor;

  private int threadPoolSize = 50;

  @Getter
  private ThreadLocal<ContextAwareJob> requestContext = new ThreadLocal<>();

  public AsyncService() {
    executor = newLimitedCachedThreadPool(threadPoolSize, "service");
  }

  public CompletableFuture<Void> run(Runnable jobSupplier) {
    if (requestContext.get() != null && isNotEmpty(requestContext.get().getErrors())) {
      log.info("Skip processing because of previous errors");
      return CompletableFuture.completedFuture(null);
    }
    return runAnyway(jobSupplier);
  }

  public CompletableFuture<Void> runAnyway(Runnable jobSupplier) {
    return runAsync(ContextAwareJob.of(jobSupplier, getHeadersFromRequest()), executor);
  }

  private ExecutorService newLimitedCachedThreadPool(int threadNum, String name) {
    return new ThreadPoolExecutor(0, threadNum,
      20L, TimeUnit.SECONDS, new SynchronousQueue<>(),
      new CustomizableThreadFactory(name + "-exec-")) {
      @Override
      @SneakyThrows
      protected void beforeExecute(Thread t, Runnable r) {
        requestContext.set(((ContextAwareJob) FieldUtils.readField(r, "fn", true)));
        super.beforeExecute(t, r);
      }
    };
  }

  public ContextAwareJob getContext() {
    return requestContext.get();
  }
}

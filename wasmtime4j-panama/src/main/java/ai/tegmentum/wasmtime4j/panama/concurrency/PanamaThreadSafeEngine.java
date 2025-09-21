package ai.tegmentum.wasmtime4j.panama.concurrency;

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.EngineStatistics;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.aot.AotCompiler;
import ai.tegmentum.wasmtime4j.concurrency.ConcurrencyStatistics;
import ai.tegmentum.wasmtime4j.concurrency.ConcurrentExecutionContext;
import ai.tegmentum.wasmtime4j.concurrency.ConcurrentModule;
import ai.tegmentum.wasmtime4j.concurrency.ThreadSafeEngine;
import ai.tegmentum.wasmtime4j.concurrency.ThreadSafeStore;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.PanamaEngine;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.serialization.ModuleSerializer;
import ai.tegmentum.wasmtime4j.serialization.SerializedModule;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of a thread-safe WebAssembly engine with concurrent execution support.
 *
 * <p>This implementation wraps the standard Panama engine with thread safety guarantees and
 * concurrent execution capabilities. It uses the PanamaConcurrencyManager for resource
 * synchronization and adds async compilation support.
 *
 * @since 1.0.0
 */
public final class PanamaThreadSafeEngine implements ThreadSafeEngine {

  private static final Logger LOGGER = Logger.getLogger(PanamaThreadSafeEngine.class.getName());

  private final PanamaEngine delegate;
  private final PanamaConcurrencyManager concurrencyManager;
  private final AtomicInteger maxConcurrentCompilations;
  private final AtomicInteger activeCompilations;
  private final AtomicLong totalCompilations;
  private final Semaphore compilationSemaphore;
  private final ConcurrentHashMap<Long, CompletableFuture<?>> activeOperations;
  private volatile ExecutorService executorService;
  private volatile boolean closed;

  /**
   * Creates a new thread-safe engine wrapping the provided Panama engine.
   *
   * @param delegate the underlying Panama engine
   * @throws IllegalArgumentException if delegate is null
   */
  public PanamaThreadSafeEngine(final PanamaEngine delegate) {
    this.delegate = PanamaValidation.requireNonNull(delegate, "delegate");
    this.concurrencyManager = new PanamaConcurrencyManager();
    this.maxConcurrentCompilations =
        new AtomicInteger(Runtime.getRuntime().availableProcessors() * 2);
    this.activeCompilations = new AtomicInteger(0);
    this.totalCompilations = new AtomicLong(0);
    this.compilationSemaphore = new Semaphore(maxConcurrentCompilations.get(), true);
    this.activeOperations = new ConcurrentHashMap<>();
    this.executorService = createDefaultExecutorService();
    this.closed = false;

    LOGGER.info(
        "Created PanamaThreadSafeEngine with max concurrent compilations: "
            + maxConcurrentCompilations.get());
  }

  @Override
  public ThreadSafeStore createStore() throws WasmException {
    validateNotClosed();
    return concurrencyManager.executeWithWriteLock(
        getEngineHandle(),
        () -> new PanamaThreadSafeStore(delegate.createStore(), concurrencyManager));
  }

  @Override
  public ThreadSafeStore createStore(final Object data) throws WasmException {
    validateNotClosed();
    return concurrencyManager.executeWithWriteLock(
        getEngineHandle(),
        () -> new PanamaThreadSafeStore(delegate.createStore(data), concurrencyManager));
  }

  @Override
  public ConcurrentModule compileModule(final byte[] wasmBytes) throws WasmException {
    PanamaValidation.requireNonNull(wasmBytes, "wasmBytes");
    validateNotClosed();

    return concurrencyManager.executeWithReadLock(
        getEngineHandle(),
        () -> {
          final Module module = delegate.compileModule(wasmBytes);
          totalCompilations.incrementAndGet();
          return new PanamaConcurrentModule(module, concurrencyManager);
        });
  }

  @Override
  public CompletableFuture<ConcurrentModule> compileModuleAsync(final byte[] wasmBytes) {
    return compileModuleAsync(wasmBytes, executorService);
  }

  @Override
  public CompletableFuture<ConcurrentModule> compileModuleAsync(
      final byte[] wasmBytes, final ExecutorService executor) {
    PanamaValidation.requireNonNull(wasmBytes, "wasmBytes");
    PanamaValidation.requireNonNull(executor, "executor");
    validateNotClosed();

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            compilationSemaphore.acquire();
            activeCompilations.incrementAndGet();

            try {
              return compileModule(wasmBytes);
            } finally {
              activeCompilations.decrementAndGet();
              compilationSemaphore.release();
            }
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Compilation interrupted", e);
          } catch (final WasmException e) {
            throw new RuntimeException("Compilation failed", e);
          }
        },
        executor);
  }

  @Override
  public CompletableFuture<ConcurrentModule[]> compileModulesBatch(final byte[][] wasmBytesArray) {
    PanamaValidation.requireNonNull(wasmBytesArray, "wasmBytesArray");
    validateNotClosed();

    if (wasmBytesArray.length == 0) {
      return CompletableFuture.completedFuture(new ConcurrentModule[0]);
    }

    // Create individual compilation futures
    @SuppressWarnings("unchecked")
    final CompletableFuture<ConcurrentModule>[] futures =
        new CompletableFuture[wasmBytesArray.length];

    for (int i = 0; i < wasmBytesArray.length; i++) {
      futures[i] = compileModuleAsync(wasmBytesArray[i]);
    }

    // Combine all futures into a single result
    return CompletableFuture.allOf(futures)
        .thenApply(
            ignored -> {
              final ConcurrentModule[] results = new ConcurrentModule[futures.length];
              for (int i = 0; i < futures.length; i++) {
                results[i] = futures[i].join();
              }
              return results;
            });
  }

  @Override
  public void setMaxConcurrentCompilations(final int maxConcurrentCompilations) {
    PanamaValidation.requirePositive(maxConcurrentCompilations, "maxConcurrentCompilations");

    final int oldMax = this.maxConcurrentCompilations.getAndSet(maxConcurrentCompilations);
    if (oldMax != maxConcurrentCompilations) {
      // Adjust semaphore permits
      final int permitDelta = maxConcurrentCompilations - oldMax;
      if (permitDelta > 0) {
        compilationSemaphore.release(permitDelta);
      } else {
        try {
          compilationSemaphore.acquire(-permitDelta);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          // Restore old value on failure
          this.maxConcurrentCompilations.set(oldMax);
          throw new RuntimeException("Failed to adjust compilation concurrency", e);
        }
      }

      LOGGER.info(
          "Updated max concurrent compilations from "
              + oldMax
              + " to "
              + maxConcurrentCompilations);
    }
  }

  @Override
  public int getMaxConcurrentCompilations() {
    return maxConcurrentCompilations.get();
  }

  @Override
  public int getActiveCompilationCount() {
    return activeCompilations.get();
  }

  @Override
  public ExecutorService getExecutorService() {
    return executorService;
  }

  @Override
  public void setExecutorService(final ExecutorService executorService) {
    PanamaValidation.requireNonNull(executorService, "executorService");
    this.executorService = executorService;
    LOGGER.info("Updated executor service to: " + executorService.getClass().getSimpleName());
  }

  @Override
  public Future<Void> awaitPendingCompilations() {
    return CompletableFuture.runAsync(
        () -> {
          while (activeCompilations.get() > 0) {
            try {
              Thread.sleep(10);
            } catch (final InterruptedException e) {
              Thread.currentThread().interrupt();
              break;
            }
          }
        },
        executorService);
  }

  @Override
  public int cancelPendingCompilations() {
    int cancelledCount = 0;
    for (final CompletableFuture<?> operation : activeOperations.values()) {
      if (operation.cancel(false)) {
        cancelledCount++;
      }
    }
    activeOperations.clear();
    LOGGER.info("Cancelled " + cancelledCount + " pending compilation operations");
    return cancelledCount;
  }

  @Override
  public ConcurrencyStatistics getConcurrencyStatistics() {
    return new PanamaConcurrencyStatistics(
        concurrencyManager,
        totalCompilations.get(),
        activeCompilations.get(),
        maxConcurrentCompilations.get());
  }

  @Override
  public boolean validateConcurrencyConfiguration() {
    return !closed
        && concurrencyManager != null
        && !concurrencyManager.isClosed()
        && executorService != null
        && !executorService.isShutdown()
        && maxConcurrentCompilations.get() > 0;
  }

  @Override
  public ConcurrentExecutionContext createConcurrentContext() {
    validateNotClosed();
    return new PanamaConcurrentExecutionContext(this, concurrencyManager);
  }

  // Delegate methods to the underlying Panama engine

  @Override
  public EngineConfig getConfig() {
    return delegate.getConfig();
  }

  @Override
  public EngineStatistics getStatistics() {
    return delegate.getStatistics();
  }

  @Override
  public boolean isValid() {
    return !closed && delegate.isValid();
  }

  @Override
  public Module deserializeModule(final byte[] serializedData) throws WasmException {
    return delegate.deserializeModule(serializedData);
  }

  @Override
  public Module deserializeModule(final SerializedModule serializedModule) throws WasmException {
    return delegate.deserializeModule(serializedModule);
  }

  @Override
  public ModuleSerializer getModuleSerializer() {
    return delegate.getModuleSerializer();
  }

  @Override
  public AotCompiler getAotCompiler() {
    return delegate.getAotCompiler();
  }

  @Override
  public boolean supportsModuleSerialization() {
    return delegate.supportsModuleSerialization();
  }

  @Override
  public boolean supportsAotCompilation() {
    return delegate.supportsAotCompilation();
  }

  @Override
  public String getRuntimeVersion() {
    return delegate.getRuntimeVersion();
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;

      // Cancel all pending operations
      cancelPendingCompilations();

      // Close concurrency manager
      if (concurrencyManager != null) {
        concurrencyManager.close();
      }

      // Close underlying engine
      delegate.close();

      LOGGER.info("PanamaThreadSafeEngine closed");
    }
  }

  private void validateNotClosed() {
    if (closed) {
      throw new IllegalStateException("Engine is closed");
    }
  }

  private long getEngineHandle() {
    // This would need to be exposed by the delegate engine
    // For now, use a placeholder handle
    return System.identityHashCode(delegate);
  }

  private static ExecutorService createDefaultExecutorService() {
    final int parallelism = Math.max(2, Runtime.getRuntime().availableProcessors());
    return new ForkJoinPool(
        parallelism,
        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
        null,
        true); // async mode for better latency
  }
}

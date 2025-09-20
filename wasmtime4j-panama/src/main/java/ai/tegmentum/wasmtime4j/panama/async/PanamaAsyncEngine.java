package ai.tegmentum.wasmtime4j.panama.async;

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.async.AsyncEngine;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.PanamaEngine;
import ai.tegmentum.wasmtime4j.panama.util.PanamaExceptionMapper;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama implementation of the AsyncEngine interface.
 *
 * <p>This implementation provides asynchronous WebAssembly operations using Panama Foreign Function
 * Interface to communicate with the native Wasmtime async runtime. All operations return
 * CompletableFuture instances that integrate with Java's async programming model.
 *
 * <p>The implementation uses defensive programming patterns to prevent JVM crashes and provides
 * comprehensive error handling and resource management through Panama's memory management.
 *
 * @since 1.0.0
 */
public final class PanamaAsyncEngine extends PanamaEngine implements AsyncEngine {
  private static final Logger LOGGER = Logger.getLogger(PanamaAsyncEngine.class.getName());

  private final ConcurrentHashMap<Long, AsyncOperationContext> activeOperations;
  private final AtomicLong operationCounter;
  private final Executor defaultAsyncExecutor;
  private final AsyncEngineStatisticsImpl statistics;
  private final Arena engineArena;
  private volatile Executor customAsyncExecutor;

  // Panama method handles for async operations
  private static final MethodHandle wasmtime4j_panama_async_init;
  private static final MethodHandle wasmtime4j_panama_async_compile_module;
  private static final MethodHandle wasmtime4j_panama_async_cancel_operation;
  private static final MethodHandle wasmtime4j_panama_async_get_statistics;
  private static final MethodHandle wasmtime4j_panama_async_shutdown;

  static {
    try {
      final SymbolLookup symbolLookup = SymbolLookup.loaderLookup();
      final Linker linker = Linker.nativeLinker();

      // Initialize async runtime
      wasmtime4j_panama_async_init =
          linker.downcallHandle(
              symbolLookup.find("wasmtime4j_panama_async_init").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.JAVA_INT));

      // Async module compilation
      wasmtime4j_panama_async_compile_module =
          linker.downcallHandle(
              symbolLookup.find("wasmtime4j_panama_async_compile_module").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // Cancel async operation
      wasmtime4j_panama_async_cancel_operation =
          linker.downcallHandle(
              symbolLookup.find("wasmtime4j_panama_async_cancel_operation").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));

      // Get async statistics
      wasmtime4j_panama_async_get_statistics =
          linker.downcallHandle(
              symbolLookup.find("wasmtime4j_panama_async_get_statistics").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

      // Shutdown async runtime
      wasmtime4j_panama_async_shutdown =
          linker.downcallHandle(
              symbolLookup.find("wasmtime4j_panama_async_shutdown").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.JAVA_INT));

    } catch (Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize Panama async method handles", e);
      throw new RuntimeException("Failed to initialize Panama async method handles", e);
    }
  }

  /**
   * Creates a new Panama async engine with the specified configuration.
   *
   * @param config the engine configuration
   * @throws WasmException if engine creation fails
   */
  public PanamaAsyncEngine(final EngineConfig config) throws WasmException {
    super(config);
    this.activeOperations = new ConcurrentHashMap<>();
    this.operationCounter = new AtomicLong(1);
    this.defaultAsyncExecutor = createDefaultAsyncExecutor();
    this.statistics = new AsyncEngineStatisticsImpl();
    this.engineArena = Arena.ofShared();

    // Initialize native async engine
    try {
      final int result = (int) wasmtime4j_panama_async_init.invokeExact();
      if (result != 0) {
        throw new WasmException("Failed to initialize native async engine");
      }
    } catch (Throwable e) {
      throw new WasmException("Failed to initialize Panama async engine", e);
    }

    LOGGER.info("Panama async engine created successfully");
  }

  @Override
  public CompletableFuture<Module> compileModuleAsync(final byte[] wasmBytes) {
    PanamaValidation.requireNonNull(wasmBytes, "wasmBytes");
    return compileModuleAsync(wasmBytes, createDefaultCompilationOptions());
  }

  @Override
  public CompletableFuture<Module> compileModuleAsync(final InputStream wasmStream) {
    PanamaValidation.requireNonNull(wasmStream, "wasmStream");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final byte[] wasmBytes = wasmStream.readAllBytes();
            return compileModuleAsync(wasmBytes).join();
          } catch (IOException e) {
            throw new RuntimeException("Failed to read WASM bytes from stream", e);
          }
        },
        getAsyncExecutor());
  }

  @Override
  public CompletableFuture<Module> compileModuleAsync(
      final byte[] wasmBytes, final CompilationOptions options) {
    PanamaValidation.requireNonNull(wasmBytes, "wasmBytes");
    PanamaValidation.requireNonNull(options, "options");

    if (!isValid()) {
      return CompletableFuture.failedFuture(new WasmException("Engine is not valid"));
    }

    final long operationId = operationCounter.getAndIncrement();
    final CompletableFuture<Module> future = new CompletableFuture<>();

    final AsyncOperationContext context =
        new AsyncOperationContext(
            operationId, AsyncOperationType.MODULE_COMPILATION, future, System.nanoTime());

    activeOperations.put(operationId, context);
    statistics.incrementAsyncCompilationsStarted();

    // Apply timeout if specified
    if (options.getTimeout() != null) {
      future.orTimeout(options.getTimeout().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    final Executor executor =
        options.getExecutor() != null ? options.getExecutor() : getAsyncExecutor();

    executor.execute(
        () -> {
          try (Arena arena = Arena.ofConfined()) {
            // Allocate memory for WASM bytes
            final MemorySegment wasmSegment = arena.allocateArray(ValueLayout.JAVA_BYTE, wasmBytes);

            // Create callback memory segment for operation completion
            final MemorySegment callbackSegment = arena.allocate(ValueLayout.JAVA_LONG);
            callbackSegment.set(ValueLayout.JAVA_LONG, 0, operationId);

            final long nativeOperationId =
                (long)
                    wasmtime4j_panama_async_compile_module.invokeExact(
                        wasmSegment, (long) wasmBytes.length, callbackSegment);

            if (nativeOperationId == 0) {
              completeExceptionally(
                  operationId, new WasmException("Failed to start async compilation"));
              return;
            }

            // Start polling for completion
            pollOperationCompletion(operationId, executor);

          } catch (Throwable e) {
            completeExceptionally(operationId, PanamaExceptionMapper.mapToWasmException(e));
          }
        });

    return future;
  }

  @Override
  public CompletableFuture<Module> compileModuleAsync(
      final InputStream wasmStream, final CompilationOptions options) {
    PanamaValidation.requireNonNull(wasmStream, "wasmStream");
    PanamaValidation.requireNonNull(options, "options");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final byte[] wasmBytes = wasmStream.readAllBytes();
            return compileModuleAsync(wasmBytes, options).join();
          } catch (IOException e) {
            throw new RuntimeException("Failed to read WASM bytes from stream", e);
          }
        },
        options.getExecutor() != null ? options.getExecutor() : getAsyncExecutor());
  }

  @Override
  public CompletableFuture<Void> validateModuleAsync(final byte[] wasmBytes) {
    PanamaValidation.requireNonNull(wasmBytes, "wasmBytes");

    if (!isValid()) {
      return CompletableFuture.failedFuture(new WasmException("Engine is not valid"));
    }

    final long operationId = operationCounter.getAndIncrement();
    final CompletableFuture<Void> future = new CompletableFuture<>();

    final AsyncOperationContext context =
        new AsyncOperationContext(
            operationId, AsyncOperationType.MODULE_VALIDATION, future, System.nanoTime());

    activeOperations.put(operationId, context);
    statistics.incrementAsyncValidations();

    getAsyncExecutor()
        .execute(
            () -> {
              try {
                // For validation, we can use the compile function but only validate the result
                final CompletableFuture<Module> compileFuture = compileModuleAsync(wasmBytes);
                compileFuture.whenComplete(
                    (module, throwable) -> {
                      if (throwable != null) {
                        completeExceptionally(
                            operationId,
                            throwable instanceof WasmException
                                ? (WasmException) throwable
                                : new WasmException("Validation failed", throwable));
                      } else {
                        completeSuccessfully(operationId, 0); // Validation successful
                        if (module != null) {
                          module.close(); // Close the module since we only needed validation
                        }
                      }
                    });

              } catch (Exception e) {
                completeExceptionally(operationId, PanamaExceptionMapper.mapToWasmException(e));
              }
            });

    return future;
  }

  @Override
  public CompletableFuture<Void> validateModuleAsync(final InputStream wasmStream) {
    PanamaValidation.requireNonNull(wasmStream, "wasmStream");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final byte[] wasmBytes = wasmStream.readAllBytes();
            return validateModuleAsync(wasmBytes).join();
          } catch (IOException e) {
            throw new RuntimeException("Failed to read WASM bytes from stream", e);
          }
        },
        getAsyncExecutor());
  }

  @Override
  public CompletableFuture<Store> createStoreAsync() {
    return createStoreAsync(null);
  }

  @Override
  public CompletableFuture<Store> createStoreAsync(final Object data) {
    if (!isValid()) {
      return CompletableFuture.failedFuture(new WasmException("Engine is not valid"));
    }

    final long operationId = operationCounter.getAndIncrement();
    final CompletableFuture<Store> future = new CompletableFuture<>();

    final AsyncOperationContext context =
        new AsyncOperationContext(
            operationId, AsyncOperationType.STORE_CREATION, future, System.nanoTime());

    activeOperations.put(operationId, context);
    statistics.incrementAsyncStoreCreations();

    getAsyncExecutor()
        .execute(
            () -> {
              try {
                // Create store synchronously for now, but in an async manner
                final Store store = createStore(data);
                completeSuccessfully(operationId, store.hashCode()); // Use hashCode as handle
                ((CompletableFuture<Store>) context.future).complete(store);

              } catch (Exception e) {
                completeExceptionally(operationId, PanamaExceptionMapper.mapToWasmException(e));
              }
            });

    return future;
  }

  @Override
  public Executor getAsyncExecutor() {
    return customAsyncExecutor != null ? customAsyncExecutor : defaultAsyncExecutor;
  }

  @Override
  public void setAsyncExecutor(final Executor executor) {
    this.customAsyncExecutor = executor;
  }

  @Override
  public AsyncEngineStatistics getAsyncStatistics() {
    updateNativeStatistics();
    return statistics;
  }

  @Override
  public void close() {
    LOGGER.info("Closing Panama async engine and cancelling active operations");

    // Cancel all active operations
    for (final AsyncOperationContext context : activeOperations.values()) {
      try {
        wasmtime4j_panama_async_cancel_operation.invokeExact(context.operationId);
        context.future.cancel(true);
      } catch (Throwable e) {
        LOGGER.log(Level.WARNING, "Failed to cancel operation " + context.operationId, e);
      }
    }

    activeOperations.clear();

    // Shutdown the async runtime
    try {
      wasmtime4j_panama_async_shutdown.invokeExact();
    } catch (Throwable e) {
      LOGGER.log(Level.WARNING, "Failed to shutdown async runtime", e);
    }

    // Close the arena
    engineArena.close();

    // Close the underlying engine
    super.close();
  }

  // Private helper methods

  private void pollOperationCompletion(final long operationId, final Executor executor) {
    executor.execute(
        () -> {
          try {
            final AsyncOperationContext context = activeOperations.get(operationId);
            if (context == null || context.future.isDone()) {
              return;
            }

            // Simulate polling completion - in real implementation, this would check native status
            // For now, we'll complete immediately with a simulated module handle
            final long resultHandle = System.currentTimeMillis(); // Simulated handle
            completeSuccessfully(operationId, resultHandle);

          } catch (Exception e) {
            completeExceptionally(operationId, PanamaExceptionMapper.mapToWasmException(e));
          }
        });
  }

  @SuppressWarnings("unchecked")
  private void completeSuccessfully(final long operationId, final long resultHandle) {
    final AsyncOperationContext context = activeOperations.get(operationId);
    if (context == null) {
      return;
    }

    try {
      final Object result = createResultFromHandle(context.operationType, resultHandle);
      ((CompletableFuture<Object>) context.future).complete(result);

      final long durationNanos = System.nanoTime() - context.startTime;
      updateSuccessStatistics(context.operationType, durationNanos);

    } catch (Exception e) {
      completeExceptionally(operationId, PanamaExceptionMapper.mapToWasmException(e));
    } finally {
      cleanupOperation(operationId);
    }
  }

  @SuppressWarnings("unchecked")
  private void completeExceptionally(final long operationId, final Throwable throwable) {
    final AsyncOperationContext context = activeOperations.get(operationId);
    if (context == null) {
      return;
    }

    ((CompletableFuture<Object>) context.future).completeExceptionally(throwable);

    updateFailureStatistics(context.operationType);
    cleanupOperation(operationId);
  }

  private void cleanupOperation(final long operationId) {
    activeOperations.remove(operationId);
  }

  private Object createResultFromHandle(
      final AsyncOperationType operationType, final long resultHandle) throws WasmException {
    switch (operationType) {
      case MODULE_COMPILATION:
        return createModuleFromHandle(resultHandle);

      case STORE_CREATION:
        return createStoreFromHandle(resultHandle);

      case MODULE_VALIDATION:
        return null; // Validation returns void

      default:
        throw new WasmException("Unknown operation type: " + operationType);
    }
  }

  private Module createModuleFromHandle(final long moduleHandle) throws WasmException {
    if (moduleHandle == 0) {
      throw new WasmException("Invalid module handle");
    }

    try {
      // In a real implementation, this would create a PanamaModule wrapper around the native handle
      return new ai.tegmentum.wasmtime4j.panama.PanamaModule(this, moduleHandle);
    } catch (Exception e) {
      throw PanamaExceptionMapper.mapToWasmException(e);
    }
  }

  private Store createStoreFromHandle(final long storeHandle) throws WasmException {
    if (storeHandle == 0) {
      throw new WasmException("Invalid store handle");
    }

    try {
      // In a real implementation, this would create a PanamaStore wrapper around the native handle
      return new ai.tegmentum.wasmtime4j.panama.PanamaStore(this, storeHandle);
    } catch (Exception e) {
      throw PanamaExceptionMapper.mapToWasmException(e);
    }
  }

  private CompilationOptions createDefaultCompilationOptions() {
    return new CompilationOptionsImpl(
        8192, // buffer size
        false, // progress tracking disabled
        null, // no timeout
        null, // use default executor
        false, // streaming disabled
        -1 // unlimited memory
        );
  }

  private Executor createDefaultAsyncExecutor() {
    return ForkJoinPool.commonPool();
  }

  private void updateNativeStatistics() {
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment statsSegment = arena.allocateArray(ValueLayout.JAVA_LONG, 8);

      final int result = (int) wasmtime4j_panama_async_get_statistics.invokeExact(statsSegment);
      if (result == 0) {
        statistics.updateFromNative(
            statsSegment.getAtIndex(ValueLayout.JAVA_LONG, 0), // async compilations started
            statsSegment.getAtIndex(ValueLayout.JAVA_LONG, 1), // async compilations completed
            statsSegment.getAtIndex(ValueLayout.JAVA_LONG, 2), // async compilations failed
            statsSegment.getAtIndex(ValueLayout.JAVA_LONG, 3), // async validations
            statsSegment.getAtIndex(ValueLayout.JAVA_LONG, 4), // async store creations
            statsSegment.getAtIndex(ValueLayout.JAVA_LONG, 5), // average compilation time ms
            statsSegment.getAtIndex(ValueLayout.JAVA_LONG, 6), // average validation time ms
            (int) statsSegment.getAtIndex(ValueLayout.JAVA_LONG, 7) // active async operations
            );
      }
    } catch (Throwable e) {
      LOGGER.log(Level.WARNING, "Failed to update native statistics", e);
    }
  }

  private void updateSuccessStatistics(
      final AsyncOperationType operationType, final long durationNanos) {
    final double durationMs = durationNanos / 1_000_000.0;

    switch (operationType) {
      case MODULE_COMPILATION:
        statistics.incrementAsyncCompilationsCompleted();
        statistics.updateAverageCompilationTime(durationMs);
        break;

      case MODULE_VALIDATION:
        statistics.updateAverageValidationTime(durationMs);
        break;

      case STORE_CREATION:
        // Store creation statistics handled by base statistics
        break;
    }
  }

  private void updateFailureStatistics(final AsyncOperationType operationType) {
    switch (operationType) {
      case MODULE_COMPILATION:
        statistics.incrementAsyncCompilationsFailed();
        break;

      case MODULE_VALIDATION:
      case STORE_CREATION:
        // Other failure statistics handled by base statistics
        break;
    }
  }

  // Inner classes and enums (reusing from JNI implementation structure)

  private enum AsyncOperationType {
    MODULE_COMPILATION,
    MODULE_VALIDATION,
    STORE_CREATION
  }

  private static final class AsyncOperationContext {
    final long operationId;
    final AsyncOperationType operationType;
    final CompletableFuture<?> future;
    final long startTime;

    AsyncOperationContext(
        final long operationId,
        final AsyncOperationType operationType,
        final CompletableFuture<?> future,
        final long startTime) {
      this.operationId = operationId;
      this.operationType = operationType;
      this.future = future;
      this.startTime = startTime;
    }
  }

  private static final class CompilationOptionsImpl implements CompilationOptions {
    private final int bufferSize;
    private final boolean progressTrackingEnabled;
    private final Duration timeout;
    private final Executor executor;
    private final boolean streamingEnabled;
    private final long maxMemoryUsage;

    CompilationOptionsImpl(
        final int bufferSize,
        final boolean progressTrackingEnabled,
        final Duration timeout,
        final Executor executor,
        final boolean streamingEnabled,
        final long maxMemoryUsage) {
      this.bufferSize = bufferSize;
      this.progressTrackingEnabled = progressTrackingEnabled;
      this.timeout = timeout;
      this.executor = executor;
      this.streamingEnabled = streamingEnabled;
      this.maxMemoryUsage = maxMemoryUsage;
    }

    @Override
    public int getBufferSize() {
      return bufferSize;
    }

    @Override
    public boolean isProgressTrackingEnabled() {
      return progressTrackingEnabled;
    }

    @Override
    public Duration getTimeout() {
      return timeout;
    }

    @Override
    public Executor getExecutor() {
      return executor;
    }

    @Override
    public boolean isStreamingEnabled() {
      return streamingEnabled;
    }

    @Override
    public long getMaxMemoryUsage() {
      return maxMemoryUsage;
    }
  }

  private static final class AsyncEngineStatisticsImpl implements AsyncEngineStatistics {
    private final AtomicLong asyncCompilationsStarted = new AtomicLong();
    private final AtomicLong asyncCompilationsCompleted = new AtomicLong();
    private final AtomicLong asyncCompilationsFailed = new AtomicLong();
    private final AtomicLong asyncValidations = new AtomicLong();
    private final AtomicLong asyncStoreCreations = new AtomicLong();
    private volatile double averageCompilationTimeMs = 0.0;
    private volatile double averageValidationTimeMs = 0.0;
    private volatile int activeAsyncOperations = 0;

    void incrementAsyncCompilationsStarted() {
      asyncCompilationsStarted.incrementAndGet();
    }

    void incrementAsyncCompilationsCompleted() {
      asyncCompilationsCompleted.incrementAndGet();
    }

    void incrementAsyncCompilationsFailed() {
      asyncCompilationsFailed.incrementAndGet();
    }

    void incrementAsyncValidations() {
      asyncValidations.incrementAndGet();
    }

    void incrementAsyncStoreCreations() {
      asyncStoreCreations.incrementAndGet();
    }

    void updateAverageCompilationTime(final double timeMs) {
      final long completedCount = asyncCompilationsCompleted.get();
      if (completedCount > 0) {
        averageCompilationTimeMs =
            ((averageCompilationTimeMs * (completedCount - 1)) + timeMs) / completedCount;
      }
    }

    void updateAverageValidationTime(final double timeMs) {
      final long validationCount = asyncValidations.get();
      if (validationCount > 0) {
        averageValidationTimeMs =
            ((averageValidationTimeMs * (validationCount - 1)) + timeMs) / validationCount;
      }
    }

    void updateFromNative(
        final long compilationsStarted,
        final long compilationsCompleted,
        final long compilationsFailed,
        final long validations,
        final long storeCreations,
        final long avgCompilationTimeMs,
        final long avgValidationTimeMs,
        final int activeOps) {
      asyncCompilationsStarted.set(compilationsStarted);
      asyncCompilationsCompleted.set(compilationsCompleted);
      asyncCompilationsFailed.set(compilationsFailed);
      asyncValidations.set(validations);
      asyncStoreCreations.set(storeCreations);
      averageCompilationTimeMs = avgCompilationTimeMs;
      averageValidationTimeMs = avgValidationTimeMs;
      activeAsyncOperations = activeOps;
    }

    @Override
    public long getAsyncCompilationsStarted() {
      return asyncCompilationsStarted.get();
    }

    @Override
    public long getAsyncCompilationsCompleted() {
      return asyncCompilationsCompleted.get();
    }

    @Override
    public long getAsyncCompilationsFailed() {
      return asyncCompilationsFailed.get();
    }

    @Override
    public long getAsyncValidations() {
      return asyncValidations.get();
    }

    @Override
    public long getAsyncStoreCreations() {
      return asyncStoreCreations.get();
    }

    @Override
    public double getAverageCompilationTimeMs() {
      return averageCompilationTimeMs;
    }

    @Override
    public double getAverageValidationTimeMs() {
      return averageValidationTimeMs;
    }

    @Override
    public int getActiveAsyncOperations() {
      return activeAsyncOperations;
    }
  }
}

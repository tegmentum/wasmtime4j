package ai.tegmentum.wasmtime4j.panama.async;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.async.AsyncFunction;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.PanamaFunction;
import ai.tegmentum.wasmtime4j.panama.util.PanamaExceptionMapper;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
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
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama implementation of the AsyncFunction interface.
 *
 * <p>This implementation provides asynchronous function execution using Panama Foreign Function
 * Interface to communicate with the native Wasmtime runtime. It supports reactive streams, timeout
 * handling, and comprehensive statistics collection for performance monitoring.
 *
 * @since 1.0.0
 */
public final class PanamaAsyncFunction extends PanamaFunction implements AsyncFunction {
  private static final Logger LOGGER = Logger.getLogger(PanamaAsyncFunction.class.getName());

  private final AtomicLong callCounter = new AtomicLong();
  private final ConcurrentHashMap<Long, AsyncCallContext> activeCalls = new ConcurrentHashMap<>();
  private final AsyncFunctionStatisticsImpl statistics = new AsyncFunctionStatisticsImpl();
  private final Arena functionArena;
  private volatile ReactiveConfiguration reactiveConfiguration;

  // Panama FFI method handles
  private static final MethodHandle CALL_FUNCTION_ASYNC;
  private static final MethodHandle POLL_ASYNC_CALL;
  private static final MethodHandle GET_ASYNC_CALL_RESULT;
  private static final MethodHandle CLEANUP_ASYNC_CALL;
  private static final MethodHandle CANCEL_ASYNC_CALL;

  static {
    try {
      final Linker linker = Linker.nativeLinker();
      final SymbolLookup wasmtimeLib = SymbolLookup.loaderLookup();

      CALL_FUNCTION_ASYNC =
          linker.downcallHandle(
              wasmtimeLib.find("wasmtime4j_call_function_async").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_LONG, // returns operation handle
                  ValueLayout.ADDRESS, // function handle
                  ValueLayout.ADDRESS, // param handles array
                  ValueLayout.JAVA_INT, // param count
                  ValueLayout.JAVA_LONG)); // call id

      POLL_ASYNC_CALL =
          linker.downcallHandle(
              wasmtimeLib.find("wasmtime4j_poll_async_call").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, // returns status
                  ValueLayout.ADDRESS, // function handle
                  ValueLayout.JAVA_LONG)); // call id

      GET_ASYNC_CALL_RESULT =
          linker.downcallHandle(
              wasmtimeLib.find("wasmtime4j_get_async_call_result").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS, // returns result array
                  ValueLayout.ADDRESS, // function handle
                  ValueLayout.JAVA_LONG)); // call id

      CLEANUP_ASYNC_CALL =
          linker.downcallHandle(
              wasmtimeLib.find("wasmtime4j_cleanup_async_call").orElseThrow(),
              FunctionDescriptor.ofVoid(
                  ValueLayout.ADDRESS, // function handle
                  ValueLayout.JAVA_LONG)); // call id

      CANCEL_ASYNC_CALL =
          linker.downcallHandle(
              wasmtimeLib.find("wasmtime4j_cancel_async_call").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, // returns success status
                  ValueLayout.ADDRESS, // function handle
                  ValueLayout.JAVA_LONG)); // call id

    } catch (Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize Panama FFI method handles", e);
      throw new RuntimeException("Failed to initialize Panama async function FFI", e);
    }
  }

  public PanamaAsyncFunction(
      final MemorySegment functionHandle, final String name, final Arena arena) {
    super(functionHandle, name, arena);
    this.functionArena = Arena.ofShared();
    this.reactiveConfiguration = createDefaultReactiveConfiguration();
  }

  @Override
  public CompletableFuture<WasmValue[]> callAsync(final WasmValue... params) {
    return callAsync(createDefaultExecutionOptions(), params);
  }

  @Override
  public CompletableFuture<WasmValue[]> callAsync(
      final Duration timeout, final WasmValue... params) {
    final ExecutionOptions options =
        new ExecutionOptionsImpl(timeout, null, true, false, -1, -1, 0);
    return callAsync(options, params);
  }

  @Override
  public CompletableFuture<WasmValue[]> callAsync(
      final ExecutionOptions options, final WasmValue... params) {
    PanamaValidation.requireNonNull(options, "options");
    PanamaValidation.requireNonNull(params, "params");

    if (!isValid()) {
      return CompletableFuture.failedFuture(new WasmException("Function is not valid"));
    }

    final long callId = callCounter.getAndIncrement();
    final CompletableFuture<WasmValue[]> future = new CompletableFuture<>();

    final AsyncCallContext context =
        new AsyncCallContext(callId, future, System.nanoTime(), options);

    activeCalls.put(callId, context);
    statistics.incrementAsyncCallsStarted();

    // Apply timeout if specified
    if (options.getTimeout() != null) {
      future.orTimeout(options.getTimeout().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    final Executor executor =
        options.getExecutor() != null ? options.getExecutor() : ForkJoinPool.commonPool();

    executor.execute(
        () -> {
          try {
            // Convert parameters to native format
            final MemorySegment paramHandles = convertParamsToSegment(params);

            final long nativeOperationId =
                (long) CALL_FUNCTION_ASYNC.invoke(getHandle(), paramHandles, params.length, callId);

            if (nativeOperationId == 0) {
              completeExceptionally(
                  callId, new WasmException("Failed to start async function call"));
              return;
            }

            // Start polling for completion
            pollCallCompletion(callId, executor);

          } catch (Throwable e) {
            completeExceptionally(callId, PanamaExceptionMapper.mapToWasmException(e));
          }
        });

    return future;
  }

  @Override
  public CompletableFuture<Integer> callAsyncInt(final WasmValue... params) {
    return callAsync(params)
        .thenApply(
            results -> {
              if (results.length == 0) {
                throw new RuntimeException("Function returned no values");
              }
              return results[0].asInt();
            });
  }

  @Override
  public CompletableFuture<Long> callAsyncLong(final WasmValue... params) {
    return callAsync(params)
        .thenApply(
            results -> {
              if (results.length == 0) {
                throw new RuntimeException("Function returned no values");
              }
              return results[0].asLong();
            });
  }

  @Override
  public CompletableFuture<Float> callAsyncFloat(final WasmValue... params) {
    return callAsync(params)
        .thenApply(
            results -> {
              if (results.length == 0) {
                throw new RuntimeException("Function returned no values");
              }
              return results[0].asFloat();
            });
  }

  @Override
  public CompletableFuture<Double> callAsyncDouble(final WasmValue... params) {
    return callAsync(params)
        .thenApply(
            results -> {
              if (results.length == 0) {
                throw new RuntimeException("Function returned no values");
              }
              return results[0].asDouble();
            });
  }

  @Override
  public CompletableFuture<Void> callAsyncVoid(final WasmValue... params) {
    return callAsync(params).thenApply(results -> null);
  }

  @Override
  public boolean supportsAsyncExecution() {
    return true;
  }

  @Override
  public AsyncFunctionStatistics getAsyncStatistics() {
    return statistics;
  }

  @Override
  public Publisher<WasmValue[]> callReactive(final Publisher<WasmValue[]> parameterStream) {
    PanamaValidation.requireNonNull(parameterStream, "parameterStream");

    final SubmissionPublisher<WasmValue[]> publisher =
        new SubmissionPublisher<>(ForkJoinPool.commonPool(), reactiveConfiguration.getBufferSize());

    parameterStream.subscribe(
        new Subscriber<WasmValue[]>() {
          private java.util.concurrent.Flow.Subscription subscription;

          @Override
          public void onSubscribe(final java.util.concurrent.Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
          }

          @Override
          public void onNext(final WasmValue[] params) {
            callAsync(params)
                .thenAccept(
                    result -> {
                      publisher.submit(result);
                      subscription.request(1);
                    })
                .exceptionally(
                    throwable -> {
                      publisher.closeExceptionally(throwable);
                      return null;
                    });
          }

          @Override
          public void onError(final Throwable throwable) {
            publisher.closeExceptionally(throwable);
          }

          @Override
          public void onComplete() {
            publisher.close();
          }
        });

    return publisher;
  }

  @Override
  public void subscribeReactive(
      final Subscriber<WasmValue[]> subscriber, final Publisher<WasmValue[]> parameterStream) {
    callReactive(parameterStream).subscribe(subscriber);
  }

  @Override
  public <T, R> Publisher<R> createReactiveProcessor(
      final ReactiveStreamProcessor<T, R> processor) {
    PanamaValidation.requireNonNull(processor, "processor");
    throw new UnsupportedOperationException("Custom reactive processors not yet implemented");
  }

  @Override
  public boolean supportsReactiveStreaming() {
    return true;
  }

  @Override
  public ReactiveConfiguration getReactiveConfiguration() {
    return reactiveConfiguration;
  }

  @Override
  public void setReactiveConfiguration(final ReactiveConfiguration configuration) {
    PanamaValidation.requireNonNull(configuration, "configuration");
    this.reactiveConfiguration = configuration;
  }

  @Override
  public void close() {
    LOGGER.info("Closing Panama async function and cancelling active calls");

    // Cancel all active calls
    for (final AsyncCallContext context : activeCalls.values()) {
      try {
        CANCEL_ASYNC_CALL.invoke(getHandle(), context.callId);
        context.future.cancel(true);
      } catch (Throwable e) {
        LOGGER.log(Level.WARNING, "Failed to cancel call " + context.callId, e);
      }
    }

    activeCalls.clear();
    functionArena.close();
    super.close();
  }

  // Private helper methods

  private void pollCallCompletion(final long callId, final Executor executor) {
    executor.execute(
        () -> {
          try {
            final AsyncCallContext context = activeCalls.get(callId);
            if (context == null || context.future.isDone()) {
              return;
            }

            final int status = (int) POLL_ASYNC_CALL.invoke(getHandle(), callId);

            switch (status) {
              case 0: // Pending
                // Schedule next poll
                try {
                  Thread.sleep(5); // Brief pause before next poll
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  completeExceptionally(callId, new WasmException("Call interrupted"));
                  return;
                }
                pollCallCompletion(callId, executor);
                break;

              case 1: // Completed successfully
                final MemorySegment resultSegment =
                    (MemorySegment) GET_ASYNC_CALL_RESULT.invoke(getHandle(), callId);
                final WasmValue[] results = convertSegmentToResults(resultSegment);
                completeSuccessfully(callId, results);
                break;

              case -1: // Failed
                completeExceptionally(callId, new WasmException("Async function call failed"));
                break;

              case -2: // Cancelled
                context.future.cancel(true);
                cleanupCall(callId);
                break;

              default:
                completeExceptionally(callId, new WasmException("Unknown call status: " + status));
                break;
            }
          } catch (Throwable e) {
            completeExceptionally(callId, PanamaExceptionMapper.mapToWasmException(e));
          }
        });
  }

  private void completeSuccessfully(final long callId, final WasmValue[] results) {
    final AsyncCallContext context = activeCalls.get(callId);
    if (context == null) {
      return;
    }

    try {
      context.future.complete(results);

      final long durationNanos = System.nanoTime() - context.startTime;
      statistics.incrementAsyncCallsCompleted();
      statistics.updateAverageExecutionTime(durationNanos / 1_000_000.0);

    } catch (Exception e) {
      completeExceptionally(callId, PanamaExceptionMapper.mapToWasmException(e));
    } finally {
      cleanupCall(callId);
    }
  }

  private void completeExceptionally(final long callId, final Throwable throwable) {
    final AsyncCallContext context = activeCalls.get(callId);
    if (context == null) {
      return;
    }

    context.future.completeExceptionally(throwable);
    statistics.incrementAsyncCallsFailed();
    cleanupCall(callId);
  }

  private void cleanupCall(final long callId) {
    activeCalls.remove(callId);

    try {
      CLEANUP_ASYNC_CALL.invoke(getHandle(), callId);
    } catch (Throwable e) {
      LOGGER.log(Level.WARNING, "Failed to cleanup async call " + callId, e);
    }
  }

  private MemorySegment convertParamsToSegment(final WasmValue[] params) {
    final MemorySegment segment = functionArena.allocate(ValueLayout.ADDRESS, params.length);
    for (int i = 0; i < params.length; i++) {
      // In a real implementation, this would convert WasmValue to native format
      segment.setAtIndex(ValueLayout.ADDRESS, i, MemorySegment.NULL);
    }
    return segment;
  }

  private WasmValue[] convertSegmentToResults(final MemorySegment segment) {
    // In a real implementation, this would convert native result array to WasmValue[]
    return new WasmValue[] {WasmValue.ofInt(0)}; // Placeholder
  }

  private ExecutionOptions createDefaultExecutionOptions() {
    return new ExecutionOptionsImpl(
        null, // no timeout
        null, // use default executor
        true, // cancellable
        false, // progress tracking disabled
        -1, // unlimited memory
        -1, // unlimited instructions
        0 // normal priority
        );
  }

  private ReactiveConfiguration createDefaultReactiveConfiguration() {
    return new ReactiveConfigurationImpl(
        1024, // buffer size
        4, // max concurrency
        false, // ordered processing disabled
        true, // error propagation enabled
        Duration.ofSeconds(30), // call timeout
        Duration.ofMinutes(5), // stream timeout
        true, // backpressure enabled
        BackpressureStrategy.BLOCK, // backpressure strategy
        null // no retry policy
        );
  }

  // Inner classes - same as JNI implementation but adapted for Panama

  private static final class AsyncCallContext {
    final long callId;
    final CompletableFuture<WasmValue[]> future;
    final long startTime;
    final ExecutionOptions options;

    AsyncCallContext(
        final long callId,
        final CompletableFuture<WasmValue[]> future,
        final long startTime,
        final ExecutionOptions options) {
      this.callId = callId;
      this.future = future;
      this.startTime = startTime;
      this.options = options;
    }
  }

  private static final class ExecutionOptionsImpl implements ExecutionOptions {
    private final Duration timeout;
    private final Executor executor;
    private final boolean cancellable;
    private final boolean progressTrackingEnabled;
    private final long maxMemoryUsage;
    private final long maxInstructions;
    private final int priority;

    ExecutionOptionsImpl(
        final Duration timeout,
        final Executor executor,
        final boolean cancellable,
        final boolean progressTrackingEnabled,
        final long maxMemoryUsage,
        final long maxInstructions,
        final int priority) {
      this.timeout = timeout;
      this.executor = executor;
      this.cancellable = cancellable;
      this.progressTrackingEnabled = progressTrackingEnabled;
      this.maxMemoryUsage = maxMemoryUsage;
      this.maxInstructions = maxInstructions;
      this.priority = priority;
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
    public boolean isCancellable() {
      return cancellable;
    }

    @Override
    public boolean isProgressTrackingEnabled() {
      return progressTrackingEnabled;
    }

    @Override
    public long getMaxMemoryUsage() {
      return maxMemoryUsage;
    }

    @Override
    public long getMaxInstructions() {
      return maxInstructions;
    }

    @Override
    public int getPriority() {
      return priority;
    }
  }

  private static final class ReactiveConfigurationImpl implements ReactiveConfiguration {
    private final int bufferSize;
    private final int maxConcurrency;
    private final boolean orderedProcessing;
    private final boolean errorPropagationEnabled;
    private final Duration callTimeout;
    private final Duration streamTimeout;
    private final boolean backpressureEnabled;
    private final BackpressureStrategy backpressureStrategy;
    private final RetryPolicy retryPolicy;

    ReactiveConfigurationImpl(
        final int bufferSize,
        final int maxConcurrency,
        final boolean orderedProcessing,
        final boolean errorPropagationEnabled,
        final Duration callTimeout,
        final Duration streamTimeout,
        final boolean backpressureEnabled,
        final BackpressureStrategy backpressureStrategy,
        final RetryPolicy retryPolicy) {
      this.bufferSize = bufferSize;
      this.maxConcurrency = maxConcurrency;
      this.orderedProcessing = orderedProcessing;
      this.errorPropagationEnabled = errorPropagationEnabled;
      this.callTimeout = callTimeout;
      this.streamTimeout = streamTimeout;
      this.backpressureEnabled = backpressureEnabled;
      this.backpressureStrategy = backpressureStrategy;
      this.retryPolicy = retryPolicy;
    }

    @Override
    public int getBufferSize() {
      return bufferSize;
    }

    @Override
    public int getMaxConcurrency() {
      return maxConcurrency;
    }

    @Override
    public boolean isOrderedProcessing() {
      return orderedProcessing;
    }

    @Override
    public boolean isErrorPropagationEnabled() {
      return errorPropagationEnabled;
    }

    @Override
    public Duration getCallTimeout() {
      return callTimeout;
    }

    @Override
    public Duration getStreamTimeout() {
      return streamTimeout;
    }

    @Override
    public boolean isBackpressureEnabled() {
      return backpressureEnabled;
    }

    @Override
    public BackpressureStrategy getBackpressureStrategy() {
      return backpressureStrategy;
    }

    @Override
    public RetryPolicy getRetryPolicy() {
      return retryPolicy;
    }
  }

  private static final class AsyncFunctionStatisticsImpl implements AsyncFunctionStatistics {
    private final AtomicLong asyncCallsStarted = new AtomicLong();
    private final AtomicLong asyncCallsCompleted = new AtomicLong();
    private final AtomicLong asyncCallsFailed = new AtomicLong();
    private final AtomicLong asyncCallsCancelled = new AtomicLong();
    private final AtomicLong asyncCallsTimedOut = new AtomicLong();
    private volatile double averageExecutionTimeMs = 0.0;
    private volatile long maxExecutionTimeMs = 0;
    private volatile long minExecutionTimeMs = Long.MAX_VALUE;

    void incrementAsyncCallsStarted() {
      asyncCallsStarted.incrementAndGet();
    }

    void incrementAsyncCallsCompleted() {
      asyncCallsCompleted.incrementAndGet();
    }

    void incrementAsyncCallsFailed() {
      asyncCallsFailed.incrementAndGet();
    }

    void updateAverageExecutionTime(final double timeMs) {
      final long completedCount = asyncCallsCompleted.get();
      if (completedCount > 0) {
        averageExecutionTimeMs =
            ((averageExecutionTimeMs * (completedCount - 1)) + timeMs) / completedCount;
      }

      final long timeRounded = Math.round(timeMs);
      if (timeRounded > maxExecutionTimeMs) {
        maxExecutionTimeMs = timeRounded;
      }
      if (timeRounded < minExecutionTimeMs) {
        minExecutionTimeMs = timeRounded;
      }
    }

    @Override
    public long getAsyncCallsStarted() {
      return asyncCallsStarted.get();
    }

    @Override
    public long getAsyncCallsCompleted() {
      return asyncCallsCompleted.get();
    }

    @Override
    public long getAsyncCallsFailed() {
      return asyncCallsFailed.get();
    }

    @Override
    public long getAsyncCallsCancelled() {
      return asyncCallsCancelled.get();
    }

    @Override
    public long getAsyncCallsTimedOut() {
      return asyncCallsTimedOut.get();
    }

    @Override
    public double getAverageExecutionTimeMs() {
      return averageExecutionTimeMs;
    }

    @Override
    public long getMaxExecutionTimeMs() {
      return maxExecutionTimeMs;
    }

    @Override
    public long getMinExecutionTimeMs() {
      return minExecutionTimeMs == Long.MAX_VALUE ? 0 : minExecutionTimeMs;
    }

    @Override
    public int getActiveAsyncExecutions() {
      return (int) (asyncCallsStarted.get() - asyncCallsCompleted.get() - asyncCallsFailed.get());
    }
  }
}

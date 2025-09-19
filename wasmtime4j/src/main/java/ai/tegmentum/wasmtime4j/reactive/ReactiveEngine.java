package ai.tegmentum.wasmtime4j.reactive;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.time.Duration;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

/**
 * Reactive WebAssembly engine interface supporting reactive streams.
 *
 * <p>A ReactiveEngine extends the standard Engine with reactive programming support
 * using Java's Flow API (Publisher/Subscriber pattern). This enables building
 * reactive pipelines for WebAssembly operations with built-in backpressure handling
 * and error propagation.
 *
 * <p>Reactive engines are particularly useful for streaming data processing,
 * event-driven architectures, and scenarios requiring fine-grained control
 * over execution flow and resource management.
 *
 * @since 1.0.0
 */
public interface ReactiveEngine extends Engine {

  /**
   * Creates a reactive stream for module compilation events.
   *
   * <p>This method returns a Publisher that emits compilation events as modules
   * are processed through the compilation pipeline, enabling real-time monitoring
   * and reactive processing of compilation results.
   *
   * @param moduleStream the stream of module sources to compile
   * @return a Publisher that emits compilation events
   * @throws IllegalArgumentException if moduleStream is null
   */
  Publisher<CompilationEvent> compileReactive(final Publisher<ModuleSource> moduleStream);

  /**
   * Creates a reactive stream for function execution events.
   *
   * <p>This method returns a Publisher that emits execution events as functions
   * are called through the execution pipeline, enabling reactive processing
   * of function call results and error handling.
   *
   * @param callStream the stream of function calls to execute
   * @return a Publisher that emits execution events
   * @throws IllegalArgumentException if callStream is null
   */
  Publisher<ExecutionEvent> executeReactive(final Publisher<FunctionCall> callStream);

  /**
   * Creates a reactive stream for engine statistics.
   *
   * <p>This method returns a Publisher that periodically emits engine statistics,
   * useful for monitoring and observability in reactive applications.
   *
   * @param interval the interval between statistics emissions
   * @return a Publisher that emits engine statistics
   * @throws IllegalArgumentException if interval is null or negative
   */
  Publisher<EngineStatistics> getStatisticsReactive(final Duration interval);

  /**
   * Creates a reactive stream for engine events.
   *
   * <p>This method returns a Publisher that emits various engine events such as
   * module loading, instance creation, and resource lifecycle events.
   *
   * @return a Publisher that emits engine events
   */
  Publisher<EngineEvent> getEventStream();

  /**
   * Creates a reactive stream for performance metrics.
   *
   * <p>This method returns a Publisher that periodically emits performance metrics,
   * enabling real-time performance monitoring and reactive adjustment of system
   * parameters.
   *
   * @param interval the interval between metrics emissions
   * @return a Publisher that emits performance metrics
   * @throws IllegalArgumentException if interval is null or negative
   */
  Publisher<PerformanceMetrics> getMetricsStream(final Duration interval);

  /**
   * Creates a reactive stream processor for custom operations.
   *
   * <p>This method allows creation of custom reactive processors that can transform
   * streams of WebAssembly operations according to application-specific logic.
   *
   * @param <T> the input type
   * @param <R> the output type
   * @param processor the stream processor
   * @return a reactive stream processor
   * @throws IllegalArgumentException if processor is null
   */
  <T, R> Publisher<R> createReactiveProcessor(final StreamProcessor<T, R> processor);

  /**
   * Subscribes to reactive operations with custom subscriber.
   *
   * <p>This method enables integration with external reactive libraries and
   * custom processing logic by accepting any Flow.Subscriber implementation.
   *
   * @param <T> the event type
   * @param publisher the event publisher
   * @param subscriber the custom subscriber
   * @throws IllegalArgumentException if publisher or subscriber is null
   */
  <T> void subscribe(final Publisher<T> publisher, final Subscriber<T> subscriber);

  /**
   * Gets the reactive configuration for this engine.
   *
   * @return reactive configuration settings
   */
  ReactiveConfiguration getReactiveConfiguration();

  /**
   * Sets the reactive configuration for this engine.
   *
   * @param configuration the new reactive configuration
   * @throws IllegalArgumentException if configuration is null
   */
  void setReactiveConfiguration(final ReactiveConfiguration configuration);

  /**
   * Represents a module source for reactive compilation.
   */
  interface ModuleSource {
    /**
     * Gets the unique identifier for this module source.
     *
     * @return module source ID
     */
    String getId();

    /**
     * Gets the WebAssembly bytecode.
     *
     * @return WebAssembly bytecode
     */
    byte[] getWasmBytes();

    /**
     * Gets optional metadata associated with this module.
     *
     * @return module metadata, or null if none
     */
    Object getMetadata();
  }

  /**
   * Represents a function call for reactive execution.
   */
  interface FunctionCall {
    /**
     * Gets the unique identifier for this function call.
     *
     * @return function call ID
     */
    String getId();

    /**
     * Gets the name of the function to call.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets the parameters for the function call.
     *
     * @return function parameters
     */
    WasmValue[] getParameters();

    /**
     * Gets optional metadata associated with this call.
     *
     * @return call metadata, or null if none
     */
    Object getMetadata();
  }

  /**
   * Represents a compilation event in the reactive stream.
   */
  interface CompilationEvent {
    /**
     * Gets the module ID associated with this event.
     *
     * @return module ID
     */
    String getModuleId();

    /**
     * Gets the compilation phase.
     *
     * @return compilation phase
     */
    CompilationPhase getPhase();

    /**
     * Gets the compilation progress (0.0 to 1.0).
     *
     * @return compilation progress
     */
    double getProgress();

    /**
     * Gets any error that occurred during compilation.
     *
     * @return compilation error, or null if none
     */
    Exception getError();

    /**
     * Gets the time elapsed since compilation started.
     *
     * @return elapsed time
     */
    Duration getElapsed();

    /**
     * Gets optional metadata associated with this event.
     *
     * @return event metadata, or null if none
     */
    Object getMetadata();
  }

  /**
   * Represents an execution event in the reactive stream.
   */
  interface ExecutionEvent {
    /**
     * Gets the function call ID associated with this event.
     *
     * @return function call ID
     */
    String getCallId();

    /**
     * Gets the execution phase.
     *
     * @return execution phase
     */
    ExecutionPhase getPhase();

    /**
     * Gets the execution results, if available.
     *
     * @return execution results, or null if not yet available
     */
    WasmValue[] getResults();

    /**
     * Gets any error that occurred during execution.
     *
     * @return execution error, or null if none
     */
    Exception getError();

    /**
     * Gets the time elapsed since execution started.
     *
     * @return elapsed time
     */
    Duration getElapsed();

    /**
     * Gets optional metadata associated with this event.
     *
     * @return event metadata, or null if none
     */
    Object getMetadata();
  }

  /**
   * Engine event types for reactive monitoring.
   */
  interface EngineEvent {
    /**
     * Gets the event type.
     *
     * @return event type
     */
    EngineEventType getType();

    /**
     * Gets the timestamp when the event occurred.
     *
     * @return event timestamp
     */
    java.time.Instant getTimestamp();

    /**
     * Gets optional data associated with this event.
     *
     * @return event data, or null if none
     */
    Object getData();

    /**
     * Gets the source component that generated this event.
     *
     * @return event source
     */
    String getSource();
  }

  /**
   * Performance metrics for reactive monitoring.
   */
  interface PerformanceMetrics {
    /**
     * Gets CPU usage percentage.
     *
     * @return CPU usage (0.0 to 1.0)
     */
    double getCpuUsage();

    /**
     * Gets memory usage in bytes.
     *
     * @return memory usage
     */
    long getMemoryUsage();

    /**
     * Gets the number of active operations.
     *
     * @return active operation count
     */
    int getActiveOperations();

    /**
     * Gets throughput in operations per second.
     *
     * @return operations per second
     */
    double getThroughput();

    /**
     * Gets average latency in milliseconds.
     *
     * @return average latency
     */
    double getAverageLatency();

    /**
     * Gets error rate as a percentage.
     *
     * @return error rate (0.0 to 1.0)
     */
    double getErrorRate();
  }

  /**
   * Stream processor for custom reactive transformations.
   */
  interface StreamProcessor<T, R> {
    /**
     * Processes input items and produces output items.
     *
     * @param input the input stream
     * @return the output stream
     */
    Publisher<R> process(Publisher<T> input);
  }

  /**
   * Configuration for reactive operations.
   */
  interface ReactiveConfiguration {
    /**
     * Gets the buffer size for reactive streams.
     *
     * @return buffer size
     */
    int getBufferSize();

    /**
     * Gets the timeout for reactive operations.
     *
     * @return operation timeout
     */
    Duration getOperationTimeout();

    /**
     * Checks if backpressure is enabled.
     *
     * @return true if backpressure is enabled
     */
    boolean isBackpressureEnabled();

    /**
     * Gets the maximum concurrent operations.
     *
     * @return maximum concurrency level
     */
    int getMaxConcurrency();

    /**
     * Checks if error recovery is enabled.
     *
     * @return true if error recovery is enabled
     */
    boolean isErrorRecoveryEnabled();
  }

  /**
   * Compilation phases for progress tracking.
   */
  enum CompilationPhase {
    PARSING, VALIDATION, COMPILATION, OPTIMIZATION, COMPLETED, FAILED
  }

  /**
   * Execution phases for progress tracking.
   */
  enum ExecutionPhase {
    STARTING, EXECUTING, COMPLETED, FAILED
  }

  /**
   * Engine event types.
   */
  enum EngineEventType {
    MODULE_LOADED, MODULE_UNLOADED, INSTANCE_CREATED, INSTANCE_DESTROYED,
    STORE_CREATED, STORE_DESTROYED, ERROR_OCCURRED, PERFORMANCE_THRESHOLD_EXCEEDED
  }
}
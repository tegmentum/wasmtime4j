package ai.tegmentum.wasmtime4j.async.reactive;

import ai.tegmentum.wasmtime4j.EngineStatistics;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.async.AsyncEngine;
import java.time.Duration;
import java.util.function.Predicate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive WebAssembly engine interface with Project Reactor integration.
 *
 * <p>A ReactiveEngine extends the AsyncEngine with full reactive streams support using Project
 * Reactor. This enables reactive programming patterns, backpressure handling, and sophisticated
 * stream composition for WebAssembly operations.
 *
 * <p>The interface provides reactive streams for compilation events, execution monitoring,
 * performance metrics, and engine lifecycle events, enabling real-time monitoring and responsive
 * application architectures.
 *
 * @since 1.0.0
 */
public interface ReactiveEngine extends AsyncEngine {

  /**
   * Creates a reactive stream of compilation events for module compilation operations.
   *
   * <p>This method enables real-time monitoring of compilation progress, phase transitions, and
   * completion or failure events through a Flux stream.
   *
   * @param moduleStream the stream of module sources to compile
   * @return a Flux of compilation events
   * @throws IllegalArgumentException if moduleStream is null
   */
  Flux<CompilationEvent> compileReactive(final Flux<ModuleSource> moduleStream);

  /**
   * Creates a reactive stream of execution events for function call operations.
   *
   * <p>This method enables real-time monitoring of function execution, including parameter
   * validation, execution phases, performance metrics, and results.
   *
   * @param callStream the stream of function calls to execute
   * @return a Flux of execution events
   * @throws IllegalArgumentException if callStream is null
   */
  Flux<ExecutionEvent> executeReactive(final Flux<FunctionCall> callStream);

  /**
   * Gets engine statistics as a reactive Mono.
   *
   * <p>This method provides reactive access to engine statistics, enabling integration with
   * reactive pipelines and composition with other reactive operations.
   *
   * @return a Mono containing engine statistics
   */
  Mono<EngineStatistics> getStatisticsReactive();

  /**
   * Creates a reactive stream of engine events.
   *
   * <p>This stream emits events related to engine lifecycle, resource management, performance, and
   * error conditions, enabling comprehensive monitoring of engine health and activity.
   *
   * @return a Flux of engine events
   */
  Flux<EngineEvent> getEventStream();

  /**
   * Creates a reactive stream of engine events filtered by type.
   *
   * <p>This method allows subscribing to specific types of engine events, reducing noise and
   * enabling focused monitoring of particular aspects of engine operation.
   *
   * @param eventFilter predicate to filter events
   * @return a Flux of filtered engine events
   * @throws IllegalArgumentException if eventFilter is null
   */
  Flux<EngineEvent> getEventStream(final Predicate<EngineEvent> eventFilter);

  /**
   * Creates a reactive stream of performance metrics at regular intervals.
   *
   * <p>This method provides periodic snapshots of engine performance metrics, enabling time-series
   * monitoring and alerting based on performance thresholds.
   *
   * @param interval the interval between metric snapshots
   * @return a Flux of performance metrics
   * @throws IllegalArgumentException if interval is null or non-positive
   */
  Flux<PerformanceMetrics> getMetricsStream(final Duration interval);

  /**
   * Creates a reactive stream of memory usage statistics.
   *
   * <p>This method provides real-time monitoring of memory usage across all stores and instances
   * managed by this engine.
   *
   * @param interval the interval between memory usage snapshots
   * @return a Flux of memory usage metrics
   * @throws IllegalArgumentException if interval is null or non-positive
   */
  Flux<MemoryMetrics> getMemoryMetricsStream(final Duration interval);

  /**
   * Creates a reactive stream of compilation cache statistics.
   *
   * <p>This method provides insights into compilation cache efficiency, hit rates, and memory usage
   * for optimization and debugging purposes.
   *
   * @param interval the interval between cache statistics snapshots
   * @return a Flux of compilation cache metrics
   * @throws IllegalArgumentException if interval is null or non-positive
   */
  Flux<CacheMetrics> getCacheMetricsStream(final Duration interval);

  /**
   * Creates a reactive stream for monitoring store lifecycle events.
   *
   * <p>This method enables monitoring of store creation, destruction, and state changes across all
   * stores managed by this engine.
   *
   * @return a Flux of store lifecycle events
   */
  Flux<StoreEvent> getStoreEventStream();

  /**
   * Creates a reactive stream for monitoring module lifecycle events.
   *
   * <p>This method enables monitoring of module loading, unloading, and compilation across all
   * modules managed by this engine.
   *
   * @return a Flux of module lifecycle events
   */
  Flux<ModuleEvent> getModuleEventStream();

  /**
   * Creates a reactive stream for monitoring instance lifecycle events.
   *
   * <p>This method enables monitoring of instance creation, destruction, and execution across all
   * instances managed by this engine.
   *
   * @return a Flux of instance lifecycle events
   */
  Flux<InstanceEvent> getInstanceEventStream();

  /**
   * Gets the current health status of the engine as a reactive Mono.
   *
   * <p>Health status includes operational state, resource utilization, error rates, and overall
   * engine wellness indicators.
   *
   * @return a Mono containing the current health status
   */
  Mono<EngineHealth> getHealthStatus();

  /**
   * Creates a reactive stream that monitors engine health at regular intervals.
   *
   * <p>This method provides continuous health monitoring with configurable check intervals,
   * enabling proactive alerting and automated recovery actions.
   *
   * @param interval the interval between health checks
   * @return a Flux of health status updates
   * @throws IllegalArgumentException if interval is null or non-positive
   */
  Flux<EngineHealth> getHealthStream(final Duration interval);

  /**
   * Creates a reactive Mono for graceful engine shutdown.
   *
   * <p>This method initiates a graceful shutdown process and returns a Mono that completes when the
   * shutdown is finished, enabling reactive composition of shutdown sequences.
   *
   * @param timeout maximum time to wait for graceful shutdown
   * @return a Mono that completes when shutdown is finished
   * @throws IllegalArgumentException if timeout is null or non-positive
   */
  Mono<Void> shutdownGracefully(final Duration timeout);

  // Supporting interfaces and classes

  /** Represents a module source for reactive compilation. */
  interface ModuleSource {
    /**
     * Gets the module identifier.
     *
     * @return the module identifier
     */
    String getModuleId();

    /**
     * Gets the WebAssembly bytecode.
     *
     * @return the WASM bytecode
     */
    byte[] getWasmBytes();

    /**
     * Gets optional compilation metadata.
     *
     * @return compilation metadata, or null if not available
     */
    CompilationMetadata getMetadata();
  }

  /** Represents a function call for reactive execution. */
  interface FunctionCall {
    /**
     * Gets the call identifier.
     *
     * @return the call identifier
     */
    String getCallId();

    /**
     * Gets the store to execute in.
     *
     * @return the execution store
     */
    Store getStore();

    /**
     * Gets the function name to call.
     *
     * @return the function name
     */
    String getFunctionName();

    /**
     * Gets the function parameters.
     *
     * @return array of function parameters
     */
    Object[] getParameters();

    /**
     * Gets optional execution metadata.
     *
     * @return execution metadata, or null if not available
     */
    ExecutionMetadata getMetadata();
  }

  /** Compilation metadata for modules. */
  interface CompilationMetadata {
    /**
     * Gets the optimization level.
     *
     * @return the optimization level
     */
    String getOptimizationLevel();

    /**
     * Checks if debug information should be included.
     *
     * @return true if debug information is enabled
     */
    boolean isDebugEnabled();

    /**
     * Gets the target platform, if specified.
     *
     * @return the target platform, or null if not specified
     */
    String getTargetPlatform();
  }

  /** Execution metadata for function calls. */
  interface ExecutionMetadata {
    /**
     * Gets the maximum execution time.
     *
     * @return the execution timeout, or null for no timeout
     */
    Duration getTimeout();

    /**
     * Gets the maximum instruction count.
     *
     * @return the instruction limit, or -1 for no limit
     */
    long getMaxInstructions();

    /**
     * Gets the maximum memory usage.
     *
     * @return the memory limit in bytes, or -1 for no limit
     */
    long getMaxMemoryUsage();

    /**
     * Checks if detailed metrics should be collected.
     *
     * @return true if detailed metrics are enabled
     */
    boolean isDetailedMetricsEnabled();
  }

  /** Performance metrics for the engine. */
  interface PerformanceMetrics {
    /**
     * Gets the average compilation time.
     *
     * @return average compilation time in milliseconds
     */
    double getAverageCompilationTime();

    /**
     * Gets the average function execution time.
     *
     * @return average execution time in milliseconds
     */
    double getAverageExecutionTime();

    /**
     * Gets the compilation throughput.
     *
     * @return compilations per second
     */
    double getCompilationThroughput();

    /**
     * Gets the execution throughput.
     *
     * @return function calls per second
     */
    double getExecutionThroughput();

    /**
     * Gets the current CPU usage.
     *
     * @return CPU usage percentage (0.0 to 100.0)
     */
    double getCpuUsage();

    /**
     * Gets the garbage collection overhead.
     *
     * @return GC overhead percentage (0.0 to 100.0)
     */
    double getGcOverhead();
  }

  /** Memory usage metrics. */
  interface MemoryMetrics {
    /**
     * Gets the total allocated memory.
     *
     * @return total memory in bytes
     */
    long getTotalMemory();

    /**
     * Gets the currently used memory.
     *
     * @return used memory in bytes
     */
    long getUsedMemory();

    /**
     * Gets the free memory.
     *
     * @return free memory in bytes
     */
    long getFreeMemory();

    /**
     * Gets the memory usage by WebAssembly instances.
     *
     * @return WASM memory usage in bytes
     */
    long getWasmMemoryUsage();

    /**
     * Gets the memory usage by native code.
     *
     * @return native memory usage in bytes
     */
    long getNativeMemoryUsage();

    /**
     * Gets the number of memory allocations.
     *
     * @return allocation count
     */
    long getAllocationCount();

    /**
     * Gets the number of memory deallocations.
     *
     * @return deallocation count
     */
    long getDeallocationCount();
  }

  /** Compilation cache metrics. */
  interface CacheMetrics {
    /**
     * Gets the cache hit rate.
     *
     * @return hit rate percentage (0.0 to 100.0)
     */
    double getHitRate();

    /**
     * Gets the total number of cache accesses.
     *
     * @return cache access count
     */
    long getTotalAccesses();

    /**
     * Gets the number of cache hits.
     *
     * @return cache hit count
     */
    long getHits();

    /**
     * Gets the number of cache misses.
     *
     * @return cache miss count
     */
    long getMisses();

    /**
     * Gets the current cache size.
     *
     * @return cache size in bytes
     */
    long getCacheSize();

    /**
     * Gets the maximum cache size.
     *
     * @return maximum cache size in bytes
     */
    long getMaxCacheSize();

    /**
     * Gets the number of cached entries.
     *
     * @return cache entry count
     */
    int getEntryCount();
  }

  /** Store lifecycle events. */
  interface StoreEvent extends EngineEvent {
    /**
     * Gets the store identifier.
     *
     * @return the store identifier
     */
    String getStoreId();

    /**
     * Gets the store event type.
     *
     * @return the store event type
     */
    StoreEventType getStoreEventType();
  }

  /** Store event types. */
  enum StoreEventType {
    CREATED,
    DESTROYED,
    SUSPENDED,
    RESUMED,
    ERROR
  }

  /** Module lifecycle events. */
  interface ModuleEvent extends EngineEvent {
    /**
     * Gets the module identifier.
     *
     * @return the module identifier
     */
    String getModuleId();

    /**
     * Gets the module event type.
     *
     * @return the module event type
     */
    ModuleEventType getModuleEventType();
  }

  /** Module event types. */
  enum ModuleEventType {
    LOADED,
    UNLOADED,
    COMPILED,
    COMPILATION_FAILED,
    VALIDATION_FAILED
  }

  /** Instance lifecycle events. */
  interface InstanceEvent extends EngineEvent {
    /**
     * Gets the instance identifier.
     *
     * @return the instance identifier
     */
    String getInstanceId();

    /**
     * Gets the instance event type.
     *
     * @return the instance event type
     */
    InstanceEventType getInstanceEventType();
  }

  /** Instance event types. */
  enum InstanceEventType {
    CREATED,
    DESTROYED,
    FUNCTION_CALLED,
    FUNCTION_RETURNED,
    FUNCTION_FAILED,
    MEMORY_GROWN
  }

  /** Engine health status. */
  interface EngineHealth {
    /**
     * Gets the overall health status.
     *
     * @return the health status
     */
    HealthStatus getStatus();

    /**
     * Gets the health score (0.0 to 100.0).
     *
     * @return the health score
     */
    double getHealthScore();

    /**
     * Gets active health issues.
     *
     * @return array of health issues
     */
    HealthIssue[] getIssues();

    /**
     * Gets the timestamp of the health check.
     *
     * @return the check timestamp
     */
    java.time.Instant getCheckTime();
  }

  /** Health status levels. */
  enum HealthStatus {
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
    CRITICAL
  }

  /** Health issue information. */
  interface HealthIssue {
    /**
     * Gets the issue severity.
     *
     * @return the issue severity
     */
    Severity getSeverity();

    /**
     * Gets the issue description.
     *
     * @return the issue description
     */
    String getDescription();

    /**
     * Gets the issue category.
     *
     * @return the issue category
     */
    String getCategory();

    /**
     * Gets suggested remediation actions.
     *
     * @return remediation suggestions
     */
    String[] getRemediationSuggestions();
  }

  /** Issue severity levels. */
  enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }
}

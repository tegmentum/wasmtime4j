package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import java.lang.foreign.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama Foreign Function Interface implementation of hot reload manager for dynamic WebAssembly
 * module updates (Java 23+).
 *
 * <p>This class provides comprehensive hot-reload capabilities using the Panama FFI, offering
 * better performance and type safety compared to JNI for Java 23+:
 *
 * <ul>
 *   <li>Live module replacement without service interruption
 *   <li>State migration and preservation during updates
 *   <li>Version compatibility checking and rollback mechanisms
 *   <li>Background component loading and validation
 *   <li>Multiple deployment strategies (canary, blue-green, rolling)
 *   <li>Health monitoring and automatic rollback
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * PanamaEngine engine = PanamaEngine.newEngine();
 * HotReloadConfig config = HotReloadConfig.builder()
 *     .validationEnabled(true)
 *     .statePreservationEnabled(true)
 *     .build();
 *
 * try (PanamaHotReloadManager manager = new PanamaHotReloadManager(engine, config)) {
 *     // Start a canary deployment
 *     String operationId = manager.startHotSwap("my-component", "2.0.0",
 *         SwapStrategy.canary(10.0f, 25.0f, 0.99f));
 *
 *     // Monitor the operation
 *     HotSwapStatus status = manager.getSwapStatus(operationId);
 *     System.out.println("Progress: " + status.getProgress());
 * }
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe and can be used concurrently from
 * multiple threads.
 *
 * <p><strong>Resource Management:</strong> This class implements {@link AutoCloseable} and should
 * be used with try-with-resources to ensure proper cleanup.
 */
public final class PanamaHotReloadManager implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(PanamaHotReloadManager.class.getName());

  // Panama FFI function handles
  private static final MethodHandle createHotReloadManager;
  private static final MethodHandle destroyHotReloadManager;
  private static final MethodHandle startHotSwap;
  private static final MethodHandle getSwapStatus;
  private static final MethodHandle cancelHotSwap;
  private static final MethodHandle loadComponentAsync;
  private static final MethodHandle getHotReloadMetrics;
  private static final MethodHandle freeString;
  private static final MethodHandle freeSwapStatus;

  // Memory layouts for C structs
  private static final MemoryLayout SWAP_STATUS_LAYOUT =
      MemoryLayout.structLayout(
          ValueLayout.ADDRESS.withName("component_name"),
          ValueLayout.ADDRESS.withName("from_version"),
          ValueLayout.ADDRESS.withName("to_version"),
          ValueLayout.JAVA_INT.withName("status"),
          ValueLayout.JAVA_FLOAT.withName("progress"),
          ValueLayout.JAVA_LONG.withName("started_at_secs"),
          ValueLayout.JAVA_LONG.withName("total_requests"),
          ValueLayout.JAVA_LONG.withName("successful_requests"),
          ValueLayout.JAVA_LONG.withName("failed_requests"),
          ValueLayout.JAVA_FLOAT.withName("error_rate"));

  private static final MemoryLayout METRICS_LAYOUT =
      MemoryLayout.structLayout(
          ValueLayout.JAVA_LONG.withName("total_swaps"),
          ValueLayout.JAVA_LONG.withName("successful_swaps"),
          ValueLayout.JAVA_LONG.withName("failed_swaps"),
          ValueLayout.JAVA_LONG.withName("rollbacks"),
          ValueLayout.JAVA_LONG.withName("avg_swap_time_ms"),
          ValueLayout.JAVA_INT.withName("current_active_swaps"),
          ValueLayout.JAVA_LONG.withName("components_loaded"),
          ValueLayout.JAVA_FLOAT.withName("cache_efficiency"));

  static {
    try {
      final Linker linker = Linker.nativeLinker();
      final SymbolLookup lookup = SymbolLookup.loaderLookup().or(linker.defaultLookup());

      // Initialize function handles
      createHotReloadManager =
          linker.downcallHandle(
              lookup.find("panama_create_hot_reload_manager").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS, // engine_ptr
                  ValueLayout.JAVA_INT, // validation_enabled
                  ValueLayout.JAVA_INT, // state_preservation_enabled
                  ValueLayout.JAVA_LONG, // debounce_delay_ms
                  ValueLayout.JAVA_INT, // precompilation_enabled
                  ValueLayout.JAVA_INT, // max_reload_attempts
                  ValueLayout.JAVA_LONG, // health_check_interval_secs
                  ValueLayout.JAVA_INT, // loader_thread_count
                  ValueLayout.JAVA_INT // cache_size
                  ));

      destroyHotReloadManager =
          linker.downcallHandle(
              lookup.find("panama_destroy_hot_reload_manager").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

      startHotSwap =
          linker.downcallHandle(
              lookup.find("panama_start_hot_swap").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // manager_ptr
                  ValueLayout.ADDRESS, // component_name
                  ValueLayout.ADDRESS, // version_string
                  ValueLayout.JAVA_INT, // swap_strategy_type
                  ValueLayout.JAVA_LONG, // strategy_param1
                  ValueLayout.JAVA_LONG, // strategy_param2
                  ValueLayout.JAVA_DOUBLE, // strategy_param3
                  ValueLayout.ADDRESS // operation_id_out
                  ));

      getSwapStatus =
          linker.downcallHandle(
              lookup.find("panama_get_swap_status").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // manager_ptr
                  ValueLayout.ADDRESS, // operation_id
                  ValueLayout.ADDRESS // status_out
                  ));

      cancelHotSwap =
          linker.downcallHandle(
              lookup.find("panama_cancel_hot_swap").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // manager_ptr
                  ValueLayout.ADDRESS // operation_id
                  ));

      loadComponentAsync =
          linker.downcallHandle(
              lookup.find("panama_load_component_async").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // manager_ptr
                  ValueLayout.ADDRESS, // component_name
                  ValueLayout.ADDRESS, // component_path
                  ValueLayout.ADDRESS, // version_string
                  ValueLayout.JAVA_INT, // priority
                  ValueLayout.JAVA_INT, // validate_interfaces
                  ValueLayout.JAVA_INT, // validate_dependencies
                  ValueLayout.JAVA_INT, // validate_security
                  ValueLayout.JAVA_INT, // validate_performance
                  ValueLayout.JAVA_LONG, // timeout_secs
                  ValueLayout.ADDRESS // request_id_out
                  ));

      getHotReloadMetrics =
          linker.downcallHandle(
              lookup.find("panama_get_hot_reload_metrics").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // manager_ptr
                  ValueLayout.ADDRESS // metrics_out
                  ));

      freeString =
          linker.downcallHandle(
              lookup.find("panama_free_string").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

      freeSwapStatus =
          linker.downcallHandle(
              lookup.find("panama_free_swap_status").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    } catch (final Throwable e) {
      throw new RuntimeException("Failed to initialize Panama hot reload manager", e);
    }
  }

  private final MemorySegment nativeHandle;
  private final Arena arena;
  private volatile boolean closed = false;

  /**
   * Creates a new hot reload manager with the specified engine and configuration.
   *
   * @param engine The WebAssembly engine to use for module compilation
   * @param config The hot reload configuration
   * @throws IllegalArgumentException if engine or config is null
   * @throws WasmRuntimeException if the native manager cannot be created
   */
  public PanamaHotReloadManager(final PanamaEngine engine, final HotReloadConfig config) {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    logger.fine("Creating Panama hot reload manager");

    this.arena = Arena.ofConfined();

    try {
      this.nativeHandle =
          (MemorySegment)
              createHotReloadManager.invoke(
                  engine.getNativeHandle(),
                  config.isValidationEnabled() ? 1 : 0,
                  config.isStatePreservationEnabled() ? 1 : 0,
                  config.getDebounceDelayMs(),
                  config.isPrecompilationEnabled() ? 1 : 0,
                  config.getMaxReloadAttempts(),
                  config.getHealthCheckIntervalSecs(),
                  config.getLoaderThreadCount(),
                  config.getCacheSize());

      if (nativeHandle == null || nativeHandle.equals(MemorySegment.NULL)) {
        throw new WasmRuntimeException("Failed to create native hot reload manager");
      }

      logger.info("Panama hot reload manager created successfully");

    } catch (final Throwable e) {
      throw new WasmRuntimeException("Failed to create hot reload manager", e);
    }
  }

  /**
   * Starts a hot swap operation to replace the current component version.
   *
   * @param componentName The name of the component to swap
   * @param targetVersion The target version to swap to
   * @param strategy The swap strategy to use (null for default)
   * @return A unique operation ID for tracking the swap
   * @throws IllegalArgumentException if componentName or targetVersion is null/empty
   * @throws WasmRuntimeException if the swap cannot be started
   * @throws IllegalStateException if this manager has been closed
   */
  public String startHotSwap(
      final String componentName, final String targetVersion, final SwapStrategy strategy) {
    checkNotClosed();

    if (componentName == null || componentName.trim().isEmpty()) {
      throw new IllegalArgumentException("Component name cannot be null or empty");
    }
    if (targetVersion == null || targetVersion.trim().isEmpty()) {
      throw new IllegalArgumentException("Target version cannot be null or empty");
    }

    logger.info(String.format("Starting hot swap: %s -> %s", componentName, targetVersion));

    final SwapStrategy actualStrategy = strategy != null ? strategy : SwapStrategy.getDefault();

    try (final Arena localArena = Arena.ofConfined()) {
      final MemorySegment nameSegment = localArena.allocateUtf8String(componentName);
      final MemorySegment versionSegment = localArena.allocateUtf8String(targetVersion);
      final MemorySegment operationIdPtr = localArena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              startHotSwap.invoke(
                  nativeHandle,
                  nameSegment,
                  versionSegment,
                  actualStrategy.getType(),
                  actualStrategy.getParam1(),
                  actualStrategy.getParam2(),
                  actualStrategy.getParam3(),
                  operationIdPtr);

      if (result != 0) {
        throw new WasmRuntimeException("Failed to start hot swap operation");
      }

      final MemorySegment operationIdSegment = operationIdPtr.get(ValueLayout.ADDRESS, 0);
      final String operationId = operationIdSegment.getUtf8String(0);

      // Free the native string
      freeString.invoke(operationIdSegment);

      logger.info(String.format("Hot swap operation started: %s", operationId));
      return operationId;

    } catch (final Throwable e) {
      throw new WasmRuntimeException("Failed to start hot swap", e);
    }
  }

  /**
   * Gets the current status of a hot swap operation.
   *
   * @param operationId The operation ID returned by startHotSwap
   * @return The current status of the operation, or null if not found
   * @throws IllegalArgumentException if operationId is null/empty
   * @throws IllegalStateException if this manager has been closed
   */
  public HotSwapStatus getSwapStatus(final String operationId) {
    checkNotClosed();

    if (operationId == null || operationId.trim().isEmpty()) {
      throw new IllegalArgumentException("Operation ID cannot be null or empty");
    }

    try (final Arena localArena = Arena.ofConfined()) {
      final MemorySegment operationIdSegment = localArena.allocateUtf8String(operationId);
      final MemorySegment statusSegment = localArena.allocate(SWAP_STATUS_LAYOUT);

      final int result =
          (int) getSwapStatus.invoke(nativeHandle, operationIdSegment, statusSegment);

      if (result == 1) {
        return null; // Not found
      }
      if (result != 0) {
        throw new WasmRuntimeException("Failed to get swap status");
      }

      // Extract status information
      final MemorySegment componentNamePtr = statusSegment.get(ValueLayout.ADDRESS, 0);
      final MemorySegment fromVersionPtr =
          statusSegment.get(ValueLayout.ADDRESS, ValueLayout.ADDRESS.byteSize());
      final MemorySegment toVersionPtr =
          statusSegment.get(ValueLayout.ADDRESS, ValueLayout.ADDRESS.byteSize() * 2);

      final String componentName = componentNamePtr.getUtf8String(0);
      final String fromVersion = fromVersionPtr.getUtf8String(0);
      final String toVersion = toVersionPtr.getUtf8String(0);
      final int status =
          statusSegment.get(ValueLayout.JAVA_INT, ValueLayout.ADDRESS.byteSize() * 3);
      final float progress =
          statusSegment.get(
              ValueLayout.JAVA_FLOAT,
              ValueLayout.ADDRESS.byteSize() * 3 + ValueLayout.JAVA_INT.byteSize());

      final HotSwapStatus hotSwapStatus =
          new HotSwapStatus(operationId, componentName, fromVersion, toVersion, status, progress);

      // Free the native memory
      freeSwapStatus.invoke(statusSegment);

      return hotSwapStatus;

    } catch (final Throwable e) {
      throw new WasmRuntimeException("Failed to get swap status", e);
    }
  }

  /**
   * Cancels a hot swap operation if it's still in progress.
   *
   * @param operationId The operation ID to cancel
   * @return true if the operation was cancelled, false if it couldn't be cancelled
   * @throws IllegalArgumentException if operationId is null/empty
   * @throws IllegalStateException if this manager has been closed
   */
  public boolean cancelHotSwap(final String operationId) {
    checkNotClosed();

    if (operationId == null || operationId.trim().isEmpty()) {
      throw new IllegalArgumentException("Operation ID cannot be null or empty");
    }

    logger.info(String.format("Cancelling hot swap operation: %s", operationId));

    try (final Arena localArena = Arena.ofConfined()) {
      final MemorySegment operationIdSegment = localArena.allocateUtf8String(operationId);

      final int result = (int) cancelHotSwap.invoke(nativeHandle, operationIdSegment);

      final boolean success = result == 0;

      if (success) {
        logger.info(String.format("Hot swap operation cancelled: %s", operationId));
      } else {
        logger.warning(String.format("Failed to cancel hot swap operation: %s", operationId));
      }

      return success;

    } catch (final Throwable e) {
      logger.log(Level.WARNING, "Error cancelling hot swap", e);
      return false;
    }
  }

  /**
   * Loads a component asynchronously in the background for faster hot swaps.
   *
   * @param request The load request containing component details
   * @return A CompletableFuture that completes with the request ID
   * @throws IllegalArgumentException if request is null or invalid
   * @throws IllegalStateException if this manager has been closed
   */
  public CompletableFuture<String> loadComponentAsync(final LoadRequest request) {
    checkNotClosed();

    if (request == null) {
      throw new IllegalArgumentException("Load request cannot be null");
    }
    request.validate();

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            logger.fine(
                String.format("Loading component asynchronously: %s", request.getComponentName()));

            try (final Arena localArena = Arena.ofConfined()) {
              final MemorySegment nameSegment =
                  localArena.allocateUtf8String(request.getComponentName());
              final MemorySegment pathSegment =
                  localArena.allocateUtf8String(request.getComponentPath());
              final MemorySegment versionSegment =
                  localArena.allocateUtf8String(request.getVersion());
              final MemorySegment requestIdPtr = localArena.allocate(ValueLayout.ADDRESS);

              final int result =
                  (int)
                      loadComponentAsync.invoke(
                          nativeHandle,
                          nameSegment,
                          pathSegment,
                          versionSegment,
                          request.getPriority().ordinal(),
                          request.getValidationConfig().isValidateInterfaces() ? 1 : 0,
                          request.getValidationConfig().isValidateDependencies() ? 1 : 0,
                          request.getValidationConfig().isValidateSecurity() ? 1 : 0,
                          request.getValidationConfig().isValidatePerformance() ? 1 : 0,
                          request.getValidationConfig().getTimeoutSecs(),
                          requestIdPtr);

              if (result != 0) {
                throw new WasmRuntimeException("Failed to submit component load request");
              }

              final MemorySegment requestIdSegment = requestIdPtr.get(ValueLayout.ADDRESS, 0);
              final String requestId = requestIdSegment.getUtf8String(0);

              // Free the native string
              freeString.invoke(requestIdSegment);

              logger.fine(String.format("Component load request submitted: %s", requestId));
              return requestId;
            }

          } catch (final Throwable e) {
            logger.log(Level.WARNING, "Failed to load component asynchronously", e);
            throw new CompletionException(e);
          }
        });
  }

  /**
   * Gets the current hot reload metrics and performance statistics.
   *
   * @return The current metrics
   * @throws IllegalStateException if this manager has been closed
   */
  public HotReloadMetrics getMetrics() {
    checkNotClosed();

    try (final Arena localArena = Arena.ofConfined()) {
      final MemorySegment metricsSegment = localArena.allocate(METRICS_LAYOUT);

      final int result = (int) getHotReloadMetrics.invoke(nativeHandle, metricsSegment);

      if (result != 0) {
        throw new WasmRuntimeException("Failed to retrieve hot reload metrics");
      }

      // Extract metrics
      long offset = 0;
      final long totalSwaps = metricsSegment.get(ValueLayout.JAVA_LONG, offset);
      offset += ValueLayout.JAVA_LONG.byteSize();
      final long successfulSwaps = metricsSegment.get(ValueLayout.JAVA_LONG, offset);
      offset += ValueLayout.JAVA_LONG.byteSize();
      final long failedSwaps = metricsSegment.get(ValueLayout.JAVA_LONG, offset);
      offset += ValueLayout.JAVA_LONG.byteSize();
      final long rollbacks = metricsSegment.get(ValueLayout.JAVA_LONG, offset);
      offset += ValueLayout.JAVA_LONG.byteSize();
      final long avgSwapTimeMs = metricsSegment.get(ValueLayout.JAVA_LONG, offset);
      offset += ValueLayout.JAVA_LONG.byteSize();
      final int currentActiveSwaps = metricsSegment.get(ValueLayout.JAVA_INT, offset);
      offset += ValueLayout.JAVA_INT.byteSize();
      final long componentsLoaded = metricsSegment.get(ValueLayout.JAVA_LONG, offset);
      offset += ValueLayout.JAVA_LONG.byteSize();
      final float cacheEfficiency = metricsSegment.get(ValueLayout.JAVA_FLOAT, offset);

      return new HotReloadMetrics(
          totalSwaps,
          successfulSwaps,
          failedSwaps,
          rollbacks,
          avgSwapTimeMs,
          currentActiveSwaps,
          componentsLoaded,
          cacheEfficiency);

    } catch (final Throwable e) {
      throw new WasmRuntimeException("Failed to get metrics", e);
    }
  }

  /**
   * Gets the native handle for this hot reload manager.
   *
   * @return The native handle
   * @throws IllegalStateException if this manager has been closed
   */
  public MemorySegment getNativeHandle() {
    checkNotClosed();
    return nativeHandle;
  }

  /**
   * Checks if this hot reload manager has been closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed;
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;

      logger.fine("Closing Panama hot reload manager");

      try {
        destroyHotReloadManager.invoke(nativeHandle);
        arena.close();
        logger.info("Panama hot reload manager closed successfully");
      } catch (final Throwable e) {
        logger.log(Level.WARNING, "Error closing Panama hot reload manager", e);
      }
    }
  }

  @Override
  public String toString() {
    return String.format("PanamaHotReloadManager{handle=%s, closed=%s}", nativeHandle, closed);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PanamaHotReloadManager)) return false;
    final PanamaHotReloadManager other = (PanamaHotReloadManager) obj;
    return Objects.equals(nativeHandle, other.nativeHandle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nativeHandle);
  }

  private void checkNotClosed() {
    if (closed) {
      throw new IllegalStateException("Hot reload manager has been closed");
    }
  }

  // Static classes shared with JNI implementation - these would typically be in a common module

  /** Configuration for hot reload behavior. */
  public static final class HotReloadConfig {
    private final boolean validationEnabled;
    private final boolean statePreservationEnabled;
    private final long debounceDelayMs;
    private final boolean precompilationEnabled;
    private final int maxReloadAttempts;
    private final long healthCheckIntervalSecs;
    private final int loaderThreadCount;
    private final int cacheSize;

    private HotReloadConfig(final Builder builder) {
      this.validationEnabled = builder.validationEnabled;
      this.statePreservationEnabled = builder.statePreservationEnabled;
      this.debounceDelayMs = builder.debounceDelayMs;
      this.precompilationEnabled = builder.precompilationEnabled;
      this.maxReloadAttempts = builder.maxReloadAttempts;
      this.healthCheckIntervalSecs = builder.healthCheckIntervalSecs;
      this.loaderThreadCount = builder.loaderThreadCount;
      this.cacheSize = builder.cacheSize;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static HotReloadConfig getDefault() {
      return builder().build();
    }

    // Getters
    public boolean isValidationEnabled() {
      return validationEnabled;
    }

    public boolean isStatePreservationEnabled() {
      return statePreservationEnabled;
    }

    public long getDebounceDelayMs() {
      return debounceDelayMs;
    }

    public boolean isPrecompilationEnabled() {
      return precompilationEnabled;
    }

    public int getMaxReloadAttempts() {
      return maxReloadAttempts;
    }

    public long getHealthCheckIntervalSecs() {
      return healthCheckIntervalSecs;
    }

    public int getLoaderThreadCount() {
      return loaderThreadCount;
    }

    public int getCacheSize() {
      return cacheSize;
    }

    public static final class Builder {
      private boolean validationEnabled = true;
      private boolean statePreservationEnabled = true;
      private long debounceDelayMs = 100;
      private boolean precompilationEnabled = true;
      private int maxReloadAttempts = 3;
      private long healthCheckIntervalSecs = 30;
      private int loaderThreadCount = 4;
      private int cacheSize = 100;

      public Builder validationEnabled(final boolean enabled) {
        this.validationEnabled = enabled;
        return this;
      }

      public Builder statePreservationEnabled(final boolean enabled) {
        this.statePreservationEnabled = enabled;
        return this;
      }

      public Builder debounceDelayMs(final long delayMs) {
        this.debounceDelayMs = delayMs;
        return this;
      }

      public Builder precompilationEnabled(final boolean enabled) {
        this.precompilationEnabled = enabled;
        return this;
      }

      public Builder maxReloadAttempts(final int attempts) {
        this.maxReloadAttempts = attempts;
        return this;
      }

      public Builder healthCheckIntervalSecs(final long intervalSecs) {
        this.healthCheckIntervalSecs = intervalSecs;
        return this;
      }

      public Builder loaderThreadCount(final int threadCount) {
        this.loaderThreadCount = threadCount;
        return this;
      }

      public Builder cacheSize(final int size) {
        this.cacheSize = size;
        return this;
      }

      public HotReloadConfig build() {
        return new HotReloadConfig(this);
      }
    }
  }

  /** Hot swap deployment strategies. */
  public abstract static class SwapStrategy {
    protected final int type;
    protected final long param1;
    protected final long param2;
    protected final double param3;

    protected SwapStrategy(
        final int type, final long param1, final long param2, final double param3) {
      this.type = type;
      this.param1 = param1;
      this.param2 = param2;
      this.param3 = param3;
    }

    public int getType() {
      return type;
    }

    public long getParam1() {
      return param1;
    }

    public long getParam2() {
      return param2;
    }

    public double getParam3() {
      return param3;
    }

    public static SwapStrategy immediate() {
      return new SwapStrategy(0, 0, 0, 0.0) {};
    }

    public static SwapStrategy canary(
        final float initialPercentage,
        final float incrementPercentage,
        final float successThreshold) {
      return new SwapStrategy(
          1,
          (long) (initialPercentage * 100),
          (long) (incrementPercentage * 100),
          successThreshold) {};
    }

    public static SwapStrategy blueGreen() {
      return new SwapStrategy(2, 0, 0, 0.0) {};
    }

    public static SwapStrategy rollingUpdate(final int batchSize, final long batchIntervalSecs) {
      return new SwapStrategy(3, batchSize, batchIntervalSecs, 0.0) {};
    }

    public static SwapStrategy abTest(final float testPercentage, final long testDurationSecs) {
      return new SwapStrategy(4, (long) (testPercentage * 100), testDurationSecs, 0.0) {};
    }

    public static SwapStrategy getDefault() {
      return canary(10.0f, 25.0f, 0.99f);
    }
  }

  /** Status of a hot swap operation. */
  public static final class HotSwapStatus {
    private final String operationId;
    private final String componentName;
    private final String fromVersion;
    private final String toVersion;
    private final int status;
    private final float progress;

    public HotSwapStatus(
        final String operationId,
        final String componentName,
        final String fromVersion,
        final String toVersion,
        final int status,
        final float progress) {
      this.operationId = operationId;
      this.componentName = componentName;
      this.fromVersion = fromVersion;
      this.toVersion = toVersion;
      this.status = status;
      this.progress = progress;
    }

    public String getOperationId() {
      return operationId;
    }

    public String getComponentName() {
      return componentName;
    }

    public String getFromVersion() {
      return fromVersion;
    }

    public String getToVersion() {
      return toVersion;
    }

    public SwapStatus getStatus() {
      return SwapStatus.fromOrdinal(status);
    }

    public float getProgress() {
      return progress;
    }

    @Override
    public String toString() {
      return String.format(
          "HotSwapStatus{id=%s, component=%s, %s->%s, status=%s, progress=%.1f%%}",
          operationId, componentName, fromVersion, toVersion, getStatus(), progress * 100);
    }
  }

  public enum SwapStatus {
    PENDING,
    PRE_LOADING,
    VALIDATING,
    STARTING,
    TRAFFIC_SHIFTING,
    MONITORING,
    COMPLETED,
    FAILED,
    ROLLING_BACK,
    ROLLBACK_COMPLETED;

    public static SwapStatus fromOrdinal(final int ordinal) {
      final SwapStatus[] values = values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PENDING;
    }
  }

  public static final class LoadRequest {
    private final String componentName;
    private final String componentPath;
    private final String version;
    private final LoadPriority priority;
    private final ValidationConfig validationConfig;

    public LoadRequest(
        final String componentName,
        final String componentPath,
        final String version,
        final LoadPriority priority,
        final ValidationConfig validationConfig) {
      this.componentName = componentName;
      this.componentPath = componentPath;
      this.version = version;
      this.priority = priority != null ? priority : LoadPriority.NORMAL;
      this.validationConfig =
          validationConfig != null ? validationConfig : ValidationConfig.getDefault();
    }

    public void validate() {
      if (componentName == null || componentName.trim().isEmpty()) {
        throw new IllegalArgumentException("Component name cannot be null or empty");
      }
      if (componentPath == null || componentPath.trim().isEmpty()) {
        throw new IllegalArgumentException("Component path cannot be null or empty");
      }
      if (version == null || version.trim().isEmpty()) {
        throw new IllegalArgumentException("Version cannot be null or empty");
      }
    }

    public String getComponentName() {
      return componentName;
    }

    public String getComponentPath() {
      return componentPath;
    }

    public String getVersion() {
      return version;
    }

    public LoadPriority getPriority() {
      return priority;
    }

    public ValidationConfig getValidationConfig() {
      return validationConfig;
    }
  }

  public enum LoadPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
  }

  public static final class ValidationConfig {
    private final boolean validateInterfaces;
    private final boolean validateDependencies;
    private final boolean validateSecurity;
    private final boolean validatePerformance;
    private final long timeoutSecs;

    public ValidationConfig(
        final boolean validateInterfaces,
        final boolean validateDependencies,
        final boolean validateSecurity,
        final boolean validatePerformance,
        final long timeoutSecs) {
      this.validateInterfaces = validateInterfaces;
      this.validateDependencies = validateDependencies;
      this.validateSecurity = validateSecurity;
      this.validatePerformance = validatePerformance;
      this.timeoutSecs = timeoutSecs;
    }

    public static ValidationConfig getDefault() {
      return new ValidationConfig(true, true, true, false, 30);
    }

    public boolean isValidateInterfaces() {
      return validateInterfaces;
    }

    public boolean isValidateDependencies() {
      return validateDependencies;
    }

    public boolean isValidateSecurity() {
      return validateSecurity;
    }

    public boolean isValidatePerformance() {
      return validatePerformance;
    }

    public long getTimeoutSecs() {
      return timeoutSecs;
    }
  }

  public static final class HotReloadMetrics {
    private final long totalSwaps;
    private final long successfulSwaps;
    private final long failedSwaps;
    private final long rollbacks;
    private final long avgSwapTimeMs;
    private final int currentActiveSwaps;
    private final long componentsLoaded;
    private final float cacheEfficiency;

    public HotReloadMetrics(
        final long totalSwaps,
        final long successfulSwaps,
        final long failedSwaps,
        final long rollbacks,
        final long avgSwapTimeMs,
        final int currentActiveSwaps,
        final long componentsLoaded,
        final float cacheEfficiency) {
      this.totalSwaps = totalSwaps;
      this.successfulSwaps = successfulSwaps;
      this.failedSwaps = failedSwaps;
      this.rollbacks = rollbacks;
      this.avgSwapTimeMs = avgSwapTimeMs;
      this.currentActiveSwaps = currentActiveSwaps;
      this.componentsLoaded = componentsLoaded;
      this.cacheEfficiency = cacheEfficiency;
    }

    public long getTotalSwaps() {
      return totalSwaps;
    }

    public long getSuccessfulSwaps() {
      return successfulSwaps;
    }

    public long getFailedSwaps() {
      return failedSwaps;
    }

    public long getRollbacks() {
      return rollbacks;
    }

    public long getAvgSwapTimeMs() {
      return avgSwapTimeMs;
    }

    public int getCurrentActiveSwaps() {
      return currentActiveSwaps;
    }

    public long getComponentsLoaded() {
      return componentsLoaded;
    }

    public float getCacheEfficiency() {
      return cacheEfficiency;
    }

    public double getSuccessRate() {
      return totalSwaps > 0 ? (double) successfulSwaps / totalSwaps : 1.0;
    }

    @Override
    public String toString() {
      return String.format(
          "HotReloadMetrics{totalSwaps=%d, successRate=%.1f%%, avgSwapTime=%dms}",
          totalSwaps, getSuccessRate() * 100, avgSwapTimeMs);
    }
  }
}

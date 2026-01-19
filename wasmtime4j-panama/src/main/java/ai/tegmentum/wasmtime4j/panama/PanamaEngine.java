package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.ComponentVersion;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.performance.EngineStatistics;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Engine.
 *
 * @since 1.0.0
 */
public final class PanamaEngine implements Engine {
  private static final Logger LOGGER = Logger.getLogger(PanamaEngine.class.getName());
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final Arena arena;
  private final MemorySegment nativeEngine;
  private final MemorySegment profilerHandle;
  private final EngineConfig config;
  private final Instant createdAt;
  private final WasmRuntime runtime;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama engine with default configuration.
   *
   * <p>This constructor is intended for unit tests. Production code should use {@link
   * #PanamaEngine(EngineConfig, WasmRuntime)}.
   *
   * @throws WasmException if engine creation fails
   */
  public PanamaEngine() throws WasmException {
    this(new EngineConfig(), null);
  }

  /**
   * Creates a new Panama engine with specified configuration.
   *
   * <p>This constructor is intended for unit tests. Production code should use {@link
   * #PanamaEngine(EngineConfig, WasmRuntime)}.
   *
   * @param config the engine configuration
   * @throws WasmException if engine creation fails
   */
  public PanamaEngine(final EngineConfig config) throws WasmException {
    this(config, null);
  }

  /**
   * Creates a new Panama engine with specified configuration and runtime reference.
   *
   * @param config the engine configuration
   * @param runtime the runtime that owns this engine
   * @throws WasmException if engine creation fails
   */
  public PanamaEngine(final EngineConfig config, final WasmRuntime runtime) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }
    this.config = config;
    this.runtime = runtime;
    this.arena = Arena.ofShared();

    // Create native engine via Panama FFI with config
    this.nativeEngine = NATIVE_BINDINGS.engineCreateWithConfig(config);

    if (this.nativeEngine == null || this.nativeEngine.equals(MemorySegment.NULL)) {
      arena.close();
      throw new WasmException("Failed to create native engine");
    }

    // Profiler disabled by default for performance - enable via config if needed
    this.profilerHandle = null;
    this.createdAt = Instant.now();

    LOGGER.fine("Created Panama engine");
  }

  @Override
  public Store createStore() throws WasmException {
    ensureNotClosed();
    return new PanamaStore(this);
  }

  @Override
  public Store createStore(final Object data) throws WasmException {
    final Store store = createStore();
    store.setData(data);
    return store;
  }

  @Override
  public WasmRuntime getRuntime() {
    return runtime;
  }

  @Override
  public Module compileModule(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("WASM bytes cannot be null or empty");
    }
    ensureNotClosed();
    return new PanamaModule(this, wasmBytes);
  }

  @Override
  public Module compileWat(final String wat) throws WasmException {
    if (wat == null) {
      throw new IllegalArgumentException("wat cannot be null");
    }
    if (wat.isEmpty()) {
      throw new IllegalArgumentException("wat cannot be empty");
    }
    ensureNotClosed();

    // Allocate C string for WAT text
    final MemorySegment watSegment = arena.allocateFrom(wat);

    // Allocate pointer for output module
    final MemorySegment modulePtr = arena.allocate(ValueLayout.ADDRESS);

    // Call native function
    final int result = NATIVE_BINDINGS.moduleCompileWat(nativeEngine, watSegment, modulePtr);

    if (result != 0) {
      final String nativeError = retrieveNativeErrorMessage();
      if (nativeError != null && !nativeError.isEmpty()) {
        throw new WasmException("Failed to compile WAT: " + nativeError);
      }
      throw new WasmException("Failed to compile WAT (error code: " + result + ")");
    }

    // Get the module pointer
    final MemorySegment nativeModulePtr = modulePtr.get(ValueLayout.ADDRESS, 0);

    if (nativeModulePtr == null || nativeModulePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Native WAT compilation returned null module pointer");
    }

    return new PanamaModule(this, nativeModulePtr);
  }

  @Override
  public byte[] precompileModule(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null) {
      throw new IllegalArgumentException("wasmBytes cannot be null");
    }
    if (wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be empty");
    }
    ensureNotClosed();

    return NATIVE_BINDINGS.enginePrecompileModule(nativeEngine, wasmBytes);
  }

  @Override
  public Module compileFromStream(final InputStream stream) throws WasmException, IOException {
    if (stream == null) {
      throw new IllegalArgumentException("stream cannot be null");
    }
    ensureNotClosed();

    // Read entire stream into byte array
    // Wasmtime requires complete bytecode before compilation
    final byte[] wasmBytes = readAllBytes(stream);

    if (wasmBytes.length == 0) {
      throw new WasmException("Stream contained no data");
    }

    return compileModule(wasmBytes);
  }

  /**
   * Reads all bytes from an input stream.
   *
   * @param stream the input stream to read
   * @return all bytes from the stream
   * @throws IOException if reading fails
   */
  private byte[] readAllBytes(final InputStream stream) throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final byte[] data = new byte[8192];
    int bytesRead;
    while ((bytesRead = stream.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, bytesRead);
    }
    return buffer.toByteArray();
  }

  @Override
  public EngineConfig getConfig() {
    return config;
  }

  @Override
  public boolean isValid() {
    return !closed;
  }

  @Override
  public boolean supportsFeature(final WasmFeature feature) {
    if (feature == null) {
      return false;
    }
    ensureNotClosed();
    return NATIVE_BINDINGS.engineSupportsFeature(nativeEngine, feature.name());
  }

  @Override
  public int getMemoryLimitPages() {
    ensureNotClosed();
    final int limit = NATIVE_BINDINGS.engineMemoryLimitPages(nativeEngine);
    return limit == -1 ? 0 : limit;
  }

  @Override
  public long getStackSizeLimit() {
    ensureNotClosed();
    final long limit = NATIVE_BINDINGS.engineStackSizeLimit(nativeEngine);
    return limit == -1 ? 0 : limit;
  }

  @Override
  public boolean isFuelEnabled() {
    ensureNotClosed();
    return NATIVE_BINDINGS.engineFuelEnabled(nativeEngine);
  }

  @Override
  public boolean isEpochInterruptionEnabled() {
    ensureNotClosed();
    return NATIVE_BINDINGS.engineEpochInterruptionEnabled(nativeEngine);
  }

  @Override
  public boolean isCoredumpOnTrapEnabled() {
    ensureNotClosed();
    return NATIVE_BINDINGS.engineCoredumpOnTrapEnabled(nativeEngine);
  }

  @Override
  public int getMaxInstances() {
    // Max instances tracking not implemented - return unlimited
    // Matches JNI backend which returns Integer.MAX_VALUE
    return Integer.MAX_VALUE;
  }

  @Override
  public long getReferenceCount() {
    // Reference counting not implemented - return single reference
    // Matches JNI backend behavior
    return 1;
  }

  @Override
  public void incrementEpoch() {
    ensureNotClosed();
    NATIVE_BINDINGS.engineIncrementEpoch(nativeEngine);
  }

  @Override
  public EngineStatistics captureStatistics() {
    ensureNotClosed();
    final MemorySegment profiler = this.profilerHandle;
    final Instant created = this.createdAt;
    final Instant captureTime = Instant.now();

    return new EngineStatistics() {
      @Override
      public long getModulesCompiled() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return 0;
        }
        return NATIVE_BINDINGS.profilerGetModulesCompiled(profiler);
      }

      @Override
      public Duration getTotalCompilationTime() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return Duration.ZERO;
        }
        final long nanos = NATIVE_BINDINGS.profilerGetTotalCompilationTimeNanos(profiler);
        return Duration.ofNanos(nanos);
      }

      @Override
      public Duration getAverageCompilationTime() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return Duration.ZERO;
        }
        final long nanos = NATIVE_BINDINGS.profilerGetAverageCompilationTimeNanos(profiler);
        return Duration.ofNanos(nanos);
      }

      @Override
      public long getBytesCompiled() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return 0;
        }
        return NATIVE_BINDINGS.profilerGetBytesCompiled(profiler);
      }

      @Override
      public double getCompilationThroughput() {
        final Duration totalTime = getTotalCompilationTime();
        if (totalTime.isZero()) {
          return 0.0;
        }
        return (double) getBytesCompiled() / totalTime.toSeconds();
      }

      @Override
      public long getFunctionsExecuted() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return 0;
        }
        return NATIVE_BINDINGS.profilerGetTotalFunctionCalls(profiler);
      }

      @Override
      public Duration getTotalExecutionTime() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return Duration.ZERO;
        }
        final long nanos = NATIVE_BINDINGS.profilerGetTotalExecutionTimeNanos(profiler);
        return Duration.ofNanos(nanos);
      }

      @Override
      public long getInstructionsExecuted() {
        // Not tracked at instruction level - return function calls as proxy
        return getFunctionsExecuted();
      }

      @Override
      public double getExecutionThroughput() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return 0.0;
        }
        return NATIVE_BINDINGS.profilerGetFunctionCallsPerSecond(profiler);
      }

      @Override
      public long getPeakMemoryUsage() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return 0;
        }
        return NATIVE_BINDINGS.profilerGetPeakMemoryBytes(profiler);
      }

      @Override
      public long getCurrentMemoryUsage() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return 0;
        }
        return NATIVE_BINDINGS.profilerGetCurrentMemoryBytes(profiler);
      }

      @Override
      public long getTotalAllocations() {
        // Not tracked separately - return 0
        return 0;
      }

      @Override
      public long getTotalDeallocations() {
        // Not tracked separately - return 0
        return 0;
      }

      @Override
      public long getCacheHits() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return 0;
        }
        return NATIVE_BINDINGS.profilerGetCacheHits(profiler);
      }

      @Override
      public long getCacheMisses() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return 0;
        }
        return NATIVE_BINDINGS.profilerGetCacheMisses(profiler);
      }

      @Override
      public double getCacheHitRatio() {
        final long hits = getCacheHits();
        final long misses = getCacheMisses();
        final long total = hits + misses;
        if (total == 0) {
          return 0.0;
        }
        return (double) hits / total;
      }

      @Override
      public long getJitCompilations() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return 0;
        }
        return NATIVE_BINDINGS.profilerGetOptimizedModules(profiler);
      }

      @Override
      public Duration getJitCompilationTime() {
        // JIT time not tracked separately - return total compilation time
        return getTotalCompilationTime();
      }

      @Override
      public long getJitCodeSize() {
        // JIT code size not tracked separately - return bytes compiled
        return getBytesCompiled();
      }

      @Override
      public Instant getCaptureTime() {
        return captureTime;
      }

      @Override
      public Duration getUptime() {
        if (profiler == null || profiler.equals(MemorySegment.NULL)) {
          return Duration.between(created, captureTime);
        }
        final long nanos = NATIVE_BINDINGS.profilerGetUptimeNanos(profiler);
        return Duration.ofNanos(nanos);
      }

      @Override
      public void reset() {
        if (profiler != null && !profiler.equals(MemorySegment.NULL)) {
          NATIVE_BINDINGS.profilerReset(profiler);
        }
      }

      @Override
      public Map<String, Object> getExtendedStatistics() {
        return Collections.emptyMap();
      }
    };
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    try {
      // Stop and destroy profiler
      if (profilerHandle != null && !profilerHandle.equals(MemorySegment.NULL)) {
        NATIVE_BINDINGS.profilerStop(profilerHandle);
        NATIVE_BINDINGS.profilerDestroy(profilerHandle);
      }

      // Destroy native engine
      if (nativeEngine != null && !nativeEngine.equals(MemorySegment.NULL)) {
        NATIVE_BINDINGS.engineDestroy(nativeEngine);
      }
      arena.close();
      closed = true;
      LOGGER.fine("Closed Panama engine");
    } catch (final Exception e) {
      LOGGER.warning("Error closing engine: " + e.getMessage());
    }
  }

  /**
   * Gets the native engine pointer.
   *
   * @return native engine memory segment
   */
  public MemorySegment getNativeEngine() {
    return nativeEngine;
  }

  /**
   * Gets the unique identifier for this engine.
   *
   * @return the engine ID
   */
  public long getId() {
    return System.identityHashCode(this);
  }

  /**
   * Validates a component.
   *
   * @param component the component to validate
   * @return the validation result
   */
  public ComponentValidationResult validateComponent(final ComponentSimple component) {
    // TODO: Implement actual component validation
    final ComponentValidationResult.ValidationContext context =
        new ComponentValidationResult.ValidationContext("unknown", new ComponentVersion(1, 0, 0));
    return ComponentValidationResult.success(context);
  }

  /**
   * Gets the engine pointer.
   *
   * @return engine pointer
   */
  public MemorySegment getEnginePointer() {
    return nativeEngine;
  }

  /**
   * Gets the profiler handle for recording statistics.
   *
   * @return profiler memory segment, or null if not available
   */
  public MemorySegment getProfilerHandle() {
    return profilerHandle;
  }

  /**
   * Records a module compilation event.
   *
   * @param bytesCompiled the number of bytes compiled
   * @param compilationTimeNanos the compilation time in nanoseconds
   * @param cached whether this was a cached compilation
   * @param optimized whether optimization was applied
   */
  public void recordCompilation(
      final long bytesCompiled,
      final long compilationTimeNanos,
      final boolean cached,
      final boolean optimized) {
    if (profilerHandle != null && !profilerHandle.equals(MemorySegment.NULL)) {
      NATIVE_BINDINGS.profilerRecordCompilation(
          profilerHandle, compilationTimeNanos, bytesCompiled, cached, optimized);
    }
  }

  /**
   * Ensures the engine is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
  }

  /**
   * Retrieves the last error message from the native library and clears it.
   *
   * @return the error message, or null if no error
   */
  private static String retrieveNativeErrorMessage() {
    try {
      final MemorySegment errorPtr = NATIVE_BINDINGS.getLastErrorMessage();
      if (errorPtr == null || errorPtr.equals(MemorySegment.NULL)) {
        return null;
      }
      try {
        return errorPtr.reinterpret(Long.MAX_VALUE).getString(0);
      } finally {
        NATIVE_BINDINGS.freeErrorMessage(errorPtr);
      }
    } catch (final Exception e) {
      LOGGER.log(java.util.logging.Level.WARNING, "Failed to retrieve native error message", e);
      return null;
    }
  }

  @Override
  public boolean isPulley() {
    if (closed) {
      return false;
    }
    try {
      return NATIVE_BINDINGS.engineIsPulley(nativeEngine);
    } catch (final Exception e) {
      return false;
    }
  }

  @Override
  public byte[] precompileCompatibilityHash() {
    if (closed) {
      return new byte[0];
    }
    try {
      final byte[] hash = NATIVE_BINDINGS.enginePrecompileCompatibilityHash(nativeEngine);
      return hash != null ? hash : new byte[0];
    } catch (final Exception e) {
      return new byte[0];
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.Precompiled detectPrecompiled(final byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    if (bytes.length == 0) {
      return null;
    }
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }

    try (final java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
      final java.lang.foreign.MemorySegment bytesSegment = arena.allocate(bytes.length);
      bytesSegment.copyFrom(java.lang.foreign.MemorySegment.ofArray(bytes));
      final int result =
          NATIVE_BINDINGS.engineDetectPrecompiled(nativeEngine, bytesSegment, bytes.length);
      // -1 means not precompiled, 0 = MODULE, 1 = COMPONENT
      if (result < 0) {
        return null;
      }
      return ai.tegmentum.wasmtime4j.Precompiled.fromValue(result);
    }
  }

  @Override
  public boolean same(final ai.tegmentum.wasmtime4j.Engine other) {
    if (other == null) {
      throw new IllegalArgumentException("other cannot be null");
    }
    if (closed) {
      return false;
    }
    if (!(other instanceof PanamaEngine)) {
      return false;
    }
    final PanamaEngine otherEngine = (PanamaEngine) other;
    if (otherEngine.nativeEngine == null
        || otherEngine.nativeEngine.equals(java.lang.foreign.MemorySegment.NULL)) {
      return false;
    }
    // Compare native pointers for equality
    return this.nativeEngine.equals(otherEngine.nativeEngine);
  }

  @Override
  public boolean isAsync() {
    // Panama engines don't support async mode by default
    // This would require async_support feature in wasmtime
    return false;
  }
}

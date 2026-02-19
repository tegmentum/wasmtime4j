package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.util.StreamUtils;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Engine.
 *
 * @since 1.0.0
 */
public final class PanamaEngine implements Engine {
  private static final Logger LOGGER = Logger.getLogger(PanamaEngine.class.getName());
  private static final NativeEngineBindings NATIVE_BINDINGS = NativeEngineBindings.getInstance();

  private final Arena arena;
  private final MemorySegment nativeEngine;
  private final EngineConfig config;
  private final WasmRuntime runtime;
  private final NativeResourceHandle resourceHandle;

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

    final MemorySegment engineHandle = this.nativeEngine;
    final Arena engineArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaEngine",
            () -> {
              if (nativeEngine != null && !nativeEngine.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.engineDestroy(nativeEngine);
              }
              arena.close();
            },
            this,
            () -> {
              if (engineHandle != null && !engineHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.engineDestroy(engineHandle);
              }
              engineArena.close();
            });

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
      final String nativeError = PanamaErrorMapper.retrieveNativeErrorMessage();
      if (nativeError != null && !nativeError.isEmpty()) {
        throw new WasmException("Failed to compile WAT: " + nativeError);
      }
      throw PanamaErrorMapper.mapNativeError(result, "Failed to compile WAT");
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
    final byte[] wasmBytes = StreamUtils.readAllBytes(stream);

    if (wasmBytes.length == 0) {
      throw new WasmException("Stream contained no data");
    }

    return compileModule(wasmBytes);
  }


  @Override
  public EngineConfig getConfig() {
    return config;
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
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
  public void incrementEpoch() {
    ensureNotClosed();
    NATIVE_BINDINGS.engineIncrementEpoch(nativeEngine);
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  /**
   * Gets the native engine pointer.
   *
   * @return native engine memory segment
   * @throws IllegalStateException if the engine has been closed
   */
  public MemorySegment getNativeEngine() {
    resourceHandle.ensureNotClosed();
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
   * Ensures the engine is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }


  @Override
  public boolean isPulley() {
    if (resourceHandle.isClosed()) {
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
    if (resourceHandle.isClosed()) {
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
    ensureNotClosed();

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
    if (resourceHandle.isClosed()) {
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
    return NATIVE_BINDINGS.engineSame(this.nativeEngine, otherEngine.nativeEngine);
  }

  @Override
  public boolean isAsync() {
    if (resourceHandle.isClosed()) {
      return false;
    }
    return NATIVE_BINDINGS.engineIsAsync(nativeEngine);
  }
}

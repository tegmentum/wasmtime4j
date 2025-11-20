package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.ComponentVersion;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.exception.WasmException;
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
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final Arena arena;
  private final MemorySegment nativeEngine;
  private final EngineConfig config;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama engine with default configuration.
   *
   * @throws WasmException if engine creation fails
   */
  public PanamaEngine() throws WasmException {
    this(new EngineConfig());
  }

  /**
   * Creates a new Panama engine with specified configuration.
   *
   * @param config the engine configuration
   * @throws WasmException if engine creation fails
   */
  public PanamaEngine(final EngineConfig config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }
    this.config = config;
    this.arena = Arena.ofShared();

    // Create native engine via Panama FFI
    this.nativeEngine = NATIVE_BINDINGS.engineCreate();

    if (this.nativeEngine == null || this.nativeEngine.equals(MemorySegment.NULL)) {
      arena.close();
      throw new WasmException("Failed to create native engine");
    }

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
  public void close() {
    if (closed) {
      return;
    }

    try {
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
   * Ensures the engine is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
  }
}

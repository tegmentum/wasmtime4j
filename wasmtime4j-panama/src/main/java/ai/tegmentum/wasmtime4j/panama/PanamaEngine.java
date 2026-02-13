package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.component.ComponentVersion;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.ByteArrayOutputStream;
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
  private static final NativeMemoryBindings MEMORY_BINDINGS = NativeMemoryBindings.getInstance();

  private final Arena arena;
  private final MemorySegment nativeEngine;
  private final EngineConfig config;
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
  public void close() {
    if (closed) {
      return;
    }
    closed = true;

    try {
      // Destroy native engine
      if (nativeEngine != null && !nativeEngine.equals(MemorySegment.NULL)) {
        NATIVE_BINDINGS.engineDestroy(nativeEngine);
      }
      arena.close();
      LOGGER.fine("Closed Panama engine");
    } catch (final Exception e) {
      LOGGER.warning("Error closing engine: " + e.getMessage());
    }
  }

  /**
   * Gets the native engine pointer.
   *
   * @return native engine memory segment
   * @throws IllegalStateException if the engine has been closed
   */
  public MemorySegment getNativeEngine() {
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
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
  public ComponentValidationResult validateComponent(final Component component) {
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

  /**
   * Retrieves the last error message from the native library and clears it.
   *
   * @return the error message, or null if no error
   */
  private static String retrieveNativeErrorMessage() {
    try {
      final MemorySegment errorPtr = MEMORY_BINDINGS.getLastErrorMessage();
      if (errorPtr == null || errorPtr.equals(MemorySegment.NULL)) {
        return null;
      }
      try {
        return errorPtr.reinterpret(Long.MAX_VALUE).getString(0);
      } finally {
        MEMORY_BINDINGS.freeErrorMessage(errorPtr);
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

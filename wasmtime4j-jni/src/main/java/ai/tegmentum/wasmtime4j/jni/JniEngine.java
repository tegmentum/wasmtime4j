package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.util.StreamUtils;
import java.io.IOException;
import java.io.InputStream;

/**
 * JNI implementation of the Engine interface.
 *
 * <p>Extends {@link JniResource} for thread-safe lifecycle management and automatic cleanup via
 * phantom references.
 *
 * @since 1.0.0
 */
public class JniEngine extends JniResource implements Engine {
  private final WasmRuntime runtime;
  private final EngineConfig config;

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Creates a new JNI engine with the given native handle and runtime.
   *
   * @param nativeHandle the native handle (must be non-zero)
   * @param runtime the runtime that created this engine
   */
  public JniEngine(final long nativeHandle, final WasmRuntime runtime) {
    this(nativeHandle, runtime, null);
  }

  /**
   * Creates a new JNI engine with the given native handle.
   *
   * <p>This constructor is intended for unit tests that don't need a runtime reference. Production
   * code should use {@link #JniEngine(long, WasmRuntime)}.
   *
   * @param nativeHandle the native handle (must be non-zero)
   */
  public JniEngine(final long nativeHandle) {
    this(nativeHandle, null, null);
  }

  /**
   * Creates a new JNI engine with the given native handle, runtime, and configuration.
   *
   * @param nativeHandle the native handle (must be non-zero)
   * @param runtime the runtime that created this engine
   * @param config the engine configuration used to create this engine, or null for default
   */
  JniEngine(final long nativeHandle, final WasmRuntime runtime, final EngineConfig config) {
    super(nativeHandle);
    this.runtime = runtime;
    this.config = config;
  }

  @Override
  public WasmRuntime getRuntime() {
    return runtime;
  }

  @Override
  public Store createStore() throws WasmException {
    ensureNotClosed();

    final long storeHandle = nativeCreateStore(nativeHandle);
    if (storeHandle == 0) {
      throw new WasmException("Failed to create store");
    }
    return new JniStore(storeHandle, this);
  }

  @Override
  public Store createStore(final Object data) throws WasmException {
    final Store store = createStore();
    store.setData(data);
    return store;
  }

  @Override
  public WasmMemory createSharedMemory(final int initialPages, final int maxPages)
      throws WasmException {
    if (initialPages < 0) {
      throw new IllegalArgumentException("Initial pages cannot be negative: " + initialPages);
    }
    if (maxPages < 1) {
      throw new IllegalArgumentException(
          "Shared memory requires a positive maximum page count");
    }
    if (maxPages < initialPages) {
      throw new IllegalArgumentException(
          "Max pages (" + maxPages + ") cannot be less than initial pages (" + initialPages + ")");
    }
    ensureNotClosed();

    final long memoryHandle = nativeCreateSharedMemory(nativeHandle, initialPages, maxPages);
    if (memoryHandle == 0) {
      throw new WasmException("Failed to create shared memory");
    }
    return new JniMemory(memoryHandle, null);
  }

  @Override
  public boolean isEpochInterruptionEnabled() {
    if (isClosed()) {
      return false;
    }
    return nativeIsEpochInterruptionEnabled(nativeHandle);
  }

  @Override
  public boolean isCoredumpOnTrapEnabled() {
    if (isClosed()) {
      return false;
    }
    return nativeIsCoredumpOnTrapEnabled(nativeHandle);
  }

  @Override
  public boolean isFuelEnabled() {
    if (isClosed()) {
      return false;
    }
    return nativeIsFuelEnabled(nativeHandle);
  }

  @Override
  public long getStackSizeLimit() {
    if (isClosed()) {
      return 0;
    }
    return nativeGetStackSizeLimit(nativeHandle);
  }

  @Override
  public int getMemoryLimitPages() {
    if (isClosed()) {
      return 0;
    }
    return nativeGetMemoryLimitPages(nativeHandle);
  }

  @Override
  public boolean supportsFeature(final ai.tegmentum.wasmtime4j.WasmFeature feature) {
    if (feature == null) {
      return false;
    }
    if (isClosed()) {
      return false;
    }
    return nativeSupportsFeature(nativeHandle, feature.name());
  }

  @Override
  public boolean isValid() {
    return !isClosed();
  }

  @Override
  public void incrementEpoch() {
    ensureNotClosed();
    nativeIncrementEpoch(nativeHandle);
  }

  @Override
  public Module compileModule(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null) {
      throw new IllegalArgumentException("wasmBytes cannot be null");
    }
    if (wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be empty");
    }
    ensureNotClosed();

    final long moduleHandle = nativeCompileModule(nativeHandle, wasmBytes);
    if (moduleHandle == 0) {
      throw new WasmException("Failed to compile module from bytes");
    }
    return new JniModule(moduleHandle, this);
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

    final long moduleHandle = nativeCompileWat(nativeHandle, wat);
    if (moduleHandle == 0) {
      throw new WasmException("Failed to compile WAT");
    }
    return new JniModule(moduleHandle, this);
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

    final byte[] precompiled = nativePrecompileModule(nativeHandle, wasmBytes);
    if (precompiled == null || precompiled.length == 0) {
      throw new WasmException("Failed to precompile module");
    }
    return precompiled;
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
  public Module compileFromFile(final java.nio.file.Path path) throws WasmException {
    if (path == null) {
      throw new IllegalArgumentException("path cannot be null");
    }
    ensureNotClosed();

    final long moduleHandle = JniModule.nativeCompileFromFile(nativeHandle, path.toString());
    if (moduleHandle == 0) {
      throw new WasmException("Failed to compile module from file: " + path);
    }
    return new JniModule(moduleHandle, this);
  }

  @Override
  public byte[] precompileComponent(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null) {
      throw new IllegalArgumentException("wasmBytes cannot be null");
    }
    if (wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be empty");
    }
    ensureNotClosed();

    final byte[] precompiled = nativePrecompileComponent(nativeHandle, wasmBytes);
    if (precompiled == null || precompiled.length == 0) {
      throw new WasmException("Failed to precompile component");
    }
    return precompiled;
  }

  @Override
  public ai.tegmentum.wasmtime4j.pool.PoolStatistics getPoolingAllocatorMetrics() {
    if (isClosed()) {
      return null;
    }
    final long[] metrics = nativeGetPoolingAllocatorMetrics(nativeHandle);
    if (metrics == null) {
      return null;
    }
    return new ai.tegmentum.wasmtime4j.jni.pool.JniPoolStatistics(metrics);
  }

  private native long nativeCompileModule(long engineHandle, byte[] wasmBytes);

  private native long nativeCompileWat(long engineHandle, String wat);

  private native byte[] nativePrecompileModule(long engineHandle, byte[] wasmBytes);

  private native byte[] nativePrecompileComponent(long engineHandle, byte[] wasmBytes);

  private native long[] nativeGetPoolingAllocatorMetrics(long engineHandle);

  @Override
  public EngineConfig getConfig() {
    return config != null ? config : new EngineConfig();
  }

  @Override
  protected void doClose() throws Exception {
    if (nativeHandle != 0) {
      nativeDestroyEngine(nativeHandle);
    }
  }

  @Override
  protected String getResourceType() {
    return "JniEngine";
  }

  private native long nativeCreateStore(long engineHandle);

  private native long nativeCreateSharedMemory(long engineHandle, int initialPages, int maxPages);

  private native void nativeDestroyEngine(long handle);

  private native boolean nativeIsEpochInterruptionEnabled(long engineHandle);

  private native boolean nativeIsCoredumpOnTrapEnabled(long engineHandle);

  private native boolean nativeIsFuelEnabled(long engineHandle);

  private native long nativeGetStackSizeLimit(long engineHandle);

  private native int nativeGetMemoryLimitPages(long engineHandle);

  private native boolean nativeSupportsFeature(long engineHandle, String featureName);

  private static native boolean nativeDetectHostFeature(String featureName);

  @Override
  public java.util.Optional<Boolean> detectHostFeature(final String feature) {
    if (feature == null) {
      throw new IllegalArgumentException("feature cannot be null");
    }
    return java.util.Optional.of(nativeDetectHostFeature(feature));
  }

  @Override
  public boolean isPulley() {
    if (isClosed()) {
      return false;
    }
    try {
      return nativeIsPulley(nativeHandle);
    } catch (final Exception e) {
      return false;
    }
  }

  private native boolean nativeIsPulley(long engineHandle);

  @Override
  public byte[] precompileCompatibilityHash() {
    if (isClosed()) {
      return new byte[0];
    }
    try {
      final byte[] hash = nativePrecompileCompatibilityHash(nativeHandle);
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

    final int result = nativeDetectPrecompiled(nativeHandle, bytes);
    // -1 means not precompiled, 0 = MODULE, 1 = COMPONENT
    if (result < 0) {
      return null;
    }
    return ai.tegmentum.wasmtime4j.Precompiled.fromValue(result);
  }

  private native byte[] nativePrecompileCompatibilityHash(long engineHandle);

  private native int nativeDetectPrecompiled(long engineHandle, byte[] bytes);

  @Override
  public boolean same(final ai.tegmentum.wasmtime4j.Engine other) {
    if (other == null) {
      throw new IllegalArgumentException("other cannot be null");
    }
    if (isClosed()) {
      return false;
    }
    if (!(other instanceof JniEngine)) {
      return false;
    }
    final JniEngine otherEngine = (JniEngine) other;
    return nativeEngineSame(this.nativeHandle, otherEngine.nativeHandle);
  }

  @Override
  public boolean isAsync() {
    if (isClosed()) {
      return false;
    }
    return nativeIsAsync(nativeHandle);
  }

  @Override
  public ai.tegmentum.wasmtime4j.debug.GuestProfiler createGuestProfiler(
      final String moduleName,
      final java.time.Duration interval,
      final java.util.Map<String, ai.tegmentum.wasmtime4j.Module> modules)
      throws WasmException {
    if (moduleName == null || moduleName.isEmpty()) {
      throw new IllegalArgumentException("moduleName cannot be null or empty");
    }
    if (interval == null) {
      throw new IllegalArgumentException("interval cannot be null");
    }
    if (modules == null || modules.isEmpty()) {
      throw new IllegalArgumentException("modules cannot be null or empty");
    }
    ensureNotClosed();

    return new JniGuestProfiler(nativeHandle, moduleName, interval.toNanos(), modules);
  }

  @Override
  public ai.tegmentum.wasmtime4j.WeakEngine weak() {
    ensureNotClosed();
    final long weakHandle = nativeCreateWeakEngine(nativeHandle);
    if (weakHandle == 0) {
      throw new ai.tegmentum.wasmtime4j.jni.exception.JniResourceException(
          "Failed to create weak engine reference");
    }
    return new JniWeakEngine(weakHandle, this);
  }

  private native boolean nativeEngineSame(long handle1, long handle2);

  private native boolean nativeIsAsync(long engineHandle);

  private native void nativeIncrementEpoch(long engineHandle);

  private static native long nativeCreateWeakEngine(long engineHandle);

  /**
   * Creates a new engine with the specified configuration.
   *
   * @param config the engine configuration
   * @param runtime the runtime that owns this engine
   * @return a new JniEngine instance
   * @throws WasmException if the engine cannot be created
   */
  public static JniEngine createWithConfig(final EngineConfig config, final WasmRuntime runtime)
      throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("config cannot be null");
    }

    final byte[] jsonBytes = config.toJson();
    final long handle = nativeCreateEngineFromJsonConfig(jsonBytes);

    if (handle == 0) {
      throw new WasmException("Failed to create engine with configuration");
    }

    return new JniEngine(handle, runtime, config);
  }

  private static native long nativeCreateEngineFromJsonConfig(byte[] jsonConfig);

  /**
   * Clears the native handle registries used for memory and store validation.
   *
   * <p>This method should be called during test cleanup to prevent stale handles from causing
   * validation failures in subsequent tests. It clears the global registries that track valid
   * memory and store handles.
   *
   * <p>Warning: This should only be used in test contexts. Calling this while handles are still in
   * use will cause subsequent operations on those handles to fail.
   *
   * @return 0 on success, -1 on failure
   */
  public static int clearHandleRegistries() {
    return nativeClearHandleRegistries();
  }

  private static native int nativeClearHandleRegistries();
}

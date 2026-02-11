package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * JNI implementation of the Engine interface.
 *
 * @since 1.0.0
 */
public class JniEngine implements Engine {
  private final long nativeHandle;
  private final WasmRuntime runtime;
  private volatile boolean closed = false;

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
   * @param nativeHandle the native handle
   * @param runtime the runtime that created this engine
   */
  public JniEngine(final long nativeHandle, final WasmRuntime runtime) {
    this.nativeHandle = nativeHandle;
    this.runtime = runtime;
  }

  /**
   * Creates a new JNI engine with the given native handle.
   *
   * <p>This constructor is intended for unit tests that don't need a runtime reference. Production
   * code should use {@link #JniEngine(long, WasmRuntime)}.
   *
   * @param nativeHandle the native handle
   */
  public JniEngine(final long nativeHandle) {
    this(nativeHandle, null);
  }

  /**
   * Gets the native handle.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  @Override
  public WasmRuntime getRuntime() {
    return runtime;
  }

  @Override
  public Store createStore() throws WasmException {
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
    if (nativeHandle == 0) {
      throw new IllegalStateException("Engine has invalid native handle");
    }

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
  public boolean isEpochInterruptionEnabled() {
    if (closed || nativeHandle == 0) {
      return false;
    }
    return nativeIsEpochInterruptionEnabled(nativeHandle);
  }

  @Override
  public boolean isCoredumpOnTrapEnabled() {
    if (closed || nativeHandle == 0) {
      return false;
    }
    return nativeIsCoredumpOnTrapEnabled(nativeHandle);
  }

  @Override
  public boolean isFuelEnabled() {
    if (closed || nativeHandle == 0) {
      return false;
    }
    return nativeIsFuelEnabled(nativeHandle);
  }

  @Override
  public long getStackSizeLimit() {
    if (closed || nativeHandle == 0) {
      return 0;
    }
    return nativeGetStackSizeLimit(nativeHandle);
  }

  @Override
  public int getMemoryLimitPages() {
    if (closed || nativeHandle == 0) {
      return 0;
    }
    return nativeGetMemoryLimitPages(nativeHandle);
  }

  @Override
  public boolean supportsFeature(final ai.tegmentum.wasmtime4j.WasmFeature feature) {
    if (feature == null) {
      return false;
    }
    if (closed || nativeHandle == 0) {
      return false;
    }
    return nativeSupportsFeature(nativeHandle, feature.name());
  }

  @Override
  public boolean isValid() {
    return !closed && nativeHandle != 0;
  }

  @Override
  public void incrementEpoch() {
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
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
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
    if (nativeHandle == 0) {
      throw new IllegalStateException("Engine has invalid native handle");
    }

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
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
    if (nativeHandle == 0) {
      throw new IllegalStateException("Engine has invalid native handle");
    }

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
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
    if (nativeHandle == 0) {
      throw new IllegalStateException("Engine has invalid native handle");
    }

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
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }
    if (nativeHandle == 0) {
      throw new IllegalStateException("Engine has invalid native handle");
    }

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

  private native long nativeCompileModule(long engineHandle, byte[] wasmBytes);

  private native long nativeCompileWat(long engineHandle, String wat);

  private native byte[] nativePrecompileModule(long engineHandle, byte[] wasmBytes);

  @Override
  public EngineConfig getConfig() {
    // TODO: Implement config retrieval
    return null;
  }

  @Override
  public long getReferenceCount() {
    // TODO: Implement reference counting
    return 1;
  }

  @Override
  public int getMaxInstances() {
    // TODO: Implement max instances tracking
    return Integer.MAX_VALUE;
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      nativeDestroyEngine(nativeHandle);
    }
  }

  private native long nativeCreateStore(long engineHandle);

  private native void nativeDestroyEngine(long handle);

  private native boolean nativeIsEpochInterruptionEnabled(long engineHandle);

  private native boolean nativeIsCoredumpOnTrapEnabled(long engineHandle);

  private native boolean nativeIsFuelEnabled(long engineHandle);

  private native long nativeGetStackSizeLimit(long engineHandle);

  private native int nativeGetMemoryLimitPages(long engineHandle);

  private native boolean nativeSupportsFeature(long engineHandle, String featureName);

  @Override
  public boolean isPulley() {
    if (closed) {
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
    if (closed) {
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
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }

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
    if (closed) {
      return false;
    }
    if (!(other instanceof JniEngine)) {
      return false;
    }
    final JniEngine otherEngine = (JniEngine) other;
    if (otherEngine.nativeHandle == 0 || this.nativeHandle == 0) {
      return false;
    }
    return nativeEngineSame(this.nativeHandle, otherEngine.nativeHandle);
  }

  @Override
  public boolean isAsync() {
    if (closed || nativeHandle == 0) {
      return false;
    }
    return nativeIsAsync(nativeHandle);
  }

  private native boolean nativeEngineSame(long handle1, long handle2);

  private native boolean nativeIsAsync(long engineHandle);

  private native void nativeIncrementEpoch(long engineHandle);

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

    // Map optimization level to native value
    final int optLevel;
    switch (config.getOptimizationLevel()) {
      case NONE:
        optLevel = 0;
        break;
      case SPEED:
        optLevel = 1;
        break;
      case SPEED_AND_SIZE:
        optLevel = 2;
        break;
      default:
        optLevel = 1; // Default to SPEED
    }

    // Convert max memory from bytes to pages (1 page = 64KB)
    // Use 0 to indicate default if not explicitly configured
    final long maxMemoryBytes = config.getMaxMemoryPerInstance();
    final int maxMemoryPages = maxMemoryBytes > 0 ? (int) (maxMemoryBytes / 65536L) : 0;

    final long handle =
        nativeCreateEngineWithExtendedConfig(
            0, // strategy (0 = auto/cranelift)
            optLevel,
            config.isDebugInfo(),
            config.isWasmThreads(),
            config.isWasmSimd(),
            config.isWasmReferenceTypes(),
            config.isWasmBulkMemory(),
            config.isWasmMultiValue(),
            config.isConsumeFuel(),
            maxMemoryPages,
            (int) config.getMaxWasmStack(),
            config.isEpochInterruption(),
            config.getInstancePoolSize(),
            config.isAsyncSupport(),
            // GC configuration
            config.isWasmGc(),
            config.isWasmFunctionReferences(),
            config.isWasmExceptions(),
            // Memory configuration
            config.getMemoryReservation(),
            config.getMemoryGuardSize(),
            config.getMemoryReservationForGrowth(),
            // Additional features
            config.isWasmTailCall(),
            config.isWasmRelaxedSimd(),
            config.isWasmMultiMemory(),
            config.isWasmMemory64(),
            config.isWasmExtendedConstExpressions(),
            false, // wasmComponentModel - handled separately
            config.isCoredumpOnTrap(),
            config.isCraneliftNanCanonicalization(),
            // Experimental features
            config.isWasmCustomPageSizes(),
            config.isWasmWideArithmetic());

    if (handle == 0) {
      throw new WasmException("Failed to create engine with configuration");
    }

    return new JniEngine(handle, runtime);
  }

  private static native long nativeCreateEngineWithConfig(
      int strategy,
      int optLevel,
      boolean debugInfo,
      boolean wasmThreads,
      boolean wasmSimd,
      boolean wasmReferenceTypes,
      boolean wasmBulkMemory,
      boolean wasmMultiValue,
      boolean fuelEnabled,
      int maxMemoryPages,
      int maxStackSize,
      boolean epochInterruption,
      int maxInstances,
      boolean asyncSupport);

  private static native long nativeCreateEngineWithExtendedConfig(
      int strategy,
      int optLevel,
      boolean debugInfo,
      boolean wasmThreads,
      boolean wasmSimd,
      boolean wasmReferenceTypes,
      boolean wasmBulkMemory,
      boolean wasmMultiValue,
      boolean fuelEnabled,
      int maxMemoryPages,
      int maxStackSize,
      boolean epochInterruption,
      int maxInstances,
      boolean asyncSupport,
      boolean wasmGc,
      boolean wasmFunctionReferences,
      boolean wasmExceptions,
      long memoryReservation,
      long memoryGuardSize,
      long memoryReservationForGrowth,
      boolean wasmTailCall,
      boolean wasmRelaxedSimd,
      boolean wasmMultiMemory,
      boolean wasmMemory64,
      boolean wasmExtendedConst,
      boolean wasmComponentModel,
      boolean coredumpOnTrap,
      boolean craneliftNanCanonicalization,
      boolean wasmCustomPageSizes,
      boolean wasmWideArithmetic);

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

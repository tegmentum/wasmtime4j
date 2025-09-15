package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeMethodBindings;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.logging.Logger;

/**
 * JNI implementation of the WebAssembly Engine.
 *
 * <p>This class manages the configuration and lifecycle of a WebAssembly execution engine through
 * JNI calls to the native Wasmtime library. The engine controls compilation settings, optimization
 * levels, and resource management for WebAssembly modules.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic resource management with {@link AutoCloseable}
 *   <li>Defensive programming to prevent JVM crashes
 *   <li>Comprehensive parameter validation
 *   <li>Thread-safe operations
 *   <li>Configuration management for optimization and debug settings
 * </ul>
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * try (JniEngine engine = JniEngine.create()) {
 *   engine.setOptimizationLevel(2); // Optimize for speed and size
 *   engine.setDebugInfo(true);      // Enable debug information
 *
 *   // Compile and instantiate modules using this engine
 *   JniModule module = engine.compileModule(wasmBytes);
 *   JniInstance instance = engine.instantiate(module, store);
 * }
 * }</pre>
 *
 * <p>This implementation extends {@link JniResource} to provide automatic native resource
 * management and follows defensive programming practices to prevent native crashes.
 *
 * @since 1.0.0
 */
public final class JniEngine extends JniResource implements Engine {

  private static final Logger LOGGER = Logger.getLogger(JniEngine.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniEngine: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Creates a new JNI engine with the given native handle.
   *
   * <p>This constructor is package-private and should only be used by factory methods or other JNI
   * classes. External code should use {@link #create()} or similar factory methods.
   *
   * @param nativeHandle the native engine handle from Wasmtime
   * @throws JniResourceException if nativeHandle is invalid
   */
  JniEngine(final long nativeHandle) {
    super(nativeHandle);
    LOGGER.fine("Created JNI engine with handle: 0x" + Long.toHexString(nativeHandle));
  }

  /**
   * Creates a new default engine with standard configuration.
   *
   * <p>This factory method creates an engine with default Wasmtime settings suitable for most use
   * cases. For custom configuration, use {@link #createWithConfig(EngineConfig)}.
   *
   * @return a new engine instance
   * @throws JniException if engine creation fails
   */
  public static JniEngine create() {
    NativeMethodBindings.ensureInitialized();

    try {
      final long engineHandle = nativeCreateEngine();
      JniValidation.requireValidHandle(engineHandle, "engineHandle");
      return new JniEngine(engineHandle);
    } catch (final Exception e) {
      throw new JniException("Failed to create Wasmtime engine", e);
    }
  }

  /**
   * Compiles WebAssembly bytecode into a module using this engine.
   *
   * <p>This method validates and compiles the provided WebAssembly bytecode into a module that can
   * be instantiated and executed. The compilation process includes validation of the WebAssembly
   * format and optimization based on the engine's configuration.
   *
   * @param wasmBytes the WebAssembly bytecode to compile
   * @return a compiled module
   * @throws JniException if compilation fails
   * @throws JniResourceException if this engine has been closed
   */
  @Override
  public Module compileModule(final byte[] wasmBytes) throws WasmException {
    JniValidation.requireNonEmpty(wasmBytes, "wasmBytes");
    ensureNotClosed();

    final byte[] wasmBytesCopy = JniValidation.defensiveCopy(wasmBytes);

    // Start performance monitoring for compilation
    final long startTime =
        ai.tegmentum.wasmtime4j.jni.performance.PerformanceMonitor.startOperation(
            "module_compilation", "compile");

    try {
      // Try to load from compilation cache first
      final String engineOptions = getEngineOptionsString();
      final byte[] cachedModule =
          ai.tegmentum.wasmtime4j.jni.performance.CompilationCache.loadFromCache(
              wasmBytesCopy, engineOptions);

      if (cachedModule != null) {
        // Cache hit - use cached compilation (this is simulated for now)
        LOGGER.fine("Using cached compilation for module");
      }

      // Compile the module (either fresh or validate cached)
      final long moduleHandle = nativeCompileModule(getNativeHandle(), wasmBytesCopy);
      JniValidation.requireValidHandle(moduleHandle, "moduleHandle");

      // Store compilation result in cache for future use
      if (cachedModule == null) {
        // This is a simplified implementation - in reality we'd store the actual compiled module
        // data
        ai.tegmentum.wasmtime4j.jni.performance.CompilationCache.storeInCache(
            wasmBytesCopy, wasmBytesCopy, engineOptions);
      }

      return new JniModule(moduleHandle, this);
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw new WasmException(e.getMessage(), e);
      }
      throw new WasmException("Failed to compile WebAssembly module", e);
    } finally {
      ai.tegmentum.wasmtime4j.jni.performance.PerformanceMonitor.endOperation(
          "module_compilation", startTime);
    }
  }

  /**
   * Gets a string representation of current engine options for cache keys.
   *
   * @return engine options string
   */
  private String getEngineOptionsString() {
    try {
      return "opt_" + getOptimizationLevel() + "_debug_" + isDebugInfo();
    } catch (final Exception e) {
      return "default";
    }
  }

  /**
   * Creates a new store associated with this engine.
   *
   * <p>A store represents an execution context for WebAssembly instances. All instances created
   * from modules compiled with this engine must use a store from the same engine.
   *
   * @return a new store instance
   * @throws JniException if store creation fails
   * @throws JniResourceException if this engine has been closed
   */
  @Override
  public Store createStore() {
    ensureNotClosed();

    try {
      final long storeHandle = nativeCreateStore(getNativeHandle());
      JniValidation.requireValidHandle(storeHandle, "storeHandle");
      return new JniStore(storeHandle, this);
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Failed to create store", e);
    }
  }

  @Override
  public Store createStore(final Object data) {
    final Store store = createStore();
    if (data != null) {
      store.setData(data);
    }
    return store;
  }

  /**
   * Creates a new store with custom configuration.
   *
   * <p>This method allows fine-grained control over store behavior including resource limits,
   * execution timeouts, and other advanced settings.
   *
   * @param fuelLimit the fuel limit (0 = no limit)
   * @param memoryLimitBytes the memory limit in bytes (0 = no limit)
   * @param executionTimeoutSecs the execution timeout in seconds (0 = no timeout)
   * @param maxInstances the maximum number of instances (0 = no limit)
   * @param maxTableElements the maximum table elements (0 = no limit)
   * @param maxFunctions the maximum functions (0 = no limit)
   * @return a new configured store instance
   * @throws JniException if store creation fails
   * @throws JniResourceException if this engine has been closed
   */
  public Store createStoreWithConfig(
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSecs,
      final int maxInstances,
      final int maxTableElements,
      final int maxFunctions) {
    ensureNotClosed();

    try {
      final long storeHandle =
          nativeCreateStoreWithConfig(
              getNativeHandle(),
              fuelLimit,
              memoryLimitBytes,
              executionTimeoutSecs,
              maxInstances,
              maxTableElements,
              maxFunctions);
      JniValidation.requireValidHandle(storeHandle, "storeHandle");
      return new JniStore(storeHandle, this);
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Failed to create store with configuration", e);
    }
  }

  /**
   * Sets the optimization level for this engine.
   *
   * <p>This method controls how aggressively the Wasmtime engine optimizes compiled WebAssembly
   * code. Higher optimization levels produce faster code but may increase compilation time.
   *
   * @param level the optimization level:
   *     <ul>
   *       <li>0 = No optimization (fastest compilation)
   *       <li>1 = Optimize for speed
   *       <li>2 = Optimize for both speed and size
   *     </ul>
   *
   * @throws JniException if the configuration cannot be changed
   * @throws JniResourceException if this engine has been closed
   */
  public void setOptimizationLevel(final int level) {
    JniValidation.requireInRange(level, 0, 2, "level");
    ensureNotClosed();

    try {
      final boolean success = nativeSetOptimizationLevel(getNativeHandle(), level);
      if (!success) {
        throw new JniException("Failed to set optimization level to " + level);
      }
      LOGGER.fine(
          "Set optimization level to "
              + level
              + " for engine 0x"
              + Long.toHexString(getNativeHandle()));
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Unexpected error setting optimization level", e);
    }
  }

  /**
   * Gets the current optimization level for this engine.
   *
   * @return the optimization level (0-2)
   * @throws JniException if the configuration cannot be retrieved
   * @throws JniResourceException if this engine has been closed
   */
  public int getOptimizationLevel() {
    ensureNotClosed();

    try {
      return nativeGetOptimizationLevel(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get optimization level", e);
    }
  }

  /**
   * Enables or disables debug information generation.
   *
   * <p>When enabled, the engine will generate additional debug information during compilation which
   * can be useful for debugging WebAssembly modules but may increase compilation time and memory
   * usage.
   *
   * @param enabled true to enable debug information, false to disable
   * @throws JniException if the configuration cannot be changed
   * @throws JniResourceException if this engine has been closed
   */
  public void setDebugInfo(final boolean enabled) {
    ensureNotClosed();

    try {
      final boolean success = nativeSetDebugInfo(getNativeHandle(), enabled);
      if (!success) {
        throw new JniException(
            "Failed to " + (enabled ? "enable" : "disable") + " debug information");
      }
      LOGGER.fine(
          (enabled ? "Enabled" : "Disabled")
              + " debug info for engine 0x"
              + Long.toHexString(getNativeHandle()));
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Unexpected error setting debug info", e);
    }
  }

  /**
   * Checks if debug information generation is enabled.
   *
   * @return true if debug information is enabled, false otherwise
   * @throws JniException if the configuration cannot be retrieved
   * @throws JniResourceException if this engine has been closed
   */
  public boolean isDebugInfo() {
    ensureNotClosed();

    try {
      return nativeIsDebugInfo(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get debug info configuration", e);
    }
  }

  @Override
  protected void doClose() throws Exception {
    if (getNativeHandle() != 0) {
      nativeDestroyEngine(getNativeHandle());
      LOGGER.fine("Destroyed JNI engine with handle: 0x" + Long.toHexString(getNativeHandle()));
    }
  }

  @Override
  protected String getResourceType() {
    return "Engine";
  }

  @Override
  public boolean isValid() {
    return !isClosed() && getNativeHandle() != 0;
  }

  @Override
  public EngineConfig getConfig() {
    // TODO: This should return the actual configuration used to create this engine
    // For now, return null as a placeholder - this needs proper implementation
    throw new UnsupportedOperationException("getConfig() not yet implemented");
  }

  // Native method declarations

  /**
   * Creates a new native Wasmtime engine with default configuration.
   *
   * @return native engine handle or 0 on failure
   */
  private static native long nativeCreateEngine();

  /**
   * Compiles WebAssembly bytecode into a module using the specified engine.
   *
   * @param engineHandle the native engine handle
   * @param wasmBytes the WebAssembly bytecode
   * @return native module handle or 0 on failure
   */
  private static native long nativeCompileModule(long engineHandle, byte[] wasmBytes);

  /**
   * Creates a new store associated with the specified engine.
   *
   * @param engineHandle the native engine handle
   * @return native store handle or 0 on failure
   */
  private static native long nativeCreateStore(long engineHandle);

  /**
   * Creates a new store with custom configuration.
   *
   * @param engineHandle the native engine handle
   * @param fuelLimit the fuel limit (0 = no limit)
   * @param memoryLimitBytes the memory limit in bytes (0 = no limit)
   * @param executionTimeoutSecs the execution timeout in seconds (0 = no timeout)
   * @param maxInstances the maximum number of instances (0 = no limit)
   * @param maxTableElements the maximum table elements (0 = no limit)
   * @param maxFunctions the maximum functions (0 = no limit)
   * @return native store handle or 0 on failure
   */
  private static native long nativeCreateStoreWithConfig(
      long engineHandle,
      long fuelLimit,
      long memoryLimitBytes,
      long executionTimeoutSecs,
      int maxInstances,
      int maxTableElements,
      int maxFunctions);

  /**
   * Sets the optimization level for an engine.
   *
   * @param engineHandle the native engine handle
   * @param level the optimization level (0-2)
   * @return true on success, false on failure
   */
  private static native boolean nativeSetOptimizationLevel(long engineHandle, int level);

  /**
   * Gets the optimization level for an engine.
   *
   * @param engineHandle the native engine handle
   * @return the optimization level (0-2)
   */
  private static native int nativeGetOptimizationLevel(long engineHandle);

  /**
   * Enables or disables debug information generation.
   *
   * @param engineHandle the native engine handle
   * @param enabled true to enable debug information
   * @return true on success, false on failure
   */
  private static native boolean nativeSetDebugInfo(long engineHandle, boolean enabled);

  /**
   * Checks if debug information generation is enabled.
   *
   * @param engineHandle the native engine handle
   * @return true if debug information is enabled
   */
  private static native boolean nativeIsDebugInfo(long engineHandle);

  /**
   * Destroys a native engine and releases all associated resources.
   *
   * @param engineHandle the native engine handle
   */
  private static native void nativeDestroyEngine(long engineHandle);
}

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.StreamingCompiler;
import ai.tegmentum.wasmtime4j.WasmFeature;
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

  /**
   * Checks if the engine supports a specific WebAssembly feature.
   *
   * <p>This method allows checking for feature support such as threads, SIMD, reference types, and
   * other WebAssembly proposals.
   *
   * @param feature the WebAssembly feature to check
   * @return true if the feature is supported, false otherwise
   * @throws IllegalArgumentException if feature is null
   * @since 1.0.0
   */
  @Override
  public boolean supportsFeature(final WasmFeature feature) {
    JniValidation.requireNonNull(feature, "feature");
    ensureNotClosed();

    try {
      return nativeSupportsFeature(getNativeHandle(), feature.ordinal());
    } catch (final Exception e) {
      LOGGER.warning("Failed to check feature support for " + feature + ": " + e.getMessage());
      return false;
    }
  }

  /**
   * Gets the memory limit in pages for this engine.
   *
   * <p>Returns the maximum number of WebAssembly pages (64KB each) that can be allocated for linear
   * memory in this engine. A value of 0 indicates no limit.
   *
   * @return the memory limit in pages, or 0 for unlimited
   * @since 1.0.0
   */
  @Override
  public int getMemoryLimitPages() {
    ensureNotClosed();

    try {
      return (int) nativeGetMemoryLimitPages(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get memory limit pages", e);
    }
  }

  /**
   * Gets the stack size limit for this engine.
   *
   * <p>Returns the maximum stack size in bytes for WebAssembly execution. A value of 0 indicates
   * the default stack size is used.
   *
   * @return the stack size limit in bytes, or 0 for default
   * @since 1.0.0
   */
  @Override
  public long getStackSizeLimit() {
    ensureNotClosed();

    try {
      return nativeGetStackSizeLimit(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get stack size limit", e);
    }
  }

  /**
   * Checks if fuel consumption is enabled for this engine.
   *
   * <p>Fuel consumption allows limiting the amount of computation that can be performed by
   * WebAssembly code, providing protection against infinite loops and other resource exhaustion
   * attacks.
   *
   * @return true if fuel consumption is enabled, false otherwise
   * @since 1.0.0
   */
  @Override
  public boolean isFuelEnabled() {
    ensureNotClosed();

    try {
      return nativeIsFuelEnabled(getNativeHandle());
    } catch (final Exception e) {
      LOGGER.warning("Failed to check fuel enabled status: " + e.getMessage());
      return false;
    }
  }

  /**
   * Checks if epoch-based interruption is enabled for this engine.
   *
   * <p>Epoch interruption provides a way to interrupt long-running WebAssembly code at regular
   * intervals, enabling cooperative multitasking and timeout handling.
   *
   * @return true if epoch interruption is enabled, false otherwise
   * @since 1.0.0
   */
  @Override
  public boolean isEpochInterruptionEnabled() {
    ensureNotClosed();

    try {
      return nativeIsEpochInterruptionEnabled(getNativeHandle());
    } catch (final Exception e) {
      LOGGER.warning("Failed to check epoch interruption enabled status: " + e.getMessage());
      return false;
    }
  }

  /**
   * Gets the maximum number of instances that can be created with this engine.
   *
   * <p>Returns the maximum number of WebAssembly instances that can be active simultaneously. A
   * value of 0 indicates no limit.
   *
   * @return the maximum number of instances, or 0 for unlimited
   * @since 1.0.0
   */
  @Override
  public int getMaxInstances() {
    ensureNotClosed();

    try {
      return (int) nativeGetMaxInstances(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get max instances", e);
    }
  }

  /**
   * Gets the reference count for this engine.
   *
   * <p>Returns the number of stores and other objects that are currently holding references to this
   * engine. This is useful for debugging and resource management.
   *
   * @return the current reference count
   * @since 1.0.0
   */
  @Override
  public long getReferenceCount() {
    ensureNotClosed();

    try {
      return nativeGetReferenceCount(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get reference count", e);
    }
  }

  /**
   * Creates a streaming compiler for progressive WebAssembly module compilation.
   *
   * <p>This method creates a StreamingCompiler that can compile WebAssembly modules progressively
   * as data arrives, enabling efficient processing of large modules and network-delivered content.
   *
   * @return a new StreamingCompiler instance
   * @throws WasmException if the streaming compiler cannot be created
   */
  @Override
  public StreamingCompiler createStreamingCompiler() throws WasmException {
    ensureNotClosed();

    try {
      return new JniStreamingCompiler(this);
    } catch (final Exception e) {
      throw new WasmException("Failed to create streaming compiler", e);
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
    ensureNotClosed();

    try {
      // Create config using existing methods to get actual configuration
      final EngineConfig config = new EngineConfig();

      // Set debug info using existing method
      config.debugInfo(isDebugInfo());

      // Set optimization level using existing method
      final int optLevel = getOptimizationLevel();
      final OptimizationLevel optimizationLevel;
      switch (optLevel) {
        case 0:
          optimizationLevel = OptimizationLevel.NONE;
          break;
        case 1:
          optimizationLevel = OptimizationLevel.SPEED;
          break;
        case 2:
          optimizationLevel = OptimizationLevel.SPEED_AND_SIZE;
          break;
        default:
          // Default to SPEED for unknown values
          optimizationLevel = OptimizationLevel.SPEED;
          break;
      }
      config.optimizationLevel(optimizationLevel);

      return config;
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Failed to retrieve engine configuration", e);
    }
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

  /**
   * Checks if the engine supports a specific WebAssembly feature.
   *
   * @param engineHandle the native engine handle
   * @param featureOrdinal the feature ordinal value
   * @return true if the feature is supported, false otherwise
   */
  private static native boolean nativeSupportsFeature(long engineHandle, int featureOrdinal);

  /**
   * Gets the memory limit in pages for the engine.
   *
   * @param engineHandle the native engine handle
   * @return the memory limit in pages, or 0 for unlimited
   */
  private static native long nativeGetMemoryLimitPages(long engineHandle);

  /**
   * Gets the stack size limit for the engine.
   *
   * @param engineHandle the native engine handle
   * @return the stack size limit in bytes, or 0 for default
   */
  private static native long nativeGetStackSizeLimit(long engineHandle);

  /**
   * Checks if fuel consumption is enabled for the engine.
   *
   * @param engineHandle the native engine handle
   * @return true if fuel consumption is enabled, false otherwise
   */
  private static native boolean nativeIsFuelEnabled(long engineHandle);

  /**
   * Checks if epoch-based interruption is enabled for the engine.
   *
   * @param engineHandle the native engine handle
   * @return true if epoch interruption is enabled, false otherwise
   */
  private static native boolean nativeIsEpochInterruptionEnabled(long engineHandle);

  /**
   * Gets the maximum number of instances for the engine.
   *
   * @param engineHandle the native engine handle
   * @return the maximum number of instances, or 0 for unlimited
   */
  private static native long nativeGetMaxInstances(long engineHandle);

  /**
   * Gets the reference count for the engine.
   *
   * @param engineHandle the native engine handle
   * @return the current reference count
   */
  private static native long nativeGetReferenceCount(long engineHandle);
}

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.PlatformDetector;
import ai.tegmentum.wasmtime4j.RuntimeInfo;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.jni.util.JniConcurrencyManager;
import ai.tegmentum.wasmtime4j.jni.util.JniPhantomReferenceManager;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniResourceCache;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasmRuntime interface.
 *
 * <p>This class provides WebAssembly runtime functionality using Java Native Interface (JNI) to
 * communicate with the native Wasmtime library. It manages the lifecycle of native resources and
 * provides defensive programming to prevent JVM crashes.
 *
 * <p>This implementation is designed for Java 8+ compatibility and uses JNI calls to interact with
 * the shared wasmtime4j-native Rust library. It includes advanced features like resource caching,
 * concurrency management, and automatic cleanup via phantom references.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe concurrent access to WebAssembly resources
 *   <li>Automatic native resource cleanup and management
 *   <li>Performance optimizations through caching and batching
 *   <li>Comprehensive error handling and validation
 *   <li>Integration with public wasmtime4j API
 * </ul>
 */
public final class JniWasmRuntime extends JniResource implements WasmRuntime {

  private static final Logger LOGGER = Logger.getLogger(JniWasmRuntime.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasmRuntime: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Resource cache for engines and modules. */
  private final JniResourceCache<String, Object> resourceCache;

  /** Concurrency manager for thread-safe operations. */
  private final JniConcurrencyManager concurrencyManager;

  /** Phantom reference manager for automatic cleanup. */
  private final JniPhantomReferenceManager phantomManager;

  /**
   * Creates a new JNI WebAssembly runtime.
   *
   * @throws WasmException if the native library cannot be loaded or runtime cannot be initialized
   */
  public JniWasmRuntime() throws WasmException {
    super(initializeRuntime());

    try {
      // Initialize performance and resource management utilities
      this.resourceCache = new JniResourceCache<>(500); // Cache up to 500 resources
      this.concurrencyManager =
          new JniConcurrencyManager(20, 30000); // 20 concurrent ops, 30s timeout
      this.phantomManager = JniPhantomReferenceManager.getInstance();

      // Register this runtime for automatic cleanup
      this.phantomManager.register(this, nativeHandle, "nativeDestroyRuntime");
      this.concurrencyManager.registerResource(nativeHandle);

      LOGGER.fine(
          "Created JNI WebAssembly runtime with handle: 0x" + Long.toHexString(nativeHandle));
    } catch (final Exception e) {
      // Clean up if initialization fails
      try {
        nativeDestroyRuntime(nativeHandle);
      } catch (final Exception cleanupException) {
        LOGGER.warning(
            "Failed to cleanup runtime after initialization failure: "
                + cleanupException.getMessage());
      }
      throw new WasmException("Failed to initialize JNI runtime", e);
    }
  }

  /**
   * Initializes the native runtime and loads the native library.
   *
   * @return the native runtime handle
   * @throws WasmException if initialization fails
   */
  private static long initializeRuntime() throws WasmException {
    try {
      // Ensure native library is loaded
      NativeLibraryLoader.loadNativeLibrary();

      final long handle = nativeCreateRuntime();
      if (handle == 0) {
        throw new WasmException("Failed to create native runtime");
      }
      return handle;
    } catch (final UnsatisfiedLinkError e) {
      throw new WasmException("Native library not available", e);
    } catch (final Exception e) {
      throw new WasmException("Failed to initialize native runtime", e);
    }
  }

  @Override
  public Engine createEngine() throws WasmException {
    JniValidation.requireNonNull(this, "runtime");

    try {
      return concurrencyManager.executeWithReadLock(
          getNativeHandle(),
          () -> {
            try {
              final long engineHandle = nativeCreateEngine(nativeHandle);
              if (engineHandle == 0) {
                throw new WasmException("Failed to create engine");
              }

              final JniEngine engine = new JniEngine(engineHandle);

              // Register engine for concurrency management and cleanup
              concurrencyManager.registerResource(engineHandle);
              phantomManager.register(engine, engineHandle, "nativeDestroyEngine");

              // Cache the engine
              resourceCache.put("engine-" + engineHandle, engine);

              LOGGER.fine("Created engine with handle: 0x" + Long.toHexString(engineHandle));
              return engine;
            } catch (final WasmException e) {
              throw new RuntimeException(e);
            } catch (final Exception e) {
              throw new RuntimeException(new WasmException("Unexpected error creating engine", e));
            }
          });
    } catch (final RuntimeException e) {
      if (e.getCause() instanceof WasmException) {
        throw (WasmException) e.getCause();
      }
      throw new WasmException("Unexpected error creating engine", e);
    }
  }

  @Override
  public Engine createEngine(final EngineConfig config) throws WasmException {
    JniValidation.requireNonNull(config, "config");

    // For now, create engine with default config and log the custom config request
    LOGGER.fine(
        "Creating engine with custom config (config details will be implemented in future)");
    return createEngine();
  }

  @Override
  public Module compileModule(final Engine engine, final byte[] wasmBytes) throws WasmException {
    JniValidation.requireNonNull(engine, "engine");
    JniValidation.requireNonNull(wasmBytes, "wasmBytes");

    if (wasmBytes.length == 0) {
      throw new WasmException("WebAssembly bytecode cannot be empty");
    }

    try {
      return concurrencyManager.executeWithReadLock(
          getNativeHandle(),
          () -> {
            try {
              final long moduleHandle = nativeCompileModule(nativeHandle, wasmBytes);
              if (moduleHandle == 0) {
                throw new WasmException("Failed to compile WebAssembly module");
              }

              final JniModule module = new JniModule(moduleHandle, (JniEngine) engine);

              // Register module for concurrency management and cleanup
              concurrencyManager.registerResource(moduleHandle);
              phantomManager.register(module, moduleHandle, "nativeDestroyModule");

              // Cache the module (using bytecode hash as key for potential reuse)
              final String cacheKey = "module-" + java.util.Arrays.hashCode(wasmBytes);
              resourceCache.put(cacheKey, module);

              LOGGER.fine("Compiled module with handle: 0x" + Long.toHexString(moduleHandle));
              return module;
            } catch (final WasmException e) {
              throw new RuntimeException(e);
            } catch (final Exception e) {
              throw new RuntimeException(new WasmException("Unexpected error compiling module", e));
            }
          });
    } catch (final RuntimeException e) {
      if (e.getCause() instanceof WasmException) {
        throw (WasmException) e.getCause();
      }
      throw new WasmException("Unexpected error compiling module", e);
    }
  }

  @Override
  public Instance instantiate(final Module module) throws WasmException {
    return instantiate(module, null);
  }

  @Override
  public Instance instantiate(final Module module, final ImportMap imports) throws WasmException {
    JniValidation.requireNonNull(module, "module");

    try {
      return concurrencyManager.executeWithWriteLock(
          getNativeHandle(),
          () -> {
            try {
              // For now, we'll create a basic instance without imports
              // Full import support will be added when ImportMap interface is implemented
              if (imports != null) {
                LOGGER.fine(
                    "Instantiating module with imports (import details will be implemented in"
                        + " future)");
              }

              // This is a simplified instantiation - actual implementation would use JniStore
              // and handle imports properly
              final long instanceHandle =
                  nativeInstantiateModule(nativeHandle, ((JniModule) module).getNativeHandle());
              if (instanceHandle == 0) {
                throw new WasmException("Failed to instantiate WebAssembly module");
              }

              final JniInstance instance = new JniInstance(instanceHandle);

              // Register instance for concurrency management and cleanup
              concurrencyManager.registerResource(instanceHandle);
              phantomManager.register(instance, instanceHandle, "nativeDestroyInstance");

              LOGGER.fine(
                  "Instantiated module with instance handle: 0x"
                      + Long.toHexString(instanceHandle));
              return instance;
            } catch (final WasmException e) {
              throw new RuntimeException(e);
            } catch (final Exception e) {
              throw new RuntimeException(
                  new WasmException("Unexpected error instantiating module", e));
            }
          });
    } catch (final RuntimeException e) {
      if (e.getCause() instanceof WasmException) {
        throw (WasmException) e.getCause();
      }
      throw new WasmException("Unexpected error instantiating module", e);
    }
  }

  @Override
  public RuntimeInfo getRuntimeInfo() {
    return concurrencyManager.executeWithReadLock(
        getNativeHandle(),
        () -> {
          try {
            final String version = nativeGetWasmtimeVersion();
            return new RuntimeInfo(
                "wasmtime4j-jni",
                "1.0.0-SNAPSHOT",
                version != null ? version : "unknown",
                RuntimeType.JNI,
                System.getProperty("java.version"),
                PlatformDetector.getPlatformDescription());
          } catch (final Exception e) {
            LOGGER.warning("Failed to get runtime info: " + e.getMessage());
            return new RuntimeInfo(
                "wasmtime4j-jni",
                "1.0.0-SNAPSHOT",
                "unknown",
                RuntimeType.JNI,
                System.getProperty("java.version"),
                PlatformDetector.getPlatformDescription());
          }
        });
  }

  @Override
  public boolean isValid() {
    return !isClosed() && nativeHandle != 0;
  }

  @Override
  protected void doClose() throws Exception {
    // Close utility managers first
    if (resourceCache != null && !resourceCache.isClosed()) {
      try {
        resourceCache.close();
        LOGGER.fine("Closed resource cache");
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource cache: " + e.getMessage());
      }
    }

    if (concurrencyManager != null && !concurrencyManager.isClosed()) {
      try {
        concurrencyManager.unregisterResource(nativeHandle);
        concurrencyManager.close();
        LOGGER.fine("Closed concurrency manager");
      } catch (final Exception e) {
        LOGGER.warning("Error closing concurrency manager: " + e.getMessage());
      }
    }

    if (phantomManager != null && !phantomManager.isClosed()) {
      try {
        phantomManager.unregister(this);
      } catch (final Exception e) {
        LOGGER.warning("Error unregistering from phantom manager: " + e.getMessage());
      }
    }

    // Close native resource
    nativeDestroyRuntime(nativeHandle);
  }

  @Override
  protected String getResourceType() {
    return "WasmRuntime";
  }

  // Native method declarations - these will be implemented in the native library

  /**
   * Creates a new native runtime.
   *
   * @return native runtime handle or 0 on failure
   */
  private static native long nativeCreateRuntime();

  /**
   * Creates a new engine for the given runtime.
   *
   * @param runtimeHandle the native runtime handle
   * @return native engine handle or 0 on failure
   */
  private static native long nativeCreateEngine(long runtimeHandle);

  /**
   * Compiles a WebAssembly module.
   *
   * @param runtimeHandle the native runtime handle
   * @param bytecode the WebAssembly bytecode
   * @return native module handle or 0 on failure
   */
  private static native long nativeCompileModule(long runtimeHandle, byte[] bytecode);

  /**
   * Instantiates a WebAssembly module.
   *
   * @param runtimeHandle the native runtime handle
   * @param moduleHandle the native module handle
   * @return native instance handle or 0 on failure
   */
  private static native long nativeInstantiateModule(long runtimeHandle, long moduleHandle);

  /**
   * Gets the Wasmtime version string.
   *
   * @return the version string or null on error
   */
  private static native String nativeGetWasmtimeVersion();

  /**
   * Destroys a native runtime.
   *
   * @param runtimeHandle the native runtime handle
   */
  private static native void nativeDestroyRuntime(long runtimeHandle);
}

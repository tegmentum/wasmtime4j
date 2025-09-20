package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.aot.AotCompiler;
import ai.tegmentum.wasmtime4j.aot.AotCompilerInfo;
import ai.tegmentum.wasmtime4j.aot.AotExecutable;
import ai.tegmentum.wasmtime4j.aot.AotOptions;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniExceptionHandler;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.serialization.SerializedModule;
import ai.tegmentum.wasmtime4j.serialization.TargetPlatform;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * JNI implementation of the AOT compiler interface.
 *
 * <p>This class provides JNI bindings to the native Wasmtime AOT compilation functionality,
 * enabling ahead-of-time compilation of WebAssembly modules for optimized deployment.
 *
 * <p>All methods in this class are thread-safe and implement defensive programming patterns
 * to prevent JVM crashes.
 *
 * @since 1.0.0
 */
public final class JniAotCompiler implements AotCompiler {

  private static final Logger LOGGER = Logger.getLogger(JniAotCompiler.class.getName());

  // Native library loading
  static {
    NativeLibraryLoader.loadNativeLibrary();
  }

  // Native compiler handle
  private final long nativeHandle;

  // Cache for compiled executables to avoid redundant work
  private final Map<String, AotExecutable> compilationCache;

  // Flag to track if this compiler has been closed
  private volatile boolean closed = false;

  /**
   * Creates a new JNI AOT compiler instance.
   *
   * @throws WasmException if the native compiler cannot be created
   */
  public JniAotCompiler() throws WasmException {
    this.compilationCache = new ConcurrentHashMap<>();
    this.nativeHandle = nativeCreateCompiler();
    if (this.nativeHandle == 0) {
      throw new WasmException("Failed to create native AOT compiler");
    }
    LOGGER.fine("JNI AOT compiler created with handle: " + this.nativeHandle);
  }

  @Override
  public SerializedModule compileModule(
      final Engine engine,
      final byte[] wasmBytes,
      final AotOptions options) throws WasmException {
    return compileModule(engine, wasmBytes, options, TargetPlatform.current());
  }

  @Override
  public SerializedModule compileModule(
      final Engine engine,
      final byte[] wasmBytes,
      final AotOptions options,
      final TargetPlatform targetPlatform) throws WasmException {

    validateNotClosed();
    validateParameters(engine, wasmBytes, options, targetPlatform);

    try {
      // First compile the WebAssembly bytes to a module
      final Module module = engine.compileModule(wasmBytes);
      return compileModule(module, options, targetPlatform);
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Failed to compile module from bytes");
    }
  }

  @Override
  public SerializedModule compileModule(
      final Module module,
      final AotOptions options) throws WasmException {
    return compileModule(module, options, TargetPlatform.current());
  }

  @Override
  public SerializedModule compileModule(
      final Module module,
      final AotOptions options,
      final TargetPlatform targetPlatform) throws WasmException {

    validateNotClosed();
    validateParameters(module, options, targetPlatform);

    // Create cache key
    final String cacheKey = createCacheKey(module, options, targetPlatform);

    try {
      // Check cache first
      final AotExecutable cachedExecutable = compilationCache.get(cacheKey);
      if (cachedExecutable != null) {
        LOGGER.fine("AOT compilation cache hit for key: " + cacheKey);
        return convertExecutableToSerializedModule(cachedExecutable);
      }

      // Get native handles
      final long engineHandle = extractEngineHandle(module.getEngine());
      final long moduleHandle = extractModuleHandle(module);

      // Call native compilation
      final long executableHandle = nativeCompileToNative(
          nativeHandle,
          engineHandle,
          moduleHandle,
          options.getOptimizationLevel().ordinal(),
          targetPlatform.ordinal()
      );

      if (executableHandle == 0) {
        throw new WasmException("Native AOT compilation failed");
      }

      // Create executable wrapper
      final AotExecutable executable = new JniAotExecutable(executableHandle, targetPlatform, options);

      // Cache the result
      compilationCache.put(cacheKey, executable);

      // Convert to serialized module
      return convertExecutableToSerializedModule(executable);

    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "AOT compilation failed");
    }
  }

  @Override
  public AotExecutable createExecutable(
      final SerializedModule module,
      final TargetPlatform platform) throws WasmException {

    validateNotClosed();
    validateParameters(module, platform);

    try {
      // For now, we'll return a simple executable wrapper
      // In a full implementation, this would involve loading the serialized module
      // and creating a native executable from it
      return new JniAotExecutable(0, platform, getDefaultOptions());
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Failed to create executable");
    }
  }

  @Override
  public AotExecutable createExecutable(final SerializedModule module) throws WasmException {
    return createExecutable(module, TargetPlatform.current());
  }

  @Override
  public List<TargetPlatform> getSupportedPlatforms() {
    validateNotClosed();

    try {
      final int[] platformIds = nativeGetSupportedPlatforms(nativeHandle);
      return java.util.Arrays.stream(platformIds)
          .mapToObj(id -> TargetPlatform.values()[id])
          .collect(java.util.stream.Collectors.toList());
    } catch (final Exception e) {
      LOGGER.warning("Failed to get supported platforms: " + e.getMessage());
      // Return default platforms as fallback
      return List.of(TargetPlatform.current());
    }
  }

  @Override
  public boolean isPlatformSupported(final TargetPlatform platform) {
    if (platform == null) {
      throw new IllegalArgumentException("Platform cannot be null");
    }

    validateNotClosed();

    try {
      return nativeIsPlatformSupported(nativeHandle, platform.ordinal());
    } catch (final Exception e) {
      LOGGER.warning("Failed to check platform support: " + e.getMessage());
      return platform == TargetPlatform.current();
    }
  }

  @Override
  public AotOptions getDefaultOptions() {
    // Return default AOT options
    return AotOptions.builder()
        .optimizationLevel(ai.tegmentum.wasmtime4j.aot.OptimizationLevel.SPEED)
        .debugInfo(false)
        .profiling(false)
        .build();
  }

  @Override
  public boolean validateOptions(final AotOptions options) {
    if (options == null) {
      throw new IllegalArgumentException("Options cannot be null");
    }

    validateNotClosed();

    try {
      return nativeValidateOptions(
          nativeHandle,
          options.getOptimizationLevel().ordinal(),
          options.isDebugInfo(),
          options.isProfiling()
      );
    } catch (final Exception e) {
      LOGGER.warning("Failed to validate options: " + e.getMessage());
      return false;
    }
  }

  @Override
  public AotCompilerInfo getCompilerInfo() {
    validateNotClosed();

    try {
      final String version = nativeGetCompilerVersion(nativeHandle);
      final String[] capabilities = nativeGetCompilerCapabilities(nativeHandle);

      return new AotCompilerInfo() {
        @Override
        public String getVersion() {
          return version;
        }

        @Override
        public List<String> getCapabilities() {
          return List.of(capabilities);
        }

        @Override
        public List<TargetPlatform> getSupportedPlatforms() {
          return JniAotCompiler.this.getSupportedPlatforms();
        }
      };
    } catch (final Exception e) {
      LOGGER.warning("Failed to get compiler info: " + e.getMessage());
      // Return default info as fallback
      return new AotCompilerInfo() {
        @Override
        public String getVersion() {
          return "1.0.0";
        }

        @Override
        public List<String> getCapabilities() {
          return List.of("cranelift");
        }

        @Override
        public List<TargetPlatform> getSupportedPlatforms() {
          return List.of(TargetPlatform.current());
        }
      };
    }
  }

  /**
   * Closes this compiler and releases native resources.
   */
  public void close() {
    if (!closed) {
      closed = true;
      compilationCache.clear();
      if (nativeHandle != 0) {
        nativeDestroyCompiler(nativeHandle);
      }
      LOGGER.fine("JNI AOT compiler closed");
    }
  }

  // Private helper methods

  private void validateNotClosed() {
    if (closed) {
      throw new IllegalStateException("AOT compiler has been closed");
    }
  }

  private void validateParameters(final Object... params) {
    for (final Object param : params) {
      if (param == null) {
        throw new IllegalArgumentException("Parameter cannot be null");
      }
    }
  }

  private String createCacheKey(
      final Module module,
      final AotOptions options,
      final TargetPlatform targetPlatform) {
    // Create a deterministic cache key
    final String moduleId = module.getId() != null ? module.getId() : "unknown";
    final String optionsHash = String.valueOf(options.hashCode());
    final String platformStr = targetPlatform.name();

    return String.format("%s_%s_%s", moduleId, optionsHash, platformStr);
  }

  private long extractEngineHandle(final Engine engine) throws WasmException {
    if (engine instanceof JniEngine) {
      return ((JniEngine) engine).getNativeHandle();
    }
    throw new WasmException("Engine is not a JNI engine implementation");
  }

  private long extractModuleHandle(final Module module) throws WasmException {
    if (module instanceof JniModule) {
      return ((JniModule) module).getNativeHandle();
    }
    throw new WasmException("Module is not a JNI module implementation");
  }

  private SerializedModule convertExecutableToSerializedModule(final AotExecutable executable)
      throws WasmException {
    // For now, create a simple serialized module wrapper
    // In a full implementation, this would properly serialize the executable
    final byte[] nativeCode = executable.getNativeCode();
    return new JniSerializedModule(nativeCode, executable.getMetadata());
  }

  // Native method declarations

  /**
   * Creates a new native AOT compiler.
   *
   * @return native compiler handle, or 0 on failure
   */
  private static native long nativeCreateCompiler();

  /**
   * Destroys a native AOT compiler.
   *
   * @param compilerHandle the compiler handle to destroy
   */
  private static native void nativeDestroyCompiler(long compilerHandle);

  /**
   * Compiles a module to native code.
   *
   * @param compilerHandle the compiler handle
   * @param engineHandle the engine handle
   * @param moduleHandle the module handle
   * @param optimizationLevel the optimization level (0-2)
   * @param targetPlatform the target platform ID
   * @return native executable handle, or 0 on failure
   */
  private static native long nativeCompileToNative(
      long compilerHandle,
      long engineHandle,
      long moduleHandle,
      int optimizationLevel,
      int targetPlatform
  );

  /**
   * Gets the list of supported platform IDs.
   *
   * @param compilerHandle the compiler handle
   * @return array of supported platform IDs
   */
  private static native int[] nativeGetSupportedPlatforms(long compilerHandle);

  /**
   * Checks if a platform is supported.
   *
   * @param compilerHandle the compiler handle
   * @param platformId the platform ID to check
   * @return true if supported, false otherwise
   */
  private static native boolean nativeIsPlatformSupported(long compilerHandle, int platformId);

  /**
   * Validates AOT compilation options.
   *
   * @param compilerHandle the compiler handle
   * @param optimizationLevel the optimization level
   * @param debugInfo whether debug info is enabled
   * @param profiling whether profiling is enabled
   * @return true if options are valid, false otherwise
   */
  private static native boolean nativeValidateOptions(
      long compilerHandle,
      int optimizationLevel,
      boolean debugInfo,
      boolean profiling
  );

  /**
   * Gets the compiler version string.
   *
   * @param compilerHandle the compiler handle
   * @return version string
   */
  private static native String nativeGetCompilerVersion(long compilerHandle);

  /**
   * Gets the compiler capabilities.
   *
   * @param compilerHandle the compiler handle
   * @return array of capability strings
   */
  private static native String[] nativeGetCompilerCapabilities(long compilerHandle);
}
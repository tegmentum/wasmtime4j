package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.aot.AotExecutable;
import ai.tegmentum.wasmtime4j.aot.AotExecutableMetadata;
import ai.tegmentum.wasmtime4j.aot.AotOptions;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniExceptionHandler;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.serialization.TargetPlatform;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * JNI implementation of AOT executable.
 *
 * <p>This class wraps a native AOT executable and provides Java access to the compiled
 * WebAssembly code. It implements defensive programming patterns to ensure safe access
 * to native resources.
 *
 * @since 1.0.0
 */
public final class JniAotExecutable implements AotExecutable {

  private static final Logger LOGGER = Logger.getLogger(JniAotExecutable.class.getName());

  // Native library loading
  static {
    NativeLibraryLoader.loadNativeLibrary();
  }

  // Native executable handle
  private final long nativeHandle;

  // Target platform this executable is for
  private final TargetPlatform targetPlatform;

  // Compilation metadata
  private final AotExecutableMetadata metadata;

  // Flag to track if this executable has been closed
  private volatile boolean closed = false;

  /**
   * Creates a new JNI AOT executable.
   *
   * @param nativeHandle the native executable handle
   * @param targetPlatform the target platform
   * @param options the compilation options used
   * @throws IllegalArgumentException if any parameter is null
   */
  public JniAotExecutable(
      final long nativeHandle,
      final TargetPlatform targetPlatform,
      final AotOptions options) {

    if (targetPlatform == null) {
      throw new IllegalArgumentException("Target platform cannot be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("Options cannot be null");
    }

    this.nativeHandle = nativeHandle;
    this.targetPlatform = targetPlatform;
    this.metadata = createMetadata(options);

    LOGGER.fine("JNI AOT executable created with handle: " + this.nativeHandle);
  }

  @Override
  public byte[] getNativeCode() throws WasmException {
    validateNotClosed();

    if (nativeHandle == 0) {
      return new byte[0]; // Return empty array for null handle
    }

    try {
      final byte[] nativeCode = nativeGetNativeCode(nativeHandle);
      if (nativeCode == null) {
        throw new WasmException("Failed to retrieve native code");
      }
      return nativeCode.clone(); // Defensive copy
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Failed to get native code");
    }
  }

  @Override
  public TargetPlatform getTargetPlatform() {
    return targetPlatform;
  }

  @Override
  public Module loadAsModule(final Engine engine) throws WasmException {
    validateNotClosed();
    validateParameters(engine);

    if (nativeHandle == 0) {
      throw new WasmException("Cannot load module from invalid executable");
    }

    try {
      // Extract engine handle
      final long engineHandle = extractEngineHandle(engine);

      // Load as module through native call
      final long moduleHandle = nativeLoadAsModule(nativeHandle, engineHandle);
      if (moduleHandle == 0) {
        throw new WasmException("Failed to load executable as module");
      }

      // Create JNI module wrapper
      return new JniModule(moduleHandle, engine);
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Failed to load executable as module");
    }
  }

  @Override
  public AotExecutableMetadata getMetadata() {
    return metadata;
  }

  @Override
  public boolean isValidForPlatform(final TargetPlatform platform) {
    if (platform == null) {
      throw new IllegalArgumentException("Platform cannot be null");
    }

    validateNotClosed();

    if (nativeHandle == 0) {
      return false;
    }

    try {
      return nativeIsValidForPlatform(nativeHandle, platform.ordinal());
    } catch (final Exception e) {
      LOGGER.warning("Failed to check platform validity: " + e.getMessage());
      // Fallback to simple comparison
      return targetPlatform.equals(platform) || platform == TargetPlatform.CURRENT;
    }
  }

  @Override
  public boolean isValid() {
    validateNotClosed();

    if (nativeHandle == 0) {
      return false;
    }

    try {
      return nativeValidateExecutable(nativeHandle);
    } catch (final Exception e) {
      LOGGER.warning("Failed to validate executable: " + e.getMessage());
      return false;
    }
  }

  @Override
  public long getSize() {
    validateNotClosed();

    if (nativeHandle == 0) {
      return 0;
    }

    try {
      return nativeGetSize(nativeHandle);
    } catch (final Exception e) {
      LOGGER.warning("Failed to get executable size: " + e.getMessage());
      return 0;
    }
  }

  @Override
  public String getChecksum() throws WasmException {
    validateNotClosed();

    if (nativeHandle == 0) {
      throw new WasmException("Cannot get checksum from invalid executable");
    }

    try {
      final String checksum = nativeGetChecksum(nativeHandle);
      if (checksum == null || checksum.isEmpty()) {
        throw new WasmException("Failed to calculate checksum");
      }
      return checksum;
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Failed to get checksum");
    }
  }

  /**
   * Gets the native handle for this executable.
   *
   * <p>This method is intended for internal use by other JNI components.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  /**
   * Closes this executable and releases native resources.
   */
  public void close() {
    if (!closed) {
      closed = true;
      if (nativeHandle != 0) {
        nativeDestroyExecutable(nativeHandle);
      }
      LOGGER.fine("JNI AOT executable closed");
    }
  }

  // Private helper methods

  private void validateNotClosed() {
    if (closed) {
      throw new IllegalStateException("AOT executable has been closed");
    }
  }

  private void validateParameters(final Object... params) {
    for (final Object param : params) {
      if (param == null) {
        throw new IllegalArgumentException("Parameter cannot be null");
      }
    }
  }

  private long extractEngineHandle(final Engine engine) throws WasmException {
    if (engine instanceof JniEngine) {
      return ((JniEngine) engine).getNativeHandle();
    }
    throw new WasmException("Engine is not a JNI engine implementation");
  }

  private AotExecutableMetadata createMetadata(final AotOptions options) {
    // Create metadata based on the compilation options
    return new AotExecutableMetadata() {
      private final Instant compilationTime = Instant.now();

      @Override
      public Instant getCompilationTime() {
        return compilationTime;
      }

      @Override
      public String getWasmtimeVersion() {
        return "36.0.2"; // Current Wasmtime version
      }

      @Override
      public AotOptions getCompilationOptions() {
        return options;
      }

      @Override
      public String getModuleHash() {
        try {
          return getChecksum();
        } catch (final WasmException e) {
          LOGGER.warning("Failed to get module hash: " + e.getMessage());
          return "unknown";
        }
      }

      @Override
      public long getOriginalSize() {
        // Would need to be passed in during construction in a full implementation
        return 0;
      }

      @Override
      public long getCompiledSize() {
        return getSize();
      }

      @Override
      public TargetPlatform getTargetPlatform() {
        return targetPlatform;
      }
    };
  }

  // Native method declarations

  /**
   * Gets the native code bytes from the executable.
   *
   * @param executableHandle the executable handle
   * @return native code bytes, or null on failure
   */
  private static native byte[] nativeGetNativeCode(long executableHandle);

  /**
   * Loads the executable as a module.
   *
   * @param executableHandle the executable handle
   * @param engineHandle the engine handle
   * @return module handle, or 0 on failure
   */
  private static native long nativeLoadAsModule(long executableHandle, long engineHandle);

  /**
   * Checks if the executable is valid for the given platform.
   *
   * @param executableHandle the executable handle
   * @param platformId the platform ID
   * @return true if valid, false otherwise
   */
  private static native boolean nativeIsValidForPlatform(long executableHandle, int platformId);

  /**
   * Validates the executable.
   *
   * @param executableHandle the executable handle
   * @return true if valid, false otherwise
   */
  private static native boolean nativeValidateExecutable(long executableHandle);

  /**
   * Gets the size of the executable in bytes.
   *
   * @param executableHandle the executable handle
   * @return size in bytes
   */
  private static native long nativeGetSize(long executableHandle);

  /**
   * Gets the checksum of the executable.
   *
   * @param executableHandle the executable handle
   * @return checksum string, or null on failure
   */
  private static native String nativeGetChecksum(long executableHandle);

  /**
   * Destroys the native executable.
   *
   * @param executableHandle the executable handle to destroy
   */
  private static native void nativeDestroyExecutable(long executableHandle);
}
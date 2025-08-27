package ai.tegmentum.wasmtime4j.jni;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasmRuntime interface.
 *
 * <p>This class provides WebAssembly runtime functionality using Java Native Interface (JNI) to
 * communicate with the native Wasmtime library. It manages the lifecycle of native resources and
 * provides defensive programming to prevent JVM crashes.
 *
 * <p>This implementation is designed for Java 8+ compatibility and uses JNI calls to interact with
 * the shared wasmtime4j-native Rust library.
 */
public final class JniWasmRuntime implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(JniWasmRuntime.class.getName());

  /** Native runtime handle. */
  private volatile long nativeHandle;

  /** Flag to track if this runtime has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new JNI WebAssembly runtime.
   *
   * @throws IllegalStateException if the native library cannot be loaded
   * @throws RuntimeException if the native runtime cannot be initialized
   */
  public JniWasmRuntime() {
    try {
      this.nativeHandle = nativeCreateRuntime();
      if (this.nativeHandle == 0) {
        throw new RuntimeException("Failed to create native runtime");
      }
      LOGGER.fine("Created JNI WebAssembly runtime with handle: " + this.nativeHandle);
    } catch (final UnsatisfiedLinkError e) {
      throw new IllegalStateException("Native library not available", e);
    }
  }

  /**
   * Creates a new engine with default configuration.
   *
   * @return a new engine instance
   * @throws IllegalStateException if this runtime is closed
   * @throws RuntimeException if the engine cannot be created
   */
  public JniEngine createEngine() {
    validateNotClosed();
    try {
      final long engineHandle = nativeCreateEngine(nativeHandle);
      if (engineHandle == 0) {
        throw new RuntimeException("Failed to create engine");
      }
      return new JniEngine(engineHandle);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error creating engine", e);
    }
  }

  /**
   * Compiles a WebAssembly module from bytecode.
   *
   * @param bytecode the WebAssembly module bytecode
   * @return a compiled module
   * @throws IllegalArgumentException if bytecode is null or empty
   * @throws IllegalStateException if this runtime is closed
   * @throws RuntimeException if compilation fails
   */
  public JniModule compileModule(final byte[] bytecode) {
    if (bytecode == null || bytecode.length == 0) {
      throw new IllegalArgumentException("Bytecode cannot be null or empty");
    }
    validateNotClosed();

    try {
      final long moduleHandle = nativeCompileModule(nativeHandle, bytecode);
      if (moduleHandle == 0) {
        throw new RuntimeException("Failed to compile module");
      }
      return new JniModule(moduleHandle);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error compiling module", e);
    }
  }

  /**
   * Gets the version of the underlying Wasmtime library.
   *
   * @return the Wasmtime version string
   * @throws IllegalStateException if this runtime is closed
   */
  public String getWasmtimeVersion() {
    validateNotClosed();
    try {
      final String version = nativeGetWasmtimeVersion();
      return version != null ? version : "unknown";
    } catch (final Exception e) {
      LOGGER.warning("Failed to get Wasmtime version: " + e.getMessage());
      return "unknown";
    }
  }

  /**
   * Closes this runtime and releases all associated native resources.
   *
   * <p>After calling this method, all operations on this runtime will throw {@link
   * IllegalStateException}. This method is idempotent.
   */
  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      if (nativeHandle != 0) {
        try {
          nativeDestroyRuntime(nativeHandle);
          LOGGER.fine("Destroyed JNI WebAssembly runtime with handle: " + nativeHandle);
        } catch (final Exception e) {
          LOGGER.warning("Error destroying native runtime: " + e.getMessage());
        } finally {
          nativeHandle = 0;
        }
      }
    }
  }

  /** Finalizer to ensure native resources are released if close() wasn't called. */
  @Override
  protected void finalize() throws Throwable {
    try {
      if (!closed.get()) {
        LOGGER.warning("JniWasmRuntime was finalized without being closed");
        close();
      }
    } finally {
      super.finalize();
    }
  }

  /**
   * Validates that this runtime is not closed.
   *
   * @throws IllegalStateException if this runtime is closed
   */
  private void validateNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("Runtime is closed");
    }
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

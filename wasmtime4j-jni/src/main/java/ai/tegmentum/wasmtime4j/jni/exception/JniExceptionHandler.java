package ai.tegmentum.wasmtime4j.jni.exception;

import ai.tegmentum.wasmtime4j.exception.WasmtimeException;

/**
 * JNI-specific exception handler for WebAssembly components.
 *
 * @since 1.0.0
 */
public class JniExceptionHandler {

  /**
   * Handles JNI exceptions and converts them to appropriate WebAssembly exceptions.
   *
   * @param nativeException the native exception
   * @return converted WebAssembly exception
   */
  public static WasmtimeException handleNativeException(final String nativeException) {
    if (nativeException == null || nativeException.isEmpty()) {
      return new WasmtimeException("Unknown native exception");
    }
    return new WasmtimeException("Native exception: " + nativeException);
  }

  /**
   * Handles JNI exceptions with error codes.
   *
   * @param errorCode the error code
   * @param message the error message
   * @return converted WebAssembly exception
   */
  public static WasmtimeException handleNativeException(final int errorCode, final String message) {
    return new WasmtimeException("Native error [" + errorCode + "]: " + message);
  }

  private JniExceptionHandler() {
    // Utility class
  }
}

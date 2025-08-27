package ai.tegmentum.wasmtime4j.jni.util;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import java.util.logging.Logger;

/**
 * Utility class for mapping native errors to appropriate Java exceptions.
 *
 * <p>This class provides defensive programming by translating native error codes and messages into
 * meaningful Java exceptions. It ensures that native errors don't propagate as generic runtime
 * errors and provides proper exception hierarchy mapping.
 *
 * <p>The mapper supports various categories of WebAssembly-related exceptions including compilation
 * errors, runtime errors, validation errors, and system errors.
 */
public final class JniExceptionMapper {

  private static final Logger LOGGER = Logger.getLogger(JniExceptionMapper.class.getName());

  // Native error code constants (these should match the native library)
  /** No error occurred. */
  private static final int NATIVE_ERROR_NONE = 0;

  /** Generic compilation error. */
  private static final int NATIVE_ERROR_COMPILATION = 1;

  /** Runtime execution error. */
  private static final int NATIVE_ERROR_RUNTIME = 2;

  /** Module validation error. */
  private static final int NATIVE_ERROR_VALIDATION = 3;

  /** Memory access error. */
  private static final int NATIVE_ERROR_MEMORY = 4;

  /** Invalid argument error. */
  private static final int NATIVE_ERROR_INVALID_ARG = 5;

  /** Resource not found error. */
  private static final int NATIVE_ERROR_NOT_FOUND = 6;

  /** System/IO error. */
  private static final int NATIVE_ERROR_SYSTEM = 7;

  /** Out of memory error. */
  private static final int NATIVE_ERROR_OUT_OF_MEMORY = 8;

  /** Trap occurred during execution. */
  private static final int NATIVE_ERROR_TRAP = 9;

  /** Type mismatch error. */
  private static final int NATIVE_ERROR_TYPE_MISMATCH = 10;

  /** Private constructor to prevent instantiation of utility class. */
  private JniExceptionMapper() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Maps a native error code and message to an appropriate Java exception.
   *
   * @param errorCode the native error code
   * @param message the error message from native code
   * @return the appropriate Java exception
   */
  public static JniException mapNativeError(final int errorCode, final String message) {
    final String safeMessage = message != null ? message : "Unknown native error";

    switch (errorCode) {
      case NATIVE_ERROR_NONE:
        LOGGER.warning("mapNativeError called with NATIVE_ERROR_NONE");
        return new JniException("No error occurred", errorCode);

      case NATIVE_ERROR_COMPILATION:
        return new JniException("Compilation failed: " + safeMessage, errorCode);

      case NATIVE_ERROR_RUNTIME:
        return new JniException("Runtime error: " + safeMessage, errorCode);

      case NATIVE_ERROR_VALIDATION:
        return new JniException("Validation failed: " + safeMessage, errorCode);

      case NATIVE_ERROR_MEMORY:
        return new JniResourceException("Memory access error: " + safeMessage, errorCode);

      case NATIVE_ERROR_INVALID_ARG:
        return new JniException("Invalid argument: " + safeMessage, errorCode);

      case NATIVE_ERROR_NOT_FOUND:
        return new JniException("Resource not found: " + safeMessage, errorCode);

      case NATIVE_ERROR_SYSTEM:
        return new JniException("System error: " + safeMessage, errorCode);

      case NATIVE_ERROR_OUT_OF_MEMORY:
        return new JniResourceException("Native out of memory: " + safeMessage, errorCode);

      case NATIVE_ERROR_TRAP:
        return new JniException("WebAssembly trap: " + safeMessage, errorCode);

      case NATIVE_ERROR_TYPE_MISMATCH:
        return new JniException("Type mismatch: " + safeMessage, errorCode);

      default:
        LOGGER.warning("Unknown native error code: " + errorCode);
        return new JniException(
            "Unknown native error (code " + errorCode + "): " + safeMessage, errorCode);
    }
  }

  /**
   * Maps a native error code to an appropriate Java exception with a default message.
   *
   * @param errorCode the native error code
   * @return the appropriate Java exception
   */
  public static JniException mapNativeError(final int errorCode) {
    return mapNativeError(errorCode, null);
  }

  /**
   * Wraps a generic exception that occurred during native operations.
   *
   * <p>This method provides a standardized way to wrap unexpected exceptions that occur during JNI
   * calls, ensuring consistent error handling.
   *
   * @param operation the operation that failed
   * @param cause the underlying exception
   * @return a JniException wrapping the cause
   */
  public static JniException wrapNativeException(
      final String operation, final Throwable cause) {
    final String message =
        operation != null ? "Native operation failed: " + operation : "Native operation failed";

    LOGGER.warning(message + " - " + cause.getMessage());
    return new JniException(message, cause);
  }

  /**
   * Validates that a native handle is not zero and throws an appropriate exception if it is.
   *
   * @param handle the native handle to validate
   * @param resourceType the type of resource (for error messaging)
   * @throws JniResourceException if the handle is 0
   */
  public static void validateNativeHandle(final long handle, final String resourceType) {
    if (handle == 0) {
      final String message =
          "Failed to create "
              + (resourceType != null ? resourceType : "resource")
              + ": native handle is null";
      throw new JniResourceException(message);
    }
  }

  /**
   * Validates that a native operation result indicates success.
   *
   * @param result the result from a native operation (typically boolean)
   * @param operation the operation description
   * @throws JniException if the result indicates failure
   */
  public static void validateNativeResult(final boolean result, final String operation) {
    if (!result) {
      final String message =
          "Native operation failed: " + (operation != null ? operation : "unknown operation");
      throw new JniException(message);
    }
  }

  /**
   * Safely retrieves an error message from native code, handling null values.
   *
   * @param nativeMessage the message from native code (may be null)
   * @param defaultMessage the default message to use if native message is null
   * @return a safe, non-null error message
   */
  public static String getSafeErrorMessage(
      final String nativeMessage, final String defaultMessage) {
    if (nativeMessage != null && !nativeMessage.trim().isEmpty()) {
      return nativeMessage;
    }
    return defaultMessage != null ? defaultMessage : "Unknown error";
  }

  /**
   * Creates a standardized exception for resource cleanup failures.
   *
   * @param resourceType the type of resource being cleaned up
   * @param cause the underlying exception (may be null)
   * @return a JniResourceException for the cleanup failure
   */
  public static JniResourceException createCleanupException(
      final String resourceType, final Throwable cause) {
    final String message =
        "Failed to cleanup " + (resourceType != null ? resourceType : "resource");
    if (cause != null) {
      return new JniResourceException(message, cause);
    } else {
      return new JniResourceException(message);
    }
  }

  /**
   * Creates a standardized exception for invalid state operations.
   *
   * @param resourceType the type of resource
   * @param state the invalid state description
   * @return a JniException for the invalid state
   */
  public static JniException createInvalidStateException(
      final String resourceType, final String state) {
    final String message =
        (resourceType != null ? resourceType : "Resource")
            + " is "
            + (state != null ? state : "in invalid state");
    return new JniException(message);
  }

  /**
   * Gets a human-readable description of a native error code.
   *
   * @param errorCode the native error code
   * @return a description of the error code
   */
  public static String getErrorCodeDescription(final int errorCode) {
    switch (errorCode) {
      case NATIVE_ERROR_NONE:
        return "No error";
      case NATIVE_ERROR_COMPILATION:
        return "Compilation error";
      case NATIVE_ERROR_RUNTIME:
        return "Runtime error";
      case NATIVE_ERROR_VALIDATION:
        return "Validation error";
      case NATIVE_ERROR_MEMORY:
        return "Memory access error";
      case NATIVE_ERROR_INVALID_ARG:
        return "Invalid argument";
      case NATIVE_ERROR_NOT_FOUND:
        return "Resource not found";
      case NATIVE_ERROR_SYSTEM:
        return "System error";
      case NATIVE_ERROR_OUT_OF_MEMORY:
        return "Out of memory";
      case NATIVE_ERROR_TRAP:
        return "WebAssembly trap";
      case NATIVE_ERROR_TYPE_MISMATCH:
        return "Type mismatch";
      default:
        return "Unknown error (code " + errorCode + ")";
    }
  }
}

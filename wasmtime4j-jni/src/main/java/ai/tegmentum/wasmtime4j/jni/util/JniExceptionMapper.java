package ai.tegmentum.wasmtime4j.jni.util;

import ai.tegmentum.wasmtime4j.exception.ErrorMapper;
import ai.tegmentum.wasmtime4j.exception.WasmException;
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
 * <p>Error codes are defined by the Rust {@code ErrorCode} enum and mirrored in {@link
 * WasmErrorCode}. This mapper uses {@link WasmErrorCode} as the single source of truth for error
 * code classification.
 */
public final class JniExceptionMapper {

  private static final Logger LOGGER = Logger.getLogger(JniExceptionMapper.class.getName());

  /** Private constructor to prevent instantiation of utility class. */
  private JniExceptionMapper() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Maps a native error code and message to an appropriate categorized Java exception.
   *
   * <p>Delegates to the shared {@link ErrorMapper} for consistent error categorization across both
   * JNI and Panama implementations.
   *
   * @param errorCode the native error code
   * @param message the error message from native code
   * @return the appropriate Java exception
   */
  public static WasmException mapNativeError(final int errorCode, final String message) {
    final String safeMessage = message != null ? message : "Unknown native error";
    return ErrorMapper.mapErrorCode(errorCode, safeMessage);
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
  public static JniException wrapNativeException(final String operation, final Throwable cause) {
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
   * Maps a generic Java exception to an appropriate JniException.
   *
   * <p>This method provides a standardized way to convert generic exceptions that occur during JNI
   * operations into appropriate JNI-specific exceptions.
   *
   * @param exception the exception to map
   * @return a JniException wrapping or representing the original exception
   */
  public static JniException mapException(final Exception exception) {
    if (exception == null) {
      return new JniException("Unknown error occurred");
    }

    // If it's already a JniException, return it as-is
    if (exception instanceof JniException) {
      return (JniException) exception;
    }

    // Map common Java exceptions to appropriate JNI exceptions
    if (exception instanceof IllegalArgumentException) {
      return new JniException("Invalid parameter: " + exception.getMessage(), exception);
    }

    if (exception instanceof IllegalStateException) {
      return new JniException("Resource in invalid state: " + exception.getMessage(), exception);
    }

    if (exception instanceof IndexOutOfBoundsException) {
      return new JniException("Index out of bounds: " + exception.getMessage(), exception);
    }

    if (exception instanceof NullPointerException) {
      return new JniException("Null pointer error: " + exception.getMessage(), exception);
    }

    // Default mapping for all other exceptions
    return new JniException("Operation failed: " + exception.getMessage(), exception);
  }
}

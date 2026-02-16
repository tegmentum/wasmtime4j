package ai.tegmentum.wasmtime4j.jni.util;

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

  // Native error code constants (must match the Rust error.rs enum ErrorCode exactly)
  /** No error occurred. */
  private static final int NATIVE_ERROR_NONE = 0;

  /** WebAssembly compilation failed. */
  private static final int NATIVE_ERROR_COMPILATION = -1;

  /** WebAssembly module validation failed. */
  private static final int NATIVE_ERROR_VALIDATION = -2;

  /** WebAssembly runtime error occurred. */
  private static final int NATIVE_ERROR_RUNTIME = -3;

  /** Engine configuration error. */
  private static final int NATIVE_ERROR_ENGINE_CONFIG = -4;

  /** Store creation or management error. */
  private static final int NATIVE_ERROR_STORE = -5;

  /** Instance creation or management error. */
  private static final int NATIVE_ERROR_INSTANCE = -6;

  /** Memory access or allocation error. */
  private static final int NATIVE_ERROR_MEMORY = -7;

  /** Function invocation error. */
  private static final int NATIVE_ERROR_FUNCTION = -8;

  /** Import or export resolution error. */
  private static final int NATIVE_ERROR_IMPORT_EXPORT = -9;

  /** Type conversion or validation error. */
  private static final int NATIVE_ERROR_TYPE = -10;

  /** Resource management error. */
  private static final int NATIVE_ERROR_RESOURCE = -11;

  /** I/O operation error. */
  private static final int NATIVE_ERROR_IO = -12;

  /** Invalid parameter provided. */
  private static final int NATIVE_ERROR_INVALID_PARAMETER = -13;

  /** Threading or concurrency error. */
  private static final int NATIVE_ERROR_CONCURRENCY = -14;

  /** WASI-related error. */
  private static final int NATIVE_ERROR_WASI = -15;

  /** Component model error. */
  private static final int NATIVE_ERROR_COMPONENT = -16;

  /** Interface definition or binding error. */
  private static final int NATIVE_ERROR_INTERFACE = -17;

  /** Internal system error. */
  private static final int NATIVE_ERROR_INTERNAL = -18;

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

      case NATIVE_ERROR_VALIDATION:
        return new JniException("Validation failed: " + safeMessage, errorCode);

      case NATIVE_ERROR_RUNTIME:
        return new JniException("Runtime error: " + safeMessage, errorCode);

      case NATIVE_ERROR_ENGINE_CONFIG:
        return new JniException("Engine configuration error: " + safeMessage, errorCode);

      case NATIVE_ERROR_STORE:
        return new JniException("Store error: " + safeMessage, errorCode);

      case NATIVE_ERROR_INSTANCE:
        return new JniException("Instance error: " + safeMessage, errorCode);

      case NATIVE_ERROR_MEMORY:
        return new JniResourceException("Memory access error: " + safeMessage, errorCode);

      case NATIVE_ERROR_FUNCTION:
        return new JniException("Function invocation failed: " + safeMessage, errorCode);

      case NATIVE_ERROR_IMPORT_EXPORT:
        return new JniException("Import/Export error: " + safeMessage, errorCode);

      case NATIVE_ERROR_TYPE:
        return new JniException("Type error: " + safeMessage, errorCode);

      case NATIVE_ERROR_RESOURCE:
        return new JniResourceException("Resource error: " + safeMessage, errorCode);

      case NATIVE_ERROR_IO:
        return new JniException("I/O error: " + safeMessage, errorCode);

      case NATIVE_ERROR_INVALID_PARAMETER:
        return new JniException("Invalid parameter: " + safeMessage, errorCode);

      case NATIVE_ERROR_CONCURRENCY:
        return new JniException("Concurrency error: " + safeMessage, errorCode);

      case NATIVE_ERROR_WASI:
        return new JniException("WASI error: " + safeMessage, errorCode);

      case NATIVE_ERROR_COMPONENT:
        return new JniException("Component error: " + safeMessage, errorCode);

      case NATIVE_ERROR_INTERFACE:
        return new JniException("Interface error: " + safeMessage, errorCode);

      case NATIVE_ERROR_INTERNAL:
        return new JniException("Internal error: " + safeMessage, errorCode);

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
      return new JniResourceException(
          "Resource in invalid state: " + exception.getMessage(), exception);
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

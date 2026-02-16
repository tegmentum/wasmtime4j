package ai.tegmentum.wasmtime4j.jni.util;

import ai.tegmentum.wasmtime4j.exception.WasmErrorCode;
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
   * Maps a native error code and message to an appropriate Java exception.
   *
   * @param errorCode the native error code
   * @param message the error message from native code
   * @return the appropriate Java exception
   */
  public static JniException mapNativeError(final int errorCode, final String message) {
    final String safeMessage = message != null ? message : "Unknown native error";
    final WasmErrorCode wasmErrorCode = WasmErrorCode.fromCode(errorCode);

    if (wasmErrorCode == null) {
      LOGGER.warning("Unknown native error code: " + errorCode);
      return new JniException(
          "Unknown native error (code " + errorCode + "): " + safeMessage, errorCode);
    }

    switch (wasmErrorCode) {
      case SUCCESS:
        LOGGER.warning("mapNativeError called with SUCCESS");
        return new JniException("No error occurred", errorCode);

      case COMPILATION_ERROR:
        return new JniException("Compilation failed: " + safeMessage, errorCode);

      case VALIDATION_ERROR:
        return new JniException("Validation failed: " + safeMessage, errorCode);

      case RUNTIME_ERROR:
        return new JniException("Runtime error: " + safeMessage, errorCode);

      case ENGINE_CONFIG_ERROR:
        return new JniException("Engine configuration error: " + safeMessage, errorCode);

      case STORE_ERROR:
        return new JniException("Store error: " + safeMessage, errorCode);

      case INSTANCE_ERROR:
        return new JniException("Instance error: " + safeMessage, errorCode);

      case MEMORY_ERROR:
        return new JniException("Memory access error: " + safeMessage, errorCode);

      case FUNCTION_ERROR:
        return new JniException("Function invocation failed: " + safeMessage, errorCode);

      case IMPORT_EXPORT_ERROR:
        return new JniException("Import/Export error: " + safeMessage, errorCode);

      case TYPE_ERROR:
        return new JniException("Type error: " + safeMessage, errorCode);

      case RESOURCE_ERROR:
        return new JniException("Resource error: " + safeMessage, errorCode);

      case IO_ERROR:
        return new JniException("I/O error: " + safeMessage, errorCode);

      case INVALID_PARAMETER_ERROR:
        return new JniException("Invalid parameter: " + safeMessage, errorCode);

      case CONCURRENCY_ERROR:
        return new JniException("Concurrency error: " + safeMessage, errorCode);

      case WASI_ERROR:
        return new JniException("WASI error: " + safeMessage, errorCode);

      case SECURITY_ERROR:
        return new JniException("Security error: " + safeMessage, errorCode);

      case COMPONENT_ERROR:
        return new JniException("Component error: " + safeMessage, errorCode);

      case INTERFACE_ERROR:
        return new JniException("Interface error: " + safeMessage, errorCode);

      case NETWORK_ERROR:
        return new JniException("Network error: " + safeMessage, errorCode);

      case PROCESS_ERROR:
        return new JniException("Process error: " + safeMessage, errorCode);

      case INTERNAL_ERROR:
        return new JniException("Internal error: " + safeMessage, errorCode);

      case SECURITY_VIOLATION:
        return new JniException("Security violation: " + safeMessage, errorCode);

      case INVALID_DATA:
        return new JniException("Invalid data: " + safeMessage, errorCode);

      case IO_OPERATION_ERROR:
        return new JniException("I/O operation error: " + safeMessage, errorCode);

      case UNSUPPORTED_OPERATION:
        return new JniException("Unsupported operation: " + safeMessage, errorCode);

      case WOULD_BLOCK:
        return new JniException("Would block: " + safeMessage, errorCode);

      default:
        LOGGER.warning("Unhandled WasmErrorCode: " + wasmErrorCode);
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

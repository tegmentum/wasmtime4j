package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;

/**
 * Utility class for mapping native errors to appropriate Java exceptions in Panama implementation.
 *
 * <p>This class provides defensive programming by translating native error codes and messages into
 * meaningful Java exceptions. It ensures that native errors don't propagate as generic runtime
 * errors and provides proper exception hierarchy mapping.
 *
 * <p>The mapper supports various categories of WebAssembly-related exceptions including compilation
 * errors, runtime errors, validation errors, and system errors.
 */
public final class PanamaErrorHandler {

  private static final Logger LOGGER = Logger.getLogger(PanamaErrorHandler.class.getName());

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
  private PanamaErrorHandler() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Gets a human-readable description of a native error code.
   *
   * @param errorCode the native error code
   * @return a description of the error code
   */
  public static String getErrorDescription(final int errorCode) {
    switch (errorCode) {
      case NATIVE_ERROR_NONE:
        return "No Error";
      case NATIVE_ERROR_COMPILATION:
        return "Compilation Error";
      case NATIVE_ERROR_VALIDATION:
        return "Validation Error";
      case NATIVE_ERROR_RUNTIME:
        return "Runtime Error";
      case NATIVE_ERROR_ENGINE_CONFIG:
        return "Engine Configuration Error";
      case NATIVE_ERROR_STORE:
        return "Store Error";
      case NATIVE_ERROR_INSTANCE:
        return "Instance Error";
      case NATIVE_ERROR_MEMORY:
        return "Memory Access Error";
      case NATIVE_ERROR_FUNCTION:
        return "Function Error";
      case NATIVE_ERROR_IMPORT_EXPORT:
        return "Import/Export Error";
      case NATIVE_ERROR_TYPE:
        return "Type Error";
      case NATIVE_ERROR_RESOURCE:
        return "Resource Error";
      case NATIVE_ERROR_IO:
        return "I/O Error";
      case NATIVE_ERROR_INVALID_PARAMETER:
        return "Invalid Parameter";
      case NATIVE_ERROR_CONCURRENCY:
        return "Concurrency Error";
      case NATIVE_ERROR_WASI:
        return "WASI Error";
      case NATIVE_ERROR_COMPONENT:
        return "Component Error";
      case NATIVE_ERROR_INTERFACE:
        return "Interface Error";
      case NATIVE_ERROR_INTERNAL:
        return "Internal Error";
      default:
        LOGGER.warning("Unknown native error code: " + errorCode);
        return "Unknown Error (code " + errorCode + ")";
    }
  }

  /**
   * Checks if an error code represents a recoverable error.
   *
   * @param errorCode the native error code
   * @return true if the error is recoverable, false otherwise
   */
  public static boolean isRecoverableError(final int errorCode) {
    switch (errorCode) {
      case NATIVE_ERROR_MEMORY:
      case NATIVE_ERROR_RESOURCE:
      case NATIVE_ERROR_INVALID_PARAMETER:
      case NATIVE_ERROR_IO:
        return true;
      case NATIVE_ERROR_COMPILATION:
      case NATIVE_ERROR_VALIDATION:
      case NATIVE_ERROR_INTERNAL:
        return false;
      default:
        return false;
    }
  }

  /**
   * Checks an error code and throws an appropriate exception if it indicates an error.
   *
   * @param errorCode the error code to check
   * @param operation the operation description for error messages
   * @throws WasmException if the error code indicates an error
   */
  public static void checkErrorCode(final int errorCode, final String operation) throws WasmException {
    if (errorCode != NATIVE_ERROR_NONE) {
      final String description = getErrorDescription(errorCode);
      final String message = operation != null ? operation + ": " + description : description;
      throw new WasmException(message);
    }
  }

  /**
   * Checks an error struct and throws an appropriate exception if it indicates an error.
   *
   * @param errorStruct the error struct to check (may be null)
   * @param operation the operation description for error messages
   * @param defaultMessage the default message if error struct is null
   * @throws WasmException if the error struct indicates an error
   */
  public static void checkErrorStruct(final Object errorStruct, final String operation,
      final String defaultMessage) throws WasmException {
    // For Panama implementation, this would typically check a native error struct
    // For now, we assume null means no error
    if (errorStruct != null) {
      final String message = operation != null ? operation + ": " + defaultMessage : defaultMessage;
      throw new WasmException(message != null ? message : "Unknown error");
    }
  }

  /**
   * Creates a detailed error message from components.
   *
   * @param operation the operation that failed (may be null)
   * @param errorCode the error code (may be null)
   * @param nativeMessage the native error message (may be null)
   * @return a detailed error message
   */
  public static String createDetailedErrorMessage(final String operation, final Integer errorCode,
      final String nativeMessage) {
    final StringBuilder message = new StringBuilder();
    
    if (operation != null && !operation.trim().isEmpty()) {
      message.append(operation).append(": ");
    }
    
    if (errorCode != null) {
      message.append(getErrorDescription(errorCode));
      if (nativeMessage != null && !nativeMessage.trim().isEmpty()) {
        message.append(" - ").append(nativeMessage);
      }
    } else if (nativeMessage != null && !nativeMessage.trim().isEmpty()) {
      message.append(nativeMessage);
    } else {
      message.append("Unknown error");
    }
    
    return message.toString();
  }

  /**
   * Creates a detailed error message from string components.
   *
   * @param operation the operation that failed (may be null)
   * @param context the operation context (may be null)
   * @param nativeMessage the native error message (may be null)
   * @return a detailed error message
   */
  public static String createDetailedErrorMessage(final String operation, final String context,
      final String nativeMessage) {
    final StringBuilder message = new StringBuilder();
    
    if (operation != null && !operation.trim().isEmpty()) {
      message.append(operation);
      if (context != null && !context.trim().isEmpty()) {
        message.append(" (").append(context).append(")");
      }
      message.append(": ");
    }
    
    if (nativeMessage != null && !nativeMessage.trim().isEmpty()) {
      message.append(nativeMessage);
    } else {
      message.append("Unknown error");
    }
    
    return message.toString();
  }

  /**
   * Validates that a value is non-negative.
   *
   * @param value the value to check
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the value is negative
   */
  public static void requireNonNegative(final long value, final String parameterName) {
    if (value < 0) {
      throw new IllegalArgumentException(parameterName + " must be non-negative, got: " + value);
    }
  }

  /**
   * Validates that a string is not null or empty.
   *
   * @param value the string to check
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the string is null or empty
   */
  public static void requireNotEmpty(final String value, final String parameterName) {
    if (value == null) {
      throw new IllegalArgumentException(parameterName + " cannot be null");
    }
    if (value.trim().isEmpty()) {
      throw new IllegalArgumentException(parameterName + " cannot be empty");
    }
  }

  /**
   * Validates that a string is not null or empty (alias for requireNotEmpty).
   *
   * @param value the string to check
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the string is null or empty
   */
  public static void requireNonEmpty(final String value, final String parameterName) {
    requireNotEmpty(value, parameterName);
  }

  /**
   * Validates that a memory segment is not null.
   *
   * @param segment the memory segment to check
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the segment is null
   */
  public static void requireValidPointer(final MemorySegment segment, final String parameterName) {
    if (segment == null) {
      throw new IllegalArgumentException(parameterName + " cannot be null");
    }
  }

  /**
   * Validates that a value is positive.
   *
   * @param value the value to check
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the value is not positive
   */
  public static void requirePositive(final long value, final String parameterName) {
    if (value <= 0) {
      throw new IllegalArgumentException(parameterName + " must be positive, got: " + value);
    }
  }

  /**
   * Safely checks an error code and throws appropriate exception.
   *
   * @param errorCode the error code to check
   * @param operation the operation description
   * @param context additional context information
   * @throws WasmException if error code indicates an error
   */
  public static void safeCheckError(final int errorCode, final String operation,
      final String context) throws WasmException {
    if (errorCode != NATIVE_ERROR_NONE) {
      final String description = getErrorDescription(errorCode);
      final String message = createDetailedErrorMessage(operation, errorCode, context);
      throw new WasmException(message);
    }
  }

  /**
   * Maps a native error to a WasmException.
   *
   * @param errorCode the native error code
   * @param message the error message
   * @return a WasmException with appropriate details
   */
  public static WasmException mapToWasmException(final int errorCode, final String message) {
    final String description = getErrorDescription(errorCode);
    final String detailedMessage = createDetailedErrorMessage(null, errorCode, message);
    return new WasmException(detailedMessage);
  }
}
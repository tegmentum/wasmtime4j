/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaExceptionMapper;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Error handling integration for Panama FFI implementation.
 *
 * <p>This class provides comprehensive error handling by mapping native error codes and messages to
 * appropriate Java exceptions. It ensures consistent error reporting across all Panama FFI
 * operations while providing detailed error information.
 *
 * <p>The error handler supports both simple error code mapping and detailed error structure
 * processing for complete error information extraction.
 */
public final class PanamaErrorHandler {

  private static final Logger LOGGER = Logger.getLogger(PanamaErrorHandler.class.getName());

  // Native error codes (matching Wasmtime C API)
  private static final int WASMTIME_SUCCESS = 0;
  private static final int WASMTIME_ERROR_GENERIC = -1;
  private static final int WASMTIME_ERROR_COMPILATION = -2;
  private static final int WASMTIME_ERROR_VALIDATION = -3;
  private static final int WASMTIME_ERROR_RUNTIME = -4;
  private static final int WASMTIME_ERROR_MEMORY = -5;
  private static final int WASMTIME_ERROR_INVALID_ARGUMENT = -6;
  private static final int WASMTIME_ERROR_UNSUPPORTED = -7;
  private static final int WASMTIME_ERROR_TRAP = -8;

  // Default error messages
  private static final String DEFAULT_GENERIC_ERROR = "Native operation failed";
  private static final String DEFAULT_COMPILATION_ERROR = "WebAssembly compilation failed";
  private static final String DEFAULT_VALIDATION_ERROR = "WebAssembly validation failed";
  private static final String DEFAULT_RUNTIME_ERROR = "WebAssembly runtime error";
  private static final String DEFAULT_MEMORY_ERROR = "Memory allocation or access error";
  private static final String DEFAULT_INVALID_ARGUMENT = "Invalid argument provided";
  private static final String DEFAULT_UNSUPPORTED = "Unsupported operation";
  private static final String DEFAULT_TRAP_ERROR = "WebAssembly trap occurred";

  // Private constructor to prevent instantiation
  private PanamaErrorHandler() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Checks a native error code and throws appropriate exception if error occurred.
   *
   * @param errorCode the native error code to check
   * @param operation description of the operation that produced the error
   * @throws WasmException if the error code indicates failure
   */
  public static void checkErrorCode(final int errorCode, final String operation)
      throws WasmException {
    if (errorCode == WASMTIME_SUCCESS) {
      return; // No error
    }

    String message = getDefaultErrorMessage(errorCode);
    if (operation != null && !operation.trim().isEmpty()) {
      message = operation + ": " + message;
    }

    WasmException exception = createExceptionFromCode(errorCode, message);
    LOGGER.log(Level.WARNING, "Native operation failed: " + message + " (code=" + errorCode + ")");
    throw exception;
  }

  /**
   * Checks a native error code and throws appropriate exception with custom message.
   *
   * @param errorCode the native error code to check
   * @param customMessage custom error message to use
   * @throws WasmException if the error code indicates failure
   */
  public static void checkErrorCode(
      final int errorCode, final String customMessage, final Object... messageArgs)
      throws WasmException {
    if (errorCode == WASMTIME_SUCCESS) {
      return; // No error
    }

    String formattedMessage = customMessage;
    if (messageArgs.length > 0) {
      formattedMessage = String.format(customMessage, messageArgs);
    }

    WasmException exception = createExceptionFromCode(errorCode, formattedMessage);
    LOGGER.log(
        Level.WARNING,
        "Native operation failed: " + formattedMessage + " (code=" + errorCode + ")");
    throw exception;
  }

  /**
   * Processes a native error structure and throws appropriate exception.
   *
   * @param errorStructPtr pointer to the native error structure
   * @param operation description of the operation that produced the error
   * @param resourceManager resource manager for memory operations
   * @throws WasmException if the error structure indicates failure
   */
  public static void checkErrorStruct(
      final MemorySegment errorStructPtr,
      final String operation,
      final ArenaResourceManager resourceManager)
      throws WasmException {
    if (errorStructPtr == null || errorStructPtr.equals(MemorySegment.NULL)) {
      return; // No error
    }

    try {
      // Extract error information from the structure
      int errorCode = (int) MemoryLayouts.WASMTIME_ERROR_CODE.get(errorStructPtr, 0);
      MemorySegment messagePtr =
          (MemorySegment) MemoryLayouts.WASMTIME_ERROR_MESSAGE.get(errorStructPtr, 0);
      long messageLen = (long) MemoryLayouts.WASMTIME_ERROR_MESSAGE_LEN.get(errorStructPtr, 0);

      String message = extractErrorMessage(messagePtr, messageLen, resourceManager, errorCode);

      if (operation != null && !operation.trim().isEmpty()) {
        message = operation + ": " + message;
      }

      WasmException exception = createExceptionFromCode(errorCode, message);
      LOGGER.log(
          Level.WARNING, "Native operation failed: " + message + " (code=" + errorCode + ")");
      throw exception;

    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e; // Re-throw WasmExceptions directly
      } else {
        // Error processing the error structure itself
        String fallbackMessage =
            operation != null ? operation + ": " + DEFAULT_GENERIC_ERROR : DEFAULT_GENERIC_ERROR;
        LOGGER.log(Level.SEVERE, "Failed to process error structure", e);
        throw new IllegalStateException(fallbackMessage, e);
      }
    }
  }

  /**
   * Creates a safe error check wrapper for native operations.
   *
   * @param errorCode the error code to check
   * @param operation description of the operation
   * @param fallbackMessage fallback message if error processing fails
   */
  public static void safeCheckError(
      final int errorCode, final String operation, final String fallbackMessage)
      throws WasmException {
    try {
      checkErrorCode(errorCode, operation);
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      } else {
        // Fallback for unexpected errors
        LOGGER.log(Level.SEVERE, "Error during error checking", e);
        throw new IllegalStateException(
            fallbackMessage != null ? fallbackMessage : DEFAULT_GENERIC_ERROR, e);
      }
    }
  }

  /**
   * Maps a native error code to an appropriate Java exception type.
   *
   * @param errorCode the native error code
   * @param message the error message
   * @return appropriate exception instance
   */
  private static WasmException createExceptionFromCode(final int errorCode, final String message) {
    return switch (errorCode) {
      case WASMTIME_ERROR_COMPILATION -> new CompilationException(message);
      case WASMTIME_ERROR_VALIDATION -> new ValidationException(message);
      case WASMTIME_ERROR_RUNTIME, WASMTIME_ERROR_TRAP -> new ai.tegmentum.wasmtime4j.exception
          .RuntimeException(message);
      case WASMTIME_ERROR_MEMORY,
          WASMTIME_ERROR_INVALID_ARGUMENT,
          WASMTIME_ERROR_UNSUPPORTED -> new ai.tegmentum.wasmtime4j.exception.RuntimeException(
          message);
      default -> new ai.tegmentum.wasmtime4j.exception.RuntimeException(message);
    };
  }

  /**
   * Gets the default error message for an error code.
   *
   * @param errorCode the error code
   * @return default error message
   */
  private static String getDefaultErrorMessage(final int errorCode) {
    return switch (errorCode) {
      case WASMTIME_ERROR_COMPILATION -> DEFAULT_COMPILATION_ERROR;
      case WASMTIME_ERROR_VALIDATION -> DEFAULT_VALIDATION_ERROR;
      case WASMTIME_ERROR_RUNTIME -> DEFAULT_RUNTIME_ERROR;
      case WASMTIME_ERROR_MEMORY -> DEFAULT_MEMORY_ERROR;
      case WASMTIME_ERROR_INVALID_ARGUMENT -> DEFAULT_INVALID_ARGUMENT;
      case WASMTIME_ERROR_UNSUPPORTED -> DEFAULT_UNSUPPORTED;
      case WASMTIME_ERROR_TRAP -> DEFAULT_TRAP_ERROR;
      default -> DEFAULT_GENERIC_ERROR;
    };
  }

  /**
   * Extracts error message from native memory.
   *
   * @param messagePtr pointer to the error message string
   * @param messageLen length of the error message
   * @param resourceManager resource manager for memory operations
   * @param errorCode error code for fallback message
   * @return extracted or fallback error message
   */
  private static String extractErrorMessage(
      final MemorySegment messagePtr,
      final long messageLen,
      final ArenaResourceManager resourceManager,
      final int errorCode) {
    if (messagePtr == null || messagePtr.equals(MemorySegment.NULL) || messageLen <= 0) {
      return getDefaultErrorMessage(errorCode);
    }

    try {
      // Safely read the error message from native memory
      MemorySegment messageSegment = messagePtr.reinterpret(messageLen);
      byte[] messageBytes = messageSegment.toArray(ValueLayout.JAVA_BYTE);

      // Convert to string, handling potential encoding issues
      String message = new String(messageBytes, java.nio.charset.StandardCharsets.UTF_8).trim();

      if (message.isEmpty()) {
        return getDefaultErrorMessage(errorCode);
      }

      return message;

    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to extract native error message", e);
      return getDefaultErrorMessage(errorCode);
    }
  }

  /**
   * Validates that an operation result indicates success.
   *
   * @param result the operation result
   * @param operation description of the operation
   * @return the result if successful
   * @throws RuntimeException if the result indicates failure
   */
  public static <T> T requireSuccess(final T result, final String operation) {
    if (result == null) {
      String message =
          operation != null ? operation + ": Operation returned null" : "Operation returned null";
      throw new IllegalArgumentException(message);
    }
    return result;
  }

  /**
   * Validates that a pointer is not null.
   *
   * @param pointer the pointer to validate
   * @param paramName the parameter name for error messages
   * @return the pointer if valid
   * @throws RuntimeException if the pointer is null
   */
  public static MemorySegment requireValidPointer(
      final MemorySegment pointer, final String paramName) {
    if (pointer == null || pointer.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException(paramName + " cannot be null");
    }
    return pointer;
  }

  /**
   * Validates that a size or index is within valid bounds.
   *
   * @param value the value to validate
   * @param paramName the parameter name for error messages
   * @return the value if valid
   * @throws RuntimeException if the value is invalid
   */
  public static long requirePositive(final long value, final String paramName) {
    if (value <= 0) {
      throw new IllegalArgumentException(paramName + " must be positive: " + value);
    }
    return value;
  }

  /**
   * Validates that an index is within bounds.
   *
   * @param index the index to validate
   * @param size the size of the collection
   * @param paramName the parameter name for error messages
   * @return the index if valid
   * @throws RuntimeException if the index is out of bounds
   */
  public static int requireValidIndex(final int index, final int size, final String paramName) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException(
          paramName + " out of bounds: " + index + " (size=" + size + ")");
    }
    return index;
  }

  /**
   * Creates a detailed error message with context.
   *
   * @param operation the operation that failed
   * @param context additional context information
   * @param cause the underlying cause
   * @return formatted error message
   */
  public static String createDetailedErrorMessage(
      final String operation, final String context, final String cause) {
    StringBuilder sb = new StringBuilder();

    if (operation != null && !operation.trim().isEmpty()) {
      sb.append(operation);
    }

    if (context != null && !context.trim().isEmpty()) {
      if (sb.length() > 0) {
        sb.append(" (").append(context).append(")");
      } else {
        sb.append(context);
      }
    }

    if (cause != null && !cause.trim().isEmpty()) {
      if (sb.length() > 0) {
        sb.append(": ").append(cause);
      } else {
        sb.append(cause);
      }
    }

    return sb.length() > 0 ? sb.toString() : DEFAULT_GENERIC_ERROR;
  }

  /**
   * Gets a user-friendly error description for an error code.
   *
   * @param errorCode the error code
   * @return user-friendly description
   */
  public static String getErrorDescription(final int errorCode) {
    return switch (errorCode) {
      case WASMTIME_SUCCESS -> "Success";
      case WASMTIME_ERROR_COMPILATION -> "Compilation Error";
      case WASMTIME_ERROR_VALIDATION -> "Validation Error";
      case WASMTIME_ERROR_RUNTIME -> "Runtime Error";
      case WASMTIME_ERROR_MEMORY -> "Memory Error";
      case WASMTIME_ERROR_INVALID_ARGUMENT -> "Invalid Argument";
      case WASMTIME_ERROR_UNSUPPORTED -> "Unsupported Operation";
      case WASMTIME_ERROR_TRAP -> "WebAssembly Trap";
      default -> "Unknown Error (" + errorCode + ")";
    };
  }

  /**
   * Checks if an error code represents a recoverable error.
   *
   * @param errorCode the error code to check
   * @return true if the error might be recoverable, false otherwise
   */
  public static boolean isRecoverableError(final int errorCode) {
    return switch (errorCode) {
      case WASMTIME_ERROR_MEMORY, WASMTIME_ERROR_INVALID_ARGUMENT -> true;
      default -> false;
    };
  }

  /**
   * Maps a throwable to an appropriate WebAssembly exception with context.
   *
   * @param throwable the throwable to map
   * @param context additional context information
   * @return the mapped WebAssembly exception
   */
  public static WasmException mapToWasmException(final Throwable throwable, final String context) {
    if (throwable instanceof WasmException) {
      return (WasmException) throwable;
    }

    // Use PanamaExceptionMapper for consistent exception mapping
    final PanamaExceptionMapper mapper = new PanamaExceptionMapper();
    final WasmException mappedException = mapper.mapException(throwable);

    // Add context if provided
    if (context != null && !context.trim().isEmpty()) {
      final String message = context + ": " + mappedException.getMessage();
      return switch (mappedException) {
        case CompilationException ce -> new CompilationException(message, mappedException);
        case ValidationException ve -> new ValidationException(message, mappedException);
        case ai.tegmentum.wasmtime4j.exception.RuntimeException re -> new ai.tegmentum.wasmtime4j
            .exception.RuntimeException(message, mappedException);
        default -> new WasmException(message, mappedException);
      };
    }

    return mappedException;
  }

  /**
   * Maps an exception to an appropriate WebAssembly exception.
   *
   * @param exception the exception to map
   * @param context additional context information
   * @return the mapped WebAssembly exception
   */
  public static WasmException mapToWasmException(final Exception exception, final String context) {
    return mapToWasmException((Throwable) exception, context);
  }

  /**
   * Validates that a numeric value is non-negative.
   *
   * @param value the value to validate
   * @param paramName the parameter name for error messages
   * @return the value if valid
   * @throws IllegalArgumentException if the value is negative
   */
  public static long requireNonNegative(final long value, final String paramName) {
    if (value < 0) {
      throw new IllegalArgumentException(paramName + " must be non-negative: " + value);
    }
    return value;
  }

  /**
   * Validates that a numeric value is non-negative.
   *
   * @param value the value to validate
   * @param paramName the parameter name for error messages
   * @return the value if valid
   * @throws IllegalArgumentException if the value is negative
   */
  public static int requireNonNegative(final int value, final String paramName) {
    if (value < 0) {
      throw new IllegalArgumentException(paramName + " must be non-negative: " + value);
    }
    return value;
  }

  /**
   * Validates that a string is not null or empty.
   *
   * @param value the string to validate
   * @param paramName the parameter name for error messages
   * @return the value if valid
   * @throws IllegalArgumentException if the string is null or empty
   */
  public static String requireNotEmpty(final String value, final String paramName) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(paramName + " cannot be null or empty");
    }
    return value;
  }
}

/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama.util;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.TrapException;
import ai.tegmentum.wasmtime4j.exception.TrapException.TrapType;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import java.lang.foreign.MemorySegment;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exception mapping utility for Panama FFI operations.
 *
 * <p>This class maps native errors and Java exceptions to the appropriate WebAssembly exception
 * hierarchy. It provides centralized exception handling and ensures consistent error reporting
 * across the Panama implementation.
 *
 * <p>The mapper handles both exceptions thrown by FFI operations and native error codes returned by
 * Wasmtime functions.
 *
 * @since 1.0.0
 */
public final class PanamaExceptionMapper {
  private static final Logger logger = Logger.getLogger(PanamaExceptionMapper.class.getName());

  /** Creates a new Panama exception mapper. */
  public PanamaExceptionMapper() {
    logger.fine("Created Panama exception mapper");
  }

  /**
   * Maps a Java throwable to the appropriate WebAssembly exception.
   *
   * <p>This method examines the input throwable and maps it to the most appropriate WebAssembly
   * exception type based on the throwable type and message content.
   *
   * @param exception the throwable to map
   * @return the mapped WebAssembly exception
   */
  public static WasmException mapException(final Throwable exception) {
    if (exception == null) {
      return new WasmException("Unknown error occurred");
    }

    // If it's already a WebAssembly exception, return as-is
    if (exception instanceof WasmException) {
      return (WasmException) exception;
    }

    final String message = exception.getMessage();
    final String exceptionType = exception.getClass().getSimpleName();

    logger.fine("Mapping exception: " + exceptionType + " - " + message);

    // Map specific exception types
    if (exception instanceof OutOfMemoryError) {
      return new WasmException("Out of memory in native runtime", exception);
    }

    if (exception instanceof IllegalArgumentException) {
      return new ValidationException("Invalid argument: " + message, exception);
    }

    if (exception instanceof IllegalStateException) {
      return new RuntimeException("Invalid state: " + message, exception);
    }

    if (exception instanceof SecurityException) {
      return new WasmException("Security violation: " + message, exception);
    }

    if (exception instanceof UnsupportedOperationException) {
      return new WasmException("Unsupported operation: " + message, exception);
    }

    // Map based on message content patterns
    if (message != null) {
      // Try to detect and map trap exceptions using native introspection
      final WasmException trapException = tryMapTrapException(message, exception);
      if (trapException != null) {
        return trapException;
      }

      final String lowerMessage = message.toLowerCase();

      if (lowerMessage.contains("compilation") || lowerMessage.contains("compile")) {
        return new CompilationException("Compilation error: " + message, exception);
      }

      if (lowerMessage.contains("validation") || lowerMessage.contains("invalid")) {
        return new ValidationException("Validation error: " + message, exception);
      }

      if (lowerMessage.contains("runtime") || lowerMessage.contains("execution")) {
        return new RuntimeException("Runtime error: " + message, exception);
      }

      if (lowerMessage.contains("memory") || lowerMessage.contains("allocation")) {
        return new WasmException("Memory error: " + message, exception);
      }

      if (lowerMessage.contains("function") || lowerMessage.contains("call")) {
        return new RuntimeException("Function call error: " + message, exception);
      }
    }

    // Default mapping
    return new WasmException("Native runtime error: " + message, exception);
  }

  /**
   * Attempts to map an exception to a TrapException using native trap introspection.
   *
   * <p>This method uses the native trap introspection APIs to detect if the error message indicates
   * a WebAssembly trap and extract detailed trap information including trap type, function name,
   * and instruction offset.
   *
   * @param message the error message to analyze
   * @param cause the underlying exception
   * @return a TrapException if the message indicates a trap, null otherwise
   */
  private static WasmException tryMapTrapException(final String message, final Throwable cause) {
    if (message == null || message.isEmpty()) {
      return null;
    }

    try {
      final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();

      // Check if this is a trap message
      if (!bindings.trapIsTrap(message)) {
        return null;
      }

      // Extract trap information using native introspection
      final NativeFunctionBindings.TrapInfo trapInfo = bindings.trapExtractInfo(message);
      if (trapInfo == null) {
        return null;
      }

      // Map native trap code to TrapType enum
      final TrapType trapType = mapTrapCodeToTrapType(trapInfo.getTrapCode());

      // Extract instruction offset (convert -1 to null)
      final Integer instructionOffset =
          trapInfo.getInstructionOffset() >= 0 ? (int) trapInfo.getInstructionOffset() : null;

      // Try to extract function name from backtrace (if present in message)
      final String functionName = extractFunctionNameFromMessage(message, bindings);

      // Create TrapException with full details
      return new TrapException(
          trapType,
          message,
          null, // wasmBacktrace - could extract full backtrace if needed
          functionName,
          instructionOffset,
          cause);
    } catch (Exception e) {
      logger.log(Level.FINE, "Failed to perform trap introspection: " + e.getMessage(), e);
      return null;
    }
  }

  /**
   * Maps a native trap code to the corresponding TrapType enum value.
   *
   * <p>The native trap codes are designed to match the ordinal values of the TrapType enum.
   *
   * @param trapCode the native trap code
   * @return the corresponding TrapType
   */
  private static TrapType mapTrapCodeToTrapType(final int trapCode) {
    final TrapType[] values = TrapType.values();
    if (trapCode >= 0 && trapCode < values.length) {
      return values[trapCode];
    }
    return TrapType.UNKNOWN;
  }

  /**
   * Attempts to extract a function name from the error message.
   *
   * <p>This method looks for backtrace lines in the message and uses native introspection to
   * extract function names.
   *
   * @param message the error message
   * @param bindings the native function bindings
   * @return the function name if found, null otherwise
   */
  private static String extractFunctionNameFromMessage(
      final String message, final NativeFunctionBindings bindings) {
    // Look for backtrace lines containing function information
    // Format: "   0: 0x... - <module>!<function>"
    final String[] lines = message.split("\n");
    for (final String line : lines) {
      final String trimmed = line.trim();
      if (trimmed.matches("^\\d+:.*")) {
        final String funcName = bindings.trapExtractFunctionName(trimmed);
        if (funcName != null && !funcName.isEmpty()) {
          return funcName;
        }
      }
    }
    return null;
  }

  /**
   * Maps a native error code to a WebAssembly exception.
   *
   * <p>This method interprets Wasmtime error codes and creates appropriate WebAssembly exceptions.
   * Error codes are typically returned by native Wasmtime functions to indicate various failure
   * conditions.
   *
   * @param errorCode the native error code
   * @param errorMessage additional error message, if available
   * @return the mapped WebAssembly exception
   */
  public WasmException mapNativeError(final int errorCode, final String errorMessage) {
    final String message = errorMessage != null ? errorMessage : "Native error code: " + errorCode;

    logger.fine("Mapping native error code " + errorCode + ": " + message);

    // Map common Wasmtime error codes
    // TODO: Define actual Wasmtime error code constants
    switch (errorCode) {
      case 0:
        // Success - this shouldn't be called for success cases
        logger.warning("mapNativeError called with success code 0");
        return new WasmException("Unexpected success code");

      case 1:
        return new CompilationException("Module compilation failed: " + message);

      case 2:
        return new ValidationException("Module validation failed: " + message);

      case 3:
        return new RuntimeException("Runtime execution failed: " + message);

      case 4:
        return new WasmException("Memory allocation failed: " + message);

      case 5:
        return new RuntimeException("Function call failed: " + message);

      case 6:
        return new WasmException("Resource limit exceeded: " + message);

      case 7:
        return new WasmException("Import resolution failed: " + message);

      case 8:
        return new WasmException("Export not found: " + message);

      default:
        return new WasmException("Unknown native error (code " + errorCode + "): " + message);
    }
  }

  /**
   * Maps a native error pointer to a WebAssembly exception.
   *
   * <p>Many Wasmtime functions return error pointers instead of error codes. This method extracts
   * error information from such pointers and creates appropriate exceptions.
   *
   * @param errorPtr the native error pointer
   * @return the mapped WebAssembly exception, or null if no error
   */
  public WasmException mapNativeError(final MemorySegment errorPtr) {
    if (errorPtr == null || errorPtr == MemorySegment.NULL) {
      return null; // No error
    }

    try {
      // TODO: Implement error pointer interpretation
      // This would extract error type and message from the native error structure
      logger.fine("Mapping native error pointer: " + errorPtr.address());

      // For now, create a generic error
      return new WasmException("Native error occurred (error pointer: " + errorPtr.address() + ")");
    } catch (Exception e) {
      logger.warning("Failed to interpret native error pointer: " + e.getMessage());
      return new WasmException("Failed to interpret native error", e);
    }
  }

  /**
   * Creates a compilation exception with additional context.
   *
   * @param message the error message
   * @param cause the underlying cause, if any
   * @return a new compilation exception
   */
  public CompilationException createCompilationException(
      final String message, final Throwable cause) {
    return new CompilationException("Panama FFI: " + message, cause);
  }

  /**
   * Creates a runtime exception with additional context.
   *
   * @param message the error message
   * @param cause the underlying cause, if any
   * @return a new runtime exception
   */
  public RuntimeException createRuntimeException(final String message, final Throwable cause) {
    return new RuntimeException("Panama FFI: " + message, cause);
  }

  /**
   * Creates a validation exception with additional context.
   *
   * @param message the error message
   * @param cause the underlying cause, if any
   * @return a new validation exception
   */
  public ValidationException createValidationException(
      final String message, final Throwable cause) {
    return new ValidationException("Panama FFI: " + message, cause);
  }

  /**
   * Creates a generic WebAssembly exception with additional context.
   *
   * @param message the error message
   * @param cause the underlying cause, if any
   * @return a new WebAssembly exception
   */
  public WasmException createWasmException(final String message, final Throwable cause) {
    return new WasmException("Panama FFI: " + message, cause);
  }

  /**
   * Checks if an exception indicates a recoverable error.
   *
   * <p>Some errors may be recoverable or represent expected failure conditions (e.g., function not
   * found). This method helps identify such cases.
   *
   * @param exception the exception to check
   * @return true if the error may be recoverable, false otherwise
   */
  public boolean isRecoverableError(final WasmException exception) {
    if (exception == null) {
      return false;
    }

    // Validation errors are typically not recoverable
    if (exception instanceof ValidationException) {
      return false;
    }

    // Compilation errors are typically not recoverable
    if (exception instanceof CompilationException) {
      return false;
    }

    final String message = exception.getMessage();
    if (message != null) {
      final String lowerMessage = message.toLowerCase();

      // These types of errors might be recoverable
      if (lowerMessage.contains("not found")
          || lowerMessage.contains("unavailable")
          || lowerMessage.contains("timeout")) {
        return true;
      }

      // These are typically not recoverable
      if (lowerMessage.contains("out of memory")
          || lowerMessage.contains("corruption")
          || lowerMessage.contains("invalid")) {
        return false;
      }
    }

    // Runtime exceptions might be recoverable depending on context
    return exception instanceof RuntimeException;
  }
}

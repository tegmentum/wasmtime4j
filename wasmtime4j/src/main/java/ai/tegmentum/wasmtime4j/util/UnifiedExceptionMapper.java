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

package ai.tegmentum.wasmtime4j.util;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.InstantiationException;
import ai.tegmentum.wasmtime4j.exception.ResourceException;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.SecurityException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasiComponentException;
import ai.tegmentum.wasmtime4j.exception.WasiConfigurationException;
import ai.tegmentum.wasmtime4j.exception.WasiException;
import ai.tegmentum.wasmtime4j.exception.WasiResourceException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Unified exception mapper that converts implementation-specific exceptions to public API
 * exceptions.
 *
 * <p>This utility class provides methods to map JNI and Panama implementation exceptions to the
 * public WebAssembly API exception hierarchy. It ensures consistent error handling across different
 * runtime implementations while preserving stack trace information and error context.
 *
 * <p>The mapper supports error recovery by identifying which exceptions may be recoverable and
 * provides appropriate exception chaining to preserve error information across the mapping
 * boundary.
 *
 * @since 1.0.0
 */
public final class UnifiedExceptionMapper {
  private static final Logger LOGGER = Logger.getLogger(UnifiedExceptionMapper.class.getName());

  /** Private constructor to prevent instantiation of utility class. */
  private UnifiedExceptionMapper() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Maps any exception to the appropriate public API WebAssembly exception.
   *
   * <p>This method examines the input exception and maps it to the most appropriate public API
   * exception type. It preserves stack traces and error context while providing a consistent
   * exception hierarchy for users of the public API.
   *
   * @param exception the exception to map (may be null)
   * @return the mapped WebAssembly exception (never null)
   */
  public static WasmException mapToPublicException(final Throwable exception) {
    if (exception == null) {
      return new WasmException("Unknown error occurred");
    }

    // If it's already a public API exception, return as-is
    if (exception instanceof WasmException) {
      return (WasmException) exception;
    }

    final String message = exception.getMessage();
    final String exceptionType = exception.getClass().getSimpleName();

    LOGGER.fine("Mapping exception to public API: " + exceptionType + " - " + message);

    // Map implementation-specific exceptions by package/class name patterns
    final String className = exception.getClass().getName();

    if (className.contains(".jni.exception.") || className.contains(".panama.")) {
      return mapImplementationException(exception);
    }

    // Map standard Java exceptions based on type and content
    return mapStandardException(exception);
  }

  /**
   * Maps implementation-specific exceptions (JNI or Panama) to public API exceptions.
   *
   * @param exception the implementation-specific exception
   * @return the mapped public API exception
   */
  private static WasmException mapImplementationException(final Throwable exception) {
    final String className = exception.getClass().getSimpleName();
    final String message = exception.getMessage();

    // Map JNI-specific exceptions
    if (className.contains("Jni")) {
      return mapJniException(exception, message);
    }

    // Map Panama-specific exceptions
    if (className.contains("Panama")) {
      return mapPanamaException(exception, message);
    }

    // Map based on message content for unknown implementation exceptions
    return mapByMessageContent(exception, message);
  }

  /**
   * Maps JNI-specific exceptions to public API exceptions.
   *
   * @param exception the JNI exception
   * @param message the exception message
   * @return the mapped public API exception
   */
  private static WasmException mapJniException(final Throwable exception, final String message) {
    final String className = exception.getClass().getSimpleName();

    if (className.contains("Resource")) {
      return new ResourceException("Resource error: " + message, exception);
    }

    if (className.contains("Validation")) {
      return new ValidationException("Validation error: " + message, exception);
    }

    if (className.contains("Library")) {
      return new ResourceException("Native library error: " + message, exception);
    }

    // Default JNI mapping based on message content
    return mapByMessageContent(exception, message);
  }

  /**
   * Maps Panama-specific exceptions to public API exceptions.
   *
   * @param exception the Panama exception
   * @param message the exception message
   * @return the mapped public API exception
   */
  private static WasmException mapPanamaException(final Throwable exception, final String message) {
    // Panama exceptions are typically already properly mapped in PanamaExceptionMapper
    // but this provides a fallback
    return mapByMessageContent(exception, message);
  }

  /**
   * Maps standard Java exceptions to public API exceptions.
   *
   * @param exception the standard Java exception
   * @return the mapped public API exception
   */
  private static WasmException mapStandardException(final Throwable exception) {
    final String message = exception.getMessage();

    // Map specific exception types
    if (exception instanceof IllegalArgumentException) {
      return new ValidationException("Invalid argument: " + message, exception);
    }

    if (exception instanceof IllegalStateException) {
      return new RuntimeException("Invalid state: " + message, exception);
    }

    if (exception instanceof OutOfMemoryError) {
      return new ResourceException("Out of memory: " + message, exception);
    }

    if (exception instanceof java.lang.SecurityException) {
      return new SecurityException("Security violation: " + message, exception);
    }

    if (exception instanceof NullPointerException) {
      return new ValidationException("Null pointer error: " + message, exception);
    }

    if (exception instanceof IndexOutOfBoundsException) {
      return new ValidationException("Index out of bounds: " + message, exception);
    }

    if (exception instanceof ClassNotFoundException || exception instanceof NoSuchMethodException) {
      return new ResourceException("Implementation not available: " + message, exception);
    }

    // Map based on message content for unknown exception types
    return mapByMessageContent(exception, message);
  }

  /**
   * Maps exceptions based on message content patterns.
   *
   * @param exception the original exception
   * @param message the exception message
   * @return the mapped public API exception
   */
  private static WasmException mapByMessageContent(
      final Throwable exception, final String message) {
    if (message != null) {
      final String lowerMessage = message.toLowerCase(Locale.ROOT);

      // Compilation-related errors
      if (lowerMessage.contains("compilation") || lowerMessage.contains("compile")) {
        return new CompilationException("Compilation error: " + message, exception);
      }

      // Validation-related errors
      if (lowerMessage.contains("validation")
          || lowerMessage.contains("invalid")
          || lowerMessage.contains("parameter")) {
        return new ValidationException("Validation error: " + message, exception);
      }

      // Runtime execution errors
      if (lowerMessage.contains("runtime")
          || lowerMessage.contains("execution")
          || lowerMessage.contains("trap")
          || lowerMessage.contains("call")) {
        return new RuntimeException("Runtime error: " + message, exception);
      }

      // Resource-related errors
      if (lowerMessage.contains("memory")
          || lowerMessage.contains("resource")
          || lowerMessage.contains("allocation")
          || lowerMessage.contains("limit")) {
        return new ResourceException("Resource error: " + message, exception);
      }

      // Instantiation errors
      if (lowerMessage.contains("instantiat") || lowerMessage.contains("instance")) {
        return new InstantiationException("Instantiation error: " + message, exception);
      }

      // WASI-related errors
      if (lowerMessage.contains("wasi")) {
        if (lowerMessage.contains("component")) {
          return new WasiComponentException("WASI component error: " + message, exception);
        }
        if (lowerMessage.contains("config")) {
          return new WasiConfigurationException("WASI configuration error: " + message, exception);
        }
        if (lowerMessage.contains("resource")) {
          return new WasiResourceException("WASI resource error: " + message, exception);
        }
        return new WasiException("WASI error: " + message, exception);
      }

      // Security-related errors
      if (lowerMessage.contains("security")
          || lowerMessage.contains("permission")
          || lowerMessage.contains("access")) {
        return new SecurityException("Security error: " + message, exception);
      }
    }

    // Default mapping for unrecognized patterns
    return new WasmException("WebAssembly operation failed: " + message, exception);
  }

  /**
   * Determines if an exception represents a recoverable error condition.
   *
   * <p>Some errors may be recoverable or represent expected failure conditions. This method helps
   * identify such cases to support error recovery mechanisms.
   *
   * @param exception the exception to check
   * @return true if the error may be recoverable, false otherwise
   */
  public static boolean isRecoverableError(final WasmException exception) {
    if (exception == null) {
      return false;
    }

    // Validation and compilation errors are typically not recoverable
    if (exception instanceof ValidationException || exception instanceof CompilationException) {
      return false;
    }

    final String message = exception.getMessage();
    if (message != null) {
      final String lowerMessage = message.toLowerCase(Locale.ROOT);

      // These types of errors might be recoverable
      if (lowerMessage.contains("not found")
          || lowerMessage.contains("unavailable")
          || lowerMessage.contains("timeout")
          || lowerMessage.contains("temporary")) {
        return true;
      }

      // These are typically not recoverable
      if (lowerMessage.contains("out of memory")
          || lowerMessage.contains("corruption")
          || lowerMessage.contains("invalid")) {
        return false;
      }
    }

    // Runtime and resource exceptions might be recoverable depending on context
    return exception instanceof RuntimeException || exception instanceof ResourceException;
  }

  /**
   * Creates a standardized error message with context information.
   *
   * @param operation the operation that failed
   * @param context additional context information
   * @param originalMessage the original error message
   * @return a standardized error message
   */
  public static String createContextualErrorMessage(
      final String operation, final String context, final String originalMessage) {
    final StringBuilder message = new StringBuilder();

    if (operation != null && !operation.trim().isEmpty()) {
      message.append("Operation '").append(operation).append("' failed");
    } else {
      message.append("WebAssembly operation failed");
    }

    if (context != null && !context.trim().isEmpty()) {
      message.append(" (").append(context).append(")");
    }

    if (originalMessage != null && !originalMessage.trim().isEmpty()) {
      message.append(": ").append(originalMessage);
    }

    return message.toString();
  }

  /**
   * Wraps an exception with additional context while preserving the original exception chain.
   *
   * @param operation the operation that failed
   * @param context additional context information
   * @param originalException the original exception
   * @return a new exception with enhanced context
   */
  public static WasmException wrapWithContext(
      final String operation, final String context, final Throwable originalException) {
    final String contextualMessage =
        createContextualErrorMessage(
            operation, context, originalException != null ? originalException.getMessage() : null);

    final WasmException mappedException = mapToPublicException(originalException);

    // Create a new exception of the same type with enhanced context
    if (mappedException instanceof CompilationException) {
      return new CompilationException(contextualMessage, mappedException);
    } else if (mappedException instanceof ValidationException) {
      return new ValidationException(contextualMessage, mappedException);
    } else if (mappedException instanceof RuntimeException) {
      return new RuntimeException(contextualMessage, mappedException);
    } else if (mappedException instanceof ResourceException) {
      return new ResourceException(contextualMessage, mappedException);
    } else if (mappedException instanceof SecurityException) {
      return new SecurityException(contextualMessage, mappedException);
    } else if (mappedException instanceof InstantiationException) {
      return new InstantiationException(contextualMessage, mappedException);
    } else if (mappedException instanceof WasiComponentException) {
      return new WasiComponentException(contextualMessage, mappedException);
    } else if (mappedException instanceof WasiConfigurationException) {
      return new WasiConfigurationException(contextualMessage, mappedException);
    } else if (mappedException instanceof WasiResourceException) {
      return new WasiResourceException(contextualMessage, mappedException);
    } else if (mappedException instanceof WasiException) {
      return new WasiException(contextualMessage, mappedException);
    } else {
      return new WasmException(contextualMessage, mappedException);
    }
  }
}

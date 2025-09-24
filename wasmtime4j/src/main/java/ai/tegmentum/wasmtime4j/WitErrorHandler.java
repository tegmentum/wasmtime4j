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

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.ResourceException;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Comprehensive error handling system for WIT operations.
 *
 * <p>This class provides centralized error handling, recovery strategies, and diagnostic
 * capabilities for all WebAssembly Interface Type operations and component interactions.
 *
 * @since 1.0.0
 */
public final class WitErrorHandler {

  private static final Logger LOGGER = Logger.getLogger(WitErrorHandler.class.getName());

  private final Map<String, ErrorHandler> errorHandlers;
  private final Map<Class<? extends Exception>, ErrorRecoveryStrategy> recoveryStrategies;
  private final List<ErrorListener> errorListeners;
  private final ErrorMetrics metrics;

  /** Creates a new WIT error handler. */
  public WitErrorHandler() {
    this.errorHandlers = new ConcurrentHashMap<>();
    this.recoveryStrategies = new ConcurrentHashMap<>();
    this.errorListeners = new ArrayList<>();
    this.metrics = new ErrorMetrics();
    initializeDefaultHandlers();
  }

  /**
   * Handles a WIT operation error and attempts recovery.
   *
   * @param operation the operation that failed
   * @param error the error that occurred
   * @param context the operation context
   * @return error handling result
   */
  public ErrorHandlingResult handleError(
      final String operation, final Throwable error, final WitOperationContext context) {
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(error, "error");
    Objects.requireNonNull(context, "context");

    final long startTime = System.nanoTime();

    try {
      // Record error metrics
      metrics.recordError(operation, error.getClass());

      // Notify error listeners
      notifyErrorListeners(operation, error, context);

      // Get specific error handler
      final ErrorHandler handler = errorHandlers.getOrDefault(operation, getDefaultErrorHandler());

      // Handle the error
      final ErrorHandlingResult result = handler.handleError(error, context);

      // Attempt recovery if requested
      if (result.shouldAttemptRecovery()) {
        final Optional<Object> recoveryResult = attemptRecovery(error, context);
        if (recoveryResult.isPresent()) {
          return new ErrorHandlingResult(
              true, "Error handled with recovery", Optional.empty(), recoveryResult);
        }
      }

      // Log the error handling result
      final long duration = System.nanoTime() - startTime;
      LOGGER.log(
          result.isHandled() ? Level.FINE : Level.WARNING,
          String.format(
              "Error handling for operation '%s' completed in %.2f ms: %s",
              operation, duration / 1_000_000.0, result.getMessage()));

      return result;

    } catch (final Exception handlingError) {
      LOGGER.log(
          Level.SEVERE,
          "Error occurred while handling error for operation: " + operation,
          handlingError);
      return new ErrorHandlingResult(
          false,
          "Error handler failed: " + handlingError.getMessage(),
          Optional.of(new WasmException("Error handling failed", handlingError)),
          Optional.empty());
    }
  }

  /**
   * Registers a custom error handler for a specific operation.
   *
   * @param operation the operation name
   * @param handler the error handler
   */
  public void registerErrorHandler(final String operation, final ErrorHandler handler) {
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(handler, "handler");

    errorHandlers.put(operation, handler);
    LOGGER.fine("Registered error handler for operation: " + operation);
  }

  /**
   * Registers a recovery strategy for a specific exception type.
   *
   * @param exceptionType the exception type
   * @param strategy the recovery strategy
   */
  public void registerRecoveryStrategy(
      final Class<? extends Exception> exceptionType, final ErrorRecoveryStrategy strategy) {
    Objects.requireNonNull(exceptionType, "exceptionType");
    Objects.requireNonNull(strategy, "strategy");

    recoveryStrategies.put(exceptionType, strategy);
    LOGGER.fine("Registered recovery strategy for exception type: " + exceptionType.getName());
  }

  /**
   * Adds an error listener.
   *
   * @param listener the error listener
   */
  public void addErrorListener(final ErrorListener listener) {
    Objects.requireNonNull(listener, "listener");
    errorListeners.add(listener);
  }

  /**
   * Removes an error listener.
   *
   * @param listener the error listener
   */
  public void removeErrorListener(final ErrorListener listener) {
    errorListeners.remove(listener);
  }

  /**
   * Gets error metrics.
   *
   * @return error metrics
   */
  public ErrorMetrics getMetrics() {
    return metrics;
  }

  /**
   * Creates a WasmException with appropriate type based on the error context.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @param context the operation context
   * @return typed WasmException
   */
  public WasmException createTypedException(
      final String message, final Throwable cause, final WitOperationContext context) {
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(context, "context");

    // Determine appropriate exception type based on context
    switch (context.getOperationType()) {
      case COMPILATION:
        return new CompilationException(message, cause);
      case VALIDATION:
        return new ValidationException(message, cause);
      case RUNTIME:
        return new RuntimeException(message, cause);
      case RESOURCE_MANAGEMENT:
        return new ResourceException(message, cause);
      default:
        return new WasmException(message, cause);
    }
  }

  /**
   * Wraps an operation with error handling.
   *
   * @param operation the operation name
   * @param context the operation context
   * @param operationFunction the operation to execute
   * @param <T> the return type
   * @return the operation result
   * @throws WasmException if the operation fails and cannot be recovered
   */
  public <T> T executeWithErrorHandling(
      final String operation,
      final WitOperationContext context,
      final ThrowingSupplier<T> operationFunction)
      throws WasmException {

    try {
      return operationFunction.get();
    } catch (final Exception e) {
      final ErrorHandlingResult result = handleError(operation, e, context);

      if (result.getRecoveryResult().isPresent()) {
        @SuppressWarnings("unchecked")
        final T recoveredResult = (T) result.getRecoveryResult().get();
        return recoveredResult;
      }

      if (result.getTransformedException().isPresent()) {
        throw result.getTransformedException().get();
      }

      throw createTypedException("Operation failed: " + operation, e, context);
    }
  }

  /**
   * Attempts error recovery using registered strategies.
   *
   * @param error the error to recover from
   * @param context the operation context
   * @return recovery result if successful
   */
  private Optional<Object> attemptRecovery(
      final Throwable error, final WitOperationContext context) {
    // Try exact exception type first
    ErrorRecoveryStrategy strategy = recoveryStrategies.get(error.getClass());

    // Try superclass types if no exact match
    if (strategy == null) {
      for (final Map.Entry<Class<? extends Exception>, ErrorRecoveryStrategy> entry :
          recoveryStrategies.entrySet()) {
        if (entry.getKey().isAssignableFrom(error.getClass())) {
          strategy = entry.getValue();
          break;
        }
      }
    }

    if (strategy != null) {
      try {
        final Optional<Object> result = strategy.attemptRecovery(error, context);
        if (result.isPresent()) {
          LOGGER.fine("Successfully recovered from error: " + error.getClass().getSimpleName());
          metrics.recordRecovery(error.getClass());
        }
        return result;
      } catch (final Exception recoveryError) {
        LOGGER.warning("Recovery strategy failed: " + recoveryError.getMessage());
      }
    }

    return Optional.empty();
  }

  /**
   * Notifies all registered error listeners.
   *
   * @param operation the operation that failed
   * @param error the error that occurred
   * @param context the operation context
   */
  private void notifyErrorListeners(
      final String operation, final Throwable error, final WitOperationContext context) {
    for (final ErrorListener listener : errorListeners) {
      try {
        listener.onError(operation, error, context);
      } catch (final Exception e) {
        LOGGER.warning("Error listener failed: " + e.getMessage());
      }
    }
  }

  /**
   * Gets the default error handler.
   *
   * @return default error handler
   */
  private ErrorHandler getDefaultErrorHandler() {
    return (error, context) ->
        new ErrorHandlingResult(
            false,
            "No specific handler for error: " + error.getMessage(),
            Optional.of(createTypedException("Unhandled error", error, context)),
            Optional.empty());
  }

  /** Initializes default error handlers for common operations. */
  private void initializeDefaultHandlers() {
    // WIT parsing error handler
    registerErrorHandler(
        "wit-parsing",
        (error, context) -> {
          if (error instanceof IllegalArgumentException) {
            return new ErrorHandlingResult(
                true,
                "Invalid WIT syntax",
                Optional.of(
                    new ValidationException("WIT parsing failed: " + error.getMessage(), error)),
                Optional.empty());
          }
          return new ErrorHandlingResult(
              false, "Unhandled WIT parsing error", Optional.empty(), Optional.empty());
        });

    // Component linking error handler
    registerErrorHandler(
        "component-linking",
        (error, context) -> {
          if (error instanceof ValidationException) {
            return new ErrorHandlingResult(
                true, "Component linking validation failed", Optional.of(error), Optional.empty());
          }
          return new ErrorHandlingResult(
              false, "Unhandled linking error", Optional.empty(), Optional.empty());
        });

    // Resource management error handler
    registerErrorHandler(
        "resource-management",
        (error, context) -> {
          if (error instanceof IllegalStateException) {
            return new ErrorHandlingResult(
                true,
                "Resource state error",
                Optional.of(
                    new ResourceException(
                        "Resource management error: " + error.getMessage(), error)),
                Optional.empty());
          }
          return new ErrorHandlingResult(
              false, "Unhandled resource error", Optional.empty(), Optional.empty());
        });

    // Function binding error handler
    registerErrorHandler(
        "function-binding",
        (error, context) -> {
          if (error instanceof NoSuchMethodException) {
            return new ErrorHandlingResult(
                true,
                "Method not found for binding",
                Optional.of(
                    new ValidationException(
                        "Function binding failed: " + error.getMessage(), error)),
                Optional.empty());
          }
          return new ErrorHandlingResult(
              false, "Unhandled binding error", Optional.empty(), Optional.empty());
        });
  }

  // Supporting interfaces and classes

  /** Error handler interface. */
  @FunctionalInterface
  public interface ErrorHandler {
    /**
     * Handles an error for a specific operation.
     *
     * @param error the error that occurred
     * @param context the operation context
     * @return error handling result
     */
    ErrorHandlingResult handleError(Throwable error, WitOperationContext context);
  }

  /** Error recovery strategy interface. */
  @FunctionalInterface
  public interface ErrorRecoveryStrategy {
    /**
     * Attempts to recover from an error.
     *
     * @param error the error to recover from
     * @param context the operation context
     * @return recovery result if successful
     */
    Optional<Object> attemptRecovery(Throwable error, WitOperationContext context);
  }

  /** Error listener interface. */
  @FunctionalInterface
  public interface ErrorListener {
    /**
     * Called when an error occurs.
     *
     * @param operation the operation that failed
     * @param error the error that occurred
     * @param context the operation context
     */
    void onError(String operation, Throwable error, WitOperationContext context);
  }

  /** Throwing supplier interface. */
  @FunctionalInterface
  public interface ThrowingSupplier<T> {
    /**
     * Gets a result.
     *
     * @return the result
     * @throws Exception if the operation fails
     */
    T get() throws Exception;
  }

  /** Error handling result. */
  public static final class ErrorHandlingResult {
    private final boolean handled;
    private final String message;
    private final Optional<WasmException> transformedException;
    private final Optional<Object> recoveryResult;

    public ErrorHandlingResult(
        final boolean handled,
        final String message,
        final Optional<WasmException> transformedException,
        final Optional<Object> recoveryResult) {
      this.handled = handled;
      this.message = Objects.requireNonNull(message);
      this.transformedException = Objects.requireNonNull(transformedException);
      this.recoveryResult = Objects.requireNonNull(recoveryResult);
    }

    public boolean isHandled() {
      return handled;
    }

    public String getMessage() {
      return message;
    }

    public Optional<WasmException> getTransformedException() {
      return transformedException;
    }

    public Optional<Object> getRecoveryResult() {
      return recoveryResult;
    }

    public boolean shouldAttemptRecovery() {
      return handled && recoveryResult.isEmpty();
    }
  }

  /** WIT operation context. */
  public static final class WitOperationContext {
    private final WitOperationType operationType;
    private final String componentId;
    private final String interfaceName;
    private final Map<String, Object> metadata;

    public WitOperationContext(
        final WitOperationType operationType,
        final String componentId,
        final String interfaceName,
        final Map<String, Object> metadata) {
      this.operationType = Objects.requireNonNull(operationType);
      this.componentId = componentId;
      this.interfaceName = interfaceName;
      this.metadata = Map.copyOf(Objects.requireNonNull(metadata));
    }

    public static WitOperationContext compilation(final String componentId) {
      return new WitOperationContext(WitOperationType.COMPILATION, componentId, null, Map.of());
    }

    public static WitOperationContext validation(
        final String componentId, final String interfaceName) {
      return new WitOperationContext(
          WitOperationType.VALIDATION, componentId, interfaceName, Map.of());
    }

    public static WitOperationContext runtime(final String componentId) {
      return new WitOperationContext(WitOperationType.RUNTIME, componentId, null, Map.of());
    }

    public static WitOperationContext resourceManagement(final String componentId) {
      return new WitOperationContext(
          WitOperationType.RESOURCE_MANAGEMENT, componentId, null, Map.of());
    }

    public WitOperationType getOperationType() {
      return operationType;
    }

    public String getComponentId() {
      return componentId;
    }

    public String getInterfaceName() {
      return interfaceName;
    }

    public Map<String, Object> getMetadata() {
      return metadata;
    }
  }

  /** WIT operation type. */
  public enum WitOperationType {
    COMPILATION,
    VALIDATION,
    RUNTIME,
    RESOURCE_MANAGEMENT,
    INTERFACE_BINDING,
    TYPE_MARSHALING
  }

  /** Error metrics tracking. */
  public static final class ErrorMetrics {
    private final Map<String, Long> errorsByOperation;
    private final Map<Class<?>, Long> errorsByType;
    private final Map<Class<?>, Long> recoveriesByType;

    public ErrorMetrics() {
      this.errorsByOperation = new ConcurrentHashMap<>();
      this.errorsByType = new ConcurrentHashMap<>();
      this.recoveriesByType = new ConcurrentHashMap<>();
    }

    public void recordError(final String operation, final Class<?> errorType) {
      errorsByOperation.merge(operation, 1L, Long::sum);
      errorsByType.merge(errorType, 1L, Long::sum);
    }

    public void recordRecovery(final Class<?> errorType) {
      recoveriesByType.merge(errorType, 1L, Long::sum);
    }

    public Map<String, Long> getErrorsByOperation() {
      return Map.copyOf(errorsByOperation);
    }

    public Map<Class<?>, Long> getErrorsByType() {
      return Map.copyOf(errorsByType);
    }

    public Map<Class<?>, Long> getRecoveriesByType() {
      return Map.copyOf(recoveriesByType);
    }

    public long getTotalErrors() {
      return errorsByOperation.values().stream().mapToLong(Long::longValue).sum();
    }

    public long getTotalRecoveries() {
      return recoveriesByType.values().stream().mapToLong(Long::longValue).sum();
    }
  }
}

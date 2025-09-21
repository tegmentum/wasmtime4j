package ai.tegmentum.wasmtime4j.resilience;

import ai.tegmentum.wasmtime4j.diagnostics.WasmError;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Error recovery mechanisms for WebAssembly execution.
 *
 * <p>This interface provides strategies for recovering from errors during WebAssembly execution,
 * including retry policies, fallback mechanisms, and graceful degradation.
 *
 * @since 1.0.0
 */
public interface ErrorRecovery {

  /** Recovery strategy types. */
  enum Strategy {
    /** No recovery - fail immediately */
    NONE,
    /** Retry the operation */
    RETRY,
    /** Use a fallback implementation */
    FALLBACK,
    /** Graceful degradation */
    GRACEFUL_DEGRADATION,
    /** Circuit breaker pattern */
    CIRCUIT_BREAKER,
    /** Bulkhead isolation */
    BULKHEAD,
    /** Custom recovery logic */
    CUSTOM
  }

  /** Recovery attempt result. */
  enum RecoveryResult {
    /** Recovery was successful */
    SUCCESS,
    /** Recovery failed, should retry */
    RETRY,
    /** Recovery failed permanently */
    FAILED,
    /** Recovery not applicable */
    NOT_APPLICABLE
  }

  /** Recovery context containing error information and execution state. */
  interface RecoveryContext {
    /**
     * Gets the original error that triggered recovery.
     *
     * @return the original error
     */
    WasmError getOriginalError();

    /**
     * Gets the number of recovery attempts made so far.
     *
     * @return the attempt count
     */
    int getAttemptCount();

    /**
     * Gets the maximum number of recovery attempts allowed.
     *
     * @return the maximum attempts
     */
    int getMaxAttempts();

    /**
     * Gets the time elapsed since the first recovery attempt.
     *
     * @return the elapsed time in milliseconds
     */
    long getElapsedTimeMs();

    /**
     * Gets the recovery strategy being used.
     *
     * @return the recovery strategy
     */
    Strategy getStrategy();

    /**
     * Gets additional context properties.
     *
     * @return the context properties
     */
    java.util.Map<String, Object> getProperties();
  }

  /**
   * Attempts to recover from the given error.
   *
   * @param error the error to recover from
   * @param operation the operation to retry if applicable
   * @param <T> the operation result type
   * @return the recovery result
   */
  <T> CompletableFuture<RecoveryResult> recover(WasmError error, Supplier<T> operation);

  /**
   * Attempts to recover from the given error with context.
   *
   * @param context the recovery context
   * @param operation the operation to retry if applicable
   * @param <T> the operation result type
   * @return the recovery result
   */
  <T> CompletableFuture<RecoveryResult> recover(RecoveryContext context, Supplier<T> operation);

  /**
   * Checks if recovery is possible for the given error.
   *
   * @param error the error to check
   * @return true if recovery is possible
   */
  boolean canRecover(WasmError error);

  /**
   * Gets the recovery strategy for the given error.
   *
   * @param error the error to analyze
   * @return the recommended recovery strategy
   */
  Strategy getRecoveryStrategy(WasmError error);

  /**
   * Gets the maximum number of recovery attempts for the given error.
   *
   * @param error the error to analyze
   * @return the maximum attempts
   */
  int getMaxRecoveryAttempts(WasmError error);

  /**
   * Gets the delay between recovery attempts for the given error.
   *
   * @param error the error to analyze
   * @param attemptNumber the current attempt number
   * @return the delay in milliseconds
   */
  long getRecoveryDelay(WasmError error, int attemptNumber);

  /**
   * Gets the available fallback operations for the given error.
   *
   * @param error the error to analyze
   * @return list of fallback operations
   */
  List<FallbackOperation> getFallbackOperations(WasmError error);

  /**
   * Registers a custom recovery handler for specific error types.
   *
   * @param errorCategory the error category to handle
   * @param handler the recovery handler
   */
  void registerRecoveryHandler(WasmError.Category errorCategory, RecoveryHandler handler);

  /**
   * Removes a recovery handler for specific error types.
   *
   * @param errorCategory the error category
   */
  void unregisterRecoveryHandler(WasmError.Category errorCategory);

  /**
   * Gets the recovery statistics.
   *
   * @return the recovery statistics
   */
  RecoveryStatistics getStatistics();

  /** Resets the recovery statistics. */
  void resetStatistics();

  /**
   * Checks if the error recovery is currently enabled.
   *
   * @return true if error recovery is enabled
   */
  boolean isEnabled();

  /**
   * Enables or disables error recovery.
   *
   * @param enabled true to enable error recovery
   */
  void setEnabled(boolean enabled);

  /**
   * Creates a builder for constructing ErrorRecovery instances.
   *
   * @return a new error recovery builder
   */
  static ErrorRecoveryBuilder builder() {
    return new ErrorRecoveryBuilder();
  }

  /**
   * Creates a default ErrorRecovery with standard retry policies.
   *
   * @return the default error recovery
   */
  static ErrorRecovery defaultRecovery() {
    return builder()
        .withRetryPolicy(RetryPolicy.exponentialBackoff())
        .withCircuitBreaker(CircuitBreaker.defaultCircuitBreaker())
        .build();
  }

  /**
   * Creates an ErrorRecovery with no recovery mechanisms.
   *
   * @return the no-recovery instance
   */
  static ErrorRecovery noRecovery() {
    return builder().strategy(Strategy.NONE).build();
  }
}

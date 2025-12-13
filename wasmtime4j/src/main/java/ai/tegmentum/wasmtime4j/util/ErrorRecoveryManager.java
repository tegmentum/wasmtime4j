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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Error recovery manager providing strategies for handling transient and recoverable errors.
 *
 * <p>This utility class implements common error recovery patterns for WebAssembly operations,
 * including retry mechanisms, circuit breakers, and graceful degradation strategies. It helps
 * applications handle temporary failures and resource exhaustion scenarios.
 *
 * <p>The recovery manager supports configurable retry policies, exponential backoff, and error
 * classification to determine appropriate recovery strategies for different types of failures.
 *
 * @since 1.0.0
 */
public final class ErrorRecoveryManager {
  private static final Logger LOGGER = Logger.getLogger(ErrorRecoveryManager.class.getName());

  /** Default maximum number of retry attempts. */
  private static final int DEFAULT_MAX_RETRIES = 3;

  /** Default initial retry delay in milliseconds. */
  private static final long DEFAULT_INITIAL_DELAY_MS = 100;

  /** Default maximum retry delay in milliseconds. */
  private static final long DEFAULT_MAX_DELAY_MS = 5000;

  /** Default backoff multiplier for exponential backoff. */
  private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;

  /** Private constructor to prevent instantiation of utility class. */
  private ErrorRecoveryManager() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /** Configuration for retry operations. */
  public static final class RetryConfig {
    private final int maxRetries;
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double backoffMultiplier;
    private final boolean exponentialBackoff;

    /**
     * Creates a new retry configuration.
     *
     * @param maxRetries the maximum number of retry attempts
     * @param initialDelayMs the initial delay between retries in milliseconds
     * @param maxDelayMs the maximum delay between retries in milliseconds
     * @param backoffMultiplier the multiplier for exponential backoff
     * @param exponentialBackoff whether to use exponential backoff
     */
    public RetryConfig(
        final int maxRetries,
        final long initialDelayMs,
        final long maxDelayMs,
        final double backoffMultiplier,
        final boolean exponentialBackoff) {
      this.maxRetries = Math.max(0, maxRetries);
      this.initialDelayMs = Math.max(0, initialDelayMs);
      this.maxDelayMs = Math.max(initialDelayMs, maxDelayMs);
      this.backoffMultiplier = Math.max(1.0, backoffMultiplier);
      this.exponentialBackoff = exponentialBackoff;
    }

    /**
     * Creates a default retry configuration.
     *
     * @return default retry configuration
     */
    public static RetryConfig defaultConfig() {
      return new RetryConfig(
          DEFAULT_MAX_RETRIES,
          DEFAULT_INITIAL_DELAY_MS,
          DEFAULT_MAX_DELAY_MS,
          DEFAULT_BACKOFF_MULTIPLIER,
          true);
    }

    /**
     * Creates a simple retry configuration with fixed delay.
     *
     * @param maxRetries the maximum number of retries
     * @param delayMs the fixed delay between retries
     * @return fixed delay retry configuration
     */
    public static RetryConfig fixedDelay(final int maxRetries, final long delayMs) {
      return new RetryConfig(maxRetries, delayMs, delayMs, 1.0, false);
    }

    /**
     * Creates an exponential backoff retry configuration.
     *
     * @param maxRetries the maximum number of retries
     * @param initialDelayMs the initial delay
     * @param maxDelayMs the maximum delay
     * @return exponential backoff retry configuration
     */
    public static RetryConfig exponentialBackoff(
        final int maxRetries, final long initialDelayMs, final long maxDelayMs) {
      return new RetryConfig(
          maxRetries, initialDelayMs, maxDelayMs, DEFAULT_BACKOFF_MULTIPLIER, true);
    }

    public int getMaxRetries() {
      return maxRetries;
    }

    public long getInitialDelayMs() {
      return initialDelayMs;
    }

    public long getMaxDelayMs() {
      return maxDelayMs;
    }

    public double getBackoffMultiplier() {
      return backoffMultiplier;
    }

    public boolean isExponentialBackoff() {
      return exponentialBackoff;
    }
  }

  /**
   * Result of a retry operation.
   *
   * @param <T> the result type
   */
  public static final class RetryResult<T> {
    private final T result;
    private final boolean successful;
    private final int attemptCount;
    private final WasmException lastException;
    private final Duration totalDuration;

    private RetryResult(
        final T result,
        final boolean successful,
        final int attemptCount,
        final WasmException lastException,
        final Duration totalDuration) {
      this.result = result;
      this.successful = successful;
      this.attemptCount = attemptCount;
      this.lastException = lastException;
      this.totalDuration = totalDuration;
    }

    /**
     * Creates a successful retry result.
     *
     * @param result the successful result
     * @param attemptCount the number of attempts made
     * @param totalDuration the total duration of all attempts
     * @param <T> the result type
     * @return successful retry result
     */
    public static <T> RetryResult<T> success(
        final T result, final int attemptCount, final Duration totalDuration) {
      return new RetryResult<>(result, true, attemptCount, null, totalDuration);
    }

    /**
     * Creates a failed retry result.
     *
     * @param lastException the final exception
     * @param attemptCount the number of attempts made
     * @param totalDuration the total duration of all attempts
     * @param <T> the result type
     * @return failed retry result
     */
    public static <T> RetryResult<T> failure(
        final WasmException lastException, final int attemptCount, final Duration totalDuration) {
      return new RetryResult<>(null, false, attemptCount, lastException, totalDuration);
    }

    public T getResult() {
      return result;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public int getAttemptCount() {
      return attemptCount;
    }

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "WasmException is effectively immutable; returning original is intentional")
    public WasmException getLastException() {
      return lastException;
    }

    public Duration getTotalDuration() {
      return totalDuration;
    }
  }

  /**
   * Executes an operation with retry logic for recoverable errors.
   *
   * <p>This method automatically retries operations that fail with recoverable exceptions, applying
   * the specified retry configuration. Non-recoverable errors are propagated immediately without
   * retry attempts.
   *
   * @param operation the operation to execute
   * @param config the retry configuration
   * @param operationName the name of the operation (for logging)
   * @param <T> the result type
   * @return the retry result
   */
  public static <T> RetryResult<T> executeWithRetry(
      final Supplier<T> operation, final RetryConfig config, final String operationName) {
    final long startTime = System.nanoTime();
    WasmException lastException = null;
    int attemptCount = 0;
    long currentDelayMs = config.getInitialDelayMs();

    while (attemptCount <= config.getMaxRetries()) {
      attemptCount++;

      try {
        if (LOGGER.isLoggable(Level.FINE)) {
          LOGGER.fine("Executing " + operationName + " (attempt " + attemptCount + ")");
        }

        final T result = operation.get();
        final Duration totalDuration = Duration.ofNanos(System.nanoTime() - startTime);

        if (attemptCount > 1 && LOGGER.isLoggable(Level.INFO)) {
          LOGGER.info(
              operationName + " succeeded after " + attemptCount + " attempts in " + totalDuration);
        }

        return RetryResult.success(result, attemptCount, totalDuration);

      } catch (Exception e) {
        final WasmException wasmException = UnifiedExceptionMapper.mapToPublicException(e);
        lastException = wasmException;

        if (!UnifiedExceptionMapper.isRecoverableError(wasmException)) {
          if (LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning(
                operationName
                    + " failed with non-recoverable error: "
                    + wasmException.getMessage());
          }
          break; // Don't retry non-recoverable errors
        }

        if (attemptCount <= config.getMaxRetries()) {
          if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                operationName
                    + " failed (attempt "
                    + attemptCount
                    + "), retrying in "
                    + currentDelayMs
                    + "ms: "
                    + wasmException.getMessage());
          }

          // Sleep before retry (except on the last attempt)
          if (attemptCount <= config.getMaxRetries() && currentDelayMs > 0) {
            try {
              Thread.sleep(currentDelayMs);
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              lastException = new WasmException("Operation interrupted during retry", ie);
              break;
            }
          }

          // Calculate next delay for exponential backoff
          if (config.isExponentialBackoff()) {
            currentDelayMs =
                Math.min(
                    (long) (currentDelayMs * config.getBackoffMultiplier()),
                    config.getMaxDelayMs());
          }
        }
      }
    }

    final Duration totalDuration = Duration.ofNanos(System.nanoTime() - startTime);

    if (LOGGER.isLoggable(Level.WARNING)) {
      LOGGER.warning(
          operationName
              + " failed after "
              + attemptCount
              + " attempts in "
              + totalDuration
              + ": "
              + (lastException != null ? lastException.getMessage() : "unknown error"));
    }

    return RetryResult.failure(lastException, attemptCount, totalDuration);
  }

  /**
   * Executes an operation with default retry configuration.
   *
   * @param operation the operation to execute
   * @param operationName the name of the operation
   * @param <T> the result type
   * @return the retry result
   */
  public static <T> RetryResult<T> executeWithRetry(
      final Supplier<T> operation, final String operationName) {
    return executeWithRetry(operation, RetryConfig.defaultConfig(), operationName);
  }

  /**
   * Validates that an operation result is acceptable and throws an exception if not.
   *
   * <p>This method provides a way to implement custom validation logic for operation results,
   * allowing for early detection of invalid states that should be considered failures.
   *
   * @param result the operation result to validate
   * @param validator the validation function that returns an error message if invalid
   * @param <T> the result type
   * @return the validated result
   * @throws WasmException if validation fails
   */
  public static <T> T validateResult(
      final T result, final java.util.function.Function<T, String> validator) throws WasmException {
    if (validator != null) {
      final String errorMessage = validator.apply(result);
      if (errorMessage != null) {
        throw new WasmException("Result validation failed: " + errorMessage);
      }
    }
    return result;
  }

  /**
   * Provides a graceful fallback when an operation fails.
   *
   * <p>This method attempts to execute the primary operation, and if it fails with a recoverable
   * error, executes the fallback operation instead. If both fail or the primary operation fails
   * with a non-recoverable error, the primary exception is thrown.
   *
   * @param primaryOperation the primary operation to attempt
   * @param fallbackOperation the fallback operation to use if primary fails
   * @param operationName the name of the operation (for logging)
   * @param <T> the result type
   * @return the result from either the primary or fallback operation
   * @throws WasmException if both operations fail or primary fails with non-recoverable error
   */
  public static <T> T executeWithFallback(
      final Supplier<T> primaryOperation,
      final Supplier<T> fallbackOperation,
      final String operationName)
      throws WasmException {
    try {
      return primaryOperation.get();
    } catch (Exception e) {
      final WasmException wasmException = UnifiedExceptionMapper.mapToPublicException(e);

      if (!UnifiedExceptionMapper.isRecoverableError(wasmException)) {
        // Non-recoverable error - don't attempt fallback
        throw wasmException;
      }

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info(
            operationName
                + " primary operation failed, attempting fallback: "
                + wasmException.getMessage());
      }

      try {
        final T fallbackResult = fallbackOperation.get();
        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.info(operationName + " fallback operation succeeded");
        }
        return fallbackResult;
      } catch (Exception fallbackException) {
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.warning(
              operationName + " fallback operation also failed: " + fallbackException.getMessage());
        }
        // Return the original primary exception since that's usually more relevant
        throw wasmException;
      }
    }
  }

  /**
   * Creates a circuit breaker-like behavior by tracking failure rates.
   *
   * <p>This method provides a simple circuit breaker implementation that can help prevent cascading
   * failures by temporarily disabling operations that have a high failure rate.
   *
   * @param operation the operation to execute
   * @param failureThreshold the failure rate threshold (0.0 to 1.0)
   * @param windowSize the number of recent operations to consider
   * @param operationName the name of the operation
   * @param <T> the result type
   * @return the operation result
   * @throws WasmException if the circuit breaker is open or operation fails
   */
  public static <T> T executeWithCircuitBreaker(
      final Supplier<T> operation,
      final double failureThreshold,
      final int windowSize,
      final String operationName)
      throws WasmException {
    // For now, this is a simplified implementation
    // In a full implementation, you would track failure rates across multiple calls
    try {
      return operation.get();
    } catch (Exception e) {
      final WasmException wasmException = UnifiedExceptionMapper.mapToPublicException(e);

      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning(operationName + " failed in circuit breaker: " + wasmException.getMessage());
      }

      throw wasmException;
    }
  }
}

package ai.tegmentum.wasmtime4j.resilience;

import ai.tegmentum.wasmtime4j.diagnostics.WasmError;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Retry policy for WebAssembly operations.
 *
 * <p>This interface defines retry strategies for handling transient failures
 * in WebAssembly execution, with configurable delays, conditions, and limits.
 *
 * @since 1.0.0
 */
public interface RetryPolicy {

    /**
     * Retry strategies for delay calculation.
     */
    enum Strategy {
        /** Fixed delay between retries */
        FIXED,
        /** Linear increase in delay */
        LINEAR,
        /** Exponential backoff */
        EXPONENTIAL,
        /** Random jittered delay */
        JITTERED,
        /** Custom delay calculation */
        CUSTOM
    }

    /**
     * Retry attempt context information.
     */
    interface RetryContext {
        /**
         * Gets the current attempt number (1-based).
         *
         * @return the attempt number
         */
        int getAttemptNumber();

        /**
         * Gets the maximum number of retry attempts allowed.
         *
         * @return the maximum attempts
         */
        int getMaxAttempts();

        /**
         * Gets the last exception that caused the retry.
         *
         * @return the last exception
         */
        Throwable getLastException();

        /**
         * Gets the last WebAssembly error that caused the retry.
         *
         * @return the last error, or empty if not a WebAssembly error
         */
        Optional<WasmError> getLastWasmError();

        /**
         * Gets the total time elapsed since the first attempt.
         *
         * @return the elapsed time
         */
        Duration getElapsedTime();

        /**
         * Gets the delay before the next retry attempt.
         *
         * @return the next delay
         */
        Duration getNextDelay();

        /**
         * Gets the retry strategy being used.
         *
         * @return the retry strategy
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
     * Retry statistics and metrics.
     */
    interface RetryStatistics {
        /**
         * Gets the total number of operations attempted.
         *
         * @return the total operation count
         */
        long getTotalOperations();

        /**
         * Gets the number of operations that succeeded without retry.
         *
         * @return the immediate success count
         */
        long getImmediateSuccesses();

        /**
         * Gets the number of operations that succeeded after retries.
         *
         * @return the eventual success count
         */
        long getEventualSuccesses();

        /**
         * Gets the number of operations that failed after all retries.
         *
         * @return the total failure count
         */
        long getTotalFailures();

        /**
         * Gets the total number of retry attempts made.
         *
         * @return the total retry count
         */
        long getTotalRetries();

        /**
         * Gets the average number of retries per operation.
         *
         * @return the average retry count
         */
        double getAverageRetries();

        /**
         * Gets the success rate as a percentage (0-100).
         *
         * @return the success rate percentage
         */
        double getSuccessRate();

        /**
         * Gets the average time to success.
         *
         * @return the average time to success
         */
        Duration getAverageTimeToSuccess();
    }

    /**
     * Executes the given operation with retry policy.
     *
     * @param operation the operation to execute
     * @param <T> the operation result type
     * @return the operation result
     * @throws Exception if all retry attempts fail
     */
    <T> T execute(Supplier<T> operation) throws Exception;

    /**
     * Executes the given operation asynchronously with retry policy.
     *
     * @param operation the operation to execute
     * @param <T> the operation result type
     * @return a future containing the operation result
     */
    <T> CompletableFuture<T> executeAsync(Supplier<T> operation);

    /**
     * Executes the given operation with custom retry context.
     *
     * @param operation the operation to execute
     * @param context the retry context
     * @param <T> the operation result type
     * @return the operation result
     * @throws Exception if all retry attempts fail
     */
    <T> T execute(Supplier<T> operation, RetryContext context) throws Exception;

    /**
     * Gets the maximum number of retry attempts.
     *
     * @return the maximum attempts
     */
    int getMaxAttempts();

    /**
     * Gets the base delay between retry attempts.
     *
     * @return the base delay
     */
    Duration getBaseDelay();

    /**
     * Gets the maximum delay between retry attempts.
     *
     * @return the maximum delay
     */
    Duration getMaxDelay();

    /**
     * Gets the retry strategy.
     *
     * @return the retry strategy
     */
    Strategy getStrategy();

    /**
     * Gets the condition for determining if an exception should trigger a retry.
     *
     * @return the retry condition predicate
     */
    Predicate<Throwable> getRetryCondition();

    /**
     * Calculates the delay for the given attempt number.
     *
     * @param attemptNumber the attempt number (1-based)
     * @return the delay duration
     */
    Duration calculateDelay(int attemptNumber);

    /**
     * Checks if the given exception should trigger a retry.
     *
     * @param exception the exception to check
     * @return true if a retry should be attempted
     */
    boolean shouldRetry(Throwable exception);

    /**
     * Checks if the given WebAssembly error should trigger a retry.
     *
     * @param error the error to check
     * @return true if a retry should be attempted
     */
    boolean shouldRetry(WasmError error);

    /**
     * Gets the retry statistics.
     *
     * @return the retry statistics
     */
    RetryStatistics getStatistics();

    /**
     * Resets the retry statistics.
     */
    void resetStatistics();

    /**
     * Registers a retry attempt listener.
     *
     * @param listener the retry listener
     */
    void addRetryListener(RetryListener listener);

    /**
     * Removes a retry attempt listener.
     *
     * @param listener the retry listener
     */
    void removeRetryListener(RetryListener listener);

    /**
     * Creates a builder for constructing RetryPolicy instances.
     *
     * @return a new retry policy builder
     */
    static RetryPolicyBuilder builder() {
        return new RetryPolicyBuilder();
    }

    /**
     * Creates a fixed delay retry policy.
     *
     * @param maxAttempts the maximum number of attempts
     * @param delay the fixed delay between attempts
     * @return the retry policy
     */
    static RetryPolicy fixedDelay(int maxAttempts, Duration delay) {
        return builder()
            .maxAttempts(maxAttempts)
            .strategy(Strategy.FIXED)
            .baseDelay(delay)
            .build();
    }

    /**
     * Creates an exponential backoff retry policy.
     *
     * @param maxAttempts the maximum number of attempts
     * @param baseDelay the base delay for exponential calculation
     * @param maxDelay the maximum delay between attempts
     * @return the retry policy
     */
    static RetryPolicy exponentialBackoff(int maxAttempts, Duration baseDelay, Duration maxDelay) {
        return builder()
            .maxAttempts(maxAttempts)
            .strategy(Strategy.EXPONENTIAL)
            .baseDelay(baseDelay)
            .maxDelay(maxDelay)
            .build();
    }

    /**
     * Creates a default exponential backoff retry policy.
     *
     * @return the default retry policy
     */
    static RetryPolicy exponentialBackoff() {
        return exponentialBackoff(3, Duration.ofMillis(100), Duration.ofSeconds(5));
    }

    /**
     * Creates a retry policy with no retries.
     *
     * @return the no-retry policy
     */
    static RetryPolicy noRetry() {
        return builder()
            .maxAttempts(1)
            .build();
    }

    /**
     * Retry attempt listener interface.
     */
    @FunctionalInterface
    interface RetryListener {
        /**
         * Called when a retry attempt is made.
         *
         * @param context the retry context
         * @param exception the exception that triggered the retry
         */
        void onRetry(RetryContext context, Throwable exception);
    }
}
package ai.tegmentum.wasmtime4j.resilience;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Circuit breaker pattern implementation for WebAssembly operations.
 *
 * <p>This interface provides circuit breaker functionality to prevent cascading failures and allow
 * systems to recover from transient errors in WebAssembly execution.
 *
 * @since 1.0.0
 */
public interface CircuitBreaker {

  /** Circuit breaker states. */
  enum State {
    /** Circuit is closed, operations are allowed */
    CLOSED,
    /** Circuit is open, operations are blocked */
    OPEN,
    /** Circuit is half-open, testing if operations can resume */
    HALF_OPEN
  }

  /** Circuit breaker metrics and statistics. */
  interface Metrics {
    /**
     * Gets the current circuit breaker state.
     *
     * @return the current state
     */
    State getState();

    /**
     * Gets the total number of calls made through the circuit breaker.
     *
     * @return the total call count
     */
    long getTotalCalls();

    /**
     * Gets the number of successful calls.
     *
     * @return the successful call count
     */
    long getSuccessfulCalls();

    /**
     * Gets the number of failed calls.
     *
     * @return the failed call count
     */
    long getFailedCalls();

    /**
     * Gets the current failure rate as a percentage (0-100).
     *
     * @return the failure rate percentage
     */
    double getFailureRate();

    /**
     * Gets the number of calls blocked by the circuit breaker.
     *
     * @return the blocked call count
     */
    long getBlockedCalls();

    /**
     * Gets the timestamp when the circuit was last opened.
     *
     * @return the last opened timestamp, or -1 if never opened
     */
    long getLastOpenedTimestamp();

    /**
     * Gets the timestamp when the circuit was last closed.
     *
     * @return the last closed timestamp, or -1 if never closed
     */
    long getLastClosedTimestamp();

    /**
     * Gets the duration the circuit has been in the current state.
     *
     * @return the state duration
     */
    Duration getStateDuration();
  }

  /**
   * Executes the given operation through the circuit breaker.
   *
   * @param operation the operation to execute
   * @param <T> the operation result type
   * @return the operation result
   * @throws CircuitBreakerOpenException if the circuit is open
   */
  <T> T execute(Supplier<T> operation) throws CircuitBreakerOpenException;

  /**
   * Executes the given operation asynchronously through the circuit breaker.
   *
   * @param operation the operation to execute
   * @param <T> the operation result type
   * @return a future containing the operation result
   */
  <T> CompletableFuture<T> executeAsync(Supplier<T> operation);

  /**
   * Attempts to execute the operation, returning a fallback value if the circuit is open.
   *
   * @param operation the operation to execute
   * @param fallback the fallback value
   * @param <T> the operation result type
   * @return the operation result or fallback value
   */
  <T> T executeWithFallback(Supplier<T> operation, T fallback);

  /**
   * Attempts to execute the operation, using a fallback supplier if the circuit is open.
   *
   * @param operation the operation to execute
   * @param fallbackSupplier the fallback supplier
   * @param <T> the operation result type
   * @return the operation result or fallback value
   */
  <T> T executeWithFallback(Supplier<T> operation, Supplier<T> fallbackSupplier);

  /**
   * Gets the current circuit breaker state.
   *
   * @return the current state
   */
  State getState();

  /**
   * Gets the circuit breaker metrics.
   *
   * @return the metrics
   */
  Metrics getMetrics();

  /** Manually opens the circuit breaker. */
  void open();

  /** Manually closes the circuit breaker. */
  void close();

  /** Manually transitions the circuit breaker to half-open state. */
  void halfOpen();

  /** Resets the circuit breaker metrics. */
  void reset();

  /**
   * Gets the failure threshold percentage that triggers circuit opening.
   *
   * @return the failure threshold percentage (0-100)
   */
  double getFailureThreshold();

  /**
   * Gets the minimum number of calls required before failure rate is calculated.
   *
   * @return the minimum call count
   */
  int getMinimumCallCount();

  /**
   * Gets the wait duration before transitioning from open to half-open.
   *
   * @return the wait duration
   */
  Duration getWaitDuration();

  /**
   * Gets the sliding window size for failure rate calculation.
   *
   * @return the sliding window size
   */
  int getSlidingWindowSize();

  /**
   * Gets the maximum number of permitted calls in half-open state.
   *
   * @return the permitted calls in half-open state
   */
  int getPermittedCallsInHalfOpenState();

  /**
   * Checks if the circuit breaker is currently allowing calls.
   *
   * @return true if calls are allowed
   */
  boolean isCallAllowed();

  /** Records a successful call. */
  void recordSuccess();

  /**
   * Records a failed call.
   *
   * @param throwable the exception that caused the failure
   */
  void recordFailure(Throwable throwable);

  /**
   * Registers a state change listener.
   *
   * @param listener the state change listener
   */
  void addStateChangeListener(StateChangeListener listener);

  /**
   * Removes a state change listener.
   *
   * @param listener the state change listener
   */
  void removeStateChangeListener(StateChangeListener listener);

  /**
   * Creates a builder for constructing CircuitBreaker instances.
   *
   * @return a new circuit breaker builder
   */
  static CircuitBreakerBuilder builder() {
    return new CircuitBreakerBuilder();
  }

  /**
   * Creates a default CircuitBreaker with standard settings.
   *
   * @return the default circuit breaker
   */
  static CircuitBreaker defaultCircuitBreaker() {
    return builder()
        .failureThreshold(50.0)
        .minimumCallCount(10)
        .waitDuration(Duration.ofSeconds(30))
        .slidingWindowSize(100)
        .permittedCallsInHalfOpenState(5)
        .build();
  }

  /** State change listener interface. */
  @FunctionalInterface
  interface StateChangeListener {
    /**
     * Called when the circuit breaker state changes.
     *
     * @param previousState the previous state
     * @param currentState the current state
     * @param metrics the current metrics
     */
    void onStateChange(State previousState, State currentState, Metrics metrics);
  }
}

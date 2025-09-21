package ai.tegmentum.wasmtime4j.resilience;

/**
 * Exception thrown when a circuit breaker is open and blocks execution.
 *
 * <p>This exception is thrown when an operation is attempted through a circuit breaker that is
 * currently in the open state, preventing execution to avoid cascading failures.
 *
 * @since 1.0.0
 */
public class CircuitBreakerOpenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String circuitBreakerName;
  private final CircuitBreaker.State state;
  private final long blockedCallCount;

  /**
   * Creates a new circuit breaker open exception.
   *
   * @param circuitBreakerName the name of the circuit breaker
   * @param state the current state of the circuit breaker
   * @param blockedCallCount the number of calls blocked so far
   */
  public CircuitBreakerOpenException(
      final String circuitBreakerName,
      final CircuitBreaker.State state,
      final long blockedCallCount) {
    super(
        String.format(
            "Circuit breaker '%s' is %s and has blocked %d calls",
            circuitBreakerName, state, blockedCallCount));
    this.circuitBreakerName = circuitBreakerName;
    this.state = state;
    this.blockedCallCount = blockedCallCount;
  }

  /**
   * Creates a new circuit breaker open exception with a custom message.
   *
   * @param message the exception message
   * @param circuitBreakerName the name of the circuit breaker
   * @param state the current state of the circuit breaker
   * @param blockedCallCount the number of calls blocked so far
   */
  public CircuitBreakerOpenException(
      final String message,
      final String circuitBreakerName,
      final CircuitBreaker.State state,
      final long blockedCallCount) {
    super(message);
    this.circuitBreakerName = circuitBreakerName;
    this.state = state;
    this.blockedCallCount = blockedCallCount;
  }

  /**
   * Gets the name of the circuit breaker that is open.
   *
   * @return the circuit breaker name
   */
  public String getCircuitBreakerName() {
    return circuitBreakerName;
  }

  /**
   * Gets the current state of the circuit breaker.
   *
   * @return the circuit breaker state
   */
  public CircuitBreaker.State getState() {
    return state;
  }

  /**
   * Gets the number of calls that have been blocked by the circuit breaker.
   *
   * @return the blocked call count
   */
  public long getBlockedCallCount() {
    return blockedCallCount;
  }
}

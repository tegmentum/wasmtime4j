package ai.tegmentum.wasmtime4j.debug;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Event representing a WebAssembly function call for debugging and tracing.
 *
 * <p>Contains information about function invocation including parameters,
 * execution context, and timing information.
 *
 * @since 1.0.0
 */
public final class FunctionCallEvent {

  private final String functionName;
  private final List<Object> parameters;
  private final Instant timestamp;
  private final long executionTimeNanos;
  private final String stackTrace;
  private final boolean isHostFunction;

  private FunctionCallEvent(final String functionName,
                            final List<Object> parameters,
                            final Instant timestamp,
                            final long executionTimeNanos,
                            final String stackTrace,
                            final boolean isHostFunction) {
    this.functionName = Objects.requireNonNull(functionName);
    this.parameters = List.copyOf(parameters);
    this.timestamp = Objects.requireNonNull(timestamp);
    this.executionTimeNanos = executionTimeNanos;
    this.stackTrace = stackTrace;
    this.isHostFunction = isHostFunction;
  }

  /**
   * Creates a new function call event.
   *
   * @param functionName the name of the called function
   * @param parameters the function parameters
   * @param timestamp when the call occurred
   * @param executionTimeNanos execution time in nanoseconds
   * @param stackTrace the call stack trace
   * @param isHostFunction true if this is a host function call
   * @return new function call event
   */
  public static FunctionCallEvent create(final String functionName,
                                         final List<Object> parameters,
                                         final Instant timestamp,
                                         final long executionTimeNanos,
                                         final String stackTrace,
                                         final boolean isHostFunction) {
    return new FunctionCallEvent(functionName, parameters, timestamp,
                                 executionTimeNanos, stackTrace, isHostFunction);
  }

  /**
   * Gets the function name.
   *
   * @return function name
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the function parameters.
   *
   * @return list of parameters
   */
  public List<Object> getParameters() {
    return parameters;
  }

  /**
   * Gets the timestamp when the call occurred.
   *
   * @return call timestamp
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the execution time in nanoseconds.
   *
   * @return execution time in nanoseconds
   */
  public long getExecutionTimeNanos() {
    return executionTimeNanos;
  }

  /**
   * Gets the call stack trace.
   *
   * @return stack trace string
   */
  public String getStackTrace() {
    return stackTrace;
  }

  /**
   * Checks if this is a host function call.
   *
   * @return true if host function call
   */
  public boolean isHostFunction() {
    return isHostFunction;
  }

  @Override
  public String toString() {
    return String.format("FunctionCallEvent{name='%s', params=%d, time=%dns, host=%s}",
        functionName, parameters.size(), executionTimeNanos, isHostFunction);
  }
}
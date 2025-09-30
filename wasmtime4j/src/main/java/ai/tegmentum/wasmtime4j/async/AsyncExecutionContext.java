package ai.tegmentum.wasmtime4j.async;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Execution context for asynchronous WebAssembly function calls.
 *
 * <p>Provides contextual information and control mechanisms for async function execution, including
 * timeout handling, execution metadata, and resource management.
 *
 * @since 1.0.0
 */
public final class AsyncExecutionContext {

  private final String functionName;
  private final Instant startTime;
  private final Optional<Duration> timeout;
  private final Executor executor;
  private final Map<String, Object> metadata;
  private final String executionId;

  private volatile boolean cancelled;
  private volatile boolean completed;
  private volatile Throwable error;

  /**
   * Creates a new execution context.
   *
   * @param functionName the name of the function being executed
   * @param timeout optional timeout for the execution
   * @param executor the executor to use for async operations
   * @param executionId unique identifier for this execution
   */
  public AsyncExecutionContext(
      final String functionName,
      final Optional<Duration> timeout,
      final Executor executor,
      final String executionId) {
    this.functionName = Objects.requireNonNull(functionName);
    this.startTime = Instant.now();
    this.timeout = Objects.requireNonNull(timeout);
    this.executor = Objects.requireNonNull(executor);
    this.executionId = Objects.requireNonNull(executionId);
    this.metadata = new ConcurrentHashMap<>();
    this.cancelled = false;
    this.completed = false;
  }

  /**
   * Gets the function name.
   *
   * @return the function name
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the execution start time.
   *
   * @return the start time
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Gets the timeout for this execution.
   *
   * @return optional timeout duration
   */
  public Optional<Duration> getTimeout() {
    return timeout;
  }

  /**
   * Gets the executor for async operations.
   *
   * @return the executor
   */
  public Executor getExecutor() {
    return executor;
  }

  /**
   * Gets the unique execution identifier.
   *
   * @return the execution ID
   */
  public String getExecutionId() {
    return executionId;
  }

  /**
   * Gets the elapsed execution time.
   *
   * @return elapsed time since start
   */
  public Duration getElapsedTime() {
    return Duration.between(startTime, Instant.now());
  }

  /**
   * Checks if the execution has timed out.
   *
   * @return true if timed out, false otherwise
   */
  public boolean isTimedOut() {
    return timeout.map(t -> getElapsedTime().compareTo(t) > 0).orElse(false);
  }

  /**
   * Checks if the execution has been cancelled.
   *
   * @return true if cancelled, false otherwise
   */
  public boolean isCancelled() {
    return cancelled;
  }

  /**
   * Checks if the execution has completed.
   *
   * @return true if completed, false otherwise
   */
  public boolean isCompleted() {
    return completed;
  }

  /**
   * Gets the error that occurred during execution, if any.
   *
   * @return optional error
   */
  public Optional<Throwable> getError() {
    return Optional.ofNullable(error);
  }

  /** Cancels the execution. */
  public void cancel() {
    this.cancelled = true;
  }

  /** Marks the execution as completed. */
  public void markCompleted() {
    this.completed = true;
  }

  /**
   * Sets an error for this execution.
   *
   * @param error the error that occurred
   */
  public void setError(final Throwable error) {
    this.error = error;
    this.completed = true;
  }

  /**
   * Sets metadata for this execution.
   *
   * @param key the metadata key
   * @param value the metadata value
   */
  public void setMetadata(final String key, final Object value) {
    metadata.put(Objects.requireNonNull(key), value);
  }

  /**
   * Gets metadata for this execution.
   *
   * @param key the metadata key
   * @return optional metadata value
   */
  public Optional<Object> getMetadata(final String key) {
    return Optional.ofNullable(metadata.get(key));
  }

  /**
   * Gets metadata for this execution with a specific type.
   *
   * @param key the metadata key
   * @param type the expected type
   * @param <T> the type parameter
   * @return optional typed metadata value
   */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getMetadata(final String key, final Class<T> type) {
    final Object value = metadata.get(key);
    if (value != null && type.isInstance(value)) {
      return Optional.of((T) value);
    }
    return Optional.empty();
  }

  /**
   * Gets all metadata.
   *
   * @return copy of all metadata
   */
  public Map<String, Object> getAllMetadata() {
    return Map.copyOf(metadata);
  }

  /**
   * Checks if the execution should be terminated.
   *
   * @return true if should terminate, false otherwise
   */
  public boolean shouldTerminate() {
    return cancelled || isTimedOut() || error != null;
  }

  @Override
  public String toString() {
    return String.format(
        "AsyncExecutionContext{functionName='%s', executionId='%s', elapsed=%s, cancelled=%s,"
            + " completed=%s}",
        functionName, executionId, getElapsedTime(), cancelled, completed);
  }
}

package ai.tegmentum.wasmtime4j.debug;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a debugging event that occurred during WebAssembly execution.
 *
 * <p>Debug events are generated when execution is paused due to breakpoints, stepping, or other
 * debugging operations. Each event contains information about the current execution state and the
 * reason for the pause.
 *
 * @since 1.0.0
 */
public final class DebugEvent {

  /** The type of debug event. */
  private final DebugEventType type;

  /** The execution state when the event occurred. */
  private final ExecutionState executionState;

  /** Additional event-specific data. */
  private final Map<String, Object> data;

  /** Timestamp when the event was created. */
  private final long timestamp;

  /**
   * Creates a new debug event.
   *
   * @param type the event type
   * @param executionState the execution state
   */
  public DebugEvent(final DebugEventType type, final ExecutionState executionState) {
    this(type, executionState, Collections.emptyMap());
  }

  /**
   * Creates a new debug event with additional data.
   *
   * @param type the event type
   * @param executionState the execution state
   * @param data additional event data
   */
  public DebugEvent(
      final DebugEventType type,
      final ExecutionState executionState,
      final Map<String, Object> data) {
    this.type = Objects.requireNonNull(type, "Event type cannot be null");
    this.executionState = Objects.requireNonNull(executionState, "Execution state cannot be null");
    this.data = Collections.unmodifiableMap(new HashMap<>(data));
    this.timestamp = System.currentTimeMillis();
  }

  /**
   * Gets the event type.
   *
   * @return the event type
   */
  public DebugEventType getType() {
    return type;
  }

  /**
   * Gets the execution state when the event occurred.
   *
   * @return the execution state
   */
  public ExecutionState getExecutionState() {
    return executionState;
  }

  /**
   * Gets additional event-specific data.
   *
   * @return an unmodifiable map of event data
   */
  public Map<String, Object> getData() {
    return data;
  }

  /**
   * Gets a specific data value.
   *
   * @param key the data key
   * @return the data value, or null if not present
   */
  public Object getData(final String key) {
    return data.get(key);
  }

  /**
   * Gets the timestamp when the event was created.
   *
   * @return the event timestamp in milliseconds since epoch
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * Checks if the event has specific data.
   *
   * @param key the data key
   * @return true if the data is present
   */
  public boolean hasData(final String key) {
    return data.containsKey(key);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DebugEvent other = (DebugEvent) obj;
    return Objects.equals(type, other.type)
        && Objects.equals(executionState, other.executionState)
        && Objects.equals(data, other.data)
        && timestamp == other.timestamp;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, executionState, data, timestamp);
  }

  @Override
  public String toString() {
    return String.format(
        "DebugEvent{type=%s, executionState=%s, data=%s, timestamp=%d}",
        type, executionState, data, timestamp);
  }

  /** Enumeration of debug event types. */
  public enum DebugEventType {
    /** Execution paused at a breakpoint. */
    BREAKPOINT,

    /** Execution paused after a step operation. */
    STEP,

    /** Execution paused explicitly. */
    PAUSE,

    /** Execution resumed. */
    RESUME,

    /** Execution completed. */
    COMPLETE,

    /** An exception occurred. */
    EXCEPTION,

    /** Execution terminated abnormally. */
    TERMINATE
  }
}

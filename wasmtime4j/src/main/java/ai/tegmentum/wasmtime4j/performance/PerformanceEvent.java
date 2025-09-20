package ai.tegmentum.wasmtime4j.performance;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a significant performance event during WebAssembly execution.
 *
 * <p>PerformanceEvent captures discrete events that may impact performance including function
 * calls, memory operations, compilation events, and threshold violations.
 *
 * <p>Events are timestamped and include contextual information to aid in performance analysis
 * and debugging.
 *
 * @since 1.0.0
 */
public interface PerformanceEvent {

  /**
   * Gets the timestamp when this event occurred.
   *
   * @return event timestamp
   */
  Instant getTimestamp();

  /**
   * Gets the type of performance event.
   *
   * @return event type
   */
  PerformanceEventType getEventType();

  /**
   * Gets the severity level of this event.
   *
   * <p>Severity indicates the potential impact on performance, from informational
   * events to critical performance issues.
   *
   * @return event severity level
   */
  PerformanceEventSeverity getSeverity();

  /**
   * Gets a human-readable description of the event.
   *
   * @return event description
   */
  String getDescription();

  /**
   * Gets the WebAssembly instance associated with this event.
   *
   * <p>May be null for events that are not specific to a particular instance.
   *
   * @return instance identifier, or null if not applicable
   */
  String getInstanceId();

  /**
   * Gets the function name associated with this event.
   *
   * <p>Applicable for function-related events such as calls, compilation, or optimization.
   *
   * @return function name, or null if not applicable
   */
  String getFunctionName();

  /**
   * Gets the duration of this event if applicable.
   *
   * <p>For events that represent operations with measurable duration such as function
   * calls or compilation events.
   *
   * @return event duration in nanoseconds, or 0 if not applicable
   */
  long getDurationNanos();

  /**
   * Gets additional event-specific data.
   *
   * <p>Contains contextual information specific to the event type, such as memory
   * addresses, instruction counts, or error details.
   *
   * @return map of event data properties
   */
  Map<String, Object> getEventData();

  /**
   * Gets the thread identifier where this event occurred.
   *
   * <p>Useful for correlating events in multi-threaded WebAssembly execution.
   *
   * @return thread identifier
   */
  long getThreadId();

  /**
   * Gets the call stack at the time of this event if available.
   *
   * <p>Provides context for where in the execution this event occurred.
   *
   * @return call stack information, or null if not available
   */
  CallStackInfo getCallStack();

  /**
   * Gets the memory usage at the time of this event.
   *
   * <p>Provides memory context for analysis of memory-related performance events.
   *
   * @return memory usage in bytes, or -1 if not available
   */
  long getMemoryUsage();

  /**
   * Checks if this event indicates a performance threshold violation.
   *
   * @return true if this event represents a threshold violation
   */
  boolean isThresholdViolation();

  /**
   * Gets the performance threshold that was violated, if applicable.
   *
   * @return violated threshold information, or null if not applicable
   */
  ThresholdViolationInfo getThresholdViolation();

  /**
   * Gets the correlation ID for related events.
   *
   * <p>Events that are part of the same logical operation (such as a function call
   * and its completion) will share the same correlation ID.
   *
   * @return correlation ID, or null if not correlated
   */
  String getCorrelationId();

  /**
   * Creates a formatted string representation of this event.
   *
   * <p>Includes timestamp, event type, description, and key metrics in a readable format.
   *
   * @return formatted event string
   */
  String toString();
}
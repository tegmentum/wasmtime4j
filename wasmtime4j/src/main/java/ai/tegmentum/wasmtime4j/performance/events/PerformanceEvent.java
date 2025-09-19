package ai.tegmentum.wasmtime4j.performance.events;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a performance event with associated data and metadata.
 *
 * <p>Performance events are generated when significant performance conditions are detected,
 * such as high resource usage, slow operations, or potential bottlenecks.
 *
 * @since 1.0.0
 */
public final class PerformanceEvent {
  private final PerformanceEventType type;
  private final Instant timestamp;
  private final Map<String, Object> data;
  private final String message;
  private final double severity;

  /**
   * Creates a performance event.
   *
   * @param type the event type
   * @param timestamp when the event occurred
   * @param data additional event data
   * @param message human-readable event message
   * @param severity event severity (0.0 to 1.0, where 1.0 is most severe)
   */
  public PerformanceEvent(
      final PerformanceEventType type,
      final Instant timestamp,
      final Map<String, Object> data,
      final String message,
      final double severity) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
    this.data = Map.copyOf(Objects.requireNonNull(data, "data cannot be null"));
    this.message = Objects.requireNonNull(message, "message cannot be null");
    this.severity = Math.max(0.0, Math.min(1.0, severity));
  }

  /**
   * Creates a performance event with current timestamp.
   *
   * @param type the event type
   * @param data additional event data
   * @param message human-readable event message
   * @param severity event severity (0.0 to 1.0)
   * @return new performance event
   */
  public static PerformanceEvent create(
      final PerformanceEventType type,
      final Map<String, Object> data,
      final String message,
      final double severity) {
    return new PerformanceEvent(type, Instant.now(), data, message, severity);
  }

  /**
   * Creates a performance event with automatic severity based on type.
   *
   * @param type the event type
   * @param data additional event data
   * @param message human-readable event message
   * @return new performance event
   */
  public static PerformanceEvent create(
      final PerformanceEventType type,
      final Map<String, Object> data,
      final String message) {
    final double autoSeverity = type.isCritical() ? 0.8 : 0.5;
    return create(type, data, message, autoSeverity);
  }

  /**
   * Gets the event type.
   *
   * @return event type
   */
  public PerformanceEventType getType() {
    return type;
  }

  /**
   * Gets the timestamp when this event occurred.
   *
   * @return event timestamp
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the event data.
   *
   * @return map of event data
   */
  public Map<String, Object> getData() {
    return data;
  }

  /**
   * Gets the human-readable event message.
   *
   * @return event message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets the event severity.
   *
   * @return severity (0.0 to 1.0)
   */
  public double getSeverity() {
    return severity;
  }

  /**
   * Gets a specific data value as a string.
   *
   * @param key the data key
   * @return data value as string, or empty string if not found
   */
  public String getDataAsString(final String key) {
    final Object value = data.get(key);
    return value != null ? value.toString() : "";
  }

  /**
   * Gets a specific data value as a number.
   *
   * @param key the data key
   * @return data value as double, or 0.0 if not found or not numeric
   */
  public double getDataAsNumber(final String key) {
    final Object value = data.get(key);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    return 0.0;
  }

  /**
   * Gets a specific data value as a boolean.
   *
   * @param key the data key
   * @return data value as boolean, or false if not found or not boolean
   */
  public boolean getDataAsBoolean(final String key) {
    final Object value = data.get(key);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return false;
  }

  /**
   * Checks if this event has a specific data key.
   *
   * @param key the data key
   * @return true if the key exists
   */
  public boolean hasData(final String key) {
    return data.containsKey(key);
  }

  /**
   * Checks if this is a critical event.
   *
   * @return true if critical
   */
  public boolean isCritical() {
    return type.isCritical() || severity > 0.7;
  }

  /**
   * Checks if this is a warning-level event.
   *
   * @return true if warning level
   */
  public boolean isWarning() {
    return severity > 0.4 && severity <= 0.7;
  }

  /**
   * Checks if this is an informational event.
   *
   * @return true if informational
   */
  public boolean isInformational() {
    return severity <= 0.4;
  }

  /**
   * Gets the severity level as a string.
   *
   * @return severity level ("CRITICAL", "WARNING", "INFO")
   */
  public String getSeverityLevel() {
    if (isCritical()) {
      return "CRITICAL";
    } else if (isWarning()) {
      return "WARNING";
    } else {
      return "INFO";
    }
  }

  /**
   * Formats this event for logging.
   *
   * @return formatted event string
   */
  public String toLogString() {
    return String.format("[%s] %s: %s (severity: %.2f) - %s",
        getSeverityLevel(), type.name(), message, severity, data);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final PerformanceEvent that = (PerformanceEvent) obj;
    return Double.compare(that.severity, severity) == 0 &&
        type == that.type &&
        Objects.equals(timestamp, that.timestamp) &&
        Objects.equals(data, that.data) &&
        Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, timestamp, data, message, severity);
  }

  @Override
  public String toString() {
    return String.format(
        "PerformanceEvent{type=%s, timestamp=%s, message='%s', severity=%.2f, data=%s}",
        type, timestamp, message, severity, data);
  }
}
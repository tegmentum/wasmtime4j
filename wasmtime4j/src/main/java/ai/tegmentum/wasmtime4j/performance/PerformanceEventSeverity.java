package ai.tegmentum.wasmtime4j.performance;

/**
 * Severity levels for performance events.
 *
 * <p>These severity levels indicate the potential impact of performance events on
 * overall system performance and operation.
 *
 * @since 1.0.0
 */
public enum PerformanceEventSeverity {

  /** Informational event with no performance impact. */
  INFO,

  /** Low-impact event that may indicate minor performance variations. */
  LOW,

  /** Medium-impact event that may affect performance but is within acceptable bounds. */
  MEDIUM,

  /** High-impact event that significantly affects performance. */
  HIGH,

  /** Critical event that severely impacts performance or indicates serious issues. */
  CRITICAL
}
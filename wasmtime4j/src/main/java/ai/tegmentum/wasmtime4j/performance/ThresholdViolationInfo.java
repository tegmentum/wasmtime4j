package ai.tegmentum.wasmtime4j.performance;

import java.time.Instant;

/**
 * Information about a performance threshold violation.
 *
 * <p>ThresholdViolationInfo provides details about which threshold was exceeded,
 * by how much, and the context in which the violation occurred.
 *
 * @since 1.0.0
 */
public interface ThresholdViolationInfo {

  /**
   * Gets the timestamp when the threshold violation occurred.
   *
   * @return violation timestamp
   */
  Instant getTimestamp();

  /**
   * Gets the type of threshold that was violated.
   *
   * @return threshold type
   */
  ThresholdType getThresholdType();

  /**
   * Gets the configured threshold value that was exceeded.
   *
   * @return threshold value
   */
  Object getThresholdValue();

  /**
   * Gets the actual measured value that exceeded the threshold.
   *
   * @return measured value
   */
  Object getMeasuredValue();

  /**
   * Gets the percentage by which the threshold was exceeded.
   *
   * <p>For example, if the threshold was 100ms and the measured value was 150ms,
   * this would return 50.0 (50% over the threshold).
   *
   * @return percentage over threshold
   */
  double getExcessPercentage();

  /**
   * Gets the severity of this threshold violation.
   *
   * @return violation severity
   */
  PerformanceEventSeverity getSeverity();

  /**
   * Gets a human-readable description of the violation.
   *
   * @return violation description
   */
  String getDescription();

  /**
   * Gets the WebAssembly instance associated with this violation.
   *
   * <p>May be null for violations that are not instance-specific.
   *
   * @return instance identifier, or null if not applicable
   */
  String getInstanceId();

  /**
   * Gets the function name associated with this violation.
   *
   * <p>Applicable for function-related threshold violations.
   *
   * @return function name, or null if not applicable
   */
  String getFunctionName();

  /**
   * Gets the thread where the violation occurred.
   *
   * @return thread identifier
   */
  long getThreadId();

  /**
   * Gets suggested actions to address this threshold violation.
   *
   * @return suggested remediation actions
   */
  String getSuggestedActions();

  /**
   * Enumeration of threshold types that can be violated.
   */
  enum ThresholdType {
    /** Function execution time threshold. */
    FUNCTION_EXECUTION_TIME,

    /** Memory allocation size threshold. */
    ALLOCATION_SIZE,

    /** Total memory usage threshold. */
    TOTAL_MEMORY_USAGE,

    /** CPU usage threshold. */
    CPU_USAGE,

    /** Instructions per second threshold. */
    INSTRUCTIONS_PER_SECOND,

    /** Garbage collection frequency threshold. */
    GC_FREQUENCY,

    /** JIT compilation time threshold. */
    JIT_COMPILATION_TIME,

    /** Host function call frequency threshold. */
    HOST_FUNCTION_CALL_FREQUENCY,

    /** Error rate threshold. */
    ERROR_RATE,

    /** Monitoring overhead threshold. */
    MONITORING_OVERHEAD
  }
}
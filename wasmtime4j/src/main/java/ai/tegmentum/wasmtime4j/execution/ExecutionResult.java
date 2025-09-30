package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution result interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionResult {

  /**
   * Gets the request ID.
   *
   * @return request ID
   */
  String getRequestId();

  /**
   * Gets the execution status.
   *
   * @return execution status
   */
  ExecutionStatus getStatus();

  /**
   * Gets the execution result value.
   *
   * @return result value, or null if none
   */
  Object getResult();

  /**
   * Gets the execution error.
   *
   * @return error, or null if none
   */
  Throwable getError();

  /**
   * Gets the execution duration in nanoseconds.
   *
   * @return execution duration
   */
  long getDuration();

  /**
   * Gets the execution statistics.
   *
   * @return execution statistics
   */
  ExecutionStatistics getStatistics();

  /**
   * Gets the execution metadata.
   *
   * @return metadata map
   */
  java.util.Map<String, Object> getMetadata();

  /**
   * Gets the completion timestamp.
   *
   * @return timestamp
   */
  long getTimestamp();

  /**
   * Gets the memory usage peak during execution.
   *
   * @return peak memory usage in bytes
   */
  long getPeakMemoryUsage();

  /**
   * Gets the final memory usage after execution.
   *
   * @return final memory usage in bytes
   */
  long getFinalMemoryUsage();

  /**
   * Gets warning messages generated during execution.
   *
   * @return list of warnings
   */
  java.util.List<String> getWarnings();

  /**
   * Checks if the execution was successful.
   *
   * @return true if successful
   */
  boolean isSuccessful();

  /**
   * Checks if the execution was terminated.
   *
   * @return true if terminated
   */
  boolean isTerminated();

  /**
   * Gets the termination reason.
   *
   * @return termination reason, or null if not terminated
   */
  TerminationReason getTerminationReason();

  /** Termination reason enumeration. */
  enum TerminationReason {
    /** Execution completed normally. */
    COMPLETED,
    /** Execution timeout exceeded. */
    TIMEOUT,
    /** Memory quota exceeded. */
    MEMORY_LIMIT,
    /** Fuel quota exceeded. */
    FUEL_LIMIT,
    /** Instruction quota exceeded. */
    INSTRUCTION_LIMIT,
    /** User requested termination. */
    USER_REQUESTED,
    /** System error occurred. */
    SYSTEM_ERROR,
    /** Security violation. */
    SECURITY_VIOLATION
  }
}

package ai.tegmentum.wasmtime4j.debug;

/**
 * Execution result interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionResult {

  /**
   * Gets the execution status.
   *
   * @return execution status
   */
  ExecutionStatus getStatus();

  /**
   * Gets the result value.
   *
   * @return result value or null
   */
  Object getResult();

  /**
   * Gets the execution time in nanoseconds.
   *
   * @return execution time
   */
  long getExecutionTime();

  /**
   * Gets the error message if execution failed.
   *
   * @return error message or null
   */
  String getErrorMessage();

  /**
   * Gets the exception if execution failed.
   *
   * @return exception or null
   */
  Throwable getException();

  /**
   * Gets execution statistics.
   *
   * @return execution statistics
   */
  ExecutionStatistics getStatistics();

  /** Execution status enumeration. */
  enum ExecutionStatus {
    /** Execution completed successfully. */
    SUCCESS,
    /** Execution failed with error. */
    ERROR,
    /** Execution was cancelled. */
    CANCELLED,
    /** Execution timed out. */
    TIMEOUT,
    /** Execution was interrupted. */
    INTERRUPTED
  }

  /** Execution statistics interface. */
  interface ExecutionStatistics {
    /**
     * Gets the instruction count.
     *
     * @return instruction count
     */
    long getInstructionCount();

    /**
     * Gets the memory allocations.
     *
     * @return allocation count
     */
    long getMemoryAllocations();

    /**
     * Gets the function calls.
     *
     * @return function call count
     */
    long getFunctionCalls();

    /**
     * Gets the peak memory usage.
     *
     * @return peak memory in bytes
     */
    long getPeakMemoryUsage();
  }
}

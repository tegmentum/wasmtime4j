package ai.tegmentum.wasmtime4j.component;

/**
 * Execution state interface for WebAssembly execution monitoring.
 *
 * @since 1.0.0
 */
public interface ExecutionState {

  /**
   * Gets the current execution status.
   *
   * @return execution status
   */
  ai.tegmentum.wasmtime4j.execution.ExecutionStatus getStatus();

  /**
   * Gets the current instruction pointer.
   *
   * @return instruction pointer
   */
  long getInstructionPointer();

  /**
   * Gets the current stack pointer.
   *
   * @return stack pointer
   */
  long getStackPointer();

  /**
   * Gets the current frame pointer.
   *
   * @return frame pointer
   */
  long getFramePointer();

  /**
   * Gets the currently executing function.
   *
   * @return function name or null
   */
  String getCurrentFunction();

  /**
   * Gets the current execution context.
   *
   * @return execution context
   */
  ai.tegmentum.wasmtime4j.execution.ExecutionContext getContext();

  /**
   * Gets execution statistics.
   *
   * @return execution statistics
   */
  ExecutionStatistics getStatistics();

  /**
   * Gets the current call stack depth.
   *
   * @return stack depth
   */
  int getStackDepth();

  /**
   * Gets the current memory usage.
   *
   * @return memory usage in bytes
   */
  long getMemoryUsage();

  /**
   * Gets the execution start time.
   *
   * @return start timestamp
   */
  long getStartTime();

  /**
   * Gets the current execution time.
   *
   * @return elapsed time in milliseconds
   */
  long getElapsedTime();

  /**
   * Gets the last error if execution failed.
   *
   * @return last error or null
   */
  Throwable getLastError();

  /** Execution statistics interface. */
  interface ExecutionStatistics {
    /**
     * Gets instructions executed.
     *
     * @return instruction count
     */
    long getInstructionsExecuted();

    /**
     * Gets function calls made.
     *
     * @return function call count
     */
    long getFunctionCalls();

    /**
     * Gets memory allocations.
     *
     * @return allocation count
     */
    long getMemoryAllocations();

    /**
     * Gets I/O operations.
     *
     * @return I/O operation count
     */
    long getIoOperations();

    /**
     * Gets system calls made.
     *
     * @return system call count
     */
    long getSystemCalls();

    /**
     * Gets exceptions thrown.
     *
     * @return exception count
     */
    long getExceptions();
  }
}

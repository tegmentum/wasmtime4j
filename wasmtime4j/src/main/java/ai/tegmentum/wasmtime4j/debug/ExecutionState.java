package ai.tegmentum.wasmtime4j.debug;

/**
 * Execution state interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionState {

  /**
   * Gets the current execution status.
   *
   * @return execution status
   */
  ExecutionStatus getStatus();

  /**
   * Gets the current instruction pointer.
   *
   * @return instruction pointer
   */
  long getInstructionPointer();

  /**
   * Gets the current stack frames.
   *
   * @return list of stack frames
   */
  java.util.List<StackFrame> getStackFrames();

  /**
   * Gets the current module name.
   *
   * @return module name
   */
  String getCurrentModule();

  /**
   * Gets the current function name.
   *
   * @return function name
   */
  String getCurrentFunction();

  /**
   * Gets the execution statistics.
   *
   * @return execution statistics
   */
  ExecutionStatistics getStatistics();

  /** Execution status enumeration. */
  enum ExecutionStatus {
    /** Running. */
    RUNNING,
    /** Paused at breakpoint. */
    PAUSED,
    /** Stopped. */
    STOPPED,
    /** Crashed. */
    CRASHED,
    /** Completed. */
    COMPLETED
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
     * Gets the execution time in milliseconds.
     *
     * @return execution time
     */
    long getExecutionTime();

    /**
     * Gets the function call count.
     *
     * @return call count
     */
    long getFunctionCallCount();
  }
}

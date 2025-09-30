package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution state interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionState {

  /**
   * Gets the execution ID.
   *
   * @return execution ID
   */
  String getExecutionId();

  /**
   * Gets the current execution status.
   *
   * @return execution status
   */
  ExecutionStatus getStatus();

  /**
   * Gets the execution phase.
   *
   * @return execution phase
   */
  ExecutionPhase getPhase();

  /**
   * Gets the execution start time.
   *
   * @return start timestamp
   */
  long getStartTime();

  /**
   * Gets the execution current time.
   *
   * @return current timestamp
   */
  long getCurrentTime();

  /**
   * Gets the execution duration so far.
   *
   * @return duration in nanoseconds
   */
  long getDuration();

  /**
   * Gets the current execution context.
   *
   * @return execution context
   */
  ExecutionContext getContext();

  /**
   * Gets the current execution quotas.
   *
   * @return execution quotas
   */
  ExecutionQuotas getQuotas();

  /**
   * Gets the current execution statistics.
   *
   * @return execution statistics
   */
  ExecutionStatistics getStatistics();

  /**
   * Gets the execution stack trace.
   *
   * @return stack trace
   */
  java.util.List<StackFrame> getStackTrace();

  /**
   * Gets the current function being executed.
   *
   * @return current function info
   */
  CurrentFunctionInfo getCurrentFunction();

  /**
   * Gets the execution metadata.
   *
   * @return metadata map
   */
  java.util.Map<String, Object> getMetadata();

  /**
   * Gets the execution warnings.
   *
   * @return list of warnings
   */
  java.util.List<ExecutionWarning> getWarnings();

  /**
   * Gets the execution error (if any).
   *
   * @return execution error, or null if none
   */
  Throwable getError();

  /**
   * Checks if execution is active.
   *
   * @return true if active
   */
  boolean isActive();

  /**
   * Checks if execution is suspended.
   *
   * @return true if suspended
   */
  boolean isSuspended();

  /**
   * Checks if execution is completed.
   *
   * @return true if completed
   */
  boolean isCompleted();

  /**
   * Checks if execution has failed.
   *
   * @return true if failed
   */
  boolean hasFailed();

  /**
   * Gets the suspension reason (if suspended).
   *
   * @return suspension reason, or null if not suspended
   */
  SuspensionReason getSuspensionReason();

  /**
   * Gets the termination reason (if terminated).
   *
   * @return termination reason, or null if not terminated
   */
  TerminationReason getTerminationReason();

  /**
   * Gets execution progress information.
   *
   * @return progress info
   */
  ProgressInfo getProgress();

  /**
   * Creates a snapshot of the current execution state.
   *
   * @return state snapshot
   */
  ExecutionStateSnapshot createSnapshot();

  /** Execution phase enumeration. */
  enum ExecutionPhase {
    /** Initialization phase. */
    INITIALIZATION,
    /** Pre-execution phase. */
    PRE_EXECUTION,
    /** Function execution. */
    FUNCTION_EXECUTION,
    /** Host function call. */
    HOST_FUNCTION_CALL,
    /** Memory operation. */
    MEMORY_OPERATION,
    /** Exception handling. */
    EXCEPTION_HANDLING,
    /** Post-execution phase. */
    POST_EXECUTION,
    /** Cleanup phase. */
    CLEANUP,
    /** Finalization phase. */
    FINALIZATION
  }

  /** Current function information interface. */
  interface CurrentFunctionInfo {
    /**
     * Gets the function name.
     *
     * @return function name
     */
    String getName();

    /**
     * Gets the module name.
     *
     * @return module name
     */
    String getModuleName();

    /**
     * Gets the function index.
     *
     * @return function index
     */
    int getIndex();

    /**
     * Gets the current instruction pointer.
     *
     * @return instruction pointer
     */
    int getInstructionPointer();

    /**
     * Gets function parameters.
     *
     * @return parameter values
     */
    Object[] getParameters();

    /**
     * Gets local variables.
     *
     * @return local variables
     */
    java.util.Map<String, Object> getLocalVariables();

    /**
     * Gets the function entry time.
     *
     * @return entry timestamp
     */
    long getEntryTime();

    /**
     * Gets the function execution duration so far.
     *
     * @return duration in nanoseconds
     */
    long getExecutionDuration();
  }

  /** Execution warning interface. */
  interface ExecutionWarning {
    /**
     * Gets the warning type.
     *
     * @return warning type
     */
    WarningType getType();

    /**
     * Gets the warning message.
     *
     * @return warning message
     */
    String getMessage();

    /**
     * Gets the warning timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Gets the warning context.
     *
     * @return warning context
     */
    String getContext();

    /**
     * Gets the warning severity.
     *
     * @return severity level
     */
    WarningSeverity getSeverity();
  }

  /** Progress information interface. */
  interface ProgressInfo {
    /**
     * Gets the completion percentage.
     *
     * @return completion percentage (0.0-1.0)
     */
    double getCompletionPercentage();

    /**
     * Gets the estimated time remaining.
     *
     * @return estimated time in milliseconds
     */
    long getEstimatedTimeRemaining();

    /**
     * Gets progress milestones.
     *
     * @return list of milestones
     */
    java.util.List<ProgressMilestone> getMilestones();

    /**
     * Gets the current milestone.
     *
     * @return current milestone, or null if none
     */
    ProgressMilestone getCurrentMilestone();
  }

  /** Progress milestone interface. */
  interface ProgressMilestone {
    /**
     * Gets the milestone name.
     *
     * @return milestone name
     */
    String getName();

    /**
     * Gets the milestone description.
     *
     * @return milestone description
     */
    String getDescription();

    /**
     * Gets the milestone completion percentage.
     *
     * @return completion percentage (0.0-1.0)
     */
    double getCompletionPercentage();

    /**
     * Gets the milestone timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Checks if the milestone is completed.
     *
     * @return true if completed
     */
    boolean isCompleted();
  }

  /** Execution state snapshot interface. */
  interface ExecutionStateSnapshot {
    /**
     * Gets the snapshot timestamp.
     *
     * @return snapshot timestamp
     */
    long getSnapshotTime();

    /**
     * Gets the execution state at snapshot time.
     *
     * @return execution state
     */
    ExecutionState getState();

    /**
     * Gets the snapshot metadata.
     *
     * @return metadata map
     */
    java.util.Map<String, Object> getMetadata();
  }

  /** Stack frame interface. */
  interface StackFrame {
    /**
     * Gets the function name.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets the module name.
     *
     * @return module name
     */
    String getModuleName();

    /**
     * Gets the instruction pointer.
     *
     * @return instruction pointer
     */
    int getInstructionPointer();

    /**
     * Gets the frame depth.
     *
     * @return frame depth
     */
    int getDepth();

    /**
     * Gets frame local variables.
     *
     * @return local variables
     */
    java.util.Map<String, Object> getLocalVariables();
  }

  /** Suspension reason enumeration. */
  enum SuspensionReason {
    /** User requested suspension. */
    USER_REQUESTED,
    /** Resource limit reached. */
    RESOURCE_LIMIT,
    /** Breakpoint hit. */
    BREAKPOINT,
    /** Debugging pause. */
    DEBUGGING,
    /** System overload. */
    SYSTEM_OVERLOAD,
    /** Waiting for external resource. */
    WAITING_FOR_RESOURCE
  }

  /** Termination reason enumeration. */
  enum TerminationReason {
    /** Normal completion. */
    COMPLETED,
    /** User requested termination. */
    USER_TERMINATED,
    /** Timeout exceeded. */
    TIMEOUT,
    /** Memory limit exceeded. */
    MEMORY_LIMIT,
    /** Fuel exhausted. */
    FUEL_EXHAUSTED,
    /** Exception thrown. */
    EXCEPTION,
    /** System error. */
    SYSTEM_ERROR,
    /** Security violation. */
    SECURITY_VIOLATION
  }

  /** Warning type enumeration. */
  enum WarningType {
    /** Performance warning. */
    PERFORMANCE,
    /** Memory usage warning. */
    MEMORY_USAGE,
    /** Quota approaching limit. */
    QUOTA_LIMIT,
    /** Resource contention. */
    RESOURCE_CONTENTION,
    /** Deprecated feature usage. */
    DEPRECATED_FEATURE,
    /** Security concern. */
    SECURITY,
    /** Custom warning. */
    CUSTOM
  }

  /** Warning severity enumeration. */
  enum WarningSeverity {
    /** Info level. */
    INFO,
    /** Low severity. */
    LOW,
    /** Medium severity. */
    MEDIUM,
    /** High severity. */
    HIGH
  }
}

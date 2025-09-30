package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution controller interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionController {

  /** Starts execution control. */
  void start();

  /** Stops execution control. */
  void stop();

  /** Pauses execution. */
  void pause();

  /** Resumes execution. */
  void resume();

  /**
   * Sets execution quotas.
   *
   * @param quotas execution quotas
   */
  void setQuotas(ExecutionQuotas quotas);

  /**
   * Gets current execution quotas.
   *
   * @return execution quotas
   */
  ExecutionQuotas getQuotas();

  /**
   * Sets execution policy.
   *
   * @param policy execution policy
   */
  void setPolicy(ExecutionPolicy policy);

  /**
   * Gets current execution policy.
   *
   * @return execution policy
   */
  ExecutionPolicy getPolicy();

  /**
   * Gets execution status.
   *
   * @return execution status
   */
  ExecutionStatus getStatus();

  /**
   * Gets execution statistics.
   *
   * @return execution statistics
   */
  ExecutionStatistics getStatistics();

  /**
   * Checks if execution is active.
   *
   * @return true if active
   */
  boolean isActive();

  /** Execution status enumeration. */
  enum ExecutionStatus {
    /** Not started. */
    IDLE,
    /** Currently running. */
    RUNNING,
    /** Paused. */
    PAUSED,
    /** Stopped. */
    STOPPED,
    /** Error state. */
    ERROR
  }
}

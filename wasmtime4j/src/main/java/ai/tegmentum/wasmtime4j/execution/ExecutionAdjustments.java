package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution adjustments interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionAdjustments {

  /**
   * Gets the execution ID.
   *
   * @return execution ID
   */
  String getExecutionId();

  /**
   * Gets quota adjustments.
   *
   * @return quota adjustments
   */
  QuotaAdjustments getQuotaAdjustments();

  /**
   * Sets quota adjustments.
   *
   * @param adjustments quota adjustments
   */
  void setQuotaAdjustments(QuotaAdjustments adjustments);

  /**
   * Gets priority adjustments.
   *
   * @return priority adjustments
   */
  PriorityAdjustments getPriorityAdjustments();

  /**
   * Sets priority adjustments.
   *
   * @param adjustments priority adjustments
   */
  void setPriorityAdjustments(PriorityAdjustments adjustments);

  /**
   * Gets performance adjustments.
   *
   * @return performance adjustments
   */
  PerformanceAdjustments getPerformanceAdjustments();

  /**
   * Sets performance adjustments.
   *
   * @param adjustments performance adjustments
   */
  void setPerformanceAdjustments(PerformanceAdjustments adjustments);

  /**
   * Gets the adjustment timestamp.
   *
   * @return timestamp
   */
  long getTimestamp();

  /**
   * Gets the adjustment reason.
   *
   * @return adjustment reason
   */
  String getReason();

  /**
   * Applies the adjustments.
   *
   * @return true if successful
   */
  boolean apply();

  /**
   * Reverts the adjustments.
   *
   * @return true if successful
   */
  boolean revert();

  /** Quota adjustments interface. */
  interface QuotaAdjustments {
    /**
     * Gets fuel quota adjustment.
     *
     * @return fuel adjustment
     */
    long getFuelAdjustment();

    /**
     * Gets memory quota adjustment.
     *
     * @return memory adjustment in bytes
     */
    long getMemoryAdjustment();

    /**
     * Gets time quota adjustment.
     *
     * @return time adjustment in milliseconds
     */
    long getTimeAdjustment();

    /**
     * Gets instruction quota adjustment.
     *
     * @return instruction adjustment
     */
    long getInstructionAdjustment();
  }

  /** Priority adjustments interface. */
  interface PriorityAdjustments {
    /**
     * Gets the new priority level.
     *
     * @return priority level
     */
    int getPriorityLevel();

    /**
     * Gets the priority boost factor.
     *
     * @return boost factor
     */
    double getBoostFactor();
  }

  /** Performance adjustments interface. */
  interface PerformanceAdjustments {
    /**
     * Gets CPU affinity adjustments.
     *
     * @return CPU affinity set
     */
    java.util.Set<Integer> getCpuAffinity();

    /**
     * Gets thread pool size adjustment.
     *
     * @return thread pool size
     */
    int getThreadPoolSize();

    /**
     * Gets optimization level adjustment.
     *
     * @return optimization level
     */
    OptimizationLevel getOptimizationLevel();
  }

  /** Optimization level enumeration. */
  enum OptimizationLevel {
    /** No optimization. */
    NONE,
    /** Basic optimization. */
    BASIC,
    /** Standard optimization. */
    STANDARD,
    /** Aggressive optimization. */
    AGGRESSIVE
  }
}

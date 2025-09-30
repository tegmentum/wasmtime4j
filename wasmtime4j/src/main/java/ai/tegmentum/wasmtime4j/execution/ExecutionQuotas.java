package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution quotas interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionQuotas {

  /**
   * Gets the fuel quota.
   *
   * @return fuel quota
   */
  long getFuelQuota();

  /**
   * Sets the fuel quota.
   *
   * @param fuel fuel quota
   */
  void setFuelQuota(long fuel);

  /**
   * Gets the remaining fuel.
   *
   * @return remaining fuel
   */
  long getRemainingFuel();

  /**
   * Gets the memory quota in bytes.
   *
   * @return memory quota
   */
  long getMemoryQuota();

  /**
   * Sets the memory quota.
   *
   * @param memoryBytes memory quota in bytes
   */
  void setMemoryQuota(long memoryBytes);

  /**
   * Gets the time quota in milliseconds.
   *
   * @return time quota
   */
  long getTimeQuota();

  /**
   * Sets the time quota.
   *
   * @param timeMs time quota in milliseconds
   */
  void setTimeQuota(long timeMs);

  /**
   * Gets the remaining time in milliseconds.
   *
   * @return remaining time
   */
  long getRemainingTime();

  /**
   * Gets the instruction quota.
   *
   * @return instruction quota
   */
  long getInstructionQuota();

  /**
   * Sets the instruction quota.
   *
   * @param instructions instruction quota
   */
  void setInstructionQuota(long instructions);

  /**
   * Gets the remaining instructions.
   *
   * @return remaining instructions
   */
  long getRemainingInstructions();

  /**
   * Checks if any quota has been exceeded.
   *
   * @return true if quota exceeded
   */
  boolean isQuotaExceeded();

  /**
   * Gets quota usage statistics.
   *
   * @return quota usage statistics
   */
  QuotaUsage getUsage();

  /** Resets all quotas to their initial values. */
  void reset();

  /** Quota usage statistics interface. */
  interface QuotaUsage {
    /**
     * Gets fuel usage percentage.
     *
     * @return fuel usage (0.0-1.0)
     */
    double getFuelUsage();

    /**
     * Gets memory usage percentage.
     *
     * @return memory usage (0.0-1.0)
     */
    double getMemoryUsage();

    /**
     * Gets time usage percentage.
     *
     * @return time usage (0.0-1.0)
     */
    double getTimeUsage();

    /**
     * Gets instruction usage percentage.
     *
     * @return instruction usage (0.0-1.0)
     */
    double getInstructionUsage();

    /**
     * Gets the highest usage percentage.
     *
     * @return highest usage (0.0-1.0)
     */
    double getMaxUsage();
  }
}

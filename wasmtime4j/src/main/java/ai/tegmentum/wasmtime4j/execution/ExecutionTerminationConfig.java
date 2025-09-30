package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution termination configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionTerminationConfig {

  /**
   * Gets the termination timeout in milliseconds.
   *
   * @return termination timeout
   */
  long getTerminationTimeout();

  /**
   * Sets the termination timeout.
   *
   * @param timeoutMs termination timeout in milliseconds
   */
  void setTerminationTimeout(long timeoutMs);

  /**
   * Gets the termination strategy.
   *
   * @return termination strategy
   */
  TerminationStrategy getStrategy();

  /**
   * Sets the termination strategy.
   *
   * @param strategy termination strategy
   */
  void setStrategy(TerminationStrategy strategy);

  /**
   * Checks if forced termination is enabled.
   *
   * @return true if forced termination is enabled
   */
  boolean isForcedTerminationEnabled();

  /**
   * Sets forced termination enabled state.
   *
   * @param enabled forced termination enabled state
   */
  void setForcedTerminationEnabled(boolean enabled);

  /**
   * Gets the cleanup timeout in milliseconds.
   *
   * @return cleanup timeout
   */
  long getCleanupTimeout();

  /**
   * Sets the cleanup timeout.
   *
   * @param timeoutMs cleanup timeout in milliseconds
   */
  void setCleanupTimeout(long timeoutMs);

  /**
   * Gets termination handlers.
   *
   * @return list of termination handlers
   */
  java.util.List<TerminationHandler> getHandlers();

  /**
   * Adds a termination handler.
   *
   * @param handler termination handler
   */
  void addHandler(TerminationHandler handler);

  /**
   * Removes a termination handler.
   *
   * @param handler termination handler
   */
  void removeHandler(TerminationHandler handler);

  /** Termination strategy enumeration. */
  enum TerminationStrategy {
    /** Graceful termination. */
    GRACEFUL,
    /** Immediate termination. */
    IMMEDIATE,
    /** Progressive termination. */
    PROGRESSIVE,
    /** Cooperative termination. */
    COOPERATIVE
  }

  /** Termination handler interface. */
  interface TerminationHandler {
    /**
     * Handles termination event.
     *
     * @param context termination context
     */
    void onTermination(TerminationContext context);

    /**
     * Gets handler priority.
     *
     * @return handler priority
     */
    int getPriority();
  }

  /** Termination context interface. */
  interface TerminationContext {
    /**
     * Gets the execution ID.
     *
     * @return execution ID
     */
    String getExecutionId();

    /**
     * Gets the termination reason.
     *
     * @return termination reason
     */
    String getReason();

    /**
     * Gets the termination timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();
  }
}

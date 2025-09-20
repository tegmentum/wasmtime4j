package ai.tegmentum.wasmtime4j.async.reactive;

/**
 * Enumeration of WebAssembly function execution phases.
 *
 * <p>This enum represents the various stages that a WebAssembly function goes through during
 * execution, from initial preparation through completion or failure.
 *
 * <p>These phases are used in reactive execution events to provide detailed progress
 * information and enable monitoring of function execution lifecycle.
 *
 * @since 1.0.0
 */
public enum ExecutionPhase {

  /** Function execution is being prepared and initialized. */
  STARTING("Starting", "Preparing function execution", 0),

  /** Function parameters are being validated and converted. */
  PARAMETER_VALIDATION("Parameter Validation", "Validating function parameters", 1),

  /** Function is actively executing WebAssembly instructions. */
  EXECUTING("Executing", "Executing WebAssembly instructions", 2),

  /** Function execution is in a host function call. */
  HOST_CALL("Host Call", "Executing host function callback", 3),

  /** Function execution is processing return values. */
  RESULT_PROCESSING("Result Processing", "Processing function results", 4),

  /** Function execution completed successfully. */
  COMPLETED("Completed", "Function execution completed successfully", 5),

  /** Function execution failed with an error. */
  FAILED("Failed", "Function execution failed with error", -1),

  /** Function execution was cancelled or interrupted. */
  CANCELLED("Cancelled", "Function execution was cancelled", -2),

  /** Function execution timed out. */
  TIMEOUT("Timeout", "Function execution timed out", -3);

  private final String displayName;
  private final String description;
  private final int order;

  ExecutionPhase(final String displayName, final String description, final int order) {
    this.displayName = displayName;
    this.description = description;
    this.order = order;
  }

  /**
   * Gets the human-readable display name for this phase.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Gets the detailed description of this phase.
   *
   * @return the phase description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the order of this phase in the execution process.
   *
   * <p>Normal phases have positive order values, with higher values indicating later phases.
   * Terminal phases (FAILED, CANCELLED, TIMEOUT) have negative order values.
   *
   * @return the phase order
   */
  public int getOrder() {
    return order;
  }

  /**
   * Checks if this phase represents a successful completion.
   *
   * @return true if this phase indicates successful completion
   */
  public boolean isCompleted() {
    return this == COMPLETED;
  }

  /**
   * Checks if this phase represents a failure state.
   *
   * @return true if this phase indicates failure
   */
  public boolean isFailed() {
    return this == FAILED;
  }

  /**
   * Checks if this phase represents a cancelled state.
   *
   * @return true if this phase indicates cancellation
   */
  public boolean isCancelled() {
    return this == CANCELLED;
  }

  /**
   * Checks if this phase represents a timeout state.
   *
   * @return true if this phase indicates timeout
   */
  public boolean isTimeout() {
    return this == TIMEOUT;
  }

  /**
   * Checks if this phase represents a terminal state.
   *
   * <p>Terminal phases are those where execution cannot proceed further,
   * including completion, failure, cancellation, and timeout.
   *
   * @return true if this is a terminal phase
   */
  public boolean isTerminal() {
    return order < 0 || this == COMPLETED;
  }

  /**
   * Checks if this phase represents an active execution state.
   *
   * <p>Active phases are those where execution is actively proceeding.
   *
   * @return true if this is an active execution phase
   */
  public boolean isActive() {
    return order >= 0 && this != COMPLETED;
  }

  /**
   * Checks if this phase represents an error state.
   *
   * <p>Error phases include failure, cancellation, and timeout.
   *
   * @return true if this is an error phase
   */
  public boolean isError() {
    return order < 0;
  }

  /**
   * Gets the estimated progress percentage for this phase.
   *
   * <p>This provides a rough estimate of execution progress based on
   * the current phase, useful for progress indicators.
   *
   * @return estimated progress percentage (0.0 to 100.0)
   */
  public double getEstimatedProgress() {
    switch (this) {
      case STARTING:
        return 5.0;
      case PARAMETER_VALIDATION:
        return 15.0;
      case EXECUTING:
        return 70.0;
      case HOST_CALL:
        return 80.0;
      case RESULT_PROCESSING:
        return 95.0;
      case COMPLETED:
        return 100.0;
      case FAILED:
      case CANCELLED:
      case TIMEOUT:
        return 0.0;
      default:
        return 0.0;
    }
  }

  /**
   * Gets the next phase in the execution process.
   *
   * @return the next phase, or null if this is a terminal phase
   */
  public ExecutionPhase getNextPhase() {
    switch (this) {
      case STARTING:
        return PARAMETER_VALIDATION;
      case PARAMETER_VALIDATION:
        return EXECUTING;
      case EXECUTING:
        return RESULT_PROCESSING;
      case HOST_CALL:
        return EXECUTING; // Return to execution after host call
      case RESULT_PROCESSING:
        return COMPLETED;
      default:
        return null; // Terminal phases have no next phase
    }
  }

  /**
   * Gets the previous phase in the execution process.
   *
   * @return the previous phase, or null if this is the first phase
   */
  public ExecutionPhase getPreviousPhase() {
    switch (this) {
      case PARAMETER_VALIDATION:
        return STARTING;
      case EXECUTING:
        return PARAMETER_VALIDATION;
      case HOST_CALL:
        return EXECUTING;
      case RESULT_PROCESSING:
        return EXECUTING;
      case COMPLETED:
        return RESULT_PROCESSING;
      default:
        return null; // First phase or terminal phases have no previous phase
    }
  }

  /**
   * Creates an ExecutionPhase from its order value.
   *
   * @param order the phase order
   * @return the corresponding phase, or null if order is invalid
   */
  public static ExecutionPhase fromOrder(final int order) {
    for (final ExecutionPhase phase : values()) {
      if (phase.order == order) {
        return phase;
      }
    }
    return null;
  }

  /**
   * Gets all active execution phases in order.
   *
   * @return array of active phases in execution order
   */
  public static ExecutionPhase[] getActivePhases() {
    return new ExecutionPhase[] {
      STARTING, PARAMETER_VALIDATION, EXECUTING, HOST_CALL, RESULT_PROCESSING
    };
  }

  /**
   * Gets all terminal phases.
   *
   * @return array of terminal phases
   */
  public static ExecutionPhase[] getTerminalPhases() {
    return new ExecutionPhase[] {COMPLETED, FAILED, CANCELLED, TIMEOUT};
  }

  /**
   * Gets all error phases.
   *
   * @return array of error phases
   */
  public static ExecutionPhase[] getErrorPhases() {
    return new ExecutionPhase[] {FAILED, CANCELLED, TIMEOUT};
  }
}
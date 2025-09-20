package ai.tegmentum.wasmtime4j.async.reactive;

/**
 * Enumeration of WebAssembly compilation phases.
 *
 * <p>This enum represents the various stages that a WebAssembly module goes through during
 * compilation, from initial parsing through final optimization and completion.
 *
 * <p>These phases are used in reactive compilation events to provide detailed progress information
 * and enable fine-grained monitoring of the compilation process.
 *
 * @since 1.0.0
 */
public enum CompilationPhase {

  /** Initial parsing of the WebAssembly bytecode structure. */
  PARSING("Parsing", "Parsing WebAssembly bytecode structure", 0),

  /** Validation of the parsed module for correctness. */
  VALIDATION("Validation", "Validating module structure and types", 1),

  /** Code generation and initial compilation. */
  COMPILATION("Compilation", "Generating native code from WebAssembly", 2),

  /** Optimization passes for performance improvement. */
  OPTIMIZATION("Optimization", "Applying optimization passes", 3),

  /** Linking and finalization of the compiled module. */
  LINKING("Linking", "Linking imports and finalizing module", 4),

  /** Final preparation and cleanup. */
  FINALIZATION("Finalization", "Finalizing compilation and cleanup", 5),

  /** Compilation completed successfully. */
  COMPLETED("Completed", "Compilation completed successfully", 6),

  /** Compilation failed with errors. */
  FAILED("Failed", "Compilation failed with errors", -1),

  /** Compilation was cancelled before completion. */
  CANCELLED("Cancelled", "Compilation was cancelled", -2);

  private final String displayName;
  private final String description;
  private final int order;

  CompilationPhase(final String displayName, final String description, final int order) {
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
   * Gets the order of this phase in the compilation process.
   *
   * <p>Normal phases have positive order values, with higher values indicating later phases.
   * Terminal phases (FAILED, CANCELLED) have negative order values.
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
   * Checks if this phase represents a terminal state.
   *
   * <p>Terminal phases are those where compilation cannot proceed further, including completion,
   * failure, and cancellation.
   *
   * @return true if this is a terminal phase
   */
  public boolean isTerminal() {
    return order < 0 || this == COMPLETED;
  }

  /**
   * Checks if this phase represents an active compilation state.
   *
   * <p>Active phases are those where compilation is actively proceeding.
   *
   * @return true if this is an active compilation phase
   */
  public boolean isActive() {
    return order >= 0 && this != COMPLETED;
  }

  /**
   * Gets the estimated progress percentage for this phase.
   *
   * <p>This provides a rough estimate of overall compilation progress based on the current phase,
   * useful for progress bars and status displays.
   *
   * @return estimated progress percentage (0.0 to 100.0)
   */
  public double getEstimatedProgress() {
    switch (this) {
      case PARSING:
        return 10.0;
      case VALIDATION:
        return 20.0;
      case COMPILATION:
        return 60.0;
      case OPTIMIZATION:
        return 85.0;
      case LINKING:
        return 95.0;
      case FINALIZATION:
        return 98.0;
      case COMPLETED:
        return 100.0;
      case FAILED:
      case CANCELLED:
        return 0.0;
      default:
        return 0.0;
    }
  }

  /**
   * Gets the next phase in the compilation process.
   *
   * @return the next phase, or null if this is a terminal phase
   */
  public CompilationPhase getNextPhase() {
    switch (this) {
      case PARSING:
        return VALIDATION;
      case VALIDATION:
        return COMPILATION;
      case COMPILATION:
        return OPTIMIZATION;
      case OPTIMIZATION:
        return LINKING;
      case LINKING:
        return FINALIZATION;
      case FINALIZATION:
        return COMPLETED;
      default:
        return null; // Terminal phases have no next phase
    }
  }

  /**
   * Gets the previous phase in the compilation process.
   *
   * @return the previous phase, or null if this is the first phase
   */
  public CompilationPhase getPreviousPhase() {
    switch (this) {
      case VALIDATION:
        return PARSING;
      case COMPILATION:
        return VALIDATION;
      case OPTIMIZATION:
        return COMPILATION;
      case LINKING:
        return OPTIMIZATION;
      case FINALIZATION:
        return LINKING;
      case COMPLETED:
        return FINALIZATION;
      default:
        return null; // First phase or terminal phases have no previous phase
    }
  }

  /**
   * Creates a CompilationPhase from its order value.
   *
   * @param order the phase order
   * @return the corresponding phase, or null if order is invalid
   */
  public static CompilationPhase fromOrder(final int order) {
    for (final CompilationPhase phase : values()) {
      if (phase.order == order) {
        return phase;
      }
    }
    return null;
  }

  /**
   * Gets all active compilation phases in order.
   *
   * @return array of active phases in compilation order
   */
  public static CompilationPhase[] getActivePhases() {
    return new CompilationPhase[] {
      PARSING, VALIDATION, COMPILATION, OPTIMIZATION, LINKING, FINALIZATION
    };
  }

  /**
   * Gets all terminal phases.
   *
   * @return array of terminal phases
   */
  public static CompilationPhase[] getTerminalPhases() {
    return new CompilationPhase[] {COMPLETED, FAILED, CANCELLED};
  }
}

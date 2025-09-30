package ai.tegmentum.wasmtime4j.debug;

/**
 * Types of stepping operations available during debugging.
 *
 * <p>Defines the different ways execution can be stepped through
 * during debugging sessions.
 *
 * @since 1.0.0
 */
public enum StepType {
  /** Step to the next instruction in the current function */
  STEP_INTO,

  /** Step over function calls, stopping at the next instruction in current function */
  STEP_OVER,

  /** Step out of the current function to the calling function */
  STEP_OUT,

  /** Continue execution until next breakpoint or completion */
  CONTINUE,

  /** Step to a specific instruction address */
  STEP_TO_ADDRESS,

  /** Step until a specific condition is met */
  STEP_UNTIL_CONDITION,

  /** Step through a specific number of instructions */
  STEP_COUNT,

  /** Step until function entry */
  STEP_TO_FUNCTION_ENTRY,

  /** Step until function exit */
  STEP_TO_FUNCTION_EXIT,

  /** Step until memory access */
  STEP_TO_MEMORY_ACCESS,

  /** Step backward (reverse debugging) */
  STEP_BACKWARD,

  /** Run until exception or trap */
  RUN_TO_EXCEPTION;

  /**
   * Checks if this step type involves continuing execution.
   *
   * @return true if execution continues
   */
  public boolean isContinuousExecution() {
    return this == CONTINUE || this == STEP_TO_ADDRESS ||
           this == STEP_UNTIL_CONDITION || this == RUN_TO_EXCEPTION;
  }

  /**
   * Checks if this step type is function-oriented.
   *
   * @return true if function-oriented
   */
  public boolean isFunctionOriented() {
    return this == STEP_INTO || this == STEP_OVER || this == STEP_OUT ||
           this == STEP_TO_FUNCTION_ENTRY || this == STEP_TO_FUNCTION_EXIT;
  }

  /**
   * Checks if this step type supports reverse execution.
   *
   * @return true if reverse execution is supported
   */
  public boolean supportsReverse() {
    return this == STEP_BACKWARD;
  }

  /**
   * Checks if this step type requires additional parameters.
   *
   * @return true if parameters are required
   */
  public boolean requiresParameters() {
    return this == STEP_TO_ADDRESS || this == STEP_UNTIL_CONDITION || this == STEP_COUNT;
  }

  /**
   * Gets a human-readable description of this step type.
   *
   * @return description string
   */
  public String getDescription() {
    return switch (this) {
      case STEP_INTO -> "Step into next instruction";
      case STEP_OVER -> "Step over function calls";
      case STEP_OUT -> "Step out of current function";
      case CONTINUE -> "Continue execution";
      case STEP_TO_ADDRESS -> "Step to specific address";
      case STEP_UNTIL_CONDITION -> "Step until condition met";
      case STEP_COUNT -> "Step specific number of instructions";
      case STEP_TO_FUNCTION_ENTRY -> "Step to function entry";
      case STEP_TO_FUNCTION_EXIT -> "Step to function exit";
      case STEP_TO_MEMORY_ACCESS -> "Step to memory access";
      case STEP_BACKWARD -> "Step backward";
      case RUN_TO_EXCEPTION -> "Run until exception";
    };
  }
}
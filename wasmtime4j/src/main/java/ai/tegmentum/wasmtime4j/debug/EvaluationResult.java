package ai.tegmentum.wasmtime4j.debug;

/**
 * Evaluation result interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface EvaluationResult {

  /**
   * Gets the evaluation status.
   *
   * @return evaluation status
   */
  EvaluationStatus getStatus();

  /**
   * Gets the result value.
   *
   * @return result value or null
   */
  VariableValue getValue();

  /**
   * Gets the error message if evaluation failed.
   *
   * @return error message or null
   */
  String getErrorMessage();

  /**
   * Gets the evaluation duration in milliseconds.
   *
   * @return duration
   */
  long getDuration();

  /**
   * Gets the expression that was evaluated.
   *
   * @return expression
   */
  String getExpression();

  /** Evaluation status enumeration. */
  enum EvaluationStatus {
    /** Evaluation succeeded. */
    SUCCESS,
    /** Evaluation failed. */
    FAILED,
    /** Evaluation timed out. */
    TIMEOUT,
    /** Invalid expression. */
    INVALID_EXPRESSION
  }
}

package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of evaluating a WebAssembly expression during debugging.
 *
 * <p>Contains the result value, type information, and any errors that occurred
 * during evaluation.
 *
 * @since 1.0.0
 */
public final class EvaluationResult {

    private final boolean success;
    private final VariableValue value;
    private final String type;
    private final String expression;
    private final String error;
    private final long evaluationTimeMs;

    /**
     * Creates an evaluation result.
     *
     * @param success whether evaluation succeeded
     * @param value the result value (null if failed)
     * @param type the result type (null if failed)
     * @param expression the original expression
     * @param error error message (null if successful)
     * @param evaluationTimeMs time taken to evaluate in milliseconds
     */
    public EvaluationResult(final boolean success, final VariableValue value,
                           final String type, final String expression,
                           final String error, final long evaluationTimeMs) {
        this.success = success;
        this.value = value;
        this.type = type;
        this.expression = Objects.requireNonNull(expression, "expression cannot be null");
        this.error = error;
        this.evaluationTimeMs = evaluationTimeMs;
    }

    /**
     * Creates a successful evaluation result.
     *
     * @param value the result value
     * @param type the result type
     * @param expression the original expression
     * @param evaluationTimeMs evaluation time in milliseconds
     * @return successful result
     */
    public static EvaluationResult success(final VariableValue value, final String type,
                                          final String expression, final long evaluationTimeMs) {
        return new EvaluationResult(true, value, type, expression, null, evaluationTimeMs);
    }

    /**
     * Creates a failed evaluation result.
     *
     * @param expression the original expression
     * @param error the error message
     * @param evaluationTimeMs evaluation time in milliseconds
     * @return failed result
     */
    public static EvaluationResult failure(final String expression, final String error,
                                          final long evaluationTimeMs) {
        return new EvaluationResult(false, null, null, expression, error, evaluationTimeMs);
    }

    /**
     * Creates a failed evaluation result with no timing information.
     *
     * @param expression the original expression
     * @param error the error message
     * @return failed result
     */
    public static EvaluationResult failure(final String expression, final String error) {
        return failure(expression, error, 0L);
    }

    /**
     * Checks if evaluation was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Gets the result value.
     *
     * @return the value if successful, empty otherwise
     */
    public Optional<VariableValue> getValue() {
        return Optional.ofNullable(value);
    }

    /**
     * Gets the result type.
     *
     * @return the type if successful, empty otherwise
     */
    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    /**
     * Gets the original expression.
     *
     * @return the expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Gets the error message.
     *
     * @return the error message if failed, empty otherwise
     */
    public Optional<String> getError() {
        return Optional.ofNullable(error);
    }

    /**
     * Gets the evaluation time in milliseconds.
     *
     * @return evaluation time
     */
    public long getEvaluationTimeMs() {
        return evaluationTimeMs;
    }

    /**
     * Gets the result value or throws if failed.
     *
     * @return the result value
     * @throws IllegalStateException if evaluation failed
     */
    public VariableValue getValueOrThrow() {
        if (!success) {
            throw new IllegalStateException("Evaluation failed: " + error);
        }
        return value;
    }

    /**
     * Gets the result type or throws if failed.
     *
     * @return the result type
     * @throws IllegalStateException if evaluation failed
     */
    public String getTypeOrThrow() {
        if (!success) {
            throw new IllegalStateException("Evaluation failed: " + error);
        }
        return type;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EvaluationResult other = (EvaluationResult) obj;
        return success == other.success &&
                evaluationTimeMs == other.evaluationTimeMs &&
                Objects.equals(value, other.value) &&
                Objects.equals(type, other.type) &&
                Objects.equals(expression, other.expression) &&
                Objects.equals(error, other.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, value, type, expression, error, evaluationTimeMs);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EvaluationResult{");
        sb.append("expression='").append(expression).append('\'');
        sb.append(", success=").append(success);
        if (success) {
            sb.append(", value=").append(value);
            sb.append(", type='").append(type).append('\'');
        } else {
            sb.append(", error='").append(error).append('\'');
        }
        sb.append(", timeMs=").append(evaluationTimeMs);
        sb.append('}');
        return sb.toString();
    }
}
package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Objects;

/**
 * Represents the result of watch expression evaluation.
 */
public final class WatchEvaluationResult {
    private final boolean successful;
    private final WasmValue value;
    private final String error;
    private final String typeName;

    private WatchEvaluationResult(final boolean successful, final WasmValue value,
                                 final String error, final String typeName) {
        this.successful = successful;
        this.value = value;
        this.error = error;
        this.typeName = typeName;
    }

    public static WatchEvaluationResult successful(final WasmValue value, final String typeName) {
        return new WatchEvaluationResult(true, value, null, typeName);
    }

    public static WatchEvaluationResult failed(final String error) {
        return new WatchEvaluationResult(false, null, error, null);
    }

    // Getters
    public boolean isSuccessful() { return successful; }
    public WasmValue getValue() { return value; }
    public String getError() { return error; }
    public String getTypeName() { return typeName; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof WatchEvaluationResult)) return false;
        final WatchEvaluationResult that = (WatchEvaluationResult) o;
        return successful == that.successful &&
               Objects.equals(value, that.value) &&
               Objects.equals(error, that.error) &&
               Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(successful, value, error, typeName);
    }

    @Override
    public String toString() {
        if (successful) {
            return String.format("WatchEvaluationResult{value=%s, type='%s'}", value, typeName);
        } else {
            return String.format("WatchEvaluationResult{error='%s'}", error);
        }
    }
}
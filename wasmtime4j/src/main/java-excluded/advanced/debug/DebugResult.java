package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents the result of a debug execution.
 */
public final class DebugResult {
    private final boolean successful;
    private final WasmValue[] results;
    private final String error;
    private final Duration executionTime;
    private final long instructionCount;

    private DebugResult(final boolean successful, final WasmValue[] results,
                       final String error, final Duration executionTime,
                       final long instructionCount) {
        this.successful = successful;
        this.results = results;
        this.error = error;
        this.executionTime = executionTime;
        this.instructionCount = instructionCount;
    }

    public static DebugResult successful(final WasmValue[] results, final Duration executionTime,
                                        final long instructionCount) {
        return new DebugResult(true, results, null, executionTime, instructionCount);
    }

    public static DebugResult failed(final String error) {
        return new DebugResult(false, null, error, Duration.ZERO, 0);
    }

    // Getters
    public boolean isSuccessful() { return successful; }
    public WasmValue[] getResults() { return results != null ? results.clone() : null; }
    public String getError() { return error; }
    public Duration getExecutionTime() { return executionTime; }
    public long getInstructionCount() { return instructionCount; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof DebugResult)) return false;
        final DebugResult that = (DebugResult) o;
        return successful == that.successful &&
               instructionCount == that.instructionCount &&
               Arrays.equals(results, that.results) &&
               Objects.equals(error, that.error) &&
               Objects.equals(executionTime, that.executionTime);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(successful, error, executionTime, instructionCount);
        result = 31 * result + Arrays.hashCode(results);
        return result;
    }

    @Override
    public String toString() {
        if (successful) {
            return String.format("DebugResult{successful=true, results=%s, time=%s, instructions=%d}",
                               Arrays.toString(results), executionTime, instructionCount);
        } else {
            return String.format("DebugResult{successful=false, error='%s'}", error);
        }
    }
}
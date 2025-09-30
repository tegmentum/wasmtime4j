package ai.tegmentum.wasmtime4j.debug;

import java.time.Duration;
import java.util.Objects;

/**
 * Profiling information for a WebAssembly function during execution tracing.
 */
public final class FunctionProfile {
    private final String functionName;
    private final long callCount;
    private final Duration totalTime;
    private final Duration minTime;
    private final Duration maxTime;
    private final long instructionCount;

    public FunctionProfile(final String functionName, final long callCount,
                          final Duration totalTime, final Duration minTime,
                          final Duration maxTime, final long instructionCount) {
        this.functionName = Objects.requireNonNull(functionName);
        this.callCount = callCount;
        this.totalTime = Objects.requireNonNull(totalTime);
        this.minTime = Objects.requireNonNull(minTime);
        this.maxTime = Objects.requireNonNull(maxTime);
        this.instructionCount = instructionCount;
    }

    // Getters
    public String getFunctionName() { return functionName; }
    public long getCallCount() { return callCount; }
    public Duration getTotalTime() { return totalTime; }
    public Duration getMinTime() { return minTime; }
    public Duration getMaxTime() { return maxTime; }
    public long getInstructionCount() { return instructionCount; }

    /**
     * Calculates average execution time per call.
     */
    public Duration getAverageTime() {
        if (callCount == 0) {
            return Duration.ZERO;
        }
        return Duration.ofNanos(totalTime.toNanos() / callCount);
    }

    /**
     * Calculates average instructions per call.
     */
    public double getAverageInstructions() {
        if (callCount == 0) {
            return 0.0;
        }
        return (double) instructionCount / callCount;
    }

    /**
     * Calculates instructions per nanosecond execution rate.
     */
    public double getInstructionRate() {
        if (totalTime.isZero()) {
            return 0.0;
        }
        return (double) instructionCount / totalTime.toNanos();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FunctionProfile)) return false;
        final FunctionProfile that = (FunctionProfile) o;
        return Objects.equals(functionName, that.functionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionName);
    }

    @Override
    public String toString() {
        return String.format("FunctionProfile{name='%s', calls=%d, total=%s, avg=%s, min=%s, max=%s, instructions=%d}",
                           functionName, callCount, totalTime, getAverageTime(),
                           minTime, maxTime, instructionCount);
    }
}
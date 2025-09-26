package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the current execution state during WebAssembly debugging.
 *
 * <p>Contains information about the current execution position, state,
 * and any active operations.
 *
 * @since 1.0.0
 */
public final class ExecutionState {

    /** Execution states */
    public enum State {
        /** Not started */
        NOT_STARTED,
        /** Currently running */
        RUNNING,
        /** Paused at breakpoint */
        PAUSED_BREAKPOINT,
        /** Paused by step operation */
        PAUSED_STEP,
        /** Paused by user request */
        PAUSED_MANUAL,
        /** Paused due to exception */
        PAUSED_EXCEPTION,
        /** Execution completed normally */
        COMPLETED,
        /** Execution terminated with error */
        ERROR,
        /** Execution was aborted */
        ABORTED
    }

    private final State state;
    private final String functionName;
    private final long instructionOffset;
    private final int line;
    private final int column;
    private final String sourceFile;
    private final String reason;
    private final long timestamp;
    private final boolean canContinue;
    private final boolean canStep;

    /**
     * Creates execution state.
     *
     * @param state current state
     * @param functionName current function name
     * @param instructionOffset current instruction offset
     * @param line current source line (0 if unknown)
     * @param column current source column (0 if unknown)
     * @param sourceFile current source file (null if unknown)
     * @param reason reason for current state (null if none)
     * @param timestamp state timestamp
     * @param canContinue whether execution can continue
     * @param canStep whether stepping is possible
     */
    public ExecutionState(final State state, final String functionName,
                         final long instructionOffset, final int line, final int column,
                         final String sourceFile, final String reason, final long timestamp,
                         final boolean canContinue, final boolean canStep) {
        this.state = Objects.requireNonNull(state, "state cannot be null");
        this.functionName = functionName;
        this.instructionOffset = instructionOffset;
        this.line = line;
        this.column = column;
        this.sourceFile = sourceFile;
        this.reason = reason;
        this.timestamp = timestamp;
        this.canContinue = canContinue;
        this.canStep = canStep;
    }

    /**
     * Creates a running state.
     *
     * @param functionName current function
     * @param instructionOffset current offset
     * @return running execution state
     */
    public static ExecutionState running(final String functionName, final long instructionOffset) {
        return new ExecutionState(State.RUNNING, functionName, instructionOffset, 0, 0,
                null, null, System.currentTimeMillis(), true, false);
    }

    /**
     * Creates a paused state at breakpoint.
     *
     * @param functionName current function
     * @param instructionOffset current offset
     * @param reason breakpoint reason
     * @return paused execution state
     */
    public static ExecutionState pausedAtBreakpoint(final String functionName,
                                                   final long instructionOffset,
                                                   final String reason) {
        return new ExecutionState(State.PAUSED_BREAKPOINT, functionName, instructionOffset,
                0, 0, null, reason, System.currentTimeMillis(), true, true);
    }

    /**
     * Creates a paused state after step.
     *
     * @param functionName current function
     * @param instructionOffset current offset
     * @return paused execution state
     */
    public static ExecutionState pausedAfterStep(final String functionName,
                                                final long instructionOffset) {
        return new ExecutionState(State.PAUSED_STEP, functionName, instructionOffset,
                0, 0, null, "Step completed", System.currentTimeMillis(), true, true);
    }

    /**
     * Creates a completed state.
     *
     * @return completed execution state
     */
    public static ExecutionState completed() {
        return new ExecutionState(State.COMPLETED, null, -1, 0, 0, null,
                "Execution completed", System.currentTimeMillis(), false, false);
    }

    /**
     * Creates an error state.
     *
     * @param reason error reason
     * @return error execution state
     */
    public static ExecutionState error(final String reason) {
        return new ExecutionState(State.ERROR, null, -1, 0, 0, null,
                reason, System.currentTimeMillis(), false, false);
    }

    /**
     * Gets the current execution state.
     *
     * @return execution state
     */
    public State getState() {
        return state;
    }

    /**
     * Gets the current function name.
     *
     * @return function name or empty if unknown
     */
    public Optional<String> getFunctionName() {
        return Optional.ofNullable(functionName);
    }

    /**
     * Gets the current instruction offset.
     *
     * @return instruction offset, or -1 if unknown
     */
    public long getInstructionOffset() {
        return instructionOffset;
    }

    /**
     * Gets the current source line.
     *
     * @return line number, or 0 if unknown
     */
    public int getLine() {
        return line;
    }

    /**
     * Gets the current source column.
     *
     * @return column number, or 0 if unknown
     */
    public int getColumn() {
        return column;
    }

    /**
     * Gets the current source file.
     *
     * @return source file or empty if unknown
     */
    public Optional<String> getSourceFile() {
        return Optional.ofNullable(sourceFile);
    }

    /**
     * Gets the reason for the current state.
     *
     * @return reason or empty if none
     */
    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }

    /**
     * Gets the state timestamp.
     *
     * @return timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Checks if execution can continue.
     *
     * @return true if can continue
     */
    public boolean canContinue() {
        return canContinue;
    }

    /**
     * Checks if stepping is possible.
     *
     * @return true if can step
     */
    public boolean canStep() {
        return canStep;
    }

    /**
     * Checks if execution is currently running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return state == State.RUNNING;
    }

    /**
     * Checks if execution is currently paused.
     *
     * @return true if paused
     */
    public boolean isPaused() {
        return state == State.PAUSED_BREAKPOINT ||
               state == State.PAUSED_STEP ||
               state == State.PAUSED_MANUAL ||
               state == State.PAUSED_EXCEPTION;
    }

    /**
     * Checks if execution is complete.
     *
     * @return true if complete
     */
    public boolean isComplete() {
        return state == State.COMPLETED ||
               state == State.ERROR ||
               state == State.ABORTED;
    }

    /**
     * Creates a copy with updated source location.
     *
     * @param line source line
     * @param column source column
     * @param sourceFile source file
     * @return updated execution state
     */
    public ExecutionState withSourceLocation(final int line, final int column, final String sourceFile) {
        return new ExecutionState(state, functionName, instructionOffset, line, column,
                sourceFile, reason, timestamp, canContinue, canStep);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ExecutionState other = (ExecutionState) obj;
        return instructionOffset == other.instructionOffset &&
                line == other.line &&
                column == other.column &&
                timestamp == other.timestamp &&
                canContinue == other.canContinue &&
                canStep == other.canStep &&
                state == other.state &&
                Objects.equals(functionName, other.functionName) &&
                Objects.equals(sourceFile, other.sourceFile) &&
                Objects.equals(reason, other.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, functionName, instructionOffset, line, column,
                sourceFile, reason, timestamp, canContinue, canStep);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExecutionState{");
        sb.append("state=").append(state);
        if (functionName != null) {
            sb.append(", function='").append(functionName).append('\'');
        }
        if (instructionOffset >= 0) {
            sb.append(", offset=").append(instructionOffset);
        }
        if (sourceFile != null) {
            sb.append(", location=").append(sourceFile).append(':').append(line);
            if (column > 0) {
                sb.append(':').append(column);
            }
        }
        if (reason != null) {
            sb.append(", reason='").append(reason).append('\'');
        }
        sb.append(", canContinue=").append(canContinue);
        sb.append(", canStep=").append(canStep);
        sb.append(", timestamp=").append(timestamp);
        sb.append('}');
        return sb.toString();
    }
}
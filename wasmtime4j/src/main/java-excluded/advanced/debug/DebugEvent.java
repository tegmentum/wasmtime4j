package ai.tegmentum.wasmtime4j.debug;

import java.time.Instant;
import java.util.Objects;

/**
 * Base class for all debug events in WebAssembly debugging.
 * Events are fired asynchronously during debugging sessions.
 */
public abstract class DebugEvent {
    private final Instant timestamp;

    protected DebugEvent() {
        this.timestamp = Instant.now();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public abstract DebugEventType getType();

    /**
     * Debug event types.
     */
    public enum DebugEventType {
        BREAKPOINT_SET,
        BREAKPOINT_REMOVED,
        BREAKPOINT_CHANGED,
        BREAKPOINT_HIT,
        WATCH_EXPRESSION_ADDED,
        WATCH_EXPRESSION_REMOVED,
        EXECUTION_STARTED,
        EXECUTION_FINISHED,
        EXECUTION_PAUSED,
        EXECUTION_CONTINUED,
        EXECUTION_STOPPED,
        STEP_COMPLETED,
        ERROR
    }

    /**
     * Breakpoint set event.
     */
    public static final class BreakpointSetEvent extends DebugEvent {
        private final Breakpoint breakpoint;

        public BreakpointSetEvent(final Breakpoint breakpoint) {
            this.breakpoint = Objects.requireNonNull(breakpoint);
        }

        @Override
        public DebugEventType getType() {
            return DebugEventType.BREAKPOINT_SET;
        }

        public Breakpoint getBreakpoint() {
            return breakpoint;
        }

        @Override
        public String toString() {
            return "BreakpointSetEvent{breakpoint=" + breakpoint + ", timestamp=" + getTimestamp() + '}';
        }
    }

    /**
     * Breakpoint removed event.
     */
    public static final class BreakpointRemovedEvent extends DebugEvent {
        private final Breakpoint breakpoint;

        public BreakpointRemovedEvent(final Breakpoint breakpoint) {
            this.breakpoint = Objects.requireNonNull(breakpoint);
        }

        @Override
        public DebugEventType getType() {
            return DebugEventType.BREAKPOINT_REMOVED;
        }

        public Breakpoint getBreakpoint() {
            return breakpoint;
        }

        @Override
        public String toString() {
            return "BreakpointRemovedEvent{breakpoint=" + breakpoint + ", timestamp=" + getTimestamp() + '}';
        }
    }

    /**
     * Breakpoint changed event.
     */
    public static final class BreakpointChangedEvent extends DebugEvent {
        private final Breakpoint breakpoint;

        public BreakpointChangedEvent(final Breakpoint breakpoint) {
            this.breakpoint = Objects.requireNonNull(breakpoint);
        }

        @Override
        public DebugEventType getType() {
            return DebugEventType.BREAKPOINT_CHANGED;
        }

        public Breakpoint getBreakpoint() {
            return breakpoint;
        }

        @Override
        public String toString() {
            return "BreakpointChangedEvent{breakpoint=" + breakpoint + ", timestamp=" + getTimestamp() + '}';
        }
    }

    /**
     * Breakpoint hit event.
     */
    public static final class BreakpointHitEvent extends DebugEvent {
        private final Breakpoint breakpoint;
        private final ExecutionContext context;

        public BreakpointHitEvent(final Breakpoint breakpoint, final ExecutionContext context) {
            this.breakpoint = Objects.requireNonNull(breakpoint);
            this.context = Objects.requireNonNull(context);
        }

        @Override
        public DebugEventType getType() {
            return DebugEventType.BREAKPOINT_HIT;
        }

        public Breakpoint getBreakpoint() {
            return breakpoint;
        }

        public ExecutionContext getContext() {
            return context;
        }

        @Override
        public String toString() {
            return "BreakpointHitEvent{breakpoint=" + breakpoint +
                   ", context=" + context + ", timestamp=" + getTimestamp() + '}';
        }
    }

    /**
     * Watch expression added event.
     */
    public static final class WatchExpressionAddedEvent extends DebugEvent {
        private final WatchExpression watchExpression;

        public WatchExpressionAddedEvent(final WatchExpression watchExpression) {
            this.watchExpression = Objects.requireNonNull(watchExpression);
        }

        @Override
        public DebugEventType getType() {
            return DebugEventType.WATCH_EXPRESSION_ADDED;
        }

        public WatchExpression getWatchExpression() {
            return watchExpression;
        }

        @Override
        public String toString() {
            return "WatchExpressionAddedEvent{watch=" + watchExpression +
                   ", timestamp=" + getTimestamp() + '}';
        }
    }

    /**
     * Watch expression removed event.
     */
    public static final class WatchExpressionRemovedEvent extends DebugEvent {
        private final WatchExpression watchExpression;

        public WatchExpressionRemovedEvent(final WatchExpression watchExpression) {
            this.watchExpression = Objects.requireNonNull(watchExpression);
        }

        @Override
        public DebugEventType getType() {
            return DebugEventType.WATCH_EXPRESSION_REMOVED;
        }

        public WatchExpression getWatchExpression() {
            return watchExpression;
        }

        @Override
        public String toString() {
            return "WatchExpressionRemovedEvent{watch=" + watchExpression +
                   ", timestamp=" + getTimestamp() + '}';
        }
    }

    /**
     * Execution started event.
     */
    public static final class ExecutionStartedEvent extends DebugEvent {
        private final String functionName;

        public ExecutionStartedEvent(final String functionName) {
            this.functionName = Objects.requireNonNull(functionName);
        }

        @Override
        public DebugEventType getType() {
            return DebugEventType.EXECUTION_STARTED;
        }

        public String getFunctionName() {
            return functionName;
        }

        @Override
        public String toString() {
            return "ExecutionStartedEvent{function='" + functionName +
                   "', timestamp=" + getTimestamp() + '}';
        }
    }

    /**
     * Execution finished event.
     */
    public static final class ExecutionFinishedEvent extends DebugEvent {
        private final DebugResult result;

        public ExecutionFinishedEvent(final DebugResult result) {
            this.result = Objects.requireNonNull(result);
        }

        @Override
        public DebugEventType getType() {
            return DebugEventType.EXECUTION_FINISHED;
        }

        public DebugResult getResult() {
            return result;
        }

        @Override
        public String toString() {
            return "ExecutionFinishedEvent{result=" + result +
                   ", timestamp=" + getTimestamp() + '}';
        }
    }

    /**
     * Execution paused event.
     */
    public static final class ExecutionPausedEvent extends DebugEvent {
        private final ExecutionContext context;

        public ExecutionPausedEvent(final ExecutionContext context) {
            this.context = Objects.requireNonNull(context);
        }

        @Override
        public DebugEventType getType() {
            return DebugEventType.EXECUTION_PAUSED;
        }

        public ExecutionContext getContext() {
            return context;
        }

        @Override
        public String toString() {
            return "ExecutionPausedEvent{context=" + context +
                   ", timestamp=" + getTimestamp() + '}';
        }
    }

    /**
     * Execution continued event.
     */
    public static final class ExecutionContinuedEvent extends DebugEvent {
        @Override
        public DebugEventType getType() {
            return DebugEventType.EXECUTION_CONTINUED;
        }

        @Override
        public String toString() {
            return "ExecutionContinuedEvent{timestamp=" + getTimestamp() + '}';
        }
    }

    /**
     * Execution stopped event.
     */
    public static final class ExecutionStoppedEvent extends DebugEvent {
        @Override
        public DebugEventType getType() {
            return DebugEventType.EXECUTION_STOPPED;
        }

        @Override
        public String toString() {
            return "ExecutionStoppedEvent{timestamp=" + getTimestamp() + '}';
        }
    }

    /**
     * Step completed event.
     */
    public static final class StepCompletedEvent extends DebugEvent {
        private final ExecutionContext context;

        public StepCompletedEvent(final ExecutionContext context) {
            this.context = Objects.requireNonNull(context);
        }

        @Override
        public DebugEventType getType() {
            return DebugEventType.STEP_COMPLETED;
        }

        public ExecutionContext getContext() {
            return context;
        }

        @Override
        public String toString() {
            return "StepCompletedEvent{context=" + context +
                   ", timestamp=" + getTimestamp() + '}';
        }
    }

    /**
     * Error event.
     */
    public static final class ErrorEvent extends DebugEvent {
        private final String message;
        private final Throwable cause;

        public ErrorEvent(final String message, final Throwable cause) {
            this.message = Objects.requireNonNull(message);
            this.cause = cause;
        }

        @Override
        public DebugEventType getType() {
            return DebugEventType.ERROR;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getCause() {
            return cause;
        }

        @Override
        public String toString() {
            return "ErrorEvent{message='" + message +
                   "', cause=" + cause + ", timestamp=" + getTimestamp() + '}';
        }
    }
}
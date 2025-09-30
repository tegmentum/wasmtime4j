package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Base class for all execution trace events.
 */
public abstract class TraceEvent {
    private final Instant timestamp;

    protected TraceEvent(final Instant timestamp) {
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }

    protected TraceEvent() {
        this.timestamp = Instant.now();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public abstract ExecutionTracer.TraceEventType getType();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{timestamp=" + timestamp + '}';
    }

    /**
     * Trace started event.
     */
    public static final class TraceStartedEvent extends TraceEvent {
        @Override
        public ExecutionTracer.TraceEventType getType() {
            return ExecutionTracer.TraceEventType.TRACE_STARTED;
        }
    }

    /**
     * Trace stopped event.
     */
    public static final class TraceStoppedEvent extends TraceEvent {
        @Override
        public ExecutionTracer.TraceEventType getType() {
            return ExecutionTracer.TraceEventType.TRACE_STOPPED;
        }
    }

    /**
     * Function call event.
     */
    public static final class FunctionCallEvent extends TraceEvent {
        private final String functionName;
        private final List<WasmValue> arguments;
        private final Duration executionTime;
        private final long instructionCount;

        public FunctionCallEvent(final String functionName, final List<WasmValue> arguments,
                               final Instant timestamp) {
            super(timestamp);
            this.functionName = Objects.requireNonNull(functionName);
            this.arguments = List.copyOf(Objects.requireNonNull(arguments));
            this.executionTime = Duration.ZERO; // Will be updated when function returns
            this.instructionCount = 0; // Will be updated when function returns
        }

        @Override
        public ExecutionTracer.TraceEventType getType() {
            return ExecutionTracer.TraceEventType.FUNCTION_CALL;
        }

        public String getFunctionName() { return functionName; }
        public List<WasmValue> getArguments() { return arguments; }
        public Duration getExecutionTime() { return executionTime; }
        public long getInstructionCount() { return instructionCount; }

        @Override
        public String toString() {
            return String.format("FunctionCallEvent{function='%s', args=%d, time=%s, instructions=%d, timestamp=%s}",
                               functionName, arguments.size(), executionTime, instructionCount, getTimestamp());
        }
    }

    /**
     * Instruction executed event.
     */
    public static final class InstructionExecutedEvent extends TraceEvent {
        private final ExecutionContext context;

        public InstructionExecutedEvent(final ExecutionContext context, final Instant timestamp) {
            super(timestamp);
            this.context = Objects.requireNonNull(context);
        }

        @Override
        public ExecutionTracer.TraceEventType getType() {
            return ExecutionTracer.TraceEventType.INSTRUCTION_EXECUTED;
        }

        public ExecutionContext getContext() { return context; }

        @Override
        public String toString() {
            return String.format("InstructionExecutedEvent{context=%s, timestamp=%s}",
                               context, getTimestamp());
        }
    }

    /**
     * Breakpoint hit trace event.
     */
    public static final class BreakpointHitTraceEvent extends TraceEvent {
        private final Breakpoint breakpoint;
        private final ExecutionContext context;

        public BreakpointHitTraceEvent(final Breakpoint breakpoint, final ExecutionContext context,
                                     final Instant timestamp) {
            super(timestamp);
            this.breakpoint = Objects.requireNonNull(breakpoint);
            this.context = Objects.requireNonNull(context);
        }

        @Override
        public ExecutionTracer.TraceEventType getType() {
            return ExecutionTracer.TraceEventType.BREAKPOINT_HIT;
        }

        public Breakpoint getBreakpoint() { return breakpoint; }
        public ExecutionContext getContext() { return context; }

        @Override
        public String toString() {
            return String.format("BreakpointHitTraceEvent{breakpoint=%s, context=%s, timestamp=%s}",
                               breakpoint, context, getTimestamp());
        }
    }

    /**
     * Debug event trace event wrapper.
     */
    public static final class DebugEventTraceEvent extends TraceEvent {
        private final DebugEvent debugEvent;

        public DebugEventTraceEvent(final DebugEvent debugEvent, final Instant timestamp) {
            super(timestamp);
            this.debugEvent = Objects.requireNonNull(debugEvent);
        }

        @Override
        public ExecutionTracer.TraceEventType getType() {
            return ExecutionTracer.TraceEventType.DEBUG_EVENT;
        }

        public DebugEvent getDebugEvent() { return debugEvent; }

        @Override
        public String toString() {
            return String.format("DebugEventTraceEvent{debugEvent=%s, timestamp=%s}",
                               debugEvent, getTimestamp());
        }
    }
}
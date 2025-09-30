package ai.tegmentum.wasmtime4j.debug;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Statistics collected during execution tracing.
 */
public final class TraceStatistics {
    private final long totalEvents;
    private final long totalInstructions;
    private final Duration totalExecutionTime;
    private final Map<ExecutionTracer.TraceEventType, Long> eventCounts;
    private final int profiledFunctions;

    public TraceStatistics(final long totalEvents, final long totalInstructions,
                         final Duration totalExecutionTime,
                         final Map<ExecutionTracer.TraceEventType, Long> eventCounts,
                         final int profiledFunctions) {
        this.totalEvents = totalEvents;
        this.totalInstructions = totalInstructions;
        this.totalExecutionTime = Objects.requireNonNull(totalExecutionTime);
        this.eventCounts = Collections.unmodifiableMap(Map.copyOf(eventCounts));
        this.profiledFunctions = profiledFunctions;
    }

    // Getters
    public long getTotalEvents() { return totalEvents; }
    public long getTotalInstructions() { return totalInstructions; }
    public Duration getTotalExecutionTime() { return totalExecutionTime; }
    public Map<ExecutionTracer.TraceEventType, Long> getEventCounts() { return eventCounts; }
    public int getProfiledFunctions() { return profiledFunctions; }

    /**
     * Gets the count for a specific event type.
     */
    public long getEventCount(final ExecutionTracer.TraceEventType type) {
        return eventCounts.getOrDefault(type, 0L);
    }

    /**
     * Calculates instructions per second rate.
     */
    public double getInstructionsPerSecond() {
        if (totalExecutionTime.isZero()) {
            return 0.0;
        }
        return (double) totalInstructions / totalExecutionTime.toSeconds();
    }

    /**
     * Calculates events per second rate.
     */
    public double getEventsPerSecond() {
        if (totalExecutionTime.isZero()) {
            return 0.0;
        }
        return (double) totalEvents / totalExecutionTime.toSeconds();
    }

    @Override
    public String toString() {
        return String.format("TraceStatistics{events=%d, instructions=%d, time=%s, functions=%d, ips=%.2f}",
                           totalEvents, totalInstructions, totalExecutionTime,
                           profiledFunctions, getInstructionsPerSecond());
    }
}
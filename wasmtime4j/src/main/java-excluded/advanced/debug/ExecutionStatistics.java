package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;

/**
 * Execution statistics for debugging sessions.
 */
public final class ExecutionStatistics {
    private final long totalExecutions;
    private final int activeBreakpoints;
    private final int activeWatchExpressions;
    private final int eventListeners;

    public ExecutionStatistics(final long totalExecutions, final int activeBreakpoints,
                             final int activeWatchExpressions, final int eventListeners) {
        this.totalExecutions = totalExecutions;
        this.activeBreakpoints = activeBreakpoints;
        this.activeWatchExpressions = activeWatchExpressions;
        this.eventListeners = eventListeners;
    }

    // Getters
    public long getTotalExecutions() { return totalExecutions; }
    public int getActiveBreakpoints() { return activeBreakpoints; }
    public int getActiveWatchExpressions() { return activeWatchExpressions; }
    public int getEventListeners() { return eventListeners; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ExecutionStatistics)) return false;
        final ExecutionStatistics that = (ExecutionStatistics) o;
        return totalExecutions == that.totalExecutions &&
               activeBreakpoints == that.activeBreakpoints &&
               activeWatchExpressions == that.activeWatchExpressions &&
               eventListeners == that.eventListeners;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalExecutions, activeBreakpoints, activeWatchExpressions, eventListeners);
    }

    @Override
    public String toString() {
        return String.format("ExecutionStatistics{executions=%d, breakpoints=%d, watches=%d, listeners=%d}",
                           totalExecutions, activeBreakpoints, activeWatchExpressions, eventListeners);
    }
}
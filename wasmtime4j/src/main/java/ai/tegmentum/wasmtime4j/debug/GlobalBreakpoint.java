package ai.tegmentum.wasmtime4j.debug;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A global breakpoint that applies across multiple debug targets.
 *
 * <p>Global breakpoints are automatically applied to all targets in a
 * multi-target debugging session and maintained as targets are added or removed.
 *
 * @since 1.0.0
 */
public final class GlobalBreakpoint {

    private final String id;
    private final String functionName;
    private final int line;
    private final long creationTime;
    private final ConcurrentMap<String, Breakpoint> targetBreakpoints;
    private final AtomicLong hitCount;

    /**
     * Creates a new global breakpoint.
     *
     * @param id the unique breakpoint ID
     * @param functionName the function name
     * @param line the line number
     * @param creationTime the creation time
     */
    GlobalBreakpoint(final String id, final String functionName, final int line, final long creationTime) {
        this.id = id;
        this.functionName = functionName;
        this.line = line;
        this.creationTime = creationTime;
        this.targetBreakpoints = new ConcurrentHashMap<>();
        this.hitCount = new AtomicLong(0);
    }

    /**
     * Gets the breakpoint ID.
     *
     * @return the breakpoint ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the function name.
     *
     * @return the function name
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * Gets the line number.
     *
     * @return the line number
     */
    public int getLine() {
        return line;
    }

    /**
     * Gets the creation time.
     *
     * @return the creation time in milliseconds since epoch
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Gets the age of this breakpoint.
     *
     * @return the age in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - creationTime;
    }

    /**
     * Gets the total hit count across all targets.
     *
     * @return the total hit count
     */
    public long getHitCount() {
        return hitCount.get();
    }

    /**
     * Increments the hit count.
     *
     * @return the new hit count
     */
    long incrementHitCount() {
        return hitCount.incrementAndGet();
    }

    /**
     * Gets the number of targets this breakpoint is applied to.
     *
     * @return the number of targets
     */
    public int getTargetCount() {
        return targetBreakpoints.size();
    }

    /**
     * Gets the target breakpoints map.
     *
     * @return immutable map of target ID to breakpoint
     */
    public Map<String, Breakpoint> getTargetBreakpoints() {
        return Collections.unmodifiableMap(targetBreakpoints);
    }

    /**
     * Adds a target-specific breakpoint.
     *
     * @param targetId the target ID
     * @param breakpoint the target breakpoint
     */
    void addTargetBreakpoint(final String targetId, final Breakpoint breakpoint) {
        targetBreakpoints.put(targetId, breakpoint);
    }

    /**
     * Removes a target-specific breakpoint.
     *
     * @param targetId the target ID
     * @return the removed breakpoint, or null if not found
     */
    Breakpoint removeTargetBreakpoint(final String targetId) {
        return targetBreakpoints.remove(targetId);
    }

    /**
     * Gets a target-specific breakpoint.
     *
     * @param targetId the target ID
     * @return the breakpoint, or null if not found
     */
    public Breakpoint getTargetBreakpoint(final String targetId) {
        return targetBreakpoints.get(targetId);
    }

    /**
     * Checks if this breakpoint is applied to the specified target.
     *
     * @param targetId the target ID
     * @return true if applied to the target
     */
    public boolean isAppliedToTarget(final String targetId) {
        return targetBreakpoints.containsKey(targetId);
    }

    /**
     * Gets breakpoint statistics.
     *
     * @return breakpoint statistics
     */
    public GlobalBreakpointStats getStats() {
        return new GlobalBreakpointStats(
            id,
            functionName,
            line,
            creationTime,
            getAge(),
            hitCount.get(),
            targetBreakpoints.size(),
            targetBreakpoints.values().stream()
                .mapToLong(bp -> bp.getHitCount())
                .sum()
        );
    }

    @Override
    public String toString() {
        return "GlobalBreakpoint{" +
               "id='" + id + '\'' +
               ", functionName='" + functionName + '\'' +
               ", line=" + line +
               ", targetCount=" + getTargetCount() +
               ", hitCount=" + getHitCount() +
               ", age=" + getAge() + "ms" +
               '}';
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final GlobalBreakpoint that = (GlobalBreakpoint) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Statistics for a global breakpoint.
     *
     * @param id the breakpoint ID
     * @param functionName the function name
     * @param line the line number
     * @param creationTime the creation time
     * @param age the age in milliseconds
     * @param globalHitCount the global hit count
     * @param targetCount the number of targets
     * @param totalTargetHits the total hits across all targets
     */
    public record GlobalBreakpointStats(
            String id,
            String functionName,
            int line,
            long creationTime,
            long age,
            long globalHitCount,
            int targetCount,
            long totalTargetHits
    ) {
        /**
         * Gets the average hits per target.
         *
         * @return the average hits per target
         */
        public double getAverageHitsPerTarget() {
            return targetCount > 0 ? (double) totalTargetHits / targetCount : 0.0;
        }

        /**
         * Formats the statistics as a string.
         *
         * @return formatted statistics
         */
        public String format() {
            return String.format(
                "Breakpoint %s (%s:%d): %d targets, %d global hits, %d total target hits, avg %.1f hits/target",
                id,
                functionName,
                line,
                targetCount,
                globalHitCount,
                totalTargetHits,
                getAverageHitsPerTarget()
            );
        }
    }
}
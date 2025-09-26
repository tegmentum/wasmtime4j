package ai.tegmentum.wasmtime4j.debug;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Performance profiling data collected during WebAssembly debugging.
 *
 * <p>Contains execution statistics, timing information, and performance
 * metrics that can be used for optimization and analysis.
 *
 * @since 1.0.0
 */
public final class ProfilingData {

    private final long startTime;
    private final long endTime;
    private final long totalInstructions;
    private final long totalCalls;
    private final Map<String, FunctionProfile> functionProfiles;
    private final List<String> hotSpots;
    private final Map<String, Long> counters;
    private final double cpuUsage;
    private final long memoryUsage;
    private final boolean complete;

    /**
     * Creates profiling data.
     *
     * @param startTime profiling start time
     * @param endTime profiling end time
     * @param totalInstructions total instructions executed
     * @param totalCalls total function calls
     * @param functionProfiles per-function profiling data
     * @param hotSpots list of performance hot spots
     * @param counters custom performance counters
     * @param cpuUsage CPU usage percentage
     * @param memoryUsage memory usage in bytes
     * @param complete whether profiling data is complete
     */
    public ProfilingData(final long startTime, final long endTime, final long totalInstructions,
                        final long totalCalls, final Map<String, FunctionProfile> functionProfiles,
                        final List<String> hotSpots, final Map<String, Long> counters,
                        final double cpuUsage, final long memoryUsage, final boolean complete) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalInstructions = totalInstructions;
        this.totalCalls = totalCalls;
        this.functionProfiles = functionProfiles != null ?
                Collections.unmodifiableMap(functionProfiles) : Collections.emptyMap();
        this.hotSpots = hotSpots != null ?
                Collections.unmodifiableList(hotSpots) : Collections.emptyList();
        this.counters = counters != null ?
                Collections.unmodifiableMap(counters) : Collections.emptyMap();
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.complete = complete;
    }

    /**
     * Creates empty profiling data.
     *
     * @return empty profiling data
     */
    public static ProfilingData empty() {
        return new ProfilingData(0, 0, 0, 0, Collections.emptyMap(),
                Collections.emptyList(), Collections.emptyMap(), 0.0, 0, false);
    }

    /**
     * Gets the profiling start time.
     *
     * @return start time in milliseconds
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Gets the profiling end time.
     *
     * @return end time in milliseconds
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Gets the profiling duration.
     *
     * @return duration in milliseconds
     */
    public long getDuration() {
        return endTime - startTime;
    }

    /**
     * Gets the total number of instructions executed.
     *
     * @return total instructions
     */
    public long getTotalInstructions() {
        return totalInstructions;
    }

    /**
     * Gets the total number of function calls.
     *
     * @return total calls
     */
    public long getTotalCalls() {
        return totalCalls;
    }

    /**
     * Gets per-function profiling data.
     *
     * @return function profiles map
     */
    public Map<String, FunctionProfile> getFunctionProfiles() {
        return functionProfiles;
    }

    /**
     * Gets performance hot spots.
     *
     * @return list of hot spot identifiers
     */
    public List<String> getHotSpots() {
        return hotSpots;
    }

    /**
     * Gets custom performance counters.
     *
     * @return counters map
     */
    public Map<String, Long> getCounters() {
        return counters;
    }

    /**
     * Gets CPU usage percentage.
     *
     * @return CPU usage (0.0 to 100.0)
     */
    public double getCpuUsage() {
        return cpuUsage;
    }

    /**
     * Gets memory usage in bytes.
     *
     * @return memory usage
     */
    public long getMemoryUsage() {
        return memoryUsage;
    }

    /**
     * Checks if profiling data is complete.
     *
     * @return true if complete
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Gets instructions per second.
     *
     * @return instructions per second, or 0 if duration is 0
     */
    public double getInstructionsPerSecond() {
        final long duration = getDuration();
        if (duration == 0) {
            return 0.0;
        }
        return (double) totalInstructions / (duration / 1000.0);
    }

    /**
     * Gets calls per second.
     *
     * @return calls per second, or 0 if duration is 0
     */
    public double getCallsPerSecond() {
        final long duration = getDuration();
        if (duration == 0) {
            return 0.0;
        }
        return (double) totalCalls / (duration / 1000.0);
    }

    /**
     * Gets the value of a custom counter.
     *
     * @param name counter name
     * @return counter value, or 0 if not found
     */
    public long getCounter(final String name) {
        return counters.getOrDefault(name, 0L);
    }

    /**
     * Gets profiling data for a specific function.
     *
     * @param functionName function name
     * @return function profile, or null if not found
     */
    public FunctionProfile getFunctionProfile(final String functionName) {
        return functionProfiles.get(functionName);
    }

    /**
     * Merges this profiling data with another.
     *
     * @param other other profiling data
     * @return merged profiling data
     */
    public ProfilingData merge(final ProfilingData other) {
        if (other == null) {
            return this;
        }

        // For simplicity, we'll create basic merged data
        // A real implementation would properly merge all fields
        return new ProfilingData(
                Math.min(startTime, other.startTime),
                Math.max(endTime, other.endTime),
                totalInstructions + other.totalInstructions,
                totalCalls + other.totalCalls,
                functionProfiles, // For now, keep this data as-is
                hotSpots, // For now, keep this data as-is
                counters, // For now, keep this data as-is
                (cpuUsage + other.cpuUsage) / 2.0,
                Math.max(memoryUsage, other.memoryUsage),
                complete && other.complete
        );
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ProfilingData other = (ProfilingData) obj;
        return startTime == other.startTime &&
                endTime == other.endTime &&
                totalInstructions == other.totalInstructions &&
                totalCalls == other.totalCalls &&
                Double.compare(other.cpuUsage, cpuUsage) == 0 &&
                memoryUsage == other.memoryUsage &&
                complete == other.complete &&
                Objects.equals(functionProfiles, other.functionProfiles) &&
                Objects.equals(hotSpots, other.hotSpots) &&
                Objects.equals(counters, other.counters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, totalInstructions, totalCalls,
                functionProfiles, hotSpots, counters, cpuUsage, memoryUsage, complete);
    }

    @Override
    public String toString() {
        return "ProfilingData{" +
                "duration=" + getDuration() + "ms" +
                ", totalInstructions=" + totalInstructions +
                ", totalCalls=" + totalCalls +
                ", functionsProfiled=" + functionProfiles.size() +
                ", hotSpots=" + hotSpots.size() +
                ", cpuUsage=" + String.format("%.1f%%", cpuUsage) +
                ", memoryUsage=" + memoryUsage +
                ", complete=" + complete +
                ", ips=" + String.format("%.0f", getInstructionsPerSecond()) +
                ", cps=" + String.format("%.0f", getCallsPerSecond()) +
                '}';
    }
}
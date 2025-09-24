package ai.tegmentum.wasmtime4j.debug;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Performance analysis result from execution tracing.
 * Identifies performance bottlenecks and provides optimization recommendations.
 */
public final class PerformanceAnalysis {
    private final Duration totalExecutionTime;
    private final long totalInstructions;
    private final List<PerformanceBottleneck> bottlenecks;
    private final int analyzedFunctions;

    public PerformanceAnalysis(final Duration totalExecutionTime, final long totalInstructions,
                             final List<PerformanceBottleneck> bottlenecks, final int analyzedFunctions) {
        this.totalExecutionTime = Objects.requireNonNull(totalExecutionTime);
        this.totalInstructions = totalInstructions;
        this.bottlenecks = Collections.unmodifiableList(List.copyOf(bottlenecks));
        this.analyzedFunctions = analyzedFunctions;
    }

    // Getters
    public Duration getTotalExecutionTime() { return totalExecutionTime; }
    public long getTotalInstructions() { return totalInstructions; }
    public List<PerformanceBottleneck> getBottlenecks() { return bottlenecks; }
    public int getAnalyzedFunctions() { return analyzedFunctions; }

    /**
     * Calculates overall instructions per second rate.
     */
    public double getInstructionsPerSecond() {
        if (totalExecutionTime.isZero()) {
            return 0.0;
        }
        return (double) totalInstructions / totalExecutionTime.toSeconds();
    }

    /**
     * Gets high-priority bottlenecks that should be addressed first.
     */
    public List<PerformanceBottleneck> getHighPriorityBottlenecks() {
        return bottlenecks.stream()
            .filter(b -> b.getType() == BottleneckType.HOT_FUNCTION ||
                        b.getType() == BottleneckType.MEMORY_INTENSIVE)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Generates optimization recommendations based on the analysis.
     */
    public List<String> getOptimizationRecommendations() {
        final List<String> recommendations = new java.util.ArrayList<>();

        for (final PerformanceBottleneck bottleneck : bottlenecks) {
            switch (bottleneck.getType()) {
                case HOT_FUNCTION:
                    recommendations.add(String.format(
                        "Optimize function '%s' - accounts for significant execution time",
                        bottleneck.getFunctionName()));
                    break;
                case FREQUENT_CALLS:
                    recommendations.add(String.format(
                        "Reduce call frequency for function '%s' (%d calls) - consider inlining or caching",
                        bottleneck.getFunctionName(), bottleneck.getCallCount()));
                    break;
                case MEMORY_INTENSIVE:
                    recommendations.add(String.format(
                        "Optimize memory usage in function '%s' - high memory access overhead",
                        bottleneck.getFunctionName()));
                    break;
                case SLOW_EXECUTION:
                    recommendations.add(String.format(
                        "Investigate slow execution in function '%s' - average time %s",
                        bottleneck.getFunctionName(), bottleneck.getExecutionTime()));
                    break;
                default:
                    recommendations.add(String.format(
                        "Review function '%s' for potential optimizations",
                        bottleneck.getFunctionName()));
                    break;
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("No significant performance bottlenecks identified");
        }

        return Collections.unmodifiableList(recommendations);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof PerformanceAnalysis)) return false;
        final PerformanceAnalysis that = (PerformanceAnalysis) o;
        return totalInstructions == that.totalInstructions &&
               analyzedFunctions == that.analyzedFunctions &&
               Objects.equals(totalExecutionTime, that.totalExecutionTime) &&
               Objects.equals(bottlenecks, that.bottlenecks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalExecutionTime, totalInstructions, bottlenecks, analyzedFunctions);
    }

    @Override
    public String toString() {
        return String.format("PerformanceAnalysis{totalTime=%s, instructions=%d, bottlenecks=%d, functions=%d, ips=%.2f}",
                           totalExecutionTime, totalInstructions, bottlenecks.size(),
                           analyzedFunctions, getInstructionsPerSecond());
    }
}

/**
 * Represents a performance bottleneck identified during analysis.
 */
final class PerformanceBottleneck {
    private final String functionName;
    private final BottleneckType type;
    private final Duration executionTime;
    private final long callCount;
    private final String description;

    public PerformanceBottleneck(final String functionName, final BottleneckType type,
                               final Duration executionTime, final long callCount,
                               final String description) {
        this.functionName = Objects.requireNonNull(functionName);
        this.type = Objects.requireNonNull(type);
        this.executionTime = Objects.requireNonNull(executionTime);
        this.callCount = callCount;
        this.description = Objects.requireNonNull(description);
    }

    // Getters
    public String getFunctionName() { return functionName; }
    public BottleneckType getType() { return type; }
    public Duration getExecutionTime() { return executionTime; }
    public long getCallCount() { return callCount; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format("PerformanceBottleneck{function='%s', type=%s, time=%s, calls=%d, desc='%s'}",
                           functionName, type, executionTime, callCount, description);
    }
}

/**
 * Types of performance bottlenecks.
 */
enum BottleneckType {
    HOT_FUNCTION,      // Function consuming significant CPU time
    FREQUENT_CALLS,    // Function called very frequently
    SLOW_EXECUTION,    // Function with slow individual execution
    MEMORY_INTENSIVE,  // Function with high memory access overhead
    IO_BOUND          // Function waiting on I/O operations
}
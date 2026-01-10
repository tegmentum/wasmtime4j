/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.profiling;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Analyzes profiling data to generate performance insights and recommendations.
 *
 * <p>This class processes profiling statistics and flame graph data to identify:
 *
 * <ul>
 *   <li>Hot spots (functions consuming the most time)
 *   <li>Memory-intensive operations
 *   <li>Performance bottlenecks
 *   <li>Optimization opportunities
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * PerformanceInsights insights = new PerformanceInsights();
 * PerformanceInsightsResult result = insights.analyzePerformance(flameGraph, stats);
 *
 * // Get hot spots
 * List<HotSpot> hotSpots = result.getHotSpots();
 * for (HotSpot hotSpot : hotSpots) {
 *     System.out.println(hotSpot.getFunctionName() + ": " + hotSpot.getTimePercentage() + "%");
 * }
 *
 * // Get recommendations
 * List<String> recommendations = result.getRecommendations();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class PerformanceInsights {

  private static final double HOT_SPOT_THRESHOLD_PERCENT = 5.0;
  private static final int MAX_HOT_SPOTS = 10;

  /** Creates a new performance insights analyzer. */
  public PerformanceInsights() {}

  /**
   * Analyzes performance data and generates insights.
   *
   * @param flameGraph the flame graph root frame
   * @param statistics the profiling statistics
   * @return the analysis results
   */
  public PerformanceInsightsResult analyzePerformance(
      final FlameGraphGenerator.FlameFrame flameGraph,
      final AdvancedProfiler.ProfilingStatistics statistics) {
    Objects.requireNonNull(flameGraph, "Flame graph cannot be null");
    Objects.requireNonNull(statistics, "Statistics cannot be null");

    final List<HotSpot> hotSpots = identifyHotSpots(flameGraph, statistics);
    final List<String> recommendations = generateRecommendations(hotSpots, statistics);
    final PerformanceSummary summary = generateSummary(statistics, hotSpots);

    return new PerformanceInsightsResult(hotSpots, recommendations, summary);
  }

  private List<HotSpot> identifyHotSpots(
      final FlameGraphGenerator.FlameFrame flameGraph,
      final AdvancedProfiler.ProfilingStatistics statistics) {
    final List<HotSpot> hotSpots = new ArrayList<>();
    final long totalNanos = flameGraph.getTotalTime().toNanos();

    if (totalNanos == 0) {
      return hotSpots;
    }

    // Collect hot spots from flame graph
    collectHotSpots(flameGraph, totalNanos, hotSpots, 0);

    // Also add from statistics
    final Map<String, Long> functionCalls = statistics.getFunctionCalls();
    for (final Map.Entry<String, Long> entry : functionCalls.entrySet()) {
      final double callPercentage = (double) entry.getValue() / statistics.getTotalCalls() * 100.0;
      if (callPercentage >= HOT_SPOT_THRESHOLD_PERCENT) {
        // Check if already in list
        boolean found = false;
        for (final HotSpot existing : hotSpots) {
          if (existing.getFunctionName().equals(entry.getKey())) {
            found = true;
            break;
          }
        }
        if (!found) {
          hotSpots.add(
              new HotSpot(
                  entry.getKey(),
                  Duration.ZERO,
                  callPercentage,
                  entry.getValue(),
                  0,
                  HotSpotType.HIGH_CALL_COUNT));
        }
      }
    }

    // Sort by time percentage descending
    hotSpots.sort(Comparator.comparingDouble(HotSpot::getTimePercentage).reversed());

    // Limit to top N
    if (hotSpots.size() > MAX_HOT_SPOTS) {
      return new ArrayList<>(hotSpots.subList(0, MAX_HOT_SPOTS));
    }

    return hotSpots;
  }

  private void collectHotSpots(
      final FlameGraphGenerator.FlameFrame frame,
      final long totalNanos,
      final List<HotSpot> hotSpots,
      final int depth) {
    if (depth > 0) { // Skip root frame
      final double timePercentage = (double) frame.getTotalTime().toNanos() / totalNanos * 100.0;
      if (timePercentage >= HOT_SPOT_THRESHOLD_PERCENT) {
        hotSpots.add(
            new HotSpot(
                frame.getName(),
                frame.getTotalTime(),
                timePercentage,
                0,
                depth,
                HotSpotType.HIGH_CPU_TIME));
      }
    }

    for (final FlameGraphGenerator.FlameFrame child : frame.getChildren()) {
      collectHotSpots(child, totalNanos, hotSpots, depth + 1);
    }
  }

  private List<String> generateRecommendations(
      final List<HotSpot> hotSpots, final AdvancedProfiler.ProfilingStatistics statistics) {
    final List<String> recommendations = new ArrayList<>();

    if (hotSpots.isEmpty()) {
      recommendations.add(
          "No significant hot spots detected. Performance appears to be well-distributed.");
      return recommendations;
    }

    // Analyze hot spots and generate recommendations
    for (final HotSpot hotSpot : hotSpots) {
      if (hotSpot.getTimePercentage() > 50.0) {
        recommendations.add(
            String.format(
                "Critical: Function '%s' consumes %.1f%% of execution time. "
                    + "Consider optimizing or caching results.",
                hotSpot.getFunctionName(), hotSpot.getTimePercentage()));
      } else if (hotSpot.getTimePercentage() > 20.0) {
        recommendations.add(
            String.format(
                "High impact: Function '%s' uses %.1f%% of execution time. "
                    + "Review for optimization opportunities.",
                hotSpot.getFunctionName(), hotSpot.getTimePercentage()));
      }

      if (hotSpot.getCallCount() > 10000 && hotSpot.getTimePercentage() < 5.0) {
        recommendations.add(
            String.format(
                "Function '%s' is called frequently (%d times) but has low individual cost. "
                    + "Consider if call overhead is significant.",
                hotSpot.getFunctionName(), hotSpot.getCallCount()));
      }
    }

    // Memory recommendations
    if (statistics.getTotalMemory() > 100_000_000) { // > 100MB
      recommendations.add(
          String.format(
              "High memory usage detected (%.1f MB). "
                  + "Consider memory pooling or reducing allocation frequency.",
              statistics.getTotalMemory() / 1_000_000.0));
    }

    if (recommendations.isEmpty()) {
      recommendations.add(
          "Performance is acceptable. Minor optimizations may be possible in hot spot functions.");
    }

    return recommendations;
  }

  private PerformanceSummary generateSummary(
      final AdvancedProfiler.ProfilingStatistics statistics, final List<HotSpot> hotSpots) {
    final double avgCallTime =
        statistics.getTotalCalls() > 0
            ? (double) statistics.getTotalTime().toNanos() / statistics.getTotalCalls()
            : 0.0;

    final double hotSpotCoverage = hotSpots.stream().mapToDouble(HotSpot::getTimePercentage).sum();

    return new PerformanceSummary(
        statistics.getTotalCalls(),
        statistics.getTotalTime(),
        statistics.getTotalMemory(),
        avgCallTime,
        hotSpots.size(),
        hotSpotCoverage);
  }

  /** Results of performance analysis. */
  public static final class PerformanceInsightsResult {

    private final List<HotSpot> hotSpots;
    private final List<String> recommendations;
    private final PerformanceSummary summary;

    PerformanceInsightsResult(
        final List<HotSpot> hotSpots,
        final List<String> recommendations,
        final PerformanceSummary summary) {
      this.hotSpots = new ArrayList<>(hotSpots);
      this.recommendations = new ArrayList<>(recommendations);
      this.summary = summary;
    }

    /**
     * Gets the identified hot spots.
     *
     * @return the identified hot spots
     */
    public List<HotSpot> getHotSpots() {
      return Collections.unmodifiableList(hotSpots);
    }

    /**
     * Gets the performance recommendations.
     *
     * @return the performance recommendations
     */
    public List<String> getRecommendations() {
      return Collections.unmodifiableList(recommendations);
    }

    /**
     * Gets the performance summary.
     *
     * @return the performance summary
     */
    public PerformanceSummary getSummary() {
      return summary;
    }
  }

  /** A performance hot spot - a function or scope that consumes significant resources. */
  public static final class HotSpot {

    private final String functionName;
    private final Duration totalTime;
    private final double timePercentage;
    private final long callCount;
    private final int stackDepth;
    private final HotSpotType type;

    HotSpot(
        final String functionName,
        final Duration totalTime,
        final double timePercentage,
        final long callCount,
        final int stackDepth,
        final HotSpotType type) {
      this.functionName = functionName;
      this.totalTime = totalTime;
      this.timePercentage = timePercentage;
      this.callCount = callCount;
      this.stackDepth = stackDepth;
      this.type = type;
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
     * Gets the total time spent in this function.
     *
     * @return the total time spent in this function
     */
    public Duration getTotalTime() {
      return totalTime;
    }

    /**
     * Gets the percentage of total time spent in this function.
     *
     * @return the percentage of total time spent in this function
     */
    public double getTimePercentage() {
      return timePercentage;
    }

    /**
     * Gets the number of times this function was called.
     *
     * @return the number of times this function was called
     */
    public long getCallCount() {
      return callCount;
    }

    /**
     * Gets the stack depth where this function typically appears.
     *
     * @return the stack depth where this function typically appears
     */
    public int getStackDepth() {
      return stackDepth;
    }

    /**
     * Gets the type of hot spot.
     *
     * @return the type of hot spot
     */
    public HotSpotType getType() {
      return type;
    }

    @Override
    public String toString() {
      return String.format(
          "HotSpot{function='%s', time=%.1f%%, calls=%d, type=%s}",
          functionName, timePercentage, callCount, type);
    }
  }

  /** Type of hot spot. */
  public enum HotSpotType {
    /** High CPU time consumption. */
    HIGH_CPU_TIME,

    /** High call count. */
    HIGH_CALL_COUNT,

    /** High memory usage. */
    HIGH_MEMORY,

    /** Deep call stack. */
    DEEP_STACK
  }

  /** Summary of overall performance. */
  public static final class PerformanceSummary {

    private final long totalCalls;
    private final Duration totalTime;
    private final long totalMemory;
    private final double avgCallTimeNanos;
    private final int hotSpotCount;
    private final double hotSpotCoveragePercent;

    PerformanceSummary(
        final long totalCalls,
        final Duration totalTime,
        final long totalMemory,
        final double avgCallTimeNanos,
        final int hotSpotCount,
        final double hotSpotCoveragePercent) {
      this.totalCalls = totalCalls;
      this.totalTime = totalTime;
      this.totalMemory = totalMemory;
      this.avgCallTimeNanos = avgCallTimeNanos;
      this.hotSpotCount = hotSpotCount;
      this.hotSpotCoveragePercent = hotSpotCoveragePercent;
    }

    /**
     * Gets the total number of function calls.
     *
     * @return the total number of function calls
     */
    public long getTotalCalls() {
      return totalCalls;
    }

    /**
     * Gets the total execution time.
     *
     * @return the total execution time
     */
    public Duration getTotalTime() {
      return totalTime;
    }

    /**
     * Gets the total memory used in bytes.
     *
     * @return the total memory used in bytes
     */
    public long getTotalMemory() {
      return totalMemory;
    }

    /**
     * Gets the average call time in nanoseconds.
     *
     * @return the average call time in nanoseconds
     */
    public double getAvgCallTimeNanos() {
      return avgCallTimeNanos;
    }

    /**
     * Gets the number of identified hot spots.
     *
     * @return the number of identified hot spots
     */
    public int getHotSpotCount() {
      return hotSpotCount;
    }

    /**
     * Gets the percentage of time covered by identified hot spots.
     *
     * @return the percentage of time covered by identified hot spots
     */
    public double getHotSpotCoveragePercent() {
      return hotSpotCoveragePercent;
    }

    @Override
    public String toString() {
      return String.format(
          "PerformanceSummary{calls=%d, time=%s, memory=%d, avgCall=%.1fns, hotSpots=%d (%.1f%%)}",
          totalCalls,
          totalTime,
          totalMemory,
          avgCallTimeNanos,
          hotSpotCount,
          hotSpotCoveragePercent);
    }
  }
}

package ai.tegmentum.wasmtime4j.compilation;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregated performance statistics for optimization strategies.
 *
 * <p>Provides a comprehensive view of optimization strategy performance across
 * all strategies, including success rates, compilation times, and performance gains.
 *
 * @since 1.0.0
 */
public final class StrategyPerformanceStats {

  private final long totalOptimizations;
  private final long successfulOptimizations;
  private final long failedOptimizations;
  private final Map<String, StrategyPerformanceTracker.StrategyMetrics> strategyMetrics;
  private final double overallSuccessRate;
  private final Duration averageCompilationTime;
  private final double averagePerformanceGain;

  /**
   * Creates performance statistics from tracking data.
   *
   * @param totalOptimizations total number of optimization attempts
   * @param successfulOptimizations number of successful optimizations
   * @param failedOptimizations number of failed optimizations
   * @param strategyMetrics metrics for each strategy
   */
  public StrategyPerformanceStats(final long totalOptimizations,
                                  final long successfulOptimizations,
                                  final long failedOptimizations,
                                  final Map<String, StrategyPerformanceTracker.StrategyMetrics> strategyMetrics) {
    this.totalOptimizations = totalOptimizations;
    this.successfulOptimizations = successfulOptimizations;
    this.failedOptimizations = failedOptimizations;
    this.strategyMetrics = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(strategyMetrics)));

    // Calculate derived statistics
    this.overallSuccessRate = totalOptimizations == 0 ? 0.0 : (double) successfulOptimizations / totalOptimizations;
    this.averageCompilationTime = calculateAverageCompilationTime();
    this.averagePerformanceGain = calculateAveragePerformanceGain();
  }

  /**
   * Gets the total number of optimization attempts across all strategies.
   *
   * @return total optimization count
   */
  public long getTotalOptimizations() {
    return totalOptimizations;
  }

  /**
   * Gets the number of successful optimizations across all strategies.
   *
   * @return successful optimization count
   */
  public long getSuccessfulOptimizations() {
    return successfulOptimizations;
  }

  /**
   * Gets the number of failed optimizations across all strategies.
   *
   * @return failed optimization count
   */
  public long getFailedOptimizations() {
    return failedOptimizations;
  }

  /**
   * Gets the overall success rate across all strategies.
   *
   * @return success rate (0.0 to 1.0)
   */
  public double getOverallSuccessRate() {
    return overallSuccessRate;
  }

  /**
   * Gets the average compilation time across all strategies.
   *
   * @return average compilation time
   */
  public Duration getAverageCompilationTime() {
    return averageCompilationTime;
  }

  /**
   * Gets the average performance gain across all successful optimizations.
   *
   * @return average performance gain multiplier
   */
  public double getAveragePerformanceGain() {
    return averagePerformanceGain;
  }

  /**
   * Gets the metrics for all strategies.
   *
   * @return map of strategy names to their metrics
   */
  public Map<String, StrategyPerformanceTracker.StrategyMetrics> getStrategyMetrics() {
    return strategyMetrics;
  }

  /**
   * Gets the metrics for a specific strategy.
   *
   * @param strategyName the strategy name
   * @return strategy metrics, or null if not found
   */
  public StrategyPerformanceTracker.StrategyMetrics getStrategyMetrics(final String strategyName) {
    return strategyMetrics.get(strategyName);
  }

  /**
   * Gets the number of strategies tracked.
   *
   * @return strategy count
   */
  public int getStrategyCount() {
    return strategyMetrics.size();
  }

  /**
   * Finds the most successful strategy by success rate.
   *
   * @return name of the most successful strategy, or null if no strategies
   */
  public String getMostSuccessfulStrategy() {
    return strategyMetrics.entrySet().stream()
        .max((e1, e2) -> Double.compare(e1.getValue().getSuccessRate(), e2.getValue().getSuccessRate()))
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /**
   * Finds the fastest strategy by average compilation time.
   *
   * @return name of the fastest strategy, or null if no strategies
   */
  public String getFastestStrategy() {
    return strategyMetrics.entrySet().stream()
        .min((e1, e2) -> e1.getValue().getAverageCompilationTime().compareTo(e2.getValue().getAverageCompilationTime()))
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /**
   * Finds the strategy with the highest average performance gain.
   *
   * @return name of the highest performing strategy, or null if no strategies
   */
  public String getHighestPerformingStrategy() {
    return strategyMetrics.entrySet().stream()
        .max((e1, e2) -> Double.compare(e1.getValue().getAveragePerformanceGain(), e2.getValue().getAveragePerformanceGain()))
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /**
   * Checks if there are any optimization statistics available.
   *
   * @return true if statistics are available, false otherwise
   */
  public boolean hasStatistics() {
    return totalOptimizations > 0;
  }

  /**
   * Creates a summary report of the performance statistics.
   *
   * @return formatted summary string
   */
  public String getSummary() {
    if (!hasStatistics()) {
      return "No optimization statistics available";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("Optimization Performance Summary:\n");
    sb.append(String.format("  Total optimizations: %d\n", totalOptimizations));
    sb.append(String.format("  Successful: %d (%.1f%%)\n", successfulOptimizations, overallSuccessRate * 100));
    sb.append(String.format("  Failed: %d (%.1f%%)\n", failedOptimizations, (1.0 - overallSuccessRate) * 100));
    sb.append(String.format("  Average compilation time: %s\n", averageCompilationTime));
    sb.append(String.format("  Average performance gain: %.2fx\n", averagePerformanceGain));
    sb.append(String.format("  Strategies tracked: %d\n", getStrategyCount()));

    final String mostSuccessful = getMostSuccessfulStrategy();
    if (mostSuccessful != null) {
      sb.append(String.format("  Most successful strategy: %s\n", mostSuccessful));
    }

    final String fastest = getFastestStrategy();
    if (fastest != null) {
      sb.append(String.format("  Fastest strategy: %s\n", fastest));
    }

    final String highestPerforming = getHighestPerformingStrategy();
    if (highestPerforming != null) {
      sb.append(String.format("  Highest performing strategy: %s\n", highestPerforming));
    }

    return sb.toString();
  }

  private Duration calculateAverageCompilationTime() {
    if (strategyMetrics.isEmpty()) {
      return Duration.ZERO;
    }

    long totalMs = 0;
    long totalAttempts = 0;

    for (final StrategyPerformanceTracker.StrategyMetrics metrics : strategyMetrics.values()) {
      totalMs += metrics.getAverageCompilationTime().toMillis() * metrics.getAttemptCount();
      totalAttempts += metrics.getAttemptCount();
    }

    return totalAttempts == 0 ? Duration.ZERO : Duration.ofMillis(totalMs / totalAttempts);
  }

  private double calculateAveragePerformanceGain() {
    if (strategyMetrics.isEmpty()) {
      return 0.0;
    }

    double totalGain = 0.0;
    long totalSuccesses = 0;

    for (final StrategyPerformanceTracker.StrategyMetrics metrics : strategyMetrics.values()) {
      totalGain += metrics.getAveragePerformanceGain() * metrics.getSuccessCount();
      totalSuccesses += metrics.getSuccessCount();
    }

    return totalSuccesses == 0 ? 0.0 : totalGain / totalSuccesses;
  }

  @Override
  public String toString() {
    return String.format("StrategyPerformanceStats{total=%d, successful=%d, failed=%d, successRate=%.2f, strategies=%d}",
        totalOptimizations, successfulOptimizations, failedOptimizations, overallSuccessRate, getStrategyCount());
  }
}
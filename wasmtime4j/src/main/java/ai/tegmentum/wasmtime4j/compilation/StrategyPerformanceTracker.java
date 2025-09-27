package ai.tegmentum.wasmtime4j.compilation;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks performance metrics for optimization strategies.
 *
 * <p>Monitors the effectiveness of different optimization strategies by tracking
 * compilation times, execution improvements, and resource usage.
 *
 * @since 1.0.0
 */
public final class StrategyPerformanceTracker {

  private final Map<String, StrategyMetrics> strategyMetrics;
  private final AtomicLong totalOptimizations;
  private final AtomicLong successfulOptimizations;
  private final AtomicLong failedOptimizations;

  public StrategyPerformanceTracker() {
    this.strategyMetrics = new ConcurrentHashMap<>();
    this.totalOptimizations = new AtomicLong(0);
    this.successfulOptimizations = new AtomicLong(0);
    this.failedOptimizations = new AtomicLong(0);
  }

  /**
   * Records the start of an optimization strategy.
   *
   * @param strategyName the name of the optimization strategy
   * @param functionName the function being optimized
   * @return tracking token for this optimization
   */
  public OptimizationToken startOptimization(final String strategyName, final String functionName) {
    final String token = strategyName + ":" + functionName + ":" + System.nanoTime();
    return new OptimizationToken(token, strategyName, functionName, Instant.now());
  }

  /**
   * Records a successful optimization completion.
   *
   * @param token the optimization tracking token
   * @param performanceGain the measured performance improvement
   * @param compilationTimeMs the time spent on compilation
   */
  public void recordSuccess(final OptimizationToken token,
                            final double performanceGain,
                            final long compilationTimeMs) {
    Objects.requireNonNull(token);

    totalOptimizations.incrementAndGet();
    successfulOptimizations.incrementAndGet();

    final StrategyMetrics metrics = strategyMetrics.computeIfAbsent(
        token.getStrategyName(), k -> new StrategyMetrics());

    metrics.recordSuccess(performanceGain, compilationTimeMs, token.getStartTime());
  }

  /**
   * Records a failed optimization attempt.
   *
   * @param token the optimization tracking token
   * @param errorMessage the error that occurred
   * @param compilationTimeMs the time spent before failure
   */
  public void recordFailure(final OptimizationToken token,
                            final String errorMessage,
                            final long compilationTimeMs) {
    Objects.requireNonNull(token);

    totalOptimizations.incrementAndGet();
    failedOptimizations.incrementAndGet();

    final StrategyMetrics metrics = strategyMetrics.computeIfAbsent(
        token.getStrategyName(), k -> new StrategyMetrics());

    metrics.recordFailure(errorMessage, compilationTimeMs, token.getStartTime());
  }

  /**
   * Gets performance statistics for all strategies.
   *
   * @return performance statistics
   */
  public StrategyPerformanceStats getOverallStats() {
    return new StrategyPerformanceStats(
        totalOptimizations.get(),
        successfulOptimizations.get(),
        failedOptimizations.get(),
        strategyMetrics);
  }

  /**
   * Gets performance statistics for a specific strategy.
   *
   * @param strategyName the strategy name
   * @return strategy-specific statistics, or null if strategy not found
   */
  public StrategyMetrics getStrategyStats(final String strategyName) {
    return strategyMetrics.get(strategyName);
  }

  /**
   * Gets the success rate across all optimizations.
   *
   * @return success rate (0.0 to 1.0)
   */
  public double getOverallSuccessRate() {
    final long total = totalOptimizations.get();
    return total == 0 ? 0.0 : (double) successfulOptimizations.get() / total;
  }

  /**
   * Resets all performance tracking data.
   */
  public void reset() {
    strategyMetrics.clear();
    totalOptimizations.set(0);
    successfulOptimizations.set(0);
    failedOptimizations.set(0);
  }

  /**
   * Token representing an active optimization operation.
   */
  public static final class OptimizationToken {
    private final String token;
    private final String strategyName;
    private final String functionName;
    private final Instant startTime;

    private OptimizationToken(final String token,
                              final String strategyName,
                              final String functionName,
                              final Instant startTime) {
      this.token = token;
      this.strategyName = strategyName;
      this.functionName = functionName;
      this.startTime = startTime;
    }

    public String getToken() { return token; }
    public String getStrategyName() { return strategyName; }
    public String getFunctionName() { return functionName; }
    public Instant getStartTime() { return startTime; }

    /**
     * Gets the elapsed time since optimization started.
     *
     * @return elapsed duration
     */
    public Duration getElapsedTime() {
      return Duration.between(startTime, Instant.now());
    }
  }

  /**
   * Metrics for a specific optimization strategy.
   */
  public static final class StrategyMetrics {
    private final AtomicLong attemptCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong totalCompilationTimeMs = new AtomicLong(0);
    private volatile double totalPerformanceGain = 0.0;
    private volatile double minPerformanceGain = Double.MAX_VALUE;
    private volatile double maxPerformanceGain = 0.0;

    private synchronized void recordSuccess(final double performanceGain,
                                            final long compilationTimeMs,
                                            final Instant startTime) {
      attemptCount.incrementAndGet();
      successCount.incrementAndGet();
      totalCompilationTimeMs.addAndGet(compilationTimeMs);

      totalPerformanceGain += performanceGain;
      minPerformanceGain = Math.min(minPerformanceGain, performanceGain);
      maxPerformanceGain = Math.max(maxPerformanceGain, performanceGain);
    }

    private void recordFailure(final String errorMessage,
                               final long compilationTimeMs,
                               final Instant startTime) {
      attemptCount.incrementAndGet();
      failureCount.incrementAndGet();
      totalCompilationTimeMs.addAndGet(compilationTimeMs);
    }

    public long getAttemptCount() { return attemptCount.get(); }
    public long getSuccessCount() { return successCount.get(); }
    public long getFailureCount() { return failureCount.get(); }

    public double getSuccessRate() {
      final long attempts = attemptCount.get();
      return attempts == 0 ? 0.0 : (double) successCount.get() / attempts;
    }

    public Duration getAverageCompilationTime() {
      final long attempts = attemptCount.get();
      return attempts == 0 ? Duration.ZERO :
          Duration.ofMillis(totalCompilationTimeMs.get() / attempts);
    }

    public synchronized double getAveragePerformanceGain() {
      final long successes = successCount.get();
      return successes == 0 ? 0.0 : totalPerformanceGain / successes;
    }

    public synchronized double getMinPerformanceGain() {
      return minPerformanceGain == Double.MAX_VALUE ? 0.0 : minPerformanceGain;
    }

    public synchronized double getMaxPerformanceGain() {
      return maxPerformanceGain;
    }
  }
}
package ai.tegmentum.wasmtime4j.monitoring;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageMetrics;
import ai.tegmentum.wasmtime4j.comparison.analyzers.GlobalCoverageStatistics;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Real-time coverage monitoring system that provides continuous tracking, trend analysis, and
 * automated alerting for test coverage across WebAssembly features and runtime implementations.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Real-time coverage percentage tracking with sub-minute updates
 *   <li>Coverage trend analysis with historical progression tracking
 *   <li>Automated regression detection with configurable thresholds
 *   <li>Multi-runtime coverage consistency monitoring
 *   <li>Integration with dashboard and alerting systems
 * </ul>
 *
 * @since 1.0.0
 */
public final class RealTimeCoverageMonitor {
  private static final Logger LOGGER = Logger.getLogger(RealTimeCoverageMonitor.class.getName());

  // Monitoring configuration constants
  private static final Duration DEFAULT_UPDATE_INTERVAL = Duration.ofMinutes(1);
  private static final Duration TREND_ANALYSIS_WINDOW = Duration.ofHours(24);
  private static final double DEFAULT_REGRESSION_THRESHOLD = 1.0; // 1% drop triggers alert
  private static final double CRITICAL_THRESHOLD = 90.0; // Coverage below 90% is critical
  private static final double WARNING_THRESHOLD = 95.0; // Coverage below 95% is warning

  // Monitoring state
  private final ScheduledExecutorService scheduler;
  private final Map<String, CoverageSnapshot> coverageHistory;
  private final AtomicReference<GlobalCoverageStatistics> currentStatistics;
  private final AtomicLong totalTestsExecuted;
  private final AtomicLong lastUpdateTimestamp;
  private final List<CoverageRegressionListener> regressionListeners;
  private final List<DashboardUpdateListener> dashboardListeners;

  // Configuration
  private final Duration updateInterval;
  private final double regressionThreshold;
  private final boolean realTimeMode;

  /** Creates a new real-time coverage monitor with default configuration. */
  public RealTimeCoverageMonitor() {
    this(DEFAULT_UPDATE_INTERVAL, DEFAULT_REGRESSION_THRESHOLD, true);
  }

  /**
   * Creates a new real-time coverage monitor with custom configuration.
   *
   * @param updateInterval the interval between coverage updates
   * @param regressionThreshold the percentage drop that triggers regression alerts
   * @param realTimeMode whether to enable real-time monitoring
   */
  public RealTimeCoverageMonitor(
      final Duration updateInterval, final double regressionThreshold, final boolean realTimeMode) {
    this.updateInterval = updateInterval;
    this.regressionThreshold = regressionThreshold;
    this.realTimeMode = realTimeMode;
    this.scheduler = Executors.newScheduledThreadPool(2);
    this.coverageHistory = new ConcurrentHashMap<>();
    this.currentStatistics = new AtomicReference<>();
    this.totalTestsExecuted = new AtomicLong(0);
    this.lastUpdateTimestamp = new AtomicLong(System.currentTimeMillis());
    this.regressionListeners = new ArrayList<>();
    this.dashboardListeners = new ArrayList<>();

    if (realTimeMode) {
      startRealTimeMonitoring();
    }

    LOGGER.info(
        "Real-time coverage monitor initialized with " + updateInterval + " update interval");
  }

  /**
   * Records a new coverage analysis result and updates real-time tracking.
   *
   * @param testName the name of the test
   * @param coverageResult the coverage analysis result
   */
  public void recordCoverageResult(
      final String testName, final CoverageAnalysisResult coverageResult) {
    final Instant timestamp = Instant.now();
    final CoverageMetrics metrics = coverageResult.getCoverageMetrics();

    // Update total test count
    totalTestsExecuted.incrementAndGet();

    // Create coverage snapshot
    final CoverageSnapshot snapshot =
        new CoverageSnapshot(
            timestamp,
            metrics.getOverallCoveragePercentage(),
            metrics.getRuntimeCoveragePercentages(),
            testName,
            metrics.getTotalFeaturesDetected(),
            metrics.getSuccessRate());

    // Store in history
    coverageHistory.put(generateSnapshotKey(timestamp), snapshot);

    // Update current statistics
    updateCurrentStatistics(snapshot);

    // Check for regressions
    checkForRegressions(snapshot);

    // Notify dashboard listeners
    notifyDashboardUpdate(snapshot);

    // Update last update timestamp
    lastUpdateTimestamp.set(System.currentTimeMillis());

    LOGGER.fine(
        String.format(
            "Recorded coverage result for test %s: %.2f%% coverage, %d features",
            testName, metrics.getOverallCoveragePercentage(), metrics.getTotalFeaturesDetected()));
  }

  /**
   * Gets the current real-time coverage statistics.
   *
   * @return current coverage statistics
   */
  public GlobalCoverageStatistics getCurrentStatistics() {
    return currentStatistics.get();
  }

  /**
   * Analyzes coverage trends over the specified time window.
   *
   * @param timeWindow the time window for trend analysis
   * @return coverage trend analysis result
   */
  public CoverageTrendAnalysis analyzeTrend(final Duration timeWindow) {
    final Instant cutoff = Instant.now().minus(timeWindow);
    final List<CoverageSnapshot> recentSnapshots = new ArrayList<>();

    coverageHistory.values().stream()
        .filter(snapshot -> snapshot.getTimestamp().isAfter(cutoff))
        .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
        .forEach(recentSnapshots::add);

    if (recentSnapshots.isEmpty()) {
      return new CoverageTrendAnalysis(TrendDirection.STABLE, 0.0, 0.0, 0.0);
    }

    if (recentSnapshots.size() == 1) {
      final double current = recentSnapshots.get(0).getCoveragePercentage();
      return new CoverageTrendAnalysis(TrendDirection.STABLE, current, current, 0.0);
    }

    final double startCoverage = recentSnapshots.get(0).getCoveragePercentage();
    final double endCoverage =
        recentSnapshots.get(recentSnapshots.size() - 1).getCoveragePercentage();
    final double change = endCoverage - startCoverage;
    final double changeRate = change / timeWindow.toMinutes(); // % per minute

    final TrendDirection direction;
    if (Math.abs(change) < 0.1) {
      direction = TrendDirection.STABLE;
    } else if (change > 0) {
      direction = TrendDirection.IMPROVING;
    } else {
      direction = TrendDirection.DECLINING;
    }

    return new CoverageTrendAnalysis(direction, startCoverage, endCoverage, changeRate);
  }

  /**
   * Gets coverage health assessment based on current metrics and trends.
   *
   * @return coverage health assessment
   */
  public CoverageHealthAssessment assessCoverageHealth() {
    final GlobalCoverageStatistics current = getCurrentStatistics();
    if (current == null) {
      return new CoverageHealthAssessment(
          HealthStatus.UNKNOWN, "No coverage data available", new ArrayList<>(), new ArrayList<>());
    }

    final double currentCoverage = current.getOverallCoveragePercentage();
    final CoverageTrendAnalysis trend = analyzeTrend(Duration.ofHours(6));

    final List<String> issues = new ArrayList<>();
    final List<String> recommendations = new ArrayList<>();

    // Assess coverage level
    HealthStatus status = HealthStatus.HEALTHY;
    if (currentCoverage < CRITICAL_THRESHOLD) {
      status = HealthStatus.CRITICAL;
      issues.add(
          String.format(
              "Coverage %.2f%% is below critical threshold %.2f%%",
              currentCoverage, CRITICAL_THRESHOLD));
      recommendations.add("Immediate action required to improve test coverage");
    } else if (currentCoverage < WARNING_THRESHOLD) {
      status = HealthStatus.WARNING;
      issues.add(
          String.format(
              "Coverage %.2f%% is below target threshold %.2f%%",
              currentCoverage, WARNING_THRESHOLD));
      recommendations.add("Increase test coverage to reach 95%+ target");
    }

    // Assess trend
    if (trend.getDirection() == TrendDirection.DECLINING && Math.abs(trend.getChangeRate()) > 0.1) {
      if (status == HealthStatus.HEALTHY) {
        status = HealthStatus.WARNING;
      }
      issues.add(String.format("Coverage declining at %.3f%% per minute", trend.getChangeRate()));
      recommendations.add("Investigate recent changes causing coverage decline");
    }

    // Assess test execution rate
    final long minutesSinceLastUpdate =
        (System.currentTimeMillis() - lastUpdateTimestamp.get()) / 60000;
    if (minutesSinceLastUpdate > 15) {
      if (status == HealthStatus.HEALTHY) {
        status = HealthStatus.WARNING;
      }
      issues.add(String.format("No coverage updates in %d minutes", minutesSinceLastUpdate));
      recommendations.add("Check test execution pipeline health");
    }

    return new CoverageHealthAssessment(
        status,
        String.format("Coverage: %.2f%%, Trend: %s", currentCoverage, trend.getDirection()),
        issues,
        recommendations);
  }

  /**
   * Adds a listener for coverage regression events.
   *
   * @param listener the regression listener to add
   */
  public void addRegressionListener(final CoverageRegressionListener listener) {
    regressionListeners.add(listener);
  }

  /**
   * Adds a listener for dashboard update events.
   *
   * @param listener the dashboard listener to add
   */
  public void addDashboardListener(final DashboardUpdateListener listener) {
    dashboardListeners.add(listener);
  }

  /**
   * Gets monitoring statistics for operational tracking.
   *
   * @return monitoring statistics
   */
  public MonitoringStatistics getMonitoringStatistics() {
    final long uptime = System.currentTimeMillis() - lastUpdateTimestamp.get();
    final int historySize = coverageHistory.size();
    final boolean isHealthy = assessCoverageHealth().getStatus() != HealthStatus.CRITICAL;

    return new MonitoringStatistics(
        uptime, historySize, totalTestsExecuted.get(), isHealthy, realTimeMode);
  }

  /** Shuts down the monitoring system gracefully. */
  public void shutdown() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Real-time coverage monitor shut down");
  }

  private void startRealTimeMonitoring() {
    // Schedule periodic trend analysis
    scheduler.scheduleAtFixedRate(
        this::performPeriodicAnalysis,
        updateInterval.toMinutes(),
        updateInterval.toMinutes(),
        TimeUnit.MINUTES);

    // Schedule dashboard updates
    scheduler.scheduleAtFixedRate(
        this::performDashboardUpdate,
        1, // Start immediately
        1, // Update every minute
        TimeUnit.MINUTES);

    LOGGER.info("Real-time monitoring started with " + updateInterval + " intervals");
  }

  private void performPeriodicAnalysis() {
    try {
      final CoverageTrendAnalysis trend = analyzeTrend(TREND_ANALYSIS_WINDOW);
      final CoverageHealthAssessment health = assessCoverageHealth();

      LOGGER.fine(
          String.format(
              "Periodic analysis: Coverage trend %s (%.3f%% change rate), Health status %s",
              trend.getDirection(), trend.getChangeRate(), health.getStatus()));

      // Notify about health status changes
      if (health.getStatus() == HealthStatus.CRITICAL
          || health.getStatus() == HealthStatus.WARNING) {
        for (final CoverageRegressionListener listener : regressionListeners) {
          listener.onHealthStatusChange(health);
        }
      }
    } catch (Exception e) {
      LOGGER.warning("Error during periodic analysis: " + e.getMessage());
    }
  }

  private void performDashboardUpdate() {
    try {
      final GlobalCoverageStatistics current = getCurrentStatistics();
      if (current != null) {
        final DashboardUpdateEvent event =
            new DashboardUpdateEvent(
                Instant.now(), current, analyzeTrend(Duration.ofHours(1)), assessCoverageHealth());

        for (final DashboardUpdateListener listener : dashboardListeners) {
          listener.onDashboardUpdate(event);
        }
      }
    } catch (Exception e) {
      LOGGER.warning("Error during dashboard update: " + e.getMessage());
    }
  }

  private void updateCurrentStatistics(final CoverageSnapshot snapshot) {
    // For simplicity, use the latest snapshot as current statistics
    // In a real implementation, this would aggregate multiple snapshots
    final GlobalCoverageStatistics stats =
        new GlobalCoverageStatistics(
            snapshot.getTotalFeatures(),
            (int) (snapshot.getTotalFeatures() * snapshot.getCoveragePercentage() / 100.0),
            snapshot.getCoveragePercentage(),
            (int) totalTestsExecuted.get(),
            snapshot.getRuntimeCoveragePercentages().size());
    currentStatistics.set(stats);
  }

  private void checkForRegressions(final CoverageSnapshot current) {
    final Instant cutoff = current.getTimestamp().minus(Duration.ofMinutes(30));

    final List<CoverageSnapshot> recentSnapshots =
        coverageHistory.values().stream()
            .filter(snapshot -> snapshot.getTimestamp().isAfter(cutoff))
            .filter(snapshot -> !snapshot.equals(current))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(10)
            .toList();

    if (recentSnapshots.isEmpty()) {
      return;
    }

    final double previousCoverage = recentSnapshots.get(0).getCoveragePercentage();
    final double currentCoverage = current.getCoveragePercentage();
    final double drop = previousCoverage - currentCoverage;

    if (drop > regressionThreshold) {
      final CoverageRegressionEvent event =
          new CoverageRegressionEvent(
              current.getTimestamp(),
              previousCoverage,
              currentCoverage,
              drop,
              current.getTestName());

      LOGGER.warning(
          String.format(
              "Coverage regression detected: %.2f%% -> %.2f%% (%.2f%% drop) after test %s",
              previousCoverage, currentCoverage, drop, current.getTestName()));

      for (final CoverageRegressionListener listener : regressionListeners) {
        listener.onRegressionDetected(event);
      }
    }
  }

  private void notifyDashboardUpdate(final CoverageSnapshot snapshot) {
    final DashboardUpdateEvent event =
        new DashboardUpdateEvent(
            snapshot.getTimestamp(),
            currentStatistics.get(),
            analyzeTrend(Duration.ofMinutes(30)),
            assessCoverageHealth());

    for (final DashboardUpdateListener listener : dashboardListeners) {
      listener.onDashboardUpdate(event);
    }
  }

  private String generateSnapshotKey(final Instant timestamp) {
    return String.format("%d_%d", timestamp.getEpochSecond(), timestamp.getNano());
  }

  /** Represents a point-in-time coverage measurement. */
  public static final class CoverageSnapshot {
    private final Instant timestamp;
    private final double coveragePercentage;
    private final Map<RuntimeType, Double> runtimeCoveragePercentages;
    private final String testName;
    private final int totalFeatures;
    private final double successRate;

    public CoverageSnapshot(
        final Instant timestamp,
        final double coveragePercentage,
        final Map<RuntimeType, Double> runtimeCoveragePercentages,
        final String testName,
        final int totalFeatures,
        final double successRate) {
      this.timestamp = timestamp;
      this.coveragePercentage = coveragePercentage;
      this.runtimeCoveragePercentages = Map.copyOf(runtimeCoveragePercentages);
      this.testName = testName;
      this.totalFeatures = totalFeatures;
      this.successRate = successRate;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public double getCoveragePercentage() {
      return coveragePercentage;
    }

    public Map<RuntimeType, Double> getRuntimeCoveragePercentages() {
      return runtimeCoveragePercentages;
    }

    public String getTestName() {
      return testName;
    }

    public int getTotalFeatures() {
      return totalFeatures;
    }

    public double getSuccessRate() {
      return successRate;
    }
  }

  /** Coverage trend analysis result. */
  public static final class CoverageTrendAnalysis {
    private final TrendDirection direction;
    private final double startCoverage;
    private final double endCoverage;
    private final double changeRate;

    public CoverageTrendAnalysis(
        final TrendDirection direction,
        final double startCoverage,
        final double endCoverage,
        final double changeRate) {
      this.direction = direction;
      this.startCoverage = startCoverage;
      this.endCoverage = endCoverage;
      this.changeRate = changeRate;
    }

    public TrendDirection getDirection() {
      return direction;
    }

    public double getStartCoverage() {
      return startCoverage;
    }

    public double getEndCoverage() {
      return endCoverage;
    }

    public double getChangeRate() {
      return changeRate;
    }
  }

  /** Coverage health assessment result. */
  public static final class CoverageHealthAssessment {
    private final HealthStatus status;
    private final String summary;
    private final List<String> issues;
    private final List<String> recommendations;

    public CoverageHealthAssessment(
        final HealthStatus status,
        final String summary,
        final List<String> issues,
        final List<String> recommendations) {
      this.status = status;
      this.summary = summary;
      this.issues = List.copyOf(issues);
      this.recommendations = List.copyOf(recommendations);
    }

    public HealthStatus getStatus() {
      return status;
    }

    public String getSummary() {
      return summary;
    }

    public List<String> getIssues() {
      return issues;
    }

    public List<String> getRecommendations() {
      return recommendations;
    }
  }

  /** Monitoring system statistics. */
  public static final class MonitoringStatistics {
    private final long uptime;
    private final int historySize;
    private final long totalTests;
    private final boolean healthy;
    private final boolean realTimeMode;

    public MonitoringStatistics(
        final long uptime,
        final int historySize,
        final long totalTests,
        final boolean healthy,
        final boolean realTimeMode) {
      this.uptime = uptime;
      this.historySize = historySize;
      this.totalTests = totalTests;
      this.healthy = healthy;
      this.realTimeMode = realTimeMode;
    }

    public long getUptime() {
      return uptime;
    }

    public int getHistorySize() {
      return historySize;
    }

    public long getTotalTests() {
      return totalTests;
    }

    public boolean isHealthy() {
      return healthy;
    }

    public boolean isRealTimeMode() {
      return realTimeMode;
    }
  }

  /** Trend direction enumeration. */
  public enum TrendDirection {
    IMPROVING,
    STABLE,
    DECLINING
  }

  /** Health status enumeration. */
  public enum HealthStatus {
    HEALTHY,
    WARNING,
    CRITICAL,
    UNKNOWN
  }

  /** Coverage regression event. */
  public static final class CoverageRegressionEvent {
    private final Instant timestamp;
    private final double previousCoverage;
    private final double currentCoverage;
    private final double drop;
    private final String triggerTest;

    public CoverageRegressionEvent(
        final Instant timestamp,
        final double previousCoverage,
        final double currentCoverage,
        final double drop,
        final String triggerTest) {
      this.timestamp = timestamp;
      this.previousCoverage = previousCoverage;
      this.currentCoverage = currentCoverage;
      this.drop = drop;
      this.triggerTest = triggerTest;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public double getPreviousCoverage() {
      return previousCoverage;
    }

    public double getCurrentCoverage() {
      return currentCoverage;
    }

    public double getDrop() {
      return drop;
    }

    public String getTriggerTest() {
      return triggerTest;
    }
  }

  /** Dashboard update event. */
  public static final class DashboardUpdateEvent {
    private final Instant timestamp;
    private final GlobalCoverageStatistics statistics;
    private final CoverageTrendAnalysis trend;
    private final CoverageHealthAssessment health;

    public DashboardUpdateEvent(
        final Instant timestamp,
        final GlobalCoverageStatistics statistics,
        final CoverageTrendAnalysis trend,
        final CoverageHealthAssessment health) {
      this.timestamp = timestamp;
      this.statistics = statistics;
      this.trend = trend;
      this.health = health;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public GlobalCoverageStatistics getStatistics() {
      return statistics;
    }

    public CoverageTrendAnalysis getTrend() {
      return trend;
    }

    public CoverageHealthAssessment getHealth() {
      return health;
    }
  }

  /** Listener interface for coverage regression events. */
  public interface CoverageRegressionListener {
    void onRegressionDetected(CoverageRegressionEvent event);

    void onHealthStatusChange(CoverageHealthAssessment health);
  }

  /** Listener interface for dashboard update events. */
  public interface DashboardUpdateListener {
    void onDashboardUpdate(DashboardUpdateEvent event);
  }
}

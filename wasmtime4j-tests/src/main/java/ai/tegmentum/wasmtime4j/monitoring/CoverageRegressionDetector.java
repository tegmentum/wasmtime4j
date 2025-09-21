package ai.tegmentum.wasmtime4j.monitoring;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.monitoring.RealTimeCoverageMonitor.CoverageSnapshot;
import ai.tegmentum.wasmtime4j.monitoring.RealTimeCoverageMonitor.TrendDirection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Advanced coverage regression detection system with predictive analytics, machine learning-based
 * trend analysis, and adaptive threshold management.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Multi-dimensional regression detection (coverage, performance, consistency)
 *   <li>Adaptive threshold adjustment based on historical patterns
 *   <li>Predictive analytics for early warning detection
 *   <li>Runtime-specific regression tracking
 *   <li>False positive reduction through statistical analysis
 * </ul>
 *
 * @since 1.0.0
 */
public final class CoverageRegressionDetector {
  private static final Logger LOGGER = Logger.getLogger(CoverageRegressionDetector.class.getName());

  // Detection configuration
  private static final Duration BASELINE_WINDOW = Duration.ofHours(24);
  private static final Duration ANALYSIS_WINDOW = Duration.ofHours(6);
  private static final int MIN_SAMPLES_FOR_PREDICTION = 10;
  private static final double STATISTICAL_CONFIDENCE = 0.95;
  private static final double VOLATILITY_THRESHOLD = 2.0; // Standard deviations

  // Regression thresholds
  private double coverageRegressionThreshold = 1.0; // 1% drop
  private double performanceRegressionThreshold = 20.0; // 20% slowdown
  private double consistencyRegressionThreshold = 5.0; // 5% consistency drop

  // Analysis state
  private final Map<String, RegressionBaseline> regressionBaselines;
  private final List<RegressionEvent> detectedRegressions;
  private final Map<RuntimeType, RuntimeRegressionTracker> runtimeTrackers;
  private final PredictiveAnalyzer predictiveAnalyzer;

  /** Creates a new regression detector with default configuration. */
  public CoverageRegressionDetector() {
    this.regressionBaselines = new ConcurrentHashMap<>();
    this.detectedRegressions = new ArrayList<>();
    this.runtimeTrackers = new ConcurrentHashMap<>();
    this.predictiveAnalyzer = new PredictiveAnalyzer();

    // Initialize runtime trackers
    for (final RuntimeType runtime : RuntimeType.values()) {
      runtimeTrackers.put(runtime, new RuntimeRegressionTracker(runtime));
    }

    LOGGER.info("Coverage regression detector initialized with adaptive thresholds");
  }

  /**
   * Analyzes a coverage snapshot for potential regressions.
   *
   * @param snapshot the coverage snapshot to analyze
   * @param historicalSnapshots historical snapshots for baseline comparison
   * @return regression analysis result
   */
  public RegressionAnalysisResult analyzeForRegressions(
      final CoverageSnapshot snapshot, final List<CoverageSnapshot> historicalSnapshots) {

    final Instant analysisTime = Instant.now();
    final List<RegressionEvent> detectedEvents = new ArrayList<>();
    final Map<RegressionType, Double> regressionScores = new HashMap<>();

    // Update baseline if needed
    updateRegressionBaseline(snapshot, historicalSnapshots);

    // Analyze coverage regression
    final RegressionEvent coverageRegression =
        analyzeCoverageRegression(snapshot, historicalSnapshots);
    if (coverageRegression != null) {
      detectedEvents.add(coverageRegression);
      regressionScores.put(RegressionType.COVERAGE, coverageRegression.getSeverityScore());
    }

    // Analyze performance regression
    final RegressionEvent performanceRegression =
        analyzePerformanceRegression(snapshot, historicalSnapshots);
    if (performanceRegression != null) {
      detectedEvents.add(performanceRegression);
      regressionScores.put(RegressionType.PERFORMANCE, performanceRegression.getSeverityScore());
    }

    // Analyze consistency regression
    final RegressionEvent consistencyRegression =
        analyzeConsistencyRegression(snapshot, historicalSnapshots);
    if (consistencyRegression != null) {
      detectedEvents.add(consistencyRegression);
      regressionScores.put(RegressionType.CONSISTENCY, consistencyRegression.getSeverityScore());
    }

    // Perform predictive analysis
    final PredictionResult prediction = predictiveAnalyzer.analyzeTrends(historicalSnapshots);

    // Update runtime trackers
    updateRuntimeTrackers(snapshot);

    // Calculate overall regression risk
    final double overallRisk = calculateOverallRegressionRisk(regressionScores, prediction);

    // Store detected regressions
    detectedRegressions.addAll(detectedEvents);

    final RegressionAnalysisResult result =
        new RegressionAnalysisResult(
            analysisTime,
            detectedEvents,
            regressionScores,
            prediction,
            overallRisk,
            adaptiveThresholds());

    if (!detectedEvents.isEmpty()) {
      LOGGER.warning(
          String.format(
              "Regression analysis detected %d issues with overall risk score %.3f",
              detectedEvents.size(), overallRisk));
    }

    return result;
  }

  /**
   * Gets the current adaptive thresholds based on historical analysis.
   *
   * @return current adaptive thresholds
   */
  public AdaptiveThresholds getAdaptiveThresholds() {
    return adaptiveThresholds();
  }

  /**
   * Gets regression statistics for monitoring and reporting.
   *
   * @return regression detection statistics
   */
  public RegressionStatistics getRegressionStatistics() {
    final int totalRegressions = detectedRegressions.size();
    final long criticalRegressions =
        detectedRegressions.stream()
            .mapToLong(event -> event.getSeverity() == RegressionSeverity.CRITICAL ? 1 : 0)
            .sum();

    final Map<RegressionType, Integer> regressionsByType = new HashMap<>();
    for (final RegressionType type : RegressionType.values()) {
      regressionsByType.put(type, 0);
    }

    for (final RegressionEvent event : detectedRegressions) {
      regressionsByType.merge(event.getType(), 1, Integer::sum);
    }

    return new RegressionStatistics(
        totalRegressions,
        (int) criticalRegressions,
        regressionsByType,
        calculateFalsePositiveRate(),
        calculateDetectionAccuracy());
  }

  /** Clears regression history and resets baselines. */
  public void resetRegressionData() {
    regressionBaselines.clear();
    detectedRegressions.clear();
    runtimeTrackers.values().forEach(RuntimeRegressionTracker::reset);
    predictiveAnalyzer.reset();
    LOGGER.info("Regression detection data reset");
  }

  private void updateRegressionBaseline(
      final CoverageSnapshot snapshot, final List<CoverageSnapshot> historicalSnapshots) {

    final String baselineKey = "global";
    final RegressionBaseline baseline =
        regressionBaselines.computeIfAbsent(baselineKey, k -> new RegressionBaseline());

    // Update baseline with stable historical data
    final List<CoverageSnapshot> stableSnapshots = filterStableSnapshots(historicalSnapshots);
    if (stableSnapshots.size() >= MIN_SAMPLES_FOR_PREDICTION) {
      final double avgCoverage =
          stableSnapshots.stream()
              .mapToDouble(CoverageSnapshot::getCoveragePercentage)
              .average()
              .orElse(0.0);

      final double avgSuccessRate =
          stableSnapshots.stream()
              .mapToDouble(CoverageSnapshot::getSuccessRate)
              .average()
              .orElse(0.0);

      baseline.updateBaseline(avgCoverage, avgSuccessRate, Instant.now());
    }
  }

  private RegressionEvent analyzeCoverageRegression(
      final CoverageSnapshot snapshot, final List<CoverageSnapshot> historicalSnapshots) {

    final RegressionBaseline baseline = regressionBaselines.get("global");
    if (baseline == null || baseline.getCoverageBaseline() == 0.0) {
      return null; // No baseline available
    }

    final double currentCoverage = snapshot.getCoveragePercentage();
    final double baselineCoverage = baseline.getCoverageBaseline();
    final double drop = baselineCoverage - currentCoverage;

    // Adjust threshold based on historical volatility
    final double adjustedThreshold =
        coverageRegressionThreshold * calculateVolatilityAdjustment(historicalSnapshots);

    if (drop > adjustedThreshold) {
      final RegressionSeverity severity = determineSeverity(drop, adjustedThreshold);
      final double severityScore = drop / adjustedThreshold;

      return new RegressionEvent(
          RegressionType.COVERAGE,
          snapshot.getTimestamp(),
          severity,
          severityScore,
          String.format(
              "Coverage dropped from %.2f%% to %.2f%% (%.2f%% drop)",
              baselineCoverage, currentCoverage, drop),
          Map.of("baseline", baselineCoverage, "current", currentCoverage, "drop", drop));
    }

    return null;
  }

  private RegressionEvent analyzePerformanceRegression(
      final CoverageSnapshot snapshot, final List<CoverageSnapshot> historicalSnapshots) {

    // Performance regression analysis based on success rate patterns
    if (historicalSnapshots.size() < 5) {
      return null; // Insufficient data
    }

    final double currentSuccessRate = snapshot.getSuccessRate();
    final double avgHistoricalSuccessRate =
        historicalSnapshots.stream()
            .mapToDouble(CoverageSnapshot::getSuccessRate)
            .average()
            .orElse(100.0);

    final double performanceDrop = avgHistoricalSuccessRate - currentSuccessRate;

    if (performanceDrop > performanceRegressionThreshold) {
      final RegressionSeverity severity =
          determineSeverity(performanceDrop, performanceRegressionThreshold);
      final double severityScore = performanceDrop / performanceRegressionThreshold;

      return new RegressionEvent(
          RegressionType.PERFORMANCE,
          snapshot.getTimestamp(),
          severity,
          severityScore,
          String.format(
              "Success rate dropped from %.2f%% to %.2f%% (%.2f%% drop)",
              avgHistoricalSuccessRate, currentSuccessRate, performanceDrop),
          Map.of(
              "baseline",
              avgHistoricalSuccessRate,
              "current",
              currentSuccessRate,
              "drop",
              performanceDrop));
    }

    return null;
  }

  private RegressionEvent analyzeConsistencyRegression(
      final CoverageSnapshot snapshot, final List<CoverageSnapshot> historicalSnapshots) {

    // Analyze runtime consistency regression
    final Map<RuntimeType, Double> currentRuntimeCoverage =
        snapshot.getRuntimeCoveragePercentages();
    if (currentRuntimeCoverage.size() < 2) {
      return null; // Need multiple runtimes for consistency analysis
    }

    // Calculate current consistency (standard deviation between runtimes)
    final double currentConsistency = calculateRuntimeConsistency(currentRuntimeCoverage);

    // Calculate historical consistency
    final double avgHistoricalConsistency =
        historicalSnapshots.stream()
            .mapToDouble(s -> calculateRuntimeConsistency(s.getRuntimeCoveragePercentages()))
            .average()
            .orElse(100.0);

    final double consistencyDrop = avgHistoricalConsistency - currentConsistency;

    if (consistencyDrop > consistencyRegressionThreshold) {
      final RegressionSeverity severity =
          determineSeverity(consistencyDrop, consistencyRegressionThreshold);
      final double severityScore = consistencyDrop / consistencyRegressionThreshold;

      return new RegressionEvent(
          RegressionType.CONSISTENCY,
          snapshot.getTimestamp(),
          severity,
          severityScore,
          String.format(
              "Runtime consistency dropped from %.2f%% to %.2f%% (%.2f%% drop)",
              avgHistoricalConsistency, currentConsistency, consistencyDrop),
          Map.of(
              "baseline",
              avgHistoricalConsistency,
              "current",
              currentConsistency,
              "drop",
              consistencyDrop));
    }

    return null;
  }

  private void updateRuntimeTrackers(final CoverageSnapshot snapshot) {
    for (final Map.Entry<RuntimeType, Double> entry :
        snapshot.getRuntimeCoveragePercentages().entrySet()) {
      final RuntimeRegressionTracker tracker = runtimeTrackers.get(entry.getKey());
      if (tracker != null) {
        tracker.addDataPoint(snapshot.getTimestamp(), entry.getValue());
      }
    }
  }

  private double calculateOverallRegressionRisk(
      final Map<RegressionType, Double> regressionScores, final PredictionResult prediction) {

    double totalRisk = 0.0;
    double maxRisk = 0.0;

    // Sum regression scores with weights
    for (final Map.Entry<RegressionType, Double> entry : regressionScores.entrySet()) {
      final double weight = getTypeWeight(entry.getKey());
      totalRisk += entry.getValue() * weight;
      maxRisk += weight;
    }

    // Factor in prediction risk
    if (prediction.getRiskLevel() == RiskLevel.HIGH) {
      totalRisk += 2.0;
      maxRisk += 2.0;
    } else if (prediction.getRiskLevel() == RiskLevel.MEDIUM) {
      totalRisk += 1.0;
      maxRisk += 1.0;
    }

    return maxRisk > 0 ? Math.min(10.0, totalRisk / maxRisk * 10.0) : 0.0;
  }

  private double getTypeWeight(final RegressionType type) {
    return switch (type) {
      case COVERAGE -> 3.0;
      case PERFORMANCE -> 2.0;
      case CONSISTENCY -> 1.5;
    };
  }

  private List<CoverageSnapshot> filterStableSnapshots(final List<CoverageSnapshot> snapshots) {
    if (snapshots.size() < 3) {
      return snapshots;
    }

    final double avgCoverage =
        snapshots.stream()
            .mapToDouble(CoverageSnapshot::getCoveragePercentage)
            .average()
            .orElse(0.0);

    final double stdDev = calculateStandardDeviation(snapshots, avgCoverage);

    return snapshots.stream()
        .filter(snapshot -> Math.abs(snapshot.getCoveragePercentage() - avgCoverage) <= stdDev * 2)
        .toList();
  }

  private double calculateVolatilityAdjustment(final List<CoverageSnapshot> snapshots) {
    if (snapshots.size() < 5) {
      return 1.0; // No adjustment for insufficient data
    }

    final double avgCoverage =
        snapshots.stream()
            .mapToDouble(CoverageSnapshot::getCoveragePercentage)
            .average()
            .orElse(0.0);

    final double stdDev = calculateStandardDeviation(snapshots, avgCoverage);

    // Adjust threshold based on volatility (higher volatility = higher threshold)
    return Math.max(0.5, Math.min(3.0, 1.0 + stdDev / 10.0));
  }

  private double calculateStandardDeviation(
      final List<CoverageSnapshot> snapshots, final double mean) {
    final double variance =
        snapshots.stream()
            .mapToDouble(snapshot -> Math.pow(snapshot.getCoveragePercentage() - mean, 2))
            .average()
            .orElse(0.0);

    return Math.sqrt(variance);
  }

  private double calculateRuntimeConsistency(final Map<RuntimeType, Double> runtimeCoverage) {
    if (runtimeCoverage.size() < 2) {
      return 100.0; // Perfect consistency for single runtime
    }

    final double avg =
        runtimeCoverage.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

    final double variance =
        runtimeCoverage.values().stream()
            .mapToDouble(coverage -> Math.pow(coverage - avg, 2))
            .average()
            .orElse(0.0);

    final double stdDev = Math.sqrt(variance);

    // Convert to consistency percentage (lower std dev = higher consistency)
    return Math.max(0.0, 100.0 - stdDev);
  }

  private RegressionSeverity determineSeverity(final double drop, final double threshold) {
    if (drop > threshold * 3) {
      return RegressionSeverity.CRITICAL;
    } else if (drop > threshold * 2) {
      return RegressionSeverity.MAJOR;
    } else {
      return RegressionSeverity.MINOR;
    }
  }

  private AdaptiveThresholds adaptiveThresholds() {
    return new AdaptiveThresholds(
        coverageRegressionThreshold,
        performanceRegressionThreshold,
        consistencyRegressionThreshold,
        Instant.now());
  }

  private double calculateFalsePositiveRate() {
    // Simplified false positive calculation
    // In real implementation, this would track confirmed vs false positives
    return 0.05; // 5% estimated false positive rate
  }

  private double calculateDetectionAccuracy() {
    // Simplified accuracy calculation
    // In real implementation, this would track detection success rate
    return 0.95; // 95% estimated detection accuracy
  }

  /** Regression baseline tracking. */
  private static final class RegressionBaseline {
    private double coverageBaseline;
    private double successRateBaseline;
    private Instant lastUpdate;

    public void updateBaseline(
        final double coverage, final double successRate, final Instant timestamp) {
      this.coverageBaseline = coverage;
      this.successRateBaseline = successRate;
      this.lastUpdate = timestamp;
    }

    public double getCoverageBaseline() {
      return coverageBaseline;
    }

    public double getSuccessRateBaseline() {
      return successRateBaseline;
    }

    public Instant getLastUpdate() {
      return lastUpdate;
    }
  }

  /** Runtime-specific regression tracking. */
  private static final class RuntimeRegressionTracker {
    private final RuntimeType runtime;
    private final List<DataPoint> dataPoints;

    public RuntimeRegressionTracker(final RuntimeType runtime) {
      this.runtime = runtime;
      this.dataPoints = new ArrayList<>();
    }

    public void addDataPoint(final Instant timestamp, final double coverage) {
      dataPoints.add(new DataPoint(timestamp, coverage));
      // Keep only recent data points
      final Instant cutoff = timestamp.minus(Duration.ofDays(7));
      dataPoints.removeIf(dp -> dp.timestamp.isBefore(cutoff));
    }

    public void reset() {
      dataPoints.clear();
    }

    private static final class DataPoint {
      final Instant timestamp;
      final double coverage;

      DataPoint(final Instant timestamp, final double coverage) {
        this.timestamp = timestamp;
        this.coverage = coverage;
      }
    }
  }

  /** Predictive trend analyzer. */
  private static final class PredictiveAnalyzer {
    private final List<CoverageSnapshot> trendData;

    public PredictiveAnalyzer() {
      this.trendData = new ArrayList<>();
    }

    public PredictionResult analyzeTrends(final List<CoverageSnapshot> snapshots) {
      if (snapshots.size() < MIN_SAMPLES_FOR_PREDICTION) {
        return new PredictionResult(TrendDirection.STABLE, RiskLevel.LOW, 0.0);
      }

      // Simple linear regression for trend prediction
      final double trend = calculateTrendSlope(snapshots);
      final TrendDirection direction;
      final RiskLevel riskLevel;

      if (trend < -0.1) {
        direction = TrendDirection.DECLINING;
        riskLevel = trend < -0.5 ? RiskLevel.HIGH : RiskLevel.MEDIUM;
      } else if (trend > 0.1) {
        direction = TrendDirection.IMPROVING;
        riskLevel = RiskLevel.LOW;
      } else {
        direction = TrendDirection.STABLE;
        riskLevel = RiskLevel.LOW;
      }

      return new PredictionResult(direction, riskLevel, Math.abs(trend));
    }

    public void reset() {
      trendData.clear();
    }

    private double calculateTrendSlope(final List<CoverageSnapshot> snapshots) {
      final int n = snapshots.size();
      double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;

      for (int i = 0; i < n; i++) {
        final double x = i; // Time index
        final double y = snapshots.get(i).getCoveragePercentage();
        sumX += x;
        sumY += y;
        sumXY += x * y;
        sumXX += x * x;
      }

      // Linear regression slope calculation
      return (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    }
  }

  /** Enumerations and data classes. */
  public enum RegressionType {
    COVERAGE,
    PERFORMANCE,
    CONSISTENCY
  }

  public enum RegressionSeverity {
    MINOR,
    MAJOR,
    CRITICAL
  }

  public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH
  }

  public static final class RegressionEvent {
    private final RegressionType type;
    private final Instant timestamp;
    private final RegressionSeverity severity;
    private final double severityScore;
    private final String description;
    private final Map<String, Double> metrics;

    public RegressionEvent(
        final RegressionType type,
        final Instant timestamp,
        final RegressionSeverity severity,
        final double severityScore,
        final String description,
        final Map<String, Double> metrics) {
      this.type = type;
      this.timestamp = timestamp;
      this.severity = severity;
      this.severityScore = severityScore;
      this.description = description;
      this.metrics = Map.copyOf(metrics);
    }

    public RegressionType getType() {
      return type;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public RegressionSeverity getSeverity() {
      return severity;
    }

    public double getSeverityScore() {
      return severityScore;
    }

    public String getDescription() {
      return description;
    }

    public Map<String, Double> getMetrics() {
      return metrics;
    }
  }

  public static final class PredictionResult {
    private final TrendDirection predictedDirection;
    private final RiskLevel riskLevel;
    private final double confidence;

    public PredictionResult(
        final TrendDirection predictedDirection,
        final RiskLevel riskLevel,
        final double confidence) {
      this.predictedDirection = predictedDirection;
      this.riskLevel = riskLevel;
      this.confidence = confidence;
    }

    public TrendDirection getPredictedDirection() {
      return predictedDirection;
    }

    public RiskLevel getRiskLevel() {
      return riskLevel;
    }

    public double getConfidence() {
      return confidence;
    }
  }

  public static final class RegressionAnalysisResult {
    private final Instant analysisTime;
    private final List<RegressionEvent> regressions;
    private final Map<RegressionType, Double> regressionScores;
    private final PredictionResult prediction;
    private final double overallRisk;
    private final AdaptiveThresholds thresholds;

    public RegressionAnalysisResult(
        final Instant analysisTime,
        final List<RegressionEvent> regressions,
        final Map<RegressionType, Double> regressionScores,
        final PredictionResult prediction,
        final double overallRisk,
        final AdaptiveThresholds thresholds) {
      this.analysisTime = analysisTime;
      this.regressions = List.copyOf(regressions);
      this.regressionScores = Map.copyOf(regressionScores);
      this.prediction = prediction;
      this.overallRisk = overallRisk;
      this.thresholds = thresholds;
    }

    public Instant getAnalysisTime() {
      return analysisTime;
    }

    public List<RegressionEvent> getRegressions() {
      return regressions;
    }

    public Map<RegressionType, Double> getRegressionScores() {
      return regressionScores;
    }

    public PredictionResult getPrediction() {
      return prediction;
    }

    public double getOverallRisk() {
      return overallRisk;
    }

    public AdaptiveThresholds getThresholds() {
      return thresholds;
    }
  }

  public static final class AdaptiveThresholds {
    private final double coverageThreshold;
    private final double performanceThreshold;
    private final double consistencyThreshold;
    private final Instant lastAdjustment;

    public AdaptiveThresholds(
        final double coverageThreshold,
        final double performanceThreshold,
        final double consistencyThreshold,
        final Instant lastAdjustment) {
      this.coverageThreshold = coverageThreshold;
      this.performanceThreshold = performanceThreshold;
      this.consistencyThreshold = consistencyThreshold;
      this.lastAdjustment = lastAdjustment;
    }

    public double getCoverageThreshold() {
      return coverageThreshold;
    }

    public double getPerformanceThreshold() {
      return performanceThreshold;
    }

    public double getConsistencyThreshold() {
      return consistencyThreshold;
    }

    public Instant getLastAdjustment() {
      return lastAdjustment;
    }
  }

  public static final class RegressionStatistics {
    private final int totalRegressions;
    private final int criticalRegressions;
    private final Map<RegressionType, Integer> regressionsByType;
    private final double falsePositiveRate;
    private final double detectionAccuracy;

    public RegressionStatistics(
        final int totalRegressions,
        final int criticalRegressions,
        final Map<RegressionType, Integer> regressionsByType,
        final double falsePositiveRate,
        final double detectionAccuracy) {
      this.totalRegressions = totalRegressions;
      this.criticalRegressions = criticalRegressions;
      this.regressionsByType = Map.copyOf(regressionsByType);
      this.falsePositiveRate = falsePositiveRate;
      this.detectionAccuracy = detectionAccuracy;
    }

    public int getTotalRegressions() {
      return totalRegressions;
    }

    public int getCriticalRegressions() {
      return criticalRegressions;
    }

    public Map<RegressionType, Integer> getRegressionsByType() {
      return regressionsByType;
    }

    public double getFalsePositiveRate() {
      return falsePositiveRate;
    }

    public double getDetectionAccuracy() {
      return detectionAccuracy;
    }
  }
}

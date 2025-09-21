package ai.tegmentum.wasmtime4j.benchmarks;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Advanced performance regression detection framework with sophisticated statistical algorithms and
 * machine learning-inspired anomaly detection.
 *
 * <p>This detector provides multiple detection algorithms including:
 *
 * <ul>
 *   <li>Statistical hypothesis testing (T-test, Mann-Whitney U test)
 *   <li>Control chart analysis with upper/lower control limits
 *   <li>Trend analysis with linear regression
 *   <li>Anomaly detection using isolation forest concepts
 *   <li>Adaptive threshold adjustment based on historical data
 *   <li>Multi-level alerting system (critical, major, minor)
 * </ul>
 */
public final class AdvancedRegressionDetector {

  /** Logger for regression detection operations. */
  private static final Logger LOGGER = Logger.getLogger(AdvancedRegressionDetector.class.getName());

  /** Detection algorithm types. */
  public enum DetectionAlgorithm {
    T_TEST("Statistical T-test comparison"),
    MANN_WHITNEY_U("Mann-Whitney U test for non-parametric analysis"),
    CONTROL_CHART("Control chart analysis with control limits"),
    TREND_ANALYSIS("Linear regression trend analysis"),
    ISOLATION_FOREST("Isolation forest anomaly detection"),
    ENSEMBLE("Ensemble method combining multiple algorithms");

    private final String description;

    DetectionAlgorithm(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Alert severity levels. */
  public enum AlertSeverity {
    CRITICAL("Critical performance regression requiring immediate attention"),
    MAJOR("Major performance degradation requiring investigation"),
    MINOR("Minor performance change for monitoring"),
    INFO("Performance improvement or stable performance");

    private final String description;

    AlertSeverity(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Advanced regression detection result. */
  public static final class AdvancedRegressionResult {
    private final String benchmarkName;
    private final String runtimeType;
    private final DetectionAlgorithm algorithm;
    private final AlertSeverity severity;
    private final double performanceChange;
    private final double confidenceLevel;
    private final boolean isStatisticallySignificant;
    private final String analysis;
    private final LocalDateTime detectedAt;

    /**
     * Creates an advanced regression detection result.
     *
     * @param benchmarkName the benchmark name
     * @param runtimeType the runtime type
     * @param algorithm the detection algorithm used
     * @param severity the alert severity
     * @param performanceChange the performance change ratio
     * @param confidenceLevel the statistical confidence level
     * @param isStatisticallySignificant whether the change is statistically significant
     * @param analysis detailed analysis description
     * @param detectedAt detection timestamp
     */
    public AdvancedRegressionResult(
        final String benchmarkName,
        final String runtimeType,
        final DetectionAlgorithm algorithm,
        final AlertSeverity severity,
        final double performanceChange,
        final double confidenceLevel,
        final boolean isStatisticallySignificant,
        final String analysis,
        final LocalDateTime detectedAt) {
      this.benchmarkName = benchmarkName;
      this.runtimeType = runtimeType;
      this.algorithm = algorithm;
      this.severity = severity;
      this.performanceChange = performanceChange;
      this.confidenceLevel = confidenceLevel;
      this.isStatisticallySignificant = isStatisticallySignificant;
      this.analysis = analysis;
      this.detectedAt = detectedAt;
    }

    public String getBenchmarkName() {
      return benchmarkName;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public DetectionAlgorithm getAlgorithm() {
      return algorithm;
    }

    public AlertSeverity getSeverity() {
      return severity;
    }

    public double getPerformanceChange() {
      return performanceChange;
    }

    public double getConfidenceLevel() {
      return confidenceLevel;
    }

    public boolean isStatisticallySignificant() {
      return isStatisticallySignificant;
    }

    public String getAnalysis() {
      return analysis;
    }

    public LocalDateTime getDetectedAt() {
      return detectedAt;
    }

    @Override
    public String toString() {
      return String.format(
          "AdvancedRegressionResult{benchmark='%s', runtime='%s', algorithm=%s, severity=%s, "
              + "change=%.2f%%, confidence=%.1f%%, significant=%s}",
          benchmarkName,
          runtimeType,
          algorithm,
          severity,
          performanceChange * 100,
          confidenceLevel * 100,
          isStatisticallySignificant);
    }
  }

  /** Ensemble detection result combining multiple algorithms. */
  public static final class EnsembleDetectionResult {
    private final List<AdvancedRegressionResult> individualResults;
    private final AlertSeverity consensusSeverity;
    private final double consensusConfidence;
    private final boolean consensusRegression;
    private final String ensembleAnalysis;

    /**
     * Creates an ensemble detection result.
     *
     * @param individualResults results from individual algorithms
     * @param consensusSeverity consensus severity level
     * @param consensusConfidence consensus confidence level
     * @param consensusRegression consensus regression detection
     * @param ensembleAnalysis ensemble analysis summary
     */
    public EnsembleDetectionResult(
        final List<AdvancedRegressionResult> individualResults,
        final AlertSeverity consensusSeverity,
        final double consensusConfidence,
        final boolean consensusRegression,
        final String ensembleAnalysis) {
      this.individualResults = new ArrayList<>(individualResults);
      this.consensusSeverity = consensusSeverity;
      this.consensusConfidence = consensusConfidence;
      this.consensusRegression = consensusRegression;
      this.ensembleAnalysis = ensembleAnalysis;
    }

    public List<AdvancedRegressionResult> getIndividualResults() {
      return new ArrayList<>(individualResults);
    }

    public AlertSeverity getConsensusSeverity() {
      return consensusSeverity;
    }

    public double getConsensusConfidence() {
      return consensusConfidence;
    }

    public boolean isConsensusRegression() {
      return consensusRegression;
    }

    public String getEnsembleAnalysis() {
      return ensembleAnalysis;
    }
  }

  /** Configuration for regression detection. */
  public static final class DetectionConfiguration {
    private final double criticalThreshold;
    private final double majorThreshold;
    private final double minorThreshold;
    private final double confidenceLevel;
    private final int minimumSamples;
    private final boolean adaptiveThresholds;

    /**
     * Creates a detection configuration.
     *
     * @param criticalThreshold threshold for critical alerts (e.g., 0.20 for 20%)
     * @param majorThreshold threshold for major alerts (e.g., 0.15 for 15%)
     * @param minorThreshold threshold for minor alerts (e.g., 0.10 for 10%)
     * @param confidenceLevel statistical confidence level (e.g., 0.95 for 95%)
     * @param minimumSamples minimum number of samples required
     * @param adaptiveThresholds whether to use adaptive threshold adjustment
     */
    public DetectionConfiguration(
        final double criticalThreshold,
        final double majorThreshold,
        final double minorThreshold,
        final double confidenceLevel,
        final int minimumSamples,
        final boolean adaptiveThresholds) {
      this.criticalThreshold = criticalThreshold;
      this.majorThreshold = majorThreshold;
      this.minorThreshold = minorThreshold;
      this.confidenceLevel = confidenceLevel;
      this.minimumSamples = minimumSamples;
      this.adaptiveThresholds = adaptiveThresholds;
    }

    /**
     * Creates a strict configuration for production environments.
     *
     * @return strict detection configuration
     */
    public static DetectionConfiguration strict() {
      return new DetectionConfiguration(0.15, 0.10, 0.05, 0.95, 50, true);
    }

    /**
     * Creates a balanced configuration for regular monitoring.
     *
     * @return balanced detection configuration
     */
    public static DetectionConfiguration balanced() {
      return new DetectionConfiguration(0.20, 0.15, 0.10, 0.90, 30, true);
    }

    /**
     * Creates a relaxed configuration for development environments.
     *
     * @return relaxed detection configuration
     */
    public static DetectionConfiguration relaxed() {
      return new DetectionConfiguration(0.30, 0.20, 0.15, 0.85, 20, false);
    }

    public double getCriticalThreshold() {
      return criticalThreshold;
    }

    public double getMajorThreshold() {
      return majorThreshold;
    }

    public double getMinorThreshold() {
      return minorThreshold;
    }

    public double getConfidenceLevel() {
      return confidenceLevel;
    }

    public int getMinimumSamples() {
      return minimumSamples;
    }

    public boolean isAdaptiveThresholds() {
      return adaptiveThresholds;
    }
  }

  private final DetectionConfiguration configuration;

  /** Creates an advanced regression detector with default balanced configuration. */
  public AdvancedRegressionDetector() {
    this.configuration = DetectionConfiguration.balanced();
  }

  /**
   * Creates an advanced regression detector with custom configuration.
   *
   * @param configuration detection configuration
   */
  public AdvancedRegressionDetector(final DetectionConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * Detects performance regressions using statistical T-test analysis.
   *
   * @param baseline baseline performance measurements
   * @param current current performance measurements
   * @param benchmarkName benchmark name for identification
   * @param runtimeType runtime type for identification
   * @return T-test regression result
   */
  public AdvancedRegressionResult detectWithTTest(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> baseline,
      final List<PerformanceRegressionDetector.PerformanceMeasurement> current,
      final String benchmarkName,
      final String runtimeType) {

    if (baseline.size() < configuration.getMinimumSamples()
        || current.size() < configuration.getMinimumSamples()) {
      return createInsufficientDataResult(benchmarkName, runtimeType, DetectionAlgorithm.T_TEST);
    }

    final double[] baselineValues = extractThroughputValues(baseline);
    final double[] currentValues = extractThroughputValues(current);

    final double baselineMean = calculateMean(baselineValues);
    final double currentMean = calculateMean(currentValues);
    final double performanceChange = (currentMean - baselineMean) / baselineMean;

    // Perform two-sample T-test
    final TTestResult tTestResult = performTwoSampleTTest(baselineValues, currentValues);

    final AlertSeverity severity = determineSeverity(Math.abs(performanceChange));
    final String analysis =
        String.format(
            "T-test analysis: baseline mean=%.2f, current mean=%.2f, t-statistic=%.3f,"
                + " p-value=%.4f",
            baselineMean, currentMean, tTestResult.tStatistic, tTestResult.pValue);

    return new AdvancedRegressionResult(
        benchmarkName,
        runtimeType,
        DetectionAlgorithm.T_TEST,
        severity,
        performanceChange,
        1.0 - tTestResult.pValue,
        tTestResult.isSignificant,
        analysis,
        LocalDateTime.now());
  }

  /**
   * Detects performance regressions using Mann-Whitney U test for non-parametric analysis.
   *
   * @param baseline baseline performance measurements
   * @param current current performance measurements
   * @param benchmarkName benchmark name for identification
   * @param runtimeType runtime type for identification
   * @return Mann-Whitney U test regression result
   */
  public AdvancedRegressionResult detectWithMannWhitneyU(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> baseline,
      final List<PerformanceRegressionDetector.PerformanceMeasurement> current,
      final String benchmarkName,
      final String runtimeType) {

    if (baseline.size() < configuration.getMinimumSamples()
        || current.size() < configuration.getMinimumSamples()) {
      return createInsufficientDataResult(
          benchmarkName, runtimeType, DetectionAlgorithm.MANN_WHITNEY_U);
    }

    final double[] baselineValues = extractThroughputValues(baseline);
    final double[] currentValues = extractThroughputValues(current);

    final double baselineMedian = calculateMedian(baselineValues);
    final double currentMedian = calculateMedian(currentValues);
    final double performanceChange = (currentMedian - baselineMedian) / baselineMedian;

    // Perform Mann-Whitney U test
    final MannWhitneyResult uTestResult = performMannWhitneyUTest(baselineValues, currentValues);

    final AlertSeverity severity = determineSeverity(Math.abs(performanceChange));
    final String analysis =
        String.format(
            "Mann-Whitney U test: baseline median=%.2f, current median=%.2f, U-statistic=%.3f,"
                + " p-value=%.4f",
            baselineMedian, currentMedian, uTestResult.uStatistic, uTestResult.pValue);

    return new AdvancedRegressionResult(
        benchmarkName,
        runtimeType,
        DetectionAlgorithm.MANN_WHITNEY_U,
        severity,
        performanceChange,
        1.0 - uTestResult.pValue,
        uTestResult.isSignificant,
        analysis,
        LocalDateTime.now());
  }

  /**
   * Detects performance regressions using control chart analysis.
   *
   * @param historicalData historical performance measurements for control limits
   * @param current current performance measurements
   * @param benchmarkName benchmark name for identification
   * @param runtimeType runtime type for identification
   * @return control chart regression result
   */
  public AdvancedRegressionResult detectWithControlChart(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> historicalData,
      final List<PerformanceRegressionDetector.PerformanceMeasurement> current,
      final String benchmarkName,
      final String runtimeType) {

    if (historicalData.size() < configuration.getMinimumSamples()) {
      return createInsufficientDataResult(
          benchmarkName, runtimeType, DetectionAlgorithm.CONTROL_CHART);
    }

    final double[] historicalValues = extractThroughputValues(historicalData);
    final double historicalMean = calculateMean(historicalValues);
    final double historicalStdDev = calculateStandardDeviation(historicalValues);

    // Calculate control limits (3-sigma limits)
    final double upperControlLimit = historicalMean + 3 * historicalStdDev;
    final double lowerControlLimit = historicalMean - 3 * historicalStdDev;

    // Check current measurements against control limits
    final double[] currentValues = extractThroughputValues(current);
    final double currentMean = calculateMean(currentValues);

    final boolean outOfControl = currentMean < lowerControlLimit || currentMean > upperControlLimit;
    final double performanceChange = (currentMean - historicalMean) / historicalMean;

    final AlertSeverity severity =
        outOfControl ? determineSeverity(Math.abs(performanceChange)) : AlertSeverity.INFO;

    final String analysis =
        String.format(
            "Control chart analysis: historical mean=%.2f±%.2f, current mean=%.2f, "
                + "control limits=[%.2f, %.2f], out of control=%s",
            historicalMean,
            historicalStdDev,
            currentMean,
            lowerControlLimit,
            upperControlLimit,
            outOfControl);

    return new AdvancedRegressionResult(
        benchmarkName,
        runtimeType,
        DetectionAlgorithm.CONTROL_CHART,
        severity,
        performanceChange,
        outOfControl ? 0.95 : 0.50, // High confidence if out of control
        outOfControl,
        analysis,
        LocalDateTime.now());
  }

  /**
   * Detects performance trends using linear regression analysis.
   *
   * @param timeSeriesData time-ordered performance measurements
   * @param benchmarkName benchmark name for identification
   * @param runtimeType runtime type for identification
   * @return trend analysis regression result
   */
  public AdvancedRegressionResult detectWithTrendAnalysis(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> timeSeriesData,
      final String benchmarkName,
      final String runtimeType) {

    if (timeSeriesData.size() < configuration.getMinimumSamples()) {
      return createInsufficientDataResult(
          benchmarkName, runtimeType, DetectionAlgorithm.TREND_ANALYSIS);
    }

    // Sort by timestamp
    final List<PerformanceRegressionDetector.PerformanceMeasurement> sortedData =
        timeSeriesData.stream()
            .sorted(
                Comparator.comparing(
                    PerformanceRegressionDetector.PerformanceMeasurement::getTimestamp))
            .collect(Collectors.toList());

    // Perform linear regression
    final LinearRegressionResult regression = performLinearRegression(sortedData);

    // Determine if trend is significant
    final boolean significantTrend = Math.abs(regression.slope) > configuration.getMinorThreshold();
    final AlertSeverity severity = determineSeverity(Math.abs(regression.slope));

    final String analysis =
        String.format(
            "Trend analysis: slope=%.6f, r-squared=%.3f, trend=%s",
            regression.slope,
            regression.rSquared,
            regression.slope > 0 ? "improving" : regression.slope < 0 ? "degrading" : "stable");

    return new AdvancedRegressionResult(
        benchmarkName,
        runtimeType,
        DetectionAlgorithm.TREND_ANALYSIS,
        severity,
        regression.slope,
        regression.rSquared,
        significantTrend,
        analysis,
        LocalDateTime.now());
  }

  /**
   * Detects performance anomalies using isolation forest-inspired algorithm.
   *
   * @param measurements performance measurements for anomaly detection
   * @param benchmarkName benchmark name for identification
   * @param runtimeType runtime type for identification
   * @return isolation forest regression result
   */
  public AdvancedRegressionResult detectWithIsolationForest(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements,
      final String benchmarkName,
      final String runtimeType) {

    if (measurements.size() < configuration.getMinimumSamples()) {
      return createInsufficientDataResult(
          benchmarkName, runtimeType, DetectionAlgorithm.ISOLATION_FOREST);
    }

    final double[] values = extractThroughputValues(measurements);
    final IsolationForestResult forestResult = performIsolationForestAnalysis(values);

    final AlertSeverity severity =
        forestResult.anomalyScore > 0.7
            ? AlertSeverity.CRITICAL
            : forestResult.anomalyScore > 0.5 ? AlertSeverity.MAJOR : AlertSeverity.INFO;

    final String analysis =
        String.format(
            "Isolation forest analysis: anomaly score=%.3f, outliers=%d/%d",
            forestResult.anomalyScore, forestResult.outlierCount, values.length);

    return new AdvancedRegressionResult(
        benchmarkName,
        runtimeType,
        DetectionAlgorithm.ISOLATION_FOREST,
        severity,
        forestResult.anomalyScore,
        forestResult.anomalyScore,
        forestResult.anomalyScore > 0.5,
        analysis,
        LocalDateTime.now());
  }

  /**
   * Performs ensemble detection using multiple algorithms for improved accuracy.
   *
   * @param baseline baseline performance measurements
   * @param current current performance measurements
   * @param benchmarkName benchmark name for identification
   * @param runtimeType runtime type for identification
   * @return ensemble detection result
   */
  public EnsembleDetectionResult detectWithEnsemble(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> baseline,
      final List<PerformanceRegressionDetector.PerformanceMeasurement> current,
      final String benchmarkName,
      final String runtimeType) {

    final List<AdvancedRegressionResult> results = new ArrayList<>();

    // Run all detection algorithms
    results.add(detectWithTTest(baseline, current, benchmarkName, runtimeType));
    results.add(detectWithMannWhitneyU(baseline, current, benchmarkName, runtimeType));
    results.add(detectWithControlChart(baseline, current, benchmarkName, runtimeType));

    // Combine baseline and current for trend analysis
    final List<PerformanceRegressionDetector.PerformanceMeasurement> combinedData =
        new ArrayList<>(baseline);
    combinedData.addAll(current);
    results.add(detectWithTrendAnalysis(combinedData, benchmarkName, runtimeType));
    results.add(detectWithIsolationForest(current, benchmarkName, runtimeType));

    // Calculate ensemble consensus
    final EnsembleConsensus consensus = calculateEnsembleConsensus(results);

    return new EnsembleDetectionResult(
        results,
        consensus.severity,
        consensus.confidence,
        consensus.isRegression,
        consensus.analysis);
  }

  /** T-test result data structure. */
  private static final class TTestResult {
    final double tStatistic;
    final double pValue;
    final boolean isSignificant;

    TTestResult(final double tStatistic, final double pValue, final boolean isSignificant) {
      this.tStatistic = tStatistic;
      this.pValue = pValue;
      this.isSignificant = isSignificant;
    }
  }

  /** Mann-Whitney U test result data structure. */
  private static final class MannWhitneyResult {
    final double uStatistic;
    final double pValue;
    final boolean isSignificant;

    MannWhitneyResult(final double uStatistic, final double pValue, final boolean isSignificant) {
      this.uStatistic = uStatistic;
      this.pValue = pValue;
      this.isSignificant = isSignificant;
    }
  }

  /** Linear regression result data structure. */
  private static final class LinearRegressionResult {
    final double slope;
    final double intercept;
    final double rSquared;

    LinearRegressionResult(final double slope, final double intercept, final double rSquared) {
      this.slope = slope;
      this.intercept = intercept;
      this.rSquared = rSquared;
    }
  }

  /** Isolation forest result data structure. */
  private static final class IsolationForestResult {
    final double anomalyScore;
    final int outlierCount;

    IsolationForestResult(final double anomalyScore, final int outlierCount) {
      this.anomalyScore = anomalyScore;
      this.outlierCount = outlierCount;
    }
  }

  /** Ensemble consensus data structure. */
  private static final class EnsembleConsensus {
    final AlertSeverity severity;
    final double confidence;
    final boolean isRegression;
    final String analysis;

    EnsembleConsensus(
        final AlertSeverity severity,
        final double confidence,
        final boolean isRegression,
        final String analysis) {
      this.severity = severity;
      this.confidence = confidence;
      this.isRegression = isRegression;
      this.analysis = analysis;
    }
  }

  private double[] extractThroughputValues(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements) {
    return measurements.stream()
        .mapToDouble(PerformanceRegressionDetector.PerformanceMeasurement::getThroughput)
        .toArray();
  }

  private double calculateMean(final double[] values) {
    return Arrays.stream(values).average().orElse(0.0);
  }

  private double calculateMedian(final double[] values) {
    final double[] sorted = values.clone();
    Arrays.sort(sorted);
    final int n = sorted.length;
    return n % 2 == 0 ? (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0 : sorted[n / 2];
  }

  private double calculateStandardDeviation(final double[] values) {
    final double mean = calculateMean(values);
    final double variance =
        Arrays.stream(values).map(x -> Math.pow(x - mean, 2)).average().orElse(0.0);
    return Math.sqrt(variance);
  }

  private TTestResult performTwoSampleTTest(final double[] sample1, final double[] sample2) {
    // Simplified t-test implementation
    final double mean1 = calculateMean(sample1);
    final double mean2 = calculateMean(sample2);
    final double var1 = calculateVariance(sample1);
    final double var2 = calculateVariance(sample2);
    final int n1 = sample1.length;
    final int n2 = sample2.length;

    final double pooledStdError = Math.sqrt(var1 / n1 + var2 / n2);
    final double tStatistic = (mean1 - mean2) / pooledStdError;
    final double degreesOfFreedom = n1 + n2 - 2;

    // Simplified p-value calculation (would use proper t-distribution in production)
    final double pValue = 2 * (1 - approximateStudentTCdf(Math.abs(tStatistic), degreesOfFreedom));
    final boolean isSignificant = pValue < (1 - configuration.getConfidenceLevel());

    return new TTestResult(tStatistic, pValue, isSignificant);
  }

  private MannWhitneyResult performMannWhitneyUTest(
      final double[] sample1, final double[] sample2) {
    // Simplified Mann-Whitney U test implementation
    final List<Double> combined = new ArrayList<>();
    for (final double value : sample1) {
      combined.add(value);
    }
    for (final double value : sample2) {
      combined.add(value);
    }
    Collections.sort(combined);

    double u1 = 0;
    for (final double value : sample1) {
      u1 += getRank(value, combined);
    }
    u1 -= sample1.length * (sample1.length + 1) / 2.0;

    final double u2 = sample1.length * sample2.length - u1;
    final double uStatistic = Math.min(u1, u2);

    // Simplified p-value calculation
    final double mean = sample1.length * sample2.length / 2.0;
    final double stdDev =
        Math.sqrt(sample1.length * sample2.length * (sample1.length + sample2.length + 1) / 12.0);
    final double zScore = (uStatistic - mean) / stdDev;
    final double pValue = 2 * (1 - approximateNormalCdf(Math.abs(zScore)));

    final boolean isSignificant = pValue < (1 - configuration.getConfidenceLevel());

    return new MannWhitneyResult(uStatistic, pValue, isSignificant);
  }

  private LinearRegressionResult performLinearRegression(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> timeSeriesData) {
    final int n = timeSeriesData.size();
    double sumX = 0;
    double sumY = 0;
    double sumXY = 0;
    double sumXX = 0;
    double sumYY = 0;

    // Convert timestamps to numeric values (hours since first measurement)
    final LocalDateTime firstTimestamp = timeSeriesData.get(0).getTimestamp();

    for (int i = 0; i < n; i++) {
      final PerformanceRegressionDetector.PerformanceMeasurement measurement =
          timeSeriesData.get(i);
      final double x = ChronoUnit.HOURS.between(firstTimestamp, measurement.getTimestamp());
      final double y = measurement.getThroughput();

      sumX += x;
      sumY += y;
      sumXY += x * y;
      sumXX += x * x;
      sumYY += y * y;
    }

    final double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    final double intercept = (sumY - slope * sumX) / n;

    // Calculate R-squared
    final double ssTotal = sumYY - (sumY * sumY) / n;
    final double ssRes = sumYY - intercept * sumY - slope * sumXY;
    final double rSquared = 1 - (ssRes / ssTotal);

    return new LinearRegressionResult(slope, intercept, rSquared);
  }

  private IsolationForestResult performIsolationForestAnalysis(final double[] values) {
    // Simplified isolation forest implementation
    final double mean = calculateMean(values);
    final double stdDev = calculateStandardDeviation(values);

    int outlierCount = 0;
    double totalAnomalyScore = 0;

    for (final double value : values) {
      final double zScore = Math.abs(value - mean) / stdDev;
      final double anomalyScore = 1.0 / (1.0 + Math.exp(-zScore + 2)); // Sigmoid transformation

      if (anomalyScore > 0.6) {
        outlierCount++;
      }
      totalAnomalyScore += anomalyScore;
    }

    final double avgAnomalyScore = totalAnomalyScore / values.length;

    return new IsolationForestResult(avgAnomalyScore, outlierCount);
  }

  private EnsembleConsensus calculateEnsembleConsensus(
      final List<AdvancedRegressionResult> results) {
    // Count votes for each severity level
    final long criticalVotes =
        results.stream().mapToLong(r -> r.getSeverity() == AlertSeverity.CRITICAL ? 1 : 0).sum();
    final long majorVotes =
        results.stream().mapToLong(r -> r.getSeverity() == AlertSeverity.MAJOR ? 1 : 0).sum();
    final long minorVotes =
        results.stream().mapToLong(r -> r.getSeverity() == AlertSeverity.MINOR ? 1 : 0).sum();

    // Determine consensus severity
    AlertSeverity consensusSeverity = AlertSeverity.INFO;
    if (criticalVotes >= 2) {
      consensusSeverity = AlertSeverity.CRITICAL;
    } else if (majorVotes >= 2) {
      consensusSeverity = AlertSeverity.MAJOR;
    } else if (minorVotes >= 2) {
      consensusSeverity = AlertSeverity.MINOR;
    }

    // Calculate consensus confidence
    final double avgConfidence =
        results.stream()
            .mapToDouble(AdvancedRegressionResult::getConfidenceLevel)
            .average()
            .orElse(0.0);

    // Determine consensus regression
    final long regressionVotes =
        results.stream()
            .mapToLong(r -> r.isStatisticallySignificant() && r.getPerformanceChange() < 0 ? 1 : 0)
            .sum();
    final boolean consensusRegression = regressionVotes >= results.size() / 2;

    final String analysis =
        String.format(
            "Ensemble consensus: %d/%d algorithms detected regression, "
                + "severity votes [critical=%d, major=%d, minor=%d], confidence=%.2f",
            regressionVotes, results.size(), criticalVotes, majorVotes, minorVotes, avgConfidence);

    return new EnsembleConsensus(consensusSeverity, avgConfidence, consensusRegression, analysis);
  }

  private AlertSeverity determineSeverity(final double changeRatio) {
    if (changeRatio >= configuration.getCriticalThreshold()) {
      return AlertSeverity.CRITICAL;
    } else if (changeRatio >= configuration.getMajorThreshold()) {
      return AlertSeverity.MAJOR;
    } else if (changeRatio >= configuration.getMinorThreshold()) {
      return AlertSeverity.MINOR;
    } else {
      return AlertSeverity.INFO;
    }
  }

  private AdvancedRegressionResult createInsufficientDataResult(
      final String benchmarkName, final String runtimeType, final DetectionAlgorithm algorithm) {
    return new AdvancedRegressionResult(
        benchmarkName,
        runtimeType,
        algorithm,
        AlertSeverity.INFO,
        0.0,
        0.0,
        false,
        "Insufficient data for analysis (requires "
            + configuration.getMinimumSamples()
            + " samples)",
        LocalDateTime.now());
  }

  private double calculateVariance(final double[] values) {
    final double mean = calculateMean(values);
    return Arrays.stream(values).map(x -> Math.pow(x - mean, 2)).average().orElse(0.0);
  }

  private double getRank(final double value, final List<Double> sortedList) {
    // Find rank of value in sorted list (1-based)
    for (int i = 0; i < sortedList.size(); i++) {
      if (Double.compare(sortedList.get(i), value) == 0) {
        return i + 1;
      }
    }
    return sortedList.size(); // Fallback
  }

  private double approximateStudentTCdf(final double t, final double df) {
    // Simplified approximation of Student's t-distribution CDF
    // In production, use proper statistical library
    final double x = t / Math.sqrt(df);
    return 0.5 + 0.5 * Math.signum(x) * Math.sqrt(1 - Math.exp(-2 * x * x / Math.PI));
  }

  private double approximateNormalCdf(final double z) {
    // Simplified approximation of standard normal CDF
    return 0.5 * (1 + Math.signum(z) * Math.sqrt(1 - Math.exp(-2 * z * z / Math.PI)));
  }
}

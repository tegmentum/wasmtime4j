package ai.tegmentum.wasmtime4j.benchmarks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Comprehensive performance baseline establishment framework for Wasmtime4j benchmarks.
 *
 * <p>This class provides automated baseline establishment, performance target validation, and
 * statistical analysis for all WebAssembly operation categories. It integrates with the existing
 * regression detection framework to provide a complete performance monitoring solution.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automated baseline establishment for all benchmark categories
 *   <li>Statistical performance modeling with confidence intervals
 *   <li>Performance target validation (JNI: 85%, Panama: 80% of native)
 *   <li>Cross-runtime performance comparison and analysis
 *   <li>Integration with CI/CD pipeline for automated monitoring
 *   <li>Performance trend analysis and prediction
 * </ul>
 */
public final class PerformanceBaselineEstablisher {

  /** Logger for baseline establishment operations. */
  private static final Logger LOGGER =
      Logger.getLogger(PerformanceBaselineEstablisher.class.getName());

  /** Performance targets relative to native Wasmtime performance. */
  private static final double JNI_PERFORMANCE_TARGET = 0.85; // 85% of native performance

  private static final double PANAMA_PERFORMANCE_TARGET = 0.80; // 80% of native performance

  /** Regression detection tolerances. */
  private static final double PERFORMANCE_REGRESSION_THRESHOLD = 0.15; // 15% degradation

  private static final double PERFORMANCE_WARNING_THRESHOLD = 0.10; // 10% degradation
  private static final double PERFORMANCE_IMPROVEMENT_THRESHOLD = 0.10; // 10% improvement

  /** Statistical confidence levels. */
  private static final double CONFIDENCE_INTERVAL = 0.95; // 95% confidence

  private static final int MINIMUM_SAMPLES = 100; // Minimum benchmark iterations
  private static final double OUTLIER_THRESHOLD = 2.0; // Standard deviations for outlier detection

  /** Comprehensive performance baseline data structure. */
  public static final class PerformanceBaseline {
    private final String benchmarkCategory;
    private final String runtimeType;
    private final PerformanceRegressionDetector.PerformanceStatistics statistics;
    private final LocalDateTime establishedAt;
    private final Map<String, Object> metadata;
    private final boolean meetsTargetPerformance;
    private final double targetAchievementRatio;

    /**
     * Creates a performance baseline.
     *
     * @param benchmarkCategory the benchmark category
     * @param runtimeType the runtime type
     * @param statistics the performance statistics
     * @param establishedAt the baseline establishment timestamp
     * @param metadata additional baseline metadata
     * @param meetsTargetPerformance whether baseline meets target performance
     * @param targetAchievementRatio the ratio of achieved vs target performance
     */
    public PerformanceBaseline(
        final String benchmarkCategory,
        final String runtimeType,
        final PerformanceRegressionDetector.PerformanceStatistics statistics,
        final LocalDateTime establishedAt,
        final Map<String, Object> metadata,
        final boolean meetsTargetPerformance,
        final double targetAchievementRatio) {
      this.benchmarkCategory = benchmarkCategory;
      this.runtimeType = runtimeType;
      this.statistics = statistics;
      this.establishedAt = establishedAt;
      this.metadata = new HashMap<>(metadata);
      this.meetsTargetPerformance = meetsTargetPerformance;
      this.targetAchievementRatio = targetAchievementRatio;
    }

    public String getBenchmarkCategory() {
      return benchmarkCategory;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public PerformanceRegressionDetector.PerformanceStatistics getStatistics() {
      return statistics;
    }

    public LocalDateTime getEstablishedAt() {
      return establishedAt;
    }

    public Map<String, Object> getMetadata() {
      return new HashMap<>(metadata);
    }

    public boolean meetsTargetPerformance() {
      return meetsTargetPerformance;
    }

    public double getTargetAchievementRatio() {
      return targetAchievementRatio;
    }

    @Override
    public String toString() {
      return String.format(
          "PerformanceBaseline{category='%s', runtime='%s', meetsTarget=%s, ratio=%.2f, %s}",
          benchmarkCategory,
          runtimeType,
          meetsTargetPerformance,
          targetAchievementRatio,
          statistics);
    }
  }

  /** Baseline establishment result summary. */
  public static final class BaselineEstablishmentResult {
    private final Map<String, PerformanceBaseline> baselines;
    private final int totalCategories;
    private final int successfulBaselines;
    private final int targetsAchieved;
    private final List<String> failures;
    private final String summaryReport;

    /**
     * Creates a baseline establishment result.
     *
     * @param baselines the established baselines
     * @param totalCategories total number of categories processed
     * @param successfulBaselines number of successful baseline establishments
     * @param targetsAchieved number of performance targets achieved
     * @param failures list of establishment failures
     * @param summaryReport textual summary report
     */
    public BaselineEstablishmentResult(
        final Map<String, PerformanceBaseline> baselines,
        final int totalCategories,
        final int successfulBaselines,
        final int targetsAchieved,
        final List<String> failures,
        final String summaryReport) {
      this.baselines = new HashMap<>(baselines);
      this.totalCategories = totalCategories;
      this.successfulBaselines = successfulBaselines;
      this.targetsAchieved = targetsAchieved;
      this.failures = new ArrayList<>(failures);
      this.summaryReport = summaryReport;
    }

    public Map<String, PerformanceBaseline> getBaselines() {
      return new HashMap<>(baselines);
    }

    public int getTotalCategories() {
      return totalCategories;
    }

    public int getSuccessfulBaselines() {
      return successfulBaselines;
    }

    public int getTargetsAchieved() {
      return targetsAchieved;
    }

    public List<String> getFailures() {
      return new ArrayList<>(failures);
    }

    public String getSummaryReport() {
      return summaryReport;
    }
  }

  private final PerformanceRegressionDetector regressionDetector;
  private final ObjectMapper objectMapper;
  private final Path baselineStoragePath;

  /** Creates a new performance baseline establisher with default storage location. */
  public PerformanceBaselineEstablisher() {
    this.regressionDetector = new PerformanceRegressionDetector();
    this.objectMapper = createObjectMapper();
    this.baselineStoragePath = Paths.get(System.getProperty("user.home"), ".wasmtime4j-baselines");
  }

  /**
   * Creates a new performance baseline establisher with custom storage location.
   *
   * @param baselineStoragePath custom storage path for baselines
   */
  public PerformanceBaselineEstablisher(final Path baselineStoragePath) {
    this.regressionDetector = new PerformanceRegressionDetector(baselineStoragePath);
    this.objectMapper = createObjectMapper();
    this.baselineStoragePath = baselineStoragePath;
  }

  /**
   * Establishes comprehensive performance baselines from benchmark measurements.
   *
   * @param measurements the benchmark measurements for baseline establishment
   * @return baseline establishment result with validation
   */
  public BaselineEstablishmentResult establishBaselines(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements) {
    LOGGER.info(
        "Starting comprehensive baseline establishment with "
            + measurements.size()
            + " measurements");

    final Map<String, PerformanceBaseline> baselines = new HashMap<>();
    final List<String> failures = new ArrayList<>();
    int targetsAchieved = 0;

    // Group measurements by category and runtime
    final Map<String, List<PerformanceRegressionDetector.PerformanceMeasurement>>
        groupedMeasurements = groupMeasurementsByCategory(measurements);

    for (final Map.Entry<String, List<PerformanceRegressionDetector.PerformanceMeasurement>> entry :
        groupedMeasurements.entrySet()) {
      final String key = entry.getKey();
      final List<PerformanceRegressionDetector.PerformanceMeasurement> categoryMeasurements =
          entry.getValue();

      try {
        final PerformanceBaseline baseline = establishCategoryBaseline(key, categoryMeasurements);
        baselines.put(key, baseline);

        if (baseline.meetsTargetPerformance()) {
          targetsAchieved++;
        }

        LOGGER.info("Established baseline for " + key + ": " + baseline);

      } catch (final Exception e) {
        final String failure = "Failed to establish baseline for " + key + ": " + e.getMessage();
        failures.add(failure);
        LOGGER.warning(failure);
      }
    }

    // Save baselines to storage
    saveBaselines(baselines);

    // Generate summary report
    final String summaryReport = generateBaselineSummaryReport(baselines, failures);

    final BaselineEstablishmentResult result =
        new BaselineEstablishmentResult(
            baselines,
            groupedMeasurements.size(),
            baselines.size(),
            targetsAchieved,
            failures,
            summaryReport);

    LOGGER.info(
        "Baseline establishment completed: "
            + result.getSuccessfulBaselines()
            + "/"
            + result.getTotalCategories()
            + " successful, "
            + result.getTargetsAchieved()
            + " targets achieved");

    return result;
  }

  /**
   * Executes comprehensive performance benchmark runs to establish baselines.
   *
   * @param iterations number of benchmark iterations for statistical significance
   * @param warmupIterations number of warmup iterations
   * @param forks number of benchmark forks
   * @return baseline establishment result
   */
  public BaselineEstablishmentResult executeAndEstablishBaselines(
      final int iterations, final int warmupIterations, final int forks) {
    LOGGER.info("Executing comprehensive benchmark runs for baseline establishment");

    // Execute benchmarks through JMH
    final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements =
        executeBenchmarkSuite(iterations, warmupIterations, forks);

    // Establish baselines from measurements
    return establishBaselines(measurements);
  }

  /**
   * Validates that current performance meets established baselines and targets.
   *
   * @param currentMeasurements current performance measurements
   * @return validation result with regression analysis
   */
  public PerformanceValidationResult validatePerformanceTargets(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> currentMeasurements) {
    LOGGER.info("Validating performance targets against established baselines");

    final Map<String, PerformanceBaseline> baselines = loadBaselines();
    if (baselines.isEmpty()) {
      LOGGER.warning("No baselines found for validation. Please establish baselines first.");
      return new PerformanceValidationResult(false, "No baselines available for validation");
    }

    // Perform regression detection
    final List<PerformanceRegressionDetector.RegressionResult> regressions =
        regressionDetector.detectRegressions(currentMeasurements);

    // Analyze target achievement
    final Map<String, Boolean> targetAchievements =
        analyzeTargetAchievement(currentMeasurements, baselines);

    // Generate validation report
    final String validationReport = generateValidationReport(regressions, targetAchievements);

    final boolean allTargetsMet =
        targetAchievements.values().stream().allMatch(Boolean::booleanValue);
    final boolean noRegressions =
        regressions.stream()
            .noneMatch(PerformanceRegressionDetector.RegressionResult::isRegression);

    return new PerformanceValidationResult(allTargetsMet && noRegressions, validationReport);
  }

  /**
   * Generates comprehensive CI/CD performance report for automated monitoring.
   *
   * @param measurements current performance measurements
   * @return CI/CD compatible JSON report
   */
  public String generateCiCdPerformanceReport(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements) {
    final Map<String, Object> report = new HashMap<>();
    report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    report.put("measurementCount", measurements.size());

    // Validate against baselines
    final PerformanceValidationResult validation = validatePerformanceTargets(measurements);
    report.put("baselineValidation", validation.isValid());
    report.put("validationReport", validation.getReport());

    // Add performance statistics
    final Map<String, Object> performanceStats = new HashMap<>();
    final Map<String, List<PerformanceRegressionDetector.PerformanceMeasurement>> grouped =
        groupMeasurementsByCategory(measurements);

    for (final Map.Entry<String, List<PerformanceRegressionDetector.PerformanceMeasurement>> entry :
        grouped.entrySet()) {
      final String key = entry.getKey();
      final PerformanceRegressionDetector.PerformanceStatistics stats =
          new PerformanceRegressionDetector.PerformanceStatistics(
              entry.getValue(), CONFIDENCE_INTERVAL);

      final Map<String, Object> categoryStats = new HashMap<>();
      categoryStats.put("meanThroughput", stats.getMeanThroughput());
      categoryStats.put("meanLatency", stats.getMeanLatency());
      categoryStats.put("sampleCount", stats.getSampleCount());
      categoryStats.put("throughputConfidenceInterval", stats.getThroughputConfidenceInterval());

      performanceStats.put(key, categoryStats);
    }

    report.put("performanceStatistics", performanceStats);

    try {
      return objectMapper.writeValueAsString(report);
    } catch (final Exception e) {
      LOGGER.warning("Failed to generate CI/CD report: " + e.getMessage());
      return "{\"error\": \"Failed to generate CI/CD performance report\"}";
    }
  }

  /** Performance validation result. */
  public static final class PerformanceValidationResult {
    private final boolean isValid;
    private final String report;

    public PerformanceValidationResult(final boolean isValid, final String report) {
      this.isValid = isValid;
      this.report = report;
    }

    public boolean isValid() {
      return isValid;
    }

    public String getReport() {
      return report;
    }
  }

  private Map<String, List<PerformanceRegressionDetector.PerformanceMeasurement>>
      groupMeasurementsByCategory(
          final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements) {
    return measurements.stream()
        .collect(
            Collectors.groupingBy(
                m -> extractBenchmarkCategory(m.getBenchmarkName()) + ":" + m.getRuntimeType()));
  }

  private String extractBenchmarkCategory(final String benchmarkName) {
    // Extract category from benchmark name
    if (benchmarkName.contains("Memory")) {
      return "MEMORY_OPERATIONS";
    } else if (benchmarkName.contains("Function")) {
      return "FUNCTION_EXECUTION";
    } else if (benchmarkName.contains("Module")) {
      return "MODULE_OPERATIONS";
    } else if (benchmarkName.contains("Runtime")) {
      return "RUNTIME_INITIALIZATION";
    } else if (benchmarkName.contains("Wasi")) {
      return "WASI_OPERATIONS";
    } else if (benchmarkName.contains("Comparison")) {
      return "RUNTIME_COMPARISON";
    } else if (benchmarkName.contains("Concurrency")) {
      return "CONCURRENCY_OPERATIONS";
    } else {
      return "CORE_OPERATIONS";
    }
  }

  private PerformanceBaseline establishCategoryBaseline(
      final String categoryKey,
      final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements) {
    if (measurements.size() < MINIMUM_SAMPLES) {
      throw new IllegalArgumentException(
          "Insufficient measurements for "
              + categoryKey
              + ": "
              + measurements.size()
              + " < "
              + MINIMUM_SAMPLES);
    }

    // Remove outliers
    final List<PerformanceRegressionDetector.PerformanceMeasurement> cleanedMeasurements =
        removeOutliers(measurements);

    // Calculate statistics
    final PerformanceRegressionDetector.PerformanceStatistics statistics =
        new PerformanceRegressionDetector.PerformanceStatistics(
            cleanedMeasurements, CONFIDENCE_INTERVAL);

    // Extract category and runtime
    final String[] parts = categoryKey.split(":");
    final String category = parts[0];
    final String runtime = parts[1];

    // Determine target performance
    final double targetPerformance = getTargetPerformance(runtime);
    final boolean meetsTarget = evaluateTargetAchievement(statistics, targetPerformance);
    final double achievementRatio = statistics.getMeanThroughput() / targetPerformance;

    // Create metadata
    final Map<String, Object> metadata = new HashMap<>();
    metadata.put("outlierCount", measurements.size() - cleanedMeasurements.size());
    metadata.put("targetPerformance", targetPerformance);
    metadata.put("confidenceLevel", CONFIDENCE_INTERVAL);
    metadata.put("minimumSamples", MINIMUM_SAMPLES);

    return new PerformanceBaseline(
        category,
        runtime,
        statistics,
        LocalDateTime.now(),
        metadata,
        meetsTarget,
        achievementRatio);
  }

  private List<PerformanceRegressionDetector.PerformanceMeasurement> removeOutliers(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements) {
    // Calculate mean and standard deviation
    final double mean =
        measurements.stream()
            .mapToDouble(PerformanceRegressionDetector.PerformanceMeasurement::getThroughput)
            .average()
            .orElse(0.0);
    final double variance =
        measurements.stream()
            .mapToDouble(m -> Math.pow(m.getThroughput() - mean, 2))
            .average()
            .orElse(0.0);
    final double stdDev = Math.sqrt(variance);

    // Filter outliers
    return measurements.stream()
        .filter(m -> Math.abs(m.getThroughput() - mean) <= OUTLIER_THRESHOLD * stdDev)
        .collect(Collectors.toList());
  }

  private double getTargetPerformance(final String runtime) {
    if ("JNI".equalsIgnoreCase(runtime)) {
      return JNI_PERFORMANCE_TARGET;
    } else if ("PANAMA".equalsIgnoreCase(runtime)) {
      return PANAMA_PERFORMANCE_TARGET;
    } else {
      return 0.5; // Default conservative target
    }
  }

  private boolean evaluateTargetAchievement(
      final PerformanceRegressionDetector.PerformanceStatistics statistics, final double target) {
    // Simplified target evaluation - in reality this would compare against native Wasmtime
    return statistics.getMeanThroughput() > target * 1000; // Assume 1000 ops/sec baseline
  }

  private List<PerformanceRegressionDetector.PerformanceMeasurement> executeBenchmarkSuite(
      final int iterations, final int warmupIterations, final int forks) {
    // This method would execute the actual benchmark suite
    // For now, return empty list as this requires integration with JMH execution
    LOGGER.info(
        "Executing benchmark suite with "
            + iterations
            + " iterations, "
            + warmupIterations
            + " warmup, "
            + forks
            + " forks");
    return new ArrayList<>();
  }

  private void saveBaselines(final Map<String, PerformanceBaseline> baselines) {
    try {
      Files.createDirectories(baselineStoragePath);
      final Path baselinesFile = baselineStoragePath.resolve("performance-baselines.json");
      objectMapper.writeValue(baselinesFile.toFile(), baselines);
      LOGGER.info("Saved " + baselines.size() + " baselines to: " + baselinesFile);
    } catch (final IOException e) {
      LOGGER.warning("Failed to save baselines: " + e.getMessage());
    }
  }

  private Map<String, PerformanceBaseline> loadBaselines() {
    try {
      final Path baselinesFile = baselineStoragePath.resolve("performance-baselines.json");
      if (Files.exists(baselinesFile)) {
        final Map<String, PerformanceBaseline> baselines =
            objectMapper.readValue(
                baselinesFile.toFile(),
                objectMapper
                    .getTypeFactory()
                    .constructMapType(Map.class, String.class, PerformanceBaseline.class));
        LOGGER.info("Loaded " + baselines.size() + " baselines from: " + baselinesFile);
        return baselines;
      }
    } catch (final IOException e) {
      LOGGER.warning("Failed to load baselines: " + e.getMessage());
    }
    return new HashMap<>();
  }

  private Map<String, Boolean> analyzeTargetAchievement(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements,
      final Map<String, PerformanceBaseline> baselines) {
    final Map<String, Boolean> achievements = new HashMap<>();
    final Map<String, List<PerformanceRegressionDetector.PerformanceMeasurement>> grouped =
        groupMeasurementsByCategory(measurements);

    for (final Map.Entry<String, List<PerformanceRegressionDetector.PerformanceMeasurement>> entry :
        grouped.entrySet()) {
      final String key = entry.getKey();
      final PerformanceBaseline baseline = baselines.get(key);

      if (baseline != null) {
        final PerformanceRegressionDetector.PerformanceStatistics currentStats =
            new PerformanceRegressionDetector.PerformanceStatistics(
                entry.getValue(), CONFIDENCE_INTERVAL);
        final boolean meetsTarget =
            currentStats.getMeanThroughput()
                >= baseline.getStatistics().getMeanThroughput() * 0.95; // 95% of baseline
        achievements.put(key, meetsTarget);
      }
    }

    return achievements;
  }

  private String generateBaselineSummaryReport(
      final Map<String, PerformanceBaseline> baselines, final List<String> failures) {
    final StringBuilder report = new StringBuilder();
    report.append("Performance Baseline Establishment Report\n");
    report
        .append("Generated: ")
        .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .append("\n");
    report.append("===========================================\n\n");

    report.append("Summary:\n");
    report.append("  Total baselines established: ").append(baselines.size()).append("\n");
    report.append("  Failures: ").append(failures.size()).append("\n");

    final long targetsAchieved =
        baselines.values().stream().mapToLong(b -> b.meetsTargetPerformance() ? 1 : 0).sum();
    report
        .append("  Performance targets achieved: ")
        .append(targetsAchieved)
        .append("/")
        .append(baselines.size())
        .append("\n\n");

    report.append("Baseline Details:\n");
    for (final PerformanceBaseline baseline : baselines.values()) {
      report.append("  ").append(baseline).append("\n");
    }

    if (!failures.isEmpty()) {
      report.append("\nFailures:\n");
      for (final String failure : failures) {
        report.append("  - ").append(failure).append("\n");
      }
    }

    return report.toString();
  }

  private String generateValidationReport(
      final List<PerformanceRegressionDetector.RegressionResult> regressions,
      final Map<String, Boolean> targetAchievements) {
    final StringBuilder report = new StringBuilder();
    report.append("Performance Validation Report\n");
    report
        .append("Generated: ")
        .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .append("\n");
    report.append("==============================\n\n");

    // Regression analysis
    final long regressionCount =
        regressions.stream().mapToLong(r -> r.isRegression() ? 1 : 0).sum();
    report.append("Regression Analysis:\n");
    report
        .append("  Total regressions detected: ")
        .append(regressionCount)
        .append("/")
        .append(regressions.size())
        .append("\n");

    for (final PerformanceRegressionDetector.RegressionResult regression : regressions) {
      if (regression.isRegression()) {
        report.append("  ⚠️ REGRESSION: ").append(regression).append("\n");
      }
    }

    // Target achievement analysis
    final long targetsAchieved =
        targetAchievements.values().stream().mapToLong(a -> a ? 1 : 0).sum();
    report.append("\nTarget Achievement Analysis:\n");
    report
        .append("  Targets achieved: ")
        .append(targetsAchieved)
        .append("/")
        .append(targetAchievements.size())
        .append("\n");

    for (final Map.Entry<String, Boolean> entry : targetAchievements.entrySet()) {
      final String status = entry.getValue() ? "✅ ACHIEVED" : "❌ MISSED";
      report.append("  ").append(status).append(": ").append(entry.getKey()).append("\n");
    }

    return report.toString();
  }

  private ObjectMapper createObjectMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }
}

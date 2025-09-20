package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Provides dashboard integration for WASI test results and analytics. Creates comprehensive
 * visualizations and reports for WASI test execution, compatibility analysis, and performance
 * metrics.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Real-time WASI test execution dashboards
 *   <li>WASI Preview 1/2 compatibility tracking and visualization
 *   <li>Cross-runtime performance comparison charts
 *   <li>WASI feature coverage heat maps
 *   <li>Test failure analysis and trend reporting
 *   <li>Resource utilization monitoring for WASI operations
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiDashboardIntegration {
  private static final Logger LOGGER = Logger.getLogger(WasiDashboardIntegration.class.getName());

  /** WASI dashboard configuration for customizing reports and visualizations. */
  public static final class WasiDashboardConfiguration {
    private final boolean enableRealTimeUpdates;
    private final boolean includePerformanceCharts;
    private final boolean includeCompatibilityMatrix;
    private final boolean includeCoverageHeatMap;
    private final int maxHistoricalDataPoints;
    private final Map<String, String> customDashboardSettings;

    private WasiDashboardConfiguration(final Builder builder) {
      this.enableRealTimeUpdates = builder.enableRealTimeUpdates;
      this.includePerformanceCharts = builder.includePerformanceCharts;
      this.includeCompatibilityMatrix = builder.includeCompatibilityMatrix;
      this.includeCoverageHeatMap = builder.includeCoverageHeatMap;
      this.maxHistoricalDataPoints = builder.maxHistoricalDataPoints;
      this.customDashboardSettings = Map.copyOf(builder.customDashboardSettings);
    }

    public boolean isEnableRealTimeUpdates() {
      return enableRealTimeUpdates;
    }

    public boolean isIncludePerformanceCharts() {
      return includePerformanceCharts;
    }

    public boolean isIncludeCompatibilityMatrix() {
      return includeCompatibilityMatrix;
    }

    public boolean isIncludeCoverageHeatMap() {
      return includeCoverageHeatMap;
    }

    public int getMaxHistoricalDataPoints() {
      return maxHistoricalDataPoints;
    }

    public Map<String, String> getCustomDashboardSettings() {
      return customDashboardSettings;
    }

    public static final class Builder {
      private boolean enableRealTimeUpdates = true;
      private boolean includePerformanceCharts = true;
      private boolean includeCompatibilityMatrix = true;
      private boolean includeCoverageHeatMap = true;
      private int maxHistoricalDataPoints = 1000;
      private final Map<String, String> customDashboardSettings = new HashMap<>();

      public Builder enableRealTimeUpdates(final boolean enable) {
        this.enableRealTimeUpdates = enable;
        return this;
      }

      public Builder includePerformanceCharts(final boolean include) {
        this.includePerformanceCharts = include;
        return this;
      }

      public Builder includeCompatibilityMatrix(final boolean include) {
        this.includeCompatibilityMatrix = include;
        return this;
      }

      public Builder includeCoverageHeatMap(final boolean include) {
        this.includeCoverageHeatMap = include;
        return this;
      }

      public Builder maxHistoricalDataPoints(final int max) {
        this.maxHistoricalDataPoints = max;
        return this;
      }

      public Builder customSetting(final String key, final String value) {
        customDashboardSettings.put(key, value);
        return this;
      }

      public WasiDashboardConfiguration build() {
        return new WasiDashboardConfiguration(this);
      }
    }
  }

  /** WASI dashboard report containing all visualization data and metrics. */
  public static final class WasiDashboardReport {
    private final WasiTestExecutionSummary executionSummary;
    private final WasiCompatibilityMatrix compatibilityMatrix;
    private final WasiPerformanceCharts performanceCharts;
    private final WasiCoverageHeatMap coverageHeatMap;
    private final WasiTrendAnalysis trendAnalysis;
    private final List<WasiRecommendation> recommendations;
    private final Instant generationTime;

    public WasiDashboardReport(
        final WasiTestExecutionSummary executionSummary,
        final WasiCompatibilityMatrix compatibilityMatrix,
        final WasiPerformanceCharts performanceCharts,
        final WasiCoverageHeatMap coverageHeatMap,
        final WasiTrendAnalysis trendAnalysis,
        final List<WasiRecommendation> recommendations) {
      this.executionSummary = executionSummary;
      this.compatibilityMatrix = compatibilityMatrix;
      this.performanceCharts = performanceCharts;
      this.coverageHeatMap = coverageHeatMap;
      this.trendAnalysis = trendAnalysis;
      this.recommendations = List.copyOf(recommendations);
      this.generationTime = Instant.now();
    }

    public WasiTestExecutionSummary getExecutionSummary() {
      return executionSummary;
    }

    public WasiCompatibilityMatrix getCompatibilityMatrix() {
      return compatibilityMatrix;
    }

    public WasiPerformanceCharts getPerformanceCharts() {
      return performanceCharts;
    }

    public WasiCoverageHeatMap getCoverageHeatMap() {
      return coverageHeatMap;
    }

    public WasiTrendAnalysis getTrendAnalysis() {
      return trendAnalysis;
    }

    public List<WasiRecommendation> getRecommendations() {
      return recommendations;
    }

    public Instant getGenerationTime() {
      return generationTime;
    }
  }

  /** WASI test execution summary for dashboard overview. */
  public static final class WasiTestExecutionSummary {
    private final int totalTestsExecuted;
    private final int successfulTests;
    private final int failedTests;
    private final Map<WasiTestIntegrator.WasiTestCategory, Integer> testsPerCategory;
    private final Map<RuntimeType, Double> runtimeSuccessRates;
    private final double overallSuccessRate;
    private final long totalExecutionTime;
    private final Map<String, Object> additionalMetrics;

    public WasiTestExecutionSummary(
        final int totalTestsExecuted,
        final int successfulTests,
        final int failedTests,
        final Map<WasiTestIntegrator.WasiTestCategory, Integer> testsPerCategory,
        final Map<RuntimeType, Double> runtimeSuccessRates,
        final double overallSuccessRate,
        final long totalExecutionTime,
        final Map<String, Object> additionalMetrics) {
      this.totalTestsExecuted = totalTestsExecuted;
      this.successfulTests = successfulTests;
      this.failedTests = failedTests;
      this.testsPerCategory = Map.copyOf(testsPerCategory);
      this.runtimeSuccessRates = Map.copyOf(runtimeSuccessRates);
      this.overallSuccessRate = overallSuccessRate;
      this.totalExecutionTime = totalExecutionTime;
      this.additionalMetrics = Map.copyOf(additionalMetrics);
    }

    public int getTotalTestsExecuted() {
      return totalTestsExecuted;
    }

    public int getSuccessfulTests() {
      return successfulTests;
    }

    public int getFailedTests() {
      return failedTests;
    }

    public Map<WasiTestIntegrator.WasiTestCategory, Integer> getTestsPerCategory() {
      return testsPerCategory;
    }

    public Map<RuntimeType, Double> getRuntimeSuccessRates() {
      return runtimeSuccessRates;
    }

    public double getOverallSuccessRate() {
      return overallSuccessRate;
    }

    public long getTotalExecutionTime() {
      return totalExecutionTime;
    }

    public Map<String, Object> getAdditionalMetrics() {
      return additionalMetrics;
    }
  }

  /** WASI compatibility matrix for runtime comparison. */
  public static final class WasiCompatibilityMatrix {
    private final Map<RuntimeType, Map<WasiTestIntegrator.WasiTestCategory, Double>>
        compatibilityScores;
    private final Map<RuntimeType, Double> overallRuntimeScores;
    private final Map<WasiTestIntegrator.WasiTestCategory, Double> categoryAverageScores;
    private final List<String> compatibilityIssues;

    public WasiCompatibilityMatrix(
        final Map<RuntimeType, Map<WasiTestIntegrator.WasiTestCategory, Double>>
            compatibilityScores,
        final Map<RuntimeType, Double> overallRuntimeScores,
        final Map<WasiTestIntegrator.WasiTestCategory, Double> categoryAverageScores,
        final List<String> compatibilityIssues) {
      this.compatibilityScores = Map.copyOf(compatibilityScores);
      this.overallRuntimeScores = Map.copyOf(overallRuntimeScores);
      this.categoryAverageScores = Map.copyOf(categoryAverageScores);
      this.compatibilityIssues = List.copyOf(compatibilityIssues);
    }

    public Map<RuntimeType, Map<WasiTestIntegrator.WasiTestCategory, Double>>
        getCompatibilityScores() {
      return compatibilityScores;
    }

    public Map<RuntimeType, Double> getOverallRuntimeScores() {
      return overallRuntimeScores;
    }

    public Map<WasiTestIntegrator.WasiTestCategory, Double> getCategoryAverageScores() {
      return categoryAverageScores;
    }

    public List<String> getCompatibilityIssues() {
      return compatibilityIssues;
    }
  }

  /** WASI performance charts data for visualization. */
  public static final class WasiPerformanceCharts {
    private final Map<RuntimeType, List<Double>> executionTimeTrends;
    private final Map<RuntimeType, List<Long>> memoryUsageTrends;
    private final Map<WasiTestIntegrator.WasiTestCategory, Map<RuntimeType, Double>>
        categoryPerformance;
    private final Map<String, List<Double>> customMetricTrends;

    public WasiPerformanceCharts(
        final Map<RuntimeType, List<Double>> executionTimeTrends,
        final Map<RuntimeType, List<Long>> memoryUsageTrends,
        final Map<WasiTestIntegrator.WasiTestCategory, Map<RuntimeType, Double>>
            categoryPerformance,
        final Map<String, List<Double>> customMetricTrends) {
      this.executionTimeTrends = Map.copyOf(executionTimeTrends);
      this.memoryUsageTrends = Map.copyOf(memoryUsageTrends);
      this.categoryPerformance = Map.copyOf(categoryPerformance);
      this.customMetricTrends = Map.copyOf(customMetricTrends);
    }

    public Map<RuntimeType, List<Double>> getExecutionTimeTrends() {
      return executionTimeTrends;
    }

    public Map<RuntimeType, List<Long>> getMemoryUsageTrends() {
      return memoryUsageTrends;
    }

    public Map<WasiTestIntegrator.WasiTestCategory, Map<RuntimeType, Double>>
        getCategoryPerformance() {
      return categoryPerformance;
    }

    public Map<String, List<Double>> getCustomMetricTrends() {
      return customMetricTrends;
    }
  }

  /** WASI coverage heat map for feature analysis. */
  public static final class WasiCoverageHeatMap {
    private final Map<WasiTestIntegrator.WasiTestCategory, Map<RuntimeType, Double>> coverageMatrix;
    private final Map<String, Double> featureCoverageScores;
    private final List<String> uncoveredFeatures;
    private final Map<String, Integer> featureTestCounts;

    public WasiCoverageHeatMap(
        final Map<WasiTestIntegrator.WasiTestCategory, Map<RuntimeType, Double>> coverageMatrix,
        final Map<String, Double> featureCoverageScores,
        final List<String> uncoveredFeatures,
        final Map<String, Integer> featureTestCounts) {
      this.coverageMatrix = Map.copyOf(coverageMatrix);
      this.featureCoverageScores = Map.copyOf(featureCoverageScores);
      this.uncoveredFeatures = List.copyOf(uncoveredFeatures);
      this.featureTestCounts = Map.copyOf(featureTestCounts);
    }

    public Map<WasiTestIntegrator.WasiTestCategory, Map<RuntimeType, Double>> getCoverageMatrix() {
      return coverageMatrix;
    }

    public Map<String, Double> getFeatureCoverageScores() {
      return featureCoverageScores;
    }

    public List<String> getUncoveredFeatures() {
      return uncoveredFeatures;
    }

    public Map<String, Integer> getFeatureTestCounts() {
      return featureTestCounts;
    }
  }

  /** WASI trend analysis for historical data. */
  public static final class WasiTrendAnalysis {
    private final Map<String, List<Double>> historicalSuccessRates;
    private final Map<String, List<Double>> historicalPerformanceMetrics;
    private final List<String> improvingAreas;
    private final List<String> decliningAreas;
    private final Map<String, String> trendSummaries;

    public WasiTrendAnalysis(
        final Map<String, List<Double>> historicalSuccessRates,
        final Map<String, List<Double>> historicalPerformanceMetrics,
        final List<String> improvingAreas,
        final List<String> decliningAreas,
        final Map<String, String> trendSummaries) {
      this.historicalSuccessRates = Map.copyOf(historicalSuccessRates);
      this.historicalPerformanceMetrics = Map.copyOf(historicalPerformanceMetrics);
      this.improvingAreas = List.copyOf(improvingAreas);
      this.decliningAreas = List.copyOf(decliningAreas);
      this.trendSummaries = Map.copyOf(trendSummaries);
    }

    public Map<String, List<Double>> getHistoricalSuccessRates() {
      return historicalSuccessRates;
    }

    public Map<String, List<Double>> getHistoricalPerformanceMetrics() {
      return historicalPerformanceMetrics;
    }

    public List<String> getImprovingAreas() {
      return improvingAreas;
    }

    public List<String> getDecliningAreas() {
      return decliningAreas;
    }

    public Map<String, String> getTrendSummaries() {
      return trendSummaries;
    }
  }

  /** WASI-specific recommendations for improvement. */
  public static final class WasiRecommendation {
    private final String category;
    private final String description;
    private final String priority;
    private final List<String> actionItems;
    private final Map<String, String> additionalContext;

    public WasiRecommendation(
        final String category,
        final String description,
        final String priority,
        final List<String> actionItems,
        final Map<String, String> additionalContext) {
      this.category = category;
      this.description = description;
      this.priority = priority;
      this.actionItems = List.copyOf(actionItems);
      this.additionalContext = Map.copyOf(additionalContext);
    }

    public String getCategory() {
      return category;
    }

    public String getDescription() {
      return description;
    }

    public String getPriority() {
      return priority;
    }

    public List<String> getActionItems() {
      return actionItems;
    }

    public Map<String, String> getAdditionalContext() {
      return additionalContext;
    }
  }

  private final WasiDashboardConfiguration configuration;
  private final Map<String, WasiTestIntegrator.WasiTestExecutionResult> historicalResults;
  private final Map<String, List<Double>> performanceHistory;

  /** Creates a new WASI dashboard integration with default configuration. */
  public WasiDashboardIntegration() {
    this(new WasiDashboardConfiguration.Builder().build());
  }

  /**
   * Creates a new WASI dashboard integration with specified configuration.
   *
   * @param configuration the dashboard configuration
   */
  public WasiDashboardIntegration(final WasiDashboardConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
    this.historicalResults = new ConcurrentHashMap<>();
    this.performanceHistory = new ConcurrentHashMap<>();
  }

  /**
   * Generates a comprehensive WASI dashboard report from execution results.
   *
   * @param executionResults the WASI test execution results
   * @return comprehensive dashboard report
   */
  public WasiDashboardReport generateDashboardReport(
      final Map<String, WasiTestIntegrator.WasiTestExecutionResult> executionResults) {
    Objects.requireNonNull(executionResults, "executionResults cannot be null");

    LOGGER.info(
        "Generating WASI dashboard report for " + executionResults.size() + " test results");

    // Update historical data
    updateHistoricalData(executionResults);

    // Generate execution summary
    final WasiTestExecutionSummary executionSummary = generateExecutionSummary(executionResults);

    // Generate compatibility matrix
    final WasiCompatibilityMatrix compatibilityMatrix =
        configuration.isIncludeCompatibilityMatrix()
            ? generateCompatibilityMatrix(executionResults)
            : null;

    // Generate performance charts
    final WasiPerformanceCharts performanceCharts =
        configuration.isIncludePerformanceCharts()
            ? generatePerformanceCharts(executionResults)
            : null;

    // Generate coverage heat map
    final WasiCoverageHeatMap coverageHeatMap =
        configuration.isIncludeCoverageHeatMap() ? generateCoverageHeatMap(executionResults) : null;

    // Generate trend analysis
    final WasiTrendAnalysis trendAnalysis = generateTrendAnalysis();

    // Generate recommendations
    final List<WasiRecommendation> recommendations = generateRecommendations(executionResults);

    LOGGER.info("WASI dashboard report generation completed");

    return new WasiDashboardReport(
        executionSummary,
        compatibilityMatrix,
        performanceCharts,
        coverageHeatMap,
        trendAnalysis,
        recommendations);
  }

  /**
   * Updates the dashboard with new test execution results.
   *
   * @param result the new test execution result
   */
  public void updateWithResult(final WasiTestIntegrator.WasiTestExecutionResult result) {
    Objects.requireNonNull(result, "result cannot be null");

    historicalResults.put(result.getTestName(), result);

    // Update performance history
    updatePerformanceHistory(result);

    // Trim historical data if necessary
    trimHistoricalData();

    if (configuration.isEnableRealTimeUpdates()) {
      LOGGER.fine("Dashboard updated with new WASI test result: " + result.getTestName());
    }
  }

  /** Clears all dashboard data and history. */
  public void clearData() {
    historicalResults.clear();
    performanceHistory.clear();
    LOGGER.info("WASI dashboard data cleared");
  }

  private void updateHistoricalData(
      final Map<String, WasiTestIntegrator.WasiTestExecutionResult> results) {
    for (final Map.Entry<String, WasiTestIntegrator.WasiTestExecutionResult> entry :
        results.entrySet()) {
      updateWithResult(entry.getValue());
    }
  }

  private WasiTestExecutionSummary generateExecutionSummary(
      final Map<String, WasiTestIntegrator.WasiTestExecutionResult> executionResults) {

    final int totalTests = executionResults.size();
    final int successfulTests =
        (int)
            executionResults.values().stream()
                .mapToLong(result -> result.isSuccessful() ? 1 : 0)
                .sum();
    final int failedTests = totalTests - successfulTests;

    final Map<WasiTestIntegrator.WasiTestCategory, Integer> testsPerCategory =
        new EnumMap<>(WasiTestIntegrator.WasiTestCategory.class);
    for (final WasiTestIntegrator.WasiTestExecutionResult result : executionResults.values()) {
      testsPerCategory.merge(result.getCategory(), 1, Integer::sum);
    }

    final Map<RuntimeType, Double> runtimeSuccessRates = new EnumMap<>(RuntimeType.class);
    for (final RuntimeType runtime : RuntimeType.values()) {
      final long totalForRuntime =
          executionResults.values().stream()
              .mapToLong(result -> result.getRuntimeResults().containsKey(runtime) ? 1 : 0)
              .sum();
      final long successfulForRuntime =
          executionResults.values().stream()
              .mapToLong(
                  result ->
                      result.getRuntimeResults().containsKey(runtime)
                              && result.getRuntimeResults().get(runtime).isSuccessful()
                          ? 1
                          : 0)
              .sum();

      if (totalForRuntime > 0) {
        runtimeSuccessRates.put(runtime, (double) successfulForRuntime / totalForRuntime * 100.0);
      }
    }

    final double overallSuccessRate =
        totalTests > 0 ? (double) successfulTests / totalTests * 100.0 : 0.0;

    final long totalExecutionTime =
        executionResults.values().stream()
            .mapToLong(
                result ->
                    result.getPerformanceMetrics().getIoOperationTimes().values().stream()
                        .mapToLong(Long::longValue)
                        .sum())
            .sum();

    final Map<String, Object> additionalMetrics = new HashMap<>();
    additionalMetrics.put("average_execution_time", totalExecutionTime / Math.max(totalTests, 1));
    additionalMetrics.put(
        "preview1_tests",
        testsPerCategory.getOrDefault(WasiTestIntegrator.WasiTestCategory.PREVIEW1, 0));
    additionalMetrics.put(
        "preview2_tests",
        testsPerCategory.getOrDefault(WasiTestIntegrator.WasiTestCategory.PREVIEW2, 0));

    return new WasiTestExecutionSummary(
        totalTests,
        successfulTests,
        failedTests,
        testsPerCategory,
        runtimeSuccessRates,
        overallSuccessRate,
        totalExecutionTime,
        additionalMetrics);
  }

  private WasiCompatibilityMatrix generateCompatibilityMatrix(
      final Map<String, WasiTestIntegrator.WasiTestExecutionResult> executionResults) {

    final Map<RuntimeType, Map<WasiTestIntegrator.WasiTestCategory, Double>> compatibilityScores =
        new EnumMap<>(RuntimeType.class);
    final Map<RuntimeType, Double> overallRuntimeScores = new EnumMap<>(RuntimeType.class);
    final Map<WasiTestIntegrator.WasiTestCategory, Double> categoryAverageScores =
        new EnumMap<>(WasiTestIntegrator.WasiTestCategory.class);
    final List<String> compatibilityIssues = new ArrayList<>();

    // Calculate compatibility scores
    for (final RuntimeType runtime : RuntimeType.values()) {
      final Map<WasiTestIntegrator.WasiTestCategory, Double> runtimeCategoryScores =
          new EnumMap<>(WasiTestIntegrator.WasiTestCategory.class);

      for (final WasiTestIntegrator.WasiTestCategory category :
          WasiTestIntegrator.WasiTestCategory.values()) {
        final double categoryScore =
            executionResults.values().stream()
                .filter(result -> result.getCategory() == category)
                .mapToDouble(
                    result -> result.getCompatibilityAnalysis().getOverallCompatibilityScore())
                .average()
                .orElse(0.0);

        runtimeCategoryScores.put(category, categoryScore);
      }

      compatibilityScores.put(runtime, runtimeCategoryScores);

      final double overallScore =
          runtimeCategoryScores.values().stream()
              .mapToDouble(Double::doubleValue)
              .average()
              .orElse(0.0);
      overallRuntimeScores.put(runtime, overallScore);
    }

    // Calculate category averages
    for (final WasiTestIntegrator.WasiTestCategory category :
        WasiTestIntegrator.WasiTestCategory.values()) {
      final double avgScore =
          compatibilityScores.values().stream()
              .mapToDouble(scores -> scores.getOrDefault(category, 0.0))
              .average()
              .orElse(0.0);
      categoryAverageScores.put(category, avgScore);
    }

    // Collect compatibility issues
    for (final WasiTestIntegrator.WasiTestExecutionResult result : executionResults.values()) {
      compatibilityIssues.addAll(
          result.getCompatibilityAnalysis().getCompatibilityIssues().values());
    }

    return new WasiCompatibilityMatrix(
        compatibilityScores, overallRuntimeScores, categoryAverageScores, compatibilityIssues);
  }

  private WasiPerformanceCharts generatePerformanceCharts(
      final Map<String, WasiTestIntegrator.WasiTestExecutionResult> executionResults) {

    final Map<RuntimeType, List<Double>> executionTimeTrends = new EnumMap<>(RuntimeType.class);
    final Map<RuntimeType, List<Long>> memoryUsageTrends = new EnumMap<>(RuntimeType.class);
    final Map<WasiTestIntegrator.WasiTestCategory, Map<RuntimeType, Double>> categoryPerformance =
        new EnumMap<>(WasiTestIntegrator.WasiTestCategory.class);
    final Map<String, List<Double>> customMetricTrends = new HashMap<>();

    // Generate performance data
    for (final RuntimeType runtime : RuntimeType.values()) {
      final List<Double> execTimes =
          executionResults.values().stream()
              .filter(result -> result.getRuntimeResults().containsKey(runtime))
              .map(result -> (double) result.getRuntimeResults().get(runtime).getExecutionTime())
              .collect(Collectors.toList());
      executionTimeTrends.put(runtime, execTimes);

      final List<Long> memUsage =
          executionResults.values().stream()
              .filter(
                  result -> result.getPerformanceMetrics().getMemoryUsage().containsKey(runtime))
              .map(result -> result.getPerformanceMetrics().getMemoryUsage().get(runtime))
              .collect(Collectors.toList());
      memoryUsageTrends.put(runtime, memUsage);
    }

    // Generate category performance data
    for (final WasiTestIntegrator.WasiTestCategory category :
        WasiTestIntegrator.WasiTestCategory.values()) {
      final Map<RuntimeType, Double> categoryRuntimePerf = new EnumMap<>(RuntimeType.class);

      for (final RuntimeType runtime : RuntimeType.values()) {
        final double avgPerf =
            executionResults.values().stream()
                .filter(result -> result.getCategory() == category)
                .filter(result -> result.getRuntimeResults().containsKey(runtime))
                .mapToDouble(result -> result.getRuntimeResults().get(runtime).getExecutionTime())
                .average()
                .orElse(0.0);
        categoryRuntimePerf.put(runtime, avgPerf);
      }

      categoryPerformance.put(category, categoryRuntimePerf);
    }

    return new WasiPerformanceCharts(
        executionTimeTrends, memoryUsageTrends, categoryPerformance, customMetricTrends);
  }

  private WasiCoverageHeatMap generateCoverageHeatMap(
      final Map<String, WasiTestIntegrator.WasiTestExecutionResult> executionResults) {

    final Map<WasiTestIntegrator.WasiTestCategory, Map<RuntimeType, Double>> coverageMatrix =
        new EnumMap<>(WasiTestIntegrator.WasiTestCategory.class);
    final Map<String, Double> featureCoverageScores = new HashMap<>();
    final List<String> uncoveredFeatures = new ArrayList<>();
    final Map<String, Integer> featureTestCounts = new HashMap<>();

    // Generate coverage matrix and feature analysis
    for (final WasiTestIntegrator.WasiTestCategory category :
        WasiTestIntegrator.WasiTestCategory.values()) {
      final Map<RuntimeType, Double> categoryRuntimeCoverage = new EnumMap<>(RuntimeType.class);

      for (final RuntimeType runtime : RuntimeType.values()) {
        final long totalTests =
            executionResults.values().stream()
                .filter(result -> result.getCategory() == category)
                .count();
        final long successfulTests =
            executionResults.values().stream()
                .filter(result -> result.getCategory() == category)
                .filter(result -> result.getRuntimeResults().containsKey(runtime))
                .filter(result -> result.getRuntimeResults().get(runtime).isSuccessful())
                .count();

        final double coverage =
            totalTests > 0 ? (double) successfulTests / totalTests * 100.0 : 0.0;
        categoryRuntimeCoverage.put(runtime, coverage);
      }

      coverageMatrix.put(category, categoryRuntimeCoverage);
    }

    return new WasiCoverageHeatMap(
        coverageMatrix, featureCoverageScores, uncoveredFeatures, featureTestCounts);
  }

  private WasiTrendAnalysis generateTrendAnalysis() {
    final Map<String, List<Double>> historicalSuccessRates = new HashMap<>();
    final Map<String, List<Double>> historicalPerformanceMetrics = new HashMap<>();
    final List<String> improvingAreas = new ArrayList<>();
    final List<String> decliningAreas = new ArrayList<>();
    final Map<String, String> trendSummaries = new HashMap<>();

    // Generate trend data from performance history
    historicalSuccessRates.putAll(performanceHistory);

    return new WasiTrendAnalysis(
        historicalSuccessRates,
        historicalPerformanceMetrics,
        improvingAreas,
        decliningAreas,
        trendSummaries);
  }

  private List<WasiRecommendation> generateRecommendations(
      final Map<String, WasiTestIntegrator.WasiTestExecutionResult> executionResults) {

    final List<WasiRecommendation> recommendations = new ArrayList<>();

    // Analyze results and generate recommendations
    final double overallSuccessRate =
        executionResults.values().stream()
                .mapToDouble(result -> result.isSuccessful() ? 1.0 : 0.0)
                .average()
                .orElse(0.0)
            * 100.0;

    if (overallSuccessRate < 90.0) {
      recommendations.add(
          new WasiRecommendation(
              "Test Success Rate",
              "Overall WASI test success rate is below 90%",
              "HIGH",
              List.of(
                  "Review failing tests",
                  "Improve WASI implementation",
                  "Update test expectations"),
              Map.of("current_rate", String.format("%.2f%%", overallSuccessRate))));
    }

    return recommendations;
  }

  private void updatePerformanceHistory(final WasiTestIntegrator.WasiTestExecutionResult result) {
    final String key = "success_rate_" + result.getCategory().name();
    performanceHistory
        .computeIfAbsent(key, k -> new ArrayList<>())
        .add(result.isSuccessful() ? 100.0 : 0.0);
  }

  private void trimHistoricalData() {
    if (historicalResults.size() > configuration.getMaxHistoricalDataPoints()) {
      // Remove oldest entries
      final List<String> keysToRemove =
          historicalResults.keySet().stream()
              .sorted(
                  (k1, k2) ->
                      historicalResults
                          .get(k1)
                          .getExecutionTime()
                          .compareTo(historicalResults.get(k2).getExecutionTime()))
              .limit(historicalResults.size() - configuration.getMaxHistoricalDataPoints())
              .collect(Collectors.toList());

      keysToRemove.forEach(historicalResults::remove);
    }

    // Trim performance history
    for (final List<Double> history : performanceHistory.values()) {
      if (history.size() > configuration.getMaxHistoricalDataPoints()) {
        while (history.size() > configuration.getMaxHistoricalDataPoints()) {
          history.remove(0);
        }
      }
    }
  }
}

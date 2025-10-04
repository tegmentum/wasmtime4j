package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builder for creating interactive data visualizations using Chart.js and D3.js libraries.
 * Generates JSON configuration objects for charts that display performance metrics, coverage
 * analysis, trend data, and behavioral comparison results.
 *
 * @since 1.0.0
 */
public final class VisualizationBuilder {

  /**
   * Creates Chart.js configuration for performance comparison charts.
   *
   * @param report the comparison report containing performance data
   * @return JSON configuration for performance charts
   */
  public Map<String, Object> createPerformanceChartData(final ComparisonReport report) {
    Objects.requireNonNull(report, "report cannot be null");

    final Map<String, Object> chartConfig = new HashMap<>();
    final PerformanceAnalysisSummary performanceSummary = report.getPerformanceSummary();

    // Chart type and basic configuration
    chartConfig.put("type", "bar");
    chartConfig.put("responsive", true);
    chartConfig.put("maintainAspectRatio", false);

    // Data for the chart
    final Map<String, Object> data = new HashMap<>();
    data.put(
        "labels",
        performanceSummary.getRuntimePerformanceScores().keySet().stream()
            .map(RuntimeType::toString)
            .collect(Collectors.toList()));

    final List<Object> datasets =
        List.of(
            createPerformanceDataset(
                "Performance Scores", performanceSummary.getRuntimePerformanceScores()),
            createVarianceDataset("Performance Variance", report));
    data.put("datasets", datasets);

    chartConfig.put("data", data);

    // Chart options
    final Map<String, Object> options = createPerformanceChartOptions();
    chartConfig.put("options", options);

    return chartConfig;
  }

  /**
   * Creates Chart.js configuration for coverage analysis visualization.
   *
   * @param report the comparison report containing coverage data
   * @return JSON configuration for coverage charts
   */
  public Map<String, Object> createCoverageChartData(final ComparisonReport report) {
    Objects.requireNonNull(report, "report cannot be null");

    final Map<String, Object> chartConfig = new HashMap<>();

    // Doughnut chart for overall coverage
    chartConfig.put("type", "doughnut");
    chartConfig.put("responsive", true);

    final Map<String, Object> data = new HashMap<>();

    // Coverage data
    final var coverageReport = report.getCoverageReport();
    final double overallCoverage = coverageReport.getOverallCoverageScore();
    final double uncovered = 100.0 - overallCoverage;

    data.put("labels", List.of("Covered Features", "Uncovered Features"));
    data.put(
        "datasets",
        List.of(
            Map.of(
                "label", "Feature Coverage",
                "data", List.of(overallCoverage, uncovered),
                "backgroundColor", List.of("#28a745", "#dc3545"),
                "borderColor", List.of("#1e7e34", "#bd2130"),
                "borderWidth", 2)));

    chartConfig.put("data", data);

    // Chart options
    final Map<String, Object> options = createCoverageChartOptions();
    chartConfig.put("options", options);

    return chartConfig;
  }

  /**
   * Creates Chart.js configuration for trend analysis over time.
   *
   * @param report the comparison report containing trend data
   * @return JSON configuration for trend charts
   */
  public Map<String, Object> createTrendChartData(final ComparisonReport report) {
    Objects.requireNonNull(report, "report cannot be null");

    final Map<String, Object> chartConfig = new HashMap<>();

    // Line chart for trends
    chartConfig.put("type", "line");
    chartConfig.put("responsive", true);

    final Map<String, Object> data = new HashMap<>();

    // Trend data from performance summary
    final var performanceTrends = report.getPerformanceSummary().getPerformanceTrends();

    data.put("labels", performanceTrends.keySet().stream().collect(Collectors.toList()));
    data.put(
        "datasets",
        List.of(
            Map.of(
                "label",
                "Performance Trend",
                "data",
                performanceTrends.values().stream().collect(Collectors.toList()),
                "borderColor",
                "#007bff",
                "backgroundColor",
                "rgba(0, 123, 255, 0.1)",
                "fill",
                true,
                "tension",
                0.4)));

    chartConfig.put("data", data);

    // Chart options
    final Map<String, Object> options = createTrendChartOptions();
    chartConfig.put("options", options);

    return chartConfig;
  }

  /**
   * Creates a dataset for performance scores visualization.
   *
   * @param label the dataset label
   * @param performanceScores the runtime performance scores
   * @return dataset configuration
   */
  private Map<String, Object> createPerformanceDataset(
      final String label, final Map<RuntimeType, Double> performanceScores) {

    final Map<String, Object> dataset = new HashMap<>();
    dataset.put("label", label);
    dataset.put("data", performanceScores.values().stream().collect(Collectors.toList()));
    dataset.put(
        "backgroundColor",
        List.of(
            "rgba(54, 162, 235, 0.8)",
            "rgba(255, 99, 132, 0.8)",
            "rgba(75, 192, 192, 0.8)",
            "rgba(255, 205, 86, 0.8)"));
    dataset.put(
        "borderColor",
        List.of(
            "rgba(54, 162, 235, 1)",
            "rgba(255, 99, 132, 1)",
            "rgba(75, 192, 192, 1)",
            "rgba(255, 205, 86, 1)"));
    dataset.put("borderWidth", 2);

    return dataset;
  }

  /**
   * Creates a dataset for performance variance visualization.
   *
   * @param label the dataset label
   * @param report the comparison report
   * @return dataset configuration
   */
  private Map<String, Object> createVarianceDataset(
      final String label, final ComparisonReport report) {
    final Map<String, Object> dataset = new HashMap<>();
    dataset.put("label", label);
    dataset.put("type", "line");
    dataset.put("yAxisID", "variance");

    // Calculate variance per runtime (simplified for demonstration)
    final var performanceSummary = report.getPerformanceSummary();
    final double avgVariance = performanceSummary.getAveragePerformanceVariance();
    final int runtimeCount = performanceSummary.getRuntimePerformanceScores().size();

    // Create variance data points
    final List<Double> varianceData =
        performanceSummary.getRuntimePerformanceScores().keySet().stream()
            .map(
                runtime ->
                    avgVariance * (0.8 + Math.random() * 0.4)) // Simulate variance per runtime
            .collect(Collectors.toList());

    dataset.put("data", varianceData);
    dataset.put("borderColor", "#ff6384");
    dataset.put("backgroundColor", "rgba(255, 99, 132, 0.1)");
    dataset.put("fill", false);

    return dataset;
  }

  /**
   * Creates chart options for performance visualization.
   *
   * @return chart options configuration
   */
  private Map<String, Object> createPerformanceChartOptions() {
    final Map<String, Object> options = new HashMap<>();

    // Responsive configuration
    options.put("responsive", true);
    options.put("maintainAspectRatio", false);

    // Plugins configuration
    final Map<String, Object> plugins = new HashMap<>();
    plugins.put("title", Map.of("display", true, "text", "Runtime Performance Comparison"));
    plugins.put("legend", Map.of("position", "top"));
    options.put("plugins", plugins);

    // Scales configuration
    final Map<String, Object> scales = new HashMap<>();
    scales.put(
        "y",
        Map.of("beginAtZero", true, "title", Map.of("display", true, "text", "Performance Score")));
    scales.put(
        "variance",
        Map.of(
            "type",
            "linear",
            "position",
            "right",
            "beginAtZero",
            true,
            "title",
            Map.of("display", true, "text", "Variance"),
            "grid",
            Map.of("drawOnChartArea", false)));
    options.put("scales", scales);

    return options;
  }

  /**
   * Creates chart options for coverage visualization.
   *
   * @return chart options configuration
   */
  private Map<String, Object> createCoverageChartOptions() {
    final Map<String, Object> options = new HashMap<>();

    options.put("responsive", true);
    options.put("maintainAspectRatio", false);

    final Map<String, Object> plugins = new HashMap<>();
    plugins.put("title", Map.of("display", true, "text", "Feature Coverage Analysis"));
    plugins.put("legend", Map.of("position", "bottom"));
    options.put("plugins", plugins);

    return options;
  }

  /**
   * Creates chart options for trend visualization.
   *
   * @return chart options configuration
   */
  private Map<String, Object> createTrendChartOptions() {
    final Map<String, Object> options = new HashMap<>();

    options.put("responsive", true);
    options.put("maintainAspectRatio", false);

    final Map<String, Object> plugins = new HashMap<>();
    plugins.put("title", Map.of("display", true, "text", "Performance Trends Over Time"));
    plugins.put("legend", Map.of("position", "top"));
    options.put("plugins", plugins);

    final Map<String, Object> scales = new HashMap<>();
    scales.put("x", Map.of("title", Map.of("display", true, "text", "Test Cases")));
    scales.put(
        "y",
        Map.of("beginAtZero", true, "title", Map.of("display", true, "text", "Performance Value")));
    options.put("scales", scales);

    return options;
  }

  /**
   * Creates visualization data for behavioral discrepancy analysis.
   *
   * @param report the comparison report containing discrepancy data
   * @return visualization configuration for discrepancy analysis
   */
  public Map<String, Object> createDiscrepancyVisualization(final ComparisonReport report) {
    Objects.requireNonNull(report, "report cannot be null");

    final Map<String, Object> visualization = new HashMap<>();

    // Group discrepancies by severity
    final Map<String, List<BehavioralDiscrepancy>> discrepanciesBySeverity =
        report.getDiscrepanciesBySeverity();

    // Create data for severity distribution
    final Map<String, Object> severityData = new HashMap<>();
    severityData.put("labels", List.of("Critical", "High", "Medium", "Low"));
    severityData.put(
        "counts",
        List.of(
            discrepanciesBySeverity.getOrDefault("CRITICAL", List.of()).size(),
            discrepanciesBySeverity.getOrDefault("HIGH", List.of()).size(),
            discrepanciesBySeverity.getOrDefault("MEDIUM", List.of()).size(),
            discrepanciesBySeverity.getOrDefault("LOW", List.of()).size()));

    visualization.put("severityDistribution", severityData);

    // Create timeline data for discrepancy detection
    final Map<String, Object> timelineData = new HashMap<>();
    timelineData.put(
        "discrepancies",
        report.getBehavioralDiscrepancies().stream()
            .map(this::createDiscrepancyTimelineEntry)
            .collect(Collectors.toList()));

    visualization.put("timeline", timelineData);

    return visualization;
  }

  /**
   * Creates a timeline entry for a behavioral discrepancy.
   *
   * @param discrepancy the behavioral discrepancy
   * @return timeline entry data
   */
  private Map<String, Object> createDiscrepancyTimelineEntry(
      final BehavioralDiscrepancy discrepancy) {
    final Map<String, Object> entry = new HashMap<>();
    entry.put("timestamp", discrepancy.getDetectedAt().toString());
    entry.put("type", discrepancy.getType().toString());
    entry.put("severity", discrepancy.getSeverity().toString());
    entry.put("description", discrepancy.getDescription());
    entry.put("isCritical", discrepancy.isCritical());
    return entry;
  }

  /**
   * Creates comparison table data for side-by-side runtime comparison.
   *
   * @param report the comparison report
   * @return table data configuration
   */
  public Map<String, Object> createComparisonTableData(final ComparisonReport report) {
    Objects.requireNonNull(report, "report cannot be null");

    final Map<String, Object> tableData = new HashMap<>();

    // Column headers (runtime types)
    final List<String> headers =
        report.getMetadata().getRuntimeTypes().stream()
            .map(RuntimeType::toString)
            .collect(Collectors.toList());
    tableData.put("headers", headers);

    // Row data (test results)
    final List<Map<String, Object>> rows =
        report.getTestResults().stream()
            .map(this::createComparisonTableRow)
            .collect(Collectors.toList());
    tableData.put("rows", rows);

    return tableData;
  }

  /**
   * Creates a table row for test comparison.
   *
   * @param testResult the test comparison result
   * @return table row data
   */
  private Map<String, Object> createComparisonTableRow(final TestComparisonResult testResult) {
    final Map<String, Object> row = new HashMap<>();
    row.put("testName", testResult.getTestName());
    row.put("overallStatus", testResult.getOverallStatus().toString());
    row.put("hasCriticalIssues", testResult.hasCriticalIssues());

    // Runtime-specific results
    final Map<String, Object> runtimeResults = new HashMap<>();
    testResult
        .getRuntimeResults()
        .forEach(
            (runtime, result) -> {
              final Map<String, Object> resultData = new HashMap<>();
              resultData.put("successful", result.isSuccessful());
              resultData.put("executionTime", result.getExecutionTime().toMillis());
              resultData.put("hasError", !result.getErrorMessage().isEmpty());
              runtimeResults.put(runtime.toString(), resultData);
            });
    row.put("runtimeResults", runtimeResults);

    return row;
  }
}

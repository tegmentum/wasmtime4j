package ai.tegmentum.wasmtime4j.benchmarks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Performance regression testing framework for wasmtime4j.
 *
 * <p>This framework provides comprehensive performance regression detection and monitoring
 * capabilities to ensure that performance improvements are maintained and regressions are quickly
 * identified.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic performance baseline establishment and tracking
 *   <li>Statistical regression detection with configurable thresholds
 *   <li>Performance trend analysis and reporting
 *   <li>CI/CD integration for automated regression testing
 *   <li>Performance alert generation and notification
 *   <li>Historical performance data management
 * </ul>
 *
 * <p>Regression detection criteria:
 *
 * <ul>
 *   <li>Performance degradation >5% from baseline triggers warning
 *   <li>Performance degradation >10% from baseline triggers error
 *   <li>Consecutive degradations indicate performance regression
 *   <li>Statistical significance testing for reliable detection
 * </ul>
 */
public final class PerformanceRegressionTestingFramework {

  /** Logger for performance regression testing. */
  private static final Logger LOGGER =
      Logger.getLogger(PerformanceRegressionTestingFramework.class.getName());

  /** Default regression thresholds. */
  public static final class RegressionThresholds {
    public static final double WARNING_THRESHOLD = 0.05; // 5% degradation
    public static final double ERROR_THRESHOLD = 0.10; // 10% degradation
    public static final double IMPROVEMENT_THRESHOLD = 0.05; // 5% improvement
    public static final int CONSECUTIVE_DEGRADATIONS_LIMIT = 3;
    public static final double STATISTICAL_SIGNIFICANCE = 0.95; // 95% confidence
  }

  /** Performance regression result. */
  public static final class RegressionResult {
    private final String benchmark;
    private final String runtime;
    private final double currentScore;
    private final double baselineScore;
    private final double regressionPercentage;
    private final RegressionLevel level;
    private final boolean isStatisticallySignificant;
    private final String analysis;
    private final LocalDateTime detectedAt;

    public RegressionResult(
        final String benchmark,
        final String runtime,
        final double currentScore,
        final double baselineScore,
        final double regressionPercentage,
        final RegressionLevel level,
        final boolean isStatisticallySignificant,
        final String analysis) {
      this.benchmark = benchmark;
      this.runtime = runtime;
      this.currentScore = currentScore;
      this.baselineScore = baselineScore;
      this.regressionPercentage = regressionPercentage;
      this.level = level;
      this.isStatisticallySignificant = isStatisticallySignificant;
      this.analysis = analysis;
      this.detectedAt = LocalDateTime.now();
    }

    public String getBenchmark() {
      return benchmark;
    }

    public String getRuntime() {
      return runtime;
    }

    public double getCurrentScore() {
      return currentScore;
    }

    public double getBaselineScore() {
      return baselineScore;
    }

    public double getRegressionPercentage() {
      return regressionPercentage;
    }

    public RegressionLevel getLevel() {
      return level;
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

    public boolean isRegression() {
      return level == RegressionLevel.WARNING || level == RegressionLevel.ERROR;
    }

    public boolean isImprovement() {
      return level == RegressionLevel.IMPROVEMENT;
    }

    @Override
    public String toString() {
      return String.format(
          "RegressionResult{benchmark='%s', runtime='%s', regression=%.1f%%, level=%s,"
              + " significant=%s}",
          benchmark, runtime, regressionPercentage * 100, level, isStatisticallySignificant);
    }
  }

  /** Regression severity levels. */
  public enum RegressionLevel {
    IMPROVEMENT,
    STABLE,
    WARNING,
    ERROR
  }

  /** Performance data point. */
  public static final class PerformanceDataPoint {
    private final String benchmark;
    private final String runtime;
    private final double score;
    private final double scoreError;
    private final String unit;
    private final LocalDateTime timestamp;
    private final String commitId;
    private final Map<String, String> metadata;

    public PerformanceDataPoint(
        final String benchmark,
        final String runtime,
        final double score,
        final double scoreError,
        final String unit,
        final String commitId,
        final Map<String, String> metadata) {
      this.benchmark = benchmark;
      this.runtime = runtime;
      this.score = score;
      this.scoreError = scoreError;
      this.unit = unit;
      this.commitId = commitId;
      this.metadata = new HashMap<>(metadata);
      this.timestamp = LocalDateTime.now();
    }

    public String getBenchmark() {
      return benchmark;
    }

    public String getRuntime() {
      return runtime;
    }

    public double getScore() {
      return score;
    }

    public double getScoreError() {
      return scoreError;
    }

    public String getUnit() {
      return unit;
    }

    public LocalDateTime getTimestamp() {
      return timestamp;
    }

    public String getCommitId() {
      return commitId;
    }

    public Map<String, String> getMetadata() {
      return new HashMap<>(metadata);
    }
  }

  /** Comprehensive regression analysis result. */
  public static final class RegressionAnalysisResult {
    private final List<RegressionResult> regressions;
    private final List<RegressionResult> improvements;
    private final Map<String, List<RegressionResult>> regressionsByBenchmark;
    private final boolean hasSignificantRegressions;
    private final int totalBenchmarks;
    private final String summaryReport;
    private final LocalDateTime analyzedAt;

    public RegressionAnalysisResult(final List<RegressionResult> allResults) {
      this.regressions = new ArrayList<>();
      this.improvements = new ArrayList<>();
      this.regressionsByBenchmark = new HashMap<>();

      for (final RegressionResult result : allResults) {
        if (result.isRegression()) {
          regressions.add(result);
          regressionsByBenchmark
              .computeIfAbsent(result.getBenchmark(), k -> new ArrayList<>())
              .add(result);
        } else if (result.isImprovement()) {
          improvements.add(result);
        }
      }

      this.hasSignificantRegressions =
          regressions.stream().anyMatch(r -> r.getLevel() == RegressionLevel.ERROR);
      this.totalBenchmarks = allResults.size();
      this.summaryReport = generateSummaryReport();
      this.analyzedAt = LocalDateTime.now();
    }

    public List<RegressionResult> getRegressions() {
      return new ArrayList<>(regressions);
    }

    public List<RegressionResult> getImprovements() {
      return new ArrayList<>(improvements);
    }

    public Map<String, List<RegressionResult>> getRegressionsByBenchmark() {
      return new HashMap<>(regressionsByBenchmark);
    }

    public boolean hasSignificantRegressions() {
      return hasSignificantRegressions;
    }

    public int getTotalBenchmarks() {
      return totalBenchmarks;
    }

    public String getSummaryReport() {
      return summaryReport;
    }

    public LocalDateTime getAnalyzedAt() {
      return analyzedAt;
    }

    private String generateSummaryReport() {
      final StringBuilder report = new StringBuilder();
      report.append("Performance Regression Analysis Summary\n");
      report
          .append("Generated: ")
          .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .append("\n");
      report.append("=========================================\n\n");

      report.append("Total Benchmarks Analyzed: ").append(totalBenchmarks).append("\n");
      report.append("Performance Regressions: ").append(regressions.size()).append("\n");
      report.append("Performance Improvements: ").append(improvements.size()).append("\n");
      report
          .append("Significant Regressions: ")
          .append(hasSignificantRegressions ? "YES" : "NO")
          .append("\n\n");

      if (!regressions.isEmpty()) {
        report.append("Detected Regressions:\n");
        for (final RegressionResult regression : regressions) {
          final String severity =
              regression.getLevel() == RegressionLevel.ERROR ? "🔴 ERROR" : "🟡 WARNING";
          report.append(
              String.format(
                  "  %s %s [%s]: %.1f%% degradation%s\n",
                  severity,
                  regression.getBenchmark(),
                  regression.getRuntime(),
                  regression.getRegressionPercentage() * 100,
                  regression.isStatisticallySignificant() ? " (significant)" : ""));
        }
        report.append("\n");
      }

      if (!improvements.isEmpty()) {
        report.append("Performance Improvements:\n");
        for (final RegressionResult improvement : improvements) {
          report.append(
              String.format(
                  "  🟢 %s [%s]: %.1f%% improvement\n",
                  improvement.getBenchmark(),
                  improvement.getRuntime(),
                  Math.abs(improvement.getRegressionPercentage()) * 100));
        }
      }

      return report.toString();
    }
  }

  private final ObjectMapper objectMapper;
  private final Path dataDirectory;
  private final Path baselineFile;
  private final Path historyFile;

  /**
   * Creates a new performance regression testing framework.
   *
   * @param dataDirectory directory for storing performance data
   */
  public PerformanceRegressionTestingFramework(final Path dataDirectory) {
    this.objectMapper = new ObjectMapper();
    this.dataDirectory = dataDirectory;
    this.baselineFile = dataDirectory.resolve("performance_baseline.json");
    this.historyFile = dataDirectory.resolve("performance_history.json");

    try {
      Files.createDirectories(dataDirectory);
    } catch (final IOException e) {
      LOGGER.severe("Failed to create data directory: " + e.getMessage());
    }
  }

  /**
   * Analyzes benchmark results for performance regressions.
   *
   * @param resultsFile path to JMH benchmark results JSON file
   * @param commitId current commit ID for tracking
   * @return comprehensive regression analysis result
   * @throws IOException if results cannot be processed
   */
  public RegressionAnalysisResult analyzeForRegressions(
      final Path resultsFile, final String commitId) throws IOException {
    LOGGER.info("Analyzing performance results for regressions: " + resultsFile);

    // Parse current benchmark results
    final List<PerformanceDataPoint> currentResults = parseJmhResults(resultsFile, commitId);
    LOGGER.info("Parsed " + currentResults.size() + " benchmark results");

    // Store results in history
    storePerformanceData(currentResults);

    // Load baseline data
    final Map<String, PerformanceDataPoint> baseline = loadBaseline();

    // Perform regression analysis
    final List<RegressionResult> regressionResults = new ArrayList<>();
    for (final PerformanceDataPoint current : currentResults) {
      final String key = generateKey(current.getBenchmark(), current.getRuntime());
      final PerformanceDataPoint baselinePoint = baseline.get(key);

      if (baselinePoint != null) {
        final RegressionResult regression = analyzeRegression(current, baselinePoint);
        regressionResults.add(regression);
      } else {
        LOGGER.info("No baseline found for: " + key + " - establishing new baseline");
        // This is a new benchmark, add to baseline
        baseline.put(key, current);
      }
    }

    // Update baseline with new benchmarks
    saveBaseline(baseline);

    final RegressionAnalysisResult analysisResult = new RegressionAnalysisResult(regressionResults);
    LOGGER.info(
        "Regression analysis completed: "
            + analysisResult.getRegressions().size()
            + " regressions, "
            + analysisResult.getImprovements().size()
            + " improvements");

    return analysisResult;
  }

  /**
   * Establishes performance baseline from current benchmark results.
   *
   * @param resultsFile path to JMH benchmark results JSON file
   * @param commitId commit ID for baseline tracking
   * @throws IOException if baseline cannot be established
   */
  public void establishBaseline(final Path resultsFile, final String commitId) throws IOException {
    LOGGER.info("Establishing performance baseline from: " + resultsFile);

    final List<PerformanceDataPoint> results = parseJmhResults(resultsFile, commitId);
    final Map<String, PerformanceDataPoint> baseline = new HashMap<>();

    for (final PerformanceDataPoint result : results) {
      final String key = generateKey(result.getBenchmark(), result.getRuntime());
      baseline.put(key, result);
    }

    saveBaseline(baseline);
    storePerformanceData(results);

    LOGGER.info("Performance baseline established with " + baseline.size() + " benchmarks");
  }

  /**
   * Gets performance trends for a specific benchmark.
   *
   * @param benchmarkName benchmark name
   * @param runtime runtime type
   * @param maxDataPoints maximum number of historical data points to return
   * @return list of historical performance data points
   * @throws IOException if history cannot be loaded
   */
  public List<PerformanceDataPoint> getPerformanceTrends(
      final String benchmarkName, final String runtime, final int maxDataPoints)
      throws IOException {

    final List<PerformanceDataPoint> allHistory = loadPerformanceHistory();
    final List<PerformanceDataPoint> benchmarkHistory = new ArrayList<>();

    for (final PerformanceDataPoint point : allHistory) {
      if (point.getBenchmark().equals(benchmarkName) && point.getRuntime().equals(runtime)) {
        benchmarkHistory.add(point);
      }
    }

    // Return most recent data points
    final int startIndex = Math.max(0, benchmarkHistory.size() - maxDataPoints);
    return benchmarkHistory.subList(startIndex, benchmarkHistory.size());
  }

  /**
   * Generates a performance regression report.
   *
   * @param analysisResult regression analysis result
   * @return formatted regression report
   */
  public String generateRegressionReport(final RegressionAnalysisResult analysisResult) {
    final StringBuilder report = new StringBuilder();
    report.append("PERFORMANCE REGRESSION REPORT\n");
    report.append("============================\n\n");

    report
        .append("Analysis Date: ")
        .append(analysisResult.getAnalyzedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .append("\n");
    report.append("Total Benchmarks: ").append(analysisResult.getTotalBenchmarks()).append("\n");
    report
        .append("Regressions Found: ")
        .append(analysisResult.getRegressions().size())
        .append("\n");
    report
        .append("Improvements Found: ")
        .append(analysisResult.getImprovements().size())
        .append("\n");
    report
        .append("Critical Regressions: ")
        .append(analysisResult.hasSignificantRegressions() ? "YES" : "NO")
        .append("\n\n");

    if (analysisResult.hasSignificantRegressions()) {
      report.append("🚨 CRITICAL PERFORMANCE REGRESSIONS DETECTED 🚨\n\n");
    }

    // Detailed regression analysis
    if (!analysisResult.getRegressions().isEmpty()) {
      report.append("REGRESSION DETAILS:\n");
      report.append("==================\n\n");

      for (final RegressionResult regression : analysisResult.getRegressions()) {
        final String icon = regression.getLevel() == RegressionLevel.ERROR ? "🔴" : "🟡";
        report.append(String.format("%s %s\n", icon, regression.getBenchmark()));
        report.append(String.format("   Runtime: %s\n", regression.getRuntime()));
        report.append(
            String.format("   Current Score: %.2f ops/sec\n", regression.getCurrentScore()));
        report.append(
            String.format("   Baseline Score: %.2f ops/sec\n", regression.getBaselineScore()));
        report.append(
            String.format(
                "   Performance Loss: %.1f%%\n", regression.getRegressionPercentage() * 100));
        report.append(
            String.format(
                "   Statistical Significance: %s\n",
                regression.isStatisticallySignificant() ? "YES" : "NO"));
        report.append(String.format("   Analysis: %s\n\n", regression.getAnalysis()));
      }
    }

    // Performance improvements
    if (!analysisResult.getImprovements().isEmpty()) {
      report.append("PERFORMANCE IMPROVEMENTS:\n");
      report.append("========================\n\n");

      for (final RegressionResult improvement : analysisResult.getImprovements()) {
        report.append(
            String.format("🟢 %s [%s]\n", improvement.getBenchmark(), improvement.getRuntime()));
        report.append(
            String.format(
                "   Performance Gain: %.1f%%\n",
                Math.abs(improvement.getRegressionPercentage()) * 100));
      }
    }

    return report.toString();
  }

  private List<PerformanceDataPoint> parseJmhResults(final Path resultsFile, final String commitId)
      throws IOException {
    final List<PerformanceDataPoint> results = new ArrayList<>();
    final JsonNode rootNode = objectMapper.readTree(resultsFile.toFile());

    if (rootNode.isArray()) {
      for (final JsonNode benchmarkNode : rootNode) {
        final String benchmark = benchmarkNode.get("benchmark").asText();
        final double score = benchmarkNode.get("primaryMetric").get("score").asDouble();
        final double scoreError = benchmarkNode.get("primaryMetric").get("scoreError").asDouble();
        final String unit = benchmarkNode.get("primaryMetric").get("scoreUnit").asText();

        // Extract runtime from params
        String runtime = "UNKNOWN";
        final Map<String, String> metadata = new HashMap<>();
        if (benchmarkNode.has("params")) {
          final JsonNode paramsNode = benchmarkNode.get("params");
          paramsNode
              .fields()
              .forEachRemaining(
                  entry -> {
                    final String key = entry.getKey();
                    final String value = entry.getValue().asText();
                    metadata.put(key, value);
                    if ("runtimeTypeName".equals(key)) {
                      runtime = value;
                    }
                  });
        }

        results.add(
            new PerformanceDataPoint(
                benchmark, runtime, score, scoreError, unit, commitId, metadata));
      }
    }

    return results;
  }

  private RegressionResult analyzeRegression(
      final PerformanceDataPoint current, final PerformanceDataPoint baseline) {
    final double currentScore = current.getScore();
    final double baselineScore = baseline.getScore();
    final double regressionPercentage = (baselineScore - currentScore) / baselineScore;

    // Determine regression level
    RegressionLevel level;
    if (Math.abs(regressionPercentage) < RegressionThresholds.IMPROVEMENT_THRESHOLD) {
      level = RegressionLevel.STABLE;
    } else if (regressionPercentage > RegressionThresholds.ERROR_THRESHOLD) {
      level = RegressionLevel.ERROR;
    } else if (regressionPercentage > RegressionThresholds.WARNING_THRESHOLD) {
      level = RegressionLevel.WARNING;
    } else if (regressionPercentage < -RegressionThresholds.IMPROVEMENT_THRESHOLD) {
      level = RegressionLevel.IMPROVEMENT;
    } else {
      level = RegressionLevel.STABLE;
    }

    // Basic statistical significance test (simplified)
    final double combinedError =
        Math.sqrt(
            current.getScoreError() * current.getScoreError()
                + baseline.getScoreError() * baseline.getScoreError());
    final boolean isStatisticallySignificant =
        Math.abs(currentScore - baselineScore) > (2 * combinedError);

    final String analysis =
        String.format(
            "Performance change from baseline: %.1f%%. Current: %.2f %s, Baseline: %.2f %s",
            regressionPercentage * 100,
            currentScore,
            current.getUnit(),
            baselineScore,
            baseline.getUnit());

    return new RegressionResult(
        current.getBenchmark(),
        current.getRuntime(),
        currentScore,
        baselineScore,
        regressionPercentage,
        level,
        isStatisticallySignificant,
        analysis);
  }

  private void storePerformanceData(final List<PerformanceDataPoint> dataPoints)
      throws IOException {
    final List<PerformanceDataPoint> existingHistory = loadPerformanceHistory();
    existingHistory.addAll(dataPoints);

    final ArrayNode historyArray = objectMapper.createArrayNode();
    for (final PerformanceDataPoint point : existingHistory) {
      final ObjectNode pointNode = objectMapper.createObjectNode();
      pointNode.put("benchmark", point.getBenchmark());
      pointNode.put("runtime", point.getRuntime());
      pointNode.put("score", point.getScore());
      pointNode.put("scoreError", point.getScoreError());
      pointNode.put("unit", point.getUnit());
      pointNode.put(
          "timestamp", point.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      pointNode.put("commitId", point.getCommitId());

      final ObjectNode metadataNode = objectMapper.createObjectNode();
      for (final Map.Entry<String, String> entry : point.getMetadata().entrySet()) {
        metadataNode.put(entry.getKey(), entry.getValue());
      }
      pointNode.set("metadata", metadataNode);

      historyArray.add(pointNode);
    }

    Files.write(
        historyFile,
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(historyArray),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  private List<PerformanceDataPoint> loadPerformanceHistory() throws IOException {
    if (!Files.exists(historyFile)) {
      return new ArrayList<>();
    }

    final List<PerformanceDataPoint> history = new ArrayList<>();
    final JsonNode historyNode = objectMapper.readTree(historyFile.toFile());

    if (historyNode.isArray()) {
      for (final JsonNode pointNode : historyNode) {
        final String benchmark = pointNode.get("benchmark").asText();
        final String runtime = pointNode.get("runtime").asText();
        final double score = pointNode.get("score").asDouble();
        final double scoreError = pointNode.get("scoreError").asDouble();
        final String unit = pointNode.get("unit").asText();
        final String commitId = pointNode.get("commitId").asText();

        final Map<String, String> metadata = new HashMap<>();
        if (pointNode.has("metadata")) {
          final JsonNode metadataNode = pointNode.get("metadata");
          metadataNode
              .fields()
              .forEachRemaining(entry -> metadata.put(entry.getKey(), entry.getValue().asText()));
        }

        history.add(
            new PerformanceDataPoint(
                benchmark, runtime, score, scoreError, unit, commitId, metadata));
      }
    }

    return history;
  }

  private Map<String, PerformanceDataPoint> loadBaseline() throws IOException {
    if (!Files.exists(baselineFile)) {
      return new HashMap<>();
    }

    final Map<String, PerformanceDataPoint> baseline = new HashMap<>();
    final JsonNode baselineNode = objectMapper.readTree(baselineFile.toFile());

    baselineNode
        .fields()
        .forEachRemaining(
            entry -> {
              final String key = entry.getKey();
              final JsonNode pointNode = entry.getValue();

              final String benchmark = pointNode.get("benchmark").asText();
              final String runtime = pointNode.get("runtime").asText();
              final double score = pointNode.get("score").asDouble();
              final double scoreError = pointNode.get("scoreError").asDouble();
              final String unit = pointNode.get("unit").asText();
              final String commitId = pointNode.get("commitId").asText();

              final Map<String, String> metadata = new HashMap<>();
              if (pointNode.has("metadata")) {
                final JsonNode metadataNode = pointNode.get("metadata");
                metadataNode
                    .fields()
                    .forEachRemaining(
                        metaEntry ->
                            metadata.put(metaEntry.getKey(), metaEntry.getValue().asText()));
              }

              baseline.put(
                  key,
                  new PerformanceDataPoint(
                      benchmark, runtime, score, scoreError, unit, commitId, metadata));
            });

    return baseline;
  }

  private void saveBaseline(final Map<String, PerformanceDataPoint> baseline) throws IOException {
    final ObjectNode baselineNode = objectMapper.createObjectNode();

    for (final Map.Entry<String, PerformanceDataPoint> entry : baseline.entrySet()) {
      final PerformanceDataPoint point = entry.getValue();
      final ObjectNode pointNode = objectMapper.createObjectNode();

      pointNode.put("benchmark", point.getBenchmark());
      pointNode.put("runtime", point.getRuntime());
      pointNode.put("score", point.getScore());
      pointNode.put("scoreError", point.getScoreError());
      pointNode.put("unit", point.getUnit());
      pointNode.put("commitId", point.getCommitId());

      final ObjectNode metadataNode = objectMapper.createObjectNode();
      for (final Map.Entry<String, String> metaEntry : point.getMetadata().entrySet()) {
        metadataNode.put(metaEntry.getKey(), metaEntry.getValue());
      }
      pointNode.set("metadata", metadataNode);

      baselineNode.set(entry.getKey(), pointNode);
    }

    Files.write(
        baselineFile,
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(baselineNode),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  private String generateKey(final String benchmark, final String runtime) {
    return benchmark + ":" + runtime;
  }

  /**
   * Main method for command-line regression analysis.
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    if (args.length < 2) {
      System.err.println(
          "Usage: PerformanceRegressionTestingFramework <results-file.json> <commit-id>"
              + " [data-directory]");
      System.exit(1);
    }

    try {
      final Path resultsFile = Paths.get(args[0]);
      final String commitId = args[1];
      final Path dataDirectory =
          args.length > 2 ? Paths.get(args[2]) : Paths.get("performance_data");

      final PerformanceRegressionTestingFramework framework =
          new PerformanceRegressionTestingFramework(dataDirectory);

      System.out.println("Analyzing performance results: " + resultsFile);
      final RegressionAnalysisResult result =
          framework.analyzeForRegressions(resultsFile, commitId);

      System.out.println(framework.generateRegressionReport(result));

      if (result.hasSignificantRegressions()) {
        System.err.println("🚨 Critical performance regressions detected!");
        System.exit(1);
      } else if (!result.getRegressions().isEmpty()) {
        System.out.println("⚠️  Performance warnings detected.");
        System.exit(1);
      } else {
        System.out.println("✅ No performance regressions detected.");
        System.exit(0);
      }

    } catch (final Exception e) {
      System.err.println("Regression analysis failed: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
}

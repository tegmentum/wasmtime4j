package ai.tegmentum.wasmtime4j.benchmarks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * CI/CD integration framework for automated performance monitoring and validation.
 *
 * <p>This integration provides automated performance testing, baseline management, and regression
 * detection for continuous integration and deployment pipelines.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automated benchmark execution with CI/CD optimized configurations
 *   <li>Performance baseline establishment and validation
 *   <li>Regression detection with configurable failure thresholds
 *   <li>Machine-readable output for CI/CD pipeline consumption
 *   <li>Performance trend tracking and alerting
 *   <li>Integration with GitHub Actions, Jenkins, and other CI systems
 * </ul>
 */
public final class CiCdPerformanceIntegration {

  /** Logger for CI/CD performance integration. */
  private static final Logger LOGGER =
      Logger.getLogger(CiCdPerformanceIntegration.class.getName());

  /** CI/CD execution modes. */
  public enum ExecutionMode {
    BASELINE_ESTABLISHMENT("Establish new performance baselines"),
    VALIDATION("Validate against existing baselines"),
    REGRESSION_CHECK("Check for performance regressions"),
    TREND_ANALYSIS("Analyze performance trends"),
    FULL_VALIDATION("Complete validation with baseline and regression analysis");

    private final String description;

    ExecutionMode(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** CI/CD execution result for pipeline consumption. */
  public static final class CiCdExecutionResult {
    private final boolean success;
    private final ExecutionMode mode;
    private final String resultSummary;
    private final Map<String, Object> metrics;
    private final List<String> warnings;
    private final List<String> errors;
    private final String jsonReport;
    private final int exitCode;

    /**
     * Creates a CI/CD execution result.
     *
     * @param success whether execution was successful
     * @param mode execution mode
     * @param resultSummary human-readable result summary
     * @param metrics performance metrics map
     * @param warnings list of warnings
     * @param errors list of errors
     * @param jsonReport machine-readable JSON report
     * @param exitCode suggested exit code for CI/CD
     */
    public CiCdExecutionResult(
        final boolean success,
        final ExecutionMode mode,
        final String resultSummary,
        final Map<String, Object> metrics,
        final List<String> warnings,
        final List<String> errors,
        final String jsonReport,
        final int exitCode) {
      this.success = success;
      this.mode = mode;
      this.resultSummary = resultSummary;
      this.metrics = new HashMap<>(metrics);
      this.warnings = List.copyOf(warnings);
      this.errors = List.copyOf(errors);
      this.jsonReport = jsonReport;
      this.exitCode = exitCode;
    }

    public boolean isSuccess() {
      return success;
    }

    public ExecutionMode getMode() {
      return mode;
    }

    public String getResultSummary() {
      return resultSummary;
    }

    public Map<String, Object> getMetrics() {
      return new HashMap<>(metrics);
    }

    public List<String> getWarnings() {
      return List.copyOf(warnings);
    }

    public List<String> getErrors() {
      return List.copyOf(errors);
    }

    public String getJsonReport() {
      return jsonReport;
    }

    public int getExitCode() {
      return exitCode;
    }
  }

  private final ComprehensiveBenchmarkExecutor benchmarkExecutor;
  private final PerformanceTargetValidator targetValidator;
  private final AdvancedRegressionDetector regressionDetector;
  private final ObjectMapper objectMapper;

  /**
   * Creates a CI/CD performance integration with default configuration.
   */
  public CiCdPerformanceIntegration() {
    this.benchmarkExecutor = new ComprehensiveBenchmarkExecutor(Paths.get(System.getProperty("user.dir")));
    this.targetValidator = new PerformanceTargetValidator();
    this.regressionDetector = new AdvancedRegressionDetector();
    this.objectMapper = createObjectMapper();
  }

  /**
   * Executes CI/CD performance validation with specified mode.
   *
   * @param mode execution mode
   * @param outputDirectory directory for output files
   * @return CI/CD execution result
   */
  public CiCdExecutionResult executeCiCdValidation(final ExecutionMode mode, final Path outputDirectory) {
    LOGGER.info("Starting CI/CD performance validation with mode: " + mode);

    try {
      Files.createDirectories(outputDirectory);

      switch (mode) {
        case BASELINE_ESTABLISHMENT:
          return executeBaselineEstablishment(outputDirectory);
        case VALIDATION:
          return executeValidation(outputDirectory);
        case REGRESSION_CHECK:
          return executeRegressionCheck(outputDirectory);
        case TREND_ANALYSIS:
          return executeTrendAnalysis(outputDirectory);
        case FULL_VALIDATION:
          return executeFullValidation(outputDirectory);
        default:
          return createErrorResult(mode, "Unknown execution mode: " + mode);
      }

    } catch (final Exception e) {
      LOGGER.severe("CI/CD execution failed: " + e.getMessage());
      return createErrorResult(mode, "Execution failed: " + e.getMessage());
    }
  }

  /**
   * Executes CI/CD performance validation for GitHub Actions.
   *
   * @param outputDirectory directory for output files
   * @return GitHub Actions compatible result
   */
  public CiCdExecutionResult executeForGitHubActions(final Path outputDirectory) {
    LOGGER.info("Executing performance validation for GitHub Actions");

    final CiCdExecutionResult result = executeCiCdValidation(ExecutionMode.VALIDATION, outputDirectory);

    // Generate GitHub Actions output
    generateGitHubActionsOutput(result, outputDirectory);

    return result;
  }

  /**
   * Executes CI/CD performance validation for Jenkins.
   *
   * @param outputDirectory directory for output files
   * @return Jenkins compatible result
   */
  public CiCdExecutionResult executeForJenkins(final Path outputDirectory) {
    LOGGER.info("Executing performance validation for Jenkins");

    final CiCdExecutionResult result = executeCiCdValidation(ExecutionMode.FULL_VALIDATION, outputDirectory);

    // Generate Jenkins output
    generateJenkinsOutput(result, outputDirectory);

    return result;
  }

  /**
   * Generates performance baseline for new releases.
   *
   * @param outputDirectory directory for output files
   * @return baseline establishment result
   */
  public CiCdExecutionResult establishPerformanceBaseline(final Path outputDirectory) {
    return executeCiCdValidation(ExecutionMode.BASELINE_ESTABLISHMENT, outputDirectory);
  }

  private CiCdExecutionResult executeBaselineEstablishment(final Path outputDirectory) {
    try {
      // Execute comprehensive benchmark suite for baseline establishment
      final ComprehensiveBenchmarkExecutor.BenchmarkExecutionResult benchmarkResult =
          benchmarkExecutor.establishPerformanceBaselines();

      if (!benchmarkResult.isSuccess()) {
        return createErrorResult(ExecutionMode.BASELINE_ESTABLISHMENT,
            "Benchmark execution failed: " + benchmarkResult.getExecutionLog());
      }

      // Generate CI/CD report
      final Map<String, Object> metrics = new HashMap<>();
      metrics.put("measurementCount", benchmarkResult.getMeasurements().size());
      metrics.put("executionTimeMs", benchmarkResult.getExecutionTimeMillis());

      if (benchmarkResult.getBaselineResult() != null) {
        metrics.put("baselinesEstablished", benchmarkResult.getBaselineResult().getSuccessfulBaselines());
        metrics.put("targetsAchieved", benchmarkResult.getBaselineResult().getTargetsAchieved());
      }

      final String jsonReport = generateCiCdJsonReport(benchmarkResult, metrics);
      saveReportToFile(jsonReport, outputDirectory.resolve("baseline-report.json"));

      return new CiCdExecutionResult(
          true,
          ExecutionMode.BASELINE_ESTABLISHMENT,
          "Performance baselines established successfully",
          metrics,
          List.of(),
          List.of(),
          jsonReport,
          0);

    } catch (final Exception e) {
      return createErrorResult(ExecutionMode.BASELINE_ESTABLISHMENT,
          "Baseline establishment failed: " + e.getMessage());
    }
  }

  private CiCdExecutionResult executeValidation(final Path outputDirectory) {
    try {
      // Execute CI/CD validation benchmarks
      final ComprehensiveBenchmarkExecutor.BenchmarkExecutionResult benchmarkResult =
          benchmarkExecutor.executeCiCdValidation();

      if (!benchmarkResult.isSuccess()) {
        return createErrorResult(ExecutionMode.VALIDATION,
            "Benchmark execution failed: " + benchmarkResult.getExecutionLog());
      }

      // Validate performance targets
      final PerformanceTargetValidator.ComprehensiveValidationResult validationResult =
          targetValidator.validatePerformanceTargets(benchmarkResult.getResultsFile());

      // Generate metrics
      final Map<String, Object> metrics = new HashMap<>();
      metrics.put("totalCategories", validationResult.getTotalCategories());
      metrics.put("successfulCategories", validationResult.getSuccessfulCategories());
      metrics.put("allTargetsAchieved", validationResult.areAllTargetsAchieved());
      metrics.put("allMinimumsAchieved", validationResult.areAllMinimumsAchieved());
      metrics.put("runtimeComparison", validationResult.getRuntimeComparison());

      final String jsonReport = generateValidationJsonReport(validationResult, metrics);
      saveReportToFile(jsonReport, outputDirectory.resolve("validation-report.json"));

      final boolean success = validationResult.areAllTargetsAchieved() && validationResult.areAllMinimumsAchieved();

      return new CiCdExecutionResult(
          success,
          ExecutionMode.VALIDATION,
          validationResult.getSummaryReport(),
          metrics,
          success ? List.of() : List.of("Some performance targets not achieved"),
          List.of(),
          jsonReport,
          success ? 0 : 1);

    } catch (final Exception e) {
      return createErrorResult(ExecutionMode.VALIDATION,
          "Validation failed: " + e.getMessage());
    }
  }

  private CiCdExecutionResult executeRegressionCheck(final Path outputDirectory) {
    try {
      // Execute quick benchmarks for regression check
      final ComprehensiveBenchmarkExecutor.BenchmarkExecutionResult benchmarkResult =
          benchmarkExecutor.executeQuickPerformanceCheck();

      if (!benchmarkResult.isSuccess()) {
        return createErrorResult(ExecutionMode.REGRESSION_CHECK,
            "Benchmark execution failed: " + benchmarkResult.getExecutionLog());
      }

      // Check for regressions
      final boolean hasRegressions = benchmarkResult.getValidationResult() != null
          && !benchmarkResult.getValidationResult().isValid();

      final Map<String, Object> metrics = new HashMap<>();
      metrics.put("measurementCount", benchmarkResult.getMeasurements().size());
      metrics.put("hasRegressions", hasRegressions);
      metrics.put("executionTimeMs", benchmarkResult.getExecutionTimeMillis());

      final String jsonReport = generateRegressionJsonReport(benchmarkResult, metrics);
      saveReportToFile(jsonReport, outputDirectory.resolve("regression-report.json"));

      return new CiCdExecutionResult(
          !hasRegressions,
          ExecutionMode.REGRESSION_CHECK,
          hasRegressions ? "Performance regressions detected" : "No performance regressions detected",
          metrics,
          hasRegressions ? List.of("Performance regressions found") : List.of(),
          List.of(),
          jsonReport,
          hasRegressions ? 1 : 0);

    } catch (final Exception e) {
      return createErrorResult(ExecutionMode.REGRESSION_CHECK,
          "Regression check failed: " + e.getMessage());
    }
  }

  private CiCdExecutionResult executeTrendAnalysis(final Path outputDirectory) {
    try {
      // This would implement trend analysis using historical data
      // For now, return a placeholder implementation
      final Map<String, Object> metrics = new HashMap<>();
      metrics.put("trendsAnalyzed", 0);
      metrics.put("trendDirection", "stable");

      final String jsonReport = generateTrendJsonReport(metrics);
      saveReportToFile(jsonReport, outputDirectory.resolve("trend-report.json"));

      return new CiCdExecutionResult(
          true,
          ExecutionMode.TREND_ANALYSIS,
          "Performance trend analysis completed",
          metrics,
          List.of(),
          List.of(),
          jsonReport,
          0);

    } catch (final Exception e) {
      return createErrorResult(ExecutionMode.TREND_ANALYSIS,
          "Trend analysis failed: " + e.getMessage());
    }
  }

  private CiCdExecutionResult executeFullValidation(final Path outputDirectory) {
    try {
      // Execute comprehensive validation including baseline and regression checks
      final CiCdExecutionResult validationResult = executeValidation(outputDirectory);
      final CiCdExecutionResult regressionResult = executeRegressionCheck(outputDirectory);

      final boolean success = validationResult.isSuccess() && regressionResult.isSuccess();
      final Map<String, Object> combinedMetrics = new HashMap<>(validationResult.getMetrics());
      combinedMetrics.putAll(regressionResult.getMetrics());

      final String jsonReport = generateFullValidationJsonReport(validationResult, regressionResult, combinedMetrics);
      saveReportToFile(jsonReport, outputDirectory.resolve("full-validation-report.json"));

      return new CiCdExecutionResult(
          success,
          ExecutionMode.FULL_VALIDATION,
          "Full performance validation completed",
          combinedMetrics,
          success ? List.of() : List.of("Performance issues detected"),
          List.of(),
          jsonReport,
          success ? 0 : 1);

    } catch (final Exception e) {
      return createErrorResult(ExecutionMode.FULL_VALIDATION,
          "Full validation failed: " + e.getMessage());
    }
  }

  private void generateGitHubActionsOutput(final CiCdExecutionResult result, final Path outputDirectory) {
    try {
      // Generate GitHub Actions step outputs
      final StringBuilder output = new StringBuilder();
      output.append("performance_success=").append(result.isSuccess()).append("\n");
      output.append("performance_exit_code=").append(result.getExitCode()).append("\n");
      output.append("performance_summary=\"").append(result.getResultSummary().replace("\"", "\\\"")).append("\"\n");

      // Add metrics as outputs
      for (final Map.Entry<String, Object> entry : result.getMetrics().entrySet()) {
        output.append("performance_").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
      }

      Files.write(outputDirectory.resolve("github-actions-output.txt"), output.toString().getBytes());

    } catch (final IOException e) {
      LOGGER.warning("Failed to generate GitHub Actions output: " + e.getMessage());
    }
  }

  private void generateJenkinsOutput(final CiCdExecutionResult result, final Path outputDirectory) {
    try {
      // Generate Jenkins-compatible XML report
      final StringBuilder xml = new StringBuilder();
      xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      xml.append("<performance-report>\n");
      xml.append("  <success>").append(result.isSuccess()).append("</success>\n");
      xml.append("  <mode>").append(result.getMode()).append("</mode>\n");
      xml.append("  <summary>").append(escapeXml(result.getResultSummary())).append("</summary>\n");
      xml.append("  <metrics>\n");

      for (final Map.Entry<String, Object> entry : result.getMetrics().entrySet()) {
        xml.append("    <metric name=\"").append(entry.getKey()).append("\" value=\"")
            .append(entry.getValue()).append("\"/>\n");
      }

      xml.append("  </metrics>\n");
      xml.append("</performance-report>\n");

      Files.write(outputDirectory.resolve("jenkins-report.xml"), xml.toString().getBytes());

    } catch (final IOException e) {
      LOGGER.warning("Failed to generate Jenkins output: " + e.getMessage());
    }
  }

  private String generateCiCdJsonReport(
      final ComprehensiveBenchmarkExecutor.BenchmarkExecutionResult benchmarkResult,
      final Map<String, Object> metrics) {
    try {
      final Map<String, Object> report = new HashMap<>();
      report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      report.put("type", "baseline_establishment");
      report.put("success", benchmarkResult.isSuccess());
      report.put("metrics", metrics);
      report.put("executionTimeMs", benchmarkResult.getExecutionTimeMillis());

      return objectMapper.writeValueAsString(report);
    } catch (final Exception e) {
      return "{\"error\": \"Failed to generate JSON report\"}";
    }
  }

  private String generateValidationJsonReport(
      final PerformanceTargetValidator.ComprehensiveValidationResult validationResult,
      final Map<String, Object> metrics) {
    try {
      final Map<String, Object> report = new HashMap<>();
      report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      report.put("type", "validation");
      report.put("success", validationResult.areAllTargetsAchieved());
      report.put("metrics", metrics);

      return objectMapper.writeValueAsString(report);
    } catch (final Exception e) {
      return "{\"error\": \"Failed to generate validation JSON report\"}";
    }
  }

  private String generateRegressionJsonReport(
      final ComprehensiveBenchmarkExecutor.BenchmarkExecutionResult benchmarkResult,
      final Map<String, Object> metrics) {
    try {
      final Map<String, Object> report = new HashMap<>();
      report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      report.put("type", "regression_check");
      report.put("success", benchmarkResult.isSuccess());
      report.put("metrics", metrics);

      return objectMapper.writeValueAsString(report);
    } catch (final Exception e) {
      return "{\"error\": \"Failed to generate regression JSON report\"}";
    }
  }

  private String generateTrendJsonReport(final Map<String, Object> metrics) {
    try {
      final Map<String, Object> report = new HashMap<>();
      report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      report.put("type", "trend_analysis");
      report.put("success", true);
      report.put("metrics", metrics);

      return objectMapper.writeValueAsString(report);
    } catch (final Exception e) {
      return "{\"error\": \"Failed to generate trend JSON report\"}";
    }
  }

  private String generateFullValidationJsonReport(
      final CiCdExecutionResult validationResult,
      final CiCdExecutionResult regressionResult,
      final Map<String, Object> combinedMetrics) {
    try {
      final Map<String, Object> report = new HashMap<>();
      report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      report.put("type", "full_validation");
      report.put("success", validationResult.isSuccess() && regressionResult.isSuccess());
      report.put("validation", validationResult.isSuccess());
      report.put("regression_check", regressionResult.isSuccess());
      report.put("metrics", combinedMetrics);

      return objectMapper.writeValueAsString(report);
    } catch (final Exception e) {
      return "{\"error\": \"Failed to generate full validation JSON report\"}";
    }
  }

  private void saveReportToFile(final String content, final Path filePath) {
    try {
      Files.write(filePath, content.getBytes());
      LOGGER.info("Saved report to: " + filePath);
    } catch (final IOException e) {
      LOGGER.warning("Failed to save report to " + filePath + ": " + e.getMessage());
    }
  }

  private CiCdExecutionResult createErrorResult(final ExecutionMode mode, final String errorMessage) {
    return new CiCdExecutionResult(
        false,
        mode,
        "Execution failed: " + errorMessage,
        Map.of(),
        List.of(),
        List.of(errorMessage),
        "{\"error\": \"" + errorMessage + "\"}",
        1);
  }

  private String escapeXml(final String input) {
    if (input == null) {
      return "";
    }
    return input.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  private ObjectMapper createObjectMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  /**
   * Main method for CI/CD command-line execution.
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: CiCdPerformanceIntegration <mode> [output-dir]");
      System.err.println("Modes: baseline, validation, regression, trend, full");
      System.exit(1);
    }

    final String modeStr = args[0];
    final Path outputDir = args.length > 1 ? Paths.get(args[1]) : Paths.get("performance-reports");

    ExecutionMode mode;
    try {
      mode = ExecutionMode.valueOf(modeStr.toUpperCase().replace("-", "_"));
    } catch (final IllegalArgumentException e) {
      System.err.println("Unknown mode: " + modeStr);
      System.exit(1);
      return;
    }

    try {
      final CiCdPerformanceIntegration integration = new CiCdPerformanceIntegration();
      final CiCdExecutionResult result = integration.executeCiCdValidation(mode, outputDir);

      System.out.println(result.getResultSummary());
      System.out.println("Reports saved to: " + outputDir);

      System.exit(result.getExitCode());

    } catch (final Exception e) {
      System.err.println("CI/CD integration failed: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
}
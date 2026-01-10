package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.RuntimeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Comprehensive CI/CD performance gate enforcement framework for Wasmtime4j.
 *
 * <p>This framework provides automated performance validation in CI/CD pipelines with configurable
 * performance gates, automated failure detection, and comprehensive reporting. It integrates with
 * CI/CD systems to enforce performance standards and prevent regressions.
 *
 * <p>Key features: - Configurable performance gates with multiple thresholds - Automated regression
 * detection and failure reporting - CI/CD pipeline integration with exit codes - Performance
 * baseline validation - Multi-runtime comparison gates - Comprehensive failure analysis and
 * recommendations - Integration with notification systems - Performance trend monitoring -
 * Configurable gate strictness levels
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms2g", "-Xmx2g"})
public class CiCdPerformanceGateEnforcement extends BenchmarkBase {

  private static final Logger LOGGER =
      Logger.getLogger(CiCdPerformanceGateEnforcement.class.getName());
  private static final ObjectMapper JSON_MAPPER = createJsonMapper();

  // CI/CD environment detection
  private static final String CI_COMMIT_SHA = getEnvironmentVariable("CI_COMMIT_SHA", "unknown");
  private static final String CI_PIPELINE_ID = getEnvironmentVariable("CI_PIPELINE_ID", "unknown");
  private static final String CI_JOB_NAME = getEnvironmentVariable("CI_JOB_NAME", "local");
  private static final String CI_BRANCH_NAME = getEnvironmentVariable("CI_COMMIT_REF_NAME", "main");

  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  /** Performance gate configuration. */
  public static final class PerformanceGateConfig {
    private final double regressionThreshold; // e.g., 0.05 = 5% regression triggers failure
    private final double warningThreshold; // e.g., 0.02 = 2% regression triggers warning
    private final double minimumScore; // Absolute minimum performance score
    private final int minimumSamples; // Minimum samples required for validation
    private final boolean strictMode; // Fail on any regression
    private final boolean enableTrending; // Enable trend analysis
    private final Map<String, Double> customThresholds; // Benchmark-specific thresholds

    private PerformanceGateConfig(final Builder builder) {
      this.regressionThreshold = builder.regressionThreshold;
      this.warningThreshold = builder.warningThreshold;
      this.minimumScore = builder.minimumScore;
      this.minimumSamples = builder.minimumSamples;
      this.strictMode = builder.strictMode;
      this.enableTrending = builder.enableTrending;
      this.customThresholds = new HashMap<>(builder.customThresholds);
    }

    // Getters
    public double getRegressionThreshold() {
      return regressionThreshold;
    }

    public double getWarningThreshold() {
      return warningThreshold;
    }

    public double getMinimumScore() {
      return minimumScore;
    }

    public int getMinimumSamples() {
      return minimumSamples;
    }

    public boolean isStrictMode() {
      return strictMode;
    }

    public boolean isEnableTrending() {
      return enableTrending;
    }

    public Map<String, Double> getCustomThresholds() {
      return new HashMap<>(customThresholds);
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private double regressionThreshold = 0.05; // 5% default
      private double warningThreshold = 0.02; // 2% default
      private double minimumScore = 0.0;
      private int minimumSamples = 3;
      private boolean strictMode = false;
      private boolean enableTrending = true;
      private Map<String, Double> customThresholds = new HashMap<>();

      public Builder regressionThreshold(final double threshold) {
        if (threshold <= 0 || threshold >= 1) {
          throw new IllegalArgumentException("Regression threshold must be between 0 and 1");
        }
        this.regressionThreshold = threshold;
        return this;
      }

      public Builder warningThreshold(final double threshold) {
        if (threshold <= 0 || threshold >= 1) {
          throw new IllegalArgumentException("Warning threshold must be between 0 and 1");
        }
        this.warningThreshold = threshold;
        return this;
      }

      public Builder minimumScore(final double score) {
        if (score < 0) {
          throw new IllegalArgumentException("Minimum score must be non-negative");
        }
        this.minimumScore = score;
        return this;
      }

      public Builder minimumSamples(final int samples) {
        if (samples <= 0) {
          throw new IllegalArgumentException("Minimum samples must be positive");
        }
        this.minimumSamples = samples;
        return this;
      }

      public Builder strictMode(final boolean strict) {
        this.strictMode = strict;
        return this;
      }

      public Builder enableTrending(final boolean trending) {
        this.enableTrending = trending;
        return this;
      }

      public Builder customThreshold(final String benchmarkName, final double threshold) {
        if (benchmarkName == null || benchmarkName.trim().isEmpty()) {
          throw new IllegalArgumentException("Benchmark name cannot be null or empty");
        }
        if (threshold <= 0 || threshold >= 1) {
          throw new IllegalArgumentException("Custom threshold must be between 0 and 1");
        }
        this.customThresholds.put(benchmarkName, threshold);
        return this;
      }

      public PerformanceGateConfig build() {
        return new PerformanceGateConfig(this);
      }
    }
  }

  /** Performance gate validation result. */
  public static final class PerformanceGateResult {
    private final String benchmarkName;
    private final String runtimeType;
    private final double currentScore;
    private final double baselineScore;
    private final double changePercent;
    private final GateStatus status;
    private final String message;
    private final LocalDateTime validatedAt;
    private final String commitSha;
    private final Map<String, Object> metadata;

    public PerformanceGateResult(
        final String benchmarkName,
        final String runtimeType,
        final double currentScore,
        final double baselineScore,
        final double changePercent,
        final GateStatus status,
        final String message,
        final String commitSha,
        final Map<String, Object> metadata) {
      this.benchmarkName = validateNotNull(benchmarkName, "benchmarkName");
      this.runtimeType = validateNotNull(runtimeType, "runtimeType");
      this.currentScore = validateFinite(currentScore, "currentScore");
      this.baselineScore = validateFinite(baselineScore, "baselineScore");
      this.changePercent = changePercent;
      this.status = validateNotNull(status, "status");
      this.message = message != null ? message : "";
      this.commitSha = commitSha != null ? commitSha : "unknown";
      this.validatedAt = LocalDateTime.now();
      this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    // Getters
    public String getBenchmarkName() {
      return benchmarkName;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public double getCurrentScore() {
      return currentScore;
    }

    public double getBaselineScore() {
      return baselineScore;
    }

    public double getChangePercent() {
      return changePercent;
    }

    public GateStatus getStatus() {
      return status;
    }

    public String getMessage() {
      return message;
    }

    public LocalDateTime getValidatedAt() {
      return validatedAt;
    }

    public String getCommitSha() {
      return commitSha;
    }

    public Map<String, Object> getMetadata() {
      return new HashMap<>(metadata);
    }

    public boolean isPassing() {
      return status == GateStatus.PASS;
    }

    public boolean isWarning() {
      return status == GateStatus.WARNING;
    }

    public boolean isFailing() {
      return status == GateStatus.FAIL;
    }

    private static <T> T validateNotNull(final T value, final String fieldName) {
      if (value == null) {
        throw new IllegalArgumentException(fieldName + " cannot be null");
      }
      return value;
    }

    private static double validateFinite(final double value, final String fieldName) {
      if (!Double.isFinite(value)) {
        throw new IllegalArgumentException(fieldName + " must be finite");
      }
      return value;
    }
  }

  /** Performance gate status. */
  public enum GateStatus {
    PASS, // Performance meets requirements
    WARNING, // Minor regression detected
    FAIL, // Significant regression or below minimum threshold
    ERROR // Validation error (missing baseline, etc.)
  }

  /** Comprehensive gate validation result for entire CI/CD run. */
  public static final class CiCdGateValidationResult {
    private final List<PerformanceGateResult> gateResults;
    private final boolean overallPassing;
    private final int passCount;
    private final int warningCount;
    private final int failCount;
    private final int errorCount;
    private final String ciContext;
    private final String summaryMessage;
    private final LocalDateTime validatedAt;

    public CiCdGateValidationResult(
        final List<PerformanceGateResult> gateResults, final String ciContext) {
      this.gateResults = new ArrayList<>(gateResults);
      this.ciContext = ciContext != null ? ciContext : "unknown";
      this.validatedAt = LocalDateTime.now();

      // Calculate summary statistics
      this.passCount =
          (int) gateResults.stream().filter(r -> r.getStatus() == GateStatus.PASS).count();
      this.warningCount =
          (int) gateResults.stream().filter(r -> r.getStatus() == GateStatus.WARNING).count();
      this.failCount =
          (int) gateResults.stream().filter(r -> r.getStatus() == GateStatus.FAIL).count();
      this.errorCount =
          (int) gateResults.stream().filter(r -> r.getStatus() == GateStatus.ERROR).count();

      this.overallPassing = failCount == 0 && errorCount == 0;
      this.summaryMessage = generateSummaryMessage();
    }

    // Getters
    public List<PerformanceGateResult> getGateResults() {
      return new ArrayList<>(gateResults);
    }

    public boolean isOverallPassing() {
      return overallPassing;
    }

    public int getPassCount() {
      return passCount;
    }

    public int getWarningCount() {
      return warningCount;
    }

    public int getFailCount() {
      return failCount;
    }

    public int getErrorCount() {
      return errorCount;
    }

    public String getCiContext() {
      return ciContext;
    }

    public String getSummaryMessage() {
      return summaryMessage;
    }

    public LocalDateTime getValidatedAt() {
      return validatedAt;
    }

    private String generateSummaryMessage() {
      if (overallPassing && warningCount == 0) {
        return "All performance gates passed successfully";
      } else if (overallPassing && warningCount > 0) {
        return String.format("Performance gates passed with %d warning(s)", warningCount);
      } else {
        return String.format(
            "Performance gates FAILED: %d failures, %d errors", failCount, errorCount);
      }
    }

    public int getExitCode() {
      if (failCount > 0 || errorCount > 0) {
        return 1; // Failure exit code for CI/CD
      }
      return 0; // Success exit code
    }
  }

  /** CI/CD performance gate enforcement engine. */
  public static final class PerformanceGateEnforcer {
    private final PerformanceGateConfig config;
    private final PerformanceRegressionTestingFramework regressionFramework;

    public PerformanceGateEnforcer(final PerformanceGateConfig config) {
      this.config = validateNotNull(config, "config");
      this.regressionFramework =
          new PerformanceRegressionTestingFramework(Paths.get("performance_data"));
    }

    /** Validates performance results against configured gates. */
    public CiCdGateValidationResult validatePerformanceGates(final Iterable<RunResult> results) {
      final List<PerformanceGateResult> gateResults = new ArrayList<>();
      final String ciContext =
          String.format(
              "Pipeline: %s, Job: %s, Commit: %s, Branch: %s",
              CI_PIPELINE_ID, CI_JOB_NAME, CI_COMMIT_SHA, CI_BRANCH_NAME);

      try {
        // Load baseline data
        final Map<String, PerformanceRegressionTestingFramework.PerformanceDataPoint> baseline =
            PerformanceRegressionTestingFramework.BaselineManager.loadBaseline();

        for (final RunResult result : results) {
          final PerformanceGateResult gateResult = validateSingleBenchmark(result, baseline);
          gateResults.add(gateResult);
        }

      } catch (final Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to validate performance gates", e);
        final PerformanceGateResult errorResult =
            new PerformanceGateResult(
                "ERROR",
                "UNKNOWN",
                0.0,
                0.0,
                0.0,
                GateStatus.ERROR,
                "Validation error: " + e.getMessage(),
                CI_COMMIT_SHA,
                null);
        gateResults.add(errorResult);
      }

      return new CiCdGateValidationResult(gateResults, ciContext);
    }

    private PerformanceGateResult validateSingleBenchmark(
        final RunResult result,
        final Map<String, PerformanceRegressionTestingFramework.PerformanceDataPoint> baseline) {

      final String benchmarkName = result.getParams().getBenchmark();
      final String runtimeType = extractRuntimeType(benchmarkName);
      final double currentScore = result.getPrimaryResult().getScore();
      final String baselineKey = generateBaselineKey(benchmarkName, runtimeType);

      final PerformanceRegressionTestingFramework.PerformanceDataPoint baselinePoint =
          baseline.get(baselineKey);

      if (baselinePoint == null) {
        return new PerformanceGateResult(
            benchmarkName,
            runtimeType,
            currentScore,
            0.0,
            0.0,
            GateStatus.ERROR,
            "No baseline found for benchmark",
            CI_COMMIT_SHA,
            createMetadata(result));
      }

      final double baselineScore = baselinePoint.getScore();
      final double changePercent = (currentScore - baselineScore) / baselineScore;

      // Determine gate status
      final GateStatus status =
          determineGateStatus(benchmarkName, currentScore, baselineScore, changePercent);
      final String message =
          generateGateMessage(status, changePercent, currentScore, baselineScore);

      return new PerformanceGateResult(
          benchmarkName,
          runtimeType,
          currentScore,
          baselineScore,
          changePercent,
          status,
          message,
          CI_COMMIT_SHA,
          createMetadata(result));
    }

    private GateStatus determineGateStatus(
        final String benchmarkName,
        final double currentScore,
        final double baselineScore,
        final double changePercent) {

      // Check absolute minimum
      if (currentScore < config.getMinimumScore()) {
        return GateStatus.FAIL;
      }

      // Get appropriate threshold (custom or default)
      final double regressionThreshold =
          config.getCustomThresholds().getOrDefault(benchmarkName, config.getRegressionThreshold());

      // Check for regression
      if (changePercent < -regressionThreshold) {
        return GateStatus.FAIL;
      }

      // Check for warning level
      if (changePercent < -config.getWarningThreshold()) {
        return config.isStrictMode() ? GateStatus.FAIL : GateStatus.WARNING;
      }

      return GateStatus.PASS;
    }

    private String generateGateMessage(
        final GateStatus status,
        final double changePercent,
        final double currentScore,
        final double baselineScore) {
      switch (status) {
        case PASS:
          if (changePercent > 0.01) {
            return String.format(
                "Performance improved by %.1f%% (%.2f vs %.2f)",
                changePercent * 100, currentScore, baselineScore);
          } else {
            return String.format("Performance stable (%.2f vs %.2f)", currentScore, baselineScore);
          }
        case WARNING:
          return String.format(
              "Performance regression warning: %.1f%% decrease (%.2f vs %.2f)",
              Math.abs(changePercent) * 100, currentScore, baselineScore);
        case FAIL:
          return String.format(
              "Performance gate FAILED: %.1f%% regression (%.2f vs %.2f)",
              Math.abs(changePercent) * 100, currentScore, baselineScore);
        case ERROR:
        default:
          return "Gate validation error";
      }
    }

    private Map<String, Object> createMetadata(final RunResult result) {
      final Map<String, Object> metadata = new HashMap<>();
      metadata.put("iterations", result.getPrimaryResult().getStatistics().getN());
      metadata.put("scoreError", result.getPrimaryResult().getScoreError());
      metadata.put("scoreUnit", result.getPrimaryResult().getScoreUnit());
      metadata.put("mode", result.getParams().getMode().shortLabel());
      metadata.put("warmupIterations", result.getParams().getWarmup().getCount());
      metadata.put("measurementIterations", result.getParams().getMeasurement().getCount());
      return metadata;
    }

    private String extractRuntimeType(final String benchmarkName) {
      if (benchmarkName.toLowerCase().contains("jni")) {
        return "JNI";
      } else if (benchmarkName.toLowerCase().contains("panama")) {
        return "PANAMA";
      }
      return "UNKNOWN";
    }

    private String generateBaselineKey(final String benchmarkName, final String runtimeType) {
      return benchmarkName + "_" + runtimeType;
    }

    private static <T> T validateNotNull(final T value, final String fieldName) {
      if (value == null) {
        throw new IllegalArgumentException(fieldName + " cannot be null");
      }
      return value;
    }
  }

  /** Sample CI/CD gate benchmark for JNI runtime. */
  @Benchmark
  public int ciCdGateBenchmarkJni() throws Exception {
    if (!"JNI".equals(runtimeTypeName)) {
      return preventOptimization(0);
    }

    final var runtime = createRuntime(RuntimeType.JNI);
    final var engine = createEngine(runtime);
    final var store = createStore(engine);
    final var module = compileModule(engine, SIMPLE_WASM_MODULE);
    final var instance = instantiateModule(store, module);

    return performStandardOperation();
  }

  /** Sample CI/CD gate benchmark for Panama runtime. */
  @Benchmark
  public int ciCdGateBenchmarkPanama() throws Exception {
    if (!"PANAMA".equals(runtimeTypeName) || getJavaVersion() < 23) {
      return preventOptimization(0);
    }

    final var runtime = createRuntime(RuntimeType.PANAMA);
    final var engine = createEngine(runtime);
    final var store = createStore(engine);
    final var module = compileModule(engine, SIMPLE_WASM_MODULE);
    final var instance = instantiateModule(store, module);

    return performStandardOperation();
  }

  /** Performs a standard operation for gate validation. */
  private int performStandardOperation() {
    int result = 0;
    for (int i = 0; i < 1000; i++) {
      result += i * 2;
    }
    return preventOptimization(result);
  }

  /** Creates a configured JSON mapper. */
  private static ObjectMapper createJsonMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  /** Saves gate validation results to file. */
  private static void saveGateResults(final CiCdGateValidationResult result) throws IOException {
    final Path resultsDir = Paths.get("ci-cd-gate-results");
    Files.createDirectories(resultsDir);

    final String filename =
        String.format(
            "gate-results_%s_%s.json",
            CI_COMMIT_SHA.substring(0, Math.min(8, CI_COMMIT_SHA.length())),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

    final Path resultFile = resultsDir.resolve(filename);

    final ObjectNode root = JSON_MAPPER.createObjectNode();
    root.put("timestamp", result.getValidatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    root.put("ciContext", result.getCiContext());
    root.put("overallPassing", result.isOverallPassing());
    root.put("passCount", result.getPassCount());
    root.put("warningCount", result.getWarningCount());
    root.put("failCount", result.getFailCount());
    root.put("errorCount", result.getErrorCount());
    root.put("summaryMessage", result.getSummaryMessage());
    root.put("exitCode", result.getExitCode());

    final ArrayNode resultsArray = JSON_MAPPER.createArrayNode();
    for (final PerformanceGateResult gateResult : result.getGateResults()) {
      final ObjectNode gateNode = JSON_MAPPER.createObjectNode();
      gateNode.put("benchmarkName", gateResult.getBenchmarkName());
      gateNode.put("runtimeType", gateResult.getRuntimeType());
      gateNode.put("currentScore", gateResult.getCurrentScore());
      gateNode.put("baselineScore", gateResult.getBaselineScore());
      gateNode.put("changePercent", gateResult.getChangePercent());
      gateNode.put("status", gateResult.getStatus().name());
      gateNode.put("message", gateResult.getMessage());
      gateNode.put("commitSha", gateResult.getCommitSha());
      resultsArray.add(gateNode);
    }
    root.set("gateResults", resultsArray);

    Files.write(
        resultFile,
        JSON_MAPPER.writeValueAsBytes(root),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);

    LOGGER.info("Gate validation results saved to: " + resultFile);
  }

  /** Gets environment variable with default fallback. */
  private static String getEnvironmentVariable(final String name, final String defaultValue) {
    final String value = System.getenv(name);
    return value != null ? value : defaultValue;
  }

  /** Main method for CI/CD performance gate enforcement. */
  public static void main(final String[] args) throws Exception {
    LOGGER.info("Starting CI/CD performance gate enforcement");
    LOGGER.info(
        String.format(
            "CI Context: Pipeline=%s, Job=%s, Commit=%s, Branch=%s",
            CI_PIPELINE_ID, CI_JOB_NAME, CI_COMMIT_SHA, CI_BRANCH_NAME));

    // Configure performance gates
    final PerformanceGateConfig gateConfig =
        PerformanceGateConfig.builder()
            .regressionThreshold(0.05) // 5% regression threshold
            .warningThreshold(0.02) // 2% warning threshold
            .minimumScore(100.0) // Minimum 100 ops/sec
            .minimumSamples(3)
            .strictMode(false) // Allow warnings without failing
            .enableTrending(true)
            .build();

    // Configure and run benchmarks
    final Options options =
        new OptionsBuilder()
            .include(CiCdPerformanceGateEnforcement.class.getSimpleName())
            .param("runtimeTypeName", "JNI", "PANAMA")
            .forks(2)
            .warmupIterations(3)
            .measurementIterations(5)
            .build();

    final var results = new Runner(options).run();

    // Validate performance gates
    final PerformanceGateEnforcer enforcer = new PerformanceGateEnforcer(gateConfig);
    final CiCdGateValidationResult validationResult = enforcer.validatePerformanceGates(results);

    // Save results
    saveGateResults(validationResult);

    // Generate report
    final String report = generateCiCdReport(validationResult);
    System.out.println(report);

    // Log final status
    if (validationResult.isOverallPassing()) {
      LOGGER.info("✅ All performance gates passed");
    } else {
      LOGGER.severe("❌ Performance gates failed");
    }

    // Exit with appropriate code for CI/CD
    System.exit(validationResult.getExitCode());
  }

  /** Generates comprehensive CI/CD report. */
  private static String generateCiCdReport(final CiCdGateValidationResult result) {
    final StringBuilder report = new StringBuilder();

    report.append("CI/CD PERFORMANCE GATE ENFORCEMENT REPORT\n");
    report.append("========================================\n\n");

    report.append("Execution Context:\n");
    report.append("-----------------\n");
    report.append(result.getCiContext()).append("\n");
    report
        .append("Validation Time: ")
        .append(result.getValidatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .append("\n\n");

    report.append("Gate Validation Summary:\n");
    report.append("-----------------------\n");
    report
        .append("Overall Status: ")
        .append(result.isOverallPassing() ? "✅ PASS" : "❌ FAIL")
        .append("\n");
    report.append(result.getSummaryMessage()).append("\n");
    report.append("Total Gates: ").append(result.getGateResults().size()).append("\n");
    report.append("Passed: ").append(result.getPassCount()).append("\n");
    report.append("Warnings: ").append(result.getWarningCount()).append("\n");
    report.append("Failed: ").append(result.getFailCount()).append("\n");
    report.append("Errors: ").append(result.getErrorCount()).append("\n\n");

    if (!result.getGateResults().isEmpty()) {
      report.append("Detailed Gate Results:\n");
      report.append("---------------------\n");

      for (final PerformanceGateResult gateResult : result.getGateResults()) {
        final String statusIcon = getStatusIcon(gateResult.getStatus());
        report.append(
            String.format(
                "%s %s [%s]\n",
                statusIcon, gateResult.getBenchmarkName(), gateResult.getRuntimeType()));
        report.append(
            String.format(
                "   Score: %.2f (baseline: %.2f)\n",
                gateResult.getCurrentScore(), gateResult.getBaselineScore()));
        report.append(String.format("   Change: %+.1f%%\n", gateResult.getChangePercent() * 100));
        report.append(String.format("   Message: %s\n", gateResult.getMessage()));
        report.append("\n");
      }
    }

    return report.toString();
  }

  private static String getStatusIcon(final GateStatus status) {
    switch (status) {
      case PASS:
        return "✅";
      case WARNING:
        return "⚠️";
      case FAIL:
        return "❌";
      case ERROR:
        return "🔴";
      default:
        return "❓";
    }
  }
}

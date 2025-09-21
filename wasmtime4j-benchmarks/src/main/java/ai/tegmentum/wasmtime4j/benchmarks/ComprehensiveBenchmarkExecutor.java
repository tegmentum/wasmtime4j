package ai.tegmentum.wasmtime4j.benchmarks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Comprehensive benchmark executor for establishing performance baselines and running automated
 * performance testing with regression detection.
 *
 * <p>This executor provides automated execution of JMH benchmarks with configurable parameters,
 * integration with baseline establishment, and comprehensive result analysis for CI/CD pipelines.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automated JMH benchmark execution with configurable parameters
 *   <li>Integration with performance baseline establishment
 *   <li>Comprehensive result parsing and analysis
 *   <li>CI/CD compatible execution modes
 *   <li>Performance target validation and reporting
 * </ul>
 */
public final class ComprehensiveBenchmarkExecutor {

  /** Logger for benchmark execution. */
  private static final Logger LOGGER =
      Logger.getLogger(ComprehensiveBenchmarkExecutor.class.getName());

  /** Benchmark execution configuration. */
  public static final class BenchmarkConfiguration {
    private final int iterations;
    private final int warmupIterations;
    private final int forks;
    private final int threads;
    private final String mode;
    private final String timeUnit;
    private final int timeoutSeconds;
    private final String benchmarkFilter;
    private final boolean establishBaseline;
    private final boolean validateTargets;

    /**
     * Creates a benchmark configuration.
     *
     * @param iterations number of measurement iterations
     * @param warmupIterations number of warmup iterations
     * @param forks number of benchmark forks
     * @param threads number of benchmark threads
     * @param mode benchmark mode (throughput, average time, etc.)
     * @param timeUnit time unit for measurements
     * @param timeoutSeconds benchmark timeout in seconds
     * @param benchmarkFilter regex filter for benchmark selection
     * @param establishBaseline whether to establish performance baselines
     * @param validateTargets whether to validate against performance targets
     */
    public BenchmarkConfiguration(
        final int iterations,
        final int warmupIterations,
        final int forks,
        final int threads,
        final String mode,
        final String timeUnit,
        final int timeoutSeconds,
        final String benchmarkFilter,
        final boolean establishBaseline,
        final boolean validateTargets) {
      this.iterations = iterations;
      this.warmupIterations = warmupIterations;
      this.forks = forks;
      this.threads = threads;
      this.mode = mode;
      this.timeUnit = timeUnit;
      this.timeoutSeconds = timeoutSeconds;
      this.benchmarkFilter = benchmarkFilter;
      this.establishBaseline = establishBaseline;
      this.validateTargets = validateTargets;
    }

    /**
     * Creates a configuration for baseline establishment with comprehensive parameters.
     *
     * @return configuration optimized for baseline establishment
     */
    public static BenchmarkConfiguration forBaselineEstablishment() {
      return new BenchmarkConfiguration(
          1000, // High iterations for statistical significance
          100, // Extensive warmup for stable measurements
          3, // Multiple forks for reliability
          1, // Single thread for consistent results
          "thrpt,avgt", // Both throughput and average time
          "s,ns", // Seconds and nanoseconds
          600, // 10 minute timeout per benchmark
          ".*", // All benchmarks
          true, // Establish baselines
          false // Don't validate targets during establishment
          );
    }

    /**
     * Creates a configuration for CI/CD validation with balanced parameters.
     *
     * @return configuration optimized for CI/CD validation
     */
    public static BenchmarkConfiguration forCiCdValidation() {
      return new BenchmarkConfiguration(
          100, // Moderate iterations for reasonable CI time
          20, // Quick warmup for CI efficiency
          2, // Two forks for basic reliability
          1, // Single thread
          "thrpt", // Throughput only for faster execution
          "s", // Seconds only
          300, // 5 minute timeout
          ".*", // All benchmarks
          false, // Don't establish baselines in CI
          true // Validate against targets
          );
    }

    /**
     * Creates a configuration for quick performance checks.
     *
     * @return configuration optimized for quick validation
     */
    public static BenchmarkConfiguration forQuickCheck() {
      return new BenchmarkConfiguration(
          10, // Few iterations for speed
          5, // Minimal warmup
          1, // Single fork
          1, // Single thread
          "thrpt", // Throughput only
          "s", // Seconds only
          60, // 1 minute timeout
          ".*ComparisonBenchmark.*", // Focus on comparison benchmarks
          false, // No baseline establishment
          true // Validate targets
          );
    }

    public int getIterations() {
      return iterations;
    }

    public int getWarmupIterations() {
      return warmupIterations;
    }

    public int getForks() {
      return forks;
    }

    public int getThreads() {
      return threads;
    }

    public String getMode() {
      return mode;
    }

    public String getTimeUnit() {
      return timeUnit;
    }

    public int getTimeoutSeconds() {
      return timeoutSeconds;
    }

    public String getBenchmarkFilter() {
      return benchmarkFilter;
    }

    public boolean isEstablishBaseline() {
      return establishBaseline;
    }

    public boolean isValidateTargets() {
      return validateTargets;
    }
  }

  /** Benchmark execution result. */
  public static final class BenchmarkExecutionResult {
    private final boolean success;
    private final Path resultsFile;
    private final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements;
    private final PerformanceBaselineEstablisher.BaselineEstablishmentResult baselineResult;
    private final PerformanceBaselineEstablisher.PerformanceValidationResult validationResult;
    private final String executionLog;
    private final long executionTimeMillis;

    /**
     * Creates a benchmark execution result.
     *
     * @param success whether execution was successful
     * @param resultsFile path to JMH results file
     * @param measurements parsed performance measurements
     * @param baselineResult baseline establishment result (if applicable)
     * @param validationResult validation result (if applicable)
     * @param executionLog execution log content
     * @param executionTimeMillis total execution time in milliseconds
     */
    public BenchmarkExecutionResult(
        final boolean success,
        final Path resultsFile,
        final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements,
        final PerformanceBaselineEstablisher.BaselineEstablishmentResult baselineResult,
        final PerformanceBaselineEstablisher.PerformanceValidationResult validationResult,
        final String executionLog,
        final long executionTimeMillis) {
      this.success = success;
      this.resultsFile = resultsFile;
      this.measurements = new ArrayList<>(measurements);
      this.baselineResult = baselineResult;
      this.validationResult = validationResult;
      this.executionLog = executionLog;
      this.executionTimeMillis = executionTimeMillis;
    }

    public boolean isSuccess() {
      return success;
    }

    public Path getResultsFile() {
      return resultsFile;
    }

    public List<PerformanceRegressionDetector.PerformanceMeasurement> getMeasurements() {
      return new ArrayList<>(measurements);
    }

    public PerformanceBaselineEstablisher.BaselineEstablishmentResult getBaselineResult() {
      return baselineResult;
    }

    public PerformanceBaselineEstablisher.PerformanceValidationResult getValidationResult() {
      return validationResult;
    }

    public String getExecutionLog() {
      return executionLog;
    }

    public long getExecutionTimeMillis() {
      return executionTimeMillis;
    }
  }

  private final Path benchmarksDirectory;
  private final PerformanceBaselineEstablisher baselineEstablisher;
  private final BenchmarkResultAnalyzer resultAnalyzer;

  /**
   * Creates a comprehensive benchmark executor.
   *
   * @param benchmarksDirectory directory containing benchmark JAR and configuration
   */
  public ComprehensiveBenchmarkExecutor(final Path benchmarksDirectory) {
    this.benchmarksDirectory = benchmarksDirectory;
    this.baselineEstablisher = new PerformanceBaselineEstablisher();
    this.resultAnalyzer = new BenchmarkResultAnalyzer();
  }

  /**
   * Executes comprehensive benchmark suite with the specified configuration.
   *
   * @param configuration benchmark execution configuration
   * @return execution result with analysis
   */
  public BenchmarkExecutionResult executeBenchmarkSuite(
      final BenchmarkConfiguration configuration) {
    LOGGER.info(
        "Starting comprehensive benchmark execution with configuration: "
            + configuration.getIterations()
            + " iterations, "
            + configuration.getForks()
            + " forks, "
            + configuration.getWarmupIterations()
            + " warmup");

    final long startTime = System.currentTimeMillis();
    final Path outputDir = benchmarksDirectory.resolve("benchmark-results");
    final String timestamp = LocalDateTime.now().toString().replace(":", "-");
    final Path resultsFile = outputDir.resolve("jmh-results-" + timestamp + ".json");

    try {
      // Create output directory
      Files.createDirectories(outputDir);

      // Execute JMH benchmarks
      final boolean jmhSuccess = executeJmhBenchmarks(configuration, resultsFile);
      if (!jmhSuccess) {
        return new BenchmarkExecutionResult(
            false,
            resultsFile,
            new ArrayList<>(),
            null,
            null,
            "JMH benchmark execution failed",
            System.currentTimeMillis() - startTime);
      }

      // Parse results
      final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements =
          parseJmhResults(resultsFile);

      // Establish baselines if requested
      PerformanceBaselineEstablisher.BaselineEstablishmentResult baselineResult = null;
      if (configuration.isEstablishBaseline()) {
        LOGGER.info("Establishing performance baselines...");
        baselineResult = baselineEstablisher.establishBaselines(measurements);
      }

      // Validate targets if requested
      PerformanceBaselineEstablisher.PerformanceValidationResult validationResult = null;
      if (configuration.isValidateTargets()) {
        LOGGER.info("Validating performance targets...");
        validationResult = baselineEstablisher.validatePerformanceTargets(measurements);
      }

      final long executionTime = System.currentTimeMillis() - startTime;
      LOGGER.info("Benchmark execution completed in " + executionTime + "ms");

      return new BenchmarkExecutionResult(
          true,
          resultsFile,
          measurements,
          baselineResult,
          validationResult,
          "Execution completed successfully",
          executionTime);

    } catch (final Exception e) {
      LOGGER.severe("Benchmark execution failed: " + e.getMessage());
      return new BenchmarkExecutionResult(
          false,
          resultsFile,
          new ArrayList<>(),
          null,
          null,
          "Execution failed: " + e.getMessage(),
          System.currentTimeMillis() - startTime);
    }
  }

  /**
   * Executes performance baseline establishment with comprehensive configuration.
   *
   * @return baseline establishment result
   */
  public BenchmarkExecutionResult establishPerformanceBaselines() {
    LOGGER.info("Executing comprehensive performance baseline establishment");
    return executeBenchmarkSuite(BenchmarkConfiguration.forBaselineEstablishment());
  }

  /**
   * Executes CI/CD performance validation.
   *
   * @return validation execution result
   */
  public BenchmarkExecutionResult executeCiCdValidation() {
    LOGGER.info("Executing CI/CD performance validation");
    return executeBenchmarkSuite(BenchmarkConfiguration.forCiCdValidation());
  }

  /**
   * Executes quick performance check for development.
   *
   * @return quick check execution result
   */
  public BenchmarkExecutionResult executeQuickPerformanceCheck() {
    LOGGER.info("Executing quick performance check");
    return executeBenchmarkSuite(BenchmarkConfiguration.forQuickCheck());
  }

  /**
   * Generates comprehensive performance report from execution result.
   *
   * @param result benchmark execution result
   * @param outputDirectory directory for report output
   * @return path to generated report
   */
  public Path generateComprehensiveReport(
      final BenchmarkExecutionResult result, final Path outputDirectory) {
    try {
      Files.createDirectories(outputDirectory);

      // Generate HTML report
      final Path htmlReport = outputDirectory.resolve("comprehensive-performance-report.html");
      resultAnalyzer.generateHtmlReport(
          convertToAnalyzerResults(result.getMeasurements()), htmlReport);

      // Generate CSV export
      final Path csvReport = outputDirectory.resolve("performance-data.csv");
      resultAnalyzer.exportToCsv(convertToAnalyzerResults(result.getMeasurements()), csvReport);

      // Generate CI/CD report
      final Path ciReport = outputDirectory.resolve("ci-cd-report.json");
      final String ciReportContent =
          baselineEstablisher.generateCiCdPerformanceReport(result.getMeasurements());
      Files.write(ciReport, ciReportContent.getBytes());

      LOGGER.info("Comprehensive reports generated in: " + outputDirectory);
      return htmlReport;

    } catch (final IOException e) {
      LOGGER.warning("Failed to generate comprehensive report: " + e.getMessage());
      return null;
    }
  }

  private boolean executeJmhBenchmarks(
      final BenchmarkConfiguration config, final Path resultsFile) {
    try {
      final Path benchmarkJar =
          benchmarksDirectory.resolve("target").resolve("wasmtime4j-benchmarks.jar");
      if (!Files.exists(benchmarkJar)) {
        LOGGER.severe("Benchmark JAR not found: " + benchmarkJar);
        return false;
      }

      final List<String> command = new ArrayList<>();
      command.add("java");
      command.add("-jar");
      command.add(benchmarkJar.toString());
      command.add("-rf");
      command.add("json");
      command.add("-rff");
      command.add(resultsFile.toString());
      command.add("-i");
      command.add(String.valueOf(config.getIterations()));
      command.add("-wi");
      command.add(String.valueOf(config.getWarmupIterations()));
      command.add("-f");
      command.add(String.valueOf(config.getForks()));
      command.add("-t");
      command.add(String.valueOf(config.getThreads()));
      command.add("-bm");
      command.add(config.getMode());
      command.add("-tu");
      command.add(config.getTimeUnit());
      command.add("-to");
      command.add(config.getTimeoutSeconds() + "s");

      // Add benchmark filter if specified
      if (!".*".equals(config.getBenchmarkFilter())) {
        command.add(config.getBenchmarkFilter());
      }

      LOGGER.info("Executing JMH command: " + String.join(" ", command));

      final ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.directory(benchmarksDirectory.toFile());
      processBuilder.redirectErrorStream(true);

      final Process process = processBuilder.start();

      // Read output
      final StringBuilder output = new StringBuilder();
      try (final BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
          // Log progress for long-running benchmarks
          if (line.contains("# Benchmark:") || line.contains("# Run complete")) {
            LOGGER.info(line);
          }
        }
      }

      final boolean success =
          process.waitFor(config.getTimeoutSeconds() * 2, TimeUnit.SECONDS)
              && process.exitValue() == 0;

      if (!success) {
        LOGGER.severe("JMH execution failed. Output:\n" + output);
      }

      return success;

    } catch (final Exception e) {
      LOGGER.severe("Failed to execute JMH benchmarks: " + e.getMessage());
      return false;
    }
  }

  private List<PerformanceRegressionDetector.PerformanceMeasurement> parseJmhResults(
      final Path resultsFile) {
    try {
      if (!Files.exists(resultsFile)) {
        LOGGER.warning("Results file not found: " + resultsFile);
        return new ArrayList<>();
      }

      // Parse JMH JSON results and convert to PerformanceMeasurement objects
      final List<BenchmarkResultAnalyzer.BenchmarkResult> analyzerResults =
          resultAnalyzer.parseJmhResults(resultsFile);

      return analyzerResults.stream()
          .map(this::convertToPerformanceMeasurement)
          .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);

    } catch (final Exception e) {
      LOGGER.warning("Failed to parse JMH results: " + e.getMessage());
      return new ArrayList<>();
    }
  }

  private PerformanceRegressionDetector.PerformanceMeasurement convertToPerformanceMeasurement(
      final BenchmarkResultAnalyzer.BenchmarkResult result) {
    // Convert throughput score to latency approximation
    final double latency = result.getScore() > 0 ? 1000.0 / result.getScore() : 0.0;

    return new PerformanceRegressionDetector.PerformanceMeasurement(
        result.getBenchmarkName(),
        result.getRuntime(),
        result.getScore(),
        latency,
        1024 * 1024 // Default memory usage approximation
        );
  }

  private List<BenchmarkResultAnalyzer.BenchmarkResult> convertToAnalyzerResults(
      final List<PerformanceRegressionDetector.PerformanceMeasurement> measurements) {
    return measurements.stream()
        .map(
            m ->
                new BenchmarkResultAnalyzer.BenchmarkResult(
                    m.getBenchmarkName(),
                    m.getRuntimeType(),
                    "thrpt",
                    m.getThroughput(),
                    0.0, // No error available
                    "ops/s",
                    m.getTimestamp(),
                    m.getMetadata()))
        .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
  }

  /**
   * Main method for command-line execution.
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: ComprehensiveBenchmarkExecutor <mode> [options]");
      System.err.println("Modes:");
      System.err.println("  baseline    - Establish performance baselines");
      System.err.println("  validate    - Validate against performance targets");
      System.err.println("  quick       - Quick performance check");
      System.err.println("  ci-cd       - CI/CD validation");
      System.exit(1);
    }

    final String mode = args[0];
    final Path benchmarksDir = Paths.get(System.getProperty("user.dir"));
    final ComprehensiveBenchmarkExecutor executor =
        new ComprehensiveBenchmarkExecutor(benchmarksDir);

    try {
      BenchmarkExecutionResult result;

      switch (mode.toLowerCase()) {
        case "baseline":
          result = executor.establishPerformanceBaselines();
          break;
        case "validate":
        case "ci-cd":
          result = executor.executeCiCdValidation();
          break;
        case "quick":
          result = executor.executeQuickPerformanceCheck();
          break;
        default:
          System.err.println("Unknown mode: " + mode);
          System.exit(1);
          return;
      }

      if (result.isSuccess()) {
        System.out.println("Benchmark execution completed successfully");
        System.out.println("Results file: " + result.getResultsFile());
        System.out.println("Measurements: " + result.getMeasurements().size());
        System.out.println("Execution time: " + result.getExecutionTimeMillis() + "ms");

        if (result.getBaselineResult() != null) {
          System.out.println(
              "Baselines established: " + result.getBaselineResult().getSuccessfulBaselines());
          System.out.println(
              "Targets achieved: " + result.getBaselineResult().getTargetsAchieved());
        }

        if (result.getValidationResult() != null) {
          System.out.println("Validation result: " + result.getValidationResult().isValid());
        }

        // Generate reports
        final Path reportDir = benchmarksDir.resolve("performance-reports");
        executor.generateComprehensiveReport(result, reportDir);
        System.out.println("Reports generated in: " + reportDir);

      } else {
        System.err.println("Benchmark execution failed: " + result.getExecutionLog());
        System.exit(1);
      }

    } catch (final Exception e) {
      System.err.println("Execution failed: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
}

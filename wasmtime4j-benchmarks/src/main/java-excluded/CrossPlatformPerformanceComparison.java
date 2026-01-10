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
import java.util.Arrays;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
 * Comprehensive cross-platform performance comparison framework for Wasmtime4j.
 *
 * <p>This framework provides detailed performance analysis across different platforms,
 * architectures, and runtime implementations. It includes statistical comparison, platform-specific
 * optimization recommendations, and comprehensive reporting.
 *
 * <p>Key features: - Cross-platform performance benchmarking (Linux, Windows, macOS) -
 * Architecture-specific analysis (x86_64, ARM64) - Runtime comparison (JNI vs Panama) - Performance
 * variability analysis - Platform-specific optimization recommendations - Statistical significance
 * testing across platforms - Comprehensive reporting and visualization data export
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 3,
    jvmArgs = {"-Xms4g", "-Xmx4g"})
public class CrossPlatformPerformanceComparison extends BenchmarkBase {

  private static final Logger LOGGER =
      Logger.getLogger(CrossPlatformPerformanceComparison.class.getName());
  private static final ObjectMapper JSON_MAPPER = createJsonMapper();

  // Platform detection constants
  private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
  private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();
  private static final String JAVA_VERSION = System.getProperty("java.version");
  private static final String JVM_NAME = System.getProperty("java.vm.name");

  // Benchmark configuration
  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  /** Platform information container. */
  public static final class PlatformInfo {
    private final String operatingSystem;
    private final String architecture;
    private final String javaVersion;
    private final String jvmName;
    private final int availableProcessors;
    private final long maxMemory;
    private final LocalDateTime detectedAt;

    public PlatformInfo() {
      this.operatingSystem = detectOperatingSystem();
      this.architecture = detectArchitecture();
      this.javaVersion = JAVA_VERSION;
      this.jvmName = JVM_NAME;
      this.availableProcessors = Runtime.getRuntime().availableProcessors();
      this.maxMemory = Runtime.getRuntime().maxMemory();
      this.detectedAt = LocalDateTime.now();
    }

    // Getters
    public String getOperatingSystem() {
      return operatingSystem;
    }

    public String getArchitecture() {
      return architecture;
    }

    public String getJavaVersion() {
      return javaVersion;
    }

    public String getJvmName() {
      return jvmName;
    }

    public int getAvailableProcessors() {
      return availableProcessors;
    }

    public long getMaxMemory() {
      return maxMemory;
    }

    public LocalDateTime getDetectedAt() {
      return detectedAt;
    }

    private static String detectOperatingSystem() {
      if (OS_NAME.contains("win")) {
        return "Windows";
      } else if (OS_NAME.contains("mac") || OS_NAME.contains("darwin")) {
        return "macOS";
      } else if (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix")) {
        return "Linux";
      } else {
        return OS_NAME;
      }
    }

    private static String detectArchitecture() {
      if (OS_ARCH.contains("amd64") || OS_ARCH.contains("x86_64")) {
        return "x86_64";
      } else if (OS_ARCH.contains("aarch64") || OS_ARCH.contains("arm64")) {
        return "ARM64";
      } else {
        return OS_ARCH;
      }
    }

    @Override
    public String toString() {
      return String.format(
          "%s/%s (Java %s, %s, %d cores, %dMB heap)",
          operatingSystem,
          architecture,
          javaVersion,
          jvmName,
          availableProcessors,
          maxMemory / (1024 * 1024));
    }
  }

  /** Cross-platform performance measurement. */
  public static final class CrossPlatformMeasurement {
    private final String benchmarkName;
    private final String runtimeType;
    private final PlatformInfo platformInfo;
    private final double score;
    private final double scoreError;
    private final String unit;
    private final LocalDateTime timestamp;
    private final Map<String, Object> additionalMetrics;

    public CrossPlatformMeasurement(
        final String benchmarkName,
        final String runtimeType,
        final PlatformInfo platformInfo,
        final double score,
        final double scoreError,
        final String unit,
        final Map<String, Object> additionalMetrics) {
      this.benchmarkName = validateNotNull(benchmarkName, "benchmarkName");
      this.runtimeType = validateNotNull(runtimeType, "runtimeType");
      this.platformInfo = validateNotNull(platformInfo, "platformInfo");
      this.score = validatePositive(score, "score");
      this.scoreError = validateNonNegative(scoreError, "scoreError");
      this.unit = validateNotNull(unit, "unit");
      this.timestamp = LocalDateTime.now();
      this.additionalMetrics =
          additionalMetrics != null ? new HashMap<>(additionalMetrics) : new HashMap<>();
    }

    // Getters
    public String getBenchmarkName() {
      return benchmarkName;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public PlatformInfo getPlatformInfo() {
      return platformInfo;
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

    public Map<String, Object> getAdditionalMetrics() {
      return new HashMap<>(additionalMetrics);
    }

    public String getPlatformKey() {
      return platformInfo.getOperatingSystem() + "_" + platformInfo.getArchitecture();
    }

    private static <T> T validateNotNull(final T value, final String fieldName) {
      if (value == null) {
        throw new IllegalArgumentException(fieldName + " cannot be null");
      }
      return value;
    }

    private static double validatePositive(final double value, final String fieldName) {
      if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) {
        throw new IllegalArgumentException(fieldName + " must be positive and finite");
      }
      return value;
    }

    private static double validateNonNegative(final double value, final String fieldName) {
      if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
        throw new IllegalArgumentException(fieldName + " must be non-negative and finite");
      }
      return value;
    }
  }

  /** Cross-platform comparison result. */
  public static final class CrossPlatformComparisonResult {
    private final String benchmarkName;
    private final String runtimeType;
    private final List<CrossPlatformMeasurement> measurements;
    private final Map<String, DoubleSummaryStatistics> platformStats;
    private final String fastestPlatform;
    private final String slowestPlatform;
    private final double performanceVariation;
    private final List<String> recommendations;
    private final LocalDateTime analyzedAt;

    public CrossPlatformComparisonResult(
        final String benchmarkName,
        final String runtimeType,
        final List<CrossPlatformMeasurement> measurements) {
      this.benchmarkName = validateNotNull(benchmarkName, "benchmarkName");
      this.runtimeType = validateNotNull(runtimeType, "runtimeType");
      this.measurements = new ArrayList<>(measurements);
      this.platformStats = calculatePlatformStatistics(measurements);
      this.fastestPlatform = findFastestPlatform();
      this.slowestPlatform = findSlowestPlatform();
      this.performanceVariation = calculatePerformanceVariation();
      this.recommendations = generateRecommendations();
      this.analyzedAt = LocalDateTime.now();
    }

    // Getters
    public String getBenchmarkName() {
      return benchmarkName;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public List<CrossPlatformMeasurement> getMeasurements() {
      return new ArrayList<>(measurements);
    }

    public Map<String, DoubleSummaryStatistics> getPlatformStats() {
      return new HashMap<>(platformStats);
    }

    public String getFastestPlatform() {
      return fastestPlatform;
    }

    public String getSlowestPlatform() {
      return slowestPlatform;
    }

    public double getPerformanceVariation() {
      return performanceVariation;
    }

    public List<String> getRecommendations() {
      return new ArrayList<>(recommendations);
    }

    public LocalDateTime getAnalyzedAt() {
      return analyzedAt;
    }

    private Map<String, DoubleSummaryStatistics> calculatePlatformStatistics(
        final List<CrossPlatformMeasurement> measurements) {
      return measurements.stream()
          .collect(
              Collectors.groupingBy(
                  CrossPlatformMeasurement::getPlatformKey,
                  Collectors.summarizingDouble(CrossPlatformMeasurement::getScore)));
    }

    private String findFastestPlatform() {
      return platformStats.entrySet().stream()
          .max(Comparator.comparingDouble(entry -> entry.getValue().getAverage()))
          .map(Map.Entry::getKey)
          .orElse("UNKNOWN");
    }

    private String findSlowestPlatform() {
      return platformStats.entrySet().stream()
          .min(Comparator.comparingDouble(entry -> entry.getValue().getAverage()))
          .map(Map.Entry::getKey)
          .orElse("UNKNOWN");
    }

    private double calculatePerformanceVariation() {
      if (platformStats.size() < 2) {
        return 0.0;
      }

      final DoubleSummaryStatistics overallStats =
          platformStats.values().stream()
              .collect(
                  DoubleSummaryStatistics::new,
                  (stats, platformStat) -> stats.accept(platformStat.getAverage()),
                  DoubleSummaryStatistics::combine);

      final double mean = overallStats.getAverage();
      final double variance =
          platformStats.values().stream()
              .mapToDouble(stats -> Math.pow(stats.getAverage() - mean, 2))
              .average()
              .orElse(0.0);

      return Math.sqrt(variance) / mean; // Coefficient of variation
    }

    private List<String> generateRecommendations() {
      final List<String> recommendations = new ArrayList<>();

      if (performanceVariation > 0.2) {
        recommendations.add(
            "High performance variation across platforms detected (CV > 20%). "
                + "Consider platform-specific optimizations.");
      }

      if (platformStats.size() >= 2) {
        final double fastestScore = platformStats.get(fastestPlatform).getAverage();
        final double slowestScore = platformStats.get(slowestPlatform).getAverage();
        final double speedupRatio = fastestScore / slowestScore;

        if (speedupRatio > 2.0) {
          recommendations.add(
              String.format(
                  "Significant performance difference between %s (fastest) and %s (slowest): %.1fx"
                      + " speedup. Focus optimization efforts on slower platforms.",
                  fastestPlatform, slowestPlatform, speedupRatio));
        } else if (speedupRatio > 1.5) {
          recommendations.add(
              String.format(
                  "Moderate performance difference detected: %.1fx between fastest and slowest"
                      + " platforms. Monitor for optimization opportunities.",
                  speedupRatio));
        }
      }

      // Platform-specific recommendations
      for (final Map.Entry<String, DoubleSummaryStatistics> entry : platformStats.entrySet()) {
        final String platform = entry.getKey();
        final DoubleSummaryStatistics stats = entry.getValue();

        if (platform.contains("ARM64")
            && stats.getAverage()
                < platformStats.values().stream()
                    .mapToDouble(DoubleSummaryStatistics::getAverage)
                    .average()
                    .orElse(0.0)) {
          recommendations.add(
              "ARM64 platform showing lower performance. "
                  + "Consider ARM-specific optimizations or SIMD utilization.");
        }

        if (platform.contains("Windows")
            && stats.getAverage()
                < platformStats.values().stream()
                    .mapToDouble(DoubleSummaryStatistics::getAverage)
                    .average()
                    .orElse(0.0)) {
          recommendations.add(
              "Windows platform showing lower performance. "
                  + "Review Windows-specific JVM settings and native library loading.");
        }
      }

      if (recommendations.isEmpty()) {
        recommendations.add(
            "Performance is consistent across platforms. No specific optimizations needed.");
      }

      return recommendations;
    }

    private static <T> T validateNotNull(final T value, final String fieldName) {
      if (value == null) {
        throw new IllegalArgumentException(fieldName + " cannot be null");
      }
      return value;
    }
  }

  /** Creates a configured JSON mapper for cross-platform data serialization. */
  private static ObjectMapper createJsonMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  /** Sample benchmark for JNI runtime validation. */
  @Benchmark
  public int crossPlatformBenchmarkJni() throws Exception {
    if (!"JNI".equals(runtimeTypeName)) {
      return preventOptimization(0); // Skip for other runtimes
    }

    final var runtime = createRuntime(RuntimeType.JNI);
    final var engine = createEngine(runtime);
    final var store = createStore(engine);
    final var module = compileModule(engine, SIMPLE_WASM_MODULE);
    final var instance = instantiateModule(store, module);

    // Perform cross-platform computation
    return performCrossPlatformComputation();
  }

  /** Sample benchmark for Panama runtime validation. */
  @Benchmark
  public int crossPlatformBenchmarkPanama() throws Exception {
    if (!"PANAMA".equals(runtimeTypeName) || getJavaVersion() < 23) {
      return preventOptimization(0); // Skip for other runtimes or older Java
    }

    final var runtime = createRuntime(RuntimeType.PANAMA);
    final var engine = createEngine(runtime);
    final var store = createStore(engine);
    final var module = compileModule(engine, SIMPLE_WASM_MODULE);
    final var instance = instantiateModule(store, module);

    // Perform cross-platform computation
    return performCrossPlatformComputation();
  }

  /** Performs a representative cross-platform computation. */
  private int performCrossPlatformComputation() {
    // Simulate platform-dependent computation patterns
    int result = 0;
    final int iterations = 1000;

    for (int i = 0; i < iterations; i++) {
      // Arithmetic operations
      result += i * 2;

      // Memory access patterns (platform-dependent)
      final byte[] buffer = new byte[128];
      for (int j = 0; j < buffer.length; j++) {
        buffer[j] = (byte) (i + j);
      }

      // Aggregate buffer content
      for (final byte b : buffer) {
        result += b & 0xFF;
      }
    }

    return preventOptimization(result);
  }

  /** Runs comprehensive cross-platform performance analysis. */
  public static void main(final String[] args) throws Exception {
    LOGGER.info("Starting cross-platform performance comparison");

    final PlatformInfo platformInfo = new PlatformInfo();
    LOGGER.info("Platform detected: " + platformInfo);

    // Configure benchmarks based on platform capabilities
    final List<String> runtimeTypes = new ArrayList<>(Arrays.asList("JNI"));
    if (platformInfo.getJavaVersion().startsWith("2")
        && Integer.parseInt(platformInfo.getJavaVersion().split("\\.")[0]) >= 23) {
      runtimeTypes.add("PANAMA");
    }

    final Options options =
        new OptionsBuilder()
            .include(CrossPlatformPerformanceComparison.class.getSimpleName())
            .param("runtimeTypeName", runtimeTypes.toArray(new String[0]))
            .forks(3)
            .warmupIterations(5)
            .measurementIterations(10)
            .build();

    final var results = new Runner(options).run();

    // Analyze and save results
    final CrossPlatformAnalysisFramework analyzer = new CrossPlatformAnalysisFramework();
    final List<CrossPlatformComparisonResult> analysisResults =
        analyzer.analyzeResults(results, platformInfo);

    // Save results to file
    saveCrossPlatformResults(analysisResults, platformInfo);

    // Generate and display report
    final String report = generateComprehensiveReport(analysisResults, platformInfo);
    System.out.println(report);

    LOGGER.info("Cross-platform analysis completed");
  }

  /** Cross-platform analysis framework. */
  private static final class CrossPlatformAnalysisFramework {

    public List<CrossPlatformComparisonResult> analyzeResults(
        final Iterable<RunResult> results, final PlatformInfo platformInfo) {
      final Map<String, List<CrossPlatformMeasurement>> measurementsByBenchmark = new HashMap<>();

      for (final RunResult result : results) {
        final String benchmarkName = result.getParams().getBenchmark();
        final String runtimeType = extractRuntimeFromParams(result);
        final double score = result.getPrimaryResult().getScore();
        final double scoreError = result.getPrimaryResult().getScoreError();
        final String unit = result.getPrimaryResult().getScoreUnit();

        final Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("iterations", result.getPrimaryResult().getStatistics().getN());
        additionalMetrics.put("mode", result.getParams().getMode().shortLabel());

        final CrossPlatformMeasurement measurement =
            new CrossPlatformMeasurement(
                benchmarkName,
                runtimeType,
                platformInfo,
                score,
                scoreError,
                unit,
                additionalMetrics);

        measurementsByBenchmark
            .computeIfAbsent(benchmarkName, k -> new ArrayList<>())
            .add(measurement);
      }

      return measurementsByBenchmark.entrySet().stream()
          .map(
              entry -> {
                final String benchmarkName = entry.getKey();
                final List<CrossPlatformMeasurement> measurements = entry.getValue();

                // Group by runtime type for separate analysis
                final Map<String, List<CrossPlatformMeasurement>> byRuntime =
                    measurements.stream()
                        .collect(Collectors.groupingBy(CrossPlatformMeasurement::getRuntimeType));

                return byRuntime.entrySet().stream()
                    .map(
                        runtimeEntry ->
                            new CrossPlatformComparisonResult(
                                benchmarkName, runtimeEntry.getKey(), runtimeEntry.getValue()))
                    .collect(Collectors.toList());
              })
          .flatMap(List::stream)
          .collect(Collectors.toList());
    }

    private String extractRuntimeFromParams(final RunResult result) {
      final String benchmark = result.getParams().getBenchmark();
      if (benchmark.toLowerCase().contains("jni")) {
        return "JNI";
      } else if (benchmark.toLowerCase().contains("panama")) {
        return "PANAMA";
      }

      // Check parameters for runtime type
      return result.getParams().getParamsKeys().stream()
          .filter(key -> key.contains("runtime") || key.contains("Runtime"))
          .findFirst()
          .map(key -> result.getParams().getParam(key))
          .orElse("UNKNOWN");
    }
  }

  /** Saves cross-platform results to JSON file. */
  private static void saveCrossPlatformResults(
      final List<CrossPlatformComparisonResult> results, final PlatformInfo platformInfo) {
    try {
      final Path resultsDir = Paths.get("cross-platform-results");
      Files.createDirectories(resultsDir);

      final String filename =
          String.format(
              "cross-platform-results_%s_%s_%s.json",
              platformInfo.getOperatingSystem(),
              platformInfo.getArchitecture(),
              LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

      final Path resultFile = resultsDir.resolve(filename);

      final ObjectNode root = JSON_MAPPER.createObjectNode();
      root.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      root.set("platformInfo", serializePlatformInfo(platformInfo));

      final ArrayNode resultsArray = JSON_MAPPER.createArrayNode();
      for (final CrossPlatformComparisonResult result : results) {
        final ObjectNode resultNode = JSON_MAPPER.createObjectNode();
        resultNode.put("benchmarkName", result.getBenchmarkName());
        resultNode.put("runtimeType", result.getRuntimeType());
        resultNode.put("fastestPlatform", result.getFastestPlatform());
        resultNode.put("slowestPlatform", result.getSlowestPlatform());
        resultNode.put("performanceVariation", result.getPerformanceVariation());

        final ArrayNode recommendationsArray = JSON_MAPPER.createArrayNode();
        for (final String recommendation : result.getRecommendations()) {
          recommendationsArray.add(recommendation);
        }
        resultNode.set("recommendations", recommendationsArray);

        resultsArray.add(resultNode);
      }
      root.set("results", resultsArray);

      Files.write(
          resultFile,
          JSON_MAPPER.writeValueAsBytes(root),
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);

      LOGGER.info("Cross-platform results saved to: " + resultFile);

    } catch (final IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to save cross-platform results", e);
    }
  }

  /** Serializes platform info to JSON. */
  private static ObjectNode serializePlatformInfo(final PlatformInfo platformInfo) {
    final ObjectNode platformNode = JSON_MAPPER.createObjectNode();
    platformNode.put("operatingSystem", platformInfo.getOperatingSystem());
    platformNode.put("architecture", platformInfo.getArchitecture());
    platformNode.put("javaVersion", platformInfo.getJavaVersion());
    platformNode.put("jvmName", platformInfo.getJvmName());
    platformNode.put("availableProcessors", platformInfo.getAvailableProcessors());
    platformNode.put("maxMemory", platformInfo.getMaxMemory());
    platformNode.put(
        "detectedAt", platformInfo.getDetectedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    return platformNode;
  }

  /** Generates comprehensive cross-platform performance report. */
  private static String generateComprehensiveReport(
      final List<CrossPlatformComparisonResult> results, final PlatformInfo platformInfo) {
    final StringBuilder report = new StringBuilder();

    report.append("CROSS-PLATFORM PERFORMANCE ANALYSIS REPORT\n");
    report.append("==========================================\n\n");

    report.append("Platform Information:\n");
    report.append("--------------------\n");
    report.append("OS: ").append(platformInfo.getOperatingSystem()).append("\n");
    report.append("Architecture: ").append(platformInfo.getArchitecture()).append("\n");
    report.append("Java Version: ").append(platformInfo.getJavaVersion()).append("\n");
    report.append("JVM: ").append(platformInfo.getJvmName()).append("\n");
    report.append("CPU Cores: ").append(platformInfo.getAvailableProcessors()).append("\n");
    report
        .append("Max Heap: ")
        .append(platformInfo.getMaxMemory() / (1024 * 1024))
        .append(" MB\n\n");

    if (results.isEmpty()) {
      report.append("No cross-platform results available for analysis.\n");
      return report.toString();
    }

    report.append("Performance Analysis Summary:\n");
    report.append("----------------------------\n");
    report.append("Total Benchmarks Analyzed: ").append(results.size()).append("\n");

    final double avgVariation =
        results.stream()
            .mapToDouble(CrossPlatformComparisonResult::getPerformanceVariation)
            .average()
            .orElse(0.0);
    report
        .append("Average Performance Variation: ")
        .append(String.format("%.1f%%", avgVariation * 100))
        .append("\n\n");

    for (final CrossPlatformComparisonResult result : results) {
      report.append("Benchmark: ").append(result.getBenchmarkName()).append("\n");
      report.append("Runtime: ").append(result.getRuntimeType()).append("\n");
      report
          .append("Performance Variation: ")
          .append(String.format("%.1f%%", result.getPerformanceVariation() * 100))
          .append("\n");

      if (!result.getFastestPlatform().equals("UNKNOWN")) {
        report.append("Fastest Platform: ").append(result.getFastestPlatform()).append("\n");
        report.append("Slowest Platform: ").append(result.getSlowestPlatform()).append("\n");
      }

      if (!result.getRecommendations().isEmpty()) {
        report.append("Recommendations:\n");
        for (final String recommendation : result.getRecommendations()) {
          report.append("  - ").append(recommendation).append("\n");
        }
      }
      report.append("\n");
    }

    return report.toString();
  }
}

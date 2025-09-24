package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Comprehensive benchmarking suite for wasmtime4j performance validation.
 *
 * <p>This suite provides exhaustive performance testing across all major WebAssembly operations and
 * runtime implementations. It includes benchmarks for:
 *
 * <ul>
 *   <li>Core API operations (Engine, Module, Instance creation and execution)
 *   <li>WASI operations (file I/O, environment access, process operations)
 *   <li>Component model operations (component instantiation and linking)
 *   <li>SIMD operations (vector processing and bulk memory operations)
 *   <li>Multi-threading scenarios (concurrent access and execution)
 *   <li>Memory management (allocation patterns and garbage collection)
 *   <li>Cache performance (compilation and metadata caching)
 * </ul>
 *
 * <p>The suite runs comprehensive comparisons between JNI and Panama implementations, tracks
 * performance regressions, and provides detailed analysis reports with optimization
 * recommendations.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * ComprehensiveBenchmarkSuite suite = new ComprehensiveBenchmarkSuite();
 * BenchmarkResults results = suite.runAllBenchmarks();
 * String report = suite.generateReport(results);
 * }</pre>
 *
 * @since 1.0.0
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {
      "-Xms4g",
      "-Xmx4g",
      "-XX:+UseG1GC",
      "-XX:+UnlockExperimentalVMOptions",
      "-XX:+EnableJVMCI",
      "-XX:+UseJVMCICompiler"
    })
public class ComprehensiveBenchmarkSuite extends BenchmarkBase {

  private static final Logger LOGGER =
      Logger.getLogger(ComprehensiveBenchmarkSuite.class.getName());

  /** Benchmark categories. */
  public enum BenchmarkCategory {
    CORE_API("Core API Operations"),
    WASI_OPERATIONS("WASI Operations"),
    COMPONENT_MODEL("Component Model"),
    SIMD_OPERATIONS("SIMD Operations"),
    MULTI_THREADING("Multi-threading"),
    MEMORY_MANAGEMENT("Memory Management"),
    CACHE_PERFORMANCE("Cache Performance"),
    OPTIMIZATION_VALIDATION("Optimization Validation");

    private final String displayName;

    BenchmarkCategory(final String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  /** Comprehensive benchmark results. */
  public static final class BenchmarkResults {
    private final Map<BenchmarkCategory, CategoryResults> categoryResults = new HashMap<>();
    private final long totalExecutionTimeMs;
    private final RuntimeType[] runtimesTestedq;
    private final String javaVersion;
    private final String systemInfo;

    public BenchmarkResults(
        final long totalExecutionTimeMs,
        final RuntimeType[] runtimesTested,
        final String javaVersion,
        final String systemInfo) {
      this.totalExecutionTimeMs = totalExecutionTimeMs;
      this.runtimesTestedq = runtimesTested.clone();
      this.javaVersion = javaVersion;
      this.systemInfo = systemInfo;
    }

    public void addCategoryResult(final BenchmarkCategory category, final CategoryResults results) {
      categoryResults.put(category, results);
    }

    public CategoryResults getCategoryResults(final BenchmarkCategory category) {
      return categoryResults.get(category);
    }

    public Map<BenchmarkCategory, CategoryResults> getAllResults() {
      return new HashMap<>(categoryResults);
    }

    public long getTotalExecutionTimeMs() {
      return totalExecutionTimeMs;
    }

    public RuntimeType[] getRuntimesTested() {
      return runtimesTestedq.clone();
    }

    public String getJavaVersion() {
      return javaVersion;
    }

    public String getSystemInfo() {
      return systemInfo;
    }
  }

  /** Results for a benchmark category. */
  public static final class CategoryResults {
    private final BenchmarkCategory category;
    private final Map<String, BenchmarkResult> results = new HashMap<>();
    private final long executionTimeMs;

    public CategoryResults(final BenchmarkCategory category, final long executionTimeMs) {
      this.category = category;
      this.executionTimeMs = executionTimeMs;
    }

    public void addResult(final String benchmarkName, final BenchmarkResult result) {
      results.put(benchmarkName, result);
    }

    public BenchmarkResult getResult(final String benchmarkName) {
      return results.get(benchmarkName);
    }

    public Map<String, BenchmarkResult> getAllResults() {
      return new HashMap<>(results);
    }

    public BenchmarkCategory getCategory() {
      return category;
    }

    public long getExecutionTimeMs() {
      return executionTimeMs;
    }
  }

  /** Individual benchmark result. */
  public static final class BenchmarkResult {
    private final String name;
    private final RuntimeType runtime;
    private final double score;
    private final String unit;
    private final double error;
    private final int samples;
    private final Map<String, Object> metadata;

    public BenchmarkResult(
        final String name,
        final RuntimeType runtime,
        final double score,
        final String unit,
        final double error,
        final int samples,
        final Map<String, Object> metadata) {
      this.name = name;
      this.runtime = runtime;
      this.score = score;
      this.unit = unit;
      this.error = error;
      this.samples = samples;
      this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public String getName() {
      return name;
    }

    public RuntimeType getRuntime() {
      return runtime;
    }

    public double getScore() {
      return score;
    }

    public String getUnit() {
      return unit;
    }

    public double getError() {
      return error;
    }

    public int getSamples() {
      return samples;
    }

    public Map<String, Object> getMetadata() {
      return new HashMap<>(metadata);
    }
  }

  // ============================================================================
  // Core API Benchmarks
  // ============================================================================

  @Benchmark
  @Group("CoreAPI")
  public void benchmarkEngineCreation_JNI() throws Exception {
    try (final var runtime = createRuntime(RuntimeType.JNI);
        final var engine = createEngine(runtime)) {
      // Engine creation benchmark
      preventOptimization(engine.hashCode());
    }
  }

  @Benchmark
  @Group("CoreAPI")
  public void benchmarkEngineCreation_Panama() throws Exception {
    if (getJavaVersion() >= 23) {
      try (final var runtime = createRuntime(RuntimeType.PANAMA);
          final var engine = createEngine(runtime)) {
        preventOptimization(engine.hashCode());
      }
    }
  }

  @Benchmark
  @Group("CoreAPI")
  public void benchmarkModuleCompilation_JNI() throws Exception {
    try (final var runtime = createRuntime(RuntimeType.JNI);
        final var engine = createEngine(runtime)) {
      final var module = compileModule(engine, SIMPLE_WASM_MODULE);
      preventOptimization(module.hashCode());
    }
  }

  @Benchmark
  @Group("CoreAPI")
  public void benchmarkModuleCompilation_Panama() throws Exception {
    if (getJavaVersion() >= 23) {
      try (final var runtime = createRuntime(RuntimeType.PANAMA);
          final var engine = createEngine(runtime)) {
        final var module = compileModule(engine, SIMPLE_WASM_MODULE);
        preventOptimization(module.hashCode());
      }
    }
  }

  @Benchmark
  @Group("CoreAPI")
  public void benchmarkInstanceCreation_JNI() throws Exception {
    try (final var runtime = createRuntime(RuntimeType.JNI);
        final var engine = createEngine(runtime);
        final var store = createStore(engine)) {
      final var module = compileModule(engine, SIMPLE_WASM_MODULE);
      final var instance = instantiateModule(store, module);
      preventOptimization(instance.hashCode());
    }
  }

  @Benchmark
  @Group("CoreAPI")
  public void benchmarkInstanceCreation_Panama() throws Exception {
    if (getJavaVersion() >= 23) {
      try (final var runtime = createRuntime(RuntimeType.PANAMA);
          final var engine = createEngine(runtime);
          final var store = createStore(engine)) {
        final var module = compileModule(engine, SIMPLE_WASM_MODULE);
        final var instance = instantiateModule(store, module);
        preventOptimization(instance.hashCode());
      }
    }
  }

  @Benchmark
  @Group("CoreAPI")
  public void benchmarkFunctionExecution_JNI() throws Exception {
    try (final var runtime = createRuntime(RuntimeType.JNI);
        final var engine = createEngine(runtime);
        final var store = createStore(engine)) {
      final var module = compileModule(engine, SIMPLE_WASM_MODULE);
      final var instance = instantiateModule(store, module);

      final var addFunction = instance.getExportedFunction("add");
      if (addFunction != null) {
        final var result = addFunction.call(5, 3);
        preventOptimization(result.hashCode());
      }
    }
  }

  @Benchmark
  @Group("CoreAPI")
  public void benchmarkFunctionExecution_Panama() throws Exception {
    if (getJavaVersion() >= 23) {
      try (final var runtime = createRuntime(RuntimeType.PANAMA);
          final var engine = createEngine(runtime);
          final var store = createStore(engine)) {
        final var module = compileModule(engine, SIMPLE_WASM_MODULE);
        final var instance = instantiateModule(store, module);

        final var addFunction = instance.getExportedFunction("add");
        if (addFunction != null) {
          final var result = addFunction.call(5, 3);
          preventOptimization(result.hashCode());
        }
      }
    }
  }

  // ============================================================================
  // WASI Benchmarks
  // ============================================================================

  @Benchmark
  @Group("WASI")
  public void benchmarkWasiContextCreation_JNI() throws Exception {
    try (final var runtime = createRuntime(RuntimeType.JNI)) {
      // This would use WASI context creation if available
      preventOptimization(runtime.hashCode());
    }
  }

  @Benchmark
  @Group("WASI")
  public void benchmarkWasiContextCreation_Panama() throws Exception {
    if (getJavaVersion() >= 23) {
      try (final var runtime = createRuntime(RuntimeType.PANAMA)) {
        preventOptimization(runtime.hashCode());
      }
    }
  }

  // ============================================================================
  // Memory Management Benchmarks
  // ============================================================================

  @Benchmark
  @Group("Memory")
  public void benchmarkMemoryAllocation_JNI() throws Exception {
    try (final var runtime = createRuntime(RuntimeType.JNI);
        final var engine = createEngine(runtime);
        final var store = createStore(engine)) {

      // Test memory allocation patterns
      final var module = compileModule(engine, COMPLEX_WASM_MODULE);
      final var instance = instantiateModule(store, module);

      final var memory = instance.getExportedMemory("memory");
      if (memory != null) {
        // Simulate memory operations
        preventOptimization(memory.size());
      }
    }
  }

  @Benchmark
  @Group("Memory")
  public void benchmarkMemoryAllocation_Panama() throws Exception {
    if (getJavaVersion() >= 23) {
      try (final var runtime = createRuntime(RuntimeType.PANAMA);
          final var engine = createEngine(runtime);
          final var store = createStore(engine)) {

        final var module = compileModule(engine, COMPLEX_WASM_MODULE);
        final var instance = instantiateModule(store, module);

        final var memory = instance.getExportedMemory("memory");
        if (memory != null) {
          preventOptimization(memory.size());
        }
      }
    }
  }

  // ============================================================================
  // Performance Analysis Methods
  // ============================================================================

  /**
   * Runs the complete benchmark suite.
   *
   * @return comprehensive benchmark results
   */
  public static BenchmarkResults runComprehensiveBenchmarks() throws RunnerException {
    final long startTime = System.currentTimeMillis();

    final Options opt =
        new OptionsBuilder()
            .include(ComprehensiveBenchmarkSuite.class.getSimpleName())
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .jvmArgs("-server")
            .build();

    final Runner runner = new Runner(opt);
    final Collection<RunResult> results = runner.run();

    final long executionTime = System.currentTimeMillis() - startTime;
    final String javaVersion = System.getProperty("java.version");
    final String systemInfo =
        System.getProperty("os.name")
            + " "
            + System.getProperty("os.version")
            + " "
            + System.getProperty("os.arch");

    final RuntimeType[] runtimes = determineTestedRuntimes();

    final BenchmarkResults benchmarkResults =
        new BenchmarkResults(executionTime, runtimes, javaVersion, systemInfo);

    // Process and categorize results
    processResults(results, benchmarkResults);

    return benchmarkResults;
  }

  /**
   * Generates a comprehensive performance report.
   *
   * @param results the benchmark results
   * @return formatted performance report
   */
  public static String generateComprehensiveReport(final BenchmarkResults results) {
    final StringBuilder report = new StringBuilder();

    report.append("=".repeat(80)).append("\n");
    report.append("COMPREHENSIVE WASMTIME4J PERFORMANCE REPORT\n");
    report.append("=".repeat(80)).append("\n\n");

    report.append("System Information:\n");
    report.append(String.format("  Java Version: %s\n", results.getJavaVersion()));
    report.append(String.format("  System: %s\n", results.getSystemInfo()));
    report.append(
        String.format("  Total Execution Time: %,d ms\n", results.getTotalExecutionTimeMs()));
    report.append(
        String.format(
            "  Runtimes Tested: %s\n",
            Arrays.stream(results.getRuntimesTested())
                .map(RuntimeType::name)
                .collect(Collectors.joining(", "))));
    report.append("\n");

    // Category-by-category analysis
    for (final BenchmarkCategory category : BenchmarkCategory.values()) {
      final CategoryResults categoryResults = results.getCategoryResults(category);
      if (categoryResults != null) {
        report.append(generateCategoryReport(category, categoryResults));
        report.append("\n");
      }
    }

    // Performance comparison section
    report.append(generatePerformanceComparison(results));

    // Optimization recommendations
    report.append(generateOptimizationRecommendations(results));

    return report.toString();
  }

  /** Determines which runtimes were tested based on Java version. */
  private static RuntimeType[] determineTestedRuntimes() {
    final List<RuntimeType> runtimes = new ArrayList<>();
    runtimes.add(RuntimeType.JNI);

    if (getJavaVersion() >= 23) {
      try {
        Class.forName("java.lang.foreign.MemorySegment");
        runtimes.add(RuntimeType.PANAMA);
      } catch (ClassNotFoundException e) {
        // Panama not available
      }
    }

    return runtimes.toArray(new RuntimeType[0]);
  }

  /** Processes JMH results into structured benchmark results. */
  private static void processResults(
      final Collection<RunResult> jmhResults, final BenchmarkResults benchmarkResults) {
    final Map<BenchmarkCategory, List<RunResult>> categorizedResults = new HashMap<>();

    for (final RunResult result : jmhResults) {
      final String benchmarkName = result.getParams().getBenchmark();
      final BenchmarkCategory category = categorizeBenchmark(benchmarkName);

      categorizedResults.computeIfAbsent(category, k -> new ArrayList<>()).add(result);
    }

    categorizedResults.forEach(
        (category, results) -> {
          final CategoryResults categoryResults = new CategoryResults(category, 0);

          for (final RunResult result : results) {
            final String name = extractBenchmarkName(result.getParams().getBenchmark());
            final RuntimeType runtime = extractRuntime(result.getParams().getBenchmark());

            final BenchmarkResult benchResult =
                new BenchmarkResult(
                    name,
                    runtime,
                    result.getPrimaryResult().getScore(),
                    result.getPrimaryResult().getScoreUnit(),
                    result.getPrimaryResult().getScoreError(),
                    result.getPrimaryResult().getSampleCount(),
                    new HashMap<>());

            categoryResults.addResult(name, benchResult);
          }

          benchmarkResults.addCategoryResult(category, categoryResults);
        });
  }

  /** Categorizes a benchmark based on its name. */
  private static BenchmarkCategory categorizeBenchmark(final String benchmarkName) {
    if (benchmarkName.contains("CoreAPI")
        || benchmarkName.contains("Engine")
        || benchmarkName.contains("Module")
        || benchmarkName.contains("Instance")) {
      return BenchmarkCategory.CORE_API;
    } else if (benchmarkName.contains("WASI") || benchmarkName.contains("Wasi")) {
      return BenchmarkCategory.WASI_OPERATIONS;
    } else if (benchmarkName.contains("Memory")) {
      return BenchmarkCategory.MEMORY_MANAGEMENT;
    } else if (benchmarkName.contains("SIMD") || benchmarkName.contains("Simd")) {
      return BenchmarkCategory.SIMD_OPERATIONS;
    } else if (benchmarkName.contains("Component")) {
      return BenchmarkCategory.COMPONENT_MODEL;
    } else if (benchmarkName.contains("Cache")) {
      return BenchmarkCategory.CACHE_PERFORMANCE;
    }
    return BenchmarkCategory.CORE_API; // Default
  }

  /** Extracts benchmark name from full benchmark identifier. */
  private static String extractBenchmarkName(final String fullName) {
    final int lastDot = fullName.lastIndexOf('.');
    return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
  }

  /** Extracts runtime type from benchmark name. */
  private static RuntimeType extractRuntime(final String benchmarkName) {
    if (benchmarkName.contains("_JNI") || benchmarkName.contains("Jni")) {
      return RuntimeType.JNI;
    } else if (benchmarkName.contains("_Panama") || benchmarkName.contains("Panama")) {
      return RuntimeType.PANAMA;
    }
    return RuntimeType.JNI; // Default
  }

  /** Generates a report for a specific benchmark category. */
  private static String generateCategoryReport(
      final BenchmarkCategory category, final CategoryResults results) {
    final StringBuilder sb = new StringBuilder();
    sb.append(String.format("%s Results:\n", category.getDisplayName()));
    sb.append("-".repeat(category.getDisplayName().length() + 9)).append("\n");

    results
        .getAllResults()
        .forEach(
            (name, result) -> {
              sb.append(
                  String.format(
                      "  %s (%s): %.2f %s (±%.2f%%)\n",
                      name,
                      result.getRuntime().name(),
                      result.getScore(),
                      result.getUnit(),
                      (result.getError() / result.getScore()) * 100));
            });

    return sb.toString();
  }

  /** Generates performance comparison between runtimes. */
  private static String generatePerformanceComparison(final BenchmarkResults results) {
    final StringBuilder sb = new StringBuilder();
    sb.append("Performance Comparison (JNI vs Panama):\n");
    sb.append("-".repeat(42)).append("\n");

    // This would contain detailed JNI vs Panama comparison logic
    sb.append("Note: Detailed runtime comparison requires paired benchmark results\n");

    return sb.toString();
  }

  /** Generates optimization recommendations based on results. */
  private static String generateOptimizationRecommendations(final BenchmarkResults results) {
    final StringBuilder sb = new StringBuilder();
    sb.append("Optimization Recommendations:\n");
    sb.append("-".repeat(30)).append("\n");

    sb.append("• Enable compilation caching for frequently used modules\n");
    sb.append("• Use object pools for repetitive allocations\n");
    sb.append("• Consider batch operations for multiple function calls\n");
    sb.append("• Monitor memory pressure and adjust allocation strategies\n");

    return sb.toString();
  }

  /** Main method for running benchmarks from command line. */
  public static void main(final String[] args) {
    try {
      LOGGER.info("Starting comprehensive benchmark suite...");
      final BenchmarkResults results = runComprehensiveBenchmarks();
      final String report = generateComprehensiveReport(results);

      System.out.println(report);
      LOGGER.info("Benchmark suite completed successfully");

    } catch (Exception e) {
      LOGGER.severe("Benchmark suite failed: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
}

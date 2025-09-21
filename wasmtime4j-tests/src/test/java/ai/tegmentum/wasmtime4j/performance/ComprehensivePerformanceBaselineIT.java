package ai.tegmentum.wasmtime4j.performance;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Function;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Memory;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Comprehensive performance baseline establishment for wasmtime4j. This test suite establishes
 * performance baselines for all critical operations and provides regression detection capabilities.
 * Results are stored for future comparison and trend analysis.
 */
@DisplayName("Comprehensive Performance Baseline")
@EnabledIfSystemProperty(named = "wasmtime4j.test.performance", matches = "true")
class ComprehensivePerformanceBaselineIT extends BaseIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComprehensivePerformanceBaselineIT.class.getName());

  // Performance test configuration
  private static final int WARMUP_ITERATIONS =
      Integer.parseInt(System.getProperty("wasmtime4j.perf.warmup", "1000"));
  private static final int MEASUREMENT_ITERATIONS =
      Integer.parseInt(System.getProperty("wasmtime4j.perf.iterations", "10000"));
  private static final int BENCHMARK_RUNS =
      Integer.parseInt(System.getProperty("wasmtime4j.perf.runs", "5"));

  // Results storage
  private static final Path BASELINE_DIR =
      Paths.get(System.getProperty("wasmtime4j.perf.baseline.dir", "target/performance-baselines"));
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
      .registerModule(new JavaTimeModule());

  private static PerformanceTestSuite testSuite;

  @BeforeAll
  static void setupPerformanceBaselines() throws IOException {
    LOGGER.info("Setting up performance baseline establishment");
    LOGGER.info("Configuration: warmup=" + WARMUP_ITERATIONS +
                ", iterations=" + MEASUREMENT_ITERATIONS +
                ", runs=" + BENCHMARK_RUNS);

    // Create baseline directory
    Files.createDirectories(BASELINE_DIR);

    testSuite = new PerformanceTestSuite();
  }

  @AfterAll
  static void savePerformanceBaselines() throws IOException {
    if (testSuite != null) {
      final String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
      final Path baselineFile = BASELINE_DIR.resolve("baseline_" + timestamp + ".json");

      JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(baselineFile.toFile(), testSuite);
      LOGGER.info("Performance baselines saved to: " + baselineFile);

      // Also save as latest baseline
      final Path latestFile = BASELINE_DIR.resolve("baseline_latest.json");
      JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(latestFile.toFile(), testSuite);

      // Generate performance report
      generatePerformanceReport();
    }
  }

  @Test
  @DisplayName("Should establish engine and store creation performance baselines")
  @Timeout(value = 10, unit = TimeUnit.MINUTES)
  void shouldEstablishEngineAndStoreCreationPerformanceBaselines() throws Exception {
    LOGGER.info("=== Engine and Store Creation Performance Baselines ===");

    // Test JNI runtime
    establishEngineStoreBaselinesForRuntime(RuntimeType.JNI);

    // Test Panama runtime if available
    if (TestUtils.isPanamaAvailable()) {
      establishEngineStoreBaselinesForRuntime(RuntimeType.PANAMA);
    }
  }

  @Test
  @DisplayName("Should establish module compilation performance baselines")
  @Timeout(value = 15, unit = TimeUnit.MINUTES)
  void shouldEstablishModuleCompilationPerformanceBaselines() throws Exception {
    LOGGER.info("=== Module Compilation Performance Baselines ===");

    // Test different module types
    establishModuleCompilationBaselines("simple", TestUtils.createSimpleWasmModule());
    establishModuleCompilationBaselines("arithmetic", TestUtils.createArithmeticWasmModule());
    establishModuleCompilationBaselines("memory", TestUtils.createMemoryWasmModule());
    establishModuleCompilationBaselines("comprehensive", TestUtils.createComprehensiveWasmModule());
  }

  @Test
  @DisplayName("Should establish function invocation performance baselines")
  @Timeout(value = 15, unit = TimeUnit.MINUTES)
  void shouldEstablishFunctionInvocationPerformanceBaselines() throws Exception {
    LOGGER.info("=== Function Invocation Performance Baselines ===");

    // Test JNI runtime
    establishFunctionInvocationBaselinesForRuntime(RuntimeType.JNI);

    // Test Panama runtime if available
    if (TestUtils.isPanamaAvailable()) {
      establishFunctionInvocationBaselinesForRuntime(RuntimeType.PANAMA);
    }
  }

  @Test
  @DisplayName("Should establish memory operations performance baselines")
  @Timeout(value = 15, unit = TimeUnit.MINUTES)
  void shouldEstablishMemoryOperationsPerformanceBaselines() throws Exception {
    LOGGER.info("=== Memory Operations Performance Baselines ===");

    // Test JNI runtime
    establishMemoryOperationsBaselinesForRuntime(RuntimeType.JNI);

    // Test Panama runtime if available
    if (TestUtils.isPanamaAvailable()) {
      establishMemoryOperationsBaselinesForRuntime(RuntimeType.PANAMA);
    }
  }

  @Test
  @DisplayName("Should establish end-to-end workflow performance baselines")
  @Timeout(value = 20, unit = TimeUnit.MINUTES)
  void shouldEstablishEndToEndWorkflowPerformanceBaselines() throws Exception {
    LOGGER.info("=== End-to-End Workflow Performance Baselines ===");

    // Test JNI runtime
    establishEndToEndBaselinesForRuntime(RuntimeType.JNI);

    // Test Panama runtime if available
    if (TestUtils.isPanamaAvailable()) {
      establishEndToEndBaselinesForRuntime(RuntimeType.PANAMA);
    }
  }

  @Test
  @DisplayName("Should compare performance between JNI and Panama runtimes")
  @Timeout(value = 10, unit = TimeUnit.MINUTES)
  void shouldComparePerformanceBetweenJniAndPanamaRuntimes() throws Exception {
    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.info("Skipping runtime comparison - Panama not available");
      return;
    }

    LOGGER.info("=== JNI vs Panama Performance Comparison ===");

    // Compare engine creation
    final BenchmarkResult jniEngineCreation = benchmarkEngineCreation(RuntimeType.JNI);
    final BenchmarkResult panamaEngineCreation = benchmarkEngineCreation(RuntimeType.PANAMA);

    testSuite.addComparison("engine_creation", jniEngineCreation, panamaEngineCreation);

    // Compare function invocation
    final BenchmarkResult jniFunctionCall = benchmarkSimpleFunctionCall(RuntimeType.JNI);
    final BenchmarkResult panamaFunctionCall = benchmarkSimpleFunctionCall(RuntimeType.PANAMA);

    testSuite.addComparison("function_call", jniFunctionCall, panamaFunctionCall);

    // Log comparison results
    logComparisonResults("Engine Creation", jniEngineCreation, panamaEngineCreation);
    logComparisonResults("Function Call", jniFunctionCall, panamaFunctionCall);
  }

  private void establishEngineStoreBaselinesForRuntime(final RuntimeType runtimeType) throws Exception {
    LOGGER.info("Establishing engine/store baselines for " + runtimeType);

    // Benchmark engine creation
    final BenchmarkResult engineCreation = benchmarkEngineCreation(runtimeType);
    testSuite.addBaseline(runtimeType + "_engine_creation", engineCreation);

    // Benchmark store creation
    final BenchmarkResult storeCreation = benchmarkStoreCreation(runtimeType);
    testSuite.addBaseline(runtimeType + "_store_creation", storeCreation);

    LOGGER.info(runtimeType + " Engine creation: " + engineCreation.getSummary());
    LOGGER.info(runtimeType + " Store creation: " + storeCreation.getSummary());
  }

  private void establishModuleCompilationBaselines(final String moduleType, final byte[] moduleBytes)
      throws Exception {
    LOGGER.info("Establishing module compilation baselines for " + moduleType);

    // Test JNI runtime
    final BenchmarkResult jniResult = benchmarkModuleCompilation(RuntimeType.JNI, moduleBytes);
    testSuite.addBaseline("JNI_module_compilation_" + moduleType, jniResult);

    // Test Panama runtime if available
    if (TestUtils.isPanamaAvailable()) {
      final BenchmarkResult panamaResult = benchmarkModuleCompilation(RuntimeType.PANAMA, moduleBytes);
      testSuite.addBaseline("PANAMA_module_compilation_" + moduleType, panamaResult);
    }

    LOGGER.info(moduleType + " module compilation (JNI): " + jniResult.getSummary());
  }

  private void establishFunctionInvocationBaselinesForRuntime(final RuntimeType runtimeType)
      throws Exception {
    LOGGER.info("Establishing function invocation baselines for " + runtimeType);

    // Simple function call
    final BenchmarkResult simpleFunctionCall = benchmarkSimpleFunctionCall(runtimeType);
    testSuite.addBaseline(runtimeType + "_simple_function_call", simpleFunctionCall);

    // Complex function call (if available)
    final BenchmarkResult complexFunctionCall = benchmarkComplexFunctionCall(runtimeType);
    if (complexFunctionCall != null) {
      testSuite.addBaseline(runtimeType + "_complex_function_call", complexFunctionCall);
    }

    LOGGER.info(runtimeType + " Simple function call: " + simpleFunctionCall.getSummary());
  }

  private void establishMemoryOperationsBaselinesForRuntime(final RuntimeType runtimeType)
      throws Exception {
    LOGGER.info("Establishing memory operations baselines for " + runtimeType);

    // Memory read operations
    final BenchmarkResult memoryRead = benchmarkMemoryRead(runtimeType);
    testSuite.addBaseline(runtimeType + "_memory_read", memoryRead);

    // Memory write operations
    final BenchmarkResult memoryWrite = benchmarkMemoryWrite(runtimeType);
    testSuite.addBaseline(runtimeType + "_memory_write", memoryWrite);

    LOGGER.info(runtimeType + " Memory read: " + memoryRead.getSummary());
    LOGGER.info(runtimeType + " Memory write: " + memoryWrite.getSummary());
  }

  private void establishEndToEndBaselinesForRuntime(final RuntimeType runtimeType) throws Exception {
    LOGGER.info("Establishing end-to-end workflow baselines for " + runtimeType);

    // Complete workflow benchmark
    final BenchmarkResult endToEndWorkflow = benchmarkEndToEndWorkflow(runtimeType);
    testSuite.addBaseline(runtimeType + "_end_to_end_workflow", endToEndWorkflow);

    LOGGER.info(runtimeType + " End-to-end workflow: " + endToEndWorkflow.getSummary());
  }

  // Benchmark implementation methods

  private BenchmarkResult benchmarkEngineCreation(final RuntimeType runtimeType) throws Exception {
    return runBenchmark("Engine Creation (" + runtimeType + ")", () -> {
      try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
        try (final Engine engine = runtime.createEngine()) {
          assertThat(engine).isNotNull();
        }
      }
    });
  }

  private BenchmarkResult benchmarkStoreCreation(final RuntimeType runtimeType) throws Exception {
    return runBenchmark("Store Creation (" + runtimeType + ")", () -> {
      try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
        try (final Engine engine = runtime.createEngine()) {
          try (final Store store = engine.createStore()) {
            assertThat(store).isNotNull();
          }
        }
      }
    });
  }

  private BenchmarkResult benchmarkModuleCompilation(final RuntimeType runtimeType,
                                                    final byte[] moduleBytes) throws Exception {
    return runBenchmark("Module Compilation (" + runtimeType + ")", () -> {
      try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
        try (final Engine engine = runtime.createEngine()) {
          final Module module = engine.compileModule(moduleBytes);
          assertThat(module).isNotNull();
        }
      }
    });
  }

  private BenchmarkResult benchmarkSimpleFunctionCall(final RuntimeType runtimeType) throws Exception {
    // Pre-compile module for fair benchmark
    try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      try (final Engine engine = runtime.createEngine()) {
        try (final Store store = engine.createStore()) {
          final byte[] moduleBytes = TestUtils.createSimpleWasmModule();
          final Module module = engine.compileModule(moduleBytes);
          final Instance instance = runtime.instantiate(module);

          final Function addFunction = instance.getFunction("add")
              .orElseThrow(() -> new AssertionError("add function should be exported"));

          final BenchmarkResult result = runBenchmark("Simple Function Call (" + runtimeType + ")", () -> {
            final WasmValue[] args = {WasmValue.i32(10), WasmValue.i32(20)};
            final WasmValue[] results = addFunction.call(args);
            assertThat(results[0].asI32()).isEqualTo(30);
          });

          instance.close();
          return result;
        }
      }
    }
  }

  private BenchmarkResult benchmarkComplexFunctionCall(final RuntimeType runtimeType) throws Exception {
    try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      try (final Engine engine = runtime.createEngine()) {
        try (final Store store = engine.createStore()) {
          final byte[] moduleBytes = TestUtils.createArithmeticWasmModule();
          final Module module = engine.compileModule(moduleBytes);
          final Instance instance = runtime.instantiate(module);

          final Function complexFunction = instance.getFunction("complex_calculation").orElse(null);
          if (complexFunction == null) {
            LOGGER.info("Complex function not available in arithmetic module");
            return null;
          }

          final BenchmarkResult result = runBenchmark("Complex Function Call (" + runtimeType + ")", () -> {
            final WasmValue[] args = {WasmValue.i32(100), WasmValue.i32(200)};
            final WasmValue[] results = complexFunction.call(args);
            assertThat(results).isNotEmpty();
          });

          instance.close();
          return result;
        }
      }
    }
  }

  private BenchmarkResult benchmarkMemoryRead(final RuntimeType runtimeType) throws Exception {
    try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      try (final Engine engine = runtime.createEngine()) {
        try (final Store store = engine.createStore()) {
          final byte[] moduleBytes = TestUtils.createMemoryWasmModule();
          final Module module = engine.compileModule(moduleBytes);
          final Instance instance = runtime.instantiate(module);

          final Memory memory = instance.getMemory("memory").orElse(null);
          if (memory == null) {
            throw new AssertionError("Memory not available for benchmark");
          }

          // Pre-populate memory
          final byte[] testData = new byte[1024];
          Arrays.fill(testData, (byte) 0x42);
          memory.writeBytes(0, testData);

          final BenchmarkResult result = runBenchmark("Memory Read (" + runtimeType + ")", () -> {
            final byte[] readData = memory.readBytes(0, 1024);
            assertThat(readData).hasSize(1024);
          });

          instance.close();
          return result;
        }
      }
    }
  }

  private BenchmarkResult benchmarkMemoryWrite(final RuntimeType runtimeType) throws Exception {
    try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      try (final Engine engine = runtime.createEngine()) {
        try (final Store store = engine.createStore()) {
          final byte[] moduleBytes = TestUtils.createMemoryWasmModule();
          final Module module = engine.compileModule(moduleBytes);
          final Instance instance = runtime.instantiate(module);

          final Memory memory = instance.getMemory("memory").orElse(null);
          if (memory == null) {
            throw new AssertionError("Memory not available for benchmark");
          }

          final byte[] testData = new byte[1024];
          Arrays.fill(testData, (byte) 0x55);

          final BenchmarkResult result = runBenchmark("Memory Write (" + runtimeType + ")", () -> {
            memory.writeBytes(0, testData);
          });

          instance.close();
          return result;
        }
      }
    }
  }

  private BenchmarkResult benchmarkEndToEndWorkflow(final RuntimeType runtimeType) throws Exception {
    final byte[] moduleBytes = TestUtils.createSimpleWasmModule();

    return runBenchmark("End-to-End Workflow (" + runtimeType + ")", () -> {
      try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
        try (final Engine engine = runtime.createEngine()) {
          try (final Store store = engine.createStore()) {
            final Module module = engine.compileModule(moduleBytes);
            final Instance instance = runtime.instantiate(module);

            final Function addFunction = instance.getFunction("add")
                .orElseThrow(() -> new AssertionError("add function should be exported"));

            final WasmValue[] args = {WasmValue.i32(15), WasmValue.i32(27)};
            final WasmValue[] results = addFunction.call(args);

            assertThat(results[0].asI32()).isEqualTo(42);

            instance.close();
          }
        }
      }
    });
  }

  // Utility methods

  private BenchmarkResult runBenchmark(final String name, final BenchmarkOperation operation)
      throws Exception {
    LOGGER.info("Running benchmark: " + name);

    // Warmup phase
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      operation.execute();
    }

    // Measurement phase
    final List<Double> measurements = new ArrayList<>();

    for (int run = 0; run < BENCHMARK_RUNS; run++) {
      final List<Long> runTimes = new ArrayList<>();

      for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
        final long startTime = System.nanoTime();
        operation.execute();
        final long endTime = System.nanoTime();
        runTimes.add(endTime - startTime);
      }

      // Calculate average for this run (in microseconds)
      final double runAverage = runTimes.stream()
          .mapToLong(Long::longValue)
          .average()
          .orElse(0.0) / 1000.0;

      measurements.add(runAverage);
    }

    return new BenchmarkResult(name, measurements);
  }

  private void logComparisonResults(final String operationName,
                                   final BenchmarkResult jniResult,
                                   final BenchmarkResult panamaResult) {
    final double jniAvg = jniResult.getAverageMicroseconds();
    final double panamaAvg = panamaResult.getAverageMicroseconds();
    final double ratio = jniAvg / panamaAvg;

    LOGGER.info(operationName + " Performance Comparison:");
    LOGGER.info("  JNI: " + String.format("%.2f", jniAvg) + " μs");
    LOGGER.info("  Panama: " + String.format("%.2f", panamaAvg) + " μs");
    LOGGER.info("  Ratio (JNI/Panama): " + String.format("%.2f", ratio));

    if (ratio > 1.2) {
      LOGGER.info("  Panama is " + String.format("%.1f", ratio) + "x faster than JNI");
    } else if (ratio < 0.8) {
      LOGGER.info("  JNI is " + String.format("%.1f", 1.0 / ratio) + "x faster than Panama");
    } else {
      LOGGER.info("  Performance is comparable between runtimes");
    }
  }

  private static void generatePerformanceReport() throws IOException {
    final StringBuilder report = new StringBuilder();
    report.append("Wasmtime4j Performance Baseline Report\n");
    report.append("=====================================\n\n");

    report.append("Test Environment:\n");
    report.append("  Java Version: ").append(System.getProperty("java.version")).append("\n");
    report.append("  JVM: ").append(System.getProperty("java.vm.name")).append("\n");
    report.append("  OS: ").append(System.getProperty("os.name")).append(" ");
    report.append(System.getProperty("os.version")).append("\n");
    report.append("  Architecture: ").append(System.getProperty("os.arch")).append("\n");
    report.append("  Timestamp: ").append(Instant.now()).append("\n\n");

    report.append("Test Configuration:\n");
    report.append("  Warmup Iterations: ").append(WARMUP_ITERATIONS).append("\n");
    report.append("  Measurement Iterations: ").append(MEASUREMENT_ITERATIONS).append("\n");
    report.append("  Benchmark Runs: ").append(BENCHMARK_RUNS).append("\n\n");

    report.append("Performance Baselines:\n");
    report.append("======================\n");

    for (final Map.Entry<String, BenchmarkResult> entry : testSuite.getBaselines().entrySet()) {
      final String name = entry.getKey();
      final BenchmarkResult result = entry.getValue();

      report.append(name).append(":\n");
      report.append("  Average: ").append(String.format("%.2f", result.getAverageMicroseconds())).append(" μs\n");
      report.append("  Std Dev: ").append(String.format("%.2f", result.getStandardDeviation())).append(" μs\n");
      report.append("  Min: ").append(String.format("%.2f", result.getMinMicroseconds())).append(" μs\n");
      report.append("  Max: ").append(String.format("%.2f", result.getMaxMicroseconds())).append(" μs\n");
      report.append("  CV: ").append(String.format("%.2f", result.getCoefficientOfVariation())).append("%\n\n");
    }

    if (!testSuite.getComparisons().isEmpty()) {
      report.append("Runtime Comparisons:\n");
      report.append("====================\n");

      for (final Map.Entry<String, PerformanceComparison> entry : testSuite.getComparisons().entrySet()) {
        final String name = entry.getKey();
        final PerformanceComparison comparison = entry.getValue();

        report.append(name).append(":\n");
        report.append("  JNI: ").append(String.format("%.2f", comparison.jniResult.getAverageMicroseconds())).append(" μs\n");
        report.append("  Panama: ").append(String.format("%.2f", comparison.panamaResult.getAverageMicroseconds())).append(" μs\n");
        report.append("  Ratio: ").append(String.format("%.2f", comparison.getRatio())).append("\n\n");
      }
    }

    final Path reportFile = BASELINE_DIR.resolve("performance_report.txt");
    Files.write(reportFile, report.toString().getBytes());
    LOGGER.info("Performance report generated: " + reportFile);
  }

  // Inner classes for data structures

  @FunctionalInterface
  private interface BenchmarkOperation {
    void execute() throws Exception;
  }

  private static class BenchmarkResult {
    @JsonProperty private final String name;
    @JsonProperty private final List<Double> measurements;
    @JsonProperty private final double average;
    @JsonProperty private final double standardDeviation;
    @JsonProperty private final double min;
    @JsonProperty private final double max;

    public BenchmarkResult(final String name, final List<Double> measurements) {
      this.name = name;
      this.measurements = new ArrayList<>(measurements);

      final DoubleStream stream = measurements.stream().mapToDouble(Double::doubleValue);
      this.average = stream.average().orElse(0.0);
      this.min = measurements.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
      this.max = measurements.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

      final double variance = measurements.stream()
          .mapToDouble(val -> Math.pow(val - average, 2))
          .average().orElse(0.0);
      this.standardDeviation = Math.sqrt(variance);
    }

    public String getName() { return name; }
    public List<Double> getMeasurements() { return new ArrayList<>(measurements); }
    public double getAverageMicroseconds() { return average; }
    public double getStandardDeviation() { return standardDeviation; }
    public double getMinMicroseconds() { return min; }
    public double getMaxMicroseconds() { return max; }
    public double getCoefficientOfVariation() {
      return average > 0 ? (standardDeviation / average) * 100.0 : 0.0;
    }

    public String getSummary() {
      return String.format("%.2f±%.2f μs (min=%.2f, max=%.2f, cv=%.1f%%)",
                          average, standardDeviation, min, max, getCoefficientOfVariation());
    }
  }

  private static class PerformanceComparison {
    @JsonProperty final BenchmarkResult jniResult;
    @JsonProperty final BenchmarkResult panamaResult;

    public PerformanceComparison(final BenchmarkResult jniResult, final BenchmarkResult panamaResult) {
      this.jniResult = jniResult;
      this.panamaResult = panamaResult;
    }

    public double getRatio() {
      return jniResult.getAverageMicroseconds() / panamaResult.getAverageMicroseconds();
    }
  }

  private static class PerformanceTestSuite {
    @JsonProperty private final Map<String, BenchmarkResult> baselines = new HashMap<>();
    @JsonProperty private final Map<String, PerformanceComparison> comparisons = new HashMap<>();
    @JsonProperty private final Instant timestamp = Instant.now();

    public void addBaseline(final String name, final BenchmarkResult result) {
      baselines.put(name, result);
    }

    public void addComparison(final String name, final BenchmarkResult jniResult,
                             final BenchmarkResult panamaResult) {
      comparisons.put(name, new PerformanceComparison(jniResult, panamaResult));
    }

    public Map<String, BenchmarkResult> getBaselines() { return baselines; }
    public Map<String, PerformanceComparison> getComparisons() { return comparisons; }
    public Instant getTimestamp() { return timestamp; }
  }
}
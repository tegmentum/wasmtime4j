package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.functions.WasmFunction;
import ai.tegmentum.wasmtime4j.performance.PerformanceTestHarness;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive performance tests for WASI functionality. Tests I/O operation benchmarks, context
 * creation performance, memory usage patterns, and cross-runtime performance comparison for WASI
 * operations.
 */
@Tag(TestCategories.PERFORMANCE)
@Tag(TestCategories.WASI)
@Tag(TestCategories.BENCHMARKS)
public final class WasiPerformanceTest {
  private static final Logger LOGGER = Logger.getLogger(WasiPerformanceTest.class.getName());

  @TempDir private Path tempDirectory;

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp() {
    runtime = WasmRuntimeFactory.create();
    engine = runtime.createEngine();
    store = engine.createStore();
    LOGGER.info("Set up WASI performance test with runtime: " + runtime.getRuntimeType());
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    if (runtime != null) {
      runtime.close();
    }
  }

  /** Tests WASI context creation performance. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testWasiContextCreationPerformance() {
    LOGGER.info("Testing WASI context creation performance");

    final PerformanceTestHarness.BenchmarkOperation contextCreation =
        () -> {
          final WasiConfig config =
              WasiConfig.builder()
                  .inheritEnv(true)
                  .inheritStdin(true)
                  .inheritStdout(true)
                  .inheritStderr(true)
                  .build();

          final Wasi wasi = store.createWasi(config);
          assertTrue(wasi.isValid());
          wasi.close();
        };

    final PerformanceTestHarness.Configuration config =
        PerformanceTestHarness.getFastConfiguration();

    final PerformanceTestHarness.MeasurementResult result =
        PerformanceTestHarness.runBenchmark("WASI Context Creation", contextCreation, config);

    assertNotNull(result);
    assertTrue(result.getMean() > 0);

    LOGGER.info(
        String.format(
            "WASI context creation: %.2f ops/sec (%.2f μs/op)",
            result.getOperationsPerSecond(), result.getMean() / 1000.0));
  }

  /** Tests WASI context creation with complex configuration performance. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testComplexWasiContextCreationPerformance() throws IOException {
    LOGGER.info("Testing complex WASI context creation performance");

    final Path testDir = tempDirectory.resolve("perf_test");
    Files.createDirectories(testDir);

    final Map<String, String> largeEnv = new HashMap<>();
    for (int i = 0; i < 100; i++) {
      largeEnv.put("PERF_VAR_" + i, "performance_value_" + i);
    }

    final List<String> manyArgs =
        Arrays.asList(
            "program",
            "--perf",
            "--benchmark",
            "--iterations",
            "1000",
            "--input",
            "file1.txt",
            "--input",
            "file2.txt",
            "--output",
            "result.txt");

    final PerformanceTestHarness.BenchmarkOperation complexContextCreation =
        () -> {
          final WasiConfig config =
              WasiConfig.builder()
                  .inheritEnv(false)
                  .environment(largeEnv)
                  .arguments(manyArgs)
                  .preopenDir(testDir.toString(), "perf", true, true)
                  .inheritStdin(true)
                  .inheritStdout(true)
                  .inheritStderr(true)
                  .build();

          final Wasi wasi = store.createWasi(config);
          assertTrue(wasi.isValid());

          // Verify configuration
          assertEquals(100, wasi.getEnvironment().size());
          assertEquals(manyArgs.size(), wasi.getArguments().size());

          wasi.close();
        };

    final PerformanceTestHarness.Configuration config =
        PerformanceTestHarness.getFastConfiguration();

    final PerformanceTestHarness.MeasurementResult result =
        PerformanceTestHarness.runBenchmark(
            "Complex WASI Context Creation", complexContextCreation, config);

    assertNotNull(result);
    assertTrue(result.getMean() > 0);

    LOGGER.info(
        String.format(
            "Complex WASI context creation: %.2f ops/sec (%.2f μs/op)",
            result.getOperationsPerSecond(), result.getMean() / 1000.0));
  }

  /** Tests WASI module instantiation performance. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testWasiModuleInstantiationPerformance() {
    LOGGER.info("Testing WASI module instantiation performance");

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");
    final Module module = engine.createModule(wasmBytes);

    final PerformanceTestHarness.BenchmarkOperation moduleInstantiation =
        () -> {
          final WasiConfig config =
              WasiConfig.builder()
                  .inheritEnv(true)
                  .inheritStdin(true)
                  .inheritStdout(true)
                  .inheritStderr(true)
                  .build();

          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          assertTrue(instance.isValid());

          instance.close();
          wasi.close();
        };

    final PerformanceTestHarness.Configuration config =
        PerformanceTestHarness.getFastConfiguration();

    final PerformanceTestHarness.MeasurementResult result =
        PerformanceTestHarness.runBenchmark(
            "WASI Module Instantiation", moduleInstantiation, config);

    assertNotNull(result);
    assertTrue(result.getMean() > 0);

    LOGGER.info(
        String.format(
            "WASI module instantiation: %.2f ops/sec (%.2f μs/op)",
            result.getOperationsPerSecond(), result.getMean() / 1000.0));

    module.close();
  }

  /** Tests WASI I/O redirection performance. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testWasiIORedirectionPerformance() {
    LOGGER.info("Testing WASI I/O redirection performance");

    final String testInput = "Performance test input data\n".repeat(100);
    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");
    final Module module = engine.createModule(wasmBytes);

    final PerformanceTestHarness.BenchmarkOperation ioRedirection =
        () -> {
          final ByteArrayInputStream stdin = new ByteArrayInputStream(testInput.getBytes());
          final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
          final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

          final WasiConfig config =
              WasiConfig.builder()
                  .inheritEnv(true)
                  .stdin(stdin)
                  .stdout(stdout)
                  .stderr(stderr)
                  .build();

          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Execute I/O operation
          if (instance.hasExport("_start")) {
            final WasmFunction startFunction = instance.getExport("_start").asFunction();
            assertDoesNotThrow(() -> startFunction.call());
          }

          final byte[] output = stdout.toByteArray();
          assertNotNull(output);

          instance.close();
          wasi.close();
        };

    final PerformanceTestHarness.Configuration config =
        PerformanceTestHarness.getFastConfiguration();

    final PerformanceTestHarness.MeasurementResult result =
        PerformanceTestHarness.runBenchmark("WASI I/O Redirection", ioRedirection, config);

    assertNotNull(result);
    assertTrue(result.getMean() > 0);

    LOGGER.info(
        String.format(
            "WASI I/O redirection: %.2f ops/sec (%.2f μs/op)",
            result.getOperationsPerSecond(), result.getMean() / 1000.0));

    module.close();
  }

  /** Tests large data I/O performance through WASI. */
  @Test
  @Timeout(value = 120, unit = TimeUnit.SECONDS)
  void testLargeDataIOPerformance() {
    LOGGER.info("Testing large data I/O performance");

    // Create 1MB of test data
    final int dataSize = 1024 * 1024;
    final byte[] largeData = new byte[dataSize];
    Arrays.fill(largeData, (byte) 0xAA);

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");
    final Module module = engine.createModule(wasmBytes);

    final PerformanceTestHarness.BenchmarkOperation largeDataIO =
        () -> {
          final ByteArrayInputStream stdin = new ByteArrayInputStream(largeData);
          final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

          final WasiConfig config =
              WasiConfig.builder()
                  .inheritEnv(true)
                  .stdin(stdin)
                  .stdout(stdout)
                  .inheritStderr(true)
                  .build();

          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Process large data
          if (instance.hasExport("_start")) {
            final WasmFunction startFunction = instance.getExport("_start").asFunction();
            assertDoesNotThrow(() -> startFunction.call());
          }

          final byte[] output = stdout.toByteArray();
          assertNotNull(output);

          instance.close();
          wasi.close();
        };

    final PerformanceTestHarness.Configuration config =
        PerformanceTestHarness.Configuration.builder()
            .warmupIterations(2)
            .measurementIterations(3)
            .iterationTime(Duration.ofSeconds(2))
            .build();

    final PerformanceTestHarness.MeasurementResult result =
        PerformanceTestHarness.runBenchmark("Large Data I/O", largeDataIO, config);

    assertNotNull(result);
    assertTrue(result.getMean() > 0);

    final double mbPerSecond = (dataSize * result.getOperationsPerSecond()) / (1024 * 1024);
    LOGGER.info(
        String.format(
            "Large data I/O: %.2f ops/sec (%.2f MB/sec throughput)",
            result.getOperationsPerSecond(), mbPerSecond));

    module.close();
  }

  /** Tests WASI filesystem operation performance. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testWasiFilesystemPerformance() throws IOException {
    LOGGER.info("Testing WASI filesystem performance");

    final Path testDir = tempDirectory.resolve("fs_perf");
    Files.createDirectories(testDir);

    // Create test files
    for (int i = 0; i < 10; i++) {
      final Path testFile = testDir.resolve("file_" + i + ".txt");
      Files.write(testFile, ("File content " + i + "\n").repeat(100).getBytes());
    }

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");
    final Module module = engine.createModule(wasmBytes);

    final PerformanceTestHarness.BenchmarkOperation filesystemOps =
        () -> {
          final WasiConfig config =
              WasiConfig.builder()
                  .inheritEnv(true)
                  .preopenDir(testDir.toString(), "testdir", true, false)
                  .inheritStdin(true)
                  .inheritStdout(true)
                  .inheritStderr(true)
                  .build();

          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Access filesystem through WASI
          if (instance.hasExport("_start")) {
            final WasmFunction startFunction = instance.getExport("_start").asFunction();
            assertDoesNotThrow(() -> startFunction.call());
          }

          instance.close();
          wasi.close();
        };

    final PerformanceTestHarness.Configuration config =
        PerformanceTestHarness.getFastConfiguration();

    final PerformanceTestHarness.MeasurementResult result =
        PerformanceTestHarness.runBenchmark("WASI Filesystem Operations", filesystemOps, config);

    assertNotNull(result);
    assertTrue(result.getMean() > 0);

    LOGGER.info(
        String.format(
            "WASI filesystem operations: %.2f ops/sec (%.2f μs/op)",
            result.getOperationsPerSecond(), result.getMean() / 1000.0));

    module.close();
  }

  /** Tests cross-runtime WASI performance comparison. */
  @Test
  @Timeout(value = 120, unit = TimeUnit.SECONDS)
  void testCrossRuntimeWasiPerformance() {
    LOGGER.info("Testing cross-runtime WASI performance");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime performance test");
      return;
    }

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");

    final PerformanceTestHarness.RuntimeBenchmarkOperation wasiOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasiConfig config =
                WasiConfig.builder()
                    .inheritEnv(true)
                    .arguments(Arrays.asList("perf_test", "--benchmark"))
                    .inheritStdin(true)
                    .inheritStdout(true)
                    .inheritStderr(true)
                    .build();

            final Module module = engine.createModule(wasmBytes);
            final Wasi wasi = store.createWasi(config);
            final Instance instance = store.createInstance(module, wasi.getImports());

            if (instance.hasExport("_start")) {
              final WasmFunction startFunction = instance.getExport("_start").asFunction();
              startFunction.call();
            }

            instance.close();
            wasi.close();
            module.close();
          }
        };

    final PerformanceTestHarness.Configuration config =
        PerformanceTestHarness.getFastConfiguration();

    final PerformanceTestHarness.ComparisonResult comparison =
        PerformanceTestHarness.runCrossRuntimeBenchmark(
            "Cross-Runtime WASI", wasiOperation, config);

    assertNotNull(comparison);
    assertTrue(comparison.getBaseline().getMean() > 0);
    assertTrue(comparison.getComparison().getMean() > 0);

    LOGGER.info(
        String.format(
            "Cross-runtime WASI performance - JNI: %.2f ops/sec, Panama: %.2f ops/sec (%.2fx)",
            comparison.getBaseline().getOperationsPerSecond(),
            comparison.getComparison().getOperationsPerSecond(),
            comparison.getSpeedupRatio()));

    final String comparisonReport = PerformanceTestHarness.generateComparisonReport(comparison);
    LOGGER.info("\n" + comparisonReport);
  }

  /** Tests WASI memory usage and allocation patterns. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testWasiMemoryUsagePerformance() {
    LOGGER.info("Testing WASI memory usage performance");

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");
    final Module module = engine.createModule(wasmBytes);

    final PerformanceTestHarness.BenchmarkOperation memoryTest =
        () -> {
          final Map<String, String> envVars = new HashMap<>();
          for (int i = 0; i < 50; i++) {
            envVars.put("MEM_VAR_" + i, "value_" + i + "_" + "x".repeat(20));
          }

          final WasiConfig config =
              WasiConfig.builder()
                  .inheritEnv(false)
                  .environment(envVars)
                  .arguments(Arrays.asList("memory_test", "--allocate", "1000"))
                  .inheritStdin(true)
                  .inheritStdout(true)
                  .inheritStderr(true)
                  .build();

          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Force some memory allocation through function calls
          if (instance.hasExport("_start")) {
            final WasmFunction startFunction = instance.getExport("_start").asFunction();
            assertDoesNotThrow(() -> startFunction.call());
          }

          instance.close();
          wasi.close();
        };

    final PerformanceTestHarness.Configuration config =
        PerformanceTestHarness.getFastConfiguration();

    final PerformanceTestHarness.MeasurementResult result =
        PerformanceTestHarness.runBenchmark("WASI Memory Usage", memoryTest, config);

    assertNotNull(result);
    assertTrue(result.getMean() > 0);

    LOGGER.info(
        String.format(
            "WASI memory usage: %.2f ops/sec (%.2f μs/op)",
            result.getOperationsPerSecond(), result.getMean() / 1000.0));

    module.close();
  }

  /** Tests WASI scalability with increasing load. */
  @Test
  @Timeout(value = 120, unit = TimeUnit.SECONDS)
  void testWasiScalabilityPerformance() {
    LOGGER.info("Testing WASI scalability performance");

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");
    final Module module = engine.createModule(wasmBytes);

    final PerformanceTestHarness.BenchmarkOperation scalabilityTest =
        () -> {
          final WasiConfig config =
              WasiConfig.builder()
                  .inheritEnv(true)
                  .inheritStdin(true)
                  .inheritStdout(true)
                  .inheritStderr(true)
                  .build();

          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Execute multiple operations to test scalability
          if (instance.hasExport("_start")) {
            final WasmFunction startFunction = instance.getExport("_start").asFunction();

            // Simulate load with multiple calls
            for (int i = 0; i < 5; i++) {
              assertDoesNotThrow(() -> startFunction.call());
            }
          }

          instance.close();
          wasi.close();
        };

    final int[] threadCounts = {1, 2, 4, 8};
    final PerformanceTestHarness.Configuration config =
        PerformanceTestHarness.Configuration.builder()
            .warmupIterations(2)
            .measurementIterations(3)
            .iterationTime(Duration.ofSeconds(1))
            .build();

    final Map<Integer, PerformanceTestHarness.MeasurementResult> scalabilityResults =
        PerformanceTestHarness.runThroughputBenchmark(
            "WASI Scalability", scalabilityTest, config, threadCounts);

    assertNotNull(scalabilityResults);

    for (final Map.Entry<Integer, PerformanceTestHarness.MeasurementResult> entry :
        scalabilityResults.entrySet()) {
      final int threads = entry.getKey();
      final PerformanceTestHarness.MeasurementResult result = entry.getValue();

      LOGGER.info(
          String.format(
              "WASI scalability (%d threads): %.2f ops/sec",
              threads, result.getOperationsPerSecond()));
    }

    module.close();
  }

  /** Tests WASI performance regression detection. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testWasiPerformanceRegressionDetection() {
    LOGGER.info("Testing WASI performance regression detection");

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");
    final Module module = engine.createModule(wasmBytes);

    final PerformanceTestHarness.BenchmarkOperation baselineTest =
        () -> {
          final WasiConfig config =
              WasiConfig.builder()
                  .inheritEnv(true)
                  .inheritStdin(true)
                  .inheritStdout(true)
                  .inheritStderr(true)
                  .build();

          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          if (instance.hasExport("_start")) {
            final WasmFunction startFunction = instance.getExport("_start").asFunction();
            assertDoesNotThrow(() -> startFunction.call());
          }

          instance.close();
          wasi.close();
        };

    final PerformanceTestHarness.Configuration config =
        PerformanceTestHarness.getDefaultConfiguration();

    // Run baseline measurement
    final PerformanceTestHarness.MeasurementResult baselineResult =
        PerformanceTestHarness.runBenchmark("WASI Baseline", baselineTest, config);

    // Run current measurement
    final PerformanceTestHarness.MeasurementResult currentResult =
        PerformanceTestHarness.runBenchmark("WASI Current", baselineTest, config);

    // Analyze for regression
    final List<PerformanceTestHarness.MeasurementResult> historicalResults =
        Arrays.asList(baselineResult);

    final PerformanceTestHarness.PerformanceRegressionAnalysis regression =
        PerformanceTestHarness.analyzePerformanceRegression(
            "WASI Regression Test", currentResult, historicalResults, 0.1);

    assertNotNull(regression);

    LOGGER.info("Regression analysis: " + regression.getAnalysis());

    if (regression.isRegression()) {
      LOGGER.warning(
          "Performance regression detected: "
              + (regression.getRegressionRatio() * 100)
              + "% degradation");
    }

    module.close();
  }
}

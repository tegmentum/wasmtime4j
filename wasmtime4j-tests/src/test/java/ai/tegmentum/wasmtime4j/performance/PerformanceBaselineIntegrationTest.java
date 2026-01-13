/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.performance;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;

/**
 * Performance baseline integration tests for wasmtime4j operations.
 *
 * <p>These tests establish performance baselines for:
 *
 * <ul>
 *   <li>Engine creation and configuration
 *   <li>Module compilation from WAT/WASM
 *   <li>Instance creation
 *   <li>Function invocation overhead
 *   <li>Memory operations
 *   <li>Native call overhead
 * </ul>
 *
 * <p>Results are logged for analysis and can be compared against JMH benchmark results.
 *
 * @since 1.0.0
 */
@DisplayName("Performance Baseline Integration Tests")
public final class PerformanceBaselineIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(PerformanceBaselineIntegrationTest.class.getName());

  // Performance thresholds (generous to avoid flaky tests)
  private static final long MAX_ENGINE_CREATION_MS = 500;
  private static final long MAX_MODULE_COMPILE_MS = 1000;
  private static final long MAX_INSTANCE_CREATION_MS = 100;
  private static final long MAX_FUNCTION_CALL_US = 1000; // 1ms per call
  private static final long MAX_MEMORY_OVERHEAD_BYTES = 50 * 1024 * 1024; // 50MB

  // Warm-up and measurement iterations
  private static final int WARMUP_ITERATIONS = 100;
  private static final int MEASUREMENT_ITERATIONS = 1000;

  // Simple WAT for testing
  private static final String ADD_WAT =
      "(module\n"
          + "  (func $add (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add)\n"
          + ")";

  private static final String MEMORY_WAT =
      "(module\n"
          + "  (memory (export \"memory\") 1 10)\n"
          + "  (func (export \"load\") (param i32) (result i32)\n"
          + "    local.get 0\n"
          + "    i32.load)\n"
          + "  (func (export \"store\") (param i32 i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.store)\n"
          + ")";

  private static boolean nativeAvailable = false;
  private static String unavailableReason;

  @BeforeAll
  static void checkNativeAvailable() {
    try {
      final Engine testEngine = Engine.create();
      testEngine.close();
      nativeAvailable = true;
      LOGGER.info("Native Wasmtime library available for performance testing");
    } catch (final Throwable t) {
      unavailableReason = "Native library not available: " + t.getMessage();
      LOGGER.warning("Performance tests will be skipped: " + unavailableReason);
    }
  }

  private static void assumeNativeAvailable() {
    assumeTrue(nativeAvailable, "Native library not available: " + unavailableReason);
  }

  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("Engine Creation Performance")
  class EngineCreationPerformanceTests {

    @Test
    @Timeout(30)
    @DisplayName("should create engine within acceptable time")
    void shouldCreateEngineWithinAcceptableTime(final TestInfo testInfo) throws Exception {
      assumeNativeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Warmup
      for (int i = 0; i < 10; i++) {
        try (Engine engine = Engine.create()) {
          assertNotNull(engine);
        }
      }

      // Measure single engine creation
      final long startTime = System.nanoTime();
      final Engine engine = Engine.create();
      final long duration = System.nanoTime() - startTime;
      resources.add(engine);

      final long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);
      LOGGER.info(
          String.format("Engine creation time: %d ns (%.2f ms)", duration, durationMs / 1.0));

      assertTrue(
          durationMs < MAX_ENGINE_CREATION_MS,
          String.format(
              "Engine creation should complete within %d ms (took %d ms)",
              MAX_ENGINE_CREATION_MS, durationMs));
    }

    @Test
    @Timeout(60)
    @DisplayName("should measure repeated engine creation performance")
    void shouldMeasureRepeatedEngineCreationPerformance(final TestInfo testInfo) throws Exception {
      assumeNativeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Warmup
      for (int i = 0; i < WARMUP_ITERATIONS; i++) {
        try (Engine engine = Engine.create()) {
          assertNotNull(engine);
        }
      }

      // Measure multiple engine creations
      final List<Long> measurements = new ArrayList<>();
      for (int i = 0; i < 100; i++) {
        final long startTime = System.nanoTime();
        try (Engine engine = Engine.create()) {
          assertNotNull(engine);
          final long duration = System.nanoTime() - startTime;
          measurements.add(duration);
        }
      }

      // Calculate statistics
      Collections.sort(measurements);
      final long minTime = measurements.get(0);
      final long maxTime = measurements.get(measurements.size() - 1);
      final long medianTime = measurements.get(measurements.size() / 2);
      final double avgTime = measurements.stream().mapToLong(Long::longValue).average().orElse(0);
      final long p95Time = measurements.get((int) (measurements.size() * 0.95));

      LOGGER.info(
          String.format(
              "Engine creation stats - Min: %.2f ms, Median: %.2f ms, Avg: %.2f ms, P95: %.2f ms,"
                  + " Max: %.2f ms",
              minTime / 1_000_000.0,
              medianTime / 1_000_000.0,
              avgTime / 1_000_000.0,
              p95Time / 1_000_000.0,
              maxTime / 1_000_000.0));

      assertTrue(
          TimeUnit.NANOSECONDS.toMillis(medianTime) < MAX_ENGINE_CREATION_MS,
          "Median engine creation time should be within threshold");
    }
  }

  @Nested
  @DisplayName("Module Compilation Performance")
  class ModuleCompilationPerformanceTests {

    @Test
    @Timeout(30)
    @DisplayName("should compile simple module within acceptable time")
    void shouldCompileSimpleModuleWithinAcceptableTime(final TestInfo testInfo) throws Exception {
      assumeNativeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);

      // Warmup
      for (int i = 0; i < 10; i++) {
        try (Module module = engine.compileWat(ADD_WAT)) {
          assertNotNull(module);
        }
      }

      // Measure compilation
      final long startTime = System.nanoTime();
      final Module module = engine.compileWat(ADD_WAT);
      final long duration = System.nanoTime() - startTime;
      resources.add(module);

      final long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);
      LOGGER.info(
          String.format("Module compilation time: %d ns (%.2f ms)", duration, durationMs / 1.0));

      assertTrue(
          durationMs < MAX_MODULE_COMPILE_MS,
          String.format(
              "Module compilation should complete within %d ms (took %d ms)",
              MAX_MODULE_COMPILE_MS, durationMs));
    }

    @Test
    @Timeout(60)
    @DisplayName("should measure repeated module compilation performance")
    void shouldMeasureRepeatedModuleCompilationPerformance(final TestInfo testInfo)
        throws Exception {
      assumeNativeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);

      // Warmup
      for (int i = 0; i < WARMUP_ITERATIONS; i++) {
        try (Module module = engine.compileWat(ADD_WAT)) {
          assertNotNull(module);
        }
      }

      // Measure multiple compilations
      final List<Long> measurements = new ArrayList<>();
      for (int i = 0; i < 100; i++) {
        final long startTime = System.nanoTime();
        try (Module module = engine.compileWat(ADD_WAT)) {
          assertNotNull(module);
          final long duration = System.nanoTime() - startTime;
          measurements.add(duration);
        }
      }

      // Calculate statistics
      Collections.sort(measurements);
      final long minTime = measurements.get(0);
      final long medianTime = measurements.get(measurements.size() / 2);
      final double avgTime = measurements.stream().mapToLong(Long::longValue).average().orElse(0);
      final long p95Time = measurements.get((int) (measurements.size() * 0.95));
      final long maxTime = measurements.get(measurements.size() - 1);

      LOGGER.info(
          String.format(
              "Module compilation stats - Min: %.2f ms, Median: %.2f ms, Avg: %.2f ms, P95: %.2f"
                  + " ms, Max: %.2f ms",
              minTime / 1_000_000.0,
              medianTime / 1_000_000.0,
              avgTime / 1_000_000.0,
              p95Time / 1_000_000.0,
              maxTime / 1_000_000.0));

      assertTrue(
          TimeUnit.NANOSECONDS.toMillis(medianTime) < MAX_MODULE_COMPILE_MS,
          "Median module compilation time should be within threshold");
    }
  }

  @Nested
  @DisplayName("Instance Creation Performance")
  class InstanceCreationPerformanceTests {

    @Test
    @Timeout(30)
    @DisplayName("should create instance within acceptable time")
    void shouldCreateInstanceWithinAcceptableTime(final TestInfo testInfo) throws Exception {
      assumeNativeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Module module = engine.compileWat(ADD_WAT);
      resources.add(module);
      final Store store = Store.create(engine);
      resources.add(store);

      // Warmup
      for (int i = 0; i < 10; i++) {
        try (Instance instance = module.instantiate(store)) {
          assertNotNull(instance);
        }
      }

      // Measure instance creation
      final long startTime = System.nanoTime();
      final Instance instance = module.instantiate(store);
      final long duration = System.nanoTime() - startTime;
      resources.add(instance);

      final long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);
      LOGGER.info(
          String.format("Instance creation time: %d ns (%.2f ms)", duration, durationMs / 1.0));

      assertTrue(
          durationMs < MAX_INSTANCE_CREATION_MS,
          String.format(
              "Instance creation should complete within %d ms (took %d ms)",
              MAX_INSTANCE_CREATION_MS, durationMs));
    }
  }

  @Nested
  @DisplayName("Function Call Performance")
  class FunctionCallPerformanceTests {

    @Test
    @Timeout(60)
    @DisplayName("should call function within acceptable time")
    void shouldCallFunctionWithinAcceptableTime(final TestInfo testInfo) throws Exception {
      assumeNativeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Module module = engine.compileWat(ADD_WAT);
      resources.add(module);
      final Store store = Store.create(engine);
      resources.add(store);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final Optional<WasmFunction> addFuncOpt = instance.getFunction("add");
      assertTrue(addFuncOpt.isPresent(), "add function should exist");
      final WasmFunction addFunc = addFuncOpt.get();

      // Warmup
      for (int i = 0; i < WARMUP_ITERATIONS; i++) {
        addFunc.call(WasmValue.i32(i), WasmValue.i32(i));
      }

      // Measure single function call
      final long startTime = System.nanoTime();
      final WasmValue[] result = addFunc.call(WasmValue.i32(5), WasmValue.i32(7));
      final long duration = System.nanoTime() - startTime;

      assertNotNull(result);
      assertTrue(result.length > 0, "Result should have values");

      final long durationUs = TimeUnit.NANOSECONDS.toMicros(duration);
      LOGGER.info(String.format("Function call time: %d ns (%.2f μs)", duration, durationUs / 1.0));

      assertTrue(
          durationUs < MAX_FUNCTION_CALL_US,
          String.format(
              "Function call should complete within %d μs (took %d μs)",
              MAX_FUNCTION_CALL_US, durationUs));
    }

    @Test
    @Timeout(120)
    @DisplayName("should measure repeated function call performance")
    void shouldMeasureRepeatedFunctionCallPerformance(final TestInfo testInfo) throws Exception {
      assumeNativeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Module module = engine.compileWat(ADD_WAT);
      resources.add(module);
      final Store store = Store.create(engine);
      resources.add(store);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final Optional<WasmFunction> addFuncOpt = instance.getFunction("add");
      assertTrue(addFuncOpt.isPresent());
      final WasmFunction addFunc = addFuncOpt.get();

      // Warmup
      for (int i = 0; i < WARMUP_ITERATIONS; i++) {
        addFunc.call(WasmValue.i32(i), WasmValue.i32(i));
      }

      // Measure multiple function calls
      final List<Long> measurements = new ArrayList<>();
      for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
        final long startTime = System.nanoTime();
        addFunc.call(WasmValue.i32(i), WasmValue.i32(i + 1));
        final long duration = System.nanoTime() - startTime;
        measurements.add(duration);
      }

      // Calculate statistics
      Collections.sort(measurements);
      final long minTime = measurements.get(0);
      final long medianTime = measurements.get(measurements.size() / 2);
      final double avgTime = measurements.stream().mapToLong(Long::longValue).average().orElse(0);
      final long p95Time = measurements.get((int) (measurements.size() * 0.95));
      final long p99Time = measurements.get((int) (measurements.size() * 0.99));
      final long maxTime = measurements.get(measurements.size() - 1);

      // Calculate throughput
      final long totalTimeNs = measurements.stream().mapToLong(Long::longValue).sum();
      final double throughput = MEASUREMENT_ITERATIONS / (totalTimeNs / 1_000_000_000.0);

      LOGGER.info(
          String.format(
              "Function call stats (%d iterations) - Min: %d ns, Median: %d ns, Avg: %.1f ns, P95:"
                  + " %d ns, P99: %d ns, Max: %d ns",
              MEASUREMENT_ITERATIONS, minTime, medianTime, avgTime, p95Time, p99Time, maxTime));
      LOGGER.info(String.format("Throughput: %.2f calls/second", throughput));

      assertTrue(
          TimeUnit.NANOSECONDS.toMicros(medianTime) < MAX_FUNCTION_CALL_US,
          "Median function call time should be within threshold");
    }
  }

  @Nested
  @DisplayName("Memory Operation Performance")
  class MemoryOperationPerformanceTests {

    @Test
    @Timeout(60)
    @DisplayName("should measure memory load/store performance")
    void shouldMeasureMemoryLoadStorePerformance(final TestInfo testInfo) throws Exception {
      assumeNativeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Module module = engine.compileWat(MEMORY_WAT);
      resources.add(module);
      final Store store = Store.create(engine);
      resources.add(store);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final Optional<WasmFunction> loadFuncOpt = instance.getFunction("load");
      final Optional<WasmFunction> storeFuncOpt = instance.getFunction("store");
      assertTrue(loadFuncOpt.isPresent(), "load function should exist");
      assertTrue(storeFuncOpt.isPresent(), "store function should exist");
      final WasmFunction loadFunc = loadFuncOpt.get();
      final WasmFunction storeFunc = storeFuncOpt.get();

      // Warmup
      for (int i = 0; i < WARMUP_ITERATIONS; i++) {
        final int offset = (i % 1000) * 4;
        storeFunc.call(WasmValue.i32(offset), WasmValue.i32(i));
        loadFunc.call(WasmValue.i32(offset));
      }

      // Measure store operations
      final List<Long> storeMeasurements = new ArrayList<>();
      for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
        final int offset = (i % 1000) * 4;
        final long startTime = System.nanoTime();
        storeFunc.call(WasmValue.i32(offset), WasmValue.i32(i));
        final long duration = System.nanoTime() - startTime;
        storeMeasurements.add(duration);
      }

      // Measure load operations
      final List<Long> loadMeasurements = new ArrayList<>();
      for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
        final int offset = (i % 1000) * 4;
        final long startTime = System.nanoTime();
        loadFunc.call(WasmValue.i32(offset));
        final long duration = System.nanoTime() - startTime;
        loadMeasurements.add(duration);
      }

      // Calculate store statistics
      Collections.sort(storeMeasurements);
      final long storeMedian = storeMeasurements.get(storeMeasurements.size() / 2);
      final double storeAvg =
          storeMeasurements.stream().mapToLong(Long::longValue).average().orElse(0);

      // Calculate load statistics
      Collections.sort(loadMeasurements);
      final long loadMedian = loadMeasurements.get(loadMeasurements.size() / 2);
      final double loadAvg =
          loadMeasurements.stream().mapToLong(Long::longValue).average().orElse(0);

      LOGGER.info(
          String.format("Memory store stats - Median: %d ns, Avg: %.1f ns", storeMedian, storeAvg));
      LOGGER.info(
          String.format("Memory load stats - Median: %d ns, Avg: %.1f ns", loadMedian, loadAvg));

      // Memory operations should be fast
      assertTrue(
          TimeUnit.NANOSECONDS.toMicros(storeMedian) < MAX_FUNCTION_CALL_US,
          "Memory store should be within function call threshold");
      assertTrue(
          TimeUnit.NANOSECONDS.toMicros(loadMedian) < MAX_FUNCTION_CALL_US,
          "Memory load should be within function call threshold");
    }
  }

  @Nested
  @DisplayName("Memory Allocation Tracking")
  class MemoryAllocationTrackingTests {

    @Test
    @Timeout(60)
    @DisplayName("should track memory allocation during operations")
    void shouldTrackMemoryAllocationDuringOperations(final TestInfo testInfo) throws Exception {
      assumeNativeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Force GC before measurement
      System.gc();
      Thread.yield();
      System.gc();

      final Runtime runtime = Runtime.getRuntime();
      final long initialMemory = runtime.totalMemory() - runtime.freeMemory();

      // Perform many operations
      final int iterations = 100;
      for (int i = 0; i < iterations; i++) {
        try (Engine engine = Engine.create();
            Store store = Store.create(engine);
            Module module = engine.compileWat(ADD_WAT);
            Instance instance = module.instantiate(store)) {

          final Optional<WasmFunction> addFuncOpt = instance.getFunction("add");
          if (addFuncOpt.isPresent()) {
            final WasmFunction addFunc = addFuncOpt.get();
            for (int j = 0; j < 10; j++) {
              addFunc.call(WasmValue.i32(j), WasmValue.i32(j + 1));
            }
          }
        }
      }

      // Force GC after operations
      System.gc();
      Thread.yield();
      System.gc();

      final long finalMemory = runtime.totalMemory() - runtime.freeMemory();
      final long memoryDelta = finalMemory - initialMemory;

      LOGGER.info(
          String.format(
              "Memory tracking - Initial: %d bytes, Final: %d bytes, Delta: %d bytes (%.2f MB)",
              initialMemory, finalMemory, memoryDelta, memoryDelta / (1024.0 * 1024.0)));

      assertTrue(
          memoryDelta < MAX_MEMORY_OVERHEAD_BYTES,
          String.format(
              "Memory overhead should be under %d MB (was %.2f MB)",
              MAX_MEMORY_OVERHEAD_BYTES / (1024 * 1024), memoryDelta / (1024.0 * 1024.0)));
    }
  }

  @Nested
  @DisplayName("Native Call Overhead")
  class NativeCallOverheadTests {

    @Test
    @Timeout(60)
    @DisplayName("should measure native call overhead")
    void shouldMeasureNativeCallOverhead(final TestInfo testInfo) throws Exception {
      assumeNativeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Module module = engine.compileWat(ADD_WAT);
      resources.add(module);
      final Store store = Store.create(engine);
      resources.add(store);
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final Optional<WasmFunction> addFuncOpt = instance.getFunction("add");
      assertTrue(addFuncOpt.isPresent());
      final WasmFunction addFunc = addFuncOpt.get();

      // Measure pure Java baseline (simple addition)
      final List<Long> javaTimings = new ArrayList<>();
      for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
        final long startTime = System.nanoTime();
        @SuppressWarnings("unused")
        final int result = i + (i + 1); // Simple Java addition
        final long duration = System.nanoTime() - startTime;
        javaTimings.add(duration);
      }

      // Warmup WASM calls
      for (int i = 0; i < WARMUP_ITERATIONS; i++) {
        addFunc.call(WasmValue.i32(i), WasmValue.i32(i + 1));
      }

      // Measure WASM function calls
      final List<Long> wasmTimings = new ArrayList<>();
      for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
        final long startTime = System.nanoTime();
        addFunc.call(WasmValue.i32(i), WasmValue.i32(i + 1));
        final long duration = System.nanoTime() - startTime;
        wasmTimings.add(duration);
      }

      // Calculate statistics
      Collections.sort(javaTimings);
      Collections.sort(wasmTimings);

      final long javaMedian = javaTimings.get(javaTimings.size() / 2);
      final long wasmMedian = wasmTimings.get(wasmTimings.size() / 2);
      final double overhead = wasmMedian > 0 ? (double) wasmMedian / Math.max(javaMedian, 1) : 0;

      LOGGER.info(String.format("Java baseline median: %d ns", javaMedian));
      LOGGER.info(String.format("WASM call median: %d ns", wasmMedian));
      LOGGER.info(String.format("Native call overhead ratio: %.2fx", overhead));

      // WASM calls will have overhead, but should still be reasonable
      assertTrue(
          TimeUnit.NANOSECONDS.toMicros(wasmMedian) < MAX_FUNCTION_CALL_US,
          "WASM calls should still be within acceptable threshold");
    }
  }

  @Nested
  @DisplayName("Concurrent Performance")
  class ConcurrentPerformanceTests {

    @Test
    @Timeout(120)
    @DisplayName("should measure concurrent engine creation performance")
    void shouldMeasureConcurrentEngineCreationPerformance(final TestInfo testInfo)
        throws Exception {
      assumeNativeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final int threadCount = Math.min(Runtime.getRuntime().availableProcessors(), 8);
      final Thread[] threads = new Thread[threadCount];
      final long[] threadTimes = new long[threadCount];
      final boolean[] threadSuccess = new boolean[threadCount];

      final long overallStartTime = System.nanoTime();

      // Start all threads
      for (int i = 0; i < threadCount; i++) {
        final int threadIndex = i;
        threads[i] =
            new Thread(
                () -> {
                  try {
                    final long threadStartTime = System.nanoTime();
                    for (int j = 0; j < 10; j++) {
                      try (Engine engine = Engine.create();
                          Module module = engine.compileWat(ADD_WAT)) {
                        assertNotNull(engine);
                        assertNotNull(module);
                      }
                    }
                    threadTimes[threadIndex] = System.nanoTime() - threadStartTime;
                    threadSuccess[threadIndex] = true;
                  } catch (final Exception e) {
                    LOGGER.warning("Thread " + threadIndex + " failed: " + e.getMessage());
                    threadSuccess[threadIndex] = false;
                  }
                });
        threads[i].start();
      }

      // Wait for all threads
      for (final Thread thread : threads) {
        thread.join();
      }

      final long overallTime = System.nanoTime() - overallStartTime;

      // Verify all threads succeeded
      int successCount = 0;
      for (int i = 0; i < threadCount; i++) {
        if (threadSuccess[i]) {
          successCount++;
        }
      }

      // Calculate statistics
      long minTime = Long.MAX_VALUE;
      long maxTime = 0;
      long totalTime = 0;
      for (int i = 0; i < threadCount; i++) {
        if (threadSuccess[i]) {
          minTime = Math.min(minTime, threadTimes[i]);
          maxTime = Math.max(maxTime, threadTimes[i]);
          totalTime += threadTimes[i];
        }
      }

      final double avgTime = successCount > 0 ? (double) totalTime / successCount : 0;

      LOGGER.info(
          String.format(
              "Concurrent performance (%d threads, %d successful) - Overall: %.2f ms, Thread"
                  + " times - Min: %.2f ms, Avg: %.2f ms, Max: %.2f ms",
              threadCount,
              successCount,
              overallTime / 1_000_000.0,
              minTime / 1_000_000.0,
              avgTime / 1_000_000.0,
              maxTime / 1_000_000.0));

      assertTrue(successCount == threadCount, "All threads should complete successfully");
    }
  }

  @Nested
  @DisplayName("Performance Summary")
  class PerformanceSummaryTests {

    @Test
    @Timeout(30)
    @DisplayName("should generate performance summary")
    void shouldGeneratePerformanceSummary(final TestInfo testInfo) throws Exception {
      assumeNativeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      System.out.println("=== Performance Baseline Summary ===");
      System.out.printf("Java version: %s%n", System.getProperty("java.version"));
      System.out.printf("JVM name: %s%n", System.getProperty("java.vm.name"));
      System.out.printf(
          "OS: %s %s%n", System.getProperty("os.name"), System.getProperty("os.arch"));
      System.out.printf("Available processors: %d%n", Runtime.getRuntime().availableProcessors());
      System.out.printf(
          "Max memory: %.2f MB%n", Runtime.getRuntime().maxMemory() / (1024.0 * 1024));
      System.out.println("====================================");

      // Quick performance check
      final long engineStart = System.nanoTime();
      final Engine engine = Engine.create();
      final long engineTime = System.nanoTime() - engineStart;
      resources.add(engine);

      final long moduleStart = System.nanoTime();
      final Module module = engine.compileWat(ADD_WAT);
      final long moduleTime = System.nanoTime() - moduleStart;
      resources.add(module);

      final Store store = Store.create(engine);
      resources.add(store);

      final long instanceStart = System.nanoTime();
      final Instance instance = module.instantiate(store);
      final long instanceTime = System.nanoTime() - instanceStart;
      resources.add(instance);

      final Optional<WasmFunction> addFuncOpt = instance.getFunction("add");
      assertTrue(addFuncOpt.isPresent());
      final WasmFunction addFunc = addFuncOpt.get();

      final long callStart = System.nanoTime();
      addFunc.call(WasmValue.i32(5), WasmValue.i32(7));
      final long callTime = System.nanoTime() - callStart;

      System.out.printf("Engine creation: %.2f ms%n", engineTime / 1_000_000.0);
      System.out.printf("Module compilation: %.2f ms%n", moduleTime / 1_000_000.0);
      System.out.printf("Instance creation: %.2f ms%n", instanceTime / 1_000_000.0);
      System.out.printf("Function call: %.2f μs%n", callTime / 1_000.0);
      System.out.println("====================================");

      assertTrue(engineTime > 0, "Engine should be created");
      assertTrue(moduleTime > 0, "Module should be compiled");
      assertTrue(instanceTime > 0, "Instance should be created");
      assertTrue(callTime > 0, "Function should be called");
    }
  }
}

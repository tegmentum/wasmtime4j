package ai.tegmentum.wasmtime4j.function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Performance validation tests for WebAssembly function execution that integrate with the existing
 * benchmark framework.
 *
 * <p>This test class validates:
 *
 * <ul>
 *   <li>Function call performance characteristics
 *   <li>Parameter marshaling overhead
 *   <li>Concurrent function execution performance
 *   <li>Memory allocation and GC impact during function calls
 *   <li>Cross-runtime performance consistency
 *   <li>Resource utilization during intensive operations
 * </ul>
 */
@DisplayName("Function Performance Validation Tests")
public final class FunctionPerformanceValidationIT extends BaseIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(FunctionPerformanceValidationIT.class.getName());

  // Performance thresholds (in milliseconds)
  private static final long SINGLE_CALL_MAX_DURATION_MS = 10;
  private static final long BATCH_CALLS_MAX_DURATION_MS = 1000;
  private static final long RECURSIVE_CALL_MAX_DURATION_MS = 100;

  /**
   * Tests basic function call performance and validates execution time stays within acceptable
   * bounds.
   */
  @Test
  @DisplayName("Basic function call performance")
  void testBasicFunctionCallPerformance() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing basic function call performance with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module = engine.compileModule(WasmTestModules.getModule("basic_add"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> addFunction = instance.getFunction("add");
            assertTrue(addFunction.isPresent(), "Add function should be exported");

            final WasmFunction add = addFunction.get();

            // Warm up the function (JIT compilation, etc.)
            for (int i = 0; i < 100; i++) {
              add.call(WasmValue.i32(i), WasmValue.i32(i + 1));
            }

            // Test single function call performance
            assertExecutionTime(
                Duration.ofMillis(SINGLE_CALL_MAX_DURATION_MS),
                () -> {
                  try {
                    final WasmValue[] result = add.call(WasmValue.i32(42), WasmValue.i32(58));
                    assertEquals(100, result[0].asI32());
                  } catch (final WasmException e) {
                    throw new RuntimeException("Single function call failed", e);
                  }
                },
                "Single function call with " + runtimeType);

            LOGGER.info("Basic function call performance test completed for " + runtimeType);
          }
        });
  }

  /** Tests batch function call performance to measure call overhead and throughput. */
  @Test
  @DisplayName("Batch function call performance")
  void testBatchFunctionCallPerformance() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing batch function call performance with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module = engine.compileModule(WasmTestModules.getModule("basic_add"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> addFunction = instance.getFunction("add");
            assertTrue(addFunction.isPresent(), "Add function should be exported");

            final WasmFunction add = addFunction.get();

            // Warm up
            for (int i = 0; i < 100; i++) {
              add.call(WasmValue.i32(i), WasmValue.i32(i + 1));
            }

            final int batchSize = 10000;

            // Test batch function call performance
            final Duration batchDuration =
                timeOperation(
                    "Batch " + batchSize + " function calls with " + runtimeType,
                    () -> {
                      try {
                        for (int i = 0; i < batchSize; i++) {
                          final WasmValue[] result =
                              add.call(WasmValue.i32(i), WasmValue.i32(i + 1));
                          assertEquals(i + i + 1, result[0].asI32());
                        }
                      } catch (final WasmException e) {
                        throw new RuntimeException("Batch function calls failed", e);
                      }
                    });

            // Calculate throughput
            final double callsPerSecond = (double) batchSize / (batchDuration.toMillis() / 1000.0);
            LOGGER.info(
                String.format(
                    "Batch performance for %s: %.2f calls/second", runtimeType, callsPerSecond));

            // Ensure batch execution is reasonably fast
            assertTrue(
                batchDuration.toMillis() < BATCH_CALLS_MAX_DURATION_MS,
                String.format(
                    "Batch calls should complete within %dms, took %dms",
                    BATCH_CALLS_MAX_DURATION_MS, batchDuration.toMillis()));

            LOGGER.info("Batch function call performance test completed for " + runtimeType);
          }
        });
  }

  /** Tests parameter marshaling performance for different value types. */
  @Test
  @DisplayName("Parameter marshaling performance")
  void testParameterMarshalingPerformance() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing parameter marshaling performance with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine)) {

            registerForCleanup(engine);
            registerForCleanup(store);

            final int iterations = 10000;

            // Test I32 marshaling performance
            measureExecutionTime(
                "I32 marshaling (" + iterations + " iterations) with " + runtimeType,
                () -> {
                  for (int i = 0; i < iterations; i++) {
                    final WasmValue i32Value = WasmValue.i32(i);
                    assertEquals(i, i32Value.asI32());
                  }
                });

            // Test I64 marshaling performance
            measureExecutionTime(
                "I64 marshaling (" + iterations + " iterations) with " + runtimeType,
                () -> {
                  for (int i = 0; i < iterations; i++) {
                    final WasmValue i64Value = WasmValue.i64(i * 1000L);
                    assertEquals(i * 1000L, i64Value.asI64());
                  }
                });

            // Test F32 marshaling performance
            measureExecutionTime(
                "F32 marshaling (" + iterations + " iterations) with " + runtimeType,
                () -> {
                  for (int i = 0; i < iterations; i++) {
                    final WasmValue f32Value = WasmValue.f32(i * 3.14f);
                    assertEquals(i * 3.14f, f32Value.asF32(), 0.001f);
                  }
                });

            // Test F64 marshaling performance
            measureExecutionTime(
                "F64 marshaling (" + iterations + " iterations) with " + runtimeType,
                () -> {
                  for (int i = 0; i < iterations; i++) {
                    final WasmValue f64Value = WasmValue.f64(i * 2.718);
                    assertEquals(i * 2.718, f64Value.asF64(), 0.001);
                  }
                });

            // Test V128 marshaling performance
            measureExecutionTime(
                "V128 marshaling (" + iterations + " iterations) with " + runtimeType,
                () -> {
                  final byte[] testData = new byte[16];
                  for (int i = 0; i < iterations; i++) {
                    // Create pattern based on iteration
                    for (int j = 0; j < 16; j++) {
                      testData[j] = (byte) ((i + j) % 256);
                    }
                    final WasmValue v128Value = WasmValue.v128(testData);
                    assertArrayEquals(testData, v128Value.asV128());
                  }
                });

            LOGGER.info("Parameter marshaling performance test completed for " + runtimeType);
          }
        });
  }

  /** Tests recursive function call performance. */
  @Test
  @DisplayName("Recursive function call performance")
  void testRecursiveFunctionCallPerformance() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info(
              "Testing recursive function call performance with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module =
                  engine.compileModule(WasmTestModules.getModule("function_fibonacci"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> fibFunction = instance.getFunction("fib");
            assertTrue(fibFunction.isPresent(), "Fibonacci function should be exported");

            final WasmFunction fib = fibFunction.get();

            // Warm up
            for (int i = 0; i < 10; i++) {
              fib.call(WasmValue.i32(10));
            }

            // Test various fibonacci numbers and measure performance
            final int[] fibNumbers = {15, 20, 25};
            final int[] expectedResults = {610, 6765, 75025};

            for (int i = 0; i < fibNumbers.length; i++) {
              final int fibN = fibNumbers[i];
              final int expected = expectedResults[i];

              assertExecutionTime(
                  Duration.ofMillis(RECURSIVE_CALL_MAX_DURATION_MS),
                  () -> {
                    try {
                      final WasmValue[] result = fib.call(WasmValue.i32(fibN));
                      assertEquals(expected, result[0].asI32());
                    } catch (final WasmException e) {
                      throw new RuntimeException("Fibonacci call failed", e);
                    }
                  },
                  "Fibonacci(" + fibN + ") with " + runtimeType);
            }

            LOGGER.info("Recursive function call performance test completed for " + runtimeType);
          }
        });
  }

  /** Tests concurrent function execution performance and thread safety. */
  @Test
  @DisplayName("Concurrent function execution performance")
  void testConcurrentFunctionExecutionPerformance() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info(
              "Testing concurrent function execution performance with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine()) {

            registerForCleanup(engine);

            final int threadCount = 4;
            final int callsPerThread = 1000;
            final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            try {
              final CountDownLatch startLatch = new CountDownLatch(1);
              final List<Future<Duration>> futures = new ArrayList<>();

              // Create concurrent tasks
              for (int threadId = 0; threadId < threadCount; threadId++) {
                final int finalThreadId = threadId;
                final Future<Duration> future =
                    executor.submit(
                        () -> {
                          try {
                            // Wait for all threads to be ready
                            startLatch.await();

                            // Each thread gets its own store and instance
                            try (final Store store = runtime.createStore(engine);
                                final Module module =
                                    engine.compileModule(WasmTestModules.getModule("basic_add"))) {

                              final Instance instance = store.createInstance(module);
                              final Optional<WasmFunction> addFunction =
                                  instance.getFunction("add");
                              assertTrue(
                                  addFunction.isPresent(), "Add function should be exported");

                              final WasmFunction add = addFunction.get();

                              final long startTime = System.nanoTime();

                              // Execute function calls
                              for (int i = 0; i < callsPerThread; i++) {
                                final WasmValue[] result =
                                    add.call(
                                        WasmValue.i32(finalThreadId * 1000 + i),
                                        WasmValue.i32(i + 1));
                                assertEquals(finalThreadId * 1000 + i + i + 1, result[0].asI32());
                              }

                              final long endTime = System.nanoTime();
                              instance.close();
                              return Duration.ofNanos(endTime - startTime);
                            }
                          } catch (final Exception e) {
                            throw new RuntimeException("Concurrent execution failed", e);
                          }
                        });

                futures.add(future);
              }

              // Start all threads simultaneously
              measureExecutionTime(
                  "Concurrent execution with "
                      + threadCount
                      + " threads, "
                      + callsPerThread
                      + " calls each, "
                      + runtimeType,
                  () -> {
                    try {
                      startLatch.countDown();

                      // Wait for all threads to complete
                      final List<Duration> durations = new ArrayList<>();
                      for (final Future<Duration> future : futures) {
                        durations.add(future.get(30, TimeUnit.SECONDS));
                      }

                      // Log individual thread performance
                      for (int i = 0; i < durations.size(); i++) {
                        LOGGER.info(
                            String.format(
                                "Thread %d completed %d calls in %dms (%.2f calls/sec)",
                                i,
                                callsPerThread,
                                durations.get(i).toMillis(),
                                (double) callsPerThread / (durations.get(i).toMillis() / 1000.0)));
                      }
                    } catch (final Exception e) {
                      throw new RuntimeException("Concurrent execution failed", e);
                    }
                  });

            } finally {
              executor.shutdown();
              if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
              }
            }

            LOGGER.info(
                "Concurrent function execution performance test completed for " + runtimeType);
          }
        });
  }

  /** Tests memory pressure impact on function execution performance. */
  @Test
  @DisplayName("Memory pressure impact on function performance")
  void testMemoryPressureImpactOnFunctionPerformance() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info(
              "Testing memory pressure impact on function performance with "
                  + runtimeType
                  + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine);
              final Module module = engine.compileModule(WasmTestModules.getModule("basic_add"))) {

            registerForCleanup(engine);
            registerForCleanup(store);
            registerForCleanup(module);

            final Instance instance = store.createInstance(module);
            registerForCleanup(instance);

            final Optional<WasmFunction> addFunction = instance.getFunction("add");
            assertTrue(addFunction.isPresent(), "Add function should be exported");

            final WasmFunction add = addFunction.get();

            final int iterations = 1000;

            // Baseline performance without memory pressure
            final Duration baselineDuration =
                timeOperation(
                    "Baseline function calls (" + iterations + " iterations) with " + runtimeType,
                    () -> {
                      try {
                        for (int i = 0; i < iterations; i++) {
                          final WasmValue[] result =
                              add.call(WasmValue.i32(i), WasmValue.i32(i + 1));
                          assertEquals(i + i + 1, result[0].asI32());
                        }
                      } catch (final WasmException e) {
                        throw new RuntimeException("Baseline function calls failed", e);
                      }
                    });

            // Performance under memory pressure
            final Duration memoryPressureDuration =
                timeOperation(
                    "Function calls under memory pressure ("
                        + iterations
                        + " iterations) with "
                        + runtimeType,
                    () -> {
                      // Create memory pressure
                      final List<byte[]> memoryPressure = new ArrayList<>();
                      try {
                        // Allocate memory chunks
                        for (int i = 0; i < 1000; i++) {
                          memoryPressure.add(new byte[1024]); // 1KB each
                        }

                        // Execute function calls under memory pressure
                        for (int i = 0; i < iterations; i++) {
                          final WasmValue[] result =
                              add.call(WasmValue.i32(i), WasmValue.i32(i + 1));
                          assertEquals(i + i + 1, result[0].asI32());

                          // Occasionally force GC by creating more objects
                          if (i % 100 == 0) {
                            memoryPressure.add(new byte[2048]);
                          }
                        }
                      } catch (final WasmException e) {
                        throw new RuntimeException("Memory pressure function calls failed", e);
                      } finally {
                        // Clear memory pressure
                        memoryPressure.clear();
                        System.gc(); // Suggest garbage collection
                      }
                    });

            // Analyze performance impact
            final double impactRatio =
                (double) memoryPressureDuration.toMillis() / baselineDuration.toMillis();
            LOGGER.info(
                String.format(
                    "Memory pressure impact for %s: %.2fx slower (baseline: %dms, under pressure:"
                        + " %dms)",
                    runtimeType,
                    impactRatio,
                    baselineDuration.toMillis(),
                    memoryPressureDuration.toMillis()));

            // Memory pressure shouldn't cause more than 5x slowdown
            assertTrue(
                impactRatio < 5.0,
                "Memory pressure should not cause more than 5x slowdown, was " + impactRatio + "x");

            LOGGER.info("Memory pressure impact test completed for " + runtimeType);
          }
        });
  }

  /** Tests cross-runtime performance consistency between JNI and Panama. */
  @Test
  @DisplayName("Cross-runtime performance consistency")
  void testCrossRuntimePerformanceConsistency() {
    skipIfPanamaNotAvailable();

    LOGGER.info("Testing cross-runtime performance consistency");

    final int iterations = 10000;
    Duration jniDuration = null;
    Duration panamaDuration = null;

    // Measure JNI performance
    try (final WasmRuntime jniRuntime = createTestRuntime(RuntimeType.JNI);
        final Engine jniEngine = jniRuntime.createEngine();
        final Store jniStore = jniRuntime.createStore(jniEngine);
        final Module jniModule = jniEngine.compileModule(WasmTestModules.getModule("basic_add"))) {

      final Instance jniInstance = jniStore.createInstance(jniModule);
      final Optional<WasmFunction> jniAddFunction = jniInstance.getFunction("add");
      assertTrue(jniAddFunction.isPresent(), "JNI add function should be exported");

      final WasmFunction jniAdd = jniAddFunction.get();

      // Warm up JNI
      for (int i = 0; i < 100; i++) {
        jniAdd.call(WasmValue.i32(i), WasmValue.i32(i + 1));
      }

      jniDuration =
          timeOperation(
              "JNI function calls (" + iterations + " iterations)",
              () -> {
                try {
                  for (int i = 0; i < iterations; i++) {
                    final WasmValue[] result = jniAdd.call(WasmValue.i32(i), WasmValue.i32(i + 1));
                    assertEquals(i + i + 1, result[0].asI32());
                  }
                } catch (final WasmException e) {
                  throw new RuntimeException("JNI function calls failed", e);
                }
              });

      jniInstance.close();
    }

    // Measure Panama performance
    try (final WasmRuntime panamaRuntime = createTestRuntime(RuntimeType.PANAMA);
        final Engine panamaEngine = panamaRuntime.createEngine();
        final Store panamaStore = panamaRuntime.createStore(panamaEngine);
        final Module panamaModule =
            panamaEngine.compileModule(WasmTestModules.getModule("basic_add"))) {

      final Instance panamaInstance = panamaStore.createInstance(panamaModule);
      final Optional<WasmFunction> panamaAddFunction = panamaInstance.getFunction("add");
      assertTrue(panamaAddFunction.isPresent(), "Panama add function should be exported");

      final WasmFunction panamaAdd = panamaAddFunction.get();

      // Warm up Panama
      for (int i = 0; i < 100; i++) {
        panamaAdd.call(WasmValue.i32(i), WasmValue.i32(i + 1));
      }

      panamaDuration =
          timeOperation(
              "Panama function calls (" + iterations + " iterations)",
              () -> {
                try {
                  for (int i = 0; i < iterations; i++) {
                    final WasmValue[] result =
                        panamaAdd.call(WasmValue.i32(i), WasmValue.i32(i + 1));
                    assertEquals(i + i + 1, result[0].asI32());
                  }
                } catch (final WasmException e) {
                  throw new RuntimeException("Panama function calls failed", e);
                }
              });

      panamaInstance.close();
    }

    // Analyze performance comparison
    assertNotNull(jniDuration, "JNI duration should be measured");
    assertNotNull(panamaDuration, "Panama duration should be measured");

    final double jniCallsPerSecond = (double) iterations / (jniDuration.toMillis() / 1000.0);
    final double panamaCallsPerSecond = (double) iterations / (panamaDuration.toMillis() / 1000.0);

    LOGGER.info(
        String.format(
            "JNI performance: %.2f calls/second (%dms total)",
            jniCallsPerSecond, jniDuration.toMillis()));
    LOGGER.info(
        String.format(
            "Panama performance: %.2f calls/second (%dms total)",
            panamaCallsPerSecond, panamaDuration.toMillis()));

    final double performanceRatio =
        Math.max(jniCallsPerSecond, panamaCallsPerSecond)
            / Math.min(jniCallsPerSecond, panamaCallsPerSecond);

    LOGGER.info(
        String.format(
            "Performance ratio: %.2f (closer to 1.0 means more consistent)", performanceRatio));

    // Performance should be reasonably consistent (within 10x of each other)
    assertTrue(
        performanceRatio < 10.0,
        "Cross-runtime performance should be within 10x, actual ratio: " + performanceRatio);

    LOGGER.info("Cross-runtime performance consistency test completed");
  }

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    LOGGER.info("Setting up function performance validation test: " + testInfo.getDisplayName());
  }
}

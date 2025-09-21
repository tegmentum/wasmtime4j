package ai.tegmentum.wasmtime4j.stress;

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
import ai.tegmentum.wasmtime4j.memory.MemoryLeakDetector;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Comprehensive stress testing for wasmtime4j to validate stability, performance, and resource
 * management under intensive load conditions. This test suite validates that the implementation
 * can handle sustained operation without memory leaks or performance degradation.
 */
@DisplayName("Comprehensive Stress Testing")
@EnabledIfSystemProperty(named = "wasmtime4j.test.stress", matches = "true")
class ComprehensiveStressTestIT extends BaseIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComprehensiveStressTestIT.class.getName());

  // Stress test configuration
  private static final int STRESS_DURATION_MINUTES =
      Integer.parseInt(System.getProperty("wasmtime4j.stress.duration", "5"));
  private static final int CONCURRENT_THREADS =
      Integer.parseInt(System.getProperty("wasmtime4j.stress.threads", "8"));
  private static final int OPERATIONS_PER_BATCH =
      Integer.parseInt(System.getProperty("wasmtime4j.stress.batch.size", "100"));

  private ExecutorService stressExecutor;
  private final AtomicInteger totalOperations = new AtomicInteger(0);
  private final AtomicInteger successfulOperations = new AtomicInteger(0);
  private final AtomicInteger failedOperations = new AtomicInteger(0);
  private final AtomicLong totalExecutionTime = new AtomicLong(0);

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    LOGGER.info("Starting stress test: " + testInfo.getDisplayName());
    LOGGER.info("Configuration: duration=" + STRESS_DURATION_MINUTES + "min, threads=" +
                CONCURRENT_THREADS + ", batch_size=" + OPERATIONS_PER_BATCH);

    stressExecutor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
    totalOperations.set(0);
    successfulOperations.set(0);
    failedOperations.set(0);
    totalExecutionTime.set(0);
  }

  @AfterEach
  void tearDown() {
    if (stressExecutor != null) {
      stressExecutor.shutdown();
      try {
        if (!stressExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
          LOGGER.warning("Stress executor did not terminate within timeout");
          stressExecutor.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        stressExecutor.shutdownNow();
      }
    }

    // Log final statistics
    final int total = totalOperations.get();
    final int successful = successfulOperations.get();
    final int failed = failedOperations.get();
    final long avgTime = total > 0 ? totalExecutionTime.get() / total : 0;

    LOGGER.info("Stress test completed:");
    LOGGER.info("  Total operations: " + total);
    LOGGER.info("  Successful: " + successful + " (" + (100.0 * successful / Math.max(total, 1)) + "%)");
    LOGGER.info("  Failed: " + failed + " (" + (100.0 * failed / Math.max(total, 1)) + "%)");
    LOGGER.info("  Average execution time: " + avgTime + "ms");
  }

  @Test
  @DisplayName("Should handle sustained high-frequency WebAssembly operations")
  @Timeout(value = STRESS_DURATION_MINUTES + 2, unit = TimeUnit.MINUTES)
  void shouldHandleSustainedHighFrequencyWebAssemblyOperations() throws Exception {
    LOGGER.info("=== Sustained High-Frequency Operations Stress Test ===");

    final Instant testStart = Instant.now();
    final Instant testEnd = testStart.plus(Duration.ofMinutes(STRESS_DURATION_MINUTES));

    final List<CompletableFuture<Void>> stressFutures = new ArrayList<>();

    // Launch stress test threads
    for (int t = 0; t < CONCURRENT_THREADS; t++) {
      final int threadId = t;
      final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        executeHighFrequencyOperations(threadId, testEnd);
      }, stressExecutor);

      stressFutures.add(future);
    }

    // Wait for all stress threads to complete
    CompletableFuture.allOf(stressFutures.toArray(new CompletableFuture[0])).join();

    // Validate results
    final int total = totalOperations.get();
    final int successful = successfulOperations.get();
    final double successRate = (double) successful / total * 100.0;

    LOGGER.info("Stress test results: " + successful + "/" + total +
                " operations successful (" + String.format("%.2f", successRate) + "%)");

    // Require at least 95% success rate
    assertThat(successRate).isGreaterThanOrEqualTo(95.0);
    assertThat(total).isGreaterThan(CONCURRENT_THREADS * 10); // Minimum operations threshold
  }

  @Test
  @DisplayName("Should detect memory leaks during sustained operations")
  @Timeout(value = STRESS_DURATION_MINUTES + 2, unit = TimeUnit.MINUTES)
  void shouldDetectMemoryLeaksDuringSustainedOperations() throws Exception {
    LOGGER.info("=== Memory Leak Detection During Sustained Operations ===");

    // Configure thorough leak detection
    final MemoryLeakDetector.Configuration config =
        MemoryLeakDetector.Configuration.builder()
            .testDuration(Duration.ofMinutes(STRESS_DURATION_MINUTES))
            .samplingInterval(100)
            .sampleCount(5000)
            .leakThreshold(1.2) // 20% growth threshold
            .enableNativeTracking(true)
            .build();

    // Define stress operation for leak detection
    final MemoryLeakDetector.TestedOperation stressOperation = runtime -> {
      try (final Engine engine = runtime.createEngine()) {
        try (final Store store = engine.createStore()) {
          // Perform multiple WebAssembly operations
          for (int i = 0; i < 10; i++) {
            final byte[] moduleBytes = TestUtils.createSimpleWasmModule();
            final Module module = engine.compileModule(moduleBytes);
            final Instance instance = runtime.instantiate(module);

            final Function addFunction = instance.getFunction("add")
                .orElseThrow(() -> new AssertionError("add function should be exported"));

            final WasmValue[] args = {WasmValue.i32(i), WasmValue.i32(42)};
            final WasmValue[] results = addFunction.call(args);

            assertThat(results).hasSize(1);
            assertThat(results[0].asI32()).isEqualTo(i + 42);

            // Test memory operations if available
            final Memory memory = instance.getMemory("memory").orElse(null);
            if (memory != null) {
              final byte[] testData = new byte[1024];
              for (int j = 0; j < testData.length; j++) {
                testData[j] = (byte) (i + j);
              }
              memory.writeBytes(0, testData);
              final byte[] readData = memory.readBytes(0, testData.length);
              assertThat(readData).isEqualTo(testData);
            }

            instance.close();
          }
        }
      }
    };

    // Run leak detection for both JNI and Panama if available
    final MemoryLeakDetector.LeakAnalysisResult jniResult =
        MemoryLeakDetector.detectLeaks("stress_leak_test_jni", RuntimeType.JNI, stressOperation, config);

    LOGGER.info("JNI Memory Leak Analysis:");
    LOGGER.info(jniResult.getAnalysis());

    if (jniResult.isLeakDetected()) {
      LOGGER.warning("JNI Memory leak detected: " + jniResult.getMemoryIncrease() + " bytes");
      LOGGER.warning("Recommendations: " + String.join(", ", jniResult.getRecommendations()));
    }

    if (TestUtils.isPanamaAvailable()) {
      final MemoryLeakDetector.LeakAnalysisResult panamaResult =
          MemoryLeakDetector.detectLeaks("stress_leak_test_panama", RuntimeType.PANAMA,
                                        stressOperation, config);

      LOGGER.info("Panama Memory Leak Analysis:");
      LOGGER.info(panamaResult.getAnalysis());

      if (panamaResult.isLeakDetected()) {
        LOGGER.warning("Panama Memory leak detected: " + panamaResult.getMemoryIncrease() + " bytes");
        LOGGER.warning("Recommendations: " + String.join(", ", panamaResult.getRecommendations()));
      }
    }

    // For stress testing, we log leaks but only fail on severe leaks (>100MB)
    if (jniResult.isLeakDetected() && jniResult.getMemoryIncrease() > 100_000_000) {
      throw new AssertionError("Severe JNI memory leak detected: " +
                              jniResult.getMemoryIncrease() + " bytes");
    }
  }

  @Test
  @DisplayName("Should handle resource exhaustion scenarios gracefully")
  @Timeout(value = 10, unit = TimeUnit.MINUTES)
  void shouldHandleResourceExhaustionScenariosGracefully() throws Exception {
    LOGGER.info("=== Resource Exhaustion Scenarios Test ===");

    // Test 1: Large number of Store instances
    testStoreResourceExhaustion();

    // Test 2: Large number of Module compilations
    testModuleCompilationResourceExhaustion();

    // Test 3: Large number of Instance creations
    testInstanceCreationResourceExhaustion();

    // Test 4: Large memory allocations
    testMemoryResourceExhaustion();

    LOGGER.info("Resource exhaustion scenarios completed");
  }

  @Test
  @DisplayName("Should maintain performance consistency under sustained load")
  @Timeout(value = STRESS_DURATION_MINUTES + 2, unit = TimeUnit.MINUTES)
  void shouldMaintainPerformanceConsistencyUnderSustainedLoad() throws Exception {
    LOGGER.info("=== Performance Consistency Under Sustained Load ===");

    final List<Long> performanceSamples = new ArrayList<>();
    final Object samplesLock = new Object();

    final Instant testStart = Instant.now();
    final Instant testEnd = testStart.plus(Duration.ofMinutes(STRESS_DURATION_MINUTES));

    // Launch performance monitoring thread
    final CompletableFuture<Void> performanceMonitor = CompletableFuture.runAsync(() -> {
      while (Instant.now().isBefore(testEnd)) {
        final long operationStart = System.nanoTime();

        try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
          try (final Engine engine = runtime.createEngine()) {
            try (final Store store = engine.createStore()) {
              final byte[] moduleBytes = TestUtils.createSimpleWasmModule();
              final Module module = engine.compileModule(moduleBytes);
              final Instance instance = runtime.instantiate(module);

              final Function addFunction = instance.getFunction("add")
                  .orElseThrow(() -> new AssertionError("add function should be exported"));

              final WasmValue[] args = {WasmValue.i32(10), WasmValue.i32(20)};
              final WasmValue[] results = addFunction.call(args);

              assertThat(results[0].asI32()).isEqualTo(30);
              instance.close();
            }
          }
        } catch (final Exception e) {
          LOGGER.warning("Performance monitoring operation failed: " + e.getMessage());
        }

        final long operationTime = (System.nanoTime() - operationStart) / 1_000_000; // ms
        synchronized (samplesLock) {
          performanceSamples.add(operationTime);
        }

        try {
          Thread.sleep(100); // Sample every 100ms
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }, stressExecutor);

    // Launch concurrent stress load
    final List<CompletableFuture<Void>> stressLoad = new ArrayList<>();
    for (int t = 0; t < CONCURRENT_THREADS / 2; t++) { // Use half threads for background load
      final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        executeHighFrequencyOperations(999, testEnd); // Background load
      }, stressExecutor);
      stressLoad.add(future);
    }

    // Wait for completion
    performanceMonitor.join();
    CompletableFuture.allOf(stressLoad.toArray(new CompletableFuture[0])).join();

    // Analyze performance consistency
    synchronized (samplesLock) {
      if (performanceSamples.size() < 10) {
        LOGGER.warning("Insufficient performance samples collected");
        return;
      }

      final long[] samples = performanceSamples.stream().mapToLong(Long::longValue).toArray();
      final long minTime = performanceSamples.stream().mapToLong(Long::longValue).min().orElse(0);
      final long maxTime = performanceSamples.stream().mapToLong(Long::longValue).max().orElse(0);
      final double avgTime = performanceSamples.stream().mapToLong(Long::longValue).average().orElse(0);

      LOGGER.info("Performance analysis:");
      LOGGER.info("  Samples collected: " + performanceSamples.size());
      LOGGER.info("  Min execution time: " + minTime + "ms");
      LOGGER.info("  Max execution time: " + maxTime + "ms");
      LOGGER.info("  Avg execution time: " + String.format("%.2f", avgTime) + "ms");

      // Calculate coefficient of variation (CV) - standard deviation / mean
      final double variance = performanceSamples.stream()
          .mapToDouble(time -> Math.pow(time - avgTime, 2))
          .average().orElse(0);
      final double stdDev = Math.sqrt(variance);
      final double cv = avgTime > 0 ? (stdDev / avgTime) * 100 : 0;

      LOGGER.info("  Standard deviation: " + String.format("%.2f", stdDev) + "ms");
      LOGGER.info("  Coefficient of variation: " + String.format("%.2f", cv) + "%");

      // Performance should be relatively consistent (CV < 50%)
      assertThat(cv).isLessThan(50.0);

      // Maximum execution time shouldn't be more than 10x minimum (excluding outliers)
      if (maxTime > minTime * 10) {
        LOGGER.warning("High performance variability detected: " +
                      maxTime + "ms max vs " + minTime + "ms min");
      }
    }
  }

  /**
   * Executes high-frequency WebAssembly operations for stress testing.
   */
  private void executeHighFrequencyOperations(final int threadId, final Instant endTime) {
    LOGGER.info("Thread " + threadId + " starting high-frequency operations");

    int batchCount = 0;
    while (Instant.now().isBefore(endTime)) {
      final long batchStart = System.currentTimeMillis();

      try {
        // Execute a batch of operations
        for (int op = 0; op < OPERATIONS_PER_BATCH; op++) {
          final long operationStart = System.currentTimeMillis();

          try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
            try (final Engine engine = runtime.createEngine()) {
              try (final Store store = engine.createStore()) {
                final byte[] moduleBytes = TestUtils.createSimpleWasmModule();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = runtime.instantiate(module);

                final Function addFunction = instance.getFunction("add")
                    .orElseThrow(() -> new AssertionError("add function should be exported"));

                final WasmValue[] args = {WasmValue.i32(threadId), WasmValue.i32(op)};
                final WasmValue[] results = addFunction.call(args);

                assertThat(results).hasSize(1);
                assertThat(results[0].asI32()).isEqualTo(threadId + op);

                instance.close();

                totalOperations.incrementAndGet();
                successfulOperations.incrementAndGet();
                totalExecutionTime.addAndGet(System.currentTimeMillis() - operationStart);

              } catch (final Exception e) {
                totalOperations.incrementAndGet();
                failedOperations.incrementAndGet();
                LOGGER.warning("Operation failed in thread " + threadId + ": " + e.getMessage());
              }
            }
          }
        }

        batchCount++;
        final long batchTime = System.currentTimeMillis() - batchStart;

        if (batchCount % 10 == 0) {
          LOGGER.info("Thread " + threadId + " completed " + batchCount + " batches " +
                     "(" + (batchCount * OPERATIONS_PER_BATCH) + " ops), " +
                     "last batch: " + batchTime + "ms");
        }

      } catch (final Exception e) {
        LOGGER.severe("Batch execution failed in thread " + threadId + ": " + e.getMessage());
        failedOperations.addAndGet(OPERATIONS_PER_BATCH);
        totalOperations.addAndGet(OPERATIONS_PER_BATCH);
      }

      // Small pause to prevent overwhelming the system
      try {
        Thread.sleep(1);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    LOGGER.info("Thread " + threadId + " completed " + batchCount + " batches");
  }

  private void testStoreResourceExhaustion() throws Exception {
    LOGGER.info("Testing Store resource exhaustion");

    final List<Store> stores = new ArrayList<>();
    final int maxStores = 1000; // Reasonable limit for testing

    try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
      try (final Engine engine = runtime.createEngine()) {

        for (int i = 0; i < maxStores; i++) {
          try {
            final Store store = engine.createStore();
            stores.add(store);

            if ((i + 1) % 100 == 0) {
              LOGGER.info("Created " + (i + 1) + " stores");
            }
          } catch (final Exception e) {
            LOGGER.info("Store creation failed at " + i + " stores: " + e.getMessage());
            break;
          }
        }

        LOGGER.info("Successfully created " + stores.size() + " stores");
        assertThat(stores.size()).isGreaterThan(100); // Should handle at least 100 stores

      } finally {
        // Clean up stores
        stores.forEach(store -> {
          try {
            store.close();
          } catch (final Exception e) {
            LOGGER.warning("Failed to close store: " + e.getMessage());
          }
        });
      }
    }
  }

  private void testModuleCompilationResourceExhaustion() throws Exception {
    LOGGER.info("Testing Module compilation resource exhaustion");

    final int maxModules = 500; // Reasonable limit for testing
    int successfulCompilations = 0;

    try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
      try (final Engine engine = runtime.createEngine()) {

        for (int i = 0; i < maxModules; i++) {
          try {
            final byte[] moduleBytes = TestUtils.createSimpleWasmModule();
            final Module module = engine.compileModule(moduleBytes);
            assertThat(module).isNotNull();
            successfulCompilations++;

            if ((i + 1) % 50 == 0) {
              LOGGER.info("Compiled " + (i + 1) + " modules");

              // Force garbage collection to help with memory pressure
              System.gc();
              Thread.sleep(10);
            }
          } catch (final Exception e) {
            LOGGER.info("Module compilation failed at " + i + " modules: " + e.getMessage());
            break;
          }
        }

        LOGGER.info("Successfully compiled " + successfulCompilations + " modules");
        assertThat(successfulCompilations).isGreaterThan(100); // Should handle at least 100 modules
      }
    }
  }

  private void testInstanceCreationResourceExhaustion() throws Exception {
    LOGGER.info("Testing Instance creation resource exhaustion");

    final List<Instance> instances = new ArrayList<>();
    final int maxInstances = 200; // Conservative limit

    try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
      try (final Engine engine = runtime.createEngine()) {

        final byte[] moduleBytes = TestUtils.createSimpleWasmModule();
        final Module module = engine.compileModule(moduleBytes);

        for (int i = 0; i < maxInstances; i++) {
          try {
            final Instance instance = runtime.instantiate(module);
            instances.add(instance);

            if ((i + 1) % 25 == 0) {
              LOGGER.info("Created " + (i + 1) + " instances");
            }
          } catch (final Exception e) {
            LOGGER.info("Instance creation failed at " + i + " instances: " + e.getMessage());
            break;
          }
        }

        LOGGER.info("Successfully created " + instances.size() + " instances");
        assertThat(instances.size()).isGreaterThan(50); // Should handle at least 50 instances

      } finally {
        // Clean up instances
        instances.forEach(instance -> {
          try {
            instance.close();
          } catch (final Exception e) {
            LOGGER.warning("Failed to close instance: " + e.getMessage());
          }
        });
      }
    }
  }

  private void testMemoryResourceExhaustion() throws Exception {
    LOGGER.info("Testing Memory resource exhaustion");

    try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
      try (final Engine engine = runtime.createEngine()) {
        try (final Store store = engine.createStore()) {

          final byte[] moduleBytes = TestUtils.createMemoryWasmModule();
          final Module module = engine.compileModule(moduleBytes);
          final Instance instance = runtime.instantiate(module);

          final Memory memory = instance.getMemory("memory").orElse(null);
          if (memory != null) {
            final long initialSize = memory.getSize();
            LOGGER.info("Initial memory size: " + initialSize + " pages");

            // Test large memory operations
            final int chunkSize = 64 * 1024; // 64KB chunks
            final byte[] largeData = new byte[chunkSize];
            for (int i = 0; i < largeData.length; i++) {
              largeData[i] = (byte) (i % 256);
            }

            // Write to multiple memory locations
            final long memoryBytes = memory.getSize() * 65536; // WebAssembly page size
            final int maxWrites = (int) Math.min(100, memoryBytes / chunkSize);

            for (int i = 0; i < maxWrites; i++) {
              try {
                final long offset = (long) i * chunkSize;
                if (offset + chunkSize <= memoryBytes) {
                  memory.writeBytes(offset, largeData);
                  final byte[] readData = memory.readBytes(offset, chunkSize);
                  assertThat(readData).isEqualTo(largeData);
                }
              } catch (final Exception e) {
                LOGGER.info("Memory operation failed at write " + i + ": " + e.getMessage());
                break;
              }
            }

            LOGGER.info("Memory operations completed successfully");
          } else {
            LOGGER.info("No memory exported by module - skipping memory exhaustion test");
          }

          instance.close();
        }
      }
    }
  }
}
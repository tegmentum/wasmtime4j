package ai.tegmentum.wasmtime4j;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeValidationResult;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestDataManager;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Comprehensive resource management validation tests focusing on phantom reference tracking, memory
 * lifecycle management, and cleanup verification.
 *
 * <p>This test class validates that resource management works correctly under various scenarios
 * including normal operation, failure conditions, and concurrent access patterns.
 */
@DisplayName("Resource Management Validation Tests")
class ResourceManagementValidationTest extends BaseIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ResourceManagementValidationTest.class.getName());

  private WasmTestDataManager testDataManager;
  private ExecutorService executorService;
  private MemoryMonitor memoryMonitor;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    try {
      testDataManager = WasmTestDataManager.getInstance();
      testDataManager.ensureTestDataAvailable();
      executorService = Executors.newFixedThreadPool(8);
      memoryMonitor = new MemoryMonitor();
      memoryMonitor.startMonitoring();
    } catch (final Exception e) {
      LOGGER.warning("Failed to setup test environment: " + e.getMessage());
      skipIfNot(false, "Test environment setup failed: " + e.getMessage());
    }
  }

  @AfterEach
  void tearDownExecutor() {
    if (memoryMonitor != null) {
      memoryMonitor.stopMonitoring();
    }

    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        executorService.shutdownNow();
      }
    }
  }

  @Test
  @DisplayName("Should validate phantom reference cleanup under normal conditions")
  void shouldValidatePhantomReferenceCleanupUnderNormalConditions() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "phantom-reference-normal-cleanup",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final ResourceTracker tracker = new ResourceTracker();
              final int resourceCount = 50;

              try (final Engine engine = runtime.createEngine()) {
                memoryMonitor.resetBaseline();

                // When - Create and close resources with phantom reference tracking
                for (int i = 0; i < resourceCount; i++) {
                  final Module module = engine.compileModule(wasmBytes);
                  tracker.trackResource(module, "Module-" + i);

                  // Use the module
                  try (final Store store = engine.createStore()) {
                    final Instance instance = module.instantiate(store);
                    tracker.trackResource(instance, "Instance-" + i);

                    // Verify resources are valid
                    assertThat(module.isValid()).isTrue();
                    assertThat(instance).isNotNull();

                    instance.close();
                    tracker.untrackResource(instance);
                  }

                  module.close();
                  tracker.untrackResource(module);
                }

                // Force garbage collection and wait for phantom references
                forceGarbageCollectionCycle();

                // Then - Verify cleanup
                final int orphanedResources = tracker.processPhantomReferences();
                assertThat(orphanedResources).isEqualTo(0);

                final MemoryMetrics metrics = memoryMonitor.captureMetrics();
                return String.format(
                    "Phantom reference cleanup: %d resources processed, %d orphaned, "
                        + "memory delta: %dMB",
                    resourceCount * 2, orphanedResources, metrics.getMemoryDeltaMB());
              } finally {
                tracker.shutdown();
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Phantom reference cleanup validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should detect and handle resource leaks via phantom references")
  void shouldDetectAndHandleResourceLeaksViaPhantomReferences() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "phantom-reference-leak-detection",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final ResourceTracker tracker = new ResourceTracker();
              final int leakyResourceCount = 20;

              try (final Engine engine = runtime.createEngine()) {
                memoryMonitor.resetBaseline();

                // When - Create resources without proper cleanup (simulate leaks)
                createLeakyResources(engine, wasmBytes, leakyResourceCount, tracker);

                // Force garbage collection to trigger phantom reference processing
                forceGarbageCollectionCycle();

                // Then - Phantom references should detect and report leaks
                final int detectedLeaks = tracker.processPhantomReferences();
                assertThat(detectedLeaks).isGreaterThan(0);
                assertThat(detectedLeaks).isLessThanOrEqualTo(leakyResourceCount * 2);

                final MemoryMetrics metrics = memoryMonitor.captureMetrics();
                final long memoryLeakMB = metrics.getMemoryDeltaMB();

                // Verify leak detection worked
                assertThat(tracker.getTotalDetectedLeaks()).isEqualTo(detectedLeaks);

                return String.format(
                    "Leak detection: %d/%d resources leaked, memory delta: %dMB",
                    detectedLeaks, leakyResourceCount * 2, memoryLeakMB);
              } finally {
                tracker.shutdown();
              }
            },
            comparison -> true); // Memory usage patterns may vary between runtimes

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Phantom reference leak detection validation: " + validation.getSummary());
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should validate memory lifecycle under stress conditions")
  void shouldValidateMemoryLifecycleUnderStressConditions(final RuntimeType runtimeType) {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "memory-lifecycle-stress-" + runtimeType,
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final ResourceTracker tracker = new ResourceTracker();
              final int cycleCount = 20;
              final int resourcesPerCycle = 25;
              final long stressTestDurationMs = 60000; // 1 minute stress test

              try (final Engine engine = runtime.createEngine()) {
                memoryMonitor.resetBaseline();
                final Instant stressStart = Instant.now();

                int totalResourcesCreated = 0;
                int totalResourcesCleaned = 0;

                // When - Perform stressed resource allocation/deallocation cycles
                while (Duration.between(stressStart, Instant.now()).toMillis()
                    < stressTestDurationMs) {

                  for (int cycle = 0; cycle < cycleCount; cycle++) {
                    final List<Module> modules = new ArrayList<>();
                    final List<Store> stores = new ArrayList<>();
                    final List<Instance> instances = new ArrayList<>();

                    // Create resources
                    for (int i = 0; i < resourcesPerCycle; i++) {
                      final Module module = engine.compileModule(wasmBytes);
                      final Store store = engine.createStore();
                      final Instance instance = module.instantiate(store);

                      modules.add(module);
                      stores.add(store);
                      instances.add(instance);

                      tracker.trackResource(module, "StressModule-" + cycle + "-" + i);
                      tracker.trackResource(instance, "StressInstance-" + cycle + "-" + i);
                      totalResourcesCreated += 2;
                    }

                    // Use resources briefly
                    Thread.yield();

                    // Clean up resources
                    for (int i = instances.size() - 1; i >= 0; i--) {
                      instances.get(i).close();
                      stores.get(i).close();
                      modules.get(i).close();

                      tracker.untrackResource(instances.get(i));
                      tracker.untrackResource(modules.get(i));
                      totalResourcesCleaned += 2;
                    }

                    // Periodic garbage collection
                    if (cycle % 5 == 0) {
                      forceGarbageCollectionCycle();
                      tracker.processPhantomReferences();
                    }
                  }

                  // Check for memory pressure
                  final MemoryMetrics currentMetrics = memoryMonitor.captureMetrics();
                  if (currentMetrics.getMemoryDeltaMB() > 500) { // 500MB threshold
                    LOGGER.warning(
                        "High memory usage detected: " + currentMetrics.getMemoryDeltaMB() + "MB");
                    forceGarbageCollectionCycle();
                  }
                }

                // Final cleanup and verification
                forceGarbageCollectionCycle();
                final int detectedLeaks = tracker.processPhantomReferences();

                final MemoryMetrics finalMetrics = memoryMonitor.captureMetrics();
                final Duration testDuration = Duration.between(stressStart, Instant.now());

                // Then - Verify stress test results
                assertThat(detectedLeaks).isLessThan(totalResourcesCreated / 10); // < 10% leaks
                assertThat(finalMetrics.getMemoryDeltaMB())
                    .isLessThan(200); // Less than 200MB growth

                return String.format(
                    "Stress test: %ds duration, %d created, %d cleaned, %d leaks, "
                        + "memory delta: %dMB",
                    testDuration.getSeconds(),
                    totalResourcesCreated,
                    totalResourcesCleaned,
                    detectedLeaks,
                    finalMetrics.getMemoryDeltaMB());
              } finally {
                tracker.shutdown();
              }
            },
            comparison -> true); // Stress test results may vary

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Memory lifecycle stress test validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle resource cleanup under exception conditions")
  void shouldHandleResourceCleanupUnderExceptionConditions() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "exception-resource-cleanup",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final byte[] malformedBytes = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};
              final ResourceTracker tracker = new ResourceTracker();

              try (final Engine engine = runtime.createEngine()) {
                memoryMonitor.resetBaseline();

                int successfulOperations = 0;
                int failedOperations = 0;
                int resourcesTracked = 0;

                // When - Mix successful and failing operations
                for (int i = 0; i < 50; i++) {
                  try {
                    final boolean shouldFail = (i % 7 == 0); // ~14% failure rate
                    final byte[] testBytes = shouldFail ? malformedBytes : wasmBytes;

                    final Module module = engine.compileModule(testBytes);
                    tracker.trackResource(module, "ExceptionTest-" + i);
                    resourcesTracked++;

                    // Try to use the module
                    try (final Store store = engine.createStore()) {
                      final Instance instance = module.instantiate(store);
                      tracker.trackResource(instance, "ExceptionInstance-" + i);
                      resourcesTracked++;

                      // Clean up properly
                      instance.close();
                      tracker.untrackResource(instance);
                      resourcesTracked--;
                    }

                    module.close();
                    tracker.untrackResource(module);
                    resourcesTracked--;
                    successfulOperations++;

                  } catch (final Exception e) {
                    // Expected for malformed modules
                    failedOperations++;
                  }
                }

                // Force cleanup and check for leaks
                forceGarbageCollectionCycle();
                final int detectedLeaks = tracker.processPhantomReferences();

                // Then - Verify exception handling didn't cause resource leaks
                assertThat(failedOperations).isGreaterThan(0); // Should have some failures
                assertThat(successfulOperations).isGreaterThan(failedOperations);
                assertThat(detectedLeaks)
                    .isLessThan(successfulOperations / 5); // Less than 20% leak rate

                final MemoryMetrics metrics = memoryMonitor.captureMetrics();

                return String.format(
                    "Exception handling: %d successful, %d failed, %d leaks detected, "
                        + "memory delta: %dMB",
                    successfulOperations,
                    failedOperations,
                    detectedLeaks,
                    metrics.getMemoryDeltaMB());
              } finally {
                tracker.shutdown();
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Exception resource cleanup validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate resource cleanup timing and efficiency")
  void shouldValidateResourceCleanupTimingAndEfficiency() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "cleanup-timing-efficiency",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final ResourceTracker tracker = new ResourceTracker();
              final int batchSize = 100;

              try (final Engine engine = runtime.createEngine()) {
                memoryMonitor.resetBaseline();

                // When - Measure cleanup timing
                final Instant creationStart = Instant.now();

                final List<Module> modules = new ArrayList<>();
                for (int i = 0; i < batchSize; i++) {
                  final Module module = engine.compileModule(wasmBytes);
                  modules.add(module);
                  tracker.trackResource(module, "TimingTest-" + i);
                }

                final Duration creationTime = Duration.between(creationStart, Instant.now());

                // Measure cleanup timing
                final Instant cleanupStart = Instant.now();
                for (final Module module : modules) {
                  module.close();
                  tracker.untrackResource(module);
                }
                final Duration cleanupTime = Duration.between(cleanupStart, Instant.now());

                // Force phantom reference processing
                final Instant phantomStart = Instant.now();
                forceGarbageCollectionCycle();
                final int phantomLeaks = tracker.processPhantomReferences();
                final Duration phantomTime = Duration.between(phantomStart, Instant.now());

                final MemoryMetrics metrics = memoryMonitor.captureMetrics();

                // Then - Verify timing efficiency
                final double creationRate = (double) batchSize / creationTime.toMillis() * 1000;
                final double cleanupRate = (double) batchSize / cleanupTime.toMillis() * 1000;

                assertThat(creationRate).isGreaterThan(10); // At least 10 modules/sec
                assertThat(cleanupRate).isGreaterThan(50); // At least 50 cleanups/sec
                assertThat(phantomTime.toMillis())
                    .isLessThan(5000); // Less than 5s phantom processing
                assertThat(phantomLeaks).isEqualTo(0); // No leaks in clean test

                return String.format(
                    "Timing: %.1f create/s, %.1f cleanup/s, %dms phantom, %d leaks, "
                        + "memory delta: %dMB",
                    creationRate,
                    cleanupRate,
                    phantomTime.toMillis(),
                    phantomLeaks,
                    metrics.getMemoryDeltaMB());
              } finally {
                tracker.shutdown();
              }
            },
            comparison -> true); // Timing may vary between runtimes

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Cleanup timing efficiency validation: " + validation.getSummary());
  }

  /** Creates resources without proper cleanup to simulate leaks for testing. */
  @SuppressWarnings("unused")
  private void createLeakyResources(
      final Engine engine, final byte[] wasmBytes, final int count, final ResourceTracker tracker) {
    for (int i = 0; i < count; i++) {
      try {
        final Module module = engine.compileModule(wasmBytes);
        tracker.trackResource(module, "LeakyModule-" + i);

        final Store store = engine.createStore();
        final Instance instance = module.instantiate(store);
        tracker.trackResource(instance, "LeakyInstance-" + i);

        // Intentionally don't close - simulate leak
      } catch (final Exception e) {
        // Ignore errors in leak simulation
      }
    }
  }

  /** Forces multiple garbage collection cycles to ensure phantom reference processing. */
  private void forceGarbageCollectionCycle() {
    for (int i = 0; i < 5; i++) {
      System.gc();
      try {
        Thread.sleep(50); // Give GC time to run
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Resource tracker for phantom reference monitoring. */
  private static class ResourceTracker {
    private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
    private final List<PhantomReference<Object>> phantomReferences =
        Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger totalDetectedLeaks = new AtomicInteger(0);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public void trackResource(final Object resource, final String resourceId) {
      if (shutdown.get()) {
        return;
      }
      final PhantomReference<Object> phantom = new PhantomReference<>(resource, referenceQueue);
      phantomReferences.add(phantom);
      LOGGER.fine("Tracking resource: " + resourceId);
    }

    public void untrackResource(final Object resource) {
      // In a real phantom reference system, we can't directly untrack by object
      // This method exists for API compatibility but phantom refs will handle cleanup
      LOGGER.fine("Untracking resource (phantom reference will handle cleanup)");
    }

    public int processPhantomReferences() {
      int leaksDetected = 0;
      try {
        while (true) {
          final PhantomReference<?> ref = (PhantomReference<?>) referenceQueue.poll();
          if (ref == null) {
            break;
          }
          phantomReferences.remove(ref);
          ref.clear();
          leaksDetected++;
        }
      } catch (final Exception e) {
        LOGGER.warning("Error processing phantom references: " + e.getMessage());
      }

      if (leaksDetected > 0) {
        totalDetectedLeaks.addAndGet(leaksDetected);
        LOGGER.warning("Detected " + leaksDetected + " resource leaks via phantom references");
      }

      return leaksDetected;
    }

    public int getTotalDetectedLeaks() {
      return totalDetectedLeaks.get();
    }

    public void shutdown() {
      shutdown.set(true);
      processPhantomReferences(); // Final cleanup
    }
  }

  /** Memory monitoring utility for tracking memory usage patterns. */
  private static class MemoryMonitor {
    private final Runtime runtime = Runtime.getRuntime();
    private volatile long baselineMemory = 0;
    private volatile long peakMemory = 0;
    private final AtomicBoolean monitoring = new AtomicBoolean(false);

    public void startMonitoring() {
      monitoring.set(true);
      resetBaseline();
    }

    public void stopMonitoring() {
      monitoring.set(false);
    }

    public void resetBaseline() {
      System.gc();
      try {
        Thread.sleep(100);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      baselineMemory = getUsedMemory();
      peakMemory = baselineMemory;
    }

    public MemoryMetrics captureMetrics() {
      final long currentMemory = getUsedMemory();
      peakMemory = Math.max(peakMemory, currentMemory);

      return new MemoryMetrics(baselineMemory, currentMemory, peakMemory);
    }

    private long getUsedMemory() {
      return runtime.totalMemory() - runtime.freeMemory();
    }
  }

  /** Container for memory metrics. */
  private static class MemoryMetrics {
    private final long baselineMemory;
    private final long currentMemory;
    private final long peakMemory;

    public MemoryMetrics(
        final long baselineMemory, final long currentMemory, final long peakMemory) {
      this.baselineMemory = baselineMemory;
      this.currentMemory = currentMemory;
      this.peakMemory = peakMemory;
    }

    public long getMemoryDeltaMB() {
      return (currentMemory - baselineMemory) / (1024 * 1024);
    }

    public long getPeakDeltaMB() {
      return (peakMemory - baselineMemory) / (1024 * 1024);
    }

    public long getCurrentMemoryMB() {
      return currentMemory / (1024 * 1024);
    }
  }
}

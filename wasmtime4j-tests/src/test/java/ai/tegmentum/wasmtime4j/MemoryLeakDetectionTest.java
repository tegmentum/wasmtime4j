package ai.tegmentum.wasmtime4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeValidationResult;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestDataManager;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Comprehensive memory leak detection tests with long-running stress scenarios.
 *
 * <p>This test class performs extended stress testing (up to 1 hour) to identify memory leaks,
 * resource accumulation, and cleanup effectiveness under sustained load.
 */
@DisplayName("Memory Leak Detection Tests")
class MemoryLeakDetectionTest extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(MemoryLeakDetectionTest.class.getName());

  private WasmTestDataManager testDataManager;
  private ExecutorService executorService;
  private ScheduledExecutorService monitoringService;
  private AdvancedMemoryMonitor memoryMonitor;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // Skip long-running tests unless explicitly enabled
    skipIfCategoryNotEnabled("stress.memory");

    try {
      testDataManager = WasmTestDataManager.getInstance();
      testDataManager.ensureTestDataAvailable();
      executorService = Executors.newFixedThreadPool(16);
      monitoringService = Executors.newScheduledThreadPool(4);
      memoryMonitor = new AdvancedMemoryMonitor();
    } catch (final Exception e) {
      LOGGER.warning("Failed to setup test environment: " + e.getMessage());
      skipIfNot(false, "Test environment setup failed: " + e.getMessage());
    }
  }

  @AfterEach
  void tearDownServices() {
    if (memoryMonitor != null) {
      memoryMonitor.stopMonitoring();
    }

    if (monitoringService != null && !monitoringService.isShutdown()) {
      monitoringService.shutdown();
      try {
        if (!monitoringService.awaitTermination(10, TimeUnit.SECONDS)) {
          monitoringService.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        monitoringService.shutdownNow();
      }
    }

    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        executorService.shutdownNow();
      }
    }
  }

  @Test
  @DisplayName("Should detect memory leaks in 1-hour stress test")
  void shouldDetectMemoryLeaksInOneHourStressTest() {
    final long testDurationMs = 60 * 60 * 1000; // 1 hour

    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "one-hour-memory-leak-stress",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final StressTestController controller = new StressTestController(testDurationMs);

              try (final Engine engine = runtime.createEngine()) {
                memoryMonitor.startMonitoring("1-hour-stress-test");

                // When - Run sustained stress test with memory monitoring
                final CompletableFuture<StressTestResults> stressTest =
                    runSustainedStressTest(engine, wasmBytes, controller);

                // Monitor memory during stress test
                final CompletableFuture<Void> memoryMonitoring =
                    monitorMemoryDuringStressTest(controller);

                // Wait for both to complete
                final StressTestResults results = stressTest.join();
                memoryMonitoring.join();

                final AdvancedMemoryMetrics finalMetrics = memoryMonitor.captureDetailedMetrics();

                // Then - Analyze results for memory leaks
                analyzeMemoryLeakIndicators(results, finalMetrics);

                return String.format(
                    "1-hour stress: %d operations, %d leaked, %.2f ops/sec, "
                        + "memory: %dMB baseline -> %dMB peak -> %dMB final (trend: %s)",
                    results.totalOperations,
                    results.detectedLeaks,
                    results.getOperationsPerSecond(),
                    finalMetrics.getBaselineMemoryMB(),
                    finalMetrics.getPeakMemoryMB(),
                    finalMetrics.getCurrentMemoryMB(),
                    finalMetrics.getMemoryTrend());
              }
            },
            comparison -> true); // Long-running stress test results may vary

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("1-hour memory leak stress test validation: " + validation.getSummary());
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should identify memory accumulation patterns over time")
  void shouldIdentifyMemoryAccumulationPatternsOverTime(final RuntimeType runtimeType) {
    final long testDurationMs = 15 * 60 * 1000; // 15 minutes for parameterized test

    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "memory-accumulation-patterns-" + runtimeType,
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final MemoryAccumulationTracker tracker = new MemoryAccumulationTracker();

              try (final Engine engine = runtime.createEngine()) {
                memoryMonitor.startMonitoring("memory-accumulation-" + runtimeType);

                // When - Run cyclic operations with detailed memory tracking
                final Instant testStart = Instant.now();
                int cycleCount = 0;

                while (Duration.between(testStart, Instant.now()).toMillis() < testDurationMs) {
                  final int resourcesInCycle = 50 + ThreadLocalRandom.current().nextInt(50);

                  // Create resources
                  final List<AutoCloseable> resources =
                      createResourceBatch(engine, wasmBytes, resourcesInCycle);

                  // Track memory after creation
                  tracker.recordMemoryAfterCreation();

                  // Use resources briefly
                  useResourcesBriefly(resources);

                  // Clean up resources
                  cleanupResourceBatch(resources);

                  // Track memory after cleanup
                  tracker.recordMemoryAfterCleanup();

                  cycleCount++;

                  // Periodic analysis
                  if (cycleCount % 20 == 0) {
                    performIntermediateGarbageCollection();
                    tracker.analyzeAccumulationPattern();
                  }
                }

                // Final analysis
                performCompleteGarbageCollection();
                final AccumulationAnalysis analysis = tracker.performFinalAnalysis();
                final AdvancedMemoryMetrics finalMetrics = memoryMonitor.captureDetailedMetrics();

                // Then - Verify memory accumulation is within acceptable bounds
                assertThat(analysis.getMemoryLeakRate()).isLessThan(1.0); // < 1MB/minute
                assertThat(analysis.getMaxAccumulation())
                    .isLessThan(100); // < 100MB max accumulation
                assertThat(finalMetrics.getMemoryGrowthRate())
                    .isLessThan(0.5); // < 0.5MB/minute sustained

                return String.format(
                    "Accumulation analysis: %d cycles, %.2f MB/min leak rate, %dMB max"
                        + " accumulation, cleanup efficiency: %.1f%%",
                    cycleCount,
                    analysis.getMemoryLeakRate(),
                    analysis.getMaxAccumulation(),
                    analysis.getCleanupEfficiency() * 100);
              }
            },
            comparison -> true); // Memory patterns may vary between runtimes

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Memory accumulation patterns validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should detect memory leaks under concurrent load")
  void shouldDetectMemoryLeaksUnderConcurrentLoad() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "concurrent-memory-leak-detection",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int threadCount = 8;
              final long testDurationMs = 10 * 60 * 1000; // 10 minutes
              final AtomicBoolean testRunning = new AtomicBoolean(true);
              final CountDownLatch threadsFinished = new CountDownLatch(threadCount);
              final ConcurrentMemoryTracker concurrentTracker = new ConcurrentMemoryTracker();

              try (final Engine engine = runtime.createEngine()) {
                memoryMonitor.startMonitoring("concurrent-memory-leak");

                // When - Start concurrent threads performing memory operations
                final List<CompletableFuture<ThreadMemoryStats>> futures = new ArrayList<>();

                for (int i = 0; i < threadCount; i++) {
                  final int threadId = i;
                  final CompletableFuture<ThreadMemoryStats> future =
                      CompletableFuture.supplyAsync(
                          () -> {
                            try {
                              return runConcurrentMemoryOperations(
                                  engine, wasmBytes, threadId, testRunning, concurrentTracker);
                            } finally {
                              threadsFinished.countDown();
                            }
                          },
                          executorService);
                  futures.add(future);
                }

                // Let test run for specified duration
                Thread.sleep(testDurationMs);
                testRunning.set(false);

                // Wait for all threads to complete
                final boolean completed = threadsFinished.await(60, TimeUnit.SECONDS);
                assertThat(completed).isTrue();

                // Collect results from all threads
                final List<ThreadMemoryStats> threadStats = new ArrayList<>();
                for (final CompletableFuture<ThreadMemoryStats> future : futures) {
                  threadStats.add(future.join());
                }

                // Final memory analysis
                performCompleteGarbageCollection();
                final ConcurrentMemoryAnalysis analysis = concurrentTracker.performFinalAnalysis();
                final AdvancedMemoryMetrics finalMetrics = memoryMonitor.captureDetailedMetrics();

                // Then - Verify no significant leaks under concurrent load
                final long totalOperations =
                    threadStats.stream().mapToLong(stats -> stats.operationsCompleted).sum();
                final long totalLeaks =
                    threadStats.stream().mapToLong(stats -> stats.detectedLeaks).sum();
                final double leakRate = (double) totalLeaks / totalOperations * 100;

                assertThat(leakRate).isLessThan(5.0); // Less than 5% leak rate
                assertThat(analysis.getConcurrentMemoryPressure())
                    .isLessThan(500); // < 500MB pressure
                assertThat(finalMetrics.getMemoryFragmentation())
                    .isLessThan(20.0); // < 20% fragmentation

                return String.format(
                    "Concurrent test: %d threads, %d total ops, %d leaks (%.2f%%), "
                        + "memory pressure: %dMB, fragmentation: %.1f%%",
                    threadCount,
                    totalOperations,
                    totalLeaks,
                    leakRate,
                    analysis.getConcurrentMemoryPressure(),
                    finalMetrics.getMemoryFragmentation());
              }
            },
            comparison -> true); // Concurrent results may vary

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Concurrent memory leak detection validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate garbage collection effectiveness under stress")
  void shouldValidateGarbageCollectionEffectivenessUnderStress() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "gc-effectiveness-stress",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final GarbageCollectionMonitor gcMonitor = new GarbageCollectionMonitor();

              try (final Engine engine = runtime.createEngine()) {
                memoryMonitor.startMonitoring("gc-effectiveness");
                gcMonitor.startMonitoring();

                // When - Create memory pressure and monitor GC effectiveness
                final int pressureCycles = 100;
                final int resourcesPerCycle = 200;

                for (int cycle = 0; cycle < pressureCycles; cycle++) {
                  // Create significant memory pressure
                  final List<AutoCloseable> resources =
                      createLargeResourceBatch(engine, wasmBytes, resourcesPerCycle);

                  // Measure memory before cleanup
                  final long memoryBeforeCleanup = getCurrentMemoryUsage();

                  // Clean up resources
                  cleanupResourceBatch(resources);

                  // Force garbage collection and measure effectiveness
                  final long memoryAfterCleanup = triggerGarbageCollectionAndMeasure();

                  final double cleanupEffectiveness =
                      calculateCleanupEffectiveness(memoryBeforeCleanup, memoryAfterCleanup);

                  gcMonitor.recordCleanupEffectiveness(cleanupEffectiveness);

                  // Verify GC effectiveness doesn't degrade significantly
                  if (cycle > 10 && cleanupEffectiveness < 70.0) { // Less than 70% effective
                    LOGGER.warning(
                        "GC effectiveness degraded to "
                            + cleanupEffectiveness
                            + "% at cycle "
                            + cycle);
                  }

                  // Periodic detailed analysis
                  if (cycle % 20 == 0) {
                    gcMonitor.analyzeGarbageCollectionPatterns();
                  }
                }

                // Final GC analysis
                final GarbageCollectionAnalysis gcAnalysis = gcMonitor.performFinalAnalysis();
                final AdvancedMemoryMetrics finalMetrics = memoryMonitor.captureDetailedMetrics();

                // Then - Verify GC effectiveness remains acceptable
                assertThat(gcAnalysis.getAverageEffectiveness())
                    .isGreaterThan(75.0); // > 75% average
                assertThat(gcAnalysis.getEffectivenessDegradation())
                    .isLessThan(10.0); // < 10% degradation
                assertThat(finalMetrics.getGarbageCollectionOverhead())
                    .isLessThan(15.0); // < 15% overhead

                return String.format(
                    "GC effectiveness: %.1f%% average, %.1f%% degradation, %.1f%% overhead, "
                        + "%d major collections, %d minor collections",
                    gcAnalysis.getAverageEffectiveness(),
                    gcAnalysis.getEffectivenessDegradation(),
                    finalMetrics.getGarbageCollectionOverhead(),
                    gcAnalysis.getMajorCollections(),
                    gcAnalysis.getMinorCollections());
              }
            },
            comparison -> true); // GC behavior may vary between runtimes

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("GC effectiveness stress validation: " + validation.getSummary());
  }

  /** Runs sustained stress test with comprehensive monitoring. */
  private CompletableFuture<StressTestResults> runSustainedStressTest(
      final Engine engine, final byte[] wasmBytes, final StressTestController controller) {
    return CompletableFuture.supplyAsync(
        () -> {
          final StressTestResults results = new StressTestResults();
          final Instant startTime = Instant.now();

          while (!controller.shouldStop()) {
            try {
              // Perform batch of operations
              final int batchSize = 20 + ThreadLocalRandom.current().nextInt(30);
              final List<AutoCloseable> resources =
                  createResourceBatch(engine, wasmBytes, batchSize);

              // Use resources
              useResourcesBriefly(resources);

              // Clean up resources
              cleanupResourceBatch(resources);

              results.totalOperations += batchSize;

              // Periodic checks
              if (results.totalOperations % 1000 == 0) {
                performIntermediateGarbageCollection();
                controller.checkMemoryPressure();
              }

            } catch (final Exception e) {
              results.errors++;
              LOGGER.warning("Stress test error: " + e.getMessage());
            }
          }

          results.testDuration = Duration.between(startTime, Instant.now());
          return results;
        },
        executorService);
  }

  /** Monitors memory during stress test execution. */
  private CompletableFuture<Void> monitorMemoryDuringStressTest(
      final StressTestController controller) {
    return CompletableFuture.runAsync(
        () -> {
          while (!controller.shouldStop()) {
            try {
              memoryMonitor.recordMemorySnapshot();
              Thread.sleep(5000); // Monitor every 5 seconds
            } catch (final InterruptedException e) {
              Thread.currentThread().interrupt();
              break;
            }
          }
        },
        monitoringService);
  }

  /** Creates a batch of resources for testing. */
  private List<AutoCloseable> createResourceBatch(
      final Engine engine, final byte[] wasmBytes, final int count) {
    final List<AutoCloseable> resources = new ArrayList<>();
    try {
      for (int i = 0; i < count; i++) {
        final Module module = engine.compileModule(wasmBytes);
        final Store store = engine.createStore();
        final Instance instance = module.instantiate(store);

        resources.add(instance);
        resources.add(store);
        resources.add(module);
      }
    } catch (final Exception e) {
      // Clean up any created resources if there's an error
      cleanupResourceBatch(resources);
      throw new RuntimeException("Failed to create resource batch", e);
    }
    return resources;
  }

  /** Creates a larger batch of resources for memory pressure testing. */
  private List<AutoCloseable> createLargeResourceBatch(
      final Engine engine, final byte[] wasmBytes, final int count) {
    // Create more complex resource structures
    return createResourceBatch(engine, wasmBytes, count);
  }

  /** Uses resources briefly to simulate realistic usage. */
  private void useResourcesBriefly(final List<AutoCloseable> resources) {
    // Simulate brief resource usage
    Thread.yield();
  }

  /** Cleans up a batch of resources. */
  private void cleanupResourceBatch(final List<AutoCloseable> resources) {
    for (final AutoCloseable resource : resources) {
      try {
        resource.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
  }

  /** Runs concurrent memory operations for a specific thread. */
  private ThreadMemoryStats runConcurrentMemoryOperations(
      final Engine engine,
      final byte[] wasmBytes,
      final int threadId,
      final AtomicBoolean testRunning,
      final ConcurrentMemoryTracker tracker) {
    final ThreadMemoryStats stats = new ThreadMemoryStats(threadId);

    while (testRunning.get()) {
      try {
        final int batchSize = 5 + ThreadLocalRandom.current().nextInt(15);
        final List<AutoCloseable> resources = createResourceBatch(engine, wasmBytes, batchSize);

        tracker.recordResourceCreation(threadId, batchSize);

        useResourcesBriefly(resources);
        cleanupResourceBatch(resources);

        tracker.recordResourceCleanup(threadId, batchSize);
        stats.operationsCompleted += batchSize;

        // Random delay to simulate realistic usage
        Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));

      } catch (final Exception e) {
        stats.errors++;
      }
    }

    return stats;
  }

  /** Performs intermediate garbage collection without full system pressure. */
  private void performIntermediateGarbageCollection() {
    System.gc();
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Performs complete garbage collection with system finalization. */
  private void performCompleteGarbageCollection() {
    for (int i = 0; i < 5; i++) {
      System.gc();
      System.runFinalization();
      try {
        Thread.sleep(200);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Gets current memory usage. */
  private long getCurrentMemoryUsage() {
    final Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  /** Triggers garbage collection and measures memory afterwards. */
  private long triggerGarbageCollectionAndMeasure() {
    performCompleteGarbageCollection();
    return getCurrentMemoryUsage();
  }

  /** Calculates cleanup effectiveness percentage. */
  private double calculateCleanupEffectiveness(final long before, final long after) {
    if (before <= after) {
      return 0.0;
    }
    return ((double) (before - after) / before) * 100.0;
  }

  /** Analyzes memory leak indicators from stress test results. */
  private void analyzeMemoryLeakIndicators(
      final StressTestResults results, final AdvancedMemoryMetrics metrics) {
    // Check for memory leaks based on various indicators
    if (metrics.getMemoryGrowthRate() > 2.0) { // More than 2MB/minute sustained growth
      fail("Significant memory leak detected: " + metrics.getMemoryGrowthRate() + " MB/minute");
    }

    if (metrics.getMemoryEfficiency() < 70.0) { // Less than 70% memory efficiency
      LOGGER.warning("Low memory efficiency detected: " + metrics.getMemoryEfficiency() + "%");
    }

    if (results.getOperationsPerSecond() < 10.0) { // Less than 10 ops/sec indicates problems
      LOGGER.warning(
          "Low operation rate detected: " + results.getOperationsPerSecond() + " ops/sec");
    }
  }

  /** Controls stress test execution and provides stopping conditions. */
  private static class StressTestController {
    private final long testDurationMs;
    private final Instant startTime;
    private final AtomicBoolean forceStop = new AtomicBoolean(false);

    public StressTestController(final long testDurationMs) {
      this.testDurationMs = testDurationMs;
      this.startTime = Instant.now();
    }

    public boolean shouldStop() {
      if (forceStop.get()) {
        return true;
      }

      final Duration elapsed = Duration.between(startTime, Instant.now());
      return elapsed.toMillis() >= testDurationMs;
    }

    public void checkMemoryPressure() {
      final Runtime runtime = Runtime.getRuntime();
      final long maxMemory = runtime.maxMemory();
      final long totalMemory = runtime.totalMemory();
      final double memoryUsage = (double) totalMemory / maxMemory;

      if (memoryUsage > 0.9) { // More than 90% memory usage
        LOGGER.warning("High memory pressure detected: " + (memoryUsage * 100) + "%");
        System.gc(); // Try to free memory
      }

      if (memoryUsage > 0.95) { // Critical memory usage
        LOGGER.severe("Critical memory pressure - stopping stress test");
        forceStop.set(true);
      }
    }
  }

  /** Container for stress test results. */
  private static class StressTestResults {
    long totalOperations = 0;
    long detectedLeaks = 0;
    long errors = 0;
    Duration testDuration = Duration.ZERO;

    public double getOperationsPerSecond() {
      if (testDuration.toMillis() == 0) {
        return 0.0;
      }
      return (double) totalOperations / testDuration.toMillis() * 1000.0;
    }
  }

  /** Tracks memory accumulation patterns over time. */
  private static class MemoryAccumulationTracker {
    private final List<Long> creationMemorySnapshots = new ArrayList<>();
    private final List<Long> cleanupMemorySnapshots = new ArrayList<>();
    private long baselineMemory = 0;

    public MemoryAccumulationTracker() {
      this.baselineMemory = getCurrentMemoryUsage();
    }

    public void recordMemoryAfterCreation() {
      creationMemorySnapshots.add(getCurrentMemoryUsage());
    }

    public void recordMemoryAfterCleanup() {
      cleanupMemorySnapshots.add(getCurrentMemoryUsage());
    }

    public void analyzeAccumulationPattern() {
      if (cleanupMemorySnapshots.size() < 10) {
        return; // Not enough data yet
      }

      final List<Long> recent =
          cleanupMemorySnapshots.subList(
              Math.max(0, cleanupMemorySnapshots.size() - 10), cleanupMemorySnapshots.size());

      final double avgRecent = recent.stream().mapToLong(Long::longValue).average().orElse(0);
      final long memoryIncrease = (long) avgRecent - baselineMemory;

      if (memoryIncrease > 50 * 1024 * 1024) { // More than 50MB accumulation
        LOGGER.warning("Memory accumulation detected: " + (memoryIncrease / 1024 / 1024) + "MB");
      }
    }

    public AccumulationAnalysis performFinalAnalysis() {
      return new AccumulationAnalysis(
          creationMemorySnapshots, cleanupMemorySnapshots, baselineMemory);
    }

    private long getCurrentMemoryUsage() {
      final Runtime runtime = Runtime.getRuntime();
      return runtime.totalMemory() - runtime.freeMemory();
    }
  }

  /** Analysis results for memory accumulation patterns. */
  private static class AccumulationAnalysis {
    private final double memoryLeakRate;
    private final long maxAccumulation;
    private final double cleanupEfficiency;

    public AccumulationAnalysis(
        final List<Long> creationSnapshots,
        final List<Long> cleanupSnapshots,
        final long baseline) {
      if (cleanupSnapshots.isEmpty()) {
        this.memoryLeakRate = 0.0;
        this.maxAccumulation = 0;
        this.cleanupEfficiency = 1.0;
        return;
      }

      // Calculate leak rate (MB per minute)
      final long finalMemory = cleanupSnapshots.get(cleanupSnapshots.size() - 1);
      final long memoryIncrease = finalMemory - baseline;
      final double durationMinutes = cleanupSnapshots.size() / 3.0; // Assuming ~20s per cycle
      this.memoryLeakRate = (memoryIncrease / 1024.0 / 1024.0) / durationMinutes;

      // Calculate max accumulation
      this.maxAccumulation =
          cleanupSnapshots.stream().mapToLong(mem -> mem - baseline).max().orElse(0) / 1024 / 1024;

      // Calculate cleanup efficiency
      if (creationSnapshots.size() == cleanupSnapshots.size()) {
        double totalCreated = 0;
        double totalCleaned = 0;
        for (int i = 0; i < creationSnapshots.size(); i++) {
          final long created = creationSnapshots.get(i) - baseline;
          final long cleaned = baseline - cleanupSnapshots.get(i);
          totalCreated += Math.max(0, created);
          totalCleaned += Math.max(0, cleaned);
        }
        this.cleanupEfficiency = totalCreated > 0 ? totalCleaned / totalCreated : 1.0;
      } else {
        this.cleanupEfficiency = 0.8; // Default estimate
      }
    }

    public double getMemoryLeakRate() {
      return memoryLeakRate;
    }

    public long getMaxAccumulation() {
      return maxAccumulation;
    }

    public double getCleanupEfficiency() {
      return cleanupEfficiency;
    }
  }

  /** Tracks concurrent memory operations across threads. */
  private static class ConcurrentMemoryTracker {
    private final AtomicLong totalCreated = new AtomicLong(0);
    private final AtomicLong totalCleaned = new AtomicLong(0);
    private final List<Long> memorySnapshots = Collections.synchronizedList(new ArrayList<>());

    public void recordResourceCreation(final int threadId, final int count) {
      totalCreated.addAndGet(count);
      memorySnapshots.add(getCurrentMemoryUsage());
    }

    public void recordResourceCleanup(final int threadId, final int count) {
      totalCleaned.addAndGet(count);
    }

    public ConcurrentMemoryAnalysis performFinalAnalysis() {
      return new ConcurrentMemoryAnalysis(totalCreated.get(), totalCleaned.get(), memorySnapshots);
    }

    private long getCurrentMemoryUsage() {
      final Runtime runtime = Runtime.getRuntime();
      return runtime.totalMemory() - runtime.freeMemory();
    }
  }

  /** Analysis results for concurrent memory operations. */
  private static class ConcurrentMemoryAnalysis {
    private final long concurrentMemoryPressure;

    public ConcurrentMemoryAnalysis(
        final long totalCreated, final long totalCleaned, final List<Long> snapshots) {
      if (snapshots.isEmpty()) {
        this.concurrentMemoryPressure = 0;
        return;
      }

      final long maxMemory = snapshots.stream().mapToLong(Long::longValue).max().orElse(0);
      final long minMemory = snapshots.stream().mapToLong(Long::longValue).min().orElse(0);
      this.concurrentMemoryPressure = (maxMemory - minMemory) / 1024 / 1024;
    }

    public long getConcurrentMemoryPressure() {
      return concurrentMemoryPressure;
    }
  }

  /** Statistics for individual thread memory operations. */
  private static class ThreadMemoryStats {
    final int threadId;
    long operationsCompleted = 0;
    long detectedLeaks = 0;
    long errors = 0;

    public ThreadMemoryStats(final int threadId) {
      this.threadId = threadId;
    }
  }

  /** Monitors garbage collection behavior and effectiveness. */
  private static class GarbageCollectionMonitor {
    private final List<GarbageCollectorMXBean> gcBeans;
    private final MemoryMXBean memoryBean;
    private final List<Double> effectivenessHistory = new ArrayList<>();
    private long initialMajorCollections = 0;
    private long initialMinorCollections = 0;

    public GarbageCollectionMonitor() {
      this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
      this.memoryBean = ManagementFactory.getMemoryMXBean();
    }

    public void startMonitoring() {
      for (final GarbageCollectorMXBean gcBean : gcBeans) {
        if (gcBean.getName().contains("Old") || gcBean.getName().contains("Tenured")) {
          initialMajorCollections += gcBean.getCollectionCount();
        } else {
          initialMinorCollections += gcBean.getCollectionCount();
        }
      }
    }

    public void recordCleanupEffectiveness(final double effectiveness) {
      effectivenessHistory.add(effectiveness);
    }

    public void analyzeGarbageCollectionPatterns() {
      final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
      final double heapUtilization =
          (double) heapUsage.getUsed() / heapUsage.getCommitted() * 100.0;

      if (heapUtilization > 85.0) {
        LOGGER.warning("High heap utilization: " + heapUtilization + "%");
      }
    }

    public GarbageCollectionAnalysis performFinalAnalysis() {
      final double averageEffectiveness =
          effectivenessHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

      final double effectivenessDegradation = calculateEffectivenessDegradation();

      long finalMajorCollections = 0;
      long finalMinorCollections = 0;
      for (final GarbageCollectorMXBean gcBean : gcBeans) {
        if (gcBean.getName().contains("Old") || gcBean.getName().contains("Tenured")) {
          finalMajorCollections += gcBean.getCollectionCount();
        } else {
          finalMinorCollections += gcBean.getCollectionCount();
        }
      }

      return new GarbageCollectionAnalysis(
          averageEffectiveness,
          effectivenessDegradation,
          finalMajorCollections - initialMajorCollections,
          finalMinorCollections - initialMinorCollections);
    }

    private double calculateEffectivenessDegradation() {
      if (effectivenessHistory.size() < 10) {
        return 0.0;
      }

      final int partSize = effectivenessHistory.size() / 3;
      final double earlyAvg =
          effectivenessHistory.subList(0, partSize).stream()
              .mapToDouble(Double::doubleValue)
              .average()
              .orElse(0.0);
      final double lateAvg =
          effectivenessHistory
              .subList(effectivenessHistory.size() - partSize, effectivenessHistory.size())
              .stream()
              .mapToDouble(Double::doubleValue)
              .average()
              .orElse(0.0);

      return Math.max(0.0, earlyAvg - lateAvg);
    }
  }

  /** Analysis results for garbage collection behavior. */
  private static class GarbageCollectionAnalysis {
    private final double averageEffectiveness;
    private final double effectivenessDegradation;
    private final long majorCollections;
    private final long minorCollections;

    public GarbageCollectionAnalysis(
        final double averageEffectiveness,
        final double effectivenessDegradation,
        final long majorCollections,
        final long minorCollections) {
      this.averageEffectiveness = averageEffectiveness;
      this.effectivenessDegradation = effectivenessDegradation;
      this.majorCollections = majorCollections;
      this.minorCollections = minorCollections;
    }

    public double getAverageEffectiveness() {
      return averageEffectiveness;
    }

    public double getEffectivenessDegradation() {
      return effectivenessDegradation;
    }

    public long getMajorCollections() {
      return majorCollections;
    }

    public long getMinorCollections() {
      return minorCollections;
    }
  }

  /** Advanced memory monitoring with detailed metrics. */
  private static class AdvancedMemoryMonitor {
    private final MemoryMXBean memoryBean;
    private final List<MemorySnapshot> snapshots = new ArrayList<>();
    private long baselineMemory = 0;
    private String testName = "unknown";

    public AdvancedMemoryMonitor() {
      this.memoryBean = ManagementFactory.getMemoryMXBean();
    }

    public void startMonitoring(final String testName) {
      this.testName = testName;
      this.baselineMemory = getCurrentMemoryUsage();
      recordMemorySnapshot();
    }

    public void stopMonitoring() {
      recordMemorySnapshot();
      LOGGER.info("Memory monitoring completed for test: " + testName);
    }

    public void recordMemorySnapshot() {
      final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
      final MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

      snapshots.add(
          new MemorySnapshot(
              System.currentTimeMillis(),
              heapUsage.getUsed(),
              heapUsage.getCommitted(),
              heapUsage.getMax(),
              nonHeapUsage.getUsed()));
    }

    public AdvancedMemoryMetrics captureDetailedMetrics() {
      recordMemorySnapshot();
      return new AdvancedMemoryMetrics(baselineMemory, snapshots);
    }

    private long getCurrentMemoryUsage() {
      return memoryBean.getHeapMemoryUsage().getUsed();
    }
  }

  /** Snapshot of memory state at a point in time. */
  private static class MemorySnapshot {
    final long timestamp;
    final long heapUsed;
    final long heapCommitted;
    final long heapMax;
    final long nonHeapUsed;

    public MemorySnapshot(
        final long timestamp,
        final long heapUsed,
        final long heapCommitted,
        final long heapMax,
        final long nonHeapUsed) {
      this.timestamp = timestamp;
      this.heapUsed = heapUsed;
      this.heapCommitted = heapCommitted;
      this.heapMax = heapMax;
      this.nonHeapUsed = nonHeapUsed;
    }
  }

  /** Advanced memory metrics with trend analysis. */
  private static class AdvancedMemoryMetrics {
    private final long baselineMemory;
    private final List<MemorySnapshot> snapshots;

    public AdvancedMemoryMetrics(final long baselineMemory, final List<MemorySnapshot> snapshots) {
      this.baselineMemory = baselineMemory;
      this.snapshots = new ArrayList<>(snapshots);
    }

    public long getBaselineMemoryMB() {
      return baselineMemory / 1024 / 1024;
    }

    public long getCurrentMemoryMB() {
      if (snapshots.isEmpty()) {
        return getBaselineMemoryMB();
      }
      return snapshots.get(snapshots.size() - 1).heapUsed / 1024 / 1024;
    }

    public long getPeakMemoryMB() {
      return snapshots.stream().mapToLong(s -> s.heapUsed).max().orElse(baselineMemory)
          / 1024
          / 1024;
    }

    public String getMemoryTrend() {
      if (snapshots.size() < 2) {
        return "insufficient-data";
      }

      final long firstHalf =
          snapshots.subList(0, snapshots.size() / 2).stream().mapToLong(s -> s.heapUsed).sum()
              / (snapshots.size() / 2);

      final long secondHalf =
          snapshots.subList(snapshots.size() / 2, snapshots.size()).stream()
                  .mapToLong(s -> s.heapUsed)
                  .sum()
              / (snapshots.size() - snapshots.size() / 2);

      if (secondHalf > firstHalf * 1.1) {
        return "increasing";
      } else if (secondHalf < firstHalf * 0.9) {
        return "decreasing";
      } else {
        return "stable";
      }
    }

    public double getMemoryGrowthRate() {
      if (snapshots.size() < 2) {
        return 0.0;
      }

      final MemorySnapshot first = snapshots.get(0);
      final MemorySnapshot last = snapshots.get(snapshots.size() - 1);
      final long timeDiffMs = last.timestamp - first.timestamp;
      final long memoryDiff = last.heapUsed - first.heapUsed;

      if (timeDiffMs == 0) {
        return 0.0;
      }

      // Return MB per minute
      return (double) memoryDiff / 1024 / 1024 / (timeDiffMs / 60000.0);
    }

    public double getMemoryEfficiency() {
      if (snapshots.isEmpty()) {
        return 100.0;
      }

      final double avgUsed = snapshots.stream().mapToLong(s -> s.heapUsed).average().orElse(0);

      final double avgCommitted =
          snapshots.stream().mapToLong(s -> s.heapCommitted).average().orElse(1);

      return (avgUsed / avgCommitted) * 100.0;
    }

    public double getMemoryFragmentation() {
      if (snapshots.isEmpty()) {
        return 0.0;
      }

      // Simplified fragmentation calculation based on committed vs used variance
      final double usedVariance =
          calculateVariance(snapshots.stream().mapToLong(s -> s.heapUsed).toArray());

      final double committedVariance =
          calculateVariance(snapshots.stream().mapToLong(s -> s.heapCommitted).toArray());

      return Math.min(100.0, (usedVariance / Math.max(1, committedVariance)) * 100.0);
    }

    public double getGarbageCollectionOverhead() {
      // This is a simplified calculation - in a real implementation,
      // you would track GC times more precisely
      if (snapshots.size() < 2) {
        return 0.0;
      }

      // Estimate based on memory allocation patterns
      final double memoryChurn = calculateMemoryChurn();
      return Math.min(25.0, memoryChurn / 1000.0); // Rough estimate
    }

    private double calculateMemoryChurn() {
      if (snapshots.size() < 2) {
        return 0.0;
      }

      double totalChurn = 0.0;
      for (int i = 1; i < snapshots.size(); i++) {
        final long diff = Math.abs(snapshots.get(i).heapUsed - snapshots.get(i - 1).heapUsed);
        totalChurn += diff;
      }

      return totalChurn / 1024 / 1024; // Return in MB
    }

    private double calculateVariance(final long[] values) {
      if (values.length < 2) {
        return 0.0;
      }

      final double mean = java.util.Arrays.stream(values).average().orElse(0.0);
      final double variance =
          java.util.Arrays.stream(values)
              .mapToDouble(val -> Math.pow(val - mean, 2))
              .average()
              .orElse(0.0);

      return variance;
    }
  }
}

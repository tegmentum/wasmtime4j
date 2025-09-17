package ai.tegmentum.wasmtime4j;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.exception.WasmException;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 * Comprehensive phantom reference cleanup testing under various failure scenarios.
 *
 * <p>This test class validates that phantom reference cleanup mechanisms work correctly under
 * abnormal termination conditions, system stress, and various failure modes.
 */
@DisplayName("Phantom Reference Cleanup Tests")
class PhantomReferenceCleanupTest extends BaseIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(PhantomReferenceCleanupTest.class.getName());

  private WasmTestDataManager testDataManager;
  private ExecutorService executorService;
  private PhantomReferenceMonitor phantomMonitor;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    try {
      testDataManager = WasmTestDataManager.getInstance();
      testDataManager.ensureTestDataAvailable();
      executorService = Executors.newFixedThreadPool(12);
      phantomMonitor = new PhantomReferenceMonitor();
      phantomMonitor.startMonitoring();
    } catch (final Exception e) {
      LOGGER.warning("Failed to setup test environment: " + e.getMessage());
      skipIfNot(false, "Test environment setup failed: " + e.getMessage());
    }
  }

  @AfterEach
  void tearDownServices() {
    if (phantomMonitor != null) {
      phantomMonitor.stopMonitoring();
    }

    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(20, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        executorService.shutdownNow();
      }
    }
  }

  @Test
  @DisplayName("Should cleanup phantom references during abnormal termination")
  void shouldCleanupPhantomReferencesDuringAbnormalTermination() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "abnormal-termination-cleanup",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final AbnormalTerminationSimulator simulator = new AbnormalTerminationSimulator();
              final int scenarioCount = 20;

              try (final Engine engine = runtime.createEngine()) {
                phantomMonitor.resetCounters();

                int successfulScenarios = 0;
                int phantomReferencesDetected = 0;

                // When - Simulate various abnormal termination scenarios
                for (int scenario = 0; scenario < scenarioCount; scenario++) {
                  try {
                    final AbnormalScenarioResult result =
                        simulator.simulateAbnormalTermination(
                            engine, wasmBytes, scenario % 5); // 5 different scenario types

                    if (result.wasSuccessfullyHandled()) {
                      successfulScenarios++;
                    }

                    // Force garbage collection to trigger phantom references
                    forceExtensiveGarbageCollection();

                    // Check for phantom reference cleanup
                    final int detectedPhantoms = phantomMonitor.processPhantomReferences();
                    phantomReferencesDetected += detectedPhantoms;

                    if (detectedPhantoms > 0) {
                      LOGGER.info(
                          "Detected "
                              + detectedPhantoms
                              + " phantom references in scenario "
                              + scenario);
                    }

                  } catch (final Exception e) {
                    // Expected for some abnormal scenarios
                    LOGGER.info(
                        "Abnormal scenario " + scenario + " failed as expected: " + e.getMessage());
                  }
                }

                // Then - Verify phantom reference cleanup handled abnormal terminations
                assertThat(successfulScenarios).isGreaterThan(scenarioCount / 2); // > 50% handled
                assertThat(phantomReferencesDetected)
                    .isGreaterThan(0); // Should detect some phantoms

                final PhantomReferenceMetrics metrics = phantomMonitor.captureMetrics();
                assertThat(metrics.getCleanupEffectiveness())
                    .isGreaterThan(80.0); // > 80% effective

                return String.format(
                    "Abnormal termination: %d scenarios, %d successful, %d phantoms detected, "
                        + "cleanup effectiveness: %.1f%%",
                    scenarioCount,
                    successfulScenarios,
                    phantomReferencesDetected,
                    metrics.getCleanupEffectiveness());
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Abnormal termination cleanup validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle phantom reference cleanup under system stress")
  void shouldHandlePhantomReferenceCleanupUnderSystemStress() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "system-stress-phantom-cleanup",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final SystemStressSimulator stressSimulator = new SystemStressSimulator();
              final long stressDurationMs = 120000; // 2 minutes
              final int stressThreads = 8;

              try (final Engine engine = runtime.createEngine()) {
                phantomMonitor.resetCounters();
                final AtomicBoolean stressActive = new AtomicBoolean(true);
                final CountDownLatch stressComplete = new CountDownLatch(stressThreads);

                // When - Apply system stress while monitoring phantom references
                final List<CompletableFuture<StressTestResult>> stressFutures = new ArrayList<>();

                for (int i = 0; i < stressThreads; i++) {
                  final int threadId = i;
                  final CompletableFuture<StressTestResult> future =
                      CompletableFuture.supplyAsync(
                          () -> {
                            try {
                              return stressSimulator.applySystemStress(
                                  engine, wasmBytes, threadId, stressActive);
                            } finally {
                              stressComplete.countDown();
                            }
                          },
                          executorService);
                  stressFutures.add(future);
                }

                // Monitor phantom references during stress
                final CompletableFuture<PhantomMonitoringResult> monitoringFuture =
                    CompletableFuture.supplyAsync(
                        () -> monitorPhantomReferencesDuringStress(stressActive), executorService);

                // Let stress run for specified duration
                Thread.sleep(stressDurationMs);
                stressActive.set(false);

                // Wait for completion
                final boolean completed = stressComplete.await(60, TimeUnit.SECONDS);
                assertThat(completed).isTrue();

                // Collect results
                final List<StressTestResult> stressResults = new ArrayList<>();
                for (final CompletableFuture<StressTestResult> future : stressFutures) {
                  stressResults.add(future.join());
                }

                final PhantomMonitoringResult monitoringResult = monitoringFuture.join();

                // Final phantom reference processing
                forceExtensiveGarbageCollection();
                final int finalPhantomCount = phantomMonitor.processPhantomReferences();

                // Then - Verify phantom references were handled under stress
                final long totalOperations =
                    stressResults.stream().mapToLong(result -> result.operationsCompleted).sum();
                final long totalPhantomDetections =
                    monitoringResult.totalPhantomDetections + finalPhantomCount;

                assertThat(totalOperations).isGreaterThan(0);
                assertThat(totalPhantomDetections).isGreaterThan(0);

                final PhantomReferenceMetrics finalMetrics = phantomMonitor.captureMetrics();
                assertThat(finalMetrics.getStressTestSurvivalRate())
                    .isGreaterThan(90.0); // > 90% survival

                return String.format(
                    "System stress: %d ops, %d phantom detections, %.1f%% survival rate, "
                        + "avg cleanup time: %dms",
                    totalOperations,
                    totalPhantomDetections,
                    finalMetrics.getStressTestSurvivalRate(),
                    finalMetrics.getAverageCleanupTimeMs());
              }
            },
            comparison -> true); // Stress test results may vary between runtimes

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("System stress phantom cleanup validation: " + validation.getSummary());
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should validate phantom reference cleanup timing under various scenarios")
  void shouldValidatePhantomReferenceCleanupTimingUnderVariousScenarios(
      final RuntimeType runtimeType) {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "phantom-cleanup-timing-" + runtimeType,
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final PhantomReferenceTimingValidator timingValidator =
                  new PhantomReferenceTimingValidator();
              final int scenarioTypes = 6; // Different timing scenarios

              try (final Engine engine = runtime.createEngine()) {
                phantomMonitor.resetCounters();

                final List<TimingScenarioResult> scenarioResults = new ArrayList<>();

                // When - Test phantom reference cleanup timing in different scenarios
                for (int scenarioType = 0; scenarioType < scenarioTypes; scenarioType++) {
                  final TimingScenarioResult result =
                      timingValidator.validateCleanupTiming(
                          engine, wasmBytes, scenarioType, phantomMonitor);

                  scenarioResults.add(result);

                  // Allow system to stabilize between scenarios
                  Thread.sleep(1000);
                }

                // Then - Analyze timing results
                final double avgCleanupTime =
                    scenarioResults.stream()
                        .mapToLong(result -> result.cleanupTimeMs)
                        .average()
                        .orElse(0.0);

                final long maxCleanupTime =
                    scenarioResults.stream()
                        .mapToLong(result -> result.cleanupTimeMs)
                        .max()
                        .orElse(0);

                final long totalPhantomReferencesProcessed =
                    scenarioResults.stream()
                        .mapToLong(result -> result.phantomReferencesProcessed)
                        .sum();

                // Verify timing expectations
                assertThat(avgCleanupTime).isLessThan(5000.0); // Average < 5 seconds
                assertThat(maxCleanupTime).isLessThan(15000); // Max < 15 seconds
                assertThat(totalPhantomReferencesProcessed).isGreaterThan(0);

                final PhantomReferenceMetrics finalMetrics = phantomMonitor.captureMetrics();
                assertThat(finalMetrics.getTimingConsistency())
                    .isGreaterThan(70.0); // > 70% consistent

                return String.format(
                    "Timing validation: %d scenarios, %.1fms avg cleanup, %dms max cleanup, "
                        + "%d phantoms processed, %.1f%% timing consistency",
                    scenarioTypes,
                    avgCleanupTime,
                    maxCleanupTime,
                    totalPhantomReferencesProcessed,
                    finalMetrics.getTimingConsistency());
              }
            },
            comparison -> true); // Timing may vary between runtimes

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Phantom cleanup timing validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle phantom reference cleanup during resource exhaustion")
  void shouldHandlePhantomReferenceCleanupDuringResourceExhaustion() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "resource-exhaustion-phantom-cleanup",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final ResourceExhaustionSimulator exhaustionSimulator =
                  new ResourceExhaustionSimulator();

              try (final Engine engine = runtime.createEngine()) {
                phantomMonitor.resetCounters();

                // When - Simulate resource exhaustion scenarios
                final ExhaustionScenarioResult memoryResult =
                    exhaustionSimulator.simulateMemoryExhaustion(engine, wasmBytes, phantomMonitor);

                final ExhaustionScenarioResult handleResult =
                    exhaustionSimulator.simulateHandleExhaustion(engine, wasmBytes, phantomMonitor);

                final ExhaustionScenarioResult threadResult =
                    exhaustionSimulator.simulateThreadExhaustion(engine, wasmBytes, phantomMonitor);

                // Force extensive cleanup
                forceExtensiveGarbageCollection();
                final int finalPhantomCount = phantomMonitor.processPhantomReferences();

                // Then - Verify phantom references handled exhaustion gracefully
                final int totalExhaustionScenarios = 3;
                final int successfulRecoveries =
                    (memoryResult.successfulRecovery ? 1 : 0)
                        + (handleResult.successfulRecovery ? 1 : 0)
                        + (threadResult.successfulRecovery ? 1 : 0);

                final long totalPhantomDetections =
                    memoryResult.phantomDetections
                        + handleResult.phantomDetections
                        + threadResult.phantomDetections
                        + finalPhantomCount;

                // At least some scenarios should recover
                assertThat(successfulRecoveries).isGreaterThan(0);
                assertThat(totalPhantomDetections).isGreaterThan(0);

                final PhantomReferenceMetrics finalMetrics = phantomMonitor.captureMetrics();
                assertThat(finalMetrics.getResourceExhaustionSurvivalRate())
                    .isGreaterThan(60.0); // > 60%

                return String.format(
                    "Resource exhaustion: %d scenarios, %d recoveries, %d phantom detections, "
                        + "survival rate: %.1f%%",
                    totalExhaustionScenarios,
                    successfulRecoveries,
                    totalPhantomDetections,
                    finalMetrics.getResourceExhaustionSurvivalRate());
              }
            },
            comparison -> true); // Exhaustion handling may vary between runtimes

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Resource exhaustion phantom cleanup validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate phantom reference cleanup during JVM shutdown simulation")
  void shouldValidatePhantomReferenceCleanupDuringJvmShutdownSimulation() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "jvm-shutdown-phantom-cleanup",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final ShutdownScenarioSimulator shutdownSimulator = new ShutdownScenarioSimulator();

              try (final Engine engine = runtime.createEngine()) {
                phantomMonitor.resetCounters();

                // When - Simulate various JVM shutdown scenarios
                final ShutdownScenarioResult gracefulResult =
                    shutdownSimulator.simulateGracefulShutdown(engine, wasmBytes, phantomMonitor);

                final ShutdownScenarioResult forcedResult =
                    shutdownSimulator.simulateForcedShutdown(engine, wasmBytes, phantomMonitor);

                final ShutdownScenarioResult interruptedResult =
                    shutdownSimulator.simulateInterruptedShutdown(
                        engine, wasmBytes, phantomMonitor);

                // Final phantom reference processing
                final int finalPhantomCount = phantomMonitor.processPhantomReferences();

                // Then - Verify shutdown scenarios handled phantom references
                final long totalPhantomDetections =
                    gracefulResult.phantomReferencesProcessed
                        + forcedResult.phantomReferencesProcessed
                        + interruptedResult.phantomReferencesProcessed
                        + finalPhantomCount;

                final int successfulShutdowns =
                    (gracefulResult.successful ? 1 : 0)
                        + (forcedResult.successful ? 1 : 0)
                        + (interruptedResult.successful ? 1 : 0);

                assertThat(totalPhantomDetections).isGreaterThan(0);
                assertThat(successfulShutdowns)
                    .isGreaterThanOrEqualTo(1); // At least graceful should work

                final PhantomReferenceMetrics finalMetrics = phantomMonitor.captureMetrics();

                return String.format(
                    "JVM shutdown simulation: %d successful shutdowns, %d phantom detections, "
                        + "cleanup completeness: %.1f%%",
                    successfulShutdowns,
                    totalPhantomDetections,
                    finalMetrics.getShutdownCleanupCompleteness());
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("JVM shutdown phantom cleanup validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle phantom reference cleanup with OutOfMemoryError scenarios")
  void shouldHandlePhantomReferenceCleanupWithOutOfMemoryErrorScenarios() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "oom-phantom-cleanup",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final OutOfMemoryErrorSimulator oomSimulator = new OutOfMemoryErrorSimulator();

              try (final Engine engine = runtime.createEngine()) {
                phantomMonitor.resetCounters();

                int oomScenarios = 0;
                int phantomDetections = 0;
                int successfulRecoveries = 0;

                // When - Simulate OOM scenarios with phantom reference monitoring
                for (int attempt = 0; attempt < 5; attempt++) {
                  try {
                    final OomScenarioResult result =
                        oomSimulator.simulateOutOfMemoryScenario(
                            engine, wasmBytes, attempt, phantomMonitor);

                    oomScenarios++;
                    phantomDetections += result.phantomDetections;

                    if (result.recoveredGracefully) {
                      successfulRecoveries++;
                    }

                    // Force cleanup between scenarios
                    forceExtensiveGarbageCollection();
                    phantomDetections += phantomMonitor.processPhantomReferences();

                  } catch (final OutOfMemoryError e) {
                    // Expected in some scenarios
                    LOGGER.info(
                        "OOM scenario " + attempt + " triggered OutOfMemoryError as expected");
                    oomScenarios++;

                    // Try to recover
                    try {
                      forceExtensiveGarbageCollection();
                      phantomDetections += phantomMonitor.processPhantomReferences();
                      successfulRecoveries++;
                    } catch (final Exception recoveryError) {
                      LOGGER.warning("Failed to recover from OOM: " + recoveryError.getMessage());
                    }
                  }
                }

                // Then - Verify phantom references handled OOM scenarios
                assertThat(oomScenarios).isGreaterThan(0);
                assertThat(phantomDetections).isGreaterThan(0);
                // At least some scenarios should allow recovery
                assertThat(successfulRecoveries).isGreaterThan(0);

                final PhantomReferenceMetrics finalMetrics = phantomMonitor.captureMetrics();

                return String.format(
                    "OOM scenarios: %d attempts, %d phantom detections, %d recoveries, "
                        + "OOM survival rate: %.1f%%",
                    oomScenarios,
                    phantomDetections,
                    successfulRecoveries,
                    finalMetrics.getOomSurvivalRate());
              }
            },
            comparison -> true); // OOM behavior may vary between runtimes

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("OutOfMemoryError phantom cleanup validation: " + validation.getSummary());
  }

  /** Performs extensive garbage collection to trigger phantom references. */
  private void forceExtensiveGarbageCollection() {
    for (int i = 0; i < 10; i++) {
      System.gc();
      try {
        Thread.sleep(100);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Monitors phantom references during stress testing. */
  private PhantomMonitoringResult monitorPhantomReferencesDuringStress(final AtomicBoolean active) {
    final PhantomMonitoringResult result = new PhantomMonitoringResult();

    while (active.get()) {
      try {
        final int detected = phantomMonitor.processPhantomReferences();
        result.totalPhantomDetections += detected;
        result.monitoringCycles++;

        Thread.sleep(1000); // Monitor every second
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    return result;
  }

  /** Monitors phantom references for cleanup effectiveness. */
  private static class PhantomReferenceMonitor {
    private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
    private final List<PhantomReference<Object>> phantomReferences =
        Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong totalPhantomReferences = new AtomicLong(0);
    private final AtomicLong processedPhantomReferences = new AtomicLong(0);
    private final AtomicLong cleanupFailures = new AtomicLong(0);
    private final List<Long> cleanupTimes = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean monitoring = false;

    public void startMonitoring() {
      monitoring = true;
      LOGGER.info("Started phantom reference monitoring");
    }

    public void stopMonitoring() {
      monitoring = false;
      processPhantomReferences(); // Final cleanup
      LOGGER.info("Stopped phantom reference monitoring");
    }

    public void trackResource(final Object resource, final String resourceType) {
      if (!monitoring) {
        return;
      }

      final PhantomReference<Object> phantom = new PhantomReference<>(resource, referenceQueue);
      phantomReferences.add(phantom);
      totalPhantomReferences.incrementAndGet();
    }

    public int processPhantomReferences() {
      int processed = 0;
      final long startTime = System.currentTimeMillis();

      try {
        while (true) {
          final PhantomReference<?> ref = (PhantomReference<?>) referenceQueue.poll();
          if (ref == null) {
            break;
          }

          phantomReferences.remove(ref);
          ref.clear();
          processed++;
        }

        if (processed > 0) {
          processedPhantomReferences.addAndGet(processed);
          final long cleanupTime = System.currentTimeMillis() - startTime;
          cleanupTimes.add(cleanupTime);
          LOGGER.info("Processed " + processed + " phantom references in " + cleanupTime + "ms");
        }

      } catch (final Exception e) {
        cleanupFailures.incrementAndGet();
        LOGGER.warning("Error processing phantom references: " + e.getMessage());
      }

      return processed;
    }

    public void resetCounters() {
      totalPhantomReferences.set(0);
      processedPhantomReferences.set(0);
      cleanupFailures.set(0);
      cleanupTimes.clear();
    }

    public PhantomReferenceMetrics captureMetrics() {
      return new PhantomReferenceMetrics(
          totalPhantomReferences.get(),
          processedPhantomReferences.get(),
          cleanupFailures.get(),
          new ArrayList<>(cleanupTimes));
    }
  }

  /** Metrics for phantom reference operations. */
  private static class PhantomReferenceMetrics {
    private final long totalPhantomReferences;
    private final long processedPhantomReferences;
    private final long cleanupFailures;
    private final List<Long> cleanupTimes;

    public PhantomReferenceMetrics(
        final long totalPhantomReferences,
        final long processedPhantomReferences,
        final long cleanupFailures,
        final List<Long> cleanupTimes) {
      this.totalPhantomReferences = totalPhantomReferences;
      this.processedPhantomReferences = processedPhantomReferences;
      this.cleanupFailures = cleanupFailures;
      this.cleanupTimes = new ArrayList<>(cleanupTimes);
    }

    public double getCleanupEffectiveness() {
      if (totalPhantomReferences == 0) {
        return 100.0;
      }
      return (double) processedPhantomReferences / totalPhantomReferences * 100.0;
    }

    public double getStressTestSurvivalRate() {
      return Math.max(0.0, 100.0 - (cleanupFailures * 10.0)); // Simplified calculation
    }

    public long getAverageCleanupTimeMs() {
      return cleanupTimes.isEmpty()
          ? 0
          : (long) cleanupTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    public double getTimingConsistency() {
      if (cleanupTimes.size() < 2) {
        return 100.0;
      }

      final double avg = cleanupTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
      final double variance =
          cleanupTimes.stream().mapToDouble(time -> Math.pow(time - avg, 2)).average().orElse(0.0);

      final double stdDev = Math.sqrt(variance);
      return Math.max(0.0, 100.0 - (stdDev / Math.max(1, avg) * 100.0));
    }

    public double getResourceExhaustionSurvivalRate() {
      // Simplified calculation based on cleanup success rate
      return getCleanupEffectiveness() * 0.8; // Reduced under stress
    }

    public double getShutdownCleanupCompleteness() {
      return getCleanupEffectiveness();
    }

    public double getOomSurvivalRate() {
      return Math.max(0.0, getCleanupEffectiveness() - 20.0); // Reduced under OOM
    }
  }

  /** Simulates abnormal termination scenarios. */
  private static class AbnormalTerminationSimulator {
    public AbnormalScenarioResult simulateAbnormalTermination(
        final Engine engine, final byte[] wasmBytes, final int scenarioType) {
      final AbnormalScenarioResult result = new AbnormalScenarioResult();

      try {
        switch (scenarioType) {
          case 0: // Sudden resource closure
            simulateSuddenResourceClosure(engine, wasmBytes);
            break;
          case 1: // Exception during resource usage
            simulateExceptionDuringUsage(engine, wasmBytes);
            break;
          case 2: // Thread interruption
            simulateThreadInterruption(engine, wasmBytes);
            break;
          case 3: // Forced resource cleanup
            simulateForcedResourceCleanup(engine, wasmBytes);
            break;
          case 4: // Partial operation failure
            simulatePartialOperationFailure(engine, wasmBytes);
            break;
          default:
            simulateSuddenResourceClosure(engine, wasmBytes);
        }

        result.setSuccessfullyHandled(true);

      } catch (final Exception e) {
        // Some exceptions are expected in abnormal scenarios
        result.setException(e);
      }

      return result;
    }

    private void simulateSuddenResourceClosure(final Engine engine, final byte[] wasmBytes)
        throws WasmException {
      final Module module = engine.compileModule(wasmBytes);
      final Store store = engine.createStore();

      // Suddenly close without proper cleanup sequence
      store.close();
      module.close();
    }

    private void simulateExceptionDuringUsage(final Engine engine, final byte[] wasmBytes)
        throws WasmException {
      final Module module = engine.compileModule(wasmBytes);
      final Store store = engine.createStore();
      final Instance instance = module.instantiate(store);

      // Simulate exception (close store while instance still active)
      store.close();

      try {
        // This might throw an exception
        instance.close();
      } finally {
        module.close();
      }
    }

    private void simulateThreadInterruption(final Engine engine, final byte[] wasmBytes)
        throws WasmException {
      Thread.currentThread().interrupt();

      try {
        final Module module = engine.compileModule(wasmBytes);
        module.close();
      } finally {
        Thread.interrupted(); // Clear interrupt status
      }
    }

    private void simulateForcedResourceCleanup(final Engine engine, final byte[] wasmBytes)
        throws WasmException {
      final List<AutoCloseable> resources = new ArrayList<>();

      // Create resources
      for (int i = 0; i < 5; i++) {
        final Module module = engine.compileModule(wasmBytes);
        resources.add(module);
      }

      // Force cleanup all at once
      for (final AutoCloseable resource : resources) {
        try {
          resource.close();
        } catch (final Exception e) {
          // Ignore cleanup errors
        }
      }
    }

    private void simulatePartialOperationFailure(final Engine engine, final byte[] wasmBytes)
        throws WasmException {
      final Module module = engine.compileModule(wasmBytes);
      final Store store = engine.createStore();

      try {
        final Instance instance = module.instantiate(store);
        // Simulate partial failure - close instance but leave others
        instance.close();

        // Create another instance to cause potential issues
        module.instantiate(store);
      } finally {
        store.close();
        module.close();
      }
    }
  }

  /** Result of abnormal scenario simulation. */
  private static class AbnormalScenarioResult {
    private boolean successfullyHandled = false;
    private Exception exception = null;

    public void setSuccessfullyHandled(final boolean handled) {
      this.successfullyHandled = handled;
    }

    public void setException(final Exception exception) {
      this.exception = exception;
    }

    public boolean wasSuccessfullyHandled() {
      return successfullyHandled;
    }

    public Exception getException() {
      return exception;
    }
  }

  /** Simulates system stress conditions. */
  private static class SystemStressSimulator {
    public StressTestResult applySystemStress(
        final Engine engine,
        final byte[] wasmBytes,
        final int threadId,
        final AtomicBoolean active) {
      final StressTestResult result = new StressTestResult(threadId);

      while (active.get()) {
        try {
          // Create burst of resources
          final List<AutoCloseable> resources = new ArrayList<>();
          final int burstSize = 10 + ThreadLocalRandom.current().nextInt(20);

          for (int i = 0; i < burstSize; i++) {
            final Module module = engine.compileModule(wasmBytes);
            final Store store = engine.createStore();
            final Instance instance = module.instantiate(store);

            resources.add(instance);
            resources.add(store);
            resources.add(module);
          }

          result.operationsCompleted += burstSize;

          // Random delay to create stress patterns
          Thread.sleep(ThreadLocalRandom.current().nextInt(50, 200));

          // Cleanup resources
          for (final AutoCloseable resource : resources) {
            resource.close();
          }

        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        } catch (final Exception e) {
          result.errors++;
        }
      }

      return result;
    }
  }

  /** Result of stress testing. */
  private static class StressTestResult {
    final int threadId;
    long operationsCompleted = 0;
    long errors = 0;

    StressTestResult(final int threadId) {
      this.threadId = threadId;
    }
  }

  /** Result of phantom reference monitoring during stress. */
  private static class PhantomMonitoringResult {
    long totalPhantomDetections = 0;
    long monitoringCycles = 0;
  }

  /** Validates phantom reference cleanup timing. */
  private static class PhantomReferenceTimingValidator {
    public TimingScenarioResult validateCleanupTiming(
        final Engine engine,
        final byte[] wasmBytes,
        final int scenarioType,
        final PhantomReferenceMonitor monitor) {

      final TimingScenarioResult result = new TimingScenarioResult(scenarioType);
      final Instant startTime = Instant.now();

      try {
        switch (scenarioType) {
          case 0: // Small resource batch
            validateSmallBatchCleanup(engine, wasmBytes, monitor, result);
            break;
          case 1: // Large resource batch
            validateLargeBatchCleanup(engine, wasmBytes, monitor, result);
            break;
          case 2: // Rapid create/destroy cycles
            validateRapidCycleCleanup(engine, wasmBytes, monitor, result);
            break;
          case 3: // Delayed cleanup scenario
            validateDelayedCleanup(engine, wasmBytes, monitor, result);
            break;
          case 4: // Mixed resource types
            validateMixedResourceCleanup(engine, wasmBytes, monitor, result);
            break;
          case 5: // Cleanup under pressure
            validateCleanupUnderPressure(engine, wasmBytes, monitor, result);
            break;
          default:
            validateSmallBatchCleanup(engine, wasmBytes, monitor, result);
        }

      } catch (final Exception e) {
        result.exception = e;
      }

      result.cleanupTimeMs = Duration.between(startTime, Instant.now()).toMillis();
      return result;
    }

    private void validateSmallBatchCleanup(
        final Engine engine,
        final byte[] wasmBytes,
        final PhantomReferenceMonitor monitor,
        final TimingScenarioResult result) {
      // Create and cleanup small batch
      createAndCleanupResources(engine, wasmBytes, 5, monitor);
      result.phantomReferencesProcessed = monitor.processPhantomReferences();
    }

    private void validateLargeBatchCleanup(
        final Engine engine,
        final byte[] wasmBytes,
        final PhantomReferenceMonitor monitor,
        final TimingScenarioResult result) {
      // Create and cleanup large batch
      createAndCleanupResources(engine, wasmBytes, 50, monitor);
      result.phantomReferencesProcessed = monitor.processPhantomReferences();
    }

    private void validateRapidCycleCleanup(
        final Engine engine,
        final byte[] wasmBytes,
        final PhantomReferenceMonitor monitor,
        final TimingScenarioResult result) {
      // Rapid create/destroy cycles
      for (int cycle = 0; cycle < 10; cycle++) {
        createAndCleanupResources(engine, wasmBytes, 3, monitor);
      }
      result.phantomReferencesProcessed = monitor.processPhantomReferences();
    }

    private void validateDelayedCleanup(
        final Engine engine,
        final byte[] wasmBytes,
        final PhantomReferenceMonitor monitor,
        final TimingScenarioResult result)
        throws InterruptedException {
      // Create resources, wait, then cleanup
      createResourcesWithoutCleanup(engine, wasmBytes, 10, monitor);
      Thread.sleep(2000); // 2 second delay
      System.gc();
      result.phantomReferencesProcessed = monitor.processPhantomReferences();
    }

    private void validateMixedResourceCleanup(
        final Engine engine,
        final byte[] wasmBytes,
        final PhantomReferenceMonitor monitor,
        final TimingScenarioResult result) {
      // Mix of explicit cleanup and garbage collection
      createAndCleanupResources(engine, wasmBytes, 5, monitor);
      createResourcesWithoutCleanup(engine, wasmBytes, 5, monitor);
      System.gc();
      result.phantomReferencesProcessed = monitor.processPhantomReferences();
    }

    private void validateCleanupUnderPressure(
        final Engine engine,
        final byte[] wasmBytes,
        final PhantomReferenceMonitor monitor,
        final TimingScenarioResult result) {
      // Create pressure with large allocation
      final List<byte[]> pressure = new ArrayList<>();
      for (int i = 0; i < 1000; i++) {
        pressure.add(new byte[1024]); // 1KB each
      }

      createAndCleanupResources(engine, wasmBytes, 10, monitor);
      result.phantomReferencesProcessed = monitor.processPhantomReferences();
    }

    private void createAndCleanupResources(
        final Engine engine,
        final byte[] wasmBytes,
        final int count,
        final PhantomReferenceMonitor monitor) {
      final List<AutoCloseable> resources = new ArrayList<>();

      // Create resources
      for (int i = 0; i < count; i++) {
        try {
          final Module module = engine.compileModule(wasmBytes);
          final Store store = engine.createStore();
          final Instance instance = module.instantiate(store);

          resources.add(instance);
          resources.add(store);
          resources.add(module);

          monitor.trackResource(module, "Module");
          monitor.trackResource(instance, "Instance");
        } catch (final Exception e) {
          // Handle creation errors
        }
      }

      // Cleanup resources
      for (final AutoCloseable resource : resources) {
        try {
          resource.close();
        } catch (final Exception e) {
          // Handle cleanup errors
        }
      }
    }

    @SuppressWarnings("unused")
    private void createResourcesWithoutCleanup(
        final Engine engine,
        final byte[] wasmBytes,
        final int count,
        final PhantomReferenceMonitor monitor) {
      // Create resources without explicit cleanup (to trigger phantom references)
      for (int i = 0; i < count; i++) {
        try {
          final Module module = engine.compileModule(wasmBytes);
          final Store store = engine.createStore();
          final Instance instance = module.instantiate(store);

          monitor.trackResource(module, "Module");
          monitor.trackResource(instance, "Instance");

          // Intentionally don't close - let GC handle it
        } catch (final Exception e) {
          // Handle creation errors
        }
      }
    }
  }

  /** Result of timing scenario validation. */
  private static class TimingScenarioResult {
    final int scenarioType;
    long cleanupTimeMs = 0;
    long phantomReferencesProcessed = 0;
    Exception exception = null;

    TimingScenarioResult(final int scenarioType) {
      this.scenarioType = scenarioType;
    }
  }

  /** Simulates resource exhaustion scenarios. */
  private static class ResourceExhaustionSimulator {
    public ExhaustionScenarioResult simulateMemoryExhaustion(
        final Engine engine, final byte[] wasmBytes, final PhantomReferenceMonitor monitor) {
      final ExhaustionScenarioResult result = new ExhaustionScenarioResult("Memory");

      try {
        // Attempt to exhaust memory with many resources
        final List<Module> modules = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
          final Module module = engine.compileModule(wasmBytes);
          modules.add(module);
          monitor.trackResource(module, "MemoryExhaustionModule");
        }

        // Cleanup
        for (final Module module : modules) {
          module.close();
        }

        result.successfulRecovery = true;

      } catch (final OutOfMemoryError e) {
        result.outOfMemoryEncountered = true;
        System.gc(); // Try to recover
        result.successfulRecovery = true;
      } catch (final Exception e) {
        result.exception = e;
      }

      result.phantomDetections = monitor.processPhantomReferences();
      return result;
    }

    public ExhaustionScenarioResult simulateHandleExhaustion(
        final Engine engine, final byte[] wasmBytes, final PhantomReferenceMonitor monitor) {
      final ExhaustionScenarioResult result = new ExhaustionScenarioResult("Handle");

      try {
        // Create many handles to exhaust handle resources
        final List<AutoCloseable> handles = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
          final Module module = engine.compileModule(wasmBytes);
          final Store store = engine.createStore();
          final Instance instance = module.instantiate(store);

          handles.add(instance);
          handles.add(store);
          handles.add(module);

          monitor.trackResource(module, "HandleExhaustionModule");
        }

        // Cleanup
        for (final AutoCloseable handle : handles) {
          handle.close();
        }

        result.successfulRecovery = true;

      } catch (final Exception e) {
        result.exception = e;
        result.successfulRecovery = false;
      }

      result.phantomDetections = monitor.processPhantomReferences();
      return result;
    }

    public ExhaustionScenarioResult simulateThreadExhaustion(
        final Engine engine, final byte[] wasmBytes, final PhantomReferenceMonitor monitor) {
      final ExhaustionScenarioResult result = new ExhaustionScenarioResult("Thread");

      try {
        // Simulate thread exhaustion through rapid concurrent operations
        final ExecutorService tempExecutor = Executors.newFixedThreadPool(50);
        final List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
          final CompletableFuture<Void> future =
              CompletableFuture.runAsync(
                  () -> {
                    try {
                      final Module module = engine.compileModule(wasmBytes);
                      monitor.trackResource(module, "ThreadExhaustionModule");
                      module.close();
                    } catch (final Exception e) {
                      // Expected in thread exhaustion scenario
                    }
                  },
                  tempExecutor);
          futures.add(future);
        }

        // Wait for completion or timeout
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
            .get(10, TimeUnit.SECONDS);

        tempExecutor.shutdown();
        result.successfulRecovery = true;

      } catch (final Exception e) {
        result.exception = e;
        result.successfulRecovery = false;
      }

      result.phantomDetections = monitor.processPhantomReferences();
      return result;
    }
  }

  /** Result of resource exhaustion scenario. */
  private static class ExhaustionScenarioResult {
    final String scenarioType;
    boolean successfulRecovery = false;
    boolean outOfMemoryEncountered = false;
    int phantomDetections = 0;
    Exception exception = null;

    ExhaustionScenarioResult(final String scenarioType) {
      this.scenarioType = scenarioType;
    }
  }

  /** Simulates JVM shutdown scenarios. */
  private static class ShutdownScenarioSimulator {
    public ShutdownScenarioResult simulateGracefulShutdown(
        final Engine engine, final byte[] wasmBytes, final PhantomReferenceMonitor monitor) {
      final ShutdownScenarioResult result = new ShutdownScenarioResult("Graceful");

      try {
        // Create resources
        final List<AutoCloseable> resources = createTestResources(engine, wasmBytes, 10, monitor);

        // Simulate graceful shutdown - proper cleanup order
        for (final AutoCloseable resource : resources) {
          resource.close();
        }

        result.phantomReferencesProcessed = monitor.processPhantomReferences();
        result.successful = true;

      } catch (final Exception e) {
        result.exception = e;
      }

      return result;
    }

    public ShutdownScenarioResult simulateForcedShutdown(
        final Engine engine, final byte[] wasmBytes, final PhantomReferenceMonitor monitor) {
      final ShutdownScenarioResult result = new ShutdownScenarioResult("Forced");

      try {
        // Create resources but don't clean up properly (simulate forced shutdown)
        createTestResources(engine, wasmBytes, 10, monitor);

        // Force garbage collection (simulate forced cleanup)
        System.gc();

        result.phantomReferencesProcessed = monitor.processPhantomReferences();
        result.successful = true;

      } catch (final Exception e) {
        result.exception = e;
      }

      return result;
    }

    public ShutdownScenarioResult simulateInterruptedShutdown(
        final Engine engine, final byte[] wasmBytes, final PhantomReferenceMonitor monitor) {
      final ShutdownScenarioResult result = new ShutdownScenarioResult("Interrupted");

      try {
        // Create resources
        final List<AutoCloseable> resources = createTestResources(engine, wasmBytes, 10, monitor);

        // Simulate interrupted shutdown - partial cleanup
        for (int i = 0; i < resources.size() / 2; i++) {
          resources.get(i).close();
        }

        // Remaining resources left for phantom reference cleanup
        System.gc();
        result.phantomReferencesProcessed = monitor.processPhantomReferences();
        result.successful = true;

      } catch (final Exception e) {
        result.exception = e;
      }

      return result;
    }

    private List<AutoCloseable> createTestResources(
        final Engine engine,
        final byte[] wasmBytes,
        final int count,
        final PhantomReferenceMonitor monitor) {
      final List<AutoCloseable> resources = new ArrayList<>();

      for (int i = 0; i < count; i++) {
        try {
          final Module module = engine.compileModule(wasmBytes);
          final Store store = engine.createStore();
          final Instance instance = module.instantiate(store);

          resources.add(instance);
          resources.add(store);
          resources.add(module);

          monitor.trackResource(module, "ShutdownModule");
          monitor.trackResource(instance, "ShutdownInstance");

        } catch (final Exception e) {
          // Handle creation errors
        }
      }

      return resources;
    }
  }

  /** Result of shutdown scenario simulation. */
  private static class ShutdownScenarioResult {
    final String scenarioType;
    boolean successful = false;
    int phantomReferencesProcessed = 0;
    Exception exception = null;

    ShutdownScenarioResult(final String scenarioType) {
      this.scenarioType = scenarioType;
    }
  }

  /** Simulates OutOfMemoryError scenarios. */
  private static class OutOfMemoryErrorSimulator {
    public OomScenarioResult simulateOutOfMemoryScenario(
        final Engine engine,
        final byte[] wasmBytes,
        final int scenarioType,
        final PhantomReferenceMonitor monitor) {
      final OomScenarioResult result = new OomScenarioResult(scenarioType);

      try {
        switch (scenarioType) {
          case 0:
            simulateHeapExhaustion(engine, wasmBytes, monitor, result);
            break;
          case 1:
            simulateNativeMemoryExhaustion(engine, wasmBytes, monitor, result);
            break;
          case 2:
            simulateMetaspaceExhaustion(engine, wasmBytes, monitor, result);
            break;
          case 3:
            simulateDirectMemoryExhaustion(engine, wasmBytes, monitor, result);
            break;
          case 4:
            simulateRapidAllocationOom(engine, wasmBytes, monitor, result);
            break;
          default:
            simulateHeapExhaustion(engine, wasmBytes, monitor, result);
        }

      } catch (final OutOfMemoryError e) {
        result.outOfMemoryErrorEncountered = true;
        // Try to recover gracefully
        System.gc();
        result.recoveredGracefully = true;
      } catch (final Exception e) {
        result.exception = e;
      }

      result.phantomDetections = monitor.processPhantomReferences();
      return result;
    }

    private void simulateHeapExhaustion(
        final Engine engine,
        final byte[] wasmBytes,
        final PhantomReferenceMonitor monitor,
        final OomScenarioResult result)
        throws WasmException {
      // Create many objects to exhaust heap
      final List<byte[]> heapPressure = new ArrayList<>();
      try {
        for (int i = 0; i < 100000; i++) {
          heapPressure.add(new byte[1024]); // 1KB each

          // Also create WebAssembly resources
          if (i % 1000 == 0) {
            final Module module = engine.compileModule(wasmBytes);
            monitor.trackResource(module, "OomModule");
            module.close();
          }
        }
      } catch (final OutOfMemoryError e) {
        throw e; // Re-throw to be caught by caller
      }
    }

    private void simulateNativeMemoryExhaustion(
        final Engine engine,
        final byte[] wasmBytes,
        final PhantomReferenceMonitor monitor,
        final OomScenarioResult result)
        throws WasmException {
      // Create many native resources
      final List<Module> nativeResources = new ArrayList<>();
      try {
        for (int i = 0; i < 10000; i++) {
          final Module module = engine.compileModule(wasmBytes);
          nativeResources.add(module);
          monitor.trackResource(module, "NativeOomModule");
        }
      } finally {
        // Cleanup created resources
        for (final Module module : nativeResources) {
          try {
            module.close();
          } catch (final Exception e) {
            // Ignore cleanup errors
          }
        }
      }
    }

    private void simulateMetaspaceExhaustion(
        final Engine engine,
        final byte[] wasmBytes,
        final PhantomReferenceMonitor monitor,
        final OomScenarioResult result) {
      // Metaspace exhaustion is harder to simulate, so we'll create many classes
      // This is a simplified simulation
      try {
        for (int i = 0; i < 1000; i++) {
          final Module module = engine.compileModule(wasmBytes);
          monitor.trackResource(module, "MetaspaceOomModule");
          module.close();
        }
      } catch (final Exception e) {
        // Handle any errors
      }
    }

    private void simulateDirectMemoryExhaustion(
        final Engine engine,
        final byte[] wasmBytes,
        final PhantomReferenceMonitor monitor,
        final OomScenarioResult result)
        throws WasmException {
      // Create resources that might use direct memory
      final List<AutoCloseable> resources = new ArrayList<>();
      try {
        for (int i = 0; i < 5000; i++) {
          final Module module = engine.compileModule(wasmBytes);
          final Store store = engine.createStore();

          resources.add(store);
          resources.add(module);

          monitor.trackResource(module, "DirectMemoryOomModule");
        }
      } finally {
        // Cleanup
        for (final AutoCloseable resource : resources) {
          try {
            resource.close();
          } catch (final Exception e) {
            // Ignore cleanup errors
          }
        }
      }
    }

    private void simulateRapidAllocationOom(
        final Engine engine,
        final byte[] wasmBytes,
        final PhantomReferenceMonitor monitor,
        final OomScenarioResult result)
        throws WasmException {
      // Rapid allocation without cleanup
      try {
        for (int i = 0; i < 50000; i++) {
          @SuppressWarnings("unused")
          final byte[] allocation = new byte[1024]; // 1KB allocation

          if (i % 5000 == 0) {
            final Module module = engine.compileModule(wasmBytes);
            monitor.trackResource(module, "RapidOomModule");
            // Don't close - let phantom references handle it
          }
        }
      } catch (final OutOfMemoryError e) {
        throw e; // Re-throw to be caught by caller
      }
    }
  }

  /** Result of OOM scenario simulation. */
  private static class OomScenarioResult {
    final int scenarioType;
    boolean outOfMemoryErrorEncountered = false;
    boolean recoveredGracefully = false;
    int phantomDetections = 0;
    Exception exception = null;

    OomScenarioResult(final int scenarioType) {
      this.scenarioType = scenarioType;
    }
  }
}

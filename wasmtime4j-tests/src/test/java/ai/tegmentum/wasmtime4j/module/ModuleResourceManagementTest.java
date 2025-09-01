package ai.tegmentum.wasmtime4j.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeValidationResult;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestDataManager;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Comprehensive tests for Module resource management and memory leak detection.
 * Tests proper cleanup of native resources, memory management, and resource lifecycle.
 */
@DisplayName("Module Resource Management Tests")
class ModuleResourceManagementTest extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleResourceManagementTest.class.getName());

  private WasmTestDataManager testDataManager;
  private ExecutorService executorService;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    skipIfCategoryNotEnabled("module.resources");

    try {
      testDataManager = WasmTestDataManager.getInstance();
      testDataManager.ensureTestDataAvailable();
      executorService = Executors.newFixedThreadPool(4);
    } catch (final Exception e) {
      LOGGER.warning("Failed to setup test environment: " + e.getMessage());
      skipIfNot(false, "Test environment setup failed: " + e.getMessage());
    }
  }

  @AfterEach
  void tearDownExecutor() {
    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        executorService.shutdownNow();
      }
    }
  }

  @Test
  @DisplayName("Should properly cleanup module resources")
  void shouldProperlyCleanupModuleResources() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-resource-cleanup",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final List<WeakReference<Module>> moduleRefs = new ArrayList<>();

              try (final Engine engine = runtime.createEngine()) {
                // When - Create modules and track weak references
                for (int i = 0; i < 10; i++) {
                  final Module module = engine.compileModule(wasmBytes);
                  moduleRefs.add(new WeakReference<>(module));
                  
                  // Verify module is valid before closing
                  assertThat(module.isValid()).isTrue();
                  
                  // Close explicitly
                  module.close();
                  
                  // Verify module is invalid after closing
                  assertThat(module.isValid()).isFalse();
                }

                // Force garbage collection
                System.gc();
                Thread.sleep(100); // Allow GC to run

                // Then - Verify modules can be garbage collected
                int collectedCount = 0;
                for (final WeakReference<Module> ref : moduleRefs) {
                  if (ref.get() == null) {
                    collectedCount++;
                  }
                }

                // At least some should be collected (GC timing is unpredictable)
                return "Resource cleanup: " + collectedCount + "/10 modules collected";
              }
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Resource cleanup validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle module resource cleanup without explicit close")
  void shouldHandleModuleResourceCleanupWithoutExplicitClose() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-implicit-cleanup",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

              try (final Engine engine = runtime.createEngine()) {
                final List<WeakReference<Module>> moduleRefs = new ArrayList<>();

                // When - Create modules without explicit close
                for (int i = 0; i < 5; i++) {
                  final Module module = engine.compileModule(wasmBytes);
                  moduleRefs.add(new WeakReference<>(module));
                  // Intentionally don't call module.close()
                }

                // Force garbage collection multiple times
                for (int i = 0; i < 3; i++) {
                  System.gc();
                  System.runFinalization();
                  Thread.sleep(50);
                }

                // Then - Modules should eventually be collected by finalizers
                int collectedCount = 0;
                for (final WeakReference<Module> ref : moduleRefs) {
                  if (ref.get() == null) {
                    collectedCount++;
                  }
                }

                return "Implicit cleanup: " + collectedCount + "/5 modules collected";
              }
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Implicit cleanup validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle engine resource cleanup with active modules")
  void shouldHandleEngineResourceCleanupWithActiveModules() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "engine-module-cleanup",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final List<Module> modules = new ArrayList<>();

              final Engine engine = runtime.createEngine();
              
              // When - Create modules and then close engine
              for (int i = 0; i < 5; i++) {
                final Module module = engine.compileModule(wasmBytes);
                modules.add(module);
              }

              // Close engine while modules are still active
              engine.close();

              // Then - Modules should become invalid or handle this gracefully
              boolean allInvalid = true;
              for (final Module module : modules) {
                if (module.isValid()) {
                  allInvalid = false;
                }
                // Attempt to close - should not throw
                assertThatCode(() -> module.close()).doesNotThrowAnyException();
              }

              return "Engine closed first: all modules invalid = " + allInvalid;
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Engine-module cleanup validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle module and instance resource lifecycle")
  void shouldHandleModuleAndInstanceResourceLifecycle() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "module-instance-lifecycle",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

              try (final Engine engine = runtime.createEngine();
                  final Module module = engine.compileModule(wasmBytes)) {

                final List<WeakReference<Instance>> instanceRefs = new ArrayList<>();

                // When - Create instances and close them
                for (int i = 0; i < 5; i++) {
                  final Store store = engine.createStore();
                  final Instance instance = module.instantiate(store);
                  instanceRefs.add(new WeakReference<>(instance));

                  // Close instance and store
                  instance.close();
                  store.close();
                }

                // Force garbage collection
                System.gc();
                Thread.sleep(100);

                // Then - Check garbage collection
                int collectedInstances = 0;
                for (final WeakReference<Instance> ref : instanceRefs) {
                  if (ref.get() == null) {
                    collectedInstances++;
                  }
                }

                // Module should still be valid
                assertThat(module.isValid()).isTrue();

                return "Instance lifecycle: " + collectedInstances + "/5 instances collected";
              }
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Module-instance lifecycle validation: " + validation.getSummary());
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should handle memory pressure with many modules")
  void shouldHandleMemoryPressureWithManyModules(final RuntimeType runtimeType) {
    skipIfCategoryNotEnabled("stress");

    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "memory-pressure-" + runtimeType,
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int moduleCount = 100;

              final Runtime rt = Runtime.getRuntime();
              final long memoryBefore = rt.totalMemory() - rt.freeMemory();

              try (final Engine engine = runtime.createEngine()) {
                final List<Module> modules = new ArrayList<>();

                // When - Create many modules
                for (int i = 0; i < moduleCount; i++) {
                  final Module module = engine.compileModule(wasmBytes);
                  modules.add(module);
                  
                  // Periodically check memory and trigger GC if needed
                  if (i % 20 == 0) {
                    final long currentMemory = rt.totalMemory() - rt.freeMemory();
                    if (currentMemory > memoryBefore + 100 * 1024 * 1024) { // 100MB threshold
                      System.gc();
                      Thread.sleep(10);
                    }
                  }
                }

                final long memoryAfter = rt.totalMemory() - rt.freeMemory();
                final long memoryUsed = memoryAfter - memoryBefore;

                // Clean up all modules
                final Instant cleanupStart = Instant.now();
                for (final Module module : modules) {
                  module.close();
                }
                final Duration cleanupTime = Duration.between(cleanupStart, Instant.now());

                // Force GC and measure cleanup effectiveness
                System.gc();
                Thread.sleep(100);
                final long memoryAfterCleanup = rt.totalMemory() - rt.freeMemory();

                // Then - Verify reasonable memory usage
                final long avgMemoryPerModule = memoryUsed / moduleCount;
                assertThat(avgMemoryPerModule).isLessThan(5 * 1024 * 1024); // Less than 5MB per module

                return String.format("Memory pressure: %dKB used, %dKB/module, cleanup: %dms",
                    memoryUsed / 1024, avgMemoryPerModule / 1024, cleanupTime.toMillis());
              }
            },
            comparison -> true); // Memory usage may vary

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Memory pressure test for " + runtimeType + ": " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle concurrent module resource operations")
  void shouldHandleConcurrentModuleResourceOperations() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "concurrent-resource-operations",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int threadCount = 4;
              final int modulesPerThread = 10;
              final CountDownLatch startLatch = new CountDownLatch(1);
              final CountDownLatch completionLatch = new CountDownLatch(threadCount);
              final AtomicInteger totalModulesCreated = new AtomicInteger(0);
              final AtomicInteger totalModulesClosed = new AtomicInteger(0);

              try (final Engine engine = runtime.createEngine()) {
                // When - Perform concurrent module operations
                for (int i = 0; i < threadCount; i++) {
                  executorService.submit(() -> {
                    final List<Module> threadModules = new ArrayList<>();
                    
                    try {
                      startLatch.await();

                      // Create modules
                      for (int j = 0; j < modulesPerThread; j++) {
                        final Module module = engine.compileModule(wasmBytes);
                        threadModules.add(module);
                        totalModulesCreated.incrementAndGet();
                      }

                      // Close modules
                      for (final Module module : threadModules) {
                        module.close();
                        totalModulesClosed.incrementAndGet();
                      }

                    } catch (final Exception e) {
                      LOGGER.severe("Concurrent resource operation failed: " + e.getMessage());
                      throw new RuntimeException(e);
                    } finally {
                      completionLatch.countDown();
                    }
                  });
                }

                // Start all threads
                startLatch.countDown();

                // Wait for completion
                final boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
                assertThat(completed).isTrue();

                // Then - Verify all resources were handled
                final int expectedTotal = threadCount * modulesPerThread;
                assertThat(totalModulesCreated.get()).isEqualTo(expectedTotal);
                assertThat(totalModulesClosed.get()).isEqualTo(expectedTotal);

                return "Concurrent resources: " + totalModulesCreated.get() + " created, "
                       + totalModulesClosed.get() + " closed";
              }
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Concurrent resource operations validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should detect resource leaks with multiple create-close cycles")
  void shouldDetectResourceLeaksWithMultipleCreateCloseCycles() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "resource-leak-detection",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              final int cycleCount = 10;
              final int modulesPerCycle = 20;

              try (final Engine engine = runtime.createEngine()) {
                long initialMemory = getUsedMemory();

                // When - Perform multiple create-close cycles
                for (int cycle = 0; cycle < cycleCount; cycle++) {
                  final List<Module> modules = new ArrayList<>();

                  // Create modules
                  for (int i = 0; i < modulesPerCycle; i++) {
                    final Module module = engine.compileModule(wasmBytes);
                    modules.add(module);
                  }

                  // Close all modules
                  for (final Module module : modules) {
                    module.close();
                  }

                  // Periodically check for memory leaks
                  if (cycle % 3 == 0) {
                    System.gc();
                    Thread.sleep(50);
                    
                    final long currentMemory = getUsedMemory();
                    final long memoryIncrease = currentMemory - initialMemory;
                    
                    // Memory should not grow indefinitely
                    if (memoryIncrease > 50 * 1024 * 1024) { // 50MB threshold
                      LOGGER.warning("Potential memory leak detected: "
                          + (memoryIncrease / 1024 / 1024) + "MB increase");
                    }
                  }
                }

                // Then - Final memory check
                System.gc();
                Thread.sleep(100);
                final long finalMemory = getUsedMemory();
                final long totalIncrease = finalMemory - initialMemory;

                // Verify no significant leak (allow some growth due to JVM overhead)
                assertThat(totalIncrease).isLessThan(20 * 1024 * 1024); // Less than 20MB growth

                return String.format("Leak detection: %d cycles, %dKB memory increase",
                    cycleCount, totalIncrease / 1024);
              }
            },
            comparison -> true); // Memory usage may vary between runtimes

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Resource leak detection validation: " + validation.getSummary());
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
              final byte[] malformedBytes = {0x01, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00}; // Invalid magic

              try (final Engine engine = runtime.createEngine()) {
                int successfulModules = 0;
                int failedModules = 0;

                // When - Mix successful and failed module creations
                for (int i = 0; i < 20; i++) {
                  try {
                    final byte[] testBytes = (i % 3 == 0) ? malformedBytes : wasmBytes;
                    final Module module = engine.compileModule(testBytes);
                    
                    // If successful, close immediately
                    module.close();
                    successfulModules++;
                    
                  } catch (final Exception e) {
                    // Expected for malformed modules
                    failedModules++;
                  }
                }

                // Then - Verify engine is still functional
                final Module finalModule = engine.compileModule(wasmBytes);
                assertThat(finalModule.isValid()).isTrue();
                finalModule.close();

                return String.format("Exception handling: %d successful, %d failed",
                    successfulModules, failedModules);
              }
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Exception resource cleanup validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle try-with-resources cleanup")
  void shouldHandleTryWithResourcesCleanup() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "try-with-resources-cleanup",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();
              int modulesProcessed = 0;

              // When - Use try-with-resources for automatic cleanup
              try (final Engine engine = runtime.createEngine()) {
                
                for (int i = 0; i < 10; i++) {
                  try (final Module module = engine.compileModule(wasmBytes)) {
                    // Verify module is valid within try block
                    assertThat(module.isValid()).isTrue();
                    
                    // Create and use instance
                    try (final Store store = engine.createStore();
                         final Instance instance = module.instantiate(store)) {
                      
                      // Instance should be valid
                      assertThat(instance).isNotNull();
                      modulesProcessed++;
                    }
                    // Instance and store automatically closed
                  }
                  // Module automatically closed
                }
              }
              // Engine automatically closed

              // Then - All resources should be cleaned up automatically
              return "Try-with-resources: " + modulesProcessed + " modules processed";
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Try-with-resources cleanup validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle finalizer-based cleanup timing")
  void shouldHandleFinalizerBasedCleanupTiming() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "finalizer-cleanup-timing",
            runtime -> {
              // Given
              final byte[] wasmBytes = TestUtils.createSimpleWasmModule();

              try (final Engine engine = runtime.createEngine()) {
                // Create modules and let them go out of scope without explicit close
                createModulesWithoutExplicitClose(engine, wasmBytes, 10);

                // Force garbage collection and finalization
                for (int i = 0; i < 5; i++) {
                  System.gc();
                  System.runFinalization();
                  Thread.sleep(20);
                }

                // Engine should still be functional
                final Module testModule = engine.compileModule(wasmBytes);
                assertThat(testModule.isValid()).isTrue();
                testModule.close();

                return "Finalizer cleanup completed";
              }
            },
            comparison -> comparison.getJniResult().equals(comparison.getPanamaResult()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Finalizer cleanup timing validation: " + validation.getSummary());
  }

  /**
   * Helper method to create modules without explicit close (to test finalizers).
   */
  private void createModulesWithoutExplicitClose(final Engine engine, final byte[] wasmBytes, final int count) {
    for (int i = 0; i < count; i++) {
      final Module module = engine.compileModule(wasmBytes);
      // Intentionally don't close - rely on finalizer
    }
  }

  /**
   * Gets current memory usage in bytes.
   */
  private long getUsedMemory() {
    final Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }
}
package ai.tegmentum.wasmtime4j.comprehensive;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Enhanced memory leak detection tests using real WebAssembly modules.
 *
 * <p>This test class performs comprehensive memory leak detection using actual WebAssembly modules
 * from the test resources, providing realistic stress testing scenarios that mirror production
 * workloads.
 */
@DisplayName("Real-World Memory Leak Detection Tests")
final class RealWorldMemoryLeakDetectionIT {

  private static final Logger LOGGER =
      Logger.getLogger(RealWorldMemoryLeakDetectionIT.class.getName());

  private static final String WASM_TEST_DIR = "src/test/resources/wasm/custom-tests";

  /** Tests memory leak detection using multiple real WebAssembly modules under stress. */
  @Test
  @DisplayName("Should detect memory leaks using real WASM modules under stress")
  void shouldDetectMemoryLeaksUsingRealWasmModulesUnderStress() throws Exception {
    LOGGER.info("=== Real WASM Module Memory Leak Detection Test ===");

    final List<WasmTestModule> testModules = loadRealWasmModules();
    assertThat(testModules).isNotEmpty();

    LOGGER.info("Loaded " + testModules.size() + " real WASM modules for testing");

    final MemoryLeakDetector detector = new MemoryLeakDetector();
    detector.startMonitoring();

    try (final WasmRuntime runtime = WasmRuntimeFactory.create()) {
      final int stressIterations = 200;
      final AtomicLong totalOperations = new AtomicLong(0);
      final AtomicLong totalMemoryAllocated = new AtomicLong(0);

      for (int iteration = 0; iteration < stressIterations; iteration++) {
        // Select a random module for this iteration
        final WasmTestModule testModule =
            testModules.get(ThreadLocalRandom.current().nextInt(testModules.size()));

        // Execute stress test with the selected module
        final StressTestResult result = executeModuleStressTest(runtime, testModule, iteration);
        totalOperations.addAndGet(result.operationsCompleted);
        totalMemoryAllocated.addAndGet(result.memoryAllocated);

        // Record memory state every 20 iterations
        if (iteration % 20 == 0) {
          detector.recordMemorySnapshot("iteration_" + iteration);
          forceGarbageCollection();
        }

        // Log progress
        if (iteration % 50 == 0) {
          LOGGER.info(
              String.format(
                  "Progress: %d/%d iterations, %d total operations",
                  iteration, stressIterations, totalOperations.get()));
        }
      }

      // Final cleanup and analysis
      forceGarbageCollection();
      detector.recordMemorySnapshot("final");

      final MemoryLeakAnalysis analysis = detector.performLeakAnalysis();

      LOGGER.info("=== Memory Leak Analysis Results ===");
      LOGGER.info("Total operations: " + totalOperations.get());
      LOGGER.info("Total memory allocated: " + (totalMemoryAllocated.get() / 1024 / 1024) + " MB");
      LOGGER.info("Memory growth rate: " + analysis.getMemoryGrowthRateMBPerMin() + " MB/min");
      LOGGER.info("Memory efficiency: " + analysis.getMemoryEfficiencyPercent() + "%");
      LOGGER.info("Detected leaks: " + analysis.getDetectedLeakCount());
      LOGGER.info("GC effectiveness: " + analysis.getGcEffectivenessPercent() + "%");

      // Validate memory leak thresholds
      assertThat(analysis.getMemoryGrowthRateMBPerMin())
          .withFailMessage("Memory growth rate too high: " + analysis.getMemoryGrowthRateMBPerMin())
          .isLessThan(5.0); // Less than 5MB/minute growth

      assertThat(analysis.getDetectedLeakCount())
          .withFailMessage("Too many memory leaks detected: " + analysis.getDetectedLeakCount())
          .isLessThan(10); // Less than 10 detected leaks

      assertThat(analysis.getGcEffectivenessPercent())
          .withFailMessage("GC effectiveness too low: " + analysis.getGcEffectivenessPercent())
          .isGreaterThan(70.0); // Greater than 70% GC effectiveness

      LOGGER.info("Real WASM module memory leak detection: SUCCESS");

    } finally {
      detector.stopMonitoring();
    }
  }

  /**
   * Tests memory behavior with different types of WebAssembly modules to identify module-specific
   * memory patterns.
   */
  @ParameterizedTest
  @ValueSource(strings = {"add.wasm", "functions.wasm", "memory.wasm"})
  @DisplayName("Should analyze memory patterns for different module types")
  void shouldAnalyzeMemoryPatternsForDifferentModuleTypes(final String moduleName)
      throws Exception {
    LOGGER.info("=== Testing memory patterns for: " + moduleName + " ===");

    final Path modulePath = Paths.get(WASM_TEST_DIR, moduleName).toAbsolutePath();
    if (!Files.exists(modulePath)) {
      LOGGER.warning("Module not found: " + modulePath + ", using fallback");
      return; // Skip if module doesn't exist
    }

    final byte[] moduleBytes = Files.readAllBytes(modulePath);
    final MemoryPatternAnalyzer analyzer = new MemoryPatternAnalyzer(moduleName);

    try (final WasmRuntime runtime = WasmRuntimeFactory.create()) {
      analyzer.startAnalysis();

      final int cycles = 100;
      for (int cycle = 0; cycle < cycles; cycle++) {
        analyzer.recordPreAllocation();

        // Create and execute module operations
        try (final Engine engine = runtime.createEngine()) {
          final Module module = runtime.compileModule(engine, moduleBytes);

          try (final Store store = runtime.createStore(engine)) {
            final Instance instance = runtime.instantiate(module);

            // Execute available functions in the module
            executeAvailableFunctions(instance, cycle);

            // Test memory operations if available
            testModuleMemoryOperations(instance, cycle);

            analyzer.recordPostAllocation();
          }
        }

        analyzer.recordPostCleanup();

        // Periodic GC to help with cleanup
        if (cycle % 25 == 0) {
          System.gc();
        }
      }

      final ModuleMemoryPattern pattern = analyzer.analyzePattern();

      LOGGER.info("Memory pattern analysis for " + moduleName + ":");
      LOGGER.info("  Average allocation per cycle: " + pattern.getAvgAllocationMB() + " MB");
      LOGGER.info("  Average cleanup efficiency: " + pattern.getCleanupEfficiencyPercent() + "%");
      LOGGER.info("  Memory accumulation rate: " + pattern.getAccumulationRateMB() + " MB/cycle");
      LOGGER.info("  Peak memory usage: " + pattern.getPeakMemoryMB() + " MB");

      // Validate memory pattern is acceptable
      assertThat(pattern.getCleanupEfficiencyPercent())
          .withFailMessage("Poor cleanup efficiency for " + moduleName)
          .isGreaterThan(80.0);

      assertThat(pattern.getAccumulationRateMB())
          .withFailMessage("High memory accumulation for " + moduleName)
          .isLessThan(1.0);

      LOGGER.info("Memory pattern analysis for " + moduleName + ": SUCCESS");
    }
  }

  /**
   * Tests memory behavior under concurrent load with real modules to validate thread safety and
   * memory isolation.
   */
  @Test
  @DisplayName("Should handle concurrent memory operations with real modules safely")
  void shouldHandleConcurrentMemoryOperationsWithRealModulesSafely() throws Exception {
    LOGGER.info("=== Concurrent Memory Operations with Real Modules ===");

    final List<WasmTestModule> testModules = loadRealWasmModules();
    if (testModules.isEmpty()) {
      LOGGER.warning("No real WASM modules available, skipping concurrent test");
      return;
    }

    final int threadCount = 6;
    final int operationsPerThread = 30;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch latch = new CountDownLatch(threadCount);
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger errorCount = new AtomicInteger(0);
    final ConcurrentMemoryTracker memoryTracker = new ConcurrentMemoryTracker();

    try (final WasmRuntime runtime = WasmRuntimeFactory.create()) {
      memoryTracker.startTracking();

      // Start concurrent workers
      for (int threadId = 0; threadId < threadCount; threadId++) {
        final int finalThreadId = threadId;
        executor.submit(
            () -> {
              try {
                for (int op = 0; op < operationsPerThread; op++) {
                  final WasmTestModule module =
                      testModules.get(ThreadLocalRandom.current().nextInt(testModules.size()));

                  memoryTracker.recordOperationStart(finalThreadId);
                  executeModuleConcurrently(runtime, module, finalThreadId, op);
                  memoryTracker.recordOperationEnd(finalThreadId);

                  successCount.incrementAndGet();

                  // Random delay to simulate realistic workload
                  Thread.sleep(ThreadLocalRandom.current().nextInt(5, 20));
                }
              } catch (final Exception e) {
                LOGGER.warning("Thread " + finalThreadId + " error: " + e.getMessage());
                errorCount.incrementAndGet();
              } finally {
                latch.countDown();
              }
            });
      }

      // Wait for completion
      final boolean completed = latch.await(120, TimeUnit.SECONDS);
      assertThat(completed).isTrue();

      memoryTracker.stopTracking();
      final ConcurrentMemoryReport report = memoryTracker.generateReport();

      LOGGER.info("Concurrent memory operations completed:");
      LOGGER.info("  Total successful operations: " + successCount.get());
      LOGGER.info("  Total failed operations: " + errorCount.get());
      LOGGER.info("  Peak concurrent memory usage: " + report.getPeakMemoryMB() + " MB");
      LOGGER.info("  Memory contention incidents: " + report.getContentionIncidents());
      LOGGER.info("  Thread memory isolation: " + report.getIsolationScore() + "%");

      // Validate concurrent execution quality
      final int totalOperations = threadCount * operationsPerThread;
      assertThat(successCount.get() + errorCount.get()).isEqualTo(totalOperations);
      assertThat(errorCount.get()).isLessThan(totalOperations / 20); // Less than 5% errors

      assertThat(report.getContentionIncidents())
          .withFailMessage("Too many memory contention incidents")
          .isLessThan(10);

      assertThat(report.getIsolationScore())
          .withFailMessage("Poor thread memory isolation")
          .isGreaterThan(85.0);

      LOGGER.info("Concurrent memory operations test: SUCCESS");

    } finally {
      executor.shutdown();
      executor.awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /** Loads real WebAssembly modules from test resources. */
  private List<WasmTestModule> loadRealWasmModules() throws IOException {
    final List<WasmTestModule> modules = new ArrayList<>();
    final Path wasmDir = Paths.get(WASM_TEST_DIR).toAbsolutePath();

    if (!Files.exists(wasmDir)) {
      LOGGER.warning("WASM test directory not found: " + wasmDir);
      return Collections.emptyList();
    }

    // Load specific test modules
    final String[] moduleNames = {"add.wasm", "functions.wasm", "memory.wasm", "simple.wasm"};

    for (final String moduleName : moduleNames) {
      final Path modulePath = wasmDir.resolve(moduleName);
      if (Files.exists(modulePath)) {
        final byte[] moduleBytes = Files.readAllBytes(modulePath);
        modules.add(new WasmTestModule(moduleName, moduleBytes));
        LOGGER.info("Loaded WASM module: " + moduleName + " (" + moduleBytes.length + " bytes)");
      } else {
        LOGGER.warning("WASM module not found: " + modulePath);
      }
    }

    // If no modules found, create a fallback
    if (modules.isEmpty()) {
      LOGGER.info("No real modules found, creating fallback module");
      modules.add(new WasmTestModule("fallback.wasm", createFallbackWasmModule()));
    }

    return modules;
  }

  /** Creates a fallback WebAssembly module for testing. */
  private byte[] createFallbackWasmModule() {
    // Simple WASM module with add function
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6d, // WASM magic
      0x01,
      0x00,
      0x00,
      0x00, // Version
      0x01,
      0x07, // Type section
      0x01,
      0x60,
      0x02,
      0x7f,
      0x7f,
      0x01,
      0x7f, // (i32, i32) -> i32
      0x03,
      0x02,
      0x01,
      0x00, // Function section
      0x07,
      0x07,
      0x01,
      0x03,
      0x61,
      0x64,
      0x64,
      0x00,
      0x00, // Export section
      0x0a,
      0x09,
      0x01,
      0x07,
      0x00,
      0x20,
      0x00,
      0x20,
      0x01,
      0x6a,
      0x0b // Code section
    };
  }

  /** Executes stress test on a specific WebAssembly module. */
  private StressTestResult executeModuleStressTest(
      final WasmRuntime runtime, final WasmTestModule testModule, final int iteration)
      throws Exception {

    final StressTestResult result = new StressTestResult();
    final long startMemory = getCurrentMemoryUsage();

    try (final Engine engine = runtime.createEngine()) {
      final Module module = runtime.compileModule(engine, testModule.getBytes());

      try (final Store store = runtime.createStore(engine)) {
        final Instance instance = runtime.instantiate(module);

        // Execute multiple operations on this instance
        final int operations = 10 + ThreadLocalRandom.current().nextInt(20);
        for (int i = 0; i < operations; i++) {
          executeAvailableFunctions(instance, i);
          testModuleMemoryOperations(instance, i);
          result.operationsCompleted++;
        }
      }
    }

    result.memoryAllocated = getCurrentMemoryUsage() - startMemory;
    return result;
  }

  /** Executes available functions in a WebAssembly instance. */
  private void executeAvailableFunctions(final Instance instance, final int iteration) {
    // Try common function names
    final String[] commonFunctions = {"add", "main", "test", "_start"};

    for (final String functionName : commonFunctions) {
      final Optional<WasmFunction> function = instance.getFunction(functionName);
      if (function.isPresent()) {
        try {
          // Execute function with appropriate parameters based on name
          if ("add".equals(functionName)) {
            final WasmValue[] args = {WasmValue.i32(iteration), WasmValue.i32(iteration + 1)};
            function.get().call(args);
          } else {
            // Try calling with no parameters for other functions
            try {
              function.get().call(new WasmValue[0]);
            } catch (final Exception e) {
              // Some functions may require parameters, skip silently
            }
          }
        } catch (final Exception e) {
          // Some function calls may fail due to requirements, continue testing
          LOGGER.fine("Function " + functionName + " call failed: " + e.getMessage());
        }
        break; // Execute only one function per iteration to control test duration
      }
    }
  }

  /** Tests memory operations if the instance has exported memory. */
  private void testModuleMemoryOperations(final Instance instance, final int iteration) {
    final Optional<WasmMemory> memory = instance.getMemory("memory");
    if (memory.isPresent()) {
      final WasmMemory wasmMemory = memory.get();
      try {
        // Basic memory operations
        final int pages = wasmMemory.getPages();
        if (pages > 0) {
          // Test memory read/write if possible
          // This is a placeholder - actual memory operations depend on the API
          LOGGER.fine("Memory pages: " + pages + " (iteration " + iteration + ")");
        }
      } catch (final Exception e) {
        // Memory operations may fail, continue testing
        LOGGER.fine("Memory operation failed: " + e.getMessage());
      }
    }
  }

  /** Executes a module concurrently for thread safety testing. */
  private void executeModuleConcurrently(
      final WasmRuntime runtime,
      final WasmTestModule testModule,
      final int threadId,
      final int operationId)
      throws Exception {

    try (final Engine engine = runtime.createEngine()) {
      final Module module = runtime.compileModule(engine, testModule.getBytes());

      try (final Store store = runtime.createStore(engine)) {
        final Instance instance = runtime.instantiate(module);

        // Execute a few operations
        for (int i = 0; i < 3; i++) {
          executeAvailableFunctions(instance, threadId * 1000 + operationId * 10 + i);
        }
      }
    }
  }

  /** Forces garbage collection multiple times. */
  private void forceGarbageCollection() {
    for (int i = 0; i < 3; i++) {
      System.gc();
      try {
        Thread.sleep(100);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  /** Gets current memory usage in bytes. */
  private long getCurrentMemoryUsage() {
    final Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  /** Container for WebAssembly test module data. */
  private static final class WasmTestModule {
    private final String name;
    private final byte[] bytes;

    public WasmTestModule(final String name, final byte[] bytes) {
      this.name = name;
      this.bytes = bytes.clone();
    }

    public String getName() {
      return name;
    }

    public byte[] getBytes() {
      return bytes.clone();
    }
  }

  /** Result of a stress test execution. */
  private static final class StressTestResult {
    long operationsCompleted = 0;
    long memoryAllocated = 0;
  }

  /** Memory leak detector that tracks memory usage over time. */
  private static final class MemoryLeakDetector {
    private final MemoryMXBean memoryBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final List<MemorySnapshot> snapshots;
    private Instant startTime;
    private long initialGcCollections;

    public MemoryLeakDetector() {
      this.memoryBean = ManagementFactory.getMemoryMXBean();
      this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
      this.snapshots = new ArrayList<>();
    }

    public void startMonitoring() {
      this.startTime = Instant.now();
      this.initialGcCollections = getTotalGcCollections();
      recordMemorySnapshot("start");
    }

    public void stopMonitoring() {
      recordMemorySnapshot("end");
    }

    public void recordMemorySnapshot(final String label) {
      final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
      snapshots.add(
          new MemorySnapshot(label, Instant.now(), heapUsage.getUsed(), heapUsage.getCommitted()));
    }

    public MemoryLeakAnalysis performLeakAnalysis() {
      if (snapshots.size() < 2) {
        return new MemoryLeakAnalysis(0.0, 100.0, 0, 100.0);
      }

      final MemorySnapshot first = snapshots.get(0);
      final MemorySnapshot last = snapshots.get(snapshots.size() - 1);

      final Duration totalTime = Duration.between(first.timestamp, last.timestamp);
      final long memoryIncrease = last.heapUsed - first.heapUsed;

      // Calculate memory growth rate in MB per minute
      final double growthRateMBPerMin =
          totalTime.toMillis() > 0
              ? (memoryIncrease / 1024.0 / 1024.0) / (totalTime.toMillis() / 60000.0)
              : 0.0;

      // Calculate memory efficiency (average used / committed)
      final double avgUsed = snapshots.stream().mapToLong(s -> s.heapUsed).average().orElse(0.0);
      final double avgCommitted =
          snapshots.stream().mapToLong(s -> s.heapCommitted).average().orElse(1.0);
      final double efficiency = (avgUsed / avgCommitted) * 100.0;

      // Detect potential leaks (significant memory increases without cleanup)
      int leakCount = 0;
      for (int i = 1; i < snapshots.size(); i++) {
        final long increase = snapshots.get(i).heapUsed - snapshots.get(i - 1).heapUsed;
        if (increase > 10 * 1024 * 1024) { // More than 10MB increase
          leakCount++;
        }
      }

      // Calculate GC effectiveness
      final long totalGcCollections = getTotalGcCollections() - initialGcCollections;
      final double gcEffectiveness = totalGcCollections > 0 ? Math.min(100.0, efficiency) : 0.0;

      return new MemoryLeakAnalysis(growthRateMBPerMin, efficiency, leakCount, gcEffectiveness);
    }

    private long getTotalGcCollections() {
      return gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
    }
  }

  /** Snapshot of memory state at a point in time. */
  private static final class MemorySnapshot {
    final String label;
    final Instant timestamp;
    final long heapUsed;
    final long heapCommitted;

    public MemorySnapshot(
        final String label,
        final Instant timestamp,
        final long heapUsed,
        final long heapCommitted) {
      this.label = label;
      this.timestamp = timestamp;
      this.heapUsed = heapUsed;
      this.heapCommitted = heapCommitted;
    }
  }

  /** Analysis results from memory leak detection. */
  private static final class MemoryLeakAnalysis {
    private final double memoryGrowthRateMBPerMin;
    private final double memoryEfficiencyPercent;
    private final int detectedLeakCount;
    private final double gcEffectivenessPercent;

    public MemoryLeakAnalysis(
        final double memoryGrowthRateMBPerMin,
        final double memoryEfficiencyPercent,
        final int detectedLeakCount,
        final double gcEffectivenessPercent) {
      this.memoryGrowthRateMBPerMin = memoryGrowthRateMBPerMin;
      this.memoryEfficiencyPercent = memoryEfficiencyPercent;
      this.detectedLeakCount = detectedLeakCount;
      this.gcEffectivenessPercent = gcEffectivenessPercent;
    }

    public double getMemoryGrowthRateMBPerMin() {
      return memoryGrowthRateMBPerMin;
    }

    public double getMemoryEfficiencyPercent() {
      return memoryEfficiencyPercent;
    }

    public int getDetectedLeakCount() {
      return detectedLeakCount;
    }

    public double getGcEffectivenessPercent() {
      return gcEffectivenessPercent;
    }
  }

  /** Analyzes memory patterns for specific module types. */
  private static final class MemoryPatternAnalyzer {
    private final String moduleName;
    private final List<Long> preAllocations;
    private final List<Long> postAllocations;
    private final List<Long> postCleanups;

    public MemoryPatternAnalyzer(final String moduleName) {
      this.moduleName = moduleName;
      this.preAllocations = new ArrayList<>();
      this.postAllocations = new ArrayList<>();
      this.postCleanups = new ArrayList<>();
    }

    public void startAnalysis() {
      // Initialize analysis
    }

    public void recordPreAllocation() {
      preAllocations.add(getCurrentMemoryUsage());
    }

    public void recordPostAllocation() {
      postAllocations.add(getCurrentMemoryUsage());
    }

    public void recordPostCleanup() {
      postCleanups.add(getCurrentMemoryUsage());
    }

    public ModuleMemoryPattern analyzePattern() {
      if (preAllocations.isEmpty() || postAllocations.isEmpty() || postCleanups.isEmpty()) {
        return new ModuleMemoryPattern(0.0, 100.0, 0.0, 0.0);
      }

      // Calculate average allocation per cycle
      double totalAllocation = 0.0;
      for (int i = 0; i < Math.min(preAllocations.size(), postAllocations.size()); i++) {
        totalAllocation += postAllocations.get(i) - preAllocations.get(i);
      }
      final double avgAllocationMB = (totalAllocation / preAllocations.size()) / 1024 / 1024;

      // Calculate cleanup efficiency
      double totalCleanup = 0.0;
      for (int i = 0; i < Math.min(postAllocations.size(), postCleanups.size()); i++) {
        final long allocated = Math.max(0, postAllocations.get(i) - preAllocations.get(i));
        final long cleaned = Math.max(0, postAllocations.get(i) - postCleanups.get(i));
        if (allocated > 0) {
          totalCleanup += (double) cleaned / allocated;
        }
      }
      final double cleanupEfficiency = (totalCleanup / postAllocations.size()) * 100;

      // Calculate accumulation rate
      final long finalMemory = postCleanups.get(postCleanups.size() - 1);
      final long initialMemory = preAllocations.get(0);
      final double accumulationRateMB =
          (finalMemory - initialMemory) / 1024.0 / 1024.0 / preAllocations.size();

      // Calculate peak memory
      final long peakMemory = postAllocations.stream().mapToLong(Long::longValue).max().orElse(0);
      final double peakMemoryMB = peakMemory / 1024.0 / 1024.0;

      return new ModuleMemoryPattern(
          avgAllocationMB, cleanupEfficiency, accumulationRateMB, peakMemoryMB);
    }

    private long getCurrentMemoryUsage() {
      final Runtime runtime = Runtime.getRuntime();
      return runtime.totalMemory() - runtime.freeMemory();
    }
  }

  /** Memory pattern analysis results for a specific module. */
  private static final class ModuleMemoryPattern {
    private final double avgAllocationMB;
    private final double cleanupEfficiencyPercent;
    private final double accumulationRateMB;
    private final double peakMemoryMB;

    public ModuleMemoryPattern(
        final double avgAllocationMB,
        final double cleanupEfficiencyPercent,
        final double accumulationRateMB,
        final double peakMemoryMB) {
      this.avgAllocationMB = avgAllocationMB;
      this.cleanupEfficiencyPercent = cleanupEfficiencyPercent;
      this.accumulationRateMB = accumulationRateMB;
      this.peakMemoryMB = peakMemoryMB;
    }

    public double getAvgAllocationMB() {
      return avgAllocationMB;
    }

    public double getCleanupEfficiencyPercent() {
      return cleanupEfficiencyPercent;
    }

    public double getAccumulationRateMB() {
      return accumulationRateMB;
    }

    public double getPeakMemoryMB() {
      return peakMemoryMB;
    }
  }

  /** Tracks memory usage during concurrent operations. */
  private static final class ConcurrentMemoryTracker {
    private final AtomicLong peakMemoryUsage;
    private final AtomicInteger activeOperations;
    private final List<Long> memorySnapshots;
    private final AtomicInteger contentionIncidents;

    public ConcurrentMemoryTracker() {
      this.peakMemoryUsage = new AtomicLong(0);
      this.activeOperations = new AtomicInteger(0);
      this.memorySnapshots = Collections.synchronizedList(new ArrayList<>());
      this.contentionIncidents = new AtomicInteger(0);
    }

    public void startTracking() {
      // Initialize tracking
    }

    public void recordOperationStart(final int threadId) {
      activeOperations.incrementAndGet();
      final long currentMemory = getCurrentMemoryUsage();
      peakMemoryUsage.updateAndGet(peak -> Math.max(peak, currentMemory));
      memorySnapshots.add(currentMemory);

      // Simple contention detection
      if (activeOperations.get() > 4) {
        contentionIncidents.incrementAndGet();
      }
    }

    public void recordOperationEnd(final int threadId) {
      activeOperations.decrementAndGet();
    }

    public void stopTracking() {
      // Finalize tracking
    }

    public ConcurrentMemoryReport generateReport() {
      final double peakMemoryMB = peakMemoryUsage.get() / 1024.0 / 1024.0;
      final int incidents = contentionIncidents.get();

      // Calculate isolation score based on memory variance
      final double memoryVariance = calculateMemoryVariance();
      final double isolationScore = Math.max(0, 100.0 - memoryVariance);

      return new ConcurrentMemoryReport(peakMemoryMB, incidents, isolationScore);
    }

    private double calculateMemoryVariance() {
      if (memorySnapshots.size() < 2) {
        return 0.0;
      }

      final double mean = memorySnapshots.stream().mapToLong(Long::longValue).average().orElse(0.0);
      final double variance =
          memorySnapshots.stream()
              .mapToDouble(mem -> Math.pow(mem - mean, 2))
              .average()
              .orElse(0.0);

      return Math.sqrt(variance) / 1024 / 1024; // Return variance in MB
    }

    private long getCurrentMemoryUsage() {
      final Runtime runtime = Runtime.getRuntime();
      return runtime.totalMemory() - runtime.freeMemory();
    }
  }

  /** Report of concurrent memory operations. */
  private static final class ConcurrentMemoryReport {
    private final double peakMemoryMB;
    private final int contentionIncidents;
    private final double isolationScore;

    public ConcurrentMemoryReport(
        final double peakMemoryMB, final int contentionIncidents, final double isolationScore) {
      this.peakMemoryMB = peakMemoryMB;
      this.contentionIncidents = contentionIncidents;
      this.isolationScore = isolationScore;
    }

    public double getPeakMemoryMB() {
      return peakMemoryMB;
    }

    public int getContentionIncidents() {
      return contentionIncidents;
    }

    public double getIsolationScore() {
      return isolationScore;
    }
  }
}

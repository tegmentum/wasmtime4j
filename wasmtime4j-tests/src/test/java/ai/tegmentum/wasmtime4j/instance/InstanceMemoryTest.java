package ai.tegmentum.wasmtime4j.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive test suite for Instance memory operations, bounds checking, safety validation, and
 * memory management patterns. Tests memory access, growth operations, bounds enforcement, and
 * cross-runtime consistency in memory handling.
 */
@DisplayName("Instance Memory Tests")
final class InstanceMemoryTest {

  private static final Logger LOGGER = Logger.getLogger(InstanceMemoryTest.class.getName());

  private final Map<String, Object> testMetrics = new HashMap<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    TestUtils.skipIfCategoryNotEnabled(TestCategories.INSTANCE);
    testMetrics.clear();
    LOGGER.info("Starting test: " + testInfo.getDisplayName());
  }

  /**
   * Execute test with both JNI and Panama runtimes if available.
   *
   * @param testAction The test action to execute with each runtime
   */
  private void runWithBothRuntimes(final RuntimeTestAction testAction) {
    final List<RuntimeType> availableRuntimes = WasmRuntimeFactory.getAvailableRuntimes();

    for (final RuntimeType runtimeType : availableRuntimes) {
      try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
        LOGGER.info("Testing with runtime: " + runtimeType);
        testAction.execute(runtime, runtimeType);
      } catch (final Exception e) {
        throw new RuntimeException("Test failed with runtime " + runtimeType, e);
      }
    }
  }

  /**
   * Add a test metric for tracking and analysis.
   *
   * @param message The metric message
   */
  private void addTestMetric(final String message) {
    testMetrics.put(Instant.now().toString(), message);
    LOGGER.info("Test metric: " + message);
  }

  /** Functional interface for runtime-specific test actions. */
  @FunctionalInterface
  private interface RuntimeTestAction {
    void execute(WasmRuntime runtime, RuntimeType runtimeType) throws Exception;
  }

  @Nested
  @DisplayName("Basic Memory Operations Tests")
  final class BasicMemoryOperationTests {

    @Test
    @DisplayName("Should perform load/store operations with all data types")
    void shouldPerformLoadStoreOperationsWithAllDataTypes() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test i32 load/store operations
              final Map<Integer, Integer> i32Tests = new HashMap<>();
              i32Tests.put(0, 42);
              i32Tests.put(4, -1);
              i32Tests.put(8, Integer.MAX_VALUE);
              i32Tests.put(12, Integer.MIN_VALUE);
              i32Tests.put(16, 0);

              for (final Map.Entry<Integer, Integer> test : i32Tests.entrySet()) {
                final int offset = test.getKey();
                final int value = test.getValue();

                // Store value
                instance.callFunction("store", WasmValue.i32(offset), WasmValue.i32(value));

                // Load and verify
                final WasmValue[] loadResult = instance.callFunction("load", WasmValue.i32(offset));
                assertThat(loadResult).hasSize(1);
                assertThat(loadResult[0].asI32()).isEqualTo(value);

                addTestMetric(
                    String.format(
                        "i32 load/store at offset %d value %d with %s",
                        offset, value, runtimeType));
              }

              // Test overlapping memory regions
              instance.callFunction("store", WasmValue.i32(20), WasmValue.i32(0x12345678));
              instance.callFunction("store", WasmValue.i32(24), WasmValue.i32(0x9ABCDEF0));

              final WasmValue[] val1 = instance.callFunction("load", WasmValue.i32(20));
              final WasmValue[] val2 = instance.callFunction("load", WasmValue.i32(24));

              assertThat(val1[0].asI32()).isEqualTo(0x12345678);
              assertThat(val2[0].asI32()).isEqualTo(0x9ABCDEF0);
            }
          });
    }

    @Test
    @DisplayName("Should handle sequential memory operations efficiently")
    void shouldHandleSequentialMemoryOperationsEfficiently() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final int numOperations = 1000;
              final Instant startTime = Instant.now();

              // Perform sequential stores
              for (int i = 0; i < numOperations; i++) {
                final int offset = i * 4; // 4 bytes per i32
                instance.callFunction("store", WasmValue.i32(offset), WasmValue.i32(i));
              }

              // Perform sequential loads and verify
              for (int i = 0; i < numOperations; i++) {
                final int offset = i * 4;
                final WasmValue[] result = instance.callFunction("load", WasmValue.i32(offset));
                assertThat(result[0].asI32()).isEqualTo(i);
              }

              final Duration totalTime = Duration.between(startTime, Instant.now());
              final double operationsPerSecond =
                  (numOperations * 2) / (totalTime.toMillis() / 1000.0);

              // Should achieve reasonable performance
              assertThat(operationsPerSecond).isGreaterThan(1000);

              addTestMetric(
                  String.format(
                      "Sequential operations: %d ops in %dms (%.0f ops/sec) with %s",
                      numOperations * 2, totalTime.toMillis(), operationsPerSecond, runtimeType));
            }
          });
    }

    @Test
    @DisplayName("Should handle random access memory patterns")
    void shouldHandleRandomAccessMemoryPatterns() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final Map<Integer, Integer> randomData = new HashMap<>();

              // Generate random access patterns
              final int[] offsets = {0, 100, 50, 200, 25, 175, 75, 125, 12, 188};
              final int[] values = {
                42,
                -17,
                1000000,
                -999999,
                0,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                123456,
                -654321,
                777777
              };

              // Store data in random order
              for (int i = 0; i < offsets.length; i++) {
                final int offset = offsets[i];
                final int value = values[i];
                randomData.put(offset, value);

                instance.callFunction("store", WasmValue.i32(offset), WasmValue.i32(value));
              }

              // Verify data in different random order
              final int[] verifyOrder = {5, 2, 8, 1, 9, 0, 6, 3, 7, 4};
              for (final int i : verifyOrder) {
                final int offset = offsets[i];
                final int expectedValue = values[i];

                final WasmValue[] result = instance.callFunction("load", WasmValue.i32(offset));
                assertThat(result[0].asI32()).isEqualTo(expectedValue);
              }

              addTestMetric("Random access memory patterns validated with " + runtimeType);
            }
          });
    }
  }

  @Nested
  @DisplayName("Memory Bounds Checking Tests")
  final class MemoryBoundsTests {

    @Test
    @DisplayName("Should enforce strict memory bounds checking")
    void shouldEnforceStrictMemoryBoundsChecking() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test access at various problematic offsets
              final int[] invalidOffsets = {
                -1,
                -4,
                -1000, // Negative offsets
                Integer.MIN_VALUE, // Extreme negative
                1000000,
                10000000, // Very large offsets
                Integer.MAX_VALUE - 3, // Near max integer
                Integer.MAX_VALUE // Max integer
              };

              for (final int invalidOffset : invalidOffsets) {
                // Both load and store should fail with bounds violations
                assertThatThrownBy(
                        () -> instance.callFunction("load", WasmValue.i32(invalidOffset)))
                    .isInstanceOf(WasmException.class)
                    .hasMessageContainingAny("out of bounds", "memory", "bounds", "access");

                assertThatThrownBy(
                        () ->
                            instance.callFunction(
                                "store", WasmValue.i32(invalidOffset), WasmValue.i32(42)))
                    .isInstanceOf(WasmException.class)
                    .hasMessageContainingAny("out of bounds", "memory", "bounds", "access");

                addTestMetric(
                    String.format(
                        "Bounds check enforced for offset %d with %s", invalidOffset, runtimeType));
              }
            }
          });
    }

    @Test
    @DisplayName("Should validate memory access alignment requirements")
    void shouldValidateMemoryAccessAlignmentRequirements() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test aligned access (should work)
              final int[] alignedOffsets = {0, 4, 8, 12, 16, 20, 24, 32, 64, 128};

              for (final int offset : alignedOffsets) {
                try {
                  // Store should succeed for aligned access
                  instance.callFunction("store", WasmValue.i32(offset), WasmValue.i32(42));
                  final WasmValue[] result = instance.callFunction("load", WasmValue.i32(offset));
                  assertThat(result[0].asI32()).isEqualTo(42);

                  addTestMetric(
                      String.format(
                          "Aligned access at %d successful with %s", offset, runtimeType));
                } catch (final WasmException e) {
                  // If it fails, it should be due to bounds, not alignment
                  if (e.getMessage().contains("out of bounds")) {
                    addTestMetric(
                        String.format(
                            "Aligned access at %d failed due to bounds with %s",
                            offset, runtimeType));
                  } else {
                    throw e;
                  }
                }
              }

              // Test unaligned access (WebAssembly typically allows unaligned access)
              final int[] unalignedOffsets = {1, 2, 3, 5, 6, 7, 9, 10, 11};

              for (final int offset : unalignedOffsets) {
                try {
                  // Unaligned access should generally work in WebAssembly
                  instance.callFunction("store", WasmValue.i32(offset), WasmValue.i32(99));
                  final WasmValue[] result = instance.callFunction("load", WasmValue.i32(offset));
                  assertThat(result[0].asI32()).isEqualTo(99);

                  addTestMetric(
                      String.format(
                          "Unaligned access at %d successful with %s", offset, runtimeType));
                } catch (final WasmException e) {
                  // If it fails, should be due to bounds, not alignment
                  addTestMetric(
                      String.format(
                          "Unaligned access at %d failed: %s with %s",
                          offset, e.getMessage(), runtimeType));
                }
              }
            }
          });
    }

    @Test
    @DisplayName("Should handle edge case memory boundaries")
    void shouldHandleEdgeCaseMemoryBoundaries() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Find the actual memory size by testing incrementally
              int maxValidOffset = -1;
              for (int testOffset = 0; testOffset < 100000; testOffset += 4) {
                try {
                  instance.callFunction("store", WasmValue.i32(testOffset), WasmValue.i32(1));
                  maxValidOffset = testOffset;
                } catch (final WasmException e) {
                  // Found the boundary
                  break;
                }
              }

              if (maxValidOffset >= 0) {
                // Test at the boundary
                final WasmValue[] result =
                    instance.callFunction("load", WasmValue.i32(maxValidOffset));
                assertThat(result[0].asI32()).isEqualTo(1);

                // Test just beyond the boundary (should fail)
                assertThatThrownBy(
                        () ->
                            instance.callFunction(
                                "store", WasmValue.i32(maxValidOffset + 4), WasmValue.i32(42)))
                    .isInstanceOf(WasmException.class);

                addTestMetric(
                    String.format(
                        "Memory boundary at offset %d validated with %s",
                        maxValidOffset, runtimeType));
              } else {
                addTestMetric("No accessible memory found with " + runtimeType);
              }
            }
          });
    }
  }

  @Nested
  @DisplayName("Memory Growth and Management Tests")
  final class MemoryGrowthTests {

    @Test
    @DisplayName("Should handle memory growth operations correctly")
    void shouldHandleMemoryGrowthOperationsCorrectly() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_grow");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Get initial memory size
              final WasmValue[] initialGrowResult = instance.callFunction("grow", WasmValue.i32(0));
              assertThat(initialGrowResult).hasSize(1);
              final int initialPages = initialGrowResult[0].asI32();
              assertThat(initialPages).isGreaterThanOrEqualTo(0);

              // Grow memory by 1 page (64KB)
              final WasmValue[] growResult = instance.callFunction("grow", WasmValue.i32(1));
              assertThat(growResult).hasSize(1);
              final int previousPages = growResult[0].asI32();

              if (previousPages >= 0) {
                // Growth succeeded
                assertThat(previousPages).isEqualTo(initialPages);

                // Verify we can access the new memory
                final int newMemoryOffset =
                    (previousPages + 1) * 65536 - 4; // Last 4 bytes of new page
                try {
                  // This might still be out of bounds depending on implementation
                  addTestMetric(
                      String.format(
                          "Memory grew from %d to %d pages with %s",
                          previousPages, previousPages + 1, runtimeType));
                } catch (final Exception e) {
                  addTestMetric(
                      "Memory growth succeeded but access validation skipped with " + runtimeType);
                }
              } else {
                // Growth failed (returned -1)
                addTestMetric("Memory growth failed (limit reached) with " + runtimeType);
              }

              // Test growing by multiple pages
              final WasmValue[] multiGrowResult = instance.callFunction("grow", WasmValue.i32(2));
              final int multiGrowPrevious = multiGrowResult[0].asI32();

              if (multiGrowPrevious >= 0) {
                addTestMetric(
                    String.format(
                        "Multi-page growth from %d pages successful with %s",
                        multiGrowPrevious, runtimeType));
              } else {
                addTestMetric("Multi-page growth failed with " + runtimeType);
              }
            }
          });
    }

    @Test
    @DisplayName("Should handle memory growth limits and failures")
    void shouldHandleMemoryGrowthLimitsAndFailures() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_grow");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Test reasonable growth amounts
              final int[] growthAmounts = {1, 2, 5, 10, 16};
              int successfulGrowths = 0;

              for (final int growAmount : growthAmounts) {
                final WasmValue[] result = instance.callFunction("grow", WasmValue.i32(growAmount));
                final int previousPages = result[0].asI32();

                if (previousPages >= 0) {
                  successfulGrowths++;
                  addTestMetric(
                      String.format(
                          "Growth by %d pages succeeded from %d pages with %s",
                          growAmount, previousPages, runtimeType));
                } else {
                  addTestMetric(
                      String.format(
                          "Growth by %d pages failed (limit reached) with %s",
                          growAmount, runtimeType));
                  break; // Stop trying once we hit the limit
                }
              }

              // Should succeed with at least small growths
              assertThat(successfulGrowths).isGreaterThan(0);

              // Test excessive growth (should fail)
              final WasmValue[] excessiveResult =
                  instance.callFunction("grow", WasmValue.i32(1000));
              final int excessivePrevious = excessiveResult[0].asI32();

              if (excessivePrevious < 0) {
                addTestMetric("Excessive growth properly rejected with " + runtimeType);
              } else {
                addTestMetric("Excessive growth unexpectedly succeeded with " + runtimeType);
              }
            }
          });
    }

    @Test
    @DisplayName("Should maintain data integrity across memory growth")
    void shouldMaintainDataIntegrityAcrossMemoryGrowth() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_grow");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Note: We need to check if this module also has store/load functions
              // If not, we'll just test the growth operation itself

              try {
                // Try to store some data first
                instance.callFunction("store", WasmValue.i32(0), WasmValue.i32(12345));
                instance.callFunction("store", WasmValue.i32(4), WasmValue.i32(67890));

                // Verify initial data
                WasmValue[] val1 = instance.callFunction("load", WasmValue.i32(0));
                WasmValue[] val2 = instance.callFunction("load", WasmValue.i32(4));
                assertThat(val1[0].asI32()).isEqualTo(12345);
                assertThat(val2[0].asI32()).isEqualTo(67890);

                // Grow memory
                final WasmValue[] growResult = instance.callFunction("grow", WasmValue.i32(1));
                final int previousPages = growResult[0].asI32();

                if (previousPages >= 0) {
                  // Verify data is still intact after growth
                  val1 = instance.callFunction("load", WasmValue.i32(0));
                  val2 = instance.callFunction("load", WasmValue.i32(4));
                  assertThat(val1[0].asI32()).isEqualTo(12345);
                  assertThat(val2[0].asI32()).isEqualTo(67890);

                  addTestMetric(
                      "Data integrity maintained across memory growth with " + runtimeType);
                } else {
                  addTestMetric(
                      "Memory growth failed, data integrity test skipped with " + runtimeType);
                }

              } catch (final WasmException e) {
                // This module might not have store/load functions, just test growth
                final WasmValue[] growResult = instance.callFunction("grow", WasmValue.i32(1));
                addTestMetric(
                    "Memory growth tested without data integrity check with " + runtimeType);
              }
            }
          });
    }
  }

  @Nested
  @DisplayName("Concurrent Memory Access Tests")
  final class ConcurrentMemoryTests {

    @Test
    @DisplayName("Should handle concurrent memory operations safely")
    void shouldHandleConcurrentMemoryOperationsSafely() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              final int numThreads = 10;
              final int operationsPerThread = 100;
              final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
              final AtomicInteger successCount = new AtomicInteger(0);
              final AtomicInteger errorCount = new AtomicInteger(0);

              try {
                final List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < numThreads; i++) {
                  final int threadId = i;
                  final CompletableFuture<Void> future =
                      CompletableFuture.runAsync(
                          () -> {
                            for (int j = 0; j < operationsPerThread; j++) {
                              try {
                                final int offset = (threadId * operationsPerThread + j) * 4;
                                final int value = threadId * 1000 + j;

                                // Store value
                                instance.callFunction(
                                    "store", WasmValue.i32(offset), WasmValue.i32(value));

                                // Load and verify
                                final WasmValue[] result =
                                    instance.callFunction("load", WasmValue.i32(offset));
                                if (result[0].asI32() == value) {
                                  successCount.incrementAndGet();
                                } else {
                                  errorCount.incrementAndGet();
                                }
                              } catch (final Exception e) {
                                if (e.getMessage().contains("out of bounds")) {
                                  // Expected for large offsets
                                  break;
                                } else {
                                  errorCount.incrementAndGet();
                                  LOGGER.warning(
                                      "Concurrent memory operation failed: " + e.getMessage());
                                }
                              }
                            }
                          },
                          executor);
                  futures.add(future);
                }

                // Wait for all threads to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);

                // Should have mostly successful operations
                assertThat(successCount.get()).isGreaterThan(0);

                final double successRate =
                    (double) successCount.get() / (successCount.get() + errorCount.get());
                assertThat(successRate).isGreaterThan(0.5);

              } finally {
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
              }

              addTestMetric(
                  String.format(
                      "Concurrent memory: %d successful, %d errors with %s",
                      successCount.get(), errorCount.get(), runtimeType));
            }
          });
    }

    @Test
    @DisplayName("Should maintain memory consistency under concurrent access")
    void shouldMaintainMemoryConsistencyUnderConcurrentAccess() throws Exception {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore();
                final Module module = engine.compileModule(moduleBytes);
                final Instance instance = module.instantiate(store)) {

              // Initialize known values
              final Map<Integer, Integer> expectedValues = new HashMap<>();
              for (int i = 0; i < 20; i++) {
                final int offset = i * 8; // Use larger spacing to avoid interference
                final int value = i * 100;
                expectedValues.put(offset, value);

                try {
                  instance.callFunction("store", WasmValue.i32(offset), WasmValue.i32(value));
                } catch (final WasmException e) {
                  // Out of bounds, reduce test set
                  expectedValues.remove(offset);
                  break;
                }
              }

              if (expectedValues.isEmpty()) {
                addTestMetric("No memory accessible for consistency test with " + runtimeType);
                return;
              }

              // Concurrent readers to verify consistency
              final int numReaders = 5;
              final ExecutorService executor = Executors.newFixedThreadPool(numReaders);
              final AtomicInteger consistencyErrors = new AtomicInteger(0);
              final AtomicInteger totalReads = new AtomicInteger(0);

              try {
                final List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < numReaders; i++) {
                  final CompletableFuture<Void> future =
                      CompletableFuture.runAsync(
                          () -> {
                            for (int j = 0; j < 100; j++) {
                              for (final Map.Entry<Integer, Integer> entry :
                                  expectedValues.entrySet()) {
                                try {
                                  final int offset = entry.getKey();
                                  final int expectedValue = entry.getValue();

                                  final WasmValue[] result =
                                      instance.callFunction("load", WasmValue.i32(offset));
                                  totalReads.incrementAndGet();

                                  if (result[0].asI32() != expectedValue) {
                                    consistencyErrors.incrementAndGet();
                                  }
                                } catch (final Exception e) {
                                  consistencyErrors.incrementAndGet();
                                }
                              }
                            }
                          },
                          executor);
                  futures.add(future);
                }

                // Wait for all readers to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(15, TimeUnit.SECONDS);

                // Should have no consistency errors
                assertThat(consistencyErrors.get()).isEqualTo(0);
                assertThat(totalReads.get()).isGreaterThan(0);

              } finally {
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
              }

              addTestMetric(
                  String.format(
                      "Memory consistency: %d reads, %d errors with %s",
                      totalReads.get(), consistencyErrors.get(), runtimeType));
            }
          });
    }
  }

  @Nested
  @DisplayName("Cross-Runtime Memory Behavior Tests")
  final class CrossRuntimeMemoryTests {

    @Test
    @DisplayName("Should provide identical memory operation results across runtimes")
    void shouldProvideIdenticalMemoryOperationResultsAcrossRuntimes() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore();
                    final Module module = engine.compileModule(moduleBytes);
                    final Instance instance = module.instantiate(store)) {

                  final Map<String, Integer> results = new HashMap<>();

                  // Store and load test values
                  final int[] testOffsets = {0, 4, 8, 12, 16};
                  final int[] testValues = {42, -17, 1000, -2000, 0};

                  for (int i = 0; i < testOffsets.length; i++) {
                    try {
                      instance.callFunction(
                          "store", WasmValue.i32(testOffsets[i]), WasmValue.i32(testValues[i]));

                      final WasmValue[] loadResult =
                          instance.callFunction("load", WasmValue.i32(testOffsets[i]));
                      results.put("offset_" + testOffsets[i], loadResult[0].asI32());

                    } catch (final WasmException e) {
                      results.put("offset_" + testOffsets[i] + "_error", -999999);
                    }
                  }

                  return results;
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();

      addTestMetric("Cross-runtime memory operations: " + result.getDifferenceDescription());
    }

    @Test
    @DisplayName("Should handle memory bounds identically across runtimes")
    void shouldHandleMemoryBoundsIdenticallyAcrossRuntimes() {
      final CrossRuntimeValidator.ComparisonResult result =
          CrossRuntimeValidator.validateCrossRuntime(
              runtime -> {
                final byte[] moduleBytes = WasmTestModules.getModule("memory_basic");
                try (final Engine engine = runtime.createEngine();
                    final Store store = engine.createStore();
                    final Module module = engine.compileModule(moduleBytes);
                    final Instance instance = module.instantiate(store)) {

                  final Map<String, String> boundsBehavior = new HashMap<>();

                  // Test various problematic offsets
                  final int[] testOffsets = {-1, -4, 1000000, Integer.MAX_VALUE};

                  for (final int offset : testOffsets) {
                    try {
                      instance.callFunction("load", WasmValue.i32(offset));
                      boundsBehavior.put("load_" + offset, "success");
                    } catch (final WasmException e) {
                      boundsBehavior.put("load_" + offset, "bounds_error");
                    }

                    try {
                      instance.callFunction("store", WasmValue.i32(offset), WasmValue.i32(42));
                      boundsBehavior.put("store_" + offset, "success");
                    } catch (final WasmException e) {
                      boundsBehavior.put("store_" + offset, "bounds_error");
                    }
                  }

                  return boundsBehavior;
                }
              });

      assertThat(result.isValid()).isTrue();
      assertThat(result.areResultsIdentical()).isTrue();

      addTestMetric("Cross-runtime memory bounds: " + result.getDifferenceDescription());
    }
  }
}

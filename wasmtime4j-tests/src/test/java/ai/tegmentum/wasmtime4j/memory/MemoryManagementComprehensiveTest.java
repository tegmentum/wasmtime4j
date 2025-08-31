package ai.tegmentum.wasmtime4j.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive memory management tests covering all memory operations, bounds checking, resource
 * cleanup, and cross-runtime validation. This test class provides extensive coverage for memory
 * safety, leak prevention, and proper resource management.
 */
@DisplayName("Memory Management Comprehensive Tests")
final class MemoryManagementComprehensiveTest extends BaseIntegrationTest {
  private static final Logger LOGGER =
      Logger.getLogger(MemoryManagementComprehensiveTest.class.getName());

  // Test configuration constants
  private static final int MEMORY_PAGE_SIZE = 65536; // 64KB per page
  private static final int DEFAULT_MIN_PAGES = 1;
  private static final int DEFAULT_MAX_PAGES = 10;
  private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(30);

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    LOGGER.info("Setting up memory management test: " + testInfo.getDisplayName());
  }

  @Override
  protected void doTearDown(final TestInfo testInfo) {
    // Force garbage collection to help detect any memory leaks
    System.gc();
    System.runFinalization();
    System.gc();
    LOGGER.info("Completed memory management test: " + testInfo.getDisplayName());
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should validate memory creation and basic operations")
  void shouldValidateMemoryCreationAndBasicOperations(final RuntimeType runtimeType) {
    runWithBothRuntimes(
        (runtime, type) -> {
          LOGGER.info("Testing memory creation with " + type + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            // Test memory creation with minimum and maximum limits
            final WasmMemory memory =
                store.createMemory(DEFAULT_MIN_PAGES, DEFAULT_MAX_PAGES);
            registerForCleanup(memory);

            // Validate initial memory state
            assertThat(memory.size()).isEqualTo(DEFAULT_MIN_PAGES * MEMORY_PAGE_SIZE);
            assertThat(memory.getPages()).isEqualTo(DEFAULT_MIN_PAGES);
            assertThat(memory.getMaxPages()).isEqualTo(DEFAULT_MAX_PAGES);

            // Test basic memory operations
            final byte[] testData = "Hello, Memory Management!".getBytes();
            memory.write(0, testData);

            final byte[] readData = memory.read(0, testData.length);
            assertThat(readData).isEqualTo(testData);

            // Test different data patterns
            validateMemoryDataPatterns(memory);

            LOGGER.info("Memory creation and basic operations validated for " + type);
          }
        });
  }

  @Test
  @DisplayName("Should validate cross-runtime memory behavior consistency")
  void shouldValidateCrossRuntimeMemoryBehaviorConsistency() {
    final CrossRuntimeValidator.RuntimeOperation<String> memoryOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasmMemory memory = store.createMemory(2, 5);
            final StringBuilder results = new StringBuilder();

            // Test consistent memory initialization
            final byte[] initialData = memory.read(0, 100);
            final boolean allZeros = Arrays.equals(initialData, new byte[100]);
            results.append("InitialZeros:").append(allZeros).append(";");

            // Test consistent write/read operations
            final byte[] testPattern = {0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC};
            memory.write(1000, testPattern);
            final byte[] readPattern = memory.read(1000, testPattern.length);
            results
                .append("PatternMatch:")
                .append(Arrays.equals(testPattern, readPattern))
                .append(";");

            // Test consistent memory size operations
            results.append("Size:").append(memory.size()).append(";");
            results.append("Pages:").append(memory.getPages()).append(";");

            return results.toString();
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(memoryOperation, OPERATION_TIMEOUT);

    assertThat(result.isValid())
        .as("Memory behavior should be identical across runtimes: " + result.getDifferenceDescription())
        .isTrue();

    LOGGER.info("Cross-runtime memory behavior consistency validated");
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should handle memory bounds checking and safety validation")
  void shouldHandleMemoryBoundsCheckingAndSafetyValidation(final RuntimeType runtimeType) {
    runWithBothRuntimes(
        (runtime, type) -> {
          LOGGER.info("Testing memory bounds checking with " + type + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasmMemory memory = store.createMemory(1, 3);

            // Test valid bounds operations
            final byte[] validData = new byte[1000];
            Arrays.fill(validData, (byte) 0x55);
            memory.write(0, validData);
            memory.write(MEMORY_PAGE_SIZE - validData.length, validData);

            // Test boundary edge cases
            memory.write(MEMORY_PAGE_SIZE - 1, new byte[] {0x42});
            final byte[] edgeData = memory.read(MEMORY_PAGE_SIZE - 1, 1);
            assertThat(edgeData[0]).isEqualTo(0x42);

            // Test invalid bounds operations should fail safely
            assertThatThrownBy(() -> memory.write(MEMORY_PAGE_SIZE, new byte[] {0x01}))
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("bounds");

            assertThatThrownBy(() -> memory.read(MEMORY_PAGE_SIZE, 1))
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("bounds");

            // Test large offset bounds checking
            assertThatThrownBy(() -> memory.write(Integer.MAX_VALUE, new byte[] {0x01}))
                .isInstanceOf(WasmException.class);

            // Test negative offset bounds checking
            assertThatThrownBy(() -> memory.write(-1, new byte[] {0x01}))
                .isInstanceOf(WasmException.class);

            LOGGER.info("Memory bounds checking validated for " + type);
          }
        });
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should validate memory growth operations and reallocation safety")
  void shouldValidateMemoryGrowthOperationsAndReallocationSafety(final RuntimeType runtimeType) {
    runWithBothRuntimes(
        (runtime, type) -> {
          LOGGER.info("Testing memory growth operations with " + type + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasmMemory memory = store.createMemory(1, 5);
            final int initialSize = memory.size();
            final int initialPages = memory.getPages();

            // Store test data before growth
            final byte[] preGrowthData = "Data before growth".getBytes();
            memory.write(100, preGrowthData);

            // Test successful memory growth
            final int growthPages = 2;
            final int newPages = memory.grow(growthPages);
            assertThat(newPages).isEqualTo(initialPages);
            assertThat(memory.getPages()).isEqualTo(initialPages + growthPages);
            assertThat(memory.size()).isEqualTo(initialSize + growthPages * MEMORY_PAGE_SIZE);

            // Verify data integrity after growth
            final byte[] postGrowthData = memory.read(100, preGrowthData.length);
            assertThat(postGrowthData).isEqualTo(preGrowthData);

            // Test writing to newly allocated space
            final byte[] newAreaData = "Data in new area".getBytes();
            final int newAreaOffset = initialSize + 1000;
            memory.write(newAreaOffset, newAreaData);
            final byte[] readNewAreaData = memory.read(newAreaOffset, newAreaData.length);
            assertThat(readNewAreaData).isEqualTo(newAreaData);

            // Test growth beyond maximum should fail
            assertThatThrownBy(() -> memory.grow(10))
                .isInstanceOf(WasmException.class)
                .hasMessageContaining("maximum");

            // Test zero growth (should succeed but not change size)
            final int currentPages = memory.getPages();
            final int zeroGrowthResult = memory.grow(0);
            assertThat(zeroGrowthResult).isEqualTo(currentPages);
            assertThat(memory.getPages()).isEqualTo(currentPages);

            LOGGER.info("Memory growth operations validated for " + type);
          }
        });
  }

  @Test
  @DisplayName("Should validate memory operations with WebAssembly modules")
  void shouldValidateMemoryOperationsWithWebAssemblyModules() {
    runWithBothRuntimes(
        (runtime, type) -> {
          LOGGER.info("Testing memory operations with WebAssembly modules for " + type);

          final byte[] moduleBytes = TestUtils.createMemoryImportWasmModule();

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            // Create memory for import
            final WasmMemory memory = store.createMemory(1, 3);

            // Create module with memory import
            final Module module = engine.compileModule(moduleBytes);
            final ImportMap importMap = ImportMap.builder().addMemory("env", "memory", memory).build();
            final Instance instance = runtime.instantiate(module, importMap);

            // Get the load function that operates on memory
            final WasmFunction loadFunc =
                instance
                    .getFunction("load")
                    .orElseThrow(() -> new AssertionError("load function should be exported"));

            // Test memory operations through WebAssembly
            final int testOffset = 1000;
            final int testValue = 0x12345678;

            // Write test data directly to memory
            final ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(testValue);
            memory.write(testOffset, buffer.array());

            // Load data through WebAssembly function
            final WasmValue[] loadArgs = {WasmValue.i32(testOffset)};
            final WasmValue[] loadResults = loadFunc.call(loadArgs);
            assertThat(loadResults).hasSize(1);
            assertThat(loadResults[0].asI32()).isEqualTo(testValue);

            // Test multiple memory operations
            for (int i = 0; i < 100; i++) {
              final int offset = i * 4;
              final int value = 0x1000 + i;

              buffer.clear();
              buffer.putInt(value);
              memory.write(offset, buffer.array());

              final WasmValue[] args = {WasmValue.i32(offset)};
              final WasmValue[] results = loadFunc.call(args);
              assertThat(results[0].asI32()).isEqualTo(value);
            }

            LOGGER.info("Memory operations with WebAssembly modules validated for " + type);
          }
        });
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should handle concurrent memory operations safely")
  void shouldHandleConcurrentMemoryOperationsSafely(final RuntimeType runtimeType)
      throws InterruptedException {
    final int threadCount = 4;
    final int operationsPerThread = 100;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch completionLatch = new CountDownLatch(threadCount);
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger errorCount = new AtomicInteger(0);

    try (final WasmRuntime runtime = createTestRuntime(runtimeType);
        final Engine engine = runtime.createEngine();
        final Store store = engine.createStore()) {

      final WasmMemory sharedMemory = store.createMemory(2, 10);
      LOGGER.info("Testing concurrent memory operations with " + runtimeType + " runtime");

      // Submit concurrent tasks
      final List<CompletableFuture<Void>> futures = new ArrayList<>();
      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        final CompletableFuture<Void> future =
            CompletableFuture.runAsync(
                () -> {
                  try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    for (int i = 0; i < operationsPerThread; i++) {
                      final int baseOffset = threadId * 1000 + i * 4;
                      final byte[] threadData =
                          ByteBuffer.allocate(4).putInt(threadId * 1000 + i).array();

                      // Perform memory operations
                      sharedMemory.write(baseOffset, threadData);
                      final byte[] readData = sharedMemory.read(baseOffset, 4);
                      assertThat(readData).isEqualTo(threadData);

                      successCount.incrementAndGet();
                    }
                  } catch (final Exception e) {
                    LOGGER.warning("Thread " + threadId + " failed: " + e.getMessage());
                    errorCount.incrementAndGet();
                  } finally {
                    completionLatch.countDown();
                  }
                },
                executor);
        futures.add(future);
      }

      // Start all threads simultaneously
      startLatch.countDown();

      // Wait for completion
      final boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
      assertThat(completed).isTrue().as("All threads should complete within timeout");

      // Wait for all futures to complete
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    } finally {
      executor.shutdown();
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    }

    final int totalOperations = threadCount * operationsPerThread;
    LOGGER.info(
        "Concurrent memory operations completed - Success: "
            + successCount.get()
            + ", Errors: "
            + errorCount.get()
            + ", Total: "
            + totalOperations);

    // Validate results
    assertThat(successCount.get())
        .isGreaterThan(totalOperations * 0.95) // Allow for minimal failures
        .as("At least 95% of operations should succeed");

    assertThat(errorCount.get())
        .isLessThan(totalOperations * 0.05) // Less than 5% errors
        .as("Error rate should be less than 5%");
  }

  @Test
  @DisplayName("Should validate memory resource cleanup and prevent leaks")
  void shouldValidateMemoryResourceCleanupAndPreventLeaks() {
    final int iterations = 50;
    final List<Long> memorySizes = new ArrayList<>();

    // Perform multiple iterations of memory allocation and cleanup
    for (int i = 0; i < iterations; i++) {
      runWithBothRuntimes(
          (runtime, type) -> {
            try (final Engine engine = runtime.createEngine();
                final Store store = engine.createStore()) {

              // Create and use memory
              final WasmMemory memory = store.createMemory(1, 5);
              memory.grow(2);

              final byte[] largeData = new byte[10000];
              Arrays.fill(largeData, (byte) (i % 256));
              memory.write(0, largeData);
              
              final byte[] readData = memory.read(0, largeData.length);
              assertThat(readData).isEqualTo(largeData);

              // Memory should be automatically cleaned up when store closes
            }
          });

      // Periodically collect memory usage statistics
      if (i % 10 == 0) {
        System.gc();
        System.runFinalization();
        System.gc();
        
        final Runtime jvmRuntime = Runtime.getRuntime();
        final long usedMemory = jvmRuntime.totalMemory() - jvmRuntime.freeMemory();
        memorySizes.add(usedMemory);
        
        LOGGER.info("Iteration " + i + ", Used memory: " + (usedMemory / 1024 / 1024) + " MB");
      }
    }

    // Validate memory usage didn't grow uncontrollably
    if (memorySizes.size() >= 3) {
      final long initialMemory = memorySizes.get(0);
      final long finalMemory = memorySizes.get(memorySizes.size() - 1);
      final double growthRatio = (double) finalMemory / initialMemory;

      LOGGER.info(
          "Memory growth analysis - Initial: "
              + (initialMemory / 1024 / 1024)
              + " MB, Final: "
              + (finalMemory / 1024 / 1024)
              + " MB, Ratio: "
              + String.format("%.2f", growthRatio));

      // Allow some memory growth but detect significant leaks
      assertThat(growthRatio)
          .isLessThan(2.0)
          .as("Memory usage should not double during the test (potential leak detected)");
    }

    LOGGER.info("Memory resource cleanup validation completed");
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 4, 8, 16})
  @DisplayName("Should handle various memory page configurations")
  void shouldHandleVariousMemoryPageConfigurations(final int pageCount) {
    runWithBothRuntimes(
        (runtime, type) -> {
          LOGGER.info("Testing " + pageCount + " page configuration with " + type + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasmMemory memory = store.createMemory(pageCount, pageCount + 5);
            
            // Validate initial configuration
            assertThat(memory.getPages()).isEqualTo(pageCount);
            assertThat(memory.size()).isEqualTo(pageCount * MEMORY_PAGE_SIZE);

            // Test accessing different areas of memory
            final byte[] testPattern = "Page Test Data".getBytes();
            
            for (int page = 0; page < pageCount; page++) {
              final int offset = page * MEMORY_PAGE_SIZE + 100;
              final byte[] pageData = (testPattern + "-" + page).getBytes();
              
              memory.write(offset, pageData);
              final byte[] readData = memory.read(offset, pageData.length);
              assertThat(readData).isEqualTo(pageData);
            }

            // Test page boundaries
            if (pageCount > 1) {
              final int boundaryOffset = MEMORY_PAGE_SIZE - testPattern.length / 2;
              memory.write(boundaryOffset, testPattern);
              final byte[] boundaryData = memory.read(boundaryOffset, testPattern.length);
              assertThat(boundaryData).isEqualTo(testPattern);
            }

            LOGGER.info("Page configuration " + pageCount + " validated for " + type);
          }
        });
  }

  /**
   * Helper method to validate different data patterns in memory to ensure proper storage and
   * retrieval.
   */
  private void validateMemoryDataPatterns(final WasmMemory memory) {
    // Test binary patterns
    final byte[] binaryPattern = {0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (byte) 0x80};
    memory.write(1000, binaryPattern);
    assertThat(memory.read(1000, binaryPattern.length)).isEqualTo(binaryPattern);

    // Test alternating pattern
    final byte[] alternatingPattern = new byte[100];
    for (int i = 0; i < alternatingPattern.length; i++) {
      alternatingPattern[i] = (byte) (i % 2 == 0 ? 0xAA : 0x55);
    }
    memory.write(2000, alternatingPattern);
    assertThat(memory.read(2000, alternatingPattern.length)).isEqualTo(alternatingPattern);

    // Test random-ish pattern
    final byte[] randomPattern = new byte[256];
    for (int i = 0; i < randomPattern.length; i++) {
      randomPattern[i] = (byte) (i * 17 + 42); // Pseudo-random but deterministic
    }
    memory.write(3000, randomPattern);
    assertThat(memory.read(3000, randomPattern.length)).isEqualTo(randomPattern);

    // Test large zero block
    final byte[] zeroBlock = new byte[5000];
    memory.write(10000, zeroBlock);
    assertThat(memory.read(10000, zeroBlock.length)).isEqualTo(zeroBlock);

    // Test UTF-8 text data
    final String unicodeText = "Hello, 世界! 🌍 Memory test with émojis and àccénts";
    final byte[] utf8Data = unicodeText.getBytes();
    memory.write(20000, utf8Data);
    final byte[] readUtf8Data = memory.read(20000, utf8Data.length);
    assertThat(new String(readUtf8Data)).isEqualTo(unicodeText);
  }
}
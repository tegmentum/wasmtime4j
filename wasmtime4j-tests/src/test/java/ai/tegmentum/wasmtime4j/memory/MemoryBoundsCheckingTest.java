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
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
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
 * Comprehensive memory bounds checking tests to validate memory safety, overflow prevention, and
 * proper error handling for out-of-bounds access attempts. These tests are critical for preventing
 * JVM crashes and ensuring memory safety across all runtime implementations.
 */
@DisplayName("Memory Bounds Checking Tests")
final class MemoryBoundsCheckingTest extends BaseIntegrationTest {
  private static final Logger LOGGER = Logger.getLogger(MemoryBoundsCheckingTest.class.getName());

  private static final int MEMORY_PAGE_SIZE = 65536; // 64KB per page
  private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(10);

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    LOGGER.info("Setting up memory bounds checking test: " + testInfo.getDisplayName());
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should enforce strict bounds checking for memory write operations")
  void shouldEnforceStrictBoundsCheckingForMemoryWriteOperations(final RuntimeType runtimeType) {
    runWithBothRuntimes(
        (runtime, type) -> {
          LOGGER.info("Testing memory write bounds checking with " + type + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasmMemory memory = store.createMemory(2, 5); // 2 pages = 128KB
            final int memorySize = memory.size();

            // Test valid writes at boundaries
            final byte[] testData = "Boundary Test".getBytes();

            // Valid: Write at start
            memory.write(0, testData);
            assertThat(memory.read(0, testData.length)).isEqualTo(testData);

            // Valid: Write at end boundary
            memory.write(memorySize - testData.length, testData);
            assertThat(memory.read(memorySize - testData.length, testData.length))
                .isEqualTo(testData);

            // Valid: Write one byte at last position
            memory.write(memorySize - 1, new byte[] {0x42});
            assertThat(memory.read(memorySize - 1, 1)[0]).isEqualTo(0x42);

            // Invalid: Write beyond memory bounds
            assertThatThrownBy(() -> memory.write(memorySize, new byte[] {0x01}))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("bounds", "out of bounds", "invalid");

            // Invalid: Write starting within bounds but extending beyond
            assertThatThrownBy(() -> memory.write(memorySize - 5, new byte[10]))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("bounds", "out of bounds", "invalid");

            // Invalid: Write at negative offset
            assertThatThrownBy(() -> memory.write(-1, testData))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("bounds", "out of bounds", "invalid", "negative");

            // Invalid: Write at large positive offset
            assertThatThrownBy(() -> memory.write(Integer.MAX_VALUE, testData))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("bounds", "out of bounds", "invalid");

            LOGGER.info("Memory write bounds checking validated for " + type);
          }
        });
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should enforce strict bounds checking for memory read operations")
  void shouldEnforceStrictBoundsCheckingForMemoryReadOperations(final RuntimeType runtimeType) {
    runWithBothRuntimes(
        (runtime, type) -> {
          LOGGER.info("Testing memory read bounds checking with " + type + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasmMemory memory = store.createMemory(1, 3); // 1 page = 64KB
            final int memorySize = memory.size();

            // Prepare test data
            final byte[] referenceData = "Reference Data For Bounds Testing".getBytes();
            memory.write(1000, referenceData);

            // Test valid reads
            assertThat(memory.read(0, 100)).hasSize(100);
            assertThat(memory.read(1000, referenceData.length)).isEqualTo(referenceData);
            assertThat(memory.read(memorySize - 1, 1)).hasSize(1);
            assertThat(memory.read(memorySize - 100, 100)).hasSize(100);

            // Invalid: Read beyond memory bounds
            assertThatThrownBy(() -> memory.read(memorySize, 1))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("bounds", "out of bounds", "invalid");

            // Invalid: Read starting within bounds but extending beyond
            assertThatThrownBy(() -> memory.read(memorySize - 10, 20))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("bounds", "out of bounds", "invalid");

            // Invalid: Read from negative offset
            assertThatThrownBy(() -> memory.read(-1, 10))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("bounds", "out of bounds", "invalid", "negative");

            // Invalid: Read with zero length (edge case)
            assertThatThrownBy(() -> memory.read(0, 0))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("length", "invalid");

            // Invalid: Read with negative length
            assertThatThrownBy(() -> memory.read(0, -1))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("length", "invalid", "negative");

            // Invalid: Read at large positive offset
            assertThatThrownBy(() -> memory.read(Integer.MAX_VALUE, 1))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("bounds", "out of bounds", "invalid");

            LOGGER.info("Memory read bounds checking validated for " + type);
          }
        });
  }

  @Test
  @DisplayName("Should validate consistent bounds checking across runtimes")
  void shouldValidateConsistentBoundsCheckingAcrossRuntimes() {
    final CrossRuntimeValidator.RuntimeOperation<String> boundsCheckingOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasmMemory memory = store.createMemory(1, 2);
            final StringBuilder results = new StringBuilder();

            // Test various bounds scenarios and record results
            final int[][] testCases = {
              {0, 100}, // Valid: start of memory
              {MEMORY_PAGE_SIZE - 100, 100}, // Valid: end of memory
              {MEMORY_PAGE_SIZE, 1}, // Invalid: exactly at boundary
              {MEMORY_PAGE_SIZE + 1000, 100}, // Invalid: well beyond boundary
              {-1, 10}, // Invalid: negative offset
              {MEMORY_PAGE_SIZE - 10, 20} // Invalid: spans boundary
            };

            for (int i = 0; i < testCases.length; i++) {
              final int offset = testCases[i][0];
              final int length = testCases[i][1];

              try {
                memory.read(offset, length);
                results.append("T").append(i).append(":SUCCESS;");
              } catch (final WasmException e) {
                results.append("T").append(i).append(":EXCEPTION(")
                    .append(e.getClass().getSimpleName()).append(");");
              }
            }

            return results.toString();
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(boundsCheckingOperation, OPERATION_TIMEOUT);

    assertThat(result.isValid())
        .as("Bounds checking behavior should be identical across runtimes: " + result.getDifferenceDescription())
        .isTrue();

    LOGGER.info("Cross-runtime bounds checking consistency validated");
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should handle edge cases in memory bounds validation")
  void shouldHandleEdgeCasesInMemoryBoundsValidation(final RuntimeType runtimeType) {
    runWithBothRuntimes(
        (runtime, type) -> {
          LOGGER.info("Testing memory bounds edge cases with " + type + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasmMemory memory = store.createMemory(3, 8); // 3 pages = 192KB
            final int memorySize = memory.size();

            // Edge case: Maximum single-byte operations
            memory.write(0, new byte[] {(byte) 0xFF});
            memory.write(memorySize - 1, new byte[] {(byte) 0xAA});
            assertThat(memory.read(0, 1)[0]).isEqualTo((byte) 0xFF);
            assertThat(memory.read(memorySize - 1, 1)[0]).isEqualTo((byte) 0xAA);

            // Edge case: Large valid operations
            final byte[] largeData = new byte[MEMORY_PAGE_SIZE]; // Full page
            Arrays.fill(largeData, (byte) 0x55);
            memory.write(0, largeData);
            assertThat(memory.read(0, largeData.length)).isEqualTo(largeData);

            // Edge case: Cross-page boundary operations
            final byte[] crossPageData = new byte[1000];
            Arrays.fill(crossPageData, (byte) 0x77);
            final int crossPageOffset = MEMORY_PAGE_SIZE - 500; // Spans two pages
            memory.write(crossPageOffset, crossPageData);
            assertThat(memory.read(crossPageOffset, crossPageData.length)).isEqualTo(crossPageData);

            // Edge case: Attempt operations with maximum integer values
            assertThatThrownBy(() -> memory.write(Integer.MAX_VALUE - 1000, new byte[2000]))
                .isInstanceOf(WasmException.class);

            assertThatThrownBy(() -> memory.read(Integer.MAX_VALUE - 1000, 2000))
                .isInstanceOf(WasmException.class);

            // Edge case: Operations at page boundaries after growth
            memory.grow(2); // Now 5 pages
            final int newSize = memory.size();
            memory.write(newSize - 1, new byte[] {(byte) 0x99});
            assertThat(memory.read(newSize - 1, 1)[0]).isEqualTo((byte) 0x99);

            // Should still fail at new boundary
            assertThatThrownBy(() -> memory.write(newSize, new byte[] {0x01}))
                .isInstanceOf(WasmException.class);

            LOGGER.info("Memory bounds edge cases validated for " + type);
          }
        });
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should validate bounds checking with WebAssembly memory access")
  void shouldValidateBoundsCheckingWithWebAssemblyMemoryAccess(final RuntimeType runtimeType) {
    runWithBothRuntimes(
        (runtime, type) -> {
          LOGGER.info("Testing WebAssembly memory access bounds with " + type + " runtime");

          final byte[] moduleBytes = TestUtils.createMemoryImportWasmModule();

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasmMemory memory = store.createMemory(2, 4); // 128KB
            final Module module = engine.compileModule(moduleBytes);
            final ImportMap importMap = ImportMap.builder()
                .addMemory("env", "memory", memory)
                .build();
            final Instance instance = runtime.instantiate(module, importMap);

            final WasmFunction loadFunc = instance.getFunction("load")
                .orElseThrow(() -> new AssertionError("load function required"));

            // Test valid WebAssembly memory access
            final int validOffset = 1000;
            final int testValue = 0x12345678;
            
            // Write through direct memory API
            final ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(testValue);
            memory.write(validOffset, buffer.array());

            // Read through WebAssembly function
            final WasmValue[] result = loadFunc.call(WasmValue.i32(validOffset));
            assertThat(result[0].asI32()).isEqualTo(testValue);

            // Test bounds checking through WebAssembly
            final int memorySize = memory.size();
            
            // Valid: Read near end of memory
            memory.write(memorySize - 4, buffer.array());
            final WasmValue[] validResult = loadFunc.call(WasmValue.i32(memorySize - 4));
            assertThat(validResult[0].asI32()).isEqualTo(testValue);

            // Invalid: WebAssembly access beyond bounds should fail
            // Note: WebAssembly bounds checking might manifest differently
            // but should still prevent invalid access
            assertThatThrownBy(() -> loadFunc.call(WasmValue.i32(memorySize)))
                .isInstanceOf(Exception.class); // Could be WasmException or execution exception

            assertThatThrownBy(() -> loadFunc.call(WasmValue.i32(memorySize + 1000)))
                .isInstanceOf(Exception.class);

            assertThatThrownBy(() -> loadFunc.call(WasmValue.i32(-1)))
                .isInstanceOf(Exception.class);

            LOGGER.info("WebAssembly memory access bounds validated for " + type);
          }
        });
  }

  @Test
  @DisplayName("Should maintain bounds checking under concurrent access")
  void shouldMaintainBoundsCheckingUnderConcurrentAccess() throws InterruptedException {
    final int threadCount = 6;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final AtomicInteger boundsViolationsCaught = new AtomicInteger(0);
    final AtomicInteger totalOperations = new AtomicInteger(0);

    runWithBothRuntimes(
        (runtime, type) -> {
          LOGGER.info("Testing concurrent bounds checking with " + type + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasmMemory memory = store.createMemory(2, 6); // 128KB, growable
            final CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];

            for (int t = 0; t < threadCount; t++) {
              final int threadId = t;
              futures[t] = CompletableFuture.runAsync(
                  () -> {
                    for (int i = 0; i < 100; i++) {
                      totalOperations.incrementAndGet();

                      try {
                        // Valid operations (should succeed)
                        final int validOffset = threadId * 1000 + i * 4;
                        if (validOffset < memory.size() - 10) {
                          final byte[] data = ("T" + threadId + "I" + i).getBytes();
                          memory.write(validOffset, data);
                          final byte[] readData = memory.read(validOffset, data.length);
                          assertThat(readData).isEqualTo(data);
                        }

                        // Intentionally invalid operations (should fail safely)
                        if (i % 20 == 0) {
                          try {
                            memory.write(memory.size() + threadId * 100, new byte[] {0x01});
                            // Should not reach here
                            throw new AssertionError("Expected bounds violation not caught");
                          } catch (final WasmException expected) {
                            boundsViolationsCaught.incrementAndGet();
                          }
                        }

                      } catch (final Exception e) {
                        if (e instanceof WasmException && 
                            (e.getMessage().contains("bounds") || 
                             e.getMessage().contains("invalid"))) {
                          boundsViolationsCaught.incrementAndGet();
                        } else {
                          throw new RuntimeException("Unexpected error in thread " + threadId, e);
                        }
                      }
                    }
                  },
                  executor);
            }

            // Wait for all threads to complete
            CompletableFuture.allOf(futures).join();

          } finally {
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
              executor.shutdownNow();
            }
          }
        });

    LOGGER.info("Concurrent bounds checking completed - Operations: " + totalOperations.get() 
        + ", Bounds violations caught: " + boundsViolationsCaught.get());

    // Validate that bounds violations were properly caught
    assertThat(boundsViolationsCaught.get())
        .isGreaterThan(0)
        .as("Bounds violations should have been caught and handled properly");

    // Validate that most operations were successful
    assertThat(totalOperations.get())
        .isGreaterThan(boundsViolationsCaught.get())
        .as("Valid operations should have succeeded");
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 5, 10, 16})
  @DisplayName("Should enforce bounds checking across different memory sizes")
  void shouldEnforceBoundsCheckingAcrossDifferentMemorySizes(final int pageCount) {
    runWithBothRuntimes(
        (runtime, type) -> {
          LOGGER.info("Testing bounds checking with " + pageCount + " pages using " + type + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasmMemory memory = store.createMemory(pageCount, pageCount + 5);
            final int memorySize = memory.size();
            
            // Validate expected size
            assertThat(memorySize).isEqualTo(pageCount * MEMORY_PAGE_SIZE);

            // Test valid operations within bounds
            final byte[] testData = ("PageCount" + pageCount).getBytes();
            memory.write(0, testData);
            memory.write(memorySize - testData.length, testData);
            
            assertThat(memory.read(0, testData.length)).isEqualTo(testData);
            assertThat(memory.read(memorySize - testData.length, testData.length)).isEqualTo(testData);

            // Test invalid operations beyond bounds (should always fail regardless of size)
            assertThatThrownBy(() -> memory.write(memorySize, new byte[] {0x01}))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("bounds", "out of bounds", "invalid");

            assertThatThrownBy(() -> memory.read(memorySize, 1))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("bounds", "out of bounds", "invalid");

            // Test operations that span the boundary
            assertThatThrownBy(() -> memory.write(memorySize - 5, new byte[10]))
                .isInstanceOf(WasmException.class)
                .hasMessageContainingAny("bounds", "out of bounds", "invalid");

            LOGGER.info("Bounds checking validated for " + pageCount + " pages with " + type);
          }
        });
  }

  @Test
  @DisplayName("Should validate bounds checking recovery after failed operations")
  void shouldValidateBoundsCheckingRecoveryAfterFailedOperations() {
    runWithBothRuntimes(
        (runtime, type) -> {
          LOGGER.info("Testing bounds checking recovery with " + type + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasmMemory memory = store.createMemory(2, 4);
            final byte[] testData = "Recovery Test Data".getBytes();

            // Perform valid operation first
            memory.write(1000, testData);
            assertThat(memory.read(1000, testData.length)).isEqualTo(testData);

            // Attempt several invalid operations
            for (int i = 0; i < 5; i++) {
              try {
                memory.write(memory.size() + i * 100, testData);
                throw new AssertionError("Expected bounds violation not caught");
              } catch (final WasmException expected) {
                // Expected - bounds violation should be caught
              }
            }

            // Verify memory is still functional after failed operations
            final byte[] recoveryData = ("Recovery " + System.currentTimeMillis()).getBytes();
            memory.write(2000, recoveryData);
            assertThat(memory.read(2000, recoveryData.length)).isEqualTo(recoveryData);

            // Verify original data is still intact
            assertThat(memory.read(1000, testData.length)).isEqualTo(testData);

            // Test read recovery after failed read operations
            for (int i = 0; i < 3; i++) {
              try {
                memory.read(memory.size() + i * 1000, 100);
                throw new AssertionError("Expected bounds violation not caught");
              } catch (final WasmException expected) {
                // Expected
              }
            }

            // Verify reads still work correctly
            assertThat(memory.read(1000, testData.length)).isEqualTo(testData);
            assertThat(memory.read(2000, recoveryData.length)).isEqualTo(recoveryData);

            LOGGER.info("Bounds checking recovery validated for " + type);
          }
        });
  }
}
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Comprehensive unit tests for WebAssembly bulk memory operations.
 *
 * <p>Tests the bulk memory operations proposal including: - memory.copy (copy within same memory) -
 * memory.fill (fill memory region with value) - memory.init (initialize memory from data segment) -
 * data.drop (drop data segment)
 *
 * <p>All tests include comprehensive bounds checking and error validation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BulkMemoryOperationsTest {

  private static final Logger LOGGER = Logger.getLogger(BulkMemoryOperationsTest.class.getName());

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private WasmMemory memory;

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up bulk memory operations test environment");

    try {
      // Create runtime with bulk memory operations enabled
      runtime = WasmRuntimeFactory.create();

      // Create engine with bulk memory feature enabled
      EngineConfig config = new EngineConfig();
      config.enableBulkMemory(true);
      engine = runtime.createEngine(config);

      // Create store
      store = runtime.createStore(engine);

      // Create memory with sufficient size for testing (1 page = 64KB)
      MemoryType memoryType = new MemoryType(2, 4); // 2 initial pages, 4 max pages
      memory = runtime.createMemory(store, memoryType);

      LOGGER.info("Test environment setup completed successfully");
    } catch (Exception e) {
      LOGGER.severe("Failed to setup test environment: " + e.getMessage());
      throw new RuntimeException("Test setup failed", e);
    }
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test environment");

    try {
      if (memory != null) {
        memory.close();
      }
      if (store != null) {
        store.close();
      }
      if (engine != null) {
        engine.close();
      }
      if (runtime != null) {
        runtime.close();
      }
    } catch (Exception e) {
      LOGGER.warning("Error during test cleanup: " + e.getMessage());
    }
  }

  @Nested
  @DisplayName("Memory Copy Operations")
  class MemoryCopyTests {

    @Test
    @DisplayName("should copy non-overlapping memory regions correctly")
    void testNonOverlappingMemoryCopy() {
      // Prepare test data
      byte[] sourceData = {1, 2, 3, 4, 5, 6, 7, 8};
      int srcOffset = 0;
      int destOffset = 1000;
      int length = sourceData.length;

      // Write source data to memory
      memory.writeBytes(srcOffset, sourceData, 0, length);

      // Perform memory copy
      assertDoesNotThrow(
          () -> memory.copy(destOffset, srcOffset, length),
          "Memory copy should not throw for valid non-overlapping regions");

      // Verify copied data
      byte[] copiedData = new byte[length];
      memory.readBytes(destOffset, copiedData, 0, length);

      assertArrayEquals(sourceData, copiedData, "Copied data should match original source data");

      LOGGER.info("Non-overlapping memory copy test passed");
    }

    @Test
    @DisplayName("should handle overlapping memory regions correctly (forward)")
    void testForwardOverlappingMemoryCopy() {
      // Prepare test data with overlapping forward copy
      byte[] sourceData = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
      int srcOffset = 0;
      int destOffset = 4; // Overlaps with source
      int length = 6;

      // Write source data to memory
      memory.writeBytes(srcOffset, sourceData, 0, sourceData.length);

      // Perform overlapping copy
      assertDoesNotThrow(
          () -> memory.copy(destOffset, srcOffset, length),
          "Memory copy should handle forward overlapping regions correctly");

      // Verify the overlapping copy worked correctly
      byte[] result = new byte[sourceData.length];
      memory.readBytes(0, result, 0, sourceData.length);

      // Expected result: [1, 2, 3, 4, 1, 2, 3, 4, 5, 6]
      byte[] expected = {1, 2, 3, 4, 1, 2, 3, 4, 5, 6};
      assertArrayEquals(expected, result, "Forward overlapping copy should produce correct result");

      LOGGER.info("Forward overlapping memory copy test passed");
    }

    @Test
    @DisplayName("should handle overlapping memory regions correctly (backward)")
    void testBackwardOverlappingMemoryCopy() {
      // Prepare test data with overlapping backward copy
      byte[] sourceData = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
      int srcOffset = 4;
      int destOffset = 0; // Overlaps with source
      int length = 6;

      // Write source data to memory
      memory.writeBytes(0, sourceData, 0, sourceData.length);

      // Perform overlapping copy
      assertDoesNotThrow(
          () -> memory.copy(destOffset, srcOffset, length),
          "Memory copy should handle backward overlapping regions correctly");

      // Verify the overlapping copy worked correctly
      byte[] result = new byte[sourceData.length];
      memory.readBytes(0, result, 0, sourceData.length);

      // Expected result: [5, 6, 7, 8, 9, 10, 7, 8, 9, 10]
      byte[] expected = {5, 6, 7, 8, 9, 10, 7, 8, 9, 10};
      assertArrayEquals(
          expected, result, "Backward overlapping copy should produce correct result");

      LOGGER.info("Backward overlapping memory copy test passed");
    }

    @Test
    @DisplayName("should throw exception for out of bounds copy destination")
    void testCopyDestinationOutOfBounds() {
      int memorySize = memory.getSize() * 65536; // Convert pages to bytes
      int srcOffset = 0;
      int destOffset = memorySize - 10; // Near end of memory
      int length = 20; // Would exceed memory bounds

      IndexOutOfBoundsException exception =
          assertThrows(
              IndexOutOfBoundsException.class,
              () -> memory.copy(destOffset, srcOffset, length),
              "Should throw IndexOutOfBoundsException for destination out of bounds");

      assertTrue(
          exception.getMessage().contains("exceeds memory bounds"),
          "Exception message should indicate bounds violation");

      LOGGER.info("Copy destination bounds checking test passed");
    }

    @Test
    @DisplayName("should throw exception for out of bounds copy source")
    void testCopySourceOutOfBounds() {
      int memorySize = memory.getSize() * 65536; // Convert pages to bytes
      int srcOffset = memorySize - 10; // Near end of memory
      int destOffset = 0;
      int length = 20; // Would exceed memory bounds

      IndexOutOfBoundsException exception =
          assertThrows(
              IndexOutOfBoundsException.class,
              () -> memory.copy(destOffset, srcOffset, length),
              "Should throw IndexOutOfBoundsException for source out of bounds");

      assertTrue(
          exception.getMessage().contains("exceeds memory bounds"),
          "Exception message should indicate bounds violation");

      LOGGER.info("Copy source bounds checking test passed");
    }

    @Test
    @DisplayName("should throw exception for negative offsets or length")
    void testCopyNegativeParameters() {
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.copy(-1, 0, 10),
          "Should throw for negative destination offset");

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.copy(0, -1, 10),
          "Should throw for negative source offset");

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.copy(0, 0, -1),
          "Should throw for negative length");

      LOGGER.info("Copy negative parameters validation test passed");
    }

    @Test
    @DisplayName("should handle zero-length copy correctly")
    void testZeroLengthCopy() {
      assertDoesNotThrow(
          () -> memory.copy(100, 200, 0), "Zero-length copy should not throw exception");

      LOGGER.info("Zero-length copy test passed");
    }
  }

  @Nested
  @DisplayName("Memory Fill Operations")
  class MemoryFillTests {

    @Test
    @DisplayName("should fill memory region with specified byte value")
    void testMemoryFill() {
      int offset = 100;
      byte fillValue = (byte) 0xAB;
      int length = 50;

      // Fill memory region
      assertDoesNotThrow(
          () -> memory.fill(offset, fillValue, length),
          "Memory fill should not throw for valid parameters");

      // Verify fill operation
      byte[] result = new byte[length];
      memory.readBytes(offset, result, 0, length);

      for (int i = 0; i < length; i++) {
        assertEquals(fillValue, result[i], "All bytes in filled region should have fill value");
      }

      LOGGER.info("Memory fill test passed");
    }

    @Test
    @DisplayName("should fill entire memory page correctly")
    void testFillEntirePage() {
      int offset = 0;
      byte fillValue = (byte) 0xFF;
      int length = 65536; // One full page

      // Fill entire page
      assertDoesNotThrow(
          () -> memory.fill(offset, fillValue, length),
          "Filling entire page should not throw exception");

      // Verify a sample of the filled data (checking all would be expensive)
      byte[] sample = new byte[100];
      memory.readBytes(1000, sample, 0, sample.length);

      for (byte b : sample) {
        assertEquals(fillValue, b, "Sample bytes should have fill value");
      }

      LOGGER.info("Fill entire page test passed");
    }

    @Test
    @DisplayName("should throw exception for fill out of bounds")
    void testFillOutOfBounds() {
      int memorySize = memory.getSize() * 65536; // Convert pages to bytes
      int offset = memorySize - 10;
      byte fillValue = 0x42;
      int length = 20; // Would exceed memory bounds

      IndexOutOfBoundsException exception =
          assertThrows(
              IndexOutOfBoundsException.class,
              () -> memory.fill(offset, fillValue, length),
              "Should throw IndexOutOfBoundsException for fill out of bounds");

      assertTrue(
          exception.getMessage().contains("exceeds memory bounds"),
          "Exception message should indicate bounds violation");

      LOGGER.info("Fill bounds checking test passed");
    }

    @Test
    @DisplayName("should throw exception for negative fill parameters")
    void testFillNegativeParameters() {
      byte fillValue = 0x42;

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.fill(-1, fillValue, 10),
          "Should throw for negative offset");

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.fill(0, fillValue, -1),
          "Should throw for negative length");

      LOGGER.info("Fill negative parameters validation test passed");
    }

    @Test
    @DisplayName("should handle zero-length fill correctly")
    void testZeroLengthFill() {
      byte fillValue = 0x42;

      assertDoesNotThrow(
          () -> memory.fill(100, fillValue, 0), "Zero-length fill should not throw exception");

      LOGGER.info("Zero-length fill test passed");
    }

    @Test
    @DisplayName("should fill with all possible byte values")
    void testFillAllByteValues() {
      int offset = 0;
      int length = 10;

      // Test filling with all possible byte values
      for (int i = 0; i < 256; i++) {
        byte fillValue = (byte) i;

        memory.fill(offset, fillValue, length);

        byte[] result = new byte[length];
        memory.readBytes(offset, result, 0, length);

        for (byte b : result) {
          assertEquals(fillValue, b, "Fill should work correctly for byte value " + i);
        }
      }

      LOGGER.info("Fill all byte values test passed");
    }
  }

  @Nested
  @DisplayName("Data Segment Operations")
  class DataSegmentTests {

    @Test
    @DisplayName("should throw exception for memory.init without module context")
    void testMemoryInitWithoutModule() {
      int destOffset = 0;
      int dataSegmentIndex = 0;
      int srcOffset = 0;
      int length = 10;

      // memory.init requires module/instance context which is not available in isolated memory
      // tests
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> memory.init(destOffset, dataSegmentIndex, srcOffset, length),
              "memory.init should throw when module context is not available");

      assertTrue(
          exception.getMessage().contains("module") || exception.getMessage().contains("context"),
          "Exception should indicate missing module context");

      LOGGER.info("Memory init without module context test passed");
    }

    @Test
    @DisplayName("should throw exception for data.drop without module context")
    void testDataDropWithoutModule() {
      int dataSegmentIndex = 0;

      // data.drop requires module/instance context which is not available in isolated memory tests
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> memory.dropDataSegment(dataSegmentIndex),
              "data.drop should throw when module context is not available");

      assertTrue(
          exception.getMessage().contains("module") || exception.getMessage().contains("context"),
          "Exception should indicate missing module context");

      LOGGER.info("Data drop without module context test passed");
    }

    @Test
    @DisplayName("should validate parameters for memory.init")
    void testMemoryInitParameterValidation() {
      // Test negative destination offset
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.init(-1, 0, 0, 10),
          "Should throw for negative destination offset");

      // Test negative data segment index
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.init(0, -1, 0, 10),
          "Should throw for negative data segment index");

      // Test negative source offset
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.init(0, 0, -1, 10),
          "Should throw for negative source offset");

      // Test negative length
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.init(0, 0, 0, -1),
          "Should throw for negative length");

      LOGGER.info("Memory init parameter validation test passed");
    }

    @Test
    @DisplayName("should validate parameters for data.drop")
    void testDataDropParameterValidation() {
      // Test negative data segment index
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.dropDataSegment(-1),
          "Should throw for negative data segment index");

      LOGGER.info("Data drop parameter validation test passed");
    }
  }

  @Nested
  @DisplayName("Performance and Edge Cases")
  class PerformanceAndEdgeCases {

    @Test
    @DisplayName("should handle large bulk operations efficiently")
    void testLargeBulkOperations() {
      // Test with large data sizes to verify performance
      int largeSize = 32768; // Half a page
      int srcOffset = 0;
      int destOffset = largeSize;
      byte fillValue = (byte) 0x55;

      // Measure fill operation performance
      long startTime = System.nanoTime();
      memory.fill(srcOffset, fillValue, largeSize);
      long fillTime = System.nanoTime() - startTime;

      // Measure copy operation performance
      startTime = System.nanoTime();
      memory.copy(destOffset, srcOffset, largeSize);
      long copyTime = System.nanoTime() - startTime;

      // Verify operations completed correctly
      byte[] sample = new byte[100];
      memory.readBytes(destOffset + 1000, sample, 0, sample.length);

      for (byte b : sample) {
        assertEquals(fillValue, b, "Large copy operation should preserve data correctly");
      }

      LOGGER.info(
          "Large bulk operations test passed - Fill: "
              + fillTime / 1000000
              + "ms, Copy: "
              + copyTime / 1000000
              + "ms");
    }

    @Test
    @DisplayName("should handle memory operations at page boundaries")
    void testPageBoundaryOperations() {
      int pageSize = 65536;
      int nearBoundaryOffset = pageSize - 100;
      int crossBoundaryLength = 200; // Crosses page boundary
      byte fillValue = (byte) 0xCC;

      // Test fill across page boundary
      assertDoesNotThrow(
          () -> memory.fill(nearBoundaryOffset, fillValue, crossBoundaryLength),
          "Fill operation should work across page boundaries");

      // Test copy across page boundary
      int destOffset = nearBoundaryOffset + 1000;
      assertDoesNotThrow(
          () -> memory.copy(destOffset, nearBoundaryOffset, crossBoundaryLength),
          "Copy operation should work across page boundaries");

      LOGGER.info("Page boundary operations test passed");
    }

    @Test
    @DisplayName("should handle concurrent bulk operations safely")
    void testConcurrentBulkOperations() throws InterruptedException {
      final int numThreads = 4;
      final int operationsPerThread = 100;
      Thread[] threads = new Thread[numThreads];

      // Create threads that perform bulk operations concurrently
      for (int i = 0; i < numThreads; i++) {
        final int threadId = i;
        threads[i] =
            new Thread(
                () -> {
                  try {
                    for (int j = 0; j < operationsPerThread; j++) {
                      int offset = threadId * 1000 + j * 10;
                      byte fillValue = (byte) (threadId + j);

                      // Perform fill operation
                      memory.fill(offset, fillValue, 8);

                      // Verify the operation
                      byte[] result = new byte[8];
                      memory.readBytes(offset, result, 0, 8);

                      for (byte b : result) {
                        assertEquals(fillValue, b, "Concurrent operations should not interfere");
                      }
                    }
                  } catch (Exception e) {
                    fail(
                        "Concurrent bulk operations should not throw exceptions: "
                            + e.getMessage());
                  }
                });
      }

      // Start all threads
      for (Thread thread : threads) {
        thread.start();
      }

      // Wait for all threads to complete
      for (Thread thread : threads) {
        thread.join();
      }

      LOGGER.info("Concurrent bulk operations test passed");
    }
  }

  @Nested
  @DisplayName("Error Recovery and Robustness")
  class ErrorRecoveryTests {

    @Test
    @DisplayName("should maintain memory integrity after failed operations")
    void testMemoryIntegrityAfterFailure() {
      // Fill memory with known pattern
      byte initialValue = (byte) 0x42;
      memory.fill(0, initialValue, 1000);

      // Attempt an operation that should fail
      try {
        memory.copy(0, 0, Integer.MAX_VALUE); // Should fail due to bounds
        fail("Expected IndexOutOfBoundsException");
      } catch (IndexOutOfBoundsException expected) {
        // Expected failure
      }

      // Verify memory integrity is maintained
      byte[] result = new byte[100];
      memory.readBytes(0, result, 0, result.length);

      for (byte b : result) {
        assertEquals(initialValue, b, "Memory should maintain integrity after failed operations");
      }

      LOGGER.info("Memory integrity after failure test passed");
    }

    @Test
    @DisplayName("should handle memory growth during bulk operations")
    void testBulkOperationsWithMemoryGrowth() {
      // Grow memory
      int previousSize = memory.grow(1); // Add one page
      assertTrue(previousSize >= 0, "Memory growth should succeed");

      // Perform bulk operations in the new memory region
      int newRegionOffset = (previousSize + 1) * 65536 - 1000; // Near the end of new region
      byte fillValue = (byte) 0x99;
      int length = 500;

      assertDoesNotThrow(
          () -> memory.fill(newRegionOffset, fillValue, length),
          "Bulk operations should work in grown memory regions");

      // Verify the operation
      byte[] result = new byte[length];
      memory.readBytes(newRegionOffset, result, 0, length);

      for (byte b : result) {
        assertEquals(fillValue, b, "Bulk operations should work correctly in grown memory");
      }

      LOGGER.info("Bulk operations with memory growth test passed");
    }
  }
}

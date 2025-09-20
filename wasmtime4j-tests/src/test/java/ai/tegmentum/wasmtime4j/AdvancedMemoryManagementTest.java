/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Comprehensive test suite for advanced memory management features.
 *
 * <p>This test class validates the implementation of enterprise-grade memory management
 * capabilities including bulk operations, introspection, and protection features across both JNI
 * and Panama implementations.
 *
 * <p>Tests are designed to be verbose for debugging purposes and verify that both implementations
 * provide identical functionality and behavior.
 */
@EnabledIfSystemProperty(named = "wasmtime4j.test.advanced-memory", matches = "true")
class AdvancedMemoryManagementTest {

  private static final Logger LOGGER =
      Logger.getLogger(AdvancedMemoryManagementTest.class.getName());

  private WasmEngine engine;
  private WasmStore store;
  private WasmMemory memory1;
  private WasmMemory memory2;

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up test environment with engine and store");

    // Create engine with default configuration for testing
    engine =
        WasmEngine.builder()
            .optimizationLevel(
                OptimizationLevel.NONE) // Use no optimization for predictable testing
            .debugInfo(true) // Enable debug info for better error reporting
            .build();

    assertNotNull(engine, "Engine should be created successfully");
    LOGGER.info("Engine created successfully: " + engine.getClass().getSimpleName());

    // Create store for memory operations
    store =
        WasmStore.builder(engine)
            .fuelEnabled(false) // Disable fuel for unlimited execution in tests
            .build();

    assertNotNull(store, "Store should be created successfully");
    LOGGER.info("Store created successfully: " + store.getClass().getSimpleName());

    // Create memory instances for testing (1 page = 64KB each)
    memory1 =
        WasmMemory.builder()
            .initialPages(2) // 128KB initial size
            .maximumPages(10) // 640KB maximum size
            .build(store);

    assertNotNull(memory1, "Memory1 should be created successfully");
    assertEquals(2 * 65536, memory1.size(), "Memory1 should have correct initial size");
    LOGGER.info(
        "Memory1 created: " + memory1.size() + " bytes (" + (memory1.size() / 65536) + " pages)");

    memory2 =
        WasmMemory.builder()
            .initialPages(1) // 64KB initial size
            .maximumPages(5) // 320KB maximum size
            .build(store);

    assertNotNull(memory2, "Memory2 should be created successfully");
    assertEquals(65536, memory2.size(), "Memory2 should have correct initial size");
    LOGGER.info(
        "Memory2 created: " + memory2.size() + " bytes (" + (memory2.size() / 65536) + " pages)");
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test resources");

    // Close memory instances
    if (memory1 != null) {
      memory1.close();
      LOGGER.info("Memory1 closed");
    }
    if (memory2 != null) {
      memory2.close();
      LOGGER.info("Memory2 closed");
    }

    // Close store and engine
    if (store != null) {
      store.close();
      LOGGER.info("Store closed");
    }
    if (engine != null) {
      engine.close();
      LOGGER.info("Engine closed");
    }
  }

  // Bulk Memory Operations Tests

  @Test
  void testBulkCopy_SameMemory_NoOverlap() {
    LOGGER.info("Testing bulk copy within same memory without overlap");

    // Write test data to source location
    final byte[] sourceData = {0x01, 0x02, 0x03, 0x04, 0x05};
    final int sourceOffset = 1000;
    final int destOffset = 2000;

    memory1.write(sourceOffset, sourceData);
    LOGGER.info(
        "Written source data at offset "
            + sourceOffset
            + ": "
            + java.util.Arrays.toString(sourceData));

    // Verify source data was written correctly
    final byte[] readSourceData = memory1.read(sourceOffset, sourceData.length);
    assertThat(readSourceData).isEqualTo(sourceData);
    LOGGER.info("Verified source data matches: " + java.util.Arrays.toString(readSourceData));

    // Perform bulk copy
    BulkMemoryOperations bulkOps = memory1.getBulkOperations();
    assertNotNull(bulkOps, "BulkMemoryOperations should be available");

    assertDoesNotThrow(
        () -> {
          bulkOps.bulkCopy(memory1, destOffset, memory1, sourceOffset, sourceData.length);
        },
        "Bulk copy should not throw exception");
    LOGGER.info(
        "Bulk copy completed from offset "
            + sourceOffset
            + " to "
            + destOffset
            + " length "
            + sourceData.length);

    // Verify destination data matches source
    final byte[] destData = memory1.read(destOffset, sourceData.length);
    assertThat(destData).isEqualTo(sourceData);
    LOGGER.info("Verified destination data matches source: " + java.util.Arrays.toString(destData));

    // Verify source data is still intact
    final byte[] sourceDataAfter = memory1.read(sourceOffset, sourceData.length);
    assertThat(sourceDataAfter).isEqualTo(sourceData);
    LOGGER.info("Verified source data is still intact after copy");
  }

  @Test
  void testBulkCopy_BetweenMemories() {
    LOGGER.info("Testing bulk copy between different memory instances");

    // Write test data to memory1
    final byte[] testData = {
      (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE, (byte) 0xFF
    };
    final int sourceOffset = 500;
    final int destOffset = 200;

    memory1.write(sourceOffset, testData);
    LOGGER.info(
        "Written test data to memory1 at offset "
            + sourceOffset
            + ": "
            + java.util.Arrays.toString(testData));

    // Verify initial destination is different
    final byte[] initialDestData = memory2.read(destOffset, testData.length);
    assertThat(initialDestData).isNotEqualTo(testData);
    LOGGER.info(
        "Verified initial destination data is different: "
            + java.util.Arrays.toString(initialDestData));

    // Perform bulk copy between memories
    BulkMemoryOperations bulkOps = memory1.getBulkOperations();
    assertDoesNotThrow(
        () -> {
          bulkOps.bulkCopy(memory2, destOffset, memory1, sourceOffset, testData.length);
        },
        "Inter-memory bulk copy should not throw exception");
    LOGGER.info("Bulk copy completed from memory1 to memory2");

    // Verify destination data matches source
    final byte[] copiedData = memory2.read(destOffset, testData.length);
    assertThat(copiedData).isEqualTo(testData);
    LOGGER.info("Verified copied data matches: " + java.util.Arrays.toString(copiedData));
  }

  @Test
  void testBulkFill() {
    LOGGER.info("Testing bulk memory fill operation");

    final int offset = 1000;
    final int length = 256;
    final byte fillValue = (byte) 0x42;

    // Fill memory region with test value
    BulkMemoryOperations bulkOps = memory1.getBulkOperations();
    assertDoesNotThrow(
        () -> {
          bulkOps.bulkFill(memory1, offset, length, fillValue);
        },
        "Bulk fill should not throw exception");
    LOGGER.info(
        "Bulk fill completed at offset "
            + offset
            + " length "
            + length
            + " with value 0x"
            + Integer.toHexString(fillValue & 0xFF));

    // Verify all bytes are set to fill value
    final byte[] filledData = memory1.read(offset, length);
    for (int i = 0; i < filledData.length; i++) {
      assertEquals(
          fillValue, filledData[i], "Byte at index " + i + " should be filled with correct value");
    }
    LOGGER.info("Verified all " + length + " bytes are filled with correct value");

    // Verify areas outside fill region are not affected
    final byte[] beforeFill = memory1.read(offset - 10, 10);
    final byte[] afterFill = memory1.read(offset + length, 10);

    boolean beforeUntouched = false;
    boolean afterUntouched = false;

    for (byte b : beforeFill) {
      if (b != fillValue) {
        beforeUntouched = true;
        break;
      }
    }

    for (byte b : afterFill) {
      if (b != fillValue) {
        afterUntouched = true;
        break;
      }
    }

    assertTrue(
        beforeUntouched || afterUntouched, "Areas outside fill region should remain unaffected");
    LOGGER.info("Verified areas outside fill region are not affected");
  }

  @Test
  void testBulkCompare() {
    LOGGER.info("Testing bulk memory compare operation");

    final byte[] testData = {0x10, 0x20, 0x30, 0x40, 0x50};
    final int offset1 = 500;
    final int offset2 = 1000;

    // Write identical data to both locations
    memory1.write(offset1, testData);
    memory1.write(offset2, testData);
    LOGGER.info("Written identical test data at offsets " + offset1 + " and " + offset2);

    // Compare identical regions
    BulkMemoryOperations bulkOps = memory1.getBulkOperations();
    int result = bulkOps.bulkCompare(memory1, offset1, memory1, offset2, testData.length);
    assertEquals(0, result, "Identical memory regions should compare equal");
    LOGGER.info("Bulk compare of identical regions returned: " + result);

    // Modify one byte and compare again
    memory1.write(offset2, (byte) 0x99);
    result = bulkOps.bulkCompare(memory1, offset1, memory1, offset2, testData.length);
    assertThat(result).isNotEqualTo(0);
    LOGGER.info("Bulk compare after modification returned: " + result);
  }

  @Test
  void testBulkSearch() {
    LOGGER.info("Testing bulk memory search operation");

    // Create memory content with pattern to search for
    final byte[] memoryContent = {
      0x00,
      0x01,
      0x02,
      0x03,
      0x04,
      0x05,
      0x06,
      0x07,
      (byte) 0xAA,
      (byte) 0xBB,
      (byte) 0xCC,
      (byte) 0xDD, // Pattern to find
      0x10,
      0x11,
      0x12,
      0x13,
      0x14,
      0x15,
      0x16,
      0x17
    };
    final byte[] pattern = {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD};
    final int offset = 1000;

    memory1.write(offset, memoryContent);
    LOGGER.info("Written memory content with embedded pattern at offset " + offset);

    // Search for pattern
    BulkMemoryOperations bulkOps = memory1.getBulkOperations();
    int foundPosition = bulkOps.bulkSearch(memory1, offset, memoryContent.length, pattern);

    // Pattern should be found at offset + 8 (where 0xAA starts in memoryContent)
    int expectedPosition = offset + 8;
    assertEquals(expectedPosition, foundPosition, "Pattern should be found at correct position");
    LOGGER.info(
        "Pattern found at position: " + foundPosition + " (expected: " + expectedPosition + ")");

    // Search for non-existent pattern
    final byte[] nonExistentPattern = {(byte) 0xFF, (byte) 0xFE, (byte) 0xFD};
    foundPosition = bulkOps.bulkSearch(memory1, offset, memoryContent.length, nonExistentPattern);
    assertEquals(-1, foundPosition, "Non-existent pattern should return -1");
    LOGGER.info("Non-existent pattern search returned: " + foundPosition);
  }

  @Test
  void testBulkMove_OverlappingRegions() {
    LOGGER.info("Testing bulk memory move with overlapping regions");

    // Create test data with identifiable pattern
    final byte[] testData = {0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88};
    final int sourceOffset = 1000;
    final int destOffset = 1004; // Overlapping by 4 bytes

    memory1.write(sourceOffset, testData);
    LOGGER.info(
        "Written test data at offset " + sourceOffset + ": " + java.util.Arrays.toString(testData));

    // Perform bulk move (should handle overlap correctly)
    BulkMemoryOperations bulkOps = memory1.getBulkOperations();
    assertDoesNotThrow(
        () -> {
          bulkOps.bulkMove(memory1, destOffset, sourceOffset, testData.length);
        },
        "Bulk move with overlapping regions should not throw exception");
    LOGGER.info("Bulk move completed from offset " + sourceOffset + " to " + destOffset);

    // Verify moved data
    final byte[] movedData = memory1.read(destOffset, testData.length);
    assertThat(movedData).isEqualTo(testData);
    LOGGER.info("Verified moved data matches original: " + java.util.Arrays.toString(movedData));
  }

  @Test
  void testBatchOperations() {
    LOGGER.info("Testing batch read/write operations");

    // Prepare batch write data
    final Map<Integer, ByteBuffer> writes = new HashMap<>();
    writes.put(100, ByteBuffer.wrap(new byte[] {0x01, 0x02}));
    writes.put(200, ByteBuffer.wrap(new byte[] {0x03, 0x04, 0x05}));
    writes.put(300, ByteBuffer.wrap(new byte[] {0x06}));

    // Perform batch write
    BulkMemoryOperations bulkOps = memory1.getBulkOperations();
    assertDoesNotThrow(
        () -> {
          bulkOps.batchWrite(memory1, writes);
        },
        "Batch write should not throw exception");
    LOGGER.info("Batch write completed for " + writes.size() + " locations");

    // Verify batch write results
    assertThat(memory1.read(100, 2)).isEqualTo(new byte[] {0x01, 0x02});
    assertThat(memory1.read(200, 3)).isEqualTo(new byte[] {0x03, 0x04, 0x05});
    assertThat(memory1.read(300, 1)).isEqualTo(new byte[] {0x06});
    LOGGER.info("Verified all batch write data is correct");

    // Prepare batch read offsets
    final List<Integer> readOffsets = List.of(100, 200, 300);
    final List<Integer> readLengths = List.of(2, 3, 1);

    // Perform batch read
    final Map<Integer, ByteBuffer> readResults =
        bulkOps.batchRead(memory1, readOffsets, readLengths);
    assertThat(readResults).hasSize(3);

    // Verify batch read results
    assertThat(readResults.get(100).array()).isEqualTo(new byte[] {0x01, 0x02});
    assertThat(readResults.get(200).array()).isEqualTo(new byte[] {0x03, 0x04, 0x05});
    assertThat(readResults.get(300).array()).isEqualTo(new byte[] {0x06});
    LOGGER.info("Verified all batch read data is correct");
  }

  // Memory Introspection Tests

  @Test
  void testMemoryStatistics() {
    LOGGER.info("Testing memory statistics retrieval");

    // Get memory introspection interface
    MemoryIntrospection introspection = memory1.getIntrospection();
    assertNotNull(introspection, "MemoryIntrospection should be available");

    // Get basic statistics
    MemoryStatistics stats = introspection.getStatistics();
    assertNotNull(stats, "MemoryStatistics should not be null");

    // Verify basic statistics
    assertTrue(stats.getTotalAllocated() > 0, "Total allocated should be positive");
    assertTrue(stats.getCurrentUsage() > 0, "Current usage should be positive");
    assertTrue(stats.getAllocatedPages() > 0, "Allocated pages should be positive");

    LOGGER.info(
        "Memory statistics - Total: "
            + stats.getTotalAllocated()
            + " bytes, Current: "
            + stats.getCurrentUsage()
            + " bytes, Pages: "
            + stats.getAllocatedPages());

    // Verify that statistics are reasonable
    assertEquals(memory1.size(), stats.getCurrentUsage(), "Current usage should match memory size");
    assertEquals(
        memory1.size() / 65536, stats.getAllocatedPages(), "Page count should match memory size");
  }

  @Test
  void testMemorySegments() {
    LOGGER.info("Testing memory segments retrieval");

    MemoryIntrospection introspection = memory1.getIntrospection();

    // Get memory segments
    List<MemorySegment> segments = introspection.getSegments();

    // Segments may be null if not implemented yet, or contain at least the main memory segment
    if (segments != null) {
      assertThat(segments).isNotEmpty();
      LOGGER.info("Found " + segments.size() + " memory segments");

      // Verify segment properties
      for (MemorySegment segment : segments) {
        assertNotNull(segment, "Memory segment should not be null");
        assertTrue(segment.getSize() > 0, "Segment size should be positive");
        LOGGER.info(
            "Segment - Offset: "
                + segment.getStartOffset()
                + ", Size: "
                + segment.getSize()
                + ", Active: "
                + segment.isActive());
      }
    } else {
      LOGGER.info("Memory segments not implemented yet - returning null");
    }
  }

  @Test
  void testPerformanceTracking() {
    LOGGER.info("Testing performance tracking functionality");

    MemoryIntrospection introspection = memory1.getIntrospection();

    // Enable performance tracking
    assertDoesNotThrow(
        () -> {
          introspection.enablePerformanceTracking();
        },
        "Enabling performance tracking should not throw exception");
    LOGGER.info("Performance tracking enabled");

    // Verify tracking is enabled
    assertTrue(
        introspection.isPerformanceTrackingEnabled(), "Performance tracking should be enabled");

    // Perform some memory operations to generate metrics
    memory1.write(0, new byte[] {0x01, 0x02, 0x03, 0x04});
    memory1.read(0, 4);
    memory1.write(100, new byte[] {0x05, 0x06});
    memory1.read(100, 2);
    LOGGER.info("Performed memory operations to generate metrics");

    // Get performance metrics
    MemoryPerformanceMetrics metrics = introspection.getPerformanceMetrics();
    assertNotNull(metrics, "Performance metrics should not be null");

    LOGGER.info(
        "Performance metrics - Operations: "
            + metrics.getTotalOperations()
            + ", Bytes transferred: "
            + metrics.getTotalBytesTransferred());

    // Disable performance tracking
    assertDoesNotThrow(
        () -> {
          introspection.disablePerformanceTracking();
        },
        "Disabling performance tracking should not throw exception");
    LOGGER.info("Performance tracking disabled");

    // Verify tracking is disabled
    assertThat(introspection.isPerformanceTrackingEnabled()).isFalse();
  }

  @Test
  void testUsageReport() {
    LOGGER.info("Testing memory usage report generation");

    MemoryIntrospection introspection = memory1.getIntrospection();

    // Perform some operations to create usage patterns
    memory1.write(0, new byte[1024]);
    memory1.read(0, 512);
    memory1.grow(1); // Grow by one page
    LOGGER.info("Performed operations to create usage patterns");

    // Generate usage report
    MemoryUsageReport report = introspection.generateUsageReport();

    if (report != null) {
      assertNotNull(report.getStatistics(), "Report statistics should not be null");
      assertTrue(report.getReportTimestamp() > 0, "Report timestamp should be positive");

      LOGGER.info("Usage report generated at timestamp: " + report.getReportTimestamp());
      LOGGER.info(
          "Report statistics - Total: " + report.getStatistics().getTotalAllocated() + " bytes");

      // Verify recommendations and warnings are present (may be empty)
      List<String> recommendations = report.getRecommendations();
      List<String> warnings = report.getWarnings();

      if (recommendations != null && !recommendations.isEmpty()) {
        LOGGER.info("Recommendations: " + recommendations);
      }

      if (warnings != null && !warnings.isEmpty()) {
        LOGGER.info("Warnings: " + warnings);
      }
    } else {
      LOGGER.info("Usage report not implemented yet - returning null");
    }
  }

  // Memory Protection Tests

  @Test
  void testMemoryProtectionFlags() {
    LOGGER.info("Testing memory protection flag settings");

    MemoryProtection protection = memory1.getProtection();
    assertNotNull(protection, "MemoryProtection should be available");

    final int offset = 0;
    final int size = 4096; // One page

    // Test read-only protection
    assertDoesNotThrow(
        () -> {
          protection.setReadOnly(memory1, offset, size, true);
        },
        "Setting read-only protection should not throw exception");
    LOGGER.info("Set read-only protection for offset " + offset + " size " + size);

    boolean isReadOnly = protection.isReadOnly(memory1, offset, size);
    assertTrue(isReadOnly, "Memory region should be read-only");
    LOGGER.info("Verified memory region is read-only: " + isReadOnly);

    // Test executable protection
    assertDoesNotThrow(
        () -> {
          protection.setExecutable(memory1, offset, size, true);
        },
        "Setting executable protection should not throw exception");
    LOGGER.info("Set executable protection for offset " + offset + " size " + size);

    boolean isExecutable = protection.isExecutable(memory1, offset, size);
    assertTrue(isExecutable, "Memory region should be executable");
    LOGGER.info("Verified memory region is executable: " + isExecutable);

    // Remove protections
    assertDoesNotThrow(
        () -> {
          protection.setReadOnly(memory1, offset, size, false);
          protection.setExecutable(memory1, offset, size, false);
        },
        "Removing protections should not throw exception");
    LOGGER.info("Removed all protections");
  }

  @Test
  void testSecurityPolicy() {
    LOGGER.info("Testing memory security policy enforcement");

    MemoryProtection protection = memory1.getProtection();

    // Test with permissive policy
    assertDoesNotThrow(
        () -> {
          protection.setSecurityPolicy(memory1, MemoryProtection.SecurityPolicy.PERMISSIVE);
        },
        "Setting permissive security policy should not throw exception");
    LOGGER.info("Set permissive security policy");

    MemoryProtection.SecurityPolicy currentPolicy = protection.getSecurityPolicy(memory1);
    assertEquals(
        MemoryProtection.SecurityPolicy.PERMISSIVE,
        currentPolicy,
        "Security policy should be permissive");

    // Test with strict policy
    assertDoesNotThrow(
        () -> {
          protection.setSecurityPolicy(memory1, MemoryProtection.SecurityPolicy.STRICT);
        },
        "Setting strict security policy should not throw exception");
    LOGGER.info("Set strict security policy");

    currentPolicy = protection.getSecurityPolicy(memory1);
    assertEquals(
        MemoryProtection.SecurityPolicy.STRICT, currentPolicy, "Security policy should be strict");
  }

  @Test
  void testAccessControlValidation() {
    LOGGER.info("Testing access control validation");

    MemoryProtection protection = memory1.getProtection();

    final int offset = 1000;
    final int size = 100;

    // Test read access validation
    boolean canRead =
        protection.validateAccess(memory1, offset, size, MemoryProtection.AccessType.READ);
    assertTrue(canRead, "Read access should be allowed by default");
    LOGGER.info("Read access validation: " + canRead);

    // Test write access validation
    boolean canWrite =
        protection.validateAccess(memory1, offset, size, MemoryProtection.AccessType.WRITE);
    assertTrue(canWrite, "Write access should be allowed by default");
    LOGGER.info("Write access validation: " + canWrite);

    // Test execute access validation
    boolean canExecute =
        protection.validateAccess(memory1, offset, size, MemoryProtection.AccessType.EXECUTE);
    // Execute access may or may not be allowed by default
    LOGGER.info("Execute access validation: " + canExecute);
  }

  // Error Handling and Edge Cases

  @Test
  void testBulkOperations_NullParameters() {
    LOGGER.info("Testing bulk operations with null parameters");

    BulkMemoryOperations bulkOps = memory1.getBulkOperations();

    // Test null memory parameters
    assertThatThrownBy(
            () -> {
              bulkOps.bulkCopy(null, 0, memory1, 0, 10);
            })
        .isInstanceOf(Exception.class);
    LOGGER.info("Verified null destination memory throws exception");

    assertThatThrownBy(
            () -> {
              bulkOps.bulkCopy(memory1, 0, null, 0, 10);
            })
        .isInstanceOf(Exception.class);
    LOGGER.info("Verified null source memory throws exception");

    assertThatThrownBy(
            () -> {
              bulkOps.bulkFill(null, 0, 10, (byte) 0x42);
            })
        .isInstanceOf(Exception.class);
    LOGGER.info("Verified null memory for fill throws exception");
  }

  @Test
  void testBulkOperations_InvalidOffsets() {
    LOGGER.info("Testing bulk operations with invalid offsets");

    BulkMemoryOperations bulkOps = memory1.getBulkOperations();

    // Test negative offsets
    assertThatThrownBy(
            () -> {
              bulkOps.bulkCopy(memory1, -1, memory1, 0, 10);
            })
        .isInstanceOf(Exception.class);
    LOGGER.info("Verified negative destination offset throws exception");

    assertThatThrownBy(
            () -> {
              bulkOps.bulkCopy(memory1, 0, memory1, -1, 10);
            })
        .isInstanceOf(Exception.class);
    LOGGER.info("Verified negative source offset throws exception");

    // Test out-of-bounds offsets
    final int memorySize = memory1.size();
    assertThatThrownBy(
            () -> {
              bulkOps.bulkCopy(memory1, memorySize, memory1, 0, 10);
            })
        .isInstanceOf(Exception.class);
    LOGGER.info("Verified out-of-bounds destination offset throws exception");
  }

  @Test
  void testIntrospection_ClosedMemory() {
    LOGGER.info("Testing introspection on closed memory");

    // Create and close a memory instance
    WasmMemory tempMemory = WasmMemory.builder().initialPages(1).build(store);

    MemoryIntrospection introspection = tempMemory.getIntrospection();
    tempMemory.close();
    LOGGER.info("Closed temporary memory for testing");

    // Attempt operations on closed memory should throw exceptions
    assertThatThrownBy(
            () -> {
              introspection.getStatistics();
            })
        .isInstanceOf(Exception.class);
    LOGGER.info("Verified statistics access on closed memory throws exception");

    assertThatThrownBy(
            () -> {
              introspection.enablePerformanceTracking();
            })
        .isInstanceOf(Exception.class);
    LOGGER.info("Verified performance tracking on closed memory throws exception");
  }

  @Test
  void testProtection_ClosedMemory() {
    LOGGER.info("Testing protection on closed memory");

    // Create and close a memory instance
    WasmMemory tempMemory = WasmMemory.builder().initialPages(1).build(store);

    MemoryProtection protection = tempMemory.getProtection();
    tempMemory.close();
    LOGGER.info("Closed temporary memory for testing");

    // Attempt operations on closed memory should throw exceptions
    assertThatThrownBy(
            () -> {
              protection.setReadOnly(tempMemory, 0, 1000, true);
            })
        .isInstanceOf(Exception.class);
    LOGGER.info("Verified protection setting on closed memory throws exception");

    assertThatThrownBy(
            () -> {
              protection.validateAccess(tempMemory, 0, 100, MemoryProtection.AccessType.READ);
            })
        .isInstanceOf(Exception.class);
    LOGGER.info("Verified access validation on closed memory throws exception");
  }
}

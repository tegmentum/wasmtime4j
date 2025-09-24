package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive tests for 64-bit memory addressing functionality.
 *
 * <p>These tests verify that the wasmtime4j library correctly handles 64-bit memory operations,
 * including large memory sizes, 64-bit offsets, and proper compatibility with existing 32-bit
 * applications.
 */
class Memory64Test {

  @Nested
  @DisplayName("Memory64Type Tests")
  class Memory64TypeTests {

    @Test
    @DisplayName("Create 64-bit memory type with valid parameters")
    void testCreateMemory64Type() {
      Memory64Type memoryType = Memory64Type.create(1L, 1000L, false);

      assertNotNull(memoryType);
      assertEquals(1L, memoryType.getMinimum64());
      assertTrue(memoryType.getMaximum64().isPresent());
      assertEquals(1000L, memoryType.getMaximum64().get());
      assertTrue(memoryType.is64Bit());
      assertFalse(memoryType.isShared());
    }

    @Test
    @DisplayName("Create unlimited 64-bit memory type")
    void testCreateUnlimited64BitType() {
      Memory64Type memoryType = Memory64Type.createUnlimited(1L);

      assertNotNull(memoryType);
      assertEquals(1L, memoryType.getMinimum64());
      assertFalse(memoryType.getMaximum64().isPresent());
      assertTrue(memoryType.is64Bit());
      assertFalse(memoryType.isShared());
    }

    @Test
    @DisplayName("Create shared 64-bit memory type")
    void testCreateShared64BitType() {
      Memory64Type memoryType = Memory64Type.create(1L, 1000L, true);

      assertTrue(memoryType.isShared());
      assertTrue(memoryType.is64Bit());
    }

    @Test
    @DisplayName("Test memory size calculations")
    void testMemorySizeCalculations() {
      Memory64Type memoryType = Memory64Type.create(10L, 100L, false);

      assertEquals(10L * 65536L, memoryType.getMinimumSizeBytes());
      assertTrue(memoryType.getMaximumSizeBytes().isPresent());
      assertEquals(100L * 65536L, memoryType.getMaximumSizeBytes().get());
      assertEquals(65536, memoryType.getPageSizeBytes());
    }

    @Test
    @DisplayName("Test memory limit validations")
    void testMemoryLimitValidations() {
      Memory64Type memoryType = Memory64Type.create(10L, 100L, false);

      // Within limits
      assertTrue(memoryType.canAccommodatePages(50L));
      assertTrue(memoryType.canAccommodateSize(50L * 65536L));

      // Below minimum
      assertFalse(memoryType.canAccommodatePages(5L));
      assertFalse(memoryType.canAccommodateSize(5L * 65536L));

      // Above maximum
      assertFalse(memoryType.canAccommodatePages(200L));
      assertFalse(memoryType.canAccommodateSize(200L * 65536L));

      // Non-page-aligned size
      assertFalse(memoryType.canAccommodateSize(65535L)); // One byte less than a page
    }

    @Test
    @DisplayName("Test unlimited memory type validations")
    void testUnlimitedMemoryValidations() {
      Memory64Type memoryType = Memory64Type.createUnlimited(10L);

      // Within minimum
      assertTrue(memoryType.canAccommodatePages(50L));
      assertTrue(memoryType.canAccommodatePages(1_000_000L)); // Very large

      // Below minimum
      assertFalse(memoryType.canAccommodatePages(5L));
    }
  }

  @Nested
  @DisplayName("MemoryAddressingMode Tests")
  class MemoryAddressingModeTests {

    @Test
    @DisplayName("Test Memory32 characteristics")
    void testMemory32Characteristics() {
      MemoryAddressingMode mode = MemoryAddressingMode.MEMORY32;

      assertEquals("32-bit", mode.getDisplayName());
      assertEquals(4_294_967_296L, mode.getMaxMemorySize());
      assertEquals(65536L, mode.getMaxPageCount());
      assertEquals(Integer.class, mode.getAddressType());
      assertEquals(Integer.class, mode.getPageCountType());
      assertFalse(mode.is64Bit());
    }

    @Test
    @DisplayName("Test Memory64 characteristics")
    void testMemory64Characteristics() {
      MemoryAddressingMode mode = MemoryAddressingMode.MEMORY64;

      assertEquals("64-bit", mode.getDisplayName());
      assertEquals(Long.MAX_VALUE, mode.getMaxMemorySize());
      assertEquals(Long.MAX_VALUE / 65536L, mode.getMaxPageCount());
      assertEquals(Long.class, mode.getAddressType());
      assertEquals(Long.class, mode.getPageCountType());
      assertTrue(mode.is64Bit());
    }

    @Test
    @DisplayName("Test page to byte conversions")
    void testPageByteConversions() {
      MemoryAddressingMode mode32 = MemoryAddressingMode.MEMORY32;
      MemoryAddressingMode mode64 = MemoryAddressingMode.MEMORY64;

      // Test valid conversions
      assertEquals(65536L, mode32.pagesToBytes(1L));
      assertEquals(65536L * 10, mode32.pagesToBytes(10L));
      assertEquals(65536L, mode64.pagesToBytes(1L));
      assertEquals(65536L * 1000, mode64.pagesToBytes(1000L));

      // Test reverse conversions
      assertEquals(1L, mode32.bytesToPages(65536L));
      assertEquals(10L, mode32.bytesToPages(65536L * 10));
      assertEquals(1L, mode64.bytesToPages(65536L));
      assertEquals(1000L, mode64.bytesToPages(65536L * 1000));
    }

    @Test
    @DisplayName("Test conversion with invalid inputs")
    void testInvalidConversions() {
      MemoryAddressingMode mode32 = MemoryAddressingMode.MEMORY32;

      // Non-page-aligned byte size
      assertThrows(
          IllegalArgumentException.class,
          () -> mode32.bytesToPages(65535L)); // One byte less than a page

      // Exceeding 32-bit limits
      assertThrows(
          IllegalArgumentException.class,
          () -> mode32.pagesToBytes(100_000L)); // Exceeds 32-bit limit
    }

    @Test
    @DisplayName("Test optimal mode selection")
    void testOptimalModeSelection() {
      // Small memory - should prefer 32-bit
      assertEquals(
          MemoryAddressingMode.MEMORY32, MemoryAddressingMode.getOptimalMode(1000L, 10000L));

      // Large memory - requires 64-bit
      assertEquals(
          MemoryAddressingMode.MEMORY64, MemoryAddressingMode.getOptimalMode(100_000L, null));

      // At 32-bit limit
      assertEquals(
          MemoryAddressingMode.MEMORY32, MemoryAddressingMode.getOptimalMode(1000L, 65536L));

      // Just over 32-bit limit
      assertEquals(
          MemoryAddressingMode.MEMORY64, MemoryAddressingMode.getOptimalMode(1000L, 65537L));
    }

    @Test
    @DisplayName("Test optimal mode selection for byte sizes")
    void testOptimalModeSelectionForBytes() {
      // Small memory - should prefer 32-bit
      assertEquals(
          MemoryAddressingMode.MEMORY32,
          MemoryAddressingMode.getOptimalModeForSize(65536L * 1000, 65536L * 10000));

      // Large memory - requires 64-bit
      assertEquals(
          MemoryAddressingMode.MEMORY64,
          MemoryAddressingMode.getOptimalModeForSize(65536L * 100_000, null));

      // At 32-bit limit (4GB)
      assertEquals(
          MemoryAddressingMode.MEMORY32,
          MemoryAddressingMode.getOptimalModeForSize(65536L, 4_294_967_296L));

      // Just over 32-bit limit
      assertEquals(
          MemoryAddressingMode.MEMORY64,
          MemoryAddressingMode.getOptimalModeForSize(65536L, 4_294_967_296L + 65536L));
    }

    @Test
    @DisplayName("Test memory support validation")
    void testMemorySupportValidation() {
      MemoryAddressingMode mode32 = MemoryAddressingMode.MEMORY32;
      MemoryAddressingMode mode64 = MemoryAddressingMode.MEMORY64;

      // 32-bit mode limits
      assertTrue(mode32.supportsMemorySize(4_294_967_295L));
      assertFalse(mode32.supportsMemorySize(4_294_967_297L));
      assertTrue(mode32.supportsPageCount(65535L));
      assertFalse(mode32.supportsPageCount(65537L));

      // 64-bit mode should support larger sizes
      assertTrue(mode64.supportsMemorySize(4_294_967_297L));
      assertTrue(mode64.supportsPageCount(100_000L));

      // Negative values should be rejected
      assertFalse(mode32.supportsMemorySize(-1L));
      assertFalse(mode32.supportsPageCount(-1L));
      assertFalse(mode64.supportsMemorySize(-1L));
      assertFalse(mode64.supportsPageCount(-1L));
    }
  }

  @Nested
  @DisplayName("Memory64Config Tests")
  class Memory64ConfigTests {

    @Test
    @DisplayName("Create default 32-bit memory configuration")
    void testDefault32BitConfig() {
      Memory64Config config = Memory64Config.createDefault32Bit(10L);

      assertEquals(10L, config.getMinimumPages());
      assertTrue(config.getMaximumPages().isPresent());
      assertEquals(65536L, config.getMaximumPages().get()); // 4GB limit
      assertEquals(MemoryAddressingMode.MEMORY32, config.getAddressingMode());
      assertFalse(config.isShared());
      assertFalse(config.isAutoGrowthAllowed());
      assertFalse(config.is64BitAddressing());
    }

    @Test
    @DisplayName("Create default 64-bit memory configuration")
    void testDefault64BitConfig() {
      Memory64Config config = Memory64Config.createDefault64Bit(10L);

      assertEquals(10L, config.getMinimumPages());
      assertFalse(config.getMaximumPages().isPresent()); // Unlimited by default
      assertEquals(MemoryAddressingMode.MEMORY64, config.getAddressingMode());
      assertFalse(config.isShared());
      assertFalse(config.isAutoGrowthAllowed());
      assertTrue(config.is64BitAddressing());
    }

    @Test
    @DisplayName("Create unlimited 64-bit memory configuration")
    void testUnlimited64BitConfig() {
      Memory64Config config = Memory64Config.createUnlimited64Bit(10L);

      assertEquals(10L, config.getMinimumPages());
      assertFalse(config.getMaximumPages().isPresent());
      assertEquals(MemoryAddressingMode.MEMORY64, config.getAddressingMode());
      assertFalse(config.isShared());
      assertTrue(config.isAutoGrowthAllowed());
      assertTrue(config.is64BitAddressing());
    }

    @Test
    @DisplayName("Create custom memory configuration with builder")
    void testCustomConfigWithBuilder() {
      Memory64Config config =
          Memory64Config.builder(5L)
              .maximumPages(1000L)
              .shared()
              .addressing64Bit()
              .autoGrowth(true, 2.0)
              .growthLimit(500L)
              .debugName("test-memory")
              .build();

      assertEquals(5L, config.getMinimumPages());
      assertTrue(config.getMaximumPages().isPresent());
      assertEquals(1000L, config.getMaximumPages().get());
      assertTrue(config.isShared());
      assertEquals(MemoryAddressingMode.MEMORY64, config.getAddressingMode());
      assertTrue(config.isAutoGrowthAllowed());
      assertEquals(2.0, config.getGrowthFactor(), 0.001);
      assertEquals(500L, config.getGrowthLimitPages());
      assertTrue(config.getDebugName().isPresent());
      assertEquals("test-memory", config.getDebugName().get());
    }

    @Test
    @DisplayName("Test memory size calculations")
    void testMemorySizeCalculations() {
      Memory64Config config = Memory64Config.builder(10L).maximumPages(100L).build();

      assertEquals(10L * 65536L, config.getMinimumSizeBytes());
      assertTrue(config.getMaximumSizeBytes().isPresent());
      assertEquals(100L * 65536L, config.getMaximumSizeBytes().get());
    }

    @Test
    @DisplayName("Test memory limit validations")
    void testMemoryLimitValidations() {
      Memory64Config config = Memory64Config.builder(10L).maximumPages(100L).build();

      // Within limits
      assertTrue(config.isWithinLimits(50L));
      assertTrue(config.isWithinSizeLimits(50L * 65536L));

      // Below minimum
      assertFalse(config.isWithinLimits(5L));
      assertFalse(config.isWithinSizeLimits(5L * 65536L));

      // Above maximum
      assertFalse(config.isWithinLimits(200L));
      assertFalse(config.isWithinSizeLimits(200L * 65536L));

      // Non-page-aligned size
      assertFalse(config.isWithinSizeLimits(65535L)); // One byte less than a page
    }

    @Test
    @DisplayName("Test growth size calculations")
    void testGrowthSizeCalculations() {
      Memory64Config config =
          Memory64Config.builder(10L)
              .maximumPages(1000L)
              .autoGrowth(true, 2.0)
              .growthLimit(500L)
              .build();

      // Test growth from 100 pages
      var growthResult = config.calculateGrowthSize(100L);
      assertTrue(growthResult.isPresent());
      assertEquals(200L, growthResult.get()); // 100 * 2.0

      // Test growth that hits growth limit
      growthResult = config.calculateGrowthSize(300L);
      assertTrue(growthResult.isPresent());
      assertEquals(500L, growthResult.get()); // Limited by growth limit

      // Test growth that hits maximum limit
      growthResult = config.calculateGrowthSize(600L);
      assertTrue(growthResult.isPresent());
      assertEquals(1000L, growthResult.get()); // Limited by maximum pages

      // Test when already at maximum
      growthResult = config.calculateGrowthSize(1000L);
      assertFalse(growthResult.isPresent()); // Cannot grow further
    }

    @Test
    @DisplayName("Test growth when disabled")
    void testGrowthWhenDisabled() {
      Memory64Config config =
          Memory64Config.builder(10L)
              .maximumPages(1000L)
              .autoGrowth(false, 1.5) // Growth disabled
              .build();

      var growthResult = config.calculateGrowthSize(100L);
      assertFalse(growthResult.isPresent()); // No growth allowed
    }

    @Test
    @DisplayName("Test configuration validation")
    void testConfigurationValidation() {
      // Negative minimum pages
      assertThrows(IllegalArgumentException.class, () -> Memory64Config.builder(-1L));

      // Maximum less than minimum
      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Config.builder(100L).maximumPages(50L).build());

      // Invalid growth factor
      assertThrows(
          IllegalArgumentException.class,
          () ->
              Memory64Config.builder(10L)
                  .autoGrowth(true, 1.0) // Must be > 1.0
                  .build());

      // Growth limit less than minimum
      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Config.builder(100L).growthLimit(50L).build());
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.1, 1.5, 2.0, 3.0})
    @DisplayName("Test various growth factors")
    void testVariousGrowthFactors(double growthFactor) {
      Memory64Config config = Memory64Config.builder(10L).autoGrowth(true, growthFactor).build();

      assertEquals(growthFactor, config.getGrowthFactor(), 0.001);

      // Test actual growth calculation
      var growthResult = config.calculateGrowthSize(100L);
      assertTrue(growthResult.isPresent());
      assertEquals((long) Math.ceil(100 * growthFactor), growthResult.get());
    }

    @Test
    @DisplayName("Test configuration equality and hashcode")
    void testConfigurationEquality() {
      Memory64Config config1 =
          Memory64Config.builder(10L)
              .maximumPages(100L)
              .shared()
              .addressing64Bit()
              .debugName("test")
              .build();

      Memory64Config config2 =
          Memory64Config.builder(10L)
              .maximumPages(100L)
              .shared()
              .addressing64Bit()
              .debugName("test")
              .build();

      Memory64Config config3 =
          Memory64Config.builder(20L) // Different minimum
              .maximumPages(100L)
              .shared()
              .addressing64Bit()
              .debugName("test")
              .build();

      assertEquals(config1, config2);
      assertEquals(config1.hashCode(), config2.hashCode());
      assertNotEquals(config1, config3);
      assertNotEquals(config1.hashCode(), config3.hashCode());
    }

    @Test
    @DisplayName("Test configuration toString")
    void testConfigurationToString() {
      Memory64Config config =
          Memory64Config.builder(10L)
              .maximumPages(100L)
              .addressing64Bit()
              .debugName("test-memory")
              .build();

      String str = config.toString();
      assertNotNull(str);
      assertTrue(str.contains("min=10"));
      assertTrue(str.contains("max=100"));
      assertTrue(str.contains("64-bit"));
      assertTrue(str.contains("test-memory"));
    }
  }

  @Nested
  @DisplayName("Memory Interface Default Methods Tests")
  class MemoryInterfaceDefaultMethodTests {

    /** Mock implementation of WasmMemory for testing default 64-bit methods. */
    private static class MockMemory implements WasmMemory {
      private int size32 = 10;
      private int maxSize32 = 100;
      private boolean supports64Bit = false;

      @Override
      public int getSize() {
        return size32;
      }

      @Override
      public int grow(int pages) {
        int oldSize = size32;
        size32 += pages;
        return oldSize;
      }

      @Override
      public int getMaxSize() {
        return maxSize32;
      }

      @Override
      public java.nio.ByteBuffer getBuffer() {
        return null;
      }

      @Override
      public byte readByte(int offset) {
        return 0;
      }

      @Override
      public void writeByte(int offset, byte value) {}

      @Override
      public void readBytes(int offset, byte[] dest, int destOffset, int length) {}

      @Override
      public void writeBytes(int offset, byte[] src, int srcOffset, int length) {}

      @Override
      public void copy(int destOffset, int srcOffset, int length) {}

      @Override
      public void fill(int offset, byte value, int length) {}

      @Override
      public void init(int destOffset, int dataSegmentIndex, int srcOffset, int length) {}

      @Override
      public void dropDataSegment(int dataSegmentIndex) {}

      @Override
      public boolean isShared() {
        return false;
      }

      @Override
      public int atomicCompareAndSwapInt(int offset, int expected, int newValue) {
        return 0;
      }

      @Override
      public long atomicCompareAndSwapLong(int offset, long expected, long newValue) {
        return 0;
      }

      @Override
      public int atomicLoadInt(int offset) {
        return 0;
      }

      @Override
      public long atomicLoadLong(int offset) {
        return 0;
      }

      @Override
      public void atomicStoreInt(int offset, int value) {}

      @Override
      public void atomicStoreLong(int offset, long value) {}

      @Override
      public int atomicAddInt(int offset, int value) {
        return 0;
      }

      @Override
      public long atomicAddLong(int offset, long value) {
        return 0;
      }

      @Override
      public int atomicAndInt(int offset, int value) {
        return 0;
      }

      @Override
      public int atomicOrInt(int offset, int value) {
        return 0;
      }

      @Override
      public int atomicXorInt(int offset, int value) {
        return 0;
      }

      @Override
      public void atomicFence() {}

      @Override
      public int atomicNotify(int offset, int count) {
        return 0;
      }

      @Override
      public int atomicWait32(int offset, int expected, long timeoutNanos) {
        return 0;
      }

      @Override
      public int atomicWait64(int offset, long expected, long timeoutNanos) {
        return 0;
      }

      @Override
      public boolean supports64BitAddressing() {
        return supports64Bit;
      }

      public void setSupports64Bit(boolean supports) {
        this.supports64Bit = supports;
      }
    }

    private MockMemory mockMemory;

    @BeforeEach
    void setUp() {
      mockMemory = new MockMemory();
    }

    @Test
    @DisplayName("Test default 64-bit size methods")
    void testDefault64BitSizeMethods() {
      assertEquals(10L, mockMemory.getSize64());
      assertEquals(100L, mockMemory.getMaxSize64());
      assertEquals(10L * 65536L, mockMemory.getSizeInBytes64());
      assertEquals(100L * 65536L, mockMemory.getMaxSizeInBytes64());
    }

    @Test
    @DisplayName("Test default 64-bit grow method")
    void testDefault64BitGrowMethod() {
      // Test grow within 32-bit range
      long previousSize = mockMemory.grow64(5L);
      assertEquals(10L, previousSize); // Original size

      // Test grow exceeding 32-bit range
      previousSize = mockMemory.grow64(Integer.MAX_VALUE + 1L);
      assertEquals(-1L, previousSize); // Should fail for default implementation
    }

    @Test
    @DisplayName("Test default 64-bit read/write methods")
    void testDefault64BitReadWriteMethods() {
      // Test within 32-bit range
      mockMemory.writeByte64(100L, (byte) 42);
      // Should not throw exception for mock implementation

      // Test exceeding 32-bit range
      assertThrows(
          IndexOutOfBoundsException.class, () -> mockMemory.readByte64(Integer.MAX_VALUE + 1L));

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> mockMemory.writeByte64(Integer.MAX_VALUE + 1L, (byte) 42));
    }

    @Test
    @DisplayName("Test default 64-bit bulk operations")
    void testDefault64BitBulkOperations() {
      byte[] buffer = new byte[100];

      // Test within 32-bit range
      mockMemory.readBytes64(100L, buffer, 0, 50);
      mockMemory.writeBytes64(100L, buffer, 0, 50);
      mockMemory.copy64(100L, 200L, 50L);
      mockMemory.fill64(100L, (byte) 0, 50L);
      mockMemory.init64(100L, 0, 200L, 50L);
      // Should not throw exceptions for mock implementation

      // Test exceeding 32-bit range
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> mockMemory.copy64(Integer.MAX_VALUE + 1L, 200L, 50L));

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> mockMemory.fill64(Integer.MAX_VALUE + 1L, (byte) 0, 50L));
    }

    @Test
    @DisplayName("Test 64-bit addressing support detection")
    void test64BitAddressingSupportDetection() {
      // Default should be false
      assertFalse(mockMemory.supports64BitAddressing());

      // Test setting support
      mockMemory.setSupports64Bit(true);
      assertTrue(mockMemory.supports64BitAddressing());
    }

    @Test
    @DisplayName("Test compatibility with 32-bit operations")
    void testCompatibilityWith32BitOperations() {
      // Ensure 64-bit methods work with small values that fit in 32-bit
      assertEquals(mockMemory.getSize(), (int) mockMemory.getSize64());
      assertEquals(mockMemory.getMaxSize(), (int) mockMemory.getMaxSize64());

      // Test grow compatibility
      int grow32Result = mockMemory.grow(1);
      long grow64Result = mockMemory.grow64(1L);
      // Both should return the previous size (though values may differ due to state change)
      assertTrue(grow32Result >= 0 || grow32Result == -1);
      assertTrue(grow64Result >= 0 || grow64Result == -1);
    }

    @Test
    @DisplayName("Test edge cases for large values")
    void testEdgeCasesForLargeValues() {
      // Test maximum long values
      assertThrows(IndexOutOfBoundsException.class, () -> mockMemory.readByte64(Long.MAX_VALUE));

      // Test negative values
      assertThrows(IndexOutOfBoundsException.class, () -> mockMemory.readByte64(-1L));

      // Test zero offset (should work)
      mockMemory.writeByte64(0L, (byte) 42);
      // Should not throw exception
    }
  }
}

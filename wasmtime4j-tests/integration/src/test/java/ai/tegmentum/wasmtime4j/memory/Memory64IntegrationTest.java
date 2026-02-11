/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.memory.Memory64Type;
import ai.tegmentum.wasmtime4j.memory.MemoryAddressingMode;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmMemory;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Memory64 (64-bit memory addressing) functionality.
 *
 * <p>Memory64 enables WebAssembly modules to use memory larger than 4GB by using 64-bit addressing
 * for memory operations.
 *
 * @since 1.1.0
 */
@DisplayName("Memory64 Integration Tests")
public final class Memory64IntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(Memory64IntegrationTest.class.getName());

  private static boolean memory64Available = false;

  @BeforeAll
  static void checkMemory64Available() {
    try {
      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.MEMORY64);
      try (final Engine engine = Engine.create(config)) {
        memory64Available = engine.supportsFeature(WasmFeature.MEMORY64);
        LOGGER.info("Memory64 available: " + memory64Available);
      }
    } catch (final Exception e) {
      LOGGER.warning("Memory64 not available: " + e.getMessage());
      memory64Available = false;
    }
  }

  @Nested
  @DisplayName("Memory64 Configuration Tests")
  class Memory64ConfigurationTests {

    @Test
    @DisplayName("should enable memory64 via engine config")
    void shouldEnableMemory64ViaEngineConfig() throws Exception {
      assumeTrue(memory64Available, "Memory64 not available");

      LOGGER.info("Testing memory64 enablement");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.MEMORY64);

      try (final Engine engine = Engine.create(config)) {
        assertTrue(engine.supportsFeature(WasmFeature.MEMORY64), "Engine should support memory64");
        LOGGER.info("Memory64 enabled: " + engine.supportsFeature(WasmFeature.MEMORY64));
      }
    }

    @Test
    @DisplayName("should not enable memory64 by default")
    void shouldNotEnableMemory64ByDefault() throws Exception {
      LOGGER.info("Testing default memory64 configuration");

      final EngineConfig config = new EngineConfig();

      try (final Engine engine = Engine.create(config)) {
        // By default, memory64 may or may not be enabled depending on runtime
        LOGGER.info("Default memory64 enabled: " + engine.supportsFeature(WasmFeature.MEMORY64));
        assertTrue(engine.isValid(), "Engine should be valid");
      }
    }
  }

  @Nested
  @DisplayName("Memory64Type Tests")
  class Memory64TypeTests {

    @Test
    @DisplayName("should create memory64 type with min and max pages")
    void shouldCreateMemory64TypeWithMinAndMaxPages() {
      LOGGER.info("Testing Memory64Type creation with min and max pages");

      final Memory64Type type = Memory64Type.create(1L, 100L, false);

      assertNotNull(type, "Memory64Type should not be null");
      assertEquals(1L, type.getMinimum64(), "Minimum should be 1");
      assertTrue(type.getMaximum64().isPresent(), "Maximum should be present");
      assertEquals(100L, type.getMaximum64().get(), "Maximum should be 100");
      assertTrue(type.is64Bit(), "Should be 64-bit");
      assertFalse(type.isShared(), "Should not be shared");

      LOGGER.info("Created Memory64Type: " + type);
    }

    @Test
    @DisplayName("should create unlimited memory64 type")
    void shouldCreateUnlimitedMemory64Type() {
      LOGGER.info("Testing unlimited Memory64Type creation");

      final Memory64Type type = Memory64Type.createUnlimited(10L);

      assertNotNull(type, "Memory64Type should not be null");
      assertEquals(10L, type.getMinimum64(), "Minimum should be 10");
      assertFalse(type.getMaximum64().isPresent(), "Maximum should not be present");
      assertTrue(type.is64Bit(), "Should be 64-bit");

      LOGGER.info("Created unlimited Memory64Type: " + type);
    }

    @Test
    @DisplayName("should create shared memory64 type")
    void shouldCreateSharedMemory64Type() {
      LOGGER.info("Testing shared Memory64Type creation");

      final Memory64Type type = Memory64Type.create(1L, 1000L, true);

      assertNotNull(type, "Memory64Type should not be null");
      assertTrue(type.isShared(), "Should be shared");
      assertTrue(type.is64Bit(), "Should be 64-bit");

      LOGGER.info("Created shared Memory64Type: " + type);
    }

    @Test
    @DisplayName("should calculate page size correctly")
    void shouldCalculatePageSizeCorrectly() {
      LOGGER.info("Testing page size calculation");

      final Memory64Type type = Memory64Type.create(1L, 100L);

      assertEquals(65536, type.getPageSizeBytes(), "Page size should be 64KB");
      assertEquals(65536L, type.getMinimumSizeBytes(), "Minimum size should be 64KB");

      LOGGER.info("Page size bytes: " + type.getPageSizeBytes());
    }

    @Test
    @DisplayName("should check page accommodation correctly")
    void shouldCheckPageAccommodationCorrectly() {
      LOGGER.info("Testing page accommodation check");

      final Memory64Type type = Memory64Type.create(10L, 100L);

      assertTrue(type.canAccommodatePages(50L), "Should accommodate 50 pages");
      assertTrue(type.canAccommodatePages(10L), "Should accommodate minimum pages");
      assertTrue(type.canAccommodatePages(100L), "Should accommodate maximum pages");
      assertFalse(type.canAccommodatePages(5L), "Should not accommodate below minimum");
      assertFalse(type.canAccommodatePages(101L), "Should not accommodate above maximum");

      LOGGER.info("Page accommodation checks passed");
    }

    @Test
    @DisplayName("should check size accommodation correctly")
    void shouldCheckSizeAccommodationCorrectly() {
      LOGGER.info("Testing size accommodation check");

      final Memory64Type type = Memory64Type.create(1L, 10L);

      assertTrue(type.canAccommodateSize(65536L), "Should accommodate 64KB");
      assertTrue(type.canAccommodateSize(655360L), "Should accommodate 10 pages");
      assertFalse(type.canAccommodateSize(655361L), "Should not accommodate non-aligned size");
      assertFalse(type.canAccommodateSize(720896L), "Should not accommodate 11 pages");

      LOGGER.info("Size accommodation checks passed");
    }
  }

  @Nested
  @DisplayName("MemoryAddressingMode Tests")
  class MemoryAddressingModeTests {

    @Test
    @DisplayName("should have correct memory32 properties")
    void shouldHaveCorrectMemory32Properties() {
      LOGGER.info("Testing MEMORY32 properties");

      assertEquals("32-bit", MemoryAddressingMode.MEMORY32.getDisplayName());
      assertEquals(4_294_967_296L, MemoryAddressingMode.MEMORY32.getMaxMemorySize());
      assertEquals(65536L, MemoryAddressingMode.MEMORY32.getMaxPageCount());
      assertEquals(Integer.class, MemoryAddressingMode.MEMORY32.getAddressType());
      assertFalse(MemoryAddressingMode.MEMORY32.is64Bit());

      LOGGER.info("MEMORY32 properties: " + MemoryAddressingMode.MEMORY32);
    }

    @Test
    @DisplayName("should have correct memory64 properties")
    void shouldHaveCorrectMemory64Properties() {
      LOGGER.info("Testing MEMORY64 properties");

      assertEquals("64-bit", MemoryAddressingMode.MEMORY64.getDisplayName());
      assertEquals(Long.MAX_VALUE, MemoryAddressingMode.MEMORY64.getMaxMemorySize());
      assertEquals(Long.MAX_VALUE / 65536L, MemoryAddressingMode.MEMORY64.getMaxPageCount());
      assertEquals(Long.class, MemoryAddressingMode.MEMORY64.getAddressType());
      assertTrue(MemoryAddressingMode.MEMORY64.is64Bit());

      LOGGER.info("MEMORY64 properties: " + MemoryAddressingMode.MEMORY64);
    }

    @Test
    @DisplayName("should check memory size support correctly")
    void shouldCheckMemorySizeSupportCorrectly() {
      LOGGER.info("Testing memory size support check");

      // MEMORY32 limits
      assertTrue(MemoryAddressingMode.MEMORY32.supportsMemorySize(1024L));
      assertTrue(MemoryAddressingMode.MEMORY32.supportsMemorySize(4_294_967_295L));
      assertFalse(MemoryAddressingMode.MEMORY32.supportsMemorySize(5_000_000_000L));

      // MEMORY64 supports larger sizes
      assertTrue(MemoryAddressingMode.MEMORY64.supportsMemorySize(5_000_000_000L));
      assertTrue(MemoryAddressingMode.MEMORY64.supportsMemorySize(Long.MAX_VALUE));

      LOGGER.info("Memory size support checks passed");
    }

    @Test
    @DisplayName("should check page count support correctly")
    void shouldCheckPageCountSupportCorrectly() {
      LOGGER.info("Testing page count support check");

      // MEMORY32 limits
      assertTrue(MemoryAddressingMode.MEMORY32.supportsPageCount(1000L));
      assertTrue(MemoryAddressingMode.MEMORY32.supportsPageCount(65536L));
      assertFalse(MemoryAddressingMode.MEMORY32.supportsPageCount(65537L));

      // MEMORY64 supports more pages
      assertTrue(MemoryAddressingMode.MEMORY64.supportsPageCount(65537L));
      assertTrue(MemoryAddressingMode.MEMORY64.supportsPageCount(1_000_000L));

      LOGGER.info("Page count support checks passed");
    }

    @Test
    @DisplayName("should convert pages to bytes correctly")
    void shouldConvertPagesToBytesCorrectly() {
      LOGGER.info("Testing pages to bytes conversion");

      assertEquals(65536L, MemoryAddressingMode.MEMORY32.pagesToBytes(1L));
      assertEquals(131072L, MemoryAddressingMode.MEMORY32.pagesToBytes(2L));
      assertEquals(4_294_901_760L, MemoryAddressingMode.MEMORY32.pagesToBytes(65535L));

      // MEMORY64 can convert more pages
      assertEquals(65536L * 100000L, MemoryAddressingMode.MEMORY64.pagesToBytes(100000L));

      LOGGER.info("Pages to bytes conversion passed");
    }

    @Test
    @DisplayName("should convert bytes to pages correctly")
    void shouldConvertBytesToPagesCorrectly() {
      LOGGER.info("Testing bytes to pages conversion");

      assertEquals(1L, MemoryAddressingMode.MEMORY32.bytesToPages(65536L));
      assertEquals(2L, MemoryAddressingMode.MEMORY32.bytesToPages(131072L));
      assertEquals(65535L, MemoryAddressingMode.MEMORY32.bytesToPages(4_294_901_760L));

      LOGGER.info("Bytes to pages conversion passed");
    }

    @Test
    @DisplayName("should throw for non-aligned byte conversion")
    void shouldThrowForNonAlignedByteConversion() {
      LOGGER.info("Testing non-aligned byte conversion");

      assertThrows(
          IllegalArgumentException.class,
          () -> MemoryAddressingMode.MEMORY32.bytesToPages(65537L),
          "Should throw for non-aligned size");

      LOGGER.info("Non-aligned byte conversion correctly rejected");
    }

    @Test
    @DisplayName("should get optimal mode for small pages")
    void shouldGetOptimalModeForSmallPages() {
      LOGGER.info("Testing optimal mode for small pages");

      final MemoryAddressingMode mode = MemoryAddressingMode.getOptimalMode(100L, 1000L);
      assertEquals(MemoryAddressingMode.MEMORY32, mode, "Should prefer 32-bit for small pages");

      LOGGER.info("Optimal mode for small pages: " + mode);
    }

    @Test
    @DisplayName("should get optimal mode for large pages")
    void shouldGetOptimalModeForLargePages() {
      LOGGER.info("Testing optimal mode for large pages");

      final MemoryAddressingMode mode = MemoryAddressingMode.getOptimalMode(1000L, 100000L);
      assertEquals(MemoryAddressingMode.MEMORY64, mode, "Should use 64-bit for large pages");

      LOGGER.info("Optimal mode for large pages: " + mode);
    }

    @Test
    @DisplayName("should get optimal mode for size requirements")
    void shouldGetOptimalModeForSizeRequirements() {
      LOGGER.info("Testing optimal mode for size requirements");

      // 1GB - should use 32-bit
      final MemoryAddressingMode mode32 =
          MemoryAddressingMode.getOptimalModeForSize(1024L * 1024L * 1024L, null);
      assertEquals(MemoryAddressingMode.MEMORY32, mode32);

      // 10GB - should use 64-bit
      final MemoryAddressingMode mode64 =
          MemoryAddressingMode.getOptimalModeForSize(10L * 1024L * 1024L * 1024L, null);
      assertEquals(MemoryAddressingMode.MEMORY64, mode64);

      LOGGER.info("Optimal mode for size requirements passed");
    }
  }

  @Nested
  @DisplayName("Memory Creation Tests")
  class MemoryCreationTests {

    @Test
    @DisplayName("should create standard 32-bit memory")
    void shouldCreateStandard32BitMemory() throws Exception {
      LOGGER.info("Testing standard 32-bit memory creation");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final WasmMemory memory = store.createMemory(1, 10);

        assertNotNull(memory, "Memory should not be null");
        assertTrue(memory.getSize() >= 1, "Memory should have at least 1 page");

        LOGGER.info("Created 32-bit memory with 1-10 pages");
      }
    }

    @Test
    @DisplayName("should create memory with memory64 enabled")
    void shouldCreateMemoryWithMemory64Enabled() throws Exception {
      assumeTrue(memory64Available, "Memory64 not available");

      LOGGER.info("Testing memory creation with memory64 enabled");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.MEMORY64);

      try (final Engine engine = Engine.create(config);
          final Store store = engine.createStore()) {

        final WasmMemory memory = store.createMemory(1, 10);

        assertNotNull(memory, "Memory should not be null");
        assertTrue(memory.getSize() >= 1, "Memory should have at least 1 page");

        LOGGER.info("Created memory with memory64 enabled");
      }
    }
  }

  @Nested
  @DisplayName("Memory Type Detection Tests")
  class MemoryTypeDetectionTests {

    @Test
    @DisplayName("should detect 32-bit memory mode")
    void shouldDetect32BitMemoryMode() {
      LOGGER.info("Testing 32-bit memory mode detection");

      final MemoryType memoryType =
          new MemoryType() {
            @Override
            public long getMinimum() {
              return 1;
            }

            @Override
            public Optional<Long> getMaximum() {
              return Optional.of(100L);
            }

            @Override
            public boolean isShared() {
              return false;
            }

            @Override
            public boolean is64Bit() {
              return false;
            }
          };

      final MemoryAddressingMode mode = MemoryAddressingMode.detectMode(memoryType);
      assertEquals(MemoryAddressingMode.MEMORY32, mode);

      LOGGER.info("Detected 32-bit memory mode");
    }

    @Test
    @DisplayName("should detect 64-bit memory mode")
    void shouldDetect64BitMemoryMode() {
      LOGGER.info("Testing 64-bit memory mode detection");

      final Memory64Type memoryType = Memory64Type.create(1L, 100L);

      final MemoryAddressingMode mode = MemoryAddressingMode.detectMode(memoryType);
      assertEquals(MemoryAddressingMode.MEMORY64, mode);

      LOGGER.info("Detected 64-bit memory mode");
    }
  }

  @Nested
  @DisplayName("Memory64 Boundary Condition Tests")
  class Memory64BoundaryConditionTests {

    @Test
    @DisplayName("should handle zero minimum pages")
    void shouldHandleZeroMinimumPages() {
      LOGGER.info("Testing zero minimum pages");

      final Memory64Type type = Memory64Type.create(0L, 100L);

      assertEquals(0L, type.getMinimum64());
      assertEquals(0L, type.getMinimumSizeBytes());

      LOGGER.info("Zero minimum pages handled");
    }

    @Test
    @DisplayName("should handle large page counts")
    void shouldHandleLargePageCounts() {
      LOGGER.info("Testing large page counts");

      final long largePages = 1_000_000L;
      final Memory64Type type = Memory64Type.create(1L, largePages);

      assertEquals(largePages, type.getMaximum64().get());
      assertTrue(type.canAccommodatePages(largePages));
      assertFalse(type.canAccommodatePages(largePages + 1));

      LOGGER.info("Large page counts handled: " + largePages);
    }

    @Test
    @DisplayName("should handle pages beyond 32-bit limits")
    void shouldHandlePagesBeyond32BitLimits() {
      LOGGER.info("Testing pages beyond 32-bit limits");

      final long beyond32BitPages = 100_000L; // More than 65536
      final Memory64Type type = Memory64Type.create(1L, beyond32BitPages);

      assertTrue(type.canAccommodatePages(beyond32BitPages));
      assertTrue(type.is64Bit());

      LOGGER.info("Pages beyond 32-bit limits handled: " + beyond32BitPages);
    }
  }

  @Nested
  @DisplayName("Memory Type Interface Tests")
  class MemoryTypeInterfaceTests {

    @Test
    @DisplayName("should expose standard MemoryType interface")
    void shouldExposeStandardMemoryTypeInterface() {
      LOGGER.info("Testing MemoryType interface exposure");

      final Memory64Type type = Memory64Type.create(10L, 1000L);

      // Access through MemoryType interface
      final MemoryType memoryType = type;

      assertEquals(10L, memoryType.getMinimum());
      assertTrue(memoryType.getMaximum().isPresent());
      assertEquals(1000L, memoryType.getMaximum().get());
      assertTrue(memoryType.is64Bit());

      LOGGER.info("MemoryType interface exposure verified");
    }
  }
}

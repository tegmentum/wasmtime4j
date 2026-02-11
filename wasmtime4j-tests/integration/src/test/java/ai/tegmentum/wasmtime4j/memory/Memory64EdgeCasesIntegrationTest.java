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
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.memory.Memory64Config;
import ai.tegmentum.wasmtime4j.memory.Memory64Type;
import ai.tegmentum.wasmtime4j.memory.MemoryAddressingMode;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Edge case integration tests for Memory64 (64-bit memory addressing) functionality.
 *
 * <p>This test class focuses on boundary conditions, error handling, and stress scenarios for
 * Memory64 support, including:
 *
 * <ul>
 *   <li>Large memory allocation edge cases
 *   <li>Address space exhaustion handling
 *   <li>64-bit boundary operations
 *   <li>Memory growth at extreme boundaries
 *   <li>Mixed 32/64-bit module interactions
 *   <li>Concurrent memory operations
 * </ul>
 *
 * @since 1.1.0
 */
@DisplayName("Memory64 Edge Cases Integration Tests")
public final class Memory64EdgeCasesIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(Memory64EdgeCasesIntegrationTest.class.getName());

  /** Page size in bytes (64KB). */
  private static final int PAGE_SIZE = 65536;

  /** Maximum pages for 32-bit addressing. */
  private static final long MAX_32BIT_PAGES = 65536L;

  /** 4GB boundary in bytes. */
  private static final long FOUR_GB_BOUNDARY = 4_294_967_296L;

  private static boolean memory64Available = false;

  private Engine engine;
  private Store store;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void checkMemory64Available() {
    try {
      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.MEMORY64);
      try (final Engine testEngine = Engine.create(config)) {
        memory64Available = testEngine.supportsFeature(WasmFeature.MEMORY64);
        LOGGER.info("Memory64 available: " + memory64Available);
      }
    } catch (final Exception e) {
      LOGGER.warning("Memory64 not available: " + e.getMessage());
      memory64Available = false;
    }
  }

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up test: " + testInfo.getDisplayName());
    final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.MEMORY64);
    engine = Engine.create(config);
    resources.add(engine);
    store = engine.createStore();
    resources.add(store);
    LOGGER.info("Test setup completed with Memory64 enabled");
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Cleaning up test: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    LOGGER.info("Test cleanup completed");
  }

  @Nested
  @DisplayName("64-bit Boundary Tests")
  class BoundaryTests {

    @Test
    @DisplayName("should handle page count at exactly 32-bit maximum")
    void shouldHandlePageCountAtExactly32BitMaximum() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing page count at 32-bit maximum boundary");

      final Memory64Type type = Memory64Type.create(1L, MAX_32BIT_PAGES);

      assertEquals(MAX_32BIT_PAGES, type.getMaximum64().get(), "Should accept max 32-bit pages");
      assertTrue(type.canAccommodatePages(MAX_32BIT_PAGES), "Should accommodate max 32-bit pages");
      assertFalse(
          type.canAccommodatePages(MAX_32BIT_PAGES + 1),
          "Should not accommodate beyond max 32-bit pages");

      LOGGER.info("32-bit maximum page count boundary handled correctly");
    }

    @Test
    @DisplayName("should handle page count just above 32-bit maximum")
    void shouldHandlePageCountJustAbove32BitMaximum() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing page count just above 32-bit maximum boundary");

      final long beyond32BitPages = MAX_32BIT_PAGES + 1;
      final Memory64Type type = Memory64Type.create(1L, beyond32BitPages);

      assertTrue(type.is64Bit(), "Should be 64-bit type");
      assertEquals(beyond32BitPages, type.getMaximum64().get(), "Should accept beyond 32-bit max");
      assertTrue(
          type.canAccommodatePages(beyond32BitPages), "Should accommodate beyond 32-bit max");

      LOGGER.info("Beyond 32-bit page count handled: " + beyond32BitPages);
    }

    @Test
    @DisplayName("should correctly calculate size at 4GB boundary")
    void shouldCorrectlyCalculateSizeAt4GBBoundary() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing size calculation at 4GB boundary");

      final Memory64Type type = Memory64Type.create(MAX_32BIT_PAGES, MAX_32BIT_PAGES);

      final long sizeBytes = type.getMinimumSizeBytes();
      assertEquals(FOUR_GB_BOUNDARY, sizeBytes, "Size should equal exactly 4GB");
      assertTrue(type.canAccommodateSize(FOUR_GB_BOUNDARY), "Should accommodate 4GB");
      assertFalse(
          type.canAccommodateSize(FOUR_GB_BOUNDARY + PAGE_SIZE),
          "Should not accommodate beyond 4GB");

      LOGGER.info("4GB boundary calculation correct: " + sizeBytes + " bytes");
    }

    @Test
    @DisplayName("should handle addressing mode detection at boundary")
    void shouldHandleAddressingModeDetectionAtBoundary() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing addressing mode detection at boundaries");

      // At 32-bit limit - should use 32-bit
      MemoryAddressingMode mode = MemoryAddressingMode.getOptimalMode(1L, MAX_32BIT_PAGES);
      assertEquals(MemoryAddressingMode.MEMORY32, mode, "Should use 32-bit at limit");

      // Just above 32-bit limit - should use 64-bit
      mode = MemoryAddressingMode.getOptimalMode(1L, MAX_32BIT_PAGES + 1);
      assertEquals(MemoryAddressingMode.MEMORY64, mode, "Should use 64-bit beyond limit");

      LOGGER.info("Addressing mode detection at boundaries verified");
    }

    @ParameterizedTest
    @ValueSource(
        longs = {
          0L, // Zero pages
          1L, // Single page
          MAX_32BIT_PAGES - 1, // Just below 32-bit max
          MAX_32BIT_PAGES, // Exactly 32-bit max
          MAX_32BIT_PAGES + 1, // Just above 32-bit max
          MAX_32BIT_PAGES * 2, // Double 32-bit max
          1_000_000L // Large but not extreme
        })
    @DisplayName("should handle various page counts correctly")
    void shouldHandleVariousPageCountsCorrectly(final long pageCount) {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing page count: " + pageCount);

      if (pageCount == 0) {
        // Zero minimum is valid
        final Memory64Type type = Memory64Type.create(0L, 100L);
        assertEquals(0L, type.getMinimum64(), "Zero minimum should be accepted");
      } else {
        final Memory64Type type = Memory64Type.create(1L, pageCount);
        assertEquals(
            pageCount, type.getMaximum64().get(), "Should accept page count: " + pageCount);
        assertTrue(type.canAccommodatePages(pageCount), "Should accommodate: " + pageCount);
      }

      LOGGER.info("Page count " + pageCount + " handled correctly");
    }
  }

  @Nested
  @DisplayName("Address Space Exhaustion Tests")
  class AddressSpaceExhaustionTests {

    @Test
    @DisplayName("should handle negative page counts in Memory64Type")
    void shouldHandleNegativePageCountsInMemory64Type() {
      LOGGER.info("Testing negative page count handling in Memory64Type");

      // Memory64Type.create() is a simple factory without validation
      // Negative values are accepted but canAccommodatePages will handle bounds
      final Memory64Type type = Memory64Type.create(-1L, 100L);
      assertNotNull(type, "Should create type even with negative minimum");

      // The type exists but canAccommodatePages will handle bounds checking
      assertFalse(type.canAccommodatePages(-2L), "Should not accommodate negative pages");

      LOGGER.info("Negative page count handling verified");
    }

    @Test
    @DisplayName("should handle maximum less than minimum in Memory64Type")
    void shouldHandleMaximumLessThanMinimumInMemory64Type() {
      LOGGER.info("Testing max < min configuration handling");

      // Memory64Type.create() is a simple factory without validation
      // Invalid configurations are created but canAccommodatePages will fail logically
      final Memory64Type type = Memory64Type.create(100L, 50L);
      assertNotNull(type, "Should create type even with max < min");

      // The type exists but no valid page count can satisfy both min and max
      assertFalse(type.canAccommodatePages(75L), "No pages should be valid for invalid config");
      assertFalse(type.canAccommodatePages(100L), "Min pages exceeds max");

      LOGGER.info("Invalid max < min configuration handling verified");
    }

    @Test
    @DisplayName("should handle unlimited memory type without overflow")
    void shouldHandleUnlimitedMemoryTypeWithoutOverflow() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing unlimited memory type edge cases");

      final Memory64Type type = Memory64Type.createUnlimited(1L);

      assertFalse(type.getMaximum64().isPresent(), "Unlimited should have no maximum");
      assertTrue(type.canAccommodatePages(Long.MAX_VALUE / PAGE_SIZE - 1), "Should handle large");

      LOGGER.info("Unlimited memory type handled without overflow");
    }

    @Test
    @DisplayName("should gracefully handle extreme page counts")
    void shouldGracefullyHandleExtremePageCounts() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing extreme page count handling");

      // Very large but valid page count
      final long extremePageCount = Long.MAX_VALUE / PAGE_SIZE - 1;
      final Memory64Type type = Memory64Type.create(1L, extremePageCount);

      assertNotNull(type, "Should create type with extreme page count");
      assertEquals(extremePageCount, type.getMaximum64().get(), "Should preserve extreme count");

      LOGGER.info("Extreme page count handled: " + extremePageCount);
    }
  }

  @Nested
  @DisplayName("Memory64 Configuration Tests")
  class ConfigurationTests {

    @Test
    @DisplayName("should create memory config at 64-bit boundaries")
    void shouldCreateMemoryConfigAt64BitBoundaries() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing Memory64Config at 64-bit boundaries");

      final Memory64Config config =
          Memory64Config.builder(1L).maximumPages(MAX_32BIT_PAGES + 100).addressing64Bit().build();

      assertTrue(config.is64BitAddressing(), "Should be 64-bit addressing");
      assertEquals(
          MAX_32BIT_PAGES + 100, config.getMaximumPages().get(), "Should have correct max pages");

      LOGGER.info("64-bit boundary config created successfully");
    }

    @Test
    @DisplayName("should validate growth parameters at boundaries")
    void shouldValidateGrowthParametersAtBoundaries() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing growth parameter validation at boundaries");

      // Growth factor must be > 1.0
      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Config.builder(10L).autoGrowth(true, 0.5).build(),
          "Should reject growth factor < 1");

      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Config.builder(10L).autoGrowth(true, 1.0).build(),
          "Should reject growth factor = 1");

      // Valid growth factor just above 1.0
      final Memory64Config config = Memory64Config.builder(10L).autoGrowth(true, 1.001).build();
      assertEquals(1.001, config.getGrowthFactor(), 0.0001, "Should accept factor just above 1.0");

      LOGGER.info("Growth parameter boundary validation passed");
    }

    @Test
    @DisplayName("should handle growth calculations at large sizes")
    void shouldHandleGrowthCalculationsAtLargeSizes() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing growth calculations at large memory sizes");

      // Use a large but valid maximum that doesn't exceed 64-bit addressing limit
      final long largeMaximum = 100_000_000L; // 100 million pages (still large but valid)
      final Memory64Config config =
          Memory64Config.builder(1L)
              .maximumPages(largeMaximum)
              .autoGrowth(true, 2.0)
              .addressing64Bit()
              .build();

      // Test growth at a large but not overflowing size
      final long largeCurrentSize = 50_000_000L;
      final var growthResult = config.calculateGrowthSize(largeCurrentSize);

      assertTrue(growthResult.isPresent(), "Should calculate growth for large size");
      // Growth should be limited by maximum pages
      assertTrue(
          growthResult.get() <= config.getMaximumPages().get(), "Growth should respect maximum");

      LOGGER.info(
          "Large size growth calculation: " + largeCurrentSize + " -> " + growthResult.get());
    }
  }

  @Nested
  @DisplayName("Addressing Mode Edge Cases")
  class AddressingModeEdgeCases {

    @Test
    @DisplayName("should correctly convert pages to bytes at maximum")
    void shouldCorrectlyConvertPagesToBytesAtMaximum() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing page-to-byte conversion at maximum");

      final MemoryAddressingMode mode64 = MemoryAddressingMode.MEMORY64;

      // Large but not overflowing page count
      final long largePages = 1_000_000_000L;
      final long expectedBytes = largePages * PAGE_SIZE;
      final long actualBytes = mode64.pagesToBytes(largePages);

      assertEquals(expectedBytes, actualBytes, "Should correctly convert large page counts");

      LOGGER.info("Large page conversion: " + largePages + " pages = " + actualBytes + " bytes");
    }

    @Test
    @DisplayName("should reject non-page-aligned byte conversions")
    void shouldRejectNonPageAlignedByteConversions() {
      LOGGER.info("Testing rejection of non-page-aligned byte conversions");

      final MemoryAddressingMode mode32 = MemoryAddressingMode.MEMORY32;
      final MemoryAddressingMode mode64 = MemoryAddressingMode.MEMORY64;

      // Various non-aligned values
      assertThrows(
          IllegalArgumentException.class, () -> mode32.bytesToPages(1L), "Should reject 1 byte");
      assertThrows(
          IllegalArgumentException.class,
          () -> mode32.bytesToPages(PAGE_SIZE - 1),
          "Should reject PAGE_SIZE - 1");
      assertThrows(
          IllegalArgumentException.class,
          () -> mode64.bytesToPages(PAGE_SIZE + 1),
          "Should reject PAGE_SIZE + 1");

      // Page-aligned should work
      assertEquals(1L, mode32.bytesToPages(PAGE_SIZE), "Should accept PAGE_SIZE");
      assertEquals(2L, mode64.bytesToPages(PAGE_SIZE * 2), "Should accept 2 * PAGE_SIZE");

      LOGGER.info("Non-page-aligned byte conversion rejection verified");
    }

    @Test
    @DisplayName("should detect mode from memory type correctly")
    void shouldDetectModeFromMemoryTypeCorrectly() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing mode detection from memory types");

      // 32-bit type
      final MemoryType type32 =
          new MemoryType() {
            @Override
            public long getMinimum() {
              return 1;
            }

            @Override
            public java.util.Optional<Long> getMaximum() {
              return java.util.Optional.of(100L);
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

      assertEquals(
          MemoryAddressingMode.MEMORY32,
          MemoryAddressingMode.detectMode(type32),
          "Should detect 32-bit");

      // 64-bit type
      final Memory64Type type64 = Memory64Type.create(1L, 100L);
      assertEquals(
          MemoryAddressingMode.MEMORY64,
          MemoryAddressingMode.detectMode(type64),
          "Should detect 64-bit");

      LOGGER.info("Mode detection from memory types verified");
    }
  }

  @Nested
  @DisplayName("Concurrent Memory64 Operations")
  class ConcurrentOperationsTests {

    @Test
    @Timeout(30)
    @DisplayName("should handle concurrent Memory64Type creation")
    void shouldHandleConcurrentMemory64TypeCreation() throws InterruptedException {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing concurrent Memory64Type creation");

      final int threadCount = 20;
      final int operationsPerThread = 100;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(threadCount);
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger errorCount = new AtomicInteger(0);

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        executor.submit(
            () -> {
              try {
                startLatch.await();
                for (int i = 0; i < operationsPerThread; i++) {
                  try {
                    final long minPages = (threadId * operationsPerThread + i) % 1000 + 1;
                    final long maxPages = minPages + 1000;
                    final Memory64Type type = Memory64Type.create(minPages, maxPages);
                    if (type != null
                        && type.getMinimum64() == minPages
                        && type.getMaximum64().get() == maxPages) {
                      successCount.incrementAndGet();
                    }
                  } catch (final Exception e) {
                    errorCount.incrementAndGet();
                  }
                }
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                completionLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(
          completionLatch.await(25, TimeUnit.SECONDS),
          "All threads should complete within timeout");

      executor.shutdown();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor should terminate");

      final int expectedTotal = threadCount * operationsPerThread;
      assertEquals(expectedTotal, successCount.get(), "All operations should succeed");
      assertEquals(0, errorCount.get(), "No errors should occur");

      LOGGER.info(
          "Concurrent creation test completed: "
              + successCount.get()
              + " successes, "
              + errorCount.get()
              + " errors");
    }

    @Test
    @Timeout(30)
    @DisplayName("should handle concurrent Memory64Config building")
    void shouldHandleConcurrentMemory64ConfigBuilding() throws InterruptedException {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing concurrent Memory64Config building");

      final int threadCount = 10;
      final int operationsPerThread = 50;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(threadCount);
      final AtomicInteger successCount = new AtomicInteger(0);

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        executor.submit(
            () -> {
              try {
                startLatch.await();
                for (int i = 0; i < operationsPerThread; i++) {
                  final long minPages = 10L + threadId;
                  final Memory64Config config =
                      Memory64Config.builder(minPages)
                          .maximumPages(minPages + 1000)
                          .addressing64Bit()
                          .autoGrowth(true, 1.5)
                          .debugName("thread-" + threadId + "-op-" + i)
                          .build();
                  if (config != null && config.getMinimumPages() == minPages) {
                    successCount.incrementAndGet();
                  }
                }
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                completionLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(
          completionLatch.await(25, TimeUnit.SECONDS),
          "All threads should complete within timeout");

      executor.shutdown();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor should terminate");

      final int expectedTotal = threadCount * operationsPerThread;
      assertEquals(expectedTotal, successCount.get(), "All operations should succeed");

      LOGGER.info("Concurrent config building completed: " + successCount.get() + " successes");
    }
  }

  @Nested
  @DisplayName("Memory64 Interface Default Method Tests")
  class DefaultMethodEdgeCases {

    @Test
    @DisplayName("should handle 64-bit size methods with large values")
    void shouldHandle64BitSizeMethodsWithLargeValues() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing 64-bit size methods with large values");

      final Memory64Type type = Memory64Type.create(1_000_000L, 10_000_000L);

      final long minBytes = type.getMinimumSizeBytes();
      final long maxBytes = type.getMaximumSizeBytes().get();

      assertEquals(1_000_000L * PAGE_SIZE, minBytes, "Should calculate large minimum correctly");
      assertEquals(10_000_000L * PAGE_SIZE, maxBytes, "Should calculate large maximum correctly");

      LOGGER.info("Large value size methods verified: min=" + minBytes + ", max=" + maxBytes);
    }

    @Test
    @DisplayName("should verify unlimited type has no maximum size bytes")
    void shouldVerifyUnlimitedTypeHasNoMaximumSizeBytes() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing unlimited type maximum size bytes");

      final Memory64Type type = Memory64Type.createUnlimited(10L);

      assertFalse(
          type.getMaximumSizeBytes().isPresent(), "Unlimited type should have no max bytes");
      assertTrue(type.getMinimumSizeBytes() > 0, "Minimum bytes should be positive");

      LOGGER.info("Unlimited type size verification passed");
    }
  }

  @Nested
  @DisplayName("Mixed Mode Edge Cases")
  class MixedModeEdgeCases {

    @Test
    @DisplayName("should correctly identify optimal mode for various size combinations")
    void shouldCorrectlyIdentifyOptimalModeForVariousSizeCombinations() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing optimal mode identification for various sizes");

      // Small sizes - should prefer 32-bit
      assertEquals(
          MemoryAddressingMode.MEMORY32,
          MemoryAddressingMode.getOptimalModeForSize((long) PAGE_SIZE, (long) PAGE_SIZE * 1000),
          "Small sizes should use 32-bit");

      // At 4GB boundary - should still use 32-bit
      assertEquals(
          MemoryAddressingMode.MEMORY32,
          MemoryAddressingMode.getOptimalModeForSize((long) PAGE_SIZE, FOUR_GB_BOUNDARY),
          "At 4GB should use 32-bit");

      // Just over 4GB - should use 64-bit
      assertEquals(
          MemoryAddressingMode.MEMORY64,
          MemoryAddressingMode.getOptimalModeForSize(
              (long) PAGE_SIZE, FOUR_GB_BOUNDARY + PAGE_SIZE),
          "Over 4GB should use 64-bit");

      // Very large - should use 64-bit
      assertEquals(
          MemoryAddressingMode.MEMORY64,
          MemoryAddressingMode.getOptimalModeForSize((long) PAGE_SIZE * 100_000, null),
          "Very large should use 64-bit");

      LOGGER.info("Optimal mode identification verified for all size combinations");
    }

    @Test
    @DisplayName("should handle transition from 32-bit to 64-bit addressing")
    void shouldHandleTransitionFrom32BitTo64BitAddressing() {
      assumeTrue(memory64Available, "Memory64 not available");
      LOGGER.info("Testing 32-bit to 64-bit addressing transition");

      // Create 32-bit config
      final Memory64Config config32 = Memory64Config.createDefault32Bit(10L);
      assertFalse(config32.is64BitAddressing(), "Should be 32-bit");

      // Create 64-bit config with same minimum
      final Memory64Config config64 = Memory64Config.createDefault64Bit(10L);
      assertTrue(config64.is64BitAddressing(), "Should be 64-bit");

      // Both should have same minimum
      assertEquals(config32.getMinimumPages(), config64.getMinimumPages(), "Minimums should match");

      // 32-bit should have max, 64-bit unlimited
      assertTrue(config32.getMaximumPages().isPresent(), "32-bit should have max");
      assertFalse(config64.getMaximumPages().isPresent(), "64-bit default should be unlimited");

      LOGGER.info("32-bit to 64-bit transition handling verified");
    }
  }

  @Nested
  @DisplayName("Error Handling Edge Cases")
  class ErrorHandlingEdgeCases {

    @Test
    @DisplayName("should handle null debug name gracefully")
    void shouldHandleNullDebugNameGracefully() {
      LOGGER.info("Testing null debug name handling");

      final Memory64Config config = Memory64Config.builder(10L).debugName(null).build();

      assertFalse(config.getDebugName().isPresent(), "Null debug name should result in empty");

      LOGGER.info("Null debug name handled gracefully");
    }

    @Test
    @DisplayName("should validate growth limit against minimum pages")
    void shouldValidateGrowthLimitAgainstMinimumPages() {
      LOGGER.info("Testing growth limit validation against minimum");

      // Growth limit less than minimum should fail
      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Config.builder(100L).growthLimit(50L).build(),
          "Should reject growth limit < minimum");

      // Growth limit equal to minimum should work
      final Memory64Config config = Memory64Config.builder(100L).growthLimit(100L).build();
      assertEquals(100L, config.getGrowthLimitPages(), "Should accept growth limit = minimum");

      LOGGER.info("Growth limit validation passed");
    }

    @Test
    @DisplayName("should handle zero growth result when at maximum")
    void shouldHandleZeroGrowthResultWhenAtMaximum() {
      LOGGER.info("Testing growth result when already at maximum");

      final Memory64Config config =
          Memory64Config.builder(100L).maximumPages(100L).autoGrowth(true, 2.0).build();

      final var growthResult = config.calculateGrowthSize(100L);
      assertFalse(growthResult.isPresent(), "Should return empty when at maximum");

      LOGGER.info("At-maximum growth handling verified");
    }
  }
}

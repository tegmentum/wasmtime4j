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

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.memory.MemoryAddressingMode;

import ai.tegmentum.wasmtime4j.memory.Memory64Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Memory64Config} class.
 *
 * <p>Memory64Config provides comprehensive configuration for 64-bit WebAssembly memory instances
 * including page limits, addressing mode, shared memory, growth policies, and debug names.
 */
@DisplayName("Memory64Config Tests")
class Memory64ConfigTest {

  @Nested
  @DisplayName("Builder Default Values Tests")
  class BuilderDefaultValuesTests {

    @Test
    @DisplayName("should build with defaults for minimumPages=0")
    void shouldBuildWithDefaultsForZeroMinPages() {
      final Memory64Config config = Memory64Config.builder(0).build();

      assertNotNull(config, "Config should not be null");
      assertEquals(0L, config.getMinimumPages(), "Default minimumPages should be 0");
      assertFalse(config.getMaximumPages().isPresent(), "Default maximumPages should be empty");
      assertFalse(config.isShared(), "Default isShared should be false");
      assertEquals(
          MemoryAddressingMode.MEMORY32, config.getAddressingMode(),
          "Default addressing mode should be MEMORY32");
      assertFalse(config.isAutoGrowthAllowed(), "Default autoGrowth should be false");
      assertEquals(1.5, config.getGrowthFactor(), 0.001, "Default growthFactor should be 1.5");
      assertFalse(config.getDebugName().isPresent(), "Default debugName should be empty");
    }
  }

  @Nested
  @DisplayName("Builder Configuration Tests")
  class BuilderConfigurationTests {

    @Test
    @DisplayName("should set maximumPages")
    void shouldSetMaximumPages() {
      final Memory64Config config = Memory64Config.builder(10)
          .maximumPages(100L)
          .build();

      assertTrue(config.getMaximumPages().isPresent(), "maximumPages should be present");
      assertEquals(100L, config.getMaximumPages().get(), "maximumPages should be 100");
    }

    @Test
    @DisplayName("should set shared flag")
    void shouldSetSharedFlag() {
      final Memory64Config config = Memory64Config.builder(0)
          .shared()
          .build();

      assertTrue(config.isShared(), "isShared should be true");
    }

    @Test
    @DisplayName("should set 64-bit addressing")
    void shouldSet64BitAddressing() {
      final Memory64Config config = Memory64Config.builder(0)
          .addressing64Bit()
          .build();

      assertEquals(
          MemoryAddressingMode.MEMORY64, config.getAddressingMode(),
          "Addressing mode should be MEMORY64");
      assertTrue(config.is64BitAddressing(), "is64BitAddressing should return true");
    }

    @Test
    @DisplayName("should set 32-bit addressing")
    void shouldSet32BitAddressing() {
      final Memory64Config config = Memory64Config.builder(0)
          .addressing32Bit()
          .build();

      assertEquals(
          MemoryAddressingMode.MEMORY32, config.getAddressingMode(),
          "Addressing mode should be MEMORY32");
      assertFalse(config.is64BitAddressing(), "is64BitAddressing should return false");
    }

    @Test
    @DisplayName("should configure auto growth with factor")
    void shouldConfigureAutoGrowth() {
      final Memory64Config config = Memory64Config.builder(10)
          .autoGrowth(true, 2.0)
          .build();

      assertTrue(config.isAutoGrowthAllowed(), "autoGrowth should be enabled");
      assertEquals(2.0, config.getGrowthFactor(), 0.001, "growthFactor should be 2.0");
    }

    @Test
    @DisplayName("should set growth limit")
    void shouldSetGrowthLimit() {
      final Memory64Config config = Memory64Config.builder(10)
          .growthLimit(500L)
          .build();

      assertEquals(500L, config.getGrowthLimitPages(), "growthLimitPages should be 500");
    }

    @Test
    @DisplayName("should set debug name")
    void shouldSetDebugName() {
      final Memory64Config config = Memory64Config.builder(0)
          .debugName("test-memory")
          .build();

      assertTrue(config.getDebugName().isPresent(), "debugName should be present");
      assertEquals("test-memory", config.getDebugName().get(), "debugName should match");
    }

    @Test
    @DisplayName("should allow unlimited growth via unlimitedGrowth()")
    void shouldAllowUnlimitedGrowth() {
      final Memory64Config config = Memory64Config.builder(10)
          .maximumPages(100L)
          .unlimitedGrowth()
          .build();

      assertFalse(
          config.getMaximumPages().isPresent(),
          "maximumPages should be empty after unlimitedGrowth()");
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("should reject negative minimumPages in builder constructor")
    void shouldRejectNegativeMinimumPages() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Config.builder(-1),
          "Negative minimumPages should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should reject maximumPages less than minimumPages")
    void shouldRejectMaxLessThanMin() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Config.builder(100).maximumPages(50L),
          "maximumPages < minimumPages should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should reject growthFactor <= 1.0 when enabled")
    void shouldRejectGrowthFactorBelowOne() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Config.builder(0).autoGrowth(true, 0.5),
          "growthFactor <= 1.0 should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should reject growthLimit less than minimumPages")
    void shouldRejectGrowthLimitBelowMinimum() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Config.builder(100).growthLimit(50L),
          "growthLimit < minimumPages should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Computed Value Tests")
  class ComputedValueTests {

    @Test
    @DisplayName("getMinimumSizeBytes should return pages times 64KB")
    void getMinimumSizeBytesShouldComputeCorrectly() {
      final Memory64Config config = Memory64Config.builder(10).build();

      assertEquals(
          10L * 65536L, config.getMinimumSizeBytes(),
          "minimumSizeBytes should be 10 * 64KB");
    }

    @Test
    @DisplayName("getMaximumSizeBytes should return pages times 64KB when set")
    void getMaximumSizeBytesShouldComputeCorrectly() {
      final Memory64Config config = Memory64Config.builder(1)
          .maximumPages(100L)
          .build();

      final Optional<Long> maxBytes = config.getMaximumSizeBytes();
      assertTrue(maxBytes.isPresent(), "maxSizeBytes should be present");
      assertEquals(100L * 65536L, maxBytes.get(), "maxSizeBytes should be 100 * 64KB");
    }

    @Test
    @DisplayName("getMaximumSizeBytes should be empty when no max set")
    void getMaximumSizeBytesShouldBeEmptyWhenNoMax() {
      final Memory64Config config = Memory64Config.builder(0).build();

      assertFalse(
          config.getMaximumSizeBytes().isPresent(),
          "maxSizeBytes should be empty when no maximum");
    }

    @Test
    @DisplayName("isWithinLimits should validate page count")
    void isWithinLimitsShouldValidatePageCount() {
      final Memory64Config config = Memory64Config.builder(5)
          .maximumPages(20L)
          .build();

      assertFalse(config.isWithinLimits(3L), "3 pages should be below minimum 5");
      assertTrue(config.isWithinLimits(5L), "5 pages should be within limits");
      assertTrue(config.isWithinLimits(15L), "15 pages should be within limits");
      assertTrue(config.isWithinLimits(20L), "20 pages should be at max limit");
      assertFalse(config.isWithinLimits(21L), "21 pages should exceed max limit");
    }

    @Test
    @DisplayName("isWithinSizeLimits should reject non-page-aligned sizes")
    void isWithinSizeLimitsShouldRejectNonAligned() {
      final Memory64Config config = Memory64Config.builder(1)
          .maximumPages(10L)
          .build();

      assertFalse(
          config.isWithinSizeLimits(100L),
          "Non-page-aligned size should not be within limits");
    }

    @Test
    @DisplayName("isWithinSizeLimits should accept page-aligned sizes within range")
    void isWithinSizeLimitsShouldAcceptAligned() {
      final Memory64Config config = Memory64Config.builder(1)
          .maximumPages(10L)
          .build();

      assertTrue(
          config.isWithinSizeLimits(5L * 65536L),
          "Page-aligned size within range should be valid");
    }
  }

  @Nested
  @DisplayName("Growth Calculation Tests")
  class GrowthCalculationTests {

    @Test
    @DisplayName("calculateGrowthSize should return empty when autoGrowth disabled")
    void shouldReturnEmptyWhenGrowthDisabled() {
      final Memory64Config config = Memory64Config.builder(10).build();

      assertFalse(
          config.calculateGrowthSize(10L).isPresent(),
          "Should return empty when autoGrowth is disabled");
    }

    @Test
    @DisplayName("calculateGrowthSize should compute growth when enabled")
    void shouldComputeGrowthWhenEnabled() {
      final Memory64Config config = Memory64Config.builder(10)
          .autoGrowth(true, 2.0)
          .maximumPages(100L)
          .growthLimit(100L)
          .build();

      final Optional<Long> result = config.calculateGrowthSize(10L);
      assertTrue(result.isPresent(), "Growth should be calculated");
      assertTrue(result.get() > 10L, "New size should be greater than current: " + result.get());
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("createDefault64Bit should create 64-bit config")
    void createDefault64BitShouldCreate64BitConfig() {
      final Memory64Config config = Memory64Config.createDefault64Bit(10);

      assertEquals(
          MemoryAddressingMode.MEMORY64, config.getAddressingMode(),
          "Should use 64-bit addressing");
      assertEquals(10L, config.getMinimumPages(), "minimumPages should be 10");
      assertTrue(config.getDebugName().isPresent(), "debugName should be set");
    }

    @Test
    @DisplayName("createDefault32Bit should create 32-bit config")
    void createDefault32BitShouldCreate32BitConfig() {
      final Memory64Config config = Memory64Config.createDefault32Bit(1);

      assertEquals(
          MemoryAddressingMode.MEMORY32, config.getAddressingMode(),
          "Should use 32-bit addressing");
      assertTrue(
          config.getMaximumPages().isPresent(),
          "32-bit config should have maximum pages set");
    }

    @Test
    @DisplayName("createUnlimited64Bit should have autoGrowth enabled")
    void createUnlimited64BitShouldEnableAutoGrowth() {
      final Memory64Config config = Memory64Config.createUnlimited64Bit(1);

      assertTrue(config.is64BitAddressing(), "Should be 64-bit");
      assertTrue(config.isAutoGrowthAllowed(), "autoGrowth should be enabled");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal configs should be equal")
    void equalConfigsShouldBeEqual() {
      final Memory64Config config1 = Memory64Config.builder(10)
          .maximumPages(100L)
          .addressing64Bit()
          .build();
      final Memory64Config config2 = Memory64Config.builder(10)
          .maximumPages(100L)
          .addressing64Bit()
          .build();

      assertEquals(config1, config2, "Identical configs should be equal");
      assertEquals(
          config1.hashCode(), config2.hashCode(),
          "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("different configs should not be equal")
    void differentConfigsShouldNotBeEqual() {
      final Memory64Config config1 = Memory64Config.builder(10).build();
      final Memory64Config config2 = Memory64Config.builder(20).build();

      assertNotEquals(config1, config2, "Different configs should not be equal");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain key information")
    void toStringShouldContainKeyInfo() {
      final Memory64Config config = Memory64Config.builder(10)
          .maximumPages(100L)
          .addressing64Bit()
          .debugName("test")
          .build();

      final String result = config.toString();
      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("10"), "toString should contain minimumPages");
      assertTrue(result.contains("100"), "toString should contain maximumPages");
      assertTrue(result.contains("test"), "toString should contain debugName");
    }
  }
}

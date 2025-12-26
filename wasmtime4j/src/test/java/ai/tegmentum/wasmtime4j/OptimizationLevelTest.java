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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link OptimizationLevel} enum.
 *
 * <p>This test class verifies the OptimizationLevel enum which controls the trade-off between
 * compilation time and runtime performance.
 */
@DisplayName("OptimizationLevel Tests")
class OptimizationLevelTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("Should have NONE value")
    void shouldHaveNoneValue() {
      assertNotNull(OptimizationLevel.valueOf("NONE"), "Should have NONE value");
    }

    @Test
    @DisplayName("Should have SPEED value")
    void shouldHaveSpeedValue() {
      assertNotNull(OptimizationLevel.valueOf("SPEED"), "Should have SPEED value");
    }

    @Test
    @DisplayName("Should have SIZE value")
    void shouldHaveSizeValue() {
      assertNotNull(OptimizationLevel.valueOf("SIZE"), "Should have SIZE value");
    }

    @Test
    @DisplayName("Should have SPEED_AND_SIZE value")
    void shouldHaveSpeedAndSizeValue() {
      assertNotNull(
          OptimizationLevel.valueOf("SPEED_AND_SIZE"), "Should have SPEED_AND_SIZE value");
    }

    @Test
    @DisplayName("Should have exactly 4 values")
    void shouldHaveExactly4Values() {
      assertEquals(
          4, OptimizationLevel.values().length, "Should have exactly 4 optimization levels");
    }

    @Test
    @DisplayName("NONE should be at ordinal 0")
    void noneShouldBeAtOrdinal0() {
      assertEquals(0, OptimizationLevel.NONE.ordinal(), "NONE should be at ordinal 0");
    }

    @Test
    @DisplayName("SPEED should be at ordinal 1")
    void speedShouldBeAtOrdinal1() {
      assertEquals(1, OptimizationLevel.SPEED.ordinal(), "SPEED should be at ordinal 1");
    }

    @Test
    @DisplayName("SIZE should be at ordinal 2")
    void sizeShouldBeAtOrdinal2() {
      assertEquals(2, OptimizationLevel.SIZE.ordinal(), "SIZE should be at ordinal 2");
    }

    @Test
    @DisplayName("SPEED_AND_SIZE should be at ordinal 3")
    void speedAndSizeShouldBeAtOrdinal3() {
      assertEquals(
          3, OptimizationLevel.SPEED_AND_SIZE.ordinal(), "SPEED_AND_SIZE should be at ordinal 3");
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return NONE for 'NONE'")
    void valueOfShouldReturnNoneForNone() {
      assertEquals(
          OptimizationLevel.NONE,
          OptimizationLevel.valueOf("NONE"),
          "valueOf('NONE') should return NONE");
    }

    @Test
    @DisplayName("valueOf should return SPEED for 'SPEED'")
    void valueOfShouldReturnSpeedForSpeed() {
      assertEquals(
          OptimizationLevel.SPEED,
          OptimizationLevel.valueOf("SPEED"),
          "valueOf('SPEED') should return SPEED");
    }

    @Test
    @DisplayName("valueOf should return SIZE for 'SIZE'")
    void valueOfShouldReturnSizeForSize() {
      assertEquals(
          OptimizationLevel.SIZE,
          OptimizationLevel.valueOf("SIZE"),
          "valueOf('SIZE') should return SIZE");
    }

    @Test
    @DisplayName("valueOf should return SPEED_AND_SIZE for 'SPEED_AND_SIZE'")
    void valueOfShouldReturnSpeedAndSizeForSpeedAndSize() {
      assertEquals(
          OptimizationLevel.SPEED_AND_SIZE,
          OptimizationLevel.valueOf("SPEED_AND_SIZE"),
          "valueOf('SPEED_AND_SIZE') should return SPEED_AND_SIZE");
    }

    @Test
    @DisplayName("valueOf should throw for invalid value")
    void valueOfShouldThrowForInvalidValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> OptimizationLevel.valueOf("INVALID"),
          "valueOf should throw for invalid value");
    }

    @Test
    @DisplayName("valueOf should throw for lowercase value")
    void valueOfShouldThrowForLowercaseValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> OptimizationLevel.valueOf("speed"),
          "valueOf should throw for lowercase value");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null")
    void valueOfShouldThrowNpeForNull() {
      assertThrows(
          NullPointerException.class,
          () -> OptimizationLevel.valueOf(null),
          "valueOf should throw NPE for null");
    }
  }

  @Nested
  @DisplayName("name Tests")
  class NameTests {

    @Test
    @DisplayName("NONE name should be 'NONE'")
    void noneNameShouldBeNone() {
      assertEquals("NONE", OptimizationLevel.NONE.name(), "NONE name should be 'NONE'");
    }

    @Test
    @DisplayName("SPEED name should be 'SPEED'")
    void speedNameShouldBeSpeed() {
      assertEquals("SPEED", OptimizationLevel.SPEED.name(), "SPEED name should be 'SPEED'");
    }

    @Test
    @DisplayName("SIZE name should be 'SIZE'")
    void sizeNameShouldBeSize() {
      assertEquals("SIZE", OptimizationLevel.SIZE.name(), "SIZE name should be 'SIZE'");
    }

    @Test
    @DisplayName("SPEED_AND_SIZE name should be 'SPEED_AND_SIZE'")
    void speedAndSizeNameShouldBeSpeedAndSize() {
      assertEquals(
          "SPEED_AND_SIZE",
          OptimizationLevel.SPEED_AND_SIZE.name(),
          "SPEED_AND_SIZE name should be 'SPEED_AND_SIZE'");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("NONE toString should return 'NONE'")
    void noneToStringShouldReturnNone() {
      assertEquals("NONE", OptimizationLevel.NONE.toString(), "NONE toString should return 'NONE'");
    }

    @Test
    @DisplayName("SPEED toString should return 'SPEED'")
    void speedToStringShouldReturnSpeed() {
      assertEquals(
          "SPEED", OptimizationLevel.SPEED.toString(), "SPEED toString should return 'SPEED'");
    }

    @Test
    @DisplayName("SIZE toString should return 'SIZE'")
    void sizeToStringShouldReturnSize() {
      assertEquals("SIZE", OptimizationLevel.SIZE.toString(), "SIZE toString should return 'SIZE'");
    }

    @Test
    @DisplayName("SPEED_AND_SIZE toString should return 'SPEED_AND_SIZE'")
    void speedAndSizeToStringShouldReturnSpeedAndSize() {
      assertEquals(
          "SPEED_AND_SIZE",
          OptimizationLevel.SPEED_AND_SIZE.toString(),
          "SPEED_AND_SIZE toString should return 'SPEED_AND_SIZE'");
    }
  }

  @Nested
  @DisplayName("values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array with all levels")
    void valuesShouldReturnArrayWithAllLevels() {
      final OptimizationLevel[] values = OptimizationLevel.values();

      assertEquals(4, values.length, "Should have 4 values");
      assertEquals(OptimizationLevel.NONE, values[0], "First value should be NONE");
      assertEquals(OptimizationLevel.SPEED, values[1], "Second value should be SPEED");
      assertEquals(OptimizationLevel.SIZE, values[2], "Third value should be SIZE");
      assertEquals(
          OptimizationLevel.SPEED_AND_SIZE, values[3], "Fourth value should be SPEED_AND_SIZE");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final OptimizationLevel[] values1 = OptimizationLevel.values();
      final OptimizationLevel[] values2 = OptimizationLevel.values();

      assertTrue(values1 != values2, "values() should return new array each call");
      assertEquals(values1.length, values2.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      final OptimizationLevel level = OptimizationLevel.SPEED;
      String result;

      switch (level) {
        case NONE:
          result = "No optimization";
          break;
        case SPEED:
          result = "Speed optimized";
          break;
        case SIZE:
          result = "Size optimized";
          break;
        case SPEED_AND_SIZE:
          result = "Speed and size optimized";
          break;
        default:
          result = "Unknown";
      }

      assertEquals("Speed optimized", result, "Switch should work with SPEED");
    }

    @Test
    @DisplayName("Should be comparable using equals")
    void shouldBeComparableUsingEquals() {
      final OptimizationLevel level1 = OptimizationLevel.SIZE;
      final OptimizationLevel level2 = OptimizationLevel.SIZE;
      final OptimizationLevel level3 = OptimizationLevel.SPEED;

      assertEquals(level1, level2, "Same enum values should be equal");
      assertTrue(!level1.equals(level3), "Different enum values should not be equal");
    }

    @Test
    @DisplayName("Should be usable with compareTo")
    void shouldBeUsableWithCompareTo() {
      assertTrue(
          OptimizationLevel.NONE.compareTo(OptimizationLevel.SPEED) < 0,
          "NONE should be less than SPEED by ordinal");
      assertTrue(
          OptimizationLevel.SPEED_AND_SIZE.compareTo(OptimizationLevel.NONE) > 0,
          "SPEED_AND_SIZE should be greater than NONE by ordinal");
      assertEquals(
          0,
          OptimizationLevel.SIZE.compareTo(OptimizationLevel.SIZE),
          "Same value should compare equal");
    }

    @Test
    @DisplayName("Should work for compilation configuration scenarios")
    void shouldWorkForCompilationConfigurationScenarios() {
      // Simulating configuration based on use case
      final OptimizationLevel devLevel = OptimizationLevel.NONE;
      final OptimizationLevel prodLevel = OptimizationLevel.SPEED;
      final OptimizationLevel embeddedLevel = OptimizationLevel.SIZE;
      final OptimizationLevel balancedLevel = OptimizationLevel.SPEED_AND_SIZE;

      assertEquals(OptimizationLevel.NONE, devLevel, "Dev should use NONE for fast compile");
      assertEquals(OptimizationLevel.SPEED, prodLevel, "Prod should use SPEED");
      assertEquals(OptimizationLevel.SIZE, embeddedLevel, "Embedded should use SIZE");
      assertEquals(
          OptimizationLevel.SPEED_AND_SIZE, balancedLevel, "Balanced should use SPEED_AND_SIZE");
    }
  }
}

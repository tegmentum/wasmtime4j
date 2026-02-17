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

import ai.tegmentum.wasmtime4j.config.OptimizationLevel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link OptimizationLevel} enum.
 *
 * <p>Verifies enum structure, constants, ordinals, valueOf, and switch exhaustiveness.
 */
@DisplayName("OptimizationLevel Tests")
class OptimizationLevelTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(OptimizationLevel.class.isEnum(), "OptimizationLevel should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 4 values")
    void shouldHaveExactValueCount() {
      assertEquals(
          4, OptimizationLevel.values().length, "OptimizationLevel should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain NONE")
    void shouldContainNone() {
      assertNotNull(OptimizationLevel.NONE, "NONE constant should exist");
      assertEquals("NONE", OptimizationLevel.NONE.name(), "NONE name should match");
    }

    @Test
    @DisplayName("should contain SPEED")
    void shouldContainSpeed() {
      assertNotNull(OptimizationLevel.SPEED, "SPEED constant should exist");
      assertEquals("SPEED", OptimizationLevel.SPEED.name(), "SPEED name should match");
    }

    @Test
    @DisplayName("should contain SIZE")
    void shouldContainSize() {
      assertNotNull(OptimizationLevel.SIZE, "SIZE constant should exist");
      assertEquals("SIZE", OptimizationLevel.SIZE.name(), "SIZE name should match");
    }

    @Test
    @DisplayName("should contain SPEED_AND_SIZE")
    void shouldContainSpeedAndSize() {
      assertNotNull(OptimizationLevel.SPEED_AND_SIZE, "SPEED_AND_SIZE constant should exist");
      assertEquals(
          "SPEED_AND_SIZE",
          OptimizationLevel.SPEED_AND_SIZE.name(),
          "SPEED_AND_SIZE name should match");
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final OptimizationLevel value : OptimizationLevel.values()) {
        assertEquals(
            value,
            OptimizationLevel.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> OptimizationLevel.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("values() Tests")
  class ValuesTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllEnumConstants() {
      final OptimizationLevel[] values = OptimizationLevel.values();
      final Set<OptimizationLevel> valueSet = new HashSet<>(Arrays.asList(values));

      assertTrue(valueSet.contains(OptimizationLevel.NONE), "Should contain NONE");
      assertTrue(valueSet.contains(OptimizationLevel.SPEED), "Should contain SPEED");
      assertTrue(valueSet.contains(OptimizationLevel.SIZE), "Should contain SIZE");
      assertTrue(
          valueSet.contains(OptimizationLevel.SPEED_AND_SIZE), "Should contain SPEED_AND_SIZE");
    }

    @Test
    @DisplayName("values() should return new array each time")
    void valuesShouldReturnNewArrayEachTime() {
      final OptimizationLevel[] first = OptimizationLevel.values();
      final OptimizationLevel[] second = OptimizationLevel.values();

      assertTrue(first != second, "Should return new array each time");
      assertEquals(first.length, second.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return enum name for each constant")
    void toStringShouldReturnEnumName() {
      for (final OptimizationLevel level : OptimizationLevel.values()) {
        assertEquals(
            level.name(), level.toString(), "toString should return name for " + level.name());
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("all enum values should be usable in switch statement")
    void allEnumValuesShouldBeUsableInSwitchStatement() {
      for (final OptimizationLevel level : OptimizationLevel.values()) {
        final String result;
        switch (level) {
          case NONE:
            result = "none";
            break;
          case SPEED:
            result = "speed";
            break;
          case SIZE:
            result = "size";
            break;
          case SPEED_AND_SIZE:
            result = "speed_and_size";
            break;
          default:
            result = "unknown";
            break;
        }
        assertTrue(!result.equals("unknown"), "Should match a case: " + level);
      }
    }

    @Test
    @DisplayName("enum values should be comparable")
    void enumValuesShouldBeComparable() {
      assertTrue(
          OptimizationLevel.NONE.compareTo(OptimizationLevel.SPEED) != 0,
          "Different enums should be different");
      assertEquals(
          0, OptimizationLevel.NONE.compareTo(OptimizationLevel.NONE), "Same enum should be equal");
    }
  }
}

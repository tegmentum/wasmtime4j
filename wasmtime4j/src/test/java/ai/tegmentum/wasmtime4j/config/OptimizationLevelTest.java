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

package ai.tegmentum.wasmtime4j.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the OptimizationLevel enum.
 *
 * <p>This test class verifies the enum structure and values for OptimizationLevel using
 * reflection-based testing.
 */
@DisplayName("OptimizationLevel Tests")
class OptimizationLevelTest {

  // ========================================================================
  // Enum Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("OptimizationLevel should be an enum")
    void shouldBeAnEnum() {
      assertTrue(OptimizationLevel.class.isEnum(), "OptimizationLevel should be an enum");
    }

    @Test
    @DisplayName("OptimizationLevel should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(OptimizationLevel.class.getModifiers()),
          "OptimizationLevel should be public");
    }
  }

  // ========================================================================
  // Enum Values Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("OptimizationLevel should have 4 values")
    void shouldHaveFourValues() {
      OptimizationLevel[] values = OptimizationLevel.values();
      assertEquals(4, values.length, "OptimizationLevel should have 4 values");
    }

    @Test
    @DisplayName("OptimizationLevel should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("NONE", "SPEED", "SIZE", "SPEED_AND_SIZE");
      Set<String> actualNames = new HashSet<>();
      for (OptimizationLevel level : OptimizationLevel.values()) {
        actualNames.add(level.name());
      }
      assertEquals(expectedNames, actualNames, "OptimizationLevel should have expected values");
    }

    @Test
    @DisplayName("NONE value should exist")
    void shouldHaveNoneValue() {
      OptimizationLevel none = OptimizationLevel.valueOf("NONE");
      assertNotNull(none, "NONE value should exist");
      assertEquals("NONE", none.name(), "Name should be NONE");
    }

    @Test
    @DisplayName("SPEED value should exist")
    void shouldHaveSpeedValue() {
      OptimizationLevel speed = OptimizationLevel.valueOf("SPEED");
      assertNotNull(speed, "SPEED value should exist");
      assertEquals("SPEED", speed.name(), "Name should be SPEED");
    }

    @Test
    @DisplayName("SIZE value should exist")
    void shouldHaveSizeValue() {
      OptimizationLevel size = OptimizationLevel.valueOf("SIZE");
      assertNotNull(size, "SIZE value should exist");
      assertEquals("SIZE", size.name(), "Name should be SIZE");
    }

    @Test
    @DisplayName("SPEED_AND_SIZE value should exist")
    void shouldHaveSpeedAndSizeValue() {
      OptimizationLevel speedAndSize = OptimizationLevel.valueOf("SPEED_AND_SIZE");
      assertNotNull(speedAndSize, "SPEED_AND_SIZE value should exist");
      assertEquals("SPEED_AND_SIZE", speedAndSize.name(), "Name should be SPEED_AND_SIZE");
    }
  }

  // ========================================================================
  // Ordinal Tests
  // ========================================================================

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("NONE should have ordinal 0")
    void noneShouldHaveOrdinalZero() {
      assertEquals(0, OptimizationLevel.NONE.ordinal(), "NONE should have ordinal 0");
    }

    @Test
    @DisplayName("SPEED should have ordinal 1")
    void speedShouldHaveOrdinalOne() {
      assertEquals(1, OptimizationLevel.SPEED.ordinal(), "SPEED should have ordinal 1");
    }

    @Test
    @DisplayName("SIZE should have ordinal 2")
    void sizeShouldHaveOrdinalTwo() {
      assertEquals(2, OptimizationLevel.SIZE.ordinal(), "SIZE should have ordinal 2");
    }

    @Test
    @DisplayName("SPEED_AND_SIZE should have ordinal 3")
    void speedAndSizeShouldHaveOrdinalThree() {
      assertEquals(
          3, OptimizationLevel.SPEED_AND_SIZE.ordinal(), "SPEED_AND_SIZE should have ordinal 3");
    }
  }
}

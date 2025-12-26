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
    @DisplayName("OptimizationLevel should have 5 values")
    void shouldHaveFiveValues() {
      OptimizationLevel[] values = OptimizationLevel.values();
      assertEquals(5, values.length, "OptimizationLevel should have 5 values");
    }

    @Test
    @DisplayName("OptimizationLevel should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("NONE", "BASIC", "STANDARD", "AGGRESSIVE", "MAXIMUM");
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
    @DisplayName("BASIC value should exist")
    void shouldHaveBasicValue() {
      OptimizationLevel basic = OptimizationLevel.valueOf("BASIC");
      assertNotNull(basic, "BASIC value should exist");
      assertEquals("BASIC", basic.name(), "Name should be BASIC");
    }

    @Test
    @DisplayName("STANDARD value should exist")
    void shouldHaveStandardValue() {
      OptimizationLevel standard = OptimizationLevel.valueOf("STANDARD");
      assertNotNull(standard, "STANDARD value should exist");
      assertEquals("STANDARD", standard.name(), "Name should be STANDARD");
    }

    @Test
    @DisplayName("AGGRESSIVE value should exist")
    void shouldHaveAggressiveValue() {
      OptimizationLevel aggressive = OptimizationLevel.valueOf("AGGRESSIVE");
      assertNotNull(aggressive, "AGGRESSIVE value should exist");
      assertEquals("AGGRESSIVE", aggressive.name(), "Name should be AGGRESSIVE");
    }

    @Test
    @DisplayName("MAXIMUM value should exist")
    void shouldHaveMaximumValue() {
      OptimizationLevel maximum = OptimizationLevel.valueOf("MAXIMUM");
      assertNotNull(maximum, "MAXIMUM value should exist");
      assertEquals("MAXIMUM", maximum.name(), "Name should be MAXIMUM");
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
    @DisplayName("BASIC should have ordinal 1")
    void basicShouldHaveOrdinalOne() {
      assertEquals(1, OptimizationLevel.BASIC.ordinal(), "BASIC should have ordinal 1");
    }

    @Test
    @DisplayName("STANDARD should have ordinal 2")
    void standardShouldHaveOrdinalTwo() {
      assertEquals(2, OptimizationLevel.STANDARD.ordinal(), "STANDARD should have ordinal 2");
    }

    @Test
    @DisplayName("AGGRESSIVE should have ordinal 3")
    void aggressiveShouldHaveOrdinalThree() {
      assertEquals(3, OptimizationLevel.AGGRESSIVE.ordinal(), "AGGRESSIVE should have ordinal 3");
    }

    @Test
    @DisplayName("MAXIMUM should have ordinal 4")
    void maximumShouldHaveOrdinalFour() {
      assertEquals(4, OptimizationLevel.MAXIMUM.ordinal(), "MAXIMUM should have ordinal 4");
    }
  }
}

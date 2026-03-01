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
}

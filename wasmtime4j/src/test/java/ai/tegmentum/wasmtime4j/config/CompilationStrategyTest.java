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
 * Tests for the CompilationStrategy enum.
 *
 * <p>This test class verifies the enum structure and values for CompilationStrategy using
 * reflection-based testing.
 */
@DisplayName("CompilationStrategy Tests")
class CompilationStrategyTest {

  // ========================================================================
  // Enum Values Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("CompilationStrategy should have 7 values")
    void shouldHaveSevenValues() {
      CompilationStrategy[] values = CompilationStrategy.values();
      assertEquals(7, values.length, "CompilationStrategy should have 7 values");
    }

    @Test
    @DisplayName("CompilationStrategy should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of("AUTO", "CRANELIFT", "WINCH", "SPEED", "PERFORMANCE", "SIZE", "DEFAULT");
      Set<String> actualNames = new HashSet<>();
      for (CompilationStrategy strategy : CompilationStrategy.values()) {
        actualNames.add(strategy.name());
      }
      assertEquals(expectedNames, actualNames, "CompilationStrategy should have expected values");
    }

    @Test
    @DisplayName("AUTO value should exist")
    void shouldHaveAutoValue() {
      CompilationStrategy auto = CompilationStrategy.valueOf("AUTO");
      assertNotNull(auto, "AUTO value should exist");
      assertEquals("AUTO", auto.name(), "Name should be AUTO");
    }

    @Test
    @DisplayName("CRANELIFT value should exist")
    void shouldHaveCraneliftValue() {
      CompilationStrategy cranelift = CompilationStrategy.valueOf("CRANELIFT");
      assertNotNull(cranelift, "CRANELIFT value should exist");
      assertEquals("CRANELIFT", cranelift.name(), "Name should be CRANELIFT");
    }

    @Test
    @DisplayName("WINCH value should exist")
    void shouldHaveWinchValue() {
      CompilationStrategy winch = CompilationStrategy.valueOf("WINCH");
      assertNotNull(winch, "WINCH value should exist");
      assertEquals("WINCH", winch.name(), "Name should be WINCH");
    }

    @Test
    @DisplayName("SPEED value should exist (deprecated)")
    void shouldHaveSpeedValue() {
      CompilationStrategy speed = CompilationStrategy.valueOf("SPEED");
      assertNotNull(speed, "SPEED value should exist");
      assertEquals("SPEED", speed.name(), "Name should be SPEED");
    }

    @Test
    @DisplayName("PERFORMANCE value should exist (deprecated)")
    void shouldHavePerformanceValue() {
      CompilationStrategy performance = CompilationStrategy.valueOf("PERFORMANCE");
      assertNotNull(performance, "PERFORMANCE value should exist");
      assertEquals("PERFORMANCE", performance.name(), "Name should be PERFORMANCE");
    }

    @Test
    @DisplayName("SIZE value should exist (deprecated)")
    void shouldHaveSizeValue() {
      CompilationStrategy size = CompilationStrategy.valueOf("SIZE");
      assertNotNull(size, "SIZE value should exist");
      assertEquals("SIZE", size.name(), "Name should be SIZE");
    }

    @Test
    @DisplayName("DEFAULT value should exist (deprecated)")
    void shouldHaveDefaultValue() {
      CompilationStrategy defaultValue = CompilationStrategy.valueOf("DEFAULT");
      assertNotNull(defaultValue, "DEFAULT value should exist");
      assertEquals("DEFAULT", defaultValue.name(), "Name should be DEFAULT");
    }
  }
}

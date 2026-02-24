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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.OptimizationLevel;
import ai.tegmentum.wasmtime4j.execution.ProfilingStrategy;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for core enums in the wasmtime4j package.
 *
 * <p>This test class verifies the enum structures, values, and functionality for
 * OptimizationLevel and ProfilingStrategy.
 */
@DisplayName("Core Enums Tests")
class CoreEnumsTest {

  // ========================================================================
  // OptimizationLevel Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("OptimizationLevel Enum Tests")
  class OptimizationLevelTests {

    @Test
    @DisplayName("OptimizationLevel should be an enum")
    void shouldBeAnEnum() {
      assertTrue(OptimizationLevel.class.isEnum(), "OptimizationLevel should be an enum");
    }

    @Test
    @DisplayName("OptimizationLevel should be a public enum")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(OptimizationLevel.class.getModifiers()),
          "OptimizationLevel should be public");
    }

    @Test
    @DisplayName("OptimizationLevel should have exactly 4 values")
    void shouldHaveExactlyFourValues() {
      OptimizationLevel[] values = OptimizationLevel.values();
      assertEquals(4, values.length, "OptimizationLevel should have exactly 4 values");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> OptimizationLevel.valueOf("INVALID"),
          "valueOf should throw for invalid name");
    }

    @Test
    @DisplayName("Enum should be comparable")
    void enumShouldBeComparable() {
      assertTrue(
          OptimizationLevel.NONE.compareTo(OptimizationLevel.SPEED) < 0,
          "NONE should come before SPEED");
      assertTrue(
          OptimizationLevel.SPEED_AND_SIZE.compareTo(OptimizationLevel.NONE) > 0,
          "SPEED_AND_SIZE should come after NONE");
    }
  }

  // ========================================================================
  // ProfilingStrategy Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ProfilingStrategy Enum Tests")
  class ProfilingStrategyTests {

    @Test
    @DisplayName("ProfilingStrategy should be an enum")
    void shouldBeAnEnum() {
      assertTrue(ProfilingStrategy.class.isEnum(), "ProfilingStrategy should be an enum");
    }

    @Test
    @DisplayName("ProfilingStrategy should be a public enum")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ProfilingStrategy.class.getModifiers()),
          "ProfilingStrategy should be public");
    }

    @Test
    @DisplayName("ProfilingStrategy should have exactly 4 values")
    void shouldHaveExactlyFourValues() {
      ProfilingStrategy[] values = ProfilingStrategy.values();
      assertEquals(4, values.length, "ProfilingStrategy should have exactly 4 values");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ProfilingStrategy.valueOf("INVALID"),
          "valueOf should throw for invalid name");
    }

    @Test
    @DisplayName("Enum should work with Arrays.asList")
    void enumShouldWorkWithArraysList() {
      var list = Arrays.asList(ProfilingStrategy.values());
      assertEquals(4, list.size(), "List should contain 4 elements");
      assertTrue(list.contains(ProfilingStrategy.JIT_DUMP), "List should contain JIT_DUMP");
    }
  }

  // ========================================================================
  // Cross-Enum Tests
  // ========================================================================

}

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

import ai.tegmentum.wasmtime4j.type.WasmTypeKind;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ai.tegmentum.wasmtime4j.execution.ProfilingStrategy;

/**
 * Tests for core enums in the wasmtime4j package.
 *
 * <p>This test class verifies the enum structures, values, and functionality for WasmTypeKind,
 * OptimizationLevel, and ProfilingStrategy using reflection-based testing.
 */
@DisplayName("Core Enums Tests")
class CoreEnumsTest {

  // ========================================================================
  // WasmTypeKind Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmTypeKind Enum Tests")
  class WasmTypeKindTests {

    @Test
    @DisplayName("WasmTypeKind should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasmTypeKind.class.isEnum(), "WasmTypeKind should be an enum");
    }

    @Test
    @DisplayName("WasmTypeKind should be a public enum")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmTypeKind.class.getModifiers()), "WasmTypeKind should be public");
    }

    @Test
    @DisplayName("WasmTypeKind should have exactly 4 values")
    void shouldHaveExactlyFourValues() {
      WasmTypeKind[] values = WasmTypeKind.values();
      assertEquals(4, values.length, "WasmTypeKind should have exactly 4 values");
    }

    @Test
    @DisplayName("FUNCTION should exist")
    void shouldHaveFunction() {
      assertNotNull(WasmTypeKind.valueOf("FUNCTION"), "FUNCTION should exist");
    }

    @Test
    @DisplayName("GLOBAL should exist")
    void shouldHaveGlobal() {
      assertNotNull(WasmTypeKind.valueOf("GLOBAL"), "GLOBAL should exist");
    }

    @Test
    @DisplayName("MEMORY should exist")
    void shouldHaveMemory() {
      assertNotNull(WasmTypeKind.valueOf("MEMORY"), "MEMORY should exist");
    }

    @Test
    @DisplayName("TABLE should exist")
    void shouldHaveTable() {
      assertNotNull(WasmTypeKind.valueOf("TABLE"), "TABLE should exist");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmTypeKind.valueOf("INVALID"),
          "valueOf should throw for invalid name");
    }

    @Test
    @DisplayName("All WasmTypeKind values should have unique ordinals")
    void allValuesShouldHaveUniqueOrdinals() {
      WasmTypeKind[] values = WasmTypeKind.values();
      Set<Integer> ordinals = new HashSet<>();
      for (WasmTypeKind kind : values) {
        ordinals.add(kind.ordinal());
      }
      assertEquals(values.length, ordinals.size(), "All values should have unique ordinals");
    }

    @Test
    @DisplayName("FUNCTION should have ordinal 0")
    void functionShouldHaveOrdinalZero() {
      assertEquals(0, WasmTypeKind.FUNCTION.ordinal(), "FUNCTION should have ordinal 0");
    }

    @Test
    @DisplayName("TABLE should have ordinal 3")
    void tableShouldHaveOrdinalThree() {
      assertEquals(3, WasmTypeKind.TABLE.ordinal(), "TABLE should have ordinal 3");
    }

    @Test
    @DisplayName("toString should return enum name")
    void toStringShouldReturnEnumName() {
      assertEquals("FUNCTION", WasmTypeKind.FUNCTION.toString(), "toString should return name");
      assertEquals("MEMORY", WasmTypeKind.MEMORY.toString(), "toString should return name");
    }
  }

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
    @DisplayName("NONE should exist")
    void shouldHaveNone() {
      assertNotNull(OptimizationLevel.valueOf("NONE"), "NONE should exist");
    }

    @Test
    @DisplayName("SPEED should exist")
    void shouldHaveSpeed() {
      assertNotNull(OptimizationLevel.valueOf("SPEED"), "SPEED should exist");
    }

    @Test
    @DisplayName("SIZE should exist")
    void shouldHaveSize() {
      assertNotNull(OptimizationLevel.valueOf("SIZE"), "SIZE should exist");
    }

    @Test
    @DisplayName("SPEED_AND_SIZE should exist")
    void shouldHaveSpeedAndSize() {
      assertNotNull(OptimizationLevel.valueOf("SPEED_AND_SIZE"), "SPEED_AND_SIZE should exist");
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
    @DisplayName("All OptimizationLevel values should have unique ordinals")
    void allValuesShouldHaveUniqueOrdinals() {
      OptimizationLevel[] values = OptimizationLevel.values();
      Set<Integer> ordinals = new HashSet<>();
      for (OptimizationLevel level : values) {
        ordinals.add(level.ordinal());
      }
      assertEquals(values.length, ordinals.size(), "All values should have unique ordinals");
    }

    @Test
    @DisplayName("NONE should have ordinal 0")
    void noneShouldHaveOrdinalZero() {
      assertEquals(0, OptimizationLevel.NONE.ordinal(), "NONE should have ordinal 0");
    }

    @Test
    @DisplayName("SPEED_AND_SIZE should have ordinal 3")
    void speedAndSizeShouldHaveOrdinalThree() {
      assertEquals(
          3, OptimizationLevel.SPEED_AND_SIZE.ordinal(), "SPEED_AND_SIZE should have ordinal 3");
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
    @DisplayName("NONE should exist")
    void shouldHaveNone() {
      assertNotNull(ProfilingStrategy.valueOf("NONE"), "NONE should exist");
    }

    @Test
    @DisplayName("JIT_DUMP should exist")
    void shouldHaveJitDump() {
      assertNotNull(ProfilingStrategy.valueOf("JIT_DUMP"), "JIT_DUMP should exist");
    }

    @Test
    @DisplayName("PERF_MAP should exist")
    void shouldHavePerfMap() {
      assertNotNull(ProfilingStrategy.valueOf("PERF_MAP"), "PERF_MAP should exist");
    }

    @Test
    @DisplayName("VTUNE should exist")
    void shouldHaveVtune() {
      assertNotNull(ProfilingStrategy.valueOf("VTUNE"), "VTUNE should exist");
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
    @DisplayName("All ProfilingStrategy values should have unique ordinals")
    void allValuesShouldHaveUniqueOrdinals() {
      ProfilingStrategy[] values = ProfilingStrategy.values();
      Set<Integer> ordinals = new HashSet<>();
      for (ProfilingStrategy strategy : values) {
        ordinals.add(strategy.ordinal());
      }
      assertEquals(values.length, ordinals.size(), "All values should have unique ordinals");
    }

    @Test
    @DisplayName("NONE should have ordinal 0")
    void noneShouldHaveOrdinalZero() {
      assertEquals(0, ProfilingStrategy.NONE.ordinal(), "NONE should have ordinal 0");
    }

    @Test
    @DisplayName("VTUNE should have ordinal 3")
    void vtuneShouldHaveOrdinalThree() {
      assertEquals(3, ProfilingStrategy.VTUNE.ordinal(), "VTUNE should have ordinal 3");
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

  @Nested
  @DisplayName("Cross-Enum Tests")
  class CrossEnumTests {

    @Test
    @DisplayName("All core enums should be in the same package")
    void allEnumsShouldBeInSamePackage() {
      assertEquals(
          WasmTypeKind.class.getPackage(),
          OptimizationLevel.class.getPackage(),
          "WasmTypeKind and OptimizationLevel should be in same package");
      assertEquals(
          OptimizationLevel.class.getPackage(),
          ProfilingStrategy.class.getPackage(),
          "OptimizationLevel and ProfilingStrategy should be in same package");
    }

    @Test
    @DisplayName("All core enums should be final (implicit)")
    void allEnumsShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasmTypeKind.class.getModifiers()), "WasmTypeKind should be final");
      assertTrue(
          Modifier.isFinal(OptimizationLevel.class.getModifiers()),
          "OptimizationLevel should be final");
      assertTrue(
          Modifier.isFinal(ProfilingStrategy.class.getModifiers()),
          "ProfilingStrategy should be final");
    }

    @Test
    @DisplayName("All core enums should extend Enum")
    void allEnumsShouldExtendEnum() {
      assertTrue(
          Enum.class.isAssignableFrom(WasmTypeKind.class), "WasmTypeKind should extend Enum");
      assertTrue(
          Enum.class.isAssignableFrom(OptimizationLevel.class),
          "OptimizationLevel should extend Enum");
      assertTrue(
          Enum.class.isAssignableFrom(ProfilingStrategy.class),
          "ProfilingStrategy should extend Enum");
    }

    @Test
    @DisplayName("All core enums should implement Comparable")
    void allEnumsShouldImplementComparable() {
      assertTrue(
          Comparable.class.isAssignableFrom(WasmTypeKind.class),
          "WasmTypeKind should implement Comparable");
      assertTrue(
          Comparable.class.isAssignableFrom(OptimizationLevel.class),
          "OptimizationLevel should implement Comparable");
      assertTrue(
          Comparable.class.isAssignableFrom(ProfilingStrategy.class),
          "ProfilingStrategy should implement Comparable");
    }
  }
}

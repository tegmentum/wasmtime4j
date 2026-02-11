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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ai.tegmentum.wasmtime4j.execution.ProfilingStrategy;

/**
 * Tests for the {@link ProfilingStrategy} enum.
 *
 * <p>Verifies enum structure, constants, ordinals, valueOf, and switch exhaustiveness.
 */
@DisplayName("ProfilingStrategy Tests")
class ProfilingStrategyTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(ProfilingStrategy.class.isEnum(), "ProfilingStrategy should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 4 values")
    void shouldHaveExactValueCount() {
      assertEquals(
          4, ProfilingStrategy.values().length, "ProfilingStrategy should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain NONE")
    void shouldContainNone() {
      assertNotNull(ProfilingStrategy.NONE, "NONE constant should exist");
      assertEquals("NONE", ProfilingStrategy.NONE.name(), "NONE name should match");
    }

    @Test
    @DisplayName("should contain JIT_DUMP")
    void shouldContainJitDump() {
      assertNotNull(ProfilingStrategy.JIT_DUMP, "JIT_DUMP constant should exist");
      assertEquals("JIT_DUMP", ProfilingStrategy.JIT_DUMP.name(), "JIT_DUMP name should match");
    }

    @Test
    @DisplayName("should contain PERF_MAP")
    void shouldContainPerfMap() {
      assertNotNull(ProfilingStrategy.PERF_MAP, "PERF_MAP constant should exist");
      assertEquals("PERF_MAP", ProfilingStrategy.PERF_MAP.name(), "PERF_MAP name should match");
    }

    @Test
    @DisplayName("should contain VTUNE")
    void shouldContainVtune() {
      assertNotNull(ProfilingStrategy.VTUNE, "VTUNE constant should exist");
      assertEquals("VTUNE", ProfilingStrategy.VTUNE.name(), "VTUNE name should match");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final ProfilingStrategy[] values = ProfilingStrategy.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(
            i, values[i].ordinal(), "Ordinal of " + values[i].name() + " should be " + i);
      }
    }

    @Test
    @DisplayName("ordinals should be unique")
    void ordinalsShouldBeUnique() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final ProfilingStrategy strategy : ProfilingStrategy.values()) {
        assertTrue(
            ordinals.add(strategy.ordinal()), "Ordinal should be unique: " + strategy.ordinal());
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final ProfilingStrategy value : ProfilingStrategy.values()) {
        assertEquals(
            value,
            ProfilingStrategy.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ProfilingStrategy.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("values() Tests")
  class ValuesTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllEnumConstants() {
      final ProfilingStrategy[] values = ProfilingStrategy.values();
      final Set<ProfilingStrategy> valueSet = new HashSet<>(Arrays.asList(values));

      assertTrue(valueSet.contains(ProfilingStrategy.NONE), "Should contain NONE");
      assertTrue(valueSet.contains(ProfilingStrategy.JIT_DUMP), "Should contain JIT_DUMP");
      assertTrue(valueSet.contains(ProfilingStrategy.PERF_MAP), "Should contain PERF_MAP");
      assertTrue(valueSet.contains(ProfilingStrategy.VTUNE), "Should contain VTUNE");
    }

    @Test
    @DisplayName("values() should return new array each time")
    void valuesShouldReturnNewArrayEachTime() {
      final ProfilingStrategy[] first = ProfilingStrategy.values();
      final ProfilingStrategy[] second = ProfilingStrategy.values();

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
      for (final ProfilingStrategy strategy : ProfilingStrategy.values()) {
        assertEquals(
            strategy.name(),
            strategy.toString(),
            "toString should return name for " + strategy.name());
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("all enum values should be usable in switch statement")
    void allEnumValuesShouldBeUsableInSwitchStatement() {
      for (final ProfilingStrategy strategy : ProfilingStrategy.values()) {
        final String result;
        switch (strategy) {
          case NONE:
            result = "none";
            break;
          case JIT_DUMP:
            result = "jitdump";
            break;
          case PERF_MAP:
            result = "perfmap";
            break;
          case VTUNE:
            result = "vtune";
            break;
          default:
            result = "unknown";
            break;
        }
        assertTrue(!result.equals("unknown"), "Should match a case: " + strategy);
      }
    }

    @Test
    @DisplayName("enum values should be comparable")
    void enumValuesShouldBeComparable() {
      assertTrue(
          ProfilingStrategy.NONE.compareTo(ProfilingStrategy.JIT_DUMP) != 0,
          "Different enums should be different");
      assertEquals(
          0,
          ProfilingStrategy.NONE.compareTo(ProfilingStrategy.NONE),
          "Same enum should be equal");
    }
  }
}

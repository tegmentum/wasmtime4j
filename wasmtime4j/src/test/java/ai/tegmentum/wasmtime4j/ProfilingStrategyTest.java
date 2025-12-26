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
 * Tests for the {@link ProfilingStrategy} enum.
 *
 * <p>This test class verifies the ProfilingStrategy enum which controls how profiling information
 * is collected during WebAssembly execution.
 */
@DisplayName("ProfilingStrategy Tests")
class ProfilingStrategyTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("Should have NONE value")
    void shouldHaveNoneValue() {
      assertNotNull(ProfilingStrategy.valueOf("NONE"), "Should have NONE value");
    }

    @Test
    @DisplayName("Should have JIT_DUMP value")
    void shouldHaveJitDumpValue() {
      assertNotNull(ProfilingStrategy.valueOf("JIT_DUMP"), "Should have JIT_DUMP value");
    }

    @Test
    @DisplayName("Should have PERF_MAP value")
    void shouldHavePerfMapValue() {
      assertNotNull(ProfilingStrategy.valueOf("PERF_MAP"), "Should have PERF_MAP value");
    }

    @Test
    @DisplayName("Should have VTUNE value")
    void shouldHaveVtuneValue() {
      assertNotNull(ProfilingStrategy.valueOf("VTUNE"), "Should have VTUNE value");
    }

    @Test
    @DisplayName("Should have exactly 4 values")
    void shouldHaveExactly4Values() {
      assertEquals(
          4, ProfilingStrategy.values().length, "Should have exactly 4 profiling strategies");
    }

    @Test
    @DisplayName("NONE should be at ordinal 0")
    void noneShouldBeAtOrdinal0() {
      assertEquals(0, ProfilingStrategy.NONE.ordinal(), "NONE should be at ordinal 0");
    }

    @Test
    @DisplayName("JIT_DUMP should be at ordinal 1")
    void jitDumpShouldBeAtOrdinal1() {
      assertEquals(1, ProfilingStrategy.JIT_DUMP.ordinal(), "JIT_DUMP should be at ordinal 1");
    }

    @Test
    @DisplayName("PERF_MAP should be at ordinal 2")
    void perfMapShouldBeAtOrdinal2() {
      assertEquals(2, ProfilingStrategy.PERF_MAP.ordinal(), "PERF_MAP should be at ordinal 2");
    }

    @Test
    @DisplayName("VTUNE should be at ordinal 3")
    void vtuneShouldBeAtOrdinal3() {
      assertEquals(3, ProfilingStrategy.VTUNE.ordinal(), "VTUNE should be at ordinal 3");
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return NONE for 'NONE'")
    void valueOfShouldReturnNoneForNone() {
      assertEquals(
          ProfilingStrategy.NONE,
          ProfilingStrategy.valueOf("NONE"),
          "valueOf('NONE') should return NONE");
    }

    @Test
    @DisplayName("valueOf should return JIT_DUMP for 'JIT_DUMP'")
    void valueOfShouldReturnJitDumpForJitDump() {
      assertEquals(
          ProfilingStrategy.JIT_DUMP,
          ProfilingStrategy.valueOf("JIT_DUMP"),
          "valueOf('JIT_DUMP') should return JIT_DUMP");
    }

    @Test
    @DisplayName("valueOf should return PERF_MAP for 'PERF_MAP'")
    void valueOfShouldReturnPerfMapForPerfMap() {
      assertEquals(
          ProfilingStrategy.PERF_MAP,
          ProfilingStrategy.valueOf("PERF_MAP"),
          "valueOf('PERF_MAP') should return PERF_MAP");
    }

    @Test
    @DisplayName("valueOf should return VTUNE for 'VTUNE'")
    void valueOfShouldReturnVtuneForVtune() {
      assertEquals(
          ProfilingStrategy.VTUNE,
          ProfilingStrategy.valueOf("VTUNE"),
          "valueOf('VTUNE') should return VTUNE");
    }

    @Test
    @DisplayName("valueOf should throw for invalid value")
    void valueOfShouldThrowForInvalidValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ProfilingStrategy.valueOf("INVALID"),
          "valueOf should throw for invalid value");
    }

    @Test
    @DisplayName("valueOf should throw for lowercase value")
    void valueOfShouldThrowForLowercaseValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ProfilingStrategy.valueOf("vtune"),
          "valueOf should throw for lowercase value");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null")
    void valueOfShouldThrowNpeForNull() {
      assertThrows(
          NullPointerException.class,
          () -> ProfilingStrategy.valueOf(null),
          "valueOf should throw NPE for null");
    }
  }

  @Nested
  @DisplayName("name Tests")
  class NameTests {

    @Test
    @DisplayName("NONE name should be 'NONE'")
    void noneNameShouldBeNone() {
      assertEquals("NONE", ProfilingStrategy.NONE.name(), "NONE name should be 'NONE'");
    }

    @Test
    @DisplayName("JIT_DUMP name should be 'JIT_DUMP'")
    void jitDumpNameShouldBeJitDump() {
      assertEquals(
          "JIT_DUMP", ProfilingStrategy.JIT_DUMP.name(), "JIT_DUMP name should be 'JIT_DUMP'");
    }

    @Test
    @DisplayName("PERF_MAP name should be 'PERF_MAP'")
    void perfMapNameShouldBePerfMap() {
      assertEquals(
          "PERF_MAP", ProfilingStrategy.PERF_MAP.name(), "PERF_MAP name should be 'PERF_MAP'");
    }

    @Test
    @DisplayName("VTUNE name should be 'VTUNE'")
    void vtuneNameShouldBeVtune() {
      assertEquals("VTUNE", ProfilingStrategy.VTUNE.name(), "VTUNE name should be 'VTUNE'");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("NONE toString should return 'NONE'")
    void noneToStringShouldReturnNone() {
      assertEquals("NONE", ProfilingStrategy.NONE.toString(), "NONE toString should return 'NONE'");
    }

    @Test
    @DisplayName("JIT_DUMP toString should return 'JIT_DUMP'")
    void jitDumpToStringShouldReturnJitDump() {
      assertEquals(
          "JIT_DUMP",
          ProfilingStrategy.JIT_DUMP.toString(),
          "JIT_DUMP toString should return 'JIT_DUMP'");
    }

    @Test
    @DisplayName("PERF_MAP toString should return 'PERF_MAP'")
    void perfMapToStringShouldReturnPerfMap() {
      assertEquals(
          "PERF_MAP",
          ProfilingStrategy.PERF_MAP.toString(),
          "PERF_MAP toString should return 'PERF_MAP'");
    }

    @Test
    @DisplayName("VTUNE toString should return 'VTUNE'")
    void vtuneToStringShouldReturnVtune() {
      assertEquals(
          "VTUNE", ProfilingStrategy.VTUNE.toString(), "VTUNE toString should return 'VTUNE'");
    }
  }

  @Nested
  @DisplayName("values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array with all strategies")
    void valuesShouldReturnArrayWithAllStrategies() {
      final ProfilingStrategy[] values = ProfilingStrategy.values();

      assertEquals(4, values.length, "Should have 4 values");
      assertEquals(ProfilingStrategy.NONE, values[0], "First value should be NONE");
      assertEquals(ProfilingStrategy.JIT_DUMP, values[1], "Second value should be JIT_DUMP");
      assertEquals(ProfilingStrategy.PERF_MAP, values[2], "Third value should be PERF_MAP");
      assertEquals(ProfilingStrategy.VTUNE, values[3], "Fourth value should be VTUNE");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final ProfilingStrategy[] values1 = ProfilingStrategy.values();
      final ProfilingStrategy[] values2 = ProfilingStrategy.values();

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
      final ProfilingStrategy strategy = ProfilingStrategy.PERF_MAP;
      String result;

      switch (strategy) {
        case NONE:
          result = "No profiling";
          break;
        case JIT_DUMP:
          result = "JIT dump enabled";
          break;
        case PERF_MAP:
          result = "Perf map enabled";
          break;
        case VTUNE:
          result = "VTune enabled";
          break;
        default:
          result = "Unknown";
      }

      assertEquals("Perf map enabled", result, "Switch should work with PERF_MAP");
    }

    @Test
    @DisplayName("Should be comparable using equals")
    void shouldBeComparableUsingEquals() {
      final ProfilingStrategy strategy1 = ProfilingStrategy.JIT_DUMP;
      final ProfilingStrategy strategy2 = ProfilingStrategy.JIT_DUMP;
      final ProfilingStrategy strategy3 = ProfilingStrategy.VTUNE;

      assertEquals(strategy1, strategy2, "Same enum values should be equal");
      assertTrue(!strategy1.equals(strategy3), "Different enum values should not be equal");
    }

    @Test
    @DisplayName("Should be usable with compareTo")
    void shouldBeUsableWithCompareTo() {
      assertTrue(
          ProfilingStrategy.NONE.compareTo(ProfilingStrategy.VTUNE) < 0,
          "NONE should be less than VTUNE by ordinal");
      assertTrue(
          ProfilingStrategy.VTUNE.compareTo(ProfilingStrategy.NONE) > 0,
          "VTUNE should be greater than NONE by ordinal");
      assertEquals(
          0,
          ProfilingStrategy.JIT_DUMP.compareTo(ProfilingStrategy.JIT_DUMP),
          "Same value should compare equal");
    }

    @Test
    @DisplayName("Should work for profiling configuration scenarios")
    void shouldWorkForProfilingConfigurationScenarios() {
      // Simulating configuration based on environment
      final ProfilingStrategy prodStrategy = ProfilingStrategy.NONE;
      final ProfilingStrategy devStrategy = ProfilingStrategy.JIT_DUMP;
      final ProfilingStrategy linuxProfiler = ProfilingStrategy.PERF_MAP;
      final ProfilingStrategy intelProfiler = ProfilingStrategy.VTUNE;

      assertEquals(
          ProfilingStrategy.NONE, prodStrategy, "Production should use NONE for minimal overhead");
      assertEquals(ProfilingStrategy.JIT_DUMP, devStrategy, "Development can use JIT_DUMP");
      assertEquals(
          ProfilingStrategy.PERF_MAP, linuxProfiler, "Linux profiling should use PERF_MAP");
      assertEquals(ProfilingStrategy.VTUNE, intelProfiler, "Intel profiling should use VTUNE");
    }
  }
}

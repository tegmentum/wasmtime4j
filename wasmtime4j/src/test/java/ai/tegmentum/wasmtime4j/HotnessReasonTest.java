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

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ai.tegmentum.wasmtime4j.execution.HotnessReason;

/**
 * Tests for {@link HotnessReason}.
 *
 * <p>Verifies enum structure, constants, valueOf/values, ordinals, and toString.
 */
@DisplayName("HotnessReason Tests")
class HotnessReasonTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(HotnessReason.class.isEnum(), "HotnessReason should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 8 values")
    void shouldHaveExactValueCount() {
      assertEquals(8, HotnessReason.values().length,
          "HotnessReason should have exactly 8 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain STATIC_ANALYSIS")
    void shouldContainStaticAnalysis() {
      assertNotNull(HotnessReason.STATIC_ANALYSIS,
          "STATIC_ANALYSIS constant should exist");
    }

    @Test
    @DisplayName("should contain RUNTIME_PROFILING")
    void shouldContainRuntimeProfiling() {
      assertNotNull(HotnessReason.RUNTIME_PROFILING,
          "RUNTIME_PROFILING constant should exist");
    }

    @Test
    @DisplayName("should contain HOT_CALLER_PROPAGATION")
    void shouldContainHotCallerPropagation() {
      assertNotNull(HotnessReason.HOT_CALLER_PROPAGATION,
          "HOT_CALLER_PROPAGATION constant should exist");
    }

    @Test
    @DisplayName("should contain CRITICAL_PATH")
    void shouldContainCriticalPath() {
      assertNotNull(HotnessReason.CRITICAL_PATH,
          "CRITICAL_PATH constant should exist");
    }

    @Test
    @DisplayName("should contain LOOP_ANALYSIS")
    void shouldContainLoopAnalysis() {
      assertNotNull(HotnessReason.LOOP_ANALYSIS,
          "LOOP_ANALYSIS constant should exist");
    }

    @Test
    @DisplayName("should contain HEURISTIC")
    void shouldContainHeuristic() {
      assertNotNull(HotnessReason.HEURISTIC, "HEURISTIC constant should exist");
    }

    @Test
    @DisplayName("should contain USER_ANNOTATION")
    void shouldContainUserAnnotation() {
      assertNotNull(HotnessReason.USER_ANNOTATION,
          "USER_ANNOTATION constant should exist");
    }

    @Test
    @DisplayName("should contain MACHINE_LEARNING")
    void shouldContainMachineLearning() {
      assertNotNull(HotnessReason.MACHINE_LEARNING,
          "MACHINE_LEARNING constant should exist");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final HotnessReason value : HotnessReason.values()) {
        ordinals.add(value.ordinal());
      }
      assertEquals(HotnessReason.values().length, ordinals.size(),
          "All ordinals should be unique");
    }

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final HotnessReason[] values = HotnessReason.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(),
            "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final HotnessReason value : HotnessReason.values()) {
        assertEquals(value, HotnessReason.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> HotnessReason.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call")
    void shouldReturnNewArrayOnEachCall() {
      final HotnessReason[] first = HotnessReason.values();
      final HotnessReason[] second = HotnessReason.values();
      assertTrue(first != second, "values() should return a new array on each call");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return enum name as toString")
    void shouldReturnEnumNameAsToString() {
      for (final HotnessReason value : HotnessReason.values()) {
        assertEquals(value.name(), value.toString(),
            "toString should return the enum name for " + value.name());
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should support switch statement over all values")
    void shouldSupportSwitchStatement() {
      for (final HotnessReason reason : HotnessReason.values()) {
        final String result;
        switch (reason) {
          case STATIC_ANALYSIS:
          case RUNTIME_PROFILING:
          case HOT_CALLER_PROPAGATION:
          case CRITICAL_PATH:
          case LOOP_ANALYSIS:
          case HEURISTIC:
          case USER_ANNOTATION:
          case MACHINE_LEARNING:
            result = reason.name();
            break;
          default:
            result = "unknown";
            break;
        }
        assertEquals(reason.name(), result, "Switch should handle " + reason.name());
      }
    }
  }
}

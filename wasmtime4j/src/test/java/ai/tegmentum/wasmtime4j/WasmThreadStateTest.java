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

import ai.tegmentum.wasmtime4j.concurrent.WasmThreadState;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmThreadState}.
 *
 * <p>Verifies enum structure, constants, valueOf/values, ordinals, and toString.
 */
@DisplayName("WasmThreadState Tests")
class WasmThreadStateTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(WasmThreadState.class.isEnum(), "WasmThreadState should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 9 values")
    void shouldHaveExactValueCount() {
      assertEquals(
          9, WasmThreadState.values().length, "WasmThreadState should have exactly 9 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain all expected constants")
    void shouldContainAllExpectedConstants() {
      assertNotNull(WasmThreadState.NEW, "NEW constant should exist");
      assertNotNull(WasmThreadState.RUNNING, "RUNNING constant should exist");
      assertNotNull(WasmThreadState.WAITING, "WAITING constant should exist");
      assertNotNull(WasmThreadState.TIMED_WAITING, "TIMED_WAITING constant should exist");
      assertNotNull(WasmThreadState.BLOCKED, "BLOCKED constant should exist");
      assertNotNull(WasmThreadState.SUSPENDED, "SUSPENDED constant should exist");
      assertNotNull(WasmThreadState.TERMINATED, "TERMINATED constant should exist");
      assertNotNull(WasmThreadState.ERROR, "ERROR constant should exist");
      assertNotNull(WasmThreadState.KILLED, "KILLED constant should exist");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final WasmThreadState value : WasmThreadState.values()) {
        ordinals.add(value.ordinal());
      }
      assertEquals(
          WasmThreadState.values().length, ordinals.size(), "All ordinals should be unique");
    }

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final WasmThreadState[] values = WasmThreadState.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final WasmThreadState value : WasmThreadState.values()) {
        assertEquals(
            value, WasmThreadState.valueOf(value.name()), "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmThreadState.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call")
    void shouldReturnNewArrayOnEachCall() {
      final WasmThreadState[] first = WasmThreadState.values();
      final WasmThreadState[] second = WasmThreadState.values();
      assertTrue(first != second, "values() should return a new array on each call");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return enum name as toString")
    void shouldReturnEnumNameAsToString() {
      for (final WasmThreadState value : WasmThreadState.values()) {
        assertEquals(
            value.name(),
            value.toString(),
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
      for (final WasmThreadState state : WasmThreadState.values()) {
        final String result;
        switch (state) {
          case NEW:
          case RUNNING:
          case WAITING:
          case TIMED_WAITING:
          case BLOCKED:
          case SUSPENDED:
          case TERMINATED:
          case ERROR:
          case KILLED:
            result = state.name();
            break;
          default:
            result = "unknown";
            break;
        }
        assertEquals(state.name(), result, "Switch should handle " + state.name());
      }
    }
  }
}

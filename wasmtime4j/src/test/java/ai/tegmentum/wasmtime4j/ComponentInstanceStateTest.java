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

/**
 * Tests for {@link ComponentInstanceState}.
 *
 * <p>Verifies enum structure, constants, valueOf/values, ordinals, and toString.
 */
@DisplayName("ComponentInstanceState Tests")
class ComponentInstanceStateTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(ComponentInstanceState.class.isEnum(),
          "ComponentInstanceState should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 6 values")
    void shouldHaveExactValueCount() {
      assertEquals(6, ComponentInstanceState.values().length,
          "ComponentInstanceState should have exactly 6 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain INITIALIZING")
    void shouldContainInitializing() {
      assertNotNull(ComponentInstanceState.INITIALIZING,
          "INITIALIZING constant should exist");
    }

    @Test
    @DisplayName("should contain ACTIVE")
    void shouldContainActive() {
      assertNotNull(ComponentInstanceState.ACTIVE, "ACTIVE constant should exist");
    }

    @Test
    @DisplayName("should contain SUSPENDED")
    void shouldContainSuspended() {
      assertNotNull(ComponentInstanceState.SUSPENDED, "SUSPENDED constant should exist");
    }

    @Test
    @DisplayName("should contain ERROR")
    void shouldContainError() {
      assertNotNull(ComponentInstanceState.ERROR, "ERROR constant should exist");
    }

    @Test
    @DisplayName("should contain TERMINATING")
    void shouldContainTerminating() {
      assertNotNull(ComponentInstanceState.TERMINATING,
          "TERMINATING constant should exist");
    }

    @Test
    @DisplayName("should contain TERMINATED")
    void shouldContainTerminated() {
      assertNotNull(ComponentInstanceState.TERMINATED,
          "TERMINATED constant should exist");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final ComponentInstanceState value : ComponentInstanceState.values()) {
        ordinals.add(value.ordinal());
      }
      assertEquals(ComponentInstanceState.values().length, ordinals.size(),
          "All ordinals should be unique");
    }

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final ComponentInstanceState[] values = ComponentInstanceState.values();
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
      for (final ComponentInstanceState value : ComponentInstanceState.values()) {
        assertEquals(value, ComponentInstanceState.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> ComponentInstanceState.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call")
    void shouldReturnNewArrayOnEachCall() {
      final ComponentInstanceState[] first = ComponentInstanceState.values();
      final ComponentInstanceState[] second = ComponentInstanceState.values();
      assertTrue(first != second, "values() should return a new array on each call");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return enum name as toString")
    void shouldReturnEnumNameAsToString() {
      for (final ComponentInstanceState value : ComponentInstanceState.values()) {
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
      for (final ComponentInstanceState state : ComponentInstanceState.values()) {
        final String result;
        switch (state) {
          case INITIALIZING:
          case ACTIVE:
          case SUSPENDED:
          case ERROR:
          case TERMINATING:
          case TERMINATED:
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

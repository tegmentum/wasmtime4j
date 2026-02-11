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
 * Tests for {@link InstanceState}.
 *
 * <p>Verifies enum structure, constants, valueOf/values, ordinals, and toString.
 */
@DisplayName("InstanceState Tests")
class InstanceStateTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(InstanceState.class.isEnum(), "InstanceState should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 7 values")
    void shouldHaveExactValueCount() {
      assertEquals(7, InstanceState.values().length, "InstanceState should have exactly 7 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain CREATING")
    void shouldContainCreating() {
      assertNotNull(InstanceState.CREATING, "CREATING constant should exist");
    }

    @Test
    @DisplayName("should contain CREATED")
    void shouldContainCreated() {
      assertNotNull(InstanceState.CREATED, "CREATED constant should exist");
    }

    @Test
    @DisplayName("should contain RUNNING")
    void shouldContainRunning() {
      assertNotNull(InstanceState.RUNNING, "RUNNING constant should exist");
    }

    @Test
    @DisplayName("should contain SUSPENDED")
    void shouldContainSuspended() {
      assertNotNull(InstanceState.SUSPENDED, "SUSPENDED constant should exist");
    }

    @Test
    @DisplayName("should contain ERROR")
    void shouldContainError() {
      assertNotNull(InstanceState.ERROR, "ERROR constant should exist");
    }

    @Test
    @DisplayName("should contain DISPOSED")
    void shouldContainDisposed() {
      assertNotNull(InstanceState.DISPOSED, "DISPOSED constant should exist");
    }

    @Test
    @DisplayName("should contain DESTROYING")
    void shouldContainDestroying() {
      assertNotNull(InstanceState.DESTROYING, "DESTROYING constant should exist");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final InstanceState value : InstanceState.values()) {
        ordinals.add(value.ordinal());
      }
      assertEquals(InstanceState.values().length, ordinals.size(), "All ordinals should be unique");
    }

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final InstanceState[] values = InstanceState.values();
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
      for (final InstanceState value : InstanceState.values()) {
        assertEquals(
            value, InstanceState.valueOf(value.name()), "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> InstanceState.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call")
    void shouldReturnNewArrayOnEachCall() {
      final InstanceState[] first = InstanceState.values();
      final InstanceState[] second = InstanceState.values();
      assertTrue(first != second, "values() should return a new array on each call");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return enum name as toString")
    void shouldReturnEnumNameAsToString() {
      for (final InstanceState value : InstanceState.values()) {
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
      for (final InstanceState state : InstanceState.values()) {
        final String result;
        switch (state) {
          case CREATING:
          case CREATED:
          case RUNNING:
          case SUSPENDED:
          case ERROR:
          case DISPOSED:
          case DESTROYING:
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

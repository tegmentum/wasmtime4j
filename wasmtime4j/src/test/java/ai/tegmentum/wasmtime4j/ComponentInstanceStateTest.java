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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentInstanceState} enum.
 *
 * <p>ComponentInstanceState represents the state of a WebAssembly component instance.
 */
@DisplayName("ComponentInstanceState Tests")
class ComponentInstanceStateTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be public enum")
    void shouldBePublicEnum() {
      assertTrue(
          Modifier.isPublic(ComponentInstanceState.class.getModifiers()),
          "ComponentInstanceState should be public");
      assertTrue(ComponentInstanceState.class.isEnum(), "ComponentInstanceState should be an enum");
    }

    @Test
    @DisplayName("should have correct number of values")
    void shouldHaveCorrectNumberOfValues() {
      final var values = ComponentInstanceState.values();
      assertEquals(6, values.length, "Should have 6 instance states");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have INITIALIZING state")
    void shouldHaveInitializingState() {
      assertEquals(
          ComponentInstanceState.INITIALIZING, ComponentInstanceState.valueOf("INITIALIZING"));
    }

    @Test
    @DisplayName("should have ACTIVE state")
    void shouldHaveActiveState() {
      assertEquals(ComponentInstanceState.ACTIVE, ComponentInstanceState.valueOf("ACTIVE"));
    }

    @Test
    @DisplayName("should have SUSPENDED state")
    void shouldHaveSuspendedState() {
      assertEquals(ComponentInstanceState.SUSPENDED, ComponentInstanceState.valueOf("SUSPENDED"));
    }

    @Test
    @DisplayName("should have ERROR state")
    void shouldHaveErrorState() {
      assertEquals(ComponentInstanceState.ERROR, ComponentInstanceState.valueOf("ERROR"));
    }

    @Test
    @DisplayName("should have TERMINATING state")
    void shouldHaveTerminatingState() {
      assertEquals(
          ComponentInstanceState.TERMINATING, ComponentInstanceState.valueOf("TERMINATING"));
    }

    @Test
    @DisplayName("should have TERMINATED state")
    void shouldHaveTerminatedState() {
      assertEquals(ComponentInstanceState.TERMINATED, ComponentInstanceState.valueOf("TERMINATED"));
    }
  }

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("INITIALIZING should have ordinal 0")
    void initializingShouldHaveOrdinalZero() {
      assertEquals(
          0, ComponentInstanceState.INITIALIZING.ordinal(), "INITIALIZING should have ordinal 0");
    }

    @Test
    @DisplayName("ordinals should be sequential")
    void ordinalsShouldBeSequential() {
      final var values = ComponentInstanceState.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal should be sequential for " + values[i]);
      }
    }
  }

  @Nested
  @DisplayName("Name Tests")
  class NameTests {

    @Test
    @DisplayName("name should match enum constant")
    void nameShouldMatchEnumConstant() {
      for (ComponentInstanceState state : ComponentInstanceState.values()) {
        assertNotNull(state.name(), "Name should not be null");
        assertEquals(state, ComponentInstanceState.valueOf(state.name()), "valueOf should work");
      }
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return state name")
    void toStringShouldReturnStateName() {
      for (ComponentInstanceState state : ComponentInstanceState.values()) {
        assertEquals(state.name(), state.toString(), "toString should match name");
      }
    }
  }
}

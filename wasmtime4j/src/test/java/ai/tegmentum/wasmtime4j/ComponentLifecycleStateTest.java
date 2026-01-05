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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the ComponentLifecycleState enum.
 *
 * <p>ComponentLifecycleState represents the lifecycle state of a WebAssembly component from
 * creation through disposal.
 */
@DisplayName("ComponentLifecycleState Enum Tests")
class ComponentLifecycleStateTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          ComponentLifecycleState.class.isEnum(), "ComponentLifecycleState should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentLifecycleState.class.getModifiers()),
          "ComponentLifecycleState should be public");
    }

    @Test
    @DisplayName("should be final (enums are implicitly final)")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(ComponentLifecycleState.class.getModifiers()),
          "ComponentLifecycleState should be final (enums are implicitly final)");
    }
  }

  // ========================================================================
  // Enum Constants Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Constants Tests")
  class EnumConstantsTests {

    @Test
    @DisplayName("should have CREATING constant")
    void shouldHaveCreatingConstant() {
      assertNotNull(ComponentLifecycleState.CREATING, "CREATING constant should exist");
    }

    @Test
    @DisplayName("should have READY constant")
    void shouldHaveReadyConstant() {
      assertNotNull(ComponentLifecycleState.READY, "READY constant should exist");
    }

    @Test
    @DisplayName("should have ACTIVE constant")
    void shouldHaveActiveConstant() {
      assertNotNull(ComponentLifecycleState.ACTIVE, "ACTIVE constant should exist");
    }

    @Test
    @DisplayName("should have SUSPENDED constant")
    void shouldHaveSuspendedConstant() {
      assertNotNull(ComponentLifecycleState.SUSPENDED, "SUSPENDED constant should exist");
    }

    @Test
    @DisplayName("should have UPDATING constant")
    void shouldHaveUpdatingConstant() {
      assertNotNull(ComponentLifecycleState.UPDATING, "UPDATING constant should exist");
    }

    @Test
    @DisplayName("should have DEPRECATED constant")
    void shouldHaveDeprecatedConstant() {
      assertNotNull(ComponentLifecycleState.DEPRECATED, "DEPRECATED constant should exist");
    }

    @Test
    @DisplayName("should have ERROR constant")
    void shouldHaveErrorConstant() {
      assertNotNull(ComponentLifecycleState.ERROR, "ERROR constant should exist");
    }

    @Test
    @DisplayName("should have DESTROYING constant")
    void shouldHaveDestroyingConstant() {
      assertNotNull(ComponentLifecycleState.DESTROYING, "DESTROYING constant should exist");
    }

    @Test
    @DisplayName("should have DESTROYED constant")
    void shouldHaveDestroyedConstant() {
      assertNotNull(ComponentLifecycleState.DESTROYED, "DESTROYED constant should exist");
    }

    @Test
    @DisplayName("should have exactly 9 enum constants")
    void shouldHaveExactly9EnumConstants() {
      assertEquals(
          9,
          ComponentLifecycleState.values().length,
          "ComponentLifecycleState should have exactly 9 constants");
    }

    @Test
    @DisplayName("should have all expected enum constants")
    void shouldHaveAllExpectedEnumConstants() {
      Set<String> expectedConstants =
          Set.of(
              "CREATING",
              "READY",
              "ACTIVE",
              "SUSPENDED",
              "UPDATING",
              "DEPRECATED",
              "ERROR",
              "DESTROYING",
              "DESTROYED");

      Set<String> actualConstants =
          Arrays.stream(ComponentLifecycleState.values())
              .map(Enum::name)
              .collect(Collectors.toSet());

      assertEquals(expectedConstants, actualConstants, "Should have all expected enum constants");
    }
  }

  // ========================================================================
  // Enum Behavior Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Behavior Tests")
  class EnumBehaviorTests {

    @Test
    @DisplayName("valueOf should return correct constant for CREATING")
    void valueOfShouldReturnCorrectConstantForCreating() {
      assertEquals(
          ComponentLifecycleState.CREATING,
          ComponentLifecycleState.valueOf("CREATING"),
          "valueOf should return CREATING");
    }

    @Test
    @DisplayName("valueOf should return correct constant for ACTIVE")
    void valueOfShouldReturnCorrectConstantForActive() {
      assertEquals(
          ComponentLifecycleState.ACTIVE,
          ComponentLifecycleState.valueOf("ACTIVE"),
          "valueOf should return ACTIVE");
    }

    @Test
    @DisplayName("valueOf should return correct constant for DESTROYED")
    void valueOfShouldReturnCorrectConstantForDestroyed() {
      assertEquals(
          ComponentLifecycleState.DESTROYED,
          ComponentLifecycleState.valueOf("DESTROYED"),
          "valueOf should return DESTROYED");
    }

    @Test
    @DisplayName("name should return correct string for each constant")
    void nameShouldReturnCorrectString() {
      assertEquals("CREATING", ComponentLifecycleState.CREATING.name());
      assertEquals("READY", ComponentLifecycleState.READY.name());
      assertEquals("ACTIVE", ComponentLifecycleState.ACTIVE.name());
      assertEquals("SUSPENDED", ComponentLifecycleState.SUSPENDED.name());
      assertEquals("UPDATING", ComponentLifecycleState.UPDATING.name());
      assertEquals("DEPRECATED", ComponentLifecycleState.DEPRECATED.name());
      assertEquals("ERROR", ComponentLifecycleState.ERROR.name());
      assertEquals("DESTROYING", ComponentLifecycleState.DESTROYING.name());
      assertEquals("DESTROYED", ComponentLifecycleState.DESTROYED.name());
    }

    @Test
    @DisplayName("ordinal should return correct index")
    void ordinalShouldReturnCorrectIndex() {
      assertEquals(0, ComponentLifecycleState.CREATING.ordinal(), "CREATING should be at index 0");
      assertEquals(1, ComponentLifecycleState.READY.ordinal(), "READY should be at index 1");
      assertEquals(2, ComponentLifecycleState.ACTIVE.ordinal(), "ACTIVE should be at index 2");
    }

    @Test
    @DisplayName("toString should return enum name")
    void toStringShouldReturnEnumName() {
      assertEquals("CREATING", ComponentLifecycleState.CREATING.toString());
      assertEquals("ACTIVE", ComponentLifecycleState.ACTIVE.toString());
      assertEquals("DESTROYED", ComponentLifecycleState.DESTROYED.toString());
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Enum")
    void shouldExtendEnum() {
      assertTrue(
          Enum.class.isAssignableFrom(ComponentLifecycleState.class),
          "ComponentLifecycleState should extend Enum");
    }

    @Test
    @DisplayName("should not implement any additional interfaces")
    void shouldNotImplementAnyAdditionalInterfaces() {
      Class<?>[] interfaces = ComponentLifecycleState.class.getInterfaces();
      assertEquals(
          0,
          interfaces.length,
          "ComponentLifecycleState should not implement additional interfaces directly");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("values should return all constants")
    void valuesShouldReturnAllConstants() {
      ComponentLifecycleState[] values = ComponentLifecycleState.values();
      assertNotNull(values, "values() should not return null");
      assertEquals(9, values.length, "values() should return 9 constants");
    }

    @Test
    @DisplayName("values should return constants in declaration order")
    void valuesShouldReturnConstantsInDeclarationOrder() {
      ComponentLifecycleState[] values = ComponentLifecycleState.values();
      assertEquals(ComponentLifecycleState.CREATING, values[0]);
      assertEquals(ComponentLifecycleState.READY, values[1]);
      assertEquals(ComponentLifecycleState.ACTIVE, values[2]);
      assertEquals(ComponentLifecycleState.SUSPENDED, values[3]);
      assertEquals(ComponentLifecycleState.UPDATING, values[4]);
      assertEquals(ComponentLifecycleState.DEPRECATED, values[5]);
      assertEquals(ComponentLifecycleState.ERROR, values[6]);
      assertEquals(ComponentLifecycleState.DESTROYING, values[7]);
      assertEquals(ComponentLifecycleState.DESTROYED, values[8]);
    }
  }

  // ========================================================================
  // State Category Tests
  // ========================================================================

  @Nested
  @DisplayName("State Category Tests")
  class StateCategoryTests {

    @Test
    @DisplayName("should have initialization states")
    void shouldHaveInitializationStates() {
      Set<ComponentLifecycleState> initStates =
          Set.of(ComponentLifecycleState.CREATING, ComponentLifecycleState.READY);

      for (ComponentLifecycleState state : initStates) {
        assertTrue(
            Arrays.asList(ComponentLifecycleState.values()).contains(state),
            "Should contain initialization state: " + state);
      }
    }

    @Test
    @DisplayName("should have operational states")
    void shouldHaveOperationalStates() {
      Set<ComponentLifecycleState> operationalStates =
          Set.of(
              ComponentLifecycleState.ACTIVE,
              ComponentLifecycleState.SUSPENDED,
              ComponentLifecycleState.UPDATING,
              ComponentLifecycleState.DEPRECATED);

      for (ComponentLifecycleState state : operationalStates) {
        assertTrue(
            Arrays.asList(ComponentLifecycleState.values()).contains(state),
            "Should contain operational state: " + state);
      }
    }

    @Test
    @DisplayName("should have terminal states")
    void shouldHaveTerminalStates() {
      Set<ComponentLifecycleState> terminalStates =
          Set.of(
              ComponentLifecycleState.ERROR,
              ComponentLifecycleState.DESTROYING,
              ComponentLifecycleState.DESTROYED);

      for (ComponentLifecycleState state : terminalStates) {
        assertTrue(
            Arrays.asList(ComponentLifecycleState.values()).contains(state),
            "Should contain terminal state: " + state);
      }
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no additional instance fields")
    void shouldHaveNoAdditionalInstanceFields() {
      long nonEnumFields =
          Arrays.stream(ComponentLifecycleState.class.getDeclaredFields())
              .filter(f -> !f.isEnumConstant())
              .filter(f -> !f.isSynthetic())
              .filter(f -> !f.getName().equals("$VALUES"))
              .count();
      assertEquals(
          0, nonEnumFields, "ComponentLifecycleState should have no additional instance fields");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          ComponentLifecycleState.class.getDeclaredClasses().length,
          "ComponentLifecycleState should have no nested classes");
    }
  }

  // ========================================================================
  // Comparison Tests
  // ========================================================================

  @Nested
  @DisplayName("Comparison Tests")
  class ComparisonTests {

    @Test
    @DisplayName("enum constants should be comparable")
    void enumConstantsShouldBeComparable() {
      assertTrue(
          ComponentLifecycleState.CREATING.compareTo(ComponentLifecycleState.READY) < 0,
          "CREATING should come before READY");
      assertTrue(
          ComponentLifecycleState.READY.compareTo(ComponentLifecycleState.CREATING) > 0,
          "READY should come after CREATING");
      assertEquals(
          0,
          ComponentLifecycleState.CREATING.compareTo(ComponentLifecycleState.CREATING),
          "Same constant should compare equal");
    }

    @Test
    @DisplayName("enum constants should support equals")
    void enumConstantsShouldSupportEquals() {
      assertEquals(ComponentLifecycleState.CREATING, ComponentLifecycleState.CREATING);
      assertFalse(ComponentLifecycleState.CREATING.equals(ComponentLifecycleState.READY));
    }
  }

  // ========================================================================
  // Lifecycle Flow Tests
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Flow Tests")
  class LifecycleFlowTests {

    @Test
    @DisplayName("lifecycle should start with CREATING")
    void lifecycleShouldStartWithCreating() {
      assertEquals(
          ComponentLifecycleState.CREATING,
          ComponentLifecycleState.values()[0],
          "Lifecycle should start with CREATING");
    }

    @Test
    @DisplayName("lifecycle should end with DESTROYED")
    void lifecycleShouldEndWithDestroyed() {
      ComponentLifecycleState[] values = ComponentLifecycleState.values();
      assertEquals(
          ComponentLifecycleState.DESTROYED,
          values[values.length - 1],
          "Lifecycle should end with DESTROYED");
    }

    @Test
    @DisplayName("READY should come after CREATING")
    void readyShouldComeAfterCreating() {
      assertTrue(
          ComponentLifecycleState.READY.ordinal() > ComponentLifecycleState.CREATING.ordinal(),
          "READY should come after CREATING");
    }

    @Test
    @DisplayName("ACTIVE should come after READY")
    void activeShouldComeAfterReady() {
      assertTrue(
          ComponentLifecycleState.ACTIVE.ordinal() > ComponentLifecycleState.READY.ordinal(),
          "ACTIVE should come after READY");
    }

    @Test
    @DisplayName("DESTROYING should come before DESTROYED")
    void destroyingShouldComeBeforeDestroyed() {
      assertTrue(
          ComponentLifecycleState.DESTROYING.ordinal()
              < ComponentLifecycleState.DESTROYED.ordinal(),
          "DESTROYING should come before DESTROYED");
    }
  }
}

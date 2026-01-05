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
 * Comprehensive test suite for the WasmThreadState enum.
 *
 * <p>WasmThreadState represents the possible states of a WebAssembly thread during its lifecycle.
 */
@DisplayName("WasmThreadState Enum Tests")
class WasmThreadStateTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasmThreadState.class.isEnum(), "WasmThreadState should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmThreadState.class.getModifiers()),
          "WasmThreadState should be public");
    }

    @Test
    @DisplayName("should be final (enums are implicitly final)")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasmThreadState.class.getModifiers()),
          "WasmThreadState should be final (enums are implicitly final)");
    }
  }

  // ========================================================================
  // Enum Constants Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Constants Tests")
  class EnumConstantsTests {

    @Test
    @DisplayName("should have NEW constant")
    void shouldHaveNewConstant() {
      assertNotNull(WasmThreadState.NEW, "NEW constant should exist");
    }

    @Test
    @DisplayName("should have RUNNING constant")
    void shouldHaveRunningConstant() {
      assertNotNull(WasmThreadState.RUNNING, "RUNNING constant should exist");
    }

    @Test
    @DisplayName("should have WAITING constant")
    void shouldHaveWaitingConstant() {
      assertNotNull(WasmThreadState.WAITING, "WAITING constant should exist");
    }

    @Test
    @DisplayName("should have TIMED_WAITING constant")
    void shouldHaveTimedWaitingConstant() {
      assertNotNull(WasmThreadState.TIMED_WAITING, "TIMED_WAITING constant should exist");
    }

    @Test
    @DisplayName("should have BLOCKED constant")
    void shouldHaveBlockedConstant() {
      assertNotNull(WasmThreadState.BLOCKED, "BLOCKED constant should exist");
    }

    @Test
    @DisplayName("should have SUSPENDED constant")
    void shouldHaveSuspendedConstant() {
      assertNotNull(WasmThreadState.SUSPENDED, "SUSPENDED constant should exist");
    }

    @Test
    @DisplayName("should have TERMINATED constant")
    void shouldHaveTerminatedConstant() {
      assertNotNull(WasmThreadState.TERMINATED, "TERMINATED constant should exist");
    }

    @Test
    @DisplayName("should have ERROR constant")
    void shouldHaveErrorConstant() {
      assertNotNull(WasmThreadState.ERROR, "ERROR constant should exist");
    }

    @Test
    @DisplayName("should have KILLED constant")
    void shouldHaveKilledConstant() {
      assertNotNull(WasmThreadState.KILLED, "KILLED constant should exist");
    }

    @Test
    @DisplayName("should have exactly 9 enum constants")
    void shouldHaveExactly9EnumConstants() {
      assertEquals(
          9, WasmThreadState.values().length, "WasmThreadState should have exactly 9 constants");
    }

    @Test
    @DisplayName("should have all expected enum constants")
    void shouldHaveAllExpectedEnumConstants() {
      Set<String> expectedConstants =
          Set.of(
              "NEW",
              "RUNNING",
              "WAITING",
              "TIMED_WAITING",
              "BLOCKED",
              "SUSPENDED",
              "TERMINATED",
              "ERROR",
              "KILLED");

      Set<String> actualConstants =
          Arrays.stream(WasmThreadState.values()).map(Enum::name).collect(Collectors.toSet());

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
    @DisplayName("valueOf should return correct constant for NEW")
    void valueOfShouldReturnCorrectConstantForNew() {
      assertEquals(
          WasmThreadState.NEW, WasmThreadState.valueOf("NEW"), "valueOf should return NEW");
    }

    @Test
    @DisplayName("valueOf should return correct constant for RUNNING")
    void valueOfShouldReturnCorrectConstantForRunning() {
      assertEquals(
          WasmThreadState.RUNNING,
          WasmThreadState.valueOf("RUNNING"),
          "valueOf should return RUNNING");
    }

    @Test
    @DisplayName("valueOf should return correct constant for TERMINATED")
    void valueOfShouldReturnCorrectConstantForTerminated() {
      assertEquals(
          WasmThreadState.TERMINATED,
          WasmThreadState.valueOf("TERMINATED"),
          "valueOf should return TERMINATED");
    }

    @Test
    @DisplayName("name should return correct string for each constant")
    void nameShouldReturnCorrectString() {
      assertEquals("NEW", WasmThreadState.NEW.name());
      assertEquals("RUNNING", WasmThreadState.RUNNING.name());
      assertEquals("WAITING", WasmThreadState.WAITING.name());
      assertEquals("TIMED_WAITING", WasmThreadState.TIMED_WAITING.name());
      assertEquals("BLOCKED", WasmThreadState.BLOCKED.name());
      assertEquals("SUSPENDED", WasmThreadState.SUSPENDED.name());
      assertEquals("TERMINATED", WasmThreadState.TERMINATED.name());
      assertEquals("ERROR", WasmThreadState.ERROR.name());
      assertEquals("KILLED", WasmThreadState.KILLED.name());
    }

    @Test
    @DisplayName("ordinal should return correct index")
    void ordinalShouldReturnCorrectIndex() {
      assertEquals(0, WasmThreadState.NEW.ordinal(), "NEW should be at index 0");
      assertEquals(1, WasmThreadState.RUNNING.ordinal(), "RUNNING should be at index 1");
      assertEquals(2, WasmThreadState.WAITING.ordinal(), "WAITING should be at index 2");
    }

    @Test
    @DisplayName("toString should return enum name")
    void toStringShouldReturnEnumName() {
      assertEquals("NEW", WasmThreadState.NEW.toString());
      assertEquals("RUNNING", WasmThreadState.RUNNING.toString());
      assertEquals("TERMINATED", WasmThreadState.TERMINATED.toString());
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
          Enum.class.isAssignableFrom(WasmThreadState.class), "WasmThreadState should extend Enum");
    }

    @Test
    @DisplayName("should not implement any additional interfaces")
    void shouldNotImplementAnyAdditionalInterfaces() {
      Class<?>[] interfaces = WasmThreadState.class.getInterfaces();
      assertEquals(
          0,
          interfaces.length,
          "WasmThreadState should not implement additional interfaces directly");
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
      WasmThreadState[] values = WasmThreadState.values();
      assertNotNull(values, "values() should not return null");
      assertEquals(9, values.length, "values() should return 9 constants");
    }

    @Test
    @DisplayName("values should return constants in declaration order")
    void valuesShouldReturnConstantsInDeclarationOrder() {
      WasmThreadState[] values = WasmThreadState.values();
      assertEquals(WasmThreadState.NEW, values[0]);
      assertEquals(WasmThreadState.RUNNING, values[1]);
      assertEquals(WasmThreadState.WAITING, values[2]);
      assertEquals(WasmThreadState.TIMED_WAITING, values[3]);
      assertEquals(WasmThreadState.BLOCKED, values[4]);
      assertEquals(WasmThreadState.SUSPENDED, values[5]);
      assertEquals(WasmThreadState.TERMINATED, values[6]);
      assertEquals(WasmThreadState.ERROR, values[7]);
      assertEquals(WasmThreadState.KILLED, values[8]);
    }
  }

  // ========================================================================
  // State Category Tests
  // ========================================================================

  @Nested
  @DisplayName("State Category Tests")
  class StateCategoryTests {

    @Test
    @DisplayName("should have active states")
    void shouldHaveActiveStates() {
      Set<WasmThreadState> activeStates = Set.of(WasmThreadState.NEW, WasmThreadState.RUNNING);

      for (WasmThreadState state : activeStates) {
        assertTrue(
            Arrays.asList(WasmThreadState.values()).contains(state),
            "Should contain active state: " + state);
      }
    }

    @Test
    @DisplayName("should have waiting states")
    void shouldHaveWaitingStates() {
      Set<WasmThreadState> waitingStates =
          Set.of(
              WasmThreadState.WAITING,
              WasmThreadState.TIMED_WAITING,
              WasmThreadState.BLOCKED,
              WasmThreadState.SUSPENDED);

      for (WasmThreadState state : waitingStates) {
        assertTrue(
            Arrays.asList(WasmThreadState.values()).contains(state),
            "Should contain waiting state: " + state);
      }
    }

    @Test
    @DisplayName("should have terminal states")
    void shouldHaveTerminalStates() {
      Set<WasmThreadState> terminalStates =
          Set.of(WasmThreadState.TERMINATED, WasmThreadState.ERROR, WasmThreadState.KILLED);

      for (WasmThreadState state : terminalStates) {
        assertTrue(
            Arrays.asList(WasmThreadState.values()).contains(state),
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
          Arrays.stream(WasmThreadState.class.getDeclaredFields())
              .filter(f -> !f.isEnumConstant())
              .filter(f -> !f.isSynthetic())
              .filter(f -> !f.getName().equals("$VALUES"))
              .count();
      assertEquals(0, nonEnumFields, "WasmThreadState should have no additional instance fields");
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
          WasmThreadState.class.getDeclaredClasses().length,
          "WasmThreadState should have no nested classes");
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
          WasmThreadState.NEW.compareTo(WasmThreadState.RUNNING) < 0,
          "NEW should come before RUNNING");
      assertTrue(
          WasmThreadState.RUNNING.compareTo(WasmThreadState.NEW) > 0,
          "RUNNING should come after NEW");
      assertEquals(
          0,
          WasmThreadState.NEW.compareTo(WasmThreadState.NEW),
          "Same constant should compare equal");
    }

    @Test
    @DisplayName("enum constants should support equals")
    void enumConstantsShouldSupportEquals() {
      assertEquals(WasmThreadState.NEW, WasmThreadState.NEW);
      assertFalse(WasmThreadState.NEW.equals(WasmThreadState.RUNNING));
    }
  }

  // ========================================================================
  // Java Thread State Parallel Tests
  // ========================================================================

  @Nested
  @DisplayName("Java Thread State Parallel Tests")
  class JavaThreadStateParallelTests {

    @Test
    @DisplayName("should have states parallel to Java Thread.State")
    void shouldHaveStatesParallelToJavaThreadState() {
      // WasmThreadState has similar concepts to Java's Thread.State
      // NEW, RUNNABLE->RUNNING, WAITING, TIMED_WAITING, BLOCKED, TERMINATED
      assertNotNull(WasmThreadState.NEW, "NEW parallel to Thread.State.NEW");
      assertNotNull(WasmThreadState.RUNNING, "RUNNING parallel to Thread.State.RUNNABLE");
      assertNotNull(WasmThreadState.WAITING, "WAITING parallel to Thread.State.WAITING");
      assertNotNull(
          WasmThreadState.TIMED_WAITING, "TIMED_WAITING parallel to Thread.State.TIMED_WAITING");
      assertNotNull(WasmThreadState.BLOCKED, "BLOCKED parallel to Thread.State.BLOCKED");
      assertNotNull(WasmThreadState.TERMINATED, "TERMINATED parallel to Thread.State.TERMINATED");
    }

    @Test
    @DisplayName("should have WebAssembly-specific states")
    void shouldHaveWebAssemblySpecificStates() {
      // These are WebAssembly-specific and not in Java's Thread.State
      assertNotNull(WasmThreadState.SUSPENDED, "SUSPENDED is WebAssembly-specific");
      assertNotNull(WasmThreadState.ERROR, "ERROR is WebAssembly-specific");
      assertNotNull(WasmThreadState.KILLED, "KILLED is WebAssembly-specific");
    }
  }
}

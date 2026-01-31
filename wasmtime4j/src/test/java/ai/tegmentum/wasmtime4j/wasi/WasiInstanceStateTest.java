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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiInstanceState} enum.
 *
 * <p>Verifies instance state values, boolean lifecycle methods, and description retrieval.
 */
@DisplayName("WasiInstanceState Tests")
class WasiInstanceStateTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasiInstanceState should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiInstanceState.class.isEnum(), "WasiInstanceState should be an enum");
    }

    @Test
    @DisplayName("WasiInstanceState should have exactly 8 values")
    void shouldHaveExactlyEightValues() {
      assertEquals(
          8, WasiInstanceState.values().length, "Should have exactly 8 instance state values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have CREATED value")
    void shouldHaveCreatedValue() {
      assertNotNull(WasiInstanceState.CREATED, "CREATED should exist");
      assertEquals("CREATED", WasiInstanceState.CREATED.name(), "Name should be CREATED");
    }

    @Test
    @DisplayName("should have RUNNING value")
    void shouldHaveRunningValue() {
      assertNotNull(WasiInstanceState.RUNNING, "RUNNING should exist");
      assertEquals("RUNNING", WasiInstanceState.RUNNING.name(), "Name should be RUNNING");
    }

    @Test
    @DisplayName("should have SUSPENDED value")
    void shouldHaveSuspendedValue() {
      assertNotNull(WasiInstanceState.SUSPENDED, "SUSPENDED should exist");
      assertEquals("SUSPENDED", WasiInstanceState.SUSPENDED.name(), "Name should be SUSPENDED");
    }

    @Test
    @DisplayName("should have WAITING value")
    void shouldHaveWaitingValue() {
      assertNotNull(WasiInstanceState.WAITING, "WAITING should exist");
      assertEquals("WAITING", WasiInstanceState.WAITING.name(), "Name should be WAITING");
    }

    @Test
    @DisplayName("should have COMPLETED value")
    void shouldHaveCompletedValue() {
      assertNotNull(WasiInstanceState.COMPLETED, "COMPLETED should exist");
      assertEquals("COMPLETED", WasiInstanceState.COMPLETED.name(), "Name should be COMPLETED");
    }

    @Test
    @DisplayName("should have TERMINATED value")
    void shouldHaveTerminatedValue() {
      assertNotNull(WasiInstanceState.TERMINATED, "TERMINATED should exist");
      assertEquals(
          "TERMINATED", WasiInstanceState.TERMINATED.name(), "Name should be TERMINATED");
    }

    @Test
    @DisplayName("should have ERROR value")
    void shouldHaveErrorValue() {
      assertNotNull(WasiInstanceState.ERROR, "ERROR should exist");
      assertEquals("ERROR", WasiInstanceState.ERROR.name(), "Name should be ERROR");
    }

    @Test
    @DisplayName("should have CLOSED value")
    void shouldHaveClosedValue() {
      assertNotNull(WasiInstanceState.CLOSED, "CLOSED should exist");
      assertEquals("CLOSED", WasiInstanceState.CLOSED.name(), "Name should be CLOSED");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("ordinals should be unique")
    void ordinalsShouldBeUnique() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final WasiInstanceState state : WasiInstanceState.values()) {
        assertTrue(
            ordinals.add(state.ordinal()), "Ordinal should be unique: " + state.ordinal());
      }
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final WasiInstanceState[] values = WasiInstanceState.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal should match index for " + values[i]);
      }
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return correct constant for each name")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(
          WasiInstanceState.CREATED,
          WasiInstanceState.valueOf("CREATED"),
          "Should return CREATED");
      assertEquals(
          WasiInstanceState.RUNNING,
          WasiInstanceState.valueOf("RUNNING"),
          "Should return RUNNING");
      assertEquals(
          WasiInstanceState.SUSPENDED,
          WasiInstanceState.valueOf("SUSPENDED"),
          "Should return SUSPENDED");
      assertEquals(
          WasiInstanceState.WAITING,
          WasiInstanceState.valueOf("WAITING"),
          "Should return WAITING");
      assertEquals(
          WasiInstanceState.COMPLETED,
          WasiInstanceState.valueOf("COMPLETED"),
          "Should return COMPLETED");
      assertEquals(
          WasiInstanceState.TERMINATED,
          WasiInstanceState.valueOf("TERMINATED"),
          "Should return TERMINATED");
      assertEquals(
          WasiInstanceState.ERROR, WasiInstanceState.valueOf("ERROR"), "Should return ERROR");
      assertEquals(
          WasiInstanceState.CLOSED, WasiInstanceState.valueOf("CLOSED"), "Should return CLOSED");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiInstanceState.valueOf("INVALID"),
          "Should throw for invalid enum name");
    }
  }

  @Nested
  @DisplayName("values() Tests")
  class ValuesTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllEnumConstants() {
      final WasiInstanceState[] values = WasiInstanceState.values();
      final Set<WasiInstanceState> valueSet = new HashSet<>(Arrays.asList(values));

      assertTrue(valueSet.contains(WasiInstanceState.CREATED), "Should contain CREATED");
      assertTrue(valueSet.contains(WasiInstanceState.RUNNING), "Should contain RUNNING");
      assertTrue(valueSet.contains(WasiInstanceState.SUSPENDED), "Should contain SUSPENDED");
      assertTrue(valueSet.contains(WasiInstanceState.WAITING), "Should contain WAITING");
      assertTrue(valueSet.contains(WasiInstanceState.COMPLETED), "Should contain COMPLETED");
      assertTrue(valueSet.contains(WasiInstanceState.TERMINATED), "Should contain TERMINATED");
      assertTrue(valueSet.contains(WasiInstanceState.ERROR), "Should contain ERROR");
      assertTrue(valueSet.contains(WasiInstanceState.CLOSED), "Should contain CLOSED");
    }

    @Test
    @DisplayName("values() should return new array each time")
    void valuesShouldReturnNewArrayEachTime() {
      final WasiInstanceState[] first = WasiInstanceState.values();
      final WasiInstanceState[] second = WasiInstanceState.values();

      assertTrue(first != second, "Should return new array each time");
      assertEquals(first.length, second.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("Boolean Method Tests")
  class BooleanMethodTests {

    @Test
    @DisplayName("isActive should return true for CREATED, RUNNING, SUSPENDED, WAITING, COMPLETED")
    void isActiveShouldReturnTrueForActiveStates() {
      assertTrue(
          WasiInstanceState.CREATED.isActive(), "CREATED.isActive() should be true");
      assertTrue(
          WasiInstanceState.RUNNING.isActive(), "RUNNING.isActive() should be true");
      assertTrue(
          WasiInstanceState.SUSPENDED.isActive(), "SUSPENDED.isActive() should be true");
      assertTrue(
          WasiInstanceState.WAITING.isActive(), "WAITING.isActive() should be true");
      assertTrue(
          WasiInstanceState.COMPLETED.isActive(), "COMPLETED.isActive() should be true");
      assertFalse(
          WasiInstanceState.TERMINATED.isActive(), "TERMINATED.isActive() should be false");
      assertFalse(WasiInstanceState.ERROR.isActive(), "ERROR.isActive() should be false");
      assertFalse(WasiInstanceState.CLOSED.isActive(), "CLOSED.isActive() should be false");
    }

    @Test
    @DisplayName("isTerminal should return true for TERMINATED, ERROR, CLOSED")
    void isTerminalShouldReturnTrueForTerminalStates() {
      assertTrue(
          WasiInstanceState.TERMINATED.isTerminal(), "TERMINATED.isTerminal() should be true");
      assertTrue(WasiInstanceState.ERROR.isTerminal(), "ERROR.isTerminal() should be true");
      assertTrue(WasiInstanceState.CLOSED.isTerminal(), "CLOSED.isTerminal() should be true");
      assertFalse(
          WasiInstanceState.CREATED.isTerminal(), "CREATED.isTerminal() should be false");
      assertFalse(
          WasiInstanceState.RUNNING.isTerminal(), "RUNNING.isTerminal() should be false");
      assertFalse(
          WasiInstanceState.SUSPENDED.isTerminal(), "SUSPENDED.isTerminal() should be false");
      assertFalse(
          WasiInstanceState.WAITING.isTerminal(), "WAITING.isTerminal() should be false");
      assertFalse(
          WasiInstanceState.COMPLETED.isTerminal(), "COMPLETED.isTerminal() should be false");
    }

    @Test
    @DisplayName("isCallable should return true for CREATED, COMPLETED")
    void isCallableShouldReturnTrueForCallableStates() {
      assertTrue(
          WasiInstanceState.CREATED.isCallable(), "CREATED.isCallable() should be true");
      assertTrue(
          WasiInstanceState.COMPLETED.isCallable(), "COMPLETED.isCallable() should be true");
      assertFalse(
          WasiInstanceState.RUNNING.isCallable(), "RUNNING.isCallable() should be false");
      assertFalse(
          WasiInstanceState.SUSPENDED.isCallable(), "SUSPENDED.isCallable() should be false");
      assertFalse(
          WasiInstanceState.WAITING.isCallable(), "WAITING.isCallable() should be false");
      assertFalse(
          WasiInstanceState.TERMINATED.isCallable(), "TERMINATED.isCallable() should be false");
      assertFalse(WasiInstanceState.ERROR.isCallable(), "ERROR.isCallable() should be false");
      assertFalse(WasiInstanceState.CLOSED.isCallable(), "CLOSED.isCallable() should be false");
    }

    @Test
    @DisplayName("isSuspendable should return true for RUNNING, WAITING")
    void isSuspendableShouldReturnTrueForSuspendableStates() {
      assertTrue(
          WasiInstanceState.RUNNING.isSuspendable(), "RUNNING.isSuspendable() should be true");
      assertTrue(
          WasiInstanceState.WAITING.isSuspendable(), "WAITING.isSuspendable() should be true");
      assertFalse(
          WasiInstanceState.CREATED.isSuspendable(), "CREATED.isSuspendable() should be false");
      assertFalse(
          WasiInstanceState.SUSPENDED.isSuspendable(),
          "SUSPENDED.isSuspendable() should be false");
      assertFalse(
          WasiInstanceState.COMPLETED.isSuspendable(),
          "COMPLETED.isSuspendable() should be false");
      assertFalse(
          WasiInstanceState.TERMINATED.isSuspendable(),
          "TERMINATED.isSuspendable() should be false");
      assertFalse(
          WasiInstanceState.ERROR.isSuspendable(), "ERROR.isSuspendable() should be false");
      assertFalse(
          WasiInstanceState.CLOSED.isSuspendable(), "CLOSED.isSuspendable() should be false");
    }

    @Test
    @DisplayName("isResumable should return true only for SUSPENDED")
    void isResumableShouldReturnTrueOnlyForSuspended() {
      assertTrue(
          WasiInstanceState.SUSPENDED.isResumable(), "SUSPENDED.isResumable() should be true");
      for (final WasiInstanceState state : WasiInstanceState.values()) {
        if (state != WasiInstanceState.SUSPENDED) {
          assertFalse(
              state.isResumable(), state + ".isResumable() should be false");
        }
      }
    }
  }

  @Nested
  @DisplayName("GetDescription Tests")
  class GetDescriptionTests {

    @Test
    @DisplayName("each constant should have a non-empty description")
    void eachConstantShouldHaveNonEmptyDescription() {
      for (final WasiInstanceState state : WasiInstanceState.values()) {
        assertNotNull(state.getDescription(), "Description should not be null for " + state);
        assertFalse(
            state.getDescription().isEmpty(),
            "Description should not be empty for " + state);
      }
    }
  }
}

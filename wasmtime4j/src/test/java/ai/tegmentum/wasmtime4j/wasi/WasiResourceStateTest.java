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
 * Tests for the {@link WasiResourceState} enum.
 *
 * <p>Verifies resource state values, boolean lifecycle methods, and terminal state detection.
 */
@DisplayName("WasiResourceState Tests")
class WasiResourceStateTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasiResourceState should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiResourceState.class.isEnum(), "WasiResourceState should be an enum");
    }

    @Test
    @DisplayName("WasiResourceState should have exactly 6 values")
    void shouldHaveExactlySixValues() {
      assertEquals(
          6, WasiResourceState.values().length, "Should have exactly 6 resource state values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have CREATED value")
    void shouldHaveCreatedValue() {
      assertNotNull(WasiResourceState.CREATED, "CREATED should exist");
      assertEquals("CREATED", WasiResourceState.CREATED.name(), "Name should be CREATED");
    }

    @Test
    @DisplayName("should have OPEN value")
    void shouldHaveOpenValue() {
      assertNotNull(WasiResourceState.OPEN, "OPEN should exist");
      assertEquals("OPEN", WasiResourceState.OPEN.name(), "Name should be OPEN");
    }

    @Test
    @DisplayName("should have ACTIVE value")
    void shouldHaveActiveValue() {
      assertNotNull(WasiResourceState.ACTIVE, "ACTIVE should exist");
      assertEquals("ACTIVE", WasiResourceState.ACTIVE.name(), "Name should be ACTIVE");
    }

    @Test
    @DisplayName("should have SUSPENDED value")
    void shouldHaveSuspendedValue() {
      assertNotNull(WasiResourceState.SUSPENDED, "SUSPENDED should exist");
      assertEquals("SUSPENDED", WasiResourceState.SUSPENDED.name(), "Name should be SUSPENDED");
    }

    @Test
    @DisplayName("should have ERROR value")
    void shouldHaveErrorValue() {
      assertNotNull(WasiResourceState.ERROR, "ERROR should exist");
      assertEquals("ERROR", WasiResourceState.ERROR.name(), "Name should be ERROR");
    }

    @Test
    @DisplayName("should have CLOSED value")
    void shouldHaveClosedValue() {
      assertNotNull(WasiResourceState.CLOSED, "CLOSED should exist");
      assertEquals("CLOSED", WasiResourceState.CLOSED.name(), "Name should be CLOSED");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("ordinals should be unique")
    void ordinalsShouldBeUnique() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final WasiResourceState state : WasiResourceState.values()) {
        assertTrue(ordinals.add(state.ordinal()), "Ordinal should be unique: " + state.ordinal());
      }
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final WasiResourceState[] values = WasiResourceState.values();
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
          WasiResourceState.CREATED, WasiResourceState.valueOf("CREATED"), "Should return CREATED");
      assertEquals(WasiResourceState.OPEN, WasiResourceState.valueOf("OPEN"), "Should return OPEN");
      assertEquals(
          WasiResourceState.ACTIVE, WasiResourceState.valueOf("ACTIVE"), "Should return ACTIVE");
      assertEquals(
          WasiResourceState.SUSPENDED,
          WasiResourceState.valueOf("SUSPENDED"),
          "Should return SUSPENDED");
      assertEquals(
          WasiResourceState.ERROR, WasiResourceState.valueOf("ERROR"), "Should return ERROR");
      assertEquals(
          WasiResourceState.CLOSED, WasiResourceState.valueOf("CLOSED"), "Should return CLOSED");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiResourceState.valueOf("INVALID"),
          "Should throw for invalid enum name");
    }
  }

  @Nested
  @DisplayName("values() Tests")
  class ValuesTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllEnumConstants() {
      final WasiResourceState[] values = WasiResourceState.values();
      final Set<WasiResourceState> valueSet = new HashSet<>(Arrays.asList(values));

      assertTrue(valueSet.contains(WasiResourceState.CREATED), "Should contain CREATED");
      assertTrue(valueSet.contains(WasiResourceState.OPEN), "Should contain OPEN");
      assertTrue(valueSet.contains(WasiResourceState.ACTIVE), "Should contain ACTIVE");
      assertTrue(valueSet.contains(WasiResourceState.SUSPENDED), "Should contain SUSPENDED");
      assertTrue(valueSet.contains(WasiResourceState.ERROR), "Should contain ERROR");
      assertTrue(valueSet.contains(WasiResourceState.CLOSED), "Should contain CLOSED");
    }

    @Test
    @DisplayName("values() should return new array each time")
    void valuesShouldReturnNewArrayEachTime() {
      final WasiResourceState[] first = WasiResourceState.values();
      final WasiResourceState[] second = WasiResourceState.values();

      assertTrue(first != second, "Should return new array each time");
      assertEquals(first.length, second.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("Boolean Method Tests")
  class BooleanMethodTests {

    @Test
    @DisplayName("isUsable should return true only for OPEN and ACTIVE")
    void isUsableShouldReturnTrueOnlyForOpenAndActive() {
      assertTrue(WasiResourceState.OPEN.isUsable(), "OPEN.isUsable() should be true");
      assertTrue(WasiResourceState.ACTIVE.isUsable(), "ACTIVE.isUsable() should be true");
      assertFalse(WasiResourceState.CREATED.isUsable(), "CREATED.isUsable() should be false");
      assertFalse(WasiResourceState.SUSPENDED.isUsable(), "SUSPENDED.isUsable() should be false");
      assertFalse(WasiResourceState.ERROR.isUsable(), "ERROR.isUsable() should be false");
      assertFalse(WasiResourceState.CLOSED.isUsable(), "CLOSED.isUsable() should be false");
    }

    @Test
    @DisplayName("isTerminal should return true only for CLOSED and ERROR")
    void isTerminalShouldReturnTrueOnlyForClosedAndError() {
      assertTrue(WasiResourceState.CLOSED.isTerminal(), "CLOSED.isTerminal() should be true");
      assertTrue(WasiResourceState.ERROR.isTerminal(), "ERROR.isTerminal() should be true");
      assertFalse(WasiResourceState.CREATED.isTerminal(), "CREATED.isTerminal() should be false");
      assertFalse(WasiResourceState.OPEN.isTerminal(), "OPEN.isTerminal() should be false");
      assertFalse(WasiResourceState.ACTIVE.isTerminal(), "ACTIVE.isTerminal() should be false");
      assertFalse(
          WasiResourceState.SUSPENDED.isTerminal(), "SUSPENDED.isTerminal() should be false");
    }
  }
}

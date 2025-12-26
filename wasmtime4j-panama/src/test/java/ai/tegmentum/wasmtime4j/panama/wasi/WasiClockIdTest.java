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

package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiClockId} enum.
 *
 * <p>This test class verifies WasiClockId enum values and methods.
 */
@DisplayName("WasiClockId Tests")
class WasiClockIdTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasiClockId should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiClockId.class.isEnum(), "WasiClockId should be an enum");
    }

    @Test
    @DisplayName("Should have expected number of values")
    void shouldHaveExpectedNumberOfValues() {
      assertEquals(4, WasiClockId.values().length, "Should have 4 clock IDs");
    }

    @Test
    @DisplayName("All enum values should have unique names")
    void allEnumValuesShouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (WasiClockId clockId : WasiClockId.values()) {
        assertTrue(names.add(clockId.name()), "Name should be unique: " + clockId.name());
      }
    }

    @Test
    @DisplayName("All enum values should have unique numeric values")
    void allEnumValuesShouldHaveUniqueValues() {
      final Set<Integer> values = new HashSet<>();
      for (WasiClockId clockId : WasiClockId.values()) {
        assertTrue(values.add(clockId.getValue()), "Value should be unique: " + clockId.name());
      }
    }
  }

  @Nested
  @DisplayName("Value Tests")
  class ValueTests {

    @Test
    @DisplayName("REALTIME should have value 0")
    void realtimeShouldHaveValueZero() {
      assertEquals(0, WasiClockId.REALTIME.getValue(), "REALTIME should have value 0");
    }

    @Test
    @DisplayName("MONOTONIC should have value 1")
    void monotonicShouldHaveValue1() {
      assertEquals(1, WasiClockId.MONOTONIC.getValue(), "MONOTONIC should have value 1");
    }

    @Test
    @DisplayName("PROCESS_CPUTIME_ID should have value 2")
    void processCputimeIdShouldHaveValue2() {
      assertEquals(
          2, WasiClockId.PROCESS_CPUTIME_ID.getValue(), "PROCESS_CPUTIME_ID should have value 2");
    }

    @Test
    @DisplayName("THREAD_CPUTIME_ID should have value 3")
    void threadCputimeIdShouldHaveValue3() {
      assertEquals(
          3, WasiClockId.THREAD_CPUTIME_ID.getValue(), "THREAD_CPUTIME_ID should have value 3");
    }
  }

  @Nested
  @DisplayName("fromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("fromValue should return correct clock ID for valid values")
    void fromValueShouldReturnCorrectClockId() {
      assertEquals(WasiClockId.REALTIME, WasiClockId.fromValue(0), "Should return REALTIME");
      assertEquals(WasiClockId.MONOTONIC, WasiClockId.fromValue(1), "Should return MONOTONIC");
      assertEquals(
          WasiClockId.PROCESS_CPUTIME_ID,
          WasiClockId.fromValue(2),
          "Should return PROCESS_CPUTIME_ID");
      assertEquals(
          WasiClockId.THREAD_CPUTIME_ID,
          WasiClockId.fromValue(3),
          "Should return THREAD_CPUTIME_ID");
    }

    @Test
    @DisplayName("fromValue should throw for invalid value")
    void fromValueShouldThrowForInvalidValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiClockId.fromValue(4),
          "Should throw for value 4");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiClockId.fromValue(-1),
          "Should throw for negative value");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiClockId.fromValue(100),
          "Should throw for value 100");
    }

    @Test
    @DisplayName("Round trip getValue/fromValue should work")
    void roundTripShouldWork() {
      for (WasiClockId clockId : WasiClockId.values()) {
        assertEquals(
            clockId,
            WasiClockId.fromValue(clockId.getValue()),
            "Round trip should return same clock ID: " + clockId.name());
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      final WasiClockId clockId = WasiClockId.MONOTONIC;

      final String result;
      switch (clockId) {
        case REALTIME:
          result = "wall clock";
          break;
        case MONOTONIC:
          result = "monotonic";
          break;
        case PROCESS_CPUTIME_ID:
          result = "process cpu";
          break;
        case THREAD_CPUTIME_ID:
          result = "thread cpu";
          break;
        default:
          result = "unknown";
          break;
      }

      assertEquals("monotonic", result, "Switch should work correctly");
    }

    @Test
    @DisplayName("All values should have non-null names")
    void allValuesShouldHaveNonNullNames() {
      for (WasiClockId clockId : WasiClockId.values()) {
        assertNotNull(clockId.name(), "Name should not be null: " + clockId.ordinal());
        assertFalse(clockId.name().isEmpty(), "Name should not be empty: " + clockId.ordinal());
      }
    }

    @Test
    @DisplayName("Values should be sequential from 0")
    void valuesShouldBeSequentialFromZero() {
      for (int i = 0; i < WasiClockId.values().length; i++) {
        assertNotNull(WasiClockId.fromValue(i), "Should have value for: " + i);
      }
    }
  }
}

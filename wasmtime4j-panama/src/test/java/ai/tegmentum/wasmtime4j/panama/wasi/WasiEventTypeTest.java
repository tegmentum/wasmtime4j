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
 * Tests for the {@link WasiEventType} enum.
 *
 * <p>This test class verifies WasiEventType enum values and methods.
 */
@DisplayName("WasiEventType Tests")
class WasiEventTypeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasiEventType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiEventType.class.isEnum(), "WasiEventType should be an enum");
    }

    @Test
    @DisplayName("Should have expected number of values")
    void shouldHaveExpectedNumberOfValues() {
      assertEquals(3, WasiEventType.values().length, "Should have 3 event types");
    }

    @Test
    @DisplayName("All enum values should have unique names")
    void allEnumValuesShouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (WasiEventType eventType : WasiEventType.values()) {
        assertTrue(names.add(eventType.name()), "Name should be unique: " + eventType.name());
      }
    }

    @Test
    @DisplayName("All enum values should have unique numeric values")
    void allEnumValuesShouldHaveUniqueValues() {
      final Set<Integer> values = new HashSet<>();
      for (WasiEventType eventType : WasiEventType.values()) {
        assertTrue(values.add(eventType.getValue()), "Value should be unique: " + eventType.name());
      }
    }
  }

  @Nested
  @DisplayName("Value Tests")
  class ValueTests {

    @Test
    @DisplayName("CLOCK should have value 0")
    void clockShouldHaveValueZero() {
      assertEquals(0, WasiEventType.CLOCK.getValue(), "CLOCK should have value 0");
    }

    @Test
    @DisplayName("FD_READ should have value 1")
    void fdReadShouldHaveValue1() {
      assertEquals(1, WasiEventType.FD_READ.getValue(), "FD_READ should have value 1");
    }

    @Test
    @DisplayName("FD_WRITE should have value 2")
    void fdWriteShouldHaveValue2() {
      assertEquals(2, WasiEventType.FD_WRITE.getValue(), "FD_WRITE should have value 2");
    }
  }

  @Nested
  @DisplayName("fromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("fromValue should return correct event type for valid values")
    void fromValueShouldReturnCorrectEventType() {
      assertEquals(WasiEventType.CLOCK, WasiEventType.fromValue(0), "Should return CLOCK");
      assertEquals(WasiEventType.FD_READ, WasiEventType.fromValue(1), "Should return FD_READ");
      assertEquals(WasiEventType.FD_WRITE, WasiEventType.fromValue(2), "Should return FD_WRITE");
    }

    @Test
    @DisplayName("fromValue should throw for invalid value")
    void fromValueShouldThrowForInvalidValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiEventType.fromValue(3),
          "Should throw for value 3");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiEventType.fromValue(-1),
          "Should throw for negative value");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiEventType.fromValue(100),
          "Should throw for value 100");
    }

    @Test
    @DisplayName("Round trip getValue/fromValue should work")
    void roundTripShouldWork() {
      for (WasiEventType eventType : WasiEventType.values()) {
        assertEquals(
            eventType,
            WasiEventType.fromValue(eventType.getValue()),
            "Round trip should return same event type: " + eventType.name());
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      final WasiEventType eventType = WasiEventType.FD_READ;

      final String result;
      switch (eventType) {
        case CLOCK:
          result = "clock";
          break;
        case FD_READ:
          result = "read";
          break;
        case FD_WRITE:
          result = "write";
          break;
        default:
          result = "unknown";
          break;
      }

      assertEquals("read", result, "Switch should work correctly");
    }

    @Test
    @DisplayName("All values should have non-null names")
    void allValuesShouldHaveNonNullNames() {
      for (WasiEventType eventType : WasiEventType.values()) {
        assertNotNull(eventType.name(), "Name should not be null: " + eventType.ordinal());
        assertFalse(eventType.name().isEmpty(), "Name should not be empty: " + eventType.ordinal());
      }
    }

    @Test
    @DisplayName("Values should be sequential from 0")
    void valuesShouldBeSequentialFromZero() {
      for (int i = 0; i < WasiEventType.values().length; i++) {
        assertNotNull(WasiEventType.fromValue(i), "Should have value for: " + i);
      }
    }
  }
}

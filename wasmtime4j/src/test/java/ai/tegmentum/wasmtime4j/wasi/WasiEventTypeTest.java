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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiEventType} enum.
 *
 * <p>WasiEventType defines the types of events for WASI poll operations.
 */
@DisplayName("WasiEventType Enum Tests")
class WasiEventTypeTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have all expected event types")
    void shouldHaveAllExpectedEventTypes() {
      assertNotNull(WasiEventType.CLOCK, "Should have CLOCK");
      assertNotNull(WasiEventType.FD_READ, "Should have FD_READ");
      assertNotNull(WasiEventType.FD_WRITE, "Should have FD_WRITE");
    }

    @Test
    @DisplayName("should have exactly 3 event types")
    void shouldHaveExactlyThreeEventTypes() {
      final WasiEventType[] values = WasiEventType.values();
      assertEquals(3, values.length, "Should have exactly 3 event types");
    }
  }

  @Nested
  @DisplayName("Numeric Value Tests")
  class NumericValueTests {

    @Test
    @DisplayName("CLOCK should have value 0")
    void clockShouldHaveValueZero() {
      assertEquals(0, WasiEventType.CLOCK.getValue());
    }

    @Test
    @DisplayName("FD_READ should have value 1")
    void fdReadShouldHaveValueOne() {
      assertEquals(1, WasiEventType.FD_READ.getValue());
    }

    @Test
    @DisplayName("FD_WRITE should have value 2")
    void fdWriteShouldHaveValueTwo() {
      assertEquals(2, WasiEventType.FD_WRITE.getValue());
    }
  }

  @Nested
  @DisplayName("FromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("fromValue(0) should return CLOCK")
    void fromValueZeroShouldReturnClock() {
      assertEquals(WasiEventType.CLOCK, WasiEventType.fromValue(0));
    }

    @Test
    @DisplayName("fromValue(1) should return FD_READ")
    void fromValueOneShouldReturnFdRead() {
      assertEquals(WasiEventType.FD_READ, WasiEventType.fromValue(1));
    }

    @Test
    @DisplayName("fromValue(2) should return FD_WRITE")
    void fromValueTwoShouldReturnFdWrite() {
      assertEquals(WasiEventType.FD_WRITE, WasiEventType.fromValue(2));
    }

    @Test
    @DisplayName("fromValue with invalid value should throw IllegalArgumentException")
    void fromValueWithInvalidValueShouldThrowIllegalArgumentException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiEventType.fromValue(-1),
          "Should throw for negative value");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiEventType.fromValue(3),
          "Should throw for value >= 3");
    }
  }

  @Nested
  @DisplayName("Round Trip Tests")
  class RoundTripTests {

    @Test
    @DisplayName("getValue and fromValue should round-trip correctly")
    void getValueAndFromValueShouldRoundTripCorrectly() {
      for (final WasiEventType eventType : WasiEventType.values()) {
        final int value = eventType.getValue();
        final WasiEventType fromValue = WasiEventType.fromValue(value);
        assertEquals(
            eventType, fromValue, "Round trip should return same enum value for " + eventType);
      }
    }
  }
}

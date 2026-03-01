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
 * Tests for {@link WasiClockId} enum.
 *
 * <p>WasiClockId defines the different clock types available in WASI.
 */
@DisplayName("WasiClockId Enum Tests")
class WasiClockIdTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have all expected clock IDs")
    void shouldHaveAllExpectedClockIds() {
      assertNotNull(WasiClockId.REALTIME, "Should have REALTIME");
      assertNotNull(WasiClockId.MONOTONIC, "Should have MONOTONIC");
      assertNotNull(WasiClockId.PROCESS_CPUTIME_ID, "Should have PROCESS_CPUTIME_ID");
      assertNotNull(WasiClockId.THREAD_CPUTIME_ID, "Should have THREAD_CPUTIME_ID");
    }

    @Test
    @DisplayName("should have exactly 4 clock IDs")
    void shouldHaveExactlyFourClockIds() {
      final WasiClockId[] values = WasiClockId.values();
      assertEquals(4, values.length, "Should have exactly 4 clock IDs");
    }
  }

  @Nested
  @DisplayName("Numeric Value Tests")
  class NumericValueTests {

    @Test
    @DisplayName("REALTIME should have value 0")
    void realtimeShouldHaveValueZero() {
      assertEquals(0, WasiClockId.REALTIME.getValue());
    }

    @Test
    @DisplayName("MONOTONIC should have value 1")
    void monotonicShouldHaveValueOne() {
      assertEquals(1, WasiClockId.MONOTONIC.getValue());
    }

    @Test
    @DisplayName("PROCESS_CPUTIME_ID should have value 2")
    void processCputimeIdShouldHaveValueTwo() {
      assertEquals(2, WasiClockId.PROCESS_CPUTIME_ID.getValue());
    }

    @Test
    @DisplayName("THREAD_CPUTIME_ID should have value 3")
    void threadCputimeIdShouldHaveValueThree() {
      assertEquals(3, WasiClockId.THREAD_CPUTIME_ID.getValue());
    }
  }

  @Nested
  @DisplayName("FromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("fromValue(0) should return REALTIME")
    void fromValueZeroShouldReturnRealtime() {
      assertEquals(WasiClockId.REALTIME, WasiClockId.fromValue(0));
    }

    @Test
    @DisplayName("fromValue(1) should return MONOTONIC")
    void fromValueOneShouldReturnMonotonic() {
      assertEquals(WasiClockId.MONOTONIC, WasiClockId.fromValue(1));
    }

    @Test
    @DisplayName("fromValue(2) should return PROCESS_CPUTIME_ID")
    void fromValueTwoShouldReturnProcessCputimeId() {
      assertEquals(WasiClockId.PROCESS_CPUTIME_ID, WasiClockId.fromValue(2));
    }

    @Test
    @DisplayName("fromValue(3) should return THREAD_CPUTIME_ID")
    void fromValueThreeShouldReturnThreadCputimeId() {
      assertEquals(WasiClockId.THREAD_CPUTIME_ID, WasiClockId.fromValue(3));
    }

    @Test
    @DisplayName("fromValue with invalid value should throw IllegalArgumentException")
    void fromValueWithInvalidValueShouldThrowIllegalArgumentException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiClockId.fromValue(-1),
          "Should throw for negative value");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiClockId.fromValue(4),
          "Should throw for value >= 4");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiClockId.fromValue(100),
          "Should throw for large value");
    }
  }

  @Nested
  @DisplayName("Round Trip Tests")
  class RoundTripTests {

    @Test
    @DisplayName("getValue and fromValue should round-trip correctly")
    void getValueAndFromValueShouldRoundTripCorrectly() {
      for (final WasiClockId clockId : WasiClockId.values()) {
        final int value = clockId.getValue();
        final WasiClockId fromValue = WasiClockId.fromValue(value);
        assertEquals(clockId, fromValue, "Round trip should return same enum value for " + clockId);
      }
    }
  }
}

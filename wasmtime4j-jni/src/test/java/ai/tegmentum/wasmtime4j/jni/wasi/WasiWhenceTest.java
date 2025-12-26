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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiWhence} enum.
 *
 * <p>WasiWhence defines seek whence positions for file operations.
 */
@DisplayName("WasiWhence Enum Tests")
class WasiWhenceTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have all expected whence values")
    void shouldHaveAllExpectedWhenceValues() {
      assertNotNull(WasiWhence.SET, "Should have SET");
      assertNotNull(WasiWhence.CUR, "Should have CUR");
      assertNotNull(WasiWhence.END, "Should have END");
    }

    @Test
    @DisplayName("should have exactly 3 whence values")
    void shouldHaveExactlyThreeWhenceValues() {
      final WasiWhence[] values = WasiWhence.values();
      assertEquals(3, values.length, "Should have exactly 3 whence values");
    }
  }

  @Nested
  @DisplayName("Numeric Value Tests")
  class NumericValueTests {

    @Test
    @DisplayName("SET should have value 0")
    void setShouldHaveValueZero() {
      assertEquals(0, WasiWhence.SET.getValue());
    }

    @Test
    @DisplayName("CUR should have value 1")
    void curShouldHaveValueOne() {
      assertEquals(1, WasiWhence.CUR.getValue());
    }

    @Test
    @DisplayName("END should have value 2")
    void endShouldHaveValueTwo() {
      assertEquals(2, WasiWhence.END.getValue());
    }
  }

  @Nested
  @DisplayName("FromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("fromValue(0) should return SET")
    void fromValueZeroShouldReturnSet() {
      assertEquals(WasiWhence.SET, WasiWhence.fromValue(0));
    }

    @Test
    @DisplayName("fromValue(1) should return CUR")
    void fromValueOneShouldReturnCur() {
      assertEquals(WasiWhence.CUR, WasiWhence.fromValue(1));
    }

    @Test
    @DisplayName("fromValue(2) should return END")
    void fromValueTwoShouldReturnEnd() {
      assertEquals(WasiWhence.END, WasiWhence.fromValue(2));
    }

    @Test
    @DisplayName("fromValue with invalid value should throw IllegalArgumentException")
    void fromValueWithInvalidValueShouldThrowIllegalArgumentException() {
      assertThrows(IllegalArgumentException.class, () -> WasiWhence.fromValue(-1),
          "Should throw for negative value");
      assertThrows(IllegalArgumentException.class, () -> WasiWhence.fromValue(3),
          "Should throw for value >= 3");
    }
  }

  @Nested
  @DisplayName("Round Trip Tests")
  class RoundTripTests {

    @Test
    @DisplayName("getValue and fromValue should round-trip correctly")
    void getValueAndFromValueShouldRoundTripCorrectly() {
      for (final WasiWhence whence : WasiWhence.values()) {
        final int value = whence.getValue();
        final WasiWhence fromValue = WasiWhence.fromValue(value);
        assertEquals(whence, fromValue,
            "Round trip should return same enum value for " + whence);
      }
    }
  }
}

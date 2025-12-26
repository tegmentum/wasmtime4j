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
 * Tests for the {@link WasiWhence} enum.
 *
 * <p>This test class verifies WasiWhence enum values and methods.
 */
@DisplayName("WasiWhence Tests")
class WasiWhenceTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasiWhence should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiWhence.class.isEnum(), "WasiWhence should be an enum");
    }

    @Test
    @DisplayName("Should have expected number of values")
    void shouldHaveExpectedNumberOfValues() {
      assertEquals(3, WasiWhence.values().length, "Should have 3 whence values");
    }

    @Test
    @DisplayName("All enum values should have unique names")
    void allEnumValuesShouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (WasiWhence whence : WasiWhence.values()) {
        assertTrue(names.add(whence.name()), "Name should be unique: " + whence.name());
      }
    }

    @Test
    @DisplayName("All enum values should have unique numeric values")
    void allEnumValuesShouldHaveUniqueValues() {
      final Set<Integer> values = new HashSet<>();
      for (WasiWhence whence : WasiWhence.values()) {
        assertTrue(values.add(whence.getValue()), "Value should be unique: " + whence.name());
      }
    }
  }

  @Nested
  @DisplayName("Value Tests")
  class ValueTests {

    @Test
    @DisplayName("SET should have value 0")
    void setShouldHaveValueZero() {
      assertEquals(0, WasiWhence.SET.getValue(), "SET should have value 0");
    }

    @Test
    @DisplayName("CUR should have value 1")
    void curShouldHaveValue1() {
      assertEquals(1, WasiWhence.CUR.getValue(), "CUR should have value 1");
    }

    @Test
    @DisplayName("END should have value 2")
    void endShouldHaveValue2() {
      assertEquals(2, WasiWhence.END.getValue(), "END should have value 2");
    }
  }

  @Nested
  @DisplayName("fromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("fromValue should return correct whence for valid values")
    void fromValueShouldReturnCorrectWhence() {
      assertEquals(WasiWhence.SET, WasiWhence.fromValue(0), "Should return SET");
      assertEquals(WasiWhence.CUR, WasiWhence.fromValue(1), "Should return CUR");
      assertEquals(WasiWhence.END, WasiWhence.fromValue(2), "Should return END");
    }

    @Test
    @DisplayName("fromValue should throw for invalid value")
    void fromValueShouldThrowForInvalidValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiWhence.fromValue(3),
          "Should throw for value 3");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiWhence.fromValue(-1),
          "Should throw for negative value");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiWhence.fromValue(100),
          "Should throw for value 100");
    }

    @Test
    @DisplayName("Round trip getValue/fromValue should work")
    void roundTripShouldWork() {
      for (WasiWhence whence : WasiWhence.values()) {
        assertEquals(
            whence,
            WasiWhence.fromValue(whence.getValue()),
            "Round trip should return same whence: " + whence.name());
      }
    }
  }

  @Nested
  @DisplayName("Semantic Tests")
  class SemanticTests {

    @Test
    @DisplayName("SET represents seeking from start of file")
    void setShouldRepresentSeekFromStart() {
      // SET corresponds to SEEK_SET in POSIX - seeking from start of file
      assertEquals("SET", WasiWhence.SET.name(), "SET should seek from start of file");
      assertEquals(0, WasiWhence.SET.getValue(), "SET should have value 0 (SEEK_SET)");
    }

    @Test
    @DisplayName("CUR represents seeking from current position")
    void curShouldRepresentSeekFromCurrent() {
      // CUR corresponds to SEEK_CUR in POSIX - seeking from current position
      assertEquals("CUR", WasiWhence.CUR.name(), "CUR should seek from current position");
      assertEquals(1, WasiWhence.CUR.getValue(), "CUR should have value 1 (SEEK_CUR)");
    }

    @Test
    @DisplayName("END represents seeking from end of file")
    void endShouldRepresentSeekFromEnd() {
      // END corresponds to SEEK_END in POSIX - seeking from end of file
      assertEquals("END", WasiWhence.END.name(), "END should seek from end of file");
      assertEquals(2, WasiWhence.END.getValue(), "END should have value 2 (SEEK_END)");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      final WasiWhence whence = WasiWhence.CUR;

      final String result;
      switch (whence) {
        case SET:
          result = "start";
          break;
        case CUR:
          result = "current";
          break;
        case END:
          result = "end";
          break;
        default:
          result = "unknown";
          break;
      }

      assertEquals("current", result, "Switch should work correctly");
    }

    @Test
    @DisplayName("All values should have non-null names")
    void allValuesShouldHaveNonNullNames() {
      for (WasiWhence whence : WasiWhence.values()) {
        assertNotNull(whence.name(), "Name should not be null: " + whence.ordinal());
        assertFalse(whence.name().isEmpty(), "Name should not be empty: " + whence.ordinal());
      }
    }

    @Test
    @DisplayName("Values should be sequential from 0")
    void valuesShouldBeSequentialFromZero() {
      for (int i = 0; i < WasiWhence.values().length; i++) {
        assertNotNull(WasiWhence.fromValue(i), "Should have value for: " + i);
      }
    }
  }
}

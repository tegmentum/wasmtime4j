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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiFileType} enum.
 *
 * <p>WasiFileType defines the different file types in WASI file system.
 */
@DisplayName("WasiFileType Enum Tests")
class WasiFileTypeTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have all expected file types")
    void shouldHaveAllExpectedFileTypes() {
      assertNotNull(WasiFileType.UNKNOWN, "Should have UNKNOWN");
      assertNotNull(WasiFileType.BLOCK_DEVICE, "Should have BLOCK_DEVICE");
      assertNotNull(WasiFileType.CHARACTER_DEVICE, "Should have CHARACTER_DEVICE");
      assertNotNull(WasiFileType.DIRECTORY, "Should have DIRECTORY");
      assertNotNull(WasiFileType.REGULAR_FILE, "Should have REGULAR_FILE");
      assertNotNull(WasiFileType.SOCKET_DGRAM, "Should have SOCKET_DGRAM");
      assertNotNull(WasiFileType.SOCKET_STREAM, "Should have SOCKET_STREAM");
      assertNotNull(WasiFileType.SYMBOLIC_LINK, "Should have SYMBOLIC_LINK");
    }

    @Test
    @DisplayName("should have exactly 8 file types")
    void shouldHaveExactlyEightFileTypes() {
      final WasiFileType[] values = WasiFileType.values();
      assertEquals(8, values.length, "Should have exactly 8 file types");
    }
  }

  @Nested
  @DisplayName("Numeric Value Tests")
  class NumericValueTests {

    @Test
    @DisplayName("UNKNOWN should have value 0")
    void unknownShouldHaveValueZero() {
      assertEquals(0, WasiFileType.UNKNOWN.getValue());
    }

    @Test
    @DisplayName("BLOCK_DEVICE should have value 1")
    void blockDeviceShouldHaveValueOne() {
      assertEquals(1, WasiFileType.BLOCK_DEVICE.getValue());
    }

    @Test
    @DisplayName("CHARACTER_DEVICE should have value 2")
    void characterDeviceShouldHaveValueTwo() {
      assertEquals(2, WasiFileType.CHARACTER_DEVICE.getValue());
    }

    @Test
    @DisplayName("DIRECTORY should have value 3")
    void directoryShouldHaveValueThree() {
      assertEquals(3, WasiFileType.DIRECTORY.getValue());
    }

    @Test
    @DisplayName("REGULAR_FILE should have value 4")
    void regularFileShouldHaveValueFour() {
      assertEquals(4, WasiFileType.REGULAR_FILE.getValue());
    }

    @Test
    @DisplayName("SOCKET_DGRAM should have value 5")
    void socketDgramShouldHaveValueFive() {
      assertEquals(5, WasiFileType.SOCKET_DGRAM.getValue());
    }

    @Test
    @DisplayName("SOCKET_STREAM should have value 6")
    void socketStreamShouldHaveValueSix() {
      assertEquals(6, WasiFileType.SOCKET_STREAM.getValue());
    }

    @Test
    @DisplayName("SYMBOLIC_LINK should have value 7")
    void symbolicLinkShouldHaveValueSeven() {
      assertEquals(7, WasiFileType.SYMBOLIC_LINK.getValue());
    }
  }

  @Nested
  @DisplayName("FromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("fromValue should return correct file type for each value")
    void fromValueShouldReturnCorrectFileType() {
      assertEquals(WasiFileType.UNKNOWN, WasiFileType.fromValue(0));
      assertEquals(WasiFileType.BLOCK_DEVICE, WasiFileType.fromValue(1));
      assertEquals(WasiFileType.CHARACTER_DEVICE, WasiFileType.fromValue(2));
      assertEquals(WasiFileType.DIRECTORY, WasiFileType.fromValue(3));
      assertEquals(WasiFileType.REGULAR_FILE, WasiFileType.fromValue(4));
      assertEquals(WasiFileType.SOCKET_DGRAM, WasiFileType.fromValue(5));
      assertEquals(WasiFileType.SOCKET_STREAM, WasiFileType.fromValue(6));
      assertEquals(WasiFileType.SYMBOLIC_LINK, WasiFileType.fromValue(7));
    }

    @Test
    @DisplayName("fromValue with invalid value should throw IllegalArgumentException")
    void fromValueWithInvalidValueShouldThrowIllegalArgumentException() {
      assertThrows(IllegalArgumentException.class, () -> WasiFileType.fromValue(-1),
          "Should throw for negative value");
      assertThrows(IllegalArgumentException.class, () -> WasiFileType.fromValue(8),
          "Should throw for value >= 8");
    }
  }

  @Nested
  @DisplayName("Type Check Method Tests")
  class TypeCheckMethodTests {

    @Test
    @DisplayName("isRegularFile should return true only for REGULAR_FILE")
    void isRegularFileShouldReturnTrueOnlyForRegularFile() {
      assertTrue(WasiFileType.REGULAR_FILE.isRegularFile());
      assertFalse(WasiFileType.DIRECTORY.isRegularFile());
      assertFalse(WasiFileType.SYMBOLIC_LINK.isRegularFile());
      assertFalse(WasiFileType.UNKNOWN.isRegularFile());
    }

    @Test
    @DisplayName("isDirectory should return true only for DIRECTORY")
    void isDirectoryShouldReturnTrueOnlyForDirectory() {
      assertTrue(WasiFileType.DIRECTORY.isDirectory());
      assertFalse(WasiFileType.REGULAR_FILE.isDirectory());
      assertFalse(WasiFileType.SYMBOLIC_LINK.isDirectory());
      assertFalse(WasiFileType.UNKNOWN.isDirectory());
    }

    @Test
    @DisplayName("isSymbolicLink should return true only for SYMBOLIC_LINK")
    void isSymbolicLinkShouldReturnTrueOnlyForSymbolicLink() {
      assertTrue(WasiFileType.SYMBOLIC_LINK.isSymbolicLink());
      assertFalse(WasiFileType.REGULAR_FILE.isSymbolicLink());
      assertFalse(WasiFileType.DIRECTORY.isSymbolicLink());
      assertFalse(WasiFileType.UNKNOWN.isSymbolicLink());
    }
  }

  @Nested
  @DisplayName("Round Trip Tests")
  class RoundTripTests {

    @Test
    @DisplayName("getValue and fromValue should round-trip correctly")
    void getValueAndFromValueShouldRoundTripCorrectly() {
      for (final WasiFileType fileType : WasiFileType.values()) {
        final int value = fileType.getValue();
        final WasiFileType fromValue = WasiFileType.fromValue(value);
        assertEquals(fileType, fromValue,
            "Round trip should return same enum value for " + fileType);
      }
    }
  }
}

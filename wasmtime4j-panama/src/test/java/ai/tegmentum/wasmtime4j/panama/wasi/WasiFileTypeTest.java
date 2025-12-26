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
 * Tests for the {@link WasiFileType} enum.
 *
 * <p>This test class verifies WasiFileType enum values and methods.
 */
@DisplayName("WasiFileType Tests")
class WasiFileTypeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasiFileType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiFileType.class.isEnum(), "WasiFileType should be an enum");
    }

    @Test
    @DisplayName("Should have expected number of values")
    void shouldHaveExpectedNumberOfValues() {
      assertEquals(8, WasiFileType.values().length, "Should have 8 file types");
    }

    @Test
    @DisplayName("All enum values should have unique names")
    void allEnumValuesShouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (WasiFileType type : WasiFileType.values()) {
        assertTrue(names.add(type.name()), "Name should be unique: " + type.name());
      }
    }

    @Test
    @DisplayName("All enum values should have unique numeric values")
    void allEnumValuesShouldHaveUniqueValues() {
      final Set<Integer> values = new HashSet<>();
      for (WasiFileType type : WasiFileType.values()) {
        assertTrue(values.add(type.getValue()), "Value should be unique: " + type.name());
      }
    }
  }

  @Nested
  @DisplayName("Value Tests")
  class ValueTests {

    @Test
    @DisplayName("UNKNOWN should have value 0")
    void unknownShouldHaveValueZero() {
      assertEquals(0, WasiFileType.UNKNOWN.getValue(), "UNKNOWN should have value 0");
    }

    @Test
    @DisplayName("BLOCK_DEVICE should have value 1")
    void blockDeviceShouldHaveValue1() {
      assertEquals(1, WasiFileType.BLOCK_DEVICE.getValue(), "BLOCK_DEVICE should have value 1");
    }

    @Test
    @DisplayName("CHARACTER_DEVICE should have value 2")
    void characterDeviceShouldHaveValue2() {
      assertEquals(2, WasiFileType.CHARACTER_DEVICE.getValue(),
          "CHARACTER_DEVICE should have value 2");
    }

    @Test
    @DisplayName("DIRECTORY should have value 3")
    void directoryShouldHaveValue3() {
      assertEquals(3, WasiFileType.DIRECTORY.getValue(), "DIRECTORY should have value 3");
    }

    @Test
    @DisplayName("REGULAR_FILE should have value 4")
    void regularFileShouldHaveValue4() {
      assertEquals(4, WasiFileType.REGULAR_FILE.getValue(), "REGULAR_FILE should have value 4");
    }

    @Test
    @DisplayName("SOCKET_DGRAM should have value 5")
    void socketDgramShouldHaveValue5() {
      assertEquals(5, WasiFileType.SOCKET_DGRAM.getValue(), "SOCKET_DGRAM should have value 5");
    }

    @Test
    @DisplayName("SOCKET_STREAM should have value 6")
    void socketStreamShouldHaveValue6() {
      assertEquals(6, WasiFileType.SOCKET_STREAM.getValue(), "SOCKET_STREAM should have value 6");
    }

    @Test
    @DisplayName("SYMBOLIC_LINK should have value 7")
    void symbolicLinkShouldHaveValue7() {
      assertEquals(7, WasiFileType.SYMBOLIC_LINK.getValue(), "SYMBOLIC_LINK should have value 7");
    }
  }

  @Nested
  @DisplayName("fromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("fromValue should return correct file type for valid values")
    void fromValueShouldReturnCorrectFileType() {
      assertEquals(WasiFileType.UNKNOWN, WasiFileType.fromValue(0), "Should return UNKNOWN");
      assertEquals(WasiFileType.BLOCK_DEVICE, WasiFileType.fromValue(1),
          "Should return BLOCK_DEVICE");
      assertEquals(WasiFileType.CHARACTER_DEVICE, WasiFileType.fromValue(2),
          "Should return CHARACTER_DEVICE");
      assertEquals(WasiFileType.DIRECTORY, WasiFileType.fromValue(3), "Should return DIRECTORY");
      assertEquals(WasiFileType.REGULAR_FILE, WasiFileType.fromValue(4),
          "Should return REGULAR_FILE");
      assertEquals(WasiFileType.SOCKET_DGRAM, WasiFileType.fromValue(5),
          "Should return SOCKET_DGRAM");
      assertEquals(WasiFileType.SOCKET_STREAM, WasiFileType.fromValue(6),
          "Should return SOCKET_STREAM");
      assertEquals(WasiFileType.SYMBOLIC_LINK, WasiFileType.fromValue(7),
          "Should return SYMBOLIC_LINK");
    }

    @Test
    @DisplayName("fromValue should throw for invalid value")
    void fromValueShouldThrowForInvalidValue() {
      assertThrows(IllegalArgumentException.class, () -> WasiFileType.fromValue(8),
          "Should throw for value 8");
      assertThrows(IllegalArgumentException.class, () -> WasiFileType.fromValue(-1),
          "Should throw for negative value");
      assertThrows(IllegalArgumentException.class, () -> WasiFileType.fromValue(100),
          "Should throw for value 100");
    }

    @Test
    @DisplayName("Round trip getValue/fromValue should work")
    void roundTripShouldWork() {
      for (WasiFileType type : WasiFileType.values()) {
        assertEquals(type, WasiFileType.fromValue(type.getValue()),
            "Round trip should return same type: " + type.name());
      }
    }
  }

  @Nested
  @DisplayName("Type Check Methods Tests")
  class TypeCheckMethodsTests {

    @Test
    @DisplayName("isRegularFile should return true only for REGULAR_FILE")
    void isRegularFileShouldReturnTrueOnlyForRegularFile() {
      assertTrue(WasiFileType.REGULAR_FILE.isRegularFile(),
          "REGULAR_FILE.isRegularFile() should be true");
      assertFalse(WasiFileType.DIRECTORY.isRegularFile(),
          "DIRECTORY.isRegularFile() should be false");
      assertFalse(WasiFileType.SYMBOLIC_LINK.isRegularFile(),
          "SYMBOLIC_LINK.isRegularFile() should be false");
      assertFalse(WasiFileType.UNKNOWN.isRegularFile(),
          "UNKNOWN.isRegularFile() should be false");
    }

    @Test
    @DisplayName("isDirectory should return true only for DIRECTORY")
    void isDirectoryShouldReturnTrueOnlyForDirectory() {
      assertTrue(WasiFileType.DIRECTORY.isDirectory(), "DIRECTORY.isDirectory() should be true");
      assertFalse(WasiFileType.REGULAR_FILE.isDirectory(),
          "REGULAR_FILE.isDirectory() should be false");
      assertFalse(WasiFileType.SYMBOLIC_LINK.isDirectory(),
          "SYMBOLIC_LINK.isDirectory() should be false");
      assertFalse(WasiFileType.UNKNOWN.isDirectory(), "UNKNOWN.isDirectory() should be false");
    }

    @Test
    @DisplayName("isSymbolicLink should return true only for SYMBOLIC_LINK")
    void isSymbolicLinkShouldReturnTrueOnlyForSymbolicLink() {
      assertTrue(WasiFileType.SYMBOLIC_LINK.isSymbolicLink(),
          "SYMBOLIC_LINK.isSymbolicLink() should be true");
      assertFalse(WasiFileType.REGULAR_FILE.isSymbolicLink(),
          "REGULAR_FILE.isSymbolicLink() should be false");
      assertFalse(WasiFileType.DIRECTORY.isSymbolicLink(),
          "DIRECTORY.isSymbolicLink() should be false");
      assertFalse(WasiFileType.UNKNOWN.isSymbolicLink(),
          "UNKNOWN.isSymbolicLink() should be false");
    }

    @Test
    @DisplayName("All type check methods should be mutually exclusive")
    void typeCheckMethodsShouldBeMutuallyExclusive() {
      for (WasiFileType type : WasiFileType.values()) {
        int trueCount = 0;
        if (type.isRegularFile()) {
          trueCount++;
        }
        if (type.isDirectory()) {
          trueCount++;
        }
        if (type.isSymbolicLink()) {
          trueCount++;
        }
        assertTrue(trueCount <= 1, "At most one type check should be true for: " + type.name());
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      final WasiFileType type = WasiFileType.REGULAR_FILE;

      final String result;
      switch (type) {
        case REGULAR_FILE:
          result = "file";
          break;
        case DIRECTORY:
          result = "directory";
          break;
        case SYMBOLIC_LINK:
          result = "symlink";
          break;
        default:
          result = "other";
          break;
      }

      assertEquals("file", result, "Switch should work correctly");
    }

    @Test
    @DisplayName("All values should have non-null names")
    void allValuesShouldHaveNonNullNames() {
      for (WasiFileType type : WasiFileType.values()) {
        assertNotNull(type.name(), "Name should not be null: " + type.ordinal());
        assertFalse(type.name().isEmpty(), "Name should not be empty: " + type.ordinal());
      }
    }
  }
}

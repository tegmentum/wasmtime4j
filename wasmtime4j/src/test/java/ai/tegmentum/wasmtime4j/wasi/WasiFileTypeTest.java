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
 * Tests for the {@link WasiFileType} enum.
 *
 * <p>Verifies file type values, integer mappings, boolean classification methods, and fromValue
 * conversion.
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
    @DisplayName("WasiFileType should have exactly 8 values")
    void shouldHaveExactlyEightValues() {
      assertEquals(8, WasiFileType.values().length, "Should have exactly 8 file type values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(WasiFileType.UNKNOWN, "UNKNOWN should exist");
      assertEquals("UNKNOWN", WasiFileType.UNKNOWN.name(), "Name should be UNKNOWN");
    }

    @Test
    @DisplayName("should have BLOCK_DEVICE value")
    void shouldHaveBlockDeviceValue() {
      assertNotNull(WasiFileType.BLOCK_DEVICE, "BLOCK_DEVICE should exist");
      assertEquals("BLOCK_DEVICE", WasiFileType.BLOCK_DEVICE.name(), "Name should be BLOCK_DEVICE");
    }

    @Test
    @DisplayName("should have CHARACTER_DEVICE value")
    void shouldHaveCharacterDeviceValue() {
      assertNotNull(WasiFileType.CHARACTER_DEVICE, "CHARACTER_DEVICE should exist");
      assertEquals(
          "CHARACTER_DEVICE",
          WasiFileType.CHARACTER_DEVICE.name(),
          "Name should be CHARACTER_DEVICE");
    }

    @Test
    @DisplayName("should have DIRECTORY value")
    void shouldHaveDirectoryValue() {
      assertNotNull(WasiFileType.DIRECTORY, "DIRECTORY should exist");
      assertEquals("DIRECTORY", WasiFileType.DIRECTORY.name(), "Name should be DIRECTORY");
    }

    @Test
    @DisplayName("should have REGULAR_FILE value")
    void shouldHaveRegularFileValue() {
      assertNotNull(WasiFileType.REGULAR_FILE, "REGULAR_FILE should exist");
      assertEquals("REGULAR_FILE", WasiFileType.REGULAR_FILE.name(), "Name should be REGULAR_FILE");
    }

    @Test
    @DisplayName("should have SOCKET_STREAM value")
    void shouldHaveSocketStreamValue() {
      assertNotNull(WasiFileType.SOCKET_STREAM, "SOCKET_STREAM should exist");
      assertEquals(
          "SOCKET_STREAM", WasiFileType.SOCKET_STREAM.name(), "Name should be SOCKET_STREAM");
    }

    @Test
    @DisplayName("should have SOCKET_DGRAM value")
    void shouldHaveSocketDgramValue() {
      assertNotNull(WasiFileType.SOCKET_DGRAM, "SOCKET_DGRAM should exist");
      assertEquals("SOCKET_DGRAM", WasiFileType.SOCKET_DGRAM.name(), "Name should be SOCKET_DGRAM");
    }

    @Test
    @DisplayName("should have SYMBOLIC_LINK value")
    void shouldHaveSymbolicLinkValue() {
      assertNotNull(WasiFileType.SYMBOLIC_LINK, "SYMBOLIC_LINK should exist");
      assertEquals(
          "SYMBOLIC_LINK", WasiFileType.SYMBOLIC_LINK.name(), "Name should be SYMBOLIC_LINK");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("ordinals should be unique")
    void ordinalsShouldBeUnique() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final WasiFileType type : WasiFileType.values()) {
        assertTrue(ordinals.add(type.ordinal()), "Ordinal should be unique: " + type.ordinal());
      }
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final WasiFileType[] values = WasiFileType.values();
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
      assertEquals(WasiFileType.UNKNOWN, WasiFileType.valueOf("UNKNOWN"), "Should return UNKNOWN");
      assertEquals(
          WasiFileType.BLOCK_DEVICE,
          WasiFileType.valueOf("BLOCK_DEVICE"),
          "Should return BLOCK_DEVICE");
      assertEquals(
          WasiFileType.CHARACTER_DEVICE,
          WasiFileType.valueOf("CHARACTER_DEVICE"),
          "Should return CHARACTER_DEVICE");
      assertEquals(
          WasiFileType.DIRECTORY, WasiFileType.valueOf("DIRECTORY"), "Should return DIRECTORY");
      assertEquals(
          WasiFileType.REGULAR_FILE,
          WasiFileType.valueOf("REGULAR_FILE"),
          "Should return REGULAR_FILE");
      assertEquals(
          WasiFileType.SOCKET_STREAM,
          WasiFileType.valueOf("SOCKET_STREAM"),
          "Should return SOCKET_STREAM");
      assertEquals(
          WasiFileType.SOCKET_DGRAM,
          WasiFileType.valueOf("SOCKET_DGRAM"),
          "Should return SOCKET_DGRAM");
      assertEquals(
          WasiFileType.SYMBOLIC_LINK,
          WasiFileType.valueOf("SYMBOLIC_LINK"),
          "Should return SYMBOLIC_LINK");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiFileType.valueOf("INVALID"),
          "Should throw for invalid enum name");
    }
  }

  @Nested
  @DisplayName("values() Tests")
  class ValuesTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllEnumConstants() {
      final WasiFileType[] values = WasiFileType.values();
      final Set<WasiFileType> valueSet = new HashSet<>(Arrays.asList(values));

      assertTrue(valueSet.contains(WasiFileType.UNKNOWN), "Should contain UNKNOWN");
      assertTrue(valueSet.contains(WasiFileType.BLOCK_DEVICE), "Should contain BLOCK_DEVICE");
      assertTrue(
          valueSet.contains(WasiFileType.CHARACTER_DEVICE), "Should contain CHARACTER_DEVICE");
      assertTrue(valueSet.contains(WasiFileType.DIRECTORY), "Should contain DIRECTORY");
      assertTrue(valueSet.contains(WasiFileType.REGULAR_FILE), "Should contain REGULAR_FILE");
      assertTrue(valueSet.contains(WasiFileType.SOCKET_STREAM), "Should contain SOCKET_STREAM");
      assertTrue(valueSet.contains(WasiFileType.SOCKET_DGRAM), "Should contain SOCKET_DGRAM");
      assertTrue(valueSet.contains(WasiFileType.SYMBOLIC_LINK), "Should contain SYMBOLIC_LINK");
    }

    @Test
    @DisplayName("values() should return new array each time")
    void valuesShouldReturnNewArrayEachTime() {
      final WasiFileType[] first = WasiFileType.values();
      final WasiFileType[] second = WasiFileType.values();

      assertTrue(first != second, "Should return new array each time");
      assertEquals(first.length, second.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("GetValue Tests")
  class GetValueTests {

    @Test
    @DisplayName("UNKNOWN should have value 0")
    void unknownShouldHaveValueZero() {
      assertEquals(0, WasiFileType.UNKNOWN.getValue(), "UNKNOWN value should be 0");
    }

    @Test
    @DisplayName("BLOCK_DEVICE should have value 1")
    void blockDeviceShouldHaveValueOne() {
      assertEquals(1, WasiFileType.BLOCK_DEVICE.getValue(), "BLOCK_DEVICE value should be 1");
    }

    @Test
    @DisplayName("CHARACTER_DEVICE should have value 2")
    void characterDeviceShouldHaveValueTwo() {
      assertEquals(
          2, WasiFileType.CHARACTER_DEVICE.getValue(), "CHARACTER_DEVICE value should be 2");
    }

    @Test
    @DisplayName("DIRECTORY should have value 3")
    void directoryShouldHaveValueThree() {
      assertEquals(3, WasiFileType.DIRECTORY.getValue(), "DIRECTORY value should be 3");
    }

    @Test
    @DisplayName("REGULAR_FILE should have value 4")
    void regularFileShouldHaveValueFour() {
      assertEquals(4, WasiFileType.REGULAR_FILE.getValue(), "REGULAR_FILE value should be 4");
    }

    @Test
    @DisplayName("SOCKET_STREAM should have value 5")
    void socketStreamShouldHaveValueFive() {
      assertEquals(5, WasiFileType.SOCKET_STREAM.getValue(), "SOCKET_STREAM value should be 5");
    }

    @Test
    @DisplayName("SOCKET_DGRAM should have value 6")
    void socketDgramShouldHaveValueSix() {
      assertEquals(6, WasiFileType.SOCKET_DGRAM.getValue(), "SOCKET_DGRAM value should be 6");
    }

    @Test
    @DisplayName("SYMBOLIC_LINK should have value 7")
    void symbolicLinkShouldHaveValueSeven() {
      assertEquals(7, WasiFileType.SYMBOLIC_LINK.getValue(), "SYMBOLIC_LINK value should be 7");
    }
  }

  @Nested
  @DisplayName("FromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("should resolve valid values to correct constants")
    void shouldResolveValidValues() {
      assertEquals(WasiFileType.UNKNOWN, WasiFileType.fromValue(0), "Value 0 should be UNKNOWN");
      assertEquals(
          WasiFileType.BLOCK_DEVICE, WasiFileType.fromValue(1), "Value 1 should be BLOCK_DEVICE");
      assertEquals(
          WasiFileType.CHARACTER_DEVICE,
          WasiFileType.fromValue(2),
          "Value 2 should be CHARACTER_DEVICE");
      assertEquals(
          WasiFileType.DIRECTORY, WasiFileType.fromValue(3), "Value 3 should be DIRECTORY");
      assertEquals(
          WasiFileType.REGULAR_FILE, WasiFileType.fromValue(4), "Value 4 should be REGULAR_FILE");
      assertEquals(
          WasiFileType.SOCKET_STREAM, WasiFileType.fromValue(5), "Value 5 should be SOCKET_STREAM");
      assertEquals(
          WasiFileType.SOCKET_DGRAM, WasiFileType.fromValue(6), "Value 6 should be SOCKET_DGRAM");
      assertEquals(
          WasiFileType.SYMBOLIC_LINK, WasiFileType.fromValue(7), "Value 7 should be SYMBOLIC_LINK");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid value")
    void shouldThrowForInvalidValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiFileType.fromValue(-1),
          "Should throw for negative value -1");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiFileType.fromValue(8),
          "Should throw for out-of-range value 8");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiFileType.fromValue(100),
          "Should throw for out-of-range value 100");
    }
  }

  @Nested
  @DisplayName("Boolean Method Tests")
  class BooleanMethodTests {

    @Test
    @DisplayName("isRegularFile should return true only for REGULAR_FILE")
    void isRegularFileShouldReturnTrueOnlyForRegularFile() {
      assertTrue(
          WasiFileType.REGULAR_FILE.isRegularFile(), "REGULAR_FILE.isRegularFile() should be true");
      for (final WasiFileType type : WasiFileType.values()) {
        if (type != WasiFileType.REGULAR_FILE) {
          assertFalse(type.isRegularFile(), type + ".isRegularFile() should be false");
        }
      }
    }

    @Test
    @DisplayName("isDirectory should return true only for DIRECTORY")
    void isDirectoryShouldReturnTrueOnlyForDirectory() {
      assertTrue(WasiFileType.DIRECTORY.isDirectory(), "DIRECTORY.isDirectory() should be true");
      for (final WasiFileType type : WasiFileType.values()) {
        if (type != WasiFileType.DIRECTORY) {
          assertFalse(type.isDirectory(), type + ".isDirectory() should be false");
        }
      }
    }

    @Test
    @DisplayName("isSymbolicLink should return true only for SYMBOLIC_LINK")
    void isSymbolicLinkShouldReturnTrueOnlyForSymbolicLink() {
      assertTrue(
          WasiFileType.SYMBOLIC_LINK.isSymbolicLink(),
          "SYMBOLIC_LINK.isSymbolicLink() should be true");
      for (final WasiFileType type : WasiFileType.values()) {
        if (type != WasiFileType.SYMBOLIC_LINK) {
          assertFalse(type.isSymbolicLink(), type + ".isSymbolicLink() should be false");
        }
      }
    }

    @Test
    @DisplayName("isDevice should return true only for BLOCK_DEVICE and CHARACTER_DEVICE")
    void isDeviceShouldReturnTrueOnlyForDeviceTypes() {
      assertTrue(WasiFileType.BLOCK_DEVICE.isDevice(), "BLOCK_DEVICE.isDevice() should be true");
      assertTrue(
          WasiFileType.CHARACTER_DEVICE.isDevice(), "CHARACTER_DEVICE.isDevice() should be true");
      assertFalse(WasiFileType.UNKNOWN.isDevice(), "UNKNOWN.isDevice() should be false");
      assertFalse(WasiFileType.DIRECTORY.isDevice(), "DIRECTORY.isDevice() should be false");
      assertFalse(WasiFileType.REGULAR_FILE.isDevice(), "REGULAR_FILE.isDevice() should be false");
      assertFalse(
          WasiFileType.SOCKET_STREAM.isDevice(), "SOCKET_STREAM.isDevice() should be false");
      assertFalse(WasiFileType.SOCKET_DGRAM.isDevice(), "SOCKET_DGRAM.isDevice() should be false");
      assertFalse(
          WasiFileType.SYMBOLIC_LINK.isDevice(), "SYMBOLIC_LINK.isDevice() should be false");
    }

    @Test
    @DisplayName("isSocket should return true only for SOCKET_STREAM and SOCKET_DGRAM")
    void isSocketShouldReturnTrueOnlyForSocketTypes() {
      assertTrue(WasiFileType.SOCKET_STREAM.isSocket(), "SOCKET_STREAM.isSocket() should be true");
      assertTrue(WasiFileType.SOCKET_DGRAM.isSocket(), "SOCKET_DGRAM.isSocket() should be true");
      assertFalse(WasiFileType.UNKNOWN.isSocket(), "UNKNOWN.isSocket() should be false");
      assertFalse(WasiFileType.BLOCK_DEVICE.isSocket(), "BLOCK_DEVICE.isSocket() should be false");
      assertFalse(
          WasiFileType.CHARACTER_DEVICE.isSocket(), "CHARACTER_DEVICE.isSocket() should be false");
      assertFalse(WasiFileType.DIRECTORY.isSocket(), "DIRECTORY.isSocket() should be false");
      assertFalse(WasiFileType.REGULAR_FILE.isSocket(), "REGULAR_FILE.isSocket() should be false");
      assertFalse(
          WasiFileType.SYMBOLIC_LINK.isSocket(), "SYMBOLIC_LINK.isSocket() should be false");
    }

    @Test
    @DisplayName("isSpecialFile should return true for devices and sockets")
    void isSpecialFileShouldReturnTrueForDevicesAndSockets() {
      assertTrue(
          WasiFileType.BLOCK_DEVICE.isSpecialFile(), "BLOCK_DEVICE.isSpecialFile() should be true");
      assertTrue(
          WasiFileType.CHARACTER_DEVICE.isSpecialFile(),
          "CHARACTER_DEVICE.isSpecialFile() should be true");
      assertTrue(
          WasiFileType.SOCKET_STREAM.isSpecialFile(),
          "SOCKET_STREAM.isSpecialFile() should be true");
      assertTrue(
          WasiFileType.SOCKET_DGRAM.isSpecialFile(), "SOCKET_DGRAM.isSpecialFile() should be true");
      assertFalse(WasiFileType.UNKNOWN.isSpecialFile(), "UNKNOWN.isSpecialFile() should be false");
      assertFalse(
          WasiFileType.DIRECTORY.isSpecialFile(), "DIRECTORY.isSpecialFile() should be false");
      assertFalse(
          WasiFileType.REGULAR_FILE.isSpecialFile(),
          "REGULAR_FILE.isSpecialFile() should be false");
      assertFalse(
          WasiFileType.SYMBOLIC_LINK.isSpecialFile(),
          "SYMBOLIC_LINK.isSpecialFile() should be false");
    }
  }
}

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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link WasiFileType} enum.
 *
 * <p>WasiFileType represents different kinds of filesystem entries in the WASI sandbox.
 */
@DisplayName("WasiFileType Tests")
class WasiFileTypeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      final WasiFileType[] values = WasiFileType.values();
      assertEquals(8, values.length, "WasiFileType should have 8 values");

      assertNotNull(WasiFileType.valueOf("UNKNOWN"), "UNKNOWN should exist");
      assertNotNull(WasiFileType.valueOf("BLOCK_DEVICE"), "BLOCK_DEVICE should exist");
      assertNotNull(WasiFileType.valueOf("CHARACTER_DEVICE"), "CHARACTER_DEVICE should exist");
      assertNotNull(WasiFileType.valueOf("DIRECTORY"), "DIRECTORY should exist");
      assertNotNull(WasiFileType.valueOf("REGULAR_FILE"), "REGULAR_FILE should exist");
      assertNotNull(WasiFileType.valueOf("SOCKET_STREAM"), "SOCKET_STREAM should exist");
      assertNotNull(WasiFileType.valueOf("SOCKET_DGRAM"), "SOCKET_DGRAM should exist");
      assertNotNull(WasiFileType.valueOf("SYMBOLIC_LINK"), "SYMBOLIC_LINK should exist");
    }

    @Test
    @DisplayName("values should be in expected order")
    void valuesShouldBeInExpectedOrder() {
      final WasiFileType[] values = WasiFileType.values();
      assertEquals(WasiFileType.UNKNOWN, values[0], "First should be UNKNOWN");
      assertEquals(WasiFileType.BLOCK_DEVICE, values[1], "Second should be BLOCK_DEVICE");
      assertEquals(WasiFileType.CHARACTER_DEVICE, values[2], "Third should be CHARACTER_DEVICE");
      assertEquals(WasiFileType.DIRECTORY, values[3], "Fourth should be DIRECTORY");
      assertEquals(WasiFileType.REGULAR_FILE, values[4], "Fifth should be REGULAR_FILE");
      assertEquals(WasiFileType.SOCKET_STREAM, values[5], "Sixth should be SOCKET_STREAM");
      assertEquals(WasiFileType.SOCKET_DGRAM, values[6], "Seventh should be SOCKET_DGRAM");
      assertEquals(WasiFileType.SYMBOLIC_LINK, values[7], "Eighth should be SYMBOLIC_LINK");
    }
  }

  @Nested
  @DisplayName("getValue Method Tests")
  class GetValueMethodTests {

    @Test
    @DisplayName("UNKNOWN should have value 0")
    void unknownShouldHaveValue0() {
      assertEquals(0, WasiFileType.UNKNOWN.getValue(), "UNKNOWN value should be 0");
    }

    @Test
    @DisplayName("BLOCK_DEVICE should have value 1")
    void blockDeviceShouldHaveValue1() {
      assertEquals(1, WasiFileType.BLOCK_DEVICE.getValue(), "BLOCK_DEVICE value should be 1");
    }

    @Test
    @DisplayName("CHARACTER_DEVICE should have value 2")
    void characterDeviceShouldHaveValue2() {
      assertEquals(
          2, WasiFileType.CHARACTER_DEVICE.getValue(), "CHARACTER_DEVICE value should be 2");
    }

    @Test
    @DisplayName("DIRECTORY should have value 3")
    void directoryShouldHaveValue3() {
      assertEquals(3, WasiFileType.DIRECTORY.getValue(), "DIRECTORY value should be 3");
    }

    @Test
    @DisplayName("REGULAR_FILE should have value 4")
    void regularFileShouldHaveValue4() {
      assertEquals(4, WasiFileType.REGULAR_FILE.getValue(), "REGULAR_FILE value should be 4");
    }

    @Test
    @DisplayName("SOCKET_STREAM should have value 5")
    void socketStreamShouldHaveValue5() {
      assertEquals(5, WasiFileType.SOCKET_STREAM.getValue(), "SOCKET_STREAM value should be 5");
    }

    @Test
    @DisplayName("SOCKET_DGRAM should have value 6")
    void socketDgramShouldHaveValue6() {
      assertEquals(6, WasiFileType.SOCKET_DGRAM.getValue(), "SOCKET_DGRAM value should be 6");
    }

    @Test
    @DisplayName("SYMBOLIC_LINK should have value 7")
    void symbolicLinkShouldHaveValue7() {
      assertEquals(7, WasiFileType.SYMBOLIC_LINK.getValue(), "SYMBOLIC_LINK value should be 7");
    }
  }

  @Nested
  @DisplayName("fromValue Method Tests")
  class FromValueMethodTests {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    @DisplayName("fromValue should return correct type for valid values")
    void fromValueShouldReturnCorrectTypeForValidValues(final int value) {
      final WasiFileType type = WasiFileType.fromValue(value);
      assertNotNull(type, "Should return a type for value " + value);
      assertEquals(value, type.getValue(), "getValue should return original value");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 8, 100, 999})
    @DisplayName("fromValue should throw for invalid values")
    void fromValueShouldThrowForInvalidValues(final int value) {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiFileType.fromValue(value),
              "Should throw for invalid value " + value);
      assertTrue(
          exception.getMessage().contains(String.valueOf(value)),
          "Exception should contain the invalid value");
    }
  }

  @Nested
  @DisplayName("isRegularFile Method Tests")
  class IsRegularFileMethodTests {

    @Test
    @DisplayName("REGULAR_FILE should be regular file")
    void regularFileShouldBeRegularFile() {
      assertTrue(WasiFileType.REGULAR_FILE.isRegularFile(), "REGULAR_FILE should be regular file");
    }

    @ParameterizedTest
    @EnumSource(
        value = WasiFileType.class,
        names = {"REGULAR_FILE"},
        mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("other types should not be regular file")
    void otherTypesShouldNotBeRegularFile(final WasiFileType type) {
      assertFalse(type.isRegularFile(), type.name() + " should not be regular file");
    }
  }

  @Nested
  @DisplayName("isDirectory Method Tests")
  class IsDirectoryMethodTests {

    @Test
    @DisplayName("DIRECTORY should be directory")
    void directoryShouldBeDirectory() {
      assertTrue(WasiFileType.DIRECTORY.isDirectory(), "DIRECTORY should be directory");
    }

    @ParameterizedTest
    @EnumSource(
        value = WasiFileType.class,
        names = {"DIRECTORY"},
        mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("other types should not be directory")
    void otherTypesShouldNotBeDirectory(final WasiFileType type) {
      assertFalse(type.isDirectory(), type.name() + " should not be directory");
    }
  }

  @Nested
  @DisplayName("isSymbolicLink Method Tests")
  class IsSymbolicLinkMethodTests {

    @Test
    @DisplayName("SYMBOLIC_LINK should be symbolic link")
    void symbolicLinkShouldBeSymbolicLink() {
      assertTrue(
          WasiFileType.SYMBOLIC_LINK.isSymbolicLink(), "SYMBOLIC_LINK should be symbolic link");
    }

    @ParameterizedTest
    @EnumSource(
        value = WasiFileType.class,
        names = {"SYMBOLIC_LINK"},
        mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("other types should not be symbolic link")
    void otherTypesShouldNotBeSymbolicLink(final WasiFileType type) {
      assertFalse(type.isSymbolicLink(), type.name() + " should not be symbolic link");
    }
  }

  @Nested
  @DisplayName("isDevice Method Tests")
  class IsDeviceMethodTests {

    @Test
    @DisplayName("BLOCK_DEVICE should be device")
    void blockDeviceShouldBeDevice() {
      assertTrue(WasiFileType.BLOCK_DEVICE.isDevice(), "BLOCK_DEVICE should be device");
    }

    @Test
    @DisplayName("CHARACTER_DEVICE should be device")
    void characterDeviceShouldBeDevice() {
      assertTrue(WasiFileType.CHARACTER_DEVICE.isDevice(), "CHARACTER_DEVICE should be device");
    }

    @ParameterizedTest
    @EnumSource(
        value = WasiFileType.class,
        names = {"BLOCK_DEVICE", "CHARACTER_DEVICE"},
        mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("non-device types should not be device")
    void nonDeviceTypesShouldNotBeDevice(final WasiFileType type) {
      assertFalse(type.isDevice(), type.name() + " should not be device");
    }
  }

  @Nested
  @DisplayName("isSocket Method Tests")
  class IsSocketMethodTests {

    @Test
    @DisplayName("SOCKET_STREAM should be socket")
    void socketStreamShouldBeSocket() {
      assertTrue(WasiFileType.SOCKET_STREAM.isSocket(), "SOCKET_STREAM should be socket");
    }

    @Test
    @DisplayName("SOCKET_DGRAM should be socket")
    void socketDgramShouldBeSocket() {
      assertTrue(WasiFileType.SOCKET_DGRAM.isSocket(), "SOCKET_DGRAM should be socket");
    }

    @ParameterizedTest
    @EnumSource(
        value = WasiFileType.class,
        names = {"SOCKET_STREAM", "SOCKET_DGRAM"},
        mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("non-socket types should not be socket")
    void nonSocketTypesShouldNotBeSocket(final WasiFileType type) {
      assertFalse(type.isSocket(), type.name() + " should not be socket");
    }
  }

  @Nested
  @DisplayName("isSpecialFile Method Tests")
  class IsSpecialFileMethodTests {

    @ParameterizedTest
    @EnumSource(
        value = WasiFileType.class,
        names = {"BLOCK_DEVICE", "CHARACTER_DEVICE", "SOCKET_STREAM", "SOCKET_DGRAM"})
    @DisplayName("devices and sockets should be special files")
    void devicesAndSocketsShouldBeSpecialFiles(final WasiFileType type) {
      assertTrue(type.isSpecialFile(), type.name() + " should be special file");
    }

    @ParameterizedTest
    @EnumSource(
        value = WasiFileType.class,
        names = {"UNKNOWN", "DIRECTORY", "REGULAR_FILE", "SYMBOLIC_LINK"})
    @DisplayName("non-special types should not be special files")
    void nonSpecialTypesShouldNotBeSpecialFiles(final WasiFileType type) {
      assertFalse(type.isSpecialFile(), type.name() + " should not be special file");
    }
  }
}

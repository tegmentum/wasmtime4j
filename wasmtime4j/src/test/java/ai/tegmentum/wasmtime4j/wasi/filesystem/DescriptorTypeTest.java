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

package ai.tegmentum.wasmtime4j.wasi.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Tests for DescriptorType enum.
 *
 * <p>Verifies enum constants, ordinals, valueOf, values, toString, and switch statement coverage for
 * WASI filesystem descriptor types.
 */
@DisplayName("DescriptorType Tests")
class DescriptorTypeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 8 enum constants")
    void shouldHaveExactlyEightEnumConstants() {
      final DescriptorType[] values = DescriptorType.values();

      assertEquals(8, values.length, "DescriptorType should have exactly 8 constants");
    }

    @Test
    @DisplayName("should be a valid enum type")
    void shouldBeValidEnumType() {
      assertTrue(DescriptorType.class.isEnum(), "DescriptorType should be an enum type");
    }

    @Test
    @DisplayName("all constants should be non-null")
    void allConstantsShouldBeNonNull() {
      for (final DescriptorType type : DescriptorType.values()) {
        assertNotNull(type, "Every DescriptorType constant should be non-null");
      }
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have UNKNOWN constant")
    void shouldHaveUnknownConstant() {
      final DescriptorType type = DescriptorType.UNKNOWN;

      assertNotNull(type, "UNKNOWN should not be null");
      assertEquals("UNKNOWN", type.name(), "Name should be UNKNOWN");
    }

    @Test
    @DisplayName("should have BLOCK_DEVICE constant")
    void shouldHaveBlockDeviceConstant() {
      final DescriptorType type = DescriptorType.BLOCK_DEVICE;

      assertNotNull(type, "BLOCK_DEVICE should not be null");
      assertEquals("BLOCK_DEVICE", type.name(), "Name should be BLOCK_DEVICE");
    }

    @Test
    @DisplayName("should have CHARACTER_DEVICE constant")
    void shouldHaveCharacterDeviceConstant() {
      final DescriptorType type = DescriptorType.CHARACTER_DEVICE;

      assertNotNull(type, "CHARACTER_DEVICE should not be null");
      assertEquals("CHARACTER_DEVICE", type.name(), "Name should be CHARACTER_DEVICE");
    }

    @Test
    @DisplayName("should have DIRECTORY constant")
    void shouldHaveDirectoryConstant() {
      final DescriptorType type = DescriptorType.DIRECTORY;

      assertNotNull(type, "DIRECTORY should not be null");
      assertEquals("DIRECTORY", type.name(), "Name should be DIRECTORY");
    }

    @Test
    @DisplayName("should have FIFO constant")
    void shouldHaveFifoConstant() {
      final DescriptorType type = DescriptorType.FIFO;

      assertNotNull(type, "FIFO should not be null");
      assertEquals("FIFO", type.name(), "Name should be FIFO");
    }

    @Test
    @DisplayName("should have SYMBOLIC_LINK constant")
    void shouldHaveSymbolicLinkConstant() {
      final DescriptorType type = DescriptorType.SYMBOLIC_LINK;

      assertNotNull(type, "SYMBOLIC_LINK should not be null");
      assertEquals("SYMBOLIC_LINK", type.name(), "Name should be SYMBOLIC_LINK");
    }

    @Test
    @DisplayName("should have REGULAR_FILE constant")
    void shouldHaveRegularFileConstant() {
      final DescriptorType type = DescriptorType.REGULAR_FILE;

      assertNotNull(type, "REGULAR_FILE should not be null");
      assertEquals("REGULAR_FILE", type.name(), "Name should be REGULAR_FILE");
    }

    @Test
    @DisplayName("should have SOCKET constant")
    void shouldHaveSocketConstant() {
      final DescriptorType type = DescriptorType.SOCKET;

      assertNotNull(type, "SOCKET should not be null");
      assertEquals("SOCKET", type.name(), "Name should be SOCKET");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("UNKNOWN should have ordinal 0")
    void unknownShouldHaveOrdinalZero() {
      assertEquals(0, DescriptorType.UNKNOWN.ordinal(), "UNKNOWN ordinal should be 0");
    }

    @Test
    @DisplayName("BLOCK_DEVICE should have ordinal 1")
    void blockDeviceShouldHaveOrdinalOne() {
      assertEquals(
          1, DescriptorType.BLOCK_DEVICE.ordinal(), "BLOCK_DEVICE ordinal should be 1");
    }

    @Test
    @DisplayName("CHARACTER_DEVICE should have ordinal 2")
    void characterDeviceShouldHaveOrdinalTwo() {
      assertEquals(
          2,
          DescriptorType.CHARACTER_DEVICE.ordinal(),
          "CHARACTER_DEVICE ordinal should be 2");
    }

    @Test
    @DisplayName("DIRECTORY should have ordinal 3")
    void directoryShouldHaveOrdinalThree() {
      assertEquals(3, DescriptorType.DIRECTORY.ordinal(), "DIRECTORY ordinal should be 3");
    }

    @Test
    @DisplayName("FIFO should have ordinal 4")
    void fifoShouldHaveOrdinalFour() {
      assertEquals(4, DescriptorType.FIFO.ordinal(), "FIFO ordinal should be 4");
    }

    @Test
    @DisplayName("SYMBOLIC_LINK should have ordinal 5")
    void symbolicLinkShouldHaveOrdinalFive() {
      assertEquals(
          5, DescriptorType.SYMBOLIC_LINK.ordinal(), "SYMBOLIC_LINK ordinal should be 5");
    }

    @Test
    @DisplayName("REGULAR_FILE should have ordinal 6")
    void regularFileShouldHaveOrdinalSix() {
      assertEquals(
          6, DescriptorType.REGULAR_FILE.ordinal(), "REGULAR_FILE ordinal should be 6");
    }

    @Test
    @DisplayName("SOCKET should have ordinal 7")
    void socketShouldHaveOrdinalSeven() {
      assertEquals(7, DescriptorType.SOCKET.ordinal(), "SOCKET ordinal should be 7");
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final DescriptorType[] values = DescriptorType.values();

      for (int i = 0; i < values.length; i++) {
        assertEquals(
            i, values[i].ordinal(), "Ordinal should be " + i + " for " + values[i]);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return correct constant for each name")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(
          DescriptorType.UNKNOWN,
          DescriptorType.valueOf("UNKNOWN"),
          "valueOf('UNKNOWN') should return UNKNOWN");
      assertEquals(
          DescriptorType.BLOCK_DEVICE,
          DescriptorType.valueOf("BLOCK_DEVICE"),
          "valueOf('BLOCK_DEVICE') should return BLOCK_DEVICE");
      assertEquals(
          DescriptorType.CHARACTER_DEVICE,
          DescriptorType.valueOf("CHARACTER_DEVICE"),
          "valueOf('CHARACTER_DEVICE') should return CHARACTER_DEVICE");
      assertEquals(
          DescriptorType.DIRECTORY,
          DescriptorType.valueOf("DIRECTORY"),
          "valueOf('DIRECTORY') should return DIRECTORY");
      assertEquals(
          DescriptorType.FIFO,
          DescriptorType.valueOf("FIFO"),
          "valueOf('FIFO') should return FIFO");
      assertEquals(
          DescriptorType.SYMBOLIC_LINK,
          DescriptorType.valueOf("SYMBOLIC_LINK"),
          "valueOf('SYMBOLIC_LINK') should return SYMBOLIC_LINK");
      assertEquals(
          DescriptorType.REGULAR_FILE,
          DescriptorType.valueOf("REGULAR_FILE"),
          "valueOf('REGULAR_FILE') should return REGULAR_FILE");
      assertEquals(
          DescriptorType.SOCKET,
          DescriptorType.valueOf("SOCKET"),
          "valueOf('SOCKET') should return SOCKET");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> DescriptorType.valueOf("INVALID"),
          "valueOf('INVALID') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null name")
    void valueOfShouldThrowForNullName() {
      assertThrows(
          NullPointerException.class,
          () -> DescriptorType.valueOf(null),
          "valueOf(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array of length 8")
    void valuesShouldReturnArrayOfLengthEight() {
      assertEquals(
          8,
          DescriptorType.values().length,
          "values() should return array with 8 elements");
    }

    @Test
    @DisplayName("values should contain all constants")
    void valuesShouldContainAllConstants() {
      final Set<DescriptorType> valueSet =
          new HashSet<>(Arrays.asList(DescriptorType.values()));

      assertTrue(valueSet.contains(DescriptorType.UNKNOWN), "values() should contain UNKNOWN");
      assertTrue(
          valueSet.contains(DescriptorType.BLOCK_DEVICE),
          "values() should contain BLOCK_DEVICE");
      assertTrue(
          valueSet.contains(DescriptorType.CHARACTER_DEVICE),
          "values() should contain CHARACTER_DEVICE");
      assertTrue(
          valueSet.contains(DescriptorType.DIRECTORY), "values() should contain DIRECTORY");
      assertTrue(valueSet.contains(DescriptorType.FIFO), "values() should contain FIFO");
      assertTrue(
          valueSet.contains(DescriptorType.SYMBOLIC_LINK),
          "values() should contain SYMBOLIC_LINK");
      assertTrue(
          valueSet.contains(DescriptorType.REGULAR_FILE),
          "values() should contain REGULAR_FILE");
      assertTrue(valueSet.contains(DescriptorType.SOCKET), "values() should contain SOCKET");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final DescriptorType[] first = DescriptorType.values();
      final DescriptorType[] second = DescriptorType.values();

      assertTrue(first != second, "values() should return a new array instance each call");
      assertEquals(
          Arrays.asList(first),
          Arrays.asList(second),
          "values() arrays should have identical contents");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return constant name for each value")
    void toStringShouldReturnConstantName() {
      for (final DescriptorType type : DescriptorType.values()) {
        assertEquals(
            type.name(),
            type.toString(),
            "toString() should match name() for " + type.name());
      }
    }

    @Test
    @DisplayName("toString should return 'UNKNOWN' for UNKNOWN")
    void toStringShouldReturnUnknown() {
      assertEquals(
          "UNKNOWN",
          DescriptorType.UNKNOWN.toString(),
          "toString() should return 'UNKNOWN'");
    }

    @Test
    @DisplayName("toString should return 'REGULAR_FILE' for REGULAR_FILE")
    void toStringShouldReturnRegularFile() {
      assertEquals(
          "REGULAR_FILE",
          DescriptorType.REGULAR_FILE.toString(),
          "toString() should return 'REGULAR_FILE'");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should handle all constants in switch statement")
    void shouldHandleAllConstantsInSwitchStatement() {
      for (final DescriptorType type : DescriptorType.values()) {
        final String result;
        switch (type) {
          case UNKNOWN:
            result = "unknown";
            break;
          case BLOCK_DEVICE:
            result = "block_device";
            break;
          case CHARACTER_DEVICE:
            result = "character_device";
            break;
          case DIRECTORY:
            result = "directory";
            break;
          case FIFO:
            result = "fifo";
            break;
          case SYMBOLIC_LINK:
            result = "symbolic_link";
            break;
          case REGULAR_FILE:
            result = "regular_file";
            break;
          case SOCKET:
            result = "socket";
            break;
          default:
            result = "unhandled";
        }
        assertTrue(
            Arrays.asList(
                    "unknown",
                    "block_device",
                    "character_device",
                    "directory",
                    "fifo",
                    "symbolic_link",
                    "regular_file",
                    "socket")
                .contains(result),
            "Switch should handle " + type + " but got: " + result);
      }
    }
  }
}

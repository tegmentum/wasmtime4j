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
 * Tests for DescriptorFlags enum.
 *
 * <p>Verifies enum constants, ordinals, valueOf, values, toString, static factory methods, and
 * switch statement coverage for WASI filesystem descriptor flags.
 */
@DisplayName("DescriptorFlags Tests")
class DescriptorFlagsTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 6 enum constants")
    void shouldHaveExactlySixEnumConstants() {
      final DescriptorFlags[] values = DescriptorFlags.values();

      assertEquals(6, values.length, "DescriptorFlags should have exactly 6 constants");
    }

    @Test
    @DisplayName("should be a valid enum type")
    void shouldBeValidEnumType() {
      assertTrue(DescriptorFlags.class.isEnum(), "DescriptorFlags should be an enum type");
    }

    @Test
    @DisplayName("all constants should be non-null")
    void allConstantsShouldBeNonNull() {
      for (final DescriptorFlags flag : DescriptorFlags.values()) {
        assertNotNull(flag, "Every DescriptorFlags constant should be non-null");
      }
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have READ constant")
    void shouldHaveReadConstant() {
      final DescriptorFlags flag = DescriptorFlags.READ;

      assertNotNull(flag, "READ should not be null");
      assertEquals("READ", flag.name(), "Name should be READ");
    }

    @Test
    @DisplayName("should have WRITE constant")
    void shouldHaveWriteConstant() {
      final DescriptorFlags flag = DescriptorFlags.WRITE;

      assertNotNull(flag, "WRITE should not be null");
      assertEquals("WRITE", flag.name(), "Name should be WRITE");
    }

    @Test
    @DisplayName("should have FILE_INTEGRITY_SYNC constant")
    void shouldHaveFileIntegritySyncConstant() {
      final DescriptorFlags flag = DescriptorFlags.FILE_INTEGRITY_SYNC;

      assertNotNull(flag, "FILE_INTEGRITY_SYNC should not be null");
      assertEquals(
          "FILE_INTEGRITY_SYNC", flag.name(), "Name should be FILE_INTEGRITY_SYNC");
    }

    @Test
    @DisplayName("should have DATA_INTEGRITY_SYNC constant")
    void shouldHaveDataIntegritySyncConstant() {
      final DescriptorFlags flag = DescriptorFlags.DATA_INTEGRITY_SYNC;

      assertNotNull(flag, "DATA_INTEGRITY_SYNC should not be null");
      assertEquals(
          "DATA_INTEGRITY_SYNC", flag.name(), "Name should be DATA_INTEGRITY_SYNC");
    }

    @Test
    @DisplayName("should have REQUESTED_WRITE_SYNC constant")
    void shouldHaveRequestedWriteSyncConstant() {
      final DescriptorFlags flag = DescriptorFlags.REQUESTED_WRITE_SYNC;

      assertNotNull(flag, "REQUESTED_WRITE_SYNC should not be null");
      assertEquals(
          "REQUESTED_WRITE_SYNC", flag.name(), "Name should be REQUESTED_WRITE_SYNC");
    }

    @Test
    @DisplayName("should have MUTATE_DIRECTORY constant")
    void shouldHaveMutateDirectoryConstant() {
      final DescriptorFlags flag = DescriptorFlags.MUTATE_DIRECTORY;

      assertNotNull(flag, "MUTATE_DIRECTORY should not be null");
      assertEquals("MUTATE_DIRECTORY", flag.name(), "Name should be MUTATE_DIRECTORY");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("READ should have ordinal 0")
    void readShouldHaveOrdinalZero() {
      assertEquals(0, DescriptorFlags.READ.ordinal(), "READ ordinal should be 0");
    }

    @Test
    @DisplayName("WRITE should have ordinal 1")
    void writeShouldHaveOrdinalOne() {
      assertEquals(1, DescriptorFlags.WRITE.ordinal(), "WRITE ordinal should be 1");
    }

    @Test
    @DisplayName("FILE_INTEGRITY_SYNC should have ordinal 2")
    void fileIntegritySyncShouldHaveOrdinalTwo() {
      assertEquals(
          2,
          DescriptorFlags.FILE_INTEGRITY_SYNC.ordinal(),
          "FILE_INTEGRITY_SYNC ordinal should be 2");
    }

    @Test
    @DisplayName("DATA_INTEGRITY_SYNC should have ordinal 3")
    void dataIntegritySyncShouldHaveOrdinalThree() {
      assertEquals(
          3,
          DescriptorFlags.DATA_INTEGRITY_SYNC.ordinal(),
          "DATA_INTEGRITY_SYNC ordinal should be 3");
    }

    @Test
    @DisplayName("REQUESTED_WRITE_SYNC should have ordinal 4")
    void requestedWriteSyncShouldHaveOrdinalFour() {
      assertEquals(
          4,
          DescriptorFlags.REQUESTED_WRITE_SYNC.ordinal(),
          "REQUESTED_WRITE_SYNC ordinal should be 4");
    }

    @Test
    @DisplayName("MUTATE_DIRECTORY should have ordinal 5")
    void mutateDirectoryShouldHaveOrdinalFive() {
      assertEquals(
          5,
          DescriptorFlags.MUTATE_DIRECTORY.ordinal(),
          "MUTATE_DIRECTORY ordinal should be 5");
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final DescriptorFlags[] values = DescriptorFlags.values();

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
    @DisplayName("valueOf should return READ for 'READ'")
    void valueOfShouldReturnRead() {
      assertEquals(
          DescriptorFlags.READ,
          DescriptorFlags.valueOf("READ"),
          "valueOf('READ') should return READ");
    }

    @Test
    @DisplayName("valueOf should return WRITE for 'WRITE'")
    void valueOfShouldReturnWrite() {
      assertEquals(
          DescriptorFlags.WRITE,
          DescriptorFlags.valueOf("WRITE"),
          "valueOf('WRITE') should return WRITE");
    }

    @Test
    @DisplayName("valueOf should return FILE_INTEGRITY_SYNC for 'FILE_INTEGRITY_SYNC'")
    void valueOfShouldReturnFileIntegritySync() {
      assertEquals(
          DescriptorFlags.FILE_INTEGRITY_SYNC,
          DescriptorFlags.valueOf("FILE_INTEGRITY_SYNC"),
          "valueOf('FILE_INTEGRITY_SYNC') should return FILE_INTEGRITY_SYNC");
    }

    @Test
    @DisplayName("valueOf should return DATA_INTEGRITY_SYNC for 'DATA_INTEGRITY_SYNC'")
    void valueOfShouldReturnDataIntegritySync() {
      assertEquals(
          DescriptorFlags.DATA_INTEGRITY_SYNC,
          DescriptorFlags.valueOf("DATA_INTEGRITY_SYNC"),
          "valueOf('DATA_INTEGRITY_SYNC') should return DATA_INTEGRITY_SYNC");
    }

    @Test
    @DisplayName("valueOf should return REQUESTED_WRITE_SYNC for 'REQUESTED_WRITE_SYNC'")
    void valueOfShouldReturnRequestedWriteSync() {
      assertEquals(
          DescriptorFlags.REQUESTED_WRITE_SYNC,
          DescriptorFlags.valueOf("REQUESTED_WRITE_SYNC"),
          "valueOf('REQUESTED_WRITE_SYNC') should return REQUESTED_WRITE_SYNC");
    }

    @Test
    @DisplayName("valueOf should return MUTATE_DIRECTORY for 'MUTATE_DIRECTORY'")
    void valueOfShouldReturnMutateDirectory() {
      assertEquals(
          DescriptorFlags.MUTATE_DIRECTORY,
          DescriptorFlags.valueOf("MUTATE_DIRECTORY"),
          "valueOf('MUTATE_DIRECTORY') should return MUTATE_DIRECTORY");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> DescriptorFlags.valueOf("INVALID"),
          "valueOf('INVALID') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null name")
    void valueOfShouldThrowForNullName() {
      assertThrows(
          NullPointerException.class,
          () -> DescriptorFlags.valueOf(null),
          "valueOf(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array of length 6")
    void valuesShouldReturnArrayOfLengthSix() {
      assertEquals(
          6,
          DescriptorFlags.values().length,
          "values() should return array with 6 elements");
    }

    @Test
    @DisplayName("values should contain all constants")
    void valuesShouldContainAllConstants() {
      final Set<DescriptorFlags> valueSet =
          new HashSet<>(Arrays.asList(DescriptorFlags.values()));

      assertTrue(valueSet.contains(DescriptorFlags.READ), "values() should contain READ");
      assertTrue(valueSet.contains(DescriptorFlags.WRITE), "values() should contain WRITE");
      assertTrue(
          valueSet.contains(DescriptorFlags.FILE_INTEGRITY_SYNC),
          "values() should contain FILE_INTEGRITY_SYNC");
      assertTrue(
          valueSet.contains(DescriptorFlags.DATA_INTEGRITY_SYNC),
          "values() should contain DATA_INTEGRITY_SYNC");
      assertTrue(
          valueSet.contains(DescriptorFlags.REQUESTED_WRITE_SYNC),
          "values() should contain REQUESTED_WRITE_SYNC");
      assertTrue(
          valueSet.contains(DescriptorFlags.MUTATE_DIRECTORY),
          "values() should contain MUTATE_DIRECTORY");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final DescriptorFlags[] first = DescriptorFlags.values();
      final DescriptorFlags[] second = DescriptorFlags.values();

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
    @DisplayName("toString should return constant name for READ")
    void toStringShouldReturnNameForRead() {
      assertEquals("READ", DescriptorFlags.READ.toString(), "toString() should return 'READ'");
    }

    @Test
    @DisplayName("toString should return constant name for WRITE")
    void toStringShouldReturnNameForWrite() {
      assertEquals(
          "WRITE", DescriptorFlags.WRITE.toString(), "toString() should return 'WRITE'");
    }

    @Test
    @DisplayName("toString should return constant name for FILE_INTEGRITY_SYNC")
    void toStringShouldReturnNameForFileIntegritySync() {
      assertEquals(
          "FILE_INTEGRITY_SYNC",
          DescriptorFlags.FILE_INTEGRITY_SYNC.toString(),
          "toString() should return 'FILE_INTEGRITY_SYNC'");
    }

    @Test
    @DisplayName("toString should match name for all constants")
    void toStringShouldMatchNameForAllConstants() {
      for (final DescriptorFlags flag : DescriptorFlags.values()) {
        assertEquals(
            flag.name(),
            flag.toString(),
            "toString() should match name() for " + flag.name());
      }
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("of should create set with multiple flags")
    void ofShouldCreateSetWithMultipleFlags() {
      final Set<DescriptorFlags> flags =
          DescriptorFlags.of(DescriptorFlags.READ, DescriptorFlags.WRITE);

      assertEquals(2, flags.size(), "Set should contain exactly 2 flags");
      assertTrue(flags.contains(DescriptorFlags.READ), "Set should contain READ");
      assertTrue(flags.contains(DescriptorFlags.WRITE), "Set should contain WRITE");
    }

    @Test
    @DisplayName("of should create set with single flag")
    void ofShouldCreateSetWithSingleFlag() {
      final Set<DescriptorFlags> flags = DescriptorFlags.of(DescriptorFlags.READ);

      assertEquals(1, flags.size(), "Set should contain exactly 1 flag");
      assertTrue(flags.contains(DescriptorFlags.READ), "Set should contain READ");
    }

    @Test
    @DisplayName("of should create set with all flags")
    void ofShouldCreateSetWithAllFlags() {
      final Set<DescriptorFlags> flags =
          DescriptorFlags.of(
              DescriptorFlags.READ,
              DescriptorFlags.WRITE,
              DescriptorFlags.FILE_INTEGRITY_SYNC,
              DescriptorFlags.DATA_INTEGRITY_SYNC,
              DescriptorFlags.REQUESTED_WRITE_SYNC,
              DescriptorFlags.MUTATE_DIRECTORY);

      assertEquals(6, flags.size(), "Set should contain all 6 flags");
    }

    @Test
    @DisplayName("of should deduplicate flags")
    void ofShouldDeduplicateFlags() {
      final Set<DescriptorFlags> flags =
          DescriptorFlags.of(
              DescriptorFlags.READ, DescriptorFlags.READ, DescriptorFlags.WRITE);

      assertEquals(2, flags.size(), "Set should deduplicate repeated flags");
    }

    @Test
    @DisplayName("none should return empty set")
    void noneShouldReturnEmptySet() {
      final Set<DescriptorFlags> flags = DescriptorFlags.none();

      assertNotNull(flags, "none() should not return null");
      assertTrue(flags.isEmpty(), "none() should return an empty set");
      assertEquals(0, flags.size(), "none() set size should be 0");
    }

    @Test
    @DisplayName("all should return set with all constants")
    void allShouldReturnSetWithAllConstants() {
      final Set<DescriptorFlags> flags = DescriptorFlags.all();

      assertNotNull(flags, "all() should not return null");
      assertEquals(6, flags.size(), "all() should return set with 6 flags");
      assertTrue(flags.contains(DescriptorFlags.READ), "all() should contain READ");
      assertTrue(flags.contains(DescriptorFlags.WRITE), "all() should contain WRITE");
      assertTrue(
          flags.contains(DescriptorFlags.FILE_INTEGRITY_SYNC),
          "all() should contain FILE_INTEGRITY_SYNC");
      assertTrue(
          flags.contains(DescriptorFlags.DATA_INTEGRITY_SYNC),
          "all() should contain DATA_INTEGRITY_SYNC");
      assertTrue(
          flags.contains(DescriptorFlags.REQUESTED_WRITE_SYNC),
          "all() should contain REQUESTED_WRITE_SYNC");
      assertTrue(
          flags.contains(DescriptorFlags.MUTATE_DIRECTORY),
          "all() should contain MUTATE_DIRECTORY");
    }

    @Test
    @DisplayName("all should have same size as values array")
    void allShouldHaveSameSizeAsValuesArray() {
      assertEquals(
          DescriptorFlags.values().length,
          DescriptorFlags.all().size(),
          "all() size should match values().length");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should handle all constants in switch statement")
    void shouldHandleAllConstantsInSwitchStatement() {
      for (final DescriptorFlags flag : DescriptorFlags.values()) {
        final String result;
        switch (flag) {
          case READ:
            result = "read";
            break;
          case WRITE:
            result = "write";
            break;
          case FILE_INTEGRITY_SYNC:
            result = "file_integrity_sync";
            break;
          case DATA_INTEGRITY_SYNC:
            result = "data_integrity_sync";
            break;
          case REQUESTED_WRITE_SYNC:
            result = "requested_write_sync";
            break;
          case MUTATE_DIRECTORY:
            result = "mutate_directory";
            break;
          default:
            result = "unknown";
        }
        assertTrue(
            Arrays.asList(
                    "read",
                    "write",
                    "file_integrity_sync",
                    "data_integrity_sync",
                    "requested_write_sync",
                    "mutate_directory")
                .contains(result),
            "Switch should handle " + flag + " but got: " + result);
      }
    }
  }
}

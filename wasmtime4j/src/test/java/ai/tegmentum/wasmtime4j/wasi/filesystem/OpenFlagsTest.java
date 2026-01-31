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
 * Tests for OpenFlags enum.
 *
 * <p>Verifies enum constants, ordinals, valueOf, values, toString, static factory methods, and
 * switch statement coverage for WASI filesystem open flags.
 */
@DisplayName("OpenFlags Tests")
class OpenFlagsTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 4 enum constants")
    void shouldHaveExactlyFourEnumConstants() {
      final OpenFlags[] values = OpenFlags.values();

      assertEquals(4, values.length, "OpenFlags should have exactly 4 constants");
    }

    @Test
    @DisplayName("should be a valid enum type")
    void shouldBeValidEnumType() {
      assertTrue(OpenFlags.class.isEnum(), "OpenFlags should be an enum type");
    }

    @Test
    @DisplayName("all constants should be non-null")
    void allConstantsShouldBeNonNull() {
      for (final OpenFlags flag : OpenFlags.values()) {
        assertNotNull(flag, "Every OpenFlags constant should be non-null");
      }
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have CREATE constant")
    void shouldHaveCreateConstant() {
      final OpenFlags flag = OpenFlags.CREATE;

      assertNotNull(flag, "CREATE should not be null");
      assertEquals("CREATE", flag.name(), "Name should be CREATE");
    }

    @Test
    @DisplayName("should have DIRECTORY constant")
    void shouldHaveDirectoryConstant() {
      final OpenFlags flag = OpenFlags.DIRECTORY;

      assertNotNull(flag, "DIRECTORY should not be null");
      assertEquals("DIRECTORY", flag.name(), "Name should be DIRECTORY");
    }

    @Test
    @DisplayName("should have EXCLUSIVE constant")
    void shouldHaveExclusiveConstant() {
      final OpenFlags flag = OpenFlags.EXCLUSIVE;

      assertNotNull(flag, "EXCLUSIVE should not be null");
      assertEquals("EXCLUSIVE", flag.name(), "Name should be EXCLUSIVE");
    }

    @Test
    @DisplayName("should have TRUNCATE constant")
    void shouldHaveTruncateConstant() {
      final OpenFlags flag = OpenFlags.TRUNCATE;

      assertNotNull(flag, "TRUNCATE should not be null");
      assertEquals("TRUNCATE", flag.name(), "Name should be TRUNCATE");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("CREATE should have ordinal 0")
    void createShouldHaveOrdinalZero() {
      assertEquals(0, OpenFlags.CREATE.ordinal(), "CREATE ordinal should be 0");
    }

    @Test
    @DisplayName("DIRECTORY should have ordinal 1")
    void directoryShouldHaveOrdinalOne() {
      assertEquals(1, OpenFlags.DIRECTORY.ordinal(), "DIRECTORY ordinal should be 1");
    }

    @Test
    @DisplayName("EXCLUSIVE should have ordinal 2")
    void exclusiveShouldHaveOrdinalTwo() {
      assertEquals(2, OpenFlags.EXCLUSIVE.ordinal(), "EXCLUSIVE ordinal should be 2");
    }

    @Test
    @DisplayName("TRUNCATE should have ordinal 3")
    void truncateShouldHaveOrdinalThree() {
      assertEquals(3, OpenFlags.TRUNCATE.ordinal(), "TRUNCATE ordinal should be 3");
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final OpenFlags[] values = OpenFlags.values();

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
          OpenFlags.CREATE,
          OpenFlags.valueOf("CREATE"),
          "valueOf('CREATE') should return CREATE");
      assertEquals(
          OpenFlags.DIRECTORY,
          OpenFlags.valueOf("DIRECTORY"),
          "valueOf('DIRECTORY') should return DIRECTORY");
      assertEquals(
          OpenFlags.EXCLUSIVE,
          OpenFlags.valueOf("EXCLUSIVE"),
          "valueOf('EXCLUSIVE') should return EXCLUSIVE");
      assertEquals(
          OpenFlags.TRUNCATE,
          OpenFlags.valueOf("TRUNCATE"),
          "valueOf('TRUNCATE') should return TRUNCATE");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> OpenFlags.valueOf("INVALID"),
          "valueOf('INVALID') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null name")
    void valueOfShouldThrowForNullName() {
      assertThrows(
          NullPointerException.class,
          () -> OpenFlags.valueOf(null),
          "valueOf(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array of length 4")
    void valuesShouldReturnArrayOfLengthFour() {
      assertEquals(
          4, OpenFlags.values().length, "values() should return array with 4 elements");
    }

    @Test
    @DisplayName("values should contain all constants")
    void valuesShouldContainAllConstants() {
      final Set<OpenFlags> valueSet = new HashSet<>(Arrays.asList(OpenFlags.values()));

      assertTrue(valueSet.contains(OpenFlags.CREATE), "values() should contain CREATE");
      assertTrue(valueSet.contains(OpenFlags.DIRECTORY), "values() should contain DIRECTORY");
      assertTrue(valueSet.contains(OpenFlags.EXCLUSIVE), "values() should contain EXCLUSIVE");
      assertTrue(valueSet.contains(OpenFlags.TRUNCATE), "values() should contain TRUNCATE");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final OpenFlags[] first = OpenFlags.values();
      final OpenFlags[] second = OpenFlags.values();

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
    @DisplayName("toString should match name for all constants")
    void toStringShouldMatchNameForAllConstants() {
      for (final OpenFlags flag : OpenFlags.values()) {
        assertEquals(
            flag.name(),
            flag.toString(),
            "toString() should match name() for " + flag.name());
      }
    }

    @Test
    @DisplayName("toString should return 'CREATE' for CREATE")
    void toStringShouldReturnCreate() {
      assertEquals(
          "CREATE", OpenFlags.CREATE.toString(), "toString() should return 'CREATE'");
    }

    @Test
    @DisplayName("toString should return 'TRUNCATE' for TRUNCATE")
    void toStringShouldReturnTruncate() {
      assertEquals(
          "TRUNCATE", OpenFlags.TRUNCATE.toString(), "toString() should return 'TRUNCATE'");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("of should create set with multiple flags")
    void ofShouldCreateSetWithMultipleFlags() {
      final Set<OpenFlags> flags = OpenFlags.of(OpenFlags.CREATE, OpenFlags.TRUNCATE);

      assertEquals(2, flags.size(), "Set should contain exactly 2 flags");
      assertTrue(flags.contains(OpenFlags.CREATE), "Set should contain CREATE");
      assertTrue(flags.contains(OpenFlags.TRUNCATE), "Set should contain TRUNCATE");
    }

    @Test
    @DisplayName("of should create set with single flag")
    void ofShouldCreateSetWithSingleFlag() {
      final Set<OpenFlags> flags = OpenFlags.of(OpenFlags.EXCLUSIVE);

      assertEquals(1, flags.size(), "Set should contain exactly 1 flag");
      assertTrue(flags.contains(OpenFlags.EXCLUSIVE), "Set should contain EXCLUSIVE");
    }

    @Test
    @DisplayName("of should create set with all flags")
    void ofShouldCreateSetWithAllFlags() {
      final Set<OpenFlags> flags =
          OpenFlags.of(
              OpenFlags.CREATE,
              OpenFlags.DIRECTORY,
              OpenFlags.EXCLUSIVE,
              OpenFlags.TRUNCATE);

      assertEquals(4, flags.size(), "Set should contain all 4 flags");
    }

    @Test
    @DisplayName("of should return empty set for empty args")
    void ofShouldReturnEmptySetForEmptyArgs() {
      final Set<OpenFlags> flags = OpenFlags.of();

      assertNotNull(flags, "of() with no args should not return null");
      assertTrue(flags.isEmpty(), "of() with no args should return an empty set");
    }

    @Test
    @DisplayName("of should deduplicate flags")
    void ofShouldDeduplicateFlags() {
      final Set<OpenFlags> flags =
          OpenFlags.of(OpenFlags.CREATE, OpenFlags.CREATE, OpenFlags.TRUNCATE);

      assertEquals(2, flags.size(), "Set should deduplicate repeated flags");
    }

    @Test
    @DisplayName("none should return empty set")
    void noneShouldReturnEmptySet() {
      final Set<OpenFlags> flags = OpenFlags.none();

      assertNotNull(flags, "none() should not return null");
      assertTrue(flags.isEmpty(), "none() should return an empty set");
      assertEquals(0, flags.size(), "none() set size should be 0");
    }

    @Test
    @DisplayName("all should return set with all constants")
    void allShouldReturnSetWithAllConstants() {
      final Set<OpenFlags> flags = OpenFlags.all();

      assertNotNull(flags, "all() should not return null");
      assertEquals(4, flags.size(), "all() should return set with 4 flags");
      assertTrue(flags.contains(OpenFlags.CREATE), "all() should contain CREATE");
      assertTrue(flags.contains(OpenFlags.DIRECTORY), "all() should contain DIRECTORY");
      assertTrue(flags.contains(OpenFlags.EXCLUSIVE), "all() should contain EXCLUSIVE");
      assertTrue(flags.contains(OpenFlags.TRUNCATE), "all() should contain TRUNCATE");
    }

    @Test
    @DisplayName("all should have same size as values array")
    void allShouldHaveSameSizeAsValuesArray() {
      assertEquals(
          OpenFlags.values().length,
          OpenFlags.all().size(),
          "all() size should match values().length");
    }

    @Test
    @DisplayName("none result should not contain any flag")
    void noneResultShouldNotContainAnyFlag() {
      final Set<OpenFlags> empty = OpenFlags.none();

      for (final OpenFlags flag : OpenFlags.values()) {
        assertFalse(
            empty.contains(flag), "none() result should not contain " + flag);
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should handle all constants in switch statement")
    void shouldHandleAllConstantsInSwitchStatement() {
      for (final OpenFlags flag : OpenFlags.values()) {
        final String result;
        switch (flag) {
          case CREATE:
            result = "create";
            break;
          case DIRECTORY:
            result = "directory";
            break;
          case EXCLUSIVE:
            result = "exclusive";
            break;
          case TRUNCATE:
            result = "truncate";
            break;
          default:
            result = "unknown";
        }
        assertTrue(
            Arrays.asList("create", "directory", "exclusive", "truncate").contains(result),
            "Switch should handle " + flag + " but got: " + result);
      }
    }
  }
}

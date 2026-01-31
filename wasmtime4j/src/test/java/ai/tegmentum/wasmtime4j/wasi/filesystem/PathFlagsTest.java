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
 * Tests for PathFlags enum.
 *
 * <p>Verifies enum constants, ordinals, valueOf, values, toString, static factory methods, and
 * switch statement coverage for WASI filesystem path flags.
 */
@DisplayName("PathFlags Tests")
class PathFlagsTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 1 enum constant")
    void shouldHaveExactlyOneEnumConstant() {
      final PathFlags[] values = PathFlags.values();

      assertEquals(1, values.length, "PathFlags should have exactly 1 constant");
    }

    @Test
    @DisplayName("should be a valid enum type")
    void shouldBeValidEnumType() {
      assertTrue(PathFlags.class.isEnum(), "PathFlags should be an enum type");
    }

    @Test
    @DisplayName("all constants should be non-null")
    void allConstantsShouldBeNonNull() {
      for (final PathFlags flag : PathFlags.values()) {
        assertNotNull(flag, "Every PathFlags constant should be non-null");
      }
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have SYMLINK_FOLLOW constant")
    void shouldHaveSymlinkFollowConstant() {
      final PathFlags flag = PathFlags.SYMLINK_FOLLOW;

      assertNotNull(flag, "SYMLINK_FOLLOW should not be null");
      assertEquals("SYMLINK_FOLLOW", flag.name(), "Name should be SYMLINK_FOLLOW");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("SYMLINK_FOLLOW should have ordinal 0")
    void symlinkFollowShouldHaveOrdinalZero() {
      assertEquals(
          0, PathFlags.SYMLINK_FOLLOW.ordinal(), "SYMLINK_FOLLOW ordinal should be 0");
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final PathFlags[] values = PathFlags.values();

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
    @DisplayName("valueOf should return SYMLINK_FOLLOW for 'SYMLINK_FOLLOW'")
    void valueOfShouldReturnSymlinkFollow() {
      assertEquals(
          PathFlags.SYMLINK_FOLLOW,
          PathFlags.valueOf("SYMLINK_FOLLOW"),
          "valueOf('SYMLINK_FOLLOW') should return SYMLINK_FOLLOW");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PathFlags.valueOf("INVALID"),
          "valueOf('INVALID') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null name")
    void valueOfShouldThrowForNullName() {
      assertThrows(
          NullPointerException.class,
          () -> PathFlags.valueOf(null),
          "valueOf(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array of length 1")
    void valuesShouldReturnArrayOfLengthOne() {
      assertEquals(
          1, PathFlags.values().length, "values() should return array with 1 element");
    }

    @Test
    @DisplayName("values should contain SYMLINK_FOLLOW")
    void valuesShouldContainSymlinkFollow() {
      final Set<PathFlags> valueSet = new HashSet<>(Arrays.asList(PathFlags.values()));

      assertTrue(
          valueSet.contains(PathFlags.SYMLINK_FOLLOW),
          "values() should contain SYMLINK_FOLLOW");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final PathFlags[] first = PathFlags.values();
      final PathFlags[] second = PathFlags.values();

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
    @DisplayName("toString should return 'SYMLINK_FOLLOW' for SYMLINK_FOLLOW")
    void toStringShouldReturnSymlinkFollow() {
      assertEquals(
          "SYMLINK_FOLLOW",
          PathFlags.SYMLINK_FOLLOW.toString(),
          "toString() should return 'SYMLINK_FOLLOW'");
    }

    @Test
    @DisplayName("toString should match name for all constants")
    void toStringShouldMatchNameForAllConstants() {
      for (final PathFlags flag : PathFlags.values()) {
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
    @DisplayName("of should create set with SYMLINK_FOLLOW")
    void ofShouldCreateSetWithSymlinkFollow() {
      final Set<PathFlags> flags = PathFlags.of(PathFlags.SYMLINK_FOLLOW);

      assertEquals(1, flags.size(), "Set should contain exactly 1 flag");
      assertTrue(
          flags.contains(PathFlags.SYMLINK_FOLLOW), "Set should contain SYMLINK_FOLLOW");
    }

    @Test
    @DisplayName("of should return empty set for empty args")
    void ofShouldReturnEmptySetForEmptyArgs() {
      final Set<PathFlags> flags = PathFlags.of();

      assertNotNull(flags, "of() with no args should not return null");
      assertTrue(flags.isEmpty(), "of() with no args should return an empty set");
    }

    @Test
    @DisplayName("of should deduplicate flags")
    void ofShouldDeduplicateFlags() {
      final Set<PathFlags> flags =
          PathFlags.of(PathFlags.SYMLINK_FOLLOW, PathFlags.SYMLINK_FOLLOW);

      assertEquals(1, flags.size(), "Set should deduplicate repeated flags");
    }

    @Test
    @DisplayName("none should return empty set")
    void noneShouldReturnEmptySet() {
      final Set<PathFlags> flags = PathFlags.none();

      assertNotNull(flags, "none() should not return null");
      assertTrue(flags.isEmpty(), "none() should return an empty set");
      assertEquals(0, flags.size(), "none() set size should be 0");
    }

    @Test
    @DisplayName("none result should not contain SYMLINK_FOLLOW")
    void noneResultShouldNotContainSymlinkFollow() {
      final Set<PathFlags> empty = PathFlags.none();

      assertFalse(
          empty.contains(PathFlags.SYMLINK_FOLLOW),
          "none() result should not contain SYMLINK_FOLLOW");
    }

    @Test
    @DisplayName("all should return set with all constants")
    void allShouldReturnSetWithAllConstants() {
      final Set<PathFlags> flags = PathFlags.all();

      assertNotNull(flags, "all() should not return null");
      assertEquals(1, flags.size(), "all() should return set with 1 flag");
      assertTrue(
          flags.contains(PathFlags.SYMLINK_FOLLOW),
          "all() should contain SYMLINK_FOLLOW");
    }

    @Test
    @DisplayName("all should have same size as values array")
    void allShouldHaveSameSizeAsValuesArray() {
      assertEquals(
          PathFlags.values().length,
          PathFlags.all().size(),
          "all() size should match values().length");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should handle all constants in switch statement")
    void shouldHandleAllConstantsInSwitchStatement() {
      for (final PathFlags flag : PathFlags.values()) {
        final String result;
        switch (flag) {
          case SYMLINK_FOLLOW:
            result = "symlink_follow";
            break;
          default:
            result = "unknown";
        }
        assertEquals(
            "symlink_follow",
            result,
            "Switch should handle " + flag + " but got: " + result);
      }
    }
  }
}

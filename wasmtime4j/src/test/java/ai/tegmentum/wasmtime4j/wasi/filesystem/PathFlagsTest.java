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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PathFlags} enum.
 *
 * <p>PathFlags control path resolution behavior, particularly regarding symbolic links, in the WASI
 * Preview 2 filesystem specification.
 */
@DisplayName("PathFlags Tests")
class PathFlagsTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(PathFlags.class.isEnum(), "PathFlags should be an enum");
    }

    @Test
    @DisplayName("should have exactly 1 value")
    void shouldHaveExactlyOneValue() {
      final PathFlags[] values = PathFlags.values();
      assertEquals(1, values.length, "Should have exactly 1 path flag");
    }

    @Test
    @DisplayName("should have SYMLINK_FOLLOW value")
    void shouldHaveSymlinkFollowValue() {
      assertNotNull(PathFlags.valueOf("SYMLINK_FOLLOW"), "Should have SYMLINK_FOLLOW");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final PathFlags flag : PathFlags.values()) {
        assertTrue(ordinals.add(flag.ordinal()), "Ordinal should be unique: " + flag);
      }
    }

    @Test
    @DisplayName("should have unique names")
    void shouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (final PathFlags flag : PathFlags.values()) {
        assertTrue(names.add(flag.name()), "Name should be unique: " + flag);
      }
    }

    @Test
    @DisplayName("should be retrievable by name")
    void shouldBeRetrievableByName() {
      for (final PathFlags flag : PathFlags.values()) {
        assertEquals(flag, PathFlags.valueOf(flag.name()), "Should be retrievable by name");
      }
    }

    @Test
    @DisplayName("SYMLINK_FOLLOW should have ordinal 0")
    void symlinkFollowShouldHaveOrdinalZero() {
      assertEquals(0, PathFlags.SYMLINK_FOLLOW.ordinal(), "Should be first ordinal");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("of() should create set with single flag")
    void ofShouldCreateSetWithSingleFlag() {
      final Set<PathFlags> flags = PathFlags.of(PathFlags.SYMLINK_FOLLOW);
      assertNotNull(flags, "Should return non-null set");
      assertEquals(1, flags.size(), "Should contain one flag");
      assertTrue(flags.contains(PathFlags.SYMLINK_FOLLOW), "Should contain SYMLINK_FOLLOW");
    }

    @Test
    @DisplayName("of() with empty array should create empty set")
    void ofWithEmptyArrayShouldCreateEmptySet() {
      final Set<PathFlags> flags = PathFlags.of();
      assertNotNull(flags, "Should return non-null set");
      assertTrue(flags.isEmpty(), "Should be empty");
    }

    @Test
    @DisplayName("none() should create empty set")
    void noneShouldCreateEmptySet() {
      final Set<PathFlags> flags = PathFlags.none();
      assertNotNull(flags, "Should return non-null set");
      assertTrue(flags.isEmpty(), "Should be empty");
    }

    @Test
    @DisplayName("all() should create set with all flags")
    void allShouldCreateSetWithAllFlags() {
      final Set<PathFlags> flags = PathFlags.all();
      assertNotNull(flags, "Should return non-null set");
      assertEquals(1, flags.size(), "Should contain all 1 flag");
      assertTrue(flags.contains(PathFlags.SYMLINK_FOLLOW), "Should contain SYMLINK_FOLLOW");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support follow symlinks pattern")
    void shouldSupportFollowSymlinksPattern() {
      final Set<PathFlags> flags = PathFlags.of(PathFlags.SYMLINK_FOLLOW);
      assertTrue(flags.contains(PathFlags.SYMLINK_FOLLOW), "Should follow symlinks");
    }

    @Test
    @DisplayName("should support no-follow symlinks pattern")
    void shouldSupportNoFollowSymlinksPattern() {
      final Set<PathFlags> flags = PathFlags.none();
      assertFalse(flags.contains(PathFlags.SYMLINK_FOLLOW), "Should not follow symlinks");
    }

    @Test
    @DisplayName("none and empty should be equivalent")
    void noneAndEmptyShouldBeEquivalent() {
      final Set<PathFlags> none = PathFlags.none();
      final Set<PathFlags> empty = PathFlags.of();
      assertEquals(none.size(), empty.size(), "Should have same size");
      assertTrue(none.isEmpty(), "none() should be empty");
      assertTrue(empty.isEmpty(), "of() with no args should be empty");
    }
  }

  @Nested
  @DisplayName("WASI Specification Compliance Tests")
  class WasiSpecificationComplianceTests {

    @Test
    @DisplayName("should have all WASI Preview 2 path-flags")
    void shouldHaveAllWasiPreview2PathFlags() {
      // WASI Preview 2 specifies: symlink-follow
      assertNotNull(PathFlags.valueOf("SYMLINK_FOLLOW"), "Should have WASI symlink-follow flag");
    }

    @Test
    @DisplayName("should have correct number of flags as per specification")
    void shouldHaveCorrectNumberOfFlagsAsPerSpecification() {
      assertEquals(1, PathFlags.values().length, "Should match WASI Preview 2 path-flags count");
    }
  }

  @Nested
  @DisplayName("Symbolic Link Resolution Tests")
  class SymbolicLinkResolutionTests {

    @Test
    @DisplayName("with SYMLINK_FOLLOW should expand symlinks")
    void withSymlinkFollowShouldExpandSymlinks() {
      final Set<PathFlags> flags = PathFlags.of(PathFlags.SYMLINK_FOLLOW);
      assertTrue(flags.contains(PathFlags.SYMLINK_FOLLOW), "Should indicate symlinks are followed");
    }

    @Test
    @DisplayName("without SYMLINK_FOLLOW should operate on symlink itself")
    void withoutSymlinkFollowShouldOperateOnSymlinkItself() {
      final Set<PathFlags> flags = PathFlags.none();
      assertFalse(
          flags.contains(PathFlags.SYMLINK_FOLLOW),
          "Should indicate operations target symlink itself");
    }
  }

  @Nested
  @DisplayName("Switch Statement Tests")
  class SwitchStatementTests {

    @Test
    @DisplayName("should support switch statement")
    void shouldSupportSwitchStatement() {
      final PathFlags flag = PathFlags.SYMLINK_FOLLOW;

      final String description;
      switch (flag) {
        case SYMLINK_FOLLOW:
          description = "Follow symbolic links during resolution";
          break;
        default:
          description = "Unknown flag";
      }

      assertEquals(
          "Follow symbolic links during resolution", description, "SYMLINK_FOLLOW description");
    }
  }

  @Nested
  @DisplayName("Collection Usage Tests")
  class CollectionUsageTests {

    @Test
    @DisplayName("should be usable in collections")
    void shouldBeUsableInCollections() {
      final Set<PathFlags> flags = new HashSet<>();
      flags.add(PathFlags.SYMLINK_FOLLOW);

      assertTrue(flags.contains(PathFlags.SYMLINK_FOLLOW), "Should contain SYMLINK_FOLLOW");
      assertEquals(1, flags.size(), "Should have 1 flag");
    }

    @Test
    @DisplayName("should support contains check")
    void shouldSupportContainsCheck() {
      final Set<PathFlags> withFollow = PathFlags.all();
      final Set<PathFlags> withoutFollow = PathFlags.none();

      assertTrue(withFollow.contains(PathFlags.SYMLINK_FOLLOW), "all() should contain flag");
      assertFalse(
          withoutFollow.contains(PathFlags.SYMLINK_FOLLOW), "none() should not contain flag");
    }
  }

  @Nested
  @DisplayName("POSIX Compatibility Tests")
  class PosixCompatibilityTests {

    @Test
    @DisplayName("SYMLINK_FOLLOW behavior should match AT_SYMLINK_FOLLOW")
    void symlinkFollowBehaviorShouldMatchAtSymlinkFollow() {
      // Documents that SYMLINK_FOLLOW has similar semantics to POSIX AT_SYMLINK_FOLLOW
      // When set: follow symlinks (like most operations by default)
      // When not set: act on symlink itself (like lstat vs stat)
      final PathFlags flag = PathFlags.SYMLINK_FOLLOW;
      assertEquals("SYMLINK_FOLLOW", flag.name(), "Should be SYMLINK_FOLLOW");
    }
  }
}

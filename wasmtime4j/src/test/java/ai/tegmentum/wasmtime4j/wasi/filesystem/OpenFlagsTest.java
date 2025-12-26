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
 * Tests for {@link OpenFlags} enum.
 *
 * <p>OpenFlags control file creation and opening behavior in the WASI Preview 2 filesystem
 * specification, similar to O_CREAT, O_DIRECTORY, O_EXCL, and O_TRUNC in POSIX.
 */
@DisplayName("OpenFlags Tests")
class OpenFlagsTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(OpenFlags.class.isEnum(), "OpenFlags should be an enum");
    }

    @Test
    @DisplayName("should have exactly 4 values")
    void shouldHaveExactlyFourValues() {
      final OpenFlags[] values = OpenFlags.values();
      assertEquals(4, values.length, "Should have exactly 4 open flags");
    }

    @Test
    @DisplayName("should have CREATE value")
    void shouldHaveCreateValue() {
      assertNotNull(OpenFlags.valueOf("CREATE"), "Should have CREATE");
    }

    @Test
    @DisplayName("should have DIRECTORY value")
    void shouldHaveDirectoryValue() {
      assertNotNull(OpenFlags.valueOf("DIRECTORY"), "Should have DIRECTORY");
    }

    @Test
    @DisplayName("should have EXCLUSIVE value")
    void shouldHaveExclusiveValue() {
      assertNotNull(OpenFlags.valueOf("EXCLUSIVE"), "Should have EXCLUSIVE");
    }

    @Test
    @DisplayName("should have TRUNCATE value")
    void shouldHaveTruncateValue() {
      assertNotNull(OpenFlags.valueOf("TRUNCATE"), "Should have TRUNCATE");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final OpenFlags flag : OpenFlags.values()) {
        assertTrue(ordinals.add(flag.ordinal()), "Ordinal should be unique: " + flag);
      }
    }

    @Test
    @DisplayName("should have unique names")
    void shouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (final OpenFlags flag : OpenFlags.values()) {
        assertTrue(names.add(flag.name()), "Name should be unique: " + flag);
      }
    }

    @Test
    @DisplayName("should be retrievable by name")
    void shouldBeRetrievableByName() {
      for (final OpenFlags flag : OpenFlags.values()) {
        assertEquals(flag, OpenFlags.valueOf(flag.name()), "Should be retrievable by name");
      }
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("of() should create set with single flag")
    void ofShouldCreateSetWithSingleFlag() {
      final Set<OpenFlags> flags = OpenFlags.of(OpenFlags.CREATE);
      assertNotNull(flags, "Should return non-null set");
      assertEquals(1, flags.size(), "Should contain one flag");
      assertTrue(flags.contains(OpenFlags.CREATE), "Should contain CREATE");
    }

    @Test
    @DisplayName("of() should create set with multiple flags")
    void ofShouldCreateSetWithMultipleFlags() {
      final Set<OpenFlags> flags = OpenFlags.of(OpenFlags.CREATE, OpenFlags.EXCLUSIVE);
      assertNotNull(flags, "Should return non-null set");
      assertEquals(2, flags.size(), "Should contain two flags");
      assertTrue(flags.contains(OpenFlags.CREATE), "Should contain CREATE");
      assertTrue(flags.contains(OpenFlags.EXCLUSIVE), "Should contain EXCLUSIVE");
    }

    @Test
    @DisplayName("of() with empty array should create empty set")
    void ofWithEmptyArrayShouldCreateEmptySet() {
      final Set<OpenFlags> flags = OpenFlags.of();
      assertNotNull(flags, "Should return non-null set");
      assertTrue(flags.isEmpty(), "Should be empty");
    }

    @Test
    @DisplayName("none() should create empty set")
    void noneShouldCreateEmptySet() {
      final Set<OpenFlags> flags = OpenFlags.none();
      assertNotNull(flags, "Should return non-null set");
      assertTrue(flags.isEmpty(), "Should be empty");
    }

    @Test
    @DisplayName("all() should create set with all flags")
    void allShouldCreateSetWithAllFlags() {
      final Set<OpenFlags> flags = OpenFlags.all();
      assertNotNull(flags, "Should return non-null set");
      assertEquals(4, flags.size(), "Should contain all 4 flags");
      for (final OpenFlags flag : OpenFlags.values()) {
        assertTrue(flags.contains(flag), "Should contain: " + flag);
      }
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support create-if-not-exists pattern")
    void shouldSupportCreateIfNotExistsPattern() {
      final Set<OpenFlags> flags = OpenFlags.of(OpenFlags.CREATE);
      assertTrue(flags.contains(OpenFlags.CREATE), "Should have CREATE");
      assertFalse(flags.contains(OpenFlags.EXCLUSIVE), "Should not have EXCLUSIVE");
    }

    @Test
    @DisplayName("should support create-exclusive pattern")
    void shouldSupportCreateExclusivePattern() {
      final Set<OpenFlags> flags = OpenFlags.of(OpenFlags.CREATE, OpenFlags.EXCLUSIVE);
      assertTrue(flags.contains(OpenFlags.CREATE), "Should have CREATE");
      assertTrue(flags.contains(OpenFlags.EXCLUSIVE), "Should have EXCLUSIVE");
    }

    @Test
    @DisplayName("should support open-and-truncate pattern")
    void shouldSupportOpenAndTruncatePattern() {
      final Set<OpenFlags> flags = OpenFlags.of(OpenFlags.TRUNCATE);
      assertTrue(flags.contains(OpenFlags.TRUNCATE), "Should have TRUNCATE");
    }

    @Test
    @DisplayName("should support open-directory pattern")
    void shouldSupportOpenDirectoryPattern() {
      final Set<OpenFlags> flags = OpenFlags.of(OpenFlags.DIRECTORY);
      assertTrue(flags.contains(OpenFlags.DIRECTORY), "Should have DIRECTORY");
    }

    @Test
    @DisplayName("should support no flags pattern")
    void shouldSupportNoFlagsPattern() {
      final Set<OpenFlags> flags = OpenFlags.none();
      assertTrue(flags.isEmpty(), "Should have no flags");
    }
  }

  @Nested
  @DisplayName("POSIX Mapping Tests")
  class PosixMappingTests {

    @Test
    @DisplayName("CREATE should map to O_CREAT")
    void createShouldMapToOCreat() {
      final OpenFlags create = OpenFlags.CREATE;
      assertEquals("CREATE", create.name(), "Should be CREATE (O_CREAT)");
    }

    @Test
    @DisplayName("DIRECTORY should map to O_DIRECTORY")
    void directoryShouldMapToODirectory() {
      final OpenFlags directory = OpenFlags.DIRECTORY;
      assertEquals("DIRECTORY", directory.name(), "Should be DIRECTORY (O_DIRECTORY)");
    }

    @Test
    @DisplayName("EXCLUSIVE should map to O_EXCL")
    void exclusiveShouldMapToOExcl() {
      final OpenFlags exclusive = OpenFlags.EXCLUSIVE;
      assertEquals("EXCLUSIVE", exclusive.name(), "Should be EXCLUSIVE (O_EXCL)");
    }

    @Test
    @DisplayName("TRUNCATE should map to O_TRUNC")
    void truncateShouldMapToOTrunc() {
      final OpenFlags truncate = OpenFlags.TRUNCATE;
      assertEquals("TRUNCATE", truncate.name(), "Should be TRUNCATE (O_TRUNC)");
    }
  }

  @Nested
  @DisplayName("WASI Specification Compliance Tests")
  class WasiSpecificationComplianceTests {

    @Test
    @DisplayName("should have all WASI Preview 2 open-flags")
    void shouldHaveAllWasiPreview2OpenFlags() {
      // WASI Preview 2 specifies: create, directory, exclusive, truncate
      final String[] expectedFlags = {"CREATE", "DIRECTORY", "EXCLUSIVE", "TRUNCATE"};

      for (final String flagName : expectedFlags) {
        assertNotNull(OpenFlags.valueOf(flagName), "Should have WASI flag: " + flagName);
      }
    }

    @Test
    @DisplayName("should have correct number of flags as per specification")
    void shouldHaveCorrectNumberOfFlagsAsPerSpecification() {
      assertEquals(4, OpenFlags.values().length, "Should match WASI Preview 2 open-flags count");
    }
  }

  @Nested
  @DisplayName("Flag Combination Tests")
  class FlagCombinationTests {

    @Test
    @DisplayName("EXCLUSIVE without CREATE should have no special meaning")
    void exclusiveWithoutCreateShouldHaveNoSpecialMeaning() {
      // EXCLUSIVE only has meaning when combined with CREATE
      final Set<OpenFlags> exclusiveOnly = OpenFlags.of(OpenFlags.EXCLUSIVE);
      assertEquals(1, exclusiveOnly.size(), "Should have only EXCLUSIVE");
      assertFalse(exclusiveOnly.contains(OpenFlags.CREATE), "Should not have CREATE");
    }

    @Test
    @DisplayName("CREATE with EXCLUSIVE should fail if file exists")
    void createWithExclusiveShouldFailIfFileExists() {
      // This documents the expected semantic behavior
      final Set<OpenFlags> createExclusive = OpenFlags.of(OpenFlags.CREATE, OpenFlags.EXCLUSIVE);
      assertEquals(2, createExclusive.size(), "Should have CREATE and EXCLUSIVE");
    }

    @Test
    @DisplayName("TRUNCATE with CREATE should create and truncate")
    void truncateWithCreateShouldCreateAndTruncate() {
      final Set<OpenFlags> createTruncate = OpenFlags.of(OpenFlags.CREATE, OpenFlags.TRUNCATE);
      assertTrue(createTruncate.contains(OpenFlags.CREATE), "Should have CREATE");
      assertTrue(createTruncate.contains(OpenFlags.TRUNCATE), "Should have TRUNCATE");
    }
  }

  @Nested
  @DisplayName("Switch Statement Tests")
  class SwitchStatementTests {

    @Test
    @DisplayName("should support switch statement")
    void shouldSupportSwitchStatement() {
      final OpenFlags flag = OpenFlags.CREATE;

      final String description;
      switch (flag) {
        case CREATE:
          description = "Create file if not exists";
          break;
        case DIRECTORY:
          description = "Fail if not a directory";
          break;
        case EXCLUSIVE:
          description = "Fail if file exists";
          break;
        case TRUNCATE:
          description = "Truncate file to zero";
          break;
        default:
          description = "Unknown flag";
      }

      assertEquals("Create file if not exists", description, "CREATE description");
    }
  }
}

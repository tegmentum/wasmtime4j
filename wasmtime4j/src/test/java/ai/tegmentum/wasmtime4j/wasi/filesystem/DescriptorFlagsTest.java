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
 * Tests for {@link DescriptorFlags} enum.
 *
 * <p>DescriptorFlags control access permissions and synchronization behavior for file descriptors
 * in the WASI Preview 2 filesystem specification.
 */
@DisplayName("DescriptorFlags Tests")
class DescriptorFlagsTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(DescriptorFlags.class.isEnum(), "DescriptorFlags should be an enum");
    }

    @Test
    @DisplayName("should have exactly 6 values")
    void shouldHaveExactlySixValues() {
      final DescriptorFlags[] values = DescriptorFlags.values();
      assertEquals(6, values.length, "Should have exactly 6 descriptor flags");
    }

    @Test
    @DisplayName("should have READ value")
    void shouldHaveReadValue() {
      assertNotNull(DescriptorFlags.valueOf("READ"), "Should have READ");
    }

    @Test
    @DisplayName("should have WRITE value")
    void shouldHaveWriteValue() {
      assertNotNull(DescriptorFlags.valueOf("WRITE"), "Should have WRITE");
    }

    @Test
    @DisplayName("should have FILE_INTEGRITY_SYNC value")
    void shouldHaveFileIntegritySyncValue() {
      assertNotNull(
          DescriptorFlags.valueOf("FILE_INTEGRITY_SYNC"), "Should have FILE_INTEGRITY_SYNC");
    }

    @Test
    @DisplayName("should have DATA_INTEGRITY_SYNC value")
    void shouldHaveDataIntegritySyncValue() {
      assertNotNull(
          DescriptorFlags.valueOf("DATA_INTEGRITY_SYNC"), "Should have DATA_INTEGRITY_SYNC");
    }

    @Test
    @DisplayName("should have REQUESTED_WRITE_SYNC value")
    void shouldHaveRequestedWriteSyncValue() {
      assertNotNull(
          DescriptorFlags.valueOf("REQUESTED_WRITE_SYNC"), "Should have REQUESTED_WRITE_SYNC");
    }

    @Test
    @DisplayName("should have MUTATE_DIRECTORY value")
    void shouldHaveMutateDirectoryValue() {
      assertNotNull(DescriptorFlags.valueOf("MUTATE_DIRECTORY"), "Should have MUTATE_DIRECTORY");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final DescriptorFlags flag : DescriptorFlags.values()) {
        assertTrue(ordinals.add(flag.ordinal()), "Ordinal should be unique: " + flag);
      }
    }

    @Test
    @DisplayName("should have unique names")
    void shouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (final DescriptorFlags flag : DescriptorFlags.values()) {
        assertTrue(names.add(flag.name()), "Name should be unique: " + flag);
      }
    }

    @Test
    @DisplayName("should be retrievable by name")
    void shouldBeRetrievableByName() {
      for (final DescriptorFlags flag : DescriptorFlags.values()) {
        assertEquals(flag, DescriptorFlags.valueOf(flag.name()), "Should be retrievable by name");
      }
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("of() should create set with single flag")
    void ofShouldCreateSetWithSingleFlag() {
      final Set<DescriptorFlags> flags = DescriptorFlags.of(DescriptorFlags.READ);
      assertNotNull(flags, "Should return non-null set");
      assertEquals(1, flags.size(), "Should contain one flag");
      assertTrue(flags.contains(DescriptorFlags.READ), "Should contain READ");
    }

    @Test
    @DisplayName("of() should create set with multiple flags")
    void ofShouldCreateSetWithMultipleFlags() {
      final Set<DescriptorFlags> flags =
          DescriptorFlags.of(DescriptorFlags.READ, DescriptorFlags.WRITE);
      assertNotNull(flags, "Should return non-null set");
      assertEquals(2, flags.size(), "Should contain two flags");
      assertTrue(flags.contains(DescriptorFlags.READ), "Should contain READ");
      assertTrue(flags.contains(DescriptorFlags.WRITE), "Should contain WRITE");
    }

    @Test
    @DisplayName("none() should create empty set")
    void noneShouldCreateEmptySet() {
      final Set<DescriptorFlags> flags = DescriptorFlags.none();
      assertNotNull(flags, "Should return non-null set");
      assertTrue(flags.isEmpty(), "Should be empty");
    }

    @Test
    @DisplayName("all() should create set with all flags")
    void allShouldCreateSetWithAllFlags() {
      final Set<DescriptorFlags> flags = DescriptorFlags.all();
      assertNotNull(flags, "Should return non-null set");
      assertEquals(6, flags.size(), "Should contain all 6 flags");
      for (final DescriptorFlags flag : DescriptorFlags.values()) {
        assertTrue(flags.contains(flag), "Should contain: " + flag);
      }
    }
  }

  @Nested
  @DisplayName("Flag Category Tests")
  class FlagCategoryTests {

    @Test
    @DisplayName("should have access flags")
    void shouldHaveAccessFlags() {
      final Set<DescriptorFlags> accessFlags = Set.of(DescriptorFlags.READ, DescriptorFlags.WRITE);
      for (final DescriptorFlags flag : accessFlags) {
        assertNotNull(flag, "Should have access flag: " + flag);
      }
    }

    @Test
    @DisplayName("should have sync flags")
    void shouldHaveSyncFlags() {
      final Set<DescriptorFlags> syncFlags =
          Set.of(
              DescriptorFlags.FILE_INTEGRITY_SYNC,
              DescriptorFlags.DATA_INTEGRITY_SYNC,
              DescriptorFlags.REQUESTED_WRITE_SYNC);
      for (final DescriptorFlags flag : syncFlags) {
        assertNotNull(flag, "Should have sync flag: " + flag);
      }
    }

    @Test
    @DisplayName("should have directory flag")
    void shouldHaveDirectoryFlag() {
      assertNotNull(DescriptorFlags.MUTATE_DIRECTORY, "Should have MUTATE_DIRECTORY");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support typical read-only pattern")
    void shouldSupportTypicalReadOnlyPattern() {
      final Set<DescriptorFlags> readOnly = DescriptorFlags.of(DescriptorFlags.READ);
      assertTrue(readOnly.contains(DescriptorFlags.READ), "Should have READ");
      assertFalse(readOnly.contains(DescriptorFlags.WRITE), "Should not have WRITE");
    }

    @Test
    @DisplayName("should support typical read-write pattern")
    void shouldSupportTypicalReadWritePattern() {
      final Set<DescriptorFlags> readWrite =
          DescriptorFlags.of(DescriptorFlags.READ, DescriptorFlags.WRITE);
      assertTrue(readWrite.contains(DescriptorFlags.READ), "Should have READ");
      assertTrue(readWrite.contains(DescriptorFlags.WRITE), "Should have WRITE");
    }

    @Test
    @DisplayName("should support synchronized write pattern")
    void shouldSupportSynchronizedWritePattern() {
      final Set<DescriptorFlags> syncWrite =
          DescriptorFlags.of(DescriptorFlags.WRITE, DescriptorFlags.FILE_INTEGRITY_SYNC);
      assertTrue(syncWrite.contains(DescriptorFlags.WRITE), "Should have WRITE");
      assertTrue(syncWrite.contains(DescriptorFlags.FILE_INTEGRITY_SYNC), "Should have sync");
    }

    @Test
    @DisplayName("should support directory modification pattern")
    void shouldSupportDirectoryModificationPattern() {
      final Set<DescriptorFlags> dirMod =
          DescriptorFlags.of(DescriptorFlags.READ, DescriptorFlags.MUTATE_DIRECTORY);
      assertTrue(dirMod.contains(DescriptorFlags.READ), "Should have READ");
      assertTrue(dirMod.contains(DescriptorFlags.MUTATE_DIRECTORY), "Should have MUTATE_DIRECTORY");
    }
  }

  @Nested
  @DisplayName("WASI Specification Compliance Tests")
  class WasiSpecificationComplianceTests {

    @Test
    @DisplayName("should have all WASI Preview 2 descriptor-flags")
    void shouldHaveAllWasiPreview2DescriptorFlags() {
      // WASI Preview 2 specifies: read, write, file-integrity-sync,
      // data-integrity-sync, requested-write-sync, mutate-directory
      final String[] expectedFlags = {
        "READ",
        "WRITE",
        "FILE_INTEGRITY_SYNC",
        "DATA_INTEGRITY_SYNC",
        "REQUESTED_WRITE_SYNC",
        "MUTATE_DIRECTORY"
      };

      for (final String flagName : expectedFlags) {
        assertNotNull(DescriptorFlags.valueOf(flagName), "Should have WASI flag: " + flagName);
      }
    }

    @Test
    @DisplayName("should have correct number of flags as per specification")
    void shouldHaveCorrectNumberOfFlagsAsPerSpecification() {
      assertEquals(
          6, DescriptorFlags.values().length, "Should match WASI Preview 2 descriptor-flags count");
    }
  }

  @Nested
  @DisplayName("Synchronization Flag Tests")
  class SynchronizationFlagTests {

    @Test
    @DisplayName("FILE_INTEGRITY_SYNC should be more strict than DATA_INTEGRITY_SYNC")
    void fileIntegritySyncShouldBeMoreStrictThanDataIntegritySync() {
      // FILE_INTEGRITY_SYNC ensures both data and metadata are synchronized
      // DATA_INTEGRITY_SYNC ensures only data is synchronized
      final DescriptorFlags fileSync = DescriptorFlags.FILE_INTEGRITY_SYNC;
      final DescriptorFlags dataSync = DescriptorFlags.DATA_INTEGRITY_SYNC;

      // Both should exist but be distinct
      assertTrue(fileSync != dataSync, "Should be distinct flags");
      assertTrue(fileSync.ordinal() != dataSync.ordinal(), "Should have different ordinals");
    }

    @Test
    @DisplayName("REQUESTED_WRITE_SYNC should work with other sync flags")
    void requestedWriteSyncShouldWorkWithOtherSyncFlags() {
      final Set<DescriptorFlags> syncCombination =
          DescriptorFlags.of(
              DescriptorFlags.READ,
              DescriptorFlags.FILE_INTEGRITY_SYNC,
              DescriptorFlags.REQUESTED_WRITE_SYNC);

      assertEquals(3, syncCombination.size(), "Should have 3 flags");
      assertTrue(
          syncCombination.contains(DescriptorFlags.REQUESTED_WRITE_SYNC),
          "Should contain REQUESTED_WRITE_SYNC");
    }
  }
}

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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiOpenFlags} enum.
 *
 * <p>This test class verifies WasiOpenFlags enum values and flag operations.
 */
@DisplayName("WasiOpenFlags Tests")
class WasiOpenFlagsTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasiOpenFlags should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiOpenFlags.class.isEnum(), "WasiOpenFlags should be an enum");
    }

    @Test
    @DisplayName("Should have expected number of values")
    void shouldHaveExpectedNumberOfValues() {
      assertEquals(4, WasiOpenFlags.values().length, "Should have 4 open flags");
    }

    @Test
    @DisplayName("All enum values should have unique names")
    void allEnumValuesShouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (WasiOpenFlags flag : WasiOpenFlags.values()) {
        assertTrue(names.add(flag.name()), "Name should be unique: " + flag.name());
      }
    }

    @Test
    @DisplayName("All enum values should have unique numeric values")
    void allEnumValuesShouldHaveUniqueValues() {
      final Set<Integer> values = new HashSet<>();
      for (WasiOpenFlags flag : WasiOpenFlags.values()) {
        assertTrue(values.add(flag.getValue()), "Value should be unique: " + flag.name());
      }
    }
  }

  @Nested
  @DisplayName("Value Tests")
  class ValueTests {

    @Test
    @DisplayName("CREAT should have value 1")
    void creatShouldHaveValue1() {
      assertEquals(1, WasiOpenFlags.CREAT.getValue(), "CREAT should have value 1");
    }

    @Test
    @DisplayName("DIRECTORY should have value 2")
    void directoryShouldHaveValue2() {
      assertEquals(2, WasiOpenFlags.DIRECTORY.getValue(), "DIRECTORY should have value 2");
    }

    @Test
    @DisplayName("EXCL should have value 4")
    void exclShouldHaveValue4() {
      assertEquals(4, WasiOpenFlags.EXCL.getValue(), "EXCL should have value 4");
    }

    @Test
    @DisplayName("TRUNC should have value 8")
    void truncShouldHaveValue8() {
      assertEquals(8, WasiOpenFlags.TRUNC.getValue(), "TRUNC should have value 8");
    }
  }

  @Nested
  @DisplayName("combine Tests")
  class CombineTests {

    @Test
    @DisplayName("combine with no flags should return 0")
    void combineWithNoFlagsShouldReturnZero() {
      assertEquals(0, WasiOpenFlags.combine(), "combine() with no args should return 0");
    }

    @Test
    @DisplayName("combine with single flag should return flag value")
    void combineWithSingleFlagShouldReturnFlagValue() {
      assertEquals(1, WasiOpenFlags.combine(WasiOpenFlags.CREAT), "combine(CREAT) should return 1");
      assertEquals(4, WasiOpenFlags.combine(WasiOpenFlags.EXCL), "combine(EXCL) should return 4");
    }

    @Test
    @DisplayName("combine with multiple flags should return OR of values")
    void combineWithMultipleFlagsShouldReturnOrOfValues() {
      // CREAT (1) | EXCL (4) = 5
      assertEquals(
          5,
          WasiOpenFlags.combine(WasiOpenFlags.CREAT, WasiOpenFlags.EXCL),
          "combine(CREAT, EXCL) should return 5");
      // CREAT (1) | TRUNC (8) = 9
      assertEquals(
          9,
          WasiOpenFlags.combine(WasiOpenFlags.CREAT, WasiOpenFlags.TRUNC),
          "combine(CREAT, TRUNC) should return 9");
      // All flags: 1 | 2 | 4 | 8 = 15
      assertEquals(
          15,
          WasiOpenFlags.combine(
              WasiOpenFlags.CREAT,
              WasiOpenFlags.DIRECTORY,
              WasiOpenFlags.EXCL,
              WasiOpenFlags.TRUNC),
          "combine(all) should return 15");
    }

    @Test
    @DisplayName("combine with duplicate flags should return same value")
    void combineWithDuplicateFlagsShouldReturnSameValue() {
      final int single = WasiOpenFlags.combine(WasiOpenFlags.CREAT);
      final int duplicate = WasiOpenFlags.combine(WasiOpenFlags.CREAT, WasiOpenFlags.CREAT);
      assertEquals(single, duplicate, "Combining same flag twice should be idempotent");
    }
  }

  @Nested
  @DisplayName("contains Tests")
  class ContainsTests {

    @Test
    @DisplayName("contains should return true when flag is present")
    void containsShouldReturnTrueWhenFlagIsPresent() {
      final int mask = WasiOpenFlags.CREAT.getValue();
      assertTrue(
          WasiOpenFlags.contains(mask, WasiOpenFlags.CREAT),
          "contains should return true when flag is in mask");
    }

    @Test
    @DisplayName("contains should return false when flag is not present")
    void containsShouldReturnFalseWhenFlagIsNotPresent() {
      final int mask = WasiOpenFlags.CREAT.getValue();
      assertFalse(
          WasiOpenFlags.contains(mask, WasiOpenFlags.EXCL),
          "contains should return false when flag is not in mask");
    }

    @Test
    @DisplayName("contains should return false when mask is zero")
    void containsShouldReturnFalseWhenMaskIsZero() {
      assertFalse(
          WasiOpenFlags.contains(0, WasiOpenFlags.CREAT),
          "contains should return false when mask is zero");
    }

    @Test
    @DisplayName("contains should work with combined flags")
    void containsShouldWorkWithCombinedFlags() {
      final int mask = WasiOpenFlags.combine(WasiOpenFlags.CREAT, WasiOpenFlags.EXCL);
      assertTrue(
          WasiOpenFlags.contains(mask, WasiOpenFlags.CREAT), "Should find CREAT in combined mask");
      assertTrue(
          WasiOpenFlags.contains(mask, WasiOpenFlags.EXCL), "Should find EXCL in combined mask");
      assertFalse(
          WasiOpenFlags.contains(mask, WasiOpenFlags.TRUNC),
          "Should not find TRUNC in combined mask");
      assertFalse(
          WasiOpenFlags.contains(mask, WasiOpenFlags.DIRECTORY),
          "Should not find DIRECTORY in combined mask");
    }
  }

  @Nested
  @DisplayName("Bitmask Pattern Tests")
  class BitmaskPatternTests {

    @Test
    @DisplayName("Flag values should be powers of two for bitmask operations")
    void flagValuesShouldBePowersOfTwo() {
      for (WasiOpenFlags flag : WasiOpenFlags.values()) {
        final int value = flag.getValue();
        assertTrue(value > 0, "Flag value should be positive: " + flag.name());
        assertTrue(
            (value & (value - 1)) == 0,
            "Flag value should be power of 2 for bitwise operations: " + flag.name());
      }
    }

    @Test
    @DisplayName("Flags should not overlap")
    void flagsShouldNotOverlap() {
      final WasiOpenFlags[] flags = WasiOpenFlags.values();
      for (int i = 0; i < flags.length; i++) {
        for (int j = i + 1; j < flags.length; j++) {
          assertEquals(
              0,
              flags[i].getValue() & flags[j].getValue(),
              "Flags should not overlap: " + flags[i].name() + " and " + flags[j].name());
        }
      }
    }

    @Test
    @DisplayName("All flags combined should have all bits set")
    void allFlagsCombinedShouldHaveAllBitsSet() {
      final int combined = WasiOpenFlags.combine(WasiOpenFlags.values());
      for (WasiOpenFlags flag : WasiOpenFlags.values()) {
        assertTrue(
            WasiOpenFlags.contains(combined, flag), "Combined mask should contain: " + flag.name());
      }
    }
  }

  @Nested
  @DisplayName("Semantic Tests")
  class SemanticTests {

    @Test
    @DisplayName("CREAT flag should enable file creation")
    void creatFlagShouldEnableFileCreation() {
      // CREAT corresponds to O_CREAT in POSIX
      assertEquals("CREAT", WasiOpenFlags.CREAT.name());
      assertEquals(1, WasiOpenFlags.CREAT.getValue());
    }

    @Test
    @DisplayName("DIRECTORY flag should require directory")
    void directoryFlagShouldRequireDirectory() {
      // DIRECTORY corresponds to O_DIRECTORY in POSIX
      assertEquals("DIRECTORY", WasiOpenFlags.DIRECTORY.name());
      assertEquals(2, WasiOpenFlags.DIRECTORY.getValue());
    }

    @Test
    @DisplayName("EXCL flag should fail if file exists")
    void exclFlagShouldFailIfFileExists() {
      // EXCL corresponds to O_EXCL in POSIX
      assertEquals("EXCL", WasiOpenFlags.EXCL.name());
      assertEquals(4, WasiOpenFlags.EXCL.getValue());
    }

    @Test
    @DisplayName("TRUNC flag should truncate file")
    void truncFlagShouldTruncateFile() {
      // TRUNC corresponds to O_TRUNC in POSIX
      assertEquals("TRUNC", WasiOpenFlags.TRUNC.name());
      assertEquals(8, WasiOpenFlags.TRUNC.getValue());
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      final WasiOpenFlags flag = WasiOpenFlags.CREAT;

      final String result;
      switch (flag) {
        case CREAT:
          result = "create";
          break;
        case DIRECTORY:
          result = "directory";
          break;
        case EXCL:
          result = "exclusive";
          break;
        case TRUNC:
          result = "truncate";
          break;
        default:
          result = "unknown";
          break;
      }

      assertEquals("create", result, "Switch should work correctly");
    }

    @Test
    @DisplayName("All values should have non-null names")
    void allValuesShouldHaveNonNullNames() {
      for (WasiOpenFlags flag : WasiOpenFlags.values()) {
        assertNotNull(flag.name(), "Name should not be null: " + flag.ordinal());
        assertFalse(flag.name().isEmpty(), "Name should not be empty: " + flag.ordinal());
      }
    }

    @Test
    @DisplayName("Common flag combinations should work")
    void commonFlagCombinationsShouldWork() {
      // Create new file: CREAT | EXCL
      final int createNew = WasiOpenFlags.combine(WasiOpenFlags.CREAT, WasiOpenFlags.EXCL);
      assertEquals(5, createNew, "CREAT | EXCL should be 5");

      // Create or truncate: CREAT | TRUNC
      final int createOrTrunc = WasiOpenFlags.combine(WasiOpenFlags.CREAT, WasiOpenFlags.TRUNC);
      assertEquals(9, createOrTrunc, "CREAT | TRUNC should be 9");
    }
  }
}

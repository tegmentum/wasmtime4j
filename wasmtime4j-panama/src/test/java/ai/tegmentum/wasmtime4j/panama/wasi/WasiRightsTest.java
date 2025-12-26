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
 * Tests for the {@link WasiRights} enum.
 *
 * <p>This test class verifies WasiRights enum values and bitmask operations.
 */
@DisplayName("WasiRights Tests")
class WasiRightsTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasiRights should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiRights.class.isEnum(), "WasiRights should be an enum");
    }

    @Test
    @DisplayName("Should have expected number of values")
    void shouldHaveExpectedNumberOfValues() {
      assertEquals(29, WasiRights.values().length, "Should have 29 rights");
    }

    @Test
    @DisplayName("All enum values should have unique names")
    void allEnumValuesShouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (WasiRights right : WasiRights.values()) {
        assertTrue(names.add(right.name()), "Name should be unique: " + right.name());
      }
    }

    @Test
    @DisplayName("All enum values should have unique numeric values")
    void allEnumValuesShouldHaveUniqueValues() {
      final Set<Long> values = new HashSet<>();
      for (WasiRights right : WasiRights.values()) {
        assertTrue(values.add(right.getValue()), "Value should be unique: " + right.name());
      }
    }
  }

  @Nested
  @DisplayName("Value Tests")
  class ValueTests {

    @Test
    @DisplayName("FD_DATASYNC should have value 1")
    void fdDatasyncShouldHaveValue1() {
      assertEquals(1L, WasiRights.FD_DATASYNC.getValue(), "FD_DATASYNC should have value 1");
    }

    @Test
    @DisplayName("FD_READ should have value 2")
    void fdReadShouldHaveValue2() {
      assertEquals(2L, WasiRights.FD_READ.getValue(), "FD_READ should have value 2");
    }

    @Test
    @DisplayName("FD_SEEK should have value 4")
    void fdSeekShouldHaveValue4() {
      assertEquals(4L, WasiRights.FD_SEEK.getValue(), "FD_SEEK should have value 4");
    }

    @Test
    @DisplayName("FD_WRITE should have value 64")
    void fdWriteShouldHaveValue64() {
      assertEquals(64L, WasiRights.FD_WRITE.getValue(), "FD_WRITE should have value 64");
    }

    @Test
    @DisplayName("PATH_OPEN should have value 8192")
    void pathOpenShouldHaveValue8192() {
      assertEquals(8192L, WasiRights.PATH_OPEN.getValue(), "PATH_OPEN should have value 8192");
    }

    @Test
    @DisplayName("SOCK_SHUTDOWN should have last bit position")
    void sockShutdownShouldHaveLastBitPosition() {
      assertEquals(1L << 28, WasiRights.SOCK_SHUTDOWN.getValue(),
          "SOCK_SHUTDOWN should have value 1 << 28");
    }

    @Test
    @DisplayName("All rights should be powers of two")
    void allRightsShouldBePowersOfTwo() {
      for (WasiRights right : WasiRights.values()) {
        final long value = right.getValue();
        assertTrue(value > 0, "Right value should be positive: " + right.name());
        assertTrue((value & (value - 1)) == 0,
            "Right value should be power of 2 for bitwise operations: " + right.name());
      }
    }
  }

  @Nested
  @DisplayName("combine Tests")
  class CombineTests {

    @Test
    @DisplayName("combine with no rights should return 0")
    void combineWithNoRightsShouldReturnZero() {
      assertEquals(0L, WasiRights.combine(), "combine() with no args should return 0");
    }

    @Test
    @DisplayName("combine with single right should return right value")
    void combineWithSingleRightShouldReturnRightValue() {
      assertEquals(WasiRights.FD_READ.getValue(), WasiRights.combine(WasiRights.FD_READ),
          "combine(FD_READ) should return FD_READ value");
    }

    @Test
    @DisplayName("combine with multiple rights should return OR of values")
    void combineWithMultipleRightsShouldReturnOrOfValues() {
      // FD_READ (2) | FD_WRITE (64) = 66
      assertEquals(66L, WasiRights.combine(WasiRights.FD_READ, WasiRights.FD_WRITE),
          "combine(FD_READ, FD_WRITE) should return 66");
    }

    @Test
    @DisplayName("combine with duplicate rights should return same value")
    void combineWithDuplicateRightsShouldReturnSameValue() {
      final long single = WasiRights.combine(WasiRights.FD_READ);
      final long duplicate = WasiRights.combine(WasiRights.FD_READ, WasiRights.FD_READ);
      assertEquals(single, duplicate, "Combining same right twice should be idempotent");
    }

    @Test
    @DisplayName("combine with all rights should produce valid mask")
    void combineWithAllRightsShouldProduceValidMask() {
      final long allRights = WasiRights.combine(WasiRights.values());
      assertTrue(allRights > 0, "Combined rights should be positive");
      for (WasiRights right : WasiRights.values()) {
        assertTrue(WasiRights.contains(allRights, right),
            "Combined mask should contain: " + right.name());
      }
    }
  }

  @Nested
  @DisplayName("contains Tests")
  class ContainsTests {

    @Test
    @DisplayName("contains should return true when right is present")
    void containsShouldReturnTrueWhenRightIsPresent() {
      final long mask = WasiRights.FD_READ.getValue();
      assertTrue(WasiRights.contains(mask, WasiRights.FD_READ),
          "contains should return true when right is in mask");
    }

    @Test
    @DisplayName("contains should return false when right is not present")
    void containsShouldReturnFalseWhenRightIsNotPresent() {
      final long mask = WasiRights.FD_READ.getValue();
      assertFalse(WasiRights.contains(mask, WasiRights.FD_WRITE),
          "contains should return false when right is not in mask");
    }

    @Test
    @DisplayName("contains should return false when mask is zero")
    void containsShouldReturnFalseWhenMaskIsZero() {
      assertFalse(WasiRights.contains(0L, WasiRights.FD_READ),
          "contains should return false when mask is zero");
    }

    @Test
    @DisplayName("contains should work with combined rights")
    void containsShouldWorkWithCombinedRights() {
      final long mask = WasiRights.combine(WasiRights.FD_READ, WasiRights.FD_WRITE,
          WasiRights.FD_SEEK);
      assertTrue(WasiRights.contains(mask, WasiRights.FD_READ), "Should find FD_READ");
      assertTrue(WasiRights.contains(mask, WasiRights.FD_WRITE), "Should find FD_WRITE");
      assertTrue(WasiRights.contains(mask, WasiRights.FD_SEEK), "Should find FD_SEEK");
      assertFalse(WasiRights.contains(mask, WasiRights.FD_SYNC), "Should not find FD_SYNC");
      assertFalse(WasiRights.contains(mask, WasiRights.PATH_OPEN), "Should not find PATH_OPEN");
    }
  }

  @Nested
  @DisplayName("Bitmask Pattern Tests")
  class BitmaskPatternTests {

    @Test
    @DisplayName("Rights should not overlap")
    void rightsShouldNotOverlap() {
      final WasiRights[] rights = WasiRights.values();
      for (int i = 0; i < rights.length; i++) {
        for (int j = i + 1; j < rights.length; j++) {
          assertEquals(0L, rights[i].getValue() & rights[j].getValue(),
              "Rights should not overlap: " + rights[i].name() + " and " + rights[j].name());
        }
      }
    }

    @Test
    @DisplayName("Rights should use consecutive bit positions")
    void rightsShouldUseConsecutiveBitPositions() {
      // Check that rights use bits 0-28
      final Set<Integer> bitPositions = new HashSet<>();
      for (WasiRights right : WasiRights.values()) {
        final long value = right.getValue();
        final int bitPos = Long.numberOfTrailingZeros(value);
        bitPositions.add(bitPos);
      }
      assertEquals(29, bitPositions.size(), "Should have 29 unique bit positions");
      for (int i = 0; i < 29; i++) {
        assertTrue(bitPositions.contains(i), "Should have right at bit position: " + i);
      }
    }
  }

  @Nested
  @DisplayName("Right Category Tests")
  class RightCategoryTests {

    @Test
    @DisplayName("File descriptor rights should exist")
    void fileDescriptorRightsShouldExist() {
      assertNotNull(WasiRights.FD_DATASYNC);
      assertNotNull(WasiRights.FD_READ);
      assertNotNull(WasiRights.FD_SEEK);
      assertNotNull(WasiRights.FD_FDSTAT_SET_FLAGS);
      assertNotNull(WasiRights.FD_SYNC);
      assertNotNull(WasiRights.FD_TELL);
      assertNotNull(WasiRights.FD_WRITE);
      assertNotNull(WasiRights.FD_ADVISE);
      assertNotNull(WasiRights.FD_ALLOCATE);
      assertNotNull(WasiRights.FD_READDIR);
      assertNotNull(WasiRights.FD_FILESTAT_GET);
      assertNotNull(WasiRights.FD_FILESTAT_SET_SIZE);
      assertNotNull(WasiRights.FD_FILESTAT_SET_TIMES);
    }

    @Test
    @DisplayName("Path rights should exist")
    void pathRightsShouldExist() {
      assertNotNull(WasiRights.PATH_CREATE_DIRECTORY);
      assertNotNull(WasiRights.PATH_CREATE_FILE);
      assertNotNull(WasiRights.PATH_LINK_SOURCE);
      assertNotNull(WasiRights.PATH_LINK_TARGET);
      assertNotNull(WasiRights.PATH_OPEN);
      assertNotNull(WasiRights.PATH_READLINK);
      assertNotNull(WasiRights.PATH_RENAME_SOURCE);
      assertNotNull(WasiRights.PATH_RENAME_TARGET);
      assertNotNull(WasiRights.PATH_FILESTAT_GET);
      assertNotNull(WasiRights.PATH_FILESTAT_SET_SIZE);
      assertNotNull(WasiRights.PATH_FILESTAT_SET_TIMES);
      assertNotNull(WasiRights.PATH_SYMLINK);
      assertNotNull(WasiRights.PATH_REMOVE_DIRECTORY);
      assertNotNull(WasiRights.PATH_UNLINK_FILE);
    }

    @Test
    @DisplayName("Poll and socket rights should exist")
    void pollAndSocketRightsShouldExist() {
      assertNotNull(WasiRights.POLL_FD_READWRITE);
      assertNotNull(WasiRights.SOCK_SHUTDOWN);
    }
  }

  @Nested
  @DisplayName("Common Right Combinations Tests")
  class CommonRightCombinationsTests {

    @Test
    @DisplayName("Read-only file rights should work")
    void readOnlyFileRightsShouldWork() {
      final long readOnly = WasiRights.combine(
          WasiRights.FD_READ,
          WasiRights.FD_SEEK,
          WasiRights.FD_TELL,
          WasiRights.FD_FILESTAT_GET
      );
      assertTrue(WasiRights.contains(readOnly, WasiRights.FD_READ));
      assertTrue(WasiRights.contains(readOnly, WasiRights.FD_SEEK));
      assertFalse(WasiRights.contains(readOnly, WasiRights.FD_WRITE));
    }

    @Test
    @DisplayName("Write file rights should work")
    void writeFileRightsShouldWork() {
      final long writeRights = WasiRights.combine(
          WasiRights.FD_WRITE,
          WasiRights.FD_SEEK,
          WasiRights.FD_TELL,
          WasiRights.FD_DATASYNC,
          WasiRights.FD_SYNC
      );
      assertTrue(WasiRights.contains(writeRights, WasiRights.FD_WRITE));
      assertTrue(WasiRights.contains(writeRights, WasiRights.FD_SYNC));
      assertFalse(WasiRights.contains(writeRights, WasiRights.FD_READ));
    }

    @Test
    @DisplayName("Directory listing rights should work")
    void directoryListingRightsShouldWork() {
      final long dirRights = WasiRights.combine(
          WasiRights.FD_READDIR,
          WasiRights.PATH_OPEN,
          WasiRights.PATH_FILESTAT_GET
      );
      assertTrue(WasiRights.contains(dirRights, WasiRights.FD_READDIR));
      assertTrue(WasiRights.contains(dirRights, WasiRights.PATH_OPEN));
      assertFalse(WasiRights.contains(dirRights, WasiRights.PATH_UNLINK_FILE));
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      final WasiRights right = WasiRights.FD_READ;

      final String result;
      switch (right) {
        case FD_READ:
          result = "read";
          break;
        case FD_WRITE:
          result = "write";
          break;
        default:
          result = "other";
          break;
      }

      assertEquals("read", result, "Switch should work correctly");
    }

    @Test
    @DisplayName("All values should have non-null names")
    void allValuesShouldHaveNonNullNames() {
      for (WasiRights right : WasiRights.values()) {
        assertNotNull(right.name(), "Name should not be null: " + right.ordinal());
        assertFalse(right.name().isEmpty(), "Name should not be empty: " + right.ordinal());
      }
    }

    @Test
    @DisplayName("Round trip combine/contains should work")
    void roundTripCombineContainsShouldWork() {
      final WasiRights[] testRights = {WasiRights.FD_READ, WasiRights.FD_WRITE, WasiRights.PATH_OPEN};
      final long mask = WasiRights.combine(testRights);
      for (WasiRights right : testRights) {
        assertTrue(WasiRights.contains(mask, right),
            "Should find right after combining: " + right.name());
      }
    }
  }
}

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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiRights} enum.
 *
 * <p>WasiRights defines file descriptor rights constants for WASI access control.
 */
@DisplayName("WasiRights Enum Tests")
class WasiRightsTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have all expected rights")
    void shouldHaveAllExpectedRights() {
      assertNotNull(WasiRights.FD_DATASYNC, "Should have FD_DATASYNC");
      assertNotNull(WasiRights.FD_READ, "Should have FD_READ");
      assertNotNull(WasiRights.FD_SEEK, "Should have FD_SEEK");
      assertNotNull(WasiRights.FD_WRITE, "Should have FD_WRITE");
      assertNotNull(WasiRights.PATH_OPEN, "Should have PATH_OPEN");
      assertNotNull(WasiRights.PATH_CREATE_DIRECTORY, "Should have PATH_CREATE_DIRECTORY");
      assertNotNull(WasiRights.PATH_SYMLINK, "Should have PATH_SYMLINK");
      assertNotNull(WasiRights.SOCK_SHUTDOWN, "Should have SOCK_SHUTDOWN");
    }

    @Test
    @DisplayName("should have exactly 29 rights")
    void shouldHaveExactlyTwentyNineRights() {
      final WasiRights[] values = WasiRights.values();
      assertEquals(29, values.length, "Should have exactly 29 rights");
    }
  }

  @Nested
  @DisplayName("Numeric Value Tests")
  class NumericValueTests {

    @Test
    @DisplayName("FD_DATASYNC should have value 1 (1 << 0)")
    void fdDatasyncShouldHaveValueOne() {
      assertEquals(1L, WasiRights.FD_DATASYNC.getValue());
    }

    @Test
    @DisplayName("FD_READ should have value 2 (1 << 1)")
    void fdReadShouldHaveValueTwo() {
      assertEquals(2L, WasiRights.FD_READ.getValue());
    }

    @Test
    @DisplayName("FD_SEEK should have value 4 (1 << 2)")
    void fdSeekShouldHaveValueFour() {
      assertEquals(4L, WasiRights.FD_SEEK.getValue());
    }

    @Test
    @DisplayName("FD_WRITE should have value 64 (1 << 6)")
    void fdWriteShouldHaveValue64() {
      assertEquals(64L, WasiRights.FD_WRITE.getValue());
    }

    @Test
    @DisplayName("PATH_OPEN should have value 8192 (1 << 13)")
    void pathOpenShouldHaveValue8192() {
      assertEquals(8192L, WasiRights.PATH_OPEN.getValue());
    }

    @Test
    @DisplayName("values should be powers of 2 for bitwise operations")
    void valuesShouldBePowersOfTwo() {
      for (final WasiRights right : WasiRights.values()) {
        final long value = right.getValue();
        assertTrue(value > 0 && (value & (value - 1)) == 0,
            right.name() + " value should be a power of 2");
      }
    }

    @Test
    @DisplayName("all rights should have unique values")
    void allRightsShouldHaveUniqueValues() {
      final WasiRights[] rights = WasiRights.values();
      for (int i = 0; i < rights.length; i++) {
        for (int j = i + 1; j < rights.length; j++) {
          assertFalse(rights[i].getValue() == rights[j].getValue(),
              rights[i].name() + " and " + rights[j].name() + " should have different values");
        }
      }
    }
  }

  @Nested
  @DisplayName("Combine Method Tests")
  class CombineMethodTests {

    @Test
    @DisplayName("combine with no rights should return 0")
    void combineWithNoRightsShouldReturnZero() {
      assertEquals(0L, WasiRights.combine());
    }

    @Test
    @DisplayName("combine with single right should return that right's value")
    void combineWithSingleRightShouldReturnRightValue() {
      assertEquals(1L, WasiRights.combine(WasiRights.FD_DATASYNC));
      assertEquals(2L, WasiRights.combine(WasiRights.FD_READ));
      assertEquals(64L, WasiRights.combine(WasiRights.FD_WRITE));
    }

    @Test
    @DisplayName("combine with multiple rights should OR their values")
    void combineWithMultipleRightsShouldOrValues() {
      final long combined = WasiRights.combine(WasiRights.FD_READ, WasiRights.FD_WRITE);
      assertEquals(66L, combined, "FD_READ(2) | FD_WRITE(64) should be 66");
    }

    @Test
    @DisplayName("combine read/write/seek rights")
    void combineReadWriteSeekRights() {
      final long combined = WasiRights.combine(
          WasiRights.FD_READ,
          WasiRights.FD_WRITE,
          WasiRights.FD_SEEK);
      assertEquals(70L, combined, "FD_READ(2) | FD_WRITE(64) | FD_SEEK(4) should be 70");
    }
  }

  @Nested
  @DisplayName("Contains Method Tests")
  class ContainsMethodTests {

    @Test
    @DisplayName("contains should return true when right is present")
    void containsShouldReturnTrueWhenRightPresent() {
      final long mask = WasiRights.combine(WasiRights.FD_READ, WasiRights.FD_WRITE);
      assertTrue(WasiRights.contains(mask, WasiRights.FD_READ));
      assertTrue(WasiRights.contains(mask, WasiRights.FD_WRITE));
    }

    @Test
    @DisplayName("contains should return false when right is absent")
    void containsShouldReturnFalseWhenRightAbsent() {
      final long mask = WasiRights.combine(WasiRights.FD_READ, WasiRights.FD_WRITE);
      assertFalse(WasiRights.contains(mask, WasiRights.FD_SEEK));
      assertFalse(WasiRights.contains(mask, WasiRights.PATH_OPEN));
    }

    @Test
    @DisplayName("contains should return false for empty mask")
    void containsShouldReturnFalseForEmptyMask() {
      assertFalse(WasiRights.contains(0L, WasiRights.FD_READ));
      assertFalse(WasiRights.contains(0L, WasiRights.FD_WRITE));
    }
  }

  @Nested
  @DisplayName("Round Trip Tests")
  class RoundTripTests {

    @Test
    @DisplayName("combine and contains should round-trip correctly")
    void combineAndContainsShouldRoundTripCorrectly() {
      for (final WasiRights right : WasiRights.values()) {
        final long mask = WasiRights.combine(right);
        assertTrue(WasiRights.contains(mask, right),
            "Round trip should work for " + right.name());
      }
    }
  }

  @Nested
  @DisplayName("Rights Category Tests")
  class RightsCategoryTests {

    @Test
    @DisplayName("should have file descriptor rights")
    void shouldHaveFileDescriptorRights() {
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
    @DisplayName("should have path rights")
    void shouldHavePathRights() {
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
    @DisplayName("should have socket rights")
    void shouldHaveSocketRights() {
      assertNotNull(WasiRights.SOCK_SHUTDOWN);
    }

    @Test
    @DisplayName("should have poll rights")
    void shouldHavePollRights() {
      assertNotNull(WasiRights.POLL_FD_READWRITE);
    }
  }
}

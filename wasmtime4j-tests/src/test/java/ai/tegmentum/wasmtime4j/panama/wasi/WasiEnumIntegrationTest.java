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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WASI enum types.
 *
 * <p>These tests exercise actual code execution for WasiClockId, WasiFileType, and other WASI enums
 * to improve JaCoCo coverage.
 */
@DisplayName("WASI Enum Integration Tests")
public class WasiEnumIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WasiEnumIntegrationTest.class.getName());

  @Nested
  @DisplayName("WasiClockId Tests")
  class WasiClockIdTests {

    @Test
    @DisplayName("Should have correct enum values")
    void shouldHaveCorrectEnumValues() {
      LOGGER.info("Testing WasiClockId enum values");

      assertEquals(0, WasiClockId.REALTIME.getValue(), "REALTIME should be 0");
      assertEquals(1, WasiClockId.MONOTONIC.getValue(), "MONOTONIC should be 1");
      assertEquals(2, WasiClockId.PROCESS_CPUTIME_ID.getValue(), "PROCESS_CPUTIME_ID should be 2");
      assertEquals(3, WasiClockId.THREAD_CPUTIME_ID.getValue(), "THREAD_CPUTIME_ID should be 3");

      LOGGER.info("WasiClockId values verified");
    }

    @Test
    @DisplayName("Should find clock ID by value")
    void shouldFindClockIdByValue() {
      LOGGER.info("Testing WasiClockId.fromValue");

      assertEquals(
          WasiClockId.REALTIME, WasiClockId.fromValue(0), "Value 0 should return REALTIME");
      assertEquals(
          WasiClockId.MONOTONIC, WasiClockId.fromValue(1), "Value 1 should return MONOTONIC");
      assertEquals(
          WasiClockId.PROCESS_CPUTIME_ID,
          WasiClockId.fromValue(2),
          "Value 2 should return PROCESS_CPUTIME_ID");
      assertEquals(
          WasiClockId.THREAD_CPUTIME_ID,
          WasiClockId.fromValue(3),
          "Value 3 should return THREAD_CPUTIME_ID");

      LOGGER.info("WasiClockId.fromValue verified");
    }

    @Test
    @DisplayName("Should throw exception for invalid clock ID")
    void shouldThrowExceptionForInvalidClockId() {
      LOGGER.info("Testing invalid clock ID");

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> WasiClockId.fromValue(99));

      assertTrue(
          ex.getMessage().contains("Invalid clock ID"),
          "Error should mention invalid clock ID: " + ex.getMessage());

      LOGGER.info("Correctly threw exception: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should iterate all clock IDs")
    void shouldIterateAllClockIds() {
      LOGGER.info("Testing all clock IDs");

      assertEquals(4, WasiClockId.values().length, "Should have 4 clock IDs");

      for (final WasiClockId clockId : WasiClockId.values()) {
        final int value = clockId.getValue();
        final WasiClockId found = WasiClockId.fromValue(value);
        assertEquals(clockId, found, "Should find " + clockId + " by value " + value);
        LOGGER.fine("Verified: " + clockId + " = " + value);
      }

      LOGGER.info("All clock IDs verified");
    }
  }

  @Nested
  @DisplayName("WasiFileType Tests")
  class WasiFileTypeTests {

    @Test
    @DisplayName("Should have correct enum values")
    void shouldHaveCorrectEnumValues() {
      LOGGER.info("Testing WasiFileType enum values");

      assertEquals(0, WasiFileType.UNKNOWN.getValue(), "UNKNOWN should be 0");
      assertEquals(1, WasiFileType.BLOCK_DEVICE.getValue(), "BLOCK_DEVICE should be 1");
      assertEquals(2, WasiFileType.CHARACTER_DEVICE.getValue(), "CHARACTER_DEVICE should be 2");
      assertEquals(3, WasiFileType.DIRECTORY.getValue(), "DIRECTORY should be 3");
      assertEquals(4, WasiFileType.REGULAR_FILE.getValue(), "REGULAR_FILE should be 4");
      assertEquals(5, WasiFileType.SOCKET_DGRAM.getValue(), "SOCKET_DGRAM should be 5");
      assertEquals(6, WasiFileType.SOCKET_STREAM.getValue(), "SOCKET_STREAM should be 6");
      assertEquals(7, WasiFileType.SYMBOLIC_LINK.getValue(), "SYMBOLIC_LINK should be 7");

      LOGGER.info("WasiFileType values verified");
    }

    @Test
    @DisplayName("Should find file type by value")
    void shouldFindFileTypeByValue() {
      LOGGER.info("Testing WasiFileType.fromValue");

      assertEquals(
          WasiFileType.UNKNOWN, WasiFileType.fromValue(0), "Value 0 should return UNKNOWN");
      assertEquals(
          WasiFileType.BLOCK_DEVICE,
          WasiFileType.fromValue(1),
          "Value 1 should return BLOCK_DEVICE");
      assertEquals(
          WasiFileType.CHARACTER_DEVICE,
          WasiFileType.fromValue(2),
          "Value 2 should return CHARACTER_DEVICE");
      assertEquals(
          WasiFileType.DIRECTORY, WasiFileType.fromValue(3), "Value 3 should return DIRECTORY");
      assertEquals(
          WasiFileType.REGULAR_FILE,
          WasiFileType.fromValue(4),
          "Value 4 should return REGULAR_FILE");
      assertEquals(
          WasiFileType.SOCKET_DGRAM,
          WasiFileType.fromValue(5),
          "Value 5 should return SOCKET_DGRAM");
      assertEquals(
          WasiFileType.SOCKET_STREAM,
          WasiFileType.fromValue(6),
          "Value 6 should return SOCKET_STREAM");
      assertEquals(
          WasiFileType.SYMBOLIC_LINK,
          WasiFileType.fromValue(7),
          "Value 7 should return SYMBOLIC_LINK");

      LOGGER.info("WasiFileType.fromValue verified");
    }

    @Test
    @DisplayName("Should throw exception for invalid file type")
    void shouldThrowExceptionForInvalidFileType() {
      LOGGER.info("Testing invalid file type");

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> WasiFileType.fromValue(99));

      assertTrue(
          ex.getMessage().contains("Invalid file type"),
          "Error should mention invalid file type: " + ex.getMessage());

      LOGGER.info("Correctly threw exception: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should identify regular files")
    void shouldIdentifyRegularFiles() {
      LOGGER.info("Testing isRegularFile method");

      assertTrue(
          WasiFileType.REGULAR_FILE.isRegularFile(), "REGULAR_FILE.isRegularFile() should be true");
      assertFalse(
          WasiFileType.DIRECTORY.isRegularFile(), "DIRECTORY.isRegularFile() should be false");
      assertFalse(
          WasiFileType.SYMBOLIC_LINK.isRegularFile(),
          "SYMBOLIC_LINK.isRegularFile() should be false");
      assertFalse(WasiFileType.UNKNOWN.isRegularFile(), "UNKNOWN.isRegularFile() should be false");

      LOGGER.info("isRegularFile method verified");
    }

    @Test
    @DisplayName("Should identify directories")
    void shouldIdentifyDirectories() {
      LOGGER.info("Testing isDirectory method");

      assertTrue(WasiFileType.DIRECTORY.isDirectory(), "DIRECTORY.isDirectory() should be true");
      assertFalse(
          WasiFileType.REGULAR_FILE.isDirectory(), "REGULAR_FILE.isDirectory() should be false");
      assertFalse(
          WasiFileType.SYMBOLIC_LINK.isDirectory(), "SYMBOLIC_LINK.isDirectory() should be false");
      assertFalse(WasiFileType.UNKNOWN.isDirectory(), "UNKNOWN.isDirectory() should be false");

      LOGGER.info("isDirectory method verified");
    }

    @Test
    @DisplayName("Should identify symbolic links")
    void shouldIdentifySymbolicLinks() {
      LOGGER.info("Testing isSymbolicLink method");

      assertTrue(
          WasiFileType.SYMBOLIC_LINK.isSymbolicLink(),
          "SYMBOLIC_LINK.isSymbolicLink() should be true");
      assertFalse(
          WasiFileType.REGULAR_FILE.isSymbolicLink(),
          "REGULAR_FILE.isSymbolicLink() should be false");
      assertFalse(
          WasiFileType.DIRECTORY.isSymbolicLink(), "DIRECTORY.isSymbolicLink() should be false");
      assertFalse(
          WasiFileType.UNKNOWN.isSymbolicLink(), "UNKNOWN.isSymbolicLink() should be false");

      LOGGER.info("isSymbolicLink method verified");
    }

    @Test
    @DisplayName("Should iterate all file types")
    void shouldIterateAllFileTypes() {
      LOGGER.info("Testing all file types");

      assertEquals(8, WasiFileType.values().length, "Should have 8 file types");

      for (final WasiFileType fileType : WasiFileType.values()) {
        final int value = fileType.getValue();
        final WasiFileType found = WasiFileType.fromValue(value);
        assertEquals(fileType, found, "Should find " + fileType + " by value " + value);

        // Test all boolean methods
        final boolean isRegular = fileType.isRegularFile();
        final boolean isDir = fileType.isDirectory();
        final boolean isSymlink = fileType.isSymbolicLink();

        LOGGER.fine(
            "Verified: "
                + fileType
                + " = "
                + value
                + ", isRegular="
                + isRegular
                + ", isDir="
                + isDir
                + ", isSymlink="
                + isSymlink);
      }

      LOGGER.info("All file types verified");
    }

    @Test
    @DisplayName("Should have mutually exclusive type checks")
    void shouldHaveMutuallyExclusiveTypeChecks() {
      LOGGER.info("Testing mutual exclusivity of type checks");

      for (final WasiFileType fileType : WasiFileType.values()) {
        int trueCount = 0;
        if (fileType.isRegularFile()) {
          trueCount++;
        }
        if (fileType.isDirectory()) {
          trueCount++;
        }
        if (fileType.isSymbolicLink()) {
          trueCount++;
        }

        // At most one should be true
        assertTrue(trueCount <= 1, fileType + " should have at most one type check return true");
      }

      LOGGER.info("Mutual exclusivity verified");
    }
  }

  @Nested
  @DisplayName("WasiEventType Tests")
  class WasiEventTypeTests {

    @Test
    @DisplayName("Should have correct enum values")
    void shouldHaveCorrectEnumValues() {
      LOGGER.info("Testing WasiEventType enum values");

      assertEquals(0, WasiEventType.CLOCK.getValue(), "CLOCK should be 0");
      assertEquals(1, WasiEventType.FD_READ.getValue(), "FD_READ should be 1");
      assertEquals(2, WasiEventType.FD_WRITE.getValue(), "FD_WRITE should be 2");

      LOGGER.info("WasiEventType values verified");
    }

    @Test
    @DisplayName("Should find event type by value")
    void shouldFindEventTypeByValue() {
      LOGGER.info("Testing WasiEventType.fromValue");

      assertEquals(WasiEventType.CLOCK, WasiEventType.fromValue(0), "Value 0 should return CLOCK");
      assertEquals(
          WasiEventType.FD_READ, WasiEventType.fromValue(1), "Value 1 should return FD_READ");
      assertEquals(
          WasiEventType.FD_WRITE, WasiEventType.fromValue(2), "Value 2 should return FD_WRITE");

      LOGGER.info("WasiEventType.fromValue verified");
    }

    @Test
    @DisplayName("Should throw exception for invalid event type")
    void shouldThrowExceptionForInvalidEventType() {
      LOGGER.info("Testing invalid event type");

      assertThrows(IllegalArgumentException.class, () -> WasiEventType.fromValue(99));

      LOGGER.info("Correctly threw exception for invalid event type");
    }
  }

  @Nested
  @DisplayName("WasiWhence Tests")
  class WasiWhenceTests {

    @Test
    @DisplayName("Should have correct enum values")
    void shouldHaveCorrectEnumValues() {
      LOGGER.info("Testing WasiWhence enum values");

      assertEquals(0, WasiWhence.SET.getValue(), "SET should be 0");
      assertEquals(1, WasiWhence.CUR.getValue(), "CUR should be 1");
      assertEquals(2, WasiWhence.END.getValue(), "END should be 2");

      LOGGER.info("WasiWhence values verified");
    }

    @Test
    @DisplayName("Should find whence by value")
    void shouldFindWhenceByValue() {
      LOGGER.info("Testing WasiWhence.fromValue");

      assertEquals(WasiWhence.SET, WasiWhence.fromValue(0), "Value 0 should return SET");
      assertEquals(WasiWhence.CUR, WasiWhence.fromValue(1), "Value 1 should return CUR");
      assertEquals(WasiWhence.END, WasiWhence.fromValue(2), "Value 2 should return END");

      LOGGER.info("WasiWhence.fromValue verified");
    }

    @Test
    @DisplayName("Should throw exception for invalid whence")
    void shouldThrowExceptionForInvalidWhence() {
      LOGGER.info("Testing invalid whence");

      assertThrows(IllegalArgumentException.class, () -> WasiWhence.fromValue(99));

      LOGGER.info("Correctly threw exception for invalid whence");
    }
  }

  @Nested
  @DisplayName("WasiOpenFlags Tests")
  class WasiOpenFlagsTests {

    @Test
    @DisplayName("Should have correct enum values")
    void shouldHaveCorrectEnumValues() {
      LOGGER.info("Testing WasiOpenFlags enum values");

      assertEquals(1, WasiOpenFlags.CREAT.getValue(), "CREAT should be 1");
      assertEquals(2, WasiOpenFlags.DIRECTORY.getValue(), "DIRECTORY should be 2");
      assertEquals(4, WasiOpenFlags.EXCL.getValue(), "EXCL should be 4");
      assertEquals(8, WasiOpenFlags.TRUNC.getValue(), "TRUNC should be 8");

      LOGGER.info("WasiOpenFlags values verified");
    }

    @Test
    @DisplayName("Should combine flags")
    void shouldCombineFlags() {
      LOGGER.info("Testing WasiOpenFlags.combine");

      final int combined =
          WasiOpenFlags.combine(WasiOpenFlags.CREAT, WasiOpenFlags.EXCL, WasiOpenFlags.TRUNC);
      assertEquals(1 | 4 | 8, combined, "Combined flags should be CREAT|EXCL|TRUNC");

      final int single = WasiOpenFlags.combine(WasiOpenFlags.DIRECTORY);
      assertEquals(2, single, "Single flag should be DIRECTORY");

      final int empty = WasiOpenFlags.combine();
      assertEquals(0, empty, "Empty combine should be 0");

      LOGGER.info("WasiOpenFlags.combine verified");
    }

    @Test
    @DisplayName("Should check if mask contains flag")
    void shouldCheckIfMaskContainsFlag() {
      LOGGER.info("Testing WasiOpenFlags.contains");

      final int mask = WasiOpenFlags.combine(WasiOpenFlags.CREAT, WasiOpenFlags.TRUNC);

      assertTrue(WasiOpenFlags.contains(mask, WasiOpenFlags.CREAT), "Mask should contain CREAT");
      assertTrue(WasiOpenFlags.contains(mask, WasiOpenFlags.TRUNC), "Mask should contain TRUNC");
      assertFalse(
          WasiOpenFlags.contains(mask, WasiOpenFlags.DIRECTORY),
          "Mask should not contain DIRECTORY");
      assertFalse(WasiOpenFlags.contains(mask, WasiOpenFlags.EXCL), "Mask should not contain EXCL");

      LOGGER.info("WasiOpenFlags.contains verified");
    }
  }

  @Nested
  @DisplayName("WasiRights Tests")
  class WasiRightsTests {

    @Test
    @DisplayName("Should have correct enum values")
    void shouldHaveCorrectEnumValues() {
      LOGGER.info("Testing WasiRights enum values");

      assertEquals(1L << 0, WasiRights.FD_DATASYNC.getValue(), "FD_DATASYNC should be 1<<0");
      assertEquals(1L << 1, WasiRights.FD_READ.getValue(), "FD_READ should be 1<<1");
      assertEquals(1L << 2, WasiRights.FD_SEEK.getValue(), "FD_SEEK should be 1<<2");
      assertEquals(1L << 6, WasiRights.FD_WRITE.getValue(), "FD_WRITE should be 1<<6");
      assertEquals(1L << 13, WasiRights.PATH_OPEN.getValue(), "PATH_OPEN should be 1<<13");

      LOGGER.info("WasiRights values verified");
    }

    @Test
    @DisplayName("Should combine rights")
    void shouldCombineRights() {
      LOGGER.info("Testing WasiRights.combine");

      final long combined =
          WasiRights.combine(WasiRights.FD_READ, WasiRights.FD_WRITE, WasiRights.FD_SEEK);
      assertEquals(
          (1L << 1) | (1L << 6) | (1L << 2),
          combined,
          "Combined rights should be FD_READ|FD_WRITE|FD_SEEK");

      final long single = WasiRights.combine(WasiRights.PATH_OPEN);
      assertEquals(1L << 13, single, "Single right should be PATH_OPEN");

      final long empty = WasiRights.combine();
      assertEquals(0L, empty, "Empty combine should be 0");

      LOGGER.info("WasiRights.combine verified");
    }

    @Test
    @DisplayName("Should check if mask contains right")
    void shouldCheckIfMaskContainsRight() {
      LOGGER.info("Testing WasiRights.contains");

      final long mask = WasiRights.combine(WasiRights.FD_READ, WasiRights.FD_WRITE);

      assertTrue(WasiRights.contains(mask, WasiRights.FD_READ), "Mask should contain FD_READ");
      assertTrue(WasiRights.contains(mask, WasiRights.FD_WRITE), "Mask should contain FD_WRITE");
      assertFalse(WasiRights.contains(mask, WasiRights.FD_SEEK), "Mask should not contain FD_SEEK");
      assertFalse(
          WasiRights.contains(mask, WasiRights.PATH_OPEN), "Mask should not contain PATH_OPEN");

      LOGGER.info("WasiRights.contains verified");
    }

    @Test
    @DisplayName("Should iterate all rights")
    void shouldIterateAllRights() {
      LOGGER.info("Testing all rights");

      assertEquals(29, WasiRights.values().length, "Should have 29 rights");

      for (final WasiRights right : WasiRights.values()) {
        final long value = right.getValue();
        assertTrue(value > 0, "Right value should be positive: " + right);
        LOGGER.fine("Verified: " + right + " = " + value);
      }

      LOGGER.info("All rights verified");
    }
  }

  @Nested
  @DisplayName("WasiEventRwFlags Tests")
  class WasiEventRwFlagsTests {

    @Test
    @DisplayName("Should have correct enum values")
    void shouldHaveCorrectEnumValues() {
      LOGGER.info("Testing WasiEventRwFlags enum values");

      assertEquals(
          1, WasiEventRwFlags.FD_READWRITE_HANGUP.getValue(), "FD_READWRITE_HANGUP should be 1");

      LOGGER.info("WasiEventRwFlags values verified");
    }

    @Test
    @DisplayName("Should combine flags")
    void shouldCombineFlags() {
      LOGGER.info("Testing WasiEventRwFlags.combine");

      final int combined = WasiEventRwFlags.combine(WasiEventRwFlags.FD_READWRITE_HANGUP);
      assertEquals(1, combined, "Combined flags should be FD_READWRITE_HANGUP");

      final int empty = WasiEventRwFlags.combine();
      assertEquals(0, empty, "Empty combine should be 0");

      LOGGER.info("WasiEventRwFlags.combine verified");
    }

    @Test
    @DisplayName("Should check if mask contains flag")
    void shouldCheckIfMaskContainsFlag() {
      LOGGER.info("Testing WasiEventRwFlags.contains");

      final int mask = WasiEventRwFlags.combine(WasiEventRwFlags.FD_READWRITE_HANGUP);

      assertTrue(
          WasiEventRwFlags.contains(mask, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "Mask should contain FD_READWRITE_HANGUP");
      assertFalse(
          WasiEventRwFlags.contains(0, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "Empty mask should not contain FD_READWRITE_HANGUP");

      LOGGER.info("WasiEventRwFlags.contains verified");
    }
  }
}

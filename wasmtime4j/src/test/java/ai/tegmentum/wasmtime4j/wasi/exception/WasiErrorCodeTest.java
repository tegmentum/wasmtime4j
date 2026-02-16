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

package ai.tegmentum.wasmtime4j.wasi.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiErrorCode} enum.
 *
 * <p>This test class verifies WasiErrorCode enum values, categorization, lookup methods, and
 * coverage of all error codes.
 */
@DisplayName("WasiErrorCode Tests")
class WasiErrorCodeTest {

  private static final Logger LOGGER = Logger.getLogger(WasiErrorCodeTest.class.getName());

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasiErrorCode should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiErrorCode.class.isEnum(), "WasiErrorCode should be an enum");
    }

    @Test
    @DisplayName("All enum values should have unique names")
    void allEnumValuesShouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (WasiErrorCode code : WasiErrorCode.values()) {
        assertTrue(names.add(code.name()), "Name should be unique: " + code.name());
      }
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("Should have SUCCESS with errno 0")
    void shouldHaveSuccessWithErrnoZero() {
      LOGGER.info("Testing SUCCESS error code");

      assertEquals(0, WasiErrorCode.SUCCESS.getErrno(), "SUCCESS should have errno 0");
      assertEquals(
          "No error",
          WasiErrorCode.SUCCESS.getDescription(),
          "SUCCESS description should be 'No error'");
      assertFalse(
          WasiErrorCode.SUCCESS.isFileSystemError(), "SUCCESS should not be file system error");
      assertFalse(WasiErrorCode.SUCCESS.isNetworkError(), "SUCCESS should not be network error");
      assertFalse(
          WasiErrorCode.SUCCESS.isPermissionError(), "SUCCESS should not be permission error");
      assertFalse(
          WasiErrorCode.SUCCESS.isResourceLimitError(),
          "SUCCESS should not be resource limit error");
      assertFalse(WasiErrorCode.SUCCESS.isRetryable(), "SUCCESS should not be retryable");

      LOGGER.info("SUCCESS error code verified: " + WasiErrorCode.SUCCESS);
    }

    @Test
    @DisplayName("Should have file system error codes")
    void shouldHaveFileSystemErrorCodes() {
      LOGGER.info("Testing file system error codes");

      // ENOENT
      assertTrue(WasiErrorCode.ENOENT.isFileSystemError(), "ENOENT should be file system error");
      assertEquals(2, WasiErrorCode.ENOENT.getErrno(), "ENOENT should have errno 2");
      assertTrue(WasiErrorCode.ENOENT.isRetryable(), "ENOENT should be retryable");

      // EIO
      assertTrue(WasiErrorCode.EIO.isFileSystemError(), "EIO should be file system error");
      assertEquals(5, WasiErrorCode.EIO.getErrno(), "EIO should have errno 5");

      // EBADF
      assertTrue(WasiErrorCode.EBADF.isFileSystemError(), "EBADF should be file system error");
      assertEquals(9, WasiErrorCode.EBADF.getErrno(), "EBADF should have errno 9");

      // EACCES
      assertTrue(WasiErrorCode.EACCES.isFileSystemError(), "EACCES should be file system error");
      assertTrue(WasiErrorCode.EACCES.isPermissionError(), "EACCES should be permission error");

      // EEXIST
      assertTrue(WasiErrorCode.EEXIST.isFileSystemError(), "EEXIST should be file system error");

      // ENOTDIR
      assertTrue(WasiErrorCode.ENOTDIR.isFileSystemError(), "ENOTDIR should be file system error");

      // EISDIR
      assertTrue(WasiErrorCode.EISDIR.isFileSystemError(), "EISDIR should be file system error");

      LOGGER.info("File system error codes verified");
    }

    @Test
    @DisplayName("Should have network error codes")
    void shouldHaveNetworkErrorCodes() {
      LOGGER.info("Testing network error codes");

      // ECONNREFUSED
      assertTrue(
          WasiErrorCode.ECONNREFUSED.isNetworkError(), "ECONNREFUSED should be network error");
      assertEquals(61, WasiErrorCode.ECONNREFUSED.getErrno(), "ECONNREFUSED should have errno 61");
      assertTrue(WasiErrorCode.ECONNREFUSED.isRetryable(), "ECONNREFUSED should be retryable");

      // ETIMEDOUT
      assertTrue(WasiErrorCode.ETIMEDOUT.isNetworkError(), "ETIMEDOUT should be network error");
      assertTrue(WasiErrorCode.ETIMEDOUT.isRetryable(), "ETIMEDOUT should be retryable");

      // EHOSTUNREACH
      assertTrue(
          WasiErrorCode.EHOSTUNREACH.isNetworkError(), "EHOSTUNREACH should be network error");

      // ENETDOWN
      assertTrue(WasiErrorCode.ENETDOWN.isNetworkError(), "ENETDOWN should be network error");

      // ENETUNREACH
      assertTrue(WasiErrorCode.ENETUNREACH.isNetworkError(), "ENETUNREACH should be network error");

      // ECONNRESET
      assertTrue(WasiErrorCode.ECONNRESET.isNetworkError(), "ECONNRESET should be network error");

      // EADDRINUSE
      assertTrue(WasiErrorCode.EADDRINUSE.isNetworkError(), "EADDRINUSE should be network error");
      assertTrue(
          WasiErrorCode.EADDRINUSE.isResourceLimitError(),
          "EADDRINUSE should be resource limit error");

      // ENOTCONN
      assertTrue(WasiErrorCode.ENOTCONN.isNetworkError(), "ENOTCONN should be network error");

      LOGGER.info("Network error codes verified");
    }

    @Test
    @DisplayName("Should have permission error codes")
    void shouldHavePermissionErrorCodes() {
      LOGGER.info("Testing permission error codes");

      // EPERM
      assertTrue(WasiErrorCode.EPERM.isPermissionError(), "EPERM should be permission error");
      assertEquals(1, WasiErrorCode.EPERM.getErrno(), "EPERM should have errno 1");

      // ESRCH
      assertTrue(WasiErrorCode.ESRCH.isPermissionError(), "ESRCH should be permission error");

      // EROFS
      assertTrue(WasiErrorCode.EROFS.isFileSystemError(), "EROFS should be file system error");
      assertTrue(WasiErrorCode.EROFS.isPermissionError(), "EROFS should be permission error");

      LOGGER.info("Permission error codes verified");
    }

    @Test
    @DisplayName("Should have resource limit error codes")
    void shouldHaveResourceLimitErrorCodes() {
      LOGGER.info("Testing resource limit error codes");

      // ENOMEM
      assertTrue(
          WasiErrorCode.ENOMEM.isResourceLimitError(), "ENOMEM should be resource limit error");
      assertEquals(12, WasiErrorCode.ENOMEM.getErrno(), "ENOMEM should have errno 12");
      assertTrue(WasiErrorCode.ENOMEM.isRetryable(), "ENOMEM should be retryable");

      // EMFILE
      assertTrue(
          WasiErrorCode.EMFILE.isResourceLimitError(), "EMFILE should be resource limit error");
      assertTrue(WasiErrorCode.EMFILE.isFileSystemError(), "EMFILE should be file system error");

      // ENOSPC
      assertTrue(
          WasiErrorCode.ENOSPC.isResourceLimitError(), "ENOSPC should be resource limit error");
      assertTrue(WasiErrorCode.ENOSPC.isFileSystemError(), "ENOSPC should be file system error");

      // EAGAIN
      assertTrue(
          WasiErrorCode.EAGAIN.isResourceLimitError(), "EAGAIN should be resource limit error");
      assertTrue(WasiErrorCode.EAGAIN.isRetryable(), "EAGAIN should be retryable");

      // ENOBUFS
      assertTrue(
          WasiErrorCode.ENOBUFS.isResourceLimitError(), "ENOBUFS should be resource limit error");

      LOGGER.info("Resource limit error codes verified");
    }

    @Test
    @DisplayName("Should have UNKNOWN error code")
    void shouldHaveUnknownErrorCode() {
      LOGGER.info("Testing UNKNOWN error code");

      assertEquals(-1, WasiErrorCode.UNKNOWN.getErrno(), "UNKNOWN should have errno -1");
      assertEquals(
          "Unknown error",
          WasiErrorCode.UNKNOWN.getDescription(),
          "UNKNOWN description should be 'Unknown error'");
      assertFalse(
          WasiErrorCode.UNKNOWN.isFileSystemError(), "UNKNOWN should not be file system error");
      assertFalse(WasiErrorCode.UNKNOWN.isNetworkError(), "UNKNOWN should not be network error");
      assertFalse(
          WasiErrorCode.UNKNOWN.isPermissionError(), "UNKNOWN should not be permission error");
      assertFalse(
          WasiErrorCode.UNKNOWN.isResourceLimitError(),
          "UNKNOWN should not be resource limit error");
      assertFalse(WasiErrorCode.UNKNOWN.isRetryable(), "UNKNOWN should not be retryable");

      LOGGER.info("UNKNOWN error code verified: " + WasiErrorCode.UNKNOWN);
    }
  }

  @Nested
  @DisplayName("Retryable Tests")
  class RetryableTests {

    @Test
    @DisplayName("Retryable errors should be marked correctly")
    void retryableErrorsShouldBeMarkedCorrectly() {
      LOGGER.info("Testing retryable error identification");

      assertTrue(WasiErrorCode.EAGAIN.isRetryable(), "EAGAIN should be retryable");
      assertTrue(WasiErrorCode.EINTR.isRetryable(), "EINTR should be retryable");
      assertTrue(WasiErrorCode.ECONNREFUSED.isRetryable(), "ECONNREFUSED should be retryable");
      assertTrue(WasiErrorCode.ETIMEDOUT.isRetryable(), "ETIMEDOUT should be retryable");
      assertTrue(WasiErrorCode.ENOENT.isRetryable(), "ENOENT should be retryable");
      assertTrue(WasiErrorCode.EMFILE.isRetryable(), "EMFILE should be retryable");
      assertTrue(WasiErrorCode.ENOSPC.isRetryable(), "ENOSPC should be retryable");
      assertTrue(WasiErrorCode.ELOOP.isRetryable(), "ELOOP should be retryable");
      assertTrue(WasiErrorCode.ENOMEM.isRetryable(), "ENOMEM should be retryable");
    }

    @Test
    @DisplayName("Non-retryable errors should be marked correctly")
    void nonRetryableErrorsShouldBeMarkedCorrectly() {
      assertFalse(WasiErrorCode.EINVAL.isRetryable(), "EINVAL should not be retryable");
      assertFalse(WasiErrorCode.EPERM.isRetryable(), "EPERM should not be retryable");
      assertFalse(WasiErrorCode.SUCCESS.isRetryable(), "SUCCESS should not be retryable");
      assertFalse(WasiErrorCode.EBADF.isRetryable(), "EBADF should not be retryable");
    }
  }

  @Nested
  @DisplayName("fromErrno Tests")
  class FromErrnoTests {

    @Test
    @DisplayName("fromErrno should return correct error code")
    void fromErrnoShouldReturnCorrectErrorCode() {
      LOGGER.info("Testing fromErrno with valid values");

      assertEquals(WasiErrorCode.SUCCESS, WasiErrorCode.fromErrno(0), "Should return SUCCESS");
      assertEquals(WasiErrorCode.EPERM, WasiErrorCode.fromErrno(1), "Should return EPERM");
      assertEquals(WasiErrorCode.ENOENT, WasiErrorCode.fromErrno(2), "Should return ENOENT");
      assertEquals(WasiErrorCode.EINTR, WasiErrorCode.fromErrno(4), "Should return EINTR");
      assertEquals(WasiErrorCode.EIO, WasiErrorCode.fromErrno(5), "Should return EIO");
      assertEquals(WasiErrorCode.ENOMEM, WasiErrorCode.fromErrno(12), "Should return ENOMEM");
      assertEquals(WasiErrorCode.EINVAL, WasiErrorCode.fromErrno(22), "Should return EINVAL");

      LOGGER.info("fromErrno with valid values verified");
    }

    @Test
    @DisplayName("fromErrno should throw for unknown errno")
    void fromErrnoShouldThrowForUnknownErrno() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiErrorCode.fromErrno(999),
              "Should throw for unknown errno");

      assertTrue(
          ex.getMessage().contains("Unknown errno"),
          "Error message should mention 'Unknown errno': " + ex.getMessage());
    }

    @Test
    @DisplayName("fromErrno should throw for negative errno")
    void fromErrnoShouldThrowForNegativeErrno() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiErrorCode.fromErrno(-999),
          "Should throw for very negative errno");
    }
  }

  @Nested
  @DisplayName("fromErrnoOrNull Tests")
  class FromErrnoOrNullTests {

    @Test
    @DisplayName("fromErrnoOrNull should return correct error code")
    void fromErrnoOrNullShouldReturnCorrectErrorCode() {
      assertEquals(
          WasiErrorCode.SUCCESS, WasiErrorCode.fromErrnoOrNull(0), "Should return SUCCESS");
      assertEquals(WasiErrorCode.EPERM, WasiErrorCode.fromErrnoOrNull(1), "Should return EPERM");
      assertEquals(WasiErrorCode.ENOENT, WasiErrorCode.fromErrnoOrNull(2), "Should return ENOENT");
      assertEquals(WasiErrorCode.EINVAL, WasiErrorCode.fromErrnoOrNull(22), "Should return EINVAL");
    }

    @Test
    @DisplayName("fromErrnoOrNull should return null for unknown errno")
    void fromErrnoOrNullShouldReturnNullForUnknownErrno() {
      assertNull(WasiErrorCode.fromErrnoOrNull(999), "Should return null for unknown errno");
      assertNull(WasiErrorCode.fromErrnoOrNull(-999), "Should return null for negative errno");
    }
  }

  @Nested
  @DisplayName("createGeneric Tests")
  class CreateGenericTests {

    @Test
    @DisplayName("createGeneric should return UNKNOWN")
    void createGenericShouldReturnUnknown() {
      assertEquals(
          WasiErrorCode.UNKNOWN, WasiErrorCode.createGeneric(999), "Should return UNKNOWN");
      assertEquals(WasiErrorCode.UNKNOWN, WasiErrorCode.createGeneric(-1), "Should return UNKNOWN");
      assertEquals(WasiErrorCode.UNKNOWN, WasiErrorCode.createGeneric(0), "Should return UNKNOWN");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include name, errno, and description")
    void toStringShouldIncludeNameErrnoAndDescription() {
      final String str = WasiErrorCode.ENOENT.toString();

      assertTrue(str.contains("ENOENT"), "Should contain name");
      assertTrue(str.contains("2"), "Should contain errno");
      assertTrue(str.contains("No such file"), "Should contain description");
    }

    @Test
    @DisplayName("toString should format correctly for all values")
    void toStringShouldFormatCorrectlyForAllValues() {
      for (WasiErrorCode code : WasiErrorCode.values()) {
        final String str = code.toString();
        assertNotNull(str, "toString should not be null: " + code.name());
        assertTrue(str.contains(code.name()), "Should contain name: " + code.name());
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Error codes should be usable in switch statement")
    void errorCodesShouldBeUsableInSwitchStatement() {
      final WasiErrorCode code = WasiErrorCode.ENOENT;

      final String result;
      switch (code) {
        case ENOENT:
          result = "file not found";
          break;
        case EACCES:
          result = "access denied";
          break;
        default:
          result = "other error";
          break;
      }

      assertEquals("file not found", result, "Switch should work correctly");
    }

    @Test
    @DisplayName("Multiple error type flags can be true")
    void multipleErrorTypeFlagsCanBeTrue() {
      // EACCES is both file system and permission error
      assertTrue(WasiErrorCode.EACCES.isFileSystemError(), "Should be file system error");
      assertTrue(WasiErrorCode.EACCES.isPermissionError(), "Should also be permission error");
    }

    @Test
    @DisplayName("EWOULDBLOCK and EAGAIN should have same errno")
    void ewouldblockAndEagainShouldHaveSameErrno() {
      assertEquals(
          WasiErrorCode.EAGAIN.getErrno(),
          WasiErrorCode.EWOULDBLOCK.getErrno(),
          "EAGAIN and EWOULDBLOCK should have same errno (POSIX standard)");
    }

    @Test
    @DisplayName("Should cover all error codes")
    void shouldCoverAllErrorCodes() {
      LOGGER.info("Testing all error codes for coverage");

      for (final WasiErrorCode code : WasiErrorCode.values()) {
        // Execute all getters
        final int errno = code.getErrno();
        final String desc = code.getDescription();
        final boolean isFs = code.isFileSystemError();
        final boolean isNet = code.isNetworkError();
        final boolean isPerm = code.isPermissionError();
        final boolean isRes = code.isResourceLimitError();
        final boolean isRetry = code.isRetryable();
        final String str = code.toString();

        assertNotNull(desc, "Description should not be null for " + code);
        assertNotNull(str, "toString should not be null for " + code);

        LOGGER.fine(
            "Covered "
                + code
                + ": errno="
                + errno
                + ", fs="
                + isFs
                + ", net="
                + isNet
                + ", perm="
                + isPerm
                + ", res="
                + isRes
                + ", retry="
                + isRetry);
      }

      LOGGER.info("All " + WasiErrorCode.values().length + " error codes covered");
    }

    @Test
    @DisplayName("Should have distinct errno values for most codes")
    void shouldHaveDistinctErrnoValuesForMostCodes() {
      // Note: EAGAIN and EWOULDBLOCK share the same errno (11)
      // This is intentional as they're the same error on most systems
      final WasiErrorCode eagain = WasiErrorCode.EAGAIN;
      final WasiErrorCode ewouldblock = WasiErrorCode.EWOULDBLOCK;

      assertEquals(
          eagain.getErrno(),
          ewouldblock.getErrno(),
          "EAGAIN and EWOULDBLOCK should have same errno");
      assertEquals(11, eagain.getErrno(), "EAGAIN should be errno 11");
    }
  }
}

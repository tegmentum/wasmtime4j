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

package ai.tegmentum.wasmtime4j.panama.wasi.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Panama {@link WasiErrorCode} enum.
 *
 * <p>This test class verifies WasiErrorCode enum values and behavior.
 */
@DisplayName("Panama WasiErrorCode Tests")
class WasiErrorCodeTest {

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
  @DisplayName("Error Code Attribute Tests")
  class ErrorCodeAttributeTests {

    @Test
    @DisplayName("SUCCESS should have errno 0")
    void successShouldHaveErrnoZero() {
      assertEquals(0, WasiErrorCode.SUCCESS.getErrno(), "SUCCESS should have errno 0");
    }

    @Test
    @DisplayName("ENOENT should have errno 2")
    void enoentShouldHaveErrno2() {
      assertEquals(2, WasiErrorCode.ENOENT.getErrno(), "ENOENT should have errno 2");
    }

    @Test
    @DisplayName("EPERM should have errno 1")
    void epermShouldHaveErrno1() {
      assertEquals(1, WasiErrorCode.EPERM.getErrno(), "EPERM should have errno 1");
    }

    @Test
    @DisplayName("All error codes should have descriptions")
    void allErrorCodesShouldHaveDescriptions() {
      for (WasiErrorCode code : WasiErrorCode.values()) {
        assertNotNull(code.getDescription(), "Description should not be null: " + code.name());
        assertFalse(
            code.getDescription().isEmpty(), "Description should not be empty: " + code.name());
      }
    }
  }

  @Nested
  @DisplayName("Error Category Tests")
  class ErrorCategoryTests {

    @Test
    @DisplayName("File system errors should be marked correctly")
    void fileSystemErrorsShouldBeMarkedCorrectly() {
      assertTrue(WasiErrorCode.ENOENT.isFileSystemError(), "ENOENT should be file system error");
      assertTrue(WasiErrorCode.EIO.isFileSystemError(), "EIO should be file system error");
      assertTrue(WasiErrorCode.EBADF.isFileSystemError(), "EBADF should be file system error");
      assertTrue(WasiErrorCode.EACCES.isFileSystemError(), "EACCES should be file system error");
      assertTrue(WasiErrorCode.EEXIST.isFileSystemError(), "EEXIST should be file system error");
      assertTrue(WasiErrorCode.ENOTDIR.isFileSystemError(), "ENOTDIR should be file system error");
      assertTrue(WasiErrorCode.EISDIR.isFileSystemError(), "EISDIR should be file system error");
    }

    @Test
    @DisplayName("Network errors should be marked correctly")
    void networkErrorsShouldBeMarkedCorrectly() {
      assertTrue(
          WasiErrorCode.ECONNREFUSED.isNetworkError(), "ECONNREFUSED should be network error");
      assertTrue(WasiErrorCode.ETIMEDOUT.isNetworkError(), "ETIMEDOUT should be network error");
      assertTrue(
          WasiErrorCode.EHOSTUNREACH.isNetworkError(), "EHOSTUNREACH should be network error");
      assertTrue(WasiErrorCode.ENETDOWN.isNetworkError(), "ENETDOWN should be network error");
      assertTrue(WasiErrorCode.ECONNRESET.isNetworkError(), "ECONNRESET should be network error");
    }

    @Test
    @DisplayName("Permission errors should be marked correctly")
    void permissionErrorsShouldBeMarkedCorrectly() {
      assertTrue(WasiErrorCode.EPERM.isPermissionError(), "EPERM should be permission error");
      assertTrue(WasiErrorCode.EACCES.isPermissionError(), "EACCES should be permission error");
      assertTrue(WasiErrorCode.EROFS.isPermissionError(), "EROFS should be permission error");
    }

    @Test
    @DisplayName("Resource limit errors should be marked correctly")
    void resourceLimitErrorsShouldBeMarkedCorrectly() {
      assertTrue(
          WasiErrorCode.ENOMEM.isResourceLimitError(), "ENOMEM should be resource limit error");
      assertTrue(
          WasiErrorCode.EMFILE.isResourceLimitError(), "EMFILE should be resource limit error");
      assertTrue(
          WasiErrorCode.ENOBUFS.isResourceLimitError(), "ENOBUFS should be resource limit error");
      assertTrue(
          WasiErrorCode.EAGAIN.isResourceLimitError(), "EAGAIN should be resource limit error");
    }

    @Test
    @DisplayName("SUCCESS should not be any error type")
    void successShouldNotBeAnyErrorType() {
      assertFalse(
          WasiErrorCode.SUCCESS.isFileSystemError(), "SUCCESS should not be file system error");
      assertFalse(WasiErrorCode.SUCCESS.isNetworkError(), "SUCCESS should not be network error");
      assertFalse(
          WasiErrorCode.SUCCESS.isPermissionError(), "SUCCESS should not be permission error");
      assertFalse(
          WasiErrorCode.SUCCESS.isResourceLimitError(),
          "SUCCESS should not be resource limit error");
    }
  }

  @Nested
  @DisplayName("Retryable Tests")
  class RetryableTests {

    @Test
    @DisplayName("Retryable errors should be marked correctly")
    void retryableErrorsShouldBeMarkedCorrectly() {
      assertTrue(WasiErrorCode.EAGAIN.isRetryable(), "EAGAIN should be retryable");
      assertTrue(WasiErrorCode.EINTR.isRetryable(), "EINTR should be retryable");
      assertTrue(WasiErrorCode.ECONNREFUSED.isRetryable(), "ECONNREFUSED should be retryable");
      assertTrue(WasiErrorCode.ETIMEDOUT.isRetryable(), "ETIMEDOUT should be retryable");
    }

    @Test
    @DisplayName("Non-retryable errors should be marked correctly")
    void nonRetryableErrorsShouldBeMarkedCorrectly() {
      assertFalse(WasiErrorCode.EINVAL.isRetryable(), "EINVAL should not be retryable");
      assertFalse(WasiErrorCode.EPERM.isRetryable(), "EPERM should not be retryable");
      assertFalse(WasiErrorCode.SUCCESS.isRetryable(), "SUCCESS should not be retryable");
    }
  }

  @Nested
  @DisplayName("fromErrno Tests")
  class FromErrnoTests {

    @Test
    @DisplayName("fromErrno should return correct error code")
    void fromErrnoShouldReturnCorrectErrorCode() {
      assertEquals(WasiErrorCode.SUCCESS, WasiErrorCode.fromErrno(0), "Should return SUCCESS");
      assertEquals(WasiErrorCode.EPERM, WasiErrorCode.fromErrno(1), "Should return EPERM");
      assertEquals(WasiErrorCode.ENOENT, WasiErrorCode.fromErrno(2), "Should return ENOENT");
      assertEquals(WasiErrorCode.EIO, WasiErrorCode.fromErrno(5), "Should return EIO");
    }

    @Test
    @DisplayName("fromErrno should throw for unknown errno")
    void fromErrnoShouldThrowForUnknownErrno() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiErrorCode.fromErrno(999),
          "Should throw for unknown errno");
    }

    @Test
    @DisplayName("fromErrno should throw for negative errno")
    void fromErrnoShouldThrowForNegativeErrno() {
      // Note: -1 is used for UNKNOWN, so test with -999
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
    }

    @Test
    @DisplayName("fromErrnoOrNull should return null for unknown errno")
    void fromErrnoOrNullShouldReturnNullForUnknownErrno() {
      assertNull(WasiErrorCode.fromErrnoOrNull(999), "Should return null for unknown errno");
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
  }
}

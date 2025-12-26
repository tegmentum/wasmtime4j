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

package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasiFileSystemException.FileSystemErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiFileSystemException} class.
 *
 * <p>This test class verifies the construction and behavior of WASI file system exceptions,
 * including error types, errno codes, and error categorization.
 */
@DisplayName("WasiFileSystemException Tests")
class WasiFileSystemExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiFileSystemException should extend WasiException")
    void shouldExtendWasiException() {
      assertTrue(
          WasiException.class.isAssignableFrom(WasiFileSystemException.class),
          "WasiFileSystemException should extend WasiException");
    }

    @Test
    @DisplayName("WasiFileSystemException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(WasiFileSystemException.class),
          "WasiFileSystemException should be serializable");
    }
  }

  @Nested
  @DisplayName("FileSystemErrorType Enum Tests")
  class FileSystemErrorTypeEnumTests {

    @Test
    @DisplayName("Should have NOT_FOUND value")
    void shouldHaveNotFoundValue() {
      assertNotNull(FileSystemErrorType.valueOf("NOT_FOUND"), "Should have NOT_FOUND value");
    }

    @Test
    @DisplayName("Should have PERMISSION_DENIED value")
    void shouldHavePermissionDeniedValue() {
      assertNotNull(
          FileSystemErrorType.valueOf("PERMISSION_DENIED"), "Should have PERMISSION_DENIED value");
    }

    @Test
    @DisplayName("Should have ALREADY_EXISTS value")
    void shouldHaveAlreadyExistsValue() {
      assertNotNull(
          FileSystemErrorType.valueOf("ALREADY_EXISTS"), "Should have ALREADY_EXISTS value");
    }

    @Test
    @DisplayName("Should have IS_DIRECTORY value")
    void shouldHaveIsDirectoryValue() {
      assertNotNull(FileSystemErrorType.valueOf("IS_DIRECTORY"), "Should have IS_DIRECTORY value");
    }

    @Test
    @DisplayName("Should have NOT_DIRECTORY value")
    void shouldHaveNotDirectoryValue() {
      assertNotNull(
          FileSystemErrorType.valueOf("NOT_DIRECTORY"), "Should have NOT_DIRECTORY value");
    }

    @Test
    @DisplayName("Should have IO_ERROR value")
    void shouldHaveIoErrorValue() {
      assertNotNull(FileSystemErrorType.valueOf("IO_ERROR"), "Should have IO_ERROR value");
    }

    @Test
    @DisplayName("Should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(FileSystemErrorType.valueOf("UNKNOWN"), "Should have UNKNOWN value");
    }

    @Test
    @DisplayName("Each error type should have description")
    void eachErrorTypeShouldHaveDescription() {
      for (final FileSystemErrorType type : FileSystemErrorType.values()) {
        assertNotNull(type.getDescription(), type.name() + " should have description");
        assertFalse(
            type.getDescription().isEmpty(), type.name() + " should have non-empty description");
      }
    }

    @Test
    @DisplayName("Each error type should have errno code")
    void eachErrorTypeShouldHaveErrnoCode() {
      for (final FileSystemErrorType type : FileSystemErrorType.values()) {
        // All should have errno codes, UNKNOWN uses -1
        int errno = type.getErrnoCode();
        if (type != FileSystemErrorType.UNKNOWN) {
          assertTrue(errno > 0, type.name() + " should have positive errno");
        }
      }
    }

    @Test
    @DisplayName("Should have 21 error types")
    void shouldHave21ErrorTypes() {
      assertEquals(21, FileSystemErrorType.values().length, "Should have 21 error types");
    }

    @Test
    @DisplayName("fromErrno should return correct type for known errno")
    void fromErrnoShouldReturnCorrectTypeForKnownErrno() {
      assertEquals(
          FileSystemErrorType.NOT_FOUND,
          FileSystemErrorType.fromErrno(44),
          "Errno 44 should map to NOT_FOUND");
      assertEquals(
          FileSystemErrorType.PERMISSION_DENIED,
          FileSystemErrorType.fromErrno(63),
          "Errno 63 should map to PERMISSION_DENIED");
    }

    @Test
    @DisplayName("fromErrno should return UNKNOWN for unknown errno")
    void fromErrnoShouldReturnUnknownForUnknownErrno() {
      assertEquals(
          FileSystemErrorType.UNKNOWN,
          FileSystemErrorType.fromErrno(99999),
          "Unknown errno should map to UNKNOWN");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with error type and message should set defaults")
    void constructorWithErrorTypeAndMessageShouldSetDefaults() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, "File not found");

      assertEquals(
          FileSystemErrorType.NOT_FOUND,
          exception.getFileSystemErrorType(),
          "Error type should be NOT_FOUND");
      assertTrue(
          exception.getMessage().contains("File not found"), "Message should contain error text");
      assertNull(exception.getFilePath(), "File path should be null");
      assertNull(exception.getFileOperation(), "File operation should be null");
    }

    @Test
    @DisplayName("Constructor with error type, message, and cause should set all")
    void constructorWithErrorTypeMessageAndCauseShouldSetAll() {
      final Throwable cause = new RuntimeException("Root cause");
      final WasiFileSystemException exception =
          new WasiFileSystemException(FileSystemErrorType.IO_ERROR, "I/O error occurred", cause);

      assertEquals(
          FileSystemErrorType.IO_ERROR,
          exception.getFileSystemErrorType(),
          "Error type should be IO_ERROR");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Full constructor should set all fields")
    void fullConstructorShouldSetAllFields() {
      final Throwable cause = new RuntimeException("Root cause");
      final WasiFileSystemException exception =
          new WasiFileSystemException(
              FileSystemErrorType.PERMISSION_DENIED,
              "Cannot access file",
              "/etc/passwd",
              "read",
              63,
              cause);

      assertEquals(
          FileSystemErrorType.PERMISSION_DENIED,
          exception.getFileSystemErrorType(),
          "Error type should be PERMISSION_DENIED");
      assertEquals("/etc/passwd", exception.getFilePath(), "File path should be '/etc/passwd'");
      assertEquals("read", exception.getFileOperation(), "File operation should be 'read'");
      assertEquals(Integer.valueOf(63), exception.getErrnoCode(), "Errno code should be 63");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Constructor should handle null error type")
    void constructorShouldHandleNullErrorType() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(null, "Error message", null, null, null, null);

      assertEquals(
          FileSystemErrorType.UNKNOWN,
          exception.getFileSystemErrorType(),
          "Null error type should default to UNKNOWN");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getFileSystemErrorType should return error type")
    void getFileSystemErrorTypeShouldReturnErrorType() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(FileSystemErrorType.NO_SPACE, "No space left");

      assertEquals(
          FileSystemErrorType.NO_SPACE,
          exception.getFileSystemErrorType(),
          "getFileSystemErrorType should return NO_SPACE");
    }

    @Test
    @DisplayName("getErrnoCode should return errno code")
    void getErrnoCodeShouldReturnErrnoCode() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Error", "/path", "open", 44, null);

      assertEquals(Integer.valueOf(44), exception.getErrnoCode(), "getErrnoCode should return 44");
    }

    @Test
    @DisplayName("getErrnoCode should return default from error type when not specified")
    void getErrnoCodeShouldReturnDefaultFromErrorType() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, "Error");

      assertEquals(
          Integer.valueOf(44),
          exception.getErrnoCode(),
          "getErrnoCode should return default errno from error type");
    }

    @Test
    @DisplayName("getFilePath should return file path")
    void getFilePathShouldReturnFilePath() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(
              FileSystemErrorType.UNKNOWN, "Error", "/home/user/file.txt", null, null, null);

      assertEquals(
          "/home/user/file.txt",
          exception.getFilePath(),
          "getFilePath should return '/home/user/file.txt'");
    }

    @Test
    @DisplayName("getFileOperation should return file operation")
    void getFileOperationShouldReturnFileOperation() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(
              FileSystemErrorType.UNKNOWN, "Error", null, "write", null, null);

      assertEquals("write", exception.getFileOperation(), "getFileOperation should return 'write'");
    }
  }

  @Nested
  @DisplayName("Error Category Check Tests")
  class ErrorCategoryCheckTests {

    @Test
    @DisplayName("isPermissionError should return true for permission errors")
    void isPermissionErrorShouldReturnTrueForPermissionErrors() {
      final WasiFileSystemException permDenied =
          new WasiFileSystemException(FileSystemErrorType.PERMISSION_DENIED, "Error");
      final WasiFileSystemException readOnly =
          new WasiFileSystemException(FileSystemErrorType.READ_ONLY, "Error");

      assertTrue(permDenied.isPermissionError(), "PERMISSION_DENIED should be permission error");
      assertTrue(readOnly.isPermissionError(), "READ_ONLY should be permission error");
    }

    @Test
    @DisplayName("isPermissionError should return false for non-permission errors")
    void isPermissionErrorShouldReturnFalseForNonPermissionErrors() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, "Error");

      assertFalse(exception.isPermissionError(), "NOT_FOUND should not be permission error");
    }

    @Test
    @DisplayName("isExistenceError should return true for existence errors")
    void isExistenceErrorShouldReturnTrueForExistenceErrors() {
      final WasiFileSystemException notFound =
          new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, "Error");
      final WasiFileSystemException exists =
          new WasiFileSystemException(FileSystemErrorType.ALREADY_EXISTS, "Error");

      assertTrue(notFound.isExistenceError(), "NOT_FOUND should be existence error");
      assertTrue(exists.isExistenceError(), "ALREADY_EXISTS should be existence error");
    }

    @Test
    @DisplayName("isFileTypeError should return true for file type errors")
    void isFileTypeErrorShouldReturnTrueForFileTypeErrors() {
      final WasiFileSystemException isDir =
          new WasiFileSystemException(FileSystemErrorType.IS_DIRECTORY, "Error");
      final WasiFileSystemException notDir =
          new WasiFileSystemException(FileSystemErrorType.NOT_DIRECTORY, "Error");
      final WasiFileSystemException notEmpty =
          new WasiFileSystemException(FileSystemErrorType.DIRECTORY_NOT_EMPTY, "Error");

      assertTrue(isDir.isFileTypeError(), "IS_DIRECTORY should be file type error");
      assertTrue(notDir.isFileTypeError(), "NOT_DIRECTORY should be file type error");
      assertTrue(notEmpty.isFileTypeError(), "DIRECTORY_NOT_EMPTY should be file type error");
    }

    @Test
    @DisplayName("isResourceLimitError should return true for resource limit errors")
    void isResourceLimitErrorShouldReturnTrueForResourceLimitErrors() {
      final WasiFileSystemException noSpace =
          new WasiFileSystemException(FileSystemErrorType.NO_SPACE, "Error");
      final WasiFileSystemException tooLarge =
          new WasiFileSystemException(FileSystemErrorType.FILE_TOO_LARGE, "Error");
      final WasiFileSystemException tooMany =
          new WasiFileSystemException(FileSystemErrorType.TOO_MANY_OPEN_FILES, "Error");
      final WasiFileSystemException nameTooLong =
          new WasiFileSystemException(FileSystemErrorType.NAME_TOO_LONG, "Error");

      assertTrue(noSpace.isResourceLimitError(), "NO_SPACE should be resource limit error");
      assertTrue(tooLarge.isResourceLimitError(), "FILE_TOO_LARGE should be resource limit error");
      assertTrue(
          tooMany.isResourceLimitError(), "TOO_MANY_OPEN_FILES should be resource limit error");
      assertTrue(
          nameTooLong.isResourceLimitError(), "NAME_TOO_LONG should be resource limit error");
    }

    @Test
    @DisplayName("isTransientError should return true for transient errors")
    void isTransientErrorShouldReturnTrueForTransientErrors() {
      final WasiFileSystemException ioError =
          new WasiFileSystemException(FileSystemErrorType.IO_ERROR, "Error");
      final WasiFileSystemException wouldBlock =
          new WasiFileSystemException(FileSystemErrorType.WOULD_BLOCK, "Error");
      final WasiFileSystemException connReset =
          new WasiFileSystemException(FileSystemErrorType.CONNECTION_RESET, "Error");

      assertTrue(ioError.isTransientError(), "IO_ERROR should be transient error");
      assertTrue(wouldBlock.isTransientError(), "WOULD_BLOCK should be transient error");
      assertTrue(connReset.isTransientError(), "CONNECTION_RESET should be transient error");
    }
  }

  @Nested
  @DisplayName("Message Formatting Tests")
  class MessageFormattingTests {

    @Test
    @DisplayName("Message should include error type")
    void messageShouldIncludeErrorType() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, "Error message");

      assertTrue(exception.getMessage().contains("NOT_FOUND"), "Message should contain error type");
    }

    @Test
    @DisplayName("Message should include file path when provided")
    void messageShouldIncludeFilePathWhenProvided() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Error", "/path/to/file", null, null, null);

      assertTrue(
          exception.getMessage().contains("/path/to/file"), "Message should contain file path");
    }

    @Test
    @DisplayName("Message should include file operation when provided")
    void messageShouldIncludeFileOperationWhenProvided() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Error", null, "open", null, null);

      assertTrue(exception.getMessage().contains("open"), "Message should contain file operation");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      final WasiFileSystemException exception =
          new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, "Test");

      assertTrue(exception instanceof Throwable, "WasiFileSystemException should be throwable");
    }

    @Test
    @DisplayName("Should be catchable as WasiException")
    void shouldBeCatchableAsWasiException() {
      try {
        throw new WasiFileSystemException(FileSystemErrorType.PERMISSION_DENIED, "Test error");
      } catch (WasiException e) {
        assertTrue(e.getMessage().contains("Test error"), "Should be catchable as WasiException");
      }
    }

    @Test
    @DisplayName("Should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      try {
        throw new WasiFileSystemException(FileSystemErrorType.IO_ERROR, "Test error");
      } catch (WasmException e) {
        assertNotNull(e, "Should be catchable as WasmException");
      }
    }
  }
}

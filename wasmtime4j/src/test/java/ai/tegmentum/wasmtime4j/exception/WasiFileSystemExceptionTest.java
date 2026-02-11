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

    @Test
    @DisplayName("Operation suffix must be added for non-null non-empty operation")
    void operationSuffixMustBeAddedForNonNullNonEmptyOperation() {
      // Mutation killer: tests that the operation condition evaluates correctly
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.IO_ERROR, "msg", null, "myop", null, null);
      // Check for the specific format with parentheses
      assertTrue(
          ex.getMessage().contains("(operation: myop)"),
          "Operation suffix (operation: myop) must be present: " + ex.getMessage());
    }

    @Test
    @DisplayName("Operation suffix absent when operation is null")
    void operationSuffixAbsentWhenOperationIsNull() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.IO_ERROR, "msg", "/path", null, null, null);
      assertFalse(
          ex.getMessage().contains("(operation:"),
          "Operation suffix must be absent when null: " + ex.getMessage());
    }

    @Test
    @DisplayName("Operation suffix absent when operation is empty string")
    void operationSuffixAbsentWhenOperationIsEmptyString() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.IO_ERROR, "msg", "/path", "", null, null);
      assertFalse(
          ex.getMessage().contains("(operation:"),
          "Operation suffix must be absent when empty: " + ex.getMessage());
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

  // ============================================================================
  // MUTATION TESTING COVERAGE TESTS
  // ============================================================================
  // The following tests are specifically designed to kill PIT mutations that
  // survive basic functionality tests. They test:
  // 1. Boolean return value mutations for error category methods
  // 2. Exact count verification for error type categories
  // 3. formatFileSystemMessage edge cases (null/empty path/operation)
  // 4. isRetryableError switch statement coverage
  // 5. fromErrno edge cases
  // 6. Message validation for null/empty
  // 7. Getter return value exactness
  // ============================================================================

  @Nested
  @DisplayName("Error Category Boolean Return Mutation Tests")
  class ErrorCategoryBooleanReturnMutationTests {

    // -------------------------------------------------------------------------
    // isPermissionError() - Tests for false returns on non-permission types
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isPermissionError returns false for NOT_FOUND")
    void isPermissionErrorReturnsFalseForNotFound() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, "test");
      assertFalse(ex.isPermissionError(), "NOT_FOUND should NOT be permission error");
    }

    @Test
    @DisplayName("isPermissionError returns false for ALREADY_EXISTS")
    void isPermissionErrorReturnsFalseForAlreadyExists() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.ALREADY_EXISTS, "test");
      assertFalse(ex.isPermissionError(), "ALREADY_EXISTS should NOT be permission error");
    }

    @Test
    @DisplayName("isPermissionError returns false for IS_DIRECTORY")
    void isPermissionErrorReturnsFalseForIsDirectory() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.IS_DIRECTORY, "test");
      assertFalse(ex.isPermissionError(), "IS_DIRECTORY should NOT be permission error");
    }

    @Test
    @DisplayName("isPermissionError returns false for IO_ERROR")
    void isPermissionErrorReturnsFalseForIoError() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.IO_ERROR, "test");
      assertFalse(ex.isPermissionError(), "IO_ERROR should NOT be permission error");
    }

    @Test
    @DisplayName("isPermissionError returns false for UNKNOWN")
    void isPermissionErrorReturnsFalseForUnknown() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.UNKNOWN, "test");
      assertFalse(ex.isPermissionError(), "UNKNOWN should NOT be permission error");
    }

    // -------------------------------------------------------------------------
    // isExistenceError() - Tests for false returns on non-existence types
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isExistenceError returns false for PERMISSION_DENIED")
    void isExistenceErrorReturnsFalseForPermissionDenied() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.PERMISSION_DENIED, "test");
      assertFalse(ex.isExistenceError(), "PERMISSION_DENIED should NOT be existence error");
    }

    @Test
    @DisplayName("isExistenceError returns false for IS_DIRECTORY")
    void isExistenceErrorReturnsFalseForIsDirectory() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.IS_DIRECTORY, "test");
      assertFalse(ex.isExistenceError(), "IS_DIRECTORY should NOT be existence error");
    }

    @Test
    @DisplayName("isExistenceError returns false for IO_ERROR")
    void isExistenceErrorReturnsFalseForIoError() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.IO_ERROR, "test");
      assertFalse(ex.isExistenceError(), "IO_ERROR should NOT be existence error");
    }

    @Test
    @DisplayName("isExistenceError returns false for NO_SPACE")
    void isExistenceErrorReturnsFalseForNoSpace() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.NO_SPACE, "test");
      assertFalse(ex.isExistenceError(), "NO_SPACE should NOT be existence error");
    }

    @Test
    @DisplayName("isExistenceError returns false for UNKNOWN")
    void isExistenceErrorReturnsFalseForUnknown() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.UNKNOWN, "test");
      assertFalse(ex.isExistenceError(), "UNKNOWN should NOT be existence error");
    }

    // -------------------------------------------------------------------------
    // isFileTypeError() - Tests for false returns on non-file-type types
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isFileTypeError returns false for NOT_FOUND")
    void isFileTypeErrorReturnsFalseForNotFound() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, "test");
      assertFalse(ex.isFileTypeError(), "NOT_FOUND should NOT be file type error");
    }

    @Test
    @DisplayName("isFileTypeError returns false for PERMISSION_DENIED")
    void isFileTypeErrorReturnsFalseForPermissionDenied() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.PERMISSION_DENIED, "test");
      assertFalse(ex.isFileTypeError(), "PERMISSION_DENIED should NOT be file type error");
    }

    @Test
    @DisplayName("isFileTypeError returns false for IO_ERROR")
    void isFileTypeErrorReturnsFalseForIoError() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.IO_ERROR, "test");
      assertFalse(ex.isFileTypeError(), "IO_ERROR should NOT be file type error");
    }

    @Test
    @DisplayName("isFileTypeError returns false for NO_SPACE")
    void isFileTypeErrorReturnsFalseForNoSpace() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.NO_SPACE, "test");
      assertFalse(ex.isFileTypeError(), "NO_SPACE should NOT be file type error");
    }

    @Test
    @DisplayName("isFileTypeError returns false for UNKNOWN")
    void isFileTypeErrorReturnsFalseForUnknown() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.UNKNOWN, "test");
      assertFalse(ex.isFileTypeError(), "UNKNOWN should NOT be file type error");
    }

    // -------------------------------------------------------------------------
    // isResourceLimitError() - Tests for false returns on non-resource types
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isResourceLimitError returns false for NOT_FOUND")
    void isResourceLimitErrorReturnsFalseForNotFound() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, "test");
      assertFalse(ex.isResourceLimitError(), "NOT_FOUND should NOT be resource limit error");
    }

    @Test
    @DisplayName("isResourceLimitError returns false for PERMISSION_DENIED")
    void isResourceLimitErrorReturnsFalseForPermissionDenied() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.PERMISSION_DENIED, "test");
      assertFalse(
          ex.isResourceLimitError(), "PERMISSION_DENIED should NOT be resource limit error");
    }

    @Test
    @DisplayName("isResourceLimitError returns false for IS_DIRECTORY")
    void isResourceLimitErrorReturnsFalseForIsDirectory() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.IS_DIRECTORY, "test");
      assertFalse(ex.isResourceLimitError(), "IS_DIRECTORY should NOT be resource limit error");
    }

    @Test
    @DisplayName("isResourceLimitError returns false for IO_ERROR")
    void isResourceLimitErrorReturnsFalseForIoError() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.IO_ERROR, "test");
      assertFalse(ex.isResourceLimitError(), "IO_ERROR should NOT be resource limit error");
    }

    @Test
    @DisplayName("isResourceLimitError returns false for UNKNOWN")
    void isResourceLimitErrorReturnsFalseForUnknown() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.UNKNOWN, "test");
      assertFalse(ex.isResourceLimitError(), "UNKNOWN should NOT be resource limit error");
    }

    // -------------------------------------------------------------------------
    // isTransientError() - Tests for false returns on non-transient types
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isTransientError returns false for NOT_FOUND")
    void isTransientErrorReturnsFalseForNotFound() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, "test");
      assertFalse(ex.isTransientError(), "NOT_FOUND should NOT be transient error");
    }

    @Test
    @DisplayName("isTransientError returns false for PERMISSION_DENIED")
    void isTransientErrorReturnsFalseForPermissionDenied() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.PERMISSION_DENIED, "test");
      assertFalse(ex.isTransientError(), "PERMISSION_DENIED should NOT be transient error");
    }

    @Test
    @DisplayName("isTransientError returns false for IS_DIRECTORY")
    void isTransientErrorReturnsFalseForIsDirectory() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.IS_DIRECTORY, "test");
      assertFalse(ex.isTransientError(), "IS_DIRECTORY should NOT be transient error");
    }

    @Test
    @DisplayName("isTransientError returns false for NO_SPACE")
    void isTransientErrorReturnsFalseForNoSpace() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.NO_SPACE, "test");
      assertFalse(ex.isTransientError(), "NO_SPACE should NOT be transient error");
    }

    @Test
    @DisplayName("isTransientError returns false for UNKNOWN")
    void isTransientErrorReturnsFalseForUnknown() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.UNKNOWN, "test");
      assertFalse(ex.isTransientError(), "UNKNOWN should NOT be transient error");
    }
  }

  @Nested
  @DisplayName("Error Category Count Verification Tests")
  class ErrorCategoryCountVerificationTests {

    @Test
    @DisplayName("Should have exactly 2 permission error types")
    void shouldHaveExactlyTwoPermissionErrorTypes() {
      int count = 0;
      for (final FileSystemErrorType type : FileSystemErrorType.values()) {
        final WasiFileSystemException ex = new WasiFileSystemException(type, "test");
        if (ex.isPermissionError()) {
          count++;
        }
      }
      assertEquals(
          2, count, "Should have exactly 2 permission error types (PERMISSION_DENIED, READ_ONLY)");
    }

    @Test
    @DisplayName("Should have exactly 2 existence error types")
    void shouldHaveExactlyTwoExistenceErrorTypes() {
      int count = 0;
      for (final FileSystemErrorType type : FileSystemErrorType.values()) {
        final WasiFileSystemException ex = new WasiFileSystemException(type, "test");
        if (ex.isExistenceError()) {
          count++;
        }
      }
      assertEquals(
          2, count, "Should have exactly 2 existence error types (NOT_FOUND, ALREADY_EXISTS)");
    }

    @Test
    @DisplayName("Should have exactly 3 file type error types")
    void shouldHaveExactlyThreeFileTypeErrorTypes() {
      int count = 0;
      for (final FileSystemErrorType type : FileSystemErrorType.values()) {
        final WasiFileSystemException ex = new WasiFileSystemException(type, "test");
        if (ex.isFileTypeError()) {
          count++;
        }
      }
      assertEquals(
          3,
          count,
          "Should have exactly 3 file type error types "
              + "(IS_DIRECTORY, NOT_DIRECTORY, DIRECTORY_NOT_EMPTY)");
    }

    @Test
    @DisplayName("Should have exactly 4 resource limit error types")
    void shouldHaveExactlyFourResourceLimitErrorTypes() {
      int count = 0;
      for (final FileSystemErrorType type : FileSystemErrorType.values()) {
        final WasiFileSystemException ex = new WasiFileSystemException(type, "test");
        if (ex.isResourceLimitError()) {
          count++;
        }
      }
      assertEquals(
          4,
          count,
          "Should have exactly 4 resource limit error types "
              + "(NO_SPACE, FILE_TOO_LARGE, TOO_MANY_OPEN_FILES, NAME_TOO_LONG)");
    }

    @Test
    @DisplayName("Should have exactly 3 transient error types")
    void shouldHaveExactlyThreeTransientErrorTypes() {
      int count = 0;
      for (final FileSystemErrorType type : FileSystemErrorType.values()) {
        final WasiFileSystemException ex = new WasiFileSystemException(type, "test");
        if (ex.isTransientError()) {
          count++;
        }
      }
      assertEquals(
          3,
          count,
          "Should have exactly 3 transient error types "
              + "(IO_ERROR, WOULD_BLOCK, CONNECTION_RESET)");
    }
  }

  @Nested
  @DisplayName("FormatFileSystemMessage Edge Case Mutation Tests")
  class FormatFileSystemMessageEdgeCaseMutationTests {

    @Test
    @DisplayName("Message with null path should not include path suffix")
    void messageWithNullPathShouldNotIncludePathSuffix() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Test message", null, "open", null, null);
      assertFalse(ex.getMessage().contains("(path:"), "Should NOT include path suffix when null");
      assertTrue(ex.getMessage().contains("(operation: open)"), "Should include operation suffix");
    }

    @Test
    @DisplayName("Message with empty path should not include path suffix")
    void messageWithEmptyPathShouldNotIncludePathSuffix() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Test message", "", "open", null, null);
      assertFalse(ex.getMessage().contains("(path:"), "Should NOT include path suffix when empty");
    }

    @Test
    @DisplayName("Message with null operation should not include operation suffix")
    void messageWithNullOperationShouldNotIncludeOperationSuffix() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Test message", "/path", null, null, null);
      assertFalse(
          ex.getMessage().contains("(operation:"), "Should NOT include operation suffix when null");
      assertTrue(ex.getMessage().contains("(path: /path)"), "Should include path suffix");
    }

    @Test
    @DisplayName("Message with empty operation should not include operation suffix")
    void messageWithEmptyOperationShouldNotIncludeOperationSuffix() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Test message", "/path", "", null, null);
      assertFalse(
          ex.getMessage().contains("(operation:"),
          "Should NOT include operation suffix when empty");
    }

    @Test
    @DisplayName("Message with both null path and operation should have no suffixes")
    void messageWithBothNullShouldHaveNoSuffixes() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Test message", null, null, null, null);
      assertFalse(ex.getMessage().contains("(path:"), "Should NOT include path suffix");
      assertFalse(ex.getMessage().contains("(operation:"), "Should NOT include operation suffix");
      assertTrue(ex.getMessage().contains("[NOT_FOUND]"), "Should include error type");
      assertTrue(ex.getMessage().contains("Test message"), "Should include message");
    }

    @Test
    @DisplayName("Message with valid path and operation should include both")
    void messageWithValidPathAndOperationShouldIncludeBoth() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Test message", "/my/path", "read", null, null);
      assertTrue(ex.getMessage().contains("(operation: read)"), "Should include operation suffix");
      assertTrue(ex.getMessage().contains("(path: /my/path)"), "Should include path suffix");
    }

    @Test
    @DisplayName("Message with empty operation should have exact format without operation")
    void messageWithEmptyOperationShouldHaveExactFormatWithoutOperation() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Test message", "/my/path", "", null, null);
      // Verify the message does NOT contain the empty operation (which would be "(operation: )")
      assertFalse(
          ex.getMessage().contains("operation:"),
          "Empty operation should be excluded entirely, not included as '(operation: )'");
      // But should still include path
      assertTrue(ex.getMessage().contains("(path: /my/path)"), "Should include path suffix");
    }

    @Test
    @DisplayName("Message with empty path should have exact format without path")
    void messageWithEmptyPathShouldHaveExactFormatWithoutPath() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Test message", "", "read", null, null);
      // Verify the message does NOT contain the empty path (which would be "(path: )")
      assertFalse(
          ex.getMessage().contains("path:"),
          "Empty path should be excluded entirely, not included as '(path: )'");
      // But should still include operation
      assertTrue(ex.getMessage().contains("(operation: read)"), "Should include operation suffix");
    }

    @Test
    @DisplayName("Empty path with whitespace should be treated as empty")
    void emptyPathWithWhitespaceShouldBeTreatedAsEmpty() {
      // Note: isEmpty() returns false for whitespace-only strings
      // This test documents the current behavior
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Test message", " ", null, null, null);
      // Whitespace-only string is not empty per String.isEmpty()
      // So it will be included in the message
      assertTrue(
          ex.getMessage().contains("(path:  )"),
          "Whitespace-only path is included since isEmpty() returns false for ' '");
    }

    @Test
    @DisplayName("Operation with valid value MUST be included in message")
    void operationWithValidValueMustBeIncluded() {
      // This test is specifically designed to kill mutations that replace
      // the fileOperation null/empty check with false
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "Error", null, "write", null, null);
      final String msg = ex.getMessage();
      assertTrue(
          msg.contains("(operation: write)"),
          "Non-empty operation MUST be included in message, got: " + msg);
    }

    @Test
    @DisplayName("Operation only (no path) should include operation in message")
    void operationOnlyNoPathShouldIncludeOperation() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.IO_ERROR, "IO failure", null, "sync", null, null);
      assertTrue(
          ex.getMessage().contains("(operation: sync)"),
          "Operation should be in message: " + ex.getMessage());
      assertFalse(
          ex.getMessage().contains("(path:"),
          "Path should NOT be in message when null: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("Message Validation Mutation Tests")
  class MessageValidationMutationTests {

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for null message")
    void constructorShouldThrowForNullMessage() {
      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalArgumentException.class,
          () -> new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, null),
          "Should throw IllegalArgumentException for null message");
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for empty message")
    void constructorShouldThrowForEmptyMessage() {
      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalArgumentException.class,
          () -> new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, ""),
          "Should throw IllegalArgumentException for empty message");
    }

    @Test
    @DisplayName("Full constructor should throw for null message")
    void fullConstructorShouldThrowForNullMessage() {
      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalArgumentException.class,
          () ->
              new WasiFileSystemException(
                  FileSystemErrorType.NOT_FOUND, null, "/path", "op", 44, null),
          "Should throw IllegalArgumentException for null message");
    }

    @Test
    @DisplayName("Full constructor should throw for empty message")
    void fullConstructorShouldThrowForEmptyMessage() {
      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalArgumentException.class,
          () ->
              new WasiFileSystemException(
                  FileSystemErrorType.NOT_FOUND, "", "/path", "op", 44, null),
          "Should throw IllegalArgumentException for empty message");
    }
  }

  @Nested
  @DisplayName("IsRetryableError Switch Statement Mutation Tests")
  class IsRetryableErrorSwitchStatementMutationTests {

    @Test
    @DisplayName("IO_ERROR should be retryable (via isRetryable from parent)")
    void ioErrorShouldBeRetryable() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.IO_ERROR, "test");
      assertTrue(ex.isRetryable(), "IO_ERROR should make exception retryable");
    }

    @Test
    @DisplayName("WOULD_BLOCK should be retryable")
    void wouldBlockShouldBeRetryable() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.WOULD_BLOCK, "test");
      assertTrue(ex.isRetryable(), "WOULD_BLOCK should make exception retryable");
    }

    @Test
    @DisplayName("CONNECTION_RESET should be retryable")
    void connectionResetShouldBeRetryable() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.CONNECTION_RESET, "test");
      assertTrue(ex.isRetryable(), "CONNECTION_RESET should make exception retryable");
    }

    @Test
    @DisplayName("NOT_FOUND should not be retryable")
    void notFoundShouldNotBeRetryable() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.NOT_FOUND, "test");
      assertFalse(ex.isRetryable(), "NOT_FOUND should NOT be retryable");
    }

    @Test
    @DisplayName("PERMISSION_DENIED should not be retryable")
    void permissionDeniedShouldNotBeRetryable() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.PERMISSION_DENIED, "test");
      assertFalse(ex.isRetryable(), "PERMISSION_DENIED should NOT be retryable");
    }

    @Test
    @DisplayName("UNKNOWN should not be retryable")
    void unknownShouldNotBeRetryable() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.UNKNOWN, "test");
      assertFalse(ex.isRetryable(), "UNKNOWN should NOT be retryable");
    }

    @Test
    @DisplayName("null error type should not be retryable")
    void nullErrorTypeShouldNotBeRetryable() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(null, "test", null, null, null, null);
      assertFalse(ex.isRetryable(), "null error type should NOT be retryable");
    }
  }

  @Nested
  @DisplayName("FromErrno Edge Case Mutation Tests")
  class FromErrnoEdgeCaseMutationTests {

    @Test
    @DisplayName("fromErrno should return correct type for all known errno codes")
    void fromErrnoShouldReturnCorrectTypeForAllKnownErrnoCodes() {
      for (final FileSystemErrorType type : FileSystemErrorType.values()) {
        if (type != FileSystemErrorType.UNKNOWN) {
          assertEquals(
              type,
              FileSystemErrorType.fromErrno(type.getErrnoCode()),
              "fromErrno(" + type.getErrnoCode() + ") should return " + type.name());
        }
      }
    }

    @Test
    @DisplayName("fromErrno should return UNKNOWN for negative errno")
    void fromErrnoShouldReturnUnknownForNegativeErrno() {
      assertEquals(
          FileSystemErrorType.UNKNOWN,
          FileSystemErrorType.fromErrno(-999),
          "Negative errno should map to UNKNOWN");
    }

    @Test
    @DisplayName("fromErrno should return UNKNOWN for zero")
    void fromErrnoShouldReturnUnknownForZero() {
      assertEquals(
          FileSystemErrorType.UNKNOWN,
          FileSystemErrorType.fromErrno(0),
          "Zero errno should map to UNKNOWN");
    }

    @Test
    @DisplayName("fromErrno should return UNKNOWN for large unrecognized errno")
    void fromErrnoShouldReturnUnknownForLargeUnrecognizedErrno() {
      assertEquals(
          FileSystemErrorType.UNKNOWN,
          FileSystemErrorType.fromErrno(Integer.MAX_VALUE),
          "Large errno should map to UNKNOWN");
    }
  }

  @Nested
  @DisplayName("Getter Return Value Mutation Tests")
  class GetterReturnValueMutationTests {

    @Test
    @DisplayName("getFileSystemErrorType returns exact value set in constructor")
    void getFileSystemErrorTypeReturnsExactValue() {
      for (final FileSystemErrorType type : FileSystemErrorType.values()) {
        final WasiFileSystemException ex = new WasiFileSystemException(type, "test");
        assertSame(
            type,
            ex.getFileSystemErrorType(),
            "getFileSystemErrorType should return exact value for " + type.name());
      }
    }

    @Test
    @DisplayName("getFilePath returns exact value set in constructor")
    void getFilePathReturnsExactValue() {
      final String path = "/unique/path/12345";
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "test", path, null, null, null);
      assertSame(path, ex.getFilePath(), "getFilePath should return exact same string instance");
    }

    @Test
    @DisplayName("getFileOperation returns exact value set in constructor")
    void getFileOperationReturnsExactValue() {
      final String operation = "unique_operation_12345";
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "test", null, operation, null, null);
      assertSame(
          operation,
          ex.getFileOperation(),
          "getFileOperation should return exact same string instance");
    }

    @Test
    @DisplayName("getErrnoCode returns exact value set in constructor")
    void getErrnoCodeReturnsExactValue() {
      final Integer errno = Integer.valueOf(12345);
      final WasiFileSystemException ex =
          new WasiFileSystemException(FileSystemErrorType.UNKNOWN, "test", null, null, errno, null);
      assertEquals(errno, ex.getErrnoCode(), "getErrnoCode should return exact value");
    }

    @Test
    @DisplayName("getErrnoCode returns default from error type when null")
    void getErrnoCodeReturnsDefaultFromErrorTypeWhenNull() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.NOT_FOUND, "test", null, null, null, null);
      assertEquals(
          Integer.valueOf(FileSystemErrorType.NOT_FOUND.getErrnoCode()),
          ex.getErrnoCode(),
          "getErrnoCode should return default errno from error type");
    }

    @Test
    @DisplayName("getCause returns exact value set in constructor")
    void getCauseReturnsExactValue() {
      final Throwable cause = new java.lang.RuntimeException("original cause");
      final WasiFileSystemException ex =
          new WasiFileSystemException(
              FileSystemErrorType.IO_ERROR, "test", null, null, null, cause);
      assertSame(cause, ex.getCause(), "getCause should return exact same Throwable instance");
    }
  }

  @Nested
  @DisplayName("FileSystemErrorType Description Mutation Tests")
  class FileSystemErrorTypeDescriptionMutationTests {

    @Test
    @DisplayName("All descriptions should be distinct")
    void allDescriptionsShouldBeDistinct() {
      final java.util.Set<String> descriptions = new java.util.HashSet<>();
      for (final FileSystemErrorType type : FileSystemErrorType.values()) {
        final String desc = type.getDescription();
        assertFalse(
            descriptions.contains(desc),
            "Description for " + type.name() + " should be distinct: " + desc);
        descriptions.add(desc);
      }
      assertEquals(
          FileSystemErrorType.values().length,
          descriptions.size(),
          "All error types should have distinct descriptions");
    }

    @Test
    @DisplayName("All errno codes should be distinct except UNKNOWN")
    void allErrnoCodesShouldBeDistinctExceptUnknown() {
      final java.util.Set<Integer> errnoCodes = new java.util.HashSet<>();
      for (final FileSystemErrorType type : FileSystemErrorType.values()) {
        if (type != FileSystemErrorType.UNKNOWN) {
          final int errno = type.getErrnoCode();
          assertFalse(
              errnoCodes.contains(errno),
              "Errno for " + type.name() + " should be distinct: " + errno);
          errnoCodes.add(errno);
        }
      }
    }
  }

  @Nested
  @DisplayName("Null Error Type Handling Mutation Tests")
  class NullErrorTypeHandlingMutationTests {

    @Test
    @DisplayName("Null error type is normalized to UNKNOWN")
    void nullErrorTypeIsNormalizedToUnknown() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(null, "test", null, null, null, null);
      assertEquals(
          FileSystemErrorType.UNKNOWN,
          ex.getFileSystemErrorType(),
          "Null error type should be normalized to UNKNOWN");
    }

    @Test
    @DisplayName("Null error type uses UNKNOWN errno code")
    void nullErrorTypeUsesUnknownErrnoCode() {
      final WasiFileSystemException ex =
          new WasiFileSystemException(null, "test", null, null, null, null);
      assertEquals(
          Integer.valueOf(FileSystemErrorType.UNKNOWN.getErrnoCode()),
          ex.getErrnoCode(),
          "Null error type should use UNKNOWN errno code (-1)");
    }
  }
}

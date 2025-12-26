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

package ai.tegmentum.wasmtime4j.jni.wasi.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiFileSystemException} class.
 *
 * <p>This test class verifies WasiFileSystemException constructors and factory methods.
 */
@DisplayName("WasiFileSystemException Tests")
class WasiFileSystemExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiFileSystemException should be final")
    void shouldBeFinal() {
      assertTrue(Modifier.isFinal(WasiFileSystemException.class.getModifiers()),
          "WasiFileSystemException should be final");
    }

    @Test
    @DisplayName("WasiFileSystemException should extend WasiException")
    void shouldExtendWasiException() {
      assertTrue(WasiException.class.isAssignableFrom(WasiFileSystemException.class),
          "WasiFileSystemException should extend WasiException");
    }
  }

  @Nested
  @DisplayName("Constructor with file path Tests")
  class ConstructorWithFilePathTests {

    @Test
    @DisplayName("Should create exception with message, error code, operation, and file path")
    void shouldCreateExceptionWithFilePath() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          "File not found",
          WasiErrorCode.ENOENT,
          "open",
          "/path/to/missing");

      assertEquals("/path/to/missing", exception.getFilePath(), "File path should match");
      assertEquals(-1, exception.getFileDescriptor(), "File descriptor should be -1");
      assertTrue(exception.hasFilePath(), "Should have file path");
      assertFalse(exception.hasFileDescriptor(), "Should not have file descriptor");
    }

    @Test
    @DisplayName("Should include file path in message")
    void shouldIncludeFilePathInMessage() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          "Read error",
          WasiErrorCode.EIO,
          "read",
          "/data/file.txt");

      assertTrue(exception.getMessage().contains("/data/file.txt"),
          "Message should contain file path");
    }
  }

  @Nested
  @DisplayName("Constructor with file descriptor Tests")
  class ConstructorWithFileDescriptorTests {

    @Test
    @DisplayName("Should create exception with file descriptor")
    void shouldCreateExceptionWithFileDescriptor() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          "Bad descriptor",
          WasiErrorCode.EBADF,
          "read",
          42);

      assertNull(exception.getFilePath(), "File path should be null");
      assertEquals(42, exception.getFileDescriptor(), "File descriptor should match");
      assertFalse(exception.hasFilePath(), "Should not have file path");
      assertTrue(exception.hasFileDescriptor(), "Should have file descriptor");
    }

    @Test
    @DisplayName("Should include file descriptor in resource")
    void shouldIncludeFileDescriptorInResource() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          "Error",
          WasiErrorCode.EBADF,
          "close",
          100);

      assertEquals("fd:100", exception.getResource(), "Resource should contain fd prefix");
    }
  }

  @Nested
  @DisplayName("Constructor with cause Tests")
  class ConstructorWithCauseTests {

    @Test
    @DisplayName("Should preserve cause")
    void shouldPreserveCause() {
      final RuntimeException cause = new RuntimeException("IO failure");
      final WasiFileSystemException exception = new WasiFileSystemException(
          "File error",
          WasiErrorCode.EIO,
          "write",
          "/output.txt",
          cause);

      assertSame(cause, exception.getCause(), "Cause should be preserved");
      assertEquals("/output.txt", exception.getFilePath(), "File path should match");
    }
  }

  @Nested
  @DisplayName("Error code only constructor Tests")
  class ErrorCodeOnlyConstructorTests {

    @Test
    @DisplayName("Should create exception from error code, operation, and path")
    void shouldCreateExceptionFromErrorCodeOperationAndPath() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          WasiErrorCode.EACCES,
          "open",
          "/restricted/file");

      assertEquals(WasiErrorCode.EACCES, exception.getErrorCode(), "Error code should match");
      assertEquals("open", exception.getOperation(), "Operation should match");
      assertEquals("/restricted/file", exception.getFilePath(), "File path should match");
      assertTrue(exception.getMessage().contains("Permission denied"),
          "Message should contain error description");
    }

    @Test
    @DisplayName("Should create exception from error code, operation, and descriptor")
    void shouldCreateExceptionFromErrorCodeOperationAndDescriptor() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          WasiErrorCode.EBADF,
          "read",
          999);

      assertEquals(WasiErrorCode.EBADF, exception.getErrorCode(), "Error code should match");
      assertEquals(999, exception.getFileDescriptor(), "File descriptor should match");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("fileNotFound should create ENOENT exception")
    void fileNotFoundShouldCreateEnoentException() {
      final WasiFileSystemException exception = WasiFileSystemException.fileNotFound(
          "stat", "/missing/file");

      assertEquals(WasiErrorCode.ENOENT, exception.getErrorCode(), "Should be ENOENT");
      assertEquals("stat", exception.getOperation(), "Operation should match");
      assertEquals("/missing/file", exception.getFilePath(), "File path should match");
    }

    @Test
    @DisplayName("permissionDenied should create EACCES exception")
    void permissionDeniedShouldCreateEaccesException() {
      final WasiFileSystemException exception = WasiFileSystemException.permissionDenied(
          "write", "/protected/file");

      assertEquals(WasiErrorCode.EACCES, exception.getErrorCode(), "Should be EACCES");
    }

    @Test
    @DisplayName("ioError should create EIO exception")
    void ioErrorShouldCreateEioException() {
      final WasiFileSystemException exception = WasiFileSystemException.ioError(
          "read", "/corrupted/file");

      assertEquals(WasiErrorCode.EIO, exception.getErrorCode(), "Should be EIO");
    }

    @Test
    @DisplayName("noSpaceLeft should create ENOSPC exception")
    void noSpaceLeftShouldCreateEnospcException() {
      final WasiFileSystemException exception = WasiFileSystemException.noSpaceLeft(
          "write", "/disk/full");

      assertEquals(WasiErrorCode.ENOSPC, exception.getErrorCode(), "Should be ENOSPC");
    }

    @Test
    @DisplayName("badFileDescriptor should create EBADF exception")
    void badFileDescriptorShouldCreateEbadfException() {
      final WasiFileSystemException exception = WasiFileSystemException.badFileDescriptor(
          "close", 999);

      assertEquals(WasiErrorCode.EBADF, exception.getErrorCode(), "Should be EBADF");
      assertEquals(999, exception.getFileDescriptor(), "Descriptor should match");
    }

    @Test
    @DisplayName("fileExists should create EEXIST exception")
    void fileExistsShouldCreateEexistException() {
      final WasiFileSystemException exception = WasiFileSystemException.fileExists(
          "create", "/existing/file");

      assertEquals(WasiErrorCode.EEXIST, exception.getErrorCode(), "Should be EEXIST");
    }

    @Test
    @DisplayName("directoryNotEmpty should create ENOTEMPTY exception")
    void directoryNotEmptyShouldCreateEnotemptyException() {
      final WasiFileSystemException exception = WasiFileSystemException.directoryNotEmpty(
          "rmdir", "/non/empty/dir");

      assertEquals(WasiErrorCode.ENOTEMPTY, exception.getErrorCode(), "Should be ENOTEMPTY");
    }

    @Test
    @DisplayName("readOnlyFileSystem should create EROFS exception")
    void readOnlyFileSystemShouldCreateErofsException() {
      final WasiFileSystemException exception = WasiFileSystemException.readOnlyFileSystem(
          "write", "/readonly/fs/file");

      assertEquals(WasiErrorCode.EROFS, exception.getErrorCode(), "Should be EROFS");
    }

    @Test
    @DisplayName("fileNameTooLong should create ENAMETOOLONG exception")
    void fileNameTooLongShouldCreateEnametoolongException() {
      final WasiFileSystemException exception = WasiFileSystemException.fileNameTooLong(
          "create", "/very/very/very/very/long/name...");

      assertEquals(WasiErrorCode.ENAMETOOLONG, exception.getErrorCode(),
          "Should be ENAMETOOLONG");
    }

    @Test
    @DisplayName("tooManySymbolicLinks should create ELOOP exception")
    void tooManySymbolicLinksShouldCreateEloopException() {
      final WasiFileSystemException exception = WasiFileSystemException.tooManySymbolicLinks(
          "stat", "/symlink/loop");

      assertEquals(WasiErrorCode.ELOOP, exception.getErrorCode(), "Should be ELOOP");
    }
  }

  @Nested
  @DisplayName("String error code constructor Tests")
  class StringErrorCodeConstructorTests {

    @Test
    @DisplayName("Should parse valid error code string")
    void shouldParseValidErrorCodeString() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          "Test error", "ENOENT");

      assertEquals(WasiErrorCode.ENOENT, exception.getErrorCode(),
          "Should parse ENOENT correctly");
    }

    @Test
    @DisplayName("Should fallback to EIO for invalid error code string")
    void shouldFallbackToEioForInvalidErrorCodeString() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          "Test error", "INVALID_CODE");

      assertEquals(WasiErrorCode.EIO, exception.getErrorCode(),
          "Should fallback to EIO for invalid code");
    }

    @Test
    @DisplayName("Should handle cause with string error code")
    void shouldHandleCauseWithStringErrorCode() {
      final RuntimeException cause = new RuntimeException("Underlying error");
      final WasiFileSystemException exception = new WasiFileSystemException(
          "Test error", "EACCES", cause);

      assertEquals(WasiErrorCode.EACCES, exception.getErrorCode(), "Should parse EACCES");
      assertSame(cause, exception.getCause(), "Cause should be preserved");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be throwable and catchable")
    void shouldBeThrowableAndCatchable() {
      try {
        throw WasiFileSystemException.fileNotFound("open", "/missing");
      } catch (WasiFileSystemException e) {
        assertEquals(WasiErrorCode.ENOENT, e.getErrorCode(), "Should catch with correct code");
        assertEquals("/missing", e.getFilePath(), "Should have correct file path");
      }
    }

    @Test
    @DisplayName("Should be catchable as WasiException")
    void shouldBeCatchableAsWasiException() {
      try {
        throw WasiFileSystemException.permissionDenied("write", "/secret");
      } catch (WasiException e) {
        assertTrue(e instanceof WasiFileSystemException,
            "Should be instance of WasiFileSystemException");
        assertTrue(e.isFileSystemError(), "Should be file system error");
      }
    }

    @Test
    @DisplayName("File path and descriptor should be mutually exclusive")
    void filePathAndDescriptorShouldBeMutuallyExclusive() {
      final WasiFileSystemException pathException = new WasiFileSystemException(
          "Error", WasiErrorCode.EIO, "read", "/file");
      final WasiFileSystemException fdException = new WasiFileSystemException(
          "Error", WasiErrorCode.EIO, "read", 42);

      assertTrue(pathException.hasFilePath(), "Path exception should have path");
      assertFalse(pathException.hasFileDescriptor(), "Path exception should not have fd");
      assertFalse(fdException.hasFilePath(), "FD exception should not have path");
      assertTrue(fdException.hasFileDescriptor(), "FD exception should have fd");
    }
  }
}

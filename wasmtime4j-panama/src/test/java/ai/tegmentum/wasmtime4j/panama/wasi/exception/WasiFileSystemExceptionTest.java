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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Panama {@link WasiFileSystemException} class.
 *
 * <p>This test class verifies WasiFileSystemException constructors and behavior.
 */
@DisplayName("Panama WasiFileSystemException Tests")
class WasiFileSystemExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiFileSystemException should extend PanamaException")
    void shouldExtendPanamaException() {
      assertTrue(PanamaException.class.isAssignableFrom(WasiFileSystemException.class),
          "WasiFileSystemException should extend PanamaException");
    }

    @Test
    @DisplayName("WasiFileSystemException should be a RuntimeException")
    void shouldBeRuntimeException() {
      assertTrue(RuntimeException.class.isAssignableFrom(WasiFileSystemException.class),
          "WasiFileSystemException should be a RuntimeException");
    }
  }

  @Nested
  @DisplayName("Constructor(String, String) Tests")
  class ConstructorWithErrorCodeTests {

    @Test
    @DisplayName("Should create exception with message and error code")
    void shouldCreateExceptionWithMessageAndErrorCode() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          "File not found", "ENOENT");

      assertEquals("File not found", exception.getMessage(), "Message should match");
      assertEquals("ENOENT", exception.getWasiErrorCode(), "Error code should match");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Should handle various error codes")
    void shouldHandleVariousErrorCodes() {
      final String[] errorCodes = {"ENOENT", "EACCES", "EIO", "EBADF", "EINVAL"};
      for (String errorCode : errorCodes) {
        final WasiFileSystemException exception = new WasiFileSystemException(
            "Error occurred", errorCode);
        assertEquals(errorCode, exception.getWasiErrorCode(),
            "Error code should match: " + errorCode);
      }
    }

    @Test
    @DisplayName("Should handle null error code")
    void shouldHandleNullErrorCode() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          "Unknown error", null);

      assertEquals("Unknown error", exception.getMessage(), "Message should match");
      assertNull(exception.getWasiErrorCode(), "Error code should be null");
    }
  }

  @Nested
  @DisplayName("Constructor(String, String, Throwable) Tests")
  class ConstructorWithCauseTests {

    @Test
    @DisplayName("Should create exception with message, error code, and cause")
    void shouldCreateExceptionWithMessageErrorCodeAndCause() {
      final Throwable cause = new RuntimeException("Underlying error");
      final WasiFileSystemException exception = new WasiFileSystemException(
          "File operation failed", "EIO", cause);

      assertEquals("File operation failed", exception.getMessage(), "Message should match");
      assertEquals("EIO", exception.getWasiErrorCode(), "Error code should match");
      assertSame(cause, exception.getCause(), "Cause should match");
    }

    @Test
    @DisplayName("Should handle null cause")
    void shouldHandleNullCause() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          "Error", "ENOENT", null);

      assertEquals("ENOENT", exception.getWasiErrorCode(), "Error code should match");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Should preserve exception chain")
    void shouldPreserveExceptionChain() {
      final Exception root = new IllegalStateException("Root cause");
      final RuntimeException middle = new RuntimeException("Middle", root);
      final WasiFileSystemException exception = new WasiFileSystemException(
          "Top level", "EIO", middle);

      assertSame(middle, exception.getCause(), "Direct cause should match");
      assertSame(root, exception.getCause().getCause(), "Root cause should be preserved");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include class name, error code, and message")
    void toStringShouldIncludeClassNameErrorCodeAndMessage() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          "File not found", "ENOENT");
      final String str = exception.toString();

      assertTrue(str.contains("WasiFileSystemException"), "Should contain class name");
      assertTrue(str.contains("ENOENT"), "Should contain error code");
      assertTrue(str.contains("File not found"), "Should contain message");
    }

    @Test
    @DisplayName("toString should format correctly")
    void toStringShouldFormatCorrectly() {
      final WasiFileSystemException exception = new WasiFileSystemException(
          "Permission denied", "EACCES");
      final String expected = "WasiFileSystemException[EACCES]: Permission denied";

      assertEquals(expected, exception.toString(), "toString should format correctly");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be throwable and catchable")
    void shouldBeThrowableAndCatchable() {
      try {
        throw new WasiFileSystemException("File not found", "ENOENT");
      } catch (WasiFileSystemException e) {
        assertEquals("ENOENT", e.getWasiErrorCode(), "Should catch with correct error code");
        assertEquals("File not found", e.getMessage(), "Should catch with correct message");
      }
    }

    @Test
    @DisplayName("Should be catchable as PanamaException")
    void shouldBeCatchableAsPanamaException() {
      try {
        throw new WasiFileSystemException("Error", "EIO");
      } catch (PanamaException e) {
        assertTrue(e instanceof WasiFileSystemException,
            "Should be instance of WasiFileSystemException");
      }
    }

    @Test
    @DisplayName("Should be catchable as RuntimeException")
    void shouldBeCatchableAsRuntimeException() {
      try {
        throw new WasiFileSystemException("Error", "EBADF");
      } catch (RuntimeException e) {
        assertTrue(e instanceof WasiFileSystemException,
            "Should be instance of WasiFileSystemException");
      }
    }

    @Test
    @DisplayName("Typical usage pattern for file operations")
    void typicalUsagePatternForFileOperations() {
      final String filePath = "/path/to/missing/file";
      final WasiFileSystemException exception;

      try {
        // Simulate file operation failure
        throw new java.io.FileNotFoundException(filePath);
      } catch (java.io.FileNotFoundException e) {
        exception = new WasiFileSystemException(
            "File not found: " + filePath, "ENOENT", e);
      }

      assertNotNull(exception, "Exception should be created");
      assertEquals("ENOENT", exception.getWasiErrorCode(), "Should have ENOENT error code");
      assertTrue(exception.getMessage().contains(filePath), "Message should contain file path");
      assertNotNull(exception.getCause(), "Cause should be present");
    }

    @Test
    @DisplayName("Common error code scenarios")
    void commonErrorCodeScenarios() {
      // File not found
      final WasiFileSystemException notFound = new WasiFileSystemException(
          "File not found", "ENOENT");
      assertEquals("ENOENT", notFound.getWasiErrorCode());

      // Permission denied
      final WasiFileSystemException permDenied = new WasiFileSystemException(
          "Permission denied", "EACCES");
      assertEquals("EACCES", permDenied.getWasiErrorCode());

      // I/O error
      final WasiFileSystemException ioError = new WasiFileSystemException(
          "I/O error", "EIO");
      assertEquals("EIO", ioError.getWasiErrorCode());

      // Bad file descriptor
      final WasiFileSystemException badFd = new WasiFileSystemException(
          "Bad file descriptor", "EBADF");
      assertEquals("EBADF", badFd.getWasiErrorCode());
    }
  }
}

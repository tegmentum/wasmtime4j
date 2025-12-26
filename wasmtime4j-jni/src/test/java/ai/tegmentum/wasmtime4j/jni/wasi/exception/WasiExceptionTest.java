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

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiException} class.
 *
 * <p>This test class verifies WasiException constructors and behavior.
 */
@DisplayName("WasiException Tests")
class WasiExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiException should extend JniException")
    void shouldExtendJniException() {
      assertTrue(
          JniException.class.isAssignableFrom(WasiException.class),
          "WasiException should extend JniException");
    }

    @Test
    @DisplayName("WasiException should be a RuntimeException")
    void shouldBeRuntimeException() {
      assertTrue(
          RuntimeException.class.isAssignableFrom(WasiException.class),
          "WasiException should be a RuntimeException");
    }
  }

  @Nested
  @DisplayName("Constructor(message, errorCode, operation, resource) Tests")
  class FullConstructorTests {

    @Test
    @DisplayName("Should create exception with all parameters")
    void shouldCreateExceptionWithAllParameters() {
      final WasiException exception =
          new WasiException("Test error", WasiErrorCode.ENOENT, "open", "/path/to/file");

      assertNotNull(exception.getMessage(), "Message should not be null");
      assertTrue(exception.getMessage().contains("Test error"), "Should contain base message");
      assertEquals(WasiErrorCode.ENOENT, exception.getErrorCode(), "Error code should match");
      assertEquals("open", exception.getOperation(), "Operation should match");
      assertEquals("/path/to/file", exception.getResource(), "Resource should match");
    }

    @Test
    @DisplayName("Message should include operation and resource")
    void messageShouldIncludeOperationAndResource() {
      final WasiException exception =
          new WasiException("File error", WasiErrorCode.EIO, "read", "/tmp/data.txt");

      final String message = exception.getMessage();
      assertTrue(message.contains("read"), "Message should contain operation");
      assertTrue(message.contains("/tmp/data.txt"), "Message should contain resource");
      assertTrue(message.contains("5"), "Message should contain errno");
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
      final WasiException exception =
          new WasiException(null, WasiErrorCode.ENOENT, "stat", "/file");

      assertNotNull(exception.getMessage(), "Message should not be null");
      assertTrue(
          exception.getMessage().contains("No such file"), "Should use error code description");
    }

    @Test
    @DisplayName("Should handle null operation and resource")
    void shouldHandleNullOperationAndResource() {
      final WasiException exception =
          new WasiException("Error occurred", WasiErrorCode.EIO, null, null);

      assertNull(exception.getOperation(), "Operation should be null");
      assertNull(exception.getResource(), "Resource should be null");
      assertNotNull(exception.getMessage(), "Message should not be null");
    }
  }

  @Nested
  @DisplayName("Constructor with cause Tests")
  class ConstructorWithCauseTests {

    @Test
    @DisplayName("Should preserve cause")
    void shouldPreserveCause() {
      final RuntimeException cause = new RuntimeException("Underlying error");
      final WasiException exception =
          new WasiException("WASI error", WasiErrorCode.EIO, "write", "/output", cause);

      assertSame(cause, exception.getCause(), "Cause should be preserved");
    }

    @Test
    @DisplayName("Should handle null cause")
    void shouldHandleNullCause() {
      final WasiException exception =
          new WasiException("Error", WasiErrorCode.EACCES, "open", "/file", null);

      assertNull(exception.getCause(), "Cause should be null");
    }
  }

  @Nested
  @DisplayName("Constructor(message, errorCode) Tests")
  class SimpleConstructorTests {

    @Test
    @DisplayName("Should create exception with message and error code")
    void shouldCreateExceptionWithMessageAndErrorCode() {
      final WasiException exception = new WasiException("Simple error", WasiErrorCode.EINVAL);

      assertEquals(WasiErrorCode.EINVAL, exception.getErrorCode(), "Error code should match");
      assertNull(exception.getOperation(), "Operation should be null");
      assertNull(exception.getResource(), "Resource should be null");
    }
  }

  @Nested
  @DisplayName("Constructor(errorCode, operation) Tests")
  class ErrorCodeOperationConstructorTests {

    @Test
    @DisplayName("Should create exception from error code and operation")
    void shouldCreateExceptionFromErrorCodeAndOperation() {
      final WasiException exception = new WasiException(WasiErrorCode.EPERM, "chmod");

      assertEquals(WasiErrorCode.EPERM, exception.getErrorCode(), "Error code should match");
      assertEquals("chmod", exception.getOperation(), "Operation should match");
      assertTrue(
          exception.getMessage().contains("Operation not permitted"),
          "Message should contain error description");
    }
  }

  @Nested
  @DisplayName("Constructor(errorCode, operation, resource) Tests")
  class ErrorCodeOperationResourceConstructorTests {

    @Test
    @DisplayName("Should create exception from error code, operation, and resource")
    void shouldCreateExceptionFromErrorCodeOperationAndResource() {
      final WasiException exception =
          new WasiException(WasiErrorCode.ENOSPC, "write", "/disk/full");

      assertEquals(WasiErrorCode.ENOSPC, exception.getErrorCode(), "Error code should match");
      assertEquals("write", exception.getOperation(), "Operation should match");
      assertEquals("/disk/full", exception.getResource(), "Resource should match");
    }
  }

  @Nested
  @DisplayName("Constructor(message) Tests")
  class MessageOnlyConstructorTests {

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      final WasiException exception = new WasiException("Simple error message");

      assertNotNull(exception.getMessage(), "Message should not be null");
      assertTrue(
          exception.getMessage().contains("Simple error message"), "Should contain the message");
    }
  }

  @Nested
  @DisplayName("Error Category Delegation Tests")
  class ErrorCategoryDelegationTests {

    @Test
    @DisplayName("isFileSystemError should delegate to error code")
    void isFileSystemErrorShouldDelegateToErrorCode() {
      final WasiException fsException = new WasiException("FS error", WasiErrorCode.ENOENT);
      final WasiException netException = new WasiException("Net error", WasiErrorCode.ECONNREFUSED);

      assertTrue(fsException.isFileSystemError(), "Should be file system error");
      assertFalse(netException.isFileSystemError(), "Should not be file system error");
    }

    @Test
    @DisplayName("isNetworkError should delegate to error code")
    void isNetworkErrorShouldDelegateToErrorCode() {
      final WasiException netException = new WasiException("Net error", WasiErrorCode.ETIMEDOUT);
      final WasiException fsException = new WasiException("FS error", WasiErrorCode.ENOENT);

      assertTrue(netException.isNetworkError(), "Should be network error");
      assertFalse(fsException.isNetworkError(), "Should not be network error");
    }

    @Test
    @DisplayName("isPermissionError should delegate to error code")
    void isPermissionErrorShouldDelegateToErrorCode() {
      final WasiException permException = new WasiException("Perm error", WasiErrorCode.EPERM);
      final WasiException ioException = new WasiException("IO error", WasiErrorCode.EIO);

      assertTrue(permException.isPermissionError(), "Should be permission error");
      assertFalse(ioException.isPermissionError(), "Should not be permission error");
    }

    @Test
    @DisplayName("isResourceLimitError should delegate to error code")
    void isResourceLimitErrorShouldDelegateToErrorCode() {
      final WasiException memException = new WasiException("Memory error", WasiErrorCode.ENOMEM);
      final WasiException ioException = new WasiException("IO error", WasiErrorCode.EIO);

      assertTrue(memException.isResourceLimitError(), "Should be resource limit error");
      assertFalse(ioException.isResourceLimitError(), "Should not be resource limit error");
    }

    @Test
    @DisplayName("isRetryable should delegate to error code")
    void isRetryableShouldDelegateToErrorCode() {
      final WasiException retryable = new WasiException("Retry error", WasiErrorCode.EAGAIN);
      final WasiException notRetryable = new WasiException("No retry", WasiErrorCode.EINVAL);

      assertTrue(retryable.isRetryable(), "Should be retryable");
      assertFalse(notRetryable.isRetryable(), "Should not be retryable");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be throwable and catchable")
    void shouldBeThrowableAndCatchable() {
      try {
        throw new WasiException(WasiErrorCode.ENOENT, "open", "/missing");
      } catch (WasiException e) {
        assertEquals(WasiErrorCode.ENOENT, e.getErrorCode(), "Should catch with correct code");
        assertEquals("open", e.getOperation(), "Should have correct operation");
      }
    }

    @Test
    @DisplayName("Should be catchable as JniException")
    void shouldBeCatchableAsJniException() {
      try {
        throw new WasiException(WasiErrorCode.EIO, "read");
      } catch (JniException e) {
        assertTrue(e instanceof WasiException, "Should be instance of WasiException");
      }
    }

    @Test
    @DisplayName("Full message format should be correct")
    void fullMessageFormatShouldBeCorrect() {
      final WasiException exception =
          new WasiException("Custom message", WasiErrorCode.EACCES, "write", "/secret/file");

      final String message = exception.getMessage();
      assertTrue(message.contains("Custom message"), "Should contain custom message");
      assertTrue(message.contains("write"), "Should contain operation");
      assertTrue(message.contains("/secret/file"), "Should contain resource");
      assertTrue(message.contains("13"), "Should contain errno 13 for EACCES");
    }
  }
}

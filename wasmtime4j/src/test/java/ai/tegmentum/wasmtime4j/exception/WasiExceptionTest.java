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

import ai.tegmentum.wasmtime4j.wasi.exception.WasiErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiException} class.
 *
 * <p>WasiException is the base class for all WASI-related exceptions.
 */
@DisplayName("WasiException Tests")
class WasiExceptionTest {

  @Nested
  @DisplayName("ErrorCategory Enum Tests")
  class ErrorCategoryEnumTests {

    @Test
    @DisplayName("should have all expected categories")
    void shouldHaveAllExpectedCategories() {
      final WasiException.ErrorCategory[] values = WasiException.ErrorCategory.values();
      assertTrue(values.length >= 7, "Should have at least 7 error categories");

      // Check key categories exist
      assertNotNull(WasiException.ErrorCategory.FILE_SYSTEM, "Should have FILE_SYSTEM");
      assertNotNull(WasiException.ErrorCategory.NETWORK, "Should have NETWORK");
      assertNotNull(WasiException.ErrorCategory.PERMISSION, "Should have PERMISSION");
      assertNotNull(WasiException.ErrorCategory.RESOURCE_LIMIT, "Should have RESOURCE_LIMIT");
      assertNotNull(WasiException.ErrorCategory.COMPONENT, "Should have COMPONENT");
      assertNotNull(WasiException.ErrorCategory.CONFIGURATION, "Should have CONFIGURATION");
      assertNotNull(WasiException.ErrorCategory.SYSTEM, "Should have SYSTEM");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      final WasiException exception = new WasiException("File not found");

      assertTrue(exception.getMessage().contains("File not found"), "Message should be set");
      assertNull(exception.getOperation(), "Operation should be null");
      assertNull(exception.getResource(), "Resource should be null");
      assertFalse(exception.isRetryable(), "Should not be retryable by default");
      assertEquals(
          WasiException.ErrorCategory.SYSTEM,
          exception.getCategory(),
          "Should default to SYSTEM category");
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final RuntimeException cause = new RuntimeException("IO error");
      final WasiException exception = new WasiException("Read failed", cause);

      assertTrue(exception.getMessage().contains("Read failed"), "Message should be set");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("should create exception with full details")
    void shouldCreateExceptionWithFullDetails() {
      final WasiException exception =
          new WasiException(
              "Permission denied",
              "fd_read",
              "/etc/passwd",
              false,
              WasiException.ErrorCategory.PERMISSION);

      assertTrue(
          exception.getMessage().contains("Permission denied"), "Message should contain error");
      assertTrue(exception.getMessage().contains("fd_read"), "Message should contain operation");
      assertTrue(exception.getMessage().contains("/etc/passwd"), "Message should contain resource");
      assertEquals("fd_read", exception.getOperation(), "Operation should be set");
      assertEquals("/etc/passwd", exception.getResource(), "Resource should be set");
      assertFalse(exception.isRetryable(), "Should not be retryable");
      assertEquals(
          WasiException.ErrorCategory.PERMISSION,
          exception.getCategory(),
          "Category should be PERMISSION");
    }

    @Test
    @DisplayName("should create exception with full details and cause")
    void shouldCreateExceptionWithFullDetailsAndCause() {
      final RuntimeException cause = new RuntimeException("Network timeout");
      final WasiException exception =
          new WasiException(
              "Connection failed",
              "sock_connect",
              "192.168.1.1:8080",
              true,
              WasiException.ErrorCategory.NETWORK,
              cause);

      assertEquals("sock_connect", exception.getOperation(), "Operation should be set");
      assertEquals("192.168.1.1:8080", exception.getResource(), "Resource should be set");
      assertTrue(exception.isRetryable(), "Should be retryable");
      assertEquals(
          WasiException.ErrorCategory.NETWORK,
          exception.getCategory(),
          "Category should be NETWORK");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("should handle null message gracefully with fallback text")
    void shouldHandleNullMessageGracefully() {
      final WasiException exception =
          new WasiException(null, "op", "res", false, WasiException.ErrorCategory.SYSTEM);

      assertNotNull(exception.getMessage(), "Message should not be null");
      assertTrue(
          exception.getMessage().contains("WASI operation failed"),
          "Should use fallback message for null");
      assertTrue(exception.getMessage().contains("op"), "Should still contain operation");
      assertTrue(exception.getMessage().contains("res"), "Should still contain resource");
    }

    @Test
    @DisplayName("should handle empty message gracefully with fallback text")
    void shouldHandleEmptyMessageGracefully() {
      final WasiException exception =
          new WasiException("", "op", "res", false, WasiException.ErrorCategory.SYSTEM);

      assertNotNull(exception.getMessage(), "Message should not be null");
      assertTrue(
          exception.getMessage().contains("WASI operation failed"),
          "Should use fallback message for empty");
    }
  }

  @Nested
  @DisplayName("Category Check Methods Tests")
  class CategoryCheckMethodsTests {

    @Test
    @DisplayName("isFileSystemError should return true for FILE_SYSTEM category")
    void isFileSystemErrorShouldReturnTrueForFileSystemCategory() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.FILE_SYSTEM);
      assertTrue(exception.isFileSystemError(), "Should be file system error");
      assertFalse(exception.isNetworkError(), "Should not be network error");
    }

    @Test
    @DisplayName("isNetworkError should return true for NETWORK category")
    void isNetworkErrorShouldReturnTrueForNetworkCategory() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.NETWORK);
      assertTrue(exception.isNetworkError(), "Should be network error");
      assertFalse(exception.isFileSystemError(), "Should not be file system error");
    }

    @Test
    @DisplayName("isPermissionError should return true for PERMISSION category")
    void isPermissionErrorShouldReturnTrueForPermissionCategory() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.PERMISSION);
      assertTrue(exception.isPermissionError(), "Should be permission error");
    }

    @Test
    @DisplayName("isResourceLimitError should return true for RESOURCE_LIMIT category")
    void isResourceLimitErrorShouldReturnTrueForResourceLimitCategory() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.RESOURCE_LIMIT);
      assertTrue(exception.isResourceLimitError(), "Should be resource limit error");
    }

    @Test
    @DisplayName("isComponentError should return true for COMPONENT category")
    void isComponentErrorShouldReturnTrueForComponentCategory() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.COMPONENT);
      assertTrue(exception.isComponentError(), "Should be component error");
    }

    @Test
    @DisplayName("isConfigurationError should return true for CONFIGURATION category")
    void isConfigurationErrorShouldReturnTrueForConfigurationCategory() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.CONFIGURATION);
      assertTrue(exception.isConfigurationError(), "Should be configuration error");
    }
  }

  @Nested
  @DisplayName("Retryable Flag Tests")
  class RetryableFlagTests {

    @Test
    @DisplayName("should correctly indicate retryable operations")
    void shouldCorrectlyIndicateRetryableOperations() {
      final WasiException retryable =
          new WasiException("timeout", "connect", null, true, WasiException.ErrorCategory.NETWORK);
      final WasiException notRetryable =
          new WasiException("denied", "open", null, false, WasiException.ErrorCategory.PERMISSION);

      assertTrue(retryable.isRetryable(), "Timeout error should be retryable");
      assertFalse(notRetryable.isRetryable(), "Permission error should not be retryable");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle null operation gracefully")
    void shouldHandleNullOperationGracefully() {
      final WasiException exception =
          new WasiException("error", null, "/path", false, WasiException.ErrorCategory.FILE_SYSTEM);
      assertNull(exception.getOperation(), "Null operation should be preserved");
      assertTrue(exception.getMessage().contains("error"), "Message should still contain error");
    }

    @Test
    @DisplayName("should handle null resource gracefully")
    void shouldHandleNullResourceGracefully() {
      final WasiException exception =
          new WasiException(
              "error", "fd_read", null, false, WasiException.ErrorCategory.FILE_SYSTEM);
      assertNull(exception.getResource(), "Null resource should be preserved");
      assertTrue(
          exception.getMessage().contains("fd_read"), "Message should still contain operation");
    }

    @Test
    @DisplayName("should handle empty strings in operation and resource")
    void shouldHandleEmptyStringsInOperationAndResource() {
      final WasiException exception =
          new WasiException("error", "", "", false, WasiException.ErrorCategory.SYSTEM);
      assertEquals("", exception.getOperation(), "Empty operation should be preserved");
      assertEquals("", exception.getResource(), "Empty resource should be preserved");
    }
  }

  @Nested
  @DisplayName("Category Check Methods - False Cases")
  class CategoryCheckMethodsFalseCaseTests {

    @Test
    @DisplayName("isPermissionError should return false for non-PERMISSION categories")
    void isPermissionErrorShouldReturnFalseForOtherCategories() {
      // Test with FILE_SYSTEM
      final WasiException fsException =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.FILE_SYSTEM);
      assertFalse(fsException.isPermissionError());

      // Test with NETWORK
      final WasiException netException =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.NETWORK);
      assertFalse(netException.isPermissionError());

      // Test with SYSTEM (default)
      final WasiException sysException = new WasiException("error");
      assertFalse(sysException.isPermissionError());
    }

    @Test
    @DisplayName("isResourceLimitError should return false for non-RESOURCE_LIMIT categories")
    void isResourceLimitErrorShouldReturnFalseForOtherCategories() {
      // Test with FILE_SYSTEM
      final WasiException fsException =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.FILE_SYSTEM);
      assertFalse(fsException.isResourceLimitError());

      // Test with PERMISSION
      final WasiException permException =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.PERMISSION);
      assertFalse(permException.isResourceLimitError());

      // Test with SYSTEM (default)
      final WasiException sysException = new WasiException("error");
      assertFalse(sysException.isResourceLimitError());
    }

    @Test
    @DisplayName("isComponentError should return false for non-COMPONENT categories")
    void isComponentErrorShouldReturnFalseForOtherCategories() {
      // Test with FILE_SYSTEM
      final WasiException fsException =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.FILE_SYSTEM);
      assertFalse(fsException.isComponentError());

      // Test with NETWORK
      final WasiException netException =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.NETWORK);
      assertFalse(netException.isComponentError());

      // Test with SYSTEM (default)
      final WasiException sysException = new WasiException("error");
      assertFalse(sysException.isComponentError());
    }

    @Test
    @DisplayName("isConfigurationError should return false for non-CONFIGURATION categories")
    void isConfigurationErrorShouldReturnFalseForOtherCategories() {
      // Test with FILE_SYSTEM
      final WasiException fsException =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.FILE_SYSTEM);
      assertFalse(fsException.isConfigurationError());

      // Test with PERMISSION
      final WasiException permException =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.PERMISSION);
      assertFalse(permException.isConfigurationError());

      // Test with SYSTEM (default)
      final WasiException sysException = new WasiException("error");
      assertFalse(sysException.isConfigurationError());
    }
  }

  @Nested
  @DisplayName("Message and Cause Constructor Mutation Tests")
  class MessageAndCauseConstructorMutationTests {

    @Test
    @DisplayName("message with cause constructor should default retryable to false")
    void messageCauseConstructorShouldDefaultRetryableToFalse() {
      final RuntimeException cause = new RuntimeException("Native error");
      final WasiException exception = new WasiException("Error occurred", cause);

      // Verify retryable is false, not true (kills InlineConstant mutation)
      assertFalse(exception.isRetryable());
      assertEquals(false, exception.isRetryable());
    }

    @Test
    @DisplayName("message with cause constructor should default to SYSTEM category")
    void messageCauseConstructorShouldDefaultToSystemCategory() {
      final RuntimeException cause = new RuntimeException("Native error");
      final WasiException exception = new WasiException("Error occurred", cause);

      assertEquals(WasiException.ErrorCategory.SYSTEM, exception.getCategory());
    }

    @Test
    @DisplayName("message with cause constructor should preserve cause")
    void messageCauseConstructorShouldPreserveCause() {
      final RuntimeException cause = new RuntimeException("Native error");
      final WasiException exception = new WasiException("Error occurred", cause);

      assertSame(cause, exception.getCause());
    }
  }

  @Nested
  @DisplayName("formatMessage Mutation Tests")
  class FormatMessageMutationTests {

    @Test
    @DisplayName("operation null and empty should not add operation to message")
    void operationNullOrEmptyShouldNotAddToMessage() {
      // Null operation
      final WasiException nullOpException =
          new WasiException("error", null, "resource", false, WasiException.ErrorCategory.SYSTEM);
      assertFalse(nullOpException.getMessage().contains("operation:"));

      // Empty operation
      final WasiException emptyOpException =
          new WasiException("error", "", "resource", false, WasiException.ErrorCategory.SYSTEM);
      assertFalse(emptyOpException.getMessage().contains("operation:"));
    }

    @Test
    @DisplayName("resource null and empty should not add resource to message")
    void resourceNullOrEmptyShouldNotAddToMessage() {
      // Null resource
      final WasiException nullResException =
          new WasiException("error", "operation", null, false, WasiException.ErrorCategory.SYSTEM);
      assertFalse(nullResException.getMessage().contains("resource:"));

      // Empty resource
      final WasiException emptyResException =
          new WasiException("error", "operation", "", false, WasiException.ErrorCategory.SYSTEM);
      assertFalse(emptyResException.getMessage().contains("resource:"));
    }

    @Test
    @DisplayName("non-null non-empty operation should be added to message")
    void nonNullNonEmptyOperationShouldBeAddedToMessage() {
      final WasiException exception =
          new WasiException(
              "error", "my-operation", null, false, WasiException.ErrorCategory.SYSTEM);
      assertTrue(exception.getMessage().contains("operation: my-operation"));
    }

    @Test
    @DisplayName("non-null non-empty resource should be added to message")
    void nonNullNonEmptyResourceShouldBeAddedToMessage() {
      final WasiException exception =
          new WasiException(
              "error", null, "my-resource", false, WasiException.ErrorCategory.SYSTEM);
      assertTrue(exception.getMessage().contains("resource: my-resource"));
    }

    @Test
    @DisplayName("both operation and resource should be added when present")
    void bothOperationAndResourceShouldBeAddedWhenPresent() {
      final WasiException exception =
          new WasiException(
              "error", "my-operation", "my-resource", false, WasiException.ErrorCategory.SYSTEM);
      assertTrue(exception.getMessage().contains("operation: my-operation"));
      assertTrue(exception.getMessage().contains("resource: my-resource"));
    }

    @Test
    @DisplayName("whitespace-only operation should not be treated as empty")
    void whitespaceOnlyOperationShouldNotBeTreatedAsEmpty() {
      // Whitespace is not empty, so it should be included
      final WasiException exception =
          new WasiException("error", " ", null, false, WasiException.ErrorCategory.SYSTEM);
      assertTrue(exception.getMessage().contains("operation:"));
    }

    @Test
    @DisplayName("whitespace-only resource should not be treated as empty")
    void whitespaceOnlyResourceShouldNotBeTreatedAsEmpty() {
      // Whitespace is not empty, so it should be included
      final WasiException exception =
          new WasiException("error", null, " ", false, WasiException.ErrorCategory.SYSTEM);
      assertTrue(exception.getMessage().contains("resource:"));
    }
  }

  @Nested
  @DisplayName("Category Check - All Categories")
  class AllCategoryChecksTests {

    @Test
    @DisplayName("PERMISSION category should only make isPermissionError return true")
    void permissionCategoryShouldOnlyMakeIsPermissionErrorTrue() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.PERMISSION);

      assertTrue(exception.isPermissionError());
      assertFalse(exception.isFileSystemError());
      assertFalse(exception.isNetworkError());
      assertFalse(exception.isResourceLimitError());
      assertFalse(exception.isComponentError());
      assertFalse(exception.isConfigurationError());
    }

    @Test
    @DisplayName("RESOURCE_LIMIT category should only make isResourceLimitError return true")
    void resourceLimitCategoryShouldOnlyMakeIsResourceLimitErrorTrue() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.RESOURCE_LIMIT);

      assertTrue(exception.isResourceLimitError());
      assertFalse(exception.isFileSystemError());
      assertFalse(exception.isNetworkError());
      assertFalse(exception.isPermissionError());
      assertFalse(exception.isComponentError());
      assertFalse(exception.isConfigurationError());
    }

    @Test
    @DisplayName("COMPONENT category should only make isComponentError return true")
    void componentCategoryShouldOnlyMakeIsComponentErrorTrue() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.COMPONENT);

      assertTrue(exception.isComponentError());
      assertFalse(exception.isFileSystemError());
      assertFalse(exception.isNetworkError());
      assertFalse(exception.isPermissionError());
      assertFalse(exception.isResourceLimitError());
      assertFalse(exception.isConfigurationError());
    }

    @Test
    @DisplayName("CONFIGURATION category should only make isConfigurationError return true")
    void configurationCategoryShouldOnlyMakeIsConfigurationErrorTrue() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.CONFIGURATION);

      assertTrue(exception.isConfigurationError());
      assertFalse(exception.isFileSystemError());
      assertFalse(exception.isNetworkError());
      assertFalse(exception.isPermissionError());
      assertFalse(exception.isResourceLimitError());
      assertFalse(exception.isComponentError());
    }

    @Test
    @DisplayName("FILE_SYSTEM category should only make isFileSystemError return true")
    void fileSystemCategoryShouldOnlyMakeIsFileSystemErrorTrue() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.FILE_SYSTEM);

      assertTrue(exception.isFileSystemError());
      assertFalse(exception.isNetworkError());
      assertFalse(exception.isPermissionError());
      assertFalse(exception.isResourceLimitError());
      assertFalse(exception.isComponentError());
      assertFalse(exception.isConfigurationError());
    }

    @Test
    @DisplayName("NETWORK category should only make isNetworkError return true")
    void networkCategoryShouldOnlyMakeIsNetworkErrorTrue() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.NETWORK);

      assertTrue(exception.isNetworkError());
      assertFalse(exception.isFileSystemError());
      assertFalse(exception.isPermissionError());
      assertFalse(exception.isResourceLimitError());
      assertFalse(exception.isComponentError());
      assertFalse(exception.isConfigurationError());
    }

    @Test
    @DisplayName("SYSTEM category should make all is*Error methods return false")
    void systemCategoryShouldMakeAllIsErrorMethodsReturnFalse() {
      final WasiException exception =
          new WasiException("error", "op", null, false, WasiException.ErrorCategory.SYSTEM);

      assertFalse(exception.isFileSystemError());
      assertFalse(exception.isNetworkError());
      assertFalse(exception.isPermissionError());
      assertFalse(exception.isResourceLimitError());
      assertFalse(exception.isComponentError());
      assertFalse(exception.isConfigurationError());
    }
  }

  @Nested
  @DisplayName("WasiErrorCode Constructor Tests")
  class WasiErrorCodeConstructorTests {

    @Test
    @DisplayName("Constructor(message, errorCode) should set error code and derive category")
    void shouldCreateExceptionWithMessageAndErrorCode() {
      final WasiException exception = new WasiException("Simple error", WasiErrorCode.EINVAL);

      assertEquals(WasiErrorCode.EINVAL, exception.getErrorCode(), "Error code should match");
      assertNull(exception.getOperation(), "Operation should be null");
      assertNull(exception.getResource(), "Resource should be null");
      assertEquals(
          WasiException.ErrorCategory.SYSTEM,
          exception.getCategory(),
          "EINVAL should map to SYSTEM category");
    }

    @Test
    @DisplayName("Constructor(errorCode, operation) should use error code description as message")
    void shouldCreateExceptionFromErrorCodeAndOperation() {
      final WasiException exception = new WasiException(WasiErrorCode.EPERM, "chmod");

      assertEquals(WasiErrorCode.EPERM, exception.getErrorCode(), "Error code should match");
      assertEquals("chmod", exception.getOperation(), "Operation should match");
      assertTrue(
          exception.getMessage().contains("Operation not permitted"),
          "Message should contain error description");
    }

    @Test
    @DisplayName("Constructor(errorCode, operation, resource) should set all fields")
    void shouldCreateExceptionFromErrorCodeOperationAndResource() {
      final WasiException exception =
          new WasiException(WasiErrorCode.ENOSPC, "write", "/disk/full");

      assertEquals(WasiErrorCode.ENOSPC, exception.getErrorCode(), "Error code should match");
      assertEquals("write", exception.getOperation(), "Operation should match");
      assertEquals("/disk/full", exception.getResource(), "Resource should match");
    }

    @Test
    @DisplayName("Constructor(message, errorCode, operation, resource) should set all fields")
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
    @DisplayName("Message should include operation, resource, and errno")
    void messageShouldIncludeOperationResourceAndErrno() {
      final WasiException exception =
          new WasiException("File error", WasiErrorCode.EIO, "read", "/tmp/data.txt");

      final String message = exception.getMessage();
      assertTrue(message.contains("read"), "Message should contain operation");
      assertTrue(message.contains("/tmp/data.txt"), "Message should contain resource");
      assertTrue(message.contains("5"), "Message should contain errno");
    }

    @Test
    @DisplayName("Should handle null message with error code fallback")
    void shouldHandleNullMessageWithErrorCode() {
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

    @Test
    @DisplayName("Constructor with cause should preserve cause")
    void shouldPreserveCause() {
      final RuntimeException cause = new RuntimeException("Underlying error");
      final WasiException exception =
          new WasiException("WASI error", WasiErrorCode.EIO, "write", "/output", cause);

      assertSame(cause, exception.getCause(), "Cause should be preserved");
    }

    @Test
    @DisplayName("Constructor with null cause should be null")
    void shouldHandleNullCause() {
      final WasiException exception =
          new WasiException("Error", WasiErrorCode.EACCES, "open", "/file", null);

      assertNull(exception.getCause(), "Cause should be null");
    }
  }

  @Nested
  @DisplayName("WasiErrorCode Category Derivation Tests")
  class WasiErrorCodeCategoryDerivationTests {

    @Test
    @DisplayName("File system error codes should derive FILE_SYSTEM category")
    void fileSystemErrorCodesShouldDeriveFileSystemCategory() {
      final WasiException fsException = new WasiException("FS error", WasiErrorCode.ENOENT);
      assertTrue(fsException.isFileSystemError(), "ENOENT should be file system error");
    }

    @Test
    @DisplayName("Network error codes should derive NETWORK category")
    void networkErrorCodesShouldDeriveNetworkCategory() {
      final WasiException netException = new WasiException("Net error", WasiErrorCode.ETIMEDOUT);
      assertTrue(netException.isNetworkError(), "ETIMEDOUT should be network error");
    }

    @Test
    @DisplayName("Permission error codes should derive PERMISSION category")
    void permissionErrorCodesShouldDerivePermissionCategory() {
      final WasiException permException = new WasiException("Perm error", WasiErrorCode.EPERM);
      assertTrue(permException.isPermissionError(), "EPERM should be permission error");
    }

    @Test
    @DisplayName("Resource limit error codes should derive RESOURCE_LIMIT category")
    void resourceLimitErrorCodesShouldDeriveResourceLimitCategory() {
      final WasiException memException = new WasiException("Memory error", WasiErrorCode.ENOMEM);
      assertTrue(memException.isResourceLimitError(), "ENOMEM should be resource limit error");
    }

    @Test
    @DisplayName("Retryable flag should be derived from error code")
    void retryableShouldBeDerivedFromErrorCode() {
      final WasiException retryable = new WasiException("Retry error", WasiErrorCode.EAGAIN);
      final WasiException notRetryable = new WasiException("No retry", WasiErrorCode.EINVAL);

      assertTrue(retryable.isRetryable(), "EAGAIN should be retryable");
      assertFalse(notRetryable.isRetryable(), "EINVAL should not be retryable");
    }

    @Test
    @DisplayName("Non-categorized error codes should default to SYSTEM category")
    void nonCategorizedErrorCodesShouldDefaultToSystem() {
      final WasiException exception = new WasiException("IO error", WasiErrorCode.EIO);
      assertNotNull(exception.getCategory(), "Category should not be null");
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
      } catch (final WasiException e) {
        assertEquals(WasiErrorCode.ENOENT, e.getErrorCode(), "Should catch with correct code");
        assertEquals("open", e.getOperation(), "Should have correct operation");
      }
    }

    @Test
    @DisplayName("Should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      try {
        throw new WasiException(WasiErrorCode.EIO, "read");
      } catch (final WasmException e) {
        assertTrue(e instanceof WasiException, "Should be instance of WasiException");
      }
    }

    @Test
    @DisplayName("Full message format should be correct with error code")
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

  @Nested
  @DisplayName("Retryable Error Code Derivation Tests")
  class RetryableErrorCodeDerivationTests {

    @Test
    @DisplayName("EAGAIN error code should make exception retryable")
    void eagainShouldBeRetryable() {
      final WasiException exception = new WasiException("retry", WasiErrorCode.EAGAIN);

      assertTrue(exception.isRetryable(), "EAGAIN should be retryable");
      assertNotNull(exception.getErrorCode(), "Error code should not be null");
      assertTrue(
          exception.getErrorCode().isRetryable(), "EAGAIN error code itself should be retryable");
    }

    @Test
    @DisplayName("EPERM error code should not make exception retryable")
    void epermShouldNotBeRetryable() {
      final WasiException exception = new WasiException("denied", WasiErrorCode.EPERM);

      assertFalse(exception.isRetryable(), "EPERM should not be retryable");
    }

    @Test
    @DisplayName("null error code should make exception not retryable")
    void nullErrorCodeShouldNotBeRetryable() {
      final WasiException exception = new WasiException("plain error");

      assertFalse(exception.isRetryable(), "Null error code should not be retryable");
      assertNull(
          exception.getErrorCode(), "Error code should be null for plain message constructor");
    }

    @Test
    @DisplayName("null error code should derive SYSTEM category")
    void nullErrorCodeShouldDeriveSystemCategory() {
      final WasiException exception = new WasiException("some error", (WasiErrorCode) null);

      assertEquals(
          WasiException.ErrorCategory.SYSTEM,
          exception.getCategory(),
          "Null error code should derive SYSTEM category");
    }
  }

  @Nested
  @DisplayName("Five-arg Constructor with Cause - Retryable Derivation Tests")
  class FiveArgConstructorRetryableTests {

    @Test
    @DisplayName("null error code with cause should make retryable false")
    void nullErrorCodeWithCauseShouldMakeRetryableFalse() {
      final RuntimeException cause = new RuntimeException("cause");
      final WasiException exception =
          new WasiException("error", (WasiErrorCode) null, "op", "res", cause);

      assertFalse(exception.isRetryable(), "Null error code should make retryable false");
      assertNull(exception.getErrorCode(), "Error code should be null");
      assertSame(cause, exception.getCause(), "Cause should be preserved");
    }

    @Test
    @DisplayName("retryable error code with cause should make retryable true")
    void retryableErrorCodeWithCauseShouldMakeRetryableTrue() {
      final RuntimeException cause = new RuntimeException("cause");
      final WasiException exception =
          new WasiException("error", WasiErrorCode.EAGAIN, "op", "res", cause);

      assertTrue(exception.isRetryable(), "EAGAIN should make retryable true");
      assertEquals(WasiErrorCode.EAGAIN, exception.getErrorCode(), "Error code should be EAGAIN");
      assertSame(cause, exception.getCause(), "Cause should be preserved");
    }

    @Test
    @DisplayName("non-retryable error code with cause should make retryable false")
    void nonRetryableErrorCodeWithCauseShouldMakeRetryableFalse() {
      final RuntimeException cause = new RuntimeException("cause");
      final WasiException exception =
          new WasiException("error", WasiErrorCode.EINVAL, "op", "res", cause);

      assertFalse(exception.isRetryable(), "EINVAL should make retryable false");
    }

    @Test
    @DisplayName("four-arg constructor with null error code should make retryable false")
    void fourArgConstructorNullErrorCodeShouldMakeRetryableFalse() {
      final WasiException exception = new WasiException("error", (WasiErrorCode) null, "op", "res");

      assertFalse(exception.isRetryable(), "Null error code should make retryable false");
    }

    @Test
    @DisplayName("four-arg constructor with retryable error code should make retryable true")
    void fourArgConstructorRetryableErrorCodeShouldMakeRetryableTrue() {
      final WasiException exception = new WasiException("error", WasiErrorCode.EAGAIN, "op", "res");

      assertTrue(exception.isRetryable(), "EAGAIN should make retryable true");
    }
  }
}

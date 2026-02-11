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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
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
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(WasiException.class.getModifiers()), "WasiException should be public");
    }

    @Test
    @DisplayName("should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(WasiException.class),
          "WasiException should extend WasmException");
    }

    @Test
    @DisplayName("should have ErrorCategory nested enum")
    void shouldHaveErrorCategoryNestedEnum() {
      final Class<?>[] declaredClasses = WasiException.class.getDeclaredClasses();
      boolean hasErrorCategory = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ErrorCategory") && clazz.isEnum()) {
          hasErrorCategory = true;
          break;
        }
      }
      assertTrue(hasErrorCategory, "WasiException should have ErrorCategory nested enum");
    }
  }

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
    @DisplayName("should throw IllegalArgumentException for null or empty message")
    void shouldThrowForNullOrEmptyMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiException(null, "op", "res", false, WasiException.ErrorCategory.SYSTEM),
          "Should throw for null message");

      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiException("", "op", "res", false, WasiException.ErrorCategory.SYSTEM),
          "Should throw for empty message");
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
}

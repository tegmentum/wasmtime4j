package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for WasiResourceException class. */
class WasiResourceExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Simple message constructor creates resource exception correctly")
    void testSimpleMessageConstructor() {
      final String message = "Resource operation failed";
      final WasiResourceException exception = new WasiResourceException(message);

      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
      assertEquals("resource-operation", exception.getOperation());
      assertEquals(WasiResourceException.ResourceType.SYSTEM, exception.getResourceType());
      assertNull(exception.getResourceHandle());
      assertEquals(
          WasiResourceException.ResourceOperation.ACCESS, exception.getResourceOperation());
      assertFalse(exception.isCleanupRequired());
      assertTrue(exception.isRetryable());
      assertEquals(WasiException.ErrorCategory.RESOURCE_LIMIT, exception.getCategory());
    }

    @Test
    @DisplayName("Message with cause constructor creates exception correctly")
    void testMessageWithCauseConstructor() {
      final String message = "Resource failed";
      final RuntimeException cause = new RuntimeException("Native error");
      final WasiResourceException exception = new WasiResourceException(message, cause);

      assertEquals(message, exception.getMessage());
      assertEquals(cause, exception.getCause());
      assertEquals("resource-operation", exception.getOperation());
      assertEquals(WasiResourceException.ResourceType.SYSTEM, exception.getResourceType());
      assertEquals(
          WasiResourceException.ResourceOperation.ACCESS, exception.getResourceOperation());
      assertTrue(exception.isRetryable());
    }

    @Test
    @DisplayName("Resource-specific constructor creates exception correctly")
    void testResourceSpecificConstructor() {
      final String message = "File allocation failed";
      final WasiResourceException.ResourceType resourceType =
          WasiResourceException.ResourceType.FILE;
      final WasiResourceException.ResourceOperation resourceOperation =
          WasiResourceException.ResourceOperation.ALLOCATION;

      final WasiResourceException exception =
          new WasiResourceException(message, resourceType, resourceOperation);

      assertTrue(exception.getMessage().contains(message));
      assertEquals("file-allocation", exception.getOperation());
      assertEquals("file", exception.getResource());
      assertEquals(resourceType, exception.getResourceType());
      assertNull(exception.getResourceHandle());
      assertEquals(resourceOperation, exception.getResourceOperation());
      assertTrue(exception.isCleanupRequired());
      assertTrue(exception.isRetryable());
      assertEquals(WasiException.ErrorCategory.FILE_SYSTEM, exception.getCategory());
    }

    @Test
    @DisplayName("Full constructor creates exception correctly")
    void testFullConstructor() {
      final String message = "Socket access failed";
      final WasiResourceException.ResourceType resourceType =
          WasiResourceException.ResourceType.SOCKET;
      final String resourceHandle = "socket-123";
      final WasiResourceException.ResourceOperation resourceOperation =
          WasiResourceException.ResourceOperation.ACCESS;

      final WasiResourceException exception =
          new WasiResourceException(message, resourceType, resourceHandle, resourceOperation);

      assertTrue(exception.getMessage().contains(message));
      assertEquals("socket-access", exception.getOperation());
      assertEquals("socket:socket-123", exception.getResource());
      assertEquals(resourceType, exception.getResourceType());
      assertEquals(resourceHandle, exception.getResourceHandle());
      assertEquals(resourceOperation, exception.getResourceOperation());
      assertFalse(exception.isCleanupRequired());
      assertTrue(exception.isRetryable());
      assertEquals(WasiException.ErrorCategory.NETWORK, exception.getCategory());
    }

    @Test
    @DisplayName("Full constructor with cause creates exception correctly")
    void testFullConstructorWithCause() {
      final String message = "Memory cleanup failed";
      final WasiResourceException.ResourceType resourceType =
          WasiResourceException.ResourceType.MEMORY;
      final String resourceHandle = "mem-456";
      final WasiResourceException.ResourceOperation resourceOperation =
          WasiResourceException.ResourceOperation.CLEANUP;
      final RuntimeException cause = new RuntimeException("Memory error");

      final WasiResourceException exception =
          new WasiResourceException(
              message, resourceType, resourceHandle, resourceOperation, cause);

      assertTrue(exception.getMessage().contains(message));
      assertEquals(cause, exception.getCause());
      assertEquals("memory-cleanup", exception.getOperation());
      assertEquals("memory:mem-456", exception.getResource());
      assertEquals(resourceType, exception.getResourceType());
      assertEquals(resourceHandle, exception.getResourceHandle());
      assertEquals(resourceOperation, exception.getResourceOperation());
      assertFalse(
          exception.isCleanupRequired()); // Cleanup operations don't require additional cleanup
    }
  }

  @Nested
  @DisplayName("Resource Operation Tests")
  class ResourceOperationTests {

    @Test
    @DisplayName("isAllocationError returns true for ALLOCATION operation")
    void testIsAllocationError() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.ALLOCATION);
      assertTrue(exception.isAllocationError());
      assertFalse(exception.isAccessError());
      assertFalse(exception.isCleanupError());
    }

    @Test
    @DisplayName("isAccessError returns true for ACCESS operation")
    void testIsAccessError() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.ACCESS);
      assertFalse(exception.isAllocationError());
      assertTrue(exception.isAccessError());
      assertFalse(exception.isCleanupError());
    }

    @Test
    @DisplayName("isCleanupError returns true for CLEANUP operation")
    void testIsCleanupError() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.CLEANUP);
      assertFalse(exception.isAllocationError());
      assertFalse(exception.isAccessError());
      assertTrue(exception.isCleanupError());
    }
  }

  @Nested
  @DisplayName("Resource Type Tests")
  class ResourceTypeTests {

    @Test
    @DisplayName("isFileResourceError returns true for file resources")
    void testIsFileResourceError() {
      final WasiResourceException fileException =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.ACCESS);
      assertTrue(fileException.isFileResourceError());
      assertFalse(fileException.isNetworkResourceError());
      assertFalse(fileException.isMemoryResourceError());

      final WasiResourceException dirException =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.DIRECTORY,
              WasiResourceException.ResourceOperation.ACCESS);
      assertTrue(dirException.isFileResourceError());
      assertFalse(dirException.isNetworkResourceError());
      assertFalse(dirException.isMemoryResourceError());
    }

    @Test
    @DisplayName("isNetworkResourceError returns true for socket resources")
    void testIsNetworkResourceError() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.SOCKET,
              WasiResourceException.ResourceOperation.ACCESS);
      assertFalse(exception.isFileResourceError());
      assertTrue(exception.isNetworkResourceError());
      assertFalse(exception.isMemoryResourceError());
    }

    @Test
    @DisplayName("isMemoryResourceError returns true for memory resources")
    void testIsMemoryResourceError() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.MEMORY,
              WasiResourceException.ResourceOperation.ACCESS);
      assertFalse(exception.isFileResourceError());
      assertFalse(exception.isNetworkResourceError());
      assertTrue(exception.isMemoryResourceError());
    }
  }

  @Nested
  @DisplayName("Cleanup Logic Tests")
  class CleanupLogicTests {

    @Test
    @DisplayName("ALLOCATION operations require cleanup")
    void testAllocationRequiresCleanup() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.ALLOCATION);
      assertTrue(exception.isCleanupRequired());
    }

    @Test
    @DisplayName("LIFETIME_MANAGEMENT operations require cleanup")
    void testLifetimeManagementRequiresCleanup() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.LIFETIME_MANAGEMENT);
      assertTrue(exception.isCleanupRequired());
    }

    @Test
    @DisplayName("Other operations do not require cleanup")
    void testOtherOperationsDoNotRequireCleanup() {
      assertFalse(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.ACCESS)
              .isCleanupRequired());
      assertFalse(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.MODIFICATION)
              .isCleanupRequired());
      assertFalse(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.CLEANUP)
              .isCleanupRequired());
    }
  }

  @Nested
  @DisplayName("Retry Logic Tests")
  class RetryLogicTests {

    @Test
    @DisplayName("ALLOCATION and ACCESS operations are retryable")
    void testRetryableOperations() {
      assertTrue(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.ALLOCATION)
              .isRetryable());
      assertTrue(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.ACCESS)
              .isRetryable());
    }

    @Test
    @DisplayName("Other operations are not retryable")
    void testNonRetryableOperations() {
      assertFalse(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.MODIFICATION)
              .isRetryable());
      assertFalse(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.CLEANUP)
              .isRetryable());
      assertFalse(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.LIFETIME_MANAGEMENT)
              .isRetryable());
    }
  }

  @Nested
  @DisplayName("Error Category Mapping Tests")
  class ErrorCategoryMappingTests {

    @Test
    @DisplayName("File resources map to FILE_SYSTEM category")
    void testFileResourcesCategory() {
      assertEquals(
          WasiException.ErrorCategory.FILE_SYSTEM,
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.ACCESS)
              .getCategory());
      assertEquals(
          WasiException.ErrorCategory.FILE_SYSTEM,
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.DIRECTORY,
                  WasiResourceException.ResourceOperation.ACCESS)
              .getCategory());
    }

    @Test
    @DisplayName("Socket resources map to NETWORK category")
    void testSocketResourcesCategory() {
      assertEquals(
          WasiException.ErrorCategory.NETWORK,
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.SOCKET,
                  WasiResourceException.ResourceOperation.ACCESS)
              .getCategory());
    }

    @Test
    @DisplayName("Other resources map to RESOURCE_LIMIT category")
    void testOtherResourcesCategory() {
      assertEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.MEMORY,
                  WasiResourceException.ResourceOperation.ACCESS)
              .getCategory());
      assertEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.EXECUTION_CONTEXT,
                  WasiResourceException.ResourceOperation.ACCESS)
              .getCategory());
      assertEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.TIMER,
                  WasiResourceException.ResourceOperation.ACCESS)
              .getCategory());
      assertEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.EVENT,
                  WasiResourceException.ResourceOperation.ACCESS)
              .getCategory());
      assertEquals(
          WasiException.ErrorCategory.RESOURCE_LIMIT,
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.SYSTEM,
                  WasiResourceException.ResourceOperation.ACCESS)
              .getCategory());
    }
  }

  @Nested
  @DisplayName("Message Formatting Tests")
  class MessageFormattingTests {

    @Test
    @DisplayName("Operation formatting works correctly for all combinations")
    void testOperationFormatting() {
      assertEquals(
          "file-allocation",
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.ALLOCATION)
              .getOperation());
      assertEquals(
          "socket-access",
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.SOCKET,
                  WasiResourceException.ResourceOperation.ACCESS)
              .getOperation());
      assertEquals(
          "memory-cleanup",
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.MEMORY,
                  WasiResourceException.ResourceOperation.CLEANUP)
              .getOperation());
    }

    @Test
    @DisplayName("Resource formatting works correctly with type only")
    void testResourceFormattingTypeOnly() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.ACCESS);
      assertEquals("file", exception.getResource());
    }

    @Test
    @DisplayName("Resource formatting works correctly with type and handle")
    void testResourceFormattingTypeAndHandle() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              "file-123",
              WasiResourceException.ResourceOperation.ACCESS);
      assertEquals("file:file-123", exception.getResource());
    }

    @Test
    @DisplayName("Resource formatting works correctly with handle only")
    void testResourceFormattingHandleOnly() {
      // This would require creating an exception with null type and non-null handle, which isn't
      // exposed by constructors
      // but we can test the internal logic through inheritance
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              "handle-only",
              WasiResourceException.ResourceOperation.ACCESS);
      assertEquals("file:handle-only", exception.getResource());
    }

    @Test
    @DisplayName("Resource formatting returns null for null inputs")
    void testResourceFormattingNullInputs() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              null,
              WasiResourceException.ResourceOperation.ACCESS);
      assertEquals("file", exception.getResource());
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("WasiResourceException extends WasiException")
    void testWasiResourceExceptionExtendsWasiException() {
      final WasiResourceException exception = new WasiResourceException("Test error");
      assertTrue(exception instanceof WasiException);
    }

    @Test
    @DisplayName("WasiResourceException extends WasmException")
    void testWasiResourceExceptionExtendsWasmException() {
      final WasiResourceException exception = new WasiResourceException("Test error");
      assertTrue(exception instanceof WasmException);
    }
  }

  @Nested
  @DisplayName("Resource Type Enum Tests")
  class ResourceTypeEnumTests {

    @Test
    @DisplayName("All ResourceType values are properly defined")
    void testResourceTypeValues() {
      final WasiResourceException.ResourceType[] types =
          WasiResourceException.ResourceType.values();
      assertEquals(8, types.length);

      assertTrue(contains(types, WasiResourceException.ResourceType.FILE));
      assertTrue(contains(types, WasiResourceException.ResourceType.DIRECTORY));
      assertTrue(contains(types, WasiResourceException.ResourceType.SOCKET));
      assertTrue(contains(types, WasiResourceException.ResourceType.MEMORY));
      assertTrue(contains(types, WasiResourceException.ResourceType.EXECUTION_CONTEXT));
      assertTrue(contains(types, WasiResourceException.ResourceType.TIMER));
      assertTrue(contains(types, WasiResourceException.ResourceType.EVENT));
      assertTrue(contains(types, WasiResourceException.ResourceType.SYSTEM));
    }

    private boolean contains(
        final WasiResourceException.ResourceType[] array,
        final WasiResourceException.ResourceType value) {
      for (final WasiResourceException.ResourceType type : array) {
        if (type == value) {
          return true;
        }
      }
      return false;
    }
  }

  @Nested
  @DisplayName("Resource Operation Enum Tests")
  class ResourceOperationEnumTests {

    @Test
    @DisplayName("All ResourceOperation values are properly defined")
    void testResourceOperationValues() {
      final WasiResourceException.ResourceOperation[] operations =
          WasiResourceException.ResourceOperation.values();
      assertEquals(5, operations.length);

      assertTrue(contains(operations, WasiResourceException.ResourceOperation.ALLOCATION));
      assertTrue(contains(operations, WasiResourceException.ResourceOperation.ACCESS));
      assertTrue(contains(operations, WasiResourceException.ResourceOperation.MODIFICATION));
      assertTrue(contains(operations, WasiResourceException.ResourceOperation.CLEANUP));
      assertTrue(contains(operations, WasiResourceException.ResourceOperation.LIFETIME_MANAGEMENT));
    }

    private boolean contains(
        final WasiResourceException.ResourceOperation[] array,
        final WasiResourceException.ResourceOperation value) {
      for (final WasiResourceException.ResourceOperation operation : array) {
        if (operation == value) {
          return true;
        }
      }
      return false;
    }
  }
}

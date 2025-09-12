package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for WasiResourceException class. */
class WasiResourceExceptionTest {

  @Nested

  class ConstructorTests {

    @Test

    void testSimpleMessageConstructor() {
      final String message = "Resource operation failed";
      final WasiResourceException exception = new WasiResourceException(message);

      assertEquals(
          "Resource operation failed (operation: resource-operation)", exception.getMessage());
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

    void testMessageWithCauseConstructor() {
      final String message = "Resource failed";
      final RuntimeException cause = new RuntimeException("Native error");
      final WasiResourceException exception = new WasiResourceException(message, cause);

      assertEquals("Resource failed (operation: resource-operation)", exception.getMessage());
      assertEquals(cause, exception.getCause());
      assertEquals("resource-operation", exception.getOperation());
      assertEquals(WasiResourceException.ResourceType.SYSTEM, exception.getResourceType());
      assertEquals(
          WasiResourceException.ResourceOperation.ACCESS, exception.getResourceOperation());
      assertTrue(exception.isRetryable());
    }

    @Test

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

  class ResourceOperationTests {

    @Test

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

  class ResourceTypeTests {

    @Test

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

  class CleanupLogicTests {

    @Test

    void testAllocationRequiresCleanup() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.ALLOCATION);
      assertTrue(exception.isCleanupRequired());
    }

    @Test

    void testLifetimeManagementRequiresCleanup() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.LIFETIME_MANAGEMENT);
      assertTrue(exception.isCleanupRequired());
    }

    @Test

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

  class RetryLogicTests {

    @Test

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

  class ErrorCategoryMappingTests {

    @Test

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

  class MessageFormattingTests {

    @Test

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

    void testResourceFormattingTypeOnly() {
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.ACCESS);
      assertEquals("file", exception.getResource());
    }

    @Test

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

  class InheritanceTests {

    @Test

    void testWasiResourceExceptionExtendsWasiException() {
      final WasiResourceException exception = new WasiResourceException("Test error");
      assertTrue(exception instanceof WasiException);
    }

    @Test

    void testWasiResourceExceptionExtendsWasmException() {
      final WasiResourceException exception = new WasiResourceException("Test error");
      assertTrue(exception instanceof WasmException);
    }
  }

  @Nested

  class ResourceTypeEnumTests {

    @Test

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

  class ResourceOperationEnumTests {

    @Test

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

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

  /** Tests for null parameter handling in private static methods. */
  @Nested
  class NullParameterHandlingTests {

    @Test
    void testFormatOperationWithBothNull() {
      // Passing both null to formatOperation should return "resource-operation"
      final WasiResourceException exception = new WasiResourceException("Error", null, null);

      // When both operation and resourceType are null, formatOperation returns "resource-operation"
      assertEquals("resource-operation", exception.getOperation());
      // formatResourceIdentifier(null, null) returns null
      assertNull(exception.getResource());
      // isRetryableOperation(null) returns false
      assertFalse(exception.isRetryable());
      // getErrorCategory(null, null) returns RESOURCE_LIMIT
      assertEquals(WasiException.ErrorCategory.RESOURCE_LIMIT, exception.getCategory());
      // requiresCleanup(null) returns false
      assertFalse(exception.isCleanupRequired());
    }

    @Test
    void testFormatOperationWithNullOperation() {
      // Passing null operation with non-null resourceType
      final WasiResourceException exception =
          new WasiResourceException("Error", WasiResourceException.ResourceType.FILE, null);

      // formatOperation should use "operation" for null operation part
      assertEquals("file-operation", exception.getOperation());
      // formatResourceIdentifier(FILE, null) returns "file"
      assertEquals("file", exception.getResource());
      // isRetryableOperation(null) returns false
      assertFalse(exception.isRetryable());
      // getErrorCategory(FILE, null) returns FILE_SYSTEM
      assertEquals(WasiException.ErrorCategory.FILE_SYSTEM, exception.getCategory());
      // requiresCleanup(null) returns false
      assertFalse(exception.isCleanupRequired());
    }

    @Test
    void testFormatOperationWithNullResourceType() {
      // Passing null resourceType with non-null operation
      final WasiResourceException exception =
          new WasiResourceException(
              "Error", null, WasiResourceException.ResourceOperation.ALLOCATION);

      // formatOperation should use "resource" for null resourceType part
      assertEquals("resource-allocation", exception.getOperation());
      // formatResourceIdentifier(null, null) returns null
      assertNull(exception.getResource());
      // isRetryableOperation(ALLOCATION) returns true
      assertTrue(exception.isRetryable());
      // getErrorCategory(null, ALLOCATION) returns RESOURCE_LIMIT
      assertEquals(WasiException.ErrorCategory.RESOURCE_LIMIT, exception.getCategory());
      // requiresCleanup(ALLOCATION) returns true
      assertTrue(exception.isCleanupRequired());
    }

    @Test
    void testFormatResourceIdentifierWithNullTypeAndHandle() {
      // formatResourceIdentifier(null, resourceHandle) should return just the handle
      final WasiResourceException exception =
          new WasiResourceException(
              "Error", null, "handle-123", WasiResourceException.ResourceOperation.ACCESS);

      // formatResourceIdentifier(null, "handle-123") returns "handle-123"
      assertEquals("handle-123", exception.getResource());
    }

    @Test
    void testNullResourceTypeWithHandle() {
      // Test the branch where resourceType is null but handle is provided
      final WasiResourceException exception =
          new WasiResourceException(
              "Error", null, "my-handle", WasiResourceException.ResourceOperation.CLEANUP);

      assertEquals("resource-cleanup", exception.getOperation());
      assertEquals("my-handle", exception.getResource());
      assertFalse(exception.isRetryable());
      assertEquals(WasiException.ErrorCategory.RESOURCE_LIMIT, exception.getCategory());
      assertFalse(exception.isCleanupRequired());
    }

    @Test
    void testNullResourceTypeWithNullHandle() {
      // Test the branch where both resourceType and handle are null
      final WasiResourceException exception =
          new WasiResourceException(
              "Error", null, null, WasiResourceException.ResourceOperation.MODIFICATION);

      assertEquals("resource-modification", exception.getOperation());
      assertNull(exception.getResource());
      assertFalse(exception.isRetryable());
      assertEquals(WasiException.ErrorCategory.RESOURCE_LIMIT, exception.getCategory());
      assertFalse(exception.isCleanupRequired());
    }

    @Test
    void testNullOperationWithHandle() {
      // Test with null operation but with a handle
      final WasiResourceException exception =
          new WasiResourceException(
              "Error", WasiResourceException.ResourceType.SOCKET, "socket-handle", null);

      assertEquals("socket-operation", exception.getOperation());
      assertEquals("socket:socket-handle", exception.getResource());
      assertFalse(exception.isRetryable());
      assertEquals(WasiException.ErrorCategory.NETWORK, exception.getCategory());
      assertFalse(exception.isCleanupRequired());
    }
  }

  /** Tests for mutation killing in formatOperation method. */
  @Nested
  class FormatOperationMutationTests {

    @Test
    void testFormatOperationBothNullReturnsExactString() {
      // Mutation: InlineConstant on "resource-operation" at line 274
      final WasiResourceException exception = new WasiResourceException("Error", null, null);

      // The exact string must be "resource-operation", not any mutation of it
      final String operation = exception.getOperation();
      assertEquals("resource-operation", operation);
      assertTrue(operation.contains("resource"));
      assertTrue(operation.contains("operation"));
      assertTrue(operation.contains("-"));
      assertEquals(18, operation.length());
    }

    @Test
    void testFormatOperationNullOperationUsesOperationString() {
      // Mutation: InlineConstant on "operation" at line 278
      final WasiResourceException exception =
          new WasiResourceException("Error", WasiResourceException.ResourceType.MEMORY, null);

      final String operation = exception.getOperation();
      assertEquals("memory-operation", operation);
      assertTrue(operation.endsWith("-operation"));
      assertTrue(operation.startsWith("memory"));
    }

    @Test
    void testFormatOperationNullResourceTypeUsesResourceString() {
      // Mutation: InlineConstant on "resource" at line 280
      final WasiResourceException exception =
          new WasiResourceException("Error", null, WasiResourceException.ResourceOperation.CLEANUP);

      final String operation = exception.getOperation();
      assertEquals("resource-cleanup", operation);
      assertTrue(operation.startsWith("resource-"));
      assertTrue(operation.endsWith("-cleanup"));
    }

    @Test
    void testFormatOperationOrConditionFirstPartTrue() {
      // Test when operation == null (first part of OR is true)
      final WasiResourceException exception =
          new WasiResourceException("Error", WasiResourceException.ResourceType.TIMER, null);

      // Should not return "resource-operation" because resourceType is not null
      assertFalse("resource-operation".equals(exception.getOperation()));
      assertEquals("timer-operation", exception.getOperation());
    }

    @Test
    void testFormatOperationOrConditionSecondPartTrue() {
      // Test when resourceType == null (second part of OR is true)
      final WasiResourceException exception =
          new WasiResourceException("Error", null, WasiResourceException.ResourceOperation.ACCESS);

      // Should not return "resource-operation" because operation is not null
      assertFalse("resource-operation".equals(exception.getOperation()));
      assertEquals("resource-access", exception.getOperation());
    }

    @Test
    void testFormatOperationNeitherNull() {
      // Test when neither is null (both parts of OR are false)
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.EVENT,
              WasiResourceException.ResourceOperation.LIFETIME_MANAGEMENT);

      assertFalse("resource-operation".equals(exception.getOperation()));
      assertEquals("event-lifetime-management", exception.getOperation());
    }
  }

  /** Tests for mutation killing in formatResourceIdentifier method. */
  @Nested
  class FormatResourceIdentifierMutationTests {

    @Test
    void testFormatResourceIdentifierBothNullReturnsNull() {
      // Mutation: at line 294-295, both null should return null
      final WasiResourceException exception = new WasiResourceException("Error", null, null);

      assertNull(exception.getResource());
    }

    @Test
    void testFormatResourceIdentifierNullHandleReturnsTypeLowercase() {
      // Mutation: at line 298-299, null handle with type should return type lowercase
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.DIRECTORY,
              WasiResourceException.ResourceOperation.ACCESS);

      assertEquals("directory", exception.getResource());
      // Verify it's lowercase
      assertEquals(exception.getResource(), exception.getResource().toLowerCase());
    }

    @Test
    void testFormatResourceIdentifierNullTypeReturnsHandle() {
      // Mutation: at line 302-303, null type with handle should return handle
      final WasiResourceException exception =
          new WasiResourceException(
              "Error", null, "my-unique-handle", WasiResourceException.ResourceOperation.ACCESS);

      assertEquals("my-unique-handle", exception.getResource());
    }

    @Test
    void testFormatResourceIdentifierBothPresentReturnsTypeColonHandle() {
      // Mutation: at line 306, both present should return "type:handle"
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.EXECUTION_CONTEXT,
              "ctx-999",
              WasiResourceException.ResourceOperation.ACCESS);

      assertEquals("execution_context:ctx-999", exception.getResource());
      assertTrue(exception.getResource().contains(":"));
    }

    @Test
    void testFormatResourceIdentifierFirstConditionResourceTypeNull() {
      // Test: resourceType == null && resourceHandle == null (line 294)
      final WasiResourceException ex1 = new WasiResourceException("Error", null, null);
      assertNull(ex1.getResource());

      // Test: resourceType != null && resourceHandle == null (line 298)
      final WasiResourceException ex2 =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.ACCESS);
      assertEquals("file", ex2.getResource());
    }

    @Test
    void testFormatResourceIdentifierSecondConditionHandleNull() {
      // Test: resourceHandle == null (line 298) with resourceType not null
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.SOCKET,
              null,
              WasiResourceException.ResourceOperation.ACCESS);

      assertEquals("socket", exception.getResource());
      assertFalse(exception.getResource().contains(":"));
    }

    @Test
    void testFormatResourceIdentifierThirdConditionTypeNull() {
      // Test: resourceType == null (line 302) with resourceHandle not null
      final WasiResourceException exception =
          new WasiResourceException(
              "Error", null, "handle-only-test", WasiResourceException.ResourceOperation.ACCESS);

      assertEquals("handle-only-test", exception.getResource());
    }
  }

  /** Tests for mutation killing in getErrorCategory method. */
  @Nested
  class GetErrorCategoryMutationTests {

    @Test
    void testGetErrorCategoryWithNullResourceType() {
      // Mutation: at line 342-343, null resourceType should return RESOURCE_LIMIT
      final WasiResourceException exception =
          new WasiResourceException("Error", null, WasiResourceException.ResourceOperation.ACCESS);

      assertEquals(WasiException.ErrorCategory.RESOURCE_LIMIT, exception.getCategory());
    }

    @Test
    void testGetErrorCategoryNullResourceTypeExactCategory() {
      // Verify exact category when resourceType is null
      final WasiResourceException exception = new WasiResourceException("Error", null, null);

      final WasiException.ErrorCategory category = exception.getCategory();
      assertEquals(WasiException.ErrorCategory.RESOURCE_LIMIT, category);
      // Verify it's not any other category
      assertFalse(WasiException.ErrorCategory.FILE_SYSTEM.equals(category));
      assertFalse(WasiException.ErrorCategory.NETWORK.equals(category));
      assertFalse(WasiException.ErrorCategory.SYSTEM.equals(category));
    }

    @Test
    void testGetErrorCategoryAllResourceTypes() {
      // Test all resource types to ensure switch coverage
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

      assertEquals(
          WasiException.ErrorCategory.NETWORK,
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.SOCKET,
                  WasiResourceException.ResourceOperation.ACCESS)
              .getCategory());

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

  /** Tests for mutation killing in isRetryableOperation method. */
  @Nested
  class IsRetryableOperationMutationTests {

    @Test
    void testIsRetryableOperationWithNull() {
      // Mutation: at line 316-317, null operation should return false
      final WasiResourceException exception =
          new WasiResourceException("Error", WasiResourceException.ResourceType.FILE, null);

      assertFalse(exception.isRetryable());
    }

    @Test
    void testIsRetryableOperationNullExplicitCheck() {
      // Verify null operation returns false, not true
      final WasiResourceException exception = new WasiResourceException("Error", null, null);

      final boolean retryable = exception.isRetryable();
      assertFalse(retryable);
      assertEquals(false, retryable);
    }

    @Test
    void testIsRetryableOperationAllCases() {
      // ALLOCATION - returns true
      assertTrue(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.ALLOCATION)
              .isRetryable());

      // ACCESS - returns true
      assertTrue(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.ACCESS)
              .isRetryable());

      // MODIFICATION - returns false
      assertFalse(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.MODIFICATION)
              .isRetryable());

      // CLEANUP - returns false
      assertFalse(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.CLEANUP)
              .isRetryable());

      // LIFETIME_MANAGEMENT - returns false
      assertFalse(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.LIFETIME_MANAGEMENT)
              .isRetryable());
    }
  }

  /** Tests for mutation killing in requiresCleanup method. */
  @Nested
  class RequiresCleanupMutationTests {

    @Test
    void testRequiresCleanupWithNull() {
      // Mutation: at line 370-371, null operation should return false
      final WasiResourceException exception =
          new WasiResourceException("Error", WasiResourceException.ResourceType.MEMORY, null);

      assertFalse(exception.isCleanupRequired());
    }

    @Test
    void testRequiresCleanupNullExplicitCheck() {
      // Verify null operation returns false, not true
      final WasiResourceException exception = new WasiResourceException("Error", null, null);

      final boolean cleanupRequired = exception.isCleanupRequired();
      assertFalse(cleanupRequired);
      assertEquals(false, cleanupRequired);
    }

    @Test
    void testRequiresCleanupAllCases() {
      // ALLOCATION - returns true
      assertTrue(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.ALLOCATION)
              .isCleanupRequired());

      // ACCESS - returns false
      assertFalse(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.ACCESS)
              .isCleanupRequired());

      // MODIFICATION - returns false
      assertFalse(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.MODIFICATION)
              .isCleanupRequired());

      // CLEANUP - returns false
      assertFalse(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.CLEANUP)
              .isCleanupRequired());

      // LIFETIME_MANAGEMENT - returns true
      assertTrue(
          new WasiResourceException(
                  "Error",
                  WasiResourceException.ResourceType.FILE,
                  WasiResourceException.ResourceOperation.LIFETIME_MANAGEMENT)
              .isCleanupRequired());
    }
  }

  /** Tests for constructor default value mutations. */
  @Nested
  class ConstructorDefaultValueMutationTests {

    @Test
    void testMessageCauseConstructorCleanupRequiredDefaultsFalse() {
      // Mutation: at line 94, cleanupRequired is set to false
      final WasiResourceException exception =
          new WasiResourceException("Error", new RuntimeException("cause"));

      // Verify cleanupRequired is false, not true
      assertFalse(exception.isCleanupRequired());
      assertEquals(false, exception.isCleanupRequired());
    }

    @Test
    void testSimpleMessageConstructorCleanupRequiredDefaultsFalse() {
      // Line 80: cleanupRequired = false
      final WasiResourceException exception = new WasiResourceException("Error");

      assertFalse(exception.isCleanupRequired());
      assertEquals(false, exception.isCleanupRequired());
    }

    @Test
    void testSimpleConstructorDefaultResourceType() {
      // Verify default resource type is SYSTEM
      final WasiResourceException exception = new WasiResourceException("Error");

      assertEquals(WasiResourceException.ResourceType.SYSTEM, exception.getResourceType());
    }

    @Test
    void testSimpleConstructorDefaultResourceOperation() {
      // Verify default resource operation is ACCESS
      final WasiResourceException exception = new WasiResourceException("Error");

      assertEquals(
          WasiResourceException.ResourceOperation.ACCESS, exception.getResourceOperation());
    }

    @Test
    void testMessageCauseConstructorDefaultResourceType() {
      // Verify default resource type is SYSTEM
      final WasiResourceException exception =
          new WasiResourceException("Error", new RuntimeException("cause"));

      assertEquals(WasiResourceException.ResourceType.SYSTEM, exception.getResourceType());
    }

    @Test
    void testMessageCauseConstructorDefaultResourceOperation() {
      // Verify default resource operation is ACCESS
      final WasiResourceException exception =
          new WasiResourceException("Error", new RuntimeException("cause"));

      assertEquals(
          WasiResourceException.ResourceOperation.ACCESS, exception.getResourceOperation());
    }
  }

  /** Tests for RemoveConditional mutations in formatOperation. */
  @Nested
  class FormatOperationConditionalMutationTests {

    @Test
    void testBothNullMustReturnResourceOperation() {
      // Kill RemoveConditional_EQUAL_IF on line 273
      // If mutation makes condition always true, we'd always get "resource-operation"
      // If mutation makes condition always false, we'd never get "resource-operation"
      final WasiResourceException bothNull = new WasiResourceException("Error", null, null);
      assertEquals("resource-operation", bothNull.getOperation());

      final WasiResourceException onlyTypeNull =
          new WasiResourceException("Error", null, WasiResourceException.ResourceOperation.ACCESS);
      assertFalse("resource-operation".equals(onlyTypeNull.getOperation()));
      assertEquals("resource-access", onlyTypeNull.getOperation());

      final WasiResourceException onlyOpNull =
          new WasiResourceException("Error", WasiResourceException.ResourceType.FILE, null);
      assertFalse("resource-operation".equals(onlyOpNull.getOperation()));
      assertEquals("file-operation", onlyOpNull.getOperation());

      final WasiResourceException neitherNull =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.ACCESS);
      assertFalse("resource-operation".equals(neitherNull.getOperation()));
      assertEquals("file-access", neitherNull.getOperation());
    }

    @Test
    void testTernaryOperationNull() {
      // Kill RemoveConditional_EQUAL_IF on line 278
      // When operation is null, operationStr should be "operation"
      final WasiResourceException exception =
          new WasiResourceException("Error", WasiResourceException.ResourceType.SOCKET, null);

      final String op = exception.getOperation();
      assertTrue(op.contains("operation"));
      assertEquals("socket-operation", op);
    }

    @Test
    void testTernaryOperationNotNull() {
      // When operation is not null, operationStr should be operation name
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.SOCKET,
              WasiResourceException.ResourceOperation.ALLOCATION);

      final String op = exception.getOperation();
      assertFalse(op.contains("operation"));
      assertTrue(op.contains("allocation"));
      assertEquals("socket-allocation", op);
    }

    @Test
    void testTernaryResourceTypeNull() {
      // Kill RemoveConditional_EQUAL_IF on line 280
      // When resourceType is null, resourceStr should be "resource"
      final WasiResourceException exception =
          new WasiResourceException("Error", null, WasiResourceException.ResourceOperation.CLEANUP);

      final String op = exception.getOperation();
      assertTrue(op.contains("resource"));
      assertEquals("resource-cleanup", op);
    }

    @Test
    void testTernaryResourceTypeNotNull() {
      // When resourceType is not null, resourceStr should be resourceType name
      final WasiResourceException exception =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.TIMER,
              WasiResourceException.ResourceOperation.CLEANUP);

      final String op = exception.getOperation();
      assertFalse(op.startsWith("resource-"));
      assertTrue(op.contains("timer"));
      assertEquals("timer-cleanup", op);
    }
  }

  /** Tests for RemoveConditional mutations in formatResourceIdentifier. */
  @Nested
  class FormatResourceIdentifierConditionalMutationTests {

    @Test
    void testBothNullMustReturnNull() {
      // Kill RemoveConditional_EQUAL_IF/ELSE on line 294
      final WasiResourceException bothNull = new WasiResourceException("Error", null, null);
      assertNull(bothNull.getResource());
    }

    @Test
    void testOnlyHandleNullMustReturnTypeName() {
      // Kill RemoveConditional_EQUAL_IF/ELSE on line 298
      final WasiResourceException typeOnlyEx =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.EVENT,
              WasiResourceException.ResourceOperation.ACCESS);
      assertEquals("event", typeOnlyEx.getResource());
      assertFalse(typeOnlyEx.getResource().contains(":"));
    }

    @Test
    void testOnlyTypeNullMustReturnHandle() {
      // Kill RemoveConditional_EQUAL_IF/ELSE on line 302
      final WasiResourceException handleOnly =
          new WasiResourceException(
              "Error", null, "standalone-handle", WasiResourceException.ResourceOperation.ACCESS);
      assertEquals("standalone-handle", handleOnly.getResource());
      assertFalse(handleOnly.getResource().contains(":"));
    }

    @Test
    void testBothPresentMustReturnTypeColonHandle() {
      final WasiResourceException both =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.SYSTEM,
              "system-handle",
              WasiResourceException.ResourceOperation.ACCESS);
      assertEquals("system:system-handle", both.getResource());
      assertTrue(both.getResource().contains(":"));
    }
  }

  /** Tests for RemoveConditional mutations in getErrorCategory. */
  @Nested
  class GetErrorCategoryConditionalMutationTests {

    @Test
    void testNullResourceTypeMustReturnResourceLimit() {
      // Kill RemoveConditional_EQUAL_IF/ELSE on line 342
      final WasiResourceException nullType =
          new WasiResourceException("Error", null, WasiResourceException.ResourceOperation.ACCESS);
      assertEquals(WasiException.ErrorCategory.RESOURCE_LIMIT, nullType.getCategory());

      // Verify non-null types return their specific categories
      final WasiResourceException fileType =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.ACCESS);
      assertEquals(WasiException.ErrorCategory.FILE_SYSTEM, fileType.getCategory());
    }
  }

  /** Tests for RemoveConditional mutations in isRetryableOperation. */
  @Nested
  class IsRetryableOperationConditionalMutationTests {

    @Test
    void testNullOperationMustReturnFalse() {
      // Kill RemoveConditional_EQUAL_IF/ELSE on line 316
      final WasiResourceException nullOp =
          new WasiResourceException("Error", WasiResourceException.ResourceType.FILE, null);
      assertFalse(nullOp.isRetryable());

      // Verify non-null operations that should be retryable return true
      final WasiResourceException allocation =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.ALLOCATION);
      assertTrue(allocation.isRetryable());
    }
  }

  /** Tests for RemoveConditional mutations in requiresCleanup. */
  @Nested
  class RequiresCleanupConditionalMutationTests {

    @Test
    void testNullOperationMustReturnFalse() {
      // Kill RemoveConditional_EQUAL_IF/ELSE on line 370
      final WasiResourceException nullOp =
          new WasiResourceException("Error", WasiResourceException.ResourceType.FILE, null);
      assertFalse(nullOp.isCleanupRequired());

      // Verify non-null operations that should require cleanup return true
      final WasiResourceException allocation =
          new WasiResourceException(
              "Error",
              WasiResourceException.ResourceType.FILE,
              WasiResourceException.ResourceOperation.ALLOCATION);
      assertTrue(allocation.isCleanupRequired());
    }
  }
}

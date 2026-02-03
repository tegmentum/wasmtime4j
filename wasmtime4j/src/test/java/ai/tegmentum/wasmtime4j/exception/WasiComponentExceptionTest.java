package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for WasiComponentException class. */
class WasiComponentExceptionTest {

  @Nested
  class ConstructorTests {

    @Test
    void testSimpleMessageConstructor() {
      final String message = "Component operation failed";
      final WasiComponentException exception = new WasiComponentException(message);

      assertEquals(
          "Component operation failed (operation: component-operation)", exception.getMessage());
      assertNull(exception.getCause());
      assertEquals("component-operation", exception.getOperation());
      assertNull(exception.getComponentId());
      assertNull(exception.getInterfaceName());
      assertEquals(
          WasiComponentException.ComponentOperation.EXECUTION, exception.getOperationType());
      assertFalse(exception.isRetryable());
      assertEquals(WasiException.ErrorCategory.COMPONENT, exception.getCategory());
    }

    @Test
    void testMessageWithCauseConstructor() {
      final String message = "Component failed";
      final RuntimeException cause = new RuntimeException("Native error");
      final WasiComponentException exception = new WasiComponentException(message, cause);

      assertEquals("Component failed (operation: component-operation)", exception.getMessage());
      assertEquals(cause, exception.getCause());
      assertEquals("component-operation", exception.getOperation());
      assertNull(exception.getComponentId());
      assertNull(exception.getInterfaceName());
      assertEquals(
          WasiComponentException.ComponentOperation.EXECUTION, exception.getOperationType());
    }

    @Test
    void testComponentSpecificConstructor() {
      final String message = "Component instantiation failed";
      final String componentId = "test-component";
      final WasiComponentException.ComponentOperation operationType =
          WasiComponentException.ComponentOperation.INSTANTIATION;

      final WasiComponentException exception =
          new WasiComponentException(message, componentId, operationType);

      assertTrue(exception.getMessage().contains(message));
      assertEquals("component-instantiation", exception.getOperation());
      assertEquals(componentId, exception.getResource());
      assertEquals(componentId, exception.getComponentId());
      assertNull(exception.getInterfaceName());
      assertEquals(operationType, exception.getOperationType());
      assertTrue(exception.isRetryable());
    }

    @Test
    void testInterfaceSpecificConstructor() {
      final String message = "Interface binding failed";
      final String componentId = "test-component";
      final String interfaceName = "test-interface";
      final WasiComponentException.ComponentOperation operationType =
          WasiComponentException.ComponentOperation.INTERFACE_BINDING;

      final WasiComponentException exception =
          new WasiComponentException(message, componentId, interfaceName, operationType);

      assertTrue(exception.getMessage().contains(message));
      assertEquals("interface-binding", exception.getOperation());
      assertEquals("test-component:test-interface", exception.getResource());
      assertEquals(componentId, exception.getComponentId());
      assertEquals(interfaceName, exception.getInterfaceName());
      assertEquals(operationType, exception.getOperationType());
      assertFalse(exception.isRetryable());
    }

    @Test
    void testFullConstructorWithCause() {
      final String message = "Component linking failed";
      final String componentId = "test-component";
      final String interfaceName = "test-interface";
      final WasiComponentException.ComponentOperation operationType =
          WasiComponentException.ComponentOperation.LINKING;
      final RuntimeException cause = new RuntimeException("Link error");

      final WasiComponentException exception =
          new WasiComponentException(message, componentId, interfaceName, operationType, cause);

      assertTrue(exception.getMessage().contains(message));
      assertEquals(cause, exception.getCause());
      assertEquals("component-linking", exception.getOperation());
      assertEquals("test-component:test-interface", exception.getResource());
      assertEquals(componentId, exception.getComponentId());
      assertEquals(interfaceName, exception.getInterfaceName());
      assertEquals(operationType, exception.getOperationType());
    }
  }

  @Nested
  class OperationTypeTests {

    @Test
    void testIsInstantiationError() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", "comp", WasiComponentException.ComponentOperation.INSTANTIATION);
      assertTrue(exception.isInstantiationError());
      assertFalse(exception.isInterfaceBindingError());
      assertFalse(exception.isResolutionError());
      assertFalse(exception.isLinkingError());
      assertFalse(exception.isExecutionError());
    }

    @Test
    void testIsInterfaceBindingError() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", "comp", WasiComponentException.ComponentOperation.INTERFACE_BINDING);
      assertFalse(exception.isInstantiationError());
      assertTrue(exception.isInterfaceBindingError());
      assertFalse(exception.isResolutionError());
      assertFalse(exception.isLinkingError());
      assertFalse(exception.isExecutionError());
    }

    @Test
    void testIsResolutionError() {
      final WasiComponentException exportException =
          new WasiComponentException(
              "Error", "comp", WasiComponentException.ComponentOperation.EXPORT_RESOLUTION);
      assertTrue(exportException.isResolutionError());
      assertFalse(exportException.isInstantiationError());

      final WasiComponentException importException =
          new WasiComponentException(
              "Error", "comp", WasiComponentException.ComponentOperation.IMPORT_RESOLUTION);
      assertTrue(importException.isResolutionError());
      assertFalse(importException.isInstantiationError());
    }

    @Test
    void testIsLinkingError() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", "comp", WasiComponentException.ComponentOperation.LINKING);
      assertFalse(exception.isInstantiationError());
      assertFalse(exception.isInterfaceBindingError());
      assertFalse(exception.isResolutionError());
      assertTrue(exception.isLinkingError());
      assertFalse(exception.isExecutionError());
    }

    @Test
    void testIsExecutionError() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", "comp", WasiComponentException.ComponentOperation.EXECUTION);
      assertFalse(exception.isInstantiationError());
      assertFalse(exception.isInterfaceBindingError());
      assertFalse(exception.isResolutionError());
      assertFalse(exception.isLinkingError());
      assertTrue(exception.isExecutionError());
    }
  }

  @Nested
  class RetryLogicTests {

    @Test
    void testInstantiationRetryable() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", "comp", WasiComponentException.ComponentOperation.INSTANTIATION);
      assertTrue(exception.isRetryable());
    }

    @Test
    void testExecutionRetryable() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", "comp", WasiComponentException.ComponentOperation.EXECUTION);
      assertTrue(exception.isRetryable());
    }

    @Test
    void testConfigurationOperationsNotRetryable() {
      assertFalse(
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.INTERFACE_BINDING)
              .isRetryable());
      assertFalse(
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.EXPORT_RESOLUTION)
              .isRetryable());
      assertFalse(
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.IMPORT_RESOLUTION)
              .isRetryable());
      assertFalse(
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.LINKING)
              .isRetryable());
      assertFalse(
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.LIFECYCLE)
              .isRetryable());
    }
  }

  @Nested
  class MessageFormattingTests {

    @Test
    void testOperationFormatting() {
      assertEquals(
          "component-instantiation",
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.INSTANTIATION)
              .getOperation());
      assertEquals(
          "interface-binding",
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.INTERFACE_BINDING)
              .getOperation());
      assertEquals(
          "export-resolution",
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.EXPORT_RESOLUTION)
              .getOperation());
      assertEquals(
          "import-resolution",
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.IMPORT_RESOLUTION)
              .getOperation());
      assertEquals(
          "component-linking",
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.LINKING)
              .getOperation());
      assertEquals(
          "component-execution",
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.EXECUTION)
              .getOperation());
      assertEquals(
          "component-lifecycle",
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.LIFECYCLE)
              .getOperation());
    }

    @Test
    void testResourceFormattingComponentOnly() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", "test-component", WasiComponentException.ComponentOperation.INSTANTIATION);
      assertEquals("test-component", exception.getResource());
    }

    @Test
    void testResourceFormattingComponentAndInterface() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error",
              "test-component",
              "test-interface",
              WasiComponentException.ComponentOperation.INTERFACE_BINDING);
      assertEquals("test-component:test-interface", exception.getResource());
    }

    @Test
    void testResourceFormattingNullComponent() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error",
              null,
              "test-interface",
              WasiComponentException.ComponentOperation.INTERFACE_BINDING);
      assertEquals("interface:test-interface", exception.getResource());
    }

    @Test
    void testResourceFormattingNullInputs() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", null, null, WasiComponentException.ComponentOperation.EXECUTION);
      assertNull(exception.getResource());
    }
  }

  @Nested
  class InheritanceTests {

    @Test
    void testWasiComponentExceptionExtendsWasiException() {
      final WasiComponentException exception = new WasiComponentException("Test error");
      assertTrue(exception instanceof WasiException);
    }

    @Test
    void testWasiComponentExceptionExtendsWasmException() {
      final WasiComponentException exception = new WasiComponentException("Test error");
      assertTrue(exception instanceof WasmException);
    }
  }

  @Nested
  class ComponentOperationEnumTests {

    @Test
    void testComponentOperationValues() {
      final WasiComponentException.ComponentOperation[] operations =
          WasiComponentException.ComponentOperation.values();
      assertEquals(7, operations.length);

      assertTrue(contains(operations, WasiComponentException.ComponentOperation.INSTANTIATION));
      assertTrue(contains(operations, WasiComponentException.ComponentOperation.INTERFACE_BINDING));
      assertTrue(contains(operations, WasiComponentException.ComponentOperation.EXPORT_RESOLUTION));
      assertTrue(contains(operations, WasiComponentException.ComponentOperation.IMPORT_RESOLUTION));
      assertTrue(contains(operations, WasiComponentException.ComponentOperation.LINKING));
      assertTrue(contains(operations, WasiComponentException.ComponentOperation.EXECUTION));
      assertTrue(contains(operations, WasiComponentException.ComponentOperation.LIFECYCLE));
    }

    private boolean contains(
        final WasiComponentException.ComponentOperation[] array,
        final WasiComponentException.ComponentOperation value) {
      for (final WasiComponentException.ComponentOperation operation : array) {
        if (operation == value) {
          return true;
        }
      }
      return false;
    }
  }

  @Nested
  class EdgeCaseTests {

    @Test
    void testNullOperationTypeHandling() {
      // This tests internal method behavior - null operation types should be handled gracefully
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", "comp", WasiComponentException.ComponentOperation.EXECUTION);
      assertNotNull(exception.getOperationType());
    }

    @Test
    void testEmptyComponentIdHandling() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", "", WasiComponentException.ComponentOperation.EXECUTION);
      assertEquals("", exception.getComponentId());
    }

    @Test
    void testEmptyInterfaceNameHandling() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", "comp", "", WasiComponentException.ComponentOperation.INTERFACE_BINDING);
      assertEquals("", exception.getInterfaceName());
    }
  }

  @Nested
  class NullParameterHandlingTests {

    @Test
    void testNullOperationTypeInThreeArgConstructor() {
      // Test with null operationType - formatOperation should return "component-operation"
      final WasiComponentException exception =
          new WasiComponentException("Error", "test-component", null);

      assertEquals("component-operation", exception.getOperation());
      assertEquals("test-component", exception.getResource());
      assertFalse(exception.isRetryable());
      assertNull(exception.getOperationType());
    }

    @Test
    void testNullOperationTypeInFourArgConstructor() {
      // Test with null operationType in 4-arg constructor
      final WasiComponentException exception =
          new WasiComponentException("Error", "test-component", "test-interface", null);

      assertEquals("component-operation", exception.getOperation());
      assertEquals("test-component:test-interface", exception.getResource());
      assertFalse(exception.isRetryable());
      assertNull(exception.getOperationType());
    }

    @Test
    void testNullOperationTypeInFiveArgConstructor() {
      // Test with null operationType in 5-arg constructor
      final RuntimeException cause = new RuntimeException("Test cause");
      final WasiComponentException exception =
          new WasiComponentException("Error", "test-component", "test-interface", null, cause);

      assertEquals("component-operation", exception.getOperation());
      assertEquals("test-component:test-interface", exception.getResource());
      assertFalse(exception.isRetryable());
      assertNull(exception.getOperationType());
      assertEquals(cause, exception.getCause());
    }

    @Test
    void testNullComponentIdAndInterfaceName() {
      // Test with both componentId and interfaceName null
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", null, null, WasiComponentException.ComponentOperation.EXECUTION);

      assertNull(exception.getResource());
      assertNull(exception.getComponentId());
      assertNull(exception.getInterfaceName());
    }

    @Test
    void testNullComponentIdWithInterfaceName() {
      // Test with null componentId but non-null interfaceName
      final WasiComponentException exception =
          new WasiComponentException(
              "Error",
              null,
              "test-interface",
              WasiComponentException.ComponentOperation.INTERFACE_BINDING);

      assertEquals("interface:test-interface", exception.getResource());
      assertNull(exception.getComponentId());
      assertEquals("test-interface", exception.getInterfaceName());
    }

    @Test
    void testAllNullsInFourArgConstructor() {
      // Test with all optional parameters as null
      final WasiComponentException exception =
          new WasiComponentException("Error", null, null, null);

      assertEquals("component-operation", exception.getOperation());
      assertNull(exception.getResource());
      assertFalse(exception.isRetryable());
      assertNull(exception.getOperationType());
      assertNull(exception.getComponentId());
      assertNull(exception.getInterfaceName());
    }
  }

  @Nested
  class ConstructorDefaultValueMutationTests {

    @Test
    void testMessageWithCauseConstructorDefaultsRetryableToFalse() {
      // Kill mutation on line 71: Substituted 0 with 1
      final RuntimeException cause = new RuntimeException("Test cause");
      final WasiComponentException exception = new WasiComponentException("Error", cause);

      assertFalse(exception.isRetryable());
      assertEquals(false, exception.isRetryable());
    }

    @Test
    void testSimpleMessageConstructorDefaultsRetryableToFalse() {
      final WasiComponentException exception = new WasiComponentException("Error");

      assertFalse(exception.isRetryable());
      assertEquals(false, exception.isRetryable());
    }

    @Test
    void testNullOperationTypeDefaultsRetryableToFalse() {
      // When operationType is null, isRetryableOperation should return false
      final WasiComponentException exception =
          new WasiComponentException("Error", "comp", null);

      assertFalse(exception.isRetryable());
      assertEquals(false, exception.isRetryable());
    }
  }

  @Nested
  class FormatOperationMutationTests {

    @Test
    void testFormatOperationWithNullReturnsComponentOperation() {
      // Kill mutation at line 228: null check
      final WasiComponentException exception =
          new WasiComponentException("Error", "comp", null);

      assertEquals("component-operation", exception.getOperation());
      // Verify it's exactly this string
      assertTrue(exception.getOperation().equals("component-operation"));
    }

    @Test
    void testFormatOperationWithNonNullReturnsFormattedString() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", "comp", WasiComponentException.ComponentOperation.LIFECYCLE);

      assertEquals("component-lifecycle", exception.getOperation());
      assertFalse(exception.getOperation().equals("component-operation"));
    }
  }

  @Nested
  class FormatResourceMutationTests {

    @Test
    void testFormatResourceBothNull() {
      // Kill mutations at line 260: compound null check
      final WasiComponentException exception =
          new WasiComponentException(
              "Error", null, null, WasiComponentException.ComponentOperation.EXECUTION);

      assertNull(exception.getResource());
    }

    @Test
    void testFormatResourceOnlyComponentIdNull() {
      // Kill mutations at line 264/268: null componentId with non-null interface
      final WasiComponentException exception =
          new WasiComponentException(
              "Error",
              null,
              "my-interface",
              WasiComponentException.ComponentOperation.INTERFACE_BINDING);

      assertEquals("interface:my-interface", exception.getResource());
      assertTrue(exception.getResource().startsWith("interface:"));
    }

    @Test
    void testFormatResourceOnlyInterfaceNameNull() {
      // When interfaceName is null but componentId is not
      final WasiComponentException exception =
          new WasiComponentException(
              "Error",
              "my-component",
              null,
              WasiComponentException.ComponentOperation.EXECUTION);

      assertEquals("my-component", exception.getResource());
      assertFalse(exception.getResource().contains(":"));
    }

    @Test
    void testFormatResourceBothPresent() {
      final WasiComponentException exception =
          new WasiComponentException(
              "Error",
              "my-component",
              "my-interface",
              WasiComponentException.ComponentOperation.INTERFACE_BINDING);

      assertEquals("my-component:my-interface", exception.getResource());
      assertTrue(exception.getResource().contains(":"));
    }

    @Test
    void testFormatResourceFirstConditionBothNull() {
      // Test first condition: componentId == null && interfaceName == null
      final WasiComponentException bothNull =
          new WasiComponentException(
              "Error", null, null, WasiComponentException.ComponentOperation.EXECUTION);
      assertNull(bothNull.getResource());

      // Test when only one is null (should not return null)
      final WasiComponentException onlyCompNull =
          new WasiComponentException(
              "Error",
              null,
              "interface",
              WasiComponentException.ComponentOperation.EXECUTION);
      assertNotNull(onlyCompNull.getResource());

      final WasiComponentException onlyIntfNull =
          new WasiComponentException(
              "Error",
              "component",
              null,
              WasiComponentException.ComponentOperation.EXECUTION);
      assertNotNull(onlyIntfNull.getResource());
    }
  }

  @Nested
  class IsRetryableOperationMutationTests {

    @Test
    void testIsRetryableOperationWithNull() {
      // Kill mutation at line 282: null check
      final WasiComponentException exception =
          new WasiComponentException("Error", "comp", null);

      assertFalse(exception.isRetryable());
    }

    @Test
    void testIsRetryableOperationAllCases() {
      // INSTANTIATION - returns true
      assertTrue(
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.INSTANTIATION)
              .isRetryable());

      // EXECUTION - returns true
      assertTrue(
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.EXECUTION)
              .isRetryable());

      // INTERFACE_BINDING - returns false
      assertFalse(
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.INTERFACE_BINDING)
              .isRetryable());

      // EXPORT_RESOLUTION - returns false
      assertFalse(
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.EXPORT_RESOLUTION)
              .isRetryable());

      // IMPORT_RESOLUTION - returns false
      assertFalse(
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.IMPORT_RESOLUTION)
              .isRetryable());

      // LINKING - returns false
      assertFalse(
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.LINKING)
              .isRetryable());

      // LIFECYCLE - returns false
      assertFalse(
          new WasiComponentException(
                  "Error", "comp", WasiComponentException.ComponentOperation.LIFECYCLE)
              .isRetryable());
    }
  }
}

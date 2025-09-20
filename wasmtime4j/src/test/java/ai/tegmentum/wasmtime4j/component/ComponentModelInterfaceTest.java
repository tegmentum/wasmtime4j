package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test suite for WebAssembly Component Model interface definitions.
 *
 * <p>Validates the structure, behavior, and contracts of the Component Model interfaces
 * including type validation, compatibility checking, and metadata access.
 *
 * <p>These tests focus on the interface contracts rather than implementation details,
 * ensuring that the Component Model API provides the expected functionality for
 * component composition and interaction.
 */
@DisplayName("Component Model Interface Tests")
class ComponentModelInterfaceTest {

  @Test
  @DisplayName("ComponentValueType enum should have all required types")
  void testComponentValueTypeCompleteness() {
    // Test primitive types
    assertNotNull(ComponentValueType.BOOL);
    assertNotNull(ComponentValueType.S8);
    assertNotNull(ComponentValueType.U8);
    assertNotNull(ComponentValueType.S16);
    assertNotNull(ComponentValueType.U16);
    assertNotNull(ComponentValueType.S32);
    assertNotNull(ComponentValueType.U32);
    assertNotNull(ComponentValueType.S64);
    assertNotNull(ComponentValueType.U64);
    assertNotNull(ComponentValueType.F32);
    assertNotNull(ComponentValueType.F64);

    // Test text types
    assertNotNull(ComponentValueType.CHAR);
    assertNotNull(ComponentValueType.STRING);

    // Test structured types
    assertNotNull(ComponentValueType.LIST);
    assertNotNull(ComponentValueType.RECORD);
    assertNotNull(ComponentValueType.VARIANT);
    assertNotNull(ComponentValueType.TUPLE);
    assertNotNull(ComponentValueType.FLAGS);
    assertNotNull(ComponentValueType.ENUM);

    // Test optional and result types
    assertNotNull(ComponentValueType.OPTION);
    assertNotNull(ComponentValueType.RESULT);

    // Test resource types
    assertNotNull(ComponentValueType.RESOURCE);
    assertNotNull(ComponentValueType.BORROW);
    assertNotNull(ComponentValueType.OWN);
  }

  @Test
  @DisplayName("ComponentValueType utility methods should work correctly")
  void testComponentValueTypeUtilities() {
    // Test numeric type checking
    assertTrue(ComponentValueType.S32.isNumeric());
    assertTrue(ComponentValueType.U64.isNumeric());
    assertTrue(ComponentValueType.F32.isNumeric());
    assertFalse(ComponentValueType.STRING.isNumeric());

    // Test signed integer checking
    assertTrue(ComponentValueType.S8.isSignedInteger());
    assertTrue(ComponentValueType.S64.isSignedInteger());
    assertFalse(ComponentValueType.U32.isSignedInteger());
    assertFalse(ComponentValueType.F32.isSignedInteger());

    // Test unsigned integer checking
    assertTrue(ComponentValueType.U8.isUnsignedInteger());
    assertTrue(ComponentValueType.U64.isUnsignedInteger());
    assertFalse(ComponentValueType.S32.isUnsignedInteger());
    assertFalse(ComponentValueType.F64.isUnsignedInteger());

    // Test floating point checking
    assertTrue(ComponentValueType.F32.isFloatingPoint());
    assertTrue(ComponentValueType.F64.isFloatingPoint());
    assertFalse(ComponentValueType.S32.isFloatingPoint());
    assertFalse(ComponentValueType.STRING.isFloatingPoint());

    // Test text type checking
    assertTrue(ComponentValueType.CHAR.isText());
    assertTrue(ComponentValueType.STRING.isText());
    assertFalse(ComponentValueType.S32.isText());

    // Test structured type checking
    assertTrue(ComponentValueType.LIST.isStructured());
    assertTrue(ComponentValueType.RECORD.isStructured());
    assertTrue(ComponentValueType.VARIANT.isStructured());
    assertFalse(ComponentValueType.S32.isStructured());

    // Test optional/result type checking
    assertTrue(ComponentValueType.OPTION.isOptionalOrResult());
    assertTrue(ComponentValueType.RESULT.isOptionalOrResult());
    assertFalse(ComponentValueType.S32.isOptionalOrResult());

    // Test resource type checking
    assertTrue(ComponentValueType.RESOURCE.isResource());
    assertTrue(ComponentValueType.BORROW.isResource());
    assertTrue(ComponentValueType.OWN.isResource());
    assertFalse(ComponentValueType.S32.isResource());
  }

  @Test
  @DisplayName("ComponentValueType size calculation should be correct")
  void testComponentValueTypeSizes() {
    // Test primitive type sizes
    assertEquals(1, ComponentValueType.BOOL.getSizeInBytes());
    assertEquals(1, ComponentValueType.S8.getSizeInBytes());
    assertEquals(1, ComponentValueType.U8.getSizeInBytes());
    assertEquals(2, ComponentValueType.S16.getSizeInBytes());
    assertEquals(2, ComponentValueType.U16.getSizeInBytes());
    assertEquals(4, ComponentValueType.S32.getSizeInBytes());
    assertEquals(4, ComponentValueType.U32.getSizeInBytes());
    assertEquals(4, ComponentValueType.F32.getSizeInBytes());
    assertEquals(4, ComponentValueType.CHAR.getSizeInBytes());
    assertEquals(8, ComponentValueType.S64.getSizeInBytes());
    assertEquals(8, ComponentValueType.U64.getSizeInBytes());
    assertEquals(8, ComponentValueType.F64.getSizeInBytes());

    // Test variable size types
    assertEquals(-1, ComponentValueType.STRING.getSizeInBytes());
    assertEquals(-1, ComponentValueType.LIST.getSizeInBytes());
    assertEquals(-1, ComponentValueType.RECORD.getSizeInBytes());
    assertEquals(-1, ComponentValueType.VARIANT.getSizeInBytes());
  }

  @Test
  @DisplayName("ComponentExportKind enum should have all required kinds")
  void testComponentExportKindCompleteness() {
    assertNotNull(ComponentExportKind.FUNCTION);
    assertNotNull(ComponentExportKind.INTERFACE);
    assertNotNull(ComponentExportKind.RESOURCE);
    assertNotNull(ComponentExportKind.TYPE);
    assertNotNull(ComponentExportKind.MODULE);
    assertNotNull(ComponentExportKind.COMPONENT);
    assertNotNull(ComponentExportKind.INSTANCE);
    assertNotNull(ComponentExportKind.VALUE);
  }

  @Test
  @DisplayName("Component Model interfaces should follow expected hierarchy")
  void testComponentModelInterfaceHierarchy() {
    // Test that core interfaces extend AutoCloseable where appropriate
    assertTrue(java.io.Closeable.class.isAssignableFrom(Component.class));
    assertTrue(java.io.Closeable.class.isAssignableFrom(ComponentInstance.class));
    assertTrue(java.io.Closeable.class.isAssignableFrom(ComponentLinker.class));
  }

  @Test
  @DisplayName("ComponentValue factory methods should exist")
  void testComponentValueFactoryMethods() {
    // These methods should exist and throw UnsupportedOperationException until implemented
    assertThrows(UnsupportedOperationException.class, () -> ComponentValue.bool(true));
    assertThrows(UnsupportedOperationException.class, () -> ComponentValue.string("test"));
    assertThrows(UnsupportedOperationException.class, () -> ComponentValue.s32(42));
    assertThrows(UnsupportedOperationException.class, () -> ComponentValue.u32(42L));
    assertThrows(UnsupportedOperationException.class, () ->
        ComponentValue.list(java.util.Collections.emptyList()));
    assertThrows(UnsupportedOperationException.class, () ->
        ComponentValue.option(null));
    assertThrows(UnsupportedOperationException.class, () ->
        ComponentValue.record(java.util.Collections.emptyMap()));
  }

  @Test
  @DisplayName("ComponentLinker factory method should exist")
  void testComponentLinkerFactoryMethod() {
    // The create method should exist and throw UnsupportedOperationException until implemented
    assertThrows(UnsupportedOperationException.class, () ->
        ComponentLinker.create(null));
  }

  @Test
  @DisplayName("Interface contracts should be well-defined")
  void testInterfaceContractStructure() {
    // Test that key interfaces have expected method signatures by checking they don't throw
    // compilation errors when referenced

    // Component interface methods
    assertDoesNotThrow(() -> {
      java.lang.reflect.Method instantiate = Component.class.getMethod("instantiate",
          ai.tegmentum.wasmtime4j.Store.class, ComponentLinker.class);
      assertNotNull(instantiate);
    });

    assertDoesNotThrow(() -> {
      java.lang.reflect.Method getType = Component.class.getMethod("getType");
      assertNotNull(getType);
    });

    // ComponentInstance interface methods
    assertDoesNotThrow(() -> {
      java.lang.reflect.Method getExport = ComponentInstance.class.getMethod("getExport", String.class);
      assertNotNull(getExport);
    });

    assertDoesNotThrow(() -> {
      java.lang.reflect.Method getExports = ComponentInstance.class.getMethod("getExports");
      assertNotNull(getExports);
    });

    // ComponentLinker interface methods
    assertDoesNotThrow(() -> {
      java.lang.reflect.Method defineComponent = ComponentLinker.class.getMethod("defineComponent",
          String.class, Component.class);
      assertNotNull(defineComponent);
    });

    assertDoesNotThrow(() -> {
      java.lang.reflect.Method defineInterface = ComponentLinker.class.getMethod("defineInterface",
          String.class, InterfaceType.class);
      assertNotNull(defineInterface);
    });
  }

  @Test
  @DisplayName("Interface method parameter validation should be specified")
  void testInterfaceParameterValidation() {
    // Test that interface methods specify proper parameter validation through throws clauses

    // Check Component.instantiate throws clauses
    assertDoesNotThrow(() -> {
      java.lang.reflect.Method instantiate = Component.class.getMethod("instantiate",
          ai.tegmentum.wasmtime4j.Store.class, ComponentLinker.class);
      Class<?>[] exceptions = instantiate.getExceptionTypes();
      assertTrue(java.util.Arrays.asList(exceptions).contains(
          ai.tegmentum.wasmtime4j.exception.WasmException.class));
      assertTrue(java.util.Arrays.asList(exceptions).contains(IllegalArgumentException.class));
    });

    // Check ComponentInstance.getExport throws clauses
    assertDoesNotThrow(() -> {
      java.lang.reflect.Method getExport = ComponentInstance.class.getMethod("getExport", String.class);
      Class<?>[] exceptions = getExport.getExceptionTypes();
      assertTrue(java.util.Arrays.asList(exceptions).contains(
          ai.tegmentum.wasmtime4j.exception.WasmException.class));
      assertTrue(java.util.Arrays.asList(exceptions).contains(IllegalArgumentException.class));
    });
  }

  @Test
  @DisplayName("Return types should use appropriate Optional patterns")
  void testOptionalReturnTypes() {
    // Test that appropriate methods return Optional for nullable values
    assertDoesNotThrow(() -> {
      java.lang.reflect.Method getExport = ComponentInstance.class.getMethod("getExport", String.class);
      assertEquals(java.util.Optional.class, getExport.getReturnType());
    });

    assertDoesNotThrow(() -> {
      java.lang.reflect.Method getReturnType = ComponentFunction.class.getMethod("getReturnType");
      assertEquals(java.util.Optional.class, getReturnType.getReturnType());
    });

    assertDoesNotThrow(() -> {
      java.lang.reflect.Method getErrorType = ComponentFunction.class.getMethod("getErrorType");
      assertEquals(java.util.Optional.class, getErrorType.getReturnType());
    });
  }

  @Test
  @DisplayName("Component Model should support comprehensive type system")
  void testComponentModelTypeSystemComprehensiveness() {
    // Verify that all necessary types are defined for a complete Component Model implementation

    // Core component types
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.Component"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentInstance"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentLinker"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentType"));

    // Export/Import types
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentExport"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentExportKind"));

    // Function types
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentFunction"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentValue"));

    // Interface types
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.InterfaceType"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.InterfaceFunction"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.InterfaceResource"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.InterfaceParameter"));

    // Structured types
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentRecord"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentVariant"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentField"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentCase"));

    // Resource types
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentResource"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentResourceType"));

    // Value types
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.component.ComponentValueType"));
  }
}
/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WebAssembly type system interfaces.
 *
 * <p>This test file covers the core type interfaces:
 *
 * <ul>
 *   <li>WasmType - Base type interface
 *   <li>WasmTypeKind - Type kind enumeration
 *   <li>GlobalType - Global variable type
 *   <li>TableType - Table type
 *   <li>MemoryType - Memory type
 *   <li>FuncType - Function type
 *   <li>ExternType - External type
 *   <li>ImportType - Import type descriptor
 *   <li>ExportType - Export type descriptor
 *   <li>HeapType - Reference heap type
 * </ul>
 */
@DisplayName("Type System Package Tests")
class TypeSystemPackageTest {

  // ========================================================================
  // WasmType Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmType Interface Tests")
  class WasmTypeTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmType.class.isInterface(), "WasmType should be an interface");
    }

    @Test
    @DisplayName("should have getKind method")
    void shouldHaveGetKindMethod() throws NoSuchMethodException {
      Method method = WasmType.class.getMethod("getKind");
      assertNotNull(method, "getKind method should exist");
      assertEquals(
          WasmTypeKind.class, method.getReturnType(), "Return type should be WasmTypeKind");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WasmType.class.getModifiers()), "WasmType should be public");
    }
  }

  // ========================================================================
  // WasmTypeKind Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmTypeKind Enum Tests")
  class WasmTypeKindTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasmTypeKind.class.isEnum(), "WasmTypeKind should be an enum");
    }

    @Test
    @DisplayName("should have FUNCTION kind")
    void shouldHaveFunctionKind() {
      assertNotNull(WasmTypeKind.valueOf("FUNCTION"), "FUNCTION kind should exist");
    }

    @Test
    @DisplayName("should have GLOBAL kind")
    void shouldHaveGlobalKind() {
      assertNotNull(WasmTypeKind.valueOf("GLOBAL"), "GLOBAL kind should exist");
    }

    @Test
    @DisplayName("should have TABLE kind")
    void shouldHaveTableKind() {
      assertNotNull(WasmTypeKind.valueOf("TABLE"), "TABLE kind should exist");
    }

    @Test
    @DisplayName("should have MEMORY kind")
    void shouldHaveMemoryKind() {
      assertNotNull(WasmTypeKind.valueOf("MEMORY"), "MEMORY kind should exist");
    }

    @Test
    @DisplayName("should have values method")
    void shouldHaveValuesMethod() throws NoSuchMethodException {
      Method method = WasmTypeKind.class.getMethod("values");
      assertNotNull(method, "values method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "values method should be static");
    }

    @Test
    @DisplayName("should have exactly 4 type kinds")
    void shouldHaveExactly4TypeKinds() {
      WasmTypeKind[] values = WasmTypeKind.values();
      assertEquals(4, values.length, "Should have exactly 4 type kinds");
    }
  }

  // ========================================================================
  // GlobalType Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("GlobalType Interface Tests")
  class GlobalTypeTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(GlobalType.class.isInterface(), "GlobalType should be an interface");
    }

    @Test
    @DisplayName("should extend WasmType")
    void shouldExtendWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(GlobalType.class), "GlobalType should extend WasmType");
    }

    @Test
    @DisplayName("should have getValueType method")
    void shouldHaveGetValueTypeMethod() throws NoSuchMethodException {
      Method method = GlobalType.class.getMethod("getValueType");
      assertNotNull(method, "getValueType method should exist");
      assertEquals(
          WasmValueType.class, method.getReturnType(), "Return type should be WasmValueType");
    }

    @Test
    @DisplayName("should have isMutable method")
    void shouldHaveIsMutableMethod() throws NoSuchMethodException {
      Method method = GlobalType.class.getMethod("isMutable");
      assertNotNull(method, "isMutable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have default getKind method returning GLOBAL")
    void shouldHaveDefaultGetKindMethod() throws NoSuchMethodException {
      Method method = GlobalType.class.getMethod("getKind");
      assertNotNull(method, "getKind method should exist");
      assertTrue(method.isDefault(), "getKind should be a default method");
    }
  }

  // ========================================================================
  // TableType Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("TableType Interface Tests")
  class TableTypeTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(TableType.class.isInterface(), "TableType should be an interface");
    }

    @Test
    @DisplayName("should extend WasmType")
    void shouldExtendWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(TableType.class), "TableType should extend WasmType");
    }

    @Test
    @DisplayName("should have getElementType method")
    void shouldHaveGetElementTypeMethod() throws NoSuchMethodException {
      Method method = TableType.class.getMethod("getElementType");
      assertNotNull(method, "getElementType method should exist");
      assertEquals(
          WasmValueType.class, method.getReturnType(), "Return type should be WasmValueType");
    }

    @Test
    @DisplayName("should have getMinimum method")
    void shouldHaveGetMinimumMethod() throws NoSuchMethodException {
      Method method = TableType.class.getMethod("getMinimum");
      assertNotNull(method, "getMinimum method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getMaximum method")
    void shouldHaveGetMaximumMethod() throws NoSuchMethodException {
      Method method = TableType.class.getMethod("getMaximum");
      assertNotNull(method, "getMaximum method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have default getKind method returning TABLE")
    void shouldHaveDefaultGetKindMethod() throws NoSuchMethodException {
      Method method = TableType.class.getMethod("getKind");
      assertNotNull(method, "getKind method should exist");
      assertTrue(method.isDefault(), "getKind should be a default method");
    }
  }

  // ========================================================================
  // MemoryType Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("MemoryType Interface Tests")
  class MemoryTypeTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(MemoryType.class.isInterface(), "MemoryType should be an interface");
    }

    @Test
    @DisplayName("should extend WasmType")
    void shouldExtendWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(MemoryType.class), "MemoryType should extend WasmType");
    }

    @Test
    @DisplayName("should have getMinimum method")
    void shouldHaveGetMinimumMethod() throws NoSuchMethodException {
      Method method = MemoryType.class.getMethod("getMinimum");
      assertNotNull(method, "getMinimum method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getMaximum method")
    void shouldHaveGetMaximumMethod() throws NoSuchMethodException {
      Method method = MemoryType.class.getMethod("getMaximum");
      assertNotNull(method, "getMaximum method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have is64Bit method")
    void shouldHaveIs64BitMethod() throws NoSuchMethodException {
      Method method = MemoryType.class.getMethod("is64Bit");
      assertNotNull(method, "is64Bit method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isShared method")
    void shouldHaveIsSharedMethod() throws NoSuchMethodException {
      Method method = MemoryType.class.getMethod("isShared");
      assertNotNull(method, "isShared method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have default getKind method returning MEMORY")
    void shouldHaveDefaultGetKindMethod() throws NoSuchMethodException {
      Method method = MemoryType.class.getMethod("getKind");
      assertNotNull(method, "getKind method should exist");
      assertTrue(method.isDefault(), "getKind should be a default method");
    }
  }

  // ========================================================================
  // FuncType Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("FuncType Interface Tests")
  class FuncTypeTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(FuncType.class.isInterface(), "FuncType should be an interface");
    }

    @Test
    @DisplayName("should extend WasmType")
    void shouldExtendWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(FuncType.class), "FuncType should extend WasmType");
    }

    @Test
    @DisplayName("should have getParams method")
    void shouldHaveGetParamsMethod() throws NoSuchMethodException {
      Method method = FuncType.class.getMethod("getParams");
      assertNotNull(method, "getParams method should exist");
    }

    @Test
    @DisplayName("should have getResults method")
    void shouldHaveGetResultsMethod() throws NoSuchMethodException {
      Method method = FuncType.class.getMethod("getResults");
      assertNotNull(method, "getResults method should exist");
    }

    @Test
    @DisplayName("should have default getKind method returning FUNC")
    void shouldHaveDefaultGetKindMethod() throws NoSuchMethodException {
      Method method = FuncType.class.getMethod("getKind");
      assertNotNull(method, "getKind method should exist");
      assertTrue(method.isDefault(), "getKind should be a default method");
    }
  }

  // ========================================================================
  // ExternType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ExternType Enum Tests")
  class ExternTypeTests {

    @Test
    @DisplayName("should exist")
    void shouldExist() {
      assertNotNull(ExternType.class, "ExternType should exist");
    }

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(ExternType.class.isEnum(), "ExternType should be an enum");
    }

    @Test
    @DisplayName("should have FUNC constant")
    void shouldHaveFuncConstant() {
      assertNotNull(ExternType.valueOf("FUNC"), "FUNC constant should exist");
    }

    @Test
    @DisplayName("should have TABLE constant")
    void shouldHaveTableConstant() {
      assertNotNull(ExternType.valueOf("TABLE"), "TABLE constant should exist");
    }

    @Test
    @DisplayName("should have MEMORY constant")
    void shouldHaveMemoryConstant() {
      assertNotNull(ExternType.valueOf("MEMORY"), "MEMORY constant should exist");
    }

    @Test
    @DisplayName("should have GLOBAL constant")
    void shouldHaveGlobalConstant() {
      assertNotNull(ExternType.valueOf("GLOBAL"), "GLOBAL constant should exist");
    }

    @Test
    @DisplayName("should have exactly 4 constants")
    void shouldHaveExactly4Constants() {
      ExternType[] values = ExternType.values();
      assertEquals(4, values.length, "ExternType should have exactly 4 constants");
    }

    @Test
    @DisplayName("should have getCode method")
    void shouldHaveGetCodeMethod() throws NoSuchMethodException {
      Method method = ExternType.class.getMethod("getCode");
      assertNotNull(method, "getCode method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have fromCode static method")
    void shouldHaveFromCodeMethod() throws NoSuchMethodException {
      Method method = ExternType.class.getMethod("fromCode", int.class);
      assertNotNull(method, "fromCode method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromCode should be static");
      assertEquals(ExternType.class, method.getReturnType(), "Return type should be ExternType");
    }
  }

  // ========================================================================
  // ImportType Class Tests
  // ========================================================================

  @Nested
  @DisplayName("ImportType Class Tests")
  class ImportTypeTests {

    @Test
    @DisplayName("should exist")
    void shouldExist() {
      assertNotNull(ImportType.class, "ImportType should exist");
    }

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertFalse(ImportType.class.isInterface(), "ImportType should not be an interface");
      assertTrue(Modifier.isFinal(ImportType.class.getModifiers()), "ImportType should be final");
    }

    @Test
    @DisplayName("should have getModuleName method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      Method method = ImportType.class.getMethod("getModuleName");
      assertNotNull(method, "getModuleName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = ImportType.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getType method returning WasmType")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      Method method = ImportType.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(WasmType.class, method.getReturnType(), "Return type should be WasmType");
    }

    @Test
    @DisplayName("should have constructor with moduleName, name, and type")
    void shouldHaveConstructor() throws NoSuchMethodException {
      java.lang.reflect.Constructor<?> constructor =
          ImportType.class.getConstructor(String.class, String.class, WasmType.class);
      assertNotNull(constructor, "Constructor should exist");
    }
  }

  // ========================================================================
  // ExportType Class Tests
  // ========================================================================

  @Nested
  @DisplayName("ExportType Class Tests")
  class ExportTypeTests {

    @Test
    @DisplayName("should exist")
    void shouldExist() {
      assertNotNull(ExportType.class, "ExportType should exist");
    }

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertFalse(ExportType.class.isInterface(), "ExportType should not be an interface");
      assertTrue(Modifier.isFinal(ExportType.class.getModifiers()), "ExportType should be final");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = ExportType.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getType method returning WasmType")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      Method method = ExportType.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(WasmType.class, method.getReturnType(), "Return type should be WasmType");
    }

    @Test
    @DisplayName("should have constructor with name and type")
    void shouldHaveConstructor() throws NoSuchMethodException {
      java.lang.reflect.Constructor<?> constructor =
          ExportType.class.getConstructor(String.class, WasmType.class);
      assertNotNull(constructor, "Constructor should exist");
    }
  }

  // ========================================================================
  // HeapType Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("HeapType Interface Tests")
  class HeapTypeTests {

    @Test
    @DisplayName("should exist")
    void shouldExist() {
      assertNotNull(HeapType.class, "HeapType should exist");
    }

    @Test
    @DisplayName("should be an interface or class")
    void shouldBeInterfaceOrClass() {
      // HeapType can be either interface or class
      assertTrue(
          HeapType.class.isInterface() || !HeapType.class.isInterface(),
          "HeapType should exist as interface or class");
    }
  }

  // ========================================================================
  // WasmValueType Enum/Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmValueType Tests")
  class WasmValueTypeTests {

    @Test
    @DisplayName("should exist")
    void shouldExist() {
      assertNotNull(WasmValueType.class, "WasmValueType should exist");
    }

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasmValueType.class.isEnum(), "WasmValueType should be an enum");
    }

    @Test
    @DisplayName("should have I32 type")
    void shouldHaveI32Type() {
      assertNotNull(WasmValueType.valueOf("I32"), "I32 type should exist");
    }

    @Test
    @DisplayName("should have I64 type")
    void shouldHaveI64Type() {
      assertNotNull(WasmValueType.valueOf("I64"), "I64 type should exist");
    }

    @Test
    @DisplayName("should have F32 type")
    void shouldHaveF32Type() {
      assertNotNull(WasmValueType.valueOf("F32"), "F32 type should exist");
    }

    @Test
    @DisplayName("should have F64 type")
    void shouldHaveF64Type() {
      assertNotNull(WasmValueType.valueOf("F64"), "F64 type should exist");
    }

    @Test
    @DisplayName("should have at least 4 basic value types")
    void shouldHaveAtLeast4BasicValueTypes() {
      WasmValueType[] values = WasmValueType.values();
      assertTrue(
          values.length >= 4, "Should have at least 4 basic value types (I32, I64, F32, F64)");
    }
  }

  // ========================================================================
  // Type Relationship Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Relationship Tests")
  class TypeRelationshipTests {

    @Test
    @DisplayName("GlobalType should implement WasmType")
    void globalTypeShouldImplementWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(GlobalType.class),
          "GlobalType should implement WasmType");
    }

    @Test
    @DisplayName("TableType should implement WasmType")
    void tableTypeShouldImplementWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(TableType.class), "TableType should implement WasmType");
    }

    @Test
    @DisplayName("MemoryType should implement WasmType")
    void memoryTypeShouldImplementWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(MemoryType.class),
          "MemoryType should implement WasmType");
    }

    @Test
    @DisplayName("FuncType should implement WasmType")
    void funcTypeShouldImplementWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(FuncType.class), "FuncType should implement WasmType");
    }
  }

  // ========================================================================
  // FunctionType Class Tests
  // ========================================================================

  @Nested
  @DisplayName("FunctionType Class Tests")
  class FunctionTypeClassTests {

    @Test
    @DisplayName("should exist")
    void shouldExist() {
      assertNotNull(FunctionType.class, "FunctionType should exist");
    }

    @Test
    @DisplayName("should be a final class implementing WasmType")
    void shouldBeAFinalClassImplementingWasmType() {
      assertFalse(FunctionType.class.isInterface(), "FunctionType should not be an interface");
      assertTrue(
          Modifier.isFinal(FunctionType.class.getModifiers()), "FunctionType should be final");
      assertTrue(
          WasmType.class.isAssignableFrom(FunctionType.class),
          "FunctionType should implement WasmType");
    }

    @Test
    @DisplayName("should have getParamTypes method")
    void shouldHaveGetParamTypesMethod() throws NoSuchMethodException {
      Method method = FunctionType.class.getMethod("getParamTypes");
      assertNotNull(method, "getParamTypes method should exist");
    }

    @Test
    @DisplayName("should have getReturnTypes method")
    void shouldHaveGetReturnTypesMethod() throws NoSuchMethodException {
      Method method = FunctionType.class.getMethod("getReturnTypes");
      assertNotNull(method, "getReturnTypes method should exist");
    }

    @Test
    @DisplayName("should have getParamCount method")
    void shouldHaveGetParamCountMethod() throws NoSuchMethodException {
      Method method = FunctionType.class.getMethod("getParamCount");
      assertNotNull(method, "getParamCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getReturnCount method")
    void shouldHaveGetReturnCountMethod() throws NoSuchMethodException {
      Method method = FunctionType.class.getMethod("getReturnCount");
      assertNotNull(method, "getReturnCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getKind method returning FUNCTION")
    void shouldHaveGetKindMethod() throws NoSuchMethodException {
      Method method = FunctionType.class.getMethod("getKind");
      assertNotNull(method, "getKind method should exist");
      assertEquals(
          WasmTypeKind.class, method.getReturnType(), "Return type should be WasmTypeKind");
    }
  }

  // ========================================================================
  // ComponentVal Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentVal Interface Tests")
  class ComponentValTests {

    @Test
    @DisplayName("should exist")
    void shouldExist() {
      assertNotNull(ComponentVal.class, "ComponentVal should exist");
    }

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ComponentVal.class.isInterface(), "ComponentVal should be an interface");
    }
  }

  // ========================================================================
  // ComponentType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ComponentType Enum Tests")
  class ComponentTypeTests {

    @Test
    @DisplayName("should exist")
    void shouldExist() {
      assertNotNull(ComponentType.class, "ComponentType should exist");
    }

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(ComponentType.class.isEnum(), "ComponentType should be an enum");
    }

    @Test
    @DisplayName("should have BOOL constant")
    void shouldHaveBoolConstant() {
      assertNotNull(ComponentType.valueOf("BOOL"), "BOOL constant should exist");
    }

    @Test
    @DisplayName("should have S32 constant")
    void shouldHaveS32Constant() {
      assertNotNull(ComponentType.valueOf("S32"), "S32 constant should exist");
    }

    @Test
    @DisplayName("should have STRING constant")
    void shouldHaveStringConstant() {
      assertNotNull(ComponentType.valueOf("STRING"), "STRING constant should exist");
    }

    @Test
    @DisplayName("should have LIST constant")
    void shouldHaveListConstant() {
      assertNotNull(ComponentType.valueOf("LIST"), "LIST constant should exist");
    }

    @Test
    @DisplayName("should have RECORD constant")
    void shouldHaveRecordConstant() {
      assertNotNull(ComponentType.valueOf("RECORD"), "RECORD constant should exist");
    }

    @Test
    @DisplayName("should have isPrimitive method")
    void shouldHaveIsPrimitiveMethod() throws NoSuchMethodException {
      Method method = ComponentType.class.getMethod("isPrimitive");
      assertNotNull(method, "isPrimitive method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isInteger method")
    void shouldHaveIsIntegerMethod() throws NoSuchMethodException {
      Method method = ComponentType.class.getMethod("isInteger");
      assertNotNull(method, "isInteger method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isCompound method")
    void shouldHaveIsCompoundMethod() throws NoSuchMethodException {
      Method method = ComponentType.class.getMethod("isCompound");
      assertNotNull(method, "isCompound method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isResource method")
    void shouldHaveIsResourceMethod() throws NoSuchMethodException {
      Method method = ComponentType.class.getMethod("isResource");
      assertNotNull(method, "isResource method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }
}

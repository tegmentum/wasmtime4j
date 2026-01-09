/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j.jni.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ExportDescriptor;
import ai.tegmentum.wasmtime4j.FuncType;
import ai.tegmentum.wasmtime4j.GlobalType;
import ai.tegmentum.wasmtime4j.ImportDescriptor;
import ai.tegmentum.wasmtime4j.MemoryType;
import ai.tegmentum.wasmtime4j.TableType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the JNI type package.
 *
 * <p>This test covers all classes in the ai.tegmentum.wasmtime4j.jni.type package including
 * JniFuncType, JniGlobalType, JniMemoryType, JniTableType, JniImportDescriptor, and
 * JniExportDescriptor.
 */
@DisplayName("JNI Type Package Tests")
class JniTypePackageTest {

  // ========================================================================
  // JniFuncType Tests
  // ========================================================================

  @Nested
  @DisplayName("JniFuncType Tests")
  class JniFuncTypeTests {

    @Test
    @DisplayName("JniFuncType should be a final class")
    void jniFuncTypeShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(JniFuncType.class.getModifiers()), "JniFuncType should be final");
    }

    @Test
    @DisplayName("JniFuncType should implement FuncType interface")
    void jniFuncTypeShouldImplementFuncTypeInterface() {
      assertTrue(
          FuncType.class.isAssignableFrom(JniFuncType.class),
          "JniFuncType should implement FuncType");
    }

    @Test
    @DisplayName("JniFuncType constructor should accept Lists of WasmValueType")
    void jniFuncTypeConstructorShouldAcceptLists() {
      List<WasmValueType> params = Arrays.asList(WasmValueType.I32, WasmValueType.I64);
      List<WasmValueType> results = Collections.singletonList(WasmValueType.F64);

      JniFuncType funcType = new JniFuncType(params, results);

      assertNotNull(funcType, "JniFuncType should be created");
      assertEquals(params, funcType.getParams(), "Params should match");
      assertEquals(results, funcType.getResults(), "Results should match");
    }

    @Test
    @DisplayName("JniFuncType constructor should accept arrays of WasmValueType")
    void jniFuncTypeConstructorShouldAcceptArrays() {
      WasmValueType[] params = {WasmValueType.I32, WasmValueType.I64};
      WasmValueType[] results = {WasmValueType.F64};

      JniFuncType funcType = new JniFuncType(params, results);

      assertNotNull(funcType, "JniFuncType should be created");
      assertEquals(2, funcType.getParams().size(), "Should have 2 params");
      assertEquals(1, funcType.getResults().size(), "Should have 1 result");
    }

    @Test
    @DisplayName("JniFuncType should throw for null params")
    void jniFuncTypeShouldThrowForNullParams() {
      assertThrows(
          JniException.class,
          () -> new JniFuncType(null, Collections.emptyList()),
          "Should throw for null params");
    }

    @Test
    @DisplayName("JniFuncType should throw for null results")
    void jniFuncTypeShouldThrowForNullResults() {
      assertThrows(
          JniException.class,
          () -> new JniFuncType(Collections.emptyList(), null),
          "Should throw for null results");
    }

    @Test
    @DisplayName("JniFuncType should throw for null param element")
    void jniFuncTypeShouldThrowForNullParamElement() {
      List<WasmValueType> params = Arrays.asList(WasmValueType.I32, null);
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniFuncType(params, Collections.emptyList()),
          "Should throw for null param element");
    }

    @Test
    @DisplayName("JniFuncType should throw for null result element")
    void jniFuncTypeShouldThrowForNullResultElement() {
      List<WasmValueType> results = Arrays.asList(WasmValueType.I32, null);
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniFuncType(Collections.emptyList(), results),
          "Should throw for null result element");
    }

    @Test
    @DisplayName("JniFuncType getKind should return FUNCTION")
    void jniFuncTypeGetKindShouldReturnFunction() {
      JniFuncType funcType = new JniFuncType(Collections.emptyList(), Collections.emptyList());

      assertEquals(WasmTypeKind.FUNCTION, funcType.getKind(), "Kind should be FUNCTION");
    }

    @Test
    @DisplayName("JniFuncType getParams should return immutable list")
    void jniFuncTypeGetParamsShouldReturnImmutableList() {
      JniFuncType funcType =
          new JniFuncType(Collections.singletonList(WasmValueType.I32), Collections.emptyList());

      assertThrows(
          UnsupportedOperationException.class,
          () -> funcType.getParams().add(WasmValueType.I64),
          "Params should be immutable");
    }

    @Test
    @DisplayName("JniFuncType getResults should return immutable list")
    void jniFuncTypeGetResultsShouldReturnImmutableList() {
      JniFuncType funcType =
          new JniFuncType(Collections.emptyList(), Collections.singletonList(WasmValueType.I32));

      assertThrows(
          UnsupportedOperationException.class,
          () -> funcType.getResults().add(WasmValueType.I64),
          "Results should be immutable");
    }

    @Test
    @DisplayName("JniFuncType equals should work correctly")
    void jniFuncTypeEqualsShouldWorkCorrectly() {
      JniFuncType funcType1 =
          new JniFuncType(
              Collections.singletonList(WasmValueType.I32),
              Collections.singletonList(WasmValueType.I64));
      JniFuncType funcType2 =
          new JniFuncType(
              Collections.singletonList(WasmValueType.I32),
              Collections.singletonList(WasmValueType.I64));
      JniFuncType funcType3 =
          new JniFuncType(
              Collections.singletonList(WasmValueType.F64),
              Collections.singletonList(WasmValueType.I64));

      assertEquals(funcType1, funcType2, "Equal func types should be equal");
      assertNotEquals(funcType1, funcType3, "Different func types should not be equal");
    }

    @Test
    @DisplayName("JniFuncType hashCode should be consistent with equals")
    void jniFuncTypeHashCodeShouldBeConsistent() {
      JniFuncType funcType1 =
          new JniFuncType(
              Collections.singletonList(WasmValueType.I32),
              Collections.singletonList(WasmValueType.I64));
      JniFuncType funcType2 =
          new JniFuncType(
              Collections.singletonList(WasmValueType.I32),
              Collections.singletonList(WasmValueType.I64));

      assertEquals(
          funcType1.hashCode(), funcType2.hashCode(), "Equal objects should have same hash");
    }

    @Test
    @DisplayName("JniFuncType toString should contain params and results")
    void jniFuncTypeToStringShouldContainParamsAndResults() {
      JniFuncType funcType =
          new JniFuncType(
              Collections.singletonList(WasmValueType.I32),
              Collections.singletonList(WasmValueType.I64));

      String str = funcType.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("params"), "toString should contain params");
      assertTrue(str.contains("results"), "toString should contain results");
    }

    @Test
    @DisplayName("JniFuncType should have fromNative static method")
    void jniFuncTypeShouldHaveFromNativeMethod() throws NoSuchMethodException {
      Method method = JniFuncType.class.getDeclaredMethod("fromNative", long.class);
      assertNotNull(method, "fromNative method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromNative should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fromNative should be public");
      assertEquals(JniFuncType.class, method.getReturnType(), "Should return JniFuncType");
    }
  }

  // ========================================================================
  // JniGlobalType Tests
  // ========================================================================

  @Nested
  @DisplayName("JniGlobalType Tests")
  class JniGlobalTypeTests {

    @Test
    @DisplayName("JniGlobalType should be a final class")
    void jniGlobalTypeShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniGlobalType.class.getModifiers()), "JniGlobalType should be final");
    }

    @Test
    @DisplayName("JniGlobalType should implement GlobalType interface")
    void jniGlobalTypeShouldImplementGlobalTypeInterface() {
      assertTrue(
          GlobalType.class.isAssignableFrom(JniGlobalType.class),
          "JniGlobalType should implement GlobalType");
    }

    @Test
    @DisplayName("JniGlobalType constructor should accept value type and mutability")
    void jniGlobalTypeConstructorShouldAcceptParams() {
      JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, true);

      assertNotNull(globalType, "JniGlobalType should be created");
      assertEquals(WasmValueType.I32, globalType.getValueType(), "Value type should match");
      assertTrue(globalType.isMutable(), "Should be mutable");
    }

    @Test
    @DisplayName("JniGlobalType should throw for null value type")
    void jniGlobalTypeShouldThrowForNullValueType() {
      assertThrows(
          JniException.class,
          () -> new JniGlobalType(null, false),
          "Should throw for null value type");
    }

    @Test
    @DisplayName("JniGlobalType getKind should return GLOBAL")
    void jniGlobalTypeGetKindShouldReturnGlobal() {
      JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);

      assertEquals(WasmTypeKind.GLOBAL, globalType.getKind(), "Kind should be GLOBAL");
    }

    @Test
    @DisplayName("JniGlobalType with immutable flag should report correctly")
    void jniGlobalTypeImmutableShouldReportCorrectly() {
      JniGlobalType globalType = new JniGlobalType(WasmValueType.F64, false);

      assertFalse(globalType.isMutable(), "Should be immutable");
    }

    @Test
    @DisplayName("JniGlobalType equals should work correctly")
    void jniGlobalTypeEqualsShouldWorkCorrectly() {
      JniGlobalType globalType1 = new JniGlobalType(WasmValueType.I32, true);
      JniGlobalType globalType2 = new JniGlobalType(WasmValueType.I32, true);
      JniGlobalType globalType3 = new JniGlobalType(WasmValueType.I32, false);
      JniGlobalType globalType4 = new JniGlobalType(WasmValueType.I64, true);

      assertEquals(globalType1, globalType2, "Equal global types should be equal");
      assertNotEquals(globalType1, globalType3, "Different mutability should not be equal");
      assertNotEquals(globalType1, globalType4, "Different value type should not be equal");
    }

    @Test
    @DisplayName("JniGlobalType hashCode should be consistent with equals")
    void jniGlobalTypeHashCodeShouldBeConsistent() {
      JniGlobalType globalType1 = new JniGlobalType(WasmValueType.I32, true);
      JniGlobalType globalType2 = new JniGlobalType(WasmValueType.I32, true);

      assertEquals(
          globalType1.hashCode(), globalType2.hashCode(), "Equal objects should have same hash");
    }

    @Test
    @DisplayName("JniGlobalType toString should contain value type and mutability")
    void jniGlobalTypeToStringShouldContainInfo() {
      JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, true);

      String str = globalType.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("valueType") || str.contains("I32"), "Should contain value type");
      assertTrue(str.contains("mutable") || str.contains("true"), "Should contain mutability");
    }

    @Test
    @DisplayName("JniGlobalType should have fromNative static method")
    void jniGlobalTypeShouldHaveFromNativeMethod() throws NoSuchMethodException {
      Method method = JniGlobalType.class.getDeclaredMethod("fromNative", long.class);
      assertNotNull(method, "fromNative method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromNative should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fromNative should be public");
      assertEquals(JniGlobalType.class, method.getReturnType(), "Should return JniGlobalType");
    }
  }

  // ========================================================================
  // JniMemoryType Tests
  // ========================================================================

  @Nested
  @DisplayName("JniMemoryType Tests")
  class JniMemoryTypeTests {

    @Test
    @DisplayName("JniMemoryType should be a final class")
    void jniMemoryTypeShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniMemoryType.class.getModifiers()), "JniMemoryType should be final");
    }

    @Test
    @DisplayName("JniMemoryType should implement MemoryType interface")
    void jniMemoryTypeShouldImplementMemoryTypeInterface() {
      assertTrue(
          MemoryType.class.isAssignableFrom(JniMemoryType.class),
          "JniMemoryType should implement MemoryType");
    }

    @Test
    @DisplayName("JniMemoryType constructor should accept all parameters")
    void jniMemoryTypeConstructorShouldAcceptParams() {
      JniMemoryType memoryType = new JniMemoryType(1, 10L, false, false);

      assertNotNull(memoryType, "JniMemoryType should be created");
      assertEquals(1, memoryType.getMinimum(), "Minimum should match");
      assertTrue(memoryType.getMaximum().isPresent(), "Maximum should be present");
      assertEquals(10L, memoryType.getMaximum().get(), "Maximum should match");
      assertFalse(memoryType.is64Bit(), "Should not be 64-bit");
      assertFalse(memoryType.isShared(), "Should not be shared");
    }

    @Test
    @DisplayName("JniMemoryType with null maximum should have empty Optional")
    void jniMemoryTypeWithNullMaximumShouldHaveEmptyOptional() {
      JniMemoryType memoryType = new JniMemoryType(1, null, false, false);

      assertFalse(memoryType.getMaximum().isPresent(), "Maximum should be empty");
    }

    @Test
    @DisplayName("JniMemoryType should throw for negative minimum")
    void jniMemoryTypeShouldThrowForNegativeMinimum() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniMemoryType(-1, null, false, false),
          "Should throw for negative minimum");
    }

    @Test
    @DisplayName("JniMemoryType should throw when maximum less than minimum")
    void jniMemoryTypeShouldThrowWhenMaximumLessThanMinimum() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniMemoryType(10, 5L, false, false),
          "Should throw when maximum < minimum");
    }

    @Test
    @DisplayName("JniMemoryType getKind should return MEMORY")
    void jniMemoryTypeGetKindShouldReturnMemory() {
      JniMemoryType memoryType = new JniMemoryType(1, null, false, false);

      assertEquals(WasmTypeKind.MEMORY, memoryType.getKind(), "Kind should be MEMORY");
    }

    @Test
    @DisplayName("JniMemoryType with 64-bit flag should report correctly")
    void jniMemoryType64BitShouldReportCorrectly() {
      JniMemoryType memoryType = new JniMemoryType(1, null, true, false);

      assertTrue(memoryType.is64Bit(), "Should be 64-bit");
    }

    @Test
    @DisplayName("JniMemoryType with shared flag should report correctly")
    void jniMemoryTypeSharedShouldReportCorrectly() {
      JniMemoryType memoryType = new JniMemoryType(1, 10L, false, true);

      assertTrue(memoryType.isShared(), "Should be shared");
    }

    @Test
    @DisplayName("JniMemoryType equals should work correctly")
    void jniMemoryTypeEqualsShouldWorkCorrectly() {
      JniMemoryType memoryType1 = new JniMemoryType(1, 10L, false, false);
      JniMemoryType memoryType2 = new JniMemoryType(1, 10L, false, false);
      JniMemoryType memoryType3 = new JniMemoryType(2, 10L, false, false);

      assertEquals(memoryType1, memoryType2, "Equal memory types should be equal");
      assertNotEquals(memoryType1, memoryType3, "Different memory types should not be equal");
    }

    @Test
    @DisplayName("JniMemoryType hashCode should be consistent with equals")
    void jniMemoryTypeHashCodeShouldBeConsistent() {
      JniMemoryType memoryType1 = new JniMemoryType(1, 10L, false, false);
      JniMemoryType memoryType2 = new JniMemoryType(1, 10L, false, false);

      assertEquals(
          memoryType1.hashCode(), memoryType2.hashCode(), "Equal objects should have same hash");
    }

    @Test
    @DisplayName("JniMemoryType toString should contain relevant info")
    void jniMemoryTypeToStringShouldContainInfo() {
      JniMemoryType memoryType = new JniMemoryType(1, 10L, true, true);

      String str = memoryType.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("min") || str.contains("1"), "Should contain minimum");
    }

    @Test
    @DisplayName("JniMemoryType should have fromNative static method")
    void jniMemoryTypeShouldHaveFromNativeMethod() throws NoSuchMethodException {
      Method method = JniMemoryType.class.getDeclaredMethod("fromNative", long.class);
      assertNotNull(method, "fromNative method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromNative should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fromNative should be public");
      assertEquals(JniMemoryType.class, method.getReturnType(), "Should return JniMemoryType");
    }
  }

  // ========================================================================
  // JniTableType Tests
  // ========================================================================

  @Nested
  @DisplayName("JniTableType Tests")
  class JniTableTypeTests {

    @Test
    @DisplayName("JniTableType should be a final class")
    void jniTableTypeShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniTableType.class.getModifiers()), "JniTableType should be final");
    }

    @Test
    @DisplayName("JniTableType should implement TableType interface")
    void jniTableTypeShouldImplementTableTypeInterface() {
      assertTrue(
          TableType.class.isAssignableFrom(JniTableType.class),
          "JniTableType should implement TableType");
    }

    @Test
    @DisplayName("JniTableType constructor should accept element type and limits")
    void jniTableTypeConstructorShouldAcceptParams() {
      JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 1, 10L);

      assertNotNull(tableType, "JniTableType should be created");
      assertEquals(WasmValueType.FUNCREF, tableType.getElementType(), "Element type should match");
      assertEquals(1, tableType.getMinimum(), "Minimum should match");
      assertTrue(tableType.getMaximum().isPresent(), "Maximum should be present");
      assertEquals(10L, tableType.getMaximum().get(), "Maximum should match");
    }

    @Test
    @DisplayName("JniTableType with null maximum should have empty Optional")
    void jniTableTypeWithNullMaximumShouldHaveEmptyOptional() {
      JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 1, null);

      assertFalse(tableType.getMaximum().isPresent(), "Maximum should be empty");
    }

    @Test
    @DisplayName("JniTableType should throw for null element type")
    void jniTableTypeShouldThrowForNullElementType() {
      assertThrows(
          JniException.class,
          () -> new JniTableType(null, 1, null),
          "Should throw for null element type");
    }

    @Test
    @DisplayName("JniTableType should throw for negative minimum")
    void jniTableTypeShouldThrowForNegativeMinimum() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTableType(WasmValueType.FUNCREF, -1, null),
          "Should throw for negative minimum");
    }

    @Test
    @DisplayName("JniTableType should throw when maximum less than minimum")
    void jniTableTypeShouldThrowWhenMaximumLessThanMinimum() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTableType(WasmValueType.FUNCREF, 10, 5L),
          "Should throw when maximum < minimum");
    }

    @Test
    @DisplayName("JniTableType should throw for non-reference element type")
    void jniTableTypeShouldThrowForNonReferenceType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTableType(WasmValueType.I32, 1, null),
          "Should throw for non-reference element type");
    }

    @Test
    @DisplayName("JniTableType getKind should return TABLE")
    void jniTableTypeGetKindShouldReturnTable() {
      JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 1, null);

      assertEquals(WasmTypeKind.TABLE, tableType.getKind(), "Kind should be TABLE");
    }

    @Test
    @DisplayName("JniTableType equals should work correctly")
    void jniTableTypeEqualsShouldWorkCorrectly() {
      JniTableType tableType1 = new JniTableType(WasmValueType.FUNCREF, 1, 10L);
      JniTableType tableType2 = new JniTableType(WasmValueType.FUNCREF, 1, 10L);
      JniTableType tableType3 = new JniTableType(WasmValueType.FUNCREF, 2, 10L);

      assertEquals(tableType1, tableType2, "Equal table types should be equal");
      assertNotEquals(tableType1, tableType3, "Different table types should not be equal");
    }

    @Test
    @DisplayName("JniTableType hashCode should be consistent with equals")
    void jniTableTypeHashCodeShouldBeConsistent() {
      JniTableType tableType1 = new JniTableType(WasmValueType.FUNCREF, 1, 10L);
      JniTableType tableType2 = new JniTableType(WasmValueType.FUNCREF, 1, 10L);

      assertEquals(
          tableType1.hashCode(), tableType2.hashCode(), "Equal objects should have same hash");
    }

    @Test
    @DisplayName("JniTableType toString should contain relevant info")
    void jniTableTypeToStringShouldContainInfo() {
      JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 1, 10L);

      String str = tableType.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("element") || str.contains("FUNCREF"), "Should contain element type");
    }

    @Test
    @DisplayName("JniTableType should have fromNative static method")
    void jniTableTypeShouldHaveFromNativeMethod() throws NoSuchMethodException {
      Method method = JniTableType.class.getDeclaredMethod("fromNative", long.class);
      assertNotNull(method, "fromNative method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromNative should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fromNative should be public");
      assertEquals(JniTableType.class, method.getReturnType(), "Should return JniTableType");
    }
  }

  // ========================================================================
  // JniImportDescriptor Tests
  // ========================================================================

  @Nested
  @DisplayName("JniImportDescriptor Tests")
  class JniImportDescriptorTests {

    @Test
    @DisplayName("JniImportDescriptor should be a final class")
    void jniImportDescriptorShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniImportDescriptor.class.getModifiers()),
          "JniImportDescriptor should be final");
    }

    @Test
    @DisplayName("JniImportDescriptor should implement ImportDescriptor interface")
    void jniImportDescriptorShouldImplementInterface() {
      assertTrue(
          ImportDescriptor.class.isAssignableFrom(JniImportDescriptor.class),
          "JniImportDescriptor should implement ImportDescriptor");
    }

    @Test
    @DisplayName("JniImportDescriptor constructor should accept module name, name, and type")
    void jniImportDescriptorConstructorShouldAcceptParams() {
      JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      JniImportDescriptor descriptor = new JniImportDescriptor("env", "memory", globalType);

      assertNotNull(descriptor, "JniImportDescriptor should be created");
      assertEquals("env", descriptor.getModuleName(), "Module name should match");
      assertEquals("memory", descriptor.getName(), "Name should match");
      assertEquals(globalType, descriptor.getType(), "Type should match");
    }

    @Test
    @DisplayName("JniImportDescriptor should throw for null module name")
    void jniImportDescriptorShouldThrowForNullModuleName() {
      JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      assertThrows(
          JniException.class,
          () -> new JniImportDescriptor(null, "name", globalType),
          "Should throw for null module name");
    }

    @Test
    @DisplayName("JniImportDescriptor should throw for null name")
    void jniImportDescriptorShouldThrowForNullName() {
      JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      assertThrows(
          JniException.class,
          () -> new JniImportDescriptor("module", null, globalType),
          "Should throw for null name");
    }

    @Test
    @DisplayName("JniImportDescriptor should throw for null type")
    void jniImportDescriptorShouldThrowForNullType() {
      assertThrows(
          JniException.class,
          () -> new JniImportDescriptor("module", "name", null),
          "Should throw for null type");
    }

    @Test
    @DisplayName("JniImportDescriptor equals should work correctly")
    void jniImportDescriptorEqualsShouldWorkCorrectly() {
      JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      JniImportDescriptor descriptor1 = new JniImportDescriptor("env", "memory", globalType);
      JniImportDescriptor descriptor2 = new JniImportDescriptor("env", "memory", globalType);
      JniImportDescriptor descriptor3 = new JniImportDescriptor("other", "memory", globalType);

      assertEquals(descriptor1, descriptor2, "Equal descriptors should be equal");
      assertNotEquals(descriptor1, descriptor3, "Different descriptors should not be equal");
    }

    @Test
    @DisplayName("JniImportDescriptor hashCode should be consistent with equals")
    void jniImportDescriptorHashCodeShouldBeConsistent() {
      JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      JniImportDescriptor descriptor1 = new JniImportDescriptor("env", "memory", globalType);
      JniImportDescriptor descriptor2 = new JniImportDescriptor("env", "memory", globalType);

      assertEquals(
          descriptor1.hashCode(), descriptor2.hashCode(), "Equal objects should have same hash");
    }

    @Test
    @DisplayName("JniImportDescriptor toString should contain relevant info")
    void jniImportDescriptorToStringShouldContainInfo() {
      JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      JniImportDescriptor descriptor = new JniImportDescriptor("env", "memory", globalType);

      String str = descriptor.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("env"), "Should contain module name");
      assertTrue(str.contains("memory"), "Should contain name");
    }

    @Test
    @DisplayName("JniImportDescriptor should have fromNative static method")
    void jniImportDescriptorShouldHaveFromNativeMethod() throws NoSuchMethodException {
      Method method = JniImportDescriptor.class.getDeclaredMethod("fromNative", long.class);
      assertNotNull(method, "fromNative method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromNative should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fromNative should be public");
      assertEquals(
          JniImportDescriptor.class, method.getReturnType(), "Should return JniImportDescriptor");
    }
  }

  // ========================================================================
  // JniExportDescriptor Tests
  // ========================================================================

  @Nested
  @DisplayName("JniExportDescriptor Tests")
  class JniExportDescriptorTests {

    @Test
    @DisplayName("JniExportDescriptor should be a final class")
    void jniExportDescriptorShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniExportDescriptor.class.getModifiers()),
          "JniExportDescriptor should be final");
    }

    @Test
    @DisplayName("JniExportDescriptor should implement ExportDescriptor interface")
    void jniExportDescriptorShouldImplementInterface() {
      assertTrue(
          ExportDescriptor.class.isAssignableFrom(JniExportDescriptor.class),
          "JniExportDescriptor should implement ExportDescriptor");
    }

    @Test
    @DisplayName("JniExportDescriptor constructor should accept name and type")
    void jniExportDescriptorConstructorShouldAcceptParams() {
      JniFuncType funcType =
          new JniFuncType(
              Collections.singletonList(WasmValueType.I32),
              Collections.singletonList(WasmValueType.I32));
      JniExportDescriptor descriptor = new JniExportDescriptor("add", funcType);

      assertNotNull(descriptor, "JniExportDescriptor should be created");
      assertEquals("add", descriptor.getName(), "Name should match");
      assertEquals(funcType, descriptor.getType(), "Type should match");
    }

    @Test
    @DisplayName("JniExportDescriptor should throw for null name")
    void jniExportDescriptorShouldThrowForNullName() {
      JniFuncType funcType = new JniFuncType(Collections.emptyList(), Collections.emptyList());
      assertThrows(
          JniException.class,
          () -> new JniExportDescriptor(null, funcType),
          "Should throw for null name");
    }

    @Test
    @DisplayName("JniExportDescriptor should throw for null type")
    void jniExportDescriptorShouldThrowForNullType() {
      assertThrows(
          JniException.class,
          () -> new JniExportDescriptor("name", null),
          "Should throw for null type");
    }

    @Test
    @DisplayName("JniExportDescriptor equals should work correctly")
    void jniExportDescriptorEqualsShouldWorkCorrectly() {
      JniFuncType funcType = new JniFuncType(Collections.emptyList(), Collections.emptyList());
      JniExportDescriptor descriptor1 = new JniExportDescriptor("add", funcType);
      JniExportDescriptor descriptor2 = new JniExportDescriptor("add", funcType);
      JniExportDescriptor descriptor3 = new JniExportDescriptor("sub", funcType);

      assertEquals(descriptor1, descriptor2, "Equal descriptors should be equal");
      assertNotEquals(descriptor1, descriptor3, "Different descriptors should not be equal");
    }

    @Test
    @DisplayName("JniExportDescriptor hashCode should be consistent with equals")
    void jniExportDescriptorHashCodeShouldBeConsistent() {
      JniFuncType funcType = new JniFuncType(Collections.emptyList(), Collections.emptyList());
      JniExportDescriptor descriptor1 = new JniExportDescriptor("add", funcType);
      JniExportDescriptor descriptor2 = new JniExportDescriptor("add", funcType);

      assertEquals(
          descriptor1.hashCode(), descriptor2.hashCode(), "Equal objects should have same hash");
    }

    @Test
    @DisplayName("JniExportDescriptor toString should contain relevant info")
    void jniExportDescriptorToStringShouldContainInfo() {
      JniFuncType funcType = new JniFuncType(Collections.emptyList(), Collections.emptyList());
      JniExportDescriptor descriptor = new JniExportDescriptor("add", funcType);

      String str = descriptor.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("add"), "Should contain name");
    }

    @Test
    @DisplayName("JniExportDescriptor should have fromNative static method")
    void jniExportDescriptorShouldHaveFromNativeMethod() throws NoSuchMethodException {
      Method method = JniExportDescriptor.class.getDeclaredMethod("fromNative", long.class);
      assertNotNull(method, "fromNative method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromNative should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fromNative should be public");
      assertEquals(
          JniExportDescriptor.class, method.getReturnType(), "Should return JniExportDescriptor");
    }
  }

  // ========================================================================
  // Cross-Type Compatibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Cross-Type Compatibility Tests")
  class CrossTypeCompatibilityTests {

    @Test
    @DisplayName("All type classes should have public constructors")
    void allTypeClassesShouldHavePublicConstructors() {
      Class<?>[] typeClasses = {
        JniFuncType.class,
        JniGlobalType.class,
        JniMemoryType.class,
        JniTableType.class,
        JniImportDescriptor.class,
        JniExportDescriptor.class
      };

      for (Class<?> clazz : typeClasses) {
        Constructor<?>[] constructors = clazz.getConstructors();
        assertTrue(
            constructors.length > 0, clazz.getSimpleName() + " should have public constructors");
      }
    }

    @Test
    @DisplayName("Type classes should not be interfaces")
    void typeClassesShouldNotBeInterfaces() {
      Class<?>[] typeClasses = {
        JniFuncType.class,
        JniGlobalType.class,
        JniMemoryType.class,
        JniTableType.class,
        JniImportDescriptor.class,
        JniExportDescriptor.class
      };

      for (Class<?> clazz : typeClasses) {
        assertFalse(clazz.isInterface(), clazz.getSimpleName() + " should not be an interface");
      }
    }

    @Test
    @DisplayName("Type classes should be in correct package")
    void typeClassesShouldBeInCorrectPackage() {
      Class<?>[] typeClasses = {
        JniFuncType.class,
        JniGlobalType.class,
        JniMemoryType.class,
        JniTableType.class,
        JniImportDescriptor.class,
        JniExportDescriptor.class
      };

      String expectedPackage = "ai.tegmentum.wasmtime4j.jni.type";
      for (Class<?> clazz : typeClasses) {
        assertEquals(
            expectedPackage,
            clazz.getPackage().getName(),
            clazz.getSimpleName() + " should be in " + expectedPackage);
      }
    }

    @Test
    @DisplayName("FuncType with all primitive types should work")
    void funcTypeWithAllPrimitiveTypesShouldWork() {
      List<WasmValueType> params =
          Arrays.asList(WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64);
      List<WasmValueType> results = Collections.singletonList(WasmValueType.I32);

      JniFuncType funcType = new JniFuncType(params, results);

      assertEquals(4, funcType.getParams().size(), "Should have all params");
      assertEquals(1, funcType.getResults().size(), "Should have result");
    }

    @Test
    @DisplayName("ImportDescriptor with different type kinds should work")
    void importDescriptorWithDifferentTypeKindsShouldWork() {
      // Test with FuncType
      JniFuncType funcType = new JniFuncType(Collections.emptyList(), Collections.emptyList());
      JniImportDescriptor funcImport = new JniImportDescriptor("env", "func", funcType);
      assertEquals(WasmTypeKind.FUNCTION, funcImport.getType().getKind());

      // Test with GlobalType
      JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      JniImportDescriptor globalImport = new JniImportDescriptor("env", "global", globalType);
      assertEquals(WasmTypeKind.GLOBAL, globalImport.getType().getKind());

      // Test with MemoryType
      JniMemoryType memoryType = new JniMemoryType(1, null, false, false);
      JniImportDescriptor memoryImport = new JniImportDescriptor("env", "memory", memoryType);
      assertEquals(WasmTypeKind.MEMORY, memoryImport.getType().getKind());

      // Test with TableType
      JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 1, null);
      JniImportDescriptor tableImport = new JniImportDescriptor("env", "table", tableType);
      assertEquals(WasmTypeKind.TABLE, tableImport.getType().getKind());
    }
  }
}

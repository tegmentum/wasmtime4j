/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.gc.AnyRef;
import ai.tegmentum.wasmtime4j.gc.ArrayInstance;
import ai.tegmentum.wasmtime4j.gc.ArrayRef;
import ai.tegmentum.wasmtime4j.gc.ArrayType;
import ai.tegmentum.wasmtime4j.gc.EqRef;
import ai.tegmentum.wasmtime4j.gc.FieldDefinition;
import ai.tegmentum.wasmtime4j.gc.FieldType;
import ai.tegmentum.wasmtime4j.gc.GcCollectionResult;
import ai.tegmentum.wasmtime4j.gc.GcException;
import ai.tegmentum.wasmtime4j.gc.GcHeapInspection;
import ai.tegmentum.wasmtime4j.gc.GcHeapStats;
import ai.tegmentum.wasmtime4j.gc.GcInvariantValidation;
import ai.tegmentum.wasmtime4j.gc.GcObject;
import ai.tegmentum.wasmtime4j.gc.GcProfiler;
import ai.tegmentum.wasmtime4j.gc.GcRef;
import ai.tegmentum.wasmtime4j.gc.GcReferenceType;
import ai.tegmentum.wasmtime4j.gc.GcRootManager;
import ai.tegmentum.wasmtime4j.gc.GcRuntime;
import ai.tegmentum.wasmtime4j.gc.GcStats;
import ai.tegmentum.wasmtime4j.gc.GcValue;
import ai.tegmentum.wasmtime4j.gc.I31Instance;
import ai.tegmentum.wasmtime4j.gc.I31Type;
import ai.tegmentum.wasmtime4j.gc.MemoryCorruptionAnalysis;
import ai.tegmentum.wasmtime4j.gc.MemoryLeakAnalysis;
import ai.tegmentum.wasmtime4j.gc.ObjectLifecycleTracker;
import ai.tegmentum.wasmtime4j.gc.OwnedRooted;
import ai.tegmentum.wasmtime4j.gc.ReferenceGraph;
import ai.tegmentum.wasmtime4j.gc.ReferenceSafetyResult;
import ai.tegmentum.wasmtime4j.gc.RootScope;
import ai.tegmentum.wasmtime4j.gc.Rooted;
import ai.tegmentum.wasmtime4j.gc.StructInstance;
import ai.tegmentum.wasmtime4j.gc.StructRef;
import ai.tegmentum.wasmtime4j.gc.StructType;
import ai.tegmentum.wasmtime4j.gc.WeakGcReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WebAssembly GC package.
 *
 * <p>This test suite validates the API contracts for WebAssembly Garbage Collection types including
 * reference types, type definitions, and GC management interfaces.
 */
@DisplayName("GC Package Tests")
class GcPackageTest {

  // ========================================================================
  // GcRef Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("GcRef Interface Tests")
  class GcRefTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(GcRef.class.isInterface(), "GcRef should be an interface");
    }

    @Test
    @DisplayName("should have isNull method")
    void shouldHaveIsNullMethod() throws NoSuchMethodException {
      Method method = GcRef.class.getMethod("isNull");
      assertNotNull(method, "isNull method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getReferenceType method")
    void shouldHaveGetReferenceTypeMethod() throws NoSuchMethodException {
      Method method = GcRef.class.getMethod("getReferenceType");
      assertNotNull(method, "getReferenceType method should exist");
      assertEquals(
          GcReferenceType.class, method.getReturnType(), "Return type should be GcReferenceType");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = GcRef.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // GcReferenceType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("GcReferenceType Enum Tests")
  class GcReferenceTypeTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(GcReferenceType.class.isEnum(), "GcReferenceType should be an enum");
    }

    @Test
    @DisplayName("should have ANY_REF constant")
    void shouldHaveAnyRefConstant() {
      GcReferenceType anyRef = GcReferenceType.ANY_REF;
      assertNotNull(anyRef, "ANY_REF constant should exist");
      assertEquals("anyref", anyRef.getWasmName(), "ANY_REF should have correct wasm name");
    }

    @Test
    @DisplayName("should have EQ_REF constant")
    void shouldHaveEqRefConstant() {
      GcReferenceType eqRef = GcReferenceType.EQ_REF;
      assertNotNull(eqRef, "EQ_REF constant should exist");
      assertEquals("eqref", eqRef.getWasmName(), "EQ_REF should have correct wasm name");
    }

    @Test
    @DisplayName("should have I31_REF constant")
    void shouldHaveI31RefConstant() {
      GcReferenceType i31Ref = GcReferenceType.I31_REF;
      assertNotNull(i31Ref, "I31_REF constant should exist");
      assertEquals("i31ref", i31Ref.getWasmName(), "I31_REF should have correct wasm name");
    }

    @Test
    @DisplayName("should have STRUCT_REF constant")
    void shouldHaveStructRefConstant() {
      GcReferenceType structRef = GcReferenceType.STRUCT_REF;
      assertNotNull(structRef, "STRUCT_REF constant should exist");
      assertEquals(
          "structref", structRef.getWasmName(), "STRUCT_REF should have correct wasm name");
    }

    @Test
    @DisplayName("should have ARRAY_REF constant")
    void shouldHaveArrayRefConstant() {
      GcReferenceType arrayRef = GcReferenceType.ARRAY_REF;
      assertNotNull(arrayRef, "ARRAY_REF constant should exist");
      assertEquals("arrayref", arrayRef.getWasmName(), "ARRAY_REF should have correct wasm name");
    }

    @Test
    @DisplayName("should have getWasmName method")
    void shouldHaveGetWasmNameMethod() throws NoSuchMethodException {
      Method method = GcReferenceType.class.getMethod("getWasmName");
      assertNotNull(method, "getWasmName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have isSubtypeOf method")
    void shouldHaveIsSubtypeOfMethod() throws NoSuchMethodException {
      Method method = GcReferenceType.class.getMethod("isSubtypeOf", GcReferenceType.class);
      assertNotNull(method, "isSubtypeOf method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have supportsEquality method")
    void shouldHaveSupportsEqualityMethod() throws NoSuchMethodException {
      Method method = GcReferenceType.class.getMethod("supportsEquality");
      assertNotNull(method, "supportsEquality method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("I31_REF should be subtype of EQ_REF")
    void i31RefShouldBeSubtypeOfEqRef() {
      assertTrue(
          GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "I31_REF should be subtype of EQ_REF");
    }

    @Test
    @DisplayName("STRUCT_REF should be subtype of EQ_REF")
    void structRefShouldBeSubtypeOfEqRef() {
      assertTrue(
          GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "STRUCT_REF should be subtype of EQ_REF");
    }

    @Test
    @DisplayName("EQ_REF should be subtype of ANY_REF")
    void eqRefShouldBeSubtypeOfAnyRef() {
      assertTrue(
          GcReferenceType.EQ_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "EQ_REF should be subtype of ANY_REF");
    }
  }

  // ========================================================================
  // AnyRef Class Tests
  // ========================================================================

  @Nested
  @DisplayName("AnyRef Class Tests")
  class AnyRefTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(AnyRef.class.getModifiers()), "AnyRef should be final");
      assertFalse(AnyRef.class.isInterface(), "AnyRef should not be an interface");
    }

    @Test
    @DisplayName("should implement GcRef")
    void shouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(AnyRef.class), "AnyRef should implement GcRef");
    }

    @Test
    @DisplayName("should have static of factory method for GcObject")
    void shouldHaveOfFactoryMethodForGcObject() throws NoSuchMethodException {
      Method method = AnyRef.class.getMethod("of", GcObject.class);
      assertNotNull(method, "of(GcObject) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of method should be static");
      assertEquals(AnyRef.class, method.getReturnType(), "Return type should be AnyRef");
    }

    @Test
    @DisplayName("should have static nullRef factory method")
    void shouldHaveNullRefFactoryMethod() throws NoSuchMethodException {
      Method method = AnyRef.class.getMethod("nullRef");
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullRef method should be static");
      assertEquals(AnyRef.class, method.getReturnType(), "Return type should be AnyRef");
    }

    @Test
    @DisplayName("should have isI31 method")
    void shouldHaveIsI31Method() throws NoSuchMethodException {
      Method method = AnyRef.class.getMethod("isI31");
      assertNotNull(method, "isI31 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isStruct method")
    void shouldHaveIsStructMethod() throws NoSuchMethodException {
      Method method = AnyRef.class.getMethod("isStruct");
      assertNotNull(method, "isStruct method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isArray method")
    void shouldHaveIsArrayMethod() throws NoSuchMethodException {
      Method method = AnyRef.class.getMethod("isArray");
      assertNotNull(method, "isArray method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isEq method")
    void shouldHaveIsEqMethod() throws NoSuchMethodException {
      Method method = AnyRef.class.getMethod("isEq");
      assertNotNull(method, "isEq method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have asI31 method")
    void shouldHaveAsI31Method() throws NoSuchMethodException {
      Method method = AnyRef.class.getMethod("asI31", Store.class);
      assertNotNull(method, "asI31 method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have asStruct method")
    void shouldHaveAsStructMethod() throws NoSuchMethodException {
      Method method = AnyRef.class.getMethod("asStruct", Store.class);
      assertNotNull(method, "asStruct method should exist");
      assertEquals(StructRef.class, method.getReturnType(), "Return type should be StructRef");
    }

    @Test
    @DisplayName("should have asArray method")
    void shouldHaveAsArrayMethod() throws NoSuchMethodException {
      Method method = AnyRef.class.getMethod("asArray", Store.class);
      assertNotNull(method, "asArray method should exist");
      assertEquals(ArrayRef.class, method.getReturnType(), "Return type should be ArrayRef");
    }

    @Test
    @DisplayName("should have asEq method")
    void shouldHaveAsEqMethod() throws NoSuchMethodException {
      Method method = AnyRef.class.getMethod("asEq");
      assertNotNull(method, "asEq method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have toWasmValue method")
    void shouldHaveToWasmValueMethod() throws NoSuchMethodException {
      Method method = AnyRef.class.getMethod("toWasmValue");
      assertNotNull(method, "toWasmValue method should exist");
      assertEquals(WasmValue.class, method.getReturnType(), "Return type should be WasmValue");
    }

    @Test
    @DisplayName("should have refEquals method")
    void shouldHaveRefEqualsMethod() throws NoSuchMethodException {
      Method method = AnyRef.class.getMethod("refEquals", AnyRef.class);
      assertNotNull(method, "refEquals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // EqRef Class Tests
  // ========================================================================

  @Nested
  @DisplayName("EqRef Class Tests")
  class EqRefTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(EqRef.class.getModifiers()), "EqRef should be final");
      assertFalse(EqRef.class.isInterface(), "EqRef should not be an interface");
    }

    @Test
    @DisplayName("should implement GcRef")
    void shouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(EqRef.class), "EqRef should implement GcRef");
    }

    @Test
    @DisplayName("should have static of factory methods")
    void shouldHaveOfFactoryMethods() throws NoSuchMethodException {
      Method gcObjectMethod = EqRef.class.getMethod("of", GcObject.class);
      assertNotNull(gcObjectMethod, "of(GcObject) method should exist");
      assertTrue(Modifier.isStatic(gcObjectMethod.getModifiers()), "of method should be static");

      Method structRefMethod = EqRef.class.getMethod("of", StructRef.class);
      assertNotNull(structRefMethod, "of(StructRef) method should exist");

      Method arrayRefMethod = EqRef.class.getMethod("of", ArrayRef.class);
      assertNotNull(arrayRefMethod, "of(ArrayRef) method should exist");
    }

    @Test
    @DisplayName("should have static nullRef factory method")
    void shouldHaveNullRefFactoryMethod() throws NoSuchMethodException {
      Method method = EqRef.class.getMethod("nullRef");
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullRef method should be static");
      assertEquals(EqRef.class, method.getReturnType(), "Return type should be EqRef");
    }

    @Test
    @DisplayName("should have toAnyRef method")
    void shouldHaveToAnyRefMethod() throws NoSuchMethodException {
      Method method = EqRef.class.getMethod("toAnyRef");
      assertNotNull(method, "toAnyRef method should exist");
      assertEquals(AnyRef.class, method.getReturnType(), "Return type should be AnyRef");
    }

    @Test
    @DisplayName("should have refEquals method")
    void shouldHaveRefEqualsMethod() throws NoSuchMethodException {
      Method method = EqRef.class.getMethod("refEquals", EqRef.class);
      assertNotNull(method, "refEquals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // StructRef Class Tests
  // ========================================================================

  @Nested
  @DisplayName("StructRef Class Tests")
  class StructRefTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(StructRef.class.getModifiers()), "StructRef should be final");
    }

    @Test
    @DisplayName("should implement GcRef")
    void shouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(StructRef.class), "StructRef should implement GcRef");
    }

    @Test
    @DisplayName("should have static of factory method")
    void shouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = StructRef.class.getMethod("of", StructInstance.class);
      assertNotNull(method, "of(StructInstance) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of method should be static");
      assertEquals(StructRef.class, method.getReturnType(), "Return type should be StructRef");
    }

    @Test
    @DisplayName("should have static nullRef factory method")
    void shouldHaveNullRefFactoryMethod() throws NoSuchMethodException {
      Method method = StructRef.class.getMethod("nullRef");
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullRef method should be static");
    }

    @Test
    @DisplayName("should have getInstance method")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      Method method = StructRef.class.getMethod("getInstance");
      assertNotNull(method, "getInstance method should exist");
      assertEquals(
          StructInstance.class, method.getReturnType(), "Return type should be StructInstance");
    }

    @Test
    @DisplayName("should have getStructType method")
    void shouldHaveGetStructTypeMethod() throws NoSuchMethodException {
      Method method = StructRef.class.getMethod("getStructType", Store.class);
      assertNotNull(method, "getStructType method should exist");
      assertEquals(StructType.class, method.getReturnType(), "Return type should be StructType");
    }

    @Test
    @DisplayName("should have getFieldCount method")
    void shouldHaveGetFieldCountMethod() throws NoSuchMethodException {
      Method method = StructRef.class.getMethod("getFieldCount", Store.class);
      assertNotNull(method, "getFieldCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getField method")
    void shouldHaveGetFieldMethod() throws NoSuchMethodException {
      Method method = StructRef.class.getMethod("getField", Store.class, int.class);
      assertNotNull(method, "getField method should exist");
      assertEquals(GcValue.class, method.getReturnType(), "Return type should be GcValue");
    }

    @Test
    @DisplayName("should have setField method")
    void shouldHaveSetFieldMethod() throws NoSuchMethodException {
      Method method = StructRef.class.getMethod("setField", Store.class, int.class, GcValue.class);
      assertNotNull(method, "setField method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have toEqRef method")
    void shouldHaveToEqRefMethod() throws NoSuchMethodException {
      Method method = StructRef.class.getMethod("toEqRef");
      assertNotNull(method, "toEqRef method should exist");
      assertEquals(EqRef.class, method.getReturnType(), "Return type should be EqRef");
    }

    @Test
    @DisplayName("should have toAnyRef method")
    void shouldHaveToAnyRefMethod() throws NoSuchMethodException {
      Method method = StructRef.class.getMethod("toAnyRef");
      assertNotNull(method, "toAnyRef method should exist");
      assertEquals(AnyRef.class, method.getReturnType(), "Return type should be AnyRef");
    }
  }

  // ========================================================================
  // ArrayRef Class Tests
  // ========================================================================

  @Nested
  @DisplayName("ArrayRef Class Tests")
  class ArrayRefTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(ArrayRef.class.getModifiers()), "ArrayRef should be final");
    }

    @Test
    @DisplayName("should implement GcRef")
    void shouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(ArrayRef.class), "ArrayRef should implement GcRef");
    }

    @Test
    @DisplayName("should have static of factory method")
    void shouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = ArrayRef.class.getMethod("of", ArrayInstance.class);
      assertNotNull(method, "of(ArrayInstance) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of method should be static");
      assertEquals(ArrayRef.class, method.getReturnType(), "Return type should be ArrayRef");
    }

    @Test
    @DisplayName("should have static nullRef factory method")
    void shouldHaveNullRefFactoryMethod() throws NoSuchMethodException {
      Method method = ArrayRef.class.getMethod("nullRef");
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullRef method should be static");
    }

    @Test
    @DisplayName("should have getInstance method")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      Method method = ArrayRef.class.getMethod("getInstance");
      assertNotNull(method, "getInstance method should exist");
      assertEquals(
          ArrayInstance.class, method.getReturnType(), "Return type should be ArrayInstance");
    }

    @Test
    @DisplayName("should have getArrayType method")
    void shouldHaveGetArrayTypeMethod() throws NoSuchMethodException {
      Method method = ArrayRef.class.getMethod("getArrayType", Store.class);
      assertNotNull(method, "getArrayType method should exist");
      assertEquals(ArrayType.class, method.getReturnType(), "Return type should be ArrayType");
    }

    @Test
    @DisplayName("should have getLength method")
    void shouldHaveGetLengthMethod() throws NoSuchMethodException {
      Method method = ArrayRef.class.getMethod("getLength", Store.class);
      assertNotNull(method, "getLength method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getElement method")
    void shouldHaveGetElementMethod() throws NoSuchMethodException {
      Method method = ArrayRef.class.getMethod("getElement", Store.class, int.class);
      assertNotNull(method, "getElement method should exist");
      assertEquals(GcValue.class, method.getReturnType(), "Return type should be GcValue");
    }

    @Test
    @DisplayName("should have setElement method")
    void shouldHaveSetElementMethod() throws NoSuchMethodException {
      Method method = ArrayRef.class.getMethod("setElement", Store.class, int.class, GcValue.class);
      assertNotNull(method, "setElement method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have toEqRef method")
    void shouldHaveToEqRefMethod() throws NoSuchMethodException {
      Method method = ArrayRef.class.getMethod("toEqRef");
      assertNotNull(method, "toEqRef method should exist");
      assertEquals(EqRef.class, method.getReturnType(), "Return type should be EqRef");
    }

    @Test
    @DisplayName("should have toAnyRef method")
    void shouldHaveToAnyRefMethod() throws NoSuchMethodException {
      Method method = ArrayRef.class.getMethod("toAnyRef");
      assertNotNull(method, "toAnyRef method should exist");
      assertEquals(AnyRef.class, method.getReturnType(), "Return type should be AnyRef");
    }
  }

  // ========================================================================
  // StructType Class Tests
  // ========================================================================

  @Nested
  @DisplayName("StructType Class Tests")
  class StructTypeTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(StructType.class.getModifiers()), "StructType should be final");
    }

    @Test
    @DisplayName("should have static builder factory method")
    void shouldHaveBuilderFactoryMethod() throws NoSuchMethodException {
      Method method = StructType.class.getMethod("builder", String.class);
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder method should be static");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = StructType.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getFields method")
    void shouldHaveGetFieldsMethod() throws NoSuchMethodException {
      Method method = StructType.class.getMethod("getFields");
      assertNotNull(method, "getFields method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getSupertype method")
    void shouldHaveGetSupertypeMethod() throws NoSuchMethodException {
      Method method = StructType.class.getMethod("getSupertype");
      assertNotNull(method, "getSupertype method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have getTypeId method")
    void shouldHaveGetTypeIdMethod() throws NoSuchMethodException {
      Method method = StructType.class.getMethod("getTypeId");
      assertNotNull(method, "getTypeId method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getFieldCount method")
    void shouldHaveGetFieldCountMethod() throws NoSuchMethodException {
      Method method = StructType.class.getMethod("getFieldCount");
      assertNotNull(method, "getFieldCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getField method by index")
    void shouldHaveGetFieldByIndexMethod() throws NoSuchMethodException {
      Method method = StructType.class.getMethod("getField", int.class);
      assertNotNull(method, "getField(int) method should exist");
      assertEquals(
          FieldDefinition.class, method.getReturnType(), "Return type should be FieldDefinition");
    }

    @Test
    @DisplayName("should have getField method by name")
    void shouldHaveGetFieldByNameMethod() throws NoSuchMethodException {
      Method method = StructType.class.getMethod("getField", String.class);
      assertNotNull(method, "getField(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have isSubtypeOf method")
    void shouldHaveIsSubtypeOfMethod() throws NoSuchMethodException {
      Method method = StructType.class.getMethod("isSubtypeOf", StructType.class);
      assertNotNull(method, "isSubtypeOf method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      Method method = StructType.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getSizeBytes method")
    void shouldHaveGetSizeBytesMethod() throws NoSuchMethodException {
      Method method = StructType.class.getMethod("getSizeBytes");
      assertNotNull(method, "getSizeBytes method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have nested Builder class")
    void shouldHaveNestedBuilderClass() {
      Class<?>[] nestedClasses = StructType.class.getDeclaredClasses();
      boolean hasBuilder =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("Builder"));
      assertTrue(hasBuilder, "StructType should have nested Builder class");
    }
  }

  // ========================================================================
  // ArrayType Class Tests
  // ========================================================================

  @Nested
  @DisplayName("ArrayType Class Tests")
  class ArrayTypeTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(ArrayType.class.getModifiers()), "ArrayType should be final");
    }

    @Test
    @DisplayName("should have static builder factory method")
    void shouldHaveBuilderFactoryMethod() throws NoSuchMethodException {
      Method method = ArrayType.class.getMethod("builder", String.class);
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder method should be static");
    }

    @Test
    @DisplayName("should have static of factory method")
    void shouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = ArrayType.class.getMethod("of", String.class, FieldType.class);
      assertNotNull(method, "of method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of method should be static");
      assertEquals(ArrayType.class, method.getReturnType(), "Return type should be ArrayType");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = ArrayType.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getElementType method")
    void shouldHaveGetElementTypeMethod() throws NoSuchMethodException {
      Method method = ArrayType.class.getMethod("getElementType");
      assertNotNull(method, "getElementType method should exist");
      assertEquals(FieldType.class, method.getReturnType(), "Return type should be FieldType");
    }

    @Test
    @DisplayName("should have isMutable method")
    void shouldHaveIsMutableMethod() throws NoSuchMethodException {
      Method method = ArrayType.class.getMethod("isMutable");
      assertNotNull(method, "isMutable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getTypeId method")
    void shouldHaveGetTypeIdMethod() throws NoSuchMethodException {
      Method method = ArrayType.class.getMethod("getTypeId");
      assertNotNull(method, "getTypeId method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getElementSizeBytes method")
    void shouldHaveGetElementSizeBytesMethod() throws NoSuchMethodException {
      Method method = ArrayType.class.getMethod("getElementSizeBytes");
      assertNotNull(method, "getElementSizeBytes method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getArraySizeBytes method")
    void shouldHaveGetArraySizeBytesMethod() throws NoSuchMethodException {
      Method method = ArrayType.class.getMethod("getArraySizeBytes", int.class);
      assertNotNull(method, "getArraySizeBytes method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have isSubtypeOf method")
    void shouldHaveIsSubtypeOfMethod() throws NoSuchMethodException {
      Method method = ArrayType.class.getMethod("isSubtypeOf", ArrayType.class);
      assertNotNull(method, "isSubtypeOf method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      Method method = ArrayType.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have nested Builder class")
    void shouldHaveNestedBuilderClass() {
      Class<?>[] nestedClasses = ArrayType.class.getDeclaredClasses();
      boolean hasBuilder =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("Builder"));
      assertTrue(hasBuilder, "ArrayType should have nested Builder class");
    }
  }

  // ========================================================================
  // I31Type Class Tests
  // ========================================================================

  @Nested
  @DisplayName("I31Type Class Tests")
  class I31TypeTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(I31Type.class.getModifiers()), "I31Type should be final");
    }

    @Test
    @DisplayName("should have MIN_VALUE constant")
    void shouldHaveMinValueConstant() throws NoSuchFieldException {
      var field = I31Type.class.getField("MIN_VALUE");
      assertNotNull(field, "MIN_VALUE field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "MIN_VALUE should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "MIN_VALUE should be final");
    }

    @Test
    @DisplayName("should have MAX_VALUE constant")
    void shouldHaveMaxValueConstant() throws NoSuchFieldException {
      var field = I31Type.class.getField("MAX_VALUE");
      assertNotNull(field, "MAX_VALUE field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "MAX_VALUE should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "MAX_VALUE should be final");
    }

    @Test
    @DisplayName("should have BIT_WIDTH constant")
    void shouldHaveBitWidthConstant() throws NoSuchFieldException {
      var field = I31Type.class.getField("BIT_WIDTH");
      assertNotNull(field, "BIT_WIDTH field should exist");
      assertEquals(31, I31Type.BIT_WIDTH, "BIT_WIDTH should be 31");
    }

    @Test
    @DisplayName("should have static isValidValue method for int")
    void shouldHaveIsValidValueMethodForInt() throws NoSuchMethodException {
      Method method = I31Type.class.getMethod("isValidValue", int.class);
      assertNotNull(method, "isValidValue(int) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isValidValue should be static");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have static isValidValue method for long")
    void shouldHaveIsValidValueMethodForLong() throws NoSuchMethodException {
      Method method = I31Type.class.getMethod("isValidValue", long.class);
      assertNotNull(method, "isValidValue(long) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isValidValue should be static");
    }

    @Test
    @DisplayName("should have static validateValue method")
    void shouldHaveValidateValueMethod() throws NoSuchMethodException {
      Method method = I31Type.class.getMethod("validateValue", int.class);
      assertNotNull(method, "validateValue method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "validateValue should be static");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have static clampValue method")
    void shouldHaveClampValueMethod() throws NoSuchMethodException {
      Method method = I31Type.class.getMethod("clampValue", int.class);
      assertNotNull(method, "clampValue method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "clampValue should be static");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have static getMinValue method")
    void shouldHaveGetMinValueMethod() throws NoSuchMethodException {
      Method method = I31Type.class.getMethod("getMinValue");
      assertNotNull(method, "getMinValue method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have static getMaxValue method")
    void shouldHaveGetMaxValueMethod() throws NoSuchMethodException {
      Method method = I31Type.class.getMethod("getMaxValue");
      assertNotNull(method, "getMaxValue method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have static getRange method")
    void shouldHaveGetRangeMethod() throws NoSuchMethodException {
      Method method = I31Type.class.getMethod("getRange");
      assertNotNull(method, "getRange method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have nested I31Value class")
    void shouldHaveNestedI31ValueClass() {
      Class<?>[] nestedClasses = I31Type.class.getDeclaredClasses();
      boolean hasI31Value =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("I31Value"));
      assertTrue(hasI31Value, "I31Type should have nested I31Value class");
    }
  }

  // ========================================================================
  // GcObject Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("GcObject Interface Tests")
  class GcObjectTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(GcObject.class.isInterface(), "GcObject should be an interface");
    }

    @Test
    @DisplayName("should have getReferenceType method")
    void shouldHaveGetReferenceTypeMethod() throws NoSuchMethodException {
      Method method = GcObject.class.getMethod("getReferenceType");
      assertNotNull(method, "getReferenceType method should exist");
      assertEquals(
          GcReferenceType.class, method.getReturnType(), "Return type should be GcReferenceType");
    }
  }

  // ========================================================================
  // GcValue Abstract Class Tests
  // ========================================================================

  @Nested
  @DisplayName("GcValue Abstract Class Tests")
  class GcValueTests {

    @Test
    @DisplayName("should be an abstract class")
    void shouldBeAnAbstractClass() {
      assertTrue(
          Modifier.isAbstract(GcValue.class.getModifiers()), "GcValue should be an abstract class");
      assertFalse(GcValue.class.isInterface(), "GcValue should not be an interface");
    }

    @Test
    @DisplayName("should have Type enum")
    void shouldHaveTypeEnum() {
      Class<?>[] declaredClasses = GcValue.class.getDeclaredClasses();
      boolean hasTypeEnum =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.isEnum() && c.getSimpleName().equals("Type"));
      assertTrue(hasTypeEnum, "GcValue should have a Type enum");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      Method method = GcValue.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "getType should be abstract");
    }

    @Test
    @DisplayName("should have isNull method")
    void shouldHaveIsNullMethod() throws NoSuchMethodException {
      Method method = GcValue.class.getMethod("isNull");
      assertNotNull(method, "isNull method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // GcException Class Tests
  // ========================================================================

  @Nested
  @DisplayName("GcException Class Tests")
  class GcExceptionTests {

    @Test
    @DisplayName("should be a class extending Exception")
    void shouldExtendException() {
      assertTrue(
          Exception.class.isAssignableFrom(GcException.class),
          "GcException should extend Exception");
    }
  }

  // ========================================================================
  // FieldType Class Tests
  // ========================================================================

  @Nested
  @DisplayName("FieldType Class Tests")
  class FieldTypeTests {

    @Test
    @DisplayName("should exist as a class")
    void shouldExistAsClass() {
      assertNotNull(FieldType.class, "FieldType class should exist");
    }
  }

  // ========================================================================
  // FieldDefinition Class Tests
  // ========================================================================

  @Nested
  @DisplayName("FieldDefinition Class Tests")
  class FieldDefinitionTests {

    @Test
    @DisplayName("should exist as a class")
    void shouldExistAsClass() {
      assertNotNull(FieldDefinition.class, "FieldDefinition class should exist");
    }
  }

  // ========================================================================
  // GcRuntime Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("GcRuntime Interface Tests")
  class GcRuntimeTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(GcRuntime.class.isInterface(), "GcRuntime should be an interface");
    }
  }

  // ========================================================================
  // GcRootManager Final Class Tests
  // ========================================================================

  @Nested
  @DisplayName("GcRootManager Final Class Tests")
  class GcRootManagerTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(GcRootManager.class.getModifiers()),
          "GcRootManager should be a final class");
      assertFalse(GcRootManager.class.isInterface(), "GcRootManager should not be an interface");
    }

    @Test
    @DisplayName("should have getInstance method for singleton pattern")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      Method method = GcRootManager.class.getMethod("getInstance");
      assertNotNull(method, "getInstance method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getInstance should be static");
      assertEquals(
          GcRootManager.class, method.getReturnType(), "Return type should be GcRootManager");
    }
  }

  // ========================================================================
  // GcStats Final Class Tests
  // ========================================================================

  @Nested
  @DisplayName("GcStats Final Class Tests")
  class GcStatsTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(GcStats.class.getModifiers()), "GcStats should be a final class");
      assertFalse(GcStats.class.isInterface(), "GcStats should not be an interface");
    }

    @Test
    @DisplayName("should have builder method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      Method method = GcStats.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }

    @Test
    @DisplayName("should have getTotalAllocated method")
    void shouldHaveGetTotalAllocatedMethod() throws NoSuchMethodException {
      Method method = GcStats.class.getMethod("getTotalAllocated");
      assertNotNull(method, "getTotalAllocated method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // GcHeapStats Final Class Tests
  // ========================================================================

  @Nested
  @DisplayName("GcHeapStats Final Class Tests")
  class GcHeapStatsTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(GcHeapStats.class.getModifiers()),
          "GcHeapStats should be a final class");
      assertFalse(GcHeapStats.class.isInterface(), "GcHeapStats should not be an interface");
    }

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = GcHeapStats.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
    }

    @Test
    @DisplayName("should have getTotalAllocated method")
    void shouldHaveGetTotalAllocatedMethod() throws NoSuchMethodException {
      Method method = GcHeapStats.class.getMethod("getTotalAllocated");
      assertNotNull(method, "getTotalAllocated method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getCurrentHeapSize method")
    void shouldHaveGetCurrentHeapSizeMethod() throws NoSuchMethodException {
      Method method = GcHeapStats.class.getMethod("getCurrentHeapSize");
      assertNotNull(method, "getCurrentHeapSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // GcCollectionResult Final Class Tests
  // ========================================================================

  @Nested
  @DisplayName("GcCollectionResult Final Class Tests")
  class GcCollectionResultTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(GcCollectionResult.class.getModifiers()),
          "GcCollectionResult should be a final class");
      assertFalse(
          GcCollectionResult.class.isInterface(), "GcCollectionResult should not be an interface");
    }

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = GcCollectionResult.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
    }

    @Test
    @DisplayName("should have getObjectsCollected method")
    void shouldHaveGetObjectsCollectedMethod() throws NoSuchMethodException {
      Method method = GcCollectionResult.class.getMethod("getObjectsCollected");
      assertNotNull(method, "getObjectsCollected method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getBytesCollected method")
    void shouldHaveGetBytesCollectedMethod() throws NoSuchMethodException {
      Method method = GcCollectionResult.class.getMethod("getBytesCollected");
      assertNotNull(method, "getBytesCollected method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // Rooted Final Class Tests
  // ========================================================================

  @Nested
  @DisplayName("Rooted Final Class Tests")
  class RootedTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(Rooted.class.getModifiers()), "Rooted should be a final class");
      assertFalse(Rooted.class.isInterface(), "Rooted should not be an interface");
    }

    @Test
    @DisplayName("should have generic type parameter T")
    void shouldHaveGenericTypeParameter() {
      TypeVariable<?>[] typeParams = Rooted.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Rooted should have exactly one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("should have get method with Store parameter")
    void shouldHaveGetMethod() throws NoSuchMethodException {
      Method method = Rooted.class.getMethod("get", Store.class);
      assertNotNull(method, "get method should exist");
    }

    @Test
    @DisplayName("should have unroot method with Store parameter")
    void shouldHaveUnrootMethod() throws NoSuchMethodException {
      Method method = Rooted.class.getMethod("unroot", Store.class);
      assertNotNull(method, "unroot method should exist");
    }
  }

  // ========================================================================
  // OwnedRooted Final Class Tests
  // ========================================================================

  @Nested
  @DisplayName("OwnedRooted Final Class Tests")
  class OwnedRootedTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(OwnedRooted.class.getModifiers()),
          "OwnedRooted should be a final class");
      assertFalse(OwnedRooted.class.isInterface(), "OwnedRooted should not be an interface");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(OwnedRooted.class),
          "OwnedRooted should implement AutoCloseable");
    }

    @Test
    @DisplayName("should have generic type parameter T")
    void shouldHaveGenericTypeParameter() {
      TypeVariable<?>[] typeParams = OwnedRooted.class.getTypeParameters();
      assertEquals(1, typeParams.length, "OwnedRooted should have exactly one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("should have close method from AutoCloseable")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = OwnedRooted.class.getMethod("close");
      assertNotNull(method, "close method should exist");
    }
  }

  // ========================================================================
  // RootScope Final Class Tests
  // ========================================================================

  @Nested
  @DisplayName("RootScope Final Class Tests")
  class RootScopeTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(RootScope.class.getModifiers()), "RootScope should be a final class");
      assertFalse(RootScope.class.isInterface(), "RootScope should not be an interface");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(RootScope.class),
          "RootScope should implement AutoCloseable");
    }

    @Test
    @DisplayName("should have create factory method")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      Method method = RootScope.class.getMethod("create", Store.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(RootScope.class, method.getReturnType(), "Return type should be RootScope");
    }

    @Test
    @DisplayName("should have close method from AutoCloseable")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = RootScope.class.getMethod("close");
      assertNotNull(method, "close method should exist");
    }
  }

  // ========================================================================
  // WeakGcReference Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WeakGcReference Interface Tests")
  class WeakGcReferenceTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WeakGcReference.class.isInterface(), "WeakGcReference should be an interface");
    }
  }

  // ========================================================================
  // GcProfiler Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("GcProfiler Interface Tests")
  class GcProfilerTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(GcProfiler.class.isInterface(), "GcProfiler should be an interface");
    }
  }

  // ========================================================================
  // GcHeapInspection Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("GcHeapInspection Interface Tests")
  class GcHeapInspectionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(GcHeapInspection.class.isInterface(), "GcHeapInspection should be an interface");
    }
  }

  // ========================================================================
  // ReferenceGraph Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ReferenceGraph Interface Tests")
  class ReferenceGraphTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ReferenceGraph.class.isInterface(), "ReferenceGraph should be an interface");
    }
  }

  // ========================================================================
  // ObjectLifecycleTracker Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ObjectLifecycleTracker Interface Tests")
  class ObjectLifecycleTrackerTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ObjectLifecycleTracker.class.isInterface(),
          "ObjectLifecycleTracker should be an interface");
    }
  }

  // ========================================================================
  // MemoryLeakAnalysis Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("MemoryLeakAnalysis Interface Tests")
  class MemoryLeakAnalysisTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          MemoryLeakAnalysis.class.isInterface(), "MemoryLeakAnalysis should be an interface");
    }
  }

  // ========================================================================
  // MemoryCorruptionAnalysis Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("MemoryCorruptionAnalysis Interface Tests")
  class MemoryCorruptionAnalysisTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          MemoryCorruptionAnalysis.class.isInterface(),
          "MemoryCorruptionAnalysis should be an interface");
    }
  }

  // ========================================================================
  // ReferenceSafetyResult Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ReferenceSafetyResult Interface Tests")
  class ReferenceSafetyResultTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ReferenceSafetyResult.class.isInterface(),
          "ReferenceSafetyResult should be an interface");
    }
  }

  // ========================================================================
  // GcInvariantValidation Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("GcInvariantValidation Interface Tests")
  class GcInvariantValidationTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          GcInvariantValidation.class.isInterface(),
          "GcInvariantValidation should be an interface");
    }
  }

  // ========================================================================
  // Instance Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Classes Tests")
  class InstanceClassesTests {

    @Test
    @DisplayName("StructInstance should be an interface")
    void structInstanceShouldBeAnInterface() {
      assertTrue(StructInstance.class.isInterface(), "StructInstance should be an interface");
    }

    @Test
    @DisplayName("ArrayInstance should be an interface")
    void arrayInstanceShouldBeAnInterface() {
      assertTrue(ArrayInstance.class.isInterface(), "ArrayInstance should be an interface");
    }

    @Test
    @DisplayName("I31Instance should be an interface")
    void i31InstanceShouldBeAnInterface() {
      assertTrue(I31Instance.class.isInterface(), "I31Instance should be an interface");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("GcRef should have at least 3 methods")
    void gcRefShouldHaveMinimumMethods() {
      int methodCount = GcRef.class.getDeclaredMethods().length;
      assertTrue(methodCount >= 3, "GcRef should have at least 3 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("GcReferenceType should have multiple values")
    void gcReferenceTypeShouldHaveMultipleValues() {
      GcReferenceType[] values = GcReferenceType.values();
      assertTrue(
          values.length >= 5,
          "GcReferenceType should have at least 5 values, found: " + values.length);
    }

    @Test
    @DisplayName("AnyRef should have at least 10 methods")
    void anyRefShouldHaveMinimumMethods() {
      int methodCount = AnyRef.class.getDeclaredMethods().length;
      assertTrue(
          methodCount >= 10, "AnyRef should have at least 10 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("StructType should have at least 10 methods")
    void structTypeShouldHaveMinimumMethods() {
      int methodCount = StructType.class.getDeclaredMethods().length;
      assertTrue(
          methodCount >= 10, "StructType should have at least 10 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("I31Type should have at least 10 static methods")
    void i31TypeShouldHaveMinimumMethods() {
      long staticMethodCount =
          Arrays.stream(I31Type.class.getDeclaredMethods())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertTrue(
          staticMethodCount >= 10,
          "I31Type should have at least 10 static methods, found: " + staticMethodCount);
    }
  }
}

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

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.type.ValType;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ValType} interface.
 *
 * <p>ValType represents a WebAssembly value type with rich querying capabilities.
 */
@DisplayName("ValType Tests")
class ValTypeTest {

  /** Test implementation of ValType for testing purposes. */
  private static class TestValType implements ValType {
    private final WasmValueType valueType;

    TestValType(final WasmValueType valueType) {
      this.valueType = valueType;
    }

    @Override
    public WasmValueType getValueType() {
      return valueType;
    }

    @Override
    public boolean isNumeric() {
      return valueType == WasmValueType.I32
          || valueType == WasmValueType.I64
          || valueType == WasmValueType.F32
          || valueType == WasmValueType.F64;
    }

    @Override
    public boolean isInteger() {
      return valueType == WasmValueType.I32 || valueType == WasmValueType.I64;
    }

    @Override
    public boolean isFloat() {
      return valueType == WasmValueType.F32 || valueType == WasmValueType.F64;
    }

    @Override
    public boolean isReference() {
      return valueType == WasmValueType.FUNCREF
          || valueType == WasmValueType.EXTERNREF
          || isGcReference();
    }

    @Override
    public boolean isGcReference() {
      return valueType == WasmValueType.ANYREF
          || valueType == WasmValueType.EQREF
          || valueType == WasmValueType.I31REF
          || valueType == WasmValueType.STRUCTREF
          || valueType == WasmValueType.ARRAYREF
          || isNullableReference();
    }

    @Override
    public boolean isNullableReference() {
      return valueType == WasmValueType.NULLREF
          || valueType == WasmValueType.NULLFUNCREF
          || valueType == WasmValueType.NULLEXTERNREF;
    }

    @Override
    public boolean isVector() {
      return valueType == WasmValueType.V128;
    }

    @Override
    public boolean matches(final ValType other) {
      if (other == null) {
        return false;
      }
      return valueType == other.getValueType();
    }

    @Override
    public boolean eq(final ValType other) {
      if (other == null) {
        return false;
      }
      return valueType == other.getValueType();
    }
  }

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(Modifier.isPublic(ValType.class.getModifiers()), "ValType should be public");
      assertTrue(ValType.class.isInterface(), "ValType should be an interface");
    }

    @Test
    @DisplayName("should have getValueType method")
    void shouldHaveGetValueTypeMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("getValueType");
      assertNotNull(method, "getValueType method should exist");
      assertEquals(
          WasmValueType.class, method.getReturnType(), "getValueType should return WasmValueType");
    }

    @Test
    @DisplayName("should have isNumeric method")
    void shouldHaveIsNumericMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("isNumeric");
      assertNotNull(method, "isNumeric method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isNumeric should return boolean");
    }

    @Test
    @DisplayName("should have isInteger method")
    void shouldHaveIsIntegerMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("isInteger");
      assertNotNull(method, "isInteger method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isInteger should return boolean");
    }

    @Test
    @DisplayName("should have isFloat method")
    void shouldHaveIsFloatMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("isFloat");
      assertNotNull(method, "isFloat method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isFloat should return boolean");
    }

    @Test
    @DisplayName("should have isReference method")
    void shouldHaveIsReferenceMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("isReference");
      assertNotNull(method, "isReference method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isReference should return boolean");
    }

    @Test
    @DisplayName("should have isGcReference method")
    void shouldHaveIsGcReferenceMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("isGcReference");
      assertNotNull(method, "isGcReference method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isGcReference should return boolean");
    }

    @Test
    @DisplayName("should have isNullableReference method")
    void shouldHaveIsNullableReferenceMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("isNullableReference");
      assertNotNull(method, "isNullableReference method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isNullableReference should return boolean");
    }

    @Test
    @DisplayName("should have isVector method")
    void shouldHaveIsVectorMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("isVector");
      assertNotNull(method, "isVector method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isVector should return boolean");
    }

    @Test
    @DisplayName("should have matches method")
    void shouldHaveMatchesMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("matches", ValType.class);
      assertNotNull(method, "matches method should exist");
      assertEquals(boolean.class, method.getReturnType(), "matches should return boolean");
    }

    @Test
    @DisplayName("should have eq method")
    void shouldHaveEqMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("eq", ValType.class);
      assertNotNull(method, "eq method should exist");
      assertEquals(boolean.class, method.getReturnType(), "eq should return boolean");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have from static method")
    void shouldHaveFromStaticMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("from", WasmValueType.class);
      assertNotNull(method, "from method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "from should be static");
    }

    @Test
    @DisplayName("should have i32 static method")
    void shouldHaveI32StaticMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("i32");
      assertNotNull(method, "i32 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "i32 should be static");
    }

    @Test
    @DisplayName("should have i64 static method")
    void shouldHaveI64StaticMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("i64");
      assertNotNull(method, "i64 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "i64 should be static");
    }

    @Test
    @DisplayName("should have f32 static method")
    void shouldHaveF32StaticMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("f32");
      assertNotNull(method, "f32 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "f32 should be static");
    }

    @Test
    @DisplayName("should have f64 static method")
    void shouldHaveF64StaticMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("f64");
      assertNotNull(method, "f64 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "f64 should be static");
    }

    @Test
    @DisplayName("should have v128 static method")
    void shouldHaveV128StaticMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("v128");
      assertNotNull(method, "v128 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "v128 should be static");
    }

    @Test
    @DisplayName("should have funcref static method")
    void shouldHaveFuncrefStaticMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("funcref");
      assertNotNull(method, "funcref method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "funcref should be static");
    }

    @Test
    @DisplayName("should have externref static method")
    void shouldHaveExternrefStaticMethod() throws NoSuchMethodException {
      final Method method = ValType.class.getMethod("externref");
      assertNotNull(method, "externref method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "externref should be static");
    }
  }

  @Nested
  @DisplayName("Numeric Type Tests")
  class NumericTypeTests {

    @Test
    @DisplayName("I32 should be numeric")
    void i32ShouldBeNumeric() {
      final ValType valType = new TestValType(WasmValueType.I32);
      assertTrue(valType.isNumeric(), "I32 should be numeric");
      assertTrue(valType.isInteger(), "I32 should be integer");
      assertFalse(valType.isFloat(), "I32 should not be float");
    }

    @Test
    @DisplayName("I64 should be numeric")
    void i64ShouldBeNumeric() {
      final ValType valType = new TestValType(WasmValueType.I64);
      assertTrue(valType.isNumeric(), "I64 should be numeric");
      assertTrue(valType.isInteger(), "I64 should be integer");
      assertFalse(valType.isFloat(), "I64 should not be float");
    }

    @Test
    @DisplayName("F32 should be numeric")
    void f32ShouldBeNumeric() {
      final ValType valType = new TestValType(WasmValueType.F32);
      assertTrue(valType.isNumeric(), "F32 should be numeric");
      assertFalse(valType.isInteger(), "F32 should not be integer");
      assertTrue(valType.isFloat(), "F32 should be float");
    }

    @Test
    @DisplayName("F64 should be numeric")
    void f64ShouldBeNumeric() {
      final ValType valType = new TestValType(WasmValueType.F64);
      assertTrue(valType.isNumeric(), "F64 should be numeric");
      assertFalse(valType.isInteger(), "F64 should not be integer");
      assertTrue(valType.isFloat(), "F64 should be float");
    }
  }

  @Nested
  @DisplayName("Reference Type Tests")
  class ReferenceTypeTests {

    @Test
    @DisplayName("FUNCREF should be reference")
    void funcrefShouldBeReference() {
      final ValType valType = new TestValType(WasmValueType.FUNCREF);
      assertTrue(valType.isReference(), "FUNCREF should be reference");
      assertFalse(valType.isNumeric(), "FUNCREF should not be numeric");
    }

    @Test
    @DisplayName("EXTERNREF should be reference")
    void externrefShouldBeReference() {
      final ValType valType = new TestValType(WasmValueType.EXTERNREF);
      assertTrue(valType.isReference(), "EXTERNREF should be reference");
      assertFalse(valType.isNumeric(), "EXTERNREF should not be numeric");
    }
  }

  @Nested
  @DisplayName("Vector Type Tests")
  class VectorTypeTests {

    @Test
    @DisplayName("V128 should be vector")
    void v128ShouldBeVector() {
      final ValType valType = new TestValType(WasmValueType.V128);
      assertTrue(valType.isVector(), "V128 should be vector");
      assertFalse(valType.isNumeric(), "V128 should not be numeric");
      assertFalse(valType.isReference(), "V128 should not be reference");
    }
  }

  @Nested
  @DisplayName("GC Reference Type Tests")
  class GcReferenceTypeTests {

    @Test
    @DisplayName("ANYREF should be GC reference")
    void anyrefShouldBeGcReference() {
      final ValType valType = new TestValType(WasmValueType.ANYREF);
      assertTrue(valType.isGcReference(), "ANYREF should be GC reference");
      assertTrue(valType.isReference(), "ANYREF should be reference");
    }

    @Test
    @DisplayName("EQREF should be GC reference")
    void eqrefShouldBeGcReference() {
      final ValType valType = new TestValType(WasmValueType.EQREF);
      assertTrue(valType.isGcReference(), "EQREF should be GC reference");
    }

    @Test
    @DisplayName("I31REF should be GC reference")
    void i31refShouldBeGcReference() {
      final ValType valType = new TestValType(WasmValueType.I31REF);
      assertTrue(valType.isGcReference(), "I31REF should be GC reference");
    }

    @Test
    @DisplayName("STRUCTREF should be GC reference")
    void structrefShouldBeGcReference() {
      final ValType valType = new TestValType(WasmValueType.STRUCTREF);
      assertTrue(valType.isGcReference(), "STRUCTREF should be GC reference");
    }

    @Test
    @DisplayName("ARRAYREF should be GC reference")
    void arrayrefShouldBeGcReference() {
      final ValType valType = new TestValType(WasmValueType.ARRAYREF);
      assertTrue(valType.isGcReference(), "ARRAYREF should be GC reference");
    }
  }

  @Nested
  @DisplayName("Nullable Reference Type Tests")
  class NullableReferenceTypeTests {

    @Test
    @DisplayName("NULLREF should be nullable reference")
    void nullrefShouldBeNullableReference() {
      final ValType valType = new TestValType(WasmValueType.NULLREF);
      assertTrue(valType.isNullableReference(), "NULLREF should be nullable reference");
      assertTrue(valType.isGcReference(), "NULLREF should be GC reference");
    }

    @Test
    @DisplayName("NULLFUNCREF should be nullable reference")
    void nullfuncrefShouldBeNullableReference() {
      final ValType valType = new TestValType(WasmValueType.NULLFUNCREF);
      assertTrue(valType.isNullableReference(), "NULLFUNCREF should be nullable reference");
    }

    @Test
    @DisplayName("NULLEXTERNREF should be nullable reference")
    void nullexternrefShouldBeNullableReference() {
      final ValType valType = new TestValType(WasmValueType.NULLEXTERNREF);
      assertTrue(valType.isNullableReference(), "NULLEXTERNREF should be nullable reference");
    }
  }

  @Nested
  @DisplayName("Type Matching Tests")
  class TypeMatchingTests {

    @Test
    @DisplayName("same types should match")
    void sameTypesShouldMatch() {
      final ValType type1 = new TestValType(WasmValueType.I32);
      final ValType type2 = new TestValType(WasmValueType.I32);

      assertTrue(type1.matches(type2), "Same types should match");
      assertTrue(type2.matches(type1), "Matching should be symmetric");
    }

    @Test
    @DisplayName("different types should not match")
    void differentTypesShouldNotMatch() {
      final ValType i32Type = new TestValType(WasmValueType.I32);
      final ValType i64Type = new TestValType(WasmValueType.I64);

      assertFalse(i32Type.matches(i64Type), "Different types should not match");
    }

    @Test
    @DisplayName("null type should not match")
    void nullTypeShouldNotMatch() {
      final ValType type = new TestValType(WasmValueType.I32);

      assertFalse(type.matches(null), "Should not match null");
    }
  }

  @Nested
  @DisplayName("Type Equality Tests")
  class TypeEqualityTests {

    @Test
    @DisplayName("same types should be equal")
    void sameTypesShouldBeEqual() {
      final ValType type1 = new TestValType(WasmValueType.F64);
      final ValType type2 = new TestValType(WasmValueType.F64);

      assertTrue(type1.eq(type2), "Same types should be equal");
    }

    @Test
    @DisplayName("different types should not be equal")
    void differentTypesShouldNotBeEqual() {
      final ValType f32Type = new TestValType(WasmValueType.F32);
      final ValType f64Type = new TestValType(WasmValueType.F64);

      assertFalse(f32Type.eq(f64Type), "Different types should not be equal");
    }

    @Test
    @DisplayName("null type should not be equal")
    void nullTypeShouldNotBeEqual() {
      final ValType type = new TestValType(WasmValueType.I32);

      assertFalse(type.eq(null), "Should not equal null");
    }
  }
}

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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcValue} class.
 *
 * <p>GcValue represents values that can be stored in GC structs and arrays.
 */
@DisplayName("GcValue Tests")
class GcValueTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(Modifier.isPublic(GcValue.class.getModifiers()), "GcValue should be public");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = GcValue.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(
          GcValue.Type.class, method.getReturnType(), "getType should return GcValue.Type");
    }

    @Test
    @DisplayName("should have accessor methods for value extraction")
    void shouldHaveAccessorMethods() throws NoSuchMethodException {
      assertNotNull(GcValue.class.getMethod("asI32"), "asI32 method should exist");
      assertNotNull(GcValue.class.getMethod("asI64"), "asI64 method should exist");
      assertNotNull(GcValue.class.getMethod("asF32"), "asF32 method should exist");
      assertNotNull(GcValue.class.getMethod("asF64"), "asF64 method should exist");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static i32 factory method")
    void shouldHaveStaticI32FactoryMethod() throws NoSuchMethodException {
      final Method method = GcValue.class.getMethod("i32", int.class);
      assertNotNull(method, "i32 factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "i32 should be static");
      assertEquals(GcValue.class, method.getReturnType(), "i32 should return GcValue");
    }

    @Test
    @DisplayName("should have static i64 factory method")
    void shouldHaveStaticI64FactoryMethod() throws NoSuchMethodException {
      final Method method = GcValue.class.getMethod("i64", long.class);
      assertNotNull(method, "i64 factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "i64 should be static");
      assertEquals(GcValue.class, method.getReturnType(), "i64 should return GcValue");
    }

    @Test
    @DisplayName("should have static f32 factory method")
    void shouldHaveStaticF32FactoryMethod() throws NoSuchMethodException {
      final Method method = GcValue.class.getMethod("f32", float.class);
      assertNotNull(method, "f32 factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "f32 should be static");
      assertEquals(GcValue.class, method.getReturnType(), "f32 should return GcValue");
    }

    @Test
    @DisplayName("should have static f64 factory method")
    void shouldHaveStaticF64FactoryMethod() throws NoSuchMethodException {
      final Method method = GcValue.class.getMethod("f64", double.class);
      assertNotNull(method, "f64 factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "f64 should be static");
      assertEquals(GcValue.class, method.getReturnType(), "f64 should return GcValue");
    }

    @Test
    @DisplayName("should have static anyRef factory method")
    void shouldHaveStaticAnyRefFactoryMethod() throws NoSuchMethodException {
      final Method method = GcValue.class.getMethod("anyRef", AnyRef.class);
      assertNotNull(method, "anyRef factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "anyRef should be static");
      assertEquals(GcValue.class, method.getReturnType(), "anyRef should return GcValue");
    }

    @Test
    @DisplayName("should have static structRef factory method")
    void shouldHaveStaticStructRefFactoryMethod() throws NoSuchMethodException {
      final Method method = GcValue.class.getMethod("structRef", StructRef.class);
      assertNotNull(method, "structRef factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "structRef should be static");
      assertEquals(GcValue.class, method.getReturnType(), "structRef should return GcValue");
    }

    @Test
    @DisplayName("should have static arrayRef factory method")
    void shouldHaveStaticArrayRefFactoryMethod() throws NoSuchMethodException {
      final Method method = GcValue.class.getMethod("arrayRef", ArrayRef.class);
      assertNotNull(method, "arrayRef factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "arrayRef should be static");
      assertEquals(GcValue.class, method.getReturnType(), "arrayRef should return GcValue");
    }
  }

  @Nested
  @DisplayName("Value Creation Tests")
  class ValueCreationTests {

    @Test
    @DisplayName("should create i32 value")
    void shouldCreateI32Value() {
      final GcValue value = GcValue.i32(42);
      assertNotNull(value, "i32 value should be created");
      assertEquals(GcValue.Type.I32, value.getType(), "Type should be I32");
      assertEquals(42, value.asI32(), "Value should be 42");
    }

    @Test
    @DisplayName("should create i64 value")
    void shouldCreateI64Value() {
      final GcValue value = GcValue.i64(123456789L);
      assertNotNull(value, "i64 value should be created");
      assertEquals(GcValue.Type.I64, value.getType(), "Type should be I64");
      assertEquals(123456789L, value.asI64(), "Value should be 123456789");
    }

    @Test
    @DisplayName("should create f32 value")
    void shouldCreateF32Value() {
      final GcValue value = GcValue.f32(3.14f);
      assertNotNull(value, "f32 value should be created");
      assertEquals(GcValue.Type.F32, value.getType(), "Type should be F32");
      assertEquals(3.14f, value.asF32(), "Value should be 3.14");
    }

    @Test
    @DisplayName("should create f64 value")
    void shouldCreateF64Value() {
      final GcValue value = GcValue.f64(2.718281828);
      assertNotNull(value, "f64 value should be created");
      assertEquals(GcValue.Type.F64, value.getType(), "Type should be F64");
      assertEquals(2.718281828, value.asF64(), "Value should be 2.718281828");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have asI32 method")
    void shouldHaveAsI32Method() throws NoSuchMethodException {
      final Method method = GcValue.class.getMethod("asI32");
      assertNotNull(method, "asI32 method should exist");
      assertEquals(int.class, method.getReturnType(), "asI32 should return int");
    }

    @Test
    @DisplayName("should have asI64 method")
    void shouldHaveAsI64Method() throws NoSuchMethodException {
      final Method method = GcValue.class.getMethod("asI64");
      assertNotNull(method, "asI64 method should exist");
      assertEquals(long.class, method.getReturnType(), "asI64 should return long");
    }

    @Test
    @DisplayName("should have asF32 method")
    void shouldHaveAsF32Method() throws NoSuchMethodException {
      final Method method = GcValue.class.getMethod("asF32");
      assertNotNull(method, "asF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "asF32 should return float");
    }

    @Test
    @DisplayName("should have asF64 method")
    void shouldHaveAsF64Method() throws NoSuchMethodException {
      final Method method = GcValue.class.getMethod("asF64");
      assertNotNull(method, "asF64 method should exist");
      assertEquals(double.class, method.getReturnType(), "asF64 should return double");
    }

    @Test
    @DisplayName("asI32 should return correct value")
    void asI32ShouldReturnCorrectValue() {
      final GcValue value = GcValue.i32(999);
      assertEquals(999, value.asI32(), "asI32 should return 999");
    }

    @Test
    @DisplayName("asI64 should return correct value")
    void asI64ShouldReturnCorrectValue() {
      final GcValue value = GcValue.i64(Long.MAX_VALUE);
      assertEquals(Long.MAX_VALUE, value.asI64(), "asI64 should return Long.MAX_VALUE");
    }

    @Test
    @DisplayName("asF32 should return correct value")
    void asF32ShouldReturnCorrectValue() {
      final GcValue value = GcValue.f32(Float.MIN_VALUE);
      assertEquals(Float.MIN_VALUE, value.asF32(), "asF32 should return Float.MIN_VALUE");
    }

    @Test
    @DisplayName("asF64 should return correct value")
    void asF64ShouldReturnCorrectValue() {
      final GcValue value = GcValue.f64(Double.MAX_VALUE);
      assertEquals(Double.MAX_VALUE, value.asF64(), "asF64 should return Double.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle zero values")
    void shouldHandleZeroValues() {
      final GcValue i32Zero = GcValue.i32(0);
      final GcValue i64Zero = GcValue.i64(0L);
      final GcValue f32Zero = GcValue.f32(0.0f);
      final GcValue f64Zero = GcValue.f64(0.0);

      assertEquals(0, i32Zero.asI32(), "i32 zero should be 0");
      assertEquals(0L, i64Zero.asI64(), "i64 zero should be 0L");
      assertEquals(0.0f, f32Zero.asF32(), "f32 zero should be 0.0f");
      assertEquals(0.0, f64Zero.asF64(), "f64 zero should be 0.0");
    }

    @Test
    @DisplayName("should handle negative values")
    void shouldHandleNegativeValues() {
      final GcValue i32Neg = GcValue.i32(-100);
      final GcValue i64Neg = GcValue.i64(-9999999999L);
      final GcValue f32Neg = GcValue.f32(-3.14f);
      final GcValue f64Neg = GcValue.f64(-2.71828);

      assertEquals(-100, i32Neg.asI32(), "i32 negative should work");
      assertEquals(-9999999999L, i64Neg.asI64(), "i64 negative should work");
      assertEquals(-3.14f, f32Neg.asF32(), "f32 negative should work");
      assertEquals(-2.71828, f64Neg.asF64(), "f64 negative should work");
    }

    @Test
    @DisplayName("should handle max/min integer values")
    void shouldHandleMaxMinIntegerValues() {
      final GcValue i32Max = GcValue.i32(Integer.MAX_VALUE);
      final GcValue i32Min = GcValue.i32(Integer.MIN_VALUE);
      final GcValue i64Max = GcValue.i64(Long.MAX_VALUE);
      final GcValue i64Min = GcValue.i64(Long.MIN_VALUE);

      assertEquals(Integer.MAX_VALUE, i32Max.asI32(), "i32 MAX should work");
      assertEquals(Integer.MIN_VALUE, i32Min.asI32(), "i32 MIN should work");
      assertEquals(Long.MAX_VALUE, i64Max.asI64(), "i64 MAX should work");
      assertEquals(Long.MIN_VALUE, i64Min.asI64(), "i64 MIN should work");
    }

    @Test
    @DisplayName("should handle special float values")
    void shouldHandleSpecialFloatValues() {
      final GcValue f32PosInf = GcValue.f32(Float.POSITIVE_INFINITY);
      final GcValue f32NegInf = GcValue.f32(Float.NEGATIVE_INFINITY);
      final GcValue f64PosInf = GcValue.f64(Double.POSITIVE_INFINITY);
      final GcValue f64NegInf = GcValue.f64(Double.NEGATIVE_INFINITY);

      assertEquals(Float.POSITIVE_INFINITY, f32PosInf.asF32(), "f32 +Inf should work");
      assertEquals(Float.NEGATIVE_INFINITY, f32NegInf.asF32(), "f32 -Inf should work");
      assertEquals(Double.POSITIVE_INFINITY, f64PosInf.asF64(), "f64 +Inf should work");
      assertEquals(Double.NEGATIVE_INFINITY, f64NegInf.asF64(), "f64 -Inf should work");
    }

    @Test
    @DisplayName("should handle NaN float values")
    void shouldHandleNaNFloatValues() {
      final GcValue f32Nan = GcValue.f32(Float.NaN);
      final GcValue f64Nan = GcValue.f64(Double.NaN);

      assertTrue(Float.isNaN(f32Nan.asF32()), "f32 NaN should be NaN");
      assertTrue(Double.isNaN(f64Nan.asF64()), "f64 NaN should be NaN");
    }
  }
}

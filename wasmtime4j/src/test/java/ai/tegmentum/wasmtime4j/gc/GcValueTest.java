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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link GcValue} abstract class.
 *
 * <p>GcValue represents any value that can be stored in GC objects including primitive types,
 * packed types, and reference types. Tests exercise concrete subclass behavior via factory methods.
 */
@DisplayName("GcValue Tests")
class GcValueTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be abstract")
    void shouldBeAbstract() {
      assertTrue(Modifier.isAbstract(GcValue.class.getModifiers()), "GcValue should be abstract");
    }

    @Test
    @DisplayName("Type enum should have all expected values")
    void typeEnumShouldHaveAllExpectedValues() {
      final GcValue.Type[] values = GcValue.Type.values();
      assertEquals(7, values.length, "Type enum should have 7 values");
      assertNotNull(GcValue.Type.I32, "Should have I32");
      assertNotNull(GcValue.Type.I64, "Should have I64");
      assertNotNull(GcValue.Type.F32, "Should have F32");
      assertNotNull(GcValue.Type.F64, "Should have F64");
      assertNotNull(GcValue.Type.V128, "Should have V128");
      assertNotNull(GcValue.Type.REFERENCE, "Should have REFERENCE");
      assertNotNull(GcValue.Type.NULL, "Should have NULL");
    }
  }

  @Nested
  @DisplayName("I32 Value Tests")
  class I32ValueTests {

    @Test
    @DisplayName("i32 should create I32 value")
    void i32ShouldCreateI32Value() {
      final GcValue val = GcValue.i32(42);
      assertEquals(GcValue.Type.I32, val.getType(), "Type should be I32");
      assertEquals(42, val.asI32(), "asI32 should return 42");
    }

    @Test
    @DisplayName("i32 should not be null")
    void i32ShouldNotBeNull() {
      final GcValue val = GcValue.i32(0);
      assertFalse(val.isNull(), "I32 should not be null");
    }

    @Test
    @DisplayName("i32 should not be reference")
    void i32ShouldNotBeReference() {
      final GcValue val = GcValue.i32(0);
      assertFalse(val.isReference(), "I32 should not be a reference");
    }

    @Test
    @DisplayName("equal i32 values should be equal")
    void equalI32ValuesShouldBeEqual() {
      final GcValue v1 = GcValue.i32(42);
      final GcValue v2 = GcValue.i32(42);
      assertEquals(v1, v2, "Equal I32 values should be equal");
      assertEquals(v1.hashCode(), v2.hashCode(), "Equal values should have same hash");
    }

    @Test
    @DisplayName("different i32 values should not be equal")
    void differentI32ValuesShouldNotBeEqual() {
      final GcValue v1 = GcValue.i32(42);
      final GcValue v2 = GcValue.i32(43);
      assertNotEquals(v1, v2, "Different I32 values should not be equal");
    }

    @Test
    @DisplayName("i32 toString should contain value")
    void i32ToStringShouldContainValue() {
      final GcValue val = GcValue.i32(42);
      assertTrue(val.toString().contains("42"), "toString should contain the value");
    }

    @Test
    @DisplayName("i32 toWasmValue should not return null")
    void i32ToWasmValueShouldNotReturnNull() {
      final GcValue val = GcValue.i32(42);
      assertNotNull(val.toWasmValue(), "toWasmValue should not return null");
    }

    @ParameterizedTest(name = "i32({0}) should create valid I32 value and round-trip")
    @ValueSource(ints = {0, -1, 1, Integer.MAX_VALUE, Integer.MIN_VALUE, 42, -42})
    @DisplayName("i32 should handle boundary and typical values correctly")
    void i32ShouldHandleBoundaryValues(int value) {
      final GcValue val = GcValue.i32(value);
      assertEquals(GcValue.Type.I32, val.getType(), "Type should be I32");
      assertEquals(value, val.asI32(), "asI32 should return " + value);
      assertFalse(val.isNull(), "I32 should not be null");
      assertFalse(val.isReference(), "I32 should not be a reference");
      assertNotNull(val.toWasmValue(), "toWasmValue should not return null");
    }
  }

  @Nested
  @DisplayName("I64 Value Tests")
  class I64ValueTests {

    @Test
    @DisplayName("i64 should create I64 value")
    void i64ShouldCreateI64Value() {
      final GcValue val = GcValue.i64(100L);
      assertEquals(GcValue.Type.I64, val.getType(), "Type should be I64");
      assertEquals(100L, val.asI64(), "asI64 should return 100");
    }

    @Test
    @DisplayName("equal i64 values should be equal")
    void equalI64ValuesShouldBeEqual() {
      final GcValue v1 = GcValue.i64(100L);
      final GcValue v2 = GcValue.i64(100L);
      assertEquals(v1, v2, "Equal I64 values should be equal");
    }

    @ParameterizedTest(name = "i64({0}) should create valid I64 value and round-trip")
    @ValueSource(longs = {0L, -1L, 1L, Long.MAX_VALUE, Long.MIN_VALUE, 100L, -100L})
    @DisplayName("i64 should handle boundary and typical values correctly")
    void i64ShouldHandleBoundaryValues(long value) {
      final GcValue val = GcValue.i64(value);
      assertEquals(GcValue.Type.I64, val.getType(), "Type should be I64");
      assertEquals(value, val.asI64(), "asI64 should return " + value);
      assertFalse(val.isNull(), "I64 should not be null");
      assertFalse(val.isReference(), "I64 should not be a reference");
    }
  }

  @Nested
  @DisplayName("F32 Value Tests")
  class F32ValueTests {

    @Test
    @DisplayName("f32 should create F32 value")
    void f32ShouldCreateF32Value() {
      final GcValue val = GcValue.f32(3.14f);
      assertEquals(GcValue.Type.F32, val.getType(), "Type should be F32");
      assertEquals(3.14f, val.asF32(), 0.001f, "asF32 should return 3.14");
    }

    @Test
    @DisplayName("equal f32 values should be equal")
    void equalF32ValuesShouldBeEqual() {
      final GcValue v1 = GcValue.f32(1.0f);
      final GcValue v2 = GcValue.f32(1.0f);
      assertEquals(v1, v2, "Equal F32 values should be equal");
    }

    @ParameterizedTest(name = "f32({0}) should create valid F32 value and round-trip")
    @ValueSource(
        floats = {
          0.0f,
          -1.0f,
          1.0f,
          Float.MAX_VALUE,
          Float.MIN_VALUE,
          Float.POSITIVE_INFINITY,
          Float.NEGATIVE_INFINITY
        })
    @DisplayName("f32 should handle boundary and special float values correctly")
    void f32ShouldHandleBoundaryValues(float value) {
      final GcValue val = GcValue.f32(value);
      assertEquals(GcValue.Type.F32, val.getType(), "Type should be F32");
      assertEquals(value, val.asF32(), "asF32 should return " + value);
    }
  }

  @Nested
  @DisplayName("F64 Value Tests")
  class F64ValueTests {

    @Test
    @DisplayName("f64 should create F64 value")
    void f64ShouldCreateF64Value() {
      final GcValue val = GcValue.f64(2.718);
      assertEquals(GcValue.Type.F64, val.getType(), "Type should be F64");
      assertEquals(2.718, val.asF64(), 0.001, "asF64 should return 2.718");
    }

    @Test
    @DisplayName("equal f64 values should be equal")
    void equalF64ValuesShouldBeEqual() {
      final GcValue v1 = GcValue.f64(1.0);
      final GcValue v2 = GcValue.f64(1.0);
      assertEquals(v1, v2, "Equal F64 values should be equal");
    }

    @ParameterizedTest(name = "f64({0}) should create valid F64 value and round-trip")
    @ValueSource(
        doubles = {
          0.0,
          -1.0,
          1.0,
          Double.MAX_VALUE,
          Double.MIN_VALUE,
          Double.POSITIVE_INFINITY,
          Double.NEGATIVE_INFINITY
        })
    @DisplayName("f64 should handle boundary and special double values correctly")
    void f64ShouldHandleBoundaryValues(double value) {
      final GcValue val = GcValue.f64(value);
      assertEquals(GcValue.Type.F64, val.getType(), "Type should be F64");
      assertEquals(value, val.asF64(), "asF64 should return " + value);
    }
  }

  @Nested
  @DisplayName("V128 Value Tests")
  class V128ValueTests {

    @Test
    @DisplayName("v128 should create V128 value")
    void v128ShouldCreateV128Value() {
      final byte[] bytes = new byte[16];
      bytes[0] = 1;
      final GcValue val = GcValue.v128(bytes);
      assertEquals(GcValue.Type.V128, val.getType(), "Type should be V128");
    }

    @Test
    @DisplayName("v128 with wrong length should throw")
    void v128WithWrongLengthShouldThrow() {
      final byte[] wrongBytes = new byte[8];
      assertThrows(
          IllegalArgumentException.class,
          () -> GcValue.v128(wrongBytes),
          "v128 with non-16-byte array should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("v128 asV128 should return copy")
    void v128AsV128ShouldReturnCopy() {
      final byte[] bytes = new byte[16];
      bytes[0] = 42;
      final GcValue val = GcValue.v128(bytes);
      final byte[] result = val.asV128();
      assertEquals(42, result[0], "First byte should be 42");
    }
  }

  @Nested
  @DisplayName("Null Value Tests")
  class NullValueTests {

    @Test
    @DisplayName("nullValue should create NULL type")
    void nullValueShouldCreateNullType() {
      final GcValue val = GcValue.nullValue();
      assertEquals(GcValue.Type.NULL, val.getType(), "Type should be NULL");
      assertTrue(val.isNull(), "Null value isNull should be true");
    }

    @Test
    @DisplayName("two null values should be equal")
    void twoNullValuesShouldBeEqual() {
      final GcValue v1 = GcValue.nullValue();
      final GcValue v2 = GcValue.nullValue();
      assertEquals(v1, v2, "Two null values should be equal");
    }

    @Test
    @DisplayName("null value toString should contain null")
    void nullValueToStringShouldContainNull() {
      final GcValue val = GcValue.nullValue();
      assertNotNull(val.toString(), "toString should not return null");
      assertTrue(val.toString().contains("null"), "toString should contain 'null'");
    }
  }

  @Nested
  @DisplayName("FromObject Tests")
  class FromObjectTests {

    @Test
    @DisplayName("fromObject null should return null value")
    void fromObjectNullShouldReturnNullValue() {
      final GcValue val = GcValue.fromObject(null);
      assertTrue(val.isNull(), "fromObject(null) should return null value");
    }

    @Test
    @DisplayName("fromObject Integer should return I32")
    void fromObjectIntegerShouldReturnI32() {
      final GcValue val = GcValue.fromObject(42);
      assertEquals(GcValue.Type.I32, val.getType(), "Integer should map to I32");
      assertEquals(42, val.asI32(), "Value should be 42");
    }

    @Test
    @DisplayName("fromObject Long should return I64")
    void fromObjectLongShouldReturnI64() {
      final GcValue val = GcValue.fromObject(100L);
      assertEquals(GcValue.Type.I64, val.getType(), "Long should map to I64");
    }

    @Test
    @DisplayName("fromObject Float should return F32")
    void fromObjectFloatShouldReturnF32() {
      final GcValue val = GcValue.fromObject(1.5f);
      assertEquals(GcValue.Type.F32, val.getType(), "Float should map to F32");
    }

    @Test
    @DisplayName("fromObject Double should return F64")
    void fromObjectDoubleShouldReturnF64() {
      final GcValue val = GcValue.fromObject(2.5);
      assertEquals(GcValue.Type.F64, val.getType(), "Double should map to F64");
    }

    @Test
    @DisplayName("fromObject 16-byte array should return V128")
    void fromObject16ByteArrayShouldReturnV128() {
      final GcValue val = GcValue.fromObject(new byte[16]);
      assertEquals(GcValue.Type.V128, val.getType(), "16-byte array should map to V128");
    }

    @Test
    @DisplayName("fromObject unsupported type should throw")
    void fromObjectUnsupportedTypeShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> GcValue.fromObject("string"),
          "Unsupported type should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Type Mismatch Tests")
  class TypeMismatchTests {

    @Test
    @DisplayName("asI32 on I64 should throw IllegalStateException")
    void asI32OnI64ShouldThrow() {
      final GcValue val = GcValue.i64(100L);
      assertThrows(
          IllegalStateException.class,
          val::asI32,
          "asI32 on I64 value should throw IllegalStateException");
    }

    @Test
    @DisplayName("asI64 on I32 should throw IllegalStateException")
    void asI64OnI32ShouldThrow() {
      final GcValue val = GcValue.i32(42);
      assertThrows(
          IllegalStateException.class,
          val::asI64,
          "asI64 on I32 value should throw IllegalStateException");
    }

    @Test
    @DisplayName("asF32 on I32 should throw IllegalStateException")
    void asF32OnI32ShouldThrow() {
      final GcValue val = GcValue.i32(42);
      assertThrows(
          IllegalStateException.class,
          val::asF32,
          "asF32 on I32 value should throw IllegalStateException");
    }

    @Test
    @DisplayName("asF64 on I32 should throw IllegalStateException")
    void asF64OnI32ShouldThrow() {
      final GcValue val = GcValue.i32(42);
      assertThrows(
          IllegalStateException.class,
          val::asF64,
          "asF64 on I32 value should throw IllegalStateException");
    }

    @Test
    @DisplayName("asReference on I32 should throw IllegalStateException")
    void asReferenceOnI32ShouldThrow() {
      final GcValue val = GcValue.i32(42);
      assertThrows(
          IllegalStateException.class,
          val::asReference,
          "asReference on I32 value should throw IllegalStateException");
    }
  }
}

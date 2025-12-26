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

package ai.tegmentum.wasmtime4j.jni.debug;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link JniVariableValue}. */
@DisplayName("JniVariableValue Tests")
class JniVariableValueTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniVariableValue should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniVariableValue.class.getModifiers()),
          "JniVariableValue should be final");
    }

    @Test
    @DisplayName("JniVariableValue should have no public constructors")
    void shouldHaveNoPublicConstructors() {
      assertEquals(
          0, JniVariableValue.class.getConstructors().length, "Should have no public constructors");
    }
  }

  @Nested
  @DisplayName("i32 Factory Tests")
  class I32FactoryTests {

    @Test
    @DisplayName("i32 should create I32 value")
    void i32ShouldCreateI32Value() {
      final JniVariableValue value = JniVariableValue.i32(42);

      assertEquals(JniVariableValue.ValueType.I32, value.getType(), "Type should be I32");
      assertEquals(42, value.asI32(), "Value should be 42");
    }

    @Test
    @DisplayName("i32 should handle negative values")
    void i32ShouldHandleNegativeValues() {
      final JniVariableValue value = JniVariableValue.i32(-100);
      assertEquals(-100, value.asI32(), "Value should be -100");
    }

    @Test
    @DisplayName("i32 should handle Integer.MAX_VALUE")
    void i32ShouldHandleMaxValue() {
      final JniVariableValue value = JniVariableValue.i32(Integer.MAX_VALUE);
      assertEquals(Integer.MAX_VALUE, value.asI32(), "Value should be Integer.MAX_VALUE");
    }

    @Test
    @DisplayName("i32 should handle Integer.MIN_VALUE")
    void i32ShouldHandleMinValue() {
      final JniVariableValue value = JniVariableValue.i32(Integer.MIN_VALUE);
      assertEquals(Integer.MIN_VALUE, value.asI32(), "Value should be Integer.MIN_VALUE");
    }

    @Test
    @DisplayName("asI32 should throw for non-I32 type")
    void asI32ShouldThrowForNonI32Type() {
      final JniVariableValue value = JniVariableValue.i64(42L);
      assertThrows(IllegalStateException.class, value::asI32, "Should throw for non-I32 type");
    }
  }

  @Nested
  @DisplayName("i64 Factory Tests")
  class I64FactoryTests {

    @Test
    @DisplayName("i64 should create I64 value")
    void i64ShouldCreateI64Value() {
      final JniVariableValue value = JniVariableValue.i64(1000L);

      assertEquals(JniVariableValue.ValueType.I64, value.getType(), "Type should be I64");
      assertEquals(1000L, value.asI64(), "Value should be 1000");
    }

    @Test
    @DisplayName("i64 should handle Long.MAX_VALUE")
    void i64ShouldHandleMaxValue() {
      final JniVariableValue value = JniVariableValue.i64(Long.MAX_VALUE);
      assertEquals(Long.MAX_VALUE, value.asI64(), "Value should be Long.MAX_VALUE");
    }

    @Test
    @DisplayName("i64 should handle Long.MIN_VALUE")
    void i64ShouldHandleMinValue() {
      final JniVariableValue value = JniVariableValue.i64(Long.MIN_VALUE);
      assertEquals(Long.MIN_VALUE, value.asI64(), "Value should be Long.MIN_VALUE");
    }

    @Test
    @DisplayName("asI64 should throw for non-I64 type")
    void asI64ShouldThrowForNonI64Type() {
      final JniVariableValue value = JniVariableValue.i32(42);
      assertThrows(IllegalStateException.class, value::asI64, "Should throw for non-I64 type");
    }
  }

  @Nested
  @DisplayName("f32 Factory Tests")
  class F32FactoryTests {

    @Test
    @DisplayName("f32 should create F32 value")
    void f32ShouldCreateF32Value() {
      final JniVariableValue value = JniVariableValue.f32(3.14f);

      assertEquals(JniVariableValue.ValueType.F32, value.getType(), "Type should be F32");
      assertEquals(3.14f, value.asF32(), 0.001f, "Value should be 3.14");
    }

    @Test
    @DisplayName("f32 should handle Float.MAX_VALUE")
    void f32ShouldHandleMaxValue() {
      final JniVariableValue value = JniVariableValue.f32(Float.MAX_VALUE);
      assertEquals(Float.MAX_VALUE, value.asF32(), "Value should be Float.MAX_VALUE");
    }

    @Test
    @DisplayName("f32 should handle Float.MIN_VALUE")
    void f32ShouldHandleMinValue() {
      final JniVariableValue value = JniVariableValue.f32(Float.MIN_VALUE);
      assertEquals(Float.MIN_VALUE, value.asF32(), "Value should be Float.MIN_VALUE");
    }

    @Test
    @DisplayName("f32 should handle NaN")
    void f32ShouldHandleNaN() {
      final JniVariableValue value = JniVariableValue.f32(Float.NaN);
      assertTrue(Float.isNaN(value.asF32()), "Value should be NaN");
    }

    @Test
    @DisplayName("f32 should handle Infinity")
    void f32ShouldHandleInfinity() {
      final JniVariableValue value = JniVariableValue.f32(Float.POSITIVE_INFINITY);
      assertTrue(Float.isInfinite(value.asF32()), "Value should be Infinity");
    }

    @Test
    @DisplayName("asF32 should throw for non-F32 type")
    void asF32ShouldThrowForNonF32Type() {
      final JniVariableValue value = JniVariableValue.i32(42);
      assertThrows(IllegalStateException.class, value::asF32, "Should throw for non-F32 type");
    }
  }

  @Nested
  @DisplayName("f64 Factory Tests")
  class F64FactoryTests {

    @Test
    @DisplayName("f64 should create F64 value")
    void f64ShouldCreateF64Value() {
      final JniVariableValue value = JniVariableValue.f64(2.718);

      assertEquals(JniVariableValue.ValueType.F64, value.getType(), "Type should be F64");
      assertEquals(2.718, value.asF64(), 0.001, "Value should be 2.718");
    }

    @Test
    @DisplayName("f64 should handle Double.MAX_VALUE")
    void f64ShouldHandleMaxValue() {
      final JniVariableValue value = JniVariableValue.f64(Double.MAX_VALUE);
      assertEquals(Double.MAX_VALUE, value.asF64(), "Value should be Double.MAX_VALUE");
    }

    @Test
    @DisplayName("asF64 should throw for non-F64 type")
    void asF64ShouldThrowForNonF64Type() {
      final JniVariableValue value = JniVariableValue.i32(42);
      assertThrows(IllegalStateException.class, value::asF64, "Should throw for non-F64 type");
    }
  }

  @Nested
  @DisplayName("v128 Factory Tests")
  class V128FactoryTests {

    @Test
    @DisplayName("v128 should create V128 value")
    void v128ShouldCreateV128Value() {
      final byte[] bytes = new byte[16];
      for (int i = 0; i < 16; i++) {
        bytes[i] = (byte) i;
      }
      final JniVariableValue value = JniVariableValue.v128(bytes);

      assertEquals(JniVariableValue.ValueType.V128, value.getType(), "Type should be V128");
      assertArrayEquals(bytes, value.asV128(), "Bytes should match");
    }

    @Test
    @DisplayName("v128 should throw for null bytes")
    void v128ShouldThrowForNullBytes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> JniVariableValue.v128(null),
          "Should throw for null bytes");
    }

    @Test
    @DisplayName("v128 should throw for wrong length bytes")
    void v128ShouldThrowForWrongLengthBytes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> JniVariableValue.v128(new byte[15]),
          "Should throw for 15 bytes");
      assertThrows(
          IllegalArgumentException.class,
          () -> JniVariableValue.v128(new byte[17]),
          "Should throw for 17 bytes");
    }

    @Test
    @DisplayName("v128 should make defensive copy of bytes")
    void v128ShouldMakeDefensiveCopyOfBytes() {
      final byte[] bytes = new byte[16];
      bytes[0] = 42;
      final JniVariableValue value = JniVariableValue.v128(bytes);
      bytes[0] = 100;

      assertEquals(42, value.asV128()[0], "Value should not change when original is modified");
    }

    @Test
    @DisplayName("asV128 should return defensive copy")
    void asV128ShouldReturnDefensiveCopy() {
      final byte[] bytes = new byte[16];
      bytes[0] = 42;
      final JniVariableValue value = JniVariableValue.v128(bytes);

      final byte[] retrieved = value.asV128();
      retrieved[0] = 100;

      assertEquals(
          42, value.asV128()[0], "Value should not change when retrieved copy is modified");
    }

    @Test
    @DisplayName("asV128 should throw for non-V128 type")
    void asV128ShouldThrowForNonV128Type() {
      final JniVariableValue value = JniVariableValue.i32(42);
      assertThrows(IllegalStateException.class, value::asV128, "Should throw for non-V128 type");
    }
  }

  @Nested
  @DisplayName("funcRef Factory Tests")
  class FuncRefFactoryTests {

    @Test
    @DisplayName("funcRef should create FUNCREF value")
    void funcRefShouldCreateFuncRefValue() {
      final JniVariableValue value = JniVariableValue.funcRef(5);

      assertEquals(JniVariableValue.ValueType.FUNCREF, value.getType(), "Type should be FUNCREF");
      assertEquals(5, value.asFuncRef().intValue(), "Value should be 5");
      assertFalse(value.isNull(), "Should not be null");
    }

    @Test
    @DisplayName("funcRef should handle null reference")
    void funcRefShouldHandleNullReference() {
      final JniVariableValue value = JniVariableValue.funcRef(null);

      assertEquals(JniVariableValue.ValueType.FUNCREF, value.getType(), "Type should be FUNCREF");
      assertNull(value.asFuncRef(), "Value should be null");
      assertTrue(value.isNull(), "Should be null");
    }

    @Test
    @DisplayName("asFuncRef should throw for non-FUNCREF type")
    void asFuncRefShouldThrowForNonFuncRefType() {
      final JniVariableValue value = JniVariableValue.i32(42);
      assertThrows(
          IllegalStateException.class, value::asFuncRef, "Should throw for non-FUNCREF type");
    }
  }

  @Nested
  @DisplayName("externRef Factory Tests")
  class ExternRefFactoryTests {

    @Test
    @DisplayName("externRef should create EXTERNREF value")
    void externRefShouldCreateExternRefValue() {
      final JniVariableValue value = JniVariableValue.externRef(0x12345678L);

      assertEquals(
          JniVariableValue.ValueType.EXTERNREF, value.getType(), "Type should be EXTERNREF");
      assertEquals(0x12345678L, value.asExternRef().longValue(), "Value should match");
      assertFalse(value.isNull(), "Should not be null");
    }

    @Test
    @DisplayName("externRef should handle null reference")
    void externRefShouldHandleNullReference() {
      final JniVariableValue value = JniVariableValue.externRef(null);

      assertEquals(
          JniVariableValue.ValueType.EXTERNREF, value.getType(), "Type should be EXTERNREF");
      assertNull(value.asExternRef(), "Value should be null");
      assertTrue(value.isNull(), "Should be null");
    }

    @Test
    @DisplayName("asExternRef should throw for non-EXTERNREF type")
    void asExternRefShouldThrowForNonExternRefType() {
      final JniVariableValue value = JniVariableValue.i32(42);
      assertThrows(
          IllegalStateException.class, value::asExternRef, "Should throw for non-EXTERNREF type");
    }
  }

  @Nested
  @DisplayName("memory Factory Tests")
  class MemoryFactoryTests {

    @Test
    @DisplayName("memory should create MEMORY value")
    void memoryShouldCreateMemoryValue() {
      final JniVariableValue value = JniVariableValue.memory(0x1000L, 0x2000L);

      assertEquals(JniVariableValue.ValueType.MEMORY, value.getType(), "Type should be MEMORY");
      assertEquals(0x1000L, value.getMemoryAddress(), "Address should match");
      assertEquals(0x2000L, value.getMemorySize(), "Size should match");
    }

    @Test
    @DisplayName("getMemoryAddress should throw for non-MEMORY type")
    void getMemoryAddressShouldThrowForNonMemoryType() {
      final JniVariableValue value = JniVariableValue.i32(42);
      assertThrows(
          IllegalStateException.class, value::getMemoryAddress, "Should throw for non-MEMORY type");
    }

    @Test
    @DisplayName("getMemorySize should throw for non-MEMORY type")
    void getMemorySizeShouldThrowForNonMemoryType() {
      final JniVariableValue value = JniVariableValue.i32(42);
      assertThrows(
          IllegalStateException.class, value::getMemorySize, "Should throw for non-MEMORY type");
    }
  }

  @Nested
  @DisplayName("complex Factory Tests")
  class ComplexFactoryTests {

    @Test
    @DisplayName("complex should create COMPLEX value")
    void complexShouldCreateComplexValue() {
      final String json = "{\"type\": \"struct\", \"fields\": []}";
      final JniVariableValue value = JniVariableValue.complex(json);

      assertEquals(JniVariableValue.ValueType.COMPLEX, value.getType(), "Type should be COMPLEX");
      assertEquals(json, value.asComplex(), "JSON should match");
    }

    @Test
    @DisplayName("asComplex should throw for non-COMPLEX type")
    void asComplexShouldThrowForNonComplexType() {
      final JniVariableValue value = JniVariableValue.i32(42);
      assertThrows(
          IllegalStateException.class, value::asComplex, "Should throw for non-COMPLEX type");
    }
  }

  @Nested
  @DisplayName("isNull Tests")
  class IsNullTests {

    @Test
    @DisplayName("isNull should return false for non-reference types")
    void isNullShouldReturnFalseForNonReferenceTypes() {
      assertFalse(JniVariableValue.i32(42).isNull(), "i32 should not be null");
      assertFalse(JniVariableValue.i64(42L).isNull(), "i64 should not be null");
      assertFalse(JniVariableValue.f32(3.14f).isNull(), "f32 should not be null");
      assertFalse(JniVariableValue.f64(2.718).isNull(), "f64 should not be null");
      assertFalse(JniVariableValue.v128(new byte[16]).isNull(), "v128 should not be null");
    }

    @Test
    @DisplayName("isNull should return true only for null references")
    void isNullShouldReturnTrueOnlyForNullReferences() {
      assertTrue(JniVariableValue.funcRef(null).isNull(), "null funcRef should be null");
      assertFalse(JniVariableValue.funcRef(0).isNull(), "non-null funcRef should not be null");
      assertTrue(JniVariableValue.externRef(null).isNull(), "null externRef should be null");
      assertFalse(JniVariableValue.externRef(0L).isNull(), "non-null externRef should not be null");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should format i32 correctly")
    void toStringShouldFormatI32Correctly() {
      assertEquals("i32(42)", JniVariableValue.i32(42).toString());
    }

    @Test
    @DisplayName("toString should format i64 correctly")
    void toStringShouldFormatI64Correctly() {
      assertEquals("i64(1000)", JniVariableValue.i64(1000L).toString());
    }

    @Test
    @DisplayName("toString should format null funcRef correctly")
    void toStringShouldFormatNullFuncRefCorrectly() {
      assertEquals("funcref(null)", JniVariableValue.funcRef(null).toString());
    }

    @Test
    @DisplayName("toString should format non-null funcRef correctly")
    void toStringShouldFormatNonNullFuncRefCorrectly() {
      assertEquals("funcref(5)", JniVariableValue.funcRef(5).toString());
    }

    @Test
    @DisplayName("toString should format memory correctly")
    void toStringShouldFormatMemoryCorrectly() {
      final String str = JniVariableValue.memory(0x1000L, 0x2000L).toString();
      assertTrue(str.contains("addr=4096"), "Should contain address");
      assertTrue(str.contains("size=8192"), "Should contain size");
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("equals should return true for same values")
    void equalsShouldReturnTrueForSameValues() {
      assertEquals(JniVariableValue.i32(42), JniVariableValue.i32(42));
      assertEquals(JniVariableValue.i64(1000L), JniVariableValue.i64(1000L));
      assertEquals(JniVariableValue.f32(3.14f), JniVariableValue.f32(3.14f));
    }

    @Test
    @DisplayName("equals should return false for different values")
    void equalsShouldReturnFalseForDifferentValues() {
      assertNotEquals(JniVariableValue.i32(42), JniVariableValue.i32(43));
      assertNotEquals(JniVariableValue.i32(42), JniVariableValue.i64(42L));
    }

    @Test
    @DisplayName("equals should handle v128 arrays correctly")
    void equalsShouldHandleV128ArraysCorrectly() {
      final byte[] bytes1 = new byte[16];
      final byte[] bytes2 = new byte[16];
      bytes1[0] = 42;
      bytes2[0] = 42;

      assertEquals(JniVariableValue.v128(bytes1), JniVariableValue.v128(bytes2));

      bytes2[0] = 43;
      assertNotEquals(JniVariableValue.v128(bytes1), JniVariableValue.v128(bytes2));
    }

    @Test
    @DisplayName("equals should handle memory arrays correctly")
    void equalsShouldHandleMemoryArraysCorrectly() {
      assertEquals(JniVariableValue.memory(100L, 200L), JniVariableValue.memory(100L, 200L));

      assertNotEquals(JniVariableValue.memory(100L, 200L), JniVariableValue.memory(100L, 300L));
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void hashCodeShouldBeConsistentWithEquals() {
      final JniVariableValue v1 = JniVariableValue.i32(42);
      final JniVariableValue v2 = JniVariableValue.i32(42);

      assertEquals(v1.hashCode(), v2.hashCode(), "Equal objects should have same hashCode");
    }
  }
}

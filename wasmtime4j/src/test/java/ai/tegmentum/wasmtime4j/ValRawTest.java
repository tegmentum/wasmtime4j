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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ValRaw}.
 *
 * <p>Verifies factory methods, accessors, type conversion round-trips, equality, hashCode, and
 * toString.
 */
@DisplayName("ValRaw Tests")
class ValRawTest {

  @Nested
  @DisplayName("I32 Factory Tests")
  class I32FactoryTests {

    @Test
    @DisplayName("should store and retrieve positive i32")
    void shouldStoreAndRetrievePositiveI32() {
      final ValRaw val = ValRaw.i32(42);
      assertEquals(42, val.asI32(), "asI32 should return 42");
    }

    @Test
    @DisplayName("should store and retrieve zero i32")
    void shouldStoreAndRetrieveZeroI32() {
      final ValRaw val = ValRaw.i32(0);
      assertEquals(0, val.asI32(), "asI32 should return 0");
    }

    @Test
    @DisplayName("should store and retrieve negative i32")
    void shouldStoreAndRetrieveNegativeI32() {
      final ValRaw val = ValRaw.i32(-1);
      assertEquals(-1, val.asI32(), "asI32 should return -1");
    }

    @Test
    @DisplayName("should store and retrieve Integer.MAX_VALUE")
    void shouldStoreAndRetrieveMaxI32() {
      final ValRaw val = ValRaw.i32(Integer.MAX_VALUE);
      assertEquals(Integer.MAX_VALUE, val.asI32(), "asI32 should return Integer.MAX_VALUE");
    }

    @Test
    @DisplayName("should store and retrieve Integer.MIN_VALUE")
    void shouldStoreAndRetrieveMinI32() {
      final ValRaw val = ValRaw.i32(Integer.MIN_VALUE);
      assertEquals(Integer.MIN_VALUE, val.asI32(), "asI32 should return Integer.MIN_VALUE");
    }

    @Test
    @DisplayName("high bits should be zero for i32")
    void highBitsShouldBeZeroForI32() {
      final ValRaw val = ValRaw.i32(42);
      assertEquals(0, val.getHighBits(), "High bits should be 0 for i32");
    }
  }

  @Nested
  @DisplayName("U32 Accessor Tests")
  class U32AccessorTests {

    @Test
    @DisplayName("should return unsigned value for -1")
    void shouldReturnUnsignedValueForNegativeOne() {
      final ValRaw val = ValRaw.i32(-1);
      assertEquals(0xFFFFFFFFL, val.asU32(), "asU32 of -1 should be 0xFFFFFFFF");
    }

    @Test
    @DisplayName("should return same value for positive i32")
    void shouldReturnSameValueForPositiveI32() {
      final ValRaw val = ValRaw.i32(42);
      assertEquals(42L, val.asU32(), "asU32 of 42 should be 42");
    }
  }

  @Nested
  @DisplayName("I64 Factory Tests")
  class I64FactoryTests {

    @Test
    @DisplayName("should store and retrieve positive i64")
    void shouldStoreAndRetrievePositiveI64() {
      final ValRaw val = ValRaw.i64(123456789L);
      assertEquals(123456789L, val.asI64(), "asI64 should return 123456789");
    }

    @Test
    @DisplayName("should store and retrieve Long.MAX_VALUE")
    void shouldStoreAndRetrieveMaxI64() {
      final ValRaw val = ValRaw.i64(Long.MAX_VALUE);
      assertEquals(Long.MAX_VALUE, val.asI64(), "asI64 should return Long.MAX_VALUE");
    }

    @Test
    @DisplayName("should store and retrieve Long.MIN_VALUE")
    void shouldStoreAndRetrieveMinI64() {
      final ValRaw val = ValRaw.i64(Long.MIN_VALUE);
      assertEquals(Long.MIN_VALUE, val.asI64(), "asI64 should return Long.MIN_VALUE");
    }
  }

  @Nested
  @DisplayName("F32 Factory Tests")
  class F32FactoryTests {

    @Test
    @DisplayName("should store and retrieve positive f32")
    void shouldStoreAndRetrievePositiveF32() {
      final ValRaw val = ValRaw.f32(3.14f);
      assertEquals(3.14f, val.asF32(), "asF32 should return 3.14f");
    }

    @Test
    @DisplayName("should store and retrieve zero f32")
    void shouldStoreAndRetrieveZeroF32() {
      final ValRaw val = ValRaw.f32(0.0f);
      assertEquals(0.0f, val.asF32(), "asF32 should return 0.0f");
    }

    @Test
    @DisplayName("should store and retrieve negative f32")
    void shouldStoreAndRetrieveNegativeF32() {
      final ValRaw val = ValRaw.f32(-1.5f);
      assertEquals(-1.5f, val.asF32(), "asF32 should return -1.5f");
    }

    @Test
    @DisplayName("should store and retrieve Float.NaN")
    void shouldStoreAndRetrieveNaN() {
      final ValRaw val = ValRaw.f32(Float.NaN);
      assertTrue(Float.isNaN(val.asF32()), "asF32 should return NaN");
    }

    @Test
    @DisplayName("should store and retrieve Float.POSITIVE_INFINITY")
    void shouldStoreAndRetrievePositiveInfinity() {
      final ValRaw val = ValRaw.f32(Float.POSITIVE_INFINITY);
      assertEquals(
          Float.POSITIVE_INFINITY, val.asF32(), "asF32 should return POSITIVE_INFINITY");
    }
  }

  @Nested
  @DisplayName("F64 Factory Tests")
  class F64FactoryTests {

    @Test
    @DisplayName("should store and retrieve positive f64")
    void shouldStoreAndRetrievePositiveF64() {
      final ValRaw val = ValRaw.f64(3.14159265358979);
      assertEquals(3.14159265358979, val.asF64(), "asF64 should return pi");
    }

    @Test
    @DisplayName("should store and retrieve Double.NaN")
    void shouldStoreAndRetrieveNaN() {
      final ValRaw val = ValRaw.f64(Double.NaN);
      assertTrue(Double.isNaN(val.asF64()), "asF64 should return NaN");
    }

    @Test
    @DisplayName("should store and retrieve Double.MAX_VALUE")
    void shouldStoreAndRetrieveMaxF64() {
      final ValRaw val = ValRaw.f64(Double.MAX_VALUE);
      assertEquals(Double.MAX_VALUE, val.asF64(), "asF64 should return Double.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("V128 Factory Tests")
  class V128FactoryTests {

    @Test
    @DisplayName("should store and retrieve v128 bits")
    void shouldStoreAndRetrieveV128Bits() {
      final ValRaw val = ValRaw.v128(0x123456789ABCDEF0L, 0xFEDCBA9876543210L);
      assertEquals(0x123456789ABCDEF0L, val.asV128Low(), "Low bits should match");
      assertEquals(0xFEDCBA9876543210L, val.asV128High(), "High bits should match");
    }

    @Test
    @DisplayName("should store and retrieve zero v128")
    void shouldStoreAndRetrieveZeroV128() {
      final ValRaw val = ValRaw.v128(0, 0);
      assertEquals(0, val.asV128Low(), "Low bits should be 0");
      assertEquals(0, val.asV128High(), "High bits should be 0");
    }
  }

  @Nested
  @DisplayName("Funcref Factory Tests")
  class FuncrefFactoryTests {

    @Test
    @DisplayName("should store and retrieve funcref index")
    void shouldStoreAndRetrieveFuncrefIndex() {
      final ValRaw val = ValRaw.funcref(5);
      assertEquals(5, val.asFuncrefIndex(), "asFuncrefIndex should return 5");
    }

    @Test
    @DisplayName("null funcref should have index -1")
    void nullFuncrefShouldHaveIndexNegativeOne() {
      final ValRaw val = ValRaw.nullFuncref();
      assertEquals(-1, val.asFuncrefIndex(), "Null funcref should have index -1");
    }
  }

  @Nested
  @DisplayName("Externref Factory Tests")
  class ExternrefFactoryTests {

    @Test
    @DisplayName("should store and retrieve externref pointer")
    void shouldStoreAndRetrieveExternrefPtr() {
      final ValRaw val = ValRaw.externref(12345L);
      assertEquals(12345L, val.asExternrefPtr(), "asExternrefPtr should return 12345");
    }

    @Test
    @DisplayName("null externref should have ptr 0")
    void nullExternrefShouldHavePtrZero() {
      final ValRaw val = ValRaw.nullExternref();
      assertEquals(0, val.asExternrefPtr(), "Null externref should have ptr 0");
    }
  }

  @Nested
  @DisplayName("Anyref Factory Tests")
  class AnyrefFactoryTests {

    @Test
    @DisplayName("should store and retrieve anyref pointer")
    void shouldStoreAndRetrieveAnyrefPtr() {
      final ValRaw val = ValRaw.anyref(99999L);
      assertEquals(99999L, val.asAnyrefPtr(), "asAnyrefPtr should return 99999");
    }
  }

  @Nested
  @DisplayName("FromRawBits Tests")
  class FromRawBitsTests {

    @Test
    @DisplayName("should create ValRaw from raw bits")
    void shouldCreateFromRawBits() {
      final ValRaw val = ValRaw.fromRawBits(0xABCDL, 0x1234L);
      assertEquals(0xABCDL, val.getLowBits(), "Low bits should match");
      assertEquals(0x1234L, val.getHighBits(), "High bits should match");
    }
  }

  @Nested
  @DisplayName("ToWasmValue Tests")
  class ToWasmValueTests {

    @Test
    @DisplayName("should convert i32 to WasmValue")
    void shouldConvertI32ToWasmValue() {
      final ValRaw val = ValRaw.i32(42);
      final WasmValue wasm = val.toWasmValue(WasmValueType.I32);
      assertEquals(42, wasm.asInt(), "WasmValue should contain 42");
    }

    @Test
    @DisplayName("should convert i64 to WasmValue")
    void shouldConvertI64ToWasmValue() {
      final ValRaw val = ValRaw.i64(123456789L);
      final WasmValue wasm = val.toWasmValue(WasmValueType.I64);
      assertEquals(123456789L, wasm.asLong(), "WasmValue should contain 123456789");
    }

    @Test
    @DisplayName("should convert f32 to WasmValue")
    void shouldConvertF32ToWasmValue() {
      final ValRaw val = ValRaw.f32(3.14f);
      final WasmValue wasm = val.toWasmValue(WasmValueType.F32);
      assertEquals(3.14f, wasm.asFloat(), "WasmValue should contain 3.14f");
    }

    @Test
    @DisplayName("should convert f64 to WasmValue")
    void shouldConvertF64ToWasmValue() {
      final ValRaw val = ValRaw.f64(2.71828);
      final WasmValue wasm = val.toWasmValue(WasmValueType.F64);
      assertEquals(2.71828, wasm.asDouble(), "WasmValue should contain 2.71828");
    }

    @Test
    @DisplayName("should throw NullPointerException for null type")
    void shouldThrowForNullType() {
      final ValRaw val = ValRaw.i32(42);
      assertThrows(
          NullPointerException.class,
          () -> val.toWasmValue(null),
          "toWasmValue(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("FromWasmValue Tests")
  class FromWasmValueTests {

    @Test
    @DisplayName("should convert WasmValue i32 to ValRaw")
    void shouldConvertWasmValueI32ToValRaw() {
      final WasmValue wasm = WasmValue.i32(42);
      final ValRaw val = ValRaw.fromWasmValue(wasm);
      assertEquals(42, val.asI32(), "ValRaw should contain 42");
    }

    @Test
    @DisplayName("should convert WasmValue i64 to ValRaw")
    void shouldConvertWasmValueI64ToValRaw() {
      final WasmValue wasm = WasmValue.i64(123456789L);
      final ValRaw val = ValRaw.fromWasmValue(wasm);
      assertEquals(123456789L, val.asI64(), "ValRaw should contain 123456789");
    }

    @Test
    @DisplayName("should convert WasmValue f32 to ValRaw")
    void shouldConvertWasmValueF32ToValRaw() {
      final WasmValue wasm = WasmValue.f32(3.14f);
      final ValRaw val = ValRaw.fromWasmValue(wasm);
      assertEquals(3.14f, val.asF32(), "ValRaw should contain 3.14f");
    }

    @Test
    @DisplayName("should convert WasmValue f64 to ValRaw")
    void shouldConvertWasmValueF64ToValRaw() {
      final WasmValue wasm = WasmValue.f64(2.71828);
      final ValRaw val = ValRaw.fromWasmValue(wasm);
      assertEquals(2.71828, val.asF64(), "ValRaw should contain 2.71828");
    }

    @Test
    @DisplayName("should throw NullPointerException for null value")
    void shouldThrowForNullValue() {
      assertThrows(
          NullPointerException.class,
          () -> ValRaw.fromWasmValue(null),
          "fromWasmValue(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("equal ValRaws should be equal")
    void equalValRawsShouldBeEqual() {
      final ValRaw val1 = ValRaw.i32(42);
      final ValRaw val2 = ValRaw.i32(42);
      assertEquals(val1, val2, "Same i32 values should be equal");
    }

    @Test
    @DisplayName("different ValRaws should not be equal")
    void differentValRawsShouldNotBeEqual() {
      final ValRaw val1 = ValRaw.i32(42);
      final ValRaw val2 = ValRaw.i32(43);
      assertNotEquals(val1, val2, "Different i32 values should not be equal");
    }

    @Test
    @DisplayName("ValRaw should not equal null")
    void shouldNotEqualNull() {
      final ValRaw val = ValRaw.i32(42);
      assertNotEquals(null, val, "ValRaw should not equal null");
    }

    @Test
    @DisplayName("ValRaw should not equal different type")
    void shouldNotEqualDifferentType() {
      final ValRaw val = ValRaw.i32(42);
      assertNotEquals("42", val, "ValRaw should not equal String");
    }

    @Test
    @DisplayName("v128 equality should consider both low and high bits")
    void v128EqualityShouldConsiderBothBits() {
      final ValRaw val1 = ValRaw.v128(1, 2);
      final ValRaw val2 = ValRaw.v128(1, 2);
      final ValRaw val3 = ValRaw.v128(1, 3);
      assertEquals(val1, val2, "Same v128 should be equal");
      assertNotEquals(val1, val3, "Different high bits should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("equal ValRaws should have same hashCode")
    void equalValRawsShouldHaveSameHashCode() {
      final ValRaw val1 = ValRaw.i32(42);
      final ValRaw val2 = ValRaw.i32(42);
      assertEquals(val1.hashCode(), val2.hashCode(), "Equal ValRaws should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final ValRaw val = ValRaw.i64(12345L);
      final int hash1 = val.hashCode();
      final int hash2 = val.hashCode();
      assertEquals(hash1, hash2, "hashCode should be consistent");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain hex representation of low bits")
    void toStringShouldContainLowBits() {
      final ValRaw val = ValRaw.i32(255);
      final String str = val.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("ValRaw"), "toString should contain class name");
      assertTrue(str.contains("low=0x"), "toString should contain low= prefix");
    }

    @Test
    @DisplayName("toString should contain hex representation of high bits")
    void toStringShouldContainHighBits() {
      final ValRaw val = ValRaw.v128(1, 2);
      final String str = val.toString();
      assertTrue(str.contains("high=0x"), "toString should contain high= prefix");
    }
  }
}

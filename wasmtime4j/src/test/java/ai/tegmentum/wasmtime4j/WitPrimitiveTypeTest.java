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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitPrimitiveType} enum.
 *
 * <p>WitPrimitiveType defines the fundamental scalar types supported by the WebAssembly Component
 * Model type system.
 */
@DisplayName("WitPrimitiveType Tests")
class WitPrimitiveTypeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WitPrimitiveType.class.isEnum(), "WitPrimitiveType should be an enum");
    }

    @Test
    @DisplayName("should have 13 primitive types")
    void shouldHave13PrimitiveTypes() {
      final var values = WitPrimitiveType.values();
      assertEquals(13, values.length);
    }

    @Test
    @DisplayName("should have all expected types")
    void shouldHaveAllExpectedTypes() {
      assertNotNull(WitPrimitiveType.BOOL);
      assertNotNull(WitPrimitiveType.S8);
      assertNotNull(WitPrimitiveType.U8);
      assertNotNull(WitPrimitiveType.S16);
      assertNotNull(WitPrimitiveType.U16);
      assertNotNull(WitPrimitiveType.S32);
      assertNotNull(WitPrimitiveType.U32);
      assertNotNull(WitPrimitiveType.S64);
      assertNotNull(WitPrimitiveType.U64);
      assertNotNull(WitPrimitiveType.FLOAT32);
      assertNotNull(WitPrimitiveType.FLOAT64);
      assertNotNull(WitPrimitiveType.CHAR);
      assertNotNull(WitPrimitiveType.STRING);
    }
  }

  @Nested
  @DisplayName("Size Tests")
  class SizeTests {

    @Test
    @DisplayName("BOOL should be 1 byte")
    void boolShouldBe1Byte() {
      assertEquals(1, WitPrimitiveType.BOOL.getSizeBytes());
    }

    @Test
    @DisplayName("8-bit integers should be 1 byte")
    void int8ShouldBe1Byte() {
      assertEquals(1, WitPrimitiveType.S8.getSizeBytes());
      assertEquals(1, WitPrimitiveType.U8.getSizeBytes());
    }

    @Test
    @DisplayName("16-bit integers should be 2 bytes")
    void int16ShouldBe2Bytes() {
      assertEquals(2, WitPrimitiveType.S16.getSizeBytes());
      assertEquals(2, WitPrimitiveType.U16.getSizeBytes());
    }

    @Test
    @DisplayName("32-bit integers should be 4 bytes")
    void int32ShouldBe4Bytes() {
      assertEquals(4, WitPrimitiveType.S32.getSizeBytes());
      assertEquals(4, WitPrimitiveType.U32.getSizeBytes());
    }

    @Test
    @DisplayName("64-bit integers should be 8 bytes")
    void int64ShouldBe8Bytes() {
      assertEquals(8, WitPrimitiveType.S64.getSizeBytes());
      assertEquals(8, WitPrimitiveType.U64.getSizeBytes());
    }

    @Test
    @DisplayName("FLOAT32 should be 4 bytes")
    void float32ShouldBe4Bytes() {
      assertEquals(4, WitPrimitiveType.FLOAT32.getSizeBytes());
    }

    @Test
    @DisplayName("FLOAT64 should be 8 bytes")
    void float64ShouldBe8Bytes() {
      assertEquals(8, WitPrimitiveType.FLOAT64.getSizeBytes());
    }

    @Test
    @DisplayName("CHAR should be 4 bytes")
    void charShouldBe4Bytes() {
      assertEquals(4, WitPrimitiveType.CHAR.getSizeBytes());
    }

    @Test
    @DisplayName("STRING should have variable size")
    void stringShouldHaveVariableSize() {
      assertEquals(-1, WitPrimitiveType.STRING.getSizeBytes());
      assertTrue(WitPrimitiveType.STRING.isVariableSize());
    }
  }

  @Nested
  @DisplayName("Type Classification Tests")
  class TypeClassificationTests {

    @Test
    @DisplayName("integer types should be classified correctly")
    void integerTypesShouldBeClassifiedCorrectly() {
      assertTrue(WitPrimitiveType.S8.isInteger());
      assertTrue(WitPrimitiveType.U8.isInteger());
      assertTrue(WitPrimitiveType.S16.isInteger());
      assertTrue(WitPrimitiveType.U16.isInteger());
      assertTrue(WitPrimitiveType.S32.isInteger());
      assertTrue(WitPrimitiveType.U32.isInteger());
      assertTrue(WitPrimitiveType.S64.isInteger());
      assertTrue(WitPrimitiveType.U64.isInteger());

      assertFalse(WitPrimitiveType.BOOL.isInteger());
      assertFalse(WitPrimitiveType.FLOAT32.isInteger());
      assertFalse(WitPrimitiveType.FLOAT64.isInteger());
      assertFalse(WitPrimitiveType.CHAR.isInteger());
      assertFalse(WitPrimitiveType.STRING.isInteger());
    }

    @Test
    @DisplayName("floating point types should be classified correctly")
    void floatingPointTypesShouldBeClassifiedCorrectly() {
      assertTrue(WitPrimitiveType.FLOAT32.isFloatingPoint());
      assertTrue(WitPrimitiveType.FLOAT64.isFloatingPoint());

      assertFalse(WitPrimitiveType.S32.isFloatingPoint());
      assertFalse(WitPrimitiveType.U32.isFloatingPoint());
      assertFalse(WitPrimitiveType.BOOL.isFloatingPoint());
      assertFalse(WitPrimitiveType.STRING.isFloatingPoint());
    }

    @Test
    @DisplayName("signed integers should be classified correctly")
    void signedIntegersShouldBeClassifiedCorrectly() {
      assertTrue(WitPrimitiveType.S8.isSignedInteger());
      assertTrue(WitPrimitiveType.S16.isSignedInteger());
      assertTrue(WitPrimitiveType.S32.isSignedInteger());
      assertTrue(WitPrimitiveType.S64.isSignedInteger());

      assertFalse(WitPrimitiveType.U8.isSignedInteger());
      assertFalse(WitPrimitiveType.U16.isSignedInteger());
      assertFalse(WitPrimitiveType.U32.isSignedInteger());
      assertFalse(WitPrimitiveType.U64.isSignedInteger());
    }

    @Test
    @DisplayName("unsigned integers should be classified correctly")
    void unsignedIntegersShouldBeClassifiedCorrectly() {
      assertTrue(WitPrimitiveType.U8.isUnsignedInteger());
      assertTrue(WitPrimitiveType.U16.isUnsignedInteger());
      assertTrue(WitPrimitiveType.U32.isUnsignedInteger());
      assertTrue(WitPrimitiveType.U64.isUnsignedInteger());

      assertFalse(WitPrimitiveType.S8.isUnsignedInteger());
      assertFalse(WitPrimitiveType.S16.isUnsignedInteger());
      assertFalse(WitPrimitiveType.S32.isUnsignedInteger());
      assertFalse(WitPrimitiveType.S64.isUnsignedInteger());
    }
  }

  @Nested
  @DisplayName("Java Type Mapping Tests")
  class JavaTypeMappingTests {

    @Test
    @DisplayName("BOOL should map to boolean")
    void boolShouldMapToBoolean() {
      assertEquals(boolean.class, WitPrimitiveType.BOOL.getJavaType());
    }

    @Test
    @DisplayName("8-bit types should map to byte")
    void int8ShouldMapToByte() {
      assertEquals(byte.class, WitPrimitiveType.S8.getJavaType());
      assertEquals(byte.class, WitPrimitiveType.U8.getJavaType());
    }

    @Test
    @DisplayName("16-bit types should map to short")
    void int16ShouldMapToShort() {
      assertEquals(short.class, WitPrimitiveType.S16.getJavaType());
      assertEquals(short.class, WitPrimitiveType.U16.getJavaType());
    }

    @Test
    @DisplayName("32-bit integers should map to int")
    void int32ShouldMapToInt() {
      assertEquals(int.class, WitPrimitiveType.S32.getJavaType());
      assertEquals(int.class, WitPrimitiveType.U32.getJavaType());
    }

    @Test
    @DisplayName("64-bit integers should map to long")
    void int64ShouldMapToLong() {
      assertEquals(long.class, WitPrimitiveType.S64.getJavaType());
      assertEquals(long.class, WitPrimitiveType.U64.getJavaType());
    }

    @Test
    @DisplayName("FLOAT32 should map to float")
    void float32ShouldMapToFloat() {
      assertEquals(float.class, WitPrimitiveType.FLOAT32.getJavaType());
    }

    @Test
    @DisplayName("FLOAT64 should map to double")
    void float64ShouldMapToDouble() {
      assertEquals(double.class, WitPrimitiveType.FLOAT64.getJavaType());
    }

    @Test
    @DisplayName("CHAR should map to char")
    void charShouldMapToChar() {
      assertEquals(char.class, WitPrimitiveType.CHAR.getJavaType());
    }

    @Test
    @DisplayName("STRING should map to String")
    void stringShouldMapToString() {
      assertEquals(String.class, WitPrimitiveType.STRING.getJavaType());
    }
  }

  @Nested
  @DisplayName("fromString Tests")
  class FromStringTests {

    @Test
    @DisplayName("should parse bool variants")
    void shouldParseBoolVariants() {
      assertEquals(WitPrimitiveType.BOOL, WitPrimitiveType.fromString("bool"));
      assertEquals(WitPrimitiveType.BOOL, WitPrimitiveType.fromString("BOOL"));
      assertEquals(WitPrimitiveType.BOOL, WitPrimitiveType.fromString("boolean"));
    }

    @Test
    @DisplayName("should parse signed integer variants")
    void shouldParseSignedIntegerVariants() {
      assertEquals(WitPrimitiveType.S8, WitPrimitiveType.fromString("s8"));
      assertEquals(WitPrimitiveType.S8, WitPrimitiveType.fromString("i8"));
      assertEquals(WitPrimitiveType.S16, WitPrimitiveType.fromString("s16"));
      assertEquals(WitPrimitiveType.S16, WitPrimitiveType.fromString("i16"));
      assertEquals(WitPrimitiveType.S32, WitPrimitiveType.fromString("s32"));
      assertEquals(WitPrimitiveType.S32, WitPrimitiveType.fromString("i32"));
      assertEquals(WitPrimitiveType.S64, WitPrimitiveType.fromString("s64"));
      assertEquals(WitPrimitiveType.S64, WitPrimitiveType.fromString("i64"));
    }

    @Test
    @DisplayName("should parse unsigned integer types")
    void shouldParseUnsignedIntegerTypes() {
      assertEquals(WitPrimitiveType.U8, WitPrimitiveType.fromString("u8"));
      assertEquals(WitPrimitiveType.U16, WitPrimitiveType.fromString("u16"));
      assertEquals(WitPrimitiveType.U32, WitPrimitiveType.fromString("u32"));
      assertEquals(WitPrimitiveType.U64, WitPrimitiveType.fromString("u64"));
    }

    @Test
    @DisplayName("should parse floating point variants")
    void shouldParseFloatingPointVariants() {
      assertEquals(WitPrimitiveType.FLOAT32, WitPrimitiveType.fromString("f32"));
      assertEquals(WitPrimitiveType.FLOAT32, WitPrimitiveType.fromString("float32"));
      assertEquals(WitPrimitiveType.FLOAT64, WitPrimitiveType.fromString("f64"));
      assertEquals(WitPrimitiveType.FLOAT64, WitPrimitiveType.fromString("float64"));
    }

    @Test
    @DisplayName("should parse char and string")
    void shouldParseCharAndString() {
      assertEquals(WitPrimitiveType.CHAR, WitPrimitiveType.fromString("char"));
      assertEquals(WitPrimitiveType.STRING, WitPrimitiveType.fromString("string"));
    }

    @Test
    @DisplayName("should throw for null input")
    void shouldThrowForNullInput() {
      assertThrows(IllegalArgumentException.class, () -> WitPrimitiveType.fromString(null));
    }

    @Test
    @DisplayName("should throw for empty input")
    void shouldThrowForEmptyInput() {
      assertThrows(IllegalArgumentException.class, () -> WitPrimitiveType.fromString(""));
    }

    @Test
    @DisplayName("should throw for unknown type")
    void shouldThrowForUnknownType() {
      assertThrows(IllegalArgumentException.class, () -> WitPrimitiveType.fromString("unknown"));
    }

    @Test
    @DisplayName("should be case insensitive")
    void shouldBeCaseInsensitive() {
      assertEquals(WitPrimitiveType.S32, WitPrimitiveType.fromString("S32"));
      assertEquals(WitPrimitiveType.U32, WitPrimitiveType.fromString("U32"));
      assertEquals(WitPrimitiveType.STRING, WitPrimitiveType.fromString("STRING"));
    }

    @Test
    @DisplayName("should handle whitespace")
    void shouldHandleWhitespace() {
      assertEquals(WitPrimitiveType.S32, WitPrimitiveType.fromString("  s32  "));
    }
  }

  @Nested
  @DisplayName("getWitTypeName Tests")
  class GetWitTypeNameTests {

    @Test
    @DisplayName("should return lowercase type names")
    void shouldReturnLowercaseTypeNames() {
      assertEquals("bool", WitPrimitiveType.BOOL.getWitTypeName());
      assertEquals("s8", WitPrimitiveType.S8.getWitTypeName());
      assertEquals("u8", WitPrimitiveType.U8.getWitTypeName());
      assertEquals("s16", WitPrimitiveType.S16.getWitTypeName());
      assertEquals("u16", WitPrimitiveType.U16.getWitTypeName());
      assertEquals("s32", WitPrimitiveType.S32.getWitTypeName());
      assertEquals("u32", WitPrimitiveType.U32.getWitTypeName());
      assertEquals("s64", WitPrimitiveType.S64.getWitTypeName());
      assertEquals("u64", WitPrimitiveType.U64.getWitTypeName());
      assertEquals("float32", WitPrimitiveType.FLOAT32.getWitTypeName());
      assertEquals("float64", WitPrimitiveType.FLOAT64.getWitTypeName());
      assertEquals("char", WitPrimitiveType.CHAR.getWitTypeName());
      assertEquals("string", WitPrimitiveType.STRING.getWitTypeName());
    }
  }

  @Nested
  @DisplayName("Variable Size Tests")
  class VariableSizeTests {

    @Test
    @DisplayName("STRING should be variable size")
    void stringShouldBeVariableSize() {
      assertTrue(WitPrimitiveType.STRING.isVariableSize());
    }

    @Test
    @DisplayName("fixed size types should not be variable size")
    void fixedSizeTypesShouldNotBeVariableSize() {
      assertFalse(WitPrimitiveType.BOOL.isVariableSize());
      assertFalse(WitPrimitiveType.S32.isVariableSize());
      assertFalse(WitPrimitiveType.U64.isVariableSize());
      assertFalse(WitPrimitiveType.FLOAT64.isVariableSize());
      assertFalse(WitPrimitiveType.CHAR.isVariableSize());
    }
  }
}

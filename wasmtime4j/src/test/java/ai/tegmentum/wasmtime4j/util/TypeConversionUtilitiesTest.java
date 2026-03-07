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
package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link TypeConversionUtilities}.
 *
 * <p>These tests verify the shared type conversion utilities used by both JNI and Panama
 * implementations.
 */
@DisplayName("TypeConversionUtilities Tests")
class TypeConversionUtilitiesTest {

  @Nested
  @DisplayName("V128_SIZE_BYTES constant")
  class V128SizeBytesTests {

    @Test
    @DisplayName("V128_SIZE_BYTES should be 16")
    void v128SizeBytesShouldBe16() {
      assertEquals(16, TypeConversionUtilities.V128_SIZE_BYTES);
    }
  }

  @Nested
  @DisplayName("copyTypes Tests")
  class CopyTypesTests {

    @Test
    @DisplayName("copyTypes should create defensive copy")
    void copyTypesShouldCreateDefensiveCopy() {
      final WasmValueType[] original = {WasmValueType.I32, WasmValueType.I64};
      final WasmValueType[] copy = TypeConversionUtilities.copyTypes(original);

      assertArrayEquals(new WasmValueType[] {WasmValueType.I32, WasmValueType.I64}, copy);
      assertNotSame(original, copy);
    }

    @Test
    @DisplayName("copyTypes should return empty array for null input")
    void copyTypesShouldReturnEmptyForNull() {
      final WasmValueType[] result = TypeConversionUtilities.copyTypes(null);
      assertEquals(0, result.length);
    }

    @Test
    @DisplayName("copyTypes should return empty array for empty input")
    void copyTypesShouldReturnEmptyForEmpty() {
      final WasmValueType[] result = TypeConversionUtilities.copyTypes(new WasmValueType[0]);
      assertEquals(0, result.length);
    }
  }

  @Nested
  @DisplayName("validateV128Size Tests")
  class ValidateV128SizeTests {

    @Test
    @DisplayName("validateV128Size should pass for 16-byte array")
    void validateV128SizeShouldPassFor16Bytes() {
      final byte[] bytes = new byte[16];
      assertDoesNotThrow(() -> TypeConversionUtilities.validateV128Size(bytes));
    }

    @Test
    @DisplayName("validateV128Size should throw for null array")
    void validateV128SizeShouldThrowForNull() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> TypeConversionUtilities.validateV128Size(null));
      assertTrue(exception.getMessage().contains("null"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 8, 15, 17, 32})
    @DisplayName("validateV128Size should throw for wrong size")
    void validateV128SizeShouldThrowForWrongSize(final int size) {
      final byte[] bytes = new byte[size];
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TypeConversionUtilities.validateV128Size(bytes));
      assertTrue(exception.getMessage().contains("16"));
      assertTrue(exception.getMessage().contains(String.valueOf(size)));
    }
  }

  @Nested
  @DisplayName("validateParameterTypes Tests")
  class ValidateParameterTypesTests {

    @Test
    @DisplayName("validateParameterTypes should pass for matching types")
    void validateParameterTypesShouldPassForMatchingTypes() {
      final WasmValue[] params = {WasmValue.i32(1), WasmValue.i64(2L)};
      final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.I64};

      assertDoesNotThrow(
          () -> TypeConversionUtilities.validateParameterTypes(params, expectedTypes));
    }

    @Test
    @DisplayName("validateParameterTypes should pass for empty arrays")
    void validateParameterTypesShouldPassForEmptyArrays() {
      assertDoesNotThrow(
          () ->
              TypeConversionUtilities.validateParameterTypes(
                  new WasmValue[0], new WasmValueType[0]));
    }

    @Test
    @DisplayName("validateParameterTypes should throw for null params")
    void validateParameterTypesShouldThrowForNullParams() {
      final WasmValueType[] expectedTypes = {WasmValueType.I32};
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TypeConversionUtilities.validateParameterTypes(null, expectedTypes));
      assertTrue(exception.getMessage().contains("params"));
    }

    @Test
    @DisplayName("validateParameterTypes should throw for null expectedTypes")
    void validateParameterTypesShouldThrowForNullExpectedTypes() {
      final WasmValue[] params = {WasmValue.i32(1)};
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TypeConversionUtilities.validateParameterTypes(params, null));
      assertTrue(exception.getMessage().contains("expectedTypes"));
    }

    @Test
    @DisplayName("validateParameterTypes should throw for count mismatch")
    void validateParameterTypesShouldThrowForCountMismatch() {
      final WasmValue[] params = {WasmValue.i32(1)};
      final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.I64};

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TypeConversionUtilities.validateParameterTypes(params, expectedTypes));
      assertTrue(exception.getMessage().contains("count mismatch"));
      assertTrue(exception.getMessage().contains("1"));
      assertTrue(exception.getMessage().contains("2"));
    }

    @Test
    @DisplayName("validateParameterTypes should throw for type mismatch")
    void validateParameterTypesShouldThrowForTypeMismatch() {
      final WasmValue[] params = {WasmValue.i32(1)};
      final WasmValueType[] expectedTypes = {WasmValueType.I64};

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TypeConversionUtilities.validateParameterTypes(params, expectedTypes));
      assertTrue(exception.getMessage().contains("type mismatch"));
      assertTrue(exception.getMessage().contains("index 0"));
    }

    @Test
    @DisplayName("validateParameterTypes should throw for null parameter element")
    void validateParameterTypesShouldThrowForNullParameter() {
      final WasmValue[] params = {null};
      final WasmValueType[] expectedTypes = {WasmValueType.I32};

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TypeConversionUtilities.validateParameterTypes(params, expectedTypes));
      assertTrue(exception.getMessage().contains("index 0"));
      assertTrue(exception.getMessage().contains("null"));
    }
  }

  @Nested
  @DisplayName("getValueSize Tests")
  class GetValueSizeTests {

    @Test
    @DisplayName("getValueSize should return 4 for I32")
    void getValueSizeShouldReturn4ForI32() {
      assertEquals(4, TypeConversionUtilities.getValueSize(WasmValueType.I32));
    }

    @Test
    @DisplayName("getValueSize should return 4 for F32")
    void getValueSizeShouldReturn4ForF32() {
      assertEquals(4, TypeConversionUtilities.getValueSize(WasmValueType.F32));
    }

    @Test
    @DisplayName("getValueSize should return 8 for I64")
    void getValueSizeShouldReturn8ForI64() {
      assertEquals(8, TypeConversionUtilities.getValueSize(WasmValueType.I64));
    }

    @Test
    @DisplayName("getValueSize should return 8 for F64")
    void getValueSizeShouldReturn8ForF64() {
      assertEquals(8, TypeConversionUtilities.getValueSize(WasmValueType.F64));
    }

    @Test
    @DisplayName("getValueSize should return 8 for FUNCREF")
    void getValueSizeShouldReturn8ForFuncref() {
      assertEquals(8, TypeConversionUtilities.getValueSize(WasmValueType.FUNCREF));
    }

    @Test
    @DisplayName("getValueSize should return 8 for EXTERNREF")
    void getValueSizeShouldReturn8ForExternref() {
      assertEquals(8, TypeConversionUtilities.getValueSize(WasmValueType.EXTERNREF));
    }

    @Test
    @DisplayName("getValueSize should return 16 for V128")
    void getValueSizeShouldReturn16ForV128() {
      assertEquals(16, TypeConversionUtilities.getValueSize(WasmValueType.V128));
    }

    @Test
    @DisplayName("getValueSize should throw for null")
    void getValueSizeShouldThrowForNull() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> TypeConversionUtilities.getValueSize(null));
      assertTrue(exception.getMessage().contains("valueType"));
    }
  }

  @Nested
  @DisplayName("writeInt and readInt Tests")
  class WriteReadIntTests {

    @Test
    @DisplayName("writeInt and readInt should round-trip positive value")
    void writeReadIntShouldRoundTripPositive() {
      final byte[] buffer = new byte[4];
      TypeConversionUtilities.writeInt(buffer, 0, 0x12345678);
      assertEquals(0x12345678, TypeConversionUtilities.readInt(buffer, 0));
    }

    @Test
    @DisplayName("writeInt and readInt should round-trip negative value")
    void writeReadIntShouldRoundTripNegative() {
      final byte[] buffer = new byte[4];
      TypeConversionUtilities.writeInt(buffer, 0, -12345);
      assertEquals(-12345, TypeConversionUtilities.readInt(buffer, 0));
    }

    @Test
    @DisplayName("writeInt and readInt should round-trip zero")
    void writeReadIntShouldRoundTripZero() {
      final byte[] buffer = new byte[4];
      TypeConversionUtilities.writeInt(buffer, 0, 0);
      assertEquals(0, TypeConversionUtilities.readInt(buffer, 0));
    }

    @Test
    @DisplayName("writeInt and readInt should round-trip max value")
    void writeReadIntShouldRoundTripMaxValue() {
      final byte[] buffer = new byte[4];
      TypeConversionUtilities.writeInt(buffer, 0, Integer.MAX_VALUE);
      assertEquals(Integer.MAX_VALUE, TypeConversionUtilities.readInt(buffer, 0));
    }

    @Test
    @DisplayName("writeInt and readInt should round-trip min value")
    void writeReadIntShouldRoundTripMinValue() {
      final byte[] buffer = new byte[4];
      TypeConversionUtilities.writeInt(buffer, 0, Integer.MIN_VALUE);
      assertEquals(Integer.MIN_VALUE, TypeConversionUtilities.readInt(buffer, 0));
    }

    @Test
    @DisplayName("writeInt should write in little-endian format")
    void writeIntShouldWriteLittleEndian() {
      final byte[] buffer = new byte[4];
      TypeConversionUtilities.writeInt(buffer, 0, 0x04030201);
      assertArrayEquals(new byte[] {0x01, 0x02, 0x03, 0x04}, buffer);
    }

    @Test
    @DisplayName("writeInt should work at non-zero offset")
    void writeIntShouldWorkAtOffset() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeInt(buffer, 4, 0x12345678);
      assertEquals(0x12345678, TypeConversionUtilities.readInt(buffer, 4));
    }

    @Test
    @DisplayName("writeInt should throw for null buffer")
    void writeIntShouldThrowForNullBuffer() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> TypeConversionUtilities.writeInt(null, 0, 1));
      assertTrue(exception.getMessage().contains("buffer"));
    }

    @Test
    @DisplayName("writeInt should throw for negative offset")
    void writeIntShouldThrowForNegativeOffset() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TypeConversionUtilities.writeInt(new byte[4], -1, 1));
      assertTrue(exception.getMessage().contains("offset"));
    }

    @Test
    @DisplayName("writeInt should throw for buffer overflow")
    void writeIntShouldThrowForBufferOverflow() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TypeConversionUtilities.writeInt(new byte[3], 0, 1));
      assertTrue(exception.getMessage().contains("overflow"));
    }

    @Test
    @DisplayName("readInt should throw for null buffer")
    void readIntShouldThrowForNullBuffer() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> TypeConversionUtilities.readInt(null, 0));
      assertTrue(exception.getMessage().contains("buffer"));
    }

    @Test
    @DisplayName("readInt should throw for buffer underflow")
    void readIntShouldThrowForBufferUnderflow() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TypeConversionUtilities.readInt(new byte[3], 0));
      assertTrue(exception.getMessage().contains("underflow"));
    }
  }

  @Nested
  @DisplayName("writeLong and readLong Tests")
  class WriteReadLongTests {

    @Test
    @DisplayName("writeLong and readLong should round-trip positive value")
    void writeReadLongShouldRoundTripPositive() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeLong(buffer, 0, 0x123456789ABCDEF0L);
      assertEquals(0x123456789ABCDEF0L, TypeConversionUtilities.readLong(buffer, 0));
    }

    @Test
    @DisplayName("writeLong and readLong should round-trip negative value")
    void writeReadLongShouldRoundTripNegative() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeLong(buffer, 0, -123456789012345L);
      assertEquals(-123456789012345L, TypeConversionUtilities.readLong(buffer, 0));
    }

    @Test
    @DisplayName("writeLong and readLong should round-trip zero")
    void writeReadLongShouldRoundTripZero() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeLong(buffer, 0, 0L);
      assertEquals(0L, TypeConversionUtilities.readLong(buffer, 0));
    }

    @Test
    @DisplayName("writeLong and readLong should round-trip max value")
    void writeReadLongShouldRoundTripMaxValue() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeLong(buffer, 0, Long.MAX_VALUE);
      assertEquals(Long.MAX_VALUE, TypeConversionUtilities.readLong(buffer, 0));
    }

    @Test
    @DisplayName("writeLong and readLong should round-trip min value")
    void writeReadLongShouldRoundTripMinValue() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeLong(buffer, 0, Long.MIN_VALUE);
      assertEquals(Long.MIN_VALUE, TypeConversionUtilities.readLong(buffer, 0));
    }

    @Test
    @DisplayName("writeLong should write in little-endian format")
    void writeLongShouldWriteLittleEndian() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeLong(buffer, 0, 0x0807060504030201L);
      assertArrayEquals(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08}, buffer);
    }

    @Test
    @DisplayName("writeLong should work at non-zero offset")
    void writeLongShouldWorkAtOffset() {
      final byte[] buffer = new byte[16];
      TypeConversionUtilities.writeLong(buffer, 8, 0x123456789ABCDEF0L);
      assertEquals(0x123456789ABCDEF0L, TypeConversionUtilities.readLong(buffer, 8));
    }

    @Test
    @DisplayName("writeLong should throw for null buffer")
    void writeLongShouldThrowForNullBuffer() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> TypeConversionUtilities.writeLong(null, 0, 1L));
      assertTrue(exception.getMessage().contains("buffer"));
    }

    @Test
    @DisplayName("writeLong should throw for buffer overflow")
    void writeLongShouldThrowForBufferOverflow() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TypeConversionUtilities.writeLong(new byte[7], 0, 1L));
      assertTrue(exception.getMessage().contains("overflow"));
    }

    @Test
    @DisplayName("readLong should throw for null buffer")
    void readLongShouldThrowForNullBuffer() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> TypeConversionUtilities.readLong(null, 0));
      assertTrue(exception.getMessage().contains("buffer"));
    }

    @Test
    @DisplayName("readLong should throw for buffer underflow")
    void readLongShouldThrowForBufferUnderflow() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TypeConversionUtilities.readLong(new byte[7], 0));
      assertTrue(exception.getMessage().contains("underflow"));
    }
  }

  @Nested
  @DisplayName("toNativeTypes Tests")
  class ToNativeTypesTests {

    @Test
    @DisplayName("toNativeTypes should return empty array for null input")
    void toNativeTypesShouldReturnEmptyForNull() {
      assertEquals(0, TypeConversionUtilities.toNativeTypes(null).length);
    }

    @Test
    @DisplayName("toNativeTypes should return empty array for empty input")
    void toNativeTypesShouldReturnEmptyForEmpty() {
      assertEquals(0, TypeConversionUtilities.toNativeTypes(new WasmValueType[0]).length);
    }

    @Test
    @DisplayName("toNativeTypes should convert single type")
    void toNativeTypesShouldConvertSingleType() {
      final WasmValueType[] types = {WasmValueType.I32};
      final int[] result = TypeConversionUtilities.toNativeTypes(types);
      assertEquals(1, result.length);
      assertEquals(WasmValueType.I32.toNativeTypeCode(), result[0]);
    }

    @Test
    @DisplayName("toNativeTypes should convert multiple types")
    void toNativeTypesShouldConvertMultipleTypes() {
      final WasmValueType[] types = {WasmValueType.I32, WasmValueType.I64, WasmValueType.F64};
      final int[] result = TypeConversionUtilities.toNativeTypes(types);
      assertEquals(3, result.length);
      assertEquals(WasmValueType.I32.toNativeTypeCode(), result[0]);
      assertEquals(WasmValueType.I64.toNativeTypeCode(), result[1]);
      assertEquals(WasmValueType.F64.toNativeTypeCode(), result[2]);
    }

    @Test
    @DisplayName("toNativeTypes should convert all value types")
    void toNativeTypesShouldConvertAllTypes() {
      final WasmValueType[] allTypes = WasmValueType.values();
      final int[] result = TypeConversionUtilities.toNativeTypes(allTypes);
      assertEquals(allTypes.length, result.length);
      for (int i = 0; i < allTypes.length; i++) {
        assertEquals(
            allTypes[i].toNativeTypeCode(),
            result[i],
            String.format("type code for %s at index %d", allTypes[i], i));
      }
    }
  }

  @Nested
  @DisplayName("getTypeName Tests")
  class GetTypeNameTests {

    @Test
    @DisplayName("getTypeName should return 'null' for null")
    void getTypeNameShouldReturnNullForNull() {
      assertEquals("null", TypeConversionUtilities.getTypeName(null));
    }

    @Test
    @DisplayName("getTypeName should return class name for regular object")
    void getTypeNameShouldReturnClassName() {
      assertEquals("String", TypeConversionUtilities.getTypeName("test"));
      assertEquals("Integer", TypeConversionUtilities.getTypeName(42));
      assertEquals("Double", TypeConversionUtilities.getTypeName(3.14));
    }

    @Test
    @DisplayName("getTypeName should return byte array with length")
    void getTypeNameShouldReturnByteArrayWithLength() {
      assertEquals("byte[0]", TypeConversionUtilities.getTypeName(new byte[0]));
      assertEquals("byte[5]", TypeConversionUtilities.getTypeName(new byte[5]));
      assertEquals("byte[16]", TypeConversionUtilities.getTypeName(new byte[16]));
    }
  }
}

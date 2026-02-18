package ai.tegmentum.wasmtime4j.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
      assertThat(TypeConversionUtilities.V128_SIZE_BYTES).isEqualTo(16);
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

      assertThat(copy).containsExactly(WasmValueType.I32, WasmValueType.I64);
      assertThat(copy).isNotSameAs(original);
    }

    @Test
    @DisplayName("copyTypes should return empty array for null input")
    void copyTypesShouldReturnEmptyForNull() {
      final WasmValueType[] result = TypeConversionUtilities.copyTypes(null);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("copyTypes should return empty array for empty input")
    void copyTypesShouldReturnEmptyForEmpty() {
      final WasmValueType[] result = TypeConversionUtilities.copyTypes(new WasmValueType[0]);
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("validateV128Size Tests")
  class ValidateV128SizeTests {

    @Test
    @DisplayName("validateV128Size should pass for 16-byte array")
    void validateV128SizeShouldPassFor16Bytes() {
      final byte[] bytes = new byte[16];
      TypeConversionUtilities.validateV128Size(bytes); // Should not throw
    }

    @Test
    @DisplayName("validateV128Size should throw for null array")
    void validateV128SizeShouldThrowForNull() {
      assertThatThrownBy(() -> TypeConversionUtilities.validateV128Size(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("null");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 8, 15, 17, 32})
    @DisplayName("validateV128Size should throw for wrong size")
    void validateV128SizeShouldThrowForWrongSize(final int size) {
      final byte[] bytes = new byte[size];
      assertThatThrownBy(() -> TypeConversionUtilities.validateV128Size(bytes))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("16")
          .hasMessageContaining(String.valueOf(size));
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

      TypeConversionUtilities.validateParameterTypes(params, expectedTypes); // Should not throw
    }

    @Test
    @DisplayName("validateParameterTypes should pass for empty arrays")
    void validateParameterTypesShouldPassForEmptyArrays() {
      TypeConversionUtilities.validateParameterTypes(
          new WasmValue[0], new WasmValueType[0]); // Should not throw
    }

    @Test
    @DisplayName("validateParameterTypes should throw for null params")
    void validateParameterTypesShouldThrowForNullParams() {
      final WasmValueType[] expectedTypes = {WasmValueType.I32};
      assertThatThrownBy(() -> TypeConversionUtilities.validateParameterTypes(null, expectedTypes))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("params");
    }

    @Test
    @DisplayName("validateParameterTypes should throw for null expectedTypes")
    void validateParameterTypesShouldThrowForNullExpectedTypes() {
      final WasmValue[] params = {WasmValue.i32(1)};
      assertThatThrownBy(() -> TypeConversionUtilities.validateParameterTypes(params, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("expectedTypes");
    }

    @Test
    @DisplayName("validateParameterTypes should throw for count mismatch")
    void validateParameterTypesShouldThrowForCountMismatch() {
      final WasmValue[] params = {WasmValue.i32(1)};
      final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.I64};

      assertThatThrownBy(
              () -> TypeConversionUtilities.validateParameterTypes(params, expectedTypes))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("count mismatch")
          .hasMessageContaining("1")
          .hasMessageContaining("2");
    }

    @Test
    @DisplayName("validateParameterTypes should throw for type mismatch")
    void validateParameterTypesShouldThrowForTypeMismatch() {
      final WasmValue[] params = {WasmValue.i32(1)};
      final WasmValueType[] expectedTypes = {WasmValueType.I64};

      assertThatThrownBy(
              () -> TypeConversionUtilities.validateParameterTypes(params, expectedTypes))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("type mismatch")
          .hasMessageContaining("index 0");
    }

    @Test
    @DisplayName("validateParameterTypes should throw for null parameter element")
    void validateParameterTypesShouldThrowForNullParameter() {
      final WasmValue[] params = {null};
      final WasmValueType[] expectedTypes = {WasmValueType.I32};

      assertThatThrownBy(
              () -> TypeConversionUtilities.validateParameterTypes(params, expectedTypes))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("index 0")
          .hasMessageContaining("null");
    }
  }

  @Nested
  @DisplayName("getValueSize Tests")
  class GetValueSizeTests {

    @Test
    @DisplayName("getValueSize should return 4 for I32")
    void getValueSizeShouldReturn4ForI32() {
      assertThat(TypeConversionUtilities.getValueSize(WasmValueType.I32)).isEqualTo(4);
    }

    @Test
    @DisplayName("getValueSize should return 4 for F32")
    void getValueSizeShouldReturn4ForF32() {
      assertThat(TypeConversionUtilities.getValueSize(WasmValueType.F32)).isEqualTo(4);
    }

    @Test
    @DisplayName("getValueSize should return 8 for I64")
    void getValueSizeShouldReturn8ForI64() {
      assertThat(TypeConversionUtilities.getValueSize(WasmValueType.I64)).isEqualTo(8);
    }

    @Test
    @DisplayName("getValueSize should return 8 for F64")
    void getValueSizeShouldReturn8ForF64() {
      assertThat(TypeConversionUtilities.getValueSize(WasmValueType.F64)).isEqualTo(8);
    }

    @Test
    @DisplayName("getValueSize should return 8 for FUNCREF")
    void getValueSizeShouldReturn8ForFuncref() {
      assertThat(TypeConversionUtilities.getValueSize(WasmValueType.FUNCREF)).isEqualTo(8);
    }

    @Test
    @DisplayName("getValueSize should return 8 for EXTERNREF")
    void getValueSizeShouldReturn8ForExternref() {
      assertThat(TypeConversionUtilities.getValueSize(WasmValueType.EXTERNREF)).isEqualTo(8);
    }

    @Test
    @DisplayName("getValueSize should return 16 for V128")
    void getValueSizeShouldReturn16ForV128() {
      assertThat(TypeConversionUtilities.getValueSize(WasmValueType.V128)).isEqualTo(16);
    }

    @Test
    @DisplayName("getValueSize should throw for null")
    void getValueSizeShouldThrowForNull() {
      assertThatThrownBy(() -> TypeConversionUtilities.getValueSize(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("valueType");
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
      assertThat(TypeConversionUtilities.readInt(buffer, 0)).isEqualTo(0x12345678);
    }

    @Test
    @DisplayName("writeInt and readInt should round-trip negative value")
    void writeReadIntShouldRoundTripNegative() {
      final byte[] buffer = new byte[4];
      TypeConversionUtilities.writeInt(buffer, 0, -12345);
      assertThat(TypeConversionUtilities.readInt(buffer, 0)).isEqualTo(-12345);
    }

    @Test
    @DisplayName("writeInt and readInt should round-trip zero")
    void writeReadIntShouldRoundTripZero() {
      final byte[] buffer = new byte[4];
      TypeConversionUtilities.writeInt(buffer, 0, 0);
      assertThat(TypeConversionUtilities.readInt(buffer, 0)).isEqualTo(0);
    }

    @Test
    @DisplayName("writeInt and readInt should round-trip max value")
    void writeReadIntShouldRoundTripMaxValue() {
      final byte[] buffer = new byte[4];
      TypeConversionUtilities.writeInt(buffer, 0, Integer.MAX_VALUE);
      assertThat(TypeConversionUtilities.readInt(buffer, 0)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("writeInt and readInt should round-trip min value")
    void writeReadIntShouldRoundTripMinValue() {
      final byte[] buffer = new byte[4];
      TypeConversionUtilities.writeInt(buffer, 0, Integer.MIN_VALUE);
      assertThat(TypeConversionUtilities.readInt(buffer, 0)).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    @DisplayName("writeInt should write in little-endian format")
    void writeIntShouldWriteLittleEndian() {
      final byte[] buffer = new byte[4];
      TypeConversionUtilities.writeInt(buffer, 0, 0x04030201);
      assertThat(buffer).containsExactly(0x01, 0x02, 0x03, 0x04);
    }

    @Test
    @DisplayName("writeInt should work at non-zero offset")
    void writeIntShouldWorkAtOffset() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeInt(buffer, 4, 0x12345678);
      assertThat(TypeConversionUtilities.readInt(buffer, 4)).isEqualTo(0x12345678);
    }

    @Test
    @DisplayName("writeInt should throw for null buffer")
    void writeIntShouldThrowForNullBuffer() {
      assertThatThrownBy(() -> TypeConversionUtilities.writeInt(null, 0, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("buffer");
    }

    @Test
    @DisplayName("writeInt should throw for negative offset")
    void writeIntShouldThrowForNegativeOffset() {
      assertThatThrownBy(() -> TypeConversionUtilities.writeInt(new byte[4], -1, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("offset");
    }

    @Test
    @DisplayName("writeInt should throw for buffer overflow")
    void writeIntShouldThrowForBufferOverflow() {
      assertThatThrownBy(() -> TypeConversionUtilities.writeInt(new byte[3], 0, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("overflow");
    }

    @Test
    @DisplayName("readInt should throw for null buffer")
    void readIntShouldThrowForNullBuffer() {
      assertThatThrownBy(() -> TypeConversionUtilities.readInt(null, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("buffer");
    }

    @Test
    @DisplayName("readInt should throw for buffer underflow")
    void readIntShouldThrowForBufferUnderflow() {
      assertThatThrownBy(() -> TypeConversionUtilities.readInt(new byte[3], 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("underflow");
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
      assertThat(TypeConversionUtilities.readLong(buffer, 0)).isEqualTo(0x123456789ABCDEF0L);
    }

    @Test
    @DisplayName("writeLong and readLong should round-trip negative value")
    void writeReadLongShouldRoundTripNegative() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeLong(buffer, 0, -123456789012345L);
      assertThat(TypeConversionUtilities.readLong(buffer, 0)).isEqualTo(-123456789012345L);
    }

    @Test
    @DisplayName("writeLong and readLong should round-trip zero")
    void writeReadLongShouldRoundTripZero() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeLong(buffer, 0, 0L);
      assertThat(TypeConversionUtilities.readLong(buffer, 0)).isEqualTo(0L);
    }

    @Test
    @DisplayName("writeLong and readLong should round-trip max value")
    void writeReadLongShouldRoundTripMaxValue() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeLong(buffer, 0, Long.MAX_VALUE);
      assertThat(TypeConversionUtilities.readLong(buffer, 0)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @DisplayName("writeLong and readLong should round-trip min value")
    void writeReadLongShouldRoundTripMinValue() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeLong(buffer, 0, Long.MIN_VALUE);
      assertThat(TypeConversionUtilities.readLong(buffer, 0)).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    @DisplayName("writeLong should write in little-endian format")
    void writeLongShouldWriteLittleEndian() {
      final byte[] buffer = new byte[8];
      TypeConversionUtilities.writeLong(buffer, 0, 0x0807060504030201L);
      assertThat(buffer).containsExactly(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08);
    }

    @Test
    @DisplayName("writeLong should work at non-zero offset")
    void writeLongShouldWorkAtOffset() {
      final byte[] buffer = new byte[16];
      TypeConversionUtilities.writeLong(buffer, 8, 0x123456789ABCDEF0L);
      assertThat(TypeConversionUtilities.readLong(buffer, 8)).isEqualTo(0x123456789ABCDEF0L);
    }

    @Test
    @DisplayName("writeLong should throw for null buffer")
    void writeLongShouldThrowForNullBuffer() {
      assertThatThrownBy(() -> TypeConversionUtilities.writeLong(null, 0, 1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("buffer");
    }

    @Test
    @DisplayName("writeLong should throw for buffer overflow")
    void writeLongShouldThrowForBufferOverflow() {
      assertThatThrownBy(() -> TypeConversionUtilities.writeLong(new byte[7], 0, 1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("overflow");
    }

    @Test
    @DisplayName("readLong should throw for null buffer")
    void readLongShouldThrowForNullBuffer() {
      assertThatThrownBy(() -> TypeConversionUtilities.readLong(null, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("buffer");
    }

    @Test
    @DisplayName("readLong should throw for buffer underflow")
    void readLongShouldThrowForBufferUnderflow() {
      assertThatThrownBy(() -> TypeConversionUtilities.readLong(new byte[7], 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("underflow");
    }
  }

  @Nested
  @DisplayName("toNativeTypes Tests")
  class ToNativeTypesTests {

    @Test
    @DisplayName("toNativeTypes should return empty array for null input")
    void toNativeTypesShouldReturnEmptyForNull() {
      assertThat(TypeConversionUtilities.toNativeTypes(null)).isEmpty();
    }

    @Test
    @DisplayName("toNativeTypes should return empty array for empty input")
    void toNativeTypesShouldReturnEmptyForEmpty() {
      assertThat(TypeConversionUtilities.toNativeTypes(new WasmValueType[0])).isEmpty();
    }

    @Test
    @DisplayName("toNativeTypes should convert single type")
    void toNativeTypesShouldConvertSingleType() {
      final WasmValueType[] types = {WasmValueType.I32};
      final int[] result = TypeConversionUtilities.toNativeTypes(types);
      assertThat(result).hasSize(1);
      assertThat(result[0]).isEqualTo(WasmValueType.I32.toNativeTypeCode());
    }

    @Test
    @DisplayName("toNativeTypes should convert multiple types")
    void toNativeTypesShouldConvertMultipleTypes() {
      final WasmValueType[] types = {WasmValueType.I32, WasmValueType.I64, WasmValueType.F64};
      final int[] result = TypeConversionUtilities.toNativeTypes(types);
      assertThat(result).hasSize(3);
      assertThat(result[0]).isEqualTo(WasmValueType.I32.toNativeTypeCode());
      assertThat(result[1]).isEqualTo(WasmValueType.I64.toNativeTypeCode());
      assertThat(result[2]).isEqualTo(WasmValueType.F64.toNativeTypeCode());
    }

    @Test
    @DisplayName("toNativeTypes should convert all value types")
    void toNativeTypesShouldConvertAllTypes() {
      final WasmValueType[] allTypes = WasmValueType.values();
      final int[] result = TypeConversionUtilities.toNativeTypes(allTypes);
      assertThat(result).hasSize(allTypes.length);
      for (int i = 0; i < allTypes.length; i++) {
        assertThat(result[i])
            .as("type code for %s at index %d", allTypes[i], i)
            .isEqualTo(allTypes[i].toNativeTypeCode());
      }
    }
  }

  @Nested
  @DisplayName("getTypeName Tests")
  class GetTypeNameTests {

    @Test
    @DisplayName("getTypeName should return 'null' for null")
    void getTypeNameShouldReturnNullForNull() {
      assertThat(TypeConversionUtilities.getTypeName(null)).isEqualTo("null");
    }

    @Test
    @DisplayName("getTypeName should return class name for regular object")
    void getTypeNameShouldReturnClassName() {
      assertThat(TypeConversionUtilities.getTypeName("test")).isEqualTo("String");
      assertThat(TypeConversionUtilities.getTypeName(42)).isEqualTo("Integer");
      assertThat(TypeConversionUtilities.getTypeName(3.14)).isEqualTo("Double");
    }

    @Test
    @DisplayName("getTypeName should return byte array with length")
    void getTypeNameShouldReturnByteArrayWithLength() {
      assertThat(TypeConversionUtilities.getTypeName(new byte[0])).isEqualTo("byte[0]");
      assertThat(TypeConversionUtilities.getTypeName(new byte[5])).isEqualTo("byte[5]");
      assertThat(TypeConversionUtilities.getTypeName(new byte[16])).isEqualTo("byte[16]");
    }
  }
}

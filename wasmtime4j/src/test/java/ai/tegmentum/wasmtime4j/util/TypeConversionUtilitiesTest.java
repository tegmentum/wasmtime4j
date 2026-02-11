package ai.tegmentum.wasmtime4j.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
  @DisplayName("typeToString Tests")
  class TypeToStringTests {

    @Test
    @DisplayName("typeToString should convert I32 to 'i32'")
    void typeToStringShouldConvertI32() {
      assertThat(TypeConversionUtilities.typeToString(WasmValueType.I32)).isEqualTo("i32");
    }

    @Test
    @DisplayName("typeToString should convert I64 to 'i64'")
    void typeToStringShouldConvertI64() {
      assertThat(TypeConversionUtilities.typeToString(WasmValueType.I64)).isEqualTo("i64");
    }

    @Test
    @DisplayName("typeToString should convert F32 to 'f32'")
    void typeToStringShouldConvertF32() {
      assertThat(TypeConversionUtilities.typeToString(WasmValueType.F32)).isEqualTo("f32");
    }

    @Test
    @DisplayName("typeToString should convert F64 to 'f64'")
    void typeToStringShouldConvertF64() {
      assertThat(TypeConversionUtilities.typeToString(WasmValueType.F64)).isEqualTo("f64");
    }

    @Test
    @DisplayName("typeToString should convert V128 to 'v128'")
    void typeToStringShouldConvertV128() {
      assertThat(TypeConversionUtilities.typeToString(WasmValueType.V128)).isEqualTo("v128");
    }

    @Test
    @DisplayName("typeToString should convert FUNCREF to 'funcref'")
    void typeToStringShouldConvertFuncref() {
      assertThat(TypeConversionUtilities.typeToString(WasmValueType.FUNCREF)).isEqualTo("funcref");
    }

    @Test
    @DisplayName("typeToString should convert EXTERNREF to 'externref'")
    void typeToStringShouldConvertExternref() {
      assertThat(TypeConversionUtilities.typeToString(WasmValueType.EXTERNREF))
          .isEqualTo("externref");
    }

    @Test
    @DisplayName("typeToString should throw for null type")
    void typeToStringShouldThrowForNull() {
      assertThatThrownBy(() -> TypeConversionUtilities.typeToString(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("type");
    }
  }

  @Nested
  @DisplayName("stringToType Tests")
  class StringToTypeTests {

    @Test
    @DisplayName("stringToType should convert 'i32' to I32")
    void stringToTypeShouldConvertI32() {
      assertThat(TypeConversionUtilities.stringToType("i32")).isEqualTo(WasmValueType.I32);
    }

    @Test
    @DisplayName("stringToType should convert 'i64' to I64")
    void stringToTypeShouldConvertI64() {
      assertThat(TypeConversionUtilities.stringToType("i64")).isEqualTo(WasmValueType.I64);
    }

    @Test
    @DisplayName("stringToType should convert 'f32' to F32")
    void stringToTypeShouldConvertF32() {
      assertThat(TypeConversionUtilities.stringToType("f32")).isEqualTo(WasmValueType.F32);
    }

    @Test
    @DisplayName("stringToType should convert 'f64' to F64")
    void stringToTypeShouldConvertF64() {
      assertThat(TypeConversionUtilities.stringToType("f64")).isEqualTo(WasmValueType.F64);
    }

    @Test
    @DisplayName("stringToType should convert 'v128' to V128")
    void stringToTypeShouldConvertV128() {
      assertThat(TypeConversionUtilities.stringToType("v128")).isEqualTo(WasmValueType.V128);
    }

    @Test
    @DisplayName("stringToType should convert 'funcref' to FUNCREF")
    void stringToTypeShouldConvertFuncref() {
      assertThat(TypeConversionUtilities.stringToType("funcref")).isEqualTo(WasmValueType.FUNCREF);
    }

    @Test
    @DisplayName("stringToType should convert 'externref' to EXTERNREF")
    void stringToTypeShouldConvertExternref() {
      assertThat(TypeConversionUtilities.stringToType("externref"))
          .isEqualTo(WasmValueType.EXTERNREF);
    }

    @Test
    @DisplayName("stringToType should be case insensitive")
    void stringToTypeShouldBeCaseInsensitive() {
      assertThat(TypeConversionUtilities.stringToType("I32")).isEqualTo(WasmValueType.I32);
      assertThat(TypeConversionUtilities.stringToType("I64")).isEqualTo(WasmValueType.I64);
      assertThat(TypeConversionUtilities.stringToType("F32")).isEqualTo(WasmValueType.F32);
      assertThat(TypeConversionUtilities.stringToType("FUNCREF")).isEqualTo(WasmValueType.FUNCREF);
    }

    @Test
    @DisplayName("stringToType should throw for null string")
    void stringToTypeShouldThrowForNull() {
      assertThatThrownBy(() -> TypeConversionUtilities.stringToType(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("typeString");
    }

    @Test
    @DisplayName("stringToType should throw for invalid string")
    void stringToTypeShouldThrowForInvalidString() {
      assertThatThrownBy(() -> TypeConversionUtilities.stringToType("invalid"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid WebAssembly type string");
    }
  }

  @Nested
  @DisplayName("Round-trip conversion Tests")
  class RoundTripTests {

    @ParameterizedTest
    @EnumSource(
        value = WasmValueType.class,
        names = {"I32", "I64", "F32", "F64", "V128", "FUNCREF", "EXTERNREF"})
    @DisplayName("typeToString and stringToType should be inverse operations")
    void roundTripConversionShouldWork(final WasmValueType type) {
      final String typeString = TypeConversionUtilities.typeToString(type);
      final WasmValueType convertedBack = TypeConversionUtilities.stringToType(typeString);
      assertThat(convertedBack).isEqualTo(type);
    }
  }

  @Nested
  @DisplayName("typesToStrings Tests")
  class TypesToStringsTests {

    @Test
    @DisplayName("typesToStrings should convert array of types")
    void typesToStringsShouldConvertArray() {
      final WasmValueType[] types = {WasmValueType.I32, WasmValueType.I64, WasmValueType.F32};
      final String[] result = TypeConversionUtilities.typesToStrings(types);

      assertThat(result).containsExactly("i32", "i64", "f32");
    }

    @Test
    @DisplayName("typesToStrings should return empty array for empty input")
    void typesToStringsShouldReturnEmptyForEmpty() {
      final String[] result = TypeConversionUtilities.typesToStrings(new WasmValueType[0]);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("typesToStrings should throw for null array")
    void typesToStringsShouldThrowForNull() {
      assertThatThrownBy(() -> TypeConversionUtilities.typesToStrings(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("types");
    }

    @Test
    @DisplayName("typesToStrings should throw for null element")
    void typesToStringsShouldThrowForNullElement() {
      final WasmValueType[] types = {WasmValueType.I32, null, WasmValueType.F32};
      assertThatThrownBy(() -> TypeConversionUtilities.typesToStrings(types))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("index 1");
    }
  }

  @Nested
  @DisplayName("stringsToTypes Tests")
  class StringsToTypesTests {

    @Test
    @DisplayName("stringsToTypes should convert array of strings")
    void stringsToTypesShouldConvertArray() {
      final String[] strings = {"i32", "i64", "f32"};
      final WasmValueType[] result = TypeConversionUtilities.stringsToTypes(strings);

      assertThat(result).containsExactly(WasmValueType.I32, WasmValueType.I64, WasmValueType.F32);
    }

    @Test
    @DisplayName("stringsToTypes should return empty array for empty input")
    void stringsToTypesShouldReturnEmptyForEmpty() {
      final WasmValueType[] result = TypeConversionUtilities.stringsToTypes(new String[0]);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("stringsToTypes should throw for null array")
    void stringsToTypesShouldThrowForNull() {
      assertThatThrownBy(() -> TypeConversionUtilities.stringsToTypes(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("typeStrings");
    }

    @Test
    @DisplayName("stringsToTypes should throw for null element")
    void stringsToTypesShouldThrowForNullElement() {
      final String[] strings = {"i32", null, "f32"};
      assertThatThrownBy(() -> TypeConversionUtilities.stringsToTypes(strings))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("index 1");
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
  @DisplayName("encodeValueType Tests")
  class EncodeValueTypeTests {

    @Test
    @DisplayName("encodeValueType should encode I32 as 0")
    void encodeValueTypeShouldEncodeI32() {
      assertThat(TypeConversionUtilities.encodeValueType(WasmValueType.I32)).isEqualTo((byte) 0);
    }

    @Test
    @DisplayName("encodeValueType should encode I64 as 1")
    void encodeValueTypeShouldEncodeI64() {
      assertThat(TypeConversionUtilities.encodeValueType(WasmValueType.I64)).isEqualTo((byte) 1);
    }

    @Test
    @DisplayName("encodeValueType should encode F32 as 2")
    void encodeValueTypeShouldEncodeF32() {
      assertThat(TypeConversionUtilities.encodeValueType(WasmValueType.F32)).isEqualTo((byte) 2);
    }

    @Test
    @DisplayName("encodeValueType should encode F64 as 3")
    void encodeValueTypeShouldEncodeF64() {
      assertThat(TypeConversionUtilities.encodeValueType(WasmValueType.F64)).isEqualTo((byte) 3);
    }

    @Test
    @DisplayName("encodeValueType should encode V128 as 4")
    void encodeValueTypeShouldEncodeV128() {
      assertThat(TypeConversionUtilities.encodeValueType(WasmValueType.V128)).isEqualTo((byte) 4);
    }

    @Test
    @DisplayName("encodeValueType should encode FUNCREF as 5")
    void encodeValueTypeShouldEncodeFuncref() {
      assertThat(TypeConversionUtilities.encodeValueType(WasmValueType.FUNCREF))
          .isEqualTo((byte) 5);
    }

    @Test
    @DisplayName("encodeValueType should encode EXTERNREF as 6")
    void encodeValueTypeShouldEncodeExternref() {
      assertThat(TypeConversionUtilities.encodeValueType(WasmValueType.EXTERNREF))
          .isEqualTo((byte) 6);
    }

    @Test
    @DisplayName("encodeValueType should throw for null")
    void encodeValueTypeShouldThrowForNull() {
      assertThatThrownBy(() -> TypeConversionUtilities.encodeValueType(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("valueType");
    }
  }

  @Nested
  @DisplayName("decodeValueType Tests")
  class DecodeValueTypeTests {

    @Test
    @DisplayName("decodeValueType should decode 0 as I32")
    void decodeValueTypeShouldDecodeI32() {
      assertThat(TypeConversionUtilities.decodeValueType((byte) 0)).isEqualTo(WasmValueType.I32);
    }

    @Test
    @DisplayName("decodeValueType should decode 1 as I64")
    void decodeValueTypeShouldDecodeI64() {
      assertThat(TypeConversionUtilities.decodeValueType((byte) 1)).isEqualTo(WasmValueType.I64);
    }

    @Test
    @DisplayName("decodeValueType should decode 2 as F32")
    void decodeValueTypeShouldDecodeF32() {
      assertThat(TypeConversionUtilities.decodeValueType((byte) 2)).isEqualTo(WasmValueType.F32);
    }

    @Test
    @DisplayName("decodeValueType should decode 3 as F64")
    void decodeValueTypeShouldDecodeF64() {
      assertThat(TypeConversionUtilities.decodeValueType((byte) 3)).isEqualTo(WasmValueType.F64);
    }

    @Test
    @DisplayName("decodeValueType should decode 4 as V128")
    void decodeValueTypeShouldDecodeV128() {
      assertThat(TypeConversionUtilities.decodeValueType((byte) 4)).isEqualTo(WasmValueType.V128);
    }

    @Test
    @DisplayName("decodeValueType should decode 5 as FUNCREF")
    void decodeValueTypeShouldDecodeFuncref() {
      assertThat(TypeConversionUtilities.decodeValueType((byte) 5))
          .isEqualTo(WasmValueType.FUNCREF);
    }

    @Test
    @DisplayName("decodeValueType should decode 6 as EXTERNREF")
    void decodeValueTypeShouldDecodeExternref() {
      assertThat(TypeConversionUtilities.decodeValueType((byte) 6))
          .isEqualTo(WasmValueType.EXTERNREF);
    }

    @ParameterizedTest
    @ValueSource(bytes = {7, -1, 100})
    @DisplayName("decodeValueType should throw for invalid encoded value")
    void decodeValueTypeShouldThrowForInvalid(final byte encoded) {
      assertThatThrownBy(() -> TypeConversionUtilities.decodeValueType(encoded))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid encoded value type");
    }
  }

  @Nested
  @DisplayName("Encode/Decode round-trip Tests")
  class EncodeDecodeRoundTripTests {

    @ParameterizedTest
    @EnumSource(
        value = WasmValueType.class,
        names = {"I32", "I64", "F32", "F64", "V128", "FUNCREF", "EXTERNREF"})
    @DisplayName("encodeValueType and decodeValueType should be inverse operations")
    void roundTripEncodingDecodingShouldWork(final WasmValueType type) {
      final byte encoded = TypeConversionUtilities.encodeValueType(type);
      final WasmValueType decoded = TypeConversionUtilities.decodeValueType(encoded);
      assertThat(decoded).isEqualTo(type);
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

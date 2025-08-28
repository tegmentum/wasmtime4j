package ai.tegmentum.wasmtime4j.jni.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniTypeConverter}.
 *
 * <p>These tests cover type conversion between Java and WebAssembly types, including basic types,
 * SIMD types (v128), and reference types (funcref, externref).
 */
class JniTypeConverterTest {

  @Test
  void testTypeToString() {
    assertThat(JniTypeConverter.typeToString(WasmValueType.I32)).isEqualTo("i32");
    assertThat(JniTypeConverter.typeToString(WasmValueType.I64)).isEqualTo("i64");
    assertThat(JniTypeConverter.typeToString(WasmValueType.F32)).isEqualTo("f32");
    assertThat(JniTypeConverter.typeToString(WasmValueType.F64)).isEqualTo("f64");
    assertThat(JniTypeConverter.typeToString(WasmValueType.V128)).isEqualTo("v128");
    assertThat(JniTypeConverter.typeToString(WasmValueType.FUNCREF)).isEqualTo("funcref");
    assertThat(JniTypeConverter.typeToString(WasmValueType.EXTERNREF)).isEqualTo("externref");
  }

  @Test
  void testTypeToStringWithNull() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> JniTypeConverter.typeToString(null));

    assertThat(exception.getMessage()).contains("type").contains("must not be null");
  }

  @Test
  void testStringToType() {
    assertThat(JniTypeConverter.stringToType("i32")).isEqualTo(WasmValueType.I32);
    assertThat(JniTypeConverter.stringToType("i64")).isEqualTo(WasmValueType.I64);
    assertThat(JniTypeConverter.stringToType("f32")).isEqualTo(WasmValueType.F32);
    assertThat(JniTypeConverter.stringToType("f64")).isEqualTo(WasmValueType.F64);
    assertThat(JniTypeConverter.stringToType("v128")).isEqualTo(WasmValueType.V128);
    assertThat(JniTypeConverter.stringToType("funcref")).isEqualTo(WasmValueType.FUNCREF);
    assertThat(JniTypeConverter.stringToType("externref")).isEqualTo(WasmValueType.EXTERNREF);
  }

  @Test
  void testStringToTypeWithMixedCase() {
    assertThat(JniTypeConverter.stringToType("I32")).isEqualTo(WasmValueType.I32);
    assertThat(JniTypeConverter.stringToType("F64")).isEqualTo(WasmValueType.F64);
    assertThat(JniTypeConverter.stringToType("V128")).isEqualTo(WasmValueType.V128);
    assertThat(JniTypeConverter.stringToType("FuncRef")).isEqualTo(WasmValueType.FUNCREF);
    assertThat(JniTypeConverter.stringToType("ExternRef")).isEqualTo(WasmValueType.EXTERNREF);
  }

  @Test
  void testStringToTypeWithNull() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> JniTypeConverter.stringToType(null));

    assertThat(exception.getMessage()).contains("typeString").contains("must not be null");
  }

  @Test
  void testStringToTypeWithInvalidString() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> JniTypeConverter.stringToType("invalid"));

    assertThat(exception.getMessage()).contains("Invalid WebAssembly type string: invalid");
  }

  @Test
  void testWasmValueToNativeParamBasicTypes() {
    // Test i32
    final WasmValue i32Value = WasmValue.i32(42);
    assertThat(JniTypeConverter.wasmValueToNativeParam(i32Value)).isEqualTo(42);

    // Test i64
    final WasmValue i64Value = WasmValue.i64(100L);
    assertThat(JniTypeConverter.wasmValueToNativeParam(i64Value)).isEqualTo(100L);

    // Test f32
    final WasmValue f32Value = WasmValue.f32(3.14f);
    assertThat(JniTypeConverter.wasmValueToNativeParam(f32Value)).isEqualTo(3.14f);

    // Test f64
    final WasmValue f64Value = WasmValue.f64(2.718);
    assertThat(JniTypeConverter.wasmValueToNativeParam(f64Value)).isEqualTo(2.718);
  }

  @Test
  void testWasmValueToNativeParamV128() {
    final byte[] v128Bytes = new byte[16];
    for (int i = 0; i < 16; i++) {
      v128Bytes[i] = (byte) i;
    }

    final WasmValue v128Value = WasmValue.v128(v128Bytes);
    final Object result = JniTypeConverter.wasmValueToNativeParam(v128Value);

    assertThat(result).isInstanceOf(byte[].class);
    assertThat((byte[]) result).hasSize(16);
    assertThat((byte[]) result).isEqualTo(v128Bytes);
  }

  @Test
  void testWasmValueToNativeParamV128InvalidSize() {
    final byte[] invalidBytes = new byte[8]; // Wrong size

    final JniValidationException exception =
        assertThrows(
            JniValidationException.class,
            () -> {
              final WasmValue v128Value = WasmValue.v128(invalidBytes);
              // This should fail during WasmValue.v128() creation, not in converter
            });

    assertThat(exception.getMessage()).contains("v128 value must be exactly 16 bytes");
  }

  @Test
  void testWasmValueToNativeParamReferenceTypes() {
    final Object funcRef = new Object();
    final WasmValue funcrefValue = WasmValue.funcref(funcRef);
    assertThat(JniTypeConverter.wasmValueToNativeParam(funcrefValue)).isSameAs(funcRef);

    final String externRef = "external_data";
    final WasmValue externrefValue = WasmValue.externref(externRef);
    assertThat(JniTypeConverter.wasmValueToNativeParam(externrefValue)).isSameAs(externRef);

    // Test null references
    final WasmValue nullFuncref = WasmValue.funcref(null);
    assertThat(JniTypeConverter.wasmValueToNativeParam(nullFuncref)).isNull();

    final WasmValue nullExternref = WasmValue.externref(null);
    assertThat(JniTypeConverter.wasmValueToNativeParam(nullExternref)).isNull();
  }

  @Test
  void testWasmValueToNativeParamWithNull() {
    final JniValidationException exception =
        assertThrows(
            JniValidationException.class, () -> JniTypeConverter.wasmValueToNativeParam(null));

    assertThat(exception.getMessage()).contains("value").contains("must not be null");
  }

  @Test
  void testWasmValuesToNativeParams() {
    final WasmValue[] values = {
      WasmValue.i32(42), WasmValue.f64(3.14), WasmValue.externref("test")
    };

    final Object[] nativeParams = JniTypeConverter.wasmValuesToNativeParams(values);

    assertThat(nativeParams).hasSize(3);
    assertThat(nativeParams[0]).isEqualTo(42);
    assertThat(nativeParams[1]).isEqualTo(3.14);
    assertThat(nativeParams[2]).isEqualTo("test");
  }

  @Test
  void testWasmValuesToNativeParamsWithNull() {
    final JniValidationException exception =
        assertThrows(
            JniValidationException.class, () -> JniTypeConverter.wasmValuesToNativeParams(null));

    assertThat(exception.getMessage()).contains("values").contains("must not be null");
  }

  @Test
  void testWasmValuesToNativeParamsWithNullElement() {
    final WasmValue[] values = {WasmValue.i32(42), null, WasmValue.f64(3.14)};

    final JniValidationException exception =
        assertThrows(
            JniValidationException.class, () -> JniTypeConverter.wasmValuesToNativeParams(values));

    assertThat(exception.getMessage()).contains("Parameter at index 1 is null");
  }

  @Test
  void testNativeResultToWasmValueBasicTypes() {
    // Test i32
    final WasmValue i32Result = JniTypeConverter.nativeResultToWasmValue(42, WasmValueType.I32);
    assertThat(i32Result.getType()).isEqualTo(WasmValueType.I32);
    assertThat(i32Result.asInt()).isEqualTo(42);

    // Test i64
    final WasmValue i64Result = JniTypeConverter.nativeResultToWasmValue(100L, WasmValueType.I64);
    assertThat(i64Result.getType()).isEqualTo(WasmValueType.I64);
    assertThat(i64Result.asLong()).isEqualTo(100L);

    // Test f32
    final WasmValue f32Result = JniTypeConverter.nativeResultToWasmValue(3.14f, WasmValueType.F32);
    assertThat(f32Result.getType()).isEqualTo(WasmValueType.F32);
    assertThat(f32Result.asFloat()).isEqualTo(3.14f);

    // Test f64
    final WasmValue f64Result = JniTypeConverter.nativeResultToWasmValue(2.718, WasmValueType.F64);
    assertThat(f64Result.getType()).isEqualTo(WasmValueType.F64);
    assertThat(f64Result.asDouble()).isEqualTo(2.718);
  }

  @Test
  void testNativeResultToWasmValueV128() {
    final byte[] v128Bytes = new byte[16];
    for (int i = 0; i < 16; i++) {
      v128Bytes[i] = (byte) (i * 2);
    }

    final WasmValue result =
        JniTypeConverter.nativeResultToWasmValue(v128Bytes, WasmValueType.V128);

    assertThat(result.getType()).isEqualTo(WasmValueType.V128);
    assertThat(result.asV128()).isEqualTo(v128Bytes);
  }

  @Test
  void testNativeResultToWasmValueV128InvalidSize() {
    final byte[] invalidBytes = new byte[8]; // Wrong size

    final JniValidationException exception =
        assertThrows(
            JniValidationException.class,
            () -> JniTypeConverter.nativeResultToWasmValue(invalidBytes, WasmValueType.V128));

    assertThat(exception.getMessage()).contains("v128 result has invalid size: 8, expected 16");
  }

  @Test
  void testNativeResultToWasmValueReferenceTypes() {
    final Object funcRef = new Object();
    final WasmValue funcrefResult =
        JniTypeConverter.nativeResultToWasmValue(funcRef, WasmValueType.FUNCREF);
    assertThat(funcrefResult.getType()).isEqualTo(WasmValueType.FUNCREF);
    assertThat(funcrefResult.asFuncref()).isSameAs(funcRef);

    final String externRef = "external_data";
    final WasmValue externrefResult =
        JniTypeConverter.nativeResultToWasmValue(externRef, WasmValueType.EXTERNREF);
    assertThat(externrefResult.getType()).isEqualTo(WasmValueType.EXTERNREF);
    assertThat(externrefResult.asExternref()).isSameAs(externRef);

    // Test null references
    final WasmValue nullFuncrefResult =
        JniTypeConverter.nativeResultToWasmValue(null, WasmValueType.FUNCREF);
    assertThat(nullFuncrefResult.getType()).isEqualTo(WasmValueType.FUNCREF);
    assertThat(nullFuncrefResult.asFuncref()).isNull();
  }

  @Test
  void testNativeResultToWasmValueTypeMismatch() {
    // Try to convert String to i32
    final JniValidationException exception =
        assertThrows(
            JniValidationException.class,
            () -> JniTypeConverter.nativeResultToWasmValue("not_an_int", WasmValueType.I32));

    assertThat(exception.getMessage()).contains("Expected i32 result, got: String");
  }

  @Test
  void testNativeResultToWasmValueWithNullExpectedType() {
    final JniValidationException exception =
        assertThrows(
            JniValidationException.class, () -> JniTypeConverter.nativeResultToWasmValue(42, null));

    assertThat(exception.getMessage()).contains("expectedType").contains("must not be null");
  }

  @Test
  void testNativeResultsToWasmValues() {
    final Object[] nativeResults = {42, 3.14, 100L};
    final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.F64, WasmValueType.I64};

    final WasmValue[] wasmValues =
        JniTypeConverter.nativeResultsToWasmValues(nativeResults, expectedTypes);

    assertThat(wasmValues).hasSize(3);
    assertThat(wasmValues[0].getType()).isEqualTo(WasmValueType.I32);
    assertThat(wasmValues[0].asInt()).isEqualTo(42);
    assertThat(wasmValues[1].getType()).isEqualTo(WasmValueType.F64);
    assertThat(wasmValues[1].asDouble()).isEqualTo(3.14);
    assertThat(wasmValues[2].getType()).isEqualTo(WasmValueType.I64);
    assertThat(wasmValues[2].asLong()).isEqualTo(100L);
  }

  @Test
  void testNativeResultsToWasmValuesCountMismatch() {
    final Object[] nativeResults = {42, 3.14};
    final WasmValueType[] expectedTypes = {WasmValueType.I32};

    final JniValidationException exception =
        assertThrows(
            JniValidationException.class,
            () -> JniTypeConverter.nativeResultsToWasmValues(nativeResults, expectedTypes));

    assertThat(exception.getMessage()).contains("Result count mismatch: got 2, expected 1");
  }

  @Test
  void testValidateParameterTypes() {
    final WasmValue[] params = {WasmValue.i32(42), WasmValue.f64(3.14)};
    final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.F64};

    // Should not throw
    JniTypeConverter.validateParameterTypes(params, expectedTypes);
  }

  @Test
  void testValidateParameterTypesCountMismatch() {
    final WasmValue[] params = {WasmValue.i32(42)};
    final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.F64};

    final JniValidationException exception =
        assertThrows(
            JniValidationException.class,
            () -> JniTypeConverter.validateParameterTypes(params, expectedTypes));

    assertThat(exception.getMessage()).contains("Parameter count mismatch: got 1, expected 2");
  }

  @Test
  void testValidateParameterTypesTypeMismatch() {
    final WasmValue[] params = {WasmValue.i32(42), WasmValue.i32(100)};
    final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.F64};

    final JniValidationException exception =
        assertThrows(
            JniValidationException.class,
            () -> JniTypeConverter.validateParameterTypes(params, expectedTypes));

    assertThat(exception.getMessage())
        .contains("Parameter type mismatch at index 1: got I32, expected F64");
  }

  @Test
  void testValidateParameterTypesWithNullParam() {
    final WasmValue[] params = {WasmValue.i32(42), null};
    final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.F64};

    final JniValidationException exception =
        assertThrows(
            JniValidationException.class,
            () -> JniTypeConverter.validateParameterTypes(params, expectedTypes));

    assertThat(exception.getMessage()).contains("Parameter at index 1 is null");
  }

  @Test
  void testValidateV128Size() {
    final byte[] validBytes = new byte[16];
    JniTypeConverter.validateV128Size(validBytes); // Should not throw
  }

  @Test
  void testValidateV128SizeWithNull() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> JniTypeConverter.validateV128Size(null));

    assertThat(exception.getMessage()).contains("v128 bytes cannot be null");
  }

  @Test
  void testValidateV128SizeWithInvalidSize() {
    final byte[] invalidBytes = new byte[8];

    final JniValidationException exception =
        assertThrows(
            JniValidationException.class, () -> JniTypeConverter.validateV128Size(invalidBytes));

    assertThat(exception.getMessage()).contains("v128 must be exactly 16 bytes, got 8");
  }

  @Test
  void testCopyTypes() {
    final WasmValueType[] original = {WasmValueType.I32, WasmValueType.F64};
    final WasmValueType[] copy = JniTypeConverter.copyTypes(original);

    assertThat(copy).isEqualTo(original);
    assertThat(copy).isNotSameAs(original); // Should be a different array instance
  }

  @Test
  void testCopyTypesWithNull() {
    final WasmValueType[] copy = JniTypeConverter.copyTypes(null);
    assertThat(copy).isEmpty();
  }

  @Test
  void testTypesToStrings() {
    final WasmValueType[] types = {WasmValueType.I32, WasmValueType.V128, WasmValueType.FUNCREF};
    final String[] strings = JniTypeConverter.typesToStrings(types);

    assertThat(strings).containsExactly("i32", "v128", "funcref");
  }

  @Test
  void testTypesToStringsWithNullArray() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> JniTypeConverter.typesToStrings(null));

    assertThat(exception.getMessage()).contains("types").contains("must not be null");
  }

  @Test
  void testTypesToStringsWithNullElement() {
    final WasmValueType[] types = {WasmValueType.I32, null, WasmValueType.F64};

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> JniTypeConverter.typesToStrings(types));

    assertThat(exception.getMessage()).contains("Type at index 1 is null");
  }

  @Test
  void testStringsToTypes() {
    final String[] strings = {"i32", "v128", "funcref"};
    final WasmValueType[] types = JniTypeConverter.stringsToTypes(strings);

    assertThat(types).containsExactly(WasmValueType.I32, WasmValueType.V128, WasmValueType.FUNCREF);
  }

  @Test
  void testStringsToTypesWithNullArray() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> JniTypeConverter.stringsToTypes(null));

    assertThat(exception.getMessage()).contains("typeStrings").contains("must not be null");
  }

  @Test
  void testStringsToTypesWithNullElement() {
    final String[] strings = {"i32", null, "f64"};

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> JniTypeConverter.stringsToTypes(strings));

    assertThat(exception.getMessage()).contains("Type string at index 1 is null");
  }

  @Test
  void testRoundTripTypeConversion() {
    final WasmValueType[] originalTypes = {
      WasmValueType.I32,
      WasmValueType.I64,
      WasmValueType.F32,
      WasmValueType.F64,
      WasmValueType.V128,
      WasmValueType.FUNCREF,
      WasmValueType.EXTERNREF
    };

    final String[] strings = JniTypeConverter.typesToStrings(originalTypes);
    final WasmValueType[] convertedTypes = JniTypeConverter.stringsToTypes(strings);

    assertThat(convertedTypes).isEqualTo(originalTypes);
  }
}

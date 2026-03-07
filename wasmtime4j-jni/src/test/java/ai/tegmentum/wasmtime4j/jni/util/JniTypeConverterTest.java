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
package ai.tegmentum.wasmtime4j.jni.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
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
    assertEquals("i32", JniTypeConverter.typeToString(WasmValueType.I32));
    assertEquals("i64", JniTypeConverter.typeToString(WasmValueType.I64));
    assertEquals("f32", JniTypeConverter.typeToString(WasmValueType.F32));
    assertEquals("f64", JniTypeConverter.typeToString(WasmValueType.F64));
    assertEquals("v128", JniTypeConverter.typeToString(WasmValueType.V128));
    assertEquals("funcref", JniTypeConverter.typeToString(WasmValueType.FUNCREF));
    assertEquals("externref", JniTypeConverter.typeToString(WasmValueType.EXTERNREF));
  }

  @Test
  void testTypeToStringWithNull() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> JniTypeConverter.typeToString(null));

    assertTrue(exception.getMessage().contains("type"),
        "Expected message to contain: type");
    assertTrue(exception.getMessage().contains("must not be null"),
        "Expected message to contain: must not be null");
  }

  @Test
  void testStringToType() {
    assertEquals(WasmValueType.I32, JniTypeConverter.stringToType("i32"));
    assertEquals(WasmValueType.I64, JniTypeConverter.stringToType("i64"));
    assertEquals(WasmValueType.F32, JniTypeConverter.stringToType("f32"));
    assertEquals(WasmValueType.F64, JniTypeConverter.stringToType("f64"));
    assertEquals(WasmValueType.V128, JniTypeConverter.stringToType("v128"));
    assertEquals(WasmValueType.FUNCREF, JniTypeConverter.stringToType("funcref"));
    assertEquals(WasmValueType.EXTERNREF, JniTypeConverter.stringToType("externref"));
  }

  @Test
  void testStringToTypeWithMixedCase() {
    assertEquals(WasmValueType.I32, JniTypeConverter.stringToType("I32"));
    assertEquals(WasmValueType.F64, JniTypeConverter.stringToType("F64"));
    assertEquals(WasmValueType.V128, JniTypeConverter.stringToType("V128"));
    assertEquals(WasmValueType.FUNCREF, JniTypeConverter.stringToType("FuncRef"));
    assertEquals(WasmValueType.EXTERNREF, JniTypeConverter.stringToType("ExternRef"));
  }

  @Test
  void testStringToTypeWithNull() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> JniTypeConverter.stringToType(null));

    assertTrue(exception.getMessage().contains("typeString"),
        "Expected message to contain: typeString");
    assertTrue(exception.getMessage().contains("must not be null"),
        "Expected message to contain: must not be null");
  }

  @Test
  void testStringToTypeWithInvalidString() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> JniTypeConverter.stringToType("invalid"));

    assertTrue(
        exception.getMessage().contains("Invalid WebAssembly type string: invalid"),
        "Expected message to contain: Invalid WebAssembly type string: invalid");
  }

  @Test
  void testWasmValueToNativeParamBasicTypes() {
    // Test i32
    final WasmValue i32Value = WasmValue.i32(42);
    assertEquals(42, JniTypeConverter.wasmValueToNativeParam(i32Value));

    // Test i64
    final WasmValue i64Value = WasmValue.i64(100L);
    assertEquals(100L, JniTypeConverter.wasmValueToNativeParam(i64Value));

    // Test f32
    final WasmValue f32Value = WasmValue.f32(2.5f);
    assertEquals(2.5f, JniTypeConverter.wasmValueToNativeParam(f32Value));

    // Test f64
    final WasmValue f64Value = WasmValue.f64(2.718);
    assertEquals(2.718, JniTypeConverter.wasmValueToNativeParam(f64Value));
  }

  @Test
  void testWasmValueToNativeParamV128() {
    final byte[] v128Bytes = new byte[16];
    for (int i = 0; i < 16; i++) {
      v128Bytes[i] = (byte) i;
    }

    final WasmValue v128Value = WasmValue.v128(v128Bytes);
    final Object result = JniTypeConverter.wasmValueToNativeParam(v128Value);

    assertInstanceOf(byte[].class, result);
    assertEquals(16, ((byte[]) result).length);
    assertArrayEquals(v128Bytes, (byte[]) result);
  }

  @Test
  void testWasmValueToNativeParamV128InvalidSize() {
    final byte[] invalidBytes = new byte[8]; // Wrong size

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              final WasmValue v128Value = WasmValue.v128(invalidBytes);
              // This should fail during WasmValue.v128() creation, not in converter
            });

    assertTrue(
        exception.getMessage().contains("v128 value must be exactly 16 bytes"),
        "Expected message to contain: v128 value must be exactly 16 bytes");
  }

  @Test
  void testWasmValueToNativeParamReferenceTypes() {
    final Object funcRef = new Object();
    final WasmValue funcrefValue = WasmValue.funcref(funcRef);
    assertSame(funcRef, JniTypeConverter.wasmValueToNativeParam(funcrefValue));

    final String externRef = "external_data";
    final WasmValue externrefValue = WasmValue.externref(externRef);
    assertSame(externRef, JniTypeConverter.wasmValueToNativeParam(externrefValue));

    // Test null references
    final WasmValue nullFuncref = WasmValue.funcref(null);
    assertNull(JniTypeConverter.wasmValueToNativeParam(nullFuncref));

    final WasmValue nullExternref = WasmValue.externref(null);
    assertNull(JniTypeConverter.wasmValueToNativeParam(nullExternref));
  }

  @Test
  void testWasmValueToNativeParamWithNull() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> JniTypeConverter.wasmValueToNativeParam(null));

    assertTrue(exception.getMessage().contains("value"),
        "Expected message to contain: value");
    assertTrue(exception.getMessage().contains("must not be null"),
        "Expected message to contain: must not be null");
  }

  @Test
  void testWasmValuesToNativeParams() {
    final WasmValue[] values = {WasmValue.i32(42), WasmValue.f64(2.5), WasmValue.externref("test")};

    final Object[] nativeParams = JniTypeConverter.wasmValuesToNativeParams(values);

    assertEquals(3, nativeParams.length);
    assertEquals(42, nativeParams[0]);
    assertEquals(2.5, nativeParams[1]);
    assertEquals("test", nativeParams[2]);
  }

  @Test
  void testWasmValuesToNativeParamsWithNull() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> JniTypeConverter.wasmValuesToNativeParams(null));

    assertTrue(exception.getMessage().contains("values"),
        "Expected message to contain: values");
    assertTrue(exception.getMessage().contains("must not be null"),
        "Expected message to contain: must not be null");
  }

  @Test
  void testWasmValuesToNativeParamsWithNullElement() {
    final WasmValue[] values = {WasmValue.i32(42), null, WasmValue.f64(2.5)};

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JniTypeConverter.wasmValuesToNativeParams(values));

    assertTrue(
        exception.getMessage().contains("Parameter at index 1 is null"),
        "Expected message to contain: Parameter at index 1 is null");
  }

  @Test
  void testNativeResultToWasmValueBasicTypes() {
    // Test i32
    final WasmValue i32Result = JniTypeConverter.nativeResultToWasmValue(42, WasmValueType.I32);
    assertEquals(WasmValueType.I32, i32Result.getType());
    assertEquals(42, i32Result.asInt());

    // Test i64
    final WasmValue i64Result = JniTypeConverter.nativeResultToWasmValue(100L, WasmValueType.I64);
    assertEquals(WasmValueType.I64, i64Result.getType());
    assertEquals(100L, i64Result.asLong());

    // Test f32
    final WasmValue f32Result = JniTypeConverter.nativeResultToWasmValue(2.5f, WasmValueType.F32);
    assertEquals(WasmValueType.F32, f32Result.getType());
    assertEquals(2.5f, f32Result.asFloat());

    // Test f64
    final WasmValue f64Result = JniTypeConverter.nativeResultToWasmValue(2.718, WasmValueType.F64);
    assertEquals(WasmValueType.F64, f64Result.getType());
    assertEquals(2.718, f64Result.asDouble());
  }

  @Test
  void testNativeResultToWasmValueV128() {
    final byte[] v128Bytes = new byte[16];
    for (int i = 0; i < 16; i++) {
      v128Bytes[i] = (byte) (i * 2);
    }

    final WasmValue result =
        JniTypeConverter.nativeResultToWasmValue(v128Bytes, WasmValueType.V128);

    assertEquals(WasmValueType.V128, result.getType());
    assertArrayEquals(v128Bytes, result.asV128());
  }

  @Test
  void testNativeResultToWasmValueV128InvalidSize() {
    final byte[] invalidBytes = new byte[8]; // Wrong size

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JniTypeConverter.nativeResultToWasmValue(invalidBytes, WasmValueType.V128));

    assertTrue(
        exception.getMessage().contains("v128 result has invalid size: 8, expected 16"),
        "Expected message to contain: v128 result has invalid size: 8, expected 16");
  }

  @Test
  void testNativeResultToWasmValueReferenceTypes() {
    final Object funcRef = new Object();
    final WasmValue funcrefResult =
        JniTypeConverter.nativeResultToWasmValue(funcRef, WasmValueType.FUNCREF);
    assertEquals(WasmValueType.FUNCREF, funcrefResult.getType());
    assertSame(funcRef, funcrefResult.asFuncref());

    final String externRef = "external_data";
    final WasmValue externrefResult =
        JniTypeConverter.nativeResultToWasmValue(externRef, WasmValueType.EXTERNREF);
    assertEquals(WasmValueType.EXTERNREF, externrefResult.getType());
    assertSame(externRef, externrefResult.asExternref());

    // Test null references
    final WasmValue nullFuncrefResult =
        JniTypeConverter.nativeResultToWasmValue(null, WasmValueType.FUNCREF);
    assertEquals(WasmValueType.FUNCREF, nullFuncrefResult.getType());
    assertNull(nullFuncrefResult.asFuncref());
  }

  @Test
  void testNativeResultToWasmValueTypeMismatch() {
    // Try to convert String to i32
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JniTypeConverter.nativeResultToWasmValue("not_an_int", WasmValueType.I32));

    assertTrue(
        exception.getMessage().contains("Expected i32 result, got: String"),
        "Expected message to contain: Expected i32 result, got: String");
  }

  @Test
  void testNativeResultToWasmValueWithNullExpectedType() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JniTypeConverter.nativeResultToWasmValue(42, null));

    assertTrue(exception.getMessage().contains("expectedType"),
        "Expected message to contain: expectedType");
    assertTrue(exception.getMessage().contains("must not be null"),
        "Expected message to contain: must not be null");
  }

  @Test
  void testNativeResultsToWasmValues() {
    final Object[] nativeResults = {42, 2.5, 100L};
    final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.F64, WasmValueType.I64};

    final WasmValue[] wasmValues =
        JniTypeConverter.nativeResultsToWasmValues(nativeResults, expectedTypes);

    assertEquals(3, wasmValues.length);
    assertEquals(WasmValueType.I32, wasmValues[0].getType());
    assertEquals(42, wasmValues[0].asInt());
    assertEquals(WasmValueType.F64, wasmValues[1].getType());
    assertEquals(2.5, wasmValues[1].asDouble());
    assertEquals(WasmValueType.I64, wasmValues[2].getType());
    assertEquals(100L, wasmValues[2].asLong());
  }

  @Test
  void testNativeResultsToWasmValuesCountMismatch() {
    final Object[] nativeResults = {42, 2.5};
    final WasmValueType[] expectedTypes = {WasmValueType.I32};

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JniTypeConverter.nativeResultsToWasmValues(nativeResults, expectedTypes));

    assertTrue(
        exception.getMessage().contains("Result count mismatch: got 2, expected 1"),
        "Expected message to contain: Result count mismatch: got 2, expected 1");
  }

  @Test
  void testValidateParameterTypes() {
    final WasmValue[] params = {WasmValue.i32(42), WasmValue.f64(2.5)};
    final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.F64};

    // Should not throw
    JniTypeConverter.validateParameterTypes(params, expectedTypes);
  }

  @Test
  void testValidateParameterTypesCountMismatch() {
    final WasmValue[] params = {WasmValue.i32(42)};
    final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.F64};

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JniTypeConverter.validateParameterTypes(params, expectedTypes));

    assertTrue(
        exception.getMessage().contains("Parameter count mismatch: got 1, expected 2"),
        "Expected message to contain: Parameter count mismatch: got 1, expected 2");
  }

  @Test
  void testValidateParameterTypesTypeMismatch() {
    final WasmValue[] params = {WasmValue.i32(42), WasmValue.i32(100)};
    final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.F64};

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JniTypeConverter.validateParameterTypes(params, expectedTypes));

    assertTrue(
        exception.getMessage().contains("Parameter type mismatch at index 1: got I32, expected F64"),
        "Expected message to contain: Parameter type mismatch at index 1: got I32, expected F64");
  }

  @Test
  void testValidateParameterTypesWithNullParam() {
    final WasmValue[] params = {WasmValue.i32(42), null};
    final WasmValueType[] expectedTypes = {WasmValueType.I32, WasmValueType.F64};

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JniTypeConverter.validateParameterTypes(params, expectedTypes));

    assertTrue(
        exception.getMessage().contains("Parameter at index 1 is null"),
        "Expected message to contain: Parameter at index 1 is null");
  }

  @Test
  void testValidateV128Size() {
    final byte[] validBytes = new byte[16];
    JniTypeConverter.validateV128Size(validBytes); // Should not throw
  }

  @Test
  void testValidateV128SizeWithNull() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> JniTypeConverter.validateV128Size(null));

    assertTrue(
        exception.getMessage().contains("v128 bytes cannot be null"),
        "Expected message to contain: v128 bytes cannot be null");
  }

  @Test
  void testValidateV128SizeWithInvalidSize() {
    final byte[] invalidBytes = new byte[8];

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> JniTypeConverter.validateV128Size(invalidBytes));

    assertTrue(
        exception.getMessage().contains("v128 must be exactly 16 bytes, got 8"),
        "Expected message to contain: v128 must be exactly 16 bytes, got 8");
  }

  @Test
  void testCopyTypes() {
    final WasmValueType[] original = {WasmValueType.I32, WasmValueType.F64};
    final WasmValueType[] copy = JniTypeConverter.copyTypes(original);

    assertArrayEquals(original, copy);
    assertNotSame(original, copy); // Should be a different array instance
  }

  @Test
  void testCopyTypesWithNull() {
    final WasmValueType[] copy = JniTypeConverter.copyTypes(null);
    assertEquals(0, copy.length);
  }

  @Test
  void testTypesToStrings() {
    final WasmValueType[] types = {WasmValueType.I32, WasmValueType.V128, WasmValueType.FUNCREF};
    final String[] strings = JniTypeConverter.typesToStrings(types);

    assertArrayEquals(new String[] {"i32", "v128", "funcref"}, strings);
  }

  @Test
  void testTypesToStringsWithNullArray() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> JniTypeConverter.typesToStrings(null));

    assertTrue(exception.getMessage().contains("types"),
        "Expected message to contain: types");
    assertTrue(exception.getMessage().contains("must not be null"),
        "Expected message to contain: must not be null");
  }

  @Test
  void testTypesToStringsWithNullElement() {
    final WasmValueType[] types = {WasmValueType.I32, null, WasmValueType.F64};

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> JniTypeConverter.typesToStrings(types));

    assertTrue(
        exception.getMessage().contains("Type at index 1 is null"),
        "Expected message to contain: Type at index 1 is null");
  }

  @Test
  void testStringsToTypes() {
    final String[] strings = {"i32", "v128", "funcref"};
    final WasmValueType[] types = JniTypeConverter.stringsToTypes(strings);

    assertArrayEquals(
        new WasmValueType[] {WasmValueType.I32, WasmValueType.V128, WasmValueType.FUNCREF},
        types);
  }

  @Test
  void testStringsToTypesWithNullArray() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> JniTypeConverter.stringsToTypes(null));

    assertTrue(exception.getMessage().contains("typeStrings"),
        "Expected message to contain: typeStrings");
    assertTrue(exception.getMessage().contains("must not be null"),
        "Expected message to contain: must not be null");
  }

  @Test
  void testStringsToTypesWithNullElement() {
    final String[] strings = {"i32", null, "f64"};

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> JniTypeConverter.stringsToTypes(strings));

    assertTrue(
        exception.getMessage().contains("Type string at index 1 is null"),
        "Expected message to contain: Type string at index 1 is null");
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

    assertArrayEquals(originalTypes, convertedTypes);
  }
}

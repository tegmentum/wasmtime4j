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

package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.panama.MemoryLayouts;
import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaTypeConverter} class.
 *
 * <p>This test class verifies the type conversion utilities for Panama Foreign Function API.
 */
@DisplayName("PanamaTypeConverter Tests")
class PanamaTypeConverterTest {

  private Arena arena;

  @BeforeEach
  void setUp() {
    arena = Arena.ofConfined();
  }

  @AfterEach
  void tearDown() {
    if (arena.scope().isAlive()) {
      arena.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaTypeConverter should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaTypeConverter.class.getModifiers()),
          "PanamaTypeConverter should be final");
    }

    @Test
    @DisplayName("PanamaTypeConverter should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final var constructor = PanamaTypeConverter.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");
    }
  }

  @Nested
  @DisplayName("wasmTypeToNative Tests")
  class WasmTypeToNativeTests {

    @Test
    @DisplayName("wasmTypeToNative should convert I32")
    void wasmTypeToNativeShouldConvertI32() throws PanamaException {
      final int nativeType = PanamaTypeConverter.wasmTypeToNative(WasmValueType.I32);
      assertEquals(MemoryLayouts.WASM_I32, nativeType, "Should convert I32 correctly");
    }

    @Test
    @DisplayName("wasmTypeToNative should convert I64")
    void wasmTypeToNativeShouldConvertI64() throws PanamaException {
      final int nativeType = PanamaTypeConverter.wasmTypeToNative(WasmValueType.I64);
      assertEquals(MemoryLayouts.WASM_I64, nativeType, "Should convert I64 correctly");
    }

    @Test
    @DisplayName("wasmTypeToNative should convert F32")
    void wasmTypeToNativeShouldConvertF32() throws PanamaException {
      final int nativeType = PanamaTypeConverter.wasmTypeToNative(WasmValueType.F32);
      assertEquals(MemoryLayouts.WASM_F32, nativeType, "Should convert F32 correctly");
    }

    @Test
    @DisplayName("wasmTypeToNative should convert F64")
    void wasmTypeToNativeShouldConvertF64() throws PanamaException {
      final int nativeType = PanamaTypeConverter.wasmTypeToNative(WasmValueType.F64);
      assertEquals(MemoryLayouts.WASM_F64, nativeType, "Should convert F64 correctly");
    }

    @Test
    @DisplayName("wasmTypeToNative should convert V128")
    void wasmTypeToNativeShouldConvertV128() throws PanamaException {
      final int nativeType = PanamaTypeConverter.wasmTypeToNative(WasmValueType.V128);
      assertEquals(MemoryLayouts.WASM_V128, nativeType, "Should convert V128 correctly");
    }

    @Test
    @DisplayName("wasmTypeToNative should convert FUNCREF")
    void wasmTypeToNativeShouldConvertFuncref() throws PanamaException {
      final int nativeType = PanamaTypeConverter.wasmTypeToNative(WasmValueType.FUNCREF);
      assertEquals(MemoryLayouts.WASM_FUNCREF, nativeType, "Should convert FUNCREF correctly");
    }

    @Test
    @DisplayName("wasmTypeToNative should convert EXTERNREF")
    void wasmTypeToNativeShouldConvertExternref() throws PanamaException {
      final int nativeType = PanamaTypeConverter.wasmTypeToNative(WasmValueType.EXTERNREF);
      assertEquals(MemoryLayouts.WASM_ANYREF, nativeType, "Should convert EXTERNREF correctly");
    }

    @Test
    @DisplayName("wasmTypeToNative should throw for null type")
    void wasmTypeToNativeShouldThrowForNullType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.wasmTypeToNative(null),
          "Should throw for null type");
    }
  }

  @Nested
  @DisplayName("nativeToWasmType Tests")
  class NativeToWasmTypeTests {

    @Test
    @DisplayName("nativeToWasmType should convert WASM_I32")
    void nativeToWasmTypeShouldConvertI32() throws PanamaException {
      final WasmValueType type = PanamaTypeConverter.nativeToWasmType(MemoryLayouts.WASM_I32);
      assertEquals(WasmValueType.I32, type, "Should convert WASM_I32 correctly");
    }

    @Test
    @DisplayName("nativeToWasmType should convert WASM_I64")
    void nativeToWasmTypeShouldConvertI64() throws PanamaException {
      final WasmValueType type = PanamaTypeConverter.nativeToWasmType(MemoryLayouts.WASM_I64);
      assertEquals(WasmValueType.I64, type, "Should convert WASM_I64 correctly");
    }

    @Test
    @DisplayName("nativeToWasmType should convert WASM_F32")
    void nativeToWasmTypeShouldConvertF32() throws PanamaException {
      final WasmValueType type = PanamaTypeConverter.nativeToWasmType(MemoryLayouts.WASM_F32);
      assertEquals(WasmValueType.F32, type, "Should convert WASM_F32 correctly");
    }

    @Test
    @DisplayName("nativeToWasmType should convert WASM_F64")
    void nativeToWasmTypeShouldConvertF64() throws PanamaException {
      final WasmValueType type = PanamaTypeConverter.nativeToWasmType(MemoryLayouts.WASM_F64);
      assertEquals(WasmValueType.F64, type, "Should convert WASM_F64 correctly");
    }

    @Test
    @DisplayName("nativeToWasmType should convert WASM_V128")
    void nativeToWasmTypeShouldConvertV128() throws PanamaException {
      final WasmValueType type = PanamaTypeConverter.nativeToWasmType(MemoryLayouts.WASM_V128);
      assertEquals(WasmValueType.V128, type, "Should convert WASM_V128 correctly");
    }

    @Test
    @DisplayName("nativeToWasmType should convert WASM_FUNCREF")
    void nativeToWasmTypeShouldConvertFuncref() throws PanamaException {
      final WasmValueType type = PanamaTypeConverter.nativeToWasmType(MemoryLayouts.WASM_FUNCREF);
      assertEquals(WasmValueType.FUNCREF, type, "Should convert WASM_FUNCREF correctly");
    }

    @Test
    @DisplayName("nativeToWasmType should convert WASM_ANYREF")
    void nativeToWasmTypeShouldConvertExternref() throws PanamaException {
      final WasmValueType type = PanamaTypeConverter.nativeToWasmType(MemoryLayouts.WASM_ANYREF);
      assertEquals(WasmValueType.EXTERNREF, type, "Should convert WASM_ANYREF correctly");
    }

    @Test
    @DisplayName("nativeToWasmType should throw for invalid native type")
    void nativeToWasmTypeShouldThrowForInvalidNativeType() {
      assertThrows(
          PanamaException.class,
          () -> PanamaTypeConverter.nativeToWasmType(-999),
          "Should throw for invalid native type");
    }
  }

  @Nested
  @DisplayName("marshalWasmValue Tests")
  class MarshalWasmValueTests {

    @Test
    @DisplayName("marshalWasmValue should marshal I32 value")
    void marshalWasmValueShouldMarshalI32() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      final WasmValue value = WasmValue.i32(42);

      PanamaTypeConverter.marshalWasmValue(value, valueSlot);

      final int kind = (Integer) MemoryLayouts.WASM_VAL_KIND.get(valueSlot, 0L);
      final int marshalledValue = (Integer) MemoryLayouts.WASM_VAL_I32.get(valueSlot, 0L);

      assertEquals(MemoryLayouts.WASM_I32, kind, "Kind should be I32");
      assertEquals(42, marshalledValue, "Value should be 42");
    }

    @Test
    @DisplayName("marshalWasmValue should marshal I64 value")
    void marshalWasmValueShouldMarshalI64() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      final WasmValue value = WasmValue.i64(123456789L);

      PanamaTypeConverter.marshalWasmValue(value, valueSlot);

      final int kind = (Integer) MemoryLayouts.WASM_VAL_KIND.get(valueSlot, 0L);
      final long marshalledValue = (Long) MemoryLayouts.WASM_VAL_I64.get(valueSlot, 0L);

      assertEquals(MemoryLayouts.WASM_I64, kind, "Kind should be I64");
      assertEquals(123456789L, marshalledValue, "Value should be 123456789");
    }

    @Test
    @DisplayName("marshalWasmValue should marshal F32 value")
    void marshalWasmValueShouldMarshalF32() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      final WasmValue value = WasmValue.f32(3.14f);

      PanamaTypeConverter.marshalWasmValue(value, valueSlot);

      final int kind = (Integer) MemoryLayouts.WASM_VAL_KIND.get(valueSlot, 0L);
      final float marshalledValue = (Float) MemoryLayouts.WASM_VAL_F32.get(valueSlot, 0L);

      assertEquals(MemoryLayouts.WASM_F32, kind, "Kind should be F32");
      assertEquals(3.14f, marshalledValue, 0.001f, "Value should be 3.14");
    }

    @Test
    @DisplayName("marshalWasmValue should marshal F64 value")
    void marshalWasmValueShouldMarshalF64() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      final WasmValue value = WasmValue.f64(2.71828);

      PanamaTypeConverter.marshalWasmValue(value, valueSlot);

      final int kind = (Integer) MemoryLayouts.WASM_VAL_KIND.get(valueSlot, 0L);
      final double marshalledValue = (Double) MemoryLayouts.WASM_VAL_F64.get(valueSlot, 0L);

      assertEquals(MemoryLayouts.WASM_F64, kind, "Kind should be F64");
      assertEquals(2.71828, marshalledValue, 0.00001, "Value should be 2.71828");
    }

    @Test
    @DisplayName("marshalWasmValue should marshal V128 value")
    void marshalWasmValueShouldMarshalV128() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      final byte[] v128Bytes = new byte[16];
      for (int i = 0; i < 16; i++) {
        v128Bytes[i] = (byte) i;
      }
      final WasmValue value = WasmValue.v128(v128Bytes);

      PanamaTypeConverter.marshalWasmValue(value, valueSlot);

      final int kind = (Integer) MemoryLayouts.WASM_VAL_KIND.get(valueSlot, 0L);
      assertEquals(MemoryLayouts.WASM_V128, kind, "Kind should be V128");
    }

    @Test
    @DisplayName("marshalWasmValue should marshal null funcref")
    void marshalWasmValueShouldMarshalNullFuncref() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      final WasmValue value = WasmValue.funcref(null);

      PanamaTypeConverter.marshalWasmValue(value, valueSlot);

      final int kind = (Integer) MemoryLayouts.WASM_VAL_KIND.get(valueSlot, 0L);
      final MemorySegment refSegment =
          (MemorySegment) MemoryLayouts.WASM_VAL_REF.get(valueSlot, 0L);

      assertEquals(MemoryLayouts.WASM_FUNCREF, kind, "Kind should be FUNCREF");
      assertEquals(0L, refSegment.address(), "Null funcref should have ref address 0");
    }

    @Test
    @DisplayName("marshalWasmValue should marshal null externref")
    void marshalWasmValueShouldMarshalNullExternref() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      final WasmValue value = WasmValue.externref(null);

      PanamaTypeConverter.marshalWasmValue(value, valueSlot);

      final int kind = (Integer) MemoryLayouts.WASM_VAL_KIND.get(valueSlot, 0L);
      final MemorySegment refSegment =
          (MemorySegment) MemoryLayouts.WASM_VAL_REF.get(valueSlot, 0L);

      assertEquals(MemoryLayouts.WASM_ANYREF, kind, "Kind should be EXTERNREF");
      assertEquals(0L, refSegment.address(), "Null externref should have ref address 0");
    }

    @Test
    @DisplayName("marshalWasmValue should throw for null wasmValue")
    void marshalWasmValueShouldThrowForNullWasmValue() {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.marshalWasmValue(null, valueSlot),
          "Should throw for null wasmValue");
    }

    @Test
    @DisplayName("marshalWasmValue should throw for null valueSlot")
    void marshalWasmValueShouldThrowForNullValueSlot() {
      final WasmValue value = WasmValue.i32(42);

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.marshalWasmValue(value, null),
          "Should throw for null valueSlot");
    }
  }

  @Nested
  @DisplayName("unmarshalWasmValue Tests")
  class UnmarshalWasmValueTests {

    @Test
    @DisplayName("unmarshalWasmValue should unmarshal I32 value")
    void unmarshalWasmValueShouldUnmarshalI32() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      MemoryLayouts.WASM_VAL_KIND.set(valueSlot, 0L, MemoryLayouts.WASM_I32);
      MemoryLayouts.WASM_VAL_I32.set(valueSlot, 0L, 42);

      final WasmValue result = PanamaTypeConverter.unmarshalWasmValue(valueSlot, WasmValueType.I32);

      assertEquals(WasmValueType.I32, result.getType(), "Type should be I32");
      assertEquals(42, result.asI32(), "Value should be 42");
    }

    @Test
    @DisplayName("unmarshalWasmValue should unmarshal I64 value")
    void unmarshalWasmValueShouldUnmarshalI64() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      MemoryLayouts.WASM_VAL_KIND.set(valueSlot, 0L, MemoryLayouts.WASM_I64);
      MemoryLayouts.WASM_VAL_I64.set(valueSlot, 0L, 123456789L);

      final WasmValue result = PanamaTypeConverter.unmarshalWasmValue(valueSlot, WasmValueType.I64);

      assertEquals(WasmValueType.I64, result.getType(), "Type should be I64");
      assertEquals(123456789L, result.asI64(), "Value should be 123456789");
    }

    @Test
    @DisplayName("unmarshalWasmValue should unmarshal F32 value")
    void unmarshalWasmValueShouldUnmarshalF32() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      MemoryLayouts.WASM_VAL_KIND.set(valueSlot, 0L, MemoryLayouts.WASM_F32);
      MemoryLayouts.WASM_VAL_F32.set(valueSlot, 0L, 3.14f);

      final WasmValue result = PanamaTypeConverter.unmarshalWasmValue(valueSlot, WasmValueType.F32);

      assertEquals(WasmValueType.F32, result.getType(), "Type should be F32");
      assertEquals(3.14f, result.asF32(), 0.001f, "Value should be 3.14");
    }

    @Test
    @DisplayName("unmarshalWasmValue should unmarshal F64 value")
    void unmarshalWasmValueShouldUnmarshalF64() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      MemoryLayouts.WASM_VAL_KIND.set(valueSlot, 0L, MemoryLayouts.WASM_F64);
      MemoryLayouts.WASM_VAL_F64.set(valueSlot, 0L, 2.71828);

      final WasmValue result = PanamaTypeConverter.unmarshalWasmValue(valueSlot, WasmValueType.F64);

      assertEquals(WasmValueType.F64, result.getType(), "Type should be F64");
      assertEquals(2.71828, result.asF64(), 0.00001, "Value should be 2.71828");
    }

    @Test
    @DisplayName("unmarshalWasmValue should throw for type mismatch")
    void unmarshalWasmValueShouldThrowForTypeMismatch() {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      MemoryLayouts.WASM_VAL_KIND.set(valueSlot, 0L, MemoryLayouts.WASM_I32);

      assertThrows(
          PanamaException.class,
          () -> PanamaTypeConverter.unmarshalWasmValue(valueSlot, WasmValueType.I64),
          "Should throw for type mismatch");
    }

    @Test
    @DisplayName("unmarshalWasmValue should throw for null valueSlot")
    void unmarshalWasmValueShouldThrowForNullValueSlot() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.unmarshalWasmValue(null, WasmValueType.I32),
          "Should throw for null valueSlot");
    }

    @Test
    @DisplayName("unmarshalWasmValue should throw for null expectedType")
    void unmarshalWasmValueShouldThrowForNullExpectedType() {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.unmarshalWasmValue(valueSlot, null),
          "Should throw for null expectedType");
    }
  }

  @Nested
  @DisplayName("marshalParameters Tests")
  class MarshalParametersTests {

    @Test
    @DisplayName("marshalParameters should marshal array of values")
    void marshalParametersShouldMarshalArrayOfValues() throws PanamaException {
      final WasmValue[] values =
          new WasmValue[] {WasmValue.i32(1), WasmValue.i64(2L), WasmValue.f32(3.0f)};
      final MemorySegment paramsMemory =
          arena.allocate(values.length * MemoryLayouts.WASM_VAL.byteSize());

      PanamaTypeConverter.marshalParameters(values, paramsMemory);

      // Verify first parameter
      final MemorySegment slot0 = paramsMemory.asSlice(0, MemoryLayouts.WASM_VAL.byteSize());
      assertEquals(MemoryLayouts.WASM_I32, (Integer) MemoryLayouts.WASM_VAL_KIND.get(slot0, 0L));
      assertEquals(1, (Integer) MemoryLayouts.WASM_VAL_I32.get(slot0, 0L));

      // Verify second parameter
      final MemorySegment slot1 =
          paramsMemory.asSlice(
              MemoryLayouts.WASM_VAL.byteSize(), MemoryLayouts.WASM_VAL.byteSize());
      assertEquals(MemoryLayouts.WASM_I64, (Integer) MemoryLayouts.WASM_VAL_KIND.get(slot1, 0L));
      assertEquals(2L, (Long) MemoryLayouts.WASM_VAL_I64.get(slot1, 0L));
    }

    @Test
    @DisplayName("marshalParameters should handle empty array")
    void marshalParametersShouldHandleEmptyArray() throws PanamaException {
      final WasmValue[] values = new WasmValue[0];
      final MemorySegment paramsMemory = arena.allocate(1);

      assertDoesNotThrow(
          () -> PanamaTypeConverter.marshalParameters(values, paramsMemory),
          "Should handle empty array");
    }

    @Test
    @DisplayName("marshalParameters should throw for null values")
    void marshalParametersShouldThrowForNullValues() {
      final MemorySegment paramsMemory = arena.allocate(MemoryLayouts.WASM_VAL.byteSize());

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.marshalParameters(null, paramsMemory),
          "Should throw for null values");
    }

    @Test
    @DisplayName("marshalParameters should throw for null paramsMemory")
    void marshalParametersShouldThrowForNullParamsMemory() {
      final WasmValue[] values = new WasmValue[] {WasmValue.i32(1)};

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.marshalParameters(values, null),
          "Should throw for null paramsMemory");
    }

    @Test
    @DisplayName("marshalParameters should throw for null value in array")
    void marshalParametersShouldThrowForNullValueInArray() {
      final WasmValue[] values = new WasmValue[] {WasmValue.i32(1), null, WasmValue.i32(3)};
      final MemorySegment paramsMemory =
          arena.allocate(values.length * MemoryLayouts.WASM_VAL.byteSize());

      assertThrows(
          PanamaException.class,
          () -> PanamaTypeConverter.marshalParameters(values, paramsMemory),
          "Should throw for null value in array");
    }
  }

  @Nested
  @DisplayName("unmarshalResults Tests")
  class UnmarshalResultsTests {

    @Test
    @DisplayName("unmarshalResults should unmarshal array of results")
    void unmarshalResultsShouldUnmarshalArrayOfResults() throws PanamaException {
      final WasmValueType[] expectedTypes =
          new WasmValueType[] {WasmValueType.I32, WasmValueType.I64};
      final MemorySegment resultsMemory =
          arena.allocate(expectedTypes.length * MemoryLayouts.WASM_VAL.byteSize());

      // Set up first result
      final MemorySegment slot0 = resultsMemory.asSlice(0, MemoryLayouts.WASM_VAL.byteSize());
      MemoryLayouts.WASM_VAL_KIND.set(slot0, 0L, MemoryLayouts.WASM_I32);
      MemoryLayouts.WASM_VAL_I32.set(slot0, 0L, 100);

      // Set up second result
      final MemorySegment slot1 =
          resultsMemory.asSlice(
              MemoryLayouts.WASM_VAL.byteSize(), MemoryLayouts.WASM_VAL.byteSize());
      MemoryLayouts.WASM_VAL_KIND.set(slot1, 0L, MemoryLayouts.WASM_I64);
      MemoryLayouts.WASM_VAL_I64.set(slot1, 0L, 200L);

      final WasmValue[] results =
          PanamaTypeConverter.unmarshalResults(resultsMemory, expectedTypes);

      assertEquals(2, results.length, "Should return 2 results");
      assertEquals(100, results[0].asI32(), "First result should be 100");
      assertEquals(200L, results[1].asI64(), "Second result should be 200");
    }

    @Test
    @DisplayName("unmarshalResults should handle empty types array")
    void unmarshalResultsShouldHandleEmptyTypesArray() throws PanamaException {
      final WasmValueType[] expectedTypes = new WasmValueType[0];
      final MemorySegment resultsMemory = arena.allocate(1);

      final WasmValue[] results =
          PanamaTypeConverter.unmarshalResults(resultsMemory, expectedTypes);

      assertEquals(0, results.length, "Should return empty array");
    }

    @Test
    @DisplayName("unmarshalResults should throw for null resultsMemory")
    void unmarshalResultsShouldThrowForNullResultsMemory() {
      final WasmValueType[] expectedTypes = new WasmValueType[] {WasmValueType.I32};

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.unmarshalResults(null, expectedTypes),
          "Should throw for null resultsMemory");
    }

    @Test
    @DisplayName("unmarshalResults should throw for null expectedTypes")
    void unmarshalResultsShouldThrowForNullExpectedTypes() {
      final MemorySegment resultsMemory = arena.allocate(MemoryLayouts.WASM_VAL.byteSize());

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.unmarshalResults(resultsMemory, null),
          "Should throw for null expectedTypes");
    }
  }

  @Nested
  @DisplayName("validateParameterTypes Tests")
  class ValidateParameterTypesTests {

    @Test
    @DisplayName("validateParameterTypes should pass for matching types")
    void validateParameterTypesShouldPassForMatchingTypes() throws PanamaException {
      final WasmValue[] params =
          new WasmValue[] {WasmValue.i32(1), WasmValue.i64(2L), WasmValue.f32(3.0f)};
      final WasmValueType[] expectedTypes =
          new WasmValueType[] {WasmValueType.I32, WasmValueType.I64, WasmValueType.F32};

      assertDoesNotThrow(
          () -> PanamaTypeConverter.validateParameterTypes(params, expectedTypes),
          "Should pass for matching types");
    }

    @Test
    @DisplayName("validateParameterTypes should throw for count mismatch")
    void validateParameterTypesShouldThrowForCountMismatch() {
      final WasmValue[] params = new WasmValue[] {WasmValue.i32(1)};
      final WasmValueType[] expectedTypes =
          new WasmValueType[] {WasmValueType.I32, WasmValueType.I64};

      final PanamaException exception =
          assertThrows(
              PanamaException.class,
              () -> PanamaTypeConverter.validateParameterTypes(params, expectedTypes));
      assertTrue(
          exception.getMessage().contains("count mismatch"),
          "Message should mention count mismatch");
    }

    @Test
    @DisplayName("validateParameterTypes should throw for type mismatch")
    void validateParameterTypesShouldThrowForTypeMismatch() {
      final WasmValue[] params = new WasmValue[] {WasmValue.i32(1)};
      final WasmValueType[] expectedTypes = new WasmValueType[] {WasmValueType.I64};

      final PanamaException exception =
          assertThrows(
              PanamaException.class,
              () -> PanamaTypeConverter.validateParameterTypes(params, expectedTypes));
      assertTrue(
          exception.getMessage().contains("type mismatch"), "Message should mention type mismatch");
    }

    @Test
    @DisplayName("validateParameterTypes should throw for null parameter")
    void validateParameterTypesShouldThrowForNullParameter() {
      final WasmValue[] params = new WasmValue[] {null};
      final WasmValueType[] expectedTypes = new WasmValueType[] {WasmValueType.I32};

      assertThrows(
          PanamaException.class,
          () -> PanamaTypeConverter.validateParameterTypes(params, expectedTypes),
          "Should throw for null parameter");
    }

    @Test
    @DisplayName("validateParameterTypes should throw for null params")
    void validateParameterTypesShouldThrowForNullParams() {
      final WasmValueType[] expectedTypes = new WasmValueType[] {WasmValueType.I32};

      assertThrows(
          PanamaException.class,
          () -> PanamaTypeConverter.validateParameterTypes(null, expectedTypes),
          "Should throw for null params");
    }

    @Test
    @DisplayName("validateParameterTypes should throw for null expectedTypes")
    void validateParameterTypesShouldThrowForNullExpectedTypes() {
      final WasmValue[] params = new WasmValue[] {WasmValue.i32(1)};

      assertThrows(
          PanamaException.class,
          () -> PanamaTypeConverter.validateParameterTypes(params, null),
          "Should throw for null expectedTypes");
    }
  }

  @Nested
  @DisplayName("validateV128Size Tests")
  class ValidateV128SizeTests {

    @Test
    @DisplayName("validateV128Size should pass for 16 bytes")
    void validateV128SizeShouldPassFor16Bytes() {
      final byte[] bytes = new byte[16];

      assertDoesNotThrow(
          () -> PanamaTypeConverter.validateV128Size(bytes), "Should pass for 16 bytes");
    }

    @Test
    @DisplayName("validateV128Size should throw for null bytes")
    void validateV128SizeShouldThrowForNullBytes() {
      assertThrows(
          PanamaException.class,
          () -> PanamaTypeConverter.validateV128Size(null),
          "Should throw for null bytes");
    }

    @Test
    @DisplayName("validateV128Size should throw for too few bytes")
    void validateV128SizeShouldThrowForTooFewBytes() {
      final byte[] bytes = new byte[15];

      final PanamaException exception =
          assertThrows(PanamaException.class, () -> PanamaTypeConverter.validateV128Size(bytes));
      assertTrue(
          exception.getMessage().contains("16 bytes"), "Message should mention required size");
    }

    @Test
    @DisplayName("validateV128Size should throw for too many bytes")
    void validateV128SizeShouldThrowForTooManyBytes() {
      final byte[] bytes = new byte[17];

      final PanamaException exception =
          assertThrows(PanamaException.class, () -> PanamaTypeConverter.validateV128Size(bytes));
      assertTrue(
          exception.getMessage().contains("16 bytes"), "Message should mention required size");
    }
  }

  @Nested
  @DisplayName("functionTypeToNative Tests")
  class FunctionTypeToNativeTests {

    @Test
    @DisplayName("functionTypeToNative should convert function type")
    void functionTypeToNativeShouldConvertFunctionType() throws PanamaException {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64},
              new WasmValueType[] {WasmValueType.F64});

      final int[][] result = PanamaTypeConverter.functionTypeToNative(funcType);

      assertEquals(2, result.length, "Should return 2 arrays");
      assertEquals(2, result[0].length, "Param types should have 2 elements");
      assertEquals(1, result[1].length, "Return types should have 1 element");
      assertEquals(MemoryLayouts.WASM_I32, result[0][0], "First param should be I32");
      assertEquals(MemoryLayouts.WASM_I64, result[0][1], "Second param should be I64");
      assertEquals(MemoryLayouts.WASM_F64, result[1][0], "Return type should be F64");
    }

    @Test
    @DisplayName("functionTypeToNative should handle empty params")
    void functionTypeToNativeShouldHandleEmptyParams() throws PanamaException {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

      final int[][] result = PanamaTypeConverter.functionTypeToNative(funcType);

      assertEquals(0, result[0].length, "Param types should be empty");
      assertEquals(1, result[1].length, "Return types should have 1 element");
    }

    @Test
    @DisplayName("functionTypeToNative should handle empty returns")
    void functionTypeToNativeShouldHandleEmptyReturns() throws PanamaException {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[0]);

      final int[][] result = PanamaTypeConverter.functionTypeToNative(funcType);

      assertEquals(1, result[0].length, "Param types should have 1 element");
      assertEquals(0, result[1].length, "Return types should be empty");
    }

    @Test
    @DisplayName("functionTypeToNative should throw for null functionType")
    void functionTypeToNativeShouldThrowForNullFunctionType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.functionTypeToNative(null),
          "Should throw for null functionType");
    }
  }

  @Nested
  @DisplayName("nativeToFunctionType Tests")
  class NativeToFunctionTypeTests {

    @Test
    @DisplayName("nativeToFunctionType should create function type")
    void nativeToFunctionTypeShouldCreateFunctionType() throws PanamaException {
      final int[] nativeParamTypes = new int[] {MemoryLayouts.WASM_I32, MemoryLayouts.WASM_I64};
      final int[] nativeReturnTypes = new int[] {MemoryLayouts.WASM_F64};

      final FunctionType result =
          PanamaTypeConverter.nativeToFunctionType(nativeParamTypes, nativeReturnTypes);

      assertNotNull(result, "Should return function type");
      assertEquals(2, result.getParamTypes().length, "Should have 2 param types");
      assertEquals(1, result.getReturnTypes().length, "Should have 1 return type");
      assertEquals(WasmValueType.I32, result.getParamTypes()[0], "First param should be I32");
      assertEquals(WasmValueType.I64, result.getParamTypes()[1], "Second param should be I64");
      assertEquals(WasmValueType.F64, result.getReturnTypes()[0], "Return type should be F64");
    }

    @Test
    @DisplayName("nativeToFunctionType should handle empty arrays")
    void nativeToFunctionTypeShouldHandleEmptyArrays() throws PanamaException {
      final int[] nativeParamTypes = new int[0];
      final int[] nativeReturnTypes = new int[0];

      final FunctionType result =
          PanamaTypeConverter.nativeToFunctionType(nativeParamTypes, nativeReturnTypes);

      assertEquals(0, result.getParamTypes().length, "Should have no param types");
      assertEquals(0, result.getReturnTypes().length, "Should have no return types");
    }

    @Test
    @DisplayName("nativeToFunctionType should throw for null nativeParamTypes")
    void nativeToFunctionTypeShouldThrowForNullNativeParamTypes() {
      final int[] nativeReturnTypes = new int[] {MemoryLayouts.WASM_I32};

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.nativeToFunctionType(null, nativeReturnTypes),
          "Should throw for null nativeParamTypes");
    }

    @Test
    @DisplayName("nativeToFunctionType should throw for null nativeReturnTypes")
    void nativeToFunctionTypeShouldThrowForNullNativeReturnTypes() {
      final int[] nativeParamTypes = new int[] {MemoryLayouts.WASM_I32};

      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.nativeToFunctionType(nativeParamTypes, null),
          "Should throw for null nativeReturnTypes");
    }
  }

  @Nested
  @DisplayName("calculateValuesMemorySize Tests")
  class CalculateValuesMemorySizeTests {

    @Test
    @DisplayName("calculateValuesMemorySize should calculate size for types")
    void calculateValuesMemorySizeShouldCalculateSizeForTypes() {
      final WasmValueType[] types =
          new WasmValueType[] {WasmValueType.I32, WasmValueType.I64, WasmValueType.F32};

      final long size = PanamaTypeConverter.calculateValuesMemorySize(types);

      assertEquals(3 * MemoryLayouts.WASM_VAL.byteSize(), size, "Should calculate correct size");
    }

    @Test
    @DisplayName("calculateValuesMemorySize should return 0 for null types")
    void calculateValuesMemorySizeShouldReturnZeroForNullTypes() {
      final long size = PanamaTypeConverter.calculateValuesMemorySize(null);

      assertEquals(0, size, "Should return 0 for null types");
    }

    @Test
    @DisplayName("calculateValuesMemorySize should return 0 for empty types")
    void calculateValuesMemorySizeShouldReturnZeroForEmptyTypes() {
      final WasmValueType[] types = new WasmValueType[0];

      final long size = PanamaTypeConverter.calculateValuesMemorySize(types);

      assertEquals(0, size, "Should return 0 for empty types");
    }
  }

  @Nested
  @DisplayName("validateReferenceTypes Tests")
  class ValidateReferenceTypesTests {

    @Test
    @DisplayName("validateReferenceTypes should pass for basic types")
    void validateReferenceTypesShouldPassForBasicTypes() {
      final WasmValue[] values =
          new WasmValue[] {WasmValue.i32(1), WasmValue.i64(2L), WasmValue.f32(3.0f)};

      assertDoesNotThrow(
          () -> PanamaTypeConverter.validateReferenceTypes(values), "Should pass for basic types");
    }

    @Test
    @DisplayName("validateReferenceTypes should pass for null references")
    void validateReferenceTypesShouldPassForNullReferences() {
      final WasmValue[] values =
          new WasmValue[] {WasmValue.funcref(null), WasmValue.externref(null)};

      assertDoesNotThrow(
          () -> PanamaTypeConverter.validateReferenceTypes(values),
          "Should pass for null references");
    }

    @Test
    @DisplayName("validateReferenceTypes should throw for null values array")
    void validateReferenceTypesShouldThrowForNullValuesArray() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PanamaTypeConverter.validateReferenceTypes(null),
          "Should throw for null values array");
    }

    @Test
    @DisplayName("validateReferenceTypes should throw for null value in array")
    void validateReferenceTypesShouldThrowForNullValueInArray() {
      final WasmValue[] values = new WasmValue[] {WasmValue.i32(1), null};

      assertThrows(
          PanamaException.class,
          () -> PanamaTypeConverter.validateReferenceTypes(values),
          "Should throw for null value in array");
    }
  }

  @Nested
  @DisplayName("Round-Trip Tests")
  class RoundTripTests {

    @Test
    @DisplayName("I32 round-trip should preserve value")
    void i32RoundTripShouldPreserveValue() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      final WasmValue original = WasmValue.i32(-12345);

      PanamaTypeConverter.marshalWasmValue(original, valueSlot);
      final WasmValue result = PanamaTypeConverter.unmarshalWasmValue(valueSlot, WasmValueType.I32);

      assertEquals(original.asI32(), result.asI32(), "I32 value should be preserved");
    }

    @Test
    @DisplayName("I64 round-trip should preserve value")
    void i64RoundTripShouldPreserveValue() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      final WasmValue original = WasmValue.i64(Long.MAX_VALUE - 1000);

      PanamaTypeConverter.marshalWasmValue(original, valueSlot);
      final WasmValue result = PanamaTypeConverter.unmarshalWasmValue(valueSlot, WasmValueType.I64);

      assertEquals(original.asI64(), result.asI64(), "I64 value should be preserved");
    }

    @Test
    @DisplayName("F32 round-trip should preserve value")
    void f32RoundTripShouldPreserveValue() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      final WasmValue original = WasmValue.f32(Float.MIN_VALUE);

      PanamaTypeConverter.marshalWasmValue(original, valueSlot);
      final WasmValue result = PanamaTypeConverter.unmarshalWasmValue(valueSlot, WasmValueType.F32);

      assertEquals(original.asF32(), result.asF32(), "F32 value should be preserved");
    }

    @Test
    @DisplayName("F64 round-trip should preserve value")
    void f64RoundTripShouldPreserveValue() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      final WasmValue original = WasmValue.f64(Double.MAX_VALUE);

      PanamaTypeConverter.marshalWasmValue(original, valueSlot);
      final WasmValue result = PanamaTypeConverter.unmarshalWasmValue(valueSlot, WasmValueType.F64);

      assertEquals(original.asF64(), result.asF64(), "F64 value should be preserved");
    }

    @Test
    @DisplayName("V128 round-trip should preserve value")
    void v128RoundTripShouldPreserveValue() throws PanamaException {
      final MemorySegment valueSlot = arena.allocate(MemoryLayouts.WASM_VAL);
      final byte[] v128Bytes = new byte[16];
      for (int i = 0; i < 16; i++) {
        v128Bytes[i] = (byte) (i * 10);
      }
      final WasmValue original = WasmValue.v128(v128Bytes);

      PanamaTypeConverter.marshalWasmValue(original, valueSlot);
      final WasmValue result =
          PanamaTypeConverter.unmarshalWasmValue(valueSlot, WasmValueType.V128);

      assertArrayEquals(original.asV128(), result.asV128(), "V128 value should be preserved");
    }

    @Test
    @DisplayName("FunctionType round-trip should preserve signature")
    void functionTypeRoundTripShouldPreserveSignature() throws PanamaException {
      final FunctionType original =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.F64, WasmValueType.V128},
              new WasmValueType[] {WasmValueType.I64, WasmValueType.F32});

      final int[][] nativeTypes = PanamaTypeConverter.functionTypeToNative(original);
      final FunctionType result =
          PanamaTypeConverter.nativeToFunctionType(nativeTypes[0], nativeTypes[1]);

      assertArrayEquals(
          original.getParamTypes(), result.getParamTypes(), "Param types should be preserved");
      assertArrayEquals(
          original.getReturnTypes(), result.getReturnTypes(), "Return types should be preserved");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full type conversion lifecycle should work correctly")
    void fullTypeConversionLifecycleShouldWorkCorrectly() throws PanamaException {
      // Create function type
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64},
              new WasmValueType[] {WasmValueType.F64});

      // Convert to native
      final int[][] nativeTypes = PanamaTypeConverter.functionTypeToNative(funcType);

      // Create parameters
      final WasmValue[] params = new WasmValue[] {WasmValue.i32(10), WasmValue.i64(20L)};

      // Validate parameters
      PanamaTypeConverter.validateParameterTypes(params, funcType.getParamTypes());

      // Marshal parameters
      final long paramsSize =
          PanamaTypeConverter.calculateValuesMemorySize(funcType.getParamTypes());
      final MemorySegment paramsMemory = arena.allocate(paramsSize);
      PanamaTypeConverter.marshalParameters(params, paramsMemory);

      // Simulate result
      final long resultsSize =
          PanamaTypeConverter.calculateValuesMemorySize(funcType.getReturnTypes());
      final MemorySegment resultsMemory = arena.allocate(resultsSize);
      MemoryLayouts.WASM_VAL_KIND.set(resultsMemory, 0L, MemoryLayouts.WASM_F64);
      MemoryLayouts.WASM_VAL_F64.set(resultsMemory, 0L, 3.14159);

      // Unmarshal results
      final WasmValue[] results =
          PanamaTypeConverter.unmarshalResults(resultsMemory, funcType.getReturnTypes());

      // Convert back to function type
      final FunctionType reconstructed =
          PanamaTypeConverter.nativeToFunctionType(nativeTypes[0], nativeTypes[1]);

      // Verify
      assertEquals(1, results.length, "Should have 1 result");
      assertEquals(WasmValueType.F64, results[0].getType(), "Result type should be F64");
      assertEquals(3.14159, results[0].asF64(), 0.00001, "Result value should match");
      assertArrayEquals(
          funcType.getParamTypes(),
          reconstructed.getParamTypes(),
          "Reconstructed param types should match");
    }
  }
}

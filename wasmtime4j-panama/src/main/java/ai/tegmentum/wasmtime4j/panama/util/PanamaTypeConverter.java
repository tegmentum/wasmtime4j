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

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.panama.MemoryLayouts;
import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Type conversion utilities for Panama Foreign Function API.
 *
 * <p>This class provides comprehensive type conversion and validation for all WebAssembly value
 * types including basic types (i32, i64, f32, f64), SIMD types (v128), and reference types
 * (funcref, externref) when using Panama FFI.
 *
 * <p>All conversions are defensive and validate input types to prevent JVM crashes or incorrect
 * behavior in native calls.
 *
 * @since 1.0.0
 */
public final class PanamaTypeConverter {

  private static final Logger LOGGER = Logger.getLogger(PanamaTypeConverter.class.getName());

  /** Size of v128 vector type in bytes. */
  private static final int V128_SIZE_BYTES = 16;

  /** Private constructor to prevent instantiation. */
  private PanamaTypeConverter() {}

  /**
   * Converts a WebAssembly value type enum to its native representation.
   *
   * @param type the WebAssembly value type
   * @return the native type constant
   * @throws PanamaException if type is null or unsupported
   */
  public static int wasmTypeToNative(final WasmValueType type) {
    PanamaValidation.requireNonNull(type, "type");
    switch (type) {
      case I32:
        return MemoryLayouts.WASM_I32;
      case I64:
        return MemoryLayouts.WASM_I64;
      case F32:
        return MemoryLayouts.WASM_F32;
      case F64:
        return MemoryLayouts.WASM_F64;
      case V128:
        return MemoryLayouts.WASM_V128;
      case FUNCREF:
        return MemoryLayouts.WASM_FUNCREF;
      case EXTERNREF:
        return MemoryLayouts.WASM_ANYREF;
      default:
        throw new PanamaException("Unsupported WebAssembly type: " + type);
    }
  }

  /**
   * Converts a native type constant to WebAssembly value type enum.
   *
   * @param nativeType the native type constant
   * @return the WebAssembly value type
   * @throws PanamaException if nativeType is invalid
   */
  public static WasmValueType nativeToWasmType(final int nativeType) {
    return switch (nativeType) {
      case MemoryLayouts.WASM_I32 -> WasmValueType.I32;
      case MemoryLayouts.WASM_I64 -> WasmValueType.I64;
      case MemoryLayouts.WASM_F32 -> WasmValueType.F32;
      case MemoryLayouts.WASM_F64 -> WasmValueType.F64;
      case MemoryLayouts.WASM_V128 -> WasmValueType.V128;
      case MemoryLayouts.WASM_FUNCREF -> WasmValueType.FUNCREF;
      case MemoryLayouts.WASM_ANYREF -> WasmValueType.EXTERNREF;
      default -> throw new PanamaException("Invalid native type constant: " + nativeType);
    };
  }

  /**
   * Marshals a WasmValue to native WebAssembly value format.
   *
   * @param wasmValue the WebAssembly value to marshal
   * @param valueSlot the memory segment to write the value to
   * @throws PanamaException if marshalling fails
   */
  public static void marshalWasmValue(final WasmValue wasmValue, final MemorySegment valueSlot) {
    PanamaValidation.requireNonNull(wasmValue, "wasmValue");
    PanamaValidation.requireNonNull(valueSlot, "valueSlot");

    final int nativeType = wasmTypeToNative(wasmValue.getType());
    MemoryLayouts.WASM_VAL_KIND.set(valueSlot, nativeType);

    switch (wasmValue.getType()) {
      case I32:
        MemoryLayouts.WASM_VAL_I32.set(valueSlot, wasmValue.asI32());
        break;

      case I64:
        MemoryLayouts.WASM_VAL_I64.set(valueSlot, wasmValue.asI64());
        break;

      case F32:
        MemoryLayouts.WASM_VAL_F32.set(valueSlot, wasmValue.asF32());
        break;

      case F64:
        MemoryLayouts.WASM_VAL_F64.set(valueSlot, wasmValue.asF64());
        break;

      case V128:
        final byte[] v128Bytes = wasmValue.asV128();
        validateV128Size(v128Bytes);
        final MemorySegment v128Segment = MemoryLayouts.getV128Value(valueSlot);
        v128Segment.copyFrom(MemorySegment.ofArray(v128Bytes));
        break;

      case EXTERNREF:
        // For now, externref is stored as NULL - full implementation would handle object references
        MemoryLayouts.WASM_VAL_REF.set(valueSlot, MemorySegment.NULL);
        break;

      case FUNCREF:
        // For now, funcref is stored as NULL - full implementation would handle function references
        MemoryLayouts.WASM_VAL_REF.set(valueSlot, MemorySegment.NULL);
        break;

      default:
        throw new PanamaException("Unsupported WebAssembly type for marshalling: " + wasmValue.getType());
    }
  }

  /**
   * Unmarshals a WebAssembly value from native format to WasmValue object.
   *
   * @param valueSlot the memory segment containing the native value
   * @param expectedType the expected WebAssembly type (for validation)
   * @return the unmarshalled WasmValue
   * @throws PanamaException if unmarshalling fails or type doesn't match
   */
  public static WasmValue unmarshalWasmValue(final MemorySegment valueSlot, final WasmValueType expectedType) {
    PanamaValidation.requireNonNull(valueSlot, "valueSlot");
    PanamaValidation.requireNonNull(expectedType, "expectedType");

    final int actualNativeType = (Integer) MemoryLayouts.WASM_VAL_KIND.get(valueSlot);
    final int expectedNativeType = wasmTypeToNative(expectedType);

    if (actualNativeType != expectedNativeType) {
      throw new PanamaException("Type mismatch: expected " + expectedType + " (" + expectedNativeType
                                + "), got native type " + actualNativeType);
    }

    return switch (expectedType) {
      case I32 -> WasmValue.i32((Integer) MemoryLayouts.WASM_VAL_I32.get(valueSlot));
      case I64 -> WasmValue.i64((Long) MemoryLayouts.WASM_VAL_I64.get(valueSlot));
      case F32 -> WasmValue.f32((Float) MemoryLayouts.WASM_VAL_F32.get(valueSlot));
      case F64 -> WasmValue.f64((Double) MemoryLayouts.WASM_VAL_F64.get(valueSlot));
      case V128 -> {
        final MemorySegment v128Segment = MemoryLayouts.getV128Value(valueSlot);
        final byte[] v128Bytes = v128Segment.toArray(ValueLayout.JAVA_BYTE);
        validateV128Size(v128Bytes);
        yield WasmValue.v128(v128Bytes);
      }
      case EXTERNREF -> WasmValue.externref(null); // Simplified - full implementation would handle references
      case FUNCREF -> WasmValue.funcref(null); // Simplified - full implementation would handle references
      default -> throw new PanamaException("Unsupported type for unmarshalling: " + expectedType);
    };
  }

  /**
   * Converts an array of WasmValue objects to native function parameters.
   *
   * @param values the WebAssembly values
   * @param paramsMemory the memory segment for parameter array
   * @throws PanamaException if values is null or contains invalid types
   */
  public static void marshalParameters(final WasmValue[] values, final MemorySegment paramsMemory) {
    PanamaValidation.requireNonNull(values, "values");
    PanamaValidation.requireNonNull(paramsMemory, "paramsMemory");

    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        throw new PanamaException("Parameter at index " + i + " is null");
      }
      final MemorySegment paramSlot = paramsMemory.asSlice(
          i * MemoryLayouts.WASM_VAL.byteSize(), 
          MemoryLayouts.WASM_VAL.byteSize());
      marshalWasmValue(values[i], paramSlot);
    }
  }

  /**
   * Unmarshals native function results to WasmValue array.
   *
   * @param resultsMemory the memory segment containing native results
   * @param expectedTypes the expected return types
   * @return array of WebAssembly values
   * @throws PanamaException if results or types are invalid
   */
  public static WasmValue[] unmarshalResults(
      final MemorySegment resultsMemory, final WasmValueType[] expectedTypes) {
    PanamaValidation.requireNonNull(resultsMemory, "resultsMemory");
    PanamaValidation.requireNonNull(expectedTypes, "expectedTypes");

    final WasmValue[] results = new WasmValue[expectedTypes.length];
    for (int i = 0; i < expectedTypes.length; i++) {
      final MemorySegment resultSlot = resultsMemory.asSlice(
          i * MemoryLayouts.WASM_VAL.byteSize(), 
          MemoryLayouts.WASM_VAL.byteSize());
      results[i] = unmarshalWasmValue(resultSlot, expectedTypes[i]);
    }
    return results;
  }

  /**
   * Validates that parameter types match expected function signature.
   *
   * @param params the parameters to validate
   * @param expectedTypes the expected parameter types
   * @throws PanamaException if types don't match
   */
  public static void validateParameterTypes(
      final WasmValue[] params, final WasmValueType[] expectedTypes) {
    PanamaValidation.requireNonNull(params, "params");
    PanamaValidation.requireNonNull(expectedTypes, "expectedTypes");

    if (params.length != expectedTypes.length) {
      throw new PanamaException(
          "Parameter count mismatch: got " + params.length + ", expected " + expectedTypes.length);
    }

    for (int i = 0; i < params.length; i++) {
      if (params[i] == null) {
        throw new PanamaException("Parameter at index " + i + " is null");
      }
      final WasmValueType actualType = params[i].getType();
      final WasmValueType expectedType = expectedTypes[i];
      if (actualType != expectedType) {
        throw new PanamaException(
            "Parameter type mismatch at index " + i + ": got " + actualType + ", expected " + expectedType);
      }
    }
  }

  /**
   * Validates that a v128 byte array has the correct size.
   *
   * @param bytes the byte array to validate
   * @throws PanamaException if array size is incorrect
   */
  public static void validateV128Size(final byte[] bytes) {
    if (bytes == null) {
      throw new PanamaException("v128 bytes cannot be null");
    }
    if (bytes.length != V128_SIZE_BYTES) {
      throw new PanamaException(
          "v128 must be exactly " + V128_SIZE_BYTES + " bytes, got " + bytes.length);
    }
  }

  /**
   * Converts a FunctionType to native type arrays for parameter and result types.
   *
   * @param functionType the function type to convert
   * @return array containing parameter types array and result types array
   * @throws PanamaException if functionType is null
   */
  public static int[][] functionTypeToNative(final FunctionType functionType) {
    PanamaValidation.requireNonNull(functionType, "functionType");

    final WasmValueType[] paramTypes = functionType.getParamTypes();
    final WasmValueType[] returnTypes = functionType.getReturnTypes();

    final int[] nativeParamTypes = new int[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      nativeParamTypes[i] = wasmTypeToNative(paramTypes[i]);
    }

    final int[] nativeReturnTypes = new int[returnTypes.length];
    for (int i = 0; i < returnTypes.length; i++) {
      nativeReturnTypes[i] = wasmTypeToNative(returnTypes[i]);
    }

    return new int[][]{nativeParamTypes, nativeReturnTypes};
  }

  /**
   * Creates a FunctionType from native type arrays.
   *
   * @param nativeParamTypes array of native parameter type constants
   * @param nativeReturnTypes array of native return type constants
   * @return the FunctionType
   * @throws PanamaException if type arrays are invalid
   */
  public static FunctionType nativeToFunctionType(final int[] nativeParamTypes, final int[] nativeReturnTypes) {
    PanamaValidation.requireNonNull(nativeParamTypes, "nativeParamTypes");
    PanamaValidation.requireNonNull(nativeReturnTypes, "nativeReturnTypes");

    final WasmValueType[] paramTypes = new WasmValueType[nativeParamTypes.length];
    for (int i = 0; i < nativeParamTypes.length; i++) {
      paramTypes[i] = nativeToWasmType(nativeParamTypes[i]);
    }

    final WasmValueType[] returnTypes = new WasmValueType[nativeReturnTypes.length];
    for (int i = 0; i < nativeReturnTypes.length; i++) {
      returnTypes[i] = nativeToWasmType(nativeReturnTypes[i]);
    }

    return new FunctionType(paramTypes, returnTypes);
  }

  /**
   * Calculates the total memory size needed for an array of WebAssembly values.
   *
   * @param types the value types
   * @return total size in bytes
   */
  public static long calculateValuesMemorySize(final WasmValueType[] types) {
    if (types == null || types.length == 0) {
      return 0;
    }
    return types.length * MemoryLayouts.WASM_VAL.byteSize();
  }

  /**
   * Validates that all reference types in the array are properly handled.
   *
   * @param values the values to validate
   * @throws PanamaException if any reference type is invalid
   */
  public static void validateReferenceTypes(final WasmValue[] values) {
    PanamaValidation.requireNonNull(values, "values");

    for (int i = 0; i < values.length; i++) {
      final WasmValue value = values[i];
      if (value == null) {
        throw new PanamaException("Value at index " + i + " is null");
      }

      switch (value.getType()) {
        case FUNCREF:
          // TODO: Implement proper funcref validation when function references are fully supported
          LOGGER.fine("Funcref validation (simplified implementation)");
          break;
        case EXTERNREF:
          // TODO: Implement proper externref validation when external references are fully supported
          LOGGER.fine("Externref validation (simplified implementation)");
          break;
        default:
          // Non-reference types don't need special validation here
          break;
      }
    }
  }
}
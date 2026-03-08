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
package ai.tegmentum.wasmtime4j.panama.type;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.type.AbstractFuncType;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Panama implementation of FuncType interface.
 *
 * <p>This class provides type introspection capabilities for WebAssembly function types using
 * Panama Foreign Function Interface bindings to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class PanamaFuncType extends AbstractFuncType {

  private static final Logger LOGGER = Logger.getLogger(PanamaFuncType.class.getName());

  private final Arena arena;
  private final MemorySegment nativeHandle;

  /**
   * Creates a new PanamaFuncType instance.
   *
   * @param params the parameter types
   * @param results the result types
   * @param arena the memory arena for resource management
   * @param nativeHandle the native handle to the function type
   */
  public PanamaFuncType(
      final List<WasmValueType> params,
      final List<WasmValueType> results,
      final Arena arena,
      final MemorySegment nativeHandle) {
    super(params, results);
    Validation.requireNonNull(arena, "arena");
    PanamaValidation.requireValidHandle(nativeHandle, "nativeHandle");
    this.arena = arena;
    this.nativeHandle = nativeHandle;

    LOGGER.fine(String.format("Created PanamaFuncType: params=%s, results=%s", params, results));
  }

  /**
   * Creates a PanamaFuncType from type information without a native handle.
   *
   * <p>This factory method is used when type information is parsed from JSON or other sources where
   * a native handle is not available.
   *
   * @param params the parameter types
   * @param results the result types
   * @return the PanamaFuncType instance
   */
  public static PanamaFuncType of(
      final List<WasmValueType> params, final List<WasmValueType> results) {
    return new PanamaFuncType(params, results);
  }

  /**
   * Private constructor for creating type descriptors without native handles.
   *
   * @param params the parameter types
   * @param results the result types
   */
  private PanamaFuncType(final List<WasmValueType> params, final List<WasmValueType> results) {
    super(params, results);
    this.arena = null;
    this.nativeHandle = MemorySegment.NULL;

    LOGGER.fine(
        String.format(
            "Created PanamaFuncType (no native handle): params=%s, results=%s", params, results));
  }

  /**
   * Creates a PanamaFuncType from parameter and result arrays.
   *
   * @param params the parameter types array
   * @param results the result types array
   * @param arena the memory arena for resource management
   * @param nativeHandle the native handle to the function type
   */
  public PanamaFuncType(
      final WasmValueType[] params,
      final WasmValueType[] results,
      final Arena arena,
      final MemorySegment nativeHandle) {
    this(Arrays.asList(params), Arrays.asList(results), arena, nativeHandle);
  }

  /**
   * Creates a PanamaFuncType from native function type information.
   *
   * @param nativeHandle the native handle to the function type
   * @param arena the memory arena for resource management
   * @return the PanamaFuncType instance
   * @throws IllegalArgumentException if nativeHandle is invalid
   */
  public static PanamaFuncType fromNative(final MemorySegment nativeHandle, final Arena arena) {
    PanamaValidation.requireValidHandle(nativeHandle, "nativeHandle");
    Validation.requireNonNull(arena, "arena");

    // First, get the counts
    final MemorySegment countSegment = arena.allocate(16); // 2 longs * 8 bytes
    nativeGetFuncTypeCounts(nativeHandle, countSegment);

    final int paramCount = (int) countSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);
    final int resultCount = (int) countSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 8);

    // Allocate memory for the full type info
    final int totalElements = 2 + paramCount + resultCount;
    final MemorySegment typeInfoSegment = arena.allocate(totalElements * 8L); // longs * 8 bytes

    // Get the full type info
    nativeGetFuncTypeInfo(nativeHandle, typeInfoSegment);

    // Verify counts match
    final int actualParamCount =
        (int) typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);
    final int actualResultCount =
        (int) typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 8);

    if (actualParamCount != paramCount || actualResultCount != resultCount) {
      throw new IllegalStateException(
          String.format(
              "Function type count mismatch: expected params=%d, results=%d; got params=%d,"
                  + " results=%d",
              paramCount, resultCount, actualParamCount, actualResultCount));
    }

    final WasmValueType[] params = new WasmValueType[paramCount];
    final WasmValueType[] results = new WasmValueType[resultCount];

    // Extract parameter types
    for (int i = 0; i < paramCount; i++) {
      final int typeCode =
          (int) typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, (2 + i) * 8L);
      params[i] = WasmValueType.fromNativeTypeCode(typeCode);
    }

    // Extract result types
    for (int i = 0; i < resultCount; i++) {
      final int typeCode =
          (int)
              typeInfoSegment.get(
                  java.lang.foreign.ValueLayout.JAVA_LONG, (2 + paramCount + i) * 8L);
      results[i] = WasmValueType.fromNativeTypeCode(typeCode);
    }

    return new PanamaFuncType(params, results, arena, nativeHandle);
  }

  /**
   * Gets the native handle for this function type.
   *
   * @return the native handle
   */
  public MemorySegment getNativeHandle() {
    return nativeHandle;
  }

  /**
   * Gets the memory arena used by this function type.
   *
   * @return the memory arena
   */
  public Arena getArena() {
    return arena;
  }

  /**
   * Native method to get function type parameter and result counts.
   *
   * @param nativeHandle the native handle to the function type
   * @param resultBuffer the buffer to store the counts [paramCount, resultCount]
   */
  private static native void nativeGetFuncTypeCounts(
      MemorySegment nativeHandle, MemorySegment resultBuffer);

  /**
   * Native method to get function type information.
   *
   * @param nativeHandle the native handle to the function type
   * @param resultBuffer the buffer to store the result [paramCount, resultCount, param0TypeCode,
   *     param1TypeCode, ..., result0TypeCode, result1TypeCode, ...]
   */
  private static native void nativeGetFuncTypeInfo(
      MemorySegment nativeHandle, MemorySegment resultBuffer);
}

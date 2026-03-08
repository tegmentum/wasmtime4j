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
package ai.tegmentum.wasmtime4j.jni.type;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.AbstractFuncType;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * JNI implementation of FuncType interface.
 *
 * <p>This class provides type introspection capabilities for WebAssembly function types using JNI
 * bindings to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class JniFuncType extends AbstractFuncType {

  private static final Logger LOGGER = Logger.getLogger(JniFuncType.class.getName());

  /**
   * Creates a new JniFuncType instance.
   *
   * @param params the parameter types
   * @param results the result types
   */
  public JniFuncType(final List<WasmValueType> params, final List<WasmValueType> results) {
    super(params, results);
    LOGGER.fine(String.format("Created JniFuncType: params=%s, results=%s", params, results));
  }

  /**
   * Creates a JniFuncType from parameter and result arrays.
   *
   * @param params the parameter types array
   * @param results the result types array
   */
  public JniFuncType(final WasmValueType[] params, final WasmValueType[] results) {
    this(Arrays.asList(params), Arrays.asList(results));
  }

  /**
   * Creates a JniFuncType from native function type information.
   *
   * @param nativeHandle the native handle to the function type
   * @return the JniFuncType instance
   * @throws IllegalArgumentException if nativeHandle is invalid
   */
  public static JniFuncType fromNative(final long nativeHandle) {
    Validation.requireValidHandle(nativeHandle, "nativeHandle");

    final long[] typeInfo = nativeGetFuncTypeInfo(nativeHandle);
    if (typeInfo.length < 2) {
      throw new IllegalStateException("Invalid function type info from native");
    }

    final int paramCount = (int) typeInfo[0];
    final int resultCount = (int) typeInfo[1];

    if (typeInfo.length < 2 + paramCount + resultCount) {
      throw new IllegalStateException(
          "Function type info array too small: expected "
              + (2 + paramCount + resultCount)
              + ", got "
              + typeInfo.length);
    }

    final WasmValueType[] params = new WasmValueType[paramCount];
    final WasmValueType[] results = new WasmValueType[resultCount];

    // Extract parameter types
    for (int i = 0; i < paramCount; i++) {
      params[i] = WasmValueType.fromNativeTypeCode((int) typeInfo[2 + i]);
    }

    // Extract result types
    for (int i = 0; i < resultCount; i++) {
      results[i] = WasmValueType.fromNativeTypeCode((int) typeInfo[2 + paramCount + i]);
    }

    return new JniFuncType(params, results);
  }

  /**
   * Native method to get function type information.
   *
   * @param nativeHandle the native handle to the function type
   * @return array containing [paramCount, resultCount, param0TypeCode, param1TypeCode, ...,
   *     result0TypeCode, result1TypeCode, ...]
   */
  private static native long[] nativeGetFuncTypeInfo(long nativeHandle);
}

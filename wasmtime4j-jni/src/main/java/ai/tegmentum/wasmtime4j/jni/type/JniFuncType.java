package ai.tegmentum.wasmtime4j.jni.type;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
public final class JniFuncType implements FuncType {

  private static final Logger LOGGER = Logger.getLogger(JniFuncType.class.getName());

  private final List<WasmValueType> params;
  private final List<WasmValueType> results;

  /**
   * Creates a new JniFuncType instance.
   *
   * @param params the parameter types
   * @param results the result types
   */
  public JniFuncType(final List<WasmValueType> params, final List<WasmValueType> results) {
    Validation.requireNonNull(params, "params");
    Validation.requireNonNull(results, "results");

    // Validate that all parameter and result types are non-null
    for (int i = 0; i < params.size(); i++) {
      if (params.get(i) == null) {
        throw new IllegalArgumentException("Parameter type at index " + i + " is null");
      }
    }
    for (int i = 0; i < results.size(); i++) {
      if (results.get(i) == null) {
        throw new IllegalArgumentException("Result type at index " + i + " is null");
      }
    }

    this.params = Collections.unmodifiableList(new ArrayList<>(params));
    this.results = Collections.unmodifiableList(new ArrayList<>(results));

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

  @Override
  public List<WasmValueType> getParams() {
    return params;
  }

  @Override
  public List<WasmValueType> getResults() {
    return results;
  }

  @Override
  public WasmTypeKind getKind() {
    return WasmTypeKind.FUNCTION;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof FuncType)) {
      return false;
    }

    final FuncType other = (FuncType) obj;
    return params.equals(other.getParams()) && results.equals(other.getResults());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(params, results);
  }

  @Override
  public String toString() {
    return String.format("FuncType{params=%s, results=%s}", params, results);
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

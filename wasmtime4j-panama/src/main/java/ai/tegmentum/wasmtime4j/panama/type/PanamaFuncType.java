package ai.tegmentum.wasmtime4j.panama.type;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.Collections;
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
public final class PanamaFuncType implements FuncType {

  private static final Logger LOGGER = Logger.getLogger(PanamaFuncType.class.getName());

  private final List<WasmValueType> params;
  private final List<WasmValueType> results;
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
    PanamaValidation.requireNonNull(params, "params");
    PanamaValidation.requireNonNull(results, "results");
    PanamaValidation.requireNonNull(arena, "arena");
    PanamaValidation.requireValidHandle(nativeHandle, "nativeHandle");

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

    this.params = Collections.unmodifiableList(List.copyOf(params));
    this.results = Collections.unmodifiableList(List.copyOf(results));
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
    PanamaValidation.requireNonNull(params, "params");
    PanamaValidation.requireNonNull(results, "results");

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

    return new PanamaFuncType(params, results);
  }

  /**
   * Private constructor for creating type descriptors without native handles.
   *
   * @param params the parameter types
   * @param results the result types
   */
  private PanamaFuncType(final List<WasmValueType> params, final List<WasmValueType> results) {
    this.params = Collections.unmodifiableList(List.copyOf(params));
    this.results = Collections.unmodifiableList(List.copyOf(results));
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
    PanamaValidation.requireNonNull(arena, "arena");

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

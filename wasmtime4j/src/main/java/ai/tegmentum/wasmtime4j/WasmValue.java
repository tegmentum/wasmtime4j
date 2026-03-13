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
package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.type.ValType;

/**
 * Represents a WebAssembly value that can be passed to and from WebAssembly functions.
 *
 * <p>This class encapsulates all WebAssembly value types including 32-bit and 64-bit integers,
 * 32-bit and 64-bit floating-point numbers, 128-bit vectors, and reference types (funcref and
 * externref). Each value maintains its type information for proper validation and conversion.
 *
 * <p>WebAssembly values are immutable and type-safe. Once created, a value's type and content
 * cannot be changed. Type conversions must be explicit and will throw exceptions if attempted with
 * incompatible types.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create values for different WebAssembly types
 * WasmValue intValue = WasmValue.i32(42);
 * WasmValue longValue = WasmValue.i64(1000L);
 * WasmValue floatValue = WasmValue.f32(3.14f);
 * WasmValue doubleValue = WasmValue.f64(2.718);
 *
 * // Pass values to a WebAssembly function
 * WasmValue[] results = function.call(intValue, floatValue);
 *
 * // Extract results with type checking
 * int resultInt = results[0].asInt();
 * float resultFloat = results[1].asFloat();
 * }</pre>
 *
 * <p>All value creation methods perform validation to ensure type safety and correctness according
 * to WebAssembly specifications.
 *
 * @since 1.0.0
 */
public final class WasmValue {

  // --- Flyweight cache for commonly created values ---

  /** Cache range for i32 small values: [-128, 127] (matches Integer cache). */
  private static final int I32_CACHE_LOW = -128;

  private static final int I32_CACHE_HIGH = 127;
  private static final WasmValue[] I32_CACHE = new WasmValue[I32_CACHE_HIGH - I32_CACHE_LOW + 1];

  /** Cached zero values for other numeric types. */
  private static final WasmValue I64_ZERO = new WasmValue(WasmValueType.I64, Long.valueOf(0L));

  private static final WasmValue F32_ZERO = new WasmValue(WasmValueType.F32, Float.valueOf(0.0f));
  private static final WasmValue F64_ZERO = new WasmValue(WasmValueType.F64, Double.valueOf(0.0));

  /** Cached null reference singletons. */
  private static final WasmValue NULL_FUNCREF = new WasmValue(WasmValueType.FUNCREF, null);

  private static final WasmValue NULL_EXTERNREF = new WasmValue(WasmValueType.EXTERNREF, null);
  private static final WasmValue NULL_ANYREF = new WasmValue(WasmValueType.ANYREF, null);
  private static final WasmValue NULL_EQREF = new WasmValue(WasmValueType.EQREF, null);
  private static final WasmValue NULL_I31REF = new WasmValue(WasmValueType.I31REF, null);
  private static final WasmValue NULL_STRUCTREF = new WasmValue(WasmValueType.STRUCTREF, null);
  private static final WasmValue NULL_ARRAYREF = new WasmValue(WasmValueType.ARRAYREF, null);
  private static final WasmValue NULL_NULLREF = new WasmValue(WasmValueType.NULLREF, null);
  private static final WasmValue NULL_NULLFUNCREF = new WasmValue(WasmValueType.NULLFUNCREF, null);
  private static final WasmValue NULL_NULLEXTERNREF =
      new WasmValue(WasmValueType.NULLEXTERNREF, null);
  private static final WasmValue NULL_EXNREF = new WasmValue(WasmValueType.EXNREF, null);
  private static final WasmValue NULL_NULLEXNREF = new WasmValue(WasmValueType.NULLEXNREF, null);
  private static final WasmValue NULL_CONTREF = new WasmValue(WasmValueType.CONTREF, null);
  private static final WasmValue NULL_NULLCONTREF = new WasmValue(WasmValueType.NULLCONTREF, null);

  static {
    for (int i = 0; i < I32_CACHE.length; i++) {
      I32_CACHE[i] = new WasmValue(WasmValueType.I32, Integer.valueOf(i + I32_CACHE_LOW));
    }
  }

  private final WasmValueType type;
  private final Object value;

  private WasmValue(final WasmValueType type, final Object value) {
    this.type = type;
    this.value = value;
  }

  /**
   * Gets the type of this value.
   *
   * @return the value type
   */
  public WasmValueType getType() {
    return type;
  }

  /**
   * Gets the raw value.
   *
   * @return the value object
   */
  public Object getValue() {
    return value;
  }

  /**
   * Gets this value as an integer.
   *
   * @return the integer value
   * @throws ClassCastException if this value is not an integer
   */
  public int asInt() {
    return (Integer) value;
  }

  /**
   * Gets this value as a long.
   *
   * @return the long value
   * @throws ClassCastException if this value is not a long
   */
  public long asLong() {
    return (Long) value;
  }

  /**
   * Gets this value as a float.
   *
   * @return the float value
   * @throws ClassCastException if this value is not a float
   */
  public float asFloat() {
    return (Float) value;
  }

  /**
   * Gets this value as a double.
   *
   * @return the double value
   * @throws ClassCastException if this value is not a double
   */
  public double asDouble() {
    return (Double) value;
  }

  /**
   * Gets this value as a 128-bit vector.
   *
   * @return the vector value as byte array
   * @throws ClassCastException if this value is not a v128
   */
  public byte[] asV128() {
    final byte[] bytes = (byte[]) value;
    return bytes.clone();
  }

  /**
   * Gets this value as a function reference.
   *
   * @return the function reference (may be null)
   * @throws ClassCastException if this value is not a funcref
   */
  public Object asFuncref() {
    if (type != WasmValueType.FUNCREF) {
      throw new ClassCastException("Value is not a funcref, but " + type);
    }
    return value;
  }

  /**
   * Gets this value as an external reference.
   *
   * @return the external reference (may be null)
   * @throws ClassCastException if this value is not an externref
   */
  public Object asExternref() {
    if (type != WasmValueType.EXTERNREF) {
      throw new ClassCastException("Value is not an externref, but " + type);
    }
    return value;
  }

  /**
   * Gets this value as an anyref.
   *
   * @return the anyref value (may be null)
   * @throws ClassCastException if this value is not an anyref
   */
  public Object asAnyref() {
    if (type != WasmValueType.ANYREF) {
      throw new ClassCastException("Value is not an anyref, but " + type);
    }
    return value;
  }

  /**
   * Gets this value as an i31ref.
   *
   * @return the 31-bit integer value
   * @throws ClassCastException if this value is not an i31ref
   * @throws NullPointerException if the i31ref is null
   */
  public int asI31ref() {
    if (type != WasmValueType.I31REF) {
      throw new ClassCastException("Value is not an i31ref, but " + type);
    }
    if (value == null) {
      throw new NullPointerException("i31ref is null");
    }
    return (Integer) value;
  }

  /**
   * Gets this value as an exception reference.
   *
   * @return the exception reference value (may be null)
   * @throws ClassCastException if this value is not an exnref
   */
  public Object asExnref() {
    if (type != WasmValueType.EXNREF) {
      throw new ClassCastException("Value is not an exnref, but " + type);
    }
    return value;
  }

  /**
   * Gets this value as a continuation reference.
   *
   * @return the continuation reference value (may be null)
   * @throws ClassCastException if this value is not a contref
   */
  public Object asContref() {
    if (type != WasmValueType.CONTREF) {
      throw new ClassCastException("Value is not a contref, but " + type);
    }
    return value;
  }

  /**
   * Gets this value as a type-safe ExternRef wrapper.
   *
   * @return the ExternRef wrapping the external reference value
   * @throws ClassCastException if this value is not an externref
   */
  public ExternRef<Object> asExternRef() {
    if (type != WasmValueType.EXTERNREF) {
      throw new ClassCastException("Value is not an externref, but " + type);
    }
    return ExternRef.fromRaw(value);
  }

  /**
   * Gets this value as a type-safe ExternRef with the specified type.
   *
   * @param targetType the expected type of the wrapped value
   * @param <T> the type of the wrapped value
   * @return the ExternRef wrapping the external reference value
   * @throws ClassCastException if this value is not an externref or the wrapped value is not of the
   *     expected type
   */
  public <T> ExternRef<T> asExternRef(final Class<T> targetType) {
    if (type != WasmValueType.EXTERNREF) {
      throw new ClassCastException("Value is not an externref, but " + type);
    }
    final Object rawValue = value;
    if (rawValue != null && !targetType.isInstance(rawValue)) {
      throw new ClassCastException(
          "ExternRef value is not of type "
              + targetType.getName()
              + ", but "
              + rawValue.getClass().getName());
    }
    return ExternRef.ofNullable(targetType.cast(rawValue), targetType);
  }

  /**
   * Gets this value as a reference (funcref or externref).
   *
   * @return the reference value (may be null)
   * @throws ClassCastException if this value is not a reference type
   */
  public Object asReference() {
    if (type != WasmValueType.FUNCREF && type != WasmValueType.EXTERNREF) {
      throw new ClassCastException("Value is not a reference type, but " + type);
    }
    return value;
  }

  /**
   * Checks if this value is a 32-bit integer.
   *
   * @return true if this value is of type I32, false otherwise
   */
  public boolean isI32() {
    return type == WasmValueType.I32;
  }

  /**
   * Checks if this value is a 64-bit integer.
   *
   * @return true if this value is of type I64, false otherwise
   */
  public boolean isI64() {
    return type == WasmValueType.I64;
  }

  /**
   * Checks if this value is a 32-bit float.
   *
   * @return true if this value is of type F32, false otherwise
   */
  public boolean isF32() {
    return type == WasmValueType.F32;
  }

  /**
   * Checks if this value is a 64-bit float.
   *
   * @return true if this value is of type F64, false otherwise
   */
  public boolean isF64() {
    return type == WasmValueType.F64;
  }

  /**
   * Checks if this value is a 128-bit vector.
   *
   * @return true if this value is of type V128, false otherwise
   */
  public boolean isV128() {
    return type == WasmValueType.V128;
  }

  /**
   * Checks if this value is a function reference.
   *
   * @return true if this value is of type FUNCREF, false otherwise
   */
  public boolean isFuncref() {
    return type == WasmValueType.FUNCREF;
  }

  /**
   * Checks if this value is an external reference.
   *
   * @return true if this value is of type EXTERNREF, false otherwise
   */
  public boolean isExternref() {
    return type == WasmValueType.EXTERNREF;
  }

  /**
   * Creates a WasmValue from a pre-validated boxed value, avoiding unnecessary unbox+rebox.
   *
   * <p>This factory is intended for internal use by type converters that have already validated the
   * value's type (e.g., confirmed the Object is an Integer for I32). It skips the primitive
   * parameter overhead that typed factories like {@link #i32(int)} impose.
   *
   * @param type the value type
   * @param value the already-boxed value (Integer, Long, Float, Double, byte[], or reference)
   * @return a new WasmValue
   */
  public static WasmValue fromBoxed(final WasmValueType type, final Object value) {
    if (type == WasmValueType.I32 && value instanceof Integer) {
      final int intVal = (Integer) value;
      if (intVal >= I32_CACHE_LOW && intVal <= I32_CACHE_HIGH) {
        return I32_CACHE[intVal - I32_CACHE_LOW];
      }
    }
    return new WasmValue(type, value);
  }

  /**
   * Creates a 32-bit integer value.
   *
   * @param value the integer value
   * @return a new WasmValue
   */
  public static WasmValue i32(final int value) {
    if (value >= I32_CACHE_LOW && value <= I32_CACHE_HIGH) {
      return I32_CACHE[value - I32_CACHE_LOW];
    }
    return new WasmValue(WasmValueType.I32, value);
  }

  /**
   * Creates a 64-bit integer value.
   *
   * @param value the long value
   * @return a new WasmValue
   */
  public static WasmValue i64(final long value) {
    if (value == 0L) {
      return I64_ZERO;
    }
    return new WasmValue(WasmValueType.I64, value);
  }

  /**
   * Creates a 32-bit floating-point value.
   *
   * @param value the float value
   * @return a new WasmValue
   */
  public static WasmValue f32(final float value) {
    if (value == 0.0f && Float.floatToRawIntBits(value) == 0) {
      return F32_ZERO;
    }
    return new WasmValue(WasmValueType.F32, value);
  }

  /**
   * Creates a 64-bit floating-point value.
   *
   * @param value the double value
   * @return a new WasmValue
   */
  public static WasmValue f64(final double value) {
    if (value == 0.0 && Double.doubleToRawLongBits(value) == 0L) {
      return F64_ZERO;
    }
    return new WasmValue(WasmValueType.F64, value);
  }

  /**
   * Creates a 128-bit vector value.
   *
   * @param value the vector value as byte array (16 bytes)
   * @return a new WasmValue
   * @throws IllegalArgumentException if value is not exactly 16 bytes
   */
  public static WasmValue v128(final byte[] value) {
    if (value == null || value.length != 16) {
      throw new IllegalArgumentException("v128 value must be exactly 16 bytes");
    }
    return new WasmValue(WasmValueType.V128, value.clone());
  }

  /**
   * Creates a 128-bit vector value from two 64-bit values.
   *
   * @param high the high 64 bits
   * @param low the low 64 bits
   * @return a new WasmValue
   */
  public static WasmValue v128(final long high, final long low) {
    final byte[] bytes = new byte[16];
    // Store in little-endian order
    for (int i = 0; i < 8; i++) {
      bytes[i] = (byte) ((low >>> (i * 8)) & 0xFF);
      bytes[i + 8] = (byte) ((high >>> (i * 8)) & 0xFF);
    }
    return new WasmValue(WasmValueType.V128, bytes);
  }

  /**
   * Creates a function reference value.
   *
   * @param value the function reference (nullable)
   * @return a new WasmValue
   */
  public static WasmValue funcref(final Object value) {
    if (value == null) {
      return NULL_FUNCREF;
    }
    return new WasmValue(WasmValueType.FUNCREF, value);
  }

  /**
   * Creates a function reference value from a FunctionReference.
   *
   * @param functionReference the function reference
   * @return a new WasmValue
   */
  public static WasmValue funcref(final FunctionReference functionReference) {
    return new WasmValue(WasmValueType.FUNCREF, functionReference);
  }

  /**
   * Creates an external reference value.
   *
   * @param value the external reference (nullable)
   * @return a new WasmValue
   */
  public static WasmValue externref(final Object value) {
    if (value == null) {
      return NULL_EXTERNREF;
    }
    return new WasmValue(WasmValueType.EXTERNREF, value);
  }

  /**
   * Creates an external reference value from a type-safe ExternRef wrapper.
   *
   * @param externRef the type-safe externref wrapper
   * @param <T> the type of the wrapped value
   * @return a new WasmValue
   */
  public static <T> WasmValue externref(final ExternRef<T> externRef) {
    return new WasmValue(WasmValueType.EXTERNREF, externRef);
  }

  /**
   * Creates a null funcref value.
   *
   * @return a new WasmValue representing null funcref
   */
  public static WasmValue nullFuncref() {
    return NULL_FUNCREF;
  }

  /**
   * Creates a null externref value.
   *
   * @return a new WasmValue representing null externref
   */
  public static WasmValue nullExternref() {
    return NULL_EXTERNREF;
  }

  /**
   * Creates a null anyref value.
   *
   * <p>This is used in WebAssembly GC for null references in the anyref hierarchy.
   *
   * @return a new WasmValue representing null anyref
   */
  public static WasmValue nullAnyRef() {
    return NULL_ANYREF;
  }

  /**
   * Creates an anyref value.
   *
   * <p>This is used in WebAssembly GC for references in the anyref hierarchy.
   *
   * @param value the value to wrap (may be null)
   * @return a new WasmValue representing anyref
   */
  public static WasmValue anyref(final Object value) {
    return new WasmValue(WasmValueType.ANYREF, value);
  }

  /**
   * Creates a null eqref value.
   *
   * <p>This is used in WebAssembly GC for null references in the eqref hierarchy.
   *
   * @return a new WasmValue representing null eqref
   */
  public static WasmValue nullEqRef() {
    return NULL_EQREF;
  }

  /**
   * Creates an eqref value.
   *
   * <p>This is used in WebAssembly GC for equality-testable references.
   *
   * @param value the value to wrap (may be null)
   * @return a new WasmValue representing eqref
   */
  public static WasmValue eqref(final Object value) {
    return new WasmValue(WasmValueType.EQREF, value);
  }

  /**
   * Creates an i31ref value.
   *
   * <p>This is used in WebAssembly GC for immediate 31-bit integer references. The value must be in
   * the range [-2^30, 2^30 - 1] (signed 31-bit integer).
   *
   * @param value the 31-bit integer value
   * @return a new WasmValue representing i31ref
   * @throws IllegalArgumentException if value is outside the valid range
   */
  public static WasmValue i31ref(final int value) {
    // i31ref can hold values in the range [-2^30, 2^30 - 1]
    final int minValue = -(1 << 30);
    final int maxValue = (1 << 30) - 1;
    if (value < minValue || value > maxValue) {
      throw new IllegalArgumentException(
          "i31ref value must be in range [" + minValue + ", " + maxValue + "], got: " + value);
    }
    return new WasmValue(WasmValueType.I31REF, value);
  }

  /**
   * Creates a null i31ref value.
   *
   * <p>This is used in WebAssembly GC for null i31 references.
   *
   * @return a new WasmValue representing null i31ref
   */
  public static WasmValue nullI31Ref() {
    return NULL_I31REF;
  }

  /**
   * Creates a structref value.
   *
   * <p>This is used in WebAssembly GC for struct references.
   *
   * @param value the value to wrap (may be null)
   * @return a new WasmValue representing structref
   */
  public static WasmValue structref(final Object value) {
    return new WasmValue(WasmValueType.STRUCTREF, value);
  }

  /**
   * Creates a null structref value.
   *
   * <p>This is used in WebAssembly GC for null struct references.
   *
   * @return a new WasmValue representing null structref
   */
  public static WasmValue nullStructRef() {
    return NULL_STRUCTREF;
  }

  /**
   * Creates an arrayref value.
   *
   * <p>This is used in WebAssembly GC for array references.
   *
   * @param value the value to wrap (may be null)
   * @return a new WasmValue representing arrayref
   */
  public static WasmValue arrayref(final Object value) {
    return new WasmValue(WasmValueType.ARRAYREF, value);
  }

  /**
   * Creates a null arrayref value.
   *
   * <p>This is used in WebAssembly GC for null array references.
   *
   * @return a new WasmValue representing null arrayref
   */
  public static WasmValue nullArrayRef() {
    return NULL_ARRAYREF;
  }

  /**
   * Creates a null ref value (bottom type for the anyref hierarchy).
   *
   * <p>This is used in WebAssembly GC for the null bottom reference type. NULLREF can only hold
   * null and is a subtype of all nullable reference types in the anyref hierarchy.
   *
   * @return a new WasmValue representing null ref
   */
  public static WasmValue nullRef() {
    return NULL_NULLREF;
  }

  /**
   * Creates a null funcref bottom type value.
   *
   * <p>This is used in WebAssembly GC for the null bottom type in the funcref hierarchy.
   * NULLFUNCREF can only hold null and is a subtype of all nullable function reference types.
   *
   * @return a new WasmValue representing null funcref bottom type
   */
  public static WasmValue nullNullFuncRef() {
    return NULL_NULLFUNCREF;
  }

  /**
   * Creates a null externref bottom type value.
   *
   * <p>This is used in WebAssembly GC for the null bottom type in the externref hierarchy.
   * NULLEXTERNREF can only hold null and is a subtype of all nullable external reference types.
   *
   * @return a new WasmValue representing null externref bottom type
   */
  public static WasmValue nullNullExternRef() {
    return NULL_NULLEXTERNREF;
  }

  /**
   * Creates an exception reference value.
   *
   * <p>This is used in the WebAssembly exception handling proposal for references to exception
   * values.
   *
   * @param value the exception reference value (may be null)
   * @return a new WasmValue representing exnref
   */
  public static WasmValue exnref(final Object value) {
    return new WasmValue(WasmValueType.EXNREF, value);
  }

  /**
   * Creates a null exnref value.
   *
   * <p>This is used in the WebAssembly exception handling proposal for null exception references.
   *
   * @return a new WasmValue representing null exnref
   */
  public static WasmValue nullExnRef() {
    return NULL_EXNREF;
  }

  /**
   * Creates a null exnref bottom type value.
   *
   * <p>NULLEXNREF can only hold null and is a subtype of all nullable exception reference types.
   *
   * @return a new WasmValue representing null exnref bottom type
   */
  public static WasmValue nullNullExnRef() {
    return NULL_NULLEXNREF;
  }

  /**
   * Creates a continuation reference value.
   *
   * <p>This is used in the WebAssembly stack switching proposal for references to continuation
   * values.
   *
   * @param value the continuation reference value (may be null)
   * @return a new WasmValue representing contref
   */
  public static WasmValue contref(final Object value) {
    return new WasmValue(WasmValueType.CONTREF, value);
  }

  /**
   * Creates a null contref value.
   *
   * <p>This is used in the WebAssembly stack switching proposal for null continuation references.
   *
   * @return a new WasmValue representing null contref
   */
  public static WasmValue nullContRef() {
    return NULL_CONTREF;
  }

  /**
   * Creates a null contref bottom type value.
   *
   * <p>NULLCONTREF can only hold null and is a subtype of all nullable continuation reference
   * types.
   *
   * @return a new WasmValue representing null contref bottom type
   */
  public static WasmValue nullNullContRef() {
    return NULL_NULLCONTREF;
  }

  /**
   * Creates a null reference of the nofunc (bottom) type.
   *
   * <p>NOFUNC is the bottom type in the funcref hierarchy. A null nofunc reference is a subtype of
   * all nullable function reference types. This is equivalent to {@link #nullNullFuncRef()}.
   *
   * @return a new WasmValue representing a null nofunc reference
   */
  public static WasmValue noFuncNullRef() {
    return NULL_NULLFUNCREF;
  }

  /**
   * Creates a null reference of the noextern (bottom) type.
   *
   * <p>NOEXTERN is the bottom type in the externref hierarchy. A null noextern reference is a
   * subtype of all nullable external reference types. This is equivalent to {@link
   * #nullNullExternRef()}.
   *
   * @return a new WasmValue representing a null noextern reference
   */
  public static WasmValue noExternNullRef() {
    return NULL_NULLEXTERNREF;
  }

  /**
   * Creates a null reference of the noexn (bottom) type.
   *
   * <p>NOEXN is the bottom type in the exnref hierarchy. A null noexn reference is a subtype of all
   * nullable exception reference types. This is equivalent to {@link #nullNullExnRef()}.
   *
   * @return a new WasmValue representing a null noexn reference
   */
  public static WasmValue noExnNullRef() {
    return NULL_NULLEXNREF;
  }

  /**
   * Checks if this value is an exception reference.
   *
   * @return true if this value is of type EXNREF, false otherwise
   */
  public boolean isExnref() {
    return type == WasmValueType.EXNREF;
  }

  /**
   * Checks if this value is a continuation reference.
   *
   * @return true if this value is of type CONTREF, false otherwise
   */
  public boolean isContref() {
    return type == WasmValueType.CONTREF;
  }

  /**
   * Checks if this value is a reference type (any WebAssembly reference type).
   *
   * @return true if this is a reference type, false otherwise
   */
  public boolean isReference() {
    return type.isReference();
  }

  /**
   * Checks if this value is a numeric type (i32, i64, f32, f64).
   *
   * @return true if this is a numeric type, false otherwise
   */
  public boolean isNumeric() {
    return type == WasmValueType.I32
        || type == WasmValueType.I64
        || type == WasmValueType.F32
        || type == WasmValueType.F64;
  }

  /**
   * Checks if this value is a vector type (v128).
   *
   * @return true if this is a vector type, false otherwise
   */
  public boolean isVector() {
    return type == WasmValueType.V128;
  }

  /**
   * Converts this value to a {@link ai.tegmentum.wasmtime4j.type.Ref} if it holds a reference type.
   *
   * <p>The method name uses a trailing underscore because {@code ref} is a reserved word in Java.
   * This corresponds to extracting the {@code Ref} union from a WebAssembly value.
   *
   * <p>For null reference values, returns a null {@code Ref} of the appropriate kind. For non-null
   * references, wraps the underlying value in the appropriate {@code Ref} variant.
   *
   * @return the reference, or empty if this is not a reference type
   * @since 1.1.0
   */
  public java.util.Optional<ai.tegmentum.wasmtime4j.type.Ref> ref_() {
    if (!type.isReference()) {
      return java.util.Optional.empty();
    }
    switch (type) {
      case FUNCREF:
        if (value == null) {
          return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullFuncRef());
        }
        if (value instanceof WasmFunction) {
          return java.util.Optional.of(
              ai.tegmentum.wasmtime4j.type.Ref.fromFunc((WasmFunction) value));
        }
        return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullFuncRef());
      case NULLFUNCREF:
        return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullFuncRef());
      case EXTERNREF:
        if (value == null) {
          return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullExternRef());
        }
        if (value instanceof ExternRef) {
          return java.util.Optional.of(
              ai.tegmentum.wasmtime4j.type.Ref.fromExtern((ExternRef<?>) value));
        }
        return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullExternRef());
      case NULLEXTERNREF:
        return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullExternRef());
      case ANYREF:
      case EQREF:
      case I31REF:
      case STRUCTREF:
      case ARRAYREF:
      case NULLREF:
        if (value == null) {
          return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullAnyRef());
        }
        if (value instanceof ai.tegmentum.wasmtime4j.gc.AnyRef) {
          return java.util.Optional.of(
              ai.tegmentum.wasmtime4j.type.Ref.fromAny((ai.tegmentum.wasmtime4j.gc.AnyRef) value));
        }
        return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullAnyRef());
      case EXNREF:
        if (value == null) {
          return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullExnRef());
        }
        if (value instanceof ExnRef) {
          return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.fromExn((ExnRef) value));
        }
        return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullExnRef());
      case NULLEXNREF:
        return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullExnRef());
      case CONTREF:
        return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullContRef());
      case NULLCONTREF:
        return java.util.Optional.of(ai.tegmentum.wasmtime4j.type.Ref.nullContRef());
      default:
        return java.util.Optional.empty();
    }
  }

  /**
   * Validates that this value matches the expected type.
   *
   * @param expectedType the expected type
   * @throws IllegalArgumentException if types don't match
   */
  public void validateType(final WasmValueType expectedType) {
    if (expectedType == null) {
      throw new IllegalArgumentException("Expected type cannot be null");
    }
    if (type != expectedType) {
      throw new IllegalArgumentException(
          "Type mismatch: expected " + expectedType + ", got " + type);
    }
  }

  /**
   * Checks if this value's type matches the expected ValType.
   *
   * <p>Unlike {@link #validateType(WasmValueType)} which checks for exact type equality, this
   * method uses {@link ValType#matches(ValType)} which accounts for the subtyping relationship
   * between reference types. For example, a NULLREF value matches ANYREF because NULLREF is a
   * subtype of ANYREF.
   *
   * @param expected the expected ValType to check against
   * @return true if this value's type matches the expected type, false otherwise
   * @throws IllegalArgumentException if expected is null
   */
  public boolean matchesType(final ValType expected) {
    if (expected == null) {
      throw new IllegalArgumentException("Expected type cannot be null");
    }
    final ValType actual = ValType.from(type);
    return actual.matches(expected);
  }

  /**
   * Returns the default value for the given WebAssembly value type.
   *
   * <p>For numeric types this returns zero. For reference types this returns null. This corresponds
   * to Wasmtime's {@code Val::default_for_ty()}.
   *
   * @param type the WebAssembly value type
   * @return a new WasmValue with the default value for that type
   * @throws IllegalArgumentException if type is null or unknown
   */
  public static WasmValue defaultForType(final WasmValueType type) {
    if (type == null) {
      throw new IllegalArgumentException("Type cannot be null");
    }
    switch (type) {
      case I32:
        return i32(0);
      case I64:
        return i64(0L);
      case F32:
        return f32(0.0f);
      case F64:
        return f64(0.0);
      case V128:
        return v128(new byte[16]);
      case FUNCREF:
        return nullFuncref();
      case EXTERNREF:
        return nullExternref();
      case ANYREF:
        return nullAnyRef();
      case EQREF:
        return nullEqRef();
      case I31REF:
        return nullI31Ref();
      case STRUCTREF:
        return nullStructRef();
      case ARRAYREF:
        return nullArrayRef();
      case NULLREF:
        return nullRef();
      case NULLFUNCREF:
        return nullNullFuncRef();
      case NULLEXTERNREF:
        return nullNullExternRef();
      case EXNREF:
        return nullExnRef();
      case NULLEXNREF:
        return nullNullExnRef();
      case CONTREF:
        return nullContRef();
      case NULLCONTREF:
        return nullNullContRef();
      default:
        throw new IllegalArgumentException("Unknown WasmValueType: " + type);
    }
  }

  /**
   * Creates an array of values from individual WasmValues.
   *
   * @param values the values to combine into an array
   * @return array of WasmValues
   * @throws IllegalArgumentException if values is null
   */
  public static WasmValue[] multiValue(final WasmValue... values) {
    if (values == null) {
      throw new IllegalArgumentException("Values array cannot be null");
    }
    return values.clone();
  }

  /**
   * Validates an array of WasmValues against expected types.
   *
   * @param values the values to validate
   * @param expectedTypes the expected types
   * @throws IllegalArgumentException if validation fails
   */
  public static void validateMultiValue(
      final WasmValue[] values, final WasmValueType[] expectedTypes) {
    if (values == null) {
      throw new IllegalArgumentException("Values array cannot be null");
    }
    if (expectedTypes == null) {
      throw new IllegalArgumentException("Expected types array cannot be null");
    }
    if (values.length != expectedTypes.length) {
      throw new IllegalArgumentException(
          "Value count mismatch: expected " + expectedTypes.length + ", got " + values.length);
    }

    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        throw new IllegalArgumentException("Value at index " + i + " is null");
      }
      values[i].validateType(expectedTypes[i]);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final WasmValue other = (WasmValue) obj;
    if (type != other.type) {
      return false;
    }

    // Handle null values
    if (value == null) {
      return other.value == null;
    }

    // Special handling for byte arrays (V128)
    if (type == WasmValueType.V128) {
      return java.util.Arrays.equals((byte[]) value, (byte[]) other.value);
    }

    // For all other types, use standard equals
    return value.equals(other.value);
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    if (value != null) {
      if (type == WasmValueType.V128) {
        result = 31 * result + java.util.Arrays.hashCode((byte[]) value);
      } else {
        result = 31 * result + value.hashCode();
      }
    }
    return result;
  }

  @Override
  public String toString() {
    if (type == WasmValueType.V128) {
      final byte[] bytes = (byte[]) value;
      final StringBuilder sb = new StringBuilder("WasmValue{type=V128, value=[");
      for (int i = 0; i < bytes.length; i++) {
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(String.format("0x%02x", bytes[i] & 0xFF));
      }
      sb.append("]}");
      return sb.toString();
    }
    return String.format("WasmValue{type=%s, value=%s}", type, value);
  }
}

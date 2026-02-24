package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.func.FunctionReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
   * Creates a 32-bit integer value.
   *
   * @param value the integer value
   * @return a new WasmValue
   */
  public static WasmValue i32(final int value) {
    return new WasmValue(WasmValueType.I32, value);
  }

  /**
   * Creates a 64-bit integer value.
   *
   * @param value the long value
   * @return a new WasmValue
   */
  public static WasmValue i64(final long value) {
    return new WasmValue(WasmValueType.I64, value);
  }

  /**
   * Creates a 32-bit floating-point value.
   *
   * @param value the float value
   * @return a new WasmValue
   */
  public static WasmValue f32(final float value) {
    return new WasmValue(WasmValueType.F32, value);
  }

  /**
   * Creates a 64-bit floating-point value.
   *
   * @param value the double value
   * @return a new WasmValue
   */
  public static WasmValue f64(final double value) {
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
    return new WasmValue(WasmValueType.FUNCREF, null);
  }

  /**
   * Creates a null externref value.
   *
   * @return a new WasmValue representing null externref
   */
  public static WasmValue nullExternref() {
    return new WasmValue(WasmValueType.EXTERNREF, null);
  }

  /**
   * Creates a null anyref value.
   *
   * <p>This is used in WebAssembly GC for null references in the anyref hierarchy.
   *
   * @return a new WasmValue representing null anyref
   */
  public static WasmValue nullAnyRef() {
    return new WasmValue(WasmValueType.ANYREF, null);
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
    return new WasmValue(WasmValueType.EQREF, null);
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
    return new WasmValue(WasmValueType.I31REF, null);
  }

  /**
   * Creates a null structref value.
   *
   * <p>This is used in WebAssembly GC for null struct references.
   *
   * @return a new WasmValue representing null structref
   */
  public static WasmValue nullStructRef() {
    return new WasmValue(WasmValueType.STRUCTREF, null);
  }

  /**
   * Creates a null arrayref value.
   *
   * <p>This is used in WebAssembly GC for null array references.
   *
   * @return a new WasmValue representing null arrayref
   */
  public static WasmValue nullArrayRef() {
    return new WasmValue(WasmValueType.ARRAYREF, null);
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
    return new WasmValue(WasmValueType.NULLREF, null);
  }

  /**
   * Creates a null funcref bottom type value.
   *
   * <p>This is used in WebAssembly GC for the null bottom type in the funcref hierarchy. NULLFUNCREF
   * can only hold null and is a subtype of all nullable function reference types.
   *
   * @return a new WasmValue representing null funcref bottom type
   */
  public static WasmValue nullNullFuncRef() {
    return new WasmValue(WasmValueType.NULLFUNCREF, null);
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
    return new WasmValue(WasmValueType.NULLEXTERNREF, null);
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
    return new WasmValue(WasmValueType.EXNREF, null);
  }

  /**
   * Creates a null exnref bottom type value.
   *
   * <p>NULLEXNREF can only hold null and is a subtype of all nullable exception reference types.
   *
   * @return a new WasmValue representing null exnref bottom type
   */
  public static WasmValue nullNullExnRef() {
    return new WasmValue(WasmValueType.NULLEXNREF, null);
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
    return new WasmValue(WasmValueType.CONTREF, null);
  }

  /**
   * Creates a null contref bottom type value.
   *
   * <p>NULLCONTREF can only hold null and is a subtype of all nullable continuation reference types.
   *
   * @return a new WasmValue representing null contref bottom type
   */
  public static WasmValue nullNullContRef() {
    return new WasmValue(WasmValueType.NULLCONTREF, null);
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

  /**
   * Checks if an array of values represents a multi-value result (more than one value).
   *
   * @param values the values to check
   * @return true if the array contains multiple values, false otherwise
   * @deprecated Array length checks do not belong on a value class. Use {@code values.length > 1}
   *     directly.
   */
  @Deprecated
  public static boolean isMultiValue(final WasmValue[] values) {
    return values != null && values.length > 1;
  }

  /**
   * Gets the first value from a multi-value result, or null if empty.
   *
   * @param values the values array
   * @return the first value, or null if empty
   * @deprecated Array access utilities do not belong on a value class. Use {@code values[0]}
   *     directly.
   */
  @Deprecated
  public static WasmValue getFirstValue(final WasmValue[] values) {
    return (values != null && values.length > 0) ? values[0] : null;
  }

  /**
   * Gets the last value from a multi-value result, or null if empty.
   *
   * @param values the values array
   * @return the last value, or null if empty
   * @deprecated Array access utilities do not belong on a value class. Use {@code
   *     values[values.length - 1]} directly.
   */
  @Deprecated
  public static WasmValue getLastValue(final WasmValue[] values) {
    return (values != null && values.length > 0) ? values[values.length - 1] : null;
  }

  /**
   * Extracts values of a specific type from a multi-value result.
   *
   * @param values the values array
   * @param targetType the type to extract
   * @return array of values matching the target type
   * @deprecated Array filtering utilities do not belong on a value class. Use streams directly.
   */
  @Deprecated
  public static WasmValue[] extractByType(
      final WasmValue[] values, final WasmValueType targetType) {
    if (values == null || targetType == null) {
      return new WasmValue[0];
    }

    return java.util.Arrays.stream(values)
        .filter(v -> v != null && v.getType() == targetType)
        .toArray(WasmValue[]::new);
  }

  /**
   * Converts a multi-value result to a string representation.
   *
   * @param values the values array
   * @return string representation of the multi-value result
   * @deprecated Use {@link java.util.Arrays#toString(Object[])} directly.
   */
  @Deprecated
  public static String multiValueToString(final WasmValue[] values) {
    if (values == null) {
      return "null";
    }
    if (values.length == 0) {
      return "[]";
    }
    if (values.length == 1) {
      return "[" + values[0].toString() + "]";
    }

    final StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(values[i] != null ? values[i].toString() : "null");
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Creates a deep copy of a multi-value result.
   *
   * @param values the values to copy
   * @return deep copy of the values array
   * @deprecated Use {@link Object#clone()} on the array directly.
   */
  @Deprecated
  @SuppressFBWarnings(
      value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS",
      justification = "Null input produces null output - distinct from empty array input/output")
  public static WasmValue[] copyMultiValue(final WasmValue[] values) {
    if (values == null) {
      return null;
    }
    return values.clone();
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

package ai.tegmentum.wasmtime4j;

/**
 * Represents a WebAssembly value that can be passed to and from WebAssembly functions.
 *
 * <p>This class encapsulates all WebAssembly value types including 32-bit and 64-bit integers,
 * 32-bit and 64-bit floating-point numbers, 128-bit vectors, and reference types (funcref and
 * externref). Each value maintains its type information for proper validation and conversion.
 *
 * <p>WebAssembly values are immutable and type-safe. Once created, a value's type and content
 * cannot be changed. Type conversions must be explicit and will throw exceptions if attempted
 * with incompatible types.
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
 * int resultInt = results[0].asI32();
 * float resultFloat = results[1].asF32();
 * }</pre>
 *
 * <p>All value creation methods perform validation to ensure type safety and correctness
 * according to WebAssembly specifications.
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
   * Gets this value as a 32-bit integer (alias for asInt()).
   *
   * @return the integer value
   * @throws ClassCastException if this value is not an integer
   */
  public int asI32() {
    return asInt();
  }

  /**
   * Gets this value as a 64-bit integer (alias for asLong()).
   *
   * @return the long value
   * @throws ClassCastException if this value is not a long
   */
  public long asI64() {
    return asLong();
  }

  /**
   * Gets this value as a 32-bit float (alias for asFloat()).
   *
   * @return the float value
   * @throws ClassCastException if this value is not a float
   */
  public float asF32() {
    return asFloat();
  }

  /**
   * Gets this value as a 64-bit double (alias for asDouble()).
   *
   * @return the double value
   * @throws ClassCastException if this value is not a double
   */
  public double asF64() {
    return asDouble();
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
   * Creates a function reference value.
   *
   * @param value the function reference (nullable)
   * @return a new WasmValue
   */
  public static WasmValue funcref(final Object value) {
    return new WasmValue(WasmValueType.FUNCREF, value);
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
   * Creates a 32-bit integer value (alias for i32).
   *
   * @param value the integer value
   * @return a new WasmValue
   */
  public static WasmValue ofI32(final int value) {
    return i32(value);
  }

  /**
   * Creates a 64-bit integer value (alias for i64).
   *
   * @param value the long value
   * @return a new WasmValue
   */
  public static WasmValue ofI64(final long value) {
    return i64(value);
  }

  /**
   * Creates a 32-bit floating-point value (alias for f32).
   *
   * @param value the float value
   * @return a new WasmValue
   */
  public static WasmValue ofF32(final float value) {
    return f32(value);
  }

  /**
   * Creates a 64-bit floating-point value (alias for f64).
   *
   * @param value the double value
   * @return a new WasmValue
   */
  public static WasmValue ofF64(final double value) {
    return f64(value);
  }

  /**
   * Checks if this value is a reference type (funcref or externref).
   *
   * @return true if this is a reference type, false otherwise
   */
  public boolean isReference() {
    return type == WasmValueType.FUNCREF || type == WasmValueType.EXTERNREF;
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

package ai.tegmentum.wasmtime4j;

/**
 * Represents a WebAssembly value.
 *
 * <p>This class encapsulates values that can be passed to and from WebAssembly functions.
 * WebAssembly values can be integers, floating-point numbers, or references.
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
      throw new ClassCastException("Value is not a funcref");
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
      throw new ClassCastException("Value is not an externref");
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

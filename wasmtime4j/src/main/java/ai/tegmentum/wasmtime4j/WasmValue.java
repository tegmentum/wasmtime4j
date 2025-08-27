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

  @Override
  public String toString() {
    return String.format("WasmValue{type=%s, value=%s}", type, value);
  }
}

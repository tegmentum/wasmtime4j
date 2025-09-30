package ai.tegmentum.wasmtime4j.debug;

/**
 * Variable value interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface VariableValue {

  /**
   * Gets the value as string.
   *
   * @return string representation
   */
  String asString();

  /**
   * Gets the value as integer.
   *
   * @return integer value
   */
  int asInt();

  /**
   * Gets the value as long.
   *
   * @return long value
   */
  long asLong();

  /**
   * Gets the value as float.
   *
   * @return float value
   */
  float asFloat();

  /**
   * Gets the value as double.
   *
   * @return double value
   */
  double asDouble();

  /**
   * Gets the value as boolean.
   *
   * @return boolean value
   */
  boolean asBoolean();

  /**
   * Gets the raw value.
   *
   * @return raw value object
   */
  Object getRawValue();

  /**
   * Gets the value type.
   *
   * @return value type
   */
  ValueType getValueType();

  /** Value type enumeration. */
  enum ValueType {
    /** Integer type. */
    INTEGER,
    /** Long type. */
    LONG,
    /** Float type. */
    FLOAT,
    /** Double type. */
    DOUBLE,
    /** Boolean type. */
    BOOLEAN,
    /** String type. */
    STRING,
    /** Object type. */
    OBJECT
  }
}

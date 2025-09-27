package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a WebAssembly global variable instance.
 *
 * <p>WebAssembly globals are values that can be accessed globally by all instances
 * in a module. Globals can be either mutable or immutable, and can store values
 * of various types.
 *
 * @since 1.0.0
 */
public interface Global {

  /**
   * Gets the current value of this global variable.
   *
   * @return the current value
   * @throws WasmException if the operation fails
   */
  Object getValue() throws WasmException;

  /**
   * Sets the value of this global variable.
   *
   * @param value the new value to set
   * @throws WasmException if the global is immutable or the operation fails
   * @throws IllegalArgumentException if the value type is incompatible
   */
  void setValue(final Object value) throws WasmException;

  /**
   * Gets the value type of this global variable.
   *
   * @return the value type
   */
  GlobalValueType getValueType();

  /**
   * Checks if this global variable is mutable.
   *
   * @return true if the global is mutable, false if immutable
   */
  boolean isMutable();

  /**
   * Checks if this global instance is still valid.
   *
   * @return true if the global is valid and can be used, false otherwise
   */
  boolean isValid();

  /**
   * Gets the current value as an integer.
   *
   * @return the current value as an integer
   * @throws WasmException if the value type is not an integer or the operation fails
   */
  int getIntValue() throws WasmException;

  /**
   * Gets the current value as a long.
   *
   * @return the current value as a long
   * @throws WasmException if the value type is not a long or the operation fails
   */
  long getLongValue() throws WasmException;

  /**
   * Gets the current value as a float.
   *
   * @return the current value as a float
   * @throws WasmException if the value type is not a float or the operation fails
   */
  float getFloatValue() throws WasmException;

  /**
   * Gets the current value as a double.
   *
   * @return the current value as a double
   * @throws WasmException if the value type is not a double or the operation fails
   */
  double getDoubleValue() throws WasmException;

  /**
   * Sets the value as an integer.
   *
   * @param value the integer value to set
   * @throws WasmException if the global is immutable, value type is incompatible, or the operation fails
   */
  void setIntValue(final int value) throws WasmException;

  /**
   * Sets the value as a long.
   *
   * @param value the long value to set
   * @throws WasmException if the global is immutable, value type is incompatible, or the operation fails
   */
  void setLongValue(final long value) throws WasmException;

  /**
   * Sets the value as a float.
   *
   * @param value the float value to set
   * @throws WasmException if the global is immutable, value type is incompatible, or the operation fails
   */
  void setFloatValue(final float value) throws WasmException;

  /**
   * Sets the value as a double.
   *
   * @param value the double value to set
   * @throws WasmException if the global is immutable, value type is incompatible, or the operation fails
   */
  void setDoubleValue(final double value) throws WasmException;

  /**
   * Enumeration of supported global value types.
   */
  enum GlobalValueType {
    /** 32-bit integer */
    I32,
    /** 64-bit integer */
    I64,
    /** 32-bit float */
    F32,
    /** 64-bit float */
    F64,
    /** 128-bit vector */
    V128,
    /** Function reference */
    FUNCREF,
    /** External reference */
    EXTERNREF
  }
}
package ai.tegmentum.wasmtime4j;

/**
 * Represents a WebAssembly global variable.
 *
 * <p>Globals are WebAssembly values that can be read and (if mutable) written. They provide a way
 * to share state across function calls.
 *
 * @since 1.0.0
 */
public interface WasmGlobal {

  /**
   * Gets the value of this global.
   *
   * @return the current value
   */
  WasmValue get();

  /**
   * Gets the value of this global. Alias for get() for compatibility.
   *
   * @return the current value
   */
  default WasmValue getValue() {
    return get();
  }

  /**
   * Sets the value of this global.
   *
   * @param value the new value
   * @throws ai.tegmentum.wasmtime4j.exception.ValidationException if the global is immutable
   */
  void set(final WasmValue value);

  /**
   * Gets the type of this global.
   *
   * @return the global type
   */
  WasmValueType getType();

  /**
   * Checks if this global is mutable.
   *
   * @return true if the global is mutable
   */
  boolean isMutable();
}

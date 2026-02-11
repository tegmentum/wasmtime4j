package ai.tegmentum.wasmtime4j.type;

import ai.tegmentum.wasmtime4j.WasmValueType;

/**
 * Represents the type information of a WebAssembly global.
 *
 * <p>This interface provides access to global type metadata including value type and mutability.
 * Globals are variables that exist at the module level and can be exported or imported.
 *
 * @since 1.0.0
 */
public interface GlobalType extends WasmType {

  /**
   * Gets the value type of this global.
   *
   * @return the value type
   */
  WasmValueType getValueType();

  /**
   * Checks if this global is mutable.
   *
   * @return true if mutable, false if immutable (const)
   */
  boolean isMutable();

  @Override
  default WasmTypeKind getKind() {
    return WasmTypeKind.GLOBAL;
  }
}

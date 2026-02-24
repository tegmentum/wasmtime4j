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

  /**
   * Gets the content type of this global as a {@link ValType}.
   *
   * <p>This returns the full {@link ValType} for the global's content, which provides richer type
   * information than the raw {@link WasmValueType} enum from {@link #getValueType()}.
   *
   * @return the content value type
   * @since 1.1.0
   */
  default ValType getContent() {
    return ValType.from(getValueType());
  }

  /**
   * Gets the mutability of this global as a typed enum.
   *
   * @return the mutability (CONST or VAR)
   * @since 1.1.0
   */
  default Mutability getMutability() {
    return Mutability.fromBoolean(isMutable());
  }

  /**
   * Creates a GlobalType with the specified value type and mutability.
   *
   * @param valueType the value type of the global
   * @param mutability the mutability of the global
   * @return a new GlobalType
   * @throws IllegalArgumentException if valueType or mutability is null
   */
  static GlobalType of(final WasmValueType valueType, final Mutability mutability) {
    if (valueType == null) {
      throw new IllegalArgumentException("valueType cannot be null");
    }
    if (mutability == null) {
      throw new IllegalArgumentException("mutability cannot be null");
    }
    return new DefaultGlobalType(valueType, mutability);
  }

  @Override
  default WasmTypeKind getKind() {
    return WasmTypeKind.GLOBAL;
  }
}

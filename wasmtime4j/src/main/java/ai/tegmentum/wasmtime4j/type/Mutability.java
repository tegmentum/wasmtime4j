package ai.tegmentum.wasmtime4j.type;

/**
 * Represents the mutability of a WebAssembly global variable.
 *
 * <p>WebAssembly globals can be either immutable (constant) or mutable (variable). This enum
 * provides a type-safe representation of this attribute.
 *
 * @since 1.1.0
 */
public enum Mutability {

  /** An immutable (constant) global. */
  CONST,

  /** A mutable (variable) global. */
  VAR;

  /**
   * Checks if this represents an immutable global.
   *
   * @return true if this is CONST
   */
  public boolean isConst() {
    return this == CONST;
  }

  /**
   * Checks if this represents a mutable global.
   *
   * @return true if this is VAR
   */
  public boolean isVar() {
    return this == VAR;
  }

  /**
   * Converts a boolean mutability flag to a Mutability enum value.
   *
   * @param mutable true for VAR, false for CONST
   * @return the corresponding Mutability value
   */
  public static Mutability fromBoolean(final boolean mutable) {
    return mutable ? VAR : CONST;
  }
}

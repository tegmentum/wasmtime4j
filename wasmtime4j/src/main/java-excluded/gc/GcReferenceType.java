package ai.tegmentum.wasmtime4j.gc;

/**
 * Enumeration of WebAssembly GC reference types.
 *
 * <p>This enum represents the hierarchy of reference types in the WebAssembly GC proposal:
 *
 * <ul>
 *   <li>{@link #ANY_REF} - Top type in the reference hierarchy
 *   <li>{@link #EQ_REF} - Equality-testable references
 *   <li>{@link #I31_REF} - Immediate 31-bit integer references
 *   <li>{@link #STRUCT_REF} - References to struct instances
 *   <li>{@link #ARRAY_REF} - References to array instances
 * </ul>
 *
 * @since 1.0.0
 */
public enum GcReferenceType {
  /** Top type in the reference hierarchy - all GC references are subtypes of anyref. */
  ANY_REF("anyref"),

  /** Equality-testable references - subset of anyref that supports ref.eq. */
  EQ_REF("eqref"),

  /** Immediate 31-bit integer references for efficient small integer storage. */
  I31_REF("i31ref"),

  /** References to struct instances with typed field access. */
  STRUCT_REF("structref"),

  /** References to array instances with element type information. */
  ARRAY_REF("arrayref");

  private final String wasmName;

  GcReferenceType(final String wasmName) {
    this.wasmName = wasmName;
  }

  /**
   * Gets the WebAssembly name for this reference type.
   *
   * @return the WebAssembly name
   */
  public String getWasmName() {
    return wasmName;
  }

  /**
   * Checks if this type is a subtype of another reference type.
   *
   * @param supertype the potential supertype
   * @return true if this type is a subtype of the given supertype
   */
  public boolean isSubtypeOf(final GcReferenceType supertype) {
    switch (this) {
      case ANY_REF:
        return supertype == ANY_REF;
      case EQ_REF:
        return supertype == ANY_REF || supertype == EQ_REF;
      case I31_REF:
        return supertype == ANY_REF || supertype == EQ_REF || supertype == I31_REF;
      case STRUCT_REF:
        return supertype == ANY_REF || supertype == EQ_REF || supertype == STRUCT_REF;
      case ARRAY_REF:
        return supertype == ANY_REF || supertype == EQ_REF || supertype == ARRAY_REF;
      default:
        return false;
    }
  }

  /**
   * Checks if this type supports equality comparison (ref.eq).
   *
   * @return true if this type supports equality comparison
   */
  public boolean supportsEquality() {
    return isSubtypeOf(EQ_REF);
  }

  @Override
  public String toString() {
    return wasmName;
  }
}

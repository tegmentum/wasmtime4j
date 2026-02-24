package ai.tegmentum.wasmtime4j;

/**
 * WebAssembly value types.
 *
 * <p>These represent the fundamental value types in WebAssembly that can be used as parameters and
 * return values for functions.
 *
 * @since 1.0.0
 */
public enum WasmValueType {
  /** 32-bit integer type. */
  I32(4, true, false),

  /** 64-bit integer type. */
  I64(8, true, false),

  /** 32-bit floating-point type. */
  F32(4, false, true),

  /** 64-bit floating-point type. */
  F64(8, false, true),

  /** 128-bit vector type (SIMD). */
  V128(16, false, false),

  /** Reference to a function. */
  FUNCREF(-1, false, false),

  /** Reference to external data. */
  EXTERNREF(-1, false, false),

  // WasmGC reference types

  /** Top type in the GC reference hierarchy - all GC references are subtypes of anyref. */
  ANYREF(-1, false, false),

  /** Equality-testable references - subset of anyref that supports ref.eq. */
  EQREF(-1, false, false),

  /** Immediate 31-bit integer references for efficient small integer storage. */
  I31REF(-1, false, false),

  /** References to struct instances with typed field access. */
  STRUCTREF(-1, false, false),

  /** References to array instances with element type information. */
  ARRAYREF(-1, false, false),

  /** The null reference type - bottom type for nullable references. */
  NULLREF(-1, false, false),

  /** Nullable function reference type. */
  NULLFUNCREF(-1, false, false),

  /** Nullable external reference type. */
  NULLEXTERNREF(-1, false, false),

  /** Exception reference type (exception handling proposal). */
  EXNREF(-1, false, false),

  /** Null exception reference type - bottom of the exn hierarchy. */
  NULLEXNREF(-1, false, false),

  /** Continuation reference type (stack switching proposal). */
  CONTREF(-1, false, false),

  /** Null continuation reference type - bottom of the cont hierarchy. */
  NULLCONTREF(-1, false, false);

  private final int size;
  private final boolean isInteger;
  private final boolean isFloat;

  WasmValueType(final int size, final boolean isInteger, final boolean isFloat) {
    this.size = size;
    this.isInteger = isInteger;
    this.isFloat = isFloat;
  }

  /**
   * Gets the size in bytes of this value type.
   *
   * @return the size in bytes, or -1 for reference types
   */
  public int getSize() {
    return size;
  }

  /**
   * Checks if this is an integer type.
   *
   * @return true if this is I32 or I64
   */
  public boolean isInteger() {
    return isInteger;
  }

  /**
   * Checks if this is a floating-point type.
   *
   * @return true if this is F32 or F64
   */
  public boolean isFloat() {
    return isFloat;
  }

  /**
   * Checks if this is a reference type.
   *
   * @return true if this is a reference type (FUNCREF, EXTERNREF, or any GC reference type)
   */
  public boolean isReference() {
    switch (this) {
      case FUNCREF:
      case EXTERNREF:
      case ANYREF:
      case EQREF:
      case I31REF:
      case STRUCTREF:
      case ARRAYREF:
      case NULLREF:
      case NULLFUNCREF:
      case NULLEXTERNREF:
      case EXNREF:
      case NULLEXNREF:
      case CONTREF:
      case NULLCONTREF:
        return true;
      default:
        return false;
    }
  }

  /**
   * Checks if this is a GC reference type.
   *
   * @return true if this is a WasmGC reference type
   */
  public boolean isGcReference() {
    switch (this) {
      case ANYREF:
      case EQREF:
      case I31REF:
      case STRUCTREF:
      case ARRAYREF:
      case NULLREF:
      case NULLFUNCREF:
      case NULLEXTERNREF:
      case NULLEXNREF:
      case CONTREF:
      case NULLCONTREF:
        return true;
      default:
        return false;
    }
  }

  /**
   * Checks if this is a nullable reference type.
   *
   * @return true if this is a nullable reference type
   */
  public boolean isNullableReference() {
    switch (this) {
      case NULLREF:
      case NULLFUNCREF:
      case NULLEXTERNREF:
      case NULLEXNREF:
      case NULLCONTREF:
        return true;
      default:
        return false;
    }
  }

  /**
   * Checks if this is a vector type.
   *
   * @return true if this is V128
   */
  public boolean isVector() {
    return this == V128;
  }

  /**
   * Checks if this is a numeric type (integer or float).
   *
   * @return true if this is a numeric type
   */
  public boolean isNumeric() {
    return isInteger || isFloat;
  }

  /**
   * Converts a native type code to a WasmValueType.
   *
   * @param typeCode the native type code from Wasmtime
   * @return the corresponding WasmValueType
   * @throws IllegalArgumentException if the type code is unknown
   */
  public static WasmValueType fromNativeTypeCode(final int typeCode) {
    switch (typeCode) {
      case 0:
        return I32;
      case 1:
        return I64;
      case 2:
        return F32;
      case 3:
        return F64;
      case 4:
        return V128;
      case 5:
        return FUNCREF;
      case 6:
        return EXTERNREF;
      // WasmGC type codes
      case 7:
        return ANYREF;
      case 8:
        return EQREF;
      case 9:
        return I31REF;
      case 10:
        return STRUCTREF;
      case 11:
        return ARRAYREF;
      case 12:
        return NULLREF;
      case 13:
        return NULLFUNCREF;
      case 14:
        return NULLEXTERNREF;
      case 15:
        return EXNREF;
      case 16:
        return NULLEXNREF;
      case 17:
        return CONTREF;
      case 18:
        return NULLCONTREF;
      default:
        throw new IllegalArgumentException("Unknown type code: " + typeCode);
    }
  }

  /**
   * Converts this WasmValueType to a native type code.
   *
   * @return the native type code for Wasmtime
   */
  public int toNativeTypeCode() {
    switch (this) {
      case I32:
        return 0;
      case I64:
        return 1;
      case F32:
        return 2;
      case F64:
        return 3;
      case V128:
        return 4;
      case FUNCREF:
        return 5;
      case EXTERNREF:
        return 6;
      // WasmGC type codes
      case ANYREF:
        return 7;
      case EQREF:
        return 8;
      case I31REF:
        return 9;
      case STRUCTREF:
        return 10;
      case ARRAYREF:
        return 11;
      case NULLREF:
        return 12;
      case NULLFUNCREF:
        return 13;
      case NULLEXTERNREF:
        return 14;
      case EXNREF:
        return 15;
      case NULLEXNREF:
        return 16;
      case CONTREF:
        return 17;
      case NULLCONTREF:
        return 18;
      default:
        throw new IllegalStateException("Unknown value type: " + this);
    }
  }

  /**
   * Checks if this type is a subtype of another type according to WasmGC subtyping rules.
   *
   * <p>The subtyping hierarchy for GC reference types is:
   *
   * <ul>
   *   <li>anyref is the top type for all GC references
   *   <li>eqref &lt;: anyref
   *   <li>i31ref &lt;: eqref
   *   <li>structref &lt;: eqref
   *   <li>arrayref &lt;: eqref
   *   <li>nullref is the bottom type for nullable references
   *   <li>exnref is in a separate hierarchy for exception handling
   * </ul>
   *
   * @param supertype the potential supertype
   * @return true if this type is a subtype of the given supertype
   */
  public boolean isSubtypeOf(final WasmValueType supertype) {
    if (this == supertype) {
      return true;
    }

    // Non-reference types only match themselves
    if (!this.isReference() || !supertype.isReference()) {
      return false;
    }

    // NULLREF is a subtype of all nullable reference types
    if (this == NULLREF) {
      return supertype.isReference();
    }

    // NULLFUNCREF is a subtype of FUNCREF
    if (this == NULLFUNCREF) {
      return supertype == FUNCREF || supertype == NULLFUNCREF;
    }

    // NULLEXTERNREF is a subtype of EXTERNREF
    if (this == NULLEXTERNREF) {
      return supertype == EXTERNREF || supertype == NULLEXTERNREF;
    }

    // NULLEXNREF is a subtype of EXNREF
    if (this == NULLEXNREF) {
      return supertype == EXNREF || supertype == NULLEXNREF;
    }

    // NULLCONTREF is a subtype of CONTREF
    if (this == NULLCONTREF) {
      return supertype == CONTREF || supertype == NULLCONTREF;
    }

    // GC reference type hierarchy
    switch (this) {
      case ANYREF:
        return supertype == ANYREF;
      case EQREF:
        return supertype == ANYREF || supertype == EQREF;
      case I31REF:
        return supertype == ANYREF || supertype == EQREF || supertype == I31REF;
      case STRUCTREF:
        return supertype == ANYREF || supertype == EQREF || supertype == STRUCTREF;
      case ARRAYREF:
        return supertype == ANYREF || supertype == EQREF || supertype == ARRAYREF;
      case CONTREF:
        return supertype == CONTREF;
      default:
        return false;
    }
  }
}

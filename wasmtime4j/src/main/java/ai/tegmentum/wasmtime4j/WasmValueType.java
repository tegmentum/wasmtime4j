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
  EXTERNREF(-1, false, false);

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
   * @return true if this is FUNCREF or EXTERNREF
   */
  public boolean isReference() {
    return this == FUNCREF || this == EXTERNREF;
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
    return switch (typeCode) {
      case 0 -> I32;
      case 1 -> I64;
      case 2 -> F32;
      case 3 -> F64;
      case 4 -> V128;
      case 5 -> FUNCREF;
      case 6 -> EXTERNREF;
      default -> throw new IllegalArgumentException("Unknown type code: " + typeCode);
    };
  }

  /**
   * Converts this WasmValueType to a native type code.
   *
   * @return the native type code for Wasmtime
   */
  public int toNativeTypeCode() {
    return switch (this) {
      case I32 -> 0;
      case I64 -> 1;
      case F32 -> 2;
      case F64 -> 3;
      case V128 -> 4;
      case FUNCREF -> 5;
      case EXTERNREF -> 6;
    };
  }
}

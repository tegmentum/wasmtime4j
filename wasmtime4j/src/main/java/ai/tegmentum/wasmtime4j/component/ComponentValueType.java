package ai.tegmentum.wasmtime4j.component;

/**
 * Enumeration of WebAssembly Component Model value types.
 *
 * <p>Defines the type system for the WebAssembly Component Model, including primitive types,
 * structured types, and special component-specific types according to the Component Model
 * specification.
 *
 * <p>Component value types support rich data structures and enable type-safe interaction between
 * components and host environments.
 *
 * @since 1.0.0
 */
public enum ComponentValueType {
  // Primitive numeric types
  /**
   * Boolean type (true/false).
   */
  BOOL,

  /**
   * Signed 8-bit integer.
   */
  S8,

  /**
   * Unsigned 8-bit integer.
   */
  U8,

  /**
   * Signed 16-bit integer.
   */
  S16,

  /**
   * Unsigned 16-bit integer.
   */
  U16,

  /**
   * Signed 32-bit integer.
   */
  S32,

  /**
   * Unsigned 32-bit integer.
   */
  U32,

  /**
   * Signed 64-bit integer.
   */
  S64,

  /**
   * Unsigned 64-bit integer.
   */
  U64,

  /**
   * 32-bit floating point number.
   */
  F32,

  /**
   * 64-bit floating point number.
   */
  F64,

  // Text and character types
  /**
   * Unicode character.
   */
  CHAR,

  /**
   * UTF-8 encoded string.
   */
  STRING,

  // Structured types
  /**
   * List/array of values of the same type.
   */
  LIST,

  /**
   * Record type with named fields of different types.
   */
  RECORD,

  /**
   * Variant type representing one of several possible types.
   */
  VARIANT,

  /**
   * Tuple type with multiple unnamed values.
   */
  TUPLE,

  /**
   * Flags type representing a set of boolean flags.
   */
  FLAGS,

  /**
   * Enumeration type with named constants.
   */
  ENUM,

  // Optional and result types
  /**
   * Optional type that may or may not contain a value.
   */
  OPTION,

  /**
   * Result type representing either success or error.
   */
  RESULT,

  // Resource types
  /**
   * Resource handle type for managing stateful objects.
   */
  RESOURCE,

  /**
   * Borrowed resource handle with limited lifetime.
   */
  BORROW,

  /**
   * Owned resource handle with transfer semantics.
   */
  OWN;

  /**
   * Checks if this type is a primitive numeric type.
   *
   * @return true if this is a numeric primitive type
   */
  public boolean isNumeric() {
    return this == S8 || this == U8 || this == S16 || this == U16
        || this == S32 || this == U32 || this == S64 || this == U64
        || this == F32 || this == F64;
  }

  /**
   * Checks if this type is a signed integer type.
   *
   * @return true if this is a signed integer type
   */
  public boolean isSignedInteger() {
    return this == S8 || this == S16 || this == S32 || this == S64;
  }

  /**
   * Checks if this type is an unsigned integer type.
   *
   * @return true if this is an unsigned integer type
   */
  public boolean isUnsignedInteger() {
    return this == U8 || this == U16 || this == U32 || this == U64;
  }

  /**
   * Checks if this type is a floating point type.
   *
   * @return true if this is a floating point type
   */
  public boolean isFloatingPoint() {
    return this == F32 || this == F64;
  }

  /**
   * Checks if this type is a text or character type.
   *
   * @return true if this is a text type
   */
  public boolean isText() {
    return this == CHAR || this == STRING;
  }

  /**
   * Checks if this type is a structured container type.
   *
   * @return true if this is a structured type
   */
  public boolean isStructured() {
    return this == LIST || this == RECORD || this == VARIANT
        || this == TUPLE || this == FLAGS || this == ENUM;
  }

  /**
   * Checks if this type is an optional or result type.
   *
   * @return true if this is an optional or result type
   */
  public boolean isOptionalOrResult() {
    return this == OPTION || this == RESULT;
  }

  /**
   * Checks if this type is a resource-related type.
   *
   * @return true if this is a resource type
   */
  public boolean isResource() {
    return this == RESOURCE || this == BORROW || this == OWN;
  }

  /**
   * Gets the size in bytes for primitive types.
   *
   * @return the size in bytes, or -1 for non-primitive or variable-size types
   */
  public int getSizeInBytes() {
    switch (this) {
      case BOOL:
      case S8:
      case U8:
        return 1;
      case S16:
      case U16:
        return 2;
      case S32:
      case U32:
      case F32:
      case CHAR:
        return 4;
      case S64:
      case U64:
      case F64:
        return 8;
      default:
        return -1; // Variable size or non-primitive
    }
  }
}
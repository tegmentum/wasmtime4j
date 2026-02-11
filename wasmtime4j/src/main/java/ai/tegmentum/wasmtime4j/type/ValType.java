package ai.tegmentum.wasmtime4j.type;
import ai.tegmentum.wasmtime4j.WasmValueType;

/**
 * Represents a WebAssembly value type.
 *
 * <p>ValType describes the types of values that can be stored in variables, passed as function
 * parameters, or returned from functions in WebAssembly. This includes numeric types (i32, i64,
 * f32, f64), vector types (v128), and reference types (funcref, externref).
 *
 * <p>Unlike {@link WasmValueType}, which is a simple enum, ValType provides richer querying
 * capabilities and type compatibility checking, particularly important for reference types which
 * have a subtyping relationship.
 *
 * @since 1.0.0
 */
public interface ValType {

  /**
   * Gets the underlying WebAssembly value type.
   *
   * @return the WasmValueType represented by this ValType
   */
  WasmValueType getValueType();

  /**
   * Checks if this is a numeric type.
   *
   * <p>Numeric types include I32, I64, F32, and F64.
   *
   * @return true if this is a numeric type, false otherwise
   */
  boolean isNumeric();

  /**
   * Checks if this is an integer type.
   *
   * <p>Integer types include I32 and I64.
   *
   * @return true if this is an integer type, false otherwise
   */
  boolean isInteger();

  /**
   * Checks if this is a floating-point type.
   *
   * <p>Floating-point types include F32 and F64.
   *
   * @return true if this is a floating-point type, false otherwise
   */
  boolean isFloat();

  /**
   * Checks if this is a reference type.
   *
   * <p>Reference types include FUNCREF, EXTERNREF, and all GC reference types.
   *
   * @return true if this is a reference type, false otherwise
   */
  boolean isReference();

  /**
   * Checks if this is a GC reference type.
   *
   * <p>GC reference types include ANYREF, EQREF, I31REF, STRUCTREF, ARRAYREF, NULLREF, NULLFUNCREF,
   * and NULLEXTERNREF.
   *
   * @return true if this is a GC reference type, false otherwise
   */
  boolean isGcReference();

  /**
   * Checks if this is a nullable reference type.
   *
   * <p>Nullable reference types include NULLREF, NULLFUNCREF, and NULLEXTERNREF.
   *
   * @return true if this is a nullable reference type, false otherwise
   */
  boolean isNullableReference();

  /**
   * Checks if this is a vector type.
   *
   * <p>Vector type is V128.
   *
   * @return true if this is V128, false otherwise
   */
  boolean isVector();

  /**
   * Checks if this type matches another type.
   *
   * <p>For most types, this is equivalent to equality. However, reference types have a subtyping
   * relationship, and this method accounts for that. For example, a more specific reference type
   * can match a more general reference type.
   *
   * @param other the type to check against
   * @return true if this type matches the other type, false otherwise
   */
  boolean matches(final ValType other);

  /**
   * Checks for precise type equality.
   *
   * <p>Unlike {@link #matches(ValType)}, this method checks for exact type equality without
   * considering subtyping relationships.
   *
   * @param other the type to compare with
   * @return true if the types are exactly equal, false otherwise
   */
  boolean eq(final ValType other);

  /**
   * Creates a ValType from a WasmValueType.
   *
   * @param valueType the WebAssembly value type
   * @return a ValType wrapping the given value type
   * @throws IllegalArgumentException if valueType is null
   */
  static ValType from(final WasmValueType valueType) {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates an I32 value type.
   *
   * @return an I32 ValType
   */
  static ValType i32() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates an I64 value type.
   *
   * @return an I64 ValType
   */
  static ValType i64() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates an F32 value type.
   *
   * @return an F32 ValType
   */
  static ValType f32() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates an F64 value type.
   *
   * @return an F64 ValType
   */
  static ValType f64() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates a V128 value type.
   *
   * @return a V128 ValType
   */
  static ValType v128() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates a FUNCREF value type.
   *
   * @return a FUNCREF ValType
   */
  static ValType funcref() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates an EXTERNREF value type.
   *
   * @return an EXTERNREF ValType
   */
  static ValType externref() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  // WasmGC reference type factory methods

  /**
   * Creates an ANYREF value type.
   *
   * <p>ANYREF is the top type in the GC reference hierarchy - all GC references are subtypes of
   * anyref.
   *
   * @return an ANYREF ValType
   */
  static ValType anyref() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates an EQREF value type.
   *
   * <p>EQREF is the type of equality-testable references - a subset of anyref that supports ref.eq.
   *
   * @return an EQREF ValType
   */
  static ValType eqref() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates an I31REF value type.
   *
   * <p>I31REF is the type for immediate 31-bit integer references for efficient small integer
   * storage.
   *
   * @return an I31REF ValType
   */
  static ValType i31ref() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates a STRUCTREF value type.
   *
   * <p>STRUCTREF is the type for references to struct instances with typed field access.
   *
   * @return a STRUCTREF ValType
   */
  static ValType structref() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates an ARRAYREF value type.
   *
   * <p>ARRAYREF is the type for references to array instances with element type information.
   *
   * @return an ARRAYREF ValType
   */
  static ValType arrayref() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates a NULLREF value type.
   *
   * <p>NULLREF is the null reference type - the bottom type for nullable references.
   *
   * @return a NULLREF ValType
   */
  static ValType nullref() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates a NULLFUNCREF value type.
   *
   * <p>NULLFUNCREF is the nullable function reference type.
   *
   * @return a NULLFUNCREF ValType
   */
  static ValType nullfuncref() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }

  /**
   * Creates a NULLEXTERNREF value type.
   *
   * <p>NULLEXTERNREF is the nullable external reference type.
   *
   * @return a NULLEXTERNREF ValType
   */
  static ValType nullexternref() {
    throw new UnsupportedOperationException(
        "Static factory method must be provided by implementation");
  }
}

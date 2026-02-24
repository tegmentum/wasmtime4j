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
   * Checks if this is the I32 type.
   *
   * @return true if this is I32
   */
  default boolean isI32() {
    return getValueType() == WasmValueType.I32;
  }

  /**
   * Checks if this is the I64 type.
   *
   * @return true if this is I64
   */
  default boolean isI64() {
    return getValueType() == WasmValueType.I64;
  }

  /**
   * Checks if this is the F32 type.
   *
   * @return true if this is F32
   */
  default boolean isF32() {
    return getValueType() == WasmValueType.F32;
  }

  /**
   * Checks if this is the F64 type.
   *
   * @return true if this is F64
   */
  default boolean isF64() {
    return getValueType() == WasmValueType.F64;
  }

  /**
   * Checks if this is the V128 type.
   *
   * @return true if this is V128
   */
  default boolean isV128() {
    return getValueType() == WasmValueType.V128;
  }

  /**
   * Checks if this is the FUNCREF type.
   *
   * @return true if this is FUNCREF
   */
  default boolean isFuncRef() {
    return getValueType() == WasmValueType.FUNCREF;
  }

  /**
   * Checks if this is the EXTERNREF type.
   *
   * @return true if this is EXTERNREF
   */
  default boolean isExternRef() {
    return getValueType() == WasmValueType.EXTERNREF;
  }

  /**
   * Checks if this is the ANYREF type.
   *
   * @return true if this is ANYREF
   */
  default boolean isAnyRef() {
    return getValueType() == WasmValueType.ANYREF;
  }

  /**
   * Checks if this is the EXNREF type.
   *
   * @return true if this is EXNREF
   */
  default boolean isExnRef() {
    return getValueType() == WasmValueType.EXNREF;
  }

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
    return SimpleValType.Factory.from(valueType);
  }

  /**
   * Creates an I32 value type.
   *
   * @return an I32 ValType
   */
  static ValType i32() {
    return SimpleValType.Factory.i32();
  }

  /**
   * Creates an I64 value type.
   *
   * @return an I64 ValType
   */
  static ValType i64() {
    return SimpleValType.Factory.i64();
  }

  /**
   * Creates an F32 value type.
   *
   * @return an F32 ValType
   */
  static ValType f32() {
    return SimpleValType.Factory.f32();
  }

  /**
   * Creates an F64 value type.
   *
   * @return an F64 ValType
   */
  static ValType f64() {
    return SimpleValType.Factory.f64();
  }

  /**
   * Creates a V128 value type.
   *
   * @return a V128 ValType
   */
  static ValType v128() {
    return SimpleValType.Factory.v128();
  }

  /**
   * Creates a FUNCREF value type.
   *
   * @return a FUNCREF ValType
   */
  static ValType funcref() {
    return SimpleValType.Factory.funcref();
  }

  /**
   * Creates an EXTERNREF value type.
   *
   * @return an EXTERNREF ValType
   */
  static ValType externref() {
    return SimpleValType.Factory.externref();
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
    return SimpleValType.Factory.anyref();
  }

  /**
   * Creates an EQREF value type.
   *
   * <p>EQREF is the type of equality-testable references - a subset of anyref that supports ref.eq.
   *
   * @return an EQREF ValType
   */
  static ValType eqref() {
    return SimpleValType.Factory.eqref();
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
    return SimpleValType.Factory.i31ref();
  }

  /**
   * Creates a STRUCTREF value type.
   *
   * <p>STRUCTREF is the type for references to struct instances with typed field access.
   *
   * @return a STRUCTREF ValType
   */
  static ValType structref() {
    return SimpleValType.Factory.structref();
  }

  /**
   * Creates an ARRAYREF value type.
   *
   * <p>ARRAYREF is the type for references to array instances with element type information.
   *
   * @return an ARRAYREF ValType
   */
  static ValType arrayref() {
    return SimpleValType.Factory.arrayref();
  }

  /**
   * Creates a NULLREF value type.
   *
   * <p>NULLREF is the null reference type - the bottom type for nullable references.
   *
   * @return a NULLREF ValType
   */
  static ValType nullref() {
    return SimpleValType.Factory.nullref();
  }

  /**
   * Creates a NULLFUNCREF value type.
   *
   * <p>NULLFUNCREF is the nullable function reference type.
   *
   * @return a NULLFUNCREF ValType
   */
  static ValType nullfuncref() {
    return SimpleValType.Factory.nullfuncref();
  }

  /**
   * Creates a NULLEXTERNREF value type.
   *
   * <p>NULLEXTERNREF is the nullable external reference type.
   *
   * @return a NULLEXTERNREF ValType
   */
  static ValType nullexternref() {
    return SimpleValType.Factory.nullexternref();
  }

  /**
   * Creates an EXNREF value type.
   *
   * <p>EXNREF is the type for exception references (exception handling proposal).
   *
   * @return an EXNREF ValType
   */
  static ValType exnref() {
    return SimpleValType.Factory.exnref();
  }

  /**
   * Creates a NULLEXNREF value type.
   *
   * <p>NULLEXNREF is the null exception reference type - bottom of the exn hierarchy.
   *
   * @return a NULLEXNREF ValType
   */
  static ValType nullexnref() {
    return SimpleValType.Factory.nullexnref();
  }

  /**
   * Creates a CONTREF value type.
   *
   * <p>CONTREF is the continuation reference type (stack switching proposal).
   *
   * @return a CONTREF ValType
   */
  static ValType contref() {
    return SimpleValType.Factory.contref();
  }

  /**
   * Creates a NULLCONTREF value type.
   *
   * <p>NULLCONTREF is the null continuation reference type - bottom of the cont hierarchy.
   *
   * @return a NULLCONTREF ValType
   */
  static ValType nullcontref() {
    return SimpleValType.Factory.nullcontref();
  }

  /**
   * Returns this ValType as a RefType, if it represents a reference type.
   *
   * @return an Optional containing the RefType if this is a reference type, empty otherwise
   */
  default java.util.Optional<RefType> asRef() {
    if (!isReference()) {
      return java.util.Optional.empty();
    }
    final WasmValueType vt = getValueType();
    switch (vt) {
      case FUNCREF:
        return java.util.Optional.of(RefType.FUNCREF);
      case EXTERNREF:
        return java.util.Optional.of(RefType.EXTERNREF);
      case ANYREF:
        return java.util.Optional.of(RefType.ANYREF);
      case EQREF:
        return java.util.Optional.of(RefType.EQREF);
      case STRUCTREF:
        return java.util.Optional.of(RefType.STRUCTREF);
      case ARRAYREF:
        return java.util.Optional.of(RefType.ARRAYREF);
      case I31REF:
        return java.util.Optional.of(RefType.I31REF);
      case EXNREF:
        return java.util.Optional.of(RefType.EXNREF);
      case NULLREF:
        return java.util.Optional.of(RefType.NULLREF);
      case NULLFUNCREF:
        return java.util.Optional.of(RefType.NULLFUNCREF);
      case NULLEXTERNREF:
        return java.util.Optional.of(RefType.NULLEXTERNREF);
      case NULLEXNREF:
        return java.util.Optional.of(RefType.NULLEXNREF);
      case CONTREF:
        return java.util.Optional.of(RefType.CONTREF);
      case NULLCONTREF:
        return java.util.Optional.of(RefType.NULLCONTREF);
      default:
        return java.util.Optional.empty();
    }
  }

  /**
   * Returns this ValType as a RefType, throwing if it is not a reference type.
   *
   * @return the RefType
   * @throws IllegalStateException if this is not a reference type
   */
  default RefType unwrapRef() {
    return asRef()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Not a reference type: " + getValueType()));
  }

  /**
   * Returns the default value for this value type.
   *
   * <p>Default values are: 0 for integers, 0.0 for floats, zero bytes for v128, and null for
   * reference types.
   *
   * @return the default value as an Object (Integer, Long, Float, Double, byte[], or null)
   */
  default Object defaultValue() {
    switch (getValueType()) {
      case I32:
        return Integer.valueOf(0);
      case I64:
        return Long.valueOf(0L);
      case F32:
        return Float.valueOf(0.0f);
      case F64:
        return Double.valueOf(0.0);
      case V128:
        return new byte[16];
      default:
        // All reference types default to null
        return null;
    }
  }
}

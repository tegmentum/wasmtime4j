/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
  I32(4, true, false, false, false, false),

  /** 64-bit integer type. */
  I64(8, true, false, false, false, false),

  /** 32-bit floating-point type. */
  F32(4, false, true, false, false, false),

  /** 64-bit floating-point type. */
  F64(8, false, true, false, false, false),

  /** 128-bit vector type (SIMD). */
  V128(16, false, false, false, false, false),

  /** Reference to a function. */
  FUNCREF(-1, false, false, true, false, false),

  /** Reference to external data. */
  EXTERNREF(-1, false, false, true, false, false),

  // WasmGC reference types

  /** Top type in the GC reference hierarchy - all GC references are subtypes of anyref. */
  ANYREF(-1, false, false, true, true, false),

  /** Equality-testable references - subset of anyref that supports ref.eq. */
  EQREF(-1, false, false, true, true, false),

  /** Immediate 31-bit integer references for efficient small integer storage. */
  I31REF(-1, false, false, true, true, false),

  /** References to struct instances with typed field access. */
  STRUCTREF(-1, false, false, true, true, false),

  /** References to array instances with element type information. */
  ARRAYREF(-1, false, false, true, true, false),

  /** The null reference type - bottom type for nullable references. */
  NULLREF(-1, false, false, true, true, true),

  /** Nullable function reference type. */
  NULLFUNCREF(-1, false, false, true, true, true),

  /** Nullable external reference type. */
  NULLEXTERNREF(-1, false, false, true, true, true),

  /** Exception reference type (exception handling proposal). */
  EXNREF(-1, false, false, true, true, false),

  /** Null exception reference type - bottom of the exn hierarchy. */
  NULLEXNREF(-1, false, false, true, true, true),

  /** Continuation reference type (stack switching proposal). */
  CONTREF(-1, false, false, true, true, false),

  /** Null continuation reference type - bottom of the cont hierarchy. */
  NULLCONTREF(-1, false, false, true, true, true);

  /** Cached values array for O(1) type code lookup. */
  private static final WasmValueType[] VALUES = values();

  private final int size;
  private final boolean isInteger;
  private final boolean isFloat;
  private final boolean reference;
  private final boolean gcReference;
  private final boolean nullableReference;

  WasmValueType(
      final int size,
      final boolean isInteger,
      final boolean isFloat,
      final boolean reference,
      final boolean gcReference,
      final boolean nullableReference) {
    this.size = size;
    this.isInteger = isInteger;
    this.isFloat = isFloat;
    this.reference = reference;
    this.gcReference = gcReference;
    this.nullableReference = nullableReference;
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
    return reference;
  }

  /**
   * Checks if this is a GC reference type.
   *
   * @return true if this is a WasmGC reference type
   */
  public boolean isGcReference() {
    return gcReference;
  }

  /**
   * Checks if this is a nullable reference type.
   *
   * @return true if this is a nullable reference type
   */
  public boolean isNullableReference() {
    return nullableReference;
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
    if (typeCode < 0 || typeCode >= VALUES.length) {
      throw new IllegalArgumentException("Unknown type code: " + typeCode);
    }
    return VALUES[typeCode];
  }

  /**
   * Converts this WasmValueType to a native type code.
   *
   * @return the native type code for Wasmtime
   */
  public int toNativeTypeCode() {
    return ordinal();
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

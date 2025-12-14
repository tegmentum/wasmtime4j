package ai.tegmentum.wasmtime4j.gc;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Locale;

/**
 * WebAssembly GC field type representation.
 *
 * <p>Represents the type of a field in a struct or element in an array, supporting all WebAssembly
 * value types including packed storage types and reference types.
 *
 * @since 1.0.0
 */
public final class FieldType {
  private final ValueTypeKind kind;
  private final GcReferenceType referenceType;
  private final boolean nullable;

  /** Enumeration of field type kinds. */
  public enum ValueTypeKind {
    /** 32-bit integer. */
    I32,
    /** 64-bit integer. */
    I64,
    /** 32-bit float. */
    F32,
    /** 64-bit float. */
    F64,
    /** 128-bit SIMD vector. */
    V128,
    /** 8-bit packed integer (storage type). */
    PACKED_I8,
    /** 16-bit packed integer (storage type). */
    PACKED_I16,
    /** Reference type. */
    REFERENCE
  }

  private FieldType(
      final ValueTypeKind kind, final GcReferenceType referenceType, final boolean nullable) {
    this.kind = kind;
    this.referenceType = referenceType;
    this.nullable = nullable;
  }

  /**
   * Create a 32-bit integer field type.
   *
   * @return I32 field type
   */
  public static FieldType i32() {
    return new FieldType(ValueTypeKind.I32, null, false);
  }

  /**
   * Create a 64-bit integer field type.
   *
   * @return I64 field type
   */
  public static FieldType i64() {
    return new FieldType(ValueTypeKind.I64, null, false);
  }

  /**
   * Create a 32-bit float field type.
   *
   * @return F32 field type
   */
  public static FieldType f32() {
    return new FieldType(ValueTypeKind.F32, null, false);
  }

  /**
   * Create a 64-bit float field type.
   *
   * @return F64 field type
   */
  public static FieldType f64() {
    return new FieldType(ValueTypeKind.F64, null, false);
  }

  /**
   * Create a 128-bit SIMD vector field type.
   *
   * @return V128 field type
   */
  public static FieldType v128() {
    return new FieldType(ValueTypeKind.V128, null, false);
  }

  /**
   * Create a packed 8-bit integer field type.
   *
   * @return packed I8 field type
   */
  public static FieldType packedI8() {
    return new FieldType(ValueTypeKind.PACKED_I8, null, false);
  }

  /**
   * Create a packed 16-bit integer field type.
   *
   * @return packed I16 field type
   */
  public static FieldType packedI16() {
    return new FieldType(ValueTypeKind.PACKED_I16, null, false);
  }

  /**
   * Create a reference field type.
   *
   * @param referenceType the reference type
   * @param nullable whether the reference can be null
   * @return reference field type
   */
  public static FieldType reference(final GcReferenceType referenceType, final boolean nullable) {
    if (referenceType == null) {
      throw new IllegalArgumentException("Reference type cannot be null");
    }
    return new FieldType(ValueTypeKind.REFERENCE, referenceType, nullable);
  }

  /**
   * Create a non-nullable reference field type.
   *
   * @param referenceType the reference type
   * @return non-nullable reference field type
   */
  public static FieldType reference(final GcReferenceType referenceType) {
    return reference(referenceType, false);
  }

  /**
   * Create a nullable anyref field type.
   *
   * @return nullable anyref field type
   */
  public static FieldType anyRef() {
    return reference(GcReferenceType.ANY_REF, true);
  }

  /**
   * Create a nullable eqref field type.
   *
   * @return nullable eqref field type
   */
  public static FieldType eqRef() {
    return reference(GcReferenceType.EQ_REF, true);
  }

  /**
   * Create a nullable i31ref field type.
   *
   * @return nullable i31ref field type
   */
  public static FieldType i31Ref() {
    return reference(GcReferenceType.I31_REF, true);
  }

  /**
   * Create a nullable structref field type.
   *
   * @return nullable structref field type
   */
  public static FieldType structRef() {
    return reference(GcReferenceType.STRUCT_REF, true);
  }

  /**
   * Create a nullable arrayref field type.
   *
   * @return nullable arrayref field type
   */
  public static FieldType arrayRef() {
    return reference(GcReferenceType.ARRAY_REF, true);
  }

  /**
   * Gets the value type kind.
   *
   * @return the value type kind
   */
  public ValueTypeKind getKind() {
    return kind;
  }

  /**
   * Gets the reference type (only valid for REFERENCE kind).
   *
   * @return the reference type, or null if not a reference type
   */
  public GcReferenceType getReferenceType() {
    return referenceType;
  }

  /**
   * Checks if this is a reference type.
   *
   * @return true if this is a reference type
   */
  public boolean isReference() {
    return kind == ValueTypeKind.REFERENCE;
  }

  /**
   * Checks if this reference type is nullable (only valid for reference types).
   *
   * @return true if this reference type is nullable
   */
  public boolean isNullable() {
    return nullable;
  }

  /**
   * Checks if this is a packed storage type.
   *
   * @return true if this is a packed storage type
   */
  public boolean isPacked() {
    return kind == ValueTypeKind.PACKED_I8 || kind == ValueTypeKind.PACKED_I16;
  }

  /**
   * Gets the size in bytes for this field type.
   *
   * @return the size in bytes
   */
  public int getSizeBytes() {
    switch (kind) {
      case I32:
      case F32:
        return 4;
      case I64:
      case F64:
        return 8;
      case V128:
        return 16;
      case PACKED_I8:
        return 1;
      case PACKED_I16:
        return 2;
      case REFERENCE:
        return 8; // Pointer size on 64-bit systems
      default:
        throw new IllegalStateException("Unknown field type: " + kind);
    }
  }

  /**
   * Converts this field type to a WasmValueType for basic types.
   *
   * @return the corresponding WasmValueType
   * @throws IllegalStateException if this is a packed or GC-specific type
   */
  public WasmValueType toWasmValueType() {
    switch (kind) {
      case I32:
        return WasmValueType.I32;
      case I64:
        return WasmValueType.I64;
      case F32:
        return WasmValueType.F32;
      case F64:
        return WasmValueType.F64;
      case V128:
        return WasmValueType.V128;
      case PACKED_I8:
      case PACKED_I16:
        return WasmValueType.I32; // Packed types are stored as I32
      case REFERENCE:
        // Would need specific handling for reference types
        throw new IllegalStateException(
            "Reference types cannot be converted to basic WasmValueType");
      default:
        throw new IllegalStateException("Unknown field type: " + kind);
    }
  }

  /**
   * Checks if this field type is compatible with another for assignment.
   *
   * @param other the other field type
   * @return true if assignment is valid
   */
  public boolean isCompatibleWith(final FieldType other) {
    if (this.kind != other.kind) {
      return false;
    }

    if (isReference()) {
      // Reference type compatibility includes subtyping and nullability
      return this.referenceType.isSubtypeOf(other.referenceType)
          && (!this.nullable || other.nullable);
    }

    return true;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final FieldType fieldType = (FieldType) obj;
    return kind == fieldType.kind
        && referenceType == fieldType.referenceType
        && nullable == fieldType.nullable;
  }

  @Override
  public int hashCode() {
    int result = kind.hashCode();
    result = 31 * result + (referenceType != null ? referenceType.hashCode() : 0);
    result = 31 * result + (nullable ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    if (isReference()) {
      return referenceType.getWasmName() + (nullable ? "?" : "");
    }
    return kind.name().toLowerCase(Locale.ROOT);
  }
}

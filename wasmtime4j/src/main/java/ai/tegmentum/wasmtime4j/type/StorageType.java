package ai.tegmentum.wasmtime4j.type;

import java.util.Objects;

/**
 * Represents a WebAssembly GC storage type.
 *
 * <p>Storage types extend value types with packed integer formats (I8, I16) used for compact field
 * storage in GC struct and array types. A storage type is either a packed integer type or a
 * full-width value type.
 *
 * <p>In the WebAssembly GC proposal, struct fields and array elements may use packed storage types
 * to reduce memory footprint. When read, packed values are sign-extended or zero-extended to i32.
 *
 * @since 1.1.0
 */
public final class StorageType {

  /** Enumeration of storage type kinds. */
  public enum Kind {
    /** 8-bit packed integer storage. */
    I8,
    /** 16-bit packed integer storage. */
    I16,
    /** Full-width value type storage. */
    VAL
  }

  private static final StorageType INSTANCE_I8 = new StorageType(Kind.I8, null);
  private static final StorageType INSTANCE_I16 = new StorageType(Kind.I16, null);

  private final Kind kind;
  private final ValType valType;

  private StorageType(final Kind kind, final ValType valType) {
    this.kind = kind;
    this.valType = valType;
  }

  /**
   * Creates a packed I8 storage type.
   *
   * @return the I8 storage type
   */
  public static StorageType i8() {
    return INSTANCE_I8;
  }

  /**
   * Creates a packed I16 storage type.
   *
   * @return the I16 storage type
   */
  public static StorageType i16() {
    return INSTANCE_I16;
  }

  /**
   * Creates a value storage type wrapping a ValType.
   *
   * @param valType the value type to wrap
   * @return a VAL storage type
   * @throws IllegalArgumentException if valType is null
   */
  public static StorageType val(final ValType valType) {
    if (valType == null) {
      throw new IllegalArgumentException("valType cannot be null");
    }
    return new StorageType(Kind.VAL, valType);
  }

  /**
   * Gets the kind of this storage type.
   *
   * @return the storage type kind
   */
  public Kind getKind() {
    return kind;
  }

  /**
   * Checks if this is a packed I8 storage type.
   *
   * @return true if this is I8
   */
  public boolean isI8() {
    return kind == Kind.I8;
  }

  /**
   * Checks if this is a packed I16 storage type.
   *
   * @return true if this is I16
   */
  public boolean isI16() {
    return kind == Kind.I16;
  }

  /**
   * Checks if this is a full-width value type storage.
   *
   * @return true if this is a VAL kind
   */
  public boolean isValType() {
    return kind == Kind.VAL;
  }

  /**
   * Gets the wrapped ValType for VAL kind storage types.
   *
   * @return the value type
   * @throws IllegalStateException if this is not a VAL kind
   */
  public ValType asValType() {
    if (kind != Kind.VAL) {
      throw new IllegalStateException("StorageType is " + kind + ", not VAL");
    }
    return valType;
  }

  /**
   * Unpacks this storage type to a ValType.
   *
   * <p>For packed types (I8, I16), returns {@link ValType#i32()} since packed values are
   * sign-extended or zero-extended to i32 when read. For VAL types, returns the wrapped ValType.
   *
   * @return the unpacked value type
   */
  public ValType unpack() {
    switch (kind) {
      case I8:
      case I16:
        return ValType.i32();
      case VAL:
        return valType;
      default:
        throw new IllegalStateException("Unknown storage type kind: " + kind);
    }
  }

  /**
   * Checks if this storage type matches another for type compatibility.
   *
   * <p>Two packed types (I8, I16) match if they are the same kind. For VAL storage types, matching
   * delegates to {@link ValType#matches(ValType)}.
   *
   * @param other the storage type to check against
   * @return true if this storage type matches the other
   */
  public boolean matches(final StorageType other) {
    if (other == null) {
      return false;
    }
    if (this.kind != other.kind) {
      return false;
    }
    if (this.kind == Kind.VAL) {
      return this.valType.matches(other.valType);
    }
    // I8 matches I8, I16 matches I16
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
    final StorageType that = (StorageType) obj;
    return kind == that.kind && Objects.equals(valType, that.valType);
  }

  @Override
  public int hashCode() {
    int result = kind.hashCode();
    result = 31 * result + (valType != null ? valType.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    switch (kind) {
      case I8:
        return "i8";
      case I16:
        return "i16";
      case VAL:
        return valType.toString();
      default:
        return kind.name();
    }
  }
}

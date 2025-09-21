package ai.tegmentum.wasmtime4j;

/**
 * Represents a WebAssembly reference type.
 *
 * <p>Reference types are a key feature of WebAssembly that enable more sophisticated memory
 * management and interoperability patterns. This class encapsulates the different kinds of
 * reference types including function references, external references, and any references.
 *
 * @since 1.0.0
 */
public final class RefType {

  /** The heap type for this reference type. */
  private final HeapType heapType;

  /** Whether this reference type is nullable. */
  private final boolean nullable;

  /**
   * Creates a new reference type.
   *
   * @param heapType the heap type for this reference
   * @param nullable whether this reference type can be null
   * @throws IllegalArgumentException if heapType is null
   */
  public RefType(final HeapType heapType, final boolean nullable) {
    if (heapType == null) {
      throw new IllegalArgumentException("HeapType cannot be null");
    }
    this.heapType = heapType;
    this.nullable = nullable;
  }

  /**
   * Gets the heap type for this reference type.
   *
   * @return the heap type
   */
  public HeapType getHeapType() {
    return heapType;
  }

  /**
   * Checks if this reference type is nullable.
   *
   * @return true if this reference type can be null, false otherwise
   */
  public boolean isNullable() {
    return nullable;
  }

  /**
   * Creates a function reference type.
   *
   * @param nullable whether the reference can be null
   * @return a new function reference type
   */
  public static RefType funcRef(final boolean nullable) {
    return new RefType(HeapType.FUNC, nullable);
  }

  /**
   * Creates an external reference type.
   *
   * @param nullable whether the reference can be null
   * @return a new external reference type
   */
  public static RefType externRef(final boolean nullable) {
    return new RefType(HeapType.EXTERN, nullable);
  }

  /**
   * Creates an any reference type.
   *
   * @param nullable whether the reference can be null
   * @return a new any reference type
   */
  public static RefType anyRef(final boolean nullable) {
    return new RefType(HeapType.ANY, nullable);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final RefType refType = (RefType) obj;
    return nullable == refType.nullable && heapType == refType.heapType;
  }

  @Override
  public int hashCode() {
    int result = heapType.hashCode();
    result = 31 * result + (nullable ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "RefType{" + "heapType=" + heapType + ", nullable=" + nullable + '}';
  }

  /** Enum representing the different heap types for reference types. */
  public enum HeapType {
    /** Function reference heap type. */
    FUNC,
    /** External reference heap type. */
    EXTERN,
    /** Any reference heap type. */
    ANY,
    /** None heap type (bottom type). */
    NONE,
    /** No external reference heap type. */
    NO_EXTERN,
    /** No function reference heap type. */
    NO_FUNC
  }
}

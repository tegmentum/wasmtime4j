package ai.tegmentum.wasmtime4j.type;

/**
 * Represents a WebAssembly reference type with nullability and heap type information.
 *
 * <p>Reference types in WebAssembly describe the kinds of references that can be stored in tables,
 * passed as function parameters, or returned from functions. Each reference type has a heap type
 * (describing what the reference points to) and a nullability flag.
 *
 * <p>Common predefined reference types are available as constants:
 *
 * <ul>
 *   <li>{@link #FUNCREF} - nullable function reference
 *   <li>{@link #EXTERNREF} - nullable external reference
 *   <li>{@link #ANYREF} - nullable any reference (GC proposal)
 *   <li>{@link #EQREF} - nullable equality-testable reference (GC proposal)
 *   <li>{@link #STRUCTREF} - nullable struct reference (GC proposal)
 *   <li>{@link #ARRAYREF} - nullable array reference (GC proposal)
 *   <li>{@link #I31REF} - nullable i31 reference (GC proposal)
 *   <li>{@link #EXNREF} - nullable exception reference (exception handling proposal)
 * </ul>
 *
 * @since 1.1.0
 */
public interface RefType {

  /** Nullable function reference type. */
  RefType FUNCREF = DefaultRefType.create(true, HeapType.FUNC);

  /** Nullable external reference type. */
  RefType EXTERNREF = DefaultRefType.create(true, HeapType.EXTERN);

  /** Nullable any reference type (GC top type). */
  RefType ANYREF = DefaultRefType.create(true, HeapType.ANY);

  /** Nullable equality-testable reference type. */
  RefType EQREF = DefaultRefType.create(true, HeapType.EQ);

  /** Nullable struct reference type. */
  RefType STRUCTREF = DefaultRefType.create(true, HeapType.STRUCT);

  /** Nullable array reference type. */
  RefType ARRAYREF = DefaultRefType.create(true, HeapType.ARRAY);

  /** Nullable i31 reference type. */
  RefType I31REF = DefaultRefType.create(true, HeapType.I31);

  /** Nullable exception reference type (exception handling proposal). */
  RefType EXNREF = DefaultRefType.create(true, HeapType.NONE);

  /**
   * Checks if this reference type is nullable.
   *
   * @return true if references of this type can be null
   */
  boolean isNullable();

  /**
   * Gets the heap type that this reference type points to.
   *
   * @return the heap type
   */
  HeapType getHeapType();

  /**
   * Creates a reference type with the specified nullability and heap type.
   *
   * @param nullable whether the reference can be null
   * @param heapType the heap type this reference points to
   * @return a new RefType
   * @throws IllegalArgumentException if heapType is null
   */
  static RefType of(final boolean nullable, final HeapType heapType) {
    return DefaultRefType.create(nullable, heapType);
  }

  /**
   * Creates a non-nullable reference type for the specified heap type.
   *
   * @param heapType the heap type this reference points to
   * @return a new non-nullable RefType
   * @throws IllegalArgumentException if heapType is null
   */
  static RefType nonNull(final HeapType heapType) {
    return DefaultRefType.create(false, heapType);
  }
}

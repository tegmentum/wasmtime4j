package ai.tegmentum.wasmtime4j.gc;

import java.util.List;

/**
 * A pre-compiled array allocator that caches type resolution for efficient repeated allocation.
 *
 * <p>In Wasmtime, {@code ArrayRefPre} resolves the array type once and reuses the resolved
 * representation for subsequent allocations, avoiding redundant type lookups. This is analogous to
 * preparing a statement in a database — the upfront cost is amortized across many uses.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ArrayType type = ArrayType.builder("IntArray")
 *     .elementType(FieldType.i32())
 *     .mutable(true)
 *     .build();
 * ArrayRefPre pre = ArrayRefPre.create(type);
 *
 * // Efficiently allocate many arrays with the same type
 * for (int i = 0; i < 1000; i++) {
 *     ArrayRef ref = pre.allocate(gcRuntime, List.of(
 *         GcValue.i32(i), GcValue.i32(i * 2)
 *     ));
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public final class ArrayRefPre implements AutoCloseable {

  private final ArrayType arrayType;
  private volatile boolean closed;

  private ArrayRefPre(final ArrayType arrayType) {
    this.arrayType = arrayType;
  }

  /**
   * Creates a new ArrayRefPre for the given array type.
   *
   * @param arrayType the array type to pre-compile
   * @return a new ArrayRefPre
   * @throws IllegalArgumentException if arrayType is null
   */
  public static ArrayRefPre create(final ArrayType arrayType) {
    if (arrayType == null) {
      throw new IllegalArgumentException("arrayType cannot be null");
    }
    return new ArrayRefPre(arrayType);
  }

  /**
   * Gets the array type this allocator was created for.
   *
   * @return the array type
   */
  public ArrayType getArrayType() {
    return arrayType;
  }

  /**
   * Allocates a new array instance using this pre-compiled allocator.
   *
   * @param gcRuntime the GC runtime to allocate in
   * @param elements the initial element values
   * @return a new ArrayRef
   * @throws GcException if allocation fails
   * @throws IllegalStateException if this allocator has been closed
   * @throws IllegalArgumentException if gcRuntime or elements is null
   */
  public ArrayRef allocate(final GcRuntime gcRuntime, final List<GcValue> elements)
      throws GcException {
    if (closed) {
      throw new IllegalStateException("ArrayRefPre has been closed");
    }
    if (gcRuntime == null) {
      throw new IllegalArgumentException("gcRuntime cannot be null");
    }
    if (elements == null) {
      throw new IllegalArgumentException("elements cannot be null");
    }
    final ArrayInstance instance = gcRuntime.createArray(arrayType, elements);
    return ArrayRef.of(instance);
  }

  /**
   * Allocates a new array instance with default element values.
   *
   * @param gcRuntime the GC runtime to allocate in
   * @param length the array length
   * @return a new ArrayRef with default values
   * @throws GcException if allocation fails
   * @throws IllegalStateException if this allocator has been closed
   * @throws IllegalArgumentException if gcRuntime is null or length is negative
   */
  public ArrayRef allocateDefault(final GcRuntime gcRuntime, final int length) throws GcException {
    if (closed) {
      throw new IllegalStateException("ArrayRefPre has been closed");
    }
    if (gcRuntime == null) {
      throw new IllegalArgumentException("gcRuntime cannot be null");
    }
    if (length < 0) {
      throw new IllegalArgumentException("length cannot be negative: " + length);
    }
    final ArrayInstance instance = gcRuntime.createArray(arrayType, length);
    return ArrayRef.of(instance);
  }

  @Override
  public void close() {
    closed = true;
  }
}

package ai.tegmentum.wasmtime4j.gc;

import java.util.List;

/**
 * A pre-compiled struct allocator that caches type resolution for efficient repeated allocation.
 *
 * <p>In Wasmtime, {@code StructRefPre} resolves the struct type once and reuses the resolved
 * representation for subsequent allocations, avoiding redundant type lookups. This is analogous to
 * preparing a statement in a database — the upfront cost is amortized across many uses.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * StructType type = StructType.builder("Point")
 *     .addField("x", FieldType.f64(), true)
 *     .addField("y", FieldType.f64(), true)
 *     .build();
 * StructRefPre pre = StructRefPre.create(type);
 *
 * // Efficiently allocate many structs with the same type
 * for (int i = 0; i < 1000; i++) {
 *     StructRef ref = pre.allocate(gcRuntime, List.of(
 *         GcValue.f64(i), GcValue.f64(i * 2)
 *     ));
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public final class StructRefPre implements AutoCloseable {

  private final StructType structType;
  private volatile boolean closed;

  private StructRefPre(final StructType structType) {
    this.structType = structType;
  }

  /**
   * Creates a new StructRefPre for the given struct type.
   *
   * @param structType the struct type to pre-compile
   * @return a new StructRefPre
   * @throws IllegalArgumentException if structType is null
   */
  public static StructRefPre create(final StructType structType) {
    if (structType == null) {
      throw new IllegalArgumentException("structType cannot be null");
    }
    return new StructRefPre(structType);
  }

  /**
   * Gets the struct type this allocator was created for.
   *
   * @return the struct type
   */
  public StructType getStructType() {
    return structType;
  }

  /**
   * Allocates a new struct instance using this pre-compiled allocator.
   *
   * @param gcRuntime the GC runtime to allocate in
   * @param fieldValues the initial field values
   * @return a new StructRef
   * @throws GcException if allocation fails
   * @throws IllegalStateException if this allocator has been closed
   * @throws IllegalArgumentException if gcRuntime or fieldValues is null
   */
  public StructRef allocate(final GcRuntime gcRuntime, final List<GcValue> fieldValues)
      throws GcException {
    if (closed) {
      throw new IllegalStateException("StructRefPre has been closed");
    }
    if (gcRuntime == null) {
      throw new IllegalArgumentException("gcRuntime cannot be null");
    }
    if (fieldValues == null) {
      throw new IllegalArgumentException("fieldValues cannot be null");
    }
    final StructInstance instance = gcRuntime.createStruct(structType, fieldValues);
    return StructRef.of(instance);
  }

  /**
   * Allocates a new struct instance with default field values.
   *
   * @param gcRuntime the GC runtime to allocate in
   * @return a new StructRef with default values
   * @throws GcException if allocation fails
   * @throws IllegalStateException if this allocator has been closed
   * @throws IllegalArgumentException if gcRuntime is null
   */
  public StructRef allocateDefault(final GcRuntime gcRuntime) throws GcException {
    if (closed) {
      throw new IllegalStateException("StructRefPre has been closed");
    }
    if (gcRuntime == null) {
      throw new IllegalArgumentException("gcRuntime cannot be null");
    }
    final StructInstance instance = gcRuntime.createStruct(structType);
    return StructRef.of(instance);
  }

  @Override
  public void close() {
    closed = true;
  }
}

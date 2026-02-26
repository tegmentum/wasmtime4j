package ai.tegmentum.wasmtime4j.gc;

/**
 * A pre-compiled exception allocator that caches type resolution for efficient repeated allocation.
 *
 * <p>In Wasmtime, {@code ExnRefPre} resolves the exception type once and reuses the resolved
 * representation for subsequent allocations, avoiding redundant type lookups. This is analogous to
 * preparing a statement in a database — the upfront cost is amortized across many uses.
 *
 * <p>ExnRefPre is associated with an {@link ExnType} which describes the exception payload fields.
 * Exception references created through this allocator will carry values matching the tag's type
 * signature.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ExnType type = new ExnType(tagType);
 * ExnRefPre pre = ExnRefPre.create(type);
 *
 * // The allocator can be used to efficiently create multiple exception references
 * // with the same type when allocating through the GC runtime.
 * }</pre>
 *
 * @since 1.1.0
 */
public final class ExnRefPre implements AutoCloseable {

  private final ExnType exnType;
  private volatile boolean closed;

  private ExnRefPre(final ExnType exnType) {
    this.exnType = exnType;
  }

  /**
   * Creates a new ExnRefPre for the given exception type.
   *
   * @param exnType the exception type to pre-compile
   * @return a new ExnRefPre
   * @throws IllegalArgumentException if exnType is null
   */
  public static ExnRefPre create(final ExnType exnType) {
    if (exnType == null) {
      throw new IllegalArgumentException("exnType cannot be null");
    }
    return new ExnRefPre(exnType);
  }

  /**
   * Gets the exception type this allocator was created for.
   *
   * @return the exception type
   */
  public ExnType getExnType() {
    return exnType;
  }

  /**
   * Returns whether this allocator is still active (not yet closed).
   *
   * @return true if the allocator can still be used
   */
  public boolean isActive() {
    return !closed;
  }

  @Override
  public void close() {
    closed = true;
  }
}

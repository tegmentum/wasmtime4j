package ai.tegmentum.wasmtime4j.type;

/**
 * Builder for creating {@link MemoryType} instances.
 *
 * <p>This builder provides a fluent API for constructing memory type descriptors with various
 * properties including minimum/maximum page counts, 64-bit addressing, shared memory, and custom
 * page sizes.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * MemoryType memType = MemoryType.builder()
 *     .min(1)
 *     .max(10)
 *     .shared(true)
 *     .build();
 * }</pre>
 *
 * @since 1.1.0
 */
public final class MemoryTypeBuilder {

  private long min;
  private Long max;
  private boolean is64;
  private boolean shared;
  private long pageSize = 65536L;

  /** Creates a new MemoryTypeBuilder with default values. */
  public MemoryTypeBuilder() {
    // Default constructor
  }

  /**
   * Sets the minimum number of memory pages.
   *
   * @param min the minimum page count (must be non-negative)
   * @return this builder
   * @throws IllegalArgumentException if min is negative
   */
  public MemoryTypeBuilder min(final long min) {
    if (min < 0) {
      throw new IllegalArgumentException("min cannot be negative");
    }
    this.min = min;
    return this;
  }

  /**
   * Sets the maximum number of memory pages.
   *
   * @param max the maximum page count (must be non-negative)
   * @return this builder
   * @throws IllegalArgumentException if max is negative
   */
  public MemoryTypeBuilder max(final long max) {
    if (max < 0) {
      throw new IllegalArgumentException("max cannot be negative");
    }
    this.max = max;
    return this;
  }

  /**
   * Sets whether this memory uses 64-bit addressing.
   *
   * @param is64 true for 64-bit, false for 32-bit
   * @return this builder
   */
  public MemoryTypeBuilder is64(final boolean is64) {
    this.is64 = is64;
    return this;
  }

  /**
   * Sets whether this memory is shared between threads.
   *
   * @param shared true for shared memory, false for private
   * @return this builder
   */
  public MemoryTypeBuilder shared(final boolean shared) {
    this.shared = shared;
    return this;
  }

  /**
   * Sets the page size for this memory in bytes.
   *
   * <p>The standard WebAssembly page size is 65536 (64KB). The custom page sizes proposal allows
   * smaller page sizes like 1 byte.
   *
   * @param pageSize the page size in bytes (must be a power of 2)
   * @return this builder
   * @throws IllegalArgumentException if pageSize is not a positive power of 2
   */
  public MemoryTypeBuilder pageSize(final long pageSize) {
    if (pageSize <= 0 || (pageSize & (pageSize - 1)) != 0) {
      throw new IllegalArgumentException("pageSize must be a positive power of 2");
    }
    this.pageSize = pageSize;
    return this;
  }

  /**
   * Builds the MemoryType from the configured values.
   *
   * @return a new MemoryType
   * @throws IllegalStateException if max is set and less than min
   */
  public MemoryType build() {
    if (max != null && max < min) {
      throw new IllegalStateException("max (" + max + ") cannot be less than min (" + min + ")");
    }
    return new DefaultMemoryType(min, max, is64, shared, pageSize);
  }
}

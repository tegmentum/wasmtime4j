package ai.tegmentum.wasmtime4j;

/**
 * Configuration for resource limits on WebAssembly stores.
 *
 * <p>StoreLimits allows configuring resource constraints for WebAssembly execution, including
 * memory size, table element counts, and instance counts. These limits are applied when creating a
 * store and cannot be changed dynamically.
 *
 * <p>Limits are per-resource, meaning each memory, table, or instance can grow up to the specified
 * limit independently.
 *
 * <p>Use the builder pattern to create instances:
 *
 * <pre>{@code
 * StoreLimits limits = StoreLimits.builder()
 *     .memorySize(1024 * 1024 * 10)  // 10 MB
 *     .tableElements(1000)
 *     .instances(5)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class StoreLimits {

  private final long memorySize;
  private final long tableElements;
  private final long instances;

  private StoreLimits(final Builder builder) {
    this.memorySize = builder.memorySize;
    this.tableElements = builder.tableElements;
    this.instances = builder.instances;
  }

  /**
   * Gets the memory size limit in bytes.
   *
   * @return the memory size limit, or 0 for unlimited
   */
  public long getMemorySize() {
    return memorySize;
  }

  /**
   * Gets the table element limit.
   *
   * @return the table element limit, or 0 for unlimited
   */
  public long getTableElements() {
    return tableElements;
  }

  /**
   * Gets the instance limit.
   *
   * @return the instance limit, or 0 for unlimited
   */
  public long getInstances() {
    return instances;
  }

  /**
   * Creates a new builder for StoreLimits.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating StoreLimits instances.
   *
   * @since 1.0.0
   */
  public static final class Builder {
    private long memorySize = 0;
    private long tableElements = 0;
    private long instances = 0;

    private Builder() {}

    /**
     * Sets the memory size limit in bytes.
     *
     * <p>This limit applies to each individual memory instance. A value of 0 means unlimited.
     *
     * @param bytes the memory size limit in bytes
     * @return this builder
     * @throws IllegalArgumentException if bytes is negative
     */
    public Builder memorySize(final long bytes) {
      if (bytes < 0) {
        throw new IllegalArgumentException("Memory size cannot be negative");
      }
      this.memorySize = bytes;
      return this;
    }

    /**
     * Sets the table element limit.
     *
     * <p>This limit applies to each individual table. A value of 0 means unlimited.
     *
     * @param elements the table element limit
     * @return this builder
     * @throws IllegalArgumentException if elements is negative
     */
    public Builder tableElements(final long elements) {
      if (elements < 0) {
        throw new IllegalArgumentException("Table elements cannot be negative");
      }
      this.tableElements = elements;
      return this;
    }

    /**
     * Sets the instance limit.
     *
     * <p>This limit restricts the total number of instances that can be created. A value of 0
     * means unlimited.
     *
     * @param count the instance limit
     * @return this builder
     * @throws IllegalArgumentException if count is negative
     */
    public Builder instances(final long count) {
      if (count < 0) {
        throw new IllegalArgumentException("Instance count cannot be negative");
      }
      this.instances = count;
      return this;
    }

    /**
     * Builds the StoreLimits instance.
     *
     * @return a new StoreLimits with the configured limits
     */
    public StoreLimits build() {
      return new StoreLimits(this);
    }
  }
}

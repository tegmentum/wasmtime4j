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
package ai.tegmentum.wasmtime4j.config;

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
  private final long tables;
  private final long memories;
  private final boolean trapOnGrowFailure;

  private StoreLimits(final Builder builder) {
    this.memorySize = builder.memorySize;
    this.tableElements = builder.tableElements;
    this.instances = builder.instances;
    this.tables = builder.tables;
    this.memories = builder.memories;
    this.trapOnGrowFailure = builder.trapOnGrowFailure;
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
   * Gets the maximum number of tables allowed.
   *
   * @return the table count limit, or 0 for unlimited
   */
  public long getTables() {
    return tables;
  }

  /**
   * Gets the maximum number of memories allowed.
   *
   * @return the memory count limit, or 0 for unlimited
   */
  public long getMemories() {
    return memories;
  }

  /**
   * Gets whether memory/table growth failures should trap instead of returning -1.
   *
   * @return true if growth failures should trap
   */
  public boolean isTrapOnGrowFailure() {
    return trapOnGrowFailure;
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
    private long tables = 0;
    private long memories = 0;
    private boolean trapOnGrowFailure = false;

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
     * <p>This limit restricts the total number of instances that can be created. A value of 0 means
     * unlimited.
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
     * Sets the maximum number of tables allowed.
     *
     * <p>This limits the total number of tables that can be created or imported. A value of 0 means
     * unlimited.
     *
     * @param count the maximum table count
     * @return this builder
     * @throws IllegalArgumentException if count is negative
     */
    public Builder tables(final long count) {
      if (count < 0) {
        throw new IllegalArgumentException("Table count cannot be negative");
      }
      this.tables = count;
      return this;
    }

    /**
     * Sets the maximum number of memories allowed.
     *
     * <p>This limits the total number of memories that can be created or imported. A value of 0
     * means unlimited.
     *
     * @param count the maximum memory count
     * @return this builder
     * @throws IllegalArgumentException if count is negative
     */
    public Builder memories(final long count) {
      if (count < 0) {
        throw new IllegalArgumentException("Memory count cannot be negative");
      }
      this.memories = count;
      return this;
    }

    /**
     * Sets whether memory/table growth failures should trap instead of returning -1.
     *
     * <p>When enabled, if a memory.grow or table.grow operation fails due to the configured limits,
     * the WebAssembly execution will trap instead of returning -1 to the WebAssembly code.
     *
     * @param trap true to trap on grow failure
     * @return this builder
     */
    public Builder trapOnGrowFailure(final boolean trap) {
      this.trapOnGrowFailure = trap;
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

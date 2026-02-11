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
 * Fluent builder for creating {@link StoreLimits} instances.
 *
 * <p>This builder provides a convenient way to configure resource limits for WebAssembly stores.
 * All limits default to unlimited (0) and can be individually configured.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * StoreLimits limits = new StoreLimitsBuilder()
 *     .memorySize(10 * 1024 * 1024)  // 10 MB max memory
 *     .tableElements(10000)           // 10k table elements
 *     .instances(10)                  // 10 instances
 *     .tables(5)                      // 5 tables
 *     .memories(2)                    // 2 memories
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class StoreLimitsBuilder {

  private long memorySize = 0;
  private long tableElements = 0;
  private long instances = 0;
  private long tables = 0;
  private long memories = 0;

  /** Creates a new StoreLimitsBuilder with default (unlimited) values. */
  public StoreLimitsBuilder() {
    // Default values are already set to 0 (unlimited)
  }

  /**
   * Sets the maximum size of a linear memory in bytes.
   *
   * <p>This limit applies to each individual memory. A value of 0 means unlimited.
   *
   * @param bytes the maximum memory size in bytes
   * @return this builder for chaining
   * @throws IllegalArgumentException if bytes is negative
   */
  public StoreLimitsBuilder memorySize(final long bytes) {
    if (bytes < 0) {
      throw new IllegalArgumentException("Memory size cannot be negative");
    }
    this.memorySize = bytes;
    return this;
  }

  /**
   * Sets the maximum number of elements in a table.
   *
   * <p>This limit applies to each individual table. A value of 0 means unlimited.
   *
   * @param elements the maximum table element count
   * @return this builder for chaining
   * @throws IllegalArgumentException if elements is negative
   */
  public StoreLimitsBuilder tableElements(final long elements) {
    if (elements < 0) {
      throw new IllegalArgumentException("Table elements cannot be negative");
    }
    this.tableElements = elements;
    return this;
  }

  /**
   * Sets the maximum number of instances.
   *
   * <p>This limits the total number of instances that can be created in the store. A value of 0
   * means unlimited.
   *
   * @param count the maximum instance count
   * @return this builder for chaining
   * @throws IllegalArgumentException if count is negative
   */
  public StoreLimitsBuilder instances(final long count) {
    if (count < 0) {
      throw new IllegalArgumentException("Instance count cannot be negative");
    }
    this.instances = count;
    return this;
  }

  /**
   * Sets the maximum number of tables.
   *
   * <p>This limits the total number of tables that can be created or imported. A value of 0 means
   * unlimited.
   *
   * @param count the maximum table count
   * @return this builder for chaining
   * @throws IllegalArgumentException if count is negative
   */
  public StoreLimitsBuilder tables(final long count) {
    if (count < 0) {
      throw new IllegalArgumentException("Table count cannot be negative");
    }
    this.tables = count;
    return this;
  }

  /**
   * Sets the maximum number of memories.
   *
   * <p>This limits the total number of memories that can be created or imported. A value of 0 means
   * unlimited.
   *
   * @param count the maximum memory count
   * @return this builder for chaining
   * @throws IllegalArgumentException if count is negative
   */
  public StoreLimitsBuilder memories(final long count) {
    if (count < 0) {
      throw new IllegalArgumentException("Memory count cannot be negative");
    }
    this.memories = count;
    return this;
  }

  /**
   * Gets the configured memory size limit.
   *
   * @return the memory size limit in bytes
   */
  public long getMemorySize() {
    return memorySize;
  }

  /**
   * Gets the configured table elements limit.
   *
   * @return the table elements limit
   */
  public long getTableElements() {
    return tableElements;
  }

  /**
   * Gets the configured instances limit.
   *
   * @return the instances limit
   */
  public long getInstances() {
    return instances;
  }

  /**
   * Gets the configured tables limit.
   *
   * @return the tables limit
   */
  public long getTables() {
    return tables;
  }

  /**
   * Gets the configured memories limit.
   *
   * @return the memories limit
   */
  public long getMemories() {
    return memories;
  }

  /**
   * Builds the StoreLimits instance.
   *
   * @return a new StoreLimits with the configured limits
   */
  public StoreLimits build() {
    return StoreLimits.builder()
        .memorySize(memorySize)
        .tableElements(tableElements)
        .instances(instances)
        .build();
  }

  @Override
  public String toString() {
    return "StoreLimitsBuilder{"
        + "memorySize="
        + memorySize
        + ", tableElements="
        + tableElements
        + ", instances="
        + instances
        + ", tables="
        + tables
        + ", memories="
        + memories
        + "}";
  }
}

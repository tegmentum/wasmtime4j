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

package ai.tegmentum.wasmtime4j.execution;

import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Configuration for resource limits on WebAssembly stores.
 *
 * <p>This class specifies the maximum limits for various resources that WebAssembly modules can
 * consume. Each limit is optional - if not set, no limit is enforced for that resource.
 *
 * <p>Use the builder pattern to create instances:
 *
 * <pre>{@code
 * ResourceLimiterConfig config = ResourceLimiterConfig.builder()
 *     .maxMemoryBytes(1024 * 1024 * 10)  // 10 MB total
 *     .maxMemoryPages(160)               // 10 MB in 64KB pages
 *     .maxTableElements(1000)
 *     .maxInstances(5)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class ResourceLimiterConfig {

  private final Long maxMemoryBytes;
  private final Long maxMemoryPages;
  private final Long maxTableElements;
  private final Integer maxInstances;
  private final Integer maxTables;
  private final Integer maxMemories;

  private ResourceLimiterConfig(final Builder builder) {
    this.maxMemoryBytes = builder.maxMemoryBytes;
    this.maxMemoryPages = builder.maxMemoryPages;
    this.maxTableElements = builder.maxTableElements;
    this.maxInstances = builder.maxInstances;
    this.maxTables = builder.maxTables;
    this.maxMemories = builder.maxMemories;
  }

  /**
   * Gets the maximum total memory in bytes across all memories.
   *
   * @return an Optional containing the limit, or empty if unlimited
   */
  public OptionalLong getMaxMemoryBytes() {
    return maxMemoryBytes == null ? OptionalLong.empty() : OptionalLong.of(maxMemoryBytes);
  }

  /**
   * Gets the maximum memory pages per memory (64KB per page).
   *
   * @return an Optional containing the limit, or empty if unlimited
   */
  public OptionalLong getMaxMemoryPages() {
    return maxMemoryPages == null ? OptionalLong.empty() : OptionalLong.of(maxMemoryPages);
  }

  /**
   * Gets the maximum table elements per table.
   *
   * @return an Optional containing the limit, or empty if unlimited
   */
  public OptionalLong getMaxTableElements() {
    return maxTableElements == null ? OptionalLong.empty() : OptionalLong.of(maxTableElements);
  }

  /**
   * Gets the maximum number of instances.
   *
   * @return an Optional containing the limit, or empty if unlimited
   */
  public OptionalInt getMaxInstances() {
    return maxInstances == null ? OptionalInt.empty() : OptionalInt.of(maxInstances);
  }

  /**
   * Gets the maximum number of tables.
   *
   * @return an Optional containing the limit, or empty if unlimited
   */
  public OptionalInt getMaxTables() {
    return maxTables == null ? OptionalInt.empty() : OptionalInt.of(maxTables);
  }

  /**
   * Gets the maximum number of memories.
   *
   * @return an Optional containing the limit, or empty if unlimited
   */
  public OptionalInt getMaxMemories() {
    return maxMemories == null ? OptionalInt.empty() : OptionalInt.of(maxMemories);
  }

  /**
   * Creates a new builder for ResourceLimiterConfig.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default configuration with no limits.
   *
   * @return a configuration with no limits set
   */
  public static ResourceLimiterConfig defaults() {
    return new Builder().build();
  }

  @Override
  public String toString() {
    return "ResourceLimiterConfig{"
        + "maxMemoryBytes="
        + maxMemoryBytes
        + ", maxMemoryPages="
        + maxMemoryPages
        + ", maxTableElements="
        + maxTableElements
        + ", maxInstances="
        + maxInstances
        + ", maxTables="
        + maxTables
        + ", maxMemories="
        + maxMemories
        + '}';
  }

  /**
   * Builder for creating ResourceLimiterConfig instances.
   *
   * @since 1.0.0
   */
  public static final class Builder {
    private Long maxMemoryBytes;
    private Long maxMemoryPages;
    private Long maxTableElements;
    private Integer maxInstances;
    private Integer maxTables;
    private Integer maxMemories;

    private Builder() {}

    /**
     * Sets the maximum total memory in bytes across all memories.
     *
     * @param bytes the maximum memory in bytes
     * @return this builder
     * @throws IllegalArgumentException if bytes is negative
     */
    public Builder maxMemoryBytes(final long bytes) {
      if (bytes < 0) {
        throw new IllegalArgumentException("Max memory bytes cannot be negative: " + bytes);
      }
      this.maxMemoryBytes = bytes;
      return this;
    }

    /**
     * Sets the maximum memory pages per memory (64KB per page).
     *
     * @param pages the maximum number of pages
     * @return this builder
     * @throws IllegalArgumentException if pages is negative
     */
    public Builder maxMemoryPages(final long pages) {
      if (pages < 0) {
        throw new IllegalArgumentException("Max memory pages cannot be negative: " + pages);
      }
      this.maxMemoryPages = pages;
      return this;
    }

    /**
     * Sets the maximum table elements per table.
     *
     * @param elements the maximum number of elements
     * @return this builder
     * @throws IllegalArgumentException if elements is negative
     */
    public Builder maxTableElements(final long elements) {
      if (elements < 0) {
        throw new IllegalArgumentException("Max table elements cannot be negative: " + elements);
      }
      this.maxTableElements = elements;
      return this;
    }

    /**
     * Sets the maximum number of instances.
     *
     * @param count the maximum number of instances
     * @return this builder
     * @throws IllegalArgumentException if count is negative
     */
    public Builder maxInstances(final int count) {
      if (count < 0) {
        throw new IllegalArgumentException("Max instances cannot be negative: " + count);
      }
      this.maxInstances = count;
      return this;
    }

    /**
     * Sets the maximum number of tables.
     *
     * @param count the maximum number of tables
     * @return this builder
     * @throws IllegalArgumentException if count is negative
     */
    public Builder maxTables(final int count) {
      if (count < 0) {
        throw new IllegalArgumentException("Max tables cannot be negative: " + count);
      }
      this.maxTables = count;
      return this;
    }

    /**
     * Sets the maximum number of memories.
     *
     * @param count the maximum number of memories
     * @return this builder
     * @throws IllegalArgumentException if count is negative
     */
    public Builder maxMemories(final int count) {
      if (count < 0) {
        throw new IllegalArgumentException("Max memories cannot be negative: " + count);
      }
      this.maxMemories = count;
      return this;
    }

    /**
     * Builds the ResourceLimiterConfig instance.
     *
     * @return a new ResourceLimiterConfig with the configured limits
     */
    public ResourceLimiterConfig build() {
      return new ResourceLimiterConfig(this);
    }
  }
}

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
package ai.tegmentum.wasmtime4j.component;

/**
 * Configuration for stores created during component pre-instantiation.
 *
 * <p>Controls resource limits such as fuel metering, epoch deadlines, and memory bounds for
 * component instance stores created by {@link
 * ComponentInstancePre#instantiate(ComponentStoreConfig)}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ComponentStoreConfig config = ComponentStoreConfig.builder()
 *     .fuelLimit(1_000_000)
 *     .epochDeadline(5)
 *     .maxMemoryBytes(64 * 1024 * 1024)
 *     .build();
 *
 * ComponentInstance instance = pre.instantiate(config);
 * }</pre>
 *
 * @since 1.1.0
 */
public final class ComponentStoreConfig {

  private final long fuelLimit;
  private final long epochDeadline;
  private final long maxMemoryBytes;

  private ComponentStoreConfig(final Builder builder) {
    this.fuelLimit = builder.fuelLimit;
    this.epochDeadline = builder.epochDeadline;
    this.maxMemoryBytes = builder.maxMemoryBytes;
  }

  /**
   * Creates a new builder for ComponentStoreConfig.
   *
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the fuel limit for the store. When fuel is consumed, execution traps.
   *
   * <p>A value of 0 means no fuel metering is applied.
   *
   * @return the fuel limit, or 0 for unlimited
   */
  public long getFuelLimit() {
    return fuelLimit;
  }

  /**
   * Gets the epoch deadline for the store. When the engine's epoch counter reaches the deadline,
   * execution traps. This enables cooperative timeout/interruption.
   *
   * <p>A value of 0 means no epoch deadline is set.
   *
   * @return the epoch deadline ticks, or 0 for no deadline
   */
  public long getEpochDeadline() {
    return epochDeadline;
  }

  /**
   * Gets the maximum memory size in bytes for the store.
   *
   * <p>A value of 0 means no memory limit is applied.
   *
   * @return the maximum memory size in bytes, or 0 for unlimited
   */
  public long getMaxMemoryBytes() {
    return maxMemoryBytes;
  }

  /** Builder for {@link ComponentStoreConfig}. */
  public static final class Builder {

    private long fuelLimit;
    private long epochDeadline;
    private long maxMemoryBytes;

    private Builder() {}

    /**
     * Sets the fuel limit for the store. The engine must have been configured with {@code
     * consumeFuel(true)} for this to take effect.
     *
     * @param fuelLimit the amount of fuel to add (0 for no fuel metering)
     * @return this builder
     * @throws IllegalArgumentException if fuelLimit is negative
     */
    public Builder fuelLimit(final long fuelLimit) {
      if (fuelLimit < 0) {
        throw new IllegalArgumentException("fuelLimit must be non-negative");
      }
      this.fuelLimit = fuelLimit;
      return this;
    }

    /**
     * Sets the epoch deadline for the store. The engine must have been configured with {@code
     * epochInterruption(true)} for this to take effect.
     *
     * @param epochDeadline the epoch deadline in ticks (0 for no deadline)
     * @return this builder
     * @throws IllegalArgumentException if epochDeadline is negative
     */
    public Builder epochDeadline(final long epochDeadline) {
      if (epochDeadline < 0) {
        throw new IllegalArgumentException("epochDeadline must be non-negative");
      }
      this.epochDeadline = epochDeadline;
      return this;
    }

    /**
     * Sets the maximum memory size for the store. This limits the total linear memory that can be
     * allocated by WebAssembly modules within this store.
     *
     * @param maxMemoryBytes the maximum memory in bytes (0 for unlimited)
     * @return this builder
     * @throws IllegalArgumentException if maxMemoryBytes is negative
     */
    public Builder maxMemoryBytes(final long maxMemoryBytes) {
      if (maxMemoryBytes < 0) {
        throw new IllegalArgumentException("maxMemoryBytes must be non-negative");
      }
      this.maxMemoryBytes = maxMemoryBytes;
      return this;
    }

    /**
     * Builds the ComponentStoreConfig.
     *
     * @return a new ComponentStoreConfig instance
     */
    public ComponentStoreConfig build() {
      return new ComponentStoreConfig(this);
    }
  }
}

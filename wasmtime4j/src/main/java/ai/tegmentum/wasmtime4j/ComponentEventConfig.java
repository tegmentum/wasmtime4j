/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for component event system.
 *
 * <p>This class defines settings for event communication between WebAssembly components, including
 * buffer sizes, timeouts, and delivery guarantees.
 *
 * @since 1.0.0
 */
public final class ComponentEventConfig {

  private final int maxQueueSize;
  private final Duration eventTimeout;
  private final boolean enablePersistence;
  private final boolean enableOrdering;
  private final int maxRetries;

  /**
   * Creates a new component event configuration with default values.
   *
   * <p>Default values:
   *
   * <ul>
   *   <li>maxQueueSize: 1000
   *   <li>eventTimeout: 30 seconds
   *   <li>enablePersistence: false
   *   <li>enableOrdering: true
   *   <li>maxRetries: 3
   * </ul>
   */
  public ComponentEventConfig() {
    this(1000, Duration.ofSeconds(30), false, true, 3);
  }

  /**
   * Creates a new component event configuration with specified values.
   *
   * @param maxQueueSize maximum event queue size
   * @param eventTimeout timeout for event delivery
   * @param enablePersistence whether to persist events
   * @param enableOrdering whether to guarantee event ordering
   * @param maxRetries maximum number of delivery retries
   */
  public ComponentEventConfig(
      final int maxQueueSize,
      final Duration eventTimeout,
      final boolean enablePersistence,
      final boolean enableOrdering,
      final int maxRetries) {
    if (maxQueueSize <= 0) {
      throw new IllegalArgumentException("maxQueueSize must be positive");
    }
    this.maxQueueSize = maxQueueSize;
    this.eventTimeout = Objects.requireNonNull(eventTimeout, "eventTimeout cannot be null");
    this.enablePersistence = enablePersistence;
    this.enableOrdering = enableOrdering;
    this.maxRetries = Math.max(0, maxRetries);
  }

  /**
   * Gets the maximum event queue size.
   *
   * @return maximum queue size
   */
  public int getMaxQueueSize() {
    return maxQueueSize;
  }

  /**
   * Gets the event delivery timeout.
   *
   * @return event timeout
   */
  public Duration getEventTimeout() {
    return eventTimeout;
  }

  /**
   * Checks if event persistence is enabled.
   *
   * @return true if persistence is enabled
   */
  public boolean isPersistenceEnabled() {
    return enablePersistence;
  }

  /**
   * Checks if event ordering is enabled.
   *
   * @return true if ordering is enabled
   */
  public boolean isOrderingEnabled() {
    return enableOrdering;
  }

  /**
   * Gets the maximum number of delivery retries.
   *
   * @return maximum retries
   */
  public int getMaxRetries() {
    return maxRetries;
  }

  /**
   * Creates a new builder for component event configuration.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for component event configuration. */
  public static final class Builder {
    private int maxQueueSize = 1000;
    private Duration eventTimeout = Duration.ofSeconds(30);
    private boolean enablePersistence = false;
    private boolean enableOrdering = true;
    private int maxRetries = 3;

    /**
     * Sets the maximum event queue size.
     *
     * @param maxQueueSize maximum queue size
     * @return this builder
     */
    public Builder maxQueueSize(final int maxQueueSize) {
      this.maxQueueSize = maxQueueSize;
      return this;
    }

    /**
     * Sets the event delivery timeout.
     *
     * @param eventTimeout event timeout
     * @return this builder
     */
    public Builder eventTimeout(final Duration eventTimeout) {
      this.eventTimeout = eventTimeout;
      return this;
    }

    /**
     * Sets whether to enable event persistence.
     *
     * @param enablePersistence true to enable persistence
     * @return this builder
     */
    public Builder enablePersistence(final boolean enablePersistence) {
      this.enablePersistence = enablePersistence;
      return this;
    }

    /**
     * Sets whether to enable event ordering.
     *
     * @param enableOrdering true to enable ordering
     * @return this builder
     */
    public Builder enableOrdering(final boolean enableOrdering) {
      this.enableOrdering = enableOrdering;
      return this;
    }

    /**
     * Sets the maximum number of delivery retries.
     *
     * @param maxRetries maximum retries
     * @return this builder
     */
    public Builder maxRetries(final int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    /**
     * Builds the component event configuration.
     *
     * @return new configuration instance
     */
    public ComponentEventConfig build() {
      return new ComponentEventConfig(
          maxQueueSize, eventTimeout, enablePersistence, enableOrdering, maxRetries);
    }
  }
}

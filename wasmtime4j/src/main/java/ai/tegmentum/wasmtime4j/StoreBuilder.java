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
package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.config.ResourceLimiter;
import ai.tegmentum.wasmtime4j.config.StoreLimits;
import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Builder for creating Store instances with custom configuration.
 *
 * <p>This builder provides a fluent API for configuring WebAssembly stores with custom data, fuel
 * limits, and other runtime options.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Store store = Store.builder(engine)
 *     .withData(myUserData)
 *     .withFuel(10000)
 *     .withEpochDeadline(100)
 *     .build();
 * }</pre>
 *
 * @param <T> the type of user data associated with the store
 * @since 1.0.0
 */
public final class StoreBuilder<T> {
  private final Engine engine;
  private T data;
  private Long fuel;
  private Long epochDeadline;
  private StoreLimits limits;
  private ResourceLimiter resourceLimiter;

  /**
   * Creates a new StoreBuilder for the given engine.
   *
   * @param engine the engine to create the store for
   */
  StoreBuilder(final Engine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    this.engine = engine;
  }

  /**
   * Sets the user data to associate with the store.
   *
   * <p>This data will be accessible from host functions through the Caller interface.
   *
   * @param data the user data to associate
   * @return this builder for method chaining
   */
  public StoreBuilder<T> withData(final T data) {
    this.data = data;
    return this;
  }

  /**
   * Sets the initial fuel amount for the store.
   *
   * <p>Fuel metering must be enabled in the engine for this to have effect.
   *
   * @param fuel the initial fuel amount
   * @return this builder for method chaining
   * @throws IllegalArgumentException if fuel is negative
   */
  public StoreBuilder<T> withFuel(final long fuel) {
    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel cannot be negative");
    }
    this.fuel = fuel;
    return this;
  }

  /**
   * Sets the epoch deadline for the store.
   *
   * <p>Epoch interruption must be enabled in the engine for this to have effect.
   *
   * @param deadline the epoch deadline
   * @return this builder for method chaining
   * @throws IllegalArgumentException if deadline is negative
   */
  public StoreBuilder<T> withEpochDeadline(final long deadline) {
    if (deadline < 0) {
      throw new IllegalArgumentException("Epoch deadline cannot be negative");
    }
    this.epochDeadline = deadline;
    return this;
  }

  /**
   * Sets resource limits for the store.
   *
   * @param limits the resource limits to apply
   * @return this builder for method chaining
   * @throws IllegalArgumentException if limits is null
   */
  public StoreBuilder<T> withLimits(final StoreLimits limits) {
    if (limits == null) {
      throw new IllegalArgumentException("StoreLimits cannot be null");
    }
    this.limits = limits;
    return this;
  }

  /**
   * Sets a dynamic resource limiter for the store.
   *
   * <p>A {@link ResourceLimiter} provides dynamic, callback-based resource limiting that is invoked
   * each time a memory or table needs to grow. This is complementary to {@link StoreLimits}: static
   * limits can be set via {@link #withLimits(StoreLimits)} and dynamic limiting via this method.
   *
   * @param resourceLimiter the resource limiter to set
   * @return this builder for method chaining
   * @throws IllegalArgumentException if resourceLimiter is null
   */
  public StoreBuilder<T> withResourceLimiter(final ResourceLimiter resourceLimiter) {
    if (resourceLimiter == null) {
      throw new IllegalArgumentException("ResourceLimiter cannot be null");
    }
    this.resourceLimiter = resourceLimiter;
    return this;
  }

  /**
   * Builds a Store with the configured settings.
   *
   * @return a new Store instance
   * @throws WasmException if store creation fails
   */
  public Store build() throws WasmException {
    final Store store;

    if (limits != null) {
      store = Store.create(engine, limits);
    } else {
      store = Store.create(engine);
    }

    // Set user data if provided
    if (data != null) {
      store.setData(data);
    }

    // Set fuel if provided and fuel metering is enabled
    if (fuel != null && engine.isFuelEnabled()) {
      store.setFuel(fuel);
    }

    // Set epoch deadline if provided and epoch interruption is enabled
    if (epochDeadline != null && engine.isEpochInterruptionEnabled()) {
      store.setEpochDeadline(epochDeadline);
    }

    // Set resource limiter if provided
    if (resourceLimiter != null) {
      store.setResourceLimiter(resourceLimiter);
    }

    return store;
  }

  /**
   * Gets the engine this builder is configured for.
   *
   * @return the engine
   */
  Engine getEngine() {
    return engine;
  }

  /**
   * Gets the user data.
   *
   * @return the user data, or null if not set
   */
  T getData() {
    return data;
  }

  /**
   * Gets the fuel amount.
   *
   * @return the fuel amount, or null if not set
   */
  Long getFuel() {
    return fuel;
  }

  /**
   * Gets the epoch deadline.
   *
   * @return the epoch deadline, or null if not set
   */
  Long getEpochDeadline() {
    return epochDeadline;
  }

  /**
   * Gets the resource limits.
   *
   * @return the resource limits, or null if not set
   */
  StoreLimits getLimits() {
    return limits;
  }

  /**
   * Gets the resource limiter.
   *
   * @return the resource limiter, or null if not set
   */
  ResourceLimiter getResourceLimiter() {
    return resourceLimiter;
  }
}

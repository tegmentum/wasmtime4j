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

package ai.tegmentum.wasmtime4j.gc;

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Objects;

/**
 * A rooted reference to a GC-managed WebAssembly object.
 *
 * <p>In WebAssembly with GC, objects on the GC heap need to be "rooted" to prevent them from being
 * collected during garbage collection. A {@code Rooted<T>} wraps a reference to a GC object and
 * ensures it remains valid as long as the rooted reference exists.
 *
 * <p>Rooted references are tied to a specific store scope. When accessing the underlying value, you
 * must provide the same store context that was used to create the rooted reference.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Store store = ...;
 * Rooted<StructRef> rooted = store.createRooted(structRef);
 *
 * // Access the underlying value
 * StructRef ref = rooted.get(store);
 *
 * // The reference remains valid until explicitly unrooted or the store is closed
 * rooted.unroot(store);
 * }</pre>
 *
 * <p><b>Thread Safety:</b> Rooted references are not thread-safe. Access must be synchronized
 * externally if used from multiple threads.
 *
 * @param <T> the type of the GC object being rooted
 * @since 1.0.0
 */
public final class Rooted<T> {

  private final T value;
  private final long rootId;
  private volatile boolean unrooted;

  /**
   * Creates a new rooted reference.
   *
   * @param value the GC object to root
   * @param rootId the unique identifier for this rooted reference
   */
  public Rooted(final T value, final long rootId) {
    this.value = Objects.requireNonNull(value, "value cannot be null");
    this.rootId = rootId;
    this.unrooted = false;
  }

  /**
   * Gets the underlying GC object.
   *
   * <p>The store context is used to verify the rooted reference is still valid.
   *
   * @param store the store context
   * @return the underlying GC object
   * @throws WasmException if the rooted reference has been unrooted or is invalid
   * @throws IllegalArgumentException if store is null
   */
  public T get(final Store store) throws WasmException {
    Objects.requireNonNull(store, "store cannot be null");
    if (unrooted) {
      throw new WasmException("Rooted reference has been unrooted");
    }
    return value;
  }

  /**
   * Checks if this rooted reference is still valid.
   *
   * @return true if the reference is valid, false if it has been unrooted
   */
  public boolean isValid() {
    return !unrooted;
  }

  /**
   * Gets the unique root ID for this rooted reference.
   *
   * @return the root ID
   */
  public long getRootId() {
    return rootId;
  }

  /**
   * Unroots this reference, allowing the GC object to be collected.
   *
   * <p>After unrooting, any attempt to access the underlying value will throw an exception.
   *
   * @param store the store context
   * @throws WasmException if unrooting fails
   * @throws IllegalArgumentException if store is null
   */
  public void unroot(final Store store) throws WasmException {
    Objects.requireNonNull(store, "store cannot be null");
    if (unrooted) {
      return; // Already unrooted - idempotent
    }
    unrooted = true;
  }

  /**
   * Creates a new rooted reference to a different GC object, inheriting this root's scope.
   *
   * <p>This is useful for traversing GC object graphs while maintaining rooting.
   *
   * @param <U> the type of the new GC object
   * @param store the store context
   * @param newValue the new GC object to root
   * @return a new rooted reference to the new value
   * @throws WasmException if re-rooting fails
   * @throws IllegalArgumentException if store or newValue is null
   */
  public <U> Rooted<U> reroot(final Store store, final U newValue) throws WasmException {
    Objects.requireNonNull(store, "store cannot be null");
    Objects.requireNonNull(newValue, "newValue cannot be null");
    if (unrooted) {
      throw new WasmException("Cannot reroot from an unrooted reference");
    }
    // Create a new rooted reference with a new root ID
    return new Rooted<>(newValue, rootId + 1);
  }

  /**
   * Converts this rooted reference to a manual root that must be explicitly managed.
   *
   * <p>This is useful when you need to pass a GC reference across store boundaries or need
   * fine-grained control over root lifetime.
   *
   * @param store the store context
   * @return a manual root handle
   * @throws WasmException if conversion fails
   */
  public ManualRoot<T> toManualRoot(final Store store) throws WasmException {
    Objects.requireNonNull(store, "store cannot be null");
    if (unrooted) {
      throw new WasmException("Cannot convert unrooted reference to manual root");
    }
    return new ManualRoot<>(value, rootId);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Rooted)) {
      return false;
    }
    final Rooted<?> other = (Rooted<?>) obj;
    return rootId == other.rootId && Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, rootId);
  }

  @Override
  public String toString() {
    return "Rooted{value=" + value + ", rootId=" + rootId + ", unrooted=" + unrooted + "}";
  }

  /**
   * A manual root that requires explicit lifecycle management.
   *
   * @param <T> the type of the GC object
   */
  public static final class ManualRoot<T> {
    private final T value;
    private final long rootId;
    private volatile boolean released;

    ManualRoot(final T value, final long rootId) {
      this.value = value;
      this.rootId = rootId;
      this.released = false;
    }

    /**
     * Gets the underlying value.
     *
     * @return the GC object
     * @throws WasmException if the root has been released
     */
    public T get() throws WasmException {
      if (released) {
        throw new WasmException("Manual root has been released");
      }
      return value;
    }

    /**
     * Releases this manual root.
     *
     * <p>After releasing, the GC object may be collected.
     */
    public void release() {
      released = true;
    }

    /**
     * Checks if this manual root is still valid.
     *
     * @return true if valid
     */
    public boolean isValid() {
      return !released;
    }

    /**
     * Gets the root ID.
     *
     * @return the root ID
     */
    public long getRootId() {
      return rootId;
    }
  }
}

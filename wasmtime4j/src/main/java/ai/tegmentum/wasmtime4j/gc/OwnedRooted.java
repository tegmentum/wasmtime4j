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
import java.util.concurrent.atomic.AtomicLong;

/**
 * An owned rooted reference to a GC-managed WebAssembly object.
 *
 * <p>Unlike {@link Rooted} which may be scope-bound, an OwnedRooted has exclusive ownership over
 * the rooting and must be explicitly released. This is useful when you need to transfer ownership
 * of a GC reference between different contexts.
 *
 * <p>OwnedRooted implements {@link AutoCloseable} for convenient use with try-with-resources:
 *
 * <pre>{@code
 * try (OwnedRooted<StructRef> owned = OwnedRooted.create(store, structRef)) {
 *     // Work with the rooted reference
 *     StructRef ref = owned.get(store);
 * } // Automatically released here
 * }</pre>
 *
 * <p><b>Thread Safety:</b> OwnedRooted is not thread-safe. Access must be synchronized externally
 * if used from multiple threads.
 *
 * @param <T> the type of the GC object being rooted
 * @since 1.0.0
 */
public final class OwnedRooted<T> implements AutoCloseable {

  private static final AtomicLong ID_COUNTER = new AtomicLong(0);

  private final T value;
  private final long rootId;
  private final long storeId;
  private volatile boolean released;

  private OwnedRooted(final T value, final long storeId) {
    this.value = Objects.requireNonNull(value, "value cannot be null");
    this.rootId = ID_COUNTER.incrementAndGet();
    this.storeId = storeId;
    this.released = false;
  }

  /**
   * Creates an owned rooted reference.
   *
   * @param <T> the type of the GC object
   * @param store the store context for rooting
   * @param value the value to root
   * @return a new OwnedRooted
   * @throws NullPointerException if store or value is null
   */
  public static <T> OwnedRooted<T> create(final Store store, final T value) {
    Objects.requireNonNull(store, "store cannot be null");
    Objects.requireNonNull(value, "value cannot be null");
    return new OwnedRooted<>(value, System.identityHashCode(store));
  }

  /**
   * Creates an owned rooted reference for an AnyRef.
   *
   * @param store the store context for rooting
   * @param anyRef the any reference to root
   * @return a new OwnedRooted
   * @throws NullPointerException if store or anyRef is null
   */
  public static OwnedRooted<AnyRef> create(final Store store, final AnyRef anyRef) {
    Objects.requireNonNull(store, "store cannot be null");
    Objects.requireNonNull(anyRef, "anyRef cannot be null");
    return new OwnedRooted<>(anyRef, System.identityHashCode(store));
  }

  /**
   * Creates an owned rooted reference for an EqRef.
   *
   * @param store the store context for rooting
   * @param eqRef the eq reference to root
   * @return a new OwnedRooted
   * @throws NullPointerException if store or eqRef is null
   */
  public static OwnedRooted<EqRef> create(final Store store, final EqRef eqRef) {
    Objects.requireNonNull(store, "store cannot be null");
    Objects.requireNonNull(eqRef, "eqRef cannot be null");
    return new OwnedRooted<>(eqRef, System.identityHashCode(store));
  }

  /**
   * Creates an owned rooted reference for a StructRef.
   *
   * @param store the store context for rooting
   * @param structRef the struct reference to root
   * @return a new OwnedRooted
   * @throws NullPointerException if store or structRef is null
   */
  public static OwnedRooted<StructRef> create(final Store store, final StructRef structRef) {
    Objects.requireNonNull(store, "store cannot be null");
    Objects.requireNonNull(structRef, "structRef cannot be null");
    return new OwnedRooted<>(structRef, System.identityHashCode(store));
  }

  /**
   * Creates an owned rooted reference for an ArrayRef.
   *
   * @param store the store context for rooting
   * @param arrayRef the array reference to root
   * @return a new OwnedRooted
   * @throws NullPointerException if store or arrayRef is null
   */
  public static OwnedRooted<ArrayRef> create(final Store store, final ArrayRef arrayRef) {
    Objects.requireNonNull(store, "store cannot be null");
    Objects.requireNonNull(arrayRef, "arrayRef cannot be null");
    return new OwnedRooted<>(arrayRef, System.identityHashCode(store));
  }

  /**
   * Gets the underlying GC object.
   *
   * @param store the store context (must match the store used for creation)
   * @return the underlying GC object
   * @throws WasmException if the reference has been released
   * @throws IllegalArgumentException if store is null or doesn't match
   */
  public T get(final Store store) throws WasmException {
    Objects.requireNonNull(store, "store cannot be null");
    if (released) {
      throw new WasmException("OwnedRooted reference has been released");
    }
    if (System.identityHashCode(store) != storeId) {
      throw new IllegalArgumentException("Store does not match the store used for rooting");
    }
    return value;
  }

  /**
   * Checks if this owned rooted reference is still valid.
   *
   * @return true if the reference is valid, false if released
   */
  public boolean isValid() {
    return !released;
  }

  /**
   * Gets the root ID.
   *
   * @return the unique root identifier
   */
  public long getRootId() {
    return rootId;
  }

  /**
   * Gets the store ID this reference is associated with.
   *
   * @return the store ID
   */
  public long getStoreId() {
    return storeId;
  }

  /**
   * Releases this owned rooted reference.
   *
   * <p>After releasing, any attempt to access the underlying value will throw an exception. The GC
   * object may be collected if no other references exist.
   */
  public void release() {
    released = true;
  }

  /**
   * Converts this owned rooted reference to a scope-bound Rooted reference.
   *
   * <p>This transfers the rooting to the scope. After conversion, this OwnedRooted is released.
   *
   * @param scope the root scope to transfer to
   * @return a new Rooted reference in the given scope
   * @throws IllegalStateException if this reference has been released
   * @throws NullPointerException if scope is null
   */
  public Rooted<T> transferToScope(final RootScope scope) {
    Objects.requireNonNull(scope, "scope cannot be null");
    if (released) {
      throw new IllegalStateException("Cannot transfer released OwnedRooted");
    }
    released = true;
    return scope.root(value);
  }

  /**
   * Closes this owned rooted reference, releasing the root.
   *
   * <p>This is equivalent to calling {@link #release()}.
   */
  @Override
  public void close() {
    release();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof OwnedRooted)) {
      return false;
    }
    OwnedRooted<?> other = (OwnedRooted<?>) obj;
    return rootId == other.rootId && Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, rootId);
  }

  @Override
  public String toString() {
    return "OwnedRooted{value="
        + value
        + ", rootId="
        + rootId
        + ", storeId="
        + storeId
        + ", released="
        + released
        + "}";
  }
}

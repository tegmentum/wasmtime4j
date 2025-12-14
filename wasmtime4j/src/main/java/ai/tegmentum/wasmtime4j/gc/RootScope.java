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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A scope for managing rooted GC references with automatic cleanup.
 *
 * <p>RootScope provides a convenient way to manage multiple rooted references within a specific
 * scope. When the scope is exited (via {@link #close()}), all references rooted within that scope
 * are automatically unrooted, allowing them to be garbage collected.
 *
 * <p>This is particularly useful for temporary operations where multiple GC references are created
 * but only need to survive for a limited duration.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (RootScope scope = RootScope.create(store)) {
 *     Rooted<StructRef> ref1 = scope.root(structRef);
 *     Rooted<ArrayRef> ref2 = scope.root(arrayRef);
 *
 *     // Work with rooted references...
 *
 * } // All references automatically unrooted here
 * }</pre>
 *
 * <p><b>Thread Safety:</b> RootScope is not thread-safe. Access must be synchronized externally if
 * used from multiple threads.
 *
 * @since 1.0.0
 */
public final class RootScope implements AutoCloseable {

  private static final AtomicLong SCOPE_ID_COUNTER = new AtomicLong(0);

  private final Store store;
  private final List<Rooted<?>> rootedRefs;
  private final long scopeId;
  private volatile boolean closed;

  private RootScope(final Store store) {
    this.store = Objects.requireNonNull(store, "store cannot be null");
    this.rootedRefs = new ArrayList<>();
    this.scopeId = SCOPE_ID_COUNTER.incrementAndGet();
    this.closed = false;
  }

  /**
   * Creates a new root scope for the given store.
   *
   * @param store the store to associate with this scope
   * @return a new RootScope
   * @throws NullPointerException if store is null
   */
  public static RootScope create(final Store store) {
    return new RootScope(store);
  }

  /**
   * Roots a GC reference within this scope.
   *
   * <p>The reference will be automatically unrooted when this scope is closed.
   *
   * @param <T> the type of the GC object
   * @param value the GC object to root
   * @return a rooted reference to the value
   * @throws IllegalStateException if this scope is closed
   * @throws NullPointerException if value is null
   */
  public <T> Rooted<T> root(final T value) {
    checkNotClosed();
    Objects.requireNonNull(value, "value cannot be null");
    Rooted<T> rooted = new Rooted<>(value, scopeId * 1000 + rootedRefs.size());
    rootedRefs.add(rooted);
    return rooted;
  }

  /**
   * Roots an AnyRef within this scope.
   *
   * @param anyRef the any reference to root
   * @return a rooted reference
   * @throws IllegalStateException if this scope is closed
   * @throws NullPointerException if anyRef is null
   */
  public Rooted<AnyRef> root(final AnyRef anyRef) {
    checkNotClosed();
    Objects.requireNonNull(anyRef, "anyRef cannot be null");
    Rooted<AnyRef> rooted = new Rooted<>(anyRef, scopeId * 1000 + rootedRefs.size());
    rootedRefs.add(rooted);
    return rooted;
  }

  /**
   * Roots an EqRef within this scope.
   *
   * @param eqRef the eq reference to root
   * @return a rooted reference
   * @throws IllegalStateException if this scope is closed
   * @throws NullPointerException if eqRef is null
   */
  public Rooted<EqRef> root(final EqRef eqRef) {
    checkNotClosed();
    Objects.requireNonNull(eqRef, "eqRef cannot be null");
    Rooted<EqRef> rooted = new Rooted<>(eqRef, scopeId * 1000 + rootedRefs.size());
    rootedRefs.add(rooted);
    return rooted;
  }

  /**
   * Roots a StructRef within this scope.
   *
   * @param structRef the struct reference to root
   * @return a rooted reference
   * @throws IllegalStateException if this scope is closed
   * @throws NullPointerException if structRef is null
   */
  public Rooted<StructRef> root(final StructRef structRef) {
    checkNotClosed();
    Objects.requireNonNull(structRef, "structRef cannot be null");
    Rooted<StructRef> rooted = new Rooted<>(structRef, scopeId * 1000 + rootedRefs.size());
    rootedRefs.add(rooted);
    return rooted;
  }

  /**
   * Roots an ArrayRef within this scope.
   *
   * @param arrayRef the array reference to root
   * @return a rooted reference
   * @throws IllegalStateException if this scope is closed
   * @throws NullPointerException if arrayRef is null
   */
  public Rooted<ArrayRef> root(final ArrayRef arrayRef) {
    checkNotClosed();
    Objects.requireNonNull(arrayRef, "arrayRef cannot be null");
    Rooted<ArrayRef> rooted = new Rooted<>(arrayRef, scopeId * 1000 + rootedRefs.size());
    rootedRefs.add(rooted);
    return rooted;
  }

  /**
   * Gets the number of rooted references in this scope.
   *
   * @return the count of rooted references
   */
  public int getRootedCount() {
    return rootedRefs.size();
  }

  /**
   * Gets the scope ID.
   *
   * @return the unique scope identifier
   */
  public long getScopeId() {
    return scopeId;
  }

  /**
   * Checks if this scope is still open.
   *
   * @return true if the scope is open
   */
  public boolean isOpen() {
    return !closed;
  }

  /**
   * Gets the store associated with this scope.
   *
   * @return the store
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification =
          "Store is intentionally shared as it represents the WebAssembly runtime" + " context")
  public Store getStore() {
    return store;
  }

  private void checkNotClosed() {
    if (closed) {
      throw new IllegalStateException("RootScope has been closed");
    }
  }

  /**
   * Closes this scope and unroots all references.
   *
   * <p>After closing, any attempt to use this scope will throw an exception. Rooted references
   * created from this scope will no longer be valid.
   */
  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;

    // Unroot all references in reverse order
    for (int i = rootedRefs.size() - 1; i >= 0; i--) {
      try {
        rootedRefs.get(i).unroot(store);
      } catch (WasmException e) {
        // Log but continue unrooting
      }
    }
    rootedRefs.clear();
  }

  @Override
  public String toString() {
    return "RootScope{scopeId="
        + scopeId
        + ", rootedCount="
        + rootedRefs.size()
        + ", closed="
        + closed
        + "}";
  }
}

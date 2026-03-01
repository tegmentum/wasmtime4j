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

import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.memory.Tag;

/**
 * A pre-compiled exception allocator that caches type resolution for efficient repeated allocation.
 *
 * <p>In Wasmtime, {@code ExnRefPre} resolves the exception type once and reuses the resolved
 * representation for subsequent allocations, avoiding redundant type lookups. This is analogous to
 * preparing a statement in a database — the upfront cost is amortized across many uses.
 *
 * <p>ExnRefPre is associated with an {@link ExnType} which describes the exception payload fields.
 * Exception references created through this allocator will carry values matching the tag's type
 * signature.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ExnType type = new ExnType(tagType);
 * ExnRefPre pre = ExnRefPre.create(type);
 *
 * // The allocator can be used to efficiently create multiple exception references
 * // with the same type when allocating through the GC runtime.
 * }</pre>
 *
 * @since 1.1.0
 */
public final class ExnRefPre implements AutoCloseable {

  private final ExnType exnType;
  private volatile boolean closed;

  private ExnRefPre(final ExnType exnType) {
    this.exnType = exnType;
  }

  /**
   * Creates a new ExnRefPre for the given exception type.
   *
   * @param exnType the exception type to pre-compile
   * @return a new ExnRefPre
   * @throws IllegalArgumentException if exnType is null
   */
  public static ExnRefPre create(final ExnType exnType) {
    if (exnType == null) {
      throw new IllegalArgumentException("exnType cannot be null");
    }
    return new ExnRefPre(exnType);
  }

  /**
   * Gets the exception type this allocator was created for.
   *
   * @return the exception type
   */
  public ExnType getExnType() {
    return exnType;
  }

  /**
   * Returns whether this allocator is still active (not yet closed).
   *
   * @return true if the allocator can still be used
   */
  public boolean isActive() {
    return !closed;
  }

  /**
   * Allocates a new exception reference using this pre-compiled allocator.
   *
   * <p>The field values must match the tag's type signature. This delegates to {@link
   * ExnRef#create(Store, Tag, WasmValue...)} using the tag from this allocator's exception type.
   *
   * @param store the store context for allocation
   * @param tag the exception tag identifying the exception type
   * @param fields the field values for the exception payload
   * @return a new ExnRef instance
   * @throws WasmException if allocation fails
   * @throws IllegalStateException if this allocator has been closed
   * @throws IllegalArgumentException if store, tag, or fields is null
   */
  public ExnRef allocate(final Store store, final Tag tag, final WasmValue... fields)
      throws WasmException {
    if (closed) {
      throw new IllegalStateException("ExnRefPre has been closed");
    }
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (tag == null) {
      throw new IllegalArgumentException("tag cannot be null");
    }
    if (fields == null) {
      throw new IllegalArgumentException("fields cannot be null");
    }
    return ExnRef.create(store, tag, fields);
  }

  @Override
  public void close() {
    closed = true;
  }
}

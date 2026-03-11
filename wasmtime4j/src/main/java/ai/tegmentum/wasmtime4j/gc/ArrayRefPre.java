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

import java.util.List;

/**
 * A pre-compiled array allocator that caches type resolution for efficient repeated allocation.
 *
 * <p>In Wasmtime, {@code ArrayRefPre} resolves the array type once and reuses the resolved
 * representation for subsequent allocations, avoiding redundant type lookups. This is analogous to
 * preparing a statement in a database — the upfront cost is amortized across many uses.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ArrayType type = ArrayType.builder("IntArray")
 *     .elementType(FieldType.i32())
 *     .mutable(true)
 *     .build();
 * ArrayRefPre pre = ArrayRefPre.create(type);
 *
 * // Efficiently allocate many arrays with the same type
 * for (int i = 0; i < 1000; i++) {
 *     ArrayRef ref = pre.allocate(gcRuntime, List.of(
 *         GcValue.i32(i), GcValue.i32(i * 2)
 *     ));
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public final class ArrayRefPre implements AutoCloseable {

  private final ArrayType arrayType;
  private volatile boolean closed;

  private ArrayRefPre(final ArrayType arrayType) {
    this.arrayType = arrayType;
  }

  /**
   * Creates a new ArrayRefPre for the given array type.
   *
   * @param arrayType the array type to pre-compile
   * @return a new ArrayRefPre
   * @throws IllegalArgumentException if arrayType is null
   */
  public static ArrayRefPre create(final ArrayType arrayType) {
    if (arrayType == null) {
      throw new IllegalArgumentException("arrayType cannot be null");
    }
    return new ArrayRefPre(arrayType);
  }

  /**
   * Gets the array type this allocator was created for.
   *
   * @return the array type
   */
  public ArrayType getArrayType() {
    return arrayType;
  }

  /**
   * Allocates a new array instance using this pre-compiled allocator.
   *
   * @param gcRuntime the GC runtime to allocate in
   * @param elements the initial element values
   * @return a new ArrayRef
   * @throws GcException if allocation fails
   * @throws IllegalStateException if this allocator has been closed
   * @throws IllegalArgumentException if gcRuntime or elements is null
   */
  public ArrayRef allocate(final GcRuntime gcRuntime, final List<GcValue> elements)
      throws GcException {
    if (closed) {
      throw new IllegalStateException("ArrayRefPre has been closed");
    }
    if (gcRuntime == null) {
      throw new IllegalArgumentException("gcRuntime cannot be null");
    }
    if (elements == null) {
      throw new IllegalArgumentException("elements cannot be null");
    }
    final ArrayInstance instance = gcRuntime.createArray(arrayType, elements);
    return ArrayRef.of(instance);
  }

  /**
   * Allocates a new fixed-length array instance using this pre-compiled allocator.
   *
   * <p>Fixed arrays have all elements set at creation time and may be immutable. This corresponds
   * to Wasmtime's {@code ArrayRef::new_fixed} operation.
   *
   * @param gcRuntime the GC runtime to allocate in
   * @param elements the fixed element values
   * @return a new ArrayRef with the fixed elements
   * @throws GcException if allocation fails
   * @throws IllegalStateException if this allocator has been closed
   * @throws IllegalArgumentException if gcRuntime or elements is null
   */
  public ArrayRef allocateFixed(final GcRuntime gcRuntime, final List<GcValue> elements)
      throws GcException {
    if (closed) {
      throw new IllegalStateException("ArrayRefPre has been closed");
    }
    if (gcRuntime == null) {
      throw new IllegalArgumentException("gcRuntime cannot be null");
    }
    if (elements == null) {
      throw new IllegalArgumentException("elements cannot be null");
    }
    final ArrayInstance instance = gcRuntime.createArrayFixed(arrayType, elements);
    return ArrayRef.of(instance);
  }

  /**
   * Allocates a new array instance with default element values.
   *
   * @param gcRuntime the GC runtime to allocate in
   * @param length the array length
   * @return a new ArrayRef with default values
   * @throws GcException if allocation fails
   * @throws IllegalStateException if this allocator has been closed
   * @throws IllegalArgumentException if gcRuntime is null or length is negative
   */
  public ArrayRef allocateDefault(final GcRuntime gcRuntime, final int length) throws GcException {
    if (closed) {
      throw new IllegalStateException("ArrayRefPre has been closed");
    }
    if (gcRuntime == null) {
      throw new IllegalArgumentException("gcRuntime cannot be null");
    }
    if (length < 0) {
      throw new IllegalArgumentException("length cannot be negative: " + length);
    }
    final ArrayInstance instance = gcRuntime.createArray(arrayType, length);
    return ArrayRef.of(instance);
  }

  /**
   * Asynchronously allocates a new array instance using this pre-compiled allocator.
   *
   * <p>Uses Wasmtime's async resource limiter path for allocation. This should be used when the
   * store is configured with an async resource limiter.
   *
   * @param gcRuntime the GC runtime to allocate in
   * @param elements the initial element values
   * @return a new ArrayRef
   * @throws GcException if allocation fails
   * @throws IllegalStateException if this allocator has been closed
   * @throws IllegalArgumentException if gcRuntime or elements is null
   * @since 1.1.0
   */
  public ArrayRef allocateAsync(final GcRuntime gcRuntime, final List<GcValue> elements)
      throws GcException {
    if (closed) {
      throw new IllegalStateException("ArrayRefPre has been closed");
    }
    if (gcRuntime == null) {
      throw new IllegalArgumentException("gcRuntime cannot be null");
    }
    if (elements == null) {
      throw new IllegalArgumentException("elements cannot be null");
    }
    final ArrayInstance instance = gcRuntime.createArrayAsync(arrayType, elements);
    return ArrayRef.of(instance);
  }

  /**
   * Asynchronously allocates a new fixed-length array instance using this pre-compiled allocator.
   *
   * <p>Uses Wasmtime's async resource limiter path for allocation. This should be used when the
   * store is configured with an async resource limiter.
   *
   * @param gcRuntime the GC runtime to allocate in
   * @param elements the fixed element values
   * @return a new ArrayRef with the fixed elements
   * @throws GcException if allocation fails
   * @throws IllegalStateException if this allocator has been closed
   * @throws IllegalArgumentException if gcRuntime or elements is null
   * @since 1.1.0
   */
  public ArrayRef allocateFixedAsync(final GcRuntime gcRuntime, final List<GcValue> elements)
      throws GcException {
    if (closed) {
      throw new IllegalStateException("ArrayRefPre has been closed");
    }
    if (gcRuntime == null) {
      throw new IllegalArgumentException("gcRuntime cannot be null");
    }
    if (elements == null) {
      throw new IllegalArgumentException("elements cannot be null");
    }
    final ArrayInstance instance = gcRuntime.createArrayFixedAsync(arrayType, elements);
    return ArrayRef.of(instance);
  }

  @Override
  public void close() {
    closed = true;
  }
}

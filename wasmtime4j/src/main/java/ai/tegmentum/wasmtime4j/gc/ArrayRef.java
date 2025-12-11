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
import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Objects;

/**
 * Represents a WebAssembly {@code arrayref} reference type.
 *
 * <p>In the WebAssembly GC proposal, {@code arrayref} represents a reference to an array instance.
 * Arrays are homogeneous sequences of elements with a fixed element type and mutable length.
 *
 * <p>ArrayRef provides access to the underlying array's elements and type information. All arrayrefs
 * are subtypes of both eqref and anyref.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create an array ref
 * ArrayRef ref = ArrayRef.of(arrayInstance);
 *
 * // Access elements
 * GcValue elem = ref.getElement(store, 0);
 * ref.setElement(store, 0, GcValue.i32(42));
 *
 * // Get type and length
 * ArrayType type = ref.getArrayType(store);
 * int length = ref.getLength(store);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class ArrayRef implements GcRef {

  private final ArrayInstance instance;
  private final long id;
  private static long idCounter = 0;

  private ArrayRef(final ArrayInstance instance) {
    this.instance = instance;
    this.id = ++idCounter;
  }

  /**
   * Creates an ArrayRef from an array instance.
   *
   * @param instance the array instance
   * @return a new ArrayRef
   * @throws NullPointerException if instance is null; use {@link #nullRef()} for null references
   */
  public static ArrayRef of(final ArrayInstance instance) {
    Objects.requireNonNull(instance, "instance cannot be null; use nullRef() for null references");
    return new ArrayRef(instance);
  }

  /**
   * Creates a null ArrayRef.
   *
   * @return a null ArrayRef
   */
  public static ArrayRef nullRef() {
    return new ArrayRef(null);
  }

  @Override
  public boolean isNull() {
    return instance == null;
  }

  @Override
  public GcReferenceType getReferenceType() {
    return GcReferenceType.ARRAY_REF;
  }

  @Override
  public long getId() {
    return id;
  }

  /**
   * Gets the underlying array instance.
   *
   * @return the array instance, or null if this is a null reference
   */
  public ArrayInstance getInstance() {
    return instance;
  }

  /**
   * Gets the array type of the referenced array.
   *
   * @param store the store context
   * @return the array type
   * @throws IllegalStateException if this is a null reference
   */
  public ArrayType getArrayType(final Store store) {
    Objects.requireNonNull(store, "store cannot be null");
    if (instance == null) {
      throw new IllegalStateException("Cannot get type of null array reference");
    }
    return instance.getType();
  }

  /**
   * Gets the length of the referenced array.
   *
   * @param store the store context
   * @return the array length
   * @throws IllegalStateException if this is a null reference
   */
  public int getLength(final Store store) {
    Objects.requireNonNull(store, "store cannot be null");
    if (instance == null) {
      throw new IllegalStateException("Cannot get length of null array reference");
    }
    return instance.getLength();
  }

  /**
   * Gets an element from the referenced array.
   *
   * @param store the store context
   * @param index the element index
   * @return the element value
   * @throws IllegalStateException if this is a null reference
   * @throws IndexOutOfBoundsException if index is out of bounds
   * @throws GcException if element access fails
   */
  public GcValue getElement(final Store store, final int index) throws GcException {
    Objects.requireNonNull(store, "store cannot be null");
    if (instance == null) {
      throw new IllegalStateException("Cannot get element from null array reference");
    }
    return instance.getElement(index);
  }

  /**
   * Sets an element in the referenced array.
   *
   * @param store the store context
   * @param index the element index
   * @param value the new value
   * @throws IllegalStateException if this is a null reference
   * @throws IndexOutOfBoundsException if index is out of bounds
   * @throws IllegalArgumentException if array is immutable or value type is incompatible
   * @throws GcException if element assignment fails
   */
  public void setElement(final Store store, final int index, final GcValue value)
      throws GcException {
    Objects.requireNonNull(store, "store cannot be null");
    Objects.requireNonNull(value, "value cannot be null");
    if (instance == null) {
      throw new IllegalStateException("Cannot set element on null array reference");
    }
    instance.setElement(index, value);
  }

  /**
   * Upcasts this ArrayRef to an EqRef.
   *
   * @return a new EqRef containing this array reference
   */
  public EqRef toEqRef() {
    if (instance == null) {
      return EqRef.nullRef();
    }
    return EqRef.of(instance);
  }

  /**
   * Upcasts this ArrayRef to an AnyRef.
   *
   * @return a new AnyRef containing this array reference
   */
  public AnyRef toAnyRef() {
    if (instance == null) {
      return AnyRef.nullRef();
    }
    return AnyRef.of(instance);
  }

  /**
   * Converts this ArrayRef to a WasmValue.
   *
   * @return a WasmValue containing this reference
   */
  public WasmValue toWasmValue() {
    if (instance == null) {
      return WasmValue.nullArrayRef();
    }
    return instance.toWasmValue();
  }

  /**
   * Tests equality with another ArrayRef using WebAssembly ref.eq semantics.
   *
   * @param other the other reference
   * @return true if references point to the same array instance
   */
  public boolean refEquals(final ArrayRef other) {
    if (other == null) {
      return false;
    }
    if (this.instance == null && other.instance == null) {
      return true;
    }
    if (this.instance == null || other.instance == null) {
      return false;
    }
    return this.instance.refEquals(other.instance);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ArrayRef)) {
      return false;
    }
    ArrayRef other = (ArrayRef) obj;
    return Objects.equals(instance, other.instance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instance);
  }

  @Override
  public String toString() {
    if (instance == null) {
      return "ArrayRef{null}";
    }
    return "ArrayRef{type=" + instance.getType() + ", length=" + instance.getLength()
        + ", id=" + id + "}";
  }
}

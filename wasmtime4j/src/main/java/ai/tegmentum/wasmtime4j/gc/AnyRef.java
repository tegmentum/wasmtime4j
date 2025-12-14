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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a WebAssembly {@code anyref} reference type.
 *
 * <p>In the WebAssembly GC proposal, {@code anyref} is the top type of the reference type
 * hierarchy. All GC reference types (eqref, structref, arrayref, i31ref) are subtypes of anyref.
 *
 * <p>An AnyRef can hold:
 *
 * <ul>
 *   <li>A null reference
 *   <li>An i31ref (immediate 31-bit integer)
 *   <li>A structref (reference to a struct instance)
 *   <li>An arrayref (reference to an array instance)
 *   <li>An externref (external reference from the host)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create an anyref from a struct
 * StructRef structRef = store.createStruct(structType);
 * AnyRef anyRef = AnyRef.of(structRef);
 *
 * // Check the underlying type
 * if (anyRef.isStruct()) {
 *     StructRef struct = anyRef.asStruct(store);
 * }
 *
 * // Create a null anyref
 * AnyRef nullRef = AnyRef.nullRef();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class AnyRef implements GcRef {

  private static final AtomicLong ID_COUNTER = new AtomicLong(0);

  private final GcObject value;
  private final long id;

  private AnyRef(final GcObject value) {
    this.value = value;
    this.id = ID_COUNTER.incrementAndGet();
  }

  /**
   * Creates an AnyRef from a GC object.
   *
   * @param value the GC object to wrap
   * @return a new AnyRef
   * @throws NullPointerException if value is null; use {@link #nullRef()} for null references
   */
  public static AnyRef of(final GcObject value) {
    Objects.requireNonNull(value, "value cannot be null; use nullRef() for null references");
    return new AnyRef(value);
  }

  /**
   * Creates an AnyRef from an EqRef.
   *
   * @param eqRef the equality-comparable reference
   * @return a new AnyRef
   * @throws NullPointerException if eqRef is null
   */
  public static AnyRef of(final EqRef eqRef) {
    Objects.requireNonNull(eqRef, "eqRef cannot be null");
    return new AnyRef(eqRef.getUnderlying());
  }

  /**
   * Creates an AnyRef from a StructRef.
   *
   * @param structRef the struct reference
   * @return a new AnyRef
   * @throws NullPointerException if structRef is null
   */
  public static AnyRef of(final StructRef structRef) {
    Objects.requireNonNull(structRef, "structRef cannot be null");
    return new AnyRef(structRef.getInstance());
  }

  /**
   * Creates an AnyRef from an ArrayRef.
   *
   * @param arrayRef the array reference
   * @return a new AnyRef
   * @throws NullPointerException if arrayRef is null
   */
  public static AnyRef of(final ArrayRef arrayRef) {
    Objects.requireNonNull(arrayRef, "arrayRef cannot be null");
    return new AnyRef(arrayRef.getInstance());
  }

  /**
   * Creates a null AnyRef.
   *
   * @return a null AnyRef
   */
  public static AnyRef nullRef() {
    return new AnyRef(null);
  }

  /**
   * Creates an AnyRef from a WasmValue.
   *
   * @param wasmValue the WasmValue containing a reference
   * @return a new AnyRef
   * @throws IllegalArgumentException if the WasmValue is not a reference type
   */
  public static AnyRef fromWasmValue(final WasmValue wasmValue) {
    Objects.requireNonNull(wasmValue, "wasmValue cannot be null");
    if (wasmValue.getValue() == null) {
      return nullRef();
    }
    Object raw = wasmValue.getValue();
    if (raw instanceof GcObject) {
      return new AnyRef((GcObject) raw);
    }
    throw new IllegalArgumentException("WasmValue does not contain a GC reference: " + wasmValue);
  }

  @Override
  public boolean isNull() {
    return value == null;
  }

  @Override
  public GcReferenceType getReferenceType() {
    return GcReferenceType.ANY_REF;
  }

  @Override
  public long getId() {
    return id;
  }

  /**
   * Gets the underlying GC object.
   *
   * @return the underlying object, or null if this is a null reference
   */
  public GcObject getUnderlying() {
    return value;
  }

  /**
   * Checks if this reference holds an i31 value.
   *
   * @return true if this is an i31ref
   */
  public boolean isI31() {
    return value != null && value.getReferenceType() == GcReferenceType.I31_REF;
  }

  /**
   * Checks if this reference holds a struct.
   *
   * @return true if this is a structref
   */
  public boolean isStruct() {
    return value != null && value.getReferenceType() == GcReferenceType.STRUCT_REF;
  }

  /**
   * Checks if this reference holds an array.
   *
   * @return true if this is an arrayref
   */
  public boolean isArray() {
    return value != null && value.getReferenceType() == GcReferenceType.ARRAY_REF;
  }

  /**
   * Checks if this reference is equality-comparable (eqref).
   *
   * @return true if this reference supports ref.eq
   */
  public boolean isEq() {
    return value != null && value.getReferenceType().supportsEquality();
  }

  /**
   * Gets this reference as an i31 value.
   *
   * @param store the store context
   * @return the i31 value
   * @throws IllegalStateException if this is not an i31ref
   */
  public int asI31(final Store store) {
    Objects.requireNonNull(store, "store cannot be null");
    if (!isI31()) {
      throw new IllegalStateException("AnyRef does not contain an i31ref");
    }
    return ((I31Instance) value).getValue();
  }

  /**
   * Gets this reference as a StructRef.
   *
   * @param store the store context
   * @return the struct reference
   * @throws IllegalStateException if this is not a structref
   */
  public StructRef asStruct(final Store store) {
    Objects.requireNonNull(store, "store cannot be null");
    if (!isStruct()) {
      throw new IllegalStateException("AnyRef does not contain a structref");
    }
    return StructRef.of((StructInstance) value);
  }

  /**
   * Gets this reference as an ArrayRef.
   *
   * @param store the store context
   * @return the array reference
   * @throws IllegalStateException if this is not an arrayref
   */
  public ArrayRef asArray(final Store store) {
    Objects.requireNonNull(store, "store cannot be null");
    if (!isArray()) {
      throw new IllegalStateException("AnyRef does not contain an arrayref");
    }
    return ArrayRef.of((ArrayInstance) value);
  }

  /**
   * Gets this reference as an EqRef if possible.
   *
   * @return an Optional containing the EqRef, or empty if not equality-comparable
   */
  public Optional<EqRef> asEq() {
    if (isEq()) {
      return Optional.of(EqRef.of(value));
    }
    return Optional.empty();
  }

  /**
   * Converts this AnyRef to a WasmValue.
   *
   * @return a WasmValue containing this reference
   */
  public WasmValue toWasmValue() {
    if (value == null) {
      return WasmValue.nullAnyRef();
    }
    return value.toWasmValue();
  }

  /**
   * Tests equality with another AnyRef using WebAssembly ref.eq semantics.
   *
   * @param other the other reference
   * @return true if references are equal
   */
  public boolean refEquals(final AnyRef other) {
    if (other == null) {
      return false;
    }
    if (this.value == null && other.value == null) {
      return true;
    }
    if (this.value == null || other.value == null) {
      return false;
    }
    return this.value.refEquals(other.value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AnyRef)) {
      return false;
    }
    AnyRef other = (AnyRef) obj;
    return Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    if (value == null) {
      return "AnyRef{null}";
    }
    return "AnyRef{" + value.getReferenceType().getWasmName() + ", id=" + id + "}";
  }
}

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
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a WebAssembly {@code eqref} reference type.
 *
 * <p>In the WebAssembly GC proposal, {@code eqref} represents references that support structural
 * equality testing via the {@code ref.eq} instruction. All eqrefs are subtypes of anyref.
 *
 * <p>An EqRef can hold:
 *
 * <ul>
 *   <li>A null reference
 *   <li>An i31ref (immediate 31-bit integer)
 *   <li>A structref (reference to a struct instance)
 *   <li>An arrayref (reference to an array instance)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create eqrefs from different types
 * EqRef fromStruct = EqRef.of(structInstance);
 * EqRef fromArray = EqRef.of(arrayInstance);
 * EqRef fromI31 = EqRef.ofI31(42);
 *
 * // Test equality
 * boolean equal = fromStruct.refEquals(anotherRef);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class EqRef implements GcRef {

  private static final AtomicLong ID_COUNTER = new AtomicLong(0);

  private final GcObject value;
  private final long id;

  private EqRef(final GcObject value) {
    this.value = value;
    this.id = ID_COUNTER.incrementAndGet();
  }

  /**
   * Creates an EqRef from a GC object.
   *
   * @param value the GC object to wrap (must support equality)
   * @return a new EqRef
   * @throws NullPointerException if value is null; use {@link #nullRef()} for null references
   * @throws IllegalArgumentException if value does not support equality
   */
  public static EqRef of(final GcObject value) {
    Objects.requireNonNull(value, "value cannot be null; use nullRef() for null references");
    if (!value.getReferenceType().supportsEquality()) {
      throw new IllegalArgumentException(
          "GC object does not support equality: " + value.getReferenceType());
    }
    return new EqRef(value);
  }

  /**
   * Creates an EqRef from a StructRef.
   *
   * @param structRef the struct reference
   * @return a new EqRef
   * @throws NullPointerException if structRef is null
   */
  public static EqRef of(final StructRef structRef) {
    Objects.requireNonNull(structRef, "structRef cannot be null");
    return new EqRef(structRef.getInstance());
  }

  /**
   * Creates an EqRef from an ArrayRef.
   *
   * @param arrayRef the array reference
   * @return a new EqRef
   * @throws NullPointerException if arrayRef is null
   */
  public static EqRef of(final ArrayRef arrayRef) {
    Objects.requireNonNull(arrayRef, "arrayRef cannot be null");
    return new EqRef(arrayRef.getInstance());
  }

  /**
   * Creates an EqRef from a struct instance.
   *
   * @param struct the struct instance
   * @return a new EqRef
   * @throws NullPointerException if struct is null
   */
  public static EqRef of(final StructInstance struct) {
    Objects.requireNonNull(struct, "struct cannot be null");
    return new EqRef(struct);
  }

  /**
   * Creates an EqRef from an array instance.
   *
   * @param array the array instance
   * @return a new EqRef
   * @throws NullPointerException if array is null
   */
  public static EqRef of(final ArrayInstance array) {
    Objects.requireNonNull(array, "array cannot be null");
    return new EqRef(array);
  }

  /**
   * Creates an EqRef containing an i31 value.
   *
   * @param i31Instance the i31 instance
   * @return a new EqRef
   * @throws NullPointerException if i31Instance is null
   */
  public static EqRef of(final I31Instance i31Instance) {
    Objects.requireNonNull(i31Instance, "i31Instance cannot be null");
    return new EqRef(i31Instance);
  }

  /**
   * Creates an EqRef from an i31 integer value.
   *
   * <p>This is a convenience factory that creates an {@link I31Instance} from the given integer
   * value and wraps it in an EqRef. The value must fit in 31 bits.
   *
   * @param value the integer value to wrap as an i31ref
   * @return a new EqRef containing the i31 value
   * @throws WasmException if I31 creation fails
   */
  public static EqRef ofI31(final int value) throws WasmException {
    I31Instance i31 = WasmRuntimeFactory.create().getGcRuntime().createI31(value);
    return new EqRef(i31);
  }

  /**
   * Creates a null EqRef.
   *
   * @return a null EqRef
   */
  public static EqRef nullRef() {
    return new EqRef(null);
  }

  @Override
  public boolean isNull() {
    return value == null;
  }

  @Override
  public GcReferenceType getReferenceType() {
    return GcReferenceType.EQ_REF;
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
   * Gets this reference as an i31 value.
   *
   * @param store the store context
   * @return the i31 value
   * @throws IllegalStateException if this is not an i31ref
   */
  public int asI31(final Store store) {
    Objects.requireNonNull(store, "store cannot be null");
    if (!isI31()) {
      throw new IllegalStateException("EqRef does not contain an i31ref");
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
      throw new IllegalStateException("EqRef does not contain a structref");
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
      throw new IllegalStateException("EqRef does not contain an arrayref");
    }
    return ArrayRef.of((ArrayInstance) value);
  }

  /**
   * Upcasts this EqRef to an AnyRef.
   *
   * @return a new AnyRef containing the same reference
   */
  public AnyRef toAnyRef() {
    if (value == null) {
      return AnyRef.nullRef();
    }
    return AnyRef.of(value);
  }

  /**
   * Converts this EqRef to a WasmValue.
   *
   * @return a WasmValue containing this reference
   */
  public WasmValue toWasmValue() {
    if (value == null) {
      return WasmValue.nullEqRef();
    }
    return value.toWasmValue();
  }

  /**
   * Tests equality with another EqRef using WebAssembly ref.eq semantics.
   *
   * @param other the other reference
   * @return true if references are equal
   */
  public boolean refEquals(final EqRef other) {
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
    if (!(obj instanceof EqRef)) {
      return false;
    }
    EqRef other = (EqRef) obj;
    return Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    if (value == null) {
      return "EqRef{null}";
    }
    return "EqRef{" + value.getReferenceType().getWasmName() + ", id=" + id + "}";
  }
}

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
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.type.HeapType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a WebAssembly {@code structref} reference type.
 *
 * <p>In the WebAssembly GC proposal, {@code structref} represents a reference to a struct instance.
 * Structs are composite types with named or indexed fields, each with a specific type and
 * mutability.
 *
 * <p>StructRef provides access to the underlying struct's fields and type information. All
 * structrefs are subtypes of both eqref and anyref.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a struct ref
 * StructRef ref = StructRef.of(structInstance);
 *
 * // Access fields
 * GcValue x = ref.getField(store, 0);
 * ref.setField(store, 0, GcValue.i32(42));
 *
 * // Get type information
 * StructType type = ref.getStructType(store);
 * int fieldCount = ref.getFieldCount(store);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class StructRef implements GcRef {

  private static final AtomicLong ID_COUNTER = new AtomicLong(0);

  private final StructInstance instance;
  private final long id;

  private StructRef(final StructInstance instance) {
    this.instance = instance;
    this.id = ID_COUNTER.incrementAndGet();
  }

  /**
   * Creates a StructRef from a struct instance.
   *
   * @param instance the struct instance
   * @return a new StructRef
   * @throws NullPointerException if instance is null; use {@link #nullRef()} for null references
   */
  public static StructRef of(final StructInstance instance) {
    Objects.requireNonNull(instance, "instance cannot be null; use nullRef() for null references");
    return new StructRef(instance);
  }

  /**
   * Creates a null StructRef.
   *
   * @return a null StructRef
   */
  public static StructRef nullRef() {
    return new StructRef(null);
  }

  @Override
  public boolean isNull() {
    return instance == null;
  }

  @Override
  public GcReferenceType getReferenceType() {
    return GcReferenceType.STRUCT_REF;
  }

  @Override
  public long getId() {
    return id;
  }

  /**
   * Gets the underlying struct instance.
   *
   * @return the struct instance, or null if this is a null reference
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification =
          "GC references require direct access to internal instance for WebAssembly" + " interop")
  public StructInstance getInstance() {
    return instance;
  }

  /**
   * Gets the struct type of the referenced struct.
   *
   * @param store the store context
   * @return the struct type
   * @throws IllegalStateException if this is a null reference
   */
  public StructType getStructType(final Store store) {
    if (instance == null) {
      throw new IllegalStateException("Cannot get type of null struct reference");
    }
    Objects.requireNonNull(store, "store cannot be null");
    return instance.getType();
  }

  /**
   * Gets the number of fields in the referenced struct.
   *
   * @param store the store context
   * @return the field count
   * @throws IllegalStateException if this is a null reference
   */
  public int getFieldCount(final Store store) {
    if (instance == null) {
      throw new IllegalStateException("Cannot get field count of null struct reference");
    }
    Objects.requireNonNull(store, "store cannot be null");
    return instance.getFieldCount();
  }

  /**
   * Gets a field value from the referenced struct.
   *
   * @param store the store context
   * @param index the field index
   * @return the field value
   * @throws IllegalStateException if this is a null reference
   * @throws IndexOutOfBoundsException if index is out of bounds
   * @throws GcException if field access fails
   */
  public GcValue getField(final Store store, final int index) throws GcException {
    if (instance == null) {
      throw new IllegalStateException("Cannot get field from null struct reference");
    }
    Objects.requireNonNull(store, "store cannot be null");
    return instance.getField(index);
  }

  /**
   * Gets all field values from the referenced struct.
   *
   * @param store the store context
   * @return an unmodifiable list of all field values
   * @throws IllegalStateException if this is a null reference
   * @throws GcException if field access fails
   */
  public List<GcValue> fields(final Store store) throws GcException {
    if (instance == null) {
      throw new IllegalStateException("Cannot get fields from null struct reference");
    }
    Objects.requireNonNull(store, "store cannot be null");
    final int count = instance.getFieldCount();
    final List<GcValue> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      result.add(instance.getField(i));
    }
    return Collections.unmodifiableList(result);
  }

  /**
   * Sets a field value in the referenced struct.
   *
   * @param store the store context
   * @param index the field index
   * @param value the new value
   * @throws IllegalStateException if this is a null reference
   * @throws IndexOutOfBoundsException if index is out of bounds
   * @throws IllegalArgumentException if field is immutable or value type is incompatible
   * @throws GcException if field assignment fails
   */
  public void setField(final Store store, final int index, final GcValue value) throws GcException {
    if (instance == null) {
      throw new IllegalStateException("Cannot set field on null struct reference");
    }
    Objects.requireNonNull(store, "store cannot be null");
    Objects.requireNonNull(value, "value cannot be null");
    instance.setField(index, value);
  }

  /**
   * Checks if this StructRef matches a given heap type using Wasmtime's subtype-aware type
   * checking.
   *
   * @param store the store context
   * @param heapType the heap type to check against
   * @return true if this reference matches the heap type
   * @throws IllegalStateException if this is a null reference
   * @throws GcException if the native type check fails
   * @since 1.1.0
   */
  public boolean matchesTy(final Store store, final HeapType heapType) throws GcException {
    Objects.requireNonNull(store, "store cannot be null");
    Objects.requireNonNull(heapType, "heapType cannot be null");
    if (instance == null) {
      throw new IllegalStateException("Cannot check type of null StructRef");
    }
    try {
      return WasmRuntimeFactory.create()
          .getGcRuntime()
          .structRefMatchesTy(instance.getObjectId(), heapType.ordinal());
    } catch (final ai.tegmentum.wasmtime4j.exception.WasmException e) {
      throw new GcException("Failed to check StructRef type match", e);
    }
  }

  /**
   * Upcasts this StructRef to an EqRef.
   *
   * @return a new EqRef containing this struct reference
   */
  public EqRef toEqRef() {
    if (instance == null) {
      return EqRef.nullRef();
    }
    return EqRef.of(instance);
  }

  /**
   * Upcasts this StructRef to an AnyRef.
   *
   * @return a new AnyRef containing this struct reference
   */
  public AnyRef toAnyRef() {
    if (instance == null) {
      return AnyRef.nullRef();
    }
    return AnyRef.of(instance);
  }

  /**
   * Converts this StructRef to a WasmValue.
   *
   * @return a WasmValue containing this reference
   */
  public WasmValue toWasmValue() {
    if (instance == null) {
      return WasmValue.nullStructRef();
    }
    return instance.toWasmValue();
  }

  /**
   * Tests equality with another StructRef using WebAssembly ref.eq semantics.
   *
   * @param other the other reference
   * @return true if references point to the same struct instance
   */
  public boolean refEquals(final StructRef other) {
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
    if (!(obj instanceof StructRef)) {
      return false;
    }
    StructRef other = (StructRef) obj;
    return Objects.equals(instance, other.instance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instance);
  }

  @Override
  public String toString() {
    if (instance == null) {
      return "StructRef{null}";
    }
    return "StructRef{type=" + instance.getType() + ", id=" + id + "}";
  }
}

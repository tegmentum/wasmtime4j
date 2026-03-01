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
package ai.tegmentum.wasmtime4j.type;

import java.util.Optional;

/**
 * Represents a WebAssembly reference type with nullability and heap type information.
 *
 * <p>Reference types in WebAssembly describe the kinds of references that can be stored in tables,
 * passed as function parameters, or returned from functions. Each reference type has a heap type
 * (describing what the reference points to) and a nullability flag.
 *
 * <p>Common predefined reference types are available as constants:
 *
 * <ul>
 *   <li>{@link #FUNCREF} - nullable function reference
 *   <li>{@link #EXTERNREF} - nullable external reference
 *   <li>{@link #ANYREF} - nullable any reference (GC proposal)
 *   <li>{@link #EQREF} - nullable equality-testable reference (GC proposal)
 *   <li>{@link #STRUCTREF} - nullable struct reference (GC proposal)
 *   <li>{@link #ARRAYREF} - nullable array reference (GC proposal)
 *   <li>{@link #I31REF} - nullable i31 reference (GC proposal)
 *   <li>{@link #EXNREF} - nullable exception reference (exception handling proposal)
 *   <li>{@link #NULLREF} - nullable null reference (bottom of any/eq hierarchy)
 *   <li>{@link #NULLFUNCREF} - nullable null function reference (bottom of func hierarchy)
 *   <li>{@link #NULLEXTERNREF} - nullable null external reference (bottom of extern hierarchy)
 *   <li>{@link #NULLEXNREF} - nullable null exception reference (bottom of exn hierarchy)
 *   <li>{@link #CONTREF} - nullable continuation reference (stack switching proposal)
 *   <li>{@link #NULLCONTREF} - nullable null continuation reference (bottom of cont hierarchy)
 * </ul>
 *
 * @since 1.1.0
 */
public interface RefType {

  /** Nullable function reference type. */
  RefType FUNCREF = DefaultRefType.create(true, HeapType.FUNC);

  /** Nullable external reference type. */
  RefType EXTERNREF = DefaultRefType.create(true, HeapType.EXTERN);

  /** Nullable any reference type (GC top type). */
  RefType ANYREF = DefaultRefType.create(true, HeapType.ANY);

  /** Nullable equality-testable reference type. */
  RefType EQREF = DefaultRefType.create(true, HeapType.EQ);

  /** Nullable struct reference type. */
  RefType STRUCTREF = DefaultRefType.create(true, HeapType.STRUCT);

  /** Nullable array reference type. */
  RefType ARRAYREF = DefaultRefType.create(true, HeapType.ARRAY);

  /** Nullable i31 reference type. */
  RefType I31REF = DefaultRefType.create(true, HeapType.I31);

  /** Nullable exception reference type (exception handling proposal). */
  RefType EXNREF = DefaultRefType.create(true, HeapType.EXN);

  /** Nullable null reference type - bottom of the any/eq hierarchy. */
  RefType NULLREF = DefaultRefType.create(true, HeapType.NONE);

  /** Nullable null function reference type - bottom of the func hierarchy. */
  RefType NULLFUNCREF = DefaultRefType.create(true, HeapType.NOFUNC);

  /** Nullable null external reference type - bottom of the extern hierarchy. */
  RefType NULLEXTERNREF = DefaultRefType.create(true, HeapType.NOEXTERN);

  /** Nullable null exception reference type - bottom of the exn hierarchy. */
  RefType NULLEXNREF = DefaultRefType.create(true, HeapType.NOEXN);

  /** Nullable continuation reference type (stack switching proposal). */
  RefType CONTREF = DefaultRefType.create(true, HeapType.CONT);

  /** Nullable null continuation reference type - bottom of the cont hierarchy. */
  RefType NULLCONTREF = DefaultRefType.create(true, HeapType.NOCONT);

  /**
   * Checks if this reference type is nullable.
   *
   * @return true if references of this type can be null
   */
  boolean isNullable();

  /**
   * Gets the heap type that this reference type points to.
   *
   * @return the heap type
   */
  HeapType getHeapType();

  /**
   * Checks if this reference type matches another according to subtyping rules.
   *
   * <p>A reference type matches another if its heap type is a subtype of the other's heap type and
   * its nullability is compatible (nullable can match nullable, non-nullable can match either).
   *
   * @param other the reference type to check against
   * @return true if this type matches the other
   */
  default boolean matches(final RefType other) {
    if (other == null) {
      return false;
    }
    // Non-nullable cannot match where nullable is not allowed
    if (isNullable() && !other.isNullable()) {
      return false;
    }
    return getHeapType().isSubtypeOf(other.getHeapType());
  }

  /**
   * Checks for precise type equality with another reference type.
   *
   * <p>Two reference types are equal if they have the same nullability and heap type.
   *
   * @param other the reference type to compare with
   * @return true if the types are exactly equal
   */
  default boolean eq(final RefType other) {
    if (other == null) {
      return false;
    }
    return isNullable() == other.isNullable() && getHeapType() == other.getHeapType();
  }

  /**
   * Gets the concrete heap type information, if this reference type has a concrete heap type.
   *
   * <p>For abstract heap types (func, extern, any, etc.), this returns empty. For concrete types
   * that reference a specific type definition in the module's type section, this returns the
   * concrete type information.
   *
   * @return the concrete heap type, or empty if this is an abstract reference type
   */
  default Optional<ConcreteHeapType> getConcreteHeapType() {
    return Optional.empty();
  }

  /**
   * Creates a reference type with the specified nullability and heap type.
   *
   * @param nullable whether the reference can be null
   * @param heapType the heap type this reference points to
   * @return a new RefType
   * @throws IllegalArgumentException if heapType is null
   */
  static RefType of(final boolean nullable, final HeapType heapType) {
    return DefaultRefType.create(nullable, heapType);
  }

  /**
   * Creates a non-nullable reference type for the specified heap type.
   *
   * @param heapType the heap type this reference points to
   * @return a new non-nullable RefType
   * @throws IllegalArgumentException if heapType is null
   */
  static RefType nonNull(final HeapType heapType) {
    return DefaultRefType.create(false, heapType);
  }
}

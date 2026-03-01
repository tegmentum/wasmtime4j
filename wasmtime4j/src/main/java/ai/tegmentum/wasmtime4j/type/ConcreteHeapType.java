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

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a concrete heap type from the GC proposal.
 *
 * <p>Concrete heap types reference specific user-defined types (struct, array, or function types)
 * in a module's type section. They carry a type index and optionally the resolved type definition.
 *
 * @since 1.0.0
 */
public final class ConcreteHeapType {

  private final HeapType abstractKind;
  private final int typeIndex;
  private final WasmType resolvedType;

  /**
   * Creates a new concrete heap type.
   *
   * @param abstractKind the abstract kind this concrete type belongs to (e.g., FUNC, STRUCT, ARRAY)
   * @param typeIndex the type index in the module's type section
   * @param resolvedType the resolved type definition, or null if not yet resolved
   * @throws IllegalArgumentException if abstractKind is null or typeIndex is negative
   */
  public ConcreteHeapType(
      final HeapType abstractKind, final int typeIndex, final WasmType resolvedType) {
    if (abstractKind == null) {
      throw new IllegalArgumentException("abstractKind cannot be null");
    }
    if (typeIndex < 0) {
      throw new IllegalArgumentException("typeIndex cannot be negative: " + typeIndex);
    }
    this.abstractKind = abstractKind;
    this.typeIndex = typeIndex;
    this.resolvedType = resolvedType;
  }

  /**
   * Creates a concrete heap type without a resolved type.
   *
   * @param abstractKind the abstract kind this concrete type belongs to
   * @param typeIndex the type index in the module's type section
   * @return the new concrete heap type
   */
  public static ConcreteHeapType of(final HeapType abstractKind, final int typeIndex) {
    return new ConcreteHeapType(abstractKind, typeIndex, null);
  }

  /**
   * Creates a concrete heap type with a resolved type.
   *
   * @param abstractKind the abstract kind this concrete type belongs to
   * @param typeIndex the type index in the module's type section
   * @param resolvedType the resolved type definition
   * @return the new concrete heap type
   */
  public static ConcreteHeapType of(
      final HeapType abstractKind, final int typeIndex, final WasmType resolvedType) {
    return new ConcreteHeapType(abstractKind, typeIndex, resolvedType);
  }

  /**
   * Gets the abstract kind this concrete type belongs to.
   *
   * @return the abstract heap type kind (e.g., FUNC, STRUCT, ARRAY)
   */
  public HeapType getAbstractKind() {
    return abstractKind;
  }

  /**
   * Gets the type index in the module's type section.
   *
   * @return the type index
   */
  public int getTypeIndex() {
    return typeIndex;
  }

  /**
   * Gets the resolved type definition, if available.
   *
   * @return the resolved type, or empty if not yet resolved
   */
  public Optional<WasmType> getResolvedType() {
    return Optional.ofNullable(resolvedType);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ConcreteHeapType)) {
      return false;
    }
    final ConcreteHeapType other = (ConcreteHeapType) obj;
    return abstractKind == other.abstractKind && typeIndex == other.typeIndex;
  }

  @Override
  public int hashCode() {
    return Objects.hash(abstractKind, typeIndex);
  }

  @Override
  public String toString() {
    return "ConcreteHeapType{kind=" + abstractKind + ", index=" + typeIndex + "}";
  }
}

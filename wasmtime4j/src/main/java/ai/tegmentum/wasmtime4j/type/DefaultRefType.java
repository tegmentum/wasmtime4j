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

/**
 * Default implementation of {@link RefType}.
 *
 * <p>This is a simple value-type implementation that stores the nullability flag and heap type.
 *
 * @since 1.1.0
 */
final class DefaultRefType implements RefType {

  private final boolean nullable;
  private final HeapType heapType;

  private DefaultRefType(final boolean nullable, final HeapType heapType) {
    this.nullable = nullable;
    this.heapType = heapType;
  }

  /**
   * Creates a new DefaultRefType.
   *
   * @param nullable whether the reference can be null
   * @param heapType the heap type
   * @return a new DefaultRefType
   * @throws IllegalArgumentException if heapType is null
   */
  static DefaultRefType create(final boolean nullable, final HeapType heapType) {
    if (heapType == null) {
      throw new IllegalArgumentException("heapType cannot be null");
    }
    return new DefaultRefType(nullable, heapType);
  }

  @Override
  public boolean isNullable() {
    return nullable;
  }

  @Override
  public HeapType getHeapType() {
    return heapType;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RefType)) {
      return false;
    }
    final RefType other = (RefType) obj;
    return this.nullable == other.isNullable() && this.heapType == other.getHeapType();
  }

  @Override
  public int hashCode() {
    return Objects.hash(nullable, heapType);
  }

  @Override
  public String toString() {
    final String prefix = nullable ? "(ref null " : "(ref ";
    return prefix + heapType.getWasmName() + ")";
  }
}

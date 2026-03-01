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

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Default implementation of {@link TableType}.
 *
 * <p>This is a simple value-type implementation for use in the API module when no native handle is
 * needed.
 *
 * @since 1.1.0
 */
final class DefaultTableType implements TableType {

  private final WasmValueType elementType;
  private final long minimum;
  private final Long maximum;
  private final boolean is64;

  private DefaultTableType(
      final WasmValueType elementType, final long minimum, final Long maximum, final boolean is64) {
    this.elementType = elementType;
    this.minimum = minimum;
    this.maximum = maximum;
    this.is64 = is64;
  }

  /**
   * Creates a new DefaultTableType.
   *
   * @param elementType the element type
   * @param min the minimum size
   * @param max the maximum size
   * @return a new DefaultTableType
   * @throws IllegalArgumentException if elementType is null or min is negative
   */
  static DefaultTableType create(
      final WasmValueType elementType, final long min, final OptionalLong max) {
    if (elementType == null) {
      throw new IllegalArgumentException("elementType cannot be null");
    }
    if (min < 0) {
      throw new IllegalArgumentException("min cannot be negative");
    }
    if (max == null) {
      throw new IllegalArgumentException("max cannot be null; use OptionalLong.empty()");
    }
    final Long maxValue = max.isPresent() ? max.getAsLong() : null;
    return new DefaultTableType(elementType, min, maxValue, false);
  }

  /**
   * Creates a new DefaultTableType with 64-bit indices.
   *
   * @param elementType the element type
   * @param min the minimum size
   * @param max the maximum size
   * @return a new 64-bit DefaultTableType
   * @throws IllegalArgumentException if elementType is null or min is negative
   */
  static DefaultTableType create64(
      final WasmValueType elementType, final long min, final OptionalLong max) {
    if (elementType == null) {
      throw new IllegalArgumentException("elementType cannot be null");
    }
    if (min < 0) {
      throw new IllegalArgumentException("min cannot be negative");
    }
    if (max == null) {
      throw new IllegalArgumentException("max cannot be null; use OptionalLong.empty()");
    }
    final Long maxValue = max.isPresent() ? max.getAsLong() : null;
    return new DefaultTableType(elementType, min, maxValue, true);
  }

  @Override
  public WasmValueType getElementType() {
    return elementType;
  }

  @Override
  public long getMinimum() {
    return minimum;
  }

  @Override
  public Optional<Long> getMaximum() {
    return Optional.ofNullable(maximum);
  }

  @Override
  public boolean is64Bit() {
    return is64;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TableType)) {
      return false;
    }
    final TableType other = (TableType) obj;
    return this.elementType == other.getElementType()
        && this.minimum == other.getMinimum()
        && Objects.equals(getMaximum(), other.getMaximum())
        && this.is64 == other.is64Bit();
  }

  @Override
  public int hashCode() {
    return Objects.hash(elementType, minimum, maximum, is64);
  }

  @Override
  public String toString() {
    final String suffix = is64 ? ", 64-bit" : "";
    if (maximum != null) {
      return "TableType(" + elementType + ", " + minimum + ", " + maximum + suffix + ")";
    }
    return "TableType(" + elementType + ", " + minimum + suffix + ")";
  }
}

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
 * Default implementation of {@link MemoryType}.
 *
 * <p>This is a simple value-type implementation for use in the API module when no native handle is
 * needed.
 *
 * @since 1.1.0
 */
final class DefaultMemoryType implements MemoryType {

  private final long minimum;
  private final Long maximum;
  private final boolean is64Bit;
  private final boolean shared;
  private final long pageSize;

  DefaultMemoryType(
      final long minimum,
      final Long maximum,
      final boolean is64Bit,
      final boolean shared,
      final long pageSize) {
    this.minimum = minimum;
    this.maximum = maximum;
    this.is64Bit = is64Bit;
    this.shared = shared;
    this.pageSize = pageSize;
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
    return is64Bit;
  }

  @Override
  public boolean isShared() {
    return shared;
  }

  @Override
  public long getPageSize() {
    return pageSize;
  }

  @Override
  public int getPageSizeLog2() {
    return Long.numberOfTrailingZeros(pageSize);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MemoryType)) {
      return false;
    }
    final MemoryType other = (MemoryType) obj;
    return this.minimum == other.getMinimum()
        && Objects.equals(getMaximum(), other.getMaximum())
        && this.is64Bit == other.is64Bit()
        && this.shared == other.isShared()
        && this.pageSize == other.getPageSize();
  }

  @Override
  public int hashCode() {
    return Objects.hash(minimum, maximum, is64Bit, shared, pageSize);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("MemoryType(min=");
    sb.append(minimum);
    if (maximum != null) {
      sb.append(", max=").append(maximum);
    }
    if (is64Bit) {
      sb.append(", 64-bit");
    }
    if (shared) {
      sb.append(", shared");
    }
    if (pageSize != 65536L) {
      sb.append(", pageSize=").append(pageSize);
    }
    sb.append(")");
    return sb.toString();
  }
}

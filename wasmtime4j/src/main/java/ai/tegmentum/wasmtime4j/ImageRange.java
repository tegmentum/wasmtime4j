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
package ai.tegmentum.wasmtime4j;

import java.util.Objects;

/**
 * Represents the memory address range of a compiled WebAssembly image.
 *
 * <p>This corresponds to the native memory range ({@code Range<*const u8>}) returned by Wasmtime's
 * {@code Module::image_range()} and {@code Component::image_range()} methods. The range describes
 * where the compiled machine code lives in the process's virtual address space.
 *
 * <p>The start and end values are native memory addresses represented as unsigned 64-bit values.
 *
 * @since 1.1.0
 */
public final class ImageRange {

  private final long start;
  private final long end;

  /**
   * Creates a new ImageRange.
   *
   * @param start the start address of the image range (inclusive)
   * @param end the end address of the image range (exclusive)
   */
  public ImageRange(final long start, final long end) {
    this.start = start;
    this.end = end;
  }

  /**
   * Gets the start address of the image range (inclusive).
   *
   * @return the start address
   */
  public long getStart() {
    return start;
  }

  /**
   * Gets the end address of the image range (exclusive).
   *
   * @return the end address
   */
  public long getEnd() {
    return end;
  }

  /**
   * Gets the size of the image range in bytes.
   *
   * @return the size in bytes (end - start)
   */
  public long getSize() {
    return end - start;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ImageRange)) {
      return false;
    }
    ImageRange other = (ImageRange) obj;
    return start == other.start && end == other.end;
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    return "ImageRange{start=0x"
        + Long.toHexString(start)
        + ", end=0x"
        + Long.toHexString(end)
        + ", size="
        + getSize()
        + "}";
  }
}

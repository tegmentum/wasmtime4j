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
package ai.tegmentum.wasmtime4j.coredump;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a memory snapshot in a WebAssembly core dump.
 *
 * @since 1.0.0
 */
public final class CoreDumpMemory {

  /** Number of bytes per WebAssembly memory page. */
  private static final long PAGE_SIZE = 65536L;

  private final int instanceIndex;
  private final int memoryIndex;
  private final String name;
  private final long sizeInPages;
  private final boolean memory64;
  private final long minPages;
  private final Long maxPages;
  private final List<MemorySegment> segments;

  private CoreDumpMemory(final Builder builder) {
    this.instanceIndex = builder.instanceIndex;
    this.memoryIndex = builder.memoryIndex;
    this.name = builder.name;
    this.sizeInPages = builder.sizeInPages;
    this.memory64 = builder.memory64;
    this.minPages = builder.minPages;
    this.maxPages = builder.maxPages;
    this.segments = Collections.unmodifiableList(new ArrayList<>(builder.segments));
  }

  /**
   * Creates a new builder for constructing a CoreDumpMemory.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public int getInstanceIndex() {
    return instanceIndex;
  }

  public int getMemoryIndex() {
    return memoryIndex;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public long getSizeInPages() {
    return sizeInPages;
  }

  public long getSizeInBytes() {
    return sizeInPages * PAGE_SIZE;
  }

  public boolean isMemory64() {
    return memory64;
  }

  public long getMinPages() {
    return minPages;
  }

  public Optional<Long> getMaxPages() {
    return Optional.ofNullable(maxPages);
  }

  public List<MemorySegment> getSegments() {
    return segments;
  }

  /**
   * Reads a range of bytes from this memory dump.
   *
   * @param offset the byte offset to start reading from
   * @param length the number of bytes to read
   * @return a byte array containing the requested range
   * @throws IndexOutOfBoundsException if offset or length is negative, or range exceeds memory
   *     bounds
   */
  public byte[] read(final long offset, final int length) {
    if (offset < 0 || length < 0) {
      throw new IndexOutOfBoundsException("Offset and length must be non-negative");
    }
    final long endOffset = offset + length;
    if (endOffset > getSizeInBytes()) {
      throw new IndexOutOfBoundsException(
          "Read range exceeds memory bounds: " + endOffset + " > " + getSizeInBytes());
    }

    final byte[] result = new byte[length];

    for (final MemorySegment segment : segments) {
      final long segmentStart = segment.getOffset();
      final long segmentEnd = segmentStart + segment.getSize();

      // Check if this segment overlaps with the requested range
      if (segmentEnd > offset && segmentStart < endOffset) {
        final long overlapStart = Math.max(offset, segmentStart);
        final long overlapEnd = Math.min(endOffset, segmentEnd);
        final int srcOffset = (int) (overlapStart - segmentStart);
        final int dstOffset = (int) (overlapStart - offset);
        final int copyLength = (int) (overlapEnd - overlapStart);

        System.arraycopy(segment.getData(), srcOffset, result, dstOffset, copyLength);
      }
    }

    return result;
  }

  @Override
  public String toString() {
    return "CoreDumpMemory{"
        + "instanceIndex="
        + instanceIndex
        + ", memoryIndex="
        + memoryIndex
        + ", name='"
        + name
        + '\''
        + ", sizeInPages="
        + sizeInPages
        + ", memory64="
        + memory64
        + ", segments="
        + segments.size()
        + '}';
  }

  /** Represents a contiguous segment of non-zero memory data. */
  public static final class MemorySegment {

    private final long offset;
    private final byte[] data;

    /**
     * Creates a new memory segment.
     *
     * @param offset the offset in bytes
     * @param data the segment data
     */
    public MemorySegment(final long offset, final byte[] data) {
      if (offset < 0) {
        throw new IllegalArgumentException("Offset must be non-negative");
      }
      this.offset = offset;
      this.data = Objects.requireNonNull(data, "Data cannot be null").clone();
    }

    public long getOffset() {
      return offset;
    }

    public byte[] getData() {
      return data.clone();
    }

    public int getSize() {
      return data.length;
    }

    @Override
    public String toString() {
      return "MemorySegment{offset=" + offset + ", size=" + data.length + '}';
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final MemorySegment that = (MemorySegment) o;
      return offset == that.offset && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
      int result = Long.hashCode(offset);
      result = 31 * result + Arrays.hashCode(data);
      return result;
    }
  }

  /** Builder for constructing {@link CoreDumpMemory} instances. */
  public static final class Builder {

    private int instanceIndex;
    private int memoryIndex;
    private String name;
    private long sizeInPages;
    private boolean memory64;
    private long minPages;
    private Long maxPages;
    private final List<MemorySegment> segments = new ArrayList<>();

    private Builder() {}

    public Builder instanceIndex(final int instanceIndex) {
      this.instanceIndex = instanceIndex;
      return this;
    }

    public Builder memoryIndex(final int memoryIndex) {
      this.memoryIndex = memoryIndex;
      return this;
    }

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public Builder sizeInPages(final long sizeInPages) {
      this.sizeInPages = sizeInPages;
      return this;
    }

    public Builder memory64(final boolean memory64) {
      this.memory64 = memory64;
      return this;
    }

    public Builder minPages(final long minPages) {
      this.minPages = minPages;
      return this;
    }

    public Builder maxPages(final Long maxPages) {
      this.maxPages = maxPages;
      return this;
    }

    /**
     * Adds a memory segment to this memory dump.
     *
     * @param segment the memory segment to add
     * @return this builder
     */
    public Builder addSegment(final MemorySegment segment) {
      Objects.requireNonNull(segment, "Segment cannot be null");
      this.segments.add(segment);
      return this;
    }

    public Builder addSegment(final long offset, final byte[] data) {
      return addSegment(new MemorySegment(offset, data));
    }

    /**
     * Adds multiple memory segments to this memory dump.
     *
     * @param segments the list of memory segments to add
     * @return this builder
     */
    public Builder addSegments(final List<MemorySegment> segments) {
      Objects.requireNonNull(segments, "Segments cannot be null");
      this.segments.addAll(segments);
      return this;
    }

    public CoreDumpMemory build() {
      return new CoreDumpMemory(this);
    }
  }
}

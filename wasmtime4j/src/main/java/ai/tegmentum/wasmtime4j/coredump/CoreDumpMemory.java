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

import java.util.List;
import java.util.Optional;

/**
 * Represents a memory snapshot captured in a WebAssembly coredump.
 *
 * <p>Memory snapshots contain the contents of WebAssembly linear memory at the time of the trap. To
 * reduce coredump size, only non-zero memory regions are typically captured.
 *
 * @since 1.0.0
 */
public interface CoreDumpMemory {

  /**
   * Returns the index of the instance containing this memory.
   *
   * @return the instance index
   */
  int getInstanceIndex();

  /**
   * Returns the index of this memory within its instance.
   *
   * @return the memory index
   */
  int getMemoryIndex();

  /**
   * Returns the name of this memory, if available.
   *
   * @return an Optional containing the memory name, or empty if not available
   */
  Optional<String> getName();

  /**
   * Returns the current size of this memory in pages.
   *
   * <p>Each page is 64KB (65536 bytes).
   *
   * @return the size in pages
   */
  long getSizeInPages();

  /**
   * Returns the current size of this memory in bytes.
   *
   * @return the size in bytes
   */
  long getSizeInBytes();

  /**
   * Returns whether this is a 64-bit memory.
   *
   * @return true if this is a memory64 memory
   */
  boolean isMemory64();

  /**
   * Returns the minimum size of this memory in pages.
   *
   * @return the minimum size in pages
   */
  long getMinPages();

  /**
   * Returns the maximum size of this memory in pages, if specified.
   *
   * @return an Optional containing the maximum size, or empty if unbounded
   */
  Optional<Long> getMaxPages();

  /**
   * Returns the list of memory segments containing non-zero data.
   *
   * <p>Segments are ordered by their starting address and do not overlap.
   *
   * @return an unmodifiable list of memory segments
   */
  List<MemorySegment> getSegments();

  /**
   * Reads a range of bytes from this memory snapshot.
   *
   * @param offset the starting offset in bytes
   * @param length the number of bytes to read
   * @return the bytes read from memory
   * @throws IndexOutOfBoundsException if the range exceeds memory bounds
   */
  byte[] read(long offset, int length);

  /** Represents a contiguous segment of non-zero memory data. */
  interface MemorySegment {

    /**
     * Returns the starting address of this segment.
     *
     * @return the offset in bytes from the start of memory
     */
    long getOffset();

    /**
     * Returns the data contained in this segment.
     *
     * @return the segment data
     */
    byte[] getData();

    /**
     * Returns the size of this segment in bytes.
     *
     * @return the segment size
     */
    default int getSize() {
      return getData().length;
    }
  }
}

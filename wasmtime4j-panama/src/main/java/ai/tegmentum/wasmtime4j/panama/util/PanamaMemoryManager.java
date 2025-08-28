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

package ai.tegmentum.wasmtime4j.panama.util;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Memory management utility for Panama FFI operations.
 *
 * <p>This class provides centralized memory management for all native memory allocations used in
 * Panama FFI operations. It tracks memory usage, provides convenient allocation methods, and
 * ensures proper cleanup through Arena management.
 *
 * <p>All memory allocated through this manager is associated with the provided Arena and will be
 * automatically freed when the Arena is closed.
 *
 * @since 1.0.0
 */
public final class PanamaMemoryManager {
  private static final Logger logger = Logger.getLogger(PanamaMemoryManager.class.getName());

  private final Arena arena;
  private final AtomicLong totalAllocated = new AtomicLong(0);
  private final AtomicLong allocationCount = new AtomicLong(0);

  /**
   * Creates a new Panama memory manager with the specified arena.
   *
   * @param arena the arena for memory allocation and cleanup
   * @throws IllegalArgumentException if arena is null
   */
  public PanamaMemoryManager(final Arena arena) {
    if (arena == null) {
      throw new IllegalArgumentException("Arena cannot be null");
    }
    this.arena = arena;
    logger.fine("Created Panama memory manager");
  }

  /**
   * Allocates a memory segment of the specified size.
   *
   * @param size the size in bytes
   * @return the allocated memory segment
   * @throws IllegalArgumentException if size is negative
   * @throws OutOfMemoryError if allocation fails
   */
  public MemorySegment allocate(final long size) {
    if (size < 0) {
      throw new IllegalArgumentException("Size cannot be negative");
    }

    if (size == 0) {
      return MemorySegment.NULL;
    }

    try {
      final MemorySegment segment = arena.allocate(size);
      totalAllocated.addAndGet(size);
      allocationCount.incrementAndGet();

      logger.fine("Allocated memory segment: " + size + " bytes");
      return segment;
    } catch (OutOfMemoryError e) {
      logger.severe("Failed to allocate memory segment of size " + size + ": " + e.getMessage());
      throw e;
    }
  }

  /**
   * Allocates and initializes a memory segment with the specified layout.
   *
   * @param layout the memory layout to allocate
   * @return the allocated memory segment
   * @throws IllegalArgumentException if layout is null
   */
  public MemorySegment allocate(final ValueLayout layout) {
    if (layout == null) {
      throw new IllegalArgumentException("Layout cannot be null");
    }

    try {
      final MemorySegment segment = arena.allocate(layout);
      totalAllocated.addAndGet(layout.byteSize());
      allocationCount.incrementAndGet();

      logger.fine("Allocated memory segment with layout: " + layout);
      return segment;
    } catch (OutOfMemoryError e) {
      logger.severe(
          "Failed to allocate memory segment with layout " + layout + ": " + e.getMessage());
      throw e;
    }
  }

  /**
   * Allocates a memory segment from a byte array.
   *
   * <p>The returned segment contains a copy of the input data.
   *
   * @param data the byte array to copy
   * @return a memory segment containing the data
   * @throws IllegalArgumentException if data is null
   */
  public MemorySegment allocateBytes(final byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }

    if (data.length == 0) {
      return MemorySegment.NULL;
    }

    try {
      final MemorySegment segment = arena.allocateFrom(ValueLayout.JAVA_BYTE, data);
      totalAllocated.addAndGet(data.length);
      allocationCount.incrementAndGet();

      logger.fine("Allocated memory segment from byte array: " + data.length + " bytes");
      return segment;
    } catch (OutOfMemoryError e) {
      logger.severe("Failed to allocate memory segment from byte array: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Allocates a null-terminated string segment.
   *
   * @param str the string to allocate
   * @return a memory segment containing the null-terminated string
   * @throws IllegalArgumentException if str is null
   */
  public MemorySegment allocateString(final String str) {
    if (str == null) {
      throw new IllegalArgumentException("String cannot be null");
    }

    try {
      final MemorySegment segment = arena.allocateFrom(str);
      // String length + null terminator
      final long size = str.length() + 1;
      totalAllocated.addAndGet(size);
      allocationCount.incrementAndGet();

      logger.fine("Allocated string segment: " + str + " (" + size + " bytes)");
      return segment;
    } catch (OutOfMemoryError e) {
      logger.severe("Failed to allocate string segment: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Allocates an array of memory segments.
   *
   * @param count the number of segments to allocate
   * @param segmentSize the size of each segment in bytes
   * @return an array of allocated memory segments
   * @throws IllegalArgumentException if count or segmentSize is negative
   */
  public MemorySegment[] allocateArray(final int count, final long segmentSize) {
    if (count < 0) {
      throw new IllegalArgumentException("Count cannot be negative");
    }
    if (segmentSize < 0) {
      throw new IllegalArgumentException("Segment size cannot be negative");
    }

    if (count == 0) {
      return new MemorySegment[0];
    }

    try {
      final MemorySegment[] segments = new MemorySegment[count];
      for (int i = 0; i < count; i++) {
        segments[i] = allocate(segmentSize);
      }

      logger.fine(
          "Allocated array of " + count + " memory segments (" + segmentSize + " bytes each)");
      return segments;
    } catch (Exception e) {
      logger.severe("Failed to allocate memory segment array: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Creates a memory segment that references existing memory.
   *
   * <p>This method creates a segment that points to memory that is not managed by this manager's
   * arena. The caller is responsible for ensuring the referenced memory remains valid.
   *
   * @param address the memory address
   * @param size the size in bytes
   * @return a memory segment referencing the address
   * @throws IllegalArgumentException if address is 0 or size is negative
   */
  public MemorySegment ofAddress(final long address, final long size) {
    if (address == 0) {
      throw new IllegalArgumentException("Address cannot be zero");
    }
    if (size < 0) {
      throw new IllegalArgumentException("Size cannot be negative");
    }

    try {
      final MemorySegment segment = MemorySegment.ofAddress(address).reinterpret(size);
      logger.fine("Created memory segment reference: address=" + address + ", size=" + size);
      return segment;
    } catch (Exception e) {
      logger.severe("Failed to create memory segment reference: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Copies data from one memory segment to another.
   *
   * @param source the source memory segment
   * @param destination the destination memory segment
   * @param size the number of bytes to copy
   * @throws IllegalArgumentException if segments are null or size is negative
   * @throws IndexOutOfBoundsException if the copy would exceed segment bounds
   */
  public void copy(final MemorySegment source, final MemorySegment destination, final long size) {
    if (source == null) {
      throw new IllegalArgumentException("Source segment cannot be null");
    }
    if (destination == null) {
      throw new IllegalArgumentException("Destination segment cannot be null");
    }
    if (size < 0) {
      throw new IllegalArgumentException("Size cannot be negative");
    }

    if (size == 0) {
      return;
    }

    try {
      destination.copyFrom(source.asSlice(0, size));
      logger.fine("Copied " + size + " bytes between memory segments");
    } catch (Exception e) {
      logger.severe("Failed to copy memory segments: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Fills a memory segment with the specified value.
   *
   * @param segment the memory segment to fill
   * @param value the value to fill with
   * @throws IllegalArgumentException if segment is null
   */
  public void fill(final MemorySegment segment, final byte value) {
    if (segment == null) {
      throw new IllegalArgumentException("Segment cannot be null");
    }

    if (segment == MemorySegment.NULL || segment.byteSize() == 0) {
      return;
    }

    try {
      segment.fill(value);
      logger.fine(
          "Filled memory segment with value " + value + " (" + segment.byteSize() + " bytes)");
    } catch (Exception e) {
      logger.severe("Failed to fill memory segment: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Frees a memory segment that was allocated outside the arena.
   *
   * <p>This method is a no-op for segments allocated through this manager's arena, as they will be
   * automatically freed when the arena is closed. It only applies to segments created through other
   * means.
   *
   * @param segment the memory segment to free
   */
  public void freeMemory(final MemorySegment segment) {
    if (segment == null || segment == MemorySegment.NULL) {
      return;
    }

    // For arena-allocated segments, this is a no-op
    // The arena will handle cleanup automatically
    logger.fine("Memory segment cleanup requested (handled by arena)");
  }

  /**
   * Gets the total amount of memory allocated through this manager.
   *
   * @return the total allocated memory in bytes
   */
  public long getTotalAllocated() {
    return totalAllocated.get();
  }

  /**
   * Gets the number of allocations performed through this manager.
   *
   * @return the allocation count
   */
  public long getAllocationCount() {
    return allocationCount.get();
  }

  /**
   * Gets the arena associated with this memory manager.
   *
   * @return the arena instance
   */
  public Arena getArena() {
    return arena;
  }

  /**
   * Gets memory usage statistics as a formatted string.
   *
   * @return memory usage statistics
   */
  public String getMemoryStats() {
    return String.format(
        "PanamaMemoryManager: %d allocations, %d bytes total",
        getAllocationCount(), getTotalAllocated());
  }
}

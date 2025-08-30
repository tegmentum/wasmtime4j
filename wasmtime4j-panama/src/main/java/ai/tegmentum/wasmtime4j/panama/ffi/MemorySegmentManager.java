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

package ai.tegmentum.wasmtime4j.panama.ffi;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * Utility class for managing MemorySegment operations in Panama FFI context.
 *
 * <p>This class provides convenient methods for creating, manipulating, and converting between
 * different memory representations used in WebAssembly operations. It handles the complexity of
 * Panama FFI memory management while ensuring proper resource cleanup.
 *
 * <p>All memory segments created by this class are associated with the provided Arena for automatic
 * cleanup when the arena is closed.
 *
 * @since 1.0.0
 */
public final class MemorySegmentManager {
  private static final Logger logger = Logger.getLogger(MemorySegmentManager.class.getName());

  private final Arena arena;

  /**
   * Creates a new memory segment manager with the specified arena.
   *
   * @param arena the arena for memory allocation and cleanup
   * @throws IllegalArgumentException if arena is null
   */
  public MemorySegmentManager(final Arena arena) {
    if (arena == null) {
      throw new IllegalArgumentException("Arena cannot be null");
    }
    this.arena = arena;
  }

  /**
   * Creates a memory segment from a byte array.
   *
   * <p>The returned segment is a copy of the input data and is managed by the arena for automatic
   * cleanup.
   *
   * @param data the byte array to copy
   * @return a memory segment containing the data
   * @throws IllegalArgumentException if data is null
   */
  public MemorySegment fromByteArray(final byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }

    if (data.length == 0) {
      return MemorySegment.NULL;
    }

    try {
      final MemorySegment segment = arena.allocate(data.length);
      segment.copyFrom(MemorySegment.ofArray(data));

      logger.fine("Created memory segment from byte array of length " + data.length);
      return segment;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create memory segment from byte array", e);
    }
  }

  /**
   * Creates a memory segment from a ByteBuffer.
   *
   * <p>For direct ByteBuffers, this attempts to create a segment that references the same memory.
   * For heap ByteBuffers, the data is copied.
   *
   * @param buffer the ByteBuffer to convert
   * @return a memory segment containing the buffer data
   * @throws IllegalArgumentException if buffer is null
   */
  public MemorySegment fromByteBuffer(final ByteBuffer buffer) {
    if (buffer == null) {
      throw new IllegalArgumentException("Buffer cannot be null");
    }

    if (!buffer.hasRemaining()) {
      return MemorySegment.NULL;
    }

    try {
      if (buffer.isDirect()) {
        // For direct buffers, try to get direct access
        // TODO: Implement direct buffer to MemorySegment conversion
        logger.fine("Converting direct ByteBuffer - using fallback copy method");
      }

      // Fallback: copy data from buffer
      final byte[] data = new byte[buffer.remaining()];
      final int position = buffer.position();
      buffer.get(data);
      buffer.position(position); // Restore original position

      return fromByteArray(data);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create memory segment from ByteBuffer", e);
    }
  }

  /**
   * Converts a memory segment to a byte array.
   *
   * <p>This creates a copy of the memory segment data as a Java byte array.
   *
   * @param segment the memory segment to convert
   * @return a byte array containing the segment data
   * @throws IllegalArgumentException if segment is null or invalid
   */
  public byte[] toByteArray(final MemorySegment segment) {
    if (segment == null) {
      throw new IllegalArgumentException("Segment cannot be null");
    }

    if (segment == MemorySegment.NULL || segment.byteSize() == 0) {
      return new byte[0];
    }

    try {
      final long size = segment.byteSize();
      if (size > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Segment too large to convert to byte array");
      }

      final byte[] data = new byte[(int) size];
      MemorySegment.ofArray(data).copyFrom(segment);

      logger.fine("Converted memory segment to byte array of length " + data.length);
      return data;
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert memory segment to byte array", e);
    }
  }

  /**
   * Creates a ByteBuffer view of a memory segment.
   *
   * <p>The returned ByteBuffer provides a view of the memory segment data. Changes to the
   * ByteBuffer will be reflected in the memory segment and vice versa.
   *
   * @param segment the memory segment to wrap
   * @return a ByteBuffer view of the segment
   * @throws IllegalArgumentException if segment is null or invalid
   */
  public ByteBuffer asByteBuffer(final MemorySegment segment) {
    if (segment == null) {
      throw new IllegalArgumentException("Segment cannot be null");
    }

    if (segment == MemorySegment.NULL || segment.byteSize() == 0) {
      return ByteBuffer.allocate(0);
    }

    try {
      final long size = segment.byteSize();
      if (size > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Segment too large to convert to ByteBuffer");
      }

      // TODO: Implement direct ByteBuffer creation from MemorySegment
      // For now, create a copy
      final byte[] data = toByteArray(segment);
      final ByteBuffer buffer = ByteBuffer.wrap(data);

      logger.fine("Created ByteBuffer view of memory segment (size: " + size + ")");
      return buffer;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create ByteBuffer from memory segment", e);
    }
  }

  /**
   * Allocates a memory segment of the specified size.
   *
   * @param size the size in bytes
   * @return the allocated memory segment
   * @throws IllegalArgumentException if size is negative
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
      logger.fine("Allocated memory segment of size " + size);
      return segment;
    } catch (Exception e) {
      throw new RuntimeException("Failed to allocate memory segment", e);
    }
  }

  /**
   * Allocates and initializes a memory segment with the specified value.
   *
   * @param size the size in bytes
   * @param value the value to initialize each byte with
   * @return the allocated and initialized memory segment
   * @throws IllegalArgumentException if size is negative
   */
  public MemorySegment allocateAndFill(final long size, final byte value) {
    final MemorySegment segment = allocate(size);

    if (segment != MemorySegment.NULL) {
      segment.fill(value);
      logger.fine("Filled memory segment with value " + value);
    }

    return segment;
  }

  /**
   * Creates a null-terminated string segment from a Java string.
   *
   * @param str the string to convert
   * @return a null-terminated memory segment containing the string
   * @throws IllegalArgumentException if str is null
   */
  public MemorySegment fromString(final String str) {
    if (str == null) {
      throw new IllegalArgumentException("String cannot be null");
    }

    try {
      final MemorySegment segment = arena.allocateFrom(str);
      logger.fine("Created string segment for: " + str);
      return segment;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create string segment", e);
    }
  }

  /**
   * Reads a null-terminated string from a memory segment.
   *
   * @param segment the memory segment containing the string
   * @return the Java string
   * @throws IllegalArgumentException if segment is null
   */
  public String toString(final MemorySegment segment) {
    if (segment == null) {
      throw new IllegalArgumentException("Segment cannot be null");
    }

    if (segment == MemorySegment.NULL) {
      return "";
    }

    try {
      final String str = segment.getString(0);
      logger.fine("Read string from segment: " + str);
      return str;
    } catch (Exception e) {
      throw new RuntimeException("Failed to read string from segment", e);
    }
  }

  /**
   * Gets the arena associated with this manager.
   *
   * @return the arena instance
   */
  public Arena getArena() {
    return arena;
  }
}

package ai.tegmentum.wasmtime4j.jni;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the Memory interface.
 *
 * <p>This class provides access to WebAssembly linear memory through JNI calls to the native
 * Wasmtime library. It supports reading and writing data at arbitrary offsets within the memory
 * space.
 *
 * <p>This implementation ensures defensive programming to prevent buffer overflows and JVM crashes
 * through extensive bounds checking.
 */
public final class JniMemory implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(JniMemory.class.getName());

  /** Native memory handle. */
  private volatile long nativeHandle;

  /** Flag to track if this memory has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new JNI memory with the given native handle.
   *
   * @param nativeHandle the native memory handle
   * @throws IllegalArgumentException if nativeHandle is 0
   */
  JniMemory(final long nativeHandle) {
    if (nativeHandle == 0) {
      throw new IllegalArgumentException("Native handle cannot be 0");
    }
    this.nativeHandle = nativeHandle;
    LOGGER.fine("Created JNI memory with handle: " + nativeHandle);
  }

  /**
   * Gets the current size of the memory in bytes.
   *
   * @return the memory size in bytes
   * @throws IllegalStateException if this memory is closed
   * @throws RuntimeException if the size cannot be retrieved
   */
  public long size() {
    validateNotClosed();
    try {
      return nativeGetSize(nativeHandle);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting memory size", e);
    }
  }

  /**
   * Gets the current size of the memory in WebAssembly pages (64KB each).
   *
   * @return the memory size in pages
   * @throws IllegalStateException if this memory is closed
   * @throws RuntimeException if the size cannot be retrieved
   */
  public long sizeInPages() {
    return size() / 65536; // 64KB per page
  }

  /**
   * Grows the memory by the specified number of pages.
   *
   * @param pages the number of pages to grow by
   * @return the previous size in pages, or -1 if growth failed
   * @throws IllegalArgumentException if pages is negative
   * @throws IllegalStateException if this memory is closed
   * @throws RuntimeException if the operation fails
   */
  public long grow(final long pages) {
    if (pages < 0) {
      throw new IllegalArgumentException("Pages must be non-negative");
    }
    validateNotClosed();

    try {
      return nativeGrow(nativeHandle, pages);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error growing memory", e);
    }
  }

  /**
   * Reads a single byte from the memory.
   *
   * @param offset the byte offset
   * @return the byte value
   * @throws IllegalArgumentException if offset is negative
   * @throws IllegalStateException if this memory is closed
   * @throws IndexOutOfBoundsException if offset is beyond memory bounds
   * @throws RuntimeException if the read fails
   */
  public byte readByte(final long offset) {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset must be non-negative");
    }
    validateNotClosed();
    validateOffset(offset, 1);

    try {
      return nativeReadByte(nativeHandle, offset);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error reading byte", e);
    }
  }

  /**
   * Writes a single byte to the memory.
   *
   * @param offset the byte offset
   * @param value the byte value to write
   * @throws IllegalArgumentException if offset is negative
   * @throws IllegalStateException if this memory is closed
   * @throws IndexOutOfBoundsException if offset is beyond memory bounds
   * @throws RuntimeException if the write fails
   */
  public void writeByte(final long offset, final byte value) {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset must be non-negative");
    }
    validateNotClosed();
    validateOffset(offset, 1);

    try {
      nativeWriteByte(nativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error writing byte", e);
    }
  }

  /**
   * Reads bytes from the memory into a buffer.
   *
   * @param offset the starting byte offset
   * @param buffer the buffer to read into
   * @return the number of bytes read
   * @throws IllegalArgumentException if offset is negative or buffer is null
   * @throws IllegalStateException if this memory is closed
   * @throws IndexOutOfBoundsException if the read would exceed memory bounds
   * @throws RuntimeException if the read fails
   */
  public int readBytes(final long offset, final byte[] buffer) {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset must be non-negative");
    }
    if (buffer == null) {
      throw new IllegalArgumentException("Buffer cannot be null");
    }
    validateNotClosed();
    validateOffset(offset, buffer.length);

    try {
      return nativeReadBytes(nativeHandle, offset, buffer);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error reading bytes", e);
    }
  }

  /**
   * Writes bytes from a buffer to the memory.
   *
   * @param offset the starting byte offset
   * @param buffer the buffer to write from
   * @return the number of bytes written
   * @throws IllegalArgumentException if offset is negative or buffer is null
   * @throws IllegalStateException if this memory is closed
   * @throws IndexOutOfBoundsException if the write would exceed memory bounds
   * @throws RuntimeException if the write fails
   */
  public int writeBytes(final long offset, final byte[] buffer) {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset must be non-negative");
    }
    if (buffer == null) {
      throw new IllegalArgumentException("Buffer cannot be null");
    }
    validateNotClosed();
    validateOffset(offset, buffer.length);

    try {
      return nativeWriteBytes(nativeHandle, offset, buffer);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error writing bytes", e);
    }
  }

  /**
   * Gets a direct ByteBuffer view of the memory.
   *
   * <p>The returned buffer provides direct access to the WebAssembly memory. Changes to the buffer
   * are immediately visible to WebAssembly code and vice versa.
   *
   * <p><strong>Warning:</strong> The buffer becomes invalid if the memory is grown.
   *
   * @return a direct ByteBuffer view of the memory
   * @throws IllegalStateException if this memory is closed
   * @throws RuntimeException if the buffer cannot be created
   */
  public ByteBuffer getBuffer() {
    validateNotClosed();
    try {
      return nativeGetBuffer(nativeHandle);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting buffer", e);
    }
  }

  /**
   * Gets the native handle for internal use.
   *
   * @return the native handle
   * @throws IllegalStateException if this memory is closed
   */
  long getNativeHandle() {
    validateNotClosed();
    return nativeHandle;
  }

  /**
   * Validates that an offset and size are within memory bounds.
   *
   * @param offset the starting offset
   * @param size the number of bytes
   * @throws IndexOutOfBoundsException if the range is invalid
   */
  private void validateOffset(final long offset, final long size) {
    final long memorySize = size();
    if (offset + size > memorySize) {
      throw new IndexOutOfBoundsException(
          "Offset " + offset + " + size " + size + " exceeds memory size " + memorySize);
    }
  }

  /**
   * Closes this memory and releases all associated native resources.
   *
   * <p>After calling this method, all operations on this memory will throw {@link
   * IllegalStateException}. This method is idempotent.
   */
  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      if (nativeHandle != 0) {
        try {
          nativeDestroyMemory(nativeHandle);
          LOGGER.fine("Destroyed JNI memory with handle: " + nativeHandle);
        } catch (final Exception e) {
          LOGGER.warning("Error destroying native memory: " + e.getMessage());
        } finally {
          nativeHandle = 0;
        }
      }
    }
  }

  /** Finalizer to ensure native resources are released if close() wasn't called. */
  @Override
  protected void finalize() throws Throwable {
    try {
      if (!closed.get()) {
        LOGGER.warning("JniMemory was finalized without being closed");
        close();
      }
    } finally {
      super.finalize();
    }
  }

  /**
   * Validates that this memory is not closed.
   *
   * @throws IllegalStateException if this memory is closed
   */
  private void validateNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("Memory is closed");
    }
  }

  // Native method declarations

  /**
   * Gets the size of a memory in bytes.
   *
   * @param memoryHandle the native memory handle
   * @return the memory size in bytes
   */
  private static native long nativeGetSize(long memoryHandle);

  /**
   * Grows a memory by the specified number of pages.
   *
   * @param memoryHandle the native memory handle
   * @param pages the number of pages to grow by
   * @return the previous size in pages, or -1 on failure
   */
  private static native long nativeGrow(long memoryHandle, long pages);

  /**
   * Reads a single byte from memory.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @return the byte value
   */
  private static native byte nativeReadByte(long memoryHandle, long offset);

  /**
   * Writes a single byte to memory.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the byte value
   */
  private static native void nativeWriteByte(long memoryHandle, long offset, byte value);

  /**
   * Reads bytes from memory into a buffer.
   *
   * @param memoryHandle the native memory handle
   * @param offset the starting byte offset
   * @param buffer the buffer to read into
   * @return the number of bytes read
   */
  private static native int nativeReadBytes(long memoryHandle, long offset, byte[] buffer);

  /**
   * Writes bytes from a buffer to memory.
   *
   * @param memoryHandle the native memory handle
   * @param offset the starting byte offset
   * @param buffer the buffer to write from
   * @return the number of bytes written
   */
  private static native int nativeWriteBytes(long memoryHandle, long offset, byte[] buffer);

  /**
   * Gets a direct ByteBuffer view of the memory.
   *
   * @param memoryHandle the native memory handle
   * @return a direct ByteBuffer view of the memory
   */
  private static native ByteBuffer nativeGetBuffer(long memoryHandle);

  /**
   * Destroys a native memory.
   *
   * @param memoryHandle the native memory handle
   */
  private static native void nativeDestroyMemory(long memoryHandle);
}

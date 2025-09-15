package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * JNI implementation of the Memory interface.
 *
 * <p>This class provides access to WebAssembly linear memory through JNI calls to the native
 * Wasmtime library. It supports reading and writing data at arbitrary offsets within the memory
 * space.
 *
 * <p>This implementation ensures defensive programming to prevent buffer overflows and JVM crashes
 * through extensive bounds checking using JniValidation and the JniResource base class.
 */
public final class JniMemory extends JniResource implements WasmMemory {

  private static final Logger LOGGER = Logger.getLogger(JniMemory.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniMemory: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Creates a new JNI memory with the given native handle.
   *
   * @param nativeHandle the native memory handle
   * @throws JniResourceException if nativeHandle is invalid
   */
  JniMemory(final long nativeHandle) {
    super(nativeHandle);
    LOGGER.fine("Created JNI memory with handle: 0x" + Long.toHexString(nativeHandle));
  }

  /**
   * Gets the current size of the memory in bytes.
   *
   * @return the memory size in bytes
   * @throws JniResourceException if this memory is closed
   * @throws RuntimeException if the size cannot be retrieved
   */
  public long sizeInBytes() {
    ensureNotClosed();
    try {
      return nativeGetSize(getNativeHandle());
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
  public int size() {
    return (int) (sizeInBytes() / 65536); // 64KB per page
  }

  /**
   * Gets the current size of the memory in WebAssembly pages (64KB each).
   *
   * @return the memory size in pages
   * @throws IllegalStateException if this memory is closed
   * @throws RuntimeException if the size cannot be retrieved
   */
  public int sizeInPages() {
    return size();
  }

  /**
   * Grows the memory by the specified number of pages.
   *
   * @param pages the number of pages to grow by
   * @return the previous size in pages, or -1 if growth failed
   * @throws JniResourceException if pages is negative or this memory is closed
   * @throws RuntimeException if the operation fails
   */
  public long grow(final long pages) {
    JniValidation.requireNonNegative(pages, "pages");
    ensureNotClosed();

    try {
      return nativeGrow(getNativeHandle(), pages);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error growing memory", e);
    }
  }

  @Override
  public int grow(final int pages) {
    return (int) grow((long) pages);
  }

  /**
   * Reads a single byte from the memory.
   *
   * @param offset the byte offset
   * @return the byte value
   * @throws JniResourceException if offset is negative or this memory is closed
   * @throws IndexOutOfBoundsException if offset is beyond memory bounds
   * @throws RuntimeException if the read fails
   */
  public byte readByte(final long offset) {
    JniValidation.requireNonNegative(offset, "offset");
    ensureNotClosed();
    validateOffset(offset, 1);

    try {
      return nativeReadByte(getNativeHandle(), offset);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error reading byte", e);
    }
  }

  @Override
  public byte readByte(final int offset) {
    return readByte((long) offset);
  }

  /**
   * Writes a single byte to the memory.
   *
   * @param offset the byte offset
   * @param value the byte value to write
   * @throws JniResourceException if offset is negative or this memory is closed
   * @throws IndexOutOfBoundsException if offset is beyond memory bounds
   * @throws RuntimeException if the write fails
   */
  public void writeByte(final long offset, final byte value) {
    JniValidation.requireNonNegative(offset, "offset");
    ensureNotClosed();
    validateOffset(offset, 1);

    try {
      nativeWriteByte(getNativeHandle(), offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error writing byte", e);
    }
  }

  @Override
  public void writeByte(final int offset, final byte value) {
    writeByte((long) offset, value);
  }

  /**
   * Reads bytes from the memory into a buffer.
   *
   * @param offset the starting byte offset
   * @param buffer the buffer to read into
   * @return the number of bytes read
   * @throws JniResourceException if offset is negative, buffer is null, or this memory is closed
   * @throws IndexOutOfBoundsException if the read would exceed memory bounds
   * @throws RuntimeException if the read fails
   */
  public int readBytes(final long offset, final byte[] buffer) {
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNull(buffer, "buffer");
    ensureNotClosed();
    validateOffset(offset, buffer.length);

    try {
      return nativeReadBytes(getNativeHandle(), offset, buffer);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error reading bytes", e);
    }
  }

  @Override
  public void readBytes(
      final int offset, final byte[] dest, final int destOffset, final int length) {
    ensureNotClosed();
    JniValidation.requireNonNull(dest, "dest");
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNegative(destOffset, "destOffset");
    JniValidation.requireNonNegative(length, "length");

    if (destOffset + length > dest.length) {
      throw new IndexOutOfBoundsException("destOffset + length exceeds dest array length");
    }

    // Create a temporary buffer for the native call
    final byte[] tempBuffer = new byte[length];
    final int bytesRead = nativeReadBytes(getNativeHandle(), offset, tempBuffer);
    System.arraycopy(tempBuffer, 0, dest, destOffset, Math.min(bytesRead, length));
  }

  /**
   * Writes bytes from a buffer to the memory.
   *
   * @param offset the starting byte offset
   * @param buffer the buffer to write from
   * @return the number of bytes written
   * @throws JniResourceException if offset is negative, buffer is null, or this memory is closed
   * @throws IndexOutOfBoundsException if the write would exceed memory bounds
   * @throws RuntimeException if the write fails
   */
  public int writeBytes(final long offset, final byte[] buffer) {
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNull(buffer, "buffer");
    ensureNotClosed();
    validateOffset(offset, buffer.length);

    try {
      return nativeWriteBytes(getNativeHandle(), offset, buffer);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error writing bytes", e);
    }
  }

  @Override
  public void writeBytes(
      final int offset, final byte[] src, final int srcOffset, final int length) {
    ensureNotClosed();
    JniValidation.requireNonNull(src, "src");
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNegative(srcOffset, "srcOffset");
    JniValidation.requireNonNegative(length, "length");

    if (srcOffset + length > src.length) {
      throw new IndexOutOfBoundsException("srcOffset + length exceeds src array length");
    }

    // Create a temporary buffer for the native call
    final byte[] tempBuffer = new byte[length];
    System.arraycopy(src, srcOffset, tempBuffer, 0, length);
    nativeWriteBytes(getNativeHandle(), offset, tempBuffer);
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
   * @throws JniResourceException if this memory is closed
   * @throws RuntimeException if the buffer cannot be created
   */
  public ByteBuffer getBuffer() {
    ensureNotClosed();
    try {
      return nativeGetBuffer(getNativeHandle());
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting buffer", e);
    }
  }

  // Interface implementation methods for WasmMemory

  @Override
  public int getSize() {
    return (int) sizeInPages();
  }

  @Override
  public int getMaxSize() {
    // TODO: Implement native method to get max size
    return -1;
  }

  /**
   * Gets the resource type name for logging and error messages.
   *
   * @return the resource type name
   */
  @Override
  protected String getResourceType() {
    return "Memory";
  }

  /**
   * Validates that an offset and size are within memory bounds.
   *
   * @param offset the starting offset
   * @param size the number of bytes
   * @throws IndexOutOfBoundsException if the range is invalid
   */
  private void validateOffset(final long offset, final long size) {
    final long memorySize = sizeInBytes();
    if (offset + size > memorySize) {
      throw new IndexOutOfBoundsException(
          "Offset " + offset + " + size " + size + " exceeds memory size " + memorySize);
    }
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * @throws Exception if there's an error during cleanup
   */
  @Override
  protected void doClose() throws Exception {
    nativeDestroyMemory(nativeHandle);
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

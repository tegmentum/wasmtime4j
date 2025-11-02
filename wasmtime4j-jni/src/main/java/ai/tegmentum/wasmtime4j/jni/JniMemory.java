package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Memory64Instruction;
import ai.tegmentum.wasmtime4j.Memory64InstructionHandler;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

  // Performance optimization fields
  private volatile ByteBuffer cachedBuffer;
  private volatile long lastBufferCheck = 0;
  private static final long BUFFER_CACHE_VALIDITY_MS = 100; // Cache buffer for 100ms
  private static final int BULK_OPERATION_THRESHOLD = 1024; // Use bulk operations for >= 1KB

  // Memory64 instruction support
  private final Memory64InstructionHandler instructionHandler = new Memory64InstructionHandler();

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

    // Optimization: For large operations or when destOffset is 0, avoid temporary buffer
    if (destOffset == 0 && length == dest.length) {
      // Direct read into destination array
      nativeReadBytes(getNativeHandle(), offset, dest);
    } else if (length >= BULK_OPERATION_THRESHOLD) {
      // Use optimized bulk read for large operations
      readBytesOptimized(offset, dest, destOffset, length);
    } else {
      // Original implementation for small operations
      final byte[] tempBuffer = new byte[length];
      final int bytesRead = nativeReadBytes(getNativeHandle(), offset, tempBuffer);
      System.arraycopy(tempBuffer, 0, dest, destOffset, Math.min(bytesRead, length));
    }
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

    // Optimization: For large operations or when srcOffset is 0, avoid temporary buffer
    if (srcOffset == 0 && length == src.length) {
      // Direct write from source array
      nativeWriteBytes(getNativeHandle(), offset, src);
    } else if (length >= BULK_OPERATION_THRESHOLD) {
      // Use optimized bulk write for large operations
      writeBytesOptimized(offset, src, srcOffset, length);
    } else {
      // Original implementation for small operations
      final byte[] tempBuffer = new byte[length];
      System.arraycopy(src, srcOffset, tempBuffer, 0, length);
      nativeWriteBytes(getNativeHandle(), offset, tempBuffer);
    }
  }

  /**
   * Gets a direct ByteBuffer view of the memory with caching for performance.
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
  @SuppressFBWarnings(
      value = "EI_EXPOSE_BUF",
      justification = "Returns duplicate() of buffer, not the internal reference")
  public ByteBuffer getBuffer() {
    ensureNotClosed();

    // Use cached buffer if still valid (optimization for frequent access)
    final long currentTime = System.currentTimeMillis();
    if (cachedBuffer != null && (currentTime - lastBufferCheck) < BUFFER_CACHE_VALIDITY_MS) {
      return cachedBuffer.duplicate(); // Return a duplicate to prevent sharing position/limit
    }

    try {
      cachedBuffer = nativeGetBuffer(getNativeHandle());
      if (cachedBuffer != null) {
        cachedBuffer.order(ByteOrder.LITTLE_ENDIAN); // WebAssembly is little-endian
      }
      lastBufferCheck = currentTime;
      return cachedBuffer != null ? cachedBuffer.duplicate() : null;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting buffer", e);
    }
  }

  /**
   * Optimized bulk read operation using ByteBuffer for large data transfers.
   *
   * @param offset starting offset in memory
   * @param dest destination array
   * @param destOffset offset in destination array
   * @param length number of bytes to read
   */
  private void readBytesOptimized(
      final int offset, final byte[] dest, final int destOffset, final int length) {
    try {
      final ByteBuffer buffer = getBuffer();
      if (buffer != null && buffer.capacity() >= offset + length) {
        // Use direct memory access for performance
        buffer.position(offset);
        buffer.get(dest, destOffset, length);
      } else {
        // Fallback to regular read if buffer is not available or too small
        final byte[] tempBuffer = new byte[length];
        nativeReadBytes(getNativeHandle(), offset, tempBuffer);
        System.arraycopy(tempBuffer, 0, dest, destOffset, length);
      }
    } catch (final Exception e) {
      // Fallback to regular read on any error
      final byte[] tempBuffer = new byte[length];
      nativeReadBytes(getNativeHandle(), offset, tempBuffer);
      System.arraycopy(tempBuffer, 0, dest, destOffset, length);
    }
  }

  /**
   * Optimized bulk write operation using ByteBuffer for large data transfers.
   *
   * @param offset starting offset in memory
   * @param src source array
   * @param srcOffset offset in source array
   * @param length number of bytes to write
   */
  private void writeBytesOptimized(
      final int offset, final byte[] src, final int srcOffset, final int length) {
    try {
      final ByteBuffer buffer = getBuffer();
      if (buffer != null && buffer.capacity() >= offset + length) {
        // Use direct memory access for performance
        buffer.position(offset);
        buffer.put(src, srcOffset, length);
      } else {
        // Fallback to regular write if buffer is not available or too small
        final byte[] tempBuffer = new byte[length];
        System.arraycopy(src, srcOffset, tempBuffer, 0, length);
        nativeWriteBytes(getNativeHandle(), offset, tempBuffer);
      }
    } catch (final Exception e) {
      // Fallback to regular write on any error
      final byte[] tempBuffer = new byte[length];
      System.arraycopy(src, srcOffset, tempBuffer, 0, length);
      nativeWriteBytes(getNativeHandle(), offset, tempBuffer);
    }
  }

  /**
   * High-performance integer read using direct ByteBuffer access.
   *
   * @param offset the byte offset
   * @return the integer value
   * @throws IndexOutOfBoundsException if offset is invalid
   */
  public int readInt(final int offset) {
    ensureNotClosed();
    validateOffset(offset, 4);

    try {
      final ByteBuffer buffer = getBuffer();
      if (buffer != null && buffer.capacity() >= offset + 4) {
        return buffer.getInt(offset);
      } else {
        // Fallback to byte-by-byte read
        final byte[] bytes = new byte[4];
        nativeReadBytes(getNativeHandle(), offset, bytes);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
      }
    } catch (final Exception e) {
      // Fallback to byte-by-byte read
      final byte[] bytes = new byte[4];
      nativeReadBytes(getNativeHandle(), offset, bytes);
      return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
  }

  /**
   * High-performance integer write using direct ByteBuffer access.
   *
   * @param offset the byte offset
   * @param value the integer value to write
   * @throws IndexOutOfBoundsException if offset is invalid
   */
  public void writeInt(final int offset, final int value) {
    ensureNotClosed();
    validateOffset(offset, 4);

    try {
      final ByteBuffer buffer = getBuffer();
      if (buffer != null && buffer.capacity() >= offset + 4) {
        buffer.putInt(offset, value);
      } else {
        // Fallback to byte-by-byte write
        final byte[] bytes =
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
        nativeWriteBytes(getNativeHandle(), offset, bytes);
      }
    } catch (final Exception e) {
      // Fallback to byte-by-byte write
      final byte[] bytes =
          ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
      nativeWriteBytes(getNativeHandle(), offset, bytes);
    }
  }

  // Interface implementation methods for WasmMemory

  @Override
  public int getSize() {
    return sizeInPages();
  }

  @Override
  public int getMaxSize() {
    ensureNotClosed();
    try {
      long maxSize64 = nativeGetMaxSize(getNativeHandle());
      if (maxSize64 == -1 || maxSize64 > Integer.MAX_VALUE) {
        return -1; // Unlimited or exceeds 32-bit range
      }
      return (int) maxSize64;
    } catch (final Exception e) {
      return -1; // Default to unlimited on error
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.MemoryType getMemoryType() {
    ensureNotClosed();
    try {
      final long[] typeInfo = nativeGetMemoryTypeInfo(getNativeHandle());
      if (typeInfo.length < 4) {
        throw new IllegalStateException("Invalid memory type info from native");
      }

      final long minimum = typeInfo[0];
      final Long maximum = typeInfo[1] == -1 ? null : typeInfo[1];
      final boolean is64Bit = typeInfo[2] != 0;
      final boolean isShared = typeInfo[3] != 0;

      return new ai.tegmentum.wasmtime4j.jni.type.JniMemoryType(
          minimum, maximum, is64Bit, isShared);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting memory type", e);
    }
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

  /**
   * Gets memory type information directly from the memory.
   *
   * @param memoryHandle the native memory handle
   * @return array containing [minimum, maximum(-1 if unlimited), is64Bit(0/1), isShared(0/1)]
   */
  private static native long[] nativeGetMemoryTypeInfo(long memoryHandle);

  // Bulk Memory Operations Implementation

  @Override
  public void copy(final int destOffset, final int srcOffset, final int length) {
    JniValidation.requireNonNegative(destOffset, "destOffset");
    JniValidation.requireNonNegative(srcOffset, "srcOffset");
    JniValidation.requireNonNegative(length, "length");
    ensureNotClosed();

    try {
      nativeMemoryCopy(getNativeHandle(), destOffset, srcOffset, length);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error copying memory", e);
    }
  }

  @Override
  public void fill(final int offset, final byte value, final int length) {
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNegative(length, "length");
    ensureNotClosed();

    try {
      nativeMemoryFill(getNativeHandle(), offset, value, length);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error filling memory", e);
    }
  }

  @Override
  public void init(
      final int destOffset, final int dataSegmentIndex, final int srcOffset, final int length) {
    JniValidation.requireNonNegative(destOffset, "destOffset");
    JniValidation.requireNonNegative(dataSegmentIndex, "dataSegmentIndex");
    JniValidation.requireNonNegative(srcOffset, "srcOffset");
    JniValidation.requireNonNegative(length, "length");
    ensureNotClosed();

    try {
      nativeMemoryInit(getNativeHandle(), destOffset, dataSegmentIndex, srcOffset, length);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error initializing memory from data segment", e);
    }
  }

  @Override
  public void dropDataSegment(final int dataSegmentIndex) {
    JniValidation.requireNonNegative(dataSegmentIndex, "dataSegmentIndex");
    ensureNotClosed();

    try {
      nativeDataDrop(getNativeHandle(), dataSegmentIndex);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error dropping data segment", e);
    }
  }

  // Native method declarations for bulk memory operations

  /**
   * Copies memory from one region to another within the same memory instance.
   *
   * @param memoryHandle the native memory handle
   * @param destOffset the destination offset in memory
   * @param srcOffset the source offset in memory
   * @param length the number of bytes to copy
   */
  private static native void nativeMemoryCopy(
      long memoryHandle, int destOffset, int srcOffset, int length);

  /**
   * Fills a memory region with the specified byte value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the starting offset in memory
   * @param value the byte value to fill with
   * @param length the number of bytes to fill
   */
  private static native void nativeMemoryFill(
      long memoryHandle, int offset, byte value, int length);

  /**
   * Initializes memory from a data segment.
   *
   * @param memoryHandle the native memory handle
   * @param destOffset the destination offset in memory
   * @param dataSegmentIndex the index of the data segment
   * @param srcOffset the source offset within the data segment
   * @param length the number of bytes to copy
   */
  private static native void nativeMemoryInit(
      long memoryHandle, int destOffset, int dataSegmentIndex, int srcOffset, int length);

  /**
   * Drops a data segment.
   *
   * @param memoryHandle the native memory handle
   * @param dataSegmentIndex the index of the data segment to drop
   */
  private static native void nativeDataDrop(long memoryHandle, int dataSegmentIndex);

  // Shared Memory Operations Implementation

  @Override
  public boolean isShared() {
    ensureNotClosed();
    try {
      return nativeIsShared(getNativeHandle());
    } catch (final Exception e) {
      return false; // Default to non-shared if query fails
    }
  }

  @Override
  public int atomicCompareAndSwapInt(final int offset, final int expected, final int newValue) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateOffset(offset, 4);

    try {
      return nativeAtomicCompareAndSwapInt(getNativeHandle(), offset, expected, newValue);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic compare-and-swap failed", e);
    }
  }

  @Override
  public long atomicCompareAndSwapLong(final int offset, final long expected, final long newValue) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 8);
    validateOffset(offset, 8);

    try {
      return nativeAtomicCompareAndSwapLong(getNativeHandle(), offset, expected, newValue);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic compare-and-swap failed", e);
    }
  }

  @Override
  public int atomicLoadInt(final int offset) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateOffset(offset, 4);

    try {
      return nativeAtomicLoadInt(getNativeHandle(), offset);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic load failed", e);
    }
  }

  @Override
  public long atomicLoadLong(final int offset) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 8);
    validateOffset(offset, 8);

    try {
      return nativeAtomicLoadLong(getNativeHandle(), offset);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic load failed", e);
    }
  }

  @Override
  public void atomicStoreInt(final int offset, final int value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateOffset(offset, 4);

    try {
      nativeAtomicStoreInt(getNativeHandle(), offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic store failed", e);
    }
  }

  @Override
  public void atomicStoreLong(final int offset, final long value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 8);
    validateOffset(offset, 8);

    try {
      nativeAtomicStoreLong(getNativeHandle(), offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic store failed", e);
    }
  }

  @Override
  public int atomicAddInt(final int offset, final int value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateOffset(offset, 4);

    try {
      return nativeAtomicAddInt(getNativeHandle(), offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic add failed", e);
    }
  }

  @Override
  public long atomicAddLong(final int offset, final long value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 8);
    validateOffset(offset, 8);

    try {
      return nativeAtomicAddLong(getNativeHandle(), offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic add failed", e);
    }
  }

  @Override
  public int atomicAndInt(final int offset, final int value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateOffset(offset, 4);

    try {
      return nativeAtomicAndInt(getNativeHandle(), offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic AND failed", e);
    }
  }

  @Override
  public int atomicOrInt(final int offset, final int value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateOffset(offset, 4);

    try {
      return nativeAtomicOrInt(getNativeHandle(), offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic OR failed", e);
    }
  }

  @Override
  public int atomicXorInt(final int offset, final int value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateOffset(offset, 4);

    try {
      return nativeAtomicXorInt(getNativeHandle(), offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic XOR failed", e);
    }
  }

  @Override
  public void atomicFence() {
    ensureNotClosed();
    checkSharedMemory();

    try {
      nativeAtomicFence(getNativeHandle());
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic fence failed", e);
    }
  }

  @Override
  public int atomicNotify(final int offset, final int count) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateOffset(offset, 4);
    JniValidation.requireNonNegative(count, "count");

    try {
      return nativeAtomicNotify(getNativeHandle(), offset, count);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic notify failed", e);
    }
  }

  @Override
  public int atomicWait32(final int offset, final int expected, final long timeoutNanos) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateOffset(offset, 4);

    try {
      return nativeAtomicWait32(getNativeHandle(), offset, expected, timeoutNanos);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic wait failed", e);
    }
  }

  @Override
  public int atomicWait64(final int offset, final long expected, final long timeoutNanos) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 8);
    validateOffset(offset, 8);

    try {
      return nativeAtomicWait64(getNativeHandle(), offset, expected, timeoutNanos);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic wait failed", e);
    }
  }

  /**
   * Checks if this memory is shared, throwing an exception if not.
   *
   * @throws IllegalStateException if this memory is not shared
   */
  private void checkSharedMemory() {
    if (!isShared()) {
      throw new IllegalStateException("Operation requires shared memory");
    }
  }

  /**
   * Checks if an offset is properly aligned for the given alignment requirement.
   *
   * @param offset the offset to check
   * @param alignment the required alignment (must be a power of 2)
   * @throws IllegalArgumentException if offset is not properly aligned
   */
  private void checkAligned(final int offset, final int alignment) {
    if ((offset & (alignment - 1)) != 0) {
      throw new IllegalArgumentException(
          String.format("Offset %d is not aligned to %d-byte boundary", offset, alignment));
    }
  }

  // Native method declarations for shared memory operations

  /**
   * Checks if memory is shared.
   *
   * @param memoryHandle the native memory handle
   * @return true if memory is shared
   */
  private static native boolean nativeIsShared(long memoryHandle);

  /**
   * Performs atomic compare-and-swap on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param expected the expected value
   * @param newValue the new value
   * @return the original value
   */
  private static native int nativeAtomicCompareAndSwapInt(
      long memoryHandle, int offset, int expected, int newValue);

  /**
   * Performs atomic compare-and-swap on a 64-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param expected the expected value
   * @param newValue the new value
   * @return the original value
   */
  private static native long nativeAtomicCompareAndSwapLong(
      long memoryHandle, int offset, long expected, long newValue);

  /**
   * Performs atomic load on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @return the loaded value
   */
  private static native int nativeAtomicLoadInt(long memoryHandle, int offset);

  /**
   * Performs atomic load on a 64-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @return the loaded value
   */
  private static native long nativeAtomicLoadLong(long memoryHandle, int offset);

  /**
   * Performs atomic store on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to store
   */
  private static native void nativeAtomicStoreInt(long memoryHandle, int offset, int value);

  /**
   * Performs atomic store on a 64-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to store
   */
  private static native void nativeAtomicStoreLong(long memoryHandle, int offset, long value);

  /**
   * Performs atomic add on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to add
   * @return the original value
   */
  private static native int nativeAtomicAddInt(long memoryHandle, int offset, int value);

  /**
   * Performs atomic add on a 64-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to add
   * @return the original value
   */
  private static native long nativeAtomicAddLong(long memoryHandle, int offset, long value);

  /**
   * Performs atomic AND on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to AND with
   * @return the original value
   */
  private static native int nativeAtomicAndInt(long memoryHandle, int offset, int value);

  /**
   * Performs atomic OR on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to OR with
   * @return the original value
   */
  private static native int nativeAtomicOrInt(long memoryHandle, int offset, int value);

  /**
   * Performs atomic XOR on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to XOR with
   * @return the original value
   */
  private static native int nativeAtomicXorInt(long memoryHandle, int offset, int value);

  /**
   * Performs atomic memory fence.
   *
   * @param memoryHandle the native memory handle
   */
  private static native void nativeAtomicFence(long memoryHandle);

  /**
   * Notifies threads waiting on a memory location.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param count the number of threads to notify
   * @return the number of threads notified
   */
  private static native int nativeAtomicNotify(long memoryHandle, int offset, int count);

  /**
   * Waits for notification on a 32-bit memory location.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param expected the expected value
   * @param timeoutNanos the timeout in nanoseconds
   * @return wait result (0=notified, 1=mismatch, 2=timeout)
   */
  private static native int nativeAtomicWait32(
      long memoryHandle, int offset, int expected, long timeoutNanos);

  /**
   * Waits for notification on a 64-bit memory location.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param expected the expected value
   * @param timeoutNanos the timeout in nanoseconds
   * @return wait result (0=notified, 1=mismatch, 2=timeout)
   */
  private static native int nativeAtomicWait64(
      long memoryHandle, int offset, long expected, long timeoutNanos);

  // 64-bit Memory Operations Implementation

  @Override
  public long getSize64() {
    ensureNotClosed();
    return sizeInPages();
  }

  @Override
  public long getMaxSize64() {
    ensureNotClosed();
    try {
      return nativeGetMaxSize(getNativeHandle());
    } catch (final Exception e) {
      return -1; // Default to unlimited on error
    }
  }

  @Override
  public long grow64(final long pages) {
    if (pages < 0) {
      return -1;
    }
    ensureNotClosed();
    try {
      return nativeGrow(getNativeHandle(), pages);
    } catch (final Exception e) {
      return -1;
    }
  }

  @Override
  public byte readByte64(final long offset) {
    JniValidation.requireNonNegative(offset, "offset");
    ensureNotClosed();
    validateOffset64(offset, 1);

    try {
      return nativeReadByte(getNativeHandle(), offset);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error reading byte", e);
    }
  }

  @Override
  public void writeByte64(final long offset, final byte value) {
    JniValidation.requireNonNegative(offset, "offset");
    ensureNotClosed();
    validateOffset64(offset, 1);

    try {
      nativeWriteByte(getNativeHandle(), offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error writing byte", e);
    }
  }

  @Override
  public void readBytes64(
      final long offset, final byte[] dest, final int destOffset, final int length) {
    ensureNotClosed();
    JniValidation.requireNonNull(dest, "dest");
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNegative(destOffset, "destOffset");
    JniValidation.requireNonNegative(length, "length");

    if (destOffset + length > dest.length) {
      throw new IndexOutOfBoundsException("destOffset + length exceeds dest array length");
    }

    validateOffset64(offset, length);

    try {
      if (destOffset == 0 && length == dest.length) {
        // Direct read into destination array
        nativeReadBytes(getNativeHandle(), offset, dest);
      } else if (length >= BULK_OPERATION_THRESHOLD) {
        // Use optimized bulk read for large operations
        readBytesOptimized64(offset, dest, destOffset, length);
      } else {
        // Original implementation for small operations
        final byte[] tempBuffer = new byte[length];
        final int bytesRead = nativeReadBytes(getNativeHandle(), offset, tempBuffer);
        System.arraycopy(tempBuffer, 0, dest, destOffset, Math.min(bytesRead, length));
      }
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error reading bytes", e);
    }
  }

  @Override
  public void writeBytes64(
      final long offset, final byte[] src, final int srcOffset, final int length) {
    ensureNotClosed();
    JniValidation.requireNonNull(src, "src");
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNegative(srcOffset, "srcOffset");
    JniValidation.requireNonNegative(length, "length");

    if (srcOffset + length > src.length) {
      throw new IndexOutOfBoundsException("srcOffset + length exceeds src array length");
    }

    validateOffset64(offset, length);

    try {
      if (srcOffset == 0 && length == src.length) {
        // Direct write from source array
        nativeWriteBytes(getNativeHandle(), offset, src);
      } else if (length >= BULK_OPERATION_THRESHOLD) {
        // Use optimized bulk write for large operations
        writeBytesOptimized64(offset, src, srcOffset, length);
      } else {
        // Original implementation for small operations
        final byte[] tempBuffer = new byte[length];
        System.arraycopy(src, srcOffset, tempBuffer, 0, length);
        nativeWriteBytes(getNativeHandle(), offset, tempBuffer);
      }
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error writing bytes", e);
    }
  }

  @Override
  public void copy64(final long destOffset, final long srcOffset, final long length) {
    JniValidation.requireNonNegative(destOffset, "destOffset");
    JniValidation.requireNonNegative(srcOffset, "srcOffset");
    JniValidation.requireNonNegative(length, "length");
    ensureNotClosed();

    validateOffset64(destOffset, length);
    validateOffset64(srcOffset, length);

    try {
      nativeMemoryCopy64(getNativeHandle(), destOffset, srcOffset, length);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error copying memory", e);
    }
  }

  @Override
  public void fill64(final long offset, final byte value, final long length) {
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNegative(length, "length");
    ensureNotClosed();

    validateOffset64(offset, length);

    try {
      nativeMemoryFill64(getNativeHandle(), offset, value, length);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error filling memory", e);
    }
  }

  @Override
  public void init64(
      final long destOffset, final int dataSegmentIndex, final long srcOffset, final long length) {
    JniValidation.requireNonNegative(destOffset, "destOffset");
    JniValidation.requireNonNegative(dataSegmentIndex, "dataSegmentIndex");
    JniValidation.requireNonNegative(srcOffset, "srcOffset");
    JniValidation.requireNonNegative(length, "length");
    ensureNotClosed();

    validateOffset64(destOffset, length);

    try {
      nativeMemoryInit64(getNativeHandle(), destOffset, dataSegmentIndex, srcOffset, length);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error initializing memory from data segment", e);
    }
  }

  @Override
  public boolean supports64BitAddressing() {
    ensureNotClosed();
    try {
      return nativeSupports64BitAddressing(getNativeHandle());
    } catch (final Exception e) {
      return false; // Default to 32-bit if query fails
    }
  }

  @Override
  public long getSizeInBytes64() {
    return getSize64() * 65536L;
  }

  @Override
  public long getMaxSizeInBytes64() {
    final long maxPages = getMaxSize64();
    return maxPages == -1 ? -1 : maxPages * 65536L;
  }

  /**
   * Validates that an offset and size are within 64-bit memory bounds.
   *
   * @param offset the starting offset
   * @param size the number of bytes
   * @throws IndexOutOfBoundsException if the range is invalid
   */
  private void validateOffset64(final long offset, final long size) {
    final long memorySize = getSizeInBytes64();
    if (offset + size > memorySize) {
      throw new IndexOutOfBoundsException(
          "Offset " + offset + " + size " + size + " exceeds memory size " + memorySize);
    }
  }

  /**
   * Optimized bulk read operation using ByteBuffer for large data transfers with 64-bit addressing.
   *
   * @param offset starting offset in memory (64-bit)
   * @param dest destination array
   * @param destOffset offset in destination array
   * @param length number of bytes to read
   */
  private void readBytesOptimized64(
      final long offset, final byte[] dest, final int destOffset, final int length) {
    try {
      final ByteBuffer buffer = getBuffer();
      if (buffer != null && buffer.capacity() >= offset + length) {
        // Use direct memory access for performance
        buffer.position((int) Math.min(offset, Integer.MAX_VALUE));
        buffer.get(dest, destOffset, length);
      } else {
        // Fallback to regular read if buffer is not available or too small
        final byte[] tempBuffer = new byte[length];
        nativeReadBytes(getNativeHandle(), offset, tempBuffer);
        System.arraycopy(tempBuffer, 0, dest, destOffset, length);
      }
    } catch (final Exception e) {
      // Fallback to regular read on any error
      final byte[] tempBuffer = new byte[length];
      nativeReadBytes(getNativeHandle(), offset, tempBuffer);
      System.arraycopy(tempBuffer, 0, dest, destOffset, length);
    }
  }

  /**
   * Optimized bulk write operation using ByteBuffer for large data transfers with 64-bit
   * addressing.
   *
   * @param offset starting offset in memory (64-bit)
   * @param src source array
   * @param srcOffset offset in source array
   * @param length number of bytes to write
   */
  private void writeBytesOptimized64(
      final long offset, final byte[] src, final int srcOffset, final int length) {
    try {
      final ByteBuffer buffer = getBuffer();
      if (buffer != null && buffer.capacity() >= offset + length) {
        // Use direct memory access for performance
        buffer.position((int) Math.min(offset, Integer.MAX_VALUE));
        buffer.put(src, srcOffset, length);
      } else {
        // Fallback to regular write if buffer is not available or too small
        final byte[] tempBuffer = new byte[length];
        System.arraycopy(src, srcOffset, tempBuffer, 0, length);
        nativeWriteBytes(getNativeHandle(), offset, tempBuffer);
      }
    } catch (final Exception e) {
      // Fallback to regular write on any error
      final byte[] tempBuffer = new byte[length];
      System.arraycopy(src, srcOffset, tempBuffer, 0, length);
      nativeWriteBytes(getNativeHandle(), offset, tempBuffer);
    }
  }

  // Native method declarations for 64-bit memory operations

  /**
   * Gets the maximum size of a memory in pages.
   *
   * @param memoryHandle the native memory handle
   * @return the maximum size in pages, or -1 for unlimited
   */
  private static native long nativeGetMaxSize(long memoryHandle);

  /**
   * Checks if the memory supports 64-bit addressing.
   *
   * @param memoryHandle the native memory handle
   * @return true if 64-bit addressing is supported
   */
  private static native boolean nativeSupports64BitAddressing(long memoryHandle);

  /**
   * Copies memory from one region to another within the same memory instance using 64-bit offsets.
   *
   * @param memoryHandle the native memory handle
   * @param destOffset the destination offset in memory (64-bit)
   * @param srcOffset the source offset in memory (64-bit)
   * @param length the number of bytes to copy (64-bit)
   */
  private static native void nativeMemoryCopy64(
      long memoryHandle, long destOffset, long srcOffset, long length);

  /**
   * Fills a memory region with the specified byte value using 64-bit offsets.
   *
   * @param memoryHandle the native memory handle
   * @param offset the starting offset in memory (64-bit)
   * @param value the byte value to fill with
   * @param length the number of bytes to fill (64-bit)
   */
  private static native void nativeMemoryFill64(
      long memoryHandle, long offset, byte value, long length);

  /**
   * Initializes memory from a data segment using 64-bit offsets.
   *
   * @param memoryHandle the native memory handle
   * @param destOffset the destination offset in memory (64-bit)
   * @param dataSegmentIndex the index of the data segment
   * @param srcOffset the source offset within the data segment (64-bit)
   * @param length the number of bytes to copy (64-bit)
   */
  private static native void nativeMemoryInit64(
      long memoryHandle, long destOffset, int dataSegmentIndex, long srcOffset, long length);

  // Memory64 Instruction Support

  /**
   * Executes a Memory64 instruction by opcode.
   *
   * @param opcode the instruction opcode
   * @param offset the memory offset (64-bit)
   * @param value the value for store operations (ignored for loads)
   * @return the result value for load operations, or 0 for store/control operations
   * @throws UnsupportedOperationException if the instruction or memory doesn't support 64-bit
   * @throws IndexOutOfBoundsException if the operation is out of bounds
   * @throws IllegalArgumentException if parameters are invalid
   */
  public long executeMemory64Instruction(final int opcode, final long offset, final long value) {
    return instructionHandler.executeByOpcode(opcode, this, offset, value);
  }

  /**
   * Executes a Memory64 instruction by mnemonic.
   *
   * @param mnemonic the instruction mnemonic
   * @param offset the memory offset (64-bit)
   * @param value the value for store operations (ignored for loads)
   * @return the result value for load operations, or 0 for store/control operations
   * @throws UnsupportedOperationException if the instruction or memory doesn't support 64-bit
   * @throws IndexOutOfBoundsException if the operation is out of bounds
   * @throws IllegalArgumentException if parameters are invalid
   */
  public long executeMemory64Instruction(
      final String mnemonic, final long offset, final long value) {
    return instructionHandler.executeByMnemonic(mnemonic, this, offset, value);
  }

  /**
   * Executes a Memory64 instruction directly.
   *
   * @param instruction the instruction to execute
   * @param offset the memory offset (64-bit)
   * @param value the value for store operations (ignored for loads)
   * @return the result value for load operations, or 0 for store/control operations
   * @throws UnsupportedOperationException if the instruction or memory doesn't support 64-bit
   * @throws IndexOutOfBoundsException if the operation is out of bounds
   * @throws IllegalArgumentException if parameters are invalid
   */
  public long executeMemory64Instruction(
      final Memory64Instruction instruction, final long offset, final long value) {
    return instructionHandler.executeInstruction(instruction, this, offset, value);
  }

  /**
   * Checks if a Memory64 instruction is supported by this memory implementation.
   *
   * @param opcode the instruction opcode
   * @return true if the instruction is supported
   */
  public boolean isMemory64InstructionSupported(final int opcode) {
    return supports64BitAddressing() && instructionHandler.isInstructionSupported(opcode);
  }

  /**
   * Checks if a Memory64 instruction is supported by this memory implementation.
   *
   * @param mnemonic the instruction mnemonic
   * @return true if the instruction is supported
   */
  public boolean isMemory64InstructionSupported(final String mnemonic) {
    return supports64BitAddressing() && instructionHandler.isInstructionSupported(mnemonic);
  }

  /**
   * Gets execution statistics for Memory64 instructions.
   *
   * @return the execution statistics
   */
  public Memory64InstructionHandler.ExecutionStatistics getMemory64InstructionStatistics() {
    return instructionHandler.getStatistics();
  }
}

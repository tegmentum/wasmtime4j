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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.WaitResult;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.util.Validation;
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
 * through extensive bounds checking using Validation and the JniResource base class.
 */
public final class JniMemory extends JniResource implements WasmMemory {

  private static final Logger LOGGER = Logger.getLogger(JniMemory.class.getName());

  // Performance optimization fields
  private volatile ByteBuffer cachedBuffer;
  private volatile long lastBufferCheck = 0;
  private static final long BUFFER_CACHE_VALIDITY_MS = 100; // Cache buffer for 100ms
  private static final int BULK_OPERATION_THRESHOLD = 1024; // Use bulk operations for >= 1KB

  // Store reference for atomic operations
  private final JniStore store;

  // Cached store native handle to avoid redundant ensureNotClosed() calls via
  // store.getNativeHandle()
  private final long storeNativeHandle;

  // Cached shared flag to avoid redundant nativeIsShared JNI calls per atomic operation
  private final boolean shared;

  // Instance handle for data segment operations (memory.init, data.drop)
  private long instanceHandle;

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
   * @param store the store this memory belongs to
   * @throws JniResourceException if nativeHandle is invalid
   */
  JniMemory(final long nativeHandle, final JniStore store) {
    super(nativeHandle);
    this.store = store;
    this.storeNativeHandle = store != null ? store.getNativeHandle() : 0;
    this.shared = store != null && nativeIsShared(nativeHandle, this.storeNativeHandle);
    if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
      LOGGER.fine("Created JNI memory with handle: 0x" + Long.toHexString(nativeHandle));
    }
  }

  /**
   * Ensures this memory and its owning store are still usable.
   *
   * <p>For standalone shared memory (created from Engine without a Store), only the memory handle
   * is validated. For store-backed memory, both the memory handle and store are validated.
   *
   * @throws JniResourceException if this memory or its store has been closed
   */
  private void ensureUsable() {
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
    } finally {
      endOperation();
    }
  }

  /**
   * Returns the cached direct ByteBuffer, lazily initializing it if needed. Must be called inside a
   * beginOperation() guard. Returns null if the buffer cannot be obtained.
   */
  private ByteBuffer ensureBuffer() {
    ByteBuffer buf = cachedBuffer;
    if (buf == null) {
      buf = nativeGetBuffer(nativeHandle, storeNativeHandle);
      if (buf != null) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        cachedBuffer = buf;
        lastBufferCheck = System.currentTimeMillis();
      }
    }
    return buf;
  }

  /**
   * Sets the instance handle for data segment operations.
   *
   * <p>This must be called when the memory is obtained from an instance to enable memory.init and
   * data.drop operations which require access to the instance's data segments.
   *
   * @param instanceHandle the native instance handle
   */
  void setInstanceHandle(final long instanceHandle) {
    this.instanceHandle = instanceHandle;
  }

  /**
   * Gets the current size of the memory in bytes.
   *
   * @return the memory size in bytes
   * @throws JniResourceException if this memory is closed
   * @throws RuntimeException if the size cannot be retrieved
   */
  public long sizeInBytes() {
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeGetSize(nativeHandle, storeNativeHandle);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting memory size", e);
    } finally {
      endOperation();
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
    final int ps = pageSize();
    return ps > 0 ? (int) (sizeInBytes() / ps) : 0;
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
    Validation.requireNonNegative(pages, "pages");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }

      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine(
            "grow: memHandle=0x"
                + Long.toHexString(nativeHandle)
                + ", storeHandle=0x"
                + Long.toHexString(storeNativeHandle)
                + ", pages="
                + pages);
      }

      final long previousSize = nativeGrow(nativeHandle, storeNativeHandle, pages);
      if (previousSize >= 0) {
        // Invalidate cached buffer since memory layout changed
        cachedBuffer = null;
      }
      return previousSize;
    } finally {
      endOperation();
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
    Validation.requireNonNegative(offset, "offset");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      // Fast path: direct memory access via cached ByteBuffer
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE && buf.capacity() > offset) {
        return buf.get((int) offset);
      }
      return nativeReadByte(nativeHandle, storeNativeHandle, offset);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error reading byte", e);
    } finally {
      endOperation();
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
    Validation.requireNonNegative(offset, "offset");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      // Fast path: direct memory access via cached ByteBuffer
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE && buf.capacity() > offset) {
        buf.put((int) offset, value);
        return;
      }
      nativeWriteByte(nativeHandle, storeNativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error writing byte", e);
    } finally {
      endOperation();
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
    Validation.requireNonNegative(offset, "offset");
    Validation.requireNonNull(buffer, "buffer");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      // Fast path: direct memory access via cached ByteBuffer
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE && buf.capacity() >= offset + buffer.length) {
        final ByteBuffer slice = buf.duplicate();
        slice.position((int) offset);
        slice.get(buffer, 0, buffer.length);
        return buffer.length;
      }
      return nativeReadBytes(nativeHandle, storeNativeHandle, offset, buffer);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error reading bytes", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void readBytes(
      final int offset, final byte[] dest, final int destOffset, final int length) {
    Validation.requireNonNull(dest, "dest");
    Validation.requireNonNegative(offset, "offset");
    Validation.requireNonNegative(destOffset, "destOffset");
    Validation.requireNonNegative(length, "length");

    if (destOffset + length > dest.length) {
      throw new IndexOutOfBoundsException("destOffset + length exceeds dest array length");
    }

    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }

      // Fast path: direct memory access via cached ByteBuffer (no JNI)
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE && buf.capacity() >= offset + length) {
        final ByteBuffer slice = buf.duplicate();
        slice.position(offset);
        slice.get(dest, destOffset, length);
        return;
      }

      // Slow path: JNI for 64-bit offsets or unavailable buffer
      if (destOffset == 0 && length == dest.length) {
        nativeReadBytes(nativeHandle, storeNativeHandle, offset, dest);
      } else {
        final byte[] tempBuffer = new byte[length];
        final int bytesRead = nativeReadBytes(nativeHandle, storeNativeHandle, offset, tempBuffer);
        System.arraycopy(tempBuffer, 0, dest, destOffset, Math.min(bytesRead, length));
      }
    } finally {
      endOperation();
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
    Validation.requireNonNegative(offset, "offset");
    Validation.requireNonNull(buffer, "buffer");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      // Fast path: direct memory access via cached ByteBuffer
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE && buf.capacity() >= offset + buffer.length) {
        final ByteBuffer slice = buf.duplicate();
        slice.position((int) offset);
        slice.put(buffer, 0, buffer.length);
        return buffer.length;
      }
      return nativeWriteBytes(nativeHandle, storeNativeHandle, offset, buffer);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error writing bytes", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void writeBytes(
      final int offset, final byte[] src, final int srcOffset, final int length) {
    Validation.requireNonNull(src, "src");
    Validation.requireNonNegative(offset, "offset");
    Validation.requireNonNegative(srcOffset, "srcOffset");
    Validation.requireNonNegative(length, "length");

    if (srcOffset + length > src.length) {
      throw new IndexOutOfBoundsException("srcOffset + length exceeds src array length");
    }

    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }

      // Fast path: direct memory access via cached ByteBuffer (no JNI)
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE && buf.capacity() >= offset + length) {
        final ByteBuffer slice = buf.duplicate();
        slice.position(offset);
        slice.put(src, srcOffset, length);
        return;
      }

      // Slow path: JNI for 64-bit offsets or unavailable buffer
      if (srcOffset == 0 && length == src.length) {
        nativeWriteBytes(nativeHandle, storeNativeHandle, offset, src);
      } else {
        final byte[] tempBuffer = new byte[length];
        System.arraycopy(src, srcOffset, tempBuffer, 0, length);
        nativeWriteBytes(nativeHandle, storeNativeHandle, offset, tempBuffer);
      }
    } finally {
      endOperation();
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
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }

      // Use cached buffer if still valid (optimization for frequent access)
      final long currentTime = System.currentTimeMillis();
      if (cachedBuffer != null && (currentTime - lastBufferCheck) < BUFFER_CACHE_VALIDITY_MS) {
        return cachedBuffer.duplicate(); // Return a duplicate to prevent sharing position/limit
      }

      try {
        cachedBuffer = nativeGetBuffer(nativeHandle, storeNativeHandle);
        if (cachedBuffer != null) {
          cachedBuffer.order(ByteOrder.LITTLE_ENDIAN); // WebAssembly is little-endian
        }
        lastBufferCheck = currentTime;
        return cachedBuffer != null ? cachedBuffer.duplicate() : null;
      } catch (final RuntimeException e) {
        throw e;
      } catch (final Exception e) {
        throw new RuntimeException("Failed to get buffer: " + e.getMessage(), e);
      }
    } finally {
      endOperation();
    }
  }

  // Optimized typed memory access overrides — bypass readBytes64/writeBytes64 entirely
  // by using the cached direct ByteBuffer for zero-allocation, zero-JNI reads/writes.

  @Override
  public int readInt32(final long offset) {
    Validation.requireNonNegative(offset, "offset");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE - 4 && buf.capacity() >= offset + 4) {
        return buf.getInt((int) offset);
      }
      final byte[] bytes = new byte[4];
      nativeReadBytes(nativeHandle, storeNativeHandle, offset, bytes);
      return (bytes[0] & 0xFF)
          | ((bytes[1] & 0xFF) << 8)
          | ((bytes[2] & 0xFF) << 16)
          | ((bytes[3] & 0xFF) << 24);
    } finally {
      endOperation();
    }
  }

  @Override
  public void writeInt32(final long offset, final int value) {
    Validation.requireNonNegative(offset, "offset");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE - 4 && buf.capacity() >= offset + 4) {
        buf.putInt((int) offset, value);
        return;
      }
      final byte[] bytes = new byte[4];
      bytes[0] = (byte) (value & 0xFF);
      bytes[1] = (byte) ((value >> 8) & 0xFF);
      bytes[2] = (byte) ((value >> 16) & 0xFF);
      bytes[3] = (byte) ((value >> 24) & 0xFF);
      nativeWriteBytes(nativeHandle, storeNativeHandle, offset, bytes);
    } finally {
      endOperation();
    }
  }

  @Override
  public long readInt64(final long offset) {
    Validation.requireNonNegative(offset, "offset");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE - 8 && buf.capacity() >= offset + 8) {
        return buf.getLong((int) offset);
      }
      final byte[] bytes = new byte[8];
      nativeReadBytes(nativeHandle, storeNativeHandle, offset, bytes);
      return (bytes[0] & 0xFFL)
          | ((bytes[1] & 0xFFL) << 8)
          | ((bytes[2] & 0xFFL) << 16)
          | ((bytes[3] & 0xFFL) << 24)
          | ((bytes[4] & 0xFFL) << 32)
          | ((bytes[5] & 0xFFL) << 40)
          | ((bytes[6] & 0xFFL) << 48)
          | ((bytes[7] & 0xFFL) << 56);
    } finally {
      endOperation();
    }
  }

  @Override
  public void writeInt64(final long offset, final long value) {
    Validation.requireNonNegative(offset, "offset");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE - 8 && buf.capacity() >= offset + 8) {
        buf.putLong((int) offset, value);
        return;
      }
      final byte[] bytes = new byte[8];
      bytes[0] = (byte) (value & 0xFF);
      bytes[1] = (byte) ((value >> 8) & 0xFF);
      bytes[2] = (byte) ((value >> 16) & 0xFF);
      bytes[3] = (byte) ((value >> 24) & 0xFF);
      bytes[4] = (byte) ((value >> 32) & 0xFF);
      bytes[5] = (byte) ((value >> 40) & 0xFF);
      bytes[6] = (byte) ((value >> 48) & 0xFF);
      bytes[7] = (byte) ((value >> 56) & 0xFF);
      nativeWriteBytes(nativeHandle, storeNativeHandle, offset, bytes);
    } finally {
      endOperation();
    }
  }

  // Interface implementation methods for WasmMemory

  @Override
  public long dataPtr() {
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeGetDataPtr(nativeHandle, storeNativeHandle);
    } finally {
      endOperation();
    }
  }

  @Override
  public int pageSize() {
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return (int) nativeGetPageSize(nativeHandle);
    } finally {
      endOperation();
    }
  }

  @Override
  public int pageSizeLog2() {
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeGetPageSizeLog2(nativeHandle);
    } finally {
      endOperation();
    }
  }

  @Override
  public long dataSize() {
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeGetDataSize(nativeHandle, storeNativeHandle);
    } finally {
      endOperation();
    }
  }

  @Override
  public int getSize() {
    return sizeInPages();
  }

  @Override
  public int getMaxSize() {
    ensureUsable();
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
  public ai.tegmentum.wasmtime4j.type.MemoryType getMemoryType() {
    ensureUsable();
    try {
      final long[] typeInfo = nativeGetMemoryTypeInfo(getNativeHandle());
      if (typeInfo.length < 5) {
        throw new IllegalStateException("Invalid memory type info from native");
      }

      final long minimum = typeInfo[0];
      final Long maximum = typeInfo[1] == -1 ? null : typeInfo[1];
      final boolean is64Bit = typeInfo[2] != 0;
      final boolean isShared = typeInfo[3] != 0;
      final int pageSizeLog2 = (int) typeInfo[4];

      return new ai.tegmentum.wasmtime4j.jni.type.JniMemoryType(
          minimum, maximum, is64Bit, isShared, pageSizeLog2);
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
   * Performs the actual native resource cleanup.
   *
   * <p>Note: In wasmtime, Memories are owned by the Store. Destroying a Memory while the Store
   * still exists can corrupt the Store's internal slab state. We mark the Memory as closed but
   * don't destroy it - the Store will handle cleanup.
   *
   * @throws Exception if there's an error during cleanup
   */
  @Override
  protected void doClose() throws Exception {
    // Note: Do NOT call nativeDestroyMemory here. Memories are Store-owned resources.
    // The Store will clean up all its Memories when it is destroyed.
  }

  // Native method declarations

  /**
   * Gets the size of a memory in bytes.
   *
   * @param memoryHandle the native memory handle
   * @return the memory size in bytes
   */
  private static native long nativeGetSize(long memoryHandle, long storeHandle);

  /**
   * Grows a memory by the specified number of pages.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @param pages the number of pages to grow by
   * @return the previous size in pages, or -1 on failure
   */
  private static native long nativeGrow(long memoryHandle, long storeHandle, long pages);

  /**
   * Reads a single byte from memory.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @param offset the byte offset
   * @return the byte value
   */
  private static native byte nativeReadByte(long memoryHandle, long storeHandle, long offset);

  /**
   * Writes a single byte to memory.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @param offset the byte offset
   * @param value the byte value
   */
  private static native void nativeWriteByte(
      long memoryHandle, long storeHandle, long offset, byte value);

  /**
   * Reads bytes from memory into a buffer.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @param offset the starting byte offset
   * @param buffer the buffer to read into
   * @return the number of bytes read
   */
  private static native int nativeReadBytes(
      long memoryHandle, long storeHandle, long offset, byte[] buffer);

  /**
   * Writes bytes from a buffer to memory.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @param offset the starting byte offset
   * @param buffer the buffer to write from
   * @return the number of bytes written
   */
  private static native int nativeWriteBytes(
      long memoryHandle, long storeHandle, long offset, byte[] buffer);

  /**
   * Gets a direct ByteBuffer view of the memory.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @return a direct ByteBuffer view of the memory
   */
  private static native ByteBuffer nativeGetBuffer(long memoryHandle, long storeHandle);

  /**
   * Gets the raw data pointer of the WebAssembly linear memory.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @return the raw pointer address of the memory data
   */
  private static native long nativeGetDataPtr(long memoryHandle, long storeHandle);

  /**
   * Gets memory type information directly from the memory.
   *
   * @param memoryHandle the native memory handle
   * @return array containing [minimum, maximum(-1 if unlimited), is64Bit(0/1), isShared(0/1),
   *     pageSizeLog2]
   */
  private static native long[] nativeGetMemoryTypeInfo(long memoryHandle);

  // Bulk Memory Operations Implementation

  @Override
  public void copy(final int destOffset, final int srcOffset, final int length) {
    Validation.requireNonNegative(destOffset, "destOffset");
    Validation.requireNonNegative(srcOffset, "srcOffset");
    Validation.requireNonNegative(length, "length");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      nativeMemoryCopy(nativeHandle, storeNativeHandle, destOffset, srcOffset, length);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error copying memory", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void fill(final int offset, final byte value, final int length) {
    Validation.requireNonNegative(offset, "offset");
    Validation.requireNonNegative(length, "length");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      nativeMemoryFill(nativeHandle, storeNativeHandle, offset, value, length);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error filling memory", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void init(
      final int destOffset, final int dataSegmentIndex, final int srcOffset, final int length) {
    Validation.requireNonNegative(destOffset, "destOffset");
    Validation.requireNonNegative(dataSegmentIndex, "dataSegmentIndex");
    Validation.requireNonNegative(srcOffset, "srcOffset");
    Validation.requireNonNegative(length, "length");
    ensureUsable();

    if (instanceHandle == 0) {
      throw new IllegalStateException("Cannot init: memory not associated with an instance");
    }

    try {
      nativeMemoryInit(
          getNativeHandle(),
          store.getNativeHandle(),
          instanceHandle,
          destOffset,
          dataSegmentIndex,
          srcOffset,
          length);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error initializing memory from data segment", e);
    }
  }

  @Override
  public void dropDataSegment(final int dataSegmentIndex) {
    Validation.requireNonNegative(dataSegmentIndex, "dataSegmentIndex");
    ensureUsable();

    try {
      if (instanceHandle == 0) {
        throw new IllegalStateException(
            "Cannot drop data segment: memory not associated with an instance");
      }
      nativeDataDrop(instanceHandle, dataSegmentIndex);
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
      long memoryHandle, long storeHandle, int destOffset, int srcOffset, int length);

  /**
   * Fills a memory region with the specified byte value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the starting offset in memory
   * @param value the byte value to fill with
   * @param length the number of bytes to fill
   */
  private static native void nativeMemoryFill(
      long memoryHandle, long storeHandle, int offset, byte value, int length);

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
      long memoryHandle,
      long storeHandle,
      long instanceHandle,
      int destOffset,
      int dataSegmentIndex,
      int srcOffset,
      int length);

  /**
   * Drops a data segment.
   *
   * @param memoryHandle the native memory handle
   * @param dataSegmentIndex the index of the data segment to drop
   */
  private static native void nativeDataDrop(long instanceHandle, int dataSegmentIndex);

  // Shared Memory Operations Implementation

  @Override
  public boolean isShared() {
    return shared;
  }

  @Override
  public int atomicCompareAndSwapInt(final int offset, final int expected, final int newValue) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 4);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicCompareAndSwapInt(
          nativeHandle, storeNativeHandle, offset, expected, newValue);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic compare-and-swap failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public long atomicCompareAndSwapLong(final int offset, final long expected, final long newValue) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 8);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicCompareAndSwapLong(
          nativeHandle, storeNativeHandle, offset, expected, newValue);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic compare-and-swap failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public int atomicLoadInt(final int offset) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 4);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicLoadInt(nativeHandle, storeNativeHandle, offset);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic load failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public long atomicLoadLong(final int offset) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 8);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicLoadLong(nativeHandle, storeNativeHandle, offset);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic load failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void atomicStoreInt(final int offset, final int value) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 4);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      nativeAtomicStoreInt(nativeHandle, storeNativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic store failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void atomicStoreLong(final int offset, final long value) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 8);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      nativeAtomicStoreLong(nativeHandle, storeNativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic store failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public int atomicAddInt(final int offset, final int value) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 4);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicAddInt(nativeHandle, storeNativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic add failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public long atomicAddLong(final int offset, final long value) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 8);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicAddLong(nativeHandle, storeNativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic add failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public int atomicAndInt(final int offset, final int value) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 4);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicAndInt(nativeHandle, storeNativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic AND failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public int atomicOrInt(final int offset, final int value) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 4);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicOrInt(nativeHandle, storeNativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic OR failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public int atomicXorInt(final int offset, final int value) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 4);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicXorInt(nativeHandle, storeNativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic XOR failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public long atomicAndLong(final int offset, final long value) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 8);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicAndLong(nativeHandle, storeNativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic AND i64 failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public long atomicOrLong(final int offset, final long value) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 8);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicOrLong(nativeHandle, storeNativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic OR i64 failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public long atomicXorLong(final int offset, final long value) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 8);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicXorLong(nativeHandle, storeNativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic XOR i64 failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void atomicFence() {
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      nativeAtomicFence(nativeHandle, storeNativeHandle);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic fence failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public int atomicNotify(final int offset, final int count) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 4);
    Validation.requireNonNegative(count, "count");
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeAtomicNotify(nativeHandle, storeNativeHandle, offset, count);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic notify failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public WaitResult atomicWait32(final int offset, final int expected, final long timeoutNanos) {
    Validation.requireNonNegative(offset, "offset");
    if (timeoutNanos < -1) {
      throw new IllegalArgumentException("Timeout must be non-negative or -1");
    }
    checkAligned(offset, 4);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return WaitResult.fromNativeCode(
          nativeAtomicWait32(nativeHandle, storeNativeHandle, offset, expected, timeoutNanos));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic wait failed", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public WaitResult atomicWait64(final int offset, final long expected, final long timeoutNanos) {
    Validation.requireNonNegative(offset, "offset");
    checkAligned(offset, 8);
    checkSharedMemory();
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return WaitResult.fromNativeCode(
          nativeAtomicWait64(nativeHandle, storeNativeHandle, offset, expected, timeoutNanos));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Atomic wait failed", e);
    } finally {
      endOperation();
    }
  }

  /**
   * Checks if this memory is shared, throwing an exception if not.
   *
   * @throws UnsupportedOperationException if this memory is not shared
   */
  private void checkSharedMemory() {
    if (!shared) {
      throw new UnsupportedOperationException("Operation requires shared memory");
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
   * @param storeHandle the native store handle
   * @return true if memory is shared
   */
  private static native boolean nativeIsShared(long memoryHandle, long storeHandle);

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
      long memoryHandle, long storeHandle, int offset, int expected, int newValue);

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
      long memoryHandle, long storeHandle, int offset, long expected, long newValue);

  /**
   * Performs atomic load on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @return the loaded value
   */
  private static native int nativeAtomicLoadInt(long memoryHandle, long storeHandle, int offset);

  /**
   * Performs atomic load on a 64-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @return the loaded value
   */
  private static native long nativeAtomicLoadLong(long memoryHandle, long storeHandle, int offset);

  /**
   * Performs atomic store on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to store
   */
  private static native void nativeAtomicStoreInt(
      long memoryHandle, long storeHandle, int offset, int value);

  /**
   * Performs atomic store on a 64-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to store
   */
  private static native void nativeAtomicStoreLong(
      long memoryHandle, long storeHandle, int offset, long value);

  /**
   * Performs atomic add on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to add
   * @return the original value
   */
  private static native int nativeAtomicAddInt(
      long memoryHandle, long storeHandle, int offset, int value);

  /**
   * Performs atomic add on a 64-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to add
   * @return the original value
   */
  private static native long nativeAtomicAddLong(
      long memoryHandle, long storeHandle, int offset, long value);

  /**
   * Performs atomic AND on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to AND with
   * @return the original value
   */
  private static native int nativeAtomicAndInt(
      long memoryHandle, long storeHandle, int offset, int value);

  /**
   * Performs atomic OR on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to OR with
   * @return the original value
   */
  private static native int nativeAtomicOrInt(
      long memoryHandle, long storeHandle, int offset, int value);

  /**
   * Performs atomic XOR on a 32-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param value the value to XOR with
   * @return the original value
   */
  private static native int nativeAtomicXorInt(
      long memoryHandle, long storeHandle, int offset, int value);

  /**
   * Performs atomic AND on a 64-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @param offset the byte offset
   * @param value the value to AND with
   * @return the original value
   */
  private static native long nativeAtomicAndLong(
      long memoryHandle, long storeHandle, int offset, long value);

  /**
   * Performs atomic OR on a 64-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @param offset the byte offset
   * @param value the value to OR with
   * @return the original value
   */
  private static native long nativeAtomicOrLong(
      long memoryHandle, long storeHandle, int offset, long value);

  /**
   * Performs atomic XOR on a 64-bit value.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @param offset the byte offset
   * @param value the value to XOR with
   * @return the original value
   */
  private static native long nativeAtomicXorLong(
      long memoryHandle, long storeHandle, int offset, long value);

  /**
   * Performs atomic memory fence.
   *
   * @param memoryHandle the native memory handle
   */
  private static native void nativeAtomicFence(long memoryHandle, long storeHandle);

  /**
   * Notifies threads waiting on a memory location.
   *
   * @param memoryHandle the native memory handle
   * @param offset the byte offset
   * @param count the number of threads to notify
   * @return the number of threads notified
   */
  private static native int nativeAtomicNotify(
      long memoryHandle, long storeHandle, int offset, int count);

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
      long memoryHandle, long storeHandle, int offset, int expected, long timeoutNanos);

  /**
   * Waits for notification on a 64-bit memory location.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @param offset the byte offset
   * @param expected the expected value
   * @param timeoutNanos the timeout in nanoseconds
   * @return wait result (0=notified, 1=mismatch, 2=timeout)
   */
  private static native int nativeAtomicWait64(
      long memoryHandle, long storeHandle, int offset, long expected, long timeoutNanos);

  // 64-bit Memory Operations Implementation

  @Override
  public long getSize64() {
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeGetSize(nativeHandle, storeNativeHandle) / 65536L;
    } finally {
      endOperation();
    }
  }

  @Override
  public long getMaxSize64() {
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeGetMaxSize(nativeHandle);
    } catch (final JniResourceException e) {
      throw e;
    } catch (final Exception e) {
      return -1; // Default to unlimited on error
    } finally {
      endOperation();
    }
  }

  @Override
  public long grow64(final long pages) {
    if (pages < 0) {
      return -1;
    }
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      final long previousSize = nativeGrow(nativeHandle, storeNativeHandle, pages);
      if (previousSize >= 0) {
        // Invalidate cached buffer since memory layout changed
        cachedBuffer = null;
      }
      return previousSize;
    } finally {
      endOperation();
    }
  }

  @Override
  public byte readByte64(final long offset) {
    Validation.requireNonNegative(offset, "offset");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE && buf.capacity() > offset) {
        return buf.get((int) offset);
      }
      return nativeReadByte(nativeHandle, storeNativeHandle, offset);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error reading byte", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void writeByte64(final long offset, final byte value) {
    Validation.requireNonNegative(offset, "offset");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE && buf.capacity() > offset) {
        buf.put((int) offset, value);
        return;
      }
      nativeWriteByte(nativeHandle, storeNativeHandle, offset, value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error writing byte", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void readBytes64(
      final long offset, final byte[] dest, final int destOffset, final int length) {
    Validation.requireNonNull(dest, "dest");
    Validation.requireNonNegative(offset, "offset");
    Validation.requireNonNegative(destOffset, "destOffset");
    Validation.requireNonNegative(length, "length");

    if (destOffset + length > dest.length) {
      throw new IndexOutOfBoundsException("destOffset + length exceeds dest array length");
    }

    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }

      // Fast path: direct memory access via cached ByteBuffer (no JNI)
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE && buf.capacity() >= offset + length) {
        final ByteBuffer slice = buf.duplicate();
        slice.position((int) offset);
        slice.get(dest, destOffset, length);
        return;
      }

      // Slow path: JNI for 64-bit offsets or unavailable buffer
      if (destOffset == 0 && length == dest.length) {
        nativeReadBytes(nativeHandle, storeNativeHandle, offset, dest);
      } else {
        final byte[] tempBuffer = new byte[length];
        final int bytesRead = nativeReadBytes(nativeHandle, storeNativeHandle, offset, tempBuffer);
        System.arraycopy(tempBuffer, 0, dest, destOffset, Math.min(bytesRead, length));
      }
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error reading bytes", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void writeBytes64(
      final long offset, final byte[] src, final int srcOffset, final int length) {
    Validation.requireNonNull(src, "src");
    Validation.requireNonNegative(offset, "offset");
    Validation.requireNonNegative(srcOffset, "srcOffset");
    Validation.requireNonNegative(length, "length");

    if (srcOffset + length > src.length) {
      throw new IndexOutOfBoundsException("srcOffset + length exceeds src array length");
    }

    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }

      // Fast path: direct memory access via cached ByteBuffer (no JNI)
      final ByteBuffer buf = ensureBuffer();
      if (buf != null && offset <= Integer.MAX_VALUE && buf.capacity() >= offset + length) {
        final ByteBuffer slice = buf.duplicate();
        slice.position((int) offset);
        slice.put(src, srcOffset, length);
        return;
      }

      // Slow path: JNI for 64-bit offsets or unavailable buffer
      if (srcOffset == 0 && length == src.length) {
        nativeWriteBytes(nativeHandle, storeNativeHandle, offset, src);
      } else {
        final byte[] tempBuffer = new byte[length];
        System.arraycopy(src, srcOffset, tempBuffer, 0, length);
        nativeWriteBytes(nativeHandle, storeNativeHandle, offset, tempBuffer);
      }
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error writing bytes", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void copy64(final long destOffset, final long srcOffset, final long length) {
    Validation.requireNonNegative(destOffset, "destOffset");
    Validation.requireNonNegative(srcOffset, "srcOffset");
    Validation.requireNonNegative(length, "length");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      nativeMemoryCopy64(nativeHandle, storeNativeHandle, destOffset, srcOffset, length);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error copying memory", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void fill64(final long offset, final byte value, final long length) {
    Validation.requireNonNegative(offset, "offset");
    Validation.requireNonNegative(length, "length");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      nativeMemoryFill64(nativeHandle, storeNativeHandle, offset, value, length);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error filling memory", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void init64(
      final long destOffset, final int dataSegmentIndex, final long srcOffset, final long length) {
    Validation.requireNonNegative(destOffset, "destOffset");
    Validation.requireNonNegative(dataSegmentIndex, "dataSegmentIndex");
    Validation.requireNonNegative(srcOffset, "srcOffset");
    Validation.requireNonNegative(length, "length");

    if (instanceHandle == 0) {
      throw new IllegalStateException("Cannot init: memory not associated with an instance");
    }

    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      nativeMemoryInit64(
          nativeHandle,
          storeNativeHandle,
          instanceHandle,
          destOffset,
          dataSegmentIndex,
          srcOffset,
          length);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error initializing memory from data segment", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean supports64BitAddressing() {
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
      return nativeSupports64BitAddressing(nativeHandle);
    } catch (final JniResourceException e) {
      throw e;
    } catch (final Exception e) {
      LOGGER.warning("Failed to query 64-bit addressing support: " + e.getMessage());
      return false; // Default to 32-bit if query fails
    } finally {
      endOperation();
    }
  }

  @Override
  public long getSizeInBytes64() {
    return sizeInBytes();
  }

  @Override
  public long getMaxSizeInBytes64() {
    final long maxPages = getMaxSize64();
    return maxPages == -1 ? -1 : maxPages * 65536L;
  }

  @Override
  public long growAsync(final long pages) throws ai.tegmentum.wasmtime4j.exception.WasmException {
    Validation.requireNonNegative(pages, "pages");
    beginOperation();
    try {
      if (store != null && store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }

      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine(
            "growAsync: memHandle=0x"
                + Long.toHexString(nativeHandle)
                + ", storeHandle=0x"
                + Long.toHexString(storeNativeHandle)
                + ", pages="
                + pages);
      }

      final long previousSize = nativeGrowAsync(nativeHandle, storeNativeHandle, pages);
      // Invalidate cached buffer since memory layout changed
      cachedBuffer = null;
      return previousSize;
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException("Async memory growth failed", e);
    } finally {
      endOperation();
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
      long memoryHandle, long storeHandle, long destOffset, long srcOffset, long length);

  /**
   * Fills a memory region with the specified byte value using 64-bit offsets.
   *
   * @param memoryHandle the native memory handle
   * @param offset the starting offset in memory (64-bit)
   * @param value the byte value to fill with
   * @param length the number of bytes to fill (64-bit)
   */
  private static native void nativeMemoryFill64(
      long memoryHandle, long storeHandle, long offset, byte value, long length);

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
      long memoryHandle,
      long storeHandle,
      long instanceHandle,
      long destOffset,
      int dataSegmentIndex,
      long srcOffset,
      long length);

  /**
   * Grows memory asynchronously through the async resource limiter.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @param pages the number of pages to grow by
   * @return the previous size in pages, or -1 if growth failed
   */
  private static native long nativeGrowAsync(long memoryHandle, long storeHandle, long pages);

  /**
   * Gets the page size of the memory in bytes.
   *
   * @param memoryHandle the native memory handle
   * @return the page size in bytes (normally 65536, but may differ with custom page sizes)
   */
  private static native long nativeGetPageSize(long memoryHandle);

  /**
   * Gets the page size of the memory as log2.
   *
   * @param memoryHandle the native memory handle
   * @return the log2 of the page size (normally 16)
   */
  private static native int nativeGetPageSizeLog2(long memoryHandle);

  /**
   * Gets the data size of the memory in bytes.
   *
   * @param memoryHandle the native memory handle
   * @param storeHandle the native store handle
   * @return the data size in bytes
   */
  private static native long nativeGetDataSize(long memoryHandle, long storeHandle);
}

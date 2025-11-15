package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmThreadLocalStorage;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Panama FFI implementation of WebAssembly thread-local storage.
 *
 * <p>This implementation provides thread-local storage capabilities using Panama Foreign Function
 * Interface to interact with the native Wasmtime threading runtime. Each WebAssembly thread
 * maintains its own isolated storage space that provides:
 *
 * <ul>
 *   <li>Type-safe storage for various data types
 *   <li>Fast access without synchronization overhead
 *   <li>Automatic cleanup when thread terminates
 *   <li>Memory usage tracking and optimization
 * </ul>
 *
 * <p>All operations include comprehensive validation and error handling to prevent resource leaks
 * and ensure robust operation.
 *
 * @since 1.0.0
 */
public final class PanamaWasmThreadLocalStorage implements WasmThreadLocalStorage {

  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  /** Native thread handle for storage operations. */
  private final MemorySegment nativeThreadHandle;

  /** Memory arena for resource management. */
  private final Arena arena;

  /** Whether this storage has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new Panama thread-local storage instance.
   *
   * @param nativeThreadHandle native thread handle memory segment
   * @param arena memory arena for resource management
   * @throws WasmException if storage creation fails
   */
  public PanamaWasmThreadLocalStorage(final MemorySegment nativeThreadHandle, final Arena arena)
      throws WasmException {
    if (nativeThreadHandle == null || nativeThreadHandle.address() == 0) {
      throw new WasmException("Native thread handle cannot be null");
    }
    if (arena == null) {
      throw new WasmException("Arena cannot be null");
    }

    this.nativeThreadHandle = nativeThreadHandle;
    this.arena = arena;
  }

  @Override
  public void putInt(final String key, final int value) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment keyPtr = tempArena.allocateFrom(key);
      final int result = NATIVE_BINDINGS.threadPutInt(nativeThreadHandle, keyPtr, value);

      if (result != 0) {
        throw new WasmException("Failed to put integer value in thread-local storage");
      }
    }
  }

  @Override
  public int getInt(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment keyPtr = tempArena.allocateFrom(key);
      final MemorySegment outValue = tempArena.allocate(ValueLayout.JAVA_INT);

      final int result = NATIVE_BINDINGS.threadGetInt(nativeThreadHandle, keyPtr, outValue);

      if (result != 0) {
        throw new WasmException("Failed to get integer value from thread-local storage");
      }

      return outValue.get(ValueLayout.JAVA_INT, 0);
    }
  }

  @Override
  public void putLong(final String key, final long value) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment keyPtr = tempArena.allocateFrom(key);
      final int result = NATIVE_BINDINGS.threadPutLong(nativeThreadHandle, keyPtr, value);

      if (result != 0) {
        throw new WasmException("Failed to put long value in thread-local storage");
      }
    }
  }

  @Override
  public long getLong(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment keyPtr = tempArena.allocateFrom(key);
      final MemorySegment outValue = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result = NATIVE_BINDINGS.threadGetLong(nativeThreadHandle, keyPtr, outValue);

      if (result != 0) {
        throw new WasmException("Failed to get long value from thread-local storage");
      }

      return outValue.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public void putFloat(final String key, final float value) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment keyPtr = tempArena.allocateFrom(key);
      final int result = NATIVE_BINDINGS.threadPutFloat(nativeThreadHandle, keyPtr, value);

      if (result != 0) {
        throw new WasmException("Failed to put float value in thread-local storage");
      }
    }
  }

  @Override
  public float getFloat(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment keyPtr = tempArena.allocateFrom(key);
      final MemorySegment outValue = tempArena.allocate(ValueLayout.JAVA_FLOAT);

      final int result = NATIVE_BINDINGS.threadGetFloat(nativeThreadHandle, keyPtr, outValue);

      if (result != 0) {
        throw new WasmException("Failed to get float value from thread-local storage");
      }

      return outValue.get(ValueLayout.JAVA_FLOAT, 0);
    }
  }

  @Override
  public void putDouble(final String key, final double value) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment keyPtr = tempArena.allocateFrom(key);
      final int result = NATIVE_BINDINGS.threadPutDouble(nativeThreadHandle, keyPtr, value);

      if (result != 0) {
        throw new WasmException("Failed to put double value in thread-local storage");
      }
    }
  }

  @Override
  public double getDouble(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment keyPtr = tempArena.allocateFrom(key);
      final MemorySegment outValue = tempArena.allocate(ValueLayout.JAVA_DOUBLE);

      final int result = NATIVE_BINDINGS.threadGetDouble(nativeThreadHandle, keyPtr, outValue);

      if (result != 0) {
        throw new WasmException("Failed to get double value from thread-local storage");
      }

      return outValue.get(ValueLayout.JAVA_DOUBLE, 0);
    }
  }

  @Override
  public void putBytes(final String key, final byte[] value) throws WasmException {
    ensureNotClosed();
    validateKey(key);
    validateNotNull(value, "value");

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment keyPtr = tempArena.allocateFrom(key);
      final MemorySegment bytesPtr = tempArena.allocate(value.length);
      MemorySegment.copy(value, 0, bytesPtr, ValueLayout.JAVA_BYTE, 0, value.length);

      final int result =
          NATIVE_BINDINGS.threadPutBytes(nativeThreadHandle, keyPtr, bytesPtr, value.length);

      if (result != 0) {
        throw new WasmException("Failed to put byte array in thread-local storage");
      }
    }
  }

  @Override
  public byte[] getBytes(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment keyPtr = tempArena.allocateFrom(key);
      final MemorySegment outBytesPtr = tempArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outBytesLen = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          NATIVE_BINDINGS.threadGetBytes(nativeThreadHandle, keyPtr, outBytesPtr, outBytesLen);

      if (result != 0) {
        throw new WasmException("Failed to get byte array from thread-local storage");
      }

      final MemorySegment bytesPtr = outBytesPtr.get(ValueLayout.ADDRESS, 0);
      final long bytesLen = outBytesLen.get(ValueLayout.JAVA_LONG, 0);

      if (bytesPtr == null || bytesPtr.equals(MemorySegment.NULL) || bytesLen == 0) {
        return new byte[0];
      }

      final byte[] result_bytes = new byte[(int) bytesLen];
      MemorySegment.copy(bytesPtr, ValueLayout.JAVA_BYTE, 0, result_bytes, 0, (int) bytesLen);

      // Free the native memory allocated by the native function
      NATIVE_BINDINGS.free(bytesPtr);

      return result_bytes;
    }
  }

  @Override
  public void putString(final String key, final String value) throws WasmException {
    ensureNotClosed();
    validateKey(key);
    validateNotNull(value, "value");

    // Convert string to UTF-8 bytes for native storage
    final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    putBytes(key, bytes);
  }

  @Override
  public String getString(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    final byte[] bytes = getBytes(key);
    if (bytes == null) {
      return null;
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }

  @Override
  public boolean remove(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment keyPtr = tempArena.allocateFrom(key);
      final int result = NATIVE_BINDINGS.threadRemoveKey(nativeThreadHandle, keyPtr);

      if (result != 0) {
        throw new WasmException("Failed to remove key from thread-local storage");
      }
      return true;
    }
  }

  @Override
  public boolean contains(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment keyPtr = tempArena.allocateFrom(key);
      final MemorySegment outExists = tempArena.allocate(ValueLayout.JAVA_INT);

      final int result = NATIVE_BINDINGS.threadContainsKey(nativeThreadHandle, keyPtr, outExists);

      if (result != 0) {
        throw new WasmException("Failed to check if key exists in thread-local storage");
      }

      return outExists.get(ValueLayout.JAVA_INT, 0) != 0;
    }
  }

  @Override
  public void clear() throws WasmException {
    ensureNotClosed();

    final int result = NATIVE_BINDINGS.threadClearStorage(nativeThreadHandle);

    if (result != 0) {
      throw new WasmException("Failed to clear thread-local storage");
    }
  }

  @Override
  public int size() throws WasmException {
    ensureNotClosed();

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment outSize = tempArena.allocate(ValueLayout.JAVA_INT);

      final int result = NATIVE_BINDINGS.threadStorageSize(nativeThreadHandle, outSize);

      if (result != 0) {
        throw new WasmException("Failed to get thread-local storage size");
      }

      return outSize.get(ValueLayout.JAVA_INT, 0);
    }
  }

  @Override
  public long getMemoryUsage() throws WasmException {
    ensureNotClosed();

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment outMemoryUsage = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          NATIVE_BINDINGS.threadStorageMemoryUsage(nativeThreadHandle, outMemoryUsage);

      if (result != 0) {
        throw new WasmException("Failed to get thread-local storage memory usage");
      }

      return outMemoryUsage.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  /**
   * Close the thread-local storage and release native resources. This method is called
   * automatically when the associated thread is closed.
   */
  public void close() {
    if (!closed.compareAndSet(false, true)) {
      return; // Already closed
    }

    try {
      // Clear all thread-local data
      clear();
    } catch (final Exception ignored) {
      // Ignore cleanup errors
    }
  }

  /**
   * Ensures this storage has not been closed.
   *
   * @throws IllegalStateException if the storage has been closed
   */
  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("Thread-local storage has been closed");
    }
  }

  /**
   * Validates that a key is not null or empty.
   *
   * @param key the key to validate
   * @throws IllegalArgumentException if the key is null or empty
   */
  private void validateKey(final String key) {
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("Key cannot be null or empty");
    }
  }

  /**
   * Validates that an object is not null.
   *
   * @param obj the object to validate
   * @param name the parameter name for error messages
   * @throws IllegalArgumentException if the object is null
   */
  private void validateNotNull(final Object obj, final String name) {
    if (obj == null) {
      throw new IllegalArgumentException(name + " cannot be null");
    }
  }
}

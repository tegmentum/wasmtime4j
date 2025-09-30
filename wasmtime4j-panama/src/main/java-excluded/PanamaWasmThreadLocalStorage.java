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

  /** Native thread handle for storage operations. */
  private final MemorySegment nativeThreadHandle;

  /** Memory arena for resource management. */
  private final Arena arena;

  /** Whether this storage has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  static {
    PanamaNativeLoader.loadNativeLibrary();
  }

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

    try (final Arena callArena = Arena.ofConfined()) {
      final MemorySegment keySegment = callArena.allocateUtf8String(key);
      PanamaThreadingBindings.putThreadLocalInt(nativeThreadHandle, keySegment, value);
    } catch (final Exception e) {
      throw new WasmException("Failed to store int value: " + e.getMessage(), e);
    }
  }

  @Override
  public int getInt(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (final Arena callArena = Arena.ofConfined()) {
      final MemorySegment keySegment = callArena.allocateUtf8String(key);
      return PanamaThreadingBindings.getThreadLocalInt(nativeThreadHandle, keySegment);
    } catch (final Exception e) {
      throw new WasmException("Failed to retrieve int value: " + e.getMessage(), e);
    }
  }

  @Override
  public void putLong(final String key, final long value) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (final Arena callArena = Arena.ofConfined()) {
      final MemorySegment keySegment = callArena.allocateUtf8String(key);
      PanamaThreadingBindings.putThreadLocalLong(nativeThreadHandle, keySegment, value);
    } catch (final Exception e) {
      throw new WasmException("Failed to store long value: " + e.getMessage(), e);
    }
  }

  @Override
  public long getLong(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (final Arena callArena = Arena.ofConfined()) {
      final MemorySegment keySegment = callArena.allocateUtf8String(key);
      return PanamaThreadingBindings.getThreadLocalLong(nativeThreadHandle, keySegment);
    } catch (final Exception e) {
      throw new WasmException("Failed to retrieve long value: " + e.getMessage(), e);
    }
  }

  @Override
  public void putFloat(final String key, final float value) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (final Arena callArena = Arena.ofConfined()) {
      final MemorySegment keySegment = callArena.allocateUtf8String(key);
      PanamaThreadingBindings.putThreadLocalFloat(nativeThreadHandle, keySegment, value);
    } catch (final Exception e) {
      throw new WasmException("Failed to store float value: " + e.getMessage(), e);
    }
  }

  @Override
  public float getFloat(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (final Arena callArena = Arena.ofConfined()) {
      final MemorySegment keySegment = callArena.allocateUtf8String(key);
      return PanamaThreadingBindings.getThreadLocalFloat(nativeThreadHandle, keySegment);
    } catch (final Exception e) {
      throw new WasmException("Failed to retrieve float value: " + e.getMessage(), e);
    }
  }

  @Override
  public void putDouble(final String key, final double value) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (final Arena callArena = Arena.ofConfined()) {
      final MemorySegment keySegment = callArena.allocateUtf8String(key);
      PanamaThreadingBindings.putThreadLocalDouble(nativeThreadHandle, keySegment, value);
    } catch (final Exception e) {
      throw new WasmException("Failed to store double value: " + e.getMessage(), e);
    }
  }

  @Override
  public double getDouble(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (final Arena callArena = Arena.ofConfined()) {
      final MemorySegment keySegment = callArena.allocateUtf8String(key);
      return PanamaThreadingBindings.getThreadLocalDouble(nativeThreadHandle, keySegment);
    } catch (final Exception e) {
      throw new WasmException("Failed to retrieve double value: " + e.getMessage(), e);
    }
  }

  @Override
  public void putBytes(final String key, final byte[] value) throws WasmException {
    ensureNotClosed();
    validateKey(key);
    validateNotNull(value, "value");

    try (final Arena callArena = Arena.ofConfined()) {
      final MemorySegment keySegment = callArena.allocateUtf8String(key);
      final MemorySegment valueSegment = callArena.allocateArray(ValueLayout.JAVA_BYTE, value);

      PanamaThreadingBindings.putThreadLocalBytes(
          nativeThreadHandle, keySegment, valueSegment, value.length);
    } catch (final Exception e) {
      throw new WasmException("Failed to store byte array: " + e.getMessage(), e);
    }
  }

  @Override
  public byte[] getBytes(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (final Arena callArena = Arena.ofConfined()) {
      final MemorySegment keySegment = callArena.allocateUtf8String(key);

      // First, get the size of the data
      final long size =
          PanamaThreadingBindings.getThreadLocalBytesSize(nativeThreadHandle, keySegment);
      if (size == 0) {
        return null; // Key doesn't exist or empty data
      }

      // Allocate buffer for the data
      final MemorySegment dataSegment = callArena.allocateArray(ValueLayout.JAVA_BYTE, size);

      // Get the actual data
      final boolean success =
          PanamaThreadingBindings.getThreadLocalBytes(
              nativeThreadHandle, keySegment, dataSegment, size);

      if (success) {
        return dataSegment.toArray(ValueLayout.JAVA_BYTE);
      } else {
        return null;
      }

    } catch (final Exception e) {
      throw new WasmException("Failed to retrieve byte array: " + e.getMessage(), e);
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

    try (final Arena callArena = Arena.ofConfined()) {
      final MemorySegment keySegment = callArena.allocateUtf8String(key);
      return PanamaThreadingBindings.removeThreadLocal(nativeThreadHandle, keySegment);
    } catch (final Exception e) {
      throw new WasmException("Failed to remove value: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean contains(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try (final Arena callArena = Arena.ofConfined()) {
      final MemorySegment keySegment = callArena.allocateUtf8String(key);
      return PanamaThreadingBindings.containsThreadLocal(nativeThreadHandle, keySegment);
    } catch (final Exception e) {
      throw new WasmException("Failed to check key existence: " + e.getMessage(), e);
    }
  }

  @Override
  public void clear() throws WasmException {
    ensureNotClosed();

    try {
      PanamaThreadingBindings.clearThreadLocal(nativeThreadHandle);
    } catch (final Exception e) {
      throw new WasmException("Failed to clear storage: " + e.getMessage(), e);
    }
  }

  @Override
  public int size() throws WasmException {
    ensureNotClosed();

    try {
      return PanamaThreadingBindings.getThreadLocalSize(nativeThreadHandle);
    } catch (final Exception e) {
      throw new WasmException("Failed to get storage size: " + e.getMessage(), e);
    }
  }

  @Override
  public long getMemoryUsage() throws WasmException {
    ensureNotClosed();

    try {
      return PanamaThreadingBindings.getThreadLocalMemoryUsage(nativeThreadHandle);
    } catch (final Exception e) {
      throw new WasmException("Failed to get memory usage: " + e.getMessage(), e);
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

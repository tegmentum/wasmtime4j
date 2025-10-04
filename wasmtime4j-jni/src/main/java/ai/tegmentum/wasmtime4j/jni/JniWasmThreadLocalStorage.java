package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.WasmThreadLocalStorage;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JNI implementation of WebAssembly thread-local storage.
 *
 * <p>This implementation provides thread-local storage capabilities using JNI to interface with the
 * native Wasmtime threading runtime. Each WebAssembly thread maintains its own isolated storage
 * space that provides:
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
public final class JniWasmThreadLocalStorage implements WasmThreadLocalStorage {

  /** Native thread handle for storage operations. */
  private final long nativeThreadHandle;

  /** Whether this storage has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  static {
    // TODO: Load native library when JniLibraryLoader is implemented
    // JniLibraryLoader.loadNativeLibrary();
  }

  /**
   * Creates a new JNI thread-local storage instance.
   *
   * @param nativeThreadHandle native thread handle pointer
   * @throws WasmException if storage creation fails
   */
  public JniWasmThreadLocalStorage(final long nativeThreadHandle) throws WasmException {
    if (nativeThreadHandle == 0) {
      throw new WasmException("Native thread handle cannot be null");
    }

    this.nativeThreadHandle = nativeThreadHandle;
  }

  @Override
  public void putInt(final String key, final int value) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try {
      nativePutInt(nativeThreadHandle, key, value);
    } catch (final Exception e) {
      throw new WasmException("Failed to store int value: " + e.getMessage(), e);
    }
  }

  @Override
  public int getInt(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try {
      return nativeGetInt(nativeThreadHandle, key);
    } catch (final Exception e) {
      throw new WasmException("Failed to retrieve int value: " + e.getMessage(), e);
    }
  }

  @Override
  public void putLong(final String key, final long value) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try {
      nativePutLong(nativeThreadHandle, key, value);
    } catch (final Exception e) {
      throw new WasmException("Failed to store long value: " + e.getMessage(), e);
    }
  }

  @Override
  public long getLong(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try {
      return nativeGetLong(nativeThreadHandle, key);
    } catch (final Exception e) {
      throw new WasmException("Failed to retrieve long value: " + e.getMessage(), e);
    }
  }

  @Override
  public void putFloat(final String key, final float value) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try {
      nativePutFloat(nativeThreadHandle, key, value);
    } catch (final Exception e) {
      throw new WasmException("Failed to store float value: " + e.getMessage(), e);
    }
  }

  @Override
  public float getFloat(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try {
      return nativeGetFloat(nativeThreadHandle, key);
    } catch (final Exception e) {
      throw new WasmException("Failed to retrieve float value: " + e.getMessage(), e);
    }
  }

  @Override
  public void putDouble(final String key, final double value) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try {
      nativePutDouble(nativeThreadHandle, key, value);
    } catch (final Exception e) {
      throw new WasmException("Failed to store double value: " + e.getMessage(), e);
    }
  }

  @Override
  public double getDouble(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try {
      return nativeGetDouble(nativeThreadHandle, key);
    } catch (final Exception e) {
      throw new WasmException("Failed to retrieve double value: " + e.getMessage(), e);
    }
  }

  @Override
  public void putBytes(final String key, final byte[] value) throws WasmException {
    ensureNotClosed();
    validateKey(key);
    validateNotNull(value, "value");

    try {
      nativePutBytes(nativeThreadHandle, key, value);
    } catch (final Exception e) {
      throw new WasmException("Failed to store byte array: " + e.getMessage(), e);
    }
  }

  @Override
  public byte[] getBytes(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try {
      return nativeGetBytes(nativeThreadHandle, key);
    } catch (final Exception e) {
      throw new WasmException("Failed to retrieve byte array: " + e.getMessage(), e);
    }
  }

  @Override
  public void putString(final String key, final String value) throws WasmException {
    ensureNotClosed();
    validateKey(key);
    validateNotNull(value, "value");

    try {
      // Convert string to UTF-8 bytes for native storage
      final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
      nativePutBytes(nativeThreadHandle, key, bytes);
    } catch (final Exception e) {
      throw new WasmException("Failed to store string value: " + e.getMessage(), e);
    }
  }

  @Override
  public String getString(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try {
      final byte[] bytes = nativeGetBytes(nativeThreadHandle, key);
      if (bytes == null) {
        return null;
      }
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (final Exception e) {
      throw new WasmException("Failed to retrieve string value: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean remove(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try {
      return nativeRemove(nativeThreadHandle, key);
    } catch (final Exception e) {
      throw new WasmException("Failed to remove value: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean contains(final String key) throws WasmException {
    ensureNotClosed();
    validateKey(key);

    try {
      return nativeContains(nativeThreadHandle, key);
    } catch (final Exception e) {
      throw new WasmException("Failed to check key existence: " + e.getMessage(), e);
    }
  }

  @Override
  public void clear() throws WasmException {
    ensureNotClosed();

    try {
      nativeClear(nativeThreadHandle);
    } catch (final Exception e) {
      throw new WasmException("Failed to clear storage: " + e.getMessage(), e);
    }
  }

  @Override
  public int size() throws WasmException {
    ensureNotClosed();

    try {
      return nativeSize(nativeThreadHandle);
    } catch (final Exception e) {
      throw new WasmException("Failed to get storage size: " + e.getMessage(), e);
    }
  }

  @Override
  public long getMemoryUsage() throws WasmException {
    ensureNotClosed();

    try {
      return nativeGetMemoryUsage(nativeThreadHandle);
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
      // Storage is cleaned up automatically with the thread
      // No explicit cleanup needed for thread-local storage
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

  // Native method declarations

  /**
   * Store an integer value in thread-local storage.
   *
   * @param threadHandle native thread handle
   * @param key storage key
   * @param value value to store
   */
  private static native void nativePutInt(long threadHandle, String key, int value);

  /**
   * Retrieve an integer value from thread-local storage.
   *
   * @param threadHandle native thread handle
   * @param key storage key
   * @return stored value or 0 if not found
   */
  private static native int nativeGetInt(long threadHandle, String key);

  /**
   * Store a long value in thread-local storage.
   *
   * @param threadHandle native thread handle
   * @param key storage key
   * @param value value to store
   */
  private static native void nativePutLong(long threadHandle, String key, long value);

  /**
   * Retrieve a long value from thread-local storage.
   *
   * @param threadHandle native thread handle
   * @param key storage key
   * @return stored value or 0L if not found
   */
  private static native long nativeGetLong(long threadHandle, String key);

  /**
   * Store a float value in thread-local storage.
   *
   * @param threadHandle native thread handle
   * @param key storage key
   * @param value value to store
   */
  private static native void nativePutFloat(long threadHandle, String key, float value);

  /**
   * Retrieve a float value from thread-local storage.
   *
   * @param threadHandle native thread handle
   * @param key storage key
   * @return stored value or 0.0f if not found
   */
  private static native float nativeGetFloat(long threadHandle, String key);

  /**
   * Store a double value in thread-local storage.
   *
   * @param threadHandle native thread handle
   * @param key storage key
   * @param value value to store
   */
  private static native void nativePutDouble(long threadHandle, String key, double value);

  /**
   * Retrieve a double value from thread-local storage.
   *
   * @param threadHandle native thread handle
   * @param key storage key
   * @return stored value or 0.0 if not found
   */
  private static native double nativeGetDouble(long threadHandle, String key);

  /**
   * Store a byte array in thread-local storage.
   *
   * @param threadHandle native thread handle
   * @param key storage key
   * @param value byte array to store
   */
  private static native void nativePutBytes(long threadHandle, String key, byte[] value);

  /**
   * Retrieve a byte array from thread-local storage.
   *
   * @param threadHandle native thread handle
   * @param key storage key
   * @return stored byte array or null if not found
   */
  private static native byte[] nativeGetBytes(long threadHandle, String key);

  /**
   * Remove a value from thread-local storage.
   *
   * @param threadHandle native thread handle
   * @param key storage key
   * @return true if the key was removed, false if it didn't exist
   */
  private static native boolean nativeRemove(long threadHandle, String key);

  /**
   * Check if a key exists in thread-local storage.
   *
   * @param threadHandle native thread handle
   * @param key storage key
   * @return true if the key exists
   */
  private static native boolean nativeContains(long threadHandle, String key);

  /**
   * Clear all values from thread-local storage.
   *
   * @param threadHandle native thread handle
   */
  private static native void nativeClear(long threadHandle);

  /**
   * Get the number of entries in thread-local storage.
   *
   * @param threadHandle native thread handle
   * @return number of stored entries
   */
  private static native int nativeSize(long threadHandle);

  /**
   * Get the memory usage of thread-local storage.
   *
   * @param threadHandle native thread handle
   * @return memory usage in bytes
   */
  private static native long nativeGetMemoryUsage(long threadHandle);
}

package ai.tegmentum.wasmtime4j.concurrent;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Thread-local storage for WebAssembly threads.
 *
 * <p>Each WebAssembly thread maintains its own thread-local storage area that provides isolated
 * memory space not shared with other threads. This is useful for thread-specific data and temporary
 * storage.
 *
 * <p>Thread-local storage is separate from shared linear memory and provides:
 *
 * <ul>
 *   <li>Isolated memory space per thread
 *   <li>Fast access without synchronization
 *   <li>Automatic cleanup when thread terminates
 *   <li>Type-safe storage for various data types
 * </ul>
 *
 * @since 1.0.0
 */
public interface WasmThreadLocalStorage {

  /**
   * Stores a 32-bit integer value in thread-local storage.
   *
   * @param key the storage key
   * @param value the value to store
   * @throws WasmException if the storage operation fails
   * @throws IllegalArgumentException if the key is null or empty
   */
  void putInt(String key, int value) throws WasmException;

  /**
   * Retrieves a 32-bit integer value from thread-local storage.
   *
   * @param key the storage key
   * @return the stored value, or 0 if the key doesn't exist
   * @throws WasmException if the retrieval operation fails
   * @throws IllegalArgumentException if the key is null or empty
   */
  int getInt(String key) throws WasmException;

  /**
   * Stores a 64-bit long value in thread-local storage.
   *
   * @param key the storage key
   * @param value the value to store
   * @throws WasmException if the storage operation fails
   * @throws IllegalArgumentException if the key is null or empty
   */
  void putLong(String key, long value) throws WasmException;

  /**
   * Retrieves a 64-bit long value from thread-local storage.
   *
   * @param key the storage key
   * @return the stored value, or 0L if the key doesn't exist
   * @throws WasmException if the retrieval operation fails
   * @throws IllegalArgumentException if the key is null or empty
   */
  long getLong(String key) throws WasmException;

  /**
   * Stores a 32-bit float value in thread-local storage.
   *
   * @param key the storage key
   * @param value the value to store
   * @throws WasmException if the storage operation fails
   * @throws IllegalArgumentException if the key is null or empty
   */
  void putFloat(String key, float value) throws WasmException;

  /**
   * Retrieves a 32-bit float value from thread-local storage.
   *
   * @param key the storage key
   * @return the stored value, or 0.0f if the key doesn't exist
   * @throws WasmException if the retrieval operation fails
   * @throws IllegalArgumentException if the key is null or empty
   */
  float getFloat(String key) throws WasmException;

  /**
   * Stores a 64-bit double value in thread-local storage.
   *
   * @param key the storage key
   * @param value the value to store
   * @throws WasmException if the storage operation fails
   * @throws IllegalArgumentException if the key is null or empty
   */
  void putDouble(String key, double value) throws WasmException;

  /**
   * Retrieves a 64-bit double value from thread-local storage.
   *
   * @param key the storage key
   * @return the stored value, or 0.0 if the key doesn't exist
   * @throws WasmException if the retrieval operation fails
   * @throws IllegalArgumentException if the key is null or empty
   */
  double getDouble(String key) throws WasmException;

  /**
   * Stores a byte array in thread-local storage.
   *
   * @param key the storage key
   * @param value the byte array to store
   * @throws WasmException if the storage operation fails
   * @throws IllegalArgumentException if the key is null or empty, or value is null
   */
  void putBytes(String key, byte[] value) throws WasmException;

  /**
   * Retrieves a byte array from thread-local storage.
   *
   * @param key the storage key
   * @return the stored byte array, or null if the key doesn't exist
   * @throws WasmException if the retrieval operation fails
   * @throws IllegalArgumentException if the key is null or empty
   */
  byte[] getBytes(String key) throws WasmException;

  /**
   * Stores a string value in thread-local storage.
   *
   * @param key the storage key
   * @param value the string to store
   * @throws WasmException if the storage operation fails
   * @throws IllegalArgumentException if the key is null or empty, or value is null
   */
  void putString(String key, String value) throws WasmException;

  /**
   * Retrieves a string value from thread-local storage.
   *
   * @param key the storage key
   * @return the stored string, or null if the key doesn't exist
   * @throws WasmException if the retrieval operation fails
   * @throws IllegalArgumentException if the key is null or empty
   */
  String getString(String key) throws WasmException;

  /**
   * Removes a value from thread-local storage.
   *
   * @param key the storage key to remove
   * @return true if the key was removed, false if it didn't exist
   * @throws WasmException if the removal operation fails
   * @throws IllegalArgumentException if the key is null or empty
   */
  boolean remove(String key) throws WasmException;

  /**
   * Checks if a key exists in thread-local storage.
   *
   * @param key the storage key to check
   * @return true if the key exists, false otherwise
   * @throws WasmException if the check operation fails
   * @throws IllegalArgumentException if the key is null or empty
   */
  boolean contains(String key) throws WasmException;

  /**
   * Clears all values from thread-local storage.
   *
   * @throws WasmException if the clear operation fails
   */
  void clear() throws WasmException;

  /**
   * Gets the number of entries in thread-local storage.
   *
   * @return the number of stored entries
   * @throws WasmException if the size operation fails
   */
  int size() throws WasmException;

  /**
   * Gets the total memory usage of thread-local storage in bytes.
   *
   * @return the memory usage in bytes
   * @throws WasmException if the memory usage calculation fails
   */
  long getMemoryUsage() throws WasmException;
}

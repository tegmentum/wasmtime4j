/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

import java.util.Optional;
import java.util.Set;

/**
 * WASI-keyvalue interface providing key-value store operations.
 *
 * <p>This interface maps to the WASI Key-Value proposal ({@code wasi:keyvalue/store}) providing
 * core CRUD operations for key-value storage.
 *
 * @since 1.0.0
 */
public interface WasiKeyValue extends AutoCloseable {

  /**
   * Gets a value by key.
   *
   * @param key the key to retrieve
   * @return the value, or empty if not found
   * @throws KeyValueException if the get operation fails
   */
  Optional<byte[]> get(String key) throws KeyValueException;

  /**
   * Gets a value by key with metadata.
   *
   * @param key the key to retrieve
   * @return the entry with metadata, or empty if not found
   * @throws KeyValueException if the get operation fails
   */
  Optional<KeyValueEntry> getEntry(String key) throws KeyValueException;

  /**
   * Sets a value for a key.
   *
   * @param key the key to set
   * @param value the value to store
   * @throws KeyValueException if the set operation fails
   */
  void set(String key, byte[] value) throws KeyValueException;

  /**
   * Deletes a key.
   *
   * @param key the key to delete
   * @return true if the key was deleted, false if it didn't exist
   * @throws KeyValueException if the delete operation fails
   */
  boolean delete(String key) throws KeyValueException;

  /**
   * Checks if a key exists.
   *
   * @param key the key to check
   * @return true if the key exists
   * @throws KeyValueException if the check fails
   */
  boolean exists(String key) throws KeyValueException;

  /**
   * Lists all keys.
   *
   * @return set of all keys
   * @throws KeyValueException if the list operation fails
   */
  Set<String> keys() throws KeyValueException;

  /**
   * {@inheritDoc}
   *
   * <p>Closes the key-value store and releases resources.
   */
  @Override
  void close() throws KeyValueException;
}

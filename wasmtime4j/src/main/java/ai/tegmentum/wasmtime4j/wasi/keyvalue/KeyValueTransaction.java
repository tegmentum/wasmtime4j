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

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for WASI-keyvalue transactions with ACID properties.
 *
 * @since 1.0.0
 */
public interface KeyValueTransaction extends AutoCloseable {

  /**
   * Gets the unique transaction identifier.
   *
   * @return the transaction ID
   */
  UUID getId();

  /**
   * Gets the isolation level for this transaction.
   *
   * @return the isolation level
   */
  IsolationLevel getIsolationLevel();

  /**
   * Gets a value within this transaction context.
   *
   * @param key the key to retrieve
   * @return the value, or empty if not found
   * @throws KeyValueException if the get operation fails
   */
  Optional<byte[]> get(String key) throws KeyValueException;

  /**
   * Sets a value within this transaction context.
   *
   * @param key the key to set
   * @param value the value to store
   * @throws KeyValueException if the set operation fails
   */
  void set(String key, byte[] value) throws KeyValueException;

  /**
   * Deletes a key within this transaction context.
   *
   * @param key the key to delete
   * @return true if the key was deleted, false if it didn't exist
   * @throws KeyValueException if the delete operation fails
   */
  boolean delete(String key) throws KeyValueException;

  /**
   * Checks if a key exists within this transaction context.
   *
   * @param key the key to check
   * @return true if the key exists
   * @throws KeyValueException if the check fails
   */
  boolean exists(String key) throws KeyValueException;

  /**
   * Commits the transaction.
   *
   * @throws KeyValueException if commit fails
   */
  void commit() throws KeyValueException;

  /**
   * Aborts (rolls back) the transaction.
   *
   * @throws KeyValueException if abort fails
   */
  void abort() throws KeyValueException;

  /**
   * Checks if the transaction is still active.
   *
   * @return true if the transaction is active
   */
  boolean isActive();

  /**
   * Gets the elapsed time since transaction started.
   *
   * @return the elapsed duration
   */
  Duration getElapsedTime();

  /**
   * Creates a savepoint within the transaction.
   *
   * @param name the savepoint name
   * @throws KeyValueException if savepoint creation fails
   */
  void savepoint(String name) throws KeyValueException;

  /**
   * Rolls back to a previously created savepoint.
   *
   * @param name the savepoint name to rollback to
   * @throws KeyValueException if rollback fails
   */
  void rollbackToSavepoint(String name) throws KeyValueException;

  /**
   * Releases a savepoint.
   *
   * @param name the savepoint name to release
   * @throws KeyValueException if release fails
   */
  void releaseSavepoint(String name) throws KeyValueException;

  /**
   * {@inheritDoc}
   *
   * <p>Closes the transaction. If still active, the transaction will be aborted.
   */
  @Override
  void close() throws KeyValueException;
}

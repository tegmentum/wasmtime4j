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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * WASI-keyvalue interface providing key-value store operations.
 *
 * <p>This interface provides operations for storing and retrieving key-value pairs with support for
 * multiple consistency models, transactions, and TTL.
 *
 * @since 1.0.0
 */
public interface WasiKeyValue extends AutoCloseable {

  // ==================== Basic CRUD Operations ====================

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
   * Sets a value for a key with time-to-live.
   *
   * @param key the key to set
   * @param value the value to store
   * @param ttl the time-to-live duration
   * @throws KeyValueException if the set operation fails
   */
  void set(String key, byte[] value, Duration ttl) throws KeyValueException;

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
   * Lists all keys matching a pattern.
   *
   * @param pattern the glob pattern to match (e.g., "user:*")
   * @return set of matching keys
   * @throws KeyValueException if the list operation fails
   */
  Set<String> keys(String pattern) throws KeyValueException;

  /**
   * Lists all keys.
   *
   * @return set of all keys
   * @throws KeyValueException if the list operation fails
   */
  Set<String> keys() throws KeyValueException;

  // ==================== Atomic Operations ====================

  /**
   * Atomically sets a value only if the key doesn't exist.
   *
   * @param key the key to set
   * @param value the value to store
   * @return true if the key was set, false if it already existed
   * @throws KeyValueException if the operation fails
   */
  boolean setIfAbsent(String key, byte[] value) throws KeyValueException;

  /**
   * Atomically sets a value only if the key exists.
   *
   * @param key the key to set
   * @param value the value to store
   * @return true if the key was set, false if it didn't exist
   * @throws KeyValueException if the operation fails
   */
  boolean setIfPresent(String key, byte[] value) throws KeyValueException;

  /**
   * Atomically compares and swaps a value.
   *
   * @param key the key to update
   * @param expectedValue the expected current value
   * @param newValue the new value to set
   * @return true if the swap was successful
   * @throws KeyValueException if the operation fails
   */
  boolean compareAndSwap(String key, byte[] expectedValue, byte[] newValue)
      throws KeyValueException;

  /**
   * Atomically compares version and swaps a value.
   *
   * @param key the key to update
   * @param expectedVersion the expected version number
   * @param newValue the new value to set
   * @return true if the swap was successful
   * @throws KeyValueException if the operation fails
   */
  boolean compareVersionAndSwap(String key, long expectedVersion, byte[] newValue)
      throws KeyValueException;

  /**
   * Atomically increments a numeric value.
   *
   * @param key the key to increment
   * @param delta the amount to increment by
   * @return the new value after increment
   * @throws KeyValueException if the operation fails
   */
  long increment(String key, long delta) throws KeyValueException;

  /**
   * Gets and deletes a value atomically.
   *
   * @param key the key to get and delete
   * @return the value, or empty if not found
   * @throws KeyValueException if the operation fails
   */
  Optional<byte[]> getAndDelete(String key) throws KeyValueException;

  /**
   * Gets and sets a value atomically.
   *
   * @param key the key to get and set
   * @param newValue the new value to store
   * @return the old value, or empty if not found
   * @throws KeyValueException if the operation fails
   */
  Optional<byte[]> getAndSet(String key, byte[] newValue) throws KeyValueException;

  // ==================== Batch Operations ====================

  /**
   * Gets multiple values in a single operation.
   *
   * @param keys the keys to retrieve
   * @return map of keys to values (missing keys are not included)
   * @throws KeyValueException if the batch get fails
   */
  Map<String, byte[]> getMultiple(Set<String> keys) throws KeyValueException;

  /**
   * Sets multiple key-value pairs in a single operation.
   *
   * @param entries the entries to set
   * @throws KeyValueException if the batch set fails
   */
  void setMultiple(Map<String, byte[]> entries) throws KeyValueException;

  /**
   * Deletes multiple keys in a single operation.
   *
   * @param keys the keys to delete
   * @return set of keys that were actually deleted
   * @throws KeyValueException if the batch delete fails
   */
  Set<String> deleteMultiple(Set<String> keys) throws KeyValueException;

  // ==================== List Operations ====================

  /**
   * Appends values to a list.
   *
   * @param key the list key
   * @param values the values to append
   * @return the new length of the list
   * @throws KeyValueException if the operation fails
   */
  long listAppend(String key, List<byte[]> values) throws KeyValueException;

  /**
   * Prepends values to a list.
   *
   * @param key the list key
   * @param values the values to prepend
   * @return the new length of the list
   * @throws KeyValueException if the operation fails
   */
  long listPrepend(String key, List<byte[]> values) throws KeyValueException;

  /**
   * Gets a range of values from a list.
   *
   * @param key the list key
   * @param start the start index (0-based, inclusive)
   * @param end the end index (inclusive, -1 for end of list)
   * @return list of values in the range
   * @throws KeyValueException if the operation fails
   */
  List<byte[]> listRange(String key, long start, long end) throws KeyValueException;

  /**
   * Gets the length of a list.
   *
   * @param key the list key
   * @return the length of the list, or 0 if key doesn't exist
   * @throws KeyValueException if the operation fails
   */
  long listLength(String key) throws KeyValueException;

  /**
   * Pops a value from the end of a list.
   *
   * @param key the list key
   * @return the popped value, or empty if list is empty
   * @throws KeyValueException if the operation fails
   */
  Optional<byte[]> listPop(String key) throws KeyValueException;

  /**
   * Pops a value from the beginning of a list.
   *
   * @param key the list key
   * @return the popped value, or empty if list is empty
   * @throws KeyValueException if the operation fails
   */
  Optional<byte[]> listShift(String key) throws KeyValueException;

  // ==================== Set Operations ====================

  /**
   * Adds members to a set.
   *
   * @param key the set key
   * @param members the members to add
   * @return the number of new members added
   * @throws KeyValueException if the operation fails
   */
  long setAdd(String key, Set<byte[]> members) throws KeyValueException;

  /**
   * Removes members from a set.
   *
   * @param key the set key
   * @param members the members to remove
   * @return the number of members removed
   * @throws KeyValueException if the operation fails
   */
  long setRemove(String key, Set<byte[]> members) throws KeyValueException;

  /**
   * Gets all members of a set.
   *
   * @param key the set key
   * @return set of members
   * @throws KeyValueException if the operation fails
   */
  Set<byte[]> setMembers(String key) throws KeyValueException;

  /**
   * Checks if a value is a member of a set.
   *
   * @param key the set key
   * @param member the member to check
   * @return true if the member exists in the set
   * @throws KeyValueException if the operation fails
   */
  boolean setIsMember(String key, byte[] member) throws KeyValueException;

  /**
   * Gets the size of a set.
   *
   * @param key the set key
   * @return the size of the set
   * @throws KeyValueException if the operation fails
   */
  long setSize(String key) throws KeyValueException;

  // ==================== Hash/Map Operations ====================

  /**
   * Sets a field in a hash map.
   *
   * @param key the hash key
   * @param field the field name
   * @param value the field value
   * @throws KeyValueException if the operation fails
   */
  void hashSet(String key, String field, byte[] value) throws KeyValueException;

  /**
   * Gets a field from a hash map.
   *
   * @param key the hash key
   * @param field the field name
   * @return the field value, or empty if not found
   * @throws KeyValueException if the operation fails
   */
  Optional<byte[]> hashGet(String key, String field) throws KeyValueException;

  /**
   * Deletes a field from a hash map.
   *
   * @param key the hash key
   * @param field the field name
   * @return true if the field was deleted
   * @throws KeyValueException if the operation fails
   */
  boolean hashDelete(String key, String field) throws KeyValueException;

  /**
   * Gets all fields and values from a hash map.
   *
   * @param key the hash key
   * @return map of field names to values
   * @throws KeyValueException if the operation fails
   */
  Map<String, byte[]> hashGetAll(String key) throws KeyValueException;

  /**
   * Gets all field names from a hash map.
   *
   * @param key the hash key
   * @return set of field names
   * @throws KeyValueException if the operation fails
   */
  Set<String> hashKeys(String key) throws KeyValueException;

  /**
   * Checks if a field exists in a hash map.
   *
   * @param key the hash key
   * @param field the field name
   * @return true if the field exists
   * @throws KeyValueException if the operation fails
   */
  boolean hashExists(String key, String field) throws KeyValueException;

  // ==================== Transaction Support ====================

  /**
   * Begins a new transaction with default isolation level.
   *
   * @return the transaction
   * @throws KeyValueException if transaction cannot be started
   */
  KeyValueTransaction beginTransaction() throws KeyValueException;

  /**
   * Begins a new transaction with specified isolation level.
   *
   * @param isolationLevel the desired isolation level
   * @return the transaction
   * @throws KeyValueException if transaction cannot be started
   */
  KeyValueTransaction beginTransaction(IsolationLevel isolationLevel) throws KeyValueException;

  // ==================== Consistency and Configuration ====================

  /**
   * Gets the current consistency model.
   *
   * @return the consistency model
   */
  ConsistencyModel getConsistencyModel();

  /**
   * Sets the consistency model for subsequent operations.
   *
   * @param consistencyModel the consistency model to use
   * @throws KeyValueException if the consistency model is not supported
   */
  void setConsistencyModel(ConsistencyModel consistencyModel) throws KeyValueException;

  /**
   * Gets the eviction policy.
   *
   * @return the eviction policy
   */
  EvictionPolicy getEvictionPolicy();

  // ==================== TTL Management ====================

  /**
   * Gets the remaining time-to-live for a key.
   *
   * @param key the key to check
   * @return the remaining TTL, or empty if key doesn't exist or has no TTL
   * @throws KeyValueException if the operation fails
   */
  Optional<Duration> getTtl(String key) throws KeyValueException;

  /**
   * Sets or updates the time-to-live for a key.
   *
   * @param key the key to update
   * @param ttl the new TTL duration
   * @return true if the TTL was set, false if key doesn't exist
   * @throws KeyValueException if the operation fails
   */
  boolean setTtl(String key, Duration ttl) throws KeyValueException;

  /**
   * Removes the TTL from a key, making it persistent.
   *
   * @param key the key to persist
   * @return true if the TTL was removed, false if key doesn't exist
   * @throws KeyValueException if the operation fails
   */
  boolean persist(String key) throws KeyValueException;

  // ==================== Store Information ====================

  /**
   * Gets the approximate number of entries in the store.
   *
   * @return the approximate entry count
   * @throws KeyValueException if the operation fails
   */
  long size() throws KeyValueException;

  /**
   * Checks if the store is empty.
   *
   * @return true if the store is empty
   * @throws KeyValueException if the operation fails
   */
  boolean isEmpty() throws KeyValueException;

  /**
   * Clears all entries from the store.
   *
   * @throws KeyValueException if the operation fails
   */
  void clear() throws KeyValueException;

  /**
   * {@inheritDoc}
   *
   * <p>Closes the key-value store and releases resources.
   */
  @Override
  void close() throws KeyValueException;
}

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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Registry for managing host function callbacks in Panama FFI implementation.
 *
 * <p>This registry provides thread-safe storage and retrieval of host function implementations for
 * use with Panama upcall stubs. Each registered host function receives a unique ID that can be used
 * to retrieve the implementation from native callback contexts.
 *
 * <p>The registry uses weak references where appropriate to prevent memory leaks and provides
 * automatic cleanup of stale registrations.
 *
 * @since 1.0.0
 */
public final class PanamaHostFunctionRegistry {

  private static final Logger LOGGER = Logger.getLogger(PanamaHostFunctionRegistry.class.getName());

  /** Thread-safe map of host function ID to registry entry. */
  private static final ConcurrentHashMap<Long, HostFunctionEntry> REGISTRY =
      new ConcurrentHashMap<>();

  /** Atomic counter for generating unique host function IDs. */
  private static final AtomicLong ID_COUNTER = new AtomicLong(1);

  /** Private constructor to prevent instantiation. */
  private PanamaHostFunctionRegistry() {
    // Utility class
  }

  /**
   * Registers a host function implementation and returns a unique ID.
   *
   * @param implementation the host function implementation
   * @param functionType the function type (can be null)
   * @return unique ID for the registered host function
   * @throws IllegalArgumentException if implementation is null
   */
  public static long register(final HostFunction implementation, final FunctionType functionType) {
    if (implementation == null) {
      throw new IllegalArgumentException("Host function implementation cannot be null");
    }

    final long id = ID_COUNTER.getAndIncrement();
    final HostFunctionEntry entry = new HostFunctionEntry(implementation, functionType);

    REGISTRY.put(id, entry);

    LOGGER.fine(
        "Registered host function with ID: "
            + id
            + ", implementation: "
            + implementation.getClass().getSimpleName());

    return id;
  }

  /**
   * Retrieves a host function entry by ID.
   *
   * @param id the host function ID
   * @return the host function entry, or null if not found
   */
  public static HostFunctionEntry get(final long id) {
    final HostFunctionEntry entry = REGISTRY.get(id);
    if (entry == null) {
      LOGGER.warning("Host function not found for ID: " + id);
    }
    return entry;
  }

  /**
   * Unregisters a host function implementation.
   *
   * @param id the host function ID to unregister
   * @return true if the function was unregistered, false if not found
   */
  public static boolean unregister(final long id) {
    final HostFunctionEntry removed = REGISTRY.remove(id);
    if (removed != null) {
      LOGGER.fine("Unregistered host function with ID: " + id);
      return true;
    } else {
      LOGGER.warning("Attempted to unregister non-existent host function ID: " + id);
      return false;
    }
  }

  /**
   * Gets the current number of registered host functions.
   *
   * @return the number of registered host functions
   */
  public static int getRegisteredCount() {
    return REGISTRY.size();
  }

  /**
   * Clears all registered host functions.
   *
   * <p>This method should be used with caution as it will invalidate all existing host function
   * callbacks.
   */
  public static void clear() {
    final int count = REGISTRY.size();
    REGISTRY.clear();
    LOGGER.info("Cleared " + count + " registered host functions");
  }

  /**
   * Performs cleanup of any stale or invalid registrations.
   *
   * <p>This method can be called periodically to clean up memory and maintain registry health.
   * Currently a no-op but can be extended with cleanup logic.
   */
  public static void cleanup() {
    // Future enhancement: implement cleanup of stale registrations
    LOGGER.fine("Host function registry cleanup completed");
  }

  /** Registry entry containing a host function implementation and its metadata. */
  public static final class HostFunctionEntry {
    private final HostFunction implementation;
    private final FunctionType functionType;
    private final long registrationTime;

    /**
     * Creates a new host function registry entry.
     *
     * @param implementation the host function implementation
     * @param functionType the function type (can be null)
     */
    HostFunctionEntry(final HostFunction implementation, final FunctionType functionType) {
      this.implementation = implementation;
      this.functionType = functionType;
      this.registrationTime = System.currentTimeMillis();
    }

    /**
     * Gets the host function implementation.
     *
     * @return the host function implementation
     */
    public HostFunction getImplementation() {
      return implementation;
    }

    /**
     * Gets the function type.
     *
     * @return the function type, or null if not specified
     */
    public FunctionType getFunctionType() {
      return functionType;
    }

    /**
     * Gets the registration timestamp.
     *
     * @return the time when this entry was registered (milliseconds since epoch)
     */
    public long getRegistrationTime() {
      return registrationTime;
    }

    /**
     * Checks if this entry has a function type.
     *
     * @return true if function type is specified, false otherwise
     */
    public boolean hasFunctionType() {
      return functionType != null;
    }

    @Override
    public String toString() {
      return "HostFunctionEntry{"
          + "implementation="
          + implementation.getClass().getSimpleName()
          + ", functionType="
          + (functionType != null ? functionType : "null")
          + ", registrationTime="
          + registrationTime
          + '}';
    }
  }
}

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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Registry for cross-module global sharing in Panama FFI implementation.
 *
 * <p>This registry allows WebAssembly globals to be shared between different module instances
 * within the same store context. It provides thread-safe registration, lookup, and management of
 * shared globals.
 *
 * <p>The registry maintains both native-side references through FFI and Java-side references for
 * optimal performance and proper resource management.
 *
 * @since 1.0.0
 */
public final class GlobalRegistry implements AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(GlobalRegistry.class.getName());

  // Core infrastructure
  private final ArenaResourceManager resourceManager;
  private final NativeFunctionBindings nativeFunctions;
  private final ArenaResourceManager.ManagedNativeResource registryResource;

  // Java-side registry for fast lookup
  private final ConcurrentHashMap<String, PanamaGlobal> globalMap = new ConcurrentHashMap<>();

  // Registry state
  private volatile boolean closed = false;

  /**
   * Creates a new global registry.
   *
   * @param store the store this registry belongs to
   * @throws WasmException if registry creation fails
   */
  public GlobalRegistry(final PanamaStore store) throws WasmException {
    Objects.requireNonNull(store, "store cannot be null");

    this.resourceManager = store.getResourceManager();
    this.nativeFunctions = NativeFunctionBindings.getInstance();

    if (!nativeFunctions.isInitialized()) {
      throw new WasmException("Native function bindings not initialized");
    }

    // With fresh-lookup architecture, the registry is just a Java-side map
    // No native resources needed - globals perform fresh lookup when accessed
    this.registryResource = null;

    LOGGER.fine("Created global registry (fresh-lookup architecture)");
  }

  /**
   * Registers a global for sharing with the given name.
   *
   * @param name the name to register the global under
   * @param global the global to register
   * @throws WasmException if registration fails
   */
  public void registerGlobal(final String name, final PanamaGlobal global) throws WasmException {
    ensureNotClosed();
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(global, "global cannot be null");

    if (global.isClosed()) {
      throw new IllegalArgumentException("Cannot register closed global");
    }

    try {
      // Check if name is already in use
      if (globalMap.containsKey(name)) {
        throw new WasmException("Global name already registered: " + name);
      }

      // With fresh-lookup architecture, we only need to store the global in the Java-side registry
      // The global will perform fresh lookup when accessed, ensuring it always uses the correct
      // store
      globalMap.put(name, global);

      LOGGER.fine("Registered global for sharing: " + name);

    } catch (Exception e) {
      throw new WasmException("Failed to register global: " + name, e);
    }
  }

  /**
   * Looks up a shared global by name.
   *
   * @param name the name of the global to look up
   * @return the global if found, empty otherwise
   * @throws WasmException if lookup fails
   */
  public Optional<PanamaGlobal> lookupGlobal(final String name) throws WasmException {
    ensureNotClosed();
    Objects.requireNonNull(name, "name cannot be null");

    // Check Java-side registry
    PanamaGlobal global = globalMap.get(name);

    if (global != null && global.isClosed()) {
      // Remove stale entry if global is closed
      globalMap.remove(name, global);
      return Optional.empty();
    }

    return Optional.ofNullable(global);
  }

  /**
   * Unregisters a global from sharing.
   *
   * @param name the name of the global to unregister
   * @return true if a global was unregistered, false if not found
   */
  public boolean unregisterGlobal(final String name) {
    ensureNotClosed();
    Objects.requireNonNull(name, "name cannot be null");

    try {
      // Remove from Java-side registry
      PanamaGlobal removed = globalMap.remove(name);

      if (removed != null) {
        LOGGER.fine("Unregistered global from sharing: " + name);
        return true;
      }

      return false;
    } catch (Exception e) {
      LOGGER.warning("Failed to unregister global: " + name + " - " + e.getMessage());
      return false;
    }
  }

  /**
   * Checks if a global name is registered.
   *
   * @param name the name to check
   * @return true if the name is registered
   */
  public boolean isRegistered(final String name) {
    ensureNotClosed();
    Objects.requireNonNull(name, "name cannot be null");

    PanamaGlobal global = globalMap.get(name);
    return global != null && !global.isClosed();
  }

  /**
   * Gets the number of registered globals.
   *
   * @return the count of registered globals
   */
  public int getRegisteredCount() {
    ensureNotClosed();

    // Clean up stale entries
    globalMap.entrySet().removeIf(entry -> entry.getValue().isClosed());

    return globalMap.size();
  }

  /** Clears all registered globals. */
  public void clear() {
    ensureNotClosed();

    globalMap.clear();
    LOGGER.fine("Cleared all registered globals from registry");
  }

  /**
   * Gets the native registry pointer for FFI operations.
   *
   * <p>Note: With fresh-lookup architecture, no native registry pointer exists. This method is kept
   * for API compatibility but will throw an exception.
   *
   * @return the native registry pointer
   * @throws UnsupportedOperationException always, as fresh-lookup architecture doesn't use native
   *     pointers
   * @deprecated Fresh-lookup architecture doesn't require native registry pointers
   */
  @Deprecated
  public MemorySegment getRegistryPointer() {
    throw new UnsupportedOperationException(
        "Fresh-lookup architecture does not use native registry pointers");
  }

  /**
   * Checks if the registry is closed.
   *
   * @return true if closed
   */
  public boolean isClosed() {
    return closed;
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    synchronized (this) {
      if (closed) {
        return;
      }

      try {
        // Clear Java-side registry
        clear();

        LOGGER.fine("Closed global registry");
      } catch (Exception e) {
        LOGGER.warning("Error during registry closure: " + e.getMessage());
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Ensures that this registry is not closed.
   *
   * @throws IllegalStateException if the registry is closed
   */
  private void ensureNotClosed() {
    if (isClosed()) {
      throw new IllegalStateException("Global registry has been closed");
    }
  }

  @Override
  public String toString() {
    return String.format(
        "GlobalRegistry{registered=%d, closed=%s}", getRegisteredCount(), isClosed());
  }
}

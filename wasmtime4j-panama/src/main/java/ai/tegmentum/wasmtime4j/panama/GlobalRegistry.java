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
 * within the same store context. It provides thread-safe registration, lookup, and management
 * of shared globals.
 *
 * <p>The registry maintains both native-side references through FFI and Java-side references
 * for optimal performance and proper resource management.
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

    try {
      // Create native registry resource
      MemorySegment registryPtr = createNativeRegistry();
      PanamaErrorHandler.requireValidPointer(registryPtr, "registryPtr");

      this.registryResource = resourceManager.manageNativeResource(
          registryPtr, 
          () -> destroyNativeRegistryInternal(registryPtr), 
          "Global Registry");

      LOGGER.fine("Created global registry with native resource");

    } catch (Exception e) {
      throw new WasmException("Failed to create global registry", e);
    }
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

      // Register through native FFI
      ArenaResourceManager.ManagedMemorySegment nameSegment = 
          resourceManager.allocateString(name);

      int result = nativeFunctions.globalRegisterShared(
          global.getGlobalHandle(),
          nameSegment.getSegment(),
          registryResource.getNativePointer());

      PanamaErrorHandler.safeCheckError(
          result, "Global registration", "Failed to register global: " + name);

      // Add to Java-side registry
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

    try {
      // First check Java-side registry for fast lookup
      PanamaGlobal cached = globalMap.get(name);
      if (cached != null && !cached.isClosed()) {
        return Optional.of(cached);
      }

      // Remove stale entry if global is closed
      if (cached != null && cached.isClosed()) {
        globalMap.remove(name, cached);
      }

      // Fallback to native lookup
      ArenaResourceManager.ManagedMemorySegment nameSegment = 
          resourceManager.allocateString(name);

      MemorySegment globalPtr = nativeFunctions.globalLookupShared(
          nameSegment.getSegment(),
          registryResource.getNativePointer());

      if (globalPtr == null || globalPtr.equals(MemorySegment.NULL)) {
        return Optional.empty();
      }

      // Create Panama global wrapper for native global
      // Note: This requires the parent instance context, which would need to be tracked
      LOGGER.warning("Native global lookup found global but cannot create wrapper without instance context");
      return Optional.empty();

    } catch (Exception e) {
      throw new WasmException("Failed to lookup global: " + name, e);
    }
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

  /**
   * Clears all registered globals.
   */
  public void clear() {
    ensureNotClosed();
    
    globalMap.clear();
    LOGGER.fine("Cleared all registered globals from registry");
  }

  /**
   * Gets the native registry pointer for FFI operations.
   *
   * @return the native registry pointer
   * @throws IllegalStateException if the registry is closed
   */
  public MemorySegment getRegistryPointer() {
    ensureNotClosed();
    return registryResource.getNativePointer();
  }

  /**
   * Checks if the registry is closed.
   *
   * @return true if closed
   */
  public boolean isClosed() {
    return closed || registryResource.isClosed();
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

        // Close native registry resource
        registryResource.close();

        LOGGER.fine("Closed global registry");
      } catch (Exception e) {
        LOGGER.warning("Error during registry closure: " + e.getMessage());
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Creates the native registry resource.
   *
   * @return the native registry pointer
   * @throws WasmException if creation fails
   */
  private MemorySegment createNativeRegistry() throws WasmException {
    try {
      // Allocate memory for registry pointer
      ArenaResourceManager.ManagedMemorySegment registryOutPtr = 
          resourceManager.allocate(MemoryLayouts.C_POINTER);

      // For now, we'll use a simple memory segment as the registry handle
      // In a real implementation, this would create a native registry structure
      MemorySegment registryPtr = resourceManager.allocate(64).getSegment(); // 64-byte registry structure

      // Store the registry pointer in the output parameter
      MemoryLayouts.C_POINTER.varHandle().set(registryOutPtr.getSegment(), 0, registryPtr);

      LOGGER.fine("Created native global registry");
      return registryPtr;

    } catch (Exception e) {
      throw new WasmException("Failed to create native registry", e);
    }
  }

  /**
   * Internal cleanup method for native registry destruction.
   *
   * @param registryPtr the native registry pointer to destroy
   */
  private void destroyNativeRegistryInternal(final MemorySegment registryPtr) {
    try {
      // In a real implementation, this would call native cleanup functions
      LOGGER.fine("Destroyed native registry: " + registryPtr);
    } catch (Exception e) {
      // Log but don't throw during cleanup
      LOGGER.warning("Error during native registry cleanup: " + e.getMessage());
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
    return String.format("GlobalRegistry{registered=%d, closed=%s}", 
        getRegisteredCount(), isClosed());
  }
}
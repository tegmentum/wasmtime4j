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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Panama FFI implementation wrapper for WebAssembly Component operations.
 *
 * <p>This class provides a bridge between the Java component model API and the native Rust
 * implementation via Panama Foreign Function API calls. It handles component engine management,
 * component loading, instantiation, and resource cleanup using Arena-based resource management.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Arena-based automatic resource management with {@link ArenaResourceManager}
 *   <li>Defensive programming to prevent JVM crashes
 *   <li>Comprehensive parameter validation
 *   <li>Zero-copy optimized memory access where possible
 *   <li>Component lifecycle management
 * </ul>
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * try (ArenaResourceManager resourceManager = new ArenaResourceManager()) {
 *   PanamaComponentEngine engine = PanamaComponent.createComponentEngine(resourceManager);
 *   PanamaComponentHandle component = engine.loadComponentFromBytes(wasmBytes);
 *   PanamaComponentInstanceHandle instance = engine.instantiateComponent(component);
 *   // Use the instance...
 * }
 * }</pre>
 *
 * <p>This implementation uses Panama FFI for optimal performance and integrates with the
 * arena-based resource management system for automatic cleanup.
 *
 * @since 1.0.0
 */
public final class PanamaComponent {

  private static final Logger LOGGER = Logger.getLogger(PanamaComponent.class.getName());

  /** Prevent instantiation - this class contains only static factory methods. */
  private PanamaComponent() {}

  /**
   * Creates a new component engine with arena-based resource management.
   *
   * <p>The component engine manages component loading, instantiation, and lifecycle. It should be
   * reused for multiple components to amortize initialization costs.
   *
   * @param resourceManager the arena resource manager for lifecycle management
   * @return a new component engine instance
   * @throws WasmException if engine creation fails
   */
  public static PanamaComponentEngine createComponentEngine(
      final ArenaResourceManager resourceManager) throws WasmException {
    Objects.requireNonNull(resourceManager, "Resource manager cannot be null");

    try {
      return new PanamaComponentEngine(resourceManager);
    } catch (final Exception e) {
      throw new WasmException("Failed to create component engine", e);
    }
  }

  /**
   * Panama FFI wrapper for component engine operations.
   *
   * <p>Manages the lifecycle of a native component engine and provides methods for loading and
   * instantiating components using Panama FFI. Implements automatic resource cleanup through {@link
   * ArenaResourceManager}.
   */
  public static final class PanamaComponentEngine implements AutoCloseable {

    private final ArenaResourceManager resourceManager;
    private final NativeComponentBindings nativeFunctions;
    private final ArenaResourceManager.ManagedNativeResource engineResource;
    private final NativeResourceHandle resourceHandle;

    /**
     * Creates a new component engine wrapper with arena-based resource management.
     *
     * @param resourceManager the arena resource manager for lifecycle management
     * @throws WasmException if engine creation fails
     */
    PanamaComponentEngine(final ArenaResourceManager resourceManager) throws WasmException {
      this.resourceManager =
          Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
      this.nativeFunctions = NativeComponentBindings.getInstance();

      if (!nativeFunctions.isInitialized()) {
        throw new WasmException("Native function bindings not initialized");
      }

      try {
        // Create the native component engine through FFI
        MemorySegment enginePtr = createNativeComponentEngine();
        PanamaValidation.requireValidHandle(enginePtr, "enginePtr");

        // Create managed resource with cleanup
        this.engineResource =
            resourceManager.manageNativeResource(
                enginePtr,
                () -> destroyNativeComponentEngine(enginePtr),
                "Wasmtime Component Engine");

        this.resourceHandle =
            new NativeResourceHandle(
                "PanamaComponentEngine",
                () -> {
                  engineResource.close();
                  LOGGER.fine("Closed Panama component engine");
                });

        LOGGER.fine("Created Panama component engine instance with managed resource");

      } catch (Exception e) {
        throw new WasmException("Failed to create component engine", e);
      }
    }

    /**
     * Loads a component from WebAssembly bytes.
     *
     * <p>This method validates and compiles the provided WebAssembly component bytes into a
     * component that can be instantiated and executed.
     *
     * @param wasmBytes the WebAssembly component bytes to load
     * @return a component handle wrapper
     * @throws WasmException if loading fails
     */
    public PanamaComponentHandle loadComponentFromBytes(final byte[] wasmBytes)
        throws WasmException {
      ensureNotClosed();

      // Parameter validation with defensive programming
      Objects.requireNonNull(wasmBytes, "WebAssembly bytes cannot be null");
      Validation.requirePositive(wasmBytes.length, "wasmBytes.length");

      try {
        // Allocate memory segment for WASM bytes with zero-copy approach
        ArenaResourceManager.ManagedMemorySegment wasmMemory =
            resourceManager.allocate(wasmBytes.length);

        // Copy bytes into native memory
        ByteBuffer wasmBuffer = wasmMemory.segment().asByteBuffer();
        wasmBuffer.put(wasmBytes);

        // Load component through FFI
        MemorySegment componentPtr =
            loadNativeComponentFromBytes(engineResource.resource(), wasmMemory.segment());
        PanamaValidation.requireValidHandle(componentPtr, "componentPtr");

        return new PanamaComponentHandle(resourceManager, componentPtr);

      } catch (Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Failed to load component from bytes", e);
      }
    }

    /**
     * Instantiates a component.
     *
     * <p>Creates a new instance of the specified component that can be used to call exported
     * functions and interact with component resources.
     *
     * @param component the component to instantiate
     * @return a component instance handle wrapper
     * @throws WasmException if instantiation fails
     */
    public PanamaComponentInstanceHandle instantiateComponent(final PanamaComponentHandle component)
        throws WasmException {
      Objects.requireNonNull(component, "Component cannot be null");
      ensureNotClosed();
      component.ensureNotClosed();

      try {
        // Instantiate component through FFI
        MemorySegment instancePtr =
            instantiateNativeComponent(engineResource.resource(), component.getResource());
        PanamaValidation.requireValidHandle(instancePtr, "instancePtr");

        return new PanamaComponentInstanceHandle(resourceManager, instancePtr);

      } catch (Exception e) {
        if (e instanceof WasmException) {
          throw e;
        }
        throw new WasmException("Failed to instantiate component", e);
      }
    }

    /**
     * Gets the number of active component instances.
     *
     * @return the number of active instances
     * @throws WasmException if the operation fails
     */
    public int getActiveInstancesCount() throws WasmException {
      ensureNotClosed();

      try {
        return getActiveInstancesCountNative(engineResource.resource());
      } catch (final Exception e) {
        throw new WasmException("Failed to get active instances count", e);
      }
    }

    /**
     * Cleans up inactive component instances.
     *
     * <p>Removes references to component instances that are no longer active, freeing up resources
     * and preventing memory leaks.
     *
     * @return the number of instances that were cleaned up
     * @throws WasmException if the operation fails
     */
    public int cleanupInstances() throws WasmException {
      ensureNotClosed();

      try {
        return cleanupInstancesNative(engineResource.resource());
      } catch (final Exception e) {
        throw new WasmException("Failed to cleanup instances", e);
      }
    }

    /**
     * Checks if this engine is still valid and usable.
     *
     * @return true if the engine is valid, false if closed
     */
    public boolean isValid() {
      return !resourceHandle.isClosed() && engineResource.isValid();
    }

    private void ensureNotClosed() {
      resourceHandle.ensureNotClosed();
    }

    @Override
    public void close() {
      resourceHandle.close();
    }

    // Native method implementations using Panama FFI

    private MemorySegment createNativeComponentEngine() throws WasmException {
      try {
        return nativeFunctions.createComponentEngine();
      } catch (Exception e) {
        throw new WasmException("Failed to create native component engine", e);
      }
    }

    private MemorySegment loadNativeComponentFromBytes(
        MemorySegment enginePtr, MemorySegment wasmBytes) throws WasmException {
      try {
        return nativeFunctions.loadComponentFromBytes(enginePtr, wasmBytes);
      } catch (Exception e) {
        throw new WasmException("Failed to load component from bytes", e);
      }
    }

    private MemorySegment instantiateNativeComponent(
        MemorySegment enginePtr, MemorySegment componentPtr) throws WasmException {
      try {
        return nativeFunctions.instantiateComponent(enginePtr, componentPtr);
      } catch (Exception e) {
        throw new WasmException("Failed to instantiate component", e);
      }
    }

    private int getActiveInstancesCountNative(MemorySegment enginePtr) throws WasmException {
      try {
        return nativeFunctions.getActiveInstancesCount(enginePtr);
      } catch (Exception e) {
        throw new WasmException("Failed to get active instances count", e);
      }
    }

    private int cleanupInstancesNative(MemorySegment enginePtr) throws WasmException {
      try {
        return nativeFunctions.cleanupInstances(enginePtr);
      } catch (Exception e) {
        throw new WasmException("Failed to cleanup instances", e);
      }
    }

    private void destroyNativeComponentEngine(MemorySegment enginePtr) {
      try {
        nativeFunctions.destroyComponentEngine(enginePtr);
      } catch (Exception e) {
        LOGGER.warning("Error destroying component engine: " + e.getMessage());
      }
    }
  }

  /**
   * Panama FFI wrapper for component handles.
   *
   * <p>Represents a compiled WebAssembly component that can be instantiated multiple times.
   * Implements automatic resource cleanup through {@link ArenaResourceManager}.
   */
  public static final class PanamaComponentHandle implements AutoCloseable {

    private final ArenaResourceManager resourceManager;
    private final NativeComponentBindings nativeFunctions;
    private final ArenaResourceManager.ManagedNativeResource componentResource;
    private final NativeResourceHandle resourceHandle;

    /**
     * Creates a new component handle wrapper with arena-based resource management.
     *
     * @param resourceManager the arena resource manager for lifecycle management
     * @param componentPtr the native component pointer
     */
    PanamaComponentHandle(
        final ArenaResourceManager resourceManager, final MemorySegment componentPtr) {
      this.resourceManager =
          Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
      this.nativeFunctions = NativeComponentBindings.getInstance();

      // Create managed resource with cleanup
      this.componentResource =
          resourceManager.manageNativeResource(
              componentPtr, () -> destroyNativeComponent(componentPtr), "Wasmtime Component");

      this.resourceHandle =
          new NativeResourceHandle(
              "PanamaComponentHandle",
              () -> {
                componentResource.close();
                LOGGER.fine("Closed Panama component handle");
              });

      LOGGER.fine("Created Panama component handle with managed resource");
    }

    /**
     * Gets the size of the component in bytes.
     *
     * @return the component size in bytes
     * @throws WasmException if the operation fails
     */
    public long getSize() throws WasmException {
      ensureNotClosed();

      try {
        return getComponentSizeNative(componentResource.resource());
      } catch (final Exception e) {
        throw new WasmException("Failed to get component size", e);
      }
    }

    /**
     * Checks if the component exports the specified interface.
     *
     * @param interfaceName the interface name to check
     * @return true if the interface is exported, false otherwise
     * @throws WasmException if the operation fails
     */
    public boolean exportsInterface(final String interfaceName) throws WasmException {
      Objects.requireNonNull(interfaceName, "Interface name cannot be null");
      Validation.requireNonEmpty(interfaceName, "interfaceName");
      ensureNotClosed();

      try {
        return exportsInterfaceNative(componentResource.resource(), interfaceName);
      } catch (final Exception e) {
        throw new WasmException("Failed to check exported interface", e);
      }
    }

    /**
     * Checks if the component imports the specified interface.
     *
     * @param interfaceName the interface name to check
     * @return true if the interface is imported, false otherwise
     * @throws WasmException if the operation fails
     */
    public boolean importsInterface(final String interfaceName) throws WasmException {
      Objects.requireNonNull(interfaceName, "Interface name cannot be null");
      Validation.requireNonEmpty(interfaceName, "interfaceName");
      ensureNotClosed();

      try {
        return importsInterfaceNative(componentResource.resource(), interfaceName);
      } catch (final Exception e) {
        throw new WasmException("Failed to check imported interface", e);
      }
    }

    /**
     * Gets the native resource pointer for internal use.
     *
     * @return the native component pointer
     */
    MemorySegment getResource() {
      ensureNotClosed();
      return componentResource.resource();
    }

    /**
     * Checks if this component is still valid and usable.
     *
     * @return true if the component is valid, false if closed
     */
    public boolean isValid() {
      return !resourceHandle.isClosed() && componentResource.isValid();
    }

    void ensureNotClosed() {
      resourceHandle.ensureNotClosed();
    }

    @Override
    public void close() {
      resourceHandle.close();
    }

    // Native method implementations using Panama FFI

    private long getComponentSizeNative(MemorySegment componentPtr) throws WasmException {
      try {
        return nativeFunctions.getComponentSize(componentPtr);
      } catch (Exception e) {
        throw new WasmException("Failed to get component size", e);
      }
    }

    private boolean exportsInterfaceNative(MemorySegment componentPtr, String interfaceName)
        throws WasmException {
      try {
        return nativeFunctions.exportsInterface(componentPtr, interfaceName);
      } catch (Exception e) {
        throw new WasmException("Failed to check exported interface", e);
      }
    }

    private boolean importsInterfaceNative(MemorySegment componentPtr, String interfaceName)
        throws WasmException {
      try {
        return nativeFunctions.importsInterface(componentPtr, interfaceName);
      } catch (Exception e) {
        throw new WasmException("Failed to check imported interface", e);
      }
    }

    private void destroyNativeComponent(MemorySegment componentPtr) {
      try {
        nativeFunctions.destroyComponent(componentPtr);
      } catch (Exception e) {
        LOGGER.warning("Error destroying component: " + e.getMessage());
      }
    }
  }

  /**
   * Panama FFI wrapper for component instance handles.
   *
   * <p>Represents an instantiated WebAssembly component that can be used to call exported functions
   * and interact with component resources. Implements automatic resource cleanup through {@link
   * ArenaResourceManager}.
   */
  public static final class PanamaComponentInstanceHandle implements AutoCloseable {

    private final ArenaResourceManager resourceManager;
    private final NativeComponentBindings nativeFunctions;
    private final ArenaResourceManager.ManagedNativeResource instanceResource;
    private final NativeResourceHandle resourceHandle;

    /**
     * Creates a new component instance handle wrapper with arena-based resource management.
     *
     * @param resourceManager the arena resource manager for lifecycle management
     * @param instancePtr the native component instance pointer
     */
    PanamaComponentInstanceHandle(
        final ArenaResourceManager resourceManager, final MemorySegment instancePtr) {
      this.resourceManager =
          Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
      this.nativeFunctions = NativeComponentBindings.getInstance();

      // Create managed resource with cleanup
      this.instanceResource =
          resourceManager.manageNativeResource(
              instancePtr,
              () -> destroyNativeComponentInstance(instancePtr),
              "Wasmtime Component Instance");

      this.resourceHandle =
          new NativeResourceHandle(
              "PanamaComponentInstanceHandle",
              () -> {
                instanceResource.close();
                LOGGER.fine("Closed Panama component instance handle");
              });

      LOGGER.fine("Created Panama component instance handle with managed resource");
    }

    /**
     * Gets the native resource pointer for internal use.
     *
     * @return the native component instance pointer
     */
    public MemorySegment getResource() {
      ensureNotClosed();
      return instanceResource.resource();
    }

    /**
     * Checks if this component instance is still valid and usable.
     *
     * @return true if the instance is valid, false if closed
     */
    public boolean isValid() {
      return !resourceHandle.isClosed() && instanceResource.isValid();
    }

    private void ensureNotClosed() {
      resourceHandle.ensureNotClosed();
    }

    @Override
    public void close() {
      resourceHandle.close();
    }

    // Native method implementations using Panama FFI

    private void destroyNativeComponentInstance(MemorySegment instancePtr) {
      try {
        nativeFunctions.destroyComponentInstance(instancePtr);
      } catch (Exception e) {
        LOGGER.warning("Error destroying component instance: " + e.getMessage());
      }
    }
  }
}

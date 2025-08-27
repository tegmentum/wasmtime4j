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

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;

import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly module interface.
 *
 * <p>A WebAssembly module represents compiled WebAssembly bytecode that has been
 * validated and prepared for instantiation. This implementation uses Panama FFI with
 * optimized method handles and MemorySegment integration for direct access to the
 * underlying Wasmtime module structure.
 *
 * <p>Modules are immutable once compiled and can be instantiated multiple times
 * to create separate execution contexts. They contain metadata about imports,
 * exports, and internal structure accessed through zero-copy operations.
 */
public final class PanamaModule implements Module, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(PanamaModule.class.getName());

    // Core infrastructure components
    private final ArenaResourceManager resourceManager;
    private final NativeFunctionBindings nativeFunctions;
    private final PanamaEngine engine;
    private final ArenaResourceManager.ManagedNativeResource moduleResource;

    // Module state
    private volatile boolean closed = false;

    /**
     * Creates a new Panama module instance using Stream 1 infrastructure.
     *
     * @param modulePtr the native module pointer from compilation
     * @param resourceManager the arena resource manager for lifecycle management
     * @param engine the parent engine instance
     * @throws WasmException if the module cannot be created
     */
    public PanamaModule(final MemorySegment modulePtr,
                        final ArenaResourceManager resourceManager,
                        final PanamaEngine engine) throws WasmException {
        // Defensive parameter validation
        PanamaErrorHandler.requireValidPointer(modulePtr, "modulePtr");
        this.resourceManager = Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
        this.engine = Objects.requireNonNull(engine, "Engine cannot be null");
        this.nativeFunctions = NativeFunctionBindings.getInstance();

        if (!nativeFunctions.isInitialized()) {
            throw new WasmException("Native function bindings not initialized");
        }

        try {
            // Create managed resource with cleanup for module
            this.moduleResource = resourceManager.manageNativeResource(
                modulePtr,
                () -> destroyNativeModuleInternal(modulePtr),
                "Wasmtime Module"
            );

            LOGGER.fine("Created Panama module instance with managed resource");

        } catch (Exception e) {
            throw new WasmException("Failed to create module wrapper", e);
        }
    }

    @Override
    public Instance instantiate() throws WasmException {
        ensureNotClosed();

        try {
            // Create instance with no imports using optimized call
            return instantiate(Collections.emptyList());
        } catch (Exception e) {
            String detailedMessage = PanamaErrorHandler.createDetailedErrorMessage(
                "Module instantiation", 
                "no imports", 
                e.getMessage()
            );
            throw new WasmException(detailedMessage, e);
        }
    }

    @Override
    public Instance instantiate(final List<Object> imports) throws WasmException {
        ensureNotClosed();

        // Parameter validation with defensive programming
        Objects.requireNonNull(imports, "Imports list cannot be null");

        try {
            // For this implementation, we need a Store to create instances
            // The Store will be created by the caller and passed to a different instantiate method
            throw new UnsupportedOperationException(
                "Module instantiation requires a Store context - use PanamaStore.instantiateModule() instead");

        } catch (Exception e) {
            if (e instanceof UnsupportedOperationException) {
                throw e;
            }
            String detailedMessage = PanamaErrorHandler.createDetailedErrorMessage(
                "Module instantiation", 
                "imports.size=" + imports.size(), 
                e.getMessage()
            );
            throw new WasmException(detailedMessage, e);
        }
    }

    /**
     * Internal instantiation method used by Store implementations.
     *
     * @param storePtr the store pointer for the instance context
     * @param imports the import objects (currently unused)
     * @return the created instance
     * @throws WasmException if instantiation fails
     */
    public PanamaInstance createInstance(final MemorySegment storePtr, 
                                        final List<Object> imports) throws WasmException {
        ensureNotClosed();
        
        // Defensive parameter validation
        PanamaErrorHandler.requireValidPointer(storePtr, "storePtr");
        Objects.requireNonNull(imports, "Imports list cannot be null");

        try {
            // Create the native instance through optimized FFI
            MemorySegment instancePtr = createNativeInstance(storePtr, moduleResource.getNativePointer(), imports);
            
            // Return managed instance with proper resource tracking
            return new PanamaInstance(instancePtr, resourceManager, this);

        } catch (Exception e) {
            String detailedMessage = PanamaErrorHandler.createDetailedErrorMessage(
                "Native instance creation", 
                "store=" + storePtr + ", imports.size=" + imports.size(), 
                e.getMessage()
            );
            throw new WasmException(detailedMessage, e);
        }
    }

    @Override
    public List<String> getImports() throws WasmException {
        ensureNotClosed();

        try {
            // Module introspection through optimized FFI calls
            // For now, return empty list - full implementation would extract
            // import information from the module's metadata
            LOGGER.fine("Getting module imports - returning empty list for now");
            return Collections.emptyList();

        } catch (Exception e) {
            String detailedMessage = PanamaErrorHandler.createDetailedErrorMessage(
                "Module import introspection", 
                "module=" + moduleResource.getNativePointer(), 
                e.getMessage()
            );
            throw new WasmException(detailedMessage, e);
        }
    }

    @Override
    public List<String> getExports() throws WasmException {
        ensureNotClosed();

        try {
            // Module introspection through optimized FFI calls
            // For now, return empty list - full implementation would extract
            // export information from the module's metadata
            LOGGER.fine("Getting module exports - returning empty list for now");
            return Collections.emptyList();

        } catch (Exception e) {
            String detailedMessage = PanamaErrorHandler.createDetailedErrorMessage(
                "Module export introspection", 
                "module=" + moduleResource.getNativePointer(), 
                e.getMessage()
            );
            throw new WasmException(detailedMessage, e);
        }
    }

    @Override
    public byte[] serialize() throws WasmException {
        ensureNotClosed();

        try {
            // Module serialization through optimized FFI calls
            // For now, return empty array - full implementation would serialize
            // the compiled module back to bytecode format
            LOGGER.fine("Serializing module - returning empty array for now");
            return new byte[0];

        } catch (Exception e) {
            String detailedMessage = PanamaErrorHandler.createDetailedErrorMessage(
                "Module serialization", 
                "module=" + moduleResource.getNativePointer(), 
                e.getMessage()
            );
            throw new WasmException(detailedMessage, e);
        }
    }

    @Override
    public void close() throws WasmException {
        if (closed) {
            return;
        }

        synchronized (this) {
            if (closed) {
                return;
            }

            try {
                // Close the managed native resource - this triggers automatic cleanup
                moduleResource.close();

                LOGGER.fine("Closed Panama module instance");
            } catch (Exception e) {
                throw new WasmException("Failed to close module", e);
            } finally {
                closed = true;
            }
        }
    }

    /**
     * Gets the native module pointer for internal use.
     *
     * @return the native module handle
     * @throws IllegalStateException if the module is closed
     */
    public MemorySegment getModulePointer() {
        ensureNotClosed();
        return moduleResource.getNativePointer();
    }

    /**
     * Gets the parent engine instance.
     *
     * @return the engine instance
     * @throws IllegalStateException if the module is closed
     */
    public PanamaEngine getEngine() {
        ensureNotClosed();
        return engine;
    }

    /**
     * Gets the resource manager for this module.
     *
     * @return the resource manager
     * @throws IllegalStateException if the module is closed
     */
    public ArenaResourceManager getResourceManager() {
        ensureNotClosed();
        return resourceManager;
    }

    /**
     * Checks if the module is closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed || moduleResource.isClosed();
    }

    /**
     * Creates a new native instance through optimized FFI calls.
     *
     * @param storePtr the store pointer for the instance context
     * @param modulePtr the native module handle
     * @param imports the import objects (currently unused)
     * @return the native instance handle
     * @throws WasmException if the instance cannot be created
     */
    private MemorySegment createNativeInstance(final MemorySegment storePtr,
                                               final MemorySegment modulePtr,
                                               final List<Object> imports) throws WasmException {
        // Defensive parameter validation
        PanamaErrorHandler.requireValidPointer(storePtr, "storePtr");
        PanamaErrorHandler.requireValidPointer(modulePtr, "modulePtr");
        Objects.requireNonNull(imports, "imports");

        try {
            // Allocate memory for instance pointer output
            ArenaResourceManager.ManagedMemorySegment instanceOutPtr = 
                resourceManager.allocate(MemoryLayouts.C_POINTER);

            // Call native instance creation function with type-safe parameters
            int result = nativeFunctions.instanceCreate(
                storePtr,
                modulePtr,
                instanceOutPtr.getSegment()
            );

            // Check for instantiation errors using comprehensive error handling
            PanamaErrorHandler.safeCheckError(result, "Instance creation", 
                "WebAssembly instance creation failed");

            // Extract the created instance pointer
            MemorySegment instancePtr = (MemorySegment) MemoryLayouts.C_POINTER
                .varHandle().get(instanceOutPtr.getSegment(), 0);

            PanamaErrorHandler.requireValidPointer(instancePtr, "created instance pointer");

            LOGGER.fine("Successfully created WebAssembly instance from module");
            return instancePtr;

        } catch (Exception e) {
            String detailedMessage = PanamaErrorHandler.createDetailedErrorMessage(
                "Native instance creation", 
                "store=" + storePtr + ", module=" + modulePtr + ", imports.size=" + imports.size(), 
                e.getMessage()
            );
            throw new WasmException(detailedMessage, e);
        }
    }

    /**
     * Internal cleanup method for native module destruction.
     *
     * @param modulePtr the native module handle to destroy
     */
    private void destroyNativeModuleInternal(final MemorySegment modulePtr) {
        try {
            if (modulePtr != null && !modulePtr.equals(MemorySegment.NULL)) {
                nativeFunctions.moduleDestroy(modulePtr);
                LOGGER.fine("Destroyed native module with pointer: " + modulePtr);
            }
        } catch (Exception e) {
            // Log but don't throw - this is called during cleanup
            LOGGER.warning("Failed to destroy native module: " + e.getMessage());
        }
    }

    /**
     * Ensures that this module instance is not closed.
     *
     * @throws IllegalStateException if the module is closed
     */
    private void ensureNotClosed() {
        if (isClosed()) {
            throw new IllegalStateException("Module has been closed");
        }
    }
}
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

import ai.tegmentum.wasmtime4j.Function;
import ai.tegmentum.wasmtime4j.Global;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Memory;
import ai.tegmentum.wasmtime4j.Table;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.ffi.WasmtimeBindings;
import ai.tegmentum.wasmtime4j.panama.util.PanamaExceptionMapper;
import ai.tegmentum.wasmtime4j.panama.util.PanamaMemoryManager;
import ai.tegmentum.wasmtime4j.panama.util.PanamaResourceTracker;

import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly instance interface.
 * 
 * <p>A WebAssembly instance represents an instantiated module with its own
 * execution context, memory, and exported functions. This implementation uses
 * Panama FFI for direct access to the underlying Wasmtime instance structure.</p>
 * 
 * <p>Instances provide access to exported functions, globals, memory, and tables.
 * They maintain their own execution state and can be used to invoke WebAssembly
 * functions with proper marshalling of parameters and return values.</p>
 * 
 * @since 1.0.0
 */
public final class PanamaInstance implements Instance {
    private static final Logger logger = Logger.getLogger(PanamaInstance.class.getName());
    
    private final PanamaModule module;
    private final MemorySegment instanceHandle;
    private final WasmtimeBindings bindings;
    private final PanamaMemoryManager memoryManager;
    private final PanamaResourceTracker resourceTracker;
    private final PanamaExceptionMapper exceptionMapper;
    
    private volatile boolean closed = false;

    /**
     * Creates a new Panama instance.
     * 
     * @param module the parent module instance
     * @param instanceHandle the native instance handle
     * @param bindings the Wasmtime FFI bindings
     * @param memoryManager the memory manager for native resources
     * @param resourceTracker the resource tracker for cleanup
     * @param exceptionMapper the exception mapper for error handling
     * @throws WasmException if the instance cannot be created
     */
    PanamaInstance(final PanamaModule module,
                   final MemorySegment instanceHandle,
                   final WasmtimeBindings bindings,
                   final PanamaMemoryManager memoryManager,
                   final PanamaResourceTracker resourceTracker,
                   final PanamaExceptionMapper exceptionMapper) throws WasmException {
        this.module = module;
        this.instanceHandle = instanceHandle;
        this.bindings = bindings;
        this.memoryManager = memoryManager;
        this.resourceTracker = resourceTracker;
        this.exceptionMapper = exceptionMapper;
        
        try {
            // Register for cleanup tracking
            resourceTracker.trackResource(this, instanceHandle);
            
            logger.fine("Created Panama instance");
        } catch (Exception e) {
            throw exceptionMapper.mapException(e);
        }
    }

    @Override
    public Function getFunction(final String name) throws WasmException {
        ensureNotClosed();
        
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Function name cannot be null or empty");
        }

        try {
            // TODO: Implement function lookup through FFI
            logger.fine("Getting function '" + name + "' - placeholder implementation");
            
            // For now, return null to indicate function not found
            // This will be replaced with actual FFI calls to wasmtime_instance_export_get()
            return null;
        } catch (Exception e) {
            throw exceptionMapper.mapException(e);
        }
    }

    @Override
    public Memory getMemory(final String name) throws WasmException {
        ensureNotClosed();
        
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Memory name cannot be null or empty");
        }

        try {
            // TODO: Implement memory lookup through FFI
            logger.fine("Getting memory '" + name + "' - placeholder implementation");
            
            // For now, return null to indicate memory not found
            // This will be replaced with actual FFI calls to wasmtime_instance_export_get()
            return null;
        } catch (Exception e) {
            throw exceptionMapper.mapException(e);
        }
    }

    @Override
    public Global getGlobal(final String name) throws WasmException {
        ensureNotClosed();
        
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Global name cannot be null or empty");
        }

        try {
            // TODO: Implement global lookup through FFI
            logger.fine("Getting global '" + name + "' - placeholder implementation");
            
            // For now, return null to indicate global not found
            // This will be replaced with actual FFI calls to wasmtime_instance_export_get()
            return null;
        } catch (Exception e) {
            throw exceptionMapper.mapException(e);
        }
    }

    @Override
    public Table getTable(final String name) throws WasmException {
        ensureNotClosed();
        
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }

        try {
            // TODO: Implement table lookup through FFI
            logger.fine("Getting table '" + name + "' - placeholder implementation");
            
            // For now, return null to indicate table not found
            // This will be replaced with actual FFI calls to wasmtime_instance_export_get()
            return null;
        } catch (Exception e) {
            throw exceptionMapper.mapException(e);
        }
    }

    @Override
    public Object[] invokeFunction(final String name, final Object... args) throws RuntimeException, WasmException {
        ensureNotClosed();
        
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Function name cannot be null or empty");
        }

        try {
            // Get the function first
            final Function function = getFunction(name);
            if (function == null) {
                throw new WasmException("Function '" + name + "' not found in instance");
            }

            // Invoke the function
            return function.invoke(args);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw exceptionMapper.mapException(e);
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
                // Unregister from resource tracker
                resourceTracker.untrackResource(this);
                
                // Destroy the native instance
                destroyNativeInstance(instanceHandle);
                
                logger.fine("Closed Panama instance");
            } catch (Exception e) {
                throw exceptionMapper.mapException(e);
            } finally {
                closed = true;
            }
        }
    }

    /**
     * Gets the native instance handle for this instance.
     * 
     * @return the native instance handle
     */
    public MemorySegment getInstanceHandle() {
        ensureNotClosed();
        return instanceHandle;
    }

    /**
     * Gets the parent module instance.
     * 
     * @return the module instance
     */
    public PanamaModule getModule() {
        ensureNotClosed();
        return module;
    }

    /**
     * Destroys the native instance through FFI calls.
     * 
     * @param instanceHandle the native instance handle to destroy
     * @throws WasmException if destruction fails
     */
    private void destroyNativeInstance(final MemorySegment instanceHandle) throws WasmException {
        try {
            // TODO: Implement native instance destruction through Wasmtime FFI
            logger.fine("Destroying native instance - placeholder implementation");
            
            // This will be replaced with actual FFI calls to wasmtime_instance_delete()
        } catch (Exception e) {
            throw new WasmException("Failed to destroy native instance", e);
        }
    }

    /**
     * Ensures that this instance is not closed.
     * 
     * @throws IllegalStateException if the instance is closed
     */
    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("Instance has been closed");
        }
    }
}
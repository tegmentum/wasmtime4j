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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.ffi.WasmtimeBindings;
import ai.tegmentum.wasmtime4j.panama.util.PanamaExceptionMapper;
import ai.tegmentum.wasmtime4j.panama.util.PanamaMemoryManager;
import ai.tegmentum.wasmtime4j.panama.util.PanamaResourceTracker;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly engine interface.
 * 
 * <p>The engine is responsible for configuring the WebAssembly execution environment
 * and compiling WebAssembly modules. This implementation uses Panama FFI for
 * direct native calls to Wasmtime's engine APIs.</p>
 * 
 * <p>Engine instances are lightweight and can be shared across multiple module
 * compilations. The engine manages compilation settings, optimization levels,
 * and runtime configuration.</p>
 * 
 * @since 1.0.0
 */
public final class PanamaEngine implements Engine {
    private static final Logger logger = Logger.getLogger(PanamaEngine.class.getName());
    
    private final PanamaWasmRuntime runtime;
    private final WasmtimeBindings bindings;
    private final PanamaMemoryManager memoryManager;
    private final PanamaResourceTracker resourceTracker;
    private final PanamaExceptionMapper exceptionMapper;
    private final MemorySegment engineHandle;
    
    private volatile boolean closed = false;

    /**
     * Creates a new Panama engine instance.
     * 
     * @param runtime the parent runtime instance
     * @param bindings the Wasmtime FFI bindings
     * @param memoryManager the memory manager for native resources
     * @param resourceTracker the resource tracker for cleanup
     * @param exceptionMapper the exception mapper for error handling
     * @throws WasmException if the engine cannot be created
     */
    PanamaEngine(final PanamaWasmRuntime runtime,
                 final WasmtimeBindings bindings,
                 final PanamaMemoryManager memoryManager,
                 final PanamaResourceTracker resourceTracker,
                 final PanamaExceptionMapper exceptionMapper) throws WasmException {
        this.runtime = runtime;
        this.bindings = bindings;
        this.memoryManager = memoryManager;
        this.resourceTracker = resourceTracker;
        this.exceptionMapper = exceptionMapper;
        
        try {
            // Create the native engine handle
            this.engineHandle = createNativeEngine();
            
            // Register for cleanup tracking
            resourceTracker.trackResource(this, engineHandle);
            
            logger.fine("Created Panama engine instance");
        } catch (Exception e) {
            throw exceptionMapper.mapException(e);
        }
    }

    @Override
    public Module compileModule(final byte[] wasmBytes) throws CompilationException, WasmException {
        ensureNotClosed();
        
        if (wasmBytes == null) {
            throw new IllegalArgumentException("WebAssembly bytes cannot be null");
        }
        if (wasmBytes.length == 0) {
            throw new IllegalArgumentException("WebAssembly bytes cannot be empty");
        }

        try {
            // Allocate native memory for the WASM bytes
            final MemorySegment wasmData = memoryManager.allocateBytes(wasmBytes);
            
            try {
                // Compile the module through FFI
                final MemorySegment moduleHandle = compileModuleNative(engineHandle, wasmData, wasmBytes.length);
                
                return new PanamaModule(this, moduleHandle, bindings, memoryManager, 
                                        resourceTracker, exceptionMapper);
            } finally {
                // Clean up the temporary WASM data
                memoryManager.freeMemory(wasmData);
            }
        } catch (Exception e) {
            if (e instanceof CompilationException) {
                throw (CompilationException) e;
            }
            throw exceptionMapper.mapException(e);
        }
    }

    @Override
    public Module compileModule(final ByteBuffer wasmBuffer) throws CompilationException, WasmException {
        ensureNotClosed();
        
        if (wasmBuffer == null) {
            throw new IllegalArgumentException("WebAssembly buffer cannot be null");
        }
        if (!wasmBuffer.hasRemaining()) {
            throw new IllegalArgumentException("WebAssembly buffer cannot be empty");
        }

        try {
            // Convert ByteBuffer to byte array for now
            // TODO: Optimize to use direct MemorySegment access for direct buffers
            final byte[] wasmBytes = new byte[wasmBuffer.remaining()];
            wasmBuffer.get(wasmBytes);
            return compileModule(wasmBytes);
        } catch (Exception e) {
            if (e instanceof CompilationException) {
                throw (CompilationException) e;
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
                
                // Destroy the native engine
                destroyNativeEngine(engineHandle);
                
                logger.fine("Closed Panama engine instance");
            } catch (Exception e) {
                throw exceptionMapper.mapException(e);
            } finally {
                closed = true;
            }
        }
    }

    /**
     * Gets the native engine handle for this engine.
     * 
     * @return the native engine handle
     */
    public MemorySegment getEngineHandle() {
        ensureNotClosed();
        return engineHandle;
    }

    /**
     * Gets the parent runtime instance.
     * 
     * @return the runtime instance
     */
    public PanamaWasmRuntime getRuntime() {
        ensureNotClosed();
        return runtime;
    }

    /**
     * Creates a new native engine through FFI calls.
     * 
     * @return the native engine handle
     * @throws WasmException if the engine cannot be created
     */
    private MemorySegment createNativeEngine() throws WasmException {
        try {
            // TODO: Implement native engine creation through Wasmtime FFI
            logger.fine("Creating native engine - placeholder implementation");
            
            // For now, return a null memory segment as placeholder
            // This will be replaced with actual FFI calls to wasmtime_engine_new()
            return MemorySegment.NULL;
        } catch (Exception e) {
            throw new WasmException("Failed to create native engine", e);
        }
    }

    /**
     * Compiles a WebAssembly module through native FFI calls.
     * 
     * @param engineHandle the native engine handle
     * @param wasmData the WebAssembly bytecode
     * @param length the length of the bytecode
     * @return the compiled module handle
     * @throws CompilationException if compilation fails
     * @throws WasmException if a native error occurs
     */
    private MemorySegment compileModuleNative(final MemorySegment engineHandle,
                                              final MemorySegment wasmData,
                                              final int length) throws CompilationException, WasmException {
        try {
            // TODO: Implement module compilation through Wasmtime FFI
            logger.fine("Compiling module through native FFI - placeholder implementation");
            
            // For now, return a null memory segment as placeholder
            // This will be replaced with actual FFI calls to wasmtime_module_new()
            return MemorySegment.NULL;
        } catch (Exception e) {
            throw new CompilationException("Failed to compile WebAssembly module", e);
        }
    }

    /**
     * Destroys the native engine through FFI calls.
     * 
     * @param engineHandle the native engine handle to destroy
     * @throws WasmException if destruction fails
     */
    private void destroyNativeEngine(final MemorySegment engineHandle) throws WasmException {
        try {
            // TODO: Implement native engine destruction through Wasmtime FFI
            logger.fine("Destroying native engine - placeholder implementation");
            
            // This will be replaced with actual FFI calls to wasmtime_engine_delete()
        } catch (Exception e) {
            throw new WasmException("Failed to destroy native engine", e);
        }
    }

    /**
     * Ensures that this engine instance is not closed.
     * 
     * @throws IllegalStateException if the engine is closed
     */
    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("Engine has been closed");
        }
    }
}
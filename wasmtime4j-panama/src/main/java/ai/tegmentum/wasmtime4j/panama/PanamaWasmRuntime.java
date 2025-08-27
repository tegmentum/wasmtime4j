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
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.ffi.WasmtimeBindings;
import ai.tegmentum.wasmtime4j.panama.util.PanamaExceptionMapper;
import ai.tegmentum.wasmtime4j.panama.util.PanamaMemoryManager;
import ai.tegmentum.wasmtime4j.panama.util.PanamaResourceTracker;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly runtime interface.
 * 
 * <p>This implementation uses Java 23+ Panama Foreign Function API to provide
 * high-performance, type-safe access to the Wasmtime WebAssembly runtime.
 * It leverages MemorySegment for efficient native memory management and
 * Arena for automatic resource cleanup.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Direct native function calls without JNI overhead</li>
 *   <li>Zero-copy memory access using MemorySegment</li>
 *   <li>Automatic resource management with Arena pattern</li>
 *   <li>Type-safe native function signatures</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public final class PanamaWasmRuntime implements WasmRuntime {
    private static final Logger logger = Logger.getLogger(PanamaWasmRuntime.class.getName());
    
    private final Arena arena;
    private final SymbolLookup nativeLibrary;
    private final WasmtimeBindings bindings;
    private final PanamaMemoryManager memoryManager;
    private final PanamaResourceTracker resourceTracker;
    private final PanamaExceptionMapper exceptionMapper;
    
    private volatile boolean closed = false;

    /**
     * Creates a new Panama WebAssembly runtime instance.
     * 
     * @throws WasmException if the native library cannot be loaded or initialized
     */
    public PanamaWasmRuntime() throws WasmException {
        logger.info("Initializing Panama WebAssembly runtime");
        
        try {
            this.arena = Arena.ofConfined();
            this.nativeLibrary = loadNativeLibrary();
            this.bindings = new WasmtimeBindings(nativeLibrary);
            this.memoryManager = new PanamaMemoryManager(arena);
            this.resourceTracker = new PanamaResourceTracker();
            this.exceptionMapper = new PanamaExceptionMapper();
            
            // Initialize the Wasmtime library
            initializeWasmtime();
            
            logger.info("Panama WebAssembly runtime initialized successfully");
        } catch (Exception e) {
            // Ensure cleanup if initialization fails
            if (arena != null) {
                arena.close();
            }
            throw new WasmException("Failed to initialize Panama WebAssembly runtime", e);
        }
    }

    @Override
    public Engine createEngine() throws WasmException {
        ensureNotClosed();
        
        try {
            return new PanamaEngine(this, bindings, memoryManager, resourceTracker, exceptionMapper);
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
            // Create a temporary engine for module compilation
            try (Engine engine = createEngine()) {
                return engine.compileModule(wasmBytes);
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
            // TODO: Optimize to use direct MemorySegment access
            byte[] wasmBytes = new byte[wasmBuffer.remaining()];
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

        logger.info("Closing Panama WebAssembly runtime");
        
        synchronized (this) {
            if (closed) {
                return;
            }
            
            try {
                // Clean up resources in reverse order of creation
                if (resourceTracker != null) {
                    resourceTracker.cleanup();
                }
                
                // Close the arena, which will free all associated memory
                if (arena != null) {
                    arena.close();
                }
                
                logger.info("Panama WebAssembly runtime closed successfully");
            } catch (Exception e) {
                throw new WasmException("Failed to close Panama WebAssembly runtime", e);
            } finally {
                closed = true;
            }
        }
    }

    /**
     * Gets the native library symbol lookup for this runtime.
     * 
     * @return the symbol lookup instance
     */
    public SymbolLookup getNativeLibrary() {
        ensureNotClosed();
        return nativeLibrary;
    }

    /**
     * Gets the Wasmtime bindings for this runtime.
     * 
     * @return the bindings instance
     */
    public WasmtimeBindings getBindings() {
        ensureNotClosed();
        return bindings;
    }

    /**
     * Gets the memory manager for this runtime.
     * 
     * @return the memory manager instance
     */
    public PanamaMemoryManager getMemoryManager() {
        ensureNotClosed();
        return memoryManager;
    }

    /**
     * Gets the resource tracker for this runtime.
     * 
     * @return the resource tracker instance
     */
    public PanamaResourceTracker getResourceTracker() {
        ensureNotClosed();
        return resourceTracker;
    }

    /**
     * Gets the exception mapper for this runtime.
     * 
     * @return the exception mapper instance
     */
    public PanamaExceptionMapper getExceptionMapper() {
        ensureNotClosed();
        return exceptionMapper;
    }

    /**
     * Loads the native Wasmtime library using Panama FFI.
     * 
     * @return the symbol lookup for the loaded library
     * @throws WasmException if the library cannot be loaded
     */
    private SymbolLookup loadNativeLibrary() throws WasmException {
        try {
            // TODO: Implement proper native library loading with platform detection
            // For now, create a placeholder that will be replaced with actual implementation
            logger.warning("Native library loading not yet implemented - using placeholder");
            return SymbolLookup.loaderLookup();
        } catch (Exception e) {
            throw new WasmException("Failed to load native Wasmtime library", e);
        }
    }

    /**
     * Initializes the Wasmtime library through Panama FFI calls.
     * 
     * @throws WasmException if initialization fails
     */
    private void initializeWasmtime() throws WasmException {
        try {
            // TODO: Implement Wasmtime initialization through FFI calls
            logger.info("Wasmtime initialization placeholder - will be implemented with native bindings");
        } catch (Exception e) {
            throw new WasmException("Failed to initialize Wasmtime library", e);
        }
    }

    /**
     * Ensures that this runtime instance is not closed.
     * 
     * @throws IllegalStateException if the runtime is closed
     */
    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("Panama WebAssembly runtime has been closed");
        }
    }
}
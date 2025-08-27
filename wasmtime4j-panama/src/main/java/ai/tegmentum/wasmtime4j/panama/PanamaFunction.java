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
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.ffi.WasmtimeBindings;
import ai.tegmentum.wasmtime4j.panama.util.PanamaExceptionMapper;
import ai.tegmentum.wasmtime4j.panama.util.PanamaMemoryManager;
import ai.tegmentum.wasmtime4j.panama.util.PanamaResourceTracker;

import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly function interface.
 * 
 * @since 1.0.0
 */
public final class PanamaFunction implements Function {
    private static final Logger logger = Logger.getLogger(PanamaFunction.class.getName());
    
    private final MemorySegment functionHandle;
    private final WasmtimeBindings bindings;
    private final PanamaMemoryManager memoryManager;
    private final PanamaResourceTracker resourceTracker;
    private final PanamaExceptionMapper exceptionMapper;
    
    private volatile boolean closed = false;

    PanamaFunction(final MemorySegment functionHandle,
                   final WasmtimeBindings bindings,
                   final PanamaMemoryManager memoryManager,
                   final PanamaResourceTracker resourceTracker,
                   final PanamaExceptionMapper exceptionMapper) throws WasmException {
        this.functionHandle = functionHandle;
        this.bindings = bindings;
        this.memoryManager = memoryManager;
        this.resourceTracker = resourceTracker;
        this.exceptionMapper = exceptionMapper;
        
        try {
            resourceTracker.trackResource(this, functionHandle);
            logger.fine("Created Panama function instance");
        } catch (Exception e) {
            throw exceptionMapper.mapException(e);
        }
    }

    @Override
    public Object[] invoke(final Object... args) throws RuntimeException, WasmException {
        ensureNotClosed();
        
        try {
            // TODO: Implement function invocation through FFI
            logger.fine("Invoking function with " + (args != null ? args.length : 0) + " args - placeholder implementation");
            return new Object[0];
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
                resourceTracker.untrackResource(this);
                logger.fine("Closed Panama function instance");
            } catch (Exception e) {
                throw exceptionMapper.mapException(e);
            } finally {
                closed = true;
            }
        }
    }

    public MemorySegment getFunctionHandle() {
        ensureNotClosed();
        return functionHandle;
    }

    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("Function has been closed");
        }
    }
}
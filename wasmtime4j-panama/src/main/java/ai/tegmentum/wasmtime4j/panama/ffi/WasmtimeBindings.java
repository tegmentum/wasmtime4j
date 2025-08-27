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

package ai.tegmentum.wasmtime4j.panama.ffi;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Panama FFI bindings for the Wasmtime WebAssembly runtime library.
 * 
 * <p>This class provides type-safe, high-performance bindings to the native
 * Wasmtime C API using Java 23+ Panama Foreign Function API. It manages
 * function descriptors, method handles, and native library symbol lookup.</p>
 * 
 * <p>The bindings are created lazily and cached for optimal performance,
 * providing direct native function calls without JNI overhead.</p>
 * 
 * @since 1.0.0
 */
public final class WasmtimeBindings {
    private static final Logger logger = Logger.getLogger(WasmtimeBindings.class.getName());
    
    private final SymbolLookup symbolLookup;
    private final Linker linker;
    private final ConcurrentMap<String, MethodHandle> methodHandleCache;
    
    // Common layouts for Wasmtime structures
    public static final MemoryLayout WASMTIME_ENGINE_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("inner")
    ).withName("wasmtime_engine_t");
    
    public static final MemoryLayout WASMTIME_MODULE_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("inner")
    ).withName("wasmtime_module_t");
    
    public static final MemoryLayout WASMTIME_INSTANCE_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("inner")
    ).withName("wasmtime_instance_t");
    
    public static final MemoryLayout WASMTIME_MEMORY_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("inner")
    ).withName("wasmtime_memory_t");

    /**
     * Creates new Wasmtime FFI bindings with the given symbol lookup.
     * 
     * @param symbolLookup the symbol lookup for the loaded Wasmtime library
     * @throws IllegalArgumentException if symbolLookup is null
     */
    public WasmtimeBindings(final SymbolLookup symbolLookup) {
        if (symbolLookup == null) {
            throw new IllegalArgumentException("Symbol lookup cannot be null");
        }
        
        this.symbolLookup = symbolLookup;
        this.linker = Linker.nativeLinker();
        this.methodHandleCache = new ConcurrentHashMap<>();
        
        logger.fine("Created Wasmtime FFI bindings");
    }

    /**
     * Gets a method handle for the specified native function.
     * 
     * <p>Method handles are cached for performance. If the function is not found
     * in the native library, null is returned.</p>
     * 
     * @param functionName the name of the native function
     * @param descriptor the function descriptor defining the signature
     * @return the method handle, or null if not found
     * @throws IllegalArgumentException if parameters are null
     */
    public MethodHandle getMethodHandle(final String functionName, final FunctionDescriptor descriptor) {
        if (functionName == null || functionName.isEmpty()) {
            throw new IllegalArgumentException("Function name cannot be null or empty");
        }
        if (descriptor == null) {
            throw new IllegalArgumentException("Function descriptor cannot be null");
        }

        return methodHandleCache.computeIfAbsent(functionName, name -> {
            final Optional<MemorySegment> symbol = symbolLookup.find(name);
            if (symbol.isEmpty()) {
                logger.warning("Native function not found: " + name);
                return null;
            }

            try {
                return linker.downcallHandle(symbol.get(), descriptor);
            } catch (Exception e) {
                logger.warning("Failed to create method handle for " + name + ": " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Gets the method handle for wasmtime_engine_new().
     * 
     * @return the method handle, or null if not available
     */
    public MethodHandle wasmtimeEngineNew() {
        return getMethodHandle("wasmtime_engine_new", 
            FunctionDescriptor.of(ValueLayout.ADDRESS));
    }

    /**
     * Gets the method handle for wasmtime_engine_delete().
     * 
     * @return the method handle, or null if not available
     */
    public MethodHandle wasmtimeEngineDelete() {
        return getMethodHandle("wasmtime_engine_delete", 
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    }

    /**
     * Gets the method handle for wasmtime_module_new().
     * 
     * @return the method handle, or null if not available
     */
    public MethodHandle wasmtimeModuleNew() {
        return getMethodHandle("wasmtime_module_new", 
            FunctionDescriptor.of(ValueLayout.ADDRESS, // return module
                                  ValueLayout.ADDRESS, // engine
                                  ValueLayout.ADDRESS, // wasm data
                                  ValueLayout.JAVA_LONG)); // data length
    }

    /**
     * Gets the method handle for wasmtime_module_delete().
     * 
     * @return the method handle, or null if not available
     */
    public MethodHandle wasmtimeModuleDelete() {
        return getMethodHandle("wasmtime_module_delete", 
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    }

    /**
     * Gets the method handle for wasmtime_instance_new().
     * 
     * @return the method handle, or null if not available
     */
    public MethodHandle wasmtimeInstanceNew() {
        return getMethodHandle("wasmtime_instance_new", 
            FunctionDescriptor.of(ValueLayout.ADDRESS, // return instance
                                  ValueLayout.ADDRESS, // module
                                  ValueLayout.ADDRESS, // imports array
                                  ValueLayout.JAVA_LONG)); // imports length
    }

    /**
     * Gets the method handle for wasmtime_instance_delete().
     * 
     * @return the method handle, or null if not available
     */
    public MethodHandle wasmtimeInstanceDelete() {
        return getMethodHandle("wasmtime_instance_delete", 
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    }

    /**
     * Gets the method handle for wasmtime_memory_new().
     * 
     * @return the method handle, or null if not available
     */
    public MethodHandle wasmtimeMemoryNew() {
        return getMethodHandle("wasmtime_memory_new", 
            FunctionDescriptor.of(ValueLayout.ADDRESS, // return memory
                                  ValueLayout.JAVA_LONG, // initial pages
                                  ValueLayout.JAVA_LONG)); // maximum pages
    }

    /**
     * Gets the method handle for wasmtime_memory_size().
     * 
     * @return the method handle, or null if not available
     */
    public MethodHandle wasmtimeMemorySize() {
        return getMethodHandle("wasmtime_memory_size", 
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, // return size
                                  ValueLayout.ADDRESS)); // memory
    }

    /**
     * Gets the method handle for wasmtime_memory_grow().
     * 
     * @return the method handle, or null if not available
     */
    public MethodHandle wasmtimeMemoryGrow() {
        return getMethodHandle("wasmtime_memory_grow", 
            FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, // return success
                                  ValueLayout.ADDRESS, // memory
                                  ValueLayout.JAVA_LONG)); // pages
    }

    /**
     * Gets the method handle for wasmtime_memory_data().
     * 
     * @return the method handle, or null if not available
     */
    public MethodHandle wasmtimeMemoryData() {
        return getMethodHandle("wasmtime_memory_data", 
            FunctionDescriptor.of(ValueLayout.ADDRESS, // return data pointer
                                  ValueLayout.ADDRESS)); // memory
    }

    /**
     * Gets the symbol lookup instance for direct symbol access.
     * 
     * @return the symbol lookup instance
     */
    public SymbolLookup getSymbolLookup() {
        return symbolLookup;
    }

    /**
     * Gets the linker instance for creating additional bindings.
     * 
     * @return the linker instance
     */
    public Linker getLinker() {
        return linker;
    }

    /**
     * Clears the method handle cache.
     * 
     * <p>This method is primarily intended for testing or when the underlying
     * native library has been reloaded.</p>
     */
    public void clearCache() {
        methodHandleCache.clear();
        logger.fine("Cleared method handle cache");
    }

    /**
     * Gets the number of cached method handles.
     * 
     * @return the cache size
     */
    public int getCacheSize() {
        return methodHandleCache.size();
    }
}
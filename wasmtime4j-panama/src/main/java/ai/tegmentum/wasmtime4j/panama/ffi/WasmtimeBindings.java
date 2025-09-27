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
 * <p>This class provides type-safe, high-performance bindings to the native Wasmtime C API using
 * Java 23+ Panama Foreign Function API. It manages function descriptors, method handles, and native
 * library symbol lookup.
 *
 * <p>The bindings are created lazily and cached for optimal performance, providing direct native
 * function calls without JNI overhead.
 *
 * @since 1.0.0
 */
public final class WasmtimeBindings {
  private static final Logger logger = Logger.getLogger(WasmtimeBindings.class.getName());

  private final SymbolLookup symbolLookup;
  private final Linker linker;
  private final ConcurrentMap<String, MethodHandle> methodHandleCache;

  // Common layouts for Wasmtime structures
  public static final MemoryLayout WASMTIME_ENGINE_LAYOUT =
      MemoryLayout.structLayout(ValueLayout.ADDRESS.withName("inner"))
          .withName("wasmtime_engine_t");

  public static final MemoryLayout WASMTIME_MODULE_LAYOUT =
      MemoryLayout.structLayout(ValueLayout.ADDRESS.withName("inner"))
          .withName("wasmtime_module_t");

  public static final MemoryLayout WASMTIME_INSTANCE_LAYOUT =
      MemoryLayout.structLayout(ValueLayout.ADDRESS.withName("inner"))
          .withName("wasmtime_instance_t");

  public static final MemoryLayout WASMTIME_MEMORY_LAYOUT =
      MemoryLayout.structLayout(ValueLayout.ADDRESS.withName("inner"))
          .withName("wasmtime_memory_t");

  public static final MemoryLayout WASMTIME_TABLE_LAYOUT =
      MemoryLayout.structLayout(ValueLayout.ADDRESS.withName("inner")).withName("wasmtime_table_t");

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
   * <p>Method handles are cached for performance. If the function is not found in the native
   * library, null is returned.
   *
   * @param functionName the name of the native function
   * @param descriptor the function descriptor defining the signature
   * @return the method handle, or null if not found
   * @throws IllegalArgumentException if parameters are null
   */
  public MethodHandle getMethodHandle(
      final String functionName, final FunctionDescriptor descriptor) {
    if (functionName == null || functionName.isEmpty()) {
      throw new IllegalArgumentException("Function name cannot be null or empty");
    }
    if (descriptor == null) {
      throw new IllegalArgumentException("Function descriptor cannot be null");
    }

    return methodHandleCache.computeIfAbsent(
        functionName,
        name -> {
          // Try multiple symbol name variations for platform compatibility
          Optional<MemorySegment> symbol = findSymbolWithVariations(name);

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
   * Finds a symbol by trying multiple name variations for platform compatibility.
   *
   * <p>This method attempts to find native function symbols using different naming conventions:
   *
   * <ul>
   *   <li>Original name (e.g., "wasmtime_engine_new")
   *   <li>Underscore prefix for macOS/Darwin (e.g., "_wasmtime_engine_new")
   *   <li>Windows-style decorated names (future enhancement)
   * </ul>
   *
   * @param functionName the base function name
   * @return the symbol if found, empty otherwise
   */
  private Optional<MemorySegment> findSymbolWithVariations(final String functionName) {
    // Try original name first
    Optional<MemorySegment> symbol = symbolLookup.find(functionName);
    if (symbol.isPresent()) {
      logger.fine("Found symbol: " + functionName);
      return symbol;
    }

    // On macOS/Darwin, C symbols are typically prefixed with underscore
    final String osName = System.getProperty("os.name", "").toLowerCase();
    if (osName.contains("mac") || osName.contains("darwin")) {
      final String underscoreName = "_" + functionName;
      symbol = symbolLookup.find(underscoreName);
      if (symbol.isPresent()) {
        logger.fine("Found symbol with underscore prefix: " + underscoreName);
        return symbol;
      }
    }

    // Could add Windows-style decorated names here in the future
    // For now, return empty if no variations work
    return Optional.empty();
  }

  /**
   * Gets the method handle for wasmtime_engine_new().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeEngineNew() {
    return getMethodHandle("wasmtime_engine_new", FunctionDescriptor.of(ValueLayout.ADDRESS));
  }

  /**
   * Gets the method handle for wasmtime_engine_delete().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeEngineDelete() {
    return getMethodHandle(
        "wasmtime_engine_delete", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
  }

  /**
   * Gets the method handle for wasmtime_module_new().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeModuleNew() {
    return getMethodHandle(
        "wasmtime_module_new",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return module
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
    return getMethodHandle(
        "wasmtime_module_delete", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
  }

  /**
   * Gets the method handle for wasmtime_instance_new().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeInstanceNew() {
    return getMethodHandle(
        "wasmtime_instance_new",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return instance
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
    return getMethodHandle(
        "wasmtime_instance_delete", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
  }

  /**
   * Gets the method handle for wasmtime_memory_new().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeMemoryNew() {
    return getMethodHandle(
        "wasmtime_memory_new",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return memory
            ValueLayout.JAVA_LONG, // initial pages
            ValueLayout.JAVA_LONG)); // maximum pages
  }

  /**
   * Gets the method handle for wasmtime_memory_size().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeMemorySize() {
    return getMethodHandle(
        "wasmtime_memory_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return size
            ValueLayout.ADDRESS)); // memory
  }

  /**
   * Gets the method handle for wasmtime_memory_grow().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeMemoryGrow() {
    return getMethodHandle(
        "wasmtime_memory_grow",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // memory
            ValueLayout.JAVA_LONG)); // pages
  }

  /**
   * Gets the method handle for wasmtime_memory_data().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeMemoryData() {
    return getMethodHandle(
        "wasmtime_memory_data",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return data pointer
            ValueLayout.ADDRESS)); // memory
  }

  /**
   * Gets the method handle for wasmtime_table_new().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeTableNew() {
    return getMethodHandle(
        "wasmtime_table_new",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return table
            ValueLayout.JAVA_INT, // element type
            ValueLayout.JAVA_LONG, // initial size
            ValueLayout.JAVA_LONG)); // maximum size
  }

  /**
   * Gets the method handle for wasmtime_table_delete().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeTableDelete() {
    return getMethodHandle("wasmtime_table_delete", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
  }

  /**
   * Gets the method handle for wasmtime_table_size().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeTableSize() {
    return getMethodHandle(
        "wasmtime_table_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return size
            ValueLayout.ADDRESS)); // table
  }

  /**
   * Gets the method handle for wasmtime_table_get().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeTableGet() {
    return getMethodHandle(
        "wasmtime_table_get",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return element
            ValueLayout.ADDRESS, // table
            ValueLayout.JAVA_LONG)); // index
  }

  /**
   * Gets the method handle for wasmtime_table_set().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeTableSet() {
    return getMethodHandle(
        "wasmtime_table_set",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // table
            ValueLayout.JAVA_LONG, // index
            ValueLayout.ADDRESS)); // value
  }

  /**
   * Gets the method handle for wasmtime_table_grow().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtimeTableGrow() {
    return getMethodHandle(
        "wasmtime_table_grow",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // table
            ValueLayout.JAVA_LONG, // delta
            ValueLayout.ADDRESS)); // initial value
  }

  // WASI Preview 2 method handles

  /**
   * Gets the method handle for wasi_preview2_context_new().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasiPreview2ContextNew() {
    return getMethodHandle(
        "wasi_preview2_context_new",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return context handle
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.JAVA_INT, // enable networking
            ValueLayout.JAVA_INT, // enable filesystem
            ValueLayout.JAVA_INT)); // enable process
  }

  /**
   * Gets the method handle for wasi_preview2_context_destroy().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasiPreview2ContextDestroy() {
    return getMethodHandle(
        "wasi_preview2_context_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // context handle
  }

  /**
   * Gets the method handle for wasi_preview2_compile_component().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasiPreview2CompileComponent() {
    return getMethodHandle(
        "wasi_preview2_compile_component",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // context handle
            ValueLayout.ADDRESS, // component bytes
            ValueLayout.JAVA_LONG, // component size
            ValueLayout.ADDRESS)); // component id out
  }

  /**
   * Gets the method handle for wasi_preview2_instantiate_component().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasiPreview2InstantiateComponent() {
    return getMethodHandle(
        "wasi_preview2_instantiate_component",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // context handle
            ValueLayout.JAVA_LONG, // component id
            ValueLayout.ADDRESS)); // instance id out
  }

  /**
   * Gets the method handle for wasi_preview2_create_input_stream().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasiPreview2CreateInputStream() {
    return getMethodHandle(
        "wasi_preview2_create_input_stream",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // context handle
            ValueLayout.JAVA_LONG, // instance id
            ValueLayout.ADDRESS)); // stream id out
  }

  /**
   * Gets the method handle for wasi_preview2_create_output_stream().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasiPreview2CreateOutputStream() {
    return getMethodHandle(
        "wasi_preview2_create_output_stream",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // context handle
            ValueLayout.JAVA_LONG, // instance id
            ValueLayout.ADDRESS)); // stream id out
  }

  /**
   * Gets the method handle for wasi_preview2_stream_read().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasiPreview2StreamRead() {
    return getMethodHandle(
        "wasi_preview2_stream_read",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // context handle
            ValueLayout.JAVA_LONG, // instance id
            ValueLayout.JAVA_LONG, // stream id
            ValueLayout.ADDRESS, // buffer
            ValueLayout.JAVA_LONG, // buffer size
            ValueLayout.ADDRESS)); // bytes read out
  }

  /**
   * Gets the method handle for wasi_preview2_stream_write().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasiPreview2StreamWrite() {
    return getMethodHandle(
        "wasi_preview2_stream_write",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // context handle
            ValueLayout.JAVA_LONG, // instance id
            ValueLayout.JAVA_LONG, // stream id
            ValueLayout.ADDRESS, // buffer
            ValueLayout.JAVA_LONG, // buffer size
            ValueLayout.ADDRESS)); // bytes written out
  }

  /**
   * Gets the method handle for wasi_preview2_close_stream().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasiPreview2CloseStream() {
    return getMethodHandle(
        "wasi_preview2_close_stream",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // context handle
            ValueLayout.JAVA_LONG, // instance id
            ValueLayout.JAVA_LONG)); // stream id
  }

  // Component Model method handles

  /**
   * Gets the method handle for wasmtime4j_component_engine_new().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtime4jComponentEngineNew() {
    return getMethodHandle(
        "wasmtime4j_component_engine_new",
        FunctionDescriptor.of(ValueLayout.ADDRESS)); // return engine handle
  }

  /**
   * Gets the method handle for wasmtime4j_component_engine_destroy().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtime4jComponentEngineDestroy() {
    return getMethodHandle(
        "wasmtime4j_component_engine_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // engine handle
  }

  /**
   * Gets the method handle for wasmtime4j_component_compile().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtime4jComponentCompile() {
    return getMethodHandle(
        "wasmtime4j_component_compile",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.ADDRESS, // component bytes
            ValueLayout.JAVA_LONG, // component size
            ValueLayout.ADDRESS)); // component out
  }

  /**
   * Gets the method handle for wasmtime4j_component_compile_wat().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtime4jComponentCompileWat() {
    return getMethodHandle(
        "wasmtime4j_component_compile_wat",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.ADDRESS, // wat text
            ValueLayout.ADDRESS)); // component out
  }

  /**
   * Gets the method handle for wasmtime4j_component_instantiate().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtime4jComponentInstantiate() {
    return getMethodHandle(
        "wasmtime4j_component_instantiate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.ADDRESS, // component handle
            ValueLayout.ADDRESS)); // instance out
  }

  /**
   * Gets the method handle for wasmtime4j_component_export_count().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtime4jComponentExportCount() {
    return getMethodHandle(
        "wasmtime4j_component_export_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return count
            ValueLayout.ADDRESS)); // component handle
  }

  /**
   * Gets the method handle for wasmtime4j_component_has_export().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtime4jComponentHasExport() {
    return getMethodHandle(
        "wasmtime4j_component_has_export",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return boolean
            ValueLayout.ADDRESS, // component handle
            ValueLayout.ADDRESS)); // export name
  }

  /**
   * Gets the method handle for wasmtime4j_component_validate().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtime4jComponentValidate() {
    return getMethodHandle(
        "wasmtime4j_component_validate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return boolean
            ValueLayout.ADDRESS, // component handle
            ValueLayout.ADDRESS)); // wit interface
  }

  /**
   * Gets the method handle for wasmtime4j_wit_parser_new().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtime4jWitParserNew() {
    return getMethodHandle(
        "wasmtime4j_wit_parser_new",
        FunctionDescriptor.of(ValueLayout.ADDRESS)); // return parser handle
  }

  /**
   * Gets the method handle for wasmtime4j_wit_parser_validate_syntax().
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle wasmtime4jWitParserValidateSyntax() {
    return getMethodHandle(
        "wasmtime4j_wit_parser_validate_syntax",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return boolean
            ValueLayout.ADDRESS, // parser handle
            ValueLayout.ADDRESS)); // wit text
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
   * <p>This method is primarily intended for testing or when the underlying native library has been
   * reloaded.
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

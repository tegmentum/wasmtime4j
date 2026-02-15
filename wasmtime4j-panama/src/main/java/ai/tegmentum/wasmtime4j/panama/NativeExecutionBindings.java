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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Native function bindings for execution-related operations.
 *
 * <p>Provides type-safe wrappers for thread-local storage, thread lifecycle, profiler, flame graph
 * collector, pooling allocator, trap introspection, async runtime, coredump, WASI
 * threads, and WIT parser native functions.
 */
public final class NativeExecutionBindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeExecutionBindings.class.getName());

  private static volatile NativeExecutionBindings instance;
  private static final Object INSTANCE_LOCK = new Object();

  private NativeExecutionBindings() {
    super();
    initializeBindings();
    markInitialized();
    LOGGER.fine("Initialized NativeExecutionBindings successfully");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeExecutionBindings getInstance() {
    NativeExecutionBindings result = instance;
    if (result == null) {
      synchronized (INSTANCE_LOCK) {
        result = instance;
        if (result == null) {
          instance = result = new NativeExecutionBindings();
        }
      }
    }
    return result;
  }

  private void initializeBindings() {
    // Performance Profiler Functions
    addFunctionBinding(
        "wasmtime4j_profiler_create", FunctionDescriptor.of(ValueLayout.ADDRESS)); // -> profiler*
    addFunctionBinding(
        "wasmtime4j_profiler_start",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_stop",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_modules_compiled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_total_compilation_time_nanos",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // nanos
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_average_compilation_time_nanos",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // nanos
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_bytes_compiled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // bytes
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_cache_hits",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // hits
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_cache_misses",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // misses
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_optimized_modules",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_current_memory_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // bytes
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_peak_memory_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // bytes
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_uptime_nanos",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // nanos
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_function_calls_per_second",
        FunctionDescriptor.of(
            ValueLayout.JAVA_DOUBLE, // calls_per_sec
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_total_function_calls",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_get_total_execution_time_nanos",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // nanos
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_record_compilation",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS, // profiler
            ValueLayout.JAVA_LONG, // compilation_time_nanos
            ValueLayout.JAVA_LONG, // bytecode_size
            ValueLayout.JAVA_BOOLEAN, // cached
            ValueLayout.JAVA_BOOLEAN)); // optimized
    addFunctionBinding(
        "wasmtime4j_profiler_record_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS, // profiler
            ValueLayout.ADDRESS, // function_name (C string)
            ValueLayout.JAVA_LONG)); // execution_time_nanos
    addFunctionBinding(
        "wasmtime4j_profiler_reset",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS)); // profiler
    addFunctionBinding(
        "wasmtime4j_profiler_is_profiling",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // is_profiling
            ValueLayout.ADDRESS)); // profiler

    // Flame Graph Collector Functions
    addFunctionBinding(
        "wasmtime4j_flame_graph_collector_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // FlameGraphCollector pointer
            ValueLayout.JAVA_LONG, // max_samples (usize)
            ValueLayout.JAVA_LONG)); // sampling_interval_ms
    addFunctionBinding(
        "wasmtime4j_flame_graph_collector_start",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS)); // collector
    addFunctionBinding(
        "wasmtime4j_flame_graph_collector_record_sample",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS, // collector
            ValueLayout.ADDRESS, // function_name (C string)
            ValueLayout.ADDRESS, // file_name (C string)
            ValueLayout.JAVA_INT, // line_number (u32)
            ValueLayout.JAVA_LONG)); // duration_nanos
    addFunctionBinding(
        "wasmtime4j_flame_graph_collector_export_svg",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // success
            ValueLayout.ADDRESS, // collector
            ValueLayout.JAVA_INT, // width (u32)
            ValueLayout.JAVA_INT, // height (u32)
            ValueLayout.ADDRESS, // output_buffer
            ValueLayout.JAVA_LONG)); // buffer_size
    addFunctionBinding(
        "wasmtime4j_flame_graph_collector_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // collector

    // Pooling Allocator Functions
    addFunctionBinding(
        "wasmtime4j_pooling_allocator_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_create_with_config",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return allocator*
            ValueLayout.JAVA_LONG, // instance_pool_size (usize)
            ValueLayout.JAVA_LONG, // max_memory_per_instance
            ValueLayout.JAVA_INT, // stack_size
            ValueLayout.JAVA_LONG, // max_stacks (usize)
            ValueLayout.JAVA_INT, // max_tables_per_instance
            ValueLayout.JAVA_LONG, // max_tables (usize)
            ValueLayout.JAVA_BOOLEAN, // memory_decommit_enabled
            ValueLayout.JAVA_BOOLEAN, // pool_warming_enabled
            ValueLayout.JAVA_FLOAT)); // pool_warming_percentage

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_allocate_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // allocator*
            ValueLayout.ADDRESS)); // instance_id_out

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_reuse_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // allocator*
            ValueLayout.JAVA_LONG)); // instance_id

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_release_instance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // allocator*
            ValueLayout.JAVA_LONG)); // instance_id

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_get_statistics",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // allocator*
            ValueLayout.ADDRESS)); // stats_out

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_reset_statistics",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS)); // allocator*

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_warm_pools",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS)); // allocator*

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_perform_maintenance",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS)); // allocator*

    addFunctionBinding(
        "wasmtime4j_pooling_allocator_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // allocator*

    // Trap Introspection Functions
    addFunctionBinding(
        "wasmtime4j_panama_trap_parse_code",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // trap code
            ValueLayout.ADDRESS)); // error_message (C string)

    addFunctionBinding(
        "wasmtime4j_panama_trap_code_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // trap name (C string)
            ValueLayout.JAVA_INT)); // trap_code

    addFunctionBinding(
        "wasmtime4j_panama_trap_is_trap",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // 1 if trap, 0 otherwise
            ValueLayout.ADDRESS)); // error_message (C string)

    addFunctionBinding(
        "wasmtime4j_panama_trap_extract_function_name",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // bytes written, 0 on error
            ValueLayout.ADDRESS, // backtrace_line (C string)
            ValueLayout.ADDRESS, // out_buffer
            ValueLayout.JAVA_LONG)); // buffer_size

    addFunctionBinding(
        "wasmtime4j_panama_trap_extract_offset",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // instruction offset, -1 on error
            ValueLayout.ADDRESS)); // error_message (C string)

    addFunctionBinding(
        "wasmtime4j_panama_trap_extract_info",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // 0 on success, non-zero on error
            ValueLayout.ADDRESS, // error_message (C string)
            ValueLayout.ADDRESS)); // out_info (TrapInfo struct)

    // WASI-Threads Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_threads_is_supported",
        FunctionDescriptor.of(ValueLayout.JAVA_INT)); // returns 1 if supported

    addFunctionBinding(
        "wasmtime4j_panama_wasi_threads_context_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return context*
            ValueLayout.ADDRESS, // module_handle
            ValueLayout.ADDRESS, // linker_handle
            ValueLayout.ADDRESS)); // store_handle

    addFunctionBinding(
        "wasmtime4j_panama_wasi_threads_context_close",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // context_handle

    addFunctionBinding(
        "wasmtime4j_panama_wasi_threads_spawn",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // thread_id
            ValueLayout.ADDRESS, // context_handle
            ValueLayout.JAVA_INT)); // thread_start_arg

    addFunctionBinding(
        "wasmtime4j_panama_wasi_threads_add_to_linker",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // linker_handle
            ValueLayout.ADDRESS, // store_handle
            ValueLayout.ADDRESS)); // module_handle

    // WIT Parser Functions
    addFunctionBinding(
        "wasmtime4j_wit_parser_new", FunctionDescriptor.of(ValueLayout.ADDRESS)); // returns parser*

    addFunctionBinding(
        "wasmtime4j_wit_parser_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // parser_ptr

    addFunctionBinding(
        "wasmtime4j_wit_parser_parse_interface",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // parser_ptr
            ValueLayout.ADDRESS, // wit_text (C string)
            ValueLayout.ADDRESS)); // result_ptr (out)

    addFunctionBinding(
        "wasmtime4j_wit_parser_validate_syntax",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // parser_ptr
            ValueLayout.ADDRESS, // wit_text (C string)
            ValueLayout.ADDRESS)); // valid_ptr (out)

    // Coredump Functions
    addFunctionBinding(
        "wasmtime4j_coredump_free",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns c_int (0 on success, -1 on error)
            ValueLayout.JAVA_LONG)); // coredump_id (u64)

    addFunctionBinding(
        "wasmtime4j_coredump_get_frame_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns frame count or -1 on error
            ValueLayout.JAVA_LONG)); // coredump_id (u64)

    addFunctionBinding(
        "wasmtime4j_coredump_get_trap_message",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns char* (must be freed)
            ValueLayout.JAVA_LONG)); // coredump_id (u64)

    addFunctionBinding(
        "wasmtime4j_coredump_get_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns char* (must be freed)
            ValueLayout.JAVA_LONG)); // coredump_id (u64)

    addFunctionBinding(
        "wasmtime4j_coredump_string_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // char*

    addFunctionBinding(
        "wasmtime4j_coredump_serialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 0 on success, negative on error
            ValueLayout.JAVA_LONG, // coredump_id (u64)
            ValueLayout.ADDRESS, // store_ptr (*mut Store)
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.ADDRESS, // out_ptr (*mut *mut u8)
            ValueLayout.ADDRESS)); // out_len (*mut c_long)

    addFunctionBinding(
        "wasmtime4j_coredump_bytes_free",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // ptr (*mut u8)
            ValueLayout.JAVA_LONG)); // len (c_long)

    addFunctionBinding(
        "wasmtime4j_coredump_get_frame_info",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns char* (JSON string, must be freed)
            ValueLayout.JAVA_LONG, // coredump_id (u64)
            ValueLayout.JAVA_INT)); // frame_index (c_int)

    addFunctionBinding(
        "wasmtime4j_coredump_get_all_frames",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns char* (JSON array string, must be freed)
            ValueLayout.JAVA_LONG)); // coredump_id (u64)

    addFunctionBinding(
        "wasmtime4j_coredump_get_count",
        FunctionDescriptor.of(ValueLayout.JAVA_INT)); // returns count or -1 on error

    addFunctionBinding(
        "wasmtime4j_coredump_get_all_ids",
        FunctionDescriptor.of(ValueLayout.ADDRESS)); // returns char* (JSON array of u64 IDs)

    addFunctionBinding(
        "wasmtime4j_coredump_clear_all",
        FunctionDescriptor.of(ValueLayout.JAVA_INT)); // returns 0 on success, -1 on error

    // General Memory Management
    addFunctionBinding("wasmtime4j_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // ptr
  }

  // ===== WIT Parser Functions =====

  /**
   * Creates a new WIT parser.
   *
   * @return pointer to the WIT parser, or NULL on failure
   */
  public MemorySegment witParserNew() {
    return callNativeFunction("wasmtime4j_wit_parser_new", MemorySegment.class);
  }

  /**
   * Destroys a WIT parser.
   *
   * @param parserPtr pointer to the parser
   */
  public void witParserDestroy(final MemorySegment parserPtr) {
    if (parserPtr != null && !parserPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_wit_parser_destroy", Void.class, parserPtr);
    }
  }

  /**
   * Parses a WIT interface definition.
   *
   * @param parserPtr pointer to the parser
   * @param witText the WIT text to parse
   * @param resultPtr output pointer for parsed interface JSON
   * @return 0 on success, non-zero on failure
   */
  public int witParserParseInterface(
      final MemorySegment parserPtr, final MemorySegment witText, final MemorySegment resultPtr) {
    validatePointer(parserPtr, "parserPtr");
    validatePointer(witText, "witText");
    validatePointer(resultPtr, "resultPtr");
    return callNativeFunction(
        "wasmtime4j_wit_parser_parse_interface", Integer.class, parserPtr, witText, resultPtr);
  }

  /**
   * Validates WIT syntax.
   *
   * @param parserPtr pointer to the parser
   * @param witText the WIT text to validate
   * @param validPtr output pointer for validation result (1 = valid, 0 = invalid)
   * @return 0 on success, non-zero on failure
   */
  public int witParserValidateSyntax(
      final MemorySegment parserPtr, final MemorySegment witText, final MemorySegment validPtr) {
    validatePointer(parserPtr, "parserPtr");
    validatePointer(witText, "witText");
    validatePointer(validPtr, "validPtr");
    return callNativeFunction(
        "wasmtime4j_wit_parser_validate_syntax", Integer.class, parserPtr, witText, validPtr);
  }

  // ===== Performance Profiler Functions =====

  /**
   * Creates a new performance profiler with default configuration.
   *
   * @return pointer to the profiler, or NULL on failure
   */
  public MemorySegment profilerCreate() {
    return callNativeFunction("wasmtime4j_profiler_create", MemorySegment.class);
  }

  /**
   * Starts profiling.
   *
   * @param profilerHandle pointer to the profiler
   * @return true on success, false on failure
   */
  public boolean profilerStart(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_start", Boolean.class, profilerHandle);
  }

  /**
   * Stops profiling.
   *
   * @param profilerHandle pointer to the profiler
   * @return true on success, false on failure
   */
  public boolean profilerStop(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_stop", Boolean.class, profilerHandle);
  }

  /**
   * Destroys a performance profiler.
   *
   * @param profilerHandle pointer to the profiler
   */
  public void profilerDestroy(final MemorySegment profilerHandle) {
    if (profilerHandle != null && !profilerHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_profiler_destroy", Void.class, profilerHandle);
    }
  }

  /**
   * Gets the number of modules compiled.
   *
   * @param profilerHandle pointer to the profiler
   * @return number of modules compiled
   */
  public long profilerGetModulesCompiled(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_modules_compiled", Long.class, profilerHandle);
  }

  /**
   * Gets total compilation time in nanoseconds.
   *
   * @param profilerHandle pointer to the profiler
   * @return total compilation time in nanoseconds
   */
  public long profilerGetTotalCompilationTimeNanos(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_total_compilation_time_nanos", Long.class, profilerHandle);
  }

  /**
   * Gets average compilation time in nanoseconds.
   *
   * @param profilerHandle pointer to the profiler
   * @return average compilation time in nanoseconds
   */
  public long profilerGetAverageCompilationTimeNanos(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_average_compilation_time_nanos", Long.class, profilerHandle);
  }

  /**
   * Gets total bytes of WASM bytecode compiled.
   *
   * @param profilerHandle pointer to the profiler
   * @return total bytes compiled
   */
  public long profilerGetBytesCompiled(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_get_bytes_compiled", Long.class, profilerHandle);
  }

  /**
   * Gets compilation cache hits.
   *
   * @param profilerHandle pointer to the profiler
   * @return number of cache hits
   */
  public long profilerGetCacheHits(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_get_cache_hits", Long.class, profilerHandle);
  }

  /**
   * Gets compilation cache misses.
   *
   * @param profilerHandle pointer to the profiler
   * @return number of cache misses
   */
  public long profilerGetCacheMisses(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_get_cache_misses", Long.class, profilerHandle);
  }

  /**
   * Gets number of optimized modules.
   *
   * @param profilerHandle pointer to the profiler
   * @return number of optimized modules
   */
  public long profilerGetOptimizedModules(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_optimized_modules", Long.class, profilerHandle);
  }

  /**
   * Gets current memory usage in bytes.
   *
   * @param profilerHandle pointer to the profiler
   * @return current memory usage in bytes
   */
  public long profilerGetCurrentMemoryBytes(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_current_memory_bytes", Long.class, profilerHandle);
  }

  /**
   * Gets peak memory usage in bytes.
   *
   * @param profilerHandle pointer to the profiler
   * @return peak memory usage in bytes
   */
  public long profilerGetPeakMemoryBytes(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_peak_memory_bytes", Long.class, profilerHandle);
  }

  /**
   * Gets profiler uptime in nanoseconds.
   *
   * @param profilerHandle pointer to the profiler
   * @return uptime in nanoseconds
   */
  public long profilerGetUptimeNanos(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_get_uptime_nanos", Long.class, profilerHandle);
  }

  /**
   * Gets function calls per second.
   *
   * @param profilerHandle pointer to the profiler
   * @return function calls per second
   */
  public double profilerGetFunctionCallsPerSecond(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_function_calls_per_second", Double.class, profilerHandle);
  }

  /**
   * Gets total function call count across all profiled functions.
   *
   * @param profilerHandle pointer to the profiler
   * @return total function calls
   */
  public long profilerGetTotalFunctionCalls(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_total_function_calls", Long.class, profilerHandle);
  }

  /**
   * Gets total execution time in nanoseconds across all profiled functions.
   *
   * @param profilerHandle pointer to the profiler
   * @return total execution time in nanoseconds
   */
  public long profilerGetTotalExecutionTimeNanos(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_get_total_execution_time_nanos", Long.class, profilerHandle);
  }

  /**
   * Records a module compilation.
   *
   * @param profilerHandle pointer to the profiler
   * @param compilationTimeNanos compilation time in nanoseconds
   * @param bytecodeSize bytecode size in bytes
   * @param cached whether the compilation was from cache
   * @param optimized whether the module was optimized
   * @return true on success, false on failure
   */
  public boolean profilerRecordCompilation(
      final MemorySegment profilerHandle,
      final long compilationTimeNanos,
      final long bytecodeSize,
      final boolean cached,
      final boolean optimized) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction(
        "wasmtime4j_profiler_record_compilation",
        Boolean.class,
        profilerHandle,
        compilationTimeNanos,
        bytecodeSize,
        cached,
        optimized);
  }

  /**
   * Records a function execution.
   *
   * @param profilerHandle pointer to the profiler
   * @param functionName the function name
   * @param executionTimeNanos execution time in nanoseconds
   * @param memoryDelta memory delta in bytes (positive for allocation, negative for deallocation)
   * @return true on success, false on failure
   */
  public boolean profilerRecordFunction(
      final MemorySegment profilerHandle,
      final MemorySegment functionName,
      final long executionTimeNanos,
      final long memoryDelta) {
    validatePointer(profilerHandle, "profilerHandle");
    validatePointer(functionName, "functionName");
    return callNativeFunction(
        "wasmtime4j_profiler_record_function",
        Boolean.class,
        profilerHandle,
        functionName,
        executionTimeNanos,
        memoryDelta);
  }

  /**
   * Resets all profiler statistics.
   *
   * @param profilerHandle pointer to the profiler
   * @return true on success, false on failure
   */
  public boolean profilerReset(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_reset", Boolean.class, profilerHandle);
  }

  /**
   * Checks if profiling is currently active.
   *
   * @param profilerHandle pointer to the profiler
   * @return true if profiling is active
   */
  public boolean profilerIsProfiling(final MemorySegment profilerHandle) {
    validatePointer(profilerHandle, "profilerHandle");
    return callNativeFunction("wasmtime4j_profiler_is_profiling", Boolean.class, profilerHandle);
  }

  // ===== Flame Graph Collector Functions =====

  /**
   * Creates a new flame graph collector for performance profiling visualization.
   *
   * @param maxSamples maximum number of samples to collect
   * @param samplingIntervalMs sampling interval in milliseconds
   * @return pointer to the flame graph collector, or null on failure
   */
  public MemorySegment flameGraphCollectorCreate(
      final long maxSamples, final long samplingIntervalMs) {
    return callNativeFunction(
        "wasmtime4j_flame_graph_collector_create",
        MemorySegment.class,
        maxSamples,
        samplingIntervalMs);
  }

  /**
   * Starts flame graph collection.
   *
   * @param collectorHandle pointer to the flame graph collector
   * @return true on success, false on failure
   */
  public boolean flameGraphCollectorStart(final MemorySegment collectorHandle) {
    validatePointer(collectorHandle, "collectorHandle");
    return callNativeFunction(
        "wasmtime4j_flame_graph_collector_start", Boolean.class, collectorHandle);
  }

  /**
   * Records a stack trace sample in the flame graph collector.
   *
   * @param collectorHandle pointer to the flame graph collector
   * @param functionName pointer to the function name C string
   * @param fileName pointer to the file name C string
   * @param lineNumber line number in the source file
   * @param durationNanos execution duration in nanoseconds
   * @return true on success, false on failure
   */
  public boolean flameGraphCollectorRecordSample(
      final MemorySegment collectorHandle,
      final MemorySegment functionName,
      final MemorySegment fileName,
      final int lineNumber,
      final long durationNanos) {
    validatePointer(collectorHandle, "collectorHandle");
    validatePointer(functionName, "functionName");
    validatePointer(fileName, "fileName");
    return callNativeFunction(
        "wasmtime4j_flame_graph_collector_record_sample",
        Boolean.class,
        collectorHandle,
        functionName,
        fileName,
        lineNumber,
        durationNanos);
  }

  /**
   * Exports the flame graph as an SVG image.
   *
   * @param collectorHandle pointer to the flame graph collector
   * @param width SVG width in pixels
   * @param height SVG height in pixels
   * @param outputBuffer pointer to the output buffer for the SVG data
   * @param bufferSize size of the output buffer in bytes
   * @return true on success, false on failure (e.g., buffer too small)
   */
  public boolean flameGraphCollectorExportSvg(
      final MemorySegment collectorHandle,
      final int width,
      final int height,
      final MemorySegment outputBuffer,
      final long bufferSize) {
    validatePointer(collectorHandle, "collectorHandle");
    validatePointer(outputBuffer, "outputBuffer");
    return callNativeFunction(
        "wasmtime4j_flame_graph_collector_export_svg",
        Boolean.class,
        collectorHandle,
        width,
        height,
        outputBuffer,
        bufferSize);
  }

  /**
   * Destroys a flame graph collector and frees associated resources.
   *
   * @param collectorHandle pointer to the flame graph collector to destroy
   */
  public void flameGraphCollectorDestroy(final MemorySegment collectorHandle) {
    if (collectorHandle != null && !collectorHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_flame_graph_collector_destroy", Void.class, collectorHandle);
    }
  }

  // ===== Pooling Allocator Functions =====

  /**
   * Creates a new pooling allocator with default configuration.
   *
   * @return pointer to the allocator, or null on failure
   */
  public MemorySegment poolingAllocatorCreate() {
    return callNativeFunction("wasmtime4j_pooling_allocator_create", MemorySegment.class);
  }

  /**
   * Creates a new pooling allocator with custom configuration.
   *
   * @param instancePoolSize the number of instances in the pool
   * @param maxMemoryPerInstance maximum memory per instance in bytes
   * @param stackSize stack size for WebAssembly execution
   * @param maxStacks maximum number of stacks
   * @param maxTablesPerInstance maximum tables per instance
   * @param maxTables maximum total tables
   * @param memoryDecommitEnabled whether memory decommit is enabled
   * @param poolWarmingEnabled whether pool warming is enabled
   * @param poolWarmingPercentage pool warming percentage (0.0 to 1.0)
   * @return pointer to the allocator, or null on failure
   */
  public MemorySegment poolingAllocatorCreateWithConfig(
      final int instancePoolSize,
      final long maxMemoryPerInstance,
      final int stackSize,
      final int maxStacks,
      final int maxTablesPerInstance,
      final int maxTables,
      final boolean memoryDecommitEnabled,
      final boolean poolWarmingEnabled,
      final float poolWarmingPercentage) {
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_create_with_config",
        MemorySegment.class,
        (long) instancePoolSize,
        maxMemoryPerInstance,
        stackSize,
        (long) maxStacks,
        maxTablesPerInstance,
        (long) maxTables,
        memoryDecommitEnabled,
        poolWarmingEnabled,
        poolWarmingPercentage);
  }

  /**
   * Allocates an instance from the pool.
   *
   * @param allocatorPtr pointer to the allocator
   * @param instanceIdOut pointer to store the instance ID
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorAllocateInstance(
      final MemorySegment allocatorPtr, final MemorySegment instanceIdOut) {
    validatePointer(allocatorPtr, "allocatorPtr");
    validatePointer(instanceIdOut, "instanceIdOut");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_allocate_instance",
        Boolean.class,
        allocatorPtr,
        instanceIdOut);
  }

  /**
   * Reuses an existing instance from the pool.
   *
   * @param allocatorPtr pointer to the allocator
   * @param instanceId the instance ID to reuse
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorReuseInstance(
      final MemorySegment allocatorPtr, final long instanceId) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_reuse_instance", Boolean.class, allocatorPtr, instanceId);
  }

  /**
   * Releases an instance back to the pool.
   *
   * @param allocatorPtr pointer to the allocator
   * @param instanceId the instance ID to release
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorReleaseInstance(
      final MemorySegment allocatorPtr, final long instanceId) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_release_instance", Boolean.class, allocatorPtr, instanceId);
  }

  /**
   * Gets pool statistics.
   *
   * @param allocatorPtr pointer to the allocator
   * @param statsOut pointer to store the statistics
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorGetStatistics(
      final MemorySegment allocatorPtr, final MemorySegment statsOut) {
    validatePointer(allocatorPtr, "allocatorPtr");
    validatePointer(statsOut, "statsOut");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_get_statistics", Boolean.class, allocatorPtr, statsOut);
  }

  /**
   * Resets pool statistics.
   *
   * @param allocatorPtr pointer to the allocator
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorResetStatistics(final MemorySegment allocatorPtr) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_reset_statistics", Boolean.class, allocatorPtr);
  }

  /**
   * Warms up the pools by pre-allocating resources.
   *
   * @param allocatorPtr pointer to the allocator
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorWarmPools(final MemorySegment allocatorPtr) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_warm_pools", Boolean.class, allocatorPtr);
  }

  /**
   * Performs maintenance operations on the pools.
   *
   * @param allocatorPtr pointer to the allocator
   * @return true on success, false on failure
   */
  public boolean poolingAllocatorPerformMaintenance(final MemorySegment allocatorPtr) {
    validatePointer(allocatorPtr, "allocatorPtr");
    return callNativeFunction(
        "wasmtime4j_pooling_allocator_perform_maintenance", Boolean.class, allocatorPtr);
  }

  /**
   * Destroys a pooling allocator.
   *
   * @param allocatorPtr pointer to the allocator
   */
  public void poolingAllocatorDestroy(final MemorySegment allocatorPtr) {
    if (allocatorPtr != null && !allocatorPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_pooling_allocator_destroy", Void.class, allocatorPtr);
    }
  }

  // ===== Trap Introspection Functions =====

  /** Trap code constants matching Java TrapException.TrapType enum ordinals. */
  public static final class TrapCodes {
    /** Stack overflow trap. */
    public static final int STACK_OVERFLOW = 0;

    /** Memory out of bounds trap. */
    public static final int MEMORY_OUT_OF_BOUNDS = 1;

    /** Heap misaligned trap. */
    public static final int HEAP_MISALIGNED = 2;

    /** Table out of bounds trap. */
    public static final int TABLE_OUT_OF_BOUNDS = 3;

    /** Indirect call to null trap. */
    public static final int INDIRECT_CALL_TO_NULL = 4;

    /** Bad signature trap. */
    public static final int BAD_SIGNATURE = 5;

    /** Integer overflow trap. */
    public static final int INTEGER_OVERFLOW = 6;

    /** Integer division by zero trap. */
    public static final int INTEGER_DIVISION_BY_ZERO = 7;

    /** Bad conversion to integer trap. */
    public static final int BAD_CONVERSION_TO_INTEGER = 8;

    /** Unreachable code reached trap. */
    public static final int UNREACHABLE_CODE_REACHED = 9;

    /** Interrupt trap. */
    public static final int INTERRUPT = 10;

    /** Out of fuel trap. */
    public static final int OUT_OF_FUEL = 11;

    /** Null reference trap. */
    public static final int NULL_REFERENCE = 12;

    /** Array out of bounds trap. */
    public static final int ARRAY_OUT_OF_BOUNDS = 13;

    /** Unknown trap type. */
    public static final int UNKNOWN = 14;

    private TrapCodes() {}
  }

  /**
   * Parses trap code from an error message.
   *
   * @param errorMessage the error message to parse
   * @return trap code matching TrapCodes constants
   */
  public int trapParseCode(final String errorMessage) {
    if (errorMessage == null || errorMessage.isEmpty()) {
      return TrapCodes.UNKNOWN;
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment messageSegment = arena.allocateFrom(errorMessage);
      return callNativeFunction("wasmtime4j_panama_trap_parse_code", Integer.class, messageSegment);
    }
  }

  /**
   * Gets the name of a trap code.
   *
   * @param trapCode the trap code
   * @return the trap code name
   */
  public String trapCodeName(final int trapCode) {
    final MemorySegment namePtr =
        callNativeFunction("wasmtime4j_panama_trap_code_name", MemorySegment.class, trapCode);
    if (namePtr == null || namePtr.equals(MemorySegment.NULL)) {
      return "unknown";
    }
    return namePtr.reinterpret(256).getString(0);
  }

  /**
   * Checks if an error message indicates a trap.
   *
   * @param errorMessage the error message to check
   * @return true if the message indicates a trap
   */
  public boolean trapIsTrap(final String errorMessage) {
    if (errorMessage == null || errorMessage.isEmpty()) {
      return false;
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment messageSegment = arena.allocateFrom(errorMessage);
      final int result =
          callNativeFunction("wasmtime4j_panama_trap_is_trap", Integer.class, messageSegment);
      return result == 1;
    }
  }

  /**
   * Extracts function name from a backtrace line.
   *
   * @param backtraceLine the backtrace line to parse
   * @return the function name, or null if not found
   */
  public String trapExtractFunctionName(final String backtraceLine) {
    if (backtraceLine == null || backtraceLine.isEmpty()) {
      return null;
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment lineSegment = arena.allocateFrom(backtraceLine);
      final int bufferSize = 256;
      final MemorySegment outBuffer = arena.allocate(bufferSize);
      final int bytesWritten =
          callNativeFunction(
              "wasmtime4j_panama_trap_extract_function_name",
              Integer.class,
              lineSegment,
              outBuffer,
              (long) bufferSize);
      if (bytesWritten <= 0) {
        return null;
      }
      return outBuffer.getString(0);
    }
  }

  /**
   * Extracts instruction offset from an error message.
   *
   * @param errorMessage the error message to parse
   * @return the instruction offset, or -1 if not found
   */
  public long trapExtractOffset(final String errorMessage) {
    if (errorMessage == null || errorMessage.isEmpty()) {
      return -1;
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment messageSegment = arena.allocateFrom(errorMessage);
      return callNativeFunction(
          "wasmtime4j_panama_trap_extract_offset", Long.class, messageSegment);
    }
  }

  /** Comprehensive trap info structure returned by trapExtractInfo. */
  public static final class TrapInfo {
    private final int trapCode;
    private final long instructionOffset;
    private final boolean isTrap;

    /**
     * Creates a new TrapInfo.
     *
     * @param trapCode the trap code
     * @param instructionOffset the instruction offset
     * @param isTrap whether this is a trap
     */
    public TrapInfo(final int trapCode, final long instructionOffset, final boolean isTrap) {
      this.trapCode = trapCode;
      this.instructionOffset = instructionOffset;
      this.isTrap = isTrap;
    }

    /**
     * Gets the trap code.
     *
     * @return the trap code
     */
    public int getTrapCode() {
      return trapCode;
    }

    /**
     * Gets the instruction offset.
     *
     * @return the instruction offset, or -1 if not available
     */
    public long getInstructionOffset() {
      return instructionOffset;
    }

    /**
     * Checks if this is a trap.
     *
     * @return true if this is a trap
     */
    public boolean isTrap() {
      return isTrap;
    }

    @Override
    public String toString() {
      return "TrapInfo{trapCode="
          + trapCode
          + ", instructionOffset="
          + instructionOffset
          + ", isTrap="
          + isTrap
          + '}';
    }
  }

  /**
   * Extracts comprehensive trap information from an error message.
   *
   * @param errorMessage the error message to parse
   * @return TrapInfo containing trap code, offset, and whether it's a trap
   */
  public TrapInfo trapExtractInfo(final String errorMessage) {
    if (errorMessage == null || errorMessage.isEmpty()) {
      return new TrapInfo(TrapCodes.UNKNOWN, -1, false);
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment messageSegment = arena.allocateFrom(errorMessage);
      // TrapInfo struct layout: int trap_code, long instruction_offset, int is_trap
      // Padded: 4 bytes + 4 padding + 8 bytes + 4 bytes = 20 bytes, aligned to 8 = 24 bytes
      final MemorySegment outInfo = arena.allocate(24, 8);
      final int result =
          callNativeFunction(
              "wasmtime4j_panama_trap_extract_info", Integer.class, messageSegment, outInfo);
      if (result != 0) {
        return new TrapInfo(TrapCodes.UNKNOWN, -1, false);
      }
      final int trapCode = outInfo.get(ValueLayout.JAVA_INT, 0);
      final long instructionOffset = outInfo.get(ValueLayout.JAVA_LONG, 8);
      final int isTrap = outInfo.get(ValueLayout.JAVA_INT, 16);
      return new TrapInfo(trapCode, instructionOffset, isTrap == 1);
    }
  }

  // ===== General Memory Management =====

  /**
   * Frees memory allocated by native functions.
   *
   * @param ptr the memory pointer to free
   */
  public void free(final MemorySegment ptr) {
    if (ptr != null && !ptr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_free", Void.class, ptr);
    }
  }

  // ===== WASI-Threads Functions =====

  /**
   * Checks if WASI-Threads is supported in this build.
   *
   * @return true if WASI-Threads is supported, false otherwise
   */
  public boolean wasiThreadsIsSupported() {
    try {
      final int result =
          callNativeFunction("wasmtime4j_panama_wasi_threads_is_supported", Integer.class);
      return result != 0;
    } catch (final Exception e) {
      LOGGER.fine("WASI-Threads support check failed: " + e.getMessage());
      return false;
    }
  }

  /**
   * Creates a new WASI-Threads context.
   *
   * @param moduleHandle the module memory segment
   * @param linkerHandle the linker memory segment
   * @param storeHandle the store memory segment
   * @param arena the arena for memory management
   * @return the created context memory segment, or NULL on error
   */
  public MemorySegment wasiThreadsContextCreate(
      final MemorySegment moduleHandle,
      final MemorySegment linkerHandle,
      final MemorySegment storeHandle,
      final Arena arena) {
    validatePointer(moduleHandle, "moduleHandle");
    validatePointer(linkerHandle, "linkerHandle");
    validatePointer(storeHandle, "storeHandle");
    if (arena == null) {
      throw new IllegalArgumentException("Arena cannot be null");
    }

    return callNativeFunction(
        "wasmtime4j_panama_wasi_threads_context_create",
        MemorySegment.class,
        moduleHandle,
        linkerHandle,
        storeHandle);
  }

  /**
   * Closes and frees a WASI-Threads context.
   *
   * @param contextHandle the context memory segment
   */
  public void wasiThreadsContextClose(final MemorySegment contextHandle) {
    if (contextHandle != null && !contextHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_wasi_threads_context_close", Void.class, contextHandle);
    }
  }

  /**
   * Spawns a new thread using WASI-Threads.
   *
   * @param contextHandle the WASI-Threads context
   * @param threadStartArg the argument to pass to the thread start function
   * @return the thread ID (positive) on success, negative on error
   */
  public int wasiThreadsSpawn(final MemorySegment contextHandle, final int threadStartArg) {
    validatePointer(contextHandle, "contextHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_threads_spawn", Integer.class, contextHandle, threadStartArg);
  }

  /**
   * Adds the WASI-Threads thread-spawn function to a linker.
   *
   * @param linkerHandle the linker memory segment
   * @param storeHandle the store memory segment
   * @param moduleHandle the module memory segment
   */
  public void wasiThreadsAddToLinker(
      final MemorySegment linkerHandle,
      final MemorySegment storeHandle,
      final MemorySegment moduleHandle) {
    validatePointer(linkerHandle, "linkerHandle");
    validatePointer(storeHandle, "storeHandle");
    validatePointer(moduleHandle, "moduleHandle");

    final int result =
        callNativeFunction(
            "wasmtime4j_panama_wasi_threads_add_to_linker",
            Integer.class,
            linkerHandle,
            storeHandle,
            moduleHandle);

    if (result != 0) {
      throw new RuntimeException("Failed to add WASI-Threads to linker, error code: " + result);
    }
  }

  // ===== Coredump Functions =====

  /**
   * Frees a coredump from the registry.
   *
   * @param coredumpId the coredump ID
   * @return 0 on success, -1 on error
   */
  public int coredumpFree(final long coredumpId) {
    return callNativeFunction("wasmtime4j_coredump_free", Integer.class, coredumpId);
  }

  /**
   * Gets the number of frames in a coredump.
   *
   * @param coredumpId the coredump ID
   * @return frame count, or -1 on error
   */
  public int coredumpGetFrameCount(final long coredumpId) {
    return callNativeFunction("wasmtime4j_coredump_get_frame_count", Integer.class, coredumpId);
  }

  /**
   * Gets the trap message from a coredump.
   *
   * @param coredumpId the coredump ID
   * @return the trap message, or null if not available
   */
  public String coredumpGetTrapMessage(final long coredumpId) {
    final MemorySegment ptr =
        callNativeFunction("wasmtime4j_coredump_get_trap_message", MemorySegment.class, coredumpId);
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      return null;
    }
    try {
      return ptr.reinterpret(Long.MAX_VALUE).getString(0);
    } finally {
      coredumpStringFree(ptr);
    }
  }

  /**
   * Gets the name of a coredump.
   *
   * @param coredumpId the coredump ID
   * @return the coredump name, or null if not set
   */
  public String coredumpGetName(final long coredumpId) {
    final MemorySegment ptr =
        callNativeFunction("wasmtime4j_coredump_get_name", MemorySegment.class, coredumpId);
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      return null;
    }
    try {
      return ptr.reinterpret(Long.MAX_VALUE).getString(0);
    } finally {
      coredumpStringFree(ptr);
    }
  }

  /**
   * Frees a C string allocated by coredump functions.
   *
   * @param ptr the string pointer to free
   */
  public void coredumpStringFree(final MemorySegment ptr) {
    if (ptr != null && !ptr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_coredump_string_free", Void.class, ptr);
    }
  }

  /**
   * Serializes a coredump to bytes.
   *
   * @param coredumpId the coredump ID
   * @param storePtr pointer to the store
   * @param name the coredump name for serialization
   * @return the serialized bytes, or null on error
   */
  public byte[] coredumpSerialize(
      final long coredumpId, final MemorySegment storePtr, final String name) {
    validatePointer(storePtr, "storePtr");
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment namePtr = arena.allocateFrom(name);
      final MemorySegment outPtr = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outLen = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          callNativeFunction(
              "wasmtime4j_coredump_serialize",
              Integer.class,
              coredumpId,
              storePtr,
              namePtr,
              outPtr,
              outLen);

      if (result != 0) {
        return null;
      }

      final MemorySegment bytesPtr = outPtr.get(ValueLayout.ADDRESS, 0);
      final long length = outLen.get(ValueLayout.JAVA_LONG, 0);

      if (bytesPtr.equals(MemorySegment.NULL) || length <= 0) {
        return null;
      }

      try {
        final byte[] bytes = new byte[(int) length];
        bytesPtr.reinterpret(length).asByteBuffer().get(bytes);
        return bytes;
      } finally {
        coredumpBytesFree(bytesPtr, length);
      }
    }
  }

  /**
   * Frees bytes allocated by coredump serialization.
   *
   * @param ptr the bytes pointer to free
   * @param len the length of the bytes
   */
  public void coredumpBytesFree(final MemorySegment ptr, final long len) {
    if (ptr != null && !ptr.equals(MemorySegment.NULL) && len > 0) {
      callNativeFunction("wasmtime4j_coredump_bytes_free", Void.class, ptr, len);
    }
  }

  /**
   * Gets information about a specific frame in the coredump as JSON.
   *
   * @param coredumpId the coredump ID
   * @param frameIndex the frame index
   * @return JSON string with frame details, or null on error
   */
  public String coredumpGetFrameInfo(final long coredumpId, final int frameIndex) {
    final MemorySegment ptr =
        callNativeFunction(
            "wasmtime4j_coredump_get_frame_info", MemorySegment.class, coredumpId, frameIndex);
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      return null;
    }
    try {
      return ptr.reinterpret(Long.MAX_VALUE).getString(0);
    } finally {
      coredumpStringFree(ptr);
    }
  }

  /**
   * Gets all frame information as a JSON array.
   *
   * @param coredumpId the coredump ID
   * @return JSON array string with all frames, or null on error
   */
  public String coredumpGetAllFrames(final long coredumpId) {
    final MemorySegment ptr =
        callNativeFunction("wasmtime4j_coredump_get_all_frames", MemorySegment.class, coredumpId);
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      return null;
    }
    try {
      return ptr.reinterpret(Long.MAX_VALUE).getString(0);
    } finally {
      coredumpStringFree(ptr);
    }
  }

  /**
   * Gets the number of registered coredumps.
   *
   * @return the count, or -1 on error
   */
  public int coredumpGetCount() {
    return callNativeFunction("wasmtime4j_coredump_get_count", Integer.class);
  }

  /**
   * Gets all registered coredump IDs as a JSON array.
   *
   * @return JSON array of coredump IDs, or null on error
   */
  public String coredumpGetAllIds() {
    final MemorySegment ptr =
        callNativeFunction("wasmtime4j_coredump_get_all_ids", MemorySegment.class);
    if (ptr == null || ptr.equals(MemorySegment.NULL)) {
      return null;
    }
    try {
      return ptr.reinterpret(Long.MAX_VALUE).getString(0);
    } finally {
      coredumpStringFree(ptr);
    }
  }

  /**
   * Clears all registered coredumps.
   *
   * @return 0 on success, -1 on error
   */
  public int coredumpClearAll() {
    return callNativeFunction("wasmtime4j_coredump_clear_all", Integer.class);
  }
}

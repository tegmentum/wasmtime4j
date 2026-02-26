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

import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Native function bindings for Engine and Module operations.
 *
 * <p>Provides type-safe wrappers for all Wasmtime engine lifecycle and module compilation and
 * introspection native functions. Hot-path methods use eagerly initialized volatile {@link
 * MethodHandle} fields for {@code invokeExact} optimization.
 *
 * <p>Store-related bindings are in {@link NativeStoreBindings}.
 *
 * <p>This class follows the singleton pattern with initialization-on-demand holder.
 */
public final class NativeEngineBindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeEngineBindings.class.getName());

  /** Initialization-on-demand holder for thread-safe lazy singleton. */
  private static final class Holder {
    static final NativeEngineBindings INSTANCE = new NativeEngineBindings();
  }

  // Hot-path volatile MethodHandle fields for invokeExact optimization
  // Signature: () -> ADDRESS
  private volatile MethodHandle mhEngineCreate;

  // Signature: (ADDRESS, ADDRESS, JAVA_LONG) -> ADDRESS
  private volatile MethodHandle mhModuleCreate;

  // Signature: (ADDRESS, ADDRESS, JAVA_LONG) -> JAVA_INT
  private volatile MethodHandle mhModuleValidate;

  private NativeEngineBindings() {
    super();
    initializeBindings();
    initializeHotPathHandles();
    markInitialized();
    LOGGER.fine("Initialized NativeEngineBindings successfully");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeEngineBindings getInstance() {
    return Holder.INSTANCE;
  }

  /** Eagerly initializes hot-path MethodHandles after all bindings are registered. */
  private void initializeHotPathHandles() {
    FunctionBinding engineCreateBinding = getFunctionBinding("wasmtime4j_engine_create");
    if (engineCreateBinding != null) {
      this.mhEngineCreate = engineCreateBinding.getMethodHandle().orElse(null);
    }

    FunctionBinding moduleCreateBinding = getFunctionBinding("wasmtime4j_module_create");
    if (moduleCreateBinding != null) {
      this.mhModuleCreate = moduleCreateBinding.getMethodHandle().orElse(null);
    }

    FunctionBinding moduleValidateBinding = getFunctionBinding("wasmtime4j_engine_validate");
    if (moduleValidateBinding != null) {
      this.mhModuleValidate = moduleValidateBinding.getMethodHandle().orElse(null);
    }
  }

  // ===== Binding Registrations =====

  private void initializeBindings() {
    // ==================== Engine Functions ====================

    addFunctionBinding("wasmtime4j_engine_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_engine_create_from_json_config",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return engine_ptr
            ValueLayout.ADDRESS, // json_ptr
            ValueLayout.JAVA_LONG)); // json_len

    addFunctionBinding(
        "wasmtime4j_panama_engine_create_with_cache_store",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return engine_ptr
            ValueLayout.ADDRESS, // json_ptr
            ValueLayout.JAVA_LONG, // json_len
            ValueLayout.JAVA_LONG, // callback_id
            ValueLayout.ADDRESS, // get_fn pointer
            ValueLayout.ADDRESS, // insert_fn pointer
            ValueLayout.ADDRESS)); // free_fn pointer

    addFunctionBinding(
        "wasmtime4j_panama_engine_create_with_extensions",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return engine_ptr
            ValueLayout.ADDRESS, // json_ptr
            ValueLayout.JAVA_LONG, // json_len
            // CacheStore (3 params)
            ValueLayout.JAVA_LONG, // cs_callback_id
            ValueLayout.ADDRESS, // cs_get_fn
            ValueLayout.ADDRESS, // cs_insert_fn
            ValueLayout.ADDRESS, // cs_free_fn
            // MemoryCreator (7 params)
            ValueLayout.JAVA_LONG, // mc_id
            ValueLayout.ADDRESS, // mc_new_memory_fn
            ValueLayout.ADDRESS, // lm_byte_size_fn
            ValueLayout.ADDRESS, // lm_byte_capacity_fn
            ValueLayout.ADDRESS, // lm_grow_to_fn
            ValueLayout.ADDRESS, // lm_as_ptr_fn
            ValueLayout.ADDRESS, // lm_drop_fn
            // StackCreator (6 params)
            ValueLayout.JAVA_LONG, // sc_id
            ValueLayout.ADDRESS, // sc_new_stack_fn
            ValueLayout.ADDRESS, // sm_top_fn
            ValueLayout.ADDRESS, // sm_range_fn
            ValueLayout.ADDRESS, // sm_guard_range_fn
            ValueLayout.ADDRESS, // sm_drop_fn
            // CustomCodeMemory (4 params)
            ValueLayout.JAVA_LONG, // ccm_id
            ValueLayout.ADDRESS, // ccm_alignment_fn
            ValueLayout.ADDRESS, // ccm_publish_fn
            ValueLayout.ADDRESS)); // ccm_unpublish_fn

    addFunctionBinding("wasmtime4j_engine_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_engine_configure",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // option_name
            ValueLayout.ADDRESS)); // option_value

    addFunctionBinding(
        "wasmtime4j_engine_set_optimization_level",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_INT)); // level

    addFunctionBinding(
        "wasmtime4j_engine_get_optimization_level",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_set_debug_info",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_BOOLEAN)); // enabled

    addFunctionBinding(
        "wasmtime4j_engine_is_debug_info",
        FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_validate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // wasm_bytes
            ValueLayout.JAVA_LONG)); // wasm_size

    addFunctionBinding(
        "wasmtime4j_panama_engine_is_fuel_enabled",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_is_epoch_interruption_enabled",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_is_coredump_on_trap_enabled",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_get_stack_limit",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_is_pulley",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_supports_feature",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 1=supported, 0=not supported, -1=error
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // feature_name (C string)

    addFunctionBinding(
        "wasmtime4j_panama_engine_detect_host_feature",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 1=available, 0=not available
            ValueLayout.ADDRESS)); // feature_name (C string)

    addFunctionBinding(
        "wasmtime4j_panama_engine_tls_eager_initialize",
        FunctionDescriptor.ofVoid()); // no params, void return

    addFunctionBinding(
        "wasmtime4j_engine_reference_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_increment_epoch",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_same",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 1=same, 0=different, -1=error
            ValueLayout.ADDRESS, // engine_ptr1
            ValueLayout.ADDRESS)); // engine_ptr2

    addFunctionBinding(
        "wasmtime4j_panama_engine_is_async",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_is_recording",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_is_replaying",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_precompile_compatibility_hash",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // out_hash (pointer to u64)

    addFunctionBinding(
        "wasmtime4j_panama_engine_precompile_module",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // wasm_bytes
            ValueLayout.JAVA_LONG, // wasm_size
            ValueLayout.ADDRESS, // out_data_ptr
            ValueLayout.ADDRESS)); // out_len_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_detect_precompiled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: -1=not precompiled, 0=MODULE, 1=COMPONENT
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // bytes_ptr
            ValueLayout.JAVA_LONG)); // bytes_len

    addFunctionBinding(
        "wasmtime4j_panama_engine_precompile_component",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // wasm_bytes
            ValueLayout.JAVA_LONG, // wasm_size
            ValueLayout.ADDRESS, // out_data_ptr
            ValueLayout.ADDRESS)); // out_len_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_pooling_allocator_metrics",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 1=available, 0=not available, -1=error
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // out_metrics (pointer to 12 i64 values)

    // ==================== SharedMemory Functions ====================

    addFunctionBinding(
        "wasmtime4j_panama_engine_create_shared_memory",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return: memory_ptr
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_INT, // initial_pages
            ValueLayout.JAVA_INT)); // max_pages

    // ==================== WeakEngine Functions ====================

    addFunctionBinding(
        "wasmtime4j_panama_engine_create_weak",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return: weak_engine_ptr
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_weak_engine_upgrade",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return: engine_ptr or null
            ValueLayout.ADDRESS)); // weak_engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_weak_engine_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // weak_engine_ptr

    // ==================== GuestProfiler Functions ====================

    addFunctionBinding(
        "wasmtime4j_guest_profiler_new",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return: profiler_ptr (or NULL)
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // module_name (C string)
            ValueLayout.JAVA_LONG, // interval_nanos
            ValueLayout.ADDRESS, // module_ptrs (array of pointers)
            ValueLayout.ADDRESS, // module_names (array of C strings)
            ValueLayout.JAVA_INT)); // module_count

    addFunctionBinding(
        "wasmtime4j_guest_profiler_sample",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 0=ok, -1=error, -2=finished
            ValueLayout.ADDRESS, // profiler_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // delta_nanos

    addFunctionBinding(
        "wasmtime4j_guest_profiler_call_hook",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 0=ok, -1=error, -2=finished
            ValueLayout.ADDRESS, // profiler_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT)); // hook_kind (0-3)

    addFunctionBinding(
        "wasmtime4j_guest_profiler_finish",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 0=ok, -1=error, -2=finished
            ValueLayout.ADDRESS, // profiler_ptr
            ValueLayout.ADDRESS, // data_out (*mut *mut u8)
            ValueLayout.ADDRESS)); // len_out (*mut c_ulong)

    addFunctionBinding(
        "wasmtime4j_guest_profiler_free_data",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // data
            ValueLayout.JAVA_LONG)); // len

    addFunctionBinding(
        "wasmtime4j_guest_profiler_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // profiler_ptr

    // ==================== Module Functions ====================

    addFunctionBinding(
        "wasmtime4j_module_compile",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // wasm_bytes
            ValueLayout.JAVA_LONG, // wasm_size
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding("wasmtime4j_module_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_module_export_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return export count
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_module_get_export_names",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return actual count
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS, // names_out (array of char*)
            ValueLayout.JAVA_LONG)); // max_count

    addFunctionBinding(
        "wasmtime4j_module_get_export_kind",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return kind (0=not found, 1=function, etc)
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS)); // name (C string)

    addFunctionBinding(
        "wasmtime4j_module_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return module*
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // wasm_bytes
            ValueLayout.JAVA_LONG)); // wasm_size

    addFunctionBinding(
        "wasmtime4j_module_create_wat",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return module*
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // wat_text (null-terminated string)

    addFunctionBinding(
        "wasmtime4j_panama_module_compile_wat",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // wat_text (null-terminated string)
            ValueLayout.ADDRESS)); // module_ptr (output)

    addFunctionBinding(
        "wasmtime4j_panama_module_compile_with_dwarf",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // wasm_bytes
            ValueLayout.JAVA_LONG, // wasm_size
            ValueLayout.ADDRESS, // dwarf_bytes
            ValueLayout.JAVA_LONG, // dwarf_size
            ValueLayout.ADDRESS)); // module_ptr (output)

    addFunctionBinding(
        "wasmtime4j_panama_module_get_import_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return import count
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_module_get_export_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return export count
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_module_get_imports_json",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return JSON string pointer
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_module_get_exports_json",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return JSON string pointer
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_module_free_string",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // string_ptr

    addFunctionBinding(
        "wasmtime4j_module_export_nth",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return found
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.JAVA_LONG, // index
            ValueLayout.ADDRESS, // name_out_ptr
            ValueLayout.ADDRESS)); // type_out_ptr

    addFunctionBinding(
        "wasmtime4j_module_import_nth",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return found
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.JAVA_LONG, // index
            ValueLayout.ADDRESS, // name_out_ptr
            ValueLayout.ADDRESS)); // type_out_ptr

    addFunctionBinding(
        "wasmtime4j_module_get_name",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return name string pointer
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_module_serialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS, // data_ptr_ptr (output)
            ValueLayout.ADDRESS)); // len_ptr (output)

    addFunctionBinding(
        "wasmtime4j_panama_module_deserialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // data_ptr
            ValueLayout.JAVA_LONG, // len
            ValueLayout.ADDRESS)); // module_ptr_ptr (output)

    addFunctionBinding(
        "wasmtime4j_panama_module_free_serialized_data",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // data_ptr
            ValueLayout.JAVA_LONG)); // len

    addFunctionBinding(
        "wasmtime4j_serializer_free_buffer",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // buffer
            ValueLayout.JAVA_LONG)); // size

    addFunctionBinding(
        "wasmtime4j_module_compile_async",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // module_bytes
            ValueLayout.JAVA_INT, // module_len
            ValueLayout.JAVA_LONG, // timeout_ms
            ValueLayout.ADDRESS, // callback
            ValueLayout.ADDRESS, // progress_callback
            ValueLayout.ADDRESS)); // user_data

    // Module cache functions
    addFunctionBinding(
        "wasmtime4j_module_cache_create_with_config",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return cache_ptr
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // cache_dir_ptr
            ValueLayout.JAVA_LONG, // max_cache_size
            ValueLayout.JAVA_LONG, // max_entries
            ValueLayout.JAVA_INT, // compression_enabled
            ValueLayout.JAVA_INT)); // compression_level

    addFunctionBinding(
        "wasmtime4j_module_cache_get_or_compile",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // cache_ptr
            ValueLayout.ADDRESS, // bytecode_ptr
            ValueLayout.JAVA_LONG, // bytecode_len
            ValueLayout.ADDRESS)); // module_out_ptr

    addFunctionBinding(
        "wasmtime4j_module_cache_precompile",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return hash length or negative on error
            ValueLayout.ADDRESS, // cache_ptr
            ValueLayout.ADDRESS, // bytecode_ptr
            ValueLayout.JAVA_LONG, // bytecode_len
            ValueLayout.ADDRESS, // hash_out_ptr
            ValueLayout.JAVA_LONG)); // hash_out_len

    addFunctionBinding(
        "wasmtime4j_module_cache_clear",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // cache_ptr

    addFunctionBinding(
        "wasmtime4j_module_cache_perform_maintenance",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // cache_ptr

    addFunctionBinding(
        "wasmtime4j_module_cache_entry_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // cache_ptr

    addFunctionBinding(
        "wasmtime4j_module_cache_hit_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // cache_ptr

    addFunctionBinding(
        "wasmtime4j_module_cache_miss_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // cache_ptr

    addFunctionBinding(
        "wasmtime4j_module_cache_storage_bytes",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // cache_ptr

    addFunctionBinding(
        "wasmtime4j_module_cache_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // cache_ptr

    // Module from_file, same, get_export_index
    addFunctionBinding(
        "wasmtime4j_panama_module_compile_from_file",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // path (null-terminated string)
            ValueLayout.ADDRESS)); // module_ptr_ptr (output)

    addFunctionBinding(
        "wasmtime4j_panama_module_same",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 1 if same, 0 if not
            ValueLayout.ADDRESS, // module_ptr1
            ValueLayout.ADDRESS)); // module_ptr2

    addFunctionBinding(
        "wasmtime4j_panama_module_get_export_index",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return index or -1
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS)); // name (null-terminated string)

    addFunctionBinding(
        "wasmtime4j_panama_module_initialize_cow_image",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, -1 on error
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_module_resources_required",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, -1 on error
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS, // min_memory_bytes_out (i64 ptr)
            ValueLayout.ADDRESS, // max_memory_bytes_out (i64 ptr)
            ValueLayout.ADDRESS, // min_table_elements_out (i64 ptr)
            ValueLayout.ADDRESS, // max_table_elements_out (i64 ptr)
            ValueLayout.ADDRESS, // num_memories_out (i32 ptr)
            ValueLayout.ADDRESS, // num_tables_out (i32 ptr)
            ValueLayout.ADDRESS, // num_globals_out (i32 ptr)
            ValueLayout.ADDRESS)); // num_functions_out (i32 ptr)

    addFunctionBinding(
        "wasmtime4j_panama_module_text",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, -1 on error
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS, // data_ptr_ptr (output)
            ValueLayout.ADDRESS)); // len_ptr (output)

    addFunctionBinding(
        "wasmtime4j_panama_module_address_map",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 0 on success, 1 if unavailable, -1 on error
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS, // code_offsets_out (output)
            ValueLayout.ADDRESS, // wasm_offsets_out (output)
            ValueLayout.ADDRESS)); // count_out (output)

    addFunctionBinding(
        "wasmtime4j_free_byte_array",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // data_ptr
            ValueLayout.JAVA_LONG)); // len

    addFunctionBinding(
        "wasmtime4j_free_address_map",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // code_offsets_ptr
            ValueLayout.ADDRESS, // wasm_offsets_ptr
            ValueLayout.JAVA_LONG)); // count
  }

  // ===========================================================================================
  // Engine Operations
  // ===========================================================================================

  /**
   * Creates a new Wasmtime engine.
   *
   * @return memory segment pointer to the engine, or null on failure
   */
  public MemorySegment engineCreate() {
    try {
      if (!isInitialized()) {
        LOGGER.severe("NativeEngineBindings not initialized, cannot create engine");
        return null;
      }

      final MethodHandle mh = mhEngineCreate;
      MemorySegment result;
      if (mh != null) {
        result = (MemorySegment) mh.invokeExact();
      } else {
        result = callNativeFunction("wasmtime4j_engine_create", MemorySegment.class);
      }
      if (result == null || result.equals(MemorySegment.NULL)) {
        LOGGER.warning("Engine creation returned null - this may indicate symbol lookup failure");
      } else {
        LOGGER.fine("Engine created successfully: " + result);
      }
      return result;
    } catch (Throwable e) {
      LOGGER.severe("Exception during engine creation: " + e.getMessage());
      return null;
    }
  }

  /**
   * Creates a new Wasmtime engine with extended configuration.
   *
   * @param config the engine configuration
   * @return memory segment pointer to the engine, or null on failure
   */
  public MemorySegment engineCreateWithConfig(
      final ai.tegmentum.wasmtime4j.config.EngineConfig config) {
    try {
      if (!isInitialized()) {
        LOGGER.severe("NativeEngineBindings not initialized, cannot create engine");
        return null;
      }

      byte[] jsonBytes = config.toJson();

      try (Arena arena = Arena.ofConfined()) {
        MemorySegment jsonSeg = arena.allocate(jsonBytes.length);
        jsonSeg.copyFrom(MemorySegment.ofArray(jsonBytes));

        MemorySegment result =
            callNativeFunction(
                "wasmtime4j_panama_engine_create_from_json_config",
                MemorySegment.class,
                jsonSeg,
                (long) jsonBytes.length);

        if (result == null || result.equals(MemorySegment.NULL)) {
          LOGGER.warning(
              "Engine creation with JSON config returned null"
                  + " - this may indicate a configuration error");
        } else {
          LOGGER.fine("Engine created with JSON config successfully: " + result);
        }
        return result;
      }
    } catch (Exception e) {
      LOGGER.severe("Exception during engine creation with JSON config: " + e.getMessage());
      return null;
    }
  }

  // ============================================================================
  // CacheStore callback support for incremental compilation
  // ============================================================================

  private static final java.util.concurrent.ConcurrentHashMap<
          Long, ai.tegmentum.wasmtime4j.config.CacheStore>
      CACHE_STORES = new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.util.concurrent.atomic.AtomicLong CACHE_STORE_ID_COUNTER =
      new java.util.concurrent.atomic.AtomicLong(1);

  // Static Arena for upcall stubs that must outlive the engine
  private static final Arena CACHE_STORE_ARENA = Arena.ofAuto();

  /**
   * CacheStore.get() upcall stub.
   *
   * <p>Called from Rust: (callbackId, keyPtr, keyLen, outDataPtr, outDataLen) -> int
   */
  private static int cacheStoreGetStub(
      final long callbackId,
      final MemorySegment keyPtr,
      final long keyLen,
      final MemorySegment outDataPtr,
      final MemorySegment outDataLen) {
    final ai.tegmentum.wasmtime4j.config.CacheStore store = CACHE_STORES.get(callbackId);
    if (store == null) {
      return 0;
    }
    try {
      final byte[] key = keyPtr.reinterpret(keyLen).toArray(ValueLayout.JAVA_BYTE);
      final byte[] result = store.get(key);
      if (result == null || result.length == 0) {
        return 0;
      }

      // Allocate native memory for the result and copy data
      final MemorySegment resultSeg =
          CACHE_STORE_ARENA.allocate(ValueLayout.JAVA_BYTE, result.length);
      resultSeg.copyFrom(MemorySegment.ofArray(result));

      // Write output pointers
      outDataPtr.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0, resultSeg);
      outDataLen
          .reinterpret(ValueLayout.JAVA_LONG.byteSize())
          .set(ValueLayout.JAVA_LONG, 0, result.length);

      return 1;
    } catch (Exception e) {
      LOGGER.warning("CacheStore.get() callback failed: " + e.getMessage());
      return 0;
    }
  }

  /**
   * CacheStore.insert() upcall stub.
   *
   * <p>Called from Rust: (callbackId, keyPtr, keyLen, valuePtr, valueLen) -> int
   */
  private static int cacheStoreInsertStub(
      final long callbackId,
      final MemorySegment keyPtr,
      final long keyLen,
      final MemorySegment valuePtr,
      final long valueLen) {
    final ai.tegmentum.wasmtime4j.config.CacheStore store = CACHE_STORES.get(callbackId);
    if (store == null) {
      return 0;
    }
    try {
      final byte[] key = keyPtr.reinterpret(keyLen).toArray(ValueLayout.JAVA_BYTE);
      final byte[] value = valuePtr.reinterpret(valueLen).toArray(ValueLayout.JAVA_BYTE);
      return store.insert(key, value) ? 1 : 0;
    } catch (Exception e) {
      LOGGER.warning("CacheStore.insert() callback failed: " + e.getMessage());
      return 0;
    }
  }

  /** Free stub - no-op since Arena manages memory. */
  private static void cacheStoreFreeStub(final MemorySegment ptr, final long len) {
    // Memory allocated by CACHE_STORE_ARENA is managed by GC, no manual free needed
  }

  /**
   * Creates a new engine with JSON configuration and a CacheStore for incremental compilation.
   *
   * @param config the engine configuration (must have an incremental cache store set)
   * @return memory segment pointer to the engine, or null on failure
   */
  public MemorySegment engineCreateWithConfigAndCacheStore(
      final ai.tegmentum.wasmtime4j.config.EngineConfig config) {
    try {
      if (!isInitialized()) {
        LOGGER.severe("NativeEngineBindings not initialized, cannot create engine");
        return null;
      }

      final ai.tegmentum.wasmtime4j.config.CacheStore cacheStore =
          config.getIncrementalCacheStore();
      if (cacheStore == null) {
        LOGGER.severe("No cache store in config, falling back to non-cached creation");
        return engineCreateWithConfig(config);
      }

      // Register cache store
      final long callbackId = CACHE_STORE_ID_COUNTER.getAndIncrement();
      CACHE_STORES.put(callbackId, cacheStore);

      // Create upcall stubs
      final MemorySegment getStub;
      final MemorySegment insertStub;
      final MemorySegment freeStub;

      try {
        final java.lang.invoke.MethodHandle getHandle =
            java.lang.invoke.MethodHandles.lookup()
                .findStatic(
                    NativeEngineBindings.class,
                    "cacheStoreGetStub",
                    java.lang.invoke.MethodType.methodType(
                        int.class,
                        long.class,
                        MemorySegment.class,
                        long.class,
                        MemorySegment.class,
                        MemorySegment.class));

        final java.lang.invoke.MethodHandle insertHandle =
            java.lang.invoke.MethodHandles.lookup()
                .findStatic(
                    NativeEngineBindings.class,
                    "cacheStoreInsertStub",
                    java.lang.invoke.MethodType.methodType(
                        int.class,
                        long.class,
                        MemorySegment.class,
                        long.class,
                        MemorySegment.class,
                        long.class));

        final java.lang.invoke.MethodHandle freeHandle =
            java.lang.invoke.MethodHandles.lookup()
                .findStatic(
                    NativeEngineBindings.class,
                    "cacheStoreFreeStub",
                    java.lang.invoke.MethodType.methodType(
                        void.class, MemorySegment.class, long.class));

        getStub =
            java.lang.foreign.Linker.nativeLinker()
                .upcallStub(
                    getHandle,
                    FunctionDescriptor.of(
                        ValueLayout.JAVA_INT, // return
                        ValueLayout.JAVA_LONG, // callback_id
                        ValueLayout.ADDRESS, // key_ptr
                        ValueLayout.JAVA_LONG, // key_len
                        ValueLayout.ADDRESS, // out_data_ptr
                        ValueLayout.ADDRESS), // out_data_len
                    CACHE_STORE_ARENA);

        insertStub =
            java.lang.foreign.Linker.nativeLinker()
                .upcallStub(
                    insertHandle,
                    FunctionDescriptor.of(
                        ValueLayout.JAVA_INT, // return
                        ValueLayout.JAVA_LONG, // callback_id
                        ValueLayout.ADDRESS, // key_ptr
                        ValueLayout.JAVA_LONG, // key_len
                        ValueLayout.ADDRESS, // value_ptr
                        ValueLayout.JAVA_LONG), // value_len
                    CACHE_STORE_ARENA);

        freeStub =
            java.lang.foreign.Linker.nativeLinker()
                .upcallStub(
                    freeHandle,
                    FunctionDescriptor.ofVoid(
                        ValueLayout.ADDRESS, // ptr
                        ValueLayout.JAVA_LONG), // len
                    CACHE_STORE_ARENA);
      } catch (Exception e) {
        LOGGER.severe("Failed to create upcall stubs for CacheStore: " + e.getMessage());
        CACHE_STORES.remove(callbackId);
        return null;
      }

      final byte[] jsonBytes = config.toJson();

      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment jsonSeg = arena.allocate(jsonBytes.length);
        jsonSeg.copyFrom(MemorySegment.ofArray(jsonBytes));

        final MemorySegment result =
            callNativeFunction(
                "wasmtime4j_panama_engine_create_with_cache_store",
                MemorySegment.class,
                jsonSeg,
                (long) jsonBytes.length,
                callbackId,
                getStub,
                insertStub,
                freeStub);

        if (result == null || result.equals(MemorySegment.NULL)) {
          LOGGER.warning("Engine creation with cache store returned null");
          CACHE_STORES.remove(callbackId);
        } else {
          LOGGER.fine("Engine created with cache store successfully: " + result);
        }
        return result;
      }
    } catch (Exception e) {
      LOGGER.severe("Exception during engine creation with cache store: " + e.getMessage());
      return null;
    }
  }

  // ============================================================================
  // Extension Traits callback support (MemoryCreator, StackCreator, CustomCodeMemory)
  // ============================================================================

  private static final java.util.concurrent.ConcurrentHashMap<
          Long, ai.tegmentum.wasmtime4j.config.MemoryCreator>
      MEMORY_CREATORS = new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.util.concurrent.atomic.AtomicLong MEMORY_CREATOR_ID_COUNTER =
      new java.util.concurrent.atomic.AtomicLong(1);

  private static final java.util.concurrent.ConcurrentHashMap<
          Long, ai.tegmentum.wasmtime4j.config.LinearMemory>
      LINEAR_MEMORIES = new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.util.concurrent.atomic.AtomicLong LINEAR_MEMORY_ID_COUNTER =
      new java.util.concurrent.atomic.AtomicLong(1);

  private static final java.util.concurrent.ConcurrentHashMap<
          Long, ai.tegmentum.wasmtime4j.config.StackCreator>
      STACK_CREATORS = new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.util.concurrent.atomic.AtomicLong STACK_CREATOR_ID_COUNTER =
      new java.util.concurrent.atomic.AtomicLong(1);

  private static final java.util.concurrent.ConcurrentHashMap<
          Long, ai.tegmentum.wasmtime4j.config.StackMemory>
      STACK_MEMORIES = new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.util.concurrent.atomic.AtomicLong STACK_MEMORY_ID_COUNTER =
      new java.util.concurrent.atomic.AtomicLong(1);

  private static final java.util.concurrent.ConcurrentHashMap<
          Long, ai.tegmentum.wasmtime4j.config.CustomCodeMemory>
      CODE_MEMORIES = new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.util.concurrent.atomic.AtomicLong CODE_MEMORY_ID_COUNTER =
      new java.util.concurrent.atomic.AtomicLong(1);

  private static final Arena EXTENSION_ARENA = Arena.ofAuto();

  // ---- MemoryCreator stubs ----

  /**
   * MemoryCreator.newMemory upcall stub.
   *
   * <p>Called from Rust: (creator_id, min_bytes, max_bytes, reserved_bytes, guard_bytes) -> i64
   *
   * <p>max_bytes and reserved_bytes use -1 to represent "no limit" (maps to OptionalLong.empty()).
   */
  private static long memoryCreatorNewMemoryStub(
      final long creatorId,
      final long minBytes,
      final long maxBytes,
      final long reservedBytes,
      final long guardBytes) {
    final ai.tegmentum.wasmtime4j.config.MemoryCreator creator = MEMORY_CREATORS.get(creatorId);
    if (creator == null) {
      return 0;
    }
    try {
      final java.util.OptionalLong max =
          maxBytes < 0 ? java.util.OptionalLong.empty() : java.util.OptionalLong.of(maxBytes);
      final java.util.OptionalLong reserved =
          reservedBytes < 0
              ? java.util.OptionalLong.empty()
              : java.util.OptionalLong.of(reservedBytes);
      final ai.tegmentum.wasmtime4j.config.LinearMemory mem =
          creator.newMemory(null, minBytes, max, reserved, guardBytes);
      if (mem == null) {
        return 0;
      }
      final long memId = LINEAR_MEMORY_ID_COUNTER.getAndIncrement();
      LINEAR_MEMORIES.put(memId, mem);
      return memId;
    } catch (Exception e) {
      LOGGER.warning("MemoryCreator.newMemory() callback failed: " + e.getMessage());
      return 0;
    }
  }

  /** LinearMemory.byteSize upcall stub. */
  private static long linearMemoryByteSizeStub(final long memoryId) {
    final ai.tegmentum.wasmtime4j.config.LinearMemory mem = LINEAR_MEMORIES.get(memoryId);
    return mem != null ? mem.byteSize() : 0;
  }

  /** LinearMemory.byteCapacity upcall stub. */
  private static long linearMemoryByteCapacityStub(final long memoryId) {
    final ai.tegmentum.wasmtime4j.config.LinearMemory mem = LINEAR_MEMORIES.get(memoryId);
    return mem != null ? mem.byteCapacity() : 0;
  }

  /** LinearMemory.growTo upcall stub. Returns 0 on success, -1 on failure. */
  private static int linearMemoryGrowToStub(final long memoryId, final long newSize) {
    final ai.tegmentum.wasmtime4j.config.LinearMemory mem = LINEAR_MEMORIES.get(memoryId);
    if (mem == null) {
      return -1;
    }
    try {
      mem.growTo(newSize);
      return 0;
    } catch (Exception e) {
      LOGGER.warning("LinearMemory.growTo() callback failed: " + e.getMessage());
      return -1;
    }
  }

  /** LinearMemory.basePointer (as_ptr) upcall stub. */
  private static MemorySegment linearMemoryAsPtrStub(final long memoryId) {
    final ai.tegmentum.wasmtime4j.config.LinearMemory mem = LINEAR_MEMORIES.get(memoryId);
    if (mem == null) {
      return MemorySegment.NULL;
    }
    return MemorySegment.ofAddress(mem.basePointer());
  }

  /** LinearMemory.close (drop) upcall stub. */
  private static void linearMemoryDropStub(final long memoryId) {
    final ai.tegmentum.wasmtime4j.config.LinearMemory mem = LINEAR_MEMORIES.remove(memoryId);
    if (mem != null) {
      try {
        mem.close();
      } catch (Exception e) {
        LOGGER.warning("LinearMemory.close() callback failed: " + e.getMessage());
      }
    }
  }

  // ---- StackCreator stubs ----

  /**
   * StackCreator.newStack upcall stub.
   *
   * <p>Called from Rust: (creator_id, size, zeroed) -> i64
   */
  private static long stackCreatorNewStackStub(
      final long creatorId, final long size, final int zeroed) {
    final ai.tegmentum.wasmtime4j.config.StackCreator creator = STACK_CREATORS.get(creatorId);
    if (creator == null) {
      return 0;
    }
    try {
      final ai.tegmentum.wasmtime4j.config.StackMemory stack = creator.newStack(size, zeroed != 0);
      if (stack == null) {
        return 0;
      }
      final long stackId = STACK_MEMORY_ID_COUNTER.getAndIncrement();
      STACK_MEMORIES.put(stackId, stack);
      return stackId;
    } catch (Exception e) {
      LOGGER.warning("StackCreator.newStack() callback failed: " + e.getMessage());
      return 0;
    }
  }

  /** StackMemory.top upcall stub. */
  private static MemorySegment stackMemoryTopStub(final long stackId) {
    final ai.tegmentum.wasmtime4j.config.StackMemory stack = STACK_MEMORIES.get(stackId);
    if (stack == null) {
      return MemorySegment.NULL;
    }
    return MemorySegment.ofAddress(stack.top());
  }

  /** StackMemory.range upcall stub: writes range_start and range_end. */
  private static void stackMemoryRangeStub(
      final long stackId, final MemorySegment outStart, final MemorySegment outEnd) {
    final ai.tegmentum.wasmtime4j.config.StackMemory stack = STACK_MEMORIES.get(stackId);
    if (stack != null) {
      outStart
          .reinterpret(ValueLayout.JAVA_LONG.byteSize())
          .set(ValueLayout.JAVA_LONG, 0, stack.rangeStart());
      outEnd
          .reinterpret(ValueLayout.JAVA_LONG.byteSize())
          .set(ValueLayout.JAVA_LONG, 0, stack.rangeEnd());
    }
  }

  /** StackMemory.guardRange upcall stub: writes guard_start and guard_end pointers. */
  private static void stackMemoryGuardRangeStub(
      final long stackId, final MemorySegment outStart, final MemorySegment outEnd) {
    final ai.tegmentum.wasmtime4j.config.StackMemory stack = STACK_MEMORIES.get(stackId);
    if (stack != null) {
      outStart
          .reinterpret(ValueLayout.ADDRESS.byteSize())
          .set(ValueLayout.ADDRESS, 0, MemorySegment.ofAddress(stack.guardRangeStart()));
      outEnd
          .reinterpret(ValueLayout.ADDRESS.byteSize())
          .set(ValueLayout.ADDRESS, 0, MemorySegment.ofAddress(stack.guardRangeEnd()));
    }
  }

  /** StackMemory.close (drop) upcall stub. */
  private static void stackMemoryDropStub(final long stackId) {
    final ai.tegmentum.wasmtime4j.config.StackMemory stack = STACK_MEMORIES.remove(stackId);
    if (stack != null) {
      try {
        stack.close();
      } catch (Exception e) {
        LOGGER.warning("StackMemory.close() callback failed: " + e.getMessage());
      }
    }
  }

  // ---- CustomCodeMemory stubs ----

  /** CustomCodeMemory.requiredAlignment upcall stub. */
  private static long codeMemoryAlignmentStub(final long codeMemId) {
    final ai.tegmentum.wasmtime4j.config.CustomCodeMemory ccm = CODE_MEMORIES.get(codeMemId);
    return ccm != null ? ccm.requiredAlignment() : 1;
  }

  /** CustomCodeMemory.publishExecutable upcall stub. Returns 0 on success, -1 on failure. */
  private static int codeMemoryPublishStub(
      final long codeMemId, final MemorySegment ptr, final long len) {
    final ai.tegmentum.wasmtime4j.config.CustomCodeMemory ccm = CODE_MEMORIES.get(codeMemId);
    if (ccm == null) {
      return -1;
    }
    try {
      ccm.publishExecutable(ptr.address(), len);
      return 0;
    } catch (Exception e) {
      LOGGER.warning("CustomCodeMemory.publishExecutable() callback failed: " + e.getMessage());
      return -1;
    }
  }

  /** CustomCodeMemory.unpublishExecutable upcall stub. Returns 0 on success, -1 on failure. */
  private static int codeMemoryUnpublishStub(
      final long codeMemId, final MemorySegment ptr, final long len) {
    final ai.tegmentum.wasmtime4j.config.CustomCodeMemory ccm = CODE_MEMORIES.get(codeMemId);
    if (ccm == null) {
      return -1;
    }
    try {
      ccm.unpublishExecutable(ptr.address(), len);
      return 0;
    } catch (Exception e) {
      LOGGER.warning("CustomCodeMemory.unpublishExecutable() callback failed: " + e.getMessage());
      return -1;
    }
  }

  /**
   * Creates a new engine with JSON configuration and all optional extension traits.
   *
   * @param config the engine configuration (may have cache store, memory creator, stack creator,
   *     and/or custom code memory set)
   * @return memory segment pointer to the engine, or null on failure
   */
  @SuppressWarnings("MethodLength")
  public MemorySegment engineCreateWithExtensions(
      final ai.tegmentum.wasmtime4j.config.EngineConfig config) {
    try {
      if (!isInitialized()) {
        LOGGER.severe("NativeEngineBindings not initialized, cannot create engine");
        return null;
      }

      // Register extensions and build upcall stubs
      long csCallbackId = 0;
      MemorySegment csGetStub = MemorySegment.NULL;
      MemorySegment csInsertStub = MemorySegment.NULL;
      MemorySegment csFreeStub = MemorySegment.NULL;

      long mcId = 0;
      MemorySegment mcNewMemoryStub = MemorySegment.NULL;
      MemorySegment lmByteSizeStub = MemorySegment.NULL;
      MemorySegment lmByteCapacityStub = MemorySegment.NULL;
      MemorySegment lmGrowToStub = MemorySegment.NULL;
      MemorySegment lmAsPtrStub = MemorySegment.NULL;
      MemorySegment lmDropStub = MemorySegment.NULL;

      long scId = 0;
      MemorySegment scNewStackStub = MemorySegment.NULL;
      MemorySegment smTopStub = MemorySegment.NULL;
      MemorySegment smRangeStub = MemorySegment.NULL;
      MemorySegment smGuardRangeStub = MemorySegment.NULL;
      MemorySegment smDropStub = MemorySegment.NULL;

      long ccmId = 0;
      MemorySegment ccmAlignmentStub = MemorySegment.NULL;
      MemorySegment ccmPublishStub = MemorySegment.NULL;
      MemorySegment ccmUnpublishStub = MemorySegment.NULL;

      final java.lang.foreign.Linker linker = java.lang.foreign.Linker.nativeLinker();
      final java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup();

      try {
        // CacheStore stubs
        if (config.getIncrementalCacheStore() != null) {
          csCallbackId = CACHE_STORE_ID_COUNTER.getAndIncrement();
          CACHE_STORES.put(csCallbackId, config.getIncrementalCacheStore());

          csGetStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "cacheStoreGetStub",
                      java.lang.invoke.MethodType.methodType(
                          int.class,
                          long.class,
                          MemorySegment.class,
                          long.class,
                          MemorySegment.class,
                          MemorySegment.class)),
                  FunctionDescriptor.of(
                      ValueLayout.JAVA_INT,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.ADDRESS,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.ADDRESS,
                      ValueLayout.ADDRESS),
                  EXTENSION_ARENA);

          csInsertStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "cacheStoreInsertStub",
                      java.lang.invoke.MethodType.methodType(
                          int.class,
                          long.class,
                          MemorySegment.class,
                          long.class,
                          MemorySegment.class,
                          long.class)),
                  FunctionDescriptor.of(
                      ValueLayout.JAVA_INT,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.ADDRESS,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.ADDRESS,
                      ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);

          csFreeStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "cacheStoreFreeStub",
                      java.lang.invoke.MethodType.methodType(
                          void.class, MemorySegment.class, long.class)),
                  FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);
        }

        // MemoryCreator stubs
        if (config.getMemoryCreator() != null) {
          mcId = MEMORY_CREATOR_ID_COUNTER.getAndIncrement();
          MEMORY_CREATORS.put(mcId, config.getMemoryCreator());

          mcNewMemoryStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "memoryCreatorNewMemoryStub",
                      java.lang.invoke.MethodType.methodType(
                          long.class, long.class, long.class, long.class, long.class, long.class)),
                  FunctionDescriptor.of(
                      ValueLayout.JAVA_LONG,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);

          lmByteSizeStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "linearMemoryByteSizeStub",
                      java.lang.invoke.MethodType.methodType(long.class, long.class)),
                  FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);

          lmByteCapacityStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "linearMemoryByteCapacityStub",
                      java.lang.invoke.MethodType.methodType(long.class, long.class)),
                  FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);

          lmGrowToStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "linearMemoryGrowToStub",
                      java.lang.invoke.MethodType.methodType(int.class, long.class, long.class)),
                  FunctionDescriptor.of(
                      ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);

          lmAsPtrStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "linearMemoryAsPtrStub",
                      java.lang.invoke.MethodType.methodType(MemorySegment.class, long.class)),
                  FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);

          lmDropStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "linearMemoryDropStub",
                      java.lang.invoke.MethodType.methodType(void.class, long.class)),
                  FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);
        }

        // StackCreator stubs
        if (config.getStackCreator() != null) {
          scId = STACK_CREATOR_ID_COUNTER.getAndIncrement();
          STACK_CREATORS.put(scId, config.getStackCreator());

          scNewStackStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "stackCreatorNewStackStub",
                      java.lang.invoke.MethodType.methodType(
                          long.class, long.class, long.class, int.class)),
                  FunctionDescriptor.of(
                      ValueLayout.JAVA_LONG,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.JAVA_INT),
                  EXTENSION_ARENA);

          smTopStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "stackMemoryTopStub",
                      java.lang.invoke.MethodType.methodType(MemorySegment.class, long.class)),
                  FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);

          smRangeStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "stackMemoryRangeStub",
                      java.lang.invoke.MethodType.methodType(
                          void.class, long.class, MemorySegment.class, MemorySegment.class)),
                  FunctionDescriptor.ofVoid(
                      ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                  EXTENSION_ARENA);

          smGuardRangeStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "stackMemoryGuardRangeStub",
                      java.lang.invoke.MethodType.methodType(
                          void.class, long.class, MemorySegment.class, MemorySegment.class)),
                  FunctionDescriptor.ofVoid(
                      ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                  EXTENSION_ARENA);

          smDropStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "stackMemoryDropStub",
                      java.lang.invoke.MethodType.methodType(void.class, long.class)),
                  FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);
        }

        // CustomCodeMemory stubs
        if (config.getCustomCodeMemory() != null) {
          ccmId = CODE_MEMORY_ID_COUNTER.getAndIncrement();
          CODE_MEMORIES.put(ccmId, config.getCustomCodeMemory());

          ccmAlignmentStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "codeMemoryAlignmentStub",
                      java.lang.invoke.MethodType.methodType(long.class, long.class)),
                  FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);

          ccmPublishStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "codeMemoryPublishStub",
                      java.lang.invoke.MethodType.methodType(
                          int.class, long.class, MemorySegment.class, long.class)),
                  FunctionDescriptor.of(
                      ValueLayout.JAVA_INT,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.ADDRESS,
                      ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);

          ccmUnpublishStub =
              linker.upcallStub(
                  lookup.findStatic(
                      NativeEngineBindings.class,
                      "codeMemoryUnpublishStub",
                      java.lang.invoke.MethodType.methodType(
                          int.class, long.class, MemorySegment.class, long.class)),
                  FunctionDescriptor.of(
                      ValueLayout.JAVA_INT,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.ADDRESS,
                      ValueLayout.JAVA_LONG),
                  EXTENSION_ARENA);
        }
      } catch (Exception e) {
        LOGGER.severe("Failed to create upcall stubs for extensions: " + e.getMessage());
        if (csCallbackId != 0) {
          CACHE_STORES.remove(csCallbackId);
        }
        if (mcId != 0) {
          MEMORY_CREATORS.remove(mcId);
        }
        if (scId != 0) {
          STACK_CREATORS.remove(scId);
        }
        if (ccmId != 0) {
          CODE_MEMORIES.remove(ccmId);
        }
        return null;
      }

      final byte[] jsonBytes = config.toJson();

      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment jsonSeg = arena.allocate(jsonBytes.length);
        jsonSeg.copyFrom(MemorySegment.ofArray(jsonBytes));

        final MemorySegment result =
            callNativeFunction(
                "wasmtime4j_panama_engine_create_with_extensions",
                MemorySegment.class,
                jsonSeg,
                (long) jsonBytes.length,
                // CacheStore
                csCallbackId,
                csGetStub,
                csInsertStub,
                csFreeStub,
                // MemoryCreator
                mcId,
                mcNewMemoryStub,
                lmByteSizeStub,
                lmByteCapacityStub,
                lmGrowToStub,
                lmAsPtrStub,
                lmDropStub,
                // StackCreator
                scId,
                scNewStackStub,
                smTopStub,
                smRangeStub,
                smGuardRangeStub,
                smDropStub,
                // CustomCodeMemory
                ccmId,
                ccmAlignmentStub,
                ccmPublishStub,
                ccmUnpublishStub);

        if (result == null || result.equals(MemorySegment.NULL)) {
          LOGGER.warning("Engine creation with extensions returned null");
          cleanupExtensionRegistries(csCallbackId, mcId, scId, ccmId);
        } else {
          LOGGER.fine("Engine created with extensions successfully: " + result);
        }
        return result;
      }
    } catch (Exception e) {
      LOGGER.severe("Exception during engine creation with extensions: " + e.getMessage());
      return null;
    }
  }

  /** Clean up extension registries when engine creation fails. */
  private static void cleanupExtensionRegistries(
      final long csId, final long mcId, final long scId, final long ccmId) {
    if (csId != 0) {
      CACHE_STORES.remove(csId);
    }
    if (mcId != 0) {
      MEMORY_CREATORS.remove(mcId);
    }
    if (scId != 0) {
      STACK_CREATORS.remove(scId);
    }
    if (ccmId != 0) {
      CODE_MEMORIES.remove(ccmId);
    }
  }

  /**
   * Destroys a Wasmtime engine.
   *
   * @param enginePtr pointer to the engine to destroy
   */
  public void engineDestroy(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    callNativeFunction("wasmtime4j_engine_destroy", Void.class, enginePtr);
  }

  /**
   * Configures an engine with options.
   *
   * @param enginePtr pointer to the engine
   * @param optionName name of the configuration option
   * @param optionValue value of the configuration option
   * @return 0 on success, negative error code on failure
   */
  public int engineConfigure(
      final MemorySegment enginePtr,
      final MemorySegment optionName,
      final MemorySegment optionValue) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(optionName, "optionName");

    return callNativeFunction(
        "wasmtime4j_engine_configure", Integer.class, enginePtr, optionName, optionValue);
  }

  /**
   * Sets the optimization level for an engine.
   *
   * @param enginePtr pointer to the engine
   * @param level the optimization level (0-2)
   * @return 0 on success, negative error code on failure
   */
  public int engineSetOptimizationLevel(final MemorySegment enginePtr, final int level) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_engine_set_optimization_level", Integer.class, enginePtr, level);
  }

  /**
   * Gets the optimization level for an engine.
   *
   * @param enginePtr pointer to the engine
   * @return the optimization level (0-2)
   */
  public int engineGetOptimizationLevel(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction("wasmtime4j_engine_get_optimization_level", Integer.class, enginePtr);
  }

  /**
   * Sets debug information generation for an engine.
   *
   * @param enginePtr pointer to the engine
   * @param enabled true to enable debug information
   * @return 0 on success, negative error code on failure
   */
  public int engineSetDebugInfo(final MemorySegment enginePtr, final boolean enabled) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_engine_set_debug_info", Integer.class, enginePtr, enabled);
  }

  /**
   * Checks if debug information generation is enabled.
   *
   * @param enginePtr pointer to the engine
   * @return true if debug information is enabled
   */
  public boolean engineIsDebugInfo(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction("wasmtime4j_engine_is_debug_info", Boolean.class, enginePtr);
  }

  /**
   * Validates WebAssembly bytecode.
   *
   * @param enginePtr pointer to the engine
   * @param wasmBytes pointer to the WASM bytecode
   * @param wasmSize size of the WASM bytecode
   * @return true if the module is valid
   */
  public boolean engineValidate(
      final MemorySegment enginePtr, final MemorySegment wasmBytes, final long wasmSize) {
    return callNativeFunction(
        "wasmtime4j_engine_validate", Boolean.class, enginePtr, wasmBytes, wasmSize);
  }

  /**
   * Checks if engine supports a feature by name.
   *
   * @param enginePtr pointer to the engine
   * @param featureName feature name string
   * @return true if supported, false otherwise
   */
  public boolean engineSupportsFeature(final MemorySegment enginePtr, final String featureName) {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment featureNameSegment = arena.allocateFrom(featureName);
      final int result =
          callNativeFunction(
              "wasmtime4j_panama_engine_supports_feature",
              Integer.class,
              enginePtr,
              featureNameSegment);
      return result == 1;
    }
  }

  /**
   * Gets the stack size limit.
   *
   * @param enginePtr pointer to the engine
   * @return stack size limit in bytes, or -1 if not set
   */
  public long engineStackSizeLimit(final MemorySegment enginePtr) {
    return callNativeFunction("wasmtime4j_panama_engine_get_stack_limit", Long.class, enginePtr);
  }

  /**
   * Checks if fuel is enabled.
   *
   * @param enginePtr pointer to the engine
   * @return true if fuel is enabled, false otherwise
   */
  public boolean engineFuelEnabled(final MemorySegment enginePtr) {
    final int result =
        callNativeFunction("wasmtime4j_panama_engine_is_fuel_enabled", Integer.class, enginePtr);
    return result == 1;
  }

  /**
   * Checks if epoch interruption is enabled.
   *
   * @param enginePtr pointer to the engine
   * @return true if epoch interruption is enabled, false otherwise
   */
  public boolean engineEpochInterruptionEnabled(final MemorySegment enginePtr) {
    final int result =
        callNativeFunction(
            "wasmtime4j_panama_engine_is_epoch_interruption_enabled", Integer.class, enginePtr);
    return result == 1;
  }

  /**
   * Checks if coredump generation on trap is enabled.
   *
   * @param enginePtr pointer to the engine
   * @return true if coredump generation is enabled, false otherwise
   */
  public boolean engineCoredumpOnTrapEnabled(final MemorySegment enginePtr) {
    final int result =
        callNativeFunction(
            "wasmtime4j_panama_engine_is_coredump_on_trap_enabled", Integer.class, enginePtr);
    return result == 1;
  }

  /**
   * Checks if two engines are the same (share the same underlying Wasmtime engine).
   *
   * @param enginePtr1 pointer to the first engine
   * @param enginePtr2 pointer to the second engine
   * @return true if both engines share the same underlying engine
   */
  public boolean engineSame(final MemorySegment enginePtr1, final MemorySegment enginePtr2) {
    final int result =
        callNativeFunction("wasmtime4j_panama_engine_same", Integer.class, enginePtr1, enginePtr2);
    return result == 1;
  }

  /**
   * Checks if async support is enabled for the engine.
   *
   * @param enginePtr pointer to the engine
   * @return true if async support is enabled, false otherwise
   */
  public boolean engineIsAsync(final MemorySegment enginePtr) {
    final int result =
        callNativeFunction("wasmtime4j_panama_engine_is_async", Integer.class, enginePtr);
    return result == 1;
  }

  /**
   * Checks if the engine is configured for execution recording.
   *
   * @param enginePtr pointer to the engine
   * @return true if recording is enabled, false otherwise
   */
  public boolean engineIsRecording(final MemorySegment enginePtr) {
    final int result =
        callNativeFunction("wasmtime4j_panama_engine_is_recording", Integer.class, enginePtr);
    return result == 1;
  }

  /**
   * Checks if the engine is configured for execution replaying.
   *
   * @param enginePtr pointer to the engine
   * @return true if replaying is enabled, false otherwise
   */
  public boolean engineIsReplaying(final MemorySegment enginePtr) {
    final int result =
        callNativeFunction("wasmtime4j_panama_engine_is_replaying", Integer.class, enginePtr);
    return result == 1;
  }

  /**
   * Gets the reference count.
   *
   * @param enginePtr pointer to the engine
   * @return reference count
   */
  public long engineReferenceCount(final MemorySegment enginePtr) {
    return callNativeFunction("wasmtime4j_engine_reference_count", Long.class, enginePtr);
  }

  /**
   * Increments the epoch counter.
   *
   * <p>This method is signal-safe and performs only an atomic increment. The epoch counter is used
   * for epoch-based interruption of WebAssembly execution.
   *
   * @param enginePtr pointer to the engine
   */
  /**
   * Detects whether a host CPU feature is available.
   *
   * @param featureName the feature name (e.g., "sse4.2", "avx2")
   * @return 1 if available, 0 if not
   */
  public int engineDetectHostFeature(final String featureName) {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment featureNameSegment = arena.allocateFrom(featureName);
      return callNativeFunction(
          "wasmtime4j_panama_engine_detect_host_feature", Integer.class, featureNameSegment);
    }
  }

  /** Eagerly initializes Wasmtime's thread-local state for the current thread. */
  public void tlsEagerInitialize() {
    callNativeFunction("wasmtime4j_panama_engine_tls_eager_initialize", Void.class);
  }

  public void engineIncrementEpoch(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    callNativeFunction("wasmtime4j_panama_engine_increment_epoch", Void.class, enginePtr);
  }

  /**
   * Unloads process-level signal handlers installed by Wasmtime.
   *
   * @param enginePtr pointer to the engine (consumed by this call)
   * @return 0 on success, -1 on failure
   */
  public int engineUnloadProcessHandlers(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_panama_engine_unload_process_handlers", Integer.class, enginePtr);
  }

  /**
   * Checks if the Pulley interpreter is being used.
   *
   * @param enginePtr pointer to the engine
   * @return true if Pulley interpreter is being used, false if using Cranelift JIT
   */
  public boolean engineIsPulley(final MemorySegment enginePtr) {
    final int result =
        callNativeFunction("wasmtime4j_panama_engine_is_pulley", Integer.class, enginePtr);
    return result == 1;
  }

  /**
   * Gets the precompilation compatibility hash for this engine.
   *
   * @param enginePtr pointer to the engine
   * @return the compatibility hash as a byte array, or null if not available
   */
  public byte[] enginePrecompileCompatibilityHash(final MemorySegment enginePtr) {
    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment outHash = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          callNativeFunction(
              "wasmtime4j_panama_engine_precompile_compatibility_hash",
              Integer.class,
              enginePtr,
              outHash);

      if (result != 0) {
        return null;
      }

      // Read the u64 hash value and convert to big-endian byte array (matching JNI behavior)
      long hashValue = outHash.get(ValueLayout.JAVA_LONG, 0);
      final byte[] bytes = new byte[8];
      for (int i = 7; i >= 0; i--) {
        bytes[i] = (byte) (hashValue & 0xFF);
        hashValue >>>= 8;
      }
      return bytes;
    } catch (final Exception e) {
      return null;
    }
  }

  /**
   * Precompiles WebAssembly bytecode into a serialized form for ahead-of-time (AOT) usage.
   *
   * <p>This method compiles the WebAssembly binary into a serialized form that can be later loaded
   * via Module.deserialize() without needing to recompile.
   *
   * @param enginePtr pointer to the engine
   * @param wasmBytes the WebAssembly bytecode to precompile
   * @return the precompiled serialized module bytes
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if precompilation fails
   */
  public byte[] enginePrecompileModule(final MemorySegment enginePtr, final byte[] wasmBytes)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    validatePointer(enginePtr, "enginePtr");
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be null or empty");
    }

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment wasmBytesSegment =
          tempArena.allocateFrom(ValueLayout.JAVA_BYTE, wasmBytes);

      final MemorySegment outDataPtr = tempArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outLenPtr = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          callNativeFunction(
              "wasmtime4j_panama_engine_precompile_module",
              Integer.class,
              enginePtr,
              wasmBytesSegment,
              (long) wasmBytes.length,
              outDataPtr,
              outLenPtr);

      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Failed to precompile module");
      }

      final MemorySegment dataPtr = outDataPtr.get(ValueLayout.ADDRESS, 0);
      final long dataLen = outLenPtr.get(ValueLayout.JAVA_LONG, 0);

      if (dataPtr.equals(MemorySegment.NULL) || dataLen <= 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Precompilation returned invalid data");
      }

      final MemorySegment dataSegment = dataPtr.reinterpret(dataLen);
      final byte[] precompiledBytes = dataSegment.toArray(ValueLayout.JAVA_BYTE);

      // Free the native memory
      serializerFreeBuffer(dataPtr, dataLen);

      return precompiledBytes;
    }
  }

  /**
   * Frees a buffer allocated by serialization functions.
   *
   * @param buffer pointer to the buffer to free
   * @param size size of the buffer
   */
  public void serializerFreeBuffer(final MemorySegment buffer, final long size) {
    if (buffer != null && !buffer.equals(MemorySegment.NULL) && size > 0) {
      callNativeFunction("wasmtime4j_serializer_free_buffer", Void.class, buffer, size);
    }
  }

  /**
   * Detects whether bytes are a precompiled artifact.
   *
   * @param enginePtr the engine pointer
   * @param bytesPtr pointer to the bytes to inspect
   * @param bytesLen length of the bytes
   * @return -1 if not precompiled, 0 for MODULE, 1 for COMPONENT
   */
  public int engineDetectPrecompiled(
      final MemorySegment enginePtr, final MemorySegment bytesPtr, final long bytesLen) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(bytesPtr, "bytesPtr");
    return callNativeFunction(
        "wasmtime4j_panama_engine_detect_precompiled",
        Integer.class,
        enginePtr,
        bytesPtr,
        bytesLen);
  }

  /**
   * Precompiles a WebAssembly component for AOT usage.
   *
   * @param enginePtr pointer to the engine
   * @param wasmBytes the WebAssembly component bytecode to precompile
   * @return the precompiled serialized component bytes
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if precompilation fails
   */
  public byte[] enginePrecompileComponent(final MemorySegment enginePtr, final byte[] wasmBytes)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    validatePointer(enginePtr, "enginePtr");
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be null or empty");
    }

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment wasmBytesSegment =
          tempArena.allocateFrom(ValueLayout.JAVA_BYTE, wasmBytes);

      final MemorySegment outDataPtr = tempArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outLenPtr = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          callNativeFunction(
              "wasmtime4j_panama_engine_precompile_component",
              Integer.class,
              enginePtr,
              wasmBytesSegment,
              (long) wasmBytes.length,
              outDataPtr,
              outLenPtr);

      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Failed to precompile component");
      }

      final MemorySegment dataPtr = outDataPtr.get(ValueLayout.ADDRESS, 0);
      final long dataLen = outLenPtr.get(ValueLayout.JAVA_LONG, 0);

      if (dataPtr.equals(MemorySegment.NULL) || dataLen <= 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Precompilation returned invalid data");
      }

      final MemorySegment dataSegment = dataPtr.reinterpret(dataLen);
      final byte[] precompiledBytes = dataSegment.toArray(ValueLayout.JAVA_BYTE);

      // Free the native memory
      serializerFreeBuffer(dataPtr, dataLen);

      return precompiledBytes;
    }
  }

  /**
   * Gets pooling allocator metrics from the engine.
   *
   * @param enginePtr pointer to the engine
   * @return a 12-element long array with metrics, or null if pooling is not enabled
   */
  public long[] enginePoolingAllocatorMetrics(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");

    try (Arena tempArena = Arena.ofConfined()) {
      // Allocate buffer for 12 i64 values
      final MemorySegment outMetrics = tempArena.allocate(ValueLayout.JAVA_LONG, 12);

      final int result =
          callNativeFunction(
              "wasmtime4j_panama_engine_pooling_allocator_metrics",
              Integer.class,
              enginePtr,
              outMetrics);

      if (result <= 0) {
        // 0 = pooling not enabled, -1 = error
        return null;
      }

      // Read the 12 long values
      final long[] metrics = new long[12];
      for (int i = 0; i < 12; i++) {
        metrics[i] = outMetrics.getAtIndex(ValueLayout.JAVA_LONG, i);
      }
      return metrics;
    } catch (final Exception e) {
      LOGGER.warning("Failed to get pooling allocator metrics: " + e.getMessage());
      return null;
    }
  }

  // ===========================================================================================
  // WeakEngine Operations
  // ===========================================================================================

  /**
   * Creates a weak reference to an engine.
   *
   * @param enginePtr pointer to the engine
   * @return pointer to the weak engine, or NULL if creation failed
   */
  /**
   * Creates a standalone shared memory from an engine.
   *
   * @param enginePtr pointer to the engine
   * @param initialPages initial number of 64KB pages
   * @param maxPages maximum number of 64KB pages
   * @return pointer to the created shared memory, or NULL on failure
   */
  public MemorySegment engineCreateSharedMemory(
      final MemorySegment enginePtr, final int initialPages, final int maxPages) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_panama_engine_create_shared_memory",
        MemorySegment.class,
        enginePtr,
        initialPages,
        maxPages);
  }

  public MemorySegment engineCreateWeak(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_panama_engine_create_weak", MemorySegment.class, enginePtr);
  }

  /**
   * Upgrades a weak engine reference to a strong engine.
   *
   * @param weakPtr pointer to the weak engine
   * @return pointer to the engine, or NULL if the engine has been dropped
   */
  public MemorySegment weakEngineUpgrade(final MemorySegment weakPtr) {
    validatePointer(weakPtr, "weakPtr");
    return callNativeFunction(
        "wasmtime4j_panama_weak_engine_upgrade", MemorySegment.class, weakPtr);
  }

  /**
   * Destroys a weak engine reference.
   *
   * @param weakPtr pointer to the weak engine to destroy
   */
  public void weakEngineDestroy(final MemorySegment weakPtr) {
    if (weakPtr != null && !weakPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_weak_engine_destroy", Void.class, weakPtr);
    }
  }

  // ===========================================================================================
  // Module Operations
  // ===========================================================================================

  /**
   * Compiles a WebAssembly module.
   *
   * @param enginePtr pointer to the engine
   * @param wasmBytes pointer to the WASM bytecode
   * @param wasmSize size of the WASM bytecode
   * @param modulePtr pointer to store the compiled module
   * @return 0 on success, negative error code on failure
   */
  public int moduleCompile(
      final MemorySegment enginePtr,
      final MemorySegment wasmBytes,
      final long wasmSize,
      final MemorySegment modulePtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(wasmBytes, "wasmBytes");
    validatePointer(modulePtr, "modulePtr");
    validateSize(wasmSize, "wasmSize");

    return callNativeFunction(
        "wasmtime4j_module_compile", Integer.class, enginePtr, wasmBytes, wasmSize, modulePtr);
  }

  /**
   * Compiles a WebAssembly module with DWARF debug package.
   *
   * @param enginePtr pointer to the engine
   * @param wasmBytes pointer to the WASM bytes
   * @param wasmSize size of the WASM bytes
   * @param dwarfBytes pointer to the DWARF package bytes
   * @param dwarfSize size of the DWARF package bytes
   * @param modulePtr pointer to store the compiled module
   * @return 0 on success, negative error code on failure
   */
  public int moduleCompileWithDwarf(
      final MemorySegment enginePtr,
      final MemorySegment wasmBytes,
      final long wasmSize,
      final MemorySegment dwarfBytes,
      final long dwarfSize,
      final MemorySegment modulePtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(wasmBytes, "wasmBytes");
    validateSize(wasmSize, "wasmSize");
    validatePointer(dwarfBytes, "dwarfBytes");
    validateSize(dwarfSize, "dwarfSize");
    validatePointer(modulePtr, "modulePtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_compile_with_dwarf",
        Integer.class,
        enginePtr,
        wasmBytes,
        wasmSize,
        dwarfBytes,
        dwarfSize,
        modulePtr);
  }

  /**
   * Compiles a WebAssembly module from WAT (WebAssembly Text format).
   *
   * @param enginePtr pointer to the engine
   * @param watText pointer to the WAT text string
   * @param modulePtr pointer to store the compiled module
   * @return 0 on success, negative error code on failure
   */
  public int moduleCompileWat(
      final MemorySegment enginePtr, final MemorySegment watText, final MemorySegment modulePtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(watText, "watText");
    validatePointer(modulePtr, "modulePtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_compile_wat", Integer.class, enginePtr, watText, modulePtr);
  }

  /**
   * Serializes a WebAssembly module.
   *
   * @param modulePtr pointer to the module to serialize
   * @param dataPtrPtr pointer to receive the serialized data pointer
   * @param lenPtr pointer to receive the data length
   * @return 0 on success, non-zero on error
   */
  public int moduleSerialize(
      final MemorySegment modulePtr, final MemorySegment dataPtrPtr, final MemorySegment lenPtr) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(dataPtrPtr, "dataPtrPtr");
    validatePointer(lenPtr, "lenPtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_serialize", Integer.class, modulePtr, dataPtrPtr, lenPtr);
  }

  /**
   * Deserializes a WebAssembly module from serialized bytes.
   *
   * @param enginePtr pointer to the engine
   * @param dataPtr pointer to the serialized module data
   * @param len length of the serialized data
   * @param modulePtrPtr pointer to store the deserialized module pointer
   * @return 0 on success, negative error code on failure
   */
  public int moduleDeserialize(
      final MemorySegment enginePtr,
      final MemorySegment dataPtr,
      final long len,
      final MemorySegment modulePtrPtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(dataPtr, "dataPtr");
    validatePointer(modulePtrPtr, "modulePtrPtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_deserialize",
        Integer.class,
        enginePtr,
        dataPtr,
        len,
        modulePtrPtr);
  }

  /**
   * Frees serialized module data.
   *
   * @param dataPtr pointer to the serialized data
   * @param len length of the data
   */
  public void moduleFreeSerializedData(final MemorySegment dataPtr, final long len) {
    if (dataPtr != null && !dataPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_module_free_serialized_data", Void.class, dataPtr, len);
    }
  }

  /**
   * Destroys a WebAssembly module.
   *
   * @param modulePtr pointer to the module to destroy
   */
  public void moduleDestroy(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    callNativeFunction("wasmtime4j_module_destroy", Void.class, modulePtr);
  }

  /**
   * Compiles a WebAssembly module from a file path.
   *
   * @param enginePtr pointer to the engine
   * @param pathPtr pointer to the file path (null-terminated string)
   * @param modulePtrPtr pointer to store the compiled module pointer
   * @return 0 on success, negative error code on failure
   */
  public int moduleCompileFromFile(
      final MemorySegment enginePtr,
      final MemorySegment pathPtr,
      final MemorySegment modulePtrPtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(pathPtr, "pathPtr");
    validatePointer(modulePtrPtr, "modulePtrPtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_compile_from_file",
        Integer.class,
        enginePtr,
        pathPtr,
        modulePtrPtr);
  }

  /**
   * Checks if two modules share the same underlying compiled code.
   *
   * @param modulePtr1 pointer to the first module
   * @param modulePtr2 pointer to the second module
   * @return 1 if the modules are the same, 0 if not
   */
  public boolean moduleSame(final MemorySegment modulePtr1, final MemorySegment modulePtr2) {
    validatePointer(modulePtr1, "modulePtr1");
    validatePointer(modulePtr2, "modulePtr2");

    final int result =
        callNativeFunction("wasmtime4j_panama_module_same", Integer.class, modulePtr1, modulePtr2);
    return result == 1;
  }

  /**
   * Gets the index of an export by name.
   *
   * @param modulePtr pointer to the module
   * @param namePtr pointer to the export name (null-terminated string)
   * @return the zero-based index, or -1 if not found
   */
  public int moduleGetExportIndex(final MemorySegment modulePtr, final MemorySegment namePtr) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(namePtr, "namePtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_get_export_index", Integer.class, modulePtr, namePtr);
  }

  /**
   * Pre-initializes a module's copy-on-write image for faster instantiation.
   *
   * @param modulePtr pointer to the module
   * @return 0 on success, negative error code on failure
   */
  public int moduleInitializeCowImage(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_initialize_cow_image", Integer.class, modulePtr);
  }

  /**
   * Gets the image range of a compiled module.
   *
   * @param modulePtr pointer to the module
   * @param startPtr output pointer for the start address (u64)
   * @param endPtr output pointer for the end address (u64)
   * @return 0 on success, negative error code on failure
   */
  public int moduleImageRange(
      final MemorySegment modulePtr, final MemorySegment startPtr, final MemorySegment endPtr) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(startPtr, "startPtr");
    validatePointer(endPtr, "endPtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_image_range", Integer.class, modulePtr, startPtr, endPtr);
  }

  /**
   * Gets the resources required to instantiate a module.
   *
   * @param modulePtr pointer to the module
   * @param minMemoryBytesOut output pointer for minimum memory bytes (i64)
   * @param maxMemoryBytesOut output pointer for maximum memory bytes (i64, -1 if unbounded)
   * @param minTableElementsOut output pointer for minimum table elements (i64)
   * @param maxTableElementsOut output pointer for maximum table elements (i64, -1 if unbounded)
   * @param numMemoriesOut output pointer for number of memories (i32)
   * @param numTablesOut output pointer for number of tables (i32)
   * @param numGlobalsOut output pointer for number of globals (i32)
   * @param numFunctionsOut output pointer for number of functions (i32)
   * @return 0 on success, negative error code on failure
   */
  public int moduleResourcesRequired(
      final MemorySegment modulePtr,
      final MemorySegment minMemoryBytesOut,
      final MemorySegment maxMemoryBytesOut,
      final MemorySegment minTableElementsOut,
      final MemorySegment maxTableElementsOut,
      final MemorySegment numMemoriesOut,
      final MemorySegment numTablesOut,
      final MemorySegment numGlobalsOut,
      final MemorySegment numFunctionsOut) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction(
        "wasmtime4j_module_resources_required",
        Integer.class,
        modulePtr,
        minMemoryBytesOut,
        maxMemoryBytesOut,
        minTableElementsOut,
        maxTableElementsOut,
        numMemoriesOut,
        numTablesOut,
        numGlobalsOut,
        numFunctionsOut);
  }

  /**
   * Creates a WebAssembly module from bytecode (Panama FFI).
   *
   * @param enginePtr pointer to the engine
   * @param wasmBytes pointer to the WASM bytecode
   * @param wasmSize size of the WASM bytecode
   * @return memory segment pointer to the module, or null on failure
   */
  public MemorySegment moduleCreate(
      final MemorySegment enginePtr, final MemorySegment wasmBytes, final long wasmSize) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(wasmBytes, "wasmBytes");
    validateSize(wasmSize, "wasmSize");

    final MethodHandle mh = mhModuleCreate;
    if (mh != null) {
      try {
        return (MemorySegment) mh.invokeExact(enginePtr, wasmBytes, wasmSize);
      } catch (Throwable t) {
        throw new RuntimeException("Native moduleCreate failed", t);
      }
    }
    return callNativeFunction(
        "wasmtime4j_module_create", MemorySegment.class, enginePtr, wasmBytes, wasmSize);
  }

  /**
   * Creates a WebAssembly module from WAT text.
   *
   * @param enginePtr pointer to the engine
   * @param watText pointer to the WAT text (null-terminated)
   * @return memory segment pointer to the module, or null on failure
   */
  public MemorySegment moduleCreateWat(final MemorySegment enginePtr, final MemorySegment watText) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(watText, "watText");
    return callNativeFunction(
        "wasmtime4j_module_create_wat", MemorySegment.class, enginePtr, watText);
  }

  /**
   * Gets the number of imports in a module.
   *
   * @param modulePtr pointer to the module
   * @return the number of imports
   */
  public long moduleImportsLen(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    final int count =
        callNativeFunction("wasmtime4j_panama_module_get_import_count", Integer.class, modulePtr);
    return count;
  }

  /**
   * Gets the nth import from a module.
   *
   * @param modulePtr pointer to the module
   * @param index the index of the import to retrieve
   * @param nameOutPtr pointer to receive the import name
   * @param typeOutPtr pointer to receive the import type
   * @return true if the import exists, false otherwise
   */
  public boolean moduleImportNth(
      final MemorySegment modulePtr,
      final long index,
      final MemorySegment nameOutPtr,
      final MemorySegment typeOutPtr) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(nameOutPtr, "nameOutPtr");
    validatePointer(typeOutPtr, "typeOutPtr");
    return callNativeFunction(
        "wasmtime4j_module_import_nth", Boolean.class, modulePtr, index, nameOutPtr, typeOutPtr);
  }

  /**
   * Gets the number of exports in a module.
   *
   * @param modulePtr pointer to the module
   * @return the number of exports
   */
  public long moduleExportsLen(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    final int count =
        callNativeFunction("wasmtime4j_panama_module_get_export_count", Integer.class, modulePtr);
    return count;
  }

  /**
   * Gets the nth export from a module.
   *
   * @param modulePtr pointer to the module
   * @param index the index of the export to retrieve
   * @param nameOutPtr pointer to receive the export name
   * @param typeOutPtr pointer to receive the export type
   * @return true if the export exists, false otherwise
   */
  public boolean moduleExportNth(
      final MemorySegment modulePtr,
      final long index,
      final MemorySegment nameOutPtr,
      final MemorySegment typeOutPtr) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(nameOutPtr, "nameOutPtr");
    validatePointer(typeOutPtr, "typeOutPtr");
    return callNativeFunction(
        "wasmtime4j_module_export_nth", Boolean.class, modulePtr, index, nameOutPtr, typeOutPtr);
  }

  /**
   * Gets module exports as JSON string.
   *
   * @param modulePtr pointer to the module
   * @return MemorySegment pointing to JSON string, or NULL on error
   */
  public MemorySegment moduleGetExportsJson(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction(
        "wasmtime4j_panama_module_get_exports_json", MemorySegment.class, modulePtr);
  }

  /**
   * Gets module imports as JSON string.
   *
   * @param modulePtr pointer to the module
   * @return MemorySegment pointing to JSON string, or NULL on error
   */
  public MemorySegment moduleGetImportsJson(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction(
        "wasmtime4j_panama_module_get_imports_json", MemorySegment.class, modulePtr);
  }

  /**
   * Frees a C string returned by module functions.
   *
   * @param strPtr pointer to the string to free
   */
  public void moduleFreeString(final MemorySegment strPtr) {
    if (strPtr != null && !strPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_module_free_string", Void.class, strPtr);
    }
  }

  /**
   * Gets compiled machine code text from a module.
   *
   * @param modulePtr pointer to the module
   * @param dataPtrPtr output pointer for the data pointer
   * @param lenPtr output pointer for the data length
   * @return 0 on success, negative error code on failure
   */
  public int moduleText(
      final MemorySegment modulePtr, final MemorySegment dataPtrPtr, final MemorySegment lenPtr) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(dataPtrPtr, "dataPtrPtr");
    validatePointer(lenPtr, "lenPtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_text", Integer.class, modulePtr, dataPtrPtr, lenPtr);
  }

  /**
   * Gets address map from a module.
   *
   * @param modulePtr pointer to the module
   * @param codeOffsetsOut output pointer for code offsets array
   * @param wasmOffsetsOut output pointer for wasm offsets array
   * @param countOut output pointer for the number of entries
   * @return 0 on success, 1 if address map not available, -1 on error
   */
  public int moduleAddressMap(
      final MemorySegment modulePtr,
      final MemorySegment codeOffsetsOut,
      final MemorySegment wasmOffsetsOut,
      final MemorySegment countOut) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(codeOffsetsOut, "codeOffsetsOut");
    validatePointer(wasmOffsetsOut, "wasmOffsetsOut");
    validatePointer(countOut, "countOut");

    return callNativeFunction(
        "wasmtime4j_panama_module_address_map",
        Integer.class,
        modulePtr,
        codeOffsetsOut,
        wasmOffsetsOut,
        countOut);
  }

  /**
   * Loads a module from a trusted file, skipping code integrity validation.
   *
   * @param enginePtr pointer to the engine
   * @param pathPtr pointer to the file path (null-terminated string)
   * @param modulePtrPtr pointer to store the loaded module pointer
   * @return 0 on success, negative error code on failure
   */
  public int moduleFromTrustedFile(
      final MemorySegment enginePtr,
      final MemorySegment pathPtr,
      final MemorySegment modulePtrPtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(pathPtr, "pathPtr");
    validatePointer(modulePtrPtr, "modulePtrPtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_from_trusted_file",
        Integer.class,
        enginePtr,
        pathPtr,
        modulePtrPtr);
  }

  /**
   * Deserializes a module from raw bytes (no file format wrapper).
   *
   * @param enginePtr pointer to the engine
   * @param dataPtr pointer to the raw module bytes
   * @param len length of the raw data
   * @param modulePtrPtr pointer to store the deserialized module pointer
   * @return 0 on success, negative error code on failure
   */
  public int moduleDeserializeRaw(
      final MemorySegment enginePtr,
      final MemorySegment dataPtr,
      final long len,
      final MemorySegment modulePtrPtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(dataPtr, "dataPtr");
    validatePointer(modulePtrPtr, "modulePtrPtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_deserialize_raw",
        Integer.class,
        enginePtr,
        dataPtr,
        len,
        modulePtrPtr);
  }

  /**
   * Deserializes a module from an open file descriptor (Unix only).
   *
   * @param enginePtr pointer to the engine
   * @param fd the file descriptor
   * @param modulePtrPtr pointer to store the deserialized module pointer
   * @return 0 on success, negative error code on failure
   */
  public int moduleDeserializeOpenFile(
      final MemorySegment enginePtr, final int fd, final MemorySegment modulePtrPtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(modulePtrPtr, "modulePtrPtr");

    return callNativeFunction(
        "wasmtime4j_panama_module_deserialize_open_file",
        Integer.class,
        enginePtr,
        fd,
        modulePtrPtr);
  }

  /**
   * Frees a byte array allocated by native code.
   *
   * @param dataPtr pointer to the data
   * @param len length of the data
   */
  public void freeByteArray(final MemorySegment dataPtr, final long len) {
    if (dataPtr != null && !dataPtr.equals(MemorySegment.NULL) && len > 0) {
      callNativeFunction("wasmtime4j_free_byte_array", Void.class, dataPtr, len);
    }
  }

  /**
   * Frees address map arrays allocated by native code.
   *
   * @param codeOffsetsPtr pointer to the code offsets array
   * @param wasmOffsetsPtr pointer to the wasm offsets array
   * @param count number of entries
   */
  public void freeAddressMap(
      final MemorySegment codeOffsetsPtr, final MemorySegment wasmOffsetsPtr, final long count) {
    if (count > 0) {
      if (codeOffsetsPtr != null && !codeOffsetsPtr.equals(MemorySegment.NULL)) {
        callNativeFunction(
            "wasmtime4j_free_address_map", Void.class, codeOffsetsPtr, wasmOffsetsPtr, count);
      }
    }
  }

  /**
   * Gets the name of a module.
   *
   * @param modulePtr pointer to the module
   * @return pointer to the module name string, or null if unnamed
   */
  public MemorySegment moduleGetName(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction("wasmtime4j_module_get_name", MemorySegment.class, modulePtr);
  }

  /**
   * Gets the number of exports in a module.
   *
   * @param modulePtr pointer to the module
   * @return the number of exports
   */
  public long moduleExportCount(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction("wasmtime4j_module_export_count", Long.class, modulePtr);
  }

  /**
   * Gets export names from a module.
   *
   * @param modulePtr pointer to the module
   * @param namesOut pointer to array that will hold char* pointers
   * @param maxCount maximum number of names to retrieve
   * @return actual number of exports retrieved
   */
  public long moduleGetExportNames(
      final MemorySegment modulePtr, final MemorySegment namesOut, final long maxCount) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(namesOut, "namesOut");
    return callNativeFunction(
        "wasmtime4j_module_get_export_names", Long.class, modulePtr, namesOut, maxCount);
  }

  /**
   * Gets the kind of a specific export by name.
   *
   * @param modulePtr pointer to the module
   * @param name pointer to null-terminated C string name
   * @return 0=not found, 1=function, 2=global, 3=memory, 4=table
   */
  public int moduleGetExportKind(final MemorySegment modulePtr, final MemorySegment name) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(name, "name");
    return callNativeFunction("wasmtime4j_module_get_export_kind", Integer.class, modulePtr, name);
  }

  /**
   * Compiles a WebAssembly module asynchronously.
   *
   * @param moduleBytes pointer to the module bytes
   * @param moduleLen length of the module bytes
   * @param timeoutMs timeout in milliseconds
   * @param callback completion callback
   * @param progressCallback progress callback
   * @param userData user data pointer
   * @return 0 on success, non-zero on error
   */
  public int moduleCompileAsync(
      final MemorySegment moduleBytes,
      final int moduleLen,
      final long timeoutMs,
      final MemorySegment callback,
      final MemorySegment progressCallback,
      final MemorySegment userData) {
    return callNativeFunction(
        "wasmtime4j_module_compile_async",
        Integer.class,
        moduleBytes,
        moduleLen,
        timeoutMs,
        callback,
        progressCallback,
        userData);
  }

  // ===== Module Cache Operations =====

  /**
   * Creates a module cache with configuration.
   *
   * @param enginePtr pointer to the engine
   * @param cacheDirPtr pointer to cache directory path
   * @param maxCacheSize maximum cache size
   * @param maxEntries maximum number of entries
   * @param compressionEnabled whether compression is enabled
   * @param compressionLevel compression level
   * @return pointer to the module cache, or null on failure
   */
  public MemorySegment moduleCacheCreateWithConfig(
      final MemorySegment enginePtr,
      final MemorySegment cacheDirPtr,
      final long maxCacheSize,
      final int maxEntries,
      final boolean compressionEnabled,
      final int compressionLevel) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_module_cache_create_with_config",
        MemorySegment.class,
        enginePtr,
        cacheDirPtr,
        maxCacheSize,
        (long) maxEntries,
        compressionEnabled ? 1 : 0,
        compressionLevel);
  }

  /**
   * Gets or compiles a module from the cache.
   *
   * @param cachePtr pointer to the module cache
   * @param bytecodePtr pointer to the bytecode
   * @param bytecodeLen length of the bytecode
   * @param moduleOutPtr pointer to store the module pointer
   * @return true on success, false on failure
   */
  public boolean moduleCacheGetOrCompile(
      final MemorySegment cachePtr,
      final MemorySegment bytecodePtr,
      final long bytecodeLen,
      final MemorySegment moduleOutPtr) {
    validatePointer(cachePtr, "cachePtr");
    validatePointer(bytecodePtr, "bytecodePtr");
    validatePointer(moduleOutPtr, "moduleOutPtr");
    final int result =
        callNativeFunction(
            "wasmtime4j_module_cache_get_or_compile",
            Integer.class,
            cachePtr,
            bytecodePtr,
            bytecodeLen,
            moduleOutPtr);
    return result != 0;
  }

  /**
   * Pre-compiles and caches a module.
   *
   * @param cachePtr pointer to the module cache
   * @param bytecodePtr pointer to the bytecode
   * @param bytecodeLen length of the bytecode
   * @param hashOutPtr pointer to store the hash output
   * @param hashOutLen length of the hash output buffer
   * @return length of hash written, or negative on error
   */
  public int moduleCachePrecompile(
      final MemorySegment cachePtr,
      final MemorySegment bytecodePtr,
      final long bytecodeLen,
      final MemorySegment hashOutPtr,
      final int hashOutLen) {
    validatePointer(cachePtr, "cachePtr");
    validatePointer(bytecodePtr, "bytecodePtr");
    validatePointer(hashOutPtr, "hashOutPtr");
    return callNativeFunction(
        "wasmtime4j_module_cache_precompile",
        Integer.class,
        cachePtr,
        bytecodePtr,
        bytecodeLen,
        hashOutPtr,
        (long) hashOutLen);
  }

  /**
   * Clears all entries from the module cache.
   *
   * @param cachePtr pointer to the module cache
   * @return true on success, false on failure
   */
  public boolean moduleCacheClear(final MemorySegment cachePtr) {
    validatePointer(cachePtr, "cachePtr");
    final int result = callNativeFunction("wasmtime4j_module_cache_clear", Integer.class, cachePtr);
    return result != 0;
  }

  /**
   * Performs cache maintenance.
   *
   * @param cachePtr pointer to the module cache
   * @return true on success, false on failure
   */
  public boolean moduleCachePerformMaintenance(final MemorySegment cachePtr) {
    validatePointer(cachePtr, "cachePtr");
    final int result =
        callNativeFunction("wasmtime4j_module_cache_perform_maintenance", Integer.class, cachePtr);
    return result != 0;
  }

  /**
   * Gets the number of entries in the cache.
   *
   * @param cachePtr pointer to the module cache
   * @return entry count, or negative on error
   */
  public long moduleCacheEntryCount(final MemorySegment cachePtr) {
    validatePointer(cachePtr, "cachePtr");
    return callNativeFunction("wasmtime4j_module_cache_entry_count", Long.class, cachePtr);
  }

  /**
   * Gets the cache hit count.
   *
   * @param cachePtr pointer to the module cache
   * @return hit count, or negative on error
   */
  public long moduleCacheHitCount(final MemorySegment cachePtr) {
    validatePointer(cachePtr, "cachePtr");
    return callNativeFunction("wasmtime4j_module_cache_hit_count", Long.class, cachePtr);
  }

  /**
   * Gets the cache miss count.
   *
   * @param cachePtr pointer to the module cache
   * @return miss count, or negative on error
   */
  public long moduleCacheMissCount(final MemorySegment cachePtr) {
    validatePointer(cachePtr, "cachePtr");
    return callNativeFunction("wasmtime4j_module_cache_miss_count", Long.class, cachePtr);
  }

  /**
   * Gets the storage bytes used by the cache.
   *
   * @param cachePtr pointer to the module cache
   * @return storage bytes used, or negative on error
   */
  public long moduleCacheStorageBytes(final MemorySegment cachePtr) {
    validatePointer(cachePtr, "cachePtr");
    return callNativeFunction("wasmtime4j_module_cache_storage_bytes", Long.class, cachePtr);
  }

  /**
   * Destroys a module cache.
   *
   * @param cachePtr pointer to the module cache
   */
  public void moduleCacheDestroy(final MemorySegment cachePtr) {
    if (cachePtr != null && !cachePtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_module_cache_destroy", Void.class, cachePtr);
    }
  }

  // ===========================================================================================
  // GuestProfiler Operations
  // ===========================================================================================

  /**
   * Creates a new guest profiler.
   *
   * @param enginePtr pointer to the engine
   * @param moduleNamePtr pointer to module name C string
   * @param intervalNanos sampling interval in nanoseconds
   * @param modulePtrs pointer to array of module pointers
   * @param moduleNamePtrs pointer to array of module name C strings
   * @param moduleCount number of modules
   * @return pointer to the profiler, or NULL on failure
   */
  public MemorySegment guestProfilerNew(
      final MemorySegment enginePtr,
      final MemorySegment moduleNamePtr,
      final long intervalNanos,
      final MemorySegment modulePtrs,
      final MemorySegment moduleNamePtrs,
      final int moduleCount) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(moduleNamePtr, "moduleNamePtr");
    return callNativeFunction(
        "wasmtime4j_guest_profiler_new",
        MemorySegment.class,
        enginePtr,
        moduleNamePtr,
        intervalNanos,
        modulePtrs,
        moduleNamePtrs,
        moduleCount);
  }

  /**
   * Creates a new guest profiler for a component.
   *
   * @param enginePtr pointer to the engine
   * @param componentNamePtr pointer to component name C string
   * @param intervalNanos sampling interval in nanoseconds
   * @param componentPtr pointer to the component
   * @param extraModulePtrs pointer to array of extra module pointers
   * @param extraModuleNamePtrs pointer to array of extra module name C strings
   * @param extraModuleCount number of extra modules
   * @return pointer to the profiler, or NULL on failure
   */
  public MemorySegment guestProfilerNewComponent(
      final MemorySegment enginePtr,
      final MemorySegment componentNamePtr,
      final long intervalNanos,
      final MemorySegment componentPtr,
      final MemorySegment extraModulePtrs,
      final MemorySegment extraModuleNamePtrs,
      final int extraModuleCount) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(componentNamePtr, "componentNamePtr");
    validatePointer(componentPtr, "componentPtr");
    return callNativeFunction(
        "wasmtime4j_guest_profiler_new_component",
        MemorySegment.class,
        enginePtr,
        componentNamePtr,
        intervalNanos,
        componentPtr,
        extraModulePtrs,
        extraModuleNamePtrs,
        extraModuleCount);
  }

  /**
   * Collects a stack sample.
   *
   * @param profilerPtr pointer to the profiler
   * @param storePtr pointer to the store
   * @param deltaNanos CPU time since previous sample in nanoseconds
   * @return 0 on success, -1 on error, -2 if already finished
   */
  public int guestProfilerSample(
      final MemorySegment profilerPtr, final MemorySegment storePtr, final long deltaNanos) {
    validatePointer(profilerPtr, "profilerPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_guest_profiler_sample", Integer.class, profilerPtr, storePtr, deltaNanos);
  }

  /**
   * Records a call hook transition.
   *
   * @param profilerPtr pointer to the profiler
   * @param storePtr pointer to the store
   * @param hookKind 0=CallingWasm, 1=ReturningFromWasm, 2=CallingHost, 3=ReturningFromHost
   * @return 0 on success, -1 on error, -2 if already finished
   */
  public int guestProfilerCallHook(
      final MemorySegment profilerPtr, final MemorySegment storePtr, final int hookKind) {
    validatePointer(profilerPtr, "profilerPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_guest_profiler_call_hook", Integer.class, profilerPtr, storePtr, hookKind);
  }

  /**
   * Finishes profiling and returns the profile data.
   *
   * @param profilerPtr pointer to the profiler
   * @param dataOut pointer to receive the data pointer
   * @param lenOut pointer to receive the data length
   * @return 0 on success, -1 on error, -2 if already finished
   */
  public int guestProfilerFinish(
      final MemorySegment profilerPtr, final MemorySegment dataOut, final MemorySegment lenOut) {
    validatePointer(profilerPtr, "profilerPtr");
    validatePointer(dataOut, "dataOut");
    validatePointer(lenOut, "lenOut");
    return callNativeFunction(
        "wasmtime4j_guest_profiler_finish", Integer.class, profilerPtr, dataOut, lenOut);
  }

  /**
   * Frees profile data returned by finish.
   *
   * @param dataPtr pointer to the data
   * @param len length of the data
   */
  public void guestProfilerFreeData(final MemorySegment dataPtr, final long len) {
    if (dataPtr != null && !dataPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_guest_profiler_free_data", Void.class, dataPtr, len);
    }
  }

  /**
   * Destroys a guest profiler.
   *
   * @param profilerPtr pointer to the profiler to destroy
   */
  public void guestProfilerDestroy(final MemorySegment profilerPtr) {
    if (profilerPtr != null && !profilerPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_guest_profiler_destroy", Void.class, profilerPtr);
    }
  }

  // ==================== CodeBuilder Operations ====================

  /**
   * Creates a new CodeBuilder for the given engine.
   *
   * @param enginePtr pointer to the engine
   * @return pointer to the new CodeBuilder
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if creation fails
   */
  public MemorySegment codeBuilderCreate(final MemorySegment enginePtr)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_panama_code_builder_create", MemorySegment.class, enginePtr);
  }

  /**
   * Sets wasm binary bytes on the code builder.
   *
   * @param builderPtr pointer to the code builder
   * @param wasmBytes the wasm bytes
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if setting bytes fails
   */
  public void codeBuilderWasmBinary(final MemorySegment builderPtr, final byte[] wasmBytes)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    validatePointer(builderPtr, "builderPtr");
    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment bytesSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, wasmBytes);
      final int result =
          callNativeFunction(
              "wasmtime4j_panama_code_builder_wasm_binary",
              Integer.class,
              builderPtr,
              bytesSegment,
              wasmBytes.length);
      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Failed to set wasm binary");
      }
    }
  }

  /**
   * Sets wasm binary or text bytes on the code builder.
   *
   * @param builderPtr pointer to the code builder
   * @param wasmBytes the wasm or WAT bytes
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if setting bytes fails
   */
  public void codeBuilderWasmBinaryOrText(final MemorySegment builderPtr, final byte[] wasmBytes)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    validatePointer(builderPtr, "builderPtr");
    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment bytesSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, wasmBytes);
      final int result =
          callNativeFunction(
              "wasmtime4j_panama_code_builder_wasm_binary_or_text",
              Integer.class,
              builderPtr,
              bytesSegment,
              wasmBytes.length);
      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Failed to set wasm binary or text");
      }
    }
  }

  /**
   * Sets DWARF package bytes on the code builder.
   *
   * @param builderPtr pointer to the code builder
   * @param dwarfBytes the DWARF package bytes
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if setting bytes fails
   */
  public void codeBuilderDwarfPackage(final MemorySegment builderPtr, final byte[] dwarfBytes)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    validatePointer(builderPtr, "builderPtr");
    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment bytesSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, dwarfBytes);
      final int result =
          callNativeFunction(
              "wasmtime4j_panama_code_builder_dwarf_package",
              Integer.class,
              builderPtr,
              bytesSegment,
              dwarfBytes.length);
      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Failed to set DWARF package");
      }
    }
  }

  /**
   * Sets hint on the code builder.
   *
   * @param builderPtr pointer to the code builder
   * @param hintOrdinal the hint ordinal (0=MODULE, 1=COMPONENT)
   */
  public void codeBuilderHint(final MemorySegment builderPtr, final int hintOrdinal) {
    validatePointer(builderPtr, "builderPtr");
    callNativeFunction("wasmtime4j_panama_code_builder_hint", Void.class, builderPtr, hintOrdinal);
  }

  /**
   * Compiles a module from the code builder.
   *
   * @param builderPtr pointer to the code builder
   * @return pointer to the compiled module
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if compilation fails
   */
  public MemorySegment codeBuilderCompileModule(final MemorySegment builderPtr)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_code_builder_compile_module", MemorySegment.class, builderPtr);
  }

  /**
   * Compiles a module to serialized bytes from the code builder.
   *
   * @param builderPtr pointer to the code builder
   * @return the serialized module bytes
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if compilation fails
   */
  public byte[] codeBuilderCompileModuleSerialized(final MemorySegment builderPtr)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    validatePointer(builderPtr, "builderPtr");
    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment outDataPtr = tempArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outLenPtr = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          callNativeFunction(
              "wasmtime4j_panama_code_builder_compile_module_serialized",
              Integer.class,
              builderPtr,
              outDataPtr,
              outLenPtr);

      if (result != 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Failed to compile module serialized via CodeBuilder");
      }

      final MemorySegment dataPtr = outDataPtr.get(ValueLayout.ADDRESS, 0);
      final long dataLen = outLenPtr.get(ValueLayout.JAVA_LONG, 0);

      if (dataPtr.equals(MemorySegment.NULL) || dataLen <= 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Serialized compilation returned invalid data");
      }

      final MemorySegment dataSegment = dataPtr.reinterpret(dataLen);
      final byte[] serializedBytes = dataSegment.toArray(ValueLayout.JAVA_BYTE);

      // Free the native memory
      serializerFreeBuffer(dataPtr, dataLen);

      return serializedBytes;
    }
  }

  /**
   * Compiles a component from the code builder.
   *
   * @param builderPtr pointer to the code builder
   * @return pointer to the compiled component
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if compilation fails
   */
  public MemorySegment codeBuilderCompileComponent(final MemorySegment builderPtr)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_code_builder_compile_component", MemorySegment.class, builderPtr);
  }

  /**
   * Compiles a component to serialized bytes from the code builder.
   *
   * @param builderPtr pointer to the code builder
   * @return the serialized component bytes
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if compilation fails
   */
  public byte[] codeBuilderCompileComponentSerialized(final MemorySegment builderPtr)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    validatePointer(builderPtr, "builderPtr");
    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment outDataPtr = tempArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outLenPtr = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          callNativeFunction(
              "wasmtime4j_panama_code_builder_compile_component_serialized",
              Integer.class,
              builderPtr,
              outDataPtr,
              outLenPtr);

      if (result != 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Failed to compile component serialized via CodeBuilder");
      }

      final MemorySegment dataPtr = outDataPtr.get(ValueLayout.ADDRESS, 0);
      final long dataLen = outLenPtr.get(ValueLayout.JAVA_LONG, 0);

      if (dataPtr.equals(MemorySegment.NULL) || dataLen <= 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Serialized compilation returned invalid data");
      }

      final MemorySegment dataSegment = dataPtr.reinterpret(dataLen);
      final byte[] serializedBytes = dataSegment.toArray(ValueLayout.JAVA_BYTE);

      // Free the native memory
      serializerFreeBuffer(dataPtr, dataLen);

      return serializedBytes;
    }
  }

  /**
   * Adds compile-time builtins from binary bytes to the code builder.
   *
   * @param builderPtr pointer to the code builder
   * @param name the name of the builtin component
   * @param wasmBytes the wasm binary bytes
   */
  public void codeBuilderCompileTimeBuiltinsBinary(
      final MemorySegment builderPtr, final String name, final byte[] wasmBytes) {
    validatePointer(builderPtr, "builderPtr");
    try (Arena tempArena = Arena.ofConfined()) {
      final byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment nameSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, nameBytes);
      final MemorySegment wasmSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, wasmBytes);
      callNativeFunction(
          "wasmtime4j_panama_code_builder_compile_time_builtins_binary",
          Void.class,
          builderPtr,
          nameSegment,
          nameBytes.length,
          wasmSegment,
          wasmBytes.length);
    }
  }

  /**
   * Adds compile-time builtins from binary or text bytes to the code builder.
   *
   * @param builderPtr pointer to the code builder
   * @param name the name of the builtin component
   * @param wasmBytes the wasm binary or WAT text bytes
   */
  public void codeBuilderCompileTimeBuiltinsBinaryOrText(
      final MemorySegment builderPtr, final String name, final byte[] wasmBytes) {
    validatePointer(builderPtr, "builderPtr");
    try (Arena tempArena = Arena.ofConfined()) {
      final byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment nameSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, nameBytes);
      final MemorySegment wasmSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, wasmBytes);
      callNativeFunction(
          "wasmtime4j_panama_code_builder_compile_time_builtins_binary_or_text",
          Void.class,
          builderPtr,
          nameSegment,
          nameBytes.length,
          wasmSegment,
          wasmBytes.length);
    }
  }

  /**
   * Exposes unsafe intrinsics at the given import name on the code builder.
   *
   * @param builderPtr pointer to the code builder
   * @param importName the import name for unsafe intrinsics
   */
  public void codeBuilderExposeUnsafeIntrinsics(
      final MemorySegment builderPtr, final String importName) {
    validatePointer(builderPtr, "builderPtr");
    try (Arena tempArena = Arena.ofConfined()) {
      final byte[] nameBytes = importName.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment nameSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, nameBytes);
      callNativeFunction(
          "wasmtime4j_panama_code_builder_expose_unsafe_intrinsics",
          Void.class,
          builderPtr,
          nameSegment,
          nameBytes.length);
    }
  }

  /**
   * Destroys a code builder.
   *
   * @param builderPtr pointer to the code builder to destroy
   */
  public void codeBuilderDestroy(final MemorySegment builderPtr) {
    if (builderPtr != null && !builderPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_code_builder_destroy", Void.class, builderPtr);
    }
  }
}

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
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Native function bindings for Engine, Module, and Store operations.
 *
 * <p>Provides type-safe wrappers for all Wasmtime engine lifecycle, module compilation and
 * introspection, and store management native functions. Hot-path methods use eagerly initialized
 * volatile {@link MethodHandle} fields for {@code invokeExact} optimization.
 *
 * <p>This class follows the singleton pattern with double-checked locking.
 */
public final class NativeEngineBindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeEngineBindings.class.getName());

  private static volatile NativeEngineBindings instance;
  private static final Object INSTANCE_LOCK = new Object();

  // Hot-path volatile MethodHandle fields for invokeExact optimization
  // Signature: () -> ADDRESS
  private volatile MethodHandle mhEngineCreate;

  // Signature: (ADDRESS, ADDRESS, JAVA_LONG) -> ADDRESS
  private volatile MethodHandle mhModuleCreate;

  // Signature: (ADDRESS, ADDRESS, JAVA_LONG) -> JAVA_INT
  private volatile MethodHandle mhModuleValidate;

  // Signature: (ADDRESS) -> ADDRESS
  private volatile MethodHandle mhStoreCreate;

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
    NativeEngineBindings result = instance;
    if (result == null) {
      synchronized (INSTANCE_LOCK) {
        result = instance;
        if (result == null) {
          instance = result = new NativeEngineBindings();
        }
      }
    }
    return result;
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

    FunctionBinding storeCreateBinding = getFunctionBinding("wasmtime4j_store_create");
    if (storeCreateBinding != null) {
      this.mhStoreCreate = storeCreateBinding.getMethodHandle().orElse(null);
    }
  }

  // ===== Binding Registrations =====

  private void initializeBindings() {
    // ==================== Engine Functions ====================

    addFunctionBinding("wasmtime4j_engine_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_engine_create_with_extended_config",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return engine_ptr
            ValueLayout.JAVA_INT, // strategy
            ValueLayout.JAVA_INT, // opt_level
            ValueLayout.JAVA_INT, // debug_info
            ValueLayout.JAVA_INT, // wasm_threads
            ValueLayout.JAVA_INT, // wasm_simd
            ValueLayout.JAVA_INT, // wasm_reference_types
            ValueLayout.JAVA_INT, // wasm_bulk_memory
            ValueLayout.JAVA_INT, // wasm_multi_value
            ValueLayout.JAVA_INT, // fuel_enabled
            ValueLayout.JAVA_INT, // max_memory_pages
            ValueLayout.JAVA_INT, // max_stack_size
            ValueLayout.JAVA_INT, // epoch_interruption
            ValueLayout.JAVA_INT, // max_instances
            ValueLayout.JAVA_INT, // async_support
            ValueLayout.JAVA_INT, // wasm_gc
            ValueLayout.JAVA_INT, // wasm_function_references
            ValueLayout.JAVA_INT, // wasm_exceptions
            ValueLayout.JAVA_LONG, // memory_reservation
            ValueLayout.JAVA_LONG, // memory_guard_size
            ValueLayout.JAVA_LONG, // memory_reservation_for_growth
            ValueLayout.JAVA_INT, // wasm_tail_call
            ValueLayout.JAVA_INT, // wasm_relaxed_simd
            ValueLayout.JAVA_INT, // wasm_multi_memory
            ValueLayout.JAVA_INT, // wasm_memory64
            ValueLayout.JAVA_INT, // wasm_extended_const
            ValueLayout.JAVA_INT, // wasm_component_model
            ValueLayout.JAVA_INT, // coredump_on_trap
            ValueLayout.JAVA_INT, // cranelift_nan_canonicalization
            ValueLayout.JAVA_INT, // wasm_custom_page_sizes
            ValueLayout.JAVA_INT)); // wasm_wide_arithmetic

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
        "wasmtime4j_panama_engine_get_memory_limit",
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
        "wasmtime4j_engine_max_instances",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_engine_reference_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_increment_epoch",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_engine_precompile_compatibility_hash",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS, // out_data_ptr
            ValueLayout.ADDRESS)); // out_len_ptr

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
        "wasmtime4j_module_validate_imports",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // module_ptr
            ValueLayout.ADDRESS, // imports_ptr
            ValueLayout.JAVA_LONG)); // imports_count

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
        "wasmtime4j_panama_module_get_custom_sections",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return JSON string pointer
            ValueLayout.ADDRESS)); // module_ptr

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

    // ==================== Store Functions ====================

    addFunctionBinding(
        "wasmtime4j_store_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return store*
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding("wasmtime4j_store_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_store_new_for_module",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return store*
            ValueLayout.ADDRESS)); // module_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_create_with_config",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_LONG, // fuel_limit
            ValueLayout.JAVA_LONG, // memory_limit_bytes
            ValueLayout.JAVA_LONG, // execution_timeout_secs
            ValueLayout.JAVA_INT, // max_instances
            ValueLayout.JAVA_INT, // max_table_elements
            ValueLayout.JAVA_INT, // max_functions
            ValueLayout.ADDRESS)); // store_ptr (output)

    addFunctionBinding(
        "wasmtime4j_store_set_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // fuel

    addFunctionBinding(
        "wasmtime4j_store_get_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // fuel_out_ptr

    addFunctionBinding(
        "wasmtime4j_store_add_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // fuel

    addFunctionBinding(
        "wasmtime4j_panama_store_set_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // fuel

    addFunctionBinding(
        "wasmtime4j_panama_store_get_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // fuel_out_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_add_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // fuel

    addFunctionBinding(
        "wasmtime4j_panama_store_consume_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // fuel
            ValueLayout.ADDRESS)); // remaining_out_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_set_epoch_deadline",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // ticks

    addFunctionBinding(
        "wasmtime4j_panama_store_epoch_deadline_trap",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_clear_epoch_deadline_callback",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_set_epoch_deadline_callback_fn",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // callback_fn (function pointer)
            ValueLayout.JAVA_LONG)); // callback_id

    addFunctionBinding(
        "wasmtime4j_panama_store_set_resource_limiter",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // callback_id
            ValueLayout.ADDRESS, // memory_growing_fn
            ValueLayout.ADDRESS, // table_growing_fn
            ValueLayout.ADDRESS, // memory_grow_failed_fn (nullable)
            ValueLayout.ADDRESS)); // table_grow_failed_fn (nullable)

    addFunctionBinding(
        "wasmtime4j_store_set_epoch_deadline",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // ticks

    addFunctionBinding(
        "wasmtime4j_store_get_execution_stats",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // execution_count_ptr
            ValueLayout.ADDRESS, // fuel_consumed_ptr
            ValueLayout.ADDRESS)); // total_execution_time_ns_ptr

    addFunctionBinding(
        "wasmtime4j_store_get_memory_usage",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // total_bytes_ptr
            ValueLayout.ADDRESS, // used_bytes_ptr
            ValueLayout.ADDRESS)); // instance_count_ptr

    addFunctionBinding(
        "wasmtime4j_store_validate",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_store_set_wasi_context",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // wasi_ctx_ptr

    addFunctionBinding(
        "wasmtime4j_store_has_wasi_context",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_epoch_deadline_async_yield_and_update",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG)); // delta ticks

    addFunctionBinding(
        "wasmtime4j_store_garbage_collect",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_store_consume_fuel",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // fuel
            ValueLayout.ADDRESS)); // remaining_ptr

    addFunctionBinding(
        "wasmtime4j_store_get_metadata",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // metadata_ptr
            ValueLayout.ADDRESS, // key_ptr
            ValueLayout.ADDRESS, // value_ptr
            ValueLayout.ADDRESS)); // size_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_gc",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_gc_async",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_capture_backtrace",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // buffer_out_ptr
            ValueLayout.ADDRESS)); // buffer_len_out_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_force_capture_backtrace",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // buffer_out_ptr
            ValueLayout.ADDRESS)); // buffer_len_out_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_throw_exception",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // exn_ref_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_take_pending_exception",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return exn_ref_ptr or NULL
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_has_pending_exception",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_set_call_hook",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_clear_call_hook",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_set_call_hook_async",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_store_clear_call_hook_async",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // store_ptr
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

      int strategy = 0; // default Cranelift
      int optLevel = 2; // default Speed
      int debugInfo = config.isDebugInfo() ? 1 : 0;
      int wasmThreads = config.isWasmThreads() ? 1 : 0;
      int wasmSimd = config.isWasmSimd() ? 1 : 0;
      int wasmReferenceTypes = config.isWasmReferenceTypes() ? 1 : 0;
      int wasmBulkMemory = config.isWasmBulkMemory() ? 1 : 0;
      int wasmMultiValue = config.isWasmMultiValue() ? 1 : 0;
      int fuelEnabled = config.isConsumeFuel() ? 1 : 0;
      int maxMemoryPages = 0; // use default
      int maxStackSize = 0; // use default
      int epochInterruption = config.isEpochInterruption() ? 1 : 0;
      int maxInstances = 0; // use default
      int asyncSupport = config.isAsyncSupport() ? 1 : 0;
      int wasmGc = config.isWasmGc() ? 1 : 0;
      int wasmFunctionReferences = config.isWasmFunctionReferences() ? 1 : 0;
      int wasmExceptions = config.isWasmExceptions() ? 1 : 0;
      long memoryReservation = config.getMemoryReservation();
      long memoryGuardSize = config.getMemoryGuardSize();
      long memoryReservationForGrowth = config.getMemoryReservationForGrowth();
      int wasmTailCall = config.isWasmTailCall() ? 1 : 0;
      int wasmRelaxedSimd = config.isWasmRelaxedSimd() ? 1 : 0;
      int wasmMultiMemory = config.isWasmMultiMemory() ? 1 : 0;
      int wasmMemory64 = config.isWasmMemory64() ? 1 : 0;
      int wasmExtendedConst = config.isWasmExtendedConstExpressions() ? 1 : 0;
      int wasmComponentModel = 0; // Component model handled separately
      int coredumpOnTrap = config.isCoredumpOnTrap() ? 1 : 0;
      int craneliftNanCanonicalization = config.isCraneliftNanCanonicalization() ? 1 : 0;
      int wasmCustomPageSizes = config.isWasmCustomPageSizes() ? 1 : 0;
      int wasmWideArithmetic = config.isWasmWideArithmetic() ? 1 : 0;

      MemorySegment result =
          callNativeFunction(
              "wasmtime4j_panama_engine_create_with_extended_config",
              MemorySegment.class,
              strategy,
              optLevel,
              debugInfo,
              wasmThreads,
              wasmSimd,
              wasmReferenceTypes,
              wasmBulkMemory,
              wasmMultiValue,
              fuelEnabled,
              maxMemoryPages,
              maxStackSize,
              epochInterruption,
              maxInstances,
              asyncSupport,
              wasmGc,
              wasmFunctionReferences,
              wasmExceptions,
              memoryReservation,
              memoryGuardSize,
              memoryReservationForGrowth,
              wasmTailCall,
              wasmRelaxedSimd,
              wasmMultiMemory,
              wasmMemory64,
              wasmExtendedConst,
              wasmComponentModel,
              coredumpOnTrap,
              craneliftNanCanonicalization,
              wasmCustomPageSizes,
              wasmWideArithmetic);

      if (result == null || result.equals(MemorySegment.NULL)) {
        LOGGER.warning(
            "Engine creation with config returned null - this may indicate symbol lookup failure");
      } else {
        LOGGER.fine("Engine created with config successfully: " + result);
      }
      return result;
    } catch (Exception e) {
      LOGGER.severe("Exception during engine creation with config: " + e.getMessage());
      return null;
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
   * Gets the memory limit in pages.
   *
   * @param enginePtr pointer to the engine
   * @return memory limit in pages, or -1 if not set
   */
  public int engineMemoryLimitPages(final MemorySegment enginePtr) {
    return callNativeFunction(
        "wasmtime4j_panama_engine_get_memory_limit", Integer.class, enginePtr);
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
   * Gets the maximum number of instances.
   *
   * @param enginePtr pointer to the engine
   * @return maximum number of instances
   */
  public long engineMaxInstances(final MemorySegment enginePtr) {
    return callNativeFunction("wasmtime4j_engine_max_instances", Long.class, enginePtr);
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
  public void engineIncrementEpoch(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    callNativeFunction("wasmtime4j_panama_engine_increment_epoch", Void.class, enginePtr);
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
      final MemorySegment outDataPtr = tempArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outLenPtr = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          callNativeFunction(
              "wasmtime4j_panama_engine_precompile_compatibility_hash",
              Integer.class,
              enginePtr,
              outDataPtr,
              outLenPtr);

      if (result != 0) {
        return null;
      }

      final MemorySegment dataPtr = outDataPtr.get(ValueLayout.ADDRESS, 0);
      final long dataLen = outLenPtr.get(ValueLayout.JAVA_LONG, 0);

      if (dataPtr.equals(MemorySegment.NULL) || dataLen <= 0) {
        return null;
      }

      final MemorySegment hashData = dataPtr.reinterpret(dataLen);
      return hashData.toArray(ValueLayout.JAVA_BYTE);
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
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Failed to precompile module (error code: " + result + ")");
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
   * Gets custom sections from a module as JSON.
   *
   * <p>Returns a JSON string mapping section names to Base64-encoded data.
   *
   * @param modulePtr pointer to the module
   * @return MemorySegment pointing to JSON string, or NULL on error
   */
  public MemorySegment moduleGetCustomSections(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction(
        "wasmtime4j_panama_module_get_custom_sections", MemorySegment.class, modulePtr);
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
   * Validates imports against a module.
   *
   * @param modulePtr pointer to the module
   * @param importsPtr pointer to imports array
   * @param importsCount number of imports
   * @return 0 on success, negative error code on failure
   */
  public int moduleValidateImports(
      final MemorySegment modulePtr, final MemorySegment importsPtr, final long importsCount) {
    validatePointer(modulePtr, "modulePtr");
    validatePointer(importsPtr, "importsPtr");
    return callNativeFunction(
        "wasmtime4j_module_validate_imports", Integer.class, modulePtr, importsPtr, importsCount);
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
  // Store Operations
  // ===========================================================================================

  /**
   * Creates a new Wasmtime store.
   *
   * @param enginePtr pointer to the engine
   * @return memory segment pointer to the store, or null on failure
   */
  public MemorySegment storeCreate(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");

    final MethodHandle mh = mhStoreCreate;
    if (mh != null) {
      try {
        return (MemorySegment) mh.invokeExact(enginePtr);
      } catch (Throwable t) {
        throw new RuntimeException("Native storeCreate failed", t);
      }
    }
    return callNativeFunction("wasmtime4j_store_create", MemorySegment.class, enginePtr);
  }

  /**
   * Creates a WebAssembly store that is compatible with a specific module.
   *
   * <p>CRITICAL: This ensures the Store's internal wasmtime::Store uses the SAME Engine Arc as the
   * Module's internal wasmtime::Module. This is required because wasmtime's Instance::new() uses
   * Arc::ptr_eq() to verify engine compatibility.
   *
   * @param modulePtr pointer to the module
   * @return memory segment pointer to the store, or null on failure
   */
  public MemorySegment storeCreateForModule(final MemorySegment modulePtr) {
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction("wasmtime4j_store_new_for_module", MemorySegment.class, modulePtr);
  }

  /**
   * Destroys a WebAssembly store.
   *
   * @param storePtr pointer to the store to destroy
   */
  public void storeDestroy(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    callNativeFunction("wasmtime4j_store_destroy", Void.class, storePtr);
  }

  /**
   * Creates a WebAssembly store with custom configuration.
   *
   * @param enginePtr pointer to the engine
   * @param fuelLimit the fuel limit (0 = no limit)
   * @param memoryLimitBytes the memory limit in bytes (0 = no limit)
   * @param executionTimeoutSecs the execution timeout in seconds (0 = no timeout)
   * @param maxInstances the maximum number of instances (0 = no limit)
   * @param maxTableElements the maximum table elements (0 = no limit)
   * @param maxFunctions the maximum functions (0 = no limit)
   * @param storePtr pointer to store the created store (output parameter)
   * @return 0 on success, negative error code on failure
   */
  public int storeCreateWithConfig(
      final MemorySegment enginePtr,
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSecs,
      final int maxInstances,
      final int maxTableElements,
      final int maxFunctions,
      final MemorySegment storePtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(storePtr, "storePtr");

    return callNativeFunction(
        "wasmtime4j_panama_store_create_with_config",
        Integer.class,
        enginePtr,
        fuelLimit,
        memoryLimitBytes,
        executionTimeoutSecs,
        maxInstances,
        maxTableElements,
        maxFunctions,
        storePtr);
  }

  /**
   * Sets fuel for a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param fuel the fuel amount to set
   * @return 0 on success, negative error code on failure
   */
  public int storeSetFuel(final MemorySegment storePtr, final long fuel) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_set_fuel", Integer.class, storePtr, fuel);
  }

  /**
   * Gets remaining fuel from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param fuelOutPtr pointer to store the remaining fuel
   * @return 0 on success, negative error code on failure
   */
  public int storeGetFuel(final MemorySegment storePtr, final MemorySegment fuelOutPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(fuelOutPtr, "fuelOutPtr");
    return callNativeFunction("wasmtime4j_store_get_fuel", Integer.class, storePtr, fuelOutPtr);
  }

  /**
   * Sets WASI context on a WebAssembly store.
   *
   * <p>This function attaches a WASI context to a store, which is required before instantiating
   * modules that import WASI functions.
   *
   * @param storePtr pointer to the store
   * @param wasiCtxPtr pointer to the WASI context
   * @return 0 on success, negative error code on failure
   */
  public int storeSetWasiContext(final MemorySegment storePtr, final MemorySegment wasiCtxPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(wasiCtxPtr, "wasiCtxPtr");
    return callNativeFunction(
        "wasmtime4j_store_set_wasi_context", Integer.class, storePtr, wasiCtxPtr);
  }

  /**
   * Checks if a store has WASI context attached.
   *
   * @param storePtr pointer to the store
   * @return 1 if store has WASI context, 0 if not, negative on error
   */
  public int storeHasWasiContext(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_has_wasi_context", Integer.class, storePtr);
  }

  /**
   * Adds fuel to a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param fuel the fuel amount to add
   * @return 0 on success, negative error code on failure
   */
  public int storeAddFuel(final MemorySegment storePtr, final long fuel) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_add_fuel", Integer.class, storePtr, fuel);
  }

  /**
   * Sets epoch deadline for a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param ticks the epoch deadline in ticks
   * @return 0 on success, negative error code on failure
   */
  public int storeSetEpochDeadline(final MemorySegment storePtr, final long ticks) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_store_set_epoch_deadline", Integer.class, storePtr, ticks);
  }

  /**
   * Gets execution statistics for a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param executionCountPtr pointer to store execution count
   * @param fuelConsumedPtr pointer to store fuel consumed
   * @param totalExecutionTimeNsPtr pointer to store total execution time in nanoseconds
   * @return 0 on success, negative error code on failure
   */
  public int storeGetExecutionStats(
      final MemorySegment storePtr,
      final MemorySegment executionCountPtr,
      final MemorySegment fuelConsumedPtr,
      final MemorySegment totalExecutionTimeNsPtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_store_get_execution_stats",
        Integer.class,
        storePtr,
        executionCountPtr,
        fuelConsumedPtr,
        totalExecutionTimeNsPtr);
  }

  /**
   * Gets memory usage statistics for a WebAssembly store.
   *
   * <p>Note: {@code totalBytesPtr} and {@code usedBytesPtr} are always written as 0 because
   * Wasmtime does not expose per-store memory aggregation. Only {@code instanceCountPtr} provides
   * meaningful data.
   *
   * @param storePtr pointer to the store
   * @param totalBytesPtr pointer to store total bytes (always 0)
   * @param usedBytesPtr pointer to store used bytes (always 0)
   * @param instanceCountPtr pointer to store instance count
   * @return 0 on success, negative error code on failure
   */
  public int storeGetMemoryUsage(
      final MemorySegment storePtr,
      final MemorySegment totalBytesPtr,
      final MemorySegment usedBytesPtr,
      final MemorySegment instanceCountPtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_store_get_memory_usage",
        Integer.class,
        storePtr,
        totalBytesPtr,
        usedBytesPtr,
        instanceCountPtr);
  }

  /**
   * Validates store functionality.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeValidate(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_validate", Integer.class, storePtr);
  }

  /**
   * Triggers garbage collection in a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeGarbageCollect(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_store_garbage_collect", Integer.class, storePtr);
  }

  /**
   * Captures backtrace from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param bufferOutPtr pointer to store the backtrace buffer pointer
   * @param bufferLenOutPtr pointer to store the buffer length
   * @return 0 on success, negative error code on failure
   */
  public int storeCaptureBacktrace(
      final MemorySegment storePtr,
      final MemorySegment bufferOutPtr,
      final MemorySegment bufferLenOutPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(bufferOutPtr, "bufferOutPtr");
    validatePointer(bufferLenOutPtr, "bufferLenOutPtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_capture_backtrace",
        Integer.class,
        storePtr,
        bufferOutPtr,
        bufferLenOutPtr);
  }

  /**
   * Force captures backtrace from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param bufferOutPtr pointer to store the backtrace buffer pointer
   * @param bufferLenOutPtr pointer to store the buffer length
   * @return 0 on success, negative error code on failure
   */
  public int storeForceCaptureBacktrace(
      final MemorySegment storePtr,
      final MemorySegment bufferOutPtr,
      final MemorySegment bufferLenOutPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(bufferOutPtr, "bufferOutPtr");
    validatePointer(bufferLenOutPtr, "bufferLenOutPtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_force_capture_backtrace",
        Integer.class,
        storePtr,
        bufferOutPtr,
        bufferLenOutPtr);
  }

  /**
   * Performs garbage collection on a WebAssembly store.
   *
   * <p>This triggers garbage collection to reclaim unreachable GC objects. If GC support is not
   * enabled in the engine configuration, this is a no-op.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeGc(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_store_gc", Integer.class, storePtr);
  }

  /**
   * Performs asynchronous garbage collection on a WebAssembly store.
   *
   * <p>This is the async version of {@link #storeGc(MemorySegment)} that cooperatively yields
   * during GC if the store is configured with epoch-based interruption.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeGcAsync(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_store_gc_async", Integer.class, storePtr);
  }

  /**
   * Configures the epoch deadline to async yield and update.
   *
   * <p>When the epoch deadline is reached, execution will yield back to the async executor and then
   * continue with a new deadline of current epoch + deltaTicks.
   *
   * @param storePtr pointer to the store
   * @param deltaTicks number of ticks to add for the new deadline
   * @return 0 on success, negative error code on failure
   */
  public int storeEpochDeadlineAsyncYieldAndUpdate(
      final MemorySegment storePtr, final long deltaTicks) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_epoch_deadline_async_yield_and_update",
        Integer.class,
        storePtr,
        deltaTicks);
  }

  /**
   * Configures the epoch deadline to trap when reached.
   *
   * <p>When the epoch deadline is reached, execution will trap immediately.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeEpochDeadlineTrap(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_epoch_deadline_trap", Integer.class, storePtr);
  }

  /**
   * Clears the epoch deadline callback from the store.
   *
   * @param storePtr pointer to the store
   * @return 0 on success, negative error code on failure
   */
  public int storeClearEpochDeadlineCallback(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_clear_epoch_deadline_callback", Integer.class, storePtr);
  }

  /**
   * Sets an epoch deadline callback with a function pointer on the store.
   *
   * <p>This allows the Java code to receive callbacks when the epoch deadline is reached and decide
   * whether to continue execution (by returning a positive delta) or trap (by returning a negative
   * value).
   *
   * @param storePtr pointer to the store
   * @param callbackFn function pointer for the epoch callback (takes callback_id, epoch; returns
   *     delta or negative to trap)
   * @param callbackId identifier passed to the callback to identify the Java callback
   * @return 0 on success, negative error code on failure
   */
  public int storeSetEpochDeadlineCallbackFn(
      final MemorySegment storePtr, final MemorySegment callbackFn, final long callbackId) {
    validatePointer(storePtr, "storePtr");
    validatePointer(callbackFn, "callbackFn");
    return callNativeFunction(
        "wasmtime4j_panama_store_set_epoch_deadline_callback_fn",
        Integer.class,
        storePtr,
        callbackFn,
        callbackId);
  }

  /**
   * Sets a dynamic resource limiter on the store with callback function pointers.
   *
   * <p>The callbacks are invoked by the Wasmtime runtime each time a memory or table needs to grow,
   * allowing dynamic per-request resource allocation decisions.
   *
   * @param storePtr pointer to the store
   * @param callbackId identifier passed to callbacks for Java-side dispatch
   * @param memoryGrowingFn function pointer for memory grow decisions
   * @param tableGrowingFn function pointer for table grow decisions
   * @param memoryGrowFailedFn function pointer for memory grow failure notifications (nullable)
   * @param tableGrowFailedFn function pointer for table grow failure notifications (nullable)
   * @return 0 on success, negative error code on failure
   */
  public int storeSetResourceLimiter(
      final MemorySegment storePtr,
      final long callbackId,
      final MemorySegment memoryGrowingFn,
      final MemorySegment tableGrowingFn,
      final MemorySegment memoryGrowFailedFn,
      final MemorySegment tableGrowFailedFn) {
    validatePointer(storePtr, "storePtr");
    validatePointer(memoryGrowingFn, "memoryGrowingFn");
    validatePointer(tableGrowingFn, "tableGrowingFn");
    return callNativeFunction(
        "wasmtime4j_panama_store_set_resource_limiter",
        Integer.class,
        storePtr,
        callbackId,
        memoryGrowingFn,
        tableGrowingFn,
        memoryGrowFailedFn,
        tableGrowFailedFn);
  }

  /**
   * Gets metadata information from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param metadataPtr pointer to store metadata
   * @param keyPtr pointer to metadata key
   * @param valuePtr pointer to store metadata value
   * @param sizePtr pointer to store size
   * @return 0 on success, negative error code on failure
   */
  public int storeGetMetadata(
      final MemorySegment storePtr,
      final MemorySegment metadataPtr,
      final MemorySegment keyPtr,
      final MemorySegment valuePtr,
      final MemorySegment sizePtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(metadataPtr, "metadataPtr");
    validatePointer(keyPtr, "keyPtr");
    validatePointer(valuePtr, "valuePtr");
    validatePointer(sizePtr, "sizePtr");
    return callNativeFunction(
        "wasmtime4j_store_get_metadata",
        Integer.class,
        storePtr,
        metadataPtr,
        keyPtr,
        valuePtr,
        sizePtr);
  }

  /**
   * Consumes fuel from a WebAssembly store.
   *
   * @param storePtr pointer to the store
   * @param fuel amount of fuel to consume
   * @param remainingPtr pointer to store remaining fuel
   * @return 0 on success, negative error code on failure
   */
  public int storeConsumeFuel(
      final MemorySegment storePtr, final long fuel, final MemorySegment remainingPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(remainingPtr, "remainingPtr");
    return callNativeFunction(
        "wasmtime4j_store_consume_fuel", Integer.class, storePtr, fuel, remainingPtr);
  }

  // ===== Store Exception Methods =====

  /**
   * Throws an exception in the store.
   *
   * @param storePtr the store pointer
   * @param exnRefPtr the exception reference pointer
   * @return 0 on success, -1 on error
   */
  public int storeThrowException(final MemorySegment storePtr, final MemorySegment exnRefPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(exnRefPtr, "exnRefPtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_throw_exception", Integer.class, storePtr, exnRefPtr);
  }

  /**
   * Takes and removes the pending exception from the store.
   *
   * @param storePtr the store pointer
   * @return the exception reference pointer, or NULL if no pending exception
   */
  public MemorySegment storeTakePendingException(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_take_pending_exception", MemorySegment.class, storePtr);
  }

  /**
   * Checks if the store has a pending exception.
   *
   * @param storePtr the store pointer
   * @return 1 if pending exception exists, 0 if not, -1 on error
   */
  public int storeHasPendingException(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_has_pending_exception", Integer.class, storePtr);
  }

  // ===== Store Call Hook Methods =====

  /**
   * Sets a call hook on the store.
   *
   * @param storePtr the store pointer
   * @return 0 on success, non-zero on error
   */
  public int storeSetCallHook(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_store_set_call_hook", Integer.class, storePtr);
  }

  /**
   * Clears the call hook from the store.
   *
   * @param storePtr the store pointer
   * @return 0 on success, non-zero on error
   */
  public int storeClearCallHook(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction("wasmtime4j_panama_store_clear_call_hook", Integer.class, storePtr);
  }

  /**
   * Sets an async call hook on the store.
   *
   * @param storePtr the store pointer
   * @return 0 on success, non-zero on error
   */
  public int storeSetCallHookAsync(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_set_call_hook_async", Integer.class, storePtr);
  }

  /**
   * Clears the async call hook from the store.
   *
   * @param storePtr the store pointer
   * @return 0 on success, non-zero on error
   */
  public int storeClearCallHookAsync(final MemorySegment storePtr) {
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_store_clear_call_hook_async", Integer.class, storePtr);
  }

  // ===== Store Fuel/Epoch MethodHandle Getters =====

  /**
   * Gets the method handle for setting fuel level in a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreSetFuel() {
    FunctionBinding binding = getFunctionBinding("wasmtime4j_panama_store_set_fuel");
    return binding != null ? binding.getMethodHandle().orElse(null) : null;
  }

  /**
   * Gets the method handle for getting remaining fuel from a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreGetFuel() {
    FunctionBinding binding = getFunctionBinding("wasmtime4j_panama_store_get_fuel");
    return binding != null ? binding.getMethodHandle().orElse(null) : null;
  }

  /**
   * Gets the method handle for adding fuel to a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreAddFuel() {
    FunctionBinding binding = getFunctionBinding("wasmtime4j_panama_store_add_fuel");
    return binding != null ? binding.getMethodHandle().orElse(null) : null;
  }

  /**
   * Gets the method handle for consuming fuel from a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreConsumeFuel() {
    FunctionBinding binding = getFunctionBinding("wasmtime4j_panama_store_consume_fuel");
    return binding != null ? binding.getMethodHandle().orElse(null) : null;
  }

  /**
   * Gets the method handle for setting epoch deadline in a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreSetEpochDeadline() {
    FunctionBinding binding = getFunctionBinding("wasmtime4j_panama_store_set_epoch_deadline");
    return binding != null ? binding.getMethodHandle().orElse(null) : null;
  }

  /**
   * Gets the method handle for getting execution statistics from a store.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaStoreGetExecutionStats() {
    FunctionBinding binding = getFunctionBinding("wasmtime4j_store_get_execution_stats");
    return binding != null ? binding.getMethodHandle().orElse(null) : null;
  }
}

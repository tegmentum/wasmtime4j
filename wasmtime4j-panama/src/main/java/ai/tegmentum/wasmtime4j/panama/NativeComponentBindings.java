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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Native function bindings for the WebAssembly Component Model.
 *
 * <p>Provides type-safe wrappers for all component model native functions including component
 * engine lifecycle, component loading and instantiation, component linker operations, enhanced
 * component engine with instance IDs, component metrics, and WIT value marshalling.
 *
 * <p>This class covers the following functional areas:
 *
 * <ul>
 *   <li>Component engine creation and destruction
 *   <li>Component loading from bytes and validation
 *   <li>Component instantiation and instance management
 *   <li>Component export/import introspection
 *   <li>Component linker with WASI Preview 2 and HTTP support
 *   <li>Enhanced component engine with instance ID tracking
 *   <li>Component metrics collection and querying
 *   <li>WIT value serialization and deserialization
 *   <li>Component orchestrator and resource management
 * </ul>
 */
public final class NativeComponentBindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeComponentBindings.class.getName());

  /** Initialization-on-demand holder for thread-safe lazy singleton. */
  private static final class Holder {
    static final NativeComponentBindings INSTANCE = new NativeComponentBindings();
  }

  private NativeComponentBindings() {
    super();
    initializeBindings();
    markInitialized();
    LOGGER.fine("Initialized NativeComponentBindings successfully");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeComponentBindings getInstance() {
    return Holder.INSTANCE;
  }

  private void initializeBindings() {
    addFunctionBinding(
        "wasmtime4j_component_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // component_ptr

    // ===== Component Model (structured API with error codes) =====

    addFunctionBinding(
        "wasmtime4j_component_exports_interface",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_imports_interface",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_export_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_import_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_get_export_name",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_get_import_name",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_free_string", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // ===== Full Component Type JSON =====
    addFunctionBinding(
        "wasmtime4j_component_get_full_type_json",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // component_ptr
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // json_out

    addFunctionBinding(
        "wasmtime4j_component_linker_substituted_type_json",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // component_ptr
            ValueLayout.ADDRESS)); // json_out

    // ===== Panama Component Functions =====
    addFunctionBinding(
        "wasmtime4j_panama_component_free_string_array",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // strings pointer
            ValueLayout.JAVA_INT)); // count

    // ===== Enhanced Component Engine (using instance IDs) =====
    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_engine_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS)); // returns engine pointer

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_instantiate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.ADDRESS, // component handle
            ValueLayout.ADDRESS)); // instance ID out (pointer to u64)

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_invoke",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.JAVA_LONG, // instance ID
            ValueLayout.ADDRESS, // function name
            ValueLayout.ADDRESS, // params pointer
            ValueLayout.JAVA_INT, // params count
            ValueLayout.ADDRESS, // results out
            ValueLayout.ADDRESS)); // results count out

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_get_exports",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.JAVA_LONG, // instance ID
            ValueLayout.ADDRESS, // functions out (pointer to pointer array)
            ValueLayout.ADDRESS)); // count out

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_engine_is_async",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_engine_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // engine handle

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_load_from_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.ADDRESS, // wasm bytes
            ValueLayout.JAVA_LONG, // wasm size
            ValueLayout.ADDRESS)); // component out (pointer to pointer)

    addFunctionBinding(
        "wasmtime4j_panama_component_compile_wat",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.ADDRESS, // WAT text bytes
            ValueLayout.JAVA_LONG, // WAT text length
            ValueLayout.ADDRESS)); // component out (pointer to pointer)

    addFunctionBinding(
        "wasmtime4j_panama_component_validate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // 1=valid, 0=invalid, -1=error
            ValueLayout.ADDRESS, // component pointer
            ValueLayout.ADDRESS)); // WIT interface C string

    // ===== Enhanced Component Instance Methods =====
    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_instance_has_func",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 1=found, 0=not found, -1=error
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.JAVA_LONG, // instance id
            ValueLayout.ADDRESS)); // function name (C string)

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_instance_get_module",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.JAVA_LONG, // instance id
            ValueLayout.ADDRESS, // module name (C string)
            ValueLayout.ADDRESS)); // module out (pointer to pointer)

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_instance_has_resource",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 1=found, 0=not found, -1=error
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.JAVA_LONG, // instance id
            ValueLayout.ADDRESS)); // resource name (C string)

    // ===== WIT Value Marshalling =====
    addFunctionBinding(
        "wasmtime4j_wit_value_serialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.JAVA_INT, // type_discriminator
            ValueLayout.ADDRESS, // value_ptr
            ValueLayout.JAVA_LONG, // value_len
            ValueLayout.ADDRESS, // out_data
            ValueLayout.ADDRESS)); // out_len

    addFunctionBinding(
        "wasmtime4j_wit_value_free_buffer",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // buffer_ptr
            ValueLayout.JAVA_LONG)); // buffer_len

    addFunctionBinding(
        "wasmtime4j_wit_value_validate_discriminator",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return 1 if valid, 0 if invalid
            ValueLayout.JAVA_INT)); // type_discriminator

    addFunctionBinding(
        "wasmtime4j_wit_value_deserialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.JAVA_INT, // type_discriminator
            ValueLayout.ADDRESS, // data_ptr
            ValueLayout.JAVA_LONG, // data_len
            ValueLayout.ADDRESS, // out_value
            ValueLayout.ADDRESS)); // out_len

    // ===== Component Linker =====
    addFunctionBinding(
        "wasmtime4j_component_linker_new_with_engine",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns linker pointer
            ValueLayout.ADDRESS)); // wasmtime engine pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_is_valid",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 1 if valid, 0 if invalid
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_enable_wasi_p2",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_enable_wasi_http",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_enable_wasi_http_with_config",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_LONG)); // field_size_limit

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_args",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS)); // args JSON string

    addFunctionBinding(
        "wasmtime4j_component_linker_add_wasi_env",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // key string
            ValueLayout.ADDRESS)); // value string

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_inherit_env",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // inherit flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_inherit_args",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // inherit flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_inherit_stdio",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // inherit flag

    addFunctionBinding(
        "wasmtime4j_component_linker_add_wasi_preopen_dir",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // host path string
            ValueLayout.ADDRESS, // guest path string
            ValueLayout.JAVA_INT, // dir perms bits
            ValueLayout.JAVA_INT)); // file perms bits

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_allow_network",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // allow flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_inherit_stdin",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // inherit flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_inherit_stdout",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // inherit flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_inherit_stderr",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // inherit flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_allow_tcp",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // allow flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_allow_udp",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // allow flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_allow_ip_name_lookup",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // allow flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_allow_blocking_current_thread",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // allow flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_insecure_random_seed",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_LONG)); // seed value

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_wall_clock",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // now_fn function pointer
            ValueLayout.ADDRESS, // resolution_fn function pointer
            ValueLayout.JAVA_LONG)); // callback_id

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_monotonic_clock",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // now_fn function pointer
            ValueLayout.ADDRESS, // resolution_fn function pointer
            ValueLayout.JAVA_LONG)); // callback_id

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_secure_random",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // fill_bytes_fn function pointer
            ValueLayout.JAVA_LONG)); // callback_id

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_insecure_random",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // fill_bytes_fn function pointer
            ValueLayout.JAVA_LONG)); // callback_id

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_socket_addr_check",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // check_fn function pointer
            ValueLayout.JAVA_LONG)); // callback_id

    addFunctionBinding(
        "wasmtime4j_component_linker_instantiate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // component pointer
            ValueLayout.ADDRESS)); // instance out pointer

    // ===== Resource Definition =====
    addFunctionBinding(
        "wasmtime4j_component_linker_define_resource",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // interface_path_ptr (UTF-8)
            ValueLayout.JAVA_LONG, // interface_path_len
            ValueLayout.ADDRESS, // resource_name_ptr (UTF-8)
            ValueLayout.JAVA_LONG, // resource_name_len
            ValueLayout.JAVA_INT, // resource_id
            ValueLayout.ADDRESS, // destructor_fn (function pointer, nullable)
            ValueLayout.JAVA_LONG)); // destructor_callback_id

    // ===== Module Definition =====
    addFunctionBinding(
        "wasmtime4j_component_linker_define_module",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // instance_path_ptr (UTF-8)
            ValueLayout.JAVA_LONG, // instance_path_len
            ValueLayout.ADDRESS, // name_ptr (UTF-8)
            ValueLayout.JAVA_LONG, // name_len
            ValueLayout.ADDRESS)); // module_ptr

    // ===== Host Function Definition =====
    addFunctionBinding(
        "wasmtime4j_component_linker_define_host_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // wit_path_ptr (UTF-8)
            ValueLayout.JAVA_LONG, // wit_path_len
            ValueLayout.ADDRESS, // callback_fn (function pointer)
            ValueLayout.JAVA_LONG)); // callback_id

    addFunctionBinding(
        "wasmtime4j_component_linker_define_host_function_async",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // wit_path_ptr (UTF-8)
            ValueLayout.JAVA_LONG, // wit_path_len
            ValueLayout.ADDRESS, // callback_fn (function pointer)
            ValueLayout.JAVA_LONG)); // callback_id

    addFunctionBinding(
        "wasmtime4j_component_host_callback_alloc_result",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // allocated pointer
            ValueLayout.JAVA_LONG)); // length

    // ===== allow_shadowing / define_unknown_imports_as_traps =====
    addFunctionBinding(
        "wasmtime4j_component_linker_allow_shadowing",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // allow flag

    addFunctionBinding(
        "wasmtime4j_component_linker_define_unknown_imports_as_traps",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS)); // component pointer

    // ===== Linker Query/Config Functions =====
    addFunctionBinding(
        "wasmtime4j_component_linker_get_interfaces",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS)); // json_out

    addFunctionBinding(
        "wasmtime4j_component_linker_get_functions",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.ADDRESS, // namespace
            ValueLayout.ADDRESS, // interface_name
            ValueLayout.ADDRESS)); // json_out

    addFunctionBinding(
        "wasmtime4j_component_linker_host_function_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // returns usize
            ValueLayout.ADDRESS)); // linker_ptr

    addFunctionBinding(
        "wasmtime4j_component_linker_interface_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // returns usize
            ValueLayout.ADDRESS)); // linker_ptr

    addFunctionBinding(
        "wasmtime4j_component_linker_wasi_p2_enabled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 0/1/FFI_ERROR
            ValueLayout.ADDRESS)); // linker_ptr

    addFunctionBinding(
        "wasmtime4j_component_linker_wasi_http_enabled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 0/1/FFI_ERROR
            ValueLayout.ADDRESS)); // linker_ptr

    addFunctionBinding(
        "wasmtime4j_component_linker_set_async_support",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.JAVA_INT)); // enabled flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_max_random_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // linker_ptr
            ValueLayout.JAVA_LONG)); // max_size

    // ===== ComponentInstancePre =====
    addFunctionBinding(
        "wasmtime4j_component_linker_instantiate_pre",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns pre-instantiated handle
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS)); // component pointer

    addFunctionBinding(
        "wasmtime4j_component_instance_pre_instantiate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // pre handle
            ValueLayout.ADDRESS)); // instance out pointer

    addFunctionBinding(
        "wasmtime4j_component_instance_pre_instantiate_with_config",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // pre handle
            ValueLayout.JAVA_LONG, // fuel limit
            ValueLayout.JAVA_LONG, // epoch deadline
            ValueLayout.JAVA_LONG, // max memory bytes
            ValueLayout.JAVA_LONG, // max table elements
            ValueLayout.JAVA_LONG, // max instances
            ValueLayout.JAVA_LONG, // max tables
            ValueLayout.JAVA_LONG, // max memories
            ValueLayout.JAVA_BYTE, // trap on grow failure
            ValueLayout.ADDRESS)); // instance out pointer

    addFunctionBinding(
        "wasmtime4j_component_instance_pre_is_valid",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 1 if valid, 0 if not
            ValueLayout.ADDRESS)); // pre handle

    addFunctionBinding(
        "wasmtime4j_component_instance_pre_instance_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // returns instance count
            ValueLayout.ADDRESS)); // pre handle

    addFunctionBinding(
        "wasmtime4j_component_instance_pre_preparation_time_ns",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // returns preparation time in nanoseconds
            ValueLayout.ADDRESS)); // pre handle

    addFunctionBinding(
        "wasmtime4j_component_instance_pre_avg_instantiation_time_ns",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // returns avg instantiation time in nanoseconds
            ValueLayout.ADDRESS)); // pre handle

    addFunctionBinding(
        "wasmtime4j_component_instance_pre_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // pre handle

    // ===== Component Serialize/Deserialize =====
    addFunctionBinding(
        "wasmtime4j_component_serialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // component_ptr
            ValueLayout.ADDRESS, // data_ptr out
            ValueLayout.ADDRESS)); // len out

    addFunctionBinding(
        "wasmtime4j_component_free_serialized_data",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // data_ptr
            ValueLayout.JAVA_LONG)); // len

    addFunctionBinding(
        "wasmtime4j_panama_component_deserialize",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine_ptr (enhanced component engine)
            ValueLayout.ADDRESS, // data_ptr
            ValueLayout.JAVA_LONG, // len
            ValueLayout.ADDRESS)); // component_ptr_out

    addFunctionBinding(
        "wasmtime4j_panama_component_deserialize_file",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // engine_ptr (enhanced component engine)
            ValueLayout.ADDRESS, // path_ptr
            ValueLayout.JAVA_LONG, // path_len
            ValueLayout.ADDRESS)); // component_ptr_out

    addFunctionBinding(
        "wasmtime4j_panama_component_resources_required",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // component_ptr
            ValueLayout.ADDRESS, // num_memories_out (i32 ptr)
            ValueLayout.ADDRESS, // max_memory_out (i64 ptr)
            ValueLayout.ADDRESS, // num_tables_out (i32 ptr)
            ValueLayout.ADDRESS)); // max_table_out (i64 ptr)

    // ===== ComponentExportIndex =====
    addFunctionBinding(
        "wasmtime4j_panama_component_get_export_index",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code (0=found, 1=not found, -1=error)
            ValueLayout.ADDRESS, // component_ptr
            ValueLayout.ADDRESS, // instance_index_ptr (nullable)
            ValueLayout.ADDRESS, // name (C string)
            ValueLayout.ADDRESS)); // index_out (pointer to pointer)

    addFunctionBinding(
        "wasmtime4j_panama_component_export_index_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // index_ptr

    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_instance_has_func_by_index",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 1=found, 0=not found, -1=error
            ValueLayout.ADDRESS, // engine handle
            ValueLayout.JAVA_LONG, // instance id
            ValueLayout.ADDRESS)); // index_ptr

    // ===== Concurrent Call Support =====
    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_run_concurrent",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 0=success, -1=error
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_LONG, // instance_id
            ValueLayout.ADDRESS, // json_ptr (input bytes)
            ValueLayout.JAVA_LONG, // json_len
            ValueLayout.ADDRESS, // result_ptr (out: *mut *mut u8)
            ValueLayout.ADDRESS)); // result_len (out: *mut usize)

    addFunctionBinding(
        "wasmtime4j_panama_free_concurrent_result",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // ptr
            ValueLayout.JAVA_LONG)); // len

    // ===== Async Val Lifecycle =====
    addFunctionBinding(
        "wasmtime4j_panama_async_val_close",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 0=success, -1=not found
            ValueLayout.JAVA_LONG)); // handle

    // ===== ResourceAny Lifecycle =====
    addFunctionBinding(
        "wasmtime4j_panama_resource_any_drop",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 0=success, non-zero=error
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.JAVA_LONG, // instance_id
            ValueLayout.JAVA_LONG)); // resource_handle
  }

  /**
   * Destroys a component.
   *
   * @param componentPtr pointer to the component to destroy
   */
  public void destroyComponent(final MemorySegment componentPtr) {
    validatePointer(componentPtr, "componentPtr");
    callNativeFunction("wasmtime4j_component_destroy", Void.class, componentPtr);
  }

  // ===== Component Model (structured API) =====

  /**
   * Gets the size of a component.
   *
   * @param componentHandle the component handle
   * @return the component size in bytes
   */
  public long componentGetSize(final MemorySegment componentHandle) {
    validatePointer(componentHandle, "componentHandle");
    return callNativeFunction("wasmtime4j_component_size_bytes", Long.class, componentHandle);
  }

  /**
   * Checks if a component exports an interface.
   *
   * @param componentHandle the component handle
   * @param interfaceName the interface name (C string)
   * @return 1 if exports the interface, 0 otherwise
   */
  public int componentExportsInterface(
      final MemorySegment componentHandle, final MemorySegment interfaceName) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(interfaceName, "interfaceName");
    return callNativeFunction(
        "wasmtime4j_component_exports_interface", Integer.class, componentHandle, interfaceName);
  }

  /**
   * Checks if a component imports an interface.
   *
   * @param componentHandle the component handle
   * @param interfaceName the interface name (C string)
   * @return 1 if imports the interface, 0 otherwise
   */
  public int componentImportsInterface(
      final MemorySegment componentHandle, final MemorySegment interfaceName) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(interfaceName, "interfaceName");
    return callNativeFunction(
        "wasmtime4j_component_imports_interface", Integer.class, componentHandle, interfaceName);
  }

  /**
   * Gets the count of exports in a component.
   *
   * @param componentHandle the component handle
   * @return number of exports
   */
  public long componentExportCount(final MemorySegment componentHandle) {
    validatePointer(componentHandle, "componentHandle");
    return callNativeFunction("wasmtime4j_component_export_count", Long.class, componentHandle);
  }

  /**
   * Gets the count of imports in a component.
   *
   * @param componentHandle the component handle
   * @return number of imports
   */
  public long componentImportCount(final MemorySegment componentHandle) {
    validatePointer(componentHandle, "componentHandle");
    return callNativeFunction("wasmtime4j_component_import_count", Long.class, componentHandle);
  }

  /**
   * Gets an export interface name by index.
   *
   * @param componentHandle the component handle
   * @param index the export index
   * @param nameOut pointer to receive the name string
   * @return 0 on success, non-zero on error
   */
  public int componentGetExportName(
      final MemorySegment componentHandle, final long index, final MemorySegment nameOut) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(nameOut, "nameOut");
    return callNativeFunction(
        "wasmtime4j_component_get_export_name", Integer.class, componentHandle, index, nameOut);
  }

  /**
   * Gets an import interface name by index.
   *
   * @param componentHandle the component handle
   * @param index the import index
   * @param nameOut pointer to receive the name string
   * @return 0 on success, non-zero on error
   */
  public int componentGetImportName(
      final MemorySegment componentHandle, final long index, final MemorySegment nameOut) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(nameOut, "nameOut");
    return callNativeFunction(
        "wasmtime4j_component_get_import_name", Integer.class, componentHandle, index, nameOut);
  }

  /**
   * Frees a string returned by component functions.
   *
   * @param stringPtr the string pointer to free
   */
  public void componentFreeString(final MemorySegment stringPtr) {
    if (stringPtr != null && !stringPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_component_free_string", Void.class, stringPtr);
    }
  }

  /**
   * Gets the full component type as a JSON string with complete type information.
   *
   * @param componentHandle the component handle
   * @param engineHandle the engine handle
   * @param jsonOut pointer to receive the JSON string
   * @return 0 on success, non-zero on error
   */
  public int componentGetFullTypeJson(
      final MemorySegment componentHandle,
      final MemorySegment engineHandle,
      final MemorySegment jsonOut) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(engineHandle, "engineHandle");
    validatePointer(jsonOut, "jsonOut");
    return callNativeFunction(
        "wasmtime4j_component_get_full_type_json",
        Integer.class,
        componentHandle,
        engineHandle,
        jsonOut);
  }

  /**
   * Gets the substituted component type as a JSON string.
   *
   * @param linkerHandle the linker handle
   * @param componentHandle the component handle
   * @param jsonOut pointer to receive the JSON string
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSubstitutedTypeJson(
      final MemorySegment linkerHandle,
      final MemorySegment componentHandle,
      final MemorySegment jsonOut) {
    validatePointer(linkerHandle, "linkerHandle");
    validatePointer(componentHandle, "componentHandle");
    validatePointer(jsonOut, "jsonOut");
    return callNativeFunction(
        "wasmtime4j_component_linker_substituted_type_json",
        Integer.class,
        linkerHandle,
        componentHandle,
        jsonOut);
  }

  /**
   * Frees an array of C strings returned by component functions.
   *
   * @param stringsPtr pointer to array of string pointers
   * @param count number of strings in the array
   */
  public void componentFreeStringArray(final MemorySegment stringsPtr, final int count) {
    if (stringsPtr != null && !stringsPtr.equals(MemorySegment.NULL)) {
      callNativeFunction(
          "wasmtime4j_panama_component_free_string_array", Void.class, stringsPtr, count);
    }
  }

  // ===== Enhanced Component Engine (using instance IDs) =====

  /**
   * Creates an enhanced component engine for managing component instances.
   *
   * @return pointer to the enhanced component engine, or NULL on failure
   */
  public MemorySegment enhancedComponentEngineCreate() {
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_engine_create", MemorySegment.class);
  }

  /**
   * Loads (compiles) a component from WebAssembly bytes.
   *
   * @param engineHandle the enhanced component engine handle
   * @param wasmBytes the WebAssembly component bytes
   * @param wasmSize the size of the WebAssembly bytes
   * @param componentOut output parameter for the component handle
   * @return error code (0 for success)
   */
  public int enhancedComponentLoadFromBytes(
      final MemorySegment engineHandle,
      final MemorySegment wasmBytes,
      final long wasmSize,
      final MemorySegment componentOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(wasmBytes, "wasmBytes");
    validatePointer(componentOut, "componentOut");
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_load_from_bytes",
        Integer.class,
        engineHandle,
        wasmBytes,
        wasmSize,
        componentOut);
  }

  /**
   * Compiles a component from WAT text.
   *
   * @param engineHandle the enhanced component engine handle
   * @param watBytes pointer to WAT text bytes
   * @param watLen length of WAT text bytes
   * @param componentOut output parameter for the component pointer
   * @return 0 on success, non-zero error code on failure
   */
  public int componentCompileWat(
      final MemorySegment engineHandle,
      final MemorySegment watBytes,
      final long watLen,
      final MemorySegment componentOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(watBytes, "watBytes");
    validatePointer(componentOut, "componentOut");
    return callNativeFunction(
        "wasmtime4j_panama_component_compile_wat",
        Integer.class,
        engineHandle,
        watBytes,
        watLen,
        componentOut);
  }

  /**
   * Validates a component against a WIT interface.
   *
   * @param componentPtr pointer to the component
   * @param witInterface C string pointer to the WIT interface text
   * @return 1 if valid, 0 if invalid, negative on error
   */
  public int componentValidate(final MemorySegment componentPtr, final MemorySegment witInterface) {
    validatePointer(componentPtr, "componentPtr");
    validatePointer(witInterface, "witInterface");
    return callNativeFunction(
        "wasmtime4j_panama_component_validate", Integer.class, componentPtr, witInterface);
  }

  /**
   * Instantiates a component and returns an instance ID.
   *
   * @param engineHandle the enhanced component engine handle
   * @param componentHandle the component handle
   * @param instanceIdOut output parameter for the instance ID (u64)
   * @return 0 on success, non-zero error code on failure
   */
  public int enhancedComponentInstantiate(
      final MemorySegment engineHandle,
      final MemorySegment componentHandle,
      final MemorySegment instanceIdOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(componentHandle, "componentHandle");
    validatePointer(instanceIdOut, "instanceIdOut");
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_instantiate",
        Integer.class,
        engineHandle,
        componentHandle,
        instanceIdOut);
  }

  /**
   * Invokes a component function using instance ID.
   *
   * @param engineHandle the enhanced component engine handle
   * @param instanceId the instance ID returned from instantiation
   * @param functionName the function name to invoke
   * @param paramsPtr pointer to the parameters array
   * @param paramsCount number of parameters
   * @param resultsOut output parameter for results array
   * @param resultsCountOut output parameter for results count
   * @return 0 on success, non-zero error code on failure
   */
  public int enhancedComponentInvoke(
      final MemorySegment engineHandle,
      final long instanceId,
      final MemorySegment functionName,
      final MemorySegment paramsPtr,
      final int paramsCount,
      final MemorySegment resultsOut,
      final MemorySegment resultsCountOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(functionName, "functionName");
    validatePointer(resultsOut, "resultsOut");
    validatePointer(resultsCountOut, "resultsCountOut");
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_invoke",
        Integer.class,
        engineHandle,
        instanceId,
        functionName,
        paramsPtr,
        paramsCount,
        resultsOut,
        resultsCountOut);
  }

  /**
   * Gets the list of exported function names using instance ID.
   *
   * @param engineHandle the enhanced component engine handle
   * @param instanceId the instance ID
   * @param functionsOut output parameter for array of function name pointers
   * @param countOut output parameter for count of functions
   * @return 0 on success, non-zero error code on failure
   */
  public int enhancedComponentGetExports(
      final MemorySegment engineHandle,
      final long instanceId,
      final MemorySegment functionsOut,
      final MemorySegment countOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(functionsOut, "functionsOut");
    validatePointer(countOut, "countOut");
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_get_exports",
        Integer.class,
        engineHandle,
        instanceId,
        functionsOut,
        countOut);
  }

  /**
   * Checks if async support is enabled for an enhanced component engine.
   *
   * @param enginePtr pointer to the enhanced component engine
   * @return true if async support is enabled
   */
  public boolean enhancedComponentEngineIsAsync(final MemorySegment enginePtr) {
    final int result =
        callNativeFunction(
            "wasmtime4j_panama_enhanced_component_engine_is_async", Integer.class, enginePtr);
    return result == 1;
  }

  /**
   * Checks if a component instance has a specific function export.
   *
   * @param enginePtr pointer to the enhanced component engine
   * @param instanceId the component instance ID
   * @param functionName C string with the function name
   * @return 1 if found, 0 if not found, -1 on error
   */
  public int enhancedComponentInstanceHasFunc(
      final MemorySegment enginePtr, final long instanceId, final MemorySegment functionName) {
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_instance_has_func",
        Integer.class,
        enginePtr,
        instanceId,
        functionName);
  }

  /**
   * Looks up a core module exported by a component instance.
   *
   * @param enginePtr pointer to the enhanced component engine
   * @param instanceId the component instance ID
   * @param moduleName C string with the module name
   * @param moduleOut output pointer for the module handle
   * @return 0 on success, non-zero on error
   */
  public int enhancedComponentInstanceGetModule(
      final MemorySegment enginePtr,
      final long instanceId,
      final MemorySegment moduleName,
      final MemorySegment moduleOut) {
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_instance_get_module",
        Integer.class,
        enginePtr,
        instanceId,
        moduleName,
        moduleOut);
  }

  /**
   * Checks if a resource type is exported by a component instance.
   *
   * @param enginePtr pointer to the enhanced component engine
   * @param instanceId the component instance ID
   * @param resourceName C string with the resource name
   * @return 1 if found, 0 if not found, -1 on error
   */
  public int enhancedComponentInstanceHasResource(
      final MemorySegment enginePtr, final long instanceId, final MemorySegment resourceName) {
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_instance_has_resource",
        Integer.class,
        enginePtr,
        instanceId,
        resourceName);
  }

  /**
   * Destroys an enhanced component engine and all its instances.
   *
   * @param engineHandle the enhanced component engine handle
   */
  public void enhancedComponentEngineDestroy(final MemorySegment engineHandle) {
    if (engineHandle != null && !engineHandle.equals(MemorySegment.NULL)) {
      callNativeFunction(
          "wasmtime4j_panama_enhanced_component_engine_destroy", Void.class, engineHandle);
    }
  }

  // ===== WIT Value Marshalling =====

  /**
   * Serializes a WIT value to binary format.
   *
   * @param typeDiscriminator the type discriminator (1-6)
   * @param valuePtr pointer to the value data
   * @param valueLen length of the value data
   * @param outData output buffer for serialized data
   * @param outLen output parameter for data length
   * @return 0 on success, non-zero on error
   */
  public int witValueSerialize(
      final int typeDiscriminator,
      final MemorySegment valuePtr,
      final long valueLen,
      final MemorySegment outData,
      final MemorySegment outLen) {
    validatePointer(valuePtr, "valuePtr");
    validatePointer(outData, "outData");
    validatePointer(outLen, "outLen");
    return callNativeFunction(
        "wasmtime4j_wit_value_serialize",
        Integer.class,
        typeDiscriminator,
        valuePtr,
        valueLen,
        outData,
        outLen);
  }

  /**
   * Deserializes a WIT value from binary format.
   *
   * @param typeDiscriminator the type discriminator (1-6)
   * @param dataPtr pointer to the serialized data
   * @param dataLen length of the data
   * @param outValue output buffer for deserialized value
   * @param outLen output parameter for value length
   * @return 0 on success, non-zero on error
   */
  public int witValueDeserialize(
      final int typeDiscriminator,
      final MemorySegment dataPtr,
      final long dataLen,
      final MemorySegment outValue,
      final MemorySegment outLen) {
    validatePointer(dataPtr, "dataPtr");
    validatePointer(outValue, "outValue");
    validatePointer(outLen, "outLen");
    return callNativeFunction(
        "wasmtime4j_wit_value_deserialize",
        Integer.class,
        typeDiscriminator,
        dataPtr,
        dataLen,
        outValue,
        outLen);
  }

  /**
   * Validates a type discriminator.
   *
   * @param typeDiscriminator the type discriminator to validate
   * @return true if valid (1), false otherwise (0)
   */
  public boolean witValueValidateDiscriminator(final int typeDiscriminator) {
    final int result =
        callNativeFunction(
            "wasmtime4j_wit_value_validate_discriminator", Integer.class, typeDiscriminator);
    return result == 1;
  }

  /**
   * Frees a buffer allocated by WIT value marshalling functions.
   *
   * @param ptr pointer to the buffer to free
   * @param len length of the buffer
   */
  public void witValueFreeBuffer(final MemorySegment ptr, final long len) {
    if (ptr != null && !ptr.equals(MemorySegment.NULL) && len > 0) {
      callNativeFunction("wasmtime4j_wit_value_free_buffer", Void.class, ptr, len);
    }
  }

  // ===== Component Linker =====

  /**
   * Creates a new component linker from a Wasmtime engine.
   *
   * @param enginePtr pointer to the Wasmtime engine
   * @return pointer to the new component linker, or null on failure
   */
  public MemorySegment componentLinkerCreateWithEngine(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_new_with_engine", MemorySegment.class, enginePtr);
  }

  /**
   * Destroys a component linker.
   *
   * @param linkerPtr pointer to the component linker to destroy
   */
  public void componentLinkerDestroy(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    callNativeFunction("wasmtime4j_component_linker_destroy", Void.class, linkerPtr);
  }

  /**
   * Checks if a component linker is valid.
   *
   * @param linkerPtr pointer to the component linker
   * @return 1 if valid, 0 if invalid
   */
  public int componentLinkerIsValid(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction("wasmtime4j_component_linker_is_valid", Integer.class, linkerPtr);
  }

  /**
   * Enables WASI Preview 2 in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerEnableWasiP2(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_enable_wasi_p2", Integer.class, linkerPtr);
  }

  /**
   * Enables WASI HTTP in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerEnableWasiHttp(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_enable_wasi_http", Integer.class, linkerPtr);
  }

  /**
   * Enables WASI HTTP with configuration in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param fieldSizeLimit maximum HTTP header field size (0 for default)
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerEnableWasiHttpWithConfig(
      final MemorySegment linkerPtr, final long fieldSizeLimit) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_enable_wasi_http_with_config",
        Integer.class,
        linkerPtr,
        fieldSizeLimit);
  }

  /**
   * Enables WASI Config in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerEnableWasiConfig(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_enable_wasi_config", Integer.class, linkerPtr);
  }

  /**
   * Sets configuration variables for WASI Config.
   *
   * <p>Allocates native memory for the key-value string arrays, calls the native function, and
   * releases memory when done.
   *
   * @param linkerPtr pointer to the component linker
   * @param variables the configuration variables as key-value pairs
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetConfigVariables(
      final MemorySegment linkerPtr, final java.util.Map<String, String> variables) {
    validatePointer(linkerPtr, "linkerPtr");

    if (variables.isEmpty()) {
      return 0;
    }

    try (final java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
      final int count = variables.size();

      // Allocate arrays of pointers for keys and values
      final MemorySegment keysArray = arena.allocate(ValueLayout.ADDRESS, count);
      final MemorySegment valuesArray = arena.allocate(ValueLayout.ADDRESS, count);

      int i = 0;
      for (final java.util.Map.Entry<String, String> entry : variables.entrySet()) {
        final MemorySegment keyStr = arena.allocateFrom(entry.getKey());
        final MemorySegment valStr = arena.allocateFrom(entry.getValue());
        keysArray.setAtIndex(ValueLayout.ADDRESS, i, keyStr);
        valuesArray.setAtIndex(ValueLayout.ADDRESS, i, valStr);
        i++;
      }

      return callNativeFunction(
          "wasmtime4j_component_linker_set_config_variables",
          Integer.class,
          linkerPtr,
          keysArray,
          valuesArray,
          count);
    }
  }

  /**
   * Gets all defined interfaces from the native linker as a JSON array string.
   *
   * @param linkerPtr pointer to the component linker
   * @param jsonOut pointer to receive the JSON string (must be freed with componentFreeString)
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerGetInterfaces(
      final MemorySegment linkerPtr, final MemorySegment jsonOut) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(jsonOut, "jsonOut");
    return callNativeFunction(
        "wasmtime4j_component_linker_get_interfaces", Integer.class, linkerPtr, jsonOut);
  }

  /**
   * Gets all defined functions for a specific interface as a JSON array string.
   *
   * @param linkerPtr pointer to the component linker
   * @param namespace pointer to the interface namespace C string
   * @param interfaceName pointer to the interface name C string
   * @param jsonOut pointer to receive the JSON string (must be freed with componentFreeString)
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerGetFunctions(
      final MemorySegment linkerPtr,
      final MemorySegment namespace,
      final MemorySegment interfaceName,
      final MemorySegment jsonOut) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(namespace, "namespace");
    validatePointer(interfaceName, "interfaceName");
    validatePointer(jsonOut, "jsonOut");
    return callNativeFunction(
        "wasmtime4j_component_linker_get_functions",
        Integer.class,
        linkerPtr,
        namespace,
        interfaceName,
        jsonOut);
  }

  /**
   * Gets the number of host functions defined in the linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return the host function count
   */
  public long componentLinkerHostFunctionCount(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_host_function_count", Long.class, linkerPtr);
  }

  /**
   * Gets the number of interfaces defined in the linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return the interface count
   */
  public long componentLinkerInterfaceCount(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction("wasmtime4j_component_linker_interface_count", Long.class, linkerPtr);
  }

  /**
   * Checks if WASI Preview 2 is enabled in the linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return 1 if enabled, 0 if not, FFI_ERROR on error
   */
  public int componentLinkerWasiP2Enabled(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_wasi_p2_enabled", Integer.class, linkerPtr);
  }

  /**
   * Checks if WASI HTTP is enabled in the linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return 1 if enabled, 0 if not, FFI_ERROR on error
   */
  public int componentLinkerWasiHttpEnabled(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_wasi_http_enabled", Integer.class, linkerPtr);
  }

  /**
   * Sets async support on the linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param enabled 1 to enable, 0 to disable
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetAsyncSupport(final MemorySegment linkerPtr, final int enabled) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_async_support", Integer.class, linkerPtr, enabled);
  }

  /**
   * Sets the maximum random buffer size for WASI random operations.
   *
   * @param linkerPtr pointer to the component linker
   * @param maxSize the maximum size in bytes
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiMaxRandomSize(
      final MemorySegment linkerPtr, final long maxSize) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_max_random_size", Integer.class, linkerPtr, maxSize);
  }

  /**
   * Sets WASI Preview 2 arguments for the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param argsJsonPtr pointer to JSON array of argument strings
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiArgs(
      final MemorySegment linkerPtr, final MemorySegment argsJsonPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(argsJsonPtr, "argsJsonPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_args", Integer.class, linkerPtr, argsJsonPtr);
  }

  /**
   * Adds a WASI Preview 2 environment variable to the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param keyPtr pointer to environment variable key string
   * @param valuePtr pointer to environment variable value string
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerAddWasiEnv(
      final MemorySegment linkerPtr, final MemorySegment keyPtr, final MemorySegment valuePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(keyPtr, "keyPtr");
    validatePointer(valuePtr, "valuePtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_add_wasi_env", Integer.class, linkerPtr, keyPtr, valuePtr);
  }

  /**
   * Sets whether to inherit environment from host in WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param inherit 1 to inherit, 0 to not inherit
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiInheritEnv(final MemorySegment linkerPtr, final int inherit) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_inherit_env", Integer.class, linkerPtr, inherit);
  }

  /**
   * Sets whether to inherit arguments from host in WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param inherit 1 to inherit, 0 to not inherit
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiInheritArgs(final MemorySegment linkerPtr, final int inherit) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_inherit_args", Integer.class, linkerPtr, inherit);
  }

  /**
   * Sets whether to inherit stdio from host in WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param inherit 1 to inherit, 0 to not inherit
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiInheritStdio(final MemorySegment linkerPtr, final int inherit) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_inherit_stdio", Integer.class, linkerPtr, inherit);
  }

  /**
   * Adds a preopened directory for WASI Preview 2 with granular permissions.
   *
   * @param linkerPtr pointer to the component linker
   * @param hostPathPtr pointer to host path string
   * @param guestPathPtr pointer to guest path string
   * @param dirPermsBits directory permission bits (DirPerms)
   * @param filePermsBits file permission bits (FilePerms)
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerAddWasiPreopenDir(
      final MemorySegment linkerPtr,
      final MemorySegment hostPathPtr,
      final MemorySegment guestPathPtr,
      final int dirPermsBits,
      final int filePermsBits) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(hostPathPtr, "hostPathPtr");
    validatePointer(guestPathPtr, "guestPathPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_add_wasi_preopen_dir",
        Integer.class,
        linkerPtr,
        hostPathPtr,
        guestPathPtr,
        dirPermsBits,
        filePermsBits);
  }

  /**
   * Sets whether network access is allowed in WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param allow 1 to allow, 0 to disallow
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiAllowNetwork(final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_allow_network", Integer.class, linkerPtr, allow);
  }

  /**
   * Sets whether to inherit stdin individually.
   *
   * @param linkerPtr pointer to the component linker
   * @param inherit 1 to inherit, 0 to not inherit
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiInheritStdin(final MemorySegment linkerPtr, final int inherit) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_inherit_stdin", Integer.class, linkerPtr, inherit);
  }

  /**
   * Sets whether to inherit stdout individually.
   *
   * @param linkerPtr pointer to the component linker
   * @param inherit 1 to inherit, 0 to not inherit
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiInheritStdout(final MemorySegment linkerPtr, final int inherit) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_inherit_stdout", Integer.class, linkerPtr, inherit);
  }

  /**
   * Sets whether to inherit stderr individually.
   *
   * @param linkerPtr pointer to the component linker
   * @param inherit 1 to inherit, 0 to not inherit
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiInheritStderr(final MemorySegment linkerPtr, final int inherit) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_inherit_stderr", Integer.class, linkerPtr, inherit);
  }

  /**
   * Sets stdin bytes for the WASI context.
   *
   * @param linkerPtr pointer to the component linker
   * @param data pointer to the byte data
   * @param dataLen length of the byte data
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiStdinBytes(
      final MemorySegment linkerPtr, final MemorySegment data, final long dataLen) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_stdin_bytes",
        Integer.class,
        linkerPtr,
        data,
        dataLen);
  }

  /**
   * Sets whether TCP sockets are allowed.
   *
   * @param linkerPtr pointer to the component linker
   * @param allow 1 to allow, 0 to disallow
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiAllowTcp(final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_allow_tcp", Integer.class, linkerPtr, allow);
  }

  /**
   * Sets whether UDP sockets are allowed.
   *
   * @param linkerPtr pointer to the component linker
   * @param allow 1 to allow, 0 to disallow
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiAllowUdp(final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_allow_udp", Integer.class, linkerPtr, allow);
  }

  /**
   * Sets whether IP name lookup (DNS) is allowed.
   *
   * @param linkerPtr pointer to the component linker
   * @param allow 1 to allow, 0 to disallow
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiAllowIpNameLookup(
      final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_allow_ip_name_lookup",
        Integer.class,
        linkerPtr,
        allow);
  }

  /**
   * Sets whether blocking the current thread is allowed.
   *
   * @param linkerPtr pointer to the component linker
   * @param allow 1 to allow, 0 to disallow
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiAllowBlockingCurrentThread(
      final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_allow_blocking_current_thread",
        Integer.class,
        linkerPtr,
        allow);
  }

  /**
   * Sets the insecure random seed.
   *
   * @param linkerPtr pointer to the component linker
   * @param seed the seed value
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiInsecureRandomSeed(
      final MemorySegment linkerPtr, final long seed) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_insecure_random_seed",
        Integer.class,
        linkerPtr,
        seed);
  }

  /**
   * Sets a custom wall clock callback.
   *
   * @param linkerPtr pointer to the component linker
   * @param nowFn function pointer for now() callback
   * @param resolutionFn function pointer for resolution() callback
   * @param callbackId identifier for the Java-side clock
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiWallClock(
      final MemorySegment linkerPtr,
      final MemorySegment nowFn,
      final MemorySegment resolutionFn,
      final long callbackId) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_wall_clock",
        Integer.class,
        linkerPtr,
        nowFn,
        resolutionFn,
        callbackId);
  }

  /**
   * Sets a custom monotonic clock callback.
   *
   * @param linkerPtr pointer to the component linker
   * @param nowFn function pointer for now() callback
   * @param resolutionFn function pointer for resolution() callback
   * @param callbackId identifier for the Java-side clock
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiMonotonicClock(
      final MemorySegment linkerPtr,
      final MemorySegment nowFn,
      final MemorySegment resolutionFn,
      final long callbackId) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_monotonic_clock",
        Integer.class,
        linkerPtr,
        nowFn,
        resolutionFn,
        callbackId);
  }

  /**
   * Sets a custom secure random callback.
   *
   * @param linkerPtr pointer to the component linker
   * @param fillBytesFn function pointer for fillBytes() callback
   * @param callbackId identifier for the Java-side random source
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiSecureRandom(
      final MemorySegment linkerPtr, final MemorySegment fillBytesFn, final long callbackId) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_secure_random",
        Integer.class,
        linkerPtr,
        fillBytesFn,
        callbackId);
  }

  /**
   * Sets a custom insecure random callback.
   *
   * @param linkerPtr pointer to the component linker
   * @param fillBytesFn function pointer for fillBytes() callback
   * @param callbackId identifier for the Java-side random source
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiInsecureRandom(
      final MemorySegment linkerPtr, final MemorySegment fillBytesFn, final long callbackId) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_insecure_random",
        Integer.class,
        linkerPtr,
        fillBytesFn,
        callbackId);
  }

  /**
   * Sets a socket address check callback.
   *
   * @param linkerPtr pointer to the component linker
   * @param checkFn function pointer for check() callback
   * @param callbackId identifier for the Java-side check
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiSocketAddrCheck(
      final MemorySegment linkerPtr, final MemorySegment checkFn, final long callbackId) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_socket_addr_check",
        Integer.class,
        linkerPtr,
        checkFn,
        callbackId);
  }

  /**
   * Instantiates a component using the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param componentPtr pointer to the component
   * @param instanceOutPtr pointer to store the instance pointer
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerInstantiate(
      final MemorySegment linkerPtr,
      final MemorySegment componentPtr,
      final MemorySegment instanceOutPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(componentPtr, "componentPtr");
    validatePointer(instanceOutPtr, "instanceOutPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_instantiate",
        Integer.class,
        linkerPtr,
        componentPtr,
        instanceOutPtr);
  }

  // ===== Component Serialize/Deserialize =====

  /**
   * Serializes a component to bytes.
   *
   * <p>The caller must free the returned data using {@link #componentFreeSerializedData}.
   *
   * @param componentPtr the component pointer
   * @param dataPtrOut output pointer to receive the serialized data pointer
   * @param lenOut output pointer to receive the data length
   * @return 0 on success, non-zero on error
   */
  public int componentSerialize(
      final MemorySegment componentPtr,
      final MemorySegment dataPtrOut,
      final MemorySegment lenOut) {
    validatePointer(componentPtr, "componentPtr");
    validatePointer(dataPtrOut, "dataPtrOut");
    validatePointer(lenOut, "lenOut");
    return callNativeFunction(
        "wasmtime4j_component_serialize", Integer.class, componentPtr, dataPtrOut, lenOut);
  }

  /**
   * Frees serialized component data previously returned by {@link #componentSerialize}.
   *
   * @param dataPtr the data pointer to free
   * @param len the length of the data
   */
  public void componentFreeSerializedData(final MemorySegment dataPtr, final long len) {
    if (dataPtr != null && !dataPtr.equals(MemorySegment.NULL) && len > 0) {
      callNativeFunction("wasmtime4j_component_free_serialized_data", Void.class, dataPtr, len);
    }
  }

  /**
   * Deserializes a component from bytes using the enhanced component engine.
   *
   * @param enginePtr pointer to the enhanced component engine
   * @param dataPtr pointer to the serialized data
   * @param len length of the serialized data
   * @param componentPtrOut output pointer to receive the new component pointer
   * @return 0 on success, non-zero on error
   */
  public int panamaComponentDeserialize(
      final MemorySegment enginePtr,
      final MemorySegment dataPtr,
      final long len,
      final MemorySegment componentPtrOut) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(dataPtr, "dataPtr");
    validatePointer(componentPtrOut, "componentPtrOut");
    return callNativeFunction(
        "wasmtime4j_panama_component_deserialize",
        Integer.class,
        enginePtr,
        dataPtr,
        len,
        componentPtrOut);
  }

  /**
   * Deserializes a component from a file using the enhanced component engine.
   *
   * @param enginePtr pointer to the enhanced component engine
   * @param pathPtr pointer to the file path bytes (UTF-8)
   * @param pathLen length of the file path
   * @param componentPtrOut output pointer to receive the new component pointer
   * @return 0 on success, non-zero on error
   */
  public int panamaComponentDeserializeFile(
      final MemorySegment enginePtr,
      final MemorySegment pathPtr,
      final long pathLen,
      final MemorySegment componentPtrOut) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(pathPtr, "pathPtr");
    validatePointer(componentPtrOut, "componentPtrOut");
    return callNativeFunction(
        "wasmtime4j_panama_component_deserialize_file",
        Integer.class,
        enginePtr,
        pathPtr,
        pathLen,
        componentPtrOut);
  }

  /**
   * Gets the resources required by a component.
   *
   * @param componentPtr pointer to the component
   * @param numMemoriesOut output pointer for number of memories (i32)
   * @param maxMemoryOut output pointer for max initial memory size (i64, -1 if unbounded)
   * @param numTablesOut output pointer for number of tables (i32)
   * @param maxTableOut output pointer for max initial table size (i64, -1 if unbounded)
   * @return 0 on success, non-zero on error. numMemories=-2 means resources_required() returned
   *     None
   */
  public int panamaComponentResourcesRequired(
      final MemorySegment componentPtr,
      final MemorySegment numMemoriesOut,
      final MemorySegment maxMemoryOut,
      final MemorySegment numTablesOut,
      final MemorySegment maxTableOut) {
    validatePointer(componentPtr, "componentPtr");
    validatePointer(numMemoriesOut, "numMemoriesOut");
    validatePointer(maxMemoryOut, "maxMemoryOut");
    validatePointer(numTablesOut, "numTablesOut");
    validatePointer(maxTableOut, "maxTableOut");
    return callNativeFunction(
        "wasmtime4j_panama_component_resources_required",
        Integer.class,
        componentPtr,
        numMemoriesOut,
        maxMemoryOut,
        numTablesOut,
        maxTableOut);
  }

  /**
   * Gets the image range of a compiled component.
   *
   * @param componentPtr pointer to the component
   * @param startPtr output pointer for the start address (u64)
   * @param endPtr output pointer for the end address (u64)
   * @return 0 on success, negative error code on failure
   */
  public int componentImageRange(
      final MemorySegment componentPtr, final MemorySegment startPtr, final MemorySegment endPtr) {
    validatePointer(componentPtr, "componentPtr");
    validatePointer(startPtr, "startPtr");
    validatePointer(endPtr, "endPtr");
    return callNativeFunction(
        "wasmtime4j_panama_component_image_range", Integer.class, componentPtr, startPtr, endPtr);
  }

  /**
   * Pre-initializes a component's copy-on-write image for faster instantiation.
   *
   * @param componentPtr pointer to the component
   * @return 0 on success, negative error code on failure
   */
  public int componentInitializeCowImage(final MemorySegment componentPtr) {
    validatePointer(componentPtr, "componentPtr");
    return callNativeFunction(
        "wasmtime4j_panama_component_initialize_cow_image", Integer.class, componentPtr);
  }

  // ===== ComponentInstancePre =====

  /**
   * Pre-instantiates a component using the linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param componentPtr pointer to the component
   * @return pointer to the pre-instantiated handle, or null on failure
   */
  public MemorySegment componentLinkerInstantiatePre(
      final MemorySegment linkerPtr, final MemorySegment componentPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(componentPtr, "componentPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_instantiate_pre",
        MemorySegment.class,
        linkerPtr,
        componentPtr);
  }

  /**
   * Allow or disallow shadowing of previously defined names.
   *
   * @param linkerPtr pointer to the component linker
   * @param allow 1 to allow, 0 to disallow
   */
  public void componentLinkerAllowShadowing(final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    callNativeFunction("wasmtime4j_component_linker_allow_shadowing", void.class, linkerPtr, allow);
  }

  /**
   * Define all unknown imports as traps.
   *
   * @param linkerPtr pointer to the component linker
   * @param componentPtr pointer to the component
   * @return 0 on success, -1 on error
   */
  public int componentLinkerDefineUnknownImportsAsTraps(
      final MemorySegment linkerPtr, final MemorySegment componentPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(componentPtr, "componentPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_define_unknown_imports_as_traps",
        int.class,
        linkerPtr,
        componentPtr);
  }

  /**
   * Defines a host resource type on the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param interfacePathPtr UTF-8 encoded interface path (e.g., "wasi:io/streams")
   * @param interfacePathLen length of the interface path
   * @param resourceNamePtr UTF-8 encoded resource name
   * @param resourceNameLen length of the resource name
   * @param resourceId unique resource ID
   * @param destructorFn function pointer for the destructor callback (nullable)
   * @param destructorCallbackId callback ID passed to the destructor function
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerDefineResource(
      final MemorySegment linkerPtr,
      final MemorySegment interfacePathPtr,
      final long interfacePathLen,
      final MemorySegment resourceNamePtr,
      final long resourceNameLen,
      final int resourceId,
      final MemorySegment destructorFn,
      final long destructorCallbackId) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(interfacePathPtr, "interfacePathPtr");
    validatePointer(resourceNamePtr, "resourceNamePtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_define_resource",
        Integer.class,
        linkerPtr,
        interfacePathPtr,
        interfacePathLen,
        resourceNamePtr,
        resourceNameLen,
        resourceId,
        destructorFn,
        destructorCallbackId);
  }

  /**
   * Defines a core module on the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param instancePathPtr UTF-8 encoded instance path
   * @param instancePathLen length of the instance path
   * @param namePtr UTF-8 encoded module name
   * @param nameLen length of the module name
   * @param modulePtr pointer to the native module
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerDefineModule(
      final MemorySegment linkerPtr,
      final MemorySegment instancePathPtr,
      final long instancePathLen,
      final MemorySegment namePtr,
      final long nameLen,
      final MemorySegment modulePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(instancePathPtr, "instancePathPtr");
    validatePointer(namePtr, "namePtr");
    validatePointer(modulePtr, "modulePtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_define_module",
        Integer.class,
        linkerPtr,
        instancePathPtr,
        instancePathLen,
        namePtr,
        nameLen,
        modulePtr);
  }

  /**
   * Defines a host function on the component linker using a WIT path.
   *
   * @param linkerPtr pointer to the component linker
   * @param witPathPtr UTF-8 encoded WIT path (e.g., "wasi:cli/stdout#print")
   * @param witPathLen length of the WIT path
   * @param callbackFn function pointer for the host function callback
   * @param callbackId callback ID passed to the function pointer
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerDefineHostFunction(
      final MemorySegment linkerPtr,
      final MemorySegment witPathPtr,
      final long witPathLen,
      final MemorySegment callbackFn,
      final long callbackId) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(witPathPtr, "witPathPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_define_host_function",
        Integer.class,
        linkerPtr,
        witPathPtr,
        witPathLen,
        callbackFn,
        callbackId);
  }

  /**
   * Defines an async host function on the component linker using a WIT path.
   *
   * @param linkerPtr pointer to the component linker
   * @param witPathPtr UTF-8 encoded WIT path (e.g., "wasi:cli/stdout#print")
   * @param witPathLen length of the WIT path
   * @param callbackFn function pointer for the host function callback
   * @param callbackId callback ID passed to the function pointer
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerDefineHostFunctionAsync(
      final MemorySegment linkerPtr,
      final MemorySegment witPathPtr,
      final long witPathLen,
      final MemorySegment callbackFn,
      final long callbackId) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(witPathPtr, "witPathPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_define_host_function_async",
        Integer.class,
        linkerPtr,
        witPathPtr,
        witPathLen,
        callbackFn,
        callbackId);
  }

  /**
   * Allocates a result buffer for host function callback results.
   *
   * @param len length of the buffer to allocate
   * @return pointer to the allocated buffer
   */
  public MemorySegment componentHostCallbackAllocResult(final long len) {
    return callNativeFunction(
        "wasmtime4j_component_host_callback_alloc_result", MemorySegment.class, len);
  }

  /**
   * Instantiates from a pre-instantiated component.
   *
   * @param prePtr pointer to the pre-instantiated handle
   * @param instanceOutPtr pointer to store the instance pointer
   * @return 0 on success, non-zero on error
   */
  public int componentInstancePreInstantiate(
      final MemorySegment prePtr, final MemorySegment instanceOutPtr) {
    validatePointer(prePtr, "prePtr");
    validatePointer(instanceOutPtr, "instanceOutPtr");
    return callNativeFunction(
        "wasmtime4j_component_instance_pre_instantiate", Integer.class, prePtr, instanceOutPtr);
  }

  /**
   * Instantiates from a pre-instantiated component with store configuration.
   *
   * @param prePtr pointer to the pre-instantiated handle
   * @param fuelLimit fuel limit for the store (0 for no limit)
   * @param epochDeadline epoch deadline for the store (0 for no deadline)
   * @param maxMemoryBytes maximum memory in bytes (0 for unlimited)
   * @param maxTableElements maximum elements per table (0 for unlimited)
   * @param maxInstances maximum wasm instances (0 for unlimited)
   * @param maxTables maximum tables (0 for unlimited)
   * @param maxMemories maximum memories (0 for unlimited)
   * @param trapOnGrowFailure whether to trap on grow failure (1) or return -1 (0)
   * @param instanceOutPtr pointer to store the instance pointer
   * @return 0 on success, non-zero on error
   */
  public int componentInstancePreInstantiateWithConfig(
      final MemorySegment prePtr,
      final long fuelLimit,
      final long epochDeadline,
      final long maxMemoryBytes,
      final long maxTableElements,
      final long maxInstances,
      final long maxTables,
      final long maxMemories,
      final byte trapOnGrowFailure,
      final MemorySegment instanceOutPtr) {
    validatePointer(prePtr, "prePtr");
    validatePointer(instanceOutPtr, "instanceOutPtr");
    return callNativeFunction(
        "wasmtime4j_component_instance_pre_instantiate_with_config",
        Integer.class,
        prePtr,
        fuelLimit,
        epochDeadline,
        maxMemoryBytes,
        maxTableElements,
        maxInstances,
        maxTables,
        maxMemories,
        trapOnGrowFailure,
        instanceOutPtr);
  }

  /**
   * Checks if a pre-instantiated component handle is valid.
   *
   * @param prePtr pointer to the pre-instantiated handle
   * @return 1 if valid, 0 if not
   */
  public int componentInstancePreIsValid(final MemorySegment prePtr) {
    validatePointer(prePtr, "prePtr");
    return callNativeFunction("wasmtime4j_component_instance_pre_is_valid", Integer.class, prePtr);
  }

  /**
   * Gets the instance count from a pre-instantiated component.
   *
   * @param prePtr pointer to the pre-instantiated handle
   * @return the number of instances created
   */
  public long componentInstancePreInstanceCount(final MemorySegment prePtr) {
    validatePointer(prePtr, "prePtr");
    return callNativeFunction(
        "wasmtime4j_component_instance_pre_instance_count", Long.class, prePtr);
  }

  /**
   * Gets the preparation time in nanoseconds from a pre-instantiated component.
   *
   * @param prePtr pointer to the pre-instantiated handle
   * @return preparation time in nanoseconds
   */
  public long componentInstancePrePreparationTimeNs(final MemorySegment prePtr) {
    validatePointer(prePtr, "prePtr");
    return callNativeFunction(
        "wasmtime4j_component_instance_pre_preparation_time_ns", Long.class, prePtr);
  }

  /**
   * Gets the average instantiation time in nanoseconds from a pre-instantiated component.
   *
   * @param prePtr pointer to the pre-instantiated handle
   * @return average instantiation time in nanoseconds
   */
  public long componentInstancePreAvgInstantiationTimeNs(final MemorySegment prePtr) {
    validatePointer(prePtr, "prePtr");
    return callNativeFunction(
        "wasmtime4j_component_instance_pre_avg_instantiation_time_ns", Long.class, prePtr);
  }

  /**
   * Destroys a pre-instantiated component handle.
   *
   * @param prePtr pointer to the pre-instantiated handle to destroy
   */
  public void componentInstancePreDestroy(final MemorySegment prePtr) {
    validatePointer(prePtr, "prePtr");
    callNativeFunction("wasmtime4j_component_instance_pre_destroy", Void.class, prePtr);
  }

  // ===== ComponentExportIndex =====

  /**
   * Looks up a component export index by name.
   *
   * @param componentPtr pointer to the component
   * @param instanceIndexPtr optional parent instance index (may be NULL)
   * @param name C string with the export name
   * @param indexOut output pointer for the export index handle
   * @return 0 if found, 1 if not found, -1 on error
   */
  public int componentGetExportIndex(
      final MemorySegment componentPtr,
      final MemorySegment instanceIndexPtr,
      final MemorySegment name,
      final MemorySegment indexOut) {
    validatePointer(componentPtr, "componentPtr");
    validatePointer(name, "name");
    validatePointer(indexOut, "indexOut");
    return callNativeFunction(
        "wasmtime4j_panama_component_get_export_index",
        Integer.class,
        componentPtr,
        instanceIndexPtr,
        name,
        indexOut);
  }

  /**
   * Destroys a component export index handle.
   *
   * @param indexPtr pointer to the export index to destroy
   */
  public void componentExportIndexDestroy(final MemorySegment indexPtr) {
    if (indexPtr != null && !indexPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_component_export_index_destroy", Void.class, indexPtr);
    }
  }

  /**
   * Checks if a component instance has a function at the given export index.
   *
   * @param enginePtr pointer to the enhanced component engine
   * @param instanceId the component instance ID
   * @param indexPtr pointer to the export index
   * @return 1 if found, 0 if not found, -1 on error
   */
  public int enhancedComponentInstanceHasFuncByIndex(
      final MemorySegment enginePtr, final long instanceId, final MemorySegment indexPtr) {
    validatePointer(indexPtr, "indexPtr");
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_instance_has_func_by_index",
        Integer.class,
        enginePtr,
        instanceId,
        indexPtr);
  }

  /**
   * Looks up a general export by name on a component instance.
   *
   * <p>Returns the export kind code (0-6) on success, -1 if not found, -2 on error. On success,
   * writes the boxed ComponentExportIndex pointer to outIndexPtr.
   *
   * @param enginePtr pointer to the enhanced component engine
   * @param instanceId the component instance ID
   * @param parentIndexPtr pointer to parent export index (NULL for root)
   * @param namePtr pointer to the export name bytes
   * @param nameLen length of the export name
   * @param outIndexPtr pointer to output export index pointer (caller-allocated)
   * @return kind code (0-6), -1 if not found, -2 on error
   */
  public int enhancedComponentInstanceGetExport(
      final MemorySegment enginePtr,
      final long instanceId,
      final MemorySegment parentIndexPtr,
      final MemorySegment namePtr,
      final long nameLen,
      final MemorySegment outIndexPtr) {
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_instance_get_export",
        Integer.class,
        enginePtr,
        instanceId,
        parentIndexPtr,
        namePtr,
        nameLen,
        outIndexPtr);
  }

  // ===== Concurrent Call Support =====

  /**
   * Executes concurrent component function calls via the enhanced component engine.
   *
   * @param enginePtr the enhanced engine pointer
   * @param instanceId the component instance ID
   * @param jsonPtr pointer to the JSON input bytes
   * @param jsonLen length of the JSON input bytes
   * @param resultPtr pointer to output result pointer (caller-allocated)
   * @param resultLen pointer to output result length (caller-allocated)
   * @return 0 on success, -1 on error
   */
  public int enhancedComponentRunConcurrent(
      final MemorySegment enginePtr,
      final long instanceId,
      final MemorySegment jsonPtr,
      final long jsonLen,
      final MemorySegment resultPtr,
      final MemorySegment resultLen) {
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_run_concurrent",
        Integer.class,
        enginePtr,
        instanceId,
        jsonPtr,
        jsonLen,
        resultPtr,
        resultLen);
  }

  /**
   * Frees a result buffer allocated by the concurrent call function.
   *
   * @param ptr the pointer to the result buffer
   * @param len the length of the result buffer
   */
  public void freeConcurrentResult(final MemorySegment ptr, final long len) {
    callNativeFunction("wasmtime4j_panama_free_concurrent_result", Void.class, ptr, len);
  }

  // ===== Async Val Lifecycle =====

  /**
   * Closes an async val handle (Future/Stream/ErrorContext) in the native AsyncValRegistry.
   *
   * <p>This removes the handle from the global registry, dropping the stored Val. Safe to call with
   * handles that have already been consumed or closed (no-op).
   *
   * @param handle the async val handle to close
   * @return 0 on success (handle found and removed), -1 if handle was not found
   */
  public int asyncValClose(final long handle) {
    return callNativeFunction("wasmtime4j_panama_async_val_close", Integer.class, handle);
  }

  /**
   * Creates a {@link Runnable} close action that invokes {@link #asyncValClose(long)} for the given
   * handle. Suitable for use with {@link ai.tegmentum.wasmtime4j.component.StreamAny#create(long,
   * Runnable)}, {@link ai.tegmentum.wasmtime4j.component.FutureAny#create(long, Runnable)}, and
   * {@link ai.tegmentum.wasmtime4j.component.ErrorContext#create(long, Runnable)}.
   *
   * @param handle the async val handle
   * @return a Runnable that will close the handle when invoked
   */
  public Runnable createAsyncValCloseAction(final long handle) {
    return () -> asyncValClose(handle);
  }

  // ===== ResourceAny Lifecycle =====

  /**
   * Drops a ResourceAny held in the global resource registry.
   *
   * <p>Takes the resource from the registry and calls resource_drop on it using the store
   * associated with the given component instance.
   *
   * @param enginePtr the enhanced component engine pointer
   * @param instanceId the component instance ID that owns the store
   * @param resourceHandle the resource handle ID from the global registry
   * @return 0 on success, non-zero on error
   */
  public int resourceAnyDrop(
      final MemorySegment enginePtr, final long instanceId, final long resourceHandle) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_panama_resource_any_drop",
        Integer.class,
        enginePtr,
        instanceId,
        resourceHandle);
  }
}

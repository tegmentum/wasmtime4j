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

import ai.tegmentum.wasmtime4j.ExternRef;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Native function bindings for memory, global, and table operations.
 *
 * <p>Provides type-safe wrappers for Panama FFI memory operations (read, write, grow, size), atomic
 * memory operations (compare-and-swap, load, store, fence, notify, wait), global variable
 * operations (get, set, create, destroy, metadata), table operations (size, get, set, grow, fill,
 * copy, init, destroy), data/element segment operations, and error handling functions.
 *
 * <p>Hot-path operations ({@link #panamaMemoryReadBytes} and {@link #panamaMemoryWriteBytes}) use
 * eagerly-initialized volatile {@link MethodHandle} fields for {@code invokeExact} optimization.
 */
public final class NativeMemoryBindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeMemoryBindings.class.getName());

  /** Initialization-on-demand holder for thread-safe lazy singleton. */
  private static final class Holder {
    static final NativeMemoryBindings INSTANCE = new NativeMemoryBindings();
  }

  /** Hot-path MethodHandle for memory read bytes (invokeExact optimization). */
  private volatile MethodHandle mhPanamaMemoryReadBytes;

  /** Hot-path MethodHandle for memory write bytes (invokeExact optimization). */
  private volatile MethodHandle mhPanamaMemoryWriteBytes;

  private NativeMemoryBindings() {
    super();
    initializeBindings();

    // Eagerly initialize hot-path MethodHandles for invokeExact optimization
    FunctionBinding memReadBinding = getFunctionBinding("wasmtime4j_panama_memory_read_bytes");
    if (memReadBinding != null) {
      this.mhPanamaMemoryReadBytes = memReadBinding.getMethodHandle().orElse(null);
    }

    FunctionBinding memWriteBinding = getFunctionBinding("wasmtime4j_panama_memory_write_bytes");
    if (memWriteBinding != null) {
      this.mhPanamaMemoryWriteBytes = memWriteBinding.getMethodHandle().orElse(null);
    }

    markInitialized();
    LOGGER.fine("Initialized NativeMemoryBindings successfully");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeMemoryBindings getInstance() {
    return Holder.INSTANCE;
  }

  private void initializeBindings() {
    // ==================================================================================
    // Error handling functions
    // ==================================================================================
    addFunctionBinding(
        "wasmtime4j_panama_get_last_error_message", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_free_error_message",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // message_ptr

    addFunctionBinding(
        "wasmtime4j_free_string", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // string_ptr

    addFunctionBinding(
        "wasmtime4j_clear_error_state", FunctionDescriptor.ofVoid()); // no parameters

    // ==================================================================================
    // Panama FFI memory functions
    // ==================================================================================
    addFunctionBinding(
        "wasmtime4j_panama_memory_create_with_config",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // initial_pages (u64)
            ValueLayout.JAVA_LONG, // maximum_pages (u64)
            ValueLayout.JAVA_INT, // is_shared
            ValueLayout.JAVA_INT, // is_64
            ValueLayout.JAVA_INT, // memory_index
            ValueLayout.ADDRESS, // name
            ValueLayout.ADDRESS)); // memory_ptr_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_size_pages",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // size_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_grow",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // additional_pages (u64)
            ValueLayout.ADDRESS)); // previous_pages_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_grow64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // additional_pages
            ValueLayout.ADDRESS)); // previous_pages_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_grow_async",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // additional_pages (u64)
            ValueLayout.ADDRESS)); // previous_pages_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_read_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // length
            ValueLayout.ADDRESS)); // buffer

    addFunctionBinding(
        "wasmtime4j_panama_memory_write_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // length
            ValueLayout.ADDRESS)); // buffer

    addFunctionBinding(
        "wasmtime4j_panama_memory_size_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // size_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_get_data",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // data_ptr_out
            ValueLayout.ADDRESS)); // size_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_is_64bit",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // is_64bit_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_is_shared",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // is_shared_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_size_pages64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // size_out (u64)

    addFunctionBinding(
        "wasmtime4j_panama_memory_get_minimum",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // minimum_out (u64)

    addFunctionBinding(
        "wasmtime4j_panama_memory_get_maximum",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // maximum_out (i64, -1 if unlimited)

    addFunctionBinding(
        "wasmtime4j_panama_memory_init",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.JAVA_INT, // dest_offset
            ValueLayout.JAVA_INT, // data_segment_index
            ValueLayout.JAVA_INT, // src_offset
            ValueLayout.JAVA_INT)); // length

    addFunctionBinding(
        "wasmtime4j_panama_data_drop",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.JAVA_INT)); // data_segment_index

    // Panama Memory Registry Functions
    addFunctionBinding(
        "wasmtime4j_panama_memory_registry_create",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_memory_registry_register",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_memory_registry_get",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_memory_registry_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_memory_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_memory_clear_handle_registries",
        FunctionDescriptor.of(ValueLayout.JAVA_INT));

    // ==================================================================================
    // Atomic memory operations
    // ==================================================================================
    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_compare_and_swap_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // expected
            ValueLayout.JAVA_INT, // new_value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_compare_and_swap_i64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // expected
            ValueLayout.JAVA_LONG, // new_value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_load_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_load_i64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_store_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT)); // value

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_store_i64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG)); // value

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_add_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_add_i64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_and_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_or_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_xor_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_and_i64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_or_i64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_xor_i64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // value
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_fence",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS)); // store_ptr

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_notify",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // count
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_wait32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_INT, // expected
            ValueLayout.JAVA_LONG, // timeout_ns
            ValueLayout.ADDRESS)); // result_out

    addFunctionBinding(
        "wasmtime4j_panama_memory_atomic_wait64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // memory_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_LONG, // offset
            ValueLayout.JAVA_LONG, // expected
            ValueLayout.JAVA_LONG, // timeout_ns
            ValueLayout.ADDRESS)); // result_out

    // ==================================================================================
    // Global functions
    // ==================================================================================
    addFunctionBinding(
        "wasmtime_global_get",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // global
            ValueLayout.ADDRESS)); // value (out)

    addFunctionBinding(
        "wasmtime_global_set",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // global
            ValueLayout.ADDRESS)); // value

    addFunctionBinding(
        "wasmtime_global_type",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return global type
            ValueLayout.ADDRESS)); // global

    addFunctionBinding(
        "wasmtime4j_global_create",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return global_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // value_type
            ValueLayout.JAVA_INT, // is_mutable
            ValueLayout.JAVA_INT, // i32_value
            ValueLayout.JAVA_LONG, // i64_value
            ValueLayout.JAVA_FLOAT, // f32_value
            ValueLayout.JAVA_DOUBLE, // f64_value
            ValueLayout.ADDRESS)); // ref_value

    addFunctionBinding(
        "wasmtime4j_global_create_mutable",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // value_type
            ValueLayout.ADDRESS, // initial_value
            ValueLayout.ADDRESS)); // global_ptr (out)

    addFunctionBinding(
        "wasmtime4j_global_create_immutable",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // value_type
            ValueLayout.ADDRESS, // initial_value
            ValueLayout.ADDRESS)); // global_ptr (out)

    addFunctionBinding(
        "wasmtime4j_global_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // global_ptr

    addFunctionBinding(
        "wasmtime4j_panama_global_get",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // global_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // i32_value out
            ValueLayout.ADDRESS, // i64_value out
            ValueLayout.ADDRESS, // f32_value out
            ValueLayout.ADDRESS, // f64_value out
            ValueLayout.ADDRESS, // ref_id_present out
            ValueLayout.ADDRESS, // ref_id out
            ValueLayout.ADDRESS)); // v128_bytes out (16 bytes)

    addFunctionBinding(
        "wasmtime4j_panama_global_set",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // global_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // value_type
            ValueLayout.JAVA_INT, // i32_value
            ValueLayout.JAVA_LONG, // i64_value
            ValueLayout.JAVA_DOUBLE, // f32_value
            ValueLayout.JAVA_DOUBLE, // f64_value
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG, // ref_id
            ValueLayout.ADDRESS)); // v128_bytes ptr (16 bytes, can be null)

    addFunctionBinding(
        "wasmtime4j_global_get_type_info",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // global
            ValueLayout.ADDRESS, // type_info (out)
            ValueLayout.ADDRESS)); // mutability (out)

    addFunctionBinding(
        "wasmtime4j_panama_global_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // value_type
            ValueLayout.JAVA_INT, // mutability
            ValueLayout.JAVA_INT, // i32_value
            ValueLayout.JAVA_LONG, // i64_value
            ValueLayout.JAVA_DOUBLE, // f32_value
            ValueLayout.JAVA_DOUBLE, // f64_value
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG, // ref_id
            ValueLayout.ADDRESS, // v128_bytes ptr (16 bytes, can be null)
            ValueLayout.ADDRESS, // name_ptr (optional)
            ValueLayout.ADDRESS)); // global_ptr (out)

    addFunctionBinding(
        "wasmtime4j_panama_global_metadata",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // global_ptr
            ValueLayout.ADDRESS, // value_type (out)
            ValueLayout.ADDRESS, // mutability (out)
            ValueLayout.ADDRESS)); // name_ptr (out)

    addFunctionBinding(
        "wasmtime4j_panama_global_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // global_ptr

    // Cross-module global sharing functions
    addFunctionBinding(
        "wasmtime4j_global_register_shared",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return result code
            ValueLayout.ADDRESS, // global
            ValueLayout.ADDRESS, // name
            ValueLayout.ADDRESS)); // registry

    addFunctionBinding(
        "wasmtime4j_global_lookup_shared",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return global or null
            ValueLayout.ADDRESS, // name
            ValueLayout.ADDRESS)); // registry

    // Zero-copy global access functions
    addFunctionBinding(
        "wasmtime4j_global_get_direct_access",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return direct pointer or null
            ValueLayout.ADDRESS)); // global

    addFunctionBinding(
        "wasmtime4j_global_release_direct_access",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // global
            ValueLayout.ADDRESS)); // direct_ptr

    // ==================================================================================
    // Table functions
    // ==================================================================================
    addFunctionBinding(
        "wasmtime4j_table_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return size
            ValueLayout.ADDRESS)); // table_ptr

    addFunctionBinding(
        "wasmtime4j_table_get",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // index
            ValueLayout.ADDRESS, // ref_id_present out param
            ValueLayout.ADDRESS)); // ref_id out param

    addFunctionBinding(
        "wasmtime4j_table_set",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // index
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG)); // ref_id

    addFunctionBinding(
        "wasmtime4j_table_grow",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // elements
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG)); // ref_id

    addFunctionBinding(
        "wasmtime4j_table_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // table_ptr

    addFunctionBinding(
        "wasmtime4j_table_metadata",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // element_type out param
            ValueLayout.ADDRESS, // initial_size out param
            ValueLayout.ADDRESS, // has_maximum out param
            ValueLayout.ADDRESS, // maximum_size out param
            ValueLayout.ADDRESS)); // name_ptr out param

    // Panama FFI table functions
    addFunctionBinding(
        "wasmtime4j_panama_table_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // initial_size
            ValueLayout.JAVA_INT, // has_maximum
            ValueLayout.JAVA_INT, // maximum_size
            ValueLayout.ADDRESS, // name_ptr
            ValueLayout.ADDRESS)); // table_ptr out param

    addFunctionBinding(
        "wasmtime4j_panama_table_create64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_LONG, // initial_size (64-bit)
            ValueLayout.JAVA_INT, // has_maximum
            ValueLayout.JAVA_LONG, // maximum_size (64-bit)
            ValueLayout.ADDRESS, // name_ptr
            ValueLayout.ADDRESS)); // table_ptr out param

    addFunctionBinding(
        "wasmtime4j_panama_table_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS)); // size out param

    addFunctionBinding(
        "wasmtime4j_panama_table_get",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // index
            ValueLayout.ADDRESS, // ref_id_present out param
            ValueLayout.ADDRESS)); // ref_id out param

    addFunctionBinding(
        "wasmtime4j_panama_table_set",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // index
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG)); // ref_id

    addFunctionBinding(
        "wasmtime4j_panama_table_grow",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // delta
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG, // ref_id
            ValueLayout.ADDRESS)); // old_size out param

    addFunctionBinding(
        "wasmtime4j_panama_table_grow_async",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // delta
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG, // ref_id
            ValueLayout.ADDRESS)); // old_size out param

    addFunctionBinding(
        "wasmtime4j_panama_table_fill",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // dst
            ValueLayout.JAVA_INT, // len
            ValueLayout.JAVA_INT, // element_type
            ValueLayout.JAVA_INT, // ref_id_present
            ValueLayout.JAVA_LONG)); // ref_id

    addFunctionBinding(
        "wasmtime4j_panama_table_metadata",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // element_type out param
            ValueLayout.ADDRESS, // initial_size out param (64-bit)
            ValueLayout.ADDRESS, // has_maximum out param
            ValueLayout.ADDRESS, // maximum_size out param (64-bit)
            ValueLayout.ADDRESS, // is_64 out param
            ValueLayout.ADDRESS)); // name_ptr out param

    addFunctionBinding(
        "wasmtime4j_panama_table_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // table_ptr

    addFunctionBinding(
        "wasmtime4j_panama_table_is_64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return value (1=64-bit, 0=32-bit, -1=error)
            ValueLayout.ADDRESS)); // table_ptr

    addFunctionBinding(
        "wasmtime4j_panama_table_init",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.JAVA_INT, // dst
            ValueLayout.JAVA_INT, // src
            ValueLayout.JAVA_INT, // len
            ValueLayout.JAVA_INT)); // segment_index

    addFunctionBinding(
        "wasmtime4j_panama_table_copy",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // dst
            ValueLayout.JAVA_INT, // src
            ValueLayout.JAVA_INT)); // len

    addFunctionBinding(
        "wasmtime4j_panama_table_copy_from",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // dst_table_ptr
            ValueLayout.ADDRESS, // store_ptr
            ValueLayout.JAVA_INT, // dst
            ValueLayout.ADDRESS, // src_table_ptr
            ValueLayout.JAVA_INT, // src
            ValueLayout.JAVA_INT)); // len

    addFunctionBinding(
        "wasmtime4j_panama_elem_drop",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // instance_ptr
            ValueLayout.JAVA_INT)); // segment_index

    addFunctionBinding(
        "wasmtime4j_panama_table_supports_64bit_addressing",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return value (1=64-bit, 0=32-bit)
            ValueLayout.ADDRESS, // table_ptr
            ValueLayout.ADDRESS)); // store_ptr
  }

  // ====================================================================================
  // Panama Memory Operations
  // ====================================================================================

  /**
   * Initializes memory from a data segment (memory.init instruction).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param instancePtr pointer to the instance
   * @param destOffset destination offset in memory
   * @param dataSegmentIndex data segment index
   * @param srcOffset source offset in segment
   * @param length number of bytes to copy
   * @return 0 on success, non-zero on error
   */
  public int panamaMemoryInit(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final MemorySegment instancePtr,
      final int destOffset,
      final int dataSegmentIndex,
      final int srcOffset,
      final int length) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(instancePtr, "instancePtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_init",
        Integer.class,
        memoryPtr,
        storePtr,
        instancePtr,
        destOffset,
        dataSegmentIndex,
        srcOffset,
        length);
  }

  /**
   * Drops a data segment from memory (data.drop instruction).
   *
   * @param instancePtr pointer to the instance
   * @param dataSegmentIndex data segment index to drop
   * @return 0 on success, non-zero on error
   */
  public int dataSegmentDrop(final MemorySegment instancePtr, final int dataSegmentIndex) {
    validatePointer(instancePtr, "instancePtr");

    return callNativeFunction(
        "wasmtime4j_panama_data_drop", Integer.class, instancePtr, dataSegmentIndex);
  }

  /**
   * Gets memory size in pages (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param sizeOutPtr pointer to store the size in pages
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemorySizePages(
      final MemorySegment memoryPtr, final MemorySegment storePtr, final MemorySegment sizeOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(sizeOutPtr, "sizeOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_size_pages", Integer.class, memoryPtr, storePtr, sizeOutPtr);
  }

  /**
   * Grows memory by additional pages (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param additionalPages number of pages to grow
   * @param previousPagesOutPtr pointer to store the previous size in pages
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryGrow(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long additionalPages,
      final MemorySegment previousPagesOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(previousPagesOutPtr, "previousPagesOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_grow",
        Integer.class,
        memoryPtr,
        storePtr,
        additionalPages,
        previousPagesOutPtr);
  }

  /**
   * Grows memory by additional pages using 64-bit addressing (Panama FFI version).
   *
   * <p>This supports Memory64 proposal for memories larger than 4GB.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param additionalPages number of pages to grow (64-bit)
   * @param previousPagesOutPtr pointer to store the previous size in pages (64-bit)
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryGrow64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long additionalPages,
      final MemorySegment previousPagesOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(previousPagesOutPtr, "previousPagesOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_grow64",
        Integer.class,
        memoryPtr,
        storePtr,
        additionalPages,
        previousPagesOutPtr);
  }

  /**
   * Grows memory asynchronously through the async resource limiter (Panama FFI version).
   *
   * <p>Requires engine with {@code asyncSupport(true)}.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param additionalPages number of pages to grow
   * @param previousPagesOutPtr pointer to store the previous size in pages
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryGrowAsync(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long additionalPages,
      final MemorySegment previousPagesOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(previousPagesOutPtr, "previousPagesOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_grow_async",
        Integer.class,
        memoryPtr,
        storePtr,
        additionalPages,
        previousPagesOutPtr);
  }

  /**
   * Checks if memory uses 64-bit addressing (Memory64 proposal).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param is64BitOutPtr pointer to store the result (1 if 64-bit, 0 if 32-bit)
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryIs64Bit(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final MemorySegment is64BitOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(is64BitOutPtr, "is64BitOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_is_64bit", Integer.class, memoryPtr, storePtr, is64BitOutPtr);
  }

  /**
   * Checks if memory is shared between threads (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param isSharedOutPtr pointer to store the result (1 if shared, 0 if not shared)
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryIsShared(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final MemorySegment isSharedOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(isSharedOutPtr, "isSharedOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_is_shared", Integer.class, memoryPtr, storePtr, isSharedOutPtr);
  }

  /**
   * Gets memory type minimum pages (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param minimumOutPtr pointer to store the minimum pages (64-bit unsigned)
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryGetMinimum(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final MemorySegment minimumOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(minimumOutPtr, "minimumOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_get_minimum", Integer.class, memoryPtr, storePtr, minimumOutPtr);
  }

  /**
   * Gets memory type maximum pages (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param maximumOutPtr pointer to store the maximum pages (64-bit signed, -1 if unlimited)
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryGetMaximum(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final MemorySegment maximumOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(maximumOutPtr, "maximumOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_get_maximum", Integer.class, memoryPtr, storePtr, maximumOutPtr);
  }

  /**
   * Gets memory size in pages using 64-bit return value (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param sizeOutPtr pointer to store the size in pages (64-bit)
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemorySizePages64(
      final MemorySegment memoryPtr, final MemorySegment storePtr, final MemorySegment sizeOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(sizeOutPtr, "sizeOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_size_pages64", Integer.class, memoryPtr, storePtr, sizeOutPtr);
  }

  /**
   * Reads bytes from memory (Panama FFI version).
   *
   * <p>This method uses an eagerly-initialized {@link MethodHandle} for {@code invokeExact}
   * optimization on the hot path, falling back to the generic {@code callNativeFunction} path if
   * the handle is unavailable.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset offset in memory to read from
   * @param length number of bytes to read
   * @param bufferPtr pointer to buffer to read into
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryReadBytes(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long length,
      final MemorySegment bufferPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(bufferPtr, "bufferPtr");

    // Use fast path with invokeExact if available
    final MethodHandle mh = mhPanamaMemoryReadBytes;
    if (mh != null) {
      try {
        return (int) mh.invokeExact(memoryPtr, storePtr, offset, length, bufferPtr);
      } catch (Throwable t) {
        throw new RuntimeException("Native panamaMemoryReadBytes failed", t);
      }
    }
    return callNativeFunction(
        "wasmtime4j_panama_memory_read_bytes",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        length,
        bufferPtr);
  }

  /**
   * Writes bytes to memory (Panama FFI version).
   *
   * <p>This method uses an eagerly-initialized {@link MethodHandle} for {@code invokeExact}
   * optimization on the hot path, falling back to the generic {@code callNativeFunction} path if
   * the handle is unavailable.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset offset in memory to write to
   * @param length number of bytes to write
   * @param bufferPtr pointer to buffer to write from
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryWriteBytes(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long length,
      final MemorySegment bufferPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(bufferPtr, "bufferPtr");

    // Use fast path with invokeExact if available
    final MethodHandle mh = mhPanamaMemoryWriteBytes;
    if (mh != null) {
      try {
        return (int) mh.invokeExact(memoryPtr, storePtr, offset, length, bufferPtr);
      } catch (Throwable t) {
        throw new RuntimeException("Native panamaMemoryWriteBytes failed", t);
      }
    }
    return callNativeFunction(
        "wasmtime4j_panama_memory_write_bytes",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        length,
        bufferPtr);
  }

  /**
   * Gets memory size in bytes (Panama FFI version).
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param sizeOutPtr pointer to store the size in bytes
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemorySizeBytes(
      final MemorySegment memoryPtr, final MemorySegment storePtr, final MemorySegment sizeOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(sizeOutPtr, "sizeOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_size_bytes", Integer.class, memoryPtr, storePtr, sizeOutPtr);
  }

  /**
   * Gets raw pointer to WASM linear memory for zero-copy access.
   *
   * <p>This provides direct access to the WebAssembly linear memory without copying, enabling
   * high-performance memory operations.
   *
   * <p><strong>WARNING:</strong> The returned pointer is only valid while:
   *
   * <ul>
   *   <li>The memory instance is alive
   *   <li>The store is alive
   *   <li>No memory.grow() operations are performed
   * </ul>
   *
   * <p>After memory.grow(), the pointer may be invalidated and must be re-obtained.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param dataPtrOutPtr pointer to store the raw memory data pointer
   * @param sizeOutPtr pointer to store the memory size in bytes
   * @return 0 on success, negative error code on failure
   */
  public int panamaMemoryGetData(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final MemorySegment dataPtrOutPtr,
      final MemorySegment sizeOutPtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(dataPtrOutPtr, "dataPtrOutPtr");
    validatePointer(sizeOutPtr, "sizeOutPtr");

    return callNativeFunction(
        "wasmtime4j_panama_memory_get_data",
        Integer.class,
        memoryPtr,
        storePtr,
        dataPtrOutPtr,
        sizeOutPtr);
  }

  /**
   * Gets the method handle for Panama FFI memory creation (simple version).
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaMemoryCreate() {
    return getMethodHandleByName("wasmtime4j_panama_memory_create").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI memory creation (with configuration).
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaMemoryCreateWithConfig() {
    return getMethodHandleByName("wasmtime4j_panama_memory_create_with_config").orElse(null);
  }

  /**
   * Gets the method handle for async memory creation with config (async resource limiter path).
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaMemoryCreateAsync() {
    return getMethodHandleByName("wasmtime4j_panama_memory_create_async").orElse(null);
  }

  /**
   * Destroys a validated memory instance and unregisters it from the handle registry.
   *
   * <p>This must be called to properly free native memory pointers obtained from memory creation or
   * instance export lookup.
   *
   * @param memoryPtr pointer to the validated memory to destroy
   */
  public void memoryDestroy(final MemorySegment memoryPtr) {
    if (memoryPtr != null && !memoryPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_memory_destroy", Void.class, memoryPtr);
    }
  }

  /**
   * Clears all memory and store handle registries (for testing purposes).
   *
   * <p>This function clears both memory and store handle registries to prevent stale handles from
   * interfering with subsequent tests. Should only be called in test teardown after all handles
   * have been properly destroyed.
   *
   * @return 0 on success, negative error code on failure
   */
  public int memoryClearHandleRegistries() {
    return callNativeFunction("wasmtime4j_panama_memory_clear_handle_registries", Integer.class);
  }

  // ====================================================================================
  // Atomic Memory Operations
  // ====================================================================================

  /**
   * Atomic compare-and-swap on 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param expected expected value
   * @param newValue new value to swap in
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicCompareAndSwapI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int expected,
      final int newValue,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_compare_and_swap_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        expected,
        newValue,
        resultOut);
  }

  /**
   * Atomic compare-and-swap on 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param expected expected value
   * @param newValue new value to swap in
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicCompareAndSwapI64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long expected,
      final long newValue,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_compare_and_swap_i64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        expected,
        newValue,
        resultOut);
  }

  /**
   * Atomic load of 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param resultOut pointer to store the value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicLoadI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_load_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        resultOut);
  }

  /**
   * Atomic load of 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param resultOut pointer to store the value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicLoadI64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_load_i64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        resultOut);
  }

  /**
   * Atomic store of 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to store
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicStoreI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int value) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_store_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value);
  }

  /**
   * Atomic store of 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to store
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicStoreI64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long value) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_store_i64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value);
  }

  /**
   * Atomic add on 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to add
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicAddI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_add_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic add on 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to add
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicAddI64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_add_i64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic AND on 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to AND
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicAndI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_and_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic OR on 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to OR
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicOrI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_or_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic XOR on 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to XOR
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicXorI32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_xor_i32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic AND on 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to AND
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicAndI64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_and_i64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic OR on 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to OR
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicOrI64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_or_i64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic XOR on 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param value value to XOR
   * @param resultOut pointer to store the old value
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicXorI64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long value,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_xor_i64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        value,
        resultOut);
  }

  /**
   * Atomic memory fence.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicFence(final MemorySegment memoryPtr, final MemorySegment storePtr) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_fence", Integer.class, memoryPtr, storePtr);
  }

  /**
   * Atomic notify/wake.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param count number of waiters to wake
   * @param resultOut pointer to store number of waiters woken
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicNotify(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int count,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_notify",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        count,
        resultOut);
  }

  /**
   * Atomic wait on 32-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param expected expected value
   * @param timeoutNanos timeout in nanoseconds (-1 for infinite)
   * @param resultOut pointer to store the wait result (0=ok, 1=not-equal, 2=timed-out)
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicWait32(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final int expected,
      final long timeoutNanos,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_wait32",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        expected,
        timeoutNanos,
        resultOut);
  }

  /**
   * Atomic wait on 64-bit value.
   *
   * @param memoryPtr pointer to the memory
   * @param storePtr pointer to the store
   * @param offset byte offset in memory
   * @param expected expected value
   * @param timeoutNanos timeout in nanoseconds (-1 for infinite)
   * @param resultOut pointer to store the wait result (0=ok, 1=not-equal, 2=timed-out)
   * @return 0 on success, error code otherwise
   */
  public int memoryAtomicWait64(
      final MemorySegment memoryPtr,
      final MemorySegment storePtr,
      final long offset,
      final long expected,
      final long timeoutNanos,
      final MemorySegment resultOut) {
    validatePointer(memoryPtr, "memoryPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(resultOut, "resultOut");
    return callNativeFunction(
        "wasmtime4j_panama_memory_atomic_wait64",
        Integer.class,
        memoryPtr,
        storePtr,
        offset,
        expected,
        timeoutNanos,
        resultOut);
  }

  // ====================================================================================
  // Global Operations
  // ====================================================================================

  /**
   * Creates a new WebAssembly global variable.
   *
   * @param storePtr pointer to the store
   * @param valueType the WebAssembly value type code
   * @param isMutable 1 if mutable, 0 if immutable
   * @param initialValue the initial value for the global
   * @return pointer to the created global, or null on failure
   */
  public MemorySegment globalCreate(
      final MemorySegment storePtr,
      final int valueType,
      final int isMutable,
      final WasmValue initialValue) {
    validatePointer(storePtr, "storePtr");

    // Extract value components for native call
    Object[] valueComponents = extractValueForNative(initialValue);

    return callNativeFunction(
        "wasmtime4j_global_create",
        MemorySegment.class,
        storePtr,
        valueType,
        isMutable,
        valueComponents[0], // i32
        valueComponents[1], // i64
        valueComponents[2], // f32
        valueComponents[3], // f64
        valueComponents[4] // ref
        );
  }

  /**
   * Gets the value of a WebAssembly global.
   *
   * @param globalPtr pointer to the global
   * @param valueOutPtr pointer to store the global value
   */
  public void globalGet(final MemorySegment globalPtr, final MemorySegment valueOutPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(valueOutPtr, "valueOutPtr");
    callNativeFunction("wasmtime_global_get", Void.class, globalPtr, valueOutPtr);
  }

  /**
   * Sets the value of a WebAssembly global.
   *
   * @param globalPtr pointer to the global
   * @param valuePtr pointer to the new value
   */
  public void globalSet(final MemorySegment globalPtr, final MemorySegment valuePtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(valuePtr, "valuePtr");
    callNativeFunction("wasmtime_global_set", Void.class, globalPtr, valuePtr);
  }

  /**
   * Gets the type of a WebAssembly global.
   *
   * @param globalPtr pointer to the global
   * @return pointer to the global type
   */
  public MemorySegment globalType(final MemorySegment globalPtr) {
    validatePointer(globalPtr, "globalPtr");
    return callNativeFunction("wasmtime_global_type", MemorySegment.class, globalPtr);
  }

  /**
   * Creates a mutable WebAssembly global.
   *
   * @param storePtr pointer to the store
   * @param valueType the WebAssembly value type
   * @param initialValuePtr pointer to the initial value
   * @param globalOutPtr pointer to store the created global
   * @return 0 on success, negative error code on failure
   */
  public int globalCreateMutable(
      final MemorySegment storePtr,
      final int valueType,
      final MemorySegment initialValuePtr,
      final MemorySegment globalOutPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(initialValuePtr, "initialValuePtr");
    validatePointer(globalOutPtr, "globalOutPtr");

    return callNativeFunction(
        "wasmtime4j_global_create_mutable",
        Integer.class,
        storePtr,
        valueType,
        initialValuePtr,
        globalOutPtr);
  }

  /**
   * Creates an immutable WebAssembly global.
   *
   * @param storePtr pointer to the store
   * @param valueType the WebAssembly value type
   * @param initialValuePtr pointer to the initial value
   * @param globalOutPtr pointer to store the created global
   * @return 0 on success, negative error code on failure
   */
  public int globalCreateImmutable(
      final MemorySegment storePtr,
      final int valueType,
      final MemorySegment initialValuePtr,
      final MemorySegment globalOutPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(initialValuePtr, "initialValuePtr");
    validatePointer(globalOutPtr, "globalOutPtr");

    return callNativeFunction(
        "wasmtime4j_global_create_immutable",
        Integer.class,
        storePtr,
        valueType,
        initialValuePtr,
        globalOutPtr);
  }

  /**
   * Destroys a WebAssembly global.
   *
   * @param globalPtr pointer to the global to destroy
   */
  public void globalDestroy(final MemorySegment globalPtr) {
    validatePointer(globalPtr, "globalPtr");
    callNativeFunction("wasmtime4j_global_destroy", Void.class, globalPtr);
  }

  /**
   * Gets the value of a WebAssembly global (Panama FFI version).
   *
   * @param globalPtr pointer to the global
   * @param storePtr pointer to the store
   * @param i32ValueOutPtr pointer to store i32 value
   * @param i64ValueOutPtr pointer to store i64 value
   * @param f32ValueOutPtr pointer to store f32 value
   * @param f64ValueOutPtr pointer to store f64 value
   * @param refIdPresentOutPtr pointer to store reference presence flag
   * @param refIdOutPtr pointer to store reference ID
   * @param v128BytesOutPtr pointer to store V128 bytes (16 bytes, can be null)
   * @return 0 on success, negative error code on failure
   */
  public int panamaGlobalGet(
      final MemorySegment globalPtr,
      final MemorySegment storePtr,
      final MemorySegment i32ValueOutPtr,
      final MemorySegment i64ValueOutPtr,
      final MemorySegment f32ValueOutPtr,
      final MemorySegment f64ValueOutPtr,
      final MemorySegment refIdPresentOutPtr,
      final MemorySegment refIdOutPtr,
      final MemorySegment v128BytesOutPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(i32ValueOutPtr, "i32ValueOutPtr");
    validatePointer(i64ValueOutPtr, "i64ValueOutPtr");
    validatePointer(f32ValueOutPtr, "f32ValueOutPtr");
    validatePointer(f64ValueOutPtr, "f64ValueOutPtr");
    validatePointer(refIdPresentOutPtr, "refIdPresentOutPtr");
    validatePointer(refIdOutPtr, "refIdOutPtr");
    // v128BytesOutPtr can be null

    return callNativeFunction(
        "wasmtime4j_panama_global_get",
        Integer.class,
        globalPtr,
        storePtr,
        i32ValueOutPtr,
        i64ValueOutPtr,
        f32ValueOutPtr,
        f64ValueOutPtr,
        refIdPresentOutPtr,
        refIdOutPtr,
        v128BytesOutPtr == null ? MemorySegment.NULL : v128BytesOutPtr);
  }

  /**
   * Sets the value of a WebAssembly global (Panama FFI version).
   *
   * @param globalPtr pointer to the global
   * @param storePtr pointer to the store
   * @param valueType the value type (0=I32, 1=I64, 2=F32, 3=F64, 4=V128, 5=FuncRef, 6=ExternRef)
   * @param i32Value i32 value
   * @param i64Value i64 value
   * @param f32Value f32 value
   * @param f64Value f64 value
   * @param refIdPresent reference presence flag
   * @param refId reference ID
   * @param v128BytesPtr pointer to V128 bytes (16 bytes, can be null)
   * @return 0 on success, negative error code on failure
   */
  public int panamaGlobalSet(
      final MemorySegment globalPtr,
      final MemorySegment storePtr,
      final int valueType,
      final int i32Value,
      final long i64Value,
      final double f32Value,
      final double f64Value,
      final int refIdPresent,
      final long refId,
      final MemorySegment v128BytesPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(storePtr, "storePtr");
    // v128BytesPtr can be null

    return callNativeFunction(
        "wasmtime4j_panama_global_set",
        Integer.class,
        globalPtr,
        storePtr,
        valueType,
        i32Value,
        i64Value,
        f32Value,
        f64Value,
        refIdPresent,
        refId,
        v128BytesPtr == null ? MemorySegment.NULL : v128BytesPtr);
  }

  /**
   * Sets a WebAssembly global value using WasmValue (Panama FFI version).
   *
   * @param globalPtr pointer to the global
   * @param storePtr pointer to the store
   * @param value the value to set
   * @return 0 on success, negative error code on failure
   */
  public int panamaGlobalSet(
      final MemorySegment globalPtr, final MemorySegment storePtr, final WasmValue value) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(storePtr, "storePtr");

    // Extract value components
    int i32Value = 0;
    long i64Value = 0;
    double f32Value = 0.0;
    double f64Value = 0.0;
    int refIdPresent = 0;
    long refId = 0;
    MemorySegment v128BytesPtr = null;

    switch (value.getType()) {
      case I32:
        i32Value = value.asInt();
        break;
      case I64:
        i64Value = value.asLong();
        break;
      case F32:
        f32Value = value.asFloat();
        break;
      case F64:
        f64Value = value.asDouble();
        break;
      case V128:
        final byte[] v128Bytes = value.asV128();
        if (v128Bytes != null && v128Bytes.length == 16) {
          v128BytesPtr = Arena.global().allocate(16);
          MemorySegment.copy(v128Bytes, 0, v128BytesPtr, ValueLayout.JAVA_BYTE, 0, 16);
        }
        break;
      case FUNCREF:
        final Object funcRef = value.asFuncref();
        if (funcRef != null) {
          refIdPresent = 1;
          if (funcRef instanceof FunctionReference) {
            refId = ((FunctionReference) funcRef).getId();
          } else if (funcRef instanceof Long) {
            refId = (Long) funcRef;
          } else {
            throw new IllegalArgumentException(
                "FUNCREF value must be FunctionReference or Long, got: " + funcRef.getClass());
          }
        }
        break;
      case EXTERNREF:
        final Object externRef = value.asExternref();
        if (externRef != null) {
          refIdPresent = 1;
          if (externRef instanceof ExternRef) {
            refId = ((ExternRef<?>) externRef).getId();
          } else if (externRef instanceof Long) {
            refId = (Long) externRef;
          } else {
            throw new IllegalArgumentException(
                "EXTERNREF value must be ExternRef or Long, got: " + externRef.getClass());
          }
        }
        break;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + value.getType());
    }

    return panamaGlobalSet(
        globalPtr,
        storePtr,
        value.getType().toNativeTypeCode(),
        i32Value,
        i64Value,
        f32Value,
        f64Value,
        refIdPresent,
        refId,
        v128BytesPtr);
  }

  /**
   * Gets type information for a WebAssembly global.
   *
   * @param globalPtr pointer to the global
   * @param typeInfoOutPtr pointer to store the type information
   * @param mutabilityOutPtr pointer to store the mutability flag
   * @return 0 on success, negative error code on failure
   */
  public int globalGetTypeInfo(
      final MemorySegment globalPtr,
      final MemorySegment typeInfoOutPtr,
      final MemorySegment mutabilityOutPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(typeInfoOutPtr, "typeInfoOutPtr");
    validatePointer(mutabilityOutPtr, "mutabilityOutPtr");

    return callNativeFunction(
        "wasmtime4j_global_get_type_info",
        Integer.class,
        globalPtr,
        typeInfoOutPtr,
        mutabilityOutPtr);
  }

  /**
   * Creates a new WebAssembly global with a WasmValue (Panama FFI version).
   *
   * @param storePtr pointer to the store
   * @param valueType the value type (0=I32, 1=I64, 2=F32, 3=F64, 4=V128, 5=FuncRef, 6=ExternRef)
   * @param mutability mutability flag (0=const, 1=var)
   * @param value the initial value
   * @param namePtr optional name pointer (can be null)
   * @param globalPtrPtr pointer to store the created global pointer
   * @return 0 on success, negative error code on failure
   */
  public int panamaGlobalCreate(
      final MemorySegment storePtr,
      final int valueType,
      final int mutability,
      final WasmValue value,
      final MemorySegment namePtr,
      final MemorySegment globalPtrPtr) {
    validatePointer(storePtr, "storePtr");
    validatePointer(globalPtrPtr, "globalPtrPtr");

    // Extract value components
    int i32Value = 0;
    long i64Value = 0;
    double f32Value = 0.0;
    double f64Value = 0.0;
    int refIdPresent = 0;
    long refId = 0;
    MemorySegment v128BytesPtr = MemorySegment.NULL;

    switch (value.getType()) {
      case I32:
        i32Value = value.asInt();
        break;
      case I64:
        i64Value = value.asLong();
        break;
      case F32:
        f32Value = value.asFloat();
        break;
      case F64:
        f64Value = value.asDouble();
        break;
      case V128:
        final byte[] v128Bytes = value.asV128();
        if (v128Bytes != null && v128Bytes.length == 16) {
          v128BytesPtr = Arena.global().allocate(16);
          MemorySegment.copy(v128Bytes, 0, v128BytesPtr, ValueLayout.JAVA_BYTE, 0, 16);
        }
        break;
      case FUNCREF:
        final Object funcRef = value.asFuncref();
        if (funcRef != null) {
          refIdPresent = 1;
          if (funcRef instanceof FunctionReference) {
            refId = ((FunctionReference) funcRef).getId();
          } else if (funcRef instanceof Long) {
            refId = (Long) funcRef;
          } else {
            throw new IllegalArgumentException(
                "FUNCREF value must be FunctionReference or Long, got: " + funcRef.getClass());
          }
        }
        break;
      case EXTERNREF:
        final Object externRef = value.asExternref();
        if (externRef != null) {
          refIdPresent = 1;
          if (externRef instanceof ExternRef) {
            refId = ((ExternRef<?>) externRef).getId();
          } else if (externRef instanceof Long) {
            refId = (Long) externRef;
          } else {
            throw new IllegalArgumentException(
                "EXTERNREF value must be ExternRef or Long, got: " + externRef.getClass());
          }
        }
        break;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + value.getType());
    }

    return callNativeFunction(
        "wasmtime4j_panama_global_create",
        Integer.class,
        storePtr,
        valueType,
        mutability,
        i32Value,
        i64Value,
        f32Value,
        f64Value,
        refIdPresent,
        refId,
        v128BytesPtr,
        namePtr == null ? MemorySegment.NULL : namePtr,
        globalPtrPtr);
  }

  /**
   * Gets metadata for a WebAssembly global (Panama FFI version).
   *
   * @param globalPtr pointer to the global
   * @param valueTypePtr pointer to store the value type code
   * @param mutabilityPtr pointer to store the mutability flag
   * @return 0 on success, negative error code on failure
   */
  public int panamaGlobalMetadata(
      final MemorySegment globalPtr,
      final MemorySegment valueTypePtr,
      final MemorySegment mutabilityPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(valueTypePtr, "valueTypePtr");
    validatePointer(mutabilityPtr, "mutabilityPtr");

    return callNativeFunction(
        "wasmtime4j_panama_global_metadata",
        Integer.class,
        globalPtr,
        valueTypePtr,
        mutabilityPtr,
        MemorySegment.NULL); // name_ptr - not needed for PanamaGlobal
  }

  /**
   * Destroys a WebAssembly global (Panama FFI version).
   *
   * @param globalPtr pointer to the global
   */
  public void panamaGlobalDestroy(final MemorySegment globalPtr) {
    validatePointer(globalPtr, "globalPtr");
    callNativeFunction("wasmtime4j_panama_global_destroy", Void.class, globalPtr);
  }

  /**
   * Registers a global for cross-module sharing.
   *
   * @param globalPtr pointer to the global
   * @param namePtr pointer to the global name string
   * @param registryPtr pointer to the global registry
   * @return 0 on success, negative error code on failure
   */
  public int globalRegisterShared(
      final MemorySegment globalPtr, final MemorySegment namePtr, final MemorySegment registryPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(namePtr, "namePtr");
    validatePointer(registryPtr, "registryPtr");

    return callNativeFunction(
        "wasmtime4j_global_register_shared", Integer.class, globalPtr, namePtr, registryPtr);
  }

  /**
   * Looks up a shared global by name.
   *
   * @param namePtr pointer to the global name string
   * @param registryPtr pointer to the global registry
   * @return pointer to the global, or null if not found
   */
  public MemorySegment globalLookupShared(
      final MemorySegment namePtr, final MemorySegment registryPtr) {
    validatePointer(namePtr, "namePtr");
    validatePointer(registryPtr, "registryPtr");

    return callNativeFunction(
        "wasmtime4j_global_lookup_shared", MemorySegment.class, namePtr, registryPtr);
  }

  /**
   * Gets direct access to a global's value for zero-copy operations.
   *
   * @param globalPtr pointer to the global
   * @return pointer to direct value access, or null if not supported
   */
  public MemorySegment globalGetDirectAccess(final MemorySegment globalPtr) {
    validatePointer(globalPtr, "globalPtr");
    return callNativeFunction(
        "wasmtime4j_global_get_direct_access", MemorySegment.class, globalPtr);
  }

  /**
   * Releases direct access to a global's value.
   *
   * @param globalPtr pointer to the global
   * @param directPtr pointer to the direct access handle
   */
  public void globalReleaseDirectAccess(
      final MemorySegment globalPtr, final MemorySegment directPtr) {
    validatePointer(globalPtr, "globalPtr");
    validatePointer(directPtr, "directPtr");
    callNativeFunction("wasmtime4j_global_release_direct_access", Void.class, globalPtr, directPtr);
  }

  /**
   * Gets the method handle for Panama FFI global creation.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaGlobalCreate() {
    return getMethodHandleByName("wasmtime4j_panama_global_create").orElse(null);
  }

  // ====================================================================================
  // Table Operations
  // ====================================================================================

  /**
   * Gets the method handle for table size query.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableSize() {
    return getMethodHandleByName("wasmtime4j_table_size").orElse(null);
  }

  /**
   * Gets the method handle for table element get.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableGet() {
    return getMethodHandleByName("wasmtime4j_table_get").orElse(null);
  }

  /**
   * Gets the method handle for table element set.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableSet() {
    return getMethodHandleByName("wasmtime4j_table_set").orElse(null);
  }

  /**
   * Gets the method handle for table grow.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableGrow() {
    return getMethodHandleByName("wasmtime4j_table_grow").orElse(null);
  }

  /**
   * Gets the method handle for table deletion.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableDelete() {
    return getMethodHandleByName("wasmtime4j_table_destroy").orElse(null);
  }

  /**
   * Gets the method handle for table metadata retrieval.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getTableMetadata() {
    return getMethodHandleByName("wasmtime4j_table_metadata").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table size retrieval.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableSize() {
    return getMethodHandleByName("wasmtime4j_panama_table_size").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table element get.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableGet() {
    return getMethodHandleByName("wasmtime4j_panama_table_get").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table element set.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableSet() {
    return getMethodHandleByName("wasmtime4j_panama_table_set").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table grow.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableGrow() {
    return getMethodHandleByName("wasmtime4j_panama_table_grow").orElse(null);
  }

  /**
   * Grows a table asynchronously through the async resource limiter (Panama FFI version).
   *
   * <p>Requires engine with {@code asyncSupport(true)}.
   *
   * @param tablePtr pointer to the table
   * @param storePtr pointer to the store
   * @param delta number of elements to add
   * @param elementType element type code (5=FUNCREF, 6=EXTERNREF)
   * @param refIdPresent whether a reference ID is provided (0=no, 1=yes)
   * @param refId the reference ID for the init value
   * @param oldSizeOut pointer to store the previous size
   * @return 0 on success, non-zero on failure
   */
  public int panamaTableGrowAsync(
      final MemorySegment tablePtr,
      final MemorySegment storePtr,
      final int delta,
      final int elementType,
      final int refIdPresent,
      final long refId,
      final MemorySegment oldSizeOut) {
    validatePointer(tablePtr, "tablePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(oldSizeOut, "oldSizeOut");

    return callNativeFunction(
        "wasmtime4j_panama_table_grow_async",
        Integer.class,
        tablePtr,
        storePtr,
        delta,
        elementType,
        refIdPresent,
        refId,
        oldSizeOut);
  }

  /**
   * Gets the method handle for Panama FFI table metadata retrieval.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableMetadata() {
    return getMethodHandleByName("wasmtime4j_panama_table_metadata").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table fill.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableFill() {
    return getMethodHandleByName("wasmtime4j_panama_table_fill").orElse(null);
  }

  /**
   * Copies elements within a table (same table).
   *
   * <p>This is equivalent to the WebAssembly table.copy instruction when copying within the same
   * table.
   *
   * @param tablePtr pointer to the table
   * @param storePtr pointer to the store
   * @param dst destination index in the table
   * @param src source index in the table
   * @param count number of elements to copy
   * @return 0 on success, non-zero on error
   */
  public int panamaTableCopy(
      final MemorySegment tablePtr,
      final MemorySegment storePtr,
      final int dst,
      final int src,
      final int count) {
    validatePointer(tablePtr, "tablePtr");
    validatePointer(storePtr, "storePtr");

    return callNativeFunction(
        "wasmtime4j_panama_table_copy", Integer.class, tablePtr, storePtr, dst, src, count);
  }

  /**
   * Copies elements from one table to another.
   *
   * <p>This is equivalent to the WebAssembly table.copy instruction when copying between different
   * tables.
   *
   * @param dstTablePtr pointer to the destination table
   * @param storePtr pointer to the store
   * @param dst destination index in the destination table
   * @param srcTablePtr pointer to the source table
   * @param src source index in the source table
   * @param count number of elements to copy
   * @return 0 on success, non-zero on error
   */
  public int panamaTableCopyFrom(
      final MemorySegment dstTablePtr,
      final MemorySegment storePtr,
      final int dst,
      final MemorySegment srcTablePtr,
      final int src,
      final int count) {
    validatePointer(dstTablePtr, "dstTablePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(srcTablePtr, "srcTablePtr");

    return callNativeFunction(
        "wasmtime4j_panama_table_copy_from",
        Integer.class,
        dstTablePtr,
        storePtr,
        dst,
        srcTablePtr,
        src,
        count);
  }

  /**
   * Gets the method handle for Panama FFI table deletion.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableDelete() {
    return getMethodHandleByName("wasmtime4j_panama_table_destroy").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table creation.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableCreate() {
    return getMethodHandleByName("wasmtime4j_panama_table_create").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI table creation with initial value.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableCreateWithInit() {
    return getMethodHandleByName("wasmtime4j_panama_table_create_with_init").orElse(null);
  }

  /**
   * Gets the method handle for Panama FFI 64-bit table creation.
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableCreate64() {
    return getMethodHandleByName("wasmtime4j_panama_table_create64").orElse(null);
  }

  /**
   * Gets the method handle for async table creation (async resource limiter path).
   *
   * @return the method handle, or null if not available
   */
  public MethodHandle getPanamaTableCreateAsync() {
    return getMethodHandleByName("wasmtime4j_panama_table_create_async").orElse(null);
  }

  /**
   * Initializes a table from an element segment (table.init instruction).
   *
   * @param tablePtr pointer to the table
   * @param storePtr pointer to the store
   * @param instancePtr pointer to the instance
   * @param destIndex destination index in the table
   * @param srcIndex source index in the element segment
   * @param count number of elements to copy
   * @param segmentIndex element segment index
   * @return 0 on success, non-zero on error
   */
  public int panamaTableInit(
      final MemorySegment tablePtr,
      final MemorySegment storePtr,
      final MemorySegment instancePtr,
      final int destIndex,
      final int srcIndex,
      final int count,
      final int segmentIndex) {
    validatePointer(tablePtr, "tablePtr");
    validatePointer(storePtr, "storePtr");
    validatePointer(instancePtr, "instancePtr");

    return callNativeFunction(
        "wasmtime4j_panama_table_init",
        Integer.class,
        tablePtr,
        storePtr,
        instancePtr,
        destIndex,
        srcIndex,
        count,
        segmentIndex);
  }

  /**
   * Drops an element segment (elem.drop instruction).
   *
   * @param instancePtr pointer to the instance
   * @param segmentIndex element segment index to drop
   * @return 0 on success, non-zero on error
   */
  public int elemDrop(final MemorySegment instancePtr, final int segmentIndex) {
    validatePointer(instancePtr, "instancePtr");

    return callNativeFunction(
        "wasmtime4j_panama_elem_drop", Integer.class, instancePtr, segmentIndex);
  }

  /**
   * Checks if a table supports 64-bit addressing (Table64).
   *
   * @param tablePtr pointer to the table
   * @param storePtr pointer to the store
   * @return true if the table supports 64-bit addressing
   */
  public boolean tableSupports64BitAddressing(
      final MemorySegment tablePtr, final MemorySegment storePtr) {
    final int result =
        callNativeFunction(
            "wasmtime4j_panama_table_supports_64bit_addressing", Integer.class, tablePtr, storePtr);
    return result == 1;
  }

  // ====================================================================================
  // Error Handling Functions
  // ====================================================================================

  /**
   * Gets the last error message from the native library.
   *
   * @return pointer to error message string, or null if no error
   */
  public MemorySegment getLastErrorMessage() {
    return callNativeFunction("wasmtime4j_panama_get_last_error_message", MemorySegment.class);
  }

  /**
   * Frees an error message returned by getLastErrorMessage.
   *
   * @param messagePtr pointer to the error message to free
   */
  public void freeErrorMessage(final MemorySegment messagePtr) {
    if (messagePtr != null && !messagePtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_free_error_message", Void.class, messagePtr);
    }
  }

  /**
   * Frees a C string allocated by Rust.
   *
   * @param stringPtr pointer to the C string to free
   */
  public void freeString(final MemorySegment stringPtr) {
    if (stringPtr != null && !stringPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_free_string", Void.class, stringPtr);
    }
  }

  /** Clears any stored error state in the native library. */
  public void clearErrorState() {
    callNativeFunction("wasmtime4j_clear_error_state", Void.class);
  }

  // ====================================================================================
  // Helper Methods
  // ====================================================================================

  /**
   * Gets a cached method handle for a native function by name.
   *
   * @param functionName the name of the function
   * @return optional containing the method handle, or empty if not found
   */
  private Optional<MethodHandle> getMethodHandleByName(final String functionName) {
    FunctionBinding binding = getFunctionBinding(functionName);
    if (binding == null) {
      LOGGER.warning("Unknown function binding: " + functionName);
      return Optional.empty();
    }
    return binding.getMethodHandle();
  }
}

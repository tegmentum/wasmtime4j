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
import java.lang.foreign.ValueLayout;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Centralized repository of function descriptors for Wasmtime FFI calls.
 *
 * <p>This class provides type-safe function descriptors for all Wasmtime C API functions used by
 * the Panama implementation. Function descriptors define the parameter types, return types, and
 * calling conventions for native functions.
 *
 * <p>Descriptors are cached for performance and reuse across multiple FFI calls.
 *
 * @since 1.0.0
 */
public final class FunctionDescriptors {

  // Cache for function descriptors
  private static final ConcurrentMap<String, FunctionDescriptor> DESCRIPTOR_CACHE =
      new ConcurrentHashMap<>();

  // Prevent instantiation
  private FunctionDescriptors() {
    throw new UnsupportedOperationException("Utility class");
  }

  // Engine function descriptors

  /**
   * Function descriptor for wasmtime_engine_new().
   *
   * <p>Signature: {@code wasmtime_engine_t* wasmtime_engine_new(void)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeEngineNew() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_engine_new", key -> FunctionDescriptor.of(ValueLayout.ADDRESS));
  }

  /**
   * Function descriptor for wasmtime_engine_delete().
   *
   * <p>Signature: {@code void wasmtime_engine_delete(wasmtime_engine_t* engine)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeEngineDelete() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_engine_delete", key -> FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
  }

  // Module function descriptors

  /**
   * Function descriptor for wasmtime_module_new().
   *
   * <p>Signature: {@code wasmtime_module_t* wasmtime_module_new(wasmtime_engine_t* engine, const
   * uint8_t* wasm, size_t wasm_len)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeModuleNew() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_module_new",
        key ->
            FunctionDescriptor.of(
                ValueLayout.ADDRESS, // return: module pointer
                ValueLayout.ADDRESS, // engine pointer
                ValueLayout.ADDRESS, // wasm data pointer
                ValueLayout.JAVA_LONG // wasm data length
                ));
  }

  /**
   * Function descriptor for wasmtime_module_delete().
   *
   * <p>Signature: {@code void wasmtime_module_delete(wasmtime_module_t* module)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeModuleDelete() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_module_delete", key -> FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
  }

  /**
   * Function descriptor for wasmtime_module_serialize().
   *
   * <p>Signature: {@code size_t wasmtime_module_serialize(wasmtime_module_t* module, uint8_t*
   * buffer, size_t buffer_len)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeModuleSerialize() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_module_serialize",
        key ->
            FunctionDescriptor.of(
                ValueLayout.JAVA_LONG, // return: serialized size
                ValueLayout.ADDRESS, // module pointer
                ValueLayout.ADDRESS, // buffer pointer
                ValueLayout.JAVA_LONG // buffer length
                ));
  }

  // Instance function descriptors

  /**
   * Function descriptor for wasmtime_instance_new().
   *
   * <p>Signature: {@code wasmtime_instance_t* wasmtime_instance_new(wasmtime_module_t* module,
   * const wasmtime_extern_t* imports, size_t imports_len)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeInstanceNew() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_instance_new",
        key ->
            FunctionDescriptor.of(
                ValueLayout.ADDRESS, // return: instance pointer
                ValueLayout.ADDRESS, // module pointer
                ValueLayout.ADDRESS, // imports array pointer
                ValueLayout.JAVA_LONG // imports array length
                ));
  }

  /**
   * Function descriptor for wasmtime_instance_delete().
   *
   * <p>Signature: {@code void wasmtime_instance_delete(wasmtime_instance_t* instance)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeInstanceDelete() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_instance_delete", key -> FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
  }

  /**
   * Function descriptor for wasmtime_instance_export_get().
   *
   * <p>Signature: {@code bool wasmtime_instance_export_get(wasmtime_instance_t* instance, const
   * char* name, wasmtime_extern_t* out)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeInstanceExportGet() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_instance_export_get",
        key ->
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN, // return: found
                ValueLayout.ADDRESS, // instance pointer
                ValueLayout.ADDRESS, // name string
                ValueLayout.ADDRESS // output extern pointer
                ));
  }

  // Memory function descriptors

  /**
   * Function descriptor for wasmtime_memory_new().
   *
   * <p>Signature: {@code wasmtime_memory_t* wasmtime_memory_new(uint32_t initial, uint32_t
   * maximum)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeMemoryNew() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_memory_new",
        key ->
            FunctionDescriptor.of(
                ValueLayout.ADDRESS, // return: memory pointer
                ValueLayout.JAVA_INT, // initial pages
                ValueLayout.JAVA_INT // maximum pages
                ));
  }

  /**
   * Function descriptor for wasmtime_memory_size().
   *
   * <p>Signature: {@code uint32_t wasmtime_memory_size(wasmtime_memory_t* memory)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeMemorySize() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_memory_size",
        key ->
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT, // return: size in pages
                ValueLayout.ADDRESS // memory pointer
                ));
  }

  /**
   * Function descriptor for wasmtime_memory_grow().
   *
   * <p>Signature: {@code bool wasmtime_memory_grow(wasmtime_memory_t* memory, uint32_t pages)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeMemoryGrow() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_memory_grow",
        key ->
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN, // return: success
                ValueLayout.ADDRESS, // memory pointer
                ValueLayout.JAVA_INT // pages to grow
                ));
  }

  /**
   * Function descriptor for wasmtime_memory_data().
   *
   * <p>Signature: {@code uint8_t* wasmtime_memory_data(wasmtime_memory_t* memory)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeMemoryData() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_memory_data",
        key ->
            FunctionDescriptor.of(
                ValueLayout.ADDRESS, // return: data pointer
                ValueLayout.ADDRESS // memory pointer
                ));
  }

  // Function function descriptors

  /**
   * Function descriptor for wasmtime_func_call().
   *
   * <p>Signature: {@code wasmtime_error_t* wasmtime_func_call(wasmtime_func_t* func, const
   * wasmtime_val_t* args, size_t args_len, wasmtime_val_t* results, size_t results_len)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeFuncCall() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_func_call",
        key ->
            FunctionDescriptor.of(
                ValueLayout.ADDRESS, // return: error pointer (null if success)
                ValueLayout.ADDRESS, // function pointer
                ValueLayout.ADDRESS, // args array pointer
                ValueLayout.JAVA_LONG, // args array length
                ValueLayout.ADDRESS, // results array pointer
                ValueLayout.JAVA_LONG // results array length
                ));
  }

  // Global function descriptors

  /**
   * Function descriptor for wasmtime_global_get().
   *
   * <p>Signature: {@code void wasmtime_global_get(wasmtime_global_t* global, wasmtime_val_t* out)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeGlobalGet() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_global_get",
        key ->
            FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS, // global pointer
                ValueLayout.ADDRESS // output value pointer
                ));
  }

  /**
   * Function descriptor for wasmtime_global_set().
   *
   * <p>Signature: {@code void wasmtime_global_set(wasmtime_global_t* global, const wasmtime_val_t*
   * val)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeGlobalSet() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_global_set",
        key ->
            FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS, // global pointer
                ValueLayout.ADDRESS // value pointer
                ));
  }

  // Table function descriptors

  /**
   * Function descriptor for wasmtime_table_size().
   *
   * <p>Signature: {@code uint32_t wasmtime_table_size(wasmtime_table_t* table)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeTableSize() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_table_size",
        key ->
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT, // return: table size
                ValueLayout.ADDRESS // table pointer
                ));
  }

  /**
   * Function descriptor for wasmtime_table_get().
   *
   * <p>Signature: {@code bool wasmtime_table_get(wasmtime_table_t* table, uint32_t index,
   * wasmtime_val_t* out)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeTableGet() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_table_get",
        key ->
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN, // return: success
                ValueLayout.ADDRESS, // table pointer
                ValueLayout.JAVA_INT, // index
                ValueLayout.ADDRESS // output value pointer
                ));
  }

  /**
   * Function descriptor for wasmtime_table_set().
   *
   * <p>Signature: {@code bool wasmtime_table_set(wasmtime_table_t* table, uint32_t index, const
   * wasmtime_val_t* val)}
   *
   * @return the function descriptor
   */
  public static FunctionDescriptor wasmtimeTableSet() {
    return DESCRIPTOR_CACHE.computeIfAbsent(
        "wasmtime_table_set",
        key ->
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN, // return: success
                ValueLayout.ADDRESS, // table pointer
                ValueLayout.JAVA_INT, // index
                ValueLayout.ADDRESS // value pointer
                ));
  }

  /**
   * Gets the number of cached function descriptors.
   *
   * @return the cache size
   */
  public static int getCacheSize() {
    return DESCRIPTOR_CACHE.size();
  }

  /**
   * Clears the function descriptor cache.
   *
   * <p>This method is primarily intended for testing purposes.
   */
  public static void clearCache() {
    DESCRIPTOR_CACHE.clear();
  }
}

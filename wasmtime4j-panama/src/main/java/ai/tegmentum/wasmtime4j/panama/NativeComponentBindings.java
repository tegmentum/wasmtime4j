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
    // ===== Component Engine (legacy API) =====
    addFunctionBinding(
        "wasmtime4j_component_engine_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_engine_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_load_from_bytes",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return component_ptr
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // wasm_bytes

    addFunctionBinding(
        "wasmtime4j_component_instantiate",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return instance_ptr
            ValueLayout.ADDRESS, // engine_ptr
            ValueLayout.ADDRESS)); // component_ptr

    addFunctionBinding(
        "wasmtime4j_component_engine_active_instances",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return count
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_component_engine_cleanup",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return count
            ValueLayout.ADDRESS)); // engine_ptr

    addFunctionBinding(
        "wasmtime4j_component_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // return size
            ValueLayout.ADDRESS)); // component_ptr

    addFunctionBinding(
        "wasmtime4j_component_exports_interface",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // component_ptr
            ValueLayout.ADDRESS)); // interface_name

    addFunctionBinding(
        "wasmtime4j_component_imports_interface",
        FunctionDescriptor.of(
            ValueLayout.JAVA_BOOLEAN, // return success
            ValueLayout.ADDRESS, // component_ptr
            ValueLayout.ADDRESS)); // interface_name

    addFunctionBinding(
        "wasmtime4j_component_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // component_ptr

    addFunctionBinding(
        "wasmtime4j_component_instance_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // instance_ptr

    // ===== Component Model (structured API with error codes) =====
    addFunctionBinding(
        "wasmtime4j_component_engine_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)); // config

    addFunctionBinding(
        "wasmtime4j_component_load_from_bytes",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // return component handle
            ValueLayout.ADDRESS, // engineHandle
            ValueLayout.ADDRESS, // bytes
            ValueLayout.JAVA_LONG)); // length

    addFunctionBinding(
        "wasmtime4j_component_instantiate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code (0 = success)
            ValueLayout.ADDRESS, // engineHandle
            ValueLayout.ADDRESS, // componentHandle
            ValueLayout.ADDRESS)); // instanceOut (pointer to pointer)

    addFunctionBinding(
        "wasmtime4j_component_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_instance_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_get_size",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

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
        "wasmtime4j_component_has_export",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_has_import",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_component_validate",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

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

    // ===== Panama Component Functions =====
    addFunctionBinding(
        "wasmtime4j_panama_component_get_exported_functions",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // instance handle
            ValueLayout.ADDRESS, // functions out (pointer to pointer array)
            ValueLayout.ADDRESS)); // count out

    addFunctionBinding(
        "wasmtime4j_panama_component_invoke",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return error code
            ValueLayout.ADDRESS, // instance handle
            ValueLayout.ADDRESS, // function name
            ValueLayout.ADDRESS, // params pointer
            ValueLayout.JAVA_INT, // params count
            ValueLayout.ADDRESS, // results out
            ValueLayout.ADDRESS)); // results count out

    addFunctionBinding(
        "wasmtime4j_panama_component_free_string_array",
        FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // strings pointer
            ValueLayout.JAVA_INT)); // count

    // ===== Panama Component Engine =====
    addFunctionBinding(
        "wasmtime4j_panama_component_engine_create", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_load_from_bytes",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_component_instantiate",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_get_size",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_exports_interface",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_imports_interface",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_get_active_instances_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_cleanup_instances",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_free_wit_values",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_component_engine_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_instance_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // ===== Panama Component Orchestrator and Resource Manager =====
    addFunctionBinding(
        "wasmtime4j_panama_component_orchestrator_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_resource_manager_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_resource_manager_create_resource",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_distributed_component_manager_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

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
        "wasmtime4j_panama_enhanced_component_engine_same",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: 1=same, 0=different, -1=error
            ValueLayout.ADDRESS, // engine_ptr1
            ValueLayout.ADDRESS)); // engine_ptr2

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

    // ===== Component Metrics =====
    addFunctionBinding(
        "wasmtime4j_panama_enhanced_component_get_metrics",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_components_loaded",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_instances_created",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_instances_destroyed",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_avg_instantiation_time_nanos",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_peak_memory_usage",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_function_calls",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_get_error_count",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_component_metrics_destroy",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

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

    // ===== WIT Interface Manager =====
    addFunctionBinding(
        "wasmtime4j_panama_wit_interface_manager_create",
        FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wit_interface_manager_register",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    // ===== Component Linker =====
    addFunctionBinding(
        "wasmtime4j_component_linker_new",
        FunctionDescriptor.of(
            ValueLayout.ADDRESS, // returns linker pointer
            ValueLayout.ADDRESS)); // engine pointer

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
        "wasmtime4j_component_linker_dispose",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_has_interface",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 1 if present, 0 if not, -1 on error
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // namespace string
            ValueLayout.ADDRESS)); // interface name string

    addFunctionBinding(
        "wasmtime4j_component_linker_has_function",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 1 if present, 0 if not, -1 on error
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // namespace string
            ValueLayout.ADDRESS, // interface name string
            ValueLayout.ADDRESS)); // function name string

    addFunctionBinding(
        "wasmtime4j_component_linker_host_function_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // returns count
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_interface_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG, // returns count
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_wasi_p2_enabled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 1 if enabled, 0 if not
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_enable_wasi_p2",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS)); // linker pointer

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
            ValueLayout.JAVA_INT)); // read only flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_allow_network",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // allow flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_allow_clock",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // allow flag

    addFunctionBinding(
        "wasmtime4j_component_linker_set_wasi_allow_random",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.JAVA_INT)); // allow flag

    addFunctionBinding(
        "wasmtime4j_component_linker_enable_wasi_http",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_wasi_http_enabled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns 1 if enabled, 0 if not, negative on error
            ValueLayout.ADDRESS)); // linker pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_instantiate",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // component pointer
            ValueLayout.ADDRESS)); // instance out pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_get_interfaces",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS)); // json out pointer

    addFunctionBinding(
        "wasmtime4j_component_linker_get_functions",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // returns result code
            ValueLayout.ADDRESS, // linker pointer
            ValueLayout.ADDRESS, // namespace string
            ValueLayout.ADDRESS, // interface name string
            ValueLayout.ADDRESS)); // json out pointer
  }

  // ===== Component Engine (legacy API) =====

  /**
   * Creates a component engine.
   *
   * @return memory segment pointer to the component engine, or null on failure
   */
  public MemorySegment createComponentEngine() {
    return callNativeFunction("wasmtime4j_component_engine_create", MemorySegment.class);
  }

  /**
   * Loads a component from bytes.
   *
   * @param enginePtr pointer to the component engine
   * @param wasmBytes pointer to the WASM component bytes
   * @return memory segment pointer to the component, or null on failure
   */
  public MemorySegment loadComponentFromBytes(
      final MemorySegment enginePtr, final MemorySegment wasmBytes) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(wasmBytes, "wasmBytes");
    return callNativeFunction(
        "wasmtime4j_component_load_from_bytes", MemorySegment.class, enginePtr, wasmBytes);
  }

  /**
   * Instantiates a component.
   *
   * @param enginePtr pointer to the component engine
   * @param componentPtr pointer to the component
   * @return memory segment pointer to the component instance, or null on failure
   */
  public MemorySegment instantiateComponent(
      final MemorySegment enginePtr, final MemorySegment componentPtr) {
    validatePointer(enginePtr, "enginePtr");
    validatePointer(componentPtr, "componentPtr");
    return callNativeFunction(
        "wasmtime4j_component_instantiate", MemorySegment.class, enginePtr, componentPtr);
  }

  /**
   * Gets the active instances count for a component engine.
   *
   * @param enginePtr pointer to the component engine
   * @return number of active instances
   */
  public int getActiveInstancesCount(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction(
        "wasmtime4j_component_engine_active_instances", Integer.class, enginePtr);
  }

  /**
   * Cleans up inactive instances.
   *
   * @param enginePtr pointer to the component engine
   * @return number of instances cleaned up
   */
  public int cleanupInstances(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction("wasmtime4j_component_engine_cleanup", Integer.class, enginePtr);
  }

  /**
   * Destroys a component engine.
   *
   * @param enginePtr pointer to the component engine to destroy
   */
  public void destroyComponentEngine(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    callNativeFunction("wasmtime4j_component_engine_destroy", Void.class, enginePtr);
  }

  /**
   * Gets the size of a component.
   *
   * @param componentPtr pointer to the component
   * @return component size in bytes
   */
  public long getComponentSize(final MemorySegment componentPtr) {
    validatePointer(componentPtr, "componentPtr");
    return callNativeFunction("wasmtime4j_component_size", Long.class, componentPtr);
  }

  /**
   * Checks if a component exports a specific interface.
   *
   * @param componentPtr pointer to the component
   * @param interfaceName name of the interface to check
   * @return true if the interface is exported
   */
  public boolean exportsInterface(final MemorySegment componentPtr, final String interfaceName) {
    validatePointer(componentPtr, "componentPtr");
    return callNativeFunction(
        "wasmtime4j_component_exports_interface", Boolean.class, componentPtr, interfaceName);
  }

  /**
   * Checks if a component imports a specific interface.
   *
   * @param componentPtr pointer to the component
   * @param interfaceName name of the interface to check
   * @return true if the interface is imported
   */
  public boolean importsInterface(final MemorySegment componentPtr, final String interfaceName) {
    validatePointer(componentPtr, "componentPtr");
    return callNativeFunction(
        "wasmtime4j_component_imports_interface", Boolean.class, componentPtr, interfaceName);
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

  /**
   * Destroys a component instance.
   *
   * @param instancePtr pointer to the component instance to destroy
   */
  public void destroyComponentInstance(final MemorySegment instancePtr) {
    validatePointer(instancePtr, "instancePtr");
    callNativeFunction("wasmtime4j_component_instance_destroy", Void.class, instancePtr);
  }

  // ===== Component Model (structured API) =====

  /**
   * Creates a new component engine with configuration.
   *
   * @param config the engine configuration
   * @return memory segment pointer to the component engine, or null on failure
   */
  public MemorySegment componentEngineCreate(final MemorySegment config) {
    return callNativeFunction("wasmtime4j_component_engine_create", MemorySegment.class, config);
  }

  /**
   * Destroys a component engine.
   *
   * @param engineHandle pointer to the component engine to destroy
   */
  public void componentEngineDestroy(final MemorySegment engineHandle) {
    validatePointer(engineHandle, "engineHandle");
    callNativeFunction("wasmtime4j_component_engine_destroy", Void.class, engineHandle);
  }

  /**
   * Loads a component from bytecode.
   *
   * @param engineHandle the component engine handle
   * @param bytes the component bytecode
   * @param length the bytecode length
   * @param componentOut pointer to receive the component handle
   * @return 0 on success, non-zero on error
   */
  public int componentLoadFromBytes(
      final MemorySegment engineHandle,
      final MemorySegment bytes,
      final long length,
      final MemorySegment componentOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(bytes, "bytes");
    validatePointer(componentOut, "componentOut");
    return callNativeFunction(
        "wasmtime4j_component_compile", Integer.class, engineHandle, bytes, length, componentOut);
  }

  /**
   * Instantiates a component.
   *
   * @param engineHandle the engine handle
   * @param componentHandle the component handle
   * @param instanceOut pointer to where the instance handle will be written
   * @return error code (0 = success, non-zero = error)
   */
  public int componentInstantiate(
      final MemorySegment engineHandle,
      final MemorySegment componentHandle,
      final MemorySegment instanceOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(componentHandle, "componentHandle");
    validatePointer(instanceOut, "instanceOut");
    return callNativeFunction(
        "wasmtime4j_component_instantiate",
        Integer.class,
        engineHandle,
        componentHandle,
        instanceOut);
  }

  /**
   * Destroys a component.
   *
   * @param componentHandle pointer to the component to destroy
   */
  public void componentDestroy(final MemorySegment componentHandle) {
    validatePointer(componentHandle, "componentHandle");
    callNativeFunction("wasmtime4j_component_destroy", Void.class, componentHandle);
  }

  /**
   * Destroys a component instance.
   *
   * @param instanceHandle pointer to the component instance to destroy
   */
  public void componentInstanceDestroy(final MemorySegment instanceHandle) {
    validatePointer(instanceHandle, "instanceHandle");
    callNativeFunction("wasmtime4j_component_instance_destroy", Void.class, instanceHandle);
  }

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
   * Checks if a component has a specific export.
   *
   * @param componentHandle the component handle
   * @param exportName the export name (C string)
   * @return 1 if has the export, 0 otherwise
   */
  public int componentHasExport(
      final MemorySegment componentHandle, final MemorySegment exportName) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(exportName, "exportName");
    return callNativeFunction(
        "wasmtime4j_component_has_export", Integer.class, componentHandle, exportName);
  }

  /**
   * Checks if a component has a specific import.
   *
   * @param componentHandle the component handle
   * @param importName the import name (C string)
   * @return 1 if has the import, 0 otherwise
   */
  public int componentHasImport(
      final MemorySegment componentHandle, final MemorySegment importName) {
    validatePointer(componentHandle, "componentHandle");
    validatePointer(importName, "importName");
    return callNativeFunction(
        "wasmtime4j_component_has_import", Integer.class, componentHandle, importName);
  }

  /**
   * Validates a component.
   *
   * @param componentHandle the component handle
   * @return 1 if valid, 0 otherwise
   */
  public int componentValidate(final MemorySegment componentHandle) {
    validatePointer(componentHandle, "componentHandle");
    return callNativeFunction("wasmtime4j_component_validate", Integer.class, componentHandle);
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
   * Gets the list of exported function names from a component instance.
   *
   * @param instanceHandle the component instance handle
   * @param functionsOut output parameter for array of function name pointers
   * @param countOut output parameter for count of functions
   * @return 0 on success, non-zero error code on failure
   */
  public int componentGetExportedFunctions(
      final MemorySegment instanceHandle,
      final MemorySegment functionsOut,
      final MemorySegment countOut) {
    validatePointer(instanceHandle, "instanceHandle");
    validatePointer(functionsOut, "functionsOut");
    validatePointer(countOut, "countOut");
    return callNativeFunction(
        "wasmtime4j_panama_component_get_exported_functions",
        Integer.class,
        instanceHandle,
        functionsOut,
        countOut);
  }

  /**
   * Invokes a component function with parameters.
   *
   * @param instanceHandle the component instance handle
   * @param functionName the function name to invoke
   * @param paramsPtr pointer to the parameters array
   * @param paramsCount number of parameters
   * @param resultsOut output parameter for results array
   * @param resultsCountOut output parameter for results count
   * @return 0 on success, non-zero error code on failure
   */
  public int componentInvoke(
      final MemorySegment instanceHandle,
      final MemorySegment functionName,
      final MemorySegment paramsPtr,
      final int paramsCount,
      final MemorySegment resultsOut,
      final MemorySegment resultsCountOut) {
    validatePointer(instanceHandle, "instanceHandle");
    validatePointer(functionName, "functionName");
    validatePointer(resultsOut, "resultsOut");
    validatePointer(resultsCountOut, "resultsCountOut");
    return callNativeFunction(
        "wasmtime4j_panama_component_invoke",
        Integer.class,
        instanceHandle,
        functionName,
        paramsPtr,
        paramsCount,
        resultsOut,
        resultsCountOut);
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
   * Checks if two enhanced component engines share the same underlying Wasmtime engine.
   *
   * @param enginePtr1 pointer to the first enhanced component engine
   * @param enginePtr2 pointer to the second enhanced component engine
   * @return true if both engines share the same underlying engine
   */
  public boolean enhancedComponentEngineSame(
      final MemorySegment enginePtr1, final MemorySegment enginePtr2) {
    final int result =
        callNativeFunction(
            "wasmtime4j_panama_enhanced_component_engine_same",
            Integer.class,
            enginePtr1,
            enginePtr2);
    return result == 1;
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

  // ===== Component Metrics =====

  /**
   * Gets component metrics from an enhanced component engine.
   *
   * @param engineHandle the enhanced component engine handle
   * @param metricsOut output pointer for the metrics handle
   * @return 0 on success, non-zero on error
   */
  public int enhancedComponentGetMetrics(
      final MemorySegment engineHandle, final MemorySegment metricsOut) {
    validatePointer(engineHandle, "engineHandle");
    validatePointer(metricsOut, "metricsOut");
    return callNativeFunction(
        "wasmtime4j_panama_enhanced_component_get_metrics",
        Integer.class,
        engineHandle,
        metricsOut);
  }

  /**
   * Gets the components loaded count from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the count of components loaded
   */
  public long componentMetricsGetComponentsLoaded(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_components_loaded", Long.class, metricsHandle);
  }

  /**
   * Gets the instances created count from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the count of instances created
   */
  public long componentMetricsGetInstancesCreated(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_instances_created", Long.class, metricsHandle);
  }

  /**
   * Gets the instances destroyed count from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the count of instances destroyed
   */
  public long componentMetricsGetInstancesDestroyed(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_instances_destroyed", Long.class, metricsHandle);
  }

  /**
   * Gets the average instantiation time in nanoseconds from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the average instantiation time in nanoseconds
   */
  public long componentMetricsGetAvgInstantiationTimeNanos(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_avg_instantiation_time_nanos",
        Long.class,
        metricsHandle);
  }

  /**
   * Gets the peak memory usage from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the peak memory usage in bytes
   */
  public long componentMetricsGetPeakMemoryUsage(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_peak_memory_usage", Long.class, metricsHandle);
  }

  /**
   * Gets the function calls count from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the count of function calls
   */
  public long componentMetricsGetFunctionCalls(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_function_calls", Long.class, metricsHandle);
  }

  /**
   * Gets the error count from component metrics.
   *
   * @param metricsHandle the component metrics handle
   * @return the error count
   */
  public long componentMetricsGetErrorCount(final MemorySegment metricsHandle) {
    if (metricsHandle == null || metricsHandle.equals(MemorySegment.NULL)) {
      return 0L;
    }
    return callNativeFunction(
        "wasmtime4j_panama_component_metrics_get_error_count", Long.class, metricsHandle);
  }

  /**
   * Destroys component metrics and frees associated resources.
   *
   * @param metricsHandle the component metrics handle to destroy
   */
  public void componentMetricsDestroy(final MemorySegment metricsHandle) {
    if (metricsHandle != null && !metricsHandle.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_component_metrics_destroy", Void.class, metricsHandle);
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
   * Creates a new component linker from a component engine.
   *
   * @param enginePtr pointer to the component engine
   * @return pointer to the new component linker, or null on failure
   */
  public MemorySegment componentLinkerCreate(final MemorySegment enginePtr) {
    validatePointer(enginePtr, "enginePtr");
    return callNativeFunction("wasmtime4j_component_linker_new", MemorySegment.class, enginePtr);
  }

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
   * Disposes a component linker's resources.
   *
   * @param linkerPtr pointer to the component linker
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerDispose(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction("wasmtime4j_component_linker_dispose", Integer.class, linkerPtr);
  }

  /**
   * Checks if an interface is defined in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param namespacePtr pointer to namespace string
   * @param interfaceNamePtr pointer to interface name string
   * @return 1 if present, 0 if not, -1 on error
   */
  public int componentLinkerHasInterface(
      final MemorySegment linkerPtr,
      final MemorySegment namespacePtr,
      final MemorySegment interfaceNamePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(namespacePtr, "namespacePtr");
    validatePointer(interfaceNamePtr, "interfaceNamePtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_has_interface",
        Integer.class,
        linkerPtr,
        namespacePtr,
        interfaceNamePtr);
  }

  /**
   * Checks if a function is defined in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param namespacePtr pointer to namespace string
   * @param interfaceNamePtr pointer to interface name string
   * @param functionNamePtr pointer to function name string
   * @return 1 if present, 0 if not, -1 on error
   */
  public int componentLinkerHasFunction(
      final MemorySegment linkerPtr,
      final MemorySegment namespacePtr,
      final MemorySegment interfaceNamePtr,
      final MemorySegment functionNamePtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(namespacePtr, "namespacePtr");
    validatePointer(interfaceNamePtr, "interfaceNamePtr");
    validatePointer(functionNamePtr, "functionNamePtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_has_function",
        Integer.class,
        linkerPtr,
        namespacePtr,
        interfaceNamePtr,
        functionNamePtr);
  }

  /**
   * Gets the number of host functions defined in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return the number of host functions
   */
  public long componentLinkerHostFunctionCount(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_host_function_count", Long.class, linkerPtr);
  }

  /**
   * Gets the number of interfaces defined in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return the number of interfaces
   */
  public long componentLinkerInterfaceCount(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction("wasmtime4j_component_linker_interface_count", Long.class, linkerPtr);
  }

  /**
   * Checks if WASI Preview 2 is enabled in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return 1 if enabled, 0 if not
   */
  public int componentLinkerWasiP2Enabled(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_wasi_p2_enabled", Integer.class, linkerPtr);
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
   * Adds a preopened directory for WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param hostPathPtr pointer to host path string
   * @param guestPathPtr pointer to guest path string
   * @param readOnly 1 for read-only, 0 for read-write
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerAddWasiPreopenDir(
      final MemorySegment linkerPtr,
      final MemorySegment hostPathPtr,
      final MemorySegment guestPathPtr,
      final int readOnly) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(hostPathPtr, "hostPathPtr");
    validatePointer(guestPathPtr, "guestPathPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_add_wasi_preopen_dir",
        Integer.class,
        linkerPtr,
        hostPathPtr,
        guestPathPtr,
        readOnly);
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
   * Sets whether clock access is allowed in WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param allow 1 to allow, 0 to disallow
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiAllowClock(final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_allow_clock", Integer.class, linkerPtr, allow);
  }

  /**
   * Sets whether random number generation is allowed in WASI Preview 2.
   *
   * @param linkerPtr pointer to the component linker
   * @param allow 1 to allow, 0 to disallow
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerSetWasiAllowRandom(final MemorySegment linkerPtr, final int allow) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_set_wasi_allow_random", Integer.class, linkerPtr, allow);
  }

  /**
   * Enables WASI HTTP support in the component linker.
   *
   * <p>This enables HTTP request/response functionality in WebAssembly components. WASI Preview 2
   * must be enabled first for this to work.
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
   * Checks if WASI HTTP is enabled in the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @return 1 if WASI HTTP is enabled, 0 if not, negative on error
   */
  public int componentLinkerWasiHttpEnabled(final MemorySegment linkerPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_wasi_http_enabled", Integer.class, linkerPtr);
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

  /**
   * Gets all defined interface names from the component linker.
   *
   * @param linkerPtr pointer to the component linker
   * @param jsonOutPtr pointer to store the JSON string pointer
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerGetInterfaces(
      final MemorySegment linkerPtr, final MemorySegment jsonOutPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(jsonOutPtr, "jsonOutPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_get_interfaces", Integer.class, linkerPtr, jsonOutPtr);
  }

  /**
   * Gets all functions defined for a specific interface.
   *
   * @param linkerPtr pointer to the component linker
   * @param namespacePtr pointer to namespace string
   * @param interfaceNamePtr pointer to interface name string
   * @param jsonOutPtr pointer to store the JSON string pointer
   * @return 0 on success, non-zero on error
   */
  public int componentLinkerGetFunctions(
      final MemorySegment linkerPtr,
      final MemorySegment namespacePtr,
      final MemorySegment interfaceNamePtr,
      final MemorySegment jsonOutPtr) {
    validatePointer(linkerPtr, "linkerPtr");
    validatePointer(namespacePtr, "namespacePtr");
    validatePointer(interfaceNamePtr, "interfaceNamePtr");
    validatePointer(jsonOutPtr, "jsonOutPtr");
    return callNativeFunction(
        "wasmtime4j_component_linker_get_functions",
        Integer.class,
        linkerPtr,
        namespacePtr,
        interfaceNamePtr,
        jsonOutPtr);
  }
}

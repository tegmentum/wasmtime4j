//! Enhanced Component Model FFI functions using EnhancedComponentEngine
//!
//! This module provides proper Store/Instance lifecycle management via instance IDs.
//! It offers C-compatible functions for loading, instantiating, and invoking
//! WebAssembly components using the Component Model specification.

use crate::component::Component;
use crate::component_core::EnhancedComponentEngine;
use crate::error::ffi_utils;
use crate::wit_value_marshal;
use std::os::raw::{c_char, c_int, c_ulong, c_void};
use wasmtime::component::Val;

/// WitValueFFI structure for passing WIT values across FFI boundary
#[repr(C)]
struct WitValueFFI {
    type_discriminator: c_int,
    data_length: c_int,
    data_ptr: *const u8,
}

/// Create an enhanced component engine
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_engine_create() -> *mut c_void {
    match EnhancedComponentEngine::new() {
        Ok(engine) => Box::into_raw(Box::new(engine)) as *mut c_void,
        Err(e) => {
            log::error!("Failed to create enhanced component engine: {:?}", e);
            std::ptr::null_mut()
        }
    }
}

/// Load component from bytes using enhanced engine
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_load_from_bytes(
    engine_ptr: *mut c_void,
    wasm_bytes: *const u8,
    wasm_size: usize,
    component_out: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let engine =
            unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr, "engine")? };
        let wasm_data = unsafe {
            if wasm_bytes.is_null() || wasm_size == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid WASM bytes or size".to_string(),
                });
            }
            std::slice::from_raw_parts(wasm_bytes, wasm_size)
        };

        let component = engine.load_component_from_bytes(wasm_data)?;

        unsafe {
            *component_out = Box::into_raw(Box::new(component)) as *mut c_void;
        }

        Ok(())
    })
}

/// Instantiate a component and return instance ID
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_instantiate(
    engine_ptr: *mut c_void,
    component_ptr: *mut c_void,
    instance_id_out: *mut c_ulong,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let engine =
            unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr, "engine")? };

        let component = unsafe { ffi_utils::deref_ptr::<Component>(component_ptr, "component")? };

        let instance_id = engine.instantiate_component(component)?;

        unsafe {
            *instance_id_out = instance_id;
        }

        Ok(())
    })
}

/// Invoke a component function using instance ID
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_invoke(
    engine_ptr: *mut c_void,
    instance_id: c_ulong,
    function_name: *const c_char,
    params_ptr: *const WitValueFFI,
    params_count: c_int,
    results_out: *mut *mut WitValueFFI,
    results_count_out: *mut c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let engine =
            unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr, "engine")? };

        let func_name = unsafe {
            std::ffi::CStr::from_ptr(function_name)
                .to_str()
                .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid function name: {}", e),
                })?
        };

        // Parse parameters from FFI format to Vec<Val>
        let mut params: Vec<Val> = Vec::with_capacity(params_count as usize);
        if !params_ptr.is_null() && params_count > 0 {
            unsafe {
                let params_slice = std::slice::from_raw_parts(params_ptr, params_count as usize);
                for (_i, param_ffi) in params_slice.iter().enumerate() {
                    let data = std::slice::from_raw_parts(
                        param_ffi.data_ptr,
                        param_ffi.data_length as usize,
                    );
                    let val =
                        wit_value_marshal::deserialize_to_val(param_ffi.type_discriminator, data)?;
                    params.push(val);
                }
            }
        }

        // Invoke the function using EnhancedComponentEngine
        let results = engine.invoke_component_function(instance_id, func_name, &params)?;

        // Serialize results to FFI format
        // First, flatten any tuples into their constituent elements
        let mut flattened_results: Vec<&Val> = Vec::new();
        for val in &results {
            match val {
                Val::Tuple(elements) => {
                    // Unwrap tuple into individual elements
                    flattened_results.extend(elements.iter());
                }
                _ => {
                    flattened_results.push(val);
                }
            }
        }

        if !flattened_results.is_empty() {
            let mut results_ffi: Vec<WitValueFFI> = Vec::with_capacity(flattened_results.len());

            for val in flattened_results {
                let (type_discriminator, data) = wit_value_marshal::serialize_from_val(val)?;

                // Allocate data on heap
                let data_vec = data.into_boxed_slice();
                let data_len = data_vec.len();
                let data_ptr = Box::into_raw(data_vec) as *const u8;

                results_ffi.push(WitValueFFI {
                    type_discriminator,
                    data_length: data_len as c_int,
                    data_ptr,
                });
            }

            let results_count = results_ffi.len();
            unsafe {
                *results_out = Box::into_raw(results_ffi.into_boxed_slice()) as *mut WitValueFFI;
                *results_count_out = results_count as c_int;
            }
        } else {
            unsafe {
                *results_out = std::ptr::null_mut();
                *results_count_out = 0;
            }
        }

        Ok(())
    })
}

/// Get exported function names using instance ID
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_get_exports(
    engine_ptr: *mut c_void,
    instance_id: c_ulong,
    functions_out: *mut *mut *mut c_char,
    count_out: *mut c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let engine =
            unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr, "engine")? };

        let function_names = engine.get_exported_function_names(instance_id)?;

        // Allocate array of C strings
        let count = function_names.len();
        let mut c_strings: Vec<*mut c_char> = Vec::with_capacity(count);

        for name in function_names {
            let c_string = std::ffi::CString::new(name).map_err(|e| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid function name: {}", e),
                }
            })?;
            c_strings.push(c_string.into_raw());
        }

        // Box the array and return pointer
        unsafe {
            *functions_out = Box::into_raw(c_strings.into_boxed_slice()) as *mut *mut c_char;
            *count_out = count as c_int;
        }

        Ok(())
    })
}

/// Check if two enhanced component engines share the same underlying Wasmtime engine
///
/// Returns 1 if same, 0 if different, -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_engine_same(
    engine_ptr1: *mut c_void,
    engine_ptr2: *mut c_void,
) -> c_int {
    if engine_ptr1.is_null() || engine_ptr2.is_null() {
        return -1;
    }
    match (
        unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr1, "engine1") },
        unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr2, "engine2") },
    ) {
        (Ok(engine1), Ok(engine2)) => {
            if wasmtime::Engine::same(engine1.engine(), engine2.engine()) {
                1
            } else {
                0
            }
        }
        _ => -1,
    }
}

/// Check if async support is enabled for an enhanced component engine
///
/// Returns 1 if async, 0 if not, -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_engine_is_async(
    engine_ptr: *mut c_void,
) -> c_int {
    if engine_ptr.is_null() {
        return -1;
    }
    match unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr, "engine") } {
        Ok(_engine) => {
            // EnhancedComponentEngine always creates its engine with async_support(false)
            0
        }
        Err(_) => -1,
    }
}

/// Deserialize a component from bytes using the enhanced component engine.
///
/// Returns a new component pointer through `component_ptr_out`.
/// The engine pointer must be an `EnhancedComponentEngine`.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_component_deserialize(
    engine_ptr: *mut c_void,
    data_ptr: *const u8,
    len: usize,
    component_ptr_out: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let engine =
            unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr, "engine")? };

        if data_ptr.is_null() || len == 0 {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Serialized data pointer is null or length is zero".to_string(),
            });
        }

        if component_ptr_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Component output pointer is null".to_string(),
            });
        }

        let bytes = unsafe { std::slice::from_raw_parts(data_ptr, len) };
        let component = Component::deserialize(engine.engine(), bytes)?;

        unsafe {
            *component_ptr_out = Box::into_raw(Box::new(component)) as *mut c_void;
        }

        Ok(())
    })
}

/// Deserialize a component from a file using the enhanced component engine.
///
/// Returns a new component pointer through `component_ptr_out`.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_component_deserialize_file(
    engine_ptr: *mut c_void,
    path_ptr: *const u8,
    path_len: usize,
    component_ptr_out: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let engine =
            unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr, "engine")? };

        if path_ptr.is_null() || path_len == 0 {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "File path pointer is null or length is zero".to_string(),
            });
        }

        if component_ptr_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Component output pointer is null".to_string(),
            });
        }

        let path_bytes = unsafe { std::slice::from_raw_parts(path_ptr, path_len) };
        let path_str = std::str::from_utf8(path_bytes).map_err(|e| {
            crate::error::WasmtimeError::InvalidParameter {
                message: format!("Invalid UTF-8 in file path: {}", e),
            }
        })?;

        let component =
            crate::component::core::deserialize_component_file(engine.engine(), path_str)?;

        unsafe {
            *component_ptr_out = Box::into_raw(component) as *mut c_void;
        }

        Ok(())
    })
}

/// Get component resources required.
///
/// Returns 4 values through out-pointers:
/// - num_memories: number of memories (-2 if unavailable)
/// - max_initial_memory_size: max initial memory in bytes (-1 if unbounded, -2 if unavailable)
/// - num_tables: number of tables (-2 if unavailable)
/// - max_initial_table_size: max initial table size (-1 if unbounded, -2 if unavailable)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_component_resources_required(
    component_ptr: *mut c_void,
    num_memories_out: *mut i32,
    max_memory_out: *mut i64,
    num_tables_out: *mut i32,
    max_table_out: *mut i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let component =
            unsafe { crate::component::core::get_component_ref(component_ptr)? };

        if num_memories_out.is_null()
            || max_memory_out.is_null()
            || num_tables_out.is_null()
            || max_table_out.is_null()
        {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Output pointers cannot be null".to_string(),
            });
        }

        let (num_mem, max_mem, num_tab, max_tab) =
            crate::component::core::get_component_resources_required(component);

        unsafe {
            *num_memories_out = num_mem;
            *max_memory_out = max_mem;
            *num_tables_out = num_tab;
            *max_table_out = max_tab;
        }

        Ok(())
    })
}

/// Check if a component instance has a specific function export
///
/// Returns 1 if found, 0 if not found, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_instance_has_func(
    engine_ptr: *mut c_void,
    instance_id: c_ulong,
    function_name: *const c_char,
) -> c_int {
    if engine_ptr.is_null() || function_name.is_null() {
        return -1;
    }

    let engine = unsafe { &*(engine_ptr as *const EnhancedComponentEngine) };
    let name = match unsafe { std::ffi::CStr::from_ptr(function_name) }.to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    match engine.has_component_instance_func(instance_id as u64, name) {
        Ok(true) => 1,
        Ok(false) => 0,
        Err(_) => -1,
    }
}

/// Look up a core module exported by a component instance
///
/// Returns 0 on success (module pointer written to module_out, NULL if not found),
/// negative on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_instance_get_module(
    engine_ptr: *mut c_void,
    instance_id: c_ulong,
    module_name: *const c_char,
    module_out: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(move || {
        if engine_ptr.is_null() || module_name.is_null() || module_out.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Null pointer argument".to_string(),
            });
        }

        let engine = unsafe { &*(engine_ptr as *const EnhancedComponentEngine) };
        let name = unsafe { std::ffi::CStr::from_ptr(module_name) }
            .to_str()
            .map_err(|_| crate::error::WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in module name".to_string(),
            })?;

        match engine.get_component_instance_module(instance_id as u64, name)? {
            Some(module) => {
                let boxed = Box::new(module);
                unsafe {
                    *module_out = Box::into_raw(boxed) as *mut c_void;
                }
            }
            None => unsafe {
                *module_out = std::ptr::null_mut();
            },
        }

        Ok(())
    })
}

/// Check if a resource type is exported by a component instance
///
/// Returns 1 if found, 0 if not found, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_instance_has_resource(
    engine_ptr: *mut c_void,
    instance_id: c_ulong,
    resource_name: *const c_char,
) -> c_int {
    if engine_ptr.is_null() || resource_name.is_null() {
        return -1;
    }

    let engine = unsafe { &*(engine_ptr as *const EnhancedComponentEngine) };
    let name = match unsafe { std::ffi::CStr::from_ptr(resource_name) }.to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    match engine.get_component_instance_resource(instance_id as u64, name) {
        Ok(true) => 1,
        Ok(false) => 0,
        Err(_) => -1,
    }
}

/// Get a component export index for efficient repeated lookups.
///
/// The `instance_index_ptr` is an optional parent instance export index for nested lookups.
/// Pass null for root-level export lookups.
///
/// Returns 0 on success (index written to index_out, or null if not found), -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_component_get_export_index(
    component_ptr: *const c_void,
    instance_index_ptr: *const c_void,
    name: *const c_char,
    index_out: *mut *mut c_void,
) -> c_int {
    if component_ptr.is_null() || name.is_null() || index_out.is_null() {
        return -1;
    }

    let component = unsafe { &*(component_ptr as *const Component) };
    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let instance = if instance_index_ptr.is_null() {
        None
    } else {
        Some(unsafe {
            &*(instance_index_ptr as *const wasmtime::component::ComponentExportIndex)
        })
    };

    match crate::component::core::get_export_index(component, instance, name_str) {
        Some(boxed_index) => {
            unsafe { *index_out = Box::into_raw(boxed_index) as *mut c_void };
            0
        }
        None => {
            unsafe { *index_out = std::ptr::null_mut() };
            0 // Not an error - export just wasn't found
        }
    }
}

/// Destroy a component export index.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_component_export_index_destroy(index_ptr: *mut c_void) {
    unsafe {
        crate::component::core::destroy_export_index(index_ptr);
    }
}

/// Check if a component instance has a function at the given export index.
///
/// Returns 1 if found, 0 if not found, -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_instance_has_func_by_index(
    engine_ptr: *mut c_void,
    instance_id: c_ulong,
    index_ptr: *const c_void,
) -> c_int {
    if engine_ptr.is_null() || index_ptr.is_null() {
        return -1;
    }

    let engine = unsafe { &*(engine_ptr as *const EnhancedComponentEngine) };
    let export_index = unsafe {
        &*(index_ptr as *const wasmtime::component::ComponentExportIndex)
    };

    match engine.has_component_instance_func_by_index(instance_id as u64, export_index) {
        Ok(true) => 1,
        Ok(false) => 0,
        Err(_) => -1,
    }
}

/// Destroy an enhanced component engine
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_engine_destroy(engine_ptr: *mut c_void) {
    if !engine_ptr.is_null() {
        unsafe {
            let _ = Box::from_raw(engine_ptr as *mut EnhancedComponentEngine);
        }
    }
}

/// Run concurrent component function calls.
///
/// Takes a JSON string containing the batch of calls and returns a JSON string
/// containing the results. The caller must free the result string using
/// `wasmtime4j_panama_free_string`.
///
/// # Returns
/// 0 on success, -1 on error (error message written to result_ptr)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_run_concurrent(
    engine_ptr: *mut c_void,
    instance_id: c_ulong,
    json_ptr: *const u8,
    json_len: c_ulong,
    result_ptr: *mut *mut u8,
    result_len: *mut c_ulong,
) -> c_int {
    if engine_ptr.is_null() || json_ptr.is_null() || result_ptr.is_null() || result_len.is_null() {
        return -1;
    }

    let engine = unsafe { &*(engine_ptr as *const EnhancedComponentEngine) };

    let json_bytes = unsafe { std::slice::from_raw_parts(json_ptr, json_len as usize) };
    let json_str = match std::str::from_utf8(json_bytes) {
        Ok(s) => s,
        Err(e) => {
            log::error!("Invalid UTF-8 in concurrent calls JSON: {}", e);
            return -1;
        }
    };

    let calls =
        match crate::component_core::concurrent_call_json::deserialize_calls(json_str) {
            Ok(c) => c,
            Err(e) => {
                log::error!("Failed to deserialize concurrent calls: {}", e);
                // Write error message as result
                let err_msg = format!("{{\"error\":\"{}\"}}", e);
                let err_bytes = err_msg.into_bytes();
                let len = err_bytes.len();
                let ptr = err_bytes.as_ptr();
                std::mem::forget(err_bytes);
                unsafe {
                    *result_ptr = ptr as *mut u8;
                    *result_len = len as c_ulong;
                }
                return -1;
            }
        };

    match engine.run_concurrent_calls(instance_id as u64, calls) {
        Ok(results) => {
            match crate::component_core::concurrent_call_json::serialize_results(&results)
            {
                Ok(json_result) => {
                    let result_bytes = json_result.into_bytes();
                    let len = result_bytes.len();
                    let boxed = result_bytes.into_boxed_slice();
                    let ptr = Box::into_raw(boxed) as *mut u8;
                    unsafe {
                        *result_ptr = ptr;
                        *result_len = len as c_ulong;
                    }
                    0
                }
                Err(e) => {
                    log::error!("Failed to serialize concurrent results: {}", e);
                    -1
                }
            }
        }
        Err(e) => {
            log::error!("Concurrent calls failed: {}", e);
            let err_msg = format!("{{\"error\":\"{}\"}}", e);
            let err_bytes = err_msg.into_bytes();
            let len = err_bytes.len();
            let boxed = err_bytes.into_boxed_slice();
            let ptr = Box::into_raw(boxed) as *mut u8;
            unsafe {
                *result_ptr = ptr;
                *result_len = len as c_ulong;
            }
            -1
        }
    }
}

/// Free a string allocated by `wasmtime4j_panama_enhanced_component_run_concurrent`
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_free_concurrent_result(ptr: *mut u8, len: c_ulong) {
    if !ptr.is_null() && len > 0 {
        unsafe {
            let _ = Box::from_raw(std::slice::from_raw_parts_mut(ptr, len as usize));
        }
    }
}

/// Get image range from component (Panama FFI version)
///
/// Returns 0 on success, start_ptr and end_ptr are set.
/// Returns -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_component_image_range(
    component_ptr: *mut c_void,
    start_ptr: *mut u64,
    end_ptr: *mut u64,
) -> c_int {
    if component_ptr.is_null() || start_ptr.is_null() || end_ptr.is_null() {
        return -1;
    }

    match unsafe {
        crate::component::core::get_component_ref(component_ptr)
    } {
        Ok(component) => {
            let (start, end) = crate::component::core::get_component_image_range(component);
            unsafe {
                *start_ptr = start as u64;
                *end_ptr = end as u64;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Initialize copy-on-write image for faster instantiation (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_component_initialize_cow_image(
    component_ptr: *mut c_void,
) -> c_int {
    if component_ptr.is_null() {
        return -1;
    }

    match unsafe {
        crate::component::core::get_component_ref(component_ptr)
    } {
        Ok(component) => {
            match crate::component::core::initialize_copy_on_write_image(component) {
                Ok(()) => 0,
                Err(_) => -1,
            }
        }
        Err(_) => -1,
    }
}

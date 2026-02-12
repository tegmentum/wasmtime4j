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
    use std::fs::OpenOptions;
    use std::io::Write;

    let _ = OpenOptions::new()
        .create(true)
        .append(true)
        .open("/tmp/wasmtime4j_debug.log")
        .and_then(|mut log| {
            writeln!(log, "\n===== RUST FFI ENTRY POINT =====")?;
            writeln!(
                log,
                "RUST: Function called with engine_ptr={:?}",
                engine_ptr
            )?;
            writeln!(log, "RUST: component_ptr={:?}", component_ptr)
        });

    let result = ffi_utils::ffi_try_code(|| {
        let engine =
            unsafe { ffi_utils::deref_ptr::<EnhancedComponentEngine>(engine_ptr, "engine")? };

        let component = unsafe { ffi_utils::deref_ptr::<Component>(component_ptr, "component")? };

        let instance_id = engine.instantiate_component(component)?;

        unsafe {
            *instance_id_out = instance_id;
        }

        Ok(())
    });

    let _ = OpenOptions::new()
        .create(true)
        .append(true)
        .open("/tmp/wasmtime4j_debug.log")
        .and_then(|mut log| writeln!(log, "RUST: Result = {:?}", result));

    result
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

/// Destroy an enhanced component engine
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_enhanced_component_engine_destroy(engine_ptr: *mut c_void) {
    if !engine_ptr.is_null() {
        unsafe {
            let _ = Box::from_raw(engine_ptr as *mut EnhancedComponentEngine);
        }
    }
}

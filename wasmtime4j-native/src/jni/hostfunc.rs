//! JNI bindings for Host Function operations
//!
//! This module provides JNI bindings for creating and managing host functions
//! that can be called from WebAssembly modules.
//!
//! It exports `unmarshal_function_type` for use by other modules.

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jbyteArray, jlong};
use jni::JNIEnv;

use crate::error::jni_utils;
use crate::{WasmtimeError, WasmtimeResult};
use std::os::raw::c_void;
use wasmtime::FuncType;

/// Create a new host function (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHostFunction_nativeCreateHostFunction(
    mut env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    function_name: JString,
    function_type_data: jbyteArray,
    host_function_id: jlong,
) -> jlong {
    // Extract string and byte array data first
    let name_string = match env.get_string(&function_name) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jlong,
    };

    let type_data_bytes =
        match env.convert_byte_array(unsafe { JByteArray::from_raw(function_type_data) }) {
            Ok(data) => data,
            Err(_) => return 0 as jlong,
        };

    // Get JVM reference for callback - needed before entering jni_try_ptr closure
    let jvm = match env.get_java_vm() {
        Ok(jvm) => std::sync::Arc::new(jvm),
        Err(e) => {
            log::error!("Failed to get JVM reference: {}", e);
            return 0 as jlong;
        }
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let name: String = name_string;
        let type_data = type_data_bytes;

        // Get store reference
        let store = unsafe { crate::store::core::get_store_ref(store_handle as *const c_void)? };

        // Create function type from marshalled data
        let func_type = store.with_context(|ctx| {
            let engine = ctx.engine();
            unmarshal_function_type(engine, &type_data)
        })?;

        // Create callback wrapper using the working JniHostFunctionCallback from jni::linker
        // is_function_reference = false because this routes through JniLinker.invokeHostFunctionCallback
        let callback = Box::new(super::linker::JniHostFunctionCallback {
            jvm: jvm.clone(),
            callback_id: host_function_id,
            is_function_reference: false,
        });

        // Use Store's create_host_function method which handles weak references properly
        let (host_function_id, wasmtime_func) =
            store.create_host_function(name, func_type, callback)?;

        // Register the wasmtime_func in the function reference registry so it can be
        // retrieved later for table.set() operations
        let func_ref_id =
            crate::table::core::register_function_reference(wasmtime_func, store.id())?;

        // Create a struct to hold both IDs - the host_function_id for callback management
        // and func_ref_id for table operations
        #[repr(C)]
        struct JniHostFunctionHandle {
            host_function_id: u64,
            func_ref_id: u64,
        }

        Ok(Box::new(JniHostFunctionHandle {
            host_function_id,
            func_ref_id,
        }))
    }) as jlong
}

/// Destroy a host function (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHostFunction_nativeDestroyHostFunction(
    env: JNIEnv,
    _class: JClass,
    host_func_handle: jlong,
) {
    jni_utils::jni_try_default(&env, (), || {
        if host_func_handle == 0 {
            return Ok(());
        }

        // The handle is a pointer to JniHostFunctionHandle struct
        #[repr(C)]
        struct JniHostFunctionHandle {
            host_function_id: u64,
            func_ref_id: u64,
        }

        let handle_struct = unsafe { &*(host_func_handle as *const JniHostFunctionHandle) };
        let host_function_id = handle_struct.host_function_id;
        let func_ref_id = handle_struct.func_ref_id;

        // Remove from host function callback registry
        match crate::hostfunc::core::remove_host_function(host_function_id) {
            Ok(_) => {
                log::debug!("Destroyed JNI host function with ID: {}", host_function_id);
            }
            Err(e) => {
                log::warn!("Failed to remove host function from registry: {}", e);
            }
        }

        // Also clean up the function reference from the table registry
        // This is necessary to prevent stale entries from conflicting with new registrations
        if func_ref_id != 0 {
            match crate::table::core::remove_function_reference(func_ref_id) {
                Ok(Some(_)) => {
                    log::debug!(
                        "Removed function reference {} from table registry",
                        func_ref_id
                    );
                }
                Ok(None) => {
                    log::debug!(
                        "Function reference {} was not in table registry",
                        func_ref_id
                    );
                }
                Err(e) => {
                    log::warn!(
                        "Failed to remove function reference {} from table registry: {}",
                        func_ref_id,
                        e
                    );
                }
            }
        }

        // Clean up the boxed handle struct
        unsafe {
            let _ = Box::from_raw(host_func_handle as *mut JniHostFunctionHandle);
        }

        Ok(())
    });
}

/// Unmarshal function type from byte array
pub fn unmarshal_function_type(engine: &wasmtime::Engine, data: &[u8]) -> WasmtimeResult<FuncType> {
    // Simple marshalling format:
    // [param_count: 4 bytes][param_types: param_count bytes][return_count: 4 bytes][return_types: return_count bytes]

    if data.len() < 8 {
        return Err(WasmtimeError::Validation {
            message: "Function type data too short".to_string(),
        });
    }

    let param_count = u32::from_le_bytes([data[0], data[1], data[2], data[3]]) as usize;
    let return_count_offset = 4 + param_count;

    if data.len() < return_count_offset + 4 {
        return Err(WasmtimeError::Validation {
            message: "Function type data missing return count".to_string(),
        });
    }

    let return_count = u32::from_le_bytes([
        data[return_count_offset],
        data[return_count_offset + 1],
        data[return_count_offset + 2],
        data[return_count_offset + 3],
    ]) as usize;

    if data.len() < return_count_offset + 4 + return_count {
        return Err(WasmtimeError::Validation {
            message: "Function type data too short for return types".to_string(),
        });
    }

    // Parse parameter types
    let mut param_types = Vec::with_capacity(param_count);
    for i in 0..param_count {
        let val_type = crate::ffi_common::valtype_conversion::int_to_valtype(data[4 + i] as i32)?;
        param_types.push(val_type);
    }

    // Parse return types
    let mut return_types = Vec::with_capacity(return_count);
    for i in 0..return_count {
        let val_type = crate::ffi_common::valtype_conversion::int_to_valtype(
            data[return_count_offset + 4 + i] as i32,
        )?;
        return_types.push(val_type);
    }

    Ok(wasmtime::FuncType::new(engine, param_types, return_types))
}

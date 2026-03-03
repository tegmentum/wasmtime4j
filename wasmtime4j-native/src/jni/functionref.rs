//! JNI bindings for WebAssembly function references
//!
//! This module provides JNI bindings for creating and managing function references
//! that can be used with WebAssembly tables and indirect calls.

use jni::objects::JClass;
use jni::sys::{jbyteArray, jint, jlong};
use jni::JNIEnv;

use super::hostfunc::unmarshal_function_type;
use super::linker::JniHostFunctionCallback;
use crate::error::{jni_utils, WasmtimeError};
use crate::store::Store;

/// Create a new function reference from a host function (JNI version)
///
/// Creates a real Rust Func and registers it in the table reference registry.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeCreateFunctionReferenceFromHost(
    mut env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    function_type_data: jbyteArray,
    function_reference_id: jlong,
) -> jlong {
    // Extract data before the closure
    let type_data_result = env
        .convert_byte_array(unsafe { jni::objects::JByteArray::from_raw(function_type_data) })
        .map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to read function type data: {}", e),
            backtrace: None,
        });

    let type_data = match type_data_result {
        Ok(data) => data,
        Err(_) => return 0,
    };

    let jvm = match env.get_java_vm() {
        Ok(vm) => vm,
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        if store_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }

        // Get the store
        let store_ref = unsafe { &*(store_handle as *const Store) };

        // Get engine from store context and unmarshal function type
        let func_type =
            store_ref.with_context(|ctx| unmarshal_function_type(ctx.engine(), &type_data))?;

        // Create the JNI callback wrapper (jvm is already extracted above)
        let callback = Box::new(JniHostFunctionCallback {
            jvm: std::sync::Arc::new(jvm),
            callback_id: function_reference_id,
            is_function_reference: true, // This is a FunctionReference
        });

        // Create and register the function using Store::create_function_reference
        let name = format!("host_function_{}", function_reference_id);
        let registry_id = store_ref.create_function_reference(name, func_type, callback)?;

        // Return the registry ID as a boxed pointer so the destroy path is uniform
        // with wasm-created references (both use Box::from_raw to clean up)
        Ok(Box::into_raw(Box::new(registry_id as u64)) as jlong)
    })
}

/// Create a new function reference from a WebAssembly function (JNI version)
///
/// This is a minimal stub that just returns the function reference ID as a handle.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeCreateFunctionReferenceFromWasm(
    mut env: JNIEnv,
    _class: JClass,
    _store_handle: jlong,
    wasm_function_handle: jlong,
    _function_reference_id: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        if wasm_function_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "WebAssembly function handle cannot be null".to_string(),
            });
        }

        // The wasm_function_handle is already a function ID from a JniFunction
        // For function references from WebAssembly functions, we just return the same handle
        let func_id = unsafe { *(wasm_function_handle as *const u64) };

        // Return the function ID as the handle
        Ok(Box::new(func_id))
    }) as jlong
}

/// Dereference the boxed native handle to get the Rust reference registry ID.
///
/// The native handle is a pointer to a heap-allocated Box<u64> containing the registry ID.
/// This method reads the u64 value from the box without consuming it.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeGetRegistryId(
    _env: JNIEnv,
    _class: JClass,
    native_handle: jlong,
) -> jlong {
    if native_handle == 0 {
        return 0;
    }
    unsafe { *(native_handle as *const u64) as jlong }
}

/// Call a function reference through native code (JNI version)
///
/// Looks up the function from the reference registry and calls it via
/// the standard function call path using the store context.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeCallFunctionReference(
    mut env: JNIEnv,
    _class: JClass,
    function_reference_handle: jlong,
    store_handle: jlong,
    params_data: jbyteArray,
    results_buffer: jbyteArray,
) -> jint {
    // Validate inputs early
    if function_reference_handle == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::invalid_parameter("Function reference handle cannot be null"),
        );
        return -1;
    }
    if store_handle == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::invalid_parameter("Store handle cannot be null"),
        );
        return -1;
    }

    // Use local closure pattern (like nativeCall in function.rs) to avoid borrow conflicts
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<jint> {
        // Dereference the boxed handle to get the registry ID
        let registry_id = unsafe { *(function_reference_handle as *const u64) };

        // Get the function from the reference registry
        let func = crate::table::core::get_function_reference(registry_id)?
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!(
                    "Function reference not found in registry: {}",
                    registry_id
                ),
            })?;

        // Get the store
        let store = unsafe {
            crate::store::core::get_store_mut(store_handle as *mut std::os::raw::c_void)?
        };

        // Lock the store
        let mut store_lock = store.try_lock_store()?;

        // Get the function type for parameter/result info
        let func_type = func.ty(&*store_lock);
        let param_types: Vec<wasmtime::ValType> = func_type.params().collect();
        let result_count = func_type.results().len();

        // Unmarshal parameters from the byte array
        let params = if !params_data.is_null() {
            let params_bytes = env
                .convert_byte_array(unsafe { jni::objects::JByteArray::from_raw(params_data) })
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to read parameter data: {}", e),
                    backtrace: None,
                })?;
            unmarshal_params(&params_bytes, &param_types)?
        } else {
            Vec::new()
        };

        // Prepare result storage
        let mut results = vec![wasmtime::Val::I32(0); result_count];

        // Call the function
        func.call(&mut *store_lock, &params, &mut results)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Function reference call trapped: {}", e),
                backtrace: None,
            })?;

        // Marshal results
        let result_bytes = if result_count > 0 {
            marshal_results(&results)?
        } else {
            Vec::new()
        };

        // Write results back to the Java byte buffer
        if !result_bytes.is_empty() && !results_buffer.is_null() {
            let results_array = unsafe { jni::objects::JByteArray::from_raw(results_buffer) };
            env.set_byte_array_region(&results_array, 0, unsafe {
                std::slice::from_raw_parts(
                    result_bytes.as_ptr() as *const i8,
                    result_bytes.len(),
                )
            })
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to write result data: {}", e),
                backtrace: None,
            })?;
        }
        Ok(0)
    })) {
        Ok(Ok(result)) => result,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            -1
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            -1
        }
    }
}

/// Unmarshal parameters from JniTypeConverter byte format to Wasmtime Val.
///
/// Format: Each value is 16 bytes: [type_tag(4 bytes LE) | value(12 bytes)]
/// Type tags: 0=i32, 1=i64, 2=f32, 3=f64, 4=v128, 5=funcref, 6=externref
fn unmarshal_params(
    data: &[u8],
    expected_types: &[wasmtime::ValType],
) -> crate::error::WasmtimeResult<Vec<wasmtime::Val>> {
    if data.is_empty() && expected_types.is_empty() {
        return Ok(Vec::new());
    }

    let value_size = 16;
    if data.len() < expected_types.len() * value_size {
        return Err(WasmtimeError::InvalidParameter {
            message: format!(
                "Parameter data too short: {} bytes for {} parameters",
                data.len(),
                expected_types.len()
            ),
        });
    }

    let mut vals = Vec::with_capacity(expected_types.len());
    for (i, expected_type) in expected_types.iter().enumerate() {
        let offset = i * value_size;
        let type_tag = u32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]);
        let value_offset = offset + 4;

        let val = match expected_type {
            wasmtime::ValType::I32 => {
                let v = i32::from_le_bytes([
                    data[value_offset],
                    data[value_offset + 1],
                    data[value_offset + 2],
                    data[value_offset + 3],
                ]);
                wasmtime::Val::I32(v)
            }
            wasmtime::ValType::I64 => {
                let v = i64::from_le_bytes([
                    data[value_offset],
                    data[value_offset + 1],
                    data[value_offset + 2],
                    data[value_offset + 3],
                    data[value_offset + 4],
                    data[value_offset + 5],
                    data[value_offset + 6],
                    data[value_offset + 7],
                ]);
                wasmtime::Val::I64(v)
            }
            wasmtime::ValType::F32 => {
                let bits = u32::from_le_bytes([
                    data[value_offset],
                    data[value_offset + 1],
                    data[value_offset + 2],
                    data[value_offset + 3],
                ]);
                wasmtime::Val::F32(bits)
            }
            wasmtime::ValType::F64 => {
                let bits = u64::from_le_bytes([
                    data[value_offset],
                    data[value_offset + 1],
                    data[value_offset + 2],
                    data[value_offset + 3],
                    data[value_offset + 4],
                    data[value_offset + 5],
                    data[value_offset + 6],
                    data[value_offset + 7],
                ]);
                wasmtime::Val::F64(bits)
            }
            wasmtime::ValType::Ref(_) => {
                // For funcref/externref, use null
                if type_tag == 5 {
                    wasmtime::Val::FuncRef(None)
                } else {
                    wasmtime::Val::null_extern_ref()
                }
            }
            wasmtime::ValType::V128 => {
                let mut bytes = [0u8; 16];
                bytes[..12].copy_from_slice(&data[value_offset..value_offset + 12]);
                wasmtime::Val::V128(wasmtime::V128::from(u128::from_le_bytes(bytes)))
            }
        };
        vals.push(val);
    }

    Ok(vals)
}

/// Marshal results from Wasmtime Val to JniTypeConverter byte format.
///
/// Format: Each value is 16 bytes: [type_tag(4 bytes LE) | value(12 bytes)]
fn marshal_results(results: &[wasmtime::Val]) -> crate::error::WasmtimeResult<Vec<u8>> {
    let value_size = 16;
    let mut data = vec![0u8; results.len() * value_size];

    for (i, val) in results.iter().enumerate() {
        let offset = i * value_size;
        let value_offset = offset + 4;

        match val {
            wasmtime::Val::I32(v) => {
                data[offset..offset + 4].copy_from_slice(&0u32.to_le_bytes());
                data[value_offset..value_offset + 4].copy_from_slice(&v.to_le_bytes());
            }
            wasmtime::Val::I64(v) => {
                data[offset..offset + 4].copy_from_slice(&1u32.to_le_bytes());
                data[value_offset..value_offset + 8].copy_from_slice(&v.to_le_bytes());
            }
            wasmtime::Val::F32(v) => {
                data[offset..offset + 4].copy_from_slice(&2u32.to_le_bytes());
                data[value_offset..value_offset + 4].copy_from_slice(&v.to_le_bytes());
            }
            wasmtime::Val::F64(v) => {
                data[offset..offset + 4].copy_from_slice(&3u32.to_le_bytes());
                data[value_offset..value_offset + 8].copy_from_slice(&v.to_le_bytes());
            }
            wasmtime::Val::V128(v) => {
                data[offset..offset + 4].copy_from_slice(&4u32.to_le_bytes());
                let bytes = u128::from(*v).to_le_bytes();
                data[value_offset..value_offset + 12].copy_from_slice(&bytes[..12]);
            }
            wasmtime::Val::FuncRef(_) => {
                data[offset..offset + 4].copy_from_slice(&5u32.to_le_bytes());
            }
            wasmtime::Val::ExternRef(_) | wasmtime::Val::AnyRef(_) => {
                data[offset..offset + 4].copy_from_slice(&6u32.to_le_bytes());
            }
            _ => {
                data[offset..offset + 4].copy_from_slice(&6u32.to_le_bytes());
            }
        }
    }

    Ok(data)
}

/// Destroy a function reference (JNI version)
///
/// This is a minimal stub that cleans up the boxed handle.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeDestroyFunctionReference(
    mut env: JNIEnv,
    _class: JClass,
    function_reference_handle: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        if function_reference_handle == 0 {
            return Ok(());
        }

        // Clean up the boxed handle
        unsafe {
            let _ = Box::from_raw(function_reference_handle as *mut u64);
        }

        Ok(())
    });
}

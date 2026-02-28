//! JNI bindings for Function operations

use jni::objects::{JClass, JObject, JObjectArray};
use jni::sys::{jint, jlong, jobject, jobjectArray};
use jni::JNIEnv;

use crate::error::{jni_utils, WasmtimeError, WasmtimeResult};
use crate::ffi_common::memory_utils;
use crate::store::Store;
use wasmtime::{Func, Val, ValType};

/// Function handle that stores both the Wasmtime function and its type information
/// This allows for efficient type introspection without requiring a store context
#[derive(Debug)]
pub struct FunctionHandle {
    /// The underlying Wasmtime function
    pub func: Func,
    /// Function name for debugging
    pub name: String,
    /// Cached parameter type strings (Store-independent)
    param_types: Vec<String>,
    /// Cached return type strings (Store-independent)
    return_types: Vec<String>,
}

impl FunctionHandle {
    /// Create a new function handle with type information cached as strings
    ///
    /// Stores the Func reference and caches type info as strings at creation time.
    pub fn new(func: Func, name: String, store: &mut Store) -> Self {
        let store_guard = store.inner.lock();
        let func_type = func.ty(&*store_guard);

        let param_types = func_type
            .params()
            .map(|vt| valtype_to_string(&vt))
            .collect();
        let return_types = func_type
            .results()
            .map(|vt| valtype_to_string(&vt))
            .collect();

        Self {
            func,
            name,
            param_types,
            return_types,
        }
    }

    /// Get the underlying Wasmtime function reference
    pub fn get_func(&self) -> &Func {
        &self.func
    }

    /// Get parameter types as strings (cached at creation, Store-independent)
    pub fn get_param_type_strings(&self) -> &[String] {
        &self.param_types
    }

    /// Get return types as strings (cached at creation, Store-independent)
    pub fn get_return_type_strings(&self) -> &[String] {
        &self.return_types
    }
}

/// Convert ValType to string representation
fn valtype_to_string(vt: &ValType) -> String {
    match vt {
        ValType::I32 => "i32".to_string(),
        ValType::I64 => "i64".to_string(),
        ValType::F32 => "f32".to_string(),
        ValType::F64 => "f64".to_string(),
        ValType::V128 => "v128".to_string(),
        ValType::Ref(ref_type) => match ref_type {
            _ if ref_type.heap_type().is_func() => "funcref".to_string(),
            _ if ref_type.heap_type().is_extern() => "externref".to_string(),
            _ => "ref".to_string(),
        },
    }
}

/// Helper function to create Java String array from Vec<String>
fn create_java_string_array(env: &mut JNIEnv, strings: &[String]) -> WasmtimeResult<jobjectArray> {
    let string_class = env
        .find_class("java/lang/String")
        .map_err(|e| WasmtimeError::Function {
            message: format!("Failed to find String class: {}", e),
        })?;

    let array = env
        .new_object_array(strings.len() as i32, string_class, JObject::null())
        .map_err(|e| WasmtimeError::Function {
            message: format!("Failed to create String array: {}", e),
        })?;

    for (i, type_str) in strings.iter().enumerate() {
        let jstring = env
            .new_string(type_str)
            .map_err(|e| WasmtimeError::Function {
                message: format!("Failed to create String: {}", e),
            })?;
        env.set_object_array_element(&array, i as i32, &jstring)
            .map_err(|e| WasmtimeError::Function {
                message: format!("Failed to set array element: {}", e),
            })?;
    }

    Ok(array.into_raw())
}

/// Convert Java Object array to Wasmtime Val array for function parameters
fn convert_java_params_to_wasmtime_vals(
    env: &mut JNIEnv,
    params: jobjectArray,
    expected_types: &[ValType],
) -> WasmtimeResult<Vec<Val>> {
    if params.is_null() {
        return Ok(Vec::new());
    }

    let params_array = JObjectArray::from(unsafe { JObject::from_raw(params) });
    let param_count = env
        .get_array_length(&params_array)
        .map_err(|e| WasmtimeError::Function {
            message: format!("Failed to get parameter array length: {}", e),
        })?;

    if param_count as usize != expected_types.len() {
        return Err(WasmtimeError::Function {
            message: format!(
                "Parameter count mismatch: expected {}, got {}",
                expected_types.len(),
                param_count
            ),
        });
    }

    let mut vals = Vec::new();

    for i in 0..param_count {
        let param_obj = env
            .get_object_array_element(&params_array, i)
            .map_err(|e| WasmtimeError::Function {
                message: format!("Failed to get parameter {}: {}", i, e),
            })?;

        let expected_type = &expected_types[i as usize];
        let val = convert_java_object_to_wasmtime_val(env, param_obj.into_raw(), expected_type)?;
        vals.push(val);
    }

    Ok(vals)
}

/// Convert a single Java Object to a Wasmtime Val based on expected type
fn convert_java_object_to_wasmtime_val(
    env: &mut JNIEnv,
    obj: jobject,
    expected_type: &ValType,
) -> WasmtimeResult<Val> {
    if obj.is_null() {
        return match expected_type {
            ValType::Ref(_) => Ok(Val::null_extern_ref()),
            _ => Err(WasmtimeError::Function {
                message: format!("Null parameter for non-reference type: {:?}", expected_type),
            }),
        };
    }

    let jobject_ref = unsafe { JObject::from_raw(obj) };

    match expected_type {
        ValType::I32 => {
            // Try to convert from Integer wrapper
            let int_class =
                env.find_class("java/lang/Integer")
                    .map_err(|e| WasmtimeError::Function {
                        message: format!("Failed to find Integer class: {}", e),
                    })?;

            if env
                .is_instance_of(&jobject_ref, int_class)
                .map_err(|e| WasmtimeError::Function {
                    message: format!("Failed to check Integer instance: {}", e),
                })?
            {
                let value = env
                    .call_method(&jobject_ref, "intValue", "()I", &[])
                    .map_err(|e| WasmtimeError::Function {
                        message: format!("Failed to call intValue(): {}", e),
                    })?;

                match value {
                    jni::objects::JValueGen::Int(i) => Ok(Val::I32(i)),
                    _ => Err(WasmtimeError::Function {
                        message: "Invalid Integer value".to_string(),
                    }),
                }
            } else {
                Err(WasmtimeError::Function {
                    message: "Expected Integer parameter for i32".to_string(),
                })
            }
        }

        ValType::I64 => {
            // Try to convert from Long wrapper
            let long_class =
                env.find_class("java/lang/Long")
                    .map_err(|e| WasmtimeError::Function {
                        message: format!("Failed to find Long class: {}", e),
                    })?;

            if env.is_instance_of(&jobject_ref, long_class).map_err(|e| {
                WasmtimeError::Function {
                    message: format!("Failed to check Long instance: {}", e),
                }
            })? {
                let value = env
                    .call_method(&jobject_ref, "longValue", "()J", &[])
                    .map_err(|e| WasmtimeError::Function {
                        message: format!("Failed to call longValue(): {}", e),
                    })?;

                match value {
                    jni::objects::JValueGen::Long(l) => Ok(Val::I64(l)),
                    _ => Err(WasmtimeError::Function {
                        message: "Invalid Long value".to_string(),
                    }),
                }
            } else {
                Err(WasmtimeError::Function {
                    message: "Expected Long parameter for i64".to_string(),
                })
            }
        }

        ValType::F32 => {
            // Try to convert from Float wrapper
            let float_class =
                env.find_class("java/lang/Float")
                    .map_err(|e| WasmtimeError::Function {
                        message: format!("Failed to find Float class: {}", e),
                    })?;

            if env.is_instance_of(&jobject_ref, float_class).map_err(|e| {
                WasmtimeError::Function {
                    message: format!("Failed to check Float instance: {}", e),
                }
            })? {
                let value = env
                    .call_method(&jobject_ref, "floatValue", "()F", &[])
                    .map_err(|e| WasmtimeError::Function {
                        message: format!("Failed to call floatValue(): {}", e),
                    })?;

                match value {
                    jni::objects::JValueGen::Float(f) => Ok(Val::F32(f.to_bits())),
                    _ => Err(WasmtimeError::Function {
                        message: "Invalid Float value".to_string(),
                    }),
                }
            } else {
                Err(WasmtimeError::Function {
                    message: "Expected Float parameter for f32".to_string(),
                })
            }
        }

        ValType::F64 => {
            // Try to convert from Double wrapper
            let double_class =
                env.find_class("java/lang/Double")
                    .map_err(|e| WasmtimeError::Function {
                        message: format!("Failed to find Double class: {}", e),
                    })?;

            if env
                .is_instance_of(&jobject_ref, double_class)
                .map_err(|e| WasmtimeError::Function {
                    message: format!("Failed to check Double instance: {}", e),
                })?
            {
                let value = env
                    .call_method(&jobject_ref, "doubleValue", "()D", &[])
                    .map_err(|e| WasmtimeError::Function {
                        message: format!("Failed to call doubleValue(): {}", e),
                    })?;

                match value {
                    jni::objects::JValueGen::Double(d) => Ok(Val::F64(d.to_bits())),
                    _ => Err(WasmtimeError::Function {
                        message: "Invalid Double value".to_string(),
                    }),
                }
            } else {
                Err(WasmtimeError::Function {
                    message: "Expected Double parameter for f64".to_string(),
                })
            }
        }

        ValType::V128 => {
            // For V128, expect a byte array
            let byte_array_class = env.find_class("[B").map_err(|e| WasmtimeError::Function {
                message: format!("Failed to find byte array class: {}", e),
            })?;
            let is_byte_array =
                env.is_instance_of(&jobject_ref, byte_array_class)
                    .map_err(|e| WasmtimeError::Function {
                        message: format!("Failed to check instance type: {}", e),
                    })?;
            if is_byte_array {
                let byte_array: jni::objects::JPrimitiveArray<i8> = jobject_ref.into();
                let bytes =
                    env.convert_byte_array(byte_array)
                        .map_err(|e| WasmtimeError::Function {
                            message: format!("Failed to convert byte array: {}", e),
                        })?;

                if bytes.len() != 16 {
                    return Err(WasmtimeError::Function {
                        message: format!("V128 requires exactly 16 bytes, got {}", bytes.len()),
                    });
                }

                let mut v128_bytes = [0u8; 16];
                v128_bytes.copy_from_slice(&bytes);
                Ok(Val::V128(wasmtime::V128::from(u128::from_le_bytes(
                    v128_bytes,
                ))))
            } else {
                Err(WasmtimeError::Function {
                    message: "Expected byte array for V128".to_string(),
                })
            }
        }

        ValType::Ref(_) => {
            // For now, we'll handle references as null or set externref to null
            Ok(Val::null_extern_ref())
        }
    }
}

/// Get parameter types of a function (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeGetParameterTypes(
    mut env: JNIEnv,
    _class: JClass,
    function_ptr: jlong,
) -> jobjectArray {
    // Defensive programming: validate function pointer
    if function_ptr == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::invalid_parameter("function_ptr cannot be null"),
        );
        return std::ptr::null_mut();
    }

    match memory_utils::safe_deref(function_ptr as *const FunctionHandle, "function_ptr") {
        Ok(func_handle) => {
            let param_type_strings = func_handle.get_param_type_strings();

            match create_java_string_array(&mut env, param_type_strings) {
                Ok(array) => array,
                Err(error) => {
                    jni_utils::throw_jni_exception(&mut env, &error);
                    std::ptr::null_mut()
                }
            }
        }
        Err(memory_error) => {
            let wasmtime_error = memory_error.to_wasmtime_error();
            jni_utils::throw_jni_exception(&mut env, &wasmtime_error);
            std::ptr::null_mut()
        }
    }
}

/// Get return types of a function (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeGetReturnTypes(
    mut env: JNIEnv,
    _class: JClass,
    function_ptr: jlong,
) -> jobjectArray {
    // Defensive programming: validate function pointer
    if function_ptr == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::invalid_parameter("function_ptr cannot be null"),
        );
        return std::ptr::null_mut();
    }

    match memory_utils::safe_deref(function_ptr as *const FunctionHandle, "function_ptr") {
        Ok(func_handle) => {
            let return_type_strings = func_handle.get_return_type_strings();

            match create_java_string_array(&mut env, return_type_strings) {
                Ok(array) => array,
                Err(error) => {
                    jni_utils::throw_jni_exception(&mut env, &error);
                    std::ptr::null_mut()
                }
            }
        }
        Err(memory_error) => {
            let wasmtime_error = memory_error.to_wasmtime_error();
            jni_utils::throw_jni_exception(&mut env, &wasmtime_error);
            std::ptr::null_mut()
        }
    }
}

/// Call a function with generic parameters (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCall(
    mut env: JNIEnv,
    _class: JClass,
    function_ptr: jlong,
    store_handle: jlong,
    params: jobjectArray,
) -> jobjectArray {
    // Defensive programming: validate function pointer
    if function_ptr == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::invalid_parameter("function_ptr cannot be null"),
        );
        return std::ptr::null_mut();
    }

    // Defensive programming: validate store handle
    if store_handle == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::invalid_parameter("store_handle cannot be null"),
        );
        return std::ptr::null_mut();
    }

    use std::os::raw::c_void;

    // Helper closure for the actual work
    let result = (|| -> WasmtimeResult<jobjectArray> {
        // Get function from FunctionHandle
        let func_handle = unsafe { &*(function_ptr as *const FunctionHandle) };
        let func = func_handle.get_func();

        // Get store reference
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        // Lock the store for reentrant access
        let mut store_lock = store.try_lock_store()?;

        // Get function type for parameter conversion
        let func_type = func.ty(&*store_lock);
        let param_types = func_type.params().collect::<Vec<_>>();

        // Convert Java parameters to Wasmtime values
        let wasmtime_params = convert_java_params_to_wasmtime_vals(&mut env, params, &param_types)?;

        // Prepare result storage
        let result_count = func_type.results().len();
        let mut results = vec![Val::I32(0); result_count];

        // Call function
        match func.call(&mut *store_lock, &wasmtime_params, &mut results) {
            Ok(()) => {
                // Convert Wasmtime Val results to WasmValue
                let wasm_values: Result<Vec<_>, _> = results
                    .iter()
                    .map(|val| crate::instance::core::wasmtime_val_to_wasm_value(val))
                    .collect();
                let wasm_values = wasm_values?;

                // Convert WasmValue to Java array
                let java_array =
                    crate::jni::linker::wasm_values_to_java_array(&mut env, &wasm_values)?;
                Ok(java_array.as_raw())
            }
            Err(trap) => {
                // Handle Wasmtime trap
                Err(WasmtimeError::Runtime {
                    message: format!("Function call trapped: {}", trap),
                    backtrace: None,
                })
            }
        }
    })();

    match result {
        Ok(arr) => arr,
        Err(error) => {
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// Call a WebAssembly function asynchronously (JNI version)
#[no_mangle]
#[cfg(feature = "async")]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCallAsync(
    mut env: JNIEnv,
    _class: JClass,
    function_ptr: jlong,
    store_handle: jlong,
    params: jobjectArray,
) -> jobjectArray {
    // Defensive programming: validate function pointer
    if function_ptr == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::invalid_parameter("function_ptr cannot be null"),
        );
        return std::ptr::null_mut();
    }

    // Defensive programming: validate store handle
    if store_handle == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::invalid_parameter("store_handle cannot be null"),
        );
        return std::ptr::null_mut();
    }

    use std::os::raw::c_void;

    // Helper closure for the actual work
    let result = (|| -> WasmtimeResult<jobjectArray> {
        // Get function from FunctionHandle
        let func_handle = unsafe { &*(function_ptr as *const FunctionHandle) };
        let func = func_handle.get_func();

        // Get store reference
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        // Lock the store for reentrant access
        let mut store_lock = store.try_lock_store()?;

        // Get function type for parameter conversion
        let func_type = func.ty(&*store_lock);
        let param_types = func_type.params().collect::<Vec<_>>();

        // Convert Java parameters to Wasmtime values
        let wasmtime_params = convert_java_params_to_wasmtime_vals(&mut env, params, &param_types)?;

        // Prepare result storage
        let result_count = func_type.results().len();
        let mut results = vec![Val::I32(0); result_count];

        // Call function asynchronously using the global runtime
        let runtime = crate::async_runtime::get_async_runtime();
        match runtime.block_on(func.call_async(&mut *store_lock, &wasmtime_params, &mut results)) {
            Ok(()) => {
                // Convert Wasmtime Val results to WasmValue
                let wasm_values: Result<Vec<_>, _> = results
                    .iter()
                    .map(|val| crate::instance::core::wasmtime_val_to_wasm_value(val))
                    .collect();
                let wasm_values = wasm_values?;

                // Convert WasmValue to Java array
                let java_array =
                    crate::jni::linker::wasm_values_to_java_array(&mut env, &wasm_values)?;
                Ok(java_array.as_raw())
            }
            Err(trap) => {
                // Handle Wasmtime trap
                Err(WasmtimeError::Runtime {
                    message: format!("Async function call trapped: {}", trap),
                    backtrace: None,
                })
            }
        }
    })();

    match result {
        Ok(arr) => arr,
        Err(error) => {
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// Call a function with multiple return values (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCallMultiValue(
    mut env: JNIEnv,
    _class: JClass,
    function_ptr: jlong,
    store_handle: jlong,
    params: jobjectArray,
) -> jobjectArray {
    // Defensive programming: validate function pointer
    if function_ptr == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::invalid_parameter("function_ptr cannot be null"),
        );
        return std::ptr::null_mut();
    }

    // Defensive programming: validate store handle
    if store_handle == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::invalid_parameter("store_handle cannot be null"),
        );
        return std::ptr::null_mut();
    }

    use std::os::raw::c_void;

    // Helper closure for the actual work
    let result = (|| -> WasmtimeResult<jobjectArray> {
        // Get function from FunctionHandle
        let func_handle = unsafe { &*(function_ptr as *const FunctionHandle) };
        let func = func_handle.get_func();

        // Get store reference
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        // Lock the store for reentrant access
        let mut store_lock = store.try_lock_store()?;

        // Get function type for parameter conversion
        let func_type = func.ty(&*store_lock);
        let param_types = func_type.params().collect::<Vec<_>>();

        // Convert Java parameters to Wasmtime values
        let wasmtime_params = convert_java_params_to_wasmtime_vals(&mut env, params, &param_types)?;

        // Prepare result storage
        let result_count = func_type.results().len();
        let mut results = vec![Val::I32(0); result_count];

        // Call function
        match func.call(&mut *store_lock, &wasmtime_params, &mut results) {
            Ok(()) => {
                // Convert Wasmtime Val results to WasmValue (supports multi-value returns)
                let wasm_values: Result<Vec<_>, _> = results
                    .iter()
                    .map(|val| crate::instance::core::wasmtime_val_to_wasm_value(val))
                    .collect();
                let wasm_values = wasm_values?;

                // Convert WasmValue to Java array
                let java_array =
                    crate::jni::linker::wasm_values_to_java_array(&mut env, &wasm_values)?;
                Ok(java_array.as_raw())
            }
            Err(trap) => {
                // Handle Wasmtime trap
                Err(WasmtimeError::Runtime {
                    message: format!("Function call trapped: {}", trap),
                    backtrace: None,
                })
            }
        }
    })();

    match result {
        Ok(arr) => arr,
        Err(error) => {
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// Convert a Func to its raw funcref pointer (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeFuncToRaw(
    mut env: JNIEnv,
    _class: JClass,
    function_ptr: jlong,
    store_handle: jlong,
) -> jlong {
    use std::os::raw::c_void;

    let result = (|| -> WasmtimeResult<jlong> {
        if function_ptr == 0 {
            return Err(WasmtimeError::invalid_parameter(
                "function_ptr cannot be null",
            ));
        }
        if store_handle == 0 {
            return Err(WasmtimeError::invalid_parameter(
                "store_handle cannot be null",
            ));
        }

        let func_handle = unsafe { &*(function_ptr as *const FunctionHandle) };
        let func = func_handle.get_func();
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        let raw_ptr = crate::hostfunc::core::func_to_raw(func, store)?;
        Ok(raw_ptr as jlong)
    })();

    match result {
        Ok(raw) => raw,
        Err(error) => {
            jni_utils::throw_jni_exception(&mut env, &error);
            0
        }
    }
}

/// Reconstruct a Func from a raw funcref pointer (JNI version)
///
/// Returns 0 if the raw value is null (no function at that reference)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeFuncFromRaw(
    mut env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    raw: jlong,
) -> jlong {
    use std::os::raw::c_void;

    let result = (|| -> WasmtimeResult<jlong> {
        if store_handle == 0 {
            return Err(WasmtimeError::invalid_parameter(
                "store_handle cannot be null",
            ));
        }

        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };
        let func_opt = crate::hostfunc::core::func_from_raw(store, raw as *mut c_void)?;

        match func_opt {
            Some(func) => {
                let handle = Box::new(FunctionHandle::new(func, "from_raw".to_string(), store));
                Ok(Box::into_raw(handle) as jlong)
            }
            None => Ok(0),
        }
    })();

    match result {
        Ok(ptr) => ptr,
        Err(error) => {
            jni_utils::throw_jni_exception(&mut env, &error);
            0
        }
    }
}

/// Destroy a function (JNI version)
#[no_mangle]
/// JNI binding: check if function matches a function type using subtype-aware checking.
/// param_type_codes and result_type_codes are int[] of WasmValueType ordinals.
/// Returns 1 if matches, 0 if not, -1 on error.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeFuncMatchesTy(
    mut env: JNIEnv,
    _class: JClass,
    function_ptr: jlong,
    store_handle: jlong,
    param_type_codes: jni::sys::jintArray,
    result_type_codes: jni::sys::jintArray,
) -> jint {
    let result = (|| -> crate::WasmtimeResult<bool> {
        if function_ptr == 0 {
            return Err(crate::error::WasmtimeError::from_string("Invalid function handle"));
        }
        if store_handle == 0 {
            return Err(crate::error::WasmtimeError::from_string("Invalid store handle"));
        }

        let func_handle = unsafe { &*(function_ptr as *const FunctionHandle) };
        let func = func_handle.get_func();
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut std::ffi::c_void)? };

        let param_codes: Vec<i32> = if !param_type_codes.is_null() {
            let arr = unsafe { jni::objects::JIntArray::from_raw(param_type_codes) };
            let len = env.get_array_length(&arr).unwrap_or(0) as usize;
            let mut buf = vec![0i32; len];
            if len > 0 {
                let _ = env.get_int_array_region(&arr, 0, &mut buf);
            }
            buf
        } else {
            Vec::new()
        };

        let result_codes: Vec<i32> = if !result_type_codes.is_null() {
            let arr = unsafe { jni::objects::JIntArray::from_raw(result_type_codes) };
            let len = env.get_array_length(&arr).unwrap_or(0) as usize;
            let mut buf = vec![0i32; len];
            if len > 0 {
                let _ = env.get_int_array_region(&arr, 0, &mut buf);
            }
            buf
        } else {
            Vec::new()
        };

        crate::instance::core::func_matches_ty(func, store, &param_codes, &result_codes)
    })();

    match result {
        Ok(true) => 1,
        Ok(false) => 0,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            -1
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeDestroyFunction(
    _env: JNIEnv,
    _class: JClass,
    function_ptr: jlong,
) {
    if function_ptr != 0 {
        unsafe {
            // Free the FunctionHandle, not raw Func - FunctionHandle wraps the Func
            let _ = Box::from_raw(function_ptr as *mut FunctionHandle);
        }
    }
}

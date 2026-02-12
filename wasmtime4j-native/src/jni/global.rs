//! JNI bindings for WebAssembly global variables

use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jboolean, jbyteArray, jint, jlong, jlongArray, jobject};
use jni::JNIEnv;

use crate::error::{ffi_utils, jni_utils, WasmtimeError, WasmtimeResult};
use crate::global::core;
use crate::store::Store;
use wasmtime::{Mutability, RefType, ValType};

/// Helper function to check ValType equality (since ValType doesn't implement PartialEq)
fn val_type_matches(val_type: &ValType, expected: &ValType) -> bool {
    match (val_type, expected) {
        (ValType::I32, ValType::I32) => true,
        (ValType::I64, ValType::I64) => true,
        (ValType::F32, ValType::F32) => true,
        (ValType::F64, ValType::F64) => true,
        (ValType::V128, ValType::V128) => true,
        (ValType::Ref(_), ValType::Ref(_)) => true, // Simplified ref type checking
        _ => false,
    }
}

/// Create a new WebAssembly global variable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeCreateGlobal(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
    value_type: jint,
    mutability: jint,
    i32_value: jint,
    i64_value: jlong,
    f32_value: f64,
    f64_value: f64,
    ref_id_present: jboolean,
    ref_id: jlong,
    name: JString,
) -> jlong {
    // Extract JNI string data first
    let name_str = if name.is_null() {
        None
    } else {
        match env.get_string(&name) {
            Ok(s) => Some(s.into()),
            Err(_) => return 0 as jlong,
        }
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };

        let val_type = match value_type {
            0 => ValType::I32,
            1 => ValType::I64,
            2 => ValType::F32,
            3 => ValType::F64,
            4 => ValType::V128,
            5 => ValType::Ref(RefType::FUNCREF),
            6 => ValType::Ref(RefType::EXTERNREF),
            _ => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid value type: {}", value_type),
                })
            }
        };

        let mutability_enum = match mutability {
            0 => Mutability::Const,
            1 => Mutability::Var,
            _ => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid mutability: {}", mutability),
                })
            }
        };

        let ref_id_opt = if ref_id_present != 0 {
            Some(ref_id as u64)
        } else {
            None
        };

        let initial_value = core::create_global_value(
            val_type.clone(),
            i32_value,
            i64_value,
            f32_value as f32,
            f64_value,
            None, // v128_bytes - not supported in this call path
            ref_id_opt,
        )?;

        let global =
            core::create_global(store, val_type, mutability_enum, initial_value, name_str)?;

        Ok(global)
    }) as jlong
}

/// Get global variable value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetGlobal(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
    store_ptr: jlong,
) -> jbyteArray {
    match (|| -> WasmtimeResult<jbyteArray> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };

        let value = core::get_global_value(global, store)?;
        let (i32_val, i64_val, f32_val, f64_val, ref_id_opt) = core::extract_global_value(&value);

        // Pack the values into a byte array
        let mut data = Vec::with_capacity(29); // 5 * 8 bytes for values + 1 byte for presence flag
        data.extend_from_slice(&i32_val.to_le_bytes());
        data.extend_from_slice(&i64_val.to_le_bytes());
        data.extend_from_slice(&f32_val.to_le_bytes());
        data.extend_from_slice(&f64_val.to_le_bytes());
        data.push(if ref_id_opt.is_some() { 1 } else { 0 });
        data.extend_from_slice(&ref_id_opt.unwrap_or(0).to_le_bytes());

        let byte_array =
            env.new_byte_array(data.len() as i32)
                .map_err(|e| WasmtimeError::Memory {
                    message: format!("Failed to create byte array: {}", e),
                })?;
        let raw_array = byte_array.as_raw();
        env.set_byte_array_region(
            &byte_array,
            0,
            &data.iter().map(|&b| b as i8).collect::<Vec<i8>>(),
        )
        .map_err(|e| WasmtimeError::Memory {
            message: format!("Failed to set byte array region: {}", e),
        })?;

        Ok(raw_array)
    })() {
        Ok(result) => result,
        Err(_) => std::ptr::null_mut() as jbyteArray, // Return null on error
    }
}

/// Set global variable value (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetGlobal(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
    store_ptr: jlong,
    value_type: jint,
    i32_value: jint,
    i64_value: jlong,
    f32_value: f64,
    f64_value: f64,
    ref_id_present: jboolean,
    ref_id: jlong,
) -> jint {
    jni_utils::jni_try_code(&mut env, || {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };

        let val_type = match value_type {
            0 => ValType::I32,
            1 => ValType::I64,
            2 => ValType::F32,
            3 => ValType::F64,
            4 => ValType::V128,
            5 => ValType::Ref(RefType::FUNCREF),
            6 => ValType::Ref(RefType::EXTERNREF),
            _ => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid value type: {}", value_type),
                })
            }
        };

        let ref_id_opt = if ref_id_present != 0 {
            Some(ref_id as u64)
        } else {
            None
        };

        let value = core::create_global_value(
            val_type,
            i32_value,
            i64_value,
            f32_value as f32,
            f64_value,
            None, // v128_bytes - not supported in this call path
            ref_id_opt,
        )?;

        core::set_global_value(global, store, value)?;

        Ok(())
    })
}

/// Get global variable metadata (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetMetadata(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
) -> jbyteArray {
    match (|| -> WasmtimeResult<jbyteArray> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let metadata = core::get_global_metadata(global);

        let mut data = Vec::with_capacity(9); // 2 ints + 1 byte for name presence

        let value_type_code = match metadata.value_type {
            ValType::I32 => 0,
            ValType::I64 => 1,
            ValType::F32 => 2,
            ValType::F64 => 3,
            ValType::V128 => 4,
            ValType::Ref(_) => 5, // Generic ref type for now
        };
        data.extend_from_slice(&(value_type_code as i32).to_le_bytes());

        let mutability_code = match metadata.mutability {
            Mutability::Const => 0,
            Mutability::Var => 1,
        };
        data.extend_from_slice(&(mutability_code as i32).to_le_bytes());

        data.push(if metadata.name.is_some() { 1 } else { 0 });

        let byte_array =
            env.new_byte_array(data.len() as i32)
                .map_err(|e| WasmtimeError::Memory {
                    message: format!("Failed to create byte array: {}", e),
                })?;
        let raw_array = byte_array.as_raw();
        env.set_byte_array_region(
            &byte_array,
            0,
            &data.iter().map(|&b| b as i8).collect::<Vec<i8>>(),
        )
        .map_err(|e| WasmtimeError::Memory {
            message: format!("Failed to set byte array region: {}", e),
        })?;

        Ok(raw_array)
    })() {
        Ok(result) => result,
        Err(_) => std::ptr::null_mut() as jbyteArray, // Return null on error
    }
}

/// Get global variable name (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetName<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    global_ptr: jlong,
) -> JString<'a> {
    match (|| -> WasmtimeResult<JString<'a>> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let metadata = core::get_global_metadata(global);

        if let Some(ref name) = metadata.name {
            Ok(env
                .new_string(name)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to create JNI string: {}", e),
                })?)
        } else {
            Ok(JString::default())
        }
    })() {
        Ok(result) => result,
        Err(_) => JString::default(), // Return empty string on error
    }
}

/// Get the value type of a global variable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetValueType<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    global_ptr: jlong,
) -> JString<'a> {
    match (|| -> WasmtimeResult<JString<'a>> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let metadata = core::get_global_metadata(global);

        let type_string = match metadata.value_type {
            ValType::I32 => "i32",
            ValType::I64 => "i64",
            ValType::F32 => "f32",
            ValType::F64 => "f64",
            ValType::V128 => "v128",
            ValType::Ref(ref ref_type) => {
                use wasmtime::HeapType;
                match *ref_type.heap_type() {
                    HeapType::Func | HeapType::ConcreteFunc(_) => "funcref",
                    HeapType::Extern => "externref",
                    _ => "anyref",
                }
            }
        };

        Ok(env
            .new_string(type_string)
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to create JNI string: {}", e),
            })?)
    })() {
        Ok(result) => result,
        Err(_) => {
            // Return "unknown" as fallback
            env.new_string("unknown").unwrap_or_default()
        }
    }
}

/// Check if a global variable is mutable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeIsMutable(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
) -> jboolean {
    match (|| -> WasmtimeResult<bool> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let metadata = core::get_global_metadata(global);

        Ok(metadata.mutability == Mutability::Var)
    })() {
        Ok(is_mutable) => {
            if is_mutable {
                1
            } else {
                0
            }
        }
        Err(_) => 0, // Return false on error (safer default)
    }
}

/// Get the value of a global variable as Object (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetValue<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    global_ptr: jlong,
    store_ptr: jlong,
) -> jobject {
    match (|| -> WasmtimeResult<jobject> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };

        let value = core::get_global_value(global, store)?;

        // Convert GlobalValue to Java Object
        let java_value = match value {
            crate::global::GlobalValue::I32(val) => {
                let integer_class = env.find_class("java/lang/Integer")?;
                let integer_obj = env.new_object(integer_class, "(I)V", &[JValue::Int(val)])?;
                integer_obj.into_raw()
            }
            crate::global::GlobalValue::I64(val) => {
                let long_class = env.find_class("java/lang/Long")?;
                let long_obj = env.new_object(long_class, "(J)V", &[JValue::Long(val)])?;
                long_obj.into_raw()
            }
            crate::global::GlobalValue::F32(val) => {
                let float_class = env.find_class("java/lang/Float")?;
                let float_obj = env.new_object(float_class, "(F)V", &[JValue::Float(val)])?;
                float_obj.into_raw()
            }
            crate::global::GlobalValue::F64(val) => {
                let double_class = env.find_class("java/lang/Double")?;
                let double_obj = env.new_object(double_class, "(D)V", &[JValue::Double(val)])?;
                double_obj.into_raw()
            }
            crate::global::GlobalValue::V128(bytes) => {
                // Return V128 as byte array
                let byte_array = env.new_byte_array(16)?;
                env.set_byte_array_region(&byte_array, 0, &bytes.map(|b| b as i8))?;
                byte_array.into_raw()
            }
            crate::global::GlobalValue::FuncRef(opt_id) => {
                // Return FuncRef as Long (null for None)
                match opt_id {
                    Some(id) => {
                        let long_class = env.find_class("java/lang/Long")?;
                        let long_obj =
                            env.new_object(long_class, "(J)V", &[JValue::Long(id as i64)])?;
                        long_obj.into_raw()
                    }
                    None => std::ptr::null_mut(),
                }
            }
            crate::global::GlobalValue::ExternRef(opt_id) => {
                // Return ExternRef as Long (null for None)
                match opt_id {
                    Some(id) => {
                        let long_class = env.find_class("java/lang/Long")?;
                        let long_obj =
                            env.new_object(long_class, "(J)V", &[JValue::Long(id as i64)])?;
                        long_obj.into_raw()
                    }
                    None => std::ptr::null_mut(),
                }
            }
            crate::global::GlobalValue::AnyRef(opt_id) => {
                // Return AnyRef as Long (null for None)
                match opt_id {
                    Some(id) => {
                        let long_class = env.find_class("java/lang/Long")?;
                        let long_obj =
                            env.new_object(long_class, "(J)V", &[JValue::Long(id as i64)])?;
                        long_obj.into_raw()
                    }
                    None => std::ptr::null_mut(),
                }
            }
            crate::global::GlobalValue::EqRef(opt_id) => {
                // Return EqRef as Long (null for None)
                match opt_id {
                    Some(id) => {
                        let long_class = env.find_class("java/lang/Long")?;
                        let long_obj =
                            env.new_object(long_class, "(J)V", &[JValue::Long(id as i64)])?;
                        long_obj.into_raw()
                    }
                    None => std::ptr::null_mut(),
                }
            }
            crate::global::GlobalValue::I31Ref(opt_val) => {
                // Return I31Ref as Integer (null for None)
                match opt_val {
                    Some(val) => {
                        let integer_class = env.find_class("java/lang/Integer")?;
                        let integer_obj =
                            env.new_object(integer_class, "(I)V", &[JValue::Int(val)])?;
                        integer_obj.into_raw()
                    }
                    None => std::ptr::null_mut(),
                }
            }
            crate::global::GlobalValue::StructRef(opt_id) => {
                // Return StructRef as Long (null for None)
                match opt_id {
                    Some(id) => {
                        let long_class = env.find_class("java/lang/Long")?;
                        let long_obj =
                            env.new_object(long_class, "(J)V", &[JValue::Long(id as i64)])?;
                        long_obj.into_raw()
                    }
                    None => std::ptr::null_mut(),
                }
            }
            crate::global::GlobalValue::ArrayRef(opt_id) => {
                // Return ArrayRef as Long (null for None)
                match opt_id {
                    Some(id) => {
                        let long_class = env.find_class("java/lang/Long")?;
                        let long_obj =
                            env.new_object(long_class, "(J)V", &[JValue::Long(id as i64)])?;
                        long_obj.into_raw()
                    }
                    None => std::ptr::null_mut(),
                }
            }
        };

        Ok(java_value)
    })() {
        Ok(result) => result,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
    }
}

/// Get the int value of a global variable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetIntValue(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
    store_ptr: jlong,
) -> jint {
    match (|| -> WasmtimeResult<jint> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };
        let metadata = core::get_global_metadata(global);

        // Validate that this global is actually an I32 type
        if !val_type_matches(&metadata.value_type, &ValType::I32) {
            return Err(WasmtimeError::Type {
                message: format!("Global is not I32 type, got {:?}", metadata.value_type),
            });
        }

        let value = core::get_global_value(global, store)?;
        match value {
            crate::global::GlobalValue::I32(val) => Ok(val),
            _ => Err(WasmtimeError::Type {
                message: "Global value is not I32 type".to_string(),
            }),
        }
    })() {
        Ok(result) => result,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            0 // Return 0 on error
        }
    }
}

/// Get the long value of a global variable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetLongValue(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
    store_ptr: jlong,
) -> jlong {
    match (|| -> WasmtimeResult<jlong> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };
        let metadata = core::get_global_metadata(global);

        // Validate that this global is actually an I64 type
        if !val_type_matches(&metadata.value_type, &ValType::I64) {
            return Err(WasmtimeError::Type {
                message: format!("Global is not I64 type, got {:?}", metadata.value_type),
            });
        }

        let value = core::get_global_value(global, store)?;
        match value {
            crate::global::GlobalValue::I64(val) => Ok(val),
            _ => Err(WasmtimeError::Type {
                message: "Global value is not I64 type".to_string(),
            }),
        }
    })() {
        Ok(result) => result,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            0 // Return 0 on error
        }
    }
}

/// Get the float value of a global variable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetFloatValue(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
    store_ptr: jlong,
) -> f32 {
    match (|| -> WasmtimeResult<f32> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };
        let metadata = core::get_global_metadata(global);

        // Validate that this global is actually an F32 type
        if !val_type_matches(&metadata.value_type, &ValType::F32) {
            return Err(WasmtimeError::Type {
                message: format!("Global is not F32 type, got {:?}", metadata.value_type),
            });
        }

        let value = core::get_global_value(global, store)?;
        match value {
            crate::global::GlobalValue::F32(val) => Ok(val),
            _ => Err(WasmtimeError::Type {
                message: "Global value is not F32 type".to_string(),
            }),
        }
    })() {
        Ok(result) => result,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            0.0 // Return 0.0 on error
        }
    }
}

/// Get the double value of a global variable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetDoubleValue(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
    store_ptr: jlong,
) -> f64 {
    match (|| -> WasmtimeResult<f64> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };
        let metadata = core::get_global_metadata(global);

        // Validate that this global is actually an F64 type
        if !val_type_matches(&metadata.value_type, &ValType::F64) {
            return Err(WasmtimeError::Type {
                message: format!("Global is not F64 type, got {:?}", metadata.value_type),
            });
        }

        let value = core::get_global_value(global, store)?;
        match value {
            crate::global::GlobalValue::F64(val) => Ok(val),
            _ => Err(WasmtimeError::Type {
                message: "Global value is not F64 type".to_string(),
            }),
        }
    })() {
        Ok(result) => result,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            0.0 // Return 0.0 on error
        }
    }
}

/// Set the value of a global variable from Object (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetValue(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
    store_ptr: jlong,
    value: jobject,
) -> jboolean {
    match (|| -> WasmtimeResult<()> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };
        let metadata = core::get_global_metadata(global);

        // Validate that this global is mutable
        if metadata.mutability != Mutability::Var {
            return Err(WasmtimeError::Runtime {
                message: "Cannot set value on immutable global variable".to_string(),
                backtrace: None,
            });
        }

        // Convert Java Object to GlobalValue based on global type
        let java_object = unsafe { JObject::from_raw(value) };
        let global_value = match &metadata.value_type {
            ValType::I32 => {
                let int_val = env.call_method(&java_object, "intValue", "()I", &[])?.i()?;
                crate::global::GlobalValue::I32(int_val)
            }
            ValType::I64 => {
                let long_val = env
                    .call_method(&java_object, "longValue", "()J", &[])?
                    .j()?;
                crate::global::GlobalValue::I64(long_val)
            }
            ValType::F32 => {
                let float_val = env
                    .call_method(&java_object, "floatValue", "()F", &[])?
                    .f()?;
                crate::global::GlobalValue::F32(float_val)
            }
            ValType::F64 => {
                let double_val = env
                    .call_method(&java_object, "doubleValue", "()D", &[])?
                    .d()?;
                crate::global::GlobalValue::F64(double_val)
            }
            ValType::Ref(ref_type) => {
                use wasmtime::HeapType;

                // Check if the object is null
                if java_object.is_null() {
                    // Null reference
                    match *ref_type.heap_type() {
                        HeapType::Func | HeapType::ConcreteFunc(_) => {
                            crate::global::GlobalValue::FuncRef(None)
                        }
                        HeapType::Extern => crate::global::GlobalValue::ExternRef(None),
                        _ => crate::global::GlobalValue::AnyRef(None),
                    }
                } else {
                    // Non-null reference - extract the handle value
                    // Check if it's a JniFunctionReference (call getNativeHandle) or Long (call longValue)
                    let handle_val = if env.is_instance_of(
                        &java_object,
                        "ai/tegmentum/wasmtime4j/jni/JniFunctionReference",
                    )? {
                        // It's a JniFunctionReference - call getNativeHandle()
                        env.call_method(&java_object, "getNativeHandle", "()J", &[])?
                            .j()?
                    } else {
                        // It's a Long - call longValue()
                        env.call_method(&java_object, "longValue", "()J", &[])?
                            .j()?
                    };
                    let handle_id = handle_val as u64;

                    match *ref_type.heap_type() {
                        HeapType::Func | HeapType::ConcreteFunc(_) => {
                            crate::global::GlobalValue::FuncRef(Some(handle_id))
                        }
                        HeapType::Extern => crate::global::GlobalValue::ExternRef(Some(handle_id)),
                        _ => crate::global::GlobalValue::AnyRef(Some(handle_id)),
                    }
                }
            }
            _ => {
                return Err(WasmtimeError::Type {
                    message: format!(
                        "Unsupported global value type for Object conversion: {:?}",
                        metadata.value_type
                    ),
                });
            }
        };

        core::set_global_value(global, store, global_value)?;
        Ok(())
    })() {
        Ok(_) => 1, // Return true on success
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            0 // Return false on error
        }
    }
}

/// Set the int value of a global variable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetIntValue(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
    store_ptr: jlong,
    value: jint,
) -> jboolean {
    match (|| -> WasmtimeResult<()> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };
        let metadata = core::get_global_metadata(global);

        // Validate that this global is mutable and correct type
        if metadata.mutability != Mutability::Var {
            return Err(WasmtimeError::Runtime {
                message: "Cannot set value on immutable global variable".to_string(),
                backtrace: None,
            });
        }

        if !val_type_matches(&metadata.value_type, &ValType::I32) {
            return Err(WasmtimeError::Type {
                message: format!("Global is not I32 type, got {:?}", metadata.value_type),
            });
        }

        let global_value = crate::global::GlobalValue::I32(value);
        core::set_global_value(global, store, global_value)?;
        Ok(())
    })() {
        Ok(_) => 1, // Return true on success
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            0 // Return false on error
        }
    }
}

/// Set the long value of a global variable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetLongValue(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
    store_ptr: jlong,
    value: jlong,
) -> jboolean {
    match (|| -> WasmtimeResult<()> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };
        let metadata = core::get_global_metadata(global);

        // Validate that this global is mutable and correct type
        if metadata.mutability != Mutability::Var {
            return Err(WasmtimeError::Runtime {
                message: "Cannot set value on immutable global variable".to_string(),
                backtrace: None,
            });
        }

        if !val_type_matches(&metadata.value_type, &ValType::I64) {
            return Err(WasmtimeError::Type {
                message: format!("Global is not I64 type, got {:?}", metadata.value_type),
            });
        }

        let global_value = crate::global::GlobalValue::I64(value);
        core::set_global_value(global, store, global_value)?;
        Ok(())
    })() {
        Ok(_) => 1, // Return true on success
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            0 // Return false on error
        }
    }
}

/// Set the float value of a global variable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetFloatValue(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
    store_ptr: jlong,
    value: f32,
) -> jboolean {
    match (|| -> WasmtimeResult<()> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };
        let metadata = core::get_global_metadata(global);

        // Validate that this global is mutable and correct type
        if metadata.mutability != Mutability::Var {
            return Err(WasmtimeError::Runtime {
                message: "Cannot set value on immutable global variable".to_string(),
                backtrace: None,
            });
        }

        if !val_type_matches(&metadata.value_type, &ValType::F32) {
            return Err(WasmtimeError::Type {
                message: format!("Global is not F32 type, got {:?}", metadata.value_type),
            });
        }

        let global_value = crate::global::GlobalValue::F32(value);
        core::set_global_value(global, store, global_value)?;
        Ok(())
    })() {
        Ok(_) => 1, // Return true on success
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            0 // Return false on error
        }
    }
}

/// Set the double value of a global variable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetDoubleValue(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
    store_ptr: jlong,
    value: f64,
) -> jboolean {
    match (|| -> WasmtimeResult<()> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };
        let metadata = core::get_global_metadata(global);

        // Validate that this global is mutable and correct type
        if metadata.mutability != Mutability::Var {
            return Err(WasmtimeError::Runtime {
                message: "Cannot set value on immutable global variable".to_string(),
                backtrace: None,
            });
        }

        if !val_type_matches(&metadata.value_type, &ValType::F64) {
            return Err(WasmtimeError::Type {
                message: format!("Global is not F64 type, got {:?}", metadata.value_type),
            });
        }

        let global_value = crate::global::GlobalValue::F64(value);
        core::set_global_value(global, store, global_value)?;
        Ok(())
    })() {
        Ok(_) => 1, // Return true on success
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            0 // Return false on error
        }
    }
}

/// Destroy a global variable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeDestroyGlobal(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
) {
    unsafe {
        core::destroy_global(global_ptr as *mut std::os::raw::c_void);
    }
}

/// Destroy a global variable (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeDestroy(
    mut env: JNIEnv,
    _class: JClass,
    global_ptr: jlong,
) {
    unsafe {
        core::destroy_global(global_ptr as *mut std::os::raw::c_void);
    }
}

/// Get global type information directly from the global (JNI version)
/// Returns array: [valueTypeCode, isMutable(0/1)]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetGlobalTypeInfo<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    global_ptr: jlong,
) -> jlongArray {
    match (|| -> WasmtimeResult<jlongArray> {
        let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
        let metadata = core::get_global_metadata(global);

        // Map ValType to type code
        let type_code = match metadata.value_type {
            wasmtime::ValType::I32 => 0,
            wasmtime::ValType::I64 => 1,
            wasmtime::ValType::F32 => 2,
            wasmtime::ValType::F64 => 3,
            wasmtime::ValType::V128 => 4,
            wasmtime::ValType::Ref(_) => {
                // For now, all ref types map to FUNCREF (5) or EXTERNREF (6)
                // We'll use 5 as a generic ref type
                5
            }
        };

        let is_mutable = if metadata.mutability == wasmtime::Mutability::Var {
            1
        } else {
            0
        };

        // Create long array with [typeCode, isMutable]
        let result_array = env.new_long_array(2).map_err(|e| WasmtimeError::Memory {
            message: format!("Failed to create long array: {}", e),
        })?;

        let values = vec![type_code as i64, is_mutable as i64];
        env.set_long_array_region(&result_array, 0, &values)
            .map_err(|e| WasmtimeError::Memory {
                message: format!("Failed to set long array region: {}", e),
            })?;

        Ok(result_array.as_raw())
    })() {
        Ok(array) => array,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
    }
}

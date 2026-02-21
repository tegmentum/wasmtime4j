//! # JNI Bindings for WebAssembly GC Operations
//!
//! This module provides JNI (Java Native Interface) bindings for all WebAssembly GC
//! functionality, including struct operations, array operations, reference type conversions,
//! and heap management.
//!
//! ## Safety and Error Handling
//!
//! All JNI functions implement comprehensive defensive programming patterns to prevent
//! JVM crashes and ensure robust error handling. Input validation and null checks are
//! performed for all parameters.

use jni::objects::JObjectArray;
use jni::objects::{JByteArray, JClass, JObject, JString, JValue, JValueOwned};
use jni::sys::{jboolean, jint, jlong, jobject};
use jni::JNIEnv;

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::gc::WasmGcRuntime;
use crate::gc_heap::ObjectId;
use crate::gc_types::{
    ArrayTypeDefinition, FieldDefinition, FieldType, GcReferenceType, GcValue, StructTypeDefinition,
};
// FFI constants
const FFI_SUCCESS: jint = 0;
const FFI_ERROR: jint = -1;

/// JNI binding for creating a GC runtime
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_createRuntimeNative(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jlong {
    let runtime_result = create_gc_runtime_internal(engine_handle);

    match runtime_result {
        Ok(runtime) => {
            let runtime_ptr = Box::into_raw(Box::new(runtime));
            runtime_ptr as jlong
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            0
        }
    }
}

/// JNI binding for destroying a GC runtime
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_destroyRuntimeNative(
    _env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
) -> jint {
    if runtime_handle == 0 {
        return FFI_ERROR;
    }

    unsafe {
        let _ = Box::from_raw(runtime_handle as *mut WasmGcRuntime);
    }

    FFI_SUCCESS
}

/// JNI binding for registering a struct type
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_registerStructTypeNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    name: JString,
    field_names: JObjectArray,
    field_types: JObjectArray,
    field_mutabilities: JObjectArray,
) -> jint {
    let result = register_struct_type_internal(
        &mut env,
        runtime_handle,
        name,
        field_names,
        field_types,
        field_mutabilities,
    );

    match result {
        Ok(type_id) => type_id as jint,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            -1
        }
    }
}

/// JNI binding for registering an array type
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_registerArrayTypeNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    name: JString,
    element_type: jint,
    mutable: jboolean,
) -> jint {
    let result =
        register_array_type_internal(&mut env, runtime_handle, name, element_type, mutable);

    match result {
        Ok(type_id) => type_id as jint,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            -1
        }
    }
}

/// JNI binding for creating a struct instance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_structNewNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    type_id: jint,
    field_values: JObjectArray,
) -> jlong {
    let result = struct_new_internal(&mut env, runtime_handle, type_id, field_values);

    match result {
        Ok(object_id) => object_id as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            0
        }
    }
}

/// JNI binding for creating a struct instance with default values
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_structNewDefaultNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    type_id: jint,
) -> jlong {
    let result = struct_new_default_internal(&mut env, runtime_handle, type_id);

    match result {
        Ok(object_id) => object_id as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            0
        }
    }
}

/// JNI binding for getting a struct field
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_structGetNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
    field_index: jint,
) -> jobject {
    struct_get_internal(&mut env, runtime_handle, object_id, field_index)
}

/// JNI binding for setting a struct field
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_structSetNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
    field_index: jint,
    value: JObject,
) -> jint {
    let result = struct_set_internal(&mut env, runtime_handle, object_id, field_index, value);

    match result {
        Ok(_) => FFI_SUCCESS,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            FFI_ERROR
        }
    }
}

/// JNI binding for creating an array instance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_arrayNewNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    type_id: jint,
    elements: JObjectArray,
) -> jlong {
    let result = array_new_internal(&mut env, runtime_handle, type_id, elements);

    match result {
        Ok(object_id) => object_id as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            0
        }
    }
}

/// JNI binding for creating an array instance with default values
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_arrayNewDefaultNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    type_id: jint,
    length: jint,
) -> jlong {
    let result = array_new_default_internal(&mut env, runtime_handle, type_id, length);

    match result {
        Ok(object_id) => object_id as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            0
        }
    }
}

/// JNI binding for getting an array element
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_arrayGetNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
    element_index: jint,
) -> jobject {
    array_get_internal(&mut env, runtime_handle, object_id, element_index)
}

/// JNI binding for setting an array element
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_arraySetNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
    element_index: jint,
    value: JObject,
) -> jint {
    let result = array_set_internal(&mut env, runtime_handle, object_id, element_index, value);

    match result {
        Ok(_) => FFI_SUCCESS,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            FFI_ERROR
        }
    }
}

/// JNI binding for getting array length
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_arrayLenNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
) -> jint {
    let result = array_len_internal(&mut env, runtime_handle, object_id);

    match result {
        Ok(length) => length as jint,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            -1
        }
    }
}

/// JNI binding for creating an I31 instance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_i31NewNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    value: jint,
) -> jlong {
    let result = i31_new_internal(&mut env, runtime_handle, value);

    match result {
        Ok(object_id) => object_id as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            0
        }
    }
}

/// JNI binding for getting I31 value
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_i31GetNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
    signed: jboolean,
) -> jint {
    let result = i31_get_internal(&mut env, runtime_handle, object_id, signed);

    match result {
        Ok(value) => value,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            0
        }
    }
}

/// JNI binding for reference cast
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_refCastNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
    target_type: jint,
) -> jlong {
    let result = ref_cast_internal(&mut env, runtime_handle, object_id, target_type);

    match result {
        Ok(cast_object_id) => cast_object_id as jlong,
        Err(_) => {
            // Return 0 to indicate cast failure - Java code will throw ClassCastException
            0
        }
    }
}

/// JNI binding for reference test
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_refTestNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
    target_type: jint,
) -> jboolean {
    let result = ref_test_internal(&mut env, runtime_handle, object_id, target_type);

    match result {
        Ok(test_result) => {
            if test_result {
                1
            } else {
                0
            }
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            0
        }
    }
}

/// JNI binding for reference equality
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_refEqNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id1: jlong,
    object_id2: jlong,
) -> jboolean {
    let result = ref_eq_internal(&mut env, runtime_handle, object_id1, object_id2);

    match result {
        Ok(eq_result) => {
            if eq_result {
                1
            } else {
                0
            }
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            0
        }
    }
}

/// JNI binding for null check
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_refIsNullNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
) -> jboolean {
    let result = ref_is_null_internal(&mut env, runtime_handle, object_id);

    match result {
        Ok(is_null) => {
            if is_null {
                1
            } else {
                0
            }
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            0
        }
    }
}

/// JNI binding for garbage collection
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_collectGarbageNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
) -> jobject {
    collect_garbage_internal(&mut env, runtime_handle)
}

/// JNI binding for getting GC stats
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_getGcStatsNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
) -> jobject {
    get_gc_stats_internal(&mut env, runtime_handle)
}

// ========== Internal Implementation Functions ==========

fn create_gc_runtime_internal(engine_handle: jlong) -> WasmtimeResult<WasmGcRuntime> {
    if engine_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid engine handle"));
    }

    // Retrieve the actual engine from the handle and extract the inner wasmtime::Engine
    let engine = unsafe {
        crate::engine::core::get_engine_ref(engine_handle as *const std::os::raw::c_void)?
    };
    WasmGcRuntime::new(engine.inner().clone())
}

fn register_struct_type_internal(
    env: &mut JNIEnv,
    runtime_handle: jlong,
    name: JString,
    field_names: JObjectArray,
    field_types: JObjectArray,
    field_mutabilities: JObjectArray,
) -> WasmtimeResult<u32> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Convert Java strings and arrays to Rust types
    let name_str = env
        .get_string(&name)
        .map_err(|_| WasmtimeError::from_string("Failed to get struct name"))?
        .to_string_lossy()
        .to_string();

    let field_count =
        env.get_array_length(&field_names)
            .map_err(|_| WasmtimeError::from_string("Failed to get field count"))? as usize;

    let mut fields = Vec::with_capacity(field_count);

    for i in 0..field_count {
        // Get field name
        let field_name_obj = env
            .get_object_array_element(&field_names, i as i32)
            .map_err(|_| WasmtimeError::from_string("Failed to get field name"))?;

        let field_name = if !field_name_obj.is_null() {
            let field_name_jstring = JString::from(field_name_obj);
            let field_name_str = env
                .get_string(&field_name_jstring)
                .map_err(|_| WasmtimeError::from_string("Failed to convert field name"))?;
            Some(field_name_str.to_string_lossy().to_string())
        } else {
            None
        };

        // Get field type string
        let field_type_obj = env
            .get_object_array_element(&field_types, i as i32)
            .map_err(|_| WasmtimeError::from_string("Failed to get field type"))?;
        let field_type_jstring = JString::from(field_type_obj);
        let field_type_str = env
            .get_string(&field_type_jstring)
            .map_err(|_| WasmtimeError::from_string("Failed to convert field type"))?
            .to_string_lossy()
            .to_string();

        // Parse field type from string
        let field_type = parse_field_type(&field_type_str)?;

        // Get field mutability
        let field_mutability_obj = env
            .get_object_array_element(&field_mutabilities, i as i32)
            .map_err(|_| WasmtimeError::from_string("Failed to get field mutability"))?;
        let mutable = env
            .call_method(&field_mutability_obj, "booleanValue", "()Z", &[])
            .map_err(|_| WasmtimeError::from_string("Failed to get boolean value"))?;
        let mutable = match mutable {
            JValueOwned::Bool(b) => b != 0,
            _ => return Err(WasmtimeError::from_string("Invalid mutability value")),
        };

        fields.push(FieldDefinition {
            name: field_name,
            field_type,
            mutable,
            index: i as u32,
        });
    }

    let struct_def = StructTypeDefinition {
        type_id: 0, // Will be assigned by runtime
        fields,
        name: Some(name_str),
        supertype: None,
    };

    runtime.register_struct_type(struct_def)
}

fn register_array_type_internal(
    env: &mut JNIEnv,
    runtime_handle: jlong,
    name: JString,
    element_type: jint,
    mutable: jboolean,
) -> WasmtimeResult<u32> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let name_str = env
        .get_string(&name)
        .map_err(|_| WasmtimeError::from_string("Failed to get array name"))?
        .to_string_lossy()
        .to_string();

    // Convert element type
    let field_type = match element_type {
        0 => FieldType::I32,
        1 => FieldType::I64,
        2 => FieldType::F32,
        3 => FieldType::F64,
        4 => FieldType::V128,
        5 => FieldType::PackedI8,
        6 => FieldType::PackedI16,
        _ => {
            return Err(WasmtimeError::from_string(&format!(
                "Invalid element type: {}",
                element_type
            )))
        }
    };

    let array_def = ArrayTypeDefinition {
        type_id: 0, // Will be assigned by runtime
        element_type: field_type,
        mutable: mutable != 0,
        name: Some(name_str),
    };

    runtime.register_array_type(array_def)
}

fn struct_new_internal(
    env: &mut JNIEnv,
    runtime_handle: jlong,
    type_id: jint,
    field_values: JObjectArray,
) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Get struct type
    let struct_def = runtime.get_struct_type(type_id as u32)?;

    // Convert Java Object[] to Vec<GcValue>
    let field_count = env.get_array_length(&field_values)?;
    let mut values = Vec::with_capacity(field_count as usize);

    for i in 0..field_count {
        let obj = env.get_object_array_element(&field_values, i)?;
        let gc_value = convert_jobject_to_gc_value(env, obj)?;
        values.push(gc_value);
    }

    let result = runtime.struct_new(struct_def, values);

    if result.success {
        Ok(result.object_id.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn struct_new_default_internal(
    _env: &mut JNIEnv,
    runtime_handle: jlong,
    type_id: jint,
) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let struct_def = runtime.get_struct_type(type_id as u32)?;
    let result = runtime.struct_new_default(struct_def);

    if result.success {
        Ok(result.object_id.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn struct_get_internal(
    env: &mut JNIEnv,
    runtime_handle: jlong,
    object_id: jlong,
    field_index: jint,
) -> jobject {
    if runtime_handle == 0 {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/RuntimeException",
            "Invalid runtime handle",
        );
        return std::ptr::null_mut();
    }

    let runtime = unsafe { &mut *(runtime_handle as *mut WasmGcRuntime) };

    let result = runtime.struct_get(object_id as ObjectId, field_index as u32);

    if result.success {
        // Check if this is a reference type (object_id is set)
        if let Some(ref_object_id) = result.object_id {
            // Create a GcReferenceMarker object to indicate this is a reference ID
            // GcReferenceMarker is a simple wrapper class with a long field
            match env.new_object(
                "ai/tegmentum/wasmtime4j/jni/JniGcRuntime$GcReferenceMarker",
                "(J)V",
                &[JValue::Long(ref_object_id as jlong)],
            ) {
                Ok(obj) => obj.into_raw(),
                Err(_) => std::ptr::null_mut(),
            }
        } else if let Some(value) = result.value {
            convert_gc_value_to_jobject(env, &value)
        } else {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                "Struct field returned no value",
            );
            std::ptr::null_mut()
        }
    } else {
        let error_msg = result
            .error
            .unwrap_or_else(|| "Unknown error accessing struct field".to_string());
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/RuntimeException",
            error_msg,
        );
        std::ptr::null_mut()
    }
}

fn struct_set_internal(
    env: &mut JNIEnv,
    runtime_handle: jlong,
    object_id: jlong,
    field_index: jint,
    value: JObject,
) -> WasmtimeResult<()> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Convert Java value to GcValue
    let gc_value = convert_jobject_to_gc_value(env, value)?;

    let result = runtime.struct_set(object_id as ObjectId, field_index as u32, gc_value);

    if result.success {
        Ok(())
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn array_new_internal(
    env: &mut JNIEnv,
    runtime_handle: jlong,
    type_id: jint,
    elements: JObjectArray,
) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Get array type
    let array_def = runtime.get_array_type(type_id as u32)?;

    // Convert Java Object[] to Vec<GcValue>
    let element_count = env.get_array_length(&elements)?;
    let mut gc_elements = Vec::with_capacity(element_count as usize);

    for i in 0..element_count {
        let obj = env.get_object_array_element(&elements, i)?;
        let gc_value = convert_jobject_to_gc_value(env, obj)?;
        gc_elements.push(gc_value);
    }

    let result = runtime.array_new(array_def, gc_elements);

    if result.success {
        Ok(result.object_id.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn array_new_default_internal(
    _env: &mut JNIEnv,
    runtime_handle: jlong,
    type_id: jint,
    length: jint,
) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let array_def = runtime.get_array_type(type_id as u32)?;
    let result = runtime.array_new_default(array_def, length as u32);

    if result.success {
        Ok(result.object_id.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn array_get_internal(
    env: &mut JNIEnv,
    runtime_handle: jlong,
    object_id: jlong,
    element_index: jint,
) -> jobject {
    if runtime_handle == 0 {
        return std::ptr::null_mut();
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.array_get(object_id as ObjectId, element_index as u32);

    if result.success {
        if let Some(value) = result.value {
            convert_gc_value_to_jobject(env, &value)
        } else {
            std::ptr::null_mut()
        }
    } else {
        std::ptr::null_mut()
    }
}

fn array_set_internal(
    env: &mut JNIEnv,
    runtime_handle: jlong,
    object_id: jlong,
    element_index: jint,
    value: JObject,
) -> WasmtimeResult<()> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Convert Java value to GcValue
    let gc_value = convert_jobject_to_gc_value(env, value)?;

    let result = runtime.array_set(object_id as ObjectId, element_index as u32, gc_value);

    if result.success {
        Ok(())
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn array_len_internal(
    _env: &mut JNIEnv,
    runtime_handle: jlong,
    object_id: jlong,
) -> WasmtimeResult<u32> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.array_len(object_id as ObjectId);

    if result.success {
        Ok(result.length.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn i31_new_internal(
    _env: &mut JNIEnv,
    runtime_handle: jlong,
    value: jint,
) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.i31_new(value);

    if result.success {
        Ok(result.cast_result.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn i31_get_internal(
    _env: &mut JNIEnv,
    runtime_handle: jlong,
    object_id: jlong,
    signed: jboolean,
) -> WasmtimeResult<jint> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.i31_get(object_id as ObjectId, signed != 0);

    if result.success {
        if let Some(GcValue::I32(val)) = result.value {
            Ok(val)
        } else {
            Err(WasmtimeError::from_string(
                "Expected I32 value from i31.get",
            ))
        }
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn ref_cast_internal(
    _env: &mut JNIEnv,
    runtime_handle: jlong,
    object_id: jlong,
    target_type: jint,
) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Convert target_type to GcReferenceType
    let target_ref_type = match target_type {
        0 => GcReferenceType::AnyRef,
        1 => GcReferenceType::EqRef,
        2 => GcReferenceType::I31Ref,
        3 => {
            // STRUCT_REF
            GcReferenceType::StructRef(StructTypeDefinition {
                type_id: 0,
                fields: vec![],
                name: None,
                supertype: None,
            })
        }
        4 => {
            // ARRAY_REF
            GcReferenceType::ArrayRef(Box::new(ArrayTypeDefinition {
                type_id: 0,
                name: None,
                element_type: FieldType::I32,
                mutable: true,
            }))
        }
        _ => {
            return Err(WasmtimeError::from_string(&format!(
                "Invalid target type: {}",
                target_type
            )))
        }
    };

    let result = runtime.ref_cast(object_id as ObjectId, target_ref_type);

    if result.success {
        Ok(result.cast_result.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn ref_test_internal(
    _env: &mut JNIEnv,
    runtime_handle: jlong,
    object_id: jlong,
    target_type: jint,
) -> WasmtimeResult<bool> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Convert target_type to GcReferenceType
    let target_ref_type = match target_type {
        0 => GcReferenceType::AnyRef,
        1 => GcReferenceType::EqRef,
        2 => GcReferenceType::I31Ref,
        3 => {
            // STRUCT_REF - need to create a placeholder struct definition
            // For type testing, we don't need the exact struct type
            GcReferenceType::StructRef(StructTypeDefinition {
                type_id: 0,
                fields: vec![],
                name: None,
                supertype: None,
            })
        }
        4 => {
            // ARRAY_REF - need to create a placeholder array definition
            // For type testing, we don't need the exact array type
            GcReferenceType::ArrayRef(Box::new(ArrayTypeDefinition {
                type_id: 0,
                name: None,
                element_type: FieldType::I32,
                mutable: true,
            }))
        }
        _ => {
            return Err(WasmtimeError::from_string(&format!(
                "Invalid target type: {}",
                target_type
            )))
        }
    };

    let result = runtime.ref_test(object_id as ObjectId, target_ref_type);

    if result.success {
        Ok(result.test_result.unwrap_or(false))
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn ref_eq_internal(
    _env: &mut JNIEnv,
    runtime_handle: jlong,
    object_id1: jlong,
    object_id2: jlong,
) -> WasmtimeResult<bool> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.ref_eq(
        if object_id1 == 0 {
            None
        } else {
            Some(object_id1 as ObjectId)
        },
        if object_id2 == 0 {
            None
        } else {
            Some(object_id2 as ObjectId)
        },
    );

    if result.success {
        Ok(result.eq_result.unwrap_or(false))
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn ref_is_null_internal(
    _env: &mut JNIEnv,
    runtime_handle: jlong,
    object_id: jlong,
) -> WasmtimeResult<bool> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.ref_is_null(if object_id == 0 {
        None
    } else {
        Some(object_id as ObjectId)
    });

    if result.success {
        Ok(result.is_null.unwrap_or(true))
    } else {
        Err(WasmtimeError::from_string(
            &result.error.unwrap_or_default(),
        ))
    }
}

fn collect_garbage_internal(env: &mut JNIEnv, runtime_handle: jlong) -> jobject {
    if runtime_handle == 0 {
        return std::ptr::null_mut();
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let collection_result = match runtime.collect_garbage() {
        Ok(result) => result,
        Err(_) => return std::ptr::null_mut(),
    };

    // Create HashMap to hold collection result data
    let hashmap_obj = match env.new_object("java/util/HashMap", "()V", &[]) {
        Ok(obj) => obj,
        Err(_) => return std::ptr::null_mut(),
    };

    put_long_in_map(
        env,
        &hashmap_obj,
        "objectsCollected",
        collection_result.objects_collected as i64,
    );
    put_long_in_map(
        env,
        &hashmap_obj,
        "bytesCollected",
        collection_result.bytes_collected as i64,
    );

    hashmap_obj.into_raw()
}

fn get_gc_stats_internal(env: &mut JNIEnv, runtime_handle: jlong) -> jobject {
    if runtime_handle == 0 {
        return std::ptr::null_mut();
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let gc_stats = match runtime.get_heap_stats() {
        Ok(stats) => stats,
        Err(_) => return std::ptr::null_mut(),
    };

    // Create HashMap to hold stats data
    let hashmap_obj = match env.new_object("java/util/HashMap", "()V", &[]) {
        Ok(obj) => obj,
        Err(_) => return std::ptr::null_mut(),
    };

    put_long_in_map(
        env,
        &hashmap_obj,
        "totalAllocated",
        gc_stats.total_allocated as i64,
    );
    put_long_in_map(
        env,
        &hashmap_obj,
        "currentHeapSize",
        gc_stats.current_heap_size as i64,
    );
    put_long_in_map(
        env,
        &hashmap_obj,
        "majorCollections",
        gc_stats.major_collections as i64,
    );

    hashmap_obj.into_raw()
}

// === Array Copy and Fill Operations ===

/// JNI binding for array copy (array.copy)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_arrayCopyNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    source_object_id: jlong,
    source_index: jint,
    dest_object_id: jlong,
    dest_index: jint,
    length: jint,
) -> jint {
    let result = array_copy_internal(
        &mut env,
        runtime_handle,
        source_object_id,
        source_index,
        dest_object_id,
        dest_index,
        length,
    );

    match result {
        Ok(_) => FFI_SUCCESS,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            FFI_ERROR
        }
    }
}

/// JNI binding for array fill (array.fill)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGcRuntime_arrayFillNative(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
    start_index: jint,
    length: jint,
    value: JObject,
) -> jint {
    let result = array_fill_internal(
        &mut env,
        runtime_handle,
        object_id,
        start_index,
        length,
        value,
    );

    match result {
        Ok(_) => FFI_SUCCESS,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/RuntimeException",
                e.to_string(),
            );
            FFI_ERROR
        }
    }
}

// Helper to create GC stats as a HashMap - Java side uses Builder to create GcStats

// === Internal implementations for array operations ===

fn array_copy_internal(
    _env: &mut JNIEnv,
    runtime_handle: jlong,
    source_object_id: jlong,
    source_index: jint,
    dest_object_id: jlong,
    dest_index: jint,
    length: jint,
) -> WasmtimeResult<()> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    if source_index < 0 || dest_index < 0 || length < 0 {
        return Err(WasmtimeError::from_string(
            "Array indices and length must be non-negative",
        ));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.array_copy(
        dest_object_id as ObjectId,
        dest_index as u32,
        source_object_id as ObjectId,
        source_index as u32,
        length as u32,
    );

    if result.success {
        Ok(())
    } else {
        Err(WasmtimeError::from_string(
            &result
                .error
                .unwrap_or_else(|| "Array copy failed".to_string()),
        ))
    }
}

fn array_fill_internal(
    env: &mut JNIEnv,
    runtime_handle: jlong,
    object_id: jlong,
    start_index: jint,
    length: jint,
    value: JObject,
) -> WasmtimeResult<()> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    if start_index < 0 || length < 0 {
        return Err(WasmtimeError::from_string(
            "Start index and length must be non-negative",
        ));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Convert Java value to GcValue
    let gc_value = convert_jobject_to_gc_value(env, value)?;

    let result = runtime.array_fill(
        object_id as ObjectId,
        start_index as u32,
        gc_value,
        length as u32,
    );

    if result.success {
        Ok(())
    } else {
        Err(WasmtimeError::from_string(
            &result
                .error
                .unwrap_or_else(|| "Array fill failed".to_string()),
        ))
    }
}

// Helper function to put a long value into a HashMap
fn put_long_in_map(env: &mut JNIEnv, map: &JObject, key: &str, value: i64) {
    let key_str = match env.new_string(key) {
        Ok(s) => s,
        Err(_) => return,
    };
    let value_obj = match env.new_object("java/lang/Long", "(J)V", &[JValue::Long(value)]) {
        Ok(v) => v,
        Err(_) => return,
    };
    let _ = env.call_method(
        map,
        "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
        &[JValue::Object(&key_str.into()), JValue::Object(&value_obj)],
    );
}



/// Parse FieldType from string representation
fn parse_field_type(type_str: &str) -> WasmtimeResult<FieldType> {
    match type_str.to_lowercase().as_str() {
        "i32" => Ok(FieldType::I32),
        "i64" => Ok(FieldType::I64),
        "f32" => Ok(FieldType::F32),
        "f64" => Ok(FieldType::F64),
        "v128" => Ok(FieldType::V128),
        "i8" | "packedi8" => Ok(FieldType::PackedI8),
        "i16" | "packedi16" => Ok(FieldType::PackedI16),
        // Reference types
        "anyref" | "anyref?" => Ok(FieldType::Reference(GcReferenceType::AnyRef)),
        "eqref" | "eqref?" => Ok(FieldType::Reference(GcReferenceType::EqRef)),
        "i31ref" | "i31ref?" => Ok(FieldType::Reference(GcReferenceType::I31Ref)),
        "structref" | "structref?" => Ok(FieldType::Reference(GcReferenceType::StructRef(
            StructTypeDefinition {
                type_id: 0,
                fields: vec![],
                name: None,
                supertype: None,
            },
        ))),
        "arrayref" | "arrayref?" => Ok(FieldType::Reference(GcReferenceType::ArrayRef(Box::new(
            ArrayTypeDefinition {
                type_id: 0,
                name: None,
                element_type: FieldType::I32,
                mutable: true,
            },
        )))),
        s if s.starts_with("reference(") || s.starts_with("ref(") => {
            // For now, return a basic AnyRef - full reference type parsing can be added later
            Ok(FieldType::Reference(GcReferenceType::AnyRef))
        }
        _ => Err(WasmtimeError::from_string(&format!(
            "Unknown field type: {}",
            type_str
        ))),
    }
}

/// Convert a Java object to a GcValue
fn convert_jobject_to_gc_value(env: &mut JNIEnv, obj: JObject) -> WasmtimeResult<GcValue> {
    if obj.is_null() {
        return Ok(GcValue::Null);
    }

    // Check for Integer
    if let Ok(true) = env.is_instance_of(&obj, "java/lang/Integer") {
        let value = env.call_method(&obj, "intValue", "()I", &[])?;
        if let JValueOwned::Int(i) = value {
            return Ok(GcValue::I32(i));
        }
    }

    // Check for Long
    if let Ok(true) = env.is_instance_of(&obj, "java/lang/Long") {
        let value = env.call_method(&obj, "longValue", "()J", &[])?;
        if let JValueOwned::Long(l) = value {
            return Ok(GcValue::I64(l));
        }
    }

    // Check for Float
    if let Ok(true) = env.is_instance_of(&obj, "java/lang/Float") {
        let value = env.call_method(&obj, "floatValue", "()F", &[])?;
        if let JValueOwned::Float(f) = value {
            return Ok(GcValue::F32(f));
        }
    }

    // Check for Double
    if let Ok(true) = env.is_instance_of(&obj, "java/lang/Double") {
        let value = env.call_method(&obj, "doubleValue", "()D", &[])?;
        if let JValueOwned::Double(d) = value {
            return Ok(GcValue::F64(d));
        }
    }

    // Check for byte array (V128)
    if let Ok(true) = env.is_instance_of(&obj, "[B") {
        let byte_array = JByteArray::from(obj);
        let length = env.get_array_length(&byte_array)?;
        if length == 16 {
            let mut bytes = vec![0i8; 16];
            env.get_byte_array_region(&byte_array, 0, &mut bytes)?;
            // Vec has exactly 16 elements due to the length check above
            let unsigned_bytes: [u8; 16] = bytes
                .iter()
                .map(|&b| b as u8)
                .collect::<Vec<_>>()
                .try_into()
                .expect("Vec has exactly 16 elements from length check");
            return Ok(GcValue::V128(unsigned_bytes));
        }
    }

    Err(WasmtimeError::from_string(
        "Unsupported Java object type for GcValue conversion",
    ))
}

/// Convert a GcValue to a Java object
fn convert_gc_value_to_jobject(env: &mut JNIEnv, gc_value: &GcValue) -> jobject {
    match gc_value {
        GcValue::I32(i) => match env.new_object("java/lang/Integer", "(I)V", &[JValue::Int(*i)]) {
            Ok(obj) => obj.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        GcValue::I64(i) => match env.new_object("java/lang/Long", "(J)V", &[JValue::Long(*i)]) {
            Ok(obj) => obj.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        GcValue::F32(f) => match env.new_object("java/lang/Float", "(F)V", &[JValue::Float(*f)]) {
            Ok(obj) => obj.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        GcValue::F64(f) => {
            match env.new_object("java/lang/Double", "(D)V", &[JValue::Double(*f)]) {
                Ok(obj) => obj.into_raw(),
                Err(_) => std::ptr::null_mut(),
            }
        }
        GcValue::V128(bytes) => {
            let byte_array = match env.new_byte_array(16) {
                Ok(arr) => arr,
                Err(_) => return std::ptr::null_mut(),
            };

            let signed_bytes: Vec<i8> = bytes.iter().map(|&b| b as i8).collect();
            if env
                .set_byte_array_region(&byte_array, 0, &signed_bytes)
                .is_err()
            {
                return std::ptr::null_mut();
            }

            JObject::from(byte_array).into_raw()
        }
        GcValue::Reference | GcValue::Null => std::ptr::null_mut(),
    }
}

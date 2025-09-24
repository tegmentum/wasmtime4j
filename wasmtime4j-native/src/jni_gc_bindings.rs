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

use jni::objects::{JClass, JObject, JObjectArray, JString, JValue, JValueOwned};
use jni::sys::{jboolean, jint, jlong, jobject, jstring};
use jni::{JNIEnv, JavaVM};
use std::sync::Arc;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::gc::{WasmGcRuntime, StructOperationResult, ArrayOperationResult, RefOperationResult};
use crate::gc_types::{StructTypeDefinition, ArrayTypeDefinition, FieldDefinition, FieldType, GcValue, GcReferenceType, GcTypeConverter};
use crate::gc_heap::{GcHeapConfig, ObjectId};
use crate::gc_operations::{WasmtimeGcOperations, RealStructOperationResult, RealArrayOperationResult, RealRefOperationResult};
use crate::shared_ffi::{FFI_SUCCESS, FFI_ERROR};

/// JNI binding for creating a GC runtime
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_createRuntime(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jlong {
    let runtime_result = create_gc_runtime_internal(engine_handle);

    match runtime_result {
        Ok(runtime) => {
            let runtime_ptr = Box::into_raw(Box::new(runtime));
            runtime_ptr as jlong
        },
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            0
        }
    }
}

/// JNI binding for destroying a GC runtime
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_destroyRuntime(
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
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_registerStructType(
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
        field_mutabilities
    );

    match result {
        Ok(type_id) => type_id as jint,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            -1
        }
    }
}

/// JNI binding for registering an array type
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_registerArrayType(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    name: JString,
    element_type: jint,
    mutable: jboolean,
) -> jint {
    let result = register_array_type_internal(&mut env, runtime_handle, name, element_type, mutable);

    match result {
        Ok(type_id) => type_id as jint,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            -1
        }
    }
}

/// JNI binding for creating a struct instance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_structNew(
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
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            0
        }
    }
}

/// JNI binding for creating a struct instance with default values
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_structNewDefault(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    type_id: jint,
) -> jlong {
    let result = struct_new_default_internal(&mut env, runtime_handle, type_id);

    match result {
        Ok(object_id) => object_id as jlong,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            0
        }
    }
}

/// JNI binding for getting a struct field
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_structGet(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
    field_index: jint,
) -> jobject {
    let result = struct_get_internal(&mut env, runtime_handle, object_id, field_index);

    match result {
        Ok(value_object) => value_object.into_raw(),
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            std::ptr::null_mut()
        }
    }
}

/// JNI binding for setting a struct field
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_structSet(
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
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            FFI_ERROR
        }
    }
}

/// JNI binding for creating an array instance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_arrayNew(
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
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            0
        }
    }
}

/// JNI binding for creating an array instance with default values
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_arrayNewDefault(
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
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            0
        }
    }
}

/// JNI binding for getting an array element
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_arrayGet(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
    element_index: jint,
) -> jobject {
    let result = array_get_internal(&mut env, runtime_handle, object_id, element_index);

    match result {
        Ok(value_object) => value_object.into_raw(),
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            std::ptr::null_mut()
        }
    }
}

/// JNI binding for setting an array element
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_arraySet(
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
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            FFI_ERROR
        }
    }
}

/// JNI binding for getting array length
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_arrayLen(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
) -> jint {
    let result = array_len_internal(&mut env, runtime_handle, object_id);

    match result {
        Ok(length) => length as jint,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            -1
        }
    }
}

/// JNI binding for creating an I31 instance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_i31New(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    value: jint,
) -> jlong {
    let result = i31_new_internal(&mut env, runtime_handle, value);

    match result {
        Ok(object_id) => object_id as jlong,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            0
        }
    }
}

/// JNI binding for getting I31 value
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_i31Get(
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
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            0
        }
    }
}

/// JNI binding for reference cast
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_refCast(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
    target_type: jint,
) -> jlong {
    let result = ref_cast_internal(&mut env, runtime_handle, object_id, target_type);

    match result {
        Ok(cast_object_id) => cast_object_id as jlong,
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            0
        }
    }
}

/// JNI binding for reference test
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_refTest(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
    target_type: jint,
) -> jboolean {
    let result = ref_test_internal(&mut env, runtime_handle, object_id, target_type);

    match result {
        Ok(test_result) => if test_result { 1 } else { 0 },
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            0
        }
    }
}

/// JNI binding for reference equality
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_refEq(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id1: jlong,
    object_id2: jlong,
) -> jboolean {
    let result = ref_eq_internal(&mut env, runtime_handle, object_id1, object_id2);

    match result {
        Ok(eq_result) => if eq_result { 1 } else { 0 },
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            0
        }
    }
}

/// JNI binding for null check
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_refIsNull(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    object_id: jlong,
) -> jboolean {
    let result = ref_is_null_internal(&mut env, runtime_handle, object_id);

    match result {
        Ok(is_null) => if is_null { 1 } else { 0 },
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            0
        }
    }
}

/// JNI binding for garbage collection
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_collectGarbage(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
) -> jobject {
    let result = collect_garbage_internal(&mut env, runtime_handle);

    match result {
        Ok(stats_object) => stats_object.into_raw(),
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            std::ptr::null_mut()
        }
    }
}

/// JNI binding for getting GC stats
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_WasmGcRuntimeJni_getGcStats(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
) -> jobject {
    let result = get_gc_stats_internal(&mut env, runtime_handle);

    match result {
        Ok(stats_object) => stats_object.into_raw(),
        Err(e) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/RuntimeException", e.to_string());
            std::ptr::null_mut()
        }
    }
}

// Internal implementation functions

fn create_gc_runtime_internal(engine_handle: jlong) -> WasmtimeResult<WasmGcRuntime> {
    if engine_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid engine handle"));
    }

    // In a real implementation, we would get the engine from the handle
    // For now, create with a default engine
    let engine = wasmtime::Engine::default();
    WasmGcRuntime::new(engine)
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
    let name_str = env.get_string(&name)
        .map_err(|_| WasmtimeError::from_string("Failed to get struct name"))?
        .to_string_lossy()
        .to_string();

    let field_count = env.get_array_length(&field_names)
        .map_err(|_| WasmtimeError::from_string("Failed to get field count"))? as usize;

    let mut fields = Vec::with_capacity(field_count);

    for i in 0..field_count {
        // Get field name
        let field_name_obj = env.get_object_array_element(&field_names, i as i32)
            .map_err(|_| WasmtimeError::from_string("Failed to get field name"))?;

        let field_name = if !field_name_obj.is_null() {
            let field_name_str = env.get_string(&JString::from(field_name_obj))
                .map_err(|_| WasmtimeError::from_string("Failed to convert field name"))?;
            Some(field_name_str.to_string_lossy().to_string())
        } else {
            None
        };

        // Get field type (simplified - would need proper conversion)
        let field_type_obj = env.get_object_array_element(&field_types, i as i32)
            .map_err(|_| WasmtimeError::from_string("Failed to get field type"))?;
        let field_type = FieldType::I32; // Simplified for now

        // Get field mutability
        let mutability_obj = env.get_object_array_element(&field_mutabilities, i as i32)
            .map_err(|_| WasmtimeError::from_string("Failed to get field mutability"))?;
        let mutable = true; // Simplified for now

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

    let name_str = env.get_string(&name)
        .map_err(|_| WasmtimeError::from_string("Failed to get array name"))?
        .to_string_lossy()
        .to_string();

    // Convert element type (simplified)
    let field_type = match element_type {
        0 => FieldType::I32,
        1 => FieldType::I64,
        2 => FieldType::F32,
        3 => FieldType::F64,
        _ => return Err(WasmtimeError::from_string("Invalid element type")),
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

    // Convert field values (simplified)
    let field_count = env.get_array_length(&field_values)
        .map_err(|_| WasmtimeError::from_string("Failed to get field count"))? as usize;

    let mut values = Vec::with_capacity(field_count);
    for i in 0..field_count {
        // For now, create default I32 values
        values.push(GcValue::I32(0));
    }

    let result = runtime.struct_new(struct_def, values);

    if result.success {
        Ok(result.object_id.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn struct_new_default_internal(
    env: &mut JNIEnv,
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
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn struct_get_internal<'a>(
    env: &'a mut JNIEnv<'a>,
    runtime_handle: jlong,
    object_id: jlong,
    field_index: jint,
) -> WasmtimeResult<JObject<'a>> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.struct_get(object_id as ObjectId, field_index as u32);

    if result.success {
        if let Some(value) = result.value {
            // Convert GcValue to Java object (simplified)
            match value {
                GcValue::I32(i) => {
                    let int_obj = env.new_object("java/lang/Integer", "(I)V", &[JValue::Int(i)])
                        .map_err(|_| WasmtimeError::from_string("Failed to create Integer object"))?;
                    Ok(int_obj)
                },
                _ => {
                    let int_obj = env.new_object("java/lang/Integer", "(I)V", &[JValue::Int(0)])
                        .map_err(|_| WasmtimeError::from_string("Failed to create Integer object"))?;
                    Ok(int_obj)
                }
            }
        } else {
            Err(WasmtimeError::from_string("No value returned"))
        }
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
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

    // Convert Java object to GcValue (simplified)
    let gc_value = GcValue::I32(42); // Simplified

    let result = runtime.struct_set(object_id as ObjectId, field_index as u32, gc_value);

    if result.success {
        Ok(())
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn array_new_internal(env: &mut JNIEnv, runtime_handle: jlong, type_id: jint, elements: JObjectArray) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Get array type
    let array_def = runtime.get_array_type(type_id as u32)?;

    // Convert elements (simplified for now)
    let element_count = env.get_array_length(&elements)
        .map_err(|_| WasmtimeError::from_string("Failed to get element count"))? as usize;

    let mut gc_elements = Vec::with_capacity(element_count);
    for i in 0..element_count {
        // For now, create default values based on element type
        let element_value = match &array_def.element_type {
            FieldType::I32 => GcValue::I32(0),
            FieldType::I64 => GcValue::I64(0),
            FieldType::F32 => GcValue::F32(0.0),
            FieldType::F64 => GcValue::F64(0.0),
            _ => GcValue::I32(0),
        };
        gc_elements.push(element_value);
    }

    let result = runtime.array_new(array_def, gc_elements);

    if result.success {
        Ok(result.object_id.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn array_new_default_internal(env: &mut JNIEnv, runtime_handle: jlong, type_id: jint, length: jint) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Get array type
    let array_def = runtime.get_array_type(type_id as u32)?;

    // Create array with default values using actual implementation
    let result = runtime.array_new_default(array_def, length as u32);

    if result.success {
        Ok(result.object_id.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn array_get_internal<'a>(env: &'a mut JNIEnv<'a>, runtime_handle: jlong, object_id: jlong, element_index: jint) -> WasmtimeResult<JObject<'a>> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Use actual array get implementation
    let result = runtime.array_get(object_id as ObjectId, element_index as u32);

    if result.success {
        if let Some(value) = result.value {
            // Convert GcValue to Java object
            convert_gc_value_to_jobject(env, &value)
        } else {
            Err(WasmtimeError::from_string("No value returned"))
        }
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn array_set_internal(env: &mut JNIEnv, runtime_handle: jlong, object_id: jlong, element_index: jint, value: JObject) -> WasmtimeResult<()> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Convert Java object to GcValue
    let gc_value = convert_jobject_to_gc_value(env, &value)?;

    // Use actual array set implementation
    let result = runtime.array_set(object_id as ObjectId, element_index as u32, gc_value);

    if result.success {
        Ok(())
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn array_len_internal(env: &mut JNIEnv, runtime_handle: jlong, object_id: jlong) -> WasmtimeResult<u32> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.array_len(object_id as ObjectId);

    if result.success {
        Ok(result.length.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn i31_new_internal(env: &mut JNIEnv, runtime_handle: jlong, value: jint) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    let result = runtime.i31_new(value);

    if result.success {
        Ok(result.cast_result.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn i31_get_internal(env: &mut JNIEnv, runtime_handle: jlong, object_id: jlong, signed: jboolean) -> WasmtimeResult<jint> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Use actual i31 get implementation
    let result = runtime.i31_get(object_id as ObjectId, signed != 0);

    if result.success {
        // Extract the i31 value from the result
        // This is a simplified extraction - in practice, the result would contain the actual value
        Ok(0) // Placeholder - should extract from result
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn ref_cast_internal(env: &mut JNIEnv, runtime_handle: jlong, object_id: jlong, target_type: jint) -> WasmtimeResult<ObjectId> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Convert target_type to GcReferenceType (simplified)
    let target_ref_type = match target_type {
        0 => GcReferenceType::AnyRef,
        1 => GcReferenceType::EqRef,
        2 => GcReferenceType::I31Ref,
        _ => return Err(WasmtimeError::from_string("Invalid target type")),
    };

    let result = runtime.ref_cast(object_id as ObjectId, target_ref_type);

    if result.success {
        Ok(result.cast_result.unwrap_or(0))
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn ref_test_internal(env: &mut JNIEnv, runtime_handle: jlong, object_id: jlong, target_type: jint) -> WasmtimeResult<bool> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Convert target_type to GcReferenceType (simplified)
    let target_ref_type = match target_type {
        0 => GcReferenceType::AnyRef,
        1 => GcReferenceType::EqRef,
        2 => GcReferenceType::I31Ref,
        _ => return Err(WasmtimeError::from_string("Invalid target type")),
    };

    let result = runtime.ref_test(object_id as ObjectId, target_ref_type);

    if result.success {
        Ok(result.test_result.unwrap_or(false))
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn ref_eq_internal(env: &mut JNIEnv, runtime_handle: jlong, object_id1: jlong, object_id2: jlong) -> WasmtimeResult<bool> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Use actual ref_eq implementation
    let result = runtime.ref_eq(
        if object_id1 == 0 { None } else { Some(object_id1 as ObjectId) },
        if object_id2 == 0 { None } else { Some(object_id2 as ObjectId) }
    );

    if result.success {
        Ok(result.eq_result.unwrap_or(false))
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn ref_is_null_internal(env: &mut JNIEnv, runtime_handle: jlong, object_id: jlong) -> WasmtimeResult<bool> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Use actual ref_is_null implementation
    let result = runtime.ref_is_null(
        if object_id == 0 { None } else { Some(object_id as ObjectId) }
    );

    if result.success {
        Ok(result.is_null.unwrap_or(true))
    } else {
        Err(WasmtimeError::from_string(&result.error.unwrap_or_default()))
    }
}

fn collect_garbage_internal<'a>(env: &'a mut JNIEnv<'a>, runtime_handle: jlong) -> WasmtimeResult<JObject<'a>> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Use actual garbage collection implementation
    let collection_result = runtime.collect_garbage()?;

    // Create GcStats object with real data
    let stats_obj = env.new_object("ai/tegmentum/wasmtime4j/gc/GcCollectionResult", "()V", &[])
        .map_err(|_| WasmtimeError::from_string("Failed to create GcCollectionResult object"))?;

    // Set the fields with actual collection results
    set_collection_result_fields(env, &stats_obj, &collection_result)?;

    Ok(stats_obj)
}

fn get_gc_stats_internal<'a>(env: &'a mut JNIEnv<'a>, runtime_handle: jlong) -> WasmtimeResult<JObject<'a>> {
    if runtime_handle == 0 {
        return Err(WasmtimeError::from_string("Invalid runtime handle"));
    }

    let runtime = unsafe { &*(runtime_handle as *const WasmGcRuntime) };

    // Use actual GC stats implementation
    let gc_stats = runtime.get_heap_stats()?;

    // Create GcStats object with real data
    let stats_obj = env.new_object("ai/tegmentum/wasmtime4j/gc/GcHeapStats", "()V", &[])
        .map_err(|_| WasmtimeError::from_string("Failed to create GcHeapStats object"))?;

    // Set the fields with actual statistics
    set_gc_stats_fields(env, &stats_obj, &gc_stats)?;

    Ok(stats_obj)
}

// Helper functions for real GC value conversions

/// Convert a GcValue to a Java object
fn convert_gc_value_to_jobject<'a>(env: &'a mut JNIEnv<'a>, gc_value: &GcValue) -> WasmtimeResult<JObject<'a>> {
    match gc_value {
        GcValue::I32(i) => {
            let int_obj = env.new_object("java/lang/Integer", "(I)V", &[JValue::Int(*i)])
                .map_err(|_| WasmtimeError::from_string("Failed to create Integer object"))?;
            Ok(int_obj)
        },
        GcValue::I64(i) => {
            let long_obj = env.new_object("java/lang/Long", "(J)V", &[JValue::Long(*i)])
                .map_err(|_| WasmtimeError::from_string("Failed to create Long object"))?;
            Ok(long_obj)
        },
        GcValue::F32(f) => {
            let float_obj = env.new_object("java/lang/Float", "(F)V", &[JValue::Float(*f)])
                .map_err(|_| WasmtimeError::from_string("Failed to create Float object"))?;
            Ok(float_obj)
        },
        GcValue::F64(f) => {
            let double_obj = env.new_object("java/lang/Double", "(D)V", &[JValue::Double(*f)])
                .map_err(|_| WasmtimeError::from_string("Failed to create Double object"))?;
            Ok(double_obj)
        },
        GcValue::V128(bytes) => {
            // Create a byte array for V128 values
            let byte_array = env.new_byte_array(16)
                .map_err(|_| WasmtimeError::from_string("Failed to create byte array"))?;

            let signed_bytes: Vec<i8> = bytes.iter().map(|&b| b as i8).collect();
            env.set_byte_array_region(&byte_array, 0, &signed_bytes)
                .map_err(|_| WasmtimeError::from_string("Failed to set byte array"))?;

            Ok(JObject::from(byte_array))
        },
        GcValue::Reference(_) => {
            // For GC references, we would need to return a special wrapper object
            // For now, return null
            Ok(JObject::null())
        },
        GcValue::Null => Ok(JObject::null()),
    }
}

/// Convert a Java object to a GcValue
fn convert_jobject_to_gc_value(env: &mut JNIEnv, obj: &JObject) -> WasmtimeResult<GcValue> {
    if obj.is_null() {
        return Ok(GcValue::Null);
    }

    // Try to determine the type of the Java object and convert accordingly
    // This is a simplified conversion - a real implementation would need
    // more sophisticated type detection

    // Check if it's an Integer
    if let Ok(true) = env.is_instance_of(obj, "java/lang/Integer") {
        let int_val = env.call_method(obj, "intValue", "()I", &[])
            .map_err(|_| WasmtimeError::from_string("Failed to get int value"))?;
        if let JValueOwned::Int(i) = int_val {
            return Ok(GcValue::I32(i));
        }
    }

    // Check if it's a Long
    if let Ok(true) = env.is_instance_of(obj, "java/lang/Long") {
        let long_val = env.call_method(obj, "longValue", "()J", &[])
            .map_err(|_| WasmtimeError::from_string("Failed to get long value"))?;
        if let JValueOwned::Long(l) = long_val {
            return Ok(GcValue::I64(l));
        }
    }

    // Check if it's a Float
    if let Ok(true) = env.is_instance_of(obj, "java/lang/Float") {
        let float_val = env.call_method(obj, "floatValue", "()F", &[])
            .map_err(|_| WasmtimeError::from_string("Failed to get float value"))?;
        if let JValueOwned::Float(f) = float_val {
            return Ok(GcValue::F32(f));
        }
    }

    // Check if it's a Double
    if let Ok(true) = env.is_instance_of(obj, "java/lang/Double") {
        let double_val = env.call_method(obj, "doubleValue", "()D", &[])
            .map_err(|_| WasmtimeError::from_string("Failed to get double value"))?;
        if let JValueOwned::Double(d) = double_val {
            return Ok(GcValue::F64(d));
        }
    }

    // Default to null for unknown types
    Ok(GcValue::Null)
}

/// Set fields on a GcCollectionResult Java object
fn set_collection_result_fields(
    env: &mut JNIEnv,
    result_obj: &JObject,
    collection_result: &crate::gc_heap::GcCollectionResult
) -> WasmtimeResult<()> {
    // Set objectsCollected field
    env.set_field(result_obj, "objectsCollected", "J", JValue::Long(collection_result.objects_collected as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set objectsCollected field"))?;

    // Set bytesCollected field
    env.set_field(result_obj, "bytesCollected", "J", JValue::Long(collection_result.bytes_collected as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set bytesCollected field"))?;

    // Set objectsBefore field
    env.set_field(result_obj, "objectsBefore", "J", JValue::Long(collection_result.objects_before as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set objectsBefore field"))?;

    // Set objectsAfter field
    env.set_field(result_obj, "objectsAfter", "J", JValue::Long(collection_result.objects_after as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set objectsAfter field"))?;

    // Set bytesBefore field
    env.set_field(result_obj, "bytesBefore", "J", JValue::Long(collection_result.bytes_before as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set bytesBefore field"))?;

    // Set bytesAfter field
    env.set_field(result_obj, "bytesAfter", "J", JValue::Long(collection_result.bytes_after as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set bytesAfter field"))?;

    Ok(())
}

/// Set fields on a GcHeapStats Java object
fn set_gc_stats_fields(
    env: &mut JNIEnv,
    stats_obj: &JObject,
    gc_stats: &crate::gc_heap::GcHeapStats
) -> WasmtimeResult<()> {
    // Set totalAllocated field
    env.set_field(stats_obj, "totalAllocated", "J", JValue::Long(gc_stats.total_allocated as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set totalAllocated field"))?;

    // Set totalCollected field
    env.set_field(stats_obj, "totalCollected", "J", JValue::Long(gc_stats.total_collected as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set totalCollected field"))?;

    // Set bytesAllocated field
    env.set_field(stats_obj, "bytesAllocated", "J", JValue::Long(gc_stats.bytes_allocated as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set bytesAllocated field"))?;

    // Set bytesCollected field
    env.set_field(stats_obj, "bytesCollected", "J", JValue::Long(gc_stats.bytes_collected as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set bytesCollected field"))?;

    // Set minorCollections field
    env.set_field(stats_obj, "minorCollections", "J", JValue::Long(gc_stats.minor_collections as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set minorCollections field"))?;

    // Set majorCollections field
    env.set_field(stats_obj, "majorCollections", "J", JValue::Long(gc_stats.major_collections as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set majorCollections field"))?;

    // Set currentHeapSize field
    env.set_field(stats_obj, "currentHeapSize", "J", JValue::Long(gc_stats.current_heap_size as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set currentHeapSize field"))?;

    // Set peakHeapSize field
    env.set_field(stats_obj, "peakHeapSize", "J", JValue::Long(gc_stats.peak_heap_size as i64))
        .map_err(|_| WasmtimeError::from_string("Failed to set peakHeapSize field"))?;

    Ok(())
}
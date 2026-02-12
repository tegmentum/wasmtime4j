//! Exception Handling JNI bindings
//!
//! This module provides JNI bindings for WebAssembly exception handling,
//! including Tag creation, ExnRef management, and store exception operations.

use jni::objects::{JClass, JIntArray};
use jni::sys::{jboolean, jintArray, jlong, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use wasmtime::{FuncType, Tag, TagType, ValType};

use crate::error::jni_utils;
use crate::error::WasmtimeError;
use crate::store::Store;

/// Helper to convert ValType to integer code
fn val_type_to_code(ty: &ValType) -> i32 {
    use wasmtime::HeapType;
    match ty {
        ValType::I32 => 0,
        ValType::I64 => 1,
        ValType::F32 => 2,
        ValType::F64 => 3,
        ValType::V128 => 4,
        ValType::Ref(r) => {
            // Check heap_type() to determine the reference type
            match r.heap_type() {
                HeapType::Func => 5,   // funcref
                HeapType::Extern => 6, // externref
                _ => 7,                // Other reference types (anyref, eqref, etc.)
            }
        }
    }
}

/// Helper to convert integer code to ValType
fn code_to_val_type(code: i32) -> Option<ValType> {
    match code {
        0 => Some(ValType::I32),
        1 => Some(ValType::I64),
        2 => Some(ValType::F32),
        3 => Some(ValType::F64),
        4 => Some(ValType::V128),
        5 => Some(ValType::FUNCREF),
        6 => Some(ValType::EXTERNREF),
        _ => None,
    }
}

/// JNI binding for WasmRuntime.nativeCreateTag
/// Creates a new Tag with the given parameter types
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateTag<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    store_handle: jlong,
    param_types: jintArray,
    _return_types: jintArray,
) -> jlong {
    // Validate inputs first
    if store_handle == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            },
        );
        return 0;
    }

    let store = unsafe { &*(store_handle as *const Store) };

    // Get parameter types from Java array
    let param_array = unsafe { JIntArray::from_raw(param_types) };
    let param_len = match env.get_array_length(&param_array) {
        Ok(len) => len as usize,
        Err(e) => {
            jni_utils::throw_jni_exception(
                &mut env,
                &WasmtimeError::Internal {
                    message: format!("Failed to get param types length: {}", e),
                },
            );
            return 0;
        }
    };

    let mut param_codes = vec![0i32; param_len];
    if param_len > 0 {
        if let Err(e) = env.get_int_array_region(&param_array, 0, &mut param_codes) {
            jni_utils::throw_jni_exception(
                &mut env,
                &WasmtimeError::Internal {
                    message: format!("Failed to read param types: {}", e),
                },
            );
            return 0;
        }
    }

    // Convert codes to ValTypes
    let params: Vec<ValType> = param_codes
        .iter()
        .filter_map(|&code| code_to_val_type(code))
        .collect();

    // Create a FuncType for the tag (tags use params only, empty results)
    let func_type = FuncType::new(store.engine().inner(), params.iter().cloned(), []);

    // Create TagType from FuncType
    let tag_type = TagType::new(func_type);

    // Lock the store and create the Tag
    let mut store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return 0;
        }
    };
    match Tag::new(&mut *store_guard, &tag_type) {
        Ok(tag) => Box::into_raw(Box::new(tag)) as jlong,
        Err(e) => {
            jni_utils::throw_jni_exception(
                &mut env,
                &WasmtimeError::Internal {
                    message: format!("Failed to create tag: {}", e),
                },
            );
            0
        }
    }
}

/// JNI binding for JniTag.nativeGetParamTypes
/// Gets the parameter types of the tag
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTag_nativeGetParamTypes<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    tag_handle: jlong,
    store_handle: jlong,
) -> jintArray {
    if tag_handle == 0 || store_handle == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::InvalidParameter {
                message: "Tag or store handle cannot be null".to_string(),
            },
        );
        return std::ptr::null_mut();
    }

    let tag = unsafe { &*(tag_handle as *const Tag) };
    let store = unsafe { &*(store_handle as *const Store) };

    // Lock the store and get the tag type
    let store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return std::ptr::null_mut();
        }
    };
    let tag_type = tag.ty(&*store_guard);

    // TagType has ty() which returns &FuncType, and FuncType has params()
    let func_type = tag_type.ty();
    let params: Vec<i32> = func_type.params().map(|ty| val_type_to_code(&ty)).collect();

    // Create Java int array and fill it
    let arr = match env.new_int_array(params.len() as i32) {
        Ok(arr) => arr,
        Err(e) => {
            jni_utils::throw_jni_exception(
                &mut env,
                &WasmtimeError::Internal {
                    message: format!("Failed to create int array: {}", e),
                },
            );
            return std::ptr::null_mut();
        }
    };

    if !params.is_empty() {
        if let Err(e) = env.set_int_array_region(&arr, 0, &params) {
            jni_utils::throw_jni_exception(
                &mut env,
                &WasmtimeError::Internal {
                    message: format!("Failed to set int array: {}", e),
                },
            );
            return std::ptr::null_mut();
        }
    }

    arr.into_raw()
}

/// JNI binding for JniTag.nativeGetReturnTypes
/// Tags don't have return types in wasmtime - returns empty array
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTag_nativeGetReturnTypes<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    _tag_handle: jlong,
    _store_handle: jlong,
) -> jintArray {
    // Tags in wasmtime don't have return types - they only have params
    match env.new_int_array(0) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// JNI binding for JniTag.nativeEquals
/// Compares two tags for equality
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTag_nativeEquals(
    mut env: JNIEnv,
    _class: JClass,
    tag1_handle: jlong,
    tag2_handle: jlong,
    store_handle: jlong,
) -> jboolean {
    let result = jni_utils::jni_try_bool(&mut env, || {
        if tag1_handle == 0 || tag2_handle == 0 || store_handle == 0 {
            return Ok(false);
        }

        let tag1 = unsafe { &*(tag1_handle as *const Tag) };
        let tag2 = unsafe { &*(tag2_handle as *const Tag) };
        let store = unsafe { &*(store_handle as *const Store) };

        // Lock the store and compare tags
        let store_guard = store.try_lock_store()?;
        Ok(Tag::eq(tag1, tag2, &*store_guard))
    });

    if result {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

/// JNI binding for JniTag.nativeDestroy
/// Destroys a tag (deallocates memory)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTag_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    tag_handle: jlong,
) {
    if tag_handle != 0 {
        unsafe {
            let _ = Box::from_raw(tag_handle as *mut Tag);
        }
    }
}

/// JNI binding for JniExnRef.nativeGetTag
/// Gets the tag associated with an exception reference
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniExnRef_nativeGetTag(
    _env: JNIEnv,
    _class: JClass,
    _exnref_handle: jlong,
    _store_handle: jlong,
) -> jlong {
    // ExnRef.tag() requires Rooted<ExnRef> which is more complex to manage
    // across JNI boundary. For now, return 0 (null).
    // Full implementation would require storing ExnRef in a RootScope.
    0
}

/// JNI binding for JniExnRef.nativeIsValid
/// Checks if an exception reference is valid
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniExnRef_nativeIsValid(
    _env: JNIEnv,
    _class: JClass,
    exnref_handle: jlong,
    _store_handle: jlong,
) -> jboolean {
    // Basic validity check - non-null handle
    if exnref_handle != 0 {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

/// JNI binding for JniExnRef.nativeDestroy
/// Destroys an exception reference handle
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniExnRef_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    exnref_handle: jlong,
) {
    if exnref_handle == 0 {
        return;
    }
    unsafe {
        let _boxed = Box::from_raw(exnref_handle as *mut wasmtime::OwnedRooted<wasmtime::ExnRef>);
        // Dropping the Box cleans up the OwnedRooted
    }
}

/// JNI binding for JniStore.nativeThrowException
/// Throws an exception in the store context
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeThrowException(
    _env: JNIEnv,
    _class: JClass,
    _store_handle: jlong,
    _exnref_handle: jlong,
) {
    // Exception throwing requires careful integration with wasmtime's
    // trap handling. The Store doesn't directly expose throw_exception -
    // exceptions are thrown during WebAssembly execution via throw/throw_ref
    // instructions.
}

/// JNI binding for JniStore.nativeTakePendingException
/// Takes a pending exception from the store, returning a handle to the ExnRef
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeTakePendingException(
    mut env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let store =
            unsafe { crate::store::core::get_store_ref(store_handle as *const std::ffi::c_void)? };
        match store.take_pending_exception() {
            Some(exn_ref) => {
                // Box the OwnedRooted<ExnRef> and return as handle
                let handle = Box::into_raw(Box::new(exn_ref)) as jlong;
                Ok(handle)
            }
            None => Ok(0),
        }
    })
}

/// JNI binding for JniStore.nativeHasPendingException
/// Checks if the store has a pending exception
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeHasPendingException(
    mut env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, JNI_FALSE, || {
        let store =
            unsafe { crate::store::core::get_store_ref(store_handle as *const std::ffi::c_void)? };
        Ok(if store.has_pending_exception() {
            JNI_TRUE
        } else {
            JNI_FALSE
        })
    })
}

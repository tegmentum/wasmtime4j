//! Exception Handling JNI bindings
//!
//! This module provides JNI bindings for WebAssembly exception handling,
//! including Tag creation, ExnRef management, and store exception operations.

use jni::objects::{JClass, JIntArray};
use jni::sys::{jboolean, jintArray, jlong, jobjectArray, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use wasmtime::{FuncType, Tag, TagType, ValType};

use crate::error::jni_utils;
use crate::error::WasmtimeError;
use crate::store::Store;

/// Helper to convert ValType to integer code
fn val_type_to_code(ty: &ValType) -> i32 {
    crate::ffi_common::valtype_conversion::valtype_to_int(ty)
}

/// Helper to convert integer code to ValType
fn code_to_val_type(code: i32) -> Option<ValType> {
    crate::ffi_common::valtype_conversion::int_to_valtype(code).ok()
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
    mut env: JNIEnv,
    _class: JClass,
    exnref_handle: jlong,
    store_handle: jlong,
) -> jlong {
    use wasmtime::{ExnRef, OwnedRooted, RootScope};

    if exnref_handle == 0 || store_handle == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::InvalidParameter {
                message: "ExnRef or store handle cannot be null".to_string(),
            },
        );
        return 0;
    }

    let owned_exnref = unsafe { &*(exnref_handle as *const OwnedRooted<ExnRef>) };
    let store = unsafe { &*(store_handle as *const Store) };
    let mut store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return 0;
        }
    };

    let mut scope = RootScope::new(&mut *store_guard);
    let exnref = owned_exnref.to_rooted(&mut scope);
    match exnref.tag(&mut scope) {
        Ok(tag) => Box::into_raw(Box::new(tag)) as jlong,
        Err(e) => {
            jni_utils::throw_jni_exception(
                &mut env,
                &WasmtimeError::Internal {
                    message: format!("Failed to get tag from ExnRef: {}", e),
                },
            );
            0
        }
    }
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

/// JNI binding for JniExnRef.nativeGetField
/// Gets a single field value from an exception reference by index
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniExnRef_nativeGetField<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    exnref_handle: jlong,
    store_handle: jlong,
    index: i32,
) -> jobjectArray {
    use wasmtime::{ExnRef, OwnedRooted, RootScope};

    if exnref_handle == 0 || store_handle == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::InvalidParameter {
                message: "ExnRef or store handle cannot be null".to_string(),
            },
        );
        return std::ptr::null_mut();
    }

    let result = (|| -> Result<jobjectArray, WasmtimeError> {
        let owned_exnref = unsafe { &*(exnref_handle as *const OwnedRooted<ExnRef>) };
        let store = unsafe { &*(store_handle as *const Store) };
        let mut store_guard = store.try_lock_store()?;

        let mut scope = RootScope::new(&mut *store_guard);
        let exnref = owned_exnref.to_rooted(&mut scope);

        let val =
            exnref
                .field(&mut scope, index as usize)
                .map_err(|e| WasmtimeError::Internal {
                    message: format!("Failed to get field {} from ExnRef: {}", index, e),
                })?;

        let wasm_value = crate::instance::core::wasmtime_val_to_wasm_value(&val)?;
        let java_array = crate::jni::linker::wasm_values_to_java_array(&mut env, &[wasm_value])?;
        Ok(java_array.as_raw())
    })();

    match result {
        Ok(arr) => arr,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
    }
}

/// JNI binding for JniExnRef.nativeGetFields
/// Gets all field values from an exception reference
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniExnRef_nativeGetFields<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    exnref_handle: jlong,
    store_handle: jlong,
) -> jobjectArray {
    use wasmtime::{ExnRef, OwnedRooted, RootScope};

    if exnref_handle == 0 || store_handle == 0 {
        jni_utils::throw_jni_exception(
            &mut env,
            &WasmtimeError::InvalidParameter {
                message: "ExnRef or store handle cannot be null".to_string(),
            },
        );
        return std::ptr::null_mut();
    }

    let result = (|| -> Result<jobjectArray, WasmtimeError> {
        let owned_exnref = unsafe { &*(exnref_handle as *const OwnedRooted<ExnRef>) };
        let store = unsafe { &*(store_handle as *const Store) };
        let mut store_guard = store.try_lock_store()?;

        let mut scope = RootScope::new(&mut *store_guard);
        let exnref = owned_exnref.to_rooted(&mut scope);

        let vals: Vec<wasmtime::Val> = exnref
            .fields(&mut scope)
            .map_err(|e| WasmtimeError::Internal {
                message: format!("Failed to get fields from ExnRef: {}", e),
            })?
            .collect();

        let wasm_values: Vec<_> = vals
            .iter()
            .map(|v| crate::instance::core::wasmtime_val_to_wasm_value(v))
            .collect::<Result<Vec<_>, _>>()?;

        let java_array = crate::jni::linker::wasm_values_to_java_array(&mut env, &wasm_values)?;
        Ok(java_array.as_raw())
    })();

    match result {
        Ok(arr) => arr,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
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

/// JNI binding for JniExnRef.nativeCreate
/// Creates a new ExnRef from a tag and field values.
/// field_types is an int array of type codes, field_i64_values/field_f64_values are parallel value arrays.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniExnRef_nativeCreate<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    store_handle: jlong,
    tag_handle: jlong,
    field_types: JIntArray<'local>,
    field_i64_values: jni::objects::JLongArray<'local>,
    field_f64_values: jni::objects::JDoubleArray<'local>,
) -> jlong {
    // Extract JNI arrays outside the closure to avoid borrow conflicts
    let type_len = env.get_array_length(&field_types).unwrap_or(0) as usize;
    let mut type_codes = vec![0i32; type_len];
    let mut i64_vals = vec![0i64; type_len];
    let mut f64_vals = vec![0.0f64; type_len];

    if type_len > 0 {
        if env
            .get_int_array_region(&field_types, 0, &mut type_codes)
            .is_err()
        {
            return 0;
        }
        if env
            .get_long_array_region(&field_i64_values, 0, &mut i64_vals)
            .is_err()
        {
            return 0;
        }
        if env
            .get_double_array_region(&field_f64_values, 0, &mut f64_vals)
            .is_err()
        {
            return 0;
        }
    }

    jni_utils::jni_try_with_default(&mut env, 0, || {
        use wasmtime::{ExnRef, ExnRefPre, ExnType, RootScope, Val};

        let store =
            unsafe { crate::store::core::get_store_ref(store_handle as *const std::ffi::c_void)? };
        let tag = unsafe { &*(tag_handle as *const wasmtime::Tag) };
        let mut store_guard = store.try_lock_store()?;

        // Build field values from extracted arrays
        let mut fields = Vec::with_capacity(type_len);
        for i in 0..type_len {
            let val = match type_codes[i] {
                0 => Val::I32(i64_vals[i] as i32),
                1 => Val::I64(i64_vals[i]),
                2 => Val::F32((f64_vals[i] as f32).to_bits()),
                3 => Val::F64(f64_vals[i].to_bits()),
                _ => {
                    return Err(WasmtimeError::Internal {
                        message: format!("Unsupported field type code: {}", type_codes[i]),
                    });
                }
            };
            fields.push(val);
        }

        // Create ExnRefPre from the tag's type
        let tag_type = tag.ty(&*store_guard);
        let exn_type = ExnType::from_tag_type(&tag_type).map_err(|e| {
            WasmtimeError::Internal {
                message: format!("Failed to create ExnType from TagType: {}", e),
            }
        })?;
        let allocator = ExnRefPre::new(&mut *store_guard, exn_type);

        // Create the ExnRef
        let mut scope = RootScope::new(&mut *store_guard);
        let exnref = ExnRef::new(&mut scope, &allocator, tag, &fields).map_err(|e| {
            WasmtimeError::Internal {
                message: format!("Failed to create ExnRef: {}", e),
            }
        })?;

        // Convert to OwnedRooted and box for FFI
        let owned = exnref.to_owned_rooted(&mut scope).map_err(|e| {
            WasmtimeError::Internal {
                message: format!("Failed to convert ExnRef to owned: {}", e),
            }
        })?;

        Ok(Box::into_raw(Box::new(owned)) as jlong)
    })
}

/// JNI binding for JniExnRef.nativeToRaw
/// Converts an ExnRef to its raw u32 representation
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniExnRef_nativeToRaw(
    mut env: JNIEnv,
    _class: JClass,
    exnref_handle: jlong,
    store_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        use wasmtime::{ExnRef, OwnedRooted, RootScope};

        if exnref_handle == 0 {
            return Err(WasmtimeError::Internal {
                message: "exnref_handle is null".to_string(),
            });
        }

        let owned_exnref = unsafe { &*(exnref_handle as *const OwnedRooted<ExnRef>) };
        let store =
            unsafe { crate::store::core::get_store_ref(store_handle as *const std::ffi::c_void)? };
        let mut store_guard = store.try_lock_store()?;

        let mut scope = RootScope::new(&mut *store_guard);
        let exnref = owned_exnref.to_rooted(&mut scope);
        let raw = exnref.to_raw(&mut scope).map_err(|e| WasmtimeError::Internal {
            message: format!("Failed to convert ExnRef to raw: {}", e),
        })?;

        Ok(raw as jlong)
    })
}

/// JNI binding for JniExnRef.nativeFromRaw
/// Creates an ExnRef from a raw u32 representation
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniExnRef_nativeFromRaw(
    mut env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    raw: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        use wasmtime::{ExnRef, RootScope};

        let store =
            unsafe { crate::store::core::get_store_ref(store_handle as *const std::ffi::c_void)? };
        let mut store_guard = store.try_lock_store()?;

        let mut scope = RootScope::new(&mut *store_guard);
        match ExnRef::from_raw(&mut scope, raw as u32) {
            Some(rooted) => {
                let owned = rooted.to_owned_rooted(&mut scope).map_err(|e| {
                    WasmtimeError::Internal {
                        message: format!("Failed to convert ExnRef to owned: {}", e),
                    }
                })?;
                Ok(Box::into_raw(Box::new(owned)) as jlong)
            }
            None => Ok(0),
        }
    })
}

/// JNI binding for JniExnRef.nativeMatchesTy
/// Checks if an ExnRef matches a given heap type code
/// Returns 1 if matches, 0 if not, throws on error
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniExnRef_nativeMatchesTy(
    mut env: JNIEnv,
    _class: JClass,
    exnref_handle: jlong,
    store_handle: jlong,
    heap_type_code: jni::sys::jint,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, JNI_FALSE, || {
        use wasmtime::{ExnRef, HeapType, OwnedRooted, RootScope};

        if exnref_handle == 0 {
            return Err(WasmtimeError::Internal {
                message: "exnref_handle is null".to_string(),
            });
        }

        // Must match Java HeapType enum ordinals exactly
        let heap_type = match heap_type_code {
            0 => HeapType::Any,
            1 => HeapType::Eq,
            2 => HeapType::I31,
            3 => HeapType::Struct,
            4 => HeapType::Array,
            5 => HeapType::Func,
            6 => HeapType::NoFunc,
            7 => HeapType::Extern,
            8 => HeapType::NoExtern,
            9 => HeapType::Exn,
            10 => HeapType::NoExn,
            11 => HeapType::Cont,
            12 => HeapType::NoCont,
            13 => HeapType::None,
            _ => {
                return Err(WasmtimeError::Internal {
                    message: format!("Unknown heap type code: {}", heap_type_code),
                });
            }
        };

        let owned_exnref = unsafe { &*(exnref_handle as *const OwnedRooted<ExnRef>) };
        let store =
            unsafe { crate::store::core::get_store_ref(store_handle as *const std::ffi::c_void)? };
        let mut store_guard = store.try_lock_store()?;

        let mut scope = RootScope::new(&mut *store_guard);
        let exnref = owned_exnref.to_rooted(&mut scope);
        let matches =
            exnref
                .matches_ty(&scope, &heap_type)
                .map_err(|e| WasmtimeError::Internal {
                    message: format!("Failed to check ExnRef type match: {}", e),
                })?;

        Ok(if matches { JNI_TRUE } else { JNI_FALSE })
    })
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

//! JNI bindings for WebAssembly tables

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jbyteArray, jint, jlong, jlongArray};
use jni::JNIEnv;

use crate::error::{ffi_utils, jni_utils, WasmtimeError, WasmtimeResult};
use crate::store::Store;
use crate::table::{self, core, TableElement};
use wasmtime::{RefType, ValType};

/// Create a new WebAssembly table (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeCreateTable(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
    element_type: jint,
    initial_size: jint,
    has_maximum: jboolean,
    maximum_size: jint,
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

        let val_type = match element_type {
            5 => ValType::Ref(RefType::FUNCREF),
            6 => ValType::Ref(RefType::EXTERNREF),
            _ => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                })
            }
        };

        let max_size = if has_maximum != 0 {
            Some(maximum_size as u32)
        } else {
            None
        };

        let table = core::create_table(store, val_type, initial_size as u32, max_size, name_str)?;

        Ok(table)
    }) as jlong
}

/// Create a new 64-bit WebAssembly table (JNI version)
///
/// This creates a table with 64-bit indices as part of the Memory64 proposal.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateTable64(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
    element_type: jint,
    initial_size: jlong,
    has_maximum: jint,
    maximum_size: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")?
        };

        let val_type = match element_type {
            5 => ValType::Ref(RefType::FUNCREF),
            6 => ValType::Ref(RefType::EXTERNREF),
            _ => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                })
            }
        };

        let max_size = if has_maximum != 0 {
            Some(maximum_size as u64)
        } else {
            None
        };

        let table = core::create_table64(store, val_type, initial_size as u64, max_size, None)?;

        Ok(table)
    }) as jlong
}

/// Get table size (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetSize(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
) -> jint {
    log::debug!(
        "JNI Table.nativeGetSize: table_ptr=0x{:x}, store_ptr=0x{:x}",
        table_ptr,
        store_ptr
    );

    let result = jni_utils::jni_try_default(&env, 0, || {
        use std::os::raw::c_void;

        log::debug!("Getting table reference...");
        let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
        log::debug!("Got table reference");

        log::debug!("Getting store reference...");
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };
        log::debug!("Got store reference");

        log::debug!("Calling get_table_size...");
        let size = crate::table::core::get_table_size(table, store)?;
        log::debug!("Table size: {}", size);
        Ok(size as jint)
    });

    log::debug!("JNI Table.nativeGetSize returning: {}", result);
    result
}

/// Get table maximum size (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetMaxSize(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
) -> jint {
    jni_utils::jni_try_default(&env, -1, || {
        use std::os::raw::c_void;

        let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

        let max_size = crate::table::core::get_table_max_size(table, store)?;
        Ok(max_size as jint)
    })
}

// Note: Deprecated nativeGrow and nativeFill stubs were removed.
// Use nativeTableGrow() and nativeTableFill() which accept store context.

/// Get table metadata (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetMetadata(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
) -> jbyteArray {
    match (|| -> WasmtimeResult<jbyteArray> {
        let table = unsafe { core::get_table_ref(table_ptr as *mut std::os::raw::c_void)? };
        let metadata = core::get_table_metadata(table);

        let mut data = Vec::with_capacity(13); // 3 ints + 1 byte for name presence

        let element_type_code = match metadata.element_type {
            ValType::Ref(_) => 5, // Generic ref type for now
            _ => -1,
        };
        data.extend_from_slice(&(element_type_code as i32).to_le_bytes());
        data.extend_from_slice(&(metadata.initial_size as i32).to_le_bytes());
        data.extend_from_slice(&(metadata.maximum_size.unwrap_or(0) as i32).to_le_bytes());
        data.push(if metadata.maximum_size.is_some() {
            1
        } else {
            0
        });
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

/// Get table name (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetName<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    table_ptr: jlong,
) -> JString<'a> {
    match (|| -> WasmtimeResult<JString<'a>> {
        let table = unsafe { core::get_table_ref(table_ptr as *mut std::os::raw::c_void)? };
        let metadata = core::get_table_metadata(table);

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

/// Destroy a table (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
) {
    unsafe {
        core::destroy_table(table_ptr as *mut std::os::raw::c_void);
    }
}

/// Get table type information directly from the table (JNI version)
/// Returns array: [elementTypeCode, minimum, maximum(-1 if unlimited), is64(0 or 1)]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetTableTypeInfo<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    table_ptr: jlong,
    _store_ptr: jlong,
) -> jlongArray {
    match (|| -> WasmtimeResult<jlongArray> {
        let table = unsafe { core::get_table_ref(table_ptr as *const std::os::raw::c_void)? };
        let metadata = core::get_table_metadata(table);

        let type_code =
            crate::ffi_common::valtype_conversion::valtype_to_int(&metadata.element_type);

        let minimum = metadata.initial_size as i64;
        let maximum = metadata.maximum_size.map(|m| m as i64).unwrap_or(-1);
        let is_64 = if metadata.is_64 { 1i64 } else { 0i64 };

        // Create long array with [elementTypeCode, minimum, maximum, is64]
        let result_array = env.new_long_array(4).map_err(|e| WasmtimeError::Memory {
            message: format!("Failed to create long array: {}", e),
        })?;

        let values = vec![type_code as i64, minimum, maximum, is_64];
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

/// Grow a table by the specified number of elements (JNI version)
///
/// The `init_value` parameter is a registry ID:
/// - `0` means null reference (funcref null or externref null depending on table type)
/// - Non-zero means a registry ID for the reference
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableGrow(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
    delta: jint,
    init_value: jlong,
) -> jlong {
    jni_utils::jni_try_default(&env, -1, || {
        use std::os::raw::c_void;

        let table = unsafe { core::get_table_ref(table_ptr as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

        // Determine element type from table metadata to create the correct TableElement variant
        let metadata = core::get_table_metadata(table);
        let init_element = match &metadata.element_type {
            ValType::Ref(ref_type) => match ref_type.heap_type() {
                wasmtime::HeapType::Func => {
                    if init_value == 0 {
                        TableElement::FuncRef(None)
                    } else {
                        TableElement::FuncRef(Some(init_value as u64))
                    }
                }
                wasmtime::HeapType::Extern => {
                    if init_value == 0 {
                        TableElement::ExternRef(None)
                    } else {
                        TableElement::ExternRef(Some(init_value as u64))
                    }
                }
                _ => {
                    if init_value == 0 {
                        TableElement::AnyRef(None)
                    } else {
                        TableElement::AnyRef(Some(init_value as u64))
                    }
                }
            },
            _ => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Table has non-reference element type: {:?}",
                        metadata.element_type
                    ),
                });
            }
        };

        let prev_size = core::grow_table(table, store, delta as u32, init_element)?;
        Ok(prev_size as jlong)
    })
}

/// Grow table asynchronously (JNI version)
///
/// Requires engine with `async_support(true)`. Uses the async resource limiter.
#[no_mangle]
#[cfg(feature = "async")]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableGrowAsync(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
    delta: jint,
    init_value: jlong,
) -> jlong {
    jni_utils::jni_try_default(&env, -1, || {
        use std::os::raw::c_void;

        let table = unsafe { core::get_table_ref(table_ptr as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

        let metadata = core::get_table_metadata(table);
        let init_element = match &metadata.element_type {
            ValType::Ref(ref_type) => match ref_type.heap_type() {
                wasmtime::HeapType::Func => {
                    if init_value == 0 {
                        TableElement::FuncRef(None)
                    } else {
                        TableElement::FuncRef(Some(init_value as u64))
                    }
                }
                wasmtime::HeapType::Extern => {
                    if init_value == 0 {
                        TableElement::ExternRef(None)
                    } else {
                        TableElement::ExternRef(Some(init_value as u64))
                    }
                }
                _ => {
                    if init_value == 0 {
                        TableElement::AnyRef(None)
                    } else {
                        TableElement::AnyRef(Some(init_value as u64))
                    }
                }
            },
            _ => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Table has non-reference element type: {:?}",
                        metadata.element_type
                    ),
                });
            }
        };

        let prev_size = core::grow_table_async(table, store, delta as u32, init_element)?;
        Ok(prev_size as jlong)
    })
}

/// Fill a range of a table with the specified value (JNI version)
///
/// The `value` parameter is a registry ID:
/// - `0` means null reference
/// - Non-zero means a registry ID for the reference
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableFill(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
    dst: jint,
    value: jlong,
    len: jint,
) -> jint {
    jni_utils::jni_try_default(&env, -1, || {
        use std::os::raw::c_void;

        let table = unsafe { core::get_table_ref(table_ptr as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

        // Determine element type from table metadata to create the correct TableElement variant
        let metadata = core::get_table_metadata(table);
        let fill_element = match &metadata.element_type {
            ValType::Ref(ref_type) => match ref_type.heap_type() {
                wasmtime::HeapType::Func => {
                    if value == 0 {
                        TableElement::FuncRef(None)
                    } else {
                        TableElement::FuncRef(Some(value as u64))
                    }
                }
                wasmtime::HeapType::Extern => {
                    if value == 0 {
                        TableElement::ExternRef(None)
                    } else {
                        TableElement::ExternRef(Some(value as u64))
                    }
                }
                _ => {
                    if value == 0 {
                        TableElement::AnyRef(None)
                    } else {
                        TableElement::AnyRef(Some(value as u64))
                    }
                }
            },
            _ => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Table has non-reference element type: {:?}",
                        metadata.element_type
                    ),
                });
            }
        };

        core::fill_table(table, store, dst as u32, fill_element, len as u32)?;
        Ok(0 as jint)
    })
}

/// Check if a table supports 64-bit addressing (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeSupports64BitAddressing(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    _store_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_default(&env, 0, || {
        let table = unsafe { core::get_table_ref(table_ptr as *const std::os::raw::c_void)? };
        let metadata = core::get_table_metadata(table);
        Ok(if metadata.is_64 { 1 } else { 0 })
    })
}

/// Initialize table from element segment (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableInit(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
    instance_ptr: jlong,
    dst: jint,
    src: jint,
    len: jint,
    segment_index: jint,
) {
    jni_utils::jni_try(&mut env, || {
        let table = unsafe { core::get_table_ref(table_ptr as *const std::os::raw::c_void)? };
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *const std::os::raw::c_void, "store")?
        };
        let instance = unsafe {
            ffi_utils::deref_ptr::<crate::instance::Instance>(
                instance_ptr as *const std::os::raw::c_void,
                "instance",
            )?
        };

        table.init_from_segment(
            store,
            instance,
            dst as u32,
            src as u32,
            len as u32,
            segment_index as u32,
        )?;
        Ok(())
    });
}

/// Copy elements within a table (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeCopy(
    mut env: JNIEnv,
    _class: JClass,
    table_ptr: jlong,
    store_ptr: jlong,
    dst: jint,
    src: jint,
    count: jint,
) -> jboolean {
    let success = jni_utils::jni_try_bool(&mut env, || {
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *const std::os::raw::c_void, "store")?
        };
        let table = unsafe {
            ffi_utils::deref_ptr::<table::Table>(
                table_ptr as *const std::os::raw::c_void,
                "table",
            )?
        };
        table.copy_within(store, dst as u32, src as u32, count as u32)?;
        Ok(true)
    });
    if success { 1 } else { 0 }
}

/// Copy elements from another table (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeCopyFromTable(
    mut env: JNIEnv,
    _class: JClass,
    dst_table_ptr: jlong,
    store_ptr: jlong,
    dst: jint,
    src_table_ptr: jlong,
    src: jint,
    count: jint,
) -> jboolean {
    let success = jni_utils::jni_try_bool(&mut env, || {
        let store = unsafe {
            ffi_utils::deref_ptr::<Store>(store_ptr as *const std::os::raw::c_void, "store")?
        };
        let dst_table = unsafe {
            ffi_utils::deref_ptr::<table::Table>(
                dst_table_ptr as *const std::os::raw::c_void,
                "dst_table",
            )?
        };
        let src_table = unsafe {
            ffi_utils::deref_ptr::<table::Table>(
                src_table_ptr as *const std::os::raw::c_void,
                "src_table",
            )?
        };
        dst_table.copy_from(store, dst as u32, src_table, src as u32, count as u32)?;
        Ok(true)
    });
    if success { 1 } else { 0 }
}

/// Drop an element segment (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeElemDrop(
    mut env: JNIEnv,
    _class: JClass,
    instance_ptr: jlong,
    segment_index: jint,
) {
    jni_utils::jni_try(&mut env, || {
        let instance = unsafe {
            ffi_utils::deref_ptr::<crate::instance::Instance>(
                instance_ptr as *const std::os::raw::c_void,
                "instance",
            )?
        };

        let segment_manager = instance.get_element_segment_manager();
        segment_manager.drop_segment(segment_index as u32)?;
        Ok(())
    });
}

//! JNI bindings for Caller context operations

use std::os::raw::c_void;

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jboolean, jbyteArray, jint, jintArray, jlong, jsize};
use jni::JNIEnv;

use wasmtime::Caller as WasmtimeCaller;

use crate::caller::core;
use crate::error::{jni_utils, WasmtimeError};
use crate::linker::core as linker_core;
use crate::store::StoreData;
use crate::table::TableElement;

/// Get fuel remaining in the caller if fuel metering is enabled (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeGetFuelRemaining(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
) -> jlong {
    if caller_handle == 0 {
        return -1; // Error: null pointer
    }

    jni_utils::jni_try_with_default(&mut env, -1i64, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        match core::caller_get_fuel_remaining(caller)? {
            Some(fuel) => Ok(fuel as i64),
            None => Ok(-1i64), // Fuel metering not enabled
        }
    }) as jlong
}

/// Add fuel to the caller (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeAddFuel(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    fuel: jlong,
) {
    if caller_handle == 0 {
        return;
    }

    jni_utils::jni_try_void(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_add_fuel(caller, fuel as u64)
    });
}

/// Set fuel to a specific value for the caller (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeSetFuel(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    fuel: jlong,
) {
    if caller_handle == 0 {
        return;
    }

    jni_utils::jni_try_void(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_set_fuel(caller, fuel as u64)
    });
}

/// Set epoch deadline for the caller (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeSetEpochDeadline(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    deadline: jlong,
) {
    if caller_handle == 0 {
        return;
    }

    jni_utils::jni_try_void(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_set_epoch_deadline(caller, deadline as u64)
    });
}

/// Check if the caller has an active epoch deadline (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeHasEpochDeadline(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
) -> jboolean {
    if caller_handle == 0 {
        return 0; // Error: null pointer
    }

    jni_utils::jni_try_bool(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_has_epoch_deadline(caller)
    }) as jboolean
}

/// Check if caller has an export with the given name (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeHasExport(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    name: JString,
) -> jboolean {
    if caller_handle == 0 {
        return 0; // Error: null pointer
    }

    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0, // Error getting string
    };

    jni_utils::jni_try_bool(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_has_export(caller, &name_str)
    }) as jboolean
}

/// Get memory export from caller by name (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeGetMemory(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    name: JString,
) -> jlong {
    if caller_handle == 0 {
        return 0; // Error: null pointer
    }

    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0, // Error getting string
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        match core::caller_get_memory(caller, &name_str)? {
            Some(memory) => Ok(Box::new(memory)),
            None => Err(crate::error::WasmtimeError::ExportNotFound { name: name_str }),
        }
    }) as jlong
}

/// Get function export from caller by name (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeGetFunction(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    name: JString,
) -> jlong {
    if caller_handle == 0 {
        return 0; // Error: null pointer
    }

    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0, // Error getting string
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        match core::caller_get_function(caller, &name_str)? {
            Some(function) => Ok(Box::new(function)),
            None => Err(crate::error::WasmtimeError::ExportNotFound { name: name_str }),
        }
    }) as jlong
}

/// Get global export from caller by name (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeGetGlobal(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    name: JString,
) -> jlong {
    if caller_handle == 0 {
        return 0; // Error: null pointer
    }

    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0, // Error getting string
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        match core::caller_get_global(caller, &name_str)? {
            Some(global) => Ok(Box::new(global)),
            None => Err(crate::error::WasmtimeError::ExportNotFound { name: name_str }),
        }
    }) as jlong
}

/// Set fuel async yield interval for the caller's store (JNI version)
#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeSetFuelAsyncYieldInterval(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    interval: jlong,
) {
    if caller_handle == 0 {
        return;
    }

    jni_utils::jni_try_void(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_set_fuel_async_yield_interval(caller, interval as u64)
    });
}

/// Get table export from caller by name (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeGetTable(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    name: JString,
) -> jlong {
    if caller_handle == 0 {
        return 0; // Error: null pointer
    }

    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0, // Error getting string
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        match core::caller_get_table(caller, &name_str)? {
            Some(table) => Ok(Box::new(table)),
            None => Err(crate::error::WasmtimeError::ExportNotFound { name: name_str }),
        }
    }) as jlong
}

// ===========================================================================
// r.2: caller-scoped store-mutation natives.
//
// These operate on wasmtime objects (Table, Memory) via
// `caller.as_context_mut()`, which is wasmtime's borrow-safe entrypoint for
// reentrant store mutation from within a host callback. Contrast this with
// the analogous JniTable / JniMemory paths which acquire a fresh Store
// handle — that path is unsafe from a callback frame and is exactly what
// F-JIT-Loader-Java-Reference r.5.b showed crashing at
// libwasmtime4j.dylib:HostFunc::store_untyped_results.
//
// Java's JniCaller has already verified its generation counter before
// calling these; the wasmtime borrow's validity is what the counter
// enforces. Each native still null-checks the handles as defense in depth.
// ===========================================================================

/// Grow a caller-visible Table by `delta` slots, initialized to `init_value`.
///
/// `init_value` is a reference registry id (funcref / externref / anyref) or
/// 0 for null, matching the shape of {@code JniTable.nativeTableGrow}.
///
/// `table_handle` is a pointer to a `Box<wasmtime::Table>` (as returned by
/// {@link Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeGetTable}, i.e.
/// the raw wasmtime type — NOT the {@code crate::table::Table} wrapper used
/// by JniTable). This is what `caller.getTable(name)` hands back to Java.
///
/// Returns the previous table size on success or -1 on failure.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerGrowTable(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    table_handle: jlong,
    delta: jint,
    init_value: jlong,
) -> jlong {
    if caller_handle == 0 || table_handle == 0 {
        return -1;
    }

    jni_utils::jni_try_with_default(&mut env, -1i64, || {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        let table = unsafe { *(table_handle as *const wasmtime::Table) };

        // Determine element type via the table's TableType so we can build
        // the right Ref variant for init_value.
        let table_ty = table.ty(&caller.as_context_mut());
        let element_type = ValType_from_ref_type(table_ty.element());
        let init_element = table_element_from_ref_id(&element_type, init_value)?;
        let init_ref = table_element_to_ref(init_element)?;

        let prev_size = table
            .grow(&mut caller.as_context_mut(), delta as u64, init_ref)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Caller-scoped table grow failed: {}", e),
                backtrace: None,
            })?;
        Ok(prev_size as jlong)
    })
}

/// Set a caller-visible Table's element at `index` to the value identified by
/// `value_ref_id` (registry id or 0 for null).
///
/// Returns 1 on success, 0 on failure.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerSetTableElement(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    table_handle: jlong,
    index: jint,
    value_ref_id: jlong,
) -> jboolean {
    if caller_handle == 0 || table_handle == 0 {
        return 0;
    }

    jni_utils::jni_try_bool(&mut env, || {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        let table = unsafe { *(table_handle as *const wasmtime::Table) };

        let table_ty = table.ty(&caller.as_context_mut());
        let element_type = ValType_from_ref_type(table_ty.element());
        let element = table_element_from_ref_id(&element_type, value_ref_id)?;
        let value_ref = table_element_to_ref(element)?;

        table
            .set(&mut caller.as_context_mut(), index as u64, value_ref)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Caller-scoped table set failed: {}", e),
                backtrace: None,
            })?;
        Ok(true)
    }) as jboolean
}

/// Instantiate an `InstancePre` against the caller's live wasmtime context.
///
/// This is the reentrant-safe path used from inside a host-function callback:
/// instead of routing through `JniInstancePre.nativeInstantiate` (which
/// acquires the `Store` wrapper's ReentrantLock via `try_lock_store`),
/// we borrow the caller's already-live `StoreContextMut<StoreData>` and hand
/// it directly to `InstancePreWrapper::instantiate_with_context`. Prevents
/// the deadlock/double-borrow that r.2 identified as the reason instantiate
/// could not land in the first-cut caller surface.
///
/// Returns the resulting `Instance` handle (pointer to `Box<Instance>`) on
/// success or 0 on failure. Errors are thrown as WasmException on the JNI side.
///
/// # Safety
///
/// `caller_handle` must be a valid `*mut Caller<'_, StoreData>` produced by
/// the JNI callback dispatch (`jni::linker::JniHostFunctionCallback::execute`).
/// `instance_pre_handle` must be a valid `*const InstancePreWrapper` produced
/// by `wasmtime4j_linker_instantiate_pre` (matching `JniInstancePre.getNativeHandle()`).
///
/// The Java-side JniCaller checks its generation counter before calling here,
/// so a stale caller pointer will be rejected at the Java layer.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerInstantiate(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    instance_pre_handle: jlong,
) -> jlong {
    if caller_handle == 0 || instance_pre_handle == 0 {
        return 0;
    }

    jni_utils::jni_try_with_default(&mut env, 0i64, || {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        let pre = unsafe {
            &*(instance_pre_handle as *const crate::linker::InstancePreWrapper)
        };

        let ctx = caller.as_context_mut();
        let instance = pre.instantiate_with_context(ctx)?;
        Ok(Box::into_raw(Box::new(instance)) as jlong)
    })
}

/// Grow a caller-visible Memory by `delta_pages`. Returns the previous size in
/// pages on success or -1 on failure.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerGrowMemory(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    memory_handle: jlong,
    delta_pages: jlong,
) -> jlong {
    if caller_handle == 0 || memory_handle == 0 {
        return -1;
    }

    jni_utils::jni_try_with_default(&mut env, -1i64, || {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        let memory = unsafe { *(memory_handle as *const wasmtime::Memory) };

        let prev_pages = memory
            .grow(&mut caller.as_context_mut(), delta_pages as u64)
            .map_err(|e| WasmtimeError::Memory {
                message: format!("Caller-scoped memory grow failed: {}", e),
            })?;
        Ok(prev_pages as jlong)
    })
}

/// Convert wasmtime::RefType to ValType for TableElement matching.
#[allow(non_snake_case)]
fn ValType_from_ref_type(ref_type: &wasmtime::RefType) -> wasmtime::ValType {
    wasmtime::ValType::Ref(ref_type.clone())
}

/// Look up a TableElement for the given (element_type, registry id) pair.
///
/// A `ref_id` of 0 encodes null. For funcref / externref, the id encodes into
/// the corresponding variant. Non-reference table element types are rejected.
fn table_element_from_ref_id(
    element_type: &wasmtime::ValType,
    ref_id: jlong,
) -> crate::error::WasmtimeResult<TableElement> {
    match element_type {
        wasmtime::ValType::Ref(ref_type) => match ref_type.heap_type() {
            wasmtime::HeapType::Func => Ok(if ref_id == 0 {
                TableElement::FuncRef(None)
            } else {
                TableElement::FuncRef(Some(ref_id as u64))
            }),
            wasmtime::HeapType::Extern => Ok(if ref_id == 0 {
                TableElement::ExternRef(None)
            } else {
                TableElement::ExternRef(Some(ref_id as u64))
            }),
            _ => Ok(if ref_id == 0 {
                TableElement::AnyRef(None)
            } else {
                TableElement::AnyRef(Some(ref_id as u64))
            }),
        },
        _ => Err(WasmtimeError::InvalidParameter {
            message: format!(
                "Caller-scoped table op on non-reference element type: {:?}",
                element_type
            ),
        }),
    }
}

/// Convert a TableElement (registry id) to a wasmtime::Ref by resolving the
/// funcref through the shared function reference registry.
fn table_element_to_ref(element: TableElement) -> crate::error::WasmtimeResult<wasmtime::Ref> {
    match element {
        TableElement::FuncRef(None) => Ok(wasmtime::Ref::Func(None)),
        TableElement::FuncRef(Some(id)) => {
            // The caller-scoped path uses the same funcref registry as the
            // JniTable-based path so funcref ids stay round-trippable.
            let func = crate::table::core::get_function_reference(id)?.ok_or_else(|| {
                WasmtimeError::Runtime {
                    message: format!("Funcref id {} not in registry", id),
                    backtrace: None,
                }
            })?;
            Ok(wasmtime::Ref::Func(Some(func)))
        }
        TableElement::ExternRef(_) | TableElement::AnyRef(_) => {
            // The scoped path only implements funcref writes for r.2 — the
            // primary r.5.b use case is JIT-loader installing funcrefs. Extern
            // / any refs require the ExternRef::new/AnyRef::new path which
            // needs additional wiring; deferred to a follow-up.
            Err(WasmtimeError::Runtime {
                message: "Caller-scoped set only supports funcref values in r.2".to_string(),
                backtrace: None,
            })
        }
    }
}

/// Get debug exit frames from the caller (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeDebugExitFrames(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
) -> jintArray {
    let null_array = std::ptr::null_mut();
    if caller_handle == 0 {
        return null_array;
    }

    let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };

    match core::caller_debug_exit_frames(caller) {
        Ok(Some(frames)) => {
            if frames.is_empty() {
                env.new_int_array(0)
                    .map(|a| a.into_raw())
                    .unwrap_or(null_array)
            } else {
                let flat_len = frames.len() * 4;
                let mut flat_data: Vec<jint> = Vec::with_capacity(flat_len);
                for frame in &frames {
                    flat_data.push(frame[0]);
                    flat_data.push(frame[1]);
                    flat_data.push(frame[2]);
                    flat_data.push(frame[3]);
                }
                match env.new_int_array(flat_len as jsize) {
                    Ok(array) => {
                        if env.set_int_array_region(&array, 0, &flat_data).is_ok() {
                            array.into_raw()
                        } else {
                            null_array
                        }
                    }
                    Err(_) => null_array,
                }
            }
        }
        Ok(None) => null_array,
        Err(_) => null_array,
    }
}

/// Read a byte range from a caller-exported memory by name (F-Wasmtime4j-Caller-Scoped-Memory-IO
/// r.1 2026-07-31). Uses `caller.get_export(name).into_memory()` + `Memory::read` so registration
/// is via the callback's live borrow, not the api-layer memory adapter's frame-scoped handle.
///
/// Returns the read byte array on success; throws a WasmException on failure.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerReadMemory(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    memory_name: JString,
    offset: jlong,
    length: jint,
) -> jbyteArray {
    let null_array = std::ptr::null_mut();
    if caller_handle == 0 {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            "readMemory: null caller handle",
        );
        return null_array;
    }

    let name: String = match env.get_string(&memory_name) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                "readMemory: memoryName is not a valid Java String",
            );
            return null_array;
        }
    };

    // Do the read into a Rust Vec<u8> without touching env — env is mutably borrowed later.
    let read_result: Result<Vec<u8>, String> = (|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };

        let export = caller
            .get_export(&name)
            .ok_or_else(|| format!("caller has no exported memory named '{}'", name))?;
        let memory = export
            .into_memory()
            .ok_or_else(|| format!("caller export '{}' is not a memory", name))?;

        let mut buf = vec![0u8; length as usize];
        memory
            .read(&mut caller.as_context_mut(), offset as usize, &mut buf)
            .map_err(|e| format!("Caller-scoped memory read failed: {}", e))?;
        Ok(buf)
    })();

    match read_result {
        Ok(buf) => match env.byte_array_from_slice(&buf) {
            Ok(arr) => arr.into_raw(),
            Err(e) => {
                let _ = env.throw_new(
                    "ai/tegmentum/wasmtime4j/exception/WasmException",
                    format!("readMemory: failed to allocate return array: {}", e),
                );
                null_array
            }
        },
        Err(msg) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/WasmException", msg);
            null_array
        }
    }
}

/// Write a byte range into a caller-exported memory by name (F-Wasmtime4j-Caller-Scoped-Memory-IO
/// r.1 2026-07-31). Mirror of `nativeCallerReadMemory`.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerWriteMemory(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    memory_name: JString,
    offset: jlong,
    bytes: jbyteArray,
) {
    if caller_handle == 0 {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            "writeMemory: null caller handle",
        );
        return;
    }

    let name: String = match env.get_string(&memory_name) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                "writeMemory: memoryName is not a valid Java String",
            );
            return;
        }
    };

    // Extract byte array data outside jni_try to avoid double-mutable borrow of env.
    let data = match env
        .convert_byte_array(unsafe { JByteArray::from_raw(bytes) })
    {
        Ok(d) => d,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("writeMemory: failed to read bytes: {}", e),
            );
            return;
        }
    };

    let write_result: Result<(), String> = (|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };

        let export = caller
            .get_export(&name)
            .ok_or_else(|| format!("caller has no exported memory named '{}'", name))?;
        let memory = export
            .into_memory()
            .ok_or_else(|| format!("caller export '{}' is not a memory", name))?;

        memory
            .write(&mut caller.as_context_mut(), offset as usize, &data)
            .map_err(|e| format!("Caller-scoped memory write failed: {}", e))?;
        Ok(())
    })();

    if let Err(msg) = write_result {
        let _ = env.throw_new("ai/tegmentum/wasmtime4j/exception/WasmException", msg);
    }
}

/// Register a caller-scoped {@link JniFunction} as a funcref in the shared
/// REFERENCE_REGISTRY, keyed by the caller's store_id. Returns the registry id.
///
/// F-Wasmtime4j-Caller-Scoped-Registry-Integration r.2.a (2026-07-27).
///
/// The caller-scoped `objectToRefId` path in JniCaller.java previously routed
/// through `nativeFuncToRaw` — which returns a wasmtime-internal raw pointer
/// (not a REFERENCE_REGISTRY id). `nativeCallerGrowTable` then treated that raw
/// pointer as a registry id and failed with "Funcref id N not in registry".
///
/// This native closes that mismatch: registers the func into REFERENCE_REGISTRY
/// under `caller.data().store_id` and returns the id, so subsequent
/// `nativeCallerGrowTable` / `nativeCallerSetTableElement` calls succeed.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerFuncToRegistryId(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    function_ptr: jlong,
) -> jlong {
    if caller_handle == 0 || function_ptr == 0 {
        return 0;
    }
    jni_utils::jni_try_with_default(&mut env, 0i64, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        let store_id = caller.data().store_id;
        let func_handle =
            unsafe { &*(function_ptr as *const crate::jni::function::FunctionHandle) };
        let func = func_handle.get_func().clone();
        let id = crate::table::core::register_function_reference(func, store_id)?;
        Ok(id as jlong)
    })
}

/// Define a memory extern into a Linker using the caller's live store context
/// (F-Wasmtime4j-Caller-Scoped-Instantiate-Extern-Imports r.1 2026-07-27).
///
/// Mirrors `nativeDefineMemory` in linker.rs but uses `caller.as_context_mut()`
/// as the store context, so a callback frame can wire memory imports into a
/// linker for a nested InstancePre without acquiring the store lock.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerLinkerDefineMemory(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    linker_handle: jlong,
    module_name: JString,
    name: JString,
    memory_handle: jlong,
) -> jboolean {
    if caller_handle == 0 || linker_handle == 0 || memory_handle == 0 {
        return 0;
    }
    let module_name_str: String = match env.get_string(&module_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        let linker = unsafe { linker_core::get_linker_ref(linker_handle as *const c_void)? };
        let memory =
            unsafe { crate::memory::core::get_memory_ref(memory_handle as *const c_void)? };

        let extern_memory = if let Some(wasmtime_memory) = memory.inner() {
            wasmtime::Extern::Memory(*wasmtime_memory)
        } else if let Some(wasmtime_shared_memory) = memory.inner_shared() {
            wasmtime::Extern::SharedMemory(wasmtime_shared_memory.clone())
        } else {
            return Err(WasmtimeError::Linker {
                message: format!(
                    "Memory '{}::{}' has invalid variant",
                    module_name_str, name_str
                ),
            });
        };

        let mut linker_lock = linker.inner()?;
        linker_lock
            .define(
                &mut caller.as_context_mut(),
                &module_name_str,
                &name_str,
                extern_memory,
            )
            .map_err(|e| WasmtimeError::Linker {
                message: format!(
                    "Caller-scoped Linker.defineMemory '{}::{}' failed: {}",
                    module_name_str, name_str, e
                ),
            })?;

        Ok(1)
    })
}

/// Define a table extern into a Linker using the caller's live store context
/// (F-Wasmtime4j-Caller-Scoped-Instantiate-Extern-Imports r.1 2026-07-27).
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerLinkerDefineTable(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    linker_handle: jlong,
    module_name: JString,
    name: JString,
    table_handle: jlong,
) -> jboolean {
    if caller_handle == 0 || linker_handle == 0 || table_handle == 0 {
        return 0;
    }
    let module_name_str: String = match env.get_string(&module_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        let linker = unsafe { linker_core::get_linker_ref(linker_handle as *const c_void)? };
        let table = unsafe { crate::table::core::get_table_ref(table_handle as *const c_void)? };

        let wasmtime_table_arc = table.wasmtime_table();
        let wasmtime_table_lock =
            wasmtime_table_arc
                .lock()
                .map_err(|e| WasmtimeError::Concurrency {
                    message: format!("Failed to lock table: {}", e),
                })?;

        let mut linker_lock = linker.inner()?;
        linker_lock
            .define(
                &mut caller.as_context_mut(),
                &module_name_str,
                &name_str,
                wasmtime::Extern::Table(*wasmtime_table_lock),
            )
            .map_err(|e| WasmtimeError::Linker {
                message: format!(
                    "Caller-scoped Linker.defineTable '{}::{}' failed: {}",
                    module_name_str, name_str, e
                ),
            })?;

        Ok(1)
    })
}

/// Define a memory extern into a Linker by looking it up on the caller by export name
/// (F-Wasmtime4j-Caller-Scoped-Instantiate-Extern-Imports r.1 2026-07-27, addendum).
///
/// Preferable to `nativeCallerLinkerDefineMemory` when the source memory is the caller's
/// own export — avoids the api-layer registry-handle roundtrip that would otherwise fail
/// with "Memory handle is not registered" (caller-scoped memory handles from
/// `nativeGetMemory` are `Box<wasmtime::Memory>` raw handles, not registry-wrapped).
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerLinkerDefineMemoryFromExport(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    linker_handle: jlong,
    module_name: JString,
    name: JString,
    caller_export_name: JString,
) -> jboolean {
    if caller_handle == 0 || linker_handle == 0 {
        return 0;
    }
    let module_name_str: String = match env.get_string(&module_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    let export_name_str: String = match env.get_string(&caller_export_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        let linker = unsafe { linker_core::get_linker_ref(linker_handle as *const c_void)? };

        let export = caller.get_export(&export_name_str).ok_or_else(|| WasmtimeError::Linker {
            message: format!("caller has no export named '{}'", export_name_str),
        })?;
        let memory = export.into_memory().ok_or_else(|| WasmtimeError::Linker {
            message: format!("caller export '{}' is not a memory", export_name_str),
        })?;

        let mut linker_lock = linker.inner()?;
        linker_lock
            .define(
                &mut caller.as_context_mut(),
                &module_name_str,
                &name_str,
                wasmtime::Extern::Memory(memory),
            )
            .map_err(|e| WasmtimeError::Linker {
                message: format!(
                    "Caller-scoped Linker.defineMemoryFromExport '{}::{}' (from '{}') failed: {}",
                    module_name_str, name_str, export_name_str, e
                ),
            })?;

        Ok(1)
    })
}

/// Define a table extern into a Linker by looking it up on the caller by export name
/// (F-Wasmtime4j-Caller-Scoped-Instantiate-Extern-Imports r.1 2026-07-27, addendum).
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerLinkerDefineTableFromExport(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    linker_handle: jlong,
    module_name: JString,
    name: JString,
    caller_export_name: JString,
) -> jboolean {
    if caller_handle == 0 || linker_handle == 0 {
        return 0;
    }
    let module_name_str: String = match env.get_string(&module_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    let export_name_str: String = match env.get_string(&caller_export_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        let linker = unsafe { linker_core::get_linker_ref(linker_handle as *const c_void)? };

        let export = caller.get_export(&export_name_str).ok_or_else(|| WasmtimeError::Linker {
            message: format!("caller has no export named '{}'", export_name_str),
        })?;
        let table = export.into_table().ok_or_else(|| WasmtimeError::Linker {
            message: format!("caller export '{}' is not a table", export_name_str),
        })?;

        let mut linker_lock = linker.inner()?;
        linker_lock
            .define(
                &mut caller.as_context_mut(),
                &module_name_str,
                &name_str,
                wasmtime::Extern::Table(table),
            )
            .map_err(|e| WasmtimeError::Linker {
                message: format!(
                    "Caller-scoped Linker.defineTableFromExport '{}::{}' (from '{}') failed: {}",
                    module_name_str, name_str, export_name_str, e
                ),
            })?;

        Ok(1)
    })
}

/// Define a global extern into a Linker using the caller's live store context
/// (F-Wasmtime4j-Caller-Scoped-Instantiate-Extern-Imports r.1 2026-07-27).
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerLinkerDefineGlobal(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    linker_handle: jlong,
    module_name: JString,
    name: JString,
    global_handle: jlong,
) -> jboolean {
    if caller_handle == 0 || linker_handle == 0 || global_handle == 0 {
        return 0;
    }
    let module_name_str: String = match env.get_string(&module_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        let linker = unsafe { linker_core::get_linker_ref(linker_handle as *const c_void)? };
        let global =
            unsafe { crate::global::core::get_global_ref(global_handle as *const c_void)? };

        let wasmtime_global_arc = global.wasmtime_global();
        let wasmtime_global_lock =
            wasmtime_global_arc
                .lock()
                .map_err(|e| WasmtimeError::Concurrency {
                    message: format!("Failed to lock global: {}", e),
                })?;

        let mut linker_lock = linker.inner()?;
        linker_lock
            .define(
                &mut caller.as_context_mut(),
                &module_name_str,
                &name_str,
                wasmtime::Extern::Global(*wasmtime_global_lock),
            )
            .map_err(|e| WasmtimeError::Linker {
                message: format!(
                    "Caller-scoped Linker.defineGlobal '{}::{}' failed: {}",
                    module_name_str, name_str, e
                ),
            })?;

        Ok(1)
    })
}

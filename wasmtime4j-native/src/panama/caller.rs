//! Panama FFI bindings for Caller operations
//!
//! This module provides C-compatible functions for accessing WebAssembly
//! execution context from host functions, enabling memory access, fuel
//! management, and export introspection.

use crate::caller::core;
use crate::error::ffi_utils;
use crate::store::StoreData;
use std::os::raw::{c_char, c_int, c_void};
use wasmtime::Caller as WasmtimeCaller;

/// Get fuel remaining in the caller if fuel metering is enabled (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_get_fuel_remaining(
    caller_ptr: *mut c_void,
    fuel_out: *mut u64,
) -> c_int {
    if caller_ptr.is_null() || fuel_out.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        match core::caller_get_fuel_remaining(caller)? {
            Some(fuel) => {
                unsafe {
                    *fuel_out = fuel;
                }
                Ok(()) // Success
            }
            None => Ok(()), // Fuel metering not enabled
        }
    })
}

/// Add fuel to the caller (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_add_fuel(
    caller_ptr: *mut c_void,
    fuel: u64,
) -> c_int {
    if caller_ptr.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_add_fuel(caller, fuel)?;
        Ok(()) // Success
    })
}

/// Set fuel to a specific value for the caller (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_set_fuel(
    caller_ptr: *mut c_void,
    fuel: u64,
) -> c_int {
    if caller_ptr.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_set_fuel(caller, fuel)?;
        Ok(()) // Success
    })
}

/// Set epoch deadline for the caller (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_set_epoch_deadline(
    caller_ptr: *mut c_void,
    deadline: u64,
) -> c_int {
    if caller_ptr.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_set_epoch_deadline(caller, deadline)?;
        Ok(()) // Success
    })
}

/// Check if the caller has an active epoch deadline (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_has_epoch_deadline(caller_ptr: *mut c_void) -> c_int {
    if caller_ptr.is_null() {
        return -1; // Error: null pointer
    }

    let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
    match core::caller_has_epoch_deadline(caller) {
        Ok(true) => 1,  // Has deadline
        Ok(false) => 0, // No deadline
        Err(_) => -1,   // Error occurred
    }
}

/// Set fuel async yield interval for the caller's store (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_set_fuel_async_yield_interval(
    caller_ptr: *mut c_void,
    interval: u64,
) -> c_int {
    if caller_ptr.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_set_fuel_async_yield_interval(caller, interval)?;
        Ok(())
    })
}

/// Check if caller has an export with the given name (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_has_export(
    caller_ptr: *mut c_void,
    name: *const c_char,
) -> c_int {
    if caller_ptr.is_null() || name.is_null() {
        return -1; // Error: null pointer
    }

    let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s,
        Err(_) => return -1, // Invalid UTF-8
    };

    match core::caller_has_export(caller, name_str) {
        Ok(true) => 1,  // Has export
        Ok(false) => 0, // No export
        Err(_) => -1,   // Error occurred
    }
}

/// Get memory export from caller by name (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_get_memory(
    caller_ptr: *mut c_void,
    name: *const c_char,
    memory_out: *mut *mut c_void,
) -> c_int {
    if caller_ptr.is_null() || name.is_null() || memory_out.is_null() {
        return -1; // Error: null pointer
    }

    let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s,
        Err(_) => return -1, // Invalid UTF-8
    };

    // F-Wasmtime4j-Panama-Callback-Caller-Wire r.4 (2026-07-28): aligned
    // return convention with `wasmtime4j_panama_caller_get_function` /
    // `_get_table` / `_get_global` — 0 for both success (out-ptr populated)
    // AND not-found (out-ptr set null).
    //
    // F-Wasmtime4j-Panama-Memory-From-Caller-Wrapper-Fix (2026-07-28):
    // wrap the returned Memory in a `ValidatedMemory` via
    // `crate::memory::core::create_validated_memory`. Downstream
    // consumers (`wasmtime4j_panama_memory_size_pages` /
    // `_size_bytes` / `_grow`) dereference the ptr as
    // `*const ValidatedMemory` (see memory/core.rs:257 `get_memory_ref`).
    // Prior code boxed a raw `wasmtime::Memory`, so downstream size/grow
    // calls hit UB on the magic-check field. The sibling `get_table` /
    // `_get_global` FFIs don't have this bug because their downstream
    // getters accept raw `wasmtime::{Table,Global}` (no wrapper).
    match core::caller_get_memory(caller, name_str) {
        Ok(Some(wasmtime_memory)) => {
            // Query the memory type from the caller's context so the
            // wrapper carries the correct min/max/is_64/is_shared flags.
            use wasmtime::AsContextMut;
            let memory_type = wasmtime_memory.ty(&caller.as_context_mut());
            let wrapped =
                crate::memory::Memory::from_wasmtime_memory(wasmtime_memory, memory_type);
            match crate::memory::core::create_validated_memory(wrapped) {
                Ok(validated_ptr) => {
                    unsafe {
                        *memory_out = validated_ptr as *mut c_void;
                    }
                    0 // Memory found
                }
                Err(_) => -1,
            }
        }
        Ok(None) => {
            unsafe {
                *memory_out = std::ptr::null_mut();
            }
            0 // No memory export with this name
        }
        Err(_) => -1, // Error occurred
    }
}

/// Get function export from caller by name (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_get_function(
    caller_ptr: *mut c_void,
    name: *const c_char,
    function_out: *mut *mut c_void,
) -> c_int {
    if caller_ptr.is_null() || name.is_null() || function_out.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let name_str = unsafe { std::ffi::CStr::from_ptr(name) }
            .to_str()
            .map_err(|e| crate::error::WasmtimeError::Utf8Error {
                message: e.to_string(),
            })?;

        match core::caller_get_function(caller, name_str)? {
            Some(function) => {
                unsafe {
                    *function_out = Box::into_raw(Box::new(function)) as *mut c_void;
                }
                Ok(()) // Success
            }
            None => {
                unsafe {
                    *function_out = std::ptr::null_mut();
                }
                Ok(()) // No function export with this name
            }
        }
    })
}

/// Get global export from caller by name (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_get_global(
    caller_ptr: *mut c_void,
    name: *const c_char,
    global_out: *mut *mut c_void,
) -> c_int {
    if caller_ptr.is_null() || name.is_null() || global_out.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let name_str = unsafe { std::ffi::CStr::from_ptr(name) }
            .to_str()
            .map_err(|e| crate::error::WasmtimeError::Utf8Error {
                message: e.to_string(),
            })?;

        match core::caller_get_global(caller, name_str)? {
            Some(global) => {
                unsafe {
                    *global_out = Box::into_raw(Box::new(global)) as *mut c_void;
                }
                Ok(()) // Success
            }
            None => {
                unsafe {
                    *global_out = std::ptr::null_mut();
                }
                Ok(()) // No global export with this name
            }
        }
    })
}

/// Get table export from caller by name (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_get_table(
    caller_ptr: *mut c_void,
    name: *const c_char,
    table_out: *mut *mut c_void,
) -> c_int {
    if caller_ptr.is_null() || name.is_null() || table_out.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let name_str = unsafe { std::ffi::CStr::from_ptr(name) }
            .to_str()
            .map_err(|e| crate::error::WasmtimeError::Utf8Error {
                message: e.to_string(),
            })?;

        // F-Wasmtime4j-Panama-Table-From-Caller-Wrapper-Fix (2026-07-28):
        // wrap the returned wasmtime::Table in a `crate::table::Table`
        // wrapper (via `from_wasmtime_table_with_context`). Downstream
        // consumers — notably `wasmtime4j_panama_table_metadata` invoked
        // by `PanamaTable`'s constructor — deref this ptr as
        // `*const crate::table::Table` (see `crate::table::core::
        // get_table_ref`). Prior code boxed a raw wasmtime::Table,
        // causing UB when Java-side PanamaTable construction queried
        // metadata. Sibling `caller_grow_table` reads the wrapper's
        // inner via the same helper (updated in this arc's follow-up).
        match core::caller_get_table(caller, name_str)? {
            Some(wasmtime_table) => {
                use wasmtime::AsContextMut;
                // Pass None for name to avoid string_to_c_char round-trip
                // in the panama_table_metadata path — Java-side wraps this
                // via PanamaTable which caches its own name from the
                // Java-side caller export lookup.
                let wrapped = crate::table::Table::from_wasmtime_table_with_context(
                    wasmtime_table,
                    caller.as_context_mut(),
                    None,
                );
                unsafe {
                    *table_out = Box::into_raw(Box::new(wrapped)) as *mut c_void;
                }
                Ok(()) // Success
            }
            None => {
                unsafe {
                    *table_out = std::ptr::null_mut();
                }
                Ok(()) // No table export with this name
            }
        }
    })
}

/// Get debug exit frames from the caller (Panama FFI version)
///
/// Two-phase protocol:
/// - First call with out_data=null: writes frame count to count_out, returns 0 on success
/// - Second call with out_data pointing to buffer: writes frame data, returns 0 on success
/// - Returns -1 if debugging not enabled, -2 on error
///
/// # Safety
/// - caller_ptr must be a valid pointer to a WasmtimeCaller
/// - count_out must be a valid pointer to a c_int
/// - out_data (if non-null) must point to a buffer of at least count*4 c_int elements
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_debug_exit_frames(
    caller_ptr: *mut c_void,
    count_out: *mut c_int,
    out_data: *mut c_int,
) -> c_int {
    if caller_ptr.is_null() || count_out.is_null() {
        return -2; // Error: null pointer
    }

    let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };

    match core::caller_debug_exit_frames(caller) {
        Ok(Some(frames)) => {
            let count = frames.len() as c_int;
            unsafe {
                *count_out = count;
            }
            if out_data.is_null() {
                // Phase 1: just return count
                0
            } else {
                // Phase 2: write frame data
                for (i, frame) in frames.iter().enumerate() {
                    let base = i * 4;
                    unsafe {
                        *out_data.add(base) = frame[0];
                        *out_data.add(base + 1) = frame[1];
                        *out_data.add(base + 2) = frame[2];
                        *out_data.add(base + 3) = frame[3];
                    }
                }
                0
            }
        }
        Ok(None) => -1, // Debugging not enabled
        Err(_) => -2,   // Error
    }
}

// ===========================================================================
// F-Wasmtime4j-Panama-Caller-Scoped-Mutation-FFI r.2 slice 1 (2026-07-28).
//
// Table mutation FFI parity with JNI's `nativeCallerGrowTable` +
// `nativeCallerSetTableElement`. Bodies mirror the JNI natives verbatim —
// same ref-id resolution helpers (lifted to `pub(crate)` in `jni/caller.rs`
// this slice), same `caller.as_context_mut()` borrow-safe path per
// doctrine-store-reentrant-lock-blocks-in-callback-2026-07-27.
//
// If a third FFI tier appears, move the shared helpers
// (`ValType_from_ref_type`, `table_element_from_ref_id`,
// `table_element_to_ref`) from `jni/caller.rs` to `crate::caller::core`.
// ===========================================================================

/// Grow a caller-visible Table by `delta` slots, initialized to `init_ref_id`
/// (funcref registry id, externref registry id, or 0 for null).
///
/// Returns the previous table size on success, or `-1` on failure (error
/// stored via `set_last_error`, retrievable with the last-error FFI).
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_grow_table(
    caller_ptr: *mut c_void,
    table_ptr: *mut c_void,
    delta: c_int,
    init_ref_id: i64,
) -> i64 {
    if caller_ptr.is_null() || table_ptr.is_null() {
        return -1;
    }
    ffi_utils::ffi_try_code_i64(|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        // F-Wasmtime4j-Panama-Table-From-Caller-Wrapper-Fix (2026-07-28):
        // table_ptr is now a *const crate::table::Table wrapper per the
        // sibling caller_get_table fix. Extract the inner wasmtime::Table
        // via lock-and-copy (WasmtimeTable is Copy).
        let table_wrapper =
            unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
        let inner_arc = table_wrapper.wasmtime_table();
        let table = *inner_arc.lock().map_err(|e| crate::error::WasmtimeError::Concurrency {
            message: format!("Failed to lock table: {}", e),
        })?;

        let table_ty = table.ty(&caller.as_context_mut());
        let element_type =
            crate::jni::caller::ValType_from_ref_type(table_ty.element());
        let init_element =
            crate::jni::caller::table_element_from_ref_id(&element_type, init_ref_id)?;
        let init_ref = crate::jni::caller::table_element_to_ref(init_element)?;

        let prev_size = table
            .grow(&mut caller.as_context_mut(), delta as u64, init_ref)
            .map_err(|e| crate::error::WasmtimeError::Runtime {
                message: format!("Caller-scoped table grow failed: {}", e),
                backtrace: None,
            })?;
        Ok(prev_size as i64)
    })
}

/// Set a caller-visible Table's element at `index` to the value identified by
/// `value_ref_id` (funcref registry id, externref registry id, or 0 for null).
///
/// Returns 0 on success, non-zero error code on failure. Error retrievable
/// via the last-error FFI.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_set_table_element(
    caller_ptr: *mut c_void,
    table_ptr: *mut c_void,
    index: c_int,
    value_ref_id: i64,
) -> c_int {
    if caller_ptr.is_null() || table_ptr.is_null() {
        return -1;
    }
    ffi_utils::ffi_try_code(|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        // F-Wasmtime4j-Panama-Table-From-Caller-Wrapper-Fix follow-up
        // (2026-07-28): parallel to caller_grow_table — table_ptr is now
        // a *const crate::table::Table wrapper. Extract inner via lock.
        let table_wrapper =
            unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
        let inner_arc = table_wrapper.wasmtime_table();
        let table = *inner_arc.lock().map_err(|e| crate::error::WasmtimeError::Concurrency {
            message: format!("Failed to lock table: {}", e),
        })?;

        let table_ty = table.ty(&caller.as_context_mut());
        let element_type =
            crate::jni::caller::ValType_from_ref_type(table_ty.element());
        let element =
            crate::jni::caller::table_element_from_ref_id(&element_type, value_ref_id)?;
        let value_ref = crate::jni::caller::table_element_to_ref(element)?;

        table
            .set(&mut caller.as_context_mut(), index as u64, value_ref)
            .map_err(|e| crate::error::WasmtimeError::Runtime {
                message: format!("Caller-scoped table set failed: {}", e),
                backtrace: None,
            })?;
        Ok(())
    })
}

// ===========================================================================
// F-Wasmtime4j-Panama-Caller-Scoped-Mutation-FFI r.3 slice 2 (2026-07-28).
//
// Memory mutation FFI parity with JNI's `nativeCallerGrowMemory` +
// `nativeCallerReadMemory` + `nativeCallerWriteMemory`. Bodies mirror the JNI
// natives; Panama diverges on the byte-buffer convention:
//   - read: caller pre-allocates `out_buf` of size `length`; native fills.
//     (JNI returns a fresh jbyteArray Java-side; Panama has no allocator.)
//   - write: caller passes `bytes` + `len` directly.
//     (JNI reads a jbyteArray via env.convert_byte_array.)
// ===========================================================================

/// Grow a caller-visible Memory by `delta_pages`.
///
/// Returns the previous memory size in pages on success, or `-1` on failure
/// (error stored via `set_last_error`).
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_grow_memory(
    caller_ptr: *mut c_void,
    memory_ptr: *mut c_void,
    delta_pages: i64,
) -> i64 {
    if caller_ptr.is_null() || memory_ptr.is_null() {
        return -1;
    }
    ffi_utils::ffi_try_code_i64(|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        // F-Wasmtime4j-Panama-Memory-From-Caller-Wrapper-Fix follow-up
        // (2026-07-28): memory_ptr is now a *const ValidatedMemory per
        // the sibling `caller_get_memory` r.4 fix — extract the inner
        // wasmtime::Memory via `get_memory_ref`. Prior code deref-cast
        // as raw `wasmtime::Memory`, misinterpreting the wrapper's
        // magic-check field as the memory handle.
        let memory_wrapper =
            unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };
        let wasmtime_memory = memory_wrapper.inner().copied().ok_or_else(|| {
            crate::error::WasmtimeError::Memory {
                message: "Caller-scoped grow requires regular (non-shared) memory".to_string(),
            }
        })?;

        let prev_pages = wasmtime_memory
            .grow(&mut caller.as_context_mut(), delta_pages as u64)
            .map_err(|e| crate::error::WasmtimeError::Memory {
                message: format!("Caller-scoped memory grow failed: {}", e),
            })?;
        Ok(prev_pages as i64)
    })
}

/// Read `length` bytes from the caller's exported memory named `name`,
/// starting at `offset`, into the caller-provided buffer `out_buf`.
///
/// Uses the callback-safe `caller.get_export(&name).into_memory()` path
/// (same as JNI's readMemory, avoiding the api-layer memory adapter's
/// frame-scoped handle limitations).
///
/// Returns 0 on success, non-zero error code on failure. Caller MUST ensure
/// `out_buf` points to at least `length` writable bytes.
///
/// Safety: `out_buf` must be a valid `*mut u8` pointing to `length` writable
/// bytes; `name` must be a valid nul-terminated C string.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_read_memory(
    caller_ptr: *mut c_void,
    name: *const c_char,
    offset: i64,
    length: c_int,
    out_buf: *mut u8,
) -> c_int {
    if caller_ptr.is_null() || name.is_null() || out_buf.is_null() {
        return -1;
    }
    if length < 0 {
        return -1;
    }
    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -1,
    };
    ffi_utils::ffi_try_code(|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let export = caller
            .get_export(&name_str)
            .ok_or_else(|| crate::error::WasmtimeError::Runtime {
                message: format!("caller has no exported memory named '{}'", name_str),
                backtrace: None,
            })?;
        let memory = export
            .into_memory()
            .ok_or_else(|| crate::error::WasmtimeError::Runtime {
                message: format!("caller export '{}' is not a memory", name_str),
                backtrace: None,
            })?;

        // Read directly into the caller's buffer — no intermediate Vec<u8> like
        // JNI needs (JNI has to allocate a jbyteArray return anyway).
        let buf_slice = unsafe { std::slice::from_raw_parts_mut(out_buf, length as usize) };
        memory
            .read(&mut caller.as_context_mut(), offset as usize, buf_slice)
            .map_err(|e| crate::error::WasmtimeError::Runtime {
                message: format!("Caller-scoped memory read failed: {}", e),
                backtrace: None,
            })?;
        Ok(())
    })
}

/// Write `len` bytes from `bytes` into the caller's exported memory named
/// `name` starting at `offset`.
///
/// Uses the callback-safe `caller.get_export(&name).into_memory()` path.
///
/// Returns 0 on success, non-zero error code on failure.
///
/// Safety: `bytes` must be a valid `*const u8` pointing to `len` readable
/// bytes; `name` must be a valid nul-terminated C string.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_write_memory(
    caller_ptr: *mut c_void,
    name: *const c_char,
    offset: i64,
    bytes: *const u8,
    len: c_int,
) -> c_int {
    if caller_ptr.is_null() || name.is_null() || bytes.is_null() {
        return -1;
    }
    if len < 0 {
        return -1;
    }
    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -1,
    };
    // Copy bytes into an owned Vec so the FFI-callee owns the data across the
    // wasmtime::Memory::write boundary (bytes ptr lifetime is a caller
    // guarantee only for this call).
    let data: Vec<u8> = unsafe { std::slice::from_raw_parts(bytes, len as usize) }.to_vec();
    ffi_utils::ffi_try_code(|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let export = caller
            .get_export(&name_str)
            .ok_or_else(|| crate::error::WasmtimeError::Runtime {
                message: format!("caller has no exported memory named '{}'", name_str),
                backtrace: None,
            })?;
        let memory = export
            .into_memory()
            .ok_or_else(|| crate::error::WasmtimeError::Runtime {
                message: format!("caller export '{}' is not a memory", name_str),
                backtrace: None,
            })?;

        memory
            .write(&mut caller.as_context_mut(), offset as usize, &data)
            .map_err(|e| crate::error::WasmtimeError::Runtime {
                message: format!("Caller-scoped memory write failed: {}", e),
                backtrace: None,
            })?;
        Ok(())
    })
}

// ===========================================================================
// F-Wasmtime4j-Panama-Caller-Scoped-Mutation-FFI r.4 slice 3 (2026-07-28).
//
// Instantiate + Linker.define_{memory,table,global} FFI parity. Bodies port
// from JNI's `nativeCallerInstantiate` + `nativeCallerLinkerDefineMemory` /
// `Table` / `Global` verbatim. Same registry-lookup + as_context_mut() paths.
//
// FromExport variants excluded per charter §Out-of-scope — Panama picks up
// a cleaner by-handle path from the start; if the by-handle path has the
// same "not registered" bug pattern, addressed in a follow-on slice on
// demand.
// ===========================================================================

/// Instantiate an `InstancePre` against the caller's live wasmtime context.
///
/// Writes the resulting instance handle to `*instance_out` and returns 0
/// on success. On failure, `*instance_out` is set to null and a non-zero
/// error code is returned (error retrievable via last-error FFI).
///
/// Safety: `instance_out` must be a valid `*mut *mut c_void`.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_instantiate(
    caller_ptr: *mut c_void,
    instance_pre_ptr: *mut c_void,
    instance_out: *mut *mut c_void,
) -> c_int {
    if instance_out.is_null() {
        return -1;
    }
    unsafe {
        *instance_out = std::ptr::null_mut();
    }
    if caller_ptr.is_null() || instance_pre_ptr.is_null() {
        return -1;
    }
    ffi_utils::ffi_try_code(|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let pre = unsafe {
            &*(instance_pre_ptr as *const crate::linker::InstancePreWrapper)
        };

        let ctx = caller.as_context_mut();
        let instance = pre.instantiate_with_context(ctx)?;
        unsafe {
            *instance_out = Box::into_raw(Box::new(instance)) as *mut c_void;
        }
        Ok(())
    })
}

/// Define a memory extern into a Linker using the caller's live store
/// context. Mirrors JNI's `nativeCallerLinkerDefineMemory`.
///
/// Returns 0 on success, non-zero error code on failure.
///
/// Safety: `module_name` and `name` must be valid nul-terminated C strings.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_linker_define_memory(
    caller_ptr: *mut c_void,
    linker_ptr: *mut c_void,
    module_name: *const c_char,
    name: *const c_char,
    memory_ptr: *mut c_void,
) -> c_int {
    if caller_ptr.is_null()
        || linker_ptr.is_null()
        || memory_ptr.is_null()
        || module_name.is_null()
        || name.is_null()
    {
        return -1;
    }
    let module_name_str = match unsafe { std::ffi::CStr::from_ptr(module_name) }.to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -1,
    };
    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -1,
    };

    ffi_utils::ffi_try_code(|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let linker = unsafe {
            crate::linker::core::get_linker_ref(linker_ptr as *const c_void)?
        };
        let memory =
            unsafe { crate::memory::core::get_memory_ref(memory_ptr as *const c_void)? };

        let extern_memory = if let Some(wasmtime_memory) = memory.inner() {
            wasmtime::Extern::Memory(*wasmtime_memory)
        } else if let Some(wasmtime_shared_memory) = memory.inner_shared() {
            wasmtime::Extern::SharedMemory(wasmtime_shared_memory.clone())
        } else {
            return Err(crate::error::WasmtimeError::Linker {
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
            .map_err(|e| crate::error::WasmtimeError::Linker {
                message: format!(
                    "Caller-scoped Linker.defineMemory '{}::{}' failed: {}",
                    module_name_str, name_str, e
                ),
            })?;
        Ok(())
    })
}

/// Define a table extern into a Linker. Mirrors JNI's
/// `nativeCallerLinkerDefineTable`. Returns 0 on success, non-zero on failure.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_linker_define_table(
    caller_ptr: *mut c_void,
    linker_ptr: *mut c_void,
    module_name: *const c_char,
    name: *const c_char,
    table_ptr: *mut c_void,
) -> c_int {
    if caller_ptr.is_null()
        || linker_ptr.is_null()
        || table_ptr.is_null()
        || module_name.is_null()
        || name.is_null()
    {
        return -1;
    }
    let module_name_str = match unsafe { std::ffi::CStr::from_ptr(module_name) }.to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -1,
    };
    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -1,
    };

    ffi_utils::ffi_try_code(|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let linker = unsafe {
            crate::linker::core::get_linker_ref(linker_ptr as *const c_void)?
        };
        let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };

        let wasmtime_table_arc = table.wasmtime_table();
        let wasmtime_table_lock =
            wasmtime_table_arc
                .lock()
                .map_err(|e| crate::error::WasmtimeError::Concurrency {
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
            .map_err(|e| crate::error::WasmtimeError::Linker {
                message: format!(
                    "Caller-scoped Linker.defineTable '{}::{}' failed: {}",
                    module_name_str, name_str, e
                ),
            })?;
        Ok(())
    })
}

/// Define a global extern into a Linker. Mirrors JNI's
/// `nativeCallerLinkerDefineGlobal`. Returns 0 on success, non-zero on failure.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_linker_define_global(
    caller_ptr: *mut c_void,
    linker_ptr: *mut c_void,
    module_name: *const c_char,
    name: *const c_char,
    global_ptr: *mut c_void,
) -> c_int {
    if caller_ptr.is_null()
        || linker_ptr.is_null()
        || global_ptr.is_null()
        || module_name.is_null()
        || name.is_null()
    {
        return -1;
    }
    let module_name_str = match unsafe { std::ffi::CStr::from_ptr(module_name) }.to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -1,
    };
    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -1,
    };

    ffi_utils::ffi_try_code(|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let linker = unsafe {
            crate::linker::core::get_linker_ref(linker_ptr as *const c_void)?
        };
        let global =
            unsafe { crate::global::core::get_global_ref(global_ptr as *const c_void)? };

        let wasmtime_global_arc = global.wasmtime_global();
        let wasmtime_global_lock =
            wasmtime_global_arc
                .lock()
                .map_err(|e| crate::error::WasmtimeError::Concurrency {
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
            .map_err(|e| crate::error::WasmtimeError::Linker {
                message: format!(
                    "Caller-scoped Linker.defineGlobal '{}::{}' failed: {}",
                    module_name_str, name_str, e
                ),
            })?;
        Ok(())
    })
}

// ===========================================================================
// F-Wasmtime4j-Panama-Consumer-Gated-Followups r.2 (2026-07-28).
//
// FuncToRegistryId FFI parity with JNI's `nativeCallerFuncToRegistryId`
// (see `wasmtime4j-native/src/jni/caller.rs:705-724`). Unblocks non-null
// funcref for `wasmtime4j_panama_caller_grow_table` and
// `wasmtime4j_panama_caller_set_table_element` — previously
// `PanamaCaller.resolveRefIdForMutation` threw UnsupportedOperationException
// for non-null `init`/`value`.
//
// Body mirrors JNI verbatim (same FunctionHandle → Func extraction, same
// `crate::table::core::register_function_reference` registration under
// `caller.data().store_id`).
// ===========================================================================

/// Register a caller-scoped function as a funcref in REFERENCE_REGISTRY under
/// the caller's store_id, returning the registry id used by
/// `wasmtime4j_panama_caller_grow_table` and
/// `wasmtime4j_panama_caller_set_table_element`.
///
/// Returns 0 on null-arg (null → registry-id 0 == null funcref sentinel).
/// Returns positive registry id on success.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_func_to_registry_id(
    caller_ptr: *mut c_void,
    function_ptr: *mut c_void,
) -> i64 {
    if caller_ptr.is_null() || function_ptr.is_null() {
        return 0;
    }
    ffi_utils::ffi_try_code_i64(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let store_id = caller.data().store_id;
        let func_handle =
            unsafe { &*(function_ptr as *const crate::jni::function::FunctionHandle) };
        let func = func_handle.get_func().clone();
        let id = crate::table::core::register_function_reference(func, store_id)?;
        Ok(id as i64)
    })
}

/// F-Wasmtime4j-Panama-FuncToRegistryId-Wire-Alignment (2026-07-29) —
/// Panama-side sibling to `wasmtime4j_panama_caller_func_to_registry_id`.
///
/// Takes a `*const wasmtime::Func` (the shape `PanamaCallerFunction`
/// carries — produced by `wasmtime4j_panama_caller_get_function`), NOT
/// a `*const crate::jni::function::FunctionHandle`. Registers the func
/// into REFERENCE_REGISTRY under the caller's store_id and returns the
/// registry id.
///
/// Convention matches the sibling: 0 on null-arg (registry-id 0 == null
/// funcref sentinel), positive registry id on success.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_func_ptr_to_registry_id(
    caller_ptr: *mut c_void,
    function_ptr: *mut c_void,
) -> i64 {
    if caller_ptr.is_null() || function_ptr.is_null() {
        return 0;
    }
    ffi_utils::ffi_try_code_i64(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let store_id = caller.data().store_id;
        // Panama-tier funcHandle is a raw wasmtime::Func boxed by
        // wasmtime4j_panama_caller_get_function. wasmtime::Func is Copy.
        let func = unsafe { *(function_ptr as *const wasmtime::Func) };
        let id = crate::table::core::register_function_reference(func, store_id)?;
        Ok(id as i64)
    })
}

/// Define a memory extern into a Linker by looking it up on the caller by
/// export name. Mirrors JNI's `nativeCallerLinkerDefineMemoryFromExport`
/// (`wasmtime4j-native/src/jni/caller.rs:860`). Preferable to
/// `wasmtime4j_panama_caller_linker_define_memory` when the source memory is
/// the caller's own export — avoids the api-layer registry-handle roundtrip.
///
/// Returns 0 on success, non-zero on failure. Error retrievable via
/// last-error FFI.
///
/// Safety: `module_name`, `name`, and `caller_export_name` must be valid
/// nul-terminated C strings.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_linker_define_memory_from_export(
    caller_ptr: *mut c_void,
    linker_ptr: *mut c_void,
    module_name: *const c_char,
    name: *const c_char,
    caller_export_name: *const c_char,
) -> c_int {
    if caller_ptr.is_null()
        || linker_ptr.is_null()
        || module_name.is_null()
        || name.is_null()
        || caller_export_name.is_null()
    {
        return -1;
    }
    let module_name_str = match unsafe { std::ffi::CStr::from_ptr(module_name) }.to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -1,
    };
    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -1,
    };
    let export_name_str =
        match unsafe { std::ffi::CStr::from_ptr(caller_export_name) }.to_str() {
            Ok(s) => s.to_string(),
            Err(_) => return -1,
        };

    ffi_utils::ffi_try_code(|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let linker = unsafe {
            crate::linker::core::get_linker_ref(linker_ptr as *const c_void)?
        };

        let export = caller
            .get_export(&export_name_str)
            .ok_or_else(|| crate::error::WasmtimeError::Linker {
                message: format!("caller has no export named '{}'", export_name_str),
            })?;
        let memory = export.into_memory().ok_or_else(|| {
            crate::error::WasmtimeError::Linker {
                message: format!(
                    "caller export '{}' is not a memory",
                    export_name_str
                ),
            }
        })?;

        let mut linker_lock = linker.inner()?;
        linker_lock
            .define(
                &mut caller.as_context_mut(),
                &module_name_str,
                &name_str,
                wasmtime::Extern::Memory(memory),
            )
            .map_err(|e| crate::error::WasmtimeError::Linker {
                message: format!(
                    "Caller-scoped Linker.defineMemoryFromExport '{}::{}' (from '{}') failed: {}",
                    module_name_str, name_str, export_name_str, e
                ),
            })?;
        Ok(())
    })
}

/// Define a table extern into a Linker by looking it up on the caller by
/// export name. Mirrors JNI's `nativeCallerLinkerDefineTableFromExport`
/// (`wasmtime4j-native/src/jni/caller.rs:920`). See
/// `wasmtime4j_panama_caller_linker_define_memory_from_export` for shape;
/// this is the parallel entry for tables.
///
/// Safety: `module_name`, `name`, and `caller_export_name` must be valid
/// nul-terminated C strings.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_linker_define_table_from_export(
    caller_ptr: *mut c_void,
    linker_ptr: *mut c_void,
    module_name: *const c_char,
    name: *const c_char,
    caller_export_name: *const c_char,
) -> c_int {
    if caller_ptr.is_null()
        || linker_ptr.is_null()
        || module_name.is_null()
        || name.is_null()
        || caller_export_name.is_null()
    {
        return -1;
    }
    let module_name_str = match unsafe { std::ffi::CStr::from_ptr(module_name) }.to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -1,
    };
    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -1,
    };
    let export_name_str =
        match unsafe { std::ffi::CStr::from_ptr(caller_export_name) }.to_str() {
            Ok(s) => s.to_string(),
            Err(_) => return -1,
        };

    ffi_utils::ffi_try_code(|| {
        use wasmtime::AsContextMut;

        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let linker = unsafe {
            crate::linker::core::get_linker_ref(linker_ptr as *const c_void)?
        };

        let export = caller
            .get_export(&export_name_str)
            .ok_or_else(|| crate::error::WasmtimeError::Linker {
                message: format!("caller has no export named '{}'", export_name_str),
            })?;
        let table = export.into_table().ok_or_else(|| {
            crate::error::WasmtimeError::Linker {
                message: format!(
                    "caller export '{}' is not a table",
                    export_name_str
                ),
            }
        })?;

        let mut linker_lock = linker.inner()?;
        linker_lock
            .define(
                &mut caller.as_context_mut(),
                &module_name_str,
                &name_str,
                wasmtime::Extern::Table(table),
            )
            .map_err(|e| crate::error::WasmtimeError::Linker {
                message: format!(
                    "Caller-scoped Linker.defineTableFromExport '{}::{}' (from '{}') failed: {}",
                    module_name_str, name_str, export_name_str, e
                ),
            })?;
        Ok(())
    })
}

// ===========================================================================
// F-Wasmtime4j-Panama-Caller-Scoped-Mutation-FFI r.5 slice 4 (2026-07-28).
//
// FFI unit tests. One null-arg rejection test per new entry proves the entry
// is linkable + returns the -1 sentinel for null-pointer args without
// segfaulting or panicking. This is the minimum viable coverage — a live
// callback frame is required for positive-path testing, which happens at
// integration scope in a Panama-consumer harness.
// ===========================================================================

#[cfg(test)]
mod caller_scoped_mutation_null_arg_tests {
    use super::*;
    use std::ptr;

    // --- r.2 slice 1: Table mutation ---

    #[test]
    fn grow_table_null_caller_returns_neg_one() {
        let ret = wasmtime4j_panama_caller_grow_table(
            ptr::null_mut(),
            0x1 as *mut c_void, // non-null table pointer
            1,
            0,
        );
        assert_eq!(ret, -1);
    }

    #[test]
    fn grow_table_null_table_returns_neg_one() {
        let ret = wasmtime4j_panama_caller_grow_table(
            0x1 as *mut c_void, // non-null caller
            ptr::null_mut(),
            1,
            0,
        );
        assert_eq!(ret, -1);
    }

    #[test]
    fn set_table_element_null_caller_returns_neg_one() {
        let ret = wasmtime4j_panama_caller_set_table_element(
            ptr::null_mut(),
            0x1 as *mut c_void,
            0,
            0,
        );
        assert_eq!(ret, -1);
    }

    // --- r.3 slice 2: Memory mutation ---

    #[test]
    fn grow_memory_null_caller_returns_neg_one() {
        let ret = wasmtime4j_panama_caller_grow_memory(ptr::null_mut(), 0x1 as *mut c_void, 1);
        assert_eq!(ret, -1);
    }

    #[test]
    fn read_memory_null_out_buf_returns_neg_one() {
        let name = std::ffi::CString::new("memory").unwrap();
        let ret = wasmtime4j_panama_caller_read_memory(
            0x1 as *mut c_void, // non-null caller
            name.as_ptr(),
            0,
            4,
            ptr::null_mut(), // null out_buf triggers reject
        );
        assert_eq!(ret, -1);
    }

    #[test]
    fn read_memory_negative_length_returns_neg_one() {
        let name = std::ffi::CString::new("memory").unwrap();
        let mut buf = [0u8; 4];
        let ret = wasmtime4j_panama_caller_read_memory(
            0x1 as *mut c_void,
            name.as_ptr(),
            0,
            -1, // negative length rejected up-front
            buf.as_mut_ptr(),
        );
        assert_eq!(ret, -1);
    }

    #[test]
    fn write_memory_null_bytes_returns_neg_one() {
        let name = std::ffi::CString::new("memory").unwrap();
        let ret = wasmtime4j_panama_caller_write_memory(
            0x1 as *mut c_void,
            name.as_ptr(),
            0,
            ptr::null(),
            0,
        );
        assert_eq!(ret, -1);
    }

    // --- r.4 slice 3: Instantiate + Linker define ---

    #[test]
    fn instantiate_null_out_returns_neg_one() {
        let ret = wasmtime4j_panama_caller_instantiate(
            0x1 as *mut c_void,
            0x1 as *mut c_void,
            ptr::null_mut(), // null out param — critical safety guard
        );
        assert_eq!(ret, -1);
    }

    #[test]
    fn instantiate_null_caller_sets_out_null_and_returns_neg_one() {
        let mut out: *mut c_void = 0x1 as *mut c_void; // start non-null to verify clear
        let ret = wasmtime4j_panama_caller_instantiate(
            ptr::null_mut(),
            0x1 as *mut c_void,
            &mut out as *mut *mut c_void,
        );
        assert_eq!(ret, -1);
        assert!(out.is_null(), "instantiate should clear *out on failure");
    }

    #[test]
    fn linker_define_memory_null_module_name_returns_neg_one() {
        let name = std::ffi::CString::new("mem").unwrap();
        let ret = wasmtime4j_panama_caller_linker_define_memory(
            0x1 as *mut c_void,
            0x1 as *mut c_void,
            ptr::null(),
            name.as_ptr(),
            0x1 as *mut c_void,
        );
        assert_eq!(ret, -1);
    }

    #[test]
    fn linker_define_table_null_name_returns_neg_one() {
        let module = std::ffi::CString::new("env").unwrap();
        let ret = wasmtime4j_panama_caller_linker_define_table(
            0x1 as *mut c_void,
            0x1 as *mut c_void,
            module.as_ptr(),
            ptr::null(),
            0x1 as *mut c_void,
        );
        assert_eq!(ret, -1);
    }

    #[test]
    fn linker_define_global_null_global_returns_neg_one() {
        let module = std::ffi::CString::new("env").unwrap();
        let name = std::ffi::CString::new("g").unwrap();
        let ret = wasmtime4j_panama_caller_linker_define_global(
            0x1 as *mut c_void,
            0x1 as *mut c_void,
            module.as_ptr(),
            name.as_ptr(),
            ptr::null_mut(),
        );
        assert_eq!(ret, -1);
    }

    // --- F-Wasmtime4j-Panama-Consumer-Gated-Followups r.2 (2026-07-28) ---

    #[test]
    fn func_to_registry_id_null_caller_returns_zero() {
        // Convention: null args → 0 (registry-id 0 == null funcref sentinel).
        // Distinguishes from -1 error sentinel used by mutation FFIs.
        let ret = wasmtime4j_panama_caller_func_to_registry_id(
            ptr::null_mut(),
            0x1 as *mut c_void,
        );
        assert_eq!(ret, 0);
    }

    #[test]
    fn func_to_registry_id_null_function_returns_zero() {
        let ret = wasmtime4j_panama_caller_func_to_registry_id(
            0x1 as *mut c_void,
            ptr::null_mut(),
        );
        assert_eq!(ret, 0);
    }

    #[test]
    fn linker_define_memory_from_export_null_caller_returns_neg_one() {
        let module = std::ffi::CString::new("env").unwrap();
        let name = std::ffi::CString::new("mem").unwrap();
        let export = std::ffi::CString::new("memory").unwrap();
        let ret = wasmtime4j_panama_caller_linker_define_memory_from_export(
            ptr::null_mut(),
            0x1 as *mut c_void,
            module.as_ptr(),
            name.as_ptr(),
            export.as_ptr(),
        );
        assert_eq!(ret, -1);
    }

    #[test]
    fn linker_define_memory_from_export_null_export_name_returns_neg_one() {
        let module = std::ffi::CString::new("env").unwrap();
        let name = std::ffi::CString::new("mem").unwrap();
        let ret = wasmtime4j_panama_caller_linker_define_memory_from_export(
            0x1 as *mut c_void,
            0x1 as *mut c_void,
            module.as_ptr(),
            name.as_ptr(),
            ptr::null(),
        );
        assert_eq!(ret, -1);
    }

    #[test]
    fn linker_define_table_from_export_null_caller_returns_neg_one() {
        let module = std::ffi::CString::new("env").unwrap();
        let name = std::ffi::CString::new("tbl").unwrap();
        let export = std::ffi::CString::new("__indirect_function_table").unwrap();
        let ret = wasmtime4j_panama_caller_linker_define_table_from_export(
            ptr::null_mut(),
            0x1 as *mut c_void,
            module.as_ptr(),
            name.as_ptr(),
            export.as_ptr(),
        );
        assert_eq!(ret, -1);
    }

    #[test]
    fn linker_define_table_from_export_null_export_name_returns_neg_one() {
        let module = std::ffi::CString::new("env").unwrap();
        let name = std::ffi::CString::new("tbl").unwrap();
        let ret = wasmtime4j_panama_caller_linker_define_table_from_export(
            0x1 as *mut c_void,
            0x1 as *mut c_void,
            module.as_ptr(),
            name.as_ptr(),
            ptr::null(),
        );
        assert_eq!(ret, -1);
    }
}

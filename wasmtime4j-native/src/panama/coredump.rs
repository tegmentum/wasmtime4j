//! Panama FFI bindings for coredump introspection.
//!
//! These functions provide C-compatible access to the coredump registry,
//! matching the declarations in NativeExecutionBindings.java.

use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int, c_long};

use crate::coredump;
use crate::store::StoreData;

/// Free a coredump entry from the registry.
///
/// Returns 0 on success, -1 if the ID was not found.
#[no_mangle]
pub extern "C" fn wasmtime4j_coredump_free(coredump_id: u64) -> c_int {
    if coredump::remove(coredump_id) {
        0
    } else {
        -1
    }
}

/// Get the frame count for a coredump.
///
/// Returns the frame count, or -1 if the coredump was not found.
#[no_mangle]
pub extern "C" fn wasmtime4j_coredump_get_frame_count(coredump_id: u64) -> c_int {
    coredump::with_coredump(coredump_id, |cd| cd.frames().len() as c_int).unwrap_or(-1)
}

/// Get the trap message for a coredump.
///
/// Returns a newly allocated C string that must be freed with `wasmtime4j_coredump_string_free`,
/// or NULL if the coredump was not found.
#[no_mangle]
pub extern "C" fn wasmtime4j_coredump_get_trap_message(coredump_id: u64) -> *mut c_char {
    match coredump::get_trap_message(coredump_id) {
        Some(msg) => match CString::new(msg) {
            Ok(cs) => cs.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        None => std::ptr::null_mut(),
    }
}

/// Get the name of a coredump (typically the module name).
///
/// Returns a newly allocated C string that must be freed with `wasmtime4j_coredump_string_free`,
/// or NULL if not available.
#[no_mangle]
pub extern "C" fn wasmtime4j_coredump_get_name(coredump_id: u64) -> *mut c_char {
    let name = coredump::with_coredump(coredump_id, |cd| {
        // Try to get the name from the first module in the coredump
        let modules = cd.modules();
        if modules.is_empty() {
            return None;
        }
        modules[0].name().map(|s| s.to_string())
    });

    match name.flatten() {
        Some(n) => match CString::new(n) {
            Ok(cs) => cs.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        None => std::ptr::null_mut(),
    }
}

/// Free a C string allocated by coredump functions.
#[no_mangle]
pub extern "C" fn wasmtime4j_coredump_string_free(ptr: *mut c_char) {
    if !ptr.is_null() {
        unsafe {
            drop(CString::from_raw(ptr));
        }
    }
}

/// Serialize a coredump to the standard WebAssembly core dump binary format.
///
/// # Parameters
/// - coredump_id: ID of the coredump in the registry
/// - store_ptr: Pointer to the Store (required for serialization)
/// - name: Name for the coredump (C string)
/// - out_ptr: Output pointer for the serialized bytes
/// - out_len: Output pointer for the length of serialized bytes
///
/// Returns 0 on success, -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_coredump_serialize(
    coredump_id: u64,
    store_ptr: *mut std::ffi::c_void,
    name: *const c_char,
    out_ptr: *mut *mut u8,
    out_len: *mut c_long,
) -> c_int {
    if store_ptr.is_null() || name.is_null() || out_ptr.is_null() || out_len.is_null() {
        return -1;
    }

    let name_str = match unsafe { CStr::from_ptr(name) }.to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let store = unsafe { &mut *(store_ptr as *mut wasmtime::Store<StoreData>) };

    let result = coredump::with_coredump(coredump_id, |cd| cd.serialize(store, name_str));

    match result {
        Some(bytes) => {
            let len = bytes.len();
            let boxed = bytes.into_boxed_slice();
            let raw_ptr = Box::into_raw(boxed) as *mut u8;
            unsafe {
                *out_ptr = raw_ptr;
                *out_len = len as c_long;
            }
            0
        }
        None => -1,
    }
}

/// Free bytes allocated by coredump_serialize.
#[no_mangle]
pub extern "C" fn wasmtime4j_coredump_bytes_free(ptr: *mut u8, len: c_long) {
    if !ptr.is_null() && len > 0 {
        unsafe {
            let slice = std::slice::from_raw_parts_mut(ptr, len as usize);
            drop(Box::from_raw(slice as *mut [u8]));
        }
    }
}

/// Get frame information for a specific frame as JSON.
///
/// Returns a JSON string like:
/// `{"funcIndex":0,"funcName":"foo","moduleOffset":42,"funcOffset":10}`
///
/// Returns a newly allocated C string that must be freed with `wasmtime4j_coredump_string_free`,
/// or NULL if not found.
#[no_mangle]
pub extern "C" fn wasmtime4j_coredump_get_frame_info(
    coredump_id: u64,
    frame_index: c_int,
) -> *mut c_char {
    let json = coredump::with_coredump(coredump_id, |cd| {
        let frames = cd.frames();
        if frame_index < 0 || (frame_index as usize) >= frames.len() {
            return None;
        }
        let frame = &frames[frame_index as usize];
        Some(frame_to_json(frame))
    });

    match json.flatten() {
        Some(s) => match CString::new(s) {
            Ok(cs) => cs.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        None => std::ptr::null_mut(),
    }
}

/// Get all frames as a JSON array.
///
/// Returns a newly allocated C string that must be freed with `wasmtime4j_coredump_string_free`,
/// or NULL if not found.
#[no_mangle]
pub extern "C" fn wasmtime4j_coredump_get_all_frames(coredump_id: u64) -> *mut c_char {
    let json = coredump::with_coredump(coredump_id, |cd| {
        let frames = cd.frames();
        let frame_jsons: Vec<String> = frames.iter().map(|f| frame_to_json(f)).collect();
        format!("[{}]", frame_jsons.join(","))
    });

    match json {
        Some(s) => match CString::new(s) {
            Ok(cs) => cs.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        None => std::ptr::null_mut(),
    }
}

/// Get the total number of coredumps in the registry.
#[no_mangle]
pub extern "C" fn wasmtime4j_coredump_get_count() -> c_int {
    coredump::count() as c_int
}

/// Get all coredump IDs as a JSON array.
///
/// Returns a newly allocated C string that must be freed with `wasmtime4j_coredump_string_free`,
/// or NULL on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_coredump_get_all_ids() -> *mut c_char {
    let ids = coredump::all_ids();
    let json = format!(
        "[{}]",
        ids.iter()
            .map(|id| id.to_string())
            .collect::<Vec<_>>()
            .join(",")
    );
    match CString::new(json) {
        Ok(cs) => cs.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Clear all coredumps from the registry.
///
/// Returns 0 on success.
#[no_mangle]
pub extern "C" fn wasmtime4j_coredump_clear_all() -> c_int {
    coredump::clear_all();
    0
}

/// Convert a FrameInfo to a JSON string.
pub fn frame_to_json(frame: &wasmtime::FrameInfo) -> String {
    let func_index = frame.func_index();
    let func_name = frame
        .func_name()
        .map(|n| format!("\"{}\"", escape_json_string(n)))
        .unwrap_or_else(|| "null".to_string());
    let module_offset = frame
        .module_offset()
        .map(|o| o.to_string())
        .unwrap_or_else(|| "null".to_string());
    let func_offset = frame
        .func_offset()
        .map(|o| o.to_string())
        .unwrap_or_else(|| "null".to_string());

    format!(
        "{{\"funcIndex\":{},\"funcName\":{},\"moduleOffset\":{},\"funcOffset\":{}}}",
        func_index, func_name, module_offset, func_offset
    )
}

/// Escape special characters in a JSON string value.
fn escape_json_string(s: &str) -> String {
    let mut result = String::with_capacity(s.len());
    for c in s.chars() {
        match c {
            '"' => result.push_str("\\\""),
            '\\' => result.push_str("\\\\"),
            '\n' => result.push_str("\\n"),
            '\r' => result.push_str("\\r"),
            '\t' => result.push_str("\\t"),
            c if (c as u32) < 0x20 => {
                result.push_str(&format!("\\u{:04x}", c as u32));
            }
            c => result.push(c),
        }
    }
    result
}

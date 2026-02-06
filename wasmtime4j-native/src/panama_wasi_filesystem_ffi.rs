//! Panama FFI bindings for WASI Preview 2 Filesystem operations
//!
//! This module provides C-compatible FFI functions for use by the wasmtime4j-panama module
//! to access WASI Preview 2 filesystem operations (wasi:filesystem).
//!
//! All functions use C calling conventions and handle memory management appropriately.

use std::os::raw::{c_char, c_int, c_long, c_void};
use std::ptr;
use std::slice;
use std::ffi::CStr;

use crate::wasi_preview2::WasiPreview2Context;
use crate::wasi_filesystem_helpers;

/// Read via stream from a descriptor at the specified offset
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the descriptor
/// - `offset`: Byte offset to start reading from
/// - `out_stream_handle`: Pointer to write the created stream handle
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_read_via_stream(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    offset: c_long,
    out_stream_handle: *mut *mut c_void,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() || out_stream_handle.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Call helper function
    match wasi_filesystem_helpers::read_via_stream(context, descriptor_id, offset as u64) {
        Ok(stream_id) => {
            unsafe {
                *out_stream_handle = stream_id as *mut c_void;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Write via stream to a descriptor at the specified offset
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the descriptor
/// - `offset`: Byte offset to start writing at
/// - `out_stream_handle`: Pointer to write the created stream handle
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_write_via_stream(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    offset: c_long,
    out_stream_handle: *mut *mut c_void,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() || out_stream_handle.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Call helper function
    match wasi_filesystem_helpers::write_via_stream(context, descriptor_id, offset as u64) {
        Ok(stream_id) => {
            unsafe {
                *out_stream_handle = stream_id as *mut c_void;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Append via stream to a descriptor
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the descriptor
/// - `out_stream_handle`: Pointer to write the created stream handle
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_append_via_stream(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    out_stream_handle: *mut *mut c_void,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() || out_stream_handle.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Call helper function
    match wasi_filesystem_helpers::append_via_stream(context, descriptor_id) {
        Ok(stream_id) => {
            unsafe {
                *out_stream_handle = stream_id as *mut c_void;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get the type of a descriptor
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the descriptor
/// - `out_type`: Pointer to write the descriptor type (0-7)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_get_type(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    out_type: *mut c_int,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() || out_type.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Call helper function
    match wasi_filesystem_helpers::get_type(context, descriptor_id) {
        Ok(type_code) => {
            unsafe {
                *out_type = type_code as c_int;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get the flags of a descriptor
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the descriptor
/// - `out_flags`: Pointer to write the flags bitmask
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_get_flags(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    out_flags: *mut c_int,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() || out_flags.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Call helper function
    match wasi_filesystem_helpers::get_flags(context, descriptor_id) {
        Ok(flags) => {
            unsafe {
                *out_flags = flags as c_int;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Set the size of a file
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the descriptor
/// - `size`: New file size in bytes
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_set_size(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    size: c_long,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Call helper function
    match wasi_filesystem_helpers::set_size(context, descriptor_id, size as u64) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Synchronize file data to storage
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the descriptor
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_sync_data(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Call helper function
    match wasi_filesystem_helpers::sync_data(context, descriptor_id) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Synchronize file data and metadata to storage
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the descriptor
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_sync(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Call helper function
    match wasi_filesystem_helpers::sync(context, descriptor_id) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Open a filesystem object at a path
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the base descriptor
/// - `path`: Path string (UTF-8)
/// - `path_len`: Length of path string
/// - `path_flags`: Path resolution flags bitmask
/// - `open_flags`: File creation flags bitmask
/// - `descriptor_flags`: New descriptor flags bitmask
/// - `out_descriptor_handle`: Pointer to write the new descriptor handle
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_open_at(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    path: *const c_char,
    _path_len: c_int,
    path_flags: c_int,
    open_flags: c_int,
    descriptor_flags: c_int,
    out_descriptor_handle: *mut *mut c_void,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() || path.is_null() || out_descriptor_handle.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Convert C string to Rust string
    let path_str = unsafe {
        CStr::from_ptr(path)
            .to_str()
            .unwrap_or("")
    };

    // Combine flags
    let combined_flags = (path_flags as u32) | (open_flags as u32) | (descriptor_flags as u32);

    // Call helper function
    match wasi_filesystem_helpers::open_at(context, descriptor_id, path_str, combined_flags, 0) {
        Ok(new_descriptor_id) => {
            unsafe {
                *out_descriptor_handle = new_descriptor_id as *mut c_void;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Create a directory at a path
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the base descriptor
/// - `path`: Path string (UTF-8)
/// - `path_len`: Length of path string
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_create_directory_at(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    path: *const c_char,
    _path_len: c_int,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() || path.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Convert C string to Rust string
    let path_str = unsafe {
        CStr::from_ptr(path)
            .to_str()
            .unwrap_or("")
    };

    // Call helper function
    match wasi_filesystem_helpers::create_directory_at(context, descriptor_id, path_str) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Read directory entries
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the directory descriptor
/// - `out_entries`: Pointer to buffer for null-terminated entry names
/// - `out_entries_len`: Pointer to write total length of entries buffer
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_read_directory(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    out_entries: *mut *mut c_char,
    out_entries_len: *mut c_int,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() || out_entries.is_null() || out_entries_len.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Call helper function to get directory entries
    match wasi_filesystem_helpers::read_directory(context, descriptor_id) {
        Ok(entries) => {
            // Allocate memory for the entries array
            // Each entry consists of: [name_length (4 bytes), name (variable), entry_type (4 bytes)]
            let mut total_size = 0;
            for (name, _) in &entries {
                total_size += 4 + name.len() + 4; // length + name + type
            }

            if total_size == 0 {
                unsafe {
                    *out_entries = ptr::null_mut();
                    *out_entries_len = 0;
                }
                return 0;
            }

            // Allocate buffer
            let layout = match std::alloc::Layout::from_size_align(total_size, 1) {
                Ok(l) => l,
                Err(_) => return -1,
            };
            let buffer = unsafe { std::alloc::alloc(layout) };
            if buffer.is_null() {
                return -1;
            }

            // Write entries to buffer
            let mut offset = 0;
            for (name, entry_type) in entries {
                let name_bytes = name.as_bytes();
                let name_len = name_bytes.len() as i32;

                // Write name length
                unsafe {
                    ptr::copy_nonoverlapping(
                        &name_len as *const i32 as *const u8,
                        buffer.add(offset),
                        4,
                    );
                }
                offset += 4;

                // Write name bytes
                unsafe {
                    ptr::copy_nonoverlapping(name_bytes.as_ptr(), buffer.add(offset), name_bytes.len());
                }
                offset += name_bytes.len();

                // Write entry type
                unsafe {
                    ptr::copy_nonoverlapping(
                        &entry_type as *const u32 as *const u8,
                        buffer.add(offset),
                        4,
                    );
                }
                offset += 4;
            }

            unsafe {
                *out_entries = buffer as *mut c_char;
                *out_entries_len = total_size as c_int;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Read the target of a symbolic link
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the base descriptor
/// - `path`: Path to the symbolic link (UTF-8)
/// - `path_len`: Length of path string
/// - `out_target`: Pointer to buffer for target path
/// - `out_target_len`: Pointer to write target path length
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_read_link_at(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    path: *const c_char,
    _path_len: c_int,
    out_target: *mut *mut c_char,
    out_target_len: *mut c_int,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() || path.is_null() || out_target.is_null() || out_target_len.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Convert C string to Rust string
    let path_str = unsafe {
        CStr::from_ptr(path)
            .to_str()
            .unwrap_or("")
    };

    // Call helper function (MVP returns empty string)
    match wasi_filesystem_helpers::read_link_at(context, descriptor_id, path_str) {
        Ok(_target) => {
            unsafe {
                *out_target = ptr::null_mut();
                *out_target_len = 0;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Remove a file at a path
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the base descriptor
/// - `path`: Path to the file (UTF-8)
/// - `path_len`: Length of path string
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_unlink_file_at(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    path: *const c_char,
    _path_len: c_int,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() || path.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Convert C string to Rust string
    let path_str = unsafe {
        CStr::from_ptr(path)
            .to_str()
            .unwrap_or("")
    };

    // Call helper function
    match wasi_filesystem_helpers::unlink_file_at(context, descriptor_id, path_str) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Remove a directory at a path
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the base descriptor
/// - `path`: Path to the directory (UTF-8)
/// - `path_len`: Length of path string
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_remove_directory_at(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    path: *const c_char,
    _path_len: c_int,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() || path.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Convert C string to Rust string
    let path_str = unsafe {
        CStr::from_ptr(path)
            .to_str()
            .unwrap_or("")
    };

    // Call helper function
    match wasi_filesystem_helpers::remove_directory_at(context, descriptor_id, path_str) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Rename a filesystem object
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `old_descriptor_handle`: Pointer to the old parent descriptor
/// - `old_path`: Old path (UTF-8)
/// - `old_path_len`: Length of old path string
/// - `new_descriptor_handle`: Pointer to the new parent descriptor
/// - `new_path`: New path (UTF-8)
/// - `new_path_len`: Length of new path string
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_rename_at(
    context_handle: *mut c_void,
    old_descriptor_handle: *mut c_void,
    old_path: *const c_char,
    old_path_len: c_int,
    new_descriptor_handle: *mut c_void,
    new_path: *const c_char,
    new_path_len: c_int,
) -> c_int {
    if context_handle.is_null() || old_descriptor_handle.is_null() || old_path.is_null()
        || new_descriptor_handle.is_null() || new_path.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let old_descriptor_id = old_descriptor_handle as u64;
    let new_descriptor_id = new_descriptor_handle as u64;

    // Convert C strings to Rust strings
    let old_path_str = unsafe {
        CStr::from_ptr(old_path)
            .to_str()
            .unwrap_or("")
    };
    let new_path_str = unsafe {
        CStr::from_ptr(new_path)
            .to_str()
            .unwrap_or("")
    };

    // Call helper function
    match wasi_filesystem_helpers::rename_at(context, old_descriptor_id, old_path_str, new_descriptor_id, new_path_str) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Create a symbolic link
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the base descriptor
/// - `old_path`: Target path (UTF-8)
/// - `old_path_len`: Length of target path string
/// - `new_path`: Link path (UTF-8)
/// - `new_path_len`: Length of link path string
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_symlink_at(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
    old_path: *const c_char,
    old_path_len: c_int,
    new_path: *const c_char,
    new_path_len: c_int,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() || old_path.is_null() || new_path.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Convert C strings to Rust strings
    let old_path_str = unsafe {
        CStr::from_ptr(old_path)
            .to_str()
            .unwrap_or("")
    };
    let new_path_str = unsafe {
        CStr::from_ptr(new_path)
            .to_str()
            .unwrap_or("")
    };

    // Call helper function
    match wasi_filesystem_helpers::symlink_at(context, descriptor_id, old_path_str, new_path_str) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Create a hard link
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `old_descriptor_handle`: Pointer to the old parent descriptor
/// - `old_path_flags`: Path resolution flags for old path
/// - `old_path`: Existing object path (UTF-8)
/// - `old_path_len`: Length of old path string
/// - `new_descriptor_handle`: Pointer to the new parent descriptor
/// - `new_path`: New link path (UTF-8)
/// - `new_path_len`: Length of new path string
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_link_at(
    context_handle: *mut c_void,
    old_descriptor_handle: *mut c_void,
    old_path_flags: c_int,
    old_path: *const c_char,
    old_path_len: c_int,
    new_descriptor_handle: *mut c_void,
    new_path: *const c_char,
    new_path_len: c_int,
) -> c_int {
    if context_handle.is_null() || old_descriptor_handle.is_null() || old_path.is_null()
        || new_descriptor_handle.is_null() || new_path.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let old_descriptor_id = old_descriptor_handle as u64;
    let new_descriptor_id = new_descriptor_handle as u64;

    // Convert C strings to Rust strings
    let old_path_str = unsafe {
        CStr::from_ptr(old_path)
            .to_str()
            .unwrap_or("")
    };
    let new_path_str = unsafe {
        CStr::from_ptr(new_path)
            .to_str()
            .unwrap_or("")
    };

    // Note: old_path_flags parameter is ignored in MVP implementation
    let _ = old_path_flags;

    // Call helper function
    match wasi_filesystem_helpers::link_at(context, old_descriptor_id, old_path_str, new_descriptor_id, new_path_str) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Check if two descriptors refer to the same filesystem object
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle1`: Pointer to the first descriptor
/// - `descriptor_handle2`: Pointer to the second descriptor
/// - `out_same`: Pointer to write the result (1 = same, 0 = different)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_is_same_object(
    context_handle: *mut c_void,
    descriptor_handle1: *mut c_void,
    descriptor_handle2: *mut c_void,
    out_same: *mut c_int,
) -> c_int {
    if context_handle.is_null() || descriptor_handle1.is_null() || descriptor_handle2.is_null() || out_same.is_null() {
        return -1;
    }

    // TODO: Implement actual WASI Preview 2 descriptor is_same_object
    unsafe {
        *out_same = 0;
    }
    0
}

/// Close a descriptor
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `descriptor_handle`: Pointer to the descriptor
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_descriptor_close(
    context_handle: *mut c_void,
    descriptor_handle: *mut c_void,
) -> c_int {
    if context_handle.is_null() || descriptor_handle.is_null() {
        return -1;
    }

    // Get context and convert handles
    let context = unsafe { &*(context_handle as *const WasiPreview2Context) };
    let descriptor_id = descriptor_handle as u64;

    // Call helper function
    match wasi_filesystem_helpers::close_descriptor(context, descriptor_id) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

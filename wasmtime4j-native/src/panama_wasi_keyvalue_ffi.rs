//! Panama FFI bindings for WASI keyvalue operations
//!
//! This module provides C-compatible FFI functions for use by the wasmtime4j-panama module
//! to access WASI keyvalue operations (wasi:keyvalue).
//!
//! All functions use C calling conventions and handle memory management appropriately.

use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int, c_longlong, c_uchar, c_void};
use std::ptr;
use std::slice;

use crate::wasi_keyvalue_helpers::WasiKeyValueContext;
use crate::{ffi_boundary_i32, ffi_boundary_ptr, ffi_boundary_result, ffi_boundary_void};

// =============================================================================
// Helper Functions
// =============================================================================

/// Helper function to get context from handle
unsafe fn get_context<'a>(context_handle: *mut c_void) -> Option<&'a WasiKeyValueContext> {
    if context_handle.is_null() {
        return None;
    }
    let ptr = context_handle as *const WasiKeyValueContext;
    if ptr.is_null() {
        return None;
    }
    Some(&*ptr)
}

/// Helper function to get mutable context from handle
unsafe fn get_context_mut<'a>(context_handle: *mut c_void) -> Option<&'a mut WasiKeyValueContext> {
    if context_handle.is_null() {
        return None;
    }
    let ptr = context_handle as *mut WasiKeyValueContext;
    if ptr.is_null() {
        return None;
    }
    Some(&mut *ptr)
}

// =============================================================================
// Context Management Functions
// =============================================================================

/// Creates a new WASI keyvalue context
///
/// # Returns
/// Pointer to the context on success, null on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_context_create() -> *mut c_void {
    ffi_boundary_ptr!({
        match WasiKeyValueContext::new() {
            Ok(ctx) => Box::into_raw(Box::new(ctx)) as *mut c_void,
            Err(_) => ptr::null_mut(),
        }
    })
}

/// Destroys a WASI keyvalue context
///
/// # Safety
/// The context_handle must be a valid pointer returned by context_create
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_context_destroy(context_handle: *mut c_void) {
    ffi_boundary_void!({
        if !context_handle.is_null() {
            unsafe {
                let _ = Box::from_raw(context_handle as *mut WasiKeyValueContext);
            }
        }
    })
}

/// Gets the context ID
///
/// # Returns
/// The context ID, or 0 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_context_id(
    context_handle: *mut c_void,
) -> c_longlong {
    ffi_boundary_result!(0i64, {
        let context = unsafe { get_context(context_handle) };
        match context {
            Some(ctx) => Ok(ctx.id() as c_longlong),
            None => Ok(0),
        }
    })
}

/// Checks if the context is valid
///
/// # Returns
/// 1 if valid, 0 if invalid or error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_context_is_valid(
    context_handle: *mut c_void,
) -> c_int {
    ffi_boundary_i32!({
        let context = unsafe { get_context(context_handle) };
        match context {
            Some(ctx) => Ok(if ctx.is_valid() { 1 } else { 0 }),
            None => Ok(0),
        }
    })
}

// =============================================================================
// Basic CRUD Operations
// =============================================================================

/// Gets a value by key
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI keyvalue context
/// - `key`: The key to retrieve (null-terminated string)
/// - `out_value`: Pointer to write the value data
/// - `out_value_len`: Pointer to write the value length
///
/// # Returns
/// 0 on success (value found), 1 if key not found, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_get(
    context_handle: *mut c_void,
    key: *const c_char,
    out_value: *mut *mut c_uchar,
    out_value_len: *mut c_longlong,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null()
            || key.is_null()
            || out_value.is_null()
            || out_value_len.is_null()
        {
            return Ok(-1);
        }

        let context = unsafe { get_context(context_handle) };
        if context.is_none() {
            return Ok(-1);
        }

        let key_str = unsafe {
            match CStr::from_ptr(key).to_str() {
                Ok(s) => s,
                Err(_) => return Ok(-1),
            }
        };

        match context.unwrap().get(key_str) {
            Ok(Some(value)) => {
                let len = value.len();
                let ptr = Box::into_raw(value.into_boxed_slice()) as *mut c_uchar;
                unsafe {
                    *out_value = ptr;
                    *out_value_len = len as c_longlong;
                }
                Ok(0)
            }
            Ok(None) => {
                unsafe {
                    *out_value = ptr::null_mut();
                    *out_value_len = 0;
                }
                Ok(1) // Not found
            }
            Err(_) => Ok(-1),
        }
    })
}

/// Sets a value for a key
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI keyvalue context
/// - `key`: The key to set (null-terminated string)
/// - `value`: Pointer to the value data
/// - `value_len`: Length of the value data
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_set(
    context_handle: *mut c_void,
    key: *const c_char,
    value: *const c_uchar,
    value_len: c_longlong,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || key.is_null() {
            return Ok(-1);
        }
        if value.is_null() && value_len > 0 {
            return Ok(-1);
        }

        let context = unsafe { get_context(context_handle) };
        if context.is_none() {
            return Ok(-1);
        }

        let key_str = unsafe {
            match CStr::from_ptr(key).to_str() {
                Ok(s) => s,
                Err(_) => return Ok(-1),
            }
        };

        let value_vec = if value_len > 0 && !value.is_null() {
            unsafe { slice::from_raw_parts(value, value_len as usize).to_vec() }
        } else {
            Vec::new()
        };

        match context.unwrap().set(key_str, value_vec) {
            Ok(()) => Ok(0),
            Err(_) => Ok(-1),
        }
    })
}

/// Deletes a key
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI keyvalue context
/// - `key`: The key to delete (null-terminated string)
///
/// # Returns
/// 1 if deleted, 0 if key didn't exist, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_delete(
    context_handle: *mut c_void,
    key: *const c_char,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || key.is_null() {
            return Ok(-1);
        }

        let context = unsafe { get_context(context_handle) };
        if context.is_none() {
            return Ok(-1);
        }

        let key_str = unsafe {
            match CStr::from_ptr(key).to_str() {
                Ok(s) => s,
                Err(_) => return Ok(-1),
            }
        };

        match context.unwrap().delete(key_str) {
            Ok(true) => Ok(1),
            Ok(false) => Ok(0),
            Err(_) => Ok(-1),
        }
    })
}

/// Checks if a key exists
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI keyvalue context
/// - `key`: The key to check (null-terminated string)
///
/// # Returns
/// 1 if exists, 0 if not exists, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_exists(
    context_handle: *mut c_void,
    key: *const c_char,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || key.is_null() {
            return Ok(-1);
        }

        let context = unsafe { get_context(context_handle) };
        if context.is_none() {
            return Ok(-1);
        }

        let key_str = unsafe {
            match CStr::from_ptr(key).to_str() {
                Ok(s) => s,
                Err(_) => return Ok(-1),
            }
        };

        match context.unwrap().exists(key_str) {
            Ok(true) => Ok(1),
            Ok(false) => Ok(0),
            Err(_) => Ok(-1),
        }
    })
}

// =============================================================================
// Atomic Operations
// =============================================================================

/// Atomically increments a numeric value
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI keyvalue context
/// - `key`: The key to increment (null-terminated string)
/// - `delta`: The amount to increment by
/// - `out_value`: Pointer to write the new value
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_increment(
    context_handle: *mut c_void,
    key: *const c_char,
    delta: c_longlong,
    out_value: *mut c_longlong,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || key.is_null() || out_value.is_null() {
            return Ok(-1);
        }

        let context = unsafe { get_context(context_handle) };
        if context.is_none() {
            return Ok(-1);
        }

        let key_str = unsafe {
            match CStr::from_ptr(key).to_str() {
                Ok(s) => s,
                Err(_) => return Ok(-1),
            }
        };

        match context.unwrap().increment(key_str, delta) {
            Ok(new_value) => {
                unsafe {
                    *out_value = new_value;
                }
                Ok(0)
            }
            Err(_) => Ok(-1),
        }
    })
}

// =============================================================================
// Store Information
// =============================================================================

/// Gets the number of entries in the store
///
/// # Returns
/// The number of entries, or -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_size(context_handle: *mut c_void) -> c_longlong {
    ffi_boundary_result!(-1i64, {
        let context = unsafe { get_context(context_handle) };
        match context {
            Some(ctx) => match ctx.size() {
                Ok(size) => Ok(size as c_longlong),
                Err(_) => Ok(-1),
            },
            None => Ok(-1),
        }
    })
}

/// Clears all entries from the store
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_clear(context_handle: *mut c_void) -> c_int {
    ffi_boundary_i32!({
        let context = unsafe { get_context(context_handle) };
        match context {
            Some(ctx) => match ctx.clear() {
                Ok(()) => Ok(0),
                Err(_) => Ok(-1),
            },
            None => Ok(-1),
        }
    })
}

// =============================================================================
// Key Listing
// =============================================================================

/// Gets all keys as a JSON array string
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI keyvalue context
/// - `out_json`: Pointer to write the JSON string
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_keys(
    context_handle: *mut c_void,
    out_json: *mut *mut c_char,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || out_json.is_null() {
            return Ok(-1);
        }

        let context = unsafe { get_context(context_handle) };
        if context.is_none() {
            return Ok(-1);
        }

        match context.unwrap().keys() {
            Ok(keys) => {
                let json = serde_json::to_string(&keys).unwrap_or_else(|_| "[]".to_string());
                match CString::new(json) {
                    Ok(cstr) => {
                        unsafe {
                            *out_json = cstr.into_raw();
                        }
                        Ok(0)
                    }
                    Err(_) => Ok(-1),
                }
            }
            Err(_) => Ok(-1),
        }
    })
}

// =============================================================================
// Memory Management Functions
// =============================================================================

/// Frees memory allocated by keyvalue functions
///
/// # Safety
/// The pointer must have been allocated by a keyvalue function
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_free_bytes(ptr: *mut c_uchar, len: c_longlong) {
    ffi_boundary_void!({
        if !ptr.is_null() && len > 0 {
            unsafe {
                let _ = Vec::from_raw_parts(ptr, len as usize, len as usize);
            }
        }
    })
}

/// Frees a C string allocated by keyvalue functions
///
/// # Safety
/// The pointer must have been allocated by a keyvalue function
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_free_string(ptr: *mut c_char) {
    ffi_boundary_void!({
        if !ptr.is_null() {
            unsafe {
                let _ = CString::from_raw(ptr);
            }
        }
    })
}

// =============================================================================
// Availability Check
// =============================================================================

/// Checks if WASI keyvalue support is available
///
/// # Returns
/// 1 if available, 0 if not available
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_keyvalue_is_available() -> c_int {
    ffi_boundary_i32!({ Ok(1) })
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::ffi::CString;

    #[test]
    fn test_context_lifecycle() {
        let ctx = wasmtime4j_panama_wasi_keyvalue_context_create();
        assert!(!ctx.is_null());

        let id = wasmtime4j_panama_wasi_keyvalue_context_id(ctx);
        assert!(id > 0);

        let valid = wasmtime4j_panama_wasi_keyvalue_context_is_valid(ctx);
        assert_eq!(valid, 1);

        unsafe {
            wasmtime4j_panama_wasi_keyvalue_context_destroy(ctx);
        }
    }

    #[test]
    fn test_get_set_delete() {
        let ctx = wasmtime4j_panama_wasi_keyvalue_context_create();
        assert!(!ctx.is_null());

        let key = CString::new("test_key").unwrap();
        let value = b"test_value";

        // Set
        let result = wasmtime4j_panama_wasi_keyvalue_set(
            ctx,
            key.as_ptr(),
            value.as_ptr(),
            value.len() as c_longlong,
        );
        assert_eq!(result, 0);

        // Exists
        let exists = wasmtime4j_panama_wasi_keyvalue_exists(ctx, key.as_ptr());
        assert_eq!(exists, 1);

        // Get
        let mut out_value: *mut c_uchar = ptr::null_mut();
        let mut out_len: c_longlong = 0;
        let result =
            wasmtime4j_panama_wasi_keyvalue_get(ctx, key.as_ptr(), &mut out_value, &mut out_len);
        assert_eq!(result, 0);
        assert!(!out_value.is_null());
        assert_eq!(out_len, value.len() as c_longlong);

        // Verify value
        let retrieved = unsafe { slice::from_raw_parts(out_value, out_len as usize) };
        assert_eq!(retrieved, value);

        // Free the retrieved value
        unsafe {
            wasmtime4j_panama_wasi_keyvalue_free_bytes(out_value, out_len);
        }

        // Delete
        let deleted = wasmtime4j_panama_wasi_keyvalue_delete(ctx, key.as_ptr());
        assert_eq!(deleted, 1);

        // Verify deleted
        let exists = wasmtime4j_panama_wasi_keyvalue_exists(ctx, key.as_ptr());
        assert_eq!(exists, 0);

        unsafe {
            wasmtime4j_panama_wasi_keyvalue_context_destroy(ctx);
        }
    }

    #[test]
    fn test_increment() {
        let ctx = wasmtime4j_panama_wasi_keyvalue_context_create();
        assert!(!ctx.is_null());

        let key = CString::new("counter").unwrap();
        let mut out_value: c_longlong = 0;

        // First increment (from 0)
        let result =
            wasmtime4j_panama_wasi_keyvalue_increment(ctx, key.as_ptr(), 5, &mut out_value);
        assert_eq!(result, 0);
        assert_eq!(out_value, 5);

        // Second increment
        let result =
            wasmtime4j_panama_wasi_keyvalue_increment(ctx, key.as_ptr(), 3, &mut out_value);
        assert_eq!(result, 0);
        assert_eq!(out_value, 8);

        unsafe {
            wasmtime4j_panama_wasi_keyvalue_context_destroy(ctx);
        }
    }

    #[test]
    fn test_size_and_clear() {
        let ctx = wasmtime4j_panama_wasi_keyvalue_context_create();
        assert!(!ctx.is_null());

        // Initially empty
        let size = wasmtime4j_panama_wasi_keyvalue_size(ctx);
        assert_eq!(size, 0);

        // Add entries
        let key1 = CString::new("key1").unwrap();
        let key2 = CString::new("key2").unwrap();
        let value = b"value";

        wasmtime4j_panama_wasi_keyvalue_set(
            ctx,
            key1.as_ptr(),
            value.as_ptr(),
            value.len() as c_longlong,
        );
        wasmtime4j_panama_wasi_keyvalue_set(
            ctx,
            key2.as_ptr(),
            value.as_ptr(),
            value.len() as c_longlong,
        );

        let size = wasmtime4j_panama_wasi_keyvalue_size(ctx);
        assert_eq!(size, 2);

        // Clear
        let result = wasmtime4j_panama_wasi_keyvalue_clear(ctx);
        assert_eq!(result, 0);

        let size = wasmtime4j_panama_wasi_keyvalue_size(ctx);
        assert_eq!(size, 0);

        unsafe {
            wasmtime4j_panama_wasi_keyvalue_context_destroy(ctx);
        }
    }
}

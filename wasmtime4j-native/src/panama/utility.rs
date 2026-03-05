//! Utility and Version FFI module for Panama
//!
//! This module provides utility functions for Panama FFI, including version information.

use std::os::raw::c_char;
use std::sync::OnceLock;

/// Get the Wasmtime version string (Panama FFI).
///
/// Returns a pointer to a null-terminated C string containing the Wasmtime version
/// (e.g. "42.0.1"). The returned pointer is valid for the lifetime of the process.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_get_library_version() -> *const c_char {
    static VERSION: OnceLock<std::ffi::CString> = OnceLock::new();
    VERSION
        .get_or_init(|| {
            std::ffi::CString::new(crate::WASMTIME_VERSION)
                .expect("WASMTIME_VERSION contains no null bytes")
        })
        .as_ptr()
}

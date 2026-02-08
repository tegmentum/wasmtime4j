//! Utility and Version FFI module for Panama
//!
//! This module provides utility functions for Panama FFI, including version information.

use std::os::raw::c_char;

/// Get the library version string (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_get_library_version() -> *const c_char {
    static VERSION: &[u8] = b"1.0.0\0";
    VERSION.as_ptr() as *const c_char
}

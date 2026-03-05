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

#[cfg(test)]
mod tests {
    use super::*;
    use std::ffi::CStr;

    #[test]
    fn version_returns_non_null_pointer() {
        let ptr = wasmtime4j_panama_get_library_version();
        assert!(
            !ptr.is_null(),
            "Version pointer should not be null"
        );
    }

    #[test]
    fn version_matches_wasmtime_version_constant() {
        let ptr = wasmtime4j_panama_get_library_version();
        let c_str = unsafe { CStr::from_ptr(ptr) };
        let version = c_str.to_str().expect("Version should be valid UTF-8");

        assert_eq!(
            version,
            crate::WASMTIME_VERSION,
            "Panama version should match crate::WASMTIME_VERSION"
        );
    }

    #[test]
    fn version_is_valid_semver_format() {
        let ptr = wasmtime4j_panama_get_library_version();
        let c_str = unsafe { CStr::from_ptr(ptr) };
        let version = c_str.to_str().expect("Version should be valid UTF-8");

        assert!(
            !version.is_empty(),
            "Version should not be empty"
        );
        let parts: Vec<&str> = version.split('.').collect();
        assert_eq!(
            parts.len(),
            3,
            "Version '{}' should have 3 dot-separated parts (semver)",
            version
        );
        for (i, part) in parts.iter().enumerate() {
            assert!(
                part.parse::<u32>().is_ok(),
                "Version part {} ('{}') should be a valid number",
                i,
                part
            );
        }
    }

    #[test]
    fn version_is_stable_across_calls() {
        let ptr1 = wasmtime4j_panama_get_library_version();
        let ptr2 = wasmtime4j_panama_get_library_version();
        assert_eq!(
            ptr1, ptr2,
            "Repeated calls should return the same pointer (OnceLock)"
        );
    }

    #[test]
    fn version_does_not_return_hardcoded_1_0_0() {
        let ptr = wasmtime4j_panama_get_library_version();
        let c_str = unsafe { CStr::from_ptr(ptr) };
        let version = c_str.to_str().expect("Version should be valid UTF-8");

        assert_ne!(
            version, "1.0.0",
            "Version should not be the old hardcoded '1.0.0' — should be the actual Wasmtime version"
        );
    }
}

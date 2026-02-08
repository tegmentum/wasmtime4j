//! Panama FFI bindings for experimental WebAssembly features
//!
//! This module provides C-compatible functions for enabling and configuring
//! experimental WebAssembly features such as stack switching, call/cc,
//! extended constant expressions, and other proposals.

use std::os::raw::{c_int, c_uint, c_ulong, c_void};
use crate::experimental_features::core;
use crate::error::ffi_utils;

/// Create experimental features configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_create_config() -> *mut c_void {
    ffi_utils::ffi_try_ptr(|| core::create_experimental_features_config())
}

/// Create experimental features configuration with all features enabled (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_create_all_config() -> *mut c_void {
    ffi_utils::ffi_try_ptr(|| core::create_all_experimental_config())
}

/// Enable stack switching in experimental configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_enable_stack_switching(
    config_ptr: *mut c_void,
    stack_size: c_ulong,
    max_stacks: c_uint,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe { core::enable_stack_switching(config_ptr, stack_size as u64, max_stacks as u32) }
    })
}

/// Enable call/cc in experimental configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_enable_call_cc(
    config_ptr: *mut c_void,
    max_continuations: c_uint,
    storage_strategy: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            core::enable_call_cc(
                config_ptr,
                max_continuations as u32,
                storage_strategy
            )
        }
    })
}

/// Enable extended constant expressions in experimental configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_enable_extended_const_expressions(
    config_ptr: *mut c_void,
    import_based: c_int,
    global_deps: c_int,
    folding_level: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            core::enable_extended_const_expressions(
                config_ptr,
                import_based,
                global_deps,
                folding_level
            )
        }
    })
}

/// Apply experimental features to Wasmtime config (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_apply_features(
    experimental_config_ptr: *const c_void,
    wasmtime_config_ptr: *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            // Get mutable reference to Wasmtime Config
            let wasmtime_config = &mut *(wasmtime_config_ptr as *mut wasmtime::Config);
            core::apply_experimental_features(
                experimental_config_ptr,
                wasmtime_config
            )
        }
    })
}

/// Enable flexible vectors in experimental configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_enable_flexible_vectors(
    config_ptr: *mut c_void,
    dynamic_sizing: c_int,
    auto_vectorization: c_int,
    simd_integration: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            core::enable_flexible_vectors(
                config_ptr,
                dynamic_sizing,
                auto_vectorization,
                simd_integration
            )
        }
    })
}

/// Enable string imports in experimental configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_enable_string_imports(
    config_ptr: *mut c_void,
    encoding_format: c_int,
    string_interning: c_int,
    lazy_decoding: c_int,
    js_interop: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            core::enable_string_imports(
                config_ptr,
                encoding_format,
                string_interning,
                lazy_decoding,
                js_interop
            )
        }
    })
}

/// Enable resource types in experimental configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_enable_resource_types(
    config_ptr: *mut c_void,
    automatic_cleanup: c_int,
    reference_counting: c_int,
    cleanup_strategy: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            core::enable_resource_types(
                config_ptr,
                automatic_cleanup,
                reference_counting,
                cleanup_strategy
            )
        }
    })
}

/// Enable type imports in experimental configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_enable_type_imports(
    config_ptr: *mut c_void,
    validation_strategy: c_int,
    resolution_mechanism: c_int,
    structural_compatibility: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            core::enable_type_imports(
                config_ptr,
                validation_strategy,
                resolution_mechanism,
                structural_compatibility
            )
        }
    })
}

/// Enable shared-everything threads in experimental configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_enable_shared_everything_threads(
    config_ptr: *mut c_void,
    min_threads: c_uint,
    max_threads: c_uint,
    global_state_sharing: c_int,
    atomic_operations: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            core::enable_shared_everything_threads(
                config_ptr,
                min_threads as u32,
                max_threads as u32,
                global_state_sharing,
                atomic_operations
            )
        }
    })
}

/// Enable custom page sizes in experimental configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_enable_custom_page_sizes(
    config_ptr: *mut c_void,
    page_size: c_uint,
    strategy: c_int,
    strict_alignment: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        unsafe {
            core::enable_custom_page_sizes(
                config_ptr,
                page_size as u32,
                strategy,
                strict_alignment
            )
        }
    })
}

/// Get feature detection capabilities (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_get_feature_support(feature_id: c_int) -> c_int {
    // Feature detection based on Wasmtime capabilities
    match feature_id {
        0 => 1,  // Stack switching (experimental support)
        1 => 0,  // Call/CC (not yet supported)
        2 => 1,  // Extended const expressions (partial support)
        3 => 1,  // Memory64 extended (partial support)
        4 => 0,  // Custom page sizes (not yet supported)
        5 => 0,  // Shared-everything threads (not yet supported)
        6 => 0,  // Type imports (not yet supported)
        7 => 0,  // String imports (not yet supported)
        8 => 0,  // Resource types (not yet supported)
        9 => 0,  // Interface types (not yet supported)
        10 => 0, // Flexible vectors (not yet supported)
        _ => 0
    }
}

/// Destroy experimental features configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_experimental_destroy_config(config_ptr: *mut c_void) {
    if !config_ptr.is_null() {
        unsafe { core::destroy_experimental_features_config(config_ptr) }
    }
}

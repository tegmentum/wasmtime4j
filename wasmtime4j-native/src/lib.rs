//! # Wasmtime4j Native Library
//!
//! This crate provides unified native bindings for the Wasmtime WebAssembly runtime,
//! supporting both JNI (Java Native Interface) for Java 8-22 and Panama Foreign
//! Function Interface for Java 23+.
//!
//! The library is designed to be consumed by both wasmtime4j-jni and wasmtime4j-panama
//! modules, providing a single source of truth for native Wasmtime functionality.
//!
//! ## Architecture
//!
//! - **Core Modules**: Engine, Module, Store, Instance - Core WebAssembly runtime functionality
//! - **Interface Modules**: JNI and Panama FFI bindings for Java integration  
//! - **Error System**: Comprehensive error handling with defensive programming patterns
//! - **WASI Support**: WebAssembly System Interface for file I/O and system operations
//!
//! ## Safety and Defensive Programming
//!
//! This library implements defensive programming patterns throughout to prevent JVM crashes
//! and ensure robust operation in production environments. All public APIs validate inputs
//! and handle errors gracefully.

#![warn(missing_docs)]
#![warn(clippy::all)]

use std::os::raw::c_char;

// Core modules - foundational WebAssembly runtime components
pub mod error;
pub mod engine;
pub mod module;
pub mod store;
pub mod instance;

// Interface modules - will be implemented in later streams
pub mod jni_bindings;
pub mod panama_ffi;

// Advanced modules - will be implemented in later tasks
pub mod wasi;

// Component model support for WASI Preview 2
pub mod component;

// Re-export core types for convenience
pub use engine::{Engine, EngineBuilder, WasmFeature};
pub use error::{WasmtimeError, WasmtimeResult, ErrorCode};
pub use module::{Module, ModuleMetadata, ValueType, ImportKind, ExportKind, FunctionSignature};
pub use store::{Store, StoreBuilder, StoreData, StoreMetadata, ResourceLimits, ExecutionState, MemoryUsage};
pub use instance::{Instance, InstanceMetadata, ImportBinding, ExportBinding, WasmValue, ExecutionResult};

// Re-export component model types for WASI Preview 2 support
pub use component::{
    ComponentEngine, Component, ComponentMetadata, ComponentStoreData,
    InterfaceDefinition, FunctionDefinition, Parameter, TypeDefinition, ResourceDefinition,
    ValueType as ComponentValueType, TypeKind, FieldType, CaseType,
    ResourceManager, HostInterface, InstanceInfo
};

/// Library version information
pub const VERSION: &str = env!("CARGO_PKG_VERSION");

/// Wasmtime version this library is built against
pub const WASMTIME_VERSION: &str = "36.0.2";

/// Initialize the native library
///
/// This function should be called once before using any other functionality.
/// It sets up logging and performs any necessary one-time initialization.
///
/// # Safety
///
/// This function is safe to call multiple times, but should be called before
/// any other library functions are used.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_init() -> i32 {
    env_logger::try_init().unwrap_or(());
    log::info!("Wasmtime4j native library initialized (version {})", VERSION);
    0 // Success
}

/// Get the library version as a C string
///
/// # Safety
///
/// The returned pointer is valid for the lifetime of the program and should not be freed.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_version() -> *const c_char {
    VERSION.as_ptr() as *const c_char
}

/// Get the Wasmtime version as a C string
///
/// # Safety
///
/// The returned pointer is valid for the lifetime of the program and should not be freed.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasmtime_version() -> *const c_char {
    WASMTIME_VERSION.as_ptr() as *const c_char
}

/// Cleanup and shutdown the native library
///
/// This function performs any necessary cleanup before the library is unloaded.
///
/// # Safety
///
/// This function should only be called once, typically during library unload.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_shutdown() -> i32 {
    log::info!("Wasmtime4j native library shutting down");
    0 // Success
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_init() {
        unsafe {
            assert_eq!(wasmtime4j_init(), 0);
        }
    }

    #[test]
    fn test_version() {
        unsafe {
            let version_ptr = wasmtime4j_version();
            assert!(!version_ptr.is_null());
        }
    }

    #[test]
    fn test_wasmtime_version() {
        unsafe {
            let version_ptr = wasmtime4j_wasmtime_version();
            assert!(!version_ptr.is_null());
        }
    }

    #[test]
    fn test_shutdown() {
        unsafe {
            assert_eq!(wasmtime4j_shutdown(), 0);
        }
    }
}
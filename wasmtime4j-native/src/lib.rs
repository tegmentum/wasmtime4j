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

// =============================================================================
// Global Warning Suppressions (Phase 5 Audit Complete)
// =============================================================================
// Remaining suppressions have documented justifications:

#![allow(missing_docs)]
// Large codebase - docs added incrementally
// Note: dead_code warnings addressed file-by-file; removed blanket suppression
#![allow(unused_mut)] // Required: JNI env parameters need mut for API calls
#![allow(non_snake_case)] // Required: JNI functions follow Java naming conventions
#![allow(private_interfaces)]
// Required: FFI functions expose private types in signatures

// Phase 5 REMOVED (issues fixed):
// - unused_unsafe: Removed 3 unnecessary unsafe blocks
// - unused_assignments: Fixed with targeted allow
// - unused_doc_comments: Converted to regular comments
// - private_bounds: No issues found
// - elided_lifetimes_in_paths: No issues found
#![warn(clippy::all)]

use std::os::raw::c_char;

// Core modules - foundational WebAssembly runtime components
pub mod caller;
pub mod code_builder; // CodeBuilder stateful compilation API
pub mod data_segment; // Data segment management for memory.init()
pub mod element_segment; // Element segment management for table.init()
pub mod element_segment_parser; // Element and data segment parser using wasmparser
pub mod engine;
pub mod error;
pub mod global;
pub mod hostfunc;
pub mod instance;
pub mod interop; // FFI interop utilities
pub mod linker;
pub mod memory;
pub mod module;
pub mod store;
pub mod table;
pub mod typed_func; // Typed function wrapper for zero-cost calls
pub mod wast_runner; // WAST test execution

// Coredump registry for trap diagnostics
pub mod coredump;

// Additional core functionality for comprehensive API coverage

// Shared FFI architecture with trait-based conversions
pub mod shared_ffi;

// Common FFI utilities shared between JNI and Panama implementations
pub mod ffi_common;

// Test modules for runtime completion validation
#[cfg(test)]
pub mod test_runtime_completion;

// Interface modules
#[cfg(feature = "jni-bindings")]
pub mod jni;
#[cfg(feature = "jni-bindings")]
pub mod jni_gc_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_pooling_allocator_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_typed_func_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi_cli_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi_io_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wast_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wit_value_bindings;
#[cfg(feature = "panama-ffi")]
pub mod panama;
#[cfg(feature = "panama-ffi")]
pub mod panama_exception_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_gc_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_wasi_cli_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_wasi_io_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_wast_ffi;

// Advanced modules - will be implemented in later tasks
#[cfg(feature = "wasi")]
pub mod wasi;

// WASI HTTP support
#[cfg(feature = "wasi-http")]
pub mod wasi_http;

// WASI-NN host-side inference support
pub mod wasi_nn;

// Async runtime for async WebAssembly operations
pub mod async_runtime;

// Shared WASI configuration helpers (used by component::linker and wasi_preview2)
pub mod wasi_common_config;

// Enhanced WASI Preview 2 implementation
pub mod wasi_preview2;

// Unified WASI stream operations trait (consolidates duplicated stream code)
pub mod wasi_stream_ops;

// WIT value marshalling for Component Model
pub mod wit_value_marshal;

// Component model support for WASI Preview 2
#[cfg(feature = "component-model")]
pub mod component;
#[cfg(feature = "component-model")]
pub mod component_core;

// Guest profiler for sampling-based performance profiling
#[cfg(feature = "profiling")]
pub mod guest_profiler;

// WebAssembly GC implementation
pub mod gc;
pub mod gc_heap;
pub mod gc_operations;
pub mod gc_types;

// Re-export core types for convenience
pub use engine::{Engine, EngineBuilder, WasmFeature};
pub use error::{ErrorCode, WasmtimeError, WasmtimeResult};
pub use global::{Global, GlobalMetadata, GlobalValue};
pub use hostfunc::{HostFunction, HostFunctionBuilder, HostFunctionCallback, MarshallingResult};
pub use instance::{
    ExecutionResult, ExportBinding, ImportBinding, Instance, InstanceMetadata, WasmValue,
};
pub use linker::{
    HostFunctionDefinition, ImportDefinition, ImportType, Linker, LinkerConfig,
    LinkerInstantiationResult, LinkerMetadata,
};
pub use memory::{
    Memory, MemoryBuilder, MemoryConfig, MemoryDataType, MemoryError, MemoryMetadata,
    MemoryRegistry, MemoryUsage as MemUsage,
};
pub use module::{
    ExportKind, FunctionSignature, ImportKind, Module, ModuleMetadata, ModuleValueType,
};
pub use store::{
    ExecutionState, MemoryUsage, ResourceLimits, Store, StoreBuilder, StoreData, StoreMetadata,
};
pub use table::{Table, TableElement, TableMetadata};

// Optional re-exports based on features
#[cfg(feature = "wasi")]
pub use wasi::{
    DirectoryMapping, EnvironmentPolicy, StdioConfig, StdioSink, StdioSource,
    WasiCapabilitiesSummary, WasiConfig, WasiContext, WasiDirPermissions, WasiDirectoryDescriptor,
    WasiDirectoryEntry, WasiExecutionResult, WasiFileDescriptor, WasiFileDescriptorManager,
    WasiFilePermissions, WasiFilestat,
};

// Re-export async runtime functionality
pub use async_runtime::{get_async_runtime, get_runtime_handle, AsyncCallback};

// Re-export WASI Preview 2 functionality
pub use wasi_preview2::{
    AsyncWasiOperation, AsyncWasiOperationType, WasiPreview2Config, WasiPreview2Context,
    WasiStream, WasiStreamType,
};

// Component model re-exports
#[cfg(feature = "component-model")]
pub use component::{
    CaseType, Component, ComponentEngine, ComponentHostCallback, ComponentHostFunctionEntry,
    ComponentInstanceMetadata, ComponentInstanceState, ComponentInstanceWrapper, ComponentLinker,
    ComponentMetadata, ComponentStoreData, ComponentTypeKind, ComponentValue, ComponentValueType,
    FieldType, FunctionDefinition, HostInterface, InstanceInfo, InterfaceDefinition, Parameter,
    ResourceDefinition, ResourceManager, TypeDefinition, WasiP2Config,
};

#[cfg(feature = "component-model")]
pub use component_core::{ComponentInstanceHandle, ComponentMetrics, EnhancedComponentEngine};

// Re-export WebAssembly GC types for garbage collection support
pub use gc::{
    ArrayOperationResult, RefOperationResult, StructOperationResult, WasmGcRuntime, WasmtimeGcRef,
};

pub use gc_heap::{
    CollectionTrigger, GcCollectionResult, GcHeap, GcHeapConfig, GcHeapStats, GcWeakReference,
    ObjectId,
};
pub use gc_types::{
    ArrayTypeDefinition, FieldDefinition, FieldType as GcFieldType, GcReferenceType,
    GcTypeRegistry, GcValue, StructTypeDefinition,
};

// Re-export shared FFI utilities for interface implementations
pub use shared_ffi::{
    convert_wasm_features, validate_wasm_features, validation, BooleanReturnConverter,
    FfiWasmFeature, IntegerReturnConverter, PointerReturnConverter, ReturnValueConverter,
    FFI_ERROR, FFI_SUCCESS,
};

/// Library version information
pub const VERSION: &str = env!("CARGO_PKG_VERSION");

/// Wasmtime version this library is built against
pub const WASMTIME_VERSION: &str = "44.0.0";

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
    log::info!(
        "Wasmtime4j native library initialized (version {})",
        VERSION
    );
    0 // Success
}

/// Get the library version as a C string
///
/// # Safety
///
/// The returned pointer is valid for the lifetime of the program and should not be freed.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_version() -> *const c_char {
    concat!(env!("CARGO_PKG_VERSION"), "\0").as_ptr() as *const c_char
}

/// Get the Wasmtime version as a C string
///
/// # Safety
///
/// The returned pointer is valid for the lifetime of the program and should not be freed.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasmtime_version() -> *const c_char {
    concat!("42.0.1", "\0").as_ptr() as *const c_char
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

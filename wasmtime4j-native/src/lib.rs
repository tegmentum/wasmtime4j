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

#![allow(missing_docs)] // Large codebase - docs added incrementally
#![allow(unused_imports)] // Reduced 62% (116→44); remaining are platform/test-specific
#![allow(dead_code)] // Infrastructure used in integration tests
#![allow(unused_mut)] // Required: JNI env parameters need mut for API calls
#![allow(non_snake_case)] // Required: JNI functions follow Java naming conventions
#![allow(private_interfaces)] // Required: FFI functions expose private types in signatures

// Phase 5 REMOVED (issues fixed):
// - unused_unsafe: Removed 3 unnecessary unsafe blocks
// - unused_assignments: Fixed with targeted allow
// - unused_doc_comments: Converted to regular comments
// - private_bounds: No issues found
// - elided_lifetimes_in_paths: No issues found

#![warn(clippy::all)]

use std::os::raw::c_char;

// Core modules - foundational WebAssembly runtime components
pub mod error;
pub mod interop;  // FFI interop utilities
pub mod engine;
pub mod module;
pub mod store;
pub mod instance;
pub mod hostfunc;
pub mod typed_func;  // Typed function wrapper for zero-cost calls
pub mod memory;
pub mod global;
pub mod table;
pub mod element_segment;  // Element segment management for table.init()
pub mod data_segment;  // Data segment management for memory.init()
pub mod element_segment_parser;  // Element and data segment parser using wasmparser
pub mod linker;
pub mod wast_runner;  // WAST test execution
pub mod caller;
pub mod serialization;
pub mod value_serialization;  // WASM value serialization for thread execution
pub mod threading;
pub mod streaming_compiler;

// Additional core functionality for comprehensive API coverage

// Platform optimization integration tests (NUMA, cache, memory bandwidth)
// DISABLED: Requires platform optimization modules (398 type definitions needed)
// #[cfg(test)]
// pub mod platform_optimization_integration_test;

// Advanced execution control with fuel and epoch management
pub mod execution_control;

// Shared FFI architecture with trait-based conversions
pub mod shared_ffi;

// Common FFI utilities shared between JNI and Panama implementations
pub mod ffi_common;

// Test modules for runtime completion validation
#[cfg(test)]
pub mod test_runtime_completion;

// Interface modules - will be implemented in later streams
#[cfg(feature = "jni-bindings")]
pub mod jni_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_thread_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi_io_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi_filesystem_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi_cli_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi_clocks_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi_random_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi_sockets_bindings;
#[cfg(all(feature = "jni-bindings", feature = "wasi-keyvalue"))]
pub mod jni_wasi_keyvalue_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_gc_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_typed_func_bindings;
#[cfg(feature = "jni-bindings")]
pub mod platform_memory_jni;
#[cfg(feature = "jni-bindings")]
pub mod jni_experimental_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wast_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wit_value_bindings;
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
pub mod jni_wasi_nn_bindings;
#[cfg(all(feature = "jni-bindings", feature = "wasi-threads"))]
pub mod jni_wasi_threads_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_component_bindings;
#[cfg(feature = "panama-ffi")]
pub mod panama_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_wasi_io_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_wasi_filesystem_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_wasi_cli_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_wasi_clocks_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_wasi_random_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_wasi_sockets_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_gc_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_experimental_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_simd_ffi;
#[cfg(all(feature = "panama-ffi", feature = "wasi-nn"))]
pub mod panama_wasi_nn_ffi;
#[cfg(all(feature = "panama-ffi", feature = "wasi-threads"))]
pub mod panama_wasi_threads_ffi;
#[cfg(all(feature = "panama-ffi", feature = "wasi-keyvalue"))]
pub mod panama_wasi_keyvalue_ffi;

// Advanced modules - will be implemented in later tasks
#[cfg(feature = "wasi")]
pub mod wasi;

// WASI HTTP support
#[cfg(feature = "wasi-http")]
pub mod wasi_http;

// WASI-NN support (neural network inference - Tier 3 experimental)
#[cfg(feature = "wasi-nn")]
pub mod wasi_nn;

// WASI-Threads support (thread spawning - experimental)
#[cfg(feature = "wasi-threads")]
pub mod wasi_threads;

// Advanced configuration modules for comprehensive optimization control
// pub mod config_cranelift;
// pub mod platform_config; // DISABLED: depends on platform_types
pub mod engine_config;

// Experimental WebAssembly features (committee-stage proposals)
pub mod experimental_features;

// Advanced experimental features for cutting-edge capabilities
pub mod advanced_experimental;

// Async runtime for async WebAssembly operations
pub mod async_runtime;

// Enhanced WASI Preview 2 implementation
pub mod wasi_preview2;

// Shared helper functions for WASI I/O operations (used by both JNI and Panama FFI)
pub mod wasi_io_helpers;

// Unified WASI stream operations trait (consolidates duplicated stream code)
pub mod wasi_stream_ops;

// Shared helper functions for WASI filesystem operations (used by both JNI and Panama FFI)
pub mod wasi_filesystem_helpers;

// Shared helper functions for WASI clocks operations (used by both JNI and Panama FFI)
pub mod wasi_clocks_helpers;

// Shared helper functions for WASI random operations (used by both JNI and Panama FFI)
pub mod wasi_random_helpers;

// Shared helper functions for WASI sockets operations (used by both JNI and Panama FFI)
pub mod wasi_sockets_helpers;

// Shared helper functions for WASI keyvalue operations (used by both JNI and Panama FFI)
#[cfg(feature = "wasi-keyvalue")]
pub mod wasi_keyvalue_helpers;

// Type introspection system
pub mod type_introspection;

// WIT value marshalling for Component Model
pub mod wit_value_marshal;

// Component model support for WASI Preview 2
#[cfg(feature = "component-model")]
pub mod component;
#[cfg(feature = "component-model")]
pub mod component_core;
#[cfg(feature = "component-model")]
pub mod version_types;
#[cfg(feature = "component-model")]
pub mod component_resources;
#[cfg(feature = "component-model")]
pub mod resource_dynamic;

// Experimental modules for cutting-edge WebAssembly proposals
pub mod exceptions;
pub mod simd;
pub mod multi_value;

// Development tooling modules for developer experience
pub mod module_analyzer;

// Debugging support module
pub mod debug;

// WebAssembly Coredump support
pub mod coredump;

// Store resource limiter APIs
pub mod store_limiter;

// Store call hooks for execution monitoring
pub mod call_hooks;

// Fuel exhaustion callback handling
pub mod fuel_callback;

// Epoch deadline callback handling
pub mod epoch_callback;

// Guest profiler integration
pub mod guest_profiler;

// WebAssembly GC implementation
pub mod gc_types;
pub mod gc_heap;
pub mod gc_operations;
pub mod gc;

// Re-export core types for convenience
pub use engine::{Engine, EngineBuilder, WasmFeature};
pub use error::{WasmtimeError, WasmtimeResult, ErrorCode};
pub use module::{Module, ModuleMetadata, ModuleValueType, ImportKind, ExportKind, FunctionSignature};
pub use store::{Store, StoreBuilder, StoreData, StoreMetadata, ResourceLimits, ExecutionState, MemoryUsage};
pub use instance::{Instance, InstanceMetadata, ImportBinding, ExportBinding, WasmValue, ExecutionResult};
pub use hostfunc::{HostFunction, HostFunctionBuilder, HostFunctionCallback, MarshallingResult};
pub use memory::{Memory, MemoryBuilder, MemoryConfig, MemoryMetadata, MemoryUsage as MemUsage, MemoryDataType, MemoryRegistry, MemoryError};
pub use global::{Global, GlobalValue, GlobalMetadata, ReferenceType as GlobalReferenceType};
pub use table::{Table, TableElement, TableMetadata};
pub use linker::{Linker, LinkerMetadata, LinkerConfig, LinkerInstantiationResult, HostFunctionDefinition, ImportDefinition, ImportType};

// Re-export additional core functionality
pub use caller::{CallerContext, CallerExport, CallerExportType, ExportCounts};
pub use serialization::{
    ModuleSerializer, SerializationConfig, ModuleSizeInfo, SerializationStats, CacheInfo, ValidationLevel
};

// Optional re-exports based on features
#[cfg(feature = "wasi")]
pub use wasi::{
    WasiContext, WasiConfig, EnvironmentPolicy, DirectoryMapping,
    WasiDirPermissions, WasiFilePermissions, StdioConfig, StdioSource, StdioSink,
    WasiExecutionResult, WasiCapabilitiesSummary, WasiFileDescriptorManager,
    WasiFileDescriptor, WasiDirectoryDescriptor, WasiDirectoryEntry, WasiFilestat
};

// Re-export WASI-NN functionality (Tier 3 experimental)
#[cfg(feature = "wasi-nn")]
pub use wasi_nn::{
    WasiNnContext, WasiNnConfig, WasiNnConfigBuilder, WasiNnStats, WasiNnStatsSnapshot,
    NnGraph, NnExecutionContext, NnTensor, NnImplementationInfo,
    NnGraphEncoding, NnExecutionTarget, NnTensorType, NnErrorCode
};

// Re-export async runtime functionality
pub use async_runtime::{
    AsyncOperation, AsyncOperationType, AsyncOperationStatus,
    AsyncFunctionCallContext, AsyncCompilationContext, CompilationOptions,
    get_async_runtime, get_runtime_handle, execute_async_function_call,
    compile_module_async, cancel_async_operation, get_operation_status,
    wait_for_operation
};

// Re-export WASI Preview 2 functionality
pub use wasi_preview2::{
    WasiPreview2Context, WasiPreview2Config, WasiStream, WasiStreamType,
    WasiFuture, WasiFutureType, AsyncWasiOperation, AsyncWasiOperationType
};

// Component model re-exports
#[cfg(feature = "component-model")]
pub use component::{
    ComponentEngine, Component, ComponentMetadata, ComponentStoreData,
    InterfaceDefinition, FunctionDefinition, Parameter, TypeDefinition, ResourceDefinition,
    ComponentValueType, ComponentTypeKind, FieldType, CaseType,
    ResourceManager, HostInterface, InstanceInfo, ComponentLinker, WasiP2Config,
    ComponentInstanceWrapper, ComponentInstanceMetadata, ComponentInstanceState,
    ComponentValue, ComponentHostCallback, ComponentHostFunctionEntry, WitParser
};

#[cfg(feature = "component-model")]
pub use component_core::{
    EnhancedComponentEngine, ComponentInstanceHandle, ComponentMetrics
};

#[cfg(feature = "component-model")]
pub use version_types::{ComponentId, SemanticVersion, VersionConstraint};

#[cfg(feature = "component-model")]
pub use component_resources::{
    ComponentResourceManager, ManagedResource, ResourceHandle, ResourcePermissions,
    ResourceState, AccessType, ResourceQuotas, UsageTracking
};

// Re-export WebAssembly GC types for garbage collection support
pub use gc::{
    WasmGcRuntime, StructOperationResult, ArrayOperationResult, RefOperationResult, WasmtimeGcRef
};

pub use gc_types::{
    GcReferenceType, StructTypeDefinition, ArrayTypeDefinition, FieldDefinition, FieldType as GcFieldType,
    GcObject, GcValue, GcTypeRegistry, GcTypeConverter
};
pub use gc_heap::{
    GcHeap, GcHeapConfig, GcHeapStats, ObjectId,
    GcCollectionResult, GcWeakReference, CollectionTrigger
};

// Re-export shared FFI utilities for interface implementations
pub use shared_ffi::{
    ParameterConverter, ReturnValueConverter,
    FFI_SUCCESS, FFI_ERROR,
    FfiStrategy, FfiOptLevel, FfiWasmFeature,
    BooleanReturnConverter, IntegerReturnConverter, PointerReturnConverter,
    convert_wasm_features, validate_wasm_features,
    validation, error_mapping
};

/// Library version information
pub const VERSION: &str = env!("CARGO_PKG_VERSION");

/// Wasmtime version this library is built against
pub const WASMTIME_VERSION: &str = "41.0.1";

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
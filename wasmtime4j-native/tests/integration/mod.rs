//! Integration test modules for wasmtime4j-native.
//!
//! This module organizes integration tests by feature area:
//! - ffi_boundary_tests: FFI safety and boundary condition tests (Rust API level)
//! - panama_ffi_tests: Panama FFI layer tests (direct C FFI boundary)
//! - ffi_lifecycle_tests: Cross-cutting lifecycle and error handling tests
//! - memory_gc_ffi_tests: Memory and GC FFI layer tests

pub mod ffi_boundary_tests;
pub mod ffi_lifecycle_tests;
pub mod memory_gc_ffi_tests;
pub mod panama_ffi_tests;

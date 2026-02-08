//! Integration tests for wasmtime4j-native.
//!
//! This file serves as the entry point for all integration tests.
//! Test modules are organized under the `integration/` directory.

mod common;
mod integration;

// Re-export test modules so cargo test can discover them
pub use integration::ffi_boundary_tests;
pub use integration::ffi_lifecycle_tests;
pub use integration::panama_ffi_tests;

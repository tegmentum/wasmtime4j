//! Integration tests for wasmtime4j-native.
//!
//! This file serves as the entry point for all integration tests.
//! Test modules are organized under the `integration/` directory.

mod common;
mod integration;

// Re-export test modules so cargo test can discover them
pub use integration::ffi_boundary_tests;

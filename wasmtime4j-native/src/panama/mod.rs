//! Panama FFI bindings for wasmtime4j
//!
//! This module contains extracted Panama FFI modules that provide C-compatible
//! functions for use by the wasmtime4j-panama Java module.

pub mod caller;
pub mod code_builder;
pub mod component_enhanced;
pub mod coredump;
pub mod engine;
pub mod exception_handling;
pub mod function;
pub mod gc_refs;
pub mod global;
pub mod instance;
pub mod instance_pre;
pub mod linker;
pub mod memory;
pub mod module;
pub mod pooling_allocator;
pub mod store;
pub mod table;
pub mod trap;
pub mod utility;
pub mod wasi_context;
pub mod wasi_http;

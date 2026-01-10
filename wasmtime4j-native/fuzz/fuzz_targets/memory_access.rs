//! Fuzz target for WebAssembly memory access operations.
//!
//! This target tests the robustness of memory read/write operations by:
//! - Creating a minimal module with memory
//! - Attempting reads/writes at various offsets and lengths
//! - Testing boundary conditions
//!
//! Aims to discover:
//! - Buffer overflow/underflow
//! - Out of bounds access handling
//! - Memory growth edge cases
//! - Memory safety issues in memory access code

#![no_main]

use arbitrary::Arbitrary;
use libfuzzer_sys::fuzz_target;
use wasmtime::{Engine, Instance, Memory, MemoryType, Module, Store};

/// Structured input for memory access fuzzing
#[derive(Debug, Arbitrary)]
struct MemoryAccessInput {
    /// Offset into memory
    offset: u64,
    /// Length of data to read/write
    length: u32,
    /// Operation: 0 = read, 1 = write
    operation: u8,
    /// Data to write (if write operation)
    data: Vec<u8>,
    /// Initial memory size in pages (0-10 pages for reasonable bounds)
    initial_pages: u8,
}

/// Minimal WASM module with exported memory
const MINIMAL_MEMORY_MODULE: &[u8] = &[
    0x00, 0x61, 0x73, 0x6D, // magic
    0x01, 0x00, 0x00, 0x00, // version
    0x05, 0x03, 0x01, 0x00, 0x01, // memory section: 1 memory, min=0, max=1
    0x07, 0x08, 0x01, 0x04, 0x6D, 0x65, 0x6D, 0x30, 0x02, 0x00, // export section: "mem0" = memory 0
];

fuzz_target!(|input: MemoryAccessInput| {
    // Create engine and store
    let engine = Engine::default();
    let mut store = Store::new(&engine, ());

    // Approach 1: Use module with memory
    if let Ok(module) = Module::new(&engine, MINIMAL_MEMORY_MODULE) {
        if let Ok(instance) = Instance::new(&mut store, &module, &[]) {
            if let Some(memory_export) = instance.get_memory(&mut store, "mem0") {
                fuzz_memory_operations(&mut store, memory_export, &input);
            }
        }
    }

    // Approach 2: Create standalone memory
    let initial_pages = (input.initial_pages % 10) as u64;
    let memory_type = MemoryType::new(initial_pages as u32, Some((initial_pages + 1) as u32));

    if let Ok(memory) = Memory::new(&mut store, memory_type) {
        fuzz_memory_operations(&mut store, memory, &input);
    }
});

fn fuzz_memory_operations<T>(store: &mut Store<T>, memory: Memory, input: &MemoryAccessInput) {
    let offset = input.offset as usize;
    let length = input.length as usize;

    // Test read operation
    if input.operation == 0 || input.operation > 1 {
        let mut buffer = vec![0u8; length.min(4096)]; // Cap buffer size
        let _ = memory.read(&mut *store, offset, &mut buffer);
    }

    // Test write operation
    if input.operation == 1 || input.operation > 1 {
        let write_data = if input.data.len() >= length {
            &input.data[..length.min(4096)]
        } else {
            &input.data
        };
        let _ = memory.write(&mut *store, offset, write_data);
    }

    // Test memory growth
    if input.operation > 1 {
        let grow_pages = (input.data.len() % 5) as u64;
        let _ = memory.grow(&mut *store, grow_pages);
    }

    // Test memory size query
    let _ = memory.size(&*store);
    let _ = memory.data_size(&*store);
}

//! Memory types, enums, configuration structs, and error definitions
//!
//! This module contains all type definitions for the memory subsystem including:
//! - Memory variant enum (regular vs shared memory)
//! - Page size options
//! - Memory data types for typed operations
//! - Configuration structs
//! - Error types

use std::time::Instant;

use crate::error::{WasmtimeError, WasmtimeResult};
use wasmtime::{Memory as WasmtimeMemory, SharedMemory as WasmtimeSharedMemory};

/// Memory variant that can hold either a regular memory or a shared memory
///
/// This enum allows the Memory wrapper to handle both types of WebAssembly memory
/// transparently, providing a unified interface for the JNI layer.
#[derive(Debug)]
pub enum MemoryVariant {
    /// Regular WebAssembly memory (requires store context for operations)
    Regular(WasmtimeMemory),
    /// Shared WebAssembly memory (can be accessed from multiple threads)
    Shared(WasmtimeSharedMemory),
}

impl MemoryVariant {
    /// Get a raw pointer to memory data for atomic operations.
    ///
    /// For atomic operations on shared memory, we need raw pointer access.
    /// This method returns a pointer and length that can be used for atomic operations.
    ///
    /// # Safety
    /// The caller must ensure proper synchronization when using the returned pointer.
    pub fn data_ptr_for_atomics<'a, T: 'static>(
        &self,
        store: impl Into<wasmtime::StoreContextMut<'a, T>>,
    ) -> (*mut u8, usize) {
        match self {
            MemoryVariant::Regular(mem) => {
                let data = mem.data_mut(store);
                (data.as_mut_ptr(), data.len())
            }
            MemoryVariant::Shared(mem) => {
                let data = mem.data();
                (data.as_ptr() as *mut u8, data.len())
            }
        }
    }

    /// Get read-only data pointer for shared memory operations
    pub fn data_ptr_readonly<'a, T: 'static>(
        &self,
        store: impl Into<wasmtime::StoreContext<'a, T>>,
    ) -> (*const u8, usize) {
        match self {
            MemoryVariant::Regular(mem) => {
                let data = mem.data(store);
                (data.as_ptr(), data.len())
            }
            MemoryVariant::Shared(mem) => {
                let data = mem.data();
                (data.as_ptr() as *const u8, data.len())
            }
        }
    }

    /// Get data size
    pub fn data_size(&self, store: impl wasmtime::AsContext) -> usize {
        match self {
            MemoryVariant::Regular(mem) => mem.data_size(store),
            MemoryVariant::Shared(mem) => mem.data().len(),
        }
    }
}

/// Memory data types for type-safe read/write operations
#[derive(Debug, Clone, Copy)]
pub enum MemoryDataType {
    /// 8-bit unsigned integer
    U8,
    /// 8-bit signed integer
    I8,
    /// 16-bit unsigned integer (little endian)
    U16Le,
    /// 16-bit signed integer (little endian)
    I16Le,
    /// 32-bit unsigned integer (little endian)
    U32Le,
    /// 32-bit signed integer (little endian)
    I32Le,
    /// 64-bit unsigned integer (little endian)
    U64Le,
    /// 64-bit signed integer (little endian)
    I64Le,
    /// 32-bit IEEE 754 float (little endian)
    F32Le,
    /// 64-bit IEEE 754 float (little endian)
    F64Le,
}

/// Memory metadata and usage statistics
#[derive(Debug, Clone)]
pub struct MemoryMetadata {
    /// Timestamp when this memory was created
    pub created_at: Instant,
    /// Current size in pages (64KB each)
    pub current_pages: u64,
    /// Maximum allowed pages
    pub maximum_pages: Option<u64>,
    /// Total number of read operations performed
    pub read_operations: u64,
    /// Total number of write operations performed
    pub write_operations: u64,
    /// Total bytes read from memory
    pub bytes_read: u64,
    /// Total bytes written to memory
    pub bytes_written: u64,
    /// Number of growth operations performed
    pub growth_operations: u64,
    /// Peak memory usage in pages
    pub peak_pages: u64,
    /// Last access timestamp
    pub last_access: Option<Instant>,
    /// Number of bounds check violations prevented
    pub bounds_violations_prevented: u64,
}

/// Memory configuration and limits
#[derive(Debug, Clone)]
pub struct MemoryConfig {
    /// Initial size in pages
    pub initial_pages: u64,
    /// Maximum allowed pages (None for unlimited)
    pub maximum_pages: Option<u64>,
    /// Whether memory is shared between threads
    pub is_shared: bool,
    /// Whether this is a 64-bit memory (Memory64 proposal)
    pub is_64: bool,
    /// Memory index within the module (for multi-memory support)
    pub memory_index: u32,
    /// Name of this memory (for debugging)
    pub name: Option<String>,
}

/// Memory usage statistics and monitoring information
#[derive(Debug, Clone)]
pub struct MemoryUsage {
    /// Current memory size in bytes
    pub current_bytes: usize,
    /// Current memory size in pages
    pub current_pages: u64,
    /// Maximum possible memory size in bytes
    pub maximum_bytes: Option<usize>,
    /// Peak memory usage in bytes
    pub peak_bytes: usize,
    /// Total read operations
    pub read_count: u64,
    /// Total write operations
    pub write_count: u64,
    /// Total bytes transferred
    pub bytes_transferred: u64,
    /// Memory utilization percentage (0-100)
    pub utilization_percent: f64,
}

/// Memory access result with comprehensive error information
pub type MemoryResult<T> = Result<T, MemoryError>;

/// Specialized memory errors with detailed context
#[derive(Debug, thiserror::Error)]
#[allow(missing_docs)]
pub enum MemoryError {
    /// Memory bounds check violation
    #[error("Memory bounds violation: attempted to access offset {offset} with length {length}, but memory size is {memory_size}")]
    BoundsViolation {
        offset: usize,
        length: usize,
        memory_size: usize,
    },
    /// Memory growth failure
    #[error("Memory growth failed: attempted to grow from {current_pages} to {requested_pages} pages, but maximum is {maximum_pages:?}")]
    GrowthFailure {
        current_pages: u64,
        requested_pages: u64,
        maximum_pages: Option<u64>,
    },
    /// Memory alignment error
    #[error("Memory alignment error: offset {offset} is not aligned for {data_type:?} (requires {required_alignment} byte alignment)")]
    AlignmentError {
        offset: usize,
        data_type: MemoryDataType,
        required_alignment: usize,
    },
    /// Memory sharing violation
    #[error("Memory sharing violation: {message}")]
    SharingViolation { message: String },
    /// Memory initialization error
    #[error("Memory initialization failed: {message}")]
    InitializationError { message: String },
}

impl From<MemoryError> for WasmtimeError {
    fn from(error: MemoryError) -> Self {
        WasmtimeError::Memory {
            message: error.to_string(),
        }
    }
}

/// Builder for creating configured memory instances
#[derive(Debug)]
pub struct MemoryBuilder {
    pub(crate) initial_pages: u64,
    pub(crate) maximum_pages: Option<u64>,
    pub(crate) is_shared: bool,
    pub(crate) is_64: bool,
    pub(crate) memory_index: u32,
    pub(crate) name: Option<String>,
}

impl MemoryBuilder {
    /// Create a new memory builder with default configuration
    pub fn new(initial_pages: u64) -> Self {
        Self {
            initial_pages,
            maximum_pages: None,
            is_shared: false,
            is_64: false,
            memory_index: 0,
            name: None,
        }
    }

    /// Set maximum pages limit
    pub fn maximum_pages(mut self, max_pages: u64) -> Self {
        self.maximum_pages = Some(max_pages);
        self
    }

    /// Enable shared memory
    pub fn shared(mut self) -> Self {
        self.is_shared = true;
        self
    }

    /// Enable 64-bit memory addressing (Memory64 proposal)
    pub fn memory64(mut self) -> Self {
        self.is_64 = true;
        self
    }

    /// Set memory index for multi-memory support
    pub fn memory_index(mut self, index: u32) -> Self {
        self.memory_index = index;
        self
    }

    /// Set debug name for this memory
    pub fn name<S: Into<String>>(mut self, name: S) -> Self {
        self.name = Some(name.into());
        self
    }

    /// Build the memory instance
    pub fn build(self, store: &mut crate::store::Store) -> WasmtimeResult<super::Memory> {
        super::Memory::new_with_config(store, self.into())
    }
}

impl From<MemoryBuilder> for MemoryConfig {
    fn from(builder: MemoryBuilder) -> Self {
        Self {
            initial_pages: builder.initial_pages,
            maximum_pages: builder.maximum_pages,
            is_shared: builder.is_shared,
            is_64: builder.is_64,
            memory_index: builder.memory_index,
            name: builder.name,
        }
    }
}

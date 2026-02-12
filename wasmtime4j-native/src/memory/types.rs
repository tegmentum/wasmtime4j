//! Memory types, enums, configuration structs, and error definitions
//!
//! This module contains all type definitions for the memory subsystem including:
//! - Memory variant enum (regular vs shared memory)
//! - Page size options
//! - Memory data types for typed operations
//! - Configuration structs
//! - Error types

use std::collections::HashMap;
use std::collections::VecDeque;
use std::ffi::c_void;
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant, SystemTime};

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::store::Store;
use wasmtime::{Memory as WasmtimeMemory, MemoryType, SharedMemory as WasmtimeSharedMemory};

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

/// Page size options for memory allocation
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum PageSize {
    /// Default page size
    Default,
    /// Small page size (4KB)
    Small,
    /// Large page size (2MB)
    Large,
    /// Huge page size (1GB)
    Huge,
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

/// Memory allocation strategy configuration
#[derive(Debug, Clone)]
pub struct PlatformMemoryConfig {
    /// Enable huge pages allocation
    pub enable_huge_pages: bool,
    /// NUMA node preference (-1 for automatic)
    pub numa_node: i32,
    /// Memory pool initial size in bytes
    pub initial_pool_size: usize,
    /// Maximum pool size in bytes
    pub max_pool_size: usize,
    /// Enable memory compression
    pub enable_compression: bool,
    /// Enable memory deduplication
    pub enable_deduplication: bool,
    /// Prefetch buffer size in bytes
    pub prefetch_buffer_size: usize,
    /// Enable memory leak detection
    pub enable_leak_detection: bool,
    /// Memory alignment requirement
    pub alignment: usize,
    /// Page size preference
    pub page_size: PageSize,
}

impl Default for PlatformMemoryConfig {
    fn default() -> Self {
        Self {
            enable_huge_pages: true,
            numa_node: -1,
            initial_pool_size: 64 * 1024 * 1024,   // 64MB
            max_pool_size: 2 * 1024 * 1024 * 1024, // 2GB
            enable_compression: true,
            enable_deduplication: true,
            prefetch_buffer_size: 4 * 1024 * 1024, // 4MB
            enable_leak_detection: true,
            alignment: 64, // Cache line alignment
            page_size: PageSize::Default,
        }
    }
}

/// Platform-specific memory information
#[derive(Debug, Clone)]
pub struct PlatformMemoryInfo {
    /// Total physical memory in bytes
    pub total_physical_memory: u64,
    /// Available memory in bytes
    pub available_memory: u64,
    /// System page size in bytes
    pub page_size: u64,
    /// Huge page size in bytes
    pub huge_page_size: u64,
    /// Number of NUMA nodes
    pub numa_nodes: u32,
    /// Number of CPU cores
    pub cpu_cores: u32,
    /// Cache line size in bytes
    pub cache_line_size: u32,
    /// Whether huge pages are supported
    pub supports_huge_pages: bool,
    /// Whether NUMA is supported
    pub supports_numa: bool,
}

/// Memory allocation tracking information
#[derive(Debug, Clone)]
pub struct AllocationInfo {
    /// Pointer to the allocated memory
    pub ptr: *mut c_void,
    /// Size of the allocation in bytes
    pub size: usize,
    /// Alignment of the allocation
    pub alignment: usize,
    /// Page type used for the allocation
    pub page_type: PageSize,
    /// NUMA node where memory was allocated
    pub numa_node: i32,
    /// Timestamp when allocation was made
    pub timestamp: SystemTime,
    /// Stack trace at allocation time (if enabled)
    pub stack_trace: Option<String>,
}

/// Memory pool statistics
#[derive(Debug, Clone)]
pub struct PlatformMemoryPoolStats {
    /// Total bytes allocated
    pub total_allocated: u64,
    /// Total bytes freed
    pub total_freed: u64,
    /// Current memory usage in bytes
    pub current_usage: u64,
    /// Peak memory usage in bytes
    pub peak_usage: u64,
    /// Number of allocations made
    pub allocation_count: u64,
    /// Number of deallocations made
    pub deallocation_count: u64,
    /// Fragmentation ratio (0.0-1.0)
    pub fragmentation_ratio: f64,
    /// Compression ratio achieved
    pub compression_ratio: f64,
    /// Bytes saved through deduplication
    pub deduplication_savings: u64,
    /// Number of huge pages used
    pub huge_pages_used: u64,
    /// NUMA hit rate (0.0-1.0)
    pub numa_hit_rate: f64,
}

impl Default for PlatformMemoryPoolStats {
    fn default() -> Self {
        Self {
            total_allocated: 0,
            total_freed: 0,
            current_usage: 0,
            peak_usage: 0,
            allocation_count: 0,
            deallocation_count: 0,
            fragmentation_ratio: 0.0,
            compression_ratio: 1.0,
            deduplication_savings: 0,
            huge_pages_used: 0,
            numa_hit_rate: 1.0,
        }
    }
}

/// Memory leak detection information
#[derive(Debug, Clone)]
pub struct PlatformMemoryLeak {
    /// Information about the leaked allocation
    pub allocation_info: AllocationInfo,
    /// How long the allocation has been alive
    pub age: Duration,
    /// Whether this is a suspected leak
    pub is_suspected_leak: bool,
    /// Confidence score for leak detection (0.0-1.0)
    pub confidence_score: f64,
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
    pub(crate) platform_config: Option<PlatformMemoryConfig>,
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
            platform_config: None,
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

    /// Set platform-specific memory configuration
    pub fn platform_config(mut self, config: PlatformMemoryConfig) -> Self {
        self.platform_config = Some(config);
        self
    }

    /// Build the memory instance
    pub fn build(self, store: &mut Store) -> WasmtimeResult<super::Memory> {
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

// Internal structs for platform allocator

/// Memory pool for efficient allocation/deallocation
#[derive(Debug)]
pub(crate) struct PlatformMemoryPool {
    pub(crate) pool_size: usize,
    pub(crate) free_blocks: VecDeque<(*mut c_void, usize)>,
    pub(crate) allocated_blocks: HashMap<*mut c_void, usize>,
    pub(crate) total_size: usize,
    pub(crate) used_size: usize,
}

impl PlatformMemoryPool {
    pub(crate) fn new(pool_size: usize) -> Self {
        Self {
            pool_size,
            free_blocks: VecDeque::new(),
            allocated_blocks: HashMap::new(),
            total_size: 0,
            used_size: 0,
        }
    }
}

/// NUMA topology detection and management
#[derive(Debug)]
pub(crate) struct PlatformNumaTopology {
    pub(crate) node_count: u32,
    pub(crate) core_count: u32,
    pub(crate) nodes: HashMap<u32, PlatformNumaNode>,
    pub(crate) current_node: Arc<Mutex<u32>>,
}

/// NUMA node information
#[derive(Debug)]
pub(crate) struct PlatformNumaNode {
    pub(crate) id: u32,
    pub(crate) memory_total: u64,
    pub(crate) memory_free: u64,
    pub(crate) cpu_cores: Vec<u32>,
}

/// Memory leak detector
#[derive(Debug)]
pub(crate) struct PlatformMemoryLeakDetector {
    pub(crate) allocations: Arc<Mutex<HashMap<*mut c_void, (usize, SystemTime)>>>,
    pub(crate) suspected_leaks: Arc<Mutex<Vec<PlatformMemoryLeak>>>,
    pub(crate) check_interval: Duration,
    pub(crate) leak_threshold: Duration,
}

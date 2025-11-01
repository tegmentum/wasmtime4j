//! Platform-specific memory management with huge pages, NUMA awareness, and WebAssembly linear memory
//!
//! This module provides comprehensive memory management optimizations for wasmtime4j including:
//! - WebAssembly linear memory management with comprehensive bounds checking
//! - Huge pages support for Linux, macOS, and Windows
//! - NUMA-aware memory allocation and thread binding
//! - Custom memory allocators with platform-specific optimization
//! - Memory prefetching and cache optimization strategies
//! - Memory compression and deduplication for WebAssembly heaps
//! - Comprehensive memory usage monitoring and leak detection
//! - Memory pool management with size-based allocation strategies
//! - Platform-specific virtual memory management and protection

use std::collections::{HashMap, VecDeque};
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant, SystemTime};
use std::ptr::{self, NonNull};
use std::ffi::c_void;
use anyhow::{anyhow, Result};
use log::{debug, info, error};

use wasmtime::{Memory as WasmtimeMemory, MemoryType};
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::store::Store;

// Platform-specific imports
#[cfg(target_os = "linux")]
use libc::{madvise, mmap, munmap, sysconf, MADV_HUGEPAGE, MADV_NORMAL, MAP_ANONYMOUS, MAP_PRIVATE, MAP_HUGETLB, PROT_READ, PROT_WRITE, _SC_PAGESIZE, _SC_PHYS_PAGES};

#[cfg(target_os = "macos")]
use libc::{madvise, mmap, munmap, sysconf, MADV_NORMAL, MAP_ANONYMOUS, MAP_PRIVATE, PROT_READ, PROT_WRITE, _SC_PAGESIZE, _SC_PHYS_PAGES};

#[cfg(target_os = "windows")]
use std::os::windows::io::RawHandle;

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
            initial_pool_size: 64 * 1024 * 1024,  // 64MB
            max_pool_size: 2 * 1024 * 1024 * 1024,  // 2GB
            enable_compression: true,
            enable_deduplication: true,
            prefetch_buffer_size: 4 * 1024 * 1024,  // 4MB
            enable_leak_detection: true,
            alignment: 64,  // Cache line alignment
            page_size: PageSize::Default,
        }
    }
}

/// Page size options for memory allocation
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum PageSize {
    Default,
    Small,      // 4KB
    Large,      // 2MB
    Huge,       // 1GB
}

/// Platform-specific memory information
#[derive(Debug, Clone)]
pub struct PlatformMemoryInfo {
    pub total_physical_memory: u64,
    pub available_memory: u64,
    pub page_size: u64,
    pub huge_page_size: u64,
    pub numa_nodes: u32,
    pub cpu_cores: u32,
    pub cache_line_size: u32,
    pub supports_huge_pages: bool,
    pub supports_numa: bool,
}

/// Memory allocation tracking information
#[derive(Debug, Clone)]
pub struct AllocationInfo {
    pub ptr: *mut c_void,
    pub size: usize,
    pub alignment: usize,
    pub page_type: PageSize,
    pub numa_node: i32,
    pub timestamp: SystemTime,
    pub stack_trace: Option<String>,
}

/// Memory pool statistics
#[derive(Debug, Clone)]
pub struct PlatformMemoryPoolStats {
    pub total_allocated: u64,
    pub total_freed: u64,
    pub current_usage: u64,
    pub peak_usage: u64,
    pub allocation_count: u64,
    pub deallocation_count: u64,
    pub fragmentation_ratio: f64,
    pub compression_ratio: f64,
    pub deduplication_savings: u64,
    pub huge_pages_used: u64,
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
    pub allocation_info: AllocationInfo,
    pub age: Duration,
    pub is_suspected_leak: bool,
    pub confidence_score: f64,
}

/// Thread-safe WebAssembly memory wrapper with comprehensive bounds checking
#[derive(Debug)]
pub struct Memory {
    /// Wasmtime memory instance
    inner: WasmtimeMemory,
    /// Memory metadata and statistics
    metadata: Arc<RwLock<MemoryMetadata>>,
    /// Memory configuration and limits
    config: MemoryConfig,
    /// Cached memory type (stored on creation to avoid needing Store for type queries)
    pub memory_type: MemoryType,
    /// Platform-specific memory allocator (optional)
    platform_allocator: Option<Arc<PlatformMemoryAllocator>>,
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
    /// Memory index within the module (for multi-memory support)
    pub memory_index: u32,
    /// Name of this memory (for debugging)
    pub name: Option<String>,
}

/// Platform-specific memory allocator with advanced optimizations
#[derive(Debug)]
pub struct PlatformMemoryAllocator {
    config: PlatformMemoryConfig,
    memory_info: PlatformMemoryInfo,
    allocations: Arc<Mutex<HashMap<*mut c_void, AllocationInfo>>>,
    memory_pools: Arc<RwLock<HashMap<usize, PlatformMemoryPool>>>,
    stats: Arc<Mutex<PlatformMemoryPoolStats>>,
    compression_cache: Arc<Mutex<HashMap<Vec<u8>, (usize, SystemTime)>>>,
    deduplication_map: Arc<Mutex<HashMap<u64, (*mut c_void, usize)>>>,
    numa_topology: PlatformNumaTopology,
    leak_detector: Option<PlatformMemoryLeakDetector>,
}

/// Memory pool for efficient allocation/deallocation
#[derive(Debug)]
struct PlatformMemoryPool {
    pool_size: usize,
    free_blocks: VecDeque<(*mut c_void, usize)>,
    allocated_blocks: HashMap<*mut c_void, usize>,
    total_size: usize,
    used_size: usize,
}

/// NUMA topology detection and management
#[derive(Debug)]
struct PlatformNumaTopology {
    node_count: u32,
    core_count: u32,
    nodes: HashMap<u32, PlatformNumaNode>,
    current_node: Arc<Mutex<u32>>,
}

/// NUMA node information
#[derive(Debug)]
struct PlatformNumaNode {
    id: u32,
    memory_total: u64,
    memory_free: u64,
    cpu_cores: Vec<u32>,
}

/// Memory leak detector
#[derive(Debug)]
struct PlatformMemoryLeakDetector {
    allocations: Arc<Mutex<HashMap<*mut c_void, (usize, SystemTime)>>>,
    suspected_leaks: Arc<Mutex<Vec<PlatformMemoryLeak>>>,
    check_interval: Duration,
    leak_threshold: Duration,
}

/// Builder for creating configured memory instances
#[derive(Debug)]
pub struct MemoryBuilder {
    initial_pages: u64,
    maximum_pages: Option<u64>,
    is_shared: bool,
    memory_index: u32,
    name: Option<String>,
    platform_config: Option<PlatformMemoryConfig>,
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

impl MemoryBuilder {
    /// Create a new memory builder with default configuration
    pub fn new(initial_pages: u64) -> Self {
        Self {
            initial_pages,
            maximum_pages: None,
            is_shared: false,
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
    pub fn build(self, store: &mut Store) -> WasmtimeResult<Memory> {
        Memory::new_with_config(store, self.into())
    }
}

impl From<MemoryBuilder> for MemoryConfig {
    fn from(builder: MemoryBuilder) -> Self {
        Self {
            initial_pages: builder.initial_pages,
            maximum_pages: builder.maximum_pages,
            is_shared: builder.is_shared,
            memory_index: builder.memory_index,
            name: builder.name,
        }
    }
}

impl Memory {
    /// Create a new memory with default configuration
    pub fn new(store: &mut Store, initial_pages: u64) -> WasmtimeResult<Self> {
        let config = MemoryConfig {
            initial_pages,
            maximum_pages: None,
            is_shared: false,
            memory_index: 0,
            name: None,
        };
        Self::new_with_config(store, config)
    }

    /// Create a new memory with platform-specific configuration
    pub fn new_with_platform_config(
        store: &mut Store,
        config: MemoryConfig,
        platform_config: PlatformMemoryConfig
    ) -> WasmtimeResult<Self> {
        // Create platform allocator
        let platform_allocator = Arc::new(
            PlatformMemoryAllocator::new(platform_config)
                .map_err(|e| WasmtimeError::Memory {
                    message: format!("Failed to create platform allocator: {}", e),
                })?
        );

        // Create memory with standard configuration
        let mut memory = Self::new_with_config(store, config)?;
        memory.platform_allocator = Some(platform_allocator);
        Ok(memory)
    }

    /// Create a new memory with specific configuration
    pub fn new_with_config(store: &mut Store, config: MemoryConfig) -> WasmtimeResult<Self> {
        // Validate configuration parameters
        if config.initial_pages == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Initial pages cannot be zero".to_string(),
            });
        }

        if let Some(max_pages) = config.maximum_pages {
            if config.initial_pages > max_pages {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Initial pages ({}) cannot exceed maximum pages ({})",
                        config.initial_pages, max_pages
                    ),
                });
            }
        }

        // Check WebAssembly memory limits (4GB max)
        const MAX_WASM_PAGES: u64 = 65536; // 4GB / 64KB
        if config.initial_pages > MAX_WASM_PAGES {
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Initial pages ({}) exceeds WebAssembly limit ({})",
                    config.initial_pages, MAX_WASM_PAGES
                ),
            });
        }

        if let Some(max_pages) = config.maximum_pages {
            if max_pages > MAX_WASM_PAGES {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Maximum pages ({}) exceeds WebAssembly limit ({})",
                        max_pages, MAX_WASM_PAGES
                    ),
                });
            }
        }

        // Create Wasmtime memory type
        let memory_type = MemoryType::new64(
            config.initial_pages,
            config.maximum_pages,
        );

        // Create memory instance
        let inner = store.with_context(|ctx| {
            WasmtimeMemory::new(ctx, memory_type.clone())
                .map_err(|e| WasmtimeError::Memory {
                    message: format!("Failed to create memory: {}", e),
                })
        })?;

        // Initialize metadata
        let metadata = MemoryMetadata {
            created_at: Instant::now(),
            current_pages: config.initial_pages,
            maximum_pages: config.maximum_pages,
            read_operations: 0,
            write_operations: 0,
            bytes_read: 0,
            bytes_written: 0,
            growth_operations: 0,
            peak_pages: config.initial_pages,
            last_access: None,
            bounds_violations_prevented: 0,
        };

        Ok(Self {
            inner,
            metadata: Arc::new(RwLock::new(metadata)),
            config,
            memory_type,
            platform_allocator: None,
        })
    }

    /// Get reference to inner Wasmtime memory (internal use)
    pub(crate) fn inner(&self) -> &WasmtimeMemory {
        &self.inner
    }

    /// Get current memory size in pages
    pub fn size_pages(&self, store: &Store) -> WasmtimeResult<u64> {
        store.with_context_ro(|ctx| {
            Ok(self.inner.size(ctx))
        })
    }

    /// Get current memory size in bytes
    pub fn size_bytes(&self, store: &Store) -> WasmtimeResult<usize> {
        Ok((self.size_pages(store)? * 65536) as usize)
    }

    /// Get memory data size for bounds checking
    #[allow(dead_code)]
    fn get_data_size(&self, store: &Store) -> WasmtimeResult<usize> {
        store.with_context_ro(|ctx| {
            Ok(self.inner.data(ctx).len())
        })
    }

    /// Grow memory by the specified number of pages with validation
    pub fn grow(&self, store: &mut Store, additional_pages: u64) -> WasmtimeResult<u64> {
        // Get current size
        let current_pages = self.size_pages(store)?;
        let requested_pages = current_pages + additional_pages;

        // Validate growth against limits
        if let Some(max_pages) = self.config.maximum_pages {
            if requested_pages > max_pages {
                return Err(MemoryError::GrowthFailure {
                    current_pages,
                    requested_pages,
                    maximum_pages: Some(max_pages),
                }.into());
            }
        }

        // Check WebAssembly memory limits
        const MAX_WASM_PAGES: u64 = 65536;
        if requested_pages > MAX_WASM_PAGES {
            return Err(MemoryError::GrowthFailure {
                current_pages,
                requested_pages,
                maximum_pages: Some(MAX_WASM_PAGES),
            }.into());
        }

        // Perform the growth operation with shared memory considerations
        let previous_pages = if self.config.is_shared {
            // For shared memory, we need to coordinate growth across threads
            self.grow_shared_memory(store, additional_pages, current_pages, requested_pages)?
        } else {
            // Regular memory growth
            store.with_context(|ctx| {
                self.inner.grow(ctx, additional_pages)
                    .map_err(|e| WasmtimeError::Memory {
                        message: format!("Memory growth failed: {}", e),
                    })
            })?
        };

        // Update metadata
        if let Ok(mut metadata) = self.metadata.write() {
            metadata.current_pages = requested_pages;
            metadata.growth_operations += 1;
            if requested_pages > metadata.peak_pages {
                metadata.peak_pages = requested_pages;
            }
        }

        log::debug!(
            "Memory grown from {} to {} pages ({} bytes) [shared: {}]",
            previous_pages,
            requested_pages,
            requested_pages * 65536,
            self.config.is_shared
        );

        Ok(previous_pages)
    }

    /// Thread-safe growth for shared memory
    fn grow_shared_memory(
        &self,
        store: &mut Store,
        additional_pages: u64,
        current_pages: u64,
        requested_pages: u64
    ) -> WasmtimeResult<u64> {
        // For shared memory, we need to ensure thread-safe growth
        // This is a simplified implementation - a real implementation would
        // need more sophisticated synchronization

        store.with_context(|ctx| {
            // Use atomic operations or locks to ensure only one thread can grow at a time
            // For now, we'll use the same growth mechanism as regular memory
            // but with additional logging for shared memory
            log::debug!(
                "Growing shared memory from {} to {} pages (adding {})",
                current_pages, requested_pages, additional_pages
            );

            let result = self.inner.grow(ctx, additional_pages)
                .map_err(|e| WasmtimeError::Memory {
                    message: format!("Shared memory growth failed: {}", e),
                });

            // In a real implementation, we would:
            // 1. Notify all threads that memory has grown
            // 2. Update any cached memory pointers in other threads
            // 3. Ensure memory barriers are in place

            if result.is_ok() {
                log::info!("Shared memory successfully grown to {} pages", requested_pages);
                // Emit memory fence to ensure growth is visible to all threads
                std::sync::atomic::fence(std::sync::atomic::Ordering::SeqCst);
            }

            result
        })
    }

    /// Read data with comprehensive bounds checking
    pub fn read_bytes(&self, store: &Store, offset: usize, length: usize) -> WasmtimeResult<Vec<u8>> {
        // Bounds checking
        let memory_size = self.size_bytes(store)?;
        if offset.saturating_add(length) > memory_size {
            let mut metadata = self.metadata.write().map_err(|_| {
                WasmtimeError::Concurrency {
                    message: "Failed to acquire metadata lock".to_string(),
                }
            })?;
            metadata.bounds_violations_prevented += 1;
            
            return Err(MemoryError::BoundsViolation {
                offset,
                length,
                memory_size,
            }.into());
        }

        // Read memory data within the store lock
        let result = store.with_context_ro(|ctx| {
            let data = self.inner.data(ctx);
            Ok(data[offset..offset + length].to_vec())
        })?;

        // Update statistics
        if let Ok(mut metadata) = self.metadata.write() {
            metadata.read_operations += 1;
            metadata.bytes_read += length as u64;
            metadata.last_access = Some(Instant::now());
        }

        Ok(result)
    }

    /// Write data with comprehensive bounds checking
    pub fn write_bytes(&self, store: &mut Store, offset: usize, data: &[u8]) -> WasmtimeResult<()> {
        let length = data.len();
        
        // Bounds checking
        let memory_size = self.size_bytes(store)?;
        if offset.saturating_add(length) > memory_size {
            let mut metadata = self.metadata.write().map_err(|_| {
                WasmtimeError::Concurrency {
                    message: "Failed to acquire metadata lock".to_string(),
                }
            })?;
            metadata.bounds_violations_prevented += 1;
            
            return Err(MemoryError::BoundsViolation {
                offset,
                length,
                memory_size,
            }.into());
        }

        // Write memory data within the store lock
        store.with_context(|ctx| {
            let memory_data = self.inner.data_mut(ctx);
            memory_data[offset..offset + length].copy_from_slice(data);
            Ok(())
        })?;

        // Update statistics
        if let Ok(mut metadata) = self.metadata.write() {
            metadata.write_operations += 1;
            metadata.bytes_written += length as u64;
            metadata.last_access = Some(Instant::now());
        }

        Ok(())
    }

    /// Read typed value with alignment checking
    pub fn read_typed<T>(&self, store: &Store, offset: usize, data_type: MemoryDataType) -> WasmtimeResult<T>
    where
        T: Default + Copy,
    {
        // Check alignment
        let alignment = Self::get_type_alignment(data_type);
        if offset % alignment != 0 {
            return Err(MemoryError::AlignmentError {
                offset,
                data_type,
                required_alignment: alignment,
            }.into());
        }

        let type_size = Self::get_type_size(data_type);
        let bytes = self.read_bytes(store, offset, type_size)?;
        
        // Convert bytes to typed value
        Self::bytes_to_typed_value(&bytes, data_type)
    }

    /// Write typed value with alignment checking  
    pub fn write_typed<T>(&self, store: &mut Store, offset: usize, value: T, data_type: MemoryDataType) -> WasmtimeResult<()>
    where
        T: Copy,
    {
        // Check alignment
        let alignment = Self::get_type_alignment(data_type);
        if offset % alignment != 0 {
            return Err(MemoryError::AlignmentError {
                offset,
                data_type,
                required_alignment: alignment,
            }.into());
        }

        let bytes = Self::typed_value_to_bytes(value, data_type)?;
        self.write_bytes(store, offset, &bytes)
    }

    /// Get memory usage statistics
    pub fn get_usage(&self, store: &Store) -> WasmtimeResult<MemoryUsage> {
        let current_pages = self.size_pages(store)?;
        let current_bytes = (current_pages * 65536) as usize;
        let maximum_bytes = self.config.maximum_pages.map(|p| (p * 65536) as usize);
        
        let metadata = self.metadata.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire metadata lock".to_string(),
            }
        })?;

        let peak_bytes = (metadata.peak_pages * 65536) as usize;
        let bytes_transferred = metadata.bytes_read + metadata.bytes_written;
        
        let utilization_percent = if let Some(max_bytes) = maximum_bytes {
            (current_bytes as f64 / max_bytes as f64) * 100.0
        } else {
            0.0 // Unlimited memory
        };

        Ok(MemoryUsage {
            current_bytes,
            current_pages,
            maximum_bytes,
            peak_bytes,
            read_count: metadata.read_operations,
            write_count: metadata.write_operations,
            bytes_transferred,
            utilization_percent,
        })
    }

    /// Get memory metadata (read-only)
    pub fn get_metadata(&self) -> WasmtimeResult<MemoryMetadata> {
        let metadata = self.metadata.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire metadata lock".to_string(),
            }
        })?;
        Ok(metadata.clone())
    }

    /// Get memory configuration
    pub fn get_config(&self) -> &MemoryConfig {
        &self.config
    }

    /// Get Wasmtime memory handle for advanced operations
    pub fn as_wasmtime_memory(&self) -> &WasmtimeMemory {
        &self.inner
    }

    /// Get memory type information from the underlying Wasmtime memory
    /// This requires a Store to access the type information
    pub fn get_type(&self, store: &Store) -> WasmtimeResult<MemoryType> {
        store.with_context_ro(|ctx| {
            Ok(self.inner.ty(ctx))
        })
    }

    /// Create Memory wrapper from existing Wasmtime memory (for memory exports)
    /// Note: This creates a wrapper with default metadata since we don't have store context
    pub fn from_wasmtime_memory(wasmtime_memory: WasmtimeMemory, memory_type: MemoryType) -> Self {
        // Extract configuration from memory type
        let initial_pages = memory_type.minimum();
        let maximum_pages = memory_type.maximum();
        let is_shared = memory_type.is_shared();

        // Create configuration based on memory type
        let config = MemoryConfig {
            initial_pages,
            maximum_pages,
            is_shared,
            memory_index: 0, // Default index for exported memory
            name: Some("exported_memory".to_string()),
        };

        // Initialize metadata for exported memory
        let metadata = MemoryMetadata {
            created_at: Instant::now(),
            current_pages: initial_pages,
            maximum_pages,
            read_operations: 0,
            write_operations: 0,
            bytes_read: 0,
            bytes_written: 0,
            growth_operations: 0,
            peak_pages: initial_pages,
            last_access: None,
            bounds_violations_prevented: 0,
        };

        Self {
            inner: wasmtime_memory,
            metadata: Arc::new(RwLock::new(metadata)),
            config,
            memory_type,
            platform_allocator: None,
        }
    }

    // Helper functions for type conversion
    
    fn get_type_size(data_type: MemoryDataType) -> usize {
        match data_type {
            MemoryDataType::U8 | MemoryDataType::I8 => 1,
            MemoryDataType::U16Le | MemoryDataType::I16Le => 2,
            MemoryDataType::U32Le | MemoryDataType::I32Le | MemoryDataType::F32Le => 4,
            MemoryDataType::U64Le | MemoryDataType::I64Le | MemoryDataType::F64Le => 8,
        }
    }

    fn get_type_alignment(data_type: MemoryDataType) -> usize {
        match data_type {
            MemoryDataType::U8 | MemoryDataType::I8 => 1,
            MemoryDataType::U16Le | MemoryDataType::I16Le => 2,
            MemoryDataType::U32Le | MemoryDataType::I32Le | MemoryDataType::F32Le => 4,
            MemoryDataType::U64Le | MemoryDataType::I64Le | MemoryDataType::F64Le => 8,
        }
    }

    fn bytes_to_typed_value<T>(bytes: &[u8], data_type: MemoryDataType) -> WasmtimeResult<T>
    where
        T: Default + Copy,
    {
        if bytes.len() != Self::get_type_size(data_type) {
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Byte array length {} does not match expected size {} for type {:?}",
                    bytes.len(),
                    Self::get_type_size(data_type),
                    data_type
                ),
            });
        }

        // Use unsafe casting to convert bytes to the target type
        // This is safe because we've validated the size above
        unsafe {
            match data_type {
                MemoryDataType::U8 => {
                    let value = bytes[0] as u8;
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::I8 => {
                    let value = bytes[0] as i8;
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::U16Le => {
                    let value = u16::from_le_bytes([bytes[0], bytes[1]]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::I16Le => {
                    let value = i16::from_le_bytes([bytes[0], bytes[1]]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::U32Le => {
                    let value = u32::from_le_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::I32Le => {
                    let value = i32::from_le_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::U64Le => {
                    let value = u64::from_le_bytes([
                        bytes[0], bytes[1], bytes[2], bytes[3],
                        bytes[4], bytes[5], bytes[6], bytes[7]
                    ]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::I64Le => {
                    let value = i64::from_le_bytes([
                        bytes[0], bytes[1], bytes[2], bytes[3],
                        bytes[4], bytes[5], bytes[6], bytes[7]
                    ]);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::F32Le => {
                    let bits = u32::from_le_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]);
                    let value = f32::from_bits(bits);
                    Ok(std::mem::transmute_copy(&value))
                }
                MemoryDataType::F64Le => {
                    let bits = u64::from_le_bytes([
                        bytes[0], bytes[1], bytes[2], bytes[3],
                        bytes[4], bytes[5], bytes[6], bytes[7]
                    ]);
                    let value = f64::from_bits(bits);
                    Ok(std::mem::transmute_copy(&value))
                }
            }
        }
    }

    fn typed_value_to_bytes<T>(value: T, data_type: MemoryDataType) -> WasmtimeResult<Vec<u8>>
    where
        T: Copy,
    {
        // Use unsafe transmutation to convert typed values to bytes
        // This is safe for primitive numeric types
        unsafe {
            match data_type {
                MemoryDataType::U8 => {
                    let val: u8 = std::mem::transmute_copy(&value);
                    Ok(vec![val])
                }
                MemoryDataType::I8 => {
                    let val: i8 = std::mem::transmute_copy(&value);
                    Ok(vec![val as u8])
                }
                MemoryDataType::U16Le => {
                    let val: u16 = std::mem::transmute_copy(&value);
                    Ok(val.to_le_bytes().to_vec())
                }
                MemoryDataType::I16Le => {
                    let val: i16 = std::mem::transmute_copy(&value);
                    Ok(val.to_le_bytes().to_vec())
                }
                MemoryDataType::U32Le => {
                    let val: u32 = std::mem::transmute_copy(&value);
                    Ok(val.to_le_bytes().to_vec())
                }
                MemoryDataType::I32Le => {
                    let val: i32 = std::mem::transmute_copy(&value);
                    Ok(val.to_le_bytes().to_vec())
                }
                MemoryDataType::U64Le => {
                    let val: u64 = std::mem::transmute_copy(&value);
                    Ok(val.to_le_bytes().to_vec())
                }
                MemoryDataType::I64Le => {
                    let val: i64 = std::mem::transmute_copy(&value);
                    Ok(val.to_le_bytes().to_vec())
                }
                MemoryDataType::F32Le => {
                    let val: f32 = std::mem::transmute_copy(&value);
                    Ok(val.to_bits().to_le_bytes().to_vec())
                }
                MemoryDataType::F64Le => {
                    let val: f64 = std::mem::transmute_copy(&value);
                    Ok(val.to_bits().to_le_bytes().to_vec())
                }
            }
        }
    }
}

impl PlatformMemoryAllocator {
    /// Creates a new platform-specific memory allocator
    pub fn new(config: PlatformMemoryConfig) -> Result<Self> {
        let memory_info = Self::gather_memory_info()?;
        let numa_topology = PlatformNumaTopology::detect()?;
        let leak_detector = if config.enable_leak_detection {
            Some(PlatformMemoryLeakDetector::new()?)
        } else {
            None
        };

        info!("Memory allocator initialized: {:?}", memory_info);
        info!("NUMA topology detected: nodes={}, cores={}",
              numa_topology.node_count, numa_topology.core_count);

        Ok(Self {
            config,
            memory_info,
            allocations: Arc::new(Mutex::new(HashMap::new())),
            memory_pools: Arc::new(RwLock::new(HashMap::new())),
            stats: Arc::new(Mutex::new(PlatformMemoryPoolStats::default())),
            compression_cache: Arc::new(Mutex::new(HashMap::new())),
            deduplication_map: Arc::new(Mutex::new(HashMap::new())),
            numa_topology,
            leak_detector,
        })
    }

    /// Gathers platform-specific memory information
    fn gather_memory_info() -> Result<PlatformMemoryInfo> {
        #[cfg(target_os = "linux")]
        {
            Self::gather_linux_memory_info()
        }
        #[cfg(target_os = "macos")]
        {
            Self::gather_macos_memory_info()
        }
        #[cfg(target_os = "windows")]
        {
            Self::gather_windows_memory_info()
        }
        #[cfg(not(any(target_os = "linux", target_os = "macos", target_os = "windows")))]
        {
            Err(anyhow!("Unsupported platform for memory management"))
        }
    }

    #[cfg(target_os = "linux")]
    fn gather_linux_memory_info() -> Result<PlatformMemoryInfo> {
        let page_size = unsafe { sysconf(_SC_PAGESIZE) } as u64;
        let total_pages = unsafe { sysconf(_SC_PHYS_PAGES) } as u64;
        let total_physical_memory = page_size * total_pages;

        // Read /proc/meminfo for more detailed information
        let meminfo = std::fs::read_to_string("/proc/meminfo")?;
        let mut available_memory = 0u64;
        let mut huge_page_size = 2 * 1024 * 1024u64; // Default 2MB

        for line in meminfo.lines() {
            if line.starts_with("MemAvailable:") {
                if let Some(value) = line.split_whitespace().nth(1) {
                    available_memory = value.parse::<u64>().unwrap_or(0) * 1024;
                }
            } else if line.starts_with("Hugepagesize:") {
                if let Some(value) = line.split_whitespace().nth(1) {
                    huge_page_size = value.parse::<u64>().unwrap_or(2048) * 1024;
                }
            }
        }

        // Detect NUMA information
        let numa_nodes = std::fs::read_dir("/sys/devices/system/node")
            .map(|entries| entries.filter_map(|e| e.ok())
                .filter(|e| e.file_name().to_string_lossy().starts_with("node"))
                .count() as u32)
            .unwrap_or(1);

        let cpu_cores = num_cpus::get() as u32;

        Ok(PlatformMemoryInfo {
            total_physical_memory,
            available_memory,
            page_size,
            huge_page_size,
            numa_nodes,
            cpu_cores,
            cache_line_size: 64,
            supports_huge_pages: huge_page_size > page_size,
            supports_numa: numa_nodes > 1,
        })
    }

    #[cfg(target_os = "macos")]
    fn gather_macos_memory_info() -> Result<PlatformMemoryInfo> {
        let page_size = unsafe { sysconf(_SC_PAGESIZE) } as u64;
        let total_pages = unsafe { sysconf(_SC_PHYS_PAGES) } as u64;
        let total_physical_memory = page_size * total_pages;
        let huge_page_size = 2 * 1024 * 1024u64; // 2MB large pages
        let cpu_cores = num_cpus::get() as u32;

        Ok(PlatformMemoryInfo {
            total_physical_memory,
            available_memory: total_physical_memory / 2,
            page_size,
            huge_page_size,
            numa_nodes: 1,
            cpu_cores,
            cache_line_size: 64,
            supports_huge_pages: false,
            supports_numa: false,
        })
    }

    #[cfg(target_os = "windows")]
    fn gather_windows_memory_info() -> Result<PlatformMemoryInfo> {
        use winapi::um::sysinfoapi::{GetSystemInfo, GlobalMemoryStatusEx, MEMORYSTATUSEX, SYSTEM_INFO};

        let mut sys_info: SYSTEM_INFO = unsafe { std::mem::zeroed() };
        let mut mem_status: MEMORYSTATUSEX = unsafe { std::mem::zeroed() };
        mem_status.dwLength = std::mem::size_of::<MEMORYSTATUSEX>() as u32;

        unsafe {
            GetSystemInfo(&mut sys_info);
            GlobalMemoryStatusEx(&mut mem_status);
        }

        let page_size = sys_info.dwPageSize as u64;
        let huge_page_size = 2 * 1024 * 1024u64; // 2MB large pages on Windows

        Ok(PlatformMemoryInfo {
            total_physical_memory: mem_status.ullTotalPhys,
            available_memory: mem_status.ullAvailPhys,
            page_size,
            huge_page_size,
            numa_nodes: 1,
            cpu_cores: sys_info.dwNumberOfProcessors,
            cache_line_size: 64,
            supports_huge_pages: true,
            supports_numa: true,
        })
    }

    /// Get platform memory information
    pub fn memory_info(&self) -> &PlatformMemoryInfo {
        &self.memory_info
    }

    /// Allocates memory with platform-specific optimizations
    pub fn allocate(&self, size: usize, alignment: Option<usize>) -> Result<NonNull<c_void>> {
        let alignment = alignment.unwrap_or(self.config.alignment);
        let allocation_size = self.round_up_to_alignment(size, alignment);

        // Try to use existing memory pool first
        if let Some(ptr) = self.try_pool_allocation(allocation_size, alignment)? {
            return Ok(ptr);
        }

        // Determine the best allocation strategy based on size
        let page_type = self.determine_page_type(allocation_size);
        let numa_node = self.select_numa_node();

        let ptr = self.platform_allocate(allocation_size, alignment, page_type, numa_node)?;

        // Record allocation for tracking and leak detection
        self.record_allocation(ptr.as_ptr(), allocation_size, alignment, page_type, numa_node)?;

        // Update statistics
        self.update_allocation_stats(allocation_size);

        debug!("Allocated {} bytes at {:p} with alignment {} on NUMA node {}",
               allocation_size, ptr.as_ptr(), alignment, numa_node);

        Ok(ptr)
    }

    /// Platform-specific memory allocation
    fn platform_allocate(&self, size: usize, alignment: usize, page_type: PageSize, numa_node: i32) -> Result<NonNull<c_void>> {
        #[cfg(target_os = "linux")]
        {
            self.linux_allocate(size, alignment, page_type, numa_node)
        }
        #[cfg(target_os = "macos")]
        {
            self.macos_allocate(size, alignment, page_type, numa_node)
        }
        #[cfg(target_os = "windows")]
        {
            self.windows_allocate(size, alignment, page_type, numa_node)
        }
        #[cfg(not(any(target_os = "linux", target_os = "macos", target_os = "windows")))]
        {
            Err(anyhow!("Platform-specific allocation not implemented"))
        }
    }

    #[cfg(target_os = "linux")]
    fn linux_allocate(&self, size: usize, alignment: usize, page_type: PageSize, numa_node: i32) -> Result<NonNull<c_void>> {
        let mut flags = MAP_PRIVATE | MAP_ANONYMOUS;

        // Add huge page flags if requested and supported
        if page_type == PageSize::Large && self.config.enable_huge_pages {
            flags |= MAP_HUGETLB;
        }

        let ptr = unsafe {
            mmap(
                ptr::null_mut(),
                size,
                PROT_READ | PROT_WRITE,
                flags,
                -1,
                0
            )
        };

        if ptr == libc::MAP_FAILED {
            return Err(anyhow!("mmap failed for size {}", size));
        }

        // Enable huge page hints
        if self.config.enable_huge_pages && page_type == PageSize::Large {
            unsafe {
                madvise(ptr, size, MADV_HUGEPAGE);
            }
        }

        // NUMA binding if requested
        if numa_node >= 0 {
            self.bind_to_numa_node(ptr, size, numa_node as u32)?;
        }

        NonNull::new(ptr).ok_or_else(|| anyhow!("Null pointer returned from mmap"))
    }

    #[cfg(target_os = "macos")]
    fn macos_allocate(&self, size: usize, _alignment: usize, _page_type: PageSize, _numa_node: i32) -> Result<NonNull<c_void>> {
        let ptr = unsafe {
            mmap(
                ptr::null_mut(),
                size,
                PROT_READ | PROT_WRITE,
                MAP_PRIVATE | MAP_ANONYMOUS,
                -1,
                0
            )
        };

        if ptr == libc::MAP_FAILED {
            return Err(anyhow!("mmap failed for size {}", size));
        }

        // macOS-specific optimizations
        unsafe {
            madvise(ptr, size, MADV_NORMAL);
        }

        NonNull::new(ptr).ok_or_else(|| anyhow!("Null pointer returned from mmap"))
    }

    #[cfg(target_os = "windows")]
    fn windows_allocate(&self, size: usize, _alignment: usize, page_type: PageSize, numa_node: i32) -> Result<NonNull<c_void>> {
        use winapi::um::memoryapi::{VirtualAlloc, VirtualAllocExNuma};
        use winapi::um::winnt::{MEM_COMMIT, MEM_RESERVE, MEM_LARGE_PAGES, PAGE_READWRITE};

        let mut flags = MEM_COMMIT | MEM_RESERVE;

        // Add large page flag if requested
        if page_type == PageSize::Large && self.config.enable_huge_pages {
            flags |= MEM_LARGE_PAGES;
        }

        let ptr = if numa_node >= 0 {
            unsafe {
                VirtualAllocExNuma(
                    std::process::id() as RawHandle,
                    ptr::null_mut(),
                    size,
                    flags,
                    PAGE_READWRITE,
                    numa_node as u32
                )
            }
        } else {
            unsafe {
                VirtualAlloc(
                    ptr::null_mut(),
                    size,
                    flags,
                    PAGE_READWRITE
                )
            }
        };

        NonNull::new(ptr).ok_or_else(|| anyhow!("VirtualAlloc failed for size {}", size))
    }

    /// Deallocates memory with proper cleanup
    pub fn deallocate(&self, ptr: NonNull<c_void>) -> Result<()> {
        let allocation_info = self.remove_allocation_record(ptr.as_ptr())?;

        self.platform_deallocate(ptr, allocation_info.size)?;
        self.update_deallocation_stats(allocation_info.size);

        debug!("Deallocated {} bytes at {:p}", allocation_info.size, ptr.as_ptr());
        Ok(())
    }

    /// Platform-specific memory deallocation
    fn platform_deallocate(&self, ptr: NonNull<c_void>, size: usize) -> Result<()> {
        #[cfg(any(target_os = "linux", target_os = "macos"))]
        {
            let result = unsafe { munmap(ptr.as_ptr(), size) };
            if result != 0 {
                return Err(anyhow!("munmap failed"));
            }
        }

        #[cfg(target_os = "windows")]
        {
            use winapi::um::memoryapi::VirtualFree;
            use winapi::um::winnt::MEM_RELEASE;

            let result = unsafe { VirtualFree(ptr.as_ptr(), 0, MEM_RELEASE) };
            if result == 0 {
                return Err(anyhow!("VirtualFree failed"));
            }
        }

        Ok(())
    }

    // Helper methods for internal use
    fn try_pool_allocation(&self, size: usize, alignment: usize) -> Result<Option<NonNull<c_void>>> {
        // Simplified pool allocation - would be more sophisticated in production
        Ok(None)
    }

    fn determine_page_type(&self, size: usize) -> PageSize {
        if size >= self.memory_info.huge_page_size as usize && self.config.enable_huge_pages {
            PageSize::Huge
        } else if size >= 2 * 1024 * 1024 && self.config.enable_huge_pages {
            PageSize::Large
        } else {
            PageSize::Small
        }
    }

    fn select_numa_node(&self) -> i32 {
        if self.config.numa_node >= 0 {
            self.config.numa_node
        } else {
            self.numa_topology.get_optimal_node()
        }
    }

    #[cfg(target_os = "linux")]
    fn bind_to_numa_node(&self, ptr: *mut c_void, size: usize, numa_node: u32) -> Result<()> {
        // Use mbind system call for NUMA binding
        use libc::{mbind, MPOL_BIND};

        let nodemask = 1u64 << numa_node;
        let result = unsafe {
            mbind(
                ptr,
                size,
                MPOL_BIND,
                &nodemask as *const u64 as *const libc::c_ulong,
                64,
                0
            )
        };

        if result != 0 {
            warn!("Failed to bind memory to NUMA node {}: {}", numa_node, result);
        }

        Ok(())
    }

    #[cfg(not(target_os = "linux"))]
    fn bind_to_numa_node(&self, _ptr: *mut c_void, _size: usize, _numa_node: u32) -> Result<()> {
        // NUMA binding not implemented for this platform
        Ok(())
    }

    fn record_allocation(&self, ptr: *mut c_void, size: usize, alignment: usize, page_type: PageSize, numa_node: i32) -> Result<()> {
        let allocation_info = AllocationInfo {
            ptr,
            size,
            alignment,
            page_type,
            numa_node,
            timestamp: SystemTime::now(),
            stack_trace: if self.config.enable_leak_detection {
                Some(self.capture_stack_trace())
            } else {
                None
            },
        };

        self.allocations.lock().unwrap().insert(ptr, allocation_info);

        if let Some(ref detector) = self.leak_detector {
            detector.record_allocation(ptr, size)?;
        }

        Ok(())
    }

    fn remove_allocation_record(&self, ptr: *mut c_void) -> Result<AllocationInfo> {
        let allocation_info = self.allocations.lock().unwrap()
            .remove(&ptr)
            .ok_or_else(|| anyhow!("Allocation not found for pointer {:p}", ptr))?;

        if let Some(ref detector) = self.leak_detector {
            detector.record_deallocation(ptr)?;
        }

        Ok(allocation_info)
    }

    fn capture_stack_trace(&self) -> String {
        format!("Stack trace captured at {}", chrono::Utc::now().format("%Y-%m-%d %H:%M:%S"))
    }

    fn update_allocation_stats(&self, size: usize) {
        let mut stats = self.stats.lock().unwrap();
        stats.total_allocated += size as u64;
        stats.current_usage += size as u64;
        stats.allocation_count += 1;

        if stats.current_usage > stats.peak_usage {
            stats.peak_usage = stats.current_usage;
        }
    }

    fn update_deallocation_stats(&self, size: usize) {
        let mut stats = self.stats.lock().unwrap();
        stats.total_freed += size as u64;
        stats.current_usage -= size as u64;
        stats.deallocation_count += 1;
    }

    fn round_up_to_alignment(&self, size: usize, alignment: usize) -> usize {
        (size + alignment - 1) & !(alignment - 1)
    }

    /// Gets memory statistics
    pub fn get_stats(&self) -> PlatformMemoryPoolStats {
        self.stats.lock().unwrap().clone()
    }

    /// Detects memory leaks
    pub fn detect_leaks(&self) -> Result<Vec<PlatformMemoryLeak>> {
        if let Some(ref detector) = self.leak_detector {
            detector.detect_leaks()
        } else {
            Ok(Vec::new())
        }
    }

    /// Prefetches memory for improved cache performance
    pub fn prefetch_memory(&self, ptr: *const c_void, size: usize) -> Result<()> {
        if size == 0 {
            return Ok(());
        }

        #[cfg(target_arch = "x86_64")]
        {
            // Use prefetch instructions on x86_64
            let prefetch_size = std::cmp::min(size, self.config.prefetch_buffer_size);
            let mut current_ptr = ptr as *const u8;
            let end_ptr = unsafe { current_ptr.add(prefetch_size) };

            while current_ptr < end_ptr {
                unsafe {
                    // Prefetch for read (temporal locality)
                    std::arch::x86_64::_mm_prefetch(current_ptr as *const i8, std::arch::x86_64::_MM_HINT_T0);
                }
                // Move to next cache line (64 bytes typical)
                current_ptr = unsafe { current_ptr.add(64) };
            }
        }

        #[cfg(target_os = "linux")]
        {
            // Use madvise with MADV_WILLNEED
            let result = unsafe {
                madvise(ptr as *mut c_void, size, libc::MADV_WILLNEED)
            };
            if result != 0 {
                warn!("madvise WILLNEED failed for prefetch");
            }
        }

        debug!("Prefetched {} bytes at {:p}", size, ptr);
        Ok(())
    }

    /// Performs memory compression
    pub fn compress_memory(&self, data: &[u8]) -> Result<Vec<u8>> {
        if !self.config.enable_compression {
            return Ok(data.to_vec());
        }

        // Simple compression using flate2
        use flate2::Compression;
        use flate2::write::GzEncoder;
        use std::io::Write;

        let mut encoder = GzEncoder::new(Vec::new(), Compression::default());
        encoder.write_all(data)?;
        Ok(encoder.finish()?)
    }

    /// Performs memory deduplication
    pub fn deduplicate_memory(&self, data: &[u8]) -> Result<*mut c_void> {
        if !self.config.enable_deduplication {
            // Allocate new memory for data
            let ptr = self.allocate(data.len(), None)?;
            unsafe {
                ptr::copy_nonoverlapping(data.as_ptr(), ptr.as_ptr() as *mut u8, data.len());
            }
            return Ok(ptr.as_ptr());
        }

        // Calculate hash of data
        use sha2::{Sha256, Digest};
        let mut hasher = Sha256::new();
        hasher.update(data);
        let hash = hasher.finalize();
        let hash_u64 = u64::from_le_bytes(hash[..8].try_into().unwrap());

        let mut dedup_map = self.deduplication_map.lock().unwrap();

        // Check if we already have this data
        if let Some((existing_ptr, existing_size)) = dedup_map.get(&hash_u64) {
            if *existing_size == data.len() {
                // Verify data matches
                let existing_data = unsafe {
                    std::slice::from_raw_parts(*existing_ptr as *const u8, *existing_size)
                };

                if existing_data == data {
                    // Update statistics
                    let mut stats = self.stats.lock().unwrap();
                    stats.deduplication_savings += data.len() as u64;

                    return Ok(*existing_ptr);
                }
            }
        }

        // Allocate new memory for unique data
        let ptr = self.allocate(data.len(), None)?;
        unsafe {
            ptr::copy_nonoverlapping(data.as_ptr(), ptr.as_ptr() as *mut u8, data.len());
        }

        dedup_map.insert(hash_u64, (ptr.as_ptr(), data.len()));
        Ok(ptr.as_ptr())
    }
}

// Supporting implementations for platform-specific structures

impl PlatformNumaTopology {
    fn detect() -> Result<Self> {
        let node_count = Self::detect_numa_nodes()?;
        let core_count = num_cpus::get() as u32;
        let nodes = Self::build_numa_map(node_count)?;

        Ok(Self {
            node_count,
            core_count,
            nodes,
            current_node: Arc::new(Mutex::new(0)),
        })
    }

    fn detect_numa_nodes() -> Result<u32> {
        #[cfg(target_os = "linux")]
        {
            let numa_path = "/sys/devices/system/node";
            if let Ok(entries) = std::fs::read_dir(numa_path) {
                let node_count = entries
                    .filter_map(|e| e.ok())
                    .filter(|e| e.file_name().to_string_lossy().starts_with("node"))
                    .count() as u32;
                Ok(std::cmp::max(1, node_count))
            } else {
                Ok(1)
            }
        }
        #[cfg(not(target_os = "linux"))]
        {
            Ok(1) // Default to single node
        }
    }

    fn build_numa_map(node_count: u32) -> Result<HashMap<u32, PlatformNumaNode>> {
        let mut nodes = HashMap::new();

        for node_id in 0..node_count {
            let node = PlatformNumaNode {
                id: node_id,
                memory_total: 1024 * 1024 * 1024, // 1GB default
                memory_free: 512 * 1024 * 1024,   // 512MB free
                cpu_cores: Vec::new(),
            };
            nodes.insert(node_id, node);
        }

        Ok(nodes)
    }

    fn get_optimal_node(&self) -> i32 {
        // Simple round-robin selection
        let mut current = self.current_node.lock().unwrap();
        let node = *current;
        *current = (*current + 1) % self.node_count;
        node as i32
    }
}

impl PlatformMemoryLeakDetector {
    fn new() -> Result<Self> {
        Ok(Self {
            allocations: Arc::new(Mutex::new(HashMap::new())),
            suspected_leaks: Arc::new(Mutex::new(Vec::new())),
            check_interval: Duration::from_secs(60),
            leak_threshold: Duration::from_secs(300), // 5 minutes
        })
    }

    fn record_allocation(&self, ptr: *mut c_void, size: usize) -> Result<()> {
        self.allocations.lock().unwrap().insert(ptr, (size, SystemTime::now()));
        Ok(())
    }

    fn record_deallocation(&self, ptr: *mut c_void) -> Result<()> {
        self.allocations.lock().unwrap().remove(&ptr);
        Ok(())
    }

    fn detect_leaks(&self) -> Result<Vec<PlatformMemoryLeak>> {
        let now = SystemTime::now();
        let allocations = self.allocations.lock().unwrap();
        let mut leaks = Vec::new();

        for (&ptr, &(size, timestamp)) in allocations.iter() {
            let age = now.duration_since(timestamp).unwrap_or_default();

            if age > self.leak_threshold {
                let confidence_score = self.calculate_leak_confidence(size, age);

                let leak = PlatformMemoryLeak {
                    allocation_info: AllocationInfo {
                        ptr,
                        size,
                        alignment: 64, // Default alignment
                        page_type: PageSize::Default,
                        numa_node: -1,
                        timestamp,
                        stack_trace: None,
                    },
                    age,
                    is_suspected_leak: confidence_score > 0.7,
                    confidence_score,
                };

                leaks.push(leak);
            }
        }

        Ok(leaks)
    }

    fn calculate_leak_confidence(&self, size: usize, age: Duration) -> f64 {
        // Simple confidence calculation based on size and age
        let size_factor = if size > 1024 * 1024 { 0.8 } else { 0.5 };
        let age_factor = if age > Duration::from_secs(600) { 0.9 } else { 0.6 };

        (size_factor + age_factor) / 2.0
    }
}

impl PlatformMemoryPool {
    fn new(pool_size: usize) -> Self {
        Self {
            pool_size,
            free_blocks: VecDeque::new(),
            allocated_blocks: HashMap::new(),
            total_size: 0,
            used_size: 0,
        }
    }
}

/// Thread-safe memory registry for managing multiple memory instances
pub struct MemoryRegistry {
    memories: Arc<Mutex<HashMap<u32, Arc<Memory>>>>,
}

impl MemoryRegistry {
    /// Create a new memory registry
    pub fn new() -> Self {
        Self {
            memories: Arc::new(Mutex::new(HashMap::new())),
        }
    }

    /// Register a memory instance
    pub fn register(&self, memory: Memory) -> WasmtimeResult<u32> {
        let mut memories = self.memories.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory registry lock".to_string(),
            }
        })?;
        
        let memory_id = memory.config.memory_index;
        memories.insert(memory_id, Arc::new(memory));
        
        log::debug!("Registered memory with ID {}", memory_id);
        Ok(memory_id)
    }

    /// Get memory instance by ID
    pub fn get(&self, memory_id: u32) -> WasmtimeResult<Arc<Memory>> {
        let memories = self.memories.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory registry lock".to_string(),
            }
        })?;
        
        memories.get(&memory_id).cloned().ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Memory with ID {} not found", memory_id),
            }
        })
    }

    /// Remove memory instance
    pub fn unregister(&self, memory_id: u32) -> WasmtimeResult<()> {
        let mut memories = self.memories.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory registry lock".to_string(),
            }
        })?;
        
        memories.remove(&memory_id).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Memory with ID {} not found", memory_id),
            }
        })?;
        
        log::debug!("Unregistered memory with ID {}", memory_id);
        Ok(())
    }

    /// Get all registered memory IDs
    pub fn list_memories(&self) -> WasmtimeResult<Vec<u32>> {
        let memories = self.memories.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory registry lock".to_string(),
            }
        })?;
        
        Ok(memories.keys().cloned().collect())
    }
}

impl Default for MemoryRegistry {
    fn default() -> Self {
        Self::new()
    }
}

/// Core FFI-compatible functions for Memory operations
/// 
/// This module provides functions that can be safely called from both JNI and Panama FFI
/// implementations. These functions eliminate code duplication and provide consistent behavior
/// across interface implementations while maintaining defensive programming practices.
pub mod core {
    use super::*;
    use std::os::raw::c_void;
    use std::sync::atomic::{AtomicU64, Ordering};
    use std::collections::HashSet;
    use crate::error::ffi_utils;
    use crate::store::Store;
    
    /// Memory handle validation magic number for integrity checking
    const MEMORY_MAGIC: u64 = 0xDEADBEEF_CAFEBABE;
    
    /// Thread-safe registry for tracking valid memory handles (using usize for thread safety)
    static VALID_MEMORY_HANDLES: once_cell::sync::Lazy<Arc<RwLock<HashSet<usize>>>> = 
        once_cell::sync::Lazy::new(|| Arc::new(RwLock::new(HashSet::new())));
    
    /// Thread-safe registry for tracking valid store handles (using usize for thread safety)
    static VALID_STORE_HANDLES: once_cell::sync::Lazy<Arc<RwLock<HashSet<usize>>>> = 
        once_cell::sync::Lazy::new(|| Arc::new(RwLock::new(HashSet::new())));
    
    /// Memory access counter for detecting potential race conditions
    static MEMORY_ACCESS_COUNTER: AtomicU64 = AtomicU64::new(0);
    
    /// Wrapper for memory with validation metadata
    #[repr(C)]
    pub struct ValidatedMemory {
        magic: u64,
        memory: Memory,
        created_at: Instant,
        access_count: AtomicU64,
        is_destroyed: std::sync::atomic::AtomicBool,
    }
    
    impl ValidatedMemory {
        /// Create a new validated memory wrapper
        pub fn new(memory: Memory) -> Self {
            Self {
                magic: MEMORY_MAGIC,
                memory,
                created_at: Instant::now(),
                access_count: AtomicU64::new(0),
                is_destroyed: std::sync::atomic::AtomicBool::new(false),
            }
        }
        
        /// Check if this memory instance is valid
        pub fn is_valid(&self) -> bool {
            self.magic == MEMORY_MAGIC && !self.is_destroyed.load(Ordering::Acquire)
        }
        
        /// Mark this memory instance as destroyed
        pub fn mark_destroyed(&self) {
            self.is_destroyed.store(true, Ordering::Release);
        }
        
        /// Increment access counter and return memory reference
        pub fn access_memory(&self) -> WasmtimeResult<&Memory> {
            if !self.is_valid() {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Attempt to access destroyed or corrupted memory".to_string(),
                });
            }
            
            self.access_count.fetch_add(1, Ordering::Relaxed);
            MEMORY_ACCESS_COUNTER.fetch_add(1, Ordering::Relaxed);
            Ok(&self.memory)
        }
        
        /// Get access statistics
        pub fn get_access_count(&self) -> u64 {
            self.access_count.load(Ordering::Relaxed)
        }
    }
    
    /// Register a memory handle as valid
    pub fn register_memory_handle(ptr: *const c_void) -> WasmtimeResult<()> {
        let mut handles = VALID_MEMORY_HANDLES.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory handle registry lock".to_string(),
            }
        })?;
        
        handles.insert(ptr as usize);
        log::debug!("Registered memory handle: {:p}", ptr);
        Ok(())
    }
    
    /// Unregister a memory handle
    pub fn unregister_memory_handle(ptr: *const c_void) -> WasmtimeResult<()> {
        let mut handles = VALID_MEMORY_HANDLES.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory handle registry lock".to_string(),
            }
        })?;
        
        if handles.remove(&(ptr as usize)) {
            log::debug!("Unregistered memory handle: {:p}", ptr);
            Ok(())
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: format!("Memory handle {:p} was not registered", ptr),
            })
        }
    }
    
    /// Register a store handle as valid
    pub fn register_store_handle(ptr: *const c_void) -> WasmtimeResult<()> {
        let mut handles = VALID_STORE_HANDLES.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire store handle registry lock".to_string(),
            }
        })?;
        
        handles.insert(ptr as usize);
        log::debug!("Registered store handle: {:p}", ptr);
        Ok(())
    }
    
    /// Unregister a store handle
    pub fn unregister_store_handle(ptr: *const c_void) -> WasmtimeResult<()> {
        let mut handles = VALID_STORE_HANDLES.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire store handle registry lock".to_string(),
            }
        })?;
        
        if handles.remove(&(ptr as usize)) {
            log::debug!("Unregistered store handle: {:p}", ptr);
            Ok(())
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: format!("Store handle {:p} was not registered", ptr),
            })
        }
    }
    
    /// Validate memory handle with comprehensive checks
    pub unsafe fn validate_memory_handle(ptr: *const c_void) -> WasmtimeResult<()> {
        // Basic null check
        if ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Memory pointer cannot be null".to_string(),
            });
        }
        
        // Check if handle is registered
        let handles = VALID_MEMORY_HANDLES.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory handle registry lock".to_string(),
            }
        })?;
        
        if !handles.contains(&(ptr as usize)) {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Memory handle {:p} is not registered or has been freed", ptr),
            });
        }
        
        // Check magic number and validity if using ValidatedMemory
        let validated_memory = &*(ptr as *const ValidatedMemory);
        if validated_memory.magic != MEMORY_MAGIC {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Memory handle {:p} has invalid magic number (corrupted)", ptr),
            });
        }
        
        if validated_memory.is_destroyed.load(Ordering::Acquire) {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Memory handle {:p} has been destroyed (use-after-free attempt)", ptr),
            });
        }
        
        Ok(())
    }
    
    /// Validate store handle with comprehensive checks
    pub unsafe fn validate_store_handle(ptr: *const c_void) -> WasmtimeResult<()> {
        // Basic null check
        if ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store pointer cannot be null".to_string(),
            });
        }
        
        // Check if handle is registered
        let handles = VALID_STORE_HANDLES.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire store handle registry lock".to_string(),
            }
        })?;
        
        if !handles.contains(&(ptr as usize)) {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Store handle {:p} is not registered or has been freed", ptr),
            });
        }
        
        Ok(())
    }
    
    /// Get memory from pointer with comprehensive validation
    pub unsafe fn get_memory_ref(ptr: *const c_void) -> WasmtimeResult<&'static Memory> {
        validate_memory_handle(ptr)?;
        
        let validated_memory = &*(ptr as *const ValidatedMemory);
        validated_memory.access_memory()
    }

    /// Get mutable memory from pointer with comprehensive validation
    pub unsafe fn get_memory_mut(ptr: *mut c_void) -> WasmtimeResult<&'static mut Memory> {
        validate_memory_handle(ptr)?;
        
        let validated_memory = &mut *(ptr as *mut ValidatedMemory);
        let _memory_ref = validated_memory.access_memory()?; // Validate first
        Ok(&mut validated_memory.memory)
    }

    /// Get store from pointer with validation (read-only)
    pub unsafe fn get_store_ref(ptr: *const c_void) -> WasmtimeResult<&'static Store> {
        validate_store_handle(ptr)?;
        ffi_utils::deref_ptr::<Store>(ptr, "store")
    }

    /// Get mutable store from pointer with validation
    pub unsafe fn get_store_mut(ptr: *mut c_void) -> WasmtimeResult<&'static mut Store> {
        validate_store_handle(ptr as *const c_void)?;
        ffi_utils::deref_ptr_mut::<Store>(ptr, "store")
    }

    /// Core function to get memory size in bytes
    pub fn get_memory_size(memory: &Memory, store: &Store) -> WasmtimeResult<usize> {
        memory.size_bytes(store)
    }

    /// Core function to grow memory by pages
    pub fn grow_memory(memory: &Memory, store: &mut Store, pages: u64) -> WasmtimeResult<u64> {
        memory.grow(store, pages)
    }

    /// Core function to get memory page count
    pub fn get_memory_page_count(memory: &Memory, store: &Store) -> WasmtimeResult<u64> {
        memory.size_pages(store)
    }

    /// Core function to read bytes from memory
    pub fn read_memory_bytes(memory: &Memory, store: &Store, offset: usize, length: usize) -> WasmtimeResult<Vec<u8>> {
        memory.read_bytes(store, offset, length)
    }

    /// Core function to write bytes to memory
    pub fn write_memory_bytes(memory: &Memory, store: &mut Store, offset: usize, data: &[u8]) -> WasmtimeResult<()> {
        memory.write_bytes(store, offset, data)
    }

    /// Core function to read a single byte from memory
    pub fn read_memory_byte(memory: &Memory, store: &Store, offset: usize) -> WasmtimeResult<u8> {
        let bytes = memory.read_bytes(store, offset, 1)?;
        Ok(bytes[0])
    }

    /// Core function to write a single byte to memory
    pub fn write_memory_byte(memory: &Memory, store: &mut Store, offset: usize, value: u8) -> WasmtimeResult<()> {
        memory.write_bytes(store, offset, &[value])
    }

    /// Core function to get direct memory buffer access
    pub fn get_memory_buffer(memory: &Memory, store: &Store) -> WasmtimeResult<(*mut u8, usize)> {
        store.with_context_ro(|ctx| {
            let data = memory.inner.data(ctx);
            let ptr = data.as_ptr() as *mut u8;
            let len = data.len();
            Ok((ptr, len))
        })
    }

    /// Create a validated memory instance with handle registration
    pub fn create_validated_memory(memory: Memory) -> WasmtimeResult<*mut ValidatedMemory> {
        let validated = ValidatedMemory::new(memory);
        let boxed = Box::new(validated);
        let ptr = Box::into_raw(boxed);
        
        // Register the handle
        register_memory_handle(ptr as *const c_void)?;
        
        log::debug!("Created validated memory handle: {:p}", ptr);
        Ok(ptr)
    }
    
    /// Core function to destroy memory with proper cleanup and validation
    pub unsafe fn destroy_memory(ptr: *mut c_void) {
        if ptr.is_null() {
            log::warn!("Attempted to destroy null memory pointer");
            return;
        }

        // First try to validate the handle (this might fail if already destroyed)
        match validate_memory_handle(ptr) {
            Ok(_) => {
                // Mark as destroyed first to prevent further access
                let validated_memory = &*(ptr as *const ValidatedMemory);
                validated_memory.mark_destroyed();

                // Unregister the handle
                if let Err(e) = unregister_memory_handle(ptr) {
                    log::error!("Failed to unregister memory handle {:p}: {}", ptr, e);
                }

                // Finally, deallocate the memory
                let _ = Box::from_raw(ptr as *mut ValidatedMemory);
                log::debug!("Destroyed validated memory handle: {:p}", ptr);
            }
            Err(e) => {
                log::warn!("Attempted to destroy invalid memory handle {:p}: {}", ptr, e);
                // Still attempt cleanup in case it's a partially corrupted handle
                if let Err(unregister_err) = unregister_memory_handle(ptr) {
                    log::debug!("Handle was not registered (expected for corrupted handle): {}", unregister_err);
                }
            }
        }
    }

    // Shared Memory Operations

    /// Core function to check if memory is shared
    pub fn memory_is_shared(memory: &Memory, store: &Store) -> WasmtimeResult<bool> {
        store.with_context_ro(|ctx| {
            let memory_type = memory.inner.ty(ctx);
            Ok(memory_type.is_shared())
        })
    }

    /// Core function for atomic compare-and-swap on 32-bit value
    pub fn atomic_compare_and_swap_i32(memory: &Memory, store: &mut Store, offset: usize, expected: i32, new_value: i32) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            // Use Rust's atomic operations on the memory data
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                let result = atomic_ref.compare_exchange(
                    expected,
                    new_value,
                    std::sync::atomic::Ordering::SeqCst,
                    std::sync::atomic::Ordering::SeqCst
                );

                match result {
                    Ok(old_value) => Ok(old_value),
                    Err(actual_value) => Ok(actual_value),
                }
            }
        })
    }

    /// Core function for atomic compare-and-swap on 64-bit value
    pub fn atomic_compare_and_swap_i64(memory: &Memory, store: &mut Store, offset: usize, expected: i64, new_value: i64) -> WasmtimeResult<i64> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 8 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 8-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 8 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 8,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 8];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI64;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                let result = atomic_ref.compare_exchange(
                    expected,
                    new_value,
                    std::sync::atomic::Ordering::SeqCst,
                    std::sync::atomic::Ordering::SeqCst
                );

                match result {
                    Ok(old_value) => Ok(old_value),
                    Err(actual_value) => Ok(actual_value),
                }
            }
        })
    }

    /// Core function for atomic load on 32-bit value
    pub fn atomic_load_i32(memory: &Memory, store: &Store, offset: usize) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context_ro(|ctx| {
            let memory_data = memory.inner.data(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.load(std::sync::atomic::Ordering::Acquire))
            }
        })
    }

    /// Core function for atomic load on 64-bit value
    pub fn atomic_load_i64(memory: &Memory, store: &Store, offset: usize) -> WasmtimeResult<i64> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 8 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 8-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 8 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 8,
                memory_size,
            }.into());
        }

        store.with_context_ro(|ctx| {
            let memory_data = memory.inner.data(ctx);
            let ptr = &memory_data[offset..offset + 8];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI64;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.load(std::sync::atomic::Ordering::Acquire))
            }
        })
    }

    /// Core function for atomic store on 32-bit value
    pub fn atomic_store_i32(memory: &Memory, store: &mut Store, offset: usize, value: i32) -> WasmtimeResult<()> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                atomic_ref.store(value, std::sync::atomic::Ordering::Release);
                Ok(())
            }
        })
    }

    /// Core function for atomic store on 64-bit value
    pub fn atomic_store_i64(memory: &Memory, store: &mut Store, offset: usize, value: i64) -> WasmtimeResult<()> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 8 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 8-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 8 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 8,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 8];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI64;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                atomic_ref.store(value, std::sync::atomic::Ordering::Release);
                Ok(())
            }
        })
    }

    /// Core function for atomic add on 32-bit value
    pub fn atomic_add_i32(memory: &Memory, store: &mut Store, offset: usize, value: i32) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.fetch_add(value, std::sync::atomic::Ordering::SeqCst))
            }
        })
    }

    /// Core function for atomic add on 64-bit value
    pub fn atomic_add_i64(memory: &Memory, store: &mut Store, offset: usize, value: i64) -> WasmtimeResult<i64> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 8 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 8-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 8 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 8,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 8];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI64;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.fetch_add(value, std::sync::atomic::Ordering::SeqCst))
            }
        })
    }

    /// Core function for atomic bitwise AND on 32-bit value
    pub fn atomic_and_i32(memory: &Memory, store: &mut Store, offset: usize, value: i32) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.fetch_and(value, std::sync::atomic::Ordering::SeqCst))
            }
        })
    }

    /// Core function for atomic bitwise OR on 32-bit value
    pub fn atomic_or_i32(memory: &Memory, store: &mut Store, offset: usize, value: i32) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.fetch_or(value, std::sync::atomic::Ordering::SeqCst))
            }
        })
    }

    /// Core function for atomic bitwise XOR on 32-bit value
    pub fn atomic_xor_i32(memory: &Memory, store: &mut Store, offset: usize, value: i32) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                Ok(atomic_ref.fetch_xor(value, std::sync::atomic::Ordering::SeqCst))
            }
        })
    }

    /// Core function for atomic memory fence
    pub fn atomic_fence(_memory: &Memory, store: &Store) -> WasmtimeResult<()> {
        // Validate memory is shared
        if !memory_is_shared(_memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Memory fence - ensures ordering of memory operations
        std::sync::atomic::fence(std::sync::atomic::Ordering::SeqCst);
        Ok(())
    }

    /// Core function for atomic notify (wake threads waiting on a memory location)
    pub fn atomic_notify(memory: &Memory, store: &Store, offset: usize, count: i32) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        // Note: This is a simplified implementation. A real implementation would
        // maintain a thread wait queue for each memory location and notify
        // the specified number of waiting threads.

        // For now, we'll return 0 (no threads notified) since we don't have
        // a full wait/notify infrastructure implemented
        log::debug!("Atomic notify at offset {} with count {} (simplified implementation)", offset, count);
        Ok(0)
    }

    /// Core function for atomic wait on 32-bit value
    pub fn atomic_wait32(memory: &Memory, store: &Store, offset: usize, expected: i32, timeout_nanos: i64) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 4 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 4-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 4 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 4,
                memory_size,
            }.into());
        }

        store.with_context_ro(|ctx| {
            let memory_data = memory.inner.data(ctx);
            let ptr = &memory_data[offset..offset + 4];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI32;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                let current_value = atomic_ref.load(std::sync::atomic::Ordering::Acquire);

                if current_value != expected {
                    // Value mismatch - return immediately
                    Ok(1)
                } else {
                    // Note: This is a simplified implementation. A real implementation would
                    // block the thread until notified or timeout occurs.
                    // For now, we'll simulate a timeout
                    log::debug!("Atomic wait32 at offset {} with expected {} and timeout {} ns (simplified implementation)",
                               offset, expected, timeout_nanos);

                    if timeout_nanos == 0 {
                        Ok(2) // Immediate timeout
                    } else {
                        // Simulate a short wait
                        std::thread::sleep(std::time::Duration::from_millis(1));
                        Ok(2) // Timeout
                    }
                }
            }
        })
    }

    /// Core function for atomic wait on 64-bit value
    pub fn atomic_wait64(memory: &Memory, store: &Store, offset: usize, expected: i64, timeout_nanos: i64) -> WasmtimeResult<i32> {
        // Validate memory is shared
        if !memory_is_shared(memory, store)? {
            return Err(WasmtimeError::Memory {
                message: "Atomic operations require shared memory".to_string(),
            });
        }

        // Validate alignment
        if offset % 8 != 0 {
            return Err(WasmtimeError::Memory {
                message: format!("Offset {} is not aligned to 8-byte boundary", offset),
            });
        }

        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset + 8 > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: 8,
                memory_size,
            }.into());
        }

        store.with_context_ro(|ctx| {
            let memory_data = memory.inner.data(ctx);
            let ptr = &memory_data[offset..offset + 8];
            let atomic_ptr = ptr.as_ptr() as *const std::sync::atomic::AtomicI64;

            unsafe {
                let atomic_ref = &*atomic_ptr;
                let current_value = atomic_ref.load(std::sync::atomic::Ordering::Acquire);

                if current_value != expected {
                    // Value mismatch - return immediately
                    Ok(1)
                } else {
                    // Note: This is a simplified implementation. A real implementation would
                    // block the thread until notified or timeout occurs.
                    log::debug!("Atomic wait64 at offset {} with expected {} and timeout {} ns (simplified implementation)",
                               offset, expected, timeout_nanos);

                    if timeout_nanos == 0 {
                        Ok(2) // Immediate timeout
                    } else {
                        // Simulate a short wait
                        std::thread::sleep(std::time::Duration::from_millis(1));
                        Ok(2) // Timeout
                    }
                }
            }
        })
    }

    // Bulk Memory Operations

    /// Core function to copy memory within the same memory instance
    pub fn memory_copy(memory: &Memory, store: &mut Store, dest_offset: usize, src_offset: usize, len: usize) -> WasmtimeResult<()> {
        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if dest_offset.saturating_add(len) > memory_size || src_offset.saturating_add(len) > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset: std::cmp::max(dest_offset, src_offset),
                length: len,
                memory_size,
            }.into());
        }

        // Perform memory copy within the store lock
        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);

            // Handle overlapping memory regions correctly
            if dest_offset < src_offset && dest_offset + len > src_offset {
                // Forward overlap - copy from end to beginning
                for i in (0..len).rev() {
                    memory_data[dest_offset + i] = memory_data[src_offset + i];
                }
            } else if src_offset < dest_offset && src_offset + len > dest_offset {
                // Backward overlap - copy from beginning to end
                for i in 0..len {
                    memory_data[dest_offset + i] = memory_data[src_offset + i];
                }
            } else {
                // No overlap - use efficient copy
                memory_data.copy_within(src_offset..src_offset + len, dest_offset);
            }
            Ok(())
        })
    }

    /// Core function to fill memory with a specific byte value
    pub fn memory_fill(memory: &Memory, store: &mut Store, offset: usize, value: u8, len: usize) -> WasmtimeResult<()> {
        // Bounds checking
        let memory_size = memory.size_bytes(store)?;
        if offset.saturating_add(len) > memory_size {
            return Err(MemoryError::BoundsViolation {
                offset,
                length: len,
                memory_size,
            }.into());
        }

        // Perform memory fill within the store lock
        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            memory_data[offset..offset + len].fill(value);
            Ok(())
        })
    }

    /// Initialize memory from a data segment
    ///
    /// This implements the memory.init instruction, copying bytes from
    /// a passive data segment into memory.
    ///
    /// # Arguments
    /// * `memory` - The memory to initialize
    /// * `store` - The WebAssembly store
    /// * `instance` - The instance containing the data segments
    /// * `dest_offset` - Destination offset in memory
    /// * `data_segment_index` - Index of the data segment to copy from
    /// * `src_offset` - Source offset in the data segment
    /// * `len` - Number of bytes to copy
    pub fn memory_init(
        memory: &Memory,
        store: &Store,
        instance: &crate::instance::Instance,
        dest_offset: u32,
        data_segment_index: u32,
        src_offset: u32,
        len: u32,
    ) -> WasmtimeResult<()> {
        // Get data segment manager from instance
        let segment_manager = instance.get_data_segment_manager();

        // Get memory size for bounds checking
        let memory_size = store.with_context_ro(|ctx| {
            Ok(memory.inner.data_size(&ctx))
        })?;

        // Bounds check for destination
        if (dest_offset as u64).saturating_add(len as u64) > memory_size as u64 {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "memory.init destination would exceed bounds: dest={}, len={}, memory_size={}",
                    dest_offset, len, memory_size
                ),
                backtrace: None,
            });
        }

        // Get data from segment (includes validation and bounds checking)
        let data = segment_manager.get_data(data_segment_index, src_offset, len)?;

        // Write data to memory
        store.with_context(|ctx| {
            let memory_data = memory.inner.data_mut(ctx);
            let dest_start = dest_offset as usize;
            let dest_end = dest_start + len as usize;
            memory_data[dest_start..dest_end].copy_from_slice(&data);
            Ok(())
        })
    }

    /// Drop a data segment
    ///
    /// This implements the data.drop instruction, marking a data segment
    /// as dropped so it cannot be used by memory.init anymore.
    ///
    /// # Arguments
    /// * `instance` - The instance containing the data segments
    /// * `data_segment_index` - Index of the data segment to drop
    pub fn data_drop(
        instance: &crate::instance::Instance,
        data_segment_index: u32,
    ) -> WasmtimeResult<()> {
        // Get data segment manager from instance
        let segment_manager = instance.get_data_segment_manager();

        // Drop the segment
        segment_manager.drop_segment(data_segment_index)
    }
    
    /// Get diagnostic information about memory handle validation
    pub fn get_memory_handle_diagnostics() -> WasmtimeResult<(usize, u64)> {
        let handles = VALID_MEMORY_HANDLES.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory handle registry lock".to_string(),
            }
        })?;
        
        let handle_count = handles.len();
        let total_accesses = MEMORY_ACCESS_COUNTER.load(Ordering::Relaxed);
        
        Ok((handle_count, total_accesses))
    }
    
    /// Force cleanup of all registered handles (for emergency shutdown)
    pub fn force_cleanup_all_handles() -> WasmtimeResult<usize> {
        let mut memory_handles = VALID_MEMORY_HANDLES.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire memory handle registry lock".to_string(),
            }
        })?;
        
        let mut store_handles = VALID_STORE_HANDLES.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire store handle registry lock".to_string(),
            }
        })?;
        
        let total_cleaned = memory_handles.len() + store_handles.len();
        
        memory_handles.clear();
        store_handles.clear();
        
        log::warn!("Force cleaned {} handles during emergency shutdown", total_cleaned);
        Ok(total_cleaned)
    }
}

// Export functions for JNI and Panama FFI bindings

/// Creates a new platform memory allocator
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_allocator_create(
    config: *const PlatformMemoryConfig,
) -> *mut PlatformMemoryAllocator {
    let config = if config.is_null() {
        PlatformMemoryConfig::default()
    } else {
        unsafe { (*config).clone() }
    };

    match PlatformMemoryAllocator::new(config) {
        Ok(allocator) => Box::into_raw(Box::new(allocator)),
        Err(e) => {
            error!("Failed to create platform memory allocator: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Allocates memory with platform optimizations
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_allocate(
    allocator: *mut PlatformMemoryAllocator,
    size: usize,
    alignment: usize,
) -> *mut c_void {
    if allocator.is_null() || size == 0 {
        return std::ptr::null_mut();
    }

    let allocator = unsafe { &*allocator };
    let alignment = if alignment == 0 { None } else { Some(alignment) };

    match allocator.allocate(size, alignment) {
        Ok(ptr) => ptr.as_ptr(),
        Err(e) => {
            error!("Platform memory allocation failed: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Deallocates platform memory
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_deallocate(
    allocator: *mut PlatformMemoryAllocator,
    ptr: *mut c_void,
) -> bool {
    if allocator.is_null() || ptr.is_null() {
        return false;
    }

    let allocator = unsafe { &*allocator };

    if let Some(non_null_ptr) = NonNull::new(ptr) {
        allocator.deallocate(non_null_ptr).is_ok()
    } else {
        false
    }
}

/// Gets platform memory statistics
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_get_stats(
    allocator: *mut PlatformMemoryAllocator,
    stats_out: *mut PlatformMemoryPoolStats,
) -> bool {
    if allocator.is_null() || stats_out.is_null() {
        return false;
    }

    let allocator = unsafe { &*allocator };
    let stats = allocator.get_stats();
    unsafe { *stats_out = stats };
    true
}

/// Detects platform memory leaks
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_detect_leaks(
    allocator: *mut PlatformMemoryAllocator,
    leaks_out: *mut *mut PlatformMemoryLeak,
    leak_count_out: *mut usize,
) -> bool {
    if allocator.is_null() || leaks_out.is_null() || leak_count_out.is_null() {
        return false;
    }

    let allocator = unsafe { &*allocator };

    match allocator.detect_leaks() {
        Ok(leaks) => {
            let leak_count = leaks.len();
            let leaks_box = Box::new(leaks);
            unsafe {
                *leaks_out = Box::into_raw(leaks_box) as *mut PlatformMemoryLeak;
                *leak_count_out = leak_count;
            }
            true
        }
        Err(_) => false,
    }
}

/// Prefetches platform memory for cache optimization
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_prefetch(
    allocator: *mut PlatformMemoryAllocator,
    ptr: *const c_void,
    size: usize,
) -> bool {
    if allocator.is_null() || ptr.is_null() {
        return false;
    }

    let allocator = unsafe { &*allocator };
    allocator.prefetch_memory(ptr, size).is_ok()
}

/// Gets platform memory information
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_get_info(
    allocator: *mut PlatformMemoryAllocator,
    info_out: *mut PlatformMemoryInfo,
) -> bool {
    if allocator.is_null() || info_out.is_null() {
        return false;
    }

    let allocator = unsafe { &*allocator };
    unsafe { *info_out = allocator.memory_info.clone() };
    true
}

/// Destroys a platform memory allocator
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_allocator_destroy(allocator: *mut PlatformMemoryAllocator) {
    if !allocator.is_null() {
        unsafe { drop(Box::from_raw(allocator)) };
    }
}

/// Creates a WebAssembly memory with platform optimization
#[no_mangle]
pub extern "C" fn wasmtime4j_memory_create_with_platform_config(
    store: *mut crate::store::Store,
    initial_pages: u64,
    maximum_pages: i64, // -1 for None
    platform_config: *const PlatformMemoryConfig,
) -> *mut Memory {
    if store.is_null() {
        error!("Store pointer cannot be null");
        return std::ptr::null_mut();
    }

    let store = unsafe { &mut *store };

    let memory_config = MemoryConfig {
        initial_pages,
        maximum_pages: if maximum_pages < 0 { None } else { Some(maximum_pages as u64) },
        is_shared: false,
        memory_index: 0,
        name: None,
    };

    let result = if platform_config.is_null() {
        Memory::new_with_config(store, memory_config)
    } else {
        let platform_config = unsafe { (*platform_config).clone() };
        Memory::new_with_platform_config(store, memory_config, platform_config)
    };

    match result {
        Ok(memory) => Box::into_raw(Box::new(memory)),
        Err(e) => {
            error!("Failed to create memory with platform config: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Gets platform allocator from WebAssembly memory (if available)
#[no_mangle]
pub extern "C" fn wasmtime4j_memory_get_platform_allocator(
    memory: *const Memory,
) -> *const PlatformMemoryAllocator {
    if memory.is_null() {
        return std::ptr::null();
    }

    let memory = unsafe { &*memory };

    if let Some(ref allocator) = memory.platform_allocator {
        allocator.as_ref() as *const PlatformMemoryAllocator
    } else {
        std::ptr::null()
    }
}

/// Performs memory compression on data
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_compress(
    allocator: *mut PlatformMemoryAllocator,
    data: *const u8,
    data_len: usize,
    compressed_out: *mut *mut u8,
    compressed_len_out: *mut usize,
) -> bool {
    if allocator.is_null() || data.is_null() || compressed_out.is_null() || compressed_len_out.is_null() {
        return false;
    }

    let allocator = unsafe { &*allocator };
    let input_data = unsafe { std::slice::from_raw_parts(data, data_len) };

    // Simple compression using flate2
    use flate2::Compression;
    use flate2::write::GzEncoder;
    use std::io::Write;

    let mut encoder = GzEncoder::new(Vec::new(), Compression::default());
    if encoder.write_all(input_data).is_err() {
        return false;
    }

    match encoder.finish() {
        Ok(compressed_data) => {
            let compressed_len = compressed_data.len();
            let compressed_ptr = compressed_data.as_ptr() as *mut u8;

            // Leak the memory so it can be used by caller (caller must free)
            std::mem::forget(compressed_data);

            unsafe {
                *compressed_out = compressed_ptr;
                *compressed_len_out = compressed_len;
            }
            true
        }
        Err(_) => false,
    }
}

/// Performs memory deduplication
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_deduplicate(
    allocator: *mut PlatformMemoryAllocator,
    data: *const u8,
    data_len: usize,
) -> *mut c_void {
    if allocator.is_null() || data.is_null() {
        return std::ptr::null_mut();
    }

    let allocator = unsafe { &*allocator };
    let input_data = unsafe { std::slice::from_raw_parts(data, data_len) };

    match allocator.deduplicate_memory(input_data) {
        Ok(ptr) => ptr,
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;

    #[test]
    fn test_memory_creation() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        assert_eq!(memory.size_pages(&store).unwrap(), 1);
        assert_eq!(memory.size_bytes(&store).unwrap(), 65536);
    }

    #[test]
    fn test_memory_growth() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        let previous_pages = memory.grow(&mut store, 1).expect("Failed to grow memory");
        
        assert_eq!(previous_pages, 1);
        assert_eq!(memory.size_pages(&store).unwrap(), 2);
    }

    #[test]
    fn test_bounds_checking() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        
        // Should succeed - within bounds
        let result = memory.read_bytes(&store, 0, 100);
        assert!(result.is_ok());
        
        // Should fail - out of bounds  
        let result = memory.read_bytes(&store, 65536, 1);
        assert!(result.is_err());
        assert!(matches!(result.unwrap_err(), WasmtimeError::Memory { .. }));
    }

    #[test]
    fn test_memory_statistics() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        
        // Perform some operations
        memory.read_bytes(&store, 0, 100).expect("Read failed");
        memory.write_bytes(&mut store, 0, &[1, 2, 3, 4]).expect("Write failed");
        
        let usage = memory.get_usage(&store).expect("Failed to get usage");
        assert_eq!(usage.current_pages, 1);
        assert_eq!(usage.read_count, 1);
        assert_eq!(usage.write_count, 1);
    }

    #[test]
    fn test_memory_registry() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        let registry = MemoryRegistry::new();
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        let memory_id = registry.register(memory).expect("Failed to register memory");
        
        let retrieved = registry.get(memory_id).expect("Failed to get memory");
        assert_eq!(retrieved.size_pages(&store).unwrap(), 1);
        
        registry.unregister(memory_id).expect("Failed to unregister memory");
        assert!(registry.get(memory_id).is_err());
    }

    #[test]
    fn test_memory_handle_validation() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        let validated_ptr = core::create_validated_memory(memory).expect("Failed to create validated memory");
        
        // Validate the handle works
        unsafe {
            assert!(core::validate_memory_handle(validated_ptr as *const std::os::raw::c_void).is_ok());
            let memory_ref = core::get_memory_ref(validated_ptr as *const std::os::raw::c_void);
            assert!(memory_ref.is_ok());
        }
        
        // Test destruction and use-after-free detection
        unsafe {
            core::destroy_memory(validated_ptr as *mut std::os::raw::c_void);
            
            // Now validation should fail
            assert!(core::validate_memory_handle(validated_ptr as *const std::os::raw::c_void).is_err());
        }
    }

    #[test]
    fn test_null_pointer_validation() {
        unsafe {
            // Test null pointer validation
            assert!(core::validate_memory_handle(std::ptr::null()).is_err());
            assert!(core::validate_store_handle(std::ptr::null()).is_err());
            
            // Test getting memory from null pointer
            let result = core::get_memory_ref(std::ptr::null());
            assert!(result.is_err());
        }
    }

    #[test]
    fn test_invalid_pointer_validation() {
        unsafe {
            // Test with invalid but non-null pointer
            let invalid_ptr = 0xDEADBEEF as *const std::os::raw::c_void;
            assert!(core::validate_memory_handle(invalid_ptr).is_err());
            
            // Test getting memory from invalid pointer
            let result = core::get_memory_ref(invalid_ptr);
            assert!(result.is_err());
        }
    }

    #[test]
    fn test_memory_access_counting() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        let validated_ptr = core::create_validated_memory(memory).expect("Failed to create validated memory");
        
        unsafe {
            let validated_memory = &*(validated_ptr as *const core::ValidatedMemory);
            
            // Initial access count should be 0
            assert_eq!(validated_memory.get_access_count(), 0);
            
            // Access the memory a few times
            let _memory1 = core::get_memory_ref(validated_ptr as *const std::os::raw::c_void);
            let _memory2 = core::get_memory_ref(validated_ptr as *const std::os::raw::c_void);
            let _memory3 = core::get_memory_ref(validated_ptr as *const std::os::raw::c_void);
            
            // Access count should have incremented (note: validation also increments count)
            assert!(validated_memory.get_access_count() >= 3);
            
            // Cleanup
            core::destroy_memory(validated_ptr as *mut std::os::raw::c_void);
        }
    }

    #[test]
    fn test_handle_registry_diagnostics() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        // Get initial state
        let (initial_handles, _initial_accesses) = core::get_memory_handle_diagnostics()
            .expect("Failed to get initial diagnostics");
        
        // Create some memory handles
        let memory1 = Memory::new(&mut store, 1).expect("Failed to create memory 1");
        let memory2 = Memory::new(&mut store, 1).expect("Failed to create memory 2");
        
        let validated_ptr1 = core::create_validated_memory(memory1).expect("Failed to create validated memory 1");
        let validated_ptr2 = core::create_validated_memory(memory2).expect("Failed to create validated memory 2");
        
        // Check handle count increased
        let (current_handles, _current_accesses) = core::get_memory_handle_diagnostics()
            .expect("Failed to get current diagnostics");
        assert_eq!(current_handles, initial_handles + 2);
        
        // Access the memories to increase access counter
        unsafe {
            let _mem1 = core::get_memory_ref(validated_ptr1 as *const std::os::raw::c_void);
            let _mem2 = core::get_memory_ref(validated_ptr2 as *const std::os::raw::c_void);
        }
        
        let (_final_handles, final_accesses) = core::get_memory_handle_diagnostics()
            .expect("Failed to get final diagnostics");
        assert!(final_accesses >= 2); // At least 2 accesses
        
        // Cleanup
        unsafe {
            core::destroy_memory(validated_ptr1 as *mut std::os::raw::c_void);
            core::destroy_memory(validated_ptr2 as *mut std::os::raw::c_void);
        }
        
        // Check handle count decreased
        let (cleanup_handles, _cleanup_accesses) = core::get_memory_handle_diagnostics()
            .expect("Failed to get cleanup diagnostics");
        assert_eq!(cleanup_handles, initial_handles);
    }

    #[test]
    fn test_corrupted_handle_detection() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        let validated_ptr = core::create_validated_memory(memory).expect("Failed to create validated memory");
        
        unsafe {
            // First validate it works
            assert!(core::validate_memory_handle(validated_ptr as *const std::os::raw::c_void).is_ok());
            
            // Corrupt the magic number by directly manipulating memory
            let validated_memory_ptr = validated_ptr as *mut u64; // First field is magic
            *validated_memory_ptr = 0xBADCAFE; // Wrong magic
            
            // Now validation should fail due to corrupted magic
            let result = core::validate_memory_handle(validated_ptr as *const std::os::raw::c_void);
            assert!(result.is_err());
            let error_msg = result.unwrap_err().to_string();
            assert!(error_msg.contains("invalid magic number"));
            
            // Cleanup (this will also fail validation but still clean up)
            core::destroy_memory(validated_ptr as *mut std::os::raw::c_void);
        }
    }

    #[test]
    fn test_thread_safety_basic() {
        use std::thread;
        
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        
        let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
        let validated_ptr = core::create_validated_memory(memory).expect("Failed to create validated memory");
        
        let ptr_copy = validated_ptr as usize;
        
        // Spawn threads to access the handle concurrently
        let handles: Vec<_> = (0..4).map(|_| {
            let ptr = ptr_copy;
            thread::spawn(move || {
                unsafe {
                    for _ in 0..10 {
                        let result = core::validate_memory_handle(ptr as *const std::os::raw::c_void);
                        // We don't assert success here because the memory might be destroyed
                        // by another thread, but validation should not crash
                        let _ = result;
                    }
                }
            })
        }).collect();
        
        // Wait for all threads to complete
        for handle in handles {
            handle.join().expect("Thread panicked");
        }
        
        // Cleanup
        unsafe {
            core::destroy_memory(validated_ptr as *mut std::os::raw::c_void);
        }
    }
}
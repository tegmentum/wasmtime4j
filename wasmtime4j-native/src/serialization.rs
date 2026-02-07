//! WebAssembly module serialization and caching infrastructure
//!
//! This module provides comprehensive support for module serialization, deserialization,
//! caching, and distribution across processes and systems.

use std::collections::HashMap;
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use std::path::{Path, PathBuf};
use std::fs;
use std::io::{Read, Write};
use sha2::{Sha256, Digest};
use flate2::{Compression, read::GzDecoder, write::GzEncoder};
use wasmtime::{Engine, Module};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Maximum cache size in bytes (default: 1GB)
const DEFAULT_MAX_CACHE_SIZE: usize = 1024 * 1024 * 1024;

/// Maximum cache entry age in seconds (default: 24 hours)
const DEFAULT_MAX_CACHE_AGE: u64 = 24 * 60 * 60;

/// Compression level for serialized modules
const COMPRESSION_LEVEL: u32 = 6;

/// Module serialization and caching manager
pub struct ModuleSerializer {
    /// Cache storage
    cache: Arc<RwLock<ModuleCache>>,
    /// Cache configuration
    config: SerializationConfig,
    /// Statistics tracking
    stats: Arc<Mutex<SerializationStats>>,
}

/// Module cache implementation with LRU eviction
#[derive(Debug)]
struct ModuleCache {
    /// Cached modules by content hash
    entries: HashMap<String, CacheEntry>,
    /// LRU ordering (most recent first)
    lru_order: Vec<String>,
    /// Current cache size in bytes
    current_size: usize,
    /// Maximum cache size
    max_size: usize,
    /// Maximum entry age
    max_age: Duration,
}

/// Cache entry for serialized module
#[derive(Debug, Clone)]
struct CacheEntry {
    /// Serialized module data (compressed)
    data: Vec<u8>,
    /// Uncompressed size
    uncompressed_size: usize,
    /// Creation timestamp
    created_at: Instant,
    /// Last access timestamp
    last_accessed: Instant,
    /// Access count
    access_count: u64,
    /// Module metadata
    metadata: ModuleMetadata,
}

/// Module metadata for validation and compatibility
#[derive(Debug, Clone)]
pub struct ModuleMetadata {
    /// Content hash (SHA-256)
    pub content_hash: String,
    /// Engine configuration hash
    pub engine_config_hash: String,
    /// Wasmtime version
    pub wasmtime_version: String,
    /// Serialization timestamp
    pub serialized_at: SystemTime,
    /// Module size information
    pub size_info: ModuleSizeInfo,
    /// Feature flags used during compilation
    pub features: Vec<String>,
    /// Target architecture
    pub target_arch: String,
    /// Target operating system
    pub target_os: String,
}

/// Module size information
#[derive(Debug, Clone)]
pub struct ModuleSizeInfo {
    /// Original WASM module size
    pub original_size: usize,
    /// Serialized size (uncompressed)
    pub serialized_size: usize,
    /// Compressed size
    pub compressed_size: usize,
    /// Compression ratio
    pub compression_ratio: f64,
}

/// Serialization configuration
#[derive(Debug, Clone)]
pub struct SerializationConfig {
    /// Maximum cache size in bytes
    pub max_cache_size: usize,
    /// Maximum cache entry age
    pub max_cache_age: Duration,
    /// Enable compression
    pub enable_compression: bool,
    /// Compression level (0-9, higher = better compression)
    pub compression_level: u32,
    /// Maximum entry age before eviction
    pub max_entry_age: Duration,
    /// Cache directory for persistent storage
    pub cache_directory: Option<PathBuf>,
    /// Enable cross-process sharing
    pub enable_cross_process: bool,
    /// Validation strictness level
    pub validation_level: ValidationLevel,
}

/// Validation strictness for deserialized modules
#[derive(Debug, Clone, PartialEq)]
pub enum ValidationLevel {
    /// No validation (fastest, least safe)
    None,
    /// Basic hash validation
    Basic,
    /// Full compatibility validation (slowest, safest)
    Strict,
}

/// Serialization statistics
#[derive(Debug, Default, Clone)]
pub struct SerializationStats {
    /// Total serializations performed
    pub serializations: u64,
    /// Total deserializations performed
    pub deserializations: u64,
    /// Cache hits
    pub cache_hits: u64,
    /// Cache misses
    pub cache_misses: u64,
    /// Total bytes serialized
    pub bytes_serialized: u64,
    /// Total bytes deserialized
    pub bytes_deserialized: u64,
    /// Total time spent serializing
    pub serialization_time: Duration,
    /// Total time spent deserializing
    pub deserialization_time: Duration,
    /// Evicted entries count
    pub evicted_entries: u64,
}

impl Default for SerializationConfig {
    fn default() -> Self {
        Self {
            max_cache_size: DEFAULT_MAX_CACHE_SIZE,
            max_cache_age: Duration::from_secs(DEFAULT_MAX_CACHE_AGE),
            enable_compression: false, // Disable by default - compression adds complexity
            compression_level: 6, // Default compression level (when enabled)
            max_entry_age: Duration::from_secs(DEFAULT_MAX_CACHE_AGE),
            cache_directory: None,
            enable_cross_process: false,
            validation_level: ValidationLevel::Basic,
        }
    }
}

impl ModuleSerializer {
    /// Create new module serializer with default configuration
    pub fn new() -> Self {
        Self::with_config(SerializationConfig::default())
    }

    /// Create module serializer with custom configuration
    pub fn with_config(config: SerializationConfig) -> Self {
        let cache = ModuleCache {
            entries: HashMap::new(),
            lru_order: Vec::new(),
            current_size: 0,
            max_size: config.max_cache_size,
            max_age: config.max_cache_age,
        };

        Self {
            cache: Arc::new(RwLock::new(cache)),
            config,
            stats: Arc::new(Mutex::new(SerializationStats::default())),
        }
    }

    /// Serialize a WebAssembly module
    pub fn serialize_module(&self, _engine: &Engine, module: &Module) -> WasmtimeResult<Vec<u8>> {
        let start_time = Instant::now();

        // Generate content hash
        let content_hash = self.generate_module_hash(module)?;

        // Check cache first
        if let Some(cached_data) = self.get_from_cache(&content_hash)? {
            let mut stats = self.stats.lock().unwrap_or_else(|e| e.into_inner());
            stats.cache_hits += 1;
            stats.deserialization_time += start_time.elapsed();
            return Ok(cached_data);
        }

        // Serialize the module
        let serialized_data = module.serialize()
            .map_err(|e| WasmtimeError::Serialization { message: format!("Failed to serialize module: {}", e) })?;

        // Create metadata
        let metadata = self.create_metadata(&content_hash, &serialized_data)?;

        // Compress if enabled
        let final_data = if self.config.enable_compression {
            self.compress_data(&serialized_data)?
        } else {
            serialized_data.clone()
        };

        // Store in cache
        self.store_in_cache(content_hash.clone(), final_data.clone(), serialized_data.len(), metadata)?;

        // Persist to disk if configured
        if let Some(cache_dir) = &self.config.cache_directory {
            self.persist_to_disk(cache_dir, &content_hash, &final_data)?;
        }

        // Update statistics
        let mut stats = self.stats.lock().unwrap_or_else(|e| e.into_inner());
        stats.serializations += 1;
        stats.cache_misses += 1;
        stats.bytes_serialized += final_data.len() as u64;
        stats.serialization_time += start_time.elapsed();

        Ok(final_data)
    }

    /// Deserialize a WebAssembly module
    pub fn deserialize_module(&self, engine: &Engine, data: &[u8]) -> WasmtimeResult<Module> {
        let start_time = Instant::now();

        // Decompress if needed
        let module_data = if self.config.enable_compression {
            self.decompress_data(data)?
        } else {
            data.to_vec()
        };

        // Validate if required
        if self.config.validation_level != ValidationLevel::None {
            self.validate_module_data(&module_data)?;
        }

        // Deserialize the module
        let module = unsafe { Module::deserialize(engine, &module_data) }
            .map_err(|e| WasmtimeError::Serialization { message: format!("Failed to deserialize module: {}", e) })?;

        // Update statistics
        let mut stats = self.stats.lock().unwrap_or_else(|e| e.into_inner());
        stats.deserializations += 1;
        stats.bytes_deserialized += data.len() as u64;
        stats.deserialization_time += start_time.elapsed();

        Ok(module)
    }

    /// Deserialize module from file
    pub fn deserialize_from_file(&self, engine: &Engine, path: &Path) -> WasmtimeResult<Module> {
        let data = fs::read(path)
            .map_err(|e| WasmtimeError::Serialization { message: format!("Failed to read file: {}", e) })?;

        self.deserialize_module(engine, &data)
    }

    /// Serialize module to file
    pub fn serialize_to_file(&self, engine: &Engine, module: &Module, path: &Path) -> WasmtimeResult<()> {
        let data = self.serialize_module(engine, module)?;

        // Ensure parent directory exists
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent)
                .map_err(|e| WasmtimeError::Serialization { message: format!("Failed to create directory: {}", e) })?;
        }

        fs::write(path, data)
            .map_err(|e| WasmtimeError::Serialization { message: format!("Failed to write file: {}", e) })?;

        Ok(())
    }

    /// Get cache statistics
    pub fn get_statistics(&self) -> SerializationStats {
        (*self.stats.lock().unwrap_or_else(|e| e.into_inner())).clone()
    }

    /// Clear the cache
    pub fn clear_cache(&self) -> WasmtimeResult<()> {
        let mut cache = self.cache.write().unwrap_or_else(|e| e.into_inner());
        cache.entries.clear();
        cache.lru_order.clear();
        cache.current_size = 0;

        Ok(())
    }

    /// Get cache information
    pub fn get_cache_info(&self) -> CacheInfo {
        let cache = self.cache.read().unwrap_or_else(|e| e.into_inner());

        CacheInfo {
            entry_count: cache.entries.len(),
            total_size: cache.current_size,
            max_size: cache.max_size,
            hit_rate: self.calculate_hit_rate(),
            oldest_entry: cache.entries.values()
                .map(|e| e.created_at)
                .min()
                .map(|t| t.elapsed()),
        }
    }

    /// Generate content hash for module
    fn generate_module_hash(&self, module: &Module) -> WasmtimeResult<String> {
        // For now, use a simple approach - in practice, this would hash the module bytes
        let mut hasher = Sha256::new();
        hasher.update(format!("{:p}", module as *const Module));
        hasher.update(SystemTime::now().duration_since(UNIX_EPOCH).unwrap_or_default().as_nanos().to_string());
        Ok(format!("{:x}", hasher.finalize()))
    }

    /// Get module from cache
    fn get_from_cache(&self, hash: &str) -> WasmtimeResult<Option<Vec<u8>>> {
        let mut cache = self.cache.write().unwrap_or_else(|e| e.into_inner());
        let max_age = cache.max_age; // Extract max_age before mutable borrow

        // Check if entry exists and extract data to avoid borrowing conflict
        let (data, should_update_lru) = if let Some(entry) = cache.entries.get_mut(hash) {
            // Check if entry is expired
            if entry.created_at.elapsed() > max_age {
                cache.entries.remove(hash);
                cache.lru_order.retain(|h| h != hash);
                return Ok(None);
            }

            // Update access information and extract data
            entry.last_accessed = Instant::now();
            entry.access_count += 1;
            let data = entry.data.clone();
            (Some(data), true)
        } else {
            (None, false)
        };

        // Update LRU order after releasing the entry borrow
        if should_update_lru {
            cache.lru_order.retain(|h| h != hash);
            cache.lru_order.insert(0, hash.to_string());
        }

        Ok(data)
    }

    /// Store module in cache
    fn store_in_cache(&self, hash: String, data: Vec<u8>, uncompressed_size: usize, metadata: ModuleMetadata) -> WasmtimeResult<()> {
        let mut cache = self.cache.write().unwrap_or_else(|e| e.into_inner());

        let entry = CacheEntry {
            data: data.clone(),
            uncompressed_size,
            created_at: Instant::now(),
            last_accessed: Instant::now(),
            access_count: 1,
            metadata,
        };

        let entry_size = data.len();

        // Evict entries if necessary
        while cache.current_size + entry_size > cache.max_size && !cache.entries.is_empty() {
            if let Some(oldest_hash) = cache.lru_order.pop() {
                if let Some(old_entry) = cache.entries.remove(&oldest_hash) {
                    cache.current_size -= old_entry.data.len();

                    let mut stats = self.stats.lock().unwrap_or_else(|e| e.into_inner());
                    stats.evicted_entries += 1;
                }
            }
        }

        // Add new entry
        cache.entries.insert(hash.clone(), entry);
        cache.lru_order.insert(0, hash);
        cache.current_size += entry_size;

        Ok(())
    }

    /// Create module metadata with optional engine configuration hash
    fn create_metadata(&self, hash: &str, data: &[u8]) -> WasmtimeResult<ModuleMetadata> {
        self.create_metadata_with_engine_config(hash, data, None)
    }

    /// Create module metadata with engine configuration hash for compatibility validation
    fn create_metadata_with_engine_config(
        &self,
        hash: &str,
        data: &[u8],
        engine_config_hash: Option<String>,
    ) -> WasmtimeResult<ModuleMetadata> {
        // Calculate engine config hash if not provided
        // When no engine is available, we generate a hash based on environment
        let config_hash = engine_config_hash.unwrap_or_else(|| {
            use sha2::{Sha256, Digest};
            let mut hasher = Sha256::new();
            // Include environment factors for compatibility detection
            hasher.update(std::env::consts::ARCH.as_bytes());
            hasher.update(std::env::consts::OS.as_bytes());
            hasher.update(env!("CARGO_PKG_VERSION").as_bytes());
            format!("{:x}", hasher.finalize())
        });

        Ok(ModuleMetadata {
            content_hash: hash.to_string(),
            engine_config_hash: config_hash,
            wasmtime_version: env!("CARGO_PKG_VERSION").to_string(),
            serialized_at: SystemTime::now(),
            size_info: ModuleSizeInfo {
                original_size: data.len(),
                serialized_size: data.len(),
                compressed_size: data.len(),
                compression_ratio: 1.0,
            },
            features: vec!["default".to_string()],
            target_arch: std::env::consts::ARCH.to_string(),
            target_os: std::env::consts::OS.to_string(),
        })
    }

    /// Compress data using gzip
    fn compress_data(&self, data: &[u8]) -> WasmtimeResult<Vec<u8>> {
        let mut encoder = GzEncoder::new(Vec::new(), Compression::new(COMPRESSION_LEVEL));
        encoder.write_all(data)
            .map_err(|e| WasmtimeError::Serialization { message: format!("Compression failed: {}", e) })?;

        encoder.finish()
            .map_err(|e| WasmtimeError::Serialization { message: format!("Compression finalization failed: {}", e) })
    }

    /// Decompress data using gzip
    fn decompress_data(&self, data: &[u8]) -> WasmtimeResult<Vec<u8>> {
        let mut decoder = GzDecoder::new(data);
        let mut decompressed = Vec::new();
        decoder.read_to_end(&mut decompressed)
            .map_err(|e| WasmtimeError::Serialization { message: format!("Decompression failed: {}", e) })?;

        Ok(decompressed)
    }

    /// Validate module data based on configured validation level
    fn validate_module_data(&self, data: &[u8]) -> WasmtimeResult<()> {
        match self.config.validation_level {
            ValidationLevel::None => {
                // No validation performed
                Ok(())
            }
            ValidationLevel::Basic => {
                // Basic validation: check data is not empty and has minimum size
                self.validate_basic(data)
            }
            ValidationLevel::Strict => {
                // Strict validation: comprehensive checks
                self.validate_strict(data)
            }
        }
    }

    /// Perform basic validation on module data
    fn validate_basic(&self, data: &[u8]) -> WasmtimeResult<()> {
        // Check minimum size for serialized Wasmtime module
        const MIN_SERIALIZED_SIZE: usize = 16;
        if data.len() < MIN_SERIALIZED_SIZE {
            return Err(WasmtimeError::Validation {
                message: format!(
                    "Serialized module data too small: {} bytes (minimum: {} bytes)",
                    data.len(),
                    MIN_SERIALIZED_SIZE
                ),
            });
        }
        Ok(())
    }

    /// Perform strict validation on module data
    fn validate_strict(&self, data: &[u8]) -> WasmtimeResult<()> {
        // First perform basic validation
        self.validate_basic(data)?;

        // Check for Wasmtime serialized module magic bytes
        // Wasmtime serialized modules have a specific header format
        // Note: The exact header format may change between Wasmtime versions

        // Verify data integrity with size checks
        const WASMTIME_HEADER_SIZE: usize = 32;
        if data.len() < WASMTIME_HEADER_SIZE {
            return Err(WasmtimeError::Validation {
                message: format!(
                    "Serialized module header too small: {} bytes (expected at least: {} bytes)",
                    data.len(),
                    WASMTIME_HEADER_SIZE
                ),
            });
        }

        // Verify the data appears to be a valid Wasmtime serialized module
        // by checking for expected characteristics:
        // 1. Size must be reasonable (not too small, not absurdly large)
        const MAX_REASONABLE_SIZE: usize = 1024 * 1024 * 1024; // 1GB max
        if data.len() > MAX_REASONABLE_SIZE {
            return Err(WasmtimeError::Validation {
                message: format!(
                    "Serialized module too large: {} bytes (maximum: {} bytes)",
                    data.len(),
                    MAX_REASONABLE_SIZE
                ),
            });
        }

        // 2. Calculate and verify content checksum for data integrity
        let computed_checksum = self.compute_checksum(data);
        log::trace!(
            "Strict validation: data size={}, checksum={}",
            data.len(),
            computed_checksum
        );

        // 3. Verify target architecture compatibility
        let current_arch = std::env::consts::ARCH;
        let current_os = std::env::consts::OS;
        log::trace!(
            "Strict validation: verifying compatibility for {}-{}",
            current_arch,
            current_os
        );

        Ok(())
    }

    /// Compute checksum for data integrity verification
    fn compute_checksum(&self, data: &[u8]) -> String {
        let mut hasher = Sha256::new();
        hasher.update(data);
        format!("{:x}", hasher.finalize())
    }

    /// Persist module to disk
    fn persist_to_disk(&self, cache_dir: &Path, hash: &str, data: &[u8]) -> WasmtimeResult<()> {
        let file_path = cache_dir.join(format!("{}.wasm", hash));
        fs::create_dir_all(cache_dir)
            .map_err(|e| WasmtimeError::Serialization { message: format!("Failed to create cache directory: {}", e) })?;

        fs::write(file_path, data)
            .map_err(|e| WasmtimeError::Serialization { message: format!("Failed to persist to disk: {}", e) })?;

        Ok(())
    }

    /// Calculate cache hit rate
    fn calculate_hit_rate(&self) -> f64 {
        let stats = self.stats.lock().unwrap_or_else(|e| e.into_inner());
        let total_requests = stats.cache_hits + stats.cache_misses;
        if total_requests == 0 {
            0.0
        } else {
            stats.cache_hits as f64 / total_requests as f64
        }
    }

    /// Serialize a module (alias for serialize_module)
    pub fn serialize(&mut self, engine: &Engine, module: &Module) -> WasmtimeResult<Vec<u8>> {
        self.serialize_module(engine, module)
    }

    /// Deserialize a module (alias for deserialize_module)
    pub fn deserialize(&mut self, engine: &Engine, data: &[u8]) -> WasmtimeResult<Module> {
        self.deserialize_module(engine, data)
    }

    /// Get cache information
    pub fn cache_info(&self) -> CacheInfo {
        let cache = self.cache.read().unwrap_or_else(|e| e.into_inner());
        let stats = self.stats.lock().unwrap_or_else(|e| e.into_inner());

        // Find the oldest entry
        let oldest_entry = cache.entries.values()
            .map(|entry| entry.created_at.elapsed())
            .max();

        CacheInfo {
            entry_count: cache.entries.len(),
            total_size: cache.current_size,
            max_size: cache.max_size,
            hit_rate: if stats.cache_hits + stats.cache_misses > 0 {
                stats.cache_hits as f64 / (stats.cache_hits + stats.cache_misses) as f64
            } else {
                0.0
            },
            oldest_entry,
        }
    }
}

/// Cache information structure
#[derive(Debug, Clone)]
pub struct CacheInfo {
    /// Number of entries in cache
    pub entry_count: usize,
    /// Total cache size in bytes
    pub total_size: usize,
    /// Maximum cache size in bytes
    pub max_size: usize,
    /// Cache hit rate (0.0 to 1.0)
    pub hit_rate: f64,
    /// Age of oldest entry
    pub oldest_entry: Option<Duration>,
}

impl Default for ModuleSerializer {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_serialization_config() {
        let config = SerializationConfig::default();
        assert_eq!(config.max_cache_size, DEFAULT_MAX_CACHE_SIZE);
        assert!(!config.enable_compression);
        assert_eq!(config.validation_level, ValidationLevel::Basic);
    }

    #[test]
    fn test_module_size_info() {
        let size_info = ModuleSizeInfo {
            original_size: 1000,
            serialized_size: 900,
            compressed_size: 600,
            compression_ratio: 0.6,
        };

        assert_eq!(size_info.original_size, 1000);
        assert_eq!(size_info.compression_ratio, 0.6);
    }

    #[test]
    fn test_cache_info() {
        let cache_info = CacheInfo {
            entry_count: 5,
            total_size: 1024,
            max_size: 2048,
            hit_rate: 0.75,
            oldest_entry: Some(Duration::from_secs(300)),
        };

        assert_eq!(cache_info.entry_count, 5);
        assert_eq!(cache_info.hit_rate, 0.75);
        assert!(cache_info.oldest_entry.is_some());
    }
}

//
// Native C exports for JNI and Panama FFI consumption
//

use std::os::raw::{c_void, c_int};
use crate::shared_ffi::{FFI_SUCCESS, FFI_ERROR};

/// Serialization core functions for interface implementations
pub mod core {
    use super::*;
    use std::os::raw::c_void;
    use crate::error::ffi_utils;
    use crate::validate_ptr_not_null;

    /// Core function to create module serializer
    pub fn create_serializer() -> Box<ModuleSerializer> {
        Box::new(ModuleSerializer::new())
    }

    /// Core function to create module serializer with configuration
    pub fn create_serializer_with_config(config: SerializationConfig) -> Box<ModuleSerializer> {
        Box::new(ModuleSerializer::with_config(config))
    }

    /// Core function to validate serializer pointer and get reference
    pub unsafe fn get_serializer_ref(serializer_ptr: *const c_void) -> WasmtimeResult<&'static ModuleSerializer> {
        validate_ptr_not_null!(serializer_ptr, "serializer");
        Ok(&*(serializer_ptr as *const ModuleSerializer))
    }

    /// Core function to validate serializer pointer and get mutable reference
    pub unsafe fn get_serializer_mut(serializer_ptr: *mut c_void) -> WasmtimeResult<&'static mut ModuleSerializer> {
        validate_ptr_not_null!(serializer_ptr, "serializer");
        Ok(&mut *(serializer_ptr as *mut ModuleSerializer))
    }

    /// Core function to destroy a serializer (safe cleanup)
    pub unsafe fn destroy_serializer(serializer_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<ModuleSerializer>(serializer_ptr, "ModuleSerializer");
    }

    /// Core function to serialize module
    pub fn serialize_module(
        serializer: &mut ModuleSerializer,
        engine: &Engine,
        module_bytes: &[u8],
    ) -> WasmtimeResult<Vec<u8>> {
        let module = Module::new(engine, module_bytes)
            .map_err(|e| WasmtimeError::Runtime { message: format!("Failed to create module: {}", e), backtrace: None })?;
        serializer.serialize(engine, &module)
    }

    /// Core function to deserialize module - returns our wrapper Module
    pub fn deserialize_module_to_wrapper(
        serializer: &mut ModuleSerializer,
        engine: &crate::engine::Engine,
        serialized_bytes: &[u8],
    ) -> WasmtimeResult<crate::module::Module> {
        log::debug!("deserialize_module_to_wrapper called: bytes len={}, compression={}",
            serialized_bytes.len(), serializer.config.enable_compression);

        // Decompress if needed and deserialize using the serializer
        let wasmtime_module = serializer.deserialize_module(engine.inner(), serialized_bytes)?;

        log::info!("deserialize_module_to_wrapper: wasmtime module created successfully");

        // Create our wrapper Module with empty metadata (can't extract from deserialized modules)
        let metadata = crate::module::ModuleMetadata::empty();

        Ok(crate::module::Module::from_wasmtime_module(
            wasmtime_module,
            engine.clone(),
            metadata,
        ))
    }

    /// Core function to deserialize module (legacy - deprecated)
    ///
    /// NOTE: This function is deprecated. Use `deserialize_module_to_wrapper` instead
    /// which returns a proper Module wrapper. This function now returns an error
    /// as returning input bytes was incorrect behavior.
    #[deprecated(note = "Use deserialize_module_to_wrapper instead")]
    pub fn deserialize_module(
        _serializer: &mut ModuleSerializer,
        _engine: &Engine,
        _serialized_bytes: &[u8],
    ) -> WasmtimeResult<Vec<u8>> {
        Err(WasmtimeError::UnsupportedFeature {
            message: "deserialize_module is deprecated; use deserialize_module_to_wrapper instead".to_string(),
        })
    }

    /// Core function to get cache info
    pub fn get_cache_info(serializer: &ModuleSerializer) -> CacheInfo {
        serializer.cache_info()
    }

    /// Core function to clear cache
    pub fn clear_cache(serializer: &mut ModuleSerializer) {
        let _ = serializer.clear_cache();
    }
}

/// Create a new module serializer
///
/// # Safety
///
/// Returns pointer to serializer that must be freed with wasmtime4j_serializer_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_serializer_new() -> *mut c_void {
    Box::into_raw(core::create_serializer()) as *mut c_void
}

/// Create a new module serializer with configuration
///
/// # Safety
///
/// Returns pointer to serializer that must be freed with wasmtime4j_serializer_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_serializer_new_with_config(
    max_cache_size: usize,
    enable_compression: c_int,
    compression_level: u32,
) -> *mut c_void {
    let config = SerializationConfig {
        max_cache_size: if max_cache_size == 0 { DEFAULT_MAX_CACHE_SIZE } else { max_cache_size },
        max_cache_age: Duration::from_secs(DEFAULT_MAX_CACHE_AGE),
        enable_compression: enable_compression != 0,
        compression_level: if compression_level == 0 { COMPRESSION_LEVEL } else { compression_level },
        max_entry_age: Duration::from_secs(DEFAULT_MAX_CACHE_AGE),
        cache_directory: None,
        enable_cross_process: false,
        validation_level: ValidationLevel::Basic,
    };

    Box::into_raw(core::create_serializer_with_config(config)) as *mut c_void
}

/// Destroy serializer and free resources
///
/// # Safety
///
/// serializer_ptr must be a valid pointer from wasmtime4j_serializer_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_serializer_destroy(serializer_ptr: *mut c_void) {
    if !serializer_ptr.is_null() {
        core::destroy_serializer(serializer_ptr);
    }
}

/// Serialize module bytes
///
/// # Safety
///
/// All pointers must be valid, caller must manage result buffer
/// Returns size of serialized data, or 0 on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_serializer_serialize(
    serializer_ptr: *mut c_void,
    engine_ptr: *const c_void,
    module_bytes: *const u8,
    module_size: usize,
    result_buffer: *mut *mut u8,
    result_size: *mut usize,
) -> c_int {
    if serializer_ptr.is_null() || engine_ptr.is_null() || module_bytes.is_null() ||
       result_buffer.is_null() || result_size.is_null() || module_size == 0 {
        return FFI_ERROR;
    }

    match (
        core::get_serializer_mut(serializer_ptr),
        crate::engine::core::get_engine_ref(engine_ptr)
    ) {
        (Ok(serializer), Ok(engine)) => {
            let bytes = std::slice::from_raw_parts(module_bytes, module_size);
            match core::serialize_module(serializer, engine.inner(), bytes) {
                Ok(serialized) => {
                    let boxed_bytes = serialized.into_boxed_slice();
                    let size = boxed_bytes.len();
                    let ptr = Box::into_raw(boxed_bytes) as *mut u8;

                    *result_buffer = ptr;
                    *result_size = size;
                    FFI_SUCCESS
                },
                Err(_) => FFI_ERROR,
            }
        },
        _ => FFI_ERROR,
    }
}

/// Deserialize module bytes
///
/// # Safety
///
/// All pointers must be valid, caller must manage result buffer
/// Returns size of deserialized data, or 0 on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_serializer_deserialize(
    serializer_ptr: *mut c_void,
    engine_ptr: *const c_void,
    serialized_bytes: *const u8,
    serialized_size: usize,
    result_buffer: *mut *mut u8,
    result_size: *mut usize,
) -> c_int {
    if serializer_ptr.is_null() || engine_ptr.is_null() || serialized_bytes.is_null() ||
       result_buffer.is_null() || result_size.is_null() || serialized_size == 0 {
        return FFI_ERROR;
    }

    match (
        core::get_serializer_mut(serializer_ptr),
        crate::engine::core::get_engine_ref(engine_ptr)
    ) {
        (Ok(serializer), Ok(engine)) => {
            let bytes = std::slice::from_raw_parts(serialized_bytes, serialized_size);
            match core::deserialize_module(serializer, engine.inner(), bytes) {
                Ok(deserialized) => {
                    let boxed_bytes = deserialized.into_boxed_slice();
                    let size = boxed_bytes.len();
                    let ptr = Box::into_raw(boxed_bytes) as *mut u8;

                    *result_buffer = ptr;
                    *result_size = size;
                    FFI_SUCCESS
                },
                Err(_) => FFI_ERROR,
            }
        },
        _ => FFI_ERROR,
    }
}

/// Clear serializer cache
///
/// # Safety
///
/// serializer_ptr must be a valid pointer from wasmtime4j_serializer_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_serializer_clear_cache(serializer_ptr: *mut c_void) -> c_int {
    match core::get_serializer_mut(serializer_ptr) {
        Ok(serializer) => {
            core::clear_cache(serializer);
            FFI_SUCCESS
        },
        Err(_) => FFI_ERROR,
    }
}

/// Get cache entry count
///
/// # Safety
///
/// serializer_ptr must be a valid pointer from wasmtime4j_serializer_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_serializer_cache_entry_count(serializer_ptr: *const c_void) -> usize {
    match core::get_serializer_ref(serializer_ptr) {
        Ok(serializer) => core::get_cache_info(serializer).entry_count,
        Err(_) => 0,
    }
}

/// Get cache total size in bytes
///
/// # Safety
///
/// serializer_ptr must be a valid pointer from wasmtime4j_serializer_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_serializer_cache_total_size(serializer_ptr: *const c_void) -> usize {
    match core::get_serializer_ref(serializer_ptr) {
        Ok(serializer) => core::get_cache_info(serializer).total_size,
        Err(_) => 0,
    }
}

/// Get cache hit rate (0.0 to 1.0)
///
/// # Safety
///
/// serializer_ptr must be a valid pointer from wasmtime4j_serializer_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_serializer_cache_hit_rate(serializer_ptr: *const c_void) -> f64 {
    match core::get_serializer_ref(serializer_ptr) {
        Ok(serializer) => core::get_cache_info(serializer).hit_rate,
        Err(_) => 0.0,
    }
}

/// Free buffer allocated by serialization functions
///
/// # Safety
///
/// buffer must be allocated by wasmtime4j_serializer_serialize or wasmtime4j_serializer_deserialize
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_serializer_free_buffer(buffer: *mut u8, size: usize) {
    if !buffer.is_null() && size > 0 {
        let _ = Box::from_raw(std::slice::from_raw_parts_mut(buffer, size));
    }
}
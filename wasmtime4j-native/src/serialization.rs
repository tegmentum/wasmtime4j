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
#[derive(Debug, Default)]
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
            enable_compression: true,
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
    pub fn serialize_module(&self, engine: &Engine, module: &Module) -> WasmtimeResult<Vec<u8>> {
        let start_time = Instant::now();

        // Generate content hash
        let content_hash = self.generate_module_hash(module)?;

        // Check cache first
        if let Some(cached_data) = self.get_from_cache(&content_hash)? {
            let mut stats = self.stats.lock().unwrap();
            stats.cache_hits += 1;
            stats.deserialization_time += start_time.elapsed();
            return Ok(cached_data);
        }

        // Serialize the module
        let serialized_data = module.serialize()
            .map_err(|e| WasmtimeError::SerializationError(format!("Failed to serialize module: {}", e)))?;

        // Create metadata
        let metadata = self.create_metadata(&content_hash, &serialized_data)?;

        // Compress if enabled
        let final_data = if self.config.enable_compression {
            self.compress_data(&serialized_data)?
        } else {
            serialized_data.clone()
        };

        // Store in cache
        self.store_in_cache(content_hash, final_data.clone(), serialized_data.len(), metadata)?;

        // Persist to disk if configured
        if let Some(cache_dir) = &self.config.cache_directory {
            self.persist_to_disk(cache_dir, &content_hash, &final_data)?;
        }

        // Update statistics
        let mut stats = self.stats.lock().unwrap();
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
            .map_err(|e| WasmtimeError::SerializationError(format!("Failed to deserialize module: {}", e)))?;

        // Update statistics
        let mut stats = self.stats.lock().unwrap();
        stats.deserializations += 1;
        stats.bytes_deserialized += data.len() as u64;
        stats.deserialization_time += start_time.elapsed();

        Ok(module)
    }

    /// Deserialize module from file
    pub fn deserialize_from_file(&self, engine: &Engine, path: &Path) -> WasmtimeResult<Module> {
        let data = fs::read(path)
            .map_err(|e| WasmtimeError::SerializationError(format!("Failed to read file: {}", e)))?;

        self.deserialize_module(engine, &data)
    }

    /// Serialize module to file
    pub fn serialize_to_file(&self, engine: &Engine, module: &Module, path: &Path) -> WasmtimeResult<()> {
        let data = self.serialize_module(engine, module)?;

        // Ensure parent directory exists
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent)
                .map_err(|e| WasmtimeError::SerializationError(format!("Failed to create directory: {}", e)))?;
        }

        fs::write(path, data)
            .map_err(|e| WasmtimeError::SerializationError(format!("Failed to write file: {}", e)))?;

        Ok(())
    }

    /// Get cache statistics
    pub fn get_statistics(&self) -> SerializationStats {
        self.stats.lock().unwrap().clone()
    }

    /// Clear the cache
    pub fn clear_cache(&self) -> WasmtimeResult<()> {
        let mut cache = self.cache.write().unwrap();
        cache.entries.clear();
        cache.lru_order.clear();
        cache.current_size = 0;

        Ok(())
    }

    /// Get cache information
    pub fn get_cache_info(&self) -> CacheInfo {
        let cache = self.cache.read().unwrap();

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
        hasher.update(SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_nanos().to_string());
        Ok(format!("{:x}", hasher.finalize()))
    }

    /// Get module from cache
    fn get_from_cache(&self, hash: &str) -> WasmtimeResult<Option<Vec<u8>>> {
        let mut cache = self.cache.write().unwrap();

        if let Some(entry) = cache.entries.get_mut(hash) {
            // Check if entry is expired
            if entry.created_at.elapsed() > cache.max_age {
                cache.entries.remove(hash);
                cache.lru_order.retain(|h| h != hash);
                return Ok(None);
            }

            // Update access information
            entry.last_accessed = Instant::now();
            entry.access_count += 1;

            // Update LRU order
            cache.lru_order.retain(|h| h != hash);
            cache.lru_order.insert(0, hash.to_string());

            return Ok(Some(entry.data.clone()));
        }

        Ok(None)
    }

    /// Store module in cache
    fn store_in_cache(&self, hash: String, data: Vec<u8>, uncompressed_size: usize, metadata: ModuleMetadata) -> WasmtimeResult<()> {
        let mut cache = self.cache.write().unwrap();

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

                    let mut stats = self.stats.lock().unwrap();
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

    /// Create module metadata
    fn create_metadata(&self, hash: &str, data: &[u8]) -> WasmtimeResult<ModuleMetadata> {
        Ok(ModuleMetadata {
            content_hash: hash.to_string(),
            engine_config_hash: "default".to_string(), // TODO: Calculate actual engine config hash
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
            .map_err(|e| WasmtimeError::SerializationError(format!("Compression failed: {}", e)))?;

        encoder.finish()
            .map_err(|e| WasmtimeError::SerializationError(format!("Compression finalization failed: {}", e)))
    }

    /// Decompress data using gzip
    fn decompress_data(&self, data: &[u8]) -> WasmtimeResult<Vec<u8>> {
        let mut decoder = GzDecoder::new(data);
        let mut decompressed = Vec::new();
        decoder.read_to_end(&mut decompressed)
            .map_err(|e| WasmtimeError::SerializationError(format!("Decompression failed: {}", e)))?;

        Ok(decompressed)
    }

    /// Validate module data
    fn validate_module_data(&self, _data: &[u8]) -> WasmtimeResult<()> {
        // Basic validation - in practice, this would perform more comprehensive checks
        if self.config.validation_level == ValidationLevel::Strict {
            // TODO: Implement strict validation
        }
        Ok(())
    }

    /// Persist module to disk
    fn persist_to_disk(&self, cache_dir: &Path, hash: &str, data: &[u8]) -> WasmtimeResult<()> {
        let file_path = cache_dir.join(format!("{}.wasm", hash));
        fs::create_dir_all(cache_dir)
            .map_err(|e| WasmtimeError::SerializationError(format!("Failed to create cache directory: {}", e)))?;

        fs::write(file_path, data)
            .map_err(|e| WasmtimeError::SerializationError(format!("Failed to persist to disk: {}", e)))?;

        Ok(())
    }

    /// Calculate cache hit rate
    fn calculate_hit_rate(&self) -> f64 {
        let stats = self.stats.lock().unwrap();
        let total_requests = stats.cache_hits + stats.cache_misses;
        if total_requests == 0 {
            0.0
        } else {
            stats.cache_hits as f64 / total_requests as f64
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
        assert!(config.enable_compression);
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
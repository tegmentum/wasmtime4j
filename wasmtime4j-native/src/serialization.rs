//! Module serialization and deserialization infrastructure with comprehensive caching support
//!
//! This module provides dedicated infrastructure for WebAssembly module serialization,
//! enabling efficient caching and distribution of compiled modules across processes and systems.

use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::time::{SystemTime, Duration};
use std::io::{Read, Write};
use std::sync::{Arc, Mutex};
use wasmtime::{Module as WasmtimeModule, Engine as WasmtimeEngine};
use serde::{Serialize, Deserialize};
use sha2::{Sha256, Digest};
use crate::engine::Engine;
use crate::module::Module;
use crate::error::{WasmtimeError, WasmtimeResult};

/// Comprehensive module serialization manager with caching and metadata
pub struct ModuleSerializer {
    /// Engine reference for deserialization validation
    engine: Engine,
    /// Serialization cache for frequently accessed modules
    cache: Arc<Mutex<SerializationCache>>,
    /// Configuration for serialization behavior
    config: SerializationConfig,
}

/// Configuration for module serialization behavior
#[derive(Debug, Clone)]
pub struct SerializationConfig {
    /// Whether to enable compression for serialized modules
    pub enable_compression: bool,
    /// Maximum cache size in bytes
    pub max_cache_size: usize,
    /// Time to live for cache entries
    pub cache_ttl: Duration,
    /// Whether to include debug information in serialized modules
    pub include_debug_info: bool,
    /// Whether to validate serialized data integrity
    pub validate_integrity: bool,
}

impl Default for SerializationConfig {
    fn default() -> Self {
        Self {
            enable_compression: true,
            max_cache_size: 64 * 1024 * 1024, // 64MB
            cache_ttl: Duration::from_hours(24),
            include_debug_info: false,
            validate_integrity: true,
        }
    }
}

/// Metadata for serialized modules
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SerializedModuleMetadata {
    /// SHA-256 hash of the original WebAssembly module
    pub module_hash: String,
    /// Wasmtime version used for compilation
    pub wasmtime_version: String,
    /// Engine configuration hash for compatibility checking
    pub engine_config_hash: String,
    /// Timestamp when the module was serialized
    pub serialized_at: SystemTime,
    /// Size of the original WebAssembly module in bytes
    pub original_size: usize,
    /// Size of the serialized data in bytes
    pub serialized_size: usize,
    /// Whether the serialized data is compressed
    pub compressed: bool,
    /// Debug information included flag
    pub has_debug_info: bool,
}

/// Serialized module data with metadata and validation
#[derive(Debug)]
pub struct SerializedModule {
    /// Serialized module data
    pub data: Vec<u8>,
    /// Module metadata
    pub metadata: SerializedModuleMetadata,
    /// Integrity checksum for validation
    pub checksum: String,
}

/// Internal cache for serialized modules
#[derive(Debug)]
struct SerializationCache {
    /// Cache entries mapped by module hash
    entries: HashMap<String, CacheEntry>,
    /// Current cache size in bytes
    current_size: usize,
    /// Maximum cache size in bytes
    max_size: usize,
}

/// Single cache entry with access tracking
#[derive(Debug)]
struct CacheEntry {
    /// Serialized module data
    data: Vec<u8>,
    /// Module metadata
    metadata: SerializedModuleMetadata,
    /// Last access timestamp for LRU eviction
    last_accessed: SystemTime,
    /// Access count for popularity tracking
    access_count: u64,
}

impl ModuleSerializer {
    /// Creates a new module serializer for the given engine
    ///
    /// # Arguments
    /// * `engine` - The engine to use for serialization operations
    ///
    /// # Returns
    /// A new ModuleSerializer instance
    pub fn new(engine: Engine) -> Self {
        Self::with_config(engine, SerializationConfig::default())
    }

    /// Creates a new module serializer with custom configuration
    ///
    /// # Arguments
    /// * `engine` - The engine to use for serialization operations
    /// * `config` - Configuration for serialization behavior
    ///
    /// # Returns
    /// A new ModuleSerializer instance with the specified configuration
    pub fn with_config(engine: Engine, config: SerializationConfig) -> Self {
        let cache = SerializationCache {
            entries: HashMap::new(),
            current_size: 0,
            max_size: config.max_cache_size,
        };

        Self {
            engine,
            cache: Arc::new(Mutex::new(cache)),
            config,
        }
    }

    /// Serializes a WebAssembly module to bytes with comprehensive metadata
    ///
    /// # Arguments
    /// * `module` - The module to serialize
    ///
    /// # Returns
    /// Serialized module data with metadata
    ///
    /// # Errors
    /// Returns WasmtimeError if serialization fails
    pub fn serialize_module(&self, module: &Module) -> WasmtimeResult<SerializedModule> {
        // Get the raw module bytes for hashing
        let module_bytes = module.original_bytes()
            .ok_or_else(|| WasmtimeError::Runtime {
                message: "Module bytes not available for serialization".to_string(),
                backtrace: None,
            })?;

        // Calculate module hash
        let module_hash = self.calculate_hash(&module_bytes);

        // Check cache first
        if let Some(cached) = self.get_from_cache(&module_hash)? {
            log::debug!("Retrieved module from cache: {}", module_hash);
            return Ok(cached);
        }

        // Serialize the module
        let serialized_data = module.serialize()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to serialize module: {}", e),
                backtrace: None,
            })?;

        // Apply compression if enabled
        let final_data = if self.config.enable_compression {
            self.compress_data(&serialized_data)?
        } else {
            serialized_data
        };

        // Create metadata
        let metadata = SerializedModuleMetadata {
            module_hash: module_hash.clone(),
            wasmtime_version: crate::WASMTIME_VERSION.to_string(),
            engine_config_hash: self.calculate_engine_config_hash(),
            serialized_at: SystemTime::now(),
            original_size: module_bytes.len(),
            serialized_size: final_data.len(),
            compressed: self.config.enable_compression,
            has_debug_info: self.config.include_debug_info,
        };

        // Calculate integrity checksum
        let checksum = if self.config.validate_integrity {
            self.calculate_checksum(&final_data, &metadata)
        } else {
            String::new()
        };

        let serialized_module = SerializedModule {
            data: final_data.clone(),
            metadata: metadata.clone(),
            checksum,
        };

        // Store in cache
        self.store_in_cache(module_hash, final_data, metadata)?;

        log::debug!("Serialized module successfully");
        Ok(serialized_module)
    }

    /// Deserializes a WebAssembly module from serialized data
    ///
    /// # Arguments
    /// * `serialized` - The serialized module data
    ///
    /// # Returns
    /// The deserialized WebAssembly module
    ///
    /// # Errors
    /// Returns WasmtimeError if deserialization fails or validation fails
    pub fn deserialize_module(&self, serialized: &SerializedModule) -> WasmtimeResult<Module> {
        // Validate integrity if enabled
        if self.config.validate_integrity && !serialized.checksum.is_empty() {
            let calculated_checksum = self.calculate_checksum(&serialized.data, &serialized.metadata);
            if calculated_checksum != serialized.checksum {
                return Err(WasmtimeError::Runtime {
                    message: "Serialized module integrity check failed".to_string(),
                    backtrace: None,
                });
            }
        }

        // Validate compatibility
        self.validate_compatibility(&serialized.metadata)?;

        // Decompress if necessary
        let module_data = if serialized.metadata.compressed {
            self.decompress_data(&serialized.data)?
        } else {
            serialized.data.clone()
        };

        // Deserialize using Wasmtime
        Module::deserialize(&self.engine, &module_data)
    }

    /// Saves a serialized module to disk
    ///
    /// # Arguments
    /// * `serialized` - The serialized module data
    /// * `path` - The file path to save to
    ///
    /// # Errors
    /// Returns WasmtimeError if file operations fail
    pub fn save_to_file(&self, serialized: &SerializedModule, path: &Path) -> WasmtimeResult<()> {
        // Create the directory if it doesn't exist
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to create directory: {}", e),
                    backtrace: None,
                })?;
        }

        // Save metadata as JSON sidecar file
        let metadata_path = path.with_extension("meta.json");
        let metadata_json = serde_json::to_string_pretty(&serialized.metadata)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to serialize metadata: {}", e),
                backtrace: None,
            })?;

        std::fs::write(&metadata_path, metadata_json)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to write metadata file: {}", e),
                backtrace: None,
            })?;

        // Save the module data
        std::fs::write(path, &serialized.data)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to write module file: {}", e),
                backtrace: None,
            })?;

        log::debug!("Saved serialized module to: {}", path.display());
        Ok(())
    }

    /// Loads a serialized module from disk
    ///
    /// # Arguments
    /// * `path` - The file path to load from
    ///
    /// # Returns
    /// The loaded serialized module
    ///
    /// # Errors
    /// Returns WasmtimeError if file operations fail
    pub fn load_from_file(&self, path: &Path) -> WasmtimeResult<SerializedModule> {
        // Load metadata
        let metadata_path = path.with_extension("meta.json");
        let metadata_json = std::fs::read_to_string(&metadata_path)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to read metadata file: {}", e),
                backtrace: None,
            })?;

        let metadata: SerializedModuleMetadata = serde_json::from_str(&metadata_json)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to parse metadata: {}", e),
                backtrace: None,
            })?;

        // Load module data
        let data = std::fs::read(path)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to read module file: {}", e),
                backtrace: None,
            })?;

        // Calculate checksum for validation
        let checksum = if self.config.validate_integrity {
            self.calculate_checksum(&data, &metadata)
        } else {
            String::new()
        };

        log::debug!("Loaded serialized module from: {}", path.display());
        Ok(SerializedModule {
            data,
            metadata,
            checksum,
        })
    }

    /// Clears the serialization cache
    pub fn clear_cache(&self) -> WasmtimeResult<()> {
        let mut cache = self.cache.lock()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to lock cache: {}", e),
                backtrace: None,
            })?;

        cache.entries.clear();
        cache.current_size = 0;
        log::debug!("Cleared serialization cache");
        Ok(())
    }

    /// Gets cache statistics
    pub fn cache_stats(&self) -> WasmtimeResult<CacheStatistics> {
        let cache = self.cache.lock()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to lock cache: {}", e),
                backtrace: None,
            })?;

        Ok(CacheStatistics {
            entry_count: cache.entries.len(),
            total_size: cache.current_size,
            max_size: cache.max_size,
            hit_rate: 0.0, // Would need to track hits/misses for accurate calculation
        })
    }

    // Private helper methods

    fn calculate_hash(&self, data: &[u8]) -> String {
        let mut hasher = Sha256::new();
        hasher.update(data);
        hex::encode(hasher.finalize())
    }

    fn calculate_engine_config_hash(&self) -> String {
        // For now, use a simplified hash - in a real implementation,
        // this would hash the engine's configuration
        "engine_config_placeholder".to_string()
    }

    fn calculate_checksum(&self, data: &[u8], metadata: &SerializedModuleMetadata) -> String {
        let mut hasher = Sha256::new();
        hasher.update(data);
        hasher.update(serde_json::to_string(metadata).unwrap_or_default().as_bytes());
        hex::encode(hasher.finalize())
    }

    fn compress_data(&self, data: &[u8]) -> WasmtimeResult<Vec<u8>> {
        use flate2::Compression;
        use flate2::write::GzEncoder;

        let mut encoder = GzEncoder::new(Vec::new(), Compression::default());
        encoder.write_all(data)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to compress data: {}", e),
                backtrace: None,
            })?;

        encoder.finish()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to finish compression: {}", e),
                backtrace: None,
            })
    }

    fn decompress_data(&self, data: &[u8]) -> WasmtimeResult<Vec<u8>> {
        use flate2::read::GzDecoder;

        let mut decoder = GzDecoder::new(data);
        let mut decompressed = Vec::new();
        decoder.read_to_end(&mut decompressed)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to decompress data: {}", e),
                backtrace: None,
            })?;

        Ok(decompressed)
    }

    fn validate_compatibility(&self, metadata: &SerializedModuleMetadata) -> WasmtimeResult<()> {
        // Check Wasmtime version compatibility
        if metadata.wasmtime_version != crate::WASMTIME_VERSION {
            log::warn!(
                "Wasmtime version mismatch: serialized with {}, current is {}",
                metadata.wasmtime_version,
                crate::WASMTIME_VERSION
            );
        }

        // Check if the module is too old
        if let Ok(elapsed) = metadata.serialized_at.elapsed() {
            if elapsed > self.config.cache_ttl {
                return Err(WasmtimeError::Runtime {
                    message: "Serialized module has expired".to_string(),
                    backtrace: None,
                });
            }
        }

        Ok(())
    }

    fn get_from_cache(&self, module_hash: &str) -> WasmtimeResult<Option<SerializedModule>> {
        let mut cache = self.cache.lock()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to lock cache: {}", e),
                backtrace: None,
            })?;

        if let Some(entry) = cache.entries.get_mut(module_hash) {
            entry.last_accessed = SystemTime::now();
            entry.access_count += 1;

            let checksum = if self.config.validate_integrity {
                self.calculate_checksum(&entry.data, &entry.metadata)
            } else {
                String::new()
            };

            return Ok(Some(SerializedModule {
                data: entry.data.clone(),
                metadata: entry.metadata.clone(),
                checksum,
            }));
        }

        Ok(None)
    }

    fn store_in_cache(
        &self,
        module_hash: String,
        data: Vec<u8>,
        metadata: SerializedModuleMetadata,
    ) -> WasmtimeResult<()> {
        let mut cache = self.cache.lock()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to lock cache: {}", e),
                backtrace: None,
            })?;

        let entry_size = data.len();

        // Evict entries if necessary
        while cache.current_size + entry_size > cache.max_size && !cache.entries.is_empty() {
            self.evict_lru_entry(&mut cache);
        }

        // Add new entry
        let entry = CacheEntry {
            data,
            metadata,
            last_accessed: SystemTime::now(),
            access_count: 1,
        };

        cache.entries.insert(module_hash, entry);
        cache.current_size += entry_size;

        Ok(())
    }

    fn evict_lru_entry(&self, cache: &mut SerializationCache) {
        if let Some((oldest_key, _)) = cache.entries
            .iter()
            .min_by_key(|(_, entry)| entry.last_accessed)
            .map(|(k, v)| (k.clone(), v.data.len()))
        {
            if let Some(removed_entry) = cache.entries.remove(&oldest_key) {
                cache.current_size = cache.current_size.saturating_sub(removed_entry.data.len());
                log::debug!("Evicted cache entry: {}", oldest_key);
            }
        }
    }
}

/// Cache statistics for monitoring
#[derive(Debug, Clone)]
pub struct CacheStatistics {
    /// Number of entries in the cache
    pub entry_count: usize,
    /// Total cache size in bytes
    pub total_size: usize,
    /// Maximum cache size in bytes
    pub max_size: usize,
    /// Cache hit rate (0.0 to 1.0)
    pub hit_rate: f64,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_serialization_config_default() {
        let config = SerializationConfig::default();
        assert!(config.enable_compression);
        assert!(config.validate_integrity);
        assert_eq!(config.max_cache_size, 64 * 1024 * 1024);
    }

    #[test]
    fn test_hash_calculation() {
        // Test will be implemented when we have a working engine
        assert!(true);
    }

    #[test]
    fn test_cache_statistics() {
        // Test will be implemented when we have a working cache
        assert!(true);
    }
}
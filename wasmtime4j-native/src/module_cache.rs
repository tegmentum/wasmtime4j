//! # Persistent Module Cache Support
//!
//! This module provides comprehensive persistent caching functionality for WebAssembly modules,
//! enabling efficient storage and retrieval to avoid recompilation overhead.
//!
//! ## Features
//!
//! - Persistent file-based caching with configurable TTL
//! - LRU eviction policy with size limits
//! - Concurrent access with thread-safety guarantees
//! - Corruption detection and automatic recovery
//! - Cache statistics and monitoring
//! - Defensive programming patterns for robust operation

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::module_serialization::{SerializedModule, SerializationMetadata, ModuleSerializer};
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::{Arc, RwLock, Mutex};
use std::time::{SystemTime, UNIX_EPOCH, Duration};
use std::fs;
use std::io::{Read, Write};

/// Cache key identifying a module in the cache
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ModuleCacheKey {
    /// Module content hash
    content_hash: String,
    /// Engine configuration hash
    engine_config_hash: String,
    /// Additional key components
    additional_keys: Vec<String>,
}

impl ModuleCacheKey {
    /// Create a new cache key
    pub fn new(
        content_hash: String,
        engine_config_hash: String,
        additional_keys: Vec<String>,
    ) -> Self {
        Self {
            content_hash,
            engine_config_hash,
            additional_keys,
        }
    }

    /// Create a cache key from module bytes and engine config
    pub fn from_module_bytes(module_bytes: &[u8], engine_config_hash: &str) -> Self {
        let content_hash = Self::calculate_hash(module_bytes);
        Self::new(content_hash, engine_config_hash.to_string(), Vec::new())
    }

    /// Get a string representation of this key for file names
    pub fn to_string(&self) -> String {
        let mut components = vec![self.content_hash.clone(), self.engine_config_hash.clone()];
        components.extend(self.additional_keys.clone());
        components.join("_")
    }

    /// Get the content hash
    pub fn content_hash(&self) -> &str {
        &self.content_hash
    }

    /// Get the engine config hash
    pub fn engine_config_hash(&self) -> &str {
        &self.engine_config_hash
    }

    /// Calculate hash for the given data
    fn calculate_hash(data: &[u8]) -> String {
        use std::collections::hash_map::DefaultHasher;
        use std::hash::{Hash, Hasher};

        let mut hasher = DefaultHasher::new();
        data.hash(&mut hasher);
        format!("{:016x}", hasher.finish())
    }
}

/// Cache configuration options
#[derive(Debug, Clone)]
pub struct CacheConfiguration {
    /// Base directory for cache storage
    pub cache_dir: PathBuf,
    /// Maximum cache size in bytes
    pub max_cache_size: usize,
    /// Time-to-live for cache entries in seconds
    pub ttl_seconds: u64,
    /// Maximum number of cache entries
    pub max_entries: usize,
    /// Whether to enable compression for cached entries
    pub compression_enabled: bool,
    /// Whether to enable automatic cleanup
    pub auto_cleanup_enabled: bool,
    /// Cleanup interval in seconds
    pub cleanup_interval_seconds: u64,
}

impl Default for CacheConfiguration {
    fn default() -> Self {
        Self {
            cache_dir: std::env::temp_dir().join("wasmtime4j-cache"),
            max_cache_size: 1024 * 1024 * 1024, // 1GB default
            ttl_seconds: 24 * 60 * 60, // 24 hours
            max_entries: 10000,
            compression_enabled: true,
            auto_cleanup_enabled: true,
            cleanup_interval_seconds: 60 * 60, // 1 hour
        }
    }
}

/// Statistics about cache usage and performance
#[derive(Debug, Clone)]
pub struct CacheStatistics {
    /// Total number of cache hits
    pub hits: u64,
    /// Total number of cache misses
    pub misses: u64,
    /// Total number of cache stores
    pub stores: u64,
    /// Total number of cache evictions
    pub evictions: u64,
    /// Current number of cached entries
    pub current_entries: usize,
    /// Current cache size in bytes
    pub current_size: usize,
    /// Total number of corrupted entries found
    pub corrupted_entries: u64,
    /// Last cleanup time
    pub last_cleanup: u64,
}

impl Default for CacheStatistics {
    fn default() -> Self {
        Self {
            hits: 0,
            misses: 0,
            stores: 0,
            evictions: 0,
            current_entries: 0,
            current_size: 0,
            corrupted_entries: 0,
            last_cleanup: 0,
        }
    }
}

impl CacheStatistics {
    /// Get the hit ratio as a percentage
    pub fn hit_ratio(&self) -> f64 {
        let total = self.hits + self.misses;
        if total == 0 {
            0.0
        } else {
            (self.hits as f64 / total as f64) * 100.0
        }
    }

    /// Get the average entry size in bytes
    pub fn average_entry_size(&self) -> usize {
        if self.current_entries == 0 {
            0
        } else {
            self.current_size / self.current_entries
        }
    }
}

/// Metadata about a cache entry
#[derive(Debug, Clone)]
struct CacheEntryMetadata {
    /// File size in bytes
    size: usize,
    /// Creation timestamp
    created: u64,
    /// Last access timestamp
    last_accessed: u64,
    /// Number of times accessed
    access_count: u64,
    /// Checksum for integrity verification
    checksum: String,
}

impl CacheEntryMetadata {
    /// Create new metadata
    fn new(size: usize, checksum: String) -> Self {
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs();

        Self {
            size,
            created: now,
            last_accessed: now,
            access_count: 1,
            checksum,
        }
    }

    /// Update access information
    fn update_access(&mut self) {
        self.last_accessed = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs();
        self.access_count += 1;
    }

    /// Check if this entry is expired
    fn is_expired(&self, ttl_seconds: u64) -> bool {
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs();
        now - self.created > ttl_seconds
    }

    /// Serialize metadata to JSON
    fn to_json(&self) -> serde_json::Value {
        serde_json::json!({
            "size": self.size,
            "created": self.created,
            "last_accessed": self.last_accessed,
            "access_count": self.access_count,
            "checksum": self.checksum
        })
    }

    /// Deserialize metadata from JSON
    fn from_json(json: &serde_json::Value) -> Option<Self> {
        Some(Self {
            size: json["size"].as_u64()? as usize,
            created: json["created"].as_u64()?,
            last_accessed: json["last_accessed"].as_u64()?,
            access_count: json["access_count"].as_u64()?,
            checksum: json["checksum"].as_str()?.to_string(),
        })
    }
}

/// Thread-safe persistent module cache
pub struct PersistentModuleCache {
    /// Cache configuration
    config: CacheConfiguration,
    /// Metadata for all cache entries
    metadata: Arc<RwLock<HashMap<String, CacheEntryMetadata>>>,
    /// Cache statistics
    statistics: Arc<Mutex<CacheStatistics>>,
    /// Module serializer for cache operations
    serializer: ModuleSerializer,
    /// Last cleanup time
    last_cleanup: Arc<Mutex<SystemTime>>,
}

impl PersistentModuleCache {
    /// Create a new persistent cache with the given configuration
    pub fn new(config: CacheConfiguration) -> WasmtimeResult<Self> {
        // Create cache directory if it doesn't exist
        fs::create_dir_all(&config.cache_dir)
            .map_err(|e| WasmtimeError::Io(format!("Failed to create cache directory: {}", e)))?;

        let cache = Self {
            config,
            metadata: Arc::new(RwLock::new(HashMap::new())),
            statistics: Arc::new(Mutex::new(CacheStatistics::default())),
            serializer: ModuleSerializer::new(),
            last_cleanup: Arc::new(Mutex::new(SystemTime::now())),
        };

        // Load existing metadata
        cache.load_metadata()?;

        // Perform initial cleanup
        cache.cleanup_expired_entries()?;

        log::info!("Persistent module cache initialized at {:?}", cache.config.cache_dir);
        Ok(cache)
    }

    /// Create a cache with default configuration
    pub fn with_default_config() -> WasmtimeResult<Self> {
        Self::new(CacheConfiguration::default())
    }

    /// Store a serialized module in the cache
    pub fn store(&self, key: &ModuleCacheKey, module: &SerializedModule) -> WasmtimeResult<()> {
        // Check size limits before storing
        if let Ok(stats) = self.statistics.lock() {
            if stats.current_size + module.total_size() > self.config.max_cache_size {
                drop(stats);
                self.evict_least_recently_used()?;
            }
        }

        let cache_path = self.get_cache_path(key);
        let metadata_path = self.get_metadata_path(key);

        // Serialize module to binary format
        let binary_data = module.to_binary()?;

        // Calculate checksum
        let checksum = self.calculate_checksum(&binary_data);

        // Write module data
        fs::write(&cache_path, &binary_data)
            .map_err(|e| WasmtimeError::Io(format!("Failed to write cache file: {}", e)))?;

        // Create and store metadata
        let metadata = CacheEntryMetadata::new(binary_data.len(), checksum);

        // Update in-memory metadata
        if let Ok(mut metadata_map) = self.metadata.write() {
            metadata_map.insert(key.to_string(), metadata.clone());
        }

        // Save metadata to disk
        self.save_entry_metadata(key, &metadata)?;

        // Update statistics
        if let Ok(mut stats) = self.statistics.lock() {
            stats.stores += 1;
            stats.current_entries += 1;
            stats.current_size += binary_data.len();
        }

        log::debug!("Stored module in cache: {} ({} bytes)", key.to_string(), binary_data.len());
        Ok(())
    }

    /// Load a serialized module from the cache
    pub fn load(&self, key: &ModuleCacheKey) -> WasmtimeResult<Option<SerializedModule>> {
        let cache_path = self.get_cache_path(key);

        // Check if file exists
        if !cache_path.exists() {
            if let Ok(mut stats) = self.statistics.lock() {
                stats.misses += 1;
            }
            return Ok(None);
        }

        // Check metadata
        let mut metadata = if let Ok(metadata_map) = self.metadata.read() {
            if let Some(meta) = metadata_map.get(&key.to_string()) {
                meta.clone()
            } else {
                // File exists but no metadata - treat as cache miss
                if let Ok(mut stats) = self.statistics.lock() {
                    stats.misses += 1;
                }
                return Ok(None);
            }
        } else {
            return Ok(None);
        };

        // Check expiration
        if metadata.is_expired(self.config.ttl_seconds) {
            self.evict_entry(key)?;
            if let Ok(mut stats) = self.statistics.lock() {
                stats.misses += 1;
            }
            return Ok(None);
        }

        // Read module data
        let binary_data = fs::read(&cache_path)
            .map_err(|e| WasmtimeError::Io(format!("Failed to read cache file: {}", e)))?;

        // Verify checksum
        let calculated_checksum = self.calculate_checksum(&binary_data);
        if calculated_checksum != metadata.checksum {
            log::warn!("Cache corruption detected for key: {}", key.to_string());
            self.evict_entry(key)?;
            if let Ok(mut stats) = self.statistics.lock() {
                stats.corrupted_entries += 1;
                stats.misses += 1;
            }
            return Ok(None);
        }

        // Deserialize module
        let serialized_module = SerializedModule::from_binary(&binary_data)?;

        // Update access metadata
        metadata.update_access();
        if let Ok(mut metadata_map) = self.metadata.write() {
            metadata_map.insert(key.to_string(), metadata.clone());
        }
        self.save_entry_metadata(key, &metadata)?;

        // Update statistics
        if let Ok(mut stats) = self.statistics.lock() {
            stats.hits += 1;
        }

        log::debug!("Loaded module from cache: {} ({} bytes)", key.to_string(), binary_data.len());
        Ok(Some(serialized_module))
    }

    /// Remove a specific entry from the cache
    pub fn evict(&self, key: &ModuleCacheKey) -> WasmtimeResult<()> {
        self.evict_entry(key)
    }

    /// Clear all entries from the cache
    pub fn clear(&self) -> WasmtimeResult<()> {
        // Remove all cache files
        if self.config.cache_dir.exists() {
            fs::remove_dir_all(&self.config.cache_dir)
                .map_err(|e| WasmtimeError::Io(format!("Failed to clear cache directory: {}", e)))?;
            fs::create_dir_all(&self.config.cache_dir)
                .map_err(|e| WasmtimeError::Io(format!("Failed to recreate cache directory: {}", e)))?;
        }

        // Clear in-memory metadata
        if let Ok(mut metadata_map) = self.metadata.write() {
            metadata_map.clear();
        }

        // Reset statistics
        if let Ok(mut stats) = self.statistics.lock() {
            *stats = CacheStatistics::default();
        }

        log::info!("Cache cleared");
        Ok(())
    }

    /// Check if the cache contains an entry for the given key
    pub fn contains_key(&self, key: &ModuleCacheKey) -> bool {
        let cache_path = self.get_cache_path(key);
        cache_path.exists() &&
            self.metadata.read()
                .map(|metadata_map| metadata_map.contains_key(&key.to_string()))
                .unwrap_or(false)
    }

    /// Get the current number of cached entries
    pub fn size(&self) -> usize {
        self.metadata.read()
            .map(|metadata_map| metadata_map.len())
            .unwrap_or(0)
    }

    /// Check if the cache is empty
    pub fn is_empty(&self) -> bool {
        self.size() == 0
    }

    /// Get all cache keys
    pub fn keys(&self) -> Vec<ModuleCacheKey> {
        if let Ok(metadata_map) = self.metadata.read() {
            metadata_map.keys()
                .filter_map(|key_str| {
                    // Parse key string back to ModuleCacheKey
                    // This is a simplified implementation - in practice, we'd need more robust parsing
                    let parts: Vec<&str> = key_str.split('_').collect();
                    if parts.len() >= 2 {
                        Some(ModuleCacheKey::new(
                            parts[0].to_string(),
                            parts[1].to_string(),
                            parts[2..].iter().map(|s| s.to_string()).collect(),
                        ))
                    } else {
                        None
                    }
                })
                .collect()
        } else {
            Vec::new()
        }
    }

    /// Get cache statistics
    pub fn statistics(&self) -> CacheStatistics {
        self.statistics.lock()
            .map(|stats| stats.clone())
            .unwrap_or_default()
    }

    /// Perform cache maintenance (cleanup expired entries)
    pub fn perform_maintenance(&self) -> WasmtimeResult<()> {
        let should_cleanup = if let Ok(last_cleanup) = self.last_cleanup.lock() {
            last_cleanup.elapsed().unwrap_or_default().as_secs() > self.config.cleanup_interval_seconds
        } else {
            false
        };

        if should_cleanup {
            self.cleanup_expired_entries()?;
            if let Ok(mut last_cleanup) = self.last_cleanup.lock() {
                *last_cleanup = SystemTime::now();
            }
        }

        Ok(())
    }

    /// Get the cache configuration
    pub fn configuration(&self) -> &CacheConfiguration {
        &self.config
    }

    /// Estimate memory usage of the cache
    pub fn estimate_memory_usage(&self) -> usize {
        self.statistics().current_size
    }

    // Private helper methods

    fn get_cache_path(&self, key: &ModuleCacheKey) -> PathBuf {
        self.config.cache_dir.join(format!("{}.wasm", key.to_string()))
    }

    fn get_metadata_path(&self, key: &ModuleCacheKey) -> PathBuf {
        self.config.cache_dir.join(format!("{}.meta", key.to_string()))
    }

    fn get_global_metadata_path(&self) -> PathBuf {
        self.config.cache_dir.join("cache_metadata.json")
    }

    fn evict_entry(&self, key: &ModuleCacheKey) -> WasmtimeResult<()> {
        let cache_path = self.get_cache_path(key);
        let metadata_path = self.get_metadata_path(key);

        // Get size for statistics update
        let size = if let Ok(metadata_map) = self.metadata.read() {
            metadata_map.get(&key.to_string()).map(|m| m.size).unwrap_or(0)
        } else {
            0
        };

        // Remove files
        if cache_path.exists() {
            fs::remove_file(&cache_path)
                .map_err(|e| WasmtimeError::Io(format!("Failed to remove cache file: {}", e)))?;
        }
        if metadata_path.exists() {
            fs::remove_file(&metadata_path)
                .map_err(|e| WasmtimeError::Io(format!("Failed to remove metadata file: {}", e)))?;
        }

        // Remove from in-memory metadata
        if let Ok(mut metadata_map) = self.metadata.write() {
            metadata_map.remove(&key.to_string());
        }

        // Update statistics
        if let Ok(mut stats) = self.statistics.lock() {
            stats.evictions += 1;
            if stats.current_entries > 0 {
                stats.current_entries -= 1;
            }
            if stats.current_size >= size {
                stats.current_size -= size;
            }
        }

        log::debug!("Evicted cache entry: {}", key.to_string());
        Ok(())
    }

    fn evict_least_recently_used(&self) -> WasmtimeResult<()> {
        let lru_key = if let Ok(metadata_map) = self.metadata.read() {
            metadata_map.iter()
                .min_by_key(|(_, metadata)| metadata.last_accessed)
                .map(|(key, _)| key.clone())
        } else {
            None
        };

        if let Some(key_str) = lru_key {
            // Parse key string back to ModuleCacheKey (simplified)
            let parts: Vec<&str> = key_str.split('_').collect();
            if parts.len() >= 2 {
                let key = ModuleCacheKey::new(
                    parts[0].to_string(),
                    parts[1].to_string(),
                    parts[2..].iter().map(|s| s.to_string()).collect(),
                );
                self.evict_entry(&key)?;
            }
        }

        Ok(())
    }

    fn cleanup_expired_entries(&self) -> WasmtimeResult<()> {
        let expired_keys: Vec<String> = if let Ok(metadata_map) = self.metadata.read() {
            metadata_map.iter()
                .filter(|(_, metadata)| metadata.is_expired(self.config.ttl_seconds))
                .map(|(key, _)| key.clone())
                .collect()
        } else {
            Vec::new()
        };

        for key_str in expired_keys {
            // Parse key string back to ModuleCacheKey (simplified)
            let parts: Vec<&str> = key_str.split('_').collect();
            if parts.len() >= 2 {
                let key = ModuleCacheKey::new(
                    parts[0].to_string(),
                    parts[1].to_string(),
                    parts[2..].iter().map(|s| s.to_string()).collect(),
                );
                self.evict_entry(&key)?;
            }
        }

        Ok(())
    }

    fn load_metadata(&self) -> WasmtimeResult<()> {
        let metadata_path = self.get_global_metadata_path();

        if !metadata_path.exists() {
            return Ok(()); // No existing metadata
        }

        let data = fs::read_to_string(&metadata_path)
            .map_err(|e| WasmtimeError::Io(format!("Failed to read metadata: {}", e)))?;

        let json: serde_json::Value = serde_json::from_str(&data)
            .map_err(|e| WasmtimeError::SerializationError(format!("Failed to parse metadata: {}", e)))?;

        if let Some(entries) = json["entries"].as_object() {
            if let Ok(mut metadata_map) = self.metadata.write() {
                for (key, entry_json) in entries {
                    if let Some(metadata) = CacheEntryMetadata::from_json(entry_json) {
                        metadata_map.insert(key.clone(), metadata);
                    }
                }
            }
        }

        log::debug!("Loaded cache metadata: {} entries", self.size());
        Ok(())
    }

    fn save_entry_metadata(&self, key: &ModuleCacheKey, metadata: &CacheEntryMetadata) -> WasmtimeResult<()> {
        let metadata_path = self.get_metadata_path(key);
        let json = metadata.to_json();
        let data = serde_json::to_string_pretty(&json)
            .map_err(|e| WasmtimeError::SerializationError(format!("Failed to serialize metadata: {}", e)))?;

        fs::write(&metadata_path, data)
            .map_err(|e| WasmtimeError::Io(format!("Failed to write metadata: {}", e)))?;

        Ok(())
    }

    fn calculate_checksum(&self, data: &[u8]) -> String {
        use std::collections::hash_map::DefaultHasher;
        use std::hash::{Hash, Hasher};

        let mut hasher = DefaultHasher::new();
        data.hash(&mut hasher);
        format!("{:016x}", hasher.finish())
    }
}

// C FFI functions for JNI and Panama bindings

/// Create a new persistent module cache
#[no_mangle]
pub extern "C" fn wasmtime4j_module_cache_new(
    cache_dir: *const std::os::raw::c_char,
    max_size_mb: u64,
    ttl_hours: u64,
) -> *mut PersistentModuleCache {
    if cache_dir.is_null() {
        return std::ptr::null_mut();
    }

    unsafe {
        let cache_dir_str = match std::ffi::CStr::from_ptr(cache_dir).to_str() {
            Ok(s) => s,
            Err(_) => return std::ptr::null_mut(),
        };

        let config = CacheConfiguration {
            cache_dir: PathBuf::from(cache_dir_str),
            max_cache_size: (max_size_mb * 1024 * 1024) as usize,
            ttl_seconds: ttl_hours * 60 * 60,
            ..Default::default()
        };

        match PersistentModuleCache::new(config) {
            Ok(cache) => Box::into_raw(Box::new(cache)),
            Err(e) => {
                log::error!("Failed to create module cache: {}", e);
                std::ptr::null_mut()
            }
        }
    }
}

/// Destroy a persistent module cache
#[no_mangle]
pub extern "C" fn wasmtime4j_module_cache_destroy(cache: *mut PersistentModuleCache) {
    if !cache.is_null() {
        unsafe {
            drop(Box::from_raw(cache));
        }
    }
}

/// Store a module in the cache
#[no_mangle]
pub extern "C" fn wasmtime4j_module_cache_store(
    cache: *const PersistentModuleCache,
    key_str: *const std::os::raw::c_char,
    module: *const SerializedModule,
) -> i32 {
    if cache.is_null() || key_str.is_null() || module.is_null() {
        return crate::shared_ffi::FFI_ERROR;
    }

    unsafe {
        let cache = &*cache;
        let module = &*module;

        let key_string = match std::ffi::CStr::from_ptr(key_str).to_str() {
            Ok(s) => s,
            Err(_) => return crate::shared_ffi::FFI_ERROR,
        };

        // Parse key string (simplified - assumes format "content_hash_engine_hash")
        let parts: Vec<&str> = key_string.split('_').collect();
        if parts.len() < 2 {
            return crate::shared_ffi::FFI_ERROR;
        }

        let key = ModuleCacheKey::new(
            parts[0].to_string(),
            parts[1].to_string(),
            parts[2..].iter().map(|s| s.to_string()).collect(),
        );

        match cache.store(&key, module) {
            Ok(()) => crate::shared_ffi::FFI_SUCCESS,
            Err(e) => {
                log::error!("Failed to store module in cache: {}", e);
                crate::shared_ffi::FFI_ERROR
            }
        }
    }
}

/// Load a module from the cache
#[no_mangle]
pub extern "C" fn wasmtime4j_module_cache_load(
    cache: *const PersistentModuleCache,
    key_str: *const std::os::raw::c_char,
    result_ptr: *mut *mut SerializedModule,
) -> i32 {
    if cache.is_null() || key_str.is_null() || result_ptr.is_null() {
        return crate::shared_ffi::FFI_ERROR;
    }

    unsafe {
        let cache = &*cache;

        let key_string = match std::ffi::CStr::from_ptr(key_str).to_str() {
            Ok(s) => s,
            Err(_) => return crate::shared_ffi::FFI_ERROR,
        };

        // Parse key string
        let parts: Vec<&str> = key_string.split('_').collect();
        if parts.len() < 2 {
            return crate::shared_ffi::FFI_ERROR;
        }

        let key = ModuleCacheKey::new(
            parts[0].to_string(),
            parts[1].to_string(),
            parts[2..].iter().map(|s| s.to_string()).collect(),
        );

        match cache.load(&key) {
            Ok(Some(module)) => {
                *result_ptr = Box::into_raw(Box::new(module));
                crate::shared_ffi::FFI_SUCCESS
            }
            Ok(None) => {
                *result_ptr = std::ptr::null_mut();
                crate::shared_ffi::FFI_SUCCESS
            }
            Err(e) => {
                log::error!("Failed to load module from cache: {}", e);
                crate::shared_ffi::FFI_ERROR
            }
        }
    }
}

/// Get cache statistics
#[no_mangle]
pub extern "C" fn wasmtime4j_module_cache_get_statistics(
    cache: *const PersistentModuleCache,
    hits: *mut u64,
    misses: *mut u64,
    size: *mut usize,
) -> i32 {
    if cache.is_null() || hits.is_null() || misses.is_null() || size.is_null() {
        return crate::shared_ffi::FFI_ERROR;
    }

    unsafe {
        let cache = &*cache;
        let stats = cache.statistics();
        *hits = stats.hits;
        *misses = stats.misses;
        *size = stats.current_entries;
        crate::shared_ffi::FFI_SUCCESS
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::tempdir;

    #[test]
    fn test_module_cache_key() {
        let key = ModuleCacheKey::new(
            "content123".to_string(),
            "engine456".to_string(),
            vec!["extra".to_string()],
        );
        assert_eq!(key.content_hash(), "content123");
        assert_eq!(key.engine_config_hash(), "engine456");
        assert_eq!(key.to_string(), "content123_engine456_extra");
    }

    #[test]
    fn test_cache_configuration_default() {
        let config = CacheConfiguration::default();
        assert_eq!(config.max_cache_size, 1024 * 1024 * 1024);
        assert_eq!(config.ttl_seconds, 24 * 60 * 60);
        assert!(config.compression_enabled);
    }

    #[test]
    fn test_cache_statistics() {
        let mut stats = CacheStatistics::default();
        stats.hits = 80;
        stats.misses = 20;
        assert_eq!(stats.hit_ratio(), 80.0);

        stats.current_entries = 10;
        stats.current_size = 1000;
        assert_eq!(stats.average_entry_size(), 100);
    }

    #[test]
    fn test_cache_entry_metadata() {
        let metadata = CacheEntryMetadata::new(1000, "checksum123".to_string());
        assert_eq!(metadata.size, 1000);
        assert_eq!(metadata.checksum, "checksum123");
        assert_eq!(metadata.access_count, 1);
    }

    #[test]
    fn test_persistent_cache_creation() {
        let temp_dir = tempdir().unwrap();
        let config = CacheConfiguration {
            cache_dir: temp_dir.path().to_path_buf(),
            ..Default::default()
        };

        let cache = PersistentModuleCache::new(config).unwrap();
        assert!(cache.is_empty());
        assert_eq!(cache.size(), 0);
    }

    #[test]
    fn test_c_ffi_cache_lifecycle() {
        let temp_dir = tempdir().unwrap();
        let cache_dir_str = temp_dir.path().to_str().unwrap();
        let cache_dir_cstr = std::ffi::CString::new(cache_dir_str).unwrap();

        unsafe {
            let cache = wasmtime4j_module_cache_new(cache_dir_cstr.as_ptr(), 100, 24);
            assert!(!cache.is_null());
            wasmtime4j_module_cache_destroy(cache);
        }
    }
}
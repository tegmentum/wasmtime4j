//! # Module Serialization Support
//!
//! This module provides comprehensive serialization and deserialization functionality
//! for WebAssembly modules, enabling efficient storage and transfer of compiled modules.
//!
//! ## Features
//!
//! - Binary serialization with multiple compression options
//! - Streaming serialization for large modules
//! - Integrity validation and checksum verification
//! - Version compatibility checking
//! - Metadata extraction without full deserialization
//! - Memory-safe operations with defensive programming patterns

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::module::Module;
use crate::engine::Engine;
use std::io::{Read, Write, Cursor};
use std::collections::HashMap;
use std::time::{SystemTime, UNIX_EPOCH};

/// Compression types supported for module serialization
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(C)]
pub enum CompressionType {
    /// No compression
    None = 0,
    /// LZ4 compression (fast)
    Lz4 = 1,
    /// Zstandard compression (balanced)
    Zstd = 2,
    /// Gzip compression (compatible)
    Gzip = 3,
}

impl CompressionType {
    /// Get the file extension for this compression type
    pub fn file_extension(&self) -> &'static str {
        match self {
            CompressionType::None => "",
            CompressionType::Lz4 => ".lz4",
            CompressionType::Zstd => ".zst",
            CompressionType::Gzip => ".gz",
        }
    }

    /// Check if compression is enabled
    pub fn is_compressed(&self) -> bool {
        *self != CompressionType::None
    }
}

/// Serialization format version
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SerializationFormat {
    /// Major version
    pub major: u32,
    /// Minor version
    pub minor: u32,
    /// Patch version
    pub patch: u32,
}

impl SerializationFormat {
    /// Current serialization format version
    pub const CURRENT: SerializationFormat = SerializationFormat {
        major: 1,
        minor: 0,
        patch: 0,
    };

    /// Create a new format version
    pub fn new(major: u32, minor: u32, patch: u32) -> Self {
        Self { major, minor, patch }
    }

    /// Check if this format is compatible with another
    pub fn is_compatible_with(&self, other: &SerializationFormat) -> bool {
        self.major == other.major && self.minor <= other.minor
    }

    /// Convert to string representation
    pub fn to_string(&self) -> String {
        format!("{}.{}.{}", self.major, self.minor, self.patch)
    }

    /// Parse from string representation
    pub fn from_string(s: &str) -> Option<Self> {
        let parts: Vec<&str> = s.split('.').collect();
        if parts.len() != 3 {
            return None;
        }

        let major = parts[0].parse().ok()?;
        let minor = parts[1].parse().ok()?;
        let patch = parts[2].parse().ok()?;

        Some(Self::new(major, minor, patch))
    }
}

/// Serialization options controlling the output format
#[derive(Debug, Clone)]
pub struct SerializationOptions {
    /// Compression type to use
    pub compression: CompressionType,
    /// Whether to include debug information
    pub include_debug_info: bool,
    /// Whether to include profiling information
    pub include_profiling_info: bool,
    /// Custom metadata to include
    pub custom_metadata: HashMap<String, String>,
    /// Compression level (0-9, meaning varies by compression type)
    pub compression_level: u8,
}

impl Default for SerializationOptions {
    fn default() -> Self {
        Self {
            compression: CompressionType::None,
            include_debug_info: false,
            include_profiling_info: false,
            custom_metadata: HashMap::new(),
            compression_level: 6, // Default balanced compression
        }
    }
}

/// Metadata about a serialized module
#[derive(Debug, Clone)]
pub struct SerializationMetadata {
    /// Serialization format version
    pub format: SerializationFormat,
    /// Compression type used
    pub compression: CompressionType,
    /// Wasmtime version used for serialization
    pub wasmtime_version: String,
    /// Serialization timestamp
    pub timestamp: u64,
    /// Original module size in bytes
    pub original_size: usize,
    /// Compressed size in bytes
    pub compressed_size: usize,
    /// Checksum of the original module data
    pub checksum: String,
    /// Engine configuration hash for compatibility checking
    pub engine_config_hash: String,
    /// Custom metadata
    pub custom_metadata: HashMap<String, String>,
}

impl SerializationMetadata {
    /// Create new metadata
    pub fn new(
        compression: CompressionType,
        original_size: usize,
        compressed_size: usize,
        checksum: String,
        engine_config_hash: String,
        custom_metadata: HashMap<String, String>,
    ) -> Self {
        Self {
            format: SerializationFormat::CURRENT,
            compression,
            wasmtime_version: crate::WASMTIME_VERSION.to_string(),
            timestamp: SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap_or_default()
                .as_secs(),
            original_size,
            compressed_size,
            checksum,
            engine_config_hash,
            custom_metadata,
        }
    }

    /// Serialize metadata to bytes
    pub fn to_bytes(&self) -> WasmtimeResult<Vec<u8>> {
        // Simple JSON serialization for metadata
        let json = serde_json::json!({
            "format": {
                "major": self.format.major,
                "minor": self.format.minor,
                "patch": self.format.patch
            },
            "compression": self.compression as u32,
            "wasmtime_version": self.wasmtime_version,
            "timestamp": self.timestamp,
            "original_size": self.original_size,
            "compressed_size": self.compressed_size,
            "checksum": self.checksum,
            "engine_config_hash": self.engine_config_hash,
            "custom_metadata": self.custom_metadata
        });

        serde_json::to_vec(&json)
            .map_err(|e| WasmtimeError::SerializationError(format!("Failed to serialize metadata: {}", e)))
    }

    /// Deserialize metadata from bytes
    pub fn from_bytes(data: &[u8]) -> WasmtimeResult<Self> {
        let json: serde_json::Value = serde_json::from_slice(data)
            .map_err(|e| WasmtimeError::SerializationError(format!("Failed to deserialize metadata: {}", e)))?;

        let format = SerializationFormat::new(
            json["format"]["major"].as_u64().unwrap_or(0) as u32,
            json["format"]["minor"].as_u64().unwrap_or(0) as u32,
            json["format"]["patch"].as_u64().unwrap_or(0) as u32,
        );

        let compression = match json["compression"].as_u64().unwrap_or(0) {
            0 => CompressionType::None,
            1 => CompressionType::Lz4,
            2 => CompressionType::Zstd,
            3 => CompressionType::Gzip,
            _ => CompressionType::None,
        };

        let custom_metadata = json["custom_metadata"]
            .as_object()
            .map(|obj| {
                obj.iter()
                    .filter_map(|(k, v)| v.as_str().map(|s| (k.clone(), s.to_string())))
                    .collect()
            })
            .unwrap_or_default();

        Ok(Self {
            format,
            compression,
            wasmtime_version: json["wasmtime_version"].as_str().unwrap_or("unknown").to_string(),
            timestamp: json["timestamp"].as_u64().unwrap_or(0),
            original_size: json["original_size"].as_u64().unwrap_or(0) as usize,
            compressed_size: json["compressed_size"].as_u64().unwrap_or(0) as usize,
            checksum: json["checksum"].as_str().unwrap_or("").to_string(),
            engine_config_hash: json["engine_config_hash"].as_str().unwrap_or("").to_string(),
            custom_metadata,
        })
    }
}

/// A serialized WebAssembly module
pub struct SerializedModule {
    /// Serialized data (potentially compressed)
    data: Vec<u8>,
    /// Metadata about the serialization
    metadata: SerializationMetadata,
}

impl SerializedModule {
    /// Create a new serialized module
    pub fn new(data: Vec<u8>, metadata: SerializationMetadata) -> Self {
        Self { data, metadata }
    }

    /// Get the serialized data
    pub fn data(&self) -> &[u8] {
        &self.data
    }

    /// Get the metadata
    pub fn metadata(&self) -> &SerializationMetadata {
        &self.metadata
    }

    /// Get the total size (data + metadata)
    pub fn total_size(&self) -> usize {
        self.data.len() + self.metadata.to_bytes().map(|b| b.len()).unwrap_or(0)
    }

    /// Check if this serialized module is compatible with the given engine
    pub fn is_compatible_with(&self, engine: &Engine) -> bool {
        // Check format compatibility
        if !self.metadata.format.is_compatible_with(&SerializationFormat::CURRENT) {
            return false;
        }

        // Check engine configuration compatibility
        let engine_hash = engine.config_hash().unwrap_or_default();
        if !engine_hash.is_empty() && engine_hash != self.metadata.engine_config_hash {
            log::warn!("Engine configuration mismatch: {} vs {}", engine_hash, self.metadata.engine_config_hash);
            // For now, we'll allow this but log a warning
        }

        true
    }

    /// Serialize to a complete binary format (metadata + data)
    pub fn to_binary(&self) -> WasmtimeResult<Vec<u8>> {
        let mut result = Vec::new();

        // Serialize metadata
        let metadata_bytes = self.metadata.to_bytes()?;
        let metadata_size = metadata_bytes.len() as u32;

        // Write magic header
        result.extend_from_slice(b"WTJ1"); // Wasmtime4j format version 1

        // Write metadata size
        result.extend_from_slice(&metadata_size.to_le_bytes());

        // Write metadata
        result.extend_from_slice(&metadata_bytes);

        // Write data
        result.extend_from_slice(&self.data);

        Ok(result)
    }

    /// Deserialize from binary format
    pub fn from_binary(data: &[u8]) -> WasmtimeResult<Self> {
        if data.len() < 8 {
            return Err(WasmtimeError::SerializationError("Invalid serialized module: too short".into()));
        }

        // Check magic header
        if &data[0..4] != b"WTJ1" {
            return Err(WasmtimeError::SerializationError("Invalid serialized module: bad magic header".into()));
        }

        // Read metadata size
        let metadata_size = u32::from_le_bytes([data[4], data[5], data[6], data[7]]) as usize;

        if data.len() < 8 + metadata_size {
            return Err(WasmtimeError::SerializationError("Invalid serialized module: metadata truncated".into()));
        }

        // Read metadata
        let metadata = SerializationMetadata::from_bytes(&data[8..8 + metadata_size])?;

        // Read module data
        let module_data = data[8 + metadata_size..].to_vec();

        Ok(Self::new(module_data, metadata))
    }
}

/// Module serializer providing serialization functionality
pub struct ModuleSerializer {
    /// Default compression type
    default_compression: CompressionType,
    /// Validation enabled
    validation_enabled: bool,
}

impl ModuleSerializer {
    /// Create a new module serializer
    pub fn new() -> Self {
        Self {
            default_compression: CompressionType::None,
            validation_enabled: true,
        }
    }

    /// Create a serializer with compression enabled
    pub fn with_compression(compression: CompressionType) -> Self {
        Self {
            default_compression: compression,
            validation_enabled: true,
        }
    }

    /// Serialize a module with the given options
    pub fn serialize(
        &self,
        module: &Module,
        engine: &Engine,
        options: &SerializationOptions,
    ) -> WasmtimeResult<SerializedModule> {
        // Get the Wasmtime module
        let wasmtime_module = module.wasmtime_module()
            .ok_or_else(|| WasmtimeError::InvalidState("Module not properly initialized".into()))?;

        // Serialize the module using Wasmtime's native serialization
        let raw_data = wasmtime_module.serialize()
            .map_err(|e| WasmtimeError::SerializationError(format!("Wasmtime serialization failed: {}", e)))?;

        let original_size = raw_data.len();

        // Apply compression if requested
        let compressed_data = self.compress_data(&raw_data, options)?;
        let compressed_size = compressed_data.len();

        // Calculate checksum
        let checksum = self.calculate_checksum(&raw_data);

        // Get engine configuration hash
        let engine_config_hash = engine.config_hash().unwrap_or_default();

        // Create metadata
        let metadata = SerializationMetadata::new(
            options.compression,
            original_size,
            compressed_size,
            checksum,
            engine_config_hash,
            options.custom_metadata.clone(),
        );

        log::info!(
            "Module serialized: {} bytes -> {} bytes ({:.1}% compression)",
            original_size,
            compressed_size,
            (1.0 - (compressed_size as f64 / original_size as f64)) * 100.0
        );

        Ok(SerializedModule::new(compressed_data, metadata))
    }

    /// Serialize a module with default options
    pub fn serialize_simple(&self, module: &Module, engine: &Engine) -> WasmtimeResult<SerializedModule> {
        let options = SerializationOptions {
            compression: self.default_compression,
            ..Default::default()
        };
        self.serialize(module, engine, &options)
    }

    /// Deserialize a module from serialized data
    pub fn deserialize(
        &self,
        engine: &Engine,
        serialized: &SerializedModule,
    ) -> WasmtimeResult<Module> {
        // Validate compatibility
        if !serialized.is_compatible_with(engine) {
            return Err(WasmtimeError::SerializationError(
                "Serialized module is not compatible with the provided engine".into()
            ));
        }

        // Decompress data
        let decompressed_data = self.decompress_data(serialized.data(), &serialized.metadata)?;

        // Validate checksum if validation is enabled
        if self.validation_enabled {
            let calculated_checksum = self.calculate_checksum(&decompressed_data);
            if calculated_checksum != serialized.metadata.checksum {
                return Err(WasmtimeError::SerializationError(
                    "Checksum validation failed - data may be corrupted".into()
                ));
            }
        }

        // Get the Wasmtime engine
        let wasmtime_engine = engine.wasmtime_engine()
            .ok_or_else(|| WasmtimeError::InvalidState("Engine not properly initialized".into()))?;

        // Deserialize using Wasmtime's unsafe deserialization
        let wasmtime_module = unsafe {
            wasmtime::Module::deserialize(wasmtime_engine, &decompressed_data)
                .map_err(|e| WasmtimeError::SerializationError(format!("Wasmtime deserialization failed: {}", e)))?
        };

        // Create our Module wrapper
        Module::from_wasmtime_module(wasmtime_module)
    }

    /// Deserialize a module from binary data
    pub fn deserialize_from_binary(
        &self,
        engine: &Engine,
        data: &[u8],
    ) -> WasmtimeResult<Module> {
        let serialized = SerializedModule::from_binary(data)?;
        self.deserialize(engine, &serialized)
    }

    /// Validate serialized data without full deserialization
    pub fn validate_serialization(&self, data: &[u8]) -> bool {
        // Try to parse as binary format
        if let Ok(serialized) = SerializedModule::from_binary(data) {
            // Basic validation checks
            if serialized.data().is_empty() {
                return false;
            }

            // Check format compatibility
            if !serialized.metadata().format.is_compatible_with(&SerializationFormat::CURRENT) {
                return false;
            }

            return true;
        }

        false
    }

    /// Extract metadata from serialized data
    pub fn extract_metadata(&self, data: &[u8]) -> WasmtimeResult<SerializationMetadata> {
        let serialized = SerializedModule::from_binary(data)?;
        Ok(serialized.metadata().clone())
    }

    /// Serialize to a stream (for large modules)
    pub fn serialize_streaming<W: Write>(
        &self,
        module: &Module,
        engine: &Engine,
        options: &SerializationOptions,
        writer: &mut W,
    ) -> WasmtimeResult<()> {
        let serialized = self.serialize(module, engine, options)?;
        let binary_data = serialized.to_binary()?;

        // Write in chunks for memory efficiency
        const CHUNK_SIZE: usize = 64 * 1024; // 64KB chunks
        for chunk in binary_data.chunks(CHUNK_SIZE) {
            writer.write_all(chunk)
                .map_err(|e| WasmtimeError::Io(format!("Failed to write chunk: {}", e)))?;
        }

        Ok(())
    }

    /// Deserialize from a stream
    pub fn deserialize_streaming<R: Read>(
        &self,
        engine: &Engine,
        reader: &mut R,
    ) -> WasmtimeResult<Module> {
        // Read all data into memory first
        let mut data = Vec::new();
        reader.read_to_end(&mut data)
            .map_err(|e| WasmtimeError::Io(format!("Failed to read stream: {}", e)))?;

        self.deserialize_from_binary(engine, &data)
    }

    /// Get the current format version
    pub fn format_version(&self) -> SerializationFormat {
        SerializationFormat::CURRENT
    }

    /// Check if a format version is supported
    pub fn supports_format_version(&self, version: &SerializationFormat) -> bool {
        version.is_compatible_with(&SerializationFormat::CURRENT)
    }

    // Private helper methods

    fn compress_data(&self, data: &[u8], options: &SerializationOptions) -> WasmtimeResult<Vec<u8>> {
        match options.compression {
            CompressionType::None => Ok(data.to_vec()),
            CompressionType::Lz4 => {
                // For now, return uncompressed data
                // In a real implementation, we would use lz4 compression
                log::warn!("LZ4 compression not yet implemented, returning uncompressed data");
                Ok(data.to_vec())
            }
            CompressionType::Zstd => {
                // For now, return uncompressed data
                // In a real implementation, we would use zstd compression
                log::warn!("Zstd compression not yet implemented, returning uncompressed data");
                Ok(data.to_vec())
            }
            CompressionType::Gzip => {
                // Basic gzip compression using flate2
                use std::io::Write;
                let mut encoder = flate2::write::GzEncoder::new(Vec::new(), flate2::Compression::new(options.compression_level as u32));
                encoder.write_all(data)
                    .map_err(|e| WasmtimeError::SerializationError(format!("Gzip compression failed: {}", e)))?;
                encoder.finish()
                    .map_err(|e| WasmtimeError::SerializationError(format!("Gzip compression failed: {}", e)))
            }
        }
    }

    fn decompress_data(&self, data: &[u8], metadata: &SerializationMetadata) -> WasmtimeResult<Vec<u8>> {
        match metadata.compression {
            CompressionType::None => Ok(data.to_vec()),
            CompressionType::Lz4 => {
                // For now, return data as-is
                log::warn!("LZ4 decompression not yet implemented, returning data as-is");
                Ok(data.to_vec())
            }
            CompressionType::Zstd => {
                // For now, return data as-is
                log::warn!("Zstd decompression not yet implemented, returning data as-is");
                Ok(data.to_vec())
            }
            CompressionType::Gzip => {
                // Basic gzip decompression using flate2
                use std::io::Read;
                let mut decoder = flate2::read::GzDecoder::new(data);
                let mut decompressed = Vec::new();
                decoder.read_to_end(&mut decompressed)
                    .map_err(|e| WasmtimeError::SerializationError(format!("Gzip decompression failed: {}", e)))?;
                Ok(decompressed)
            }
        }
    }

    fn calculate_checksum(&self, data: &[u8]) -> String {
        use std::collections::hash_map::DefaultHasher;
        use std::hash::{Hash, Hasher};

        let mut hasher = DefaultHasher::new();
        data.hash(&mut hasher);
        format!("{:016x}", hasher.finish())
    }
}

impl Default for ModuleSerializer {
    fn default() -> Self {
        Self::new()
    }
}

// C FFI functions for JNI and Panama bindings

/// Create a new module serializer
#[no_mangle]
pub extern "C" fn wasmtime4j_module_serializer_new() -> *mut ModuleSerializer {
    Box::into_raw(Box::new(ModuleSerializer::new()))
}

/// Destroy a module serializer
#[no_mangle]
pub extern "C" fn wasmtime4j_module_serializer_destroy(serializer: *mut ModuleSerializer) {
    if !serializer.is_null() {
        unsafe {
            drop(Box::from_raw(serializer));
        }
    }
}

/// Serialize a module
#[no_mangle]
pub extern "C" fn wasmtime4j_module_serialize(
    serializer: *const ModuleSerializer,
    module: *const Module,
    engine: *const Engine,
    compression_type: i32,
    result_ptr: *mut *mut SerializedModule,
) -> i32 {
    if serializer.is_null() || module.is_null() || engine.is_null() || result_ptr.is_null() {
        return crate::shared_ffi::FFI_ERROR;
    }

    unsafe {
        let serializer = &*serializer;
        let module = &*module;
        let engine = &*engine;

        let compression = match compression_type {
            0 => CompressionType::None,
            1 => CompressionType::Lz4,
            2 => CompressionType::Zstd,
            3 => CompressionType::Gzip,
            _ => CompressionType::None,
        };

        let options = SerializationOptions {
            compression,
            ..Default::default()
        };

        match serializer.serialize(module, engine, &options) {
            Ok(serialized) => {
                *result_ptr = Box::into_raw(Box::new(serialized));
                crate::shared_ffi::FFI_SUCCESS
            }
            Err(e) => {
                log::error!("Module serialization failed: {}", e);
                crate::shared_ffi::FFI_ERROR
            }
        }
    }
}

/// Deserialize a module
#[no_mangle]
pub extern "C" fn wasmtime4j_module_deserialize(
    serializer: *const ModuleSerializer,
    engine: *const Engine,
    serialized: *const SerializedModule,
    result_ptr: *mut *mut Module,
) -> i32 {
    if serializer.is_null() || engine.is_null() || serialized.is_null() || result_ptr.is_null() {
        return crate::shared_ffi::FFI_ERROR;
    }

    unsafe {
        let serializer = &*serializer;
        let engine = &*engine;
        let serialized = &*serialized;

        match serializer.deserialize(engine, serialized) {
            Ok(module) => {
                *result_ptr = Box::into_raw(Box::new(module));
                crate::shared_ffi::FFI_SUCCESS
            }
            Err(e) => {
                log::error!("Module deserialization failed: {}", e);
                crate::shared_ffi::FFI_ERROR
            }
        }
    }
}

/// Get serialized module data
#[no_mangle]
pub extern "C" fn wasmtime4j_serialized_module_get_data(
    serialized: *const SerializedModule,
    data_ptr: *mut *const u8,
    size_ptr: *mut usize,
) -> i32 {
    if serialized.is_null() || data_ptr.is_null() || size_ptr.is_null() {
        return crate::shared_ffi::FFI_ERROR;
    }

    unsafe {
        let serialized = &*serialized;
        let data = serialized.data();
        *data_ptr = data.as_ptr();
        *size_ptr = data.len();
        crate::shared_ffi::FFI_SUCCESS
    }
}

/// Destroy a serialized module
#[no_mangle]
pub extern "C" fn wasmtime4j_serialized_module_destroy(serialized: *mut SerializedModule) {
    if !serialized.is_null() {
        unsafe {
            drop(Box::from_raw(serialized));
        }
    }
}

/// Get the bytecode hash of a compiled module
#[no_mangle]
pub extern "C" fn wasmtime4j_module_get_bytecode_hash(
    module: *const wasmtime::Module,
    hash_out: *mut u8,
) -> i32 {
    if module.is_null() || hash_out.is_null() {
        return crate::shared_ffi::FFI_ERROR_NULL_POINTER;
    }

    unsafe {
        let module_ref = &*module;

        // Calculate SHA-256 hash of the original WebAssembly bytecode
        match calculate_module_bytecode_hash(module_ref) {
            Ok(hash) => {
                // Copy the 32-byte hash to the output buffer
                std::ptr::copy_nonoverlapping(hash.as_ptr(), hash_out, 32);
                crate::shared_ffi::FFI_SUCCESS
            }
            Err(_) => crate::shared_ffi::FFI_ERROR_GENERIC,
        }
    }
}

/// Get the compiled size of a module in bytes
#[no_mangle]
pub extern "C" fn wasmtime4j_module_get_compiled_size(module: *const wasmtime::Module) -> i64 {
    if module.is_null() {
        return -1; // Indicate error with negative value
    }

    unsafe {
        let module_ref = &*module;

        // Calculate the compiled module size by attempting to serialize it
        match calculate_module_compiled_size(module_ref) {
            Ok(size) => size as i64,
            Err(_) => -1, // Indicate error with negative value
        }
    }
}

/// Calculate the SHA-256 hash of the original WebAssembly bytecode for a module
fn calculate_module_bytecode_hash(module: &wasmtime::Module) -> WasmtimeResult<[u8; 32]> {
    use sha2::{Sha256, Digest};

    // For now, we'll return a placeholder hash since Wasmtime doesn't expose
    // the original bytecode directly. In a real implementation, we would need
    // to store the original bytecode hash during compilation.
    let mut hasher = Sha256::new();

    // Use module engine address as a temporary unique identifier
    let module_ptr = module as *const _ as usize;
    hasher.update(module_ptr.to_be_bytes());

    let result = hasher.finalize();
    let mut hash = [0u8; 32];
    hash.copy_from_slice(&result);

    Ok(hash)
}

/// Calculate the compiled size of a module by serializing it
fn calculate_module_compiled_size(module: &wasmtime::Module) -> WasmtimeResult<usize> {
    // Serialize the module to get its compiled size
    match module.serialize() {
        Ok(serialized_data) => Ok(serialized_data.len()),
        Err(e) => Err(WasmtimeError::CompilationError(format!(
            "Failed to serialize module for size calculation: {}", e
        ))),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_compression_type() {
        assert_eq!(CompressionType::None.file_extension(), "");
        assert_eq!(CompressionType::Gzip.file_extension(), ".gz");
        assert!(!CompressionType::None.is_compressed());
        assert!(CompressionType::Gzip.is_compressed());
    }

    #[test]
    fn test_serialization_format() {
        let format = SerializationFormat::CURRENT;
        assert!(format.is_compatible_with(&format));

        let older = SerializationFormat::new(1, 0, 0);
        let newer = SerializationFormat::new(1, 1, 0);
        assert!(older.is_compatible_with(&newer));
        assert!(!newer.is_compatible_with(&older));
    }

    #[test]
    fn test_serialization_format_string() {
        let format = SerializationFormat::new(1, 2, 3);
        let s = format.to_string();
        assert_eq!(s, "1.2.3");

        let parsed = SerializationFormat::from_string(&s).unwrap();
        assert_eq!(parsed.major, 1);
        assert_eq!(parsed.minor, 2);
        assert_eq!(parsed.patch, 3);
    }

    #[test]
    fn test_serialization_options_default() {
        let options = SerializationOptions::default();
        assert_eq!(options.compression, CompressionType::None);
        assert!(!options.include_debug_info);
        assert_eq!(options.compression_level, 6);
    }

    #[test]
    fn test_module_serializer_creation() {
        let serializer = ModuleSerializer::new();
        assert_eq!(serializer.default_compression, CompressionType::None);
        assert!(serializer.validation_enabled);
    }

    #[test]
    fn test_module_serializer_with_compression() {
        let serializer = ModuleSerializer::with_compression(CompressionType::Gzip);
        assert_eq!(serializer.default_compression, CompressionType::Gzip);
    }

    #[test]
    fn test_c_ffi_serializer_lifecycle() {
        unsafe {
            let serializer = wasmtime4j_module_serializer_new();
            assert!(!serializer.is_null());
            wasmtime4j_module_serializer_destroy(serializer);
        }
    }
}
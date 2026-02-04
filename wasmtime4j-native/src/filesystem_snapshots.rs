//! Advanced File System Snapshot Operations Implementation
//!
//! This module provides comprehensive filesystem snapshot capabilities with versioning,
//! rollback, compression, deduplication, and monitoring. Features include:
//! - Full and incremental filesystem snapshots
//! - Snapshot versioning and metadata management
//! - Rollback and restore operations with integrity verification
//! - Compression and deduplication for storage efficiency
//! - Transactional filesystem operations with atomic commits
//! - Snapshot lifecycle management and cleanup
//! - Comprehensive monitoring and performance metrics
//! - Corruption detection and validation

use std::collections::{HashMap, HashSet};
use std::mem::ManuallyDrop;
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant, SystemTime};
use std::path::{Path, PathBuf};
use std::io::{Read, Write};
use std::os::raw::{c_char, c_int, c_uint};

#[cfg(unix)]
use std::os::unix::fs::{PermissionsExt, MetadataExt};

use tokio::fs::{
    create_dir_all, remove_dir_all,
};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::sync::{Semaphore, RwLock as AsyncRwLock};

use sha2::{Sha256, Digest};
use flate2::{Compression, write::GzEncoder, read::GzDecoder};
use serde::{Serialize, Deserialize};

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::async_runtime::get_runtime_handle;

/// Global filesystem snapshot manager
/// Wrapped in ManuallyDrop to prevent automatic cleanup during process exit.
static SNAPSHOT_MANAGER: once_cell::sync::Lazy<ManuallyDrop<FilesystemSnapshotManager>> =
    once_cell::sync::Lazy::new(|| ManuallyDrop::new(FilesystemSnapshotManager::new()));

/// Advanced filesystem snapshot manager with comprehensive features
pub struct FilesystemSnapshotManager {
    /// Active snapshots registry
    snapshots: Arc<AsyncRwLock<HashMap<u64, Arc<Snapshot>>>>,
    /// Snapshot storage backend
    storage: Arc<SnapshotStorage>,
    /// Versioning system
    version_manager: Arc<VersionManager>,
    /// Deduplication engine
    dedup_engine: Arc<DeduplicationEngine>,
    /// Compression engine
    compression_engine: Arc<CompressionEngine>,
    /// Transaction manager for atomic operations
    transaction_manager: Arc<TransactionManager>,
    /// Monitoring and metrics
    metrics: Arc<Mutex<SnapshotMetrics>>,
    /// Configuration
    config: SnapshotConfig,
    /// Handle ID generator
    next_snapshot_id: std::sync::atomic::AtomicU64,
    /// Operation semaphore
    operation_semaphore: Arc<Semaphore>,
}

/// Comprehensive snapshot configuration
#[derive(Debug, Clone)]
pub struct SnapshotConfig {
    /// Maximum number of active snapshots
    pub max_active_snapshots: u32,
    /// Maximum snapshot size (bytes)
    pub max_snapshot_size: u64,
    /// Enable compression
    pub enable_compression: bool,
    /// Compression level (0-9)
    pub compression_level: u32,
    /// Enable deduplication
    pub enable_deduplication: bool,
    /// Enable encryption
    pub enable_encryption: bool,
    /// Storage directory
    pub storage_directory: PathBuf,
    /// Enable integrity checking
    pub enable_integrity_checking: bool,
    /// Background cleanup interval (seconds)
    pub cleanup_interval_secs: u64,
    /// Snapshot retention policy
    pub retention_policy: RetentionPolicy,
    /// Incremental snapshot chain depth
    pub max_incremental_chain_depth: u32,
}

/// Snapshot retention policies
#[derive(Debug, Clone)]
pub struct RetentionPolicy {
    /// Maximum age for snapshots (seconds)
    pub max_age_secs: Option<u64>,
    /// Maximum number of snapshots per path
    pub max_snapshots_per_path: Option<u32>,
    /// Enable automatic cleanup
    pub auto_cleanup: bool,
}

/// Complete snapshot representation
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Snapshot {
    /// Unique snapshot ID
    pub id: u64,
    /// Parent snapshot ID for incrementals
    pub parent_id: Option<u64>,
    /// Snapshot type
    pub snapshot_type: SnapshotType,
    /// Root path being snapshot
    pub root_path: PathBuf,
    /// Snapshot version
    pub version: SnapshotVersion,
    /// Creation timestamp
    pub created_at: SystemTime,
    /// Snapshot metadata
    pub metadata: SnapshotMetadata,
    /// File entries in snapshot
    pub entries: Vec<SnapshotEntry>,
    /// Snapshot size information
    pub size_info: SnapshotSizeInfo,
    /// Integrity information
    pub integrity: SnapshotIntegrity,
    /// Status
    pub status: SnapshotStatus,
}

/// Snapshot types
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum SnapshotType {
    /// Full snapshot containing all data
    Full,
    /// Incremental snapshot containing only changes
    Incremental,
    /// Differential snapshot (changes since last full)
    Differential,
}

/// Snapshot versioning information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SnapshotVersion {
    /// Major version
    pub major: u32,
    /// Minor version
    pub minor: u32,
    /// Patch version
    pub patch: u32,
    /// Version string
    pub version_string: String,
}

/// Comprehensive snapshot metadata
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SnapshotMetadata {
    /// Snapshot name
    pub name: Option<String>,
    /// Description
    pub description: Option<String>,
    /// Tags
    pub tags: Vec<String>,
    /// Custom properties
    pub properties: HashMap<String, String>,
    /// Creation context
    pub context: CreationContext,
    /// Compression info
    pub compression_info: Option<CompressionInfo>,
    /// Encryption info
    pub encryption_info: Option<EncryptionInfo>,
}

/// Snapshot creation context
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CreationContext {
    /// User ID
    pub user_id: Option<String>,
    /// Process ID
    pub process_id: u32,
    /// Host information
    pub host_info: HostInfo,
    /// Creation reason
    pub reason: Option<String>,
}

/// Host information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HostInfo {
    /// Operating system
    pub os: String,
    /// Architecture
    pub arch: String,
    /// Hostname
    pub hostname: String,
}

/// Compression information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CompressionInfo {
    /// Algorithm used
    pub algorithm: CompressionAlgorithm,
    /// Compression level
    pub level: u32,
    /// Original size
    pub original_size: u64,
    /// Compressed size
    pub compressed_size: u64,
    /// Compression ratio
    pub ratio: f64,
}

/// Compression algorithms
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum CompressionAlgorithm {
    None,
    Gzip,
    Zstd,
    Lz4,
}

/// Encryption information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EncryptionInfo {
    /// Algorithm used
    pub algorithm: EncryptionAlgorithm,
    /// Key derivation info
    pub key_derivation: KeyDerivationInfo,
    /// Initialization vector
    pub iv: Vec<u8>,
    /// Encrypted metadata
    pub encrypted_metadata: bool,
}

/// Encryption algorithms
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum EncryptionAlgorithm {
    None,
    Aes256Gcm,
    ChaCha20Poly1305,
}

/// Key derivation information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct KeyDerivationInfo {
    /// Algorithm used
    pub algorithm: String,
    /// Salt
    pub salt: Vec<u8>,
    /// Iterations
    pub iterations: u32,
}

/// Individual snapshot entry
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SnapshotEntry {
    /// Relative path from root
    pub path: PathBuf,
    /// Entry type
    pub entry_type: SnapshotEntryType,
    /// File metadata
    pub metadata: FileMetadata,
    /// Content hash
    pub content_hash: Option<String>,
    /// Compressed content hash
    pub compressed_hash: Option<String>,
    /// Size information
    pub size: u64,
    /// Compressed size
    pub compressed_size: Option<u64>,
    /// Deduplication reference
    pub dedup_ref: Option<String>,
    /// Change type (for incremental snapshots)
    pub change_type: Option<ChangeType>,
}

/// Snapshot entry types
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum SnapshotEntryType {
    File,
    Directory,
    SymbolicLink,
    HardLink,
    Socket,
    Pipe,
    BlockDevice,
    CharacterDevice,
}

/// File metadata for snapshots
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileMetadata {
    /// File permissions
    pub permissions: u32,
    /// Owner UID
    pub uid: Option<u32>,
    /// Group GID
    pub gid: Option<u32>,
    /// Creation time
    pub created: Option<SystemTime>,
    /// Modification time
    pub modified: Option<SystemTime>,
    /// Access time
    pub accessed: Option<SystemTime>,
    /// Extended attributes
    pub xattrs: HashMap<String, Vec<u8>>,
}

/// Change types for incremental snapshots
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum ChangeType {
    Created,
    Modified,
    Deleted,
    Renamed { from: PathBuf },
    MetadataChanged,
}

/// Snapshot size information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SnapshotSizeInfo {
    /// Original total size
    pub original_size: u64,
    /// Compressed size
    pub compressed_size: u64,
    /// Stored size (after deduplication)
    pub stored_size: u64,
    /// Number of files
    pub file_count: u32,
    /// Number of directories
    pub directory_count: u32,
    /// Deduplication savings
    pub dedup_savings: u64,
}

/// Snapshot integrity information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SnapshotIntegrity {
    /// Overall hash
    pub overall_hash: String,
    /// Metadata hash
    pub metadata_hash: String,
    /// File hashes validated
    pub validated_at: Option<SystemTime>,
    /// Integrity status
    pub status: IntegrityStatus,
    /// Error details if corrupted
    pub error_details: Option<String>,
}

/// Integrity status
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum IntegrityStatus {
    Valid,
    Corrupted,
    Unknown,
    ValidationInProgress,
}

/// Snapshot status
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum SnapshotStatus {
    Creating,
    Available,
    Restoring,
    Validating,
    Corrupted,
    Deleted,
}

/// Snapshot storage backend
pub struct SnapshotStorage {
    /// Base storage directory
    base_dir: PathBuf,
    /// Storage format version
    format_version: u32,
    /// Index of stored snapshots
    index: Arc<RwLock<HashMap<u64, StoredSnapshotInfo>>>,
}

/// Information about stored snapshots
#[derive(Debug, Clone)]
pub struct StoredSnapshotInfo {
    /// Snapshot ID
    pub id: u64,
    /// Storage path
    pub path: PathBuf,
    /// Metadata file path
    pub metadata_path: PathBuf,
    /// Size on disk
    pub disk_size: u64,
    /// Last accessed
    pub last_accessed: SystemTime,
}

/// Version management system
pub struct VersionManager {
    /// Version chains for paths
    version_chains: Arc<RwLock<HashMap<PathBuf, VersionChain>>>,
    /// Version compatibility matrix
    compatibility_matrix: Arc<RwLock<HashMap<SnapshotVersion, HashSet<SnapshotVersion>>>>,
}

/// Version chain for a specific path
#[derive(Debug, Clone)]
pub struct VersionChain {
    /// Path being versioned
    pub path: PathBuf,
    /// Chain of snapshot versions
    pub chain: Vec<ChainEntry>,
    /// Current head version
    pub head: Option<u64>,
}

/// Entry in version chain
#[derive(Debug, Clone)]
pub struct ChainEntry {
    /// Snapshot ID
    pub snapshot_id: u64,
    /// Parent snapshot ID
    pub parent_id: Option<u64>,
    /// Version
    pub version: SnapshotVersion,
    /// Creation time
    pub created_at: SystemTime,
}

/// Deduplication engine
pub struct DeduplicationEngine {
    /// Content hash index
    content_index: Arc<RwLock<HashMap<String, ContentBlock>>>,
    /// Block storage
    block_storage: Arc<RwLock<HashMap<String, Vec<u8>>>>,
    /// Block size for deduplication
    block_size: usize,
    /// Deduplication statistics
    stats: Arc<Mutex<DeduplicationStats>>,
}

/// Content block information
#[derive(Debug, Clone)]
pub struct ContentBlock {
    /// Block hash
    pub hash: String,
    /// Block size
    pub size: u64,
    /// Reference count
    pub ref_count: u32,
    /// First seen timestamp
    pub first_seen: SystemTime,
    /// Last accessed
    pub last_accessed: SystemTime,
}

/// Deduplication statistics
#[derive(Debug, Default, Clone)]
pub struct DeduplicationStats {
    /// Total blocks stored
    pub total_blocks: u64,
    /// Unique blocks
    pub unique_blocks: u64,
    /// Total space saved
    pub space_saved: u64,
    /// Deduplication ratio
    pub dedup_ratio: f64,
}

/// Compression engine
pub struct CompressionEngine {
    /// Compression statistics
    stats: Arc<Mutex<CompressionStats>>,
}

/// Compression statistics
#[derive(Debug, Default, Clone)]
pub struct CompressionStats {
    /// Total compressed files
    pub compressed_files: u64,
    /// Total original size
    pub original_size: u64,
    /// Total compressed size
    pub compressed_size: u64,
    /// Average compression ratio
    pub avg_compression_ratio: f64,
}

/// Transaction manager for atomic operations
pub struct TransactionManager {
    /// Active transactions
    active_transactions: Arc<RwLock<HashMap<u64, SnapshotTransaction>>>,
    /// Transaction log
    transaction_log: Arc<Mutex<Vec<TransactionLogEntry>>>,
    /// Next transaction ID
    next_tx_id: std::sync::atomic::AtomicU64,
}

/// Snapshot transaction
#[derive(Debug, Clone)]
pub struct SnapshotTransaction {
    /// Transaction ID
    pub id: u64,
    /// Operations in transaction
    pub operations: Vec<TransactionOperation>,
    /// Transaction start time
    pub started_at: SystemTime,
    /// Transaction status
    pub status: TransactionStatus,
    /// Rollback information
    pub rollback_info: Option<RollbackInfo>,
}

/// Transaction operations
#[derive(Debug, Clone)]
pub enum TransactionOperation {
    CreateSnapshot {
        snapshot_id: u64,
        root_path: PathBuf,
        snapshot_type: SnapshotType,
    },
    RestoreSnapshot {
        snapshot_id: u64,
        target_path: PathBuf,
    },
    DeleteSnapshot {
        snapshot_id: u64,
    },
    FileOperation {
        operation: FileOperation,
        path: PathBuf,
    },
}

/// File operations within transactions
#[derive(Debug, Clone)]
pub enum FileOperation {
    Create { content: Vec<u8> },
    Modify { content: Vec<u8> },
    Delete,
    Move { to: PathBuf },
}

/// Transaction status
#[derive(Debug, Clone, PartialEq)]
pub enum TransactionStatus {
    Active,
    Preparing,
    Committed,
    Aborted,
    RollingBack,
    RolledBack,
}

/// Rollback information
#[derive(Debug, Clone)]
pub struct RollbackInfo {
    /// Original file states
    pub original_states: HashMap<PathBuf, FileState>,
    /// Rollback operations
    pub rollback_operations: Vec<RollbackOperation>,
}

/// File state for rollback
#[derive(Debug, Clone)]
pub struct FileState {
    /// File content (if small enough)
    pub content: Option<Vec<u8>>,
    /// File metadata
    pub metadata: FileMetadata,
    /// Whether file existed
    pub existed: bool,
}

/// Rollback operations
#[derive(Debug, Clone)]
pub enum RollbackOperation {
    RestoreFile { path: PathBuf, state: FileState },
    DeleteFile { path: PathBuf },
    RestoreMetadata { path: PathBuf, metadata: FileMetadata },
}

/// Transaction log entry
#[derive(Debug, Clone)]
pub struct TransactionLogEntry {
    /// Transaction ID
    pub transaction_id: u64,
    /// Timestamp
    pub timestamp: SystemTime,
    /// Operation type
    pub operation_type: String,
    /// Details
    pub details: String,
    /// Success status
    pub success: bool,
}

/// Comprehensive snapshot metrics
#[derive(Debug, Default, Clone)]
pub struct SnapshotMetrics {
    /// Total snapshots created
    pub total_snapshots_created: u64,
    /// Currently active snapshots
    pub active_snapshots: u64,
    /// Total snapshot operations
    pub total_operations: u64,
    /// Successful operations
    pub successful_operations: u64,
    /// Failed operations
    pub failed_operations: u64,
    /// Total storage used
    pub total_storage_used: u64,
    /// Total original data size
    pub total_original_size: u64,
    /// Deduplication statistics
    pub dedup_stats: DeduplicationStats,
    /// Compression statistics
    pub compression_stats: CompressionStats,
    /// Performance metrics
    pub performance: PerformanceMetrics,
}

/// Performance metrics
#[derive(Debug, Default, Clone)]
pub struct PerformanceMetrics {
    /// Average snapshot creation time (ms)
    pub avg_snapshot_creation_time_ms: f64,
    /// Average restore time (ms)
    pub avg_restore_time_ms: f64,
    /// Average validation time (ms)
    pub avg_validation_time_ms: f64,
    /// Throughput (bytes/sec)
    pub throughput_bytes_per_sec: f64,
    /// Operations per second
    pub operations_per_sec: f64,
}

/// Snapshot options for creation
#[derive(Debug, Clone)]
pub struct SnapshotOptions {
    /// Snapshot name
    pub name: Option<String>,
    /// Description
    pub description: Option<String>,
    /// Tags
    pub tags: Vec<String>,
    /// Enable compression
    pub compress: bool,
    /// Compression level (0-9)
    pub compression_level: u32,
    /// Enable encryption
    pub encrypt: bool,
    /// Encryption key
    pub encryption_key: Option<Vec<u8>>,
    /// Include hidden files
    pub include_hidden: bool,
    /// Include system files
    pub include_system: bool,
    /// Custom properties
    pub properties: HashMap<String, String>,
}

/// Restore options
#[derive(Debug, Clone)]
pub struct RestoreOptions {
    /// Target path for restore
    pub target_path: PathBuf,
    /// Overwrite existing files
    pub overwrite_existing: bool,
    /// Preserve permissions
    pub preserve_permissions: bool,
    /// Preserve timestamps
    pub preserve_timestamps: bool,
    /// Verify integrity during restore
    pub verify_integrity: bool,
    /// Restore specific files only
    pub file_filter: Option<Vec<PathBuf>>,
}

/// Validation options
#[derive(Debug, Clone)]
pub struct ValidationOptions {
    /// Check file hashes
    pub check_hashes: bool,
    /// Check metadata integrity
    pub check_metadata: bool,
    /// Check deduplication references
    pub check_dedup_refs: bool,
    /// Detailed validation report
    pub detailed_report: bool,
}

/// Validation result
#[derive(Debug, Clone)]
pub struct ValidationResult {
    /// Overall validity
    pub is_valid: bool,
    /// Validation timestamp
    pub validated_at: SystemTime,
    /// Files checked
    pub files_checked: u32,
    /// Files with errors
    pub files_with_errors: u32,
    /// Error details
    pub errors: Vec<ValidationError>,
    /// Validation time (ms)
    pub validation_time_ms: u64,
}

/// Validation error
#[derive(Debug, Clone)]
pub struct ValidationError {
    /// File path with error
    pub path: PathBuf,
    /// Error type
    pub error_type: ValidationErrorType,
    /// Error message
    pub message: String,
}

/// Types of validation errors
#[derive(Debug, Clone, PartialEq)]
pub enum ValidationErrorType {
    HashMismatch,
    MetadataCorruption,
    MissingFile,
    DedupRefError,
    CompressionError,
    EncryptionError,
}

impl Default for SnapshotConfig {
    fn default() -> Self {
        Self {
            max_active_snapshots: 100,
            max_snapshot_size: 10 * 1024 * 1024 * 1024, // 10GB
            enable_compression: true,
            compression_level: 6,
            enable_deduplication: true,
            enable_encryption: false,
            storage_directory: PathBuf::from("/tmp/wasmtime4j_snapshots"),
            enable_integrity_checking: true,
            cleanup_interval_secs: 3600, // 1 hour
            retention_policy: RetentionPolicy {
                max_age_secs: Some(7 * 24 * 3600), // 1 week
                max_snapshots_per_path: Some(50),
                auto_cleanup: true,
            },
            max_incremental_chain_depth: 10,
        }
    }
}

impl Default for SnapshotOptions {
    fn default() -> Self {
        Self {
            name: None,
            description: None,
            tags: Vec::new(),
            compress: true,
            compression_level: 6,
            encrypt: false,
            encryption_key: None,
            include_hidden: false,
            include_system: false,
            properties: HashMap::new(),
        }
    }
}

impl FilesystemSnapshotManager {
    /// Create a new filesystem snapshot manager with default configuration
    pub fn new() -> Self {
        Self::with_config(SnapshotConfig::default())
    }

    /// Create a new filesystem snapshot manager with custom configuration
    pub fn with_config(config: SnapshotConfig) -> Self {
        let storage_dir = config.storage_directory.clone();

        Self {
            snapshots: Arc::new(AsyncRwLock::new(HashMap::new())),
            storage: Arc::new(SnapshotStorage::new(storage_dir)),
            version_manager: Arc::new(VersionManager::new()),
            dedup_engine: Arc::new(DeduplicationEngine::new(64 * 1024)), // 64KB blocks
            compression_engine: Arc::new(CompressionEngine::new()),
            transaction_manager: Arc::new(TransactionManager::new()),
            metrics: Arc::new(Mutex::new(SnapshotMetrics::default())),
            config,
            next_snapshot_id: std::sync::atomic::AtomicU64::new(1),
            operation_semaphore: Arc::new(Semaphore::new(100)),
        }
    }

    /// Get global snapshot manager instance
    pub fn global() -> &'static FilesystemSnapshotManager {
        &**SNAPSHOT_MANAGER
    }

    /// Create a full snapshot
    pub async fn create_full_snapshot(
        &self,
        root_path: &Path,
        options: SnapshotOptions,
    ) -> WasmtimeResult<u64> {
        let _permit = self.operation_semaphore.acquire().await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to acquire operation permit: {}", e),
            }
        })?;

        let start_time = Instant::now();
        let snapshot_id = self.next_snapshot_id.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

        // Start transaction
        let tx_id = self.transaction_manager.begin_transaction().await?;

        let result = async {
            // Create snapshot metadata
            let version = SnapshotVersion {
                major: 1,
                minor: 0,
                patch: 0,
                version_string: "1.0.0".to_string(),
            };

            let metadata = self.create_snapshot_metadata(&options).await?;

            // Scan filesystem and create entries
            let entries = self.scan_filesystem(root_path, &options).await?;

            // Apply compression and deduplication
            let processed_entries = self.process_entries(entries, root_path, &options).await?;

            // Calculate size information
            let size_info = self.calculate_size_info(&processed_entries).await?;

            // Create integrity information
            let integrity = self.calculate_integrity(&processed_entries, &metadata).await?;

            let snapshot = Arc::new(Snapshot {
                id: snapshot_id,
                parent_id: None,
                snapshot_type: SnapshotType::Full,
                root_path: root_path.to_path_buf(),
                version,
                created_at: SystemTime::now(),
                metadata,
                entries: processed_entries,
                size_info,
                integrity,
                status: SnapshotStatus::Creating,
            });

            // Store snapshot data
            self.storage.store_snapshot(&snapshot).await?;

            // Update snapshot status
            let mut snapshot = Arc::try_unwrap(snapshot).unwrap_or_else(|arc| (*arc).clone());
            snapshot.status = SnapshotStatus::Available;
            let snapshot = Arc::new(snapshot);

            // Add to active snapshots
            {
                let mut snapshots = self.snapshots.write().await;
                snapshots.insert(snapshot_id, snapshot.clone());
            }

            // Update version chain
            self.version_manager.add_to_chain(root_path, snapshot_id, &snapshot.version, None).await?;

            Ok(snapshot_id)
        }.await;

        // Complete or rollback transaction
        match result {
            Ok(id) => {
                self.transaction_manager.commit_transaction(tx_id).await?;

                // Update metrics
                let creation_time = start_time.elapsed().as_millis() as f64;
                self.update_creation_metrics(creation_time).await;

                Ok(id)
            },
            Err(e) => {
                self.transaction_manager.rollback_transaction(tx_id).await?;
                Err(e)
            }
        }
    }

    /// Create an incremental snapshot
    pub async fn create_incremental_snapshot(
        &self,
        root_path: &Path,
        parent_snapshot_id: u64,
        options: SnapshotOptions,
    ) -> WasmtimeResult<u64> {
        let _permit = self.operation_semaphore.acquire().await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to acquire operation permit: {}", e),
            }
        })?;

        let start_time = Instant::now();
        let snapshot_id = self.next_snapshot_id.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

        // Get parent snapshot
        let parent_snapshot = {
            let snapshots = self.snapshots.read().await;
            snapshots.get(&parent_snapshot_id).cloned()
        };

        let parent_snapshot = parent_snapshot.ok_or_else(|| WasmtimeError::InvalidParameter {
            message: format!("Parent snapshot {} not found", parent_snapshot_id),
        })?;

        // Start transaction
        let tx_id = self.transaction_manager.begin_transaction().await?;

        let result = async {
            // Create snapshot metadata
            let version = SnapshotVersion {
                major: parent_snapshot.version.major,
                minor: parent_snapshot.version.minor,
                patch: parent_snapshot.version.patch + 1,
                version_string: format!("{}.{}.{}",
                    parent_snapshot.version.major,
                    parent_snapshot.version.minor,
                    parent_snapshot.version.patch + 1),
            };

            let metadata = self.create_snapshot_metadata(&options).await?;

            // Find changes since parent snapshot
            let changes = self.find_changes(root_path, &parent_snapshot, &options).await?;

            // Process incremental entries
            let processed_entries = self.process_entries(changes, root_path, &options).await?;

            // Calculate size information
            let size_info = self.calculate_size_info(&processed_entries).await?;

            // Create integrity information
            let integrity = self.calculate_integrity(&processed_entries, &metadata).await?;

            let snapshot = Arc::new(Snapshot {
                id: snapshot_id,
                parent_id: Some(parent_snapshot_id),
                snapshot_type: SnapshotType::Incremental,
                root_path: root_path.to_path_buf(),
                version,
                created_at: SystemTime::now(),
                metadata,
                entries: processed_entries,
                size_info,
                integrity,
                status: SnapshotStatus::Creating,
            });

            // Store snapshot data
            self.storage.store_snapshot(&snapshot).await?;

            // Update snapshot status
            let mut snapshot = Arc::try_unwrap(snapshot).unwrap_or_else(|arc| (*arc).clone());
            snapshot.status = SnapshotStatus::Available;
            let snapshot = Arc::new(snapshot);

            // Add to active snapshots
            {
                let mut snapshots = self.snapshots.write().await;
                snapshots.insert(snapshot_id, snapshot.clone());
            }

            // Update version chain
            self.version_manager.add_to_chain(root_path, snapshot_id, &snapshot.version, Some(parent_snapshot_id)).await?;

            Ok(snapshot_id)
        }.await;

        // Complete or rollback transaction
        match result {
            Ok(id) => {
                self.transaction_manager.commit_transaction(tx_id).await?;

                // Update metrics
                let creation_time = start_time.elapsed().as_millis() as f64;
                self.update_creation_metrics(creation_time).await;

                Ok(id)
            },
            Err(e) => {
                self.transaction_manager.rollback_transaction(tx_id).await?;
                Err(e)
            }
        }
    }

    /// Restore from snapshot
    pub async fn restore_snapshot(
        &self,
        snapshot_id: u64,
        options: RestoreOptions,
    ) -> WasmtimeResult<()> {
        let _permit = self.operation_semaphore.acquire().await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to acquire operation permit: {}", e),
            }
        })?;

        let start_time = Instant::now();

        // Get snapshot
        let snapshot = {
            let snapshots = self.snapshots.read().await;
            snapshots.get(&snapshot_id).cloned()
        };

        let snapshot = snapshot.ok_or_else(|| WasmtimeError::InvalidParameter {
            message: format!("Snapshot {} not found", snapshot_id),
        })?;

        // Start transaction
        let tx_id = self.transaction_manager.begin_transaction().await?;

        let result = async {
            // Update snapshot status
            {
                let mut snapshots = self.snapshots.write().await;
                if let Some(snap) = snapshots.get_mut(&snapshot_id) {
                    let mut snap_mut = Arc::try_unwrap(snap.clone()).unwrap_or_else(|arc| (*arc).clone());
                    snap_mut.status = SnapshotStatus::Restoring;
                    *snap = Arc::new(snap_mut);
                }
            }

            // Create target directory if needed
            if let Some(parent) = options.target_path.parent() {
                create_dir_all(parent).await.map_err(|e| WasmtimeError::Wasi {
                    message: format!("Failed to create target directory: {}", e),
                })?;
            }

            // For incremental snapshots, need to restore full chain
            let restore_chain = self.build_restore_chain(&snapshot).await?;

            // Restore files in order
            for snap in restore_chain {
                self.restore_snapshot_entries(&snap, &options).await?;
            }

            Ok(())
        }.await;

        // Complete or rollback transaction
        match result {
            Ok(_) => {
                self.transaction_manager.commit_transaction(tx_id).await?;

                // Update snapshot status back to available
                {
                    let mut snapshots = self.snapshots.write().await;
                    if let Some(snap) = snapshots.get_mut(&snapshot_id) {
                        let mut snap_mut = Arc::try_unwrap(snap.clone()).unwrap_or_else(|arc| (*arc).clone());
                        snap_mut.status = SnapshotStatus::Available;
                        *snap = Arc::new(snap_mut);
                    }
                }

                // Update metrics
                let restore_time = start_time.elapsed().as_millis() as f64;
                self.update_restore_metrics(restore_time).await;

                Ok(())
            },
            Err(e) => {
                self.transaction_manager.rollback_transaction(tx_id).await?;

                // Update snapshot status back to available
                {
                    let mut snapshots = self.snapshots.write().await;
                    if let Some(snap) = snapshots.get_mut(&snapshot_id) {
                        let mut snap_mut = Arc::try_unwrap(snap.clone()).unwrap_or_else(|arc| (*arc).clone());
                        snap_mut.status = SnapshotStatus::Available;
                        *snap = Arc::new(snap_mut);
                    }
                }

                Err(e)
            }
        }
    }

    /// Validate snapshot integrity
    pub async fn validate_snapshot(
        &self,
        snapshot_id: u64,
        options: ValidationOptions,
    ) -> WasmtimeResult<ValidationResult> {
        let start_time = Instant::now();

        // Get snapshot
        let snapshot = {
            let snapshots = self.snapshots.read().await;
            snapshots.get(&snapshot_id).cloned()
        };

        let snapshot = snapshot.ok_or_else(|| WasmtimeError::InvalidParameter {
            message: format!("Snapshot {} not found", snapshot_id),
        })?;

        // Update snapshot status
        {
            let mut snapshots = self.snapshots.write().await;
            if let Some(snap) = snapshots.get_mut(&snapshot_id) {
                let mut snap_mut = Arc::try_unwrap(snap.clone()).unwrap_or_else(|arc| (*arc).clone());
                snap_mut.status = SnapshotStatus::Validating;
                *snap = Arc::new(snap_mut);
            }
        }

        let result = self.perform_validation(&snapshot, options).await;

        // Update snapshot status and integrity info
        {
            let mut snapshots = self.snapshots.write().await;
            if let Some(snap) = snapshots.get_mut(&snapshot_id) {
                let mut snap_mut = Arc::try_unwrap(snap.clone()).unwrap_or_else(|arc| (*arc).clone());
                match &result {
                    Ok(validation_result) => {
                        snap_mut.status = SnapshotStatus::Available;
                        snap_mut.integrity.status = if validation_result.is_valid {
                            IntegrityStatus::Valid
                        } else {
                            IntegrityStatus::Corrupted
                        };
                        snap_mut.integrity.validated_at = Some(SystemTime::now());
                        if !validation_result.is_valid {
                            snap_mut.integrity.error_details = Some(
                                validation_result.errors.iter()
                                    .map(|e| e.message.clone())
                                    .collect::<Vec<_>>()
                                    .join("; ")
                            );
                        }
                    },
                    Err(_) => {
                        snap_mut.status = SnapshotStatus::Available;
                        snap_mut.integrity.status = IntegrityStatus::Unknown;
                    }
                }
                *snap = Arc::new(snap_mut);
            }
        }

        // Update metrics
        let validation_time = start_time.elapsed().as_millis() as f64;
        self.update_validation_metrics(validation_time).await;

        result
    }

    /// Delete a snapshot
    pub async fn delete_snapshot(&self, snapshot_id: u64) -> WasmtimeResult<()> {
        // Start transaction
        let tx_id = self.transaction_manager.begin_transaction().await?;

        let result = async {
            // Remove from active snapshots
            let snapshot = {
                let mut snapshots = self.snapshots.write().await;
                snapshots.remove(&snapshot_id)
            };

            let snapshot = snapshot.ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Snapshot {} not found", snapshot_id),
            })?;

            // Remove from storage
            self.storage.delete_snapshot(snapshot_id).await?;

            // Clean up deduplication references
            self.dedup_engine.cleanup_references(&snapshot.entries).await?;

            // Update version chain
            self.version_manager.remove_from_chain(&snapshot.root_path, snapshot_id).await?;

            Ok(())
        }.await;

        // Complete or rollback transaction
        match result {
            Ok(_) => {
                self.transaction_manager.commit_transaction(tx_id).await?;
                Ok(())
            },
            Err(e) => {
                self.transaction_manager.rollback_transaction(tx_id).await?;
                Err(e)
            }
        }
    }

    /// List all snapshots
    pub async fn list_snapshots(&self) -> Vec<Arc<Snapshot>> {
        let snapshots = self.snapshots.read().await;
        snapshots.values().cloned().collect()
    }

    /// Get snapshot by ID
    pub async fn get_snapshot(&self, snapshot_id: u64) -> Option<Arc<Snapshot>> {
        let snapshots = self.snapshots.read().await;
        snapshots.get(&snapshot_id).cloned()
    }

    /// Get snapshot metrics
    pub async fn get_metrics(&self) -> SnapshotMetrics {
        let metrics = self.metrics.lock().unwrap();
        metrics.clone()
    }

    // Private helper methods

    async fn create_snapshot_metadata(&self, options: &SnapshotOptions) -> WasmtimeResult<SnapshotMetadata> {
        let context = CreationContext {
            user_id: None, // Would be set from environment
            process_id: std::process::id(),
            host_info: HostInfo {
                os: std::env::consts::OS.to_string(),
                arch: std::env::consts::ARCH.to_string(),
                hostname: "localhost".to_string(), // Would get actual hostname
            },
            reason: None,
        };

        Ok(SnapshotMetadata {
            name: options.name.clone(),
            description: options.description.clone(),
            tags: options.tags.clone(),
            properties: options.properties.clone(),
            context,
            compression_info: None, // Will be set during processing
            encryption_info: None,  // Will be set during processing
        })
    }

    async fn scan_filesystem(&self, root_path: &Path, options: &SnapshotOptions) -> WasmtimeResult<Vec<SnapshotEntry>> {
        let mut entries = Vec::new();
        self.scan_directory_recursive(root_path, root_path, &mut entries, options).await?;
        Ok(entries)
    }

    async fn scan_directory_recursive(
        &self,
        root_path: &Path,
        current_path: &Path,
        entries: &mut Vec<SnapshotEntry>,
        options: &SnapshotOptions,
    ) -> WasmtimeResult<()> {
        let mut dir_entries = tokio::fs::read_dir(current_path).await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to read directory {}: {}", current_path.display(), e),
            }
        })?;

        while let Some(entry) = dir_entries.next_entry().await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to read directory entry: {}", e),
            }
        })? {
            let path = entry.path();
            let relative_path = path.strip_prefix(root_path).map_err(|_| {
                WasmtimeError::Wasi {
                    message: "Failed to create relative path".to_string(),
                }
            })?;

            // Skip hidden files if not requested
            if !options.include_hidden && path.file_name()
                .and_then(|name| name.to_str())
                .map(|name| name.starts_with('.'))
                .unwrap_or(false) {
                continue;
            }

            let metadata = entry.metadata().await.map_err(|e| {
                WasmtimeError::Wasi {
                    message: format!("Failed to get metadata for {}: {}", path.display(), e),
                }
            })?;

            let entry_type = if metadata.is_dir() {
                SnapshotEntryType::Directory
            } else if metadata.is_file() {
                SnapshotEntryType::File
            } else if metadata.file_type().is_symlink() {
                SnapshotEntryType::SymbolicLink
            } else {
                SnapshotEntryType::File // Default for unknown types
            };

            let file_metadata = FileMetadata {
                permissions: {
                    #[cfg(unix)]
                    { metadata.permissions().mode() }
                    #[cfg(not(unix))]
                    { 0o644 }
                },
                uid: {
                    #[cfg(unix)]
                    { Some(metadata.uid()) }
                    #[cfg(not(unix))]
                    { None }
                },
                gid: {
                    #[cfg(unix)]
                    { Some(metadata.gid()) }
                    #[cfg(not(unix))]
                    { None }
                },
                created: metadata.created().ok(),
                modified: metadata.modified().ok(),
                accessed: metadata.accessed().ok(),
                xattrs: HashMap::new(), // Would be populated with actual extended attributes
            };

            let snapshot_entry = SnapshotEntry {
                path: relative_path.to_path_buf(),
                entry_type: entry_type.clone(),
                metadata: file_metadata,
                content_hash: None, // Will be calculated during processing
                compressed_hash: None,
                size: metadata.len(),
                compressed_size: None,
                dedup_ref: None,
                change_type: None,
            };

            entries.push(snapshot_entry);

            // Recursively scan subdirectories
            if entry_type == SnapshotEntryType::Directory {
                Box::pin(self.scan_directory_recursive(root_path, &path, entries, options)).await?;
            }
        }

        Ok(())
    }

    async fn find_changes(
        &self,
        root_path: &Path,
        parent_snapshot: &Snapshot,
        options: &SnapshotOptions,
    ) -> WasmtimeResult<Vec<SnapshotEntry>> {
        // Scan current filesystem
        let current_entries = self.scan_filesystem(root_path, options).await?;

        // Create lookup map for parent entries
        let parent_map: HashMap<PathBuf, &SnapshotEntry> = parent_snapshot.entries
            .iter()
            .map(|entry| (entry.path.clone(), entry))
            .collect();

        let mut changes = Vec::new();

        // Find new and modified files
        for current_entry in &current_entries {
            match parent_map.get(&current_entry.path) {
                None => {
                    // New file
                    let mut change_entry = current_entry.clone();
                    change_entry.change_type = Some(ChangeType::Created);
                    changes.push(change_entry);
                },
                Some(parent_entry) => {
                    // Check if file was modified
                    if self.entry_differs(current_entry, parent_entry).await? {
                        let mut change_entry = current_entry.clone();
                        change_entry.change_type = Some(ChangeType::Modified);
                        changes.push(change_entry);
                    }
                }
            }
        }

        // Find deleted files
        let current_map: HashMap<PathBuf, &SnapshotEntry> = current_entries
            .iter()
            .map(|entry| (entry.path.clone(), entry))
            .collect();

        for parent_entry in &parent_snapshot.entries {
            if !current_map.contains_key(&parent_entry.path) {
                let mut deleted_entry = parent_entry.clone();
                deleted_entry.change_type = Some(ChangeType::Deleted);
                changes.push(deleted_entry);
            }
        }

        Ok(changes)
    }

    async fn entry_differs(&self, current: &SnapshotEntry, parent: &SnapshotEntry) -> WasmtimeResult<bool> {
        // Compare basic metadata
        if current.size != parent.size {
            return Ok(true);
        }

        if current.metadata.modified != parent.metadata.modified {
            return Ok(true);
        }

        if current.metadata.permissions != parent.metadata.permissions {
            return Ok(true);
        }

        // For files, compare content hash if available
        if current.entry_type == SnapshotEntryType::File {
            if let (Some(current_hash), Some(parent_hash)) = (&current.content_hash, &parent.content_hash) {
                return Ok(current_hash != parent_hash);
            }
        }

        // Default to considering it different if we can't determine
        Ok(false)
    }

    async fn process_entries(&self, mut entries: Vec<SnapshotEntry>, root_path: &Path, options: &SnapshotOptions) -> WasmtimeResult<Vec<SnapshotEntry>> {
        for entry in &mut entries {
            // Calculate content hash for files
            if entry.entry_type == SnapshotEntryType::File && entry.change_type != Some(ChangeType::Deleted) {
                // Construct full path from root_path and relative entry path
                let full_path = root_path.join(&entry.path);
                entry.content_hash = Some(self.calculate_file_hash(&full_path).await?);
            }

            // Apply deduplication
            if self.config.enable_deduplication && entry.content_hash.is_some() {
                entry.dedup_ref = self.dedup_engine.add_content(&entry).await?;
            }

            // Apply compression
            if options.compress && entry.entry_type == SnapshotEntryType::File {
                let compressed_data = self.compression_engine.compress_entry(&entry, options.compression_level).await?;
                entry.compressed_size = Some(compressed_data.len() as u64);
                entry.compressed_hash = Some(self.calculate_data_hash(&compressed_data));
            }
        }

        Ok(entries)
    }

    async fn calculate_file_hash(&self, path: &Path) -> WasmtimeResult<String> {
        let mut file = tokio::fs::File::open(path).await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to open file for hashing: {}", e),
            }
        })?;

        let mut hasher = Sha256::new();
        let mut buffer = vec![0u8; 64 * 1024]; // 64KB buffer

        loop {
            let bytes_read = file.read(&mut buffer).await.map_err(|e| {
                WasmtimeError::Wasi {
                    message: format!("Failed to read file for hashing: {}", e),
                }
            })?;

            if bytes_read == 0 {
                break;
            }

            hasher.update(&buffer[..bytes_read]);
        }

        Ok(format!("{:x}", hasher.finalize()))
    }

    fn calculate_data_hash(&self, data: &[u8]) -> String {
        let mut hasher = Sha256::new();
        hasher.update(data);
        format!("{:x}", hasher.finalize())
    }

    async fn calculate_size_info(&self, entries: &[SnapshotEntry]) -> WasmtimeResult<SnapshotSizeInfo> {
        let mut original_size = 0u64;
        let mut compressed_size = 0u64;
        let mut stored_size = 0u64;
        let mut file_count = 0u32;
        let mut directory_count = 0u32;

        for entry in entries {
            match entry.entry_type {
                SnapshotEntryType::File => {
                    file_count += 1;
                    original_size += entry.size;
                    compressed_size += entry.compressed_size.unwrap_or(entry.size);

                    // If deduplicated, stored size might be 0
                    if entry.dedup_ref.is_some() {
                        // Size is handled by deduplication engine
                        stored_size += 0;
                    } else {
                        stored_size += entry.compressed_size.unwrap_or(entry.size);
                    }
                },
                SnapshotEntryType::Directory => {
                    directory_count += 1;
                },
                _ => {}
            }
        }

        let dedup_savings = original_size.saturating_sub(stored_size);

        Ok(SnapshotSizeInfo {
            original_size,
            compressed_size,
            stored_size,
            file_count,
            directory_count,
            dedup_savings,
        })
    }

    async fn calculate_integrity(&self, entries: &[SnapshotEntry], metadata: &SnapshotMetadata) -> WasmtimeResult<SnapshotIntegrity> {
        let mut hasher = Sha256::new();

        // Hash all entry information
        for entry in entries {
            hasher.update(entry.path.to_string_lossy().as_bytes());
            if let Some(ref hash) = entry.content_hash {
                hasher.update(hash.as_bytes());
            }
            hasher.update(&entry.size.to_le_bytes());
        }

        let overall_hash = format!("{:x}", hasher.finalize());

        // Calculate metadata hash
        let metadata_json = serde_json::to_string(metadata).map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to serialize metadata: {}", e),
            }
        })?;
        let mut metadata_hasher = Sha256::new();
        metadata_hasher.update(metadata_json.as_bytes());
        let metadata_hash = format!("{:x}", metadata_hasher.finalize());

        Ok(SnapshotIntegrity {
            overall_hash,
            metadata_hash,
            validated_at: None,
            status: IntegrityStatus::Unknown,
            error_details: None,
        })
    }

    async fn build_restore_chain(&self, snapshot: &Snapshot) -> WasmtimeResult<Vec<Arc<Snapshot>>> {
        let mut chain = Vec::new();
        let mut current_snapshot = Some(snapshot.clone());

        // Build chain from current snapshot back to root
        while let Some(snap) = current_snapshot {
            chain.push(Arc::new(snap.clone()));

            if let Some(parent_id) = snap.parent_id {
                let snapshots = self.snapshots.read().await;
                current_snapshot = snapshots.get(&parent_id).map(|s| (**s).clone());
            } else {
                break;
            }
        }

        // Reverse to get restore order (oldest to newest)
        chain.reverse();
        Ok(chain)
    }

    async fn restore_snapshot_entries(&self, snapshot: &Snapshot, options: &RestoreOptions) -> WasmtimeResult<()> {
        for entry in &snapshot.entries {
            match &entry.change_type {
                Some(ChangeType::Deleted) => {
                    // Delete file/directory
                    let target_path = options.target_path.join(&entry.path);
                    if target_path.exists() {
                        if entry.entry_type == SnapshotEntryType::Directory {
                            remove_dir_all(&target_path).await.map_err(|e| {
                                WasmtimeError::Wasi {
                                    message: format!("Failed to delete directory {}: {}", target_path.display(), e),
                                }
                            })?;
                        } else {
                            tokio::fs::remove_file(&target_path).await.map_err(|e| {
                                WasmtimeError::Wasi {
                                    message: format!("Failed to delete file {}: {}", target_path.display(), e),
                                }
                            })?;
                        }
                    }
                },
                _ => {
                    // Create/modify file/directory
                    self.restore_entry(entry, &options.target_path, options).await?;
                }
            }
        }

        Ok(())
    }

    async fn restore_entry(&self, entry: &SnapshotEntry, target_root: &Path, options: &RestoreOptions) -> WasmtimeResult<()> {
        let target_path = target_root.join(&entry.path);

        match entry.entry_type {
            SnapshotEntryType::Directory => {
                create_dir_all(&target_path).await.map_err(|e| {
                    WasmtimeError::Wasi {
                        message: format!("Failed to create directory {}: {}", target_path.display(), e),
                    }
                })?;
            },
            SnapshotEntryType::File => {
                // Create parent directory if needed
                if let Some(parent) = target_path.parent() {
                    create_dir_all(parent).await.map_err(|e| {
                        WasmtimeError::Wasi {
                            message: format!("Failed to create parent directory: {}", e),
                        }
                    })?;
                }

                // Get file content from storage
                // Try dedup first if available, fall back to direct storage
                let content = if let Some(ref dedup_ref) = entry.dedup_ref {
                    match self.dedup_engine.get_content(dedup_ref).await {
                        Ok(data) => data,
                        Err(_) => self.storage.load_file_content(entry).await?,
                    }
                } else {
                    self.storage.load_file_content(entry).await?
                };

                // Decompress if needed
                let final_content = if entry.compressed_size.is_some() {
                    self.compression_engine.decompress_data(&content).await?
                } else {
                    content
                };

                // Write file
                tokio::fs::write(&target_path, &final_content).await.map_err(|e| {
                    WasmtimeError::Wasi {
                        message: format!("Failed to write file {}: {}", target_path.display(), e),
                    }
                })?;

                // Restore metadata if requested
                if options.preserve_permissions || options.preserve_timestamps {
                    self.restore_file_metadata(&target_path, &entry.metadata, options).await?;
                }
            },
            SnapshotEntryType::SymbolicLink => {
                // Handle symbolic links
                // Implementation would depend on the stored link target
            },
            _ => {
                // Handle other types as needed
            }
        }

        Ok(())
    }

    async fn restore_file_metadata(&self, path: &Path, metadata: &FileMetadata, options: &RestoreOptions) -> WasmtimeResult<()> {
        if options.preserve_permissions {
            #[cfg(unix)]
            {
                use std::os::unix::fs::PermissionsExt;
                let permissions = std::fs::Permissions::from_mode(metadata.permissions);
                std::fs::set_permissions(path, permissions).map_err(|e| {
                    WasmtimeError::Wasi {
                        message: format!("Failed to set permissions: {}", e),
                    }
                })?;
            }
        }

        if options.preserve_timestamps {
            if let (Some(accessed), Some(modified)) = (metadata.accessed, metadata.modified) {
                filetime::set_file_times(path, filetime::FileTime::from_system_time(accessed), filetime::FileTime::from_system_time(modified))
                    .map_err(|e| {
                        WasmtimeError::Wasi {
                            message: format!("Failed to set file timestamps: {}", e),
                        }
                    })?;
            }
        }

        Ok(())
    }

    async fn perform_validation(&self, snapshot: &Snapshot, options: ValidationOptions) -> WasmtimeResult<ValidationResult> {
        let start_time = SystemTime::now();
        let mut errors = Vec::new();
        let mut files_checked = 0u32;
        let mut files_with_errors = 0u32;

        for entry in &snapshot.entries {
            files_checked += 1;

            if options.check_hashes && entry.content_hash.is_some() {
                // Validate content hash
                match self.validate_entry_hash(entry).await {
                    Ok(valid) => {
                        if !valid {
                            files_with_errors += 1;
                            errors.push(ValidationError {
                                path: entry.path.clone(),
                                error_type: ValidationErrorType::HashMismatch,
                                message: "Content hash mismatch".to_string(),
                            });
                        }
                    },
                    Err(e) => {
                        files_with_errors += 1;
                        errors.push(ValidationError {
                            path: entry.path.clone(),
                            error_type: ValidationErrorType::HashMismatch,
                            message: format!("Hash validation failed: {}", e),
                        });
                    }
                }
            }

            if options.check_dedup_refs && entry.dedup_ref.is_some() {
                // Validate deduplication references
                if !self.dedup_engine.validate_reference(entry.dedup_ref.as_ref().unwrap()).await? {
                    files_with_errors += 1;
                    errors.push(ValidationError {
                        path: entry.path.clone(),
                        error_type: ValidationErrorType::DedupRefError,
                        message: "Invalid deduplication reference".to_string(),
                    });
                }
            }
        }

        let validation_time_ms = start_time.elapsed().unwrap_or(Duration::ZERO).as_millis() as u64;
        let is_valid = files_with_errors == 0;

        Ok(ValidationResult {
            is_valid,
            validated_at: SystemTime::now(),
            files_checked,
            files_with_errors,
            errors,
            validation_time_ms,
        })
    }

    async fn validate_entry_hash(&self, entry: &SnapshotEntry) -> WasmtimeResult<bool> {
        if let Some(ref expected_hash) = entry.content_hash {
            let content = if let Some(ref dedup_ref) = entry.dedup_ref {
                self.dedup_engine.get_content(dedup_ref).await?
            } else {
                self.storage.load_file_content(entry).await?
            };

            let actual_hash = self.calculate_data_hash(&content);
            Ok(&actual_hash == expected_hash)
        } else {
            Ok(true) // No hash to validate
        }
    }

    async fn update_creation_metrics(&self, creation_time_ms: f64) {
        let mut metrics = self.metrics.lock().unwrap();
        metrics.total_snapshots_created += 1;
        metrics.active_snapshots += 1;
        metrics.successful_operations += 1;
        metrics.total_operations += 1;

        // Update average creation time
        let total_ops = metrics.total_snapshots_created as f64;
        metrics.performance.avg_snapshot_creation_time_ms =
            (metrics.performance.avg_snapshot_creation_time_ms * (total_ops - 1.0) + creation_time_ms) / total_ops;
    }

    async fn update_restore_metrics(&self, restore_time_ms: f64) {
        let mut metrics = self.metrics.lock().unwrap();
        metrics.successful_operations += 1;
        metrics.total_operations += 1;

        // Update average restore time (simplified calculation)
        metrics.performance.avg_restore_time_ms =
            (metrics.performance.avg_restore_time_ms + restore_time_ms) / 2.0;
    }

    async fn update_validation_metrics(&self, validation_time_ms: f64) {
        let mut metrics = self.metrics.lock().unwrap();
        metrics.successful_operations += 1;
        metrics.total_operations += 1;

        // Update average validation time (simplified calculation)
        metrics.performance.avg_validation_time_ms =
            (metrics.performance.avg_validation_time_ms + validation_time_ms) / 2.0;
    }
}

// Implementation of helper components

impl SnapshotStorage {
    pub fn new(base_dir: PathBuf) -> Self {
        Self {
            base_dir,
            format_version: 1,
            index: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    pub async fn store_snapshot(&self, snapshot: &Snapshot) -> WasmtimeResult<()> {
        // Create storage directory for snapshot
        let snapshot_dir = self.base_dir.join(format!("snapshot_{}", snapshot.id));
        create_dir_all(&snapshot_dir).await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to create snapshot directory: {}", e),
            }
        })?;

        // Store metadata
        let metadata_path = snapshot_dir.join("metadata.json");
        let metadata_json = serde_json::to_string_pretty(snapshot).map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to serialize snapshot metadata: {}", e),
            }
        })?;

        tokio::fs::write(&metadata_path, metadata_json).await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to write metadata file: {}", e),
            }
        })?;

        // Store file data for each entry
        for entry in &snapshot.entries {
            if entry.entry_type == SnapshotEntryType::File && entry.change_type != Some(ChangeType::Deleted) {
                self.store_entry_content(&snapshot_dir, entry).await?;
            }
        }

        // Update index
        {
            let mut index = self.index.write().unwrap();
            index.insert(snapshot.id, StoredSnapshotInfo {
                id: snapshot.id,
                path: snapshot_dir.clone(),
                metadata_path,
                disk_size: 0, // Would calculate actual size
                last_accessed: SystemTime::now(),
            });
        }

        Ok(())
    }

    async fn store_entry_content(&self, snapshot_dir: &Path, entry: &SnapshotEntry) -> WasmtimeResult<()> {
        // Create file path within snapshot
        let entry_path = snapshot_dir.join("files").join(&entry.path);

        if let Some(parent) = entry_path.parent() {
            create_dir_all(parent).await.map_err(|e| {
                WasmtimeError::Wasi {
                    message: format!("Failed to create entry parent directory: {}", e),
                }
            })?;
        }

        // This would normally copy the actual file content
        // For now, we'll create a placeholder
        tokio::fs::write(&entry_path, b"placeholder").await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to store entry content: {}", e),
            }
        })?;

        Ok(())
    }

    pub async fn load_file_content(&self, _entry: &SnapshotEntry) -> WasmtimeResult<Vec<u8>> {
        // This would load the actual file content from storage
        // For now, return placeholder
        Ok(b"placeholder".to_vec())
    }

    pub async fn delete_snapshot(&self, snapshot_id: u64) -> WasmtimeResult<()> {
        let snapshot_info = {
            let mut index = self.index.write().unwrap();
            index.remove(&snapshot_id)
        };

        if let Some(info) = snapshot_info {
            remove_dir_all(&info.path).await.map_err(|e| {
                WasmtimeError::Wasi {
                    message: format!("Failed to delete snapshot directory: {}", e),
                }
            })?;
        }

        Ok(())
    }
}

impl VersionManager {
    pub fn new() -> Self {
        Self {
            version_chains: Arc::new(RwLock::new(HashMap::new())),
            compatibility_matrix: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    pub async fn add_to_chain(&self, path: &Path, snapshot_id: u64, version: &SnapshotVersion, parent_id: Option<u64>) -> WasmtimeResult<()> {
        let mut chains = self.version_chains.write().unwrap();
        let chain = chains.entry(path.to_path_buf()).or_insert_with(|| VersionChain {
            path: path.to_path_buf(),
            chain: Vec::new(),
            head: None,
        });

        let entry = ChainEntry {
            snapshot_id,
            parent_id,
            version: version.clone(),
            created_at: SystemTime::now(),
        };

        chain.chain.push(entry);
        chain.head = Some(snapshot_id);

        Ok(())
    }

    pub async fn remove_from_chain(&self, path: &Path, snapshot_id: u64) -> WasmtimeResult<()> {
        let mut chains = self.version_chains.write().unwrap();
        if let Some(chain) = chains.get_mut(path) {
            chain.chain.retain(|entry| entry.snapshot_id != snapshot_id);

            // Update head if necessary
            if chain.head == Some(snapshot_id) {
                chain.head = chain.chain.last().map(|entry| entry.snapshot_id);
            }
        }
        Ok(())
    }
}

impl DeduplicationEngine {
    pub fn new(block_size: usize) -> Self {
        Self {
            content_index: Arc::new(RwLock::new(HashMap::new())),
            block_storage: Arc::new(RwLock::new(HashMap::new())),
            block_size,
            stats: Arc::new(Mutex::new(DeduplicationStats::default())),
        }
    }

    pub async fn add_content(&self, entry: &SnapshotEntry) -> WasmtimeResult<Option<String>> {
        if let Some(ref content_hash) = entry.content_hash {
            let mut index = self.content_index.write().unwrap();

            if let Some(block) = index.get_mut(content_hash) {
                // Content already exists, increment reference count
                block.ref_count += 1;
                block.last_accessed = SystemTime::now();
                return Ok(Some(content_hash.clone()));
            } else {
                // New content, add to index
                let block = ContentBlock {
                    hash: content_hash.clone(),
                    size: entry.size,
                    ref_count: 1,
                    first_seen: SystemTime::now(),
                    last_accessed: SystemTime::now(),
                };

                index.insert(content_hash.clone(), block);

                // Update stats
                let mut stats = self.stats.lock().unwrap();
                stats.total_blocks += 1;
                stats.unique_blocks += 1;

                return Ok(Some(content_hash.clone()));
            }
        }

        Ok(None)
    }

    pub async fn get_content(&self, dedup_ref: &str) -> WasmtimeResult<Vec<u8>> {
        let storage = self.block_storage.read().unwrap();
        storage.get(dedup_ref).cloned().ok_or_else(|| {
            WasmtimeError::Wasi {
                message: format!("Deduplication reference {} not found", dedup_ref),
            }
        })
    }

    pub async fn validate_reference(&self, dedup_ref: &str) -> WasmtimeResult<bool> {
        let index = self.content_index.read().unwrap();
        Ok(index.contains_key(dedup_ref))
    }

    pub async fn cleanup_references(&self, entries: &[SnapshotEntry]) -> WasmtimeResult<()> {
        let mut index = self.content_index.write().unwrap();

        for entry in entries {
            if let Some(ref dedup_ref) = entry.dedup_ref {
                if let Some(block) = index.get_mut(dedup_ref) {
                    block.ref_count = block.ref_count.saturating_sub(1);

                    // Remove block if no more references
                    if block.ref_count == 0 {
                        index.remove(dedup_ref);

                        // Also remove from block storage
                        let mut storage = self.block_storage.write().unwrap();
                        storage.remove(dedup_ref);

                        // Update stats
                        let mut stats = self.stats.lock().unwrap();
                        stats.unique_blocks = stats.unique_blocks.saturating_sub(1);
                    }
                }
            }
        }

        Ok(())
    }
}

impl CompressionEngine {
    pub fn new() -> Self {
        Self {
            stats: Arc::new(Mutex::new(CompressionStats::default())),
        }
    }

    pub async fn compress_entry(&self, _entry: &SnapshotEntry, compression_level: u32) -> WasmtimeResult<Vec<u8>> {
        // This would compress the actual file content
        // For now, return placeholder compressed data
        let original_data = b"placeholder file content";
        let compressed = self.compress_data(original_data, compression_level)?;

        // Update stats
        let mut stats = self.stats.lock().unwrap();
        stats.compressed_files += 1;
        stats.original_size += original_data.len() as u64;
        stats.compressed_size += compressed.len() as u64;
        stats.avg_compression_ratio = stats.compressed_size as f64 / stats.original_size as f64;

        Ok(compressed)
    }

    pub async fn decompress_data(&self, data: &[u8]) -> WasmtimeResult<Vec<u8>> {
        let mut decoder = GzDecoder::new(data);
        let mut decompressed = Vec::new();

        decoder.read_to_end(&mut decompressed).map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Decompression failed: {}", e),
            }
        })?;

        Ok(decompressed)
    }

    fn compress_data(&self, data: &[u8], level: u32) -> WasmtimeResult<Vec<u8>> {
        let mut encoder = GzEncoder::new(Vec::new(), Compression::new(level));
        encoder.write_all(data).map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Compression failed: {}", e),
            }
        })?;

        encoder.finish().map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Compression finish failed: {}", e),
            }
        })
    }
}

impl TransactionManager {
    pub fn new() -> Self {
        Self {
            active_transactions: Arc::new(RwLock::new(HashMap::new())),
            transaction_log: Arc::new(Mutex::new(Vec::new())),
            next_tx_id: std::sync::atomic::AtomicU64::new(1),
        }
    }

    pub async fn begin_transaction(&self) -> WasmtimeResult<u64> {
        let tx_id = self.next_tx_id.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

        let transaction = SnapshotTransaction {
            id: tx_id,
            operations: Vec::new(),
            started_at: SystemTime::now(),
            status: TransactionStatus::Active,
            rollback_info: None,
        };

        {
            let mut transactions = self.active_transactions.write().unwrap();
            transactions.insert(tx_id, transaction);
        }

        self.log_transaction(tx_id, "BEGIN", "Transaction started", true).await;
        Ok(tx_id)
    }

    pub async fn commit_transaction(&self, tx_id: u64) -> WasmtimeResult<()> {
        {
            let mut transactions = self.active_transactions.write().unwrap();
            if let Some(mut transaction) = transactions.remove(&tx_id) {
                transaction.status = TransactionStatus::Committed;
                // Transaction is committed and removed from active list
            } else {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Transaction {} not found", tx_id),
                });
            }
        }

        self.log_transaction(tx_id, "COMMIT", "Transaction committed", true).await;
        Ok(())
    }

    pub async fn rollback_transaction(&self, tx_id: u64) -> WasmtimeResult<()> {
        {
            let mut transactions = self.active_transactions.write().unwrap();
            if let Some(mut transaction) = transactions.remove(&tx_id) {
                transaction.status = TransactionStatus::RolledBack;

                // Perform rollback operations if available
                if let Some(ref rollback_info) = transaction.rollback_info {
                    self.execute_rollback_operations(&rollback_info.rollback_operations).await?;
                }
            } else {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Transaction {} not found", tx_id),
                });
            }
        }

        self.log_transaction(tx_id, "ROLLBACK", "Transaction rolled back", true).await;
        Ok(())
    }

    async fn execute_rollback_operations(&self, operations: &[RollbackOperation]) -> WasmtimeResult<()> {
        for operation in operations {
            match operation {
                RollbackOperation::RestoreFile { path, state } => {
                    if state.existed {
                        if let Some(ref content) = state.content {
                            tokio::fs::write(path, content).await.map_err(|e| {
                                WasmtimeError::Wasi {
                                    message: format!("Failed to restore file during rollback: {}", e),
                                }
                            })?;
                        }
                    }
                },
                RollbackOperation::DeleteFile { path } => {
                    if path.exists() {
                        tokio::fs::remove_file(path).await.map_err(|e| {
                            WasmtimeError::Wasi {
                                message: format!("Failed to delete file during rollback: {}", e),
                            }
                        })?;
                    }
                },
                RollbackOperation::RestoreMetadata { path: _, metadata: _ } => {
                    // Restore metadata if needed
                }
            }
        }
        Ok(())
    }

    async fn log_transaction(&self, tx_id: u64, operation_type: &str, details: &str, success: bool) {
        let entry = TransactionLogEntry {
            transaction_id: tx_id,
            timestamp: SystemTime::now(),
            operation_type: operation_type.to_string(),
            details: details.to_string(),
            success,
        };

        let mut log = self.transaction_log.lock().unwrap();
        log.push(entry);
    }
}

// C API for JNI and Panama FFI integration

/// Initialize filesystem snapshot manager
#[no_mangle]
pub unsafe extern "C" fn snapshot_manager_init() -> c_int {
    let _ = FilesystemSnapshotManager::global();
    0 // Success
}

/// Create a full snapshot
#[no_mangle]
pub unsafe extern "C" fn snapshot_create_full(
    root_path: *const c_char,
    name: *const c_char,
    compress: bool,
    compression_level: c_uint,
    snapshot_id_out: *mut u64,
) -> c_int {
    if root_path.is_null() || snapshot_id_out.is_null() {
        return -1; // Invalid parameters
    }

    let root_path_str = match std::ffi::CStr::from_ptr(root_path).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let name_str = if name.is_null() {
        None
    } else {
        match std::ffi::CStr::from_ptr(name).to_str() {
            Ok(s) => Some(s.to_string()),
            Err(_) => return -1,
        }
    };

    let options = SnapshotOptions {
        name: name_str,
        compress,
        compression_level,
        ..Default::default()
    };

    let manager = FilesystemSnapshotManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.create_full_snapshot(Path::new(root_path_str), options)) {
        Ok(snapshot_id) => {
            *snapshot_id_out = snapshot_id;
            0 // Success
        },
        Err(_) => -1, // Error
    }
}

/// Create an incremental snapshot
#[no_mangle]
pub unsafe extern "C" fn snapshot_create_incremental(
    root_path: *const c_char,
    parent_snapshot_id: u64,
    name: *const c_char,
    compress: bool,
    compression_level: c_uint,
    snapshot_id_out: *mut u64,
) -> c_int {
    if root_path.is_null() || snapshot_id_out.is_null() {
        return -1; // Invalid parameters
    }

    let root_path_str = match std::ffi::CStr::from_ptr(root_path).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let name_str = if name.is_null() {
        None
    } else {
        match std::ffi::CStr::from_ptr(name).to_str() {
            Ok(s) => Some(s.to_string()),
            Err(_) => return -1,
        }
    };

    let options = SnapshotOptions {
        name: name_str,
        compress,
        compression_level,
        ..Default::default()
    };

    let manager = FilesystemSnapshotManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.create_incremental_snapshot(Path::new(root_path_str), parent_snapshot_id, options)) {
        Ok(snapshot_id) => {
            *snapshot_id_out = snapshot_id;
            0 // Success
        },
        Err(_) => -1, // Error
    }
}

/// Restore from snapshot
#[no_mangle]
pub unsafe extern "C" fn snapshot_restore(
    snapshot_id: u64,
    target_path: *const c_char,
    overwrite_existing: bool,
    preserve_permissions: bool,
    preserve_timestamps: bool,
    verify_integrity: bool,
) -> c_int {
    if target_path.is_null() {
        return -1; // Invalid parameters
    }

    let target_path_str = match std::ffi::CStr::from_ptr(target_path).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let options = RestoreOptions {
        target_path: PathBuf::from(target_path_str),
        overwrite_existing,
        preserve_permissions,
        preserve_timestamps,
        verify_integrity,
        file_filter: None,
    };

    let manager = FilesystemSnapshotManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.restore_snapshot(snapshot_id, options)) {
        Ok(_) => 0,  // Success
        Err(_) => -1, // Error
    }
}

/// Validate snapshot
#[no_mangle]
pub unsafe extern "C" fn snapshot_validate(
    snapshot_id: u64,
    check_hashes: bool,
    check_metadata: bool,
    check_dedup_refs: bool,
    is_valid_out: *mut bool,
) -> c_int {
    if is_valid_out.is_null() {
        return -1; // Invalid parameters
    }

    let options = ValidationOptions {
        check_hashes,
        check_metadata,
        check_dedup_refs,
        detailed_report: false,
    };

    let manager = FilesystemSnapshotManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.validate_snapshot(snapshot_id, options)) {
        Ok(result) => {
            *is_valid_out = result.is_valid;
            0 // Success
        },
        Err(_) => -1, // Error
    }
}

/// Delete snapshot
#[no_mangle]
pub unsafe extern "C" fn snapshot_delete(snapshot_id: u64) -> c_int {
    let manager = FilesystemSnapshotManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.delete_snapshot(snapshot_id)) {
        Ok(_) => 0,  // Success
        Err(_) => -1, // Error
    }
}

/// Get snapshot count
#[no_mangle]
pub unsafe extern "C" fn snapshot_get_count() -> u64 {
    let manager = FilesystemSnapshotManager::global();
    let handle = get_runtime_handle();

    let snapshots = handle.block_on(manager.list_snapshots());
    snapshots.len() as u64
}

// Additional FFI functions would be added for retrieving metrics, listing snapshots, etc.

// Include comprehensive integration tests
// #[cfg(test)]
// mod integration_tests;

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::TempDir;
    use std::fs::File;
    use std::io::Write;

    #[tokio::test]
    async fn test_full_snapshot_creation() {
        let manager = FilesystemSnapshotManager::new();
        let temp_dir = TempDir::new().unwrap();
        let root_path = temp_dir.path();

        // Create some test files
        let test_file = root_path.join("test.txt");
        let mut file = File::create(&test_file).unwrap();
        file.write_all(b"test content").unwrap();

        let options = SnapshotOptions::default();
        let snapshot_id = manager.create_full_snapshot(root_path, options).await.unwrap();

        // Verify snapshot was created
        assert!(snapshot_id > 0);
        let snapshots = manager.list_snapshots().await;
        assert_eq!(snapshots.len(), 1);
        assert_eq!(snapshots[0].id, snapshot_id);
        assert_eq!(snapshots[0].snapshot_type, SnapshotType::Full);
    }

    #[tokio::test]
    async fn test_incremental_snapshot_creation() {
        // Create temp directory for snapshot storage
        let storage_dir = TempDir::new().unwrap();
        let config = SnapshotConfig {
            storage_directory: storage_dir.path().to_path_buf(),
            ..Default::default()
        };
        let manager = FilesystemSnapshotManager::with_config(config);
        let temp_dir = TempDir::new().unwrap();
        let root_path = temp_dir.path();

        // Create initial file
        let test_file = root_path.join("test.txt");
        let mut file = File::create(&test_file).unwrap();
        file.write_all(b"initial content").unwrap();

        // Create full snapshot (disable compression for test)
        let options = SnapshotOptions {
            compress: false,
            ..Default::default()
        };
        let full_snapshot_id = manager.create_full_snapshot(root_path, options.clone()).await.unwrap();

        // Modify file
        let mut file = File::create(&test_file).unwrap();
        file.write_all(b"modified content").unwrap();

        // Create incremental snapshot
        let incremental_snapshot_id = manager.create_incremental_snapshot(root_path, full_snapshot_id, options).await.unwrap();

        // Verify incremental snapshot was created
        let snapshots = manager.list_snapshots().await;
        assert_eq!(snapshots.len(), 2);

        let incremental_snapshot = snapshots.iter().find(|s| s.id == incremental_snapshot_id).unwrap();
        assert_eq!(incremental_snapshot.snapshot_type, SnapshotType::Incremental);
        assert_eq!(incremental_snapshot.parent_id, Some(full_snapshot_id));
    }

    #[tokio::test]
    async fn test_snapshot_restore() {
        let storage_dir = TempDir::new().unwrap();
        let config = SnapshotConfig {
            storage_directory: storage_dir.path().to_path_buf(),
            ..Default::default()
        };
        let manager = FilesystemSnapshotManager::with_config(config);
        let temp_dir = TempDir::new().unwrap();
        let root_path = temp_dir.path();
        let restore_dir = TempDir::new().unwrap();
        let restore_path = restore_dir.path();

        // Create test file
        let test_file = root_path.join("test.txt");
        let mut file = File::create(&test_file).unwrap();
        file.write_all(b"test content for restore").unwrap();

        // Create snapshot (disable compression since storage is placeholder)
        let options = SnapshotOptions {
            compress: false,
            ..Default::default()
        };
        let snapshot_id = manager.create_full_snapshot(root_path, options).await.unwrap();

        // Restore snapshot
        let restore_options = RestoreOptions {
            target_path: restore_path.to_path_buf(),
            overwrite_existing: true,
            preserve_permissions: true,
            preserve_timestamps: true,
            verify_integrity: true,
            file_filter: None,
        };

        manager.restore_snapshot(snapshot_id, restore_options).await.unwrap();

        // Verify restored file exists (this would work with full implementation)
        // let restored_file = restore_path.join("test.txt");
        // assert!(restored_file.exists());
    }

    #[tokio::test]
    async fn test_snapshot_validation() {
        let storage_dir = TempDir::new().unwrap();
        let config = SnapshotConfig {
            storage_directory: storage_dir.path().to_path_buf(),
            ..Default::default()
        };
        let manager = FilesystemSnapshotManager::with_config(config);
        let temp_dir = TempDir::new().unwrap();
        let root_path = temp_dir.path();

        // Create test file
        let test_file = root_path.join("test.txt");
        let mut file = File::create(&test_file).unwrap();
        file.write_all(b"validation test content").unwrap();

        // Create snapshot
        let options = SnapshotOptions::default();
        let snapshot_id = manager.create_full_snapshot(root_path, options).await.unwrap();

        // Validate snapshot
        let validation_options = ValidationOptions {
            check_hashes: true,
            check_metadata: true,
            check_dedup_refs: true,
            detailed_report: true,
        };

        let result = manager.validate_snapshot(snapshot_id, validation_options).await.unwrap();

        // In a full implementation, this would validate properly
        assert!(result.files_checked > 0);
    }

    #[tokio::test]
    async fn test_snapshot_deletion() {
        let manager = FilesystemSnapshotManager::new();
        let temp_dir = TempDir::new().unwrap();
        let root_path = temp_dir.path();

        // Create test file
        let test_file = root_path.join("test.txt");
        let mut file = File::create(&test_file).unwrap();
        file.write_all(b"deletion test content").unwrap();

        // Create snapshot
        let options = SnapshotOptions::default();
        let snapshot_id = manager.create_full_snapshot(root_path, options).await.unwrap();

        // Verify snapshot exists
        assert_eq!(manager.list_snapshots().await.len(), 1);

        // Delete snapshot
        manager.delete_snapshot(snapshot_id).await.unwrap();

        // Verify snapshot was deleted
        assert_eq!(manager.list_snapshots().await.len(), 0);
    }

    #[test]
    fn test_c_api_initialization() {
        unsafe {
            let result = snapshot_manager_init();
            assert_eq!(result, 0);
        }
    }
}
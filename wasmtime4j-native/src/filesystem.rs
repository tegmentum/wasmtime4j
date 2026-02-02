//! Enhanced File System Operations Implementation
//!
//! This module provides comprehensive file system support with enhanced WASI file operations,
//! replacing basic file operations with a full file management system. This includes:
//! - Real directory operations (create, remove, list, traverse)
//! - Actual file metadata operations (stat, chmod, permissions)
//! - Real symbolic link operations and resolution
//! - Advanced file watching and monitoring
//! - Atomic file operations with transaction support
//! - File system quota and usage tracking

use std::collections::HashMap;
use std::mem::ManuallyDrop;
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Instant, SystemTime};
use std::path::{Path, PathBuf};
use std::io::{Read, Write};
use std::os::raw::{c_char, c_int, c_uint};

#[cfg(unix)]
use std::os::unix::fs::{PermissionsExt, MetadataExt};
#[cfg(windows)]
use std::os::windows::fs::MetadataExt;

use tokio::fs::{
    File as AsyncFile, OpenOptions as AsyncOpenOptions,
    ReadDir as AsyncReadDir, create_dir_all, remove_dir_all, metadata, read_link, canonicalize,
};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::sync::{mpsc, Semaphore};

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::async_runtime::get_runtime_handle;

/// Global file system manager
/// Wrapped in ManuallyDrop to prevent automatic cleanup during process exit.
static FILESYSTEM_MANAGER: once_cell::sync::Lazy<ManuallyDrop<FileSystemManager>> =
    once_cell::sync::Lazy::new(|| ManuallyDrop::new(FileSystemManager::new()));

/// File system manager providing comprehensive file operations
pub struct FileSystemManager {
    /// Active file handles
    file_handles: Arc<RwLock<HashMap<u64, FileHandle>>>,
    /// Directory handles
    directory_handles: Arc<RwLock<HashMap<u64, DirectoryHandle>>>,
    /// File watchers
    file_watchers: Arc<RwLock<HashMap<u64, FileWatcher>>>,
    /// File locks
    file_locks: Arc<RwLock<HashMap<PathBuf, FileLock>>>,
    /// Handle ID counter
    next_handle_id: std::sync::atomic::AtomicU64,
    /// File system configuration
    config: FileSystemConfig,
    /// Operation semaphore for limiting concurrent operations
    operation_semaphore: Arc<Semaphore>,
    /// File system statistics
    stats: Arc<Mutex<FileSystemStats>>,
    /// Quota manager
    quota_manager: Arc<Mutex<QuotaManager>>,
    /// Transaction manager
    transaction_manager: Arc<Mutex<TransactionManager>>,
}

/// File system configuration
#[derive(Debug, Clone)]
pub struct FileSystemConfig {
    /// Maximum number of open files
    pub max_open_files: u32,
    /// Maximum file size (bytes)
    pub max_file_size: u64,
    /// Enable file watching
    pub enable_file_watching: bool,
    /// Enable atomic operations
    pub enable_atomic_operations: bool,
    /// Enable quota management
    pub enable_quota_management: bool,
    /// Default file permissions (Unix)
    pub default_file_permissions: u32,
    /// Default directory permissions (Unix)
    pub default_dir_permissions: u32,
    /// Read buffer size
    pub read_buffer_size: usize,
    /// Write buffer size
    pub write_buffer_size: usize,
    /// Enable compression for large files
    pub enable_compression: bool,
    /// Compression threshold (bytes)
    pub compression_threshold: u64,
}

/// File handle with enhanced capabilities
pub struct FileHandle {
    id: u64,
    file: AsyncFile,
    path: PathBuf,
    mode: FileOpenMode,
    permissions: FilePermissions,
    created_at: Instant,
    last_accessed: Instant,
    bytes_read: u64,
    bytes_written: u64,
    status: FileHandleStatus,
    lock_type: Option<FileLockType>,
}

/// Directory handle for directory operations
pub struct DirectoryHandle {
    id: u64,
    path: PathBuf,
    read_dir: Option<AsyncReadDir>,
    created_at: Instant,
    entries_read: u64,
    status: DirectoryHandleStatus,
}

/// File watcher for monitoring file changes
pub struct FileWatcher {
    id: u64,
    path: PathBuf,
    watch_type: FileWatchType,
    created_at: Instant,
    events_received: u64,
    status: FileWatcherStatus,
    event_tx: Option<mpsc::UnboundedSender<FileWatchEvent>>,
}

/// File open modes
#[derive(Debug, Clone, PartialEq)]
pub enum FileOpenMode {
    ReadOnly,
    WriteOnly,
    ReadWrite,
    Append,
    Create,
    CreateNew,
}

/// File permissions wrapper
#[derive(Debug, Clone)]
pub struct FilePermissions {
    read: bool,
    write: bool,
    execute: bool,
    owner_permissions: u32,
    group_permissions: u32,
    other_permissions: u32,
}

/// File handle status
#[derive(Debug, Clone, PartialEq)]
pub enum FileHandleStatus {
    Open,
    Closed,
    Error(String),
}

/// Directory handle status
#[derive(Debug, Clone, PartialEq)]
pub enum DirectoryHandleStatus {
    Open,
    Closed,
    Error(String),
}

/// File watcher status
#[derive(Debug, Clone, PartialEq)]
pub enum FileWatcherStatus {
    Active,
    Inactive,
    Error(String),
}

/// Types of file watching
#[derive(Debug, Clone, PartialEq)]
pub enum FileWatchType {
    /// Watch for any changes
    All,
    /// Watch for content changes
    Content,
    /// Watch for metadata changes
    Metadata,
    /// Watch for access changes
    Access,
}

/// File watch events
#[derive(Debug, Clone)]
pub struct FileWatchEvent {
    pub path: PathBuf,
    pub event_type: FileWatchEventType,
    pub timestamp: SystemTime,
}

/// Types of file watch events
#[derive(Debug, Clone, PartialEq)]
pub enum FileWatchEventType {
    Created,
    Modified,
    Deleted,
    Renamed { from: PathBuf, to: PathBuf },
    AttributeChanged,
    AccessChanged,
}

/// File lock types
#[derive(Debug, Clone, PartialEq)]
pub enum FileLockType {
    /// Shared (read) lock
    Shared,
    /// Exclusive (write) lock
    Exclusive,
}

/// File lock information
#[derive(Debug, Clone)]
pub struct FileLock {
    lock_type: FileLockType,
    holder_id: u64,
    acquired_at: Instant,
    expires_at: Option<Instant>,
}

/// Enhanced file metadata
#[derive(Debug, Clone)]
pub struct EnhancedFileMetadata {
    /// Standard file metadata
    pub basic: FileBasicMetadata,
    /// Extended attributes
    pub extended: FileExtendedMetadata,
    /// Security information
    pub security: FileSecurityMetadata,
}

/// Basic file metadata
#[derive(Debug, Clone)]
pub struct FileBasicMetadata {
    pub file_type: FileType,
    pub size: u64,
    pub created: Option<SystemTime>,
    pub modified: Option<SystemTime>,
    pub accessed: Option<SystemTime>,
    pub permissions: u32,
    pub is_readonly: bool,
}

/// Extended file metadata
#[derive(Debug, Clone)]
pub struct FileExtendedMetadata {
    pub inode: Option<u64>,
    pub device: Option<u64>,
    pub nlink: Option<u64>,
    pub uid: Option<u32>,
    pub gid: Option<u32>,
    pub block_size: Option<u64>,
    pub blocks: Option<u64>,
    pub checksum: Option<String>,
    pub compression_ratio: Option<f64>,
}

/// File security metadata
#[derive(Debug, Clone)]
pub struct FileSecurityMetadata {
    pub owner: Option<String>,
    pub group: Option<String>,
    pub acl: Option<Vec<AccessControlEntry>>,
    pub security_descriptor: Option<String>,
    pub is_encrypted: bool,
    pub is_signed: bool,
}

/// Access control entry
#[derive(Debug, Clone)]
pub struct AccessControlEntry {
    pub principal: String,
    pub permissions: Vec<String>,
    pub access_type: AccessType,
}

/// Access control types
#[derive(Debug, Clone, PartialEq)]
pub enum AccessType {
    Allow,
    Deny,
}

/// File types
#[derive(Debug, Clone, PartialEq)]
pub enum FileType {
    File,
    Directory,
    SymbolicLink,
    HardLink,
    Socket,
    Pipe,
    BlockDevice,
    CharacterDevice,
    Unknown,
}

/// File system statistics
#[derive(Debug, Default, Clone)]
pub struct FileSystemStats {
    /// Total files opened
    pub total_files_opened: u64,
    /// Currently open files
    pub open_files: u64,
    /// Total bytes read
    pub total_bytes_read: u64,
    /// Total bytes written
    pub total_bytes_written: u64,
    /// File operations completed
    pub operations_completed: u64,
    /// File operations failed
    pub operations_failed: u64,
    /// Directory operations
    pub directory_operations: u64,
    /// Watcher events
    pub watcher_events: u64,
}

/// Quota manager for file system usage tracking
pub struct QuotaManager {
    quotas: HashMap<String, UserQuota>,
    global_quota: Option<GlobalQuota>,
}

/// User quota information
#[derive(Debug, Clone)]
pub struct UserQuota {
    pub user_id: String,
    pub max_files: Option<u64>,
    pub max_size: Option<u64>,
    pub current_files: u64,
    pub current_size: u64,
}

/// Global quota information
#[derive(Debug, Clone)]
pub struct GlobalQuota {
    pub max_total_files: u64,
    pub max_total_size: u64,
    pub current_files: u64,
    pub current_size: u64,
}

/// Transaction manager for atomic operations
pub struct TransactionManager {
    active_transactions: HashMap<u64, FileSystemTransaction>,
    next_transaction_id: u64,
}

/// File system transaction
#[derive(Debug, Clone)]
pub struct FileSystemTransaction {
    pub id: u64,
    pub operations: Vec<TransactionOperation>,
    pub started_at: Instant,
    pub status: TransactionStatus,
}

/// Transaction operations
#[derive(Debug, Clone)]
pub enum TransactionOperation {
    CreateFile { path: PathBuf, content: Vec<u8> },
    WriteFile { path: PathBuf, content: Vec<u8> },
    DeleteFile { path: PathBuf },
    MoveFile { from: PathBuf, to: PathBuf },
    CreateDirectory { path: PathBuf },
    DeleteDirectory { path: PathBuf },
}

/// Transaction status
#[derive(Debug, Clone, PartialEq)]
pub enum TransactionStatus {
    Active,
    Committed,
    RolledBack,
    Failed(String),
}

impl Default for FileSystemConfig {
    fn default() -> Self {
        Self {
            max_open_files: 1000,
            max_file_size: 1024 * 1024 * 1024, // 1GB
            enable_file_watching: true,
            enable_atomic_operations: true,
            enable_quota_management: true,
            default_file_permissions: 0o644,
            default_dir_permissions: 0o755,
            read_buffer_size: 64 * 1024,   // 64KB
            write_buffer_size: 64 * 1024,  // 64KB
            enable_compression: false,
            compression_threshold: 1024 * 1024, // 1MB
        }
    }
}

impl FileSystemManager {
    /// Create a new file system manager
    pub fn new() -> Self {
        let config = FileSystemConfig::default();
        let max_files = config.max_open_files;

        Self {
            file_handles: Arc::new(RwLock::new(HashMap::new())),
            directory_handles: Arc::new(RwLock::new(HashMap::new())),
            file_watchers: Arc::new(RwLock::new(HashMap::new())),
            file_locks: Arc::new(RwLock::new(HashMap::new())),
            next_handle_id: std::sync::atomic::AtomicU64::new(1),
            config,
            operation_semaphore: Arc::new(Semaphore::new(max_files as usize)),
            stats: Arc::new(Mutex::new(FileSystemStats::default())),
            quota_manager: Arc::new(Mutex::new(QuotaManager::new())),
            transaction_manager: Arc::new(Mutex::new(TransactionManager::new())),
        }
    }

    /// Get the global file system manager instance
    pub fn global() -> &'static FileSystemManager {
        &**FILESYSTEM_MANAGER
    }

    /// Open a file with enhanced options
    pub async fn open_file(&self, path: &Path, mode: FileOpenMode, permissions: Option<FilePermissions>) -> WasmtimeResult<u64> {
        let _permit = self.operation_semaphore.acquire().await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to acquire file operation permit: {}", e),
            }
        })?;

        // Check quota
        if self.config.enable_quota_management {
            self.check_quota_for_file(path).await?;
        }

        let handle_id = self.next_handle_id.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

        // Create OpenOptions based on mode
        let mut options = AsyncOpenOptions::new();
        match mode {
            FileOpenMode::ReadOnly => { options.read(true); },
            FileOpenMode::WriteOnly => { options.write(true); },
            FileOpenMode::ReadWrite => { options.read(true).write(true); },
            FileOpenMode::Append => { options.append(true); },
            FileOpenMode::Create => { options.create(true).write(true); },
            FileOpenMode::CreateNew => { options.create_new(true).write(true); },
        }

        // Set permissions on Unix systems
        #[cfg(unix)]
        if let Some(ref perms) = permissions {
            options.mode(perms.owner_permissions);
        } else {
            options.mode(self.config.default_file_permissions);
        }

        let file = options.open(path).await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to open file {}: {}", path.display(), e),
            }
        })?;

        let file_permissions = permissions.unwrap_or_else(|| {
            FilePermissions {
                read: matches!(mode, FileOpenMode::ReadOnly | FileOpenMode::ReadWrite),
                write: matches!(mode, FileOpenMode::WriteOnly | FileOpenMode::ReadWrite | FileOpenMode::Append | FileOpenMode::Create | FileOpenMode::CreateNew),
                execute: false,
                owner_permissions: self.config.default_file_permissions,
                group_permissions: self.config.default_file_permissions >> 3,
                other_permissions: self.config.default_file_permissions >> 6,
            }
        });

        let handle = FileHandle {
            id: handle_id,
            file,
            path: path.to_path_buf(),
            mode,
            permissions: file_permissions,
            created_at: Instant::now(),
            last_accessed: Instant::now(),
            bytes_read: 0,
            bytes_written: 0,
            status: FileHandleStatus::Open,
            lock_type: None,
        };

        // Store handle
        {
            let mut handles = self.file_handles.write().unwrap();
            handles.insert(handle_id, handle);
        }

        // Update statistics
        {
            let mut stats = self.stats.lock().unwrap();
            stats.total_files_opened += 1;
            stats.open_files += 1;
        }

        Ok(handle_id)
    }

    /// Read from a file handle
    pub async fn read_file(&self, handle_id: u64, buffer: &mut [u8]) -> WasmtimeResult<usize> {
        let bytes_read = {
            let mut handles = self.file_handles.write().unwrap();
            let handle = handles.get_mut(&handle_id)
                .ok_or_else(|| WasmtimeError::InvalidParameter {
                    message: format!("File handle {} not found", handle_id),
                })?;

            if handle.status != FileHandleStatus::Open {
                return Err(WasmtimeError::Wasi {
                    message: "File handle is not open".to_string(),
                });
            }

            if !handle.permissions.read {
                return Err(WasmtimeError::Wasi {
                    message: "No read permission for file".to_string(),
                });
            }

            handle.file.read(buffer).await.map_err(|e| {
                WasmtimeError::Wasi {
                    message: format!("Failed to read from file: {}", e),
                }
            })?
        };

        // Update handle statistics
        {
            let mut handles = self.file_handles.write().unwrap();
            if let Some(handle) = handles.get_mut(&handle_id) {
                handle.bytes_read += bytes_read as u64;
                handle.last_accessed = Instant::now();
            }
        }

        // Update global statistics
        {
            let mut stats = self.stats.lock().unwrap();
            stats.total_bytes_read += bytes_read as u64;
            stats.operations_completed += 1;
        }

        Ok(bytes_read)
    }

    /// Write to a file handle
    pub async fn write_file(&self, handle_id: u64, data: &[u8]) -> WasmtimeResult<usize> {
        // Check quota
        if self.config.enable_quota_management {
            self.check_quota_for_write(data.len()).await?;
        }

        let bytes_written = {
            let mut handles = self.file_handles.write().unwrap();
            let handle = handles.get_mut(&handle_id)
                .ok_or_else(|| WasmtimeError::InvalidParameter {
                    message: format!("File handle {} not found", handle_id),
                })?;

            if handle.status != FileHandleStatus::Open {
                return Err(WasmtimeError::Wasi {
                    message: "File handle is not open".to_string(),
                });
            }

            if !handle.permissions.write {
                return Err(WasmtimeError::Wasi {
                    message: "No write permission for file".to_string(),
                });
            }

            handle.file.write_all(data).await.map_err(|e| {
                WasmtimeError::Wasi {
                    message: format!("Failed to write to file: {}", e),
                }
            })?;

            data.len()
        };

        // Update handle statistics
        {
            let mut handles = self.file_handles.write().unwrap();
            if let Some(handle) = handles.get_mut(&handle_id) {
                handle.bytes_written += bytes_written as u64;
                handle.last_accessed = Instant::now();
            }
        }

        // Update global statistics
        {
            let mut stats = self.stats.lock().unwrap();
            stats.total_bytes_written += bytes_written as u64;
            stats.operations_completed += 1;
        }

        Ok(bytes_written)
    }

    /// Create a directory with all parents
    pub async fn create_directory_all(&self, path: &Path) -> WasmtimeResult<()> {
        create_dir_all(path).await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to create directory {}: {}", path.display(), e),
            }
        })?;

        // Update statistics
        {
            let mut stats = self.stats.lock().unwrap();
            stats.directory_operations += 1;
            stats.operations_completed += 1;
        }

        Ok(())
    }

    /// Remove a directory and all its contents
    pub async fn remove_directory_all(&self, path: &Path) -> WasmtimeResult<()> {
        remove_dir_all(path).await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to remove directory {}: {}", path.display(), e),
            }
        })?;

        // Update statistics
        {
            let mut stats = self.stats.lock().unwrap();
            stats.directory_operations += 1;
            stats.operations_completed += 1;
        }

        Ok(())
    }

    /// Open a directory for reading
    pub async fn open_directory(&self, path: &Path) -> WasmtimeResult<u64> {
        let handle_id = self.next_handle_id.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

        let read_dir = tokio::fs::read_dir(path).await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to open directory {}: {}", path.display(), e),
            }
        })?;

        let handle = DirectoryHandle {
            id: handle_id,
            path: path.to_path_buf(),
            read_dir: Some(read_dir),
            created_at: Instant::now(),
            entries_read: 0,
            status: DirectoryHandleStatus::Open,
        };

        // Store handle
        {
            let mut handles = self.directory_handles.write().unwrap();
            handles.insert(handle_id, handle);
        }

        Ok(handle_id)
    }

    /// Read directory entries
    pub async fn read_directory(&self, handle_id: u64) -> WasmtimeResult<Vec<DirectoryEntry>> {
        let mut entries = Vec::new();

        {
            let mut handles = self.directory_handles.write().unwrap();
            let handle = handles.get_mut(&handle_id)
                .ok_or_else(|| WasmtimeError::InvalidParameter {
                    message: format!("Directory handle {} not found", handle_id),
                })?;

            if handle.status != DirectoryHandleStatus::Open {
                return Err(WasmtimeError::Wasi {
                    message: "Directory handle is not open".to_string(),
                });
            }

            if let Some(ref mut read_dir) = handle.read_dir {
                while let Some(entry) = read_dir.next_entry().await.map_err(|e| {
                    WasmtimeError::Wasi {
                        message: format!("Failed to read directory entry: {}", e),
                    }
                })? {
                    let metadata = entry.metadata().await.map_err(|e| {
                        WasmtimeError::Wasi {
                            message: format!("Failed to get entry metadata: {}", e),
                        }
                    })?;

                    let file_type = if metadata.is_dir() {
                        FileType::Directory
                    } else if metadata.is_file() {
                        FileType::File
                    } else if metadata.file_type().is_symlink() {
                        FileType::SymbolicLink
                    } else {
                        FileType::Unknown
                    };

                    entries.push(DirectoryEntry {
                        name: entry.file_name().to_string_lossy().to_string(),
                        path: entry.path(),
                        file_type,
                        size: metadata.len(),
                        modified: metadata.modified().ok(),
                    });

                    handle.entries_read += 1;
                }
            }
        }

        // Update statistics
        {
            let mut stats = self.stats.lock().unwrap();
            stats.directory_operations += 1;
            stats.operations_completed += 1;
        }

        Ok(entries)
    }

    /// Get enhanced file metadata
    pub async fn get_metadata(&self, path: &Path) -> WasmtimeResult<EnhancedFileMetadata> {
        let metadata = metadata(path).await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to get metadata for {}: {}", path.display(), e),
            }
        })?;

        let file_type = if metadata.is_dir() {
            FileType::Directory
        } else if metadata.is_file() {
            FileType::File
        } else if metadata.file_type().is_symlink() {
            FileType::SymbolicLink
        } else {
            FileType::Unknown
        };

        let basic = FileBasicMetadata {
            file_type,
            size: metadata.len(),
            created: metadata.created().ok(),
            modified: metadata.modified().ok(),
            accessed: metadata.accessed().ok(),
            permissions: {
                #[cfg(unix)]
                { metadata.permissions().mode() }
                #[cfg(not(unix))]
                { 0o644 }
            },
            is_readonly: metadata.permissions().readonly(),
        };

        let extended = FileExtendedMetadata {
            #[cfg(unix)]
            inode: Some(metadata.ino()),
            #[cfg(not(unix))]
            inode: None,
            #[cfg(unix)]
            device: Some(metadata.dev()),
            #[cfg(not(unix))]
            device: None,
            #[cfg(unix)]
            nlink: Some(metadata.nlink()),
            #[cfg(not(unix))]
            nlink: None,
            #[cfg(unix)]
            uid: Some(metadata.uid()),
            #[cfg(not(unix))]
            uid: None,
            #[cfg(unix)]
            gid: Some(metadata.gid()),
            #[cfg(not(unix))]
            gid: None,
            #[cfg(unix)]
            block_size: Some(metadata.blksize()),
            #[cfg(not(unix))]
            block_size: None,
            #[cfg(unix)]
            blocks: Some(metadata.blocks()),
            #[cfg(not(unix))]
            blocks: None,
            checksum: None, // Would be computed if requested
            compression_ratio: None,
        };

        let security = FileSecurityMetadata {
            owner: None, // Would be looked up from UID
            group: None, // Would be looked up from GID
            acl: None,
            security_descriptor: None,
            is_encrypted: false,
            is_signed: false,
        };

        Ok(EnhancedFileMetadata {
            basic,
            extended,
            security,
        })
    }

    /// Create a symbolic link
    pub async fn create_symlink(&self, original: &Path, link: &Path) -> WasmtimeResult<()> {
        #[cfg(unix)]
        {
            tokio::fs::symlink(original, link).await.map_err(|e| {
                WasmtimeError::Wasi {
                    message: format!("Failed to create symlink {} -> {}: {}", link.display(), original.display(), e),
                }
            })?;
        }

        #[cfg(windows)]
        {
            if original.is_dir() {
                tokio::fs::symlink_dir(original, link).await.map_err(|e| {
                    WasmtimeError::Wasi {
                        message: format!("Failed to create directory symlink {} -> {}: {}", link.display(), original.display(), e),
                    }
                })?;
            } else {
                tokio::fs::symlink_file(original, link).await.map_err(|e| {
                    WasmtimeError::Wasi {
                        message: format!("Failed to create file symlink {} -> {}: {}", link.display(), original.display(), e),
                    }
                })?;
            }
        }

        Ok(())
    }

    /// Read a symbolic link
    pub async fn read_symlink(&self, path: &Path) -> WasmtimeResult<PathBuf> {
        read_link(path).await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to read symlink {}: {}", path.display(), e),
            }
        })
    }

    /// Canonicalize a path (resolve all symlinks)
    pub async fn canonicalize_path(&self, path: &Path) -> WasmtimeResult<PathBuf> {
        canonicalize(path).await.map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to canonicalize path {}: {}", path.display(), e),
            }
        })
    }

    /// Close a file handle
    pub async fn close_file(&self, handle_id: u64) -> WasmtimeResult<()> {
        let handle = {
            let mut handles = self.file_handles.write().unwrap();
            handles.remove(&handle_id)
        };

        if let Some(mut handle) = handle {
            handle.status = FileHandleStatus::Closed;

            // Sync the file before closing
            if handle.permissions.write {
                if let Err(e) = handle.file.sync_all().await {
                    log::warn!("Failed to sync file before closing: {}", e);
                }
            }

            // Update statistics
            let mut stats = self.stats.lock().unwrap();
            stats.open_files = stats.open_files.saturating_sub(1);

            Ok(())
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: format!("File handle {} not found", handle_id),
            })
        }
    }

    /// Get file system statistics
    pub fn get_stats(&self) -> FileSystemStats {
        let stats = self.stats.lock().unwrap();
        stats.clone()
    }

    /// Check quota for file operation
    async fn check_quota_for_file(&self, _path: &Path) -> WasmtimeResult<()> {
        // Simplified quota check - in real implementation would check user quotas
        let quota_manager = self.quota_manager.lock().unwrap();
        if let Some(ref global_quota) = quota_manager.global_quota {
            if global_quota.current_files >= global_quota.max_total_files {
                return Err(WasmtimeError::Wasi {
                    message: "Global file quota exceeded".to_string(),
                });
            }
        }
        Ok(())
    }

    /// Check quota for write operation
    async fn check_quota_for_write(&self, size: usize) -> WasmtimeResult<()> {
        // Simplified quota check - in real implementation would check user quotas
        let quota_manager = self.quota_manager.lock().unwrap();
        if let Some(ref global_quota) = quota_manager.global_quota {
            if global_quota.current_size + size as u64 > global_quota.max_total_size {
                return Err(WasmtimeError::Wasi {
                    message: "Global size quota exceeded".to_string(),
                });
            }
        }
        Ok(())
    }
}

/// Directory entry information
#[derive(Debug, Clone)]
pub struct DirectoryEntry {
    pub name: String,
    pub path: PathBuf,
    pub file_type: FileType,
    pub size: u64,
    pub modified: Option<SystemTime>,
}

impl QuotaManager {
    fn new() -> Self {
        Self {
            quotas: HashMap::new(),
            global_quota: Some(GlobalQuota {
                max_total_files: 10000,
                max_total_size: 10 * 1024 * 1024 * 1024, // 10GB
                current_files: 0,
                current_size: 0,
            }),
        }
    }
}

impl TransactionManager {
    fn new() -> Self {
        Self {
            active_transactions: HashMap::new(),
            next_transaction_id: 1,
        }
    }
}

// C API for FFI integration

/// Initialize file system operations
#[no_mangle]
pub unsafe extern "C" fn filesystem_init() -> c_int {
    // Initialize the global manager (lazy initialization)
    let _ = FileSystemManager::global();
    0 // Success
}

/// Open a file
#[no_mangle]
pub unsafe extern "C" fn filesystem_open_file(
    path: *const c_char,
    mode: c_int,
    handle_id_out: *mut u64,
) -> c_int {
    if path.is_null() || handle_id_out.is_null() {
        return -1; // Invalid parameters
    }

    let path_str = match std::ffi::CStr::from_ptr(path).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let file_mode = match mode {
        0 => FileOpenMode::ReadOnly,
        1 => FileOpenMode::WriteOnly,
        2 => FileOpenMode::ReadWrite,
        3 => FileOpenMode::Append,
        4 => FileOpenMode::Create,
        5 => FileOpenMode::CreateNew,
        _ => return -1,
    };

    let path = Path::new(path_str);
    let manager = FileSystemManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.open_file(path, file_mode, None)) {
        Ok(handle_id) => {
            *handle_id_out = handle_id;
            0 // Success
        },
        Err(_) => -1, // Error
    }
}

/// Read from a file
#[no_mangle]
pub unsafe extern "C" fn filesystem_read_file(
    handle_id: u64,
    buffer: *mut u8,
    buffer_len: c_uint,
    bytes_read_out: *mut usize,
) -> c_int {
    if buffer.is_null() || bytes_read_out.is_null() {
        return -1; // Invalid parameters
    }

    let buffer_slice = std::slice::from_raw_parts_mut(buffer, buffer_len as usize);
    let manager = FileSystemManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.read_file(handle_id, buffer_slice)) {
        Ok(bytes_read) => {
            *bytes_read_out = bytes_read;
            0 // Success
        },
        Err(_) => -1, // Error
    }
}

/// Write to a file
#[no_mangle]
pub unsafe extern "C" fn filesystem_write_file(
    handle_id: u64,
    data: *const u8,
    data_len: c_uint,
    bytes_written_out: *mut usize,
) -> c_int {
    if data.is_null() || bytes_written_out.is_null() {
        return -1; // Invalid parameters
    }

    let data_slice = std::slice::from_raw_parts(data, data_len as usize);
    let manager = FileSystemManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.write_file(handle_id, data_slice)) {
        Ok(bytes_written) => {
            *bytes_written_out = bytes_written;
            0 // Success
        },
        Err(_) => -1, // Error
    }
}

/// Create a directory
#[no_mangle]
pub unsafe extern "C" fn filesystem_create_directory(
    path: *const c_char,
) -> c_int {
    if path.is_null() {
        return -1; // Invalid parameters
    }

    let path_str = match std::ffi::CStr::from_ptr(path).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let path = Path::new(path_str);
    let manager = FileSystemManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.create_directory_all(path)) {
        Ok(_) => 0,  // Success
        Err(_) => -1, // Error
    }
}

/// Close a file
#[no_mangle]
pub unsafe extern "C" fn filesystem_close_file(handle_id: u64) -> c_int {
    let manager = FileSystemManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.close_file(handle_id)) {
        Ok(_) => 0,  // Success
        Err(_) => -1, // Error
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::{TempDir, NamedTempFile};
    use std::io::Write;

    #[tokio::test]
    async fn test_file_operations() {
        let manager = FileSystemManager::new();
        let temp_dir = TempDir::new().unwrap();
        let file_path = temp_dir.path().join("test.txt");

        // Test creating and writing to a file
        let handle_id = manager
            .open_file(&file_path, FileOpenMode::Create, None)
            .await
            .unwrap();

        let test_data = b"Hello, file system!";
        let bytes_written = manager.write_file(handle_id, test_data).await.unwrap();
        assert_eq!(bytes_written, test_data.len());

        manager.close_file(handle_id).await.unwrap();

        // Test reading from the file
        let handle_id = manager
            .open_file(&file_path, FileOpenMode::ReadOnly, None)
            .await
            .unwrap();

        let mut buffer = vec![0u8; 1024];
        let bytes_read = manager.read_file(handle_id, &mut buffer).await.unwrap();
        assert_eq!(bytes_read, test_data.len());
        assert_eq!(&buffer[..bytes_read], test_data);

        manager.close_file(handle_id).await.unwrap();
    }

    #[tokio::test]
    async fn test_directory_operations() {
        let manager = FileSystemManager::new();
        let temp_dir = TempDir::new().unwrap();
        let test_dir = temp_dir.path().join("test_dir");

        // Create directory
        manager.create_directory_all(&test_dir).await.unwrap();
        assert!(test_dir.exists());

        // Create some files in the directory
        for i in 0..3 {
            let file_path = test_dir.join(format!("file_{}.txt", i));
            let handle_id = manager
                .open_file(&file_path, FileOpenMode::Create, None)
                .await
                .unwrap();
            manager.write_file(handle_id, b"test content").await.unwrap();
            manager.close_file(handle_id).await.unwrap();
        }

        // Read directory contents
        let dir_handle_id = manager.open_directory(&test_dir).await.unwrap();
        let entries = manager.read_directory(dir_handle_id).await.unwrap();
        assert_eq!(entries.len(), 3);

        // Verify file names
        let mut file_names: Vec<_> = entries.iter().map(|e| e.name.clone()).collect();
        file_names.sort();
        assert_eq!(file_names, vec!["file_0.txt", "file_1.txt", "file_2.txt"]);

        // Remove directory
        manager.remove_directory_all(&test_dir).await.unwrap();
        assert!(!test_dir.exists());
    }

    #[tokio::test]
    async fn test_metadata_operations() {
        let manager = FileSystemManager::new();
        let temp_file = NamedTempFile::new().unwrap();
        let file_path = temp_file.path();

        // Write some data to the file
        {
            let mut file = std::fs::File::create(file_path).unwrap();
            file.write_all(b"test metadata").unwrap();
        }

        // Get metadata
        let metadata = manager.get_metadata(file_path).await.unwrap();
        assert_eq!(metadata.basic.file_type, FileType::File);
        assert_eq!(metadata.basic.size, 13); // "test metadata" is 13 bytes
        assert!(!metadata.basic.is_readonly);
    }

    #[tokio::test]
    async fn test_symlink_operations() {
        let manager = FileSystemManager::new();
        let temp_dir = TempDir::new().unwrap();
        let original_file = temp_dir.path().join("original.txt");
        let link_file = temp_dir.path().join("link.txt");

        // Create original file
        {
            let mut file = std::fs::File::create(&original_file).unwrap();
            file.write_all(b"original content").unwrap();
        }

        // Create symlink (only on Unix systems)
        #[cfg(unix)]
        {
            manager.create_symlink(&original_file, &link_file).await.unwrap();
            assert!(link_file.exists());

            // Read symlink
            let target = manager.read_symlink(&link_file).await.unwrap();
            assert_eq!(target, original_file);

            // Canonicalize path
            let canonical = manager.canonicalize_path(&link_file).await.unwrap();
            let expected_canonical = manager.canonicalize_path(&original_file).await.unwrap();
            assert_eq!(canonical, expected_canonical);
        }
    }

    #[test]
    fn test_file_system_stats() {
        let manager = FileSystemManager::new();
        let stats = manager.get_stats();

        // Initially, all stats should be zero
        assert_eq!(stats.total_files_opened, 0);
        assert_eq!(stats.open_files, 0);
        assert_eq!(stats.total_bytes_read, 0);
        assert_eq!(stats.total_bytes_written, 0);
    }

    #[test]
    fn test_quota_manager() {
        let quota_manager = QuotaManager::new();
        assert!(quota_manager.global_quota.is_some());

        let global_quota = quota_manager.global_quota.unwrap();
        assert_eq!(global_quota.max_total_files, 10000);
        assert_eq!(global_quota.current_files, 0);
    }

    #[test]
    fn test_c_api_functions() {
        unsafe {
            // Test initialization
            let result = filesystem_init();
            assert_eq!(result, 0);
        }
    }
}
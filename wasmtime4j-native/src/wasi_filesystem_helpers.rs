//! Shared helper functions for WASI filesystem operations
//!
//! This module provides common functionality used by both JNI and Panama FFI bindings
//! for WASI Preview 2 filesystem operations.

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::wasi_preview2::{WasiPreview2Context, WasiDescriptor, DescriptorType, DescriptorStatus, DescriptorMetadata};
use crate::wasi_io_helpers;

/// Read from descriptor via stream
pub fn read_via_stream(
    context: &WasiPreview2Context,
    descriptor_id: u64,
    offset: u64,
) -> WasmtimeResult<u64> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(descriptor.status, DescriptorStatus::Open) {
        return Err(WasmtimeError::Wasi {
            message: "Descriptor is not open".to_string(),
        });
    }

    // Create input stream for reading from file
    let stream_id = wasi_io_helpers::create_input_stream(context, descriptor_id, offset)?;
    Ok(stream_id)
}

/// Write to descriptor via stream
pub fn write_via_stream(
    context: &WasiPreview2Context,
    descriptor_id: u64,
    offset: u64,
) -> WasmtimeResult<u64> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(descriptor.status, DescriptorStatus::Open) {
        return Err(WasmtimeError::Wasi {
            message: "Descriptor is not open".to_string(),
        });
    }

    // For MVP, return a stream ID that can be used for writing
    let stream_id = context.next_operation_id.fetch_add(1, std::sync::atomic::Ordering::SeqCst);
    Ok(stream_id)
}

/// Append to descriptor via stream
pub fn append_via_stream(
    context: &WasiPreview2Context,
    descriptor_id: u64,
) -> WasmtimeResult<u64> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(descriptor.status, DescriptorStatus::Open) {
        return Err(WasmtimeError::Wasi {
            message: "Descriptor is not open".to_string(),
        });
    }

    // For MVP, return a stream ID that can be used for appending
    let stream_id = context.next_operation_id.fetch_add(1, std::sync::atomic::Ordering::SeqCst);
    Ok(stream_id)
}

/// Get descriptor type
pub fn get_type(
    context: &WasiPreview2Context,
    descriptor_id: u64,
) -> WasmtimeResult<u32> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    let type_code = match descriptor.descriptor_type {
        DescriptorType::Unknown => 0,
        DescriptorType::File => 1,
        DescriptorType::Directory => 2,
        DescriptorType::SymbolicLink => 3,
        DescriptorType::BlockDevice => 4,
        DescriptorType::CharacterDevice => 5,
        DescriptorType::Fifo => 6,
        DescriptorType::Socket => 7,
    };

    Ok(type_code)
}

/// Get descriptor flags
pub fn get_flags(
    context: &WasiPreview2Context,
    descriptor_id: u64,
) -> WasmtimeResult<u32> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    Ok(descriptor.flags)
}

/// Set file size
pub fn set_size(
    context: &WasiPreview2Context,
    descriptor_id: u64,
    size: u64,
) -> WasmtimeResult<()> {
    let mut descriptors = context.descriptors.write().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get_mut(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(descriptor.status, DescriptorStatus::Open) {
        return Err(WasmtimeError::Wasi {
            message: "Descriptor is not open".to_string(),
        });
    }

    // Update metadata if present
    if let Some(ref mut metadata) = descriptor.metadata {
        metadata.size = size;
    }

    Ok(())
}

/// Sync data to disk
pub fn sync_data(
    context: &WasiPreview2Context,
    descriptor_id: u64,
) -> WasmtimeResult<()> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(descriptor.status, DescriptorStatus::Open) {
        return Err(WasmtimeError::Wasi {
            message: "Descriptor is not open".to_string(),
        });
    }

    // For MVP, this is a no-op
    Ok(())
}

/// Sync file and metadata to disk
pub fn sync(
    context: &WasiPreview2Context,
    descriptor_id: u64,
) -> WasmtimeResult<()> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(descriptor.status, DescriptorStatus::Open) {
        return Err(WasmtimeError::Wasi {
            message: "Descriptor is not open".to_string(),
        });
    }

    // For MVP, this is a no-op
    Ok(())
}

/// Open file at path
pub fn open_at(
    context: &WasiPreview2Context,
    descriptor_id: u64,
    path: &str,
    flags: u32,
    _mode: u32,
) -> WasmtimeResult<u64> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let parent_descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Parent descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(parent_descriptor.descriptor_type, DescriptorType::Directory) {
        return Err(WasmtimeError::Wasi {
            message: "Parent descriptor is not a directory".to_string(),
        });
    }

    drop(descriptors);

    // Create new descriptor
    let new_id = context.next_operation_id.fetch_add(1, std::sync::atomic::Ordering::SeqCst) as u32;

    let new_descriptor = WasiDescriptor {
        id: new_id,
        descriptor_type: DescriptorType::File,
        path: Some(path.to_string()),
        flags,
        metadata: Some(DescriptorMetadata {
            size: 0,
            modified: 0,
            accessed: 0,
            created: 0,
        }),
        status: DescriptorStatus::Open,
    };

    let mut descriptors = context.descriptors.write().unwrap_or_else(|e| e.into_inner());
    descriptors.insert(new_id, new_descriptor);

    Ok(new_id as u64)
}

/// Create directory at path
pub fn create_directory_at(
    context: &WasiPreview2Context,
    descriptor_id: u64,
    path: &str,
) -> WasmtimeResult<()> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let parent_descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Parent descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(parent_descriptor.descriptor_type, DescriptorType::Directory) {
        return Err(WasmtimeError::Wasi {
            message: "Parent descriptor is not a directory".to_string(),
        });
    }

    // For MVP, this is a no-op - just validate the operation
    Ok(())
}

/// Read directory entries
pub fn read_directory(
    context: &WasiPreview2Context,
    descriptor_id: u64,
) -> WasmtimeResult<Vec<(String, u32)>> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(descriptor.descriptor_type, DescriptorType::Directory) {
        return Err(WasmtimeError::Wasi {
            message: "Descriptor is not a directory".to_string(),
        });
    }

    // Get directory path
    let dir_path = descriptor.path.as_ref().ok_or_else(|| {
        WasmtimeError::Wasi {
            message: "Directory path not available".to_string(),
        }
    })?;

    // Read directory entries using std::fs
    let mut entries = Vec::new();
    match std::fs::read_dir(dir_path) {
        Ok(read_dir) => {
            for entry_result in read_dir {
                match entry_result {
                    Ok(entry) => {
                        if let Ok(file_name) = entry.file_name().into_string() {
                            // Determine entry type: 0 = unknown, 1 = file, 2 = directory
                            let entry_type = if let Ok(metadata) = entry.metadata() {
                                if metadata.is_dir() {
                                    2
                                } else if metadata.is_file() {
                                    1
                                } else {
                                    0
                                }
                            } else {
                                0
                            };
                            entries.push((file_name, entry_type));
                        }
                    }
                    Err(e) => {
                        // Log error but continue reading other entries
                        eprintln!("Error reading directory entry: {}", e);
                    }
                }
            }
            Ok(entries)
        }
        Err(e) => {
            Err(WasmtimeError::Wasi {
                message: format!("Failed to read directory: {}", e),
            })
        }
    }
}

/// Read symbolic link target
pub fn read_link_at(
    context: &WasiPreview2Context,
    descriptor_id: u64,
    path: &str,
) -> WasmtimeResult<String> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(descriptor.descriptor_type, DescriptorType::Directory) {
        return Err(WasmtimeError::Wasi {
            message: "Descriptor is not a directory".to_string(),
        });
    }

    // For MVP, return empty path
    Ok(String::new())
}

/// Unlink (delete) file at path
pub fn unlink_file_at(
    context: &WasiPreview2Context,
    descriptor_id: u64,
    path: &str,
) -> WasmtimeResult<()> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(descriptor.descriptor_type, DescriptorType::Directory) {
        return Err(WasmtimeError::Wasi {
            message: "Descriptor is not a directory".to_string(),
        });
    }

    // For MVP, this is a no-op
    Ok(())
}

/// Remove directory at path
pub fn remove_directory_at(
    context: &WasiPreview2Context,
    descriptor_id: u64,
    path: &str,
) -> WasmtimeResult<()> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(descriptor.descriptor_type, DescriptorType::Directory) {
        return Err(WasmtimeError::Wasi {
            message: "Descriptor is not a directory".to_string(),
        });
    }

    // For MVP, this is a no-op
    Ok(())
}

/// Rename file or directory
pub fn rename_at(
    context: &WasiPreview2Context,
    old_descriptor_id: u64,
    old_path: &str,
    new_descriptor_id: u64,
    new_path: &str,
) -> WasmtimeResult<()> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());

    let old_descriptor = descriptors.get(&(old_descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Old descriptor {} not found", old_descriptor_id),
        }
    })?;

    if !matches!(old_descriptor.descriptor_type, DescriptorType::Directory) {
        return Err(WasmtimeError::Wasi {
            message: "Old descriptor is not a directory".to_string(),
        });
    }

    let new_descriptor = descriptors.get(&(new_descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("New descriptor {} not found", new_descriptor_id),
        }
    })?;

    if !matches!(new_descriptor.descriptor_type, DescriptorType::Directory) {
        return Err(WasmtimeError::Wasi {
            message: "New descriptor is not a directory".to_string(),
        });
    }

    // For MVP, this is a no-op
    Ok(())
}

/// Create symbolic link
pub fn symlink_at(
    context: &WasiPreview2Context,
    descriptor_id: u64,
    old_path: &str,
    new_path: &str,
) -> WasmtimeResult<()> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());
    let descriptor = descriptors.get(&(descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id),
        }
    })?;

    if !matches!(descriptor.descriptor_type, DescriptorType::Directory) {
        return Err(WasmtimeError::Wasi {
            message: "Descriptor is not a directory".to_string(),
        });
    }

    // For MVP, this is a no-op
    Ok(())
}

/// Create hard link
pub fn link_at(
    context: &WasiPreview2Context,
    old_descriptor_id: u64,
    old_path: &str,
    new_descriptor_id: u64,
    new_path: &str,
) -> WasmtimeResult<()> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());

    let old_descriptor = descriptors.get(&(old_descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Old descriptor {} not found", old_descriptor_id),
        }
    })?;

    if !matches!(old_descriptor.descriptor_type, DescriptorType::Directory) {
        return Err(WasmtimeError::Wasi {
            message: "Old descriptor is not a directory".to_string(),
        });
    }

    let new_descriptor = descriptors.get(&(new_descriptor_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("New descriptor {} not found", new_descriptor_id),
        }
    })?;

    if !matches!(new_descriptor.descriptor_type, DescriptorType::Directory) {
        return Err(WasmtimeError::Wasi {
            message: "New descriptor is not a directory".to_string(),
        });
    }

    // For MVP, this is a no-op
    Ok(())
}

/// Check if two descriptors refer to the same object
pub fn is_same_object(
    context: &WasiPreview2Context,
    descriptor_id_1: u64,
    descriptor_id_2: u64,
) -> WasmtimeResult<bool> {
    let descriptors = context.descriptors.read().unwrap_or_else(|e| e.into_inner());

    let descriptor1 = descriptors.get(&(descriptor_id_1 as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id_1),
        }
    })?;

    let descriptor2 = descriptors.get(&(descriptor_id_2 as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Descriptor {} not found", descriptor_id_2),
        }
    })?;

    // For MVP, check if paths are equal
    Ok(descriptor1.path == descriptor2.path)
}

/// Close descriptor
pub fn close_descriptor(
    context: &WasiPreview2Context,
    descriptor_id: u64,
) -> WasmtimeResult<()> {
    let mut descriptors = context.descriptors.write().unwrap_or_else(|e| e.into_inner());
    if let Some(descriptor) = descriptors.get_mut(&(descriptor_id as u32)) {
        descriptor.status = DescriptorStatus::Closed;
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::wasi_preview2::WasiPreview2Config;
    use wasmtime::Engine;

    // Use shared async engine to reduce wasmtime GLOBAL_CODE registry accumulation
    fn test_context() -> WasiPreview2Context {
        let engine = crate::engine::get_shared_async_wasmtime_engine();
        WasiPreview2Context::new(engine, WasiPreview2Config::default()).unwrap()
    }

    fn insert_descriptor(
        ctx: &WasiPreview2Context,
        id: u32,
        dtype: DescriptorType,
        path: Option<String>,
        flags: u32,
        metadata: Option<DescriptorMetadata>,
        status: DescriptorStatus,
    ) {
        let descriptor = WasiDescriptor {
            id,
            descriptor_type: dtype,
            path,
            flags,
            metadata,
            status,
        };
        ctx.descriptors.write().unwrap_or_else(|e| e.into_inner()).insert(id, descriptor);
    }

    #[test]
    fn get_type_returns_file_type_code() {
        let ctx = test_context();
        insert_descriptor(
            &ctx, 10, DescriptorType::File,
            Some("/tmp/test.txt".to_string()), 0, None, DescriptorStatus::Open,
        );
        let result = get_type(&ctx, 10);
        assert!(result.is_ok(), "get_type should succeed, got: {:?}", result.err());
        let type_code = result.unwrap();
        println!("File descriptor type code: {}", type_code);
        assert_eq!(type_code, 1, "DescriptorType::File should map to 1, got: {}", type_code);
    }

    #[test]
    fn get_type_returns_directory_type_code() {
        let ctx = test_context();
        insert_descriptor(
            &ctx, 20, DescriptorType::Directory,
            Some("/tmp".to_string()), 0, None, DescriptorStatus::Open,
        );
        let result = get_type(&ctx, 20);
        assert!(result.is_ok(), "get_type should succeed, got: {:?}", result.err());
        let type_code = result.unwrap();
        println!("Directory descriptor type code: {}", type_code);
        assert_eq!(type_code, 2, "DescriptorType::Directory should map to 2, got: {}", type_code);
    }

    #[test]
    fn get_type_nonexistent_descriptor_fails() {
        let ctx = test_context();
        let result = get_type(&ctx, 99999);
        assert!(result.is_err(), "get_type on nonexistent descriptor should fail");
        let err_msg = format!("{:?}", result.unwrap_err());
        println!("Expected error for nonexistent descriptor: {}", err_msg);
        assert!(
            err_msg.contains("not found"),
            "Error should mention 'not found', got: {}",
            err_msg,
        );
    }

    #[test]
    fn get_flags_returns_descriptor_flags() {
        let ctx = test_context();
        let expected_flags: u32 = 0b1010_0101;
        insert_descriptor(
            &ctx, 30, DescriptorType::File,
            Some("/tmp/flagged.txt".to_string()), expected_flags, None, DescriptorStatus::Open,
        );
        let result = get_flags(&ctx, 30);
        assert!(result.is_ok(), "get_flags should succeed, got: {:?}", result.err());
        let flags = result.unwrap();
        println!("Descriptor flags: 0b{:08b}", flags);
        assert_eq!(flags, expected_flags, "Flags should match inserted value");
    }

    #[test]
    fn get_flags_nonexistent_descriptor_fails() {
        let ctx = test_context();
        let result = get_flags(&ctx, 99999);
        assert!(result.is_err(), "get_flags on nonexistent descriptor should fail");
        let err_msg = format!("{:?}", result.unwrap_err());
        println!("Expected error for nonexistent descriptor: {}", err_msg);
        assert!(
            err_msg.contains("not found"),
            "Error should mention 'not found', got: {}",
            err_msg,
        );
    }

    #[test]
    fn set_size_updates_metadata() {
        let ctx = test_context();
        let metadata = DescriptorMetadata { size: 100, modified: 0, accessed: 0, created: 0 };
        insert_descriptor(
            &ctx, 40, DescriptorType::File,
            Some("/tmp/sized.txt".to_string()), 0, Some(metadata), DescriptorStatus::Open,
        );
        let result = set_size(&ctx, 40, 512);
        assert!(result.is_ok(), "set_size should succeed, got: {:?}", result.err());
        // Verify metadata was updated
        let descriptors = ctx.descriptors.read().unwrap_or_else(|e| e.into_inner());
        let desc = descriptors.get(&40).unwrap();
        let meta = desc.metadata.as_ref().unwrap();
        println!("Descriptor size after set_size: {}", meta.size);
        assert_eq!(meta.size, 512, "Metadata size should be updated to 512, got: {}", meta.size);
    }

    #[test]
    fn set_size_without_metadata_is_noop() {
        // set_size on a descriptor without metadata silently succeeds (no metadata to update)
        let ctx = test_context();
        insert_descriptor(
            &ctx, 41, DescriptorType::Directory,
            Some("/tmp/dir".to_string()), 0, None, DescriptorStatus::Open,
        );
        let result = set_size(&ctx, 41, 999);
        assert!(
            result.is_ok(),
            "set_size on descriptor without metadata should succeed (no-op), got: {:?}",
            result.err(),
        );
        // Verify metadata is still None
        let descriptors = ctx.descriptors.read().unwrap_or_else(|e| e.into_inner());
        let desc = descriptors.get(&41).unwrap();
        println!("Descriptor metadata after set_size on None: {:?}", desc.metadata.is_none());
        assert!(desc.metadata.is_none(), "Metadata should remain None");
    }

    #[test]
    fn set_size_nonexistent_fails() {
        let ctx = test_context();
        let result = set_size(&ctx, 99999, 100);
        assert!(result.is_err(), "set_size on nonexistent descriptor should fail");
        let err_msg = format!("{:?}", result.unwrap_err());
        println!("Expected error for nonexistent descriptor: {}", err_msg);
        assert!(
            err_msg.contains("not found"),
            "Error should mention 'not found', got: {}",
            err_msg,
        );
    }

    #[test]
    fn open_at_requires_directory_parent() {
        let ctx = test_context();
        insert_descriptor(
            &ctx, 50, DescriptorType::File,
            Some("/tmp/not_a_dir.txt".to_string()), 0, None, DescriptorStatus::Open,
        );
        let result = open_at(&ctx, 50, "child.txt", 0, 0);
        assert!(result.is_err(), "open_at with non-directory parent should fail");
        let err_msg = format!("{:?}", result.unwrap_err());
        println!("Expected error for non-directory parent: {}", err_msg);
        assert!(
            err_msg.contains("not a directory"),
            "Error should mention 'not a directory', got: {}",
            err_msg,
        );
    }

    #[test]
    fn open_at_with_directory_parent_succeeds() {
        let ctx = test_context();
        insert_descriptor(
            &ctx, 51, DescriptorType::Directory,
            Some("/tmp".to_string()), 0, None, DescriptorStatus::Open,
        );
        let result = open_at(&ctx, 51, "newfile.txt", 0, 0);
        assert!(result.is_ok(), "open_at with directory parent should succeed, got: {:?}", result.err());
        let new_id = result.unwrap();
        println!("Opened new descriptor with id: {}", new_id);
        // Verify the new descriptor was created
        let descriptors = ctx.descriptors.read().unwrap_or_else(|e| e.into_inner());
        let new_desc = descriptors.get(&(new_id as u32));
        assert!(new_desc.is_some(), "New descriptor {} should exist", new_id);
        let new_desc = new_desc.unwrap();
        assert!(
            matches!(new_desc.descriptor_type, DescriptorType::File),
            "Opened descriptor should be File type, got: {:?}",
            new_desc.descriptor_type,
        );
        assert_eq!(
            new_desc.path.as_deref(),
            Some("newfile.txt"),
            "Opened descriptor path should match requested path",
        );
    }

    #[test]
    fn create_directory_at_requires_directory_parent() {
        let ctx = test_context();
        insert_descriptor(
            &ctx, 60, DescriptorType::File,
            Some("/tmp/file.txt".to_string()), 0, None, DescriptorStatus::Open,
        );
        let result = create_directory_at(&ctx, 60, "subdir");
        assert!(result.is_err(), "create_directory_at with non-directory parent should fail");
        let err_msg = format!("{:?}", result.unwrap_err());
        println!("Expected error for non-directory parent: {}", err_msg);
        assert!(
            err_msg.contains("not a directory"),
            "Error should mention 'not a directory', got: {}",
            err_msg,
        );
    }

    #[test]
    fn is_same_object_same_path_returns_true() {
        let ctx = test_context();
        let shared_path = "/tmp/shared.txt".to_string();
        insert_descriptor(
            &ctx, 70, DescriptorType::File,
            Some(shared_path.clone()), 0, None, DescriptorStatus::Open,
        );
        insert_descriptor(
            &ctx, 71, DescriptorType::File,
            Some(shared_path), 0, None, DescriptorStatus::Open,
        );
        let result = is_same_object(&ctx, 70, 71);
        assert!(result.is_ok(), "is_same_object should succeed, got: {:?}", result.err());
        let same = result.unwrap();
        println!("is_same_object with identical paths: {}", same);
        assert!(same, "Descriptors with same path should be the same object");
    }

    #[test]
    fn is_same_object_different_paths_returns_false() {
        let ctx = test_context();
        insert_descriptor(
            &ctx, 80, DescriptorType::File,
            Some("/tmp/a.txt".to_string()), 0, None, DescriptorStatus::Open,
        );
        insert_descriptor(
            &ctx, 81, DescriptorType::File,
            Some("/tmp/b.txt".to_string()), 0, None, DescriptorStatus::Open,
        );
        let result = is_same_object(&ctx, 80, 81);
        assert!(result.is_ok(), "is_same_object should succeed, got: {:?}", result.err());
        let same = result.unwrap();
        println!("is_same_object with different paths: {}", same);
        assert!(!same, "Descriptors with different paths should not be the same object");
    }

    #[test]
    fn is_same_object_nonexistent_fails() {
        let ctx = test_context();
        insert_descriptor(
            &ctx, 90, DescriptorType::File,
            Some("/tmp/exists.txt".to_string()), 0, None, DescriptorStatus::Open,
        );
        let result = is_same_object(&ctx, 90, 99999);
        assert!(result.is_err(), "is_same_object with nonexistent descriptor should fail");
        let err_msg = format!("{:?}", result.unwrap_err());
        println!("Expected error for nonexistent descriptor: {}", err_msg);
        assert!(
            err_msg.contains("not found"),
            "Error should mention 'not found', got: {}",
            err_msg,
        );
    }

    #[test]
    fn close_descriptor_marks_as_closed() {
        let ctx = test_context();
        insert_descriptor(
            &ctx, 100, DescriptorType::File,
            Some("/tmp/closeme.txt".to_string()), 0, None, DescriptorStatus::Open,
        );
        let result = close_descriptor(&ctx, 100);
        assert!(result.is_ok(), "close_descriptor should succeed, got: {:?}", result.err());
        // Verify status is now Closed
        let descriptors = ctx.descriptors.read().unwrap_or_else(|e| e.into_inner());
        let desc = descriptors.get(&100).unwrap();
        println!("Descriptor status after close: {:?}", desc.status);
        assert!(
            matches!(desc.status, DescriptorStatus::Closed),
            "Descriptor should be Closed after close_descriptor, got: {:?}",
            desc.status,
        );
    }

    #[test]
    fn close_descriptor_nonexistent_is_ok() {
        let ctx = test_context();
        let result = close_descriptor(&ctx, 99999);
        println!("close_descriptor on nonexistent id result: {:?}", result);
        assert!(
            result.is_ok(),
            "close_descriptor on nonexistent id should silently succeed, got: {:?}",
            result.err(),
        );
    }

    #[test]
    fn read_via_stream_creates_input_stream() {
        let ctx = test_context();
        insert_descriptor(
            &ctx, 110, DescriptorType::File,
            Some("/tmp/readable.txt".to_string()), 0, None, DescriptorStatus::Open,
        );
        let result = read_via_stream(&ctx, 110, 0);
        assert!(result.is_ok(), "read_via_stream should succeed, got: {:?}", result.err());
        let stream_id = result.unwrap();
        println!("Created input stream with id: {}", stream_id);
        // Verify the stream was registered in the context
        let streams = ctx.streams.read().unwrap_or_else(|e| e.into_inner());
        let stream = streams.get(&(stream_id as u32));
        assert!(
            stream.is_some(),
            "Stream {} should exist in context after read_via_stream",
            stream_id,
        );
    }

    #[test]
    fn write_via_stream_succeeds_for_open_descriptor() {
        // write_via_stream only checks that the descriptor exists and is Open;
        // it does not check descriptor type
        let ctx = test_context();
        insert_descriptor(
            &ctx, 120, DescriptorType::Directory,
            Some("/tmp/dir".to_string()), 0, None, DescriptorStatus::Open,
        );
        let result = write_via_stream(&ctx, 120, 0);
        println!("write_via_stream on directory descriptor result: {:?}", result);
        assert!(
            result.is_ok(),
            "write_via_stream should succeed for any open descriptor, got: {:?}",
            result.err(),
        );
        let stream_id = result.unwrap();
        println!("Write stream id: {}", stream_id);
        assert!(stream_id > 0, "Stream id should be positive, got: {}", stream_id);
    }
}

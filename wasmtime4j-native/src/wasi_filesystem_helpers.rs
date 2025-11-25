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
    let descriptors = context.descriptors.read().unwrap();
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
    let descriptors = context.descriptors.read().unwrap();
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
    let descriptors = context.descriptors.read().unwrap();
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
    let descriptors = context.descriptors.read().unwrap();
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
    let descriptors = context.descriptors.read().unwrap();
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
    let mut descriptors = context.descriptors.write().unwrap();
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
    let descriptors = context.descriptors.read().unwrap();
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
    let descriptors = context.descriptors.read().unwrap();
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
    let descriptors = context.descriptors.read().unwrap();
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

    let mut descriptors = context.descriptors.write().unwrap();
    descriptors.insert(new_id, new_descriptor);

    Ok(new_id as u64)
}

/// Create directory at path
pub fn create_directory_at(
    context: &WasiPreview2Context,
    descriptor_id: u64,
    path: &str,
) -> WasmtimeResult<()> {
    let descriptors = context.descriptors.read().unwrap();
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
    let descriptors = context.descriptors.read().unwrap();
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

    // For MVP, return empty list
    Ok(Vec::new())
}

/// Read symbolic link target
pub fn read_link_at(
    context: &WasiPreview2Context,
    descriptor_id: u64,
    path: &str,
) -> WasmtimeResult<String> {
    let descriptors = context.descriptors.read().unwrap();
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
    let descriptors = context.descriptors.read().unwrap();
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
    let descriptors = context.descriptors.read().unwrap();
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
    let descriptors = context.descriptors.read().unwrap();

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
    let descriptors = context.descriptors.read().unwrap();
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
    let descriptors = context.descriptors.read().unwrap();

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
    let descriptors = context.descriptors.read().unwrap();

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
    let mut descriptors = context.descriptors.write().unwrap();
    if let Some(descriptor) = descriptors.get_mut(&(descriptor_id as u32)) {
        descriptor.status = DescriptorStatus::Closed;
    }
    Ok(())
}

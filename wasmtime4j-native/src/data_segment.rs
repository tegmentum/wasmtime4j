//! Data segment management for memory.init() support
//!
//! This module provides functionality to parse and cache WebAssembly data segments
//! from module bytecode, enabling memory.init() operations. Uses a hybrid design where
//! only passive data segments are cached (active segments are applied during instantiation).

use std::sync::{Arc, Mutex};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Type of data segment
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DataMode {
    /// Passive segment - can be used with memory.init()
    Passive,
    /// Active segment - automatically applied during instantiation
    Active { memory_index: u32, offset: i32 },
}

/// Cached data segment data
#[derive(Debug, Clone)]
pub struct DataSegment {
    /// Segment mode (passive or active)
    pub mode: DataMode,
    /// Raw data bytes in this segment
    pub data: Vec<u8>,
}

impl DataSegment {
    /// Create a new passive data segment
    pub fn new_passive(data: Vec<u8>) -> Self {
        Self {
            mode: DataMode::Passive,
            data,
        }
    }

    /// Create a new active data segment
    pub fn new_active(data: Vec<u8>, memory_index: u32, offset: i32) -> Self {
        Self {
            mode: DataMode::Active { memory_index, offset },
            data,
        }
    }

    /// Check if this is a passive segment
    pub fn is_passive(&self) -> bool {
        matches!(self.mode, DataMode::Passive)
    }

    /// Get the number of bytes
    pub fn len(&self) -> usize {
        self.data.len()
    }

    /// Check if segment is empty
    pub fn is_empty(&self) -> bool {
        self.data.is_empty()
    }

    /// Get data slice at specified range
    pub fn get_slice(&self, offset: usize, len: usize) -> Option<&[u8]> {
        if offset.saturating_add(len) > self.data.len() {
            return None;
        }
        Some(&self.data[offset..offset + len])
    }
}

/// Manager for data segments in an instance
#[derive(Debug)]
pub struct DataSegmentManager {
    /// Cached passive data segments (hybrid design - only passive segments cached)
    segments: Vec<Option<DataSegment>>,
    /// Track which segments have been dropped
    dropped: Arc<Mutex<Vec<bool>>>,
}

impl DataSegmentManager {
    /// Create a new data segment manager
    pub fn new(segments: Vec<Option<DataSegment>>) -> Self {
        let count = segments.len();
        Self {
            segments,
            dropped: Arc::new(Mutex::new(vec![false; count])),
        }
    }

    /// Get data slice from segment
    pub fn get_data(&self, segment_index: u32, offset: u32, len: u32) -> WasmtimeResult<Vec<u8>> {
        // Check if segment exists
        let segment = self.segments
            .get(segment_index as usize)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Data segment index {} out of bounds", segment_index),
            })?;

        // Check if segment is available (Some = passive segment cached, None = active not cached)
        let segment = segment.as_ref().ok_or_else(|| WasmtimeError::Runtime {
            message: format!(
                "Data segment {} is not available (may be active segment)",
                segment_index
            ),
            backtrace: None,
        })?;

        // Check if segment has been dropped
        let dropped_vec = self.dropped.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock dropped segments: {}", e),
        })?;

        if *dropped_vec.get(segment_index as usize).unwrap_or(&false) {
            return Err(WasmtimeError::Runtime {
                message: format!("Data segment {} has been dropped", segment_index),
                backtrace: None,
            });
        }

        // Get data slice
        let data_slice = segment.get_slice(offset as usize, len as usize)
            .ok_or_else(|| WasmtimeError::Runtime {
                message: format!(
                    "Data offset {} + length {} exceeds segment {} size ({})",
                    offset,
                    len,
                    segment_index,
                    segment.len()
                ),
                backtrace: None,
            })?;

        Ok(data_slice.to_vec())
    }

    /// Drop a data segment
    pub fn drop_segment(&self, segment_index: u32) -> WasmtimeResult<()> {
        let mut dropped_vec = self.dropped.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock dropped segments: {}", e),
        })?;

        if segment_index >= dropped_vec.len() as u32 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Data segment index {} out of bounds", segment_index),
            });
        }

        // Mark as dropped
        dropped_vec[segment_index as usize] = true;
        Ok(())
    }

    /// Check if a segment is dropped
    pub fn is_dropped(&self, segment_index: u32) -> WasmtimeResult<bool> {
        let dropped_vec = self.dropped.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock dropped segments: {}", e),
        })?;

        Ok(*dropped_vec.get(segment_index as usize).unwrap_or(&false))
    }

    /// Get the total number of segments
    pub fn segment_count(&self) -> usize {
        self.segments.len()
    }

    /// Get segment info
    pub fn get_segment(&self, segment_index: u32) -> Option<&Option<DataSegment>> {
        self.segments.get(segment_index as usize)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_data_segment_creation() {
        let data = vec![1, 2, 3, 4, 5];
        let segment = DataSegment::new_passive(data.clone());

        assert_eq!(segment.len(), 5);
        assert!(segment.is_passive());
        assert!(!segment.is_empty());
        assert_eq!(segment.get_slice(0, 3), Some(&[1, 2, 3][..]));
    }

    #[test]
    fn test_data_segment_manager() {
        let segment1 = DataSegment::new_passive(vec![10, 20, 30, 40]);
        let segments = vec![Some(segment1), None]; // One passive, one active (not cached)
        let manager = DataSegmentManager::new(segments);

        assert_eq!(manager.segment_count(), 2);

        // Can get data from passive segment
        let data = manager.get_data(0, 1, 2).unwrap();
        assert_eq!(data, vec![20, 30]);

        // Cannot get data from active segment (not cached)
        assert!(manager.get_data(1, 0, 1).is_err());

        // Drop segment
        manager.drop_segment(0).unwrap();
        assert!(manager.is_dropped(0).unwrap());

        // Cannot get data from dropped segment
        assert!(manager.get_data(0, 0, 1).is_err());
    }

    #[test]
    fn test_data_segment_bounds() {
        let segment = DataSegment::new_passive(vec![1, 2, 3]);

        // Valid access
        assert_eq!(segment.get_slice(0, 3), Some(&[1, 2, 3][..]));
        assert_eq!(segment.get_slice(1, 2), Some(&[2, 3][..]));

        // Out of bounds
        assert_eq!(segment.get_slice(0, 4), None);
        assert_eq!(segment.get_slice(3, 1), None);
    }
}

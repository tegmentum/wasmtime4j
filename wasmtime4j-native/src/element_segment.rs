//! Element segment management for table.init() support
//!
//! This module provides functionality to parse and cache WebAssembly element segments
//! from module bytecode, enabling table.init() operations. Uses a hybrid design where
//! only passive element segments are cached (active segments are applied during instantiation).

use wasmtime::Val;
use std::sync::{Arc, Mutex};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Type of element segment
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ElementMode {
    /// Passive segment - can be used with table.init()
    Passive,
    /// Active segment - automatically applied during instantiation
    Active { table_index: u32, offset: i32 },
    /// Declarative segment - used for validation only
    Declarative,
}

/// Element item in a segment
#[derive(Debug, Clone)]
pub enum ElementItem {
    /// Null function reference
    NullFunc,
    /// Function reference by index
    FuncIndex(u32),
    /// Expression-based element (for externref, etc.)
    Expr(Vec<u8>), // Stores the expression bytecode
}

/// Cached element segment data
#[derive(Debug, Clone)]
pub struct ElementSegment {
    /// Segment mode (passive, active, declarative)
    pub mode: ElementMode,
    /// Element type (funcref, externref, etc.)
    pub elem_type: wasmtime::ValType,
    /// Elements in this segment
    pub items: Vec<ElementItem>,
}

impl ElementSegment {
    /// Create a new passive element segment
    pub fn new_passive(elem_type: wasmtime::ValType, items: Vec<ElementItem>) -> Self {
        Self {
            mode: ElementMode::Passive,
            elem_type,
            items,
        }
    }

    /// Create a new active element segment
    pub fn new_active(
        elem_type: wasmtime::ValType,
        items: Vec<ElementItem>,
        table_index: u32,
        offset: i32,
    ) -> Self {
        Self {
            mode: ElementMode::Active { table_index, offset },
            elem_type,
            items,
        }
    }

    /// Check if this is a passive segment
    pub fn is_passive(&self) -> bool {
        matches!(self.mode, ElementMode::Passive)
    }

    /// Get the number of elements
    pub fn len(&self) -> usize {
        self.items.len()
    }

    /// Check if segment is empty
    pub fn is_empty(&self) -> bool {
        self.items.is_empty()
    }

    /// Get element at index
    pub fn get(&self, index: usize) -> Option<&ElementItem> {
        self.items.get(index)
    }
}

/// Manager for element segments in an instance
#[derive(Debug)]
pub struct ElementSegmentManager {
    /// Cached passive element segments (hybrid design - only passive segments cached)
    segments: Vec<Option<ElementSegment>>,
    /// Track which segments have been dropped
    dropped: Arc<Mutex<Vec<bool>>>,
}

impl ElementSegmentManager {
    /// Create a new element segment manager
    pub fn new(segments: Vec<Option<ElementSegment>>) -> Self {
        let count = segments.len();
        Self {
            segments,
            dropped: Arc::new(Mutex::new(vec![false; count])),
        }
    }

    /// Get element at specified position in segment
    pub fn get_element(&self, segment_index: u32, offset: u32) -> WasmtimeResult<ElementItem> {
        // Check if segment exists
        let segment = self.segments
            .get(segment_index as usize)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Element segment index {} out of bounds", segment_index),
            })?;

        // Check if segment is available (Some = passive segment cached, None = active/declarative not cached)
        let segment = segment.as_ref().ok_or_else(|| WasmtimeError::Runtime {
            message: format!(
                "Element segment {} is not available (may be active or declarative)",
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
                message: format!("Element segment {} has been dropped", segment_index),
                backtrace: None,
            });
        }

        // Get element at offset
        segment.get(offset as usize)
            .cloned()
            .ok_or_else(|| WasmtimeError::Runtime {
                message: format!(
                    "Element offset {} out of bounds for segment {} (size: {})",
                    offset,
                    segment_index,
                    segment.len()
                ),
                backtrace: None,
            })
    }

    /// Drop an element segment
    pub fn drop_segment(&self, segment_index: u32) -> WasmtimeResult<()> {
        let mut dropped_vec = self.dropped.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock dropped segments: {}", e),
        })?;

        if segment_index >= dropped_vec.len() as u32 {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Element segment index {} out of bounds", segment_index),
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
    pub fn get_segment(&self, segment_index: u32) -> Option<&Option<ElementSegment>> {
        self.segments.get(segment_index as usize)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use wasmtime::ValType;

    #[test]
    fn test_element_segment_creation() {
        let items = vec![
            ElementItem::FuncIndex(0),
            ElementItem::FuncIndex(1),
            ElementItem::NullFunc,
        ];

        let segment = ElementSegment::new_passive(ValType::FUNCREF, items);

        assert_eq!(segment.len(), 3);
        assert!(segment.is_passive());
        assert!(!segment.is_empty());
    }

    #[test]
    fn test_element_segment_manager() {
        let segment1 = ElementSegment::new_passive(
            ValType::FUNCREF,
            vec![ElementItem::FuncIndex(5), ElementItem::FuncIndex(10)],
        );

        let segments = vec![Some(segment1), None]; // One passive, one active (not cached)
        let manager = ElementSegmentManager::new(segments);

        assert_eq!(manager.segment_count(), 2);

        // Can get element from passive segment
        let elem = manager.get_element(0, 1).unwrap();
        assert!(matches!(elem, ElementItem::FuncIndex(10)));

        // Cannot get element from active segment (not cached)
        assert!(manager.get_element(1, 0).is_err());

        // Drop segment
        manager.drop_segment(0).unwrap();
        assert!(manager.is_dropped(0).unwrap());

        // Cannot get element from dropped segment
        assert!(manager.get_element(0, 0).is_err());
    }
}

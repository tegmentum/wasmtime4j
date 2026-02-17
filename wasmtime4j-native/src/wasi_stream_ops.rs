//! Unified WASI stream operations trait
//!
//! This module provides a trait-based abstraction for WASI stream operations
//! that works with both WasiContext (Preview 1) and WasiPreview2Context.
//!
//! This consolidates the duplicated stream operations from:
//! - wasi_io_helpers.rs (Preview 2)
//! - panama_wasi_io_ffi.rs (Panama FFI for Preview 1)
//! - jni_wasi_io_bindings.rs (JNI for Preview 1)
//!
//! ## `blocking` parameter convention
//!
//! Several functions accept a `blocking` parameter that is currently unused.
//! All stream entries are in-memory buffers where reads/writes complete
//! immediately, so blocking vs non-blocking has no observable effect. The
//! parameter is preserved in the signature for forward compatibility with
//! future real I/O stream backends (e.g., host file descriptors, sockets)
//! where blocking semantics would matter.

use crate::error::{WasmtimeError, WasmtimeResult};
use std::collections::HashMap;

/// Trait for stream entry operations
///
/// Both WasiStreamInfo (Preview 1) and WasiStream (Preview 2) implement this trait.
pub trait WasiStreamEntry {
    /// Check if the stream is closed
    fn is_closed(&self) -> bool;

    /// Mark the stream as closed
    fn set_closed(&mut self);

    /// Get the stream buffer
    fn buffer(&self) -> &Vec<u8>;

    /// Get mutable reference to the stream buffer
    fn buffer_mut(&mut self) -> &mut Vec<u8>;

    /// Clear the stream buffer
    fn clear_buffer(&mut self);
}

/// Trait for WASI context stream operations
///
/// Both WasiContext and WasiPreview2Context implement this trait.
pub trait WasiStreamContext {
    /// The stream entry type
    type StreamEntry: WasiStreamEntry;

    /// Get read access to the streams map
    fn streams_read(
        &self,
    ) -> WasmtimeResult<std::sync::RwLockReadGuard<'_, HashMap<u32, Self::StreamEntry>>>;

    /// Get write access to the streams map
    fn streams_write(
        &self,
    ) -> WasmtimeResult<std::sync::RwLockWriteGuard<'_, HashMap<u32, Self::StreamEntry>>>;
}

// ============================================================================
// Generic stream operations that work with any WasiStreamContext
// ============================================================================

/// Read data from a stream (generic version)
pub fn read_from_stream_generic<C: WasiStreamContext>(
    context: &C,
    stream_id: u64,
    length: usize,
    blocking: bool,
) -> WasmtimeResult<Vec<u8>> {
    // See module-level docs for why blocking is unused on in-memory buffers.
    let _ = blocking;
    let mut streams = context.streams_write()?;
    let stream =
        streams
            .get_mut(&(stream_id as u32))
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Stream {} not found", stream_id),
            })?;

    if stream.is_closed() {
        return Err(WasmtimeError::Wasi {
            message: "Stream is closed".to_string(),
        });
    }

    let read_len = length.min(stream.buffer().len());
    let data = stream.buffer_mut().drain(..read_len).collect();
    Ok(data)
}

/// Skip bytes in a stream (generic version)
pub fn skip_in_stream_generic<C: WasiStreamContext>(
    context: &C,
    stream_id: u64,
    length: u64,
    blocking: bool,
) -> WasmtimeResult<u64> {
    let _ = blocking;
    let mut streams = context.streams_write()?;
    let stream =
        streams
            .get_mut(&(stream_id as u32))
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Stream {} not found", stream_id),
            })?;

    if stream.is_closed() {
        return Err(WasmtimeError::Wasi {
            message: "Stream is closed".to_string(),
        });
    }

    let skip_len = (length as usize).min(stream.buffer().len());
    stream.buffer_mut().drain(..skip_len);
    Ok(skip_len as u64)
}

/// Close a stream (generic version)
pub fn close_stream_generic<C: WasiStreamContext>(
    context: &C,
    stream_id: u64,
) -> WasmtimeResult<()> {
    let mut streams = context.streams_write()?;
    if let Some(stream) = streams.get_mut(&(stream_id as u32)) {
        stream.set_closed();
        stream.clear_buffer();
    }
    Ok(())
}

/// Check write capacity for an output stream (generic version)
pub fn check_write_capacity_generic<C: WasiStreamContext>(
    context: &C,
    stream_id: u64,
) -> WasmtimeResult<u64> {
    let streams = context.streams_read()?;
    let stream =
        streams
            .get(&(stream_id as u32))
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Stream {} not found", stream_id),
            })?;

    if stream.is_closed() {
        return Err(WasmtimeError::Wasi {
            message: "Stream is closed".to_string(),
        });
    }

    Ok(65536)
}

/// Write data to a stream (generic version)
pub fn write_to_stream_generic<C: WasiStreamContext>(
    context: &C,
    stream_id: u64,
    data: &[u8],
    blocking: bool,
) -> WasmtimeResult<()> {
    let _ = blocking;
    let mut streams = context.streams_write()?;
    let stream =
        streams
            .get_mut(&(stream_id as u32))
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Stream {} not found", stream_id),
            })?;

    if stream.is_closed() {
        return Err(WasmtimeError::Wasi {
            message: "Stream is closed".to_string(),
        });
    }

    stream.buffer_mut().extend_from_slice(data);
    Ok(())
}

/// Flush a stream (generic version)
pub fn flush_stream_generic<C: WasiStreamContext>(
    context: &C,
    stream_id: u64,
    blocking: bool,
) -> WasmtimeResult<()> {
    let _ = blocking;
    let streams = context.streams_read()?;
    let stream =
        streams
            .get(&(stream_id as u32))
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Stream {} not found", stream_id),
            })?;

    if stream.is_closed() {
        return Err(WasmtimeError::Wasi {
            message: "Stream is closed".to_string(),
        });
    }

    // Flush is a no-op for in-memory streams
    Ok(())
}

/// Write zeroes to a stream (generic version)
pub fn write_zeroes_to_stream_generic<C: WasiStreamContext>(
    context: &C,
    stream_id: u64,
    length: u64,
    blocking: bool,
) -> WasmtimeResult<()> {
    let _ = blocking;
    let mut streams = context.streams_write()?;
    let stream =
        streams
            .get_mut(&(stream_id as u32))
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Stream {} not found", stream_id),
            })?;

    if stream.is_closed() {
        return Err(WasmtimeError::Wasi {
            message: "Stream is closed".to_string(),
        });
    }

    let current_len = stream.buffer().len();
    stream.buffer_mut().resize(current_len + length as usize, 0);
    Ok(())
}

/// Splice data between streams (generic version)
///
/// Note: This is a simplified version that works with the basic stream abstraction.
/// For splice, we need to read from source and write to dest in a single lock.
pub fn splice_streams_generic<C: WasiStreamContext>(
    context: &C,
    dest_stream_id: u64,
    source_stream_id: u64,
    length: u64,
    blocking: bool,
) -> WasmtimeResult<u64> {
    let _ = blocking;
    let mut streams = context.streams_write()?;

    // First check source stream
    let source_stream =
        streams
            .get(&(source_stream_id as u32))
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Source stream {} not found", source_stream_id),
            })?;

    if source_stream.is_closed() {
        return Err(WasmtimeError::Wasi {
            message: "Source stream is closed".to_string(),
        });
    }

    // Read data from source
    let read_len = (length as usize).min(source_stream.buffer().len());

    // Get source stream mutably and drain data
    let data: Vec<u8> = {
        let source = streams.get_mut(&(source_stream_id as u32)).unwrap();
        source.buffer_mut().drain(..read_len).collect()
    };

    // Check and write to dest stream
    let dest_stream = streams.get_mut(&(dest_stream_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Destination stream {} not found", dest_stream_id),
        }
    })?;

    if dest_stream.is_closed() {
        return Err(WasmtimeError::Wasi {
            message: "Destination stream is closed".to_string(),
        });
    }

    dest_stream.buffer_mut().extend_from_slice(&data);
    Ok(data.len() as u64)
}

/// Check if a stream exists (generic version)
pub fn stream_exists_generic<C: WasiStreamContext>(
    context: &C,
    stream_id: u64,
) -> WasmtimeResult<bool> {
    let streams = context.streams_read()?;
    Ok(streams.contains_key(&(stream_id as u32)))
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::RwLock;

    // Test stream entry implementation
    struct TestStream {
        buffer: Vec<u8>,
        closed: bool,
    }

    impl WasiStreamEntry for TestStream {
        fn is_closed(&self) -> bool {
            self.closed
        }

        fn set_closed(&mut self) {
            self.closed = true;
        }

        fn buffer(&self) -> &Vec<u8> {
            &self.buffer
        }

        fn buffer_mut(&mut self) -> &mut Vec<u8> {
            &mut self.buffer
        }

        fn clear_buffer(&mut self) {
            self.buffer.clear();
        }
    }

    // Test context implementation
    struct TestContext {
        streams: RwLock<HashMap<u32, TestStream>>,
    }

    impl WasiStreamContext for TestContext {
        type StreamEntry = TestStream;

        fn streams_read(
            &self,
        ) -> WasmtimeResult<std::sync::RwLockReadGuard<'_, HashMap<u32, Self::StreamEntry>>>
        {
            self.streams.read().map_err(|_| WasmtimeError::Wasi {
                message: "Failed to lock streams".to_string(),
            })
        }

        fn streams_write(
            &self,
        ) -> WasmtimeResult<std::sync::RwLockWriteGuard<'_, HashMap<u32, Self::StreamEntry>>>
        {
            self.streams.write().map_err(|_| WasmtimeError::Wasi {
                message: "Failed to lock streams".to_string(),
            })
        }
    }

    #[test]
    fn test_generic_read_from_stream() {
        let ctx = TestContext {
            streams: RwLock::new(HashMap::new()),
        };

        // Insert a test stream
        ctx.streams.write().unwrap().insert(
            1,
            TestStream {
                buffer: vec![1, 2, 3, 4, 5],
                closed: false,
            },
        );

        // Read from it
        let result = read_from_stream_generic(&ctx, 1, 3, false);
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), vec![1, 2, 3]);

        // Verify remaining data
        let streams = ctx.streams.read().unwrap();
        assert_eq!(streams.get(&1).unwrap().buffer, vec![4, 5]);
    }

    #[test]
    fn test_generic_close_stream() {
        let ctx = TestContext {
            streams: RwLock::new(HashMap::new()),
        };

        ctx.streams.write().unwrap().insert(
            1,
            TestStream {
                buffer: vec![1, 2, 3],
                closed: false,
            },
        );

        let result = close_stream_generic(&ctx, 1);
        assert!(result.is_ok());

        let streams = ctx.streams.read().unwrap();
        let stream = streams.get(&1).unwrap();
        assert!(stream.closed);
        assert!(stream.buffer.is_empty());
    }

    #[test]
    fn test_generic_read_from_closed_stream_fails() {
        let ctx = TestContext {
            streams: RwLock::new(HashMap::new()),
        };

        ctx.streams.write().unwrap().insert(
            1,
            TestStream {
                buffer: vec![1, 2, 3],
                closed: true,
            },
        );

        let result = read_from_stream_generic(&ctx, 1, 3, false);
        assert!(result.is_err());
        let err = format!("{:?}", result.unwrap_err());
        assert!(err.contains("closed"));
    }

    #[test]
    fn test_generic_write_to_stream() {
        let ctx = TestContext {
            streams: RwLock::new(HashMap::new()),
        };

        ctx.streams.write().unwrap().insert(
            1,
            TestStream {
                buffer: Vec::new(),
                closed: false,
            },
        );

        let result = write_to_stream_generic(&ctx, 1, &[1, 2, 3], false);
        assert!(result.is_ok());

        let streams = ctx.streams.read().unwrap();
        assert_eq!(streams.get(&1).unwrap().buffer, vec![1, 2, 3]);
    }
}

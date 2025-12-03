//! Shared helper functions for WASI I/O stream operations
//!
//! This module provides common functionality used by both JNI and Panama FFI bindings
//! for WASI Preview 2 I/O stream operations.

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::wasi_preview2::{
    PollableType, WasiPollable, WasiPreview2Context, WasiStream, WasiStreamStatus, WasiStreamType,
};
use std::thread;
use std::time::Duration;

/// Create a new input stream
pub fn create_input_stream(
    context: &WasiPreview2Context,
    _descriptor_id: u64,
    _offset: u64,
) -> WasmtimeResult<u64> {
    let stream_id = context.next_operation_id.fetch_add(1, std::sync::atomic::Ordering::SeqCst) as u32;

    let stream = WasiStream {
        id: stream_id,
        stream_type: WasiStreamType::InputStream,
        buffer: Vec::new(),
        status: WasiStreamStatus::Ready,
        resource_id: Some(_descriptor_id),
    };

    let mut streams = context.streams.write().unwrap();
    streams.insert(stream_id, stream);

    Ok(stream_id as u64)
}

/// Read data from a stream
pub fn read_from_stream(
    context: &WasiPreview2Context,
    stream_id: u64,
    length: usize,
    _blocking: bool,
) -> WasmtimeResult<Vec<u8>> {
    let mut streams = context.streams.write().unwrap();
    let stream = streams.get_mut(&(stream_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Stream {} not found", stream_id),
        }
    })?;

    if matches!(stream.status, WasiStreamStatus::Closed) {
        return Err(WasmtimeError::Wasi {
            message: "Stream is closed".to_string(),
        });
    }

    let read_len = length.min(stream.buffer.len());
    let data = stream.buffer.drain(..read_len).collect();
    Ok(data)
}

/// Skip bytes in a stream
pub fn skip_in_stream(
    context: &WasiPreview2Context,
    stream_id: u64,
    length: u64,
    _blocking: bool,
) -> WasmtimeResult<u64> {
    let mut streams = context.streams.write().unwrap();
    let stream = streams.get_mut(&(stream_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Stream {} not found", stream_id),
        }
    })?;

    if matches!(stream.status, WasiStreamStatus::Closed) {
        return Err(WasmtimeError::Wasi {
            message: "Stream is closed".to_string(),
        });
    }

    let skip_len = (length as usize).min(stream.buffer.len());
    stream.buffer.drain(..skip_len);
    Ok(skip_len as u64)
}

/// Close a stream
pub fn close_stream(context: &WasiPreview2Context, stream_id: u64) -> WasmtimeResult<()> {
    let mut streams = context.streams.write().unwrap();
    if let Some(stream) = streams.get_mut(&(stream_id as u32)) {
        stream.status = WasiStreamStatus::Closed;
        stream.buffer.clear();
    }
    Ok(())
}

/// Check write capacity for an output stream
pub fn check_write_capacity(context: &WasiPreview2Context, stream_id: u64) -> WasmtimeResult<u64> {
    let streams = context.streams.read().unwrap();
    let stream = streams.get(&(stream_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Stream {} not found", stream_id),
        }
    })?;

    if matches!(stream.status, WasiStreamStatus::Closed) {
        return Err(WasmtimeError::Wasi {
            message: "Stream is closed".to_string(),
        });
    }

    Ok(65536)
}

/// Write data to a stream
pub fn write_to_stream(
    context: &WasiPreview2Context,
    stream_id: u64,
    data: &[u8],
    _blocking: bool,
) -> WasmtimeResult<()> {
    let mut streams = context.streams.write().unwrap();
    let stream = streams.get_mut(&(stream_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Stream {} not found", stream_id),
        }
    })?;

    if matches!(stream.status, WasiStreamStatus::Closed) {
        return Err(WasmtimeError::Wasi {
            message: "Stream is closed".to_string(),
        });
    }

    stream.buffer.extend_from_slice(data);
    Ok(())
}

/// Flush a stream
pub fn flush_stream(
    context: &WasiPreview2Context,
    stream_id: u64,
    _blocking: bool,
) -> WasmtimeResult<()> {
    let streams = context.streams.read().unwrap();
    let stream = streams.get(&(stream_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Stream {} not found", stream_id),
        }
    })?;

    if matches!(stream.status, WasiStreamStatus::Closed) {
        return Err(WasmtimeError::Wasi {
            message: "Stream is closed".to_string(),
        });
    }

    Ok(())
}

/// Write zeroes to a stream
pub fn write_zeroes_to_stream(
    context: &WasiPreview2Context,
    stream_id: u64,
    length: u64,
    _blocking: bool,
) -> WasmtimeResult<()> {
    let mut streams = context.streams.write().unwrap();
    let stream = streams.get_mut(&(stream_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Stream {} not found", stream_id),
        }
    })?;

    if matches!(stream.status, WasiStreamStatus::Closed) {
        return Err(WasmtimeError::Wasi {
            message: "Stream is closed".to_string(),
        });
    }

    stream.buffer.resize(stream.buffer.len() + length as usize, 0);
    Ok(())
}

/// Splice data between streams
pub fn splice_streams(
    context: &WasiPreview2Context,
    dest_stream_id: u64,
    source_stream_id: u64,
    length: u64,
    _blocking: bool,
) -> WasmtimeResult<u64> {
    let mut streams = context.streams.write().unwrap();

    let source_stream = streams.get_mut(&(source_stream_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Source stream {} not found", source_stream_id),
        }
    })?;

    if matches!(source_stream.status, WasiStreamStatus::Closed) {
        return Err(WasmtimeError::Wasi {
            message: "Source stream is closed".to_string(),
        });
    }

    let read_len = (length as usize).min(source_stream.buffer.len());
    let data: Vec<u8> = source_stream.buffer.drain(..read_len).collect();

    let dest_stream = streams.get_mut(&(dest_stream_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Destination stream {} not found", dest_stream_id),
        }
    })?;

    if matches!(dest_stream.status, WasiStreamStatus::Closed) {
        return Err(WasmtimeError::Wasi {
            message: "Destination stream is closed".to_string(),
        });
    }

    dest_stream.buffer.extend_from_slice(&data);
    Ok(data.len() as u64)
}

/// Create a pollable for an output stream
pub fn create_output_stream_pollable(context: &WasiPreview2Context, stream_id: u64) -> WasmtimeResult<u64> {
    // Verify the stream exists
    {
        let streams = context.streams.read().unwrap();
        if !streams.contains_key(&(stream_id as u32)) {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Stream {} not found", stream_id),
            });
        }
    }

    // Generate a unique pollable ID
    let pollable_id = context
        .next_operation_id
        .fetch_add(1, std::sync::atomic::Ordering::SeqCst) as u32;

    // Create the pollable
    let pollable = WasiPollable::new(pollable_id, stream_id);

    // Register the pollable
    let mut pollables = context.pollables.write().unwrap();
    pollables.insert(pollable_id, pollable);

    Ok(pollable_id as u64)
}

/// Create a pollable for an input stream
pub fn create_input_stream_pollable(context: &WasiPreview2Context, stream_id: u64) -> WasmtimeResult<u64> {
    // Verify the stream exists
    {
        let streams = context.streams.read().unwrap();
        if !streams.contains_key(&(stream_id as u32)) {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Stream {} not found", stream_id),
            });
        }
    }

    // Generate a unique pollable ID
    let pollable_id = context
        .next_operation_id
        .fetch_add(1, std::sync::atomic::Ordering::SeqCst) as u32;

    // Create the pollable
    let pollable = WasiPollable::new(pollable_id, stream_id);

    // Register the pollable
    let mut pollables = context.pollables.write().unwrap();
    pollables.insert(pollable_id, pollable);

    Ok(pollable_id as u64)
}

/// Check if a pollable is ready
pub fn check_pollable_ready(context: &WasiPreview2Context, pollable_id: u64) -> WasmtimeResult<bool> {
    let pollables = context.pollables.read().unwrap();
    let pollable = pollables.get(&(pollable_id as u32)).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Pollable {} not found", pollable_id),
        }
    })?;

    // If already marked ready, return true
    if pollable.ready {
        return Ok(true);
    }

    // Handle based on pollable type
    match &pollable.pollable_type {
        PollableType::Timer { target_nanos } => {
            // Timer pollable: check if elapsed time has reached target
            let elapsed = pollable.created_at.elapsed();
            Ok(elapsed.as_nanos() as u64 >= *target_nanos)
        }
        PollableType::Stream => {
            // Stream pollable: check the underlying stream status
            let stream_id = pollable.resource_id;
            let streams = context.streams.read().unwrap();
            if let Some(stream) = streams.get(&(stream_id as u32)) {
                match stream.stream_type {
                    WasiStreamType::InputStream => {
                        // Input stream is ready if it has data or is closed
                        Ok(!stream.buffer.is_empty()
                            || matches!(stream.status, WasiStreamStatus::Closed))
                    }
                    WasiStreamType::OutputStream => {
                        // Output stream is ready if it can accept writes (not closed)
                        Ok(!matches!(stream.status, WasiStreamStatus::Closed))
                    }
                    WasiStreamType::BidirectionalStream => {
                        // Bidirectional stream is ready if it has data to read or can accept writes
                        Ok(!stream.buffer.is_empty()
                            || !matches!(stream.status, WasiStreamStatus::Closed))
                    }
                }
            } else {
                // Stream was closed/removed, pollable is ready (with closed status)
                Ok(true)
            }
        }
    }
}

/// Block until a pollable is ready
pub fn block_on_pollable(
    context: &WasiPreview2Context,
    pollable_id: u64,
    timeout_ns: Option<u64>,
) -> WasmtimeResult<()> {
    let start = std::time::Instant::now();
    let timeout = timeout_ns.map(|ns| Duration::from_nanos(ns));

    loop {
        // Check if ready
        if check_pollable_ready(context, pollable_id)? {
            // Mark as ready in the pollable registry
            let mut pollables = context.pollables.write().unwrap();
            if let Some(pollable) = pollables.get_mut(&(pollable_id as u32)) {
                pollable.set_ready(true);
            }
            return Ok(());
        }

        // Check timeout
        if let Some(timeout_duration) = timeout {
            if start.elapsed() >= timeout_duration {
                return Err(WasmtimeError::Wasi {
                    message: "Pollable timeout".to_string(),
                });
            }
        }

        // Small sleep to avoid busy-waiting
        thread::sleep(Duration::from_micros(100));
    }
}

/// Close a pollable
pub fn close_pollable(context: &WasiPreview2Context, pollable_id: u64) -> WasmtimeResult<()> {
    let mut pollables = context.pollables.write().unwrap();
    pollables.remove(&(pollable_id as u32));
    Ok(())
}

/// Poll multiple pollables at once, returning the indices of ready pollables
pub fn poll_many(
    context: &WasiPreview2Context,
    pollable_ids: &[u64],
) -> WasmtimeResult<Vec<u32>> {
    let mut ready_indices = Vec::new();

    for (index, &pollable_id) in pollable_ids.iter().enumerate() {
        if check_pollable_ready(context, pollable_id)? {
            ready_indices.push(index as u32);
        }
    }

    Ok(ready_indices)
}

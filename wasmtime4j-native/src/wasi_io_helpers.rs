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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::wasi_preview2::{WasiPreview2Config, WasiStream, WasiStreamStatus, WasiStreamType};
    use wasmtime::Engine;

    fn test_context() -> WasiPreview2Context {
        let mut config = crate::engine::safe_wasmtime_config();
        config.async_support(true);
        let engine = Engine::new(&config).unwrap();
        WasiPreview2Context::new(engine, WasiPreview2Config::default()).unwrap()
    }

    #[test]
    fn create_input_stream_returns_id() {
        let ctx = test_context();
        let result = create_input_stream(&ctx, 1, 0);
        assert!(
            result.is_ok(),
            "create_input_stream should succeed, got: {:?}",
            result.err()
        );
        let stream_id = result.unwrap();
        println!("Created input stream with id: {}", stream_id);

        // Verify the stream exists in the context
        let streams = ctx.streams.read().unwrap();
        let stream = streams.get(&(stream_id as u32));
        assert!(
            stream.is_some(),
            "Stream with id {} should exist in context",
            stream_id
        );
        let stream = stream.unwrap();
        assert!(
            matches!(stream.stream_type, WasiStreamType::InputStream),
            "Stream type should be InputStream, got: {:?}",
            stream.stream_type
        );
        assert!(
            matches!(stream.status, WasiStreamStatus::Ready),
            "Stream status should be Ready, got: {:?}",
            stream.status
        );
    }

    #[test]
    fn read_from_nonexistent_stream_fails() {
        let ctx = test_context();
        let result = read_from_stream(&ctx, 99999, 10, false);
        assert!(
            result.is_err(),
            "Reading from non-existent stream should fail"
        );
        let err = result.unwrap_err();
        let err_msg = format!("{:?}", err);
        println!("Expected error for non-existent stream read: {}", err_msg);
        assert!(
            err_msg.contains("not found"),
            "Error should mention 'not found', got: {}",
            err_msg
        );
    }

    #[test]
    fn write_to_nonexistent_stream_fails() {
        let ctx = test_context();
        let result = write_to_stream(&ctx, 99999, &[1, 2, 3], false);
        assert!(
            result.is_err(),
            "Writing to non-existent stream should fail"
        );
        let err = result.unwrap_err();
        let err_msg = format!("{:?}", err);
        println!("Expected error for non-existent stream write: {}", err_msg);
        assert!(
            err_msg.contains("not found"),
            "Error should mention 'not found', got: {}",
            err_msg
        );
    }

    #[test]
    fn close_stream_marks_as_closed() {
        let ctx = test_context();

        // Insert a stream manually
        let stream = WasiStream {
            id: 42,
            stream_type: WasiStreamType::InputStream,
            buffer: vec![10, 20, 30],
            status: WasiStreamStatus::Ready,
            resource_id: None,
        };
        ctx.streams.write().unwrap().insert(42, stream);

        // Close the stream
        let close_result = close_stream(&ctx, 42);
        assert!(
            close_result.is_ok(),
            "close_stream should succeed, got: {:?}",
            close_result.err()
        );

        // Verify the stream is marked Closed
        {
            let streams = ctx.streams.read().unwrap();
            let stream = streams.get(&42).expect("Stream 42 should still exist after close");
            assert!(
                matches!(stream.status, WasiStreamStatus::Closed),
                "Stream status should be Closed after close_stream, got: {:?}",
                stream.status
            );
            assert!(
                stream.buffer.is_empty(),
                "Stream buffer should be cleared after close, had {} bytes",
                stream.buffer.len()
            );
        }

        // Reading from the closed stream should fail
        let read_result = read_from_stream(&ctx, 42, 10, false);
        assert!(
            read_result.is_err(),
            "Reading from closed stream should fail"
        );
        let err_msg = format!("{:?}", read_result.unwrap_err());
        println!("Expected error for closed stream read: {}", err_msg);
        assert!(
            err_msg.contains("closed"),
            "Error should mention 'closed', got: {}",
            err_msg
        );
    }

    #[test]
    fn check_write_capacity_on_output_stream() {
        let ctx = test_context();

        // Insert an output stream
        let stream = WasiStream {
            id: 50,
            stream_type: WasiStreamType::OutputStream,
            buffer: Vec::new(),
            status: WasiStreamStatus::Ready,
            resource_id: None,
        };
        ctx.streams.write().unwrap().insert(50, stream);

        let result = check_write_capacity(&ctx, 50);
        assert!(
            result.is_ok(),
            "check_write_capacity should succeed for valid output stream, got: {:?}",
            result.err()
        );
        let capacity = result.unwrap();
        println!("Write capacity for output stream: {}", capacity);
        assert!(
            capacity > 0,
            "Write capacity should be greater than 0, got: {}",
            capacity
        );
    }

    #[test]
    fn check_write_capacity_on_nonexistent_stream_fails() {
        let ctx = test_context();
        let result = check_write_capacity(&ctx, 99999);
        assert!(
            result.is_err(),
            "check_write_capacity on non-existent stream should fail"
        );
        let err_msg = format!("{:?}", result.unwrap_err());
        println!(
            "Expected error for non-existent stream capacity check: {}",
            err_msg
        );
        assert!(
            err_msg.contains("not found"),
            "Error should mention 'not found', got: {}",
            err_msg
        );
    }

    #[test]
    fn write_zeroes_to_nonexistent_stream_fails() {
        let ctx = test_context();
        let result = write_zeroes_to_stream(&ctx, 99999, 100, false);
        assert!(
            result.is_err(),
            "write_zeroes_to_stream on non-existent stream should fail"
        );
        let err_msg = format!("{:?}", result.unwrap_err());
        println!(
            "Expected error for non-existent stream write_zeroes: {}",
            err_msg
        );
        assert!(
            err_msg.contains("not found"),
            "Error should mention 'not found', got: {}",
            err_msg
        );
    }

    #[test]
    fn read_from_closed_stream_fails() {
        let ctx = test_context();

        // Insert a stream that is already closed
        let stream = WasiStream {
            id: 60,
            stream_type: WasiStreamType::InputStream,
            buffer: vec![1, 2, 3],
            status: WasiStreamStatus::Closed,
            resource_id: None,
        };
        ctx.streams.write().unwrap().insert(60, stream);

        let result = read_from_stream(&ctx, 60, 10, false);
        assert!(
            result.is_err(),
            "Reading from a stream with Closed status should fail"
        );
        let err_msg = format!("{:?}", result.unwrap_err());
        println!("Expected error for pre-closed stream read: {}", err_msg);
        assert!(
            err_msg.contains("closed"),
            "Error should mention 'closed', got: {}",
            err_msg
        );
    }

    #[test]
    fn create_output_stream_pollable_for_valid_stream() {
        let ctx = test_context();

        // Insert an output stream
        let stream = WasiStream {
            id: 70,
            stream_type: WasiStreamType::OutputStream,
            buffer: Vec::new(),
            status: WasiStreamStatus::Ready,
            resource_id: None,
        };
        ctx.streams.write().unwrap().insert(70, stream);

        let result = create_output_stream_pollable(&ctx, 70);
        assert!(
            result.is_ok(),
            "create_output_stream_pollable should succeed for valid stream, got: {:?}",
            result.err()
        );
        let pollable_id = result.unwrap();
        println!(
            "Created output stream pollable with id: {}",
            pollable_id
        );

        // Verify the pollable exists
        let pollables = ctx.pollables.read().unwrap();
        let pollable = pollables.get(&(pollable_id as u32));
        assert!(
            pollable.is_some(),
            "Pollable with id {} should exist in context",
            pollable_id
        );
        let pollable = pollable.unwrap();
        assert_eq!(
            pollable.resource_id, 70,
            "Pollable resource_id should point to stream 70, got: {}",
            pollable.resource_id
        );
    }

    #[test]
    fn create_pollable_for_nonexistent_stream_fails() {
        let ctx = test_context();
        let result = create_output_stream_pollable(&ctx, 99999);
        assert!(
            result.is_err(),
            "Creating pollable for non-existent stream should fail"
        );
        let err_msg = format!("{:?}", result.unwrap_err());
        println!(
            "Expected error for non-existent stream pollable creation: {}",
            err_msg
        );
        assert!(
            err_msg.contains("not found"),
            "Error should mention 'not found', got: {}",
            err_msg
        );

        // Also test create_input_stream_pollable
        let result2 = create_input_stream_pollable(&ctx, 99999);
        assert!(
            result2.is_err(),
            "Creating input pollable for non-existent stream should fail"
        );
        let err_msg2 = format!("{:?}", result2.unwrap_err());
        println!(
            "Expected error for non-existent input stream pollable: {}",
            err_msg2
        );
        assert!(
            err_msg2.contains("not found"),
            "Error should mention 'not found', got: {}",
            err_msg2
        );
    }

    #[test]
    fn check_pollable_ready_for_nonexistent_fails() {
        let ctx = test_context();
        let result = check_pollable_ready(&ctx, 99999);
        assert!(
            result.is_err(),
            "check_pollable_ready for non-existent pollable should fail"
        );
        let err_msg = format!("{:?}", result.unwrap_err());
        println!(
            "Expected error for non-existent pollable ready check: {}",
            err_msg
        );
        assert!(
            err_msg.contains("not found"),
            "Error should mention 'not found', got: {}",
            err_msg
        );
    }

    #[test]
    fn close_pollable_for_nonexistent_succeeds() {
        let ctx = test_context();
        // close_pollable just removes from the map; removing a non-existent key is a no-op
        let result = close_pollable(&ctx, 99999);
        assert!(
            result.is_ok(),
            "close_pollable for non-existent pollable should succeed (no-op), got: {:?}",
            result.err()
        );
        println!("close_pollable for non-existent pollable returned Ok as expected (no-op removal)");
    }

    #[test]
    fn poll_many_empty_list() {
        let ctx = test_context();
        let result = poll_many(&ctx, &[]);
        assert!(
            result.is_ok(),
            "poll_many with empty list should succeed, got: {:?}",
            result.err()
        );
        let ready = result.unwrap();
        assert!(
            ready.is_empty(),
            "poll_many with empty list should return empty vec, got: {:?}",
            ready
        );
        println!("poll_many with empty pollable list returned empty vec as expected");
    }
}

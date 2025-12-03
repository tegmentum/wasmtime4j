//! Shared helper functions for WASI clocks operations
//!
//! This module provides common functionality used by both JNI and Panama FFI bindings
//! for WASI Preview 2 clocks operations (monotonic clock, wall clock, timezone).

use crate::error::WasmtimeResult;
use crate::wasi_preview2::WasiPreview2Context;
use std::time::{SystemTime, UNIX_EPOCH};

/// DateTime structure matching WIT datetime record
#[derive(Debug, Clone, Copy)]
pub struct DateTime {
    pub seconds: u64,
    pub nanoseconds: u32,
}

/// Timezone display information matching WIT timezone-display record
#[derive(Debug, Clone)]
pub struct TimezoneDisplay {
    pub utc_offset_seconds: i32,
    pub name: String,
    pub in_daylight_saving_time: bool,
}

/// Get the current monotonic clock instant in nanoseconds
///
/// Returns a u64 representing nanoseconds since an unspecified epoch.
/// The clock is monotonic - successive calls return non-decreasing values.
pub fn monotonic_now(_context: &WasiPreview2Context) -> WasmtimeResult<u64> {
    // MVP: Use system monotonic time
    // TODO: Replace with actual Wasmtime monotonic clock API
    let now = std::time::Instant::now();
    let nanos = now.elapsed().as_nanos() as u64;
    Ok(nanos)
}

/// Get the monotonic clock resolution in nanoseconds
///
/// Returns the duration of a single clock tick.
pub fn monotonic_resolution(_context: &WasiPreview2Context) -> WasmtimeResult<u64> {
    // MVP: Return 1 nanosecond resolution
    // TODO: Replace with actual Wasmtime clock resolution
    Ok(1)
}

/// Subscribe to monotonic clock at a specific instant
///
/// Creates a pollable that becomes ready when the clock reaches the specified instant.
/// Returns a pollable ID that can be used with the poll interface.
pub fn monotonic_subscribe_instant(
    context: &WasiPreview2Context,
    when: u64,
) -> WasmtimeResult<u64> {
    // Generate a unique pollable ID
    let pollable_id = context
        .next_operation_id
        .fetch_add(1, std::sync::atomic::Ordering::SeqCst) as u32;

    // Create a timer pollable that becomes ready at the target instant
    let pollable = crate::wasi_preview2::WasiPollable::new_timer_instant(pollable_id, when);

    // Register the pollable
    let mut pollables = context.pollables.write().unwrap();
    pollables.insert(pollable_id, pollable);

    Ok(pollable_id as u64)
}

/// Subscribe to monotonic clock for a duration
///
/// Creates a pollable that becomes ready after the specified duration elapses.
/// Returns a pollable ID that can be used with the poll interface.
pub fn monotonic_subscribe_duration(
    context: &WasiPreview2Context,
    duration: u64,
) -> WasmtimeResult<u64> {
    // Generate a unique pollable ID
    let pollable_id = context
        .next_operation_id
        .fetch_add(1, std::sync::atomic::Ordering::SeqCst) as u32;

    // Create a timer pollable that becomes ready after the duration
    let pollable = crate::wasi_preview2::WasiPollable::new_timer_duration(pollable_id, duration);

    // Register the pollable
    let mut pollables = context.pollables.write().unwrap();
    pollables.insert(pollable_id, pollable);

    Ok(pollable_id as u64)
}

/// Get the current wall clock time
///
/// Returns a DateTime representing seconds and nanoseconds since Unix epoch.
/// This clock is not monotonic and may be adjusted by system time changes.
pub fn wall_clock_now(_context: &WasiPreview2Context) -> WasmtimeResult<DateTime> {
    // MVP: Use system time
    // TODO: Replace with actual Wasmtime wall clock API
    let now = SystemTime::now();
    let duration = now.duration_since(UNIX_EPOCH)
        .map_err(|e| crate::error::WasmtimeError::Wasi {
            message: format!("System time before Unix epoch: {}", e),
        })?;

    let seconds = duration.as_secs();
    let nanoseconds = duration.subsec_nanos();

    Ok(DateTime {
        seconds,
        nanoseconds,
    })
}

/// Get the wall clock resolution
///
/// Returns a DateTime representing the clock resolution.
pub fn wall_clock_resolution(_context: &WasiPreview2Context) -> WasmtimeResult<DateTime> {
    // MVP: Return 1 nanosecond resolution
    // TODO: Replace with actual Wasmtime wall clock resolution
    Ok(DateTime {
        seconds: 0,
        nanoseconds: 1,
    })
}

/// Get timezone display information for a specific datetime
///
/// Returns timezone information including UTC offset, name, and DST status.
/// If timezone cannot be determined, returns UTC with zero offset.
pub fn timezone_display(
    _context: &WasiPreview2Context,
    _when: DateTime,
) -> WasmtimeResult<TimezoneDisplay> {
    // MVP: Return UTC timezone
    // TODO: Replace with actual Wasmtime timezone API
    Ok(TimezoneDisplay {
        utc_offset_seconds: 0,
        name: "UTC".to_string(),
        in_daylight_saving_time: false,
    })
}

/// Get UTC offset in seconds for a specific datetime
///
/// Returns the number of seconds to add to UTC to get local time.
/// Positive values are east of UTC, negative values are west.
pub fn timezone_utc_offset(
    _context: &WasiPreview2Context,
    _when: DateTime,
) -> WasmtimeResult<i32> {
    // MVP: Return zero offset (UTC)
    // TODO: Replace with actual Wasmtime timezone API
    Ok(0)
}

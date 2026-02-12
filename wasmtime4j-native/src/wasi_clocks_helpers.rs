//! Shared helper functions for WASI clocks operations
//!
//! This module provides common functionality used by both JNI and Panama FFI bindings
//! for WASI Preview 2 clocks operations (monotonic clock, wall clock, timezone).

use crate::error::WasmtimeResult;
use crate::wasi_preview2::WasiPreview2Context;
use std::sync::OnceLock;
use std::time::{Instant, SystemTime, UNIX_EPOCH};

/// Global epoch for monotonic clock - initialized on first use
static MONOTONIC_EPOCH: OnceLock<Instant> = OnceLock::new();

/// Get the monotonic clock epoch, initializing if needed
fn get_monotonic_epoch() -> &'static Instant {
    MONOTONIC_EPOCH.get_or_init(Instant::now)
}

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
/// Returns a u64 representing nanoseconds since the first call to this function
/// (the monotonic epoch). The clock is monotonic - successive calls return
/// non-decreasing values.
pub fn monotonic_now(_context: &WasiPreview2Context) -> WasmtimeResult<u64> {
    // Use a global epoch so that multiple calls return increasing values
    let epoch = get_monotonic_epoch();
    let elapsed = epoch.elapsed();
    Ok(elapsed.as_nanos() as u64)
}

/// Get the monotonic clock resolution in nanoseconds
///
/// Returns the duration of a single clock tick.
/// Most systems support nanosecond resolution for the monotonic clock.
pub fn monotonic_resolution(_context: &WasiPreview2Context) -> WasmtimeResult<u64> {
    // Nanosecond resolution is standard for most systems
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
    let mut pollables = context.pollables.write().unwrap_or_else(|e| e.into_inner());
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
    let mut pollables = context.pollables.write().unwrap_or_else(|e| e.into_inner());
    pollables.insert(pollable_id, pollable);

    Ok(pollable_id as u64)
}

/// Get the current wall clock time
///
/// Returns a DateTime representing seconds and nanoseconds since Unix epoch.
/// This clock is not monotonic and may be adjusted by system time changes.
pub fn wall_clock_now(_context: &WasiPreview2Context) -> WasmtimeResult<DateTime> {
    // Use host system time - this is the standard WASI implementation
    let now = SystemTime::now();
    let duration =
        now.duration_since(UNIX_EPOCH)
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
/// Most systems support nanosecond resolution for the wall clock.
pub fn wall_clock_resolution(_context: &WasiPreview2Context) -> WasmtimeResult<DateTime> {
    // Nanosecond resolution is standard for most systems
    Ok(DateTime {
        seconds: 0,
        nanoseconds: 1,
    })
}

/// Get timezone display information for a specific datetime
///
/// Returns timezone information including UTC offset, name, and DST status.
/// Currently returns UTC as the default timezone. For local timezone support,
/// the host would need to provide additional configuration.
pub fn timezone_display(
    _context: &WasiPreview2Context,
    _when: DateTime,
) -> WasmtimeResult<TimezoneDisplay> {
    // Return UTC as the default WASI timezone
    // Local timezone support requires host-specific configuration
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
/// Currently returns 0 (UTC) as the default.
pub fn timezone_utc_offset(_context: &WasiPreview2Context, _when: DateTime) -> WasmtimeResult<i32> {
    // Return UTC offset (0) as the default
    Ok(0)
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

    #[test]
    fn monotonic_now_returns_value() {
        let ctx = test_context();
        let first = monotonic_now(&ctx).unwrap();
        let second = monotonic_now(&ctx).unwrap();
        assert!(
            second >= first,
            "Second call ({}) should be >= first call ({})",
            second,
            first
        );
    }

    #[test]
    fn monotonic_now_is_monotonic() {
        let ctx = test_context();
        let mut previous = monotonic_now(&ctx).unwrap();
        for i in 0..100 {
            let current = monotonic_now(&ctx).unwrap();
            assert!(
                current >= previous,
                "Iteration {}: current ({}) should be >= previous ({})",
                i,
                current,
                previous
            );
            previous = current;
        }
    }

    #[test]
    fn monotonic_resolution_returns_one() {
        let ctx = test_context();
        let resolution = monotonic_resolution(&ctx).unwrap();
        assert_eq!(resolution, 1, "Monotonic resolution should be 1 nanosecond");
    }

    #[test]
    fn monotonic_subscribe_instant_returns_pollable_id() {
        let ctx = test_context();
        let when = monotonic_now(&ctx).unwrap() + 1_000_000_000;
        let pollable_id = monotonic_subscribe_instant(&ctx, when).unwrap();
        assert!(
            pollable_id > 0,
            "Pollable ID ({}) should be > 0",
            pollable_id
        );
    }

    #[test]
    fn monotonic_subscribe_duration_returns_pollable_id() {
        let ctx = test_context();
        let duration_ns = 1_000_000_000u64; // 1 second
        let pollable_id = monotonic_subscribe_duration(&ctx, duration_ns).unwrap();
        assert!(
            pollable_id > 0,
            "Pollable ID ({}) should be > 0",
            pollable_id
        );
    }

    #[test]
    fn wall_clock_now_returns_valid_datetime() {
        let ctx = test_context();
        let now = wall_clock_now(&ctx).unwrap();
        assert!(
            now.seconds > 0,
            "Wall clock seconds ({}) should be > 0 (after Unix epoch)",
            now.seconds
        );
        assert!(
            now.nanoseconds < 1_000_000_000,
            "Wall clock nanoseconds ({}) should be < 1_000_000_000",
            now.nanoseconds
        );
    }

    #[test]
    fn wall_clock_resolution_is_nanosecond() {
        let ctx = test_context();
        let resolution = wall_clock_resolution(&ctx).unwrap();
        assert_eq!(
            resolution.seconds, 0,
            "Wall clock resolution seconds should be 0, got {}",
            resolution.seconds
        );
        assert_eq!(
            resolution.nanoseconds, 1,
            "Wall clock resolution nanoseconds should be 1, got {}",
            resolution.nanoseconds
        );
    }

    #[test]
    fn timezone_display_returns_utc() {
        let ctx = test_context();
        let when = wall_clock_now(&ctx).unwrap();
        let display = timezone_display(&ctx, when).unwrap();
        assert_eq!(
            display.name, "UTC",
            "Timezone name should be 'UTC', got '{}'",
            display.name
        );
        assert_eq!(
            display.utc_offset_seconds, 0,
            "UTC offset should be 0, got {}",
            display.utc_offset_seconds
        );
        assert!(
            !display.in_daylight_saving_time,
            "UTC should not be in daylight saving time"
        );
    }

    #[test]
    fn timezone_utc_offset_returns_zero() {
        let ctx = test_context();
        let when = wall_clock_now(&ctx).unwrap();
        let offset = timezone_utc_offset(&ctx, when).unwrap();
        assert_eq!(offset, 0, "UTC offset should be 0, got {}", offset);
    }
}

//! Interop utilities for managing Rust objects across JNI/Panama FFI boundaries
//!
//! This module provides thread-safe pointer conversion and resource management
//! patterns inspired by kawamuray/wasmtime-java to ensure safe access across
//! the native interface boundary.

use std::cell::UnsafeCell;
use std::ops::{Deref, DerefMut};
use std::sync::atomic::{AtomicU64, Ordering};
use parking_lot::Mutex;
use std::thread;

/// Thread-safe, reentrant lock for Rust objects exposed via FFI
///
/// This allows the same thread to acquire the lock multiple times, which is
/// essential for WASM execution where Wasmtime may need to access the Store
/// context multiple times during a single function call.
///
/// Implementation based on kawamuray/wasmtime-java's approach:
/// - Uses a Mutex for inter-thread exclusion
/// - Uses AtomicU64 to track which thread currently owns the lock
/// - Allows recursive locking by the same thread
pub struct ReentrantLock<T> {
    owner: AtomicU64,
    lock_count: AtomicU64,
    mutex: Mutex<()>,
    data: UnsafeCell<T>,
}

unsafe impl<T: Send> Send for ReentrantLock<T> {}
unsafe impl<T: Send> Sync for ReentrantLock<T> {}

impl<T: std::fmt::Debug> std::fmt::Debug for ReentrantLock<T> {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ReentrantLock")
            .field("owner", &self.owner.load(Ordering::Relaxed))
            .field("lock_count", &self.lock_count.load(Ordering::Relaxed))
            .finish()
    }
}

impl<T> ReentrantLock<T> {
    /// Create a new reentrant lock containing the value
    pub fn new(value: T) -> Self {
        ReentrantLock {
            owner: AtomicU64::new(0),
            lock_count: AtomicU64::new(0),
            mutex: Mutex::new(()),
            data: UnsafeCell::new(value),
        }
    }

    /// Lock and get a guard to the inner value
    ///
    /// This will block if another thread holds the lock, but allows
    /// reentrant access from the same thread.
    pub fn lock(&self) -> ReentrantLockGuard<'_, T> {
        let current_thread_id = current_thread_id();

        // Check if current thread already owns the lock
        let owner = self.owner.load(Ordering::Acquire);
        if owner == current_thread_id {
            // Reentrant lock - just increment count
            self.lock_count.fetch_add(1, Ordering::Relaxed);
            return ReentrantLockGuard { lock: self, _guard: None };
        }

        // Need to acquire the lock
        let guard = self.mutex.lock();

        // Set this thread as owner
        self.owner.store(current_thread_id, Ordering::Release);
        self.lock_count.store(1, Ordering::Relaxed);

        ReentrantLockGuard { lock: self, _guard: Some(guard) }
    }

    /// Try to lock without blocking
    ///
    /// Returns None if the lock is held by another thread.
    pub fn try_lock(&self) -> Option<ReentrantLockGuard<'_, T>> {
        let current_thread_id = current_thread_id();

        // Check if current thread already owns the lock
        let owner = self.owner.load(Ordering::Acquire);
        if owner == current_thread_id {
            // Reentrant lock - just increment count
            self.lock_count.fetch_add(1, Ordering::Relaxed);
            return Some(ReentrantLockGuard { lock: self, _guard: None });
        }

        // Try to acquire the lock
        let guard = self.mutex.try_lock()?;

        // Set this thread as owner
        self.owner.store(current_thread_id, Ordering::Release);
        self.lock_count.store(1, Ordering::Relaxed);

        Some(ReentrantLockGuard { lock: self, _guard: Some(guard) })
    }

    /// Unlock the reentrant lock (called by guard drop)
    fn unlock(&self) {
        let count = self.lock_count.fetch_sub(1, Ordering::Relaxed);
        if count == 1 {
            // Last unlock - release ownership
            self.owner.store(0, Ordering::Release);
            // Note: MutexGuard will be dropped automatically, releasing the mutex
        }
    }
}

/// Guard providing access to the inner value of a ReentrantLock
pub struct ReentrantLockGuard<'a, T> {
    lock: &'a ReentrantLock<T>,
    _guard: Option<parking_lot::MutexGuard<'a, ()>>,
}

impl<'a, T> Drop for ReentrantLockGuard<'a, T> {
    fn drop(&mut self) {
        self.lock.unlock();
    }
}

impl<'a, T> Deref for ReentrantLockGuard<'a, T> {
    type Target = T;

    fn deref(&self) -> &Self::Target {
        unsafe { &*self.lock.data.get() }
    }
}

impl<'a, T> DerefMut for ReentrantLockGuard<'a, T> {
    fn deref_mut(&mut self) -> &mut Self::Target {
        unsafe { &mut *self.lock.data.get() }
    }
}

/// Get a unique ID for the current thread
fn current_thread_id() -> u64 {
    // Use thread ID as u64
    // Note: ThreadId doesn't have a stable numeric representation in stable Rust,
    // so we use a workaround via the debug representation
    let thread_id = thread::current().id();
    let debug_str = format!("{:?}", thread_id);
    // Extract the number from "ThreadId(N)"
    debug_str
        .trim_start_matches("ThreadId(")
        .trim_end_matches(')')
        .parse::<u64>()
        .unwrap_or(0)
}

/// Convert a Rust object into a raw pointer for FFI
///
/// The object is boxed twice: once in a regular Box, and then wrapped
/// in a ReentrantLock for thread-safe access. This returns a raw pointer
/// that can be passed across the FFI boundary.
///
/// # Safety
///
/// The returned pointer must be freed with `from_raw` or `dispose_inner`
/// to avoid memory leaks.
pub fn into_raw<T>(value: T) -> *mut std::os::raw::c_void {
    let reentrant = ReentrantLock::new(value);
    Box::into_raw(Box::new(reentrant)) as *mut std::os::raw::c_void
}

/// Restore a Rust object from a raw pointer
///
/// This takes ownership of the pointer and converts it back into a
/// boxed ReentrantLock. The object will be dropped when the Box is dropped.
///
/// # Safety
///
/// - ptr must have been created by `into_raw`
/// - ptr must not have been previously freed
/// - ptr must not be used after this call
pub unsafe fn from_raw<T>(ptr: *mut std::os::raw::c_void) -> Box<ReentrantLock<T>> {
    Box::from_raw(ptr as *mut ReentrantLock<T>)
}

/// Get a reference to a Rust object from a raw pointer
///
/// This provides temporary access to the object without taking ownership.
/// The object remains valid and owned by the FFI layer.
///
/// # Safety
///
/// - ptr must have been created by `into_raw`
/// - ptr must still be valid (not freed)
/// - The returned reference must not outlive the pointer
pub unsafe fn ref_from_raw<T>(ptr: *const std::os::raw::c_void) -> &'static ReentrantLock<T> {
    &*(ptr as *const ReentrantLock<T>)
}

/// Get a mutable reference to a Rust object from a raw pointer
///
/// This provides temporary mutable access to the object without taking ownership.
/// The object remains valid and owned by the FFI layer.
///
/// # Safety
///
/// - ptr must have been created by `into_raw`
/// - ptr must still be valid (not freed)
/// - The returned reference must not outlive the pointer
/// - No other references to the object may exist
pub unsafe fn ref_from_raw_mut<T>(ptr: *mut std::os::raw::c_void) -> &'static mut ReentrantLock<T> {
    &mut *(ptr as *mut ReentrantLock<T>)
}

/// Dispose of a Rust object by consuming the pointer
///
/// This is equivalent to `from_raw` but explicitly signals that the
/// object is being destroyed. The Box is dropped immediately.
///
/// # Safety
///
/// - ptr must have been created by `into_raw`
/// - ptr must not have been previously freed
/// - ptr must not be used after this call
pub unsafe fn dispose_inner<T>(ptr: *mut std::os::raw::c_void) {
    if !ptr.is_null() {
        let _boxed = from_raw::<T>(ptr);
        // Box is dropped here, which drops the ReentrantLock and its contents
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_reentrant_box_basic() {
        let rbox = ReentrantLock::new(42);
        let guard = rbox.lock();
        assert_eq!(*guard, 42);
    }

    #[test]
    fn test_reentrant_box_mut() {
        let rbox = ReentrantLock::new(42);
        {
            let mut guard = rbox.lock();
            *guard = 100;
        }
        let guard = rbox.lock();
        assert_eq!(*guard, 100);
    }

    #[test]
    fn test_reentrant_same_thread() {
        let rbox = ReentrantLock::new(vec![1, 2, 3]);
        let guard1 = rbox.lock();
        // Same thread can acquire lock again
        let guard2 = rbox.lock();
        assert_eq!(*guard1, vec![1, 2, 3]);
        assert_eq!(*guard2, vec![1, 2, 3]);
    }

    #[test]
    fn test_into_raw_and_back() {
        let value = vec![1, 2, 3, 4, 5];
        let ptr = into_raw(value.clone());

        unsafe {
            let reentrant_box = from_raw::<Vec<i32>>(ptr);
            let guard = reentrant_box.lock();
            assert_eq!(*guard, vec![1, 2, 3, 4, 5]);
        }
    }

    #[test]
    fn test_ref_from_raw() {
        let value = String::from("hello");
        let ptr = into_raw(value);

        unsafe {
            let reentrant_ref = ref_from_raw::<String>(ptr);
            let guard = reentrant_ref.lock();
            assert_eq!(*guard, "hello");

            // Clean up
            dispose_inner::<String>(ptr as *mut _);
        }
    }

    #[test]
    fn test_dispose_inner() {
        let value = vec![1, 2, 3];
        let ptr = into_raw(value);

        unsafe {
            dispose_inner::<Vec<i32>>(ptr as *mut _);
        }
        // Should not crash - value was properly cleaned up
    }

    #[test]
    fn test_dispose_null() {
        unsafe {
            dispose_inner::<String>(std::ptr::null_mut());
        }
        // Should not crash on null pointer
    }
}

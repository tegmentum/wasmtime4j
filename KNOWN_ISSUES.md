# Known Issues

## MemoryType min/max Swap Issue

**Status**: RESOLVED ✅
**Affected Test**: `ModuleImportValidationTest.testGetMemoryType`
**File**: `wasmtime4j-comparison-tests/src/test/java/ai/tegmentum/wasmtime4j/comparison/module/ModuleImportValidationTest.java:177`
**Fixed**: Fixed in commit [pending]

### Description

The test creates a WebAssembly memory with minimum=1 pages and maximum=10 pages:
```wat
(memory (export "mem") 1 10)
```

The test was failing because `MemoryType.getMinimum()` returned 10 and `MemoryType.getMaximum()` returned 1 (swapped values), but was expecting the correct values.

### Root Cause

The bug was in `/Users/zacharywhitley/git/wasmtime4j/wasmtime4j-native/src/jni_bindings.rs` at line 7226 in the `nativeGetMemoryTypeInfo` function.

The pointer `memory_ptr` pointed to a `ValidatedMemory` struct, but the code incorrectly cast it as `*const Memory`:
```rust
// BUGGY CODE:
let memory = unsafe { &*(memory_ptr as *const crate::memory::Memory) };
```

This caused reading from the wrong memory offset. The `ValidatedMemory` struct wraps `Memory`:
```rust
pub struct ValidatedMemory {
    magic: u64,
    memory: Memory,  // Memory is INSIDE ValidatedMemory, not at pointer address!
    created_at: Instant,
    access_count: AtomicU64,
    is_destroyed: AtomicBool,
}
```

When casting the pointer directly to `Memory`, the code read at offset 0 (the `magic` field) thinking it was the `Memory` struct, which resulted in reading garbage bytes from the wrong memory location.

### Solution

Changed the pointer access pattern to correctly cast to `ValidatedMemory` first, then use the `access_memory()` method to safely access the inner `Memory` struct:

```rust
// FIXED CODE:
let validated_memory = unsafe { &*(memory_ptr as *const crate::memory::core::ValidatedMemory) };
let memory = validated_memory.access_memory()?;
```

This ensures the code properly reads from the correct memory offset to access the `memory_type` field containing the correct minimum and maximum values.

### Code Locations

**Fixed File**:
- `wasmtime4j-native/src/jni_bindings.rs:7218-7230` - Fixed pointer casting in `nativeGetMemoryTypeInfo()`

**Related Files**:
- `wasmtime4j-native/src/memory.rs:158-169` - `Memory` struct definition
- `wasmtime4j-native/src/memory.rs:1837-1843` - `ValidatedMemory` struct definition
- `wasmtime4j-native/src/memory.rs:1868-1878` - `ValidatedMemory::access_memory()` method

### Verification

Test now passes successfully:
```
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

### References

- WebAssembly Spec: Memory limits specified as `(memory initial maximum)`
- MDN WebAssembly.Memory: Takes `initial` and `maximum` in pages (64KiB each)

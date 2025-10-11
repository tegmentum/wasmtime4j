# Critical Issue: Wasmtime Store Context Lifetime Management

## Problem Statement

Panama FFI tests crash with "object used with the wrong store" panic from Wasmtime. This affects all export operations (Memory, Global, Table) in the Panama FFI implementation.

## Root Cause

Wasmtime objects (Memory, Global, Table, etc.) are intrinsically tied to the Store context that created them via internal store IDs. The current architecture violates Wasmtime's borrowing rules:

1. When `wasmtime4j_instance_get_memory_by_name()` is called, it:
   - Gets a `&mut Store` reference via `get_store_mut(store_ptr)`
   - Calls `instance.get_memory(store, name)` which returns a `wasmtime::Memory`
   - This Memory is tied to that specific mutable borrow of the Store
   - Wraps it in our `Memory` struct and returns

2. Later, when Panama FFI memory operations are called:
   - They get another `&mut Store` reference via `get_store_mut(store_ptr)` using the SAME pointer
   - Try to use the Memory with this new mutable reference
   - Wasmtime detects that the Memory's internal store ID doesn't match this new context
   - Panics with "object used with the wrong store"

The issue is that Wasmtime tracks store contexts, and even though we're using the same Store pointer, each call to `get_store_mut` creates a new borrow context that Wasmtime treats as different.

## Evidence

1. **JNI Implementation**: The JNI version works because it uses `get_metadata()` for size operations, which doesn't require store context
2. **Both Memory and Global fail**: Same error occurs for both types, indicating systematic issue
3. **Panic location**: `wasmtime-37.0.2/src/runtime/store/data.rs:196` - Wasmtime's store validation code

## Failed Attempted Fixes

### 1. Using Correct Pointer Dereferencing
**Attempt**: Changed from `ffi_utils::deref_ptr` to `crate::memory::core::get_memory_ref`
**Result**: Still fails - the issue isn't pointer dereferencing but store context lifetime

### 2. Wrapping Memory Properly
**Attempt**: Changed `wasmtime4j_instance_get_memory_by_name` to wrap `wasmtime::Memory` in our `Memory` struct using `Memory::from_wasmtime_memory()`
**Result**: Still fails - wrapping doesn't solve the underlying store context issue

### 3. Verifying Store Pointer Consistency
**Attempt**: Verified that `PanamaStore.getNativeStore()` always returns the same pointer
**Result**: Pointer is consistent, but multiple borrows of same pointer create different contexts

## Wasmtime's Design Constraints

From Wasmtime's architecture:
- Objects like Memory contain an internal store ID (not a pointer, but an ID)
- When you call operations on these objects, Wasmtime validates the store ID matches
- Multiple `&mut` borrows of the same Store are treated as different contexts by Wasmtime's validation
- This is by design to ensure memory safety and prevent data races

## Potential Solutions

### Solution 1: Store-Less Operations (Metadata Only)
**Approach**: Use Memory wrapper's metadata for read-only operations (size, etc.)
**Pros**:
- Avoids store context issues for simple queries
- Matches JNI implementation pattern
**Cons**:
- Doesn't work for operations that need actual memory access (read/write bytes)
- Metadata may become stale if operations happen outside our wrapper

### Solution 2: Single Store Context Per Operation Chain
**Approach**: Redesign so Memory/Global objects don't store the store pointer. Instead, require store to be passed from Instance for each operation.
**Pros**:
- Ensures store context consistency
- Matches Wasmtime's intended usage pattern
**Cons**:
- Major API change - Memory operations would need Instance reference
- More complex API for users

### Solution 3: Store Context Caching
**Approach**: Have the Store wrapper maintain a single long-lived context that's reused
**Pros**:
- Minimal API changes
- Could work if we can maintain context lifetime
**Cons**:
- Complex to implement safely in Rust
- May conflict with Wasmtime's internal locking mechanisms
- Risky for thread safety

### Solution 4: Redesign Instance Export Retrieval
**Approach**: Don't return Memory/Global objects directly. Instead, return handles that defer operations back to Instance.
**Pros**:
- Instance maintains sole access to store context
- Clean separation of concerns
**Cons**:
- Complete architectural redesign needed
- Breaks current API design

### Solution 5: Use Store-Independent Memory Access (Unsafe)
**Approach**: Access memory data directly via unsafe pointer operations, bypassing Wasmtime's validation
**Pros**:
- Could work around the issue
**Cons**:
- Extremely unsafe - violates Wasmtime's safety guarantees
- Could cause memory corruption or crashes
- NOT RECOMMENDED

## Recommended Solution

**Solution 2: Single Store Context Per Operation Chain** appears to be the most viable:

1. Memory/Global objects don't store store references
2. Instance maintains the store reference
3. Operations like `memory.read(...)` become `instance.readMemory(memoryHandle, ...)`
4. This ensures all operations use the same store context (the one held by Instance)

### Implementation Changes Required:

1. **Java API Changes**:
   - Change WasmMemory interface to not require store in methods
   - Memory operations get store internally from parent instance
   - Or, make Memory operations require Instance parameter

2. **Rust FFI Changes**:
   - Panama FFI memory operations take instance_ptr instead of store_ptr
   - Extract store from instance for each operation
   - Ensures store context consistency

3. **Testing Changes**:
   - Update tests to use new API pattern
   - May need to keep Instance reference alive during memory operations

## Current Status

- **Implementation**: Complete but non-functional
- **Tests**: Created but failing with store context error
- **Code Quality**: Properly formatted and structured
- **Blocking Issue**: Cannot proceed with current architecture
- **Next Steps**: Requires architectural decision and redesign

## Alternative: Accept Store Context Limitations

If the redesign is too extensive, we could:
1. Document that Panama FFI export operations have limitations
2. Use metadata-based operations where possible (size queries, type info)
3. For operations requiring store context (read/write), require them to be called through Instance
4. Focus on function calls and WASI support instead of export manipulation

This would make Panama FFI less feature-complete but functional for primary use cases (running WASM modules, calling functions).

## Files Affected

- `/wasmtime4j-native/src/instance.rs` - Export retrieval (line 2001-2028)
- `/wasmtime4j-native/src/panama_ffi.rs` - Memory/Global operations (lines 1352-1467, 1720-1796)
- `/wasmtime4j-panama/src/main/java/ai/tegmentum/wasmtime4j/panama/PanamaMemory.java` - Memory implementation
- `/wasmtime4j-panama/src/main/java/ai/tegmentum/wasmtime4j/panama/PanamaGlobal.java` - Global implementation
- `/wasmtime4j-panama/src/test/java/ai/tegmentum/wasmtime4j/panama/PanamaMemoryTest.java` - Tests
- `/wasmtime4j-panama/src/test/java/ai/tegmentum/wasmtime4j/panama/PanamaGlobalTest.java` - Tests

## References

- Wasmtime panic location: `wasmtime-37.0.2/src/runtime/store/data.rs:196`
- JNI working implementation: `/wasmtime4j-native/src/jni_bindings.rs` (uses metadata)
- Store context management: `/wasmtime4j-native/src/store.rs`

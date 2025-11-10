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

---

## Cross-Engine Instantiation Error

**Status**: ACTIVE BUG 🔴
**Severity**: Critical
**Affects**: All tests that create Module and Instance
**First Identified**: 2025-11-10

### Symptom

When running tests, you'll see:
```
InstantiationException: Instance error: Failed to create instance:
cross-`Engine` instantiation is not currently supported
```

### Root Cause

This is a Rust-level Arc pointer equality issue in the Wasmtime integration:

1. The `Engine` struct wraps `Arc<WasmtimeEngine>`
2. When `Module::compile(engine, bytes)` is called, it uses `engine.inner()` which returns `&WasmtimeEngine`
3. When `Store::new(engine)` is called, it also uses `engine.inner()`
4. Wasmtime's `Module::new()` and `Store::new()` internally **clone the Engine Arc** and store their own Arc reference
5. Even though both start with references to the SAME `Arc<WasmtimeEngine>`, Wasmtime creates NEW Arc references
6. Wasmtime's `Instance::new()` checks if `module.engine() == store.engine()` by comparing Arc pointers using `Arc::ptr_eq()`
7. Since Module and Store have DIFFERENT Arc clones (different pointer addresses, same data), the comparison **FAILS**

### Code Locations

- Module compilation: `wasmtime4j-native/src/module.rs:207`
- Store creation: `wasmtime4j-native/src/store.rs:446`
- Instance validation: `wasmtime4j-native/src/instance.rs:169`
- Engine wrapper: `wasmtime4j-native/src/engine.rs:13,149`

### Execution Flow

```
Java: Engine.create()
  → Native: Box::new(Engine { inner: Arc::new(WasmtimeEngine) })  → Returns ptr A

Java: engine.compileModule()
  → Native: get_engine_ref(ptr A) → &Engine
  → Module::compile(engine, bytes)
    → WasmtimeModule::new(engine.inner(), bytes)  → Wasmtime clones Arc → Arc B
    → Returns Module { inner: Arc B }

Java: engine.createStore()
  → Native: get_engine_ref(ptr A) → &Engine
  → Store::new(engine)
    → WasmtimeStore::new(engine.inner(), data) → Wasmtime clones Arc → Arc C
    → Returns Store with Arc C

Java: module.instantiate(store)
  → Native: WasmtimeInstance::new(store, module, [])
    → Wasmtime checks: module.engine() == store.engine()
    → Compares: Arc B == Arc C → FALSE (different Arc pointers!)
    → ERROR: "cross-`Engine` instantiation is not currently supported"
```

### Proposed Fix

**Status**: PARTIALLY IMPLEMENTED (Commit 2ca37647)

**Phase 1 (COMPLETED)**: Store Engine reference in Module and Store wrappers

Modified `wasmtime4j-native/src/module.rs` and `wasmtime4j-native/src/store.rs`:

```rust
pub struct Module {
    inner: Arc<WasmtimeModule>,
    engine: Engine,  // ADDED: Keep reference to Engine wrapper
    metadata: ModuleMetadata,
    // ...
}

pub struct Store {
    inner: Arc<ReentrantLock<WasmtimeStore<StoreData>>>,
    engine: Engine,  // ADDED: Keep reference to Engine wrapper
    metadata: StoreMetadata,
    // ...
}
```

Both Module and Store now store the Engine, which contains `Arc<WasmtimeEngine>`.

**Phase 2 (PENDING)**: Use Module's engine for Store creation or implement alternative solution

The current fix is incomplete because even though Module and Store hold references to the same
wasmtime4j Engine wrapper (which shares `Arc<WasmtimeEngine>`), when `WasmtimeModule::new(engine.inner(), bytes)`
and `WasmtimeStore::new(engine.inner(), data)` are called, Wasmtime internally clones the Arc,
creating different Arc pointers.

Possible solutions:
1. Use `wasmtime::Module::engine()` method to get the Arc from Module and use it for Store creation
2. Create Store using the Module's engine via `Store::new(module.engine())`
3. Upgrade to a newer Wasmtime version if this issue was fixed
4. Implement a workaround in Instance creation to re-create Store if needed

### Workaround

Currently, there is no workaround. Tests that require Module instantiation will fail.

### Impact

- All TypedFunc tests fail (80 tests)
- Any integration tests that create instances will fail
- Basic functionality like loading and executing WASM modules is blocked

### Related Changes

- Commit 2357676e: Added TypedFunc tests (currently failing due to this issue)
- Commit 3ba49f8d: Added TypedFunc signatures (implementation complete, tests blocked)
- Java-side improvement in JniWasmRuntime.compileModule to delegate to Engine (correct design but insufficient to fix bug)

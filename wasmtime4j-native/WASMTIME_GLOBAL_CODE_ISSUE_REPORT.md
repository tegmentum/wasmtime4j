# Wasmtime GLOBAL_CODE Registry Scalability Issue

## Summary

When running a large test suite that creates many `wasmtime::Engine` and `wasmtime::Module` instances, wasmtime's internal `GLOBAL_CODE` registry accumulates entries that eventually cause process crashes (SIGABRT) after approximately 350-400 test runs in a single process.

## Environment

- **Wasmtime version**: 41.0.1
- **Platform**: macOS Darwin 24.5.0 (also observed on Linux)
- **Rust version**: stable
- **Configuration**: `signals_based_traps(false)` (required for JVM integration)

## Reproduction

### Minimal reproduction case

A test suite with ~860 tests, where many tests create engines and compile small WAT modules:

```rust
#[test]
fn test_example() {
    let engine = wasmtime::Engine::default();
    let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
    let module = wasmtime::Module::new(&engine, wat).unwrap();
    // ... use module ...
}
```

When running the full test suite with `cargo test --lib`, the process crashes with SIGABRT after approximately 350-400 tests.

### Observed behavior

1. **Individual tests pass**: Each test passes when run in isolation
2. **Batched tests pass**: Tests pass when run in module-specific batches (separate processes)
3. **Full suite crashes**: Running all tests in a single process causes SIGABRT

### Test results

| Run Mode | Tests Passed | Result |
|----------|--------------|--------|
| Individual modules | 863 total | All pass |
| Full suite (single-threaded) | ~350-400 | SIGABRT |
| Full suite (parallel) | ~340-360 | SIGABRT or SIGSEGV |

## Root Cause Analysis

Through detailed investigation of wasmtime 41.0.1 source code, we identified the exact mechanism causing the crashes.

### GLOBAL_CODE Registry Structure

The registry is located in `wasmtime/src/runtime/module/registry.rs`:

```rust
fn global_code() -> &'static RwLock<GlobalRegistry> {
    static GLOBAL_CODE: OnceLock<RwLock<GlobalRegistry>> = OnceLock::new();
    GLOBAL_CODE.get_or_init(Default::default)
}

type GlobalRegistry = BTreeMap<usize, (usize, Arc<CodeMemory>)>;
```

The registry is keyed by `address.end - 1` (the last byte of the text section), storing the start address and an `Arc<CodeMemory>`.

### Registration/Unregistration (`registry.rs:312-332`)

```rust
pub fn register_code(image: &Arc<CodeMemory>, address: Range<usize>) {
    if address.is_empty() { return; }
    let start = address.start;
    let end = address.end - 1;
    let prev = global_code().write().insert(end, (start, image.clone()));
    assert!(prev.is_none());  // ABORTS if duplicate key exists
}

pub fn unregister_code(address: Range<usize>) {
    if address.is_empty() { return; }
    let end = address.end - 1;
    let code = global_code().write().remove(&end);
    assert!(code.is_some());  // ABORTS if key not found
}
```

### EngineCode Lifecycle (`code.rs:127-258`)

```rust
impl EngineCode {
    pub fn new(mmap: Arc<CodeMemory>, ...) -> EngineCode {
        crate::module::register_code(&mmap, mmap.raw_addr_range());
        // ...
    }
}

impl Drop for EngineCode {
    fn drop(&mut self) {
        crate::module::unregister_code(self.original_code.raw_addr_range());
    }
}
```

### The Crash Mechanism

**Virtual Address Reuse + Arc Deferred Deallocation**

1. Each engine/module allocates code memory via mmap at a virtual address
2. When dropped, `EngineCode` may not be deallocated immediately if `Arc` references remain (from Modules, Stores, or Components still holding references)
3. The OS can reuse virtual address ranges from previously released mmaps
4. If a new engine allocates code at the same address range before an old `EngineCode`'s Arc is fully released, the `assert!(prev.is_none())` fails because the key is still registered

**Why ~350-400 Tests?**

This threshold likely corresponds to:
- Virtual address space patterns on macOS/Linux
- The point where mmap address reuse becomes statistically likely
- Accumulated Arc references that haven't been cleaned up by Rust's drop order

### StoreCode Private Copies (debug feature)

When `debug_guest` is enabled, `StoreCode::new()` creates private copies and registers them separately (`code.rs:329`):

```rust
crate::module::register_code(&engine_code.original_code, private_copy.raw_addr_range());
```

This adds additional registry entries that could contribute to the issue.

## Workarounds Attempted

1. **Shared engines within test modules**: Reduced engine creation from ~860 to ~60 (one per test module) using `Lazy<Engine>` statics. This improved the threshold from ~346 to ~389 tests.

2. **Pre-compiled shared modules**: For tests that run loops (e.g., stress tests), sharing a single engine and pre-compiled module instead of creating new ones in each iteration.

3. **Unique cache directories**: Ensuring test isolation for persistent storage tests.

4. **Batch execution**: Running tests in separate processes by module prefix (this works but is inconvenient).

## Impact

This issue affects any project using wasmtime that has a large test suite, particularly:

- Java/JVM integrations (where `signals_based_traps(false)` is required)
- Projects with extensive test coverage
- CI/CD pipelines that run full test suites

## Suggested Solutions

### For Wasmtime

1. **Convert assertions to recoverable errors**: Instead of `assert!()` which calls `abort()`, return a `Result<()>`:
   ```rust
   pub fn register_code(image: &Arc<CodeMemory>, address: Range<usize>) -> Result<(), RegistryError> {
       let prev = global_code().write().insert(end, (start, image.clone()));
       if prev.is_some() {
           return Err(RegistryError::DuplicateRegistration(end));
       }
       Ok(())
   }
   ```

2. **Check-before-insert pattern**: Before inserting, check if the key exists and if the existing entry is stale (Arc strong_count check):
   ```rust
   let mut registry = global_code().write();
   if let Some((_, existing)) = registry.get(&end) {
       if Arc::strong_count(existing) == 1 {
           // Stale entry, safe to replace
           registry.remove(&end);
       }
   }
   registry.insert(end, (start, image.clone()));
   ```

3. **Weak references for lookups**: Store `Weak<CodeMemory>` for trap handler lookups, with lazy cleanup of dead entries.

4. **Provide a testing API**: Add `Engine::force_gc()` or similar to trigger cleanup of unreferenced entries.

### For Users/Workarounds

1. **Share engines across tests**: Use `Lazy<Engine>` statics at module level
2. **Batch test execution**: Run tests in separate processes by module
3. **Explicit cleanup**: Drop engines and modules explicitly, then wait/yield before creating new ones

## Requested Investigation

1. **Is this expected behavior?** Is there a known limit to the number of engines/modules that can be created in a single process?

2. **Is the assert! intentional?** Could this be changed to a recoverable error for better integration with testing frameworks?

3. **Are there plans** for registry cleanup or compaction APIs?

4. **Are there best practices** for test suites that need to create many engines/modules?

## Reproduction Repository

The issue was discovered in wasmtime4j (Java bindings for wasmtime). A minimal reproduction case can be created by generating a test suite with ~400+ tests that each create an engine and compile a simple module.

## Additional Context

- We've confirmed `signals_based_traps(false)` is set on all engine configurations
- The issue occurs with both `Engine::new()` and `Engine::default()`
- Module compilation (not just engine creation) contributes to the accumulation
- The crash point varies slightly between runs but is consistently in the 340-400 test range

## Relevant Wasmtime Source Files

For reference, the key files involved are (paths relative to wasmtime 41.0.1):

- `src/runtime/module/registry.rs` - GLOBAL_CODE registry, register_code(), unregister_code()
- `src/runtime/code.rs` - EngineCode and StoreCode lifecycle, registration calls
- `src/runtime/code_memory.rs` - CodeMemory structure, raw_addr_range()
- `src/runtime/vm/mmap_vec.rs` - MmapVec memory allocation
- `src/runtime/vm/sys/unix/signals.rs` - Signal handler abort sources
- `src/runtime/vm/sys/unix/machports.rs` - macOS mach port handler abort sources

---

*Report prepared based on detailed investigation of wasmtime4j-native test suite failures and wasmtime 41.0.1 source code analysis*

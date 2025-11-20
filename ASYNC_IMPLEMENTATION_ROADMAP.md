# Async Operations Implementation Roadmap

**Status**: IN PROGRESS (40% Complete - Java API exists, native integration needed)
**Estimated Effort**: 1-2 weeks
**Last Updated**: 2025-11-19

## Executive Summary

Wasmtime4j has partially implemented async operations infrastructure, but the critical native integration with Wasmtime's async APIs is incomplete. The Java async API layer exists (`AsyncEngineConfig`, `AsyncHostFunction`, `AsyncFunctionCall`), and a Tokio runtime is initialized in Rust, but they are not connected to Wasmtime's async execution model.

### What Exists (40% Complete)

**Java Layer - ✅ COMPLETE:**
- `AsyncEngineConfig` - Configuration for async compilation (wasmtime4j/src/main/java/ai/tegmentum/wasmtime4j/async/)
- `AsyncHostFunction` - CompletableFuture-based async host function interface
- `AsyncFunctionCall` - Encapsulates async function call metadata
- `AsyncExecutionContext` - Execution context management
- `AsyncFunctionStatistics`, `CompilationStatistics` - Metrics tracking
- `EngineConfig.asyncSupport(boolean)` - Java configuration flag (added 2025-11-19)
- `EngineConfig.asyncStackSize(long)` - Stack size configuration

**Rust Layer - ⚠️ PARTIAL:**
- Global Tokio runtime initialized (`async_runtime.rs`)
- `AsyncOperation`, `AsyncOperationStatus`, `AsyncOperationType` structures
- C API stubs for `wasmtime4j_func_call_async`, `wasmtime4j_module_compile_async`
- **BLOCKED**: Send trait issues with callbacks prevent full implementation

### What Needs Implementation (60% Remaining)

**Critical Blockers:**
1. **Fix Send trait issues** in `async_runtime.rs` (lines 237-285)
   - Current: Stubbed out due to callback threading issues
   - Required: Safe callback dispatch from async context to Java

2. **Connect EngineConfig to Wasmtime**
   - `JniWasmRuntime.createEngine(EngineConfig)` is stubbed (line 177-184)
   - Need native method to pass config to Rust
   - Apply `Config::async_support()` in engine creation

**Core Async APIs:**
3. **Func::new_async binding**
   - Create JNI wrapper for `Func::new_async`
   - Handle async host function callbacks from WASM to Java
   - Integrate with CompletableFuture API

4. **Func::call_async binding**
   - Create JNI wrapper for `Func::call_async`
   - Return CompletableFuture to Java
   - Handle timeout and cancellation

5. **Store async methods**
   - `Store::epoch_deadline_async_yield_and_update`
   - `Store::call_hook_async`
   - `Store::gc_async`
   - `Store::limiter_async`

**WASI Async Integration:**
6. **Connect wasmtime-wasi async**
   - Use `add_to_linker_async` instead of `add_to_linker_sync`
   - Configure Tokio executor for WASI operations
   - Handle async file I/O, networking, etc.

**Testing:**
7. **Comprehensive async tests**
   - Async function calls with timeout
   - Async host function creation
   - Async WASI operations
   - Cancellation and error handling

## Implementation Plan

### Phase 1: Fix Foundation (3-4 days)

**Day 1-2: Resolve Send Trait Issues**
- Study callback safety requirements for JNI + Tokio
- Implement thread-safe callback dispatch mechanism
- Options:
  - Use `jni::JavaVM` for cross-thread JNI access
  - Queue callbacks to be processed on main thread
  - Use `Arc<Mutex<>>` for callback state

**Day 3-4: Connect EngineConfig**
- Create `nativeCreateEngineWithConfig` JNI method
- Pass async configuration to Rust
- Implement `Config::async_support(bool)` in engine creation
- Implement `Config::async_stack_size(usize)` configuration
- Test engine creation with async enabled

### Phase 2: Core Async APIs (4-5 days)

**Day 5-6: Func::new_async**
```rust
// Target implementation sketch
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeNewAsync(
    mut env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    func_type_handle: jlong,
    callback_obj: JObject,
) -> jlong {
    // Get JavaVM for cross-thread access
    let jvm = env.get_java_vm()?;

    // Create global ref to callback
    let callback_global = env.new_global_ref(callback_obj)?;

    // Create async host function
    let func = Func::new_async(
        &mut store,
        func_type,
        move |mut caller, params, results| {
            Box::new(async move {
                // Attach to JVM from async context
                let mut env = jvm.attach_current_thread()?;

                // Call Java callback
                // ... invoke callback method ...

                Ok(())
            })
        }
    );

    Ok(func_ptr as jlong)
}
```

**Day 7-8: Func::call_async**
```rust
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCallAsync(
    mut env: JNIEnv,
    _class: JClass,
    func_handle: jlong,
    store_handle: jlong,
    args: jarray,
) -> jobject {
    // Create CompletableFuture in Java
    let future = env.new_object("java/util/concurrent/CompletableFuture", "()V", &[])?;
    let future_global = env.new_global_ref(future)?;

    // Spawn async task
    let jvm = env.get_java_vm()?;
    get_runtime_handle().spawn(async move {
        match func.call_async(&mut store, params, results).await {
            Ok(_) => {
                // Complete future with results
                let mut env = jvm.attach_current_thread()?;
                env.call_method(future_global, "complete", "(Ljava/lang/Object;)Z", &[results.into()])?;
            }
            Err(e) => {
                // Complete exceptionally
                let mut env = jvm.attach_current_thread()?;
                env.call_method(future_global, "completeExceptionally", "(Ljava/lang/Throwable;)Z", &[exception.into()])?;
            }
        }
    });

    Ok(future.into_raw())
}
```

**Day 9: Store Async Methods**
- Implement `epoch_deadline_async_yield_and_update`
- Implement `call_hook_async`
- Test cooperative yielding

### Phase 3: WASI Async (2-3 days)

**Day 10-11: WASI Async Integration**
```rust
// In wasi_preview2.rs or similar
pub fn add_wasi_async_to_linker(linker: &mut Linker<T>, wasi_ctx: &WasiCtx) -> Result<()> {
    // Use async WASI instead of sync
    wasmtime_wasi::add_to_linker_async(linker)?;

    // Configure Tokio executor
    linker.func_wrap_async(
        "wasi_snapshot_preview1",
        "fd_read",
        |mut caller: Caller<'_, T>, fd: i32, iovs_ptr: i32, iovs_len: i32, nread_ptr: i32| {
            Box::new(async move {
                // Async file read using Tokio
                let bytes = tokio::fs::read(...).await?;
                Ok(bytes.len() as i32)
            })
        }
    )?;

    Ok(())
}
```

**Day 12: Connect Java WasiContext to Async**
- Modify `JniWasiContext` to support async operations
- Add async file I/O tests
- Verify non-blocking behavior

### Phase 4: Testing & Polish (2-3 days)

**Day 13-14: Comprehensive Tests**
- Async function call tests with various timeouts
- Async host function creation tests
- WASI async I/O tests
- Cancellation tests
- Error handling tests
- Performance benchmarks

**Day 15: Documentation & Examples**
- Update JavaDoc for all async APIs
- Create async usage examples
- Document async configuration options
- Update main IMPLEMENTATION_ROADMAP.md

## Technical Challenges & Solutions

### Challenge 1: JNI Thread Safety with Tokio

**Problem**: JNI `JNIEnv` is not Send, but Tokio tasks require Send.

**Solutions**:
1. **JavaVM Approach** (Recommended):
   ```rust
   let jvm = env.get_java_vm()?;
   tokio::spawn(async move {
       let mut env = jvm.attach_current_thread()?;
       // Now we have JNIEnv in async context
   });
   ```

2. **Channel-Based Callback Queue**:
   ```rust
   static CALLBACK_QUEUE: Lazy<Mutex<Vec<Callback>>> = ...;
   // Async task queues callback
   // Main thread processes queue periodically
   ```

### Challenge 2: CompletableFuture Integration

**Problem**: Bridge Rust Future with Java CompletableFuture.

**Solution**:
```rust
// Create CompletableFuture in Java
let future = env.new_object("java/util/concurrent/CompletableFuture", ...)?;

// Spawn Rust async task
tokio::spawn(async move {
    match async_operation().await {
        Ok(result) => future.complete(result),
        Err(e) => future.completeExceptionally(e),
    }
});

return future; // Return to Java immediately
```

### Challenge 3: Wasmtime Async Store Requirements

**Problem**: `call_async` requires `Store<T>` where `T: Send`.

**Solution**:
- Ensure store data type implements Send
- Use Arc<Mutex<>> for shared mutable state
- Verify all callbacks and context are Send-safe

## Testing Strategy

**Unit Tests:**
- Async function creation and invocation
- Timeout handling
- Cancellation
- Error propagation

**Integration Tests:**
- Async WASI file operations
- Async network I/O (when supported)
- Multiple concurrent async calls
- Async callback from WASM to Java

**Performance Tests:**
- Async overhead vs sync
- Tokio task spawning cost
- Callback dispatch latency
- Memory usage with many async operations

## Dependencies

**Rust Crates:**
- `tokio` = { version = "1", features = ["full"] } ✅ Already added
- `wasmtime` = { version = "38", features = ["async"] } ⚠️ Need to verify async feature enabled
- `wasmtime-wasi` = { version = "38", features = ["tokio"] }

**Java:**
- CompletableFuture (Java 8+) ✅ Available
- Executor framework ✅ Available

## Success Criteria

- [✅] Java async API complete
- [✅] EngineConfig.asyncSupport() configuration added
- [ ] Tokio runtime connected to Wasmtime async APIs
- [ ] Func::new_async and Func::call_async working
- [ ] WASI async operations non-blocking
- [ ] All tests passing
- [ ] Zero JVM crashes
- [ ] Documentation complete

## Notes

- This roadmap assumes familiarity with both JNI and Tokio
- Send trait issues are the primary blocker
- Full implementation requires ~10-15 days of focused work
- Consider starting with simple async host function support before tackling WASI async
- Performance testing should validate that async actually improves throughput

## References

- [Wasmtime Async Docs](https://docs.wasmtime.dev/api/wasmtime/struct.Config.html#method.async_support)
- [JNI and Threads](https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/invocation.html)
- [Tokio Rust Runtime](https://tokio.rs/)
- [wasmtime-wasi async](https://docs.rs/wasmtime-wasi/latest/wasmtime_wasi/)

# Thread Function Execution Implementation Guide

**Status**: Design document for final 5% of thread support
**Created**: 2025-11-19
**Complexity**: Medium-High (architectural challenge with Wasmtime's Store model)

## Problem Statement

The `nativeExecuteFunction` method in `jni_thread_bindings.rs` is currently a placeholder because Wasmtime's `Func` type is Store-bound and cannot be safely passed between threads. This document outlines the architectural challenge and provides detailed implementation options.

## Background

### Current State

**What Works:**
- ✅ All thread lifecycle operations (spawn, join, terminate, state queries)
- ✅ Thread-local storage
- ✅ Atomic operations on shared memory (15 operations)
- ✅ Thread statistics and monitoring
- ✅ JNI bindings for all non-execution operations

**What's Missing:**
- ❌ Actual WASM function execution on threads via `WasmThread.executeFunction()`

### Architectural Challenge

Wasmtime's design principles:
1. `Func` is bound to a `Store`
2. `Store` is not thread-safe and cannot be shared between threads
3. `Func` cannot outlive its `Store`
4. Each thread needs its own `Store` to execute WASM code

Current implementation:
```java
// Java
Future<WasmValue[]> executeFunction(WasmFunction function, WasmValue... args)
```

```rust
// Rust - WasmThread::execute_function
pub fn execute_function<F, R>(&self, func: F) -> WasmtimeResult<R>
where
    F: FnOnce() -> WasmtimeResult<R> + Send + 'static,
    R: Send + 'static
```

The WasmThread expects a closure, not a Wasmtime `Func`. We need to bridge these two models.

## Solution Options

### Option 1: Thread-Local Store Approach (RECOMMENDED)

**Concept:** Each thread creates its own Store and Instance from a shared Module.

**Architecture:**
```
Java Thread
    ↓
JNI nativeExecuteFunction(threadHandle, moduleHandle, functionName, args)
    ↓
Rust WasmThread
    ↓
Thread-local Store creation
    ↓
Module instantiation in thread context
    ↓
Function lookup by name
    ↓
Function execution with thread-local Store
    ↓
Results returned to Java
```

**Advantages:**
- ✅ No Store sharing, fully thread-safe
- ✅ Each thread has independent execution context
- ✅ Aligns with Wasmtime's design principles
- ✅ Best performance for parallel execution

**Disadvantages:**
- ❌ Requires Module instantiation per thread (memory overhead)
- ❌ Cannot share instance state between threads easily
- ❌ More complex API (needs Module + function name instead of just Func)

**Implementation Steps:**

1. **Modify Java JniWasmThread API:**
```java
// Change from:
private static native byte[] nativeExecuteFunction(
    long threadHandle,
    long functionHandle,  // <- Func handle
    byte[] serializedArgs);

// To:
private static native byte[] nativeExecuteFunction(
    long threadHandle,
    long moduleHandle,     // <- Module handle
    String functionName,   // <- Function name
    byte[] serializedArgs);
```

2. **Update JniFunction to provide Module access:**
```java
public class JniFunction implements WasmFunction {
    private final long moduleHandle;  // Add this
    private final String functionName; // Add this

    public long getModuleHandle() { return moduleHandle; }
    public String getName() { return functionName; }
}
```

3. **Implement thread-local execution in Rust:**
```rust
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeExecuteFunction(
    mut env: JNIEnv,
    _class: JClass,
    thread_handle: jlong,
    module_handle: jlong,
    function_name: JString,
    serialized_args: JByteArray,
) -> jbyteArray {
    jni_utils::jni_try_object(&mut env, |env_ref| {
        // Get thread and module references
        let thread = unsafe { get_thread_ref(thread_handle as *const c_void)? };
        let module = unsafe { get_module_ref(module_handle as *const c_void)? };

        // Get function name
        let func_name: String = env_ref.get_string(&function_name)?
            .into();

        // Deserialize arguments
        let args_bytes = env_ref.convert_byte_array(&serialized_args)?;
        let args: Vec<Val> = deserialize_values(&args_bytes)?;

        // Execute in thread context with thread-local Store
        let result = thread.execute_function(|| {
            // Create thread-local Store
            let engine = module.engine();
            let mut store = Store::new(engine, ());

            // Instantiate module in this thread's context
            let instance = Instance::new(&mut store, module, &[])?;

            // Look up function by name
            let func = instance.get_func(&mut store, &func_name)
                .ok_or_else(|| WasmtimeError::Function {
                    message: format!("Function '{}' not found", func_name)
                })?;

            // Prepare results buffer
            let func_type = func.ty(&store);
            let mut results = vec![Val::I32(0); func_type.results().len()];

            // Execute function
            func.call(&mut store, &args, &mut results)?;

            Ok(results)
        })?;

        // Serialize results
        let result_bytes = serialize_values(&result)?;
        let result_array = env_ref.byte_array_from_slice(&result_bytes)?;

        Ok(unsafe { jni::objects::JObject::from_raw(result_array.as_raw()) })
    })
}
```

4. **Implement value serialization:**
```rust
fn serialize_values(values: &[Val]) -> WasmtimeResult<Vec<u8>> {
    // Use bincode or similar for serialization
    // Format: [type_tag: u8, value_bytes]
    // Example:
    // I32(42) -> [0x01, 0x2A, 0x00, 0x00, 0x00]
    // I64(100) -> [0x02, 0x64, 0x00, ..., 0x00]
    // F32(3.14) -> [0x03, <IEEE 754 bytes>]
    // F64(2.71) -> [0x04, <IEEE 754 bytes>]
    todo!("Implement value serialization")
}

fn deserialize_values(bytes: &[u8]) -> WasmtimeResult<Vec<Val>> {
    // Reverse of serialize_values
    todo!("Implement value deserialization")
}
```

**Estimated Effort:** 1-2 days

### Option 2: Shared Store with Synchronization

**Concept:** Use `Arc<Mutex<Store>>` to share a single Store between threads.

**Advantages:**
- ✅ Simpler API (can keep existing function handle approach)
- ✅ Shared instance state
- ✅ Less memory overhead

**Disadvantages:**
- ❌ Serializes all function execution (defeats purpose of threading)
- ❌ Lock contention under high concurrency
- ❌ Potential for deadlocks
- ❌ Not recommended by Wasmtime team

**Not Recommended** - Defeats the purpose of threading.

### Option 3: Message Passing

**Concept:** Threads send function call requests to a central executor via channels.

**Advantages:**
- ✅ Avoids Store threading issues completely
- ✅ Simple synchronization model
- ✅ Easy to implement

**Disadvantages:**
- ❌ Serializes execution (defeats threading purpose)
- ❌ Additional latency from message passing
- ❌ Doesn't scale with thread count

**Not Recommended** - Serializes execution.

## Recommended Implementation Path

**Phase 1: Value Serialization (Day 1 - 4 hours)**
- Implement `serialize_values()` and `deserialize_values()`
- Support I32, I64, F32, F64, V128 types
- Add unit tests for round-trip serialization

**Phase 2: API Changes (Day 1 - 4 hours)**
- Update Java `JniFunction` to expose Module handle and function name
- Modify `JniWasmThread.executeFunction()` to pass Module + name
- Update native method signature

**Phase 3: Thread Execution (Day 2 - 6 hours)**
- Implement thread-local Store creation
- Implement Module instantiation in thread context
- Implement function lookup and execution
- Add comprehensive error handling

**Phase 4: Testing (Day 2 - 2 hours)**
- Write integration tests for thread function execution
- Test with multiple threads executing same function
- Test with different WASM modules
- Verify memory isolation between threads

**Total Estimated Time:** 1.5-2 days

## Testing Strategy

### Unit Tests
```rust
#[test]
fn test_value_serialization_roundtrip() {
    let values = vec![
        Val::I32(42),
        Val::I64(100),
        Val::F32(3.14_f32.to_bits()),
        Val::F64(2.71_f64.to_bits()),
    ];

    let serialized = serialize_values(&values).unwrap();
    let deserialized = deserialize_values(&serialized).unwrap();

    assert_eq!(values.len(), deserialized.len());
    // Compare each value...
}
```

### Integration Tests
```java
@Test
public void testThreadFunctionExecution() throws Exception {
    // Load simple WASM module with add function
    byte[] wasm = loadWasmModule("add.wasm");
    Module module = runtime.createModule(wasm);

    // Create shared memory for threads
    WasmMemory sharedMemory = runtime.createSharedMemory(1, 10);

    // Create thread
    WasmThread thread = runtime.createThread(threadId, sharedMemory);

    // Get function
    Instance instance = runtime.createInstance(module);
    WasmFunction addFunc = instance.getFunction("add");

    // Execute on thread
    Future<WasmValue[]> result = thread.executeFunction(
        addFunc,
        WasmValue.i32(10),
        WasmValue.i32(32)
    );

    // Verify result
    WasmValue[] values = result.get(5, TimeUnit.SECONDS);
    assertEquals(1, values.length);
    assertEquals(42, values[0].asI32());
}
```

## Performance Considerations

### Memory Overhead
- Each thread creates a new Store and Instance
- Estimate: ~100KB per thread for simple modules
- For 100 threads: ~10MB overhead
- Acceptable for most use cases

### Execution Performance
- No lock contention (each thread has own Store)
- True parallel execution
- Performance scales linearly with CPU cores

### Optimization Opportunities
1. **Instance Pooling:** Reuse Instances across function calls
2. **Store Pooling:** Reuse Stores within a thread
3. **Lazy Initialization:** Only create Store when needed

## References

- [Wasmtime Store Documentation](https://docs.wasmtime.dev/api/wasmtime/struct.Store.html)
- [Wasmtime Threading Discussion](https://github.com/bytecodealliance/wasmtime/discussions/3884)
- [WebAssembly Threads Proposal](https://github.com/WebAssembly/threads)

## Status

- ✅ Design complete
- ✅ Architecture reviewed
- ⏳ Implementation pending
- ⏳ Testing pending

Next step: Begin Phase 1 (Value Serialization)

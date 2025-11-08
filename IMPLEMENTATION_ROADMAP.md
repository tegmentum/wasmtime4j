# Wasmtime4j API Implementation Roadmap

This document outlines the implementation plan for completing high-priority Wasmtime API coverage gaps.

## Overall Coverage Status
- **Current**: 75-80% API coverage
- **Target**: 90%+ coverage for production readiness

---

## HIGH PRIORITY IMPLEMENTATIONS

### 1. Engine Configuration Queries ⭐⭐⭐
**Status**: Partially implemented (stubs return defaults)
**Impact**: High - Essential for production deployments
**Effort**: Medium (2-3 days)

#### Required Changes:

##### Java API (JniEngine.java)
Currently returns hardcoded values. Need to call native methods:

```java
@Override
public boolean isEpochInterruptionEnabled() {
    if (closed || nativeHandle == 0) return false;
    return nativeIsEpochInterruptionEnabled(nativeHandle);
}

@Override
public boolean isFuelEnabled() {
    if (closed || nativeHandle == 0) return false;
    return nativeIsFuelEnabled(nativeHandle);
}

@Override
public long getStackSizeLimit() {
    if (closed || nativeHandle == 0) return 0;
    return nativeGetStackSizeLimit(nativeHandle);
}

@Override
public int getMemoryLimitPages() {
    if (closed || nativeHandle == 0) return 0;
    return nativeGetMemoryLimitPages(nativeHandle);
}

@Override
public boolean supportsFeature(WasmFeature feature) {
    if (feature == null || closed || nativeHandle == 0) return false;
    return nativeSupportsFeature(nativeHandle, feature.name());
}

// Native method declarations
private static native boolean nativeIsEpochInterruptionEnabled(long engineHandle);
private static native boolean nativeIsFuelEnabled(long engineHandle);
private static native long nativeGetStackSizeLimit(long engineHandle);
private static native int nativeGetMemoryLimitPages(long engineHandle);
private static native boolean nativeSupportsFeature(long engineHandle, String featureName);
```

##### Rust Native Implementation (jni_bindings.rs)
Add to `jni_engine` module:

```rust
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsEpochInterruptionEnabled(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jboolean {
    jni_utils::jni_try(&mut env, || {
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_handle as *const c_void)? };
        // Query engine config
        Ok(engine.config().epoch_interruption)
    }).unwrap_or(0) as jboolean
}

// Similar implementations for:
// - nativeIsFuelEnabled
// - nativeGetStackSizeLimit
// - nativeGetMemoryLimitPages
// - nativeSupportsFeature
```

**Note**: May need to store EngineConfig alongside Engine in native code to enable queries.

---

### 2. SIMD Java API Wrapper ⭐⭐⭐
**Status**: Native methods declared, no Java API
**Impact**: High - Critical for performance-sensitive applications
**Effort**: High (4-5 days)

#### Architecture:
Create a new `SimdOperations` class that wraps native SIMD calls:

```java
package ai.tegmentum.wasmtime4j.simd;

public final class SimdOperations {
    private final long storeHandle;

    public SimdOperations(Store store) {
        this.storeHandle = ((JniStore) store).getNativeHandle();
    }

    // Vector operations
    public byte[] add(byte[] a, byte[] b, SimdLane lane) {
        return nativeSimdAdd(storeHandle, a, b, lane.ordinal());
    }

    public byte[] subtract(byte[] a, byte[] b, SimdLane lane) {
        return nativeSimdSubtract(storeHandle, a, b, lane.ordinal());
    }

    public byte[] multiply(byte[] a, byte[] b, SimdLane lane) {
        return nativeSimdMultiply(storeHandle, a, b, lane.ordinal());
    }

    // Load/Store operations
    public byte[] load(Memory memory, int offset, SimdLane lane) {
        long memHandle = ((JniMemory) memory).getNativeHandle();
        return nativeSimdLoad(storeHandle, memHandle, offset, lane.ordinal());
    }

    public void store(Memory memory, int offset, byte[] value, SimdLane lane) {
        long memHandle = ((JniMemory) memory).getNativeHandle();
        nativeSimdStore(storeHandle, memHandle, offset, value, lane.ordinal());
    }

    // Shuffle/splat operations
    public byte[] shuffle(byte[] a, byte[] b, int[] indices) {
        return nativeSimdShuffle(storeHandle, a, b, indices);
    }

    public byte[] splat(int value, SimdLane lane) {
        return nativeSimdSplat(storeHandle, value, lane.ordinal());
    }

    // Native declarations (already exist in JniWasmRuntime)
    private static native byte[] nativeSimdAdd(long storeHandle, byte[] a, byte[] b, int lane);
    // ... etc
}

public enum SimdLane {
    I8X16,   // 16x 8-bit integers
    I16X8,   // 8x 16-bit integers
    I32X4,   // 4x 32-bit integers
    I64X2,   // 2x 64-bit integers
    F32X4,   // 4x 32-bit floats
    F64X2    // 2x 64-bit floats
}
```

#### Required Native Implementation:
The native methods are declared but need full implementation in `jni_bindings.rs`:

```rust
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdAdd(
    mut env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    a: jbyteArray,
    b: jbyteArray,
    lane: jint,
) -> jbyteArray {
    jni_utils::jni_try(&mut env, |env| {
        let store = unsafe { get_store_ref(store_handle as *const c_void)? };

        // Convert Java arrays to Rust
        let a_vec = env.convert_byte_array(a)?;
        let b_vec = env.convert_byte_array(b)?;

        // Perform SIMD operation based on lane type
        let result = match lane {
            0 => simd_add_i8x16(&a_vec, &b_vec),
            1 => simd_add_i16x8(&a_vec, &b_vec),
            2 => simd_add_i32x4(&a_vec, &b_vec),
            3 => simd_add_i64x2(&a_vec, &b_vec),
            4 => simd_add_f32x4(&a_vec, &b_vec),
            5 => simd_add_f64x2(&a_vec, &b_vec),
            _ => return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid SIMD lane: {}", lane)
            }),
        };

        // Convert result back to Java array
        let result_array = env.new_byte_array(result.len() as i32)?;
        env.set_byte_array_region(result_array, 0, &result)?;
        Ok(result_array.into_raw())
    }).unwrap_or(std::ptr::null_mut())
}
```

---

### 3. Streaming Compilation Infrastructure ⭐⭐
**Status**: Interfaces defined, implementation missing
**Impact**: Medium - Important for large modules
**Effort**: Very High (1-2 weeks)

#### Current State:
- `StreamingCompiler` interface exists
- `StreamingInstantiator` interface exists
- JniEngine.createStreamingCompiler() throws UnsupportedOperationException

#### Implementation Requirements:

##### Phase 1: Basic Streaming Compilation
```java
public class JniStreamingCompiler implements StreamingCompiler {
    private final Engine engine;
    private final ByteBuffer accumulator;
    private long nativeStreamHandle;

    @Override
    public void feedBytes(byte[] chunk) throws WasmException {
        if (nativeStreamHandle == 0) {
            nativeStreamHandle = nativeCreateStream(engineHandle);
        }
        nativeFeedChunk(nativeStreamHandle, chunk);
    }

    @Override
    public boolean isComplete() {
        return nativeIsComplete(nativeStreamHandle);
    }

    @Override
    public Module finish() throws WasmException {
        long moduleHandle = nativeFinishStream(nativeStreamHandle);
        return new JniModule(moduleHandle, engine);
    }
}
```

##### Phase 2: Streaming Instantiation
```java
public class JniStreamingInstantiator implements StreamingInstantiator {
    private final Store store;
    private final StreamingCompiler compiler;

    @Override
    public Instance instantiate(ImportMap imports) throws WasmException {
        Module module = compiler.finish();
        return module.instantiate(store, imports);
    }
}
```

##### Native Implementation:
Requires Wasmtime's streaming compilation API:

```rust
// In jni_bindings.rs - jni_engine module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStreamingCompiler_nativeCreateStream(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe { get_engine_ref(engine_handle as *const c_void)? };

        // Create streaming module compiler
        let compiler = StreamingModule::new(engine)?;

        Ok(Box::into_raw(Box::new(compiler)) as *mut c_void)
    }) as jlong
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStreamingCompiler_nativeFeedChunk(
    mut env: JNIEnv,
    _class: JClass,
    stream_handle: jlong,
    chunk: jbyteArray,
) {
    jni_utils::jni_try(&mut env, |env| {
        let compiler = unsafe { get_streaming_compiler_mut(stream_handle as *mut c_void)? };
        let chunk_vec = env.convert_byte_array(chunk)?;

        compiler.feed_bytes(&chunk_vec)?;
        Ok(())
    });
}
```

**Complexity**: Requires understanding Wasmtime's incremental compilation API and managing streaming state.

---

## MEDIUM PRIORITY IMPLEMENTATIONS

### 4. Linker Advanced Features
**Effort**: Medium (3-4 days)

#### resolveDependencies()
```java
@Override
public List<String> resolveDependencies(Module... modules) throws WasmException {
    // Build dependency graph
    Map<String, Set<String>> imports = new HashMap<>();
    Map<String, Set<String>> exports = new HashMap<>();

    // Analyze each module's imports/exports
    for (Module module : modules) {
        // Use module.getImports() and module.getExports()
    }

    // Topological sort to determine link order
    return performTopologicalSort(imports, exports);
}
```

#### createInstantiationPlan()
```java
@Override
public InstantiationPlan createInstantiationPlan(Module... modules) {
    List<String> linkOrder = resolveDependencies(modules);
    return new InstantiationPlan(linkOrder, modules);
}
```

### 5. Memory64 Support
**Effort**: Medium-High (4-5 days)

Requires native Wasmtime memory64 support and extending Memory API:
- Memory64 address space handling
- Extended load/store operations
- 64-bit memory growth

### 6. Full Debugging APIs
**Effort**: High (5-7 days)

Complete the stub implementations in JniDebugger:
- Call stack introspection (nativeGetCallStack implementation)
- Variable inspection (nativeGetLocalVariables implementation)
- Breakpoint management
- Step execution

---

## LOW PRIORITY / FUTURE WORK

### 7. Async Operations
**Effort**: Very High (2-3 weeks)

Requires:
- Async WASI implementation
- Future/Promise-based Java API
- Tokio integration in native layer

### 8. Full Thread Support
**Effort**: Very High (2-3 weeks)

Requires:
- Wasm threads proposal implementation
- Shared memory support
- Atomic operations
- Thread synchronization primitives

---

## IMPLEMENTATION ORDER RECOMMENDATION

### Sprint 1 (Week 1-2):
1. ✅ Engine configuration queries - **COMPLETE**
2. 🔄 SIMD Java API wrapper - **IN PROGRESS**

### Sprint 2 (Week 3-4):
3. Linker advanced features (resolveDependencies, InstantiationPlan)
4. Streaming compilation (basic implementation)

### Sprint 3 (Week 5-6):
5. Full debugging APIs (call stack, variables)
6. Memory64 support

### Sprint 4 (Week 7+):
7. Async operations
8. Full thread support

---

## TESTING STRATEGY

For each implementation:
1. Unit tests for Java API
2. Integration tests with real WASM modules
3. Performance benchmarks (especially for SIMD)
4. Cross-platform testing (Linux, macOS, Windows)
5. JNI vs Panama parity tests

---

## DEPENDENCIES

### External:
- Wasmtime 38.0.3 (current)
- Consider upgrading to latest Wasmtime for newer features

### Internal:
- Native library build infrastructure
- JNI binding generator
- Panama FFI layer

---

## NOTES

- All implementations should maintain JNI and Panama parity
- Follow defensive programming patterns (null checks, validation)
- Use conventional commit format
- Update JavaDoc for all public APIs
- Add @since tags for new features

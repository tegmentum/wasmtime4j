# Wasmtime4j API Implementation Roadmap

This document outlines the implementation plan for completing high-priority Wasmtime API coverage gaps.

## Executive Summary

**Last Updated**: 2025-11-19

The wasmtime4j project has achieved significantly more progress than previously documented. A comprehensive audit revealed that **most Java APIs are complete**, with remaining work focused primarily on native Rust implementations.

### Overall Coverage Status
- **Current**: 85-90% API coverage (Java layer nearly complete)
- **Target**: 90%+ coverage for production readiness
- **Key Finding**: Most remaining work is native implementation, not Java API

### What's Actually Complete
- ✅ **SIMD Operations**: Fully implemented (Java + native)
- ✅ **Linker Advanced Features**: Fully implemented (resolveDependencies, createInstantiationPlan)
- ✅ **Component Model**: JNI at 100% parity with Panama for primitive types
- ✅ **GC Implementation**: Comprehensive support in both JNI and Panama
- ✅ **Engine Configuration Queries**: Fully implemented (Java + native)

### What Needs Implementation
- ❌ **Debugging APIs**: Not implemented (needs both Java and native)

---

## HIGH PRIORITY IMPLEMENTATIONS

### 1. Engine Configuration Queries ⭐⭐⭐
**Status**: ✅ FULLY COMPLETE (Java API + native implementation)
**Impact**: High - Essential for production deployments
**Effort**: N/A - Already implemented

#### Implementation Status:

##### Java API (JniEngine.java) - ✅ COMPLETE
Native method declarations already exist and properly call native layer:

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

##### Rust Native Implementation (jni_bindings.rs) - ✅ COMPLETE
Implemented in `jni_engine` module (jni_bindings.rs:951-1018):

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
**Status**: ✅ COMPLETE (Both Java API and native implementation)
**Impact**: High - Critical for performance-sensitive applications
**Effort**: N/A - Already implemented

#### Implementation Summary:
SIMD operations are fully implemented in JniWasmRuntime.java and PanamaWasmRuntime.java with complete native backing:
- ✅ Vector arithmetic operations (add, subtract, multiply)
- ✅ Load/store operations
- ✅ Shuffle and splat operations
- ✅ All lane types (I8X16, I16X8, I32X4, I64X2, F32X4, F64X2)
- ✅ Native Rust implementations complete

Original design (not needed - already implemented):

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

## MEDIUM PRIORITY IMPLEMENTATIONS

### 4. Linker Advanced Features
**Status**: ✅ COMPLETE (Both JniLinker.java and PanamaLinker.java)
**Impact**: Medium - Enables module composition
**Effort**: N/A - Already implemented

#### Implementation Summary:
- ✅ `resolveDependencies()` - Fully implemented with dependency graph analysis (wasmtime4j-jni/src/main/java/ai/tegmentum/wasmtime4j/jni/JniLinker.java:213)
- ✅ `createInstantiationPlan()` - Fully implemented with topological sorting (wasmtime4j-jni/src/main/java/ai/tegmentum/wasmtime4j/jni/JniLinker.java:258)
- ✅ Panama parity achieved

Reference implementation:

### 5. Memory64 Support
**Status**: ✅ COMPLETE
**Impact**: Medium - Enables >4GB memory spaces
**Effort**: N/A - Already implemented

#### What's Implemented:
- ✅ **Java API** - `WasmMemory.supports64BitAddressing()` and all 64-bit methods already exist
- ✅ **EngineConfig** - `isWasmMemory64()` flag for enabling Memory64 proposal
- ✅ **JNI Implementation** - Native `nativeSupports64BitAddressing()` binding complete
- ✅ **64-bit Operations** - All 64-bit memory methods implemented (getSize64(), grow64(), etc.)
- ✅ **Panama** - Uses default implementation (returns false, awaiting full Panama support)

**Note**: Memory64 is part of WebAssembly 3.0 (standardized September 2025). The Java API was already complete - only the JNI native query method needed implementation.

### 6. Debugging APIs
**Status**: ⚠️ PARTIALLY IMPLEMENTED (Limited by Wasmtime capabilities)
**Impact**: Medium - Important for development and debugging
**Effort**: N/A for remaining features (blocked on Wasmtime)

#### What's Implemented:
- ✅ **Backtrace capture** - `WasmBacktrace`, `FrameInfo`, `FrameSymbol` (JNI complete, Panama stubs)
- ✅ **DWARF debug info** - `EngineConfig.debugInfo()` for native debugger support
- ✅ **Guest debug config** - `EngineConfig.guestDebug()` for VM-level debugging instrumentation

#### Wasmtime Limitations (Not Implementable):
- ❌ **Breakpoint management** (set, remove, list) - Not yet available in Wasmtime
- ❌ **Step execution** (stepInto, stepOver, stepOut) - Not yet available in Wasmtime
- ❌ **Interactive debugger session** - Planned for future Wasmtime versions via Debug Adapter Protocol (DAP)
- ⚠️ **DebugFrameCursor API** - Available but only from hostcall context, limited use case

#### Current Capabilities:
1. **Post-mortem debugging**: Capture and inspect backtraces after errors
2. **Native debugger integration**: DWARF info for gdb/lldb debugging
3. **VM-level debugging**: Instrument code for accurate state tracking in hostcalls

**Note**: Full interactive debugging (breakpoints, stepping) requires Wasmtime DAP support, currently in development. See [Wasmtime Issue #5537](https://github.com/bytecodealliance/wasmtime/issues/5537) for status.

---

## LOW PRIORITY / FUTURE WORK

### 7. Async Operations ✅ COMPLETE (100% - Sprint 4)
**Status**: Fully implemented and production-ready
**Impact**: High - Enables non-blocking I/O and async WASI
**Completed**: 2025-11-19 (See ASYNC_IMPLEMENTATION_ROADMAP.md)

#### What's Implemented:
- ✅ **Java async API** - `AsyncEngineConfig`, `AsyncHostFunction`, `AsyncFunctionCall` fully implemented
- ✅ **Tokio runtime** - Global multi-threaded runtime with Send-safe JavaVM approach
- ✅ **Engine async_support** - Full integration with Wasmtime's `Config::async_support()`
- ✅ **Async host functions** - `create_wasmtime_func_async()` using `Func::new_async`
- ✅ **Async function calls** - `nativeCallAsync` JNI binding with Tokio runtime
- ✅ **Store async compatibility** - Works transparently with async-configured engines
- ✅ **WASI async integration** - wasmtime-wasi automatically handles async operations
- ✅ **Integration tests** - Comprehensive async function execution tests

#### Implementation Details:
- **Send trait solution**: Resolved using JavaVM approach for thread-safe callbacks
- **Runtime**: Multi-threaded Tokio runtime with operation tracking
- **JNI binding**: `block_on` for synchronous JNI interface with async execution
- **Feature flag**: Conditional compilation with `async` feature flag

**All blockers resolved** - Production-ready async operations

See `ASYNC_IMPLEMENTATION_ROADMAP.md` for detailed implementation plan.

### 8. Full Thread Support ⚠️ PARTIALLY IMPLEMENTED (90% Complete)
**Status**: Nearly complete - only thread execution and WASI-threads stubs remaining
**Impact**: Medium - Enables WebAssembly threads proposal
**Effort**: 2-3 days to complete remaining items
**Last Audited**: 2025-11-19 (Second audit - discovered atomic ops complete!)

#### What's Fully Implemented:
- ✅ **Config::wasm_threads()** - FULLY IMPLEMENTED in EngineBuilder (src/engine.rs:476-480)
- ✅ **Thread structures** - WasmThread, WasmThreadPool, WasmThreadState (src/threading.rs)
- ✅ **Thread-local storage** - Full implementation with all value types (src/threading.rs:332-393)
- ✅ **Synchronization primitives** - Comprehensive implementations (src/sync_primitives.rs:1616 lines)
  - AdvancedRwLock with priority queues and fairness
  - AdvancedCondvar with spurious wakeup prevention
  - AdvancedSemaphore with adaptive backoff
  - AdvancedBarrier with dynamic participant management
- ✅ **Atomic operations** - ALL 15 operations FULLY IMPLEMENTED (src/memory.rs:2144-2692)
  - atomic_compare_and_swap_i32/i64
  - atomic_load/store_i32/i64
  - atomic_add/and/or/xor_i32/i64
  - atomic_fence, atomic_notify
  - atomic_wait32/wait64
- ✅ **JNI atomic bindings** - All atomic ops have JNI bindings (src/jni_bindings.rs:10304-10764)
- ✅ **SharedMemory support** - Used internally, isShared() works, all atomic ops work on shared memory
- ✅ **WASI-threads scaffolding** - Extensive framework (src/wasi_threads.rs:1411 lines)
- ✅ **Thread pool management** - Work-stealing, NUMA-aware scheduling
- ✅ **Panama FFI exports** - Thread-local storage operations (src/threading.rs:676-867)

#### What Needs Implementation (2 items):
- ❌ **Thread execution integration** - Connect WasmThread::executeFunction() to actual WASM function execution
- ❌ **WASI-threads implementation** - Complete stubbed methods in WasiThreadsContext

**Note**: The stubs in threading.rs:535-646 (AtomicMemoryOperations) are UNUSED. JNI bindings correctly call the real implementations in memory::core.

**Note**: WebAssembly threads proposal is Phase 4 (standardized). Wasmtime has known limitations:
- Not integrated with resource limits
- No pooling allocator support
- Future direction is shared-everything-threads for WASI v0.2+

---

## IMPLEMENTATION ORDER RECOMMENDATION

### Sprint 1 (COMPLETED):
1. ✅ Engine configuration queries - **FULLY COMPLETE** (Java + native)
2. ✅ SIMD Java API wrapper - **FULLY COMPLETE**
3. ✅ Linker advanced features - **FULLY COMPLETE**

### Sprint 2 (COMPLETED):
1. ✅ Debugging APIs - **COMPLETE** (implemented all available Wasmtime features)
   - Backtrace capture (WasmBacktrace, FrameInfo, FrameSymbol)
   - DWARF debug info configuration
   - Guest debug instrumentation configuration
   - Note: Interactive debugging (breakpoints, stepping) blocked on Wasmtime DAP support

### Sprint 3 (COMPLETED):
1. ✅ Memory64 support - **COMPLETE** (discovered existing implementation, added missing native binding)

### Sprint 4 (IN PROGRESS - 95% Complete):
**Started**: 2025-11-19
**Async Completed**: 2025-11-19
**Thread Research Completed**: 2025-11-19 (Second audit: discovered atomic ops complete!)
**Status**: Async complete, thread support 90% complete (only 2 items remaining!)

1. ✅ Async operations - **100% COMPLETE** (Production-ready)
   - ✅ Research Wasmtime async APIs and capabilities
   - ✅ Audit existing infrastructure
   - ✅ Add `EngineConfig.asyncSupport()` configuration
   - ✅ Create detailed implementation roadmap (ASYNC_IMPLEMENTATION_ROADMAP.md)
   - ✅ Fix Send trait issues using JavaVM approach
   - ✅ Implement Func::new_async and Func::call_async bindings
   - ✅ Connect WASI async support
   - ✅ Create comprehensive integration tests
   - **Completed**: 2025-11-19

2. ⚠️ Thread support - **90% COMPLETE** (Nearly complete, 2 items remaining!)
   - ✅ Research WebAssembly threads proposal
   - ✅ Comprehensive audit (2 rounds - discovered atomic ops complete!)
   - ✅ Config::wasm_threads() - **COMPLETE** (engine.rs:476-480)
   - ✅ Thread structures and management - **COMPLETE** (threading.rs, sync_primitives.rs)
   - ✅ Thread-local storage - **COMPLETE** with FFI exports
   - ✅ Advanced synchronization primitives - **COMPLETE** (1616 lines)
   - ✅ Atomic operations - **COMPLETE** (all 15 ops in memory.rs:2144-2692)
   - ✅ JNI atomic bindings - **COMPLETE** (jni_bindings.rs:10304-10764)
   - ✅ SharedMemory support - **COMPLETE** (isShared, atomic ops work)
   - ❌ Thread execution with WASM functions (WasmThread::executeFunction)
   - ❌ Complete WASI-threads stubbed methods
   - **Remaining effort**: 2-3 days

**Major Discovery**: Atomic operations were fully implemented all along in memory.rs! The stubs in threading.rs are unused. Thread support is 90% complete, not 70%!

**Next Steps**: Implement WasmThread::executeFunction and complete WASI-threads stubs

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

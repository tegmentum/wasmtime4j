# WASI Panama Implementation - Next Steps

## Current Status

**Phase 1: COMPLETE ✅**
- PanamaWasiConfig (162 lines)
- PanamaWasiConfigBuilder (318 lines)
- PanamaWasiLinker (235 lines)

**Progress:** 36/46 files (78% complete)

## Phase 2: Core WASI Operations Implementation

### Required Work

Phase 2 involves implementing the two most critical WASI operational files:
1. `WasiPreview1Operations.java` (~600 lines, core WASI Preview 1 spec)
2. `WasiPreview2Operations.java` (~500 lines, modern WASI Preview 2 spec)

### Technical Requirements

#### 1. Rust Native Bindings

The current Rust native code (`wasmtime4j-native/src/wasi.rs`) exports JNI-specific functions:
```rust
pub unsafe extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiRandomOperations_nativeGetRandomBytes(...)
```

For Panama, we need C-ABI compatible exports:
```rust
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_get_random_bytes(
    context_handle: *const c_void,
    buffer: *mut u8,
    length: usize
) -> i32 {
    // Implementation
}
```

**Action Items:**
- [ ] Create `wasmtime4j-native/src/panama_wasi_bindings.rs`
- [ ] Export C-ABI functions for all WASI operations
- [ ] Update `Cargo.toml` to build both JNI and Panama bindings
- [ ] Test native library compiles with both binding sets

#### 2. Java Panama FFI Bindings

For each native function, create Panama MethodHandle bindings:

```java
// Example from existing PanamaGcRuntime pattern:
private static final FunctionDescriptor GET_RANDOM_BYTES_DESC =
    FunctionDescriptor.of(JAVA_INT,     // return: error code
                          ADDRESS,       // context handle
                          ADDRESS,       // buffer
                          JAVA_INT);     // length

private final MethodHandle getRandomBytes;

// In constructor:
this.getRandomBytes = linker.downcallHandle(
    lookup.find("wasmtime4j_wasi_get_random_bytes").orElseThrow(),
    GET_RANDOM_BYTES_DESC
);

// Usage:
try (Arena arena = Arena.ofConfined()) {
    MemorySegment buffer = arena.allocate(length);
    int result = (int) getRandomBytes.invokeExact(
        contextHandle,
        buffer,
        length
    );
    // Handle result
}
```

**Action Items:**
- [ ] Create `PanamaWasiPreview1Operations.java`
- [ ] Define FunctionDescriptors for all WASI Preview 1 operations
- [ ] Implement MethodHandle bindings
- [ ] Add proper memory management with Arena
- [ ] Create `PanamaWasiPreview2Operations.java` following same pattern

#### 3. Implementation Pattern

**File Structure:**
```
wasmtime4j-panama/src/main/java/ai/tegmentum/wasmtime4j/panama/wasi/
├── PanamaWasiConfig.java                    ✅ DONE
├── PanamaWasiConfigBuilder.java             ✅ DONE
├── PanamaWasiLinker.java                    ✅ DONE
├── WasiPreview1Operations.java              ⏳ TODO (Phase 2)
├── WasiPreview2Operations.java              ⏳ TODO (Phase 2)
├── WasiTimeOperationsPreview2.java          ⏳ TODO (Phase 3)
├── WasiRandomOperationsPreview2.java        ⏳ TODO (Phase 3)
└── ... (11 more files)
```

**Key Classes to Reference:**
- `PanamaGcRuntime.java` - Excellent example of Panama FFI MethodHandles
- `PanamaLinker.java` - Shows native function lookup pattern
- `WasiContext.java` (Panama) - Already exists with MemorySegment handle

### Step-by-Step Implementation Guide

#### Step 1: Add Rust Native Exports

1. Create `wasmtime4j-native/src/panama_wasi_bindings.rs`
2. For each JNI method in `wasi.rs`, create C-ABI equivalent:

```rust
use std::ffi::c_void;
use std::slice;

#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_fd_read(
    context: *const c_void,
    fd: i32,
    iovs_ptr: *const c_void,
    iovs_len: usize,
    nread_out: *mut usize
) -> i32 {
    if context.is_null() || iovs_ptr.is_null() || nread_out.is_null() {
        return 28; // EINVAL
    }

    // Implementation calling Wasmtime APIs
    // Convert pointers to Rust types
    // Call actual WASI operations
    // Return error code (0 = success)
}
```

3. Update `lib.rs` to include the new module
4. Build and verify exports: `nm -g target/release/libwasmtime4j.dylib | grep wasmtime4j_wasi`

#### Step 2: Create Java Panama Wrapper

1. Create `WasiPreview1Operations.java` in Panama package
2. Define all FunctionDescriptors at class level
3. Load native library and create MethodHandles in constructor
4. Implement each WASI operation using the MethodHandles

**Example Implementation:**

```java
package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiException;
import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiErrorCode;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.util.List;

public final class WasiPreview1Operations {

    private static final Linker NATIVE_LINKER = Linker.nativeLinker();

    // Function descriptors for all native methods
    private static final FunctionDescriptor FD_READ_DESC =
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,    // return: error code
            ValueLayout.ADDRESS,      // context
            ValueLayout.JAVA_INT,     // fd
            ValueLayout.ADDRESS,      // iovs
            ValueLayout.JAVA_INT,     // iovs_len
            ValueLayout.ADDRESS);     // nread_out

    private final WasiContext wasiContext;
    private final MethodHandle fdRead;

    public WasiPreview1Operations(final WasiContext wasiContext) {
        this.wasiContext = wasiContext;

        // Load native library
        SymbolLookup lookup = SymbolLookup.loaderLookup();

        // Create MethodHandles
        this.fdRead = NATIVE_LINKER.downcallHandle(
            lookup.find("wasmtime4j_wasi_fd_read").orElseThrow(),
            FD_READ_DESC
        );
    }

    public int fdRead(final int fd, final List<ByteBuffer> iovs) {
        try (Arena arena = Arena.ofConfined()) {
            // Allocate output parameter
            MemorySegment nreadOut = arena.allocate(ValueLayout.JAVA_LONG);

            // Convert iovs to native structure
            // ... (implementation details)

            // Call native function
            int result = (int) fdRead.invokeExact(
                wasiContext.getNativeHandle(),
                fd,
                iovsPtr,
                iovs.size(),
                nreadOut
            );

            if (result != 0) {
                throw new WasiException(
                    "fd_read failed",
                    WasiErrorCode.fromErrno(result)
                );
            }

            return (int) nreadOut.get(ValueLayout.JAVA_LONG, 0);
        } catch (Throwable e) {
            throw new WasiException("fd_read error", e);
        }
    }
}
```

#### Step 3: Test and Iterate

1. Write unit tests for each operation
2. Test with real WASM modules
3. Verify memory management (no leaks)
4. Compare behavior with JNI implementation

### Timeline Estimate

- **Rust bindings:** 8-10 hours (all WASI operations)
- **Java Panama wrappers:** 12-16 hours (Preview1 + Preview2)
- **Testing & debugging:** 4-6 hours
- **Total:** 24-32 hours for Phase 2

### Success Criteria

- [ ] All native functions exported with C-ABI
- [ ] Panama MethodHandles created for all operations
- [ ] Memory management verified (no leaks)
- [ ] Unit tests pass
- [ ] Integration tests with WASM modules pass
- [ ] Performance within 10% of JNI implementation

### Resources

- **Panama FFI Guide:** https://openjdk.org/jeps/454
- **Wasmtime Rust API:** https://docs.rs/wasmtime/latest/wasmtime/
- **Existing Examples:**
  - `wasmtime4j-panama/src/main/java/ai/tegmentum/wasmtime4j/panama/PanamaGcRuntime.java`
  - `wasmtime4j-jni/src/main/java/ai/tegmentum/wasmtime4j/jni/wasi/WasiPreview1Operations.java`

### Next Session Checklist

Before starting Phase 2 implementation:
1. ✅ Phase 1 complete and tested
2. ⏳ Rust development environment set up
3. ⏳ Understanding of Wasmtime WASI API
4. ⏳ Familiarity with Panama Foreign Function API
5. ⏳ Native library build process working

Start with a single operation (e.g., `random_get`) to establish the pattern, then scale to all operations.

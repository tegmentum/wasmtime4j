# Quick Start: Completing Panama Implementation

## Current State (2025-10-10)

### ✅ What's Done
- **Host Function Callbacks** - Fully implemented and tested
- **Memory Marshalling** - WasmValue ↔ Native (20-byte layout)
- **FFI Bindings** - Panama linker operations complete
- **Bug Fixes** - Result count, memory leaks, deadlock workaround

### ⏸️ What's Needed
- **PanamaEngine** - Native engine creation
- **PanamaStore** - Store management
- **PanamaModule** - WAT/WASM compilation

## Step-by-Step Implementation Guide

### Step 1: Implement PanamaEngine

**File:** `wasmtime4j-panama/src/main/java/ai/tegmentum/wasmtime4j/panama/PanamaEngine.java`

**Current Issue:**
```java
// Line 53 - returns NULL!
this.nativeEngine = MemorySegment.NULL;
```

**What to do:**

1. **Add FFI binding in NativeFunctionBindings.java:**
```java
// In initializeFunctionBindings()
addFunctionBinding(
    "wasmtime4j_panama_engine_create",
    FunctionDescriptor.of(
        ValueLayout.ADDRESS,      // return engine*
        ValueLayout.ADDRESS));    // config*

// Add wrapper method
public MemorySegment panamaEngineCreate(final MemorySegment configPtr) {
    validateNotNull(configPtr, "configPtr");
    return callNativeFunction("wasmtime4j_panama_engine_create",
        MemorySegment.class, configPtr);
}
```

2. **Add Rust FFI in panama_ffi.rs:**
```rust
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_create(
    _config_ptr: *const c_void  // Can ignore config for now
) -> *mut c_void {
    // Create default engine
    let engine = wasmtime::Engine::default();

    // Wrap in Engine struct (from existing code)
    let engine_wrapper = Box::new(crate::engine::Engine::new(engine));

    Box::into_raw(engine_wrapper) as *mut c_void
}

#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_destroy(
    engine_ptr: *mut c_void
) {
    if !engine_ptr.is_null() {
        unsafe {
            let _ = Box::from_raw(engine_ptr as *mut crate::engine::Engine);
        }
    }
}
```

3. **Update PanamaEngine constructor:**
```java
public PanamaEngine(final EngineConfig config) throws WasmException {
    if (config == null) {
        throw new IllegalArgumentException("Config cannot be null");
    }
    this.config = config;
    this.arena = Arena.ofShared();

    // Create native engine
    final MemorySegment configPtr = MemorySegment.NULL; // TODO: marshal config
    this.nativeEngine = NATIVE_BINDINGS.panamaEngineCreate(configPtr);

    if (this.nativeEngine == null || this.nativeEngine.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to create native engine");
    }

    LOGGER.fine("Created Panama engine");
}
```

4. **Update close() method:**
```java
@Override
public void close() {
    if (!closed) {
        closed = true;
        if (nativeEngine != null && !nativeEngine.equals(MemorySegment.NULL)) {
            NATIVE_BINDINGS.panamaEngineDestroy(nativeEngine);
        }
        arena.close();
    }
}
```

### Step 2: Implement PanamaModule

**File:** `wasmtime4j-panama/src/main/java/ai/tegmentum/wasmtime4j/panama/PanamaModule.java`

**Current Issue:**
```java
// Line 78 - throws exception!
throw new UnsupportedOperationException("Module compilation not yet implemented");
```

**What to do:**

1. **Add FFI bindings:**
```java
// NativeFunctionBindings.java
addFunctionBinding(
    "wasmtime4j_panama_module_compile",
    FunctionDescriptor.of(
        ValueLayout.ADDRESS,      // return module*
        ValueLayout.ADDRESS,      // engine*
        ValueLayout.ADDRESS,      // wasm_bytes
        ValueLayout.JAVA_INT));   // bytes_len
```

2. **Add Rust implementation:**
```rust
// panama_ffi.rs
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_module_compile(
    engine_ptr: *mut c_void,
    wasm_bytes: *const u8,
    bytes_len: c_int,
) -> *mut c_void {
    if engine_ptr.is_null() || wasm_bytes.is_null() {
        return std::ptr::null_mut();
    }

    unsafe {
        let engine = &*(engine_ptr as *mut crate::engine::Engine);
        let bytes = std::slice::from_raw_parts(wasm_bytes, bytes_len as usize);

        match crate::module::core::compile_module(engine, bytes) {
            Ok(module) => Box::into_raw(Box::new(module)) as *mut c_void,
            Err(_) => std::ptr::null_mut(),
        }
    }
}
```

3. **Update PanamaModule.compileWat:**
```java
public static PanamaModule compileWat(
    final PanamaEngine engine,
    final String wat
) throws WasmException {
    // Convert WAT to WASM (use wat2wasm crate or existing helper)
    byte[] wasmBytes = convertWatToWasm(wat);

    // Allocate native memory for bytes
    MemorySegment bytesSegment = engine.arena.allocate(wasmBytes.length);
    for (int i = 0; i < wasmBytes.length; i++) {
        bytesSegment.set(ValueLayout.JAVA_BYTE, i, wasmBytes[i]);
    }

    // Call native compile
    MemorySegment modulePtr = NATIVE_BINDINGS.panamaModuleCompile(
        engine.getNativeEngine(),
        bytesSegment,
        wasmBytes.length
    );

    if (modulePtr == null || modulePtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to compile module");
    }

    return new PanamaModule(engine, modulePtr);
}
```

### Step 3: Implement PanamaStore

**File:** `wasmtime4j-panama/src/main/java/ai/tegmentum/wasmtime4j/panama/PanamaStore.java`

**Follow similar pattern:**
- Add FFI binding for `wasmtime4j_panama_store_create(engine_ptr)`
- Implement Rust side using `wasmtime::Store::new()`
- Update constructor to create native store

### Step 4: Update Factory

**File:** `wasmtime4j/src/main/java/ai/tegmentum/wasmtime4j/factory/WasmRuntimeFactory.java`

**Change:**
```java
// Line ~220
private static RuntimeType selectRuntimeType() {
    // ... existing code ...

    // Automatic selection based on Java version
    final int majorVersion = getJavaVersion();

    // Use Panama for Java 23+
    if (majorVersion >= 23) {
        logger.info("Auto-selected Panama runtime for Java " + majorVersion);
        selectedRuntimeType = RuntimeType.PANAMA;
        return RuntimeType.PANAMA;
    }

    // Fall back to JNI for older versions
    logger.info("Auto-selected JNI runtime for Java " + majorVersion);
    selectedRuntimeType = RuntimeType.JNI;
    return RuntimeType.JNI;
}
```

### Step 5: Testing

**Test progression:**

1. **Engine Creation:**
```bash
./mvnw test -pl wasmtime4j-panama -Dtest=PanamaEngineTest
```

2. **Module Compilation:**
```bash
./mvnw test -pl wasmtime4j-panama -Dtest=PanamaModuleTest
```

3. **Store Management:**
```bash
./mvnw test -pl wasmtime4j-panama -Dtest=PanamaStoreTest
```

4. **Integration (will use Panama automatically):**
```bash
# Force Panama runtime
./mvnw test -pl wasmtime4j-comparison-tests \
  -Dwasmtime4j.runtime=panama \
  -Dtest=SimpleNoParamTest
```

5. **Full Host Function Suite:**
```bash
./mvnw test -pl wasmtime4j-comparison-tests \
  -Dwasmtime4j.runtime=panama \
  -Dtest=HostFunctionTest
```

## Common Pitfalls

### Memory Management
❌ **Wrong:** Forgetting to free native memory
```java
MemorySegment ptr = allocate(...);
// Forget to track arena lifecycle
```

✅ **Right:** Use Arena for automatic cleanup
```java
try (Arena arena = Arena.ofConfined()) {
    MemorySegment ptr = arena.allocate(...);
    // Automatically freed when arena closes
}
```

### FFI Descriptor Mismatches
❌ **Wrong:** Mismatched types
```java
FunctionDescriptor.of(
    ValueLayout.JAVA_LONG,    // Rust returns *mut c_void
    ValueLayout.ADDRESS);     // Expects pointer
```

✅ **Right:** Match Rust signature exactly
```java
FunctionDescriptor.of(
    ValueLayout.ADDRESS,      // *mut c_void = ADDRESS
    ValueLayout.ADDRESS);
```

### Null Pointer Checks
❌ **Wrong:** No validation
```java
MemorySegment result = nativeCall();
// Immediately dereference - could be NULL!
result.get(ValueLayout.JAVA_INT, 0);
```

✅ **Right:** Always validate
```java
MemorySegment result = nativeCall();
if (result == null || result.equals(MemorySegment.NULL)) {
    throw new WasmException("Native call failed");
}
```

## Debugging Tips

### Enable Native Logging
```bash
# In rust code, logs go to stderr
RUST_LOG=debug ./mvnw test ...
```

### Check Symbol Exports
```bash
nm wasmtime4j-native/target/release/libwasmtime4j.dylib | grep panama
```

### Verify Memory Layout
```java
// Print layout info
System.out.println("Segment address: " + segment.address());
System.out.println("Segment size: " + segment.byteSize());
```

### Use jextract for Validation
```bash
# Generate bindings from headers to verify layout
jextract --source -t com.example headers.h
```

## Reference Files

**Study these for patterns:**
- `PanamaLinker.java` - Complete Panama implementation example
- `panama_ffi.rs` - FFI export patterns
- `NativeFunctionBindings.java` - FFI binding setup
- `JniEngine.java` - Engine implementation reference (JNI version)

## Time Estimate

- **PanamaEngine:** 2-4 hours
- **PanamaModule:** 3-5 hours
- **PanamaStore:** 2-3 hours
- **Testing & Integration:** 3-5 hours
- **Total:** ~10-17 hours

## Success Criteria

✅ All comparison tests pass with `-Dwasmtime4j.runtime=panama`
✅ No memory leaks (verified with leak detector)
✅ Performance equal to or better than JNI
✅ Factory auto-selects Panama on Java 23+

## Questions?

Check:
1. `PANAMA_IMPLEMENTATION_NOTES.md` - Detailed technical info
2. `CLAUDE.md` - Project architecture
3. Existing JNI implementations - Pattern reference
4. Panama FFI JEP 442 - Official specification

The hard part (host functions) is done. The rest is straightforward FFI plumbing!

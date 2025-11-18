# WASI Panama Implementation Plan

## Current Status
- **Completed:** 33/~46 files (72%)
- **Remaining:** 17 files to implement

## Remaining Work Breakdown

### Phase 1: Configuration Files (3 files) - PRIORITY: HIGH
**Effort:** 4-6 hours | **Dependencies:** None

1. `PanamaWasiConfig.java` - Pure Java, implements WasiConfig interface
2. `PanamaWasiConfigBuilder.java` - Builder pattern, pure Java
3. `PanamaWasiLinker.java` - Panama FFI bindings for WASI linker (foundational)

**Approach:** Copy JNI structure, adapt to Panama patterns

---

### Phase 2: Core WASI Operations (2 files) - PRIORITY: HIGH
**Effort:** 12-16 hours | **Dependencies:** Phase 1

1. `WasiPreview1Operations.java` (~800-1000 lines)
   - fd_read, fd_write, fd_seek, fd_close
   - path_open, path_create_directory, path_unlink
   - environ_get, args_get, clock_time_get, random_get, poll_oneoff

2. `WasiPreview2Operations.java` (~600-800 lines)
   - Modernized Preview2 API operations

**Risk:** HIGH - Critical for WASI functionality

---

### Phase 3: Specialized Operations (3 files) - PRIORITY: MEDIUM
**Effort:** 6-8 hours | **Dependencies:** Phase 2

1. `WasiTimeOperationsPreview2.java` - Clock operations
2. `WasiRandomOperationsPreview2.java` - Random number generation
3. `WasiEnvironmentOperationsPreview2.java` - Environment variable access

---

### Phase 4: Advanced File Operations (3 files) - PRIORITY: MEDIUM
**Effort:** 8-10 hours | **Dependencies:** Phase 2

1. `WasiAdvancedFileOperations.java` - fd_advise, fd_allocate, fd_sync
2. `WasiAsyncFileOperations.java` - Async I/O operations
3. `WasiFilesystemSnapshot.java` - Filesystem snapshot capabilities

---

### Phase 5: Networking & I/O (5 files) - PRIORITY: LOW
**Effort:** 8-10 hours | **Dependencies:** Phase 2, 4

1. `WasiNetworkOperations.java` - Socket operations
2. `WasiAdvancedNetworking.java` - Advanced networking
3. `WasiStreamOperations.java` - Stream-based I/O
4. `WasiExperimentalIO.java` - Experimental I/O operations
5. `WasiNioIntegration.java` - Java NIO integration

---

### Phase 6: Process Operations (1 file) - PRIORITY: LOW
**Effort:** 4-6 hours | **Dependencies:** Phase 2

1. `WasiExperimentalProcess.java` - Process spawn/management

---

### Phase 7: Testing & Integration - PRIORITY: HIGH
**Effort:** 8-12 hours | **Dependencies:** All phases

- Unit tests per file
- Integration tests with real WASM modules
- JNI vs Panama compatibility tests
- Performance benchmarks

---

## Implementation Pattern

### For Each File:

1. **Read JNI Implementation**
   - Understand native method signatures
   - Identify error handling patterns

2. **Create Panama FFI Bindings**
   ```java
   // JNI pattern:
   private native int wasmtime_operation(long handle, ...);

   // Panama pattern:
   private static final FunctionDescriptor OP_DESC =
       FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ...);
   private final MethodHandle operation =
       loader.lookupFunction("wasmtime_operation", OP_DESC);
   ```

3. **Memory Management**
   - Use Arena for scoped allocation
   - MemorySegment for buffers
   - Proper cleanup in try-with-resources

4. **Test Implementation**
   - Write tests first
   - Verify error cases
   - Check memory cleanup

---

## Timeline Estimate

**Total:** 50-68 hours

- Phase 1: 4-6 hours
- Phase 2: 12-16 hours
- Phase 3: 6-8 hours
- Phase 4: 8-10 hours
- Phase 5: 8-10 hours
- Phase 6: 4-6 hours
- Phase 7: 8-12 hours

**Recommended Schedule:**
- Week 1-2: Phases 1 + 2 (Configuration + Core Operations)
- Week 3: Phases 3 + 4 (Specialized + File Operations)
- Week 4: Phases 5 + 6 (Networking + Process)
- Week 5: Phase 7 (Testing & Polish)

---

## Success Criteria

✅ All 17 files implemented
✅ Panama FFI bindings for all operations
✅ Tests pass for JNI and Panama
✅ No memory leaks
✅ Performance within 10% of JNI
✅ Full WASI compatibility

---

## Risk Mitigation

**High Risk Files:**
- WasiPreview1Operations (most complex, implement first)
- WasiPreview2Operations (depends on Preview1 patterns)
- PanamaWasiLinker (foundational, careful implementation needed)

**Strategy:**
- Start with highest priority/risk items
- Establish patterns early with Preview1
- Reuse patterns across remaining files
- Test continuously throughout implementation

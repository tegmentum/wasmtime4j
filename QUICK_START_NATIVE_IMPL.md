# Quick Start: Native Implementation

## Current Status
✅ **Java Implementation Complete** - All Store and Linker APIs implemented
⏳ **Native Implementation Pending** - 9 Rust methods need implementation

## Quick Reference

### What's Done
- Store.createTable() and createMemory() - Java layer complete
- Linker methods (7 total) - Java layer complete
- 62 comprehensive tests ready to validate implementation
- Full documentation in NATIVE_IMPLEMENTATION_GUIDE.md

### What's Next
Implement 9 native Rust methods in `wasmtime4j-native/src/`:

#### Priority Order (Start Here)
1. **Store Methods** (Foundation)
   - `nativeCreateTable` - Create WebAssembly tables
   - `nativeCreateMemory` - Create linear memory

2. **Basic Linker Methods** (Core Functionality)
   - `nativeInstantiate` - Basic module instantiation
   - `nativeDefineInstance` - Register instance exports

3. **Import Definition Methods** (Extended Functionality)
   - `nativeDefineMemory` - Register memory imports
   - `nativeDefineTable` - Register table imports
   - `nativeDefineGlobal` - Register global imports

4. **Advanced Features** (Complex)
   - `nativeDefineHostFunction` - Most complex, requires callback bridge
   - `nativeInstantiateNamed` - Named module instantiation

### File Locations

**Java Implementation (Reference)**:
```
wasmtime4j-jni/src/main/java/ai/tegmentum/wasmtime4j/jni/
├── JniStore.java         # Lines 516-604, 1191-1211
└── JniLinker.java        # Lines 58-584
```

**Native Implementation (Work Here)**:
```
wasmtime4j-native/src/
├── store.rs              # Add createTable/createMemory
├── linker.rs             # Add all Linker methods (or create new file)
└── jni_bindings.rs       # Add JNI function exports
```

**Tests (Validation)**:
```
wasmtime4j-comparison-tests/src/test/java/.../comparison/
├── hostfunc/HostFunctionTest.java   # 10 tests
├── globals/GlobalsTest.java         # 13 tests
├── tables/TablesTest.java           # 13 tests
├── wasi/WasiTest.java              # 14 tests
└── linker/LinkerTest.java          # 12 tests
```

### Implementation Workflow

1. **Pick a Method** (Start with nativeCreateTable or nativeCreateMemory)

2. **Reference Documentation**:
   ```bash
   # Open implementation guide
   cat NATIVE_IMPLEMENTATION_GUIDE.md

   # See example for your method
   grep -A 30 "nativeCreateTable" NATIVE_IMPLEMENTATION_GUIDE.md
   ```

3. **Implement in Rust**:
   - Add function to appropriate file in `wasmtime4j-native/src/`
   - Follow pattern from existing native methods
   - Use Wasmtime API examples from guide

4. **Build and Test**:
   ```bash
   # Build with native code
   ./mvnw clean compile

   # Run specific test
   ./mvnw test -Dtest=TablesTest#testBasicTableCreation

   # Run all tests for that feature
   ./mvnw test -Dtest=TablesTest
   ```

5. **Iterate**:
   - Fix compilation errors
   - Fix test failures
   - Repeat until tests pass

### Example: Implementing nativeCreateTable

**Step 1**: Open `wasmtime4j-native/src/store.rs` or create `wasmtime4j-native/src/table.rs`

**Step 2**: Add JNI function:
```rust
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateTable(
    env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    element_type: jint,
    initial_size: jint,
    max_size: jint,
) -> jlong {
    // Implementation here - see NATIVE_IMPLEMENTATION_GUIDE.md
}
```

**Step 3**: Implement logic using Wasmtime API (see guide for details)

**Step 4**: Build:
```bash
./mvnw clean compile
```

**Step 5**: Test:
```bash
./mvnw test -Dtest=TablesTest#testBasicTableCreation
```

### Testing Strategy

**Unit Test Individual Methods**:
```bash
# Test table creation
./mvnw test -Dtest=TablesTest#testBasicTableCreation

# Test memory creation
./mvnw test -Dtest=MemoryOperationsTest#testMemoryAllocation

# Test basic instantiation
./mvnw test -Dtest=LinkerTest#testBasicModuleLinking
```

**Integration Test Complete Features**:
```bash
# All table tests
./mvnw test -Dtest=TablesTest

# All linker tests
./mvnw test -Dtest=LinkerTest

# All tests
./mvnw test
```

### Common Issues & Solutions

**Issue**: JNI function not found
- **Solution**: Check function signature matches exactly (name mangling)
- **Verify**: `javap -s -p JniStore.class | grep nativeCreateTable`

**Issue**: Wasmtime type conversion errors
- **Solution**: Refer to type mapping section in NATIVE_IMPLEMENTATION_GUIDE.md
- **Check**: Ensure i32 ↔ jint, i64 ↔ jlong conversions are correct

**Issue**: Segfault or JVM crash
- **Solution**: Check null pointer handling and proper JNI error checking
- **Debug**: Use `RUST_BACKTRACE=1` and enable JNI verbose mode

**Issue**: Tests fail with UnsupportedOperationException
- **Solution**: Method still calling stub implementation, not native method
- **Check**: Verify native library loaded and JNI binding correct

### Resources

- **Native Implementation Guide**: `NATIVE_IMPLEMENTATION_GUIDE.md` (detailed)
- **Implementation Status**: `IMPLEMENTATION_STATUS.md` (overview)
- **Wasmtime Rust Docs**: https://docs.rs/wasmtime/latest/wasmtime/
- **JNI Specification**: https://docs.oracle.com/javase/8/docs/technotes/guides/jni/

### Expected Timeline

| Phase | Methods | Effort | Tests |
|-------|---------|--------|-------|
| Store APIs | 2 | ~4-6 hours | TableTest, MemoryTest |
| Basic Linker | 2 | ~4-6 hours | LinkerTest (basic) |
| Import Definitions | 3 | ~6-8 hours | LinkerTest, GlobalsTest |
| Host Functions | 2 | ~12-16 hours | HostFunctionTest (complex callbacks) |

**Total Estimated Effort**: 26-36 hours for full implementation

### Success Criteria

✅ All 9 native methods implemented
✅ All 62 tests passing
✅ No memory leaks (valgrind clean)
✅ No JVM crashes under load
✅ Code review approved

## Ready to Start?

```bash
# 1. Read the implementation guide
cat NATIVE_IMPLEMENTATION_GUIDE.md

# 2. Pick your first method (recommend nativeCreateTable)
# 3. Implement in wasmtime4j-native/src/
# 4. Build and test iteratively
# 5. Move to next method

# Good luck! 🚀
```

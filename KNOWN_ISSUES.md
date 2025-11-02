# Known Issues

## MemoryType min/max Swap Issue

**Status**: Under Investigation
**Affected Test**: `ModuleImportValidationTest.testGetMemoryType`
**File**: `wasmtime4j-comparison-tests/src/test/java/ai/tegmentum/wasmtime4j/comparison/module/ModuleImportValidationTest.java:177`

### Description

The test creates a WebAssembly memory with minimum=1 pages and maximum=10 pages:
```wat
(memory (export "mem") 1 10)
```

However, `MemoryType.getMinimum()` returns 10 and `MemoryType.getMaximum()` returns 1 (swapped values).

### Test Failure

```
org.opentest4j.AssertionFailedError: expected: <1> but was: <10>
    at ModuleImportValidationTest.testGetMemoryType(ModuleImportValidationTest.java:177)
```

### Investigation Summary

The code path has been traced through:
1. WAT `(memory (export "mem") 1 10)` → min=1, max=10 (correct per WebAssembly spec)
2. Memory retrieved via `Instance.getMemory()` → `from_wasmtime_memory()` stores Wasmtime's MemoryType
3. `nativeGetMemoryTypeInfo()` reads `memory.memory_type.minimum()` and `maximum()`
4. Java receives values as `typeInfo[0]=10, typeInfo[1]=1` (swapped)

### Code Locations

**Native Code**:
- `wasmtime4j-native/src/jni_bindings.rs:599` - Memory exported from instance
- `wasmtime4j-native/src/jni_bindings.rs:7222-7223` - MemoryType info retrieval
- `wasmtime4j-native/src/memory.rs:843-880` - `from_wasmtime_memory()` function

**Java Code**:
- `wasmtime4j-jni/src/main/java/ai/tegmentum/wasmtime4j/jni/type/JniMemoryType.java:62-76` - Type info parsing

### Potential Causes

1. **Wasmtime API**: Wasmtime's `MemoryType::minimum()` and `maximum()` might return values in unexpected order
2. **Parameter Order**: `MemoryType::new64()` at `memory.rs:502` might have swapped parameters
3. **Subtle Bug**: Logic error in the type conversion chain that hasn't been identified

### Next Steps

1. Add debug logging to verify what Wasmtime actually returns:
   ```rust
   log::debug!("MemoryType from Wasmtime: minimum={}, maximum={:?}",
       memory_type.minimum(), memory_type.maximum());
   ```

2. Create minimal Rust test case to verify Wasmtime behavior:
   ```rust
   let memory_type = MemoryType::new(1, Some(10));
   assert_eq!(memory_type.minimum(), 1);
   assert_eq!(memory_type.maximum(), Some(10));
   ```

3. Check if this is a known issue in the Wasmtime version being used

4. Verify the parameters to `MemoryType::new64()` are in the correct order per Wasmtime docs

### Workaround

None currently. Test is disabled pending investigation.

### References

- WebAssembly Spec: Memory limits specified as `(memory initial maximum)`
- MDN WebAssembly.Memory: Takes `initial` and `maximum` in pages (64KiB each)

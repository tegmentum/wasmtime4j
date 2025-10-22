# Func Test Failures - Detailed Analysis

## Overview
After enabling generated func tests, 6 failures were identified. This document provides detailed analysis and recommended fixes.

## Category 1: Test Implementation Stubs (5 tests)

### Priority: LOW (Can be deferred or disabled)

These tests are explicitly marked as unimplemented. They fail with:
```
org.opentest4j.AssertionFailedError: Test not yet implemented - awaiting test framework completion
```

**Affected Tests:**
1. `ai.tegmentum.wasmtime4j.comparison.generated.func.DtorDelayedTest.testDtorDelayed`
2. `ai.tegmentum.wasmtime4j.comparison.generated.func.CallIndirectNativeFromExportedTableTest.testCallIndirectNativeFromExportedTable`
3. `ai.tegmentum.wasmtime4j.comparison.generated.func.CallIndirectNativeFromWasmImportFuncReturnsFuncrefTest.testCallIndirectNativeFromWasmImportFuncReturnsFuncref`
4. `ai.tegmentum.wasmtime4j.comparison.generated.func.CallIndirectNativeFromWasmImportTableTest.testCallIndirectNativeFromWasmImportTable`
5. `ai.tegmentum.wasmtime4j.comparison.generated.func.ImportWorksTest.testImportWorks`

**Recommendation**: Mark these tests with `@Disabled` annotation and create tracking issues:
```java
@Disabled("Test implementation pending - tracks framework completion")
@Test
void testDtorDelayed() {
    // Implementation pending
}
```

---

## Category 2: Funcref Type Support Gap (1 test)

### Priority: HIGH (Blocks WebAssembly reference type functionality)

**Test**: `CallIndirectNativeFromExportedGlobalTest.testCallIndirectNativeFromExportedGlobal`

**Error**:
```
ai.tegmentum.wasmtime4j.WasmRuntimeException: Type error: Unsupported global value type for Object conversion: (ref null func)
```

**Location**:
- `wasmtime4j-jni/src/main/java/ai/tegmentum/wasmtime4j/jni/JniGlobal.java:211`
- Method: `JniGlobal.nativeSetValue()`

**Root Cause**:
The JNI global implementation does not support setting funcref values `(ref null func)`. This is a WebAssembly reference type that represents nullable function references.

**Impact**:
- Prevents using function references in globals
- Blocks advanced WebAssembly features using reference types
- Part of broader reference types support (funcref, externref)

**Recommended Fix**:
1. Add funcref type handling in `JniGlobal.nativeSetValue()`
2. Implement conversion from Java Object to Wasmtime funcref
3. Add corresponding support in native Rust code at `wasmtime4j-native/src/jni_bindings.rs`

**Implementation Notes**:
- Funcref values need special handling as they represent function pointers
- May require storing function references in a registry
- Should validate that the Object is actually a valid function reference
- Consider alignment with externref implementation patterns

**Estimated Complexity**: MEDIUM (requires both Java and Rust changes)

---

## Category 3: Import Linking Error (1 test)

### Priority: MEDIUM (Affects module instantiation with specific import patterns)

**Test**: `CallIndirectNativeFromWasmImportGlobalTest.testCallIndirectNativeFromWasmImportGlobal`

**Error**:
```
ai.tegmentum.wasmtime4j.LinkingException: [UNKNOWN] Instantiation error: Failed to instantiate module: incompatible import type for '::'
```

**Location**:
- `wasmtime4j-jni/src/main/java/ai/tegmentum/wasmtime4j/jni/JniLinker.java:372`
- Method: `JniLinker.nativeInstantiate()`

**Root Cause**:
The linker is reporting an incompatible import type when trying to instantiate a module. The `::` notation suggests an empty module name or default namespace.

**Possible Causes**:
1. Type mismatch between declared import and provided value
2. Missing or incorrect import definition
3. Issue with empty/default module namespace handling
4. Global type compatibility check failing

**Investigation Steps**:
1. Examine the test's WAT code to see what imports are expected
2. Check what values are being provided to the linker
3. Verify the import namespace/module handling in JniLinker
4. Compare with working import tests to identify differences

**Recommended Fix**:
1. Debug the test to identify exact import being rejected
2. Check import type matching logic in `JniLinker.nativeInstantiate()`
3. Verify Wasmtime error is being correctly translated
4. Add better error messages showing which import failed and why

**Estimated Complexity**: MEDIUM (requires debugging and potentially fixing import matching logic)

---

## Summary Statistics

| Category | Count | Priority | Can Defer |
|----------|-------|----------|-----------|
| Test Stubs | 5 | LOW | Yes |
| Funcref Support | 1 | HIGH | No |
| Import Linking | 1 | MEDIUM | Partial |

**Total Failures**: 6
**Blocking Issues**: 2 (funcref + import)
**Non-blocking**: 5 (test stubs)

---

## Recommended Action Plan

### Phase 1: Immediate (Enable more tests safely)
1. Mark 5 test stubs as `@Disabled` with tracking comments
2. Re-run tests to confirm only 2 real failures remain
3. Document known limitations

### Phase 2: Fix Critical Path (Unblock funcref)
1. Implement funcref support in `JniGlobal.nativeSetValue()`
2. Add corresponding Rust native code changes
3. Test with CallIndirectNativeFromExportedGlobalTest
4. Consider adding unit tests specifically for funcref globals

### Phase 3: Fix Import Issue (Complete func category)
1. Debug CallIndirectNativeFromWasmImportGlobalTest
2. Fix import type matching in JniLinker
3. Verify all func tests pass (except disabled stubs)

### Phase 4: Enable Next Category
1. Move to traps/ tests using same gradual approach
2. Monitor for similar patterns
3. Continue iteration

---

## Testing Notes

- **No JVM crashes observed** - All failures are clean exceptions
- **Error handling is working** - Errors are properly caught and reported
- **Gradual enablement is successful** - Able to isolate and analyze failures
- **Build verification marker present**: `ZYXWV_BUILD_VERIFIED_20251021_082527` appeared 57 times in logs (debugging artifact that should be cleaned up)

---

## Related Files

- `/tmp/comparison_test_expansion_summary.md` - Overall test expansion summary
- `/tmp/comparison_tests_with_func.txt` - Full test output log
- `wasmtime4j-comparison-tests/pom.xml:164-183` - Test configuration

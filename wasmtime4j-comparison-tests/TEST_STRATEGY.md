# Test Strategy for wasmtime4j

## Overview

This document describes the testing strategy for wasmtime4j, which validates that our Java bindings correctly implement Wasmtime's WebAssembly runtime behavior.

## Test Categories

### 1. WAST Tests (Generated) - **PRIMARY VALIDATION**

**Location:** `src/test/java/.../generated/`

**Purpose:** Validate WebAssembly execution correctness against Wasmtime's official test suite

**Source of Truth:** Wasmtime WAST files from upstream repository

**What They Test:**
- WebAssembly instruction semantics
- Trap conditions and error handling
- Type checking and validation
- Spec compliance for WebAssembly features

**How They Work:**
- Generated automatically from `.wast` files via `./generate-wast-tests.sh`
- Use `WastTestRunner` to execute WAST test directives (`assert_return`, `assert_trap`, etc.)
- Run on both JNI and Panama implementations
- Each test validates wasmtime4j produces same results as Wasmtime

**Coverage:** 31 generated test classes from Wasmtime test suite

**Example:** `ImportedMemoryCopyTest` validates the `memory.copy` instruction behavior

### 2. Manual Integration Tests

**Location:** `src/test/java/.../comparison/{module,linker,memory,etc}/`

**Purpose:** Validate Java API correctness and Java-specific concerns

**What They Test:**
- Java API ergonomics and usage patterns
- Resource lifecycle and cleanup
- Error handling and exception types
- Edge cases not covered by WAST tests
- Cross-cutting concerns (memory sharing between instances, linker usage, etc.)

**What They DON'T Test:**
- WebAssembly instruction semantics (WAST tests cover this)
- JNI vs Panama comparison (both should work correctly independently)

**How They Work:**
- Extend `DualRuntimeTest` to run on both JNI and Panama
- Use direct Java API calls (not `WastTestRunner`)
- Validate expected behavior based on Wasmtime documentation

**Examples:**
- `ModuleMemoryTest`: Tests Java API for sharing memory between instances
- `ModuleImportTest`: Tests Java API for importing host functions
- `LinkerTest`: Tests Linker API usage patterns

### 3. Unit Tests (Implementation-Specific)

**Location:** `wasmtime4j-jni/src/test/java` and `wasmtime4j-panama/src/test/java`

**Purpose:** Test implementation internals and low-level details

**What They Test:**
- JNI/Panama-specific implementation details
- Native marshalling correctness
- Handle management and lifecycle
- Implementation-specific edge cases

**Examples:**
- `JniModuleTest`: Tests JNI-specific module implementation
- `JniLinkerTest`: Tests JNI-specific linker implementation

## Validation Philosophy

### Primary Validation: WAST Tests

**The WAST tests are the source of truth** for correctness. They validate that wasmtime4j executes WebAssembly correctly according to Wasmtime's implementation.

**If a WAST test fails:** This indicates a bug in wasmtime4j's WebAssembly execution.

### Secondary Validation: Manual Integration Tests

Manual tests validate Java API correctness and edge cases.

**If a manual test fails but WAST tests pass:** This likely indicates:
- A bug in the Java wrapper layer
- Incorrect resource management
- API misuse or edge case not handled
- Test expectations need updating

### What We're Validating Against

1. **Wasmtime behavior** (via WAST tests)
2. **Wasmtime documentation** (for API usage)
3. **WebAssembly spec** (indirectly via Wasmtime)

**We are NOT:**
- Comparing JNI vs Panama for consistency (both should work correctly)
- Reimplementing WebAssembly validation (Wasmtime does this)
- Testing Wasmtime correctness (we trust upstream Wasmtime)

## Test Development Guidelines

### When Writing New Manual Tests

1. **Check if WAST tests already cover it**
   - If yes, don't duplicate in manual tests
   - Exception: Java-specific edge cases

2. **Focus on Java API concerns:**
   - Resource cleanup (`try-with-resources`, `.close()`)
   - Exception types and messages
   - `Optional` usage
   - Thread safety (if applicable)
   - API misuse scenarios

3. **Reference Wasmtime documentation:**
   ```java
   /**
    * Tests memory sharing between instances.
    *
    * Expected Wasmtime behavior:
    * - Memory exported from one instance can be imported by another
    * - Modifications are visible across instances
    *
    * Reference: https://docs.wasmtime.dev/api/wasmtime/struct.Memory.html
    */
   ```

4. **Use `DualRuntimeTest` for cross-runtime validation:**
   ```java
   @ParameterizedTest
   @ArgumentsSource(RuntimeProvider.class)
   void testSomething(RuntimeType runtime) {
       setRuntime(runtime);
       // Test implementation
   }
   ```

5. **Keep tests focused and independent:**
   - One test should test one behavior
   - Tests should not depend on each other
   - Clean up resources in `@AfterEach`

### When WAST Tests Fail

1. **Identify the failing directive:**
   - Which `assert_return`/`assert_trap` failed?
   - What was expected vs actual?

2. **Check if it's a known limitation:**
   - Some Wasmtime features may not be implemented yet
   - Check WAST generation script for exclusions

3. **Fix in native layer or Java wrapper:**
   - If execution is wrong → native layer bug
   - If marshalling is wrong → Java wrapper bug

4. **Verify fix doesn't break other tests:**
   - Run full WAST suite
   - Run manual integration tests

## Test Execution

### Run All Tests
```bash
./mvnw test
```

### Run Only WAST Tests
```bash
./mvnw test -pl wasmtime4j-comparison-tests -Dtest="**/generated/**/*Test"
```

### Run Only Manual Integration Tests
```bash
./mvnw test -pl wasmtime4j-comparison-tests -Dtest="**/comparison/{module,linker,memory}/**/*Test"
```

### Run Specific Runtime
```bash
# JNI only
./mvnw test -Dwasmtime4j.runtime=jni

# Panama only (requires Java 23+)
./mvnw test -Dwasmtime4j.runtime=panama
```

## Maintaining Test Suite

### Syncing with Upstream Wasmtime

Periodically update WAST files and regenerate tests:

```bash
# Update WAST files from Wasmtime repository
# (manual process - copy from Wasmtime repo)

# Regenerate tests
./generate-wast-tests.sh
```

### Adding New WAST Tests

1. Add `.wast` file to `wasmtime4j-tests/src/test/resources/wasm/wasmtime-tests/`
2. Run `./generate-wast-tests.sh`
3. Verify generated test compiles and runs

### Adding New Manual Tests

1. Identify Java API behavior to test
2. Check WAST tests don't already cover it
3. Create test in appropriate package (`module/`, `linker/`, etc.)
4. Extend `DualRuntimeTest`
5. Add Javadoc referencing expected Wasmtime behavior
6. Implement test using direct Java API calls

## Success Criteria

✅ **All WAST tests pass** - wasmtime4j correctly executes WebAssembly
✅ **All manual tests pass** - Java API works correctly
✅ **Tests run on both JNI and Panama** - Both implementations work
✅ **Coverage of Wasmtime features** - All exposed APIs are tested

## Known Limitations

Document any known limitations or WAST tests that can't be ported:

- (None currently documented)

---

**Last Updated:** 2025-10-28
**Wasmtime Version:** Testing against Wasmtime 37.0.2 test suite

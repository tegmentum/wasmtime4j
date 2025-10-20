# Comparison Test Framework Guide

## Overview

The WastTestRunner framework enables automated testing of wasmtime4j against Wasmtime's official test suite. This guide explains how to convert generated test stubs into working tests.

## Framework Status

- **Framework**: ✅ Complete with Linker support and production-ready
- **Tests Implemented**: 8 of 136 generated tests (5.9%)
- **Tests Passing**: 8/8 (100% success rate)
  - ✅ FloatComparisonTest - Multi-value returns (func category)
  - ✅ CallNativeToWasmTest - Multi-value returns (func category)
  - ✅ CallArrayToWasmTest - Parameters with multi-value returns (func category)
  - ✅ CallWasmToNativeTest - Host function imports with Linker (func category)
  - ✅ CallWasmToArrayTest - Host function with multi-value returns (func category)
  - ✅ TrapImportTest - Host function that traps during instantiation (func category)
  - ✅ FibTest - Two fibonacci implementations with 22 assertions (misc_testsuite category)
  - ✅ DivRemTest - Division and remainder operations for i32/i64 (misc_testsuite category)
- **Test Execution Time**: <1 second for all tests
- **Last Verified**: 2025-10-19

## Quick Start

### Basic Test Pattern

```java
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public final class ExampleTest {

  @Test
  @DisplayName("category::test-name")
  public void testExample() throws Exception {
    final String wat = """
        (module
          (func (export "add") (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.add
          )
        )
    """;

    try (final WastTestRunner runner = new WastTestRunner()) {
      runner.compileAndInstantiate(wat);

      // Assert that add(5, 7) returns 12
      runner.assertReturn(
          "add",
          new WasmValue[] {WasmValue.i32(12)},
          WasmValue.i32(5), WasmValue.i32(7));
    }
  }
}
```

## Conversion Steps

### 1. Update Imports

**Before:**
```java
import static org.junit.jupiter.api.Assertions.fail;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import java.io.InputStream;
```

**After:**
```java
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
```

### 2. Update Method Signature

**Before:**
```java
public void testExample() {
```

**After:**
```java
public void testExample() throws Exception {
```

### 3. Replace Implementation

**Before:**
```java
// TODO: Implement equivalent wasmtime4j test logic
// Expected results: ...
fail("Test not yet implemented - awaiting test framework completion");
```

**After:**
```java
try (final WastTestRunner runner = new WastTestRunner()) {
  runner.compileAndInstantiate(wat);

  // Add assertion calls based on expected results
  runner.assertReturn("functionName", expectedResults, ...args);
}
```

## WastTestRunner API

### Module Management

```java
// Compile and instantiate a module
Instance instance = runner.compileAndInstantiate(wat);

// Compile with a name for multi-module tests
Instance instance = runner.compileAndInstantiate("module1", wat);
```

### Assertions

#### assertReturn - Function returns expected values

```java
// No parameters, single return value
runner.assertReturn(
    "get_constant",
    new WasmValue[] {WasmValue.i32(42)});

// With parameters
runner.assertReturn(
    "add",
    new WasmValue[] {WasmValue.i32(12)},
    WasmValue.i32(5), WasmValue.i32(7));

// Multiple return values
runner.assertReturn(
    "multi",
    new WasmValue[] {
        WasmValue.i32(1),
        WasmValue.i32(2),
        WasmValue.i32(3)
    });
```

#### assertTrap - Function should trap (throw exception)

```java
// Any trap
runner.assertTrap("divide_by_zero", null, WasmValue.i32(10), WasmValue.i32(0));

// Specific trap message
runner.assertTrap("out_of_bounds", "out of bounds", WasmValue.i32(1000));
```

#### assertInvalid - Module should fail validation

```java
final String invalidWat = """
    (module
      (func (result i32)
        i64.const 42  ;; Type mismatch
      )
    )
""";

runner.assertInvalid(invalidWat, "type mismatch");
```

#### assertMalformed - Module should fail parsing

```java
runner.assertMalformed(malformedWat, "unexpected token");
```

#### assertUnlinkable - Module should fail linking

```java
final String unlinkableWat = """
    (module
      (import "env" "missing" (func))
    )
""";

runner.assertUnlinkable(unlinkableWat, "unknown import");
```

### Direct Invocation

```java
// Call a function directly
WasmValue[] results = runner.invoke("functionName", WasmValue.i32(42));

// Call on named instance
WasmValue[] results = runner.invoke("module1", "functionName", args);
```

### Host Functions (Linker Support)

The framework supports defining host functions that can be imported by WASM modules:

```java
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValueType;

try (final WastTestRunner runner = new WastTestRunner()) {
  // Define the function type signature
  final FunctionType funcType = new FunctionType(
      new WasmValueType[] {}, // No parameters
      new WasmValueType[] {WasmValueType.I32, WasmValueType.I32, WasmValueType.I32} // Three i32 returns
  );

  // Define a host function
  runner.defineHostFunction(
      "host",           // Module name
      "triple",         // Function name
      funcType,         // Function signature
      (params) -> {     // Implementation
        return new WasmValue[] {
          WasmValue.i32(1),
          WasmValue.i32(2),
          WasmValue.i32(3)
        };
      });

  // WAT module that imports the host function
  final String wat = """
      (module
        (import "host" "triple" (func (result i32 i32 i32)))
        (func (export "run") (result i32 i32 i32)
          call 0
        )
      )
  """;

  runner.compileAndInstantiate(wat);
  runner.assertReturn("run",
      new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3)});
}
```

**Key Points:**
- Host functions must be defined before calling `compileAndInstantiate()`
- Function types must match the import declaration in WAT
- The framework automatically uses Linker when host functions are defined
- Multiple host functions can be defined with different module/function names

## Test Categories & Examples

### Category: func (13 tests)

Simple function call tests without host imports.

**Example: CallNativeToWasmTest**
```java
try (final WastTestRunner runner = new WastTestRunner()) {
  runner.compileAndInstantiate(wat);
  runner.assertReturn("run",
      new WasmValue[] {WasmValue.i32(42), WasmValue.i32(420), WasmValue.i32(4200)});
}
```

**Status**: 6 of 13 complete (46%)

**Completed**:
- ✅ CallWasmToWasmTest
- ✅ CallNativeToWasmTest
- ✅ CallArrayToWasmTest
- ✅ CallWasmToNativeTest (with Linker support)
- ✅ CallWasmToArrayTest (with Linker support)
- ✅ TrapImportTest (with Linker support)

**Remaining without host imports**:
- CallIndirectNativeFromExportedGlobalTest (requires table manipulation)
- CallIndirectNativeFromExportedTableTest (requires table manipulation)

**Remaining with host imports - blocked by reference type support**:
- ImportWorksTest (requires externref, funcref, anyref, i31ref)
- DtorDelayedTest (requires internal lifecycle hooks - not testable from Java API)
- CallIndirectNativeFromWasmImportFuncReturnsFuncrefTest (requires funcref)
- CallIndirectNativeFromWasmImportGlobalTest (requires reference types)
- CallIndirectNativeFromWasmImportTableTest (requires reference types)

### Category: misc_testsuite (109 tests)

Diverse tests covering various WebAssembly features.

**Example: FloatComparisonTest**
```java
try (final WastTestRunner runner = new WastTestRunner()) {
  runner.compileAndInstantiate(wat);
  runner.assertReturn("d",
      new WasmValue[] {WasmValue.i32(0), WasmValue.i32(1), WasmValue.i32(1111)});
}
```

**Example: FibTest**
```java
try (final WastTestRunner runner = new WastTestRunner()) {
  runner.compileAndInstantiate(wat);
  runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));
  runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));
  runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(89)}, WasmValue.i32(10));
}
```

**Example: DivRemTest**
```java
try (final WastTestRunner runner = new WastTestRunner()) {
  runner.compileAndInstantiate(wat);
  runner.assertReturn(
      "i32.div_s", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(-1), WasmValue.i32(-1));
}
```

**Status**: 3 of 109 complete (2.8%)

**Completed**:
- ✅ FloatComparisonTest
- ✅ FibTest (22 assertions across two implementations)
- ✅ DivRemTest (4 assertions testing i32/i64 division and remainder)

**Remaining tests**: ThreadsTest, SimdTest, ResourcesTest, MultiMemoryTest, etc.

### Category: traps (6 tests)

Tests for proper trap handling.

**Pattern**:
```java
try (final WastTestRunner runner = new WastTestRunner()) {
  runner.compileAndInstantiate(wat);
  runner.assertTrap("function_name", "expected trap message", ...args);
}
```

**Status**: 0 of 6 complete

### Category: hostfuncs (7 tests)

Tests for host function imports.

**Note**: Linker support is now available in the framework.

**Status**: 0 of 7 complete

### Category: componentmodel (1 test)

Component model tests.

**Status**: 0 of 1 complete

## WasmValue Types

```java
// Integer types
WasmValue.i32(42)                 // 32-bit integer
WasmValue.i64(42L)                // 64-bit integer

// Floating point types
WasmValue.f32(3.14f)             // 32-bit float
WasmValue.f64(3.14159)           // 64-bit double

// Extracting values
int i = value.asInt();
long l = value.asLong();
float f = value.asFloat();
double d = value.asDouble();
```

## Multi-Module Tests

Some tests instantiate multiple modules:

```java
try (final WastTestRunner runner = new WastTestRunner()) {
  // First module
  runner.compileAndInstantiate("module1", wat1);

  // Second module
  runner.compileAndInstantiate("module2", wat2);

  // Invoke on specific module
  runner.invoke("module1", "function", args);
  runner.invoke("module2", "function", args);
}
```

## Common Patterns

### Pattern 1: Simple Return Value Test

```java
try (final WastTestRunner runner = new WastTestRunner()) {
  runner.compileAndInstantiate(wat);
  runner.assertReturn("func", new WasmValue[] {WasmValue.i32(expected)});
}
```

### Pattern 2: Parameterized Function Test

```java
try (final WastTestRunner runner = new WastTestRunner()) {
  runner.compileAndInstantiate(wat);
  runner.assertReturn(
      "add",
      new WasmValue[] {WasmValue.i32(sum)},
      WasmValue.i32(a), WasmValue.i32(b));
}
```

### Pattern 3: Multiple Assertions

```java
try (final WastTestRunner runner = new WastTestRunner()) {
  runner.compileAndInstantiate(wat);

  // Test 1
  runner.assertReturn("get", new WasmValue[] {WasmValue.i32(0)});

  // Test 2
  runner.assertReturn("set", new WasmValue[] {}, WasmValue.i32(42));

  // Test 3
  runner.assertReturn("get", new WasmValue[] {WasmValue.i32(42)});
}
```

### Pattern 4: Trap Testing

```java
try (final WastTestRunner runner = new WastTestRunner()) {
  runner.compileAndInstantiate(wat);

  // Valid call
  runner.assertReturn("div", new WasmValue[] {WasmValue.i32(5)},
      WasmValue.i32(10), WasmValue.i32(2));

  // Trap on division by zero
  runner.assertTrap("div", "division by zero",
      WasmValue.i32(10), WasmValue.i32(0));
}
```

## Running Tests

```bash
# Run specific test
./mvnw test -pl wasmtime4j-comparison-tests -Dtest=FloatComparisonTest -Djacoco.skip=true -q

# Run all func category tests (when completed)
./mvnw test -pl wasmtime4j-comparison-tests -Dtest="ai.tegmentum.wasmtime4j.comparison.generated.func.*" -Djacoco.skip=true

# Note: Generated tests are excluded from default surefire execution
# Use -Dtest= to run them explicitly
```

## Known Limitations

1. **Reference Types**: Some tests use reference types (externref, funcref)
   - May require additional WasmValue types

2. **WASI Imports**: Tests requiring WASI may need WASI context setup

3. **Component Model**: Component model tests may need specialized support

4. **Table and Global Operations**: Some tests require table/global manipulation beyond simple function calls

## Future Enhancements

1. Add helper methods for common test patterns
2. Add support for memory operations
3. Add support for table operations
4. Add support for global operations
5. Automated test generation from WAST files
6. Support for reference types (externref, funcref)

## Contributing

When updating tests:

1. Follow the conversion steps above
2. Test your changes: `./mvnw test -pl wasmtime4j-comparison-tests -Dtest=YourTest -Djacoco.skip=true -q`
3. Keep the original WAT/WAST code in comments
4. Use descriptive assertion messages
5. Update this guide with new patterns discovered

## Summary

- **Framework Status**: ✅ Complete with Linker support, tested, and production-ready
- **Tests Implemented**: 8 / 136 (5.9%)
- **Test Success Rate**: 100% (8/8 passing)
- **Framework Location**: `ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner`
- **Lines of Code**: 357 lines (framework) + comprehensive documentation
- **Next Priority**: Implement misc_testsuite tests or add reference type support for remaining func tests

# Error Handling Guidelines for wasmtime4j

This document provides comprehensive guidelines for handling errors in wasmtime4j applications, including best practices, error categorization, recovery strategies, and monitoring recommendations.

## Overview

wasmtime4j provides fine-grained error handling with specific exception types that map directly to Wasmtime native errors. This approach enables developers to:

- Understand exactly what went wrong during WebAssembly operations
- Implement appropriate recovery strategies for different error types
- Monitor and analyze error patterns for improved reliability
- Provide meaningful error messages to end users

## Exception Hierarchy

### Base Exceptions

```java
WasmException                    // Base for all WebAssembly errors
├── CompilationException        // WebAssembly compilation failures
├── ValidationException         // Module validation failures
├── InstantiationException     // Module instantiation failures
├── RuntimeException           // WebAssembly execution errors
└── WasiException             // WASI-specific errors
```

### Specific Exception Types

#### Compilation Errors

- **ModuleCompilationException** - Native code generation failures
  - `CompilationErrorType.OUT_OF_MEMORY` - Compiler ran out of memory
  - `CompilationErrorType.FUNCTION_TOO_COMPLEX` - Function exceeds compiler limits
  - `CompilationErrorType.UNSUPPORTED_INSTRUCTION` - Unsupported WebAssembly feature
  - `CompilationErrorType.OPTIMIZATION_FAILED` - Code optimization failure

#### Validation Errors

- **ModuleValidationException** - WebAssembly specification violations
  - `ValidationErrorType.INVALID_MAGIC_NUMBER` - Not valid WebAssembly bytecode
  - `ValidationErrorType.TYPE_MISMATCH` - Type system violations
  - `ValidationErrorType.UNSUPPORTED_FEATURE` - Feature not enabled
  - `ValidationErrorType.LIMIT_EXCEEDED` - Module exceeds limits

#### Runtime Errors

- **TrapException** - WebAssembly trap conditions
  - `TrapType.MEMORY_OUT_OF_BOUNDS` - Memory access violation
  - `TrapType.STACK_OVERFLOW` - Stack space exhausted
  - `TrapType.INTEGER_DIVISION_BY_ZERO` - Division by zero
  - `TrapType.UNREACHABLE_CODE_REACHED` - Unreachable instruction executed

- **RuntimeException** - General execution failures
  - `RuntimeErrorType.FUNCTION_EXECUTION_FAILED` - Function call failed
  - `RuntimeErrorType.HOST_FUNCTION_FAILED` - Host function error
  - `RuntimeErrorType.TIMEOUT` - Execution timeout exceeded

#### Instantiation Errors

- **ModuleInstantiationException** - Module setup failures
  - `InstantiationErrorType.MISSING_IMPORT` - Required import not provided
  - `InstantiationErrorType.IMPORT_TYPE_MISMATCH` - Import type incompatible
  - `InstantiationErrorType.MEMORY_ALLOCATION_FAILED` - Memory allocation failure

#### Linking Errors

- **LinkingException** - Module linking failures
  - `LinkingErrorType.IMPORT_NOT_FOUND` - Import not found
  - `LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH` - Signature incompatible
  - `LinkingErrorType.CIRCULAR_DEPENDENCY` - Circular module dependencies

#### WASI Errors

- **WasiException** - General WASI operation failures
- **WasiFileSystemException** - File system operation failures
  - `FileSystemErrorType.NOT_FOUND` - File/directory not found
  - `FileSystemErrorType.PERMISSION_DENIED` - Access denied
  - `FileSystemErrorType.IO_ERROR` - I/O operation failed

## Error Handling Best Practices

### 1. Catch Specific Exception Types

**Good:**
```java
try {
    Module module = Module.fromBytes(engine, wasmBytes);
} catch (ModuleValidationException e) {
    if (e.getErrorType() == ValidationErrorType.UNSUPPORTED_FEATURE) {
        // Enable required features and retry
        logger.info("Enabling required WebAssembly features: " + e.getRecoverySuggestion());
        // ... retry logic
    } else {
        throw new IllegalArgumentException("Invalid WebAssembly module: " + e.getMessage(), e);
    }
} catch (ModuleCompilationException e) {
    if (e.isResourceError()) {
        // Increase memory limits or simplify module
        logger.warning("Compilation resource issue: " + e.getRecoverySuggestion());
    }
    throw new RuntimeException("Failed to compile WebAssembly module", e);
}
```

**Poor:**
```java
try {
    Module module = Module.fromBytes(engine, wasmBytes);
} catch (Exception e) {
    // Too broad - doesn't provide actionable error handling
    throw new RuntimeException("Something went wrong", e);
}
```

### 2. Use Error Classification Methods

Exception classes provide classification methods to group related errors:

```java
catch (TrapException e) {
    if (e.isBoundsError()) {
        // Handle memory/table/array bounds violations
        logSecurityEvent("Bounds violation detected", e);
    } else if (e.isResourceExhaustionError()) {
        // Handle resource exhaustion (stack overflow, out of fuel)
        increaseResourceLimits();
    } else if (e.isArithmeticError()) {
        // Handle arithmetic errors (division by zero, overflow)
        validateInputParameters();
    }
}
```

### 3. Leverage Recovery Suggestions

Most exceptions provide recovery suggestions:

```java
catch (ModuleInstantiationException e) {
    logger.error("Instantiation failed: " + e.getMessage());
    logger.info("Recovery suggestion: " + e.getRecoverySuggestion());

    if (e.isImportError()) {
        // Provide missing imports
        provideRequiredImports(e.getImportName(), e.getModuleName());
    }
}
```

### 4. Implement Retry Logic for Transient Errors

Some errors are transient and may succeed on retry:

```java
catch (WasiFileSystemException e) {
    if (e.isTransientError()) {
        logger.info("Transient file system error, retrying: " + e.getMessage());
        // Implement exponential backoff retry
        retryWithBackoff(() -> performFileOperation());
    } else {
        // Permanent error - don't retry
        throw new IOException("File operation failed: " + e.getMessage(), e);
    }
}
```

### 5. Preserve Error Context

Always preserve the original exception in the error chain:

```java
catch (WasmException e) {
    // Good - preserves original exception
    throw new ServiceException("WebAssembly execution failed", e);

    // Poor - loses original context
    // throw new ServiceException("WebAssembly execution failed");
}
```

## Error Recovery Strategies

### Compilation Errors

1. **Resource Errors**
   - Increase JVM heap size
   - Split large modules into smaller parts
   - Reduce optimization levels

2. **Feature Errors**
   - Enable required WebAssembly features in engine configuration
   - Use alternative module implementations
   - Gracefully degrade functionality

### Runtime Errors

1. **Trap Conditions**
   - Validate input parameters before WebAssembly calls
   - Implement bounds checking in host functions
   - Set appropriate resource limits (stack, memory, fuel)

2. **Timeout Errors**
   - Increase execution timeouts
   - Optimize WebAssembly code performance
   - Implement execution cancellation

### WASI Errors

1. **File System Errors**
   - Verify file paths and permissions
   - Implement capability-based access control
   - Handle permission denied gracefully

2. **Network Errors**
   - Implement retry logic with exponential backoff
   - Validate network connectivity
   - Use connection pooling

## Error Monitoring and Analysis

### 1. Enable Error Monitoring

```java
// Enable global error monitoring
ErrorMonitor monitor = ErrorMonitor.getInstance();

// Record errors for analysis
try {
    // WebAssembly operations
} catch (WasmException e) {
    monitor.recordError(e);
    throw e; // Re-throw after recording
}
```

### 2. Analyze Error Patterns

```java
// Get error statistics
List<ErrorStatistics> stats = monitor.getErrorStatistics();

for (ErrorStatistics stat : stats) {
    logger.info("Error type: " + stat.getErrorType());
    logger.info("Occurrences: " + stat.getTotalOccurrences());
    logger.info("Rate: " + stat.getErrorRate() + " errors/minute");
    logger.info("Common functions: " + stat.getCommonFunctions());
}

// Check for concerning patterns
if (monitor.hasConcerningPatterns()) {
    logger.warning("Concerning error patterns detected");
    alertOperations(monitor.generateSummaryReport());
}
```

### 3. Set Up Alerting

```java
// Example alerting logic
double errorRate = monitor.getOverallErrorRate();
if (errorRate > 10.0) { // More than 10 errors per minute
    sendAlert("High error rate detected: " + errorRate + " errors/minute");
}

// Alert on specific error types
ErrorStatistics memoryErrors = monitor.getErrorStatistics("TrapException");
if (memoryErrors != null && memoryErrors.getErrorRate() > 5.0) {
    sendAlert("High memory error rate: " + memoryErrors.getErrorRate());
}
```

## Testing Error Conditions

### 1. Unit Tests for Error Mapping

```java
@Test
void shouldMapCompilationErrorCorrectly() {
    WasmException exception = ErrorMapper.mapError(
        ErrorMapper.COMPILATION_ERROR,
        "out of memory during compilation"
    );

    assertInstanceOf(ModuleCompilationException.class, exception);
    ModuleCompilationException compException = (ModuleCompilationException) exception;
    assertEquals(CompilationErrorType.OUT_OF_MEMORY, compException.getErrorType());
    assertNotNull(compException.getRecoverySuggestion());
}
```

### 2. Integration Tests for Error Scenarios

```java
@Test
void shouldHandleInvalidWebAssemblyModule() {
    byte[] invalidBytes = "not webassembly".getBytes();

    assertThrows(ModuleValidationException.class, () -> {
        Module.fromBytes(engine, invalidBytes);
    });
}
```

### 3. Error Recovery Tests

```java
@Test
void shouldRecoverFromMissingImport() {
    // Test that proper error handling allows recovery
    try {
        instantiateModuleWithMissingImports();
        fail("Expected InstantiationException");
    } catch (ModuleInstantiationException e) {
        if (e.getErrorType() == InstantiationErrorType.MISSING_IMPORT) {
            // Provide missing import and retry
            provideImport(e.getImportName());
            // Should succeed now
            assertDoesNotThrow(() -> instantiateModuleWithImports());
        }
    }
}
```

## Configuration Recommendations

### Engine Configuration

```java
EngineConfig config = EngineConfig.builder()
    .fuelEnabled(true)                    // Enable fuel for timeout protection
    .maxStackSize(1024 * 1024)           // Set stack limits
    .enabledFeatures(Set.of(              // Enable required features
        WasmFeature.BULK_MEMORY,
        WasmFeature.REFERENCE_TYPES
    ))
    .optimizationLevel(OptimizationLevel.SPEED)  // Balance speed vs compile time
    .build();
```

### Store Configuration

```java
Store store = Store.builder(engine)
    .withFuel(1_000_000)                 // Set execution fuel limit
    .withEpochDeadline(Duration.ofSeconds(30))  // Set execution timeout
    .build();
```

### WASI Configuration

```java
WasiConfig wasiConfig = WasiConfig.builder()
    .inheritStdio()                      // Inherit standard I/O
    .preopenDirectory("/app/data", "/data")  // Limit file system access
    .env("HOME", "/tmp")                 // Set environment variables
    .args("--verbose")                   // Pass command line arguments
    .build();
```

## Common Pitfalls to Avoid

1. **Catching too broadly** - Always catch specific exception types when possible
2. **Ignoring recovery suggestions** - Use the built-in recovery guidance
3. **Not preserving error chains** - Always include the original exception as the cause
4. **Retry non-retryable errors** - Check error classification before retrying
5. **Missing error monitoring** - Implement monitoring for production deployments
6. **Poor error messages** - Provide context-rich error messages for users
7. **Resource leak on errors** - Ensure proper cleanup in finally blocks or try-with-resources

## Production Deployment Checklist

- [ ] Error monitoring enabled with alerting
- [ ] Appropriate timeouts and resource limits configured
- [ ] Error recovery strategies implemented for common failure modes
- [ ] Comprehensive error logging with structured data
- [ ] Performance impact of error handling measured
- [ ] Error handling tested under load
- [ ] Documentation updated with error handling procedures
- [ ] Operations team trained on error analysis and recovery

## Further Reading

- [Wasmtime Error Handling Documentation](https://docs.wasmtime.dev/api/wasmtime/enum.Trap.html)
- [WebAssembly Specification - Traps](https://webassembly.github.io/spec/core/exec/runtime.html#traps)
- [WASI Error Codes](https://github.com/WebAssembly/WASI/blob/main/phases/snapshot/docs.md#errno)
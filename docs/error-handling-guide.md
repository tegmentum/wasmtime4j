# Error Handling and Diagnostics Guide

## Overview

This guide provides comprehensive information about error handling and diagnostics in wasmtime4j. The library implements a robust error handling system designed to prevent JVM crashes, provide meaningful error messages, and enable effective debugging of WebAssembly operations.

## Table of Contents

1. [Exception Hierarchy](#exception-hierarchy)
2. [Error Categories](#error-categories)
3. [Error Logging Framework](#error-logging-framework)
4. [Performance Diagnostics](#performance-diagnostics)
5. [Best Practices](#best-practices)
6. [Troubleshooting](#troubleshooting)
7. [Configuration](#configuration)

## Exception Hierarchy

wasmtime4j uses a structured exception hierarchy that provides clear categorization of different error types:

```
WasmException (base)
├── CompilationException
├── RuntimeException
├── ValidationException
├── ResourceException
├── SecurityException
├── InstantiationException
├── WasiException
│   ├── WasiComponentException
│   ├── WasiConfigurationException
│   └── WasiResourceException
```

### Base Exception: WasmException

All WebAssembly-related exceptions inherit from `WasmException`:

```java
try {
    Module module = Module.fromBytes(engine, wasmBytes);
} catch (WasmException e) {
    // Handle any WebAssembly-related error
    logger.error("WebAssembly operation failed: " + e.getMessage(), e);
}
```

### Specific Exception Types

#### CompilationException
Thrown when WebAssembly bytecode compilation fails:

```java
try {
    Module module = Module.fromBytes(engine, invalidWasmBytes);
} catch (CompilationException e) {
    System.err.println("Compilation failed: " + e.getMessage());
    // Check for common issues: invalid magic number, unsupported features
}
```

#### RuntimeException
Thrown during WebAssembly execution:

```java
try {
    Function function = instance.getFunction("main");
    Object[] result = function.call();
} catch (RuntimeException e) {
    System.err.println("Runtime error: " + e.getMessage());
    // Common causes: traps, stack overflow, invalid function calls
}
```

#### ValidationException
Thrown when WebAssembly module validation fails:

```java
try {
    Module.validate(engine, wasmBytes);
} catch (ValidationException e) {
    System.err.println("Validation failed: " + e.getMessage());
    // Check for structural issues, type mismatches, invalid references
}
```

#### ResourceException
Thrown when resource management operations fail:

```java
try {
    Memory memory = instance.getMemory("memory");
    memory.grow(1000); // Request too much memory
} catch (ResourceException e) {
    System.err.println("Resource error: " + e.getMessage());
    // Handle memory exhaustion, handle limits, cleanup failures
}
```

#### SecurityException
Thrown when security policy violations occur:

```java
try {
    WasiInstance wasi = wasiConfig.build(store);
    wasi.getFileSystem().readFile("/etc/passwd"); // Unauthorized access
} catch (SecurityException e) {
    System.err.println("Security violation: " + e.getMessage());
    // Handle unauthorized file access, capability violations
}
```

## Error Categories

### Compilation Errors

**Common Causes:**
- Invalid WebAssembly magic number (`0x00 0x61 0x73 0x6D`)
- Unsupported WebAssembly version
- Malformed sections
- Invalid instruction encoding

**Example Handling:**
```java
public Module compileModule(Engine engine, byte[] wasmBytes) {
    try {
        return Module.fromBytes(engine, wasmBytes);
    } catch (CompilationException e) {
        if (e.getMessage().contains("magic number")) {
            throw new IllegalArgumentException("Invalid WebAssembly file format", e);
        } else if (e.getMessage().contains("version")) {
            throw new UnsupportedOperationException("Unsupported WebAssembly version", e);
        }
        throw e; // Re-throw other compilation errors
    }
}
```

### Runtime Errors

**Common Causes:**
- WebAssembly traps (unreachable, division by zero)
- Stack overflow
- Invalid function signatures
- Memory access violations

**Example Handling:**
```java
public Object[] executeFunction(Function function, Object... args) {
    try {
        return function.call(args);
    } catch (RuntimeException e) {
        ErrorLogger.getLogger("Execution").logRuntimeError(e,
            function.getName(), getCurrentStackDepth());

        // Attempt recovery based on error type
        if (e.getMessage().contains("stack overflow")) {
            return recoverFromStackOverflow(function, args);
        }
        throw e;
    }
}
```

### Resource Management Errors

**Common Causes:**
- Memory allocation failures
- Handle exhaustion
- Resource cleanup failures
- Native library errors

**Example Handling:**
```java
public class ResourceManager implements AutoCloseable {
    private final List<WasmResource> resources = new ArrayList<>();

    public <T extends WasmResource> T manage(T resource) {
        resources.add(resource);
        return resource;
    }

    @Override
    public void close() {
        for (WasmResource resource : resources) {
            try {
                resource.close();
            } catch (ResourceException e) {
                ErrorLogger.getLogger("ResourceManagement")
                    .logResourceError(e, resource.getClass().getSimpleName(),
                                    resource.toString());
            }
        }
        resources.clear();
    }
}
```

## Error Logging Framework

wasmtime4j provides a comprehensive error logging framework based on `java.util.logging`:

### Basic Usage

```java
import ai.tegmentum.wasmtime4j.diagnostics.ErrorLogger;

ErrorLogger logger = ErrorLogger.getLogger("ModuleCompilation");

try {
    Module module = Module.fromBytes(engine, wasmBytes);
} catch (CompilationException e) {
    logger.logCompilationError(e, wasmBytes.length, compilationStartTime);
    throw e;
}
```

### Logger Categories

Different logger categories are available for different types of operations:

- `"Compilation"` - Module compilation errors
- `"Runtime"` - Execution-time errors
- `"Validation"` - Module validation errors
- `"Resource"` - Resource management errors
- `"Security"` - Security violation errors
- `"WASI"` - WASI-specific errors

### Error Metrics

The logging framework automatically collects metrics:

```java
ErrorLogger logger = ErrorLogger.getLogger("Compilation");
ErrorMetrics metrics = logger.getMetrics();

System.out.println("Total errors: " + metrics.getTotalErrorCount());
System.out.println("Average compilation time: " +
    metrics.getAverageCompilationErrorDuration() + "ms");
System.out.println("Error rate: " + metrics.getErrorRate() + " errors/sec");
```

## Performance Diagnostics

### Operation Tracking

Track performance of WebAssembly operations:

```java
import ai.tegmentum.wasmtime4j.diagnostics.PerformanceDiagnostics;

PerformanceDiagnostics diagnostics = PerformanceDiagnostics.getInstance();

String operationId = diagnostics.startOperation("ModuleCompilation");
try {
    Module module = Module.fromBytes(engine, wasmBytes);
    return module;
} finally {
    long duration = diagnostics.endOperation(operationId);
    System.out.println("Compilation took: " + duration + "ms");
}
```

### Performance Snapshots

Capture comprehensive performance information:

```java
PerformanceSnapshot snapshot = diagnostics.captureSnapshot();
System.out.println(snapshot.getFormattedReport());

// Example output:
// === WebAssembly Performance Snapshot ===
// Captured: 2025-09-21T10:30:00Z
//
// --- System Information ---
// Uptime: 300000 ms (5.0 minutes)
// Threads: 12 active, 8 daemon, 15 peak
//
// --- Memory Usage ---
// Heap: 256.0 MB / 1024.0 MB (25.0% used)
// Non-Heap: 64.0 MB / 256.0 MB (25.0% used)
//
// --- Operation Statistics ---
// ModuleCompilation: 150 ops, avg 45.2 ms, 3.3 ops/sec
// FunctionExecution: 5000 ops, avg 2.1 ms, 238.1 ops/sec
```

### Error Handling Overhead

Monitor the performance impact of error handling:

```java
double averageOverhead = diagnostics.getAverageErrorHandlingOverhead();
System.out.println("Error handling overhead: " + averageOverhead + "ms");
```

## Best Practices

### 1. Use Appropriate Exception Types

Catch specific exception types rather than the base `WasmException`:

```java
// Good
try {
    Module module = Module.fromBytes(engine, wasmBytes);
} catch (CompilationException e) {
    handleCompilationError(e);
} catch (ValidationException e) {
    handleValidationError(e);
}

// Avoid
try {
    Module module = Module.fromBytes(engine, wasmBytes);
} catch (WasmException e) {
    // Too broad - harder to handle appropriately
}
```

### 2. Log Errors with Context

Provide meaningful context when logging errors:

```java
ErrorLogger logger = ErrorLogger.getLogger("ModuleProcessing");

try {
    processModule(moduleName, wasmBytes);
} catch (CompilationException e) {
    logger.logCompilationError(e, wasmBytes.length, startTime);
    // Log additional context
    logger.logPerformanceDiagnostics("ModuleProcessing",
        Duration.between(startTime, Instant.now()).toMillis(),
        Map.of("moduleName", moduleName, "moduleSize", wasmBytes.length));
}
```

### 3. Implement Resource Cleanup

Always clean up resources, even when errors occur:

```java
Store store = new Store(engine);
try {
    Module module = Module.fromBytes(engine, wasmBytes);
    Instance instance = new Instance(store, module);
    // Use instance...
} catch (WasmException e) {
    ErrorLogger.getLogger("Resource").logResourceError(e, "Store", store.toString());
    throw e;
} finally {
    if (store != null) {
        store.close();
    }
}
```

### 4. Use Try-With-Resources

Leverage Java's try-with-resources for automatic cleanup:

```java
try (Store store = new Store(engine);
     Module module = Module.fromBytes(engine, wasmBytes);
     Instance instance = new Instance(store, module)) {

    Function function = instance.getFunction("main");
    return function.call();
} catch (WasmException e) {
    // Resources automatically cleaned up
    handleError(e);
    throw e;
}
```

### 5. Configure Appropriate Log Levels

Configure logging levels based on your environment:

```java
import ai.tegmentum.wasmtime4j.diagnostics.DiagnosticConfiguration;

DiagnosticConfiguration config = DiagnosticConfiguration.getInstance();

// Production: minimal logging
config.setGlobalLogLevel(Level.WARNING);
config.setPerformanceMonitoringEnabled(false);

// Development: detailed logging
config.setGlobalLogLevel(Level.FINE);
config.setPerformanceMonitoringEnabled(true);
config.setDetailedStackTracesEnabled(true);
```

## Troubleshooting

### Common Error Scenarios

#### Module Compilation Failures

**Symptoms:**
- `CompilationException` with "invalid magic number"
- `CompilationException` with "unsupported version"

**Solutions:**
```java
public void validateWebAssemblyFile(byte[] wasmBytes) {
    if (wasmBytes.length < 8) {
        throw new IllegalArgumentException("File too small to be valid WebAssembly");
    }

    // Check magic number: 0x00 0x61 0x73 0x6D
    if (wasmBytes[0] != 0x00 || wasmBytes[1] != 0x61 ||
        wasmBytes[2] != 0x73 || wasmBytes[3] != 0x6D) {
        throw new IllegalArgumentException("Invalid WebAssembly magic number");
    }

    // Check version: 0x01 0x00 0x00 0x00
    if (wasmBytes[4] != 0x01 || wasmBytes[5] != 0x00 ||
        wasmBytes[6] != 0x00 || wasmBytes[7] != 0x00) {
        throw new UnsupportedOperationException("Unsupported WebAssembly version");
    }
}
```

#### Memory Exhaustion

**Symptoms:**
- `ResourceException` with "memory allocation failed"
- Slow performance before errors

**Solutions:**
```java
public void configureMemoryLimits(EngineConfig config) {
    config.setMaxMemorySize(512 * 1024 * 1024); // 512 MB limit
    config.setMemoryGrowthEnabled(false); // Prevent unlimited growth
}

public void monitorMemoryUsage() {
    PerformanceSnapshot snapshot = PerformanceDiagnostics.getInstance().captureSnapshot();
    if (snapshot.getHeapUtilizationPercentage() > 80.0) {
        logger.warn("High memory usage: " + snapshot.getHeapUtilizationPercentage() + "%");
        // Trigger garbage collection or cleanup
        System.gc();
    }
}
```

#### Native Library Issues

**Symptoms:**
- `UnsatisfiedLinkError` during initialization
- `ResourceException` with native error codes

**Solutions:**
```java
public void diagnoseNativeLibraryIssues() {
    System.out.println("Java version: " + System.getProperty("java.version"));
    System.out.println("OS: " + System.getProperty("os.name") +
                      " " + System.getProperty("os.arch"));
    System.out.println("Library path: " + System.getProperty("java.library.path"));

    // Check if runtime detection is working
    RuntimeType runtime = RuntimeFactory.detectRuntime();
    System.out.println("Detected runtime: " + runtime);
}
```

### Debugging Performance Issues

#### Enable Performance Monitoring

```java
DiagnosticConfiguration config = DiagnosticConfiguration.getInstance();
config.setPerformanceMonitoringEnabled(true);
config.setSlowCompilationThreshold(500); // Log compilations > 500ms
config.setSlowRuntimeThreshold(50);      // Log executions > 50ms
```

#### Analyze Operation Statistics

```java
Map<String, OperationStatistics> stats =
    PerformanceDiagnostics.getInstance().getAllOperationStatistics();

stats.forEach((operationType, statistics) -> {
    System.out.printf("%s: %d operations, avg %.1fms, max %dms%n",
        operationType,
        statistics.getOperationCount(),
        statistics.getAverageDuration(),
        statistics.getMaxDuration());

    if (statistics.getMaxDuration() > 1000) {
        System.out.println("WARNING: Slow " + operationType + " detected");
    }
});
```

## Configuration

### Diagnostic Configuration

```java
import ai.tegmentum.wasmtime4j.diagnostics.DiagnosticConfiguration;

DiagnosticConfiguration config = DiagnosticConfiguration.getInstance();

// Enable features
config.setPerformanceMonitoringEnabled(true);
config.setDetailedStackTracesEnabled(true);
config.setErrorRecoveryLoggingEnabled(true);
config.setResourceTrackingEnabled(true);

// Set logging levels
config.setGlobalLogLevel(Level.INFO);
config.setErrorLogLevel(Level.SEVERE);
config.setPerformanceLogLevel(Level.FINE);

// Configure thresholds
config.setSlowCompilationThreshold(1000);  // 1 second
config.setSlowRuntimeThreshold(100);       // 100ms
config.setLargeModuleThreshold(5 * 1024 * 1024); // 5MB
```

### JUL Configuration

Configure `java.util.logging` for wasmtime4j loggers:

```properties
# logging.properties
ai.tegmentum.wasmtime4j.level=INFO
ai.tegmentum.wasmtime4j.error.level=SEVERE
ai.tegmentum.wasmtime4j.error.Compilation.level=WARNING
ai.tegmentum.wasmtime4j.error.Runtime.level=SEVERE

# Console handler
java.util.logging.ConsoleHandler.level=ALL
java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter
```

### Environment Variables

Control behavior via environment variables:

```bash
# Enable performance monitoring
export WASMTIME4J_PERFORMANCE_MONITORING=true

# Set log level
export WASMTIME4J_LOG_LEVEL=FINE

# Set memory thresholds
export WASMTIME4J_LARGE_MODULE_THRESHOLD=10485760  # 10MB
```

## Integration Examples

### Spring Boot Integration

```java
@Configuration
public class WasmConfig {

    @PostConstruct
    public void configureDiagnostics() {
        DiagnosticConfiguration config = DiagnosticConfiguration.getInstance();
        config.setPerformanceMonitoringEnabled(true);
        config.setGlobalLogLevel(Level.INFO);
    }

    @Bean
    public ErrorLogger errorLogger() {
        return ErrorLogger.getLogger("SpringBoot");
    }
}

@RestController
public class WasmController {

    @Autowired
    private ErrorLogger errorLogger;

    @PostMapping("/execute")
    public ResponseEntity<String> executeWasm(@RequestBody byte[] wasmBytes) {
        try {
            String operationId = PerformanceDiagnostics.getInstance()
                .startOperation("WebExecution");

            try (Engine engine = new Engine();
                 Store store = new Store(engine);
                 Module module = Module.fromBytes(engine, wasmBytes);
                 Instance instance = new Instance(store, module)) {

                Function main = instance.getFunction("main");
                Object[] result = main.call();
                return ResponseEntity.ok(Arrays.toString(result));

            } finally {
                PerformanceDiagnostics.getInstance().endOperation(operationId);
            }

        } catch (CompilationException e) {
            errorLogger.logCompilationError(e, wasmBytes.length, Instant.now());
            return ResponseEntity.badRequest().body("Compilation failed: " + e.getMessage());
        } catch (RuntimeException e) {
            errorLogger.logRuntimeError(e, "main", 0);
            return ResponseEntity.status(500).body("Execution failed: " + e.getMessage());
        }
    }
}
```

This error handling and diagnostics system provides robust protection against JVM crashes while enabling effective debugging and performance monitoring of WebAssembly operations in production environments.
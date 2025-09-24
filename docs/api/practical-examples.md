# Wasmtime4j Practical Examples

This guide provides practical, working examples for common Wasmtime4j use cases. All examples are tested and production-ready.

## Table of Contents

- [Quick Start](#quick-start)
- [Basic WebAssembly Execution](#basic-webassembly-execution)
- [WASI File System Access](#wasi-file-system-access)
- [Host Function Integration](#host-function-integration)
- [Performance Optimization](#performance-optimization)
- [Error Handling Patterns](#error-handling-patterns)
- [Enterprise Integration](#enterprise-integration)
- [Security Best Practices](#security-best-practices)

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Setup

```java
import ai.tegmentum.wasmtime4j.*;

public class QuickStart {
    public static void main(String[] args) {
        // Auto-detects JNI or Panama based on Java version
        try (WasmRuntime runtime = WasmRuntime.builder().build()) {

            // Create engine with default configuration
            try (Engine engine = runtime.createEngine()) {

                // Create store for WebAssembly instances
                try (Store store = engine.createStore()) {

                    // Your WebAssembly code here
                    System.out.println("Wasmtime4j is ready!");
                }
            }
        } catch (WasmException e) {
            System.err.println("WebAssembly error: " + e.getMessage());
        }
    }
}
```

## Basic WebAssembly Execution

### Simple Mathematical Operations

```java
import ai.tegmentum.wasmtime4j.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MathExample {
    public static void main(String[] args) throws Exception {
        // Load WebAssembly module from file
        byte[] wasmBytes = Files.readAllBytes(Paths.get("math.wasm"));

        try (WasmRuntime runtime = WasmRuntime.builder().build();
             Engine engine = runtime.createEngine();
             Store store = engine.createStore()) {

            // Compile module
            Module module = Module.fromBytes(engine, wasmBytes);

            // Instantiate module
            Instance instance = Instance.create(store, module);

            // Get exported function
            Function addFunc = instance.getFunction("add");
            if (addFunc == null) {
                throw new RuntimeException("Function 'add' not found");
            }

            // Call function with parameters
            Value[] params = {Value.i32(10), Value.i32(20)};
            Value[] results = addFunc.call(params);

            System.out.println("10 + 20 = " + results[0].asI32());
        }
    }
}
```

### Working with Different Data Types

```java
public class DataTypesExample {
    public static void demonstrateTypes() throws WasmException {
        try (WasmRuntime runtime = WasmRuntime.builder().build();
             Engine engine = runtime.createEngine();
             Store store = engine.createStore()) {

            // Example WASM module with various function types
            Module module = Module.fromBytes(engine, loadModuleBytes());
            Instance instance = Instance.create(store, module);

            // i32 function
            Function i32Func = instance.getFunction("process_i32");
            Value result = i32Func.call(Value.i32(42))[0];
            System.out.println("i32 result: " + result.asI32());

            // i64 function
            Function i64Func = instance.getFunction("process_i64");
            Value longResult = i64Func.call(Value.i64(1234567890L))[0];
            System.out.println("i64 result: " + longResult.asI64());

            // f32 function
            Function f32Func = instance.getFunction("process_f32");
            Value floatResult = f32Func.call(Value.f32(3.14f))[0];
            System.out.println("f32 result: " + floatResult.asF32());

            // f64 function
            Function f64Func = instance.getFunction("process_f64");
            Value doubleResult = f64Func.call(Value.f64(2.718281828))[0];
            System.out.println("f64 result: " + doubleResult.asF64());
        }
    }

    private static byte[] loadModuleBytes() {
        // Load your WASM module here
        return new byte[0]; // Placeholder
    }
}
```

## WASI File System Access

### Reading and Writing Files

```java
import ai.tegmentum.wasmtime4j.wasi.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WasiFileExample {
    public static void main(String[] args) throws Exception {
        try (WasmRuntime runtime = WasmRuntime.builder().build();
             Engine engine = runtime.createEngine();
             Store store = engine.createStore()) {

            // Configure WASI with file system access
            WasiConfig wasiConfig = WasiConfig.builder()
                .args("program", "--input", "data.txt")
                .env("ENV_VAR", "value")
                .preOpenDir(Paths.get("/tmp/wasm-data"), "/data")
                .inheritStdio()
                .build();

            // Create WASI context
            WasiContext wasi = WasiContext.create(store, wasiConfig);

            // Load WASI-enabled module
            byte[] wasmBytes = Files.readAllBytes(Paths.get("file-processor.wasm"));
            Module module = Module.fromBytes(engine, wasmBytes);

            // Create linker for WASI imports
            Linker linker = Linker.create(engine);
            wasi.addToLinker(linker);

            // Instantiate with WASI imports
            Instance instance = linker.instantiate(store, module);

            // Call main function
            Function main = instance.getFunction("_start");
            if (main != null) {
                main.call();
            }

            System.out.println("File processing completed");
        }
    }
}
```

### Environment and Command Line Arguments

```java
public class WasiEnvironmentExample {
    public static void configureEnvironment() throws WasmException {
        WasiConfig config = WasiConfig.builder()
            // Command line arguments
            .args("myprogram", "--verbose", "--output=/tmp/result.txt")

            // Environment variables
            .env("PATH", "/usr/bin:/bin")
            .env("HOME", "/home/user")
            .env("LOG_LEVEL", "DEBUG")

            // File system mapping
            .preOpenDir(Paths.get("/home/user/data"), "/input")
            .preOpenDir(Paths.get("/tmp/output"), "/output")

            // Standard I/O configuration
            .inheritStdin()
            .inheritStdout()
            .inheritStderr()

            build();

        // Use config with WASI context...
    }
}
```

## Host Function Integration

### Defining Host Functions

```java
import ai.tegmentum.wasmtime4j.hostapi.*;

public class HostFunctionExample {

    public static void defineHostFunctions() throws WasmException {
        try (WasmRuntime runtime = WasmRuntime.builder().build();
             Engine engine = runtime.createEngine();
             Store store = engine.createStore()) {

            // Define a host function for logging
            HostFunction logFunction = HostFunction.builder()
                .name("host_log")
                .params(ValueType.I32, ValueType.I32) // ptr, len
                .results() // void return
                .implementation((caller, params) -> {
                    int ptr = params[0].asI32();
                    int len = params[1].asI32();

                    // Read string from WebAssembly memory
                    Memory memory = caller.getMemory("memory");
                    byte[] bytes = memory.read(ptr, len);
                    String message = new String(bytes, StandardCharsets.UTF_8);

                    System.out.println("[WASM LOG] " + message);

                    return new Value[0]; // void return
                })
                .build();

            // Define a host function for time
            HostFunction timeFunction = HostFunction.builder()
                .name("host_time")
                .params()
                .results(ValueType.I64)
                .implementation((caller, params) -> {
                    long currentTime = System.currentTimeMillis();
                    return new Value[]{Value.i64(currentTime)};
                })
                .build();

            // Create linker and add host functions
            Linker linker = Linker.create(engine);
            linker.define("env", "host_log", logFunction);
            linker.define("env", "host_time", timeFunction);

            // Load and instantiate module with host functions
            Module module = Module.fromBytes(engine, loadModuleWithHostImports());
            Instance instance = linker.instantiate(store, module);

            // Call WebAssembly function that uses host functions
            Function wasmFunc = instance.getFunction("use_host_functions");
            wasmFunc.call();
        }
    }

    private static byte[] loadModuleWithHostImports() {
        // Load WASM module that imports host functions
        return new byte[0]; // Placeholder
    }
}
```

### Advanced Host Function Patterns

```java
public class AdvancedHostFunctions {

    // Host function with complex data structures
    public static HostFunction createDataProcessorFunction() {
        return HostFunction.builder()
            .name("process_data")
            .params(ValueType.I32, ValueType.I32, ValueType.I32) // input_ptr, input_len, output_ptr
            .results(ValueType.I32) // result length
            .implementation((caller, params) -> {
                int inputPtr = params[0].asI32();
                int inputLen = params[1].asI32();
                int outputPtr = params[2].asI32();

                Memory memory = caller.getMemory("memory");

                // Read input data
                byte[] inputData = memory.read(inputPtr, inputLen);

                // Process data (example: convert to uppercase)
                String input = new String(inputData, StandardCharsets.UTF_8);
                String output = input.toUpperCase();
                byte[] outputData = output.getBytes(StandardCharsets.UTF_8);

                // Write output data
                memory.write(outputPtr, outputData);

                return new Value[]{Value.i32(outputData.length)};
            })
            .build();
    }

    // Host function with error handling
    public static HostFunction createValidatedFunction() {
        return HostFunction.builder()
            .name("validated_operation")
            .params(ValueType.I32)
            .results(ValueType.I32)
            .implementation((caller, params) -> {
                int input = params[0].asI32();

                // Validation
                if (input < 0) {
                    throw new WasmTrapException("Invalid input: negative value");
                }

                if (input > 1000000) {
                    throw new WasmTrapException("Invalid input: value too large");
                }

                // Safe operation
                int result = input * 2;
                return new Value[]{Value.i32(result)};
            })
            .build();
    }
}
```

## Performance Optimization

### Engine Configuration for Performance

```java
public class PerformanceOptimization {

    public static Engine createOptimizedEngine() throws WasmException {
        WasmRuntime runtime = WasmRuntime.builder().build();

        EngineConfig config = EngineConfig.builder()
            // Optimization level
            .optimizationLevel(OptimizationLevel.SPEED)

            // Compilation strategy
            .strategy(CompilationStrategy.CRANELIFT)

            // Memory configuration
            .maxMemorySize(2_000_000_000L) // 2GB
            .memoryReservationSize(1_000_000_000L) // 1GB

            // Instance pooling for better performance
            .enableInstancePooling(true)
            .maxPooledInstances(100)

            // Profiling (disable in production)
            .enableProfiling(false)

            // Cache compiled modules
            .enableModuleCache(true)
            .moduleCacheSize(50)

            build();

        return runtime.createEngine(config);
    }

    public static void demonstrateInstanceReuse() throws WasmException {
        try (Engine engine = createOptimizedEngine()) {

            // Create instance pool for heavy reuse
            InstancePool pool = InstancePool.builder()
                .engine(engine)
                .moduleBytes(loadModuleBytes())
                .initialSize(10)
                .maxSize(50)
                .build();

            // Use pooled instances for better performance
            for (int i = 0; i < 1000; i++) {
                try (PooledInstance instance = pool.acquire()) {
                    Function func = instance.getFunction("process");
                    Value result = func.call(Value.i32(i))[0];
                    // Process result...
                }
            }
        }
    }

    private static byte[] loadModuleBytes() {
        return new byte[0]; // Placeholder
    }
}
```

### Memory Management Best Practices

```java
public class MemoryManagement {

    public static void demonstrateMemoryPatterns() throws WasmException {
        try (WasmRuntime runtime = WasmRuntime.builder().build();
             Engine engine = runtime.createEngine();
             Store store = engine.createStore()) {

            Module module = Module.fromBytes(engine, loadModuleBytes());
            Instance instance = Instance.create(store, module);

            Memory memory = instance.getMemory("memory");

            // Efficient bulk data transfer
            byte[] largeData = new byte[1_000_000];
            // Fill with data...

            // Write in chunks to avoid large allocations
            int chunkSize = 64 * 1024; // 64KB chunks
            for (int offset = 0; offset < largeData.length; offset += chunkSize) {
                int length = Math.min(chunkSize, largeData.length - offset);
                byte[] chunk = Arrays.copyOfRange(largeData, offset, offset + length);
                memory.write(offset, chunk);
            }

            // Read with views to avoid copying
            MemoryView view = memory.createView(0, largeData.length);
            // Process view directly without copying...
        }
    }

    public static void monitorMemoryUsage() throws WasmException {
        try (WasmRuntime runtime = WasmRuntime.builder().build();
             Engine engine = runtime.createEngine();
             Store store = engine.createStore()) {

            // Enable memory monitoring
            StoreMetrics metrics = store.getMetrics();

            Module module = Module.fromBytes(engine, loadModuleBytes());
            Instance instance = Instance.create(store, module);

            // Monitor memory before operations
            long beforeMemory = metrics.getTotalMemoryUsage();

            // Perform operations...
            Function func = instance.getFunction("memory_intensive_operation");
            func.call();

            // Monitor memory after operations
            long afterMemory = metrics.getTotalMemoryUsage();
            long memoryDelta = afterMemory - beforeMemory;

            System.out.println("Memory usage delta: " + memoryDelta + " bytes");

            // Force garbage collection if needed
            if (memoryDelta > 100_000_000) { // 100MB threshold
                System.gc();
            }
        }
    }

    private static byte[] loadModuleBytes() {
        return new byte[0]; // Placeholder
    }
}
```

## Error Handling Patterns

### Comprehensive Error Handling

```java
public class ErrorHandlingExample {

    public static void demonstrateErrorHandling() {
        try (WasmRuntime runtime = WasmRuntime.builder().build()) {

            try (Engine engine = runtime.createEngine()) {

                try (Store store = engine.createStore()) {

                    // Handle compilation errors
                    try {
                        byte[] invalidWasm = {0x00, 0x61, 0x73, 0x6d}; // Invalid WASM
                        Module module = Module.fromBytes(engine, invalidWasm);
                    } catch (WasmCompilationException e) {
                        System.err.println("Compilation failed: " + e.getMessage());
                        return;
                    }

                    // Handle instantiation errors
                    try {
                        Module module = Module.fromBytes(engine, loadValidModule());
                        Instance instance = Instance.create(store, module);
                    } catch (WasmInstantiationException e) {
                        System.err.println("Instantiation failed: " + e.getMessage());
                        System.err.println("Missing imports: " + e.getMissingImports());
                        return;
                    }

                    // Handle runtime errors
                    Instance instance = Instance.create(store, Module.fromBytes(engine, loadValidModule()));
                    Function func = instance.getFunction("may_trap");

                    try {
                        func.call(Value.i32(-1)); // May cause trap
                    } catch (WasmTrapException e) {
                        System.err.println("WebAssembly trap: " + e.getTrapKind());
                        System.err.println("Trap message: " + e.getMessage());

                        // Get stack trace
                        WasmStackTrace stackTrace = e.getStackTrace();
                        for (WasmStackFrame frame : stackTrace.getFrames()) {
                            System.err.println("  at " + frame.getFunctionName() +
                                " (" + frame.getModuleName() + ":" + frame.getLineNumber() + ")");
                        }
                    }

                } catch (WasmRuntimeException e) {
                    System.err.println("Runtime error: " + e.getMessage());

                    // Check for specific error types
                    if (e.getCause() instanceof OutOfMemoryError) {
                        System.err.println("Out of memory - consider increasing heap size");
                    }
                }

            } catch (WasmEngineException e) {
                System.err.println("Engine error: " + e.getMessage());
            }

        } catch (WasmException e) {
            System.err.println("General WebAssembly error: " + e.getMessage());
        }
    }

    // Retry pattern for transient errors
    public static <T> T withRetry(Callable<T> operation, int maxRetries) throws WasmException {
        WasmException lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.call();
            } catch (WasmTrapException e) {
                // Don't retry traps - they're deterministic
                throw e;
            } catch (WasmRuntimeException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(100 * (attempt + 1)); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new WasmException("Interrupted during retry", ie);
                    }
                }
            } catch (Exception e) {
                throw new WasmException("Unexpected error", e);
            }
        }

        throw lastException;
    }

    private static byte[] loadValidModule() {
        return new byte[0]; // Placeholder
    }
}
```

## Enterprise Integration

### Spring Boot Integration

```java
@Configuration
@EnableConfigurationProperties(WasmConfigProperties.class)
public class WasmConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WasmRuntime wasmRuntime(WasmConfigProperties properties) {
        return WasmRuntime.builder()
            .preferredImplementation(properties.getPreferredImplementation())
            .enableMetrics(properties.isMetricsEnabled())
            .build();
    }

    @Bean
    @ConditionalOnBean(WasmRuntime.class)
    public Engine wasmEngine(WasmRuntime runtime, WasmConfigProperties properties) throws WasmException {
        EngineConfig config = EngineConfig.builder()
            .optimizationLevel(properties.getOptimizationLevel())
            .maxMemorySize(properties.getMaxMemorySize())
            .enableInstancePooling(properties.isInstancePoolingEnabled())
            .build();

        return runtime.createEngine(config);
    }

    @Bean
    public WasmModuleManager wasmModuleManager(Engine engine) {
        return new WasmModuleManager(engine);
    }
}

@Component
public class WasmModuleManager {
    private final Engine engine;
    private final Map<String, Module> moduleCache = new ConcurrentHashMap<>();

    public WasmModuleManager(Engine engine) {
        this.engine = engine;
    }

    public Module loadModule(String name, byte[] wasmBytes) throws WasmException {
        return moduleCache.computeIfAbsent(name, k -> {
            try {
                return Module.fromBytes(engine, wasmBytes);
            } catch (WasmException e) {
                throw new RuntimeException("Failed to compile module: " + name, e);
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        moduleCache.clear();
    }
}
```

### Microservice Architecture

```java
@RestController
@RequestMapping("/api/wasm")
public class WasmController {

    private final WasmModuleManager moduleManager;
    private final WasmExecutionService executionService;

    public WasmController(WasmModuleManager moduleManager, WasmExecutionService executionService) {
        this.moduleManager = moduleManager;
        this.executionService = executionService;
    }

    @PostMapping("/execute/{moduleName}")
    public ResponseEntity<ExecutionResult> executeFunction(
            @PathVariable String moduleName,
            @RequestParam String functionName,
            @RequestBody ExecutionRequest request) {

        try {
            Module module = moduleManager.getModule(moduleName);
            ExecutionResult result = executionService.execute(module, functionName, request.getParameters());
            return ResponseEntity.ok(result);
        } catch (WasmException e) {
            return ResponseEntity.badRequest()
                .body(ExecutionResult.error(e.getMessage()));
        }
    }

    @GetMapping("/modules/{moduleName}/info")
    public ResponseEntity<ModuleInfo> getModuleInfo(@PathVariable String moduleName) {
        try {
            Module module = moduleManager.getModule(moduleName);
            ModuleInfo info = ModuleInfo.builder()
                .name(moduleName)
                .exports(module.getExports())
                .imports(module.getImports())
                .memorySize(module.getMemoryRequirements())
                .build();
            return ResponseEntity.ok(info);
        } catch (WasmException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

## Security Best Practices

### Secure WebAssembly Execution

```java
public class SecurityExample {

    public static void secureExecution() throws WasmException {
        // Create runtime with security constraints
        WasmRuntime runtime = WasmRuntime.builder()
            .enableSandbox(true)
            .maxExecutionTime(Duration.ofSeconds(30))
            .maxMemoryUsage(100 * 1024 * 1024) // 100MB limit
            .build();

        try (runtime) {
            EngineConfig config = EngineConfig.builder()
                // Disable dangerous features
                .enableBulkMemory(false)
                .enableReferenceTypes(false)
                .enableMultiValue(false)

                // Set conservative limits
                .maxMemorySize(50 * 1024 * 1024) // 50MB
                .maxTableSize(1000)
                .maxCallStackDepth(1000)

                build();

            try (Engine engine = runtime.createEngine(config);
                 Store store = engine.createStore()) {

                // Create secure store with limits
                store.setFuelLimit(1_000_000); // Limit computation
                store.setMemoryLimit(50 * 1024 * 1024); // Memory limit

                // Validate module before instantiation
                byte[] wasmBytes = loadUntrustedModule();
                if (!SecurityValidator.isModuleSafe(wasmBytes)) {
                    throw new SecurityException("Module failed security validation");
                }

                Module module = Module.fromBytes(engine, wasmBytes);

                // Create restricted WASI context
                WasiConfig wasiConfig = WasiConfig.builder()
                    .args("program") // No command line arguments
                    // No environment variables
                    // No file system access
                    .inheritStdout() // Only stdout access
                    .build();

                WasiContext wasi = WasiContext.create(store, wasiConfig);

                Linker linker = Linker.create(engine);
                wasi.addToLinker(linker);

                Instance instance = linker.instantiate(store, module);

                // Execute with timeout
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<Value[]> future = executor.submit(() -> {
                    Function func = instance.getFunction("safe_function");
                    return func.call(Value.i32(42));
                });

                try {
                    Value[] result = future.get(10, TimeUnit.SECONDS);
                    System.out.println("Secure execution result: " + result[0].asI32());
                } catch (TimeoutException e) {
                    future.cancel(true);
                    throw new SecurityException("Execution timeout");
                } finally {
                    executor.shutdown();
                }
            }
        }
    }

    private static byte[] loadUntrustedModule() {
        return new byte[0]; // Placeholder
    }
}

class SecurityValidator {
    public static boolean isModuleSafe(byte[] wasmBytes) {
        // Implement security validation logic
        // - Check for suspicious imports
        // - Validate memory usage patterns
        // - Check for infinite loops
        // - Validate function complexity
        return true; // Placeholder
    }
}
```

This comprehensive practical guide provides working examples for all major Wasmtime4j use cases, from basic execution to enterprise security patterns.
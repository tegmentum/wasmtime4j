# Wasmtime4j API Reference

This is the complete API reference for Wasmtime4j, providing detailed documentation and examples for all public interfaces.

## Core APIs

### Engine

The Engine is the compilation context for WebAssembly modules. It handles compilation, optimization, and resource management.

#### Basic Usage

```java
import ai.tegmentum.wasmtime4j.*;

// Create an engine with default configuration
try (Engine engine = Engine.create()) {
    // Use the engine for compilation...
}

// Create an engine with custom configuration
EngineConfig config = new EngineConfig()
    .optimizationLevel(OptimizationLevel.SPEED)
    .debugInfo(true)
    .parallelCompilation(true);

try (Engine engine = Engine.create(config)) {
    // Use the configured engine...
}
```

#### Thread Safety
Engines are thread-safe and can be shared across threads. Engine creation is expensive, so reuse engines when possible.

#### Resource Management
Always close engines when done or use try-with-resources. Closing an engine releases all associated native resources.

### WasmRuntime

The WasmRuntime is the main entry point for WebAssembly operations, abstracting implementation details between JNI and Panama FFI.

```java
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;

// Automatic runtime selection (recommended)
try (WasmRuntime runtime = WasmRuntimeFactory.create()) {

    // Create engine
    try (Engine engine = runtime.createEngine()) {

        // Load and compile module
        byte[] wasmBytes = loadWasmFromFile("example.wasm");
        Module module = runtime.compileModule(engine, wasmBytes);

        // Create store and instantiate
        Store store = runtime.createStore(engine);
        Instance instance = runtime.instantiate(module);

        // Use the instance...
    }
}

// Manual runtime selection
try (WasmRuntime jniRuntime = WasmRuntimeFactory.createJni()) {
    // Force JNI implementation
}

try (WasmRuntime panamaRuntime = WasmRuntimeFactory.createPanama()) {
    // Force Panama implementation (Java 23+ only)
}

// Check runtime information
RuntimeInfo info = runtime.getRuntimeInfo();
System.out.println("Runtime type: " + info.getRuntimeType()); // JNI or PANAMA
System.out.println("Wasmtime version: " + info.getWasmtimeVersion());
```

### Module

A Module represents compiled WebAssembly bytecode ready for instantiation.

```java
// Compile a module
byte[] wasmBytes = Files.readAllBytes(Paths.get("math.wasm"));
Module module = engine.compileModule(wasmBytes);

// Get module information
ModuleInfo info = module.getInfo();
System.out.println("Exports: " + info.getExports().size());
System.out.println("Imports: " + info.getImports().size());

// Check for specific exports
boolean hasAddFunction = module.hasExport("add");
ExportType addExportType = module.getExportType("add");

// Serialize/deserialize modules
if (engine.supportsModuleSerialization()) {
    ModuleSerializer serializer = engine.getModuleSerializer();
    SerializedModule serialized = serializer.serialize(module);

    // Later, deserialize
    Module deserializedModule = engine.deserializeModule(serialized);
}
```

### Store

A Store represents an execution context that holds runtime state for WebAssembly instances.

```java
// Create a store
Store store = engine.createStore();

// Create store with custom data
CustomStoreData storeData = new CustomStoreData();
Store storeWithData = engine.createStore(storeData);

// Configure store limits
store.setFuelConsumption(1000000); // Limit execution steps
store.setMaxMemorySize(64 * 1024 * 1024); // 64MB limit

// Get store statistics
StoreStatistics stats = store.getStatistics();
System.out.println("Fuel consumed: " + stats.getFuelConsumed());
System.out.println("Memory usage: " + stats.getMemoryUsage());
```

### Instance

An Instance represents an instantiated WebAssembly module with accessible exports.

```java
// Instantiate a module
Instance instance = module.instantiate(store);

// Get exported functions
Optional&lt;WasmFunction&gt; addFunc = instance.getFunction("add");
if (addFunc.isPresent()) {
    WasmValue[] result = addFunc.get().call(
        WasmValue.i32(5),
        WasmValue.i32(3)
    );
    int sum = result[0].asInt(); // 8
}

// Get exported memory
Optional&lt;WasmMemory&gt; memory = instance.getMemory("memory");
if (memory.isPresent()) {
    WasmMemory mem = memory.get();

    // Read from memory
    byte[] data = mem.read(0, 1024);

    // Write to memory
    mem.write(0, "Hello, WASM!".getBytes());

    // Grow memory
    long oldSize = mem.size();
    mem.grow(1); // Grow by 1 page (64KB)
    long newSize = mem.size();
}

// Get exported globals
Optional&lt;WasmGlobal&gt; global = instance.getGlobal("counter");
if (global.isPresent()) {
    WasmGlobal counter = global.get();
    WasmValue currentValue = counter.getValue();

    if (counter.isMutable()) {
        counter.setValue(WasmValue.i32(42));
    }
}

// Get all exports
Map&lt;String, Object&gt; exports = instance.getAllExports();
for (Map.Entry&lt;String, Object&gt; export : exports.entrySet()) {
    System.out.println("Export: " + export.getKey() + " = " + export.getValue());
}
```

## Host Functions and Imports

### Defining Host Functions

```java
// Define a simple host function
HostFunction logFunction = (args) -&gt; {
    int value = args[0].asInt();
    System.out.println("WASM logged: " + value);
    return new WasmValue[0]; // No return values
};

// Define host function with multiple parameters and return value
HostFunction mathFunction = (args) -&gt; {
    int a = args[0].asInt();
    int b = args[1].asInt();
    int result = a * b;
    return new WasmValue[] { WasmValue.i32(result) };
};

// Create import map
ImportMap imports = ImportMap.empty()
    .addFunction("env", "log", logFunction)
    .addFunction("math", "multiply", mathFunction);

// Instantiate with imports
Instance instance = module.instantiate(store, imports);
```

### Using Linker for Complex Imports

```java
// Create a linker
Linker linker = runtime.createLinker(engine);

// Define multiple host functions
linker.defineHostFunction("env", "log", (args) -&gt; {
    System.out.println("Log: " + args[0].asString());
    return new WasmValue[0];
});

linker.defineHostFunction("env", "random", (args) -&gt; {
    return new WasmValue[] { WasmValue.f64(Math.random()) };
});

// Define host memory
WasmMemory hostMemory = WasmMemory.create(MemoryType.create(1, 10));
linker.defineMemory("env", "memory", hostMemory);

// Define host globals
WasmGlobal hostGlobal = WasmGlobal.create(
    GlobalType.create(WasmType.I32, true),
    WasmValue.i32(100)
);
linker.defineGlobal("env", "counter", hostGlobal);

// Instantiate through linker
Instance instance = linker.instantiate(store, module);
```

## Component Model Support

### Working with Components

```java
// Load a component (WASM component model)
byte[] componentBytes = Files.readAllBytes(Paths.get("component.wasm"));
Component component = Component.compile(engine, componentBytes);

// Create component linker
ComponentLinker componentLinker = ComponentLinker.create(engine);

// Define component imports
componentLinker.defineFunction("imports", "log", (args) -&gt; {
    ComponentValue message = args[0];
    System.out.println("Component log: " + message.asString());
    return new ComponentValue[0];
});

// Instantiate component
ComponentInstance componentInstance = componentLinker.instantiate(store, component);

// Call component function
ComponentFunction processData = componentInstance.getFunction("process-data");
ComponentValue[] result = processData.call(
    ComponentValue.string("input data")
);
```

### Component Types and Values

```java
// Work with component types
ComponentType stringType = ComponentType.string();
ComponentType recordType = ComponentType.record()
    .field("name", ComponentType.string())
    .field("age", ComponentType.u32())
    .build();

// Create component values
ComponentValue stringValue = ComponentValue.string("Hello");
ComponentValue recordValue = ComponentValue.record()
    .field("name", "Alice")
    .field("age", 30)
    .build();

// Handle variants
ComponentType optionType = ComponentType.variant()
    .case("none", ComponentType.unit())
    .case("some", ComponentType.string())
    .build();

ComponentValue someValue = ComponentValue.variant("some", "data");
ComponentValue noneValue = ComponentValue.variant("none", ComponentValue.unit());
```

## Async and Reactive APIs

### Async Operations

```java
import ai.tegmentum.wasmtime4j.async.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// Async module compilation
CompletableFuture&lt;Module&gt; moduleF = AsyncModule.compileAsync(engine, wasmBytes);

// Chain async operations
CompletableFuture&lt;String&gt; resultF = moduleF
    .thenCompose(module -&gt; {
        Instance instance = module.instantiate(store);
        AsyncFunction processor = instance.getAsyncFunction("process_data");
        return processor.callAsync(WasmValue.string("large dataset"));
    })
    .thenApply(result -&gt; result[0].asString());

// Wait for completion with timeout
try {
    String result = resultF.get(30, TimeUnit.SECONDS);
    System.out.println("Async result: " + result);
} catch (TimeoutException e) {
    System.err.println("Operation timed out");
}
```

### Reactive Engine

```java
import ai.tegmentum.wasmtime4j.async.reactive.*;

// Create reactive engine
ReactiveEngine reactiveEngine = ReactiveEngine.create();

// Subscribe to compilation events
reactiveEngine.getCompilationEvents()
    .filter(event -&gt; event.getPhase() == CompilationPhase.COMPLETED)
    .subscribe(event -&gt; {
        System.out.println("Module compiled: " + event.getModuleName());
    });

// Subscribe to execution events
reactiveEngine.getExecutionEvents()
    .filter(event -&gt; event.getPhase() == ExecutionPhase.FUNCTION_CALL)
    .subscribe(event -&gt; {
        System.out.println("Function called: " + event.getFunctionName());
    });

// Compile module reactively
reactiveEngine.compileModuleReactive(wasmBytes)
    .subscribe(
        module -&gt; System.out.println("Module ready: " + module),
        error -&gt; System.err.println("Compilation failed: " + error),
        () -&gt; System.out.println("Compilation complete")
    );
```

## WASI Support

### Basic WASI Configuration

```java
import ai.tegmentum.wasmtime4j.wasi.*;

// Create WASI configuration
WasiConfig wasiConfig = WasiConfig.builder()
    .inheritEnv()           // Inherit environment variables
    .inheritStdin()         // Inherit stdin
    .inheritStdout()        // Inherit stdout
    .inheritStderr()        // Inherit stderr
    .arg("program")         // Program name
    .arg("--verbose")       // Command line arguments
    .preopenDir("/tmp", "/tmp")  // Map host directory
    .build();

// Create WASI context
WasiContext wasiContext = WasiContext.create(wasiConfig);

// Add WASI imports to linker
Linker linker = runtime.createLinker(engine);
wasiContext.addToLinker(linker);

// Instantiate WASI module
Instance wasiInstance = linker.instantiate(store, wasiModule);

// Run WASI program
Optional&lt;WasmFunction&gt; start = wasiInstance.getFunction("_start");
if (start.isPresent()) {
    start.get().call();
}
```

### WASI Preview 2 Support

```java
// Create WASI P2 context
WasiP2Context wasiP2Context = WasiP2Context.builder()
    .withFilesystem("/app", hostPath)
    .withNetwork("0.0.0.0:8080")
    .withKeyValue("config", configMap)
    .build();

// Use with component linker
ComponentLinker componentLinker = ComponentLinker.create(engine);
wasiP2Context.addToLinker(componentLinker);

ComponentInstance wasiComponent = componentLinker.instantiate(store, component);
```

## Performance and Monitoring

### Performance Monitoring

```java
import ai.tegmentum.wasmtime4j.performance.*;

// Create performance monitor
PerformanceMonitor monitor = PerformanceMonitor.create();

// Set performance thresholds
PerformanceThresholds thresholds = PerformanceThresholds.builder()
    .functionCallLatency(Duration.ofMillis(100))
    .memoryUsage(64 * 1024 * 1024) // 64MB
    .compilationTime(Duration.ofSeconds(5))
    .build();

monitor.setThresholds(thresholds);

// Add threshold violation listener
monitor.addThresholdListener((violation) -&gt; {
    System.err.println("Performance threshold violated: " +
        violation.getMetric() + " = " + violation.getValue());
});

// Monitor engine operations
monitor.startMonitoring(engine);

// Get performance report
PerformanceReport report = monitor.generateReport();
System.out.println("Average function call time: " +
    report.getAverageFunctionCallTime());
System.out.println("Peak memory usage: " +
    report.getPeakMemoryUsage());
```

### Profiling

```java
// Create profiler
WasmProfiler profiler = WasmProfiler.create();

// Configure profiling options
ProfilingOptions options = ProfilingOptions.builder()
    .enableFunctionProfiling(true)
    .enableMemoryProfiling(true)
    .samplingInterval(Duration.ofMillis(1))
    .build();

// Start profiling
profiler.startProfiling(instance, options);

// Execute profiled code
WasmFunction func = instance.getFunction("compute_intensive").get();
func.call(WasmValue.i32(1000000));

// Stop profiling and get report
ProfileReport profile = profiler.stopProfiling();

// Analyze profile
for (FunctionProfile funcProfile : profile.getFunctionProfiles()) {
    System.out.println("Function: " + funcProfile.getName());
    System.out.println("  Total time: " + funcProfile.getTotalTime());
    System.out.println("  Call count: " + funcProfile.getCallCount());
    System.out.println("  Average time: " + funcProfile.getAverageTime());
}
```

## Security and Sandboxing

### Security Configuration

```java
import ai.tegmentum.wasmtime4j.security.*;

// Create security policy
SecurityPolicy policy = SecurityPolicy.builder()
    .allowMemoryAccess(MemoryAccess.READ_WRITE)
    .allowFunction("env", "log")
    .denyFunction("env", "file_access")
    .setResourceLimits(ResourceLimits.builder()
        .maxMemory(64 * 1024 * 1024)
        .maxFuel(1000000)
        .maxStackDepth(1000)
        .build())
    .build();

// Create sandbox
Sandbox sandbox = Sandbox.builder()
    .withPolicy(policy)
    .withAuditLogging(true)
    .build();

// Run in sandbox
try {
    sandbox.execute(instance, (inst) -&gt; {
        WasmFunction func = inst.getFunction("untrusted_code").get();
        return func.call(WasmValue.i32(42));
    });
} catch (SecurityException e) {
    System.err.println("Security violation: " + e.getMessage());
}
```

### Security Auditing

```java
// Configure security audit
SecurityAuditConfig auditConfig = SecurityAuditConfig.builder()
    .logLevel(SecuritySeverity.INFO)
    .logDestination("/var/log/wasmtime4j-security.log")
    .includeStackTraces(true)
    .build();

SecurityAuditLog auditLog = SecurityAuditLog.create(auditConfig);

// Monitor security events
auditLog.addSecurityEventListener((event) -&gt; {
    if (event.getSeverity().ordinal() &gt;= SecuritySeverity.WARNING.ordinal()) {
        System.err.println("Security event: " + event.getDescription());
        System.err.println("Severity: " + event.getSeverity());
        System.err.println("Context: " + event.getContext());
    }
});

// Apply audit logging to instance
auditLog.monitor(instance);
```

## Error Handling and Diagnostics

### Exception Hierarchy

```java
try {
    Module module = engine.compileModule(invalidWasm);
} catch (CompilationException e) {
    System.err.println("Compilation failed: " + e.getMessage());

    // Get compilation diagnostics
    ErrorDiagnostics diagnostics = e.getDiagnostics();
    for (ValidationIssue issue : diagnostics.getValidationIssues()) {
        System.err.println("  Issue at " + issue.getLocation() + ": " +
            issue.getDescription());
    }
} catch (ValidationException e) {
    System.err.println("Module validation failed: " + e.getMessage());
} catch (RuntimeException e) {
    System.err.println("Runtime error: " + e.getMessage());

    // Get stack trace
    WasmStackTrace stackTrace = e.getWasmStackTrace();
    for (StackFrame frame : stackTrace.getFrames()) {
        System.err.println("  at " + frame.getFunctionName() +
            " (" + frame.getSourceLocation() + ")");
    }
} catch (WasmException e) {
    System.err.println("WebAssembly error: " + e.getMessage());
}
```

### Advanced Diagnostics

```java
// Enable detailed diagnostics
EngineConfig config = new EngineConfig()
    .debugInfo(true)
    .detailedDiagnostics(true);

Engine engine = Engine.create(config);

// Get runtime diagnostics
RuntimeMetrics metrics = engine.getRuntimeMetrics();
System.out.println("Total modules compiled: " + metrics.getModulesCompiled());
System.out.println("Total instances created: " + metrics.getInstancesCreated());
System.out.println("Memory pressure: " + metrics.getMemoryPressure());

// Get error context for debugging
try {
    // Some operation that might fail
    instance.getFunction("non_existent").get().call();
} catch (WasmException e) {
    ErrorContext context = e.getErrorContext();
    System.err.println("Error in module: " + context.getModuleName());
    System.err.println("Error in function: " + context.getFunctionName());
    System.err.println("Error at instruction: " + context.getInstructionOffset());

    // Get suggested fixes
    for (SuggestedFix fix : context.getSuggestedFixes()) {
        System.err.println("Suggestion: " + fix.getDescription());
    }
}
```

## Resource Management

### Resource Pools

```java
import ai.tegmentum.wasmtime4j.resource.*;

// Create engine pool
PoolConfiguration poolConfig = PoolConfiguration.builder()
    .initialSize(5)
    .maxSize(20)
    .maxIdleTime(Duration.ofMinutes(5))
    .validationQuery(() -&gt; true)
    .build();

ResourcePool&lt;Engine&gt; enginePool = ResourcePool.&lt;Engine&gt;builder()
    .configuration(poolConfig)
    .factory(() -&gt; Engine.create())
    .build();

// Use pooled resource
try (PooledResource&lt;Engine&gt; pooledEngine = enginePool.acquire()) {
    Engine engine = pooledEngine.getResource();

    // Use the engine...
    Module module = engine.compileModule(wasmBytes);

    // Resource automatically returned to pool when closed
}

// Get pool statistics
PoolStatistics stats = enginePool.getStatistics();
System.out.println("Active resources: " + stats.getActiveCount());
System.out.println("Idle resources: " + stats.getIdleCount());
System.out.println("Total acquisitions: " + stats.getTotalAcquisitions());
```

### Resource Caching

```java
// Create module cache
CacheConfiguration cacheConfig = CacheConfiguration.builder()
    .maxSize(100)
    .expireAfterAccess(Duration.ofHours(1))
    .evictionPolicy(EvictionPolicy.LRU)
    .build();

ResourceCache&lt;String, Module&gt; moduleCache = ResourceCache.&lt;String, Module&gt;builder()
    .configuration(cacheConfig)
    .loader((key) -&gt; {
        byte[] wasmBytes = loadWasmFromFile(key);
        return engine.compileModule(wasmBytes);
    })
    .build();

// Use cache
Module cachedModule = moduleCache.get("math.wasm");

// Cache statistics
CacheStatistics cacheStats = moduleCache.getStatistics();
System.out.println("Cache hit rate: " + cacheStats.getHitRate());
System.out.println("Cache evictions: " + cacheStats.getEvictionCount());
```

## Testing and Development

### Test Utilities

```java
import ai.tegmentum.wasmtime4j.test.*;

// Create test engine optimized for testing
Engine testEngine = TestUtils.createTestEngine();

// Generate test WebAssembly modules
byte[] testModule = TestUtils.generateTestModule("add", WasmType.I32, WasmType.I32);

// Assert implementation parity between JNI and Panama
TestUtils.assertImplementationParity(
    () -&gt; jniFunction.call(args),
    () -&gt; panamaFunction.call(args)
);

// Create test fixtures
TestFixture fixture = TestFixture.builder()
    .withModule("math.wasm")
    .withImports(testImports)
    .withExpectedExports("add", "subtract", "multiply")
    .build();

fixture.runTest((instance) -&gt; {
    WasmFunction add = instance.getFunction("add").get();
    WasmValue[] result = add.call(WasmValue.i32(2), WasmValue.i32(3));
    assertEquals(5, result[0].asInt());
});
```

### Benchmarking

```java
import ai.tegmentum.wasmtime4j.benchmark.*;

// Run built-in benchmarks
BenchmarkSuite suite = BenchmarkSuite.create();

// Add custom benchmark
suite.addBenchmark("module_compilation", () -&gt; {
    return engine.compileModule(wasmBytes);
});

// Run benchmarks comparing implementations
BenchmarkResult result = suite.runComparison(
    "JNI vs Panama",
    () -&gt; jniEngine.compileModule(wasmBytes),
    () -&gt; panamaEngine.compileModule(wasmBytes)
);

System.out.println("JNI time: " + result.getTime1());
System.out.println("Panama time: " + result.getTime2());
System.out.println("Speedup: " + result.getSpeedup());
```

## Platform-Specific Features

### Cross-Platform Compatibility

```java
import ai.tegmentum.wasmtime4j.platform.*;

// Check platform capabilities
PlatformOptimizations optimizations = PlatformOptimizations.detect();
System.out.println("SIMD support: " + optimizations.hasSimdSupport());
System.out.println("Multi-threading: " + optimizations.hasMultiThreading());
System.out.println("Bulk memory ops: " + optimizations.hasBulkMemoryOperations());

// Configure engine for platform
EngineConfig config = new EngineConfig()
    .simdSupport(optimizations.hasSimdSupport())
    .multiThreading(optimizations.hasMultiThreading())
    .bulkMemoryOperations(optimizations.hasBulkMemoryOperations());

Engine optimizedEngine = Engine.create(config);
```

### Native Library Management

```java
// Check native library version
NativeLibraryVersionManager versionManager =
    NativeLibraryVersionManager.getInstance();

String nativeVersion = versionManager.getNativeLibraryVersion();
String expectedVersion = versionManager.getExpectedVersion();

if (!nativeVersion.equals(expectedVersion)) {
    System.err.println("Warning: Native library version mismatch");
    System.err.println("Expected: " + expectedVersion);
    System.err.println("Actual: " + nativeVersion);
}

// Force native library reload (advanced use case)
versionManager.reloadNativeLibrary();
```
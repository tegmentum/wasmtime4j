# Wasmtime4j Usage Examples

This document provides comprehensive usage examples for wasmtime4j, demonstrating all major functionality including the latest API enhancements for 100% Wasmtime coverage.

## Table of Contents

1. [Basic WebAssembly Operations](#basic-webassembly-operations)
2. [Module Serialization and Deserialization](#module-serialization-and-deserialization)
3. [Advanced Store Configuration](#advanced-store-configuration)
4. [SIMD Operations](#simd-operations)
5. [Component Model](#component-model)
6. [WASI Enhanced Functionality](#wasi-enhanced-functionality)
7. [Debugging and Profiling](#debugging-and-profiling)
8. [Performance Optimization](#performance-optimization)
9. [Memory64 Support](#memory64-support)
10. [Exception Handling](#exception-handling)

## Basic WebAssembly Operations

### Creating an Engine and Store

```java
import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.exception.WasmException;

// Create engine with default configuration
try (Engine engine = Engine.create()) {

    // Create a store for execution context
    try (Store store = Store.create(engine)) {

        // Basic module compilation
        byte[] wasmBytes = loadWasmBytecode("module.wasm");
        Module module = Module.compile(engine, wasmBytes);

        // Create instance
        Instance instance = module.instantiate(store);

        // Call exported function
        WasmFunction addFunction = instance.getFunction("add").orElseThrow();
        WasmValue result = addFunction.call(store, WasmValue.i32(5), WasmValue.i32(3));
        System.out.println("Result: " + result.getI32());
    }
}
```

### Enhanced Engine Configuration

```java
// Create engine with custom configuration for production use
EngineConfig config = EngineConfig.builder()
    .withMemoryLimitPages(1024)           // 64MB memory limit
    .withStackSizeLimit(1024 * 1024)      // 1MB stack limit
    .enableFuel(true)                     // Enable fuel for timeouts
    .enableEpochInterruption(true)        // Enable epoch-based interrupts
    .withMaxInstances(100)                // Limit concurrent instances
    .build();

try (Engine engine = Engine.create(config)) {
    // Engine now enforces the specified limits

    // Check engine capabilities
    System.out.println("Stack limit: " + engine.getStackSizeLimit() + " bytes");
    System.out.println("Fuel enabled: " + engine.isFuelEnabled());

    // Check WebAssembly feature support
    if (engine.supportsFeature(WasmFeature.THREADS)) {
        System.out.println("Threads are supported");
    }

    if (engine.supportsFeature(WasmFeature.SIMD)) {
        System.out.println("SIMD operations are supported");
    }
}
```

## Module Serialization and Deserialization

### Using the Serializer Interface

```java
// Create a serializer for caching compiled modules
try (Serializer serializer = Serializer.create(
    1024 * 1024 * 10,   // 10MB cache
    true,               // Enable compression
    6                   // Compression level
)) {

    try (Engine engine = Engine.create()) {

        // Serialize a module for caching
        byte[] wasmBytes = loadWasmBytecode("compute_intensive.wasm");
        byte[] serializedModule = serializer.serialize(engine, wasmBytes);

        // Save to disk for later use
        saveToCache("compute_intensive.ser", serializedModule);

        // Later: deserialize from cache
        byte[] cachedData = loadFromCache("compute_intensive.ser");
        Module cachedModule = serializer.deserialize(engine, cachedData);

        // Cache statistics
        System.out.println("Cache entries: " + serializer.getCacheEntryCount());
        System.out.println("Cache size: " + serializer.getCacheTotalSize() + " bytes");
        System.out.println("Cache hit rate: " +
            String.format("%.2f%%", serializer.getCacheHitRate() * 100));
    }
}
```

### Direct Module Serialization

```java
try (Engine engine = Engine.create()) {

    // Compile module
    byte[] wasmBytes = loadWasmBytecode("module.wasm");
    Module module = Module.compile(engine, wasmBytes);

    // Check if module can be serialized
    if (module.isSerializable()) {

        // Serialize the compiled module
        byte[] serializedData = module.serialize();

        // Save for later use
        Files.write(Paths.get("module.cache"), serializedData);

        // Later: deserialize
        byte[] cachedData = Files.readAllBytes(Paths.get("module.cache"));
        Module deserializedModule = Module.deserialize(engine, cachedData);

        System.out.println("Module size: " + deserializedModule.getSizeBytes() + " bytes");
    }
}
```

## Advanced Store Configuration

### Fuel and Resource Management

```java
try (Engine engine = Engine.create()) {

    // Create store with custom limits
    try (Store store = Store.create(
        engine,
        1000000,        // Initial fuel: 1 million units
        64 * 1024 * 1024, // Memory limit: 64MB
        30              // Execution timeout: 30 seconds
    )) {

        // Set additional limits
        store.setMemoryLimit(32 * 1024 * 1024);     // 32MB memory
        store.setTableElementLimit(10000);          // 10K table elements
        store.setInstanceLimit(10);                 // 10 instances max

        // Monitor fuel consumption
        System.out.println("Initial fuel: " + store.getFuel());

        // Execute WebAssembly code
        Module module = Module.compile(engine, wasmBytes);
        Instance instance = module.instantiate(store);

        // Check fuel after execution
        System.out.println("Remaining fuel: " + store.getRemainingFuel());
        System.out.println("Total consumed: " + store.getTotalFuelConsumed());

        // Add more fuel if needed
        store.addFuel(500000);

        // Monitor execution statistics
        System.out.println("Function calls: " + store.getExecutionCount());
        System.out.println("Execution time: " +
            store.getTotalExecutionTimeMicros() + " microseconds");
    }
}
```

### Epoch-based Interruption

```java
try (Engine engine = Engine.create(
    EngineConfig.builder().enableEpochInterruption(true).build()
)) {

    try (Store store = Store.create(engine)) {

        // Set epoch deadline (interrupt after 1000 ticks)
        store.setEpochDeadline(1000);

        // Create a background thread to increment epoch
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                store.incrementEpoch();
            } catch (WasmException e) {
                // Handle epoch increment failure
            }
        }, 100, 100, TimeUnit.MILLISECONDS);

        try {
            // Execute potentially long-running WebAssembly code
            Module module = Module.compile(engine, wasmBytes);
            Instance instance = module.instantiate(store);

            // This will be interrupted if it runs too long
            WasmFunction longRunning = instance.getFunction("long_computation").orElseThrow();
            WasmValue result = longRunning.call(store);

        } catch (WasmException e) {
            if (e.getMessage().contains("epoch")) {
                System.out.println("Execution interrupted by epoch deadline");
            }
        } finally {
            scheduler.shutdown();
        }
    }
}
```

## SIMD Operations

### Basic SIMD Vector Operations

```java
import ai.tegmentum.wasmtime4j.SimdOperations;
import ai.tegmentum.wasmtime4j.SimdOperations.V128;

try (WasmRuntime runtime = WasmRuntimeFactory.create()) {

    // Create SIMD operations handler
    SimdOperations simd = SimdOperations.create(runtime);

    // Create vectors from different data types
    V128 vector1 = V128.fromInts(1, 2, 3, 4);
    V128 vector2 = V128.fromInts(5, 6, 7, 8);

    // Arithmetic operations
    V128 sum = simd.add(vector1, vector2);
    V128 difference = simd.subtract(vector1, vector2);
    V128 product = simd.multiply(vector1, vector2);

    // Display results
    int[] sumValues = sum.getAsInts();
    System.out.println("Sum: " + Arrays.toString(sumValues)); // [6, 8, 10, 12]

    // Logical operations
    V128 andResult = simd.and(vector1, vector2);
    V128 orResult = simd.or(vector1, vector2);
    V128 xorResult = simd.xor(vector1, vector2);
    V128 notResult = simd.not(vector1);

    // Comparison operations
    V128 equalMask = simd.equals(vector1, vector2);
    V128 lessThanMask = simd.lessThan(vector1, vector2);

    // Lane operations
    int firstLane = simd.extractLaneI32(vector1, 0);
    V128 modifiedVector = simd.replaceLaneI32(vector1, 0, 100);

    // Splat operations
    V128 splatVector = simd.splatI32(42);
    V128 floatSplat = simd.splatF32(3.14f);
}
```

### Advanced SIMD Operations

```java
// Create SIMD configuration for optimized performance
SimdOperations.SimdConfig config = SimdOperations.SimdConfig.builder()
    .enablePlatformOptimizations(true)
    .enableRelaxedOperations(true)
    .validateVectorOperands(false)    // Disable for performance
    .maxVectorWidth(256)              // Support AVX if available
    .build();

SimdOperations simd = new SimdOperations(config, runtime);

// Memory operations with WebAssembly memory
try (Store store = Store.create(engine)) {
    Module module = Module.compile(engine, wasmBytes);
    Instance instance = module.instantiate(store);

    WasmMemory memory = instance.getMemory("memory").orElseThrow();

    // Load vector from memory
    V128 loadedVector = simd.load(memory, 0);

    // Load with alignment for better performance
    V128 alignedVector = simd.loadAligned(memory, 16, 16);

    // Process vector data
    V128 processed = simd.multiply(loadedVector, V128.fromFloats(2.0f, 2.0f, 2.0f, 2.0f));

    // Store back to memory
    simd.store(memory, 32, processed);
    simd.storeAligned(memory, 48, processed, 16);

    // Type conversions
    V128 intVector = V128.fromInts(1, 2, 3, 4);
    V128 floatVector = simd.convertI32ToF32(intVector);
    V128 backToInt = simd.convertF32ToI32(floatVector);

    // Shuffle operations for complex data manipulation
    byte[] shuffleIndices = {0, 1, 2, 3, 8, 9, 10, 11, 4, 5, 6, 7, 12, 13, 14, 15};
    V128 shuffled = simd.shuffle(vector1, vector2, shuffleIndices);
}
```

## Component Model

### Component Compilation and Linking

```java
try (ComponentEngine componentEngine = WasmRuntimeFactory.create().createComponentEngine()) {

    // Compile individual components
    byte[] componentA = loadComponentBytecode("component-a.wasm");
    byte[] componentB = loadComponentBytecode("component-b.wasm");

    ComponentSimple compA = componentEngine.compileComponent(componentA, "ComponentA");
    ComponentSimple compB = componentEngine.compileComponent(componentB, "ComponentB");

    // Check component compatibility
    WitCompatibilityResult compatibility =
        componentEngine.checkCompatibility(compA, compB);

    if (compatibility.isCompatible()) {
        System.out.println("Components are compatible");

        // Link components together
        List<ComponentSimple> components = Arrays.asList(compA, compB);
        ComponentSimple linkedComponent = componentEngine.linkComponents(components);

        // Create component instance
        try (Store store = Store.create(componentEngine)) {
            ComponentInstance instance = componentEngine.createInstance(linkedComponent, store);

            // Access component interfaces
            if (instance.hasInterface("calculator")) {
                // Call component functions through WIT interface
                Object result = instance.callFunction("calculator", "add",
                    Arrays.asList(42, 58));
                System.out.println("Component calculation result: " + result);
            }
        }
    } else {
        System.out.println("Component incompatibility: " +
            compatibility.getIncompatibilityReasons());
    }
}
```

### Advanced Component Registry

```java
try (ComponentEngine engine = WasmRuntimeFactory.create().createComponentEngine()) {

    // Get the component registry
    ComponentRegistry registry = engine.getRegistry();

    // Register components with metadata
    ComponentMetadata metadata = ComponentMetadata.builder()
        .withName("math-utils")
        .withVersion("1.0.0")
        .withDescription("Mathematical utility functions")
        .withTags("math", "utilities")
        .build();

    byte[] componentBytes = loadComponentBytecode("math-utils.wasm");
    ComponentSimple component = engine.compileComponent(componentBytes);

    registry.register("math-utils", component, metadata);

    // Search for components
    ComponentSearchCriteria criteria = ComponentSearchCriteria.builder()
        .withTag("math")
        .withVersionConstraint(">=1.0.0")
        .build();

    List<ComponentSimple> foundComponents = registry.search(criteria);

    // Get registry statistics
    ComponentRegistryStatistics stats = registry.getStatistics();
    System.out.println("Registered components: " + stats.getComponentCount());
    System.out.println("Total interfaces: " + stats.getInterfaceCount());

    // Component lifecycle management
    ComponentLifecycleManager lifecycle = new ComponentLifecycleManager(registry);
    lifecycle.preloadComponents(Arrays.asList("math-utils"));
}
```

## WASI Enhanced Functionality

### WASI Linker with Enhanced Security

```java
import ai.tegmentum.wasmtime4j.wasi.*;

try (Engine engine = Engine.create()) {

    // Create WASI configuration with security policies
    WasiConfig wasiConfig = WasiConfig.builder()
        .withFileSystemAccess("/safe/directory")
        .withEnvironmentAccess(Arrays.asList("HOME", "USER"))
        .withNetworkAccess(false)
        .withMaxFileDescriptors(100)
        .withPreopenDirectories(Map.of("/data", "/host/data"))
        .build();

    // Create WASI linker with configuration
    try (WasiLinker linker = WasmRuntimeFactory.create()
        .createWasiLinker(engine, wasiConfig)) {

        // Define custom WASI modules if needed
        linker.defineWasiModule("wasi_snapshot_preview1");

        // Compile and link WASI module
        Module wasiModule = Module.compile(engine, loadWasmBytecode("wasi_app.wasm"));

        try (Store store = Store.create(engine)) {
            Instance instance = linker.instantiate(store, wasiModule);

            // Call WASI main function
            WasmFunction mainFunc = instance.getFunction("_start").orElseThrow();
            mainFunc.call(store);

            // Access WASI-specific functionality
            if (instance.hasExport("wasi_get_args")) {
                // Handle WASI command line arguments
            }
        }
    }
}
```

## Debugging and Profiling

### Using the Debugger Interface

```java
import ai.tegmentum.wasmtime4j.debug.*;

try (Engine engine = Engine.create()) {

    if (WasmRuntimeFactory.create().isDebuggingSupported()) {

        // Create debugger
        try (Debugger debugger = WasmRuntimeFactory.create().createDebugger(engine)) {

            // Set breakpoints
            debugger.setBreakpoint("module_name", "function_name", 42);

            // Configure debug settings
            debugger.enableStepByStep(true);
            debugger.enableVariableInspection(true);

            // Compile module with debug information
            byte[] wasmBytes = loadWasmBytecode("debug_module.wasm");
            Module module = Module.compile(engine, wasmBytes);

            try (Store store = Store.create(engine)) {

                // Attach debugger to store
                debugger.attachToStore(store);

                Instance instance = module.instantiate(store);

                // Execute with debugging
                WasmFunction debugFunction = instance.getFunction("debug_me").orElseThrow();

                debugger.setDebugCallback(new DebugCallback() {
                    @Override
                    public void onBreakpoint(String module, String function, int line) {
                        System.out.println(String.format(
                            "Breakpoint hit: %s::%s at line %d", module, function, line));

                        // Inspect variables
                        Map<String, Object> locals = debugger.getLocalVariables();
                        locals.forEach((name, value) ->
                            System.out.println(name + " = " + value));
                    }

                    @Override
                    public void onStep(String module, String function, int line) {
                        System.out.println(String.format(
                            "Step: %s::%s at line %d", module, function, line));
                    }
                });

                // Execute function - will trigger debug callbacks
                WasmValue result = debugFunction.call(store);
            }
        }
    }
}
```

### Performance Profiling

```java
import ai.tegmentum.wasmtime4j.profiling.*;

try (Engine engine = Engine.create()) {

    // Create performance profiler
    try (PerformanceProfiler profiler = PerformanceProfiler.create(engine)) {

        // Configure profiling
        profiler.enableFunctionProfiling(true);
        profiler.enableMemoryProfiling(true);
        profiler.setSamplingInterval(1000); // 1ms

        try (Store store = Store.create(engine)) {

            // Start profiling session
            profiler.startProfiling(store);

            Module module = Module.compile(engine, wasmBytes);
            Instance instance = module.instantiate(store);

            // Execute code to profile
            WasmFunction targetFunction = instance.getFunction("compute_heavy").orElseThrow();

            long startTime = System.nanoTime();
            WasmValue result = targetFunction.call(store);
            long endTime = System.nanoTime();

            // Stop profiling and get results
            ProfilingResults results = profiler.stopProfiling();

            // Analyze results
            System.out.println("Execution time: " +
                (endTime - startTime) / 1_000_000.0 + " ms");

            results.getFunctionProfiles().forEach((funcName, profile) -> {
                System.out.println(String.format(
                    "Function %s: %d calls, %.2f ms total",
                    funcName, profile.getCallCount(), profile.getTotalTimeMs()));
            });

            // Memory profiling results
            MemoryProfile memProfile = results.getMemoryProfile();
            System.out.println("Peak memory usage: " + memProfile.getPeakUsage() + " bytes");
            System.out.println("Allocations: " + memProfile.getAllocationCount());
        }
    }
}
```

## Performance Optimization

### Optimized Runtime Configuration

```java
// Create runtime with performance-optimized configuration
WasmRuntimeBuilder builder = WasmRuntimeBuilder.builder()
    .withOptimizationLevel(OptimizationLevel.SPEED)
    .enableJitCompilation(true)
    .withCompilerThreads(Runtime.getRuntime().availableProcessors())
    .enableInlining(true)
    .withMemoryPooling(true)
    .build();

try (WasmRuntime runtime = builder.build()) {

    // Use auto-configuration for best performance
    AutoConfig autoConfig = AutoConfig.detectOptimal();
    Engine engine = runtime.createEngine(autoConfig.getEngineConfig());

    // Performance profiling
    PerformanceProfile profile = PerformanceProfile.measure(() -> {
        try (Store store = Store.create(engine)) {
            Module module = Module.compile(engine, wasmBytes);
            Instance instance = module.instantiate(store);

            // Batch operations for better performance
            WasmFunction func = instance.getFunction("batch_process").orElseThrow();
            for (int i = 0; i < 1000; i++) {
                func.call(store, WasmValue.i32(i));
            }
        }
    });

    System.out.println("Operations/second: " + profile.getOperationsPerSecond());
    System.out.println("Average latency: " + profile.getAverageLatencyMs() + " ms");
}
```

## Memory64 Support

### Working with 64-bit Memory

```java
import ai.tegmentum.wasmtime4j.Memory64*;

// Check Memory64 support
if (engine.supportsFeature(WasmFeature.MEMORY64)) {

    // Create Memory64 configuration
    Memory64Config config = Memory64Config.builder()
        .withInitialPages(100)
        .withMaximumPages(1000)
        .enableGuardPages(true)
        .build();

    try (Store store = Store.create(engine)) {

        // Create Memory64 instance
        Memory64Type memType = Memory64Type.builder()
            .withMinimum(config.getInitialPages())
            .withMaximum(config.getMaximumPages())
            .build();

        WasmMemory memory64 = store.createMemory64(memType);

        // Work with large address spaces
        long largeOffset = 0x1_0000_0000L; // 4GB offset

        // Check compatibility
        Memory64Compatibility compat = Memory64Compatibility.check(memory64);
        if (compat.isFullySupported()) {

            // Use Memory64 instructions
            Memory64InstructionHandler handler = new Memory64InstructionHandler(memory64);

            // 64-bit memory operations
            handler.store64(largeOffset, 0x123456789ABCDEFL);
            long value = handler.load64(largeOffset);

            System.out.println("Stored and loaded 64-bit value: 0x" +
                Long.toHexString(value));
        }
    }
}
```

## Exception Handling

### Advanced Exception Processing

```java
import ai.tegmentum.wasmtime4j.experimental.*;

// Check if exception handling is supported
if (engine.supportsFeature(WasmFeature.EXCEPTION_HANDLING)) {

    try (Store store = Store.create(engine)) {

        // Create exception handler
        ExceptionHandler exceptionHandler = new ExceptionHandler(store);

        // Define exception types
        ExceptionInstructions.ExceptionType divByZero =
            ExceptionInstructions.createExceptionType("divide_by_zero");

        exceptionHandler.registerExceptionType(divByZero);

        // Compile module with exception handling
        Module module = Module.compile(engine, wasmBytesWithExceptions);
        Instance instance = module.instantiate(store);

        try {
            WasmFunction riskyFunction = instance.getFunction("divide").orElseThrow();
            WasmValue result = riskyFunction.call(store,
                WasmValue.i32(10), WasmValue.i32(0));

        } catch (WasmException e) {

            // Handle WebAssembly exceptions
            if (exceptionHandler.isWasmException(e)) {
                ExceptionInstructions.WasmExceptionInfo info =
                    exceptionHandler.getExceptionInfo(e);

                System.out.println("WebAssembly exception: " + info.getType());
                System.out.println("Exception data: " +
                    Arrays.toString(info.getPayload()));

                // Exception marshaling for complex data
                ExceptionMarshaling marshaling = new ExceptionMarshaling();
                Object marshaledData = marshaling.marshalException(info);
            }
        }
    }
}
```

## Best Practices Summary

### Resource Management
```java
// Always use try-with-resources for automatic cleanup
try (Engine engine = Engine.create();
     Store store = Store.create(engine);
     Module module = Module.compile(engine, wasmBytes)) {

    // Your WebAssembly code here

} // All resources automatically closed
```

### Error Handling
```java
try {
    // WebAssembly operations
} catch (WasmException e) {
    // Handle WebAssembly-specific errors
    logger.error("WebAssembly error: " + e.getMessage(), e);
} catch (IllegalArgumentException e) {
    // Handle parameter validation errors
    logger.error("Invalid parameters: " + e.getMessage(), e);
}
```

### Performance Tips
```java
// 1. Reuse engines and modules
Engine sharedEngine = Engine.create();
Module compiledModule = Module.compile(sharedEngine, wasmBytes);

// 2. Use module serialization for caching
if (module.isSerializable()) {
    byte[] cached = module.serialize();
    // Save cached data
}

// 3. Configure appropriate limits
Store store = Store.create(engine,
    1_000_000,      // Reasonable fuel limit
    64 * 1024 * 1024, // 64MB memory limit
    30              // 30 second timeout
);

// 4. Use SIMD for data-parallel operations
if (runtime.isSimdSupported()) {
    SimdOperations simd = SimdOperations.create(runtime);
    // Vectorized operations
}
```

This comprehensive guide demonstrates the full range of wasmtime4j capabilities, from basic WebAssembly operations to advanced features like component model, SIMD operations, and debugging support.
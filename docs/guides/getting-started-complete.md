# Getting Started with Wasmtime4j

This comprehensive getting started guide will take you from installation to running your first WebAssembly modules with Wasmtime4j.

## Table of Contents

1. [Installation](#installation)
2. [Quick Start](#quick-start)
3. [Basic Concepts](#basic-concepts)
4. [Step-by-Step Tutorial](#step-by-step-tutorial)
5. [Common Patterns](#common-patterns)
6. [Next Steps](#next-steps)

## Installation

### Prerequisites

- **Java 8+** for JNI implementation
- **Java 23+** for Panama FFI implementation (recommended for best performance)
- **Maven 3.6+** or **Gradle 6.0+** for dependency management

### Maven Dependency

Add the Wasmtime4j dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle Dependency

Add the dependency to your `build.gradle`:

```gradle
implementation 'ai.tegmentum:wasmtime4j:1.0.0'
```

### Verification

Create a simple test to verify your installation:

```java
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.RuntimeInfo;

public class InstallationTest {
    public static void main(String[] args) {
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            RuntimeInfo info = runtime.getRuntimeInfo();
            System.out.println("Wasmtime4j is working!");
            System.out.println("Runtime type: " + info.getRuntimeType());
            System.out.println("Wasmtime version: " + info.getWasmtimeVersion());
        } catch (Exception e) {
            System.err.println("Installation verification failed: " + e.getMessage());
        }
    }
}
```

If this runs successfully and prints runtime information, your installation is complete!

## Quick Start

Let's start with the most basic example - loading and running a simple WebAssembly module:

```java
import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.nio.file.Files;
import java.nio.file.Paths;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        // Load WebAssembly bytecode
        byte[] wasmBytes = createSimpleAddModule(); // We'll define this below

        // Create runtime and engine
        try (WasmRuntime runtime = WasmRuntimeFactory.create();
             Engine engine = runtime.createEngine()) {

            // Compile the WebAssembly module
            Module module = runtime.compileModule(engine, wasmBytes);

            // Create a store (execution context)
            Store store = runtime.createStore(engine);

            // Instantiate the module
            Instance instance = runtime.instantiate(module);

            // Get the exported function
            WasmFunction addFunction = instance.getFunction("add")
                .orElseThrow(() -> new RuntimeException("Function 'add' not found"));

            // Call the function
            WasmValue[] result = addFunction.call(
                WasmValue.i32(5),
                WasmValue.i32(3)
            );

            // Print the result
            System.out.println("5 + 3 = " + result[0].asInt()); // Outputs: 5 + 3 = 8
        }
    }

    // Helper method to create a simple WebAssembly module
    // In practice, you'd load this from a .wasm file
    private static byte[] createSimpleAddModule() {
        // This is a hand-crafted WASM module that exports an "add" function
        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // Magic number "\0asm"
            0x01, 0x00, 0x00, 0x00, // Version 1
            // Function type section
            0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f,
            // Function section
            0x03, 0x02, 0x01, 0x00,
            // Export section
            0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00,
            // Code section
            0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b
        };
    }
}
```

## Basic Concepts

Before diving deeper, let's understand the key concepts in Wasmtime4j:

### 1. Runtime
- **Purpose**: Main entry point that abstracts JNI vs Panama implementation
- **Lifecycle**: Create once, use throughout your application
- **Thread Safety**: Thread-safe, can be shared across threads

### 2. Engine
- **Purpose**: Compilation context for WebAssembly modules
- **Lifecycle**: Can be reused for multiple modules
- **Configuration**: Supports optimization levels, debugging, and feature flags

### 3. Module
- **Purpose**: Compiled WebAssembly bytecode ready for instantiation
- **Reusability**: Can be instantiated multiple times
- **Validation**: Automatically validated during compilation

### 4. Store
- **Purpose**: Execution context that holds runtime state
- **Isolation**: Each store provides isolated execution environment
- **Resources**: Manages memory, globals, and execution limits

### 5. Instance
- **Purpose**: Instantiated module with accessible exports
- **State**: Contains actual runtime state and memory
- **Exports**: Provides access to functions, memory, globals, and tables

## Step-by-Step Tutorial

### Step 1: Creating Your First WebAssembly Module

Let's start by creating a simple WebAssembly module. You can either:

1. **Use an existing .wasm file**
2. **Create one using WebAssembly Text Format (WAT)**
3. **Compile from a high-level language**

Here's a simple WAT file (`math.wat`):

```wat
(module
  (func $add (param $a i32) (param $b i32) (result i32)
    local.get $a
    local.get $b
    i32.add
  )
  (func $multiply (param $a i32) (param $b i32) (result i32)
    local.get $a
    local.get $b
    i32.mul
  )
  (export "add" (func $add))
  (export "multiply" (func $multiply))
)
```

Convert it to bytecode using the WebAssembly Binary Toolkit (WABT):

```bash
wat2wasm math.wat -o math.wasm
```

### Step 2: Loading and Using the Module

```java
import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MathModuleExample {
    public static void main(String[] args) throws Exception {
        // Load the compiled WebAssembly module
        byte[] wasmBytes = Files.readAllBytes(Paths.get("math.wasm"));

        try (WasmRuntime runtime = WasmRuntimeFactory.create();
             Engine engine = runtime.createEngine()) {

            // Compile the module
            Module module = runtime.compileModule(engine, wasmBytes);

            // Create store and instantiate
            Store store = runtime.createStore(engine);
            Instance instance = runtime.instantiate(module);

            // Test the add function
            WasmFunction addFunc = instance.getFunction("add").get();
            WasmValue[] addResult = addFunc.call(
                WasmValue.i32(10),
                WasmValue.i32(5)
            );
            System.out.println("10 + 5 = " + addResult[0].asInt());

            // Test the multiply function
            WasmFunction multiplyFunc = instance.getFunction("multiply").get();
            WasmValue[] multiplyResult = multiplyFunc.call(
                WasmValue.i32(7),
                WasmValue.i32(6)
            );
            System.out.println("7 * 6 = " + multiplyResult[0].asInt());
        }
    }
}
```

### Step 3: Working with Memory

WebAssembly modules often need to work with linear memory. Here's how:

```java
public class MemoryExample {
    public static void main(String[] args) throws Exception {
        // Assume we have a WASM module that exports memory and string functions
        byte[] wasmBytes = loadStringProcessingModule();

        try (WasmRuntime runtime = WasmRuntimeFactory.create();
             Engine engine = runtime.createEngine()) {

            Module module = runtime.compileModule(engine, wasmBytes);
            Store store = runtime.createStore(engine);
            Instance instance = runtime.instantiate(module);

            // Get the exported memory
            WasmMemory memory = instance.getMemory("memory")
                .orElseThrow(() -> new RuntimeException("Memory not exported"));

            // Write a string to memory
            String input = "Hello, WebAssembly!";
            byte[] inputBytes = input.getBytes("UTF-8");

            // Allocate memory in the WASM module (assuming it exports an allocate function)
            WasmFunction allocate = instance.getFunction("allocate").get();
            WasmValue[] allocResult = allocate.call(WasmValue.i32(inputBytes.length));
            int address = allocResult[0].asInt();

            // Write the string to allocated memory
            memory.write(address, inputBytes);

            // Call a function that processes the string
            WasmFunction processString = instance.getFunction("process_string").get();
            WasmValue[] processResult = processString.call(
                WasmValue.i32(address),
                WasmValue.i32(inputBytes.length)
            );

            // Read the result back from memory
            int resultAddress = processResult[0].asInt();
            int resultLength = processResult[1].asInt();
            byte[] resultBytes = memory.read(resultAddress, resultLength);
            String result = new String(resultBytes, "UTF-8");

            System.out.println("Processed string: " + result);

            // Don't forget to free the memory (if the module provides a free function)
            WasmFunction free = instance.getFunction("free").orElse(null);
            if (free != null) {
                free.call(WasmValue.i32(address));
                free.call(WasmValue.i32(resultAddress));
            }
        }
    }

    private static byte[] loadStringProcessingModule() {
        // Load your string processing WASM module here
        // This would typically be compiled from C, Rust, or AssemblyScript
        throw new UnsupportedOperationException("Implement this based on your module");
    }
}
```

### Step 4: Host Functions (Calling Java from WebAssembly)

Often you'll want WebAssembly modules to call back into your Java code:

```java
public class HostFunctionExample {
    public static void main(String[] args) throws Exception {
        // Create a WASM module that imports functions from the host
        byte[] wasmBytes = loadModuleWithImports();

        try (WasmRuntime runtime = WasmRuntimeFactory.create();
             Engine engine = runtime.createEngine()) {

            Module module = runtime.compileModule(engine, wasmBytes);
            Store store = runtime.createStore(engine);

            // Define host functions that the WASM module can call
            HostFunction logFunction = (args) -> {
                int value = args[0].asInt();
                System.out.println("WASM logged: " + value);
                return new WasmValue[0]; // No return value
            };

            HostFunction getCurrentTime = (args) -> {
                long currentTime = System.currentTimeMillis();
                return new WasmValue[] { WasmValue.i64(currentTime) };
            };

            HostFunction randomNumber = (args) -> {
                int max = args[0].asInt();
                int random = (int) (Math.random() * max);
                return new WasmValue[] { WasmValue.i32(random) };
            };

            // Create import map with host functions
            ImportMap imports = ImportMap.empty()
                .addFunction("env", "log", logFunction)
                .addFunction("env", "current_time", getCurrentTime)
                .addFunction("env", "random", randomNumber);

            // Instantiate with imports
            Instance instance = runtime.instantiate(module, imports);

            // Call a function that uses the host functions
            WasmFunction main = instance.getFunction("main").get();
            main.call();
        }
    }

    private static byte[] loadModuleWithImports() {
        // This would be a WASM module that imports functions from "env"
        // Example WAT:
        // (module
        //   (import "env" "log" (func $log (param i32)))
        //   (import "env" "current_time" (func $current_time (result i64)))
        //   (import "env" "random" (func $random (param i32) (result i32)))
        //   (func (export "main")
        //     i32.const 42
        //     call $log
        //     call $current_time
        //     drop
        //     i32.const 100
        //     call $random
        //     call $log
        //   )
        // )
        throw new UnsupportedOperationException("Load your module with imports");
    }
}
```

### Step 5: Configuration and Optimization

Learn how to configure the engine for different use cases:

```java
public class ConfigurationExample {
    public static void main(String[] args) throws Exception {
        byte[] wasmBytes = loadSomeModule();

        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {

            // Development configuration (fast compilation, debug info)
            EngineConfig devConfig = new EngineConfig()
                .optimizationLevel(OptimizationLevel.NONE)
                .debugInfo(true)
                .parallelCompilation(false) // Deterministic for debugging
                .consumeFuel(false); // No limits during development

            try (Engine devEngine = runtime.createEngine(devConfig)) {
                System.out.println("Development engine created");
                // Use for development and testing
            }

            // Production configuration (optimized for speed)
            EngineConfig prodConfig = new EngineConfig()
                .optimizationLevel(OptimizationLevel.SPEED)
                .debugInfo(false)
                .parallelCompilation(true)
                .consumeFuel(true)
                .maxFuel(1000000); // Limit execution to prevent infinite loops

            try (Engine prodEngine = runtime.createEngine(prodConfig)) {
                System.out.println("Production engine created");

                // Create module and store
                Module module = runtime.compileModule(prodEngine, wasmBytes);
                Store store = runtime.createStore(prodEngine);

                // Configure store limits
                store.setFuelConsumption(1000000);
                store.setMaxMemorySize(64 * 1024 * 1024); // 64MB limit

                Instance instance = runtime.instantiate(module);

                // Monitor fuel consumption
                long fuelBefore = store.getFuelConsumed();
                WasmFunction compute = instance.getFunction("compute").get();
                compute.call(WasmValue.i32(1000));
                long fuelAfter = store.getFuelConsumed();

                System.out.println("Fuel consumed: " + (fuelAfter - fuelBefore));
            }

            // Memory-optimized configuration (for resource-constrained environments)
            EngineConfig memoryConfig = new EngineConfig()
                .optimizationLevel(OptimizationLevel.SIZE)
                .parallelCompilation(false) // Save memory
                .precompileModules(false)   // Compile on demand
                .enableModuleCaching(true); // Cache compiled modules

            try (Engine memoryEngine = runtime.createEngine(memoryConfig)) {
                System.out.println("Memory-optimized engine created");
                // Use for environments with limited memory
            }
        }
    }

    private static byte[] loadSomeModule() {
        // Load your WebAssembly module
        throw new UnsupportedOperationException("Implement module loading");
    }
}
```

## Common Patterns

### Pattern 1: Module Compilation and Caching

```java
public class ModuleCachePattern {
    private final Map<String, Module> moduleCache = new ConcurrentHashMap<>();
    private final Engine engine;

    public ModuleCachePattern() throws Exception {
        WasmRuntime runtime = WasmRuntimeFactory.create();
        this.engine = runtime.createEngine();
    }

    public Module getOrCompileModule(String modulePath) throws Exception {
        return moduleCache.computeIfAbsent(modulePath, path -> {
            try {
                byte[] wasmBytes = Files.readAllBytes(Paths.get(path));
                return engine.compileModule(wasmBytes);
            } catch (Exception e) {
                throw new RuntimeException("Failed to compile module: " + path, e);
            }
        });
    }

    public Instance createInstance(String modulePath) throws Exception {
        Module module = getOrCompileModule(modulePath);
        Store store = engine.createStore();
        return module.instantiate(store);
    }
}
```

### Pattern 2: Function Call Wrapper

```java
public class FunctionCallWrapper {
    private final Instance instance;

    public FunctionCallWrapper(Instance instance) {
        this.instance = instance;
    }

    public <T> T callFunction(String functionName, Class<T> returnType, Object... args) {
        WasmFunction function = instance.getFunction(functionName)
            .orElseThrow(() -> new RuntimeException("Function not found: " + functionName));

        // Convert Java arguments to WASM values
        WasmValue[] wasmArgs = new WasmValue[args.length];
        for (int i = 0; i < args.length; i++) {
            wasmArgs[i] = convertToWasmValue(args[i]);
        }

        // Call the function
        WasmValue[] results = function.call(wasmArgs);

        // Convert result back to Java type
        if (results.length == 0) {
            return null;
        }

        return convertFromWasmValue(results[0], returnType);
    }

    private WasmValue convertToWasmValue(Object value) {
        if (value instanceof Integer) {
            return WasmValue.i32((Integer) value);
        } else if (value instanceof Long) {
            return WasmValue.i64((Long) value);
        } else if (value instanceof Float) {
            return WasmValue.f32((Float) value);
        } else if (value instanceof Double) {
            return WasmValue.f64((Double) value);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + value.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertFromWasmValue(WasmValue value, Class<T> type) {
        if (type == Integer.class || type == int.class) {
            return (T) Integer.valueOf(value.asInt());
        } else if (type == Long.class || type == long.class) {
            return (T) Long.valueOf(value.asLong());
        } else if (type == Float.class || type == float.class) {
            return (T) Float.valueOf(value.asFloat());
        } else if (type == Double.class || type == double.class) {
            return (T) Double.valueOf(value.asDouble());
        } else {
            throw new IllegalArgumentException("Unsupported return type: " + type);
        }
    }
}

// Usage example
public class WrapperUsageExample {
    public static void main(String[] args) throws Exception {
        // Setup
        try (WasmRuntime runtime = WasmRuntimeFactory.create();
             Engine engine = runtime.createEngine()) {

            byte[] wasmBytes = Files.readAllBytes(Paths.get("math.wasm"));
            Module module = runtime.compileModule(engine, wasmBytes);
            Store store = runtime.createStore(engine);
            Instance instance = runtime.instantiate(module);

            // Use wrapper
            FunctionCallWrapper wrapper = new FunctionCallWrapper(instance);

            // Call functions with simplified syntax
            Integer sum = wrapper.callFunction("add", Integer.class, 10, 5);
            Integer product = wrapper.callFunction("multiply", Integer.class, 7, 6);

            System.out.println("Sum: " + sum);
            System.out.println("Product: " + product);
        }
    }
}
```

### Pattern 3: Resource Management with Try-With-Resources

```java
public class ResourceManagementPattern {
    public static void processMultipleModules(String[] modulePaths) throws Exception {
        // Proper resource management using try-with-resources
        try (WasmRuntime runtime = WasmRuntimeFactory.create();
             Engine engine = runtime.createEngine()) {

            for (String modulePath : modulePaths) {
                processModule(runtime, engine, modulePath);
            }
        } // Runtime and engine automatically closed
    }

    private static void processModule(WasmRuntime runtime, Engine engine, String modulePath) throws Exception {
        byte[] wasmBytes = Files.readAllBytes(Paths.get(modulePath));

        try (Store store = runtime.createStore(engine)) {
            Module module = runtime.compileModule(engine, wasmBytes);

            try (Instance instance = runtime.instantiate(module)) {
                // Process the instance
                WasmFunction mainFunc = instance.getFunction("main")
                    .orElseThrow(() -> new RuntimeException("No main function"));

                mainFunc.call();

                System.out.println("Processed module: " + modulePath);
            } // Instance automatically closed
        } // Store automatically closed
    }
}
```

## Next Steps

Now that you've learned the basics, here are some areas to explore further:

### 1. Advanced Topics
- [WASI Support](wasi-guide.md) - Learn about WebAssembly System Interface
- [Component Model](component-model.md) - Work with WebAssembly components
- [Async Programming](async-guide.md) - Use async and reactive APIs
- [Security](security-guide.md) - Implement security policies and sandboxing

### 2. Performance Optimization
- [Performance Tuning](performance-guide.md) - Optimize your WebAssembly applications
- [Benchmarking](benchmarking-guide.md) - Measure and compare performance
- [Memory Management](memory-guide.md) - Efficient memory usage patterns

### 3. Production Deployment
- [Deployment Guide](deployment-guide.md) - Deploy to production environments
- [Monitoring](monitoring-guide.md) - Monitor WebAssembly applications
- [Troubleshooting](troubleshooting-guide.md) - Debug common issues

### 4. Integration Examples
- [Spring Boot Integration](../examples/spring-boot/) - Use with Spring Boot
- [Microservices](../examples/microservices/) - Build WebAssembly microservices
- [Plugin Systems](../examples/plugins/) - Create extensible plugin architectures

### 5. Migration Guides
- [From wasmtime-java](migration-wasmtime-java.md) - Migrate from other Wasmtime bindings
- [From Wasmer](migration-wasmer.md) - Migrate from Wasmer Java bindings

## Getting Help

If you run into issues or have questions:

1. **Check the [Troubleshooting Guide](troubleshooting-guide.md)**
2. **Review the [API Reference](../api/api-reference.md)**
3. **Browse [Examples](../examples/)**
4. **Search existing [GitHub Issues](https://github.com/wasmtime4j/wasmtime4j/issues)**
5. **Start a [GitHub Discussion](https://github.com/wasmtime4j/wasmtime4j/discussions)**
6. **Join our [Community Discord](https://discord.gg/wasmtime4j)**

Welcome to the Wasmtime4j community! 🎉
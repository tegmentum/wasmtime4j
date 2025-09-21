# Migration Guide: Moving to Wasmtime4j

This guide helps you migrate from other WebAssembly runtime libraries to Wasmtime4j, covering common migration scenarios and providing step-by-step instructions.

## Table of Contents

1. [Migration from wasmtime-java](#migration-from-wasmtime-java)
2. [Migration from Wasmer Java](#migration-from-wasmer-java)
3. [Migration from chicory](#migration-from-chicory)
4. [Migration from asmble](#migration-from-asmble)
5. [General Migration Strategies](#general-migration-strategies)
6. [Common Migration Issues](#common-migration-issues)
7. [Testing Migration](#testing-migration)

## Migration from wasmtime-java

### Overview

wasmtime-java is the original Java binding for Wasmtime. Wasmtime4j provides a more comprehensive, production-ready API with better resource management and dual JNI/Panama support.

### Key Differences

| Aspect | wasmtime-java | Wasmtime4j |
|--------|---------------|------------|
| Runtime Selection | JNI only | Automatic JNI/Panama selection |
| Resource Management | Manual cleanup | Try-with-resources + automatic cleanup |
| Error Handling | Basic exceptions | Comprehensive error diagnostics |
| Performance | Basic | Advanced optimization + profiling |
| Security | Limited | Comprehensive sandboxing |
| WASI Support | Basic | Full WASI Preview 1 & 2 support |

### Step-by-Step Migration

#### 1. Update Dependencies

**Before (wasmtime-java):**
```xml
<dependency>
    <groupId>io.github.kawamuray.wasmtime</groupId>
    <artifactId>wasmtime-java</artifactId>
    <version>0.19.0</version>
</dependency>
```

**After (Wasmtime4j):**
```xml
<dependency>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 2. Update Package Imports

**Before:**
```java
import io.github.kawamuray.wasmtime.*;
```

**After:**
```java
import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
```

#### 3. Engine Creation

**Before:**
```java
Engine engine = new Engine();
```

**After:**
```java
// Automatic runtime selection (recommended)
try (WasmRuntime runtime = WasmRuntimeFactory.create();
     Engine engine = runtime.createEngine()) {
    // Use engine
}

// Or with configuration
EngineConfig config = new EngineConfig()
    .optimizationLevel(OptimizationLevel.SPEED)
    .parallelCompilation(true);

try (Engine engine = Engine.create(config)) {
    // Use engine
}
```

#### 4. Module Compilation

**Before:**
```java
Module module = Module.fromBinary(engine, wasmBytes);
```

**After:**
```java
// With runtime
Module module = runtime.compileModule(engine, wasmBytes);

// Or directly with engine
Module module = engine.compileModule(wasmBytes);
```

#### 5. Store Creation

**Before:**
```java
Store store = new Store(engine);
```

**After:**
```java
Store store = engine.createStore();

// Or with runtime
Store store = runtime.createStore(engine);
```

#### 6. Instance Creation

**Before:**
```java
Instance instance = new Instance(store, module, imports);
```

**After:**
```java
// Without imports
Instance instance = module.instantiate(store);

// With imports
ImportMap imports = ImportMap.empty()
    .addFunction("env", "log", logFunction);
Instance instance = module.instantiate(store, imports);

// Or with runtime
Instance instance = runtime.instantiate(module, imports);
```

#### 7. Function Calls

**Before:**
```java
Func func = instance.getFunc(store, "add");
WasmValType[] results = func.call(store, WasmValType.i32(5), WasmValType.i32(3));
int result = results[0].i32();
```

**After:**
```java
WasmFunction func = instance.getFunction("add")
    .orElseThrow(() -> new RuntimeException("Function not found"));
WasmValue[] results = func.call(WasmValue.i32(5), WasmValue.i32(3));
int result = results[0].asInt();
```

#### 8. Memory Operations

**Before:**
```java
Memory memory = instance.getMemory(store, "memory");
byte[] data = memory.read(store, 0, 100);
memory.write(store, 0, data);
```

**After:**
```java
WasmMemory memory = instance.getMemory("memory")
    .orElseThrow(() -> new RuntimeException("Memory not found"));
byte[] data = memory.read(0, 100);
memory.write(0, data);
```

#### 9. Complete Migration Example

**Before (wasmtime-java):**
```java
public class OldWasmApp {
    public static void main(String[] args) throws Exception {
        byte[] wasmBytes = Files.readAllBytes(Paths.get("module.wasm"));

        Engine engine = new Engine();
        Module module = Module.fromBinary(engine, wasmBytes);
        Store store = new Store(engine);

        // Create imports
        Func logFunc = WasmFunctions.wrap(store, WasmFuncType.consumer(WasmValType.I32), (params) -> {
            System.out.println("Log: " + params[0].i32());
        });

        Instance instance = new Instance(store, module, new Extern[]{logFunc});

        Func addFunc = instance.getFunc(store, "add");
        WasmValType[] result = addFunc.call(store, WasmValType.i32(5), WasmValType.i32(3));

        System.out.println("Result: " + result[0].i32());

        // Manual cleanup
        instance.close();
        store.close();
        module.close();
        engine.close();
    }
}
```

**After (Wasmtime4j):**
```java
public class NewWasmApp {
    public static void main(String[] args) throws Exception {
        byte[] wasmBytes = Files.readAllBytes(Paths.get("module.wasm"));

        try (WasmRuntime runtime = WasmRuntimeFactory.create();
             Engine engine = runtime.createEngine()) {

            Module module = runtime.compileModule(engine, wasmBytes);

            // Create imports
            HostFunction logFunc = (args) -> {
                System.out.println("Log: " + args[0].asInt());
                return new WasmValue[0];
            };

            ImportMap imports = ImportMap.empty()
                .addFunction("env", "log", logFunc);

            Store store = runtime.createStore(engine);
            Instance instance = runtime.instantiate(module, imports);

            WasmFunction addFunc = instance.getFunction("add").get();
            WasmValue[] result = addFunc.call(WasmValue.i32(5), WasmValue.i32(3));

            System.out.println("Result: " + result[0].asInt());

            // Automatic resource cleanup with try-with-resources
        }
    }
}
```

### Migration Checklist

- [ ] Update Maven/Gradle dependencies
- [ ] Change package imports
- [ ] Update engine creation to use factory pattern
- [ ] Migrate module compilation calls
- [ ] Update store creation
- [ ] Migrate instance creation and imports
- [ ] Update function call syntax
- [ ] Update memory access patterns
- [ ] Add proper resource management (try-with-resources)
- [ ] Test all functionality
- [ ] Performance testing and optimization

## Migration from Wasmer Java

### Overview

Wasmer Java provides JNI bindings for the Wasmer runtime. The API concepts are similar, but Wasmtime4j offers better Java integration and dual runtime support.

### Key Differences

| Aspect | Wasmer Java | Wasmtime4j |
|--------|-------------|------------|
| Runtime | Wasmer | Wasmtime |
| Bindings | JNI only | JNI + Panama FFI |
| Memory Model | Manual management | Automatic + manual options |
| Type System | Wasmer types | Standard WebAssembly types |
| Configuration | Limited | Extensive configuration options |

### Step-by-Step Migration

#### 1. Update Dependencies

**Before (Wasmer Java):**
```xml
<dependency>
    <groupId>org.wasmer</groupId>
    <artifactId>wasmer-jni</artifactId>
    <version>0.3.0</version>
</dependency>
```

**After (Wasmtime4j):**
```xml
<dependency>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 2. Basic Setup Migration

**Before (Wasmer):**
```java
import org.wasmer.*;

public class WasmerApp {
    public static void main(String[] args) {
        byte[] wasmBytes = loadWasmBytes();

        Module module = new Module(wasmBytes);
        Instance instance = module.instantiate();

        Function sumFunction = instance.exports.getFunction("sum");
        Object[] results = sumFunction.apply(5, 37);

        System.out.println((Integer) results[0]); // 42

        instance.close();
        module.close();
    }
}
```

**After (Wasmtime4j):**
```java
import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;

public class Wasmtime4jApp {
    public static void main(String[] args) throws Exception {
        byte[] wasmBytes = loadWasmBytes();

        try (WasmRuntime runtime = WasmRuntimeFactory.create();
             Engine engine = runtime.createEngine()) {

            Module module = runtime.compileModule(engine, wasmBytes);
            Store store = runtime.createStore(engine);
            Instance instance = runtime.instantiate(module);

            WasmFunction sumFunction = instance.getFunction("sum").get();
            WasmValue[] results = sumFunction.call(WasmValue.i32(5), WasmValue.i32(37));

            System.out.println(results[0].asInt()); // 42

            // Automatic resource cleanup
        }
    }
}
```

#### 3. Memory Access Migration

**Before (Wasmer):**
```java
Memory memory = instance.exports.getMemory("memory");
ByteBuffer buffer = memory.buffer();

// Write data
buffer.putInt(0, 42);

// Read data
int value = buffer.getInt(0);
```

**After (Wasmtime4j):**
```java
WasmMemory memory = instance.getMemory("memory").get();

// Write data
memory.writeInt(0, 42);

// Read data
int value = memory.readInt(0);

// Or bulk operations
byte[] data = {1, 2, 3, 4};
memory.write(0, data);
byte[] readData = memory.read(0, 4);
```

#### 4. Import Functions Migration

**Before (Wasmer):**
```java
ImportObject importObject = new ImportObject();
Function hostFunction = new Function(
    new Type(new Type[]{Type.I32}, new Type[]{Type.I32}),
    (args) -> {
        int input = (Integer) args[0];
        return new Object[]{input * 2};
    }
);
importObject.register("env", "host_function", hostFunction);

Instance instance = module.instantiate(importObject);
```

**After (Wasmtime4j):**
```java
HostFunction hostFunction = (args) -> {
    int input = args[0].asInt();
    return new WasmValue[]{WasmValue.i32(input * 2)};
};

ImportMap imports = ImportMap.empty()
    .addFunction("env", "host_function", hostFunction);

Instance instance = runtime.instantiate(module, imports);
```

### Complete Migration Example

**Before (Wasmer Java):**
```java
public class WasmerExample {
    public static void main(String[] args) {
        try {
            byte[] wasmBytes = Files.readAllBytes(Paths.get("example.wasm"));

            Module module = new Module(wasmBytes);

            // Setup imports
            ImportObject importObject = new ImportObject();
            Function logFunc = new Function(
                new Type(new Type[]{Type.I32}, new Type[]{}),
                (args) -> {
                    System.out.println("Logged: " + args[0]);
                    return new Object[]{};
                }
            );
            importObject.register("env", "log", logFunc);

            Instance instance = module.instantiate(importObject);

            // Call exported function
            Function compute = instance.exports.getFunction("compute");
            Object[] result = compute.apply(42);

            System.out.println("Result: " + result[0]);

            // Access memory
            Memory memory = instance.exports.getMemory("memory");
            ByteBuffer buffer = memory.buffer();
            System.out.println("Memory size: " + buffer.capacity());

            // Cleanup
            instance.close();
            module.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**After (Wasmtime4j):**
```java
public class Wasmtime4jExample {
    public static void main(String[] args) {
        try (WasmRuntime runtime = WasmRuntimeFactory.create();
             Engine engine = runtime.createEngine()) {

            byte[] wasmBytes = Files.readAllBytes(Paths.get("example.wasm"));
            Module module = runtime.compileModule(engine, wasmBytes);

            // Setup imports
            HostFunction logFunc = (args) -> {
                System.out.println("Logged: " + args[0].asInt());
                return new WasmValue[]{};
            };

            ImportMap imports = ImportMap.empty()
                .addFunction("env", "log", logFunc);

            Store store = runtime.createStore(engine);
            Instance instance = runtime.instantiate(module, imports);

            // Call exported function
            WasmFunction compute = instance.getFunction("compute").get();
            WasmValue[] result = compute.call(WasmValue.i32(42));

            System.out.println("Result: " + result[0].asInt());

            // Access memory
            WasmMemory memory = instance.getMemory("memory").get();
            System.out.println("Memory size: " + memory.size());

            // Automatic cleanup with try-with-resources

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Migration from chicory

### Overview

chicory is a pure Java WebAssembly runtime. Migrating to Wasmtime4j provides native performance while maintaining Java integration.

### Key Differences

| Aspect | chicory | Wasmtime4j |
|--------|---------|------------|
| Implementation | Pure Java | Native (Wasmtime) |
| Performance | Java bytecode speed | Native C speed |
| Features | Core WebAssembly | Full WebAssembly + WASI |
| Dependencies | JVM only | JVM + native libraries |

### Migration Steps

#### 1. Dependencies

**Before (chicory):**
```xml
<dependency>
    <groupId>com.dylibso.chicory</groupId>
    <artifactId>runtime</artifactId>
    <version>0.0.12</version>
</dependency>
```

**After (Wasmtime4j):**
```xml
<dependency>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 2. Basic Usage

**Before (chicory):**
```java
import com.dylibso.chicory.runtime.*;

Module module = Module.builder(wasmBytes).build();
Instance instance = module.instantiate();
ExportFunction fn = instance.getExport("add");
Value[] results = fn.apply(Value.i32(1), Value.i32(2));
System.out.println(results[0].asInt()); // 3
```

**After (Wasmtime4j):**
```java
import ai.tegmentum.wasmtime4j.*;

try (WasmRuntime runtime = WasmRuntimeFactory.create();
     Engine engine = runtime.createEngine()) {

    Module module = runtime.compileModule(engine, wasmBytes);
    Store store = runtime.createStore(engine);
    Instance instance = runtime.instantiate(module);

    WasmFunction fn = instance.getFunction("add").get();
    WasmValue[] results = fn.call(WasmValue.i32(1), WasmValue.i32(2));
    System.out.println(results[0].asInt()); // 3
}
```

## Migration from asmble

### Overview

asmble converts WebAssembly to JVM bytecode. Wasmtime4j provides direct execution with better performance and feature support.

### Key Migration Points

1. **Compilation Model**: asmble compiles to JVM classes, Wasmtime4j executes directly
2. **Performance**: Native execution vs JVM bytecode
3. **Features**: Full WebAssembly specification support
4. **Integration**: Better native library integration

### Migration Example

**Before (asmble):**
```java
import asmble.run.jvm.Module;

// Compile WASM to JVM class
Module.Compiled compiled = Module.builder().
    logger(logger).
    name("Test").
    wasmBytes(wasmBytes).
    build().
    compile();

// Load as Java class
Class<?> wasmClass = compiled.getClass();
Object instance = wasmClass.newInstance();

// Call method
Method addMethod = wasmClass.getMethod("add", int.class, int.class);
int result = (Integer) addMethod.invoke(instance, 5, 3);
```

**After (Wasmtime4j):**
```java
try (WasmRuntime runtime = WasmRuntimeFactory.create();
     Engine engine = runtime.createEngine()) {

    Module module = runtime.compileModule(engine, wasmBytes);
    Store store = runtime.createStore(engine);
    Instance instance = runtime.instantiate(module);

    WasmFunction addFunction = instance.getFunction("add").get();
    WasmValue[] result = addFunction.call(WasmValue.i32(5), WasmValue.i32(3));
    int sum = result[0].asInt();
}
```

## General Migration Strategies

### 1. Incremental Migration

Start with a small, isolated component:

```java
public class MigrationWrapper {
    private final WasmRuntime runtime;
    private final Engine engine;

    public MigrationWrapper() throws Exception {
        this.runtime = WasmRuntimeFactory.create();
        this.engine = runtime.createEngine();
    }

    // Wrapper methods that match old API
    public int oldStyleCall(String functionName, int... args) throws Exception {
        // Implementation using Wasmtime4j
        Module module = getOrLoadModule();
        Store store = runtime.createStore(engine);
        Instance instance = runtime.instantiate(module);

        WasmFunction function = instance.getFunction(functionName).get();
        WasmValue[] wasmArgs = Arrays.stream(args)
            .mapToObj(WasmValue::i32)
            .toArray(WasmValue[]::new);

        WasmValue[] results = function.call(wasmArgs);
        return results[0].asInt();
    }

    public void close() throws Exception {
        if (runtime != null) {
            runtime.close();
        }
    }
}
```

### 2. Configuration Migration

Map old configuration to new:

```java
public class ConfigMigrator {
    public static EngineConfig migrateConfig(OldConfig oldConfig) {
        EngineConfig newConfig = new EngineConfig();

        // Map optimization levels
        switch (oldConfig.getOptLevel()) {
            case NONE:
                newConfig.optimizationLevel(OptimizationLevel.NONE);
                break;
            case SPEED:
                newConfig.optimizationLevel(OptimizationLevel.SPEED);
                break;
            case SIZE:
                newConfig.optimizationLevel(OptimizationLevel.SIZE);
                break;
        }

        // Map debug settings
        newConfig.debugInfo(oldConfig.isDebugEnabled());

        // Set new features
        newConfig.parallelCompilation(true); // New capability
        newConfig.consumeFuel(oldConfig.hasFuelLimit());

        return newConfig;
    }
}
```

### 3. Type System Migration

Handle type conversions:

```java
public class TypeMigrator {
    public static WasmValue[] convertArgs(Object[] oldArgs) {
        return Arrays.stream(oldArgs)
            .map(TypeMigrator::convertArg)
            .toArray(WasmValue[]::new);
    }

    private static WasmValue convertArg(Object arg) {
        if (arg instanceof Integer) {
            return WasmValue.i32((Integer) arg);
        } else if (arg instanceof Long) {
            return WasmValue.i64((Long) arg);
        } else if (arg instanceof Float) {
            return WasmValue.f32((Float) arg);
        } else if (arg instanceof Double) {
            return WasmValue.f64((Double) arg);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + arg.getClass());
        }
    }

    public static Object[] convertResults(WasmValue[] results) {
        return Arrays.stream(results)
            .map(TypeMigrator::convertResult)
            .toArray();
    }

    private static Object convertResult(WasmValue value) {
        switch (value.getType()) {
            case I32:
                return value.asInt();
            case I64:
                return value.asLong();
            case F32:
                return value.asFloat();
            case F64:
                return value.asDouble();
            default:
                throw new IllegalArgumentException("Unsupported result type: " + value.getType());
        }
    }
}
```

## Common Migration Issues

### Issue 1: Resource Management

**Problem**: Old code doesn't properly clean up resources.

**Solution**: Use try-with-resources pattern:

```java
// Problematic pattern
Engine engine = createEngine();
Module module = compileModule(engine, bytes);
// ... forgot to close resources

// Correct pattern
try (WasmRuntime runtime = WasmRuntimeFactory.create();
     Engine engine = runtime.createEngine()) {
    Module module = runtime.compileModule(engine, bytes);
    // Automatic cleanup
}
```

### Issue 2: Error Handling

**Problem**: Different error types and handling.

**Solution**: Update exception handling:

```java
try {
    // Wasmtime4j operations
} catch (CompilationException e) {
    // Handle compilation errors
    System.err.println("Compilation failed: " + e.getMessage());
} catch (ValidationException e) {
    // Handle validation errors
    System.err.println("Validation failed: " + e.getMessage());
} catch (RuntimeException e) {
    // Handle runtime errors
    System.err.println("Runtime error: " + e.getMessage());
}
```

### Issue 3: API Differences

**Problem**: Function signatures or behavior differs.

**Solution**: Create adapter layer:

```java
public class ApiAdapter {
    public static LegacyResult adaptToLegacy(WasmValue[] results) {
        // Convert new format to legacy format
        return new LegacyResult(results);
    }

    public static WasmValue[] adaptFromLegacy(LegacyArgs args) {
        // Convert legacy format to new format
        return convertArgs(args);
    }
}
```

## Testing Migration

### 1. Parallel Testing

Run both old and new implementations in parallel:

```java
@Test
public void testMigrationParity() throws Exception {
    byte[] testData = loadTestData();

    // Old implementation
    Object oldResult = runWithOldLibrary(testData);

    // New implementation
    Object newResult = runWithWasmtime4j(testData);

    // Compare results
    assertEquals(oldResult, newResult);
}
```

### 2. Performance Comparison

Compare performance between implementations:

```java
@Test
public void testPerformanceImprovement() throws Exception {
    long oldTime = measureOldImplementation();
    long newTime = measureWasmtime4j();

    // Assert that new implementation is faster or at least comparable
    assertTrue("New implementation should be faster", newTime <= oldTime * 1.1);
}
```

### 3. Feature Validation

Ensure all features work correctly:

```java
@Test
public void testFeatureCompleteness() throws Exception {
    // Test all previously working features
    testBasicFunctionCalls();
    testMemoryOperations();
    testImportFunctions();
    testErrorHandling();
    testResourceCleanup();
}
```

### Migration Checklist

- [ ] Identify all WebAssembly usage in codebase
- [ ] Create migration plan with priorities
- [ ] Set up parallel testing infrastructure
- [ ] Update dependencies
- [ ] Migrate core functionality first
- [ ] Create adapter layer for compatibility
- [ ] Update error handling
- [ ] Implement proper resource management
- [ ] Performance testing and optimization
- [ ] Update documentation and training
- [ ] Gradual rollout with monitoring
- [ ] Remove old dependencies

## Getting Help

If you encounter issues during migration:

1. **Check the [API Reference](../api/api-reference.md)** for equivalent functionality
2. **Review [Common Patterns](../guides/getting-started-complete.md#common-patterns)**
3. **Search [GitHub Issues](https://github.com/wasmtime4j/wasmtime4j/issues)** for similar migration questions
4. **Ask in [GitHub Discussions](https://github.com/wasmtime4j/wasmtime4j/discussions)**
5. **Join our [Community Discord](https://discord.gg/wasmtime4j)** for real-time help

The Wasmtime4j community is here to help make your migration successful!
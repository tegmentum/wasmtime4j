# Wasmtime4j API Reference

This document provides a comprehensive reference for the Wasmtime4j WebAssembly runtime API. The API is designed to provide a unified interface for both JNI and Panama implementations while maintaining high performance and safety.

## Table of Contents

- [Core Runtime APIs](#core-runtime-apis)
- [Engine Management](#engine-management)
- [Module Operations](#module-operations)
- [Instance Management](#instance-management)
- [Store Context](#store-context)
- [Memory Management](#memory-management)
- [Function Invocation](#function-invocation)
- [WASI Integration](#wasi-integration)
- [Host Functions](#host-functions)
- [Error Handling](#error-handling)
- [Performance Configuration](#performance-configuration)
- [Type System](#type-system)

## Core Runtime APIs

### WasmRuntime Interface

The primary entry point for WebAssembly operations.

```java
package ai.tegmentum.wasmtime4j;

/**
 * Main interface for WebAssembly runtime operations.
 */
public interface WasmRuntime extends AutoCloseable {

    /**
     * Creates a new WebAssembly engine with default configuration.
     * @return new Engine instance
     * @throws WasmException if engine creation fails
     */
    Engine createEngine() throws WasmException;

    /**
     * Creates a new WebAssembly engine with custom configuration.
     * @param config engine configuration
     * @return new Engine instance
     * @throws WasmException if engine creation fails
     */
    Engine createEngine(EngineConfig config) throws WasmException;

    /**
     * Gets runtime type information.
     * @return runtime information
     */
    RuntimeInfo getRuntimeInfo();

    /**
     * Gets the runtime type (JNI or Panama).
     * @return runtime type
     */
    RuntimeType getRuntimeType();

    /**
     * Closes the runtime and releases all resources.
     */
    @Override
    void close() throws WasmException;
}
```

### WasmRuntimeFactory

Factory for creating runtime instances with automatic or manual selection.

```java
package ai.tegmentum.wasmtime4j.factory;

/**
 * Factory for creating WebAssembly runtime instances.
 */
public final class WasmRuntimeFactory {

    /**
     * Creates a runtime with automatic implementation selection.
     * @return new WasmRuntime instance
     * @throws WasmException if no suitable runtime is available
     */
    public static WasmRuntime create() throws WasmException;

    /**
     * Creates a runtime with specified implementation type.
     * @param runtimeType the runtime type to create
     * @return new WasmRuntime instance
     * @throws WasmException if the runtime type is not available
     */
    public static WasmRuntime create(RuntimeType runtimeType) throws WasmException;

    /**
     * Checks if a runtime type is available.
     * @param runtimeType the runtime type to check
     * @return true if available
     */
    public static boolean isRuntimeAvailable(RuntimeType runtimeType);

    /**
     * Gets the automatically selected runtime type.
     * @return the selected runtime type
     */
    public static RuntimeType getSelectedRuntimeType();
}
```

## Engine Management

### Engine Interface

Represents a WebAssembly compilation engine.

```java
package ai.tegmentum.wasmtime4j;

/**
 * WebAssembly compilation engine.
 */
public interface Engine extends AutoCloseable {

    /**
     * Compiles WebAssembly module from bytes.
     * @param wasmBytes WebAssembly module bytes
     * @return compiled Module
     * @throws WasmException if compilation fails
     */
    Module compile(byte[] wasmBytes) throws WasmException;

    /**
     * Compiles WebAssembly module from file.
     * @param wasmFile path to WebAssembly file
     * @return compiled Module
     * @throws WasmException if compilation fails
     */
    Module compileFile(String wasmFile) throws WasmException;

    /**
     * Validates WebAssembly module bytes.
     * @param wasmBytes WebAssembly module bytes
     * @return true if valid
     */
    boolean validate(byte[] wasmBytes);

    /**
     * Creates a new Store for this engine.
     * @return new Store instance
     * @throws WasmException if store creation fails
     */
    Store createStore() throws WasmException;

    /**
     * Creates a new Store with custom configuration.
     * @param config store configuration
     * @return new Store instance
     * @throws WasmException if store creation fails
     */
    Store createStore(StoreConfig config) throws WasmException;

    /**
     * Gets engine configuration.
     * @return engine configuration
     */
    EngineConfig getConfig();

    /**
     * Gets engine statistics.
     * @return engine statistics
     */
    EngineStats getStats();
}
```

### EngineConfig

Configuration for WebAssembly engines.

```java
package ai.tegmentum.wasmtime4j;

/**
 * Configuration for WebAssembly engines.
 */
public final class EngineConfig {

    /**
     * Creates a new engine configuration builder.
     * @return configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Sets optimization level for compiled modules.
         * @param level optimization level
         * @return this builder
         */
        public Builder optimizationLevel(OptimizationLevel level);

        /**
         * Sets debug information generation.
         * @param enabled true to enable debug info
         * @return this builder
         */
        public Builder debugInfo(boolean enabled);

        /**
         * Sets fuel consumption for execution limits.
         * @param fuel fuel amount
         * @return this builder
         */
        public Builder fuel(long fuel);

        /**
         * Sets epoch interruption support.
         * @param enabled true to enable epoch interruption
         * @return this builder
         */
        public Builder epochInterruption(boolean enabled);

        /**
         * Sets memory usage limits.
         * @param maxMemory maximum memory in bytes
         * @return this builder
         */
        public Builder maxMemory(long maxMemory);

        /**
         * Sets compilation cache configuration.
         * @param config cache configuration
         * @return this builder
         */
        public Builder cache(CacheConfig config);

        /**
         * Builds the engine configuration.
         * @return engine configuration
         */
        public EngineConfig build();
    }

    // Getters for all configuration options
    public OptimizationLevel getOptimizationLevel();
    public boolean isDebugInfoEnabled();
    public Optional<Long> getFuel();
    public boolean isEpochInterruptionEnabled();
    public Optional<Long> getMaxMemory();
    public Optional<CacheConfig> getCache();
}
```

## Module Operations

### Module Interface

Represents a compiled WebAssembly module.

```java
package ai.tegmentum.wasmtime4j;

/**
 * Compiled WebAssembly module.
 */
public interface Module extends AutoCloseable {

    /**
     * Gets module name if available.
     * @return module name
     */
    Optional<String> getName();

    /**
     * Gets module imports.
     * @return list of module imports
     */
    List<Import> getImports();

    /**
     * Gets module exports.
     * @return list of module exports
     */
    List<Export> getExports();

    /**
     * Instantiates the module with provided imports.
     * @param store the store context
     * @param imports import map
     * @return new instance
     * @throws WasmException if instantiation fails
     */
    Instance instantiate(Store store, ImportMap imports) throws WasmException;

    /**
     * Instantiates the module with linker.
     * @param store the store context
     * @param linker the linker with imports
     * @return new instance
     * @throws WasmException if instantiation fails
     */
    Instance instantiate(Store store, Linker linker) throws WasmException;

    /**
     * Serializes the module to bytes.
     * @return serialized module bytes
     * @throws WasmException if serialization fails
     */
    byte[] serialize() throws WasmException;

    /**
     * Gets module metadata.
     * @return module metadata
     */
    ModuleMetadata getMetadata();
}
```

### Import and Export Types

```java
package ai.tegmentum.wasmtime4j;

/**
 * Represents a WebAssembly import.
 */
public final class Import {
    public String getModule();
    public String getName();
    public ImportType getType();

    public enum ImportType {
        FUNCTION, MEMORY, TABLE, GLOBAL
    }
}

/**
 * Represents a WebAssembly export.
 */
public final class Export {
    public String getName();
    public ExportType getType();

    public enum ExportType {
        FUNCTION, MEMORY, TABLE, GLOBAL
    }
}
```

## Instance Management

### Instance Interface

Represents an instantiated WebAssembly module.

```java
package ai.tegmentum.wasmtime4j;

/**
 * Instantiated WebAssembly module.
 */
public interface Instance extends AutoCloseable {

    /**
     * Gets an exported function by name.
     * @param name function name
     * @return function wrapper
     * @throws WasmException if function not found
     */
    WasmFunction getFunction(String name) throws WasmException;

    /**
     * Gets an exported memory by name.
     * @param name memory name
     * @return memory wrapper
     * @throws WasmException if memory not found
     */
    WasmMemory getMemory(String name) throws WasmException;

    /**
     * Gets an exported global by name.
     * @param name global name
     * @return global wrapper
     * @throws WasmException if global not found
     */
    WasmGlobal getGlobal(String name) throws WasmException;

    /**
     * Gets an exported table by name.
     * @param name table name
     * @return table wrapper
     * @throws WasmException if table not found
     */
    WasmTable getTable(String name) throws WasmException;

    /**
     * Gets all exported functions.
     * @return map of function name to function
     */
    Map<String, WasmFunction> getFunctions();

    /**
     * Gets all exported memories.
     * @return map of memory name to memory
     */
    Map<String, WasmMemory> getMemories();

    /**
     * Gets all exported globals.
     * @return map of global name to global
     */
    Map<String, WasmGlobal> getGlobals();

    /**
     * Gets all exported tables.
     * @return map of table name to table
     */
    Map<String, WasmTable> getTables();

    /**
     * Gets the store context for this instance.
     * @return store context
     */
    Store getStore();

    /**
     * Gets the module this instance was created from.
     * @return source module
     */
    Module getModule();
}
```

## Store Context

### Store Interface

Provides execution context for WebAssembly instances.

```java
package ai.tegmentum.wasmtime4j;

/**
 * WebAssembly execution context and resource manager.
 */
public interface Store extends AutoCloseable {

    /**
     * Sets fuel for execution limiting.
     * @param fuel fuel amount
     */
    void setFuel(long fuel);

    /**
     * Gets remaining fuel.
     * @return remaining fuel
     */
    long getFuel();

    /**
     * Sets epoch deadline for interruption.
     * @param deadline epoch deadline
     */
    void setEpochDeadline(long deadline);

    /**
     * Increments epoch counter.
     */
    void incrementEpoch();

    /**
     * Sets store data for host function access.
     * @param data store data
     */
    void setData(Object data);

    /**
     * Gets store data.
     * @return store data
     */
    <T> Optional<T> getData(Class<T> type);

    /**
     * Adds garbage collection roots.
     * @param objects objects to root
     */
    void addGcRoots(Object... objects);

    /**
     * Triggers garbage collection.
     */
    void gc();

    /**
     * Gets store configuration.
     * @return store configuration
     */
    StoreConfig getConfig();

    /**
     * Gets store statistics.
     * @return store statistics
     */
    StoreStats getStats();
}
```

## Memory Management

### WasmMemory Interface

Represents WebAssembly linear memory.

```java
package ai.tegmentum.wasmtime4j;

/**
 * WebAssembly linear memory.
 */
public interface WasmMemory extends AutoCloseable {

    /**
     * Gets memory size in pages.
     * @return size in pages (64KB each)
     */
    int getSize();

    /**
     * Gets memory size in bytes.
     * @return size in bytes
     */
    long getSizeBytes();

    /**
     * Grows memory by specified pages.
     * @param pages pages to grow
     * @return previous size in pages
     * @throws WasmException if growth fails
     */
    int grow(int pages) throws WasmException;

    /**
     * Reads data from memory.
     * @param offset memory offset
     * @param length number of bytes to read
     * @return data bytes
     * @throws WasmException if read fails
     */
    byte[] read(int offset, int length) throws WasmException;

    /**
     * Writes data to memory.
     * @param offset memory offset
     * @param data data to write
     * @throws WasmException if write fails
     */
    void write(int offset, byte[] data) throws WasmException;

    /**
     * Reads a single byte.
     * @param offset memory offset
     * @return byte value
     * @throws WasmException if read fails
     */
    byte readByte(int offset) throws WasmException;

    /**
     * Writes a single byte.
     * @param offset memory offset
     * @param value byte value
     * @throws WasmException if write fails
     */
    void writeByte(int offset, byte value) throws WasmException;

    /**
     * Reads a 32-bit integer (little endian).
     * @param offset memory offset
     * @return integer value
     * @throws WasmException if read fails
     */
    int readInt32(int offset) throws WasmException;

    /**
     * Writes a 32-bit integer (little endian).
     * @param offset memory offset
     * @param value integer value
     * @throws WasmException if write fails
     */
    void writeInt32(int offset, int value) throws WasmException;

    /**
     * Reads a 64-bit integer (little endian).
     * @param offset memory offset
     * @return long value
     * @throws WasmException if read fails
     */
    long readInt64(int offset) throws WasmException;

    /**
     * Writes a 64-bit integer (little endian).
     * @param offset memory offset
     * @param value long value
     * @throws WasmException if write fails
     */
    void writeInt64(int offset, long value) throws WasmException;

    /**
     * Reads a 32-bit float.
     * @param offset memory offset
     * @return float value
     * @throws WasmException if read fails
     */
    float readFloat32(int offset) throws WasmException;

    /**
     * Writes a 32-bit float.
     * @param offset memory offset
     * @param value float value
     * @throws WasmException if write fails
     */
    void writeFloat32(int offset, float value) throws WasmException;

    /**
     * Reads a 64-bit double.
     * @param offset memory offset
     * @return double value
     * @throws WasmException if read fails
     */
    double readFloat64(int offset) throws WasmException;

    /**
     * Writes a 64-bit double.
     * @param offset memory offset
     * @param value double value
     * @throws WasmException if write fails
     */
    void writeFloat64(int offset, double value) throws WasmException;

    /**
     * Gets direct access to memory buffer (if supported).
     * @return memory buffer
     * @throws UnsupportedOperationException if direct access not supported
     */
    ByteBuffer getBuffer() throws UnsupportedOperationException;

    /**
     * Gets memory limits.
     * @return memory limits
     */
    MemoryLimits getLimits();
}
```

## Function Invocation

### WasmFunction Interface

Represents a WebAssembly function.

```java
package ai.tegmentum.wasmtime4j;

/**
 * WebAssembly function wrapper.
 */
public interface WasmFunction {

    /**
     * Gets function name.
     * @return function name
     */
    String getName();

    /**
     * Gets function signature.
     * @return function signature
     */
    FunctionSignature getSignature();

    /**
     * Calls function with no arguments.
     * @return result values
     * @throws WasmException if call fails
     */
    WasmValue[] call() throws WasmException;

    /**
     * Calls function with arguments.
     * @param args function arguments
     * @return result values
     * @throws WasmException if call fails
     */
    WasmValue[] call(WasmValue... args) throws WasmException;

    /**
     * Calls function asynchronously.
     * @param args function arguments
     * @return future with result values
     */
    CompletableFuture<WasmValue[]> callAsync(WasmValue... args);

    /**
     * Calls function with timeout.
     * @param timeout call timeout
     * @param args function arguments
     * @return result values
     * @throws WasmException if call fails or times out
     */
    WasmValue[] call(Duration timeout, WasmValue... args) throws WasmException;
}
```

### WasmValue Type System

```java
package ai.tegmentum.wasmtime4j;

/**
 * WebAssembly value wrapper.
 */
public abstract class WasmValue {

    /**
     * Creates an i32 value.
     * @param value integer value
     * @return WasmValue
     */
    public static WasmValue i32(int value);

    /**
     * Creates an i64 value.
     * @param value long value
     * @return WasmValue
     */
    public static WasmValue i64(long value);

    /**
     * Creates an f32 value.
     * @param value float value
     * @return WasmValue
     */
    public static WasmValue f32(float value);

    /**
     * Creates an f64 value.
     * @param value double value
     * @return WasmValue
     */
    public static WasmValue f64(double value);

    /**
     * Gets the value type.
     * @return value type
     */
    public abstract WasmValueType getType();

    /**
     * Gets value as integer.
     * @return integer value
     * @throws ClassCastException if not i32 type
     */
    public abstract int asInt();

    /**
     * Gets value as long.
     * @return long value
     * @throws ClassCastException if not i64 type
     */
    public abstract long asLong();

    /**
     * Gets value as float.
     * @return float value
     * @throws ClassCastException if not f32 type
     */
    public abstract float asFloat();

    /**
     * Gets value as double.
     * @return double value
     * @throws ClassCastException if not f64 type
     */
    public abstract double asDouble();
}

/**
 * WebAssembly value types.
 */
public enum WasmValueType {
    I32, I64, F32, F64, V128, FUNCREF, EXTERNREF
}
```

## WASI Integration

### WasiConfig

Configuration for WASI (WebAssembly System Interface).

```java
package ai.tegmentum.wasmtime4j.wasi;

/**
 * WASI configuration for WebAssembly instances.
 */
public final class WasiConfig {

    /**
     * Creates a new WASI configuration builder.
     * @return configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Sets command line arguments.
         * @param args command line arguments
         * @return this builder
         */
        public Builder args(String... args);

        /**
         * Sets environment variables.
         * @param env environment variables
         * @return this builder
         */
        public Builder env(Map<String, String> env);

        /**
         * Sets environment variable.
         * @param key variable name
         * @param value variable value
         * @return this builder
         */
        public Builder env(String key, String value);

        /**
         * Maps a directory to WASI filesystem.
         * @param wasiPath WASI path
         * @param hostPath host filesystem path
         * @return this builder
         */
        public Builder mapDir(String wasiPath, String hostPath);

        /**
         * Sets stdin source.
         * @param stdin stdin input stream
         * @return this builder
         */
        public Builder stdin(InputStream stdin);

        /**
         * Sets stdout destination.
         * @param stdout stdout output stream
         * @return this builder
         */
        public Builder stdout(OutputStream stdout);

        /**
         * Sets stderr destination.
         * @param stderr stderr output stream
         * @return this builder
         */
        public Builder stderr(OutputStream stderr);

        /**
         * Builds the WASI configuration.
         * @return WASI configuration
         */
        public WasiConfig build();
    }

    // Getters for all configuration
    public List<String> getArgs();
    public Map<String, String> getEnv();
    public Map<String, String> getMappedDirs();
    public Optional<InputStream> getStdin();
    public Optional<OutputStream> getStdout();
    public Optional<OutputStream> getStderr();
}
```

### Linker Interface

For linking modules with WASI and custom imports.

```java
package ai.tegmentum.wasmtime4j;

/**
 * WebAssembly module linker.
 */
public interface Linker extends AutoCloseable {

    /**
     * Defines WASI imports for the linker.
     * @param wasiConfig WASI configuration
     * @throws WasmException if WASI setup fails
     */
    void defineWasi(WasiConfig wasiConfig) throws WasmException;

    /**
     * Defines a host function.
     * @param module module name
     * @param name function name
     * @param function host function implementation
     * @throws WasmException if definition fails
     */
    void defineFunction(String module, String name, HostFunction function) throws WasmException;

    /**
     * Defines a memory import.
     * @param module module name
     * @param name memory name
     * @param memory memory instance
     * @throws WasmException if definition fails
     */
    void defineMemory(String module, String name, WasmMemory memory) throws WasmException;

    /**
     * Defines a global import.
     * @param module module name
     * @param name global name
     * @param global global instance
     * @throws WasmException if definition fails
     */
    void defineGlobal(String module, String name, WasmGlobal global) throws WasmException;

    /**
     * Instantiates a module using this linker.
     * @param store store context
     * @param module module to instantiate
     * @return new instance
     * @throws WasmException if instantiation fails
     */
    Instance instantiate(Store store, Module module) throws WasmException;
}
```

## Host Functions

### HostFunction Interface

For implementing host functions callable from WebAssembly.

```java
package ai.tegmentum.wasmtime4j;

/**
 * Host function implementation interface.
 */
@FunctionalInterface
public interface HostFunction {

    /**
     * Executes the host function.
     * @param caller function caller context
     * @param args function arguments
     * @return function results
     * @throws WasmException if execution fails
     */
    WasmValue[] call(Caller caller, WasmValue[] args) throws WasmException;
}

/**
 * Caller context for host functions.
 */
public interface Caller {

    /**
     * Gets the calling store context.
     * @return store context
     */
    Store getStore();

    /**
     * Gets caller's memory by name.
     * @param name memory name
     * @return caller's memory
     * @throws WasmException if memory not found
     */
    WasmMemory getMemory(String name) throws WasmException;

    /**
     * Gets caller's function by name.
     * @param name function name
     * @return caller's function
     * @throws WasmException if function not found
     */
    WasmFunction getFunction(String name) throws WasmException;

    /**
     * Gets caller's data.
     * @return caller data
     */
    <T> Optional<T> getData(Class<T> type);
}
```

## Error Handling

### Exception Hierarchy

```java
package ai.tegmentum.wasmtime4j.exception;

/**
 * Base exception for all WebAssembly operations.
 */
public class WasmException extends Exception {
    public WasmException(String message);
    public WasmException(String message, Throwable cause);
    public WasmException(Throwable cause);
}

/**
 * Exception thrown during module compilation.
 */
public class CompilationException extends WasmException {
    public CompilationException(String message);
    public CompilationException(String message, Throwable cause);
}

/**
 * Exception thrown during runtime execution.
 */
public class RuntimeException extends WasmException {
    public RuntimeException(String message);
    public RuntimeException(String message, Throwable cause);
}

/**
 * Exception thrown during module validation.
 */
public class ValidationException extends WasmException {
    public ValidationException(String message);
    public ValidationException(String message, Throwable cause);
}

/**
 * Exception thrown when operations are interrupted.
 */
public class InterruptedException extends RuntimeException {
    public InterruptedException(String message);
}

/**
 * Exception thrown when fuel is exhausted.
 */
public class FuelExhaustedException extends InterruptedException {
    public FuelExhaustedException();
}
```

## Performance Configuration

### CacheConfig

Configuration for compilation caching.

```java
package ai.tegmentum.wasmtime4j;

/**
 * Configuration for compilation caching.
 */
public final class CacheConfig {

    /**
     * Creates a new cache configuration builder.
     * @return configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Sets cache directory.
         * @param directory cache directory path
         * @return this builder
         */
        public Builder directory(String directory);

        /**
         * Sets maximum cache size.
         * @param maxSize maximum size in bytes
         * @return this builder
         */
        public Builder maxSize(long maxSize);

        /**
         * Sets cache cleanup policy.
         * @param policy cleanup policy
         * @return this builder
         */
        public Builder cleanupPolicy(CacheCleanupPolicy policy);

        /**
         * Builds the cache configuration.
         * @return cache configuration
         */
        public CacheConfig build();
    }

    public enum CacheCleanupPolicy {
        NEVER, LRU, TIME_BASED
    }
}
```

## Usage Examples

### Basic Module Execution

```java
// Create runtime
try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
    // Create engine with optimized configuration
    EngineConfig engineConfig = EngineConfig.builder()
        .optimizationLevel(OptimizationLevel.SPEED)
        .debugInfo(false)
        .build();

    try (Engine engine = runtime.createEngine(engineConfig)) {
        // Compile module
        byte[] wasmBytes = Files.readAllBytes(Paths.get("module.wasm"));
        Module module = engine.compile(wasmBytes);

        // Create store
        Store store = engine.createStore();

        // Instantiate module
        Instance instance = module.instantiate(store, new ImportMap());

        // Call exported function
        WasmFunction addFunction = instance.getFunction("add");
        WasmValue[] results = addFunction.call(
            WasmValue.i32(10),
            WasmValue.i32(20)
        );

        System.out.println("Result: " + results[0].asInt()); // 30
    }
}
```

### WASI Application

```java
// WASI configuration
WasiConfig wasiConfig = WasiConfig.builder()
    .args("program", "arg1", "arg2")
    .env("PATH", "/usr/bin")
    .mapDir("/tmp", "/host/tmp")
    .stdout(System.out)
    .stderr(System.err)
    .build();

try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
    try (Engine engine = runtime.createEngine()) {
        Module module = engine.compileFile("wasi_app.wasm");
        Store store = engine.createStore();

        // Create linker with WASI
        Linker linker = store.createLinker();
        linker.defineWasi(wasiConfig);

        // Instantiate and run
        Instance instance = linker.instantiate(store, module);
        WasmFunction main = instance.getFunction("_start");
        main.call();
    }
}
```

### Host Function Implementation

```java
// Define host function
HostFunction logFunction = (caller, args) -> {
    // Get message from memory
    WasmMemory memory = caller.getMemory("memory");
    int ptr = args[0].asInt();
    int len = args[1].asInt();

    byte[] messageBytes = memory.read(ptr, len);
    String message = new String(messageBytes, StandardCharsets.UTF_8);

    System.out.println("WASM Log: " + message);
    return new WasmValue[0]; // void return
};

// Link host function
Linker linker = store.createLinker();
linker.defineFunction("env", "log", logFunction);
```

---

This API reference provides the complete interface for Wasmtime4j. For implementation examples and best practices, see the [Developer Guides](../guides/) section.
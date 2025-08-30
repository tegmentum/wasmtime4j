# Advanced Usage Guide

This guide covers advanced features of Wasmtime4j including WASI (WebAssembly System Interface) integration, host function implementation, custom imports, and complex scenarios.

## Table of Contents
- [WASI Integration](#wasi-integration)
- [Host Functions](#host-functions)
- [Custom Imports](#custom-imports)
- [Memory Management](#memory-management)
- [Multi-threading](#multi-threading)
- [Performance Optimization](#performance-optimization)
- [Error Recovery](#error-recovery)

## WASI Integration

WASI (WebAssembly System Interface) provides a standardized API for WebAssembly modules to interact with the host system in a secure, sandboxed manner.

### Basic WASI Setup

```java
import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.jni.wasi.*;

try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
    Engine engine = runtime.createEngine();
    
    // Create WASI context with default configuration
    WasiContext wasiContext = WasiContextBuilder.create()
        .inheritStdio()  // Inherit stdin/stdout/stderr from host
        .allowAll()      // Allow all operations (for development only)
        .build();
    
    // Load WASI module
    byte[] wasmBytes = Files.readAllBytes(Paths.get("wasi-program.wasm"));
    Module module = runtime.compileModule(engine, wasmBytes);
    
    // Create imports for WASI
    ImportMap imports = new ImportMap();
    wasiContext.addToImports(imports);
    
    // Instantiate with WASI imports
    Instance instance = runtime.instantiate(module, imports);
    
    // Execute WASI program
    WasmFunction mainFunction = instance.getFunction("_start");
    mainFunction.call();
}
```

### File System Access

WASI provides secure file system access with fine-grained permissions:

```java
// Create WASI context with restricted file system access
WasiContext wasiContext = WasiContextBuilder.create()
    .inheritStdio()
    .preopenDirectory("/tmp/sandbox", "/sandbox") // Map host /tmp/sandbox to guest /sandbox
    .preopenDirectory("/data", "/data", WasiDirectoryAccess.READ_ONLY)
    .build();

// File operations in the WebAssembly module will be sandboxed
```

### Environment Variables and Arguments

```java
WasiContext wasiContext = WasiContextBuilder.create()
    .inheritStdio()
    .args("program-name", "--flag", "value")
    .env("MY_VAR", "my_value")
    .env("PATH", "/usr/bin:/bin")
    .build();
```

### Advanced WASI Configuration

```java
WasiContext wasiContext = WasiContextBuilder.create()
    .inheritStdio()
    .preopenDirectory("/app/data", "/data")
    .args("myprogram", "--config", "config.toml")
    .env("LOG_LEVEL", "info")
    .exitOnReturn(true)  // Exit process when WASI program returns
    .resourceLimits(WasiResourceLimits.builder()
        .maxMemory(64 * 1024 * 1024)  // 64MB memory limit
        .maxOpenFiles(100)
        .maxNetworkConnections(10)
        .build())
    .build();
```

## Host Functions

Host functions allow WebAssembly modules to call back into Java code, enabling powerful integrations.

### Simple Host Function

```java
// Define a host function that adds logging
WasmFunction logFunction = WasmFunction.hostFunction(
    "log",
    FunctionType.of(new WasmValueType[]{WasmValueType.I32, WasmValueType.I32}, 
                   new WasmValueType[]{}),
    (args) -> {
        // args[0] = pointer to string in WebAssembly memory
        // args[1] = length of string
        int ptr = args[0].asI32();
        int len = args[1].asI32();
        
        // Get the instance's memory (you'll need access to the instance)
        WasmMemory memory = currentInstance.getMemory("memory");
        byte[] messageBytes = memory.read(ptr, len);
        String message = new String(messageBytes, StandardCharsets.UTF_8);
        
        System.out.println("[WASM LOG]: " + message);
        return new WasmValue[0];  // No return values
    }
);

// Add to imports
ImportMap imports = new ImportMap();
imports.addFunction("env", "log", logFunction);

Instance instance = runtime.instantiate(module, imports);
```

### Complex Host Function with State

```java
public class DatabaseConnector {
    private final Connection connection;
    
    public DatabaseConnector(Connection connection) {
        this.connection = connection;
    }
    
    public WasmFunction createQueryFunction() {
        return WasmFunction.hostFunction(
            "db_query",
            FunctionType.of(
                new WasmValueType[]{WasmValueType.I32, WasmValueType.I32}, // query ptr, len
                new WasmValueType[]{WasmValueType.I32}), // result count
            (args) -> {
                try {
                    // Extract query from WebAssembly memory
                    int queryPtr = args[0].asI32();
                    int queryLen = args[1].asI32();
                    
                    WasmMemory memory = getCurrentMemory();
                    byte[] queryBytes = memory.read(queryPtr, queryLen);
                    String query = new String(queryBytes, StandardCharsets.UTF_8);
                    
                    // Execute database query
                    try (PreparedStatement stmt = connection.prepareStatement(query);
                         ResultSet rs = stmt.executeQuery()) {
                        
                        int count = 0;
                        while (rs.next()) {
                            count++;
                        }
                        
                        return new WasmValue[]{WasmValue.i32(count)};
                    }
                } catch (SQLException e) {
                    // Handle database errors
                    throw new RuntimeException("Database error: " + e.getMessage());
                }
            }
        );
    }
}

// Usage
DatabaseConnector dbConnector = new DatabaseConnector(dataSource.getConnection());
ImportMap imports = new ImportMap();
imports.addFunction("env", "db_query", dbConnector.createQueryFunction());
```

### Async Host Functions

For long-running operations, you can use async host functions:

```java
public WasmFunction createAsyncHttpFunction() {
    return WasmFunction.hostFunction(
        "http_get",
        FunctionType.of(
            new WasmValueType[]{WasmValueType.I32, WasmValueType.I32, WasmValueType.I32}, 
            new WasmValueType[]{WasmValueType.I32}),
        (args) -> {
            int urlPtr = args[0].asI32();
            int urlLen = args[1].asI32();
            int callbackPtr = args[2].asI32();
            
            // Extract URL
            WasmMemory memory = getCurrentMemory();
            String url = new String(memory.read(urlPtr, urlLen), StandardCharsets.UTF_8);
            
            // Make async HTTP request
            CompletableFuture.supplyAsync(() -> {
                try {
                    HttpResponse<String> response = httpClient.send(
                        HttpRequest.newBuilder().uri(URI.create(url)).build(),
                        HttpResponse.BodyHandlers.ofString());
                    return response.body();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(responseBody -> {
                // Call back into WebAssembly with the result
                // This requires careful synchronization with the WebAssembly execution
                scheduleCallback(callbackPtr, responseBody);
            });
            
            return new WasmValue[]{WasmValue.i32(0)}; // Return immediately
        }
    );
}
```

## Custom Imports

Beyond host functions, you can import globals, memory, and tables:

```java
ImportMap imports = new ImportMap();

// Import a global variable
WasmGlobal sharedCounter = WasmGlobal.mutable(WasmValueType.I32, WasmValue.i32(0));
imports.addGlobal("env", "shared_counter", sharedCounter);

// Import memory with specific size
WasmMemory sharedMemory = WasmMemory.create(1, 10); // 1 page min, 10 pages max
imports.addMemory("env", "shared_memory", sharedMemory);

// Import a table
WasmTable funcTable = WasmTable.create(WasmType.FUNC_REF, 10, 100);
imports.addTable("env", "function_table", funcTable);
```

## Memory Management

### Direct Memory Access

```java
WasmMemory memory = instance.getMemory("memory");

// Efficient bulk operations
ByteBuffer buffer = memory.getBuffer();
buffer.position(offset);
buffer.put(data);  // Direct write

// Safe bounds checking
if (memory.size() >= offset + data.length) {
    memory.write(offset, data);
} else {
    // Handle insufficient memory
    memory.grow(requiredPages);
    memory.write(offset, data);
}
```

### Memory Mapping

```java
// Map a file directly into WebAssembly memory
try (RandomAccessFile file = new RandomAccessFile("data.bin", "r");
     FileChannel channel = file.getChannel()) {
    
    MappedByteBuffer mappedBuffer = channel.map(
        FileChannel.MapMode.READ_ONLY, 0, file.length());
    
    WasmMemory memory = instance.getMemory("memory");
    
    // Copy mapped data to WebAssembly memory
    byte[] data = new byte[mappedBuffer.remaining()];
    mappedBuffer.get(data);
    memory.write(0, data);
}
```

## Multi-threading

WebAssembly itself is single-threaded, but you can coordinate multiple WebAssembly instances:

```java
public class WebAssemblyWorkerPool {
    private final ExecutorService executorService;
    private final List<WasmRuntime> runtimes;
    private final byte[] moduleBytes;
    
    public WebAssemblyWorkerPool(int workerCount, byte[] moduleBytes) {
        this.executorService = Executors.newFixedThreadPool(workerCount);
        this.runtimes = new ArrayList<>();
        this.moduleBytes = moduleBytes;
        
        // Create runtime instances for each worker
        for (int i = 0; i < workerCount; i++) {
            WasmRuntime runtime = WasmRuntimeFactory.create();
            runtimes.add(runtime);
        }
    }
    
    public CompletableFuture<WasmValue[]> executeAsync(String functionName, WasmValue[] args) {
        return CompletableFuture.supplyAsync(() -> {
            WasmRuntime runtime = runtimes.get(
                (int) (Thread.currentThread().getId() % runtimes.size()));
            
            try {
                Engine engine = runtime.createEngine();
                Module module = runtime.compileModule(engine, moduleBytes);
                Instance instance = runtime.instantiate(module);
                
                WasmFunction function = instance.getFunction(functionName);
                return function.call(args);
                
            } catch (WasmException e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    public void shutdown() {
        runtimes.forEach(WasmRuntime::close);
        executorService.shutdown();
    }
}
```

## Performance Optimization

### Module Caching

```java
public class ModuleCache {
    private final Map<String, Module> cache = new ConcurrentHashMap<>();
    private final Engine engine;
    
    public ModuleCache(Engine engine) {
        this.engine = engine;
    }
    
    public Module getModule(String moduleId, byte[] wasmBytes) throws WasmException {
        return cache.computeIfAbsent(moduleId, id -> {
            try {
                return engine.compileModule(wasmBytes);
            } catch (WasmException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
```

### Connection Pooling

```java
public class InstancePool {
    private final BlockingQueue<Instance> pool = new ArrayBlockingQueue<>(10);
    private final Module module;
    
    public InstancePool(Module module) throws WasmException {
        this.module = module;
        
        // Pre-create instances
        for (int i = 0; i < 10; i++) {
            pool.offer(runtime.instantiate(module));
        }
    }
    
    public <T> T execute(String functionName, WasmValue[] args, 
                        Function<WasmValue[], T> resultProcessor) 
                        throws InterruptedException, WasmException {
        Instance instance = pool.take();
        try {
            WasmFunction function = instance.getFunction(functionName);
            WasmValue[] results = function.call(args);
            return resultProcessor.apply(results);
        } finally {
            pool.offer(instance);
        }
    }
}
```

## Error Recovery

### Graceful Error Handling

```java
public class RobustWebAssemblyExecutor {
    private final WasmRuntime runtime;
    private final Module module;
    private final int maxRetries;
    
    public WasmValue[] executeWithRetry(String functionName, WasmValue[] args) {
        Exception lastException = null;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                Instance instance = runtime.instantiate(module);
                WasmFunction function = instance.getFunction(functionName);
                return function.call(args);
                
            } catch (ai.tegmentum.wasmtime4j.exception.RuntimeException e) {
                lastException = e;
                
                // Check if this is a recoverable error
                if (isRecoverable(e)) {
                    // Wait before retry
                    try {
                        Thread.sleep(100 * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                } else {
                    // Non-recoverable error, fail immediately
                    throw e;
                }
                
            } catch (Exception e) {
                lastException = e;
                // Other exceptions are also non-recoverable
                throw new RuntimeException("WebAssembly execution failed", e);
            }
        }
        
        throw new RuntimeException("WebAssembly execution failed after " + 
                                 maxRetries + " attempts", lastException);
    }
    
    private boolean isRecoverable(ai.tegmentum.wasmtime4j.exception.RuntimeException e) {
        // Define your retry logic based on error type/message
        String message = e.getMessage().toLowerCase();
        return message.contains("out of memory") || 
               message.contains("timeout") ||
               message.contains("resource temporarily unavailable");
    }
}
```

### Circuit Breaker Pattern

```java
public class WebAssemblyCircuitBreaker {
    private enum State { CLOSED, OPEN, HALF_OPEN }
    
    private State state = State.CLOSED;
    private int failureCount = 0;
    private long lastFailureTime = 0;
    private final int failureThreshold = 5;
    private final long timeout = 60000; // 60 seconds
    
    public WasmValue[] execute(String functionName, WasmValue[] args) 
            throws WasmException {
        
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime > timeout) {
                state = State.HALF_OPEN;
            } else {
                throw new WasmException("Circuit breaker is OPEN");
            }
        }
        
        try {
            WasmValue[] result = executeInternal(functionName, args);
            onSuccess();
            return result;
            
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }
    
    private void onSuccess() {
        failureCount = 0;
        state = State.CLOSED;
    }
    
    private void onFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
        
        if (failureCount >= failureThreshold) {
            state = State.OPEN;
        }
    }
}
```

## Best Practices

1. **Resource Management**: Always close runtimes and instances properly
2. **Error Handling**: Use specific exception types and implement retry logic
3. **Performance**: Cache compiled modules and pool instances
4. **Security**: Use WASI with restrictive permissions in production
5. **Monitoring**: Log execution times and error rates
6. **Testing**: Test with various WebAssembly modules and edge cases

## Next Steps

- Check out the [Performance Guide](performance.md) for optimization tips
- Read about [Security considerations](security.md)
- Browse [Integration Examples](../examples/integration/) for framework-specific patterns
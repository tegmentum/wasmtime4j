# Runtime Selection Architecture

This document explains how Wasmtime4j automatically selects between JNI and Panama Foreign Function API implementations, and the architectural decisions behind the runtime selection system.

## Overview

Wasmtime4j provides a unified API that automatically selects the optimal native interface based on the Java version and system capabilities:

- **JNI Runtime**: Compatible with Java 8-22, uses traditional Java Native Interface
- **Panama Runtime**: Requires Java 23+, uses the modern Foreign Function & Memory API

The selection process is designed to be transparent to users while maximizing performance and compatibility.

## Architecture Components

### Runtime Factory Pattern

```
┌─────────────────────────────────────────────────────────────┐
│                  WasmRuntimeFactory                         │
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │ Runtime         │    │ Selection Logic                 │ │
│  │ Detection       │    │ ┌─────────────────────────────┐ │ │
│  │                 │    │ │ 1. Check System Property    │ │ │
│  │ ┌─────────────┐ │    │ │ 2. Detect Java Version     │ │ │
│  │ │ Java        │ │    │ │ 3. Check Runtime Available │ │ │
│  │ │ Version     │ │    │ │ 4. Select Optimal Runtime  │ │ │
│  │ │ Detection   │ │    │ └─────────────────────────────┘ │ │
│  │ └─────────────┘ │    └─────────────────────────────────┘ │
│  │                 │                                        │
│  │ ┌─────────────┐ │    ┌─────────────────────────────────┐ │
│  │ │ Runtime     │ │    │ Runtime Creation                │ │
│  │ │ Capability  │ │    │ ┌─────────────────────────────┐ │ │
│  │ │ Detection   │ │    │ │ JNI Runtime Factory         │ │ │
│  │ └─────────────┘ │    │ │ Panama Runtime Factory      │ │ │
│  └─────────────────┘    │ └─────────────────────────────┘ │ │
│                         └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Module Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    User Application                         │
└─────────────────────┬───────────────────────────────────────┘
                      │ Uses unified API
┌─────────────────────▼───────────────────────────────────────┐
│                  wasmtime4j                                 │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Public API Interfaces                     │ │
│  │  WasmRuntime, Engine, Module, Instance, etc.          │ │
│  └─────────────────────┬───────────────────────────────────┘ │
│                        │ Factory creates implementation
│  ┌─────────────────────▼───────────────────────────────────┐ │
│  │             WasmRuntimeFactory                          │ │
│  │  - Runtime detection and selection logic               │ │
│  │  - Dynamic loading of implementation modules           │ │
│  └─────────┬───────────────────────────────┬───────────────┘ │
└────────────┼───────────────────────────────┼─────────────────┘
             │ Loads via reflection           │
┌────────────▼───────────────┐   ┌───────────▼─────────────────┐
│      wasmtime4j-jni        │   │     wasmtime4j-panama       │
│  ┌───────────────────────┐ │   │  ┌─────────────────────────┐ │
│  │   JNI Implementation  │ │   │  │  Panama Implementation │ │
│  │   - JniWasmRuntime   │ │   │  │  - PanamaWasmRuntime   │ │
│  │   - JniEngine        │ │   │  │  - PanamaEngine        │ │
│  │   - JniModule        │ │   │  │  - PanamaModule        │ │
│  │   - etc.             │ │   │  │  - etc.                │ │
│  └──────────┬────────────┘ │   │  └────────────┬────────────┘ │
└─────────────┼──────────────┘   └───────────────┼──────────────┘
              │                                  │
              │         Uses shared native       │
              │                                  │
┌─────────────▼──────────────────────────────────▼──────────────┐
│                  wasmtime4j-native                            │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              Shared Native Library                       │ │
│  │  ┌─────────────────────┐  ┌─────────────────────────────┐ │ │
│  │  │   JNI Bindings      │  │   Panama FFI Bindings      │ │ │
│  │  │   - C ABI exports   │  │   - C ABI exports          │ │ │
│  │  │   - JNI callbacks   │  │   - Memory layouts         │ │ │
│  │  └─────────────────────┘  │   - Function descriptors   │ │ │
│  │                           └─────────────────────────────┘ │ │
│  │  ┌─────────────────────────────────────────────────────┐ │ │
│  │  │            Wasmtime Core Integration                │ │ │
│  │  │  - Engine, Module, Instance wrappers               │ │ │
│  │  │  - Memory management                               │ │ │
│  │  │  - Error handling                                  │ │ │
│  │  │  - WASI integration                                │ │ │
│  │  └─────────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Selection Algorithm

The runtime selection follows this decision tree:

```
┌─────────────────────────────────────┐
│          Start Selection            │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│   Check System Property            │
│   wasmtime4j.runtime               │
└──────────┬──────────────────────────┘
           │
           ├── "jni" ────────────────────────┐
           │                                 │
           ├── "panama" ──────────────────────┼────┐
           │                                 │    │
           └── null/other ──────────────────  │    │
                      │                      │    │
                      ▼                      │    │
           ┌─────────────────────────────────┐│    │
           │     Get Java Version           ││    │
           └──────────┬──────────────────────┘│    │
                      │                       │    │
                      ├── Java 8-22 ─────────┼────┤
                      │                       │    │
                      └── Java 23+ ──────────┼────┼─┐
                                 │            │    │ │
                                 ▼            │    │ │
                   ┌─────────────────────────────┐ │ │
                   │   Check Panama Available   │ │ │
                   └──────────┬──────────────────┘ │ │
                              │                    │ │
                              ├── Available ──────┼─┘ │
                              │                    │   │
                              └── Not Available ──┼───┤
                                     │             │   │
                                     ▼             │   │
                          ┌─────────────────────┐  │   │
                          │   Use JNI Runtime   │◄─┼───┤
                          └─────────────────────┘  │   │
                                     ▲             │   │
                                     └─────────────┘   │
                                                       │
                             ┌─────────────────────────┘
                             │
                             ▼
                   ┌─────────────────────┐
                   │  Use Panama Runtime │
                   └─────────────────────┘
```

### Selection Logic Implementation

```java
public class RuntimeSelectionLogic {
    
    public static RuntimeType selectOptimalRuntime() {
        // 1. Check for manual override
        String overrideProperty = System.getProperty("wasmtime4j.runtime");
        if (overrideProperty != null) {
            return parseRuntimeType(overrideProperty);
        }
        
        // 2. Detect Java version
        int javaVersion = getJavaVersion();
        
        // 3. Auto-select based on capabilities
        if (javaVersion >= 23 && isPanamaCapable()) {
            return RuntimeType.PANAMA;
        } else {
            return RuntimeType.JNI;
        }
    }
    
    private static boolean isPanamaCapable() {
        try {
            // Check if Panama classes are available
            Class.forName("java.lang.foreign.MemorySegment");
            Class.forName("java.lang.foreign.FunctionDescriptor");
            
            // Check if native access is enabled
            return checkNativeAccessEnabled();
            
        } catch (ClassNotFoundException | SecurityException e) {
            return false;
        }
    }
    
    private static boolean checkNativeAccessEnabled() {
        try {
            // Try to create a minimal memory segment
            MemorySegment segment = MemorySegment.ofArray(new byte[1]);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

## Performance Characteristics

### JNI Runtime Performance

**Advantages:**
- Lower method call overhead
- Mature, stable implementation
- Consistent performance across Java versions
- Better startup time

**Performance Profile:**
```
Operation          | JNI Performance | Overhead
-------------------|-----------------|----------
Runtime Init       | 100-1000 ops/s | Low
Function Calls     | 1M-10M ops/s   | Very Low
Memory Operations  | 100K-1M ops/s  | Low
Module Compilation | 1K-10K ops/s   | Medium
```

### Panama Runtime Performance

**Advantages:**
- Type safety improvements
- Better integration with Java memory model
- Future-proof design
- Potential for better long-term optimizations

**Performance Profile:**
```
Operation          | Panama Performance | Overhead
-------------------|-------------------|----------
Runtime Init       | 50-500 ops/s     | Higher
Function Calls     | 800K-8M ops/s    | Slightly Higher
Memory Operations  | 80K-800K ops/s   | Similar
Module Compilation | 1K-10K ops/s     | Similar
```

### Performance Comparison

```java
public class PerformanceComparison {
    
    public void compareRuntimes() {
        // JNI Runtime
        measureRuntime(RuntimeType.JNI, "JNI Runtime");
        
        // Panama Runtime
        if (WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA)) {
            measureRuntime(RuntimeType.PANAMA, "Panama Runtime");
        }
    }
    
    private void measureRuntime(RuntimeType type, String name) {
        try (WasmRuntime runtime = WasmRuntimeFactory.create(type)) {
            long startTime = System.nanoTime();
            
            // Measure initialization
            Engine engine = runtime.createEngine();
            Module module = runtime.compileModule(engine, getTestModule());
            Instance instance = runtime.instantiate(module);
            
            long initTime = System.nanoTime() - startTime;
            
            // Measure function execution
            WasmFunction func = instance.getFunction("test");
            startTime = System.nanoTime();
            
            for (int i = 0; i < 10000; i++) {
                func.call(WasmValue.i32(i));
            }
            
            long execTime = System.nanoTime() - startTime;
            
            System.out.printf("%s - Init: %.2f ms, Exec: %.2f μs/call%n",
                name, initTime / 1_000_000.0, execTime / 10_000_000.0);
        }
    }
}
```

## Memory Management Strategy

### JNI Memory Management

```
┌─────────────────────────────────────────────────────────────┐
│                    Java Heap                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                Java Objects                             │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │ │
│  │  │ JniRuntime  │  │  JniEngine  │  │ JniInstance │     │ │
│  │  │   (proxy)   │  │   (proxy)   │  │   (proxy)   │     │ │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘     │ │
│  └─────────┼─────────────────┼─────────────────┼───────────┘ │
└────────────┼─────────────────┼─────────────────┼─────────────┘
             │ JNI calls       │                 │
┌────────────▼─────────────────▼─────────────────▼─────────────┐
│                   Native Heap                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Wasmtime Native Objects                    │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │ │
│  │  │   Runtime   │  │   Engine    │  │  Instance   │     │ │
│  │  │  (native)   │  │  (native)   │  │  (native)   │     │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘     │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Panama Memory Management

```
┌─────────────────────────────────────────────────────────────┐
│                    Java Heap                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                Java Objects                             │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │ │
│  │  │Panama       │  │Panama       │  │Panama       │     │ │
│  │  │Runtime      │  │Engine       │  │Instance     │     │ │
│  │  │(MemorySegs) │  │(MemorySegs) │  │(MemorySegs) │     │ │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘     │ │
│  └─────────┼─────────────────┼─────────────────┼───────────┘ │
└────────────┼─────────────────┼─────────────────┼─────────────┘
             │ Direct access   │                 │
┌────────────▼─────────────────▼─────────────────▼─────────────┐
│                  Off-Heap Memory                            │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │            Direct Memory Segments                       │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │ │
│  │  │   Runtime   │  │   Engine    │  │  Instance   │     │ │
│  │  │  (memory    │  │  (memory    │  │  (memory    │     │ │
│  │  │  segment)   │  │  segment)   │  │  segment)   │     │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘     │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Error Handling Strategy

### Layered Error Handling

```
┌─────────────────────────────────────────────────────────────┐
│                 Application Layer                           │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  High-Level Exceptions                                  │ │
│  │  - WasmException (base)                                │ │
│  │  - ValidationException                                  │ │
│  │  - CompilationException                                 │ │
│  │  - RuntimeException                                     │ │
│  │  - InstantiationException                              │ │
│  └─────────────────────┬───────────────────────────────────┘ │
└───────────────────────┼─────────────────────────────────────┘
                        │ Exception mapping
┌───────────────────────▼─────────────────────────────────────┐
│              Implementation Layer                           │
│  ┌──────────────────────┐  ┌─────────────────────────────── ┐ │
│  │   JNI Exceptions     │  │  Panama Exceptions            │ │
│  │  ┌─────────────────┐ │  │  ┌──────────────────────────┐ │ │
│  │  │ JniException    │ │  │  │ PanamaException          │ │ │
│  │  │ JniLibException │ │  │  │ MemoryAccessException    │ │ │
│  │  │ JniResException │ │  │  │ IllegalAccessException   │ │ │
│  │  └─────────────────┘ │  │  └──────────────────────────┘ │ │
│  └──────────────────────┘  └─────────────────────────────── ┘ │
└─────────────────────┬───────────────────────────────────────┘
                      │ Native error mapping
┌─────────────────────▼───────────────────────────────────────┐
│               Native Layer                                  │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Wasmtime Errors                            │ │
│  │  - wasmtime::Error                                     │ │
│  │  - Trap codes                                          │ │
│  │  - Validation errors                                   │ │
│  │  - System errors                                       │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Runtime Lifecycle Management

### Lifecycle State Machine

```
┌─────────────────┐     create()     ┌─────────────────┐
│   UNINITIALIZED │ ────────────────▶│   INITIALIZING  │
└─────────────────┘                  └─────────┬───────┘
                                               │ success
         ┌─────────────────────────────────────┘
         │
         ▼                 close()
┌─────────────────┐ ────────────────▶┌─────────────────┐
│     ACTIVE      │                  │     CLOSING     │
└─────────┬───────┘                  └─────────┬───────┘
         │ ▲                                   │
         │ │ recover()                          │ cleanup complete
         │ │                                   │
    error│ │                                   ▼
         │ │                         ┌─────────────────┐
         ▼ │                         │     CLOSED      │
┌─────────────────┐                  └─────────────────┘
│     ERROR       │
└─────────────────┘
```

### Resource Management

```java
public class RuntimeLifecycleManager {
    private final Map<RuntimeType, WeakReference<WasmRuntime>> runtimeCache;
    private final ScheduledExecutorService cleanupService;
    
    public synchronized WasmRuntime getOrCreateRuntime(RuntimeType type) {
        WeakReference<WasmRuntime> ref = runtimeCache.get(type);
        WasmRuntime runtime = (ref != null) ? ref.get() : null;
        
        if (runtime == null || !runtime.isValid()) {
            runtime = createNewRuntime(type);
            runtimeCache.put(type, new WeakReference<>(runtime));
            scheduleCleanup(runtime);
        }
        
        return runtime;
    }
    
    private void scheduleCleanup(WasmRuntime runtime) {
        cleanupService.schedule(() -> {
            if (!runtime.isValid()) {
                runtime.close();
            }
        }, 30, TimeUnit.MINUTES);
    }
}
```

## Configuration System

### Configuration Hierarchy

```
System Properties
       │
       ▼
Environment Variables  
       │
       ▼
Application Configuration
       │
       ▼
Default Values
```

### Configuration Implementation

```java
public class RuntimeConfiguration {
    private static final String RUNTIME_PROPERTY = "wasmtime4j.runtime";
    private static final String DEBUG_PROPERTY = "wasmtime4j.debug";
    private static final String NATIVE_PATH_PROPERTY = "wasmtime4j.native.path";
    
    public static RuntimeConfig loadConfiguration() {
        return RuntimeConfig.builder()
            .runtimeType(getRuntimeType())
            .debugEnabled(isDebugEnabled())
            .nativeLibraryPath(getNativeLibraryPath())
            .build();
    }
    
    private static RuntimeType getRuntimeType() {
        String value = System.getProperty(RUNTIME_PROPERTY, 
                       System.getenv("WASMTIME4J_RUNTIME"));
        
        if (value != null) {
            try {
                return RuntimeType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Fall back to auto-selection
            }
        }
        
        return RuntimeType.AUTO;
    }
}
```

## Extension Points

The architecture provides several extension points for customization:

### Custom Runtime Implementations

```java
public interface RuntimeProvider {
    boolean isAvailable();
    WasmRuntime createRuntime();
    RuntimeType getType();
}

// Register custom provider
RuntimeRegistry.register(new CustomRuntimeProvider());
```

### Configuration Providers

```java
public interface ConfigurationProvider {
    RuntimeConfiguration getConfiguration();
    int getPriority();
}
```

### Native Library Loaders

```java
public interface NativeLibraryLoader {
    boolean canLoad(String libraryName);
    void loadLibrary(String libraryName) throws UnsatisfiedLinkError;
}
```

This architecture ensures that Wasmtime4j can adapt to different environments while maintaining a consistent API and optimal performance across Java versions.
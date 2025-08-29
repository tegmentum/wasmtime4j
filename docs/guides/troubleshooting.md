# Troubleshooting Guide

This guide helps you diagnose and resolve common issues when using Wasmtime4j in various environments and use cases.

## Table of Contents
- [Installation Issues](#installation-issues)
- [Runtime Selection Problems](#runtime-selection-problems)
- [Module Loading and Compilation](#module-loading-and-compilation)
- [Function Execution Issues](#function-execution-issues)
- [Memory and Resource Problems](#memory-and-resource-problems)
- [Performance Issues](#performance-issues)
- [Platform-Specific Issues](#platform-specific-issues)
- [WASI Integration Problems](#wasi-integration-problems)
- [Integration Issues](#integration-issues)
- [Debugging Techniques](#debugging-techniques)

## Installation Issues

### Dependency Not Found

**Problem**: Maven/Gradle can't find the Wasmtime4j dependency.

**Symptoms**:
```
Could not find ai.tegmentum:wasmtime4j:1.0.0-SNAPSHOT
```

**Solutions**:
1. **Check repository configuration**:
   ```xml
   <repositories>
     <repository>
       <id>sonatype-snapshots</id>
       <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
       <snapshots><enabled>true</enabled></snapshots>
     </repository>
   </repositories>
   ```

2. **Build from source**:
   ```bash
   git clone https://github.com/tegmentum/wasmtime4j.git
   cd wasmtime4j
   ./mvnw clean install
   ```

3. **Verify version**:
   ```xml
   <dependency>
     <groupId>ai.tegmentum</groupId>
     <artifactId>wasmtime4j</artifactId>
     <version>1.0.0-SNAPSHOT</version>
   </dependency>
   ```

### Native Library Loading Failures

**Problem**: Native libraries fail to load at runtime.

**Symptoms**:
```
java.lang.UnsatisfiedLinkError: no wasmtime4j in java.library.path
NativeLibraryException: Failed to load native library
```

**Solutions**:
1. **Check Java architecture**:
   ```bash
   java -version  # Verify x64/ARM64 matches your system
   ```

2. **Verify native library extraction**:
   ```java
   System.setProperty("wasmtime4j.debug", "true");
   // Enable debug logging to see library loading details
   ```

3. **Manual library path**:
   ```bash
   java -Djava.library.path=/path/to/natives YourApp
   # Or set wasmtime4j.native.path system property
   ```

4. **Check file permissions**:
   ```bash
   # Ensure extracted libraries are executable
   chmod +x /tmp/wasmtime4j/libwasmtime4j.so
   ```

## Runtime Selection Problems

### Panama Runtime Not Available on Java 23+

**Problem**: Panama runtime fails to load despite Java 23+.

**Symptoms**:
```
PanamaException: Panama runtime not available
Falling back to JNI runtime
```

**Solutions**:
1. **Enable preview features**:
   ```bash
   java --enable-preview --enable-native-access=ALL-UNNAMED YourApp
   ```

2. **Check Java version**:
   ```bash
   java --version  # Ensure it's actually Java 23+
   ```

3. **Verify module access**:
   ```bash
   # For modular applications
   --add-modules java.base
   --enable-native-access=your.module.name
   ```

### JNI Runtime Failures

**Problem**: JNI runtime fails to initialize.

**Symptoms**:
```
JniException: Failed to initialize JNI runtime
java.lang.UnsatisfiedLinkError: JNI method not found
```

**Solutions**:
1. **Check JNI headers**:
   ```bash
   javah -classpath target/classes ai.tegmentum.wasmtime4j.jni.JniWasmRuntime
   ```

2. **Verify native compilation**:
   ```bash
   cd wasmtime4j-native
   ./scripts/build-native.sh
   ```

3. **Check JNI version compatibility**:
   ```java
   System.out.println("JNI Version: " + System.getProperty("java.vm.version"));
   ```

## Module Loading and Compilation

### Invalid WebAssembly Module

**Problem**: Module validation or compilation fails.

**Symptoms**:
```
ValidationException: Invalid WebAssembly module
CompilationException: Module compilation failed
```

**Solutions**:
1. **Validate module with external tools**:
   ```bash
   wasm-validate your-module.wasm
   wasm-objdump -h your-module.wasm
   ```

2. **Check module format**:
   ```java
   // Ensure you're loading binary (.wasm), not text (.wat)
   byte[] wasmBytes = Files.readAllBytes(Paths.get("module.wasm"));
   // Not: Files.readAllBytes(Paths.get("module.wat"));
   ```

3. **Verify module completeness**:
   ```bash
   # Check if all required sections are present
   wasm-objdump -s your-module.wasm
   ```

4. **Enable debug information**:
   ```java
   EngineConfig config = EngineConfig.builder()
       .enableDebugInfo(true)
       .optimizationLevel(OptimizationLevel.NONE)
       .build();
   ```

### Module Size Limitations

**Problem**: Large modules fail to load.

**Symptoms**:
```
OutOfMemoryError: Java heap space
CompilationException: Module too large
```

**Solutions**:
1. **Increase heap size**:
   ```bash
   java -Xmx4g YourApp
   ```

2. **Enable streaming compilation**:
   ```java
   EngineConfig config = EngineConfig.builder()
       .optimizationLevel(OptimizationLevel.NONE)  // Faster compilation
       .enableDebugInfo(false)  // Reduce memory usage
       .build();
   ```

3. **Split large modules**:
   ```bash
   # Use tools to split modules into smaller chunks
   wasm-split large-module.wasm -o chunk1.wasm -o chunk2.wasm
   ```

## Function Execution Issues

### Function Not Found

**Problem**: Exported function cannot be found.

**Symptoms**:
```
RuntimeException: Function 'my_function' not found
```

**Solutions**:
1. **List available exports**:
   ```java
   Instance instance = runtime.instantiate(module);
   ExportType[] exports = instance.getExports();
   for (ExportType export : exports) {
       System.out.println("Export: " + export.getName() + " (" + export.getType() + ")");
   }
   ```

2. **Check function name in module**:
   ```bash
   wasm-objdump -x module.wasm | grep -A 10 "Export\["
   ```

3. **Verify export section**:
   ```wat
   (module
     (func $my_function (export "my_function") ...)
   )
   ```

### Wrong Function Signature

**Problem**: Function called with incorrect arguments.

**Symptoms**:
```
RuntimeException: Function signature mismatch
TypeError: Expected 2 arguments, got 1
```

**Solutions**:
1. **Check function signature**:
   ```java
   WasmFunction func = instance.getFunction("my_function");
   FunctionType signature = func.getType();
   System.out.println("Parameters: " + Arrays.toString(signature.getParameterTypes()));
   System.out.println("Results: " + Arrays.toString(signature.getResultTypes()));
   ```

2. **Use correct argument types**:
   ```java
   // Correct
   WasmValue[] args = {WasmValue.i32(42), WasmValue.f32(3.14f)};
   
   // Incorrect - type mismatch
   WasmValue[] args = {WasmValue.i64(42), WasmValue.f64(3.14)};
   ```

3. **Inspect with external tools**:
   ```bash
   wasm-objdump -x module.wasm | grep -A 5 "func\["
   ```

### Runtime Traps

**Problem**: WebAssembly function traps during execution.

**Symptoms**:
```
RuntimeException: WebAssembly trap: integer overflow
RuntimeException: WebAssembly trap: out of bounds memory access
```

**Solutions**:
1. **Enable trap debugging**:
   ```java
   EngineConfig config = EngineConfig.builder()
       .enableDebugInfo(true)
       .build();
   ```

2. **Check memory bounds**:
   ```java
   WasmMemory memory = instance.getMemory("memory");
   int maxOffset = memory.size() * 65536 - 1;  // Page size is 64KB
   System.out.println("Max valid memory offset: " + maxOffset);
   ```

3. **Validate input ranges**:
   ```java
   // Check for integer overflow before calling
   if (a > Integer.MAX_VALUE - b) {
       throw new IllegalArgumentException("Addition would overflow");
   }
   ```

## Memory and Resource Problems

### Memory Leaks

**Problem**: Memory usage grows over time.

**Symptoms**:
```
OutOfMemoryError after extended use
Gradual increase in heap usage
```

**Solutions**:
1. **Properly close resources**:
   ```java
   try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
       // Use runtime
   } // Automatically closed
   
   // Or manually
   runtime.close();
   ```

2. **Reuse instances when possible**:
   ```java
   // Bad - creates new instance each time
   for (int i = 0; i < 1000; i++) {
       Instance instance = runtime.instantiate(module);
       // use instance
   }
   
   // Good - reuse single instance
   Instance instance = runtime.instantiate(module);
   for (int i = 0; i < 1000; i++) {
       // use same instance
   }
   ```

3. **Monitor memory usage**:
   ```java
   Runtime javaRuntime = Runtime.getRuntime();
   long used = javaRuntime.totalMemory() - javaRuntime.freeMemory();
   System.out.println("Memory used: " + (used / 1024 / 1024) + " MB");
   ```

### WebAssembly Memory Issues

**Problem**: WebAssembly memory operations fail.

**Symptoms**:
```
RuntimeException: out of bounds memory access
Memory growth failed
```

**Solutions**:
1. **Check memory limits**:
   ```java
   WasmMemory memory = instance.getMemory("memory");
   System.out.println("Current size: " + memory.size() + " pages");
   System.out.println("Can grow: " + memory.canGrow());
   ```

2. **Grow memory before use**:
   ```java
   int requiredPages = (dataSize + 65535) / 65536;  // Round up to page boundary
   if (memory.size() < requiredPages) {
       memory.grow(requiredPages - memory.size());
   }
   ```

3. **Use bounds checking**:
   ```java
   public void safeMemoryWrite(WasmMemory memory, int offset, byte[] data) {
       int maxSize = memory.size() * 65536;
       if (offset + data.length > maxSize) {
           throw new IllegalArgumentException("Write would exceed memory bounds");
       }
       memory.write(offset, data);
   }
   ```

## Performance Issues

### Slow Function Execution

**Problem**: WebAssembly functions execute slower than expected.

**Solutions**:
1. **Warm up functions**:
   ```java
   // Execute function multiple times to trigger JIT compilation
   WasmValue[] args = {WasmValue.i32(0)};
   for (int i = 0; i < 10000; i++) {
       function.call(args);
   }
   ```

2. **Use optimized compilation**:
   ```java
   EngineConfig config = EngineConfig.builder()
       .optimizationLevel(OptimizationLevel.SPEED)
       .build();
   ```

3. **Profile execution**:
   ```java
   long startTime = System.nanoTime();
   WasmValue[] results = function.call(args);
   long executionTime = System.nanoTime() - startTime;
   System.out.println("Execution time: " + (executionTime / 1_000_000.0) + " ms");
   ```

### Module Compilation Too Slow

**Problem**: Large modules take too long to compile.

**Solutions**:
1. **Cache compiled modules**:
   ```java
   Map<String, Module> moduleCache = new ConcurrentHashMap<>();
   Module module = moduleCache.computeIfAbsent(moduleId, id -> {
       try {
           return runtime.compileModule(engine, wasmBytes);
       } catch (WasmException e) {
           throw new RuntimeException(e);
       }
   });
   ```

2. **Reduce optimization level**:
   ```java
   EngineConfig config = EngineConfig.builder()
       .optimizationLevel(OptimizationLevel.NONE)  // Fastest compilation
       .build();
   ```

3. **Precompile modules**:
   ```bash
   # Use wasmtime CLI to precompile modules
   wasmtime compile module.wasm -o module.cwasm
   ```

## Platform-Specific Issues

### macOS Security Issues

**Problem**: macOS Gatekeeper blocks native libraries.

**Symptoms**:
```
"libwasmtime4j.dylib" cannot be opened because the developer cannot be verified
```

**Solutions**:
1. **Allow unsigned libraries**:
   ```bash
   sudo spctl --master-disable  # Disable Gatekeeper (not recommended)
   ```

2. **Manual approval**:
   ```bash
   xattr -d com.apple.quarantine /path/to/libwasmtime4j.dylib
   ```

3. **Code signing** (for distribution):
   ```bash
   codesign --force --deep --sign "Developer ID" libwasmtime4j.dylib
   ```

### Linux Library Compatibility

**Problem**: Native library incompatible with Linux distribution.

**Symptoms**:
```
/lib64/libc.so.6: version `GLIBC_2.32' not found
```

**Solutions**:
1. **Check glibc version**:
   ```bash
   ldd --version
   ```

2. **Use compatible build**:
   ```bash
   # Build on older Linux for better compatibility
   docker run --rm -v $PWD:/work centos:7 ./build-native.sh
   ```

3. **Static linking**:
   ```toml
   # In Cargo.toml
   [target.x86_64-unknown-linux-gnu]
   rustflags = ["-C", "target-feature=+crt-static"]
   ```

### Windows Path Issues

**Problem**: Windows path handling causes issues.

**Solutions**:
1. **Use forward slashes**:
   ```java
   Path wasmFile = Paths.get("src/main/resources/wasm/module.wasm");
   ```

2. **Handle long paths**:
   ```java
   System.setProperty("wasmtime4j.native.path", "\\\\?\\C:\\very\\long\\path");
   ```

## WASI Integration Problems

### File Access Denied

**Problem**: WASI module cannot access files.

**Symptoms**:
```
WasiException: Permission denied
WasiFileSystemException: No such file or directory
```

**Solutions**:
1. **Check directory preopen**:
   ```java
   WasiContext context = WasiContextBuilder.create()
       .preopenDirectory("/host/path", "/guest/path")
       .build();
   ```

2. **Verify permissions**:
   ```bash
   # Check host directory permissions
   ls -la /host/path
   ```

3. **Enable debugging**:
   ```java
   WasiContext context = WasiContextBuilder.create()
       .inheritStdio()  // See WASI debug output
       .build();
   ```

### Environment Variable Issues

**Problem**: WASI module cannot access environment variables.

**Solutions**:
1. **Explicitly set environment**:
   ```java
   WasiContext context = WasiContextBuilder.create()
       .env("PATH", System.getenv("PATH"))
       .env("HOME", System.getenv("HOME"))
       .build();
   ```

2. **Inherit all environment**:
   ```java
   WasiContextBuilder builder = WasiContextBuilder.create();
   for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
       builder.env(entry.getKey(), entry.getValue());
   }
   WasiContext context = builder.build();
   ```

## Integration Issues

### Spring Boot Issues

**Problem**: WebAssembly integration fails in Spring Boot.

**Solutions**:
1. **Check bean lifecycle**:
   ```java
   @PreDestroy
   public void cleanup() {
       if (wasmRuntime != null) {
           wasmRuntime.close();
       }
   }
   ```

2. **Configure thread pool**:
   ```java
   @Bean
   public TaskExecutor wasmExecutor() {
       ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
       executor.setCorePoolSize(4);
       executor.setMaxPoolSize(8);
       return executor;
   }
   ```

3. **Handle classpath resources**:
   ```java
   @Value("classpath:wasm/module.wasm")
   Resource wasmResource;
   
   byte[] wasmBytes = wasmResource.getInputStream().readAllBytes();
   ```

### Docker Deployment Issues

**Problem**: Application fails in Docker containers.

**Solutions**:
1. **Check base image**:
   ```dockerfile
   # Use JDK image with glibc
   FROM openjdk:23-jdk
   # Not: FROM openjdk:23-alpine (musl libc incompatible)
   ```

2. **Copy native libraries**:
   ```dockerfile
   COPY --from=builder /app/natives /app/natives
   ENV LD_LIBRARY_PATH=/app/natives:$LD_LIBRARY_PATH
   ```

3. **Set memory limits**:
   ```dockerfile
   ENV JAVA_OPTS="-Xmx2g -XX:MaxDirectMemorySize=1g"
   ```

## Debugging Techniques

### Enable Debug Logging

```java
// System property method
System.setProperty("wasmtime4j.debug", "true");

// Logging configuration
Logger.getLogger("ai.tegmentum.wasmtime4j").setLevel(Level.FINE);
```

### Use JVM Debugging Flags

```bash
# Memory debugging
java -XX:+PrintGC -XX:+PrintGCDetails YourApp

# JNI debugging
java -verbose:jni YourApp

# Module loading debugging
java -verbose:class YourApp
```

### WebAssembly Module Inspection

```bash
# Disassemble module
wasm-objdump -d module.wasm

# View module structure
wasm-objdump -h module.wasm

# Validate module
wasm-validate module.wasm

# Convert to WAT for inspection
wasm2wat module.wasm -o module.wat
```

### Runtime Diagnostics

```java
public void diagnoseRuntime() {
    System.out.println("Java Version: " + System.getProperty("java.version"));
    System.out.println("OS: " + System.getProperty("os.name"));
    System.out.println("Arch: " + System.getProperty("os.arch"));
    
    for (RuntimeType type : RuntimeType.values()) {
        System.out.println(type + " available: " + 
            WasmRuntimeFactory.isRuntimeAvailable(type));
    }
    
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
        RuntimeInfo info = runtime.getRuntimeInfo();
        System.out.println("Selected runtime: " + info.getRuntimeType());
        System.out.println("Version: " + info.getVersion());
        System.out.println("Implementation: " + info.getImplementation());
    }
}
```

## Getting Help

If you've tried the solutions above and still have issues:

1. **Check the GitHub Issues**: [https://github.com/tegmentum/wasmtime4j/issues](https://github.com/tegmentum/wasmtime4j/issues)
2. **Create a minimal reproduction case**
3. **Include your environment details**:
   - Java version and vendor
   - Operating system and architecture
   - Wasmtime4j version
   - WebAssembly module (if possible to share)
   - Complete stack trace

4. **Enable debug logging** and include relevant log output
5. **Try with both JNI and Panama runtimes** to isolate the issue
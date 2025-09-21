# Quick Start Guide

Get up and running with Wasmtime4j in minutes. This guide covers project setup, basic usage, and common patterns.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Project Setup](#project-setup)
- [Hello World Example](#hello-world-example)
- [IDE Setup](#ide-setup)
- [Common Patterns](#common-patterns)
- [Testing WebAssembly Modules](#testing-webassembly-modules)
- [Debugging Tips](#debugging-tips)
- [Next Steps](#next-steps)

## Prerequisites

### Java Requirements

- **For JNI**: Java 8 or higher
- **For Panama**: Java 23 or higher (recommended for new projects)

```bash
# Check your Java version
java -version

# Verify Panama FFI support (Java 23+)
java --list-modules | grep jdk.incubator.foreign
```

### Build Tools

- **Maven 3.6+** or **Gradle 7.0+**
- **Docker** (optional, for containerized development)

## Project Setup

### Maven Project

Create a new Maven project or add to existing `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>wasmtime4j-demo</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>23</maven.compiler.source>
        <maven.compiler.target>23</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <wasmtime4j.version>1.0.0</wasmtime4j.version>
    </properties>

    <dependencies>
        <!-- Core Wasmtime4j API -->
        <dependency>
            <groupId>ai.tegmentum</groupId>
            <artifactId>wasmtime4j</artifactId>
            <version>${wasmtime4j.version}</version>
        </dependency>

        <!-- Runtime implementations -->
        <dependency>
            <groupId>ai.tegmentum</groupId>
            <artifactId>wasmtime4j-jni</artifactId>
            <version>${wasmtime4j.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>ai.tegmentum</groupId>
            <artifactId>wasmtime4j-panama</artifactId>
            <version>${wasmtime4j.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Native library loader -->
        <dependency>
            <groupId>ai.tegmentum</groupId>
            <artifactId>wasmtime4j-native-loader</artifactId>
            <version>${wasmtime4j.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>23</source>
                    <target>23</target>
                    <!-- Enable preview features for Panama FFI -->
                    <compilerArgs>
                        <arg>--enable-preview</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
                <configuration>
                    <argLine>--enable-preview --add-modules jdk.incubator.foreign</argLine>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Gradle Project

Add to `build.gradle`:

```gradle
plugins {
    id 'java'
    id 'application'
}

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

repositories {
    mavenCentral()
    // Add if using snapshot versions
    // maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
}

ext {
    wasmtime4jVersion = '1.0.0'
}

dependencies {
    implementation "ai.tegmentum:wasmtime4j:${wasmtime4jVersion}"

    runtimeOnly "ai.tegmentum:wasmtime4j-jni:${wasmtime4jVersion}"
    runtimeOnly "ai.tegmentum:wasmtime4j-panama:${wasmtime4jVersion}"
    runtimeOnly "ai.tegmentum:wasmtime4j-native-loader:${wasmtime4jVersion}"

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}

tasks.withType(JavaCompile) {
    options.compilerArgs += ['--enable-preview']
}

tasks.withType(Test) {
    jvmArgs '--enable-preview', '--add-modules', 'jdk.incubator.foreign'
    useJUnitPlatform()
}

application {
    mainClass = 'com.example.WasmDemo'
    applicationDefaultJvmArgs = ['--enable-preview', '--add-modules', 'jdk.incubator.foreign']
}
```

## Hello World Example

### Simple Calculator WASM

First, create a simple WebAssembly module. Save this as `calculator.wat`:

```wat
(module
  (func $add (param $lhs i32) (param $rhs i32) (result i32)
    local.get $lhs
    local.get $rhs
    i32.add)
  (export "add" (func $add))

  (func $subtract (param $lhs i32) (param $rhs i32) (result i32)
    local.get $lhs
    local.get $rhs
    i32.sub)
  (export "subtract" (func $subtract))

  (func $multiply (param $lhs i32) (param $rhs i32) (result i32)
    local.get $lhs
    local.get $rhs
    i32.mul)
  (export "multiply" (func $multiply))
)
```

Compile to binary format:

```bash
# Install wabt (WebAssembly Binary Toolkit)
# On macOS: brew install wabt
# On Ubuntu: apt-get install wabt

wat2wasm calculator.wat -o calculator.wasm
```

### Java Application

Create `src/main/java/com/example/WasmDemo.java`:

```java
package com.example;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.exception.WasmException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class WasmDemo {

    public static void main(String[] args) {
        try {
            // Load WebAssembly module
            byte[] wasmBytes = Files.readAllBytes(Paths.get("calculator.wasm"));

            // Create runtime with automatic selection
            try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
                System.out.println("Using runtime: " + runtime.getRuntimeType());

                // Create engine and compile module
                try (Engine engine = runtime.createEngine()) {
                    Module module = engine.compile(wasmBytes);

                    // Create store and instantiate module
                    Store store = engine.createStore();
                    ImportMap imports = new ImportMap(); // No imports needed
                    Instance instance = module.instantiate(store, imports);

                    // Get exported functions
                    WasmFunction addFunction = instance.getFunction("add");
                    WasmFunction subtractFunction = instance.getFunction("subtract");
                    WasmFunction multiplyFunction = instance.getFunction("multiply");

                    // Call functions
                    WasmValue[] addResult = addFunction.call(
                        WasmValue.i32(10),
                        WasmValue.i32(20)
                    );
                    System.out.println("10 + 20 = " + addResult[0].asInt());

                    WasmValue[] subtractResult = subtractFunction.call(
                        WasmValue.i32(30),
                        WasmValue.i32(15)
                    );
                    System.out.println("30 - 15 = " + subtractResult[0].asInt());

                    WasmValue[] multiplyResult = multiplyFunction.call(
                        WasmValue.i32(6),
                        WasmValue.i32(7)
                    );
                    System.out.println("6 * 7 = " + multiplyResult[0].asInt());
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading WASM file: " + e.getMessage());
        } catch (WasmException e) {
            System.err.println("WebAssembly error: " + e.getMessage());
        }
    }
}
```

### Run the Example

```bash
# Compile and run
mvn compile exec:java -Dexec.mainClass="com.example.WasmDemo"

# Or with Gradle
./gradlew run

# Expected output:
# Using runtime: PANAMA
# 10 + 20 = 30
# 30 - 15 = 15
# 6 * 7 = 42
```

## IDE Setup

### IntelliJ IDEA

1. **Install Wasmtime4j Plugin** (if available):
   - Go to `File → Settings → Plugins`
   - Search for "Wasmtime4j" and install

2. **Configure Project Structure**:
   - `File → Project Structure → Modules`
   - Add module dependencies for runtime implementations

3. **Enable Preview Features**:
   - `File → Settings → Build, Execution, Deployment → Compiler → Java Compiler`
   - Add `--enable-preview` to "Additional command line parameters"

4. **Run Configuration**:
   ```
   VM Options: --enable-preview --add-modules jdk.incubator.foreign
   Program Arguments: (your application arguments)
   Environment Variables: WASMTIME4J_RUNTIME=panama (optional)
   ```

### Eclipse

1. **Project Properties**:
   - Right-click project → Properties
   - Java Build Path → Modulepath → Add External JARs
   - Add Wasmtime4j JAR files

2. **Compiler Settings**:
   - Properties → Java Compiler
   - Enable preview features

3. **Run Configuration**:
   - Run → Run Configurations → Java Application
   - Arguments tab → VM arguments: `--enable-preview --add-modules jdk.incubator.foreign`

### Visual Studio Code

1. **Install Extensions**:
   - Extension Pack for Java
   - WebAssembly (for .wasm/.wat files)

2. **Configure settings.json**:
   ```json
   {
       "java.compile.nullAnalysis.mode": "automatic",
       "java.configuration.runtimes": [
           {
               "name": "JavaSE-23",
               "path": "/path/to/java23"
           }
       ],
       "java.jdt.ls.vmargs": "--enable-preview --add-modules jdk.incubator.foreign"
   }
   ```

## Common Patterns

### Resource Management Pattern

```java
public class WasmService implements AutoCloseable {
    private final WasmRuntime runtime;
    private final Engine engine;

    public WasmService() throws WasmException {
        this.runtime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
        this.engine = runtime.createEngine(
            EngineConfig.builder()
                .optimizationLevel(OptimizationLevel.SPEED)
                .build()
        );
    }

    public String processModule(byte[] wasmBytes, String input) throws WasmException {
        Module module = engine.compile(wasmBytes);
        Store store = engine.createStore();

        try (module; store) {
            Instance instance = module.instantiate(store, new ImportMap());
            WasmFunction processFunction = instance.getFunction("process");

            // Call function with input
            WasmValue[] result = processFunction.call(WasmValue.i32(input.length()));
            return String.valueOf(result[0].asInt());
        }
    }

    @Override
    public void close() throws WasmException {
        if (engine != null) engine.close();
        if (runtime != null) runtime.close();
    }
}
```

### Configuration Builder Pattern

```java
public class WasmConfigBuilder {

    public static EngineConfig createProductionConfig() {
        return EngineConfig.builder()
            .optimizationLevel(OptimizationLevel.SPEED)
            .debugInfo(false)
            .fuel(1_000_000L)
            .epochInterruption(true)
            .maxMemory(512 * 1024 * 1024L) // 512MB
            .cache(CacheConfig.builder()
                .directory("./wasm-cache")
                .maxSize(100 * 1024 * 1024L) // 100MB
                .cleanupPolicy(CacheCleanupPolicy.LRU)
                .build())
            .build();
    }

    public static EngineConfig createDevelopmentConfig() {
        return EngineConfig.builder()
            .optimizationLevel(OptimizationLevel.NONE)
            .debugInfo(true)
            .build();
    }
}
```

### Error Handling Pattern

```java
public class SafeWasmExecutor {

    public static <T> T executeWithTimeout(
            Duration timeout,
            WasmOperation<T> operation) throws WasmException {

        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                return operation.execute();
            } catch (WasmException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new WasmException("Operation timed out after " + timeout);
        } catch (Exception e) {
            throw new WasmException("Operation failed", e);
        }
    }

    @FunctionalInterface
    public interface WasmOperation<T> {
        T execute() throws WasmException;
    }
}
```

### Memory Management Pattern

```java
public class WasmMemoryManager {

    public static void writeString(WasmMemory memory, int offset, String value)
            throws WasmException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        memory.write(offset, bytes);
    }

    public static String readString(WasmMemory memory, int offset, int length)
            throws WasmException {
        byte[] bytes = memory.read(offset, length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void validateMemoryAccess(WasmMemory memory, int offset, int length)
            throws WasmException {
        if (offset < 0 || length < 0) {
            throw new WasmException("Invalid memory access: negative offset or length");
        }

        long memorySize = memory.getSizeBytes();
        if (offset + length > memorySize) {
            throw new WasmException(
                String.format("Memory access out of bounds: %d + %d > %d",
                    offset, length, memorySize)
            );
        }
    }
}
```

## Testing WebAssembly Modules

### Unit Testing

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class WasmCalculatorTest {

    private WasmRuntime runtime;
    private Engine engine;
    private Store store;
    private Instance instance;

    @BeforeEach
    void setUp() throws Exception {
        runtime = WasmRuntimeFactory.create();
        engine = runtime.createEngine();

        byte[] wasmBytes = getClass().getResourceAsStream("/calculator.wasm").readAllBytes();
        Module module = engine.compile(wasmBytes);

        store = engine.createStore();
        instance = module.instantiate(store, new ImportMap());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (store != null) store.close();
        if (engine != null) engine.close();
        if (runtime != null) runtime.close();
    }

    @Test
    void testAddition() throws WasmException {
        WasmFunction addFunction = instance.getFunction("add");
        WasmValue[] result = addFunction.call(
            WasmValue.i32(15),
            WasmValue.i32(25)
        );

        assertEquals(40, result[0].asInt());
    }

    @Test
    void testDivisionByZero() throws WasmException {
        WasmFunction divideFunction = instance.getFunction("divide");

        assertThrows(RuntimeException.class, () -> {
            divideFunction.call(WasmValue.i32(10), WasmValue.i32(0));
        });
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1, 2",
        "5, 3, 8",
        "-1, 1, 0",
        "100, 200, 300"
    })
    void testAdditionParameterized(int a, int b, int expected) throws WasmException {
        WasmFunction addFunction = instance.getFunction("add");
        WasmValue[] result = addFunction.call(
            WasmValue.i32(a),
            WasmValue.i32(b)
        );

        assertEquals(expected, result[0].asInt());
    }
}
```

### Integration Testing

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WasmIntegrationTest {

    private WasmService wasmService;

    @BeforeAll
    void setUpService() throws WasmException {
        wasmService = new WasmService();
    }

    @AfterAll
    void tearDownService() throws WasmException {
        wasmService.close();
    }

    @Test
    void testCompleteWorkflow() throws Exception {
        // Load test module
        byte[] wasmBytes = loadTestModule("complex_calculator.wasm");

        // Process with service
        String result = wasmService.processModule(wasmBytes, "test input");

        // Verify result
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    private byte[] loadTestModule(String filename) throws IOException {
        return getClass().getResourceAsStream("/test-modules/" + filename).readAllBytes();
    }
}
```

## Debugging Tips

### Enable Debug Logging

```java
// Add to your main method or test setup
System.setProperty("wasmtime4j.debug", "true");
System.setProperty("java.util.logging.config.file", "logging.properties");
```

Create `logging.properties`:

```properties
# Global logging level
.level = INFO

# Wasmtime4j specific logging
ai.tegmentum.wasmtime4j.level = FINE
ai.tegmentum.wasmtime4j.jni.level = FINE
ai.tegmentum.wasmtime4j.panama.level = FINE

# Console handler
handlers = java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.level = FINE
java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
java.util.logging.SimpleFormatter.format = %1$tF %1$tT %4$s %2$s %5$s%6$s%n
```

### Debugging WebAssembly Modules

```java
public class WasmDebugger {

    public static void inspectModule(Module module) {
        System.out.println("=== Module Inspection ===");

        System.out.println("Imports:");
        for (Import imp : module.getImports()) {
            System.out.printf("  %s.%s (%s)%n",
                imp.getModule(), imp.getName(), imp.getType());
        }

        System.out.println("Exports:");
        for (Export exp : module.getExports()) {
            System.out.printf("  %s (%s)%n", exp.getName(), exp.getType());
        }
    }

    public static void inspectInstance(Instance instance) {
        System.out.println("=== Instance Inspection ===");

        System.out.println("Functions:");
        instance.getFunctions().forEach((name, func) -> {
            System.out.printf("  %s: %s%n", name, func.getSignature());
        });

        System.out.println("Memories:");
        instance.getMemories().forEach((name, memory) -> {
            System.out.printf("  %s: %d pages (%d bytes)%n",
                name, memory.getSize(), memory.getSizeBytes());
        });
    }

    public static void traceExecution(WasmFunction function, WasmValue... args)
            throws WasmException {
        System.out.printf("Calling %s with args: %s%n",
            function.getName(),
            Arrays.toString(args));

        long startTime = System.nanoTime();
        WasmValue[] result = function.call(args);
        long endTime = System.nanoTime();

        System.out.printf("Result: %s (took %d μs)%n",
            Arrays.toString(result),
            (endTime - startTime) / 1000);
    }
}
```

### Performance Profiling

```java
public class WasmProfiler {

    public static void profileFunction(WasmFunction function,
                                     WasmValue[] args,
                                     int iterations) throws WasmException {

        // Warmup
        for (int i = 0; i < 100; i++) {
            function.call(args);
        }

        // Measure
        long totalTime = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            function.call(args);
            long end = System.nanoTime();
            totalTime += (end - start);
        }

        double avgTimeMs = (totalTime / iterations) / 1_000_000.0;
        System.out.printf("Average execution time: %.3f ms%n", avgTimeMs);
        System.out.printf("Throughput: %.0f calls/second%n",
            1000.0 / avgTimeMs);
    }
}
```

## Next Steps

### Explore Advanced Features

1. **[WASI Integration](wasi-integration.md)** - File system and system interface support
2. **[Host Functions](host-functions.md)** - Implementing custom functions callable from WASM
3. **[Memory Management](memory-management.md)** - Advanced memory operations and optimization
4. **[Performance Tuning](../guides/performance.md)** - Optimization strategies and benchmarking

### Sample Projects

1. **[Image Processing Service](../examples/image-processor/)** - WASM-based image filters
2. **[Serverless Functions](../examples/serverless/)** - AWS Lambda with WebAssembly
3. **[Microservice Template](../examples/microservice/)** - Spring Boot integration
4. **[Plugin System](../examples/plugin-system/)** - Dynamic WASM module loading

### Community and Support

- **Documentation**: [Full API Reference](../api/wasmtime4j-api-reference.md)
- **Examples**: [GitHub Repository](https://github.com/tegmentum-ai/wasmtime4j/tree/main/examples)
- **Issues**: [GitHub Issues](https://github.com/tegmentum-ai/wasmtime4j/issues)
- **Discussions**: [GitHub Discussions](https://github.com/tegmentum-ai/wasmtime4j/discussions)

Happy coding with Wasmtime4j! 🚀
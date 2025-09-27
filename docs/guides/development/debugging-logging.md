# Debugging and Logging Guide

This guide provides comprehensive debugging and logging strategies for wasmtime4j applications.

## Quick Start

### Enable Debug Logging

```java
// System property
System.setProperty("wasmtime4j.debug", "true");

// Or via JVM arguments
-Dwasmtime4j.debug=true
```

### Enable Detailed Java Logging

```java
// Java Util Logging
System.setProperty("java.util.logging.level", "FINE");

// Or create logging.properties
handlers=java.util.logging.ConsoleHandler
.level=FINE
ai.tegmentum.wasmtime4j.level=FINE
```

## Logging Configuration

### Java Util Logging (Default)

Wasmtime4j uses `java.util.logging` by default for minimal dependencies.

#### Basic Configuration

Create `logging.properties`:

```properties
# Root logger level
.level=INFO

# Console handler
handlers=java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.level=ALL
java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter

# Wasmtime4j specific logging
ai.tegmentum.wasmtime4j.level=INFO
ai.tegmentum.wasmtime4j.jni.level=INFO
ai.tegmentum.wasmtime4j.panama.level=INFO
ai.tegmentum.wasmtime4j.internal.level=FINE

# Native library loading
ai.tegmentum.wasmtime4j.internal.NativeLibraryLoader.level=FINE

# Performance monitoring
ai.tegmentum.wasmtime4j.monitoring.level=INFO
```

#### Apply Configuration

```bash
# Via system property
-Djava.util.logging.config.file=logging.properties

# Or programmatically
LogManager.getLogManager().readConfiguration(
    new FileInputStream("logging.properties"));
```

### SLF4J Integration

For applications using SLF4J, bridge JUL to SLF4J:

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>jul-to-slf4j</artifactId>
    <version>2.0.9</version>
</dependency>
```

```java
// Initialize bridge
SLF4JBridgeHandler.removeHandlersForRootLogger();
SLF4JBridgeHandler.install();
```

### Logback Configuration

For Spring Boot or Logback users:

```xml
<!-- logback-spring.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Console appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <!-- File appender for wasmtime4j -->
    <appender name="WASMTIME_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/wasmtime4j.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/wasmtime4j.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- Wasmtime4j loggers -->
    <logger name="ai.tegmentum.wasmtime4j" level="INFO" additivity="false">
        <appender-ref ref="WASMTIME_FILE"/>
        <appender-ref ref="CONSOLE"/>
    </logger>

    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

    <!-- Profile-specific configuration -->
    <springProfile name="dev,development">
        <logger name="ai.tegmentum.wasmtime4j" level="DEBUG"/>
        <logger name="ai.tegmentum.wasmtime4j.internal" level="TRACE"/>
    </springProfile>

    <springProfile name="prod,production">
        <logger name="ai.tegmentum.wasmtime4j" level="INFO"/>
        <logger name="ai.tegmentum.wasmtime4j.internal" level="WARN"/>
    </springProfile>
</configuration>
```

## Debugging Strategies

### 1. Native Library Loading Issues

#### Enable Native Loading Debug

```java
System.setProperty("wasmtime4j.debug", "true");
System.setProperty("wasmtime4j.native.debug", "true");
```

#### Common Issues and Solutions

**Issue**: `UnsatisfiedLinkError` - Library not found
```
Solution: Check library extraction and system library path
```

**Debug Steps**:
1. Enable debug logging
2. Check extraction directory
3. Verify architecture compatibility
4. Test library dependencies

```java
// Check native library status
public class NativeDebugInfo {
    public static void main(String[] args) {
        System.setProperty("wasmtime4j.debug", "true");

        // Try to initialize engine
        try (Engine engine = Engine.newBuilder().build()) {
            System.out.println("Native library loaded successfully");
            System.out.println("Runtime: " + engine.getClass().getSimpleName());
        } catch (Exception e) {
            System.err.println("Failed to load native library: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### 2. WebAssembly Module Issues

#### Module Compilation Debugging

```java
public class ModuleDebugger {
    private static final Logger logger = Logger.getLogger(ModuleDebugger.class.getName());

    public static void debugModule(Engine engine, byte[] wasmBytes) {
        logger.info("Attempting to compile module, size: " + wasmBytes.length + " bytes");

        try {
            // Enable detailed validation
            Module module = Module.fromBinary(engine, wasmBytes);
            logger.info("Module compiled successfully");

            // Analyze module
            analyzeModule(module);

            module.close();
        } catch (Exception e) {
            logger.severe("Module compilation failed: " + e.getMessage());
            analyzeCompilationError(e, wasmBytes);
        }
    }

    private static void analyzeModule(Module module) {
        // Implementation would analyze module exports, imports, etc.
        logger.info("Module analysis completed");
    }

    private static void analyzeCompilationError(Exception e, byte[] wasmBytes) {
        // Analyze error and provide suggestions
        if (wasmBytes.length < 8) {
            logger.warning("WASM bytes too short, minimum 8 bytes required");
        }

        // Check magic number
        if (wasmBytes.length >= 4) {
            int magic = (wasmBytes[3] & 0xFF) << 24 |
                       (wasmBytes[2] & 0xFF) << 16 |
                       (wasmBytes[1] & 0xFF) << 8 |
                       (wasmBytes[0] & 0xFF);
            if (magic != 0x6d736100) { // "\0asm"
                logger.warning("Invalid WASM magic number: 0x" + Integer.toHexString(magic));
            }
        }
    }
}
```

### 3. Function Call Debugging

#### Trace Function Calls

```java
public class FunctionCallTracer {
    private static final Logger logger = Logger.getLogger(FunctionCallTracer.class.getName());

    public static Object[] traceCall(Function function, String functionName, Object... args) {
        logger.info("Calling function: " + functionName + " with args: " + Arrays.toString(args));

        long startTime = System.nanoTime();
        try {
            Object[] results = function.call(args);
            long endTime = System.nanoTime();

            logger.info("Function " + functionName + " completed in " +
                       (endTime - startTime) / 1_000_000.0 + " ms, results: " +
                       Arrays.toString(results));

            return results;
        } catch (Exception e) {
            long endTime = System.nanoTime();
            logger.severe("Function " + functionName + " failed after " +
                         (endTime - startTime) / 1_000_000.0 + " ms: " + e.getMessage());
            throw e;
        }
    }
}
```

### 4. Memory Debugging

#### Monitor Memory Usage

```java
public class MemoryMonitor {
    private static final Logger logger = Logger.getLogger(MemoryMonitor.class.getName());

    public static void monitorMemory(Memory memory, String context) {
        ByteBuffer buffer = memory.buffer();

        logger.info(String.format("Memory state [%s]: capacity=%d, position=%d, limit=%d",
                                 context, buffer.capacity(), buffer.position(), buffer.limit()));

        // Check for common memory issues
        if (buffer.position() == buffer.limit()) {
            logger.warning("Memory buffer is at limit, no more data can be written");
        }

        if (buffer.remaining() < 1024) {
            logger.warning("Memory buffer has less than 1KB remaining");
        }
    }

    public static void dumpMemory(Memory memory, int offset, int length) {
        ByteBuffer buffer = memory.buffer();
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Memory dump at offset %d, length %d:%n", offset, length));

        for (int i = 0; i < length && offset + i < buffer.capacity(); i++) {
            if (i % 16 == 0) {
                sb.append(String.format("%08x: ", offset + i));
            }

            byte b = buffer.get(offset + i);
            sb.append(String.format("%02x ", b & 0xFF));

            if ((i + 1) % 16 == 0 || i == length - 1) {
                sb.append("%n");
            }
        }

        logger.info(sb.toString());
    }
}
```

## Performance Debugging

### 1. Profiling Integration

#### JMH Benchmarking

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class WasmPerformanceBenchmark {

    private Engine engine;
    private Store store;
    private Module module;
    private Instance instance;
    private Function addFunction;

    @Setup
    public void setup() throws IOException {
        engine = Engine.newBuilder().build();
        store = Store.newBuilder(engine).build();

        byte[] wasmBytes = loadWasmFile("math.wasm");
        module = Module.fromBinary(engine, wasmBytes);
        instance = Instance.newBuilder(store, module).build();
        addFunction = instance.getFunction("add");
    }

    @TearDown
    public void tearDown() {
        if (instance != null) instance.close();
        if (module != null) module.close();
        if (store != null) store.close();
        if (engine != null) engine.close();
    }

    @Benchmark
    public Object[] benchmarkFunctionCall() {
        return addFunction.call(42, 13);
    }

    private byte[] loadWasmFile(String filename) throws IOException {
        // Load WASM file implementation
        return new byte[0];
    }
}
```

#### JProfiler Integration

Enable JProfiler for detailed profiling:

```bash
# JVM arguments for JProfiler
-agentpath:/path/to/jprofiler/bin/linux-x64/libjprofilerti.so=port=8849
-Djprofiler.agentpath=/path/to/jprofiler/bin/linux-x64/libjprofilerti.so
```

### 2. Custom Performance Monitoring

```java
public class PerformanceMonitor {
    private static final Logger logger = Logger.getLogger(PerformanceMonitor.class.getName());
    private static final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> timings = new ConcurrentHashMap<>();

    public static void recordOperation(String operation, long durationNanos) {
        counters.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
        timings.computeIfAbsent(operation + ".total", k -> new AtomicLong(0)).addAndGet(durationNanos);
    }

    public static void logStatistics() {
        logger.info("=== Performance Statistics ===");

        for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
            String operation = entry.getKey();
            long count = entry.getValue().get();
            long totalTime = timings.getOrDefault(operation + ".total", new AtomicLong(0)).get();

            double avgTime = count > 0 ? (totalTime / (double) count) / 1_000_000.0 : 0;

            logger.info(String.format("%s: count=%d, avg=%.3f ms, total=%.3f ms",
                                     operation, count, avgTime, totalTime / 1_000_000.0));
        }
    }

    public static <T> T timeOperation(String operation, Supplier<T> supplier) {
        long startTime = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            long endTime = System.nanoTime();
            recordOperation(operation, endTime - startTime);
        }
    }
}
```

## Error Handling and Diagnostics

### 1. Comprehensive Error Analysis

```java
public class ErrorAnalyzer {
    private static final Logger logger = Logger.getLogger(ErrorAnalyzer.class.getName());

    public static void analyzeError(Throwable error, String context) {
        logger.severe("Error in " + context + ": " + error.getMessage());

        // Analyze specific error types
        if (error instanceof UnsatisfiedLinkError) {
            analyzeNativeLibraryError((UnsatisfiedLinkError) error);
        } else if (error.getMessage() != null) {
            analyzeWasmError(error);
        }

        // Log stack trace with context
        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        logger.severe("Stack trace:\n" + sw.toString());

        // Suggest solutions
        suggestSolutions(error);
    }

    private static void analyzeNativeLibraryError(UnsatisfiedLinkError error) {
        String message = error.getMessage();

        if (message.contains("no") && message.contains("in java.library.path")) {
            logger.severe("Native library not found in java.library.path");
            logger.info("Current java.library.path: " + System.getProperty("java.library.path"));
            logger.info("Try setting -Djava.library.path=/path/to/natives");
        } else if (message.contains("cannot open shared object file")) {
            logger.severe("Native library dependencies missing");
            logger.info("Install required system libraries (libc, libm, etc.)");
        }
    }

    private static void analyzeWasmError(Throwable error) {
        String message = error.getMessage();

        if (message.contains("validation")) {
            logger.info("WebAssembly validation failed - check module format");
        } else if (message.contains("instantiation")) {
            logger.info("WebAssembly instantiation failed - check imports/exports");
        } else if (message.contains("trap")) {
            logger.info("WebAssembly runtime trap - check function logic");
        }
    }

    private static void suggestSolutions(Throwable error) {
        // Provide specific suggestions based on error patterns
        logger.info("=== Suggested Solutions ===");
        logger.info("1. Enable debug logging: -Dwasmtime4j.debug=true");
        logger.info("2. Check system requirements and native library compatibility");
        logger.info("3. Verify WebAssembly module validity with external tools");
        logger.info("4. Review documentation: https://docs.wasmtime4j.io/troubleshooting");
    }
}
```

### 2. Health Monitoring

```java
@Component
public class HealthMonitor {
    private static final Logger logger = Logger.getLogger(HealthMonitor.class.getName());
    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private final AtomicReference<String> lastError = new AtomicReference<>("");

    public boolean isHealthy() {
        return healthy.get();
    }

    public String getLastError() {
        return lastError.get();
    }

    public void recordSuccess() {
        healthy.set(true);
        lastError.set("");
    }

    public void recordError(String error) {
        healthy.set(false);
        lastError.set(error);
        logger.warning("Health check failed: " + error);
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void performHealthCheck() {
        try {
            // Test basic wasmtime4j functionality
            testBasicFunctionality();
            recordSuccess();
        } catch (Exception e) {
            recordError(e.getMessage());
        }
    }

    private void testBasicFunctionality() {
        try (Engine engine = Engine.newBuilder().build();
             Store store = Store.newBuilder(engine).build()) {

            // Simple health check - create engine and store
            logger.fine("Health check passed");
        }
    }
}
```

## IDE Integration

### IntelliJ IDEA Debugging

#### Remote Debugging Configuration

1. **Run → Edit Configurations → Remote JVM Debug**
2. **Configure**:
   - **Port**: 5005
   - **Command line arguments**:
     ```
     -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
     ```

#### Logging Configuration

Create run configuration with:
```
VM Options: -Dwasmtime4j.debug=true -Djava.util.logging.level=FINE
```

### Eclipse Debugging

#### Debug Configuration

1. **Run → Debug Configurations → Java Application**
2. **Arguments tab**:
   ```
   VM arguments: -Dwasmtime4j.debug=true -Djava.util.logging.level=FINE
   ```

#### CDT Integration for Native Debugging

1. Install Eclipse CDT
2. Create C++ Debug Configuration
3. Attach to Java process for mixed-mode debugging

## Production Monitoring

### 1. Metrics Collection

```java
@Component
public class WasmtimeMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter engineCreations;
    private final Counter moduleCompilations;
    private final Timer functionCallTimer;
    private final Gauge activeEngines;

    public WasmtimeMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.engineCreations = Counter.builder("wasmtime.engine.creations")
                .description("Number of Wasmtime engines created")
                .register(meterRegistry);
        this.moduleCompilations = Counter.builder("wasmtime.module.compilations")
                .description("Number of WASM modules compiled")
                .register(meterRegistry);
        this.functionCallTimer = Timer.builder("wasmtime.function.calls")
                .description("Time spent calling WASM functions")
                .register(meterRegistry);
        this.activeEngines = Gauge.builder("wasmtime.engine.active")
                .description("Number of active Wasmtime engines")
                .register(meterRegistry, this, WasmtimeMetrics::getActiveEngineCount);
    }

    public void recordEngineCreation() {
        engineCreations.increment();
    }

    public void recordModuleCompilation() {
        moduleCompilations.increment();
    }

    public Timer.Sample startFunctionCall() {
        return Timer.start(meterRegistry);
    }

    public void recordFunctionCall(Timer.Sample sample) {
        sample.stop(functionCallTimer);
    }

    private int getActiveEngineCount() {
        // Implementation to count active engines
        return 0;
    }
}
```

### 2. Structured Logging

```java
// Structured logging example
public class StructuredLogger {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = Logger.getLogger(StructuredLogger.class.getName());

    public static void logOperation(String operation, Map<String, Object> context, Throwable error) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", Instant.now().toString());
        logEntry.put("operation", operation);
        logEntry.put("context", context);

        if (error != null) {
            logEntry.put("error", error.getMessage());
            logEntry.put("stackTrace", getStackTrace(error));
        }

        try {
            String json = mapper.writeValueAsString(logEntry);
            if (error != null) {
                logger.severe(json);
            } else {
                logger.info(json);
            }
        } catch (Exception e) {
            logger.severe("Failed to log structured message: " + e.getMessage());
        }
    }

    private static String getStackTrace(Throwable error) {
        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
```

## Troubleshooting Common Issues

### Issue: Native Library Not Found

**Symptoms**:
```
java.lang.UnsatisfiedLinkError: no wasmtime4j_native in java.library.path
```

**Debug Steps**:
1. Enable debug logging: `-Dwasmtime4j.debug=true`
2. Check library extraction
3. Verify system architecture compatibility
4. Test with explicit library path

**Solutions**:
```bash
# Set library path explicitly
-Djava.library.path=/path/to/natives

# Check extraction directory
ls -la target/natives/

# Verify architecture
file target/natives/libwasmtime4j_native.so
```

### Issue: WASM Module Compilation Fails

**Symptoms**:
```
WebAssembly module validation failed
```

**Debug Steps**:
1. Verify WASM file integrity
2. Check module format and version
3. Validate with external tools
4. Test with minimal module

**Solutions**:
```bash
# Validate with wasmtime CLI
wasmtime validate module.wasm

# Check module info
wasm-objdump -h module.wasm

# Test with simple module
wabt2wasm simple.wat -o simple.wasm
```

### Issue: Poor Performance

**Debug Steps**:
1. Enable performance monitoring
2. Profile with JProfiler/VisualVM
3. Analyze GC behavior
4. Monitor native calls overhead

**Solutions**:
- Optimize JVM settings
- Reduce native call frequency
- Implement caching strategies
- Use connection pooling

## Additional Resources

- [Java Logging Documentation](https://docs.oracle.com/javase/8/docs/api/java/util/logging/package-summary.html)
- [SLF4J Manual](http://www.slf4j.org/manual.html)
- [Logback Configuration](http://logback.qos.ch/manual/configuration.html)
- [JMH Documentation](https://openjdk.java.net/projects/code-tools/jmh/)
- [JProfiler Documentation](https://www.ej-technologies.com/products/jprofiler/overview.html)
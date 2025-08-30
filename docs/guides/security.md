# Security Guide

This guide covers security considerations when using Wasmtime4j in production environments, including sandboxing, resource limits, input validation, and secure deployment practices.

## Table of Contents
- [Security Model Overview](#security-model-overview)
- [WebAssembly Sandboxing](#webassembly-sandboxing)
- [WASI Security](#wasi-security)
- [Resource Limits](#resource-limits)
- [Input Validation](#input-validation)
- [Host Function Security](#host-function-security)
- [Memory Safety](#memory-safety)
- [Deployment Security](#deployment-security)
- [Monitoring and Auditing](#monitoring-and-auditing)
- [Security Best Practices](#security-best-practices)

## Security Model Overview

Wasmtime4j provides multiple layers of security:

1. **WebAssembly Sandboxing**: Execution isolation and memory safety
2. **WASI Security**: Controlled system interface access
3. **Resource Limits**: Prevention of resource exhaustion attacks
4. **Input Validation**: Protection against malicious inputs
5. **Host Function Security**: Safe bridging between WebAssembly and Java

### Security Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                 Java Application                            │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │               Wasmtime4j API                            │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │ │
│  │  │ Input       │  │ Resource    │  │ Host Function   │ │ │
│  │  │ Validation  │  │ Limits      │  │ Security        │ │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘     │ │
│  └─────────────────┬───────────────────────────────────────┘ │
└────────────────────┼─────────────────────────────────────────┘
                     │ Controlled Access
┌────────────────────▼─────────────────────────────────────────┐
│                WebAssembly Sandbox                          │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                WASI Interface                           │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │ │
│  │  │ File System │  │ Network     │  │ Environment     │ │ │
│  │  │ Sandbox     │  │ Isolation   │  │ Isolation       │ │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘     │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Memory Isolation                           │ │
│  │  - Linear memory bounds checking                       │ │
│  │  - Stack overflow protection                           │ │
│  │  - Heap isolation                                      │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## WebAssembly Sandboxing

WebAssembly provides fundamental security through its sandboxed execution model:

### Memory Isolation

```java
// WebAssembly memory is isolated from the host
WasmMemory memory = instance.getMemory("memory");

// Bounds checking is automatic
try {
    byte[] data = memory.read(offset, length);
} catch (RuntimeException e) {
    // Out-of-bounds access is safely trapped
    log.warn("Memory access violation: " + e.getMessage());
}

// Memory cannot escape the WebAssembly linear memory space
// No arbitrary memory access to host process memory
```

### Control Flow Integrity

```java
// WebAssembly enforces control flow integrity
// - No arbitrary jumps outside module boundaries
// - Type-safe function calls only
// - Stack overflow protection

WasmFunction function = instance.getFunction("safe_function");

// This call is guaranteed to:
// 1. Only execute within the WebAssembly module
// 2. Respect function signatures  
// 3. Not corrupt the call stack
WasmValue[] results = function.call(args);
```

### Deterministic Execution

```java
// WebAssembly execution is deterministic and isolated
// - No access to system clock (unless via WASI)
// - No access to random number generation (unless via WASI)  
// - No access to environment variables (unless via WASI)

public class DeterministicExecution {
    public void runSecureModule() throws WasmException {
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            Engine engine = runtime.createEngine();
            Module module = loadTrustedModule();
            
            // Create instance without WASI - completely sandboxed
            Instance instance = runtime.instantiate(module);
            
            // Execution is deterministic and isolated
            WasmFunction compute = instance.getFunction("compute");
            WasmValue[] results = compute.call(WasmValue.i32(42));
            
            // Result will always be the same for same input
            assert results[0].asI32() == expectedResult;
        }
    }
}
```

## WASI Security

WASI (WebAssembly System Interface) provides controlled access to system resources:

### Minimal Privilege Configuration

```java
public WasiContext createSecureWasiContext() {
    return WasiContextBuilder.create()
        // DO NOT use inheritStdio() in production with untrusted code
        .stdout(createFilteredOutputStream())
        .stderr(createFilteredOutputStream())
        
        // Grant minimal required filesystem access
        .preopenDirectory("/app/data", "/data", WasiDirectoryAccess.READ_ONLY)
        .preopenDirectory("/tmp/app-work", "/tmp", WasiDirectoryAccess.READ_WRITE)
        
        // Restrict environment variables
        .env("PATH", "/usr/bin")  // Only specific variables
        // DO NOT use inheritEnvironment()
        
        // Set resource limits
        .resourceLimits(WasiResourceLimits.builder()
            .maxMemory(64 * 1024 * 1024)      // 64MB max
            .maxOpenFiles(10)                   // Max 10 file descriptors
            .maxNetworkConnections(0)           // No network access
            .maxExecutionTime(Duration.ofSeconds(30))  // 30 second timeout
            .build())
        
        .build();
}
```

### Filesystem Sandboxing

```java
public class SecureFileAccess {
    
    public void setupSecureFileAccess() throws WasiException {
        // Create a secure sandbox directory
        Path sandboxDir = createTempDirectory("wasm-sandbox");
        
        // Set restrictive permissions
        sandboxDir.toFile().setReadable(true, true);   // Owner only
        sandboxDir.toFile().setWritable(true, true);   // Owner only  
        sandboxDir.toFile().setExecutable(false);      // No execution
        
        WasiContext context = WasiContextBuilder.create()
            // Map sandbox to WebAssembly filesystem root
            .preopenDirectory(sandboxDir.toString(), "/", WasiDirectoryAccess.READ_WRITE)
            
            // Explicitly deny access to sensitive directories
            .denyDirectory("/etc")
            .denyDirectory("/home")
            .denyDirectory("/root")
            .denyDirectory("/sys")
            .denyDirectory("/proc")
            
            .build();
    }
    
    private Path createTempDirectory(String prefix) throws IOException {
        Path tempDir = Files.createTempDirectory(prefix);
        
        // Set up automatic cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                deleteRecursively(tempDir);
            } catch (IOException e) {
                log.error("Failed to cleanup sandbox directory", e);
            }
        }));
        
        return tempDir;
    }
}
```

### Network Isolation

```java
public class NetworkSecurityConfig {
    
    public WasiContext createNetworkIsolatedContext() {
        return WasiContextBuilder.create()
            // Completely disable network access
            .networkAccess(WasiNetworkAccess.NONE)
            
            // Or allow only specific hosts/ports
            .allowedHosts("api.example.com:443", "localhost:8080")
            .allowedPorts(80, 443, 8080)
            
            // Restrict protocols
            .allowedProtocols(Protocol.HTTPS, Protocol.HTTP)
            
            .build();
    }
    
    public void validateNetworkAccess(String host, int port) {
        if (!isAllowedHost(host) || !isAllowedPort(port)) {
            throw new SecurityException("Network access denied: " + host + ":" + port);
        }
    }
}
```

## Resource Limits

Implement comprehensive resource limits to prevent DoS attacks:

### Memory Limits

```java
public class MemorySecurityConfig {
    
    public void configureMemoryLimits() throws WasmException {
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            
            // Configure engine with memory limits
            EngineConfig engineConfig = EngineConfig.builder()
                .maxMemory(128 * 1024 * 1024)  // 128MB max per instance
                .maxModules(100)               // Max 100 modules cached
                .maxInstances(50)              // Max 50 instances per engine
                .build();
                
            Engine engine = runtime.createEngine(engineConfig);
            
            // Configure WASI resource limits  
            WasiContext context = WasiContextBuilder.create()
                .resourceLimits(WasiResourceLimits.builder()
                    .maxMemory(64 * 1024 * 1024)       // 64MB WASI memory limit
                    .maxHeapGrowth(32 * 1024 * 1024)   // 32MB heap growth limit
                    .maxStackSize(1024 * 1024)         // 1MB stack limit
                    .build())
                .build();
        }
    }
    
    // Monitor memory usage in production
    public void monitorMemoryUsage(Instance instance) {
        WasmMemory memory = instance.getMemory("memory");
        
        long currentSize = memory.size() * 65536; // Pages to bytes
        long maxSize = 128 * 1024 * 1024;         // 128MB limit
        
        if (currentSize > maxSize * 0.9) {  // 90% threshold
            log.warn("WebAssembly memory usage high: {} MB", currentSize / (1024 * 1024));
            
            // Consider terminating or throttling
            if (currentSize > maxSize) {
                throw new SecurityException("Memory limit exceeded");
            }
        }
    }
}
```

### Execution Time Limits

```java
public class ExecutionTimeSecurityConfig {
    private final ScheduledExecutorService timeoutExecutor = 
        Executors.newSingleThreadScheduledExecutor();
    
    public WasmValue[] executeWithTimeout(WasmFunction function, WasmValue[] args, 
                                         Duration timeout) throws WasmException {
        
        CompletableFuture<WasmValue[]> execution = CompletableFuture.supplyAsync(() -> {
            try {
                return function.call(args);
            } catch (WasmException e) {
                throw new RuntimeException(e);
            }
        });
        
        try {
            return execution.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            execution.cancel(true);
            throw new SecurityException("Execution timeout exceeded: " + timeout);
        } catch (Exception e) {
            throw new WasmException("Execution failed", e);
        }
    }
    
    // Global execution monitoring
    public class ExecutionMonitor {
        private final Map<Instance, Instant> executionStart = new ConcurrentHashMap<>();
        private final Duration maxExecutionTime = Duration.ofSeconds(30);
        
        public void startExecution(Instance instance) {
            executionStart.put(instance, Instant.now());
            
            // Schedule termination check
            timeoutExecutor.schedule(() -> {
                Instant start = executionStart.get(instance);
                if (start != null && Duration.between(start, Instant.now()).compareTo(maxExecutionTime) > 0) {
                    log.error("Long-running WebAssembly execution detected, terminating");
                    terminateInstance(instance);
                }
            }, maxExecutionTime.toMillis(), TimeUnit.MILLISECONDS);
        }
        
        public void endExecution(Instance instance) {
            executionStart.remove(instance);
        }
    }
}
```

## Input Validation

Always validate inputs before passing to WebAssembly modules:

### Parameter Validation

```java
public class SecureParameterValidation {
    
    public WasmValue[] validateAndCall(WasmFunction function, Object... inputs) 
            throws WasmException {
        
        FunctionType signature = function.getType();
        WasmValueType[] paramTypes = signature.getParameterTypes();
        
        if (inputs.length != paramTypes.length) {
            throw new SecurityException("Parameter count mismatch");
        }
        
        WasmValue[] args = new WasmValue[inputs.length];
        
        for (int i = 0; i < inputs.length; i++) {
            args[i] = validateAndConvertParameter(inputs[i], paramTypes[i], i);
        }
        
        return function.call(args);
    }
    
    private WasmValue validateAndConvertParameter(Object input, WasmValueType expectedType, int index) {
        switch (expectedType) {
            case I32:
                if (!(input instanceof Integer)) {
                    throw new SecurityException("Parameter " + index + " must be i32");
                }
                int i32Value = (Integer) input;
                validateI32Range(i32Value, index);
                return WasmValue.i32(i32Value);
                
            case I64:
                if (!(input instanceof Long)) {
                    throw new SecurityException("Parameter " + index + " must be i64");
                }
                long i64Value = (Long) input;
                validateI64Range(i64Value, index);
                return WasmValue.i64(i64Value);
                
            case F32:
                if (!(input instanceof Float)) {
                    throw new SecurityException("Parameter " + index + " must be f32");
                }
                float f32Value = (Float) input;
                validateF32Value(f32Value, index);
                return WasmValue.f32(f32Value);
                
            case F64:
                if (!(input instanceof Double)) {
                    throw new SecurityException("Parameter " + index + " must be f64");
                }
                double f64Value = (Double) input;
                validateF64Value(f64Value, index);
                return WasmValue.f64(f64Value);
                
            default:
                throw new SecurityException("Unsupported parameter type: " + expectedType);
        }
    }
    
    private void validateI32Range(int value, int paramIndex) {
        // Add application-specific validation
        if (paramIndex == 0 && (value < 0 || value > 1000000)) {
            throw new SecurityException("Parameter 0 out of safe range: " + value);
        }
    }
    
    private void validateF32Value(float value, int paramIndex) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new SecurityException("Parameter " + paramIndex + " is not a valid number");
        }
    }
}
```

### Memory Data Validation

```java
public class SecureMemoryOperations {
    
    public void writeSecureData(WasmMemory memory, int offset, byte[] data) {
        // Validate offset and size
        validateMemoryAccess(memory, offset, data.length);
        
        // Sanitize data before writing
        byte[] sanitizedData = sanitizeData(data);
        
        memory.write(offset, sanitizedData);
    }
    
    public byte[] readSecureData(WasmMemory memory, int offset, int length) {
        // Validate offset and size  
        validateMemoryAccess(memory, offset, length);
        
        // Limit read size
        if (length > 1024 * 1024) {  // 1MB limit
            throw new SecurityException("Read size too large: " + length);
        }
        
        byte[] data = memory.read(offset, length);
        
        // Validate read data
        validateReadData(data);
        
        return data;
    }
    
    private void validateMemoryAccess(WasmMemory memory, int offset, int length) {
        if (offset < 0 || length < 0) {
            throw new SecurityException("Negative memory access parameters");
        }
        
        long memorySize = memory.size() * 65536L;  // Pages to bytes
        
        if (offset + length > memorySize) {
            throw new SecurityException("Memory access out of bounds");
        }
        
        // Check for integer overflow
        if (offset + length < offset) {
            throw new SecurityException("Memory access overflow");
        }
    }
    
    private byte[] sanitizeData(byte[] data) {
        // Remove or escape potentially dangerous byte sequences
        // This is application-specific
        
        // Example: Remove null bytes
        return Arrays.stream(data)
            .filter(b -> b != 0)
            .toArray();
    }
    
    private void validateReadData(byte[] data) {
        // Validate that read data meets security requirements
        // Example: Check for suspicious patterns
        
        for (int i = 0; i < data.length - 3; i++) {
            if (data[i] == (byte) 0x4D && data[i + 1] == (byte) 0x5A) {
                throw new SecurityException("Suspicious executable header detected in memory");
            }
        }
    }
}
```

## Host Function Security

Host functions bridge WebAssembly and Java - secure them carefully:

### Safe Host Function Implementation

```java
public class SecureHostFunctions {
    private final Set<String> allowedOperations;
    private final RateLimiter rateLimiter;
    
    public SecureHostFunctions() {
        this.allowedOperations = Set.of("log", "calculate", "validate");
        this.rateLimiter = RateLimiter.create(100.0); // 100 calls per second
    }
    
    public WasmFunction createSecureLogFunction() {
        return WasmFunction.hostFunction(
            "secure_log",
            FunctionType.of(new WasmValueType[]{WasmValueType.I32, WasmValueType.I32}, 
                           new WasmValueType[]{}),
            (args) -> {
                // Rate limiting
                if (!rateLimiter.tryAcquire()) {
                    throw new SecurityException("Rate limit exceeded for log function");
                }
                
                int messagePtr = args[0].asI32();
                int messageLen = args[1].asI32();
                
                // Validate parameters
                validateLogParameters(messagePtr, messageLen);
                
                // Get memory safely
                WasmMemory memory = getCurrentMemory();
                
                // Read with bounds checking
                byte[] messageBytes = memory.read(messagePtr, messageLen);
                
                // Sanitize message
                String message = sanitizeLogMessage(new String(messageBytes, StandardCharsets.UTF_8));
                
                // Log safely (avoid log injection)
                log.info("WebAssembly log: {}", message.replaceAll("[\r\n]", "_"));
                
                return new WasmValue[0];
            }
        );
    }
    
    private void validateLogParameters(int ptr, int len) {
        if (ptr < 0 || len < 0) {
            throw new SecurityException("Invalid log parameters");
        }
        
        if (len > 4096) {  // 4KB limit for log messages
            throw new SecurityException("Log message too long: " + len);
        }
    }
    
    private String sanitizeLogMessage(String message) {
        // Remove potentially dangerous characters
        return message.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                     .substring(0, Math.min(message.length(), 1000)); // Limit length
    }
    
    // Database access host function with strict security
    public WasmFunction createSecureDatabaseFunction(Connection connection) {
        return WasmFunction.hostFunction(
            "db_query",
            FunctionType.of(
                new WasmValueType[]{WasmValueType.I32, WasmValueType.I32}, 
                new WasmValueType[]{WasmValueType.I32}),
            (args) -> {
                // Authentication check
                if (!isAuthenticated()) {
                    throw new SecurityException("Database access requires authentication");
                }
                
                // Rate limiting
                if (!rateLimiter.tryAcquire(10)) { // Consume more tokens for DB access
                    throw new SecurityException("Database rate limit exceeded");
                }
                
                int queryPtr = args[0].asI32();
                int queryLen = args[1].asI32();
                
                // Validate and sanitize query
                String query = sanitizeDbQuery(readStringFromMemory(queryPtr, queryLen));
                
                // Execute with prepared statement only
                return executeSecureQuery(connection, query);
            }
        );
    }
    
    private String sanitizeDbQuery(String query) {
        // Whitelist approach - only allow specific query patterns
        if (!query.matches("^SELECT \\* FROM users WHERE id = \\?$")) {
            throw new SecurityException("Query not allowed: " + query);
        }
        
        return query;
    }
}
```

### Capability-Based Security

```java
public class CapabilityBasedSecurity {
    
    public enum Capability {
        FILE_READ,
        FILE_WRITE,
        NETWORK_ACCESS,
        DATABASE_READ,
        DATABASE_WRITE,
        SYSTEM_INFO
    }
    
    private final Map<String, Set<Capability>> moduleCapabilities;
    
    public void grantCapability(String moduleId, Capability capability) {
        moduleCapabilities.computeIfAbsent(moduleId, k -> new HashSet<>())
                         .add(capability);
    }
    
    public void checkCapability(String moduleId, Capability requiredCapability) {
        Set<Capability> capabilities = moduleCapabilities.get(moduleId);
        
        if (capabilities == null || !capabilities.contains(requiredCapability)) {
            throw new SecurityException("Module " + moduleId + 
                                      " lacks required capability: " + requiredCapability);
        }
    }
    
    // Use in host functions
    public WasmFunction createFileReadFunction(String moduleId) {
        return WasmFunction.hostFunction(
            "file_read",
            FunctionType.of(new WasmValueType[]{WasmValueType.I32, WasmValueType.I32}, 
                           new WasmValueType[]{WasmValueType.I32}),
            (args) -> {
                // Check capability first
                checkCapability(moduleId, Capability.FILE_READ);
                
                // Then proceed with secure file reading
                return performSecureFileRead(args);
            }
        );
    }
}
```

## Memory Safety

Ensure memory operations are safe and don't leak information:

### Secure Memory Management

```java
public class SecureMemoryManager {
    
    public void clearSensitiveMemory(WasmMemory memory) {
        // Zero out entire memory to prevent information leakage
        int memorySize = memory.size() * 65536;
        byte[] zeros = new byte[Math.min(memorySize, 1024 * 1024)]; // 1MB chunks
        
        for (int offset = 0; offset < memorySize; offset += zeros.length) {
            int chunkSize = Math.min(zeros.length, memorySize - offset);
            memory.write(offset, Arrays.copyOf(zeros, chunkSize));
        }
    }
    
    public void validateMemoryState(Instance instance) {
        WasmMemory memory = instance.getMemory("memory");
        
        // Check for suspicious memory patterns
        byte[] sample = memory.read(0, Math.min(1024, memory.size() * 65536));
        
        // Look for executable code patterns in data memory
        if (containsExecutableCode(sample)) {
            throw new SecurityException("Executable code detected in data memory");
        }
        
        // Check for excessive memory growth
        if (memory.size() > 1024) { // More than 64MB
            log.warn("WebAssembly memory size unusually large: {} pages", memory.size());
        }
    }
    
    private boolean containsExecutableCode(byte[] data) {
        // Simple heuristic - check for common executable headers/patterns
        if (data.length > 4) {
            // PE header
            if (data[0] == (byte) 0x4D && data[1] == (byte) 0x5A) return true;
            // ELF header  
            if (data[0] == (byte) 0x7F && data[1] == (byte) 0x45 && 
                data[2] == (byte) 0x4C && data[3] == (byte) 0x46) return true;
            // Mach-O header
            if (data[0] == (byte) 0xFE && data[1] == (byte) 0xED && 
                data[2] == (byte) 0xFA && data[3] == (byte) 0xCE) return true;
        }
        return false;
    }
}
```

## Deployment Security

### Production Configuration

```java
public class ProductionSecurityConfig {
    
    public WasmRuntime createProductionRuntime() throws WasmException {
        // Use specific runtime for predictable security properties
        WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI);
        
        EngineConfig config = EngineConfig.builder()
            // Disable debugging features in production
            .enableDebugInfo(false)
            .enableProfiling(false)
            
            // Strict resource limits
            .maxMemory(256 * 1024 * 1024)  // 256MB limit
            .maxModules(50)                // Limit module cache
            .maxInstances(100)             // Limit concurrent instances
            
            // Conservative optimization
            .optimizationLevel(OptimizationLevel.SPEED)
            
            .build();
            
        return runtime;
    }
    
    // Environment-specific configuration
    public void configureForEnvironment(String environment) {
        switch (environment.toLowerCase()) {
            case "production":
                System.setProperty("wasmtime4j.debug", "false");
                System.setProperty("wasmtime4j.runtime", "jni");  // Predictable choice
                break;
                
            case "staging":
                System.setProperty("wasmtime4j.debug", "false");
                // Allow auto-selection for testing
                break;
                
            case "development":
                System.setProperty("wasmtime4j.debug", "true");
                // Enable Panama for testing if available
                break;
        }
    }
}
```

### Container Security

```dockerfile
# Secure Docker configuration
FROM openjdk:23-jdk

# Create non-root user
RUN groupadd -r wasmapp && useradd -r -g wasmapp wasmapp

# Set up secure directories
WORKDIR /app
COPY --chown=wasmapp:wasmapp . .

# Remove unnecessary tools
RUN apt-get remove -y curl wget netcat-openbsd

# Run as non-root user
USER wasmapp

# Limit resources
ENTRYPOINT ["java", "-Xmx512m", "-XX:MaxDirectMemorySize=256m", \
           "-Djava.security.manager", "-Djava.security.policy=app.policy", \
           "-jar", "app.jar"]
```

## Monitoring and Auditing

### Security Event Logging

```java
public class SecurityAuditLogger {
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY");
    
    public void logSecurityEvent(SecurityEvent event) {
        MDC.put("eventType", event.getType().name());
        MDC.put("severity", event.getSeverity().name());
        MDC.put("moduleId", event.getModuleId());
        MDC.put("timestamp", event.getTimestamp().toString());
        
        switch (event.getSeverity()) {
            case CRITICAL:
                securityLog.error("Security event: {}", event.getDescription());
                // Trigger alerts
                alertingSystem.sendCriticalAlert(event);
                break;
                
            case HIGH:
                securityLog.warn("Security event: {}", event.getDescription());
                break;
                
            case MEDIUM:
                securityLog.info("Security event: {}", event.getDescription());
                break;
                
            case LOW:
                securityLog.debug("Security event: {}", event.getDescription());
                break;
        }
        
        MDC.clear();
    }
    
    public enum SecurityEventType {
        RESOURCE_LIMIT_EXCEEDED,
        INVALID_MEMORY_ACCESS,
        SUSPICIOUS_EXECUTION_PATTERN,
        UNAUTHORIZED_FILE_ACCESS,
        RATE_LIMIT_EXCEEDED,
        CAPABILITY_VIOLATION
    }
}
```

### Runtime Security Monitoring

```java
public class RuntimeSecurityMonitor {
    private final ScheduledExecutorService monitorExecutor = 
        Executors.newScheduledThreadPool(2);
    private final SecurityAuditLogger auditLogger;
    
    public void startMonitoring(WasmRuntime runtime) {
        // Monitor resource usage
        monitorExecutor.scheduleAtFixedRate(() -> {
            monitorResourceUsage(runtime);
        }, 0, 30, TimeUnit.SECONDS);
        
        // Monitor execution patterns
        monitorExecutor.scheduleAtFixedRate(() -> {
            monitorExecutionPatterns(runtime);
        }, 0, 60, TimeUnit.SECONDS);
    }
    
    private void monitorResourceUsage(WasmRuntime runtime) {
        if (!runtime.isValid()) {
            auditLogger.logSecurityEvent(SecurityEvent.builder()
                .type(SecurityEventType.RUNTIME_INVALID)
                .severity(SecuritySeverity.HIGH)
                .description("WebAssembly runtime became invalid")
                .build());
        }
        
        // Check system resource usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        
        if (usedMemory > maxMemory * 0.9) {
            auditLogger.logSecurityEvent(SecurityEvent.builder()
                .type(SecurityEventType.RESOURCE_LIMIT_EXCEEDED)
                .severity(SecuritySeverity.HIGH)
                .description("High memory usage detected: " + (usedMemory / (1024 * 1024)) + "MB")
                .build());
        }
    }
}
```

## Security Best Practices

### Checklist for Production Deployment

1. **Module Validation**:
   - ✅ Validate all WebAssembly modules before deployment
   - ✅ Use module signing/checksums for integrity
   - ✅ Scan modules for known vulnerabilities
   - ✅ Implement module allowlists

2. **Runtime Configuration**:
   - ✅ Disable debug features in production
   - ✅ Set appropriate resource limits
   - ✅ Use principle of least privilege
   - ✅ Configure secure WASI context

3. **Input Validation**:
   - ✅ Validate all parameters before WebAssembly calls
   - ✅ Implement bounds checking for memory operations
   - ✅ Sanitize all string inputs
   - ✅ Rate limit function calls

4. **Monitoring & Logging**:
   - ✅ Log all security-relevant events
   - ✅ Monitor resource usage continuously
   - ✅ Set up alerting for anomalies
   - ✅ Regular security log reviews

5. **Infrastructure**:
   - ✅ Run with minimal required permissions
   - ✅ Use container security best practices
   - ✅ Keep dependencies updated
   - ✅ Regular security assessments

### Code Review Security Checklist

When reviewing WebAssembly integration code:

- [ ] Are all WebAssembly function calls wrapped in try-catch blocks?
- [ ] Are resource limits configured appropriately?
- [ ] Are input parameters validated before passing to WebAssembly?
- [ ] Are memory operations bounds-checked?
- [ ] Are host functions implemented securely?
- [ ] Is the WASI context configured with minimal privileges?
- [ ] Are execution timeouts implemented?
- [ ] Is sensitive data cleared from memory after use?
- [ ] Are error messages sanitized to prevent information leakage?
- [ ] Is logging configured to capture security events?

Following these security practices will help ensure your Wasmtime4j integration is secure in production environments.
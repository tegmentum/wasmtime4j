# Production Deployment Guide

This guide provides comprehensive best practices and recommendations for deploying Wasmtime4j in production environments.

## Table of Contents

- [Pre-deployment Checklist](#pre-deployment-checklist)
- [Dependency Management](#dependency-management)
- [Runtime Selection](#runtime-selection)
- [JVM Configuration](#jvm-configuration)
- [Security Configuration](#security-configuration)
- [Performance Tuning](#performance-tuning)
- [Monitoring and Observability](#monitoring-and-observability)
- [Container Deployment](#container-deployment)
- [Cloud Platform Deployment](#cloud-platform-deployment)
- [Troubleshooting](#troubleshooting)

## Pre-deployment Checklist

### Environment Validation

```bash
# Verify Java version compatibility
java -version
# JNI: Java 8+ required
# Panama: Java 23+ required

# Verify native library support
java -Djava.library.path=/path/to/natives -cp app.jar \
  -Dwasmtime4j.runtime=jni YourMainClass

# Test both runtime types if available
java -Dwasmtime4j.runtime=panama -cp app.jar YourMainClass
```

### Performance Baseline

```java
// Include performance validation in deployment pipeline
@Component
public class DeploymentValidator {

    @PostConstruct
    public void validatePerformance() {
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            long startTime = System.nanoTime();

            // Create 1000 engines
            for (int i = 0; i < 1000; i++) {
                try (Engine engine = runtime.createEngine()) {
                    // Baseline validation
                }
            }

            long duration = System.nanoTime() - startTime;
            long opsPerSec = 1_000_000_000L * 1000 / duration;

            if (opsPerSec < 100_000_000L) { // 100M ops/sec threshold
                throw new RuntimeException(
                    "Performance below baseline: " + opsPerSec + " ops/sec");
            }
        }
    }
}
```

## Dependency Management

### Maven Configuration

```xml
<dependencies>
    <!-- Core API (required) -->
    <dependency>
        <groupId>ai.tegmentum</groupId>
        <artifactId>wasmtime4j</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- JNI Implementation (for Java 8+) -->
    <dependency>
        <groupId>ai.tegmentum</groupId>
        <artifactId>wasmtime4j-jni</artifactId>
        <version>1.0.0</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Panama Implementation (for Java 23+) -->
    <dependency>
        <groupId>ai.tegmentum</groupId>
        <artifactId>wasmtime4j-panama</artifactId>
        <version>1.0.0</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Native Library Loader -->
    <dependency>
        <groupId>ai.tegmentum</groupId>
        <artifactId>wasmtime4j-native-loader</artifactId>
        <version>1.0.0</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

### Gradle Configuration

```gradle
dependencies {
    implementation 'ai.tegmentum:wasmtime4j:1.0.0'
    runtimeOnly 'ai.tegmentum:wasmtime4j-jni:1.0.0'
    runtimeOnly 'ai.tegmentum:wasmtime4j-panama:1.0.0'
    runtimeOnly 'ai.tegmentum:wasmtime4j-native-loader:1.0.0'
}
```

### Native Library Deployment

```bash
# Extract and verify native libraries
mkdir -p /opt/wasmtime4j/natives
java -cp wasmtime4j-native-loader.jar \
  ai.tegmentum.wasmtime4j.nativeloader.LibraryExtractor \
  --output /opt/wasmtime4j/natives \
  --verify
```

## Runtime Selection

### Production Runtime Configuration

```java
@Configuration
@ConditionalOnProperty(value = "wasmtime4j.enabled", havingValue = "true")
public class WasmRuntimeConfig {

    @Value("${wasmtime4j.runtime.type:auto}")
    private String runtimeType;

    @Bean
    @Primary
    public WasmRuntime wasmRuntime() {
        // Explicit runtime selection for production
        switch (runtimeType.toLowerCase()) {
            case "jni":
                return WasmRuntimeFactory.create(RuntimeType.JNI);
            case "panama":
                return WasmRuntimeFactory.create(RuntimeType.PANAMA);
            case "auto":
            default:
                // Use AUTO only for development
                if (isProductionEnvironment()) {
                    // Default to JNI for production stability
                    return WasmRuntimeFactory.create(RuntimeType.JNI);
                }
                return WasmRuntimeFactory.create();
        }
    }

    @PreDestroy
    public void cleanup() throws Exception {
        wasmRuntime().close();
    }
}
```

### System Properties

```bash
# Production JVM arguments
-Dwasmtime4j.runtime=jni
-Dwasmtime4j.engine.optimization=speed
-Dwasmtime4j.cache.enabled=true
-Dwasmtime4j.cache.directory=/var/cache/wasmtime4j
-Dwasmtime4j.cache.maxSize=1073741824  # 1GB
```

## JVM Configuration

### Memory Settings

```bash
# Heap configuration for WebAssembly workloads
-Xms4g -Xmx8g

# Direct memory for native operations
-XX:MaxDirectMemorySize=2g

# Metaspace for dynamic compilation
-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m
```

### Garbage Collection

```bash
# G1GC for predictable pause times
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:G1HeapRegionSize=16m

# ZGC for ultra-low latency (Java 17+)
-XX:+UseZGC
-XX:+UseLargePages

# Parallel GC for throughput
-XX:+UseParallelGC
-XX:ParallelGCThreads=8
```

### JIT Compilation

```bash
# Aggressive compilation for long-running services
-XX:+TieredCompilation
-XX:TieredStopAtLevel=4
-XX:CompileThreshold=10000

# Disable tiered compilation for consistent performance
-XX:-TieredCompilation
-XX:CompileThreshold=1000
```

### Native Memory Tracking

```bash
# Enable native memory tracking
-XX:NativeMemoryTracking=detail

# Monitor with jcmd
jcmd <pid> VM.native_memory summary scale=MB
```

## Security Configuration

### Security Manager (if applicable)

```java
// Custom security policy for WebAssembly modules
public class WasmSecurityPolicy extends Policy {

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        PermissionCollection perms = new Permissions();

        // Minimal permissions for WebAssembly modules
        if (isWasmModule(codesource)) {
            // No file system access
            // No network access
            // No system property access
            perms.add(new RuntimePermission("accessDeclaredMembers"));
            return perms;
        }

        // Full permissions for application code
        perms.add(new AllPermission());
        return perms;
    }
}
```

### WASI Security

```java
@Service
public class SecureWasiService {

    private final WasiConfig secureWasiConfig;

    public SecureWasiService() {
        this.secureWasiConfig = WasiConfig.builder()
            // Restrict file system access
            .mapDir("/tmp", "/var/wasmtime4j/sandbox/tmp")
            .mapDir("/data", "/var/wasmtime4j/data")
            // No access to host environment
            .env("SAFE_MODE", "true")
            // Redirect output to controlled streams
            .stdout(createSecureOutputStream())
            .stderr(createSecureOutputStream())
            .build();
    }

    private OutputStream createSecureOutputStream() {
        return new FilterOutputStream(System.out) {
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                // Filter sensitive data before writing
                String output = new String(b, off, len);
                if (!containsSensitiveData(output)) {
                    super.write(b, off, len);
                }
            }
        };
    }
}
```

### Input Validation

```java
@Component
public class WasmModuleValidator {

    private static final int MAX_MODULE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final Set<String> ALLOWED_IMPORTS = Set.of(
        "env.memory", "env.log", "wasi_snapshot_preview1.*"
    );

    public void validateModule(byte[] wasmBytes) throws ValidationException {
        if (wasmBytes.length > MAX_MODULE_SIZE) {
            throw new ValidationException("Module too large: " + wasmBytes.length);
        }

        // Validate module structure
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            try (Engine engine = runtime.createEngine()) {
                if (!engine.validate(wasmBytes)) {
                    throw new ValidationException("Invalid WebAssembly module");
                }

                Module module = engine.compile(wasmBytes);
                validateImports(module.getImports());
            }
        }
    }

    private void validateImports(List<Import> imports) throws ValidationException {
        for (Import imp : imports) {
            String importName = imp.getModule() + "." + imp.getName();
            if (!isAllowedImport(importName)) {
                throw new ValidationException("Disallowed import: " + importName);
            }
        }
    }
}
```

## Performance Tuning

### Engine Configuration

```java
@Configuration
public class PerformanceConfig {

    @Bean
    public EngineConfig productionEngineConfig() {
        return EngineConfig.builder()
            .optimizationLevel(OptimizationLevel.SPEED)
            .debugInfo(false)
            .fuel(10_000_000L)  // Prevent runaway execution
            .epochInterruption(true)
            .maxMemory(1_073_741_824L)  // 1GB memory limit
            .cache(CacheConfig.builder()
                .directory("/var/cache/wasmtime4j")
                .maxSize(1_073_741_824L)  // 1GB cache
                .cleanupPolicy(CacheCleanupPolicy.LRU)
                .build())
            .build();
    }
}
```

### Connection Pooling

```java
@Service
public class WasmRuntimePool {

    private final BlockingQueue<WasmRuntime> runtimePool;
    private final ScheduledExecutorService healthChecker;

    public WasmRuntimePool(@Value("${wasmtime4j.pool.size:10}") int poolSize) {
        this.runtimePool = new ArrayBlockingQueue<>(poolSize);
        this.healthChecker = Executors.newScheduledThreadPool(1);

        // Initialize pool
        for (int i = 0; i < poolSize; i++) {
            runtimePool.offer(WasmRuntimeFactory.create(RuntimeType.JNI));
        }

        // Health check every 30 seconds
        healthChecker.scheduleAtFixedRate(this::healthCheck, 30, 30, TimeUnit.SECONDS);
    }

    public WasmRuntime borrowRuntime() throws InterruptedException {
        return runtimePool.poll(5, TimeUnit.SECONDS);
    }

    public void returnRuntime(WasmRuntime runtime) {
        if (runtime != null && isHealthy(runtime)) {
            runtimePool.offer(runtime);
        }
    }

    private void healthCheck() {
        // Validate runtime health and replace if needed
    }

    @PreDestroy
    public void shutdown() {
        healthChecker.shutdown();
        runtimePool.forEach(runtime -> {
            try {
                runtime.close();
            } catch (Exception e) {
                log.warn("Error closing runtime", e);
            }
        });
    }
}
```

### Async Processing

```java
@Service
public class AsyncWasmService {

    private final ExecutorService executorService;
    private final WasmRuntimePool runtimePool;

    public AsyncWasmService(WasmRuntimePool runtimePool) {
        this.runtimePool = runtimePool;
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
    }

    public CompletableFuture<String> processAsync(byte[] wasmBytes, String input) {
        return CompletableFuture.supplyAsync(() -> {
            WasmRuntime runtime = null;
            try {
                runtime = runtimePool.borrowRuntime();
                return processWithRuntime(runtime, wasmBytes, input);
            } catch (Exception e) {
                throw new RuntimeException("Processing failed", e);
            } finally {
                if (runtime != null) {
                    runtimePool.returnRuntime(runtime);
                }
            }
        }, executorService);
    }
}
```

## Monitoring and Observability

### Health Checks

```java
@Component
public class WasmRuntimeHealthIndicator implements HealthIndicator {

    private final WasmRuntimePool runtimePool;

    @Override
    public Health health() {
        try {
            WasmRuntime runtime = runtimePool.borrowRuntime();
            if (runtime == null) {
                return Health.down()
                    .withDetail("reason", "No runtime available")
                    .build();
            }

            // Perform health check
            long startTime = System.nanoTime();
            try (Engine engine = runtime.createEngine()) {
                // Simple validation
                engine.validate(HEALTH_CHECK_WASM);
            }
            long duration = System.nanoTime() - startTime;

            runtimePool.returnRuntime(runtime);

            return Health.up()
                .withDetail("responseTime", duration / 1_000_000 + "ms")
                .withDetail("runtimeType", runtime.getRuntimeType())
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    private static final byte[] HEALTH_CHECK_WASM = new byte[] {
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00  // Simple WASM module
    };
}
```

### Metrics

```java
@Component
public class WasmRuntimeMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter moduleCompilations;
    private final Timer functionCalls;
    private final Gauge runtimePoolSize;

    public WasmRuntimeMetrics(MeterRegistry meterRegistry, WasmRuntimePool pool) {
        this.meterRegistry = meterRegistry;

        this.moduleCompilations = Counter.builder("wasmtime4j.module.compilations")
            .description("Number of WebAssembly modules compiled")
            .register(meterRegistry);

        this.functionCalls = Timer.builder("wasmtime4j.function.calls")
            .description("WebAssembly function call duration")
            .register(meterRegistry);

        this.runtimePoolSize = Gauge.builder("wasmtime4j.runtime.pool.size")
            .description("Available runtime instances in pool")
            .register(meterRegistry, pool, WasmRuntimePool::getAvailableCount);
    }

    public void recordModuleCompilation() {
        moduleCompilations.increment();
    }

    public Timer.Sample startFunctionCallTimer() {
        return Timer.start(meterRegistry);
    }
}
```

### Logging Configuration

```yaml
# logback-spring.xml
logging:
  level:
    ai.tegmentum.wasmtime4j: INFO
    ai.tegmentum.wasmtime4j.jni: WARN
    ai.tegmentum.wasmtime4j.panama: WARN
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  appenders:
    file:
      filename: "/var/log/wasmtime4j/wasmtime4j.log"
      max-file-size: 100MB
      max-history: 30
```

## Container Deployment

### Dockerfile

```dockerfile
FROM openjdk:23-jdk-slim

# Install required packages
RUN apt-get update && apt-get install -y \
    libc6-dev \
    libgcc-s1 \
    && rm -rf /var/lib/apt/lists/*

# Create application user
RUN groupadd -r wasmapp && useradd -r -g wasmapp wasmapp

# Create directories
RUN mkdir -p /opt/wasmtime4j/{app,cache,logs} && \
    chown -R wasmapp:wasmapp /opt/wasmtime4j

# Copy application
COPY target/wasmtime4j-app.jar /opt/wasmtime4j/app/
COPY docker/entrypoint.sh /opt/wasmtime4j/

# Set permissions
RUN chmod +x /opt/wasmtime4j/entrypoint.sh

# Switch to application user
USER wasmapp

# Set working directory
WORKDIR /opt/wasmtime4j

# JVM options
ENV JAVA_OPTS="-Xms2g -Xmx4g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -XX:+UseCompressedOops \
    -Dwasmtime4j.runtime=jni \
    -Dwasmtime4j.cache.directory=/opt/wasmtime4j/cache"

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Entry point
ENTRYPOINT ["./entrypoint.sh"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  wasmtime4j-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - WASMTIME4J_RUNTIME=jni
      - WASMTIME4J_CACHE_DIRECTORY=/opt/wasmtime4j/cache
    volumes:
      - wasmtime_cache:/opt/wasmtime4j/cache
      - wasmtime_logs:/opt/wasmtime4j/logs
    deploy:
      resources:
        limits:
          memory: 6g
          cpus: '4'
        reservations:
          memory: 4g
          cpus: '2'
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  wasmtime_cache:
  wasmtime_logs:
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wasmtime4j-app
  labels:
    app: wasmtime4j-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: wasmtime4j-app
  template:
    metadata:
      labels:
        app: wasmtime4j-app
    spec:
      containers:
      - name: wasmtime4j-app
        image: wasmtime4j-app:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: WASMTIME4J_RUNTIME
          value: "jni"
        resources:
          requests:
            memory: "4Gi"
            cpu: "2"
          limits:
            memory: "6Gi"
            cpu: "4"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
        volumeMounts:
        - name: wasmtime-cache
          mountPath: /opt/wasmtime4j/cache
      volumes:
      - name: wasmtime-cache
        emptyDir:
          sizeLimit: 2Gi
---
apiVersion: v1
kind: Service
metadata:
  name: wasmtime4j-service
spec:
  selector:
    app: wasmtime4j-app
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

## Cloud Platform Deployment

### AWS ECS

```json
{
  "family": "wasmtime4j-app",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "2048",
  "memory": "6144",
  "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::account:role/ecsTaskRole",
  "containerDefinitions": [
    {
      "name": "wasmtime4j-app",
      "image": "your-ecr-repo/wasmtime4j-app:1.0.0",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "production"
        },
        {
          "name": "WASMTIME4J_RUNTIME",
          "value": "jni"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/wasmtime4j-app",
          "awslogs-region": "us-west-2",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": [
          "CMD-SHELL",
          "curl -f http://localhost:8080/actuator/health || exit 1"
        ],
        "interval": 30,
        "timeout": 10,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

### Google Cloud Run

```yaml
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: wasmtime4j-app
  annotations:
    run.googleapis.com/ingress: all
spec:
  template:
    metadata:
      annotations:
        autoscaling.knative.dev/maxScale: "10"
        run.googleapis.com/cpu-throttling: "false"
        run.googleapis.com/memory: "6Gi"
        run.googleapis.com/cpu: "4"
    spec:
      containerConcurrency: 80
      containers:
      - image: gcr.io/project-id/wasmtime4j-app:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: production
        - name: WASMTIME4J_RUNTIME
          value: jni
        resources:
          requests:
            memory: "4Gi"
            cpu: "2"
          limits:
            memory: "6Gi"
            cpu: "4"
```

### Azure Container Instances

```yaml
apiVersion: 2019-12-01
location: eastus
name: wasmtime4j-app
properties:
  containers:
  - name: wasmtime4j-app
    properties:
      image: yourregistry.azurecr.io/wasmtime4j-app:1.0.0
      ports:
      - port: 8080
      environmentVariables:
      - name: SPRING_PROFILES_ACTIVE
        value: production
      - name: WASMTIME4J_RUNTIME
        value: jni
      resources:
        requests:
          cpu: 2.0
          memoryInGb: 6.0
        limits:
          cpu: 4.0
          memoryInGb: 8.0
  osType: Linux
  restartPolicy: Always
  ipAddress:
    type: Public
    ports:
    - protocol: tcp
      port: 8080
type: Microsoft.ContainerInstance/containerGroups
```

## Troubleshooting

### Common Issues

#### Native Library Loading Failures

```bash
# Check native library availability
java -Djava.library.path=/path/to/natives \
     -Dwasmtime4j.debug=true \
     -cp app.jar YourMainClass

# Verify library architecture
file /path/to/natives/libwasmtime4j.so
# Should match JVM architecture (x86_64, aarch64)
```

#### Memory Issues

```bash
# Enable memory tracking
-XX:NativeMemoryTracking=detail

# Monitor memory usage
jcmd <pid> VM.native_memory summary scale=MB

# Check for memory leaks
jcmd <pid> VM.native_memory baseline
# ... run workload ...
jcmd <pid> VM.native_memory summary.diff scale=MB
```

#### Performance Problems

```java
// Enable performance monitoring
System.setProperty("wasmtime4j.metrics.enabled", "true");

// Check for GC pressure
-XX:+PrintGC
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps

// Profile with JFR
-XX:+FlightRecorder
-XX:StartFlightRecording=duration=60s,filename=wasmtime4j.jfr
```

### Diagnostic Commands

```bash
# Runtime information
curl http://localhost:8080/actuator/info

# Health status
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Thread dump
jcmd <pid> Thread.print

# Heap dump
jcmd <pid> GC.run_finalization
jcmd <pid> VM.heap_dump /tmp/heapdump.hprof
```

## Best Practices Summary

1. **Use explicit runtime selection** in production (avoid AUTO mode)
2. **Configure appropriate JVM settings** for your workload
3. **Implement comprehensive monitoring** and health checks
4. **Use connection pooling** for high-throughput scenarios
5. **Validate WebAssembly modules** before execution
6. **Configure security boundaries** for WASI applications
7. **Monitor native memory usage** in addition to heap
8. **Test deployment configurations** in staging environments
9. **Use blue-green deployments** for zero-downtime updates
10. **Implement proper error handling** and circuit breakers

---

For additional deployment scenarios and platform-specific guidance, consult the [Cloud Platform Documentation](cloud-platforms/) and [Container Examples](../examples/containers/).
# Error Handling Integration Guide

## Overview

This guide provides practical integration patterns and examples for incorporating wasmtime4j's error handling and diagnostics system into your applications. It covers common integration scenarios, framework-specific patterns, and production deployment considerations.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Framework Integration](#framework-integration)
3. [Production Deployment](#production-deployment)
4. [Monitoring and Observability](#monitoring-and-observability)
5. [Error Recovery Patterns](#error-recovery-patterns)
6. [Performance Optimization](#performance-optimization)
7. [Testing Strategies](#testing-strategies)

## Quick Start

### Basic Integration

```java
import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.diagnostics.*;
import ai.tegmentum.wasmtime4j.exception.*;

public class BasicWasmIntegration {
    private final ErrorLogger logger = ErrorLogger.getLogger("Application");
    private final PerformanceDiagnostics diagnostics = PerformanceDiagnostics.getInstance();

    public Object[] executeWasm(byte[] wasmBytes, String functionName, Object... args) {
        // Configure diagnostics
        DiagnosticConfiguration config = DiagnosticConfiguration.getInstance();
        config.setPerformanceMonitoringEnabled(true);
        config.setErrorRecoveryLoggingEnabled(true);

        String operationId = diagnostics.startOperation("WasmExecution");
        try (Engine engine = new Engine();
             Store store = new Store(engine);
             Module module = Module.fromBytes(engine, wasmBytes);
             Instance instance = new Instance(store, module)) {

            Function function = instance.getFunction(functionName);
            return function.call(args);

        } catch (CompilationException e) {
            logger.logCompilationError(e, wasmBytes.length, Instant.now());
            throw new IllegalArgumentException("Invalid WebAssembly module", e);
        } catch (RuntimeException e) {
            logger.logRuntimeError(e, functionName, 0);
            throw new RuntimeException("WebAssembly execution failed", e);
        } catch (ValidationException e) {
            logger.logValidationError(e, "module", "unknown");
            throw new IllegalArgumentException("WebAssembly validation failed", e);
        } finally {
            diagnostics.endOperation(operationId);
        }
    }
}
```

### Error Recovery Pattern

```java
public class ResilientWasmExecutor {
    private static final int MAX_RETRIES = 3;
    private final ErrorLogger logger = ErrorLogger.getLogger("ResilientExecutor");

    public Object[] executeWithRetry(byte[] wasmBytes, String functionName, Object... args) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return executeWasm(wasmBytes, functionName, args);
            } catch (CompilationException e) {
                // Compilation errors are not recoverable
                logger.logCompilationError(e, wasmBytes.length, Instant.now());
                throw e;
            } catch (RuntimeException e) {
                lastException = e;
                logger.logRuntimeError(e, functionName, 0);
                logger.logErrorRecovery(e, "retry", attempt < MAX_RETRIES);

                if (attempt < MAX_RETRIES) {
                    // Wait before retry
                    try {
                        Thread.sleep(100 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
            }
        }

        throw new RuntimeException("WebAssembly execution failed after " + MAX_RETRIES + " attempts", lastException);
    }
}
```

## Framework Integration

### Spring Boot Integration

#### Configuration

```java
@Configuration
@EnableConfigurationProperties(WasmProperties.class)
public class WasmAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DiagnosticConfiguration diagnosticConfiguration(WasmProperties properties) {
        DiagnosticConfiguration config = DiagnosticConfiguration.getInstance();
        config.setPerformanceMonitoringEnabled(properties.isPerformanceMonitoringEnabled());
        config.setGlobalLogLevel(Level.valueOf(properties.getLogLevel()));
        config.setSlowCompilationThreshold(properties.getSlowCompilationThreshold());
        return config;
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorLogger errorLogger() {
        return ErrorLogger.getLogger("SpringBoot");
    }

    @Bean
    @ConditionalOnMissingBean
    public WasmExecutionService wasmExecutionService(ErrorLogger errorLogger) {
        return new WasmExecutionService(errorLogger);
    }
}

@ConfigurationProperties(prefix = "wasmtime4j")
@Data
public class WasmProperties {
    private boolean performanceMonitoringEnabled = false;
    private String logLevel = "INFO";
    private long slowCompilationThreshold = 1000;
    private boolean enableDiagnostics = false;
}
```

#### Service Implementation

```java
@Service
public class WasmExecutionService {
    private final ErrorLogger errorLogger;
    private final PerformanceDiagnostics diagnostics;

    public WasmExecutionService(ErrorLogger errorLogger) {
        this.errorLogger = errorLogger;
        this.diagnostics = PerformanceDiagnostics.getInstance();
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public CompletableFuture<Object[]> executeAsync(byte[] wasmBytes, String functionName, Object... args) {
        return CompletableFuture.supplyAsync(() -> {
            String operationId = diagnostics.startOperation("AsyncExecution");
            try {
                return executeWasm(wasmBytes, functionName, args);
            } finally {
                diagnostics.endOperation(operationId);
            }
        });
    }

    @EventListener
    public void handleWasmError(WasmErrorEvent event) {
        errorLogger.logRuntimeError(event.getException(), event.getFunctionName(), 0);
        // Additional error handling logic
    }
}
```

#### REST Controller

```java
@RestController
@RequestMapping("/api/wasm")
public class WasmExecutionController {

    @Autowired
    private WasmExecutionService wasmService;

    @PostMapping("/execute")
    public ResponseEntity<?> executeWasm(
            @RequestParam("module") MultipartFile moduleFile,
            @RequestParam("function") String functionName,
            @RequestParam(value = "args", required = false) String[] args) {

        try {
            byte[] wasmBytes = moduleFile.getBytes();
            Object[] arguments = parseArguments(args);
            Object[] result = wasmService.executeWithRetry(wasmBytes, functionName, arguments);

            return ResponseEntity.ok(Map.of("result", result, "status", "success"));
        } catch (CompilationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Compilation failed",
                "message", e.getMessage(),
                "status", "error"
            ));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation failed",
                "message", e.getMessage(),
                "status", "error"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", e.getMessage(),
                "status", "error"
            ));
        }
    }

    @GetMapping("/diagnostics")
    public ResponseEntity<PerformanceSnapshot> getDiagnostics() {
        PerformanceSnapshot snapshot = PerformanceDiagnostics.getInstance().captureSnapshot();
        return ResponseEntity.ok(snapshot);
    }
}
```

### Micronaut Integration

```java
@Singleton
public class WasmExecutionBean {

    private final ErrorLogger errorLogger;

    public WasmExecutionBean() {
        this.errorLogger = ErrorLogger.getLogger("Micronaut");

        // Configure diagnostics
        DiagnosticConfiguration config = DiagnosticConfiguration.getInstance();
        config.setPerformanceMonitoringEnabled(true);
    }

    @EventListener
    public void onStartup(ServiceStartedEvent event) {
        // Perform health check on startup
        DiagnosticTool tool = new DiagnosticTool();
        DiagnosticReport report = tool.runFullDiagnostics();

        if (!report.isHealthy()) {
            throw new RuntimeException("WebAssembly system health check failed: " +
                                     report.getCompactSummary());
        }
    }
}
```

### Jakarta EE Integration

```java
@ApplicationScoped
public class WasmExecutionManager {

    @Inject
    private Logger logger; // CDI Logger

    private ErrorLogger wasmLogger;
    private PerformanceDiagnostics diagnostics;

    @PostConstruct
    public void init() {
        this.wasmLogger = ErrorLogger.getLogger("JakartaEE");
        this.diagnostics = PerformanceDiagnostics.getInstance();

        // Configure based on deployment environment
        DiagnosticConfiguration config = DiagnosticConfiguration.getInstance();
        config.setPerformanceMonitoringEnabled(isProductionEnvironment());
    }

    @Asynchronous
    public CompletableFuture<Object[]> executeAsync(byte[] wasmBytes, String functionName, Object... args) {
        return CompletableFuture.supplyAsync(() -> {
            String operationId = diagnostics.startOperation("EEAsyncExecution");
            try {
                return executeWasm(wasmBytes, functionName, args);
            } catch (WasmException e) {
                logger.log(Level.SEVERE, "WebAssembly execution failed", e);
                throw new EJBException("WebAssembly execution failed", e);
            } finally {
                diagnostics.endOperation(operationId);
            }
        });
    }
}
```

## Production Deployment

### Configuration Management

```java
public class ProductionWasmConfig {

    public static void configureForProduction() {
        DiagnosticConfiguration config = DiagnosticConfiguration.getInstance();

        // Production settings
        config.setPerformanceMonitoringEnabled(true);
        config.setDetailedStackTracesEnabled(false); // Security consideration
        config.setErrorRecoveryLoggingEnabled(true);
        config.setGlobalLogLevel(Level.INFO);
        config.setErrorLogLevel(Level.SEVERE);

        // Performance thresholds
        config.setSlowCompilationThreshold(2000); // 2 seconds
        config.setSlowRuntimeThreshold(500);      // 500ms
        config.setLargeModuleThreshold(10 * 1024 * 1024); // 10MB
    }

    public static void configureForDevelopment() {
        DiagnosticConfiguration config = DiagnosticConfiguration.getInstance();

        // Development settings
        config.setPerformanceMonitoringEnabled(true);
        config.setDetailedStackTracesEnabled(true);
        config.setErrorRecoveryLoggingEnabled(true);
        config.setGlobalLogLevel(Level.FINE);
        config.setErrorLogLevel(Level.WARNING);

        // Stricter thresholds for development
        config.setSlowCompilationThreshold(1000);
        config.setSlowRuntimeThreshold(100);
        config.setLargeModuleThreshold(5 * 1024 * 1024);
    }
}
```

### Health Checks

```java
@Component
public class WasmHealthIndicator implements HealthIndicator {

    private final DiagnosticTool diagnosticTool = new DiagnosticTool();

    @Override
    public Health health() {
        try {
            HealthCheckResult result = diagnosticTool.performHealthCheck();

            if (result.isHealthy()) {
                return Health.up()
                    .withDetail("runtime", result.getDetectedRuntime())
                    .withDetail("engine", "operational")
                    .withDetail("compilation", "functional")
                    .build();
            } else {
                return Health.down()
                    .withDetail("runtime", result.getDetectedRuntime())
                    .withDetail("errors", result.getErrors())
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Graceful Shutdown

```java
@Component
public class WasmShutdownManager {

    @PreDestroy
    public void shutdown() {
        try {
            // Capture final performance snapshot
            PerformanceSnapshot snapshot = PerformanceDiagnostics.getInstance().captureSnapshot();
            ErrorLogger.getLogger("Shutdown").logPerformanceDiagnostics(
                "FinalSnapshot", 0, Map.of("snapshot", snapshot.getCompactSummary()));

            // Reset diagnostics
            PerformanceDiagnostics.getInstance().reset();

        } catch (Exception e) {
            // Log but don't throw during shutdown
            System.err.println("Error during WebAssembly shutdown: " + e.getMessage());
        }
    }
}
```

## Monitoring and Observability

### Metrics Export

```java
@Component
public class WasmMetricsExporter {

    @Scheduled(fixedRate = 60000) // Every minute
    public void exportMetrics() {
        PerformanceSnapshot snapshot = PerformanceDiagnostics.getInstance().captureSnapshot();

        // Export to monitoring system (Prometheus, CloudWatch, etc.)
        MetricRegistry.getInstance()
            .gauge("wasm.memory.heap.usage", snapshot.getHeapUtilizationPercentage())
            .gauge("wasm.gc.overhead", snapshot.getGcOverheadPercentage())
            .gauge("wasm.operations.active", snapshot.getActiveOperationCount())
            .gauge("wasm.errors.handling.overhead", snapshot.getAverageErrorHandlingOverhead());

        // Export operation statistics
        snapshot.getOperationStatistics().forEach((type, stats) -> {
            MetricRegistry.getInstance()
                .gauge("wasm.operation." + type.toLowerCase() + ".average", stats.getAverageDuration())
                .gauge("wasm.operation." + type.toLowerCase() + ".count", stats.getOperationCount())
                .gauge("wasm.operation." + type.toLowerCase() + ".ops_per_sec", stats.getOperationsPerSecond());
        });
    }
}
```

### Logging Configuration

```properties
# logback-spring.xml or application.properties

# wasmtime4j specific loggers
logging.level.ai.tegmentum.wasmtime4j=INFO
logging.level.ai.tegmentum.wasmtime4j.error=WARN
logging.level.ai.tegmentum.wasmtime4j.diagnostics=INFO

# Pattern for structured logging
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# File appender for error analysis
logging.file.name=logs/wasmtime4j-errors.log
logging.file.max-size=100MB
logging.file.max-history=30
```

### Alerting Rules

```yaml
# Prometheus alerting rules
groups:
  - name: wasmtime4j
    rules:
      - alert: WasmHighErrorRate
        expr: increase(wasm_errors_total[5m]) > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High WebAssembly error rate detected"
          description: "WebAssembly error rate is {{ $value }} errors in 5 minutes"

      - alert: WasmSlowCompilation
        expr: wasm_operation_compilation_average > 5000
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Slow WebAssembly compilation detected"
          description: "Average compilation time is {{ $value }}ms"

      - alert: WasmHighMemoryUsage
        expr: wasm_memory_heap_usage > 90
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High WebAssembly memory usage"
          description: "Heap memory usage is {{ $value }}%"
```

## Error Recovery Patterns

### Circuit Breaker Pattern

```java
public class WasmCircuitBreaker {
    private final int failureThreshold;
    private final long timeout;
    private int failureCount = 0;
    private long lastFailureTime = 0;
    private CircuitBreakerState state = CircuitBreakerState.CLOSED;
    private final ErrorLogger logger = ErrorLogger.getLogger("CircuitBreaker");

    public Object[] executeWithCircuitBreaker(Supplier<Object[]> wasmOperation) {
        if (state == CircuitBreakerState.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime > timeout) {
                state = CircuitBreakerState.HALF_OPEN;
            } else {
                throw new RuntimeException("Circuit breaker is OPEN");
            }
        }

        try {
            Object[] result = wasmOperation.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure(e);
            throw e;
        }
    }

    private void onSuccess() {
        failureCount = 0;
        state = CircuitBreakerState.CLOSED;
    }

    private void onFailure(Exception e) {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();

        logger.logErrorRecovery(new WasmException("Circuit breaker failure", e),
                               "circuit_breaker", false);

        if (failureCount >= failureThreshold) {
            state = CircuitBreakerState.OPEN;
        }
    }

    enum CircuitBreakerState {
        CLOSED, OPEN, HALF_OPEN
    }
}
```

### Bulkhead Pattern

```java
public class WasmExecutionBulkhead {
    private final Map<String, Executor> executorPools = new HashMap<>();
    private final ErrorLogger logger = ErrorLogger.getLogger("Bulkhead");

    public WasmExecutionBulkhead() {
        // Separate thread pools for different operation types
        executorPools.put("compilation", Executors.newFixedThreadPool(2));
        executorPools.put("execution", Executors.newFixedThreadPool(8));
        executorPools.put("validation", Executors.newFixedThreadPool(4));
    }

    public CompletableFuture<Object[]> executeInBulkhead(String operationType,
                                                         Supplier<Object[]> wasmOperation) {
        Executor executor = executorPools.get(operationType);
        if (executor == null) {
            throw new IllegalArgumentException("Unknown operation type: " + operationType);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return wasmOperation.get();
            } catch (Exception e) {
                logger.logErrorRecovery(new WasmException("Bulkhead failure", e),
                                       "bulkhead_" + operationType, false);
                throw e;
            }
        }, executor);
    }
}
```

## Performance Optimization

### Compilation Caching

```java
@Component
public class WasmModuleCache {
    private final Cache<String, Module> moduleCache;
    private final ErrorLogger logger = ErrorLogger.getLogger("ModuleCache");

    public WasmModuleCache() {
        this.moduleCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(Duration.ofHours(1))
            .recordStats()
            .build();
    }

    public Module getOrCompileModule(Engine engine, byte[] wasmBytes) {
        String moduleHash = calculateHash(wasmBytes);

        return moduleCache.get(moduleHash, key -> {
            String operationId = PerformanceDiagnostics.getInstance()
                .startOperation("CachedCompilation");
            try {
                return Module.fromBytes(engine, wasmBytes);
            } catch (CompilationException e) {
                logger.logCompilationError(e, wasmBytes.length, Instant.now());
                throw e;
            } finally {
                PerformanceDiagnostics.getInstance().endOperation(operationId);
            }
        });
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logCacheStats() {
        CacheStats stats = moduleCache.stats();
        logger.logPerformanceDiagnostics("CacheStats", 0, Map.of(
            "hitRate", stats.hitRate(),
            "missRate", stats.missRate(),
            "size", moduleCache.estimatedSize()
        ));
    }
}
```

### Memory Pool Management

```java
public class WasmMemoryPoolManager {
    private final ObjectPool<Engine> enginePool;
    private final ObjectPool<Store> storePool;
    private final ErrorLogger logger = ErrorLogger.getLogger("MemoryPool");

    public WasmMemoryPoolManager() {
        this.enginePool = new GenericObjectPool<>(new EnginePooledObjectFactory());
        this.storePool = new GenericObjectPool<>(new StorePooledObjectFactory());
    }

    public <T> T executeWithPooledResources(Function<Store, T> operation) {
        Engine engine = null;
        Store store = null;

        try {
            engine = enginePool.borrowObject();
            store = storePool.borrowObject();

            return operation.apply(store);
        } catch (Exception e) {
            logger.logResourceError(new ResourceException("Pool operation failed", e),
                                   "ObjectPool", "engine+store");
            throw new RuntimeException("Pooled operation failed", e);
        } finally {
            if (store != null) {
                try {
                    storePool.returnObject(store);
                } catch (Exception e) {
                    logger.logResourceError(new ResourceException("Failed to return store", e),
                                          "ObjectPool", "store");
                }
            }
            if (engine != null) {
                try {
                    enginePool.returnObject(engine);
                } catch (Exception e) {
                    logger.logResourceError(new ResourceException("Failed to return engine", e),
                                          "ObjectPool", "engine");
                }
            }
        }
    }
}
```

## Testing Strategies

### Integration Test Base

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class WasmIntegrationTestBase {

    protected ErrorLogger testLogger;
    protected PerformanceDiagnostics diagnostics;

    @BeforeAll
    public void setupDiagnostics() {
        DiagnosticConfiguration config = DiagnosticConfiguration.getInstance();
        config.setPerformanceMonitoringEnabled(true);
        config.setDetailedStackTracesEnabled(true);
        config.setGlobalLogLevel(Level.FINE);

        this.testLogger = ErrorLogger.getLogger("IntegrationTest");
        this.diagnostics = PerformanceDiagnostics.getInstance();
    }

    @AfterEach
    public void captureTestMetrics() {
        PerformanceSnapshot snapshot = diagnostics.captureSnapshot();
        System.out.println("Test performance: " + snapshot.getCompactSummary());
    }

    @AfterAll
    public void resetDiagnostics() {
        diagnostics.reset();
    }

    protected void assertNoMemoryLeaks() {
        System.gc();
        PerformanceSnapshot snapshot = diagnostics.captureSnapshot();
        assertThat(snapshot.getHeapUtilizationPercentage()).isLessThan(80.0);
    }
}
```

### Error Scenario Testing

```java
@ExtendWith(MockitoExtension.class)
public class ErrorHandlingIntegrationTest extends WasmIntegrationTestBase {

    @Test
    public void testCompilationErrorHandling() {
        byte[] invalidWasm = {0x00, 0x00, 0x00, 0x00}; // Invalid magic

        assertThatThrownBy(() -> {
            try (Engine engine = new Engine()) {
                Module.fromBytes(engine, invalidWasm);
            }
        }).isInstanceOf(CompilationException.class)
          .hasMessageContaining("magic");

        // Verify error was logged
        ErrorMetrics metrics = testLogger.getMetrics();
        assertThat(metrics.getCompilationErrorCount()).isEqualTo(1);
    }

    @Test
    public void testErrorRecoveryAfterFailure() {
        DiagnosticTool tool = new DiagnosticTool();

        // Test that system can recover after error
        ErrorReproductionResult result = tool.reproduceErrorScenario(
            DiagnosticTool.ErrorScenario.INVALID_MAGIC_NUMBER);

        assertThat(result.isSuccessful()).isTrue();

        // Verify system still functional
        HealthCheckResult health = tool.performHealthCheck();
        assertThat(health.isHealthy()).isTrue();
    }
}
```

This integration guide provides practical patterns and examples for incorporating wasmtime4j's error handling system into real-world applications, ensuring robust and observable WebAssembly execution in production environments.
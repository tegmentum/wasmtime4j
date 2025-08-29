# Spring Boot Integration Example

This example demonstrates how to integrate Wasmtime4j with Spring Boot applications, providing a complete web service that executes WebAssembly modules.

## Features

- **RESTful API**: WebAssembly execution via HTTP endpoints
- **Async Processing**: Non-blocking WebAssembly execution
- **Module Caching**: Optimized performance with compiled module caching  
- **Health Checks**: Monitoring and diagnostics endpoints
- **File Upload**: Process binary data with WebAssembly
- **Benchmarking**: Built-in performance testing endpoints
- **Auto Configuration**: Spring Boot auto-configuration for WebAssembly

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/wasmintegration/
│   │       ├── WasmIntegrationApplication.java
│   │       ├── config/
│   │       │   └── WebAssemblyConfiguration.java
│   │       ├── controller/
│   │       │   └── WebAssemblyController.java
│   │       └── service/
│   │           └── WebAssemblyService.java
│   └── resources/
│       ├── application.yml
│       └── wasm/
│           ├── math.wasm
│           ├── text-processor.wasm
│           ├── calculator.wasm
│           ├── data-processor.wasm
│           └── benchmark.wasm
└── test/
    └── java/
        └── com/example/wasmintegration/
            └── WebAssemblyIntegrationTest.java
```

## Setup

### Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Wasmtime4j -->
    <dependency>
        <groupId>ai.tegmentum</groupId>
        <artifactId>wasmtime4j</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    
    <!-- Optional: Actuator for enhanced monitoring -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Optional: Micrometer for metrics -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-core</artifactId>
    </dependency>
</dependencies>
```

### Configuration (application.yml)

```yaml
# WebAssembly Configuration
webassembly:
  optimization-level: SPEED          # NONE, SPEED, SPEED_AND_SIZE
  enable-profiling: false           # Enable for production monitoring
  enable-debug: false               # Enable for development debugging
  runtime-type: AUTO               # AUTO, JNI, PANAMA

# Server Configuration
server:
  port: 8080

# Actuator Configuration (optional)
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,wasm
  endpoint:
    health:
      show-details: when-authorized

# Logging Configuration
logging:
  level:
    ai.tegmentum.wasmtime4j: INFO
    com.example.wasmintegration: DEBUG
```

### Main Application Class

```java
package com.example.wasmintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WasmIntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(WasmIntegrationApplication.class, args);
    }
}
```

## Usage Examples

### 1. Simple Math Operation

```bash
curl "http://localhost:8080/api/wasm/math/add?a=15&b=27"
```

Response:
```json
{
  "result": 42,
  "executionTimeMs": 0.123,
  "inputs": {
    "a": 15,
    "b": 27
  }
}
```

### 2. Text Processing

```bash
curl -X POST http://localhost:8080/api/wasm/text/process \
  -H "Content-Type: application/json" \
  -d '{"text": "hello world", "operation": "uppercase"}'
```

Response:
```json
{
  "input": "hello world",
  "output": "HELLO WORLD",
  "operation": "uppercase",
  "executionTimeMs": 0.456
}
```

### 3. Async Calculation

```bash
curl -X POST http://localhost:8080/api/wasm/async/calculate \
  -H "Content-Type: application/json" \
  -d '{"operation": "sum", "numbers": [1, 2, 3, 4, 5]}'
```

Response:
```json
{
  "result": 15,
  "executionTimeMs": 0.789,
  "operation": "sum",
  "inputCount": 5
}
```

### 4. File Processing

```bash
curl -X POST http://localhost:8080/api/wasm/data/process \
  -F "file=@document.pdf" \
  -F "operation=compress"
```

Response: Binary data with headers:
- `X-Original-Size: 1048576`
- `X-Processed-Size: 524288`  
- `X-Execution-Time-Ms: 123`
- `X-Operation: compress`

### 5. Health Check

```bash
curl http://localhost:8080/api/wasm/health
```

Response:
```json
{
  "status": "UP",
  "runtime": "PANAMA",
  "version": "36.0.2",
  "cachedModules": 3
}
```

### 6. Performance Benchmarking

```bash
curl -X POST http://localhost:8080/api/wasm/benchmark \
  -H "Content-Type: application/json" \
  -d '{
    "module": "wasm/benchmark.wasm",
    "function": "fibonacci", 
    "iterations": 10000,
    "input": 25
  }'
```

Response:
```json
{
  "module": "wasm/benchmark.wasm",
  "function": "fibonacci",
  "iterations": 10000,
  "input": 25,
  "averageTimeMs": 0.15,
  "minTimeMs": 0.12,
  "maxTimeMs": 2.34,
  "throughputOpsPerSecond": 6666.67
}
```

## WebAssembly Modules

Place your `.wasm` files in `src/main/resources/wasm/`. Example modules:

### math.wasm (WAT source)
```wat
(module
  (func $add (export "add") (param $a i32) (param $b i32) (result i32)
    local.get $a
    local.get $b
    i32.add)
)
```

### text-processor.wasm
Functions for text processing operations like uppercase, lowercase, reverse, etc.

### calculator.wasm  
Advanced math operations like sum, product, factorial, fibonacci.

## Testing

### Integration Test Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebAssemblyIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testMathAddition() {
        String url = "/api/wasm/math/add?a=10&b=20";
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("result")).isEqualTo(30);
    }

    @Test  
    void testHealthCheck() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/wasm/health", Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }
}
```

## Production Considerations

### 1. Security

```yaml
webassembly:
  # Use restrictive WASI configuration for untrusted modules
  wasi:
    inherit-stdio: false
    allowed-directories:
      - "/app/data:ro"  # Read-only access to data directory
    resource-limits:
      max-memory: 67108864  # 64MB limit
      max-execution-time: 5000  # 5 second limit
```

### 2. Performance Monitoring

```java
@Component
public class WebAssemblyMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Timer executionTimer;
    private final Counter errorCounter;
    
    public WebAssemblyMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.executionTimer = Timer.builder("wasm.execution.time")
            .description("WebAssembly function execution time")
            .register(meterRegistry);
        this.errorCounter = Counter.builder("wasm.execution.errors")
            .description("WebAssembly execution errors")
            .register(meterRegistry);
    }
    
    public void recordExecution(String module, String function, long timeNanos) {
        executionTimer.record(timeNanos, TimeUnit.NANOSECONDS);
    }
    
    public void recordError(String module, String function, Exception error) {
        errorCounter.increment(
            Tags.of("module", module, "function", function, "error", error.getClass().getSimpleName())
        );
    }
}
```

### 3. Resource Management

```java
@Configuration
public class WebAssemblyResourceConfiguration {
    
    @Bean
    @ConfigurationProperties("webassembly.thread-pool")
    public ThreadPoolTaskExecutor webAssemblyExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("wasm-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
    
    @Bean
    public WebAssemblyConnectionPool wasmConnectionPool() {
        return new WebAssemblyConnectionPool(10); // Pool of 10 instances
    }
}
```

## Running the Application

```bash
# Development mode
./mvnw spring-boot:run

# Production build and run
./mvnw clean package
java -jar target/wasm-integration-1.0.0.jar

# With specific Java version for Panama FFI
JAVA_HOME=/usr/lib/jvm/java-23 java -jar target/wasm-integration-1.0.0.jar

# Docker deployment
docker build -t wasm-integration .
docker run -p 8080:8080 wasm-integration
```

## Common Issues

1. **Native Library Loading**: Ensure proper native library packaging in JAR
2. **WASI Permissions**: Configure appropriate filesystem access for WASI modules
3. **Memory Limits**: Set appropriate memory limits for WebAssembly modules
4. **Thread Safety**: WebAssembly instances are not thread-safe; use pooling
5. **Performance**: Enable module caching and warm up critical functions

## Next Steps

- Explore [WASI integration examples](../wasi/) for filesystem and network operations
- Check [performance optimization guide](../../guides/performance.md) for production tuning
- Review [security considerations](../../guides/security.md) for production deployment
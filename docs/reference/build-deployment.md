# Build and Deployment Guide

This guide covers building Wasmtime4j from source, configuring builds for different environments, and deploying applications in production.

## Table of Contents
- [Building from Source](#building-from-source)
- [Build Configurations](#build-configurations)
- [Dependency Management](#dependency-management)
- [Native Library Compilation](#native-library-compilation)
- [Cross-Platform Building](#cross-platform-building)
- [Production Deployment](#production-deployment)
- [Container Deployment](#container-deployment)
- [Performance Tuning](#performance-tuning)
- [Monitoring and Logging](#monitoring-and-logging)

## Building from Source

### Prerequisites

- **Java**: JDK 8+ (JDK 23+ recommended for Panama FFI)
- **Rust**: 1.75.0+ with Cargo
- **Maven**: 3.6+ (or use included wrapper)
- **Platform Tools**: 
  - Linux: GCC, make, pkg-config
  - macOS: Xcode command line tools
  - Windows: Visual Studio Build Tools, MSVC

### Quick Build

```bash
# Clone the repository
git clone https://github.com/tegmentum/wasmtime4j.git
cd wasmtime4j

# Build everything (including native libraries)
./mvnw clean install

# Build without native compilation (uses pre-built libraries)
./mvnw clean install -DskipNative

# Run tests
./mvnw test

# Generate documentation
./mvnw site
```

### Build Profiles

#### Development Profile

```bash
# Fast incremental builds for development
./mvnw clean compile -Pdev
```

Configuration:
- Minimal optimization
- Debug symbols enabled
- Incremental native compilation
- Skip quality checks by default

#### Production Profile

```bash
# Optimized build for production release
./mvnw clean package -Pproduction
```

Configuration:
- Maximum optimization
- No debug symbols
- All quality checks enabled
- Code signing (if configured)

#### Cross-Platform Profile

```bash
# Build for all supported platforms
./mvnw clean package -Pall-platforms
```

## Build Configurations

### Maven Properties

Configure builds using Maven properties:

```bash
# Native compilation control
./mvnw install -Dnative.compile.skip=false
./mvnw install -Dnative.incremental.build=true
./mvnw install -Dnative.compile.parallel=true

# Target specific platform
./mvnw install -Plinux-x86_64
./mvnw install -Pmacos-aarch64
./mvnw install -Pwindows-x86_64

# Quality control
./mvnw install -DskipTests
./mvnw install -DskipQuality
./mvnw install -Dcheckstyle.skip=true
```

### Environment Configuration

Set environment variables for build customization:

```bash
# Rust configuration
export RUSTFLAGS="-C target-feature=+crt-static"
export CARGO_TARGET_DIR="/tmp/cargo-cache"

# Native library paths
export WASMTIME4J_NATIVE_PATH="/path/to/custom/natives"

# Build optimization
export MAVEN_OPTS="-Xmx4g -XX:+UseG1GC"
export JAVA_OPTS="-server -XX:+UnlockExperimentalVMOptions"
```

### Custom Build Configuration

Create `build.properties` for project-specific settings:

```properties
# build.properties
native.compile.skip=false
native.target.linux-x86_64=true
native.target.macos-aarch64=true
native.target.windows-x86_64=false

# Optimization settings
wasmtime.optimization.level=speed
wasmtime.debug.enabled=false

# Platform-specific settings
linux.cross.compile=true
macos.universal.binary=false
windows.static.linking=true
```

## Dependency Management

### Core Dependencies

The project uses minimal external dependencies:

```xml
<dependencies>
    <!-- No runtime dependencies for core library -->
    <!-- Only test dependencies -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Native Dependencies

Native dependencies are managed through the native build system:

```toml
# wasmtime4j-native/Cargo.toml
[dependencies]
wasmtime = { version = "36.0.2", features = ["wasi"] }
anyhow = "1.0"
once_cell = "1.19"

[target.'cfg(unix)'.dependencies]
libc = "0.2"

[target.'cfg(windows)'.dependencies]
winapi = { version = "0.3", features = ["processenv", "winbase"] }
```

### Version Management

Use Maven's dependency management for consistent versions:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>ai.tegmentum</groupId>
            <artifactId>wasmtime4j-bom</artifactId>
            <version>${project.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Native Library Compilation

### Automatic Compilation

Native libraries are compiled automatically during the Maven build:

```bash
# Compile for current platform
./mvnw compile

# Compile for specific platform
./mvnw compile -Ptarget-linux-x86_64

# Force recompilation
./mvnw clean compile -Dnative.incremental.build=false
```

### Manual Compilation

For development or debugging, compile native libraries manually:

```bash
cd wasmtime4j-native

# Setup cross-compilation toolchains
./scripts/setup-cross-compilation.sh

# Build for current platform
./scripts/build-native.sh

# Build for specific platform
./scripts/build-native.sh --target x86_64-unknown-linux-gnu

# Debug build
./scripts/build-native.sh --debug --verbose
```

### Native Build Scripts

The project includes platform-specific build scripts:

- `build-native.sh` - Linux/macOS build script
- `build-native.bat` - Windows build script  
- `setup-cross-compilation.sh` - Cross-compilation setup
- `build-config.sh` - Build configuration

## Cross-Platform Building

### Supported Platforms

| Platform | Architecture | Status | Notes |
|----------|-------------|--------|-------|
| Linux | x86_64 | ✅ Supported | Primary development platform |
| Linux | aarch64 | ✅ Supported | ARM64 support via cross-compilation |
| macOS | x86_64 | ✅ Supported | Intel Macs |
| macOS | aarch64 | ✅ Supported | Apple Silicon Macs |
| Windows | x86_64 | ✅ Supported | Windows 10+ |
| Windows | aarch64 | 🚧 Planned | Future support |

### Cross-Compilation Setup

#### Linux Cross-Compilation

```bash
# Install cross-compilation toolchains
sudo apt-get install gcc-aarch64-linux-gnu

# Add Rust targets
rustup target add aarch64-unknown-linux-gnu
rustup target add x86_64-unknown-linux-gnu

# Configure Cargo for cross-compilation
cat >> ~/.cargo/config.toml << EOF
[target.aarch64-unknown-linux-gnu]
linker = "aarch64-linux-gnu-gcc"
EOF

# Build for ARM64
./mvnw compile -Plinux-aarch64
```

#### macOS Cross-Compilation

```bash
# Install Xcode command line tools
xcode-select --install

# Add Rust targets  
rustup target add x86_64-apple-darwin
rustup target add aarch64-apple-darwin

# Build universal binary (both architectures)
./mvnw compile -Pmacos-universal
```

#### Windows Cross-Compilation

```powershell
# Install Visual Studio Build Tools
# Download from: https://visualstudio.microsoft.com/downloads/

# Add Rust target
rustup target add x86_64-pc-windows-msvc

# Build
./mvnw.cmd compile -Pwindows-x86_64
```

### CI/CD Cross-Platform Builds

```yaml
# .github/workflows/build.yml
name: Cross-Platform Build

on: [push, pull_request]

jobs:
  build:
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
        java: [8, 11, 17, 23]
        
    runs-on: ${{ matrix.os }}
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Setup Java
      uses: actions/setup-java@v4
      with:
        java-version: ${{ matrix.java }}
        distribution: 'temurin'
        
    - name: Setup Rust
      uses: actions-rs/toolchain@v1
      with:
        toolchain: stable
        
    - name: Build and test
      run: ./mvnw clean verify
      
    - name: Upload artifacts
      uses: actions/upload-artifact@v3
      with:
        name: wasmtime4j-${{ matrix.os }}-java${{ matrix.java }}
        path: target/*.jar
```

## Production Deployment

### Binary Distribution

Create production-ready distributions:

```bash
# Create distribution with all platforms
./mvnw clean package -Pproduction -Pall-platforms

# The resulting JAR includes native libraries for all platforms
target/wasmtime4j-1.0.0.jar
```

### Dependency Verification

Verify all dependencies are included:

```bash
# Check JAR contents
jar -tf target/wasmtime4j-1.0.0.jar | grep -E '\.(so|dylib|dll)$'

# Expected output:
# natives/linux-x86_64/libwasmtime4j.so
# natives/macos-x86_64/libwasmtime4j.dylib  
# natives/macos-aarch64/libwasmtime4j.dylib
# natives/windows-x86_64/wasmtime4j.dll
```

### Runtime Configuration

Configure for production deployment:

```java
// Production configuration
System.setProperty("wasmtime4j.runtime", "jni");  // Predictable runtime
System.setProperty("wasmtime4j.debug", "false");   // Disable debug logging
System.setProperty("wasmtime4j.native.extract", "temp"); // Extract to temp dir

// JVM tuning for production
-Xms2g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UnlockExperimentalVMOptions
-XX:+UseCGroupMemoryLimitForHeap
```

### Health Checks

Implement comprehensive health checks:

```java
@Component
public class WebAssemblyHealthCheck implements HealthIndicator {
    
    @Autowired
    private WasmRuntime runtime;
    
    @Override
    public Health health() {
        try {
            if (!runtime.isValid()) {
                return Health.down()
                    .withDetail("runtime", "Invalid")
                    .build();
            }
            
            // Test basic functionality
            Engine engine = runtime.createEngine();
            byte[] testModule = createSimpleTestModule();
            Module module = runtime.compileModule(engine, testModule);
            Instance instance = runtime.instantiate(module);
            
            // Test function execution
            WasmFunction testFunc = instance.getFunction("test");
            WasmValue[] result = testFunc.call();
            
            return Health.up()
                .withDetail("runtime", runtime.getRuntimeInfo().getRuntimeType())
                .withDetail("version", runtime.getRuntimeInfo().getVersion())
                .withDetail("test-result", result[0].asI32())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }
}
```

## Container Deployment

### Docker Configuration

#### Minimal Production Image

```dockerfile
FROM openjdk:23-jdk-slim AS builder

WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests -Pproduction

FROM openjdk:23-jre-slim

# Create non-root user
RUN groupadd -r wasmapp && useradd -r -g wasmapp wasmapp

# Install minimal runtime dependencies
RUN apt-get update && apt-get install -y \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy application JAR
COPY --from=builder /app/target/wasmtime4j-*.jar app.jar
COPY --chown=wasmapp:wasmapp docker-entrypoint.sh .

# Switch to non-root user
USER wasmapp

# Configure JVM for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD java -cp app.jar ai.tegmentum.wasmtime4j.health.HealthChecker

ENTRYPOINT ["./docker-entrypoint.sh"]
CMD ["java", "-jar", "app.jar"]
```

#### Multi-Stage Build for Development

```dockerfile
FROM rust:1.75 AS native-builder

WORKDIR /app
COPY wasmtime4j-native/ wasmtime4j-native/
COPY scripts/ scripts/

RUN cd wasmtime4j-native && \
    cargo build --release --target x86_64-unknown-linux-gnu

FROM openjdk:23-jdk AS java-builder

WORKDIR /app
COPY . .
COPY --from=native-builder /app/wasmtime4j-native/target/release/ wasmtime4j-native/target/release/

RUN ./mvnw clean package -DskipTests

FROM openjdk:23-jre-slim

RUN groupadd -r wasmapp && useradd -r -g wasmapp wasmapp
WORKDIR /app

COPY --from=java-builder /app/target/wasmtime4j-*.jar app.jar
USER wasmapp

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
CMD ["java", "-jar", "app.jar"]
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
      - name: app
        image: wasmtime4j-app:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: JAVA_OPTS
          value: "-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
        - name: WASMTIME4J_RUNTIME
          value: "jni"  # Predictable runtime choice
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
        securityContext:
          runAsNonRoot: true
          runAsUser: 1000
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
        volumeMounts:
        - name: temp
          mountPath: /tmp
        - name: wasm-modules
          mountPath: /app/wasm-modules
          readOnly: true
      volumes:
      - name: temp
        emptyDir: {}
      - name: wasm-modules
        configMap:
          name: wasm-modules
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
```

## Performance Tuning

### JVM Tuning

#### Production JVM Settings

```bash
# For high-throughput applications
JAVA_OPTS="
  -server
  -Xms2g -Xmx4g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+UnlockExperimentalVMOptions
  -XX:+UseTransparentHugePages
  -XX:+UseStringDeduplication
"

# For low-latency applications
JAVA_OPTS="
  -server
  -Xms4g -Xmx4g
  -XX:+UseZGC
  -XX:+UnlockExperimentalVMOptions
  -XX:+UseLargePages
  -XX:LargePageSizeInBytes=2m
"

# For container environments
JAVA_OPTS="
  -XX:+UseContainerSupport
  -XX:MaxRAMPercentage=75.0
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=100
"
```

#### Panama FFI Specific Settings

```bash
# For Java 23+ with Panama FFI
JAVA_OPTS="
  --enable-preview
  --enable-native-access=ALL-UNNAMED
  -XX:+UnlockExperimentalVMOptions
  -XX:+UseZGC
  -Djdk.foreign.restricted=permit
"
```

### Application Performance Tuning

```java
@Configuration
public class PerformanceConfiguration {
    
    @Bean
    public WasmRuntime optimizedRuntime() throws WasmException {
        // Pre-warm the runtime
        WasmRuntime runtime = WasmRuntimeFactory.create();
        
        // Create optimized engine
        EngineConfig config = EngineConfig.builder()
            .optimizationLevel(OptimizationLevel.SPEED)
            .enableProfiling(true)
            .build();
            
        Engine engine = runtime.createEngine(config);
        
        // Pre-compile common modules
        precompileCommonModules(engine);
        
        return runtime;
    }
    
    @Bean
    public ExecutorService wasmExecutor() {
        return Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2,
            new ThreadFactoryBuilder()
                .setNameFormat("wasm-exec-%d")
                .setDaemon(true)
                .build()
        );
    }
}
```

## Monitoring and Logging

### Application Monitoring

```java
@Component
public class WebAssemblyMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Timer executionTimer;
    private final Counter errorCounter;
    private final Gauge moduleCache;
    
    public WebAssemblyMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.executionTimer = Timer.builder("wasm.execution.time")
            .description("WebAssembly function execution time")
            .register(meterRegistry);
            
        this.errorCounter = Counter.builder("wasm.execution.errors")
            .description("WebAssembly execution errors")
            .register(meterRegistry);
            
        this.moduleCache = Gauge.builder("wasm.module.cache.size")
            .description("Number of cached WebAssembly modules")
            .register(meterRegistry, this, WebAssemblyMetrics::getCacheSize);
    }
    
    public void recordExecution(String module, String function, Duration duration) {
        executionTimer.record(duration);
        
        Tags tags = Tags.of(
            "module", module,
            "function", function
        );
        
        Timer.Sample.start(meterRegistry)
            .stop(Timer.builder("wasm.function.execution")
                .tags(tags)
                .register(meterRegistry));
    }
    
    public void recordError(String module, String function, Exception error) {
        errorCounter.increment(Tags.of(
            "module", module,
            "function", function,
            "error", error.getClass().getSimpleName()
        ));
    }
}
```

### Logging Configuration

```yaml
# application.yml
logging:
  level:
    ai.tegmentum.wasmtime4j: INFO
    ai.tegmentum.wasmtime4j.performance: DEBUG
    
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    
  file:
    name: logs/wasmtime4j.log
    max-size: 100MB
    max-history: 10
    
# Separate security logging
  loggers:
    SECURITY:
      level: INFO
      appenders:
        - name: SECURITY_FILE
          type: RollingFile
          fileName: logs/security.log
```

### Production Monitoring Setup

```bash
# JVM metrics
JAVA_OPTS="$JAVA_OPTS -javaagent:jmx_prometheus_javaagent.jar=8081:jmx_config.yml"

# Application Performance Monitoring
JAVA_OPTS="$JAVA_OPTS -javaagent:apm-agent.jar"
JAVA_OPTS="$JAVA_OPTS -Delastic.apm.service_name=wasmtime4j-app"
JAVA_OPTS="$JAVA_OPTS -Delastic.apm.server_urls=http://apm-server:8200"

# Flight Recorder for profiling
JAVA_OPTS="$JAVA_OPTS -XX:+FlightRecorder"
JAVA_OPTS="$JAVA_OPTS -XX:StartFlightRecording=duration=60s,filename=app.jfr"
```

This comprehensive build and deployment guide ensures reliable production deployment of Wasmtime4j applications across different environments and platforms.
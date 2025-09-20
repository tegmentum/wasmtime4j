# Comparison Test Framework Configuration Guide

This guide provides comprehensive configuration instructions for the wasmtime4j comparison test framework across different deployment scenarios, including local development, CI/CD environments, and production validation systems.

## Overview

The comparison test framework supports flexible configuration through multiple mechanisms:

- **System Properties**: JVM-level configuration
- **Environment Variables**: Environment-specific settings
- **Configuration Files**: Structured configuration with validation
- **Programmatic Configuration**: Runtime configuration through APIs

## Configuration Hierarchy

Configuration values are resolved in the following order (highest precedence first):

1. **Programmatic Configuration**: Values set via APIs
2. **System Properties**: JVM command-line properties (`-D`)
3. **Environment Variables**: OS environment variables
4. **Configuration Files**: `comparison-test.properties` or `comparison-test.yml`
5. **Default Values**: Built-in framework defaults

## Core Configuration Properties

### Test Execution Configuration

#### Test Suite Selection

```properties
# Test suite configuration
wasmtime4j.comparison.test.suites=smoke,core,advanced,wasi
wasmtime4j.comparison.test.include.patterns=.*
wasmtime4j.comparison.test.exclude.patterns=disabled.*,experimental.*

# Test discovery and loading
wasmtime4j.comparison.test.discovery.enabled=true
wasmtime4j.comparison.test.discovery.recursive=true
wasmtime4j.comparison.test.discovery.timeout=300000

# Test execution timeout (milliseconds)
wasmtime4j.comparison.test.execution.timeout=60000
wasmtime4j.comparison.test.execution.retry.count=3
wasmtime4j.comparison.test.execution.retry.delay=1000
```

#### Runtime Configuration

```properties
# Runtime selection and configuration
wasmtime4j.comparison.runtimes=jni,panama,native
wasmtime4j.comparison.runtime.jni.enabled=true
wasmtime4j.comparison.runtime.panama.enabled=true
wasmtime4j.comparison.runtime.native.enabled=true

# Runtime-specific settings
wasmtime4j.comparison.runtime.jni.heap.size=512m
wasmtime4j.comparison.runtime.panama.heap.size=512m
wasmtime4j.comparison.runtime.native.binary.path=/usr/local/bin/wasmtime

# Performance settings
wasmtime4j.comparison.runtime.warmup.iterations=5
wasmtime4j.comparison.runtime.benchmark.iterations=10
wasmtime4j.comparison.runtime.gc.between.tests=true
```

### Analysis Configuration

#### Coverage Analysis

```properties
# Coverage analysis settings
wasmtime4j.comparison.coverage.enabled=true
wasmtime4j.comparison.coverage.target.percentage=95.0
wasmtime4j.comparison.coverage.feature.mapping.file=wasmtime-features.json

# Coverage categories to analyze
wasmtime4j.comparison.coverage.categories=core,advanced,wasi,experimental

# Gap analysis configuration
wasmtime4j.comparison.coverage.gap.severity.threshold=medium
wasmtime4j.comparison.coverage.gap.include.experimental=false
```

#### Performance Analysis

```properties
# Performance analysis configuration
wasmtime4j.comparison.performance.enabled=true
wasmtime4j.comparison.performance.baseline.file=performance-baseline.json
wasmtime4j.comparison.performance.regression.threshold=5.0

# Performance metrics to collect
wasmtime4j.comparison.performance.metrics=execution_time,memory_usage,throughput,cpu_usage

# Statistical analysis settings
wasmtime4j.comparison.performance.statistical.confidence=95.0
wasmtime4j.comparison.performance.outlier.detection=iqr
wasmtime4j.comparison.performance.trend.analysis=true
```

#### Behavioral Analysis

```properties
# Behavioral analysis configuration
wasmtime4j.comparison.behavioral.enabled=true
wasmtime4j.comparison.behavioral.strict.mode=false
wasmtime4j.comparison.behavioral.tolerance.numeric=1e-9
wasmtime4j.comparison.behavioral.tolerance.floating.point=1e-6

# Output comparison settings
wasmtime4j.comparison.behavioral.output.comparison.mode=strict
wasmtime4j.comparison.behavioral.memory.comparison.enabled=true
wasmtime4j.comparison.behavioral.exception.comparison.enabled=true
```

### Reporting Configuration

#### Report Generation

```properties
# Report generation settings
wasmtime4j.comparison.reporting.enabled=true
wasmtime4j.comparison.reporting.formats=html,json,csv,pdf
wasmtime4j.comparison.reporting.output.directory=target/comparison-reports

# Report content configuration
wasmtime4j.comparison.reporting.include.details=true
wasmtime4j.comparison.reporting.include.recommendations=true
wasmtime4j.comparison.reporting.include.charts=true
wasmtime4j.comparison.reporting.include.raw.data=false

# HTML report configuration
wasmtime4j.comparison.reporting.html.theme=default
wasmtime4j.comparison.reporting.html.interactive=true
wasmtime4j.comparison.reporting.html.embed.resources=true
```

#### Dashboard Configuration

```properties
# Dashboard configuration
wasmtime4j.comparison.dashboard.enabled=true
wasmtime4j.comparison.dashboard.auto.refresh=true
wasmtime4j.comparison.dashboard.refresh.interval=60000

# Dashboard data sources
wasmtime4j.comparison.dashboard.data.retention.days=30
wasmtime4j.comparison.dashboard.historical.trends=true
wasmtime4j.comparison.dashboard.real.time.updates=false
```

### WASI Configuration

#### WASI Integration

```properties
# WASI test configuration
wasmtime4j.comparison.wasi.enabled=true
wasmtime4j.comparison.wasi.preview.versions=1,2
wasmtime4j.comparison.wasi.test.directory=/tmp/wasi-tests

# WASI environment configuration
wasmtime4j.comparison.wasi.filesystem.sandbox=true
wasmtime4j.comparison.wasi.filesystem.root=/tmp/wasi-sandbox
wasmtime4j.comparison.wasi.environment.variables=PATH,HOME,USER

# WASI I/O configuration
wasmtime4j.comparison.wasi.stdio.redirect=true
wasmtime4j.comparison.wasi.stdio.capture=true
wasmtime4j.comparison.wasi.network.access=false
```

## Deployment Scenarios

### 1. Local Development Environment

Configuration for development and debugging:

**System Properties:**
```bash
-Dwasmtime4j.comparison.test.suites=smoke
-Dwasmtime4j.comparison.runtimes=jni,panama
-Dwasmtime4j.comparison.reporting.formats=html
-Dwasmtime4j.comparison.reporting.include.details=true
-Dwasmtime4j.comparison.performance.enabled=false
-Dwasmtime4j.comparison.test.execution.timeout=30000
```

**Maven Configuration:**
```xml
<properties>
    <wasmtime4j.comparison.test.suites>smoke,core</wasmtime4j.comparison.test.suites>
    <wasmtime4j.comparison.reporting.output.directory>${project.build.directory}/dev-reports</wasmtime4j.comparison.reporting.output.directory>
</properties>
```

**Configuration File (`comparison-test-dev.properties`):**
```properties
# Development configuration
wasmtime4j.comparison.test.suites=smoke,core
wasmtime4j.comparison.test.execution.timeout=30000
wasmtime4j.comparison.performance.enabled=false
wasmtime4j.comparison.reporting.formats=html
wasmtime4j.comparison.reporting.include.details=true
wasmtime4j.comparison.behavioral.strict.mode=false
```

### 2. Continuous Integration Environment

Configuration for CI/CD pipelines:

**GitHub Actions Configuration:**
```yaml
# .github/workflows/comparison-tests.yml
env:
  WASMTIME4J_COMPARISON_TEST_SUITES: "smoke,core"
  WASMTIME4J_COMPARISON_RUNTIMES: "jni,panama"
  WASMTIME4J_COMPARISON_REPORTING_FORMATS: "html,json"
  WASMTIME4J_COMPARISON_PERFORMANCE_ENABLED: "true"
  WASMTIME4J_COMPARISON_PERFORMANCE_BASELINE_FILE: "ci-baseline.json"
  WASMTIME4J_COMPARISON_TEST_EXECUTION_TIMEOUT: "120000"
  WASMTIME4J_COMPARISON_REPORTING_OUTPUT_DIRECTORY: "ci-reports"
```

**Maven Surefire Configuration:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <wasmtime4j.comparison.test.suites>smoke,core,advanced</wasmtime4j.comparison.test.suites>
            <wasmtime4j.comparison.performance.regression.threshold>10.0</wasmtime4j.comparison.performance.regression.threshold>
            <wasmtime4j.comparison.behavioral.strict.mode>true</wasmtime4j.comparison.behavioral.strict.mode>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

**CI Configuration File (`comparison-test-ci.properties`):**
```properties
# CI environment configuration
wasmtime4j.comparison.test.suites=smoke,core,advanced
wasmtime4j.comparison.test.execution.timeout=120000
wasmtime4j.comparison.test.execution.retry.count=3
wasmtime4j.comparison.performance.enabled=true
wasmtime4j.comparison.performance.regression.threshold=10.0
wasmtime4j.comparison.behavioral.strict.mode=true
wasmtime4j.comparison.reporting.formats=html,json,junit
wasmtime4j.comparison.reporting.include.raw.data=true
```

### 3. Multi-Platform CI Environment

Configuration for cross-platform validation:

**Platform-Specific Configuration:**

**Linux:**
```properties
# Linux-specific configuration
wasmtime4j.comparison.runtime.native.binary.path=/usr/local/bin/wasmtime
wasmtime4j.comparison.runtime.jni.library.path=/usr/local/lib
wasmtime4j.comparison.wasi.filesystem.root=/tmp/wasi-sandbox-linux
```

**Windows:**
```properties
# Windows-specific configuration
wasmtime4j.comparison.runtime.native.binary.path=C:\\Program Files\\wasmtime\\wasmtime.exe
wasmtime4j.comparison.runtime.jni.library.path=C:\\Program Files\\wasmtime\\lib
wasmtime4j.comparison.wasi.filesystem.root=C:\\temp\\wasi-sandbox-windows
```

**macOS:**
```properties
# macOS-specific configuration
wasmtime4j.comparison.runtime.native.binary.path=/usr/local/bin/wasmtime
wasmtime4j.comparison.runtime.jni.library.path=/usr/local/lib
wasmtime4j.comparison.wasi.filesystem.root=/tmp/wasi-sandbox-macos
```

### 4. Production Validation Environment

Configuration for comprehensive production validation:

**System Properties:**
```bash
-Dwasmtime4j.comparison.test.suites=all
-Dwasmtime4j.comparison.runtimes=jni,panama,native
-Dwasmtime4j.comparison.performance.enabled=true
-Dwasmtime4j.comparison.performance.statistical.confidence=99.0
-Dwasmtime4j.comparison.behavioral.strict.mode=true
-Dwasmtime4j.comparison.coverage.target.percentage=98.0
-Dwasmtime4j.comparison.test.execution.timeout=300000
```

**Production Configuration File (`comparison-test-prod.properties`):**
```properties
# Production validation configuration
wasmtime4j.comparison.test.suites=all
wasmtime4j.comparison.test.execution.timeout=300000
wasmtime4j.comparison.test.execution.retry.count=5

# Comprehensive runtime testing
wasmtime4j.comparison.runtimes=jni,panama,native
wasmtime4j.comparison.runtime.warmup.iterations=10
wasmtime4j.comparison.runtime.benchmark.iterations=50

# Strict analysis settings
wasmtime4j.comparison.coverage.enabled=true
wasmtime4j.comparison.coverage.target.percentage=98.0
wasmtime4j.comparison.performance.enabled=true
wasmtime4j.comparison.performance.statistical.confidence=99.0
wasmtime4j.comparison.behavioral.enabled=true
wasmtime4j.comparison.behavioral.strict.mode=true

# Comprehensive reporting
wasmtime4j.comparison.reporting.formats=html,json,csv,pdf,xml
wasmtime4j.comparison.reporting.include.details=true
wasmtime4j.comparison.reporting.include.recommendations=true
wasmtime4j.comparison.reporting.include.raw.data=true

# WASI comprehensive testing
wasmtime4j.comparison.wasi.enabled=true
wasmtime4j.comparison.wasi.preview.versions=1,2
wasmtime4j.comparison.wasi.filesystem.sandbox=true
```

### 5. Performance Benchmarking Environment

Configuration optimized for performance analysis:

**Benchmarking Configuration:**
```properties
# Performance benchmarking configuration
wasmtime4j.comparison.test.suites=performance,stress
wasmtime4j.comparison.performance.enabled=true
wasmtime4j.comparison.performance.benchmark.iterations=100
wasmtime4j.comparison.runtime.warmup.iterations=20

# Statistical analysis
wasmtime4j.comparison.performance.statistical.confidence=99.5
wasmtime4j.comparison.performance.outlier.detection=modified_z_score
wasmtime4j.comparison.performance.trend.analysis=true

# Memory analysis
wasmtime4j.comparison.performance.memory.profiling=true
wasmtime4j.comparison.performance.gc.analysis=true
wasmtime4j.comparison.runtime.gc.between.tests=true

# Detailed metrics collection
wasmtime4j.comparison.performance.metrics=execution_time,memory_usage,throughput,cpu_usage,gc_time,jit_time
```

## Environment-Specific Configuration

### Docker Environment

**Dockerfile Configuration:**
```dockerfile
ENV WASMTIME4J_COMPARISON_TEST_SUITES=smoke,core
ENV WASMTIME4J_COMPARISON_RUNTIMES=jni,panama
ENV WASMTIME4J_COMPARISON_REPORTING_OUTPUT_DIRECTORY=/app/reports
ENV WASMTIME4J_COMPARISON_WASI_FILESYSTEM_ROOT=/tmp/wasi-sandbox
ENV WASMTIME4J_COMPARISON_RUNTIME_NATIVE_BINARY_PATH=/usr/local/bin/wasmtime
```

**Docker Compose Configuration:**
```yaml
version: '3.8'
services:
  comparison-tests:
    environment:
      - WASMTIME4J_COMPARISON_TEST_SUITES=all
      - WASMTIME4J_COMPARISON_PERFORMANCE_ENABLED=true
      - WASMTIME4J_COMPARISON_REPORTING_FORMATS=html,json
    volumes:
      - ./reports:/app/reports
      - ./config/comparison-test.properties:/app/config/comparison-test.properties
```

### Kubernetes Environment

**ConfigMap Configuration:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: comparison-test-config
data:
  comparison-test.properties: |
    wasmtime4j.comparison.test.suites=smoke,core,advanced
    wasmtime4j.comparison.runtimes=jni,panama
    wasmtime4j.comparison.performance.enabled=true
    wasmtime4j.comparison.reporting.formats=html,json
    wasmtime4j.comparison.reporting.output.directory=/var/reports
```

**Pod Configuration:**
```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: comparison-tests
    env:
    - name: WASMTIME4J_COMPARISON_TEST_SUITES
      valueFrom:
        configMapKeyRef:
          name: comparison-test-config
          key: test.suites
    volumeMounts:
    - name: config-volume
      mountPath: /app/config
    - name: reports-volume
      mountPath: /var/reports
```

## Advanced Configuration

### Custom Configuration Sources

**Programmatic Configuration:**
```java
// Create custom configuration
ComparisonTestConfiguration config = ComparisonTestConfiguration.builder()
    .testSuites(Arrays.asList("smoke", "core"))
    .runtimes(Arrays.asList(RuntimeType.JNI, RuntimeType.PANAMA))
    .reportingFormats(Arrays.asList("html", "json"))
    .performanceEnabled(true)
    .behavioralStrictMode(true)
    .build();

// Apply configuration
ComparisonTestFramework framework = new ComparisonTestFramework(config);
```

**Configuration Validation:**
```java
// Validate configuration
ValidationResult result = config.validate();
if (!result.isValid()) {
    System.err.println("Configuration errors:");
    result.getErrors().forEach(System.err::println);
}
```

### Configuration Profiles

**Profile-Based Configuration:**
```properties
# Base configuration (comparison-test.properties)
wasmtime4j.comparison.profiles.active=development

# Development profile (comparison-test-development.properties)
wasmtime4j.comparison.test.suites=smoke
wasmtime4j.comparison.performance.enabled=false

# Production profile (comparison-test-production.properties)
wasmtime4j.comparison.test.suites=all
wasmtime4j.comparison.performance.enabled=true
wasmtime4j.comparison.behavioral.strict.mode=true
```

## Troubleshooting Configuration

### Common Configuration Issues

1. **Test Suite Not Found**
   ```
   Error: Test suite 'invalid-suite' not found
   Solution: Check available test suites with wasmtime4j.comparison.test.suites=smoke,core,advanced,wasi
   ```

2. **Runtime Not Available**
   ```
   Error: Panama runtime not available on Java 11
   Solution: Use wasmtime4j.comparison.runtimes=jni for Java < 23
   ```

3. **Memory Issues**
   ```
   Error: OutOfMemoryError during comparison tests
   Solution: Increase heap size with wasmtime4j.comparison.runtime.jni.heap.size=1024m
   ```

### Configuration Validation

Enable configuration validation to catch issues early:

```properties
wasmtime4j.comparison.configuration.validation.enabled=true
wasmtime4j.comparison.configuration.validation.strict=true
wasmtime4j.comparison.configuration.validation.fail.on.warnings=false
```

This comprehensive configuration guide enables effective deployment of the comparison test framework across all environments while maintaining flexibility and reliability.
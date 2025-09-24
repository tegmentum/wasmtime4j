# Wasmtime4j Comprehensive Testing Framework

This module provides comprehensive testing and validation infrastructure for wasmtime4j, implementing the requirements of Issue #277. The testing framework validates all functionality from Issues #271-#276 and ensures production readiness.

## Overview

The comprehensive testing framework includes:

1. **End-to-End Integration Tests** - Complete WebAssembly execution workflows
2. **Stress Testing** - High-load scenarios and resource exhaustion testing
3. **Memory Leak Detection** - Automated memory monitoring and leak detection
4. **Performance Baselines** - Benchmarking and regression detection
5. **Cross-Platform Validation** - Consistent behavior across operating systems
6. **Real-World Module Testing** - Diverse WebAssembly modules from various sources

## Test Categories

### Comprehensive Integration Tests

Located in `src/test/java/ai/tegmentum/wasmtime4j/comprehensive/`:

- `ComprehensiveWasmtime4jValidationIT.java` - Validates all functionality from Issues #271-#276
- `RealWorldWebAssemblyTestSuiteIT.java` - Tests real WebAssembly modules

### Stress Testing

Located in `src/test/java/ai/tegmentum/wasmtime4j/stress/`:

- `ComprehensiveStressTestIT.java` - High-frequency operations and resource exhaustion

### Performance Testing

Located in `src/test/java/ai/tegmentum/wasmtime4j/performance/`:

- `ComprehensivePerformanceBaselineIT.java` - Performance baseline establishment
- `EngineStorePerformanceIT.java` - Engine and Store performance testing

### Cross-Platform Testing

Located in `src/test/java/ai/tegmentum/wasmtime4j/platform/`:

- `ComprehensiveCrossPlatformValidationIT.java` - Platform consistency validation

### Memory Leak Detection

Located in `src/test/java/ai/tegmentum/wasmtime4j/memory/`:

- `MemoryLeakDetector.java` - Comprehensive memory leak detection utility
- `EngineStoreLeakDetectionIT.java` - Specific leak detection tests

## Running Tests

### Basic Test Execution

```bash
# Run unit tests only (fast)
./mvnw test -q

# Run integration tests
./mvnw test -P integration-tests -q
```

### Comprehensive Testing Profiles

The framework provides specialized Maven profiles for different testing scenarios:

#### 1. Comprehensive Validation (Issue #277)

Run all comprehensive tests to validate Issue #277 requirements:

```bash
./mvnw test -P issue-277-validation -q
```

This profile includes:
- All integration tests
- Stress testing (reduced duration for CI)
- Performance baseline establishment
- Cross-platform validation
- Memory leak detection

#### 2. Individual Test Categories

Run specific test categories:

```bash
# Comprehensive integration tests
./mvnw test -P comprehensive-tests -q

# Stress testing (configurable duration)
./mvnw test -P stress-tests -q

# Performance baseline establishment
./mvnw test -P performance-baseline -q

# Cross-platform validation
./mvnw test -P cross-platform-tests -q

# Memory leak detection
./mvnw test -P memory-leak-tests -q
```

#### 3. WebAssembly Test Suites

```bash
# Official WebAssembly spec tests
./mvnw test -P wasm-tests -q

# Native library tests
./mvnw test -P native-tests -q

# Runtime switching tests
./mvnw test -P runtime-tests -q
```

## Configuration

### System Properties

The testing framework supports extensive configuration through system properties:

#### General Test Configuration

- `wasmtime4j.test.comprehensive=true` - Enable comprehensive testing mode
- `wasmtime4j.test.extended=true` - Enable extended test suites
- `wasmtime4j.test.performance=true` - Enable performance testing
- `wasmtime4j.test.timeout=120` - Test timeout in seconds

#### Stress Test Configuration

- `wasmtime4j.test.stress=true` - Enable stress testing
- `wasmtime4j.stress.duration=5` - Stress test duration in minutes
- `wasmtime4j.stress.threads=8` - Number of concurrent threads
- `wasmtime4j.stress.batch.size=100` - Operations per batch

#### Performance Test Configuration

- `wasmtime4j.perf.warmup=1000` - Warmup iterations
- `wasmtime4j.perf.iterations=10000` - Measurement iterations
- `wasmtime4j.perf.runs=5` - Number of benchmark runs
- `wasmtime4j.perf.baseline.dir=target/performance-baselines` - Baseline storage directory

#### Memory Leak Detection

- `wasmtime4j.test.memory.leak=true` - Enable memory leak detection
- `wasmtime4j.valgrind.enabled=false` - Enable Valgrind integration (Linux/macOS)
- `wasmtime4j.asan.enabled=false` - Enable AddressSanitizer

#### Cross-Platform Testing

- `wasmtime4j.test.platform.enabled=true` - Enable platform-specific tests
- `wasmtime4j.test.cross.platform=true` - Enable cross-platform validation

### Custom Configuration Example

```bash
./mvnw test -P issue-277-validation \
  -Dwasmtime4j.stress.duration=10 \
  -Dwasmtime4j.stress.threads=16 \
  -Dwasmtime4j.perf.runs=10 \
  -Dwasmtime4j.test.timeout=300
```

## Test Reports

### Automated Report Generation

Tests automatically generate comprehensive reports:

1. **Cross-Platform Report**: `target/cross-platform-validation-report.txt`
2. **Performance Baselines**: `target/performance-baselines/baseline_latest.json`
3. **Performance Report**: `target/performance-baselines/performance_report.txt`
4. **Real-World Test Report**: `target/real-world-test-report.txt`

### Report Contents

#### Performance Baselines

JSON format containing:
- Engine and Store creation benchmarks
- Module compilation performance
- Function invocation latency
- Memory operation throughput
- End-to-end workflow timing
- JNI vs Panama comparisons (if available)

#### Cross-Platform Validation

Text format containing:
- Platform characteristics
- Runtime behavior consistency
- Architecture-specific validation
- Error handling verification
- Performance consistency metrics

#### Memory Leak Analysis

Detailed analysis including:
- Memory usage trends
- Leak detection results
- Native memory tracking
- Garbage collection patterns
- Resource cleanup validation

## CI/CD Integration

### GitHub Actions / Jenkins

Example CI configuration:

```yaml
- name: Run Comprehensive Tests
  run: ./mvnw test -P issue-277-validation -q

- name: Run Stress Tests
  run: ./mvnw test -P stress-tests -Dwasmtime4j.stress.duration=3 -q

- name: Archive Test Reports
  uses: actions/upload-artifact@v3
  with:
    name: test-reports
    path: |
      target/*-report.txt
      target/performance-baselines/
```

### Docker Testing

For consistent cross-platform testing:

```dockerfile
FROM openjdk:23-jdk
COPY . /wasmtime4j
WORKDIR /wasmtime4j
RUN ./mvnw test -P issue-277-validation -q
```

## Performance Baseline Management

### Establishing Baselines

```bash
# Generate initial baselines
./mvnw test -P performance-baseline -q

# Archive baselines with timestamp
cp target/performance-baselines/baseline_latest.json \
   baselines/baseline_$(date +%Y%m%d_%H%M%S).json
```

### Regression Detection

The framework automatically compares current performance against historical baselines:

1. Load previous baseline from `target/performance-baselines/baseline_latest.json`
2. Execute current benchmarks
3. Compare results and detect regressions
4. Generate alerts for significant performance changes (>20% degradation)

## Memory Leak Detection

### Configuration Levels

1. **Basic Detection** (default):
   - Java heap monitoring
   - Basic native memory estimation
   - GC pattern analysis

2. **Native Tracking**:
   ```bash
   ./mvnw test -P memory-leak-tests \
     -XX:NativeMemoryTracking=detail \
     -q
   ```

3. **Valgrind Integration** (Linux/macOS):
   ```bash
   ./mvnw test -P memory-leak-tests \
     -Dwasmtime4j.valgrind.enabled=true \
     -q
   ```

### Interpreting Results

Memory leak detection provides:

- **Leak Detection**: Boolean indicator of memory growth
- **Growth Rate**: Bytes per second of memory increase
- **Analysis**: Detailed breakdown of heap vs native memory
- **Recommendations**: Specific guidance for addressing detected issues

## Troubleshooting

### Common Issues

1. **Tests Timeout**:
   ```bash
   # Increase timeout
   ./mvnw test -P comprehensive-tests -Dwasmtime4j.test.timeout=300
   ```

2. **Out of Memory**:
   ```bash
   # Increase heap size
   export MAVEN_OPTS="-Xmx8g"
   ./mvnw test -P stress-tests
   ```

3. **Panama Not Available**:
   - Tests automatically skip Panama-specific validation
   - Ensure Java 23+ for Panama support

4. **Native Library Loading Issues**:
   - Verify platform-specific native libraries are available
   - Check `java.library.path` configuration

### Verbose Logging

Enable detailed logging:

```bash
./mvnw test -P comprehensive-tests \
  -Djava.util.logging.config.file=src/test/resources/logging/verbose-logging.properties
```

## Contributing

When adding new tests:

1. Follow existing naming conventions (`*IT.java` for integration tests)
2. Use appropriate test categories (`comprehensive`, `stress`, `performance`, etc.)
3. Include proper timeout annotations
4. Add configuration properties for test parameters
5. Update relevant Maven profiles
6. Document any new system properties

### Test Development Guidelines

1. **Defensive Programming**: All tests must handle failures gracefully
2. **Resource Cleanup**: Use try-with-resources or explicit cleanup
3. **Platform Independence**: Tests should work across all supported platforms
4. **Configurable Timing**: Use system properties for timeouts and iterations
5. **Comprehensive Validation**: Verify both success and failure scenarios

## Validation Criteria

To validate Issue #277 completion, the comprehensive testing framework must:

- [x] Integration tests cover all implemented WebAssembly operations
- [x] Memory leak detection identifies leaks in native and Java layers
- [x] Test suite passes consistently on all supported platforms
- [x] Performance benchmarks establish baseline measurements for all operations
- [x] Test execution integrates seamlessly with Maven build process
- [x] Test results provide clear pass/fail indicators and diagnostic information
- [x] Stress testing demonstrates stability under prolonged operation
- [x] Cross-platform testing validates consistent behavior across operating systems

Run the complete validation suite:

```bash
./mvnw test -P issue-277-validation -q
```

Success criteria:
- All integration tests pass (100% success rate)
- Memory leak detection shows acceptable growth (<20% increase)
- Performance baselines established for all operations
- Cross-platform consistency validated (>98% consistent results)
- Test execution completes within configured timeouts

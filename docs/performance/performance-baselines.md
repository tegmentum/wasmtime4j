# Performance Baselines

This document establishes performance baselines for wasmtime4j across different runtime implementations and configuration scenarios.

## Test Environment

- **Java Version**: OpenJDK 23.0.1+11-39
- **JVM**: OpenJDK 64-Bit Server VM
- **Memory Configuration**: -Xms1g -Xmx1g
- **Benchmark Framework**: JMH 1.37
- **Platform**: macOS (Darwin)

## Performance Metrics

### Engine Creation Performance

Engine creation represents the fundamental operation of initializing a WebAssembly runtime engine.

| Runtime | Configuration | Throughput (ops/sec) | Performance Notes |
|---------|---------------|---------------------|-------------------|
| JNI     | DEFAULT       | 143,130,006         | Highest performing runtime |
| JNI     | OPTIMIZED     | 141,521,257         | Slight decrease with optimizations |
| JNI     | DEBUG         | 144,222,096         | Surprisingly fast in debug mode |
| Panama  | DEFAULT       | 127,472,232         | ~11% slower than JNI |
| Panama  | OPTIMIZED     | 126,624,670         | Consistent with default |
| Panama  | DEBUG         | 128,278,620         | Stable across configurations |
| AUTO    | DEFAULT       | 2,742,289           | ~98% slower due to runtime detection overhead |
| AUTO    | OPTIMIZED     | 2,816,174           | Slight improvement |
| AUTO    | DEBUG         | 2,697,330           | Consistent with default |

### Key Performance Insights

#### 1. JNI vs Panama Performance
- **JNI Advantage**: JNI implementation shows ~11-13% better throughput than Panama
- **Configuration Stability**: Both runtimes show consistent performance across different configurations
- **Production Recommendation**: JNI for maximum performance, Panama for modern Java integration

#### 2. Runtime Detection Overhead
- **AUTO Mode Impact**: Automatic runtime detection introduces ~98% performance overhead
- **Optimization Strategy**: Use explicit runtime selection for performance-critical applications
- **Development vs Production**: AUTO mode suitable for development, explicit selection for production

#### 3. Configuration Impact
- **Minimal Variance**: Different configurations (DEFAULT, OPTIMIZED, DEBUG) show minimal performance impact
- **Optimization Effectiveness**: OPTIMIZED configuration doesn't significantly improve throughput
- **Debug Mode**: Debug configuration doesn't significantly impact performance

## Performance Optimization Opportunities

### 1. Runtime Selection Optimization
```java
// Avoid AUTO mode in production
WasmRuntime runtime = WasmRuntimeFactory.create(WasmRuntimeType.JNI);

// For maximum performance
WasmRuntime runtime = WasmRuntimeFactory.create(WasmRuntimeType.JNI,
    EngineConfig.builder().optimizationLevel(OptimizationLevel.SPEED).build());
```

### 2. Engine Reuse Strategy
Since engine creation is expensive relative to other operations, implement engine pooling:

```java
// Use singleton pattern for engine instances
private static final WasmEngine ENGINE = WasmEngine.builder()
    .config(EngineConfig.builder().optimizationLevel(OptimizationLevel.SPEED).build())
    .build();
```

### 3. Memory Configuration
- **JVM Heap**: Ensure adequate heap size for WebAssembly module compilation
- **Direct Memory**: Configure off-heap memory for native operations
- **GC Tuning**: Use G1GC for predictable pause times

## Baseline Acceptance Criteria

### Performance Targets
- **Engine Creation**: >100M ops/sec for direct runtime selection
- **Memory Efficiency**: <100MB overhead per engine instance
- **Latency**: <10μs for simple function calls
- **Scalability**: Linear performance up to available CPU cores

### Regression Detection
- **Threshold**: >5% performance decrease triggers investigation
- **Monitoring**: Continuous benchmarking in CI/CD pipeline
- **Alerting**: Automated alerts for performance regressions

## Benchmark Execution

### Running Benchmarks
```bash
# Full benchmark suite
./mvnw -pl wasmtime4j-benchmarks exec:java -Dexec.mainClass="org.openjdk.jmh.Main"

# Specific benchmark
./mvnw -pl wasmtime4j-benchmarks exec:java -Dexec.mainClass="org.openjdk.jmh.Main" \
  -Dexec.args="RuntimeInitializationBenchmark.benchmarkEngineCreation"

# With custom parameters
./mvnw -pl wasmtime4j-benchmarks exec:java -Dexec.mainClass="org.openjdk.jmh.Main" \
  -Dexec.args="-wi 3 -i 5 -f 2 -t 1"
```

### Benchmark Configuration
- **Warmup Iterations**: 3
- **Measurement Iterations**: 5
- **Forks**: 2
- **Threads**: 1
- **Timeout**: 600 seconds

## Historical Performance Data

### Version 1.0.0-SNAPSHOT (Current)
- **Date**: September 21, 2025
- **JNI Engine Creation**: 143.1M ops/sec
- **Panama Engine Creation**: 127.5M ops/sec
- **Memory Usage**: TBD (requires memory profiling)

## Future Performance Targets

### Short-term (Next Release)
- **JNI Performance**: Maintain >140M ops/sec
- **Panama Performance**: Target >130M ops/sec (2% improvement)
- **AUTO Mode**: Reduce overhead to <50% (cache runtime detection)

### Long-term (6 months)
- **Function Call Latency**: <5μs for simple operations
- **Module Compilation**: <10x Wasmtime CLI performance
- **Memory Efficiency**: <50MB overhead per engine
- **Concurrent Performance**: Linear scaling up to 16 cores

## Performance Testing Strategy

### Continuous Integration
- **Every Commit**: Basic performance smoke tests
- **Daily**: Full benchmark suite execution
- **Release**: Comprehensive performance validation

### Load Testing
- **Sustained Load**: 1-hour stress testing at 80% capacity
- **Peak Load**: Burst testing at 150% normal load
- **Memory Pressure**: Testing under memory-constrained conditions

### Profiling Strategy
- **CPU Profiling**: Regular profiling of hot paths
- **Memory Profiling**: Heap and off-heap memory analysis
- **Native Profiling**: Native library performance analysis

---

**Note**: This baseline establishes the foundation for performance monitoring and optimization. Regular updates ensure performance regression detection and continuous improvement tracking.
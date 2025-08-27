# Wasmtime4j Benchmarks

JMH (Java Microbenchmark Harness) benchmarks for performance testing and comparison of Wasmtime4j implementations.

## Overview

This module provides comprehensive performance benchmarks for the Wasmtime4j library, comparing JNI and Panama Foreign Function API implementations across key operation categories:

- **Runtime Initialization**: Engine creation and startup performance
- **Module Operations**: WebAssembly module compilation and instantiation
- **Function Execution**: WebAssembly function call performance
- **Memory Operations**: WebAssembly memory access and manipulation
- **Direct Comparisons**: Side-by-side JNI vs Panama performance analysis

## Quick Start

### Prerequisites

- Java 8+ (JNI benchmarks)
- Java 23+ (Panama benchmarks)
- Maven 3.6+ or use included Maven wrapper

### Running Benchmarks

#### Using Shell Scripts (Linux/macOS)

```bash
# Run all benchmarks with standard profile
./run-benchmarks.sh

# Run specific benchmark category
./run-benchmarks.sh runtime quick
./run-benchmarks.sh comparison production

# Custom configuration
./run-benchmarks.sh all comprehensive --output results.json --iterations 15
```

#### Using Batch Script (Windows)

```cmd
REM Run all benchmarks with standard profile
run-benchmarks.bat

REM Run specific benchmark category
run-benchmarks.bat runtime quick
run-benchmarks.bat comparison production

REM Custom configuration
run-benchmarks.bat all comprehensive --output results.json --iterations 15
```

#### Using Maven

```bash
# Build benchmarks
./mvnw clean package -pl wasmtime4j-benchmarks

# Run with exec plugin (basic)
./mvnw exec:java -pl wasmtime4j-benchmarks

# Run specific profiles
./mvnw exec:java -pl wasmtime4j-benchmarks -Pruntime-benchmarks
./mvnw exec:java -pl wasmtime4j-benchmarks -Pcomparison-benchmarks
```

#### Direct JAR Execution

```bash
# Build the benchmark JAR
./mvnw clean package -pl wasmtime4j-benchmarks

# Run benchmarks directly
java -cp wasmtime4j-benchmarks/target/wasmtime4j-benchmarks.jar \
  ai.tegmentum.wasmtime4j.benchmarks.BenchmarkRunner [options]
```

## Benchmark Categories

### Runtime Initialization (`runtime`)

Measures performance of:
- Engine creation time
- Runtime initialization overhead
- Configuration parsing performance
- Multiple engine creation scenarios

### Module Operations (`module`)

Measures performance of:
- WebAssembly module compilation
- Module validation
- Instance creation
- Module caching efficiency
- Large module handling

### Function Execution (`function`)

Measures performance of:
- Function call overhead
- Parameter marshalling
- Return value handling
- Recursive function execution
- Batch function calls

### Memory Operations (`memory`)

Measures performance of:
- Memory allocation/deallocation
- Memory read/write operations
- Bulk memory operations
- Memory growth operations
- Cross-boundary data marshalling

### Comparison Benchmarks (`comparison`)

Direct performance comparisons between:
- JNI vs Panama implementations
- Initialization overhead comparison
- Throughput comparison
- Resource utilization comparison
- Scalability comparison

## Benchmark Profiles

### Quick Profile (`quick`)
- **Purpose**: Development and testing
- **Configuration**: 1 iteration, 1 warmup, 1 fork
- **Duration**: ~30 seconds
- **Use Case**: Quick feedback during development

### Standard Profile (`standard`)
- **Purpose**: Regular performance monitoring
- **Configuration**: 5 iterations, 3 warmup, 2 forks
- **Duration**: ~2-3 minutes
- **Use Case**: CI/CD performance regression testing

### Production Profile (`production`)
- **Purpose**: Official performance reports
- **Configuration**: 10 iterations, 5 warmup, 3 forks
- **Duration**: ~5-8 minutes
- **Use Case**: Release performance documentation

### Comprehensive Profile (`comprehensive`)
- **Purpose**: Detailed performance analysis
- **Configuration**: 15 iterations, 8 warmup, 5 forks
- **Duration**: ~10-15 minutes
- **Use Case**: Performance optimization and analysis

## Command Line Options

The `BenchmarkRunner` supports various command line options:

```bash
java -cp wasmtime4j-benchmarks.jar BenchmarkRunner [options] [category]

Categories:
  all         - Run all benchmarks (default)
  runtime     - Runtime initialization benchmarks
  module      - Module operation benchmarks  
  function    - Function execution benchmarks
  memory      - Memory operation benchmarks
  comparison  - JNI vs Panama comparison benchmarks

Options:
  --profile <profile>     Benchmark profile: [QUICK, STANDARD, PRODUCTION, COMPREHENSIVE]
  --iterations <n>        Number of measurement iterations
  --warmup <n>            Number of warmup iterations
  --forks <n>             Number of benchmark forks
  --output <file>         Output file for detailed results (JSON format)
  --help                  Show help message
```

## Output Files

Benchmark runs generate several output files in the `benchmark-results/` directory:

- **JSON Results**: Detailed JMH results in JSON format
- **Summary Report**: Human-readable performance summary  
- **Log File**: Complete execution log
- **System Info**: Hardware and software environment details

Example output structure:
```
benchmark-results/
├── benchmark_all_standard_20240827_143022.json
├── benchmark_all_standard_20240827_143022_summary.txt
├── benchmark_all_standard_20240827_143022.log
└── system_info_20240827_143022.txt
```

## Performance Metrics

All benchmarks use **throughput mode** measuring operations per second:

- **Score**: Operations per second (higher is better)
- **Error**: 99.9% confidence interval
- **Unit**: ops/sec (operations per second)

## Development Notes

### Mock Implementations

The benchmark module uses mock implementations rather than actual Wasmtime bindings to:
- Enable testing without native dependencies
- Focus on Java-side overhead measurement
- Simulate realistic workload patterns
- Ensure consistent cross-platform behavior

### JIT Warm-up

All benchmarks include proper JIT warm-up phases to ensure:
- Consistent performance measurements
- Realistic production-like performance
- Elimination of cold-start effects

### Memory Pressure Testing

Several benchmarks include memory pressure scenarios to test:
- Performance under memory constraints
- Garbage collection impact
- Memory allocation patterns

## Integration with CI/CD

The benchmarks are designed for CI/CD integration:

```bash
# Quick regression testing
./run-benchmarks.sh all quick --output ci-results.json

# Performance monitoring  
./run-benchmarks.sh comparison standard --output nightly-results.json
```

## Troubleshooting

### Common Issues

1. **OutOfMemoryError**: Increase heap size with `-Xmx4g`
2. **Slow Performance**: Use quicker profile for development
3. **Java Version Issues**: Ensure Java 23+ for Panama benchmarks
4. **Build Failures**: Run `./mvnw clean compile` first

### Environment Variables

Useful environment variables for customization:

```bash
export JVM_ARGS="-Xms2g -Xmx4g -XX:+UseG1GC"
export BENCHMARK_THREADS=4
export BENCHMARK_OUTPUT_DIR="custom-results"
```

## Contributing

When adding new benchmarks:

1. Extend appropriate base classes (`BenchmarkBase`)
2. Follow JMH annotation patterns
3. Include proper setup/teardown methods
4. Add documentation for new benchmark categories
5. Test with multiple profiles
6. Ensure cross-platform compatibility

## Performance Expectations

Expected performance characteristics (vary by hardware):

- **JNI**: Lower overhead, consistent performance
- **Panama**: Slightly higher overhead, better type safety
- **Function Calls**: 1M-10M ops/sec depending on complexity
- **Memory Operations**: 100K-1M ops/sec depending on size
- **Module Compilation**: 1K-10K ops/sec depending on module size

## License

This benchmark module is part of the Wasmtime4j project and follows the same Apache 2.0 license.
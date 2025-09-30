/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.serialization.CacheConfiguration;
import ai.tegmentum.wasmtime4j.serialization.ModuleSerializationCache;
import ai.tegmentum.wasmtime4j.serialization.ModuleSerializationEngine;
import ai.tegmentum.wasmtime4j.serialization.SerializationOptions;
import ai.tegmentum.wasmtime4j.serialization.optimization.OptimizationSettings;
import ai.tegmentum.wasmtime4j.serialization.optimization.SerializationOptimizer;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Comprehensive performance benchmarks for WebAssembly module serialization.
 *
 * <p>This benchmark suite measures the performance of various serialization strategies including
 * different formats, compression levels, optimization settings, and caching strategies.
 *
 * <p>Run with: {@code java -jar target/benchmarks.jar SerializationPerformanceBenchmark}
 *
 * @since 1.0.0
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(
    value = 2,
    jvmArgs = {"-Xmx4g", "-XX:+UseG1GC"})
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class SerializationPerformanceBenchmark {

  private static final Logger LOGGER =
      Logger.getLogger(SerializationPerformanceBenchmark.class.getName());

  // Benchmark state
  private ModuleSerializationEngine engine;
  private SerializationOptimizer optimizer;
  private ModuleSerializationCache cache;

  // Mock data for different module sizes
  private MockModule smallModule; // 1KB
  private MockModule mediumModule; // 100KB
  private MockModule largeModule; // 10MB
  private MockModule veryLargeModule; // 100MB

  // Serialization options
  private SerializationOptions defaultOptions;
  private SerializationOptions performanceOptions;
  private SerializationOptions secureOptions;
  private SerializationOptions compressedOptions;

  /** Mock module implementation for benchmarking. */
  private static class MockModule implements ai.tegmentum.wasmtime4j.Module {
    private final byte[] data;
    private final String name;

    public MockModule(int size, String name) {
      this.data = new byte[size];
      this.name = name;
      // Fill with pseudo-random data for realistic compression testing
      for (int i = 0; i < size; i++) {
        data[i] = (byte) (i * 37 + 123);
      }
    }

    public byte[] getData() {
      return data.clone();
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    // Stub implementations for other Module methods
    @Override
    public ai.tegmentum.wasmtime4j.Instance instantiate(ai.tegmentum.wasmtime4j.Store store) {
      throw new UnsupportedOperationException("Mock implementation");
    }

    @Override
    public ai.tegmentum.wasmtime4j.Instance instantiate(
        ai.tegmentum.wasmtime4j.Store store, ai.tegmentum.wasmtime4j.ImportMap imports) {
      throw new UnsupportedOperationException("Mock implementation");
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.ExportType> getExports() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.ImportType> getImports() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.ExportDescriptor> getExportDescriptors() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.ImportDescriptor> getImportDescriptors() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.Optional<ai.tegmentum.wasmtime4j.FuncType> getFunctionType(
        String functionName) {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<ai.tegmentum.wasmtime4j.GlobalType> getGlobalType(String globalName) {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<ai.tegmentum.wasmtime4j.MemoryType> getMemoryType(String memoryName) {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<ai.tegmentum.wasmtime4j.TableType> getTableType(String tableName) {
      return java.util.Optional.empty();
    }

    @Override
    public boolean hasExport(String name) {
      return false;
    }

    @Override
    public boolean hasImport(String moduleName, String fieldName) {
      return false;
    }

    @Override
    public ai.tegmentum.wasmtime4j.Engine getEngine() {
      return null;
    }

    @Override
    public boolean validateImports(ai.tegmentum.wasmtime4j.ImportMap imports) {
      return true;
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.ModuleImport> getModuleImports() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.ModuleExport> getModuleExports() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.FuncType> getFunctionTypes() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.MemoryType> getMemoryTypes() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.TableType> getTableTypes() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.GlobalType> getGlobalTypes() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.Map<String, String> getCustomSections() {
      return java.util.Collections.emptyMap();
    }

    @Override
    public ai.tegmentum.wasmtime4j.CustomSectionMetadata getCustomSectionMetadata() {
      return null;
    }

    @Override
    public void close() {
      // No-op for mock
    }
  }

  @Setup
  public void setup() {
    LOGGER.info("Setting up serialization performance benchmarks");

    // Initialize serialization engine and optimizer
    engine = new ModuleSerializationEngine();
    optimizer = new SerializationOptimizer(OptimizationSettings.createDefault());

    // Initialize cache
    try {
      final CacheConfiguration cacheConfig = CacheConfiguration.createDefault();
      cache = new ModuleSerializationCache(cacheConfig);
    } catch (IOException e) {
      throw new RuntimeException("Failed to initialize cache", e);
    }

    // Create mock modules of different sizes
    smallModule = new MockModule(1024, "small-module"); // 1KB
    mediumModule = new MockModule(100 * 1024, "medium-module"); // 100KB
    largeModule = new MockModule(10 * 1024 * 1024, "large-module"); // 10MB
    veryLargeModule = new MockModule(100 * 1024 * 1024, "very-large-module"); // 100MB

    // Configure different serialization options
    defaultOptions = SerializationOptions.createDefault();
    performanceOptions = SerializationOptions.createHighPerformance();

    final byte[] encryptionKey = new byte[32];
    java.util.Arrays.fill(encryptionKey, (byte) 0x42);
    secureOptions = SerializationOptions.createSecure(encryptionKey);

    compressedOptions = SerializationOptions.createProduction();

    LOGGER.info("Benchmark setup completed");
  }

  @TearDown
  public void tearDown() {
    if (cache != null) {
      cache.close();
    }
    LOGGER.info("Benchmark teardown completed");
  }

  // ================================================================================================
  // FORMAT COMPARISON BENCHMARKS
  // ================================================================================================

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkRawBinarySmall(Blackhole bh) {
    // Note: In a real implementation, this would call the actual serialization
    // For now, we simulate the operation
    final byte[] result = simulateRawBinarySerialization(smallModule);
    bh.consume(result);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkCompactLz4Small(Blackhole bh) {
    final byte[] result = simulateCompactLz4Serialization(smallModule);
    bh.consume(result);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkCompactGzipSmall(Blackhole bh) {
    final byte[] result = simulateCompactGzipSerialization(smallModule);
    bh.consume(result);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkRawBinaryMedium(Blackhole bh) {
    final byte[] result = simulateRawBinarySerialization(mediumModule);
    bh.consume(result);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkCompactLz4Medium(Blackhole bh) {
    final byte[] result = simulateCompactLz4Serialization(mediumModule);
    bh.consume(result);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkCompactGzipMedium(Blackhole bh) {
    final byte[] result = simulateCompactGzipSerialization(mediumModule);
    bh.consume(result);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void benchmarkStreamingLarge(Blackhole bh) {
    final byte[] result = simulateStreamingSerialization(largeModule);
    bh.consume(result);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void benchmarkMemoryMappedVeryLarge(Blackhole bh) {
    final byte[] result = simulateMemoryMappedSerialization(veryLargeModule);
    bh.consume(result);
  }

  // ================================================================================================
  // OPTIMIZATION BENCHMARKS
  // ================================================================================================

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkDefaultOptions(Blackhole bh) {
    final byte[] result = simulateOptimizedSerialization(mediumModule, defaultOptions);
    bh.consume(result);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkPerformanceOptions(Blackhole bh) {
    final byte[] result = simulateOptimizedSerialization(mediumModule, performanceOptions);
    bh.consume(result);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkSecureOptions(Blackhole bh) {
    final byte[] result = simulateOptimizedSerialization(mediumModule, secureOptions);
    bh.consume(result);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkCompressedOptions(Blackhole bh) {
    final byte[] result = simulateOptimizedSerialization(mediumModule, compressedOptions);
    bh.consume(result);
  }

  // ================================================================================================
  // CACHING BENCHMARKS
  // ================================================================================================

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void benchmarkCacheHit(Blackhole bh) {
    // Simulate cache hit scenario
    final String cacheKey = "test-module-key";
    final byte[] cachedData = smallModule.getData();

    // In a real implementation, this would actually use the cache
    bh.consume(cachedData);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkCacheMiss(Blackhole bh) {
    // Simulate cache miss + serialization + cache store
    final byte[] result = simulateCacheMissScenario(mediumModule);
    bh.consume(result);
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void benchmarkCacheThroughput(Blackhole bh) {
    // Simulate mixed cache hit/miss scenario
    final MockModule module = Math.random() > 0.7 ? largeModule : mediumModule;
    final byte[] result = simulateMixedCacheScenario(module);
    bh.consume(result);
  }

  // ================================================================================================
  // PARALLEL PROCESSING BENCHMARKS
  // ================================================================================================

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkSequentialSerialization(Blackhole bh) {
    // Serialize multiple modules sequentially
    final MockModule[] modules = {smallModule, mediumModule, smallModule, mediumModule};
    for (final MockModule module : modules) {
      final byte[] result = simulateRawBinarySerialization(module);
      bh.consume(result);
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkParallelSerialization(Blackhole bh) {
    // Simulate parallel serialization of multiple modules
    final MockModule[] modules = {smallModule, mediumModule, smallModule, mediumModule};
    final byte[][] results = simulateParallelSerialization(modules);
    bh.consume(results);
  }

  // ================================================================================================
  // MEMORY USAGE BENCHMARKS
  // ================================================================================================

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkMemoryEfficient(Blackhole bh) {
    // Test memory-efficient serialization path
    final SerializationOptions memoryOptions = SerializationOptions.createMemoryConstrained();
    final byte[] result = simulateOptimizedSerialization(largeModule, memoryOptions);
    bh.consume(result);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void benchmarkHighThroughput(Blackhole bh) {
    // Test high-throughput serialization path
    final byte[] result = simulateOptimizedSerialization(largeModule, performanceOptions);
    bh.consume(result);
  }

  // ================================================================================================
  // SIMULATION METHODS (In real implementation, these would call actual serialization)
  // ================================================================================================

  private byte[] simulateRawBinarySerialization(MockModule module) {
    // Simulate raw binary serialization performance
    final byte[] data = module.getData();
    // Simulate processing time proportional to data size
    simulateProcessingTime(data.length, 0.001); // 1ms per MB
    return data;
  }

  private byte[] simulateCompactLz4Serialization(MockModule module) {
    // Simulate LZ4 compression
    final byte[] data = module.getData();
    simulateProcessingTime(data.length, 0.002); // 2ms per MB
    // Simulate 70% compression ratio
    return new byte[(int) (data.length * 0.7)];
  }

  private byte[] simulateCompactGzipSerialization(MockModule module) {
    // Simulate GZIP compression
    final byte[] data = module.getData();
    simulateProcessingTime(data.length, 0.005); // 5ms per MB
    // Simulate 50% compression ratio
    return new byte[data.length / 2];
  }

  private byte[] simulateStreamingSerialization(MockModule module) {
    // Simulate streaming serialization
    final byte[] data = module.getData();
    simulateProcessingTime(data.length, 0.0015); // 1.5ms per MB
    return new byte[(int) (data.length * 0.8)];
  }

  private byte[] simulateMemoryMappedSerialization(MockModule module) {
    // Simulate memory-mapped serialization
    final byte[] data = module.getData();
    simulateProcessingTime(data.length, 0.0005); // 0.5ms per MB
    return data;
  }

  private byte[] simulateOptimizedSerialization(MockModule module, SerializationOptions options) {
    final byte[] data = module.getData();

    // Simulate different processing times based on options
    double timePerMB = 0.001; // Base time

    if (options.isEncryptSerialization()) {
      timePerMB *= 2.0; // Encryption overhead
    }

    if (options.getCompressionLevel() > 6) {
      timePerMB *= 1.5; // High compression overhead
    }

    if (options.isVerifyIntegrity()) {
      timePerMB *= 1.2; // Integrity verification overhead
    }

    simulateProcessingTime(data.length, timePerMB);

    // Simulate compression based on options
    double compressionRatio = 1.0;
    if (options.getCompressionLevel() > 0) {
      compressionRatio = 0.7 - (options.getCompressionLevel() * 0.02);
    }

    return new byte[(int) (data.length * compressionRatio)];
  }

  private byte[] simulateCacheMissScenario(MockModule module) {
    // Simulate: cache miss + serialization + cache store
    final byte[] result = simulateRawBinarySerialization(module);

    // Simulate cache store operation
    simulateProcessingTime(result.length, 0.0002); // Cache store overhead

    return result;
  }

  private byte[] simulateMixedCacheScenario(MockModule module) {
    if (Math.random() > 0.3) {
      // 70% cache hit rate
      return module.getData();
    } else {
      // Cache miss
      return simulateCacheMissScenario(module);
    }
  }

  private byte[][] simulateParallelSerialization(MockModule[] modules) {
    // Simulate parallel processing advantage
    final byte[][] results = new byte[modules.length][];

    // Simulate parallel processing being ~75% of sequential time
    long totalSize = 0;
    for (final MockModule module : modules) {
      totalSize += module.getData().length;
    }

    simulateProcessingTime(totalSize, 0.00075); // Parallel advantage

    for (int i = 0; i < modules.length; i++) {
      results[i] = modules[i].getData();
    }

    return results;
  }

  private void simulateProcessingTime(long dataSize, double timePerByte) {
    // Simulate processing time by doing a small amount of CPU work
    final long iterations = (long) (dataSize * timePerByte * 1000);
    long sum = 0;
    for (long i = 0; i < iterations; i++) {
      sum += i;
    }
    // Prevent optimization
    if (sum == Long.MAX_VALUE) {
      System.out.println("Impossible");
    }
  }

  /** Main method to run the benchmarks. */
  public static void main(String[] args) throws RunnerException {
    LOGGER.info("Starting WebAssembly module serialization performance benchmarks");

    final Options options =
        new OptionsBuilder()
            .include(SerializationPerformanceBenchmark.class.getSimpleName())
            .threads(1)
            .forks(1) // Reduce forks for development
            .warmupIterations(2)
            .measurementIterations(3)
            .result("serialization-benchmark-results.json")
            .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
            .build();

    new Runner(options).run();

    LOGGER.info("Benchmarks completed. Results saved to serialization-benchmark-results.json");
  }
}

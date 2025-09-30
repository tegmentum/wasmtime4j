package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.CompilationPriority;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.InstantiationConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.NetworkStreamingCompiler;
import ai.tegmentum.wasmtime4j.NetworkStreamingConfig;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.StreamingCompiler;
import ai.tegmentum.wasmtime4j.StreamingConfig;
import ai.tegmentum.wasmtime4j.StreamingInstantiator;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * JMH benchmark for comparing streaming vs traditional WebAssembly compilation performance.
 *
 * <p>This benchmark measures compilation time, instantiation time, memory usage, and throughput for
 * different compilation strategies to validate the benefits of streaming compilation.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(
    value = 1,
    jvmArgs = {"-Xms2g", "-Xmx4g"})
@Warmup(iterations = 3, time = 10)
@Measurement(iterations = 5, time = 10)
public class StreamingCompilationBenchmark {

  @Param({"1KB", "100KB", "1MB", "10MB"})
  private String moduleSize;

  @Param({"SPEED", "SIZE", "BALANCED"})
  private String optimizationLevel;

  @Param({"NORMAL", "HIGH"})
  private String compilationPriority;

  private Engine engine;
  private Store store;
  private byte[] wasmBytecode;
  private StreamingConfig streamingConfig;
  private NetworkStreamingConfig networkStreamingConfig;
  private InstantiationConfig instantiationConfig;

  @Setup
  public void setup() throws WasmException {
    // Create engine and store
    engine = Engine.create();
    store = engine.createStore();

    // Generate WebAssembly bytecode based on moduleSize parameter
    wasmBytecode = generateWasmBytecode(parseSize(moduleSize));

    // Create configurations
    streamingConfig =
        StreamingConfig.builder()
            .optimizationLevel(OptimizationLevel.valueOf(optimizationLevel))
            .priority(CompilationPriority.valueOf(compilationPriority))
            .bufferSize(64 * 1024) // 64KB buffer
            .progressiveValidation(true)
            .hotFunctionDetection(true)
            .incrementalCaching(true)
            .build();

    networkStreamingConfig =
        NetworkStreamingConfig.builder(streamingConfig)
            .segmentSize(256 * 1024) // 256KB segments
            .maxConcurrentConnections(4)
            .adaptiveStreaming(true)
            .networkProbing(true)
            .build();

    instantiationConfig =
        InstantiationConfig.builder()
            .lazyFunctionCompilation(true)
            .hotFunctionPriority(true)
            .progressiveMemoryAllocation(true)
            .instancePooling(true)
            .build();
  }

  @TearDown
  public void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  /**
   * Benchmark traditional synchronous compilation.
   *
   * <p>This benchmark measures the time to compile a WebAssembly module using the traditional
   * synchronous approach.
   */
  @Benchmark
  public Module benchmarkTraditionalCompilation() throws WasmException {
    return engine.compileModule(wasmBytecode);
  }

  /**
   * Benchmark streaming compilation from input stream.
   *
   * <p>This benchmark measures the time to compile a WebAssembly module using streaming compilation
   * from an input stream.
   */
  @Benchmark
  public Module benchmarkStreamingCompilation() throws Exception {
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(wasmBytecode);
      CompletableFuture<Module> future = compiler.compileStreaming(inputStream, streamingConfig);
      return future.get();
    }
  }

  /**
   * Benchmark streaming compilation with instantiation.
   *
   * <p>This benchmark measures the time to compile and prepare a WebAssembly module for fast
   * instantiation using streaming compilation.
   */
  @Benchmark
  public StreamingInstantiator benchmarkStreamingCompilationWithInstantiation() throws Exception {
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(wasmBytecode);
      CompletableFuture<StreamingInstantiator> future =
          compiler.compileStreamingWithInstantiation(
              inputStream, streamingConfig, instantiationConfig);
      return future.get();
    }
  }

  /**
   * Benchmark network streaming compilation simulation.
   *
   * <p>This benchmark simulates network streaming compilation by creating segments and processing
   * them progressively.
   */
  @Benchmark
  public Module benchmarkNetworkStreamingCompilation() throws Exception {
    if (engine.createStreamingCompiler() instanceof NetworkStreamingCompiler) {
      try (NetworkStreamingCompiler compiler =
          (NetworkStreamingCompiler) engine.createStreamingCompiler()) {
        // Simulate network URL (would normally be a real URL)
        URI simulatedUrl = URI.create("http://example.com/module.wasm");
        CompletableFuture<Module> future =
            compiler.compileFromUrl(simulatedUrl, networkStreamingConfig);
        return future.get();
      }
    } else {
      // Fallback to regular streaming compilation
      return benchmarkStreamingCompilation();
    }
  }

  /**
   * Benchmark traditional instantiation after compilation.
   *
   * <p>This benchmark measures the time to instantiate a traditionally compiled module.
   */
  @Benchmark
  public ai.tegmentum.wasmtime4j.Instance benchmarkTraditionalInstantiation() throws WasmException {
    Module module = engine.compileModule(wasmBytecode);
    try {
      return module.instantiate(store);
    } finally {
      module.close();
    }
  }

  /**
   * Benchmark streaming instantiation.
   *
   * <p>This benchmark measures the time to instantiate a module using streaming instantiation.
   */
  @Benchmark
  public ai.tegmentum.wasmtime4j.Instance benchmarkStreamingInstantiation() throws Exception {
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(wasmBytecode);
      CompletableFuture<StreamingInstantiator> instantiatorFuture =
          compiler.compileStreamingWithInstantiation(
              inputStream, streamingConfig, instantiationConfig);

      StreamingInstantiator instantiator = instantiatorFuture.get();
      try {
        CompletableFuture<ai.tegmentum.wasmtime4j.Instance> instanceFuture =
            instantiator.instantiateStreaming(store, instantiationConfig);
        return instanceFuture.get();
      } finally {
        instantiator.close();
      }
    }
  }

  /**
   * Benchmark memory usage during compilation.
   *
   * <p>This benchmark measures peak memory usage during different compilation strategies.
   */
  @Benchmark
  public CompilationMemoryMetrics benchmarkMemoryUsage() throws Exception {
    Runtime runtime = Runtime.getRuntime();

    // Measure traditional compilation memory usage
    long beforeTraditional = runtime.totalMemory() - runtime.freeMemory();
    Module traditionalModule = engine.compileModule(wasmBytecode);
    long peakTraditional = runtime.totalMemory() - runtime.freeMemory();
    traditionalModule.close();

    // Force garbage collection
    System.gc();
    Thread.sleep(100);

    // Measure streaming compilation memory usage
    long beforeStreaming = runtime.totalMemory() - runtime.freeMemory();
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(wasmBytecode);
      CompletableFuture<Module> future = compiler.compileStreaming(inputStream, streamingConfig);
      Module streamingModule = future.get();
      long peakStreaming = runtime.totalMemory() - runtime.freeMemory();
      streamingModule.close();

      return new CompilationMemoryMetrics(
          peakTraditional - beforeTraditional, peakStreaming - beforeStreaming);
    }
  }

  /**
   * Benchmark compilation throughput.
   *
   * <p>This benchmark measures bytes compiled per second for different compilation strategies.
   */
  @Benchmark
  public CompilationThroughputMetrics benchmarkCompilationThroughput() throws Exception {
    long startTime, endTime;

    // Measure traditional compilation throughput
    startTime = System.nanoTime();
    Module traditionalModule = engine.compileModule(wasmBytecode);
    endTime = System.nanoTime();
    long traditionalTimeNanos = endTime - startTime;
    traditionalModule.close();

    // Measure streaming compilation throughput
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(wasmBytecode);

      startTime = System.nanoTime();
      CompletableFuture<Module> future = compiler.compileStreaming(inputStream, streamingConfig);
      Module streamingModule = future.get();
      endTime = System.nanoTime();
      long streamingTimeNanos = endTime - startTime;
      streamingModule.close();

      double traditionalThroughput =
          (double) wasmBytecode.length / (traditionalTimeNanos / 1_000_000_000.0);
      double streamingThroughput =
          (double) wasmBytecode.length / (streamingTimeNanos / 1_000_000_000.0);

      return new CompilationThroughputMetrics(traditionalThroughput, streamingThroughput);
    }
  }

  private int parseSize(final String sizeStr) {
    if (sizeStr.endsWith("KB")) {
      return Integer.parseInt(sizeStr.substring(0, sizeStr.length() - 2)) * 1024;
    } else if (sizeStr.endsWith("MB")) {
      return Integer.parseInt(sizeStr.substring(0, sizeStr.length() - 2)) * 1024 * 1024;
    } else {
      return Integer.parseInt(sizeStr);
    }
  }

  private byte[] generateWasmBytecode(final int targetSize) {
    // Generate a valid WebAssembly module with approximately the target size
    // This is a simplified implementation - a real benchmark would use more sophisticated module
    // generation

    final byte[] wasmHeader = {
      0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00
    }; // WASM magic + version

    // Calculate remaining size for padding
    int remainingSize =
        Math.max(0, targetSize - wasmHeader.length - 100); // Reserve space for sections

    // Create a simple module structure with padding
    byte[] module = new byte[wasmHeader.length + remainingSize + 100];
    System.arraycopy(wasmHeader, 0, module, 0, wasmHeader.length);

    // Add minimal sections and padding to reach target size
    // This is a simplified approach - real modules would have proper section structure
    for (int i = wasmHeader.length; i < module.length; i++) {
      module[i] = (byte) (i % 256);
    }

    return module;
  }

  /** Metrics for compilation memory usage comparison. */
  public static class CompilationMemoryMetrics {
    public final long traditionalMemoryBytes;
    public final long streamingMemoryBytes;

    public CompilationMemoryMetrics(
        final long traditionalMemoryBytes, final long streamingMemoryBytes) {
      this.traditionalMemoryBytes = traditionalMemoryBytes;
      this.streamingMemoryBytes = streamingMemoryBytes;
    }

    @Override
    public String toString() {
      return String.format(
          "CompilationMemoryMetrics{traditional=%d bytes, streaming=%d bytes, improvement=%.2f%%}",
          traditionalMemoryBytes,
          streamingMemoryBytes,
          (double) (traditionalMemoryBytes - streamingMemoryBytes)
              / traditionalMemoryBytes
              * 100.0);
    }
  }

  /** Metrics for compilation throughput comparison. */
  public static class CompilationThroughputMetrics {
    public final double traditionalBytesPerSecond;
    public final double streamingBytesPerSecond;

    public CompilationThroughputMetrics(
        final double traditionalBytesPerSecond, final double streamingBytesPerSecond) {
      this.traditionalBytesPerSecond = traditionalBytesPerSecond;
      this.streamingBytesPerSecond = streamingBytesPerSecond;
    }

    @Override
    public String toString() {
      return String.format(
          "CompilationThroughputMetrics{traditional=%.2f MB/s, streaming=%.2f MB/s,"
              + " improvement=%.2f%%}",
          traditionalBytesPerSecond / (1024.0 * 1024.0),
          streamingBytesPerSecond / (1024.0 * 1024.0),
          (streamingBytesPerSecond - traditionalBytesPerSecond)
              / traditionalBytesPerSecond
              * 100.0);
    }
  }
}

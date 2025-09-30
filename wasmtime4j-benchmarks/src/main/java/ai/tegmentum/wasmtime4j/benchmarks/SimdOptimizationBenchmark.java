package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Comprehensive SIMD optimization benchmarking for wasmtime4j.
 *
 * <p>This benchmark suite tests various SIMD operations across different platforms and optimization
 * levels to validate performance improvements.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(
    value = 2,
    jvmArgs = {"-Xms2g", "-Xmx2g"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class SimdOptimizationBenchmark {

  private static final int VECTOR_SIZE = 4;
  private static final int BENCHMARK_DATA_SIZE = 1024;
  private static final Random RANDOM = new Random(42);

  // Test data
  private int[] intData1;
  private int[] intData2;
  private float[] floatData1;
  private float[] floatData2;

  // WebAssembly instances for different SIMD scenarios
  private WasmRuntime runtime;
  private Instance simdInstance;
  private Instance scalarInstance;

  // Benchmark configurations
  @Param({"JNI", "PANAMA"})
  private String runtimeType;

  @Param({"SSE41", "AVX2", "AVX512", "NEON", "SCALAR"})
  private String simdLevel;

  @Setup(Level.Trial)
  public void setupTrial() throws Exception {
    // Initialize runtime based on parameter
    EngineConfig engineConfig =
        EngineConfig.builder()
            .withFeature(WasmFeature.SIMD, true)
            .withFeature(WasmFeature.MULTI_VALUE, true)
            .withFeature(WasmFeature.BULK_MEMORY, true)
            .withOptimizationLevel(3)
            .withRuntimeProperty("wasmtime4j.runtime", runtimeType.toLowerCase())
            .build();

    runtime = WasmRuntime.builder().withEngineConfig(engineConfig).build();

    // Load SIMD-optimized WebAssembly module
    byte[] simdWasm = loadSimdTestModule();
    Module simdModule = Module.fromBinary(runtime, simdWasm);
    simdInstance = Instance.create(runtime, simdModule);

    // Load scalar fallback module for comparison
    byte[] scalarWasm = loadScalarTestModule();
    Module scalarModule = Module.fromBinary(runtime, scalarWasm);
    scalarInstance = Instance.create(runtime, scalarModule);

    System.out.println(
        "Initialized SIMD benchmark with runtime: " + runtimeType + ", SIMD level: " + simdLevel);
  }

  @Setup(Level.Iteration)
  public void setupIteration() {
    // Generate test data for each iteration
    intData1 = new int[BENCHMARK_DATA_SIZE];
    intData2 = new int[BENCHMARK_DATA_SIZE];
    floatData1 = new float[BENCHMARK_DATA_SIZE];
    floatData2 = new float[BENCHMARK_DATA_SIZE];

    for (int i = 0; i < BENCHMARK_DATA_SIZE; i++) {
      intData1[i] = RANDOM.nextInt(1000);
      intData2[i] = RANDOM.nextInt(1000);
      floatData1[i] = RANDOM.nextFloat() * 1000.0f;
      floatData2[i] = RANDOM.nextFloat() * 1000.0f;
    }
  }

  @TearDown(Level.Trial)
  public void teardownTrial() throws Exception {
    if (simdInstance != null) {
      simdInstance.close();
    }
    if (scalarInstance != null) {
      scalarInstance.close();
    }
    if (runtime != null) {
      runtime.close();
    }
  }

  /** Benchmark SIMD vector addition operations. */
  @Benchmark
  public void benchmarkSimdVectorAddition(Blackhole blackhole) throws Exception {
    int iterations = BENCHMARK_DATA_SIZE / VECTOR_SIZE;

    for (int i = 0; i < iterations; i++) {
      int offset = i * VECTOR_SIZE;

      // Create V128 vectors from test data
      WasmValue[] args = new WasmValue[8];
      for (int j = 0; j < VECTOR_SIZE; j++) {
        args[j] = WasmValue.i32(intData1[offset + j]);
        args[j + VECTOR_SIZE] = WasmValue.i32(intData2[offset + j]);
      }

      // Call SIMD addition function
      WasmValue[] result = simdInstance.call("simd_add_i32x4", args);
      blackhole.consume(result);
    }
  }

  /** Benchmark scalar addition for comparison. */
  @Benchmark
  public void benchmarkScalarVectorAddition(Blackhole blackhole) throws Exception {
    int iterations = BENCHMARK_DATA_SIZE / VECTOR_SIZE;

    for (int i = 0; i < iterations; i++) {
      int offset = i * VECTOR_SIZE;

      for (int j = 0; j < VECTOR_SIZE; j++) {
        WasmValue[] args = {
          WasmValue.i32(intData1[offset + j]), WasmValue.i32(intData2[offset + j])
        };

        WasmValue result = scalarInstance.call("scalar_add_i32", args)[0];
        blackhole.consume(result);
      }
    }
  }

  /** Benchmark SIMD floating-point multiplication. */
  @Benchmark
  public void benchmarkSimdFloatMultiplication(Blackhole blackhole) throws Exception {
    int iterations = BENCHMARK_DATA_SIZE / VECTOR_SIZE;

    for (int i = 0; i < iterations; i++) {
      int offset = i * VECTOR_SIZE;

      WasmValue[] args = new WasmValue[8];
      for (int j = 0; j < VECTOR_SIZE; j++) {
        args[j] = WasmValue.f32(floatData1[offset + j]);
        args[j + VECTOR_SIZE] = WasmValue.f32(floatData2[offset + j]);
      }

      WasmValue[] result = simdInstance.call("simd_mul_f32x4", args);
      blackhole.consume(result);
    }
  }

  /** Benchmark SIMD dot product operation. */
  @Benchmark
  public void benchmarkSimdDotProduct(Blackhole blackhole) throws Exception {
    int iterations = BENCHMARK_DATA_SIZE / VECTOR_SIZE;

    for (int i = 0; i < iterations; i++) {
      int offset = i * VECTOR_SIZE;

      WasmValue[] args = new WasmValue[8];
      for (int j = 0; j < VECTOR_SIZE; j++) {
        args[j] = WasmValue.f32(floatData1[offset + j]);
        args[j + VECTOR_SIZE] = WasmValue.f32(floatData2[offset + j]);
      }

      WasmValue result = simdInstance.call("simd_dot_product", args)[0];
      blackhole.consume(result);
    }
  }

  /** Benchmark SIMD FMA (fused multiply-add) operations. */
  @Benchmark
  public void benchmarkSimdFMA(Blackhole blackhole) throws Exception {
    int iterations = BENCHMARK_DATA_SIZE / VECTOR_SIZE;

    for (int i = 0; i < iterations; i++) {
      int offset = i * VECTOR_SIZE;

      WasmValue[] args = new WasmValue[12];
      for (int j = 0; j < VECTOR_SIZE; j++) {
        args[j] = WasmValue.f32(floatData1[offset + j]);
        args[j + VECTOR_SIZE] = WasmValue.f32(floatData2[offset + j]);
        args[j + 2 * VECTOR_SIZE] = WasmValue.f32(floatData1[offset + j] * 0.5f);
      }

      WasmValue[] result = simdInstance.call("simd_fma_f32x4", args);
      blackhole.consume(result);
    }
  }

  /** Benchmark SIMD reduction operations (sum). */
  @Benchmark
  public void benchmarkSimdReduction(Blackhole blackhole) throws Exception {
    int iterations = BENCHMARK_DATA_SIZE / VECTOR_SIZE;

    for (int i = 0; i < iterations; i++) {
      int offset = i * VECTOR_SIZE;

      WasmValue[] args = new WasmValue[4];
      for (int j = 0; j < VECTOR_SIZE; j++) {
        args[j] = WasmValue.i32(intData1[offset + j]);
      }

      WasmValue result = simdInstance.call("simd_reduce_sum", args)[0];
      blackhole.consume(result);
    }
  }

  /** Benchmark SIMD shuffle operations. */
  @Benchmark
  public void benchmarkSimdShuffle(Blackhole blackhole) throws Exception {
    int iterations = BENCHMARK_DATA_SIZE / VECTOR_SIZE;

    for (int i = 0; i < iterations; i++) {
      int offset = i * VECTOR_SIZE;

      WasmValue[] args = new WasmValue[8];
      for (int j = 0; j < VECTOR_SIZE; j++) {
        args[j] = WasmValue.i32(intData1[offset + j]);
        args[j + VECTOR_SIZE] = WasmValue.i32(intData2[offset + j]);
      }

      WasmValue[] result = simdInstance.call("simd_shuffle", args);
      blackhole.consume(result);
    }
  }

  /** Benchmark memory-intensive SIMD operations (gather/scatter). */
  @Benchmark
  public void benchmarkSimdGatherScatter(Blackhole blackhole) throws Exception {
    // This benchmark tests gather/scatter performance
    int iterations = BENCHMARK_DATA_SIZE / 16; // Fewer iterations for memory-intensive ops

    for (int i = 0; i < iterations; i++) {
      int baseOffset = i * 16;

      // Create indices for gather operation
      WasmValue[] indices = {
        WasmValue.i32(baseOffset),
        WasmValue.i32(baseOffset + 4),
        WasmValue.i32(baseOffset + 8),
        WasmValue.i32(baseOffset + 12)
      };

      WasmValue[] result = simdInstance.call("simd_gather", indices);
      blackhole.consume(result);
    }
  }

  /** Benchmark complex number operations using SIMD. */
  @Benchmark
  public void benchmarkSimdComplexArithmetic(Blackhole blackhole) throws Exception {
    int iterations = BENCHMARK_DATA_SIZE / 4; // 2 complex numbers per V128

    for (int i = 0; i < iterations; i++) {
      int offset = i * 4;

      // Two complex numbers: (a+bi) and (c+di)
      WasmValue[] args = new WasmValue[8];
      for (int j = 0; j < 4; j++) {
        args[j] = WasmValue.f32(floatData1[offset + j]);
        args[j + 4] = WasmValue.f32(floatData2[offset + j]);
      }

      WasmValue[] result = simdInstance.call("simd_complex_multiply", args);
      blackhole.consume(result);
    }
  }

  /** Benchmark platform-specific optimizations vs fallback. */
  @Benchmark
  public void benchmarkPlatformOptimizations(Blackhole blackhole) throws Exception {
    int iterations = BENCHMARK_DATA_SIZE / VECTOR_SIZE;
    String functionName = getPlatformOptimizedFunction();

    for (int i = 0; i < iterations; i++) {
      int offset = i * VECTOR_SIZE;

      WasmValue[] args = new WasmValue[8];
      for (int j = 0; j < VECTOR_SIZE; j++) {
        args[j] = WasmValue.f32(floatData1[offset + j]);
        args[j + VECTOR_SIZE] = WasmValue.f32(floatData2[offset + j]);
      }

      WasmValue[] result = simdInstance.call(functionName, args);
      blackhole.consume(result);
    }
  }

  private String getPlatformOptimizedFunction() {
    return switch (simdLevel) {
      case "SSE41" -> "simd_optimized_sse41";
      case "AVX2" -> "simd_optimized_avx2";
      case "AVX512" -> "simd_optimized_avx512";
      case "NEON" -> "simd_optimized_neon";
      default -> "simd_fallback";
    };
  }

  /**
   * Loads a SIMD-optimized WebAssembly test module. In a real implementation, this would load
   * pre-compiled WASM modules with various SIMD operations.
   */
  private byte[] loadSimdTestModule() throws Exception {
    // For this benchmark, we use a simple WASM module with SIMD operations
    // In practice, this would be loaded from a resource file
    Path wasmPath = Path.of("src/test/resources/wasm/simd_benchmark.wasm");
    if (Files.exists(wasmPath)) {
      return Files.readAllBytes(wasmPath);
    } else {
      // Return a minimal WASM module for testing
      return createMinimalSimdModule();
    }
  }

  /** Loads a scalar fallback WebAssembly test module. */
  private byte[] loadScalarTestModule() throws Exception {
    Path wasmPath = Path.of("src/test/resources/wasm/scalar_benchmark.wasm");
    if (Files.exists(wasmPath)) {
      return Files.readAllBytes(wasmPath);
    } else {
      return createMinimalScalarModule();
    }
  }

  /**
   * Creates a minimal SIMD WebAssembly module for testing. This is a fallback when the actual test
   * modules aren't available.
   */
  private byte[] createMinimalSimdModule() {
    // This would contain the actual WAT (WebAssembly Text) compiled to binary
    // For now, return a minimal valid WASM module
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // magic
      0x01, 0x00, 0x00, 0x00, // version
      // Minimal module structure would go here
    };
  }

  /** Creates a minimal scalar WebAssembly module for testing. */
  private byte[] createMinimalScalarModule() {
    // Similar to SIMD module but with scalar operations only
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // magic
      0x01, 0x00, 0x00, 0x00, // version
      // Minimal module structure would go here
    };
  }

  /** Main method for running benchmarks standalone. */
  public static void main(String[] args) throws Exception {
    // Run specific benchmark configurations
    System.out.println("Starting SIMD Optimization Benchmarks...");

    // This would run the JMH benchmarks
    org.openjdk.jmh.Main.main(args);
  }
}

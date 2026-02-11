package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * SIMD-related benchmarks for wasmtime4j.
 *
 * <p>This benchmark suite tests WebAssembly execution performance with SIMD features enabled,
 * measuring the overhead of engine configuration, module compilation, and function execution when
 * SIMD support is active.
 *
 * <p>Since WebAssembly SIMD operates at the instruction level inside WASM modules, these benchmarks
 * measure the Java-to-WASM boundary overhead when SIMD features are enabled, and memory throughput
 * patterns that benefit from SIMD-optimized Wasmtime internals.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(
    value = 2,
    jvmArgs = {"-Xms2g", "-Xmx2g", "--enable-native-access=ALL-UNNAMED"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class SimdOptimizationBenchmark {

  private static final int BENCHMARK_DATA_SIZE = 1024;
  private static final Random RANDOM = new Random(42);

  /**
   * WAT module with arithmetic operations for benchmarking under SIMD-enabled engine. Exports: add,
   * accumulate, dot_product_scalar, memory.
   */
  private static final String SIMD_BENCHMARK_WAT =
      "(module\n"
          + "  (memory (export \"memory\") 4 16)\n"
          + "  (func $add (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add)\n"
          + "  (func $accumulate (export \"accumulate\") (param i32) (result i32)\n"
          + "    (local i32)\n"
          + "    (local.set 1 (i32.const 0))\n"
          + "    (block $break\n"
          + "      (loop $loop\n"
          + "        (br_if $break (i32.le_s (local.get 0) (i32.const 0)))\n"
          + "        (local.set 1 (i32.add (local.get 1) (local.get 0)))\n"
          + "        (local.set 0 (i32.sub (local.get 0) (i32.const 1)))\n"
          + "        (br $loop)))\n"
          + "    (local.get 1))\n"
          + "  (func $dot_product_scalar (export \"dot_product_scalar\")"
          + " (param i32 i32 i32 i32) (result i32)\n"
          + "    (i32.add\n"
          + "      (i32.mul (local.get 0) (local.get 2))\n"
          + "      (i32.mul (local.get 1) (local.get 3)))))\n";

  /** WAT module without SIMD features for comparison (same functions, different engine config). */
  private static final String SCALAR_BENCHMARK_WAT = SIMD_BENCHMARK_WAT;

  // Test data
  private int[] intData1;
  private int[] intData2;

  // WebAssembly instances
  private WasmRuntime runtime;
  private Engine simdEngine;
  private Engine scalarEngine;
  private Store simdStore;
  private Store scalarStore;
  private Instance simdInstance;
  private Instance scalarInstance;
  private WasmFunction simdAdd;
  private WasmFunction scalarAdd;
  private WasmFunction simdAccumulate;
  private WasmFunction scalarAccumulate;
  private WasmFunction simdDotProduct;
  private WasmFunction scalarDotProduct;
  private WasmMemory simdMemory;
  private WasmMemory scalarMemory;

  @Param({"JNI", "PANAMA"})
  private String runtimeType;

  /** Sets up the benchmark trial with SIMD-enabled and scalar-only engines. */
  @Setup(Level.Trial)
  public void setupTrial() throws Exception {
    final RuntimeType type = RuntimeType.valueOf(runtimeType);
    if (!WasmRuntimeFactory.isRuntimeAvailable(type)) {
      throw new RuntimeException("Runtime not available: " + type);
    }
    runtime = WasmRuntimeFactory.create(type);

    // SIMD-enabled engine
    final EngineConfig simdConfig =
        new EngineConfig()
            .addWasmFeature(WasmFeature.SIMD)
            .addWasmFeature(WasmFeature.MULTI_VALUE)
            .addWasmFeature(WasmFeature.BULK_MEMORY)
            .optimizationLevel(OptimizationLevel.SPEED);
    simdEngine = runtime.createEngine(simdConfig);
    simdStore = simdEngine.createStore();
    final Module simdModule = simdEngine.compileWat(SIMD_BENCHMARK_WAT);
    simdInstance = simdModule.instantiate(simdStore);
    simdAdd =
        simdInstance
            .getFunction("add")
            .orElseThrow(() -> new RuntimeException("add not found in SIMD module"));
    simdAccumulate =
        simdInstance
            .getFunction("accumulate")
            .orElseThrow(() -> new RuntimeException("accumulate not found in SIMD module"));
    simdDotProduct =
        simdInstance
            .getFunction("dot_product_scalar")
            .orElseThrow(() -> new RuntimeException("dot_product_scalar not found"));
    simdMemory =
        simdInstance
            .getMemory("memory")
            .orElseThrow(() -> new RuntimeException("memory not found in SIMD module"));

    // Scalar-only engine (no SIMD feature)
    final EngineConfig scalarConfig = new EngineConfig().optimizationLevel(OptimizationLevel.SPEED);
    scalarEngine = runtime.createEngine(scalarConfig);
    scalarStore = scalarEngine.createStore();
    final Module scalarModule = scalarEngine.compileWat(SCALAR_BENCHMARK_WAT);
    scalarInstance = scalarModule.instantiate(scalarStore);
    scalarAdd =
        scalarInstance
            .getFunction("add")
            .orElseThrow(() -> new RuntimeException("add not found in scalar module"));
    scalarAccumulate =
        scalarInstance
            .getFunction("accumulate")
            .orElseThrow(() -> new RuntimeException("accumulate not found in scalar module"));
    scalarDotProduct =
        scalarInstance
            .getFunction("dot_product_scalar")
            .orElseThrow(() -> new RuntimeException("dot_product_scalar not found"));
    scalarMemory =
        scalarInstance
            .getMemory("memory")
            .orElseThrow(() -> new RuntimeException("memory not found in scalar module"));

    simdModule.close();
    scalarModule.close();
  }

  /** Generates test data for each iteration. */
  @Setup(Level.Iteration)
  public void setupIteration() {
    intData1 = new int[BENCHMARK_DATA_SIZE];
    intData2 = new int[BENCHMARK_DATA_SIZE];
    for (int i = 0; i < BENCHMARK_DATA_SIZE; i++) {
      intData1[i] = RANDOM.nextInt(1000);
      intData2[i] = RANDOM.nextInt(1000);
    }
  }

  /** Cleans up all resources. */
  @TearDown(Level.Trial)
  public void teardownTrial() throws Exception {
    if (simdInstance != null) {
      simdInstance.close();
    }
    if (scalarInstance != null) {
      scalarInstance.close();
    }
    if (simdStore != null) {
      simdStore.close();
    }
    if (scalarStore != null) {
      scalarStore.close();
    }
    if (simdEngine != null) {
      simdEngine.close();
    }
    if (scalarEngine != null) {
      scalarEngine.close();
    }
    if (runtime != null) {
      runtime.close();
    }
  }

  /** Benchmarks function call throughput with SIMD-enabled engine. */
  @Benchmark
  public void benchmarkSimdEngineAddThroughput(final Blackhole blackhole) throws Exception {
    for (int i = 0; i < BENCHMARK_DATA_SIZE; i++) {
      final WasmValue[] result =
          simdAdd.call(WasmValue.i32(intData1[i]), WasmValue.i32(intData2[i]));
      blackhole.consume(result[0].asInt());
    }
  }

  /** Benchmarks function call throughput with scalar-only engine. */
  @Benchmark
  public void benchmarkScalarEngineAddThroughput(final Blackhole blackhole) throws Exception {
    for (int i = 0; i < BENCHMARK_DATA_SIZE; i++) {
      final WasmValue[] result =
          scalarAdd.call(WasmValue.i32(intData1[i]), WasmValue.i32(intData2[i]));
      blackhole.consume(result[0].asInt());
    }
  }

  /** Benchmarks accumulate (loop-heavy) function with SIMD-enabled engine. */
  @Benchmark
  public void benchmarkSimdEngineAccumulate(final Blackhole blackhole) throws Exception {
    for (int i = 0; i < 100; i++) {
      final WasmValue[] result =
          simdAccumulate.call(WasmValue.i32(intData1[i % BENCHMARK_DATA_SIZE]));
      blackhole.consume(result[0].asInt());
    }
  }

  /** Benchmarks accumulate (loop-heavy) function with scalar-only engine. */
  @Benchmark
  public void benchmarkScalarEngineAccumulate(final Blackhole blackhole) throws Exception {
    for (int i = 0; i < 100; i++) {
      final WasmValue[] result =
          scalarAccumulate.call(WasmValue.i32(intData1[i % BENCHMARK_DATA_SIZE]));
      blackhole.consume(result[0].asInt());
    }
  }

  /** Benchmarks dot product computation with SIMD-enabled engine. */
  @Benchmark
  public void benchmarkSimdEngineDotProduct(final Blackhole blackhole) throws Exception {
    for (int i = 0; i < BENCHMARK_DATA_SIZE - 1; i += 2) {
      final WasmValue[] result =
          simdDotProduct.call(
              WasmValue.i32(intData1[i]),
              WasmValue.i32(intData1[i + 1]),
              WasmValue.i32(intData2[i]),
              WasmValue.i32(intData2[i + 1]));
      blackhole.consume(result[0].asInt());
    }
  }

  /** Benchmarks dot product computation with scalar-only engine. */
  @Benchmark
  public void benchmarkScalarEngineDotProduct(final Blackhole blackhole) throws Exception {
    for (int i = 0; i < BENCHMARK_DATA_SIZE - 1; i += 2) {
      final WasmValue[] result =
          scalarDotProduct.call(
              WasmValue.i32(intData1[i]),
              WasmValue.i32(intData1[i + 1]),
              WasmValue.i32(intData2[i]),
              WasmValue.i32(intData2[i + 1]));
      blackhole.consume(result[0].asInt());
    }
  }

  /** Benchmarks memory write throughput with SIMD-enabled engine. */
  @Benchmark
  public void benchmarkSimdEngineMemoryWrite(final Blackhole blackhole) throws Exception {
    final byte[] data = new byte[256];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (intData1[i % BENCHMARK_DATA_SIZE] & 0xFF);
    }
    for (int i = 0; i < 100; i++) {
      simdMemory.writeBytes(i * 256, data, 0, data.length);
    }
    blackhole.consume(data);
  }

  /** Benchmarks memory write throughput with scalar-only engine. */
  @Benchmark
  public void benchmarkScalarEngineMemoryWrite(final Blackhole blackhole) throws Exception {
    final byte[] data = new byte[256];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (intData1[i % BENCHMARK_DATA_SIZE] & 0xFF);
    }
    for (int i = 0; i < 100; i++) {
      scalarMemory.writeBytes(i * 256, data, 0, data.length);
    }
    blackhole.consume(data);
  }

  /** Benchmarks module compilation time with SIMD features enabled. */
  @Benchmark
  public void benchmarkSimdEngineCompilation(final Blackhole blackhole) throws Exception {
    final Module module = simdEngine.compileWat(SIMD_BENCHMARK_WAT);
    blackhole.consume(module.getName());
    module.close();
  }

  /** Benchmarks module compilation time without SIMD features. */
  @Benchmark
  public void benchmarkScalarEngineCompilation(final Blackhole blackhole) throws Exception {
    final Module module = scalarEngine.compileWat(SCALAR_BENCHMARK_WAT);
    blackhole.consume(module.getName());
    module.close();
  }
}

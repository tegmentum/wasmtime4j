package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Memory;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.ByteBuffer;
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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Performance optimization validation benchmark suite.
 *
 * <p>This benchmark validates performance optimizations implemented across wasmtime4j including
 * function call overhead reduction, parameter marshalling improvements, memory operation
 * optimization, and resource management efficiency.
 *
 * <p>Optimization areas tested:
 *
 * <ul>
 *   <li>Function call overhead minimization
 *   <li>Parameter marshalling efficiency improvements
 *   <li>Memory operation optimization and bounds checking
 *   <li>Resource allocation and deallocation performance
 *   <li>Exception handling and error path optimization
 *   <li>JNI and Panama FFI call path optimization
 * </ul>
 *
 * <p>Performance targets:
 *
 * <ul>
 *   <li>Function call overhead should be <10% of native Wasmtime
 *   <li>Parameter marshalling should have minimal allocation overhead
 *   <li>Memory operations should maintain near-native performance
 *   <li>Resource management should scale linearly with load
 * </ul>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@State(Scope.Benchmark)
public final class PerformanceOptimizationValidationBenchmark extends BenchmarkBase {

  /** Optimized WebAssembly module for function call testing. */
  private static final byte[] FUNCTION_CALL_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // WASM magic
        0x01,
        0x00,
        0x00,
        0x00, // Version
        0x01,
        0x1c,
        0x06, // Type section
        0x60,
        0x00,
        0x01,
        0x7f, // Type 0: () -> i32
        0x60,
        0x01,
        0x7f,
        0x01,
        0x7f, // Type 1: (i32) -> i32
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // Type 2: (i32, i32) -> i32
        0x60,
        0x03,
        0x7f,
        0x7f,
        0x7f,
        0x01,
        0x7f, // Type 3: (i32, i32, i32) -> i32
        0x60,
        0x04,
        0x7f,
        0x7f,
        0x7f,
        0x7f,
        0x01,
        0x7f, // Type 4: (i32, i32, i32, i32) -> i32
        0x60,
        0x05,
        0x7f,
        0x7f,
        0x7f,
        0x7f,
        0x7f,
        0x01,
        0x7f, // Type 5: (i32, i32, i32, i32, i32) -> i32
        0x03,
        0x07,
        0x06,
        0x00,
        0x01,
        0x02,
        0x03,
        0x04,
        0x05, // Function section: 6 functions
        0x07,
        0x3a,
        0x06, // Export section
        0x05,
        0x7a,
        0x65,
        0x72,
        0x6f,
        0x50,
        0x00,
        0x00, // Export "zeroP" (0 params)
        0x04,
        0x6f,
        0x6e,
        0x65,
        0x50,
        0x00,
        0x01, // Export "oneP" (1 param)
        0x05,
        0x74,
        0x77,
        0x6f,
        0x50,
        0x00,
        0x02, // Export "twoP" (2 params)
        0x07,
        0x74,
        0x68,
        0x72,
        0x65,
        0x65,
        0x50,
        0x00,
        0x03, // Export "threeP" (3 params)
        0x05,
        0x66,
        0x6f,
        0x75,
        0x72,
        0x50,
        0x00,
        0x04, // Export "fourP" (4 params)
        0x05,
        0x66,
        0x69,
        0x76,
        0x65,
        0x50,
        0x00,
        0x05, // Export "fiveP" (5 params)
        0x0a,
        0x2a,
        0x06, // Code section: 6 function bodies
        0x04,
        0x00,
        0x41,
        0x01,
        0x0b, // Function 0: return 1
        0x06,
        0x00,
        0x20,
        0x00,
        0x41,
        0x01,
        0x6a,
        0x0b, // Function 1: param + 1
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x0b, // Function 2: param1 + param2
        0x0b,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x20,
        0x02,
        0x6a,
        0x0b, // Function 3: param1 + param2 + param3
        0x0f,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x20,
        0x02,
        0x6a,
        0x20,
        0x03,
        0x6a,
        0x0b, // Function 4: sum of 4 params
        0x13,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x20,
        0x02,
        0x6a,
        0x20,
        0x03,
        0x6a,
        0x20,
        0x04,
        0x6a,
        0x0b // Function 5: sum of 5 params
      };

  /** Memory optimization WebAssembly module. */
  private static final byte[] MEMORY_OPT_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // WASM magic
        0x01,
        0x00,
        0x00,
        0x00, // Version
        0x01,
        0x14,
        0x05, // Type section
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x00, // Type 0: (i32, i32) -> () (store)
        0x60,
        0x01,
        0x7f,
        0x01,
        0x7f, // Type 1: (i32) -> i32 (load)
        0x60,
        0x03,
        0x7f,
        0x7f,
        0x7f,
        0x00, // Type 2: (i32, i32, i32) -> () (memcpy)
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x00, // Type 3: (i32, i32) -> () (memset)
        0x60,
        0x00,
        0x01,
        0x7f, // Type 4: () -> i32 (size)
        0x03,
        0x06,
        0x05,
        0x00,
        0x01,
        0x02,
        0x03,
        0x04, // Function section: 5 functions
        0x05,
        0x03,
        0x01,
        0x00,
        0x08, // Memory section: 8 pages initial (512KB)
        0x07,
        0x2a,
        0x05, // Export section
        0x05,
        0x73,
        0x74,
        0x6f,
        0x72,
        0x65,
        0x00,
        0x00, // Export "store"
        0x04,
        0x6c,
        0x6f,
        0x61,
        0x64,
        0x00,
        0x01, // Export "load"
        0x06,
        0x6d,
        0x65,
        0x6d,
        0x63,
        0x70,
        0x79,
        0x00,
        0x02, // Export "memcpy"
        0x06,
        0x6d,
        0x65,
        0x6d,
        0x73,
        0x65,
        0x74,
        0x00,
        0x03, // Export "memset"
        0x04,
        0x73,
        0x69,
        0x7a,
        0x65,
        0x00,
        0x04, // Export "size"
        0x0a,
        0x3f,
        0x05, // Code section: 5 function bodies
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x36,
        0x02,
        0x00,
        0x0b, // Function 0: store
        0x07,
        0x00,
        0x20,
        0x00,
        0x28,
        0x02,
        0x00,
        0x0b, // Function 1: load
        0x15,
        0x00,
        0x20,
        0x02,
        0x04,
        0x40,
        0x03,
        0x40,
        0x20,
        0x00,
        0x20,
        0x01,
        0x2d,
        0x00,
        0x00,
        0x3a,
        0x00,
        0x00,
        0x20,
        0x00,
        0x41,
        0x01,
        0x6a,
        0x21,
        0x00,
        0x20,
        0x01,
        0x41,
        0x01,
        0x6a,
        0x21,
        0x01,
        0x20,
        0x02,
        0x41,
        0x01,
        0x6b,
        0x22,
        0x02,
        0x0d,
        0x00,
        0x0b,
        0x0b,
        0x0b, // Function 2: memcpy
        0x11,
        0x00,
        0x20,
        0x01,
        0x04,
        0x40,
        0x03,
        0x40,
        0x20,
        0x00,
        0x41,
        0x00,
        0x3a,
        0x00,
        0x00,
        0x20,
        0x00,
        0x41,
        0x01,
        0x6a,
        0x21,
        0x00,
        0x20,
        0x01,
        0x41,
        0x01,
        0x6b,
        0x22,
        0x01,
        0x0d,
        0x00,
        0x0b,
        0x0b,
        0x0b, // Function 3: memset
        0x06,
        0x00,
        0x3f,
        0x00,
        0x41,
        0x10,
        0x74,
        0x0b // Function 4: memory.size * 65536
      };

  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  @Param({"BASELINE", "OPTIMIZED"})
  private String optimizationLevel;

  private Engine engineBaseline;
  private Engine engineOptimized;
  private Store storeBaseline;
  private Store storeOptimized;
  private Instance functionCallInstance;
  private Instance memoryOptInstance;
  private Function[] functionsByParamCount;
  private Function[] memoryFunctions;
  private Memory memory;

  @Setup(Level.Trial)
  public void setupTrial() throws WasmException {
    super.setupRuntime(runtimeTypeName);

    // Create baseline engine
    engineBaseline = Engine.builder().optimizationLevel("none").crankLiftOptLevel("none").build();

    // Create optimized engine with all performance features
    engineOptimized =
        Engine.builder()
            .optimizationLevel("speed")
            .crankLiftOptLevel("speed")
            .poolingAllocator(true)
            .poolSize(1000)
            .moduleCaching(true)
            .enableFunctionInlining(true)
            .enableBranchPrediction(true)
            .enableDeadCodeElimination(true)
            .build();

    final Engine activeEngine =
        "OPTIMIZED".equals(optimizationLevel) ? engineOptimized : engineBaseline;

    storeBaseline = Store.withoutData(engineBaseline);
    storeOptimized = Store.withoutData(engineOptimized);
    final Store activeStore =
        "OPTIMIZED".equals(optimizationLevel) ? storeOptimized : storeBaseline;

    // Compile modules
    final Module functionCallModule = Module.fromBinary(activeEngine, FUNCTION_CALL_WASM);
    final Module memoryOptModule = Module.fromBinary(activeEngine, MEMORY_OPT_WASM);

    // Create instances
    functionCallInstance = Instance.create(activeStore, functionCallModule);
    memoryOptInstance = Instance.create(activeStore, memoryOptModule);

    // Get function handles for different parameter counts
    functionsByParamCount = new Function[6];
    functionsByParamCount[0] = functionCallInstance.getFunction("zeroP");
    functionsByParamCount[1] = functionCallInstance.getFunction("oneP");
    functionsByParamCount[2] = functionCallInstance.getFunction("twoP");
    functionsByParamCount[3] = functionCallInstance.getFunction("threeP");
    functionsByParamCount[4] = functionCallInstance.getFunction("fourP");
    functionsByParamCount[5] = functionCallInstance.getFunction("fiveP");

    // Get memory operation functions
    memoryFunctions = new Function[5];
    memoryFunctions[0] = memoryOptInstance.getFunction("store");
    memoryFunctions[1] = memoryOptInstance.getFunction("load");
    memoryFunctions[2] = memoryOptInstance.getFunction("memcpy");
    memoryFunctions[3] = memoryOptInstance.getFunction("memset");
    memoryFunctions[4] = memoryOptInstance.getFunction("size");

    memory = memoryOptInstance.getMemory("memory");

    closeQuietly(memoryOptModule);
    closeQuietly(functionCallModule);

    logInfo(
        "Performance optimization benchmark setup completed for runtime: "
            + runtimeTypeName
            + ", optimization: "
            + optimizationLevel);
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() {
    closeQuietly(memoryOptInstance);
    closeQuietly(functionCallInstance);
    closeQuietly(storeOptimized);
    closeQuietly(storeBaseline);
    closeQuietly(engineOptimized);
    closeQuietly(engineBaseline);

    super.tearDownRuntime();
    logInfo(
        "Performance optimization benchmark teardown completed for runtime: " + runtimeTypeName);
  }

  /** Benchmarks function call overhead with zero parameters. */
  @Benchmark
  public int benchmarkZeroParameterFunction() throws WasmException {
    return functionsByParamCount[0].call();
  }

  /** Benchmarks function call overhead with one parameter. */
  @Benchmark
  public int benchmarkOneParameterFunction() throws WasmException {
    return functionsByParamCount[1].call(42);
  }

  /** Benchmarks function call overhead with two parameters. */
  @Benchmark
  public int benchmarkTwoParameterFunction() throws WasmException {
    return functionsByParamCount[2].call(10, 20);
  }

  /** Benchmarks function call overhead with three parameters. */
  @Benchmark
  public int benchmarkThreeParameterFunction() throws WasmException {
    return functionsByParamCount[3].call(5, 10, 15);
  }

  /** Benchmarks function call overhead with four parameters. */
  @Benchmark
  public int benchmarkFourParameterFunction() throws WasmException {
    return functionsByParamCount[4].call(1, 2, 3, 4);
  }

  /** Benchmarks function call overhead with five parameters. */
  @Benchmark
  public int benchmarkFiveParameterFunction() throws WasmException {
    return functionsByParamCount[5].call(1, 2, 3, 4, 5);
  }

  /** Benchmarks parameter marshalling efficiency with varying parameter counts. */
  @Benchmark
  public void benchmarkParameterMarshallingEfficiency() throws WasmException {
    // Test different parameter counts to measure marshalling overhead
    functionsByParamCount[0].call();
    functionsByParamCount[1].call(1);
    functionsByParamCount[2].call(1, 2);
    functionsByParamCount[3].call(1, 2, 3);
    functionsByParamCount[4].call(1, 2, 3, 4);
    functionsByParamCount[5].call(1, 2, 3, 4, 5);
  }

  /** Benchmarks optimized memory store operations. */
  @Benchmark
  public void benchmarkOptimizedMemoryStore() throws WasmException {
    memoryFunctions[0].call(0, 42);
  }

  /** Benchmarks optimized memory load operations. */
  @Benchmark
  public int benchmarkOptimizedMemoryLoad() throws WasmException {
    return memoryFunctions[1].call(0);
  }

  /** Benchmarks optimized memory copy operations. */
  @Benchmark
  public void benchmarkOptimizedMemoryCopy() throws WasmException {
    // Initialize source memory
    memoryFunctions[0].call(100, 0x12345678);
    memoryFunctions[0].call(104, 0x9abcdef0);

    // Copy 8 bytes from offset 100 to offset 200
    memoryFunctions[2].call(200, 100, 8);
  }

  /** Benchmarks optimized memory set operations. */
  @Benchmark
  public void benchmarkOptimizedMemorySet() throws WasmException {
    // Set 64 bytes to zero starting at offset 1000
    memoryFunctions[3].call(1000, 64);
  }

  /** Benchmarks memory size operations. */
  @Benchmark
  public int benchmarkMemorySizeOperation() throws WasmException {
    return memoryFunctions[4].call();
  }

  /** Benchmarks Java memory API performance. */
  @Benchmark
  public void benchmarkJavaMemoryAPI() throws WasmException {
    final ByteBuffer buffer = memory.buffer();

    // Write and read operations through Java API
    buffer.putInt(0, 0x12345678);
    buffer.putInt(4, 0x9abcdef0);
    final int value1 = buffer.getInt(0);
    final int value2 = buffer.getInt(4);

    // Prevent dead code elimination
    if (value1 == 0 && value2 == 0) {
      throw new WasmException("Unexpected zero values");
    }
  }

  /** Benchmarks bulk memory operations through Java API. */
  @Benchmark
  public void benchmarkBulkMemoryOperations() throws WasmException {
    final ByteBuffer buffer = memory.buffer();
    final byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    // Bulk write
    buffer.position(500);
    buffer.put(data);

    // Bulk read
    final byte[] readData = new byte[data.length];
    buffer.position(500);
    buffer.get(readData);

    // Verify data integrity
    if (readData[0] != data[0]) {
      throw new WasmException("Data integrity check failed");
    }
  }

  /** Benchmarks concurrent function calls with optimization. */
  @Benchmark
  @Threads(4)
  public int benchmarkConcurrentOptimizedCalls() throws WasmException {
    final int threadId = Thread.currentThread().hashCode() % 1000;
    return functionsByParamCount[2].call(threadId, threadId + 1);
  }

  /** Benchmarks high-frequency optimized function calls. */
  @Benchmark
  public void benchmarkHighFrequencyOptimizedCalls() throws WasmException {
    for (int i = 0; i < 100; i++) {
      functionsByParamCount[1].call(i);
    }
  }

  /** Benchmarks mixed optimization workload. */
  @Benchmark
  public void benchmarkMixedOptimizationWorkload() throws WasmException {
    // Mix of function calls with different complexities
    functionsByParamCount[0].call();
    functionsByParamCount[2].call(10, 20);

    // Memory operations
    memoryFunctions[0].call(0, 100);
    final int value = memoryFunctions[1].call(0);

    // More function calls
    functionsByParamCount[3].call(value, value + 1, value + 2);
  }

  /** Benchmarks resource allocation efficiency with optimization. */
  @Benchmark
  public void benchmarkOptimizedResourceAllocation() throws WasmException {
    final Engine engine = "OPTIMIZED".equals(optimizationLevel) ? engineOptimized : engineBaseline;

    // Quick resource allocation and deallocation to test efficiency
    final Store store = Store.withoutData(engine);
    try {
      final Module module = Module.fromBinary(engine, FUNCTION_CALL_WASM);
      try {
        final Instance instance = Instance.create(store, module);
        try {
          final Function func = instance.getFunction("oneP");
          func.call(42);
        } finally {
          closeQuietly(instance);
        }
      } finally {
        closeQuietly(module);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /** Benchmarks exception handling optimization. */
  @Benchmark
  public void benchmarkOptimizedExceptionHandling() throws WasmException {
    try {
      // Normal execution path (should be optimized)
      functionsByParamCount[1].call(42);
      memoryFunctions[1].call(0);

      // Memory bounds checking (should be efficient)
      final ByteBuffer buffer = memory.buffer();
      final int size = buffer.capacity();
      if (size > 100) {
        buffer.get(size - 100); // Safe access near end
      }

    } catch (final WasmException e) {
      // Exception handling should be optimized
      logWarn("Exception in optimization benchmark: " + e.getMessage());
      throw e;
    }
  }

  /** Benchmarks call stack optimization. */
  @Benchmark
  public void benchmarkCallStackOptimization() throws WasmException {
    // Test call stack efficiency with nested function calls
    final int result1 = functionsByParamCount[1].call(10);
    final int result2 = functionsByParamCount[2].call(result1, 20);
    final int result3 = functionsByParamCount[3].call(result2, 30, 40);

    // Store and retrieve result to test memory interaction
    memoryFunctions[0].call(0, result3);
    final int finalResult = memoryFunctions[1].call(0);

    // Prevent dead code elimination
    if (finalResult < 0) {
      throw new WasmException("Unexpected negative result");
    }
  }

  private void closeQuietly(final AutoCloseable resource) {
    if (resource != null) {
      try {
        resource.close();
      } catch (final Exception e) {
        logWarn("Error closing resource: " + e.getMessage());
      }
    }
  }
}

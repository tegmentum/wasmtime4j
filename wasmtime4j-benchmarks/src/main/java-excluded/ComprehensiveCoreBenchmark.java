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
 * Comprehensive benchmark suite for all major wasmtime4j operations.
 *
 * <p>This benchmark validates performance across all core wasmtime4j functionality including:
 *
 * <ul>
 *   <li>Engine creation and configuration performance
 *   <li>Module compilation and validation efficiency
 *   <li>Instance creation and lifecycle management
 *   <li>Function call overhead and parameter marshalling
 *   <li>Memory operations and bounds checking performance
 *   <li>Store operations and context switching efficiency
 *   <li>Error handling and exception processing overhead
 * </ul>
 *
 * <p>Performance targets based on specification requirements:
 *
 * <ul>
 *   <li>JNI runtime should achieve 85% of native Wasmtime performance
 *   <li>Panama runtime should achieve 80% of native Wasmtime performance
 *   <li>Function calls should have minimal overhead beyond native Wasmtime
 *   <li>Memory operations should maintain competitive performance
 * </ul>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@State(Scope.Benchmark)
public final class ComprehensiveCoreBenchmark extends BenchmarkBase {

  /** WebAssembly module with arithmetic functions. */
  private static final byte[] ARITHMETIC_WASM =
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
        0x0c,
        0x03, // Type section
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // Type 0: (i32, i32) -> i32
        0x60,
        0x01,
        0x7f,
        0x01,
        0x7f, // Type 1: (i32) -> i32
        0x60,
        0x00,
        0x01,
        0x7f, // Type 2: () -> i32
        0x03,
        0x05,
        0x04,
        0x00,
        0x01,
        0x00,
        0x02, // Function section: 4 functions
        0x07,
        0x1e,
        0x04, // Export section
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // Export "add" as function 0
        0x03,
        0x6d,
        0x75,
        0x6c,
        0x00,
        0x01, // Export "mul" as function 1
        0x03,
        0x73,
        0x75,
        0x62,
        0x00,
        0x02, // Export "sub" as function 2
        0x07,
        0x63,
        0x6f,
        0x6e,
        0x73,
        0x74,
        0x34,
        0x32,
        0x00,
        0x03, // Export "const42" as function 3
        0x0a,
        0x1e,
        0x04, // Code section: 4 function bodies
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x0b, // Function 0: add
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6c,
        0x0b, // Function 1: mul
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6b,
        0x0b, // Function 2: sub
        0x04,
        0x00,
        0x41,
        0x2a,
        0x0b // Function 3: const 42
      };

  /** WebAssembly module with memory operations. */
  private static final byte[] MEMORY_WASM =
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
        0x08,
        0x02, // Type section
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x00, // Type 0: (i32, i32) -> ()
        0x60,
        0x01,
        0x7f,
        0x01,
        0x7f, // Type 1: (i32) -> i32
        0x03,
        0x03,
        0x02,
        0x00,
        0x01, // Function section: 2 functions
        0x05,
        0x03,
        0x01,
        0x00,
        0x02, // Memory section: 2 pages initial
        0x07,
        0x11,
        0x02, // Export section
        0x05,
        0x73,
        0x74,
        0x6f,
        0x72,
        0x65,
        0x00,
        0x00, // Export "store" as function 0
        0x04,
        0x6c,
        0x6f,
        0x61,
        0x64,
        0x00,
        0x01, // Export "load" as function 1
        0x0a,
        0x11,
        0x02, // Code section: 2 function bodies
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
        0x0b // Function 1: load
      };

  /** WebAssembly module with control flow. */
  private static final byte[] CONTROL_FLOW_WASM =
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
        0x08,
        0x02, // Type section
        0x60,
        0x01,
        0x7f,
        0x01,
        0x7f, // Type 0: (i32) -> i32
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // Type 1: (i32, i32) -> i32
        0x03,
        0x03,
        0x02,
        0x00,
        0x01, // Function section: 2 functions
        0x07,
        0x11,
        0x02, // Export section
        0x04,
        0x66,
        0x61,
        0x63,
        0x74,
        0x00,
        0x00, // Export "fact" as function 0
        0x03,
        0x6d,
        0x61,
        0x78,
        0x00,
        0x01, // Export "max" as function 1
        0x0a,
        0x20,
        0x02, // Code section: 2 function bodies
        // Factorial function (recursive)
        0x13,
        0x00,
        0x20,
        0x00,
        0x41,
        0x01,
        0x4e,
        0x04,
        0x7f,
        0x41,
        0x01,
        0x05,
        0x20,
        0x00,
        0x20,
        0x00,
        0x41,
        0x01,
        0x6b,
        0x10,
        0x00,
        0x6c,
        0x0b,
        0x0b,
        // Max function
        0x09,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x20,
        0x00,
        0x20,
        0x01,
        0x4a,
        0x1b,
        0x0b
      };

  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  private Engine engine;
  private Store store;
  private Module arithmeticModule;
  private Module memoryModule;
  private Module controlFlowModule;
  private Instance arithmeticInstance;
  private Instance memoryInstance;
  private Instance controlFlowInstance;
  private Function addFunc;
  private Function mulFunc;
  private Function subFunc;
  private Function const42Func;
  private Function storeFunc;
  private Function loadFunc;
  private Function factFunc;
  private Function maxFunc;
  private Memory memory;

  @Setup(Level.Trial)
  public void setupTrial() throws WasmException {
    super.setupRuntime(runtimeTypeName);

    // Create optimized engine
    engine = Engine.builder().optimizationLevel("speed").crankLiftOptLevel("speed").build();

    store = Store.withoutData(engine);

    // Compile all test modules
    arithmeticModule = Module.fromBinary(engine, ARITHMETIC_WASM);
    memoryModule = Module.fromBinary(engine, MEMORY_WASM);
    controlFlowModule = Module.fromBinary(engine, CONTROL_FLOW_WASM);

    // Create instances
    arithmeticInstance = Instance.create(store, arithmeticModule);
    memoryInstance = Instance.create(store, memoryModule);
    controlFlowInstance = Instance.create(store, controlFlowModule);

    // Get function handles
    addFunc = arithmeticInstance.getFunction("add");
    mulFunc = arithmeticInstance.getFunction("mul");
    subFunc = arithmeticInstance.getFunction("sub");
    const42Func = arithmeticInstance.getFunction("const42");
    storeFunc = memoryInstance.getFunction("store");
    loadFunc = memoryInstance.getFunction("load");
    factFunc = controlFlowInstance.getFunction("fact");
    maxFunc = controlFlowInstance.getFunction("max");

    // Get memory handle
    memory = memoryInstance.getMemory("memory");

    logInfo("Comprehensive benchmark setup completed for runtime: " + runtimeTypeName);
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() {
    closeQuietly(controlFlowInstance);
    closeQuietly(memoryInstance);
    closeQuietly(arithmeticInstance);
    closeQuietly(controlFlowModule);
    closeQuietly(memoryModule);
    closeQuietly(arithmeticModule);
    closeQuietly(store);
    closeQuietly(engine);

    super.tearDownRuntime();
    logInfo("Comprehensive benchmark teardown completed for runtime: " + runtimeTypeName);
  }

  /** Benchmarks engine creation performance. */
  @Benchmark
  public Engine benchmarkEngineCreation() throws WasmException {
    return Engine.builder().build();
  }

  /** Benchmarks module compilation performance. */
  @Benchmark
  public Module benchmarkModuleCompilation() throws WasmException {
    return Module.fromBinary(engine, ARITHMETIC_WASM);
  }

  /** Benchmarks instance creation performance. */
  @Benchmark
  public Instance benchmarkInstanceCreation() throws WasmException {
    return Instance.create(store, arithmeticModule);
  }

  /** Benchmarks store creation performance. */
  @Benchmark
  public Store benchmarkStoreCreation() throws WasmException {
    return Store.withoutData(engine);
  }

  /** Benchmarks simple arithmetic function calls. */
  @Benchmark
  public int benchmarkSimpleArithmetic() throws WasmException {
    return addFunc.call(42, 24);
  }

  /** Benchmarks multiplication function calls. */
  @Benchmark
  public int benchmarkMultiplication() throws WasmException {
    return mulFunc.call(7, 6);
  }

  /** Benchmarks subtraction function calls. */
  @Benchmark
  public int benchmarkSubtraction() throws WasmException {
    return subFunc.call(100, 42);
  }

  /** Benchmarks constant function calls. */
  @Benchmark
  public int benchmarkConstantFunction() throws WasmException {
    return const42Func.call();
  }

  /** Benchmarks memory store operations. */
  @Benchmark
  public void benchmarkMemoryStore() throws WasmException {
    storeFunc.call(0, 42);
  }

  /** Benchmarks memory load operations. */
  @Benchmark
  public int benchmarkMemoryLoad() throws WasmException {
    return loadFunc.call(0);
  }

  /** Benchmarks memory read through Java API. */
  @Benchmark
  public byte benchmarkMemoryReadJava() throws WasmException {
    final ByteBuffer buffer = memory.buffer();
    return buffer.get(0);
  }

  /** Benchmarks memory write through Java API. */
  @Benchmark
  public void benchmarkMemoryWriteJava() throws WasmException {
    final ByteBuffer buffer = memory.buffer();
    buffer.put(0, (byte) 42);
  }

  /** Benchmarks control flow with recursion. */
  @Benchmark
  public int benchmarkControlFlowRecursion() throws WasmException {
    return factFunc.call(5); // 5! = 120
  }

  /** Benchmarks control flow with conditionals. */
  @Benchmark
  public int benchmarkControlFlowConditional() throws WasmException {
    return maxFunc.call(42, 24);
  }

  /** Benchmarks mixed arithmetic operations. */
  @Benchmark
  public void benchmarkMixedArithmetic() throws WasmException {
    final int a = addFunc.call(10, 20);
    final int b = mulFunc.call(a, 2);
    final int c = subFunc.call(b, 5);

    // Prevent dead code elimination
    if (c < 0) {
      throw new WasmException("Unexpected negative result");
    }
  }

  /** Benchmarks mixed memory operations. */
  @Benchmark
  public void benchmarkMixedMemoryOperations() throws WasmException {
    storeFunc.call(0, 100);
    storeFunc.call(4, 200);
    final int val1 = loadFunc.call(0);
    final int val2 = loadFunc.call(4);

    // Prevent dead code elimination
    if (val1 + val2 == 0) {
      throw new WasmException("Unexpected zero result");
    }
  }

  /** Benchmarks function call overhead with different parameter counts. */
  @Benchmark
  public void benchmarkParameterMarshallingOverhead() throws WasmException {
    const42Func.call(); // 0 parameters
    addFunc.call(1, 2); // 2 parameters
    maxFunc.call(5, 10); // 2 parameters with conditional
  }

  /** Benchmarks concurrent function execution. */
  @Benchmark
  @Threads(4)
  public int benchmarkConcurrentFunctionExecution() throws WasmException {
    final int threadId = Thread.currentThread().hashCode() % 1000;
    return addFunc.call(threadId, 42);
  }

  /** Benchmarks high-frequency function calls. */
  @Benchmark
  public void benchmarkHighFrequencyCalls() throws WasmException {
    for (int i = 0; i < 100; i++) {
      addFunc.call(i, i + 1);
    }
  }

  /** Benchmarks error handling overhead. */
  @Benchmark
  public void benchmarkErrorHandlingOverhead() throws WasmException {
    try {
      // Normal execution
      addFunc.call(1, 2);

      // This should not throw in normal cases
      loadFunc.call(0);

    } catch (final WasmException e) {
      // Handle potential errors
      logWarn("Unexpected error in benchmark: " + e.getMessage());
      throw e;
    }
  }

  /** Benchmarks comprehensive operation mix. */
  @Benchmark
  public void benchmarkComprehensiveOperationMix() throws WasmException {
    // Create new resources
    final Store tempStore = Store.withoutData(engine);
    try {
      final Instance tempInstance = Instance.create(tempStore, arithmeticModule);
      try {
        final Function tempFunc = tempInstance.getFunction("add");

        // Execute operations
        final int result = tempFunc.call(10, 20);

        // Memory operations
        storeFunc.call(0, result);
        loadFunc.call(0);

        // Control flow
        maxFunc.call(result, 50);

      } finally {
        closeQuietly(tempInstance);
      }
    } finally {
      closeQuietly(tempStore);
    }
  }

  /** Benchmarks resource cleanup efficiency. */
  @Benchmark
  public void benchmarkResourceCleanupEfficiency() throws WasmException {
    // Create and immediately cleanup resources to test efficiency
    for (int i = 0; i < 10; i++) {
      final Store tempStore = Store.withoutData(engine);
      try {
        final Instance tempInstance = Instance.create(tempStore, arithmeticModule);
        try {
          final Function tempFunc = tempInstance.getFunction("add");
          tempFunc.call(i, i + 1);
        } finally {
          closeQuietly(tempInstance);
        }
      } finally {
        closeQuietly(tempStore);
      }
    }
  }

  /** Benchmarks memory bounds checking performance. */
  @Benchmark
  public void benchmarkMemoryBoundsChecking() throws WasmException {
    // Test memory access within bounds (should be fast)
    final ByteBuffer buffer = memory.buffer();
    final int memorySize = buffer.capacity();

    // Access memory at various safe offsets
    for (int offset = 0; offset < Math.min(memorySize, 100); offset += 4) {
      buffer.put(offset, (byte) (offset % 256));
      buffer.get(offset);
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

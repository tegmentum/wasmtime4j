package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
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
 * Comprehensive benchmark to validate performance monitoring overhead claims.
 *
 * <p>This benchmark validates the claimed <5% performance monitoring overhead by comparing
 * execution performance with comprehensive monitoring enabled versus disabled across various
 * workload scenarios and monitoring levels.
 *
 * <p>Test scenarios include:
 *
 * <ul>
 *   <li>Function execution with and without performance monitoring
 *   <li>Memory operations monitoring overhead measurement
 *   <li>Instance lifecycle monitoring performance impact
 *   <li>Multi-threaded monitoring efficiency testing
 *   <li>Real-world workload monitoring overhead analysis
 * </ul>
 *
 * <p>Performance targets based on specification requirements:
 *
 * <ul>
 *   <li>Performance monitoring should add <5% overhead to function execution
 *   <li>Memory monitoring should have minimal impact on allocation performance
 *   <li>Multi-threaded monitoring should not significantly impact concurrency
 *   <li>Comprehensive monitoring should remain under 5% total overhead
 * </ul>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@State(Scope.Benchmark)
public final class PerformanceMonitoringOverheadBenchmark extends BenchmarkBase {

  /** Simple WebAssembly module with basic function for monitoring tests. */
  private static final byte[] TEST_WASM_FUNCTION =
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
        0x00,
        0x00, // Type 1: () -> ()
        0x03,
        0x02,
        0x01,
        0x00, // Function section: function 0 has type 0
        0x07,
        0x08,
        0x01,
        0x04,
        0x61,
        0x64,
        0x64,
        0x31,
        0x00,
        0x00, // Export section: export "add1" as function 0
        0x0a,
        0x09,
        0x01,
        0x07,
        0x00,
        0x20,
        0x00,
        0x41,
        0x01,
        0x6a,
        0x0b // Code section: local.get 0, i32.const 1, i32.add
      };

  /** Memory-intensive WebAssembly module for memory monitoring tests. */
  private static final byte[] TEST_WASM_MEMORY =
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
        0x04,
        0x01,
        0x60,
        0x00,
        0x01,
        0x7f, // Type section: () -> i32
        0x03,
        0x02,
        0x01,
        0x00, // Function section: function 0 has type 0
        0x05,
        0x03,
        0x01,
        0x00,
        0x01, // Memory section: 1 page initial
        0x07,
        0x0a,
        0x01,
        0x06,
        0x6d,
        0x65,
        0x6d,
        0x5f,
        0x6f,
        0x70,
        0x00,
        0x00, // Export section: export "mem_op"
        0x0a,
        0x0a,
        0x01,
        0x08,
        0x00,
        0x41,
        0x00,
        0x41,
        0x2a,
        0x36,
        0x02,
        0x00,
        0x0b // Code section: i32.const 0, i32.const 42, i32.store
      };

  /** Complex WebAssembly module with multiple functions for comprehensive monitoring. */
  private static final byte[] TEST_WASM_COMPLEX =
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
        0x00, // Type 2: () -> ()
        0x03,
        0x04,
        0x03,
        0x00,
        0x01,
        0x02, // Function section: 3 functions
        0x07,
        0x15,
        0x03, // Export section
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
        0x04,
        0x6e,
        0x6f,
        0x6f,
        0x70,
        0x00,
        0x02, // Export "noop" as function 2
        0x0a,
        0x12,
        0x03, // Code section: 3 function bodies
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
        0x02,
        0x00,
        0x0b // Function 2: noop
      };

  @Param({"true", "false"})
  private boolean enableMonitoring;

  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  @Param({"BASIC", "COMPREHENSIVE", "MEMORY_FOCUSED"})
  private String monitoringLevel;

  private Engine engineWithMonitoring;
  private Engine engineWithoutMonitoring;
  private Store storeWithMonitoring;
  private Store storeWithoutMonitoring;
  private Instance instanceFunction;
  private Instance instanceMemory;
  private Instance instanceComplex;
  private Function addFunction;
  private Function memoryOperation;
  private Function addComplexFunction;
  private Function mulComplexFunction;

  @Setup(Level.Trial)
  public void setupTrial() throws WasmException {
    super.setupRuntime(runtimeTypeName);

    // Create engines with different monitoring configurations
    engineWithMonitoring =
        Engine.builder()
            .performanceMonitoring(true)
            .monitoringLevel(getMonitoringLevel())
            .enableFunctionProfiling(true)
            .enableMemoryProfiling(isMemoryFocusedMonitoring())
            .enableInstanceProfiling(true)
            .profilingCollectionInterval(1000) // 1 second intervals
            .build();

    engineWithoutMonitoring = Engine.builder().performanceMonitoring(false).build();

    // Create stores
    final Engine activeEngine = enableMonitoring ? engineWithMonitoring : engineWithoutMonitoring;
    storeWithMonitoring = Store.withoutData(engineWithMonitoring);
    storeWithoutMonitoring = Store.withoutData(engineWithoutMonitoring);

    // Create test instances
    final Module functionModule = Module.fromBinary(activeEngine, TEST_WASM_FUNCTION);
    final Module memoryModule = Module.fromBinary(activeEngine, TEST_WASM_MEMORY);
    final Module complexModule = Module.fromBinary(activeEngine, TEST_WASM_COMPLEX);

    final Store activeStore = enableMonitoring ? storeWithMonitoring : storeWithoutMonitoring;
    instanceFunction = Instance.create(activeStore, functionModule);
    instanceMemory = Instance.create(activeStore, memoryModule);
    instanceComplex = Instance.create(activeStore, complexModule);

    // Get function handles
    addFunction = instanceFunction.getFunction("add1");
    memoryOperation = instanceMemory.getFunction("mem_op");
    addComplexFunction = instanceComplex.getFunction("add");
    mulComplexFunction = instanceComplex.getFunction("mul");

    closeQuietly(complexModule);
    closeQuietly(memoryModule);
    closeQuietly(functionModule);

    logInfo(
        "Benchmark setup completed for runtime: "
            + runtimeTypeName
            + ", monitoring: "
            + enableMonitoring
            + ", level: "
            + monitoringLevel);
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() {
    closeQuietly(instanceComplex);
    closeQuietly(instanceMemory);
    closeQuietly(instanceFunction);
    closeQuietly(storeWithoutMonitoring);
    closeQuietly(storeWithMonitoring);
    closeQuietly(engineWithoutMonitoring);
    closeQuietly(engineWithMonitoring);

    super.tearDownRuntime();
    logInfo("Benchmark teardown completed for runtime: " + runtimeTypeName);
  }

  /**
   * Benchmarks basic function execution with monitoring overhead. This is the primary test for
   * validating the <5% overhead claim.
   */
  @Benchmark
  public int benchmarkBasicFunctionExecution() throws WasmException {
    return addFunction.call(42);
  }

  /**
   * Benchmarks memory operations with monitoring overhead. Tests monitoring impact on
   * memory-intensive operations.
   */
  @Benchmark
  public int benchmarkMemoryOperationExecution() throws WasmException {
    return memoryOperation.call();
  }

  /**
   * Benchmarks complex function execution with monitoring. Tests monitoring overhead with multiple
   * function calls.
   */
  @Benchmark
  public void benchmarkComplexFunctionExecution() throws WasmException {
    final int result1 = addComplexFunction.call(10, 20);
    final int result2 = mulComplexFunction.call(result1, 2);

    // Prevent dead code elimination
    if (result2 < 0) {
      throw new WasmException("Unexpected negative result");
    }
  }

  /**
   * Benchmarks high-frequency function calls with monitoring. Tests monitoring efficiency with
   * rapid function execution.
   */
  @Benchmark
  public void benchmarkHighFrequencyFunctionCalls() throws WasmException {
    for (int i = 0; i < 100; i++) {
      addFunction.call(i);
    }
  }

  /**
   * Benchmarks concurrent function execution with monitoring. Tests monitoring impact under
   * multi-threaded load.
   */
  @Benchmark
  @Threads(4)
  public int benchmarkConcurrentFunctionExecution() throws WasmException {
    return addComplexFunction.call(Thread.currentThread().hashCode(), 42);
  }

  /**
   * Benchmarks high-concurrency monitoring overhead. Maximum stress test for monitoring performance
   * impact.
   */
  @Benchmark
  @Threads(16)
  public void benchmarkHighConcurrencyMonitoringOverhead() throws WasmException {
    final int threadId = Thread.currentThread().hashCode() % 1000;
    addComplexFunction.call(threadId, 1);
    mulComplexFunction.call(threadId, 2);
  }

  /**
   * Benchmarks monitoring overhead with mixed workloads. Simulates realistic application usage
   * patterns.
   */
  @Benchmark
  public void benchmarkMixedWorkloadMonitoring() throws WasmException {
    // Mix of different operation types
    addFunction.call(10);
    memoryOperation.call();
    addComplexFunction.call(5, 15);
    mulComplexFunction.call(3, 7);
  }

  /**
   * Benchmarks instance lifecycle monitoring overhead. Tests monitoring impact on instance creation
   * and disposal.
   */
  @Benchmark
  public void benchmarkInstanceLifecycleMonitoring() throws WasmException {
    final Engine engine = enableMonitoring ? engineWithMonitoring : engineWithoutMonitoring;
    final Store store = Store.withoutData(engine);

    try {
      final Module module = Module.fromBinary(engine, TEST_WASM_FUNCTION);
      try {
        final Instance instance = Instance.create(store, module);
        final Function func = instance.getFunction("add1");

        // Execute function to trigger monitoring
        func.call(42);

        closeQuietly(instance);
      } finally {
        closeQuietly(module);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /**
   * Benchmarks batch operations monitoring overhead. Tests monitoring efficiency with bulk
   * operations.
   */
  @Benchmark
  public void benchmarkBatchOperationsMonitoring() throws WasmException {
    final int[] inputs = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    final int[] results = new int[inputs.length];

    for (int i = 0; i < inputs.length; i++) {
      results[i] = addFunction.call(inputs[i]);
    }

    // Prevent dead code elimination
    if (results[0] == Integer.MAX_VALUE) {
      throw new WasmException("Unexpected result");
    }
  }

  /**
   * Benchmarks long-running monitoring overhead. Tests monitoring impact over extended execution
   * periods.
   */
  @Benchmark
  public void benchmarkLongRunningMonitoringOverhead() throws WasmException {
    // Simulate long-running operations with periodic function calls
    for (int batch = 0; batch < 5; batch++) {
      for (int i = 0; i < 20; i++) {
        final int result = addComplexFunction.call(i, batch);

        // Simulate some processing
        if (result % 10 == 0) {
          memoryOperation.call();
        }
      }

      // Simulate batch processing delay
      Thread.yield();
    }
  }

  /**
   * Benchmarks monitoring data collection overhead. Tests the impact of performance data gathering
   * and processing.
   */
  @Benchmark
  public void benchmarkMonitoringDataCollectionOverhead() throws WasmException {
    final Engine engine = enableMonitoring ? engineWithMonitoring : engineWithoutMonitoring;

    // Perform operations that would trigger data collection
    for (int i = 0; i < 50; i++) {
      addFunction.call(i);

      // Simulate monitoring data collection intervals
      if (i % 10 == 0 && enableMonitoring) {
        // In a real implementation, this would trigger data collection
        // For now, just ensure operations continue
        Thread.yield();
      }
    }
  }

  /**
   * Benchmarks comprehensive monitoring features overhead. Tests all monitoring features enabled
   * together.
   */
  @Benchmark
  public void benchmarkComprehensiveMonitoringOverhead() throws WasmException {
    if (!enableMonitoring) {
      // Baseline without monitoring
      addFunction.call(42);
      memoryOperation.call();
      return;
    }

    // With comprehensive monitoring enabled
    addFunction.call(42);
    memoryOperation.call();
    addComplexFunction.call(10, 20);
    mulComplexFunction.call(5, 6);

    // Simulate monitoring checkpoint
    Thread.yield();
  }

  /**
   * Benchmarks monitoring overhead scaling with operation complexity. Tests how monitoring overhead
   * scales with workload complexity.
   */
  @Benchmark
  public void benchmarkMonitoringOverheadScaling() throws WasmException {
    // Simple operations
    for (int i = 0; i < 10; i++) {
      addFunction.call(i);
    }

    // Medium complexity
    for (int i = 0; i < 5; i++) {
      addComplexFunction.call(i, i + 1);
    }

    // Higher complexity with memory operations
    for (int i = 0; i < 3; i++) {
      memoryOperation.call();
      mulComplexFunction.call(i, i + 2);
    }
  }

  private String getMonitoringLevel() {
    switch (monitoringLevel) {
      case "BASIC":
        return "BASIC";
      case "COMPREHENSIVE":
        return "COMPREHENSIVE";
      case "MEMORY_FOCUSED":
        return "MEMORY_FOCUSED";
      default:
        return "BASIC";
    }
  }

  private boolean isMemoryFocusedMonitoring() {
    return "MEMORY_FOCUSED".equals(monitoringLevel) || "COMPREHENSIVE".equals(monitoringLevel);
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

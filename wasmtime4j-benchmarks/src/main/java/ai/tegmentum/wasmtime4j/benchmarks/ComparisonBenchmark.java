package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Direct comparison benchmarks between JNI and Panama implementations.
 *
 * <p>This benchmark class provides side-by-side performance comparisons of JNI and Panama
 * implementations across key operation categories. It focuses on identifying performance
 * differences and trade-offs between the two approaches.
 *
 * <p>Key comparison areas:
 *
 * <ul>
 *   <li>Initialization overhead comparison
 *   <li>Function call performance comparison
 *   <li>Memory operation throughput comparison
 *   <li>Resource utilization comparison
 *   <li>Scalability comparison
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 3,
    jvmArgs = {"-Xms2g", "-Xmx2g"})
public class ComparisonBenchmark extends BenchmarkBase {

  /** Operation category to compare between runtimes. */
  @Param({"INITIALIZATION", "FUNCTION_CALL", "MEMORY_ACCESS", "MIXED_WORKLOAD"})
  private String operationCategory;

  /** Workload intensity level. */
  @Param({"LIGHT", "MEDIUM", "HEAVY"})
  private String workloadIntensity;

  /** Mock performance measurement result. */
  private static final class PerformanceResult {
    private final RuntimeType runtimeType;
    private final String operation;
    private final long executionTime;
    private final double throughput;
    private final long memoryUsed;

    PerformanceResult(
        final RuntimeType runtimeType,
        final String operation,
        final long executionTime,
        final double throughput,
        final long memoryUsed) {
      this.runtimeType = runtimeType;
      this.operation = operation;
      this.executionTime = executionTime;
      this.throughput = throughput;
      this.memoryUsed = memoryUsed;
    }

    public RuntimeType getRuntimeType() {
      return runtimeType;
    }

    public String getOperation() {
      return operation;
    }

    public long getExecutionTime() {
      return executionTime;
    }

    public double getThroughput() {
      return throughput;
    }

    public long getMemoryUsed() {
      return memoryUsed;
    }

    public double getEfficiencyScore() {
      // Simple efficiency metric: throughput per MB of memory
      return memoryUsed > 0 ? throughput / (memoryUsed / 1024.0 / 1024.0) : throughput;
    }
  }

  /** Real workload executor for different runtime types. */
  private static final class WorkloadExecutor {
    private final RuntimeType runtimeType;
    private final String category;
    private final String intensity;
    private WasmRuntime runtime;
    private Engine engine;
    private Store store;
    private Module module;
    private Instance instance;
    private WasmFunction targetFunction;
    private WasmMemory wasmMemory;

    WorkloadExecutor(final RuntimeType runtimeType, final String category, final String intensity) {
      this.runtimeType = runtimeType;
      this.category = category;
      this.intensity = intensity;
    }

    void initialize(final String watSource) throws WasmException {
      // Create WebAssembly runtime components
      runtime = BenchmarkBase.createRuntime(runtimeType);
      engine = BenchmarkBase.createEngine(runtime);
      store = BenchmarkBase.createStore(engine);
      module = BenchmarkBase.compileWatModule(engine, watSource);
      instance = BenchmarkBase.instantiateModule(store, module);

      // Get function and memory based on category
      switch (category) {
        case "FUNCTION_CALL":
        case "MIXED_WORKLOAD":
          targetFunction = instance.getFunction("add").orElse(null);
          break;
        default:
          break;
      }

      if (category.equals("MEMORY_ACCESS") || category.equals("MIXED_WORKLOAD")) {
        wasmMemory = instance.getMemory("memory").orElse(null);
      }
    }

    PerformanceResult execute() {
      final long startTime = System.nanoTime();
      final long memoryBefore = getMemoryUsage();

      final int workAmount = getWorkAmount();
      final double operationResult = performWork(workAmount);

      final long endTime = System.nanoTime();
      final long memoryAfter = getMemoryUsage();

      final long executionTime = endTime - startTime;
      final double throughput = operationResult / (executionTime / 1_000_000_000.0);
      final long memoryUsed = Math.max(0, memoryAfter - memoryBefore);

      return new PerformanceResult(runtimeType, category, executionTime, throughput, memoryUsed);
    }

    void cleanup() {
      try {
        // Function and memory resources are managed by the instance
        if (instance != null) {
          instance.close();
        }
        if (module != null) {
          module.close();
        }
        if (store != null) {
          store.close();
        }
        if (engine != null) {
          engine.close();
        }
        if (runtime != null) {
          runtime.close();
        }
      } catch (final Exception e) {
        // Ignore cleanup errors
      }
    }

    private int getWorkAmount() {
      int baseAmount = 100;

      // Adjust based on operation category
      switch (category) {
        case "INITIALIZATION":
          baseAmount = 50;
          break;
        case "FUNCTION_CALL":
          baseAmount = 200;
          break;
        case "MEMORY_ACCESS":
          baseAmount = 300;
          break;
        case "MIXED_WORKLOAD":
          baseAmount = 250;
          break;
        default:
          break;
      }

      // Adjust based on workload intensity
      switch (intensity) {
        case "LIGHT":
          return baseAmount;
        case "MEDIUM":
          return baseAmount * 2;
        case "HEAVY":
          return baseAmount * 4;
        default:
          return baseAmount;
      }
    }

    private double performWork(final int workAmount) {
      double result = 0;

      try {
        switch (category) {
          case "INITIALIZATION":
            result = performInitializationWork(workAmount);
            break;
          case "FUNCTION_CALL":
            result = performFunctionCallWork(workAmount);
            break;
          case "MEMORY_ACCESS":
            result = performMemoryWork(workAmount);
            break;
          case "MIXED_WORKLOAD":
            result = performMixedWork(workAmount);
            break;
          default:
            result = workAmount;
            break;
        }
      } catch (final WasmException e) {
        // Return work amount as fallback
        result = workAmount;
      }

      return result;
    }

    private double performInitializationWork(final int workAmount) throws WasmException {
      double result = 0;

      // Create and destroy multiple engine instances to measure initialization overhead
      for (int i = 0; i < Math.min(workAmount / 10, 5); i++) {
        final WasmRuntime tempRuntime = BenchmarkBase.createRuntime(runtimeType);
        final Engine tempEngine = BenchmarkBase.createEngine(tempRuntime);
        final Store tempStore = BenchmarkBase.createStore(tempEngine);

        result += tempRuntime.hashCode() % 100;

        tempStore.close();
        tempEngine.close();
        tempRuntime.close();
      }

      return result;
    }

    private double performFunctionCallWork(final int workAmount) throws WasmException {
      double result = 0;

      if (targetFunction != null) {
        final WasmValue[] params = {WasmValue.i32(1), WasmValue.i32(2)};

        for (int i = 0; i < workAmount; i++) {
          params[0] = WasmValue.i32(i);
          params[1] = WasmValue.i32(i + 1);

          final WasmValue[] results = targetFunction.call(params);
          if (results.length > 0) {
            result += results[0].asInt();
          }
        }
      } else {
        // Fallback if function not available
        result = workAmount;
      }

      return result;
    }

    private double performMemoryWork(final int workAmount) throws WasmException {
      double result = 0;

      if (wasmMemory != null) {
        final byte[] writeBuffer = new byte[64];
        final int memSize = Math.min(Math.toIntExact(wasmMemory.getSize()), 4096);

        for (int i = 0; i < workAmount && i * 64 < memSize - 64; i++) {
          // Fill write buffer with data
          for (int j = 0; j < writeBuffer.length; j++) {
            writeBuffer[j] = (byte) ((i + j) % 256);
          }

          // Write to WebAssembly memory
          wasmMemory.writeBytes(i * 64, writeBuffer, 0, writeBuffer.length);

          // Read back from WebAssembly memory
          final byte[] readBuffer = new byte[writeBuffer.length];
          wasmMemory.readBytes(i * 64, readBuffer, 0, writeBuffer.length);
          for (final byte b : readBuffer) {
            result += b & 0xFF;
          }
        }
      } else {
        // Fallback if memory not available
        result = workAmount;
      }

      return result;
    }

    private double performMixedWork(final int workAmount) throws WasmException {
      final int third = workAmount / 3;

      double result = 0;

      // Don't reinitialize for mixed workload, just do some setup work
      result += third; // Simulated initialization work

      if (targetFunction != null) {
        result += performFunctionCallWork(third);
      } else {
        result += third;
      }

      if (wasmMemory != null) {
        result += performMemoryWork(third);
      } else {
        result += third;
      }

      return result;
    }

    private long getMemoryUsage() {
      final Runtime runtime = Runtime.getRuntime();
      return runtime.totalMemory() - runtime.freeMemory();
    }
  }

  /** JNI workload executor. */
  private WorkloadExecutor jniExecutor;

  /** Panama workload executor. */
  private WorkloadExecutor panamaExecutor;

  /** WAT source for benchmarks. */
  private String watSource;

  /** Setup performed before each benchmark iteration. */
  @Setup(Level.Iteration)
  public void setupIteration() {
    try {
      // Select appropriate WAT source based on operation category
      if (operationCategory.equals("MEMORY_ACCESS") || operationCategory.equals("MIXED_WORKLOAD")) {
        watSource = COMPLEX_WAT_MODULE; // Has memory
      } else {
        watSource = SIMPLE_WAT_MODULE;
      }

      jniExecutor = new WorkloadExecutor(RuntimeType.JNI, operationCategory, workloadIntensity);
      panamaExecutor =
          new WorkloadExecutor(RuntimeType.PANAMA, operationCategory, workloadIntensity);

      // Initialize executors if not initialization benchmark
      if (!operationCategory.equals("INITIALIZATION")) {
        jniExecutor.initialize(watSource);
        panamaExecutor.initialize(watSource);
      }

      // Force garbage collection for clean comparison
      System.gc();
      try {
        Thread.sleep(50);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    } catch (final WasmException e) {
      throw new RuntimeException("Failed to setup executors", e);
    }
  }

  /** Cleanup performed after each benchmark iteration. */
  @TearDown(Level.Iteration)
  public void teardownIteration() {
    if (jniExecutor != null) {
      jniExecutor.cleanup();
      jniExecutor = null;
    }
    if (panamaExecutor != null) {
      panamaExecutor.cleanup();
      panamaExecutor = null;
    }
    watSource = null;

    // Final cleanup
    System.gc();
  }

  /**
   * Benchmarks JNI implementation performance.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   * @return performance result for JNI
   */
  @Benchmark
  public PerformanceResult benchmarkJniPerformance(final Blackhole blackhole) {
    try {
      // For initialization benchmarks, initialize on each call
      if (operationCategory.equals("INITIALIZATION")) {
        jniExecutor.initialize(watSource);
      }

      final PerformanceResult result = jniExecutor.execute();

      blackhole.consume(result.getExecutionTime());
      blackhole.consume(result.getThroughput());
      blackhole.consume(result.getMemoryUsed());

      // Cleanup initialization resources immediately
      if (operationCategory.equals("INITIALIZATION")) {
        jniExecutor.cleanup();
      }

      return result;
    } catch (final WasmException e) {
      throw new RuntimeException("JNI benchmark failed", e);
    }
  }

  /**
   * Benchmarks Panama implementation performance.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   * @return performance result for Panama
   */
  @Benchmark
  public PerformanceResult benchmarkPanamaPerformance(final Blackhole blackhole) {
    try {
      // For initialization benchmarks, initialize on each call
      if (operationCategory.equals("INITIALIZATION")) {
        panamaExecutor.initialize(watSource);
      }

      final PerformanceResult result = panamaExecutor.execute();

      blackhole.consume(result.getExecutionTime());
      blackhole.consume(result.getThroughput());
      blackhole.consume(result.getMemoryUsed());

      // Cleanup initialization resources immediately
      if (operationCategory.equals("INITIALIZATION")) {
        panamaExecutor.cleanup();
      }

      return result;
    } catch (final WasmException e) {
      throw new RuntimeException("Panama benchmark failed", e);
    }
  }

  /**
   * Direct side-by-side comparison benchmark.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkSideBySideComparison(final Blackhole blackhole) {
    try {
      // For initialization benchmarks, initialize on each call
      if (operationCategory.equals("INITIALIZATION")) {
        jniExecutor.initialize(watSource);
        panamaExecutor.initialize(watSource);
      }

      final PerformanceResult jniResult = jniExecutor.execute();
      final PerformanceResult panamaResult = panamaExecutor.execute();

      // Calculate comparison metrics
      final double throughputRatio =
          jniResult.getThroughput() > 0
              ? panamaResult.getThroughput() / jniResult.getThroughput()
              : 1.0;
      final double memoryRatio =
          jniResult.getMemoryUsed() > 0
              ? (double) panamaResult.getMemoryUsed() / jniResult.getMemoryUsed()
              : 1.0;
      final double efficiencyComparison =
          jniResult.getEfficiencyScore() > 0
              ? panamaResult.getEfficiencyScore() / jniResult.getEfficiencyScore()
              : 1.0;

      blackhole.consume(throughputRatio);
      blackhole.consume(memoryRatio);
      blackhole.consume(efficiencyComparison);
      blackhole.consume(jniResult.getExecutionTime());
      blackhole.consume(panamaResult.getExecutionTime());

      // Cleanup initialization resources immediately
      if (operationCategory.equals("INITIALIZATION")) {
        jniExecutor.cleanup();
        panamaExecutor.cleanup();
      }
    } catch (final WasmException e) {
      throw new RuntimeException("Side-by-side comparison failed", e);
    }
  }

  /**
   * Benchmarks scalability comparison across different thread counts.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkScalabilityComparison(final Blackhole blackhole) {
    final int[] threadCounts = {1, 2, 4};
    final PerformanceResult[] jniResults = new PerformanceResult[threadCounts.length];
    final PerformanceResult[] panamaResults = new PerformanceResult[threadCounts.length];

    for (int i = 0; i < threadCounts.length; i++) {
      // Simulate multi-threaded execution by running multiple executors
      double jniTotalThroughput = 0;
      double panamaTotalThroughput = 0;
      long jniTotalMemory = 0;
      long panamaTotalMemory = 0;

      for (int j = 0; j < threadCounts[i]; j++) {
        final PerformanceResult jniResult = jniExecutor.execute();
        final PerformanceResult panamaResult = panamaExecutor.execute();

        jniTotalThroughput += jniResult.getThroughput();
        panamaTotalThroughput += panamaResult.getThroughput();
        jniTotalMemory += jniResult.getMemoryUsed();
        panamaTotalMemory += panamaResult.getMemoryUsed();
      }

      jniResults[i] =
          new PerformanceResult(
              RuntimeType.JNI, operationCategory, 0, jniTotalThroughput, jniTotalMemory);
      panamaResults[i] =
          new PerformanceResult(
              RuntimeType.PANAMA, operationCategory, 0, panamaTotalThroughput, panamaTotalMemory);
    }

    // Calculate scalability metrics
    for (int i = 0; i < threadCounts.length; i++) {
      blackhole.consume(jniResults[i].getThroughput());
      blackhole.consume(panamaResults[i].getThroughput());
      blackhole.consume(threadCounts[i]);
    }
  }

  /**
   * Benchmarks resource utilization comparison.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkResourceUtilizationComparison(final Blackhole blackhole) {
    // Measure resource usage over time
    final int iterations = 10;
    long jniTotalMemory = 0;
    long panamaTotalMemory = 0;
    double jniTotalThroughput = 0;
    double panamaTotalThroughput = 0;

    for (int i = 0; i < iterations; i++) {
      // Force GC between iterations for accurate measurement
      if (i % 5 == 0) {
        System.gc();
      }

      final PerformanceResult jniResult = jniExecutor.execute();
      final PerformanceResult panamaResult = panamaExecutor.execute();

      jniTotalMemory += jniResult.getMemoryUsed();
      panamaTotalMemory += panamaResult.getMemoryUsed();
      jniTotalThroughput += jniResult.getThroughput();
      panamaTotalThroughput += panamaResult.getThroughput();
    }

    // Calculate average resource utilization
    final double avgJniMemory = (double) jniTotalMemory / iterations;
    final double avgPanamaMemory = (double) panamaTotalMemory / iterations;
    final double avgJniThroughput = jniTotalThroughput / iterations;
    final double avgPanamaThroughput = panamaTotalThroughput / iterations;

    blackhole.consume(avgJniMemory);
    blackhole.consume(avgPanamaMemory);
    blackhole.consume(avgJniThroughput);
    blackhole.consume(avgPanamaThroughput);
  }

  /**
   * Benchmarks warm-up time comparison.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkWarmupTimeComparison(final Blackhole blackhole) {
    // Measure performance improvement over multiple runs (simulating JIT warm-up)
    final int warmupRuns = 5;
    final PerformanceResult[] jniWarmupResults = new PerformanceResult[warmupRuns];
    final PerformanceResult[] panamaWarmupResults = new PerformanceResult[warmupRuns];

    for (int i = 0; i < warmupRuns; i++) {
      jniWarmupResults[i] = jniExecutor.execute();
      panamaWarmupResults[i] = panamaExecutor.execute();

      blackhole.consume(jniWarmupResults[i].getThroughput());
      blackhole.consume(panamaWarmupResults[i].getThroughput());
    }

    // Calculate warm-up improvement ratios
    final double jniImprovementRatio =
        jniWarmupResults[warmupRuns - 1].getThroughput() / jniWarmupResults[0].getThroughput();
    final double panamaImprovementRatio =
        panamaWarmupResults[warmupRuns - 1].getThroughput()
            / panamaWarmupResults[0].getThroughput();

    blackhole.consume(jniImprovementRatio);
    blackhole.consume(panamaImprovementRatio);
  }

  /**
   * Benchmarks error handling performance comparison.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkErrorHandlingComparison(final Blackhole blackhole) {
    int jniSuccessCount = 0;
    int jniErrorCount = 0;
    int panamaSuccessCount = 0;
    int panamaErrorCount = 0;

    // Simulate operations with some failures
    for (int i = 0; i < 20; i++) {
      try {
        if (i % 7 == 0) {
          // Simulate error condition
          throw new RuntimeException("Simulated error");
        } else {
          // Normal operation
          jniExecutor.execute();
          jniSuccessCount++;
        }
      } catch (final RuntimeException e) {
        jniErrorCount++;
        blackhole.consume(e.getMessage());
      }

      try {
        if (i % 7 == 0) {
          // Simulate error condition
          throw new RuntimeException("Simulated error");
        } else {
          // Normal operation
          panamaExecutor.execute();
          panamaSuccessCount++;
        }
      } catch (final RuntimeException e) {
        panamaErrorCount++;
        blackhole.consume(e.getMessage());
      }
    }

    blackhole.consume(jniSuccessCount);
    blackhole.consume(jniErrorCount);
    blackhole.consume(panamaSuccessCount);
    blackhole.consume(panamaErrorCount);
  }
}

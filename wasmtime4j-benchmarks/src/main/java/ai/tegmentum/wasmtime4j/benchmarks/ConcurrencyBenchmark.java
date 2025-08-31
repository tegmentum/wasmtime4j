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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * Benchmarks for WebAssembly concurrency and parallelism performance.
 *
 * <p>This benchmark class measures the performance characteristics of concurrent WebAssembly
 * operations, comparing JNI and Panama implementations across different concurrency patterns.
 *
 * <p>Key metrics measured:
 *
 * <ul>
 *   <li>Multiple instance creation and management
 *   <li>Concurrent function execution
 *   <li>Thread safety and synchronization overhead
 *   <li>Resource contention under load
 *   <li>Scalability with increasing thread counts
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms2g", "-Xmx4g"})
public class ConcurrencyBenchmark extends BenchmarkBase {

  /** Runtime implementation to benchmark. */
  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  /** Number of concurrent threads to use. */
  @Param({"2", "4", "8"})
  private int threadCount;

  /** Concurrency pattern to test. */
  @Param({"SHARED_RUNTIME", "ISOLATED_RUNTIMES", "MIXED_OPERATIONS"})
  private String concurrencyPattern;

  /** WebAssembly runtime components for shared pattern. */
  private WasmRuntime sharedRuntime;

  private Engine sharedEngine;

  /** Thread pool for concurrent execution. */
  private ExecutorService executorService;

  /** Module bytecode. */
  private byte[] moduleBytes;

  /** WebAssembly context for concurrent operations. */
  private static final class ConcurrentWasmContext {
    private final WasmRuntime runtime;
    private final Engine engine;
    private final Store store;
    private final Module module;
    private final Instance instance;
    private final WasmFunction function;
    private final WasmMemory memory;
    private final RuntimeType runtimeType;

    ConcurrentWasmContext(final RuntimeType runtimeType, final byte[] moduleBytes)
        throws WasmException {
      this.runtimeType = runtimeType;
      this.runtime = BenchmarkBase.createRuntime(runtimeType);
      this.engine = BenchmarkBase.createEngine(runtime);
      this.store = BenchmarkBase.createStore(engine);
      this.module = BenchmarkBase.compileModule(engine, moduleBytes);
      this.instance = BenchmarkBase.instantiateModule(store, module);
      this.function = instance.getFunction("add").orElse(null);
      this.memory = instance.getMemory("memory").orElse(null);
    }

    ConcurrentWasmContext(final Engine sharedEngine, final byte[] moduleBytes)
        throws WasmException {
      this.runtimeType = getRecommendedRuntime(); // Inherited from shared engine
      this.runtime = null; // Using shared runtime
      this.engine = sharedEngine;
      this.store = BenchmarkBase.createStore(engine);
      this.module = BenchmarkBase.compileModule(engine, moduleBytes);
      this.instance = BenchmarkBase.instantiateModule(store, module);
      this.function = instance.getFunction("add").orElse(null);
      this.memory = instance.getMemory("memory").orElse(null);
    }

    WasmValue[] callFunction(final int param1, final int param2) throws WasmException {
      final WasmValue[] params = {WasmValue.i32(param1), WasmValue.i32(param2)};
      return function.call(params);
    }

    void writeMemory(final int offset, final byte[] data) throws WasmException {
      if (memory != null && offset + data.length <= memory.getSize()) {
        memory.writeBytes(offset, data, 0, data.length);
      }
    }

    byte[] readMemory(final int offset, final int length) throws WasmException {
      if (memory != null && offset + length <= memory.getSize()) {
        byte[] result = new byte[length];
        memory.readBytes(offset, result, 0, length);
        return result;
      }
      return new byte[0];
    }

    void close() {
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
        if (runtime != null) {
          runtime.close();
        }
      } catch (final Exception e) {
        // Ignore cleanup errors
      }
    }

    public RuntimeType getRuntimeType() {
      return runtimeType;
    }
  }

  /** Converts string runtime type name to RuntimeType enum. */
  private RuntimeType getRuntimeType() {
    return RuntimeType.valueOf(runtimeTypeName);
  }

  /** Setup performed before each benchmark iteration. */
  @Setup(Level.Iteration)
  public void setupIteration() throws WasmException {
    moduleBytes = SIMPLE_WASM_MODULE.clone();
    executorService = Executors.newFixedThreadPool(threadCount);

    // Setup shared runtime for SHARED_RUNTIME pattern
    if (concurrencyPattern.equals("SHARED_RUNTIME")) {
      sharedRuntime = createRuntime(getRuntimeType());
      sharedEngine = createEngine(sharedRuntime);
    }

    // Force GC before concurrent operations
    System.gc();
  }

  /** Cleanup performed after each benchmark iteration. */
  @TearDown(Level.Iteration)
  public void teardownIteration() {
    if (executorService != null) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (final InterruptedException e) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
      executorService = null;
    }

    if (sharedEngine != null) {
      try {
        sharedEngine.close();
      } catch (final Exception e) {
        // Ignore cleanup errors
      }
      sharedEngine = null;
    }

    if (sharedRuntime != null) {
      try {
        sharedRuntime.close();
      } catch (final Exception e) {
        // Ignore cleanup errors
      }
      sharedRuntime = null;
    }

    moduleBytes = null;
  }

  /**
   * Benchmarks concurrent function calls with shared runtime.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkConcurrentFunctionCalls(final Blackhole blackhole) {
    final List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      final Future<Integer> future =
          executorService.submit(
              () -> {
                try {
                  final ConcurrentWasmContext context;
                  if (concurrencyPattern.equals("SHARED_RUNTIME")) {
                    context = new ConcurrentWasmContext(sharedEngine, moduleBytes);
                  } else {
                    context = new ConcurrentWasmContext(getRuntimeType(), moduleBytes);
                  }

                  int result = 0;
                  for (int j = 0; j < 100; j++) {
                    final WasmValue[] callResult = context.callFunction(threadId + j, j);
                    if (callResult.length > 0) {
                      result += callResult[0].asInt();
                    }
                  }

                  context.close();
                  return result;
                } catch (final WasmException e) {
                  return -1;
                }
              });
      futures.add(future);
    }

    // Wait for all tasks to complete and collect results
    int totalResult = 0;
    for (final Future<Integer> future : futures) {
      try {
        totalResult += future.get();
      } catch (final Exception e) {
        blackhole.consume(e.getMessage());
      }
    }

    blackhole.consume(totalResult);
    blackhole.consume(threadCount);
  }

  /**
   * Benchmarks concurrent memory operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkConcurrentMemoryOperations(final Blackhole blackhole) {
    final List<Future<Long>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      final Future<Long> future =
          executorService.submit(
              () -> {
                try {
                  final ConcurrentWasmContext context;
                  if (concurrencyPattern.equals("SHARED_RUNTIME")) {
                    context = new ConcurrentWasmContext(sharedEngine, moduleBytes);
                  } else {
                    context = new ConcurrentWasmContext(getRuntimeType(), moduleBytes);
                  }

                  final byte[] writeData = new byte[64];
                  for (int j = 0; j < writeData.length; j++) {
                    writeData[j] = (byte) ((threadId + j) % 256);
                  }

                  long checksum = 0;
                  for (int j = 0; j < 50; j++) {
                    final int offset = (threadId * 128) + (j % 8) * 64;

                    // Write memory
                    context.writeMemory(offset, writeData);

                    // Read memory back
                    final byte[] readData = context.readMemory(offset, writeData.length);
                    for (final byte b : readData) {
                      checksum += b & 0xFF;
                    }
                  }

                  context.close();
                  return checksum;
                } catch (final WasmException e) {
                  return -1L;
                }
              });
      futures.add(future);
    }

    // Wait for all tasks to complete and collect results
    long totalChecksum = 0;
    for (final Future<Long> future : futures) {
      try {
        totalChecksum += future.get();
      } catch (final Exception e) {
        blackhole.consume(e.getMessage());
      }
    }

    blackhole.consume(totalChecksum);
    blackhole.consume(threadCount);
  }

  /**
   * Benchmarks mixed concurrent operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMixedConcurrentOperations(final Blackhole blackhole) {
    final List<Future<String>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      final Future<String> future =
          executorService.submit(
              () -> {
                try {
                  final ConcurrentWasmContext context;
                  if (concurrencyPattern.equals("SHARED_RUNTIME")) {
                    context = new ConcurrentWasmContext(sharedEngine, moduleBytes);
                  } else {
                    context = new ConcurrentWasmContext(getRuntimeType(), moduleBytes);
                  }

                  final StringBuilder result = new StringBuilder();

                  for (int j = 0; j < 25; j++) {
                    // Function call
                    final WasmValue[] callResult = context.callFunction(threadId, j);
                    if (callResult.length > 0) {
                      result.append(callResult[0].asInt()).append(",");
                    }

                    // Memory operation
                    if (j % 5 == 0) {
                      final byte[] data = new byte[32];
                      for (int k = 0; k < data.length; k++) {
                        data[k] = (byte) ((threadId + j + k) % 256);
                      }
                      context.writeMemory(threadId * 64, data);

                      final byte[] readBack = context.readMemory(threadId * 64, data.length);
                      result.append("mem:").append(readBack.length).append(";");
                    }
                  }

                  context.close();
                  return result.toString();
                } catch (final WasmException e) {
                  return "error:" + e.getMessage();
                }
              });
      futures.add(future);
    }

    // Wait for all tasks to complete and collect results
    final List<String> results = new ArrayList<>();
    for (final Future<String> future : futures) {
      try {
        results.add(future.get());
      } catch (final Exception e) {
        results.add("exception:" + e.getMessage());
      }
    }

    blackhole.consume(results.size());
    blackhole.consume(threadCount);
    for (final String result : results) {
      blackhole.consume(result.length());
    }
  }

  /**
   * Benchmarks concurrent instance creation and destruction.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkConcurrentInstanceLifecycle(final Blackhole blackhole) {
    final List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      final Future<Integer> future =
          executorService.submit(
              () -> {
                int successCount = 0;

                // Create and destroy multiple instances per thread
                for (int j = 0; j < 10; j++) {
                  try {
                    final ConcurrentWasmContext context =
                        new ConcurrentWasmContext(getRuntimeType(), moduleBytes);

                    // Perform a simple operation
                    final WasmValue[] result = context.callFunction(threadId, j);
                    if (result.length > 0) {
                      successCount++;
                    }

                    context.close();
                  } catch (final WasmException e) {
                    // Count failures but continue
                  }
                }

                return successCount;
              });
      futures.add(future);
    }

    // Wait for all tasks to complete and collect results
    int totalSuccess = 0;
    for (final Future<Integer> future : futures) {
      try {
        totalSuccess += future.get();
      } catch (final Exception e) {
        blackhole.consume(e.getMessage());
      }
    }

    blackhole.consume(totalSuccess);
    blackhole.consume(threadCount);
  }

  /**
   * Benchmarks resource contention under high load.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkResourceContention(final Blackhole blackhole) {
    final List<CompletableFuture<Long>> futures = new ArrayList<>();

    // Create more tasks than threads to test contention
    final int taskCount = threadCount * 3;

    for (int i = 0; i < taskCount; i++) {
      final int taskId = i;
      final CompletableFuture<Long> future =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  final ConcurrentWasmContext context;
                  if (concurrencyPattern.equals("SHARED_RUNTIME")) {
                    context = new ConcurrentWasmContext(sharedEngine, moduleBytes);
                  } else {
                    context = new ConcurrentWasmContext(getRuntimeType(), moduleBytes);
                  }

                  final long startTime = System.nanoTime();

                  // Perform intensive operations
                  long result = 0;
                  for (int j = 0; j < 50; j++) {
                    final WasmValue[] callResult = context.callFunction(taskId % 100, j);
                    if (callResult.length > 0) {
                      result += callResult[0].asInt();
                    }
                  }

                  final long endTime = System.nanoTime();
                  context.close();

                  return endTime - startTime; // Return execution time
                } catch (final WasmException e) {
                  return -1L;
                }
              },
              executorService);

      futures.add(future);
    }

    // Wait for all tasks to complete
    long totalTime = 0;
    int completedTasks = 0;
    for (final CompletableFuture<Long> future : futures) {
      try {
        final Long time = future.get();
        if (time >= 0) {
          totalTime += time;
          completedTasks++;
        }
      } catch (final Exception e) {
        blackhole.consume(e.getMessage());
      }
    }

    blackhole.consume(totalTime);
    blackhole.consume(completedTasks);
    blackhole.consume(taskCount);
  }
}

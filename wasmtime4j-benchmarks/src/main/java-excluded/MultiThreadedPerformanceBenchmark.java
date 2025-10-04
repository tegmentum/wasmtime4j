package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmThread;
import ai.tegmentum.wasmtime4j.WasmThreadLocalStorage;
import ai.tegmentum.wasmtime4j.WasmThreadStatistics;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
 * Multi-threaded performance benchmarks for WebAssembly threading capabilities.
 *
 * <p>This benchmark class measures the performance characteristics of WebAssembly threading
 * operations, comparing different runtime implementations and configuration parameters across
 * various multi-threaded scenarios.
 *
 * <p>Key metrics measured:
 *
 * <ul>
 *   <li>Thread creation and destruction overhead
 *   <li>Concurrent function execution throughput
 *   <li>Shared memory access performance
 *   <li>Thread-local storage operations
 *   <li>Atomic operations and synchronization
 *   <li>Thread pool scalability
 * </ul>
 *
 * <p>Benchmarks are designed to stress-test the threading implementation and identify performance
 * bottlenecks under realistic workloads.
 *
 * @since 1.0.0
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms4g", "-Xmx8g", "-XX:+UseG1GC"})
public class MultiThreadedPerformanceBenchmark extends BenchmarkBase {

  /** Runtime implementation to benchmark. */
  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  /** Number of concurrent threads to use. */
  @Param({"1", "2", "4", "8", "16"})
  private int threadCount;

  /** Operations per thread for workload distribution. */
  @Param({"10", "100", "1000"})
  private int operationsPerThread;

  /** WebAssembly runtime components. */
  private WasmRuntime runtime;

  private Engine engine;
  private Store store;
  private Module module;
  private Instance instance;
  private WasmFunction addFunction;
  private WasmMemory sharedMemory;

  /** Module bytecode with shared memory support. */
  private static final byte[] THREADING_WASM_MODULE = {
    0x00,
    0x61,
    0x73,
    0x6d, // WASM magic
    0x01,
    0x00,
    0x00,
    0x00, // Version
    // Type section
    0x01,
    0x0b,
    0x02,
    0x60,
    0x02,
    0x7f,
    0x7f,
    0x01,
    0x7f, // Function type (i32, i32) -> i32
    0x60,
    0x03,
    0x7f,
    0x7f,
    0x7f,
    0x00, // Function type (i32, i32, i32) -> void
    // Function section
    0x03,
    0x03,
    0x02,
    0x00,
    0x01, // Two functions
    // Memory section - shared memory
    0x05,
    0x04,
    0x01,
    0x01,
    0x02,
    0x10, // Shared memory, min=2, max=16 pages
    // Export section
    0x07,
    0x1a,
    0x03,
    0x03,
    0x61,
    0x64,
    0x64,
    0x00,
    0x00, // Export "add" function
    0x0a,
    0x6d,
    0x65,
    0x6d,
    0x5f,
    0x77,
    0x72,
    0x69,
    0x74,
    0x65,
    0x00,
    0x01, // Export "mem_write"
    0x06,
    0x6d,
    0x65,
    0x6d,
    0x6f,
    0x72,
    0x79,
    0x02,
    0x00, // Export "memory"
    // Code section
    0x0a,
    0x17,
    0x02,
    // add function
    0x07,
    0x00,
    0x20,
    0x00,
    0x20,
    0x01,
    0x6a,
    0x0b,
    // mem_write function: (i32 offset, i32 value, i32 size)
    0x0c,
    0x00,
    0x20,
    0x00,
    0x20,
    0x01,
    0x36,
    0x02,
    0x00,
    0x0b // i32.store
  };

  /** Converts string runtime type name to RuntimeType enum. */
  private RuntimeType getRuntimeType() {
    return RuntimeType.valueOf(runtimeTypeName);
  }

  /** Setup performed before each benchmark iteration. */
  @Setup(Level.Iteration)
  public void setupIteration() throws WasmException {
    runtime = createRuntime(getRuntimeType());
    engine = createEngine(runtime);
    store = createStore(engine);
    module = compileModule(engine, THREADING_WASM_MODULE);
    instance = instantiateModule(store, module);

    addFunction =
        instance.getFunction("add").orElseThrow(() -> new WasmException("Add function not found"));

    sharedMemory =
        instance
            .getMemory("memory")
            .orElseThrow(() -> new WasmException("Memory export not found"));

    if (!sharedMemory.isShared()) {
      throw new WasmException("Memory must be shared for threading benchmarks");
    }

    // Force GC before benchmarking
    System.gc();
  }

  /** Cleanup performed after each benchmark iteration. */
  @TearDown(Level.Iteration)
  public void teardownIteration() {
    try {
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

  /**
   * Benchmarks thread creation and destruction overhead.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkThreadCreationDestruction(final Blackhole blackhole) {
    final List<WasmThread> threads = new ArrayList<>();

    try {
      // Create threads
      for (int i = 0; i < threadCount; i++) {
        final WasmThread thread = runtime.createThread(sharedMemory);
        threads.add(thread);
        blackhole.consume(thread.getThreadId());
      }

      // Verify threads are alive
      int aliveCount = 0;
      for (final WasmThread thread : threads) {
        if (thread.isAlive()) {
          aliveCount++;
        }
      }
      blackhole.consume(aliveCount);

    } catch (final Exception e) {
      blackhole.consume(e.getMessage());
    } finally {
      // Clean up threads
      for (final WasmThread thread : threads) {
        try {
          thread.close();
        } catch (final Exception e) {
          blackhole.consume(e.getMessage());
        }
      }
    }
  }

  /**
   * Benchmarks concurrent function execution throughput.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkConcurrentFunctionExecution(final Blackhole blackhole) {
    final List<WasmThread> threads = new ArrayList<>();
    final List<Future<Long>> futures = new ArrayList<>();

    try {
      // Create threads and execute functions concurrently
      for (int i = 0; i < threadCount; i++) {
        final WasmThread thread = runtime.createThread(sharedMemory);
        threads.add(thread);

        final int threadIndex = i;
        final CompletableFuture<Long> future =
            CompletableFuture.supplyAsync(
                () -> {
                  long totalResult = 0;
                  try {
                    for (int j = 0; j < operationsPerThread; j++) {
                      final Future<WasmValue[]> result =
                          thread.executeFunction(
                              addFunction, WasmValue.i32(threadIndex + j), WasmValue.i32(j * 2));

                      final WasmValue[] values = result.get();
                      totalResult += values[0].asInt();
                    }
                  } catch (final Exception e) {
                    return -1L;
                  }
                  return totalResult;
                });

        futures.add(future);
      }

      // Wait for all tasks to complete and collect results
      long grandTotal = 0;
      for (final Future<Long> future : futures) {
        try {
          final Long result = future.get();
          if (result >= 0) {
            grandTotal += result;
          }
        } catch (final Exception e) {
          blackhole.consume(e.getMessage());
        }
      }

      blackhole.consume(grandTotal);
      blackhole.consume(threadCount * operationsPerThread);

    } catch (final Exception e) {
      blackhole.consume(e.getMessage());
    } finally {
      // Clean up threads
      for (final WasmThread thread : threads) {
        try {
          thread.close();
        } catch (final Exception e) {
          blackhole.consume(e.getMessage());
        }
      }
    }
  }

  /**
   * Benchmarks shared memory access performance.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkSharedMemoryAccess(final Blackhole blackhole) {
    final List<WasmThread> threads = new ArrayList<>();
    final List<CompletableFuture<Integer>> futures = new ArrayList<>();

    try {
      // Create threads that will access shared memory
      for (int i = 0; i < threadCount; i++) {
        final WasmThread thread = runtime.createThread(sharedMemory);
        threads.add(thread);

        final int threadIndex = i;
        final CompletableFuture<Integer> future =
            thread.executeOperation(
                () -> {
                  try {
                    final WasmMemory memory = thread.getSharedMemory();
                    int checksum = 0;

                    for (int j = 0; j < operationsPerThread; j++) {
                      final int offset = (threadIndex * 1024) + (j % 64) * 4;

                      // Write data to memory
                      final byte[] writeData = {
                        (byte) (threadIndex & 0xFF),
                        (byte) (j & 0xFF),
                        (byte) ((threadIndex + j) & 0xFF),
                        (byte) ((threadIndex * j) & 0xFF)
                      };
                      memory.writeBytes(offset, writeData, 0, writeData.length);

                      // Read data back
                      final byte[] readData = new byte[4];
                      memory.readBytes(offset, readData, 0, readData.length);

                      // Calculate checksum
                      for (final byte b : readData) {
                        checksum += b & 0xFF;
                      }
                    }

                    return checksum;
                  } catch (final Exception e) {
                    return -1;
                  }
                });

        futures.add(future);
      }

      // Wait for all tasks to complete and collect results
      int totalChecksum = 0;
      for (final CompletableFuture<Integer> future : futures) {
        try {
          final Integer result = future.get();
          if (result >= 0) {
            totalChecksum += result;
          }
        } catch (final Exception e) {
          blackhole.consume(e.getMessage());
        }
      }

      blackhole.consume(totalChecksum);

    } catch (final Exception e) {
      blackhole.consume(e.getMessage());
    } finally {
      // Clean up threads
      for (final WasmThread thread : threads) {
        try {
          thread.close();
        } catch (final Exception e) {
          blackhole.consume(e.getMessage());
        }
      }
    }
  }

  /**
   * Benchmarks thread-local storage operations performance.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkThreadLocalStorage(final Blackhole blackhole) {
    final List<WasmThread> threads = new ArrayList<>();
    final List<CompletableFuture<Long>> futures = new ArrayList<>();

    try {
      // Create threads that will use thread-local storage
      for (int i = 0; i < threadCount; i++) {
        final WasmThread thread = runtime.createThread(sharedMemory);
        threads.add(thread);

        final int threadIndex = i;
        final CompletableFuture<Long> future =
            thread.executeOperation(
                () -> {
                  try {
                    final WasmThreadLocalStorage localStorage = thread.getThreadLocalStorage();
                    long operationCount = 0;

                    for (int j = 0; j < operationsPerThread; j++) {
                      final String keyPrefix = "thread_" + threadIndex + "_op_";

                      // Store various types of data
                      localStorage.putInt(keyPrefix + "int_" + j, threadIndex + j);
                      localStorage.putLong(keyPrefix + "long_" + j, (long) threadIndex * j);
                      localStorage.putFloat(
                          keyPrefix + "float_" + j, (float) threadIndex / (j + 1));
                      localStorage.putString(
                          keyPrefix + "string_" + j, "data_" + threadIndex + "_" + j);

                      // Read back and verify
                      final int intValue = localStorage.getInt(keyPrefix + "int_" + j);
                      final long longValue = localStorage.getLong(keyPrefix + "long_" + j);
                      final float floatValue = localStorage.getFloat(keyPrefix + "float_" + j);
                      final String stringValue = localStorage.getString(keyPrefix + "string_" + j);

                      if (intValue == threadIndex + j
                          && longValue == (long) threadIndex * j
                          && Math.abs(floatValue - (float) threadIndex / (j + 1)) < 0.001f
                          && ("data_" + threadIndex + "_" + j).equals(stringValue)) {
                        operationCount++;
                      }

                      // Periodically clean up to test removal
                      if (j > 0 && j % 10 == 0) {
                        localStorage.remove(keyPrefix + "int_" + (j - 10));
                        localStorage.remove(keyPrefix + "long_" + (j - 10));
                      }
                    }

                    // Final cleanup
                    localStorage.clear();

                    return operationCount;
                  } catch (final Exception e) {
                    return -1L;
                  }
                });

        futures.add(future);
      }

      // Wait for all tasks to complete and collect results
      long totalOperations = 0;
      for (final CompletableFuture<Long> future : futures) {
        try {
          final Long result = future.get();
          if (result >= 0) {
            totalOperations += result;
          }
        } catch (final Exception e) {
          blackhole.consume(e.getMessage());
        }
      }

      blackhole.consume(totalOperations);

    } catch (final Exception e) {
      blackhole.consume(e.getMessage());
    } finally {
      // Clean up threads
      for (final WasmThread thread : threads) {
        try {
          thread.close();
        } catch (final Exception e) {
          blackhole.consume(e.getMessage());
        }
      }
    }
  }

  /**
   * Benchmarks thread statistics collection overhead.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkThreadStatistics(final Blackhole blackhole) {
    final List<WasmThread> threads = new ArrayList<>();
    final AtomicLong statisticsCollected = new AtomicLong(0);

    try {
      // Create threads and collect statistics frequently
      for (int i = 0; i < threadCount; i++) {
        final WasmThread thread = runtime.createThread(sharedMemory);
        threads.add(thread);

        final int threadIndex = i;
        thread.executeOperation(
            () -> {
              try {
                for (int j = 0; j < operationsPerThread; j++) {
                  // Execute a function
                  final Future<WasmValue[]> result =
                      thread.executeFunction(
                          addFunction, WasmValue.i32(threadIndex), WasmValue.i32(j));
                  result.get();

                  // Collect statistics every few operations
                  if (j % 5 == 0) {
                    final WasmThreadStatistics stats = thread.getStatistics();
                    blackhole.consume(stats.getFunctionsExecuted());
                    blackhole.consume(stats.getTotalExecutionTime());
                    blackhole.consume(stats.getOperationsPerSecond());
                    statisticsCollected.incrementAndGet();
                  }
                }
                return statisticsCollected.get();
              } catch (final Exception e) {
                return -1L;
              }
            });
      }

      // Wait briefly for operations to complete
      Thread.sleep(100);

      blackhole.consume(statisticsCollected.get());

    } catch (final Exception e) {
      blackhole.consume(e.getMessage());
    } finally {
      // Clean up threads
      for (final WasmThread thread : threads) {
        try {
          thread.close();
        } catch (final Exception e) {
          blackhole.consume(e.getMessage());
        }
      }
    }
  }

  /**
   * Benchmarks thread termination and cleanup performance.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkThreadTermination(final Blackhole blackhole) {
    final List<WasmThread> threads = new ArrayList<>();
    final List<CompletableFuture<Void>> terminationFutures = new ArrayList<>();

    try {
      // Create threads
      for (int i = 0; i < threadCount; i++) {
        final WasmThread thread = runtime.createThread(sharedMemory);
        threads.add(thread);

        // Start a long-running operation
        thread.executeOperation(
            () -> {
              try {
                for (int j = 0;
                    j < operationsPerThread * 10 && !thread.isTerminationRequested();
                    j++) {
                  Thread.sleep(1);
                }
                return "completed";
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return "interrupted";
              }
            });
      }

      // Request termination for all threads
      for (final WasmThread thread : threads) {
        final CompletableFuture<Void> termination = thread.terminate();
        terminationFutures.add(termination);
      }

      // Wait for all terminations to complete
      int completedTerminations = 0;
      for (final CompletableFuture<Void> future : terminationFutures) {
        try {
          future.get();
          completedTerminations++;
        } catch (final Exception e) {
          blackhole.consume(e.getMessage());
        }
      }

      blackhole.consume(completedTerminations);

    } catch (final Exception e) {
      blackhole.consume(e.getMessage());
    } finally {
      // Clean up threads
      for (final WasmThread thread : threads) {
        try {
          thread.close();
        } catch (final Exception e) {
          blackhole.consume(e.getMessage());
        }
      }
    }
  }
}

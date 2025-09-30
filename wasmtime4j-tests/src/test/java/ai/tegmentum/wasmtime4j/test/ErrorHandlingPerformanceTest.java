package ai.tegmentum.wasmtime4j.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Performance test harness for measuring error handling overhead.
 *
 * <p>This test class measures the performance impact of error handling in various scenarios,
 * ensuring that error processing doesn't introduce significant overhead to normal operations and
 * that error handling itself is efficient.
 */
@DisplayName("Error Handling Performance Test Harness")
class ErrorHandlingPerformanceTest {

  /** Simple valid WebAssembly module for baseline performance measurements. */
  private static final byte[] VALID_MODULE = {
    0x00,
    0x61,
    0x73,
    0x6d,
    0x01,
    0x00,
    0x00,
    0x00, // Magic + version
    0x01, // Type section
    0x05, // Section size
    0x01, // 1 type
    0x60,
    0x00,
    0x01,
    0x7F, // Function type: () -> i32
    0x03, // Function section
    0x02, // Section size
    0x01,
    0x00, // 1 function with type index 0
    0x07, // Export section
    0x08, // Section size
    0x01, // 1 export
    0x04,
    't',
    'e',
    's',
    't', // Export name "test"
    0x00,
    0x00, // Function export with index 0
    0x0A, // Code section
    0x06, // Section size
    0x01, // 1 function body
    0x04, // Body size
    0x00, // No locals
    0x41,
    0x2A, // i32.const 42
    0x0B // End instruction
  };

  /** WebAssembly module that triggers trap (unreachable instruction). */
  private static final byte[] TRAP_MODULE = {
    0x00,
    0x61,
    0x73,
    0x6d,
    0x01,
    0x00,
    0x00,
    0x00, // Magic + version
    0x01, // Type section
    0x04, // Section size
    0x01, // 1 type
    0x60,
    0x00,
    0x00, // Function type: () -> ()
    0x03, // Function section
    0x02, // Section size
    0x01,
    0x00, // 1 function with type index 0
    0x07, // Export section
    0x08, // Section size
    0x01, // 1 export
    0x04,
    't',
    'r',
    'a',
    'p', // Export name "trap"
    0x00,
    0x00, // Function export with index 0
    0x0A, // Code section
    0x05, // Section size
    0x01, // 1 function body
    0x03, // Body size
    0x00, // No locals
    0x00, // unreachable instruction
    0x0B // End instruction
  };

  /** Invalid WebAssembly module (wrong magic number). */
  private static final byte[] INVALID_MODULE = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

  /** Performance measurement result. */
  private static class PerformanceResult {
    final long totalTime;
    final long operationCount;
    final double averageTime;
    final long minTime;
    final long maxTime;
    final String operation;

    PerformanceResult(String operation, List<Long> times) {
      this.operation = operation;
      this.operationCount = times.size();
      this.totalTime = times.stream().mapToLong(Long::longValue).sum();
      this.averageTime = (double) totalTime / operationCount;
      this.minTime = times.stream().mapToLong(Long::longValue).min().orElse(0);
      this.maxTime = times.stream().mapToLong(Long::longValue).max().orElse(0);
    }

    @Override
    public String toString() {
      return String.format(
          "%s: avg=%.2fns, min=%dns, max=%dns, total=%dns (%d ops)",
          operation, averageTime, minTime, maxTime, totalTime, operationCount);
    }
  }

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @DisplayName("Baseline performance measurement for normal operations")
  void testBaselinePerformance(RuntimeType runtimeType) throws WasmException {
    if (!WasmRuntimeFactory.isRuntimeAvailable(runtimeType)) {
      return; // Skip if runtime not available
    }

    try (WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module module = runtime.compileModule(engine, VALID_MODULE);
      Instance instance = runtime.instantiateModule(store, module);

      // Warm up
      for (int i = 0; i < 100; i++) {
        instance.getExportedFunction("test").call();
      }

      // Measure normal function calls
      List<Long> callTimes = new ArrayList<>();
      for (int i = 0; i < 1000; i++) {
        long start = System.nanoTime();
        Object result = instance.getExportedFunction("test").call();
        long end = System.nanoTime();
        callTimes.add(end - start);
        assertNotNull(result, "Function should return result");
      }

      PerformanceResult callResult = new PerformanceResult("Normal function call", callTimes);
      System.out.println("Baseline performance for " + runtimeType + ": " + callResult);

      // Verify performance is reasonable (less than 10ms average)
      assertTrue(
          callResult.averageTime < TimeUnit.MILLISECONDS.toNanos(10),
          "Normal function calls should be fast: " + callResult.averageTime + "ns average");
    }
  }

  @Test
  @DisplayName("Error handling overhead measurement")
  void testErrorHandlingOverhead() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
      Instance trapInstance = runtime.instantiateModule(store, trapModule);

      // Warm up error handling
      for (int i = 0; i < 100; i++) {
        try {
          trapInstance.getExportedFunction("trap").call();
        } catch (WasmException e) {
          // Expected
        }
      }

      // Measure error handling performance
      List<Long> errorTimes = new ArrayList<>();
      for (int i = 0; i < 1000; i++) {
        long start = System.nanoTime();
        try {
          trapInstance.getExportedFunction("trap").call();
        } catch (WasmException e) {
          long end = System.nanoTime();
          errorTimes.add(end - start);
          assertNotNull(e.getMessage(), "Exception should have message");
        }
      }

      PerformanceResult errorResult = new PerformanceResult("Error handling", errorTimes);
      System.out.println("Error handling performance: " + errorResult);

      // Error handling should complete within reasonable time (less than 1ms average)
      assertTrue(
          errorResult.averageTime < TimeUnit.MILLISECONDS.toNanos(1),
          "Error handling should be efficient: " + errorResult.averageTime + "ns average");

      // No single error should take excessively long (less than 10ms)
      assertTrue(
          errorResult.maxTime < TimeUnit.MILLISECONDS.toNanos(10),
          "Maximum error handling time should be reasonable: " + errorResult.maxTime + "ns");
    }
  }

  @Test
  @DisplayName("Compilation error performance measurement")
  void testCompilationErrorPerformance() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Warm up compilation error handling
      for (int i = 0; i < 100; i++) {
        try {
          runtime.compileModule(engine, INVALID_MODULE);
        } catch (WasmException e) {
          // Expected
        }
      }

      // Measure compilation error performance
      List<Long> compilationErrorTimes = new ArrayList<>();
      for (int i = 0; i < 500; i++) {
        long start = System.nanoTime();
        try {
          runtime.compileModule(engine, INVALID_MODULE);
        } catch (WasmException e) {
          long end = System.nanoTime();
          compilationErrorTimes.add(end - start);
          assertNotNull(e.getMessage(), "Exception should have message");
        }
      }

      PerformanceResult compilationResult =
          new PerformanceResult("Compilation error", compilationErrorTimes);
      System.out.println("Compilation error performance: " + compilationResult);

      // Compilation errors should be processed efficiently (less than 100ms average)
      assertTrue(
          compilationResult.averageTime < TimeUnit.MILLISECONDS.toNanos(100),
          "Compilation error handling should be efficient: "
              + compilationResult.averageTime
              + "ns average");
    }
  }

  @Test
  @DisplayName("Error recovery performance measurement")
  void testErrorRecoveryPerformance() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      Module validModule = runtime.compileModule(engine, VALID_MODULE);
      Module trapModule = runtime.compileModule(engine, TRAP_MODULE);

      Instance validInstance = runtime.instantiateModule(store, validModule);
      Instance trapInstance = runtime.instantiateModule(store, trapModule);

      // Warm up
      for (int i = 0; i < 100; i++) {
        try {
          trapInstance.getExportedFunction("trap").call();
        } catch (WasmException e) {
          // Recovery: call valid function
          validInstance.getExportedFunction("test").call();
        }
      }

      // Measure error recovery scenarios
      List<Long> recoveryTimes = new ArrayList<>();
      for (int i = 0; i < 500; i++) {
        long start = System.nanoTime();

        // Trigger error
        try {
          trapInstance.getExportedFunction("trap").call();
        } catch (WasmException e) {
          // Recover by calling valid function
          validInstance.getExportedFunction("test").call();
        }

        long end = System.nanoTime();
        recoveryTimes.add(end - start);
      }

      PerformanceResult recoveryResult = new PerformanceResult("Error recovery", recoveryTimes);
      System.out.println("Error recovery performance: " + recoveryResult);

      // Error recovery should be efficient (less than 10ms average)
      assertTrue(
          recoveryResult.averageTime < TimeUnit.MILLISECONDS.toNanos(10),
          "Error recovery should be efficient: " + recoveryResult.averageTime + "ns average");
    }
  }

  @Test
  @DisplayName("Concurrent error handling performance")
  void testConcurrentErrorHandlingPerformance() throws InterruptedException, WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      final int threadCount = 10;
      final int operationsPerThread = 100;
      final CountDownLatch latch = new CountDownLatch(threadCount);
      final AtomicLong totalErrorTime = new AtomicLong(0);
      final AtomicLong errorCount = new AtomicLong(0);

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      long testStart = System.nanoTime();

      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                Store store = runtime.createStore(engine);
                Module module = runtime.compileModule(engine, TRAP_MODULE);
                Instance instance = runtime.instantiateModule(store, module);

                for (int j = 0; j < operationsPerThread; j++) {
                  long start = System.nanoTime();
                  try {
                    instance.getExportedFunction("trap").call();
                  } catch (WasmException e) {
                    long end = System.nanoTime();
                    totalErrorTime.addAndGet(end - start);
                    errorCount.incrementAndGet();
                  }
                }
              } catch (Exception e) {
                // Unexpected error
                e.printStackTrace();
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(
          latch.await(30, TimeUnit.SECONDS), "All threads should complete within 30 seconds");
      executor.shutdown();

      long testEnd = System.nanoTime();
      long testDuration = testEnd - testStart;

      double averageErrorTime = (double) totalErrorTime.get() / errorCount.get();
      double throughput =
          (double) errorCount.get() / (testDuration / 1_000_000_000.0); // errors/sec

      System.out.printf(
          "Concurrent error handling: %.2f errors/sec, %.2fns avg per error%n",
          throughput, averageErrorTime);

      // Should maintain reasonable performance under concurrency
      assertTrue(
          averageErrorTime < TimeUnit.MILLISECONDS.toNanos(10),
          "Concurrent error handling should remain efficient: " + averageErrorTime + "ns average");

      assertTrue(
          throughput > 100,
          "Should maintain reasonable error handling throughput: " + throughput + " errors/sec");
    }
  }

  @Test
  @DisplayName("Memory allocation overhead during error handling")
  void testErrorHandlingMemoryOverhead() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module module = runtime.compileModule(engine, TRAP_MODULE);
      Instance instance = runtime.instantiateModule(store, module);

      // Force garbage collection and measure initial memory
      System.gc();
      Thread.yield();
      long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      // Perform many error operations
      List<Long> allocationTimes = new ArrayList<>();
      for (int i = 0; i < 1000; i++) {
        long start = System.nanoTime();
        try {
          instance.getExportedFunction("trap").call();
        } catch (WasmException e) {
          long end = System.nanoTime();
          allocationTimes.add(end - start);
          // Force exception to be processed
          String message = e.getMessage();
          assertNotNull(message, "Exception should have message");
        }

        // Periodic garbage collection
        if (i % 200 == 0) {
          System.gc();
          Thread.yield();
        }
      }

      // Force garbage collection and measure final memory
      System.gc();
      Thread.yield();
      long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      PerformanceResult allocationResult =
          new PerformanceResult("Error allocation", allocationTimes);
      System.out.println("Error allocation performance: " + allocationResult);

      long memoryIncrease = finalMemory - initialMemory;
      System.out.println(
          "Memory usage: initial="
              + initialMemory
              + ", final="
              + finalMemory
              + ", increase="
              + memoryIncrease);

      // Memory increase should be minimal (less than 10MB)
      assertTrue(
          memoryIncrease < 10 * 1024 * 1024,
          "Error handling should not cause significant memory overhead: " + memoryIncrease);

      // Error allocation should be efficient
      assertTrue(
          allocationResult.averageTime < TimeUnit.MICROSECONDS.toNanos(500),
          "Error allocation should be fast: " + allocationResult.averageTime + "ns average");
    }
  }

  @Test
  @DisplayName("Error handling scalability under load")
  void testErrorHandlingScalability() throws InterruptedException, WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Test different load levels
      int[] threadCounts = {1, 2, 5, 10, 20};
      List<Double> throughputs = new ArrayList<>();

      for (int threadCount : threadCounts) {
        double throughput = measureErrorThroughput(runtime, engine, threadCount, 50);
        throughputs.add(throughput);
        System.out.printf("Throughput with %d threads: %.2f errors/sec%n", threadCount, throughput);
      }

      // Throughput should scale reasonably (shouldn't decrease dramatically with more threads)
      double singleThreadThroughput = throughputs.get(0);
      double multiThreadThroughput = throughputs.get(throughputs.size() - 1);

      // Allow some degradation but not more than 50%
      assertTrue(
          multiThreadThroughput > singleThreadThroughput * 0.5,
          "Error handling should scale reasonably under load. Single: "
              + singleThreadThroughput
              + ", Multi: "
              + multiThreadThroughput);
    }
  }

  private double measureErrorThroughput(
      WasmRuntime runtime, Engine engine, int threadCount, int operationsPerThread)
      throws InterruptedException, WasmException {

    final CountDownLatch latch = new CountDownLatch(threadCount);
    final AtomicLong errorCount = new AtomicLong(0);

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    long start = System.nanoTime();

    for (int i = 0; i < threadCount; i++) {
      executor.submit(
          () -> {
            try {
              Store store = runtime.createStore(engine);
              Module module = runtime.compileModule(engine, TRAP_MODULE);
              Instance instance = runtime.instantiateModule(store, module);

              for (int j = 0; j < operationsPerThread; j++) {
                try {
                  instance.getExportedFunction("trap").call();
                } catch (WasmException e) {
                  errorCount.incrementAndGet();
                }
              }
            } catch (Exception e) {
              // Unexpected error
              e.printStackTrace();
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await(60, TimeUnit.SECONDS);
    executor.shutdown();

    long end = System.nanoTime();
    double duration = (end - start) / 1_000_000_000.0; // seconds

    return errorCount.get() / duration; // errors per second
  }

  @Test
  @DisplayName("Error message generation performance")
  void testErrorMessageGenerationPerformance() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module module = runtime.compileModule(engine, TRAP_MODULE);
      Instance instance = runtime.instantiateModule(store, module);

      // Collect many exceptions
      List<WasmException> exceptions = new ArrayList<>();
      for (int i = 0; i < 1000; i++) {
        try {
          instance.getExportedFunction("trap").call();
        } catch (WasmException e) {
          exceptions.add(e);
        }
      }

      // Measure message generation performance
      List<Long> messageTimes = new ArrayList<>();
      for (WasmException e : exceptions) {
        long start = System.nanoTime();
        String message = e.getMessage();
        long end = System.nanoTime();
        messageTimes.add(end - start);
        assertNotNull(message, "Exception should have message");
        assertTrue(message.length() > 0, "Message should not be empty");
      }

      PerformanceResult messageResult = new PerformanceResult("Message generation", messageTimes);
      System.out.println("Error message generation performance: " + messageResult);

      // Message generation should be very fast (less than 10µs average)
      assertTrue(
          messageResult.averageTime < TimeUnit.MICROSECONDS.toNanos(10),
          "Error message generation should be fast: " + messageResult.averageTime + "ns average");
    }
  }
}

package ai.tegmentum.wasmtime4j.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Memory leak detection tests for error scenarios.
 *
 * <p>This test class validates that error handling does not introduce memory leaks, ensuring that
 * resources are properly cleaned up even when exceptions occur during WebAssembly operations.
 * Tests cover various error scenarios including compilation failures, runtime traps, and resource
 * exhaustion conditions.
 */
@DisplayName("Error Memory Leak Detection Tests")
class ErrorMemoryLeakDetectionTest {

  /** Simple valid WebAssembly module. */
  private static final byte[] VALID_MODULE = {
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
    0x01, // Type section
    0x05, // Section size
    0x01, // 1 type
    0x60, 0x00, 0x01, 0x7F, // Function type: () -> i32
    0x03, // Function section
    0x02, // Section size
    0x01, 0x00, // 1 function with type index 0
    0x07, // Export section
    0x08, // Section size
    0x01, // 1 export
    0x04, 't', 'e', 's', 't', // Export name "test"
    0x00, 0x00, // Function export with index 0
    0x0A, // Code section
    0x06, // Section size
    0x01, // 1 function body
    0x04, // Body size
    0x00, // No locals
    0x41, 0x2A, // i32.const 42
    0x0B // End instruction
  };

  /** WebAssembly module that triggers a trap. */
  private static final byte[] TRAP_MODULE = {
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
    0x01, // Type section
    0x04, // Section size
    0x01, // 1 type
    0x60, 0x00, 0x00, // Function type: () -> ()
    0x03, // Function section
    0x02, // Section size
    0x01, 0x00, // 1 function with type index 0
    0x07, // Export section
    0x08, // Section size
    0x01, // 1 export
    0x04, 't', 'r', 'a', 'p', // Export name "trap"
    0x00, 0x00, // Function export with index 0
    0x0A, // Code section
    0x05, // Section size
    0x01, // 1 function body
    0x03, // Body size
    0x00, // No locals
    0x00, // unreachable instruction
    0x0B // End instruction
  };

  /** Invalid WebAssembly module for compilation errors. */
  private static final byte[] INVALID_MODULE = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

  /** Memory threshold for leak detection (50MB). */
  private static final long MEMORY_LEAK_THRESHOLD = 50 * 1024 * 1024;

  /** Number of iterations for leak detection tests. */
  private static final int LEAK_TEST_ITERATIONS = 1000;

  /**
   * Memory measurement utility.
   */
  private static class MemoryMeasurement {
    final long heapUsed;
    final long heapCommitted;
    final long nonHeapUsed;
    final long timestamp;

    MemoryMeasurement() {
      MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
      MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
      MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

      this.heapUsed = heapUsage.getUsed();
      this.heapCommitted = heapUsage.getCommitted();
      this.nonHeapUsed = nonHeapUsage.getUsed();
      this.timestamp = System.currentTimeMillis();
    }

    long totalUsed() {
      return heapUsed + nonHeapUsed;
    }

    @Override
    public String toString() {
      return String.format(
          "Memory{heap=%d, nonHeap=%d, total=%d, committed=%d}",
          heapUsed, nonHeapUsed, totalUsed(), heapCommitted);
    }
  }

  /**
   * Forces garbage collection and waits for it to complete.
   */
  private static void forceGarbageCollection() {
    System.gc();
    System.runFinalization();
    try {
      Thread.sleep(100); // Give GC time to complete
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @DisplayName("Compilation error memory leak detection")
  void testCompilationErrorMemoryLeaks(RuntimeType runtimeType) throws WasmException {
    if (!WasmRuntimeFactory.isRuntimeAvailable(runtimeType)) {
      return; // Skip if runtime not available
    }

    try (WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      Engine engine = runtime.createEngine();

      // Measure initial memory
      forceGarbageCollection();
      MemoryMeasurement initialMemory = new MemoryMeasurement();
      System.out.println("Initial memory for compilation errors: " + initialMemory);

      // Perform many compilation error operations
      for (int i = 0; i < LEAK_TEST_ITERATIONS; i++) {
        try {
          runtime.compileModule(engine, INVALID_MODULE);
          // Should not reach here
        } catch (WasmException e) {
          assertNotNull(e.getMessage(), "Exception should have message");
          // Let exception be garbage collected
        }

        // Periodic garbage collection
        if (i % 100 == 0) {
          forceGarbageCollection();
        }
      }

      // Final garbage collection and memory measurement
      forceGarbageCollection();
      MemoryMeasurement finalMemory = new MemoryMeasurement();
      System.out.println("Final memory for compilation errors: " + finalMemory);

      long memoryIncrease = finalMemory.totalUsed() - initialMemory.totalUsed();
      System.out.println(
          "Memory increase for "
              + LEAK_TEST_ITERATIONS
              + " compilation errors: "
              + memoryIncrease
              + " bytes");

      assertTrue(
          memoryIncrease < MEMORY_LEAK_THRESHOLD,
          "Compilation errors should not cause memory leaks. Increase: "
              + memoryIncrease
              + " bytes");
    }
  }

  @Test
  @DisplayName("Runtime error memory leak detection")
  void testRuntimeErrorMemoryLeaks() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
      Instance trapInstance = runtime.instantiateModule(store, trapModule);

      // Measure initial memory
      forceGarbageCollection();
      MemoryMeasurement initialMemory = new MemoryMeasurement();
      System.out.println("Initial memory for runtime errors: " + initialMemory);

      // Perform many runtime error operations
      for (int i = 0; i < LEAK_TEST_ITERATIONS; i++) {
        try {
          trapInstance.getExportedFunction("trap").call();
          // Should not reach here
        } catch (WasmException e) {
          assertNotNull(e.getMessage(), "Exception should have message");
          // Let exception be garbage collected
        }

        // Periodic garbage collection
        if (i % 100 == 0) {
          forceGarbageCollection();
        }
      }

      // Final garbage collection and memory measurement
      forceGarbageCollection();
      MemoryMeasurement finalMemory = new MemoryMeasurement();
      System.out.println("Final memory for runtime errors: " + finalMemory);

      long memoryIncrease = finalMemory.totalUsed() - initialMemory.totalUsed();
      System.out.println(
          "Memory increase for "
              + LEAK_TEST_ITERATIONS
              + " runtime errors: "
              + memoryIncrease
              + " bytes");

      assertTrue(
          memoryIncrease < MEMORY_LEAK_THRESHOLD,
          "Runtime errors should not cause memory leaks. Increase: " + memoryIncrease + " bytes");
    }
  }

  @Test
  @DisplayName("Instance creation failure memory leak detection")
  void testInstanceCreationFailureMemoryLeaks() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Measure initial memory
      forceGarbageCollection();
      MemoryMeasurement initialMemory = new MemoryMeasurement();
      System.out.println("Initial memory for instance creation failures: " + initialMemory);

      // Perform many instance creation operations
      for (int i = 0; i < LEAK_TEST_ITERATIONS / 10; i++) { // Fewer iterations for instance creation
        Store store = runtime.createStore(engine);

        try {
          // Try to compile and instantiate invalid module
          Module module = runtime.compileModule(engine, INVALID_MODULE);
          runtime.instantiateModule(store, module);
          // Should not reach here
        } catch (WasmException e) {
          assertNotNull(e.getMessage(), "Exception should have message");
          // Let objects be garbage collected
        }

        // More frequent garbage collection for this test
        if (i % 20 == 0) {
          forceGarbageCollection();
        }
      }

      // Final garbage collection and memory measurement
      forceGarbageCollection();
      MemoryMeasurement finalMemory = new MemoryMeasurement();
      System.out.println("Final memory for instance creation failures: " + finalMemory);

      long memoryIncrease = finalMemory.totalUsed() - initialMemory.totalUsed();
      System.out.println(
          "Memory increase for instance creation failures: " + memoryIncrease + " bytes");

      assertTrue(
          memoryIncrease < MEMORY_LEAK_THRESHOLD,
          "Instance creation failures should not cause memory leaks. Increase: "
              + memoryIncrease
              + " bytes");
    }
  }

  @Test
  @DisplayName("Exception object memory leak detection")
  void testExceptionObjectMemoryLeaks() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
      Instance trapInstance = runtime.instantiateModule(store, trapModule);

      // Collect weak references to exceptions to detect if they're being held
      List<WeakReference<WasmException>> exceptionRefs = new ArrayList<>();

      // Measure initial memory
      forceGarbageCollection();
      MemoryMeasurement initialMemory = new MemoryMeasurement();
      System.out.println("Initial memory for exception objects: " + initialMemory);

      // Create many exceptions
      for (int i = 0; i < LEAK_TEST_ITERATIONS; i++) {
        try {
          trapInstance.getExportedFunction("trap").call();
        } catch (WasmException e) {
          // Store weak reference to exception
          exceptionRefs.add(new WeakReference<>(e));

          // Validate exception properties
          assertNotNull(e.getMessage(), "Exception should have message");
          assertNotNull(e.getStackTrace(), "Exception should have stack trace");

          // Don't hold strong reference to exception
        }

        // Periodic cleanup
        if (i % 100 == 0) {
          forceGarbageCollection();

          // Check how many exceptions are still referenced
          long stillReferenced = exceptionRefs.stream().mapToLong(ref -> ref.get() != null ? 1 : 0).sum();
          System.out.println("Exceptions still referenced after GC: " + stillReferenced + "/" + exceptionRefs.size());
        }
      }

      // Final garbage collection
      forceGarbageCollection();
      MemoryMeasurement finalMemory = new MemoryMeasurement();
      System.out.println("Final memory for exception objects: " + finalMemory);

      // Check how many exceptions are still referenced after final GC
      long finalReferencedCount = exceptionRefs.stream().mapToLong(ref -> ref.get() != null ? 1 : 0).sum();
      System.out.println("Final exceptions still referenced: " + finalReferencedCount + "/" + exceptionRefs.size());

      // Most exceptions should be garbage collected (allow 5% to remain)
      assertTrue(
          finalReferencedCount < exceptionRefs.size() * 0.05,
          "Most exception objects should be garbage collected. Still referenced: "
              + finalReferencedCount
              + "/" + exceptionRefs.size());

      long memoryIncrease = finalMemory.totalUsed() - initialMemory.totalUsed();
      System.out.println("Memory increase for exception objects: " + memoryIncrease + " bytes");

      assertTrue(
          memoryIncrease < MEMORY_LEAK_THRESHOLD,
          "Exception objects should not cause memory leaks. Increase: "
              + memoryIncrease
              + " bytes");
    }
  }

  @Test
  @DisplayName("Concurrent error memory leak detection")
  void testConcurrentErrorMemoryLeaks() throws InterruptedException, WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Measure initial memory
      forceGarbageCollection();
      MemoryMeasurement initialMemory = new MemoryMeasurement();
      System.out.println("Initial memory for concurrent errors: " + initialMemory);

      final int threadCount = 10;
      final int operationsPerThread = 100;
      final CountDownLatch latch = new CountDownLatch(threadCount);
      final AtomicInteger totalExceptions = new AtomicInteger(0);

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                Store store = runtime.createStore(engine);
                Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
                Instance trapInstance = runtime.instantiateModule(store, trapModule);

                for (int j = 0; j < operationsPerThread; j++) {
                  try {
                    trapInstance.getExportedFunction("trap").call();
                  } catch (WasmException e) {
                    totalExceptions.incrementAndGet();
                    // Process exception but don't hold reference
                    String message = e.getMessage();
                    assertNotNull(message, "Exception should have message");
                  }
                }
              } catch (Exception e) {
                e.printStackTrace();
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(latch.await(60, TimeUnit.SECONDS), "All threads should complete");
      executor.shutdown();

      // Force garbage collection after concurrent operations
      forceGarbageCollection();
      MemoryMeasurement finalMemory = new MemoryMeasurement();
      System.out.println("Final memory for concurrent errors: " + finalMemory);

      long memoryIncrease = finalMemory.totalUsed() - initialMemory.totalUsed();
      System.out.println(
          "Memory increase for "
              + totalExceptions.get()
              + " concurrent errors: "
              + memoryIncrease
              + " bytes");

      assertTrue(
          memoryIncrease < MEMORY_LEAK_THRESHOLD,
          "Concurrent errors should not cause memory leaks. Increase: "
              + memoryIncrease
              + " bytes");
    }
  }

  @Test
  @DisplayName("Mixed error scenario memory leak detection")
  void testMixedErrorScenarioMemoryLeaks() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Measure initial memory
      forceGarbageCollection();
      MemoryMeasurement initialMemory = new MemoryMeasurement();
      System.out.println("Initial memory for mixed error scenarios: " + initialMemory);

      // Mix of different error types
      for (int i = 0; i < LEAK_TEST_ITERATIONS / 5; i++) {
        // Compilation error
        try {
          runtime.compileModule(engine, INVALID_MODULE);
        } catch (WasmException e) {
          assertNotNull(e.getMessage(), "Compilation exception should have message");
        }

        // Runtime error
        if (i % 2 == 0) {
          try {
            Store store = runtime.createStore(engine);
            Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
            Instance trapInstance = runtime.instantiateModule(store, trapModule);
            trapInstance.getExportedFunction("trap").call();
          } catch (WasmException e) {
            assertNotNull(e.getMessage(), "Runtime exception should have message");
          }
        }

        // Normal operation (for comparison)
        if (i % 3 == 0) {
          Store store = runtime.createStore(engine);
          Module validModule = runtime.compileModule(engine, VALID_MODULE);
          Instance validInstance = runtime.instantiateModule(store, validModule);
          Object result = validInstance.getExportedFunction("test").call();
          assertNotNull(result, "Valid operation should return result");
        }

        // Periodic garbage collection
        if (i % 20 == 0) {
          forceGarbageCollection();
        }
      }

      // Final garbage collection and memory measurement
      forceGarbageCollection();
      MemoryMeasurement finalMemory = new MemoryMeasurement();
      System.out.println("Final memory for mixed error scenarios: " + finalMemory);

      long memoryIncrease = finalMemory.totalUsed() - initialMemory.totalUsed();
      System.out.println("Memory increase for mixed error scenarios: " + memoryIncrease + " bytes");

      assertTrue(
          memoryIncrease < MEMORY_LEAK_THRESHOLD,
          "Mixed error scenarios should not cause memory leaks. Increase: "
              + memoryIncrease
              + " bytes");
    }
  }

  @Test
  @DisplayName("Error recovery memory leak detection")
  void testErrorRecoveryMemoryLeaks() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Measure initial memory
      forceGarbageCollection();
      MemoryMeasurement initialMemory = new MemoryMeasurement();
      System.out.println("Initial memory for error recovery: " + initialMemory);

      // Error/recovery cycles
      for (int i = 0; i < LEAK_TEST_ITERATIONS / 10; i++) {
        Store store = runtime.createStore(engine);

        // Compile modules
        Module validModule = runtime.compileModule(engine, VALID_MODULE);
        Module trapModule = runtime.compileModule(engine, TRAP_MODULE);

        // Create instances
        Instance validInstance = runtime.instantiateModule(store, validModule);
        Instance trapInstance = runtime.instantiateModule(store, trapModule);

        // Pattern: valid -> error -> recovery -> error -> recovery
        Object result1 = validInstance.getExportedFunction("test").call();
        assertNotNull(result1, "Valid operation should succeed");

        try {
          trapInstance.getExportedFunction("trap").call();
        } catch (WasmException e) {
          assertNotNull(e.getMessage(), "Trap exception should have message");
        }

        Object result2 = validInstance.getExportedFunction("test").call();
        assertNotNull(result2, "Recovery should succeed");

        try {
          trapInstance.getExportedFunction("trap").call();
        } catch (WasmException e) {
          assertNotNull(e.getMessage(), "Second trap exception should have message");
        }

        Object result3 = validInstance.getExportedFunction("test").call();
        assertNotNull(result3, "Second recovery should succeed");

        // Periodic garbage collection
        if (i % 10 == 0) {
          forceGarbageCollection();
        }
      }

      // Final garbage collection and memory measurement
      forceGarbageCollection();
      MemoryMeasurement finalMemory = new MemoryMeasurement();
      System.out.println("Final memory for error recovery: " + finalMemory);

      long memoryIncrease = finalMemory.totalUsed() - initialMemory.totalUsed();
      System.out.println("Memory increase for error recovery cycles: " + memoryIncrease + " bytes");

      assertTrue(
          memoryIncrease < MEMORY_LEAK_THRESHOLD,
          "Error recovery should not cause memory leaks. Increase: "
              + memoryIncrease
              + " bytes");
    }
  }

  @Test
  @DisplayName("Long-running error scenario memory stability")
  void testLongRunningErrorMemoryStability() throws WasmException, InterruptedException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
      Instance trapInstance = runtime.instantiateModule(store, trapModule);

      // Measure memory over time during long-running error scenarios
      List<MemoryMeasurement> measurements = new ArrayList<>();

      forceGarbageCollection();
      measurements.add(new MemoryMeasurement());
      System.out.println("Starting long-running error memory stability test");

      final long testDurationMs = 30 * 1000; // 30 seconds
      final long startTime = System.currentTimeMillis();
      int operationCount = 0;

      while (System.currentTimeMillis() - startTime < testDurationMs) {
        // Trigger error
        try {
          trapInstance.getExportedFunction("trap").call();
        } catch (WasmException e) {
          assertNotNull(e.getMessage(), "Exception should have message");
        }

        operationCount++;

        // Take memory measurement every 5 seconds
        if (operationCount % 1000 == 0) {
          forceGarbageCollection();
          measurements.add(new MemoryMeasurement());

          MemoryMeasurement current = measurements.get(measurements.size() - 1);
          MemoryMeasurement initial = measurements.get(0);
          long increase = current.totalUsed() - initial.totalUsed();

          System.out.println(
              "Memory after " + operationCount + " operations: " + current +
              " (increase: " + increase + " bytes)");
        }
      }

      System.out.println("Completed " + operationCount + " operations");

      // Analyze memory stability
      MemoryMeasurement initialMeasurement = measurements.get(0);
      MemoryMeasurement finalMeasurement = measurements.get(measurements.size() - 1);

      long totalIncrease = finalMeasurement.totalUsed() - initialMeasurement.totalUsed();

      System.out.println("Total memory increase over " + testDurationMs + "ms: " + totalIncrease + " bytes");

      // Memory should remain stable during long-running error scenarios
      assertTrue(
          totalIncrease < MEMORY_LEAK_THRESHOLD,
          "Long-running error scenarios should not cause memory leaks. Increase: "
              + totalIncrease
              + " bytes over "
              + operationCount
              + " operations");

      // Check that memory didn't grow continuously
      if (measurements.size() >= 3) {
        MemoryMeasurement midMeasurement = measurements.get(measurements.size() / 2);
        long midIncrease = midMeasurement.totalUsed() - initialMeasurement.totalUsed();
        long lateIncrease = totalIncrease - midIncrease;

        // Late increase should not be dramatically larger than mid increase
        assertTrue(
            lateIncrease < midIncrease * 2,
            "Memory growth should stabilize, not accelerate. Mid increase: "
                + midIncrease
                + ", Late increase: "
                + lateIncrease);
      }
    }
  }
}
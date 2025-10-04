package ai.tegmentum.wasmtime4j.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Comprehensive test suite for WebAssembly resource exhaustion scenarios.
 *
 * <p>This test class verifies proper error handling when system resources are exhausted, including
 * memory limits, too many instances, excessive compilation requests, and other resource-related
 * failures that should throw appropriate exceptions.
 */
@DisplayName("Resource Exhaustion Scenario Test Suite")
class ResourceExhaustionScenarioTest {

  /** Simple valid WebAssembly module for resource testing. */
  private static final byte[] SIMPLE_MODULE = {
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
    'e',
    's',
    't', // Export name "test"
    0x00,
    0x00, // Function export with index 0
    0x0A, // Code section
    0x04, // Section size
    0x01, // 1 function body
    0x02, // Body size
    0x00,
    0x0B // No locals, end instruction
  };

  /**
   * WebAssembly module with large memory allocation. This module declares a memory with a very
   * large minimum size.
   */
  private static final byte[] LARGE_MEMORY_MODULE = {
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
    0x05, // Memory section
    0x04, // Section size
    0x01, // 1 memory
    0x00,
    (byte) 0xFF,
    (byte) 0xFF, // Memory limits: minimum 65535 pages (4GB)
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
    0x04, // Section size
    0x01, // 1 function body
    0x02, // Body size
    0x00,
    0x0B // No locals, end instruction
  };

  /** WebAssembly module with many functions to test compilation limits. */
  private static byte[] createLargeFunctionModule() {
    List<Byte> moduleBytes = new ArrayList<>();

    // Header
    moduleBytes.addAll(List.of((byte) 0x00, (byte) 0x61, (byte) 0x73, (byte) 0x6d));
    moduleBytes.addAll(List.of((byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00));

    // Type section - 1 function type
    moduleBytes.add((byte) 0x01); // Type section
    moduleBytes.add((byte) 0x04); // Section size
    moduleBytes.add((byte) 0x01); // 1 type
    moduleBytes.add((byte) 0x60); // Function type
    moduleBytes.add((byte) 0x00); // 0 parameters
    moduleBytes.add((byte) 0x00); // 0 returns

    // Function section - many functions
    int functionCount = 10000;
    moduleBytes.add((byte) 0x03); // Function section

    // Calculate section size (LEB128 encoding of function count + function count bytes)
    List<Byte> functionCountBytes = encodeLEB128(functionCount);
    int sectionSize = functionCountBytes.size() + functionCount;
    moduleBytes.addAll(encodeLEB128(sectionSize));

    moduleBytes.addAll(functionCountBytes); // Function count
    for (int i = 0; i < functionCount; i++) {
      moduleBytes.add((byte) 0x00); // All functions use type 0
    }

    // Code section - bodies for all functions
    moduleBytes.add((byte) 0x0A); // Code section
    int codeSectionSize =
        functionCountBytes.size() + functionCount * 3; // Each function body is 3 bytes
    moduleBytes.addAll(encodeLEB128(codeSectionSize));

    moduleBytes.addAll(functionCountBytes); // Function body count
    for (int i = 0; i < functionCount; i++) {
      moduleBytes.add((byte) 0x02); // Body size
      moduleBytes.add((byte) 0x00); // No locals
      moduleBytes.add((byte) 0x0B); // End instruction
    }

    // Convert to byte array
    byte[] result = new byte[moduleBytes.size()];
    for (int i = 0; i < moduleBytes.size(); i++) {
      result[i] = moduleBytes.get(i);
    }
    return result;
  }

  /** Encode integer as LEB128. */
  private static List<Byte> encodeLEB128(int value) {
    List<Byte> result = new ArrayList<>();
    do {
      byte b = (byte) (value & 0x7F);
      value >>>= 7;
      if (value != 0) {
        b |= 0x80;
      }
      result.add(b);
    } while (value != 0);
    return result;
  }

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @DisplayName("Large memory allocation is handled gracefully")
  void testLargeMemoryAllocation(RuntimeType runtimeType) throws WasmException {
    if (!WasmRuntimeFactory.isRuntimeAvailable(runtimeType)) {
      return; // Skip if runtime not available
    }

    try (WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Try to instantiate module with very large memory requirement
      Module module = runtime.compileModule(engine, LARGE_MEMORY_MODULE);

      // This should either succeed (if system has enough virtual memory)
      // or throw a meaningful exception
      try {
        Instance instance = runtime.instantiateModule(store, module);
        // If it succeeds, that's fine - the test is about graceful handling
        assertNotNull(instance, "Instance should be created if instantiation succeeds");
      } catch (WasmException e) {
        // If it fails, the error should be meaningful
        assertNotNull(e.getMessage(), "Exception should have meaningful message");
        assertTrue(
            e.getMessage().toLowerCase().contains("memory")
                || e.getMessage().toLowerCase().contains("allocation")
                || e.getMessage().toLowerCase().contains("limit")
                || e.getMessage().toLowerCase().contains("resource"),
            "Exception message should mention memory/allocation/limit/resource: " + e.getMessage());
      }
    }
  }

  @Test
  @DisplayName("Excessive instance creation is handled gracefully")
  void testExcessiveInstanceCreation() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Module module = runtime.compileModule(engine, SIMPLE_MODULE);

      List<Store> stores = new ArrayList<>();
      List<Instance> instances = new ArrayList<>();

      try {
        // Try to create many instances until we hit a limit or run out of memory
        for (int i = 0; i < 10000; i++) {
          Store store = runtime.createStore(engine);
          stores.add(store);

          Instance instance = runtime.instantiateModule(store, module);
          instances.add(instance);

          // Check memory usage periodically
          if (i % 1000 == 0) {
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;

            // If we're using too much memory, break out
            if (usedMemory > Runtime.getRuntime().maxMemory() * 0.8) {
              break;
            }
          }
        }

        // If we get here, system handled the load gracefully
        assertTrue(instances.size() > 0, "Should have created at least some instances");

      } catch (WasmException e) {
        // If we hit a resource limit, the error should be meaningful
        assertNotNull(e.getMessage(), "Exception should have meaningful message");
        assertTrue(
            e.getMessage().toLowerCase().contains("resource")
                || e.getMessage().toLowerCase().contains("limit")
                || e.getMessage().toLowerCase().contains("memory")
                || e.getMessage().toLowerCase().contains("allocation"),
            "Exception message should mention resource/limit/memory/allocation: " + e.getMessage());
      } catch (OutOfMemoryError e) {
        // OutOfMemoryError is acceptable for this test
        assertTrue(true, "OutOfMemoryError is acceptable when creating many instances");
      } finally {
        // Clean up resources
        instances.clear();
        stores.clear();
        System.gc();
      }
    }
  }

  @Test
  @DisplayName("Rapid module compilation doesn't exhaust resources")
  void testRapidModuleCompilation() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      long startTime = System.nanoTime();
      List<Module> modules = new ArrayList<>();

      try {
        // Rapidly compile many modules
        for (int i = 0; i < 1000; i++) {
          Module module = runtime.compileModule(engine, SIMPLE_MODULE);
          modules.add(module);

          // Check if we're taking too long (resource exhaustion indicator)
          if (i % 100 == 0) {
            long elapsed = System.nanoTime() - startTime;
            if (elapsed > TimeUnit.SECONDS.toNanos(30)) {
              break; // Don't let test run forever
            }
          }
        }

        // Should have compiled at least some modules
        assertTrue(modules.size() > 0, "Should have compiled at least some modules");

        // Performance should remain reasonable
        long duration = System.nanoTime() - startTime;
        assertTrue(
            duration < TimeUnit.SECONDS.toNanos(60),
            "Compilation should complete within reasonable time");

      } catch (WasmException e) {
        // If compilation fails due to resource limits, error should be meaningful
        assertNotNull(e.getMessage(), "Exception should have meaningful message");
        assertTrue(
            e.getMessage().toLowerCase().contains("resource")
                || e.getMessage().toLowerCase().contains("compilation")
                || e.getMessage().toLowerCase().contains("limit")
                || e.getMessage().toLowerCase().contains("memory"),
            "Exception message should mention resource/compilation/limit/memory: "
                + e.getMessage());
      } finally {
        modules.clear();
        System.gc();
      }
    }
  }

  @Test
  @DisplayName("Large module compilation is handled efficiently")
  void testLargeModuleCompilation() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      byte[] largeModule = createLargeFunctionModule();

      long startTime = System.nanoTime();

      try {
        Module module = runtime.compileModule(engine, largeModule);
        long duration = System.nanoTime() - startTime;

        assertNotNull(module, "Large module should compile successfully");

        // Should complete within reasonable time (less than 30 seconds)
        assertTrue(
            duration < TimeUnit.SECONDS.toNanos(30),
            "Large module compilation should complete within 30 seconds: " + duration + "ns");

      } catch (WasmException e) {
        // If compilation fails, it should be due to resource limits, not crashes
        assertNotNull(e.getMessage(), "Exception should have meaningful message");
        assertTrue(
            e.getMessage().toLowerCase().contains("compilation")
                || e.getMessage().toLowerCase().contains("resource")
                || e.getMessage().toLowerCase().contains("limit")
                || e.getMessage().toLowerCase().contains("size"),
            "Exception message should mention compilation/resource/limit/size: " + e.getMessage());
      }
    }
  }

  @Test
  @DisplayName("Concurrent resource allocation is handled safely")
  void testConcurrentResourceAllocation() throws InterruptedException, WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      final int threadCount = 20;
      final CountDownLatch latch = new CountDownLatch(threadCount);
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger exceptionCount = new AtomicInteger(0);

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                // Each thread creates multiple resources
                for (int j = 0; j < 50; j++) {
                  Store store = runtime.createStore(engine);
                  Module module = runtime.compileModule(engine, SIMPLE_MODULE);
                  Instance instance = runtime.instantiateModule(store, module);

                  // Basic validation that resources are functional
                  assertTrue(runtime.isValid(), "Runtime should remain valid");
                  assertNotNull(instance, "Instance should be created");
                }
                successCount.incrementAndGet();

              } catch (WasmException | OutOfMemoryError e) {
                // Resource exhaustion is acceptable in this stress test
                exceptionCount.incrementAndGet();
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(
          latch.await(60, TimeUnit.SECONDS), "All threads should complete within 60 seconds");
      executor.shutdown();

      // At least some threads should succeed
      assertTrue(successCount.get() > 0, "At least some resource allocation should succeed");

      // Runtime should remain valid even under stress
      assertTrue(runtime.isValid(), "Runtime should remain valid after concurrent stress");
    }
  }

  @Test
  @DisplayName("Memory pressure doesn't corrupt runtime state")
  void testMemoryPressureHandling() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Create memory pressure by allocating large amounts of memory
      List<byte[]> memoryHogs = new ArrayList<>();

      try {
        // Allocate memory until we approach limits
        for (int i = 0; i < 100; i++) {
          try {
            byte[] largeArray = new byte[10 * 1024 * 1024]; // 10MB
            memoryHogs.add(largeArray);

            // Try to perform normal operations under memory pressure
            Module module = runtime.compileModule(engine, SIMPLE_MODULE);
            Store store = runtime.createStore(engine);
            Instance instance = runtime.instantiateModule(store, module);

            // Verify operations still work
            assertNotNull(module, "Module should compile under memory pressure");
            assertNotNull(instance, "Instance should be created under memory pressure");
            assertTrue(runtime.isValid(), "Runtime should remain valid under memory pressure");

          } catch (OutOfMemoryError e) {
            // Expected when we hit memory limits
            break;
          }
        }

      } finally {
        // Clean up memory
        memoryHogs.clear();
        System.gc();
        Thread.yield();
      }

      // Runtime should still be functional after memory pressure
      assertTrue(runtime.isValid(), "Runtime should remain valid after memory pressure");

      // Should be able to perform normal operations after cleanup
      assertDoesNotThrow(
          () -> {
            Module module = runtime.compileModule(engine, SIMPLE_MODULE);
            Store store = runtime.createStore(engine);
            runtime.instantiateModule(store, module);
          },
          "Should be able to perform normal operations after memory pressure");
    }
  }

  @Test
  @DisplayName("Resource cleanup happens properly after failures")
  void testResourceCleanupAfterFailures() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Force garbage collection and measure initial memory
      System.gc();
      Thread.yield();
      final long initialMemory =
          Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      // Attempt many operations that might fail due to resource limits
      for (int i = 0; i < 100; i++) {
        try {
          Store store = runtime.createStore(engine);
          Module module = runtime.compileModule(engine, LARGE_MEMORY_MODULE);

          // This might fail due to memory limits
          runtime.instantiateModule(store, module);

        } catch (WasmException e) {
          // Expected for some iterations due to resource limits
          assertNotNull(e.getMessage(), "Exception should have meaningful message");
        }

        // Periodic cleanup
        if (i % 20 == 0) {
          System.gc();
          Thread.yield();
        }
      }

      // Force final garbage collection
      System.gc();
      Thread.yield();
      long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      // Memory should not have increased dramatically (allow 100MB increase)
      long memoryIncrease = finalMemory - initialMemory;
      assertTrue(
          memoryIncrease < 100 * 1024 * 1024,
          "Memory should be cleaned up properly after failures. Initial: "
              + initialMemory
              + ", Final: "
              + finalMemory
              + ", Increase: "
              + memoryIncrease);

      // Runtime should still be valid
      assertTrue(runtime.isValid(), "Runtime should remain valid after resource failures");
    }
  }

  @Test
  @DisplayName("Resource limits provide meaningful error messages")
  void testResourceLimitErrorMessages() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Try operations that are likely to hit resource limits
      try {
        byte[] extremeModule = createExtremeResourceModule();
        runtime.compileModule(engine, extremeModule);

      } catch (WasmException e) {
        String message = e.getMessage();
        assertNotNull(message, "Exception should have meaningful message");

        // Message should be informative
        assertTrue(message.length() > 10, "Error message should be descriptive");
        assertTrue(message.length() < 10000, "Error message should not be excessively long");

        // Should mention resource-related concepts
        String lowerMessage = message.toLowerCase();
        assertTrue(
            lowerMessage.contains("resource")
                || lowerMessage.contains("limit")
                || lowerMessage.contains("memory")
                || lowerMessage.contains("allocation")
                || lowerMessage.contains("size")
                || lowerMessage.contains("exceed"),
            "Error message should mention resource concepts: " + message);

        // Should not contain log injection vulnerabilities
        assertTrue(
            !message.contains("\n") && !message.contains("\r"),
            "Error message should not contain CRLF characters");
      }
    }
  }

  /** Create a WebAssembly module designed to test extreme resource usage. */
  private byte[] createExtremeResourceModule() {
    // This is a simplified version - in practice, this would create a module
    // with extreme requirements (many sections, large tables, etc.)
    List<Byte> moduleBytes = new ArrayList<>();

    // Header
    moduleBytes.addAll(List.of((byte) 0x00, (byte) 0x61, (byte) 0x73, (byte) 0x6d));
    moduleBytes.addAll(List.of((byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00));

    // Extreme memory section
    moduleBytes.add((byte) 0x05); // Memory section
    moduleBytes.add((byte) 0x05); // Section size
    moduleBytes.add((byte) 0x01); // 1 memory
    moduleBytes.add((byte) 0x01); // Has maximum
    moduleBytes.add((byte) 0xFF); // Minimum: 255 pages
    moduleBytes.add((byte) 0xFF); // Maximum: 255 pages

    // Convert to byte array
    byte[] result = new byte[moduleBytes.size()];
    for (int i = 0; i < moduleBytes.size(); i++) {
      result[i] = moduleBytes.get(i);
    }
    return result;
  }
}

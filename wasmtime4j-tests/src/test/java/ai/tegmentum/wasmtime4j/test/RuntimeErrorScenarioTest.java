package ai.tegmentum.wasmtime4j.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
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
 * Comprehensive test suite for WebAssembly runtime error scenarios.
 *
 * <p>This test class verifies proper error handling for runtime failures that occur during
 * WebAssembly execution, including traps, stack overflows, memory access violations, and function
 * call failures.
 */
@DisplayName("Runtime Error Scenario Test Suite")
class RuntimeErrorScenarioTest {

  /**
   * WebAssembly module that contains a function which triggers an unreachable instruction.
   * Module structure:
   * - Type section: () -> ()
   * - Function section: 1 function of type 0
   * - Export section: export "trap" function 0
   * - Code section: function body with unreachable instruction
   */
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

  /**
   * WebAssembly module that contains a function which performs integer division by zero.
   * Module structure:
   * - Type section: () -> i32
   * - Function section: 1 function of type 0
   * - Export section: export "div_by_zero" function 0
   * - Code section: i32.const 42, i32.const 0, i32.div_s
   */
  private static final byte[] DIVISION_BY_ZERO_MODULE = {
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
    0x01, // Type section
    0x05, // Section size
    0x01, // 1 type
    0x60, 0x00, 0x01, 0x7F, // Function type: () -> i32
    0x03, // Function section
    0x02, // Section size
    0x01, 0x00, // 1 function with type index 0
    0x07, // Export section
    0x0C, // Section size
    0x01, // 1 export
    0x0A, 'd', 'i', 'v', '_', 'b', 'y', '_', 'z', 'e', 'r', 'o', // Export name "div_by_zero"
    0x00, 0x00, // Function export with index 0
    0x0A, // Code section
    0x09, // Section size
    0x01, // 1 function body
    0x07, // Body size
    0x00, // No locals
    0x41, 0x2A, // i32.const 42
    0x41, 0x00, // i32.const 0
    0x6D, // i32.div_s (division)
    0x0B // End instruction
  };

  /**
   * WebAssembly module with a recursive function that can cause stack overflow.
   * Module structure:
   * - Type section: () -> ()
   * - Function section: 1 function of type 0
   * - Export section: export "recurse" function 0
   * - Code section: function that calls itself
   */
  private static final byte[] STACK_OVERFLOW_MODULE = {
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
    0x01, // Type section
    0x04, // Section size
    0x01, // 1 type
    0x60, 0x00, 0x00, // Function type: () -> ()
    0x03, // Function section
    0x02, // Section size
    0x01, 0x00, // 1 function with type index 0
    0x07, // Export section
    0x0A, // Section size
    0x01, // 1 export
    0x07, 'r', 'e', 'c', 'u', 'r', 's', 'e', // Export name "recurse"
    0x00, 0x00, // Function export with index 0
    0x0A, // Code section
    0x06, // Section size
    0x01, // 1 function body
    0x04, // Body size
    0x00, // No locals
    0x10, 0x00, // call function 0 (self)
    0x0B // End instruction
  };

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @DisplayName("Unreachable instruction throws RuntimeException")
  void testUnreachableInstructionTrap(RuntimeType runtimeType) throws WasmException {
    if (!WasmRuntimeFactory.isRuntimeAvailable(runtimeType)) {
      return; // Skip if runtime not available
    }

    try (WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module module = runtime.compileModule(engine, TRAP_MODULE);
      Instance instance = runtime.instantiateModule(store, module);

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> instance.getExportedFunction("trap").call(),
              "Unreachable instruction should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("unreachable")
              || exception.getMessage().toLowerCase().contains("trap")
              || exception.getMessage().toLowerCase().contains("illegal"),
          "Exception message should mention unreachable/trap/illegal: " + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Division by zero throws RuntimeException")
  void testDivisionByZeroTrap() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module module = runtime.compileModule(engine, DIVISION_BY_ZERO_MODULE);
      Instance instance = runtime.instantiateModule(store, module);

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> instance.getExportedFunction("div_by_zero").call(),
              "Division by zero should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("division")
              || exception.getMessage().toLowerCase().contains("zero")
              || exception.getMessage().toLowerCase().contains("trap")
              || exception.getMessage().toLowerCase().contains("arithmetic"),
          "Exception message should mention division/zero/trap/arithmetic: "
              + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Stack overflow throws RuntimeException")
  void testStackOverflowTrap() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module module = runtime.compileModule(engine, STACK_OVERFLOW_MODULE);
      Instance instance = runtime.instantiateModule(store, module);

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> instance.getExportedFunction("recurse").call(),
              "Stack overflow should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("stack")
              || exception.getMessage().toLowerCase().contains("overflow")
              || exception.getMessage().toLowerCase().contains("limit")
              || exception.getMessage().toLowerCase().contains("exceed"),
          "Exception message should mention stack/overflow/limit/exceed: "
              + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Invalid function call throws RuntimeException")
  void testInvalidFunctionCall() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Simple module with no exported functions
      byte[] emptyModule = {
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version only
      };

      Module module = runtime.compileModule(engine, emptyModule);
      Instance instance = runtime.instantiateModule(store, module);

      // Try to call non-existent function
      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> instance.getExportedFunction("nonexistent"),
              "Getting non-existent function should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("function")
              || exception.getMessage().toLowerCase().contains("export")
              || exception.getMessage().toLowerCase().contains("not found")
              || exception.getMessage().toLowerCase().contains("unknown"),
          "Exception message should mention function/export/not found/unknown: "
              + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Runtime errors preserve stack traces")
  void testRuntimeErrorStackTraces() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module module = runtime.compileModule(engine, TRAP_MODULE);
      Instance instance = runtime.instantiateModule(store, module);

      try {
        instance.getExportedFunction("trap").call();
      } catch (WasmException e) {
        // Stack trace should be preserved
        StackTraceElement[] stackTrace = e.getStackTrace();
        assertNotNull(stackTrace, "Exception should have stack trace");
        assertTrue(stackTrace.length > 0, "Stack trace should not be empty");

        // At least one stack trace element should reference this test method
        boolean foundTestMethod = false;
        for (StackTraceElement element : stackTrace) {
          if (element.getMethodName().contains("testRuntimeErrorStackTraces")) {
            foundTestMethod = true;
            break;
          }
        }
        assertTrue(foundTestMethod, "Stack trace should contain test method");
      }
    }
  }

  @Test
  @DisplayName("Runtime errors allow continued operation")
  void testRuntimeErrorRecovery() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module module = runtime.compileModule(engine, TRAP_MODULE);
      Instance instance = runtime.instantiateModule(store, module);

      // Trigger runtime error
      assertThrows(
          WasmException.class,
          () -> instance.getExportedFunction("trap").call(),
          "First call should throw exception");

      // Runtime should still be valid
      assertTrue(runtime.isValid(), "Runtime should remain valid after error");

      // Should be able to create new store and instance
      Store newStore = runtime.createStore(engine);
      Module newModule = runtime.compileModule(engine, TRAP_MODULE);
      Instance newInstance = runtime.instantiateModule(newStore, newModule);

      // New instance should behave the same (error is module-specific, not runtime corruption)
      assertThrows(
          WasmException.class,
          () -> newInstance.getExportedFunction("trap").call(),
          "New instance should also throw exception");
    }
  }

  @Test
  @DisplayName("Concurrent runtime errors are handled safely")
  void testConcurrentRuntimeErrors() throws InterruptedException, WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      final int threadCount = 10;
      final CountDownLatch latch = new CountDownLatch(threadCount);
      final AtomicInteger exceptionCount = new AtomicInteger(0);
      final AtomicInteger unexpectedExceptionCount = new AtomicInteger(0);

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                Store store = runtime.createStore(engine);
                Module module = runtime.compileModule(engine, TRAP_MODULE);
                Instance instance = runtime.instantiateModule(store, module);

                instance.getExportedFunction("trap").call();
                // Should not reach here
              } catch (WasmException e) {
                exceptionCount.incrementAndGet();
              } catch (Exception e) {
                unexpectedExceptionCount.incrementAndGet();
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete within 30 seconds");
      executor.shutdown();

      assertTrue(exceptionCount.get() == threadCount, "All threads should throw WasmException");
      assertTrue(unexpectedExceptionCount.get() == 0, "No unexpected exceptions should occur");
    }
  }

  @Test
  @DisplayName("Runtime error messages are consistent")
  void testRuntimeErrorConsistency() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module module = runtime.compileModule(engine, TRAP_MODULE);
      Instance instance = runtime.instantiateModule(store, module);

      String firstMessage = null;
      for (int i = 0; i < 5; i++) {
        try {
          instance.getExportedFunction("trap").call();
        } catch (WasmException e) {
          if (firstMessage == null) {
            firstMessage = e.getMessage();
          } else {
            assertTrue(
                firstMessage.equals(e.getMessage()),
                "Error messages should be consistent. First: '"
                    + firstMessage
                    + "', Current: '"
                    + e.getMessage()
                    + "'");
          }
        }
      }

      assertNotNull(firstMessage, "Should have captured at least one error message");
    }
  }

  @Test
  @DisplayName("Memory access violations throw RuntimeException")
  void testMemoryAccessViolation() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // WebAssembly module with memory and a function that accesses out of bounds
      byte[] memoryAccessModule = {
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
        0x01, // Type section
        0x05, // Section size
        0x01, // 1 type
        0x60, 0x00, 0x01, 0x7F, // Function type: () -> i32
        0x03, // Function section
        0x02, // Section size
        0x01, 0x00, // 1 function with type index 0
        0x05, // Memory section
        0x03, // Section size
        0x01, // 1 memory
        0x00, 0x01, // Memory limits: minimum 1 page (64KB)
        0x07, // Export section
        0x0D, // Section size
        0x01, // 1 export
        0x0B, 'o', 'u', 't', '_', 'o', 'f', '_', 'b', 'o', 'u', 'n', 'd', 's', // Export name
        0x00, 0x00, // Function export with index 0
        0x0A, // Code section
        0x0A, // Section size
        0x01, // 1 function body
        0x08, // Body size
        0x00, // No locals
        0x41, (byte) 0x80, (byte) 0x80, 0x04, // i32.const 65536 (out of bounds)
        0x28, 0x02, 0x00, // i32.load align=2 offset=0
        0x0B // End instruction
      };

      Module module = runtime.compileModule(engine, memoryAccessModule);
      Instance instance = runtime.instantiateModule(store, module);

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> instance.getExportedFunction("out_of_bounds").call(),
              "Out of bounds memory access should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("bounds")
              || exception.getMessage().toLowerCase().contains("memory")
              || exception.getMessage().toLowerCase().contains("access")
              || exception.getMessage().toLowerCase().contains("trap"),
          "Exception message should mention bounds/memory/access/trap: "
              + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Runtime errors don't cause memory leaks")
  void testMemoryLeakPrevention() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Force garbage collection and measure initial memory
      System.gc();
      Thread.yield();
      long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      // Trigger many runtime errors
      for (int i = 0; i < 100; i++) {
        Store store = runtime.createStore(engine);
        Module module = runtime.compileModule(engine, TRAP_MODULE);
        Instance instance = runtime.instantiateModule(store, module);

        assertThrows(
            WasmException.class,
            () -> instance.getExportedFunction("trap").call(),
            "Should throw exception for each iteration");

        // Periodic garbage collection
        if (i % 20 == 0) {
          System.gc();
          Thread.yield();
        }
      }

      // Force garbage collection and measure final memory
      System.gc();
      Thread.yield();
      long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      // Memory usage should not increase dramatically (allow 30MB increase)
      long memoryIncrease = finalMemory - initialMemory;
      assertTrue(
          memoryIncrease < 30 * 1024 * 1024,
          "Memory usage should remain stable during runtime errors. Initial: "
              + initialMemory
              + ", Final: "
              + finalMemory
              + ", Increase: "
              + memoryIncrease);
    }
  }

  @Test
  @DisplayName("Runtime can handle rapid successive errors")
  void testRapidSuccessiveErrors() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module module = runtime.compileModule(engine, TRAP_MODULE);
      Instance instance = runtime.instantiateModule(store, module);

      // Rapidly trigger errors in a tight loop
      long startTime = System.nanoTime();
      for (int i = 0; i < 1000; i++) {
        assertThrows(
            WasmException.class,
            () -> instance.getExportedFunction("trap").call(),
            "Each call should throw exception");
      }
      long duration = System.nanoTime() - startTime;

      // Should handle errors efficiently (less than 1 second for 1000 errors)
      assertTrue(
          duration < TimeUnit.SECONDS.toNanos(1),
          "Error handling should be efficient: " + duration + "ns for 1000 errors");

      // Runtime should still be valid
      assertTrue(runtime.isValid(), "Runtime should remain valid after rapid errors");
    }
  }

  @Test
  @DisplayName("Error details include function context where available")
  void testErrorContextInformation() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);
      Module module = runtime.compileModule(engine, TRAP_MODULE);
      Instance instance = runtime.instantiateModule(store, module);

      try {
        instance.getExportedFunction("trap").call();
      } catch (WasmException e) {
        String message = e.getMessage();
        assertNotNull(message, "Exception should have message");

        // Error message should be informative and safe for logging
        assertTrue(message.length() > 5, "Error message should be descriptive");
        assertTrue(message.length() < 10000, "Error message should not be excessively long");

        // Should not contain characters that could cause log injection
        assertTrue(
            !message.contains("\n") && !message.contains("\r"),
            "Error message should not contain CRLF characters");
      }
    }
  }
}
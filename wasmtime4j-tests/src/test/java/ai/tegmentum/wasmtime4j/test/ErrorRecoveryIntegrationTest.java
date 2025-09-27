package ai.tegmentum.wasmtime4j.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Integration test framework for comprehensive error recovery testing.
 *
 * <p>This test class validates the complete error recovery pipeline across different error
 * scenarios, runtime states, and recovery strategies. It ensures that the WebAssembly runtime
 * can gracefully handle errors and continue operating correctly after various failure modes.
 */
@DisplayName("Error Recovery Integration Test Framework")
class ErrorRecoveryIntegrationTest {

  /** Valid WebAssembly module that returns a constant value. */
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

  /** WebAssembly module with unreachable instruction. */
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

  /** WebAssembly module with division by zero. */
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
    0x0A, // Section size
    0x01, // 1 export
    0x06, 'd', 'i', 'v', 'z', 'e', 'r', 'o', // Export name "divzero"
    0x00, 0x00, // Function export with index 0
    0x0A, // Code section
    0x09, // Section size
    0x01, // 1 function body
    0x07, // Body size
    0x00, // No locals
    0x41, 0x01, // i32.const 1
    0x41, 0x00, // i32.const 0
    0x6D, // i32.div_s
    0x0B // End instruction
  };

  /** Invalid WebAssembly module. */
  private static final byte[] INVALID_MODULE = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

  /**
   * Error recovery strategy for testing.
   */
  private enum RecoveryStrategy {
    /** Continue with the same runtime after error. */
    CONTINUE_SAME_RUNTIME,
    /** Create new store after error. */
    NEW_STORE,
    /** Create new engine after error. */
    NEW_ENGINE,
    /** Create completely new runtime after error. */
    NEW_RUNTIME,
    /** Reset the current store state. */
    RESET_STORE
  }

  /**
   * Recovery test scenario.
   */
  private static class RecoveryScenario {
    final String name;
    final byte[] errorModule;
    final String errorFunction;
    final RecoveryStrategy strategy;
    final byte[] recoveryModule;
    final String recoveryFunction;
    final Object expectedResult;

    RecoveryScenario(
        String name,
        byte[] errorModule,
        String errorFunction,
        RecoveryStrategy strategy,
        byte[] recoveryModule,
        String recoveryFunction,
        Object expectedResult) {
      this.name = name;
      this.errorModule = errorModule;
      this.errorFunction = errorFunction;
      this.strategy = strategy;
      this.recoveryModule = recoveryModule;
      this.recoveryFunction = recoveryFunction;
      this.expectedResult = expectedResult;
    }
  }

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @DisplayName("Basic error recovery across different strategies")
  void testBasicErrorRecovery(RuntimeType runtimeType) throws WasmException {
    if (!WasmRuntimeFactory.isRuntimeAvailable(runtimeType)) {
      return; // Skip if runtime not available
    }

    List<RecoveryScenario> scenarios =
        List.of(
            new RecoveryScenario(
                "Trap to valid function",
                TRAP_MODULE,
                "trap",
                RecoveryStrategy.CONTINUE_SAME_RUNTIME,
                VALID_MODULE,
                "test",
                42),
            new RecoveryScenario(
                "Division by zero to valid function",
                DIVISION_BY_ZERO_MODULE,
                "divzero",
                RecoveryStrategy.NEW_STORE,
                VALID_MODULE,
                "test",
                42),
            new RecoveryScenario(
                "Trap with new engine",
                TRAP_MODULE,
                "trap",
                RecoveryStrategy.NEW_ENGINE,
                VALID_MODULE,
                "test",
                42));

    for (RecoveryScenario scenario : scenarios) {
      testRecoveryScenario(runtimeType, scenario);
    }
  }

  private void testRecoveryScenario(RuntimeType runtimeType, RecoveryScenario scenario)
      throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Setup error-causing module
      Module errorModule = runtime.compileModule(engine, scenario.errorModule);
      Instance errorInstance = runtime.instantiateModule(store, errorModule);

      // Trigger the error
      assertThrows(
          WasmException.class,
          () -> errorInstance.getExportedFunction(scenario.errorFunction).call(),
          "Error function should throw exception: " + scenario.name);

      // Apply recovery strategy
      WasmRuntime recoveryRuntime = runtime;
      Engine recoveryEngine = engine;
      Store recoveryStore = store;

      switch (scenario.strategy) {
        case CONTINUE_SAME_RUNTIME:
          // Use same runtime, engine, and store
          break;
        case NEW_STORE:
          recoveryStore = runtime.createStore(engine);
          break;
        case NEW_ENGINE:
          recoveryEngine = runtime.createEngine();
          recoveryStore = runtime.createStore(recoveryEngine);
          break;
        case NEW_RUNTIME:
          recoveryRuntime = WasmRuntimeFactory.create(runtimeType);
          recoveryEngine = recoveryRuntime.createEngine();
          recoveryStore = recoveryRuntime.createStore(recoveryEngine);
          break;
        case RESET_STORE:
          // For now, reset by creating new store (store reset might not be implemented)
          recoveryStore = runtime.createStore(engine);
          break;
      }

      // Test recovery
      Module recoveryModule = recoveryRuntime.compileModule(recoveryEngine, scenario.recoveryModule);
      Instance recoveryInstance = recoveryRuntime.instantiateModule(recoveryStore, recoveryModule);

      Object result = recoveryInstance.getExportedFunction(scenario.recoveryFunction).call();
      assertEquals(
          scenario.expectedResult,
          result,
          "Recovery should work correctly: " + scenario.name);

      // Cleanup additional runtimes
      if (scenario.strategy == RecoveryStrategy.NEW_RUNTIME && recoveryRuntime != runtime) {
        recoveryRuntime.close();
      }
    }
  }

  @Test
  @DisplayName("Cascading error recovery")
  void testCascadingErrorRecovery() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Compile all modules
      Module validModule = runtime.compileModule(engine, VALID_MODULE);
      Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
      Module divZeroModule = runtime.compileModule(engine, DIVISION_BY_ZERO_MODULE);

      // Create instances
      Instance validInstance = runtime.instantiateModule(store, validModule);
      Instance trapInstance = runtime.instantiateModule(store, trapModule);
      Instance divZeroInstance = runtime.instantiateModule(store, divZeroModule);

      // Sequence of errors and recoveries
      List<String> operationLog = new ArrayList<>();

      // Operation 1: Normal call
      Object result1 = validInstance.getExportedFunction("test").call();
      assertEquals(42, result1, "First normal call should succeed");
      operationLog.add("normal_call_1");

      // Operation 2: Trap error
      assertThrows(
          WasmException.class,
          () -> trapInstance.getExportedFunction("trap").call(),
          "Trap should throw exception");
      operationLog.add("trap_error");

      // Operation 3: Recovery after trap
      Object result2 = validInstance.getExportedFunction("test").call();
      assertEquals(42, result2, "Recovery after trap should succeed");
      operationLog.add("recovery_after_trap");

      // Operation 4: Division by zero error
      assertThrows(
          WasmException.class,
          () -> divZeroInstance.getExportedFunction("divzero").call(),
          "Division by zero should throw exception");
      operationLog.add("divzero_error");

      // Operation 5: Recovery after division by zero
      Object result3 = validInstance.getExportedFunction("test").call();
      assertEquals(42, result3, "Recovery after division by zero should succeed");
      operationLog.add("recovery_after_divzero");

      // Operation 6: Multiple consecutive errors
      for (int i = 0; i < 5; i++) {
        assertThrows(
            WasmException.class,
            () -> trapInstance.getExportedFunction("trap").call(),
            "Consecutive trap should throw exception");
        operationLog.add("consecutive_trap_" + i);
      }

      // Operation 7: Final recovery
      Object result4 = validInstance.getExportedFunction("test").call();
      assertEquals(42, result4, "Final recovery should succeed");
      operationLog.add("final_recovery");

      // Verify all operations were logged correctly
      assertEquals(12, operationLog.size(), "All operations should be logged");
      assertTrue(runtime.isValid(), "Runtime should remain valid after cascading errors");
    }
  }

  @Test
  @DisplayName("Concurrent error recovery")
  void testConcurrentErrorRecovery() throws InterruptedException, WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      final int threadCount = 10;
      final int operationsPerThread = 20;
      final CountDownLatch latch = new CountDownLatch(threadCount);
      final AtomicInteger successfulRecoveries = new AtomicInteger(0);
      final AtomicInteger totalErrors = new AtomicInteger(0);
      final List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                Store store = runtime.createStore(engine);
                Module validModule = runtime.compileModule(engine, VALID_MODULE);
                Module trapModule = runtime.compileModule(engine, TRAP_MODULE);

                Instance validInstance = runtime.instantiateModule(store, validModule);
                Instance trapInstance = runtime.instantiateModule(store, trapModule);

                for (int j = 0; j < operationsPerThread; j++) {
                  try {
                    // Trigger error
                    trapInstance.getExportedFunction("trap").call();
                  } catch (WasmException e) {
                    totalErrors.incrementAndGet();
                    errorMessages.add("Thread " + threadId + ": " + e.getMessage());

                    // Attempt recovery
                    try {
                      Object result = validInstance.getExportedFunction("test").call();
                      if (Integer.valueOf(42).equals(result)) {
                        successfulRecoveries.incrementAndGet();
                      }
                    } catch (Exception recoveryError) {
                      errorMessages.add(
                          "Thread " + threadId + " recovery failed: " + recoveryError.getMessage());
                    }
                  }
                }
              } catch (Exception e) {
                errorMessages.add("Thread " + threadId + " setup failed: " + e.getMessage());
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(latch.await(60, TimeUnit.SECONDS), "All threads should complete within 60 seconds");
      executor.shutdown();

      // Verify results
      int expectedErrors = threadCount * operationsPerThread;
      assertEquals(
          expectedErrors,
          totalErrors.get(),
          "All error operations should have thrown exceptions");

      assertTrue(
          successfulRecoveries.get() >= expectedErrors * 0.9,
          "At least 90% of recoveries should succeed. Successful: "
              + successfulRecoveries.get()
              + ", Expected: "
              + expectedErrors);

      // Runtime should remain valid
      assertTrue(runtime.isValid(), "Runtime should remain valid after concurrent errors");

      // Print any unexpected error messages for debugging
      if (!errorMessages.isEmpty()) {
        System.out.println("Error messages from concurrent test:");
        errorMessages.forEach(System.out::println);
      }
    }
  }

  @Test
  @DisplayName("Error recovery with compilation failures")
  void testCompilationErrorRecovery() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Successful compilation
      Module validModule = runtime.compileModule(engine, VALID_MODULE);
      Instance validInstance = runtime.instantiateModule(store, validModule);

      Object result1 = validInstance.getExportedFunction("test").call();
      assertEquals(42, result1, "Valid module should work before compilation errors");

      // Compilation error
      assertThrows(
          WasmException.class,
          () -> runtime.compileModule(engine, INVALID_MODULE),
          "Invalid module should throw compilation exception");

      // Recovery: valid module should still work
      Object result2 = validInstance.getExportedFunction("test").call();
      assertEquals(42, result2, "Valid module should work after compilation error");

      // New compilation should work
      Module newValidModule = runtime.compileModule(engine, VALID_MODULE);
      Instance newValidInstance = runtime.instantiateModule(store, newValidModule);

      Object result3 = newValidInstance.getExportedFunction("test").call();
      assertEquals(42, result3, "New valid module should work after compilation error");

      // Multiple compilation errors
      for (int i = 0; i < 10; i++) {
        assertThrows(
            WasmException.class,
            () -> runtime.compileModule(engine, INVALID_MODULE),
            "Multiple compilation errors should be handled");
      }

      // Final recovery
      Object result4 = validInstance.getExportedFunction("test").call();
      assertEquals(42, result4, "Original module should still work after multiple compilation errors");

      assertTrue(runtime.isValid(), "Runtime should remain valid after compilation errors");
    }
  }

  @Test
  @DisplayName("Error recovery with resource exhaustion simulation")
  void testResourceExhaustionRecovery() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Baseline: normal operation
      Module validModule = runtime.compileModule(engine, VALID_MODULE);
      Instance validInstance = runtime.instantiateModule(store, validModule);

      Object baselineResult = validInstance.getExportedFunction("test").call();
      assertEquals(42, baselineResult, "Baseline operation should succeed");

      // Simulate resource pressure by creating many stores and instances
      List<Store> stores = new ArrayList<>();
      List<Instance> instances = new ArrayList<>();

      try {
        for (int i = 0; i < 100; i++) {
          Store newStore = runtime.createStore(engine);
          stores.add(newStore);

          Instance newInstance = runtime.instantiateModule(newStore, validModule);
          instances.add(newInstance);

          // Verify each instance works
          Object result = newInstance.getExportedFunction("test").call();
          assertEquals(42, result, "Instance " + i + " should work");
        }

        // Original instance should still work
        Object result = validInstance.getExportedFunction("test").call();
        assertEquals(42, result, "Original instance should work under resource pressure");

      } catch (WasmException | OutOfMemoryError e) {
        // Resource exhaustion is acceptable in this test
        System.out.println("Resource exhaustion encountered: " + e.getMessage());
      } finally {
        // Clean up resources
        instances.clear();
        stores.clear();
        System.gc();
      }

      // Recovery: original instance should still work after cleanup
      Object recoveryResult = validInstance.getExportedFunction("test").call();
      assertEquals(42, recoveryResult, "Original instance should work after resource cleanup");

      // Should be able to create new instances after cleanup
      Store newStore = runtime.createStore(engine);
      Instance newInstance = runtime.instantiateModule(newStore, validModule);
      Object newResult = newInstance.getExportedFunction("test").call();
      assertEquals(42, newResult, "New instance should work after resource recovery");

      assertTrue(runtime.isValid(), "Runtime should remain valid after resource exhaustion");
    }
  }

  @Test
  @DisplayName("Error recovery state consistency")
  void testErrorRecoveryStateConsistency() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Test that error recovery doesn't corrupt internal state
      for (int iteration = 0; iteration < 10; iteration++) {
        Store store = runtime.createStore(engine);
        Module validModule = runtime.compileModule(engine, VALID_MODULE);
        Module trapModule = runtime.compileModule(engine, TRAP_MODULE);

        Instance validInstance = runtime.instantiateModule(store, validModule);
        Instance trapInstance = runtime.instantiateModule(store, trapModule);

        // Pattern: valid -> error -> valid -> error -> valid
        Object result1 = validInstance.getExportedFunction("test").call();
        assertEquals(42, result1, "Valid call 1 should succeed in iteration " + iteration);

        assertThrows(
            WasmException.class,
            () -> trapInstance.getExportedFunction("trap").call(),
            "Trap should throw in iteration " + iteration);

        Object result2 = validInstance.getExportedFunction("test").call();
        assertEquals(42, result2, "Valid call 2 should succeed in iteration " + iteration);

        assertThrows(
            WasmException.class,
            () -> trapInstance.getExportedFunction("trap").call(),
            "Second trap should throw in iteration " + iteration);

        Object result3 = validInstance.getExportedFunction("test").call();
        assertEquals(42, result3, "Valid call 3 should succeed in iteration " + iteration);

        // Verify runtime state consistency
        assertTrue(
            runtime.isValid(),
            "Runtime should remain valid throughout iteration " + iteration);
      }
    }
  }

  @Test
  @DisplayName("Error recovery with mixed runtime types")
  void testMixedRuntimeRecovery() throws WasmException {
    List<RuntimeType> availableRuntimes = new ArrayList<>();
    for (RuntimeType type : RuntimeType.values()) {
      if (WasmRuntimeFactory.isRuntimeAvailable(type)) {
        availableRuntimes.add(type);
      }
    }

    if (availableRuntimes.size() < 2) {
      // Need at least 2 runtime types for this test
      System.out.println("Skipping mixed runtime test: insufficient runtime types available");
      return;
    }

    // Test error recovery across different runtime types
    RuntimeType runtime1Type = availableRuntimes.get(0);
    RuntimeType runtime2Type = availableRuntimes.get(1);

    try (WasmRuntime runtime1 = WasmRuntimeFactory.create(runtime1Type);
        WasmRuntime runtime2 = WasmRuntimeFactory.create(runtime2Type)) {

      Engine engine1 = runtime1.createEngine();
      Engine engine2 = runtime2.createEngine();

      Store store1 = runtime1.createStore(engine1);
      Store store2 = runtime2.createStore(engine2);

      Module validModule1 = runtime1.compileModule(engine1, VALID_MODULE);
      Module validModule2 = runtime2.compileModule(engine2, VALID_MODULE);
      Module trapModule1 = runtime1.compileModule(engine1, TRAP_MODULE);

      Instance validInstance1 = runtime1.instantiateModule(store1, validModule1);
      Instance validInstance2 = runtime2.instantiateModule(store2, validModule2);
      Instance trapInstance1 = runtime1.instantiateModule(store1, trapModule1);

      // Normal operation on both runtimes
      Object result1 = validInstance1.getExportedFunction("test").call();
      Object result2 = validInstance2.getExportedFunction("test").call();
      assertEquals(42, result1, "Runtime 1 should work normally");
      assertEquals(42, result2, "Runtime 2 should work normally");

      // Error on runtime 1
      assertThrows(
          WasmException.class,
          () -> trapInstance1.getExportedFunction("trap").call(),
          "Runtime 1 trap should throw exception");

      // Recovery: both runtimes should still work
      Object recovery1 = validInstance1.getExportedFunction("test").call();
      Object recovery2 = validInstance2.getExportedFunction("test").call();
      assertEquals(42, recovery1, "Runtime 1 should recover");
      assertEquals(42, recovery2, "Runtime 2 should remain unaffected");

      assertTrue(runtime1.isValid(), "Runtime 1 should remain valid");
      assertTrue(runtime2.isValid(), "Runtime 2 should remain valid");
    }
  }

  @Test
  @DisplayName("Asynchronous error recovery")
  void testAsynchronousErrorRecovery() throws Exception {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      final int asyncTaskCount = 20;
      List<CompletableFuture<Boolean>> futures = new ArrayList<>();
      final AtomicReference<Exception> asyncError = new AtomicReference<>();

      for (int i = 0; i < asyncTaskCount; i++) {
        final int taskId = i;
        CompletableFuture<Boolean> future =
            CompletableFuture.supplyAsync(
                () -> {
                  try {
                    Store store = runtime.createStore(engine);
                    Module validModule = runtime.compileModule(engine, VALID_MODULE);
                    Module trapModule = runtime.compileModule(engine, TRAP_MODULE);

                    Instance validInstance = runtime.instantiateModule(store, validModule);
                    Instance trapInstance = runtime.instantiateModule(store, trapModule);

                    // Pattern: valid -> error -> recovery
                    Object result1 = validInstance.getExportedFunction("test").call();
                    if (!Integer.valueOf(42).equals(result1)) {
                      throw new RuntimeException("Unexpected result: " + result1);
                    }

                    // Trigger error
                    try {
                      trapInstance.getExportedFunction("trap").call();
                      throw new RuntimeException("Expected trap exception");
                    } catch (WasmException e) {
                      // Expected
                    }

                    // Recovery
                    Object result2 = validInstance.getExportedFunction("test").call();
                    if (!Integer.valueOf(42).equals(result2)) {
                      throw new RuntimeException("Recovery failed: " + result2);
                    }

                    return true;

                  } catch (Exception e) {
                    asyncError.set(e);
                    return false;
                  }
                });

        futures.add(future);
      }

      // Wait for all tasks to complete
      CompletableFuture<Void> allTasks =
          CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
      allTasks.get(30, TimeUnit.SECONDS);

      // Verify all tasks succeeded
      for (int i = 0; i < futures.size(); i++) {
        Boolean result = futures.get(i).get();
        assertTrue(result, "Async task " + i + " should succeed");
      }

      Exception error = asyncError.get();
      if (error != null) {
        throw new RuntimeException("Async error occurred", error);
      }

      assertTrue(runtime.isValid(), "Runtime should remain valid after async error recovery");
    }
  }

  @Test
  @DisplayName("Error recovery memory consistency")
  void testErrorRecoveryMemoryConsistency() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Force garbage collection and measure initial memory
      System.gc();
      Thread.yield();
      long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      // Perform many error/recovery cycles
      for (int cycle = 0; cycle < 100; cycle++) {
        Store store = runtime.createStore(engine);
        Module validModule = runtime.compileModule(engine, VALID_MODULE);
        Module trapModule = runtime.compileModule(engine, TRAP_MODULE);

        Instance validInstance = runtime.instantiateModule(store, validModule);
        Instance trapInstance = runtime.instantiateModule(store, trapModule);

        // Multiple error/recovery sequences per cycle
        for (int seq = 0; seq < 10; seq++) {
          // Normal operation
          Object result1 = validInstance.getExportedFunction("test").call();
          assertEquals(42, result1, "Valid operation should succeed");

          // Error
          assertThrows(
              WasmException.class,
              () -> trapInstance.getExportedFunction("trap").call(),
              "Trap should throw");

          // Recovery
          Object result2 = validInstance.getExportedFunction("test").call();
          assertEquals(42, result2, "Recovery should succeed");
        }

        // Periodic garbage collection
        if (cycle % 20 == 0) {
          System.gc();
          Thread.yield();
        }
      }

      // Force final garbage collection
      System.gc();
      Thread.yield();
      long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      // Memory should not have increased dramatically (allow 50MB increase)
      long memoryIncrease = finalMemory - initialMemory;
      assertTrue(
          memoryIncrease < 50 * 1024 * 1024,
          "Memory should remain consistent after error recovery cycles. Initial: "
              + initialMemory
              + ", Final: "
              + finalMemory
              + ", Increase: "
              + memoryIncrease);

      assertTrue(runtime.isValid(), "Runtime should remain valid after memory consistency test");
    }
  }
}
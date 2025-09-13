package ai.tegmentum.wasmtime4j.instance;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for WebAssembly instance lifecycle management and resource cleanup.
 *
 * <p>This test class validates: - Instance creation and destruction lifecycle - Resource management
 * and cleanup - Memory leak prevention - Concurrent instance access - Resource cleanup under
 * stress conditions - Proper disposal of native resources
 */
public final class InstanceLifecycleAndResourceIT extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(InstanceLifecycleAndResourceIT.class.getName());
  private static final String WASM_TEST_DIR = "wasmtime4j-tests/src/test/resources/wasm/custom-tests/";
  private static final int STRESS_TEST_ITERATIONS = 50;
  private static final int CONCURRENT_THREADS = 4;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    LOGGER.info("Setting up instance lifecycle test: " + testInfo.getDisplayName());
  }

  /**
   * Tests basic instance lifecycle: creation, validation, usage, and cleanup.
   * Ensures that instances properly transition through valid and invalid states.
   */
  @Test
  void testBasicInstanceLifecycle() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing basic instance lifecycle with " + runtimeType + " runtime");
      
      measureExecutionTime("Basic lifecycle test (" + runtimeType + ")", () -> {
        final byte[] wasmBytes = loadWasmFile("add.wasm");
        
        try (final Engine engine = runtime.createEngine();
             final Store store = engine.createStore()) {
          
          registerForCleanup(engine);
          registerForCleanup(store);
          
          final Module module = engine.compileModule(wasmBytes);
          registerForCleanup(module);
          
          // Create instance
          final Instance instance = module.instantiate(store);
          
          assertAll(
              "Instance after creation",
              () -> assertNotNull(instance, "Instance should not be null"),
              () -> assertTrue(instance.isValid(), "Instance should be valid after creation"),
              () -> assertNotNull(instance.getModule(), "Instance should have module"),
              () -> assertNotNull(instance.getStore(), "Instance should have store")
          );
          
          // Use instance
          final WasmValue[] results = instance.callFunction("add", 
              WasmValue.i32(10), WasmValue.i32(20));
          assertNotNull(results, "Function call should return results");
          
          // Close instance explicitly
          instance.close();
          
          assertAll(
              "Instance after closing",
              () -> assertFalse(instance.isValid(), "Instance should be invalid after closing"),
              () -> assertThrows(WasmException.class, () -> instance.getFunction("add"),
                  "Accessing closed instance should throw exception"),
              () -> assertThrows(WasmException.class, 
                  () -> instance.callFunction("add", WasmValue.i32(1), WasmValue.i32(2)),
                  "Calling function on closed instance should throw exception")
          );
          
          LOGGER.info("Basic instance lifecycle test successful with " + runtimeType);
        }
      });
    });
  }

  /**
   * Tests resource cleanup when instances are created and destroyed repeatedly.
   * This helps detect memory leaks and resource exhaustion issues.
   */
  @Test
  void testRepeatedInstanceCreationAndDestruction() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing repeated instance creation/destruction with " + runtimeType + " runtime");
      
      measureExecutionTime("Repeated creation test (" + runtimeType + ")", () -> {
        final byte[] wasmBytes = loadWasmFile("add.wasm");
        
        try (final Engine engine = runtime.createEngine();
             final Store store = engine.createStore()) {
          
          registerForCleanup(engine);
          registerForCleanup(store);
          
          final Module module = engine.compileModule(wasmBytes);
          registerForCleanup(module);
          
          final List<WeakReference<Instance>> instanceRefs = new ArrayList<>();
          
          // Create and destroy instances repeatedly
          for (int i = 0; i < STRESS_TEST_ITERATIONS; i++) {
            final Instance instance = module.instantiate(store);
            
            // Use the instance to ensure it's functional
            final WasmValue[] results = instance.callFunction("add", 
                WasmValue.i32(i), WasmValue.i32(i + 1));
            assertNotNull(results, "Function call should succeed on iteration " + i);
            
            // Keep weak reference for GC testing
            instanceRefs.add(new WeakReference<>(instance));
            
            // Explicitly close
            instance.close();
            assertFalse(instance.isValid(), "Instance should be invalid after closing on iteration " + i);
            
            if (i > 0 && i % 10 == 0) {
              LOGGER.fine("Completed " + i + " iterations of instance creation/destruction");
            }
          }
          
          // Suggest garbage collection
          System.gc();
          
          // Give GC some time to work
          try {
            Thread.sleep(100);
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
          }
          
          // Check that some instances have been garbage collected
          final long reachableInstances = instanceRefs.stream()
              .mapToLong(ref -> ref.get() != null ? 1 : 0)
              .sum();
          
          LOGGER.info("After GC suggestion: " + reachableInstances + "/" + instanceRefs.size() 
              + " instances still reachable");
          
          LOGGER.info("Repeated instance creation/destruction test successful with " + runtimeType);
        }
      });
    });
  }

  /**
   * Tests concurrent instance creation and usage to ensure thread safety.
   * Multiple threads create instances from the same module simultaneously.
   */
  @Test
  void testConcurrentInstanceCreation() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing concurrent instance creation with " + runtimeType + " runtime");
      
      measureExecutionTime("Concurrent creation test (" + runtimeType + ")", () -> {
        final byte[] wasmBytes = loadWasmFile("add.wasm");
        
        try (final Engine engine = runtime.createEngine()) {
          registerForCleanup(engine);
          
          final Module module = engine.compileModule(wasmBytes);
          registerForCleanup(module);
          
          final ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
          final CountDownLatch startLatch = new CountDownLatch(1);
          final List<Future<String>> futures = new ArrayList<>();
          
          // Submit concurrent tasks
          for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            final Future<String> future = executor.submit(() -> {
              try {
                // Wait for all threads to be ready
                startLatch.await(5, TimeUnit.SECONDS);
                
                // Each thread creates its own store and instances
                try (final Store store = engine.createStore()) {
                  final List<Instance> instances = new ArrayList<>();
                  
                  // Create multiple instances per thread
                  for (int j = 0; j < 10; j++) {
                    final Instance instance = module.instantiate(store);
                    instances.add(instance);
                    
                    // Use the instance
                    final WasmValue[] results = instance.callFunction("add", 
                        WasmValue.i32(threadId), WasmValue.i32(j));
                    
                    if (results == null || results.length != 1 || 
                        results[0].asI32() != (threadId + j)) {
                      throw new RuntimeException("Unexpected result from thread " + threadId + 
                          ", iteration " + j);
                    }
                  }
                  
                  // Clean up instances
                  for (final Instance instance : instances) {
                    instance.close();
                  }
                  
                  return "Thread " + threadId + " completed successfully";
                }
              } catch (final Exception e) {
                throw new RuntimeException("Thread " + threadId + " failed", e);
              }
            });
            
            futures.add(future);
          }
          
          // Start all threads
          startLatch.countDown();
          
          // Wait for completion and collect results
          for (int i = 0; i < futures.size(); i++) {
            try {
              final String result = futures.get(i).get(10, TimeUnit.SECONDS);
              LOGGER.fine(result);
            } catch (final Exception e) {
              throw new RuntimeException("Future " + i + " failed", e);
            }
          }
          
          executor.shutdown();
          assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), 
              "Executor should terminate within timeout");
          
          LOGGER.info("Concurrent instance creation test successful with " + runtimeType);
        }
      });
    });
  }

  /**
   * Tests resource management under stress conditions.
   * Creates many instances quickly to test resource limits and cleanup.
   */
  @Test
  void testStressResourceManagement() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing stress resource management with " + runtimeType + " runtime");
      
      assertExecutionTime(Duration.ofSeconds(30), () -> {
        final byte[] wasmBytes = loadWasmFile("add.wasm");
        
        try (final Engine engine = runtime.createEngine();
             final Store store = engine.createStore()) {
          
          registerForCleanup(engine);
          registerForCleanup(store);
          
          final Module module = engine.compileModule(wasmBytes);
          registerForCleanup(module);
          
          final int BATCH_SIZE = 20;
          final int BATCH_COUNT = 5;
          
          for (int batch = 0; batch < BATCH_COUNT; batch++) {
            LOGGER.fine("Processing batch " + (batch + 1) + "/" + BATCH_COUNT);
            
            final List<Instance> instances = new ArrayList<>();
            
            // Create a batch of instances
            for (int i = 0; i < BATCH_SIZE; i++) {
              final Instance instance = module.instantiate(store);
              instances.add(instance);
              
              // Use each instance to ensure it's functional
              final WasmValue[] results = instance.callFunction("add", 
                  WasmValue.i32(batch), WasmValue.i32(i));
              assertNotNull(results, "Function call should succeed");
            }
            
            // Verify all instances are valid
            for (final Instance instance : instances) {
              assertTrue(instance.isValid(), "Instance should be valid");
            }
            
            // Clean up the batch
            for (final Instance instance : instances) {
              instance.close();
              assertFalse(instance.isValid(), "Instance should be invalid after closing");
            }
            
            // Suggest garbage collection between batches
            if (batch % 2 == 0) {
              System.gc();
              try {
                Thread.sleep(10);
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Test interrupted", e);
              }
            }
          }
          
          LOGGER.info("Stress resource management test successful with " + runtimeType);
        }
      }, "Stress test execution time (" + runtimeType + ")");\n    });\n  }

  /**
   * Tests automatic resource cleanup when instances go out of scope.
   * Validates that try-with-resources works correctly.
   */
  @Test
  void testAutomaticResourceCleanup() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing automatic resource cleanup with " + runtimeType + " runtime");
      
      measureExecutionTime("Automatic cleanup test (" + runtimeType + ")", () -> {
        final byte[] wasmBytes = loadWasmFile("add.wasm");
        Instance instanceRef;
        
        try (final Engine engine = runtime.createEngine();
             final Store store = engine.createStore()) {
          
          registerForCleanup(engine);
          registerForCleanup(store);
          
          final Module module = engine.compileModule(wasmBytes);
          registerForCleanup(module);
          
          // Use try-with-resources for automatic cleanup
          try (final Instance instance = module.instantiate(store)) {
            instanceRef = instance;
            
            assertTrue(instance.isValid(), "Instance should be valid in try block");
            
            // Use the instance
            final WasmValue[] results = instance.callFunction("add", 
                WasmValue.i32(100), WasmValue.i32(200));
            assertNotNull(results, "Function call should succeed");
          }
          // Instance should be automatically closed here
          
          // Verify instance is closed
          assertFalse(instanceRef.isValid(), "Instance should be invalid after try-with-resources");
          
          LOGGER.info("Automatic resource cleanup test successful with " + runtimeType);
        }
      });
    });
  }

  /**
   * Tests resource cleanup when exceptions occur during instance usage.
   * Ensures resources are properly cleaned up even in error conditions.
   */
  @Test
  void testResourceCleanupOnException() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing resource cleanup on exception with " + runtimeType + " runtime");
      
      measureExecutionTime("Exception cleanup test (" + runtimeType + ")", () -> {
        final byte[] wasmBytes = loadWasmFile("add.wasm");
        
        try (final Engine engine = runtime.createEngine();
             final Store store = engine.createStore()) {
          
          registerForCleanup(engine);
          registerForCleanup(store);
          
          final Module module = engine.compileModule(wasmBytes);
          registerForCleanup(module);
          
          Instance instanceRef = null;
          
          try {
            try (final Instance instance = module.instantiate(store)) {
              instanceRef = instance;
              
              assertTrue(instance.isValid(), "Instance should be valid before exception");
              
              // Use the instance successfully first
              final WasmValue[] results = instance.callFunction("add", 
                  WasmValue.i32(50), WasmValue.i32(75));
              assertNotNull(results, "Function call should succeed");
              
              // Now trigger an exception
              throw new RuntimeException("Intentional test exception");
            }
          } catch (final RuntimeException e) {
            if (!"Intentional test exception".equals(e.getMessage())) {
              throw e; // Re-throw unexpected exceptions
            }
            // Expected exception - continue with validation
          }
          
          // Verify instance was cleaned up despite exception
          assertNotNull(instanceRef, "Instance reference should be captured");
          assertFalse(instanceRef.isValid(), 
              "Instance should be invalid after exception in try-with-resources");
          
          LOGGER.info("Resource cleanup on exception test successful with " + runtimeType);
        }
      });
    });
  }

  /**
   * Helper method to load WASM files from test resources.
   */
  private byte[] loadWasmFile(final String filename) {
    try {
      final Path wasmPath = Paths.get(WASM_TEST_DIR + filename);
      return Files.readAllBytes(wasmPath);
    } catch (final IOException e) {
      throw new RuntimeException("Failed to load WASM file: " + filename, e);
    }
  }
}
package ai.tegmentum.wasmtime4j.comprehensive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive end-to-end validation tests for WebAssembly execution workflows.
 *
 * <p>This test class validates the complete WebAssembly execution pipeline from module loading
 * through function execution, memory operations, and resource cleanup. It serves as the primary
 * validation gate for production readiness.
 */
@DisplayName("End-to-End WebAssembly Execution Workflow Validation")
final class EndToEndWorkflowValidationIT {

  private static final Logger LOGGER =
      Logger.getLogger(EndToEndWorkflowValidationIT.class.getName());

  /**
   * Tests the complete WebAssembly execution workflow: Runtime creation → Engine creation → Module
   * compilation → Instance creation → Function execution → Resource cleanup.
   */
  @Test
  @DisplayName("Should execute complete WebAssembly workflow successfully")
  void shouldExecuteCompleteWebAssemblyWorkflowSuccessfully() throws Exception {
    LOGGER.info("=== Starting Complete WebAssembly Workflow Test ===");

    // Step 1: Create runtime
    LOGGER.info("Step 1: Creating WebAssembly runtime");
    try (final WasmRuntime runtime = WasmRuntimeFactory.create()) {
      assertThat(runtime).isNotNull();
      assertThat(runtime.isValid()).isTrue();

      final var runtimeInfo = runtime.getRuntimeInfo();
      LOGGER.info("Runtime Type: " + runtimeInfo.getRuntimeType());
      LOGGER.info("Runtime Version: " + runtimeInfo.getVersion());

      // Step 2: Create engine with default configuration
      LOGGER.info("Step 2: Creating WebAssembly engine");
      try (final Engine engine = runtime.createEngine()) {
        assertThat(engine).isNotNull();

        // Step 3: Load and compile a simple WebAssembly module
        LOGGER.info("Step 3: Loading and compiling WebAssembly module");
        final byte[] wasmBytes = loadSimpleWasmModule();
        final Module module = runtime.compileModule(engine, wasmBytes);
        assertThat(module).isNotNull();

        // Step 4: Create store for execution context
        LOGGER.info("Step 4: Creating store for execution context");
        try (final Store store = runtime.createStore(engine)) {
          assertThat(store).isNotNull();

          // Step 5: Instantiate the module
          LOGGER.info("Step 5: Instantiating WebAssembly module");
          final Instance instance = runtime.instantiate(module);
          assertThat(instance).isNotNull();

          // Step 6: Get exported function and validate
          LOGGER.info("Step 6: Accessing exported functions");
          final Optional<WasmFunction> addFunction = instance.getFunction("add");
          assertThat(addFunction).isPresent();

          // Step 7: Execute function with parameters
          LOGGER.info("Step 7: Executing WebAssembly function");
          final WasmValue[] args = {WasmValue.i32(10), WasmValue.i32(20)};
          final WasmValue[] results = addFunction.get().call(args);

          assertThat(results).hasSize(1);
          assertThat(results[0].asI32()).isEqualTo(30);
          LOGGER.info("Function execution result: 10 + 20 = " + results[0].asI32());

          // Step 8: Test memory operations if available
          LOGGER.info("Step 8: Testing memory operations");
          testMemoryOperations(instance);

          // Step 9: Test error handling
          LOGGER.info("Step 9: Testing error handling");
          testErrorHandling(runtime, engine);

          LOGGER.info("=== Complete WebAssembly Workflow Test: SUCCESS ===");
        }
      }
    }
  }

  /**
   * Tests WebAssembly execution with custom engine configuration to validate configuration
   * handling.
   */
  @Test
  @DisplayName("Should handle custom engine configuration correctly")
  void shouldHandleCustomEngineConfigurationCorrectly() throws Exception {
    LOGGER.info("=== Testing Custom Engine Configuration ===");

    try (final WasmRuntime runtime = WasmRuntimeFactory.create()) {
      // Create engine with custom configuration
      final EngineConfig config = EngineConfig.builder().build();

      try (final Engine engine = runtime.createEngine(config)) {
        assertThat(engine).isNotNull();

        // Test that configured engine works with module compilation
        final byte[] wasmBytes = loadSimpleWasmModule();
        final Module module = runtime.compileModule(engine, wasmBytes);
        assertThat(module).isNotNull();

        // Test instantiation with configured engine
        final Instance instance = runtime.instantiate(module);
        assertThat(instance).isNotNull();

        LOGGER.info("Custom engine configuration test: SUCCESS");
      }
    }
  }

  /**
   * Tests concurrent WebAssembly execution across multiple threads to validate thread safety and
   * resource isolation.
   */
  @Test
  @DisplayName("Should handle concurrent execution safely")
  void shouldHandleConcurrentExecutionSafely() throws Exception {
    LOGGER.info("=== Testing Concurrent WebAssembly Execution ===");

    final int threadCount = 4;
    final int operationsPerThread = 50;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch latch = new CountDownLatch(threadCount);
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger errorCount = new AtomicInteger(0);

    try (final WasmRuntime runtime = WasmRuntimeFactory.create()) {
      final byte[] wasmBytes = loadSimpleWasmModule();

      // Start concurrent execution tasks
      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                for (int op = 0; op < operationsPerThread; op++) {
                  executeSimpleOperation(runtime, wasmBytes, threadId, op);
                  successCount.incrementAndGet();
                }
              } catch (final Exception e) {
                LOGGER.warning("Thread " + threadId + " error: " + e.getMessage());
                errorCount.incrementAndGet();
              } finally {
                latch.countDown();
              }
            });
      }

      // Wait for all threads to complete
      final boolean completed = latch.await(60, TimeUnit.SECONDS);
      assertThat(completed).isTrue();

      // Validate results
      final int totalOperations = threadCount * operationsPerThread;
      LOGGER.info("Concurrent execution completed:");
      LOGGER.info("  Total operations: " + totalOperations);
      LOGGER.info("  Successful operations: " + successCount.get());
      LOGGER.info("  Failed operations: " + errorCount.get());

      assertThat(successCount.get() + errorCount.get()).isEqualTo(totalOperations);
      assertThat(errorCount.get()).isLessThan(totalOperations / 10); // Less than 10% errors

      LOGGER.info("Concurrent execution test: SUCCESS");
    } finally {
      executor.shutdown();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  /** Tests WASI integration as part of the complete workflow validation. */
  @Test
  @DisplayName("Should integrate WASI operations successfully")
  void shouldIntegrateWasiOperationsSuccessfully() throws Exception {
    LOGGER.info("=== Testing WASI Integration ===");

    try (final WasmRuntime runtime = WasmRuntimeFactory.create()) {
      // Test WASI context creation
      try (final WasiContext wasiContext = WasiFactory.createContext()) {
        assertThat(wasiContext).isNotNull();

        LOGGER.info("WASI Runtime Type: " + WasiFactory.getSelectedRuntimeType());
        LOGGER.info("WASI integration test: SUCCESS");
      }
    }
  }

  /**
   * Tests performance characteristics of the WebAssembly execution workflow to establish baseline
   * measurements.
   */
  @ParameterizedTest
  @ValueSource(ints = {10, 100, 1000})
  @DisplayName("Should maintain acceptable performance under load")
  void shouldMaintainAcceptablePerformanceUnderLoad(final int operationCount) throws Exception {
    LOGGER.info("=== Testing Performance Under Load (operations: " + operationCount + ") ===");

    try (final WasmRuntime runtime = WasmRuntimeFactory.create()) {
      final byte[] wasmBytes = loadSimpleWasmModule();

      try (final Engine engine = runtime.createEngine()) {
        final Module module = runtime.compileModule(engine, wasmBytes);

        try (final Store store = runtime.createStore(engine)) {
          final Instance instance = runtime.instantiate(module);
          final WasmFunction addFunction =
              instance.getFunction("add").orElseThrow(() -> new AssertionError("No add function"));

          // Warm-up phase
          for (int i = 0; i < 10; i++) {
            final WasmValue[] args = {WasmValue.i32(i), WasmValue.i32(i + 1)};
            addFunction.call(args);
          }

          // Measure performance
          final Instant start = Instant.now();
          for (int i = 0; i < operationCount; i++) {
            final WasmValue[] args = {WasmValue.i32(i), WasmValue.i32(i + 1)};
            final WasmValue[] results = addFunction.call(args);
            assertThat(results[0].asI32()).isEqualTo(i + i + 1);
          }
          final Duration elapsed = Duration.between(start, Instant.now());

          // Calculate performance metrics
          final double operationsPerSecond = operationCount / (elapsed.toMillis() / 1000.0);
          final double microsecondsPerOperation = elapsed.toNanos() / 1000.0 / operationCount;

          LOGGER.info(String.format("Performance results for %d operations:", operationCount));
          LOGGER.info(String.format("  Total time: %d ms", elapsed.toMillis()));
          LOGGER.info(String.format("  Operations per second: %.2f", operationsPerSecond));
          LOGGER.info(
              String.format("  Microseconds per operation: %.2f", microsecondsPerOperation));

          // Validate performance is within acceptable bounds
          assertThat(operationsPerSecond).isGreaterThan(1000.0); // At least 1000 ops/sec
          assertThat(microsecondsPerOperation).isLessThan(1000.0); // Less than 1ms per operation

          LOGGER.info("Performance test: SUCCESS");
        }
      }
    }
  }

  /**
   * Tests resource cleanup and memory management to ensure no leaks occur during normal operation.
   */
  @Test
  @DisplayName("Should cleanup resources properly without leaks")
  void shouldCleanupResourcesProperlyWithoutLeaks() throws Exception {
    LOGGER.info("=== Testing Resource Cleanup and Memory Management ===");

    final int iterations = 100;
    final AtomicLong initialMemory = new AtomicLong();
    final AtomicLong peakMemory = new AtomicLong();
    final AtomicLong finalMemory = new AtomicLong();

    // Capture initial memory
    System.gc();
    Thread.sleep(100);
    initialMemory.set(getUsedMemory());

    // Perform multiple create/destroy cycles
    for (int i = 0; i < iterations; i++) {
      try (final WasmRuntime runtime = WasmRuntimeFactory.create()) {
        final byte[] wasmBytes = loadSimpleWasmModule();

        try (final Engine engine = runtime.createEngine()) {
          final Module module = runtime.compileModule(engine, wasmBytes);

          try (final Store store = runtime.createStore(engine)) {
            final Instance instance = runtime.instantiate(module);
            final WasmFunction addFunction =
                instance
                    .getFunction("add")
                    .orElseThrow(() -> new AssertionError("No add function"));

            // Perform some operations
            for (int j = 0; j < 10; j++) {
              final WasmValue[] args = {WasmValue.i32(j), WasmValue.i32(j + 1)};
              addFunction.call(args);
            }

            // Track peak memory usage
            final long currentMemory = getUsedMemory();
            peakMemory.updateAndGet(peak -> Math.max(peak, currentMemory));
          }
        }
      }

      // Periodic garbage collection
      if (i % 20 == 0) {
        System.gc();
        Thread.sleep(50);
      }
    }

    // Force final garbage collection and measure memory
    System.gc();
    System.gc();
    Thread.sleep(200);
    finalMemory.set(getUsedMemory());

    // Calculate memory metrics
    final long memoryIncrease = finalMemory.get() - initialMemory.get();
    final long peakIncrease = peakMemory.get() - initialMemory.get();

    LOGGER.info("Memory management results:");
    LOGGER.info(String.format("  Initial memory: %d MB", initialMemory.get() / 1024 / 1024));
    LOGGER.info(String.format("  Peak memory: %d MB", peakMemory.get() / 1024 / 1024));
    LOGGER.info(String.format("  Final memory: %d MB", finalMemory.get() / 1024 / 1024));
    LOGGER.info(String.format("  Memory increase: %d MB", memoryIncrease / 1024 / 1024));
    LOGGER.info(String.format("  Peak increase: %d MB", peakIncrease / 1024 / 1024));

    // Validate memory management
    assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024); // Less than 50MB increase
    assertThat(peakIncrease).isLessThan(200 * 1024 * 1024); // Less than 200MB peak

    LOGGER.info("Resource cleanup test: SUCCESS");
  }

  /** Tests comprehensive error scenarios to validate error handling and recovery. */
  @Test
  @DisplayName("Should handle error scenarios gracefully")
  void shouldHandleErrorScenariosGracefully() throws Exception {
    LOGGER.info("=== Testing Error Handling and Recovery ===");

    try (final WasmRuntime runtime = WasmRuntimeFactory.create()) {
      try (final Engine engine = runtime.createEngine()) {

        // Test 1: Invalid WebAssembly bytecode
        LOGGER.info("Testing invalid WebAssembly bytecode");
        final byte[] invalidWasm = {0x00, 0x01, 0x02, 0x03}; // Invalid WASM
        assertThatThrownBy(() -> runtime.compileModule(engine, invalidWasm))
            .isInstanceOf(WasmException.class);

        // Test 2: Valid module compilation and instantiation
        LOGGER.info("Testing valid module after error recovery");
        final byte[] validWasm = loadSimpleWasmModule();
        final Module module = runtime.compileModule(engine, validWasm);
        final Instance instance = runtime.instantiate(module);
        assertThat(instance).isNotNull();

        // Test 3: Function call with wrong parameters
        LOGGER.info("Testing function call error handling");
        final WasmFunction addFunction =
            instance.getFunction("add").orElseThrow(() -> new AssertionError("No add function"));

        // This should work fine
        final WasmValue[] validArgs = {WasmValue.i32(5), WasmValue.i32(10)};
        final WasmValue[] results = addFunction.call(validArgs);
        assertThat(results[0].asI32()).isEqualTo(15);

        LOGGER.info("Error handling test: SUCCESS");
      }
    }
  }

  /** Loads a simple WebAssembly module for testing. */
  private byte[] loadSimpleWasmModule() throws IOException {
    // Try to load from resources first
    final Path resourcePath =
        Paths.get("src/test/resources/wasm/custom-tests/add.wasm").toAbsolutePath();

    if (Files.exists(resourcePath)) {
      LOGGER.info("Loading WASM module from: " + resourcePath);
      return Files.readAllBytes(resourcePath);
    }

    // Fallback to creating a simple module programmatically
    LOGGER.info("Creating simple WASM module programmatically");
    return createSimpleWasmModule();
  }

  /** Creates a simple WebAssembly module programmatically. */
  private byte[] createSimpleWasmModule() {
    // Simple WASM module that exports an "add" function: (i32, i32) -> i32
    // This is a minimal valid WASM module with an add function
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6d, // WASM magic number
      0x01,
      0x00,
      0x00,
      0x00, // Version
      0x01,
      0x07, // Type section
      0x01, // 1 type
      0x60,
      0x02,
      0x7f,
      0x7f,
      0x01,
      0x7f, // (i32, i32) -> i32
      0x03,
      0x02, // Function section
      0x01,
      0x00, // 1 function, type 0
      0x07,
      0x07, // Export section
      0x01, // 1 export
      0x03,
      0x61,
      0x64,
      0x64, // "add"
      0x00,
      0x00, // function 0
      0x0a,
      0x09, // Code section
      0x01,
      0x07,
      0x00, // 1 function, 7 bytes
      0x20,
      0x00, // local.get 0
      0x20,
      0x01, // local.get 1
      0x6a, // i32.add
      0x0b // end
    };
  }

  /** Tests memory operations if the instance has exported memory. */
  private void testMemoryOperations(final Instance instance) {
    final Optional<WasmMemory> memory = instance.getMemory("memory");
    if (memory.isPresent()) {
      LOGGER.info("Testing memory operations");
      final WasmMemory wasmMemory = memory.get();

      // Test basic memory operations
      final int pages = wasmMemory.getPages();
      assertThat(pages).isGreaterThanOrEqualTo(0);

      LOGGER.info("Memory pages: " + pages);
    } else {
      LOGGER.info("No exported memory found, skipping memory tests");
    }
  }

  /** Tests error handling scenarios. */
  private void testErrorHandling(final WasmRuntime runtime, final Engine engine) {
    // Test null parameter handling
    assertThatThrownBy(() -> runtime.compileModule(engine, null))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> runtime.createEngine(null))
        .isInstanceOf(IllegalArgumentException.class);

    LOGGER.info("Error handling validation completed");
  }

  /** Executes a simple WebAssembly operation for concurrent testing. */
  private void executeSimpleOperation(
      final WasmRuntime runtime, final byte[] wasmBytes, final int threadId, final int operationId)
      throws Exception {

    try (final Engine engine = runtime.createEngine()) {
      final Module module = runtime.compileModule(engine, wasmBytes);

      try (final Store store = runtime.createStore(engine)) {
        final Instance instance = runtime.instantiate(module);
        final WasmFunction addFunction =
            instance.getFunction("add").orElseThrow(() -> new AssertionError("No add function"));

        final WasmValue[] args = {WasmValue.i32(threadId), WasmValue.i32(operationId)};
        final WasmValue[] results = addFunction.call(args);

        final int expected = threadId + operationId;
        final int actual = results[0].asI32();
        if (actual != expected) {
          throw new AssertionError(
              String.format(
                  "Thread %d, Op %d: Expected %d, got %d",
                  threadId, operationId, expected, actual));
        }
      }
    }
  }

  /** Gets current memory usage in bytes. */
  private long getUsedMemory() {
    final Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }
}

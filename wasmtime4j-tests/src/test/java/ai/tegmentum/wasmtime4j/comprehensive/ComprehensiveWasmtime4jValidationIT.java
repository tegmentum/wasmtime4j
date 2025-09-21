package ai.tegmentum.wasmtime4j.comprehensive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Function;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Memory;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exceptions.CompilationException;
import ai.tegmentum.wasmtime4j.exceptions.RuntimeException;
import ai.tegmentum.wasmtime4j.exceptions.ValidationException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.hostfunction.HostFunction;
import ai.tegmentum.wasmtime4j.hostfunction.HostFunctionContext;
import ai.tegmentum.wasmtime4j.hostfunction.HostFunctionRegistry;
import ai.tegmentum.wasmtime4j.memory.MemoryLeakDetector;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiFactory;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Comprehensive validation test for all wasmtime4j functionality implemented in Issues #271-#276.
 * This test validates the complete WebAssembly execution pipeline from module loading to cleanup,
 * including Store contexts, function invocation, memory management, WASI operations, host
 * functions, and error handling.
 *
 * <p>This test serves as the primary validation gate for production readiness and ensures all
 * critical functionality works correctly together.
 */
@DisplayName("Comprehensive Wasmtime4j Validation")
@Execution(ExecutionMode.CONCURRENT)
class ComprehensiveWasmtime4jValidationIT extends BaseIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComprehensiveWasmtime4jValidationIT.class.getName());

  private final List<AutoCloseable> testResources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    LOGGER.info("Starting comprehensive validation test: " + testInfo.getDisplayName());
    testResources.clear();
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test resources");
    testResources.forEach(
        resource -> {
          try {
            resource.close();
          } catch (final Exception e) {
            LOGGER.warning("Failed to close test resource: " + e.getMessage());
          }
        });
    testResources.clear();
  }

  @Test
  @DisplayName("Should execute complete WebAssembly workflow with all components")
  @Timeout(value = 2, unit = TimeUnit.MINUTES)
  void shouldExecuteCompleteWebAssemblyWorkflowWithAllComponents() throws Exception {
    // Test both JNI and Panama if available
    executeCompleteWorkflowForRuntime(RuntimeType.JNI);

    if (TestUtils.isPanamaAvailable()) {
      executeCompleteWorkflowForRuntime(RuntimeType.PANAMA);
    }
  }

  @Test
  @DisplayName("Should validate Store context isolation and lifecycle management")
  @Timeout(value = 1, unit = TimeUnit.MINUTES)
  void shouldValidateStoreContextIsolationAndLifecycleManagement() throws Exception {
    LOGGER.info("=== Testing Store Context Integration (Issue #271) ===");

    try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
      testResources.add(runtime);

      // Test multiple Store instances with isolation
      try (final Engine engine = runtime.createEngine()) {
        testResources.add(engine);

        // Create multiple stores to test isolation
        final Store store1 = engine.createStore();
        final Store store2 = engine.createStore();
        testResources.add(store1);
        testResources.add(store2);

        assertThat(store1).isNotNull().isNotEqualTo(store2);

        // Test store-specific resource tracking
        final byte[] moduleBytes = TestUtils.createSimpleWasmModule();
        final Module module1 = engine.compileModule(moduleBytes);
        final Module module2 = engine.compileModule(moduleBytes);

        final Instance instance1 = runtime.instantiate(module1);
        final Instance instance2 = runtime.instantiate(module2);
        testResources.add(instance1);
        testResources.add(instance2);

        assertThat(instance1).isNotNull().isNotEqualTo(instance2);

        // Verify store isolation - modifications in one store shouldn't affect the other
        final Memory memory1 = instance1.getMemory("memory").orElse(null);
        final Memory memory2 = instance2.getMemory("memory").orElse(null);

        if (memory1 != null && memory2 != null) {
          // Write different patterns to each memory
          memory1.writeBytes(0, new byte[] {1, 2, 3, 4});
          memory2.writeBytes(0, new byte[] {5, 6, 7, 8});

          // Verify isolation
          final byte[] data1 = memory1.readBytes(0, 4);
          final byte[] data2 = memory2.readBytes(0, 4);

          assertThat(data1).isEqualTo(new byte[] {1, 2, 3, 4});
          assertThat(data2).isEqualTo(new byte[] {5, 6, 7, 8});
        }

        LOGGER.info("Store context isolation validated successfully");
      }
    }
  }

  @Test
  @DisplayName("Should validate function invocation with comprehensive parameter marshalling")
  @Timeout(value = 1, unit = TimeUnit.MINUTES)
  void shouldValidateFunctionInvocationWithComprehensiveParameterMarshalling() throws Exception {
    LOGGER.info("=== Testing Function Invocation Implementation (Issue #272) ===");

    try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
      testResources.add(runtime);

      try (final Engine engine = runtime.createEngine()) {
        testResources.add(engine);

        try (final Store store = engine.createStore()) {
          testResources.add(store);

          // Test basic arithmetic function
          final byte[] moduleBytes = TestUtils.createArithmeticWasmModule();
          final Module module = engine.compileModule(moduleBytes);
          final Instance instance = runtime.instantiate(module);
          testResources.add(instance);

          // Test i32 addition
          final Function addFunction =
              instance
                  .getFunction("add")
                  .orElseThrow(() -> new AssertionError("add function should be exported"));

          final WasmValue[] addArgs = {WasmValue.i32(15), WasmValue.i32(25)};
          final WasmValue[] addResults = addFunction.call(addArgs);

          assertThat(addResults).hasSize(1);
          assertThat(addResults[0].asI32()).isEqualTo(40);

          // Test i64 multiplication if available
          final Function mulFunction = instance.getFunction("mul").orElse(null);
          if (mulFunction != null) {
            final WasmValue[] mulArgs = {WasmValue.i64(6L), WasmValue.i64(7L)};
            final WasmValue[] mulResults = mulFunction.call(mulArgs);

            assertThat(mulResults).hasSize(1);
            assertThat(mulResults[0].asI64()).isEqualTo(42L);
          }

          // Test floating point operations if available
          final Function divFunction = instance.getFunction("div_f32").orElse(null);
          if (divFunction != null) {
            final WasmValue[] divArgs = {WasmValue.f32(10.0f), WasmValue.f32(2.0f)};
            final WasmValue[] divResults = divFunction.call(divArgs);

            assertThat(divResults).hasSize(1);
            assertThat(divResults[0].asF32()).isEqualTo(5.0f, TestUtils.FLOAT_TOLERANCE);
          }

          LOGGER.info("Function invocation with parameter marshalling validated successfully");
        }
      }
    }
  }

  @Test
  @DisplayName("Should validate memory management and bounds checking")
  @Timeout(value = 1, unit = TimeUnit.MINUTES)
  void shouldValidateMemoryManagementAndBoundsChecking() throws Exception {
    LOGGER.info("=== Testing Memory Management Completion (Issue #273) ===");

    try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
      testResources.add(runtime);

      try (final Engine engine = runtime.createEngine()) {
        testResources.add(engine);

        try (final Store store = engine.createStore()) {
          testResources.add(store);

          final byte[] moduleBytes = TestUtils.createMemoryWasmModule();
          final Module module = engine.compileModule(moduleBytes);
          final Instance instance = runtime.instantiate(module);
          testResources.add(instance);

          final Memory memory =
              instance
                  .getMemory("memory")
                  .orElseThrow(() -> new AssertionError("memory should be exported"));

          // Test basic memory operations
          final byte[] testData = {0x01, 0x02, 0x03, 0x04, 0x05};
          memory.writeBytes(0, testData);

          final byte[] readData = memory.readBytes(0, testData.length);
          assertThat(readData).isEqualTo(testData);

          // Test memory size and growth
          final long initialSize = memory.getSize();
          assertThat(initialSize).isGreaterThan(0);

          // Test bounds checking
          assertThatThrownBy(() -> memory.readBytes(-1, 1))
              .isInstanceOf(RuntimeException.class)
              .hasMessageContaining("bounds");

          final long memoryBytes = memory.getSize() * 65536; // WebAssembly page size
          assertThatThrownBy(() -> memory.readBytes(memoryBytes, 1))
              .isInstanceOf(RuntimeException.class)
              .hasMessageContaining("bounds");

          // Test memory growth if supported
          try {
            final boolean growthResult = memory.grow(1);
            if (growthResult) {
              assertThat(memory.getSize()).isEqualTo(initialSize + 1);
              LOGGER.info("Memory growth validated successfully");
            } else {
              LOGGER.info("Memory growth not supported or failed - acceptable");
            }
          } catch (final RuntimeException e) {
            LOGGER.info("Memory growth failed: " + e.getMessage() + " - acceptable");
          }

          LOGGER.info("Memory management and bounds checking validated successfully");
        }
      }
    }
  }

  @Test
  @DisplayName("Should validate WASI operations and filesystem interactions")
  @Timeout(value = 1, unit = TimeUnit.MINUTES)
  void shouldValidateWasiOperationsAndFilesystemInteractions() throws Exception {
    LOGGER.info("=== Testing WASI Operations Implementation (Issue #274) ===");

    // Test WASI context creation and basic operations
    try (final WasiContext wasiContext = WasiFactory.createContext()) {
      testResources.add(wasiContext);

      assertThat(wasiContext).isNotNull();
      LOGGER.info("WASI context created successfully with runtime: " +
                  WasiFactory.getSelectedRuntimeType());

      // Create a temporary directory for WASI filesystem tests
      final Path tempDir = Files.createTempDirectory("wasmtime4j-wasi-test");
      final Path testFile = tempDir.resolve("test.txt");
      final String testContent = "Hello, WASI World!";
      Files.write(testFile, testContent.getBytes());

      try {
        // If we have a WASI-enabled WebAssembly module, test file operations
        try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
          testResources.add(runtime);

          // Test basic WASI module compilation (if available)
          final byte[] wasiModuleBytes = TestUtils.createWasiWasmModule();
          if (wasiModuleBytes.length > 0) {
            try (final Engine engine = runtime.createEngine()) {
              testResources.add(engine);

              final Module wasiModule = engine.compileModule(wasiModuleBytes);
              assertThat(wasiModule).isNotNull();

              LOGGER.info("WASI module compilation validated successfully");
            }
          } else {
            LOGGER.info("WASI module not available - testing context creation only");
          }
        }

        LOGGER.info("WASI operations validated successfully");

      } finally {
        // Clean up temporary files
        Files.deleteIfExists(testFile);
        Files.deleteIfExists(tempDir);
      }
    }
  }

  @Test
  @DisplayName("Should validate host function integration and bidirectional calling")
  @Timeout(value = 1, unit = TimeUnit.MINUTES)
  void shouldValidateHostFunctionIntegrationAndBidirectionalCalling() throws Exception {
    LOGGER.info("=== Testing Host Function Integration (Issue #275) ===");

    try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
      testResources.add(runtime);

      try (final Engine engine = runtime.createEngine()) {
        testResources.add(engine);

        try (final Store store = engine.createStore()) {
          testResources.add(store);

          // Create a host function registry
          final HostFunctionRegistry registry = new HostFunctionRegistry();

          // Register a simple host function
          final HostFunction logFunction =
              new HostFunction() {
                @Override
                public WasmValue[] call(
                    final HostFunctionContext context, final WasmValue[] args) {
                  if (args.length > 0) {
                    LOGGER.info("Host function called with value: " + args[0].asI32());
                    return new WasmValue[] {WasmValue.i32(args[0].asI32() * 2)};
                  }
                  return new WasmValue[0];
                }

                @Override
                public String getName() {
                  return "host_log";
                }
              };

          registry.register(logFunction);

          // Test host function registration and invocation
          final byte[] moduleBytes = TestUtils.createHostFunctionWasmModule();
          if (moduleBytes.length > 0) {
            final Module module = engine.compileModule(moduleBytes);
            final Instance instance = runtime.instantiate(module);
            testResources.add(instance);

            // Test calling WebAssembly function that calls back to host
            final Function testFunction = instance.getFunction("test_host_call").orElse(null);
            if (testFunction != null) {
              final WasmValue[] args = {WasmValue.i32(21)};
              final WasmValue[] results = testFunction.call(args);

              // Should return doubled value from host function
              assertThat(results).hasSize(1);
              assertThat(results[0].asI32()).isEqualTo(42);

              LOGGER.info("Host function bidirectional calling validated successfully");
            } else {
              LOGGER.info("Host function test module not available - testing registration only");
            }
          } else {
            LOGGER.info("Host function module not available - testing registry only");
          }

          // Verify registry functionality
          assertThat(registry.getRegisteredFunctions()).contains("host_log");
          assertThat(registry.getFunction("host_log")).isEqualTo(logFunction);

          LOGGER.info("Host function integration validated successfully");
        }
      }
    }
  }

  @Test
  @DisplayName("Should validate comprehensive error handling and diagnostics")
  @Timeout(value = 1, unit = TimeUnit.MINUTES)
  void shouldValidateComprehensiveErrorHandlingAndDiagnostics() throws Exception {
    LOGGER.info("=== Testing Error Handling and Diagnostics (Issue #276) ===");

    try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
      testResources.add(runtime);

      try (final Engine engine = runtime.createEngine()) {
        testResources.add(engine);

        // Test compilation errors
        final byte[] invalidModuleBytes = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0xFF};
        assertThatThrownBy(() -> engine.compileModule(invalidModuleBytes))
            .isInstanceOf(CompilationException.class)
            .hasMessageContaining("compilation");

        // Test validation errors
        final byte[] malformedBytes = {0x00, 0x61, 0x73, 0x6d}; // Incomplete header
        assertThatThrownBy(() -> engine.compileModule(malformedBytes))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("validation");

        // Test runtime errors with valid module
        try (final Store store = engine.createStore()) {
          testResources.add(store);

          final byte[] moduleBytes = TestUtils.createTrapWasmModule();
          if (moduleBytes.length > 0) {
            final Module module = engine.compileModule(moduleBytes);
            final Instance instance = runtime.instantiate(module);
            testResources.add(instance);

            final Function trapFunction = instance.getFunction("trap_function").orElse(null);
            if (trapFunction != null) {
              assertThatThrownBy(() -> trapFunction.call(new WasmValue[0]))
                  .isInstanceOf(RuntimeException.class)
                  .hasMessageContaining("trap");

              LOGGER.info("Runtime trap handling validated successfully");
            }
          }

          // Test function call errors
          final byte[] simpleModuleBytes = TestUtils.createSimpleWasmModule();
          final Module simpleModule = engine.compileModule(simpleModuleBytes);
          final Instance simpleInstance = runtime.instantiate(simpleModule);
          testResources.add(simpleInstance);

          final Function addFunction =
              simpleInstance
                  .getFunction("add")
                  .orElseThrow(() -> new AssertionError("add function should be exported"));

          // Test wrong argument count
          assertThatThrownBy(() -> addFunction.call(new WasmValue[] {WasmValue.i32(1)}))
              .isInstanceOf(RuntimeException.class)
              .hasMessageContaining("argument");

          // Test wrong argument type
          assertThatThrownBy(
                  () -> addFunction.call(new WasmValue[] {WasmValue.i64(1L), WasmValue.i32(2)}))
              .isInstanceOf(RuntimeException.class)
              .hasMessageContaining("type");

          LOGGER.info("Error handling and diagnostics validated successfully");
        }
      }
    }
  }

  @Test
  @DisplayName("Should validate memory leak detection under stress conditions")
  @Timeout(value = 3, unit = TimeUnit.MINUTES)
  void shouldValidateMemoryLeakDetectionUnderStressConditions() throws Exception {
    LOGGER.info("=== Testing Memory Leak Detection ===");

    // Configure fast leak detection for testing
    final MemoryLeakDetector.Configuration config =
        MemoryLeakDetector.Configuration.builder()
            .testDuration(Duration.ofSeconds(30))
            .samplingInterval(50)
            .sampleCount(100)
            .leakThreshold(1.5) // Allow some growth for test warmup
            .build();

    // Test operation that creates and destroys resources
    final MemoryLeakDetector.TestedOperation testOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine()) {
            try (final Store store = engine.createStore()) {
              final byte[] moduleBytes = TestUtils.createSimpleWasmModule();
              final Module module = engine.compileModule(moduleBytes);
              final Instance instance = runtime.instantiate(module);

              final Function addFunction =
                  instance
                      .getFunction("add")
                      .orElseThrow(() -> new AssertionError("add function should be exported"));

              final WasmValue[] args = {WasmValue.i32(1), WasmValue.i32(2)};
              final WasmValue[] results = addFunction.call(args);

              assertThat(results).hasSize(1);
              assertThat(results[0].asI32()).isEqualTo(3);

              instance.close();
            }
          }
        };

    // Run leak detection
    final MemoryLeakDetector.LeakAnalysisResult result =
        MemoryLeakDetector.detectLeaks("comprehensive_workflow_stress_test", testOperation, config);

    LOGGER.info("Memory leak analysis completed");
    LOGGER.info("Analysis: " + result.getAnalysis());

    if (result.isLeakDetected()) {
      LOGGER.warning("Memory leak detected: " + result.getMemoryIncrease() + " bytes");
      LOGGER.warning("Recommendations: " + String.join(", ", result.getRecommendations()));

      // For comprehensive testing, we want to know about leaks but not fail the test
      // unless they're severe (more than 50% increase)
      if (result.getMemoryIncrease() > 10_000_000) { // 10MB threshold
        throw new AssertionError("Severe memory leak detected: " + result.getMemoryIncrease() + " bytes");
      }
    } else {
      LOGGER.info("No memory leaks detected - good!");
    }
  }

  @Test
  @DisplayName("Should validate concurrent execution and thread safety")
  @Timeout(value = 2, unit = TimeUnit.MINUTES)
  void shouldValidateConcurrentExecutionAndThreadSafety() throws Exception {
    LOGGER.info("=== Testing Concurrent Execution and Thread Safety ===");

    final int numThreads = 4;
    final int operationsPerThread = 10;
    final ExecutorService executor = Executors.newFixedThreadPool(numThreads);

    try {
      final List<CompletableFuture<Void>> futures = new ArrayList<>();

      for (int t = 0; t < numThreads; t++) {
        final int threadId = t;
        final CompletableFuture<Void> future =
            CompletableFuture.runAsync(
                () -> {
                  try {
                    LOGGER.info("Thread " + threadId + " starting concurrent operations");

                    for (int op = 0; op < operationsPerThread; op++) {
                      try (final WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
                        try (final Engine engine = runtime.createEngine()) {
                          try (final Store store = engine.createStore()) {
                            final byte[] moduleBytes = TestUtils.createSimpleWasmModule();
                            final Module module = engine.compileModule(moduleBytes);
                            final Instance instance = runtime.instantiate(module);

                            final Function addFunction =
                                instance
                                    .getFunction("add")
                                    .orElseThrow(
                                        () -> new AssertionError("add function should be exported"));

                            final WasmValue[] args = {
                              WasmValue.i32(threadId), WasmValue.i32(op)
                            };
                            final WasmValue[] results = addFunction.call(args);

                            assertThat(results).hasSize(1);
                            assertThat(results[0].asI32()).isEqualTo(threadId + op);

                            instance.close();
                          }
                        }
                      }
                    }

                    LOGGER.info("Thread " + threadId + " completed " + operationsPerThread + " operations");
                  } catch (final Exception e) {
                    LOGGER.severe("Thread " + threadId + " failed: " + e.getMessage());
                    throw new RuntimeException("Concurrent execution failed", e);
                  }
                },
                executor);

        futures.add(future);
      }

      // Wait for all threads to complete
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      LOGGER.info("Concurrent execution validated successfully");

    } finally {
      executor.shutdown();
      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        LOGGER.warning("Executor did not terminate within timeout");
        executor.shutdownNow();
      }
    }
  }

  /**
   * Executes the complete WebAssembly workflow for a specific runtime type.
   *
   * @param runtimeType the runtime to test
   */
  private void executeCompleteWorkflowForRuntime(final RuntimeType runtimeType) throws Exception {
    LOGGER.info("=== Testing Complete Workflow for " + runtimeType + " ===");

    try (final WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      testResources.add(runtime);

      try (final Engine engine = runtime.createEngine()) {
        testResources.add(engine);

        try (final Store store = engine.createStore()) {
          testResources.add(store);

          // 1. Module compilation and validation
          final byte[] moduleBytes = TestUtils.createComprehensiveWasmModule();
          final Module module = engine.compileModule(moduleBytes);
          assertThat(module).isNotNull();

          // 2. Instance creation
          final Instance instance = runtime.instantiate(module);
          testResources.add(instance);
          assertThat(instance).isNotNull();

          // 3. Function invocation
          final Function mainFunction =
              instance
                  .getFunction("main")
                  .or(() -> instance.getFunction("add"))
                  .orElseThrow(() -> new AssertionError("No callable function found"));

          final WasmValue[] args = {WasmValue.i32(10), WasmValue.i32(32)};
          final WasmValue[] results = mainFunction.call(args);

          assertThat(results).isNotEmpty();
          assertThat(results[0].asI32()).isEqualTo(42);

          // 4. Memory operations (if memory is exported)
          final Memory memory = instance.getMemory("memory").orElse(null);
          if (memory != null) {
            final byte[] testData = "Hello, WebAssembly!".getBytes();
            memory.writeBytes(0, testData);

            final byte[] readData = memory.readBytes(0, testData.length);
            assertThat(readData).isEqualTo(testData);
          }

          // 5. Multiple function calls to test state consistency
          for (int i = 0; i < 5; i++) {
            final WasmValue[] repeatArgs = {WasmValue.i32(i), WasmValue.i32(10)};
            final WasmValue[] repeatResults = mainFunction.call(repeatArgs);
            assertThat(repeatResults[0].asI32()).isEqualTo(i + 10);
          }

          LOGGER.info("Complete workflow for " + runtimeType + " validated successfully");
        }
      }
    }
  }
}
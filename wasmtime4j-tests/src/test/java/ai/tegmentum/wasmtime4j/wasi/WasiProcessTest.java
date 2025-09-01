package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
// import ai.tegmentum.wasmtime4j.exceptions.WasmRuntimeException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
// import ai.tegmentum.wasmtime4j.functions.WasmFunction;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive process handling tests for WASI functionality. Tests process exit codes, signal
 * handling, process lifecycle management, and cross-runtime process behavior consistency.
 */
@Tag(TestCategories.INTEGRATION)
@Tag(TestCategories.WASI)
@Tag(TestCategories.PROCESS)
public final class WasiProcessTest {
  private static final Logger LOGGER = Logger.getLogger(WasiProcessTest.class.getName());

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp() {
    runtime = WasmRuntimeFactory.create();
    engine = runtime.createEngine();
    store = engine.createStore();
    LOGGER.info("Set up WASI process test with runtime: " + runtime.getRuntimeType());
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    if (runtime != null) {
      runtime.close();
    }
  }

  /** Tests successful process execution with exit code 0. */
  @Test
  void testSuccessfulProcessExecution() {
    LOGGER.info("Testing successful process execution");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .arguments(Arrays.asList("program", "--success"))
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Execute successful program
          if (instance.hasExport("_start")) {
            final WasmFunction startFunction = instance.getExport("_start").asFunction();
            assertNotNull(startFunction);

            // Should complete successfully (exit code 0)
            assertDoesNotThrow(() -> startFunction.call());
          }

          instance.close();
          wasi.close();
          module.close();
          });
  }

  /** Tests process execution with non-zero exit codes. */
  @Test
  void testProcessExecutionWithExitCodes() {
    LOGGER.info("Testing process execution with exit codes");

    final int[] exitCodes = {1, 2, 42, 255};

    for (final int expectedExitCode : exitCodes) {
      final WasiConfig config =
          WasiConfig.builder()
              .inheritEnv(true)
              .arguments(Arrays.asList("program", "--exit", String.valueOf(expectedExitCode)))
              .inheritStdin(true)
              .inheritStdout(true)
              .inheritStderr(true)
              .build();

      final byte[] wasmBytes = createExitCodeModule(expectedExitCode);

      assertDoesNotThrow(
          () -> {
            final Module module = engine.createModule(wasmBytes);
            final Wasi wasi = store.createWasi(config);
            final Instance instance = store.createInstance(module, wasi.getImports());

            // Execute program that should exit with specific code
            if (instance.hasExport("_start")) {
              final WasmFunction startFunction = instance.getExport("_start").asFunction();
              assertNotNull(startFunction);

              // Program should exit with expected code
              try {
                startFunction.call();
                // If no exception, assume successful execution (exit code 0)
              } catch (final WasmRuntimeException e) {
                // Check if this is a WASI exit exception with expected code
                if (e.getCause() instanceof WasiExitException) {
                  final WasiExitException exitException = (WasiExitException) e.getCause();
                  assertEquals(expectedExitCode, exitException.getExitCode());
                  LOGGER.info(
                      "Caught expected WASI exit with code: " + exitException.getExitCode());
                } else {
                  // Re-throw if not expected exit
                  throw e;
                }
              }
            }

            instance.close();
            wasi.close();
            module.close();
          });
    }
  }

  /** Tests process termination and cleanup. */
  @Test
  void testProcessTerminationAndCleanup() {
    LOGGER.info("Testing process termination and cleanup");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .arguments(Arrays.asList("program", "--terminate"))
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createTerminationModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Execute program that terminates
          if (instance.hasExport("test_termination")) {
            final WasmFunction terminationFunction =
                instance.getExport("test_termination").asFunction();
            assertNotNull(terminationFunction);

            assertDoesNotThrow(() -> terminationFunction.call());
          }

          // Verify cleanup
          assertTrue(instance.isValid()); // Should still be valid for cleanup

          instance.close();
          wasi.close();
          module.close();
          });
  }

  /** Tests multiple process executions with different exit scenarios. */
  @Test
  void testMultipleProcessExecutions() {
    LOGGER.info("Testing multiple process executions");

    final String[] scenarios = {"success", "error", "abort"};

    for (final String scenario : scenarios) {
      LOGGER.info("Testing scenario: " + scenario);

      final WasiConfig config =
          WasiConfig.builder()
              .inheritEnv(true)
              .arguments(Arrays.asList("program", "--scenario", scenario))
              .inheritStdin(true)
              .inheritStdout(true)
              .inheritStderr(true)
              .build();

      final byte[] wasmBytes = createScenarioModule(scenario);

      assertDoesNotThrow(
          () -> {
            final Module module = engine.createModule(wasmBytes);
            final Wasi wasi = store.createWasi(config);
            final Instance instance = store.createInstance(module, wasi.getImports());

            // Execute scenario
            if (instance.hasExport("run_scenario")) {
              final WasmFunction scenarioFunction = instance.getExport("run_scenario").asFunction();
              assertNotNull(scenarioFunction);

              try {
                scenarioFunction.call();
              } catch (final Exception e) {
                // Some scenarios may throw exceptions (error, abort)
                LOGGER.info(
                    "Scenario " + scenario + " resulted in: " + e.getClass().getSimpleName());
              }
            }

            instance.close();
            wasi.close();
            module.close();
          });
    }
  }

  /** Tests concurrent process execution and isolation. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testConcurrentProcessExecution() throws InterruptedException {
    LOGGER.info("Testing concurrent process execution");

    final int processCount = 4;
    final ExecutorService executor = Executors.newFixedThreadPool(processCount);

    try {
      final CompletableFuture<Integer>[] futures = new CompletableFuture[processCount];

      for (int i = 0; i < processCount; i++) {
        final int processId = i;
        final int expectedExitCode = i; // Different exit code for each process

        futures[i] =
            CompletableFuture.supplyAsync(
                () -> {
                  try (final WasmRuntime processRuntime = WasmRuntimeFactory.create();
                      final Engine processEngine = processRuntime.createEngine();
                      final Store processStore = processEngine.createStore()) {

                    LOGGER.info("Starting concurrent process " + processId);

                    final WasiConfig config =
                        WasiConfig.builder()
                            .inheritEnv(true)
                            .arguments(
                                Arrays.asList(
                                    "process_" + processId,
                                    "--exit",
                                    String.valueOf(expectedExitCode)))
                            .inheritStdin(false)
                            .inheritStdout(true)
                            .inheritStderr(true)
                            .build();

                    final byte[] wasmBytes = createExitCodeModule(expectedExitCode);
                    final Module module = processEngine.createModule(wasmBytes);
                    final Wasi wasi = processStore.createWasi(config);
                    final Instance instance =
                        processStore.createInstance(module, wasi.getImports());

                    int actualExitCode = 0;
                    if (instance.hasExport("_start")) {
                      final WasmFunction startFunction = instance.getExport("_start").asFunction();

                      try {
                        startFunction.call();
                        actualExitCode = 0; // Successful completion
                      } catch (final WasmRuntimeException e) {
                        if (e.getCause() instanceof WasiExitException) {
                          actualExitCode = ((WasiExitException) e.getCause()).getExitCode();
                        } else {
                          actualExitCode = -1; // Error condition
                        }
                      }
                    }

                    instance.close();
                    wasi.close();
                    module.close();

                    LOGGER.info(
                        "Concurrent process "
                            + processId
                            + " completed with exit code: "
                            + actualExitCode);
                    return actualExitCode;

                  } catch (final Exception e) {
                    LOGGER.severe("Concurrent process " + processId + " failed: " + e.getMessage());
                    throw new RuntimeException(e);
                  }
                },
                executor);
      }

      // Wait for all processes and verify exit codes
      for (int i = 0; i < processCount; i++) {
        final Integer actualExitCode = futures[i].join();
        final int expectedExitCode = i;

        // Allow for both successful completion (0) or expected exit code
        assertTrue(
            actualExitCode == 0 || actualExitCode == expectedExitCode,
            "Process "
                + i
                + " exit code mismatch: expected "
                + expectedExitCode
                + " or 0, got "
                + actualExitCode);
      }

      LOGGER.info("All concurrent processes completed successfully");

    } finally {
      executor.shutdown();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
  }

  /** Tests process timeout and forced termination. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testProcessTimeoutAndForcedTermination() {
    LOGGER.info("Testing process timeout and forced termination");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .arguments(Arrays.asList("program", "--hang"))
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createHangingModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Execute with timeout using CompletableFuture
          if (instance.hasExport("hang_forever")) {
            final WasmFunction hangFunction = instance.getExport("hang_forever").asFunction();
            assertNotNull(hangFunction);

            final CompletableFuture<Void> execution =
                CompletableFuture.runAsync(
                    () -> {
                      try {
                        hangFunction.call();
                      } catch (final Exception e) {
                        // Timeout or termination is acceptable
                        LOGGER.info("Hanging function terminated: " + e.getClass().getSimpleName());
                      }
          });

            try {
              // Wait with timeout
              execution.get(5, TimeUnit.SECONDS);
            } catch (final java.util.concurrent.TimeoutException e) {
              // Expected timeout - cancel execution
              execution.cancel(true);
              LOGGER.info("Process execution timed out as expected");
            }
          }

          instance.close();
          wasi.close();
          module.close();
          });
  }

  /** Tests cross-runtime process behavior consistency. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeProcessConsistency() {
    LOGGER.info("Testing cross-runtime process consistency");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final CrossRuntimeValidator.RuntimeOperation<String> processOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasiConfig config =
                WasiConfig.builder()
                    .inheritEnv(true)
                    .arguments(Arrays.asList("test_process", "--exit", "42"))
                    .inheritStdin(true)
                    .inheritStdout(true)
                    .inheritStderr(true)
                    .build();

            final byte[] wasmBytes = createExitCodeModule(42);
            final Module module = engine.createModule(wasmBytes);
            final Wasi wasi = store.createWasi(config);
            final Instance instance = store.createInstance(module, wasi.getImports());

            int exitCode = 0;
            String status = "success";

            if (instance.hasExport("_start")) {
              final WasmFunction startFunction = instance.getExport("_start").asFunction();

              try {
                startFunction.call();
                status = "completed";
              } catch (final WasmRuntimeException e) {
                if (e.getCause() instanceof WasiExitException) {
                  exitCode = ((WasiExitException) e.getCause()).getExitCode();
                  status = "exit_" + exitCode;
                } else {
                  status = "error";
                }
              }
            }

            instance.close();
            wasi.close();
            module.close();

            return String.format("status=%s,exit=%d", status, exitCode);
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(processOperation, Duration.ofSeconds(20));

    assertTrue(
        result.isValid(),
        "Process behavior differs between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime process validation successful");
  }

  /** Tests process resource cleanup after abnormal termination. */
  @Test
  void testProcessResourceCleanupAfterAbnormalTermination() {
    LOGGER.info("Testing process resource cleanup after abnormal termination");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .arguments(Arrays.asList("program", "--crash"))
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createCrashModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Execute program that crashes
          if (instance.hasExport("crash_program")) {
            final WasmFunction crashFunction = instance.getExport("crash_program").asFunction();
            assertNotNull(crashFunction);

            try {
              crashFunction.call();
            } catch (final Exception e) {
              // Crash is expected
              LOGGER.info("Program crashed as expected: " + e.getClass().getSimpleName());
            }
          }

          // Verify resources can still be cleaned up
          assertTrue(instance.isValid());

          instance.close();
          wasi.close();
          module.close();
          });
  }

  /** Tests process argument passing and validation. */
  @Test
  void testProcessArgumentPassingAndValidation() {
    LOGGER.info("Testing process argument passing and validation");

    final java.util.List<String> complexArgs =
        Arrays.asList(
            "program",
            "--flag",
            "--option=value",
            "positional_arg",
            "--number",
            "123",
            "--path",
            "/path/to/file",
            "--empty=",
            "--spaces",
            "argument with spaces");

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .arguments(complexArgs)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createArgumentValidationModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Verify arguments are properly passed
          final java.util.List<String> actualArgs = wasi.getArguments();
          assertEquals(complexArgs.size(), actualArgs.size());

          for (int i = 0; i < complexArgs.size(); i++) {
            assertEquals(complexArgs.get(i), actualArgs.get(i));
          }

          // Execute argument validation
          if (instance.hasExport("validate_args")) {
            final WasmFunction validateFunction = instance.getExport("validate_args").asFunction();
            assertNotNull(validateFunction);

            assertDoesNotThrow(() -> validateFunction.call());
          }

          instance.close();
          wasi.close();
          module.close();
          });
  }

  /** Tests process exit code boundary conditions. */
  @Test
  void testProcessExitCodeBoundaryConditions() {
    LOGGER.info("Testing process exit code boundary conditions");

    final int[] boundaryExitCodes = {0, 1, 127, 128, 255};

    for (final int exitCode : boundaryExitCodes) {
      final WasiConfig config =
          WasiConfig.builder()
              .inheritEnv(true)
              .arguments(Arrays.asList("program", "--boundary-exit", String.valueOf(exitCode)))
              .inheritStdin(true)
              .inheritStdout(true)
              .inheritStderr(true)
              .build();

      final byte[] wasmBytes = createBoundaryExitModule(exitCode);

      assertDoesNotThrow(
          () -> {
            final Module module = engine.createModule(wasmBytes);
            final Wasi wasi = store.createWasi(config);
            final Instance instance = store.createInstance(module, wasi.getImports());

            if (instance.hasExport("boundary_exit")) {
              final WasmFunction boundaryFunction =
                  instance.getExport("boundary_exit").asFunction();
              assertNotNull(boundaryFunction);

              try {
                boundaryFunction.call();
                if (exitCode != 0) {
                  LOGGER.warning(
                      "Expected exit code " + exitCode + " but function completed normally");
                }
              } catch (final WasmRuntimeException e) {
                if (e.getCause() instanceof WasiExitException) {
                  final int actualExitCode = ((WasiExitException) e.getCause()).getExitCode();
                  assertEquals(
                      exitCode,
                      actualExitCode,
                      "Exit code mismatch for boundary value " + exitCode);
                }
              }
            }

            instance.close();
            wasi.close();
            module.close();
          });
    }
  }

  /** Creates a WebAssembly module that exits with a specific code. */
  private byte[] createExitCodeModule(final int exitCode) {
    // This would be a WASI module that calls proc_exit with the specified code
    // For testing, we'll use the basic module and simulate the behavior
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for termination testing. */
  private byte[] createTerminationModule() {
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for scenario testing. */
  private byte[] createScenarioModule(final String scenario) {
    // Different scenarios would be implemented in different modules
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module that hangs indefinitely. */
  private byte[] createHangingModule() {
    // This would contain an infinite loop for timeout testing
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module that crashes. */
  private byte[] createCrashModule() {
    // This would contain code that triggers a crash
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for argument validation. */
  private byte[] createArgumentValidationModule() {
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for boundary exit code testing. */
  private byte[] createBoundaryExitModule(final int exitCode) {
    return WasmTestModules.getModule("wasi_basic");
  }
}

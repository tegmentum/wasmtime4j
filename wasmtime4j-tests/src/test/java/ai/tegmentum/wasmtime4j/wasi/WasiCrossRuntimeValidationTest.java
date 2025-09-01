package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.functions.WasmFunction;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive cross-runtime validation tests for WASI functionality. Ensures identical behavior
 * between JNI and Panama implementations across all WASI operations, configurations, and edge
 * cases.
 */
@Tag(TestCategories.INTEGRATION)
@Tag(TestCategories.WASI)
@Tag(TestCategories.CROSS_RUNTIME)
public final class WasiCrossRuntimeValidationTest {
  private static final Logger LOGGER =
      Logger.getLogger(WasiCrossRuntimeValidationTest.class.getName());

  @TempDir private Path tempDirectory;

  /** Tests cross-runtime WASI context creation consistency. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeWasiContextCreation() {
    LOGGER.info("Testing cross-runtime WASI context creation");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final CrossRuntimeValidator.RuntimeOperation<String> contextOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasiConfig config =
                WasiConfig.builder()
                    .inheritEnv(true)
                    .arguments(Arrays.asList("test", "program"))
                    .inheritStdin(true)
                    .inheritStdout(true)
                    .inheritStderr(true)
                    .build();

            final Wasi wasi = store.createWasi(config);
            final boolean isValid = wasi.isValid();
            final int argCount = wasi.getArguments().size();
            final int envCount = wasi.getEnvironment().size();

            wasi.close();

            return String.format("valid=%s,args=%d,env=%d", isValid, argCount, envCount);
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(contextOperation, Duration.ofSeconds(15));

    assertTrue(
        result.isValid(),
        "WASI context creation differs between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime WASI context creation validation successful");
  }

  /** Tests cross-runtime WASI environment variable handling consistency. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeEnvironmentHandling() {
    LOGGER.info("Testing cross-runtime environment handling");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final Map<String, String> testEnv = new HashMap<>();
    testEnv.put("CROSS_TEST", "cross_value");
    testEnv.put("UNICODE_TEST", "测试中文");
    testEnv.put("SPECIAL_CHARS", "!@#$%^&*()");

    final CrossRuntimeValidator.RuntimeOperation<String> envOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasiConfig config =
                WasiConfig.builder()
                    .inheritEnv(false)
                    .environment(testEnv)
                    .inheritStdin(true)
                    .inheritStdout(true)
                    .inheritStderr(true)
                    .build();

            final Wasi wasi = store.createWasi(config);
            final Map<String, String> actualEnv = wasi.getEnvironment();

            final String result =
                String.format(
                    "size=%d,cross=%s,unicode=%s,special=%s",
                    actualEnv.size(),
                    actualEnv.get("CROSS_TEST"),
                    actualEnv.get("UNICODE_TEST"),
                    actualEnv.get("SPECIAL_CHARS"));

            wasi.close();
            return result;
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(envOperation, Duration.ofSeconds(15));

    assertTrue(
        result.isValid(),
        "Environment handling differs between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime environment handling validation successful");
  }

  /** Tests cross-runtime WASI filesystem access consistency. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeFilesystemAccess() throws IOException {
    LOGGER.info("Testing cross-runtime filesystem access");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final Path testDir = tempDirectory.resolve("cross_fs");
    Files.createDirectories(testDir);

    final Path testFile = testDir.resolve("test.txt");
    Files.write(testFile, "Cross-runtime test file".getBytes());

    final CrossRuntimeValidator.RuntimeOperation<String> filesystemOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasiConfig config =
                WasiConfig.builder()
                    .inheritEnv(true)
                    .preopenDir(testDir.toString(), "testdir", true, false)
                    .inheritStdin(true)
                    .inheritStdout(true)
                    .inheritStderr(true)
                    .build();

            final Wasi wasi = store.createWasi(config);
            final Map<String, String> preopenedDirs = wasi.getPreopenedDirectories();

            final boolean hasTestDir = preopenedDirs.containsKey("testdir");
            final int dirCount = preopenedDirs.size();

            wasi.close();
            return String.format("hasDir=%s,count=%d", hasTestDir, dirCount);
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(filesystemOperation, Duration.ofSeconds(15));

    assertTrue(
        result.isValid(),
        "Filesystem access differs between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime filesystem access validation successful");
  }

  /** Tests cross-runtime WASI I/O redirection consistency. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeIORedirection() {
    LOGGER.info("Testing cross-runtime I/O redirection");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final String testInput = "Cross-runtime I/O test\nLine 2\nLine 3\n";

    final CrossRuntimeValidator.RuntimeOperation<String> ioOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final ByteArrayInputStream stdin = new ByteArrayInputStream(testInput.getBytes());
            final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            final WasiConfig config =
                WasiConfig.builder()
                    .inheritEnv(true)
                    .stdin(stdin)
                    .stdout(stdout)
                    .stderr(stderr)
                    .build();

            final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");
            final Module module = engine.createModule(wasmBytes);
            final Wasi wasi = store.createWasi(config);
            final Instance instance = store.createInstance(module, wasi.getImports());

            if (instance.hasExport("_start")) {
              final WasmFunction startFunction = instance.getExport("_start").asFunction();
              assertDoesNotThrow(() -> startFunction.call());
            }

            final int stdoutSize = stdout.toByteArray().length;
            final int stderrSize = stderr.toByteArray().length;

            instance.close();
            wasi.close();
            module.close();

            return String.format(
                "input=%d,stdout=%d,stderr=%d", testInput.length(), stdoutSize, stderrSize);
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(ioOperation, Duration.ofSeconds(15));

    assertTrue(
        result.isValid(),
        "I/O redirection differs between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime I/O redirection validation successful");
  }

  /** Tests cross-runtime WASI module instantiation consistency. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeModuleInstantiation() {
    LOGGER.info("Testing cross-runtime module instantiation");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final CrossRuntimeValidator.RuntimeOperation<String> instantiationOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasiConfig config =
                WasiConfig.builder()
                    .inheritEnv(true)
                    .arguments(Arrays.asList("test_module"))
                    .inheritStdin(true)
                    .inheritStdout(true)
                    .inheritStderr(true)
                    .build();

            final byte[] wasmBytes = WasmTestModules.getModule("wasi_basic");
            final Module module = engine.createModule(wasmBytes);
            final Wasi wasi = store.createWasi(config);

            final var imports = wasi.getImports();
            final int importCount = imports.size();

            final Instance instance = store.createInstance(module, imports);
            final boolean instanceValid = instance.isValid();
            final boolean hasStartExport = instance.hasExport("_start");

            instance.close();
            wasi.close();
            module.close();

            return String.format(
                "imports=%d,valid=%s,start=%s", importCount, instanceValid, hasStartExport);
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(instantiationOperation, Duration.ofSeconds(15));

    assertTrue(
        result.isValid(),
        "Module instantiation differs between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime module instantiation validation successful");
  }

  /** Tests cross-runtime WASI error handling consistency. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeErrorHandling() {
    LOGGER.info("Testing cross-runtime error handling");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    // Test with invalid configuration that should cause errors
    final CrossRuntimeValidator.RuntimeOperation<String> errorOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            try {
              // Attempt to create WASI with invalid directory
              final WasiConfig config =
                  WasiConfig.builder()
                      .inheritEnv(true)
                      .preopenDir("/nonexistent/invalid/path", "invalid", true, false)
                      .build();

              final Wasi wasi = store.createWasi(config);
              wasi.close();
              return "success"; // Should not reach here
            } catch (final Exception e) {
              return "error_" + e.getClass().getSimpleName();
            }
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateErrorHandling(errorOperation, Duration.ofSeconds(15));

    // Both should fail with similar errors
    final List<CrossRuntimeValidator.TestResult> results = result.getResults();

    // Verify both runtimes handled the error (either threw or returned error indicator)
    boolean bothHandledError = true;
    for (final CrossRuntimeValidator.TestResult testResult : results) {
      final boolean handledError =
          testResult.hasException()
              || (testResult.getResult() != null
                  && testResult.getResult().toString().startsWith("error_"));
      if (!handledError) {
        bothHandledError = false;
        break;
      }
    }

    assertTrue(bothHandledError, "Error handling should be consistent across runtimes");
    LOGGER.info("Cross-runtime error handling validation successful");
  }

  /** Tests cross-runtime WASI performance consistency. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testCrossRuntimePerformanceConsistency() {
    LOGGER.info("Testing cross-runtime performance consistency");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final CrossRuntimeValidator.RuntimeOperation<Long> performanceOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final long startTime = System.nanoTime();

            // Perform multiple WASI operations for measurement
            for (int i = 0; i < 10; i++) {
              final WasiConfig config =
                  WasiConfig.builder()
                      .inheritEnv(true)
                      .arguments(Arrays.asList("perf_test_" + i))
                      .inheritStdin(true)
                      .inheritStdout(true)
                      .inheritStderr(true)
                      .build();

              final Wasi wasi = store.createWasi(config);
              assertTrue(wasi.isValid());
              wasi.close();
            }

            final long elapsed = System.nanoTime() - startTime;
            return elapsed / 1_000_000; // Return milliseconds
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(performanceOperation, Duration.ofSeconds(30));

    assertNotNull(result);

    // Performance may differ between runtimes, but both should complete successfully
    final List<CrossRuntimeValidator.TestResult> results = result.getResults();
    for (final CrossRuntimeValidator.TestResult testResult : results) {
      assertTrue(
          testResult.isSuccess(),
          "Performance test should succeed in " + testResult.getRuntimeType());

      final Long duration = (Long) testResult.getResult();
      assertNotNull(duration);
      assertTrue(duration > 0, "Performance measurement should be positive");

      LOGGER.info(
          String.format("%s runtime performance: %d ms", testResult.getRuntimeType(), duration));
    }

    LOGGER.info("Cross-runtime performance consistency validation successful");
  }

  /** Tests cross-runtime WASI resource management consistency. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeResourceManagement() {
    LOGGER.info("Testing cross-runtime resource management");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final CrossRuntimeValidator.RuntimeOperation<String> resourceOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            // Create multiple WASI contexts to test resource management
            final WasiConfig config =
                WasiConfig.builder()
                    .inheritEnv(true)
                    .inheritStdin(true)
                    .inheritStdout(true)
                    .inheritStderr(true)
                    .build();

            final Wasi[] wasContexts = new Wasi[5];

            // Create contexts
            for (int i = 0; i < wasContexts.length; i++) {
              wasContexts[i] = store.createWasi(config);
              assertTrue(wasContexts[i].isValid());
            }

            // Close contexts
            for (final Wasi wasi : wasContexts) {
              wasi.close();
            }

            return "managed_" + wasContexts.length + "_contexts";
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(resourceOperation, Duration.ofSeconds(20));

    assertTrue(
        result.isValid(),
        "Resource management differs between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime resource management validation successful");
  }

  /** Tests cross-runtime WASI concurrency behavior consistency. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testCrossRuntimeConcurrencyBehavior() {
    LOGGER.info("Testing cross-runtime concurrency behavior");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final CrossRuntimeValidator.RuntimeOperation<Integer> concurrencyOperation =
        runtime -> {
          final int concurrentCount = 3;
          final CrossRuntimeValidator.ComparisonResult concurrentResult =
              CrossRuntimeValidator.validateConcurrentExecution(
                  wasmRuntime -> {
                    try (final Engine engine = wasmRuntime.createEngine();
                        final Store store = engine.createStore()) {

                      final WasiConfig config =
                          WasiConfig.builder()
                              .inheritEnv(true)
                              .inheritStdin(false)
                              .inheritStdout(true)
                              .inheritStderr(true)
                              .build();

                      final Wasi wasi = store.createWasi(config);
                      assertTrue(wasi.isValid());
                      wasi.close();

                      return "concurrent_success";
                    }
                  },
                  concurrentCount,
                  2);

          return concurrentResult.isValid() ? concurrentCount : 0;
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(concurrencyOperation, Duration.ofSeconds(45));

    assertTrue(
        result.isValid(),
        "Concurrency behavior differs between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime concurrency behavior validation successful");
  }

  /** Tests cross-runtime WASI configuration edge cases consistency. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeConfigurationEdgeCases() {
    LOGGER.info("Testing cross-runtime configuration edge cases");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final CrossRuntimeValidator.RuntimeOperation<String> edgeCaseOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            // Test edge case: empty environment and arguments
            final WasiConfig emptyConfig =
                WasiConfig.builder()
                    .inheritEnv(false)
                    .environment(new HashMap<>())
                    .arguments(Arrays.asList())
                    .inheritStdin(true)
                    .inheritStdout(true)
                    .inheritStderr(true)
                    .build();

            final Wasi wasi = store.createWasi(emptyConfig);
            final int envSize = wasi.getEnvironment().size();
            final int argSize = wasi.getArguments().size();

            wasi.close();

            return String.format("env=%d,args=%d", envSize, argSize);
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(edgeCaseOperation, Duration.ofSeconds(15));

    assertTrue(
        result.isValid(),
        "Configuration edge cases differ between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime configuration edge cases validation successful");
  }
}

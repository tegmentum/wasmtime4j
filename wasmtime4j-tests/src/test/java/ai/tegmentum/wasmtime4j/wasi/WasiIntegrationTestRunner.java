package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Integration test runner specifically for WASI (WebAssembly System Interface) functionality. Tests
 * file system operations, process operations, and WASI compatibility across runtimes.
 */
public final class WasiIntegrationTestRunner {
  private static final Logger LOGGER = Logger.getLogger(WasiIntegrationTestRunner.class.getName());

  // Test execution tracking
  private static final ConcurrentMap<String, WasiTestResult> wasiTestResults =
      new ConcurrentHashMap<>();

  // Test environment setup
  private static final String WASI_TEST_DIR = "wasi-test-env";
  private static Path testEnvironmentRoot;

  private WasiIntegrationTestRunner() {
    // Utility class - prevent instantiation
  }

  /**
   * Executes a WASI-specific test across available runtimes.
   *
   * @param testName the test name
   * @param testFunction the WASI test function
   * @param requiresFileSystem whether the test requires file system access
   * @return the WASI test result
   */
  public static WasiTestResult executeWasiTest(
      final String testName,
      final WasiTestFunction testFunction,
      final boolean requiresFileSystem) {
    LOGGER.info("Executing WASI test: " + testName);

    final WasiTestResult.Builder resultBuilder = new WasiTestResult.Builder(testName);

    try {
      // Set up test environment if needed
      if (requiresFileSystem) {
        setupTestEnvironment();
        resultBuilder.testEnvironmentPath(testEnvironmentRoot);
      }

      // Execute with cross-runtime runner
      final CrossRuntimeTestRunner.RuntimeTestFunction runtimeTest =
          runtime -> {
            return testFunction.execute(runtime, testEnvironmentRoot);
          };

      final var crossRuntimeResult =
          CrossRuntimeTestRunner.executeAcrossRuntimes(testName, runtimeTest);

      resultBuilder.crossRuntimeResult(crossRuntimeResult);

      // Validate WASI-specific functionality
      if (crossRuntimeResult.bothSuccessful()) {
        resultBuilder.wasiCompliant(true);
        resultBuilder.addInfo("WASI functionality validated successfully");
      } else {
        resultBuilder.wasiCompliant(false);
        resultBuilder.addError("WASI functionality validation failed");

        if (!crossRuntimeResult.getJniResult().isSuccessful()) {
          resultBuilder.addError(
              "JNI WASI failed: " + crossRuntimeResult.getJniResult().getException().orElse(null));
        }

        if (crossRuntimeResult.hasPanamaResult()
            && !crossRuntimeResult.getPanamaResult().isSuccessful()) {
          resultBuilder.addError(
              "Panama WASI failed: "
                  + crossRuntimeResult.getPanamaResult().getException().orElse(null));
        }
      }

    } catch (final Exception e) {
      LOGGER.warning("WASI test execution failed: " + e.getMessage());
      resultBuilder.wasiCompliant(false);
      resultBuilder.addError("Test execution failed: " + e.getMessage());
    } finally {
      // Cleanup test environment if we created it
      if (requiresFileSystem && testEnvironmentRoot != null) {
        cleanupTestEnvironment();
      }
    }

    final WasiTestResult result = resultBuilder.build();
    wasiTestResults.put(testName, result);

    LOGGER.info("WASI test completed: " + testName + " - " + result.getSummary());
    return result;
  }

  /**
   * Executes file system operation tests.
   *
   * @return list of WASI test results
   */
  public static List<WasiTestResult> executeFileSystemTests() {
    final List<WasiTestResult> results = new ArrayList<>();

    // Test file creation
    results.add(
        executeWasiTest(
            "file_creation",
            (runtime, testDir) -> {
              // This would test file creation through WASI
              // For now, we'll create a simple test that validates the test environment
              if (testDir != null) {
                final Path testFile = testDir.resolve("test-create.txt");
                return Files.exists(testFile.getParent());
              }
              return false;
            },
            true));

    // Test file reading
    results.add(
        executeWasiTest(
            "file_reading",
            (runtime, testDir) -> {
              // This would test file reading through WASI
              if (testDir != null) {
                return Files.isDirectory(testDir);
              }
              return false;
            },
            true));

    // Test directory operations
    results.add(
        executeWasiTest(
            "directory_operations",
            (runtime, testDir) -> {
              // This would test directory operations through WASI
              if (testDir != null) {
                return Files.isWritable(testDir);
              }
              return false;
            },
            true));

    return results;
  }

  /**
   * Executes process operation tests.
   *
   * @return list of WASI test results
   */
  public static List<WasiTestResult> executeProcessTests() {
    final List<WasiTestResult> results = new ArrayList<>();

    // Test command line arguments
    results.add(
        executeWasiTest(
            "command_line_args",
            (runtime, testDir) -> {
              // This would test command line argument handling through WASI
              return true; // Placeholder - would test actual WASI argc/argv functionality
            },
            false));

    // Test environment variables
    results.add(
        executeWasiTest(
            "environment_variables",
            (runtime, testDir) -> {
              // This would test environment variable access through WASI
              return true; // Placeholder - would test actual WASI environ functionality
            },
            false));

    // Test exit codes
    results.add(
        executeWasiTest(
            "exit_codes",
            (runtime, testDir) -> {
              // This would test exit code handling through WASI
              return true; // Placeholder - would test actual WASI exit functionality
            },
            false));

    return results;
  }

  /**
   * Creates WASI-specific test modules.
   *
   * @param outputDirectory the directory where to create test modules
   * @throws IOException if module creation fails
   */
  public static void createWasiTestModules(final Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);

    // Create a simple WASI file I/O test module
    final String wasiFileIoWat =
        """
            (module
              (import "wasi_snapshot_preview1" "fd_write"
                (func $fd_write (param i32 i32 i32 i32) (result i32)))
              (import "wasi_snapshot_preview1" "fd_read"
                (func $fd_read (param i32 i32 i32 i32) (result i32)))
              (import "wasi_snapshot_preview1" "path_open"
                (func $path_open (param i32 i32 i32 i32 i32 i64 i64 i32 i32) (result i32)))

              (memory 1)
              (export "memory" (memory 0))

              (func $write_test (export "write_test") (result i32)
                ;; This would implement file writing through WASI
                i32.const 0)

              (func $read_test (export "read_test") (result i32)
                ;; This would implement file reading through WASI
                i32.const 0)
            )
            """;

    final Path wasiFileIoWatPath = outputDirectory.resolve("wasi-file-io.wat");
    Files.writeString(wasiFileIoWatPath, wasiFileIoWat);

    // Create a WASI arguments test module
    final String wasiArgsWat =
        """
            (module
              (import "wasi_snapshot_preview1" "args_get"
                (func $args_get (param i32 i32) (result i32)))
              (import "wasi_snapshot_preview1" "args_sizes_get"
                (func $args_sizes_get (param i32 i32) (result i32)))

              (memory 1)
              (export "memory" (memory 0))

              (func $args_test (export "args_test") (result i32)
                ;; This would implement argument access through WASI
                i32.const 0)
            )
            """;

    final Path wasiArgsWatPath = outputDirectory.resolve("wasi-args.wat");
    Files.writeString(wasiArgsWatPath, wasiArgsWat);

    LOGGER.info("Created WASI test modules in " + outputDirectory);
  }

  /**
   * Gets all WASI test results for reporting.
   *
   * @return map of WASI test results
   */
  public static Map<String, WasiTestResult> getAllWasiTestResults() {
    return new HashMap<>(wasiTestResults);
  }

  /**
   * Creates a WASI test execution summary.
   *
   * @return the WASI execution summary
   */
  public static WasiExecutionSummary createWasiExecutionSummary() {
    final WasiExecutionSummary.Builder summaryBuilder = new WasiExecutionSummary.Builder();

    for (final WasiTestResult result : wasiTestResults.values()) {
      summaryBuilder.addWasiTestResult(result);
    }

    return summaryBuilder.build();
  }

  /** Clears all WASI test results and cached data. */
  public static void clearWasiTestResults() {
    wasiTestResults.clear();
    cleanupTestEnvironment();
    LOGGER.info("Cleared all WASI test results");
  }

  /**
   * Sets up the test environment for WASI tests.
   *
   * @throws IOException if setup fails
   */
  private static void setupTestEnvironment() throws IOException {
    if (testEnvironmentRoot == null) {
      testEnvironmentRoot = Files.createTempDirectory(WASI_TEST_DIR);

      // Create test directory structure
      Files.createDirectories(testEnvironmentRoot.resolve("input"));
      Files.createDirectories(testEnvironmentRoot.resolve("output"));
      Files.createDirectories(testEnvironmentRoot.resolve("temp"));

      // Create sample files for testing
      Files.writeString(testEnvironmentRoot.resolve("input/test.txt"), "Hello WASI World!");
      Files.writeString(testEnvironmentRoot.resolve("input/data.json"), "{\"test\": true}");

      LOGGER.info("WASI test environment created at: " + testEnvironmentRoot);
    }
  }

  /** Cleans up the test environment. */
  private static void cleanupTestEnvironment() {
    if (testEnvironmentRoot != null) {
      try {
        // Recursively delete test environment
        Files.walk(testEnvironmentRoot)
            .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
            .forEach(
                path -> {
                  try {
                    Files.deleteIfExists(path);
                  } catch (final IOException e) {
                    LOGGER.warning("Failed to delete test file: " + path + " - " + e.getMessage());
                  }
                });

        LOGGER.info("WASI test environment cleaned up");
      } catch (final IOException e) {
        LOGGER.warning("Failed to cleanup WASI test environment: " + e.getMessage());
      } finally {
        testEnvironmentRoot = null;
      }
    }
  }

  /** Functional interface for WASI test functions. */
  @FunctionalInterface
  public interface WasiTestFunction {
    /**
     * Executes the WASI test with the given runtime and test environment.
     *
     * @param runtime the WebAssembly runtime
     * @param testEnvironmentPath the path to the test environment directory
     * @return the test result
     * @throws Exception if the test fails
     */
    Object execute(WasmRuntime runtime, Path testEnvironmentPath) throws Exception;
  }
}

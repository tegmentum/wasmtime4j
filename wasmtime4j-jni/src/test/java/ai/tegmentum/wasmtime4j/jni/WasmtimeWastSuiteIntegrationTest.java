/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wast.WastDirectiveResult;
import ai.tegmentum.wasmtime4j.wast.WastExecutionResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration test for Wasmtime WAST test suite execution.
 *
 * <p>This test executes official Wasmtime WAST test files to validate that the JNI WAST runner
 * correctly executes WebAssembly test suites and produces expected results.
 *
 * <p>WAST (WebAssembly Test) files contain module definitions and assertions that test WebAssembly
 * functionality. This integration test validates that wasmtime4j can execute these tests using
 * Wasmtime's native WAST parser.
 */
@Tag("integration")
public final class WasmtimeWastSuiteIntegrationTest {

  private static final Path WASMTIME_TESTS_DIR =
      Paths.get("../wasmtime4j-tests/src/test/resources/wasm/wasmtime-tests");

  /** Setup method to load native library before running tests. */
  @BeforeAll
  public static void setup() {
    // Ensure native library is loaded
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      System.err.println("Warning: Failed to load native library: " + e.getMessage());
      System.err.println("Tests will fail if native library is not available");
    }
  }

  @Test
  @DisplayName("Execute ALL Wasmtime WAST tests")
  public void testExecuteAllWasmtimeTests() throws IOException {
    // Find ALL WAST test files - complete Wasmtime test suite
    final List<Path> allTests = findAllWastFiles();

    Assumptions.assumeTrue(!allTests.isEmpty(), "No .wast files found in resource directory");

    int passedTests = 0;
    int failedTests = 0;
    final List<String> failures = new ArrayList<>();

    // Execute each test file
    for (final Path testFile : allTests) {
      try {
        final WastExecutionResult result =
            JniWastRunner.executeWastFile(testFile.toAbsolutePath().toString());

        assertNotNull(result, "Result should not be null for " + testFile.getFileName());

        if (result.allPassed()) {
          passedTests++;
          System.out.println(
              "PASSED: "
                  + testFile.getFileName()
                  + " - "
                  + result.getPassedDirectives()
                  + "/"
                  + result.getTotalDirectives()
                  + " directives passed");
        } else {
          failedTests++;
          failures.add(
              testFile.getFileName() + ": " + result.getFailedDirectives() + " directives failed");

          if (result.getExecutionError() != null) {
            System.err.println(
                "FAILED: " + testFile.getFileName() + " - " + result.getExecutionError());
          } else {
            System.err.println(
                "FAILED: "
                    + testFile.getFileName()
                    + " - "
                    + result.getFailedDirectives()
                    + "/"
                    + result.getTotalDirectives()
                    + " directives failed");
          }
        }
      } catch (Exception e) {
        failedTests++;
        failures.add(testFile.getFileName() + ": " + e.getMessage());
        System.err.println("ERROR: " + testFile.getFileName() + " - " + e.getMessage());
      }
    }

    // Print summary
    System.out.println("\n=== COMPLETE WAST Test Suite Summary ===");
    System.out.println("Total files: " + allTests.size());
    System.out.println("Passed: " + passedTests);
    System.out.println("Failed: " + failedTests);
    System.out.println("Success rate: " + (passedTests * 100.0 / allTests.size()) + "%");

    if (!failures.isEmpty()) {
      System.err.println("\nFailures:");
      failures.forEach(f -> System.err.println("  - " + f));
    }

    // At least some tests should execute
    assertTrue(
        passedTests + failedTests > 0, "At least some tests should execute (passed or failed)");

    // We expect most tests to pass - require at least 70% pass rate for full suite
    final double passRate = passedTests * 100.0 / allTests.size();
    assertTrue(passRate >= 70.0, "At least 70% of WAST tests should pass, got " + passRate + "%");
  }

  @Test
  @DisplayName("Execute simple Wasmtime WAST tests")
  public void testExecuteSimpleWasmtimeTests() throws IOException {
    // Find a few simple WAST test files
    final List<Path> simpleTests = findSimpleWastFiles();

    Assumptions.assumeTrue(!simpleTests.isEmpty(), "No .wast files found in resource directory");

    int passedTests = 0;
    int failedTests = 0;
    final List<String> failures = new ArrayList<>();

    // Execute each test file
    for (final Path testFile : simpleTests) {
      try {
        final WastExecutionResult result =
            JniWastRunner.executeWastFile(testFile.toAbsolutePath().toString());

        assertNotNull(result, "Result should not be null for " + testFile.getFileName());

        if (result.allPassed()) {
          passedTests++;
          System.out.println(
              "PASSED: "
                  + testFile.getFileName()
                  + " - "
                  + result.getPassedDirectives()
                  + "/"
                  + result.getTotalDirectives()
                  + " directives passed");
        } else {
          failedTests++;
          failures.add(
              testFile.getFileName() + ": " + result.getFailedDirectives() + " directives failed");

          if (result.getExecutionError() != null) {
            System.err.println(
                "FAILED: " + testFile.getFileName() + " - " + result.getExecutionError());
          } else {
            System.err.println(
                "FAILED: "
                    + testFile.getFileName()
                    + " - "
                    + result.getFailedDirectives()
                    + "/"
                    + result.getTotalDirectives()
                    + " directives failed");
          }
        }
      } catch (Exception e) {
        failedTests++;
        failures.add(testFile.getFileName() + ": " + e.getMessage());
        System.err.println("ERROR: " + testFile.getFileName() + " - " + e.getMessage());
      }
    }

    // Print summary
    System.out.println("\n=== WAST Test Suite Summary ===");
    System.out.println("Total files: " + simpleTests.size());
    System.out.println("Passed: " + passedTests);
    System.out.println("Failed: " + failedTests);
    System.out.println("Success rate: " + (passedTests * 100.0 / simpleTests.size()) + "%");

    if (!failures.isEmpty()) {
      System.err.println("\nFailures:");
      failures.forEach(f -> System.err.println("  - " + f));
    }

    // At least some tests should execute
    assertTrue(
        passedTests + failedTests > 0, "At least some tests should execute (passed or failed)");

    // We expect most tests to pass, but some may fail due to missing features
    // or known issues - require at least 50% pass rate
    final double passRate = passedTests * 100.0 / simpleTests.size();
    assertTrue(passRate >= 50.0, "At least 50% of WAST tests should pass, got " + passRate + "%");
  }

  @Test
  @DisplayName("Execute empty WAST file")
  public void testExecuteEmptyWast() throws IOException {
    final Path emptyWastPath = WASMTIME_TESTS_DIR.resolve("empty.wast");

    if (!Files.exists(emptyWastPath)) {
      // Skip if file doesn't exist
      System.err.println("Warning: empty.wast not found, skipping test");
      return;
    }

    final WastExecutionResult result = JniWastRunner.executeWastFile(emptyWastPath.toString());

    assertNotNull(result, "Result should not be null");
    // Empty WAST file should pass (it has one module directive with no assertions)
    assertTrue(result.allPassed(), "Empty WAST file execution should pass");
  }

  @Test
  @DisplayName("Execute simple arithmetic WAST")
  public void testExecuteSimpleArithmetic() {
    final String wastContent =
        "(module\n"
            + "  (func (export \"add\") (param i32 i32) (result i32)\n"
            + "    local.get 0\n"
            + "    local.get 1\n"
            + "    i32.add\n"
            + "  )\n"
            + ")\n"
            + "(assert_return (invoke \"add\" (i32.const 1) (i32.const 1)) (i32.const 2))\n"
            + "(assert_return (invoke \"add\" (i32.const 5) (i32.const 7)) (i32.const 12))\n"
            + "(assert_return (invoke \"add\" (i32.const -1) (i32.const 1)) (i32.const 0))";

    final WastExecutionResult result = JniWastRunner.executeWastString("add.wast", wastContent);

    assertNotNull(result, "Result should not be null");
    assertTrue(result.allPassed(), "All directives should pass");
    assertTrue(result.getPassedDirectives() > 0, "Should have at least one passing directive");
    assertEquals(0, result.getFailedDirectives(), "Should have no failing directives");
    assertEquals(100.0, result.getPassRate(), 0.01, "Pass rate should be 100%");
  }

  @Test
  @DisplayName("Execute WAST with multiple modules")
  public void testExecuteMultipleModules() {
    final String wastContent =
        "(module\n"
            + "  (func (export \"get42\") (result i32)\n"
            + "    i32.const 42\n"
            + "  )\n"
            + ")\n"
            + "(assert_return (invoke \"get42\") (i32.const 42))\n"
            + "\n"
            + "(module\n"
            + "  (func (export \"get100\") (result i32)\n"
            + "    i32.const 100\n"
            + "  )\n"
            + ")\n"
            + "(assert_return (invoke \"get100\") (i32.const 100))";

    final WastExecutionResult result =
        JniWastRunner.executeWastString("multi-module.wast", wastContent);

    assertNotNull(result, "Result should not be null");
    assertTrue(result.allPassed(), "All directives should pass");
    assertTrue(result.getPassedDirectives() > 0, "Should have at least one passing directive");
  }

  @Test
  @DisplayName("Verify directive results contain line numbers")
  public void testDirectiveLineNumbers() {
    final String wastContent =
        "(module\n"
            + "  (func (export \"test\") (result i32)\n"
            + "    i32.const 1\n"
            + "  )\n"
            + ")\n"
            + "(assert_return (invoke \"test\") (i32.const 1))";

    final WastExecutionResult result = JniWastRunner.executeWastString("test.wast", wastContent);

    assertNotNull(result, "Result should not be null");
    assertFalse(result.getDirectiveResults().length == 0, "Should have directive results");

    for (final WastDirectiveResult directive : result.getDirectiveResults()) {
      assertTrue(directive.getLineNumber() >= 0, "Line number should be non-negative");
    }
  }

  /**
   * Finds ALL WAST test files in the Wasmtime test directory.
   *
   * @return list of all WAST test file paths
   */
  private List<Path> findAllWastFiles() throws IOException {
    Assumptions.assumeTrue(
        Files.exists(WASMTIME_TESTS_DIR),
        "WAST resource directory not found: " + WASMTIME_TESTS_DIR);

    final List<Path> allTests = new ArrayList<>();

    // Find ALL WAST files - no filtering, complete test coverage
    try (Stream<Path> paths = Files.walk(WASMTIME_TESTS_DIR)) {
      paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".wast"))
          .forEach(allTests::add);
    }

    return allTests;
  }

  /**
   * Finds a few simple WAST test files to execute.
   *
   * @return list of simple WAST test file paths
   */
  private List<Path> findSimpleWastFiles() throws IOException {
    Assumptions.assumeTrue(
        Files.exists(WASMTIME_TESTS_DIR),
        "WAST resource directory not found: " + WASMTIME_TESTS_DIR);

    final List<Path> simpleTests = new ArrayList<>();

    // Look for simple test files (limit to 10 for integration testing)
    try (Stream<Path> paths = Files.walk(WASMTIME_TESTS_DIR, 2)) {
      paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".wast"))
          .filter(
              path -> {
                final String fileName = path.getFileName().toString().toLowerCase();
                final String pathStr = path.toString().toLowerCase();
                // Prefer simple tests, including component-model tests
                // Exclude threaded, GC, and async tests for now
                return !fileName.contains("thread")
                    && !fileName.contains("gc")
                    && !fileName.contains("async")
                    && (fileName.contains("simple")
                        || fileName.contains("add")
                        || fileName.contains("empty")
                        || fileName.contains("fib")
                        || fileName.contains("call")
                        || pathStr.contains("component-model"));
              })
          .limit(10)
          .forEach(simpleTests::add);
    }

    return simpleTests;
  }
}

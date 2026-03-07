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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wast.WastDirectiveResult;
import ai.tegmentum.wasmtime4j.wast.WastExecutionResult;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JniWastRunner.
 *
 * <p>These tests verify that WAST (WebAssembly Test) execution works correctly using Wasmtime's
 * native WAST parser.
 */
public final class JniWastRunnerTest {

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
  @DisplayName("Execute simple WAST file with passing assertions")
  public void testExecuteSimpleWastFile() throws URISyntaxException, IOException {
    // Get path to test WAST file
    final Path wastPath = Paths.get(getClass().getResource("/test-simple.wast").toURI());
    assertTrue(Files.exists(wastPath), "Test WAST file should exist");

    // Execute WAST file
    final WastExecutionResult result = JniWastRunner.executeWastFile(wastPath.toString());

    // Verify results
    assertNotNull(result, "Result should not be null");
    assertTrue(result.allPassed(), "All directives should pass");
    assertEquals(0, result.getFailedDirectives(), "No directives should fail");
    assertTrue(result.getPassedDirectives() > 0, "Should have at least one passing directive");
    assertNull(result.getExecutionError(), "Should have no execution error");
    assertEquals(100.0, result.getPassRate(), 0.01, "Pass rate should be 100%");
  }

  @Test
  @DisplayName("Execute WAST content from string")
  public void testExecuteWastString() {
    // Simple WAST content with a module and assertion
    final String wastContent =
        "(module\n"
            + "  (func (export \"get42\") (result i32)\n"
            + "    i32.const 42\n"
            + "  )\n"
            + ")\n"
            + "(assert_return (invoke \"get42\") (i32.const 42))";

    // Execute WAST string
    final WastExecutionResult result = JniWastRunner.executeWastString("inline.wast", wastContent);

    // Verify results
    assertNotNull(result, "Result should not be null");
    assertTrue(result.allPassed(), "All directives should pass");
    assertEquals("inline.wast", result.getFilePath(), "File path should match provided name");
  }

  @Test
  @DisplayName("Execute WAST with failing assertion")
  public void testExecuteWastWithFailure() {
    // WAST content with a failing assertion
    final String wastContent =
        "(module\n"
            + "  (func (export \"add\") (param i32 i32) (result i32)\n"
            + "    local.get 0\n"
            + "    local.get 1\n"
            + "    i32.add\n"
            + "  )\n"
            + ")\n"
            + ";; This assertion should fail: 1 + 2 = 3, not 999\n"
            + "(assert_return (invoke \"add\" (i32.const 1) (i32.const 2)) (i32.const 999))";

    // Execute WAST string
    final WastExecutionResult result = JniWastRunner.executeWastString("failing.wast", wastContent);

    // Verify that it failed
    assertNotNull(result, "Result should not be null");
    assertFalse(result.allPassed(), "Directives should not all pass");
    assertTrue(result.getFailedDirectives() > 0, "Should have at least one failing directive");
  }

  @Test
  @DisplayName("Execute WAST with invalid syntax")
  public void testExecuteWastWithInvalidSyntax() {
    // WAST content with invalid syntax
    final String wastContent =
        "(module\n"
            + "  (func (export \"invalid\") (result i32)\n"
            + "    this is not valid wast syntax\n"
            + "  )\n"
            + ")";

    // Execute WAST string
    final WastExecutionResult result = JniWastRunner.executeWastString("invalid.wast", wastContent);

    // Verify that execution error occurred
    assertNotNull(result, "Result should not be null");
    assertFalse(result.allPassed(), "Should not pass with invalid syntax");
    assertNotNull(result.getExecutionError(), "Should have execution error");
  }

  @Test
  @DisplayName("Execute non-existent WAST file")
  public void testExecuteNonExistentFile() {
    final String nonExistentPath = "/path/that/does/not/exist.wast";

    // Execute non-existent file
    final WastExecutionResult result = JniWastRunner.executeWastFile(nonExistentPath);

    // Verify error is reported
    assertNotNull(result, "Result should not be null");
    assertFalse(result.allPassed(), "Should not pass for non-existent file");
    assertNotNull(result.getExecutionError(), "Should have execution error");
    assertTrue(
        result.getExecutionError().contains("not found")
            || result.getExecutionError().contains("No such file"),
        "Error message should indicate file not found");
  }

  @Test
  @DisplayName("Execute WAST bytes")
  public void testExecuteWastBytes() {
    final String wastContent =
        "(module\n"
            + "  (func (export \"negate\") (param i32) (result i32)\n"
            + "    local.get 0\n"
            + "    i32.const -1\n"
            + "    i32.mul\n"
            + "  )\n"
            + ")\n"
            + "(assert_return (invoke \"negate\" (i32.const 5)) (i32.const -5))\n"
            + "(assert_return (invoke \"negate\" (i32.const -10)) (i32.const 10))";

    final byte[] wastBytes = wastContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

    // Execute WAST bytes
    final WastExecutionResult result = JniWastRunner.executeWastBytes("bytes.wast", wastBytes);

    // Verify results
    assertNotNull(result, "Result should not be null");
    assertTrue(result.allPassed(), "All directives should pass");
  }

  @Test
  @DisplayName("executeWastFileOrThrow should throw on failure")
  public void testExecuteWastFileOrThrow() {
    final String wastContent =
        "(module\n"
            + "  (func (export \"test\") (result i32)\n"
            + "    i32.const 1\n"
            + "  )\n"
            + ")\n"
            + "(assert_return (invoke \"test\") (i32.const 999))";

    // Should throw WastExecutionException
    assertThrows(
        ai.tegmentum.wasmtime4j.wast.WastRunner.WastExecutionException.class,
        () -> {
          JniWastRunner.executeWastStringOrThrow("fail.wast", wastContent);
        },
        "Should throw WastExecutionException for failing test");
  }

  @Test
  @DisplayName("executeWastStringOrThrow should not throw on success")
  public void testExecuteWastStringOrThrowSuccess() {
    final String wastContent =
        "(module\n"
            + "  (func (export \"test\") (result i32)\n"
            + "    i32.const 42\n"
            + "  )\n"
            + ")\n"
            + "(assert_return (invoke \"test\") (i32.const 42))";

    // Should not throw
    assertDoesNotThrow(
        () -> {
          JniWastRunner.executeWastStringOrThrow("success.wast", wastContent);
        },
        "Should not throw for passing test");
  }

  @Test
  @DisplayName("Null or empty arguments should throw IllegalArgumentException")
  public void testNullOrEmptyArguments() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          JniWastRunner.executeWastFile((String) null);
        },
        "Null file path should throw");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          JniWastRunner.executeWastFile("");
        },
        "Empty file path should throw");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          JniWastRunner.executeWastString(null, "content");
        },
        "Null filename should throw");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          JniWastRunner.executeWastString("file.wast", null);
        },
        "Null content should throw");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          JniWastRunner.executeWastBytes(null, new byte[0]);
        },
        "Null filename for bytes should throw");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          JniWastRunner.executeWastBytes("file.wast", null);
        },
        "Null bytes should throw");
  }

  @Test
  @DisplayName("WastExecutionResult toString should provide meaningful output")
  public void testWastExecutionResultToString() {
    final String wastContent =
        "(module\n"
            + "  (func (export \"test\") (result i32)\n"
            + "    i32.const 1\n"
            + "  )\n"
            + ")\n"
            + "(assert_return (invoke \"test\") (i32.const 1))";

    final WastExecutionResult result = JniWastRunner.executeWastString("test.wast", wastContent);

    final String toString = result.toString();
    assertNotNull(toString, "toString should not be null");
    assertTrue(toString.contains("test.wast"), "toString should contain file path");
    assertTrue(
        toString.contains("passed") || toString.contains("100"),
        "toString should indicate success");
  }

  @Test
  @DisplayName("WastDirectiveResult should contain meaningful information")
  public void testWastDirectiveResult() {
    final String wastContent =
        "(module\n"
            + "  (func (export \"add\") (param i32 i32) (result i32)\n"
            + "    local.get 0\n"
            + "    local.get 1\n"
            + "    i32.add\n"
            + "  )\n"
            + ")\n"
            + "(assert_return (invoke \"add\" (i32.const 1) (i32.const 2)) (i32.const 3))";

    final WastExecutionResult result =
        JniWastRunner.executeWastString("directive.wast", wastContent);

    assertFalse(result.getDirectiveResults().length == 0, "Should have directive results");

    for (final WastDirectiveResult directive : result.getDirectiveResults()) {
      assertNotNull(directive, "Directive should not be null");
      assertTrue(directive.getLineNumber() >= 0, "Line number should be non-negative");

      final String dirToString = directive.toString();
      assertNotNull(dirToString, "Directive toString should not be null");
      assertTrue(dirToString.contains("WastDirectiveResult"), "toString should contain class name");
    }
  }
}

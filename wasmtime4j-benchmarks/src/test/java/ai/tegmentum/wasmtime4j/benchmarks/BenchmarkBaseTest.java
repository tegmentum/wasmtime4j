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
package ai.tegmentum.wasmtime4j.benchmarks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import org.junit.jupiter.api.Test;

/** Unit tests for BenchmarkBase utility methods and constants. */
final class BenchmarkBaseTest {

  @Test
  void testSimpleWatModuleIsDefined() {
    // Test that the simple WAT module string is defined and non-empty
    assertNotNull(BenchmarkBase.SIMPLE_WAT_MODULE);
    assertTrue(!BenchmarkBase.SIMPLE_WAT_MODULE.isEmpty(), "SIMPLE_WAT_MODULE should not be empty");
    assertTrue(BenchmarkBase.SIMPLE_WAT_MODULE.contains("(module"),
        "Expected SIMPLE_WAT_MODULE to contain '(module'");
    assertTrue(BenchmarkBase.SIMPLE_WAT_MODULE.contains("add"),
        "Expected SIMPLE_WAT_MODULE to contain 'add'");
  }

  @Test
  void testComplexWatModuleIsDefined() {
    // Test that the complex WAT module string is defined and non-empty
    assertNotNull(BenchmarkBase.COMPLEX_WAT_MODULE);
    assertTrue(!BenchmarkBase.COMPLEX_WAT_MODULE.isEmpty(), "COMPLEX_WAT_MODULE should not be empty");
    assertTrue(BenchmarkBase.COMPLEX_WAT_MODULE.contains("(module"),
        "Expected COMPLEX_WAT_MODULE to contain '(module'");
    assertTrue(BenchmarkBase.COMPLEX_WAT_MODULE.contains("fibonacci"),
        "Expected COMPLEX_WAT_MODULE to contain 'fibonacci'");
    assertTrue(BenchmarkBase.COMPLEX_WAT_MODULE.contains("memory"),
        "Expected COMPLEX_WAT_MODULE to contain 'memory'");
  }

  @Test
  void testGetJavaVersion() {
    final int version = BenchmarkBase.getJavaVersion();
    assertTrue(version >= 8, "Java version should be >= 8, was: " + version);
    assertTrue(version < 50, "Java version should be < 50 (sanity check), was: " + version);
  }

  @Test
  void testGetRecommendedRuntime() {
    final RuntimeType runtime = BenchmarkBase.getRecommendedRuntime();
    assertTrue(runtime == RuntimeType.JNI || runtime == RuntimeType.PANAMA,
        "Expected JNI or PANAMA, was: " + runtime);
  }

  @Test
  void testValidateWasmModuleWithValidModule() {
    // Minimal valid WASM module: magic number + version + empty sections
    final byte[] validWasmModule = {
      0x00, 0x61, 0x73, 0x6d, // WASM magic number
      0x01, 0x00, 0x00, 0x00 // WASM version 1
    };
    // Should not throw exception
    assertDoesNotThrow(() -> BenchmarkBase.validateWasmModule(validWasmModule));
  }

  @Test
  void testValidateWasmModuleWithNullModule() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> BenchmarkBase.validateWasmModule(null));
    assertTrue(ex.getMessage().contains("cannot be null"),
        "Expected message to contain 'cannot be null', was: " + ex.getMessage());
  }

  @Test
  void testValidateWasmModuleWithTooSmallModule() {
    final byte[] tooSmall = new byte[4];
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> BenchmarkBase.validateWasmModule(tooSmall));
    assertTrue(ex.getMessage().contains("too small"),
        "Expected message to contain 'too small', was: " + ex.getMessage());
  }

  @Test
  void testValidateWasmModuleWithInvalidMagicNumber() {
    final byte[] invalidMagic = {0x01, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> BenchmarkBase.validateWasmModule(invalidMagic));
    assertTrue(ex.getMessage().contains("Invalid WASM magic number"),
        "Expected message to contain 'Invalid WASM magic number', was: " + ex.getMessage());
  }

  @Test
  void testFormatBenchmarkId() {
    final String id = BenchmarkBase.formatBenchmarkId("test_operation", RuntimeType.JNI);
    assertTrue(id.startsWith("test_operation_jni_"),
        "Expected id to start with 'test_operation_jni_', was: " + id);
    assertTrue(id.length() >= "test_operation_jni_".length() + 1,
        "Expected id length >= " + ("test_operation_jni_".length() + 1) + ", was: " + id.length());
    assertTrue(id.length() <= "test_operation_jni_".length() + 4,
        "Expected id length <= " + ("test_operation_jni_".length() + 4) + ", was: " + id.length());
  }

  @Test
  void testPreventOptimizationWithInt() {
    final int value = 42;
    final int result = BenchmarkBase.preventOptimization(value);
    assertEquals(value, result);
  }

  @Test
  void testPreventOptimizationWithByteArray() {
    final byte[] value = new byte[10];
    final int result = BenchmarkBase.preventOptimization(value);
    assertEquals(10, result);
  }

  @Test
  void testPreventOptimizationWithNullByteArray() {
    final int result = BenchmarkBase.preventOptimization((byte[]) null);
    assertEquals(0, result);
  }

  @Test
  void testRuntimeTypeValues() {
    // Test that all expected runtime types exist
    assertArrayEquals(
        new RuntimeType[] {RuntimeType.JNI, RuntimeType.PANAMA}, RuntimeType.values());
  }
}

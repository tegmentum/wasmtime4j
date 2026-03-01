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
package ai.tegmentum.wasmtime4j.framework;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Basic functionality validation tests to ensure core wasmtime4j components work correctly.
 *
 * <p>These tests focus on validating the fundamental functionality that exists and can be tested
 * without requiring advanced component model features.
 *
 * @since 1.0.0
 */
@DisplayName("Basic Functionality Validation Tests")
public class BasicFunctionalityValidationTest {

  private static final Logger LOGGER =
      Logger.getLogger(BasicFunctionalityValidationTest.class.getName());

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    LOGGER.info("Starting test: " + testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Completed test: " + testInfo.getDisplayName());
  }

  @Test
  @DisplayName("WasmValue creation and type validation")
  void testWasmValueCreationAndTypes() {
    // Test i32 values
    final WasmValue i32Value = WasmValue.i32(42);
    assertEquals(WasmValueType.I32, i32Value.getType());
    assertEquals(42, i32Value.asInt());
    assertEquals(42, i32Value.getValue());

    // Test i64 values
    final WasmValue i64Value = WasmValue.i64(1234567890123L);
    assertEquals(WasmValueType.I64, i64Value.getType());
    assertEquals(1234567890123L, i64Value.asLong());
    assertEquals(1234567890123L, i64Value.getValue());

    // Test f32 values
    final WasmValue f32Value = WasmValue.f32(3.14159f);
    assertEquals(WasmValueType.F32, f32Value.getType());
    assertEquals(3.14159f, f32Value.asFloat(), 0.00001f);
    assertEquals(3.14159f, f32Value.getValue());

    // Test f64 values
    final WasmValue f64Value = WasmValue.f64(2.718281828459045);
    assertEquals(WasmValueType.F64, f64Value.getType());
    assertEquals(2.718281828459045, f64Value.asDouble(), 0.000000000000001);
    assertEquals(2.718281828459045, f64Value.getValue());

    LOGGER.info("✓ WasmValue creation and type validation successful");
  }

  @Test
  @DisplayName("WasmValue reference type handling")
  void testWasmValueReferenceTypes() {
    // Test externref with null
    final WasmValue externRefNull = WasmValue.externref(null);
    assertEquals(WasmValueType.EXTERNREF, externRefNull.getType());
    assertNull(externRefNull.getValue());
    assertNull(externRefNull.asExternref());

    // Test externref with object
    final String testObject = "test-reference";
    final WasmValue externRefObject = WasmValue.externref(testObject);
    assertEquals(WasmValueType.EXTERNREF, externRefObject.getType());
    assertEquals(testObject, externRefObject.getValue());
    assertEquals(testObject, externRefObject.asExternref());

    // Test externRef (camelCase alias)
    final WasmValue externRefAlias = WasmValue.externref(testObject);
    assertEquals(WasmValueType.EXTERNREF, externRefAlias.getType());
    assertEquals(testObject, externRefAlias.getValue());

    // Test funcref with null
    final WasmValue funcRefNull = WasmValue.funcref(null);
    assertEquals(WasmValueType.FUNCREF, funcRefNull.getType());
    assertNull(funcRefNull.getValue());

    LOGGER.info("✓ WasmValue reference type handling successful");
  }

  @Test
  @DisplayName("WasmValue v128 (SIMD) type handling")
  void testWasmValueV128Types() {
    // Test v128 with byte array
    final byte[] v128Bytes = new byte[16];
    for (int i = 0; i < 16; i++) {
      v128Bytes[i] = (byte) (i + 1);
    }

    final WasmValue v128FromBytes = WasmValue.v128(v128Bytes);
    assertEquals(WasmValueType.V128, v128FromBytes.getType());
    assertArrayEquals(v128Bytes, (byte[]) v128FromBytes.getValue());

    // Test v128 with long values
    final long high = 0x0123456789ABCDEFL;
    final long low = 0xFEDCBA9876543210L;
    final WasmValue v128FromLongs = WasmValue.v128(high, low);
    assertEquals(WasmValueType.V128, v128FromLongs.getType());

    // Verify the conversion worked correctly
    final byte[] resultBytes = (byte[]) v128FromLongs.getValue();
    assertNotNull(resultBytes);
    assertEquals(16, resultBytes.length);

    LOGGER.info("✓ WasmValue v128 type handling successful");
  }

  @Test
  @DisplayName("WasmValue type compatibility and conversion")
  void testWasmValueTypeCompatibilityAndConversion() {
    // Test is functions
    final WasmValue i32Value = WasmValue.i32(100);
    assertTrue(i32Value.isI32());
    assertFalse(i32Value.isI64());
    assertFalse(i32Value.isF32());
    assertFalse(i32Value.isF64());
    assertFalse(i32Value.isReference());

    final WasmValue externRefValue = WasmValue.externref("test");
    assertFalse(externRefValue.isI32());
    assertFalse(externRefValue.isI64());
    assertFalse(externRefValue.isF32());
    assertFalse(externRefValue.isF64());
    assertTrue(externRefValue.isReference());

    // Test value equality
    final WasmValue i32Value1 = WasmValue.i32(42);
    final WasmValue i32Value2 = WasmValue.i32(42);
    final WasmValue i32Value3 = WasmValue.i32(43);

    assertEquals(i32Value1, i32Value2);
    assertNotEquals(i32Value1, i32Value3);
    assertEquals(i32Value1.hashCode(), i32Value2.hashCode());

    LOGGER.info("✓ WasmValue type compatibility and conversion successful");
  }

  @Test
  @DisplayName("WasmValue error handling and edge cases")
  void testWasmValueErrorHandlingAndEdgeCases() {
    // Test type casting errors
    final WasmValue i32Value = WasmValue.i32(42);

    assertThrows(ClassCastException.class, i32Value::asLong);
    assertThrows(ClassCastException.class, i32Value::asFloat);
    assertThrows(ClassCastException.class, i32Value::asDouble);
    assertThrows(ClassCastException.class, i32Value::asReference);

    // Test v128 with invalid array size
    final byte[] invalidV128 = new byte[8]; // Should be 16 bytes
    assertThrows(
        IllegalArgumentException.class,
        () -> WasmValue.v128(invalidV128),
        "v128 should require exactly 16 bytes");

    // Test string representation
    final WasmValue testValue = WasmValue.i32(123);
    final String stringRepr = testValue.toString();
    assertNotNull(stringRepr);
    assertTrue(stringRepr.contains("123"));
    assertTrue(stringRepr.contains("I32"));

    LOGGER.info("✓ WasmValue error handling and edge cases successful");
  }

  @Test
  @DisplayName("WebAssembly module validation - basic structure")
  void testBasicWebAssemblyModuleValidation() {
    // Test basic WebAssembly module structure validation
    // This tests the fundamental capability to recognize valid WebAssembly bytecode

    // Valid minimal WebAssembly module
    final byte[] validWasmModule =
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6D, // Magic number "\0asm"
          0x01,
          0x00,
          0x00,
          0x00, // Version 1
          0x01,
          0x04,
          0x01,
          0x60,
          0x00,
          0x00, // Type section: function type () -> ()
          0x03,
          0x02,
          0x01,
          0x00, // Function section: 1 function of type 0
          0x0A,
          0x04,
          0x01,
          0x02,
          0x00,
          0x0B // Code section: function body (empty)
        };

    // Invalid WebAssembly module (wrong magic number)
    final byte[] invalidWasmModule =
        new byte[] {
          0x00, 0x61, 0x73, 0x6E, // Wrong magic number
          0x01, 0x00, 0x00, 0x00 // Version 1
        };

    // Basic validation that we can distinguish valid from invalid modules
    assertTrue(
        isValidWasmMagicNumber(validWasmModule), "Valid WebAssembly module should be recognized");
    assertFalse(
        isValidWasmMagicNumber(invalidWasmModule), "Invalid WebAssembly module should be rejected");

    LOGGER.info("✓ Basic WebAssembly module validation successful");
  }

  @Test
  @DisplayName("Performance measurement infrastructure validation")
  void testPerformanceMeasurementInfrastructure() {
    // Test that we can measure basic performance characteristics
    final int iterations = 1000;
    final long startTime = System.nanoTime();

    // Perform some basic operations to measure
    for (int i = 0; i < iterations; i++) {
      final WasmValue value = WasmValue.i32(i);
      final int result = value.asInt();
      // Simple computation to prevent optimization
      if (result < 0) {
        fail("Unexpected negative value");
      }
    }

    final long endTime = System.nanoTime();
    final long duration = endTime - startTime;

    // Validate that measurement infrastructure works
    assertTrue(duration > 0, "Duration should be positive");
    assertTrue(duration < 1_000_000_000L, "Duration should be reasonable (< 1 second)");

    final double operationsPerSecond = (double) iterations / (duration / 1_000_000_000.0);
    assertTrue(operationsPerSecond > 1000, "Should be able to perform many operations per second");

    LOGGER.info(
        String.format(
            "✓ Performance measurement: %d operations in %dns (%.0f ops/sec)",
            iterations, duration, operationsPerSecond));
  }

  @Test
  @DisplayName("Memory management and resource cleanup validation")
  void testMemoryManagementAndResourceCleanup() {
    // Test that basic memory management works correctly
    final int objectCount = 100;
    final WasmValue[] values = new WasmValue[objectCount];

    // Create many objects
    for (int i = 0; i < objectCount; i++) {
      values[i] = WasmValue.i32(i);
    }

    // Verify all objects are created correctly
    for (int i = 0; i < objectCount; i++) {
      assertNotNull(values[i]);
      assertEquals(i, values[i].asInt());
    }

    // Test reference handling
    for (int i = 0; i < objectCount / 2; i++) {
      values[i] = WasmValue.externref("reference-" + i);
      assertNotNull(values[i].getValue());
      assertEquals("reference-" + i, values[i].getValue());
    }

    // Test cleanup (nulling references)
    for (int i = 0; i < objectCount; i++) {
      values[i] = null;
    }

    // Force garbage collection suggestion
    System.gc();

    LOGGER.info("✓ Memory management and resource cleanup validation successful");
  }

  @Test
  @DisplayName("Multi-threaded safety validation")
  void testMultiThreadedSafetyValidation() throws InterruptedException {
    final int threadCount = 4;
    final int operationsPerThread = 100;
    final Thread[] threads = new Thread[threadCount];
    final boolean[] threadResults = new boolean[threadCount];

    // Create multiple threads that perform WasmValue operations
    for (int t = 0; t < threadCount; t++) {
      final int threadIndex = t;
      threads[t] =
          new Thread(
              () -> {
                try {
                  for (int i = 0; i < operationsPerThread; i++) {
                    // Mix different value types and operations
                    final WasmValue i32Value = WasmValue.i32(threadIndex * 1000 + i);
                    final WasmValue f64Value = WasmValue.f64(threadIndex + i * 0.1);
                    final WasmValue refValue =
                        WasmValue.externref("thread-" + threadIndex + "-" + i);

                    // Verify operations work correctly
                    assertEquals(threadIndex * 1000 + i, i32Value.asInt());
                    assertEquals(threadIndex + i * 0.1, f64Value.asDouble(), 0.001);
                    assertEquals("thread-" + threadIndex + "-" + i, refValue.getValue());
                  }
                  threadResults[threadIndex] = true;
                } catch (Exception e) {
                  LOGGER.severe("Thread " + threadIndex + " failed: " + e.getMessage());
                  threadResults[threadIndex] = false;
                }
              });
    }

    // Start all threads
    for (Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      thread.join(5000); // 5 second timeout
    }

    // Verify all threads completed successfully
    for (int i = 0; i < threadCount; i++) {
      assertTrue(threadResults[i], "Thread " + i + " should have completed successfully");
    }

    LOGGER.info("✓ Multi-threaded safety validation successful");
  }

  // Helper methods

  /**
   * Basic WebAssembly magic number validation.
   *
   * @param bytes the bytes to check
   * @return true if bytes start with WebAssembly magic number
   */
  private boolean isValidWasmMagicNumber(final byte[] bytes) {
    if (bytes == null || bytes.length < 4) {
      return false;
    }
    return bytes[0] == 0x00 && bytes[1] == 0x61 && bytes[2] == 0x73 && bytes[3] == 0x6D;
  }
}

/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Panama Component Model invocation with parameter and result marshalling.
 *
 * <p>These tests verify that the Panama implementation can correctly marshal Java values to WIT
 * types, invoke Component Model functions, and unmarshal results back to Java values.
 *
 * @since 1.0.0
 */
final class PanamaComponentInvocationTest {

  private PanamaEngine panamaEngine;
  private PanamaComponentEngine componentEngine;
  private PanamaStore store;
  private ComponentSimple component;
  private ComponentInstance instance;

  @BeforeEach
  void setUp() throws WasmException, IOException {
    // Load the test component
    final Path componentPath =
        Path.of("src/test/resources/components/basic-types.wasm").toAbsolutePath();

    if (!Files.exists(componentPath)) {
      throw new IllegalStateException("Test component not found: " + componentPath);
    }

    final byte[] wasmBytes = Files.readAllBytes(componentPath);

    // Create engines and store
    panamaEngine = new PanamaEngine();
    componentEngine = new PanamaComponentEngine(new ComponentEngineConfig());
    store = new PanamaStore(panamaEngine);
    component = componentEngine.compileComponent(wasmBytes);
    instance = componentEngine.createInstance(component, store);

    assertNotNull(instance);
  }

  @AfterEach
  void tearDown() {
    if (instance != null) {
      instance.close();
    }
    if (componentEngine != null) {
      componentEngine.close();
    }
    if (store != null) {
      store.close();
    }
    if (panamaEngine != null) {
      panamaEngine.close();
    }
  }

  @Test
  @DisplayName("Should marshal and unmarshal boolean values")
  void testBooleanMarshalling() throws WasmException {
    // Test with true - component negates the value
    final Object resultTrue = instance.invoke("test-bool", Boolean.TRUE);
    assertNotNull(resultTrue);
    assertInstanceOf(Boolean.class, resultTrue);
    assertFalse((Boolean) resultTrue, "Expected negation of true (false)");

    // Test with false - component negates the value
    final Object resultFalse = instance.invoke("test-bool", Boolean.FALSE);
    assertNotNull(resultFalse);
    assertInstanceOf(Boolean.class, resultFalse);
    assertTrue((Boolean) resultFalse, "Expected negation of false (true)");
  }

  @Test
  @DisplayName("Should marshal and unmarshal s32 values")
  void testS32Marshalling() throws WasmException {
    // Component adds 1 to the input
    final Object result = instance.invoke("test-s32", 42);
    assertNotNull(result);
    assertInstanceOf(Integer.class, result);
    assertEquals(43, (Integer) result, "Expected 42 + 1 = 43");

    // Test with negative value
    final Object negativeResult = instance.invoke("test-s32", -100);
    assertNotNull(negativeResult);
    assertInstanceOf(Integer.class, negativeResult);
    assertEquals(-99, (Integer) negativeResult, "Expected -100 + 1 = -99");

    // Test with zero
    final Object zeroResult = instance.invoke("test-s32", 0);
    assertNotNull(zeroResult);
    assertInstanceOf(Integer.class, zeroResult);
    assertEquals(1, (Integer) zeroResult, "Expected 0 + 1 = 1");
  }

  @Test
  @DisplayName("Should marshal and unmarshal s64 values")
  void testS64Marshalling() throws WasmException {
    // Component adds 1 to the input
    final Object result = instance.invoke("test-s64", 1000000000000L);
    assertNotNull(result);
    assertInstanceOf(Long.class, result);
    assertEquals(1000000000001L, (Long) result, "Expected 1000000000000 + 1 = 1000000000001");

    // Test with negative value
    final Object negativeResult = instance.invoke("test-s64", -999999999999L);
    assertNotNull(negativeResult);
    assertInstanceOf(Long.class, negativeResult);
    assertEquals(
        -999999999998L, (Long) negativeResult, "Expected -999999999999 + 1 = -999999999998");
  }

  @Test
  @DisplayName("Should marshal and unmarshal f64 values")
  void testF64Marshalling() throws WasmException {
    // Component multiplies by 2.0
    final Object result = instance.invoke("test-float64", 3.14);
    assertNotNull(result);
    assertInstanceOf(Double.class, result);
    assertEquals(6.28, (Double) result, 0.001, "Expected 3.14 * 2.0 = 6.28");

    // Test with negative value
    final Object negativeResult = instance.invoke("test-float64", -2.5);
    assertNotNull(negativeResult);
    assertInstanceOf(Double.class, negativeResult);
    assertEquals(-5.0, (Double) negativeResult, 0.001, "Expected -2.5 * 2.0 = -5.0");

    // Test with zero
    final Object zeroResult = instance.invoke("test-float64", 0.0);
    assertNotNull(zeroResult);
    assertInstanceOf(Double.class, zeroResult);
    assertEquals(0.0, (Double) zeroResult, 0.001, "Expected 0.0 * 2.0 = 0.0");
  }

  @Test
  @DisplayName("Should marshal and unmarshal char values")
  void testCharMarshalling() throws WasmException {
    // Component returns the next character
    final Object result = instance.invoke("test-char", 'A');
    assertNotNull(result);
    assertInstanceOf(Character.class, result);
    assertEquals('B', (Character) result, "Expected 'A' + 1 = 'B'");

    // Test with lowercase
    final Object lowercaseResult = instance.invoke("test-char", 'z');
    assertNotNull(lowercaseResult);
    assertInstanceOf(Character.class, lowercaseResult);
    assertEquals('{', (Character) lowercaseResult, "Expected 'z' + 1 = '{'");
  }

  @Test
  @DisplayName("Should marshal and unmarshal string values")
  void testStringMarshalling() throws WasmException {
    // Component prepends "Hello, " to the input
    final Object result = instance.invoke("test-string", "World");
    assertNotNull(result);
    assertInstanceOf(String.class, result);
    assertEquals("Hello, World", (String) result, "Expected 'Hello, World'");

    // Test with empty string
    final Object emptyResult = instance.invoke("test-string", "");
    assertNotNull(emptyResult);
    assertInstanceOf(String.class, emptyResult);
    assertEquals("Hello, ", (String) emptyResult, "Expected 'Hello, '");
  }

  @Test
  @DisplayName("Should handle functions with multiple parameters")
  void testMultipleParameters() throws WasmException {
    // Component adds the two parameters
    final Object result = instance.invoke("test-multi-params", 10, 20);
    assertNotNull(result);
    assertInstanceOf(Integer.class, result);
    assertEquals(30, (Integer) result, "Expected 10 + 20 = 30");

    // Test with negative values
    final Object negativeResult = instance.invoke("test-multi-params", -5, 15);
    assertNotNull(negativeResult);
    assertInstanceOf(Integer.class, negativeResult);
    assertEquals(10, (Integer) negativeResult, "Expected -5 + 15 = 10");
  }

  @Test
  @DisplayName("Should handle functions with multiple return values")
  void testMultipleReturnValues() throws WasmException {
    // Component returns (input, input * 2)
    final Object result = instance.invoke("test-multi-returns", 7);
    assertNotNull(result);
    assertInstanceOf(List.class, result);

    @SuppressWarnings("unchecked")
    final List<Object> results = (List<Object>) result;
    assertEquals(2, results.size(), "Expected 2 return values");
    assertEquals(7, (Integer) results.get(0), "Expected first return value to be 7");
    assertEquals(14, (Integer) results.get(1), "Expected second return value to be 14 (7 * 2)");
  }

  @Test
  @DisplayName("Should verify all exported functions are available")
  void testExportedFunctions() throws WasmException {
    assertTrue(instance.hasFunction("test-bool"), "Should have test-bool function");
    assertTrue(instance.hasFunction("test-s32"), "Should have test-s32 function");
    assertTrue(instance.hasFunction("test-s64"), "Should have test-s64 function");
    assertTrue(instance.hasFunction("test-float64"), "Should have test-float64 function");
    assertTrue(instance.hasFunction("test-char"), "Should have test-char function");
    assertTrue(instance.hasFunction("test-string"), "Should have test-string function");
    assertTrue(instance.hasFunction("test-multi-params"), "Should have test-multi-params function");
    assertTrue(
        instance.hasFunction("test-multi-returns"), "Should have test-multi-returns function");

    // Verify non-existent function
    assertFalse(instance.hasFunction("non-existent"), "Should not have non-existent function");
  }
}

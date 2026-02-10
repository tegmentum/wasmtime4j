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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Component;
import ai.tegmentum.wasmtime4j.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wit.WitU32;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Panama Component Model record type marshalling.
 *
 * <p>These tests verify that the Panama implementation can correctly marshal Java Maps to WIT
 * record types, invoke Component Model functions with records, and unmarshal record results back to
 * Java Maps.
 *
 * @since 1.0.0
 */
final class PanamaComponentRecordTest {

  private PanamaEngine panamaEngine;
  private PanamaComponentEngine componentEngine;
  private PanamaStore store;
  private Component component;
  private ComponentInstance instance;

  @BeforeEach
  void setUp() throws WasmException, IOException {
    // Load the with-records test component
    final Path componentPath =
        Path.of("src/test/resources/components/with-records.wasm").toAbsolutePath();

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
  @DisplayName("Should marshal and unmarshal person record")
  void testPersonRecord() throws WasmException {
    // Create a person record as a Map
    // Note: age is u32 in WIT, so we use WitU32 explicitly
    final Map<String, Object> person = new LinkedHashMap<>();
    person.put("name", "Alice");
    person.put("age", WitU32.of(30));
    person.put("email", "alice@example.com");

    // Invoke echo-person function
    final Object result = instance.invoke("echo-person", person);

    // Verify result
    assertNotNull(result, "Result should not be null");
    assertInstanceOf(Map.class, result, "Result should be a Map");

    @SuppressWarnings("unchecked")
    final Map<String, Object> resultPerson = (Map<String, Object>) result;

    assertEquals("Alice", resultPerson.get("name"), "Name should match");
    assertEquals(30, resultPerson.get("age"), "Age should match");
    assertEquals("alice@example.com", resultPerson.get("email"), "Email should match");
  }

  @Test
  @DisplayName("Should marshal and unmarshal point record")
  void testPointRecord() throws WasmException {
    // Create a point record as a Map
    final Map<String, Object> point = new LinkedHashMap<>();
    point.put("x", 3.14);
    point.put("y", 2.71);

    // Invoke echo-point function
    final Object result = instance.invoke("echo-point", point);

    // Verify result
    assertNotNull(result, "Result should not be null");
    assertInstanceOf(Map.class, result, "Result should be a Map");

    @SuppressWarnings("unchecked")
    final Map<String, Object> resultPoint = (Map<String, Object>) result;

    assertEquals(3.14, (Double) resultPoint.get("x"), 0.001, "X coordinate should match");
    assertEquals(2.71, (Double) resultPoint.get("y"), 0.001, "Y coordinate should match");
  }

  @Test
  @DisplayName("Should create person from individual fields")
  void testCreatePerson() throws WasmException {
    // Invoke create-person function with individual parameters
    // Note: age parameter is u32 in WIT, so we use WitU32 explicitly
    final Object result = instance.invoke("create-person", "Bob", WitU32.of(25), "bob@example.com");

    // Verify result
    assertNotNull(result, "Result should not be null");
    assertInstanceOf(Map.class, result, "Result should be a Map");

    @SuppressWarnings("unchecked")
    final Map<String, Object> resultPerson = (Map<String, Object>) result;

    assertEquals("Bob", resultPerson.get("name"), "Name should match");
    assertEquals(25, resultPerson.get("age"), "Age should match");
    assertEquals("bob@example.com", resultPerson.get("email"), "Email should match");
  }

  @Test
  @DisplayName("Should handle empty string fields in person record")
  void testPersonWithEmptyFields() throws WasmException {
    // Note: age is u32 in WIT, so we use WitU32 explicitly
    final Map<String, Object> person = new LinkedHashMap<>();
    person.put("name", "");
    person.put("age", WitU32.of(0));
    person.put("email", "");

    final Object result = instance.invoke("echo-person", person);

    assertNotNull(result);
    assertInstanceOf(Map.class, result);

    @SuppressWarnings("unchecked")
    final Map<String, Object> resultPerson = (Map<String, Object>) result;

    assertEquals("", resultPerson.get("name"), "Empty name should be preserved");
    assertEquals(0, resultPerson.get("age"), "Zero age should be preserved");
    assertEquals("", resultPerson.get("email"), "Empty email should be preserved");
  }

  @Test
  @DisplayName("Should handle zero and negative coordinates in point record")
  void testPointWithZeroAndNegativeValues() throws WasmException {
    final Map<String, Object> point = new LinkedHashMap<>();
    point.put("x", -5.5);
    point.put("y", 0.0);

    final Object result = instance.invoke("echo-point", point);

    assertNotNull(result);
    assertInstanceOf(Map.class, result);

    @SuppressWarnings("unchecked")
    final Map<String, Object> resultPoint = (Map<String, Object>) result;

    assertEquals(-5.5, (Double) resultPoint.get("x"), 0.001, "Negative X should be preserved");
    assertEquals(0.0, (Double) resultPoint.get("y"), 0.001, "Zero Y should be preserved");
  }

  @Test
  @DisplayName("Should verify exported functions with records")
  void testExportedFunctionsWithRecords() throws WasmException {
    assertTrue(instance.hasFunction("echo-person"), "Should have echo-person function");
    assertTrue(instance.hasFunction("echo-point"), "Should have echo-point function");
    assertTrue(instance.hasFunction("create-person"), "Should have create-person function");
  }
}

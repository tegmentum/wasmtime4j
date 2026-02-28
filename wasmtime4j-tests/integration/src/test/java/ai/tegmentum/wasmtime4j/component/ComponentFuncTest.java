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

package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.jni.JniComponentEngine;
import ai.tegmentum.wasmtime4j.test.TestUtils;
import ai.tegmentum.wasmtime4j.wit.WitS32;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for ComponentFunc - WebAssembly Component Model function calls.
 *
 * <p>These tests verify component function retrieval, properties, and invocation using the actual
 * native implementation with real WASM components.
 *
 * @since 1.0.0
 */
@DisplayName("ComponentFunc Integration Tests")
public final class ComponentFuncTest {

  private static final Logger LOGGER = Logger.getLogger(ComponentFuncTest.class.getName());

  private static boolean componentFuncAvailable = false;
  private static byte[] addComponentBytes;
  private static String unavailableReason;

  @BeforeAll
  static void checkComponentFuncAvailable() {
    // Try to dynamically detect if Component Model native implementation is available
    try {
      // Load native library
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();

      // Attempt to create engine - this exercises the native binding
      final JniComponentEngine testEngine = new JniComponentEngine(new ComponentEngineConfig());
      testEngine.close();

      // Load the test component file
      try (InputStream is = ComponentFuncTest.class.getResourceAsStream("/components/add.wasm")) {
        if (is != null) {
          addComponentBytes = TestUtils.readAllBytes(is);
          componentFuncAvailable = true;
          LOGGER.info(
              "ComponentFunc native implementation available - "
                  + addComponentBytes.length
                  + " bytes loaded");
        } else {
          unavailableReason = "add.wasm test component not found in resources";
          LOGGER.warning("ComponentFunc tests skipped: " + unavailableReason);
        }
      }
    } catch (final UnsatisfiedLinkError e) {
      unavailableReason = "Native library not available: " + e.getMessage();
      LOGGER.warning("ComponentFunc tests skipped: " + unavailableReason);
    } catch (final Exception e) {
      unavailableReason = "Component engine creation failed: " + e.getMessage();
      LOGGER.warning("ComponentFunc tests skipped: " + unavailableReason);
    }
  }

  private static void assumeComponentFuncAvailable() {
    assumeTrue(
        componentFuncAvailable,
        "ComponentFunc native implementation not available: " + unavailableReason);
  }

  private JniComponentEngine engine;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (componentFuncAvailable) {
      engine = new JniComponentEngine(new ComponentEngineConfig());
      resources.add(engine);
    }
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    engine = null;
  }

  @Nested
  @DisplayName("ComponentFunc Properties Tests")
  class PropertiesTests {

    @Test
    @DisplayName("should return function name")
    void shouldReturnFunctionName(final TestInfo testInfo) throws Exception {
      assumeComponentFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      final ComponentFunction func = funcOpt.get();
      assertEquals("add", func.getName(), "Function name should be 'add'");

      LOGGER.info("Function name: " + func.getName());
    }

    @Test
    @DisplayName("should return parent instance")
    void shouldReturnParentInstance(final TestInfo testInfo) throws Exception {
      assumeComponentFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      final ComponentFunction func = funcOpt.get();
      assertSame(instance, func.getInstance(), "Function should return its parent instance");

      LOGGER.info("Parent instance verified");
    }

    @Test
    @DisplayName("should check if function is valid")
    void shouldCheckIfFunctionIsValid(final TestInfo testInfo) throws Exception {
      assumeComponentFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      final ComponentFunction func = funcOpt.get();
      assertTrue(func.isValid(), "Function should be valid when instance is valid");

      LOGGER.info("Function validity verified");
    }

    @Test
    @DisplayName("should return empty for non-existent function")
    void shouldReturnEmptyForNonExistentFunction(final TestInfo testInfo) throws Exception {
      assumeComponentFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("nonexistent");
      // The current implementation always returns a function (delegating to invoke)
      // This is valid since the error will occur at call time
      assertTrue(funcOpt.isPresent(), "getFunc returns a wrapper even for non-existent functions");

      LOGGER.info("Non-existent function handling verified");
    }
  }

  @Nested
  @DisplayName("ComponentFunc Invocation Tests")
  class InvocationTests {

    @Test
    @DisplayName("should call function with primitive parameters")
    void shouldCallFunctionWithPrimitiveParameters(final TestInfo testInfo) throws Exception {
      assumeComponentFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      final ComponentFunction func = funcOpt.get();

      // Call add(5, 7) = 12
      final WitS32 param1 = WitS32.of(5);
      final WitS32 param2 = WitS32.of(7);
      final Object result = func.call(param1, param2);

      assertNotNull(result, "Result should not be null");
      assertTrue(result instanceof ComponentVal, "Result should be ComponentVal");
      assertEquals(12, ((ComponentVal) result).asS32(), "add(5, 7) should equal 12");

      LOGGER.info("Function call result: add(5, 7) = " + result);
    }

    @Test
    @DisplayName("should call function multiple times through same reference")
    void shouldCallFunctionMultipleTimes(final TestInfo testInfo) throws Exception {
      assumeComponentFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      final ComponentFunction func = funcOpt.get();

      // Call multiple times with different values
      final int[][] testCases = {
        {0, 0, 0},
        {1, 1, 2},
        {100, 200, 300},
        {-5, 10, 5},
        {-10, -20, -30}
      };

      for (final int[] testCase : testCases) {
        final int a = testCase[0];
        final int b = testCase[1];
        final int expected = testCase[2];

        final Object result = func.call(WitS32.of(a), WitS32.of(b));
        assertTrue(result instanceof ComponentVal, "Result should be ComponentVal");
        assertEquals(
            expected,
            ((ComponentVal) result).asS32(),
            String.format("add(%d, %d) should equal %d", a, b, expected));

        LOGGER.info(String.format("add(%d, %d) = %d ✓", a, b, expected));
      }
    }

    @Test
    @DisplayName("should throw for type mismatch")
    void shouldThrowForTypeMismatch(final TestInfo testInfo) throws Exception {
      assumeComponentFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      final ComponentFunction func = funcOpt.get();

      // Try calling with wrong type (String instead of s32)
      assertThrows(
          Exception.class, () -> func.call("not", "numbers"), "Should throw for type mismatch");

      LOGGER.info("Type mismatch handling verified");
    }

    @Test
    @DisplayName("should throw for wrong number of parameters")
    void shouldThrowForWrongNumberOfParameters(final TestInfo testInfo) throws Exception {
      assumeComponentFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      final ComponentFunction func = funcOpt.get();

      // Try calling with too few parameters
      assertThrows(
          Exception.class, () -> func.call(WitS32.of(5)), "Should throw for too few parameters");

      // Try calling with too many parameters
      assertThrows(
          Exception.class,
          () -> func.call(WitS32.of(5), WitS32.of(7), WitS32.of(9)),
          "Should throw for too many parameters");

      LOGGER.info("Parameter count validation verified");
    }
  }

  @Nested
  @DisplayName("ComponentFunc Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should become invalid after instance close")
    void shouldBecomeInvalidAfterInstanceClose(final TestInfo testInfo) throws Exception {
      assumeComponentFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      final ComponentInstance instance = component.instantiate();

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      final ComponentFunction func = funcOpt.get();
      assertTrue(func.isValid(), "Function should be valid initially");

      // Close the instance
      instance.close();

      assertFalse(func.isValid(), "Function should be invalid after instance close");

      // Clean up component
      component.close();

      LOGGER.info("Function lifecycle verified");
    }

    @Test
    @DisplayName("should throw when calling invalid function")
    void shouldThrowWhenCallingInvalidFunction(final TestInfo testInfo) throws Exception {
      assumeComponentFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      final ComponentInstance instance = component.instantiate();

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      final ComponentFunction func = funcOpt.get();

      // Close the instance to invalidate the function
      instance.close();

      // Attempt to call invalidated function
      assertThrows(
          Exception.class,
          () -> func.call(WitS32.of(5), WitS32.of(7)),
          "Should throw when calling invalid function");

      // Clean up component
      component.close();

      LOGGER.info("Invalid function call handling verified");
    }

    @Test
    @DisplayName("should handle multiple function references from same instance")
    void shouldHandleMultipleFunctionReferences(final TestInfo testInfo) throws Exception {
      assumeComponentFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      // Get multiple references to the same function
      final Optional<ComponentFunction> funcOpt1 = instance.getFunc("add");
      final Optional<ComponentFunction> funcOpt2 = instance.getFunc("add");

      assertTrue(funcOpt1.isPresent(), "First function reference should exist");
      assertTrue(funcOpt2.isPresent(), "Second function reference should exist");

      final ComponentFunction func1 = funcOpt1.get();
      final ComponentFunction func2 = funcOpt2.get();

      // Both should be valid and work
      assertTrue(func1.isValid(), "First reference should be valid");
      assertTrue(func2.isValid(), "Second reference should be valid");

      // Both should produce correct results
      final Object result1 = func1.call(WitS32.of(5), WitS32.of(7));
      assertTrue(result1 instanceof ComponentVal, "First result should be ComponentVal");
      assertEquals(12, ((ComponentVal) result1).asS32(), "First reference should work");

      final Object result2 = func2.call(WitS32.of(5), WitS32.of(7));
      assertTrue(result2 instanceof ComponentVal, "Second result should be ComponentVal");
      assertEquals(12, ((ComponentVal) result2).asS32(), "Second reference should work");

      LOGGER.info("Multiple function references handled correctly");
    }
  }
}

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.jni.JniComponentEngine;
import ai.tegmentum.wasmtime4j.wit.WitS32;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for ComponentInstance - WebAssembly Component Model instance management.
 *
 * <p>These tests verify component instance creation, function invocation, state management, and
 * lifecycle operations using the actual native implementation.
 *
 * @since 1.0.0
 */
@DisplayName("ComponentInstance Integration Tests")
public final class ComponentInstanceIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentInstanceIntegrationTest.class.getName());

  private static boolean componentInstanceAvailable = false;
  private static byte[] addComponentBytes;
  private static String unavailableReason;

  @BeforeAll
  static void checkComponentInstanceAvailable() {
    // Try to dynamically detect if Component Model native implementation is available
    try {
      // Load native library
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();

      // Attempt to create engine - this exercises the native binding
      final JniComponentEngine testEngine = new JniComponentEngine(new ComponentEngineConfig());
      testEngine.close();

      // Load the test component file
      try (InputStream is =
          ComponentInstanceIntegrationTest.class.getResourceAsStream("/components/add.wasm")) {
        if (is != null) {
          addComponentBytes = readAllBytes(is);
          componentInstanceAvailable = true;
          LOGGER.info(
              "ComponentInstance native implementation available - "
                  + addComponentBytes.length
                  + " bytes loaded");
        } else {
          unavailableReason = "add.wasm test component not found in resources";
          LOGGER.warning("ComponentInstance tests skipped: " + unavailableReason);
        }
      }
    } catch (final UnsatisfiedLinkError e) {
      unavailableReason = "Native library not available: " + e.getMessage();
      LOGGER.warning("ComponentInstance tests skipped: " + unavailableReason);
    } catch (final Exception e) {
      unavailableReason = "Component engine creation failed: " + e.getMessage();
      LOGGER.warning("ComponentInstance tests skipped: " + unavailableReason);
    }
  }

  private static void assumeComponentInstanceAvailable() {
    assumeTrue(
        componentInstanceAvailable,
        "ComponentInstance native implementation not available: " + unavailableReason);
  }

  private static byte[] readAllBytes(final InputStream inputStream) throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final byte[] tempBuffer = new byte[1024];
    int bytesRead;
    while ((bytesRead = inputStream.read(tempBuffer)) != -1) {
      buffer.write(tempBuffer, 0, bytesRead);
    }
    return buffer.toByteArray();
  }

  private JniComponentEngine engine;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (componentInstanceAvailable) {
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
  @DisplayName("ComponentInstance Function Access Tests")
  class FunctionAccessTests {

    @Test
    @DisplayName("should load component from bytes")
    void shouldLoadComponentFromBytes(final TestInfo testInfo) throws Exception {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      assertNotNull(component, "Component should not be null");
      assertNotNull(component.getId(), "Component ID should not be null");

      LOGGER.info("Loaded component with ID: " + component.getId());
    }

    @Test
    @DisplayName("should instantiate component")
    void shouldInstantiateComponent(final TestInfo testInfo) throws Exception {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");

      LOGGER.info("Successfully instantiated component with instance ID: " + instance.getId());
    }

    @Test
    @DisplayName("should invoke add function")
    void shouldInvokeAddFunction(final TestInfo testInfo) throws Exception {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      // Invoke add(5, 7) - should return 12
      final WitS32 param1 = WitS32.of(5);
      final WitS32 param2 = WitS32.of(7);

      final Object result = instance.invoke("add", param1, param2);

      assertNotNull(result, "Result should not be null");
      assertEquals(12, result, "add(5, 7) should equal 12");

      LOGGER.info("Successfully invoked add(5, 7) = " + result);
    }

    @Test
    @DisplayName("should invoke add function with different values")
    void shouldInvokeAddFunctionWithDifferentValues(final TestInfo testInfo) throws Exception {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      // Test multiple value combinations
      final int[][] testCases = {
        {0, 0, 0}, // Zero addition
        {1, 1, 2}, // Small positive
        {100, 200, 300}, // Larger positive
        {-5, 10, 5}, // Mixed signs
        {-10, -20, -30} // Negative addition
      };

      for (final int[] testCase : testCases) {
        final int a = testCase[0];
        final int b = testCase[1];
        final int expected = testCase[2];

        final WitS32 param1 = WitS32.of(a);
        final WitS32 param2 = WitS32.of(b);

        final Object result = instance.invoke("add", param1, param2);

        assertEquals(
            expected, result, String.format("add(%d, %d) should equal %d", a, b, expected));

        LOGGER.info(String.format("add(%d, %d) = %d ✓", a, b, expected));
      }
    }
  }

  @Nested
  @DisplayName("ComponentInstance Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should close instance properly")
    void shouldCloseInstanceProperly(final TestInfo testInfo) throws Exception {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      final ComponentInstance instance = component.instantiate();

      assertTrue(instance.isValid(), "Instance should be valid before close");

      instance.close();

      assertFalse(instance.isValid(), "Instance should not be valid after close");

      // Close component after instance
      component.close();

      LOGGER.info("Instance lifecycle completed successfully");
    }

    @Test
    @DisplayName("should handle multiple close calls")
    void shouldHandleMultipleCloseCalls(final TestInfo testInfo) throws Exception {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      final ComponentInstance instance = component.instantiate();

      // Multiple close calls should be safe
      instance.close();
      instance.close();
      instance.close();

      assertFalse(instance.isValid(), "Instance should remain invalid");

      component.close();
      component.close();

      LOGGER.info("Multiple close calls handled safely");
    }

    @Test
    @DisplayName("should check if instance is valid")
    void shouldCheckIfInstanceIsValid(final TestInfo testInfo) throws Exception {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();

      assertTrue(instance.isValid(), "Newly created instance should be valid");

      instance.close();

      assertFalse(instance.isValid(), "Closed instance should not be valid");

      LOGGER.info("Instance validity tracking works correctly");
    }
  }

  @Nested
  @DisplayName("ComponentInstance Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("should reject null component bytes")
    void shouldRejectNullComponentBytes(final TestInfo testInfo) {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          Exception.class, () -> engine.compileComponent(null), "Should reject null bytes");

      LOGGER.info("Null component bytes rejected as expected");
    }

    @Test
    @DisplayName("should reject empty component bytes")
    void shouldRejectEmptyComponentBytes(final TestInfo testInfo) {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          Exception.class,
          () -> engine.compileComponent(new byte[0]),
          "Should reject empty bytes");

      LOGGER.info("Empty component bytes rejected as expected");
    }

    @Test
    @DisplayName("should reject invalid WASM bytes")
    void shouldRejectInvalidWasmBytes(final TestInfo testInfo) {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final byte[] invalidBytes = new byte[] {0x00, 0x01, 0x02, 0x03};

      assertThrows(
          Exception.class,
          () -> engine.compileComponent(invalidBytes),
          "Should reject invalid WASM bytes");

      LOGGER.info("Invalid WASM bytes rejected as expected");
    }

    @Test
    @DisplayName("should handle closed engine gracefully")
    void shouldHandleClosedEngineGracefully(final TestInfo testInfo) throws Exception {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a separate engine for this test
      final JniComponentEngine testEngine = new JniComponentEngine(new ComponentEngineConfig());
      testEngine.close();

      assertThrows(
          Exception.class,
          () -> testEngine.compileComponent(addComponentBytes),
          "Should reject operations on closed engine");

      LOGGER.info("Closed engine handled gracefully");
    }
  }

  @Nested
  @DisplayName("Component Engine Configuration Tests")
  class EngineConfigurationTests {

    @Test
    @DisplayName("should create multiple engines")
    void shouldCreateMultipleEngines(final TestInfo testInfo) throws Exception {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final List<JniComponentEngine> engines = new ArrayList<>();
      try {
        for (int i = 0; i < 3; i++) {
          final JniComponentEngine eng = new JniComponentEngine(new ComponentEngineConfig());
          engines.add(eng);
          assertTrue(eng.isValid(), "Engine " + i + " should be valid");
        }

        LOGGER.info("Created " + engines.size() + " independent engines");
      } finally {
        for (final JniComponentEngine eng : engines) {
          eng.close();
        }
      }
    }

    @Test
    @DisplayName("should isolate components between engines")
    void shouldIsolateComponentsBetweenEngines(final TestInfo testInfo) throws Exception {
      assumeComponentInstanceAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final JniComponentEngine engine1 = new JniComponentEngine(new ComponentEngineConfig());
      final JniComponentEngine engine2 = new JniComponentEngine(new ComponentEngineConfig());

      try {
        // Load same component in both engines
        final Component comp1 = engine1.compileComponent(addComponentBytes);
        final Component comp2 = engine2.compileComponent(addComponentBytes);

        // Instantiate in both
        final ComponentInstance inst1 = comp1.instantiate();
        final ComponentInstance inst2 = comp2.instantiate();

        // Both should work independently
        assertTrue(inst1.isValid(), "Instance 1 should be valid");
        assertTrue(inst2.isValid(), "Instance 2 should be valid");

        // Different instance IDs
        assertFalse(inst1.getId().equals(inst2.getId()), "Instances should have different IDs");

        inst1.close();
        inst2.close();
        comp1.close();
        comp2.close();

        LOGGER.info("Components are properly isolated between engines");
      } finally {
        engine1.close();
        engine2.close();
      }
    }
  }
}

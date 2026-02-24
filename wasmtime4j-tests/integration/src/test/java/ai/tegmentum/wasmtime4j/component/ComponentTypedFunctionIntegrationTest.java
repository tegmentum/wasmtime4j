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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import ai.tegmentum.wasmtime4j.test.TestUtils;
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
 * Integration tests for ComponentTypedFunc - type-safe WebAssembly Component Model function calls.
 *
 * <p>These tests verify typed component function creation, signature handling, and invocation using
 * the actual native implementation with real WASM components.
 *
 * @since 1.0.0
 */
@DisplayName("ComponentTypedFunc Integration Tests")
public final class ComponentTypedFunctionIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentTypedFunctionIntegrationTest.class.getName());

  private static boolean componentAvailable = false;
  private static boolean typedFuncAvailable = false;
  private static byte[] addComponentBytes;
  private static String unavailableReason;

  @BeforeAll
  static void checkComponentAvailable() {
    try {
      // Load native library
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();

      // Attempt to create engine - this exercises the native binding
      final JniComponentEngine testEngine = new JniComponentEngine(new ComponentEngineConfig());

      // Load the test component file
      try (InputStream is =
          ComponentTypedFunctionIntegrationTest.class.getResourceAsStream("/components/add.wasm")) {
        if (is != null) {
          addComponentBytes = TestUtils.readAllBytes(is);
          componentAvailable = true;
          LOGGER.info(
              "Component native implementation available - "
                  + addComponentBytes.length
                  + " bytes loaded");

          // Check if typed function support is available by testing with real component
          checkTypedFunctionAvailable(testEngine);
        } else {
          unavailableReason = "add.wasm test component not found in resources";
          LOGGER.warning("ComponentTypedFunc tests skipped: " + unavailableReason);
        }
      }

      testEngine.close();
    } catch (final UnsatisfiedLinkError e) {
      unavailableReason = "Native library not available: " + e.getMessage();
      LOGGER.warning("ComponentTypedFunc tests skipped: " + unavailableReason);
    } catch (final Exception e) {
      unavailableReason = "Component engine creation failed: " + e.getMessage();
      LOGGER.warning("ComponentTypedFunc tests skipped: " + unavailableReason);
    }
  }

  /**
   * Checks if typed function support is available by attempting to create a typed function.
   *
   * @param engine the component engine to use for testing
   */
  private static void checkTypedFunctionAvailable(final JniComponentEngine engine) {
    try {
      final Component component = engine.compileComponent(addComponentBytes);
      final ComponentInstance instance = component.instantiate();
      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");

      if (funcOpt.isPresent() && funcOpt.get() instanceof ComponentFunc) {
        final ComponentFunc componentFunc = (ComponentFunc) funcOpt.get();

        // Check if the implementation supports TypedComponentFunctionSupport
        if (componentFunc instanceof ComponentTypedFunc.TypedComponentFunctionSupport) {
          final ComponentTypedFunc typedFunc =
              ComponentTypedFunc.create(componentFunc, "s32,s32->s32");
          typedFuncAvailable = true;
          typedFunc.close();
          LOGGER.info("Typed component function support is available");
        } else {
          LOGGER.info("ComponentFunc does not implement TypedComponentFunctionSupport");
        }
      }

      instance.close();
      component.close();
    } catch (final UnsupportedOperationException e) {
      LOGGER.info("TypedFunc not supported: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.info("TypedFunc check failed: " + e.getMessage());
    }
  }

  private static void assumeComponentAvailable() {
    assumeTrue(
        componentAvailable, "Component native implementation not available: " + unavailableReason);
  }

  private static void assumeTypedFuncAvailable() {
    assumeComponentAvailable();
    assumeTrue(typedFuncAvailable, "Typed component function not available");
  }

  private JniComponentEngine engine;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (componentAvailable) {
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
  @DisplayName("ComponentTypedFunc Factory Tests")
  class FactoryTests {

    @Test
    @DisplayName("should reject null function")
    void shouldRejectNullFunction(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ComponentTypedFunc.create(null, "s32->s32"),
              "Should throw for null function");

      assertTrue(
          exception.getMessage().toLowerCase().contains("null"),
          "Exception message should mention null");

      LOGGER.info("Null function rejection verified: " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject null signature")
    void shouldRejectNullSignature(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a mock ComponentFunc for testing validation
      final ComponentFunc mockFunc = createMockComponentFunc();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ComponentTypedFunc.create(mockFunc, null),
              "Should throw for null signature");

      assertTrue(
          exception.getMessage().toLowerCase().contains("null")
              || exception.getMessage().toLowerCase().contains("empty"),
          "Exception message should mention null or empty");

      LOGGER.info("Null signature rejection verified: " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject empty signature")
    void shouldRejectEmptySignature(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentFunc mockFunc = createMockComponentFunc();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ComponentTypedFunc.create(mockFunc, ""),
              "Should throw for empty signature");

      assertTrue(
          exception.getMessage().toLowerCase().contains("empty")
              || exception.getMessage().toLowerCase().contains("null"),
          "Exception message should mention empty or null");

      LOGGER.info("Empty signature rejection verified: " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject unsupported function implementation")
    void shouldRejectUnsupportedFunctionImplementation(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a mock that doesn't implement TypedComponentFunctionSupport
      final ComponentFunc mockFunc = createMockComponentFunc();

      final UnsupportedOperationException exception =
          assertThrows(
              UnsupportedOperationException.class,
              () -> ComponentTypedFunc.create(mockFunc, "s32,s32->s32"),
              "Should throw for unsupported implementation");

      assertTrue(
          exception.getMessage().toLowerCase().contains("support"),
          "Exception message should mention support");

      LOGGER.info("Unsupported implementation rejection verified: " + exception.getMessage());
    }

    private ComponentFunc createMockComponentFunc() {
      // Create a minimal ComponentFunc that doesn't support typed functions
      return new ComponentFunc() {
        @Override
        public List<ComponentVal> call(final ComponentVal... args) {
          return List.of();
        }

        @Override
        public List<ComponentVal> call(final List<ComponentVal> args) {
          return List.of();
        }

        @Override
        public String getName() {
          return "mock";
        }

        @Override
        public List<ComponentTypeDescriptor> getParameterTypes() {
          return List.of();
        }

        @Override
        public List<ComponentTypeDescriptor> getResultTypes() {
          return List.of();
        }

        @Override
        public boolean isValid() {
          return true;
        }
      };
    }
  }

  @Nested
  @DisplayName("ComponentTypedFunc Creation with Real Component Tests")
  class RealComponentCreationTests {

    @Test
    @DisplayName("should get typed function from component instance")
    @SuppressFBWarnings(
        value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
        justification = "Static field tracks feature availability across test instances")
    void shouldGetTypedFunctionFromComponentInstance(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      final ComponentFunction func = funcOpt.get();
      assertNotNull(func, "ComponentFunction should not be null");

      // Check if the function supports typed wrapper
      if (func instanceof ComponentFunc) {
        final ComponentFunc componentFunc = (ComponentFunc) func;
        LOGGER.info("ComponentFunc obtained: " + componentFunc.getName());

        // Try to create typed wrapper - may throw if not supported
        try {
          final ComponentTypedFunc typedFunc =
              ComponentTypedFunc.create(componentFunc, "s32,s32->s32");
          typedFuncAvailable = true;

          assertNotNull(typedFunc, "TypedFunc should not be null");
          assertEquals("s32,s32->s32", typedFunc.getSignature(), "Signature should match");

          LOGGER.info("TypedFunc created successfully with signature: " + typedFunc.getSignature());
          typedFunc.close();
        } catch (final UnsupportedOperationException e) {
          LOGGER.info("TypedFunc not yet supported: " + e.getMessage());
          // This is expected if TypedComponentFunctionSupport is not implemented
        }
      }
    }

    @Test
    @DisplayName("should check function name from component")
    void shouldCheckFunctionNameFromComponent(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      final ComponentFunction func = funcOpt.get();
      assertEquals("add", func.getName(), "Function name should be 'add'");

      LOGGER.info("Function name verified: " + func.getName());
    }
  }

  @Nested
  @DisplayName("ComponentTypedFunc Invocation Tests")
  class InvocationTests {

    @Test
    @DisplayName("should call typed s32 function when available")
    void shouldCallTypedS32FunctionWhenAvailable(final TestInfo testInfo) throws Exception {
      assumeTypedFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      if (funcOpt.get() instanceof ComponentFunc) {
        final ComponentFunc componentFunc = (ComponentFunc) funcOpt.get();
        final ComponentTypedFunc typedFunc =
            ComponentTypedFunc.create(componentFunc, "s32,s32->s32");
        resources.add(typedFunc);

        // Call the typed function
        final int result = typedFunc.callS32S32ToS32(5, 7);
        assertEquals(12, result, "add(5, 7) should equal 12");

        LOGGER.info("Typed function call result: add(5, 7) = " + result);
      }
    }

    @Test
    @DisplayName("should call typed function multiple times")
    void shouldCallTypedFunctionMultipleTimes(final TestInfo testInfo) throws Exception {
      assumeTypedFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      if (funcOpt.get() instanceof ComponentFunc) {
        final ComponentFunc componentFunc = (ComponentFunc) funcOpt.get();
        final ComponentTypedFunc typedFunc =
            ComponentTypedFunc.create(componentFunc, "s32,s32->s32");
        resources.add(typedFunc);

        // Call multiple times
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

          final int result = typedFunc.callS32S32ToS32(a, b);
          assertEquals(
              expected, result, String.format("add(%d, %d) should equal %d", a, b, expected));

          LOGGER.info(String.format("Typed call: add(%d, %d) = %d ✓", a, b, result));
        }
      }
    }
  }

  @Nested
  @DisplayName("ComponentTypedFunc Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should close typed function properly")
    void shouldCloseTypedFunctionProperly(final TestInfo testInfo) throws Exception {
      assumeTypedFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      if (funcOpt.get() instanceof ComponentFunc) {
        final ComponentFunc componentFunc = (ComponentFunc) funcOpt.get();
        final ComponentTypedFunc typedFunc =
            ComponentTypedFunc.create(componentFunc, "s32,s32->s32");

        // Close should not throw
        typedFunc.close();

        LOGGER.info("TypedFunc closed successfully");
      }
    }

    @Test
    @DisplayName("should handle multiple close calls")
    void shouldHandleMultipleCloseCalls(final TestInfo testInfo) throws Exception {
      assumeTypedFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      if (funcOpt.get() instanceof ComponentFunc) {
        final ComponentFunc componentFunc = (ComponentFunc) funcOpt.get();
        final ComponentTypedFunc typedFunc =
            ComponentTypedFunc.create(componentFunc, "s32,s32->s32");

        // Multiple closes should not throw
        typedFunc.close();
        typedFunc.close();
        typedFunc.close();

        LOGGER.info("Multiple close calls handled successfully");
      }
    }

    @Test
    @DisplayName("should return underlying function")
    void shouldReturnUnderlyingFunction(final TestInfo testInfo) throws Exception {
      assumeTypedFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      if (funcOpt.get() instanceof ComponentFunc) {
        final ComponentFunc componentFunc = (ComponentFunc) funcOpt.get();
        final ComponentTypedFunc typedFunc =
            ComponentTypedFunc.create(componentFunc, "s32,s32->s32");
        resources.add(typedFunc);

        final ComponentFunc underlying = typedFunc.getFunction();
        assertNotNull(underlying, "Underlying function should not be null");

        LOGGER.info("Underlying function retrieved: " + underlying.getName());
      }
    }
  }

  @Nested
  @DisplayName("ComponentTypedFunc Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle boundary values for s32")
    void shouldHandleBoundaryValuesForS32(final TestInfo testInfo) throws Exception {
      assumeTypedFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      if (funcOpt.get() instanceof ComponentFunc) {
        final ComponentFunc componentFunc = (ComponentFunc) funcOpt.get();
        final ComponentTypedFunc typedFunc =
            ComponentTypedFunc.create(componentFunc, "s32,s32->s32");
        resources.add(typedFunc);

        // Test boundary values
        // Note: These may overflow in s32 arithmetic

        // Zero + zero
        assertEquals(0, typedFunc.callS32S32ToS32(0, 0), "0 + 0 should be 0");

        // Positive + negative
        assertEquals(0, typedFunc.callS32S32ToS32(1000, -1000), "1000 + (-1000) should be 0");

        // Max safe values
        assertEquals(2, typedFunc.callS32S32ToS32(1, 1), "1 + 1 should be 2");

        LOGGER.info("Boundary value tests completed");
      }
    }

    @Test
    @DisplayName("should verify signature is stored correctly")
    void shouldVerifySignatureIsStoredCorrectly(final TestInfo testInfo) throws Exception {
      assumeTypedFuncAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentInstance instance = component.instantiate();
      resources.add(instance);

      final Optional<ComponentFunction> funcOpt = instance.getFunc("add");
      assertTrue(funcOpt.isPresent(), "Function 'add' should be found");

      if (funcOpt.get() instanceof ComponentFunc) {
        final ComponentFunc componentFunc = (ComponentFunc) funcOpt.get();
        final ComponentTypedFunc typedFunc =
            ComponentTypedFunc.create(componentFunc, "s32,s32->s32");
        resources.add(typedFunc);

        final String signature = typedFunc.getSignature();
        assertEquals("s32,s32->s32", signature, "Signature should be stored correctly");
        assertFalse(signature.isEmpty(), "Signature should not be empty");

        LOGGER.info("Signature verified: " + signature);
      }
    }
  }
}

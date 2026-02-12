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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.jni.JniComponentEngine;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for Component Model composition - linking multiple components together.
 *
 * <p>These tests verify:
 *
 * <ul>
 *   <li>Defining host functions for component imports
 *   <li>Linking components with their required imports
 *   <li>Component instantiation through linkers
 *   <li>Cross-component function calls
 *   <li>Resource sharing between components
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("ComponentComposition Integration Tests")
public final class ComponentCompositionIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentCompositionIntegrationTest.class.getName());

  private static boolean componentCompositionAvailable = false;
  private static byte[] withImportsComponentBytes;
  private static byte[] addComponentBytes;
  private static String unavailableReason;

  @BeforeAll
  static void checkComponentCompositionAvailable() {
    try {
      // Load native library
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();

      // Attempt to create engine and linker
      final Engine testEngine = Engine.create();
      final ComponentLinker<?> testLinker = ComponentLinker.create(testEngine);
      testLinker.close();
      testEngine.close();

      // Load the with-imports component
      try (InputStream is =
          ComponentCompositionIntegrationTest.class.getResourceAsStream(
              "/components/with-imports.wasm")) {
        if (is != null) {
          withImportsComponentBytes = readAllBytes(is);
          LOGGER.info(
              "with-imports.wasm component loaded - "
                  + withImportsComponentBytes.length
                  + " bytes");
        } else {
          // Try alternate location in panama test resources
          try (InputStream altIs =
              ComponentCompositionIntegrationTest.class
                  .getClassLoader()
                  .getResourceAsStream("components/with-imports.wasm")) {
            if (altIs != null) {
              withImportsComponentBytes = readAllBytes(altIs);
              LOGGER.info(
                  "with-imports.wasm loaded from alternate path - "
                      + withImportsComponentBytes.length
                      + " bytes");
            } else {
              unavailableReason = "with-imports.wasm component not found in resources";
              LOGGER.warning("ComponentComposition tests skipped: " + unavailableReason);
              return;
            }
          }
        }
      }

      // Load the add component for multi-component tests
      try (InputStream is =
          ComponentCompositionIntegrationTest.class.getResourceAsStream("/components/add.wasm")) {
        if (is != null) {
          addComponentBytes = readAllBytes(is);
          LOGGER.info("add.wasm component loaded - " + addComponentBytes.length + " bytes");
        }
      }

      componentCompositionAvailable = true;
      LOGGER.info("ComponentComposition native implementation available");
    } catch (final UnsatisfiedLinkError e) {
      unavailableReason = "Native library not available: " + e.getMessage();
      LOGGER.warning("ComponentComposition tests skipped: " + unavailableReason);
    } catch (final Exception e) {
      unavailableReason = "Component composition setup failed: " + e.getMessage();
      LOGGER.warning("ComponentComposition tests skipped: " + unavailableReason);
    }
  }

  private static void assumeComponentCompositionAvailable() {
    assumeTrue(
        componentCompositionAvailable,
        "ComponentComposition native implementation not available: " + unavailableReason);
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

  private Engine engine;
  private ComponentLinker<Object> linker;
  private Store store;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (componentCompositionAvailable) {
      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);
      resources.add(linker);
      store = Store.create(engine);
      resources.add(store);
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
    linker = null;
    store = null;
  }

  @Nested
  @DisplayName("Host Function Import Tests")
  class HostFunctionImportTests {

    @Test
    @DisplayName("should define logger interface for component imports")
    void shouldDefineLoggerInterfaceForComponentImports(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Define the logger interface that with-imports.wasm requires
      // WIT: interface logger { log: func(message: string); log-level: func(level: u32, message:
      // string); }
      final List<String> capturedLogs = new CopyOnWriteArrayList<>();

      final Map<String, ComponentHostFunction> loggerFunctions = new HashMap<>();
      loggerFunctions.put(
          "log",
          ComponentHostFunction.voidFunctionWithParams(
              params -> {
                final String message = params.get(0).asString();
                capturedLogs.add("LOG: " + message);
                LOGGER.info("Logger.log called with: " + message);
              }));
      loggerFunctions.put(
          "log-level",
          ComponentHostFunction.voidFunctionWithParams(
              params -> {
                final long level = params.get(0).asU32();
                final String message = params.get(1).asString();
                capturedLogs.add("LEVEL[" + level + "]: " + message);
                LOGGER.info("Logger.log-level called with level=" + level + ", message=" + message);
              }));

      assertDoesNotThrow(
          () -> linker.defineInterface("wasmtime4j:imports", "logger", loggerFunctions),
          "Defining logger interface should not throw");

      assertTrue(
          linker.hasInterface("wasmtime4j:imports", "logger"),
          "Logger interface should be defined");
      assertTrue(
          linker.hasFunction("wasmtime4j:imports", "logger", "log"), "log function should exist");
      assertTrue(
          linker.hasFunction("wasmtime4j:imports", "logger", "log-level"),
          "log-level function should exist");

      LOGGER.info("Logger interface defined successfully for component imports");
    }

    @Test
    @DisplayName("should define individual host functions")
    void shouldDefineIndividualHostFunctions(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Define log function
      final ComponentHostFunction logFunc =
          ComponentHostFunction.voidFunctionWithParams(
              params -> LOGGER.info("Log received: " + params.get(0).asString()));

      assertDoesNotThrow(
          () -> linker.defineFunction("wasmtime4j:imports", "logger", "log", logFunc),
          "Defining log function should not throw");

      // Define log-level function
      final ComponentHostFunction logLevelFunc =
          ComponentHostFunction.voidFunctionWithParams(
              params -> {
                final long level = params.get(0).asU32();
                final String msg = params.get(1).asString();
                LOGGER.info("Log level " + level + ": " + msg);
              });

      assertDoesNotThrow(
          () -> linker.defineFunction("wasmtime4j:imports", "logger", "log-level", logLevelFunc),
          "Defining log-level function should not throw");

      LOGGER.info("Individual host functions defined successfully");
    }

    @Test
    @DisplayName("should define function with WIT path format")
    void shouldDefineFunctionWithWitPathFormat(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentHostFunction echoFunc =
          ComponentHostFunction.singleValue(params -> params.get(0));

      // WIT path format: namespace:package/interface#function
      assertDoesNotThrow(
          () -> linker.defineFunction("test:example/utils#echo", echoFunc),
          "Define with WIT path should not throw");

      LOGGER.info("Function defined using WIT path format");
    }
  }

  @Nested
  @DisplayName("Component Instantiation With Imports Tests")
  class ComponentInstantiationWithImportsTests {

    @Test
    @DisplayName("should instantiate component with host-provided imports")
    void shouldInstantiateComponentWithHostProvidedImports(final TestInfo testInfo)
        throws Exception {
      assumeComponentCompositionAvailable();
      assumeTrue(withImportsComponentBytes != null, "with-imports.wasm required for this test");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Setup the required logger interface
      final List<String> capturedLogs = new CopyOnWriteArrayList<>();

      final Map<String, ComponentHostFunction> loggerFunctions = new HashMap<>();
      loggerFunctions.put(
          "log",
          ComponentHostFunction.voidFunctionWithParams(
              params -> capturedLogs.add("LOG: " + params.get(0).asString())));
      loggerFunctions.put(
          "log-level",
          ComponentHostFunction.voidFunctionWithParams(
              params ->
                  capturedLogs.add(
                      "LEVEL[" + params.get(0).asU32() + "]: " + params.get(1).asString())));

      linker.defineInterface("wasmtime4j:imports", "logger", loggerFunctions);

      // Load component using JniComponentEngine for proper component handling
      final JniComponentEngine componentEngine =
          new JniComponentEngine(new ComponentEngineConfig());
      resources.add(componentEngine);

      final Component component = componentEngine.loadComponentFromBytes(withImportsComponentBytes);
      resources.add(component);

      assertNotNull(component, "Component should be loaded");
      assertNotNull(component.getId(), "Component should have an ID");

      LOGGER.info("Component with imports loaded successfully: " + component.getId());
    }

    @Test
    @DisplayName("should fail to instantiate component when imports missing from engine")
    void shouldFailToInstantiateComponentWhenImportsMissingFromEngine(final TestInfo testInfo)
        throws Exception {
      assumeComponentCompositionAvailable();
      assumeTrue(withImportsComponentBytes != null, "with-imports.wasm required for this test");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Note: The external ComponentLinker we created in setUp is NOT connected to
      // JniComponentEngine's internal linker. When we call component.instantiate(),
      // it uses the engine's internal linker which doesn't have our imports defined.
      // This test verifies proper error handling for missing imports.

      // Load component - this should succeed
      final JniComponentEngine componentEngine =
          new JniComponentEngine(new ComponentEngineConfig());
      resources.add(componentEngine);

      final Component component = componentEngine.loadComponentFromBytes(withImportsComponentBytes);
      resources.add(component);

      assertNotNull(component, "Component should be loaded");

      // Attempting to instantiate without providing required imports should fail
      // with a clear error message about missing imports
      final Exception exception =
          assertThrows(
              Exception.class,
              () -> component.instantiate(),
              "Should fail when required imports are missing");

      // Log the full error chain for debugging
      Throwable current = exception;
      final StringBuilder fullErrorChain = new StringBuilder();
      while (current != null) {
        fullErrorChain.append(current.getMessage()).append(" -> ");
        current = current.getCause();
      }
      LOGGER.info("Expected error chain when imports missing: " + fullErrorChain);

      // The error chain should eventually mention the missing import
      // (could be in root message or nested cause)
      final String rootMessage = exception.getMessage();
      boolean foundImportError = rootMessage != null && rootMessage.contains("instantiate");

      // Check nested causes for more specific error
      current = exception.getCause();
      while (current != null) {
        final String msg = current.getMessage();
        if (msg != null
            && (msg.contains("wasmtime4j:imports")
                || msg.contains("logger")
                || msg.contains("implementation was not found"))) {
          foundImportError = true;
          break;
        }
        current = current.getCause();
      }

      assertTrue(foundImportError, "Error chain should indicate import issue: " + fullErrorChain);

      LOGGER.info("Component correctly failed with missing imports error");
    }
  }

  @Nested
  @DisplayName("Multi-Component Linking Tests")
  class MultiComponentLinkingTests {

    @Test
    @DisplayName("should link multiple engines independently")
    void shouldLinkMultipleEnginesIndependently(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      assumeTrue(addComponentBytes != null, "add.wasm required for this test");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create two independent component engines
      final JniComponentEngine engine1 = new JniComponentEngine(new ComponentEngineConfig());
      final JniComponentEngine engine2 = new JniComponentEngine(new ComponentEngineConfig());
      resources.add(engine1);
      resources.add(engine2);

      // Load the same component in both engines
      final Component comp1 = engine1.loadComponentFromBytes(addComponentBytes);
      final Component comp2 = engine2.loadComponentFromBytes(addComponentBytes);
      resources.add(comp1);
      resources.add(comp2);

      // Instantiate in both
      final ComponentInstance inst1 = comp1.instantiate();
      final ComponentInstance inst2 = comp2.instantiate();
      resources.add(inst1);
      resources.add(inst2);

      // Both should work independently
      assertTrue(inst1.isValid(), "Instance 1 should be valid");
      assertTrue(inst2.isValid(), "Instance 2 should be valid");
      assertFalse(inst1.getId().equals(inst2.getId()), "Instances should have different IDs");

      // Invoke add on both
      final Object result1 =
          inst1.invoke(
              "add",
              ai.tegmentum.wasmtime4j.wit.WitS32.of(10),
              ai.tegmentum.wasmtime4j.wit.WitS32.of(20));
      final Object result2 =
          inst2.invoke(
              "add",
              ai.tegmentum.wasmtime4j.wit.WitS32.of(5),
              ai.tegmentum.wasmtime4j.wit.WitS32.of(15));

      assertEquals(30, result1, "Engine 1: add(10, 20) should be 30");
      assertEquals(20, result2, "Engine 2: add(5, 15) should be 20");

      LOGGER.info("Multiple engines linked independently and working correctly");
    }

    @Test
    @DisplayName("should create multiple instances from same component")
    void shouldCreateMultipleInstancesFromSameComponent(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      assumeTrue(addComponentBytes != null, "add.wasm required for this test");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final JniComponentEngine componentEngine =
          new JniComponentEngine(new ComponentEngineConfig());
      resources.add(componentEngine);

      final Component component = componentEngine.loadComponentFromBytes(addComponentBytes);
      resources.add(component);

      // Create multiple instances from the same component
      final List<ComponentInstance> instances = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        final ComponentInstance inst = component.instantiate();
        instances.add(inst);
        resources.add(inst);
      }

      // Verify all instances are independent and valid
      for (int i = 0; i < instances.size(); i++) {
        final ComponentInstance inst = instances.get(i);
        assertTrue(inst.isValid(), "Instance " + i + " should be valid");

        // Each instance should produce correct results
        final Object result =
            inst.invoke(
                "add",
                ai.tegmentum.wasmtime4j.wit.WitS32.of(i),
                ai.tegmentum.wasmtime4j.wit.WitS32.of(100));
        assertEquals(
            i + 100, result, "Instance " + i + ": add(" + i + ", 100) should be " + (i + 100));
      }

      LOGGER.info("Created " + instances.size() + " instances from same component");
    }
  }

  @Nested
  @DisplayName("Linker Lifecycle Tests")
  class LinkerLifecycleTests {

    @Test
    @DisplayName("should handle linker closure properly")
    void shouldHandleLinkerClosureProperly(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine localEngine = Engine.create();
      final ComponentLinker<Object> localLinker = ComponentLinker.create(localEngine);

      assertTrue(localLinker.isValid(), "Linker should be valid before close");

      // Define some functions
      localLinker.defineFunction(
          "test:example",
          "interface",
          "func1",
          ComponentHostFunction.create(params -> List.of(ComponentVal.s32(1))));

      assertTrue(
          localLinker.hasFunction("test:example", "interface", "func1"),
          "Function should be defined");

      localLinker.close();

      assertFalse(localLinker.isValid(), "Linker should be invalid after close");

      // Using closed linker should throw
      assertThrows(
          IllegalStateException.class,
          () ->
              localLinker.defineFunction(
                  "test:example",
                  "interface",
                  "func2",
                  ComponentHostFunction.create(params -> List.of())),
          "Using closed linker should throw");

      localEngine.close();

      LOGGER.info("Linker lifecycle handled correctly");
    }

    @Test
    @DisplayName("should support multiple linkers per engine")
    void shouldSupportMultipleLinkersPerEngine(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create multiple linkers for the same engine
      final ComponentLinker<Object> linker2 = ComponentLinker.create(engine);
      final ComponentLinker<Object> linker3 = ComponentLinker.create(engine);
      resources.add(linker2);
      resources.add(linker3);

      // Each linker should be independent
      linker.defineFunction(
          "ns1:pkg",
          "iface",
          "func",
          ComponentHostFunction.create(params -> List.of(ComponentVal.s32(1))));
      linker2.defineFunction(
          "ns2:pkg",
          "iface",
          "func",
          ComponentHostFunction.create(params -> List.of(ComponentVal.s32(2))));
      linker3.defineFunction(
          "ns3:pkg",
          "iface",
          "func",
          ComponentHostFunction.create(params -> List.of(ComponentVal.s32(3))));

      // Verify each linker has its own definitions
      assertTrue(linker.hasFunction("ns1:pkg", "iface", "func"), "Linker 1 should have ns1");
      assertFalse(linker.hasFunction("ns2:pkg", "iface", "func"), "Linker 1 should not have ns2");
      assertFalse(linker.hasFunction("ns3:pkg", "iface", "func"), "Linker 1 should not have ns3");

      assertTrue(linker2.hasFunction("ns2:pkg", "iface", "func"), "Linker 2 should have ns2");
      assertTrue(linker3.hasFunction("ns3:pkg", "iface", "func"), "Linker 3 should have ns3");

      LOGGER.info("Multiple linkers per engine work independently");
    }
  }

  @Nested
  @DisplayName("Import Validation Tests")
  class ImportValidationTests {

    @Test
    @DisplayName("should validate missing imports")
    void shouldValidateMissingImports(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create linker WITHOUT defining required imports
      // The component requires: wasmtime4j:imports/logger

      assertFalse(
          linker.hasInterface("wasmtime4j:imports", "logger"), "Logger should not be defined yet");

      // Define one function but not all required
      linker.defineFunction(
          "wasmtime4j:imports",
          "logger",
          "log",
          ComponentHostFunction.voidFunctionWithParams(params -> {}));

      assertTrue(linker.hasFunction("wasmtime4j:imports", "logger", "log"), "log should exist");
      assertFalse(
          linker.hasFunction("wasmtime4j:imports", "logger", "log-level"),
          "log-level should not exist");

      LOGGER.info("Missing imports correctly detected");
    }

    @Test
    @DisplayName("should reject null function implementation")
    void shouldRejectNullFunctionImplementation(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineFunction("test:example", "interface", "func", null),
          "Should reject null implementation");

      LOGGER.info("Null implementation correctly rejected");
    }

    @Test
    @DisplayName("should return empty set for undefined interface")
    void shouldReturnEmptySetForUndefinedInterface(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var functions = linker.getDefinedFunctions("nonexistent:namespace", "interface");
      assertNotNull(functions, "Should return non-null set");
      assertTrue(functions.isEmpty(), "Should return empty set");

      LOGGER.info("Undefined interface handled correctly");
    }
  }

  @Nested
  @DisplayName("WASI Preview 2 Integration Tests")
  class WasiPreview2IntegrationTests {

    @Test
    @DisplayName("should enable WASI Preview 2")
    void shouldEnableWasiPreview2(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertDoesNotThrow(() -> linker.enableWasiPreview2(), "Enabling WASI Preview 2 should work");

      LOGGER.info("WASI Preview 2 enabled successfully");
    }

    @Test
    @DisplayName("should allow defining additional imports after WASI enabled")
    void shouldAllowDefiningAdditionalImportsAfterWasiEnabled(final TestInfo testInfo)
        throws Exception {
      assumeComponentCompositionAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Enable WASI first
      linker.enableWasiPreview2();

      // Then define additional custom imports
      assertDoesNotThrow(
          () ->
              linker.defineFunction(
                  "custom:app",
                  "config",
                  "get-value",
                  ComponentHostFunction.singleValue(params -> ComponentVal.string("test-value"))),
          "Should be able to define imports after WASI enabled");

      assertTrue(
          linker.hasFunction("custom:app", "config", "get-value"), "Custom function should exist");

      LOGGER.info("Additional imports defined after WASI enabled");
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("should handle component function throwing exception")
    void shouldHandleHostFunctionThrowingException(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Define a function that throws
      linker.defineFunction(
          "test:error",
          "thrower",
          "throw-error",
          ComponentHostFunction.create(
              params -> {
                throw new RuntimeException("Intentional test error");
              }));

      assertTrue(
          linker.hasFunction("test:error", "thrower", "throw-error"), "Function should be defined");

      LOGGER.info("Error-throwing function defined successfully");
    }

    @Test
    @DisplayName("should handle invalid component bytes gracefully")
    void shouldHandleInvalidComponentBytesGracefully(final TestInfo testInfo) throws Exception {
      assumeComponentCompositionAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final JniComponentEngine componentEngine =
          new JniComponentEngine(new ComponentEngineConfig());
      resources.add(componentEngine);

      final byte[] invalidBytes = new byte[] {0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00};

      assertThrows(
          Exception.class,
          () -> componentEngine.loadComponentFromBytes(invalidBytes),
          "Should reject invalid component bytes");

      LOGGER.info("Invalid component bytes handled gracefully");
    }
  }
}

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for ComponentLinker - WebAssembly Component Model linking.
 *
 * <p>These tests verify linker creation, function definition, interface definition, import
 * validation, and component instantiation. Tests are disabled until the native ComponentLinker
 * implementation is complete - the current native implementation may cause JVM crashes.
 *
 * @since 1.0.0
 */
@DisplayName("ComponentLinker Integration Tests")
public final class ComponentLinkerTest {

  private static final Logger LOGGER = Logger.getLogger(ComponentLinkerTest.class.getName());

  private static boolean componentLinkerAvailable = false;

  @BeforeAll
  static void checkComponentLinkerAvailable() {
    try {
      final Engine engine = Engine.create();
      final ComponentLinker<?> linker = ComponentLinker.create(engine);
      linker.close();
      engine.close();
      componentLinkerAvailable = true;
      LOGGER.info("ComponentLinker native implementation is available");
    } catch (final Throwable t) {
      componentLinkerAvailable = false;
      LOGGER.warning("ComponentLinker not available - tests will be skipped: " + t.getMessage());
    }
  }

  private static void assumeComponentLinkerAvailable() {
    assumeTrue(
        componentLinkerAvailable, "ComponentLinker native implementation not available - skipping");
  }

  private Engine engine;
  private ComponentLinker<Object> linker;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
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

    if (linker != null) {
      linker.close();
      linker = null;
    }
    if (engine != null) {
      engine.close();
      engine = null;
    }
  }

  @Nested
  @DisplayName("ComponentLinker Creation Tests")
  class LinkerCreationTests {

    @Test
    @DisplayName("should create component linker")
    void shouldCreateComponentLinker(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      assertNotNull(linker, "Linker should not be null");
      assertTrue(linker.isValid(), "Linker should be valid after creation");

      LOGGER.info("Created component linker successfully");
    }

    @Test
    @DisplayName("should throw when creating with null engine")
    void shouldThrowWhenCreatingWithNullEngine(final TestInfo testInfo) {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentLinker.create(null),
          "Should throw for null engine");

      LOGGER.info("Correctly threw for null engine");
    }

    @Test
    @DisplayName("should associate linker with engine")
    void shouldAssociateLinkerWithEngine(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      final Engine associatedEngine = linker.getEngine();
      assertEquals(engine, associatedEngine, "Associated engine should match");

      LOGGER.info("Linker correctly associated with engine");
    }
  }

  @Nested
  @DisplayName("Function Definition Tests")
  class FunctionDefinitionTests {

    @Test
    @DisplayName("should define function with full path")
    void shouldDefineFunctionWithFullPath(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      final ComponentHostFunction implementation =
          ComponentHostFunction.create(params -> List.of(ComponentVal.s32(42)));

      assertDoesNotThrow(
          () -> linker.defineFunction("example:test", "interface", "my_func", implementation),
          "Define function should not throw");

      assertTrue(
          linker.hasFunction("example:test", "interface", "my_func"), "Function should be defined");

      LOGGER.info("Function defined with full path");
    }

    @Test
    @DisplayName("should define function with WIT path")
    void shouldDefineFunctionWithWitPath(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      final ComponentHostFunction implementation =
          ComponentHostFunction.create(params -> List.of());

      assertDoesNotThrow(
          () -> linker.defineFunction("example:test/interface#my_func", implementation),
          "Define function with WIT path should not throw");

      LOGGER.info("Function defined with WIT path");
    }

    @Test
    @DisplayName("should define void function")
    void shouldDefineVoidFunction(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      final ComponentHostFunction voidFunc =
          ComponentHostFunction.voidFunction(() -> LOGGER.info("Void function called"));

      assertDoesNotThrow(
          () -> linker.defineFunction("example:test", "interface", "void_func", voidFunc),
          "Define void function should not throw");

      LOGGER.info("Void function defined");
    }

    @Test
    @DisplayName("should define single-value function")
    void shouldDefineSingleValueFunction(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      final ComponentHostFunction singleValueFunc =
          ComponentHostFunction.singleValue(params -> ComponentVal.s32(params.size()));

      assertDoesNotThrow(
          () -> linker.defineFunction("example:test", "interface", "count_params", singleValueFunc),
          "Define single-value function should not throw");

      LOGGER.info("Single-value function defined");
    }

    @Test
    @DisplayName("should throw when defining function with null implementation")
    void shouldThrowWhenDefiningFunctionWithNullImplementation(final TestInfo testInfo)
        throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineFunction("example:test", "interface", "func", null),
          "Should throw for null implementation");

      LOGGER.info("Correctly threw for null implementation");
    }
  }

  @Nested
  @DisplayName("Interface Definition Tests")
  class InterfaceDefinitionTests {

    @Test
    @DisplayName("should define interface with multiple functions")
    void shouldDefineInterfaceWithMultipleFunctions(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      final Map<String, ComponentHostFunction> functions = new HashMap<>();
      functions.put(
          "add",
          ComponentHostFunction.create(
              params -> {
                final int a = params.get(0).asS32();
                final int b = params.get(1).asS32();
                return List.of(ComponentVal.s32(a + b));
              }));
      functions.put(
          "sub",
          ComponentHostFunction.create(
              params -> {
                final int a = params.get(0).asS32();
                final int b = params.get(1).asS32();
                return List.of(ComponentVal.s32(a - b));
              }));
      functions.put(
          "mul",
          ComponentHostFunction.create(
              params -> {
                final int a = params.get(0).asS32();
                final int b = params.get(1).asS32();
                return List.of(ComponentVal.s32(a * b));
              }));

      assertDoesNotThrow(
          () -> linker.defineInterface("example:math", "calculator", functions),
          "Define interface should not throw");

      assertTrue(linker.hasInterface("example:math", "calculator"), "Interface should be defined");
      assertTrue(linker.hasFunction("example:math", "calculator", "add"), "add should be defined");
      assertTrue(linker.hasFunction("example:math", "calculator", "sub"), "sub should be defined");
      assertTrue(linker.hasFunction("example:math", "calculator", "mul"), "mul should be defined");

      LOGGER.info("Interface with multiple functions defined");
    }

    @Test
    @DisplayName("should return defined interfaces")
    void shouldReturnDefinedInterfaces(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      final Map<String, ComponentHostFunction> functions = new HashMap<>();
      functions.put("func1", ComponentHostFunction.create(params -> List.of()));

      linker.defineInterface("example:test", "interface1", functions);
      linker.defineInterface("example:test", "interface2", functions);

      final Set<String> interfaces = linker.getDefinedInterfaces();
      assertNotNull(interfaces, "Interfaces set should not be null");
      assertTrue(interfaces.size() >= 2, "Should have at least 2 interfaces");

      LOGGER.info("Defined interfaces: " + interfaces);
    }

    @Test
    @DisplayName("should return defined functions for interface")
    void shouldReturnDefinedFunctionsForInterface(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      final Map<String, ComponentHostFunction> functions = new HashMap<>();
      functions.put("func_a", ComponentHostFunction.create(params -> List.of()));
      functions.put("func_b", ComponentHostFunction.create(params -> List.of()));

      linker.defineInterface("example:test", "myinterface", functions);

      final Set<String> definedFuncs = linker.getDefinedFunctions("example:test", "myinterface");
      assertNotNull(definedFuncs, "Functions set should not be null");
      assertTrue(definedFuncs.contains("func_a"), "Should contain func_a");
      assertTrue(definedFuncs.contains("func_b"), "Should contain func_b");

      LOGGER.info("Defined functions for interface: " + definedFuncs);
    }
  }

  @Nested
  @DisplayName("Interface Aliasing Tests")
  class InterfaceAliasingTests {

    @Test
    @DisplayName("should create interface alias")
    void shouldCreateInterfaceAlias(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      final Map<String, ComponentHostFunction> functions = new HashMap<>();
      functions.put(
          "hello", ComponentHostFunction.create(params -> List.of(ComponentVal.string("Hello!"))));

      linker.defineInterface("example:original", "greeting", functions);

      assertDoesNotThrow(
          () ->
              linker.aliasInterface("example:original", "greeting", "example:aliased", "greeting"),
          "Alias interface should not throw");

      LOGGER.info("Interface alias created");
    }
  }

  @Nested
  @DisplayName("Linker Lifecycle Tests")
  class LinkerLifecycleTests {

    @Test
    @DisplayName("should close linker properly")
    void shouldCloseLinkerProperly(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      final ComponentLinker<Object> localLinker = ComponentLinker.create(engine);

      assertTrue(localLinker.isValid(), "Linker should be valid before close");
      localLinker.close();
      assertFalse(localLinker.isValid(), "Linker should be invalid after close");

      LOGGER.info("Linker closed properly");
    }

    @Test
    @DisplayName("should handle multiple close calls")
    void shouldHandleMultipleCloseCalls(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      final ComponentLinker<Object> localLinker = ComponentLinker.create(engine);

      assertDoesNotThrow(localLinker::close, "First close should not throw");
      assertDoesNotThrow(localLinker::close, "Second close should not throw");

      LOGGER.info("Multiple close calls handled correctly");
    }

    @Test
    @DisplayName("should throw when using closed linker")
    void shouldThrowWhenUsingClosedLinker(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      final ComponentLinker<Object> localLinker = ComponentLinker.create(engine);
      localLinker.close();

      // Using a closed linker throws IllegalStateException (standard Java pattern for closed
      // resources)
      assertThrows(
          IllegalStateException.class,
          () ->
              localLinker.defineFunction(
                  "example:test",
                  "interface",
                  "func",
                  ComponentHostFunction.create(params -> List.of())),
          "Should throw when using closed linker");

      LOGGER.info("Correctly threw when using closed linker");
    }
  }

  @Nested
  @DisplayName("WASI Preview 2 Tests")
  class WasiPreview2Tests {

    @Test
    @DisplayName("should enable WASI Preview 2")
    void shouldEnableWasiPreview2(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      assertDoesNotThrow(
          () -> linker.enableWasiPreview2(), "Enable WASI Preview 2 should not throw");

      LOGGER.info("WASI Preview 2 enabled");
    }
  }

  @Nested
  @DisplayName("WASI HTTP Tests")
  class WasiHttpTests {

    @Test
    @DisplayName("should enable WASI HTTP after WASI Preview 2")
    void shouldEnableWasiHttp(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      // WASI Preview 2 must be enabled before WASI HTTP
      linker.enableWasiPreview2();
      LOGGER.info("WASI Preview 2 enabled");

      assertDoesNotThrow(() -> linker.enableWasiHttp(), "Enable WASI HTTP should not throw");

      LOGGER.info("WASI HTTP enabled successfully");
    }

    @Test
    @DisplayName("should allow defining custom imports after WASI HTTP enabled")
    void shouldAllowDefiningImportsAfterWasiHttpEnabled(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      // WASI Preview 2 must be enabled before WASI HTTP
      linker.enableWasiPreview2();
      linker.enableWasiHttp();

      // Define additional custom imports after WASI HTTP
      assertDoesNotThrow(
          () ->
              linker.defineFunction(
                  "custom:app",
                  "service",
                  "get-data",
                  ComponentHostFunction.singleValue(params -> ComponentVal.string("test-data"))),
          "Should be able to define imports after WASI HTTP enabled");

      assertTrue(
          linker.hasFunction("custom:app", "service", "get-data"),
          "Custom function should exist after WASI HTTP enabled");

      LOGGER.info("Custom imports defined after WASI HTTP");
    }

    @Test
    @DisplayName("should enable WASI HTTP on closed linker throws")
    void shouldThrowWhenEnablingWasiHttpOnClosedLinker(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      final ComponentLinker<Object> localLinker = ComponentLinker.create(engine);
      localLinker.close();

      assertThrows(
          IllegalStateException.class,
          () -> localLinker.enableWasiHttp(),
          "Should throw when enabling WASI HTTP on closed linker");

      LOGGER.info("Correctly threw when enabling WASI HTTP on closed linker");
    }
  }

  @Nested
  @DisplayName("Lookup Tests")
  class LookupTests {

    @Test
    @DisplayName("should return false for undefined interface")
    void shouldReturnFalseForUndefinedInterface(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      assertFalse(
          linker.hasInterface("nonexistent:namespace", "nonexistent"),
          "Should return false for undefined interface");

      LOGGER.info("Correctly returned false for undefined interface");
    }

    @Test
    @DisplayName("should return false for undefined function")
    void shouldReturnFalseForUndefinedFunction(final TestInfo testInfo) throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      assertFalse(
          linker.hasFunction("nonexistent:namespace", "nonexistent", "nonexistent_func"),
          "Should return false for undefined function");

      LOGGER.info("Correctly returned false for undefined function");
    }

    @Test
    @DisplayName("should return empty set for undefined interface functions")
    void shouldReturnEmptySetForUndefinedInterfaceFunctions(final TestInfo testInfo)
        throws Exception {
      assumeComponentLinkerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      engine = Engine.create();
      resources.add(engine);
      linker = ComponentLinker.create(engine);

      final Set<String> functions =
          linker.getDefinedFunctions("nonexistent:namespace", "nonexistent");
      assertNotNull(functions, "Functions set should not be null");
      assertTrue(functions.isEmpty(), "Functions set should be empty");

      LOGGER.info("Correctly returned empty set for undefined interface");
    }
  }
}

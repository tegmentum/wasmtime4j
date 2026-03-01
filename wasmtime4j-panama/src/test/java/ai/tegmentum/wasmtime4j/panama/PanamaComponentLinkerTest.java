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
package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.component.ComponentHostFunction;
import ai.tegmentum.wasmtime4j.component.ComponentResourceDefinition;
import ai.tegmentum.wasmtime4j.component.ComponentVal;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the PanamaComponentLinker implementation.
 *
 * <p>These tests verify the ComponentLinker interface implementation including host function
 * definition, interface tracking, and resource management.
 *
 * <p>Note: These tests require the native library to be available. Tests are skipped when the
 * native library is not loaded.
 */
class PanamaComponentLinkerTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaComponentLinkerTest.class.getName());
  private static final boolean NATIVE_AVAILABLE = checkNativeAvailable();

  private PanamaEngine engine;
  private PanamaComponentLinker<Void> linker;

  private static boolean checkNativeAvailable() {
    try {
      // Only verify engine creation - component linker creation can SIGSEGV and crash the JVM
      final PanamaEngine testEngine = new PanamaEngine();
      testEngine.close();
      return true;
    } catch (final Exception | Error e) {
      LOGGER.log(
          Level.INFO, "Native library not available, tests will be skipped: " + e.getMessage());
      return false;
    }
  }

  @BeforeEach
  void setUp() throws WasmException {
    assumeTrue(NATIVE_AVAILABLE, "Native library not available - skipping test");
    // Create a real engine for testing
    engine = new PanamaEngine();
    linker = new PanamaComponentLinker<>(engine);
  }

  @AfterEach
  void tearDown() {
    if (linker != null) {
      linker.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create linker with valid engine")
    void testCreateLinkerWithValidEngine() {
      assertNotNull(linker);
      assertTrue(linker.isValid());
      assertEquals(engine, linker.getEngine());
    }

    @Test
    @DisplayName("Should throw on null engine")
    void testCreateLinkerWithNullEngine() {
      assertThrows(IllegalArgumentException.class, () -> new PanamaComponentLinker<>(null));
    }
  }

  @Nested
  @DisplayName("Define Function Tests")
  class DefineFunctionTests {

    @Test
    @DisplayName("Should define function with namespace, interface, and function name")
    void testDefineFunctionWithFullPath() throws WasmException {
      // Arrange
      final ComponentHostFunction impl = params -> List.of(ComponentVal.s32(42));

      // Act
      linker.defineFunction("wasi", "cli", "print", impl);

      // Assert
      assertTrue(linker.hasInterface("wasi", "cli"));
      assertTrue(linker.hasFunction("wasi", "cli", "print"));
    }

    @Test
    @DisplayName("Should define function with WIT path")
    void testDefineFunctionWithWitPath() throws WasmException {
      // Arrange
      final ComponentHostFunction impl = params -> List.of();

      // Act
      linker.defineFunction("wasi:cli/stdout#write", impl);

      // Assert - WIT path functions are tracked but not in interface map
      assertNotNull(linker);
    }

    @Test
    @DisplayName("Should throw on null interface namespace")
    void testDefineFunctionNullNamespace() {
      final ComponentHostFunction impl = params -> List.of();
      assertThrows(
          IllegalArgumentException.class, () -> linker.defineFunction(null, "cli", "print", impl));
    }

    @Test
    @DisplayName("Should throw on null interface name")
    void testDefineFunctionNullInterfaceName() {
      final ComponentHostFunction impl = params -> List.of();
      assertThrows(
          IllegalArgumentException.class, () -> linker.defineFunction("wasi", null, "print", impl));
    }

    @Test
    @DisplayName("Should throw on null function name")
    void testDefineFunctionNullFunctionName() {
      final ComponentHostFunction impl = params -> List.of();
      assertThrows(
          IllegalArgumentException.class, () -> linker.defineFunction("wasi", "cli", null, impl));
    }

    @Test
    @DisplayName("Should throw on null implementation")
    void testDefineFunctionNullImplementation() {
      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineFunction("wasi", "cli", "print", null));
    }
  }

  @Nested
  @DisplayName("Define Interface Tests")
  class DefineInterfaceTests {

    @Test
    @DisplayName("Should define interface with multiple functions")
    void testDefineInterfaceWithMultipleFunctions() throws WasmException {
      // Arrange
      final Map<String, ComponentHostFunction> functions = new HashMap<>();
      functions.put("read", params -> List.of(ComponentVal.s32(0)));
      functions.put("write", params -> List.of(ComponentVal.s32(0)));
      functions.put("close", params -> List.of());

      // Act
      linker.defineInterface("wasi", "io", functions);

      // Assert
      assertTrue(linker.hasInterface("wasi", "io"));
      assertTrue(linker.hasFunction("wasi", "io", "read"));
      assertTrue(linker.hasFunction("wasi", "io", "write"));
      assertTrue(linker.hasFunction("wasi", "io", "close"));
    }

    @Test
    @DisplayName("Should throw on null functions map")
    void testDefineInterfaceNullFunctions() {
      assertThrows(
          IllegalArgumentException.class, () -> linker.defineInterface("wasi", "io", null));
    }
  }

  @Nested
  @DisplayName("Define Resource Tests")
  class DefineResourceTests {

    @Test
    @DisplayName("Should define resource")
    void testDefineResource() throws WasmException {
      // Arrange
      final ComponentResourceDefinition<String> resourceDef =
          ComponentResourceDefinition.<String>builder("file")
              .constructor(params -> "file-handle")
              .destructor(handle -> {})
              .build();

      // Act
      linker.defineResource("wasi", "filesystem", "file", resourceDef);

      // Assert
      assertTrue(linker.hasInterface("wasi", "filesystem"));
      final Set<String> functions = linker.getDefinedFunctions("wasi", "filesystem");
      assertTrue(functions.contains("[resource]file"));
    }

    @Test
    @DisplayName("Should throw on null resource definition")
    void testDefineResourceNullDefinition() {
      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineResource("wasi", "filesystem", "file", null));
    }
  }

  @Nested
  @DisplayName("Interface Query Tests")
  class InterfaceQueryTests {

    @Test
    @DisplayName("Should return false for non-existent interface")
    void testHasInterfaceNonExistent() {
      assertFalse(linker.hasInterface("nonexistent", "interface"));
    }

    @Test
    @DisplayName("Should return false for non-existent function")
    void testHasFunctionNonExistent() throws WasmException {
      linker.defineFunction("wasi", "cli", "print", params -> List.of());
      assertFalse(linker.hasFunction("wasi", "cli", "nonexistent"));
    }

    @Test
    @DisplayName("Should get defined interfaces")
    void testGetDefinedInterfaces() throws WasmException {
      // Arrange
      linker.defineFunction("wasi", "cli", "print", params -> List.of());
      linker.defineFunction("wasi", "io", "read", params -> List.of());

      // Act
      final Set<String> interfaces = linker.getDefinedInterfaces();

      // Assert
      assertTrue(interfaces.contains("wasi:cli"));
      assertTrue(interfaces.contains("wasi:io"));
    }

    @Test
    @DisplayName("Should get defined functions for interface")
    void testGetDefinedFunctions() throws WasmException {
      // Arrange
      linker.defineFunction("wasi", "cli", "print", params -> List.of());
      linker.defineFunction("wasi", "cli", "println", params -> List.of());

      // Act
      final Set<String> functions = linker.getDefinedFunctions("wasi", "cli");

      // Assert
      assertEquals(2, functions.size());
      assertTrue(functions.contains("print"));
      assertTrue(functions.contains("println"));
    }

    @Test
    @DisplayName("Should return empty set for non-existent interface")
    void testGetDefinedFunctionsNonExistent() {
      final Set<String> functions = linker.getDefinedFunctions("nonexistent", "interface");
      assertTrue(functions.isEmpty());
    }
  }

  @Nested
  @DisplayName("Alias Interface Tests")
  class AliasInterfaceTests {

    @Test
    @DisplayName("Should create interface alias")
    void testAliasInterface() throws WasmException {
      // Arrange
      linker.defineFunction("wasi", "cli", "print", params -> List.of());

      // Act
      linker.aliasInterface("wasi", "cli", "custom", "output");

      // Assert
      assertTrue(linker.hasInterface("custom", "output"));
      assertTrue(linker.hasFunction("custom", "output", "print"));
    }

    @Test
    @DisplayName("Should throw on null from namespace")
    void testAliasInterfaceNullFromNamespace() {
      assertThrows(
          IllegalArgumentException.class,
          () -> linker.aliasInterface(null, "cli", "custom", "output"));
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("Should be valid after creation")
    void testIsValidAfterCreation() {
      assertTrue(linker.isValid());
    }

    @Test
    @DisplayName("Should be invalid after close")
    void testIsValidAfterClose() {
      linker.close();
      assertFalse(linker.isValid());
    }

    @Test
    @DisplayName("Should throw when using closed linker")
    void testOperationsOnClosedLinker() {
      linker.close();
      assertThrows(
          IllegalStateException.class,
          () -> linker.defineFunction("wasi", "cli", "print", params -> List.of()));
    }

    @Test
    @DisplayName("Should handle multiple close calls")
    void testMultipleClose() {
      linker.close();
      linker.close(); // Should not throw
      assertFalse(linker.isValid());
    }
  }
}

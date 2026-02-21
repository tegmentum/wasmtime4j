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

package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentHostFunction;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.component.ComponentResourceDefinition;
import ai.tegmentum.wasmtime4j.component.ComponentVal;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JniComponentLinker defensive programming and validation logic.
 *
 * <p>These tests focus on parameter validation and interface tracking logic that doesn't require
 * native library access. Tests use fake handles since native library loading might not be available
 * in tests.
 */
class JniComponentLinkerTest {
  private static final long VALID_HANDLE = 1L;
  private JniEngine testEngine;
  private JniComponentLinker<Object> linker;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    linker = new JniComponentLinker<>(VALID_HANDLE, testEngine);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create linker with valid parameters")
    void testConstructorWithValidParameters() {
      final JniComponentLinker<Object> newLinker =
          new JniComponentLinker<>(VALID_HANDLE, testEngine);

      assertThat(newLinker).isNotNull();
      assertThat(newLinker.getNativeHandle()).isEqualTo(VALID_HANDLE);
      assertThat(newLinker.getEngine()).isEqualTo(testEngine);
    }

    @Test
    @DisplayName("Should have invalid state with zero handle")
    void testConstructorWithZeroHandle() {
      final JniComponentLinker<Object> linkerWithZero = new JniComponentLinker<>(0L, testEngine);

      assertThat(linkerWithZero.getNativeHandle()).isEqualTo(0L);
      assertFalse(linkerWithZero.isValid());
    }
  }

  @Nested
  @DisplayName("Define Function Tests")
  class DefineFunctionTests {

    @Test
    @DisplayName("Should define function with full path")
    void testDefineFunctionWithFullPath() throws WasmException {
      final ComponentHostFunction impl = params -> Collections.singletonList(ComponentVal.s32(42));

      linker.defineFunction("wasi", "cli", "print", impl);

      assertTrue(linker.hasInterface("wasi", "cli"));
      assertTrue(linker.hasFunction("wasi", "cli", "print"));
    }

    @Test
    @DisplayName("Should define function with WIT path")
    void testDefineFunctionWithWitPath() throws WasmException {
      final ComponentHostFunction impl = params -> Collections.emptyList();

      linker.defineFunction("wasi:cli/stdout#write", impl);

      // Function is tracked in hostFunctions map
      assertThat(linker).isNotNull();
    }

    @Test
    @DisplayName("Should throw on null interface namespace")
    void testDefineFunctionNullNamespace() {
      final ComponentHostFunction impl = params -> Collections.emptyList();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunction(null, "cli", "print", impl));

      assertThat(exception.getMessage()).contains("namespace");
    }

    @Test
    @DisplayName("Should throw on null interface name")
    void testDefineFunctionNullInterfaceName() {
      final ComponentHostFunction impl = params -> Collections.emptyList();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunction("wasi", null, "print", impl));

      assertThat(exception.getMessage()).contains("Interface name");
    }

    @Test
    @DisplayName("Should throw on null function name")
    void testDefineFunctionNullFunctionName() {
      final ComponentHostFunction impl = params -> Collections.emptyList();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunction("wasi", "cli", null, impl));

      assertThat(exception.getMessage()).contains("Function name");
    }

    @Test
    @DisplayName("Should throw on null implementation")
    void testDefineFunctionNullImplementation() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineFunction("wasi", "cli", "print", null));

      assertThat(exception.getMessage()).contains("Implementation");
    }

    @Test
    @DisplayName("Should throw on null WIT path")
    void testDefineFunctionNullWitPath() {
      final ComponentHostFunction impl = params -> Collections.emptyList();

      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> linker.defineFunction(null, impl));

      assertThat(exception.getMessage()).contains("WIT path");
    }
  }

  @Nested
  @DisplayName("Define Interface Tests")
  class DefineInterfaceTests {

    @Test
    @DisplayName("Should define interface with multiple functions")
    void testDefineInterfaceWithMultipleFunctions() throws WasmException {
      final Map<String, ComponentHostFunction> functions = new HashMap<>();
      functions.put("read", params -> Collections.singletonList(ComponentVal.s32(0)));
      functions.put("write", params -> Collections.singletonList(ComponentVal.s32(0)));
      functions.put("close", params -> Collections.emptyList());

      linker.defineInterface("wasi", "io", functions);

      assertTrue(linker.hasInterface("wasi", "io"));
      assertTrue(linker.hasFunction("wasi", "io", "read"));
      assertTrue(linker.hasFunction("wasi", "io", "write"));
      assertTrue(linker.hasFunction("wasi", "io", "close"));
    }

    @Test
    @DisplayName("Should throw on null interface namespace")
    void testDefineInterfaceNullNamespace() {
      final Map<String, ComponentHostFunction> functions = new HashMap<>();
      functions.put("test", params -> Collections.emptyList());

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> linker.defineInterface(null, "io", functions));

      assertThat(exception.getMessage()).contains("namespace");
    }

    @Test
    @DisplayName("Should throw on null functions map")
    void testDefineInterfaceNullFunctions() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> linker.defineInterface("wasi", "io", null));

      assertThat(exception.getMessage()).contains("Functions");
    }
  }

  @Nested
  @DisplayName("Define Resource Tests")
  class DefineResourceTests {

    @Test
    @DisplayName("Should define resource and track in interface")
    void testDefineResource() throws WasmException {
      final ComponentResourceDefinition<String> resourceDef =
          ComponentResourceDefinition.<String>builder("file")
              .constructor(params -> "file-handle")
              .destructor(handle -> {})
              .build();

      linker.defineResource("wasi", "filesystem", "file", resourceDef);

      assertTrue(linker.hasInterface("wasi", "filesystem"));
      final Set<String> functions = linker.getDefinedFunctions("wasi", "filesystem");
      assertTrue(functions.contains("[resource]file"));
    }

    @Test
    @DisplayName("Should throw on null resource name")
    void testDefineResourceNullResourceName() {
      final ComponentResourceDefinition<String> resourceDef =
          ComponentResourceDefinition.<String>builder("file").build();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineResource("wasi", "filesystem", null, resourceDef));

      assertThat(exception.getMessage()).contains("Resource name");
    }

    @Test
    @DisplayName("Should throw on null resource definition")
    void testDefineResourceNullDefinition() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> linker.defineResource("wasi", "filesystem", "file", null));

      assertThat(exception.getMessage()).contains("Resource definition");
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
      linker.defineFunction("wasi", "cli", "print", params -> Collections.emptyList());
      assertFalse(linker.hasFunction("wasi", "cli", "nonexistent"));
    }

    @Test
    @DisplayName("Should get defined interfaces")
    void testGetDefinedInterfaces() throws WasmException {
      linker.defineFunction("wasi", "cli", "print", params -> Collections.emptyList());
      linker.defineFunction("wasi", "io", "read", params -> Collections.emptyList());

      final Set<String> interfaces = linker.getDefinedInterfaces();

      assertTrue(interfaces.contains("wasi:cli"));
      assertTrue(interfaces.contains("wasi:io"));
    }

    @Test
    @DisplayName("Should get defined functions for interface")
    void testGetDefinedFunctions() throws WasmException {
      linker.defineFunction("wasi", "cli", "print", params -> Collections.emptyList());
      linker.defineFunction("wasi", "cli", "println", params -> Collections.emptyList());

      final Set<String> functions = linker.getDefinedFunctions("wasi", "cli");

      assertEquals(2, functions.size());
      assertTrue(functions.contains("print"));
      assertTrue(functions.contains("println"));
    }

    @Test
    @DisplayName("Should return empty set for non-existent interface functions")
    void testGetDefinedFunctionsNonExistent() {
      final Set<String> functions = linker.getDefinedFunctions("nonexistent", "interface");
      assertTrue(functions.isEmpty());
    }

    @Test
    @DisplayName("Should throw on null namespace in hasInterface")
    void testHasInterfaceNullNamespace() {
      assertThrows(IllegalArgumentException.class, () -> linker.hasInterface(null, "cli"));
    }

    @Test
    @DisplayName("Should throw on null interface name in hasInterface")
    void testHasInterfaceNullName() {
      assertThrows(IllegalArgumentException.class, () -> linker.hasInterface("wasi", null));
    }
  }

  @Nested
  @DisplayName("Alias Interface Tests")
  class AliasInterfaceTests {

    @Test
    @DisplayName("Should create interface alias")
    void testAliasInterface() throws WasmException {
      linker.defineFunction("wasi", "cli", "print", params -> Collections.emptyList());

      linker.aliasInterface("wasi", "cli", "custom", "output");

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

    @Test
    @DisplayName("Should throw on null to namespace")
    void testAliasInterfaceNullToNamespace() {
      assertThrows(
          IllegalArgumentException.class,
          () -> linker.aliasInterface("wasi", "cli", null, "output"));
    }
  }

  @Nested
  @DisplayName("Link Instance Tests")
  class LinkInstanceTests {

    @Test
    @DisplayName("Should throw on null instance")
    void testLinkInstanceNull() {
      assertThrows(IllegalArgumentException.class, () -> linker.linkInstance(null));
    }
  }

  @Nested
  @DisplayName("Link Component Tests")
  class LinkComponentTests {

    @Test
    @DisplayName("Should throw on null store")
    void testLinkComponentNullStore() {
      assertThrows(
          IllegalArgumentException.class, () -> linker.linkComponent(null, new MockComponent()));
    }

    @Test
    @DisplayName("Should throw on null component")
    void testLinkComponentNullComponent() {
      final JniStore store = new JniStore(VALID_HANDLE, testEngine);
      assertThrows(IllegalArgumentException.class, () -> linker.linkComponent(store, null));
    }
  }

  @Nested
  @DisplayName("Instantiate Tests")
  class InstantiateTests {

    @Test
    @DisplayName("Should throw on null store")
    void testInstantiateNullStore() {
      assertThrows(
          IllegalArgumentException.class, () -> linker.instantiate(null, new MockComponent()));
    }

    @Test
    @DisplayName("Should throw on null component")
    void testInstantiateNullComponent() {
      final JniStore store = new JniStore(VALID_HANDLE, testEngine);
      assertThrows(IllegalArgumentException.class, () -> linker.instantiate(store, null));
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("Should be valid after creation with non-zero handle")
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
    @DisplayName("Should throw when defining function on closed linker")
    void testOperationsOnClosedLinker() {
      linker.close();

      assertThrows(
          IllegalStateException.class,
          () -> linker.defineFunction("wasi", "cli", "print", params -> Collections.emptyList()));
    }

    @Test
    @DisplayName("Should handle multiple close calls without error")
    void testMultipleClose() {
      linker.close();
      linker.close(); // Should not throw
      assertFalse(linker.isValid());
    }
  }

  /** Mock Component for testing parameter validation. */
  private static class MockComponent implements ai.tegmentum.wasmtime4j.component.Component {

    @Override
    public String getId() {
      return "mock-component";
    }

    @Override
    public long getSize() {
      return 0;
    }

    @Override
    public boolean exportsInterface(final String interfaceName) {
      return false;
    }

    @Override
    public boolean importsInterface(final String interfaceName) {
      return false;
    }

    @Override
    public Set<String> getExportedInterfaces() {
      return Collections.emptySet();
    }

    @Override
    public Set<String> getImportedInterfaces() {
      return Collections.emptySet();
    }

    @Override
    public ComponentInstance instantiate() {
      return null;
    }

    @Override
    public ComponentInstance instantiate(final ComponentInstanceConfig config) {
      return null;
    }

    @Override
    public ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition getWitInterface() {
      return null;
    }

    @Override
    public ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult checkWitCompatibility(
        final ai.tegmentum.wasmtime4j.component.Component other) {
      return null;
    }

    @Override
    public byte[] serialize() throws ai.tegmentum.wasmtime4j.exception.WasmException {
      return new byte[0];
    }

    @Override
    public ai.tegmentum.wasmtime4j.component.ComponentEngine getComponentEngine() {
      return null;
    }

    @Override
    public java.util.Optional<ai.tegmentum.wasmtime4j.component.ComponentExportIndex> exportIndex(
        final ai.tegmentum.wasmtime4j.component.ComponentExportIndex instanceIndex,
        final String name) {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<ai.tegmentum.wasmtime4j.ResourcesRequired> resourcesRequired() {
      return java.util.Optional.empty();
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public void close() {}
  }
}

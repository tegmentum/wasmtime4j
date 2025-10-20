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
import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JniLinker defensive programming and validation logic.
 *
 * <p>These tests focus on parameter validation that occurs before native calls. Tests avoid complex
 * mock objects that require matching current interface signatures.
 */
class JniLinkerTest {
  private static final long VALID_HANDLE = 1L;
  private JniEngine testEngine;
  private JniStore testStore;
  private JniLinker<Object> linker;
  private FunctionType testFunctionType;
  private HostFunction testImplementation;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
    linker = new JniLinker<>(VALID_HANDLE, testEngine);
    testFunctionType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
    testImplementation = params -> null;
  }

  // Constructor tests

  @Test
  void testConstructorWithValidParameters() {
    final JniLinker<Object> newLinker = new JniLinker<>(VALID_HANDLE, testEngine);

    assertThat(newLinker).isNotNull();
    assertThat(newLinker.getNativeHandle()).isEqualTo(VALID_HANDLE);
    assertThat(newLinker.getEngine()).isEqualTo(testEngine);
  }

  @Test
  void testConstructorWithZeroHandle() {
    final JniLinker<Object> linkerWithZero = new JniLinker<>(0L, testEngine);

    assertThat(linkerWithZero.getNativeHandle()).isEqualTo(0L);
    assertFalse(linkerWithZero.isValid());
  }

  // defineHostFunction validation tests

  @Test
  void testDefineHostFunctionWithNullModuleName() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineHostFunction(null, "func", testFunctionType, testImplementation));

    assertThat(exception.getMessage()).contains("Module name cannot be null");
  }

  @Test
  void testDefineHostFunctionWithNullFunctionName() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineHostFunction("env", null, testFunctionType, testImplementation));

    assertThat(exception.getMessage()).contains("Function name cannot be null");
  }

  @Test
  void testDefineHostFunctionWithNullFunctionType() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineHostFunction("env", "func", null, testImplementation));

    assertThat(exception.getMessage()).contains("Function type cannot be null");
  }

  @Test
  void testDefineHostFunctionWithNullImplementation() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineHostFunction("env", "func", testFunctionType, null));

    assertThat(exception.getMessage()).contains("Implementation cannot be null");
  }

  // defineMemory validation tests

  @Test
  void testDefineMemoryWithNullStore() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineMemory(null, "env", "memory", memory));

    assertThat(exception.getMessage()).contains("Store cannot be null");
  }

  @Test
  void testDefineMemoryWithNullModuleName() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineMemory(testStore, null, "memory", memory));

    assertThat(exception.getMessage()).contains("Module name cannot be null");
  }

  @Test
  void testDefineMemoryWithNullMemory() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineMemory(testStore, "env", "memory", null));

    assertThat(exception.getMessage()).contains("Memory cannot be null");
  }

  // defineTable validation tests

  @Test
  void testDefineTableWithNullStore() {
    final JniTable table = new JniTable(VALID_HANDLE, testStore);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> linker.defineTable(null, "env", "table", table));

    assertThat(exception.getMessage()).contains("Store cannot be null");
  }

  @Test
  void testDefineTableWithNullModuleName() {
    final JniTable table = new JniTable(VALID_HANDLE, testStore);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineTable(testStore, null, "table", table));

    assertThat(exception.getMessage()).contains("Module name cannot be null");
  }

  @Test
  void testDefineTableWithNullTable() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineTable(testStore, "env", "table", null));

    assertThat(exception.getMessage()).contains("Table cannot be null");
  }

  // defineGlobal validation tests

  @Test
  void testDefineGlobalWithNullStore() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE, testStore);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineGlobal(null, "env", "global", global));

    assertThat(exception.getMessage()).contains("Store cannot be null");
  }

  @Test
  void testDefineGlobalWithNullModuleName() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE, testStore);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineGlobal(testStore, null, "global", global));

    assertThat(exception.getMessage()).contains("Module name cannot be null");
  }

  @Test
  void testDefineGlobalWithNullGlobal() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineGlobal(testStore, "env", "global", null));

    assertThat(exception.getMessage()).contains("Global cannot be null");
  }

  // defineInstance validation tests

  @Test
  void testDefineInstanceWithNullModuleName() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);
    final JniInstance instance = new JniInstance(VALID_HANDLE, module, testStore);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> linker.defineInstance(null, instance));

    assertThat(exception.getMessage()).contains("Module name cannot be null");
  }

  @Test
  void testDefineInstanceWithNullInstance() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> linker.defineInstance("test", null));

    assertThat(exception.getMessage()).contains("Instance cannot be null");
  }

  // instantiate validation tests

  @Test
  void testInstantiateWithNullStore() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> linker.instantiate(null, module));

    assertThat(exception.getMessage()).contains("Store cannot be null");
  }

  @Test
  void testInstantiateWithNullModule() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> linker.instantiate(testStore, (Module) null));

    assertThat(exception.getMessage()).contains("Module cannot be null");
  }

  // instantiate (named) validation tests

  @Test
  void testInstantiateNamedWithNullStore() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> linker.instantiate(null, "test", module));

    assertThat(exception.getMessage()).contains("Store cannot be null");
  }

  @Test
  void testInstantiateNamedWithNullModuleName() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> linker.instantiate(testStore, null, module));

    assertThat(exception.getMessage()).contains("Module name cannot be null");
  }

  @Test
  void testInstantiateNamedWithNullModule() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> linker.instantiate(testStore, "test", null));

    assertThat(exception.getMessage()).contains("Module cannot be null");
  }

  // Unimplemented methods tests

  @Test
  void testEnableWasiThrowsUnsupportedOperationException() {
    assertThrows(UnsupportedOperationException.class, () -> linker.enableWasi());
  }

  @Test
  void testAliasThrowsUnsupportedOperationException() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> linker.alias("from", "fromName", "to", "toName"));
  }

  @Test
  void testGetImportRegistryReturnsEmptyList() {
    final java.util.List<ai.tegmentum.wasmtime4j.ImportInfo> registry = linker.getImportRegistry();

    assertThat(registry).isNotNull();
    assertThat(registry).isEmpty();
  }

  @Test
  void testValidateImportsThrowsUnsupportedOperationException() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertThrows(UnsupportedOperationException.class, () -> linker.validateImports(module));
  }

  @Test
  void testResolveDependenciesThrowsUnsupportedOperationException() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertThrows(UnsupportedOperationException.class, () -> linker.resolveDependencies(module));
  }

  @Test
  void testCreateInstantiationPlanThrowsUnsupportedOperationException() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertThrows(UnsupportedOperationException.class, () -> linker.createInstantiationPlan(module));
  }

  // State validation tests

  @Test
  void testIsValidWithValidHandle() {
    assertTrue(linker.isValid());
  }

  @Test
  void testIsValidWithZeroHandle() {
    final JniLinker<Object> linkerWithZero = new JniLinker<>(0L, testEngine);

    assertFalse(linkerWithZero.isValid());
  }

  @Test
  void testIsValidAfterClose() {
    linker.close();

    assertFalse(linker.isValid());
  }

  // Import tracking tests

  @Test
  void testHasImportInitiallyFalse() {
    assertFalse(linker.hasImport("env", "func"));
  }

  @Test
  void testAddImportAndCheck() {
    linker.addImport("env", "func");

    assertTrue(linker.hasImport("env", "func"));
  }

  @Test
  void testHasImportWithDifferentModule() {
    linker.addImport("env", "func");

    assertFalse(linker.hasImport("other", "func"));
  }

  @Test
  void testHasImportWithDifferentName() {
    linker.addImport("env", "func");

    assertFalse(linker.hasImport("env", "other"));
  }

  // Close tests

  @Test
  void testCloseIsIdempotent() {
    linker.close();
    linker.close();
    linker.close();

    assertFalse(linker.isValid());
  }

  @Test
  void testDefineHostFunctionAfterCloseThrowsIllegalStateException() {
    linker.close();

    final IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> linker.defineHostFunction("env", "func", testFunctionType, testImplementation));

    assertThat(exception.getMessage()).contains("Linker has been closed");
  }

  @Test
  void testInstantiateAfterCloseThrowsIllegalStateException() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);
    linker.close();

    final IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> linker.instantiate(testStore, module));

    assertThat(exception.getMessage()).contains("Linker has been closed");
  }
}

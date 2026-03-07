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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import org.junit.jupiter.api.AfterEach;
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

  @AfterEach
  void tearDown() {
    // Mark all fake-handle resources as closed to prevent GC-triggered native cleanup
    // with invalid handles (which would crash the JVM).
    linker.markClosedForTesting();
    testStore.markClosedForTesting();
    testEngine.markClosedForTesting();
  }

  // Constructor tests

  @Test
  void testConstructorWithValidParameters() {
    final JniLinker<Object> newLinker = new JniLinker<>(VALID_HANDLE, testEngine);

    assertNotNull(newLinker);
    assertEquals(VALID_HANDLE, newLinker.getNativeHandle());
    assertEquals(testEngine, newLinker.getEngine());
    newLinker.markClosedForTesting();
  }

  @Test
  void testConstructorWithZeroHandle() {
    assertThrows(RuntimeException.class, () -> new JniLinker<>(0L, testEngine));
  }

  // defineHostFunction validation tests

  @Test
  void testDefineHostFunctionWithNullModuleName() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineHostFunction(null, "func", testFunctionType, testImplementation));

    assertTrue(
        exception.getMessage().contains("Module name cannot be null"),
        "Expected message to contain: Module name cannot be null");
  }

  @Test
  void testDefineHostFunctionWithNullFunctionName() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineHostFunction("env", null, testFunctionType, testImplementation));

    assertTrue(
        exception.getMessage().contains("Function name cannot be null"),
        "Expected message to contain: Function name cannot be null");
  }

  @Test
  void testDefineHostFunctionWithNullFunctionType() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineHostFunction("env", "func", null, testImplementation));

    assertTrue(
        exception.getMessage().contains("Function type cannot be null"),
        "Expected message to contain: Function type cannot be null");
  }

  @Test
  void testDefineHostFunctionWithNullImplementation() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineHostFunction("env", "func", testFunctionType, null));

    assertTrue(
        exception.getMessage().contains("Implementation cannot be null"),
        "Expected message to contain: Implementation cannot be null");
  }

  // defineMemory validation tests

  @Test
  void testDefineMemoryWithNullStore() {
    final JniMemory memory = new JniMemory(VALID_HANDLE, testStore);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineMemory(null, "env", "memory", memory));

    assertTrue(
        exception.getMessage().contains("Store cannot be null"),
        "Expected message to contain: Store cannot be null");
    memory.markClosedForTesting();
  }

  @Test
  void testDefineMemoryWithNullModuleName() {
    final JniMemory memory = new JniMemory(VALID_HANDLE, testStore);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineMemory(testStore, null, "memory", memory));

    assertTrue(
        exception.getMessage().contains("Module name cannot be null"),
        "Expected message to contain: Module name cannot be null");
    memory.markClosedForTesting();
  }

  @Test
  void testDefineMemoryWithNullMemory() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineMemory(testStore, "env", "memory", null));

    assertTrue(
        exception.getMessage().contains("Memory cannot be null"),
        "Expected message to contain: Memory cannot be null");
  }

  // defineTable validation tests

  @Test
  void testDefineTableWithNullStore() {
    final JniTable table = new JniTable(VALID_HANDLE, testStore);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> linker.defineTable(null, "env", "table", table));

    assertTrue(
        exception.getMessage().contains("Store cannot be null"),
        "Expected message to contain: Store cannot be null");
    table.markClosedForTesting();
  }

  @Test
  void testDefineTableWithNullModuleName() {
    final JniTable table = new JniTable(VALID_HANDLE, testStore);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineTable(testStore, null, "table", table));

    assertTrue(
        exception.getMessage().contains("Module name cannot be null"),
        "Expected message to contain: Module name cannot be null");
    table.markClosedForTesting();
  }

  @Test
  void testDefineTableWithNullTable() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineTable(testStore, "env", "table", null));

    assertTrue(
        exception.getMessage().contains("Table cannot be null"),
        "Expected message to contain: Table cannot be null");
  }

  // defineGlobal validation tests

  @Test
  void testDefineGlobalWithNullStore() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE, testStore);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineGlobal(null, "env", "global", global));

    assertTrue(
        exception.getMessage().contains("Store cannot be null"),
        "Expected message to contain: Store cannot be null");
    global.markClosedForTesting();
  }

  @Test
  void testDefineGlobalWithNullModuleName() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE, testStore);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineGlobal(testStore, null, "global", global));

    assertTrue(
        exception.getMessage().contains("Module name cannot be null"),
        "Expected message to contain: Module name cannot be null");
    global.markClosedForTesting();
  }

  @Test
  void testDefineGlobalWithNullGlobal() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> linker.defineGlobal(testStore, "env", "global", null));

    assertTrue(
        exception.getMessage().contains("Global cannot be null"),
        "Expected message to contain: Global cannot be null");
  }

  // defineInstance validation tests

  @Test
  void testDefineInstanceWithNullModuleName() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);
    final JniInstance instance = new JniInstance(VALID_HANDLE, module, testStore);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> linker.defineInstance(testStore, null, instance));

    assertTrue(
        exception.getMessage().contains("Module name cannot be null"),
        "Expected message to contain: Module name cannot be null");
    instance.markClosedForTesting();
    module.markClosedForTesting();
  }

  @Test
  void testDefineInstanceWithNullInstance() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> linker.defineInstance(testStore, "test", null));

    assertTrue(
        exception.getMessage().contains("Instance cannot be null"),
        "Expected message to contain: Instance cannot be null");
  }

  // instantiate validation tests

  @Test
  void testInstantiateWithNullStore() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> linker.instantiate(null, module));

    assertTrue(
        exception.getMessage().contains("Store cannot be null"),
        "Expected message to contain: Store cannot be null");
    module.markClosedForTesting();
  }

  @Test
  void testInstantiateWithNullModule() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> linker.instantiate(testStore, (Module) null));

    assertTrue(
        exception.getMessage().contains("Module cannot be null"),
        "Expected message to contain: Module cannot be null");
  }

  // instantiate (named) validation tests

  @Test
  void testInstantiateNamedWithNullStore() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> linker.instantiate(null, "test", module));

    assertTrue(
        exception.getMessage().contains("Store cannot be null"),
        "Expected message to contain: Store cannot be null");
    module.markClosedForTesting();
  }

  @Test
  void testInstantiateNamedWithNullModuleName() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> linker.instantiate(testStore, null, module));

    assertTrue(
        exception.getMessage().contains("Module name cannot be null"),
        "Expected message to contain: Module name cannot be null");
    module.markClosedForTesting();
  }

  @Test
  void testInstantiateNamedWithNullModule() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> linker.instantiate(testStore, "test", null));

    assertTrue(
        exception.getMessage().contains("Module cannot be null"),
        "Expected message to contain: Module cannot be null");
  }

  @Test
  void testGetImportRegistryReturnsEmptyList() {
    final java.util.List<ai.tegmentum.wasmtime4j.validation.ImportInfo> registry =
        linker.getImportRegistry();

    assertNotNull(registry);
    assertTrue(registry.isEmpty());
  }

  // State validation tests

  @Test
  void testIsValidWithValidHandle() {
    assertTrue(linker.isValid());
  }

  @Test
  void testIsValidWithZeroHandle() {
    assertThrows(RuntimeException.class, () -> new JniLinker<>(0L, testEngine));
  }

  @Test
  void testIsValidAfterClose() {
    linker.markClosedForTesting();

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
    // Use markClosedForTesting to avoid native calls with fake handles
    linker.markClosedForTesting();
    linker.close(); // Second close should be a no-op
    linker.close(); // Third close should be a no-op

    assertFalse(linker.isValid());
  }

  @Test
  void testDefineHostFunctionAfterCloseThrowsIllegalStateException() {
    linker.markClosedForTesting();

    final IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> linker.defineHostFunction("env", "func", testFunctionType, testImplementation));

    assertTrue(
        exception.getMessage().contains("closed"),
        "Expected message to contain: closed");
  }

  @Test
  void testInstantiateAfterCloseThrowsIllegalStateException() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);
    linker.markClosedForTesting();

    final IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> linker.instantiate(testStore, module));

    assertTrue(
        exception.getMessage().contains("closed"),
        "Expected message to contain: closed");
    module.markClosedForTesting();
  }
}

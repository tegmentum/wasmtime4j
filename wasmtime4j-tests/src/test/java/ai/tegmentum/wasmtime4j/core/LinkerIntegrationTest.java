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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.CodeBuilder;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FuncType;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;

/**
 * Integration tests for WebAssembly Linker operations.
 *
 * <p>These tests verify linker functionality for defining and linking host resources including
 * memories, tables, globals, and module instances.
 *
 * @since 1.0.0
 */
@DisplayName("Linker Integration Tests")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public final class LinkerIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(LinkerIntegrationTest.class.getName());

  /**
   * Forces garbage collection to clean up native resources between test classes. This helps prevent
   * resource exhaustion that can cause test hangs.
   */
  static void cleanupNativeResources() {
    System.gc();
    try {
      Thread.sleep(50); // Give GC time to run
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    System.gc();
  }

  /** Helper method to create a FuncType without a factory method. */
  private static FuncType funcType(
      final List<WasmValueType> params, final List<WasmValueType> results) {
    return new FuncType() {
      @Override
      public List<WasmValueType> getParams() {
        return Collections.unmodifiableList(params);
      }

      @Override
      public List<WasmValueType> getResults() {
        return Collections.unmodifiableList(results);
      }
    };
  }

  /**
   * Creates a module that imports a memory from "env" "memory" and exports store/load functions.
   *
   * <pre>
   * (module
   *   (import "env" "memory" (memory 1))
   *   (func (export "store") (param i32 i32)
   *     local.get 0
   *     local.get 1
   *     i32.store)
   *   (func (export "load") (param i32) (result i32)
   *     local.get 0
   *     i32.load))
   * </pre>
   */
  private static byte[] createImportMemoryModule() throws WasmException {
    return new CodeBuilder()
        .addType(funcType(List.of(WasmValueType.I32, WasmValueType.I32), List.of())) // store
        .addType(funcType(List.of(WasmValueType.I32), List.of(WasmValueType.I32))) // load
        .addMemoryImport("env", "memory", 1, -1)
        .addFunction(
            0, List.of(), new byte[] {0x20, 0x00, 0x20, 0x01, 0x36, 0x02, 0x00}) // store: i32.store
        .addFunction(1, List.of(), new byte[] {0x20, 0x00, 0x28, 0x02, 0x00}) // load: i32.load
        .addExport("store", CodeBuilder.ExportKind.FUNCTION, 0)
        .addExport("load", CodeBuilder.ExportKind.FUNCTION, 1)
        .build();
  }

  /**
   * Creates a module that imports a global from "env" "counter" and exports get/set/inc functions.
   *
   * <pre>
   * (module
   *   (import "env" "counter" (global $counter (mut i32)))
   *   (func (export "get") (result i32)
   *     global.get $counter)
   *   (func (export "set") (param i32)
   *     local.get 0
   *     global.set $counter)
   *   (func (export "inc")
   *     global.get $counter
   *     i32.const 1
   *     i32.add
   *     global.set $counter))
   * </pre>
   */
  private static byte[] createImportGlobalModule() throws WasmException {
    return new CodeBuilder()
        // Types: () -> i32, (i32) -> (), () -> ()
        .addType(funcType(List.of(), List.of(WasmValueType.I32)))
        .addType(funcType(List.of(WasmValueType.I32), List.of()))
        .addType(funcType(List.of(), List.of()))
        // Import mutable i32 global from "env" "counter"
        .addGlobalImport("env", "counter", WasmValueType.I32, true)
        // Function 0: get - global.get 0
        .addFunction(0, List.of(), new byte[] {0x23, 0x00})
        // Function 1: set - local.get 0, global.set 0
        .addFunction(1, List.of(), new byte[] {0x20, 0x00, 0x24, 0x00})
        // Function 2: inc - global.get 0, i32.const 1, i32.add, global.set 0
        .addFunction(2, List.of(), new byte[] {0x23, 0x00, 0x41, 0x01, 0x6a, 0x24, 0x00})
        // Exports
        .addExport("get", CodeBuilder.ExportKind.FUNCTION, 0)
        .addExport("set", CodeBuilder.ExportKind.FUNCTION, 1)
        .addExport("inc", CodeBuilder.ExportKind.FUNCTION, 2)
        .build();
  }

  /**
   * Creates a simple module that exports a "double" function.
   *
   * <pre>
   * (module
   *   (func (export "double") (param i32) (result i32)
   *     local.get 0
   *     i32.const 2
   *     i32.mul))
   * </pre>
   */
  private static byte[] createSimpleExportModule() throws WasmException {
    return new CodeBuilder()
        .addType(funcType(List.of(WasmValueType.I32), List.of(WasmValueType.I32)))
        .addFunction(
            0, List.of(), new byte[] {0x20, 0x00, 0x41, 0x02, 0x6c}) // local.get 0, i32.const
        // 2, i32.mul
        .addExport("double", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  /**
   * Creates a module that imports a function from "math" "double" and exports "quadruple".
   *
   * <pre>
   * (module
   *   (import "math" "double" (func $double (param i32) (result i32)))
   *   (func (export "quadruple") (param i32) (result i32)
   *     local.get 0
   *     call $double
   *     call $double))
   * </pre>
   */
  private static byte[] createImportFromInstanceModule() throws WasmException {
    return new CodeBuilder()
        .addType(funcType(List.of(WasmValueType.I32), List.of(WasmValueType.I32)))
        .addFunctionImport("math", "double", 0)
        .addFunction(
            0, List.of(), new byte[] {0x20, 0x00, 0x10, 0x00, 0x10, 0x00}) // local.get 0, call 0,
        // call 0
        .addExport("quadruple", CodeBuilder.ExportKind.FUNCTION, 1)
        .build();
  }

  /**
   * Creates a module that imports "env" "add" function and exports "call_add".
   *
   * <pre>
   * (module
   *   (import "env" "add" (func $add (param i32 i32) (result i32)))
   *   (func (export "call_add") (param i32 i32) (result i32)
   *     local.get 0
   *     local.get 1
   *     call $add))
   * </pre>
   */
  private static byte[] createImportAddModule() throws WasmException {
    return new CodeBuilder()
        .addType(
            funcType(List.of(WasmValueType.I32, WasmValueType.I32), List.of(WasmValueType.I32)))
        .addFunctionImport("env", "add", 0)
        .addFunction(
            0, List.of(), new byte[] {0x20, 0x00, 0x20, 0x01, 0x10, 0x00}) // local.get 0, local.get
        // 1, call 0
        .addExport("call_add", CodeBuilder.ExportKind.FUNCTION, 1)
        .build();
  }

  @Nested
  @Order(2)
  @DisplayName("Define Memory Tests")
  class DefineMemoryTests {

    @BeforeAll
    static void setUp() {
      cleanupNativeResources();
    }

    @AfterAll
    static void tearDown() {
      cleanupNativeResources();
    }

    @Test
    @DisplayName("should define and use imported memory")
    void shouldDefineAndUseImportedMemory() throws Exception {
      LOGGER.info("Testing memory definition in linker");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore()) {

        // Create a memory to provide to the module
        // Module imports memory with min=1, max=unlimited (-1), so we must match
        final WasmMemory memory = store.createMemory(1, -1);
        assertNotNull(memory, "Memory should be created");

        // Define the memory in the linker
        linker.defineMemory(store, "env", "memory", memory);
        LOGGER.info("Defined memory in linker");

        // Compile and instantiate the module
        final Module module = engine.compileModule(createImportMemoryModule());
        try (final Instance instance = linker.instantiate(store, module)) {
          assertNotNull(instance, "Instance should be created");

          // Get the exported functions
          final Optional<WasmFunction> storeFunc = instance.getFunction("store");
          final Optional<WasmFunction> loadFunc = instance.getFunction("load");
          assertTrue(storeFunc.isPresent(), "store function should exist");
          assertTrue(loadFunc.isPresent(), "load function should exist");

          // Store a value and read it back
          storeFunc.get().call(WasmValue.i32(0), WasmValue.i32(42));
          final WasmValue[] result = loadFunc.get().call(WasmValue.i32(0));
          assertEquals(42, result[0].asInt(), "Should load stored value");

          LOGGER.info("Memory import/export verified successfully");
        }
      }
    }

    @Test
    @DisplayName("should share memory between host and module")
    void shouldShareMemoryBetweenHostAndModule() throws Exception {
      LOGGER.info("Testing shared memory access");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore()) {

        // Module imports memory with min=1, max=unlimited (-1), so we must match
        final WasmMemory memory = store.createMemory(1, -1);
        linker.defineMemory(store, "env", "memory", memory);

        final Module module = engine.compileModule(createImportMemoryModule());
        try (final Instance instance = linker.instantiate(store, module)) {
          final Optional<WasmFunction> loadFunc = instance.getFunction("load");
          assertTrue(loadFunc.isPresent());

          // Write directly to memory from host
          memory.writeInt32(0, 12345);

          // Read via WASM function
          final WasmValue[] result = loadFunc.get().call(WasmValue.i32(0));
          assertEquals(12345, result[0].asInt(), "Module should see host-written value");

          LOGGER.info("Shared memory verified");
        }
      }
    }
  }

  @Nested
  @Order(3)
  @DisplayName("Define Global Tests")
  class DefineGlobalTests {

    @BeforeAll
    static void setUp() {
      cleanupNativeResources();
    }

    @AfterAll
    static void tearDown() {
      cleanupNativeResources();
    }

    @Test
    @DisplayName("should define and use imported mutable global")
    void shouldDefineAndUseImportedMutableGlobal() throws Exception {
      LOGGER.info("Testing mutable global definition in linker");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore()) {

        // Create a mutable global
        final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(0));
        assertNotNull(global, "Global should be created");

        // Define the global in the linker
        linker.defineGlobal(store, "env", "counter", global);
        LOGGER.info("Defined global in linker");

        // Compile and instantiate the module
        final Module module = engine.compileModule(createImportGlobalModule());
        try (final Instance instance = linker.instantiate(store, module)) {
          assertNotNull(instance, "Instance should be created");

          // Get the exported functions
          final Optional<WasmFunction> getFunc = instance.getFunction("get");
          final Optional<WasmFunction> setFunc = instance.getFunction("set");
          final Optional<WasmFunction> incFunc = instance.getFunction("inc");
          assertTrue(getFunc.isPresent(), "get function should exist");
          assertTrue(setFunc.isPresent(), "set function should exist");
          assertTrue(incFunc.isPresent(), "inc function should exist");

          // Test get (should return 0)
          assertEquals(0, getFunc.get().call()[0].asInt());

          // Test set
          setFunc.get().call(WasmValue.i32(42));
          assertEquals(42, getFunc.get().call()[0].asInt());

          // Test inc
          incFunc.get().call();
          assertEquals(43, getFunc.get().call()[0].asInt());

          // Verify host can also see the change
          assertEquals(43, global.get().asI32());

          LOGGER.info("Global import verified successfully");
        }
      }
    }

    @Test
    @DisplayName("should share global between host and module")
    void shouldShareGlobalBetweenHostAndModule() throws Exception {
      LOGGER.info("Testing shared global access");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore()) {

        final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(100));
        linker.defineGlobal(store, "env", "counter", global);

        final Module module = engine.compileModule(createImportGlobalModule());
        try (final Instance instance = linker.instantiate(store, module)) {
          final Optional<WasmFunction> getFunc = instance.getFunction("get");
          assertTrue(getFunc.isPresent());

          // Verify WASM sees host-set value
          assertEquals(100, getFunc.get().call()[0].asInt());

          // Host modifies global
          global.set(WasmValue.i32(200));

          // WASM should see new value
          assertEquals(200, getFunc.get().call()[0].asInt());

          LOGGER.info("Shared global verified");
        }
      }
    }
  }

  @Nested
  @Order(4)
  @DisplayName("Define Instance Tests")
  class DefineInstanceTests {

    @BeforeAll
    static void setUp() {
      cleanupNativeResources();
    }

    @AfterAll
    static void tearDown() {
      cleanupNativeResources();
    }

    @Test
    @DisplayName("should define instance and import from it")
    void shouldDefineInstanceAndImportFromIt() throws Exception {
      LOGGER.info("Testing instance definition in linker");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore()) {

        // Create and instantiate the provider module
        final Module providerModule = engine.compileModule(createSimpleExportModule());
        final Instance providerInstance = store.createInstance(providerModule);

        // Define the instance in the linker
        linker.defineInstance("math", providerInstance);
        LOGGER.info("Defined instance in linker");

        // Compile and instantiate the consumer module
        final Module consumerModule = engine.compileModule(createImportFromInstanceModule());
        try (final Instance instance = linker.instantiate(store, consumerModule)) {
          assertNotNull(instance, "Instance should be created");

          // Get the exported function
          final Optional<WasmFunction> quadruple = instance.getFunction("quadruple");
          assertTrue(quadruple.isPresent(), "quadruple function should exist");

          // Test: 5 * 2 * 2 = 20
          final WasmValue[] result = quadruple.get().call(WasmValue.i32(5));
          assertEquals(20, result[0].asInt(), "5 quadrupled should be 20");

          LOGGER.info("Instance import verified successfully");
        }
      }
    }

    @Test
    @DisplayName("should chain multiple instance definitions")
    void shouldChainMultipleInstanceDefinitions() throws Exception {
      LOGGER.info("Testing chained instance definitions");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore()) {

        // Create first provider
        final Module providerModule = engine.compileModule(createSimpleExportModule());
        final Instance providerInstance = store.createInstance(providerModule);
        linker.defineInstance("math", providerInstance);

        // Create consumer that uses first provider
        final Module consumerModule = engine.compileModule(createImportFromInstanceModule());
        final Instance consumerInstance = linker.instantiate(store, consumerModule);

        // Verify the chain works
        final Optional<WasmFunction> quadruple = consumerInstance.getFunction("quadruple");
        assertTrue(quadruple.isPresent());
        assertEquals(40, quadruple.get().call(WasmValue.i32(10))[0].asInt());

        LOGGER.info("Chained instance definitions verified");
      }
    }
  }

  @Nested
  @Order(5)
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @BeforeAll
    static void setUp() {
      cleanupNativeResources();
    }

    @AfterAll
    static void tearDown() {
      cleanupNativeResources();
    }

    @Test
    @DisplayName("should fail when import not satisfied")
    void shouldFailWhenImportNotSatisfied() throws Exception {
      LOGGER.info("Testing unsatisfied import error");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore()) {

        // Try to instantiate without providing required import
        final Module module = engine.compileModule(createImportMemoryModule());

        assertThrows(
            WasmException.class,
            () -> linker.instantiate(store, module),
            "Should throw when imports are not satisfied");

        LOGGER.info("Unsatisfied import error verified");
      }
    }

    @Test
    @DisplayName("should handle null parameters gracefully")
    void shouldHandleNullParametersGracefully() throws Exception {
      LOGGER.info("Testing null parameter handling");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore()) {

        final WasmMemory memory = store.createMemory(1, 10);

        assertThrows(
            Exception.class,
            () -> linker.defineMemory(null, "env", "memory", memory),
            "Should reject null store");

        assertThrows(
            Exception.class,
            () -> linker.defineMemory(store, null, "memory", memory),
            "Should reject null module name");

        assertThrows(
            Exception.class,
            () -> linker.defineMemory(store, "env", null, memory),
            "Should reject null name");

        assertThrows(
            Exception.class,
            () -> linker.defineMemory(store, "env", "memory", null),
            "Should reject null memory");

        LOGGER.info("Null parameter handling verified");
      }
    }
  }

  @Nested
  @Order(1)
  @DisplayName("Linker Lifecycle Tests")
  class LinkerLifecycleTests {

    @BeforeAll
    static void setUp() {
      cleanupNativeResources();
    }

    @AfterAll
    static void tearDown() {
      cleanupNativeResources();
    }

    @Test
    @DisplayName("should create and close linker")
    void shouldCreateAndCloseLinker() throws Exception {
      LOGGER.info("Testing linker lifecycle");

      final Linker<Void> linker;
      try (final Engine engine = Engine.create()) {
        linker = Linker.create(engine);
        assertNotNull(linker, "Linker should be created");
      }

      assertDoesNotThrow(linker::close, "Linker should close without error");

      LOGGER.info("Linker lifecycle verified");
    }

    @Test
    @DisplayName("should reuse linker for multiple instantiations in same store")
    void shouldReuseLinkerForMultipleInstantiations() throws Exception {
      LOGGER.info("Testing linker reuse");

      try (final Engine engine = Engine.create();
          final Linker<Void> linker = Linker.create(engine);
          final Store store = engine.createStore()) {

        // Define host function once
        final FunctionType type =
            new FunctionType(
                new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
                new WasmValueType[] {WasmValueType.I32});
        linker.defineHostFunction(
            "env",
            "add",
            type,
            (params) -> new WasmValue[] {WasmValue.i32(params[0].asI32() + params[1].asI32())});

        final Module module = engine.compileModule(createImportAddModule());

        // Instantiate multiple times with same linker and same store
        try (final Instance instance1 = linker.instantiate(store, module);
            final Instance instance2 = linker.instantiate(store, module)) {

          assertNotNull(instance1);
          assertNotNull(instance2);

          // Both should work
          final Optional<WasmFunction> f1 = instance1.getFunction("call_add");
          final Optional<WasmFunction> f2 = instance2.getFunction("call_add");
          assertTrue(f1.isPresent());
          assertTrue(f2.isPresent());

          assertEquals(7, f1.get().call(WasmValue.i32(3), WasmValue.i32(4))[0].asInt());
          assertEquals(11, f2.get().call(WasmValue.i32(5), WasmValue.i32(6))[0].asInt());

          LOGGER.info("Linker reuse verified");
        }
      }
    }
  }
}

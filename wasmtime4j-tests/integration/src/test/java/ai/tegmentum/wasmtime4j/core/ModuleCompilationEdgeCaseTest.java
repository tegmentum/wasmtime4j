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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.ImportType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for WebAssembly module compilation edge cases.
 *
 * <p>Tests compilation errors, imports, lifecycle, and module size edge cases. Separate from {@link
 * ModuleValidationTest} which covers the {@code Module.validate()} static API.
 *
 * @since 1.0.0
 */
@DisplayName("Module Compilation Edge Case Tests")
public class ModuleCompilationEdgeCaseTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(ModuleCompilationEdgeCaseTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  // ---- Valid Module Tests ----

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should compile minimal valid module")
  void shouldCompileMinimalValidModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing: should compile minimal valid module");

    try (Engine engine = Engine.create()) {
      // Minimal valid module: just magic and version
      final byte[] wasm = {
        0x00, 0x61, 0x73, 0x6D, // magic "\0asm"
        0x01, 0x00, 0x00, 0x00 // version 1
      };

      try (Module module = engine.compileModule(wasm)) {
        assertNotNull(module, "Module should be created");
        assertTrue(module.isValid(), "Module should be valid");

        // Minimal module should have no exports or imports
        assertTrue(module.getExports().isEmpty(), "Should have no exports");
        assertTrue(module.getImports().isEmpty(), "Should have no imports");

        LOGGER.info("[" + runtime + "] Minimal module compiled successfully");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should compile module with empty sections")
  void shouldCompileModuleWithEmptySections(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing: should compile module with empty sections");

    try (Engine engine = Engine.create()) {
      // Module with empty type section
      final byte[] wasm = {
        0x00, 0x61, 0x73, 0x6D, // magic
        0x01, 0x00, 0x00, 0x00, // version
        0x01, 0x01, 0x00 // empty type section
      };

      try (Module module = engine.compileModule(wasm)) {
        assertNotNull(module, "Module should be created");
        assertTrue(module.isValid(), "Module should be valid");

        LOGGER.info("[" + runtime + "] Module with empty section compiled successfully");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should validate module bytecode by successful compilation")
  void shouldValidateModuleBytecode(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info(
        "[" + runtime + "] Testing: should validate module bytecode by successful compilation");

    try (Engine engine = Engine.create()) {
      // Valid module with function
      final byte[] validWasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x05, // type section
        0x01,
        0x60,
        0x00,
        0x01,
        0x7F, // () -> i32
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x0A,
        0x06, // code section
        0x01,
        0x04,
        0x00,
        0x41,
        0x00,
        0x0B // function body
      };

      // Valid modules compile without throwing exceptions
      final Module module =
          assertDoesNotThrow(
              () -> engine.compileModule(validWasm), "Valid module should compile successfully");

      assertNotNull(module, "Module should not be null");
      assertTrue(module.isValid(), "Module should be valid");
      module.close();

      LOGGER.info("[" + runtime + "] Module validation passed");
    }
  }

  // ---- Invalid Module Tests ----

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should reject invalid magic number")
  void shouldRejectInvalidMagicNumber(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing: should reject invalid magic number");

    try (Engine engine = Engine.create()) {
      // Invalid magic number
      final byte[] invalidMagic = {
        0x00, 0x00, 0x00, 0x00, // wrong magic
        0x01, 0x00, 0x00, 0x00 // version
      };

      assertThrows(
          WasmException.class,
          () -> engine.compileModule(invalidMagic),
          "Should reject invalid magic number");

      LOGGER.info("[" + runtime + "] Invalid magic number rejected correctly");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should reject truncated module")
  void shouldRejectTruncatedModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing: should reject truncated module");

    try (Engine engine = Engine.create()) {
      // Truncated - only magic, no version
      final byte[] truncated = {
        0x00, 0x61, 0x73, 0x6D // magic only
      };

      assertThrows(
          WasmException.class,
          () -> engine.compileModule(truncated),
          "Should reject truncated module");

      LOGGER.info("[" + runtime + "] Truncated module rejected correctly");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should reject empty bytes")
  void shouldRejectEmptyBytes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing: should reject empty bytes");

    try (Engine engine = Engine.create()) {
      final byte[] empty = {};

      assertThrows(Exception.class, () -> engine.compileModule(empty), "Should reject empty bytes");

      LOGGER.info("[" + runtime + "] Empty bytes rejected correctly");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should reject module with invalid section")
  void shouldRejectModuleWithInvalidSection(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing: should reject module with invalid section");

    try (Engine engine = Engine.create()) {
      // Module with section claiming more bytes than available
      final byte[] invalidSection = {
        0x00, 0x61, 0x73, 0x6D, // magic
        0x01, 0x00, 0x00, 0x00, // version
        0x01, (byte) 0xFF // type section claiming 127 bytes but has none
      };

      assertThrows(
          WasmException.class,
          () -> engine.compileModule(invalidSection),
          "Should reject module with invalid section");

      LOGGER.info("[" + runtime + "] Invalid section rejected correctly");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should fail validation for invalid bytecode")
  void shouldFailValidationForInvalidBytecode(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing: should fail validation for invalid bytecode");

    try (Engine engine = Engine.create()) {
      // Invalid bytecode - wrong magic
      final byte[] invalidWasm = {0x01, 0x02, 0x03, 0x04, 0x01, 0x00, 0x00, 0x00};

      // Invalid modules throw exceptions during compilation
      assertThrows(
          WasmException.class,
          () -> engine.compileModule(invalidWasm),
          "Invalid module should fail to compile");

      LOGGER.info("[" + runtime + "] Invalid bytecode failed validation as expected");
    }
  }

  // ---- Module Import Tests ----

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should enumerate module imports")
  void shouldEnumerateModuleImports(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing: should enumerate module imports");

    try (Engine engine = Engine.create()) {
      // Module with function import: (import "env" "log" (func (param i32)))
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x05, // type section
        0x01,
        0x60,
        0x01,
        0x7F,
        0x00, // type 0: (i32) -> ()
        0x02,
        0x0B, // import section
        0x01, // 1 import
        0x03,
        0x65,
        0x6E,
        0x76, // "env"
        0x03,
        0x6C,
        0x6F,
        0x67, // "log"
        0x00,
        0x00 // func, type 0
      };

      try (Module module = engine.compileModule(wasm)) {
        final List<ImportType> imports = module.getImports();
        assertNotNull(imports, "Imports should not be null");
        assertEquals(1, imports.size(), "Should have 1 import");

        final ImportType imp = imports.get(0);
        assertEquals("env", imp.getModuleName(), "Module name should be 'env'");
        assertEquals("log", imp.getName(), "Import name should be 'log'");

        LOGGER.info(
            "[" + runtime + "] Found import: " + imp.getModuleName() + "::" + imp.getName());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should enumerate memory import")
  void shouldEnumerateMemoryImport(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing: should enumerate memory import");

    try (Engine engine = Engine.create()) {
      // Module with memory import: (import "env" "memory" (memory 1))
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x02,
        0x0F, // import section, 15 bytes (1 + 1 + 3 + 1 + 6 + 1 + 2)
        0x01, // 1 import
        0x03,
        0x65,
        0x6E,
        0x76, // "env"
        0x06,
        0x6D,
        0x65,
        0x6D,
        0x6F,
        0x72,
        0x79, // "memory"
        0x02,
        0x00,
        0x01 // memory, limits: min 1
      };

      try (Module module = engine.compileModule(wasm)) {
        final List<ImportType> imports = module.getImports();
        assertEquals(1, imports.size(), "Should have 1 import");

        final ImportType imp = imports.get(0);
        assertEquals("env", imp.getModuleName(), "Module name should be 'env'");
        assertEquals("memory", imp.getName(), "Import name should be 'memory'");

        LOGGER.info(
            "[" + runtime + "] Found memory import: " + imp.getModuleName() + "::" + imp.getName());
      }
    }
  }

  // ---- Module Lifecycle Tests ----

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should handle module close and recreation")
  void shouldHandleModuleCloseAndRecreation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing: should handle module close and recreation");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x05,
        0x01,
        0x60,
        0x00,
        0x01,
        0x7F, // type section
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07,
        0x01,
        0x03,
        0x67,
        0x65,
        0x74,
        0x00,
        0x00, // export "get"
        0x0A,
        0x06,
        0x01,
        0x04,
        0x00,
        0x41,
        0x0A,
        0x0B // code: return 10
      };

      // Create, use, and close module
      Module module = engine.compileModule(wasm);
      Instance instance = store.createInstance(module);
      WasmFunction func = instance.getFunction("get").orElseThrow();
      assertEquals(10, func.call()[0].asInt(), "First module should return 10");
      instance.close();
      module.close();

      // Recreate module and verify it works
      module = engine.compileModule(wasm);
      instance = store.createInstance(module);
      func = instance.getFunction("get").orElseThrow();
      assertEquals(10, func.call()[0].asInt(), "Second module should return 10");
      instance.close();
      module.close();

      LOGGER.info("[" + runtime + "] Module recreation handled correctly");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should create multiple instances from same module")
  void shouldCreateMultipleInstancesFromSameModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing: should create multiple instances from same module");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      // Module with mutable global
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x05,
        0x01,
        0x60,
        0x00,
        0x01,
        0x7F, // type: () -> i32
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x06,
        0x06,
        0x01,
        0x7F,
        0x01,
        0x41,
        0x00,
        0x0B, // global i32 mut = 0
        0x07,
        0x10, // export section
        0x02, // 2 exports
        0x06,
        0x67,
        0x6C,
        0x6F,
        0x62,
        0x61,
        0x6C,
        0x03,
        0x00, // "global"
        0x03,
        0x67,
        0x65,
        0x74,
        0x00,
        0x00, // "get"
        0x0A,
        0x06, // code section
        0x01,
        0x04,
        0x00,
        0x23,
        0x00,
        0x0B // get: global.get 0
      };

      try (Module module = engine.compileModule(wasm)) {
        // Create two instances
        try (Instance instance1 = store.createInstance(module);
            Instance instance2 = store.createInstance(module)) {

          // Get globals from each instance
          final var global1 = instance1.getGlobal("global").orElseThrow();
          final var global2 = instance2.getGlobal("global").orElseThrow();

          // Set different values
          global1.set(WasmValue.i32(100));
          global2.set(WasmValue.i32(200));

          // Verify isolation
          assertEquals(100, global1.get().asInt(), "Instance 1 global should be 100");
          assertEquals(200, global2.get().asInt(), "Instance 2 global should be 200");

          LOGGER.info("[" + runtime + "] Multiple instances from same module have isolated state");
        }
      }
    }
  }

  // ---- Module Size Tests ----

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("should handle module with many functions")
  void shouldHandleModuleWithManyFunctions(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing: should handle module with many functions");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      // Build a module with 10 functions dynamically
      final List<Byte> bytes = new ArrayList<>();

      // Magic and version
      addBytes(bytes, 0x00, 0x61, 0x73, 0x6D);
      addBytes(bytes, 0x01, 0x00, 0x00, 0x00);

      // Type section: 1 type () -> i32
      addBytes(bytes, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01, 0x7F);

      // Function section: 10 functions all of type 0
      addBytes(bytes, 0x03, 0x0B, 0x0A); // section id, size, count
      for (int i = 0; i < 10; i++) {
        addBytes(bytes, 0x00); // type 0
      }

      // Export section: export first 3 functions
      addBytes(bytes, 0x07, 0x10, 0x03); // section id, size, count
      addBytes(bytes, 0x02, 0x66, 0x30, 0x00, 0x00); // "f0" func 0
      addBytes(bytes, 0x02, 0x66, 0x31, 0x00, 0x01); // "f1" func 1
      addBytes(bytes, 0x02, 0x66, 0x32, 0x00, 0x02); // "f2" func 2

      // Code section: 10 functions each returning their index
      final int codeSectionSize = 10 * 5 + 1; // 10 functions * 5 bytes + 1 byte count
      addBytes(bytes, 0x0A, codeSectionSize & 0xFF, 0x0A); // section id, size, count
      for (int i = 0; i < 10; i++) {
        addBytes(bytes, 0x04, 0x00, 0x41, i, 0x0B); // body size, locals, i32.const i, end
      }

      final byte[] wasm = toByteArray(bytes);

      try (Module module = engine.compileModule(wasm);
          Instance instance = store.createInstance(module)) {

        for (int i = 0; i < 3; i++) {
          final WasmFunction func = instance.getFunction("f" + i).orElseThrow();
          final WasmValue[] results = func.call();
          assertEquals(i, results[0].asInt(), "Function f" + i + " should return " + i);
        }

        LOGGER.info("[" + runtime + "] Module with 10 functions handled correctly");
      }
    }
  }

  private static void addBytes(final List<Byte> list, final int... values) {
    for (final int v : values) {
      list.add((byte) v);
    }
  }

  private static byte[] toByteArray(final List<Byte> list) {
    final byte[] result = new byte[list.size()];
    for (int i = 0; i < list.size(); i++) {
      result[i] = list.get(i);
    }
    return result;
  }
}

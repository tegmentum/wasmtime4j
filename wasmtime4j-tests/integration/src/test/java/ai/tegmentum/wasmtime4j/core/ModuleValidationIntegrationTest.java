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
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.type.ImportType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for WebAssembly module validation and compilation.
 *
 * <p>Tests module validation, compilation errors, and various edge cases in module processing.
 *
 * @since 1.0.0
 */
@DisplayName("Module Validation Integration Tests")
public final class ModuleValidationIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ModuleValidationIntegrationTest.class.getName());

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    runtime = WasmRuntimeFactory.create();
    engine = runtime.createEngine();
    store = engine.createStore();
    resources.add(store);
    resources.add(engine);
    resources.add(runtime);
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
  }

  @Nested
  @DisplayName("Valid Module Tests")
  class ValidModuleTests {

    @Test
    @DisplayName("should compile minimal valid module")
    void shouldCompileMinimalValidModule(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Minimal valid module: just magic and version
      final byte[] wasm = {
        0x00, 0x61, 0x73, 0x6D, // magic "\0asm"
        0x01, 0x00, 0x00, 0x00 // version 1
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      assertNotNull(module, "Module should be created");
      assertTrue(module.isValid(), "Module should be valid");

      // Minimal module should have no exports or imports
      assertTrue(module.getExports().isEmpty(), "Should have no exports");
      assertTrue(module.getImports().isEmpty(), "Should have no imports");

      LOGGER.info("Minimal module compiled successfully");
    }

    @Test
    @DisplayName("should compile module with empty sections")
    void shouldCompileModuleWithEmptySections(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Module with empty type section
      final byte[] wasm = {
        0x00, 0x61, 0x73, 0x6D, // magic
        0x01, 0x00, 0x00, 0x00, // version
        0x01, 0x01, 0x00 // empty type section
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      assertNotNull(module, "Module should be created");
      assertTrue(module.isValid(), "Module should be valid");

      LOGGER.info("Module with empty section compiled successfully");
    }

    @Test
    @DisplayName("should validate module bytecode by successful compilation")
    void shouldValidateModuleBytecode(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

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
      resources.add(0, module);

      assertNotNull(module, "Module should not be null");
      assertTrue(module.isValid(), "Module should be valid");

      LOGGER.info("Module validation passed");
    }
  }

  @Nested
  @DisplayName("Invalid Module Tests")
  class InvalidModuleTests {

    @Test
    @DisplayName("should reject invalid magic number")
    void shouldRejectInvalidMagicNumber(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Invalid magic number
      final byte[] invalidMagic = {
        0x00, 0x00, 0x00, 0x00, // wrong magic
        0x01, 0x00, 0x00, 0x00 // version
      };

      assertThrows(
          WasmException.class,
          () -> engine.compileModule(invalidMagic),
          "Should reject invalid magic number");

      LOGGER.info("Invalid magic number rejected correctly");
    }

    @Test
    @DisplayName("should reject truncated module")
    void shouldRejectTruncatedModule(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Truncated - only magic, no version
      final byte[] truncated = {
        0x00, 0x61, 0x73, 0x6D // magic only
      };

      assertThrows(
          WasmException.class,
          () -> engine.compileModule(truncated),
          "Should reject truncated module");

      LOGGER.info("Truncated module rejected correctly");
    }

    @Test
    @DisplayName("should reject empty bytes")
    void shouldRejectEmptyBytes(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final byte[] empty = {};

      assertThrows(Exception.class, () -> engine.compileModule(empty), "Should reject empty bytes");

      LOGGER.info("Empty bytes rejected correctly");
    }

    @Test
    @DisplayName("should reject module with invalid section")
    void shouldRejectModuleWithInvalidSection(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

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

      LOGGER.info("Invalid section rejected correctly");
    }

    @Test
    @DisplayName("should fail validation for invalid bytecode")
    void shouldFailValidationForInvalidBytecode(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Invalid bytecode - wrong magic
      final byte[] invalidWasm = {
        0x01, 0x02, 0x03, 0x04,
        0x01, 0x00, 0x00, 0x00
      };

      // Invalid modules throw exceptions during compilation
      assertThrows(
          WasmException.class,
          () -> engine.compileModule(invalidWasm),
          "Invalid module should fail to compile");

      LOGGER.info("Invalid bytecode failed validation as expected");
    }
  }

  @Nested
  @DisplayName("Module Import Tests")
  class ModuleImportTests {

    @Test
    @DisplayName("should enumerate module imports")
    void shouldEnumerateModuleImports(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

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

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final List<ImportType> imports = module.getImports();
      assertNotNull(imports, "Imports should not be null");
      assertEquals(1, imports.size(), "Should have 1 import");

      final ImportType imp = imports.get(0);
      assertEquals("env", imp.getModuleName(), "Module name should be 'env'");
      assertEquals("log", imp.getName(), "Import name should be 'log'");

      LOGGER.info("Found import: " + imp.getModuleName() + "::" + imp.getName());
    }

    @Test
    @DisplayName("should enumerate memory import")
    void shouldEnumerateMemoryImport(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

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

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final List<ImportType> imports = module.getImports();
      assertEquals(1, imports.size(), "Should have 1 import");

      final ImportType imp = imports.get(0);
      assertEquals("env", imp.getModuleName(), "Module name should be 'env'");
      assertEquals("memory", imp.getName(), "Import name should be 'memory'");

      LOGGER.info("Found memory import: " + imp.getModuleName() + "::" + imp.getName());
    }
  }

  @Nested
  @DisplayName("Module Lifecycle Tests")
  class ModuleLifecycleTests {

    @Test
    @DisplayName("should handle module close and recreation")
    void shouldHandleModuleCloseAndRecreation(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

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
      resources.add(0, module);
      instance = store.createInstance(module);
      resources.add(0, instance);
      func = instance.getFunction("get").orElseThrow();
      assertEquals(10, func.call()[0].asInt(), "Second module should return 10");

      LOGGER.info("Module recreation handled correctly");
    }

    @Test
    @DisplayName("should create multiple instances from same module")
    void shouldCreateMultipleInstancesFromSameModule(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

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
        0x06, // code section, 6 bytes (1 count + 1 body_size + 4 body)
        0x01,
        0x04, // body size: 4 bytes (1 locals + 2 global.get + 1 end)
        0x00,
        0x23,
        0x00,
        0x0B // get: global.get 0
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      // Create two instances
      final Instance instance1 = store.createInstance(module);
      resources.add(0, instance1);
      final Instance instance2 = store.createInstance(module);
      resources.add(0, instance2);

      // Get globals from each instance
      final var global1 = instance1.getGlobal("global").orElseThrow();
      final var global2 = instance2.getGlobal("global").orElseThrow();

      // Set different values
      global1.set(WasmValue.i32(100));
      global2.set(WasmValue.i32(200));

      // Verify isolation
      assertEquals(100, global1.get().asInt(), "Instance 1 global should be 100");
      assertEquals(200, global2.get().asInt(), "Instance 2 global should be 200");

      LOGGER.info("Multiple instances from same module have isolated state");
    }
  }

  @Nested
  @DisplayName("Module Size Tests")
  class ModuleSizeTests {

    @Test
    @DisplayName("should handle module with many functions")
    void shouldHandleModuleWithManyFunctions(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

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

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      // Verify we can call the exported functions
      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      for (int i = 0; i < 3; i++) {
        final WasmFunction func = instance.getFunction("f" + i).orElseThrow();
        final WasmValue[] results = func.call();
        assertEquals(i, results[0].asInt(), "Function f" + i + " should return " + i);
      }

      LOGGER.info("Module with 10 functions handled correctly");
    }

    private void addBytes(final List<Byte> list, final int... values) {
      for (final int v : values) {
        list.add((byte) v);
      }
    }

    private byte[] toByteArray(final List<Byte> list) {
      final byte[] result = new byte[list.size()];
      for (int i = 0; i < list.size(); i++) {
        result[i] = list.get(i);
      }
      return result;
    }
  }
}

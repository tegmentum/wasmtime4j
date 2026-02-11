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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExportType;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for WebAssembly module and instance exports enumeration.
 *
 * <p>Tests the ability to enumerate and access various types of exports including functions,
 * memories, globals, and tables.
 *
 * @since 1.0.0
 */
@DisplayName("Module Exports Integration Tests")
public final class ModuleExportsIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ModuleExportsIntegrationTest.class.getName());

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
  @DisplayName("Function Export Tests")
  class FunctionExportTests {

    @Test
    @DisplayName("should enumerate single function export")
    void shouldEnumerateSingleFunctionExport(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Module with single function export: (func (export "add") ...)
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
        0x07, // type section
        0x01,
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x01,
        0x7F, // (i32, i32) -> i32
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07, // export section
        0x01,
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // "add"
        0x0A,
        0x09, // code section
        0x01,
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6A,
        0x0B // add function body
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final List<ExportType> exports = module.getExports();
      assertNotNull(exports, "Exports should not be null");
      assertEquals(1, exports.size(), "Should have 1 export");

      final ExportType export = exports.get(0);
      assertEquals("add", export.getName(), "Export name should be 'add'");
      assertEquals(WasmTypeKind.FUNCTION, export.getType().getKind(), "Should be a function");

      LOGGER.info("Found export: " + export.getName() + " of type " + export.getType().getKind());
    }

    @Test
    @DisplayName("should enumerate multiple function exports")
    void shouldEnumerateMultipleFunctionExports(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Module with multiple function exports
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
        0x0C, // type section, 12 bytes (1 count + 6 type0 + 5 type1)
        0x02, // 2 types
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x01,
        0x7F, // type 0: (i32, i32) -> i32
        0x60,
        0x01,
        0x7F,
        0x01,
        0x7F, // type 1: (i32) -> i32
        0x03,
        0x03, // function section
        0x02,
        0x00,
        0x01, // 2 functions: type 0, type 1
        0x07,
        0x0D, // export section
        0x02, // 2 exports
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // "add" func 0
        0x03,
        0x69,
        0x6E,
        0x63,
        0x00,
        0x01, // "inc" func 1
        0x0A,
        0x11, // code section, 17 bytes
        0x02, // 2 functions
        0x07, // func 1 body size: 7 bytes
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6A,
        0x0B, // add body: local.get 0, local.get 1, i32.add, end
        0x07, // func 2 body size: 7 bytes
        0x00,
        0x20,
        0x00,
        0x41,
        0x01,
        0x6A,
        0x0B // inc body: local.get 0, i32.const 1, i32.add, end
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final List<ExportType> exports = module.getExports();
      assertEquals(2, exports.size(), "Should have 2 exports");

      // Verify both exports are present
      boolean foundAdd = false;
      boolean foundInc = false;
      for (final ExportType export : exports) {
        if ("add".equals(export.getName())) {
          foundAdd = true;
        }
        if ("inc".equals(export.getName())) {
          foundInc = true;
        }
        assertEquals(WasmTypeKind.FUNCTION, export.getType().getKind(), "Should be function");
      }
      assertTrue(foundAdd, "Should have 'add' export");
      assertTrue(foundInc, "Should have 'inc' export");

      LOGGER.info("Found " + exports.size() + " function exports");
    }
  }

  @Nested
  @DisplayName("Memory Export Tests")
  class MemoryExportTests {

    @Test
    @DisplayName("should enumerate memory export")
    void shouldEnumerateMemoryExport(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Module with memory export: (memory (export "memory") 1)
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x05,
        0x03, // memory section
        0x01,
        0x00,
        0x01, // 1 memory, min 1 page
        0x07,
        0x0A, // export section
        0x01,
        0x06,
        0x6D,
        0x65,
        0x6D,
        0x6F,
        0x72,
        0x79,
        0x02,
        0x00 // "memory" mem 0
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final List<ExportType> exports = module.getExports();
      assertEquals(1, exports.size(), "Should have 1 export");

      final ExportType export = exports.get(0);
      assertEquals("memory", export.getName(), "Export name should be 'memory'");
      assertEquals(WasmTypeKind.MEMORY, export.getType().getKind(), "Should be a memory");

      LOGGER.info("Found memory export: " + export.getName());
    }

    @Test
    @DisplayName("should access memory export from instance")
    void shouldAccessMemoryExportFromInstance(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Module with memory export
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x05,
        0x03, // memory section
        0x01,
        0x00,
        0x01, // 1 memory, min 1 page
        0x07,
        0x0A, // export section
        0x01,
        0x06,
        0x6D,
        0x65,
        0x6D,
        0x6F,
        0x72,
        0x79,
        0x02,
        0x00 // "memory" mem 0
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
      assertTrue(memoryOpt.isPresent(), "Memory should be accessible");

      final WasmMemory memory = memoryOpt.get();
      assertNotNull(memory, "Memory should not be null");
      // getSize() returns pages (64KB each), not bytes
      assertTrue(memory.getSize() >= 1, "Memory should be at least 1 page");

      LOGGER.info(
          "Memory size: " + memory.getSize() + " pages (" + (memory.getSize() * 65536) + " bytes)");
    }
  }

  @Nested
  @DisplayName("Global Export Tests")
  class GlobalExportTests {

    @Test
    @DisplayName("should enumerate global export")
    void shouldEnumerateGlobalExport(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Module with global export: (global (export "counter") (mut i32) (i32.const 0))
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x06,
        0x06, // global section
        0x01, // 1 global
        0x7F,
        0x01, // i32, mutable
        0x41,
        0x00,
        0x0B, // i32.const 0, end
        0x07,
        0x0B, // export section
        0x01,
        0x07,
        0x63,
        0x6F,
        0x75,
        0x6E,
        0x74,
        0x65,
        0x72,
        0x03,
        0x00 // "counter" global 0
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final List<ExportType> exports = module.getExports();
      assertEquals(1, exports.size(), "Should have 1 export");

      final ExportType export = exports.get(0);
      assertEquals("counter", export.getName(), "Export name should be 'counter'");
      assertEquals(WasmTypeKind.GLOBAL, export.getType().getKind(), "Should be a global");

      LOGGER.info("Found global export: " + export.getName());
    }

    @Test
    @DisplayName("should access mutable global from instance")
    void shouldAccessMutableGlobalFromInstance(final TestInfo testInfo) throws Exception {
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
        0x06,
        0x06, // global section
        0x01,
        0x7F,
        0x01, // i32, mutable
        0x41,
        0x2A,
        0x0B, // i32.const 42, end
        0x07,
        0x0A, // export section
        0x01,
        0x06,
        0x6E,
        0x75,
        0x6D,
        0x62,
        0x65,
        0x72,
        0x03,
        0x00 // "number" global 0
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final Optional<WasmGlobal> globalOpt = instance.getGlobal("number");
      assertTrue(globalOpt.isPresent(), "Global should be accessible");

      final WasmGlobal global = globalOpt.get();
      assertNotNull(global, "Global should not be null");

      final WasmValue value = global.get();
      assertEquals(42, value.asInt(), "Global should have initial value 42");

      // Set new value
      global.set(WasmValue.i32(100));
      assertEquals(100, global.get().asInt(), "Global should have new value 100");

      LOGGER.info("Global value: " + global.get().asInt());
    }

    @Test
    @DisplayName("should access immutable global from instance")
    void shouldAccessImmutableGlobalFromInstance(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Module with immutable global
      // Note: 99 in signed LEB128 requires 0xE3, 0x00 (since 99 > 63, it needs the extra byte
      // to avoid sign-extension making it negative)
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x06,
        0x07, // global section, 7 bytes
        0x01,
        0x7F,
        0x00, // i32, immutable
        0x41,
        (byte) 0xE3,
        0x00,
        0x0B, // i32.const 99, end (99 = 0xE3, 0x00 in signed LEB128)
        0x07,
        0x0A, // export section
        0x01,
        0x06,
        0x63,
        0x6F,
        0x6E,
        0x73,
        0x74,
        0x73,
        0x03,
        0x00 // "consts" global 0
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final Optional<WasmGlobal> globalOpt = instance.getGlobal("consts");
      assertTrue(globalOpt.isPresent(), "Global should be accessible");

      final WasmGlobal global = globalOpt.get();
      assertEquals(99, global.get().asInt(), "Global should have value 99");
      assertFalse(global.isMutable(), "Global should be immutable");

      LOGGER.info("Immutable global value: " + global.get().asInt());
    }
  }

  @Nested
  @DisplayName("Table Export Tests")
  class TableExportTests {

    @Test
    @DisplayName("should enumerate table export")
    void shouldEnumerateTableExport(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Module with table export: (table (export "table") 1 funcref)
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x04,
        0x04, // table section
        0x01, // 1 table
        0x70, // funcref
        0x00,
        0x01, // min 1
        0x07,
        0x09, // export section
        0x01,
        0x05,
        0x74,
        0x61,
        0x62,
        0x6C,
        0x65,
        0x01,
        0x00 // "table" table 0
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final List<ExportType> exports = module.getExports();
      assertEquals(1, exports.size(), "Should have 1 export");

      final ExportType export = exports.get(0);
      assertEquals("table", export.getName(), "Export name should be 'table'");
      assertEquals(WasmTypeKind.TABLE, export.getType().getKind(), "Should be a table");

      LOGGER.info("Found table export: " + export.getName());
    }

    @Test
    @DisplayName("should access table export from instance")
    void shouldAccessTableExportFromInstance(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Module with table export
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x04,
        0x04, // table section
        0x01,
        0x70,
        0x00,
        0x04, // 1 funcref table, min 4
        0x07,
        0x0A, // export section
        0x01,
        0x06,
        0x6D,
        0x79,
        0x74,
        0x61,
        0x62,
        0x6C,
        0x01,
        0x00 // "mytabl" table 0
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final Instance instance = store.createInstance(module);
      resources.add(0, instance);

      final Optional<WasmTable> tableOpt = instance.getTable("mytabl");
      assertTrue(tableOpt.isPresent(), "Table should be accessible");

      final WasmTable table = tableOpt.get();
      assertNotNull(table, "Table should not be null");
      assertTrue(table.getSize() >= 4, "Table size should be at least 4");

      LOGGER.info("Table size: " + table.getSize());
    }
  }

  @Nested
  @DisplayName("Mixed Export Tests")
  class MixedExportTests {

    @Test
    @DisplayName("should enumerate all export types")
    void shouldEnumerateAllExportTypes(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Module with function, memory, global, and table exports
      final byte[] wasm = {
        0x00,
        0x61,
        0x73,
        0x6D, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        // Type section
        0x01,
        0x05,
        0x01,
        0x60,
        0x00,
        0x01,
        0x7F, // () -> i32
        // Function section
        0x03,
        0x02,
        0x01,
        0x00, // 1 function of type 0
        // Table section
        0x04,
        0x04,
        0x01,
        0x70,
        0x00,
        0x01, // 1 funcref table, min 1
        // Memory section
        0x05,
        0x03,
        0x01,
        0x00,
        0x01, // 1 memory, min 1 page
        // Global section
        0x06,
        0x06,
        0x01,
        0x7F,
        0x00,
        0x41,
        0x05,
        0x0B, // 1 i32 immutable global = 5
        // Export section
        0x07,
        0x1A, // 26 bytes (1 count + 7 func + 6 mem + 6 tbl + 6 glb)
        0x04, // 4 exports
        0x04,
        0x66,
        0x75,
        0x6E,
        0x63,
        0x00,
        0x00, // "func" function 0
        0x03,
        0x6D,
        0x65,
        0x6D,
        0x02,
        0x00, // "mem" memory 0
        0x03,
        0x74,
        0x62,
        0x6C,
        0x01,
        0x00, // "tbl" table 0
        0x03,
        0x67,
        0x6C,
        0x62,
        0x03,
        0x00, // "glb" global 0
        // Code section
        0x0A,
        0x06,
        0x01,
        0x04,
        0x00,
        0x41,
        0x2A,
        0x0B // function body: i32.const 42
      };

      final Module module = engine.compileModule(wasm);
      resources.add(0, module);

      final List<ExportType> exports = module.getExports();
      assertEquals(4, exports.size(), "Should have 4 exports");

      // Count each type
      int funcCount = 0;
      int memCount = 0;
      int tableCount = 0;
      int globalCount = 0;

      for (final ExportType export : exports) {
        switch (export.getType().getKind()) {
          case FUNCTION:
            funcCount++;
            break;
          case MEMORY:
            memCount++;
            break;
          case TABLE:
            tableCount++;
            break;
          case GLOBAL:
            globalCount++;
            break;
          default:
            break;
        }
        LOGGER.info("Export: " + export.getName() + " -> " + export.getType().getKind());
      }

      assertEquals(1, funcCount, "Should have 1 function export");
      assertEquals(1, memCount, "Should have 1 memory export");
      assertEquals(1, tableCount, "Should have 1 table export");
      assertEquals(1, globalCount, "Should have 1 global export");

      LOGGER.info("All export types enumerated correctly");
    }
  }
}

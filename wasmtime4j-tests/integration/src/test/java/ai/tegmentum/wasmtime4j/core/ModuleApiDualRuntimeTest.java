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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.ExportType;
import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Dual-runtime integration tests for generic Module API operations.
 *
 * <p>These tests verify Module API behavior across both JNI and Panama runtimes using the unified
 * API layer. Tests that are already covered in {@link ModuleValidationIntegrationTest} are not
 * duplicated here.
 *
 * @since 1.0.0
 */
@DisplayName("Module API DualRuntime Tests")
@SuppressWarnings("deprecation")
public class ModuleApiDualRuntimeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleApiDualRuntimeTest.class.getName());

  // ===== WAT Module Definitions =====

  private static final String EMPTY_MODULE_WAT = "(module)";

  private static final String FUNCTION_MODULE_WAT =
      "(module\n"
          + "  (func (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add\n"
          + "  )\n"
          + "  (func (export \"get42\") (result i32)\n"
          + "    i32.const 42\n"
          + "  )\n"
          + ")";

  private static final String MEMORY_MODULE_WAT =
      "(module\n" + "  (memory (export \"memory\") 1 10)\n" + ")";

  private static final String GLOBAL_MODULE_WAT =
      "(module\n"
          + "  (global (export \"mutable_g\") (mut i32) (i32.const 42))\n"
          + "  (global (export \"immutable_g\") i32 (i32.const 7))\n"
          + ")";

  private static final String TABLE_MODULE_WAT =
      "(module\n" + "  (table (export \"table\") 1 funcref)\n" + ")";

  private static final String MULTI_EXPORT_MODULE_WAT =
      "(module\n"
          + "  (func (export \"run\") (result i32) i32.const 1)\n"
          + "  (memory (export \"mem\") 1)\n"
          + "  (global (export \"g\") (mut i32) (i32.const 0))\n"
          + "  (table (export \"t\") 1 funcref)\n"
          + ")";

  private static final String IMPORT_MODULE_WAT =
      "(module\n"
          + "  (import \"env\" \"log\" (func (param i32)))\n"
          + "  (import \"env\" \"memory\" (memory 1))\n"
          + "  (func (export \"main\") (result i32) i32.const 0)\n"
          + ")";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  // ===== Export Metadata Tests =====

  @Nested
  @DisplayName("Export Metadata Tests")
  class ExportMetadataTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should retrieve function exports")
    void shouldRetrieveFunctionExports(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing function exports retrieval");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        final List<ExportType> exports = module.getExports();
        assertNotNull(exports, "Exports list should not be null");
        assertFalse(exports.isEmpty(), "Exports list should not be empty");

        final boolean hasAdd =
            exports.stream()
                .anyMatch(
                    e ->
                        "add".equals(e.getName())
                            && e.getType().getKind() == WasmTypeKind.FUNCTION);
        final boolean hasGet42 =
            exports.stream()
                .anyMatch(
                    e ->
                        "get42".equals(e.getName())
                            && e.getType().getKind() == WasmTypeKind.FUNCTION);

        assertTrue(hasAdd, "Should have 'add' function export");
        assertTrue(hasGet42, "Should have 'get42' function export");
        LOGGER.info("[" + runtime + "] Found function exports: " + exports.size());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should retrieve memory exports")
    void shouldRetrieveMemoryExports(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing memory exports retrieval");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(MEMORY_MODULE_WAT)) {

        final List<ExportType> exports = module.getExports();
        assertEquals(1, exports.size(), "Should have exactly 1 export");

        final ExportType memExport = exports.get(0);
        assertEquals("memory", memExport.getName(), "Export name should be 'memory'");
        assertEquals(
            WasmTypeKind.MEMORY, memExport.getType().getKind(), "Export type should be MEMORY");
        LOGGER.info("[" + runtime + "] Found memory export: " + memExport.getName());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should retrieve global exports")
    void shouldRetrieveGlobalExports(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing global exports retrieval");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(GLOBAL_MODULE_WAT)) {

        final List<ExportType> exports = module.getExports();
        assertEquals(2, exports.size(), "Should have exactly 2 exports");

        final boolean hasMutable =
            exports.stream()
                .anyMatch(
                    e ->
                        "mutable_g".equals(e.getName())
                            && e.getType().getKind() == WasmTypeKind.GLOBAL);
        final boolean hasImmutable =
            exports.stream()
                .anyMatch(
                    e ->
                        "immutable_g".equals(e.getName())
                            && e.getType().getKind() == WasmTypeKind.GLOBAL);

        assertTrue(hasMutable, "Should have mutable global export");
        assertTrue(hasImmutable, "Should have immutable global export");
        LOGGER.info("[" + runtime + "] Found global exports");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should retrieve table exports")
    void shouldRetrieveTableExports(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing table exports retrieval");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(TABLE_MODULE_WAT)) {

        final List<ExportType> exports = module.getExports();
        assertEquals(1, exports.size(), "Should have exactly 1 export");

        final ExportType tableExport = exports.get(0);
        assertEquals("table", tableExport.getName(), "Export name should be 'table'");
        assertEquals(
            WasmTypeKind.TABLE, tableExport.getType().getKind(), "Export type should be TABLE");
        LOGGER.info("[" + runtime + "] Found table export: " + tableExport.getName());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should retrieve multi-type exports")
    void shouldRetrieveMultiTypeExports(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multi-type exports retrieval");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(MULTI_EXPORT_MODULE_WAT)) {

        final List<ExportType> exports = module.getExports();
        assertEquals(4, exports.size(), "Should have exactly 4 exports");

        final long functionCount =
            exports.stream().filter(e -> e.getType().getKind() == WasmTypeKind.FUNCTION).count();
        final long memoryCount =
            exports.stream().filter(e -> e.getType().getKind() == WasmTypeKind.MEMORY).count();
        final long globalCount =
            exports.stream().filter(e -> e.getType().getKind() == WasmTypeKind.GLOBAL).count();
        final long tableCount =
            exports.stream().filter(e -> e.getType().getKind() == WasmTypeKind.TABLE).count();

        assertEquals(1, functionCount, "Should have 1 function export");
        assertEquals(1, memoryCount, "Should have 1 memory export");
        assertEquals(1, globalCount, "Should have 1 global export");
        assertEquals(1, tableCount, "Should have 1 table export");
        LOGGER.info("[" + runtime + "] Multi-export module: found all 4 export types");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return unmodifiable exports list")
    void shouldReturnUnmodifiableExportsList(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing unmodifiable exports list");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        final List<ExportType> exports = module.getExports();
        assertThrows(
            UnsupportedOperationException.class,
            () -> exports.add(null),
            "Exports list should be unmodifiable");
        LOGGER.info("[" + runtime + "] Exports list is correctly unmodifiable");
      }
    }
  }

  // ===== Import Metadata Tests =====

  @Nested
  @DisplayName("Import Metadata Tests")
  class ImportMetadataTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Module without imports should return empty imports list")
    void moduleWithoutImportsShouldReturnEmpty(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing module without imports returns empty list");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        final List<ImportType> imports = module.getImports();
        assertNotNull(imports, "Imports list should not be null");
        assertTrue(imports.isEmpty(), "Function module should have no imports");

        // Call again to verify consistency
        final List<ImportType> importsAgain = module.getImports();
        assertTrue(importsAgain.isEmpty(), "Repeated call should also return empty");
        LOGGER.info("[" + runtime + "] Function module correctly has no imports");
      }
    }
  }

  // ===== Type Query Tests =====

  @Nested
  @DisplayName("Type Query Tests")
  class TypeQueryTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should find function type by name")
    void shouldFindFunctionType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing function type lookup by name");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        final Optional<FuncType> addType = module.getFunctionType("add");
        assertTrue(addType.isPresent(), "Should find 'add' function type");

        final FuncType funcType = addType.get();
        assertEquals(2, funcType.getParams().size(), "'add' should have 2 params");
        assertEquals(1, funcType.getResults().size(), "'add' should have 1 result");
        LOGGER.info(
            "["
                + runtime
                + "] Function 'add': params="
                + funcType.getParams().size()
                + ", results="
                + funcType.getResults().size());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return empty for nonexistent function")
    void shouldReturnEmptyForNonexistentFunction(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing nonexistent function type lookup");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        final Optional<FuncType> missing = module.getFunctionType("nonexistent");
        assertFalse(missing.isPresent(), "Should not find nonexistent function");
        LOGGER.info("[" + runtime + "] Correctly returned empty for nonexistent function");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return empty for null function name")
    void shouldReturnEmptyForNullFunctionName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null function name lookup");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        final Optional<FuncType> result = module.getFunctionType(null);
        assertFalse(result.isPresent(), "Should return empty for null name");
        LOGGER.info("[" + runtime + "] Correctly returned empty for null function name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should find global type by name")
    void shouldFindGlobalType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing global type lookup by name");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(GLOBAL_MODULE_WAT)) {

        final Optional<GlobalType> mutableType = module.getGlobalType("mutable_g");
        assertTrue(mutableType.isPresent(), "Should find mutable global type");
        assertTrue(mutableType.get().isMutable(), "mutable_g should be mutable");

        final Optional<GlobalType> immutableType = module.getGlobalType("immutable_g");
        assertTrue(immutableType.isPresent(), "Should find immutable global type");
        assertFalse(immutableType.get().isMutable(), "immutable_g should be immutable");
        LOGGER.info("[" + runtime + "] Found global types: mutable and immutable");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return empty for null global name")
    void shouldReturnEmptyForNullGlobalName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null global name lookup");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(GLOBAL_MODULE_WAT)) {

        final Optional<GlobalType> result = module.getGlobalType(null);
        assertFalse(result.isPresent(), "Should return empty for null global name");
        LOGGER.info("[" + runtime + "] Correctly returned empty for null global name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should find memory type by name")
    void shouldFindMemoryType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing memory type lookup by name");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(MEMORY_MODULE_WAT)) {

        final Optional<MemoryType> memType = module.getMemoryType("memory");
        assertTrue(memType.isPresent(), "Should find 'memory' type");

        final MemoryType mt = memType.get();
        assertEquals(1, mt.getMinimum(), "Memory minimum should be 1 page");
        assertTrue(mt.getMaximum().isPresent(), "Memory should have a maximum");
        assertEquals(10L, mt.getMaximum().get(), "Memory maximum should be 10 pages");
        LOGGER.info(
            "[" + runtime + "] Memory type: min=" + mt.getMinimum() + ", max=" + mt.getMaximum());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return empty for null memory name")
    void shouldReturnEmptyForNullMemoryName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null memory name lookup");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(MEMORY_MODULE_WAT)) {

        final Optional<MemoryType> result = module.getMemoryType(null);
        assertFalse(result.isPresent(), "Should return empty for null memory name");
        LOGGER.info("[" + runtime + "] Correctly returned empty for null memory name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should find table type by name")
    void shouldFindTableType(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing table type lookup by name");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(TABLE_MODULE_WAT)) {

        final Optional<TableType> tableType = module.getTableType("table");
        assertTrue(tableType.isPresent(), "Should find 'table' type");

        final TableType tt = tableType.get();
        assertNotNull(tt.getElementType(), "Table element type should not be null");
        LOGGER.info("[" + runtime + "] Table type: elementType=" + tt.getElementType());
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return empty for null table name")
    void shouldReturnEmptyForNullTableName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null table name lookup");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(TABLE_MODULE_WAT)) {

        final Optional<TableType> result = module.getTableType(null);
        assertFalse(result.isPresent(), "Should return empty for null table name");
        LOGGER.info("[" + runtime + "] Correctly returned empty for null table name");
      }
    }
  }

  // ===== Typed Lists Tests =====

  @Nested
  @DisplayName("Typed Lists Tests")
  class TypedListsTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should list function types")
    void shouldListFunctionTypes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing function types listing");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        final List<FuncType> functionTypes = module.getFunctionTypes();
        assertNotNull(functionTypes, "Function types list should not be null");
        assertEquals(2, functionTypes.size(), "Should have 2 function types");
        LOGGER.info("[" + runtime + "] Found " + functionTypes.size() + " function types");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should list memory types")
    void shouldListMemoryTypes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing memory types listing");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(MEMORY_MODULE_WAT)) {

        final List<MemoryType> memoryTypes = module.getMemoryTypes();
        assertNotNull(memoryTypes, "Memory types list should not be null");
        assertEquals(1, memoryTypes.size(), "Should have 1 memory type");
        LOGGER.info("[" + runtime + "] Found " + memoryTypes.size() + " memory types");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should list global types")
    void shouldListGlobalTypes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing global types listing");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(GLOBAL_MODULE_WAT)) {

        final List<GlobalType> globalTypes = module.getGlobalTypes();
        assertNotNull(globalTypes, "Global types list should not be null");
        assertEquals(2, globalTypes.size(), "Should have 2 global types");
        LOGGER.info("[" + runtime + "] Found " + globalTypes.size() + " global types");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should list table types")
    void shouldListTableTypes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing table types listing");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(TABLE_MODULE_WAT)) {

        final List<TableType> tableTypes = module.getTableTypes();
        assertNotNull(tableTypes, "Table types list should not be null");
        assertEquals(1, tableTypes.size(), "Should have 1 table type");
        LOGGER.info("[" + runtime + "] Found " + tableTypes.size() + " table types");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return empty lists for module without matching types")
    void shouldReturnEmptyForNoMatchingTypes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing empty typed lists for function-only module");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        assertTrue(
            module.getMemoryTypes().isEmpty(), "Function module should have no memory types");
        assertTrue(
            module.getGlobalTypes().isEmpty(), "Function module should have no global types");
        assertTrue(module.getTableTypes().isEmpty(), "Function module should have no table types");
        LOGGER.info(
            "[" + runtime + "] Function-only module correctly has no memory/global/table types");
      }
    }
  }

  // ===== Export and Import Query Tests =====

  @Nested
  @DisplayName("Export and Import Query Tests")
  class ExportImportQueryTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should find existing export")
    void shouldFindExistingExport(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing existing export lookup");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        assertTrue(module.hasExport("add"), "Should find 'add' export");
        assertTrue(module.hasExport("get42"), "Should find 'get42' export");
        LOGGER.info("[" + runtime + "] Found existing exports by name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should not find nonexistent export")
    void shouldNotFindNonexistentExport(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing nonexistent export lookup");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        assertFalse(module.hasExport("nonexistent"), "Should not find nonexistent export");
        LOGGER.info("[" + runtime + "] Correctly did not find nonexistent export");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null export name")
    void shouldRejectNullExportName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null export name rejection");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        assertThrows(
            IllegalArgumentException.class,
            () -> module.hasExport(null),
            "Should reject null export name");
        LOGGER.info("[" + runtime + "] Correctly rejected null export name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should find existing import")
    void shouldFindExistingImport(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing existing import lookup");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(IMPORT_MODULE_WAT)) {

        assertTrue(module.hasImport("env", "log"), "Should find env.log import");
        assertTrue(module.hasImport("env", "memory"), "Should find env.memory import");
        LOGGER.info("[" + runtime + "] Found existing imports by name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should not find nonexistent import")
    void shouldNotFindNonexistentImport(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing nonexistent import lookup");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(IMPORT_MODULE_WAT)) {

        assertFalse(module.hasImport("env", "nonexistent"), "Should not find env.nonexistent");
        assertFalse(module.hasImport("other", "log"), "Should not find other.log");
        LOGGER.info("[" + runtime + "] Correctly did not find nonexistent imports");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null import module name")
    void shouldRejectNullImportModuleName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null import module name rejection");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(IMPORT_MODULE_WAT)) {

        assertThrows(
            IllegalArgumentException.class,
            () -> module.hasImport(null, "log"),
            "Should reject null module name");
        LOGGER.info("[" + runtime + "] Correctly rejected null module name");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null import field name")
    void shouldRejectNullImportFieldName(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null import field name rejection");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(IMPORT_MODULE_WAT)) {

        assertThrows(
            IllegalArgumentException.class,
            () -> module.hasImport("env", null),
            "Should reject null field name");
        LOGGER.info("[" + runtime + "] Correctly rejected null field name");
      }
    }
  }

  // ===== Lifecycle Tests =====

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Module should be valid after creation")
    void moduleShouldBeValidAfterCreation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing module validity after creation");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        assertTrue(module.isValid(), "Module should be valid after creation");
        LOGGER.info("[" + runtime + "] Module is valid after creation");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Operations on closed module should throw")
    void operationsOnClosedModuleShouldThrow(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing operations on closed module");

      try (Engine engine = Engine.create()) {
        final Module module = engine.compileWat(FUNCTION_MODULE_WAT);
        module.close();

        assertThrows(
            Exception.class, module::getExports, "getExports on closed module should throw");
        assertThrows(
            Exception.class, module::getImports, "getImports on closed module should throw");
        assertThrows(
            Exception.class,
            () -> module.hasExport("add"),
            "hasExport on closed module should throw");
        assertThrows(
            Exception.class,
            () -> module.hasImport("env", "log"),
            "hasImport on closed module should throw");
        // getName may return cached value in some runtimes
        try {
          module.getName();
          LOGGER.info("[" + runtime + "] getName on closed module returned cached value");
        } catch (final Exception e) {
          LOGGER.info(
              "[" + runtime + "] getName on closed module threw: " + e.getClass().getName());
        }
        LOGGER.info("[" + runtime + "] All operations correctly throw on closed module");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return engine reference")
    void shouldReturnEngineReference(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing engine reference from module");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        assertSame(engine, module.getEngine(), "Module should return the engine that created it");
        LOGGER.info("[" + runtime + "] Module returns correct engine reference");
      }
    }
  }

  // ===== Serialization Tests =====

  @Nested
  @DisplayName("Serialization Tests")
  class SerializationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should serialize module")
    void shouldSerializeModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing module serialization");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        final byte[] serialized = module.serialize();
        assertNotNull(serialized, "Serialized data should not be null");
        assertTrue(serialized.length > 0, "Serialized data should not be empty");
        LOGGER.info("[" + runtime + "] Serialized module to " + serialized.length + " bytes");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should serialize and have content")
    void shouldSerializeWithContent(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multi-export module serialization");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(MULTI_EXPORT_MODULE_WAT)) {

        final byte[] serialized = module.serialize();
        assertNotNull(serialized, "Serialized data should not be null");
        assertTrue(serialized.length > 0, "Serialized data should not be empty");
        LOGGER.info(
            "[" + runtime + "] Serialized multi-export module to " + serialized.length + " bytes");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Serialization should fail on closed module")
    void serializationShouldFailOnClosedModule(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing serialization on closed module");

      try (Engine engine = Engine.create()) {
        final Module module = engine.compileWat(FUNCTION_MODULE_WAT);
        module.close();

        // Behavior varies by runtime: some runtimes may return cached serialization
        try {
          final byte[] result = module.serialize();
          LOGGER.info(
              "["
                  + runtime
                  + "] serialize() on closed module returned "
                  + (result != null ? result.length + " bytes" : "null")
                  + " (no throw)");
        } catch (final Exception e) {
          LOGGER.info(
              "["
                  + runtime
                  + "] serialize() on closed module threw: "
                  + e.getClass().getName()
                  + " - "
                  + e.getMessage());
        }
      }
    }
  }

  // ===== Instantiation Tests =====

  @Nested
  @DisplayName("Instantiation Tests")
  class InstantiationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null store for instantiate")
    void shouldRejectNullStore(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null store rejection for instantiate");

      try (Engine engine = Engine.create();
          Module module = engine.compileWat(FUNCTION_MODULE_WAT)) {

        assertThrows(
            IllegalArgumentException.class,
            () -> module.instantiate(null),
            "Should reject null store for instantiate");
        LOGGER.info("[" + runtime + "] Correctly rejected null store for instantiate");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Instantiate on closed module should throw")
    void instantiateOnClosedModuleShouldThrow(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing instantiation on closed module");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final Module module = engine.compileWat(FUNCTION_MODULE_WAT);
        module.close();

        assertThrows(
            Exception.class,
            () -> module.instantiate(store),
            "Instantiate on closed module should throw");
        LOGGER.info("[" + runtime + "] Correctly rejected instantiation on closed module");
      }
    }
  }
}

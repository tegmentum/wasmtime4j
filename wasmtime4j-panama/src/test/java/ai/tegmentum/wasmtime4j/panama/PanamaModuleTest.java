package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ExportDescriptor;
import ai.tegmentum.wasmtime4j.ExportType;
import ai.tegmentum.wasmtime4j.FuncType;
import ai.tegmentum.wasmtime4j.GlobalType;
import ai.tegmentum.wasmtime4j.ImportDescriptor;
import ai.tegmentum.wasmtime4j.ImportType;
import ai.tegmentum.wasmtime4j.MemoryType;
import ai.tegmentum.wasmtime4j.ModuleExport;
import ai.tegmentum.wasmtime4j.ModuleImport;
import ai.tegmentum.wasmtime4j.TableType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaModule} that exercise actual native compilation and metadata retrieval.
 *
 * <p>These tests create real PanamaEngine and PanamaModule instances via native library calls,
 * exercising constructor logic, module metadata parsing, type introspection, and lifecycle
 * management.
 */
@DisplayName("PanamaModule Tests")
class PanamaModuleTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaModuleTest.class.getName());

  private PanamaEngine engine;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    engine = new PanamaEngine();
    resources.add(engine);
    LOGGER.info("Created PanamaEngine for test");
  }

  @AfterEach
  void tearDown() {
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  private PanamaModule compileWat(final String wat) throws Exception {
    final PanamaModule module = (PanamaModule) engine.compileWat(wat);
    resources.add(module);
    return module;
  }

  // ===== WAT Module Definitions =====

  private static final String EMPTY_MODULE_WAT = "(module)";

  private static final String FUNCTION_MODULE_WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32)
          local.get 0
          local.get 1
          i32.add
        )
        (func (export "get42") (result i32)
          i32.const 42
        )
      )
      """;

  private static final String MEMORY_MODULE_WAT =
      """
      (module
        (memory (export "memory") 1 10)
      )
      """;

  private static final String GLOBAL_MODULE_WAT =
      """
      (module
        (global (export "mutable_g") (mut i32) (i32.const 42))
        (global (export "immutable_g") i32 (i32.const 7))
      )
      """;

  private static final String TABLE_MODULE_WAT =
      """
      (module
        (table (export "table") 1 funcref)
      )
      """;

  private static final String MULTI_EXPORT_MODULE_WAT =
      """
      (module
        (func (export "run") (result i32) i32.const 1)
        (memory (export "mem") 1)
        (global (export "g") (mut i32) (i32.const 0))
        (table (export "t") 1 funcref)
      )
      """;

  private static final String IMPORT_MODULE_WAT =
      """
      (module
        (import "env" "log" (func (param i32)))
        (import "env" "memory" (memory 1))
        (func (export "main") (result i32) i32.const 0)
      )
      """;

  // ===== Constructor Tests =====

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Should reject null engine")
    void shouldRejectNullEngine() throws Exception {
      final byte[] validWasm = PanamaTestUtils.createModuleWithImmutableI32Global(1);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new PanamaModule(null, validWasm));

      assertThat(ex.getMessage()).contains("Engine cannot be null");
      LOGGER.info("Correctly rejected null engine: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject null WASM bytes")
    void shouldRejectNullWasmBytes() throws Exception {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> new PanamaModule(engine, (byte[]) null));

      assertThat(ex.getMessage()).contains("WASM bytes cannot be null or empty");
      LOGGER.info("Correctly rejected null bytes: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject empty WASM bytes")
    void shouldRejectEmptyWasmBytes() throws Exception {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new PanamaModule(engine, new byte[0]));

      assertThat(ex.getMessage()).contains("WASM bytes cannot be null or empty");
      LOGGER.info("Correctly rejected empty bytes: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject closed engine")
    void shouldRejectClosedEngine() throws Exception {
      final PanamaEngine closedEngine = new PanamaEngine();
      closedEngine.close();

      assertThrows(
          IllegalStateException.class,
          () ->
              new PanamaModule(
                  closedEngine, PanamaTestUtils.createModuleWithImmutableI32Global(1)));
      LOGGER.info("Correctly rejected closed engine");
    }

    @Test
    @DisplayName("Should compile valid WAT module with global")
    void shouldCompileValidWasmBytes() throws Exception {
      final PanamaModule module = compileWat(GLOBAL_MODULE_WAT);

      assertTrue(module.isValid(), "Module should be valid after compilation");
      assertNotNull(module.getNativeModule(), "Native module pointer should not be null");
      LOGGER.info("Successfully compiled valid WAT module with global");
    }

    @Test
    @DisplayName("Should compile from WAT source")
    void shouldCompileFromWat() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      assertTrue(module.isValid(), "Module should be valid after WAT compilation");
      assertNotNull(module.getNativeModule(), "Native module pointer should not be null");
      LOGGER.info("Successfully compiled WAT source");
    }
  }

  // ===== Module Export Metadata Tests =====

  @Nested
  @DisplayName("Export Metadata Tests")
  class ExportMetadataTests {

    @Test
    @DisplayName("Empty module should have no exports")
    void emptyModuleShouldHaveNoExports() throws Exception {
      final PanamaModule module = compileWat(EMPTY_MODULE_WAT);

      final List<ExportType> exports = module.getExports();
      assertThat(exports).isEmpty();

      final List<ModuleExport> moduleExports = module.getModuleExports();
      assertThat(moduleExports).isEmpty();
      LOGGER.info("Empty module correctly has no exports");
    }

    @Test
    @DisplayName("Should retrieve function exports")
    void shouldRetrieveFunctionExports() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final List<ExportType> exports = module.getExports();
      assertThat(exports).isNotEmpty();

      // Should have "add" and "get42" functions
      final boolean hasAdd =
          exports.stream()
              .anyMatch(
                  e -> "add".equals(e.getName()) && e.getType().getKind() == WasmTypeKind.FUNCTION);
      final boolean hasGet42 =
          exports.stream()
              .anyMatch(
                  e ->
                      "get42".equals(e.getName())
                          && e.getType().getKind() == WasmTypeKind.FUNCTION);

      assertTrue(hasAdd, "Should have 'add' function export");
      assertTrue(hasGet42, "Should have 'get42' function export");
      LOGGER.info("Found function exports: " + exports.size());
    }

    @Test
    @DisplayName("Should retrieve memory exports")
    void shouldRetrieveMemoryExports() throws Exception {
      final PanamaModule module = compileWat(MEMORY_MODULE_WAT);

      final List<ExportType> exports = module.getExports();
      assertThat(exports).hasSize(1);

      final ExportType memExport = exports.get(0);
      assertEquals("memory", memExport.getName());
      assertEquals(WasmTypeKind.MEMORY, memExport.getType().getKind());
      LOGGER.info("Found memory export: " + memExport.getName());
    }

    @Test
    @DisplayName("Should retrieve global exports")
    void shouldRetrieveGlobalExports() throws Exception {
      final PanamaModule module = compileWat(GLOBAL_MODULE_WAT);

      final List<ExportType> exports = module.getExports();
      assertThat(exports).hasSize(2);

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
      LOGGER.info("Found global exports");
    }

    @Test
    @DisplayName("Should retrieve table exports")
    void shouldRetrieveTableExports() throws Exception {
      final PanamaModule module = compileWat(TABLE_MODULE_WAT);

      final List<ExportType> exports = module.getExports();
      assertThat(exports).hasSize(1);

      final ExportType tableExport = exports.get(0);
      assertEquals("table", tableExport.getName());
      assertEquals(WasmTypeKind.TABLE, tableExport.getType().getKind());
      LOGGER.info("Found table export: " + tableExport.getName());
    }

    @Test
    @DisplayName("Should retrieve multi-type exports")
    void shouldRetrieveMultiTypeExports() throws Exception {
      final PanamaModule module = compileWat(MULTI_EXPORT_MODULE_WAT);

      final List<ExportType> exports = module.getExports();
      assertThat(exports).hasSize(4);

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
      LOGGER.info("Multi-export module: found all 4 export types");
    }

    @Test
    @DisplayName("Should retrieve export descriptors")
    void shouldRetrieveExportDescriptors() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final List<ExportDescriptor> descriptors = module.getExportDescriptors();
      assertThat(descriptors).isNotEmpty();

      for (final ExportDescriptor desc : descriptors) {
        assertNotNull(desc.getName(), "Export descriptor name should not be null");
        assertNotNull(desc.getType(), "Export descriptor type should not be null");
        LOGGER.info("Export descriptor: " + desc.getName() + " kind=" + desc.getType().getKind());
      }
    }

    @Test
    @DisplayName("Should return unmodifiable exports list")
    void shouldReturnUnmodifiableExportsList() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final List<ExportType> exports = module.getExports();
      assertThrows(UnsupportedOperationException.class, () -> exports.add(null));
      LOGGER.info("Exports list is correctly unmodifiable");
    }
  }

  // ===== Module Import Metadata Tests =====

  @Nested
  @DisplayName("Import Metadata Tests")
  class ImportMetadataTests {

    @Test
    @DisplayName("Empty module should have no imports")
    void emptyModuleShouldHaveNoImports() throws Exception {
      final PanamaModule module = compileWat(EMPTY_MODULE_WAT);

      final List<ImportType> imports = module.getImports();
      assertThat(imports).isEmpty();

      final List<ModuleImport> moduleImports = module.getModuleImports();
      assertThat(moduleImports).isEmpty();
      LOGGER.info("Empty module correctly has no imports");
    }

    @Test
    @DisplayName("Should retrieve function and memory imports")
    void shouldRetrieveImports() throws Exception {
      final PanamaModule module = compileWat(IMPORT_MODULE_WAT);

      final List<ImportType> imports = module.getImports();
      assertThat(imports).hasSize(2);

      final boolean hasFuncImport =
          imports.stream()
              .anyMatch(
                  i ->
                      "env".equals(i.getModuleName())
                          && "log".equals(i.getName())
                          && i.getType().getKind() == WasmTypeKind.FUNCTION);
      final boolean hasMemImport =
          imports.stream()
              .anyMatch(
                  i ->
                      "env".equals(i.getModuleName())
                          && "memory".equals(i.getName())
                          && i.getType().getKind() == WasmTypeKind.MEMORY);

      assertTrue(hasFuncImport, "Should have function import env.log");
      assertTrue(hasMemImport, "Should have memory import env.memory");
      LOGGER.info("Found imports: func=" + hasFuncImport + ", mem=" + hasMemImport);
    }

    @Test
    @DisplayName("Should retrieve import descriptors")
    void shouldRetrieveImportDescriptors() throws Exception {
      final PanamaModule module = compileWat(IMPORT_MODULE_WAT);

      final List<ImportDescriptor> descriptors = module.getImportDescriptors();
      assertThat(descriptors).hasSize(2);

      for (final ImportDescriptor desc : descriptors) {
        assertNotNull(desc.getModuleName(), "Import descriptor module name should not be null");
        assertNotNull(desc.getName(), "Import descriptor field name should not be null");
        assertNotNull(desc.getType(), "Import descriptor type should not be null");
        LOGGER.info(
            "Import descriptor: "
                + desc.getModuleName()
                + "."
                + desc.getName()
                + " kind="
                + desc.getType().getKind());
      }
    }

    @Test
    @DisplayName("Module without imports should return empty imports list")
    void moduleWithoutImportsShouldReturnEmpty() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final List<ImportType> imports = module.getImports();
      assertThat(imports).isEmpty();

      final List<ModuleImport> moduleImports = module.getModuleImports();
      assertThat(moduleImports).isEmpty();
      LOGGER.info("Function module correctly has no imports");
    }
  }

  // ===== Type Query Tests =====

  @Nested
  @DisplayName("Type Query Tests")
  class TypeQueryTests {

    @Test
    @DisplayName("Should find function type by name")
    void shouldFindFunctionType() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final Optional<FuncType> addType = module.getFunctionType("add");
      assertTrue(addType.isPresent(), "Should find 'add' function type");

      final FuncType funcType = addType.get();
      assertThat(funcType.getParams()).hasSize(2);
      assertThat(funcType.getResults()).hasSize(1);
      LOGGER.info(
          "Function 'add': params="
              + funcType.getParams().size()
              + ", results="
              + funcType.getResults().size());
    }

    @Test
    @DisplayName("Should return empty for nonexistent function")
    void shouldReturnEmptyForNonexistentFunction() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final Optional<FuncType> missing = module.getFunctionType("nonexistent");
      assertFalse(missing.isPresent(), "Should not find nonexistent function");
      LOGGER.info("Correctly returned empty for nonexistent function");
    }

    @Test
    @DisplayName("Should return empty for null function name")
    void shouldReturnEmptyForNullFunctionName() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final Optional<FuncType> result = module.getFunctionType(null);
      assertFalse(result.isPresent(), "Should return empty for null name");
      LOGGER.info("Correctly returned empty for null function name");
    }

    @Test
    @DisplayName("Should find global type by name")
    void shouldFindGlobalType() throws Exception {
      final PanamaModule module = compileWat(GLOBAL_MODULE_WAT);

      final Optional<GlobalType> mutableType = module.getGlobalType("mutable_g");
      assertTrue(mutableType.isPresent(), "Should find mutable global type");
      assertTrue(mutableType.get().isMutable(), "mutable_g should be mutable");

      final Optional<GlobalType> immutableType = module.getGlobalType("immutable_g");
      assertTrue(immutableType.isPresent(), "Should find immutable global type");
      assertFalse(immutableType.get().isMutable(), "immutable_g should be immutable");
      LOGGER.info("Found global types: mutable and immutable");
    }

    @Test
    @DisplayName("Should return empty for null global name")
    void shouldReturnEmptyForNullGlobalName() throws Exception {
      final PanamaModule module = compileWat(GLOBAL_MODULE_WAT);

      final Optional<GlobalType> result = module.getGlobalType(null);
      assertFalse(result.isPresent(), "Should return empty for null global name");
    }

    @Test
    @DisplayName("Should find memory type by name")
    void shouldFindMemoryType() throws Exception {
      final PanamaModule module = compileWat(MEMORY_MODULE_WAT);

      final Optional<MemoryType> memType = module.getMemoryType("memory");
      assertTrue(memType.isPresent(), "Should find 'memory' type");

      final MemoryType mt = memType.get();
      assertEquals(1, mt.getMinimum(), "Memory minimum should be 1 page");
      assertTrue(mt.getMaximum().isPresent(), "Memory should have a maximum");
      assertEquals(10L, mt.getMaximum().get(), "Memory maximum should be 10 pages");
      LOGGER.info("Memory type: min=" + mt.getMinimum() + ", max=" + mt.getMaximum());
    }

    @Test
    @DisplayName("Should return empty for null memory name")
    void shouldReturnEmptyForNullMemoryName() throws Exception {
      final PanamaModule module = compileWat(MEMORY_MODULE_WAT);

      final Optional<MemoryType> result = module.getMemoryType(null);
      assertFalse(result.isPresent(), "Should return empty for null memory name");
    }

    @Test
    @DisplayName("Should find table type by name")
    void shouldFindTableType() throws Exception {
      final PanamaModule module = compileWat(TABLE_MODULE_WAT);

      final Optional<TableType> tableType = module.getTableType("table");
      assertTrue(tableType.isPresent(), "Should find 'table' type");

      final TableType tt = tableType.get();
      assertNotNull(tt.getElementType(), "Table element type should not be null");
      LOGGER.info("Table type: elementType=" + tt.getElementType());
    }

    @Test
    @DisplayName("Should return empty for null table name")
    void shouldReturnEmptyForNullTableName() throws Exception {
      final PanamaModule module = compileWat(TABLE_MODULE_WAT);

      final Optional<TableType> result = module.getTableType(null);
      assertFalse(result.isPresent(), "Should return empty for null table name");
    }
  }

  // ===== Typed Lists Tests =====

  @Nested
  @DisplayName("Typed Lists Tests")
  class TypedListsTests {

    @Test
    @DisplayName("Should list function types")
    void shouldListFunctionTypes() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final List<FuncType> functionTypes = module.getFunctionTypes();
      assertThat(functionTypes).hasSize(2);
      LOGGER.info("Found " + functionTypes.size() + " function types");
    }

    @Test
    @DisplayName("Should list memory types")
    void shouldListMemoryTypes() throws Exception {
      final PanamaModule module = compileWat(MEMORY_MODULE_WAT);

      final List<MemoryType> memoryTypes = module.getMemoryTypes();
      assertThat(memoryTypes).hasSize(1);
      LOGGER.info("Found " + memoryTypes.size() + " memory types");
    }

    @Test
    @DisplayName("Should list global types")
    void shouldListGlobalTypes() throws Exception {
      final PanamaModule module = compileWat(GLOBAL_MODULE_WAT);

      final List<GlobalType> globalTypes = module.getGlobalTypes();
      assertThat(globalTypes).hasSize(2);
      LOGGER.info("Found " + globalTypes.size() + " global types");
    }

    @Test
    @DisplayName("Should list table types")
    void shouldListTableTypes() throws Exception {
      final PanamaModule module = compileWat(TABLE_MODULE_WAT);

      final List<TableType> tableTypes = module.getTableTypes();
      assertThat(tableTypes).hasSize(1);
      LOGGER.info("Found " + tableTypes.size() + " table types");
    }

    @Test
    @DisplayName("Should return empty lists for module without matching types")
    void shouldReturnEmptyForNoMatchingTypes() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      assertThat(module.getMemoryTypes()).isEmpty();
      assertThat(module.getGlobalTypes()).isEmpty();
      assertThat(module.getTableTypes()).isEmpty();
      LOGGER.info("Function-only module correctly has no memory/global/table types");
    }
  }

  // ===== hasExport / hasImport Tests =====

  @Nested
  @DisplayName("Export and Import Query Tests")
  class ExportImportQueryTests {

    @Test
    @DisplayName("Should find existing export")
    void shouldFindExistingExport() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      assertTrue(module.hasExport("add"), "Should find 'add' export");
      assertTrue(module.hasExport("get42"), "Should find 'get42' export");
      LOGGER.info("Found existing exports by name");
    }

    @Test
    @DisplayName("Should not find nonexistent export")
    void shouldNotFindNonexistentExport() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      assertFalse(module.hasExport("nonexistent"), "Should not find nonexistent export");
      LOGGER.info("Correctly did not find nonexistent export");
    }

    @Test
    @DisplayName("Should reject null export name")
    void shouldRejectNullExportName() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      assertThrows(IllegalArgumentException.class, () -> module.hasExport(null));
      LOGGER.info("Correctly rejected null export name");
    }

    @Test
    @DisplayName("Should find existing import")
    void shouldFindExistingImport() throws Exception {
      final PanamaModule module = compileWat(IMPORT_MODULE_WAT);

      assertTrue(module.hasImport("env", "log"), "Should find env.log import");
      assertTrue(module.hasImport("env", "memory"), "Should find env.memory import");
      LOGGER.info("Found existing imports by name");
    }

    @Test
    @DisplayName("Should not find nonexistent import")
    void shouldNotFindNonexistentImport() throws Exception {
      final PanamaModule module = compileWat(IMPORT_MODULE_WAT);

      assertFalse(module.hasImport("env", "nonexistent"), "Should not find env.nonexistent");
      assertFalse(module.hasImport("other", "log"), "Should not find other.log");
      LOGGER.info("Correctly did not find nonexistent imports");
    }

    @Test
    @DisplayName("Should reject null import module name")
    void shouldRejectNullImportModuleName() throws Exception {
      final PanamaModule module = compileWat(IMPORT_MODULE_WAT);

      assertThrows(IllegalArgumentException.class, () -> module.hasImport(null, "log"));
      LOGGER.info("Correctly rejected null module name");
    }

    @Test
    @DisplayName("Should reject null import field name")
    void shouldRejectNullImportFieldName() throws Exception {
      final PanamaModule module = compileWat(IMPORT_MODULE_WAT);

      assertThrows(IllegalArgumentException.class, () -> module.hasImport("env", null));
      LOGGER.info("Correctly rejected null field name");
    }
  }

  // ===== Lifecycle Tests =====

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("Module should be valid after creation")
    void moduleShouldBeValidAfterCreation() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      assertTrue(module.isValid(), "Module should be valid after creation");
      LOGGER.info("Module is valid after creation");
    }

    @Test
    @DisplayName("Module should be invalid after close")
    void moduleShouldBeInvalidAfterClose() throws Exception {
      final PanamaModule module = (PanamaModule) engine.compileWat(FUNCTION_MODULE_WAT);

      module.close();
      assertFalse(module.isValid(), "Module should be invalid after close");
      LOGGER.info("Module is invalid after close");
    }

    @Test
    @DisplayName("Double close should not throw")
    void doubleCloseShouldNotThrow() throws Exception {
      final PanamaModule module = (PanamaModule) engine.compileWat(FUNCTION_MODULE_WAT);

      module.close();
      assertDoesNotThrow(module::close, "Double close should not throw");
      LOGGER.info("Double close succeeded without exception");
    }

    @Test
    @DisplayName("Operations on closed module should throw")
    void operationsOnClosedModuleShouldThrow() throws Exception {
      final PanamaModule module = (PanamaModule) engine.compileWat(FUNCTION_MODULE_WAT);
      module.close();

      assertThrows(IllegalStateException.class, module::getModuleExports);
      assertThrows(IllegalStateException.class, module::getModuleImports);
      assertThrows(IllegalStateException.class, () -> module.hasExport("add"));
      assertThrows(IllegalStateException.class, () -> module.hasImport("env", "log"));
      assertThrows(IllegalStateException.class, module::getName);
      assertThrows(IllegalStateException.class, module::getCustomSections);
      LOGGER.info("All operations correctly throw on closed module");
    }

    @Test
    @DisplayName("Should return engine reference")
    void shouldReturnEngineReference() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      assertThat(module.getEngine()).isSameAs(engine);
      LOGGER.info("Module returns correct engine reference");
    }

    @Test
    @DisplayName("Should return module name")
    void shouldReturnModuleName() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final String name = module.getName();
      assertNotNull(name, "Module name should not be null");
      assertThat(name).startsWith("panama-module-");
      LOGGER.info("Module name: " + name);
    }
  }

  // ===== Serialization Tests =====

  @Nested
  @DisplayName("Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("Should serialize module")
    void shouldSerializeModule() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final byte[] serialized = module.serialize();
      assertNotNull(serialized, "Serialized data should not be null");
      assertThat(serialized.length).isGreaterThan(0);
      LOGGER.info("Serialized module to " + serialized.length + " bytes");
    }

    @Test
    @DisplayName("Should serialize and have content")
    void shouldSerializeWithContent() throws Exception {
      final PanamaModule module = compileWat(MULTI_EXPORT_MODULE_WAT);

      final byte[] serialized = module.serialize();
      assertThat(serialized.length).isGreaterThan(0);
      LOGGER.info("Serialized multi-export module to " + serialized.length + " bytes");
    }

    @Test
    @DisplayName("Serialization should fail on closed module")
    void serializationShouldFailOnClosedModule() throws Exception {
      final PanamaModule module = (PanamaModule) engine.compileWat(FUNCTION_MODULE_WAT);
      module.close();

      assertThrows(IllegalStateException.class, module::serialize);
      LOGGER.info("Serialization correctly fails on closed module");
    }
  }

  // ===== Custom Sections Tests =====

  @Nested
  @DisplayName("Custom Sections Tests")
  class CustomSectionsTests {

    @Test
    @DisplayName("getCustomSections should throw when native function not available")
    void getCustomSectionsShouldThrowWhenNativeUnavailable() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      // The native function wasmtime4j_panama_module_get_custom_sections is not yet implemented,
      // so calling getCustomSections() should throw IllegalArgumentException
      assertThrows(IllegalArgumentException.class, module::getCustomSections);
      LOGGER.info("getCustomSections correctly throws for unimplemented native function");
    }

    @Test
    @DisplayName("getCustomSections on closed module should throw IllegalStateException")
    void getCustomSectionsOnClosedModuleShouldThrow() throws Exception {
      final PanamaModule module = (PanamaModule) engine.compileWat(FUNCTION_MODULE_WAT);
      module.close();

      // Closed module check happens before native call
      assertThrows(IllegalStateException.class, module::getCustomSections);
      LOGGER.info("getCustomSections correctly throws on closed module");
    }
  }

  // ===== WASM Bytes Tests =====

  @Nested
  @DisplayName("WASM Bytes Tests")
  class WasmBytesTests {

    @Test
    @DisplayName("WAT-compiled module getWasmBytes should throw NPE due to null internal bytes")
    void watCompiledModuleShouldThrowForGetWasmBytes() throws Exception {
      final PanamaModule module = compileWat(GLOBAL_MODULE_WAT);

      // WAT-compiled modules don't store the original WASM bytes internally,
      // so getWasmBytes() attempts to clone a null field and throws NullPointerException
      assertThrows(NullPointerException.class, module::getWasmBytes);
      LOGGER.info(
          "WAT-compiled module correctly throws NullPointerException for getWasmBytes()");
    }
  }

  // ===== Instantiation Tests =====

  @Nested
  @DisplayName("Instantiation Tests")
  class InstantiationTests {

    @Test
    @DisplayName("Should reject null store for instantiate")
    void shouldRejectNullStore() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      assertThrows(IllegalArgumentException.class, () -> module.instantiate(null));
      LOGGER.info("Correctly rejected null store for instantiate");
    }

    @Test
    @DisplayName("Should instantiate simple module")
    void shouldInstantiateSimpleModule() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);
      final PanamaStore store = new PanamaStore(engine);
      resources.add(store);

      final PanamaInstance instance = (PanamaInstance) module.instantiate(store);
      resources.add(instance);

      assertTrue(instance.isValid(), "Instance should be valid");
      LOGGER.info("Successfully instantiated module");
    }

    @Test
    @DisplayName("Instantiate on closed module should throw")
    void instantiateOnClosedModuleShouldThrow() throws Exception {
      final PanamaModule module = (PanamaModule) engine.compileWat(FUNCTION_MODULE_WAT);
      final PanamaStore store = new PanamaStore(engine);
      resources.add(store);

      module.close();
      assertThrows(IllegalStateException.class, () -> module.instantiate(store));
      LOGGER.info("Correctly rejected instantiation on closed module");
    }
  }
}

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.testing.RequiresWasmRuntime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the Module interface.
 *
 * <p>Tests verify module compilation, instantiation, exports/imports inspection, serialization, and
 * resource management. These tests require the native Wasmtime runtime to be available.
 */
@DisplayName("Module Interface Tests")
@RequiresWasmRuntime
class ModuleTest {

  /** Simple WebAssembly module that exports an add function. */
  private static final String SIMPLE_ADD_WAT =
      "(module\n"
          + "  (func $add (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add))\n";

  /** Module with memory export. */
  private static final String MEMORY_MODULE_WAT =
      "(module\n" + "  (memory (export \"memory\") 1 16))\n";

  /** Module with global export. */
  private static final String GLOBAL_MODULE_WAT =
      "(module\n" + "  (global $g (export \"counter\") (mut i32) (i32.const 0)))\n";

  /** Module with table export. */
  private static final String TABLE_MODULE_WAT =
      "(module\n" + "  (table (export \"table\") 10 funcref))\n";

  /** Module with import. */
  private static final String IMPORT_MODULE_WAT =
      "(module\n"
          + "  (import \"env\" \"log\" (func $log (param i32)))\n"
          + "  (func (export \"test\") (call $log (i32.const 42))))\n";

  /** Minimal valid WebAssembly module (empty module). */
  private static final byte[] MINIMAL_WASM = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};

  private Engine engine;
  private Module module;

  @BeforeEach
  void setUp() throws WasmException {
    engine = Engine.create();
    module = engine.compileWat(SIMPLE_ADD_WAT);
  }

  @AfterEach
  void tearDown() {
    if (module != null) {
      module.close();
      module = null;
    }
    if (engine != null) {
      engine.close();
      engine = null;
    }
  }

  @Nested
  @DisplayName("Module Compilation Tests")
  class ModuleCompilationTests {

    @Test
    @DisplayName("should compile module from WAT string")
    void shouldCompileModuleFromWatString() throws WasmException {
      try (Module compiledModule = engine.compileWat(SIMPLE_ADD_WAT)) {
        assertNotNull(compiledModule, "Module should not be null");
        assertTrue(compiledModule.isValid(), "Module should be valid after compilation");
      }
    }

    @Test
    @DisplayName("should compile module from wasm bytes")
    void shouldCompileModuleFromWasmBytes() throws WasmException {
      try (Module compiledModule = engine.compileModule(MINIMAL_WASM)) {
        assertNotNull(compiledModule, "Module should not be null");
        assertTrue(compiledModule.isValid(), "Module should be valid after compilation");
      }
    }

    @Test
    @DisplayName("should compile module using static method")
    void shouldCompileModuleUsingStaticMethod() throws WasmException {
      try (Module compiledModule = Module.compile(engine, MINIMAL_WASM)) {
        assertNotNull(compiledModule, "Module should not be null");
        assertTrue(compiledModule.isValid(), "Module should be valid");
      }
    }

    @Test
    @DisplayName("should throw exception for null engine in static compile")
    void shouldThrowExceptionForNullEngineInStaticCompile() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.compile(null, MINIMAL_WASM),
          "Should throw IllegalArgumentException for null engine");
    }

    @Test
    @DisplayName("should throw exception for null bytes in static compile")
    void shouldThrowExceptionForNullBytesInStaticCompile() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.compile(engine, null),
          "Should throw IllegalArgumentException for null bytes");
    }
  }

  @Nested
  @DisplayName("Module Instantiation Tests")
  class ModuleInstantiationTests {

    @Test
    @DisplayName("should instantiate module in store")
    void shouldInstantiateModuleInStore() throws WasmException {
      try (Store store = engine.createStore();
          Instance instance = module.instantiate(store)) {
        assertNotNull(instance, "Instance should not be null");
      }
    }

    @Test
    @DisplayName("should throw exception for null store")
    void shouldThrowExceptionForNullStore() {
      assertThrows(
          IllegalArgumentException.class,
          () -> module.instantiate(null),
          "Should throw IllegalArgumentException for null store");
    }

    @Test
    @DisplayName("should create multiple instances from same module")
    void shouldCreateMultipleInstancesFromSameModule() throws WasmException {
      try (Store store = engine.createStore();
          Instance instance1 = module.instantiate(store);
          Instance instance2 = module.instantiate(store)) {
        assertNotNull(instance1, "First instance should not be null");
        assertNotNull(instance2, "Second instance should not be null");
        assertNotSame(instance1, instance2, "Instances should be different");
      }
    }
  }

  @Nested
  @DisplayName("Module Exports Tests")
  class ModuleExportsTests {

    @Test
    @DisplayName("should return list of exports")
    void shouldReturnListOfExports() {
      final List<ExportType> exports = module.getExports();
      assertNotNull(exports, "Exports list should not be null");
      assertFalse(exports.isEmpty(), "Exports list should not be empty for add module");
    }

    @Test
    @DisplayName("should return export descriptors")
    void shouldReturnExportDescriptors() {
      final List<ExportDescriptor> descriptors = module.getExportDescriptors();
      assertNotNull(descriptors, "Export descriptors should not be null");
    }

    @Test
    @DisplayName("should return module exports")
    void shouldReturnModuleExports() {
      final List<ModuleExport> exports = module.getModuleExports();
      assertNotNull(exports, "Module exports should not be null");
    }

    @Test
    @DisplayName("should check if export exists")
    void shouldCheckIfExportExists() {
      assertTrue(module.hasExport("add"), "Should have 'add' export");
      assertFalse(module.hasExport("nonexistent"), "Should not have 'nonexistent' export");
    }

    @Test
    @DisplayName("should throw exception for null export name")
    void shouldThrowExceptionForNullExportName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> module.hasExport(null),
          "Should throw IllegalArgumentException for null name");
    }

    @Test
    @DisplayName("should get function type by name")
    void shouldGetFunctionTypeByName() {
      final Optional<FuncType> funcType = module.getFunctionType("add");
      assertTrue(funcType.isPresent(), "Should find 'add' function type");
    }

    @Test
    @DisplayName("should return empty optional for nonexistent function")
    void shouldReturnEmptyOptionalForNonexistentFunction() {
      final Optional<FuncType> funcType = module.getFunctionType("nonexistent");
      assertFalse(funcType.isPresent(), "Should not find nonexistent function");
    }

    @Test
    @DisplayName("should throw exception for null function name")
    void shouldThrowExceptionForNullFunctionName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> module.getFunctionType(null),
          "Should throw IllegalArgumentException for null function name");
    }
  }

  @Nested
  @DisplayName("Module Imports Tests")
  class ModuleImportsTests {

    @Test
    @DisplayName("should return empty imports list for module without imports")
    void shouldReturnEmptyImportsListForModuleWithoutImports() {
      final List<ImportType> imports = module.getImports();
      assertNotNull(imports, "Imports list should not be null");
      assertTrue(imports.isEmpty(), "Add module should have no imports");
    }

    @Test
    @DisplayName("should return imports list for module with imports")
    void shouldReturnImportsListForModuleWithImports() throws WasmException {
      try (Module importModule = engine.compileWat(IMPORT_MODULE_WAT)) {
        final List<ImportType> imports = importModule.getImports();
        assertNotNull(imports, "Imports list should not be null");
        assertFalse(imports.isEmpty(), "Import module should have imports");
      }
    }

    @Test
    @DisplayName("should return import descriptors")
    void shouldReturnImportDescriptors() {
      final List<ImportDescriptor> descriptors = module.getImportDescriptors();
      assertNotNull(descriptors, "Import descriptors should not be null");
    }

    @Test
    @DisplayName("should return module imports")
    void shouldReturnModuleImports() {
      final List<ModuleImport> imports = module.getModuleImports();
      assertNotNull(imports, "Module imports should not be null");
    }

    @Test
    @DisplayName("should check if import exists")
    void shouldCheckIfImportExists() throws WasmException {
      try (Module importModule = engine.compileWat(IMPORT_MODULE_WAT)) {
        assertTrue(importModule.hasImport("env", "log"), "Should have 'env.log' import");
        assertFalse(
            importModule.hasImport("env", "nonexistent"),
            "Should not have 'env.nonexistent' import");
      }
    }

    @Test
    @DisplayName("should throw exception for null module name in hasImport")
    void shouldThrowExceptionForNullModuleNameInHasImport() {
      assertThrows(
          IllegalArgumentException.class,
          () -> module.hasImport(null, "field"),
          "Should throw IllegalArgumentException for null module name");
    }

    @Test
    @DisplayName("should throw exception for null field name in hasImport")
    void shouldThrowExceptionForNullFieldNameInHasImport() {
      assertThrows(
          IllegalArgumentException.class,
          () -> module.hasImport("module", null),
          "Should throw IllegalArgumentException for null field name");
    }
  }

  @Nested
  @DisplayName("Module Type Information Tests")
  class ModuleTypeInformationTests {

    @Test
    @DisplayName("should return function types")
    void shouldReturnFunctionTypes() {
      final List<FuncType> funcTypes = module.getFunctionTypes();
      assertNotNull(funcTypes, "Function types should not be null");
    }

    @Test
    @DisplayName("should return memory types")
    void shouldReturnMemoryTypes() {
      final List<MemoryType> memTypes = module.getMemoryTypes();
      assertNotNull(memTypes, "Memory types should not be null");
    }

    @Test
    @DisplayName("should return table types")
    void shouldReturnTableTypes() {
      final List<TableType> tableTypes = module.getTableTypes();
      assertNotNull(tableTypes, "Table types should not be null");
    }

    @Test
    @DisplayName("should return global types")
    void shouldReturnGlobalTypes() {
      final List<GlobalType> globalTypes = module.getGlobalTypes();
      assertNotNull(globalTypes, "Global types should not be null");
    }

    @Test
    @DisplayName("should get memory type by name")
    void shouldGetMemoryTypeByName() throws WasmException {
      try (Module memModule = engine.compileWat(MEMORY_MODULE_WAT)) {
        final Optional<MemoryType> memType = memModule.getMemoryType("memory");
        assertTrue(memType.isPresent(), "Should find 'memory' memory type");
      }
    }

    @Test
    @DisplayName("should get global type by name")
    void shouldGetGlobalTypeByName() throws WasmException {
      try (Module globalModule = engine.compileWat(GLOBAL_MODULE_WAT)) {
        final Optional<GlobalType> globalType = globalModule.getGlobalType("counter");
        assertTrue(globalType.isPresent(), "Should find 'counter' global type");
      }
    }

    @Test
    @DisplayName("should get table type by name")
    void shouldGetTableTypeByName() throws WasmException {
      try (Module tableModule = engine.compileWat(TABLE_MODULE_WAT)) {
        final Optional<TableType> tableType = tableModule.getTableType("table");
        assertTrue(tableType.isPresent(), "Should find 'table' table type");
      }
    }
  }

  @Nested
  @DisplayName("Module Metadata Tests")
  class ModuleMetadataTests {

    @Test
    @DisplayName("should return engine")
    void shouldReturnEngine() {
      final Engine moduleEngine = module.getEngine();
      assertNotNull(moduleEngine, "Module engine should not be null");
      assertSame(engine, moduleEngine, "Module engine should be the same as creation engine");
    }

    @Test
    @DisplayName("should return module name or null")
    void shouldReturnModuleNameOrNull() {
      // Module name may or may not be present
      final String name = module.getName();
      // Just verify it doesn't throw
    }

    @Test
    @DisplayName("should return custom sections")
    void shouldReturnCustomSections() {
      final Map<String, byte[]> customSections = module.getCustomSections();
      assertNotNull(customSections, "Custom sections should not be null");
    }

    @Test
    @DisplayName("should return resources required")
    void shouldReturnResourcesRequired() {
      final ResourcesRequired resources = module.resourcesRequired();
      assertNotNull(resources, "Resources required should not be null");
    }

    @Test
    @DisplayName("should return functions iterable")
    void shouldReturnFunctionsIterable() {
      final Iterable<FunctionInfo> functions = module.functions();
      assertNotNull(functions, "Functions iterable should not be null");
    }
  }

  @Nested
  @DisplayName("Module Serialization Tests")
  class ModuleSerializationTests {

    @Test
    @DisplayName("should serialize module")
    void shouldSerializeModule() throws WasmException {
      final byte[] serialized = module.serialize();
      assertNotNull(serialized, "Serialized bytes should not be null");
      assertTrue(serialized.length > 0, "Serialized bytes should not be empty");
    }

    @Test
    @DisplayName("should deserialize module")
    void shouldDeserializeModule() throws WasmException {
      final byte[] serialized = module.serialize();
      try (Module deserialized = Module.deserialize(engine, serialized)) {
        assertNotNull(deserialized, "Deserialized module should not be null");
        assertTrue(deserialized.isValid(), "Deserialized module should be valid");
      }
    }

    @Test
    @DisplayName("should throw exception for null engine in deserialize")
    void shouldThrowExceptionForNullEngineInDeserialize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.deserialize(null, new byte[10]),
          "Should throw IllegalArgumentException for null engine");
    }

    @Test
    @DisplayName("should throw exception for null bytes in deserialize")
    void shouldThrowExceptionForNullBytesInDeserialize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.deserialize(engine, null),
          "Should throw IllegalArgumentException for null bytes");
    }

    @Test
    @DisplayName("should report serializability")
    void shouldReportSerializability() {
      // Default is true
      assertTrue(module.isSerializable(), "Module should be serializable by default");
    }
  }

  @Nested
  @DisplayName("Module Validation Tests")
  class ModuleValidationTests {

    @Test
    @DisplayName("should validate valid wasm bytes")
    void shouldValidateValidWasmBytes() {
      final ModuleValidationResult result = Module.validate(engine, MINIMAL_WASM);
      assertNotNull(result, "Validation result should not be null");
      assertTrue(result.isValid(), "Valid WASM should pass validation");
    }

    @Test
    @DisplayName("should fail validation for invalid wasm bytes")
    void shouldFailValidationForInvalidWasmBytes() {
      final byte[] invalidWasm = {0x00, 0x00, 0x00, 0x00};
      final ModuleValidationResult result = Module.validate(engine, invalidWasm);
      assertNotNull(result, "Validation result should not be null");
      assertFalse(result.isValid(), "Invalid WASM should fail validation");
    }

    @Test
    @DisplayName("should throw exception for null engine in validate")
    void shouldThrowExceptionForNullEngineInValidate() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.validate(null, MINIMAL_WASM),
          "Should throw IllegalArgumentException for null engine");
    }

    @Test
    @DisplayName("should throw exception for null bytes in validate")
    void shouldThrowExceptionForNullBytesInValidate() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Module.validate(engine, null),
          "Should throw IllegalArgumentException for null bytes");
    }

    @Test
    @DisplayName("should validate imports")
    void shouldValidateImports() {
      final ImportMap imports = ImportMap.empty();
      final boolean valid = module.validateImports(imports);
      // Simple add module has no imports, so empty ImportMap should be valid
      assertTrue(valid, "Empty imports should be valid for module without imports");
    }

    @Test
    @DisplayName("should throw exception for null imports in validate")
    void shouldThrowExceptionForNullImportsInValidate() {
      assertThrows(
          IllegalArgumentException.class,
          () -> module.validateImports(null),
          "Should throw IllegalArgumentException for null imports");
    }

    @Test
    @DisplayName("should validate imports with detailed results")
    void shouldValidateImportsWithDetailedResults() {
      final ImportMap imports = ImportMap.empty();
      final ImportValidation result = module.validateImportsDetailed(imports);
      assertNotNull(result, "Validation result should not be null");
    }
  }

  @Nested
  @DisplayName("Module Lifecycle Tests")
  class ModuleLifecycleTests {

    @Test
    @DisplayName("should be valid before close")
    void shouldBeValidBeforeClose() {
      assertTrue(module.isValid(), "Module should be valid before close");
    }

    @Test
    @DisplayName("should be invalid after close")
    void shouldBeInvalidAfterClose() throws WasmException {
      final Module tempModule = engine.compileWat(SIMPLE_ADD_WAT);
      assertTrue(tempModule.isValid(), "Module should be valid before close");
      tempModule.close();
      assertFalse(tempModule.isValid(), "Module should be invalid after close");
    }

    @Test
    @DisplayName("should handle multiple close calls")
    void shouldHandleMultipleCloseCalls() throws WasmException {
      final Module tempModule = engine.compileWat(SIMPLE_ADD_WAT);
      tempModule.close();
      // Second close should not throw
      tempModule.close();
      assertFalse(tempModule.isValid(), "Module should remain invalid");
    }

    @Test
    @DisplayName("should work with try-with-resources")
    void shouldWorkWithTryWithResources() throws WasmException {
      Module tempModule;
      try (Module autoClosedModule = engine.compileWat(SIMPLE_ADD_WAT)) {
        tempModule = autoClosedModule;
        assertTrue(autoClosedModule.isValid(), "Module should be valid inside try block");
      }
      assertFalse(tempModule.isValid(), "Module should be invalid after try-with-resources");
    }
  }

  @Nested
  @DisplayName("Module Advanced Features Tests")
  class ModuleAdvancedFeaturesTests {

    @Test
    @DisplayName("should return image range or empty")
    void shouldReturnImageRangeOrEmpty() {
      final Optional<Module.ModuleImageRange> imageRange = module.imageRange();
      assertNotNull(imageRange, "Image range optional should not be null");
      // May or may not be present depending on implementation
    }

    @Test
    @DisplayName("should return compiled module or empty")
    void shouldReturnCompiledModuleOrEmpty() {
      final Optional<CompiledModule> compiledModule = module.getCompiledModule();
      assertNotNull(compiledModule, "Compiled module optional should not be null");
      // May or may not be present depending on implementation
    }
  }
}

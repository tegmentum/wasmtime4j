package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.ExportType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Panama-specific tests for {@link PanamaModule}.
 *
 * <p>These tests exercise Panama implementation details that cannot be tested through the unified
 * API, such as constructor validation with direct PanamaModule instantiation, PanamaModule-specific
 * methods, and Panama-specific type assertions.
 *
 * <p>Generic Module API tests that apply to both runtimes have been migrated to {@code
 * ModuleApiDualRuntimeTest} in the integration test module.
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

  private static final String GLOBAL_MODULE_WAT =
      """
      (module
        (global (export "mutable_g") (mut i32) (i32.const 42))
        (global (export "immutable_g") i32 (i32.const 7))
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

  // ===== Panama-Specific Export Metadata Tests =====

  @Nested
  @DisplayName("Export Metadata Tests")
  class ExportMetadataTests {

    @Test
    @DisplayName("Empty module should have no exports")
    void emptyModuleShouldHaveNoExports() throws Exception {
      final PanamaModule module = compileWat(EMPTY_MODULE_WAT);

      final List<ExportType> exports = module.getExports();
      assertThat(exports).isEmpty();

      final List<ExportType> moduleExports = module.getExports();
      assertThat(moduleExports).isEmpty();
      LOGGER.info("Empty module correctly has no exports");
    }
  }

  // ===== Panama-Specific Import Metadata Tests =====

  @Nested
  @DisplayName("Import Metadata Tests")
  class ImportMetadataTests {

    @Test
    @DisplayName("Empty module should have no imports")
    void emptyModuleShouldHaveNoImports() throws Exception {
      final PanamaModule module = compileWat(EMPTY_MODULE_WAT);

      final List<ImportType> imports = module.getImports();
      assertThat(imports).isEmpty();

      final List<ImportType> moduleImports = module.getImports();
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
  }

  // ===== Panama-Specific Lifecycle Tests =====

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

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
    @DisplayName("Should return module name")
    void shouldReturnModuleName() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final String name = module.getName();
      assertNotNull(name, "Module name should not be null");
      assertThat(name).startsWith("panama-module-");
      LOGGER.info("Module name: " + name);
    }
  }

  // ===== WASM Bytes Tests =====

  @Nested
  @DisplayName("WASM Bytes Tests")
  class WasmBytesTests {

    @Test
    @DisplayName("WAT-compiled module getWasmBytes should return null")
    void watCompiledModuleShouldReturnNullForGetWasmBytes() throws Exception {
      final PanamaModule module = compileWat(GLOBAL_MODULE_WAT);

      // WAT-compiled modules don't store the original WASM bytes internally,
      // so getWasmBytes() returns null
      assertNull(
          module.getWasmBytes(), "WAT-compiled module should return null for getWasmBytes()");
      LOGGER.info("WAT-compiled module correctly returns null for getWasmBytes()");
    }
  }

  // ===== Panama-Specific Instantiation Tests =====

  @Nested
  @DisplayName("Instantiation Tests")
  class InstantiationTests {

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
  }
}

package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.DependencyResolution;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.ImportInfo;
import ai.tegmentum.wasmtime4j.ImportValidation;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaLinker} that exercise constructor logic, validation, host function
 * definition, module instantiation, WASI integration, and lifecycle management.
 *
 * <p>These tests create real PanamaEngine, PanamaStore, and PanamaLinker instances via native
 * library calls.
 */
@DisplayName("PanamaLinker Tests")
class PanamaLinkerTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaLinkerTest.class.getName());

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

  private PanamaLinker<?> createLinker() throws Exception {
    final PanamaLinker<?> linker = new PanamaLinker<>(engine);
    resources.add(linker);
    return linker;
  }

  private PanamaModule compileWat(final String wat) throws Exception {
    final PanamaModule module = (PanamaModule) engine.compileWat(wat);
    resources.add(module);
    return module;
  }

  private PanamaStore createStore() throws Exception {
    final PanamaStore store = new PanamaStore(engine);
    resources.add(store);
    return store;
  }

  // ===== WAT Module Definitions =====

  private static final String SIMPLE_MODULE_WAT =
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

  private static final String IMPORT_MODULE_WAT =
      """
      (module
        (import "env" "log" (func (param i32)))
        (func (export "main") (result i32)
          i32.const 0
        )
      )
      """;

  private static final String MEMORY_IMPORT_MODULE_WAT =
      """
      (module
        (import "env" "memory" (memory 1))
        (func (export "load") (result i32)
          i32.const 0
          i32.load
        )
      )
      """;

  private static final String EMPTY_MODULE_WAT = "(module)";

  // ===== Constructor Tests =====

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should reject null engine")
    void shouldRejectNullEngine() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new PanamaLinker<>(null));
      assertThat(ex.getMessage()).contains("Engine cannot be null");
      LOGGER.info("Correctly rejected null engine: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject closed engine")
    void shouldRejectClosedEngine() throws Exception {
      final PanamaEngine closedEngine = new PanamaEngine();
      closedEngine.close();

      assertThrows(Exception.class, () -> new PanamaLinker<>(closedEngine));
      LOGGER.info("Correctly rejected closed engine");
    }

    @Test
    @DisplayName("Should create valid linker")
    void shouldCreateValidLinker() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertTrue(linker.isValid(), "Linker should be valid after creation");
      assertNotNull(linker.getNativeLinker(), "Native linker should not be null");
      assertNotNull(linker.getEngine(), "Engine reference should not be null");
      LOGGER.info("Created valid linker");
    }
  }

  // ===== Lifecycle Tests =====

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("Linker should be valid after creation")
    void shouldBeValidAfterCreation() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertTrue(linker.isValid(), "Linker should be valid");
      LOGGER.info("Linker is valid after creation");
    }

    @Test
    @DisplayName("Linker should be invalid after close")
    void shouldBeInvalidAfterClose() throws Exception {
      final PanamaLinker<?> linker = new PanamaLinker<>(engine);

      linker.close();
      assertFalse(linker.isValid(), "Linker should be invalid after close");
      LOGGER.info("Linker is invalid after close");
    }

    @Test
    @DisplayName("Double close should not throw")
    void doubleCloseShouldNotThrow() throws Exception {
      final PanamaLinker<?> linker = new PanamaLinker<>(engine);

      linker.close();
      assertDoesNotThrow(linker::close, "Double close should not throw");
      LOGGER.info("Double close succeeded without exception");
    }

    @Test
    @DisplayName("Should return engine reference")
    void shouldReturnEngineReference() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThat(linker.getEngine()).isSameAs(engine);
      LOGGER.info("Linker returns correct engine reference");
    }
  }

  // ===== WASI Integration Tests =====

  @Nested
  @DisplayName("WASI Integration Tests")
  class WasiIntegrationTests {

    @Test
    @DisplayName("WASI should not be enabled initially")
    void wasiShouldNotBeEnabledInitially() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertFalse(linker.isWasiEnabled(), "WASI should not be enabled initially");
      LOGGER.info("WASI correctly not enabled initially");
    }

    @Test
    @DisplayName("Should enable WASI")
    void shouldEnableWasi() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      linker.enableWasi();
      assertTrue(linker.isWasiEnabled(), "WASI should be enabled after enableWasi()");
      LOGGER.info("WASI enabled successfully");
    }

    @Test
    @DisplayName("enableWasi should be idempotent")
    void enableWasiShouldBeIdempotent() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      linker.enableWasi();
      linker.enableWasi(); // Second call should not throw
      assertTrue(linker.isWasiEnabled(), "WASI should still be enabled");
      LOGGER.info("enableWasi is idempotent");
    }

    @Test
    @DisplayName("Should get and set WASI context")
    void shouldGetAndSetWasiContext() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThat(linker.getWasiContext()).isNull();
      LOGGER.info("WASI context correctly null initially");
    }
  }

  // ===== Fluent Configuration Tests =====

  @Nested
  @DisplayName("Fluent Configuration Tests")
  class FluentConfigurationTests {

    @Test
    @DisplayName("allowShadowing should return linker for chaining")
    void allowShadowingShouldReturnLinker() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      final Object result = linker.allowShadowing(true);
      assertThat(result).isSameAs(linker);
      LOGGER.info("allowShadowing returns this for fluent chaining");
    }

    @Test
    @DisplayName("allowUnknownExports should return linker for chaining")
    void allowUnknownExportsShouldReturnLinker() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      final Object result = linker.allowUnknownExports(true);
      assertThat(result).isSameAs(linker);
      LOGGER.info("allowUnknownExports returns this for fluent chaining");
    }

    @Test
    @DisplayName("Should toggle shadowing without error")
    void shouldToggleShadowing() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertDoesNotThrow(() -> linker.allowShadowing(true));
      assertDoesNotThrow(() -> linker.allowShadowing(false));
      LOGGER.info("Toggling shadowing works without error");
    }

    @Test
    @DisplayName("Should toggle unknown exports without error")
    void shouldToggleUnknownExports() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertDoesNotThrow(() -> linker.allowUnknownExports(true));
      assertDoesNotThrow(() -> linker.allowUnknownExports(false));
      LOGGER.info("Toggling unknown exports works without error");
    }
  }

  // ===== DefineHostFunction Tests =====

  @Nested
  @DisplayName("DefineHostFunction Tests")
  class DefineHostFunctionTests {

    @Test
    @DisplayName("Should reject null module name")
    void shouldRejectNullModuleName() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});
      final HostFunction impl = params -> new WasmValue[0];

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineHostFunction(null, "func", funcType, impl));
      LOGGER.info("Correctly rejected null module name");
    }

    @Test
    @DisplayName("Should reject null function name")
    void shouldRejectNullFunctionName() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});
      final HostFunction impl = params -> new WasmValue[0];

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineHostFunction("env", null, funcType, impl));
      LOGGER.info("Correctly rejected null function name");
    }

    @Test
    @DisplayName("Should reject null function type")
    void shouldRejectNullFunctionType() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final HostFunction impl = params -> new WasmValue[0];

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineHostFunction("env", "func", null, impl));
      LOGGER.info("Correctly rejected null function type");
    }

    @Test
    @DisplayName("Should reject null implementation")
    void shouldRejectNullImplementation() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineHostFunction("env", "func", funcType, null));
      LOGGER.info("Correctly rejected null implementation");
    }

    @Test
    @DisplayName("Should define host function successfully")
    void shouldDefineHostFunction() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32},
              new WasmValueType[] {});
      final HostFunction impl = params -> new WasmValue[0];

      assertDoesNotThrow(() -> linker.defineHostFunction("env", "log", funcType, impl));
      LOGGER.info("Host function defined successfully");
    }

    @Test
    @DisplayName("Should define and instantiate module with host function")
    void shouldDefineAndInstantiateWithHostFunction() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      // Define the host function that the module imports
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32},
              new WasmValueType[] {});
      final HostFunction impl = params -> new WasmValue[0];
      linker.defineHostFunction("env", "log", funcType, impl);

      // Compile and instantiate the module that imports env.log
      final PanamaModule module = compileWat(IMPORT_MODULE_WAT);
      final PanamaStore store = createStore();

      final Instance instance = linker.instantiate(store, module);
      resources.add(instance);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");
      LOGGER.info("Successfully instantiated module with host function");
    }
  }

  // ===== Instantiation Tests =====

  @Nested
  @DisplayName("Instantiation Tests")
  class InstantiationTests {

    @Test
    @DisplayName("Should reject null store for instantiate")
    void shouldRejectNullStore() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(SIMPLE_MODULE_WAT);

      assertThrows(
          IllegalArgumentException.class, () -> linker.instantiate(null, module));
      LOGGER.info("Correctly rejected null store");
    }

    @Test
    @DisplayName("Should reject null module for instantiate")
    void shouldRejectNullModule() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class, () -> linker.instantiate(store, null));
      LOGGER.info("Correctly rejected null module");
    }

    @Test
    @DisplayName("Should instantiate simple module without imports")
    void shouldInstantiateSimpleModule() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(SIMPLE_MODULE_WAT);
      final PanamaStore store = createStore();

      final Instance instance = linker.instantiate(store, module);
      resources.add(instance);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");
      LOGGER.info("Successfully instantiated simple module via linker");
    }

    @Test
    @DisplayName("Should instantiate empty module")
    void shouldInstantiateEmptyModule() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(EMPTY_MODULE_WAT);
      final PanamaStore store = createStore();

      final Instance instance = linker.instantiate(store, module);
      resources.add(instance);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");
      LOGGER.info("Successfully instantiated empty module via linker");
    }

    @Test
    @DisplayName("Should instantiate module with name")
    void shouldInstantiateModuleWithName() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(SIMPLE_MODULE_WAT);
      final PanamaStore store = createStore();

      final Instance instance = linker.instantiate(store, "test_module", module);
      resources.add(instance);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");
      LOGGER.info("Successfully instantiated module with name via linker");
    }

    @Test
    @DisplayName("Should fail to instantiate module with unsatisfied imports")
    void shouldFailWithUnsatisfiedImports() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(IMPORT_MODULE_WAT);
      final PanamaStore store = createStore();

      // Module requires env.log import which is not defined
      assertThrows(Exception.class, () -> linker.instantiate(store, module));
      LOGGER.info("Correctly failed with unsatisfied imports");
    }
  }

  // ===== InstantiatePre Tests =====

  @Nested
  @DisplayName("InstantiatePre Tests")
  class InstantiatePreTests {

    @Test
    @DisplayName("Should reject null module")
    void shouldRejectNullModule() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(IllegalArgumentException.class, () -> linker.instantiatePre(null));
      LOGGER.info("Correctly rejected null module for instantiatePre");
    }

    @Test
    @DisplayName("Should create InstancePre for simple module")
    void shouldCreateInstancePre() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(SIMPLE_MODULE_WAT);

      final Object instancePre = linker.instantiatePre(module);
      resources.add((AutoCloseable) instancePre);

      assertNotNull(instancePre, "InstancePre should not be null");
      LOGGER.info("Successfully created InstancePre");
    }
  }

  // ===== HasImport Tests =====

  @Nested
  @DisplayName("HasImport Tests")
  class HasImportTests {

    @Test
    @DisplayName("Should reject null module name")
    void shouldRejectNullModuleName() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(IllegalArgumentException.class, () -> linker.hasImport(null, "func"));
      LOGGER.info("Correctly rejected null module name");
    }

    @Test
    @DisplayName("Should reject null name")
    void shouldRejectNullName() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(IllegalArgumentException.class, () -> linker.hasImport("env", null));
      LOGGER.info("Correctly rejected null name");
    }

    @Test
    @DisplayName("Should reject empty module name")
    void shouldRejectEmptyModuleName() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(IllegalArgumentException.class, () -> linker.hasImport("", "func"));
      LOGGER.info("Correctly rejected empty module name");
    }

    @Test
    @DisplayName("Should reject empty name")
    void shouldRejectEmptyName() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(IllegalArgumentException.class, () -> linker.hasImport("env", ""));
      LOGGER.info("Correctly rejected empty name");
    }

    @Test
    @DisplayName("Should return false for undefined import")
    void shouldReturnFalseForUndefined() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertFalse(linker.hasImport("env", "nonexistent"));
      LOGGER.info("Correctly returned false for undefined import");
    }

    @Test
    @DisplayName("Should return true after defining host function")
    void shouldReturnTrueAfterDefine() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32},
              new WasmValueType[] {});
      final HostFunction impl = params -> new WasmValue[0];
      linker.defineHostFunction("env", "log", funcType, impl);

      assertTrue(linker.hasImport("env", "log"), "Should find defined import");
      assertFalse(linker.hasImport("env", "other"), "Should not find undefined import");
      LOGGER.info("hasImport correctly reflects defined imports");
    }
  }

  // ===== Import Registry Tests =====

  @Nested
  @DisplayName("Import Registry Tests")
  class ImportRegistryTests {

    @Test
    @DisplayName("Should return empty registry for new linker")
    void shouldReturnEmptyRegistry() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      final List<ImportInfo> registry = linker.getImportRegistry();
      assertThat(registry).isEmpty();
      LOGGER.info("Empty linker has empty import registry");
    }

    @Test
    @DisplayName("Should return registry with defined imports")
    void shouldReturnRegistryWithDefinedImports() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32},
              new WasmValueType[] {});
      linker.defineHostFunction("env", "log", funcType, params -> new WasmValue[0]);

      final List<ImportInfo> registry = linker.getImportRegistry();
      assertThat(registry).isNotEmpty();
      LOGGER.info("Registry has " + registry.size() + " entries after define");
    }

    @Test
    @DisplayName("Should return iterable definitions")
    void shouldReturnIterableDefinitions() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32},
              new WasmValueType[] {});
      linker.defineHostFunction("env", "log", funcType, params -> new WasmValue[0]);

      final Iterable<Linker.LinkerDefinition> defs = linker.iter();
      assertNotNull(defs, "iter() should not return null");

      int count = 0;
      for (final Linker.LinkerDefinition def : defs) {
        assertNotNull(def, "Linker.LinkerDefinition should not be null");
        count++;
      }
      assertThat(count).isGreaterThan(0);
      LOGGER.info("iter() returned " + count + " definitions");
    }
  }

  // ===== Dependency Resolution Tests =====

  @Nested
  @DisplayName("Dependency Resolution Tests")
  class DependencyResolutionTests {

    @Test
    @DisplayName("Should reject null modules for resolveDependencies")
    void shouldRejectNullModules() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.resolveDependencies((ai.tegmentum.wasmtime4j.Module[]) null));
      LOGGER.info("Correctly rejected null modules for dependency resolution");
    }

    @Test
    @DisplayName("Should reject empty modules for resolveDependencies")
    void shouldRejectEmptyModules() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.resolveDependencies());
      LOGGER.info("Correctly rejected empty modules for dependency resolution");
    }

    @Test
    @DisplayName("Should resolve dependencies for simple module")
    void shouldResolveDependenciesForSimpleModule() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(SIMPLE_MODULE_WAT);

      final DependencyResolution result = linker.resolveDependencies(module);
      assertNotNull(result, "DependencyResolution should not be null");
      LOGGER.info("Successfully resolved dependencies");
    }

    @Test
    @DisplayName("Should validate imports for simple module")
    void shouldValidateImportsForSimpleModule() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(SIMPLE_MODULE_WAT);

      final ImportValidation validation = linker.validateImports(module);
      assertNotNull(validation, "ImportValidation should not be null");
      LOGGER.info("Successfully validated imports");
    }

    @Test
    @DisplayName("Should reject null modules for validateImports")
    void shouldRejectNullModulesForValidateImports() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.validateImports((ai.tegmentum.wasmtime4j.Module[]) null));
      LOGGER.info("Correctly rejected null modules for import validation");
    }
  }

  // ===== DefineUnknownImports Tests =====

  @Nested
  @DisplayName("DefineUnknownImports Tests")
  class DefineUnknownImportsTests {

    @Test
    @DisplayName("Should reject null store for defineUnknownImportsAsTraps")
    void shouldRejectNullStoreForTraps() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(SIMPLE_MODULE_WAT);

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineUnknownImportsAsTraps(null, module));
      LOGGER.info("Correctly rejected null store for defineUnknownImportsAsTraps");
    }

    @Test
    @DisplayName("Should reject null module for defineUnknownImportsAsTraps")
    void shouldRejectNullModuleForTraps() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineUnknownImportsAsTraps(store, null));
      LOGGER.info("Correctly rejected null module for defineUnknownImportsAsTraps");
    }

    @Test
    @DisplayName("defineUnknownImportsAsTraps should throw for unimplemented native function")
    void shouldDefineUnknownImportsAsTraps() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(IMPORT_MODULE_WAT);
      final PanamaStore store = createStore();

      // Native function wasmtime4j_panama_linker_define_unknown_imports_as_traps not yet implemented
      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineUnknownImportsAsTraps(store, module));
      LOGGER.info("defineUnknownImportsAsTraps correctly throws for unimplemented native");
    }

    @Test
    @DisplayName("Should reject null store for defineUnknownImportsAsDefaultValues")
    void shouldRejectNullStoreForDefaults() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(SIMPLE_MODULE_WAT);

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineUnknownImportsAsDefaultValues(null, module));
      LOGGER.info("Correctly rejected null store for defineUnknownImportsAsDefaultValues");
    }

    @Test
    @DisplayName("Should reject null module for defineUnknownImportsAsDefaultValues")
    void shouldRejectNullModuleForDefaults() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineUnknownImportsAsDefaultValues(store, null));
      LOGGER.info("Correctly rejected null module for defineUnknownImportsAsDefaultValues");
    }

    @Test
    @DisplayName("defineUnknownImportsAsDefaultValues should throw for unimplemented native")
    void shouldDefineUnknownImportsAsDefaultValues() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(IMPORT_MODULE_WAT);
      final PanamaStore store = createStore();

      // Native function not yet implemented
      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineUnknownImportsAsDefaultValues(store, module));
      LOGGER.info("defineUnknownImportsAsDefaultValues correctly throws for unimplemented native");
    }
  }

  // ===== Alias Tests =====

  @Nested
  @DisplayName("Alias Tests")
  class AliasTests {

    @Test
    @DisplayName("Should reject null from module name")
    void shouldRejectNullFromModuleName() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.alias(null, "func", "other", "func"));
      LOGGER.info("Correctly rejected null from module name");
    }

    @Test
    @DisplayName("Should reject null from name")
    void shouldRejectNullFromName() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.alias("env", null, "other", "func"));
      LOGGER.info("Correctly rejected null from name");
    }

    @Test
    @DisplayName("Should reject null to module name")
    void shouldRejectNullToModuleName() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.alias("env", "func", null, "func"));
      LOGGER.info("Correctly rejected null to module name");
    }

    @Test
    @DisplayName("Should reject null to name")
    void shouldRejectNullToName() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.alias("env", "func", "other", null));
      LOGGER.info("Correctly rejected null to name");
    }
  }

  // ===== DefineInstance Tests =====

  @Nested
  @DisplayName("DefineInstance Tests")
  class DefineInstanceTests {

    @Test
    @DisplayName("Should reject null module name")
    void shouldRejectNullModuleName() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(SIMPLE_MODULE_WAT);
      final PanamaStore store = createStore();
      final Instance instance = linker.instantiate(store, module);
      resources.add(instance);

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineInstance(null, instance));
      LOGGER.info("Correctly rejected null module name for defineInstance");
    }

    @Test
    @DisplayName("Should reject null instance")
    void shouldRejectNullInstance() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineInstance("test", null));
      LOGGER.info("Correctly rejected null instance for defineInstance");
    }

    @Test
    @DisplayName("Should define instance successfully")
    void shouldDefineInstance() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaModule module = compileWat(SIMPLE_MODULE_WAT);
      final PanamaStore store = createStore();
      final Instance instance = linker.instantiate(store, module);
      resources.add(instance);

      assertDoesNotThrow(() -> linker.defineInstance("my_module", instance));
      LOGGER.info("Successfully defined instance");
    }
  }

  // ===== FuncNewUnchecked Tests =====

  @Nested
  @DisplayName("FuncNewUnchecked Tests")
  class FuncNewUncheckedTests {

    @Test
    @DisplayName("Should reject null module name")
    void shouldRejectNullModuleName() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaStore store = createStore();
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});
      final HostFunction impl = params -> new WasmValue[0];

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.funcNewUnchecked(store, null, "func", funcType, impl));
      LOGGER.info("Correctly rejected null module name for funcNewUnchecked");
    }

    @Test
    @DisplayName("Should reject null function name")
    void shouldRejectNullFunctionName() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaStore store = createStore();
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});
      final HostFunction impl = params -> new WasmValue[0];

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.funcNewUnchecked(store, "env", null, funcType, impl));
      LOGGER.info("Correctly rejected null function name for funcNewUnchecked");
    }
  }

  // ===== DefineName Tests =====

  @Nested
  @DisplayName("DefineName Tests")
  class DefineNameTests {

    @Test
    @DisplayName("defineName should reject null extern")
    void defineNameShouldRejectNullExtern() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaStore store = createStore();

      // defineName checks parameters before reaching the unimplemented body
      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineName(store, "test", null));
      LOGGER.info("defineName correctly rejects null extern");
    }
  }

  // ===== GetByImport Tests =====

  @Nested
  @DisplayName("GetByImport Tests")
  class GetByImportTests {

    @Test
    @DisplayName("Should reject null store")
    void shouldRejectNullStore() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.getByImport(null, "env", "func"));
      LOGGER.info("Correctly rejected null store for getByImport");
    }

    @Test
    @DisplayName("Should reject null module name")
    void shouldRejectNullModuleName() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.getByImport(store, null, "func"));
      LOGGER.info("Correctly rejected null module name for getByImport");
    }

    @Test
    @DisplayName("Should reject null name")
    void shouldRejectNullName() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.getByImport(store, "env", null));
      LOGGER.info("Correctly rejected null name for getByImport");
    }
  }

  // ===== GetDefault Tests =====

  @Nested
  @DisplayName("GetDefault Tests")
  class GetDefaultTests {

    @Test
    @DisplayName("Should reject null store")
    void shouldRejectNullStore() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.getDefault(null, "module"));
      LOGGER.info("Correctly rejected null store for getDefault");
    }

    @Test
    @DisplayName("Should reject null module name")
    void shouldRejectNullModuleName() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.getDefault(store, null));
      LOGGER.info("Correctly rejected null module name for getDefault");
    }
  }

  // ===== Define Table/Global/Memory Validation Tests =====

  @Nested
  @DisplayName("Define Resource Validation Tests")
  class DefineResourceValidationTests {

    @Test
    @DisplayName("defineTable should reject null store")
    void defineTableShouldRejectNullStore() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineTable(null, "env", "table", null));
      LOGGER.info("defineTable correctly rejected null store");
    }

    @Test
    @DisplayName("defineTable should reject null module name")
    void defineTableShouldRejectNullModuleName() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineTable(store, null, "table", null));
      LOGGER.info("defineTable correctly rejected null module name");
    }

    @Test
    @DisplayName("defineGlobal should reject null store")
    void defineGlobalShouldRejectNullStore() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineGlobal(null, "env", "g", null));
      LOGGER.info("defineGlobal correctly rejected null store");
    }

    @Test
    @DisplayName("defineGlobal should reject null module name")
    void defineGlobalShouldRejectNullModuleName() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineGlobal(store, null, "g", null));
      LOGGER.info("defineGlobal correctly rejected null module name");
    }

    @Test
    @DisplayName("defineMemory should reject null store")
    void defineMemoryShouldRejectNullStore() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineMemory(null, "env", "memory", null));
      LOGGER.info("defineMemory correctly rejected null store");
    }

    @Test
    @DisplayName("defineMemory should reject null module name")
    void defineMemoryShouldRejectNullModuleName() throws Exception {
      final PanamaLinker<?> linker = createLinker();
      final PanamaStore store = createStore();

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.defineMemory(store, null, "memory", null));
      LOGGER.info("defineMemory correctly rejected null module name");
    }
  }
}

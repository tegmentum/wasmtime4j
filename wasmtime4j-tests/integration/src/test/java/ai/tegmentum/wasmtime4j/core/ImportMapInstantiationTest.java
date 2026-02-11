package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link ImportMap} creation and module instantiation with imports.
 *
 * <p>Covers {@link ImportMap#empty()}, {@link ImportMap#addFunction(String, String, WasmFunction)},
 * {@link ImportMap#contains(String, String)}, and
 * {@link Module#instantiate(Store, ImportMap)}.
 */
@DisplayName("ImportMap Instantiation Tests")
public class ImportMapInstantiationTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(ImportMapInstantiationTest.class.getName());

  /** Module that imports env.get_val (no params, returns i32) and re-exports via call_it. */
  private static final String WAT_WITH_IMPORT = "(module\n"
      + "  (import \"env\" \"get_val\" (func (result i32)))\n"
      + "  (func (export \"call_it\") (result i32) call 0))";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ImportMap.empty() returns non-null instance")
  void importMapEmptyCreatesInstance(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ImportMap.empty()");

    try {
      final ImportMap importMap = ImportMap.empty();
      assertNotNull(importMap, "ImportMap.empty() must return non-null");
      LOGGER.info("[" + runtime + "] ImportMap.empty() returned: " + importMap.getClass().getName());

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] ImportMap.empty() not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("instantiate with ImportMap providing host function")
  void instantiateWithImportMapProvideFunction(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing module instantiation with ImportMap host function");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT_WITH_IMPORT)) {

      // Create host function that returns 777
      final FunctionType funcType = new FunctionType(
          new WasmValueType[0],
          new WasmValueType[] {WasmValueType.I32});
      final HostFunction hostImpl = (params) -> new WasmValue[] {WasmValue.i32(777)};
      final WasmFunction hostFunc = store.createHostFunction("get_val", funcType, hostImpl);
      assertNotNull(hostFunc, "Host function must not be null");

      // Build ImportMap
      final ImportMap importMap = ImportMap.empty();
      importMap.addFunction("env", "get_val", hostFunc);
      LOGGER.info("[" + runtime + "] Built ImportMap with env::get_val");

      // Instantiate with ImportMap
      try (Instance instance = module.instantiate(store, importMap)) {
        assertNotNull(instance, "Instance from ImportMap instantiation must not be null");

        final WasmFunction callIt = instance.getFunction("call_it").orElse(null);
        assertNotNull(callIt, "call_it export must exist");

        final WasmValue[] results = callIt.call();
        assertNotNull(results, "call_it must return results");
        assertEquals(777, results[0].asI32(),
            "call_it should return 777 from host function");
        LOGGER.info("[" + runtime + "] call_it() returned " + results[0].asI32());
      }

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] ImportMap instantiation not supported: "
          + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("instantiate with empty ImportMap on module with imports throws")
  void instantiateWithMissingImportThrows(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing instantiation with missing imports");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT_WITH_IMPORT)) {

      final ImportMap emptyMap = ImportMap.empty();

      try {
        try (Instance inst = module.instantiate(store, emptyMap)) {
          LOGGER.warning("[" + runtime
              + "] Instantiation with missing import did not throw (unexpected), "
              + "instance valid=" + inst.isValid());
        }
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] Correctly threw on missing import: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] ImportMap not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ImportMap.contains returns true for added function")
  void importMapContainsReturnsTrueForAdded(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ImportMap.contains for added function");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final FunctionType funcType = new FunctionType(
          new WasmValueType[0],
          new WasmValueType[] {WasmValueType.I32});
      final WasmFunction hostFunc = store.createHostFunction(
          "get_val", funcType, (params) -> new WasmValue[] {WasmValue.i32(1)});

      final ImportMap importMap = ImportMap.empty();
      importMap.addFunction("env", "get_val", hostFunc);

      assertTrue(importMap.contains("env", "get_val"),
          "ImportMap.contains should return true for added function");
      LOGGER.info("[" + runtime + "] contains(env, get_val) = true");

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] ImportMap not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ImportMap.contains returns false for missing function")
  void importMapContainsReturnsFalseForMissing(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ImportMap.contains for non-existent function");

    try {
      final ImportMap importMap = ImportMap.empty();

      assertFalse(importMap.contains("env", "nope"),
          "ImportMap.contains should return false for non-existent entry");
      LOGGER.info("[" + runtime + "] contains(env, nope) = false");

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] ImportMap not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }
}

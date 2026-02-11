package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link WasmFunction#matchesType(WasmValueType[], WasmValueType[])}.
 *
 * <p>Verifies that the default method correctly matches function signatures against expected
 * parameter and result types, returning true for exact matches and false for mismatches.
 */
@DisplayName("WasmFunction.matchesType() Tests")
public class FunctionSignatureMatchTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(FunctionSignatureMatchTest.class.getName());

  private static final String WAT = "(module\n"
      + "  (func (export \"add\") (param i32 i32) (result i32)\n"
      + "    local.get 0 local.get 1 i32.add)\n"
      + "  (func (export \"nop\"))\n"
      + "  (func (export \"id64\") (param i64) (result i64)\n"
      + "    local.get 0))";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("matchesType returns true for correct signature")
  void matchesTypeCorrectSignatureReturnsTrue(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing matchesType with correct (i32,i32)->i32 signature");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT);
        Instance instance = module.instantiate(store)) {

      final WasmFunction add = instance.getFunction("add").orElse(null);
      assertNotNull(add, "add export must exist");

      final boolean matches = add.matchesType(
          new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
          new WasmValueType[] {WasmValueType.I32});
      assertTrue(matches,
          "matchesType should return true for exact signature (i32,i32)->i32");
      LOGGER.info("[" + runtime + "] matchesType(i32,i32->i32)=" + matches);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("matchesType returns false for wrong param types")
  void matchesTypeWrongParamsReturnsFalse(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing matchesType with wrong param types (i64,i64)->i32");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT);
        Instance instance = module.instantiate(store)) {

      final WasmFunction add = instance.getFunction("add").orElse(null);
      assertNotNull(add, "add export must exist");

      final boolean matches = add.matchesType(
          new WasmValueType[] {WasmValueType.I64, WasmValueType.I64},
          new WasmValueType[] {WasmValueType.I32});
      assertFalse(matches,
          "matchesType should return false for wrong param types (i64,i64)->i32");
      LOGGER.info("[" + runtime + "] matchesType(i64,i64->i32)=" + matches);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("matchesType returns false for wrong result type")
  void matchesTypeWrongResultReturnsFalse(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing matchesType with wrong result type (i32,i32)->i64");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT);
        Instance instance = module.instantiate(store)) {

      final WasmFunction add = instance.getFunction("add").orElse(null);
      assertNotNull(add, "add export must exist");

      final boolean matches = add.matchesType(
          new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
          new WasmValueType[] {WasmValueType.I64});
      assertFalse(matches,
          "matchesType should return false for wrong result type (i32,i32)->i64");
      LOGGER.info("[" + runtime + "] matchesType(i32,i32->i64)=" + matches);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("matchesType returns true for void signature")
  void matchesTypeVoidSignature(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing matchesType with void()->void signature on nop");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT);
        Instance instance = module.instantiate(store)) {

      final WasmFunction nop = instance.getFunction("nop").orElse(null);
      assertNotNull(nop, "nop export must exist");

      final boolean matches = nop.matchesType(
          new WasmValueType[0],
          new WasmValueType[0]);
      assertTrue(matches,
          "matchesType should return true for void()->void on nop function");
      LOGGER.info("[" + runtime + "] matchesType(()->void)=" + matches);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("matchesType returns false for wrong arity")
  void matchesTypeWrongArityReturnsFalse(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing matchesType with wrong arity (i32)->i32 on add");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT);
        Instance instance = module.instantiate(store)) {

      final WasmFunction add = instance.getFunction("add").orElse(null);
      assertNotNull(add, "add export must exist");

      final boolean matches = add.matchesType(
          new WasmValueType[] {WasmValueType.I32},
          new WasmValueType[] {WasmValueType.I32});
      assertFalse(matches,
          "matchesType should return false for wrong arity (i32)->i32 on (i32,i32)->i32 func");
      LOGGER.info("[" + runtime + "] matchesType(i32->i32) on add=" + matches);
    }
  }
}

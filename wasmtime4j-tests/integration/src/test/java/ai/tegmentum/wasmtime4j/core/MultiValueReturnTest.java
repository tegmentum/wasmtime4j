package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for WASM functions that return multiple values.
 *
 * <p>Multi-value return is a WebAssembly feature that allows functions to return more than one
 * value on the stack. This tests the marshalling of multiple return values from WASM to Java.
 */
@DisplayName("Multi-Value Return Tests")
public class MultiValueReturnTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(MultiValueReturnTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Function returns two values (i32, i64)")
  public void testReturnTwoValues(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        "(module\n"
            + "  (func (export \"two_values\") (result i32 i64)\n"
            + "    i32.const 42\n"
            + "    i64.const 123456789\n"
            + "  )\n"
            + ")";

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results = instance.callFunction("two_values");

    assertEquals(2, results.length, "Should return 2 values");
    assertEquals(WasmValueType.I32, results[0].getType(), "First value should be i32");
    assertEquals(42, results[0].asInt(), "First value should be 42");
    assertEquals(WasmValueType.I64, results[1].getType(), "Second value should be i64");
    assertEquals(123456789L, results[1].asLong(), "Second value should be 123456789");

    LOGGER.info("Two-value return: i32=" + results[0].asInt() + ", i64=" + results[1].asLong());

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Function returns three values (i32, f32, f64)")
  public void testReturnThreeValues(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        "(module\n"
            + "  (func (export \"three_values\") (result i32 f32 f64)\n"
            + "    i32.const 99\n"
            + "    f32.const 3.14\n"
            + "    f64.const 2.718281828\n"
            + "  )\n"
            + ")";

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results = instance.callFunction("three_values");

    assertEquals(3, results.length, "Should return 3 values");
    assertEquals(99, results[0].asInt(), "First value should be 99");
    assertEquals(3.14f, results[1].asFloat(), 0.01f, "Second value should be ~3.14");
    assertEquals(
        2.718281828, results[2].asDouble(), 0.000001, "Third value should be ~2.718281828");

    LOGGER.info(
        "Three-value return: i32="
            + results[0].asInt()
            + ", f32="
            + results[1].asFloat()
            + ", f64="
            + results[2].asDouble());

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Function returns four mixed values with edge values")
  public void testReturnFourMixedValues(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        "(module\n"
            + "  (func (export \"four_values\") (result i32 i64 f32 f64)\n"
            + "    i32.const 2147483647\n"
            + "    i64.const 9223372036854775807\n"
            + "    f32.const 0.0\n"
            + "    f64.const 1.7976931348623157e+308\n"
            + "  )\n"
            + ")";

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results = instance.callFunction("four_values");

    assertEquals(4, results.length, "Should return 4 values");
    assertEquals(Integer.MAX_VALUE, results[0].asInt(), "First value should be Integer.MAX_VALUE");
    assertEquals(Long.MAX_VALUE, results[1].asLong(), "Second value should be Long.MAX_VALUE");
    assertEquals(0.0f, results[2].asFloat(), 0.0f, "Third value should be 0.0f");
    assertEquals(
        Double.MAX_VALUE, results[3].asDouble(), 0.0, "Fourth value should be Double.MAX_VALUE");

    LOGGER.info(
        "Four-value return: i32="
            + results[0].asInt()
            + ", i64="
            + results[1].asLong()
            + ", f32="
            + results[2].asFloat()
            + ", f64="
            + results[3].asDouble());

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Function with parameters returns multiple values")
  public void testParametersAndMultiReturn(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Function that takes two i32 params and returns their sum and product
    final String wat =
        "(module\n"
            + "  (func (export \"sum_and_product\") (param i32 i32) (result i32 i32)\n"
            + "    local.get 0\n"
            + "    local.get 1\n"
            + "    i32.add\n"
            + "    local.get 0\n"
            + "    local.get 1\n"
            + "    i32.mul\n"
            + "  )\n"
            + ")";

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results =
        instance.callFunction("sum_and_product", WasmValue.i32(6), WasmValue.i32(7));

    assertEquals(2, results.length, "Should return 2 values");
    assertEquals(13, results[0].asInt(), "Sum of 6 + 7 should be 13");
    assertEquals(42, results[1].asInt(), "Product of 6 * 7 should be 42");

    LOGGER.info(
        "sum_and_product(6, 7): sum=" + results[0].asInt() + ", product=" + results[1].asInt());

    instance.close();
    store.close();
    engine.close();
  }
}

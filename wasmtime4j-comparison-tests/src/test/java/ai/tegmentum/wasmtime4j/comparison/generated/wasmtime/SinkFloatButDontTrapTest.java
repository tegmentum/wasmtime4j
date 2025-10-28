package ai.tegmentum.wasmtime4j.comparison.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: sink-float-but-dont-trap.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class SinkFloatButDontTrapTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = SinkFloatButDontTrapTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("sink float but dont trap")
  public void testSinkFloatButDontTrap(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/SinkFloatButDontTrapTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/SinkFloatButDontTrapTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_return ( invoke "select-with-sink" ( i32.const 0xfff8)) ( f64.const 0))
      runner.assertReturn(
          "select-with-sink", new WasmValue[] {WasmValue.f64(0)}, WasmValue.i32(0xfff8));

      // ( assert_return ( invoke "select-with-fcmp-and-sink" ( i32.const 0xfff8) ( f64.const 0) (
      // f64.const 0)) ( f64.const 1))
      runner.assertReturn(
          "select-with-fcmp-and-sink",
          new WasmValue[] {WasmValue.f64(1)},
          WasmValue.i32(0xfff8),
          WasmValue.f64(0),
          WasmValue.f64(0));

      // ( assert_trap ( invoke "select-with-sink" ( i32.const 0xfff9)) "out of bounds")
      runner.assertTrap("select-with-sink", "out of bounds", WasmValue.i32(0xfff9));

      // ( assert_trap ( invoke "select-with-fcmp-and-sink" ( i32.const 0xfff9) ( f64.const 0) (
      // f64.const 0)) "out of bounds")
      runner.assertTrap(
          "select-with-fcmp-and-sink",
          "out of bounds",
          WasmValue.i32(0xfff9),
          WasmValue.f64(0),
          WasmValue.f64(0));

      // ( assert_trap ( invoke "select-with-sink-other-way" ( i32.const 0xfff9)) "out of bounds")
      runner.assertTrap("select-with-sink-other-way", "out of bounds", WasmValue.i32(0xfff9));

      // ( assert_trap ( invoke "select-with-fcmp-and-sink-other-way" ( i32.const 0xfff9) (
      // f64.const 0) ( f64.const 0)) "out of bounds")
      runner.assertTrap(
          "select-with-fcmp-and-sink-other-way",
          "out of bounds",
          WasmValue.i32(0xfff9),
          WasmValue.f64(0),
          WasmValue.f64(0));
    }
  }
}

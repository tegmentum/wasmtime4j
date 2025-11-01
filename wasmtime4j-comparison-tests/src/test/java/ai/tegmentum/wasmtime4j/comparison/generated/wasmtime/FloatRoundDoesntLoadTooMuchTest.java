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
 * Generated test from WAST file: float-round-doesnt-load-too-much.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class FloatRoundDoesntLoadTooMuchTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = FloatRoundDoesntLoadTooMuchTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("float round doesnt load too much")
  public void testFloatRoundDoesntLoadTooMuch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/FloatRoundDoesntLoadTooMuchTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/FloatRoundDoesntLoadTooMuchTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_return ( invoke "ceil" ( i32.const 0xfff8)) ( f64.const 0))
      runner.assertReturn("ceil", new WasmValue[] {WasmValue.f64(0.0)}, WasmValue.i32(0xfff8));

      // ( assert_return ( invoke "trunc" ( i32.const 0xfff8)) ( f64.const 0))
      runner.assertReturn("trunc", new WasmValue[] {WasmValue.f64(0.0)}, WasmValue.i32(0xfff8));

      // ( assert_return ( invoke "floor" ( i32.const 0xfff8)) ( f64.const 0))
      runner.assertReturn("floor", new WasmValue[] {WasmValue.f64(0.0)}, WasmValue.i32(0xfff8));

      // ( assert_return ( invoke "nearest" ( i32.const 0xfff8)) ( f64.const 0))
      runner.assertReturn("nearest", new WasmValue[] {WasmValue.f64(0.0)}, WasmValue.i32(0xfff8));
    }
  }
}

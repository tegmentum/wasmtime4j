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
 * Generated test from WAST file: func-400-params.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class Func400ParamsTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = Func400ParamsTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("func 400 params")
  public void testFunc400Params(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/Func400ParamsTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/Func400ParamsTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_return ( invoke "x" ( i32.const 1) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) (
      // i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0) ( i32.const 0)) (
      // i32.const 1))
      runner.assertReturn(
          "x",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0));
    }
  }
}

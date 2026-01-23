package ai.tegmentum.wasmtime4j.wasmtime.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: many-results-with-exceptions.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class ManyResultsWithExceptionsTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = ManyResultsWithExceptionsTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("many results with exceptions")
  public void testManyResultsWithExceptions(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ManyResultsWithExceptionsTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/ManyResultsWithExceptionsTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_return ( invoke "f") ( i32.const 0) ( i32.const 1) ( i32.const 2) ( i32.const 3) (
      // i32.const 4) ( i32.const 5) ( i32.const 6) ( i32.const 7) ( i32.const 8) ( i32.const 9) (
      // i32.const 10) ( i32.const 11) ( i32.const 12) ( i32.const 13) ( i32.const 14) ( i32.const
      // 15) ( i32.const 16))
      runner.assertReturn(
          "f",
          new WasmValue[] {
            WasmValue.i32(0),
            WasmValue.i32(1),
            WasmValue.i32(2),
            WasmValue.i32(3),
            WasmValue.i32(4),
            WasmValue.i32(5),
            WasmValue.i32(6),
            WasmValue.i32(7),
            WasmValue.i32(8),
            WasmValue.i32(9),
            WasmValue.i32(10),
            WasmValue.i32(11),
            WasmValue.i32(12),
            WasmValue.i32(13),
            WasmValue.i32(14),
            WasmValue.i32(15),
            WasmValue.i32(16)
          });

      // ( assert_return ( invoke "f2" ( i32.const 0) ( i32.const 1) ( i32.const 2) ( i32.const 3) (
      // i32.const 4) ( i32.const 5) ( i32.const 6) ( i32.const 7) ( i32.const 8) ( i32.const 9) (
      // i32.const 10) ( i32.const 11) ( i32.const 12) ( i32.const 13) ( i32.const 14) ( i32.const
      // 15) ( i32.const 16)) ( i32.const 0) ( i32.const 1) ( i32.const 2) ( i32.const 3) (
      // i32.const 4) ( i32.const 5) ( i32.const 6) ( i32.const 7) ( i32.const 8) ( i32.const 9) (
      // i32.const 10) ( i32.const 11) ( i32.const 12) ( i32.const 13) ( i32.const 14) ( i32.const
      // 15) ( i32.const 16))
      runner.assertReturn(
          "f2",
          new WasmValue[] {
            WasmValue.i32(0),
            WasmValue.i32(1),
            WasmValue.i32(2),
            WasmValue.i32(3),
            WasmValue.i32(4),
            WasmValue.i32(5),
            WasmValue.i32(6),
            WasmValue.i32(7),
            WasmValue.i32(8),
            WasmValue.i32(9),
            WasmValue.i32(10),
            WasmValue.i32(11),
            WasmValue.i32(12),
            WasmValue.i32(13),
            WasmValue.i32(14),
            WasmValue.i32(15),
            WasmValue.i32(16)
          },
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(2),
          WasmValue.i32(3),
          WasmValue.i32(4),
          WasmValue.i32(5),
          WasmValue.i32(6),
          WasmValue.i32(7),
          WasmValue.i32(8),
          WasmValue.i32(9),
          WasmValue.i32(10),
          WasmValue.i32(11),
          WasmValue.i32(12),
          WasmValue.i32(13),
          WasmValue.i32(14),
          WasmValue.i32(15),
          WasmValue.i32(16));
    }
  }
}

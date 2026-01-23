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
 * Generated test from WAST file: fib.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class FibTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = FibTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("fib")
  public void testFib(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/FibTest_module1.wat
      final String moduleWat1 =
          loadResource("/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/FibTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_return ( invoke "fib" ( i32.const 0)) ( i32.const 1))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));

      // ( assert_return ( invoke "fib" ( i32.const 1)) ( i32.const 1))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));

      // ( assert_return ( invoke "fib" ( i32.const 2)) ( i32.const 2))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(2)}, WasmValue.i32(2));

      // ( assert_return ( invoke "fib" ( i32.const 3)) ( i32.const 3))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(3)}, WasmValue.i32(3));

      // ( assert_return ( invoke "fib" ( i32.const 4)) ( i32.const 5))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(5)}, WasmValue.i32(4));

      // ( assert_return ( invoke "fib" ( i32.const 5)) ( i32.const 8))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(8)}, WasmValue.i32(5));

      // ( assert_return ( invoke "fib" ( i32.const 6)) ( i32.const 13))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(13)}, WasmValue.i32(6));

      // ( assert_return ( invoke "fib" ( i32.const 7)) ( i32.const 21))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(21)}, WasmValue.i32(7));

      // ( assert_return ( invoke "fib" ( i32.const 8)) ( i32.const 34))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(34)}, WasmValue.i32(8));

      // ( assert_return ( invoke "fib" ( i32.const 9)) ( i32.const 55))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(55)}, WasmValue.i32(9));

      // ( assert_return ( invoke "fib" ( i32.const 10)) ( i32.const 89))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(89)}, WasmValue.i32(10));

      // Compile and instantiate module 2
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/FibTest_module2.wat
      final String moduleWat2 =
          loadResource("/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/FibTest_module2.wat");
      runner.compileAndInstantiate(moduleWat2);

      // ( assert_return ( invoke "fib" ( i32.const 0)) ( i32.const 1))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));

      // ( assert_return ( invoke "fib" ( i32.const 1)) ( i32.const 1))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));

      // ( assert_return ( invoke "fib" ( i32.const 2)) ( i32.const 2))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(2)}, WasmValue.i32(2));

      // ( assert_return ( invoke "fib" ( i32.const 3)) ( i32.const 3))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(3)}, WasmValue.i32(3));

      // ( assert_return ( invoke "fib" ( i32.const 4)) ( i32.const 5))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(5)}, WasmValue.i32(4));

      // ( assert_return ( invoke "fib" ( i32.const 5)) ( i32.const 8))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(8)}, WasmValue.i32(5));

      // ( assert_return ( invoke "fib" ( i32.const 6)) ( i32.const 13))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(13)}, WasmValue.i32(6));

      // ( assert_return ( invoke "fib" ( i32.const 7)) ( i32.const 21))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(21)}, WasmValue.i32(7));

      // ( assert_return ( invoke "fib" ( i32.const 8)) ( i32.const 34))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(34)}, WasmValue.i32(8));

      // ( assert_return ( invoke "fib" ( i32.const 9)) ( i32.const 55))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(55)}, WasmValue.i32(9));

      // ( assert_return ( invoke "fib" ( i32.const 10)) ( i32.const 89))
      runner.assertReturn("fib", new WasmValue[] {WasmValue.i32(89)}, WasmValue.i32(10));
    }
  }
}

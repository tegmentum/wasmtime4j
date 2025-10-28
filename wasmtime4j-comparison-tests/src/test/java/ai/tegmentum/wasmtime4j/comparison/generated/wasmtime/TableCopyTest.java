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
 * Generated test from WAST file: table_copy.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class TableCopyTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = TableCopyTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table copy")
  public void testTableCopy(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/TableCopyTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/TableCopyTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_return ( invoke "call" ( i32.const 1) ( i32.const 0) ( i32.const 0) ( i32.const
      // 0)) ( i32.const 1))
      runner.assertReturn(
          "call",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0));

      // ( assert_return ( invoke "call" ( i32.const 0) ( i32.const 1) ( i32.const 0) ( i32.const
      // 1)) ( i32.const 1))
      runner.assertReturn(
          "call",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(1));

      // ( assert_return ( invoke "call" ( i32.const 0) ( i32.const 0) ( i32.const 1) ( i32.const
      // 2)) ( i32.const 1))
      runner.assertReturn(
          "call",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(2));

      // ( assert_return ( invoke "call" ( i32.const 0) ( i32.const 1) ( i32.const 0) ( i32.const
      // 0)) ( i32.const 1))
      runner.assertReturn(
          "call",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0));

      // ( assert_return ( invoke "call" ( i32.const 0) ( i32.const 0) ( i32.const 1) ( i32.const
      // 1)) ( i32.const 1))
      runner.assertReturn(
          "call",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(1),
          WasmValue.i32(1));

      // ( assert_return ( invoke "call" ( i32.const 1) ( i32.const 0) ( i32.const 0) ( i32.const
      // 2)) ( i32.const 1))
      runner.assertReturn(
          "call",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.i32(1),
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(2));

      // ( assert_trap ( invoke "copy" ( i32.const 7) ( i32.const 0) ( i32.const 3)) "undefined
      // element")
      runner.assertTrap(
          "copy", "undefined element", WasmValue.i32(7), WasmValue.i32(0), WasmValue.i32(3));
    }
  }
}

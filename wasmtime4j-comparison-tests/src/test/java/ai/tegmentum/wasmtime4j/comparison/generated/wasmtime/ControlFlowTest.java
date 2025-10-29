package ai.tegmentum.wasmtime4j.comparison.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Exception;
import java.lang.String;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: control-flow.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream
 * Wasmtime implementation for this test case.
 */
public final class ControlFlowTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = ControlFlowTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("control flow")
  public void testControlFlow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module1.wat
      final String moduleWat1 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // Compile and instantiate module 2
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module2.wat
      final String moduleWat2 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module2.wat");
      runner.compileAndInstantiate(moduleWat2);

      // ( assert_return ( invoke "if-without-result" ( i32.const 2) ( i32.const 3)) ( i32.const 2))
      runner.assertReturn("if-without-result", new WasmValue[] { WasmValue.i32(2) }, WasmValue.i32(2), WasmValue.i32(3));

      // Compile and instantiate module 3
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module3.wat
      final String moduleWat3 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module3.wat");
      runner.compileAndInstantiate(moduleWat3);

      // ( assert_return ( invoke "block" ( i32.const 10) ( i32.const 20)) ( i32.const 10))
      runner.assertReturn("block", new WasmValue[] { WasmValue.i32(10) }, WasmValue.i32(10), WasmValue.i32(20));

      // Compile and instantiate module 4
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module4.wat
      final String moduleWat4 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module4.wat");
      runner.compileAndInstantiate(moduleWat4);

      // ( assert_return ( invoke "br_block" ( i32.const 5) ( i32.const 7)) ( i32.const 12))
      runner.assertReturn("br_block", new WasmValue[] { WasmValue.i32(12) }, WasmValue.i32(5), WasmValue.i32(7));

      // Compile and instantiate module 5
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module5.wat
      final String moduleWat5 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module5.wat");
      runner.compileAndInstantiate(moduleWat5);

      // ( assert_return ( invoke "brif_block" ( i32.const 5) ( i32.const 7)) ( i32.const 12))
      runner.assertReturn("brif_block", new WasmValue[] { WasmValue.i32(12) }, WasmValue.i32(5), WasmValue.i32(7));

      // Compile and instantiate module 6
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module6.wat
      final String moduleWat6 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module6.wat");
      runner.compileAndInstantiate(moduleWat6);

      // ( assert_return ( invoke "brif_block_passthru" ( i32.const 0) ( i32.const 3)) ( i32.const 6))
      runner.assertReturn("brif_block_passthru", new WasmValue[] { WasmValue.i32(6) }, WasmValue.i32(0), WasmValue.i32(3));

      // Compile and instantiate module 7
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module7.wat
      final String moduleWat7 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module7.wat");
      runner.compileAndInstantiate(moduleWat7);

      // Compile and instantiate module 8
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module8.wat
      final String moduleWat8 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ControlFlowTest_module8.wat");
      runner.compileAndInstantiate(moduleWat8);

      // ( assert_return ( invoke "br_table" ( i32.const 0)) ( i32.const 110))
      runner.assertReturn("br_table", new WasmValue[] { WasmValue.i32(110) }, WasmValue.i32(0));

      // ( assert_return ( invoke "br_table" ( i32.const 1)) ( i32.const 12))
      runner.assertReturn("br_table", new WasmValue[] { WasmValue.i32(12) }, WasmValue.i32(1));

      // ( assert_return ( invoke "br_table" ( i32.const 2)) ( i32.const 4))
      runner.assertReturn("br_table", new WasmValue[] { WasmValue.i32(4) }, WasmValue.i32(2));

      // ( assert_return ( invoke "br_table" ( i32.const 3)) ( i32.const 1116))
      runner.assertReturn("br_table", new WasmValue[] { WasmValue.i32(1116) }, WasmValue.i32(3));

      // ( assert_return ( invoke "br_table" ( i32.const 4)) ( i32.const 118))
      runner.assertReturn("br_table", new WasmValue[] { WasmValue.i32(118) }, WasmValue.i32(4));

      // ( assert_return ( invoke "br_table" ( i32.const 5)) ( i32.const 20))
      runner.assertReturn("br_table", new WasmValue[] { WasmValue.i32(20) }, WasmValue.i32(5));

      // ( assert_return ( invoke "br_table" ( i32.const 6)) ( i32.const 12))
      runner.assertReturn("br_table", new WasmValue[] { WasmValue.i32(12) }, WasmValue.i32(6));

      // ( assert_return ( invoke "br_table" ( i32.const 7)) ( i32.const 1124))
      runner.assertReturn("br_table", new WasmValue[] { WasmValue.i32(1124) }, WasmValue.i32(7));

      // ( assert_return ( invoke "br_table" ( i32.const 8)) ( i32.const 126))
      runner.assertReturn("br_table", new WasmValue[] { WasmValue.i32(126) }, WasmValue.i32(8));

    }
  }
}

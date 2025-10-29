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
 * Generated test from WAST file: partial-init-table-segment.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream
 * Wasmtime implementation for this test case.
 */
public final class PartialInitTableSegmentTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = PartialInitTableSegmentTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("partial init table segment")
  public void testPartialInitTableSegment(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/PartialInitTableSegmentTest_module1.wat
      final String moduleWat1 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/PartialInitTableSegmentTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( register "m" $m)
      runner.registerModule("m");

      // ( assert_trap ( module ( table ( import "m" "table") 10 funcref) ( func $one ( result i32) ( i32.const 1)) ( elem ( i32.const 7) $one) ( elem ( i32.const 9) $one $one $one)) "out of bounds")
      // TODO: Parse assert_trap - no function name found

      // ( assert_return ( invoke "indirect-call" ( i32.const 7)) ( i32.const 1))
      runner.assertReturn("indirect-call", new WasmValue[] { WasmValue.i32(1) }, WasmValue.i32(7));

      // ( assert_return ( invoke "indirect-call" ( i32.const 9)) ( i32.const 0))
      runner.assertReturn("indirect-call", new WasmValue[] { WasmValue.i32(0) }, WasmValue.i32(9));

    }
  }
}

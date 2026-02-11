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
 * Generated test from WAST file: partial-init-memory-segment.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class PartialInitMemorySegmentTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = PartialInitMemorySegmentTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("partial init memory segment")
  public void testPartialInitMemorySegment(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/PartialInitMemorySegmentTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/PartialInitMemorySegmentTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( register "m" $m)
      runner.registerModule("m");

      // ( assert_trap ( module ( memory ( import "m" "mem") 1) ( data ( i32.const 0) "abc") ( data
      // ( i32.const 65530) "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz")) "out of bounds")
      // Module with data segment that exceeds memory bounds should trap on instantiation
      runner.assertModuleTrap(
          "(module (memory (import \"m\" \"mem\") 1) "
              + "(data (i32.const 0) \"abc\") "
              + "(data (i32.const 65530) \"zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz\"))",
          "out of bounds");

      // ( assert_return ( invoke $m "load" ( i32.const 0)) ( i32.const 97))
      // After partial initialization failure, the "abc" data should still be written at offset 0
      runner.assertReturn("m", "load", new WasmValue[] {WasmValue.i32(97)}, WasmValue.i32(0));

      // ( assert_return ( invoke $m "load" ( i32.const 1)) ( i32.const 98))
      runner.assertReturn("m", "load", new WasmValue[] {WasmValue.i32(98)}, WasmValue.i32(1));

      // ( assert_return ( invoke $m "load" ( i32.const 2)) ( i32.const 99))
      runner.assertReturn("m", "load", new WasmValue[] {WasmValue.i32(99)}, WasmValue.i32(2));

      // ( assert_return ( invoke $m "load" ( i32.const 65530)) ( i32.const 0))
      // The failed segment at 65530 should NOT have written any data (out of bounds)
      runner.assertReturn("m", "load", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(65530));

      // ( assert_return ( invoke $m "load" ( i32.const 65531)) ( i32.const 0))
      runner.assertReturn("m", "load", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(65531));

      // ( assert_return ( invoke $m "load" ( i32.const 65532)) ( i32.const 0))
      runner.assertReturn("m", "load", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(65532));

      // ( assert_return ( invoke $m "load" ( i32.const 65533)) ( i32.const 0))
      runner.assertReturn("m", "load", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(65533));

      // ( assert_return ( invoke $m "load" ( i32.const 65534)) ( i32.const 0))
      runner.assertReturn("m", "load", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(65534));

      // ( assert_return ( invoke $m "load" ( i32.const 65535)) ( i32.const 0))
      runner.assertReturn("m", "load", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(65535));
    }
  }
}

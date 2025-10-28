package ai.tegmentum.wasmtime4j.comparison.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
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
              "/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/PartialInitMemorySegmentTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_trap ( module ( memory ( import "m" "mem") 1) ( data ( i32.const 0) "abc") ( data
      // ( i32.const 65530) "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz")) "out of bounds")
      // TODO: Parse assert_trap - no function name found

      // ( assert_return ( invoke $m "load" ( i32.const 0)) ( i32.const 97))
      // TODO: Parse assert_return - no function name found

      // ( assert_return ( invoke $m "load" ( i32.const 1)) ( i32.const 98))
      // TODO: Parse assert_return - no function name found

      // ( assert_return ( invoke $m "load" ( i32.const 2)) ( i32.const 99))
      // TODO: Parse assert_return - no function name found

      // ( assert_return ( invoke $m "load" ( i32.const 65530)) ( i32.const 0))
      // TODO: Parse assert_return - no function name found

      // ( assert_return ( invoke $m "load" ( i32.const 65531)) ( i32.const 0))
      // TODO: Parse assert_return - no function name found

      // ( assert_return ( invoke $m "load" ( i32.const 65532)) ( i32.const 0))
      // TODO: Parse assert_return - no function name found

      // ( assert_return ( invoke $m "load" ( i32.const 65533)) ( i32.const 0))
      // TODO: Parse assert_return - no function name found

      // ( assert_return ( invoke $m "load" ( i32.const 65534)) ( i32.const 0))
      // TODO: Parse assert_return - no function name found

      // ( assert_return ( invoke $m "load" ( i32.const 65535)) ( i32.const 0))
      // TODO: Parse assert_return - no function name found

    }
  }
}

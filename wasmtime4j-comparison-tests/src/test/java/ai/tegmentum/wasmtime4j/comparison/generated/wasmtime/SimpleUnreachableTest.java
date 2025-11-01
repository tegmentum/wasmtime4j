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
 * Generated test from WAST file: simple-unreachable.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class SimpleUnreachableTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = SimpleUnreachableTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("simple unreachable")
  public void testSimpleUnreachable(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/SimpleUnreachableTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/SimpleUnreachableTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_trap ( invoke "f") "unreachable")
      runner.assertTrap("f", "unreachable");
    }
  }
}

package ai.tegmentum.wasmtime4j.comparison.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
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
 * Generated test from WAST file: externref-table-dropped-segment-issue-8281.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream
 * Wasmtime implementation for this test case.
 */
public final class ExternrefTableDroppedSegmentIssue8281Test extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = ExternrefTableDroppedSegmentIssue8281Test.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("externref table dropped segment issue 8281")
  public void testExternrefTableDroppedSegmentIssue8281(final RuntimeType runtime) throws
      Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ExternrefTableDroppedSegmentIssue8281Test_module1.wat
      final String moduleWat1 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ExternrefTableDroppedSegmentIssue8281Test_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_return ( invoke "f1"))
      runner.invoke("f1");

      // ( assert_return ( invoke "f2"))
      runner.invoke("f2");

      // ( assert_return ( invoke "f3"))
      runner.invoke("f3");

    }
  }
}

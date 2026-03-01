/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.wasmtime.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: externref-table-dropped-segment-issue-8281.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class ExternrefTableDroppedSegmentIssue8281Test extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is =
        ExternrefTableDroppedSegmentIssue8281Test.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("externref table dropped segment issue 8281")
  public void testExternrefTableDroppedSegmentIssue8281(final RuntimeType runtime)
      throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ExternrefTableDroppedSegmentIssue8281Test_module1.wat
      final String path =
          "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/"
              + "ExternrefTableDroppedSegmentIssue8281Test_module1.wat";
      final String moduleWat1 = loadResource(path);
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

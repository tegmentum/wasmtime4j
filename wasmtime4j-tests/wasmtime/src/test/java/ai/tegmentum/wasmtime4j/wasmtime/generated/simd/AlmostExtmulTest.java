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
package ai.tegmentum.wasmtime4j.wasmtime.generated.simd;

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
 * Generated test from WAST file: simd/almost-extmul.wast
 *
 * <p>Regression test from #3337, there's a multiplication that sort of looks like an extmul and
 * codegen shouldn't pattern match too much.
 *
 * <p>Requires: simd = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/simd/almost-extmul.wast
 */
public final class AlmostExtmulTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = AlmostExtmulTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("module compiles without extmul pattern match bug")
  public void testModuleCompilation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/simd/AlmostExtmulTest_module1.wat");
      // This test just verifies the module compiles without triggering the pattern match bug
      runner.compileAndInstantiate(moduleWat);
    }
  }
}

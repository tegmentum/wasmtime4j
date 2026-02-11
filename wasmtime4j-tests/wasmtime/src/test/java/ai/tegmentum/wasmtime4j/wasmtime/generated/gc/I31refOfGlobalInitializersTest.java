/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wasmtime.generated.gc;

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
 * Generated test from WAST file: gc/i31ref-of-global-initializers.wast
 *
 * <p>Tests i31ref usage in global constant expressions and table initializers. The full WAST file
 * contains multiple modules that test cross-module imports, but this test validates only the first
 * module which exports two i32 globals (g1=42, g2=99).
 *
 * <p>The full WAST file tests:
 *
 * <ul>
 *   <li>Module $env: Exports two i32 globals (g1=42, g2=99)
 *   <li>Module $i31ref_of_global_const_expr_and_tables: Imports globals and uses them in ref.i31
 *       expressions to initialize tables
 *   <li>Module $i31ref_of_global_const_expr_and_globals: Uses imported global in ref.i31 expression
 *       to initialize an i31ref global
 * </ul>
 *
 * <p>Note: The full test requires module registration for cross-module imports. This compile-only
 * test validates that the first module compiles correctly.
 *
 * <p>Requires: gc = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/gc/i31ref-of-global-initializers.wast
 */
public final class I31refOfGlobalInitializersTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = I31refOfGlobalInitializersTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("i31ref in global initializers compiles correctly")
  public void testI31refOfGlobalInitializersCompile(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/gc/"
                  + "I31refOfGlobalInitializersTest_module1.wat");
      // Verifies that the first module (exporting globals for i31ref testing) compiles
      runner.compileAndInstantiate(moduleWat);
    }
  }
}

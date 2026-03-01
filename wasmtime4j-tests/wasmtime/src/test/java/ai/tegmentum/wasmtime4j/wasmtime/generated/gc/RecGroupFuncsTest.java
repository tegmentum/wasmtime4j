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
 * Generated test from WAST file: gc/rec-group-funcs.wast
 *
 * <p>Tests that function types in recursive groups are properly canonicalized across modules at the
 * engine level. This canonicalization is required for cross-module imports to work correctly.
 *
 * <p>The full WAST file contains two modules:
 *
 * <ul>
 *   <li>Module $m1: Defines a pair of mutually recursive function types ($type_a and $type_b)
 *       within a rec group. Each type returns an i32 and a nullable reference to the other type.
 *       Exports func_a and func_b implementing these types.
 *   <li>Module $m2: Defines the same recursive type pair and imports func_a and func_b from m1. The
 *       import succeeds because the types are canonically equivalent across modules.
 * </ul>
 *
 * <p>Note: The full test requires module registration for cross-module imports. This compile-only
 * test validates that the first module with recursive type definitions compiles correctly.
 *
 * <p>Requires: gc = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/gc/rec-group-funcs.wast
 */
public final class RecGroupFuncsTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = RecGroupFuncsTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("recursive group function types compile correctly")
  public void testRecGroupFuncsCompile(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/gc/RecGroupFuncsTest_module1.wat");
      // Verifies that recursive function type groups compile and the module instantiates
      runner.compileAndInstantiate(moduleWat);
    }
  }
}

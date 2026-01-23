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

package ai.tegmentum.wasmtime4j.wasmtime.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: stack_overflow.wast
 *
 * <p>This test validates stack overflow detection - verifying that deep recursion is properly
 * detected and trapped with a "call stack exhausted" error.
 *
 * <p>Note: Disabled because stack overflow handling causes JVM crash (SIGILL) on aarch64. The
 * native library's stack overflow signal handler is not properly configured on this platform.
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/stack_overflow.wast
 */
@Disabled(
    "Stack overflow detection causes JVM crash (SIGILL) on aarch64. "
        + "The signals_based_traps(false) config helps with memory traps but stack overflow "
        + "detection still conflicts with JVM signal handlers on ARM64 Darwin.")
public final class StackOverflowTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = StackOverflowTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("stack overflow - direct recursion")
  public void testStackOverflowDirectRecursion(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Module with direct recursion: foo calls foo
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/StackOverflowTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // (assert_exhaustion (invoke "stack_overflow") "call stack exhausted")
      runner.assertTrap("stack_overflow", "stack overflow");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("stack overflow - mutual recursion")
  public void testStackOverflowMutualRecursion(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Module with mutual recursion: foo calls bar, bar calls foo
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/StackOverflowTest_module2.wat");
      runner.compileAndInstantiate(moduleWat);

      // (assert_exhaustion (invoke "stack_overflow") "call stack exhausted")
      runner.assertTrap("stack_overflow", "stack overflow");
    }
  }
}

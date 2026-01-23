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
 * Generated test from WAST file: br-table-fuzzbug.wast
 *
 * <p>This test validates br_table instruction edge cases - a regression test case from Mozilla Bug
 * 1657062 validating that Cranelift can compile WebAssembly with complex br_table control flow.
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/br-table-fuzzbug.wast
 */
public final class BrTableFuzzbugTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = BrTableFuzzbugTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("br_table fuzzbug compilation test")
  public void testBrTableFuzzbug(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/"
                  + "BrTableFuzzbugTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // br_table returns the index clamped to [0, 3]
      // Test index 0 -> returns 0
      runner.assertReturn(
          "main",
          new WasmValue[] {WasmValue.i32(0)},
          WasmValue.i32(0),
          WasmValue.i32(0),
          WasmValue.i32(0));

      // Test index 2 -> returns 2
      runner.assertReturn(
          "main",
          new WasmValue[] {WasmValue.i32(2)},
          WasmValue.i32(2),
          WasmValue.i32(0),
          WasmValue.i32(0));

      // Test index 10 (out of range) -> returns 3 (default)
      runner.assertReturn(
          "main",
          new WasmValue[] {WasmValue.i32(3)},
          WasmValue.i32(10),
          WasmValue.i32(0),
          WasmValue.i32(0));
    }
  }
}

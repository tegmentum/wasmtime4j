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

package ai.tegmentum.wasmtime4j.wasmtime.generated.funcref;

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
 * Generated test from WAST file: function-references/table_fill.wast
 *
 * <p>This test validates table.fill instruction with externref values. Initially all table elements
 * are null, and fill operations set ranges of elements.
 *
 * <p>Requires: gc = true (for externref tables)
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/function-references/table_fill.wast
 */
public final class TableFillTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = TableFillTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table.fill with null externref")
  public void testTableFillNull(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/TableFillTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Initially all elements are null
      runner.assertReturn("get", new WasmValue[] {WasmValue.externRefNull()}, WasmValue.i32(1));
      runner.assertReturn("get", new WasmValue[] {WasmValue.externRefNull()}, WasmValue.i32(2));

      // Fill with null at position 9
      runner.invoke("fill", WasmValue.i32(9), WasmValue.externRefNull(), WasmValue.i32(1));

      // Position 9 should still be null
      runner.assertReturn("get", new WasmValue[] {WasmValue.externRefNull()}, WasmValue.i32(9));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table.fill out of bounds traps")
  public void testTableFillOutOfBounds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/funcref/TableFillTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);

      // Fill beyond table bounds should trap
      runner.assertTrap(
          "fill", "out of bounds", WasmValue.i32(11), WasmValue.externRefNull(), WasmValue.i32(0));
    }
  }
}
